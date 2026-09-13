(require '[futon2.aif.interoceptive-manifest :as manifest])

(def output-path
  "holes/labs/wm-contract/runs/row-18-production-manifest-2026-09-13/production-manifest-output.edn")

(try
  (prn {:unexpected-success (manifest/production-manifest!)})
  (System/exit 2)
  (catch clojure.lang.ExceptionInfo e
    (let [data (ex-data e)]
      (spit output-path (str (pr-str data) "\n"))
      (prn {:refusal (:refusal data)
            :manifest-count (count (get-in data [:manifest :manifest]))
            :output output-path})
      (if (= :interoceptive/runtime-qualification-unavailable (:refusal data))
        (System/exit 0)
        (System/exit 1)))))
