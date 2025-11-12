(ns ants.aif.perceive
  "Predictive coding style perceptual updates for AIF ants."
  (:require [ants.aif.affect :as affect]
            [ants.aif.observe :as observe]))

(def ^:private sensory-keys
  [:food :pher :food-trace :pher-trace :home-prox :enemy-prox :h :ingest
   :friendly-home :trail-grad :novelty :dist-home :reserve-home :cargo])

(def ^:private default-precisions
  {:Pi-o {:food 1.0
          :pher 0.8
          :food-trace 0.6
          :pher-trace 0.5
          :home-prox 0.8
          :enemy-prox 0.9
          :h 1.1
          :ingest 0.9
          :friendly-home 0.8
          :trail-grad 0.7
          :novelty 0.6
          :dist-home 0.9
          :reserve-home 0.6
          :cargo 1.2}
   :tau 1.6})

(defn- blend
  [src dst rate]
  (mapv (fn [s d] (+ s (* rate (- d s)))) src dst))

(defn- ensure-mu
  [world {:keys [mu loc species] :as ant} observation]
  (let [loc (vec (or loc [0 0]))
        species (or species :aif)
        existing (or mu {})
        enemy-home (when world
                     (get-in world [:homes (if (= species :aif) :classic :aif)]))
        home (when world (get-in world [:homes species]))
        goal (vec (or (:goal existing)
                      enemy-home
                      home
                      loc))
        sens-defaults (zipmap sensory-keys (repeat 0.5))
        sens (merge sens-defaults (:sens existing) observation)
        hunger (observe/clamp01 (or (:h existing)
                                    (:h observation)
                                    0.5))]
    (-> existing
        (assoc :pos loc
               :goal goal
               :h hunger
               :sens sens))))

(defn- ensure-prec
  [{:keys [prec]}]
  (merge-with (fn [m n]
                (if (map? m)
                  (merge m n)
                  n))
              default-precisions
              (or prec {})))

(defn- compute-errors
  [mu observation prec]
  (reduce (fn [acc key]
            (let [obs (double (get observation key 0.0))
                  pred (double (get-in mu [:sens key] obs))
                  precision (double (get-in prec [:Pi-o key] 1.0))
                  raw (- obs pred)
                  weighted (* precision raw)]
              (assoc acc key {:raw raw
                               :precision precision
                               :weighted weighted})))
          {}
          sensory-keys))

(defn- weighted-mse
  [errors]
  (if (seq errors)
    (/ (reduce + (map (fn [[_ {:keys [raw precision]}]]
                        (* precision raw raw))
                      errors))
       (double (count errors)))
    0.0))

(defn- update-goal
  [goal world species observation]
  (let [enemy-home (when world
                     (get-in world [:homes (if (= species :aif) :classic :aif)]))
        home (when world (get-in world [:homes species]))
        enemy-prox (double (or (:enemy-prox observation) 0.0))
        home-prox (double (or (:home-prox observation) 0.0))
        cargo (double (or (:cargo observation) 0.0))
        goal (if enemy-home
                (blend goal enemy-home (+ 0.05 (* 0.2 (max 0.0 (- enemy-prox (* 0.4 cargo))))))
                goal)
        goal (if home
                (let [bias (min 0.95 (+ 0.05 (* 0.5 cargo) (* 0.2 (max 0.0 (- home-prox 0.4)))))]
                  (blend goal home bias))
                goal)]
    goal))

(defn- update-sensory
  [sens errors alpha]
  (reduce (fn [acc [k {:keys [weighted]}]]
            (if (= k :h)
              acc
              (assoc acc k
                     (observe/clamp01
                      (+ (double (get acc k 0.5)) (* alpha weighted))))))
          sens
          errors))

(defn perceive
  "Run predictive coding micro-steps to update ant beliefs (mu) and precision.

  Returns {:mu … :prec … :errors … :free-energy … :trace …}."
  ([world ant observation]
   (perceive world ant observation nil))
  ([world ant observation {:keys [max-steps alpha beta hunger-options precision-options]
                           :or {max-steps 5
                                alpha 0.55
                                beta 0.3}
                           :as _opts}]
   (let [max-steps (max 1 (int max-steps))
         mu0 (ensure-mu world ant observation)
         prec0 (ensure-prec ant)]
     (loop [step 0
            mu mu0
            prec prec0
            trace []
            error-sum 0.0]
       (let [hunger (:h mu)
             prec-target (affect/modulate-precisions prec hunger observation precision-options)
             prec-step (affect/anneal-tau prec-target step max-steps)
             errors (compute-errors mu observation prec-step)
             sens' (update-sensory (:sens mu) errors alpha)
             hunger' (affect/tick-hunger
                      (observe/clamp01
                       (+ hunger (* beta (get-in errors [:h :weighted] 0.0))))
                      observation
                      hunger-options)
             mu' (-> mu
                     (assoc :sens sens'
                            :h hunger'
                            :pos (vec (or (:loc ant) (:pos mu)))
                            :goal (update-goal (:goal mu) world (:species ant) observation)))
             step-error (weighted-mse errors)
             trace' (conj trace {:tau (:tau prec-step)
                                 :h hunger'
                                 :error step-error})
             error-sum' (+ error-sum step-error)]
        (if (>= (inc step) max-steps)
          {:mu mu'
           :prec prec-step
           :errors errors
           :free-energy (* 0.5 (/ error-sum' max-steps))
           :trace trace'}
          (recur (inc step) mu' prec-target trace' error-sum')))))))
