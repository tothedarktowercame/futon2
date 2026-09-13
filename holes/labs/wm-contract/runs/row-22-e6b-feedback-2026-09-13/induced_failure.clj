(require '[clojure.test :as t])
(t/deftest deliberate-ledger-sensitivity-failure
  (t/is (= :exactly-once :duplicate-feedback) "induced E6b ledger failure"))
(let [{:keys [fail error]} (t/run-tests 'user)]
  (System/exit (if (zero? (+ fail error)) 0 1)))
