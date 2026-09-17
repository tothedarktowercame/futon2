(ns futon2.aif.r9-named-validation-test
  "R9 named validation properties — the four operationally checkable
   AIF properties this implementation is expected to satisfy. Each
   property is named with a quantitative acceptance criterion (the R9
   contract requirement).

   Properties tested here:
   - V-shrink     — belief entropy decreases under accumulating evidence
   - F-decrease   — free energy decreases over a run trending toward preferences
   - EFE-stress   — pragmatic/epistemic terms behave correctly under stressed input
   - Abstain-fires — agent abstains when no action meaningfully beats :no-op

   These are integration-grade properties; per-component shape tests live
   in the per-namespace test files (belief, free_energy, efe, policy)."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.belief :as belief]
            [futon2.aif.efe :as efe]
            [futon2.aif.free-energy :as fe]))

;; ---------------------------------------------------------------------------
;; V-shrink
;; ---------------------------------------------------------------------------

(deftest v-shrink-property-test
  (testing
    "**V-shrink**: belief entropy decreases under accumulating confirmatory evidence.

     Acceptance criterion: after applying 10 confirmatory events of weight 2.0
     on a single status, posterior entropy ≤ 50% of the initial uniform-prior
     entropy."
    (let [initial (belief/uniform-prior)
          h0 (belief/entropy initial)
          evolved (reduce belief/update-entity-belief
                          initial
                          (repeat 10 {:type :strengthened :weight 2.0}))
          h1 (belief/entropy evolved)]
      (is (< h1 (* 0.5 h0))
          (str "V-shrink: entropy " h0 " → " h1
               " should be < 50% of initial; ratio = " (/ h1 h0))))))

(deftest v-shrink-monotone-test
  (testing
    "**V-shrink (monotone form)**: entropy is non-increasing as evidence accumulates
     on the same status (no other channels contribute confirmatory information).

     Acceptance criterion: entropy is non-increasing across a sequence of 5
     confirmatory events."
    (let [series (reductions belief/update-entity-belief
                             (belief/uniform-prior)
                             (repeat 5 {:type :addressed :weight 1.0}))
          entropies (mapv belief/entropy series)]
      (is (apply >= entropies)
          (str "V-shrink-monotone: entropy series " entropies
               " should be non-increasing")))))

;; ---------------------------------------------------------------------------
;; F-decrease
;; ---------------------------------------------------------------------------

(def ^:private stressed-obs
  "Out-of-distribution on all weighted channels + low loop/support/attack coverage."
  {:loop-health 0.2 :support-coverage 0.2 :attack-coverage 0.2
   :mission-health 0.1 :stack-pct 0.8 :consulting-pct 0.0
   :portfolio-pct 0.05 :mathematics-pct 0.10 :active-repo-ratio 0.3
   :sorry-count-norm 0.9 :coupling-density 0.5 :ticks-firing-ratio 0.6
   :depositing-signal 0.0})

(def ^:private healthy-obs
  "In-range on every weighted channel + high loop/support/attack coverage."
  {:loop-health 0.9 :support-coverage 0.9 :attack-coverage 0.9
   :mission-health 0.7 :stack-pct 0.2 :consulting-pct 0.25
   :portfolio-pct 0.25 :mathematics-pct 0.20 :active-repo-ratio 0.8
   :sorry-count-norm 0.1 :coupling-density 0.2 :ticks-firing-ratio 0.0
   :depositing-signal 0.1})

(defn- interpolate-obs
  "Linear interpolation between two observation maps."
  [a b t]
  (into {} (for [k (keys a)]
             [k (+ (* (- 1.0 t) (get a k 0.0))
                   (* t (get b k 0.0)))])))

(deftest f-decrease-property-test
  (testing
    "**F-decrease**: free energy decreases monotonically over a run trending
     from a stressed observation toward a healthy one.

     Acceptance criterion: controller-score is monotonically non-increasing across a
     5-point linear interpolation from stressed → healthy."
    (let [ts [0.0 0.25 0.5 0.75 1.0]
          obs-series (mapv #(interpolate-obs stressed-obs healthy-obs %) ts)
          g-series (mapv #(:controller-score (fe/compute-controller-diagnostics %)) obs-series)]
      (is (apply >= g-series)
          (str "F-decrease: controller-score series " g-series
               " should be non-increasing"))
      (is (> (first g-series) (* 5.0 (last g-series)))
          (str "F-decrease (magnitude): final controller-score should be at least 5x"
               " smaller than initial; initial = " (first g-series)
               ", final = " (last g-series))))))

;; ---------------------------------------------------------------------------
;; EFE-stress
;; ---------------------------------------------------------------------------

(deftest efe-stress-pragmatic-test
  (testing
    "**EFE-stress (pragmatic)**: G-risk on a stressed observation is meaningfully
     larger than G-risk on a healthy observation.

     Acceptance criterion: G-risk(stressed) > 5x G-risk(healthy)."
    (let [state-s {:observation stressed-obs :belief {}}
          state-h {:observation healthy-obs :belief {}}
          ;; the 5x criterion is calibrated for the hinge risk (now the escape
          ;; hatch since the default flipped to :kl on 2026-07-08)
          g-s (:G-risk (efe/compute-efe state-s {:type :no-op} {:risk-mode :hinge}))
          g-h (:G-risk (efe/compute-efe state-h {:type :no-op} {:risk-mode :hinge}))]
      (is (> g-s (* 5.0 g-h))
          (str "EFE-stress: stressed G-risk = " g-s
               ", healthy G-risk = " g-h
               "; stressed should exceed healthy by 5x")))))

(deftest efe-stress-epistemic-test
  (testing
    "**EFE-stress (epistemic)**: coverage-uncertainty-pressure (in compute-controller-diagnostics) is larger
     when the agent's structural-uncertainty channels (loop-health,
     attack-coverage, support-coverage) are darker.

     Acceptance criterion: coverage-uncertainty-pressure(low-coverage) > 3x coverage-uncertainty-pressure(high-coverage)."
    (let [g-s (:coverage-uncertainty-pressure (fe/compute-controller-diagnostics stressed-obs))
          g-h (:coverage-uncertainty-pressure (fe/compute-controller-diagnostics healthy-obs))]
      (is (> g-s (* 3.0 g-h))
          (str "EFE-stress (epistemic): stressed coverage-uncertainty-pressure = " g-s
               ", healthy coverage-uncertainty-pressure = " g-h)))))

;; ---------------------------------------------------------------------------
;; Abstain-fires
;; ---------------------------------------------------------------------------

