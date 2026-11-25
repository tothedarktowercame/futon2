(ns futon2.aif.r1-three-axes-test
  "Tests transcribed from the named theorems of
  DarkTower/WarMachine/R1ThreeAxes.lean and
  DarkTower/WarMachine/R1ThreeAxesWitness.lean (mathlib4 worktree
  c14-cascade-outcome, branch c14/cascade-outcome-type). All arithmetic is
  exact ratios, as the Lean uses real literals."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.r1-three-axes :as r1t]))

;;; ## card_benefitState

(deftest card-benefitState
  (testing "R1ThreeAxes.card_benefitState: 15 states"
    (is (= 15 (count r1t/states)))
    (is (= 15 (count (distinct r1t/states))))
    (testing "habit_implies_capability: every habit state has CAPABILITY"
      (is (every? (fn [s] (or (not (:habit s)) (= :capability (:avail s)))) r1t/states)))
    (testing "3 reach values x (4 avail without habit + 1 with)"
      (is (= 12 (count (filter #(not (:habit %)) r1t/states))))
      (is (= 3 (count (filter :habit r1t/states)))))))

;;; ## witness_pending_marginal (uniform, PENDING reading at 3/4)

(def uniform (r1t/uniform-belief))

(def pending-reading
  "R1ThreeAxesWitness.pendingReading: a reader claiming availability
  PENDING with reliability 3/4. A :says-true record is the Lean's `true`."
  (r1t/axis-reading :avail :pending 3/4))

(deftest witness-pending-marginal
  (testing "R1ThreeAxesWitness.witness_pending_marginal: uniform + one PENDING reading at 3/4 -> avail-marginal :pending = 3/7"
    (let [updated (r1t/update-belief uniform pending-reading [:says-true])]
      (is (= 3/7 (r1t/avail-marginal updated :pending)))
      (is (ratio? (r1t/avail-marginal updated :pending)))
      (is (= 1 (reduce + 0 (vals updated))))
      (is (every? ratio? (vals updated))))))

(deftest witness-reach-unmoved
  (testing "R1ThreeAxesWitness.witness_reach_unmoved: the same update leaves reach-marginal at 1/3 for each reach value"
    (let [updated (r1t/update-belief uniform pending-reading [:says-true])]
      (doseq [r r1t/reach-values]
        (is (= 1/3 (r1t/reach-marginal updated r)))))))

;;; ## habit_record_pins_capability

(deftest habit-record-pins-capability
  (testing "R1ThreeAxes.habit_record_pins_capability: a record with likelihood 0 wherever :habit is false pins CAPABILITY"
    (let [habit-only (fn [state kind]
                       (case kind
                         :habit-trace (if (:habit state) 1/2 0)
                         1))
          updated (r1t/update-belief uniform habit-only [:habit-trace])]
      (is (= 1 (r1t/avail-marginal updated :capability)))
      (is (= 1 (r1t/habit-marginal updated true)))
      (is (= 0 (r1t/avail-marginal updated :pending)))
      (is (every? (fn [[s p]] (or (:habit s) (zero? p))) updated)))))

;;; ## update_cons

(deftest update-cons
  (testing "R1ThreeAxes.update_cons: one record then another equals both at once (exact)"
    (let [records [:says-true :says-true :says-false]
          stepwise (r1t/update-belief
                    (r1t/update-belief uniform pending-reading [(first records)])
                    pending-reading (rest records))
          at-once (r1t/update-belief uniform pending-reading records)]
      (is (= at-once stepwise))
      (is (= 1 (reduce + 0 (vals at-once))))
      (is (every? ratio? (vals at-once))))))

;;; ## Marginals sum to one (availMarginal_sum / reachMarginal_sum / habitMarginal_sum)

(deftest marginals-sum
  (testing "R1ThreeAxes.availMarginal_sum / reachMarginal_sum / habitMarginal_sum"
    (let [updated (r1t/update-belief uniform pending-reading [:says-true])]
      (is (= 1 (reduce + 0 (map #(r1t/avail-marginal updated %) r1t/avail-values))))
      (is (= 1 (reduce + 0 (map #(r1t/reach-marginal updated %) r1t/reach-values))))
      (is (= 1 (reduce + 0 (map #(r1t/habit-marginal updated %) [false true])))))))

;;; ## Planted bad cases (each must fail if the guard is removed)

(deftest check-belief-refuses-habit-without-capability
  (testing "Mutant: remove the :habit -> :capability constraint from check-state.
  A state with mass on {:avail :pending :habit true} is refused."
    (let [bad-state {:avail :pending :habit true :reach :none}]
      (is (thrown? clojure.lang.ExceptionInfo (r1t/check-state bad-state)))
      (is (thrown? clojure.lang.ExceptionInfo
                   (r1t/check-belief (assoc uniform bad-state 0)))))))

(deftest check-belief-refuses-malformed
  (testing "check-belief refuses a missing state, an extra state, negative mass, and mass not summing to 1"
    (is (thrown? clojure.lang.ExceptionInfo
                 (r1t/check-belief (dissoc uniform (first r1t/states)))))
    (is (thrown? clojure.lang.ExceptionInfo
                 (r1t/check-belief (assoc uniform {:avail :pending :habit true :reach :none} 0))))
    (is (thrown? clojure.lang.ExceptionInfo
                 (r1t/check-belief (assoc uniform (first r1t/states) -1/30
                                          (second r1t/states) (+ 1/15 1/30)))))
    (is (thrown? clojure.lang.ExceptionInfo
                 (r1t/check-belief (assoc uniform (first r1t/states) 1/30))))))

(deftest update-flat
  (testing "R1ThreeAxes.update_flat / planted bad case: a flat reading leaves the belief unchanged.
  Mutant: remove the normalisation by evid in post (the flat likelihood then scales the belief off 1)."
    (let [flat (fn [_state _kind] 3/10)
          skewed (r1t/update-belief uniform pending-reading [:says-true])]
      (is (= uniform (r1t/update-belief uniform flat [:anything])))
      (is (= skewed (r1t/update-belief skewed flat [:anything]))))))

(deftest update-half-reliability-unmoved
  (testing "Planted bad case: a reading with r = 1/2 moves nothing (the likelihood is 1/2 everywhere).
  Mutant: swap the r and 1 - r branches in axis-reading (r = 1/2 is symmetric, so this test is the one that pins the flat case to the reading machinery)."
    (let [coin (r1t/axis-reading :avail :pending 1/2)
          skewed (r1t/update-belief uniform pending-reading [:says-true])]
      (is (= uniform (r1t/update-belief uniform coin [:says-true])))
      (is (= skewed (r1t/update-belief skewed coin [:says-false]))))))
