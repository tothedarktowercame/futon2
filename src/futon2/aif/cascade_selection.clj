(ns futon2.aif.cascade-selection
  "Cascade selection posterior and Bayes action choice (WM-11, design P7).

   Lean to align with (mathlib4, DarkTower/WarMachine):
   - PolicySelection.lean @ a434947c63 — `selectionWeight`,
     `selectionPosterior`, `selectionWeight_top`,
     `selectionPosterior_eq_zero_of_risk_top`, `selectionPosterior_finite`,
     `selectionPosterior_all_finite`: one law, σ(log E − F − γG) with γ = 1/β,
     and a candidate whose G is infinite (it predicts, with positive mass, an
     outcome the preferences exclude) has probability exactly zero.
   - PolicyPrecision.lean @ ba1b2c42dd — β > 0, `policyPrecision` γ = 1/β,
     `precisionWeightedPosterior` (higher precision sharpens).
   - ActionMarginal.lean @ 6b55652425 — `actionMarginal` / `IsBayesAction`:
     the Bayes action maximises the posterior mass *summed over the policies
     that choose it* (per-state form `exists_bayesAction_at_state`), not the
     per-policy argmax.

   This namespace is the runtime quantity the P11 selector calls at step 3.
   It is NOT yet wired into the live path (the current selection head is the
   controller's; rebinding is a separate commission per ALIGNMENT.md order).
   Refusals are typed outcomes the Lean also states: β must be positive
   (PolicyTemperature), habit must be positive (habit prior mass), and at
   least one candidate must have finite G (selectionPosterior is undefined
   otherwise — division by a zero total)."
  (:require [clojure.math :as math]))

;; ---------------------------------------------------------------------------
;; Refusals (typed, matching cascade-g's {:refusal {:kind ...}} convention)
;; ---------------------------------------------------------------------------

(defn- refuse!
  [kind detail]
  (throw (ex-info "Cascade selection refused"
                  {:refusal {:kind kind :detail detail}})))

(defn- valid-habit?
  [x]
  (and (number? x) (pos? x)))

;; ---------------------------------------------------------------------------
;; selection-posterior
;; ---------------------------------------------------------------------------

(defn- log-sum-exp
  "Stable log-sum-exp of a nonempty seq of scores."
  [scores]
  (let [m (reduce max scores)
        sum (transduce (map #(math/exp (- % m))) + 0.0 scores)]
    (+ m (math/log sum))))

(defn selection-posterior
  "Cascade selection posterior: {id probability} over candidate cascades.

   Args: {:beta β :candidates [{:id :habit :f :g} ...]}
   - β is the selection temperature, must be > 0 (γ = 1/β).
   - :habit is the cascade's habit mass E(π), must be > 0.
   - :f is F_π, a real number.
   - :g is the cascade's risk G, a real number or :infinite (EReal ⊤).

   Per PolicySelection.selectionPosterior_finite: every infinite-G candidate
   gets probability exactly 0.0; each finite candidate gets
   habit·exp(−F − G/β) (γ = 1/β), normalised over the finite candidates only.
   Computed in log space (log-sum-exp) so large G cannot overflow.

   Refuses with typed outcomes:
   - :invalid-temperature  — β not a positive number (PolicyPrecision: β > 0).
   - :invalid-habit        — any candidate's :habit not a positive number.
   - :no-admissible-candidate — no candidates, or all have G = :infinite
     (the posterior normaliser would be 0)."
  [{:keys [beta candidates]}]
  (when-not (and (number? beta) (pos? beta))
    (refuse! :invalid-temperature {:beta beta}))
  (doseq [c candidates]
    (when-not (valid-habit? (:habit c))
      (refuse! :invalid-habit {:id (:id c) :habit (:habit c)})))
  ;; A non-finite F_pi makes every score -Inf, and log-sum-exp then divides
  ;; -Inf by -Inf: the posterior comes out NaN for every candidate and the
  ;; caller still gets a decision, chosen by tie-break, with NaN recorded as
  ;; the selection law. That is the one hole in this function's refusal
  ;; discipline (2026-09-18, found reviewing WIRE-2: under the identity-A
  ;; reduction F is infinite for every cascade that produces anything, so it
  ;; was not a corner case but the ordinary one). Refuse instead.
  (doseq [c candidates]
    (let [f (:f c)]
      (when-not (and (number? f) (Double/isFinite (double f)))
        (refuse! :invalid-free-energy {:id (:id c) :f f}))))
  (let [;; WM-06 C-4 (2026-09-18): a numeric ##Inf G is EReal ⊤ arriving as a
        ;; double, not as the :infinite keyword — the same hole the F fix
        ;; above closed one field over. rank-cascade-actions attaches ##Inf
        ;; as controller-score for every refused/infinite G, so under the
        ;; ruled zeroed-under-rates family refusal (D-ZERO-PATH a) ALL
        ;; candidates arrive at ##Inf, every score is -Inf, log-sum-exp
        ;; divides -Inf by -Inf and the posterior is NaN — while the caller
        ;; still receives a tie-broken decision. Not-finite G is infinite G.
        finite-g? (fn [c] (and (number? (:g c))
                               (Double/isFinite (double (:g c)))))
        finite (filter finite-g? candidates)
        infinite (remove finite-g? candidates)]
    (when (empty? finite)
      (refuse! :no-admissible-candidate
               {:n-candidates (count candidates)
                :n-infinite (count infinite)}))
    (let [scores (mapv (fn [c]
                         (+ (math/log (double (:habit c)))
                            (- (double (:f c)))
                            (- (/ (double (:g c)) (double beta)))))
                       finite)
          lse (log-sum-exp scores)
          probs (mapv #(math/exp (- % lse)) scores)]
      (into {}
            (concat (map (fn [c p] [(:id c) (double p)]) finite probs)
                    (map (fn [c] [(:id c) 0.0]) infinite))))))

;; ---------------------------------------------------------------------------
;; bayes-choice
;; ---------------------------------------------------------------------------

(def ^:const bayes-choice-tie-rule
  "Declared stable tie-break: on equal action mass, the action whose printed
   name (str action) sorts first wins. Ascending lexicographic order over
   names, so the winner is independent of map/seq iteration order."
  :action-name-ascending)

(defn bayes-choice
  "Bayes action under the selection posterior (ActionMarginal.IsBayesAction).

   `posterior` is {id probability} (from selection-posterior); `action-of`
   maps a candidate id to its current action (the per-state projection
   `proj π s_t` of exists_bayesAction_at_state). Returns the action with
   maximum total posterior mass, i.e. the argmax over actions of
   Σ_{π : action-of(π) = a} Q(π) — the action marginal, NOT the per-policy
   argmax.

   Ties are broken by `bayes-choice-tie-rule` (ascending action name); the
   rule is recorded in the result under :tie-break-rule.

   Returns {:action a :mass m :tie-break-rule ...}.
   Refuses :unmapped-candidate if a posterior id has no action."
  [posterior action-of]
  (let [masses (reduce-kv (fn [acc id p]
                            (let [a (get action-of id ::unmapped)]
                              (when (= a ::unmapped)
                                (refuse! :unmapped-candidate {:id id}))
                              (update acc a (fnil + 0.0) p)))
                          {} posterior)]
    (when (empty? masses)
      (refuse! :no-admissible-candidate {:posterior posterior}))
    ;; stable order: sort by action name ascending, then take the max mass;
    ;; `max-key` keeps the FIRST of equal keys, so the smallest name wins ties.
    (let [ordered (sort-by (comp str key) masses)
          [a m] (reduce (fn [[_ best-m :as best] [a' m']]
                          (if (> m' best-m) [a' m'] best))
                        (first ordered) (rest ordered))]
      {:action a :mass (double m) :tie-break-rule bayes-choice-tie-rule})))
