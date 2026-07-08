(ns futon2.aif.fold-realized-test
  "The R16→R14 producer (`fold-realized`) + the END-TO-END seam: producer →
   trace-record → γ reader (`policy-precision/fold-realized-outcome`). This is the
   judge→trace→γ round-trip claude-11 integration-tests against (E-close-the-loop
   × E-precision-over-policies, committed contract)."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.edn :as edn]
            [futon2.aif.actuator-a3 :as a3]
            [futon2.aif.fold-realized :as fr]
            [futon2.aif.fold-eval :as fe]
            [futon2.aif.trace :as trace]
            [futon2.aif.policy-precision :as pp]))

(def ^:private enacted {:boxes [{:id :a} {:id :b}] :policy-holes [{:free "metric"}]})

(defn- learning-loop-clean []
  (edn/read-string (slurp "/home/joe/code/futon6/holes/clean/M-learning-loop.clean.edn")))

(defn- learning-loop-deposit-with-clean []
  (assoc (get (a3/deposits-by-id) "ft-learning-loop-010")
         :clean (learning-loop-clean)))

(defn- with-fold-forecast
  [deposit box-count hole-count]
  (assoc-in deposit [:turn :answer]
            {:boxes (mapv (fn [i] {:id (keyword (str "f" i))}) (range box-count))
             :policy-holes (mapv (fn [i] {:free (str "h" i)}) (range hole-count))}))

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
  (testing "no boxes against obligations ⇒ :realized-G 0.0 (measured zero coverage)"
    (is (= 0.0 (:realized-G (fr/realized-outcome-of {:policy "M-z" :expected-G -0.1}
                                                    {:boxes [] :policy-holes [{:free "x"}]} 1))))))

(deftest realized-outcome-grounded-uses-substrate-dial
  (testing "M-learning-loop produces a grounded partial-discharge sample"
    (let [ro (fr/realized-outcome-grounded
              "futon5a-d/mission/learning-loop"
              {:policy "futon5a-d/mission/learning-loop" :tick 101})]
      (is (= 1 (:expected-G ro)))
      (is (= :fold-coverage (:expected-source ro)))
      (is (= 1 (:realized-G ro)))
      (is (= :substrate-dial (:realized-source ro)))
      (is (= :endpoint-count (:scale ro)))
      (is (= {:inhabited 1 :bound 2 :remaining 1 :discharged? false}
             (:dial ro)))
      (is (= :exact-grounded-forecast (:anti-tautology-flag ro)))
      (is (= 101 (:tick ro)))))
  (testing "fully inhabited bound boxes produce realized-G 0"
    (let [deposit (assoc (learning-loop-deposit-with-clean)
                         :box-bindings {:b1 {:kind :entity :type :capability}
                                        :b7 {:kind :entity :type :capability}})
          ro (fr/realized-outcome-grounded (:mission deposit)
                                           {:policy (:mission deposit)}
                                           {:deposit deposit})]
      (is (= 0 (:realized-G ro)))
      (is (= {:inhabited 2 :bound 2 :remaining 0 :discharged? true}
             (:dial ro)))))
  (testing "A5 is falsified if it reports reproduction coverage instead of substrate state"
    (let [deposit (learning-loop-deposit-with-clean)
          grounded (fr/realized-outcome-grounded (:mission deposit)
                                                 {:policy (:mission deposit)}
                                                 {:deposit deposit})
          reproduction (fr/realized-outcome-of {:policy (:mission deposit)
                                                :fold {:delta-g -1.0}}
                                               {:boxes [{:id :b1} {:id :b7}]
                                                :policy-holes []}
                                               :coverage-tick)]
      (is (= 1 (:realized-G grounded)))
      (is (= -1.0 (:realized-G reproduction)))
      (is (not= (:realized-G reproduction) (:realized-G grounded))
          "grounded realized-G must be the substrate dial, not coverage over reproduced wiring"))))

(deftest realized-outcome-grounded-expected-leg-is-two-sided
  (testing "low fold forecast plus high build gives positive performance"
    (let [deposit (-> (learning-loop-deposit-with-clean)
                      (assoc :box-bindings {:b1 {:kind :entity :type :capability}
                                            :b7 {:kind :entity :type :capability}})
                      (with-fold-forecast 1 3))
          ro (fr/realized-outcome-grounded (:mission deposit)
                                           {:policy (:mission deposit)}
                                           {:deposit deposit})
          perf (pp/policy-performance (:expected-G ro) (:realized-G ro))]
      (is (= 2 (:expected-G ro)))
      (is (= 0 (:realized-G ro)))
      (is (= :fold-coverage (:expected-source ro)))
      (is (pos? perf) "build beat forecast, so gamma can commit")))
  (testing "high fold forecast plus low build gives negative performance"
    (let [absent-type :a5-test/no-such-grounded-endpoint
          deposit (-> (learning-loop-deposit-with-clean)
                      (assoc :box-bindings {:b1 {:kind :entity :type absent-type}
                                            :b7 {:kind :entity :type absent-type}})
                      (with-fold-forecast 3 0))
          ro (fr/realized-outcome-grounded (:mission deposit)
                                           {:policy (:mission deposit)}
                                           {:deposit deposit})
          perf (pp/policy-performance (:expected-G ro) (:realized-G ro))]
      (is (= 0 (:expected-G ro)))
      (is (= 2 (:realized-G ro)))
      (is (= :fold-coverage (:expected-source ro)))
      (is (neg? perf) "build missed forecast, so gamma can hedge")))
  (testing "hand-authored CLean without fold plan falls back loudly"
    (let [deposit (-> (learning-loop-deposit-with-clean)
                      (dissoc :turn :wiring)
                      (assoc :box-bindings {:b1 {:kind :entity :type :capability}
                                            :b7 {:kind :entity :type :capability}}))
          ro (fr/realized-outcome-grounded (:mission deposit)
                                           {:policy (:mission deposit)}
                                           {:deposit deposit})]
      (is (= 0 (:expected-G ro)))
      (is (= :perfection-target (:expected-source ro)))
      (is (nil? (:predicted-coverage ro)))
      (is (= :exact-grounded-forecast (:anti-tautology-flag ro))))))

(deftest staging-gates-on-live-wire
  (testing "disarmed (*live-wire?* false) ⇒ judge-output UNCHANGED ⇒ no :realized-outcome ⇒ γ holds"
    (binding [fr/*live-wire?* false]
      (let [jo {:belief {} :free-energy 0.0}]
        (is (= jo (fr/with-realized-outcome jo {:policy "M-x" :expected-G -0.5} enacted 7)))
        (is (not (contains? (fr/with-realized-outcome jo {:policy "M-x" :expected-G -0.5} enacted 7)
                            :realized-outcome))))))
  (testing "armed (default ON as of 2026-07-08) ⇒ the field is threaded on"
    (is (contains? (fr/with-realized-outcome {:belief {}} {:policy "M-x" :expected-G -0.5} enacted 7)
                   :realized-outcome))))

(deftest gamma-grounded-feed-routes-the-producer
  ;; R14 live-wire migration: *gamma-grounded-feed?* selects WHICH realized
  ;; outcome feeds γ, orthogonally to *live-wire?*. Stub both producers so the
  ;; branch is exercised without a live substrate read.
  (with-redefs [fr/realized-outcome-of       (fn [_ _ _] {:src :coverage})
                fr/realized-outcome-grounded (fn [mid _ opts]
                                               {:src :substrate-dial :mission-id mid :opts opts})]
    (let [jo {:belief {}}
          decision {:policy "M-p" :expected-G -0.5}]
      (testing "grounded-feed OFF (explicit) ⇒ coverage producer even under live-wire"
        (binding [fr/*live-wire?* true
                  fr/*gamma-grounded-feed?* false]
          (is (= :coverage (get-in (fr/with-realized-outcome jo decision enacted 7)
                                   [:realized-outcome :src])))))
      (testing "grounded-feed ON (+ live-wire) ⇒ substrate-dial, mission-id = (:policy decision)"
        (binding [fr/*live-wire?* true
                  fr/*gamma-grounded-feed?* true]
          (let [ro (:realized-outcome (fr/with-realized-outcome jo decision enacted 7))]
            (is (= :substrate-dial (:src ro)))
            (is (= "M-p" (:mission-id ro)) "mission-id derived from (:policy decision)")
            (is (= 7 (get-in ro [:opts :tick])) "tick threaded through opts"))))
      (testing "grounded-feed ON but live-wire OFF ⇒ still no realized-outcome (live-wire gates first)"
        (binding [fr/*gamma-grounded-feed?* true
                  fr/*live-wire?* false]
          (is (not (contains? (fr/with-realized-outcome jo decision enacted 7)
                              :realized-outcome))))))))

(deftest end-to-end-producer-to-trace-to-gamma
  (testing "producer → trace-record → reader → γ updates; then dedups on :tick"
    ;; pins the COVERAGE producer (realized-outcome-of) round-trip; grounded-feed
    ;; is default-on since 2026-07-08, so bind it off to exercise this path.
    (binding [fr/*live-wire?* true
              fr/*gamma-grounded-feed?* false]
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
