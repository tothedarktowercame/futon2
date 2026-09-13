(require '[futon2.aif.interoceptive-manifest :as manifest])

(try
  (prn {:unexpected-success (manifest/production-manifest!)})
  (System/exit 2)
  (catch clojure.lang.ExceptionInfo e
    (let [data (ex-data e)]
      (prn data)
      (if (= :interoceptive/runtime-qualification-unavailable (:refusal data))
        (System/exit 0)
        (System/exit 1)))))
