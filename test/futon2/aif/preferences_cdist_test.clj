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
