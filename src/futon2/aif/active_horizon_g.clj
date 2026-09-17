(ns futon2.aif.active-horizon-g
  "H7a of SPEC-flat-removal-and-cascade-decision (Joe approved 2026-09-17,
  p4ng 462aa79): action-dependent observation channels — the acting pattern
  selects what is observed.  Aligned to mathlib4 3b19f6225e,
  DarkTower.WarMachine.EpistemicValue:

  - Lean ActiveModel/activeHorizonEFE: G(π) = Σ_{τ=1}^{T} [risk_τ +
    ambiguity_τ], where the observation at step τ goes through the channel of
    the action taken into that step, σ(τ−1).
  - Lean activePredictedOutcome = Σ_s A_σ(τ−1)(s,o)·Q(s_τ|π); here A is
    cascade-model-manifest/token-likelihood (Lean
    TokenObservation.tokenLikelihood) restricted to the step's observed
    tokens — the manifest's public function, not a copy.
  - Lean activeStepRisk is Lean OutcomeRiskKL.outcomeRisk applied to the
    channel-predicted outcome; horizon-g-sparse's pointwise reduction of it
    is private to cascade-model-manifest and cannot take a per-step channel,
    so the smallest aligned form is implemented here (same Lean declaration,
    same ⊤-iff-zero-preferred-mass semantics).
  - Lean activeStepAmbiguity = Σ_s Q(s_τ|π)·H[A_σ(τ−1)(·|s)] (activeRowEntropy);
    the per-state row comes from the manifest's observation-distribution.
  - Lean activeHorizonEFE_eq_horizonEFE: with NO masked facts the channel is
    the whole universe, and at zero adjudication rates every row is the
    deterministic observation of the state, so active-horizon-g reduces to
    cascade-model-manifest/horizon-g-sparse (Lean
    TokenObservation.tokenLikelihood_checkable).

  D3 amendment 1 (q0 spread): every masked (unknown) fact f spreads q0 over
  f⁺/f⁻ by its DECLARED prior from :unknown-prior; a missing prior is the
  typed refusal :missing-unknown-prior, never 1/2 by default.

  Channels.  The default channel observes the established tokens of the
  universe EXCEPT the masked facts.  Lean ActiveModel observes step τ through
  the channel of ONE acting pattern, σ(τ−1): the first pattern enabled by
  the P10 first-enabled rule over the step's precedence, checks and ordinary
  patterns alike (a check is enabled while its fact is masked).  If the
  acting pattern is a check ({:kind :check :opens f …}, as produced by
  futon2.aif.check-candidates), the step's transition is doing-nothing's
  (rolloutState_eq_of_B) and f's channel opens for that step; if an ordinary
  pattern acts, its transition is the manifest's cascade-kernel (rollout
  with checks removed) and the channel is the default.  A check's θ
  (:theta, required by check-candidates) is its probability of returning an
  answer: the step's observation is the independent mixture over whether the
  acting check answers, each channel's row built by token-likelihood over
  the default tokens plus the answered fact.  The masked set does NOT shrink
  after a check inside the rollout — that would be belief-conditioned
  branching, which Joe's H7a approval explicitly left open — so a check
  that stays first-enabled acts again at later steps.

  Support grows as 2^k in the number of unknown facts, so :cap (an input,
  no default) bounds them; exceeding it is the typed refusal
  :unknown-facts-over-cap."
  (:require [clojure.set :as set]
            [futon2.aif.cascade-model-manifest :as m]))

(defn- refusal? [x] (and (map? x) (contains? x :status)))

(defn- check-pattern? [p] (= :check (:kind p)))

(defn- pattern-enabled?
  "P10 first-enabled rule at the step's belief: a CHECK is enabled while its
  fact is masked (its guard {:unknown #{f}} tests unknown-ness, a property
  of the belief — the manifest's guard-holds? reads only the clauses shape,
  so this is the smallest aligned form for check guards); an ordinary
  pattern is enabled when the manifest's guard-holds? holds at some
  positive-mass state of the step's prior."
  [p masked q-prev]
  (if (check-pattern? p)
    (contains? masked (:opens p))
    (some (fn [[s mass]] (and (pos? mass) (true? (m/guard-holds? p s)))) (seq q-prev))))

(defn- step-acting-check
  "THE acting check of the step, Lean ActiveModel's σ(τ−1): ONE acting
  pattern per step, the first enabled pattern of the step's precedence under
  the P10 rule (pattern-enabled?) — checks and ordinary patterns alike.  If
  the acting pattern is a check, the step's transition is doing-nothing's
  (rolloutState_eq_of_B) and that fact's channel opens; if it is an ordinary
  pattern (or nothing) acts, the channel is the default.  Returns the acting
  check {:fact :theta} or nil.  The masked set does NOT shrink after a
  check inside the rollout — that would be belief-conditioned branching,
  which Joe's H7a approval explicitly left open — so a check that stays
  first-enabled acts again at later steps."
  [precedence masked q-prev]
  (when-let [p (first (filter #(pattern-enabled? % masked q-prev) precedence))]
    (when (check-pattern? p)
      (let [p' (m/with-pattern-theta p)]
        {:fact (:opens p') :theta (:theta p')}))))

(defn- exact-prob? [x] (and (or (ratio? x) (integer? x)) (<= 0 x 1)))

(defn- spread-unknowns
  "D3 amendment 1: split q0 over every masked fact by its declared prior.
  Lean analogue: the belief (1/2, 1/2) of EpistemicValue's fixture spread
  per fact.  A missing prior refuses; nothing defaults."
  [q0 masked prior]
  (or (when-let [missing (first (filter #(not (contains? prior %)) masked))]
        {:status :missing :kind :missing-unknown-prior :fact missing})
      (when-let [bad (first (for [f masked
                                  :let [p (get prior f)]
                                  :when (not (exact-prob? p))]
                              f))]
        {:status :missing :kind :invalid-unknown-prior :fact bad
         :value (get prior bad)})
      (reduce (fn [q f]
                (let [p (get prior f)]
                  (reduce-kv (fn [acc s mass]
                               (-> acc
                                   (update (set/union s #{f}) (fnil + 0) (* mass p))
                                   (update s (fnil + 0) (* mass (- 1 p)))))
                             {} q)))
              q0
              masked)))

(defn- channel-row
  "The observation row of one state through one observed-token set:
  cascade-model-manifest/observation-distribution (Lean
  TokenObservation.tokenLikelihood_colsum) over the rates restricted to the
  observed tokens, with the state likewise restricted — the exact marginal
  of the full token-likelihood over the unobserved tokens.  Refuses (typed)
  on any rate problem."
  [rates observed state]
  (let [r (select-keys rates observed)]
    (m/observation-distribution r (set/intersection (set state) observed))))

(defn- step-row
  "One state's observation row for one step: the independent mixture over
  which acting checks answer, over the default channel plus the answered
  checks' opened facts.  Lean activeRowEntropy/activePredictedOutcome are
  built on exactly these per-(state, channel) rows.  acting-checks is the
  empty vector when no check acts (the default channel only) or the one
  acting check's fact (step-acting-check: ONE acting pattern per step)."
  [rates default-obs acting-checks state]
  (let [facts (mapv :fact acting-checks)
        thetas (mapv :theta acting-checks)
        ;; every answer subset of the acting checks' facts
        subsets (reduce (fn [acc f] (into acc (map #(set/union % #{f})) acc))
                        [#{}]
                        facts)]
    (reduce (fn [row answered]
              (let [w (reduce * 1 (map (fn [f theta]
                                         (if (contains? answered f) theta (- 1 theta)))
                                       facts thetas))
                    observed (set/union default-obs answered)
                    channel (channel-row rates observed state)]
                (if (refusal? channel)
                  (reduced channel)
                  (reduce-kv (fn [acc o p] (update acc o (fnil + 0) (* w p))) row channel))))
            {}
            subsets)))

(defn- row-entropy
  "Lean activeRowEntropy: −Σ_o A(o|s)·ln A(o|s), zero-mass terms omitted."
  [row]
  (- (reduce + 0.0 (for [[_o p] (seq row) :when (pos? p)]
                     (* (double p) (Math/log (double p)))))))

(defn- step-risk
  "Lean activeStepRisk = OutcomeRiskKL.outcomeRisk on the channel-predicted
  outcome: ⊤ (:infinite) iff some o with Q(o) > 0 has c(o) = 0 (ln c = −∞),
  else Σ_{Q(o)>0} Q(o)·(ln Q(o) − ln c(o)).  horizon-g-sparse's identical
  reduction is private to cascade-model-manifest, so this is the smallest
  aligned form, not a reuse."
  [q-obs log-c-of]
  (if (some (fn [[o p]] (and (pos? p) (= ##-Inf (log-c-of o)))) q-obs)
    :infinite
    (double (reduce + 0.0
                    (for [[o p] q-obs :when (pos? p)]
                      (* (double p) (- (Math/log (double p)) (double (log-c-of o)))))))))

(defn active-horizon-g
  "G(π) with action-dependent observation channels, Lean
  EpistemicValue.activeHorizonEFE (mathlib4 3b19f6225e) at mission scale:

  {:q0 <token-state belief map>         ; cascade-model-manifest shape
   :precedence-fn (fn [k] <patterns…>)  ; per-step precedence (checks may act)
   :horizon T                           ; common to all candidates compared
   :spec <preference spec>              ; OR :c-fn-pointwise (fn [τ o] c(o))
   :universe <token set>                ; common observation space of C
   :rates <adjudication rates>          ; per token {:false-neg :false-pos}
   :unknown-prior {fact p}              ; DECLARED prior per masked fact
   :masked #{facts}                     ; the unknown facts
   :cap N}                              ; max number of masked facts, no default

  Returns the double G = Σ_τ (risk_τ + ambiguity_τ), :infinite when any
  step's risk is infinite (activeStepRisk's ⊤), or the first typed refusal.
  With :masked #{} this equals cascade-model-manifest/horizon-g-sparse at
  zero rates (Lean activeHorizonEFE_eq_horizonEFE +
  tokenLikelihood_checkable).  Pure; no wiring."
  [{:keys [rates q0 precedence-fn horizon spec c-fn-pointwise universe
           unknown-prior masked cap]}]
  (let [masked (set (or masked #{}))
        default-obs (set/difference (set universe) masked)]
    (cond
      (not (and (integer? horizon) (pos? horizon)))
      {:status :missing :kind :invalid-horizon :horizon horizon}
      (not (and (ifn? precedence-fn) (map? q0)))
      {:status :missing :kind :invalid-horizon-g-input}
      (and (nil? c-fn-pointwise) (nil? spec))
      {:status :missing :kind :missing-preference-spec}
      (nil? cap)
      {:status :missing :kind :cap-required
       :limitation "the number of unknown facts is capped because the spread support grows as 2^k; the cap is an input with no default"}
      (not (and (integer? cap) (<= 0 cap)))
      {:status :missing :kind :invalid-cap :cap cap}
      (> (count masked) cap)
      {:status :missing :kind :unknown-facts-over-cap
       :masked (count masked) :cap cap
       :limitation "the spread support grows as 2^k in the number of unknown facts"}
      (not (set? universe))
      {:status :missing :kind :invalid-universe :universe universe}
      (not (set/subset? masked universe))
      {:status :missing :kind :masked-outside-universe
       :masked (vec (sort masked))
       :limitation "masked facts spread Q over states and open channels inside :universe; C and Q meet only there"}
      :else
      (let [lpf (when (nil? c-fn-pointwise)
                      (m/log-preference-fn spec universe))
                point-c (cond
                          c-fn-pointwise (fn [_tau o]
                                           (let [c (c-fn-pointwise _tau o)]
                                             (if (zero? c) ##-Inf (Math/log (double c)))))
                          (refusal? lpf) lpf
                          :else (fn [_tau o] (lpf o)))]
            (if (refusal? point-c)
              point-c
              ;; transitions: the manifest's rollout with checks removed
              ;; (a check's B is doing nothing's — rolloutState_eq_of_B)
              (let [transition-fn (fn [k] (vec (remove check-pattern? (precedence-fn k))))
                    q0' (spread-unknowns q0 masked (or unknown-prior {}))]
                (if (refusal? q0')
                  q0'
                  (loop [tau 1 total 0.0 q q0']
                  (if (> tau horizon)
                    (double total)
                    (let [acting-check (step-acting-check (precedence-fn (dec tau)) masked q)
                          ;; ONE acting pattern per step: if the acting
                          ;; pattern is a check the transition is
                          ;; doing-nothing's (rolloutState_eq_of_B); if an
                          ;; ordinary pattern acts, the manifest rollout with
                          ;; checks removed applies its transition
                          q' (if acting-check
                               q
                               (m/rollout transition-fn q 1))]
                      (if (refusal? q')
                        q'
                        (let [acting (if acting-check [acting-check] [])
                              rows (into {}
                                         (map (fn [[state _mass]]
                                                [state (step-row rates default-obs acting state)]))
                                         q')
                              bad-row (first (filter refusal? (vals rows)))]
                          (if bad-row
                            bad-row
                            (let [q-obs (reduce-kv (fn [acc state row]
                                                     (let [mass (get q' state)]
                                                       (reduce-kv (fn [acc' o p]
                                                                    (update acc' o (fnil + 0) (* mass p)))
                                                                  acc row)))
                                                   {} rows)
                                  log-c (fn [o] (point-c tau o))
                                  risk (step-risk q-obs log-c)]
                              (if (= risk :infinite)
                                :infinite
                                (let [ambiguity (double
                                                  (reduce + 0.0
                                                          (for [[state mass] (seq q')
                                                                :when (pos? mass)]
                                                            (* (double mass)
                                                               (row-entropy (get rows state))))))
                                      term (+ risk ambiguity)]
                                  (recur (inc tau) (+ total term) q')))))))))))))))))
