(require '[futon2.aif.interoceptive-activation :as activation])

(try
  (prn {:unexpected-success (activation/production-controller-status!)})
  (System/exit 2)
  (catch clojure.lang.ExceptionInfo e
    (let [data (ex-data e)]
      (prn {:ok false :refusal (:refusal data) :data data})
      (System/exit
       (if (= :interoceptive/activation-controller-unavailable (:refusal data)) 0 1)))))
