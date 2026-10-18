(ns futon2.aif.wm.recorded-tick-evaluation
  "Conservative tick evaluation for recorded flights. Verified cascade effects
  are necessary but not sufficient: production carries a tick only through a
  published full-loop trace record bound to the source run."
  (:require [futon2.aif.wm.execution-journal :as journal]))

(defn evaluate
  [{:keys [source-run-id journal-validation journal-replay
           production-close-record]}]
  (cond
    (not (journal/certified? journal-validation))
    {:status :error :kind :tick-journal-not-certified}

    (empty? (:enacted journal-replay))
    {:status :error :kind :tick-no-verified-enactment}

    (nil? production-close-record)
    {:status :open :kind :production-close-evidence-missing
     :source-run-id source-run-id
     :enacted-count (count (:enacted journal-replay))}

    (not= source-run-id (:run/id production-close-record))
    {:status :error :kind :production-close-run-mismatch
     :expected source-run-id :observed (:run/id production-close-record)}

    (not (:trace-written? production-close-record))
    {:status :error :kind :production-trace-not-written}

    :else
    {:status :carried :ticks-carried 1
     :source-run-id source-run-id
     :enacted-count (count (:enacted journal-replay))}))
