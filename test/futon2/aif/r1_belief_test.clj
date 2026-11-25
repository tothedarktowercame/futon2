(ns futon2.aif.r1-belief-test
  "Tests transcribed from the named theorems of
  DarkTower/WarMachine/R1DeepTemporal.lean and R1DeepTemporalB17.lean
  (mathlib4 worktree c14-cascade-outcome, branch c14/cascade-outcome-type).
  All arithmetic is exact ratios, as the Lean uses real literals."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.r1-belief :as r1]))

(def examples
  (edn/read-string (slurp (io/resource "futon2/aif/r1_belief_examples.edn"))))

(def b17 (:b17 examples))

(def uniform (:uniform-prior b17))

(def trace-channel
  "R1DeepTemporalB17.traceChannel: at rung k, :trace with traceProb(k),
  :no-trace with 1 - traceProb(k)."
  (let [tp (:trace-prob b17)]
    (fn [rung kind]
      (case kind
        :trace (get tp rung)
        :no-trace (- 1 (get tp rung))))))

;;; ## update theorems (R1DeepTemporal.lean)

(deftest update-nil
  (testing "R1DeepTemporal.update_nil: no records leave the belief unchanged"
    (is (= uniform (r1/update-belief uniform trace-channel [])))
    (let [skewed {:none 1/2 :available 1/4 :used 1/8 :witnessed 1/16 :realised 1/16}]
      (is (= skewed (r1/update-belief skewed trace-channel []))))))

(deftest update-cons
  (testing "R1DeepTemporal.update_cons: one record then the rest equals all at once (exact)"
    (let [records [:no-trace :trace :no-trace :trace]
          stepwise (r1/update-belief
                    (r1/update-belief uniform trace-channel [(first records)])
                    trace-channel (rest records))
          at-once (r1/update-belief uniform trace-channel records)]
      (is (= at-once stepwise))
      (is (= 1 (reduce + 0 (vals at-once))))
      (is (every? ratio? (vals at-once))))))

(deftest update-flat
  (testing "R1DeepTemporal.update_flat: a record equally likely at every rung moves nothing"
    (let [flat (fn [_rung _kind] 3/10)
          belief {:none 1/2 :available 1/4 :used 1/8 :witnessed 1/16 :realised 1/16}]
      (is (= belief (r1/update-belief belief flat [:anything])))
      (is (= uniform (r1/update-belief uniform flat [:anything]))))))

(deftest update-pinned
  (testing "R1DeepTemporal.update_pinned: likelihood zero except at :witnessed makes it certain"
    (let [pinned (fn [rung _kind] (if (= :witnessed rung) 1/3 0))]
      (is (= {:none 0 :available 0 :used 0 :witnessed 1 :realised 0}
             (r1/update-belief uniform pinned [:seen-only-here]))))))

;;; ## B17 instance (R1DeepTemporalB17.lean)

(deftest b17-notrace-realised
  (testing "R1DeepTemporalB17.evid_noTrace / noTrace_realised (data from the edn)"
    (let [records (:records b17)
          no-trace (first (filter #(= :no-trace (:kind %)) records))]
      (is (some? no-trace) "the edn carries o6, the :no-trace record")
      (is (= :o6 (:id no-trace)))
      (is (= (:evid-no-trace (:expected b17))
             (r1/evid uniform trace-channel [no-trace]))
          "evid_noTrace: evidence of the no-trace record is 17/25")
      (let [posterior (r1/update-belief uniform trace-channel [no-trace])]
        (is (= (:realised-after-no-trace (:expected b17))
               (get posterior :realised))
            "noTrace_realised: REALISED moves from 1/5 to 3/34 exactly")
        (is (= 1 (reduce + 0 (vals posterior))))))))

(deftest b17-examples-edn-shape
  (testing "the edn carries all seven dated records and the six want clauses"
    (is (= [:o1 :o2 :o3 :o4 :o5 :o6 :o7] (mapv :id (:records b17))))
    (is (every? :date (:records b17)))
    (is (every? :source (:records b17)))
    (is (= 6 (count (get-in examples [:wm-loop-want :clauses]))))
    (is (every? #(and (:clause %) (:source %) (:verdict %))
                (get-in examples [:wm-loop-want :clauses])))))

;;; ## R1-cause theorems (CauseCounts, theta, learn)

(def counts
  "Two links, columns all positive, exact ratios."
  {:design-link {:none [1 3] :available [1 3] :used [1 1]
                 :witnessed [2 1] :realised [3 1]}
   :benchmark-link {:none [1 9] :available [1 9] :used [1 9]
                    :witnessed [1 9] :realised [1 9]}})

(deftest learn-theta
  (testing "R1DeepTemporal.learn_theta: theta moves toward w in proportion to the column's belief"
    (let [s-prev {:none 1/2 :available 1/4 :used 1/8 :witnessed 1/16 :realised 1/16}
          w 3/4
          rung :witnessed
          [c s] (get-in counts [:design-link rung])
          theta-before (r1/theta counts :design-link rung)
          learnt (r1/learn counts :design-link s-prev w)
          theta-after (r1/theta learnt :design-link rung)
          expected (+ theta-before
                      (/ (* (get s-prev rung) (- w theta-before))
                         (+ c s (get s-prev rung))))]
      (is (= expected theta-after))
      (is (= (/ (+ c (* (get s-prev rung) w))
                (+ c s (get s-prev rung)))
             theta-after)))))

(deftest learn-other-link
  (testing "R1DeepTemporal.learn_other_link: links other than the credited one are untouched"
    (let [s-prev {:none 1/5 :available 1/5 :used 1/5 :witnessed 1/5 :realised 1/5}
          learnt (r1/learn counts :design-link s-prev 2/3)]
      (is (= (:benchmark-link counts) (:benchmark-link learnt)))
      (is (not= (:design-link counts) (:design-link learnt))))))

(deftest learn-other-col
  (testing "R1DeepTemporal.learn_other_col: a starting rung the slow belief rules out gains nothing"
    (let [s-prev {:none 0 :available 1/4 :used 1/4 :witnessed 1/4 :realised 1/4}
          learnt (r1/learn counts :design-link s-prev 1)]
      (is (= (get-in counts [:design-link :none])
             (get-in learnt [:design-link :none])))
      (is (= (get-in learnt [:design-link :used])
             [(+ 1 1/4) 1])
          "a ruled-in rung gains sPrev(rung) * w climbs and sPrev(rung) * (1 - w) stays"))))

;;; ## Climb kernel and prediction (BenefitBelief.climb, R1DeepTemporal.predict)

(deftest climb-kernel
  (testing "BenefitBelief.climb: up with theta, stay with 1-theta, realised stays"
    (is (= {:none {:none 1/4 :available 3/4}
            :available {:available 1/4 :used 3/4}
            :used {:used 1/4 :witnessed 3/4}
            :witnessed {:witnessed 1/4 :realised 3/4}
            :realised {:realised 1}}
           (r1/climb 3/4)))))

(deftest predict-realised
  (testing "predictRung_realised through R1DeepTemporal.predict with pD = 1:
            predicted realised mass = belief realised + theta * belief witnessed"
    (let [belief {:none 1/8 :available 1/8 :used 1/8 :witnessed 1/8 :realised 1/2}
          theta 2/3
          predicted (r1/predict belief theta 1)]
      (is (= (+ (get belief :realised) (* theta (get belief :witnessed)))
             (get predicted :realised)))
      (testing "pD = 0 leaves the belief unchanged"
        (is (= belief (r1/predict belief theta 0))))
      (testing "serving (theta > 0, pD = 1) raises the realised mass"
        (is (< (get belief :realised) (get predicted :realised)))))))

;;; ## Planted bad cases: each refusal the Lean states as a hypothesis or
;;; structure field must fire here.

(deftest belief-must-sum-to-one
  (testing "a belief summing to 0.9 is refused (SlowBelief.sum_one)"
    (is (thrown? clojure.lang.ExceptionInfo
                 (r1/update-belief {:none 1/10 :available 1/5 :used 1/5
                                    :witnessed 1/5 :realised 1/5}
                                   trace-channel [:no-trace])))
    (is (try (r1/evid {:none 1/10 :available 1/5 :used 1/5
                       :witnessed 1/5 :realised 1/5}
                      trace-channel [:no-trace])
             false
             (catch clojure.lang.ExceptionInfo e
               (= :invalid-belief (:type (ex-data e))))))))

(deftest zero-evidence-refused
  (testing "zero evidence is refused, not NaN (update's hypothesis 0 < evid)"
    (let [impossible (fn [_rung _kind] 0)]
      (is (try (r1/update-belief uniform impossible [:impossible])
               false
               (catch clojure.lang.ExceptionInfo e
                 (= :zero-evidence (:type (ex-data e)))))))))

(deftest negative-likelihood-refused
  (testing "a likelihood giving a negative probability is refused (kernel nonnegativity)"
    (let [bad (fn [rung _kind] (if (= :none rung) -1/10 1/2))]
      (is (try (r1/update-belief uniform bad [:record])
               false
               (catch clojure.lang.ExceptionInfo e
                 (= :invalid-likelihood (:type (ex-data e)))))))))

(deftest negative-belief-entry-refused
  (testing "a belief with a negative entry is refused (SlowBelief.nonneg)"
    (is (thrown? clojure.lang.ExceptionInfo
                 (r1/check-belief {:none -1/5 :available 2/5 :used 2/5
                                   :witnessed 2/5 :realised 2/5})))))

(deftest cause-counts-must-be-positive
  (testing "cause counts with a non-positive entry are refused (CauseCounts.pos)"
    (is (thrown? clojure.lang.ExceptionInfo
                 (r1/theta (assoc-in counts [:design-link :used] [0 1]) :design-link :used)))))
