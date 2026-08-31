(ns obligation-ledger-reconciliation-check-test
  (:require [checks.obligation-ledger-reconciliation-check :as check]
            [clojure.test :refer [deftest is]]))

(deftest current-table-agrees-with-dated-closure-record
  (let [report (check/run-check {})]
    (is (true? (get-in report [:summary :pass?])))
    (is (= 24 (get-in report [:summary :obligations])))
    (is (= 24 (get-in report [:summary :closure-verified])))))

(deftest stale-current-row-is-a-demonstrated-falsifier
  (let [report (check/run-check {:negative? true})]
    (is (false? (get-in report [:summary :pass?])))
    (is (some #(= :current-status (:check %)) (:checks report)))))
