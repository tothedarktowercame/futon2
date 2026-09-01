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
  (let [values (mapv double xs)]
    (when (empty? values)
      (fail! (str label " must not be empty")
             {:error :empty-vector :field label}))
    (when-not (every? finite-number? values)
      (fail! (str label " must contain only finite numbers")
             {:error :non-finite-vector :field label :value xs}))
    values))

(defn- softmax
  [scores]
  (let [maximum (apply max scores)
        exponentials (mapv #(Math/exp (- (double %) maximum)) scores)
        total (reduce + exponentials)]
    (mapv #(/ % total) exponentials)))

(defn- distributions
  [beta g-values f-pi-values]
  (let [gamma (/ 1.0 beta)
        pi-0 (softmax (mapv #(- (* gamma %)) g-values))
        pi (softmax (mapv (fn [f g] (- (+ f (* gamma g))))
                          f-pi-values g-values))]
    {:gamma gamma :pi-0 pi-0 :pi pi}))

(defn- dot-difference
  [pi pi-0 g-values]
  (reduce + (map (fn [p p0 g] (* (- p p0) g)) pi pi-0 g-values)))

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

   This is Appendix B's discrete relaxation (default `:step-size` 0.25, the
   paper's simulation value), rather than repeatedly assigning the right side
   of eq. 2.7. Both have the same fixed point, while the restoring term makes
   the carried prior explicit.

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

   Convergence means `abs(epsilon_gamma) <= :tolerance` (default 1e-9). This
   directly tests the eq. 2.7 fixed-point residual; a delta-beta test could
   report convergence merely because gamma^2 made a step small. Iteration is
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
    {:keys [step-size tolerance max-iterations beta-floor beta-ceiling]
     :or {step-size 0.25
          tolerance 1.0e-9
          max-iterations 4096
          beta-floor 1.0e-6
          beta-ceiling 1.0e6}}]
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
   (let [beta-prior (double beta-prior)
         step-size (double step-size)
         tolerance (double tolerance)
         beta-floor (double beta-floor)
         beta-ceiling (double beta-ceiling)
         g-values (checked-vector :g-values g-values)
         f-pi-values (checked-vector :f-pi-values f-pi-values)]
     (when-not (= (count g-values) (count f-pi-values))
       (fail! "g-values and f-pi-values must have identical lengths"
              {:error :length-mismatch
               :g-count (count g-values)
               :f-pi-count (count f-pi-values)}))
     (loop [beta (-> beta-prior (max beta-floor) (min beta-ceiling))
            iteration 0
            floor-hit? (< beta-prior beta-floor)
            floor-hit-count (if (< beta-prior beta-floor) 1 0)
            ceiling-hit? (> beta-prior beta-ceiling)
            ceiling-hit-count (if (> beta-prior beta-ceiling) 1 0)]
       (let [{:keys [gamma pi-0 pi]} (distributions beta g-values f-pi-values)
             policy-error (dot-difference pi pi-0 g-values)
             residual (+ (- beta-prior beta) policy-error)
             converged? (<= (Math/abs residual) tolerance)
             hit-bound? (and (not converged?) (>= iteration max-iterations))
             result {:beta-prior beta-prior
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
                     :beta-ceiling-hit-count ceiling-hit-count}]
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
                      ceiling-hit-count)))))))))
