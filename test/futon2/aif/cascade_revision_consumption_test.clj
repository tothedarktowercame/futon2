(ns futon2.aif.cascade-revision-consumption-test
  "DIFFICULTY-REVISES-CASCADE packet 2: a difficulty overcome in run A
   leaves a retained :revised cascade that run B's construction consumes.
   The fixture is the packet-1 real-path round (an admitted
   :gauntlet/placenta-transfer extension revised in from an empty parent),
   not a hand-written revision shape. Run A deposits the receipt through
   cascade-feedback/record! into a temp store; run B reads the same store
   through load-construction-metadata, exactly what
   full_loop_runtime.clj's :cascade-feedback-metadata load does."
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-feedback :as feedback]
            [futon2.aif.cascade-revision :as revision]
            [futon2.aif.cascade-revision-producer :as producer]
            [futon2.aif.cascade-revision-producer-test :as p1]
            [futon2.aif.interpretation-construction :as construction]))

(def target p1/target)
(def want p1/want)

(defn- proposals
  "The same injected producer round packet 1's test runs: sources, request
   and ask! all injected, the admitted :gauntlet/placenta-transfer reading
   as the one proposal."
  [reply]
  ((producer/make-proposals-fn
    {:sources-fn (fn [_ _] p1/sources)
     :request-fn (fn [{:keys [target want]}]
                   {:schema :wm/want-interpretation-request-v1
                    :target target :want {:token want}})
     :code-root p1/library-root})
   (assoc p1/context :ask! (constantly reply))))

(defn- run-a-revision
  "Run A's ordinary revision round: the admitted proposal becomes a real
   :revised cascade revision with a recorded delta against its parent."
  []
  (let [result (proposals (str "```edn\n" (pr-str p1/response) "\n```"))
        revised (revision/revise
                 {:original (:original p1/context)
                  :head-context (revision/head-seed (:original p1/context))
                  :whole-context (dissoc (:whole-mission p1/context) :content)
                  :blocker (:blocker p1/context)
                  :proposals (:candidates result)})]
    revised))

(defn- run-a-deposits!
  [store revision outcome]
  (let [receipt (feedback/receipt
                 {:run-id "2026-11-01-runA" :target target
                  :selected-action (:original revision)
                  :outcome outcome
                  :cascade-revision revision})
        written (feedback/record! store receipt)]
    (is (= :recorded (:status written)) (pr-str written))
    receipt))

(defn- run-b-input
  "Run B's construction input: the CURRENT interpretations include the
   pattern run A's revision added, and the retained revisions come from
   what run B actually reads (load-construction-metadata over run A's
   store), never from run A's in-memory value."
  [store]
  (let [metadata (feedback/load-construction-metadata store)]
    {:target target :want [want]
     :observation {:sites-enumerated true :one-producer true
                   want false}
     :interpretations
     {:gauntlet/placenta-transfer
      {:guard {:needs #{:sites-enumerated :one-producer} :forbids #{}}
       :produces #{want}}}
     :interpretation-receipts
     {:gauntlet/placenta-transfer {:receipt (:receipt p1/response)}}
     :horizon 4 :move-cost 0
     :budget {:max-moves 4 :max-expansions 20000}
     ;; G: the empty baseline scores 0, every non-empty cascade -1, so the
     ;; construction move is worth taking (stop rule takes the move).
     :evaluate-g (fn [c] (if (seq (:precedence c)) -1.0 0.0))
     :retained-revisions
     (feedback/retained-revision-candidates (get metadata target) target)}))

(deftest run-b-menu-carries-and-selects-the-retained-revision
  (let [store (str (System/getProperty "java.io.tmpdir") "/"
                   "wm-revision-consumption-" (System/currentTimeMillis) ".edn")
        revised (run-a-revision)]
    (is (= :revised (:status revised)) (pr-str revised))
    (is (= [:gauntlet/placenta-transfer] (:added (:delta revised))))
    ;; run A deposits
    (run-a-deposits! store revised :grounded-progress)
    ;; run B reads and constructs
    (let [input (run-b-input store)
          result (construction/construct input)
          retained-entry
          (some #(when (#{:injected :already-constructed} (:status %)) %)
                (:retained-revisions result))]
      (is (= :constructed (:status result)) (pr-str result))
      ;; the menu (what selection reads) holds the revised candidate with
      ;; the added pattern and the provenance showing consideration
      (is (= [:gauntlet/placenta-transfer]
             (:precedence retained-entry)) (pr-str retained-entry))
      (is (= "2026-11-01-runA"
             (get-in retained-entry [:provenance :run/id])))
      (is (= [:gauntlet/placenta-transfer]
             (:added (:delta retained-entry))))
      (let [menu-candidate
            (some #(when (= :gauntlet/placenta-transfer
                            (first (:precedence %))) %)
                  (:candidates result))]
        (is menu-candidate (pr-str (:candidates result)))
        ;; when selected, the construction carries the added pattern and
        ;; names the retained revision it came from
        (is (= [:gauntlet/placenta-transfer] (:precedence menu-candidate)))
        (is (= "2026-11-01-runA"
               (get-in menu-candidate [:retained-revision :provenance :run/id])))))))

(deftest refused-and-noop-revisions-never-enter-run-bs-menu
  (let [store (str (System/getProperty "java.io.tmpdir") "/"
                   "wm-revision-consumption-" (System/currentTimeMillis) ".edn")
        revised (run-a-revision)
        ;; a refused revision: the pre-packet-1 shape, where the producer
        ;; could not reconstitute construction inputs
        refused-revision
        {:schema revision/revision-schema :status :refused
         :kind :revision-construction-inputs-unavailable
         :target target
         :original {:identity "sha" :patterns []}
         :blocker (:blocker revised)
         :whole-context (dissoc (:whole-mission p1/context) :content)
         :repair-evidence {:status :present
                           :reason :whole-mission-skim-found-no-extension}}
        ;; a content no-op: same pattern set as the parent (packet 1
        ;; refuses these at revise time; the store must also never
        ;; project one if such a record exists)
        noop-revision
        (assoc revised
               :delta {:added [] :removed [] :retained []}
               :history [{:ordinal 0 :identity "p" :patterns []}
                         {:ordinal 1 :identity "c" :patterns []}]
               :revised (assoc (:revised revised) :precedence []))]
    (run-a-deposits! store refused-revision :incomplete)
    (run-a-deposits! store noop-revision :grounded-progress)
    (let [metadata (feedback/load-construction-metadata store)
          candidates (feedback/retained-revision-candidates
                      (get metadata target) target)
          input (assoc (run-b-input store)
                       :retained-revisions candidates)
          result (construction/construct input)]
      (is (= [] candidates) (pr-str candidates))
      (is (= :constructed (:status result))
          (pr-str (dissoc result :retained-revisions :candidates)))
      ;; the refused/no-op revisions appear nowhere in run B's menu
      (is (nil? (some :retained-revision (:candidates result))))
      (is (nil? (some #(= :injected (:status %)) (:retained-revisions result)))))))

(deftest non-minimal-retained-revision-changes-run-bs-plan-content
  "The content-change case: pattern :sup alone produces nothing wanted, so
   run B's own construction strips every plan containing it as a superset
   of {:prod}. A retained revision whose parent was {:prod} and whose way
   out of the difficulty was ADDING :sup injects a plan the native menu can
   never hold. Run A's revision is minted by the real revise (same gates:
   typed blocker, head seed, pinned whole-mission context, no-op refusal)
   from the packet-1 context; only the proposal list is shaped, which is
   the seam production fills from the whole-mission skim."
  (let [store (str (System/getProperty "java.io.tmpdir") "/"
                   "wm-revision-content-" (System/currentTimeMillis) ".edn")
        parent {:kind :cascade-candidate :id :C0 :target target
                :precedence [:prod] :construction-receipt {:reached-wants [want]}}
        revised (revision/revise
                 {:original parent
                  :head-context (revision/head-seed parent)
                  :whole-context (dissoc (:whole-mission p1/context) :content)
                  :blocker (:blocker p1/context)
                  :proposals [{:kind :cascade-candidate :id :C1 :target target
                               :precedence [:sup :prod]
                               :construction-receipt {:reached-wants [want]}}]})]
    (is (= :revised (:status revised)) (pr-str revised))
    (is (= [:sup] (:added (:delta revised))))
    (is (= [] (:removed (:delta revised))))
    (run-a-deposits! store revised :grounded-progress)
    (let [metadata (feedback/load-construction-metadata store)
          interpretations
          {:sup {:guard {:needs #{:sites-enumerated} :forbids #{}}
                 :produces #{:support-token}}
           :prod {:guard {:needs #{:one-producer} :forbids #{}}
                  :produces #{want}}}
          result (construction/construct
                  {:target target :want [want]
                   :observation {:sites-enumerated true :one-producer true
                                 :support-token false want false}
                   :interpretations interpretations
                   :interpretation-receipts
                   {:sup {:receipt (:receipt p1/response)}
                    :prod {:receipt (:receipt p1/response)}}
                   :horizon 4 :move-cost 0
                   :budget {:max-moves 4 :max-expansions 20000}
                   ;; richer cascades score lower G, so the added-support
                   ;; plan wins selection when the menu competes
                   :evaluate-g (fn [c] (- (count (:precedence c))))
                   :retained-revisions
                   (feedback/retained-revision-candidates
                    (get metadata target) target)})
          injected (some #(when (= :injected (:status %)) %)
                         (:retained-revisions result))
          menu-entry (some #(when (= [:sup :prod] (:precedence %)) %)
                           (:candidates result))]
      (is injected (pr-str (:retained-revisions result)))
      ;; the plan changes in CONTENT: [:sup :prod] is not in run B's native
      ;; minimal family, and the menu (what selection reads) holds it with
      ;; the added pattern carried through construction
      (is menu-entry (pr-str (:candidates result)))
      (is (= :machine-constructed
             (get-in menu-entry [:construction-receipt :kind])))
      (is (= [:sup] (get-in menu-entry [:retained-revision :delta :added])))
      (is (= "2026-11-01-runA"
             (get-in menu-entry [:retained-revision :provenance :run/id]))))))
