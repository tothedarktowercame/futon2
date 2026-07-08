(ns futon2.aif.bmr
  "Pure Bayesian Model Reduction over Dirichlet concentration parameters.")

(def ^:private log-sqrt-two-pi
  (* 0.5 (Math/log (* 2.0 Math/PI))))

(def ^:private lanczos-g 7.0)

(def ^:private lanczos-coefficients
  [0.99999999999980993
   676.5203681218851
   -1259.1392167224028
   771.32342877765313
   -176.61502916214059
   12.507343278686905
   -0.13857109526572012
   9.9843695780195716e-6
   1.5056327351493116e-7])

(defn log-gamma
  "Natural logarithm of Gamma(z), using the Lanczos approximation.

   Kept local because Commons Math Gamma/logGamma is not on the current
   futon2 classpath."
  [z]
  (let [z (double z)]
    (when-not (and (pos? z) (not (Double/isNaN z)) (not (Double/isInfinite z)))
      (throw (ex-info "log-gamma requires a positive finite argument"
                      {:z z})))
    (if (< z 0.5)
      (- (Math/log Math/PI)
         (Math/log (Math/sin (* Math/PI z)))
         (log-gamma (- 1.0 z)))
      (let [z' (dec z)
            x (reduce-kv (fn [acc i c]
                           (if (zero? i)
                             acc
                             (+ acc (/ c (+ z' i)))))
                         (first lanczos-coefficients)
                         lanczos-coefficients)
            t (+ z' lanczos-g 0.5)]
        (+ log-sqrt-two-pi
           (* (+ z' 0.5) (Math/log t))
           (- t)
           (Math/log x))))))

(defn- finite-positive?
  [x]
  (and (pos? x) (not (Double/isNaN x)) (not (Double/isInfinite x))))

(defn- concentration-vector
  [label xs]
  (when-not (sequential? xs)
    (throw (ex-info "concentration parameters must be an ordered sequential collection"
                    {:label label
                     :value xs})))
  (let [v (mapv double xs)]
    (when (empty? v)
      (throw (ex-info "concentration vector must not be empty"
                      {:label label})))
    (doseq [[i x] (map-indexed vector v)]
      (when-not (finite-positive? x)
        (throw (ex-info "concentration parameters must be positive finite numbers"
                        {:label label
                         :index i
                         :value x}))))
    v))

(defn- same-cardinality!
  [full-prior full-posterior reduced-prior]
  (let [n (count full-prior)]
    (when-not (= n (count full-posterior) (count reduced-prior))
      (throw (ex-info "BMR concentration vectors must have matching cardinality"
                      {:full-prior-count n
                       :full-posterior-count (count full-posterior)
                       :reduced-prior-count (count reduced-prior)})))))

(defn log-multivariate-beta
  "Natural log of the multivariate beta function for a Dirichlet vector:
   lnB(v) = sum_i lnGamma(v_i) - lnGamma(sum_i v_i)."
  [concentrations]
  (let [v (concentration-vector :concentrations concentrations)]
    (- (reduce + (map log-gamma v))
       (log-gamma (reduce + v)))))

(defn dirichlet-moments
  "Return per-component Dirichlet posterior moments for concentration vector alpha.

   Exposes standard deviation, not raw variance, so uncertainty stays in
   endpoint-count units for the downstream ambiguity term."
  [alpha]
  (let [v (concentration-vector :alpha alpha)
        alpha0 (reduce + v)
        denom (* alpha0 alpha0 (inc alpha0))]
    (mapv (fn [alpha-i]
            (let [mean (/ alpha-i alpha0)
                  variance (/ (* alpha-i (- alpha0 alpha-i)) denom)]
              {:mean mean
               :stddev (Math/sqrt variance)}))
          v)))

(defn bayesian-model-reduction
  "Score a reduced Dirichlet model against a full parent model.

   full-prior:     a
   full-posterior: A
   reduced-prior:  a'

   Returns {:reduced-posterior A' :delta-F delta-F :accept? (<= delta-F -3)}."
  [full-prior full-posterior reduced-prior]
  (let [a (concentration-vector :full-prior full-prior)
        A (concentration-vector :full-posterior full-posterior)
        a' (concentration-vector :reduced-prior reduced-prior)]
    (same-cardinality! a A a')
    (let [A' (mapv (fn [posterior reduced prior]
                     (+ posterior reduced (- prior)))
                   A a' a)]
      (doseq [[i x] (map-indexed vector A')]
        (when-not (finite-positive? x)
          (throw (ex-info "reduced posterior concentrations must be positive finite numbers"
                          {:index i
                           :value x
                           :full-prior (nth a i)
                           :full-posterior (nth A i)
                           :reduced-prior (nth a' i)}))))
      (let [delta-F (- (+ (log-multivariate-beta A)
                          (log-multivariate-beta a'))
                       (log-multivariate-beta a)
                       (log-multivariate-beta A'))]
        {:reduced-posterior A'
         :delta-F delta-F
         :accept? (<= delta-F -3.0)}))))

(def bmr
  "Alias for bayesian-model-reduction."
  bayesian-model-reduction)
