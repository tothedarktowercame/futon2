(ns futon2.aif.cascade-fold-repair-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.fold :as fold]
            [futon2.aif.fold-classical :as classical]
            [futon2.aif.full-loop-runner :as runner]))

(defn- recorded-entry [name]
  (:selected-entry
   (edn/read-string
    (slurp (str "test/fixtures/cascade-fold-repair/" name ".edn")))))

(defn- demonstrate [name expected-count]
  (let [entry (recorded-entry name)
        action (:action entry)
        construction (runner/construct-for-decision entry)
        raw (classical/classical-fold (:shown construction) construction)
        result (runner/construction-wiring-result construction)
        output (:fold-output result)
        holes (:policy-holes output)
        outputs (set (mapcat :produces (:precedence action)))]
    ;; The real dependency constructs the exact malformed case, not a stub.
    (is (= expected-count (count (:policy-holes raw))))
    (is (false? (:ok (fold/validate-fold-output-v1 raw))))
    (is (= #{:policy-hole-free-missing :policy-hole-why-missing
             :policy-hole-obligation-id-missing}
           (set (map :finding (:findings (fold/validate-fold-output-v1 raw))))))
    (is (= (:target action) (:mission construction)))
    (is (= :wired (:status result)))
    (is (true? (:ok (fold/validate-fold-output-v1 output))))
    (is (true? (:ok (fold/validate-fold-correspondence output (:shown construction)))))
    (is (= expected-count (count holes)))
    (is (= (mapv :unfolded-pattern (:policy-holes raw))
           (mapv :unfolded-pattern holes)))
    (is (= outputs (set (map :obligation/token holes))))
    (doseq [hole holes]
      (is (= (:obligation/token hole) (edn/read-string (:obligation/id hole))))
      (is (= (:target action) (first (:obligation/token hole))))
      (is (= (get (:interpretation-receipts action)
                  (get-in hole [:obligation/source :pattern/id]))
             (get-in hole [:obligation/source :interpretation-receipt])))
      (is (= (:why hole)
             (get-in hole [:obligation/source :interpretation-receipt :reading]))))
    ;; Schema validity must not be described as construction/discharge success.
    (is (= (get-in raw [:wiring :boxes]) (get-in output [:wiring :boxes])))
    (is (empty? (get-in output [:wiring :boxes])))
    (is (nil? (:coverage-score-delta output)))
    (is (false? (fold/closes? output)))
    (prn {:demonstration name :mission (:mission construction)
          :shape-valid (get-in result [:shape-validation :ok])
          :correspondence-valid (get-in result [:correspondence-validation :ok])
          :boxes (count (get-in output [:wiring :boxes]))
          :policy-holes (count holes)
          :obligation-tokens (mapv :obligation/token holes)
          :coverage-score-delta (:coverage-score-delta output)
          :disposition :contract-valid-incomplete})))

(deftest recorded-compliance-through-production-fold
  (demonstrate "compliance" 1))

(deftest recorded-expressions-through-production-fold
  (demonstrate "expressions-of-interest" 3))

(deftest missing-grounded-context-still-fails-the-production-validator
  (let [entry (recorded-entry "expressions-of-interest")]
    (doseq [[label bad-entry]
            [[:missing-target (update entry :action dissoc :target)]
             [:missing-interpretation (assoc-in entry [:action :interpretation-receipts] {})]
             [:wrong-target-outputs
              (update-in entry [:action :precedence]
                         #(mapv (fn [p] (assoc p :produces #{["M-other" :wrong-output]})) %))]]]
      (testing (name label)
        (let [result (runner/construction-wiring-result
                      (runner/construct-for-decision bad-entry))]
          (is (= :invalid (:status result)))
          (is (= :fold-output-invalid (:failure-kind result)))
          (is (some #(= :policy-hole-obligation-id-missing (:finding %))
                    (:findings result)))
          (is (= 3 (count (get-in result [:fold-output :policy-holes]))))
          (prn {:demonstration :negative-control :case label
                :status (:status result) :findings (:findings result)}))))
    (testing "an injected malformed fold is not enriched"
      (let [construction (runner/construct-for-decision entry)
            raw (classical/classical-fold (:shown construction) construction)
            result (runner/construction-wiring-result construction (constantly raw))]
        (is (= raw (:fold-output result)))
        (is (= :invalid (:status result)))))))
