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
