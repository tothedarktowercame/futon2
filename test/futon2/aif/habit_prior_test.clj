(ns futon2.aif.habit-prior-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.habit-prior :as habit]
            [futon2.aif.policy :as policy]))

(def a {:type :open-mission :target :m1})
(def b {:type :fire-pattern :target :m2})

(deftest stable-policy-identity-test
  (is (= (habit/policy-key (assoc a :rationale "one" :score 99))
         (habit/policy-key (assoc a :rationale "two" :score -1))))
  (is (nil? (habit/policy-key :abstain))))

(deftest symmetric-dirichlet-fold-test
  (let [state (habit/fold-records
               [{:decision {:action a}}
                {:decision {:action a}}
                {:decision {:action b}}
                {:decision {:action :abstain}}
                {}])
        [ln-a ln-b ln-unseen]
        (habit/log-priors state [a b {:type :no-op}])]
    (is (= 3 (:samples state)))
    (is (= 2 (get-in state [:counts (habit/policy-key a)])))
    (is (> ln-a ln-b))
    (is (> ln-b ln-unseen))
    (is (< (Math/abs (- 1.0 (reduce + (map #(Math/exp %) [ln-a ln-b ln-unseen]))))
           1.0e-12))))

(deftest learned-prior-reaches-canonical-softmax-seam-test
  (let [state (reduce habit/observe-action (habit/initial-state)
                      (concat (repeat 8 a) (repeat 2 b)))
        ranked [{:action a :controller-score 1.0}
                {:action b :controller-score 1.0}]
        attached (habit/attach-log-priors state ranked)
        biases (mapv :habit-prior-bias attached)
        weights (policy/softmax-weights (mapv :controller-score attached)
                                        1.0 biases)]
    (testing "frequency affects ln E, not G"
      (is (= [1.0 1.0] (mapv :controller-score attached)))
      (is (> (first biases) (second biases)))
      (is (> (first weights) (second weights))))))

(deftest feature-off-reduction-safety-test
  (let [ranked [{:action a :controller-score 0.2}
                {:action b :controller-score 0.3}]]
    (is (= (policy/select-action ranked)
           (policy/select-action ranked {:temperature-opts {}})))))

(deftest span-cap-default-off-and-enabled-G-governance-test
  (let [top-mission {:type :advance-mission
                     :target "M-expressions-of-interest"
                     :central 0.8 :strategic 0.7 :doable 0.9
                     :mission-value-factor 0.786}
        fire {:type :learn-action-class :target-class :fire-pattern}
        learning {:type :advance-mission :target "M-learning-loop"
                  :mission-value-factor 0.51}
        state (reduce habit/observe-action
                      (habit/initial-state)
                      (concat (repeat 250 fire) (repeat 70 learning)))
        ranked [{:action top-mission :controller-score 31.754 :rank 1}
                {:action learning :controller-score 32.166 :rank 40}
                {:action fire :controller-score 32.837 :rank 109}
                {:action {:type :no-op} :controller-score 33.337 :rank 114}]
        default-attached (habit/attach-log-priors state ranked)
        unset-attached (habit/attach-log-priors state ranked
                                                 {:span-ratio-cap nil})
        capped (habit/attach-log-priors state ranked {:span-ratio-cap 0.25})
        select #(policy/select-action
                 % {:selection-gain 1.0
                    :temperature-opts {:tau-mode :selection-gain-only}
                    :habit-prior-stats (habit/state-stats state)})
        default-decision (select default-attached)
        unset-decision (select unset-attached)
        capped-decision (select capped)
        capped-biases (mapv :habit-prior-bias capped)
        g-span (- 33.337 31.754)]
    (testing "unset takes the historical path exactly"
      (is (= default-attached unset-attached))
      (is (= default-decision unset-decision))
      (is (= :fire-pattern
             (get-in default-decision [:action :target-class]))))
    (testing "configured cap bounds lnE relative to G and lets best G govern"
      (is (<= (- (apply max capped-biases) (apply min capped-biases))
              (+ (* 0.25 g-span) 1.0e-12)))
      (is (= "M-expressions-of-interest"
             (get-in capped-decision [:action :target])))
      (is (= :G (get-in capped-decision
                        [:decision-explanation :governed-by]))))))

(deftest malformed-persisted-state-fails-closed-test
  (is (thrown-with-msg? clojure.lang.ExceptionInfo #"malformed persisted"
                        (habit/coerce-state {:version 1 :alpha 1.0
                                             :counts {(habit/policy-key a) 2}
                                             :samples 99}))))
