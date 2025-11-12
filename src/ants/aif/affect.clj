(ns ants.aif.affect
  "Affect regulation: hunger dynamics and precision/temperature modulation."
  (:require [ants.aif.observe :as observe]
            [clojure.math :as math]))

(def ^:private default-burn 0.02)
(def ^:private default-feed 0.05)
(def ^:private default-rest 0.03)

(defn clamp01
  ^double [x]
  (-> x (max 0.0) (min 1.0)))

(defn- clamp
  [x lo hi]
  (-> x (max lo) (min hi)))

(ns ants.aif.affect
  (:require [ants.aif.observe :as observe]))

(defn next-mode
  "Finite-state controller over {:outbound|:homebound|:maintain} with hysteresis.
   Inputs: current-mode keyword, observation map, and :modes cfg block."
  [current-mode {:keys [cargo friendly-home reserve-home trail-grad food] :as obs}
   {:keys [cargo-high cargo-low home-high home-low reserve-low trail-min food-eps]
    :or {cargo-high 0.60 cargo-low 0.10 home-high 0.80 home-low 0.50
         reserve-low 0.20 trail-min 0.20 food-eps 0.02}}]
  (let [cargo        (double (or cargo 0.0))
        home         (double (or friendly-home 0.0))
        reserve-home (double (or reserve-home 0.5))
        trail        (double (or trail-grad 0.0))
        food         (double (or food 0.0))
        on-home?     (>= home home-high)
        near-home?   (>= home home-low)
        no-food?     (< food food-eps)
        weak-trail?  (< trail trail-min)]
    (case current-mode
      :homebound
      (if (<= cargo cargo-low)
        (if (and near-home? (or no-food? weak-trail?) (<= reserve-home reserve-low))
          :maintain
          :outbound)
        :homebound)

      :maintain
      (cond
        (>= cargo cargo-high) :homebound
        (and (not near-home?) (not on-home?)) :outbound
        :else :maintain)

      ; default → outbound
      :outbound
      (cond
        (>= cargo cargo-high) :homebound
        (and near-home? (or no-food? weak-trail?) (<= reserve-home reserve-low)) :maintain
        :else :outbound))))

(defn tick-hunger
  "Update hunger drive h in [0,1]. Higher values mean hungrier ants.

  Hunger increases due to metabolic burn, load stress, and decreases when food
  or home comfort is sensed."
  ([current observation]
   (tick-hunger current observation nil))
  ([current {:keys [food home-prox cargo]} {:keys [burn feed rest load-pressure] :as _opts}]
   (let [burn (double (or burn default-burn))
         feed (double (or feed default-feed))
         rest (double (or rest default-rest))
         load-pressure (double (or load-pressure 0.25))
         food (double (or food 0.0))
         home (double (or home-prox 0.0))
         load (double (or cargo 0.0))
         delta (+ burn (* load-pressure load) (- (* feed food)) (- (* rest home)))]
     (observe/clamp01 (+ (double (or current 0.5)) delta)))))

(defn update-hunger
  "Return updated hunger using observed ingest/deposit/risk this tick.

  Returns {:h' … :dh …}."
  [h obs cfg]
  (let [h0 (double (or h 0.5))
        ing (double (max 0.0 (or (:ingest obs) 0.0)))
        dep (double (max 0.0 (or (:deposit obs) 0.0)))
        risk (double (max 0.0 (or (:risk obs) 0.0)))
        mrate (double (max 0.0 (or (:metabolic-rate cfg) 0.010)))
        k-ing 0.60
        k-dep 0.05
        k-met 0.015
        k-risk 0.020
        h' (clamp01 (+ h0
                       (* -1.0 k-ing ing)
                       (* -1.0 k-dep dep)
                       (* k-met (max mrate 1.0e-6))
                       (* k-risk risk)))
        dh (- h' h0)]
    {:h' h'
     :dh dh}))

(defn warn-if-ingesting-while-hunger-rises!
  [obs dh log-fn]
  (when (and (fn? log-fn)
             (> (double (or (:ingest obs) 0.0)) 0.6)
             (> (double dh) 1.0e-6))
    (log-fn {:warn :ingest_but_hunger_up
             :ingest (:ingest obs)
             :deposit (:deposit obs)
             :risk (:risk obs)
             :delta-h dh})))

(defn hunger->tau
  "Map hunger to a softmax temperature. Hungrier ants exploit more (lower tau)."
  ([h]
   (hunger->tau h {:min 0.35 :max 2.6}))
  ([h {:keys [min max]
       :or {min 0.35 max 2.6}}]
   (let [h (observe/clamp01 h)
         span (- max min)]
     (+ min (* span (- 1.0 h))))))

(defn modulate-precisions
  "Return an updated precision structure {:Pi-o ... :tau ...} informed by hunger
  and contextual safety (home proximity)."
  ([prec hunger observation]
   (modulate-precisions prec hunger observation nil))
  ([prec hunger {:keys [home-prox] :as observation}
    {:keys [food-scale pher-scale h-scale tau-min tau-max tau-floor tau-cap]
     :or {food-scale 1.4
          pher-scale 0.6
          h-scale 1.1
          tau-min 0.35
          tau-max 2.6
          tau-floor nil
          tau-cap nil}}]
   (let [Pi-o (merge {:food 1.0
                      :pher 0.8
                      :food-trace 0.6
                      :pher-trace 0.5
                      :home-prox 0.7
                      :enemy-prox 0.9
                      :h 1.0}
                     (:Pi-o prec))
         home-prox (double (or home-prox 0.0))
         safety (+ 1.0 (* 0.5 home-prox))
         hunger (observe/clamp01 hunger)
         food-prec (+ (Pi-o :food) (* food-scale hunger))
         pher-prec (+ (Pi-o :pher) (* pher-scale (- 1.0 hunger)))
         hunger-prec (+ (Pi-o :h) (* h-scale hunger))
         prox-prec (+ (Pi-o :home-prox) (* 0.4 home-prox))
         enemy-prec (+ (Pi-o :enemy-prox) (* 0.6 hunger))
         base-floor (double (or tau-floor tau-min))
         base-cap (double (or tau-cap tau-max))
         tau (-> (hunger->tau hunger {:min tau-min :max tau-max})
                 (* (/ safety 1.5))
                 (clamp base-floor base-cap))]
     {:Pi-o (assoc Pi-o
                   :food food-prec
                   :pher pher-prec
                   :food-trace (+ (Pi-o :food-trace) (* 0.3 hunger))
                   :pher-trace (+ (Pi-o :pher-trace) (* 0.2 (- 1.0 hunger)))
                   :home-prox prox-prec
                   :enemy-prox enemy-prec
                   :h hunger-prec)
      :tau tau})))

(defn anneal-tau
  "Anneal temperature toward its hunger-informed target over predictive steps."
  [{:keys [tau] :as prec} step max-steps]
  (let [progress (if (pos? max-steps)
                   (/ (inc step) (double max-steps))
                   1.0)
        target (:tau prec)
        start (max 0.2 (* 1.5 target))
        new-tau (+ (* (- 1.0 progress) start)
                   (* progress target))]
    (assoc prec :tau (clamp new-tau 0.2 4.0))))

(defn need-error
  "Return aggregate need violation given observation :obs with hunger/ingest.

  Optional cfg allows overriding setpoints via {:hunger-thresh … :ingest-thresh …}."
  ([state]
   (need-error state {}))
  ([{:keys [obs]} {:keys [hunger-thresh ingest-thresh]
                   :or {hunger-thresh 0.45
                        ingest-thresh 0.60}}]
   (let [obs (or obs {})
         h (double (or (get obs :hunger)
                       (get obs :h)
                       0.0))
         ing (double (or (get obs :ingest) 0.0))
         h-err (max 0.0 (- h hunger-thresh))
         ingest-target (double ingest-thresh)
         i-err (max 0.0 (- ingest-target ing))]
     (+ h-err i-err))))

(defn update-tau
  "Adjust tau upward when needs are violated or hunger trend increases.

  `state` is {:obs … :dhdt …}. Optional cfg supports {:tau-floor … :tau-cap …
  :need-gain … :dhdt-gain … :hunger-thresh … :ingest-thresh …}."
  ([prec state]
   (update-tau prec state {}))
  ([{:keys [tau] :as prec} state
    {:keys [tau-floor tau-cap need-gain dhdt-gain hunger-thresh ingest-thresh reserve-home]
     :or {tau-floor 0.08
          tau-cap 1.5
          need-gain 0.6
          dhdt-gain 0.8
          hunger-thresh 0.45
          ingest-thresh 0.60}}]
   (let [need (need-error state {:hunger-thresh hunger-thresh
                                 :ingest-thresh ingest-thresh})
         dh (double (max 0.0 (or (:dhdt state) 0.0)))
         reserve (double (or reserve-home (get-in state [:obs :reserve-home]) 0.5))
         reserve-term (cond
                        (< reserve 0.2) -0.18
                        (< reserve 0.35) -0.12
                        (< reserve 0.5) -0.05
                        (> reserve 0.75) 0.08
                        (> reserve 0.6) 0.04
                        :else 0.0)
         delta (+ (* need-gain need)
                  (* dhdt-gain dh)
                  reserve-term)
         tau (double (or tau tau-floor))
         obs (:obs state)
         h (double (or (:hunger obs) (:h obs) 0.0))
         ing (double (or (:ingest obs) 0.0))
         cargo (double (or (:cargo obs) 0.0))
         clamp? (and (> h (double (or hunger-thresh 0.45)))
                     (< ing (double (or ingest-thresh 0.60)))
                     (> cargo 0.25))
         clamp-delta (if clamp?
                       (- 0.25 (* 0.35 (max 0.0 (- h (double hunger-thresh)))))
                       0.0)
         tau' (-> (+ tau delta clamp-delta)
                  (max tau-floor)
                  (min tau-cap))]
     (assoc prec :tau tau'))))
