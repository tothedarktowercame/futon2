(ns ants.aif.rollout
  "R13 policy-horizon rollout for the ant forager.

   Scores a candidate action over a multi-step horizon:
     S(π) = Σ_t ρ^t · s(s_t)
   where s(s_t) is the controller-score at the predicted state at step t,
   and ρ is the temporal discount.

   The rollout uses the ant's heuristic forward model (predict-outcome in
   policy.clj) to chain predictions — NOT the R4 kernel (forward.clj),
   because the policy's predict-outcome already maps actions to predicted
   observations at the policy layer. The R4 kernel is used by the live
   simulation step; the rollout uses the policy's internal forward model
   for speed (the two will converge when predict-outcome is replaced by
   the real kernel in a future slice).

   Design: the discount-sum skeleton (Σ ρ^t) is domain-agnostic, but the
   per-step scoring (expected-free-energy) and state transition (predict-outcome)
   are ant-specific. Kept local rather than lifted to futon2.aif.core because
   the state-transition coupling is too entangled with ant observation/mu
   structure to share cleanly with the tokamak.

   Faithfulness tag: R13 FEP-derived (policy horizon / expected free energy
   over multi-step trajectories).")
(defn rollout-score
  "Score an action over a multi-step horizon.

   Args:
     score-fn    (mu observation action opts) -> controller-score (number)
     predict-fn  (mu observation action) -> predicted-observation
     mu          current belief state
     observation current observation
     action      the candidate action to evaluate
     opts        {:horizon H (default 1 = greedy)
                  :discount ρ (default 0.9)
                  :score-opts opts passed to score-fn}

   Returns:
     {:s         the discounted sum Σ_t ρ^t · s(s_t)
      :steps     vector of {:step :score :rho-t} per step}

   At H=1 this reduces to the greedy 1-step score."
  ([score-fn predict-fn mu observation action]
   (rollout-score score-fn predict-fn mu observation action {}))
  ([score-fn predict-fn mu observation action
    {:keys [horizon discount score-opts]
     :or {horizon 1
          discount 0.9
          score-opts {}}}]
   (loop [step 0
          mu mu
          obs observation
          total 0.0
          steps []]
     (if (>= step (max 1 (int horizon)))
       {:s total
        :steps steps}
       (let [s (double (score-fn mu obs action (assoc score-opts :step step)))
             weight (Math/pow (double discount) step)
             contribution (* weight s)
             ;; Predict next observation for the next step
             predicted-obs (predict-fn mu obs action)
             ;; Evolve mu: update sensory beliefs toward predicted obs
             mu' (-> mu
                     (assoc :h (double (or (:h predicted-obs) (:h mu) 0.5)))
                     (assoc-in [:sens :food] (double (or (:food predicted-obs) 0.0)))
                     (assoc-in [:sens :ingest] (double (or (:ingest predicted-obs) 0.0)))
                     (assoc-in [:sens :cargo] (double (or (:cargo predicted-obs) 0.0))))]
         (recur (inc step)
                mu'
                predicted-obs
                (+ total contribution)
                (conj steps {:step step :score s :rho-t weight})))))))
