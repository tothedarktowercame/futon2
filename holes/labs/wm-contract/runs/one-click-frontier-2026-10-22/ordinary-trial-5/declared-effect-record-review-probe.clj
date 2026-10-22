(require '[futon2.aif.declared-effect-record :as d] '[clojure.edn :as edn])
(let [path "resources/wm/cascade-sources/T-repair-occ-444fb018.edn"
      target (:target (edn/read-string (slurp path)))
      inputs {:target target :declaration {:path path} :base-revision {:repo "wrong-repo" :sha "not-a-revision"}
              :observe (fn [_] {:observed false})}
      r (d/build-record inputs)
      forged {:schema :bogus :target "forged" :admission {:eligible true}}
      forged (assoc forged :record/digest (d/record-digest forged))
      tampered (assoc r :target "different-target")]
 (prn {:unproven-observation-admission (:admission r)
       :unproven-base (:base-revision r)
       :forged-schema-verification (d/verify-record forged)
       :tampered-reconstruction (d/reconstruct inputs tampered)}))
(shutdown-agents)
