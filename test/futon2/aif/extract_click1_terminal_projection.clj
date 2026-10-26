(ns futon2.aif.extract-click1-terminal-projection
  "One-shot extractor for the retained reading-era click-1 terminal record."
  (:require [clojure.edn :as edn]
            [clojure.pprint :as pprint]))

(let [record (edn/read-string (slurp (first *command-line-args*)))
      projection (cond->
                   {:run/id (:run/id record)
                    :click/id (:click/id record)
                    :terminal-receipt
                    (select-keys (:terminal-receipt record) [:outcome])
                    :failure (:failure record)
                    :traceWritten (:traceWritten record)
                    :run-output (select-keys (:run-output record) [:outcome])}
                   (contains? record :kind) (assoc :kind (:kind record)))]
  (pprint/pprint projection))
