(ns ants.aif.policy
  "Action evaluation via 1-step expected free energy and softmax selection."
  (:require [ants.aif.observe :as observe]
            [clojure.math :as math]))

(def default-actions
  "Canonical set of macro actions for the AIF ants."
  [:hold :forage :return :pheromone])

(def ^:private actions-by-mode
  {:outbound  [:forage :pheromone :hold :return]
   :homebound [:return :pheromone :hold :forage]
   :maintain  [:pheromone :hold :return :forage]})

;; --- Latent mode inference + mode-conditioned priors -----------------------

(def ^:private modes [:outbound :homebound :maintain])

(def ^:private mode-thresholds
  "Soft, interpretable thresholds; tune in config later if useful."
  {:near-home 0.80
   :pher-min  0.15
   :trail-weak 0.25
   :cargo-min 0.05})

(defn- derive-mode-features
  "Lift raw observation into simple feature booleans for mode inference."
  [observation]
  (let [{:keys [near-home pher-min trail-weak cargo-min]} mode-thresholds
        cargo (double (or (:cargo observation) 0.0))
        near? (>= (double (or (:home-prox observation)
                              (:friendly-home observation) 0.0)) near-home)
        pher? (>= (double (or (:pher observation) 0.0)) pher-min)
        weak? (<= (double (or (:trail-grad observation) 0.0)) trail-weak)]
    {:cargo cargo
     :near? near?
     :pher? pher?
     :weak? weak?}))

(defn- infer-mode
  "Posterior over modes q(m|o) via a soft evidence template; returns {mode p}."
  [{:keys [cargo near? pher? weak?]}]
  (let [lp {:outbound (+ (* 2.0 (if (and (<= cargo 0.0) (not near?)) 1 0))
                         (* 0.5 (if pher? 1 0))
                         (* 0.5 (if weak? 1 0)))
            :homebound (+ (* 2.5 (if (> cargo (:cargo-min mode-thresholds)) 1 0))
                          (* 1.0 (if near? 1 0)))
            :maintain (+ (* 1.5 (if weak? 1 0))
                         (* 0.5 (if (and (<= cargo 0.0) pher?) 1 0)))}
        mx (apply max (vals lp))
        zs (reduce + (map #(Math/exp (- % mx)) (vals lp)))]
    (into {} (for [m modes] [m (/ (Math/exp (- (lp m) mx)) zs)]))))

(defn- C-prior
  "Mode-conditioned prior preferences toward outcome features (linear weights)."
  []
  {:outbound  {:gath 1.0 :ing 0.4 :cargo+ 0.8 :near-nest -0.2 :trail+ 0.2}
   :homebound {:dep  1.2 :cargo0 1.0 :near-nest 0.8 :gath 0.0 :trail+ 0.1}
   :maintain  {:trail+ 1.0 :pheromone 0.8 :near-nest 0.2}})

(defn- predict-outcomes
  "Myopic one-step feature predictions p(o|a,s), kept intentionally light.
   Map keys match C-prior features."
  [observation action]
  (let [{:keys [cargo near? weak?]} (derive-mode-features observation)
        cargo (double cargo)]
    (case action
      :return    {:near-nest (if near? 1.0 0.4)
                  :cargo+ (if (> cargo 0.0) 0.1 0.0)}
      :forage    {:gath (if (> cargo 0.0) 0.0 0.5)
                  :cargo+ (if (> cargo 0.0) 0.0 0.6)}
      :pheromone {:trail+ (if weak? 0.8 0.3)
                  :pheromone 1.0}
      :hold      {}
      {})))

(defn- efe-tilt
  "Compute λ * (−G proxy) as a logit adjustment from mode-conditioned priors."
  [observation action {:keys [lambda] :or {lambda 0.6}}]
  (let [qM (infer-mode (derive-mode-features observation))
        C  (C-prior)
        oh (predict-outcomes observation action)
        extrinsic (reduce +
                          (for [[m pm] qM
                                :let [Cm (C m)]]
                            (* pm (reduce + (for [[k v] oh]
                                              (* (double (or (Cm k) 0.0))
                                                 (double (or v 0.0))))))))]
    (* (double lambda) extrinsic)))

(def ^:private default-preferences
  {:hunger {:mean 0.40 :sd 0.08}
   :ingest {:mean 0.70 :sd 0.20}})

(def ^:private default-action-costs
  {:pheromone {:base-cost 0.20
               :hunger-mult 0.1
               :no-ingest-pen 4.0
               :ingest-thresh 0.2
               :friendly-home-pen 0.8}
   :forage {:friendly-home-pen 1.2}
   :return {:base-cost 0.25
            :empty-home-pen 0.9
            :cargo-thresh 0.05
            :home-thresh 0.8
            :hunger-gap-mult 3.8}})

(def ^:private default-efe-lambda
  {:pragmatic 1.0
   :ambiguity 0.5
   :info 0.4
   :colony 0.4
   :survival 1.2})

(def ^:private default-colony-config
  {:reserve-thresh 1.0
   :non-return-pen 0.6
   :return-pen 0.0})

(def ^:private default-survival-config
  {:hunger-thresh 0.55
   :hunger-weight 1.5
   :dist-weight 0.5
   :ingest-buffer 0.30
   :return-reduction 0.40
   :pressure-norm 2.0})

(defn- clamp
  [x]
  (observe/clamp01 x))

(defn- bound
  [x lo hi]
  (-> x (max lo) (min hi)))

(defn- drift
  [value toward rate]
  (clamp (+ value (* rate (- toward value)))))

(defn- predict-outcome
  "Heuristic one-step predictive model for each abstract action, including ingest proxy."
  [mu observation action]
  (let [h (double (or (:h mu) 0.5))
        cargo (double (or (:cargo observation) (:cargo mu) 0.0))
        ingest (double (or (:ingest observation) 0.0))
        friendly-home (double (or (:friendly-home observation) 0.0))
        trail-grad (double (or (:trail-grad observation) 0.0))
        novelty (double (or (:novelty observation) 0.0))
        dist-home (double (or (:dist-home observation) 0.0))
        reserve-home (double (or (:reserve-home observation) 0.0))
        local-food (double (or (:food observation) 0.0))
        food-trace (double (or (:food-trace observation) 0.0))
        recent-gather (double (or (:recent-gather observation) 0.0))
        base observation
        {:keys [state ingest-target ingest-rate]}
        (case action
          :forage (let [availability (observe/clamp01 (+ (* 0.55 local-food)
                                                         (* 0.25 food-trace)
                                                         (* 0.20 (max recent-gather food-trace))))
                        forage-seed (+ 0.05
                                       (* 0.60 local-food)
                                       (* 0.30 availability))
                        forage-potential (observe/clamp01 (max local-food forage-seed))
                        food-target (clamp (+ local-food (* forage-potential (- 1.0 local-food))))
                        food-rate (+ 0.08
                                     (* 0.35 forage-potential)
                                     (* 0.12 availability))
                        cargo-target (clamp (+ cargo (* forage-potential (- 1.0 cargo))))
                        cargo-rate (+ 0.12
                                      (* 0.55 forage-potential)
                                      (* 0.15 availability))
                        ingest-floor (if (> friendly-home 0.5) 0.25 0.35)
                        ingest-peak (if (> friendly-home 0.5) 0.55 0.9)
                        ingest-target (clamp (+ (* forage-potential ingest-peak)
                                                (* (- 1.0 forage-potential) ingest-floor)
                                                (* 0.25 (max 0.0 (- local-food 0.25)))))
                        ingest-rate (+ 0.32
                                       (* 0.40 forage-potential)
                                       (* 0.15 (max availability local-food)))
                        pref-h (if (> friendly-home 0.5) 0.34 0.38)
                        hunger-gap (max 0.0 (- h pref-h))
                        hunger-catch (bound (+ 0.45
                                               (* 0.35 forage-potential)
                                               (* 0.30 availability))
                                            0.10 0.85)
                        hunger-fill (bound (+ 0.30
                                              (* 0.35 forage-potential)
                                              (* 0.20 availability))
                                           0.10 0.70)
                        hunger-target (clamp (if (pos? hunger-gap)
                                               (- h (* hunger-gap hunger-catch))
                                               (+ h (* (- pref-h h) hunger-fill))))
                        hunger-rate (+ 0.32
                                       (* 0.48 forage-potential)
                                       (* 0.18 availability))
                        pher-rate (+ 0.05 (* 0.18 forage-potential))]
                    {:state (-> base
                                (update :food #(drift % food-target food-rate))
                                (update :pher #(drift % 0.1 pher-rate))
                                (update :home-prox #(drift % 0.1 0.4))
                                (update :enemy-prox #(drift % 0.8 0.35))
                                (update :h #(drift % hunger-target hunger-rate))
                                (update :cargo #(drift % cargo-target cargo-rate)))
                     :ingest-target ingest-target
                     :ingest-rate ingest-rate})
          :return (let [home-approach (-> base
                                          (update :food #(drift % 0.2 0.45))
                                          (update :pher #(drift % 0.25 0.4))
                                          (update :home-prox #(drift % 1.0 0.65))
                                          (update :enemy-prox #(drift % 0.05 0.45))
                                          (assoc :cargo (drift cargo 0.0 1.2)))
                        cargo-gap (max 0.0 (- 0.3 cargo))
                        hunger-relief (max 0.15 (- h 0.25))
                        hunger-bias (min 0.35 (* hunger-relief (* 0.6 cargo)))
                        hunger-target (-> h
                                          (- (if (pos? cargo)
                                               hunger-bias
                                               (* 0.05 cargo-gap)))
                                          clamp)
                        hunger-rate (if (pos? cargo)
                                      (+ 0.25 (* 0.5 (clamp cargo)))
                                      0.08)
                        ingest-target (if (and (> friendly-home 0.5)
                                               (< cargo 0.1))
                                        0.05
                                        (min 0.9 (+ 0.5 (* 0.3 (clamp cargo)))))
                        ingest-rate (if (pos? cargo) 0.55 0.35)]
                    {:state (-> home-approach
                                (update :h #(drift % hunger-target hunger-rate)))
                     :ingest-target ingest-target
                     :ingest-rate ingest-rate})
          :pheromone {:state (-> base
                                 (update :pher #(let [rate (* 0.6 (max 0.0 (- 1.0 friendly-home)))]
                                                  (drift % 1.0 rate)))
                                 (update :pher-trace #(let [rate (* 0.55 (max 0.0 (- 1.0 friendly-home)))]
                                                        (drift % 1.0 rate)))
                                 (update :food #(drift % 0.4 0.25))
                                 (update :home-prox #(drift % 0.6 0.35))
                                 (update :enemy-prox #(drift % 0.5 0.35))
                                 (update :h #(drift % 0.4 0.3))
                                 (update :cargo #(drift % 0.7 0.45)))
                      :ingest-target 0.1
                      :ingest-rate 0.45}
          :hold {:state (-> base
                            (update :food #(drift % 0.3 0.25))
                            (update :pher #(drift % 0.15 0.25))
                            (update :home-prox #(drift % 0.2 0.35))
                            (update :enemy-prox #(drift % 0.7 0.3))
                            (update :h #(drift % 0.5 0.2))
                            (update :cargo #(drift % cargo 0.1)))
                 :ingest-target 0.35
                 :ingest-rate 0.35}
          {:state base
           :ingest-target 0.25
           :ingest-rate 0.25})
        state (or state base)
        ingest-target (double (or ingest-target 0.25))
        ingest-rate (double (or ingest-rate 0.4))
        h' (double (or (:h state) (:h observation) h))
        trail-bump (if (= action :pheromone)
                     (+ 0.12 (* 0.45 (clamp cargo)))
                     0.0)
        state (-> state
                  (assoc :ingest (drift ingest ingest-target ingest-rate))
                  (assoc :h (clamp h'))
                  (assoc :friendly-home friendly-home)
                  (assoc :trail-grad (clamp (if (= action :pheromone)
                                              (+ trail-grad (min 0.6 (+ trail-bump (* 0.25 (max cargo 0.0)))))
                                              trail-grad)))
                  (assoc :novelty (clamp (if (= action :forage)
                                           (* 0.75 novelty)
                                           novelty)))
                  (assoc :dist-home (clamp (case action
                                             :return (* 0.6 dist-home)
                                             :forage (min 1.0 (+ dist-home 0.05))
                                             dist-home)))
                  (assoc :reserve-home (clamp reserve-home))
                  (assoc :hunger (clamp h')))]
    state))

(defn- ensure-preferences
  [prefs]
  (merge default-preferences (or prefs {})))

(defn- ensure-action-costs
  [costs]
  (merge default-action-costs (or costs {})))

(defn- ensure-efe-lambda
  [lambda]
  (merge default-efe-lambda (or lambda {})))

(defn- ensure-colony-config
  [cfg]
  (merge default-colony-config (or cfg {})))

(defn- ensure-survival-config
  [cfg]
  (merge default-survival-config (or cfg {})))

(defn- nll
  [x {:keys [mean sd] :or {mean 0.0 sd 0.2}}]
  (let [sd (double (max 1e-6 sd))
        z (/ (- (double x) (double mean)) sd)]
    (* 0.5 z z)))

(defn- risk-from-preferences
  [outcome prefs]
  (let [{:keys [hunger ingest]} (ensure-preferences prefs)
        hunger-x (double (or (:hunger outcome)
                             (:h outcome)
                             0.0))
        ingest-x (double (or (:ingest outcome) 0.0))]
    (+ (nll hunger-x hunger)
       (nll ingest-x ingest))))

(defn- action-prior-cost
  [action outcome hunger action-cfg]
  (case action
    :pheromone (let [{:keys [base-cost hunger-mult no-ingest-pen ingest-thresh friendly-home-pen]
                      :or {base-cost 0.01
                           hunger-mult 0.1
                           no-ingest-pen 0.3
                           ingest-thresh 0.2
                           friendly-home-pen 0.8}} (or action-cfg {})
                     ingest (double (or (:ingest outcome) 0.0))
                     friendly-home (double (or (:friendly-home outcome) 0.0))
                     ingest-thresh (double ingest-thresh)
                     hunger (double (observe/clamp01 hunger))
                     hunger-term (* hunger-mult hunger)
                     ingest-gap (max 0.0 (- ingest-thresh ingest))
                     ingest-term (* no-ingest-pen ingest-gap)
                     friendly-term (* friendly-home-pen friendly-home)]
                 (+ base-cost hunger-term ingest-term friendly-term))

    :forage (let [{:keys [friendly-home-pen]
                   :or {friendly-home-pen 1.2}} (or action-cfg {})
                  friendly-home (double (or (:friendly-home outcome) 0.0))]
              (* friendly-home-pen friendly-home))

    :return (let [{:keys [base-cost empty-home-pen cargo-thresh home-thresh hunger-gap-mult]
                   :or {base-cost 0.25
                        empty-home-pen 0.9
                        cargo-thresh 0.05
                        home-thresh 0.8
                        hunger-gap-mult 3.8}} (or action-cfg {})
                  friendly-home (double (or (:friendly-home outcome) 0.0))
                  cargo (double (or (:cargo outcome) 0.0))
                  h (double (or (:h outcome) (:hunger outcome) 0.0))
                  reserve (double (observe/clamp01 (or (:reserve-home outcome) 0.5)))
                  dist (double (or (:dist-home outcome) 1.0))
                  empty-home? (and (>= friendly-home home-thresh)
                                   (< cargo cargo-thresh))
                  ;; NEW: penalise empty-handed return when far from home
                  ;;      (nudges policy toward forage/pheromone first).
                  far-empty-pen (if (and (< cargo cargo-thresh) (> dist 0.30)) 0.60 0.0)
                  hunger-gap (max 0.0 (- h 0.35))
                  hunger-term (* (double hunger-gap-mult)
                                 hunger-gap
                                 (if (< cargo cargo-thresh) 1.0 0.4)
                                 (max 0.2 reserve))
                  base-scale (if (< cargo cargo-thresh)
                               (max 0.2 reserve)
                               0.25)
                  base (if (< cargo cargo-thresh)
                         (+ (* (double base-cost) base-scale)
                            hunger-term
                            far-empty-pen)  ;; NEW term added here
                         (* base-scale hunger-term))]
              (if empty-home?
                (+ base (double empty-home-pen))
                base))

    0.0))

(defn- info-gain
  [observation outcome]
  (let [nov-before (double (or (:novelty observation) 0.0))
        nov-after (double (or (:novelty outcome) nov-before))
        grad-before (double (or (:trail-grad observation) 0.0))
        grad-after (double (or (:trail-grad outcome) grad-before))
        novelty-gain (max 0.0 (- nov-before nov-after))
        gradient-gain (max 0.0 (- grad-after grad-before))]
    (+ novelty-gain (* 0.25 gradient-gain))))

(defn- colony-cost
  [action observation {:keys [reserve-thresh non-return-pen return-pen]}]
  (let [reserve (double (or (:reserve-home observation) 1.0))
        thresh (double (or reserve-thresh 0.2))
        deficit (max 0.0 (- thresh reserve))
        scaled (if (pos? deficit)
                 (+ 0.15 deficit)
                 0.0)]
    (if (zero? deficit)
      0.0
      (if (= action :return)
        (* (double (or return-pen 0.0)) scaled)
        (* (double (or non-return-pen 1.0)) scaled)))))

(defn- survival-cost
  [action observation outcome {:keys [hunger-thresh hunger-weight dist-weight ingest-buffer return-reduction]}]
  (let [hunger (double (or (:hunger outcome) (:h outcome) (:h observation) 0.0))
        dist (double (or (:dist-home outcome) (:dist-home observation) 0.0))
        ingest (double (or (:ingest outcome) (:ingest observation) 0.0))
        hunger-thresh (double (or hunger-thresh 0.55))
        hunger-weight (double (or hunger-weight 1.5))
        hunger-gap (max 0.0 (- hunger hunger-thresh))
        hunger-term (* hunger-weight hunger-gap)
        dist-term (* (max 0.0 dist) (double (or dist-weight 0.5)))
        ingest-term (max 0.0 (- (double (or ingest-buffer 0.3)) ingest))
        cost (+ hunger-term dist-term ingest-term)
        reduction (double (or return-reduction 0.4))]
    (if (= action :return)
      (* reduction cost)
      cost)))

(defn- reserve-delta
  [observation {:keys [reserve-thresh] :or {reserve-thresh 1.0}}]
  (let [reserve-thresh (double reserve-thresh)
        reserve (if (contains? observation :reserve-home)
                  (double (or (:reserve-home observation) reserve-thresh))
                  reserve-thresh)]
    (- reserve-thresh reserve)))

(defn- survival-pressure-from-observation
  [observation {:keys [hunger-thresh dist-weight ingest-buffer pressure-norm]
                :or {hunger-thresh 0.55
                     dist-weight 0.5
                     ingest-buffer 0.30
                     pressure-norm 2.0}}]
  (let [h (double (or (:hunger observation)
                      (:h observation)
                      0.0))
        dist (double (or (:dist-home observation) 0.0))
        ingest (double (or (:ingest observation) 0.0))
        hunger-term (max 0.0 (- h (double hunger-thresh)))
        dist-term (* (double dist-weight) (max 0.0 dist))
        ingest-term (max 0.0 (- (double ingest-buffer) ingest))
        norm (max 1e-6 (double pressure-norm))
        raw (+ hunger-term dist-term ingest-term)]
    (clamp (/ raw norm))))

(defn- survival-pressure-from-evaluations
  [evaluations {:keys [pressure-norm]
                :or {pressure-norm 2.0}}]
  (if (seq evaluations)
    (let [norm (max 1e-6 (double pressure-norm))
          max-survival (reduce (fn [m {:keys [result]}]
                                 (max m (double (or (:survival result) 0.0))))
                               0.0
                               evaluations)]
      (clamp (/ max-survival norm)))
    0.0))

(defn- couple-tau
  [base reserve-delta survival-pressure
   {:keys [tau-floor tau-cap tau-reserve-gain tau-survival-gain]
    :or {tau-floor 0.08
         tau-cap 1.5
         tau-reserve-gain 0.6
         tau-survival-gain 0.5}}]
  (let [base (double (or base 1.0))
        reserve-delta (bound (double reserve-delta) -1.0 1.0)
        reserve-gain (double tau-reserve-gain)
        survival-gain (double tau-survival-gain)
        tau-reserve (- base (* reserve-gain reserve-delta))
        tau-survival (- tau-reserve (* survival-gain (double (clamp survival-pressure))))]
    (-> tau-survival
        (max (double tau-floor))
        (min (double tau-cap)))))

(defn- expected-ambiguity
  [prec outcome]
  (let [acc (reduce-kv (fn [sum k v]
                         (if (and (number? v) (not= k :hunger))
                           (let [precision (double (get-in prec [:Pi-o k] 1.0))
                                 v (double (clamp (or v 0.0)))]
                             (+ sum (* (/ 1.0 (max precision 0.2)) v (- 1.0 v))))
                           sum))
                       0.0
                       outcome)]
    (* 0.5 acc)))

(defn- expected-free-energy
  [mu prec observation action {:keys [preferences action-costs efe]}]
  (let [outcome (predict-outcome mu observation action)
        prefs (ensure-preferences preferences)
        costs (ensure-action-costs action-costs)
        lambda (ensure-efe-lambda (get efe :lambda))
        colony-cfg (ensure-colony-config (get efe :colony))
        survival-cfg (ensure-survival-config (get efe :survival))
        risk (risk-from-preferences outcome prefs)
        ambiguity (expected-ambiguity prec outcome)
        info (info-gain observation outcome)
        colony (colony-cost action observation colony-cfg)
        survival (survival-cost action observation outcome survival-cfg)
        hunger (double (or (:hunger outcome)
                           (:h outcome)
                           0.0))
        prior (action-prior-cost action outcome hunger (get costs action))
        G (+ (* (:pragmatic lambda) risk)
             (* (:ambiguity lambda) ambiguity)
             (* (:colony lambda) colony)
             (* (:survival lambda) survival)
             prior
             (- (* (:info lambda) info)))]
    {:G G
     :risk risk
     :ambiguity ambiguity
     :info info
     :colony colony
     :survival survival
     :action-cost prior
     :outcome outcome}))

(defn- softmax
  [scores tau]
  (let [tau (max 1e-3 (double tau))]
    (if (seq scores)
      (let [max-logit (apply max (map :logit scores))
            exp-s (map #(math/exp (- (:logit %) max-logit)) scores)
            denom (double (reduce + exp-s))]
        (map (fn [{:keys [action result]} exp]
               [action (assoc result :p (if (zero? denom)
                                          (/ 1.0 (count scores))
                                          (/ exp denom)))])
             scores
             exp-s))
      [])))

;; -- Action set helpers -------------------------------------------------------

(defn- drop-actions
  "Remove actions in `banned` from vector `xs`."
  [xs banned]
  (vec (remove banned xs)))

(defn- admissible-actions
  "Stage filtering for the candidate action set.
   Mirrors the existing cargo+home clamps and near-home guard.
   NEW: forbid :return when (empty cargo) ∧ (not near home) ∧ (there's food here)."
  [{:keys [cargo friendly-home reserve-home local-food trail-grad]}
   base-actions]
  (let [epsilon-food 0.02
        gamma-trail  0.20
        near-home?   (>= friendly-home 0.50)
        on-home?     (>= friendly-home 0.90)
        ;; 1) Original cargo/home shaping
        base-admissible (if (> cargo 0.6)
                          (let [prioritized (filter #(#{:return :hold} %) base-actions)]
                            (if (seq prioritized) (vec prioritized) base-actions))
                          base-actions)
        home-trimmed (if on-home?
                       (drop-actions base-admissible #{:forage :pheromone})
                       base-admissible)
        guarded (if (and near-home?
                         (< reserve-home 0.2)
                         (< local-food epsilon-food)
                         (< trail-grad gamma-trail))
                  (drop-actions home-trimmed #{:forage})
                  home-trimmed)
        ;; 2) NEW gate: if empty-handed, away from home, and food is here,
        ;;    don't allow :return this tick (pick up first).
        cargo-thresh 0.05
        near-thresh  0.70
        food-here    0.10
        bad-empty-return? (and (< cargo cargo-thresh)
                               (< friendly-home near-thresh)
                               (>= local-food food-here))]
    (if bad-empty-return?
      (drop-actions guarded #{:return})
      guarded)))

;; -- Policy bias helpers ------------------------------------------------------

(defn- pheromone-bonus
  "Kept as-is; just named."
  [reserve-home]
  (cond
    (< reserve-home 0.2)  -0.6
    (< reserve-home 0.35) -0.4
    :else                 -0.15))

(defn- base-adjust
  "The cargo-conditioned per-action baseline tweak you had inline."
  [cargo action pher-bonus]
  (cond
    (< cargo 0.2)
    (case action
      :forage   -1.2
      :return    0.85
      :hold      0.60
      :pheromone (+ -0.45 pher-bonus)
      0.0)

    (> cargo 0.6)
    (case action
      :return   -0.75
      :hold      0.35
      :forage    0.40
      :pheromone 0.20
      0.0)

    :else
    (case action
      :hold      0.20
      :return   (if (< cargo 0.35) 0.25 0.10)
      :pheromone -0.05
      0.0)))

(defn- situation-adjust
  "All the cond-> branches you had for (novelty, trail, friendly-home, local-food, recent-gather, reserve-home, hunger, …)
   collected in one place. Same logic, just grouped."
  [{:keys [cargo home-prox novelty trail-grad friendly-home
           reserve-home local-food recent-gather hunger dist-home]}
   action]
  (let [adj1 (if (and (< cargo 0.1) (> home-prox 0.8))
               (case action :forage -0.6 :hold -0.3 :pheromone 0.6 :return 0.1 0.0) 0.0)
        adj2 (if (and (< novelty 0.45) (< trail-grad 0.3) (< friendly-home 0.7))
               (case action :pheromone -0.55 :hold 0.4 :return 0.25 :forage -0.25 0.0) 0.0)
        adj3 (if (and (> novelty 0.7) (< cargo 0.4) (< friendly-home 0.3))
               (case action :pheromone -0.25 :hold 0.15 0.0) 0.0)
        adj4 (if (and (> trail-grad 0.35) (> novelty 0.25) (< friendly-home 0.6))
               (case action :forage -0.35 :return 0.25 :hold 0.2 :pheromone 0.1 0.0) 0.0)
        adj5 (if (> reserve-home 0.6)
               (case action :hold 0.35 0.0) 0.0)
        adj6 (if (< local-food 0.05)
               (case action :forage -0.55 :pheromone (if (< trail-grad 0.2) -0.3 -0.1) :hold 0.65 :return 0.35 0.0) 0.0)
        adj7 (if (and (< local-food 0.05) (< recent-gather 0.05))
               (case action :forage 1.50 :hold 1.20 :pheromone -0.80 0.0) 0.0)
        adj8 (if (and (< reserve-home 0.25) (> cargo 0.35))
               (case action :return -0.7 :pheromone -0.25 :hold 0.25 :forage 0.35 0.0) 0.0)
        adj9 (if (and (> hunger 0.8) (> cargo 0.2))
               (case action :return -3.0 :forage 1.2 :hold 0.6 :pheromone -0.4 0.0) 0.0)]
    (+ adj1 adj2 adj3 adj4 adj5 adj6 adj7 adj8 adj9)))

(defn- visit-bias
  "The 'visit' prior you had (novelty/dist-home + recent-gather)."
  [{:keys [novelty dist-home recent-gather]} action]
  (let [base (case action :pheromone 0.60 :hold 0.12 :return 0.05 :forage -0.30 0.0)
        v1   (if (and (< novelty 0.20) (< dist-home 0.25))
               (case action :forage 0.85 :hold -0.35 :return -0.35 :pheromone 0.65 0.0) 0.0)
        v2   (if (or (> novelty 0.70) (> dist-home 0.70))
               (case action :forage -0.40 :hold 0.12 :return 0.05 0.0) 0.0)
        v3   (if (and (< recent-gather 0.15) (> dist-home 0.25))
               (case action :forage -4.00 :pheromone 0.60 0.0) 0.0)]
    (+ base v1 v2 v3)))

;; -- White-space (low-signal) detector & bias --------------------------------

(defn- white-space?
  "Heuristic: we're in a low-signal patch (little food, weak trail, no recent gather),
   not strongly at home, and not already clearly following something."
  [{:keys [local-food trail-grad recent-gather friendly-home novelty]}]
  (and (< (double local-food) 0.05)
       (< (double trail-grad) 0.22)
       (< (double recent-gather) 0.08)
       (< (double friendly-home) 0.70)
       (> (double novelty) 0.25)))

(defn- white-space-adjust
  "Gentle nudge away from pointless foraging, toward laying trail or getting bearings.
   Tuned small because we already have other guards."
  [action]
  (case action
    :forage    0.90   ;; raises G (worse) → reduces p(forage)
    :hold      0.35
    :return   -0.40
    :pheromone -0.45
    0.0))

;; -- Tau policy ---------------------------------------------------------------

(defn- choose-tau
  "Your existing couple-tau + hunger clamp + nest clamps + (optional) white-space boost."
  [{:keys [base-tau reserve-delta survival-pressure precision-cfg
           friendly-home cargo local-food trail-grad hunger]}]
  (let [computed (couple-tau base-tau reserve-delta survival-pressure precision-cfg)
        high-h?  (> hunger 0.8)
        limited  (if high-h? (min computed 0.8) computed)
        cap      (double (or (:tau-cap precision-cfg) 1.5))
        nest-clamp (cond
                     (and (>= friendly-home 0.95) (> cargo 0.25)) 0.60
                     (and (>= friendly-home 0.80) (> cargo 0.10)) 0.75
                     :else nil)
        clamped  (if nest-clamp (min limited nest-clamp) limited)
        ;; your old "nest-boost" when empty & poor signals near nest:
        nest-boost (when (and (>= friendly-home 0.90)
                              (< cargo 0.10)
                              (< local-food 0.02)
                              (< trail-grad 0.20))
                     1.15)
        boosted  (if nest-boost (min cap (max clamped nest-boost)) clamped)]
    boosted))

(defn choose-action
  "Evaluate candidate actions and sample via softmax over -G/tau."
  ([mu prec observation]
   (choose-action mu prec observation {}))
  ([mu prec observation {:keys [actions preferences action-costs efe efe-lambda precision] :as _opts}]
   (let [prefs         preferences
         costs         action-costs
         lambda        (ensure-efe-lambda (or efe-lambda (get-in efe [:lambda])))
         efe-config    (-> (or efe {}) (assoc :lambda lambda))
         colony-cfg    (ensure-colony-config (get efe-config :colony))
         survival-cfg  (ensure-survival-config (get efe-config :survival))
         precision-cfg (or precision {})

         ;; Pull all observation fields once:
         cargo          (double (or (:cargo observation) 0.0))
         home-prox      (double (or (:home-prox observation) 0.0))
         reserve-home   (double (or (:reserve-home observation) 0.5))
         local-food     (double (or (:food observation) 0.0))
         trail-grad     (double (or (:trail-grad observation) 0.0))
         novelty        (double (or (:novelty observation) 0.0))
         friendly-home  (double (or (:friendly-home observation) 0.0))
         dist-home      (double (or (:dist-home observation) 0.0))
         recent-gather  (double (or (:recent-gather observation) 0.0))
         hunger         (double (or (:h observation) (:h mu) 0.0))

         ;; mode and candidate actions
         mode (or (:mode observation) :outbound)
         base-actions (vec (or (get actions-by-mode mode) default-actions))

         admissible     (admissible-actions
                         {:cargo cargo :friendly-home friendly-home
                          :reserve-home reserve-home :local-food local-food
                          :trail-grad trail-grad}
                         base-actions)

         ;; EFE base evaluation
         efe-opts       {:preferences prefs :action-costs costs :efe efe-config}
         evaluations    (mapv (fn [action]
                                {:action action
                                 :result (expected-free-energy mu prec observation action efe-opts)})
                              admissible)

         ;; Stack the three bias layers you had: base-adjust, situation-adjust, visit-bias
         pher-bonus     (pheromone-bonus reserve-home)
         obs-mini       {:cargo cargo :home-prox home-prox :novelty novelty :trail-grad trail-grad
                         :friendly-home friendly-home :reserve-home reserve-home
                         :local-food local-food :recent-gather recent-gather
                         :hunger hunger :dist-home dist-home}
         evaluations  (mapv (fn [{:keys [action result] :as item}]
                              (let [;; get current mode from observation (default :outbound)
                                    mode        (or (:mode observation) :outbound)
                                    ;; small, interpretable bias by mode (negative → lowers G → more likely)
                                    mode-adjust (case mode
                                                  :outbound  (case action
                                                               :forage    -0.35
                                                               :return     0.35
                                                               :hold       0.10
                                                               :pheromone -0.05
                                                               0.0)
                                                  :homebound (case action
                                                               :return    -0.50
                                                               :forage     0.40
                                                               :hold       0.10
                                                               :pheromone -0.10
                                                               0.0)
                                                  :maintain  (case action
                                                               :pheromone -0.40
                                                               :hold      -0.20
                                                               :return     0.15
                                                               :forage     0.20
                                                               0.0))
                                    adj (+ (base-adjust cargo action pher-bonus)
                                           (situation-adjust obs-mini action)
                                           (visit-bias (select-keys obs-mini [:novelty :dist-home :recent-gather]) action)
                                           ;; optional white-space nudge:
                                           (if (white-space? {:local-food local-food
                                                              :trail-grad trail-grad
                                                              :recent-gather recent-gather
                                                              :friendly-home friendly-home
                                                              :novelty novelty})
                                             (white-space-adjust action)
                                             0.0)
                                           ;; ← add the mode-aware tilt last so it's easy to see/tune
                                           mode-adjust)]
                                (-> item
                                    (assoc-in  [:result :adjust] adj)
                                    (update-in [:result :G] + adj))))
                            evaluations)

         ;; Tau coupling (unchanged behaviour, but factored)
         reserve-delta     (reserve-delta observation colony-cfg)
         survival-pressure (max (survival-pressure-from-observation observation survival-cfg)
                                (survival-pressure-from-evaluations evaluations survival-cfg))
         base-tau          (:tau prec 1.0)
         tau               (choose-tau {:base-tau base-tau
                                        :reserve-delta reserve-delta
                                        :survival-pressure survival-pressure
                                        :precision-cfg precision-cfg
                                        :friendly-home friendly-home
                                        :cargo cargo
                                        :local-food local-food
                                        :trail-grad trail-grad
                                        :hunger hunger})

         ;; Softmax
         scores (map (fn [{:keys [action result] :as item}]
                       (let [base-logit (/ (- (:G result)) tau)
                             aif-logit  (efe-tilt observation action {:lambda 0.6})]
                         (assoc item :logit (+ base-logit aif-logit))))
                     evaluations)
         dist     (softmax scores tau)
         policies (into {} dist)
         best     (if (seq dist)
                    (->> dist (apply max-key (fn [[_ {:keys [p]}]] p)) first)
                    (first admissible))]
     {:action   best
      :policies policies
      :tau      tau})))

