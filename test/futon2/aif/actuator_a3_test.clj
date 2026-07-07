(ns futon2.aif.actuator-a3-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [futon2.aif.actuator-a3 :as a3]))

(deftest extracts-all-a3-corpus-deposits
  (let [by-id (a3/deposits-by-id)
        packages (mapv #(a3/extract-build-package (get by-id %)) a3/a3-corpus-ids)]
    (is (= 4 (count packages)))
    (is (every? :ok? packages)
        (pr-str (map #(select-keys % [:fold-turn/id :missing]) packages)))
    (is (every? #(seq (:policy-holes %)) packages))
    (is (every? #(get-in % [:structure-spec :wiring :nodes]) packages))
    (is (= a3/a3-corpus-ids (mapv :fold-turn/id packages)))))

(deftest reports-spec-quality-when-typespec-or-structure-missing
  (testing "missing typespec is reported, not silently skipped"
    (let [pkg (a3/extract-build-package
               {:fold-turn/id "ft-weak"
                :mission "futonx-d/mission/weak"
                :wiring {:nodes [{:id :b1}]
                         :terminals [{:id :b1 :discharges :want-signature-missing}]}
                :turn {:answer {:policy-holes []}}})]
      (is (false? (:ok? pkg)))
      (is (= :missing-typespec (-> pkg :missing first :reason)))))
  (testing "missing structure is reported, not silently skipped"
    (let [pkg (a3/extract-build-package
               {:fold-turn/id "ft-weak"
                :mission "futonx-d/mission/weak"
                :arrow-candidate {:have "h" :want "w"}
                :turn {:answer {:policy-holes []}}})]
      (is (false? (:ok? pkg)))
      (is (= :missing-structure (-> pkg :missing first :reason))))))

(deftest witness-gate-rejects-no-or-unresolved-evidence
  (let [base {:before-open-hole-count 3
              :after-open-hole-count 2}]
    (testing "no evidence-ref rejects and dial does not move"
      (let [r (a3/verify-builder-result
               (assoc base :closures [{:hole-id :h1}])
               {:resolver (constantly true)})]
        (is (false? (-> r :closures first :closure/accepted?)))
        (is (= :missing-evidence-ref (-> r :closures first :closure/reject-reason)))
        (is (false? (:dial-moved? r)))
        (is (zero? (:dial-counted-closures r)))))
    (testing "unresolved evidence-ref rejects and dial does not move"
      (let [r (a3/verify-builder-result
               (assoc base :closures [{:hole-id :h1
                                        :evidence-ref {:kind :file-exists
                                                       :path "/tmp/no-such-a3-witness"}}])
               {:resolver (constantly false)})]
        (is (false? (-> r :closures first :closure/accepted?)))
        (is (= :unresolved-evidence-ref (-> r :closures first :closure/reject-reason)))
        (is (false? (:dial-moved? r)))))))

(deftest resolving-witness-accepts-and-counts-dial-decrement
  (let [r (a3/verify-builder-result
           {:closures [{:hole-id :h1
                        :evidence-ref {:kind :file-exists :path "/tmp/fake"}}]
            :before-open-hole-count 3
            :after-open-hole-count 2}
           {:resolver (constantly true)})]
    (is (true? (-> r :closures first :closure/accepted?)))
    (is (true? (:dial-moved? r)))
    (is (= 1 (:dial-counted-closures r)))))

(deftest dry-run-over-corpus-is-deterministic
  (let [a (with-out-str
            (doseq [pkg (:packages (a3/run-a3! {:dry-run? true :all-corpus? true}))]
              (println (a3/render-package pkg))))
        b (with-out-str
            (doseq [pkg (:packages (a3/run-a3! {:dry-run? true :all-corpus? true}))]
              (println (a3/render-package pkg))))]
    (is (= a b))
    (is (str/includes? a "ft-learning-loop-010"))
    (is (str/includes? a "ft-pattern-mining-011"))))
