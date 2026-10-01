(ns futon2.aif.construction-receipt-lean-adapter-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.construction-receipt-lean-adapter :as adapter]))

(def fixture "test/fixtures/construction-receipt-lean/two-unit.edn")
(defn fixture-bytes [] (java.nio.file.Files/readAllBytes (.toPath (io/file fixture))))
(defn refusal [f]
  (try (f) nil (catch clojure.lang.ExceptionInfo failure (:refusal (ex-data failure)))))

(deftest pinned-runtime-value-renders-the-lean-carrier
  (let [raw (fixture-bytes)
        sha (adapter/sha256 raw)
        {:keys [source-sha256 lean]} (adapter/adapt-bytes raw sha)]
    (is (= sha source-sha256))
    (is (.contains lean (str "Source SHA-256: " sha)))
    (is (.contains lean "def runtimeSemantics : Nat → UnitSemantics"))
    (is (.contains lean "| 0 => ⟨{0}, ∅⟩"))
    (is (.contains lean "| 1 => ⟨{1}, {0}⟩"))
    (is (.contains lean "support := [⟨0, 1, {0}⟩]"))
    (is (.contains lean "meets := [⟨0, 1, 1, [⟨0, 1, {0}⟩], []⟩]"))
    (is (.contains lean "theorem decoded_runtime_receipt_valid"))
    (is (= "99be41624409ce82e7b39ceaa824fb6b22156194faaf1918c61a4f3c38cc9abb"
           (adapter/sha256 (.getBytes lean java.nio.charset.StandardCharsets/UTF_8))))))

(deftest pins-and-domain-fail-closed
  (testing "changed bytes do not decode under the old pin"
    (is (= :adapter/source-pin-mismatch
           (refusal #(adapter/adapt-bytes (fixture-bytes) (apply str (repeat 64 "0")))))))
  (testing "trailing EDN is not ignored"
    (let [raw (.getBytes "{} {}" java.nio.charset.StandardCharsets/UTF_8)]
      (is (= :adapter/trailing-form
             (refusal #(adapter/adapt-bytes raw (adapter/sha256 raw)))))))
  (testing "receipt units cannot escape the interpreted domain"
    (let [raw (.getBytes
               (pr-str {:schema adapter/schema :precedence [:P]
                        :interpretations {:Q {:guard {:needs #{}} :produces #{}}}
                        :construction-receipt {}})
               java.nio.charset.StandardCharsets/UTF_8)]
      (is (= :adapter/domain-mismatch
             (refusal #(adapter/adapt-bytes raw (adapter/sha256 raw))))))))

(deftest forged-known-token-is-preserved-for-lean-to-refute
  (let [input {:schema adapter/schema :precedence [:P :Q]
               :interpretations
               {:P {:guard {:needs #{}} :produces #{:q :forged}}
                :Q {:guard {:needs #{:q}} :produces #{}}
                }
               :construction-receipt
               {:kind :machine-constructed
                :relations {:status :computed
                            :support {:relations [{:from :P :to :Q :tokens #{:forged}}]}
                            :meet {:relations []}
                            :precedence {:relations [{:from :P :to :Q :tokens #{:forged}}]
                                         :linear-extension [:P :Q] :violations []}}}}
        lean (adapter/render input (apply str (repeat 64 "a")))]
    ;; The adapter does not silently repair semantic lies. The exact forged
    ;; value reaches Lean, where decoded_runtime_receipt_valid cannot prove.
    (is (.contains lean "support := [⟨0, 1, {0}⟩]"))
    (is (.contains lean "| 1 => ⟨∅, {1}⟩"))))

(defn recorded-run
  ([] (recorded-run "M-live"))
  ([unit-target]
   {:run/id "run-live-1" :click/id "click-live-1"
    :decision
    {:chosen {:target "M-live" :candidate :C1}
     :selection-certificate
     {:candidates
      [{:id {:kind :cascade-candidate :id :C1 :target "M-live"
             :precedence
             [{:id :P :target unit-target
               :produces #{[unit-target :q]}
               :guard {:clauses [{:present #{}}]}}
              {:id :Q :target "M-live" :produces #{}
               :guard {:clauses [{:present #{["M-live" :q]}}]}}]
             :construction-receipt
             {:kind :machine-constructed
              :relations
              {:status :computed
               :support {:relations [{:from :P :to :Q :tokens #{:q}}]}
               :meet {:relations []}
               :precedence {:relations [{:from :P :to :Q :tokens #{:q}}]
                            :linear-extension [:P :Q]
                            :violations []}}}}}]}}}))

(deftest production-run-record-projects-the-selected-candidate
  (let [run (recorded-run)
        raw (.getBytes (pr-str run) java.nio.charset.StandardCharsets/UTF_8)
        sha (adapter/sha256 raw)
        adapted (adapter/adapt-run-record-bytes raw sha)
        projection (:projection adapted)]
    (is (= sha (:source-sha256 adapted)))
    (is (= {:run-id "run-live-1" :click-id "click-live-1"
            :target "M-live" :candidate :C1}
           (:runtime-source projection)))
    (is (= [:P :Q] (:precedence projection)))
    (is (= #{:q} (get-in projection [:interpretations :P :produces])))
    (is (= #{:q} (get-in projection [:interpretations :Q :guard :needs])))
    (is (.contains (:lean adapted) "theorem decoded_runtime_receipt_valid"))
    (is (.contains (:lean adapted)
                   "Runtime source: {:run-id \"run-live-1\""))))

(deftest production-projection-refuses-mixed-targets-and-ambiguous-selection
  (testing "the target wrapper is evidence, not decoration"
    (is (= :adapter/mixed-candidate-target
           (refusal #(adapter/input-from-run-record (recorded-run "M-other"))))))
  (testing "candidate identity must select exactly one recorded value"
    (let [run (recorded-run)
          duplicate (update-in run [:decision :selection-certificate :candidates]
                               #(conj % (first %)))]
      (is (= :adapter/selected-candidate-ambiguous
             (refusal #(adapter/input-from-run-record duplicate)))))))

(defn recorded-g-run [recorded-g]
  {:run/id "run-g-1" :click/id "click-g-1"
   :decision
   {:chosen {:target "M-live" :candidate :C1}
    :selection-certificate
    {:scoring
     {0 {:id {:kind :cascade-candidate :id :C1 :target "M-live"}
         :schema :wm/bounded-observation-score-v1
         :g recorded-g
         :g-terms {:risk 0.5 :ambiguity 0.25
                   :expected-information-gain 0.125
                   :combination :risk-plus-ambiguity-minus-information-gain
                   :units :nats}}}}}})

(deftest retained-g-renders-an-exact-float-combination-check
  (let [run (recorded-g-run 0.625)
        raw (.getBytes (pr-str run) java.nio.charset.StandardCharsets/UTF_8)
        adapted (adapter/adapt-run-record-g-bytes raw (adapter/sha256 raw))]
    (is (= 0.625 (get-in adapted [:projection :recorded-g])))
    (is (.contains (:lean adapted)
                   "recorded_g_matches_risk_plus_ambiguity_minus_information"))
    (is (.contains (:lean adapted) "def recordedG : Float := Float.ofBits"))))

(deftest retained-g-mutation-changes-the-exact-named-check
  (let [good (adapter/render-g
              (adapter/g-input-from-run-record (recorded-g-run 0.625)) "good")
        mutated (adapter/render-g
                 (adapter/g-input-from-run-record (recorded-g-run 0.626)) "mutated")]
    (is (not= good mutated))
    (is (.contains mutated
                   "recorded_g_matches_risk_plus_ambiguity_minus_information"))
    ;; Both inputs reach Lean as exact bits; native_decide succeeds only for
    ;; the good value and rejects this changed recorded G.
    (is (not= (re-find #"def recordedG.*" good)
              (re-find #"def recordedG.*" mutated)))))

(def controlled-record
  "data/wm-runs/tick-run-record-2026-10-01-9cc46d2e-184e-4a97-aa25-aedb383ec426.edn")
(def controlled-sha "8fe42ee7b5633d18deeb4b58a9c4c08f2107dd4cbf033665d2460671909563fa")

(deftest controlled-record-renders-target-local-aqc-certificate
  (let [bytes (java.nio.file.Files/readAllBytes (.toPath (io/file controlled-record)))
        adapted (adapter/adapt-run-record-aqc-bytes bytes controlled-sha)]
    (is (= controlled-sha (:source-sha256 adapted)))
    (is (= :related (get-in adapted [:projection :target-class])))
    (is (= 4 (get-in adapted [:projection :horizon])))
    (is (.contains (:lean adapted) "retained_A_is_target_local"))
    (is (.contains (:lean adapted) "retained_Q_reaches_target"))
    (is (.contains (:lean adapted) "retained_C_is_normalised"))))

(deftest aqc-certificate-refuses-identity-and-correspondence-mutations
  (let [record (read-string (slurp controlled-record))]
    (testing "selecting the unrelated unknown target cannot borrow C1's A/Q/C"
      (is (= :adapter/aqc-correspondence-mismatch
             (refusal #(adapter/aqc-input-from-run-record
                        (-> record
                            (assoc-in [:decision :chosen :target] "M-formal-patterns")
                            (assoc-in [:decision :selection-certificate :scoring 0 :id :target]
                                      "M-formal-patterns")
                            (assoc-in [:decision :selection-certificate :candidates 0 :id :target]
                                      "M-formal-patterns")))))))
    (testing "zero preference for the retained A class breaks correspondence"
      (is (= :adapter/aqc-correspondence-mismatch
             (refusal #(adapter/aqc-input-from-run-record
                        (-> record
                            (assoc-in [:decision :selection-certificate :scoring 0
                                       :consumed-g :C :steps 3 :distribution :related] 0)
                            (assoc-in [:decision :selection-certificate :scoring 0
                                       :consumed-g :C :steps 3 :distribution :focused] 9/10)))))))))
