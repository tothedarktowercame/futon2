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

(defn- bypassed [node refusals]
  {:node node
   :status :bypassed
   :decision :selection-abstained
   :because :typed-selection-refusal
   :evidence (mapv #(select-keys % [:target :kind :missing]) refusals)})

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
        typed-abstention? (and (= :abstained (:status decision))
                               (seq (:refusals decision)))
        by-node
        (if typed-abstention?
          (into {} (map (fn [node] [node (bypassed node (:refusals decision))])
                        [:R6 :R13 :R14 :CTAU-CLASS]))
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
           (refused :CTAU-CLASS :class-preference-not-consumed-by-scoring))})
        counts (frequencies (map :status (vals by-node)))]
    {:schema schema
     :status (if (zero? (get counts :refused 0)) :complete :incomplete)
     :by-node by-node
     :counts (merge {:present 0 :bypassed 0 :refused 0} counts)}))

(defn- decision-context
  "Retain enough of the upstream decision in a debugger stop to decide
  whether an unexercised node is a justified bypass or missing machinery.
  This deliberately avoids copying the full (and potentially large)
  construction certificate into exception data."
  [decision]
  (let [certificate (:selection-certificate decision)
        candidates (:candidates certificate)]
    {:decision-status (:status decision)
     :chosen (select-keys (:chosen decision) [:status :kind :id :target])
     :action (select-keys (:action decision) [:kind :id :target])
     :refusals (:refusals decision)
     :abstention (:abstention decision)
     :selection-law (:selection-law decision)
     :candidate-count (count candidates)
     :policy-count (count (:policies certificate))
     :candidates
     (mapv (fn [candidate]
             {:identity (select-keys candidate [:kind :id :target])
              :policy (select-keys (:policy candidate)
                                   [:kind :id :target :precedence :enacted-steps])
              :model (get-in candidate [:computed-f :model])})
           candidates)}))

(defn require-complete!
  "Return DECISION only when every required selection node was exercised.
  Otherwise throw inside the caller's restartable selection phase. This is a
  launch gate, not a post-run annotation: author/reviewer work must not begin
  for a decision already known to be incapable of demonstrating its declared
  machinery."
  [decision]
  (let [r (receipt decision)
        refused (->> (:by-node r) vals (filter #(= :refused (:status %))) vec)]
    (when (seq refused)
      (throw (ex-info "Required loop nodes were not exercised by selection"
                      {:outcome :selection-validation-failed
                       :failure-kind :required-loop-node-unexercised
                       :failure-stage :selection
                       :receipt r
                       :decision-context (decision-context decision)
                       :refused refused})))
    decision))
