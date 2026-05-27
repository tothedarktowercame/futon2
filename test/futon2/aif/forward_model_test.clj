(ns futon2.aif.forward-model-test
  "Tests for the WM AIF predictive forward model (R4)."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.belief :as belief]
            [futon2.aif.forward-model :as fm]))

(def ^:private base-obs
  "An in-distribution observation midway between healthy and stressed."
  {:loop-health 0.7
   :support-coverage 0.7
   :attack-coverage 0.7
   :mission-health 0.5
   :stack-pct 0.3
   :consulting-pct 0.2
   :portfolio-pct 0.25
   :mathematics-pct 0.2
   :active-repo-ratio 0.6
   :sorry-count-norm 0.4
   :coupling-density 0.2
   :ticks-firing-ratio 0.1
   :depositing-signal 0.1})

(def ^:private base-state
  {:observation base-obs
   :belief (belief/initial-belief-state [:m1 :m2])})

(deftest predict-purity-test
  (testing "predict is pure: same (state, action) → same output"
    (let [action {:type :address-sorry :target :m1 :weight 1.0}
          out1 (fm/predict base-state action)
          out2 (fm/predict base-state action)]
      (is (= out1 out2)))))

(deftest predict-shape-test
  (testing "predict returns the documented keys"
    (let [out (fm/predict base-state {:type :no-op})]
      (is (contains? out :next-observation))
      (is (contains? out :next-belief))
      (is (contains? out :action))
      (is (contains? out :predicted-events))
      (is (contains? (:next-observation out) :mean))
      (is (contains? (:next-observation out) :variance))))
  (testing "next-observation mean and variance have the same channel keys"
    (let [{{:keys [mean variance]} :next-observation}
          (fm/predict base-state {:type :address-sorry :target :m1})]
      (is (= (set (keys mean)) (set (keys variance))))))
  (testing "variance values are non-negative"
    (let [{{:keys [variance]} :next-observation}
          (fm/predict base-state {:type :address-sorry :target :m1})]
      (is (every? #(>= % 0.0) (vals variance))))))

(deftest predict-no-op-preserves-state-test
  (testing ":no-op leaves observation unchanged"
    (let [out (fm/predict base-state {:type :no-op})]
      (is (= base-obs (get-in out [:next-observation :mean])))))
  (testing ":no-op leaves belief unchanged"
    (let [out (fm/predict base-state {:type :no-op})]
      (is (= (:belief base-state) (:next-belief out)))))
  (testing ":no-op emits no predicted events"
    (is (empty? (:predicted-events (fm/predict base-state {:type :no-op}))))))

(deftest predict-address-sorry-effects-test
  (let [action {:type :address-sorry :target :m1 :weight 1.0}
        out (fm/predict base-state action)
        before-sorry (:sorry-count-norm base-obs)
        after-sorry (get-in out [:next-observation :mean :sorry-count-norm])
        before-belief (get-in base-state [:belief :m1 :addressed])
        after-belief (get-in out [:next-belief :m1 :addressed])]
    (testing "addressing a sorry decreases sorry-count-norm"
      (is (< after-sorry before-sorry)))
    (testing "addressing a sorry raises mission-health"
      (is (> (get-in out [:next-observation :mean :mission-health])
             (:mission-health base-obs))))
    (testing "addressing shifts target entity's belief toward :addressed"
      (is (> after-belief before-belief)))
    (testing "predicted event is the :addressed event applied"
      (let [events (:predicted-events out)]
        (is (= 1 (count events)))
        (is (= :addressed (-> events first :type)))
        (is (= :m1 (-> events first :entity-id)))))))

(deftest predict-open-mission-effects-test
  (let [out (fm/predict base-state {:type :open-mission :target :m-new :weight 1.0})]
    (testing "opening a mission raises mission-health and active-repo-ratio"
      (is (> (get-in out [:next-observation :mean :mission-health])
             (:mission-health base-obs)))
      (is (> (get-in out [:next-observation :mean :active-repo-ratio])
             (:active-repo-ratio base-obs))))
    (testing "the new mission appears in the belief state"
      (is (contains? (:next-belief out) :m-new)))
    (testing "the new mission is shifted toward :spawned"
      (let [p (get-in out [:next-belief :m-new])
            n (count belief/status-set)
            uniform-p (/ 1.0 n)]
        (is (> (:spawned p) uniform-p))))))

(deftest predict-fire-pattern-effects-test
  (let [out (fm/predict base-state {:type :fire-pattern :target :m1})]
    (testing "firing a pattern raises ticks-firing-ratio"
      (is (> (get-in out [:next-observation :mean :ticks-firing-ratio])
             (:ticks-firing-ratio base-obs))))
    (testing "firing a pattern shifts target toward :strengthened"
      (is (> (get-in out [:next-belief :m1 :strengthened])
             (get-in base-state [:belief :m1 :strengthened]))))))

(deftest predict-clamps-observation-to-unit-interval-test
  (testing "observation values stay in [0,1] under extreme actions"
    (let [extreme-obs (assoc base-obs :sorry-count-norm 0.05)
          state {:observation extreme-obs :belief {}}
          out (fm/predict state {:type :address-sorry :target :m1})]
      (is (>= (get-in out [:next-observation :mean :sorry-count-norm]) 0.0)
          "clamps below zero — the action would otherwise push to -0.05"))))

(deftest predict-rejects-invalid-action-test
  (testing "unknown action type raises ex-info"
    (is (thrown? clojure.lang.ExceptionInfo
                 (fm/predict base-state {:type :not-a-real-action :target :m1}))))
  (testing "missing :target for non-:no-op action raises ex-info"
    (is (thrown? clojure.lang.ExceptionInfo
                 (fm/predict base-state {:type :address-sorry})))))

(deftest predict-handles-empty-state-test
  (testing "missing observation defaults to empty map; predict still returns shape"
    (let [out (fm/predict {} {:type :no-op})]
      (is (map? out))
      (is (= {} (get-in out [:next-observation :mean]))))))

(deftest predict-next-belief-is-distribution-test
  (testing "next-belief entries are valid posteriors over status-set"
    (let [out (fm/predict base-state {:type :address-sorry :target :m1})
          posterior (get-in out [:next-belief :m1])]
      (is (= belief/status-set (set (keys posterior))))
      (is (< (Math/abs (- 1.0 (reduce + (vals posterior)))) 1e-9)
          "posterior sums to 1.0"))))

(deftest learn-action-class-validation-test
  (testing ":learn-action-class is a valid action type"
    (is (contains? fm/action-types :learn-action-class)))
  (testing ":learn-action-class requires :target-class, not :target"
    (is (thrown? clojure.lang.ExceptionInfo
                 (fm/predict base-state {:type :learn-action-class})))
    (is (map? (fm/predict base-state {:type :learn-action-class
                                       :target-class :address-sorry})))))

(deftest learn-action-class-effects-test
  (testing ":learn-action-class has no observation delta or events"
    (let [out (fm/predict base-state {:type :learn-action-class
                                      :target-class :address-sorry})]
      (is (= base-obs (get-in out [:next-observation :mean]))
          "observation unchanged — capability gap action doesn't move stack state")
      (is (= (:belief base-state) (:next-belief out))
          "belief unchanged")
      (is (empty? (:predicted-events out))))))

(deftest can-propose-defaults-test
  (testing "can-propose? returns false by default for unknown action types"
    (is (false? (fm/can-propose? base-state :address-sorry))
        "no substrate adapter for :address-sorry; should be false"))
  (testing ":no-op is always proposable"
    (is (true? (fm/can-propose? base-state :no-op))))
  (testing ":learn-action-class is always proposable"
    (is (true? (fm/can-propose? base-state :learn-action-class))))
  (testing "other action types currently default to false (no adapter shipped)"
    (is (false? (fm/can-propose? base-state :open-mission)))
    (is (false? (fm/can-propose? base-state :fire-pattern)))))

;; ---------------------------------------------------------------------------
;; v0.13: can-execute? per-action-instance admissibility check
;; ---------------------------------------------------------------------------

(deftest can-execute-defaults-test
  (testing ":no-op and :learn-action-class always executable"
    (is (true? (fm/can-execute? {} {:type :no-op})))
    (is (true? (fm/can-execute? {} {:type :learn-action-class
                                    :target-class :address-sorry})))))

(deftest can-execute-address-sorry-requires-target-in-state-test
  (testing ":address-sorry executable iff target in state's :sorrys"
    (let [state {:sorrys [{:id :sorry/x :status :open}
                          {:id :sorry/y :status :open}]}]
      (is (true? (fm/can-execute? state {:type :address-sorry :target :sorry/x})))
      (is (true? (fm/can-execute? state {:type :address-sorry :target :sorry/y})))
      (is (false? (fm/can-execute? state {:type :address-sorry :target :sorry/missing}))
          "target not in state → not executable")))
  (testing "empty state's :sorrys → all :address-sorry instances inadmissible"
    (is (false? (fm/can-execute? {:sorrys []} {:type :address-sorry :target :sorry/x})))))

(deftest can-execute-unknown-action-defaults-true-test
  (testing "unknown action types default to true (proposer-side gating responsibility)"
    (is (true? (fm/can-execute? {} {:type :brand-new-action :target :x})))))

;; ---------------------------------------------------------------------------
;; v0.15: predict-multi-horizon
;; ---------------------------------------------------------------------------

(deftest predict-multi-horizon-shape-test
  (testing "predict-multi-horizon returns trajectory + final-state + K"
    (let [out (fm/predict-multi-horizon base-state {:type :no-op} 3)]
      (is (contains? out :trajectory))
      (is (contains? out :final-state))
      (is (= 3 (:horizon-steps out)))
      (is (= 3 (count (:trajectory out)))))))

(deftest predict-multi-horizon-no-op-preserves-test
  (testing ":no-op chained K times preserves the original observation"
    (let [out (fm/predict-multi-horizon base-state {:type :no-op} 5)]
      (is (= (:observation base-state)
             (get-in out [:final-state :observation]))
          "no-op repeated 5 times = identity"))))

(deftest predict-multi-horizon-address-sorry-accumulates-test
  (testing ":address-sorry chained K times accumulates the observation shift"
    (let [k1 (fm/predict-multi-horizon base-state {:type :address-sorry :target :m1} 1)
          k3 (fm/predict-multi-horizon base-state {:type :address-sorry :target :m1} 3)
          obs-k1-sorry (get-in k1 [:final-state :observation :sorry-count-norm])
          obs-k3-sorry (get-in k3 [:final-state :observation :sorry-count-norm])]
      (is (< obs-k3-sorry obs-k1-sorry)
          "K=3 :address-sorry chained = more sorry-count reduction than K=1"))))

(deftest predict-multi-horizon-trajectory-belief-chains-test
  (testing "each step's belief is the next step's input belief"
    (let [out (fm/predict-multi-horizon base-state {:type :address-sorry :target :m1} 2)
          step1 (first (:trajectory out))
          step2 (second (:trajectory out))]
      ;; step2's :prediction used step1's :next-belief as input;
      ;; verify the belief in step2's prediction reflects the chained update
      (is (not= (get-in step1 [:next-belief :m1])
                (get-in step2 [:next-belief :m1]))
          "step2's belief is further-shifted than step1's (one more event applied)"))))
