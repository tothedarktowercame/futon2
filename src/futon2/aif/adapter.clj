(ns futon2.aif.adapter
  "Adapter protocol for domain-specific AIF integration.")

(defprotocol AifAdapter
  (select-pattern [adapter state context]
    "Return a PSR-like selection map for the current context.")
  (update-beliefs [adapter state observation]
    "Return an updated state and PUR-like outcome summary."))

(defn state-update [result]
  (:aif/state result))
