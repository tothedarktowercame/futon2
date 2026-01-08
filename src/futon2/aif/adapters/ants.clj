(ns futon2.aif.adapters.ants
  "Adapter stub for ants AIF integration."
  (:require [futon2.aif.adapter :as adapter]))

(defrecord AntsAdapter []
  adapter/AifAdapter
  (select-pattern [_ _state context]
    {:decision/id (:decision/id context)
     :candidates []
     :chosen nil
     :aif {:note "ants adapter not wired"}})
  (update-beliefs [_ _state observation]
    {:aif/state {}
     :aif {:note "ants adapter not wired"
           :observation observation}}))

(defn new-adapter []
  (->AntsAdapter))
