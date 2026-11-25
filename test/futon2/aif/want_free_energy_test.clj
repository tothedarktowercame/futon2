(ns futon2.aif.want-free-energy-test
  "Tests transcribed from the named theorems of
  DarkTower/WarMachine/WantFreeEnergy.lean, WantFreeEnergyBenefit.lean and
  WantFreeEnergyWitness.lean (mathlib4 worktree c14-cascade-outcome, branch
  c14/cascade-outcome-type). Chance arithmetic is exact ratios, as the Lean
  uses real literals; want terms are compared at 1e-12 (log is floating
  point, as the F8 readback form requires)."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.cascade-g :as canonical-g]
            [futon2.aif.r1-belief :as r1]
            [futon2.aif.want-free-energy :as wfe]))

(def ^:private tol 1.0e-12)
(defn- close? [a b] (< (Math/abs (- (double a) (double b))) tol))

;;; ## Witness fixtures (WantFreeEnergyWitness)

(def rungs r1/rungs)
(def uniform-r (wfe/uniform-rung-prediction))
(def rung-want wfe/rung-want)
(def met-want wfe/met-want)

;;; ## WantEffect validation (the structure's pos / le_one fields)

(deftest want-effect-refusals
  (testing "a want value of 0 (a hard want) is refused: hard wants are deferred"
    (is (thrown? clojure.lang.ExceptionInfo
                 (wfe/want-effect {:met 1 :unmet 0}))))
  (testing "a want value above 1 is refused (le_one)"
    (is (thrown? clojure.lang.ExceptionInfo
                 (wfe/want-effect {:met 1 :unmet 2}))))
  (testing "a negative want value is refused (pos)"
    (is (thrown? clojure.lang.ExceptionInfo
                 (wfe/want-effect {:met 1 :unmet -1/2}))))
  (testing "the witness wants are valid"
    (is (= met-want (wfe/want-effect met-want)))
    (is (= rung-want (wfe/want-effect rung-want)))))

(deftest prediction-refusals
  (testing "a non-normalised prediction is refused, never repaired"
    (is (thrown? clojure.lang.ExceptionInfo
                 (wfe/chance {:none 1/2 :available 1/4 :used 1/8
                              :witnessed 1/16 :realised 1/8} rung-want))))
  (testing "a prediction missing an outcome is refused"
    (is (thrown? clojure.lang.ExceptionInfo
                 (wfe/chance {:none 1/2 :available 1/2} rung-want))))
  (testing "a negative-mass prediction is refused"
    (is (thrown? clojure.lang.ExceptionInfo
                 (wfe/chance {:none -1/2 :available 1/2 :used 1/2
                              :witnessed 1/4 :realised 1/4} rung-want)))))

;;; ## chance / wantTerm theorems (WantFreeEnergy.lean)

(deftest wantTerm_nonneg
  (testing "wantTerm_nonneg: the pragmatic term is never negative"
    (doseq [q [(wfe/point rungs :realised) uniform-r
               (wfe/mix-kernel 1/2 (wfe/point rungs :realised) (wfe/point rungs :none))]]
      (is (<= 0 (wfe/want-term q rung-want))))
    (is (<= 0 (wfe/want-term (wfe/bern 1/2) met-want)))))

(deftest wantTerm_zero_of_certain
  (testing "wantTerm_zero_of_certain: all mass on outcomes meeting every want gives 0"
    (is (close? 0.0 (wfe/want-term (wfe/point rungs :realised) rung-want)))
    (let [all-met {:a 1 :b 1}
          q {:a 1/3 :b 2/3}]
      (is (close? 0.0 (wfe/want-term q all-met))))))

(deftest wantTerm_point_eq_risk
  (testing "wantTerm_point_eq_risk: on a certain prediction the want term is
  -log c(o), equal to the canonical G's risk of that prediction against the
  want values read as a distribution (CascadeEFE.risk, futon2.aif.cascade-g),
  wherever the values sum to one. metWant sums to 1."
    (let [model {:id "gprime-agreement" :revision "1"}
          outcomes [true false]
          q {:model model :state-support [:s] :mass {:s 1} :authority :observed-estimate}
          ;; deterministic observation: the predicted outcome is certainly :met
          a {:model model :state-support [:s] :outcome-support outcomes
             :rows {:s {:support outcomes :mass {true 1 false 0}}}
             :authority :observed-estimate}
          c {:model model :support outcomes :mass met-want
             :provenance {:source :want-values-as-distribution}}
          ctx {:policy/id :agreement :occurrence/id 0 :point 0}
          scored (canonical-g/step-g {:q q :a a :c c :context ctx})
          [lo hi] (:risk scored)
          wt (wfe/want-term {true 1 false 0} met-want)]
      (is (close? (- (Math/log 9/10)) wt))
      (is (and (<= (double lo) (+ wt tol)) (<= (- wt tol) (double hi)))
          "the canonical risk's rational enclosure contains G''s want term")
      (is (close? wt (/ (+ (double lo) (double hi)) 2))))))

(deftest stepG'_eq_stepG_of_point
  (testing "stepG'_eq_stepG_of_point: G' step == canonical G step on a certain
  prediction (ambiguity of a deterministic kernel is 0 on both sides)"
    (let [model {:id "gprime-agreement" :revision "1"}
          outcomes [true false]
          q {:model model :state-support [:s] :mass {:s 1} :authority :observed-estimate}
          a {:model model :state-support [:s] :outcome-support outcomes
             :rows {:s {:support outcomes :mass {true 1 false 0}}}
             :authority :observed-estimate}
          c {:model model :support outcomes :mass met-want
             :provenance {:source :want-values-as-distribution}}
          ctx {:policy/id :agreement :occurrence/id 0 :point 0}
          canonical (:g (canonical-g/step-g {:q q :a a :c c :context ctx}))
          [lo hi] canonical
          g' (wfe/step-g' {:s 1} {:s {true 1 false 0}} met-want)]
      (is (and (<= (double lo) (+ g' tol)) (<= (- g' tol) (double hi))))
      (is (close? g' (/ (+ (double lo) (double hi)) 2))))))

(deftest wantTerm_prod
  (testing "wantTerm_prod: the pragmatic term composes over independent parts"
    (let [w1 {true 9/10 false 1/10}
          w2 {:x 1/2 :y 1}
          q1 {true 3/4 false 1/4}
          q2 {:x 1/3 :y 2/3}
          w-prod (wfe/want-effect-prod w1 w2)
          q-prod (into {} (for [[a pa] q1 [b pb] q2] [[a b] (* pa pb)]))]
      (is (== 1 (reduce + 0 (vals q-prod))))
      (is (close? (+ (wfe/want-term q1 w1) (wfe/want-term q2 w2))
                  (wfe/want-term q-prod w-prod))))))

(deftest reliability_helps
  (testing "reliability_helps: a more reliable pattern has a smaller pragmatic
  term whenever meeting the clause is preferred"
    (let [t1 1/2 t2 9/10]
      (is (< (wfe/want-term (wfe/bern t2) met-want)
             (wfe/want-term (wfe/bern t1) met-want))))))

;;; ## The benefit modality (WantFreeEnergyBenefit.lean)

(deftest chance_mix
  (testing "chance_mix: chance is linear in a mixture of predictions (exact)"
    (let [p1 (wfe/point rungs :realised)
          p0 (wfe/point rungs :none)
          mixed (wfe/mix-kernel 1/2 p1 p0)]
      (is (== 11/20 (wfe/chance mixed rung-want)))
      (is (== (+ (* 1/2 (wfe/chance p1 rung-want))
                 (* 1/2 (wfe/chance p0 rung-want)))
              (wfe/chance mixed rung-want))))))

(deftest climb_raises_chance
  (testing "climb_raises_chance: with monotone wants, climbing does not lower
  the chance the wants are met (rungWant_monotone holds)"
    (doseq [theta [0 1/4 1/2 1]]
      (let [climbed (wfe/push uniform-r (r1/climb theta) rungs)]
        (is (<= (double (wfe/chance uniform-r rung-want))
                (double (wfe/chance climbed rung-want))))))
    (testing "and at theta 1/2 the climb strictly raises the chance"
      (is (< (double (wfe/chance uniform-r rung-want))
             (double (wfe/chance (wfe/push uniform-r (r1/climb 1/2) rungs) rung-want)))))))

(deftest delivery_lowers_wantTerm
  (testing "delivery_lowers_wantTerm: a plan more likely to deliver has a
  strictly smaller pragmatic term"
    (let [climbed (wfe/push uniform-r (r1/climb 1/2) rungs)
          pD1 1/4 pD2 3/4
          m1 (wfe/mix-kernel pD1 climbed uniform-r)
          m2 (wfe/mix-kernel pD2 climbed uniform-r)]
      (is (< (wfe/want-term m2 rung-want)
             (wfe/want-term m1 rung-want))))))

;;; ## Witness numerals (WantFreeEnergyWitness.lean)

(deftest witness-numerals
  (testing "chance_nine_in_ten"
    (is (== 41/50 (wfe/chance (wfe/bern 9/10) met-want))))
  (testing "chance_certain"
    (is (== 9/10 (wfe/chance (wfe/point [true false] true) met-want))))
  (testing "chance_mix_half"
    (is (== 11/20 (wfe/chance (wfe/mix-kernel 1/2 (wfe/point rungs :realised)
                                                (wfe/point rungs :none))
                              rung-want))))
  (testing "chance_uniform"
    (is (== 12/25 (wfe/chance uniform-r rung-want))))
  (testing "chance_uniform_climb"
    (is (== 57/100 (wfe/chance (wfe/push uniform-r (r1/climb 1/2) rungs) rung-want)))))

;;; ## scoreCascadeCo' / scoreCascadeCoN' and perStep

(deftest score-cascade-co
  (testing "scoreCascadeCo': G' over a candidate cascade; perStep is total / H"
    (let [candidate {:id :two-step
                     :initial {:s 1}
                     :transition {:s {:s 1}}
                     :observation {:s {true 9/10 false 1/10}}
                     :horizon 3
                     :wants [met-want met-want met-want]}
          scored (wfe/score-cascade-co' candidate)]
      (is (= :ok (:status scored)))
      (is (close? (* 3 (+ (- (Math/log 41/50))
                          (wfe/ambiguity {:s 1} {:s {true 9/10 false 1/10}} [true false])))
                  (:g' scored)))
      (is (close? (/ (:g' scored) 3) (:per-step scored)))
      (testing "scoreCascadeCoN' subtracts the supplied novelty total"
        (let [n (wfe/score-cascade-co-n' candidate 1/2)]
          (is (close? (- (:g' scored) 0.5) (:g'-novelty n)))))))
  (testing "a candidate with no initial kernel is refused, as missingInterpretation"
    (is (= :refused (:status (wfe/score-cascade-co' {:id :empty}))))))
