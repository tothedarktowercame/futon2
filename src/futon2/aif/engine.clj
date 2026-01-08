(ns futon2.aif.engine
  "Domain-agnostic AIF engine wrapper."
  (:require [futon2.aif.adapter :as adapter]))

(defn new-engine
  ([adapter] (new-engine adapter {}))
  ([adapter initial-state]
   {:adapter adapter
    :state (atom initial-state)}))

(defn select-pattern [engine context]
  (adapter/select-pattern (:adapter engine) @(:state engine) context))

(defn update-beliefs [engine observation]
  (let [result (adapter/update-beliefs (:adapter engine) @(:state engine) observation)
        update (adapter/state-update result)]
    (when (map? update)
      (swap! (:state engine) merge update))
    result))
