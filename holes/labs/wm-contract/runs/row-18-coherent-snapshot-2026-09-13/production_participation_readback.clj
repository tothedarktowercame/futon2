(require '[futon2.aif.interoceptive-manifest :as manifest])

(try
  (prn {:unexpected-success (manifest/production-manifest!)})
  (System/exit 2)
  (catch clojure.lang.ExceptionInfo e
    (let [data (ex-data e)]
      (prn {:ok false :refusal (:refusal data) :data data})
      (System/exit
       (if (= :interoceptive/activation-lease-unavailable (:refusal data)) 0 1)))))
