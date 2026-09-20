(ns futon2.aif.exact-belief-adapter
  "Standalone finite rational realization of ExactBeliefTrajectory.exactUpdate.
   No live wiring. A and B are functions returning sparse probability rows;
   B's first argument is the previous state. Invalid inputs are not Option.none."
  (:require [futon2.aif.cascade-model-manifest :as model]
            [futon2.aif.exact-belief-core :as core]))

(defn exact-update
  "Finite carrier states, row functions A/B, observation, prior, declaration.
   Realizes predictedState, observationProbability, exactUpdate on exact rationals.
   :ok/:some carries :posterior; :refused/:none iff predictive probability is zero
   on valid inputs. :invalid is a domain error, not a mathematical refusal.
   Evaluates each supplied row once and retains the values actually consumed."
  [states A B observation prior declaration]
  (let [receipt {:schema :wm/exact-belief-update-v1 :model declaration
                 :states states :observation observation :prior prior}
        invalid (fn [kind detail]
                  (assoc receipt :status :invalid :kind kind :detail detail))
        carrier (set states)]
    (cond
      (not (and (sequential? states) (seq states) (= (count states) (count carrier))))
      (invalid :invalid-state-carrier states)

      (not (and (core/distribution? prior) (every? #(contains? carrier %) (keys prior))))
      (invalid :invalid-prior prior)

      :else
      (let [arows (into {} (map (fn [s] [s (A s)]) states))
            brows (into {} (map (fn [s] [s (B s)]) states))]
        (cond
          (not (every? core/distribution? (vals arows)))
          (invalid :invalid-observation-kernel arows)

          (not (every? #(and (core/distribution? %)
                            (every? (fn [s] (contains? carrier s)) (keys %))) (vals brows)))
          (invalid :invalid-transition-kernel brows)

          :else
          (let [predicted (into {} (for [x states]
                                     [x (reduce +' 0 (for [s states]
                                                       (*' (get-in brows [s x] 0)
                                                           (get prior s 0))))]))
                likelihoods (into {} (for [x states] [x (get-in arows [x observation] 0)]))]
            (merge receipt
                   (core/condition-predicted predicted likelihoods observation)
                   {:observation-rows arows :transition-rows brows})))))))

(defn synthetic-mixture-update
  "Lab adapter using A's declared exact latent mixture. z is redrawn per step,
   NOT a persistent session condition; parameters carry no calibration authority."
  [states components B observation prior]
  (exact-update states #(model/mixture-observation-distribution components %) B
                observation prior
                {:status :declared :parameter-basis :synthetic
                 :z-semantics :per-step-redraw :components components
                 :calibration-authority :none}))
