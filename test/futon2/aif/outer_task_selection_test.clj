(ns futon2.aif.outer-task-selection-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.outer-task-selection :as sut]))

(def tasks
  [{:id "M-alpha" :kind :mission :status-class :open :open-hole-count 2}
   {:id "E-beta" :kind :excursion :status-class :active}
   {:id "T-gamma" :kind :ticket :status-class :live}])

(deftest inner-cascade-material-cannot-affect-task-selection
  (let [baseline (sut/select-task {:tasks tasks :seed 20261002})
        poisoned (mapv #(assoc %
                              :constructed-candidates [{:precedence [:wrong/order]}]
                              :interpretations {:secret {:produces #{:x}}}
                              :g -1.0e300
                              :g-terms {:risk -1.0e300})
                       tasks)
        changed (sut/select-task {:tasks poisoned :seed 20261002})]
    (is (= baseline changed))
    (is (not-any? #(some (set (keys %))
                         [:constructed-candidates :interpretations :g :g-terms])
                  (:support changed)))
    (is (= #{:id :kind :status-class :open-hole-count :priority}
           (set (get-in changed [:policy :uses]))))))

(deftest selection-is-recomputable-and-yields-a-task-action
  (let [a (sut/select-task {:tasks tasks :seed 7})
        b (sut/select-task {:tasks (reverse tasks) :seed 7})]
    (is (= a b) "input order does not alter the sorted support or draw")
    (is (= (get-in a [:chosen :id]) (get-in a [:action :target])))
    (is (contains? #{:advance-mission :advance-excursion :advance-ticket}
                   (get-in a [:action :type])))))

(deftest absent-support-and-seed-are-typed
  (testing "no valid M/E/T/A task"
    (is (= {:absent :no-selectable-task}
           (:chosen (sut/select-task {:tasks [{:id "X" :kind :unknown}] :seed 1})))))
  (testing "selection cannot smuggle in a clock-derived default seed"
    (is (= {:absent :no-seed}
           (:chosen (sut/select-task {:tasks tasks}))))))
