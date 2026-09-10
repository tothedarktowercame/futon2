(ns checks.policy-posterior-fpi-flagged-witness-test
  (:require [checks.policy-posterior-fpi-flagged-witness :as w]
            [clojure.test :refer [deftest is]]))
(def text (slurp w/fixture))
(deftest frozen-positive-and-negatives
  (is (:pass? (w/run text nil)))
  (is (false? (:pass? (w/run text "--negative-remove-f"))))
  (is (false? (:pass? (w/run text "--negative-misjoin")))))
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
