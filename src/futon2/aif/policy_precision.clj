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

   Convergence means `abs(epsilon_gamma) <= :tolerance` (default 1e-9). This
   directly tests the eq. 2.7 fixed-point residual; a delta-beta test could
   report convergence merely because gamma^2 made a step small. Iteration is
   bounded by `:max-iterations` (default 256). The result always reports
   `:iterations`, `:converged?`, and `:hit-bound?`.

   Beta is clamped to the explicit positive `:beta-floor` (default 1e-6) if an
   update would reach or cross it. The result records `:beta-floor-hit?` and
   `:beta-floor-hit-count`; gamma is therefore always finite and positive.
   Clamping is visible rather than silently producing infinite or negative
   precision. No state is read or written by this function."
  ([beta-prior g-values f-pi-values]
   (converge-beta beta-prior g-values f-pi-values {}))
  ([beta-prior g-values f-pi-values
    {:keys [step-size tolerance max-iterations beta-floor]
     :or {step-size 0.25
          tolerance 1.0e-9
          max-iterations 256
          beta-floor 1.0e-6}}]
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
   (let [beta-prior (double beta-prior)
         step-size (double step-size)
         tolerance (double tolerance)
         beta-floor (double beta-floor)
         g-values (checked-vector :g-values g-values)
         f-pi-values (checked-vector :f-pi-values f-pi-values)]
     (when-not (= (count g-values) (count f-pi-values))
       (fail! "g-values and f-pi-values must have identical lengths"
              {:error :length-mismatch
               :g-count (count g-values)
               :f-pi-count (count f-pi-values)}))
     (loop [beta (max beta-floor beta-prior)
            iteration 0
            floor-hit? (< beta-prior beta-floor)
            floor-hit-count (if (< beta-prior beta-floor) 1 0)]
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
                     :beta-floor-hit-count floor-hit-count}]
         (if (or converged? hit-bound?)
           result
           (let [proposed (+ beta (* step-size gamma gamma residual))
                 floor-now? (< proposed beta-floor)
                 beta-next (max beta-floor proposed)]
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
                    (if floor-now? (inc floor-hit-count) floor-hit-count)))))))))
