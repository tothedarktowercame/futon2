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

(defn- decision-bypass [node reason evidence]
  {:node node :status :bypassed :because reason :evidence evidence})

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
        policy-of (fn [candidate]
                    (or (:policy candidate) (:id candidate)
                        (get-in candidate [:f-prefix :policy])))
        class-scoring (filter #(= :class-emission
                                  (get-in % [:computed-f :model :kind]))
                              candidates)
        horizons (keep #(get-in % [:computed-f :model :horizon]) candidates)
        precedences (mapv #(mapv (fn [entry]
                                   (if (map? entry) (:id entry) entry))
                                 (:precedence (policy-of %)))
                          candidates)
        probabilities (or (get-in certificate [:policy-precision-state :probabilities])
                          (get-in certificate [:precision-family :probabilities])
                          (get-in decision [:selection-law :posterior]))
        temperature (or (get-in certificate [:policy-precision-state :temperature])
                        (get-in certificate [:precision-family :temperature])
                        (get-in decision [:selection-law :beta]))
        abstention (get-in decision [:abstention :status])
        typed-abstention? (and (= :abstained (:status decision))
                               (seq (:refusals decision)))
        by-node
        (if typed-abstention?
          (into {} (map (fn [node] [node (bypassed node (:refusals decision))])
                        [:R6 :R13 :R14 :CTAU-CLASS]))
          {:R6 (cond
                 (> (count candidates) 1)
                 (present :R6 {:candidate-count (count candidates)
                               :policy-count (count policies)})

                 (= 1 (count candidates))
                 (decision-bypass :R6 :singleton-admissible-candidate
                                  {:candidate-count 1 :policy-count (count policies)
                                   :candidate (select-keys (policy-of (first candidates))
                                                           [:kind :id :target])})

                 :else (refused :R6 :candidate-space-not-retained))
         :R13 (if (and (some #(> % 1) horizons)
                       (some #(> (count %) 1) precedences))
                (present :R13 {:horizons (vec horizons)
                               :precedences precedences
                               :basis :multi-step-policy-rollout})
                (if (and (= 1 (count candidates))
                         (= 1 (count (first precedences))))
                  (decision-bypass :R13 :single-step-policy
                                   {:horizons (vec horizons)
                                    :precedences precedences})
                  (refused :R13 :policy-rollout-evidence-not-retained)))
         :R14 (if (> (count candidates) 1)
                (if (and (number? temperature) (map? probabilities))
                  (present :R14 {:temperature temperature
                                 :probability-count (count probabilities)
                                 :abstention-status abstention})
                  (refused :R14 :selection-posterior-not-retained))
                (if (and (= 1 (count candidates))
                         (number? temperature) (map? probabilities)
                         (= 1 (count probabilities)))
                  (decision-bypass :R14 :singleton-policy-posterior
                                   {:temperature temperature
                                    :probability-count 1
                                    :candidate (select-keys (policy-of (first candidates))
                                                            [:kind :id :target])})
                  (refused :R14 :selection-posterior-not-retained)))
         :CTAU-CLASS
         (if (and (seq class-scoring)
                  (every? #(map? (get-in % [:computed-f :model :class-preference]))
                          class-scoring))
           (present :CTAU-CLASS
                    {:scored-candidate-count (count class-scoring)
                     :comparison-status (:status comparison)
                     :horizons (mapv #(get-in % [:computed-f :model :horizon])
                                     class-scoring)
                     :preference-kind :class-emission})
           (refused :CTAU-CLASS :class-preference-evidence-not-retained))})
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
              :policy (select-keys (or (:policy candidate) (:id candidate)
                                       (get-in candidate [:f-prefix :policy]))
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
