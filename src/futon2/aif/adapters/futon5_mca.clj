(ns futon2.aif.adapters.futon5-mca
  "Adapter stub for Futon5 meta-cellular automata search."
  (:require [futon2.aif.adapter :as adapter]))

(defrecord Futon5McaAdapter []
  adapter/AifAdapter
  (select-pattern [_ _state context]
    {:decision/id (:decision/id context)
     :candidates (:candidates context)
     :chosen (:chosen context)
     :aif {:note "futon5 MCA adapter not wired"}})
  (update-beliefs [_ _state observation]
    {:aif/state {}
     :aif {:note "futon5 MCA adapter not wired"
           :observation observation}}))

(defn new-adapter []
  (->Futon5McaAdapter))
