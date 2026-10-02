(ns futon2.aif.wm.loop-node-exercise
  "Per-click evidence that selected loop machinery ran, rather than merely
  existing in code or appearing in a declared model."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def schema :wm/loop-node-exercise-v1)

(defn- present [node evidence]
  {:node node :status :present :evidence evidence})

(defn- refused [node reason]
  {:node node :status :refused :reason reason})

(defn receipt [decision]
  (let [certificate (:selection-certificate decision)
        candidates (:candidates certificate)
        policies (:policies certificate)
        comparison (get-in decision [:selection-law :policy-comparison])
        class-scoring (filter #(= :class-emission
                                  (get-in % [:computed-f :model :kind]))
                              candidates)
        horizons (keep #(get-in % [:computed-f :model :horizon]) candidates)
        enacted-depths (map #(count (or (get-in % [:policy :enacted-steps])
                                        (get-in % [:f-prefix :policy :enacted-steps])))
                            candidates)
        probabilities (or (get-in certificate [:policy-precision-state :probabilities])
                          (get-in certificate [:precision-family :probabilities]))
        temperature (or (get-in certificate [:policy-precision-state :temperature])
                        (get-in certificate [:precision-family :temperature]))
        abstention (get-in decision [:abstention :status])
        by-node
        {:R6 (if (> (count candidates) 1)
               (present :R6 {:candidate-count (count candidates)
                             :policy-count (count policies)})
               (refused :R6 :no-competing-action-candidates))
         :R13 (if (and (some #(> % 1) horizons)
                       (some #(> % 1) enacted-depths))
                (present :R13 {:horizons (vec horizons)
                               :enacted-depths (vec enacted-depths)
                               :basis :multi-step-policy-enacted})
                (refused :R13 :no-observed-multi-step-policy-rollout))
         :R14 (if (and (number? temperature) (map? probabilities)
                       (contains? #{:abstained :not-abstained} abstention))
                (present :R14 {:temperature temperature
                               :probability-count (count probabilities)
                               :abstention-status abstention})
                (refused :R14 :temperature-probability-choice-not-retained))
         :CTAU-CLASS
         (if (and (> (count class-scoring) 1)
                  (= :compared (:status comparison)))
           (present :CTAU-CLASS
                    {:scored-candidate-count (count class-scoring)
                     :comparison-status (:status comparison)
                     :preference-kind :class-emission})
           (refused :CTAU-CLASS :class-preference-not-consumed-by-scoring))}
        counts (frequencies (map :status (vals by-node)))]
    {:schema schema
     :status (if (zero? (get counts :refused 0)) :complete :incomplete)
     :by-node by-node
     :counts (merge {:present 0 :refused 0} counts)}))
