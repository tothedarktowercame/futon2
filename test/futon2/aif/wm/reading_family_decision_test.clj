(ns futon2.aif.wm.reading-family-decision-test
  "Q5 step 2 at the production seam: cascade-decision selects once, by G,
  over the reading policy family of the conditioning target. The old
  interpreted route is unreachable, every policy's history identity is its
  structural reading-policy key, and F is a canonical carrier bound to the
  decision's ledger pin."
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [futon2.aif.efe :as efe]
            [futon2.aif.decision-gate :as decision-gate]
            [futon2.aif.enactment-fold-source :as fold-source]
            [futon2.aif.enactment-habit :as enactment-habit]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.aif.learning-trial-ledger :as ledger]
            [futon2.aif.pattern-graph-pin :as graph-pin]
            [futon2.aif.policy :as policy]
            [futon2.aif.policy-prefix-admission :as admission]
            [futon2.aif.reading-interpretation-debt :as debt]
            [futon2.aif.target-reading-registry :as reading-registry]
            [futon2.aif.ticket-queue :as ticket-queue]
            [futon2.aif.wm.cascade-decision :as wm-cd])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(use-fixtures :once hermetic/with-hermetic-stores)

(defn- temp-dir [prefix]
  (.toFile (Files/createTempDirectory prefix (make-array FileAttribute 0))))

(use-fixtures :each
  (fn [run] (with-redefs [ledger/default-root (.getPath (temp-dir "reading-decision-ledger-"))]
              (run))))

(def real-flight "test/fixtures/prefix-history/flight-278b6988.edn")

(defn- analysis [patterns]
  {:status "analyzed"
   :sentences
   [{:id "s1"
     :fragments (vec (map-indexed
                      (fn [i pattern]
                        {:start i :end (inc i) :text pattern :intent "act" :target "target"
                         :rationale "fixture fit" :relations ["action"]
                         :pattern_refs [{:id pattern :status "candidate"
                                         :rationale "fixture fit"
                                         :source_sha256 (apply str (repeat 64 "a"))}]})
                      patterns))}]})

(defn- pinned-graph []
  (let [file (io/file (temp-dir "reading-decision-graph-") "graph.json")]
    (spit file (json/generate-string
                {:records 1 :patterns 3 :pattern_ids ["p/a" "p/b" "p/c"]
                 :summary [{:through "why" :edges 2} {:through "how" :edges 0}]
                 :edges [{:a "p/a" :b "p/b" :kind "why" :evidence []}
                         {:a "p/b" :b "p/c" :kind "why" :evidence []}]}))
    (graph-pin/pin! file)
    file))

(defn- fixture
  "M-current has a current reading naming READ (default p/a and p/b);
  M-unread has a source and no reading; T-no-path has no source. The flight
  ledger is the real explicit-absence record, so it is complete and records
  no policy."
  [& [read]]
  (let [root (temp-dir "reading-decision-root-")
        current (io/file root "M-current.md")
        unread (io/file root "M-unread.md")
        flights (temp-dir "reading-decision-flights-")]
    (spit current "# Current\n\n## HEAD\nCurrent work.\n")
    (spit unread "# Unread\n\n## HEAD\nNobody has read this yet.\n")
    (io/copy (io/file real-flight) (io/file flights "flight-278b6988.edn"))
    (let [digest (reading-registry/excerpt-digest current)]
      (reading-registry/publish!
       root {:target-id "M-current" :source-path (.getPath current)
             :excerpt-digest digest
             :request {:task {:target_id "M-current" :excerpt_sha256 digest}}
             :analysis (analysis (or read ["p/a" "p/b"])) :validator-version 1}))
    {:root root :flights flights :graph (pinned-graph)
     :rows {:current {:target-id "M-current" :source-kind :head
                      :source-path (.getPath current) :source-absent nil}
            :unread {:target-id "M-unread" :source-kind :head
                     :source-path (.getPath unread) :source-absent nil}
            :no-path {:target-id "T-no-path" :source-kind :item-section
                      :source-path nil :source-absent :target-source-path-absent}}}))

(defn- assembled [rows betas]
  {:problems []
   :refusals (mapv #(hash-map :target (:target-id %) :kind :no-admitted-interpretation) rows)
   :target-sources (vec rows)
   :declared-betas betas})

(defn- decide
  "Run the production decision, counting selector calls, with the old
  interpreted scorer and lane made to throw if they are ever reached."
  [{:keys [root flights graph]} rows betas & [extra-opts]]
  (let [selector-calls (atom 0)
        select policy/select-action-cascades]
    (with-redefs [policy/select-action-cascades
                  (fn [& args] (swap! selector-calls inc) (apply select args))
                  efe/rank-actions
                  (fn [& _] (throw (ex-info "old scorer reached" {:kind :old-scorer-reached})))
                  wm-cd/cascade-lane
                  (fn [& _] (throw (ex-info "old lane reached" {:kind :old-lane-reached})))]
      (let [result (wm-cd/cascade-decision
                    (assembled rows betas)
                    (merge {:pattern-graph-path (str graph)
                            :target-reading-root (str root)
                            :conditioning-steps (fold-source/conditioning-steps (str flights))
                            :ticket-queue ticket-queue/empty-declaration
                            :novelty-inputs {}}
                           extra-opts))]
        (assoc result ::selector-calls @selector-calls)))))

(defn- selected-entry [decision]
  ((ns-resolve 'futon2.aif.full-loop-runner 'selected-entry) {:decision decision}))

(defn- debt-status [decision]
  (runner/reading-interpretation-status decision))

(deftest one-g-selection-over-the-reading-family
  ;; The reading names p/a and p/c; the graph connects them through p/b, so a
  ;; retraction policy owes the interpretation of p/b.
  (let [fx (fixture ["p/a" "p/c"])
        result (decide fx [(get-in fx [:rows :current])] {"M-current" 1})
        decision (:decision result)
        cert (:selection-certificate decision)
        family (first (:target-policy-families cert))
        action (:action decision)]
    (testing "the family has structurally distinct policies, all scored"
      (is (<= 2 (count (:policies family))))
      (is (= (count (:policies family)) (count (distinct (map :policy-id (:policies family)))))))
    (testing "exactly one selector call; the old scorer and lane were never reached"
      (is (= 1 (::selector-calls result)))
      (is (= :wm/reading-policy-family-v1 (:selection-route cert))))
    (testing "the chosen policy is a reading policy of the conditioning target"
      (is (= "M-current" (:target action)))
      (is (map? (get-in action [:precedence :co-apply])))
      (is (= (:policy-key action) (admission/candidate-key action)))
      (is (some #{(:id action)} (map :policy-id (:policies family)))))
    (testing "every compared policy has finite G and its F applicability recorded"
      (is (every? #(and (number? (:g %)) (Double/isFinite (double (:g %))))
                  (:candidates cert)))
      (is (every? #(= :not-applicable (:f-status %)) (:candidates cert)))
      (is (= (get-in cert [:policy-history-pin :digest])
             (get-in (first (:candidates cert)) [:f-prefix :ledger :digest]))))
    (testing "selection happened with the debt open, and is recorded"
      (is (= ["p/b"] (mapv :pattern (get-in action [:interpretation-debt :owed]))))
      (is (= :cascade-selection-posterior (get-in decision [:selection-law :applied])))
      (is (every? #(nil? (:interpretation-receipts (:id %))) (:candidates cert))))
    (testing "enactment waits for the owed interpretation"
      (is (nil? (selected-entry decision)))
      (let [status (debt-status decision)]
        (is (= :interpretation-owed-after-selection (:status status)))
        (is (= ["p/b"] (mapv :pattern (:remaining status))))
        (is (= (:id action) (:policy-id status)))
        (is (= :all-owed-nodes-validated-for-this-identity
               (get-in status [:rearm :condition])))))
    (testing "retired diagnostics are recorded unavailable, not consumed"
      (is (= :unavailable (get-in cert [:retired-diagnostics :token-rate-lanes :status])))
      (is (= :observation-only (get-in cert [:preference :live-c]))))))

(deftest zero-debt-policy-enacts-without-receipts
  (let [fx (fixture)
        result (decide fx [(get-in fx [:rows :current])] {"M-current" 1})
        decision (:decision result)]
    (is (= 1 (::selector-calls result)))
    (is (= [] (get-in decision [:action :interpretation-debt :owed])))
    (is (= :discharged (:status (debt-status decision))))
    (is (= (:action decision) (:action (selected-entry decision))))))

(defn- with-action
  "DECISION with its chosen action replaced by (F action) everywhere the
  enactment boundary reads it: the decision's :action and its candidate row."
  [decision f]
  (let [action (:action decision)
        forged (f action)]
    (-> decision
        (assoc :action forged)
        (update-in [:selection-certificate :candidates]
                   (fn [rows] (mapv #(if (= action (:id %)) (assoc % :id forged) %) rows))))))

(defn- receipts [decision rs]
  (assoc-in decision [:selection-certificate :interpretation-discharge :receipts] rs))

(deftest debt-binding-is-immutable
  (let [fx (fixture ["p/a" "p/c"])
        decision (:decision (decide fx [(get-in fx [:rows :current])] {"M-current" 1}))
        action (:action decision)
        carrier (:interpretation-debt action)
        id (:id action)
        good-receipt (merge (first (:owed carrier))
                            {:policy-id id :debt-digest (:digest carrier) :status :validated})
        refused? (fn [d] (nil? (selected-entry d)))]
    (testing "the full discharge for this exact identity is what re-arms enactment"
      (is (= action (:action (selected-entry (receipts decision [good-receipt]))))))
    (testing "partial or misdirected discharge leaves the debt open"
      (is (refused? (receipts decision [(assoc good-receipt :pattern "p/a")])))
      (is (refused? (receipts decision [(assoc good-receipt :status :refused)]))))
    (testing "a receipt for another policy or another debt is refused"
      (let [foreign (assoc good-receipt :policy-id "M-current/policy-0000000000000000ffff")
            stale (assoc good-receipt :debt-digest (apply str (repeat 64 "0")))]
        (is (= :foreign-interpretation-receipt
               (:reason (debt-status (receipts decision [foreign])))))
        (is (refused? (receipts decision [foreign])))
        (is (refused? (receipts decision [stale])))))
    (testing "forged or detached carriers refuse"
      (doseq [[label f]
              {:forged-empty-debt (fn [a] (assoc-in a [:interpretation-debt :owed] []))
               :forged-empty-debt-redigested
               (fn [a] (assoc a :interpretation-debt
                              (debt/debt a {:nodes (mapv (fn [p] {:pattern p :status :accepted})
                                                         (map first (:node-status carrier)))}
                                         (:analysis-digest carrier))))
               :missing-carrier (fn [a] (dissoc a :interpretation-debt))
               :order-mismatch (fn [a] (update-in a [:interpretation-debt :order] (comp vec reverse)))
               :arrangement-mismatch
               (fn [a] (assoc-in a [:interpretation-debt :arrangement-digest] (apply str (repeat 64 "1"))))
               :foreign-policy-carrier (fn [a] (assoc-in a [:interpretation-debt :policy-id]
                                                         "M-current/policy-0000000000000000ffff"))}]
        (testing (name label)
          ;; checked on the debt itself, with and without the receipt, so
          ;; the Prefix-F guard (which also sees the edited action) is not
          ;; what refuses it
          (is (not= :discharged (:status (debt-status (with-action decision f)))))
          (is (not= :discharged (:status (debt-status (receipts (with-action decision f) [good-receipt])))))
          (is (refused? (receipts (with-action decision f) [good-receipt]))))))))

(deftest unread-targets-are-counted-and-never-chosen
  (let [fx (fixture)
        rows (vals (select-keys (:rows fx) [:current :unread :no-path]))
        result (decide fx (vec rows) {"M-current" 1 "M-unread" 1 "T-no-path" 1})
        cert (get-in result [:decision :selection-certificate])]
    (is (= 1 (::selector-calls result)))
    (is (= "M-current" (get-in result [:decision :action :target])))
    (is (= #{"M-unread" "T-no-path"}
           (set (map :target-id (filter #(empty? (:policies %)) (:target-policy-families cert))))))
    (is (= #{:no-current-target-reading :target-source-path-absent}
           (set (map :kind (:family-failures cert)))))))

(deftest all-failed-field-abstains-typed-with-no-selector-call
  (let [fx (fixture)
        rows [(get-in fx [:rows :unread]) (get-in fx [:rows :no-path])]
        result (decide fx rows {"M-unread" 1 "T-no-path" 1})
        decision (:decision result)]
    (is (= 0 (::selector-calls result)))
    (is (= :abstained (:status decision)))
    (is (nil? (:action decision)))
    (is (= [[:no-policy-family :no-current-target-reading]
            [:no-policy-family :target-source-path-absent]]
           (mapv (juxt :kind :failure-kind) (:refusals decision))))
    (is (= 2 (count (get-in decision [:selection-certificate :target-policy-field :families]))))))

(deftest undeclared-beta-abstains-without-a-default
  (let [fx (fixture)
        result (decide fx [(get-in fx [:rows :current])] {})]
    (is (= 0 (::selector-calls result)))
    (is (= :beta-not-declared (get-in result [:decision :refusals 0 :kind])))))

(defn- chosen-action [fx]
  (get-in (decide fx [(get-in fx [:rows :current])] {"M-current" 1}) [:decision :action]))

(deftest reading-policy-history-key
  (let [fx (fixture)
        action (chosen-action fx)
        key (admission/candidate-key action)
        occurrence (first (get-in action [:precedence :co-apply :units]))]
    (testing "theta and learning updates leave the key unchanged"
      (is (= key (admission/candidate-key
                  (assoc-in action [:precedence :co-apply :patterns occurrence :theta] 0.9))))
      (is (= key (admission/candidate-key
                  (assoc-in action [:precedence :co-apply :patterns occurrence :theta-record]
                            {:status :recorded-trials :theta 0.9})))))
    (testing "a different arrangement is a different key"
      (let [edited (update-in action [:arrangement :edges] conj
                              {:from occurrence :to occurrence :kind :overlap})]
        (is (some? (admission/candidate-key (dissoc edited :policy-key))))
        (is (not= key (admission/candidate-key (dissoc edited :policy-key))))))
    (testing "mismatches refuse"
      (is (nil? (admission/candidate-key (update-in action [:arrangement :edges] conj
                                                    {:from occurrence :to occurrence :kind :overlap})))
          "structure edited under a carried key")
      (is (nil? (admission/candidate-key (assoc action :id "M-other/policy-0123456789abcdef0123"))))
      (is (nil? (admission/candidate-key (assoc-in action [:precedence :co-apply :units] [])))))
    (testing "the flight step writer records the same key"
      (is (= key (enactment-habit/policy-key-for
                  {:candidate (:id action)} {:decision {:action action}} nil))))))

(deftest recorded-history-without-computed-f-refuses
  (let [fx (fixture)
        action (chosen-action fx)
        ;; the chosen policy ran once; its step was refused, so it has history
        ;; and no admitted prefix: no zero-history carrier, no computed F
        _ (spit (io/file (:flights fx) "flight-mine.edn")
                (pr-str {:plan {}
                         :flight {:flight/id "f-mine" :target "M-current"
                                  :enactments [{:record-path "/runs/c1.edn" :click-id "c1"
                                                :step {:schema :wm/conditioning-step-v1
                                                       :status :refused :reason :unmeasured-class
                                                       :policy-key (admission/candidate-key action)
                                                       :occurrence {:click "c1" :flight "f-mine"}}}]}}))
        refusal (try (decide fx [(get-in fx [:rows :current])] {"M-current" 1}) nil
                     (catch clojure.lang.ExceptionInfo e (ex-data e)))]
    (is (= :free-energy-not-supplied (get-in refusal [:refusal :kind])))))

(deftest foreign-ledger-pin-refuses-enactment
  (let [fx (fixture)
        decision (:decision (decide fx [(get-in fx [:rows :current])] {"M-current" 1}))
        other (admission/census-pin
               (admission/history-census
                (fold-source/conditioning-steps (str (temp-dir "reading-decision-other-")))))]
    (is (some? (selected-entry decision)))
    (is (nil? (selected-entry (assoc-in decision [:selection-certificate :policy-history-pin] other))))))

(deftest gate-admits-reading-policies-by-identity-only
  (let [fx (fixture)
        decision (:decision (decide fx [(get-in fx [:rows :current])] {"M-current" 1}))
        action (:action decision)
        forged (assoc action :policy-key [:wm/reading-policy-v1 "M-current" (:id action) {}])
        swap (fn [d] (-> d
                         (assoc :action forged)
                         (update-in [:selection-law :posterior]
                                    (fn [p] (into {} (map (fn [[k v]] [(if (= k action) forged k) v])) p)))))]
    (is (= decision (decision-gate/emit! decision)))
    (is (= :reading-policy-identity-invalid
           (try (decision-gate/emit! (swap decision)) nil
                (catch clojure.lang.ExceptionInfo e (:reason (ex-data e))))))))
