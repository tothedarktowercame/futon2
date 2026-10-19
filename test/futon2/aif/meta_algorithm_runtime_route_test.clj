(ns futon2.aif.meta-algorithm-runtime-route-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.report.war-machine :as wm]))

(def algorithm-selection
  {:outer-task-selection
   {:schema :wm/outer-task-selection-v1
    :chosen {:id "A-self-heal" :kind :algorithm}
    :action {:type :run-algorithm :target "A-self-heal"}}
   :decision
   {:action {:type :run-algorithm :target "A-self-heal"}
    :controller-score -1.0
    :selection-law {:applied :meta-algorithm-selection
                    :source :outer-task-selection
                    :algorithm-id "A-self-heal"}}})

(deftest selected-meta-algorithm-is-refused-by-ordinary-construction
  (let [entry (#'runner/selected-entry algorithm-selection)
        construction (runner/construct-for-decision entry)]
    (is (= {:type :run-algorithm :target "A-self-heal"} (:action entry)))
    (is (= :refused (:status construction)))
    (is (= :meta-algorithm-wrong-route (:reason construction)))
    (is (= :dedicated-meta-self-heal (:required-route construction)))
    (is (= :refused
           (:status (runner/construction-wiring-result
                     {:construction-kind :meta-algorithm-execution
                      :algorithm-contract {:algorithm-id "A-self-heal"}}))))
    (is (nil? (runner/selection-terminal-condition :selection algorithm-selection))
        "a selected algorithm is not a terminal selection abstention")))

(deftest report-projects-algorithm-without-ordinary-cascade-selection
  (let [outer (:outer-task-selection algorithm-selection)
        assembled {:targets [] :refusals []}
        projected (wm/meta-algorithm-result outer assembled)]
    (is (= :meta-algorithm-selection
           (get-in projected [:decision :selection-law :applied])))
    (is (= "A-self-heal"
           (get-in projected [:decision :selection-law :algorithm-id])))
    (is (= (:action outer) (get-in projected [:decision :action])))
    (is (= assembled (:cascade-problems projected)))
    (is (empty? (:lanes projected)))))

(deftest malformed-algorithm-choice-remains-non-addressable
  (testing "identity is required"
    (is (nil? (#'runner/selected-entry
               (assoc-in algorithm-selection [:decision :action :target] nil)))))
  (testing "the explicit algorithm selection law is required"
    (is (nil? (#'runner/selected-entry
               (assoc-in algorithm-selection
                         [:decision :selection-law :applied]
                         :cascade-selection-posterior))))))
