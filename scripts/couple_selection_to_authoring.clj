(ns couple-selection-to-authoring
  (:require [futon2.aif.selection-authoring-coupling :as coupling]))

(defn -main
  [& args]
  (let [opts (coupling/parse-args args)
        result (coupling/run-once! opts)]
    (println (coupling/stable-summary result))
    (when-let [cmd (:invoke-cmd result)]
      (println "invoke-cmd=" (pr-str cmd)))
    (when (contains? result :invoke-exit)
      (println "invoke-exit=" (:invoke-exit result)))
    (when-let [out (:invoke-out result)]
      (when (seq out)
        (println "invoke-output:")
        (print out)))
    (when (and (= :fired (:status result))
               (not= 0 (:invoke-exit result)))
      (System/exit (:invoke-exit result)))))

(apply -main *command-line-args*)
