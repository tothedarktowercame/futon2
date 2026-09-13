(require '[clojure.test :refer [deftest is run-tests]])
(deftest deliberate-store-sensitivity-failure
  (is (= :atomic-joint-publication :split-state-and-ledger)))
(let [r (run-tests)]
  (System/exit (if (pos? (+ (:fail r) (:error r))) 1 0)))
