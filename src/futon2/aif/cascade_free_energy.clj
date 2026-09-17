(ns futon2.aif.cascade-free-energy
  "Categorical cascade F_π for the policy-precision learner
  (PROPOSAL-policy-precision-learning.md §4, approved 2026-09-17; P8's
  per-policy free energy under exact inference).

  For each candidate cascade π, F_π = −ln p(o_τ | π) where
  p(o_τ | π) = Σ_s Q_τ(s|π) · A(o_τ | s):

  - Q_τ is `futon2.aif.cascade-model-manifest/rollout` from q0 over the
    candidate's precedence at τ (Lean DarkTower.WarMachine.PolicyRollout
    .rolloutState, the aligned Clojure implementation);
  - A is `futon2.aif.cascade-model-manifest/token-likelihood` over the given
    adjudication rates (Lean TokenObservation.tokenLikelihood);
  - the observation o_τ is the observed token set restricted to the rate
    universe.

  This is Parr et al. 2022 eq. B.2 evaluated at the exact posterior: because
  the variational posterior here IS the predictive rollout (exact inference
  over the finite powerset carrier), the complexity term
  E_Q[ln Q(s|π) − ln P(o,s|π)] + ln P(o|π) vanishes and
  F(π) = −ln P(o|π) exactly — the equality case of the B.2 bound
  (mathlib4 DarkTower.WarMachine.PolicyVariationalFreeEnergy.vfe_posterior_eq).

  p = 0 is recorded as ##Inf, never dropped. The rollout's typed refusals
  and the likelihood's are passed through per candidate. Missing inputs are
  typed refusals; every declared value is recorded under :params."
  (:require [clojure.set :as set]
            [futon2.aif.cascade-model-manifest :as m]))

(defn- refusal? [x] (and (map? x) (contains? x :status)))

(defn- validate
  [{:keys [q0 candidates tau observed-tokens rates]}]
  (let [missing (first (filter (comp nil? val)
                               {:q0 q0 :candidates candidates :tau tau
                                :observed-tokens observed-tokens :rates rates}))]
    (cond
      missing {:status :missing :kind :missing-free-energy-input
               :input (key missing)}
      (not (and (integer? tau) (pos? tau)))
      {:status :missing :kind :invalid-tau :tau tau}
      (not (and (map? q0) (seq q0)))
      {:status :missing :kind :invalid-q0 :q0 q0}
      (not (and (map? rates) (seq rates)))
      {:status :missing :kind :invalid-rates :rates rates}
      (not (and (coll? candidates) (seq candidates)
                (every? #(and (map? %) (contains? % :id) (vector? (:precedence %)))
                        candidates)))
      {:status :missing :kind :invalid-candidates :candidates candidates}
      (not (set? observed-tokens))
      {:status :missing :kind :invalid-observed-tokens :observed-tokens observed-tokens}
      :else nil)))

(defn- candidate-free-energy
  "p = Σ_s Q_τ(s|π)·A(o|s) over the rollout support, exact rationals;
   F = −ln p as a double, ##Inf when p = 0. Refusals pass through."
  [rates precedence q0 tau obs]
  (let [q (m/rollout (fn [_] precedence) q0 tau)]
    (if (refusal? q)
      q
      (loop [[[s mass] & more] (seq q) p 0]
        (if (nil? s)
          (if (zero? p) ##Inf (double (- (Math/log (double p)))))
          (let [a (m/token-likelihood rates s obs)]
            (if (refusal? a)
              a
              (recur more (+ p (* mass a))))))))))

(defn policy-free-energy
  "F_π per candidate (see ns docstring). Returns
   {:f {id F-or-refusal} :tau tau :params {…}} with every declared input
   recorded: :q0, :rates, :observed (restricted to the rate universe),
   :universe (the rate universe), :complexity-term 0 with its reason, and
   :theta per candidate as carried on the patterns (no silent defaults —
   theta comes from `cascade-model-manifest/with-pattern-theta`'s recorded
   documented default when a pattern omits it)."
  [{:keys [q0 candidates tau observed-tokens rates] :as declared}]
  (or (validate declared)
      (let [universe (set (keys rates))
            obs (set/intersection (set observed-tokens) universe)
            f (into {}
                    (map (fn [{:keys [id precedence]}]
                           [id (candidate-free-energy rates precedence q0 tau obs)]))
                    candidates)
            thetas (into {}
                         (map (fn [{:keys [id precedence]}]
                                [id (mapv #(dissoc (m/with-pattern-theta %) :produces :guard :id :clauses)
                                          precedence)]))
                         candidates)]
        {:f f :tau tau
         :params {:q0 q0 :rates rates :observed obs :universe universe
                  :complexity-term 0
                  :complexity-term-reason "exact inference over the finite powerset carrier: the variational posterior is the rollout itself, so B.2 holds with equality (PolicyVariationalFreeEnergy.vfe_posterior_eq)"
                  :theta thetas}})))
