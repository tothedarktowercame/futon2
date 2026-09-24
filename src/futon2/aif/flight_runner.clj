(ns futon2.aif.flight-runner
  "Production adapters for futon2.aif.flight: a click is one
  full_loop_runner/run-opportunity! with the flight on its judge options;
  an observation is the want locators checked through observation-checks.

  Kept apart from futon2.aif.flight so the flight core stays pure and its
  tests need no runner."
  (:require [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.observation-checks :as checks]))

(defn click-summary
  "What the flight needs from one run's RESULT (run-opportunity-core!'s
  map) for TARGET: the chosen plan's :unreached-wants, from the
  construction receipt the chosen action carries; and, when the tick
  abstained, the flight target's own decline from the abstention carrier
  the run record is built from (D8, 97a84770)."
  [target run-id result]
  (let [decision (or (get-in result [:checkpoints :selection :judgment :controller-decision])
                     (get-in result [:checkpoints :selection :judgment :decision]))
        sorry (get-in result [:checkpoints :selection :sorry])
        carrier (runner/abstention-carrier (or decision (:decision sorry))
                                           (:dropped-candidates sorry))
        action (:action decision)
        mine (when (= :abstained (:status carrier))
               (or (first (filter #(= target (:target %)) (:targets carrier)))
                   {:kind :target-not-in-refusals :missing :refusal}))]
    (cond-> {:click-id run-id
             :chosen (when (and action (= target (:target action)))
                       {:candidate (:id action)
                        :precedence (mapv #(or (:id %) %) (:precedence action))})
             :unreached-wants (vec (when (= target (:target action))
                                     (get-in action [:construction-receipt :unreached-wants])))}
      mine (assoc :abstention mine))))

(defn click-fn
  "A flight click function over RUN! (default runner/run-opportunity!):
  each call runs one opportunity with BASE-OPTS plus the flight's judge
  options, the flight on :flight so the default selection judge assembles
  only the flight's target (war_machine/flight-assembly-input). RUN-ID-FN
  names each run."
  [{:keys [base-opts run! run-id-fn]}]
  (let [run! (or run! runner/run-opportunity!)]
    (fn [judge-opts]
      (let [flight (:flight judge-opts)
            run-id ((or run-id-fn #(str (:flight/id flight) "-click-" (:click flight))))
            result (run! (assoc base-opts :run-id run-id :flight flight))]
        (click-summary (:target flight) run-id result)))))

(defn observe-fn
  "A flight observe function: check each want's locator. A refused check
  is :unknown, never false and never true, so it cannot count as a want
  advanced or met."
  ([] (observe-fn checks/observe))
  ([observe]
   (fn [_target locators]
     (let [{:keys [observed results refused]} (observe locators)]
       (merge (into {} (map (fn [t] [t (contains? observed t)])) (keys results))
              (into {} (map (fn [t] [t :unknown])) (keys refused)))))))
