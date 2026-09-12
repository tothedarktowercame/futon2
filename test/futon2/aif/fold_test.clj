(ns futon2.aif.fold-test
  "The fold interface contract (E-close-the-loop exit-1)."
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.fold :as fold]))

(deftest valid-fold-output
  (testing "a well-formed fold output satisfies the contract"
    (is (fold/valid-fold-output? {:wiring {:id :w} :coverage-score-delta -0.4 :policy-holes []}))
    (is (fold/valid-fold-output? {:wiring {:id :w} :coverage-score-delta nil :policy-holes [{:unfolded :p}]})
        "nil ΔG is valid — it means the gate abstains, not that the output is malformed"))
  (testing "malformed outputs are rejected"
    (is (not (fold/valid-fold-output? {:coverage-score-delta -0.4 :policy-holes []})) "missing :wiring")
    (is (not (fold/valid-fold-output? {:wiring {} :coverage-score-delta "x" :policy-holes []})) ":coverage-score-delta not a number/nil")
    (is (not (fold/valid-fold-output? {:wiring {} :coverage-score-delta -0.4 :policy-holes {}})) ":policy-holes not sequential")))

(deftest coverage-score-leg-and-closes
  (testing "coverage-score-leg extracts ΔG; nil ⇒ the gate abstains"
    (is (= -0.4 (fold/coverage-score-leg {:wiring {} :coverage-score-delta -0.4 :policy-holes []})))
    (is (nil? (fold/coverage-score-leg {:wiring {} :coverage-score-delta nil :policy-holes []}))))
  (testing "closes? iff ΔG present and descending (negative)"
    (is (true?  (fold/closes? {:wiring {} :coverage-score-delta -0.4 :policy-holes []})))
    (is (false? (fold/closes? {:wiring {} :coverage-score-delta 0.4 :policy-holes []})) "non-descending ⇒ not closing")
    (is (false? (fold/closes? {:wiring {} :coverage-score-delta nil :policy-holes []})) "nil ⇒ abstain, not closing")))

(def enriched-output
  {:wiring
   {:boxes
    [{:id :pattern-step
      :fits-pattern {:pattern/id :p/a :pattern/revision "rev-1"}
      :warrant-kind :pattern
      :conditions [{:condition "premise A" :status :established
                    :witness {:receipt/id "r-1"}}
                   {:condition "premise B" :status :absent
                    :obstruction {:evidence/id "e-1"}}]}
     {:id :ordinary-step
      :fits-pattern {:pattern/id :p/a :pattern/revision "rev-1"}
      :warrant-kind :deduction :conditions []
      :hole {:obligation/id :obligation/inside :kind :sorry
             :wanted "the remaining proof"}}]
    :wires [[:pattern-step :ordinary-step]] :terminals [:ordinary-step]}
   :coverage-score-delta -0.5
   :policy-holes [{:obligation/id :obligation/outside
                   :unfolded-pattern nil :free "unknown move" :why "open"}]})

(deftest structured-proof-fold-output-v1
  (is (fold/valid-fold-output-v1? enriched-output))
  (is (not (fold/valid-fold-output-v1? nil)))
  (let [refusal (fold/validate-fold-output-v1
                 {:fold/refused true :why "premise unavailable"
                  :refusal/class :missing-premise})]
    (is (:ok refusal))
    (is (:fold/exceptional? refusal))
    (is (= :refusal (:fold/schema refusal))))
  (doseq [[mutation finding]
          [[#(update-in % [:wiring :boxes 0 :fits-pattern]
                       dissoc :pattern/revision)
            :box-pattern-revision-missing]
           [#(assoc-in % [:wiring :boxes 0 :warrant-kind] :guess)
            :box-warrant-kind-invalid]
           [#(update-in % [:wiring :boxes 0 :conditions 0] dissoc :status)
            :condition-status-missing]]]
    (let [result (fold/validate-fold-output-v1 (mutation enriched-output))]
      (is (not (:ok result)))
      (is (some #(= finding (:finding %)) (:findings result))))))

(deftest recorded-fold-turn-is-a-pinned-legacy-shape
  (let [path "holes/labs/M-evaluate-policies/exhibit/fold-turn.edn"
        construction (edn/read-string (slurp path))
        output {:wiring construction :coverage-score-delta -0.75
                :policy-holes (:policy-holes construction)}
        enriched (fold/validate-fold-output-v1 output)
        findings (set (map :finding (:findings enriched)))]
    (is (fold/valid-fold-output? output))
    (is (not (:ok enriched)))
    (is (contains? findings :box-pattern-reference-invalid))
    (is (contains? findings :box-warrant-kind-invalid))
    (is (contains? findings :condition-status-missing))
    (is (contains? findings :policy-hole-obligation-id-missing))
    (is (contains? findings :box-hole-obligation-id-missing))))

(deftest hole-interface-validation
  (testing "a bare gap marker is not a hole interface"
    (let [result (fold/validate-fold-output-v1
                  (assoc-in enriched-output [:wiring :boxes 1 :hole]
                            {:obligation/id :obligation/bare}))
          findings (set (map :finding (:findings result)))]
      (is (not (:ok result)))
      (is (contains? findings :hole-kind-missing))
      (is (contains? findings :hole-wanted-missing))))
  (testing "policy holes state the free work, grounds, and pattern position"
    (let [result (fold/validate-fold-output-v1
                  (assoc enriched-output :policy-holes
                         [{:obligation/id :obligation/statusless}]))
          findings (set (map :finding (:findings result)))]
      (is (contains? findings :policy-hole-free-missing))
      (is (contains? findings :policy-hole-why-missing))
      (is (contains? findings :policy-hole-unfolded-pattern-missing)))))

(deftest recorded-fold-turn-holes-retain-real-interface-shape
  (let [construction (edn/read-string
                      (slurp "holes/labs/M-evaluate-policies/exhibit/fold-turn.edn"))
        output {:wiring construction :coverage-score-delta -0.75
                :policy-holes (:policy-holes construction)}
        findings (:findings (fold/validate-fold-output-v1 output))
        hole-findings (set (map :finding findings))]
    (is (not (contains? hole-findings :hole-kind-missing)))
    (is (not (contains? hole-findings :hole-wanted-missing)))
    (is (not (contains? hole-findings :hole-discharge-invalid)))
    (is (not (contains? hole-findings :hole-satiety-invalid)))
    (is (not (contains? hole-findings :policy-hole-free-missing)))
    (is (not (contains? hole-findings :policy-hole-why-missing)))
    (is (not (contains? hole-findings :policy-hole-unfolded-pattern-missing)))
    (is (= 2 (count (filter #(= :box-hole-obligation-id-missing (:finding %))
                            findings))))
    (is (= 2 (count (filter #(= :policy-hole-obligation-id-missing (:finding %))
                            findings))))))

(deftest fold-output-corresponds-to-cascade-outline
  (let [construction (edn/read-string
                      (slurp "holes/labs/M-evaluate-policies/exhibit/fold-turn.edn"))
        output {:wiring construction :policy-holes (:policy-holes construction)}]
    (is (:ok (fold/validate-fold-correspondence output (:cascade construction)))))
  (testing "cascade debts and outside non-deduction warrants are findings"
    (let [result (fold/validate-fold-correspondence enriched-output [:p/missing])
          findings (set (map :finding (:findings result)))]
      (is (contains? findings :cascade-pattern-unaccounted))
      (is (contains? findings :box-warrant-outside-cascade))))
  (testing "ordinary deductions need not name a cascade pattern"
    (let [deduction-output
          (-> enriched-output
              (assoc-in [:wiring :boxes 0 :fits-pattern :pattern/id] :p/a)
              (assoc-in [:wiring :boxes 1 :fits-pattern :pattern/id] :proof/local))]
      (is (:ok (fold/validate-fold-correspondence deduction-output [:p/a]))))))
