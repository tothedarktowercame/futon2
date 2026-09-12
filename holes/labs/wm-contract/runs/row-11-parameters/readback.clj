(require '[futon2.aif.machine-parameters :as parameters]
         '[futon2.aif.machine-parameters-test :as fixture])

(def informative-state (fixture/state :spawned))
(def noninformative-state (fixture/state :strengthened))
(def informative (parameters/parameter-kernels
                  fixture/model (assoc fixture/parameter-state :state-distribution informative-state)
                  fixture/policies fixture/outcomes))
(def noninformative (parameters/parameter-kernels
                     fixture/model (assoc fixture/parameter-state :state-distribution noninformative-state)
                     fixture/policies fixture/outcomes))
(def h1-outcome (first (for [[o m] (get-in fixture/h1 [:likelihood :rows :spawned]) :when (= 1 m)] o)))
(def h2-outcome (first (for [[o m] (get-in fixture/h2 [:likelihood :rows :spawned]) :when (= 1 m)] o)))
(def common-outcome (first (for [[o m] (get-in fixture/h1 [:likelihood :rows :strengthened]) :when (= 1 m)] o)))
(defn comparison [actual reference]
  {:production actual :lean-reference reference
   :deltas (into {} (for [id (keys reference)]
                      [id (- (double (get actual id)) (double (get reference id)))]))})
(def report
  {:schema :wm/parameter-production-match-v1 :row 11
   :registrations [(get-in fixture/h1 [:registration]) (get-in fixture/h2 [:registration])]
   :prior {:authority :declared-prior
           :comparison (comparison (get-in informative [:prior-kernel "advance-twice"])
                                   {"identity-transition" 1/2 "controlled-transition" 1/2})}
   :informative
   {:outcomes [h1-outcome h2-outcome]
    :h1 {:evidence (get-in informative [:posterior-kernel ["advance-twice" h1-outcome] :evidence])
         :posterior (comparison
                     (get-in informative [:posterior-kernel ["advance-twice" h1-outcome] :mass])
                     {"identity-transition" 1 "controlled-transition" 0})}
    :h2 {:evidence (get-in informative [:posterior-kernel ["advance-twice" h2-outcome] :evidence])
         :posterior (comparison
                     (get-in informative [:posterior-kernel ["advance-twice" h2-outcome] :mass])
                     {"identity-transition" 0 "controlled-transition" 1})}}
   :noninformative
   {:outcome common-outcome
    :evidence (get-in noninformative [:posterior-kernel ["advance-twice" common-outcome] :evidence])
    :posterior (comparison
                (get-in noninformative [:posterior-kernel ["advance-twice" common-outcome] :mass])
                {"identity-transition" 1/2 "controlled-transition" 1/2})}
   :compatible (= (:posterior-predictive informative) (:likelihood-marginal informative))
   :controls
   {:zero-evidence (first (filter #(= :zero-evidence-conditioning (get-in % [:refusal :kind]))
                                  (vals (:posterior-kernel informative))))
    :support-mismatch (parameters/parameter-kernels
                       fixture/model fixture/parameter-state fixture/policies (vec (butlast fixture/outcomes)))
    :constant-posterior-must-move {:passes? (not= {"identity-transition" 1/2 "controlled-transition" 1/2}
                                                  (get-in informative [:posterior-kernel ["advance-twice" h1-outcome] :mass]))}
    :mutated-pin (parameters/parameter-kernels
                  fixture/model
                  (assoc-in fixture/parameter-state [:hypotheses 0 :registration :path]
                            (str fixture/dir "/identity-transition-mutated.edn"))
                  fixture/policies fixture/outcomes)}})
(spit (str fixture/dir "/readback.edn") (pr-str report))
(prn report)
