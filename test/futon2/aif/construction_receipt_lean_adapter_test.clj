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
    (is (= {:kind :cascade-candidate
            :id :C1
            :target "M-daily-scan-multi-axis-queue"}
           (get-in adapted [:projection :runtime-source :candidate])))
    (is (not (.contains (:lean adapted) ":precedence")))
    (is (not (.contains (:lean adapted) ":interpretation-receipts")))
    (is (.contains (:lean adapted) "retained_A_is_target_local"))
    (is (.contains (:lean adapted) "retained_Q_reaches_target"))
    (is (.contains (:lean adapted) "retained_C_is_normalised"))))

(deftest aqc-certificate-refuses-identity-and-correspondence-mutations
  (let [record (read-string (slurp controlled-record))]
    (testing "selection-law identity cannot diverge from the chosen candidate"
      (is (= :adapter/aqc-candidate-identity-mismatch
             (refusal #(adapter/aqc-input-from-run-record
                        (assoc-in record
                                  [:decision :selection-law :per-policy-argmax
                                   :action :target]
                                  "M-formal-patterns"))))))
    (testing "selecting the unrelated unknown target cannot borrow C1's A/Q/C"
      (is (= :adapter/aqc-correspondence-mismatch
             (refusal #(adapter/aqc-input-from-run-record
                        (-> record
                            (assoc-in [:decision :chosen :target] "M-formal-patterns")
                            (assoc-in [:decision :selection-law :per-policy-argmax
                                       :action :target]
                                      "M-formal-patterns")
                            (assoc-in [:decision :selection-certificate :scoring 0 :id :target]
                                      "M-formal-patterns")
                            (assoc-in [:decision :selection-certificate :candidates 0 :id :target]
                                      "M-formal-patterns")))))))
    (testing "every accepted token must occur in every positive terminal state"
      (let [accepted (get-in record [:decision :selection-certificate :scoring 0
                                     :consumed-g :A :acceptance])
            missing (second (sort-by pr-str accepted))]
        (is (= :adapter/aqc-correspondence-mismatch
               (refusal #(adapter/aqc-input-from-run-record
                          (update-in record
                                     [:decision :selection-certificate :scoring 0
                                      :consumed-g :Q :steps 3 :belief]
                                     (fn [belief]
                                       (into {} (map (fn [[state probability]]
                                                       [(disj state missing) probability]))
                                             belief)))))))))
    (testing "zero preference for the retained A class breaks correspondence"
      (is (= :adapter/aqc-correspondence-mismatch
             (refusal #(adapter/aqc-input-from-run-record
                        (-> record
                            (assoc-in [:decision :selection-certificate :scoring 0
                                       :consumed-g :C :steps 3 :distribution :related] 0)
                            (assoc-in [:decision :selection-certificate :scoring 0
                                       :consumed-g :C :steps 3 :distribution :focused] 9/10)))))))))

(def selection-record
  "data/wm-runs/tick-run-record-2026-10-01-949d08f1-3eb0-4bf7-8215-f811d7352ed6.edn")
(def selection-sha "ec1d3e47b1a657adbe69f311137e8ba6c14635bad37c33e12a619f2563849b94")

(deftest controlled-record-renders-complete-singleton-selection-certificate
  (let [bytes (java.nio.file.Files/readAllBytes (.toPath (io/file selection-record)))
        adapted (adapter/adapt-run-record-selection-bytes bytes selection-sha)]
    (is (= selection-sha (:source-sha256 adapted)))
    (is (= 1 (get-in adapted [:projection :candidate-count])))
    (is (= :no-competing-policy
           (get-in adapted [:projection :comparison-status])))
    (is (= :not-supplied (get-in adapted [:projection :f-status])))
    (is (= [:habit :free-energy :G]
           (get-in adapted [:projection :contribution-order])))
    (is (.contains (:lean adapted) "selected_is_recorded_no_competing_winner"))
    (is (.contains (:lean adapted) "retained_F_and_pairwise_contributions_are_absent"))
    (is (not (.contains (:lean adapted) ":precedence")))))

(deftest selection-certificate-refuses-identity-order-and-coverage-mutations
  (let [record (read-string (slurp selection-record))]
    (testing "the recorded comparison winner cannot diverge from selection"
      (is (= :adapter/selection-candidate-identity-mismatch
             (refusal #(adapter/selection-input-from-run-record
                        (assoc-in record
                                  [:decision :selection-law :policy-comparison
                                   :winner :id :target]
                                  "M-formal-patterns"))))))
    (testing "the retained contribution order is part of the comparison law"
      (is (= :adapter/selection-law-mismatch
             (refusal #(adapter/selection-input-from-run-record
                        (assoc-in record
                                  [:decision :selection-law :policy-comparison
                                   :contribution-tie-order]
                                  [:G :free-energy :habit]))))))
    (testing "every candidate must have exactly one retained scoring row"
      (is (= :adapter/selection-candidate-coverage-mismatch
             (refusal #(adapter/selection-input-from-run-record
                        (assoc-in record
                                  [:decision :selection-certificate :scoring]
                                  {}))))))))

(deftest controlled-record-renders-p0-admission-certificate
  (let [bytes (java.nio.file.Files/readAllBytes (.toPath (io/file selection-record)))
        adapted (adapter/adapt-run-record-admission-bytes bytes selection-sha)]
    (is (= selection-sha (:source-sha256 adapted)))
    (is (= 1 (get-in adapted [:projection :admitted-count])))
    (is (= [:interpretation :construction :review-publication :admission]
           (get-in adapted [:projection :provenance-checks])))
    (is (= :missing
           (get-in adapted [:projection :typed-exclusions :acceptance :status])))
    (is (.contains (:lean adapted) "selected_full_identity_is_admitted"))
    (is (.contains (:lean adapted) "selection_field_is_exactly_admitted_field"))
    (is (not (.contains (:lean adapted) ":precedence")))))

(deftest admission-certificate-refuses-admission-and-identity-mutations
  (let [record (read-string (slurp selection-record))]
    (testing "a forbidden provenance kind reverses the recorded admission"
      (is (= :adapter/admission-check-failed
             (refusal #(adapter/admission-input-from-run-record
                        (assoc-in record
                                  [:decision :selection-certificate
                                   :candidate-derivations :C1 :admission :kind]
                                  :hand-admitted))))))
    (testing "the admitted-field key must be the exact selected candidate id"
      (let [derivation (get-in record [:decision :selection-certificate
                                       :candidate-derivations :C1])]
        (is (= :adapter/admission-field-identity-mismatch
               (refusal #(adapter/admission-input-from-run-record
                          (-> record
                              (update-in [:decision :selection-certificate
                                          :candidate-derivations]
                                         dissoc :C1)
                              (assoc-in [:decision :selection-certificate
                                         :candidate-derivations :C2]
                                        derivation))))))))))

(def enactment-grounding-record
  "data/wm-runs/tick-run-record-2026-10-01-5a07d49b-4074-4372-958a-828c160513d3.edn")
(def enactment-grounding-sha
  "ef65928e8bd237cf1a26599bb9cb6ef78e2ae1186668f7c2104b88f08ad51a98")

(deftest controlled-record-renders-enactment-grounding-certificate
  (let [bytes (java.nio.file.Files/readAllBytes (.toPath (io/file enactment-grounding-record)))
        adapted (adapter/adapt-run-record-enactment-grounding-bytes
                 bytes enactment-grounding-sha)]
    (is (= enactment-grounding-sha (:source-sha256 adapted)))
    (is (= (get-in adapted [:projection :selected-action-sha256])
           (get-in adapted [:projection :enacted-action-sha256])))
    (is (= "0d91cc768f2a5b807c8c30edda7a131fbd2c6d52"
           (get-in adapted [:projection :commit])))
    (is (= "zai-1" (get-in adapted [:projection :reviewer])))
    (is (= :held (get-in adapted [:projection :precision-state-status])))
    (is (= :precision-model-changed
           (get-in adapted [:projection :precision-state-reason])))
    (is (= (get-in adapted [:projection :precision-beta])
           (get-in adapted [:projection :precision-initialized-beta])))
    (is (false? (get-in adapted [:projection :precision-subreceipt-admissible])))
    (is (false? (get-in adapted [:projection :token-observation-subreceipt-admissible])))
    (is (.contains (:lean adapted) "selected_enacted_exact"))
    (is (.contains (:lean adapted) "admitted_grounded_unique_terminal_no_failure"))
    (is (.contains (:lean adapted) "refused_subreceipts_are_not_admissible"))
    (is (.contains (:lean adapted) "retained_precision_is_held_with_unchanged_beta"))
    (is (not (.contains (:lean adapted) "tokenObservationConsumed")))
    (is (not (.contains (:lean adapted) ":precedence")))))

(deftest enactment-grounding-certificate-refuses-runtime-mutations
  (let [record (read-string (slurp enactment-grounding-record))]
    (testing "selected and enacted canonical digests must remain equal"
      (is (= :adapter/enactment-identity-mismatch
             (refusal #(adapter/enactment-grounding-input-from-run-record
                        (assoc-in record
                                  [:d-task-enactment :verification
                                   :candidate-to-minted-join :enacted-action-sha256]
                                  (apply str (repeat 64 "0"))))))))
    (testing "grounded, terminal and revision commits must agree"
      (is (= :adapter/grounded-commit-mismatch
             (refusal #(adapter/enactment-grounding-input-from-run-record
                        (assoc-in record [:terminal-receipt :commit :sha]
                                  (apply str (repeat 40 "0"))))))))
    (testing "the reviewer-of-record must be the terminal reviewer"
      (is (= :adapter/reviewer-mismatch
             (refusal #(adapter/enactment-grounding-input-from-run-record
                        (assoc-in record [:terminal-receipt :reviewer] "codex-11"))))))
    (testing "a refused precision subreceipt cannot become unverified success"
      (is (= :adapter/refused-subreceipt-mismatch
             (refusal #(adapter/enactment-grounding-input-from-run-record
                        (assoc-in record
                                  [:d-task-enactment :verification
                                   :precision-verification]
                                  {:status :verified}))))))
    (testing "a refused token-observation subreceipt cannot become success"
      (is (= :adapter/refused-subreceipt-mismatch
             (refusal #(adapter/enactment-grounding-input-from-run-record
                        (assoc-in record
                                  [:d-task-enactment :verification
                                   :token-observation-verification]
                                  {:status :verified}))))))
    (testing "measured false is accepted without changing the Lean projection"
      (let [old-projection (adapter/enactment-grounding-input-from-run-record record)
            measured-record
            (assoc-in record
                      [:d-task-enactment :verification :token-observation-verification]
                      {:status :refused :kind :after-token-not-observed
                       :measured-token-count 4})
            measured-projection
            (adapter/enactment-grounding-input-from-run-record measured-record)]
        (is (= old-projection measured-projection))
        (is (java.util.Arrays/equals
             (.getBytes (adapter/render-enactment-grounding old-projection "same")
                        java.nio.charset.StandardCharsets/UTF_8)
             (.getBytes (adapter/render-enactment-grounding measured-projection "same")
                        java.nio.charset.StandardCharsets/UTF_8)))))
    (testing "missing Boolean evidence keeps the existing accepted diagnostic"
      (is (map? (adapter/enactment-grounding-input-from-run-record record))))
    (testing "unrelated and malformed diagnostics still refuse"
      (doseq [diagnostic [{:status :refused :kind :something-else}
                          {:status :refused :kind :after-token-not-observed
                           :measured-token-count 0}
                          {:status :refused :kind :after-token-not-observed
                           :measured-token-count 4 :forged true}]]
        (is (= :adapter/refused-subreceipt-mismatch
               (refusal #(adapter/enactment-grounding-input-from-run-record
                          (assoc-in record
                                    [:d-task-enactment :verification
                                     :token-observation-verification]
                                    diagnostic)))))))
    (testing "changed retained beta refuses"
      (is (= :adapter/precision-state-mismatch
             (refusal #(adapter/enactment-grounding-input-from-run-record
                        (assoc-in record
                                  [:decision :selection-certificate
                                   :policy-precision-state :beta]
                                  2))))))
    (testing "an updated retained precision state refuses"
      (is (= :adapter/precision-state-mismatch
             (refusal #(adapter/enactment-grounding-input-from-run-record
                        (assoc-in record
                                  [:decision :selection-certificate
                                   :policy-precision-state :status]
                                  :updated))))))))
