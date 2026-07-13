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

(deftest malformed-persisted-state-fails-closed-test
  (is (thrown-with-msg? clojure.lang.ExceptionInfo #"malformed persisted"
                        (habit/coerce-state {:version 1 :alpha 1.0
                                             :counts {(habit/policy-key a) 2}
                                             :samples 99}))))
