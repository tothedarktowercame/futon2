(ns futon2.aif.want-free-energy
  "G' (plan by the chance the wants are met): pure port of
  DarkTower/WarMachine/WantFreeEnergy.lean, WantFreeEnergyBenefit.lean and
  WantFreeEnergyWitness.lean (mathlib4 worktree c14-cascade-outcome, branch
  c14/cascade-outcome-type), in the shape of futon2.aif.r1-belief.

  G' replaces ONLY the pragmatic (risk) term of the canonical cascade G
  (CascadeEFE.stepG / totalG, futon2.aif.cascade-g / cascade-coapply-g): the
  risk `KL(push q a || C)` is replaced by the want term

      wantTerm(q, w) = -log chance(q, w),   chance(q, w) = sum_o q(o) * c(o)

  where c(o) in (0, 1] is the want effect on outcomes (Tull, Kleiner & St
  Clere Smithe 2023, Cor. 38 / Prop. 37; Parr & Friston 2019's generalised
  free energy). The epistemic term (ambiguity; Millidge, Tschantz & Buckley
  2021) is kept by statement, unchanged, and novelty is subtracted at the
  cascade level exactly as CascadeCoApplyGNovelty.scoreCascadeCoN does.

  All chance arithmetic is exact (Clojure ratios) wherever the Lean uses real
  literals; only log is evaluated in floating point, as the F8 readback form
  requires (Lean keeps log symbolic). No wiring into the click, selection, or
  runner: data and pure functions only."
  (:require [clojure.set :as set]
            [futon2.aif.r1-belief :as r1]))

;;; ## Wants as effects (WantFreeEnergy.WantEffect)

(defn want-effect
  "Validate a want effect: a map from every outcome to a value in (0, 1]
  (the Lean structure's `pos` and `le_one` fields). 1 for an outcome that
  meets every want clause, smaller for outcomes the wants count as worse.
  Hard wants (value 0) are deferred (Joe, 2026-11-25): a value of 0 or
  greater than 1 is REFUSED, never clamped. Returns the effect."
  [c]
  (when-not (and (map? c) (seq c))
    (throw (ex-info "want effect must be a nonempty map {outcome value}"
                    {:type :invalid-want-effect :c c})))
  (doseq [[outcome v] c]
    (when-not (number? v)
      (throw (ex-info "want effect values must be numbers"
                      {:type :invalid-want-effect :outcome outcome :value v})))
    (when-not (and (pos? v) (<= v 1))
      (throw (ex-info "want effect values must be in (0, 1]: hard wants (0) are deferred and no value may exceed 1"
                      {:type :invalid-want-effect :outcome outcome :value v}))))
  c)

(defn want-effect-prod
  "WantEffect.prod: two independent want parts as one want over [a b] pairs;
  values multiply."
  [w1 w2]
  (want-effect w1)
  (want-effect w2)
  (into {} (for [[a va] w1 [b vb] w2]
             [[a b] (* va vb)])))

;;; ## Predictions (ProbabilityKernel Unit O, as plain maps)

(defn check-prediction
  "Validate a prediction over the want effect's outcome set: a map
  {outcome prob} covering exactly (keys w), nonnegative, summing to EXACTLY 1
  (the Lean kernel's `normalised` field). A non-normalised prediction is
  refused, never renormalised. Returns the prediction."
  [q w]
  (when-not (map? q)
    (throw (ex-info "prediction must be a map {outcome prob}"
                    {:type :invalid-prediction :q q})))
  (let [outcomes (set (keys w))
        missing (set/difference outcomes (set (keys q)))
        extra (set/difference (set (keys q)) outcomes)]
    (when (or (seq missing) (seq extra))
      (throw (ex-info "prediction must assign a probability to every outcome and nothing else"
                      {:type :invalid-prediction :missing missing :extra extra})))
    (doseq [[outcome p] q]
      (when-not (and (number? p) (not (neg? p)))
        (throw (ex-info "prediction entries must be nonnegative numbers"
                        {:type :invalid-prediction :outcome outcome :prob p}))))
    (when-not (== 1 (reduce + 0 (vals q)))
      (throw (ex-info "prediction must sum to 1 (non-normalised predictions are refused, never repaired)"
                      {:type :invalid-prediction :sum (reduce + 0 (vals q))})))
    q))

(defn point
  "CascadeEFE.point: the certain prediction on outcome o."
  [outcomes o]
  (into {} (map (fn [x] [x (if (= x o) 1 0)]) outcomes)))

(defn check-kernel
  "Validate a kernel {x {y prob}}: every row's mass lies inside the Y
  support (entries omitted are zero, as a Lean finiteKernel is 0 off its
  listed support), nonnegative, summing to exactly 1. Returns the kernel."
  [kernel y-support]
  (when-not (and (map? kernel) (seq kernel))
    (throw (ex-info "kernel must be a nonempty map {x {y prob}}"
                    {:type :invalid-kernel :kernel kernel})))
  (let [support (set y-support)]
    (doseq [[x row] kernel]
      (when-not (map? row)
        (throw (ex-info "kernel rows must be maps {y prob}"
                        {:type :invalid-kernel :x x :row row})))
      (let [extra (set/difference (set (keys row)) support)]
        (when (seq extra)
          (throw (ex-info "kernel row assigns mass outside its support"
                          {:type :invalid-kernel :x x :extra extra}))))
      (doseq [[y p] row]
        (when-not (and (number? p) (not (neg? p)))
          (throw (ex-info "kernel entries must be nonnegative numbers"
                          {:type :invalid-kernel :x x :y y :prob p}))))
      (when-not (== 1 (reduce + 0 (vals row)))
        (throw (ex-info "kernel rows must sum to 1"
                        {:type :invalid-kernel :x x :sum (reduce + 0 (vals row))})))))
  kernel)

(defn push
  "CascadeEFE.push: the prediction q (over X) pushed through the kernel
  a (X -> Y), giving a prediction over Y. Exact arithmetic."
  [q a y-support]
  (check-prediction q (into {} (map (fn [x] [x 1]) (keys a))))
  (check-kernel a y-support)
  (into {} (map (fn [y]
                  [y (reduce + 0 (map (fn [x]
                                        (* (get q x) (get-in a [x y] 0)))
                                      (keys a)))])
                y-support)))

(defn prediction
  "CascadeEFE.prediction: q rolled forward through the transition kernel b
  (X -> X), t times."
  [q b t]
  (let [support (vec (keys q))]
    (check-kernel b support)
    (nth (iterate #(push % b support) q) t)))

;;; ## The pragmatic term (WantFreeEnergy.chance, wantTerm)

(defn chance
  "WantFreeEnergy.chance: the (soft) chance that the wants are met under
  prediction q, sum_o q(o) * c(o). Exact (rational) arithmetic wherever the
  inputs are exact."
  [q w]
  (want-effect w)
  (check-prediction q w)
  (reduce + 0 (map (fn [[o p]] (* p (get w o))) q)))

(defn want-term
  "WantFreeEnergy.wantTerm: -log chance. log is transcendental, so this one
  step is floating point (Math/log), as the F8 readback form requires; the
  chance it takes is exact."
  [q w]
  (- (Math/log (double (chance q w)))))

;;; ## The epistemic term, kept by statement (CascadeEFE.entropy, ambiguity)

(defn- entropy
  "CascadeEFE.entropy of one kernel row: -sum_o p(o) log p(o), with
  0 log 0 = 0 by convention. Floating point (log)."
  [row]
  (reduce + 0.0
          (for [[_ p] row :when (pos? (double p))]
            (- (* (double p) (Math/log (double p)))))))

(defn ambiguity
  "CascadeEFE.ambiguity: sum_s q(s) * entropy(a(s, .)). The SAME ambiguity
  the canonical G computes (futon2.aif.categorical-ambiguity), kept by
  statement (Millidge et al. 2021); G' replaces only the risk term."
  [q a y-support]
  (check-prediction q (into {} (map (fn [x] [x 1]) (keys a))))
  (check-kernel a y-support)
  (reduce + 0.0
          (for [[s p] q]
            (* (double p) (entropy (get a s))))))

;;; ## G' for one step and over a horizon

(defn step-g'
  "WantFreeEnergy.stepG': the want term of the predicted outcome plus
  ambiguity. q over states X, a the observation kernel X -> O, w the want
  effect on O."
  [q a w]
  (let [outcomes (vec (keys w))]
    (+ (want-term (push q a outcomes) w)
       (ambiguity q a outcomes))))

(defn total-g'
  "WantFreeEnergy.totalG': sum over t in {1..T} of stepG' applied to the
  t-step prediction (time t scores observation t+1's prediction, after one
  transition application for t = 0, as CascadeEFE.totalG). WANTS is a vector
  of T want effects, one per step."
  [q b a T wants]
  (when-not (and (vector? wants) (= T (count wants)))
    (throw (ex-info "total-g' needs one want effect per horizon step"
                    {:type :invalid-horizon :T T :wants (count wants)})))
  (reduce + 0.0
          (map (fn [t w] (step-g' (prediction q b t) a w))
               (range 1 (inc T)) wants)))

(defn per-step
  "WantFreeEnergyWitness.perStep: the horizon total divided by the horizon H
  (the normalisation Joe called sensible, 2026-11-25), matching
  Requirements.GNormalization's per-step rates."
  [total H]
  (when-not (and (integer? H) (pos? H))
    (throw (ex-info "per-step needs a positive integer horizon"
                    {:type :invalid-horizon :H H})))
  (double (/ total H)))

;;; ## The benefit modality (WantFreeEnergyBenefit)

(defn mix-kernel
  "WantFreeEnergyBenefit.mixK: a mixture of two predictions over the same
  support, pD * p1 + (1 - pD) * p0 (the predicted rung after a step is
  pD . predictRung + (1 - pD) . belief; see BenefitFactoredG.one_step_rung)."
  [pD p1 p0]
  (when-not (and (number? pD) (<= 0 pD 1))
    (throw (ex-info "mixture weight pD must be a probability in [0, 1]"
                    {:type :invalid-weight :pD pD})))
  (let [support (set (keys p1))]
    (when-not (= support (set (keys p0)))
      (throw (ex-info "mixture predictions must share their support"
                      {:type :invalid-prediction :p1 (keys p1) :p0 (keys p0)})))
    (into {} (map (fn [k] [k (+ (* pD (get p1 k)) (* (- 1 pD) (get p0 k)))])
                  support))))

;;; ## G' over a candidate cascade (scoreCascadeCo', scoreCascadeCoN')

(defn score-cascade-co'
  "WantFreeEnergy.scoreCascadeCo': G' over a candidate cascade, with G's
  structural refusals and NO want-based refusal (chance is always positive).
  CANDIDATE is {:initial q :transition b :observation a :horizon H
  :wants [w per step]}. Returns {:status :ok :g' value :steps [...]} or a
  refusal map. Pure: the caller supplies the kernels the canonical G would
  compute; nothing is inferred from the click."
  [{:keys [initial transition observation horizon wants] :as candidate}]
  (cond
    (nil? initial)
    {:status :refused :reason :missing-interpretation :candidate (:id candidate)}

    (or (nil? transition) (nil? observation) (nil? horizon) (nil? wants))
    {:status :refused :reason :missing-model :candidate (:id candidate)}

    :else
    (try
      (let [outcomes (vec (keys (first wants)))
            steps (mapv (fn [t w]
                          {:tau t
                           :predicted-outcome (push (prediction initial transition t)
                                                    observation outcomes)
                           :want-term (want-term (push (prediction initial transition t)
                                                       observation outcomes) w)
                           :ambiguity (ambiguity (prediction initial transition t)
                                                 observation outcomes)})
                        (range 1 (inc horizon)) wants)
            g' (reduce + 0.0 (map #(+ (:want-term %) (:ambiguity %)) steps))]
        {:status :ok :candidate (:id candidate) :horizon horizon
         :g' g' :per-step (per-step g' horizon) :steps steps})
      (catch clojure.lang.ExceptionInfo e
        {:status :refused :reason (:type (ex-data e)) :candidate (:id candidate)
         :detail (ex-data e)}))))

(defn score-cascade-co-n'
  "WantFreeEnergy.scoreCascadeCoN': G' minus expected parameter novelty, as
  scoreCascadeCoN does for G. The novelty arrives pre-computed by the same
  code that computes it for the canonical G
  (futon2.aif.cascade-coapply-g/expected-novelty totals over the candidate's
  own counts); this namespace does not reimplement pattern counting."
  [candidate novelty-total]
  (when-not (and (number? novelty-total) (not (neg? novelty-total)))
    (throw (ex-info "novelty total must be a nonnegative number"
                    {:type :invalid-novelty :novelty novelty-total})))
  (let [scored (score-cascade-co' candidate)]
    (if (= :ok (:status scored))
      (-> scored
          (assoc :novelty novelty-total)
          (assoc :g'-novelty (- (:g' scored) (double novelty-total))))
      scored)))

;;; ## The witness wants (WantFreeEnergyWitness)

(def met-want
  "WantFreeEnergyWitness.metWant: 9/10 on met, 1/10 on not met."
  {true 9/10 false 1/10})

(def rung-want
  "WantFreeEnergyWitness.rungWant: monotone wants over the R1 rungs."
  {:none 1/10 :available 1/5 :used 2/5 :witnessed 7/10 :realised 1})

(defn bern
  "PreferenceSurpriseUncertain.bern: the Bernoulli prediction {true theta,
  false 1 - theta}."
  [theta]
  (when-not (and (number? theta) (<= 0 theta 1))
    (throw (ex-info "bern needs a probability in [0, 1]"
                    {:type :invalid-prediction :theta theta})))
  {true theta false (- 1 theta)})

(defn uniform-rung-prediction
  "WantFreeEnergyWitness.uniformR: R1 uniform over the five rungs."
  []
  (r1/uniform-belief))
