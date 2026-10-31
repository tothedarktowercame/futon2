(ns futon2.aif.cascade-revision-producer-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-revision :as revision]
            [futon2.aif.cascade-revision-producer :as producer]
            [futon2.aif.interpretation-construction :as construction]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.three-halves-square :as ths]
            [futon2.aif.wm.cascade-decision :as cascade-decision]))

(def target "M-hand")
(def want :caller-converted)
(def library-root (.getCanonicalPath (io/file "test/fixtures/want-interp-library")))
(def blend-square
  (edn/read-string
   (slurp "test/fixtures/three-halves-square/publication-cadence.edn")))

(def sources
  {:universes {target {:sites-enumerated true :one-producer true
                       :caller-converted false}}
   :wants {target [want]}
   :locators {target {:sites-enumerated {:class :C3 :stated "sites enumerated"}
                      :one-producer {:class :C3 :stated "one producer"}
                      :caller-converted {:class :C3 :stated "caller converted"}}}
   :interpretations {target {:patterns {} :receipts {}}}
   :horizon-steps 4
   :beta-by-context {:revision {:beta 1}}
   :context-of (constantly :revision)
   :construction {:construct construction/construct
                  :budget {:max-moves 4 :max-expansions 20000}
                  :move-cost 0
                  :evaluate-g cascade-decision/constructed-candidate-g}})

;; The square's object texts must BE the pattern's IF/HOWEVER/BECAUSE/THEN
;; parts (three-halves-square parts-check), so they are parsed from the
;; pinned file instead of hand-written: the old hand-written fixture was
;; refused :square-text-not-from-pattern once that gate landed.
(defn- pattern-square
  [pattern-id]
  (let [path (str "futon3/library/" (namespace pattern-id) "/"
                  (name pattern-id) ".flexiarg")
        text (slurp (str library-root "/" path))
        sha (evidence/sha256 (.getBytes text "UTF-8"))
        parts (ths/pattern-parts text)
        texts {:G (get parts "BECAUSE") :I1 (get parts "IF")
               :I2 (get parts "HOWEVER") :B (get parts "THEN")}]
    (as-> blend-square sq
      (assoc sq :pattern pattern-id)
      (reduce-kv (fn [acc role part-text]
                   (assoc-in acc [:objects role]
                             {:text part-text
                              :source {:path path :sha256 sha}
                              :theory (get-in blend-square [:objects role :theory])}))
                 sq texts))))
(def placenta-path "futon3/library/gauntlet/placenta-transfer.flexiarg")
(def placenta-text (slurp (str library-root "/" placenta-path)))
(def placenta-sha (evidence/sha256 (.getBytes placenta-text "UTF-8")))
(def placenta-square (pattern-square :gauntlet/placenta-transfer))
(def response
  {:schema :wm/want-interpretation-response-v1
   :pattern :gauntlet/placenta-transfer
   :guard {:needs #{:sites-enumerated :one-producer} :forbids #{}}
   :produces #{:caller-converted}
   :receipt
   {:source {:path placenta-path :sha256 placenta-sha}
    :reading "Transfer the caller-conversion function from the operator."
    :scope-limit "Only the identified caller conversion transfers."
    :by "codex-test"
    :blend-square placenta-square}})

(def context
  {:target target
   :original {:kind :cascade-candidate :id :C0 :target target
              :precedence [] :construction-receipt {:reached-wants []}}
   :blocker {:schema :wm/mid-run-cascade-blocker-v1 :status :present
             :target target :stage :review :kind :stuck}
   :whole-mission {:schema :wm/whole-mission-context-v1 :target target
                   :scope :whole-mission
                   :path "/pinned/M-hand.md" :sha256 (apply str (repeat 64 "a"))
                   :content "# M-hand\n\nThe whole mission."}
   :evidence-root "/tmp/not-used-by-injected-request"})

(defn- proposals [reply]
  ((producer/make-proposals-fn
    {:sources-fn (fn [_ _] sources)
     :request-fn (fn [{:keys [target want]}]
                   {:schema :wm/want-interpretation-request-v1
                    :target target :want {:token want}})
     :code-root library-root})
   (assoc context :ask! (constantly reply))))

(deftest selected-judgment-reconstitutes-the-production-construction-view
  (let [row {:target target
             :cascade-problem {:facts (get-in sources [:universes target])
                               :want (get-in sources [:wants target])
                               :locators (get-in sources [:locators target])
                               :interpretations {}
                               :horizon-steps 4 :beta 1}
             :interpretation-receipts {}}
        view (producer/sources-from-judgment
              {:cascade-problems {:problems [row]}
               :construction-parameters
               {:budget {:value {:max-moves 4 :max-expansions 20000}}
                :move-cost {:value 0}}}
              target)]
    (is (= (get-in sources [:universes target])
           (get-in view [:universes target])))
    (is (= {:max-moves 4 :max-expansions 20000}
           (get-in view [:construction :budget])))
    (is (fn? (get-in view [:construction :evaluate-g])))))

(deftest raw-retrieval-hit-is-not-an-interpretation
  (let [result (proposals
                "```edn\n{:schema :wm/cascade-proposal-v1 :pattern :gauntlet/placenta-transfer}\n```")]
    (is (= :refused (:status result)))
    (is (= :raw-or-uninterpreted-revision-response (:kind result)))
    (is (nil? (:candidates result)))))

(deftest validated-reading-produces-an-admitted-executable-extension  (let [result (proposals (str "```edn\n" (pr-str response) "\n```"))
        candidate (first (:candidates result))]
    (is (= :admitted (:status result)) (pr-str result))
    (is (= :cascade-candidate (:kind candidate)))
    (is (= :machine-constructed
           (get-in candidate [:construction-receipt :kind])))
    (is (= [:gauntlet/placenta-transfer]
           (mapv :id (:precedence candidate))))
    (is (= (:sha256 (:whole-mission context))
           (get-in result [:receipt :whole-mission-sha256])))
    (is (= (:source (:receipt response))
           (get-in result [:receipt :pattern-source])))
    (let [revised
          (revision/revise
           {:original (:original context)
            :head-context (revision/head-seed (:original context))
            :whole-context (dissoc (:whole-mission context) :content)
            :blocker (:blocker context)
            :proposals (:candidates result)})]
      (is (= :revised (:status revised)))
      (is (= [:gauntlet/placenta-transfer]
             (get-in revised [:delta :added]))))))

;; ---------------------------------------------------------------------------
;; The real production path: run 2026-10-31-0be4fc7d-70db-4e96-8beb-ea24a1867896
;; (click wm-click-f56a06af) refused :revision-construction-inputs-unavailable
;; because the reading-policy judgment carries NO interpreted problem row
;; (:cascade-problems {:problems []} — that route is retired) and nothing
;; writes :construction-parameters into a judgment. Fixtures below mirror
;; the retained record's shapes: guard-as-clauses patterns, the mission's
;; declared beta, and want tokens from holes/M-peradam-grounding.md.

(def pg-target "M-peradam-grounding")
(def pg-wants [[pg-target :hole/h0a34e3946ffb] [pg-target :hole/hb0d779d21b3a]])
(def pg-judgment
  {:cascade-problems {:problems []
                      :declared-betas {pg-target 1}}})
(def pg-original
  {:kind :cascade-candidate :id "M-peradam-grounding/policy-8eb6d33e28744a76f855"
   :target pg-target
   :want pg-wants
   :precedence
   [{:id [pg-target :p0] :pattern-id "writing-coherence/plain-language-thesis"
     :guard {:status :interpreted
             :clauses [{:present #{[pg-target :seed]} :absent #{}}]}
     :produces #{[pg-target :pattern-done :p0] [pg-target :hole/hb0d779d21b3a]}}]
   :observation-locators
   {[pg-target :seed] {:class :C4 :repo "futon7" :sha "HEAD"
                       :path "holes/M-peradam-grounding.md"
                       :decl "- [ ] the producer side consumes"}
    [pg-target :pattern-done :p0] {:class :C4 :repo "futon7" :sha "HEAD"
                                   :path "holes/M-peradam-grounding.md"
                                   :decl "- [x] CH2 delivers at least one realized record that the producer side consumes."}
    [pg-target :hole/h0a34e3946ffb] {:class :C4 :repo "futon7" :sha "HEAD"
                                     :path "holes/M-peradam-grounding.md"
                                     :decl "- [x] A real discharge emits a persisted peradam certificate or typed non-certificate joined to its `:move/id`."}
    [pg-target :hole/hb0d779d21b3a] {:class :C4 :repo "futon7" :sha "HEAD"
                                     :path "holes/M-peradam-grounding.md"
                                     :decl "- [x] CH2 delivers at least one realized record that the producer side consumes."}}
   :construction-receipt {:kind :machine-constructed :moves-taken 18}
   :interpretation-receipts
   {[pg-target :p0] {:source {:path "futon3/library/writing-coherence/plain-language-thesis.flexiarg"
                              :sha256 (evidence/sha256
                                       (.getBytes (slurp (str library-root "/futon3/library/writing-coherence/plain-language-thesis.flexiarg")) "UTF-8"))}
                     :reading "read what exists first" :by "wm"
                     ;; the only fixture-library file with all four
                     ;; square-able parts is placenta-transfer, so the
                     ;; pre-blocker pattern's square cites it (the square
                     ;; gate checks text-vs-pin, not pattern identity)
                     :blend-square (pattern-square :gauntlet/placenta-transfer)}}})

(defn- pg-proposals
  "The production port over the real-path judgment, with the reply the
   interpretation agent would give: one library pattern producing the first
   still-open want, guarded on a token the original precedence established."
  [reply]
  ((producer/make-proposals-fn
    {:judgment-fn (constantly pg-judgment)
     :request-fn (fn [{:keys [target want]}]
                   {:schema :wm/want-interpretation-request-v1
                    :target target :want {:token want}})
     :code-root library-root})
   {:target pg-target
    :original pg-original
    :blocker {:schema :wm/mid-run-cascade-blocker-v1 :status :present
              :target pg-target :stage :reviewer-verdict
              :kind :review-request-changes
              :evidence {:review-job "invoke-1793478526157-8921-e8c33395"
                         :findings-sha256 "findings"}}
    :whole-mission {:schema :wm/whole-mission-context-v1 :target pg-target
                    :scope :whole-mission :path "/pinned/M-peradam-grounding.md"
                    :sha256 (apply str (repeat 64 "a"))
                    :content "# M-peradam-grounding\n\nWhole mission."}
    :evidence-root "/tmp/not-used-by-injected-request"
    :ask! (constantly reply)}))

(deftest reading-route-judgment-still-yields-construction-inputs
  (let [view (producer/sources-from-candidate pg-judgment pg-original)]
    (is (map? view) "the fallback leg reconstitutes a real sources map")
    (is (= pg-wants (get-in view [:wants pg-target])))
    ;; post-construction snapshot: the chain the blocker interrupted is
    ;; established, the wants are not
    (is (true? (get-in view [:universes pg-target [pg-target :pattern-done :p0]])))
    (is (false? (get-in view [:universes pg-target [pg-target :hole/hb0d779d21b3a]])))
    ;; the clause grammar translates to the constructor's needs/forbids
    (is (= #{[pg-target :seed]}
           (get-in view [:interpretations pg-target :patterns
                         [pg-target :p0] :guard :needs])))
    (is (= 1 (get-in view [:beta-by-context :revision :beta]))
        "beta comes from the judgment's declared betas, not a default")
    (is (fn? (get-in view [:construction :construct])))))

(deftest real-path-revision-round-revises-the-cascade
  (let [reply (str "```edn\n"
                   (pr-str
                    {:schema :wm/want-interpretation-response-v1
                     :pattern :gauntlet/placenta-transfer
                     :guard {:needs #{[pg-target :pattern-done :p0]} :forbids #{}}
                     :produces #{[pg-target :hole/h0a34e3946ffb]}
                     :receipt (:receipt response)})
                   "\n```")
        produced (pg-proposals reply)]

    (is (= :admitted (:status produced)) (pr-str produced))
    (let [candidate (first (:candidates produced))]
      (is (= :cascade-candidate (:kind candidate)))
      (is (some #(= :gauntlet/placenta-transfer (:id %)) (:precedence candidate))
          "the revised precedence carries the blocker-responsive pattern")
      (is (not= (mapv :id (:precedence pg-original))
                (mapv :id (:precedence candidate))))
      (let [whole (assoc (:whole-mission context) :target pg-target)
            blocker (assoc (:blocker context) :target pg-target)
            revised
            (revision/revise
             {:original pg-original
              :head-context (revision/head-seed pg-original)
              :whole-context (dissoc whole :content)
              :blocker blocker
              :proposals (:candidates produced)})]
        (is (= :revised (:status revised)) (pr-str revised))
        (is (seq (get-in revised [:delta :added]))
            "the way out changes what the cascade thinks with")
        (is (empty? (get-in revised [:delta :removed])))))))

(deftest no-declared-beta-refuses-honestly
  (let [judgment {:cascade-problems {:problems [] :declared-betas {}}}
        result ((producer/make-proposals-fn
                 {:judgment-fn (constantly judgment)
                  :request-fn (fn [{:keys [target want]}]
                                {:schema :wm/want-interpretation-request-v1
                                 :target target :want {:token want}})
                  :code-root library-root})
                {:target pg-target :original pg-original
                 :blocker (:blocker context)
                 :whole-mission (:whole-mission context)
                 :evidence-root "/tmp/none"
                 :ask! (constantly "")})]
    (is (= :refused (:status result)))
    (is (= :revision-construction-inputs-unavailable (:kind result)))))
