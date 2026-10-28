(ns futon2.aif.outer-state-update-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.outer-score :as outer-score]
            [futon2.aif.outer-state :as state]))

(def makeup
  {:schema :wm/outer-makeup-v1
   :makeup/sha256 (apply str (repeat 64 "a"))
   :c-kit {:focused 55/100 :related 35/100 :unrelated 5/100
           :stop-the-line 5/100 :not-yet-evaluated 0}
   :kappa 1
   :state-kit {:ready {:doable 1 :blocked 1}
               :not-ready {:doable 1 :blocked 3}
               :undetermined {:doable 1 :blocked 1}}
   :gain-kit {:none 1 :one 2 :several 3}
   :eta-c 1 :omega-c 9/10
   :eta-state 1 :omega-state 9/10
   :eta-gain 1 :omega-gain 9/10
   :default-reach 1/2
   :cascades [{:id :ready :patterns ["test"] :steps [:ready-gate]}]})

(def candidate {:target "M-task" :reach 1/2})

(def observation
  {:focus "M-focus"
   :relation (fn [_ _] :associated)
   :field [candidate]
   :facts {"M-task" {:status :ready :unblocks 1 :cone 1}}})

(def selection
  {:cascade-index 0
   :cascade-id :ready
   :cascade-scores [0.0]
   :selection {:candidates [candidate] :selected candidate}})

(defn approx= [expected actual]
  (< (Math/abs (- (double expected) (double actual))) 1.0e-12))

(deftest gain-counts-stay-positive
  (testing "Lean advanceGainCounts_pos"
    (let [advanced (state/advance-gain-counts 1 9/10
                                              {:none 1 :one 2 :several 3}
                                              :one true)]
      (is (every? pos? (vals advanced)))
      (is (= {:none 9/10 :one 14/5 :several 27/10} advanced))))
  (testing "no selection decays every gain and adds no observation"
    (is (= {:none 1/2 :one 1 :several 3/2}
           (state/advance-gain-counts 7 1/2
                                      {:none 1 :one 2 :several 3}
                                      :several false)))))

(deftest selected-status-learns-and-other-status-decays
  (testing "Lean advanceStateCounts selected-status closed form"
    (let [before (:state-kit makeup)
          model (outer-score/outer-model
                 (assoc observation :state-counts before :cascade [:ready-gate]))
          advanced (state/advance-state-counts 1 9/10 before model
                                                [:stop-the-line :none])]
      ;; With ready counts (1,1), reach 1/2 and a stop observation, the
      ;; posterior is (1/3,2/3).
      (is (approx= (+ 9/10 1/3) (get-in advanced [:ready :doable])))
      (is (approx= (+ 9/10 2/3) (get-in advanced [:ready :blocked])))
      (is (= {:doable 9/10 :blocked 27/10} (:not-ready advanced)))
      (is (= {:doable 9/10 :blocked 9/10} (:undetermined advanced))))))

(deftest state-learning-moves-belief
  (testing "Lean state_learning_moves_belief"
    (let [before (:state-kit makeup)
          model (outer-score/outer-model
                 (assoc observation :state-counts before :cascade [:ready-gate]))
          stop (state/advance-state-counts 1 1 before model
                                           [:stop-the-line :none])
          landing (state/advance-state-counts 1 1 before model
                                              [:related :one])]
      (is (> (get-in stop [:ready :blocked])
             (get-in stop [:ready :doable])))
      (is (> (get-in landing [:ready :doable])
             (get-in landing [:ready :blocked])))))
  (testing "an impossible observation has zero posterior evidence"
    (let [before (:state-kit makeup)
          model (outer-score/outer-model
                 (assoc observation :state-counts before :cascade [:ready-gate]))
          advanced (state/advance-state-counts 1 9/10 before model
                                               [:unrelated :several])]
      (is (every? true?
                  (for [status [:ready :not-ready :undetermined]
                        task-state [:doable :blocked]]
                    (approx= (* 9/10 (get-in before [status task-state]))
                             (get-in advanced [status task-state]))))))))

(deftest learned-c-matches-slice-3b
  (testing "Lean learned_selection_click_one C update"
    (let [before (:c (state/init-state makeup))
          after (state/advance-c 1 9/10 before :related)
          total (reduce + (vals after))
          normalized (update-vals after #(/ % total))]
      (is (= {:focused 99/380 :related 263/380 :unrelated 9/380
              :stop-the-line 9/380 :not-yet-evaluated 0}
             normalized)))))

(deftest next-state-combines-the-updates
  (testing "the state part of Lean step"
    (let [before (assoc (state/init-state makeup) :source :init)
          after (state/next-state makeup before selection observation
                                  {:ending-class :related :observed-gain :one})]
      (is (= 1 (:click-count after)))
      (is (not (contains? after :source)))
      (is (= 14/5 (get-in after [:gain-counts :one])))
      (is (= 263/200 (get-in after [:c :related])))
      (is (> (get-in after [:state-counts :ready :doable])
             (get-in after [:state-counts :ready :blocked]))))))
