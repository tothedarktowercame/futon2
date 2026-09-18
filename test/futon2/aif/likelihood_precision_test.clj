(ns futon2.aif.likelihood-precision-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.set :as cset]
            [futon2.aif.likelihood-precision :as lp]
            [futon2.aif.cascade-model-manifest :as m]))

(deftest temper-row-zeta-one-is-identity
  (let [row {:s0 0.5 :s1 0.25 :s2 0.25}
        out (lp/temper-row row 1)]
    (doseq [[k v] row]
      (is (< (Math/abs (- v (get out k))) 1e-12)))))

(deftest temper-row-sharpens-and-flattens-by-ratio
  ;; p^ζ/Z preserves ratios as (p_i/p_j)^ζ — the defining Gibbs property.
  (let [row {:a 0.6 :b 0.3 :c 0.1}
        sharp (lp/temper-row row 2)
        flat (lp/temper-row row 0.5)]
    (is (< (Math/abs (- 4.0 (/ (:a sharp) (:b sharp)))) 1e-9)) ; (0.6/0.3)^2
    (is (< (Math/abs (- 9.0 (/ (:b sharp) (:c sharp)))) 1e-9)) ; (0.3/0.1)^2
    (is (< (Math/abs (- (Math/sqrt 2.0) (/ (:a flat) (:b flat)))) 1e-9))
    (is (< (Math/abs (- 1.0 (reduce + (map val sharp)))) 1e-12))
    (is (< (Math/abs (- 1.0 (reduce + (map val flat)))) 1e-12))))

(deftest temper-row-large-zeta-limits-to-argmax
  (let [row {:a 0.6 :b 0.4 :c 0.0}
        out (lp/temper-row row 64)]
    (is (> (:a out) 0.999999))
    (is (< (:b out) 1e-6))
    ;; a zero entry is never resurrected by any ζ
    (is (zero? (:c out)))
    (is (< (Math/abs (- 1.0 (+ (:a out) (:b out)))) 1e-9))))

(deftest temper-row-zeta-zero-is-declared-uninformative
  ;; ζ = 0 is admitted (nonnegative precision, not silently positive), and is
  ;; uniform over the POSITIVE support — zeros stay zero.
  (let [out (lp/temper-row {:a 0.7 :b 0.3 :c 0.0} 0)]
    (is (= {:a 0.5 :b 0.5 :c 0.0} out))))

(deftest temper-row-refusals
  ;; negative precision: typed refusal, not a flipped/quiet row
  (let [r (lp/temper-row {:a 0.5 :b 0.5} -1)]
    (is (lp/refusal? r))
    (is (= :negative-zeta (:kind r))))
  ;; non-stochastic row: typed refusal with the offending sum
  (let [r (lp/temper-row {:a 0.5 :b 0.4} 1)]
    (is (lp/refusal? r))
    (is (= :row-not-stochastic (:kind r))))
  ;; negative entry
  (let [r (lp/temper-row {:a 1.5 :b -0.5} 1)]
    (is (lp/refusal? r))
    (is (= :row-not-stochastic (:kind r))))
  ;; non-number zeta
  (let [r (lp/temper-row {:a 0.5 :b 0.5} :one)]
    (is (lp/refusal? r))
    (is (= :invalid-zeta (:kind r)))))

(deftest temper-a-tempers-every-row-and-propagates-refusal
  (let [a {"o1" {:s0 0.5 :s1 0.5}
           "o2" {:s0 0.9 :s1 0.1}}
        out (lp/temper-a a 2)]
    (is (= #{"o1" "o2"} (set (keys out))))
    (is (= {:s0 0.5 :s1 0.5} (get out "o1")))
    (is (< (Math/abs (- (/ (* 0.9 0.9) (+ (* 0.9 0.9) (* 0.1 0.1)))
                        (:s0 (get out "o2"))))
           1e-12))
    (let [bad (lp/temper-a {"o1" {:s0 0.3 :s1 0.3} "o2" {:s0 0.5 :s1 0.5}} 1)]
      (is (lp/refusal? bad))
      (is (= "o1" (:observation bad))))))

;; ---------------------------------------------------------------------------
;; R7 consumer control: the sparse scorer now HONORS ζ (2026-09-18, the
;; horizon-g-sparse* :zeta seam). On the identity path (zero rates) a ζ ≠ 1
;; is the typed refusal :zeta-with-identity-rates — a configuration error,
;; never a silent no-op; on the factorized path the tempered effect is
;; demonstrated in RUN-r7-zeta-effect-2026-09-18 and asserted in
;; cascade-model-manifest-test (r7-zeta-seam-...).
;; ---------------------------------------------------------------------------

(defn- live-tick-spec []
  (m/preference-spec {:want #{"t0" "t1"} :evidence #{"e0"}
                      :lam 2 :mu 1/4 :zeroed #{#{:bad}}}))

(deftest live-tick-consumer-refuses-zeta-with-identity-rates
  (let [spec (live-tick-spec)
        rates (into {} (map (fn [t] [t {:false-neg 0 :false-pos 0}]))
                    (cset/union (:want spec) (:evidence spec)))
        fire {:id :fire :guard {:status :interpreted :operator :and
                                :clauses [{:status :interpreted :present #{}
                                           :absent #{}}]}
              :transition {:status :interpreted :operator :union
                           :produces #{"t0"}}
              :produces #{"t0"}}
        base {:rates rates :q0 {#{} 1}
              :precedence-fn (constantly [fire])
              :horizon 2 :spec spec}
        g-base (m/horizon-g-sparse base)
        r-half (m/horizon-g-sparse (assoc base :zeta 0.5))
        r-seven (m/horizon-g-sparse (assoc base :zeta 7.0))]
    (is (number? g-base))
    ;; ζ ≠ 1 on the identity path refuses typed — the score is never
    ;; modulated by a ζ that multiplies nothing, and never silently ignores
    ;; the declaration either.
    (is (lp/refusal? r-half))
    (is (= :zeta-with-identity-rates (:kind r-half)))
    (is (lp/refusal? r-seven))
    (is (= :zeta-with-identity-rates (:kind r-seven)))
    ;; ζ = 1 is exactly the untempered call
    (is (= g-base (m/horizon-g-sparse (assoc base :zeta 1))))))

(deftest zeta-declaration-names-the-implicit-state
  (let [d lp/zeta-declaration]
    (is (= :R7 (:item d)))
    (is (= :likelihood-precision-zeta (:quantity d)))
    (is (= :caller-declared (get-in d [:live-tick :rates])))
    (is (= :wired-declared-fixed (get-in d [:live-tick :status])))
    (is (= :fixed (get-in d [:live-tick :declared])))
    ;; no prior/update law is claimed for ζ
    (is (= :none-declared (get-in d [:prior :law])))
    (is (= :none (:update d)))
    ;; distinctness from R14 policy precision and from the legacy κ exponent
    (is (= :R14 (get-in d [:distinct-from :policy-precision-gamma :item])))
    (is (contains? (:distinct-from d) :legacy-weight-exponent-kappa))
    ;; the gaps are named, not papered over
    (is (= 2 (count (:gaps d))))
    ;; the wiring entry names the demonstration and the fixed status
    (is (contains? (:wiring d) :tempered-rates))
    (is (string? (:demonstration (:wiring d))))))

;; ---------------------------------------------------------------------------
;; R7 demonstrated effect: tempered-rates makes the committed evaluation
;; consumers (token-likelihood / observation-distribution / predict-observations
;; / horizon-g) compute the ζ-tempered likelihood A_ζ = A^ζ/Z(ζ) exactly, so a
;; declared FIXED ζ measurably changes Q(o|π) and G.
;; ---------------------------------------------------------------------------

(def ^:private demo-rates
  {"t0" {:false-neg 1/10 :false-pos 1/20}
   "t1" {:false-neg 1/5 :false-pos 1/10}})

(deftest temper-bernoulli-is-the-two-outcome-row-law
  (is (< (Math/abs (- 0.25 (lp/temper-bernoulli 0.1 0.5))) 1e-12))
  (is (< (Math/abs (- 0.9 (lp/temper-bernoulli 0.75 2))) 1e-12))
  ;; p = 0.5 is the fixed point of every ζ
  (is (= 0.5 (lp/temper-bernoulli 0.5 3)))
  ;; ζ = 0: interior p becomes the fair coin; 0 and 1 stay (0^0 := 0, the
  ;; declared law of temper-row, preserved by the exact path)
  (is (= 0.5 (lp/temper-bernoulli 0.3 0)))
  (is (= 0 (lp/temper-bernoulli 0 0)))
  (is (= 1 (lp/temper-bernoulli 1 0)))
  ;; exactness: rational p, integer ζ → exact rational, equal to the
  ;; audited double law within 1e-12
  (is (= 1/10 (lp/temper-bernoulli 1/4 2)))
  (is (< (Math/abs (- 0.9 (double (lp/temper-bernoulli 3/4 2)))) 1e-12))
  ;; refusals inherit temper-row's typing
  (is (= :negative-zeta (:kind (lp/temper-bernoulli 0.3 -1)))))

(deftest tempered-rates-zeta-one-is-the-identity
  ;; exact rationals in, exact rationals out: ζ = 1 is the identity map
  (let [out (lp/tempered-rates demo-rates 1)]
    (is (= demo-rates out))))

(deftest tempered-rates-sharpens-interior-probabilities
  ;; ζ > 1 sharpens every branch away from 1/2: an interior false-pos (< 1/2)
  ;; shrinks further toward 0, and an interior detection probability (1−fn)
  ;; grows toward 1, so the effective false-neg shrinks too. Integer ζ on
  ;; rational rates stays exact rational (rational? assertions).
  (let [out (lp/tempered-rates demo-rates 3)]
    (is (rational? (:false-pos (get out "t0"))))
    (is (< 0 (:false-pos (get out "t0")) 1/20))
    (is (< 0 (:false-neg (get out "t1")) 1/5))
    (is (> (- 1 (:false-neg (get out "t1"))) (- 1 1/5))))
  ;; ζ = 0: every interior branch is the fair coin
  (let [out (lp/tempered-rates demo-rates 0)]
    (is (= 1/2 (:false-neg (get out "t0"))))
    (is (= 1/2 (:false-pos (get out "t1"))))))

(deftest tempered-rates-refusals-are-typed
  (is (= :negative-zeta (:kind (lp/tempered-rates demo-rates -0.5))))
  (is (= :invalid-zeta (:kind (lp/tempered-rates demo-rates :one))))
  ;; a rate outside [0,1] makes its two-outcome row non-stochastic
  (is (= :row-not-stochastic
         (:kind (lp/tempered-rates {"t0" {:false-neg 1.2 :false-pos 0.1}} 2)))))

(deftest tempered-rates-reproduce-temperedLikelihood-exactly
  ;; The factorization claim: token-likelihood at tempered rates EQUALS
  ;; temper-row of the enumerating observation distribution — the Lean
  ;; temperedLikelihood row A^ζ/Z, computed two independent ways.
  (let [states [#{} #{"t0"} #{"t1"} #{"t0" "t1"}]]
    ;; integer ζ keeps tempered-rates exact-rational so observation-distribution
    ;; accepts it; the non-integer double path is refused by that consumer's
    ;; rationality invariant and is covered by the temper-bernoulli tests.
    (doseq [s states
            zeta [0 1 2 3]]
      (let [direct (m/observation-distribution demo-rates s)
            tempered (lp/temper-row direct zeta)
            via-rates (m/observation-distribution (lp/tempered-rates demo-rates zeta) s)]
        (is (not (lp/refusal? tempered)))
        (is (not (lp/refusal? via-rates)))
        (doseq [o (set (concat (keys direct) (keys via-rates)))]
          (is (< (Math/abs (- (get tempered o 0) (get via-rates o 0))) 1e-12)
              (str "state " s " zeta " zeta " obs " o)))))))

(deftest zeta-changes-q-o-pi
  ;; Q(o|π) through predict-observations: a fixed ζ ≠ 1 measurably changes
  ;; the predictive distribution; ζ = 1 is exactly the untempered one.
  (let [q {#{"t0"} 0.6 #{"t1"} 0.4}
        base (m/predict-observations demo-rates q)
        z1 (m/predict-observations (lp/tempered-rates demo-rates 1) q)
        z3 (m/predict-observations (lp/tempered-rates demo-rates 3) q)]
    (is (not (lp/refusal? base)))
    (is (not (lp/refusal? z3)))
    ;; ζ = 1 identity
    (is (= base z1))
    ;; ζ = 3 sharpens: the modal observation gains mass
    (let [key-obs (apply max-key (fn [o] (get base o 0)) (keys base))
          base-mass (double (get base key-obs 0))
          z3-mass (double (get z3 key-obs 0))]
      (is (> z3-mass (+ base-mass 1e-6))
          (str "obs " key-obs " base " base-mass " z3 " z3-mass)))))

(deftest zeta-changes-g
  ;; G through the enumerating horizon-g (committed, Lean-aligned): a fixed
  ;; ζ ≠ 1 changes the expected free energy; ζ = 1 is exactly the untempered G.
  ;; horizon-g's :c-fn returns a MAP o ↦ c(o) (a missing key counts as 0),
  ;; not a function — same convention as the manifest's own fixtures.
  (let [spec (m/preference-spec {:want #{"t0"} :evidence #{} :lam 1 :mu 0 :zeroed #{}})
        rates demo-rates
        univ (set (keys rates))
        cpoint (m/preference-fn spec univ)
        cmap (into {} (map (fn [o] [o (cpoint o)]))
                   [#{} #{"t0"} #{"t1"} #{"t0" "t1"}])
        fire {:id :fire :guard {:status :interpreted :operator :and
                                :clauses [{:status :interpreted :present #{} :absent #{}}]}
              :transition {:status :interpreted :operator :union :produces #{"t0"}}
              :produces #{"t0"}}
        base {:rates rates :q0 {#{} 1}
              :precedence-fn (constantly [fire])
              :horizon 2 :c-fn (constantly cmap)}
        g1 (m/horizon-g (assoc base :rates (lp/tempered-rates rates 1)))
        g3 (m/horizon-g (assoc base :rates (lp/tempered-rates rates 3)))
        g0 (m/horizon-g (assoc base :rates (lp/tempered-rates rates 0)))]
    (is (number? g1)) (is (number? g3)) (is (number? g0))
    (is (< (Math/abs (- (double g1) (double (m/horizon-g base)))) 1e-9))
    (is (not= g1 g3))
    (is (not= g1 g0))
    (is (> (Math/abs (- (double g3) (double g1))) 1e-4)
        "the effect is measurable, not epsilon")))
