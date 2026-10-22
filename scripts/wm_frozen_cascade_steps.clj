(ns wm-frozen-cascade-steps
  (:require [futon2.aif.wm.frozen-cascade-steps :as steps]))
(let [[command & args] *command-line-args*
      result (case command
               "init" (apply steps/initialize! args)
               "next" (apply steps/advance! args)
               "fork" (apply steps/fork! args)
               "status" (mapv #(select-keys % [:stage :digest :predecessor])
                              (steps/history (first args)))
               (throw (ex-info "Usage: init SELECTED.gz COMPILED.gz ROOT | next ROOT | fork SOURCE STAGE TARGET | status ROOT" {})))]
  (prn result)
  (shutdown-agents)
  (when (= :failed (:status result)) (System/exit 1)))
