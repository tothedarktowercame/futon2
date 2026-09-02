(ns futon2.aif.preferences-cdist-test
  "D5a c-distribution tests (M-evaluate-policies §8.6; contract
   E-C-vector-live.md:230). Covers the four contract points: both Q families,
   nats/[0,1] normalisation, temperature limits (soft→hard), and — in
   efe-test — degrade-safety of the :risk-mode flag."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.preferences :as pref]))

(defn- integrate
  "Numeric ∫ exp(log-preference) over [0,1] (midpoint rule, n slices)."
  [dist n]
  (let [dx (/ 1.0 n)]
    (reduce + (for [i (range n)]
                (* dx (Math/exp (pref/log-preference dist (* dx (+ i 0.5)))))))))

(deftest range-density-normalises-over-unit-interval
  (doseq [[spec t] [[[0.2 0.4] 0.1] [[0.0 0.0] 0.1] [[0.8 1.0] 0.05]
                    [[0.5 1.0] 0.3] [[0.15 0.25] pref/default-c-temperature]]]
    (testing (str "spec " spec " T " t)
      (is (< 0.99 (integrate (pref/c-distribution spec :temperature t) 4000) 1.01)))))

(deftest temperature-limits
  (testing "T→0 hardens the range: out-of-range log-preference dives"
    (let [hard (pref/c-distribution [0.2 0.4] :temperature 0.001)
          soft (pref/c-distribution [0.2 0.4] :temperature 0.5)]
      (is (< (pref/log-preference hard 0.9) -400))
      (is (> (pref/log-preference soft 0.9) -3.0))
      ;; in-range preference is flat in both
      (is (> (pref/log-preference hard 0.3) 0.0))))
  (testing "Bernoulli: T→0 ⇒ point-mass on target; T large ⇒ ~uniform"
    (is (> (:p1 (pref/c-distribution {:becomes 1} :temperature 0.01)) 0.999999))
    (is (< 0.5 (:p1 (pref/c-distribution {:becomes 1} :temperature 100.0)) 0.51))
    (is (< (:p1 (pref/c-distribution {:becomes 0} :temperature 0.01)) 1e-6))))

(deftest bernoulli-kl-exact
  (let [dist (pref/c-distribution {:becomes 1} :temperature 0.1)
        c (:p1 dist)]
    (testing "KL(Bern(q)‖Bern(c)) matches the closed form, in nats"
      (let [q 0.7
            expected (+ (* q (Math/log (/ q c)))
                        (* (- 1 q) (Math/log (/ (- 1 q) (- 1 c)))))]
        (is (< (Math/abs (- (pref/kl {:kind :bernoulli :p q} dist) expected)) 1e-12))))
    (testing "KL ≥ 0 and zero iff q = c"
      (is (< (Math/abs (pref/kl {:kind :bernoulli :p c} dist)) 1e-12))
      (is (pos? (pref/kl {:kind :bernoulli :p 0.2} dist))))))

(deftest empirical-bernoulli-c-distribution
  (let [dist (pref/c-distribution {:p1 0.37})]
    (is (= :bernoulli (:kind dist)))
    (is (= 0.37 (:p1 dist))))
  (is (thrown? clojure.lang.ExceptionInfo
               (pref/c-distribution {:p1 1.1}))))

(deftest gaussian-range-divergence-behaviour
  (let [dist (pref/c-distribution [0.4 0.6])]
    (testing "monotone in gap: further out-of-range ⇒ larger divergence"
      (let [d (fn [mu] (pref/kl {:kind :gaussian :mu mu :sigma2 1e-4} dist))]
        (is (< (d 0.5) (d 0.7) (d 0.9)))))
    (testing "in-range tight prediction beats out-of-range tight prediction"
      (is (< (pref/kl {:kind :gaussian :mu 0.5 :sigma2 1e-4} dist)
             (pref/kl {:kind :gaussian :mu 0.9 :sigma2 1e-4} dist))))
    (testing "zero-variance guard: floored, finite"
      (is (Double/isFinite (pref/kl {:kind :gaussian :mu 0.5 :sigma2 0.0} dist))))))

;; --- item 1 (E-KL-refinements): truncated KL — the quadrature gate ----------

(defn- quad-kl
  "Numeric KL(Q~‖C) = ∫₀¹ Q~ ln(Q~/C), Q~ = N(mu,s2) truncated+renormalised to
   [0,1]. Returns nil in the degenerate M→0 regime (mu far outside + tiny sigma:
   Q~ has ~no support mass on [0,1]) — there is no valid quadrature oracle there,
   only the closed form's clamp."
  [mu s2 lo hi t]
  (let [sig (Math/sqrt (max s2 1e-9)) n 4000 dx (/ 1.0 n)
        d (pref/c-distribution [lo hi] :temperature t)
        phi (fn [x] (/ (Math/exp (* -0.5 (Math/pow (/ (- x mu) sig) 2)))
                       (* sig (Math/sqrt (* 2.0 Math/PI)))))
        xs (map #(* (+ % 0.5) dx) (range n))
        mass (* dx (reduce + (map phi xs)))]
    (when (> mass 1e-6)
      (* dx (reduce + (map (fn [x]
                             (let [qx (/ (phi x) mass) cx (Math/exp (pref/log-preference d x))]
                               (if (> qx 1e-300) (* qx (Math/log (/ qx cx))) 0.0)))
                           xs))))))

(def ^:private kl-sweep
  (for [mu [-0.5 0.0 0.3 0.5 0.8 1.5]
        s2 [1e-4 0.01 0.25 4.0]
        [lo hi] [[0.0 1.0] [0.2 0.4] [0.1 0.9]]
        t [0.1 1.0]]
    [mu s2 lo hi t]))

(deftest truncated-kl-matches-quadrature
  (testing "LOAD-BEARING: closed form agrees with Riemann quadrature (tol 1e-3)"
    (let [checked (atom 0)]
      (doseq [[mu s2 lo hi t] kl-sweep]
        (when-let [qd (quad-kl mu s2 lo hi t)]
          (swap! checked inc)
          (let [cf (pref/kl {:kind :gaussian :mu mu :sigma2 s2}
                            (pref/c-distribution [lo hi] :temperature t))]
            (is (< (Math/abs (- cf qd)) 1e-3)
                (format "closed=%.6f quad=%.6f @ mu=%s s2=%s [%s %s] T=%s" cf qd mu s2 lo hi t)))))
      (is (> @checked 40) "gate should cover a broad non-degenerate sweep"))))

(deftest truncated-kl-non-negative
  (testing "named regression case (mu 0.5, s2 1.0, [0,1], T 1.0) — was < 0 pre-item-1"
    (is (>= (pref/kl {:kind :gaussian :mu 0.5 :sigma2 1.0}
                     (pref/c-distribution [0.0 1.0] :temperature 1.0))
            0.0)))
  (testing "≥ 0 across the whole sweep (true KL on shared support)"
    (doseq [[mu s2 lo hi t] kl-sweep]
      (is (>= (pref/kl {:kind :gaussian :mu mu :sigma2 s2}
                       (pref/c-distribution [lo hi] :temperature t))
              0.0)))))

(deftest truncated-kl-monotone-outside
  (testing "fixed sigma2/T: KL rises as mu moves further outside [lo,hi]"
    (let [dist (pref/c-distribution [0.3 0.5] :temperature 0.1)
          k (fn [mu] (pref/kl {:kind :gaussian :mu mu :sigma2 0.01} dist))]
      (is (< (k 0.5) (k 0.6) (k 0.7) (k 0.8) (k 0.95))))))

;; ---------------------------------------------------------------------------
;; U16 — the three Bernoulli-outcome arms. What each test pins is the property
;; the arm EXISTS for, so a reader comparing them reads the differences here.

(deftest shipped-log-preference-is-unchanged-by-the-arms
  (testing "the defect U12 measured is still exactly the shipped behaviour —
            the arms are additions, not a flip"
    (let [d (pref/c-distribution {:becomes 1})]
      (is (< (Math/abs (- (pref/log-preference d 0.0) (pref/log-preference d 0.75))) 1e-12)
          "double 0.0 and 0.75 still score identically on the shipped fn")
      (is (< (pref/log-preference d (long 0)) -9.0)
          "and the long 0 still reaches the unsatisfied pole")
      (is (< (Math/abs (- (pref/log-preference d nil) (pref/log-preference d 1))) 1e-12)
          "and nil still reads as the target"))))

(deftest nil-is-a-typed-absence-under-every-arm
  (testing "C130 discipline, not a branch: no arm may read an unread channel as met"
    (doseq [arm pref/bernoulli-outcome-arms]
      (let [r (pref/log-preference-under (pref/c-distribution {:becomes 1}) nil arm)]
        (is (= :absent (:status r)) (str arm))
        (is (= :no-outcome-observed (:reason r)) (str arm))
        (is (not (contains? r :log-c)) (str arm " — no number where an absence belongs")))))
  (testing "including on a range spec, which would otherwise NPE on (double nil)"
    (let [r (pref/log-preference-under (pref/c-distribution [0.5 1.0]) nil :numeric-equality)]
      (is (= :absent (:status r)))
      (is (= :no-outcome-observed (:reason r))))))

(deftest arm-numeric-equality-reaches-the-unsatisfied-pole-from-a-double
  (let [d (pref/c-distribution {:becomes 1})
        under #(pref/log-preference-under d % :numeric-equality)]
    (testing "the double 0.0 and the long 0 are now the SAME outcome — the U12 defect"
      (is (= 0 (:outcome (under 0.0))))
      (is (= 0 (:outcome (under (long 0)))))
      (is (< (Math/abs (- (:log-c (under 0.0)) (:log-c (under (long 0))))) 1e-12)))
    (testing "and every other value still reads as the target, declaration-free"
      (is (= 1 (:outcome (under 0.75))))
      (is (= 1 (:outcome (under 1.0))))
      (is (= 1 (:outcome (under -3.0)))
          "including a negative one — this arm buys the type fix and nothing else"))
    (testing "booleans read as themselves under this arm as under the others"
      (is (= 0 (:outcome (under false))))
      (is (= 1 (:outcome (under true)))))))

(deftest arm-declared-binarization-refuses-a-continuous-value-with-no-threshold
  (let [d (pref/c-distribution {:becomes 1})]
    (testing "no threshold: an exactly-binary value is read, anything else refuses"
      (is (= 0 (:outcome (pref/log-preference-under d 0.0 :declared-binarization))))
      (is (= 1 (:outcome (pref/log-preference-under d 1.0 :declared-binarization))))
      (let [r (pref/log-preference-under d 0.75 :declared-binarization)]
        (is (= :absent (:status r)))
        (is (= :no-declared-threshold (:reason r)))))
    (testing "with a declared threshold the same value is read, on either side"
      (let [sel {:arm :declared-binarization :threshold 0.8}]
        (is (= 0 (:outcome (pref/log-preference-under d 0.75 sel))))
        (is (= 1 (:outcome (pref/log-preference-under d 0.85 sel))))
        (is (= 1 (:outcome (pref/log-preference-under d 0.8 sel)))
            "the threshold itself is the target side: x >= threshold")))
    (testing "the threshold is what MOVES the reading — the point of declaring it"
      (is (= 1 (:outcome (pref/bernoulli-outcome {:arm :declared-binarization :threshold 0.5} 0.75))))
      (is (= 0 (:outcome (pref/bernoulli-outcome {:arm :declared-binarization :threshold 0.9} 0.75)))))))

(deftest arm-typed-binary-only-needs-the-observable-declared
  (let [d (pref/c-distribution {:becomes 1})]
    (testing "undeclared kind refuses every value, 0.0 included — the strictest arm"
      (doseq [x [0.0 (long 0) 1.0 0.75]]
        (let [r (pref/log-preference-under d x :typed-binary-only)]
          (is (= :absent (:status r)) (pr-str x))
          (is (= :undeclared-observable-kind (:reason r)) (pr-str x)))))
    (testing "a continuous observable under a Bernoulli spec is a typed mismatch,
              and says so rather than reusing the undeclared reason"
      (let [r (pref/log-preference-under d 0.75 {:arm :typed-binary-only
                                                 :observable-kind :continuous})]
        (is (= :absent (:status r)))
        (is (= :spec-observable-mismatch (:reason r)))))
    (testing "a declared binary observable is read by ==, both classes of zero"
      (let [sel {:arm :typed-binary-only :observable-kind :binary}]
        (is (= 0 (:outcome (pref/log-preference-under d 0.0 sel))))
        (is (= 0 (:outcome (pref/log-preference-under d (long 0) sel))))
        (is (= 1 (:outcome (pref/log-preference-under d 1.0 sel))))))
    (testing "and a non-binary value ON a declared binary observable is its own refusal"
      (let [r (pref/log-preference-under d 0.75 {:arm :typed-binary-only
                                                 :observable-kind :binary})]
        (is (= :absent (:status r)))
        (is (= :non-binary-value-on-binary-observable (:reason r)))))))

(deftest arm-selector-must-name-a-known-arm
  (is (thrown-with-msg? clojure.lang.ExceptionInfo #"unknown arm"
                        (pref/bernoulli-outcome :whatever-i-like 0.0)))
  (is (thrown-with-msg? clojure.lang.ExceptionInfo #"arm keyword or a map"
                        (pref/bernoulli-outcome "numeric-equality" 0.0))))

;; ---------------------------------------------------------------------------
;; U17 — the point-mass term as a divergence >= 0. These are the DENSITY-level
;; tests; the composition-level property test is in mission_c_test.

(deftest the-unshifted-range-term-is-the-negative-U17-names
  (testing "the hazard itself, pinned before the repair that removes it: the
            pre-U17 term for a satisfied range criterion is ln Z < 0"
    (let [d (pref/c-distribution [0.5 1.0])]
      (is (< (Math/abs (- -0.5119492459595545 (- (pref/log-preference d 0.75)))) 1e-12)
          "U17's -0.512, to the digit")
      (is (neg? (- (pref/log-preference d 0.75))))))
  (testing "and it is the band being narrower than one unit that does it"
    (is (neg? (pref/point-mass-divergence-shift (pref/c-distribution [0.5 1.0]))))
    (is (< (Math/abs (pref/point-mass-divergence-shift
                      (pref/c-distribution [0.0 1.0] :temperature 1e-9)))
           1e-8)
        "a band covering the whole support normalises to Z = 1, so no shift")))

(deftest point-mass-divergence-is-nonnegative-on-every-spec-shape
  (doseq [spec [[0.5 1.0] [0.0 0.3] [0.0 1.0] [0.25 0.75] [0.2 0.2]
                {:becomes 1} {:becomes 0} {:p1 0.9} {:p1 0.5} {:p1 1.0} {:p1 0.0}]
          t [0.05 pref/default-c-temperature 0.5 2.0]
          x [0 1 0.0 1.0 0.25 0.5 0.75 0.2 0.999]]
    (let [d (pref/c-distribution spec :temperature t)
          v (pref/point-mass-divergence d x)]
      (is (<= 0.0 v) (str "spec " spec " T " t " x " x " -> " v)))))

(deftest a-satisfied-range-criterion-scores-exactly-zero
  (doseq [spec [[0.5 1.0] [0.0 0.3] [0.25 0.75]]
          t [0.05 pref/default-c-temperature 0.5]]
    (let [d (pref/c-distribution spec :temperature t)
          [lo hi] spec]
      (doseq [x [lo hi (/ (+ lo hi) 2.0)]]
        (is (< (Math/abs (pref/point-mass-divergence d x)) 1e-12)
            (str "in band: spec " spec " T " t " x " x)))
      (testing "and outside the band it is the gap in temperature units, so the
                gradient a clamp would flatten is still there"
        (let [x (max 0.0 (- lo 0.02))]
          (when (< x lo)
            (is (< (Math/abs (- (/ (- lo x) t) (pref/point-mass-divergence d x))) 1e-9))
            (is (pos? (pref/point-mass-divergence d x))
                "0.02 outside a band whose -T ln Z margin exceeds it would clamp to 0")))))))

(deftest the-bernoulli-branch-is-not-shifted
  (testing "its term is already an exact KL, so U17 leaves the numbers U12 and
            U16 recorded exactly where they were"
    (doseq [spec [{:becomes 1} {:becomes 0} {:p1 0.9}]]
      (let [d (pref/c-distribution spec)]
        (is (zero? (pref/point-mass-divergence-shift d)))
        (doseq [x [0 1]]
          (is (= (- (pref/log-preference d x)) (pref/point-mass-divergence d x)))))))
  (testing "including the T=0.1 satisfied floor U16 named"
    (let [d (pref/c-distribution {:becomes 1})]
      (is (< (Math/abs (- 4.539889921682063E-5 (pref/point-mass-divergence d 1))) 1e-12)))))

(deftest an-unknown-spec-kind-refuses-rather-than-taking-no-shift
  (is (thrown-with-msg? clojure.lang.ExceptionInfo #"unsupported preference kind"
                        (pref/point-mass-divergence-shift {:kind :something-new}))))

;; ---------------------------------------------------------------------------
;; U18 — J6's ruling: the declaration's home moves from U16's per-call selector
;; into the criterion's own :spec, so c-distribution carries it.

(deftest a-spec-that-declares-nothing-builds-the-map-it-built-before
  (testing "the declaration is present-only, which is what keeps
            mission_c_test/c-distribution-is-the-pinned-constructor true"
    (doseq [spec [{:becomes 1} {:becomes 0} {:p1 0.9} [0.5 1.0]]]
      (let [d (pref/c-distribution spec)]
        (is (= {} (pref/declaration-of d)) (str spec))
        (is (not (contains? d :observable-kind)) (str spec))
        (is (not (contains? d :threshold)) (str spec))))))

(deftest a-declared-binary-observable-needs-no-threshold
  (testing "J6's ruling as the mission documents write it: the criterion says
            its observable IS binary, and the arm reads it with no threshold"
    (let [d (pref/c-distribution {:becomes 1 :observable-kind :binary})]
      (is (= :binary (:observable-kind d)) "the spec's declaration rides on the dist")
      (is (= 0 (:outcome (pref/log-preference-under d 0.0 :declared-binarization))))
      (is (= 1 (:outcome (pref/log-preference-under d 1.0 :declared-binarization))))
      (testing "and a value that contradicts the declaration refuses under the
                declaration's own reason, not :no-declared-threshold"
        (let [r (pref/log-preference-under d 0.75 :declared-binarization)]
          (is (= :absent (:status r)))
          (is (= :non-binary-value-on-binary-observable (:reason r)))))))
  (testing "the same declaration is what :typed-binary-only was refusing for"
    (let [d (pref/c-distribution {:becomes 1 :observable-kind :binary})]
      (is (= 0 (:outcome (pref/log-preference-under d 0.0 :typed-binary-only)))
          "0/14 becomes readable once the criterion declares its kind"))))

(deftest a-spec-declared-threshold-cuts-a-continuous-observable
  (let [d (pref/c-distribution {:becomes 1 :observable-kind :continuous :threshold 0.8})]
    (is (= 0 (:outcome (pref/log-preference-under d 0.75 :declared-binarization))))
    (is (= 1 (:outcome (pref/log-preference-under d 0.85 :declared-binarization))))
    (testing "and a continuous observable with no threshold is still refused"
      (let [r (pref/log-preference-under
               (pref/c-distribution {:becomes 1 :observable-kind :continuous})
               0.75 :declared-binarization)]
        (is (= :absent (:status r)))
        (is (= :no-declared-threshold (:reason r)))))))

(deftest the-specs-declaration-wins-over-the-selectors
  (testing "a per-call selector may fill a declaration the spec omits — that is
            how U16's comparison columns still run — but may not overrule one
            the mission wrote down, which is the whole point of moving the home"
    (let [declared (pref/c-distribution {:becomes 1 :threshold 0.8})
          bare (pref/c-distribution {:becomes 1})]
      (is (= 0 (:outcome (pref/log-preference-under
                          declared 0.75 {:arm :declared-binarization :threshold 0.1})))
          "the spec's 0.8 decides, not the caller's 0.1")
      (is (= 1 (:outcome (pref/log-preference-under
                          bare 0.75 {:arm :declared-binarization :threshold 0.1})))
          "with nothing declared on the spec the selector's threshold answers")))
  (testing "and a selector naming no arm at all is still an error, declaration
            or not — the arm is never inferred from the spec"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"unknown arm"
                          (pref/log-preference-under
                           (pref/c-distribution {:becomes 1 :observable-kind :binary})
                           0.0 {:threshold 0.5})))))

(deftest a-malformed-declaration-throws-at-construction
  (testing "an unknown kind is a caller error, not a typed absence: a typed
            absence would read as 'this criterion declared nothing'"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"unknown :observable-kind"
                          (pref/c-distribution {:becomes 1 :observable-kind :binry}))))
  (doseq [t [##NaN ##Inf "0.5"]]
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #":threshold must be a finite number"
                          (pref/c-distribution {:becomes 1 :threshold t}))
        (str "threshold " t))))
