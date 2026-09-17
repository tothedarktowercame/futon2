(ns futon2.aif.policy-precision
  "Within-tick fixed-point iteration for Friston policy precision (eq. 2.7).")

(defn- fail!
  [message data]
  (throw (ex-info message data)))

(defn- finite-number?
  [x]
  (and (number? x) (Double/isFinite (double x))))

(defn- checked-vector
  [label xs]
  (when-not (sequential? xs)
    (fail! (str label " must be sequential")
           {:error :invalid-vector :field label :value xs}))
  (let [values (vec xs)]
    (when (empty? values)
      (fail! (str label " must not be empty")
             {:error :empty-vector :field label}))
    (when-not (every? finite-number? values)
      (fail! (str label " must contain only finite numbers")
             {:error :non-finite-vector :field label :value xs}))
    (mapv double values)))

(defn- softmax
  [scores]
  (let [maximum (apply max scores)
        exponentials (mapv #(Math/exp (- (double %) maximum)) scores)
        total (reduce + exponentials)]
    (mapv #(/ % total) exponentials)))

(defn- distributions
  [beta g-values f-pi-values log-priors log-prior-placement]
  (let [gamma (/ 1.0 beta)
        prior-at (fn [i] (if (= :none log-prior-placement)
                           0.0
                           (nth log-priors i)))
        pi-0 (softmax
              (mapv (fn [i g]
                      (+ (if (= :both log-prior-placement) (prior-at i) 0.0)
                         (- (* gamma g))))
                    (range) g-values))
        pi (softmax
            (mapv (fn [i f g]
                    (+ (prior-at i) (- f) (- (* gamma g))))
                  (range) f-pi-values g-values))]
    {:gamma gamma :pi-0 pi-0 :pi pi}))

(defn- dot-difference
  [pi pi-0 g-values]
  (reduce + (map (fn [p p0 g] (* (- p p0) g)) pi pi-0 g-values)))

(defn- fixed-point-residual
  "eq. 2.7's residual at BETA for a fixed carried BETA-PRIOR:
   beta_prior + (pi(beta) - pi_0(beta)) . G - beta."
  [beta-prior beta g-values f-pi-values log-priors log-prior-placement]
  (let [{:keys [pi-0 pi]}
        (distributions beta g-values f-pi-values
                       log-priors log-prior-placement)]
    (- (+ beta-prior (dot-difference pi pi-0 g-values)) beta)))

(defn- bisect-beta
  "Close a bracket on eq. 2.7's root instead of stepping toward it.

   The residual above was measured strictly decreasing in beta with exactly one
   sign change over exp(-6)..exp(6) on the 110-candidate 07-04 field, for every
   beta_prior in {0.5, 1, 2, 5} -- so the root is unique there and bisection
   reaches it. That is a NUMERICAL observation on one field, not a proof of
   monotonicity, which is why the bracket ends are checked for opposite signs
   and reported rather than assumed: if they ever agree, this returns
   `:bracketed? false` and no root, instead of halving its way to a midpoint
   that means nothing."
  [beta-prior lo hi g-values f-pi-values log-priors log-prior-placement
   tolerance max-iterations]
  (let [f #(fixed-point-residual beta-prior % g-values f-pi-values
                                 log-priors log-prior-placement)
        f-lo (f lo)
        f-hi (f hi)]
    (if (pos? (* f-lo f-hi))
      {:bracketed? false :beta nil :evaluations 2
       :residual-at-floor f-lo :residual-at-ceiling f-hi}
      (loop [lo lo hi hi n 2]
        (let [mid (* 0.5 (+ lo hi))
              width (- hi lo)]
          (if (or (>= n max-iterations)
                  (<= width (* 1.0e-15 (max 1.0 (Math/abs mid))))
                  (<= (Math/abs (f mid)) tolerance))
            {:bracketed? true :beta mid :evaluations n
             :bracket-width width}
            (if (pos? (f mid))
              (recur mid hi (inc n))
              (recur lo mid (inc n)))))))))

(defn converge-beta
  "Iterate Friston 2017 Appendix B's beta gradient flow to eq. 2.7's fixed point.

   Required inputs are the carried `beta-prior` (the previous tick's converged
   posterior, or an explicitly supplied first-tick beta_0), candidate-aligned
   expected free energies `g-values`, and candidate-aligned observed-data
   `f-pi-values`. F_pi is unscaled:

     gamma = 1 / beta
     pi_0  = softmax(-gamma * G)
     pi    = softmax(-F_pi - gamma * G)
     epsilon_gamma = (beta_prior - beta) + (pi - pi_0) dot G
     beta_next = beta + step_size * gamma^2 * epsilon_gamma

   HABIT-PRIOR PLACEMENT is explicit and default-off. `:log-priors` is the
   candidate-aligned ln E vector and must be aligned to the SAME candidate
   subset as `g-values`:

     :none  pi_0 = softmax(-gamma*G)
            pi   = softmax(-F_pi - gamma*G)
     :pi    pi_0 = softmax(-gamma*G)
            pi   = softmax(lnE - F_pi - gamma*G)
     :both  pi_0 = softmax(lnE - gamma*G)
            pi   = softmax(lnE - F_pi - gamma*G)

   ln E is ADDED. `:pi` is Friston 2017's printed form
   (`friston2017.txt:684`); `:both` is the SPM implementation form
   (`spm_MDP_VB_X.m:964`). RUN7 measured gamma at tick 20 as 0.9668 for
   `:none`, 0.9041 for `:pi`, and 0.9712 for `:both`
   (`futon2/holes/labs/wm-contract/worklist.edn :RUN7 :result`). The default is
   `:none` because that is what S2 computed, and a reader that did not request
   ln E must not move. `carry-beta` does not construct the aligned ln E vector
   yet; that join is the next slice. Passing an unaligned vector through it
   therefore fails loudly on length rather than producing a wrong join.

   TWO SOLVERS. `:solver :bisect` (DEFAULT) closes a bracket on the root of
   eq. 2.7 directly; `:solver :gradient` runs Appendix B's discrete relaxation,
   which is the scheme the paper used. They agree on this field to 1e-9 or
   better wherever the gradient solver converges, so the gradient path is
   retained as a cross-check rather than as the working solver.

   WHY BISECTION IS THE DEFAULT, on measurements against the 110-candidate
   07-04 field. Eq. 2.7 defines a scalar fixed point in one unknown; iterating
   to it is the paper's scheme, not a property the object has. Stepping in beta
   costs `step * gamma^2 = step / beta^2` per iteration, so the beta-space
   landscape flattens as beta grows and everything degrades with it:

     beta_prior   0.5      1.0      2.0      5.0       50        500
     gradient       5       24      258     1726   171018   >200000 (wrong
                                                             in the 8th
                                                             decimal)
     bisect        70       70       70       68       65        61

   Bisection is flat in beta and slightly CHEAPER for large beta, because the
   bracket is fixed while the root moves toward one end. It also has no step
   size, so it has no stability boundary: at `:step-size` 1.0 the gradient
   solver settles on the WRONG gamma for beta_prior 0.5 and 1.0 (0.319 against
   the true 2.009), and at 4.0 it diverges to beta 2e12. None of that arises
   here. Under the carried-prior ruling (worklist J4) beta is the previous
   tick's posterior, so a solver whose cost grows with beta would have made a
   drifting beta progressively more expensive every tick; with this one it does
   not (claude-1's finding, C461 review thread, 2026-09-01).

   `:beta-floor` and `:beta-ceiling` are the BRACKET under `:bisect`, not
   guards to clamp against, and the bracket ends are checked for opposite signs
   rather than assumed: `:bracketed? false` is returned with both end residuals
   if they ever agree.

   `:step-size` 0.25 is not only the paper's number; it is inside this
   iteration's STABILITY BOUNDARY at small beta, and the boundary moves with
   beta. The effective step is `step-size * gamma^2 = step-size / beta^2`, so a
   step that is stable for a large carried beta can diverge for a small one.
   Measured on the 110-candidate 07-04 field: at 0.25 all of
   beta_0 in {0.5, 1, 2, 5} converge; at 1.0, beta_0 = 0.5 and 1.0 fail to
   converge in 200,000 iterations and settle on the WRONG gamma (0.319 against
   the true 2.009); at 4.0 they diverge, beta reaching 2e12 and gamma 5e-13.
   Raise `:step-size` only with a measurement at the beta you will actually
   carry.

   ITERATION COUNT GROWS STEEPLY WITH BETA, for the same reason: the rate per
   iteration falls off like 1/beta^2. Measured on that field at the default
   step, beta_0 = 0.5 converges in 5 iterations, 1.0 in 24, 2.0 in 258, and
   5.0 in 1726. Under the carried-prior ruling (worklist J4) beta is the
   previous tick's posterior, so a beta that drifts upward makes each tick
   quadratically slower to solve. `:max-iterations` defaults to 4096, which
   covers the measured 1726 with headroom; the earlier default of 256 reported
   `:converged? false` for beta_0 = 2.0, which converges at 258 -- a bound two
   iterations short, reported as though it were a property of the problem.

   CONVERGENCE IS DEFINED ON THE RESIDUAL, not on the bracket width:
   `abs(residual) <= :tolerance` (default 1e-9), where the residual is
   `beta_prior + (pi - pi_0) . G - beta` and is therefore in BETA UNITS. Under
   `:bisect` the width test is a backstop against a non-terminating loop only,
   and `:converged?` is computed from the residual at the returned beta either
   way -- so a run that stops on width with a residual above tolerance reports
   `:converged? false`.

   Beta units are the right units here, and it is worth saying why rather than
   leaving it to be re-derived: gamma = 1/beta and the softmax consumes
   -G/tau, so tau = 1/gamma = beta. The tolerance is therefore a tolerance on
   the temperature the machine actually uses. A tolerance stated in gamma
   would be the awkward one -- a fixed gamma width is a coarse beta width at
   small gamma and a fine one at large (claude-1's question, 2026-09-01).

   Under `:gradient` this same test also avoids reporting convergence merely
   because gamma^2 made a step small. Iteration is
   bounded by `:max-iterations` (default 256). The result always reports
   `:iterations`, `:converged?`, and `:hit-bound?`.

   Beta is clamped to the explicit positive `:beta-floor` (default 1e-6) if an
   update would reach or cross it, and to `:beta-ceiling` (default 1e6) above.
   The guard is deliberately two-sided: beta -> 0 gives an infinite gamma, but
   beta -> infinity gives gamma -> 0 and a uniform policy distribution, which
   is a degenerate answer that LOOKS like a valid one. Diverging upward was
   observed here (beta 2e12, gamma 5e-13) and reached no exception, because
   those values are finite. `:converged?` is false in that case and a caller
   must read it; the ceiling and its counter make the cause legible rather
   than leaving a plausible-looking gamma. The result records
   `:beta-floor-hit?`, `:beta-floor-hit-count`, `:beta-ceiling-hit?` and
   `:beta-ceiling-hit-count`. No state is read or written by this function."
  ([beta-prior g-values f-pi-values]
   (converge-beta beta-prior g-values f-pi-values {}))
  ([beta-prior g-values f-pi-values
    {:keys [solver step-size tolerance max-iterations beta-floor beta-ceiling
            log-priors log-prior-placement]
     :or {solver :bisect
          step-size 0.25
          tolerance 1.0e-9
          max-iterations 4096
          beta-floor 1.0e-6
          beta-ceiling 1.0e6
          log-prior-placement :none}}]
   (when-not (and (finite-number? beta-prior) (pos? (double beta-prior)))
     (fail! "beta-prior is required and must be finite and positive"
            {:error :invalid-beta-prior :value beta-prior}))
   (when-not (and (finite-number? step-size) (pos? (double step-size)))
     (fail! "step-size must be finite and positive"
            {:error :invalid-step-size :value step-size}))
   (when-not (and (finite-number? tolerance) (not (neg? (double tolerance))))
     (fail! "tolerance must be finite and non-negative"
            {:error :invalid-tolerance :value tolerance}))
   (when-not (and (integer? max-iterations) (not (neg? max-iterations)))
     (fail! "max-iterations must be a non-negative integer"
            {:error :invalid-max-iterations :value max-iterations}))
   (when-not (and (finite-number? beta-floor) (pos? (double beta-floor)))
     (fail! "beta-floor must be finite and positive"
            {:error :invalid-beta-floor :value beta-floor}))
   (when-not (and (finite-number? beta-ceiling)
                  (> (double beta-ceiling) (double beta-floor)))
     (fail! "beta-ceiling must be finite and above beta-floor"
            {:error :invalid-beta-ceiling
             :value beta-ceiling :beta-floor beta-floor}))
   (when-not (#{:bisect :gradient} solver)
     (fail! ":solver must be :bisect or :gradient"
            {:error :invalid-solver :value solver}))
   (when-not (#{:none :pi :both} log-prior-placement)
     (fail! ":log-prior-placement must be :none, :pi, or :both"
            {:error :invalid-log-prior-placement
             :value log-prior-placement}))
   (when (and (= :none log-prior-placement) (some? log-priors))
     (fail! ":log-priors cannot be supplied with :none placement"
            {:error :log-priors-with-no-placement}))
   (when (and (#{:pi :both} log-prior-placement) (nil? log-priors))
     (fail! ":log-priors are required for the requested placement"
            {:error :missing-log-priors
             :log-prior-placement log-prior-placement}))
   (let [beta-prior (double beta-prior)
         step-size (double step-size)
         tolerance (double tolerance)
         beta-floor (double beta-floor)
         beta-ceiling (double beta-ceiling)
         g-values (checked-vector :g-values g-values)
         f-pi-values (checked-vector :f-pi-values f-pi-values)
         log-priors (when-not (= :none log-prior-placement)
                      (checked-vector :log-priors log-priors))]
     (when-not (= (count g-values) (count f-pi-values))
       (fail! "g-values and f-pi-values must have identical lengths"
              {:error :length-mismatch
               :g-count (count g-values)
               :f-pi-count (count f-pi-values)}))
     (when (and log-priors (not= (count g-values) (count log-priors)))
       (fail! "g-values and log-priors must have identical lengths"
              {:error :length-mismatch
               :g-count (count g-values)
               :log-prior-count (count log-priors)}))
     (if (= :bisect solver)
       (let [{:keys [bracketed? beta evaluations bracket-width
                     residual-at-floor residual-at-ceiling]}
             (bisect-beta beta-prior beta-floor beta-ceiling
                          g-values f-pi-values log-priors log-prior-placement
                          tolerance max-iterations)
             beta (or beta beta-prior)
             {:keys [gamma pi-0 pi]}
             (distributions beta g-values f-pi-values
                            log-priors log-prior-placement)
             policy-error (dot-difference pi pi-0 g-values)
             residual (+ (- beta-prior beta) policy-error)]
         {:solver :bisect
          :beta-prior beta-prior
          :log-prior-placement log-prior-placement
          :beta-posterior beta
          :gamma gamma
          :pi-0 pi-0
          :pi pi
          :policy-error policy-error
          :fixed-point-residual residual
          :iterations evaluations
          :converged? (boolean (and bracketed?
                                    (<= (Math/abs residual) tolerance)))
          :hit-bound? (boolean (and bracketed?
                                    (>= evaluations max-iterations)
                                    (> (Math/abs residual) tolerance)))
          :bracketed? bracketed?
          :bracket-width bracket-width
          :residual-at-floor residual-at-floor
          :residual-at-ceiling residual-at-ceiling
          ;; the floor and ceiling ARE the bracket under this solver, so they
          ;; are never "hit" the way a stepping solver hits them
          :beta-floor-hit? false
          :beta-floor-hit-count 0
          :beta-ceiling-hit? false
          :beta-ceiling-hit-count 0})
       (loop [beta (-> beta-prior (max beta-floor) (min beta-ceiling))
            iteration 0
            floor-hit? (< beta-prior beta-floor)
            floor-hit-count (if (< beta-prior beta-floor) 1 0)
            ceiling-hit? (> beta-prior beta-ceiling)
            ceiling-hit-count (if (> beta-prior beta-ceiling) 1 0)]
       (let [{:keys [gamma pi-0 pi]}
             (distributions beta g-values f-pi-values
                            log-priors log-prior-placement)
             policy-error (dot-difference pi pi-0 g-values)
             residual (+ (- beta-prior beta) policy-error)
             converged? (<= (Math/abs residual) tolerance)
             hit-bound? (and (not converged?) (>= iteration max-iterations))
             result {:beta-prior beta-prior
                     :log-prior-placement log-prior-placement
                     :beta-posterior beta
                     :gamma gamma
                     :pi-0 pi-0
                     :pi pi
                     :policy-error policy-error
                     :fixed-point-residual residual
                     :iterations iteration
                     :converged? converged?
                     :hit-bound? hit-bound?
                     :beta-floor-hit? floor-hit?
                     :beta-floor-hit-count floor-hit-count
                     :beta-ceiling-hit? ceiling-hit?
                     :beta-ceiling-hit-count ceiling-hit-count
                     :solver :gradient}]
         (if (or converged? hit-bound?)
           result
           (let [proposed (+ beta (* step-size gamma gamma residual))
                 floor-now? (< proposed beta-floor)
                 ceiling-now? (> proposed beta-ceiling)
                 beta-next (-> proposed (max beta-floor) (min beta-ceiling))]
             (when-not (finite-number? proposed)
               (fail! "beta iteration produced a non-finite update"
                      {:error :non-finite-beta-update
                       :iteration iteration
                       :beta beta
                       :gamma gamma
                       :residual residual}))
             (recur beta-next
                    (inc iteration)
                    (or floor-hit? floor-now?)
                    (if floor-now? (inc floor-hit-count) floor-hit-count)
                    (or ceiling-hit? ceiling-now?)
                    (if ceiling-now?
                      (inc ceiling-hit-count)
                      ceiling-hit-count))))))))))

;; ---------------------------------------------------------------------------
;; RUN7 / stage S2 -- the DARK carry lifecycle.
;;
;; `converge-beta` above is pure and stateless: one solve, one field. What a
;; run needs on top of it is a beta that persists from tick to tick, which is
;; the carried-prior ruling (worklist J4) made operational. The read/coerce/
;; write shape here is deliberately the one `futon2.aif.selection-gain` already
;; uses for the R14 gain state -- read the previous tick's map out of the trace,
;; put it through a schema guard rather than trusting it, fold this tick's
;; evidence in, hand the new map back for persistence -- so there is one
;; cross-tick state pattern in this apparatus and not two.
;;
;; NOTHING CONSUMES THIS. `carry-beta` returns a map to persist; no selection,
;; ranking, temperature or softmax path reads it. That is what makes S2 dark,
;; and it is the property RUN7's acceptance asks a reviewer to check.

(def default-initial-beta
  "beta_0 for a tick with no carried state. gamma = 1/beta, so beta = 1.0 is
   gamma = 1.0 -- the same unit prior `selection-gain/default-initial-selection-gain`
   takes, and the only value that makes the first dark tick's pi comparable to
   the live selection-gain path's.

   C22 / V6 bound, to be quoted with every number derived from a carry: over
   about 20 ticks a carried beta is STILL DOMINATED BY ITS beta_0 -- the four
   07-04 trajectories were 0.50, 0.87, 1.58 and 2.48 apart at t=17 and only
   contracted to a common value between t=17 and t=37. A 20-tick carry is not
   converged and must not be reported as one."
  1.0)

(defn initial-beta-state
  "The honest prior: beta_0, carried, with nothing solved behind it yet."
  []
  {:status :absent
   :reason :no-carried-state
   :beta default-initial-beta
   :beta-source :initial
   :solved-tick-count 0})

(defn coerce-state
  "Schema guard for a beta-carry state read back from a persisted trace record.

   Same job as `selection-gain/coerce-state`, and opened for the same reason:
   the R14 v0 gain state rode a retired schema forward through the trace for
   days because the reader inherited whatever it found. A state is usable only
   if it carries a finite positive `:beta`; anything else -- nil, a non-map, a
   later schema's shape, a beta that a bug drove to zero or NaN -- reconstructs
   the prior instead of inheriting it, and says so in `:beta-source`."
  [state]
  (if (and (map? state)
           (finite-number? (:beta state))
           (pos? (double (:beta state))))
    state
    (assoc (initial-beta-state)
           :reason (if (nil? state) :no-carried-state :malformed-carried-state))))

(defn beta-for
  "Read the beta a state carries, defaulting to beta_0. Mirrors
   `selection-gain/selection-gain-for`."
  [state]
  (double (get state :beta default-initial-beta)))

(defn held-state
  "The state a tick persists when it could not solve: beta unchanged, the
   reason named, and NO `:solve` -- the previous tick's diagnostics are dropped
   rather than carried forward, since a reader finding `:solve` on a record
   would take it for this tick's.

   `:beta-source` distinguishes a beta that is still beta_0 from one that a
   solve put there and a later tick is holding."
  [prev-state reason]
  (let [state (coerce-state prev-state)
        solved-count (long (:solved-tick-count state 0))]
    {:status :absent
     :reason reason
     :beta (beta-for state)
     :beta-source (if (zero? solved-count) :initial :held-absent)
     :solved-tick-count solved-count}))

(defn align-f-pi-and-g
  "Join the dark F_pi readback to this tick's G, by semantic action identity.

   `f-pi-by-candidate-id` is `war-machine/f-pi-dark-readback`'s per-candidate
   map: keyed by the PREVIOUS tick's `rank/N`, each entry carrying the
   `:candidate-identity` it was matched on and either `:status :present` with a
   `:value`, or `:status :absent` with a `:reason`. `current-ranked` is this
   tick's ranked-action list, whose `:controller-score` is the G that selection
   itself divides by tau -- so the G here is the machine's own, not a proxy
   recomputed from persisted terms.

   THE TWO TERMS ARE NOT CONTEMPORANEOUS, and that is a property of the
   quantity rather than of this join: G(pi) is expected under THIS tick's
   candidates, while F_pi(pi) can only be scored from a prediction the previous
   tick made about this tick's observation. So the pair is (this tick's G,
   last tick's prediction scored now) for the same policy identity. Stated here
   because a reader of a beta trajectory would otherwise take the two as
   simultaneous.

   Returns `{:g-values [...] :f-pi-values [...] :candidate-ids [...]
   :present-count n :absent-count m}` over exactly the entries that are present
   AND identity-resolvable in `current-ranked`; entries failing either are
   counted, not silently dropped to a shorter vector with no record of it."
  [f-pi-by-candidate-id current-ranked identity-fn score-fn]
  (let [by-identity (group-by identity-fn current-ranked)
        entries (sort-by key (seq (or f-pi-by-candidate-id {})))]
    (reduce
     (fn [acc [candidate-id entry]]
       (let [identity (:candidate-identity entry)
             matches (get by-identity identity)
             g (when (= 1 (count matches)) (score-fn (first matches)))]
         (cond
           (not= :present (:status entry))
           (update acc :absent-count inc)

           (not (finite-number? (:value entry)))
           (update acc :absent-count inc)

           (not (finite-number? g))
           (update acc :absent-count inc)

           :else
           (-> acc
               (update :g-values conj (double g))
               (update :f-pi-values conj (double (:value entry)))
               (update :candidate-ids conj candidate-id)
               (update :present-count inc)))))
     {:g-values [] :f-pi-values [] :candidate-ids []
      :present-count 0 :absent-count 0}
     entries)))

(defn carry-beta
  "One dark tick of the beta carry: coerce the carried state, solve eq. 2.7 over
   the aligned (F_pi, G) field, and return the state to persist.

   WHAT CARRIES, named here rather than left to a reader of a run record
   (the constraint claude-1 put on this slice): only a SOLVED posterior
   carries. `:converged? true` AND `:bracketed? true` gives
   `:beta-source :converged-posterior` and the posterior becomes the next
   tick's prior. A solve that did not converge, or whose bracket ends did not
   straddle the root, HOLDS the beta the tick came in with --
   `:beta-source :held-unsolved` -- and the failed solve is persisted beside it
   under `:solve` rather than discarded. A tick with no usable field holds too,
   as `:held-absent` with a reason. So a later reader can always tell solved
   from held, which is the distinction a silent fallback destroys.

   `opts` are `converge-beta`'s, plus the join accessors. `:identity-fn` is
   REQUIRED and is not defaulted: the cross-tick identity lives in the caller
   (`war-machine/candidate-identity`, a habit-prior policy key), and a default
   that never matched would return `:no-aligned-candidates` on every tick --
   correct, and useless, which is the failure mode the F_pi dark flag's own
   docstring warns about. `:score-fn` defaults to `:controller-score`, the
   quantity selection itself divides by tau.
   Returns a map; writes nothing."
  [prev-state f-pi-by-candidate-id current-ranked
   {:keys [identity-fn score-fn] :as opts}]
  (when-not (ifn? identity-fn)
    (fail! ":identity-fn is required for the F_pi/G join"
           {:error :missing-identity-fn}))
  (let [state (coerce-state prev-state)
        beta-prior (beta-for state)
        held (partial held-state state)]
    (cond
      (not (map? f-pi-by-candidate-id))
      (held :no-f-pi-readback)

      (empty? f-pi-by-candidate-id)
      (held :empty-f-pi-readback)

      :else
      (let [{:keys [g-values f-pi-values present-count absent-count]}
            (align-f-pi-and-g f-pi-by-candidate-id current-ranked
                              identity-fn
                              (or score-fn :controller-score))]
        (if (zero? present-count)
          (assoc (held :no-aligned-candidates)
                 :f-pi-present-count 0
                 :f-pi-absent-count absent-count)
          (let [solve (converge-beta beta-prior g-values f-pi-values
                                     (dissoc opts :identity-fn :score-fn))
                solved? (boolean (and (:converged? solve)
                                      (not (false? (:bracketed? solve)))))]
            {:status :present
             ;; the value the NEXT tick carries, and why it is that value
             :beta (if solved? (:beta-posterior solve) beta-prior)
             :beta-source (if solved? :converged-posterior :held-unsolved)
             :solved-tick-count (cond-> (long (:solved-tick-count state 0))
                                  solved? inc)
             :f-pi-present-count present-count
             :f-pi-absent-count absent-count
             ;; The solve, as DATA (RUN7 acceptance). pi and pi-0 are dropped:
             ;; they are two 110-element vectors per tick and nothing here needs
             ;; the distributions, only the scalars they produced.
             :solve (-> solve
                        (dissoc :pi :pi-0)
                        (assoc :candidate-count present-count))}))))))

;; ---------------------------------------------------------------------------
;; Per-context β update over cascade candidates (approved 2026-09-17,
;; PROPOSAL-policy-precision-learning.md §1–§4)
;; ---------------------------------------------------------------------------

(def ^:private order-only-tie-tolerance 1.0e-12)

(defn order-only-tie-groups
  "Groups of ≥2 candidates that have EQUAL G (within 1e-12) and the SAME
   pattern set in (possibly) different orders — the proposal's §2 lesson 1:
   an observation that separates candidates inside such a group is evidence
   about step ORDER, not about whether G is reliable, so the β update must not
   read it.

   Candidates carry :id, :precedence (a vector of pattern ids) and :g.
   Returns a vector of groups; each group is a vector of the member
   candidates, in input order. Candidates lacking a finite :g or a
   sequential :precedence are not grouped (they carry no tie statement)."
  [candidates]
  (let [groupable (filter (fn [c]
                            (and (map? c)
                                 (sequential? (:precedence c))
                                 (finite-number? (:g c))))
                          candidates)
        by-set (group-by (fn [c] (set (:precedence c))) groupable)
        runs (fn [members]
               (loop [acc [] run [] last-g nil sorted (sort-by :g members)]
                 (if (empty? sorted)
                   (if (>= (count run) 2) (conj acc run) acc)
                   (let [c (first sorted)
                         g (:g c)
                         same? (and last-g
                                    (<= (- g last-g) order-only-tie-tolerance))]
                     (recur (if same?
                              acc
                              (if (>= (count run) 2) (conj acc run) acc))
                            (if same? (conj run c) [c])
                            g
                            (next sorted))))))]
    (into [] (mapcat runs) (vals by-set))))

(defn- cascade-residual
  "Eq. 2.7's residual with EXACT infinite-F handling: pi_0 = softmax(-gamma*G)
   over ALL candidates (F never enters pi_0), pi = softmax(-F - gamma*G) over
   the finite-F candidates ONLY, with probability exactly 0 on the
   excluded ones — the exact limit of F → infinity, never a large finite
   stand-in. Residual: beta-prior + (pi - pi_0) . G - beta, in beta units,
   same statement as `fixed-point-residual` above.

   FINITE-IDX are the indices of the finite-F candidates INTO all-g (index
   based, because two candidates may share a G — one excluded, one not — and
   a G-keyed join would conflate them)."
  [beta-prior beta all-g finite-idx finite-g finite-f]
  (let [gamma (/ 1.0 beta)
        pi-0 (softmax (mapv (fn [g] (- (* gamma g))) all-g))
        pi (softmax (mapv (fn [f g] (+ (- f) (- (* gamma g)))) finite-f finite-g))
        pi-all (reduce (fn [v [i p]] (assoc v i p))
                       (vec (repeat (count all-g) 0.0))
                       (map vector finite-idx pi))
        dot (reduce + (map (fn [p0 p g] (* (- p p0) g)) pi-0 pi-all all-g))]
    (- (+ beta-prior dot) beta)))

(defn- bisect-cascade
  "Same bracket discipline as `bisect-beta`: the ends are checked for opposite
   signs and reported, never assumed."
  [beta-prior lo hi all-g finite-idx finite-g finite-f tolerance max-iterations]
  (let [f #(cascade-residual beta-prior % all-g finite-idx finite-g finite-f)
        f-lo (f lo)
        f-hi (f hi)]
    (if (pos? (* f-lo f-hi))
      {:bracketed? false :beta nil :evaluations 2
       :residual-at-floor f-lo :residual-at-ceiling f-hi}
      (loop [lo lo hi hi n 2]
        (let [mid (* 0.5 (+ lo hi))
              width (- hi lo)]
          (if (or (>= n max-iterations)
                  (<= width (* 1.0e-15 (max 1.0 (Math/abs mid))))
                  (<= (Math/abs (f mid)) tolerance))
            (let [residual-at-root (f mid)]
              {:bracketed? true :beta mid :evaluations n
               :bracket-width width
               :residual residual-at-root
               :converged? (<= (Math/abs residual-at-root) tolerance)})
            (if (pos? (f mid))
              (recur mid hi (inc n))
              (recur lo mid (inc n)))))))))

(defn- cascade-f-by-id
  "Resolve each candidate's F from f-by-id, refusing typed on a missing or
   non-number entry (no silent default F). ##Inf is a legal value: it is the
   exact 'prediction contradicted' case and is handled by exclusion, not by
   substitution."
  [candidates f-by-id]
  (when-not (map? f-by-id)
    (fail! "f-by-id must be a map of candidate id → F"
           {:error :invalid-f-by-id :value f-by-id}))
  (mapv (fn [c]
          (let [f (get f-by-id (:id c) ::absent)]
            (when (or (= f ::absent) (not (number? f)))
              (fail! "every candidate needs a numeric F in f-by-id"
                     {:error (if (= f ::absent) :missing-f :invalid-f)
                      :candidate (:id c) :value f}))
            (assoc c :f f)))
        candidates))

(defn cascade-beta-update
  "One per-context β update over cascade candidates (approved method,
   PROPOSAL-policy-precision-learning.md §1–§4; update site R3, read site R14).

   PREV-STATES is {context beta-state}; the context's entry is coerced with
   `coerce-state`, and a MISSING entry gives `initial-beta-state` — recorded
   as such via :beta-source :initial, never silently defaulted inside the
   solve (the prior β itself is always recorded under :beta-prior).

   CANDIDATES carry :id, :precedence (vector of pattern ids) and :g.
   F-BY-ID is {candidate-id F} from this tick's observation; a missing or
   non-number F is a typed refusal, ##Inf is legal (see below).

   ORDER-ONLY TIES. Groups of ≥2 candidates with equal G (1e-12) and the same
   pattern set (`order-only-tie-groups`) are MERGED before solving: one
   representative carries the group's G, and its F is the group's MINIMUM F —
   at least one ordering fits the observation, so the separation is not
   evidence about G. Each merge is recorded under :merged-ties.

   INFINITE F. A candidate whose prediction the observation contradicts has
   F = ##Inf, whose exact posterior probability is 0. It is EXCLUDED from π
   with probability exactly 0 and KEPT in π₀ (F never enters π₀) — the exact
   limit, never a large finite stand-in. With at least one excluded candidate
   the residual is eq. 2.7's with that exact treatment (`cascade-residual`,
   bisected with the same end-sign discipline as `converge-beta`); with no
   exclusion the solve DELEGATES to the aligned `converge-beta` unchanged.

   Returns {context new-state} merged into prev-states (other contexts
   untouched). The new state records :context, :beta-source, :beta-prior,
   :beta, :merged-ties, :candidates (ids), :g, :f and :solve. Following
   `carry-beta`'s rule, only a converged, bracketed solve carries
   (:beta-source :converged-posterior); otherwise the β is held with a
   reason (:held-unsolved / :held-absent)."
  [prev-states context candidates f-by-id opts]
  (let [prev-states (or prev-states {})
        _ (when-not (map? prev-states)
            (fail! "prev-states must be a map" {:error :invalid-prev-states}))
        had-state? (contains? prev-states context)
        state (if had-state?
                (coerce-state (get prev-states context))
                (initial-beta-state))
        beta-prior (beta-for state)
        cs (cascade-f-by-id candidates f-by-id)
        groups (order-only-tie-groups cs)
        merge-record (mapv (fn [group]
                             {:ids (mapv :id group)
                              :g (:g (first group))
                              :f (apply min (map :f group))
                              :f-values (mapv :f group)})
                           groups)
        ;; replace each group by one representative: group's G, group's min F
        grouped-ids (into #{} (mapcat (fn [g] (map :id g))) groups)
        representatives (mapv (fn [g]
                                {:id (mapv :id g) ; vector id: stable, distinct
                                 :precedence (:precedence (first g))
                                 :g (:g (first g))
                                 :f (apply min (map :f g))})
                              groups)
        merged (vec (concat (remove (fn [c] (contains? grouped-ids (:id c))) cs)
                            representatives))
        all-g (mapv (comp double :g) merged)
        finite-idx (vec (keep-indexed (fn [i c]
                                        (when (Double/isFinite (double (:f c))) i))
                                      merged))
        finite (mapv merged finite-idx)
        infinite (vec (remove (fn [c] (Double/isFinite (double (:f c)))) merged))
        finite-g (mapv (comp double :g) finite)
        finite-f (mapv (comp double :f) finite)
        {:keys [tolerance max-iterations beta-floor beta-ceiling]
         :or {tolerance 1.0e-9 max-iterations 4096
              beta-floor 1.0e-6 beta-ceiling 1.0e6}} opts
        solve (cond
                (empty? finite)
                {:status :no-finite-f-candidates
                 :excluded-infinite-f (mapv :id infinite)}

                (seq infinite)
                (bisect-cascade beta-prior (double beta-floor) (double beta-ceiling)
                                all-g finite-idx finite-g finite-f
                                (double tolerance) (long max-iterations))

                :else
                (converge-beta beta-prior all-g finite-f
                               (dissoc opts :identity-fn :score-fn)))
        solved? (boolean (and (:converged? solve)
                              (not (false? (:bracketed? solve)))
                              (number? (:beta solve))))
        new-state (cond->
                    {:context context
                     :status (if solved? :present :absent)
                     :reason (when-not solved?
                               (or (:status solve)
                                   (if (false? (:bracketed? solve))
                                     :bracket-not-straddling :not-converged)))
                     :beta-source (if solved? :converged-posterior
                                      (if had-state? :held-unsolved :held-absent))
                     :beta-prior beta-prior
                     :beta (if solved? (or (:beta-posterior solve) (:beta solve)) beta-prior)
                     :solved-tick-count (cond-> (long (:solved-tick-count state 0))
                                          solved? inc)
                     :prior-was-absent (not had-state?)
                     :merged-ties merge-record
                     :candidates (mapv :id cs)
                     :g (mapv (fn [c] [(:id c) (double (:g c))]) cs)
                     :f (mapv (fn [c] [(:id c) (:f c)]) cs)
                     :excluded-infinite-f (mapv :id infinite)}
                    true (assoc :solve (dissoc solve :pi :pi-0)))]
    (assoc prev-states context new-state)))
