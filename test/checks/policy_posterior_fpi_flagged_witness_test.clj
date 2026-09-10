(ns checks.policy-posterior-fpi-flagged-witness-test
  (:require [checks.policy-posterior-fpi-flagged-witness :as w]
            [clojure.test :refer [deftest is]]))
(def text (slurp w/fixture))
(deftest frozen-positive-and-negatives
  (is (:pass? (w/run text nil)))
  (doseq [mode ["--negative-remove-f" "--negative-misjoin"]]
    (let [result (w/run text mode)]
      (is (false? (:pass? result)))
      (is (:mutation-changed? result))
      (is (= :eligible-record-lacks-identity-joined-candidate-f (:reason result))))))
(deftest malformed-and-trailing-record-refuse
  (is (thrown? Exception (w/read-forms "{:x 1")))
  (is (thrown? Exception (w/read-forms "{:x 1}\n{:bad"))))
(deftest claimed-applied-missing-f-and-altered-identity-refuse
  (let [xs (w/read-forms text)]
    (is (false? (:pass? (w/validate-records (w/mutate xs "--negative-remove-f")))))
    (is (false? (:pass? (w/validate-records (w/mutate xs "--negative-misjoin")))))))
(deftest incomplete-coverage-is-excluded
  (let [r (w/run text nil)]
    (is (= 4 (:denominator r)))
    (is (= 3 (:eligible-positive-count r)))
    (is (= :incomplete-coverage (get-in r [:records 1 :reason])))
    (is (false? (get-in r [:records 1 :eligible?])))))

(deftest baseline-drift-cannot-masquerade-as-mutation-detection
  (let [source-drift (with-redefs [w/source-pins
                                   {"src/futon2/aif/policy.clj" "planted-stale-pin"}]
                       (w/run text "--negative-remove-f"))
        fixture-drift (with-redefs [w/fixture-sha "planted-stale-pin"]
                        (w/run text "--negative-remove-f"))]
    (is (= :source-digest-mismatch (:reason source-drift)))
    (is (not (:mutation-changed? source-drift)))
    (is (= :fixture-digest-mismatch (:reason fixture-drift)))
    (is (not (:mutation-changed? fixture-drift)))))

(deftest unknown-control-and-non-bijective-or-nonfinite-payload-refuse
  (is (= :unknown-negative-control (:reason (w/run text "--negative-unknown"))))
  (let [xs (w/read-forms text)
        duplicate (assoc-in xs [0 :ranked-actions 1 :action]
                            (get-in xs [0 :ranked-actions 0 :action]))
        infinite (assoc-in xs [0 :f-pi-by-candidate-id :by-candidate-id "rank/1" :value]
                           Double/POSITIVE_INFINITY)]
    (is (false? (:pass? (w/validate-records duplicate))))
    (is (false? (:pass? (w/validate-records infinite))))))
