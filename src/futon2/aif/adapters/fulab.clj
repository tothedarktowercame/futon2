(ns futon2.aif.adapters.fulab
  "Adapter stub for fulab (fucodex/fuclaude) AIF integration."
  (:require [futon2.aif.adapter :as adapter]))

(defrecord FulabAdapter []
  adapter/AifAdapter
  (select-pattern [_ _state context]
    {:decision/id (:decision/id context)
     :candidates (:candidates context)
     :chosen (:chosen context)
     :aif {:note "fulab adapter not wired"}})
  (update-beliefs [_ _state observation]
    {:aif/state {}
     :aif {:note "fulab adapter not wired"
           :observation observation}}))

(defn new-adapter []
  (->FulabAdapter))
