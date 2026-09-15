(require '[cheshire.core :as json]
         '[futon2.aif.efe :as efe]
         '[futon2.aif.preferences :as pref])
;; Synthetic one-channel projection of the common first-step outcome x=1.
;; Execute the existing components, not a claim to reproduce a historical tick.
(let [q {:kind :gaussian :mu 1.0 :sigma2 0.01}
      c (pref/c-distribution [0.9 1.0] :temperature 0.1)
      risk (pref/kl q c)
      ambiguity (#'efe/ambiguity {:x 0.01} :gaussian-entropy)]
  (println (json/generate-string
            {:scope "synthetic old-regime components, not a whole controller replay"
             :q q :c c :risk risk :ambiguity ambiguity
             :risk-plus-ambiguity (+ risk ambiguity)
             :other-controller-terms "not computed; not asserted zero"
             :same-for ["bad4" "good4" "good6"]})))
