(require '[clojure.test :as t])
(t/deftest deliberate-runner-failure
  (t/is (= :complete :dropped-candidate) "induced E4 coverage failure"))
(let [{:keys [fail error]} (t/run-tests 'user)]
  (System/exit (if (zero? (+ fail error)) 0 1)))
