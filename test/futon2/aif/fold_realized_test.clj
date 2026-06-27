(ns futon2.aif.fold-realized-test
  "The R16→R14 producer (`fold-realized`) + the END-TO-END seam: producer →
   trace-record → γ reader (`policy-precision/fold-realized-outcome`). This is the
   judge→trace→γ round-trip claude-11 integration-tests against (E-close-the-loop
   × E-precision-over-policies, committed contract)."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.fold-realized :as fr]
            [futon2.aif.fold-eval :as fe]
            [futon2.aif.trace :as trace]
            [futon2.aif.policy-precision :as pp]))

(def ^:private enacted {:boxes [{:id :a} {:id :b}] :policy-holes [{:free "metric"}]})

(deftest realized-outcome-of-builds-the-contract-record
  (testing "the four committed keys; both G legs the same EFE quantity"
    (let [ro (fr/realized-outcome-of {:policy "M-x" :expected-G -0.5} enacted 7)]
      (is (= "M-x" (:policy ro)))
      (is (= -0.5 (:expected-G ro)))
      (is (= (fe/coverage-delta-g enacted) (:realized-G ro)) ":realized-G is fold-eval over the ENACTED wiring")
      (is (number? (:realized-G ro)))
      (is (= 7 (:tick ro)))))
  (testing "expected-G falls back to the fold output's :delta-g (the gate-consumed leg)"
    (is (= -0.6 (:expected-G (fr/realized-outcome-of {:policy "M-y" :fold {:delta-g -0.6}} enacted 3)))))
  (testing "nothing enacted (no boxes) ⇒ :realized-G nil ⇒ γ holds (honest no-op)"
    (is (nil? (:realized-G (fr/realized-outcome-of {:policy "M-z" :expected-G -0.1}
                                                   {:boxes [] :policy-holes [{:free "x"}]} 1))))))

(deftest staging-is-off-by-default
  (testing "staged-off ⇒ judge-output UNCHANGED ⇒ no :realized-outcome key ⇒ γ holds"
    (let [jo {:belief {} :free-energy 0.0}]
      (is (= jo (fr/with-realized-outcome jo {:policy "M-x" :expected-G -0.5} enacted 7)))
      (is (not (contains? (fr/with-realized-outcome jo {:policy "M-x" :expected-G -0.5} enacted 7)
                          :realized-outcome)))))
  (testing "live-wired ⇒ the field is threaded on"
    (binding [fr/*live-wire?* true]
      (is (contains? (fr/with-realized-outcome {:belief {}} {:policy "M-x" :expected-G -0.5} enacted 7)
                     :realized-outcome)))))

(deftest end-to-end-producer-to-trace-to-gamma
  (testing "producer → trace-record → reader → γ updates; then dedups on :tick"
    (binding [fr/*live-wire?* true]
      (let [judge-output {:belief {} :observation {} :free-energy 0.0
                          :ranked-actions [] :decision nil :mode :sim}
            decision     {:policy "M-x" :expected-G -0.5}
            jo+          (fr/with-realized-outcome judge-output decision enacted 7)
            rec          (trace/trace-record jo+)
            ro           (:realized-outcome rec)
            gs0          (pp/initial-policy-precision-state)
            gs1          (pp/fold-realized-outcome gs0 ro)
            gs1-again    (pp/fold-realized-outcome gs1 ro)]
        ;; the field survived onto the record the reader consumes
        (is (map? ro))
        (is (number? (:expected-G ro)))
        (is (number? (:realized-G ro)))
        ;; γ folded the outcome exactly once
        (is (= 1 (:samples gs1)) "the realized outcome was folded")
        (is (= 7 (:last-outcome-tick gs1)) "dedup key set")
        ;; this producer's realized-G (≈ −0.667, the enacted coverage) BEATS
        ;; expected-G (−0.5) — lower G is better — so the plan over-delivered.
        (is (< (:realized-G ro) (:expected-G ro)) "realized beat expected (lower G better)")
        ;; burn-in (R14 v1, a3f8d38): one sample keeps γ EXACTLY 1.0 (reduction-
        ;; safe) — the beat is recorded but does not yet move selection.
        (is (= 1.0 (pp/gamma-for gs1)) "γ holds at 1.0 during burn-in")
        (is (pos? (first (:perf-history gs1))) "the beat IS captured (perf>0)")
        ;; directional contract: a SUSTAINED beat, once burn-in (min-history 5)
        ;; clears, drives γ>1 (commit harder) — through the live producer→reader seam.
        (let [gs5 (reduce (fn [gs t]
                            (let [ro-t (-> (fr/with-realized-outcome judge-output decision enacted t)
                                           trace/trace-record :realized-outcome)]
                              (pp/fold-realized-outcome gs ro-t)))
                          gs1 [8 9 10 11])]
          (is (= 5 (:samples gs5)) "burn-in cleared")
          (is (< 1.0 (pp/gamma-for gs5) 2.0) "sustained beat ⇒ γ>1 (within the band)"))
        ;; same tick again ⇒ no double-count
        (is (= gs1 gs1-again) "tick-dedup: the same outcome folds at most once")))))
