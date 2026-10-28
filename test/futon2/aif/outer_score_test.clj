(ns futon2.aif.outer-score-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.outer-score :as score]))

(def uniform-state-counts
  {:ready {:doable 1 :blocked 1}
   :not-ready {:doable 1 :blocked 1}
   :undetermined {:doable 1 :blocked 1}})
(def uniform-gain {:none 1 :one 1 :several 1})
(def uniform-ending
  {:focused 1 :related 1 :unrelated 1 :stop-the-line 1 :not-yet-evaluated 1})
(def relation-focus (fn [_ _] :focus))
(defn gate-target [target] {:gate #(= target (:target %))})
(defn facts [rows] (merge (zipmap (range 100) (repeat {:status :not-ready :unblocks 0 :cone 0})) rows))
(defn input [field facts relation ending gain cascade]
  {:focus 0 :relation relation :facts facts :field field
   :state-counts uniform-state-counts :ending-counts ending
   :gain-counts gain :cascade cascade})

(deftest outer-ambiguity-matters
  (testing "Lean outer_ambiguity_matters"
    (let [field [{:target 1 :reach 1} {:target 2 :reach 1/2}]
          fs (facts {1 {:status :ready :unblocks 0 :cone 0}
                     2 {:status :ready :unblocks 0 :cone 0}})
          one (score/outer-score (input field fs relation-focus uniform-ending uniform-gain
                                        [(gate-target 1)]))
          half (score/outer-score (input field fs relation-focus uniform-ending uniform-gain
                                         [(gate-target 2)]))]
      (is (< one half)))))

(deftest outer-ready-gain-matters
  (testing "Lean outer_ready_gain_matters"
    (let [field [{:target 1 :reach 1} {:target 2 :reach 1}]
          fs (facts {1 {:status :ready :unblocks 0 :cone 0}
                     2 {:status :ready :unblocks 2 :cone 0}})
          gain {:none 1 :one 1 :several 2}
          none (score/outer-score (input field fs relation-focus uniform-ending gain
                                         [(gate-target 1)]))
          several (score/outer-score (input field fs relation-focus uniform-ending gain
                                            [(gate-target 2)]))]
      (is (< several none)))))

(deftest ambiguity-changes-outer-choice
  (testing "Lean ambiguity_changes_outer_choice"
    (let [field [{:target 1 :reach 1/2} {:target 2 :reach 1}]
          fs (facts {1 {:status :ready :unblocks 0 :cone 0}
                     2 {:status :ready :unblocks 0 :cone 0}})
          relation (fn [_ target] (if (= 1 target) :focus :associated))
          c {:focused 1/2 :related 1/5 :unrelated 0
             :stop-the-line 3/10 :not-yet-evaluated 0}
          x (input field fs relation c uniform-gain [(gate-target 1)])
          y (input field fs relation c uniform-gain [(gate-target 2)])
          preference (score/outer-preference c uniform-gain)
          risk-only (fn [model]
                      (let [q (:q model) a (:kernel model)]
                        (reduce + 0.0
                                (for [outcome (keys preference)
                                      :let [p (reduce + (for [[s mass] q]
                                                            (* mass (get-in a [s outcome] 0.0))))]
                                      :when (pos? p)]
                                  (* p (Math/log (/ p (preference outcome))))))))]
      (is (< (risk-only (score/outer-model x)) (risk-only (score/outer-model y))))
      (is (< (score/outer-score y) (score/outer-score x))))))

(deftest learned-selection-flips
  (testing "Lean learned_selection_click_zero / learned_selection_click_one"
    (let [field [{:target 1 :reach 1/2} {:target 3 :reach 1/2}]
          fs (facts {1 {:status :ready :unblocks 0 :cone 0}
                     3 {:status :ready :unblocks 0 :cone 0}})
          relation (fn [_ target] (if (= 1 target) :focus :associated))
          kit {:focused 55 :related 35 :unrelated 5 :stop-the-line 5
               :not-yet-evaluated 0}
          learned {:focused 99 :related 263 :unrelated 9 :stop-the-line 9
                   :not-yet-evaluated 0}
          a [(gate-target 1)] b [(gate-target 3)]]
      (is (< (score/outer-score (input field fs relation kit uniform-gain a))
             (score/outer-score (input field fs relation kit uniform-gain b))))
      (is (< (score/outer-score (input field fs relation learned uniform-gain b))
             (score/outer-score (input field fs relation learned uniform-gain a)))))))

(deftest outer-novelty-theorems
  (testing "Lean outerKernelNovelty_uninformative / revealing / reach_half_lt_one"
    (let [base {:focus 0 :relation relation-focus
                :facts (facts {1 {:status :ready :unblocks 0 :cone 0}})
                :field [{:target 1 :reach 1}]
                :state-counts uniform-state-counts :cascade [(gate-target 1)]}
          revealing (score/outer-novelty (score/outer-model base))
          half (score/outer-novelty
                (score/outer-model (assoc base :field [{:target 1 :reach 1/2}])))
          stopped (score/outer-model (assoc base :cascade [{:gate (constantly false)}]))]
      (is (< (Math/abs (- revealing (/ 1.0 (* 2 (+ 1 1 1))))) 1.0e-9))
      (is (< half revealing))
      (is (zero? (score/outer-novelty stopped)))
      (is (zero? (score/outer-novelty
                  (assoc (score/outer-model base)
                         :kernel {:doable {[:stop-the-line :none] 1.0}
                                  :blocked {[:stop-the-line :none] 1.0}})))))))

(deftest k32-what-others-wait-on-first
  (testing "Lean K3.2 whatOthersWaitOnFirst example"
    (let [field [{:target 1 :reach 1} {:target 2 :reach 1} {:target 3 :reach 1}]
          fs {1 {:status :ready :unblocks 2 :cone 2}
              2 {:status :not-ready :unblocks 0 :cone 0}
              3 {:status :not-ready :unblocks 0 :cone 0}}
          cascade [:ready-gate :by-cone :by-unblocks]]
      (is (= {:target 1 :reach 1} (score/cascade-pick cascade fs field))))))

(deftest novelty-decides-outer-choice
  (testing "Lean slice 3f-iii (novelty decides): equal q, class, reach and gain;
            only the status's total counts differ, so the less-known pick scores lower"
    (let [field [{:target 1 :reach 1/2} {:target 2 :reach 1/2}]
          fs (facts {1 {:status :ready :unblocks 0 :cone 0}
                     2 {:status :undetermined :unblocks 0 :cone 0}})
          counts {:ready {:doable 10 :blocked 10}
                  :not-ready {:doable 1 :blocked 1}
                  :undetermined {:doable 1 :blocked 1}}
          in (fn [cascade] (assoc (input field fs relation-focus uniform-ending uniform-gain cascade)
                                  :state-counts counts))
          known (in [(gate-target 1)])
          unknown (in [(gate-target 2)])]
      (is (= (:q (score/outer-model known)) (:q (score/outer-model unknown))))
      (is (< (score/outer-novelty (score/outer-model known))
             (score/outer-novelty (score/outer-model unknown))))
      (is (< (score/outer-score unknown) (score/outer-score known))))))
