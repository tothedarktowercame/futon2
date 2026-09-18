(ns futon2.aif.grain-maps-test
  "WM-06 phase 2, part A: the weight-level pullback mirror of
  DarkTower/AIF/GrainMaps.lean and its controls. The laws mirror the Lean
  module (9ea6f2a23c + 284610789f + f5cad1ede8): pull-only direction, exact
  zeros with NEUTRAL weight-0 unmapped outcomes, NO renormalisation in the
  pullback (conservativity exact), recorded coverage, declared uniform
  fiber split, and pullZeroed's exact preimage with its amplification."
  (:require [clojure.test :refer [deftest is]]
            [clojure.set :refer [union]]
            [futon2.aif.grain-maps :as gm]
            [futon2.aif.cascade-model-manifest :as m]))

;; cascade outcomes close mission events; some map to nothing (partial g).
(def g
  {[::t :b-clean] :closed/M-b
   [::t :b-report] :closed/M-b
   [::t :a-evidence] :alive/M-a
   [::t :noise] nil})

(def w
  {:closed/M-b 1/2
   :alive/M-a 1/4
   :closed/M-unreached 1/4})

(def s (gm/uniform-split g))

(deftest conservativity-is-exact-no-renormalisation
  ;; pushforward of the pulled weights recovers the mission weight EXACTLY
  ;; on every reachable event — C-2. There is no renormalisation step
  ;; anywhere: the pulled sums are the raw fiber sums.
  (let [{:keys [weights fiber-split]} (gm/pull-weights g s w)
        pf (gm/pushforward-eq-mission-weight g s w)]
    (is (= :uniform-declared fiber-split) "the default is carried visibly")
    (is (= 1/4 (get weights [::t :b-clean]))
        "M-b's weight 1/2 splits uniformly over its 2-outcome fiber")
    (is (= 1/4 (get weights [::t :b-report])))
    (is (= 1/4 (get weights [::t :a-evidence])) "single-outcome fiber: full weight")
    (is (= 0 (get weights [::t :noise])) "unmapped outcome: NEUTRAL weight 0")
    (is (every? (fn [[a b]] (= a b)) (vals pf))
        "every fiber's pulled sum equals its mission weight EXACTLY")
    (is (= [1/2 1/2] (get pf :closed/M-b)))
    (is (= [1/4 1/4] (get pf :alive/M-a)))
    ;; sum_pullWeight: unreachable mission mass never arrives
    (is (= (+ 1/2 1/4) (gm/total-pulled g s w))
        "total pulled mass = reachable mission mass only, exact rationals")))

(deftest zeros-are-exact-and-neutral-zero-is-not-a-hard-zero
  ;; C-3, one statement both grains (pullWeight_eq_zero_iff): with a
  ;; positive split, a pulled weight is zero iff the mission weight is
  ;; zero. Unmapped is weight-0 NEUTRAL by construction.
  (let [w' (assoc w :closed/M-b 0)
        weights (:weights (gm/pull-weights g s w'))]
    (is (zero? (get weights [::t :b-clean])))
    (is (zero? (get weights [::t :b-report])))
    ;; and only then: a positive mission weight reaches every fiber member
    (is (pos? (get weights [::t :a-evidence]))))
  ;; NEUTRAL through the live normaliser: a weight-0 cascade outcome stays
  ;; OUT of :want and log-preference-fn gives it c_v = sigma(0) = 1/2 —
  ;; the measured non-want-token marginal. No new category invented.
  (let [weights (:weights (gm/pull-weights g s w))
        want (set (filter (comp pos? weights) (keys weights)))
        spec {:want want :lam 1 :mu 0 :evidence #{} :zeroed #{}}
        universe (union want #{[::t :noise]})
        lp (m/log-preference-fn spec universe)]
    (is (not (contains? want [::t :noise])))
    ;; weight-0 contributes NOTHING to u(o): the noise-only outcome has
    ;; the same utility as the empty outcome, so the same preference —
    ;; c_noise = sigma(0) = 1/2 as a MARGINAL, and no hard zero anywhere.
    (is (< (Math/abs (- (lp #{[::t :noise]}) (lp #{}))) 1e-12)
        "the neutral token adds zero utility: same ln c as the empty outcome")
    ;; and it enlarges Z (soft dispreference), never zeroes it:
    ;; ln Z includes ln(1+e^0) = ln 2 for the neutral token.
    (is (number? (lp #{[::t :a-evidence]})))))

(deftest coverage-is-a-recorded-rational-and-zero-is-the-no-overlap-point
  ;; C-5: coverage is the reachable mission-mass fraction, exact; 0 is the
  ;; :derived-no-overlap point of the same scale, recorded not errored.
  (is (= 3/4 (gm/coverage g w)) "1/4 of mission mass is unreachable")
  (is (= 0 (gm/coverage g {:closed/M-x 1})) "empty overlap: coverage 0, a value")
  (is (= {:status :missing :kind :empty-mission-mass}
         (gm/coverage g {}))
      "zero total mass is the one typed refusal — the ratio is undefined"))

(deftest no-mass-invention-c6
  ;; the pullback adds NO preference numbers: pulled mass is exactly the
  ;; reachable mission mass, in exact rationals, for every split shape.
  (let [custom (assoc s [::t :b-clean] 3/4 [::t :b-report] 1/4)
        {:keys [weights fiber-split]} (gm/pull-weights g custom w)]
    (is (= :declared fiber-split) "a non-default split is labelled, not assumed")
    (is (= 3/8 (get weights [::t :b-clean])))
    (is (= 1/8 (get weights [::t :b-report])))
    (is (= (+ 1/2 1/4) (gm/total-pulled g custom w))
        "custom split conserves total mass too")))

(deftest a-refusal-that-stops-something-invalid-splits-and-weights
  ;; the FiberSplit validation and the mission-weight validation are typed
  ;; refusals the pullback does not default past.
  (let [broken (assoc s [::t :b-clean] 1/2 [::t :b-report] 1/3) ; sums to 5/6
        r (gm/pull-weights g broken w)]
    (is (= :missing (:status r)))
    (is (= :invalid-fiber-split (:kind r)))
    (is (= :closed/M-b (get-in r [:offender :mission-event]))
        "the refusal names the offending fiber")
    (is (nil? (:weights r)) "the refusal stops the pullback — no partial output"))
  (let [r (gm/pull-weights g s {:closed/M-b -1/2})]
    (is (= :missing (:status r)))
    (is (= :invalid-mission-weight (:kind r)))
    (is (nil? (:weights r)))))

(deftest pull-zeroed-is-the-exact-preimage-and-amplifies
  ;; pullZeroed: a cascade outcome is zeroed iff it closes a zeroed
  ;; mission event — and a mission zero fans out to its ENTIRE fiber.
  (let [z (gm/pull-zeroed g #{:alive/M-a})]
    (is (= #{[::t :a-evidence]} z)))
  ;; amplification, made concrete at the live scorer: zeroing M-b — whose
  ;; fiber has TWO cascade outcomes — refuses the family under non-zero
  ;; rates, exactly as the same zeros declared at cascade grain would.
  ;; This is the traceability control: the refusal names the zeroed set
  ;; whose author declared it a grain away.
  (let [z (gm/pull-zeroed g #{:closed/M-b})
        zeroed-sets (set (map (fn [o'] #{o'}) z)) ; one-outcome preimage sets
        spec {:want #{[::t :a-evidence]} :lam 1 :mu 0
              :evidence #{} :zeroed zeroed-sets}
        universe #{[::t :a-evidence] [::t :b-clean] [::t :b-report]}
        r (m/horizon-g-sparse
           {:rates (zipmap universe (repeat {:false-neg 1/8 :false-pos 0}))
            :q0 {[::t :a-evidence] 1}
            :precedence-fn (constantly [])
            :horizon 1
            :spec spec})]
    (is (= 2 (count z)) "one mission zero -> its whole fiber, two outcomes")
    (is (= :zeroed-unsupported-with-rates (:kind r))
        "the amplified zero refuses the comparison family under rates —
        intended exact preservation, traceable to the mission zero")))
