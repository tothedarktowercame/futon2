(ns futon2.aif.r1-three-axes
  "Pure port of R1 over the benefit ladder's three axes (mathlib4 worktree
  c14-cascade-outcome, branch c14/cascade-outcome-type):

  - DarkTower/WarMachine/R1ThreeAxes.lean        : Belief over any finite
    state; lik, evid, post, update; BenefitState (avail, habit, reach)
    with habit = true -> avail = capability; the three marginals.
  - DarkTower/WarMachine/R1ThreeAxesWitness.lean : the witness numbers
    (3/7 on :pending, reach unmoved at 1/3); see the test namespace.

  Joe's ruling of 2026-11-24 (futon3c holes/missions/M-cascade-live-causal.md,
  \"Evidence bar\"): capability and habit are different axes — you need the
  capability before you can have the habit, but they are not aligned on one
  axis. R1's old five rungs (futon2.aif.r1-belief) fit none of these, so this
  namespace holds the belief over the 15 valid triples instead. All arithmetic
  is exact (Clojure ratios), as the Lean uses real literals.

  A state is a map {:avail one-of-#{:none :promised :pending :capability}
                    :habit boolean
                    :reach one-of-#{:none :witness :realised}}
  with the constraint :habit true -> :avail = :capability. A belief is a map
  from each of the 15 states to an exact rational, summing to 1. No wiring
  into the click, selection, or runner: data and pure functions only."
  (:require [clojure.set :as set]))

;;; ## The three axes (R1ThreeAxes.Avail / Reach / the constraint)

(def avail-values
  "Availability, weakest first: none < PROMISED < PENDING < CAPABILITY."
  [:none :promised :pending :capability])

(def reach-values
  "Reach, weakest first: none < WITNESS < REALISED."
  [:none :witness :realised])

(def states
  "R1ThreeAxes.BenefitState as a vector: the triples satisfying the
  constraint. 4 x 2 x 3 = 24 triples minus the 9 with :habit true and
  :avail not :capability gives card_benefitState = 15."
  (vec (for [avail avail-values
             habit [false true]
             reach reach-values
             :when (or (not habit) (= :capability avail))]
         {:avail avail :habit habit :reach reach})))

(defn check-state
  "A benefit state is one of the 15 triples: the constraint :habit true ->
  :avail = :capability is in the type (R1ThreeAxes.habit_implies_capability),
  so any other triple is refused (ex-info :invalid-state)."
  [state]
  (when-not (and (map? state)
                 (contains? (set avail-values) (:avail state))
                 (contains? #{true false} (:habit state))
                 (contains? (set reach-values) (:reach state))
                 (or (not (:habit state)) (= :capability (:avail state))))
    (throw (ex-info "benefit state must be {:avail <4 values> :habit bool :reach <3 values>} with :habit true -> :avail :capability"
                    {:type :invalid-state :state state})))
  state)

;;; ## Slow belief (R1ThreeAxes.Belief)

(defn check-belief
  "Validate a slow belief: a map from every one of the 15 states to a
  nonnegative number, summing to exactly 1. Returns the belief; refuses
  (ex-info) otherwise, as the Lean structure's `nonneg`/`sum_one` fields rule
  the value out. Also refuses any state that violates the constraint, so no
  belief can put mass on HABIT without CAPABILITY."
  [belief]
  (when-not (map? belief)
    (throw (ex-info "slow belief must be a map {state prob}"
                    {:type :invalid-belief :belief belief})))
  (doseq [state (keys belief)]
    (check-state state))
  (let [missing (set/difference (set states) (set (keys belief)))
        extra (set/difference (set (keys belief)) (set states))]
    (when (or (seq missing) (seq extra))
      (throw (ex-info "slow belief must assign a probability to every benefit state and nothing else"
                      {:type :invalid-belief :missing missing :extra extra})))
    (doseq [[state p] belief]
      (when-not (and (number? p) (not (neg? p)))
        (throw (ex-info "slow belief entries must be nonnegative numbers"
                        {:type :invalid-belief :state state :prob p}))))
    (when-not (== 1 (reduce + 0 (vals belief)))
      (throw (ex-info "slow belief must sum to 1"
                      {:type :invalid-belief :sum (reduce + 0 (vals belief))})))
    belief))

(defn uniform-belief
  "R1ThreeAxesWitness.uniform: 1/15 on every state."
  []
  (zipmap states (repeat 1/15)))

;;; ## Likelihood (the A channel)

(defn- check-prob
  "A likelihood entry is a probability: a number in [0, 1]. Refuses
  otherwise (a Lean ProbabilityKernel mass is nonnegative and at most 1,
  since each row sums to 1)."
  [state kind p]
  (when-not (and (number? p) (<= 0 p 1))
    (throw (ex-info "likelihood must be a probability in [0, 1]"
                    {:type :invalid-likelihood :state state :kind kind :prob p})))
  p)

(defn likelihood
  "Accept the likelihood A either as a function (fn [state record-kind] ->
  prob) or as a nested map {state {kind prob}}. Returns a function
  [state record-kind] -> prob that refuses entries outside [0, 1]."
  [A]
  (cond
    (fn? A) (fn [state kind] (check-prob state kind (A state kind)))
    (map? A) (fn [state kind]
               (check-prob state kind (get-in A [state kind] 0)))
    :else (throw (ex-info "likelihood must be a function or a nested map"
                          {:type :invalid-likelihood :A A}))))

(defn- record-kind
  "Records may be plain values (record kinds) or maps with a :kind key."
  [record]
  (if (map? record) (:kind record) record))

(defn lik
  "R1ThreeAxes.lik, accepting records as plain kinds or {:kind ...} maps."
  [A records state]
  (let [Af (likelihood A)]
    (reduce * 1 (map #(Af state (record-kind %)) records))))

(defn evid
  "R1ThreeAxes.evid: the evidence for a click's records under `belief`."
  [belief A records]
  (check-belief belief)
  (reduce + 0 (map (fn [state] (* (get belief state) (lik A records state))) states)))

(defn post
  "R1ThreeAxes.post: the posterior over states after a click's records
  (Bayes, once). If the evidence is zero the records are impossible under
  the prior at every state, and the Lean update's hypothesis `0 < evid`
  cannot be met: refuse (ex-info :zero-evidence), never divide."
  [belief A records]
  (let [e (evid belief A records)]
    (when-not (pos? e)
      (throw (ex-info "zero evidence: records impossible under the prior at every state"
                      {:type :zero-evidence :records records})))
    (into {} (map (fn [state]
                    [state (/ (* (get belief state) (lik A records state)) e)])
                  states))))

(defn update-belief
  "R1ThreeAxes.update: the slow belief after a click's records. This is
  `post`; the name mirrors the Lean. `update_cons` (a test in the test
  namespace) guarantees updating record by record equals updating with all
  records at once, so no record counts twice."
  [belief A records]
  (post belief A records))

;;; ## Each axis as a marginal

(defn avail-marginal
  "R1ThreeAxes.availMarginal: the total belief mass on states whose
  availability is `a`."
  [belief a]
  (check-belief belief)
  (reduce + 0 (map (fn [state] (if (= a (:avail state)) (get belief state) 0)) states)))

(defn reach-marginal
  "R1ThreeAxes.reachMarginal: the total belief mass on states whose reach
  is `r`."
  [belief r]
  (check-belief belief)
  (reduce + 0 (map (fn [state] (if (= r (:reach state)) (get belief state) 0)) states)))

(defn habit-marginal
  "R1ThreeAxes.habitMarginal: the total belief mass on states whose habit
  flag is `h`."
  [belief h]
  (check-belief belief)
  (reduce + 0 (map (fn [state] (if (= h (:habit state)) (get belief state) 0)) states)))

;;; ## Axis readings: Pearl's rule on one axis

(def axis-getters
  "The axis of an axis-reading."
  {:avail :avail :reach :reach :habit :habit})

(defn axis-reading
  "R1ThreeAxesWitness.pendingReading generalised to any axis: a reader
  claiming value v0 on axis `axis` with reliability r is a channel from the
  true state to the reader's say-so. For the record :says-true the likelihood
  is r where the state has v0 on that axis and 1 - r elsewhere; for
  :says-false the complement (Pearl's virtual evidence, as in
  futon2.aif.r1-belief/reading-channel).

  r is REQUIRED and must be a number in [0, 1]. Refuses (ex-info) a missing
  r, a non-number r, r outside [0, 1], an unknown axis, or a value not in
  the axis's range. Exact ratios throughout."
  ([axis v0]
   (throw (ex-info "axis-reading requires an explicit reliability r in [0, 1] (no default)"
                   {:type :missing-reliability :axis axis :value v0})))
  ([axis v0 r]
   (when-not (contains? axis-getters axis)
     (throw (ex-info "axis-reading: axis must be one of :avail :reach :habit"
                     {:type :invalid-axis :axis axis})))
   (let [valid (case axis
                 :avail (set avail-values)
                 :reach (set reach-values)
                 :habit #{true false})]
     (when-not (contains? valid v0)
       (throw (ex-info "axis-reading: claimed value must be in the axis's range"
                       {:type :invalid-axis-value :axis axis :value v0}))))
   (when-not (and (number? r) (<= 0 r 1))
     (throw (ex-info "axis-reading: reliability must be a number in [0, 1]"
                     {:type :invalid-reliability :r r})))
   (fn [state kind]
     (let [match? (= (get state (get axis-getters axis)) v0)]
       (case kind
         :says-true (if match? r (- 1 r))
         :says-false (if match? (- 1 r) r))))))
