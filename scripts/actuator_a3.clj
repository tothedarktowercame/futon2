(ns actuator-a3
  (:require [futon2.aif.actuator-a3 :as a3]))

(defn -main [& args]
  (let [opts (a3/parse-args args)
        result (a3/run-a3! opts)]
    (case (:mode result)
      :dry-run
      (doseq [pkg (:packages result)]
        (println (a3/render-package pkg)))

      :dispatch
      (do
        (println (pr-str (select-keys result [:mode :dispatches])))
        (when (some #(and (contains? % :exit) (not= 0 (:exit %))) (:dispatches result))
          (System/exit 1))))
    (System/exit 0)))

(apply -main *command-line-args*)
