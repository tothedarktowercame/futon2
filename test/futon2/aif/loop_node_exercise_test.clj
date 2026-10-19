(ns futon2.aif.loop-node-exercise-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.wm.loop-node-exercise :as exercise]))

(def exercised
  {:abstention {:status :not-abstained}
   :selection-law {:policy-comparison {:status :compared}}
   :selection-certificate
   {:candidates [{:policy {:target "M-x" :precedence [:p1 :p2]
                           :enacted-steps {:p1 :p1 :p2 :p2}}
                  :class-emission {:status :computed :consumed-in-g? true :risk 1.0
                                   :model {:kind :class-emission :horizon 4
                                           :class-preference {4 {:focused 1}}}}}
                 {:policy {:target "M-y" :precedence [:p3 :p4]
                           :enacted-steps {:p3 :p3 :p4 :p4}}
                  :class-emission {:status :computed :consumed-in-g? true :risk 1.0
                                   :model {:kind :class-emission :horizon 4
                                           :class-preference {4 {:focused 1}}}}}]
    :policies [{:id :C1} {:id :C2}]
    :policy-precision-state {:temperature 0.75
                             :probabilities {:C1 0.6 :C2 0.4}}}})

(deftest distinguishes-exercised-nodes-from-declared-machinery
  (let [r (exercise/receipt exercised)]
    (is (= :complete (:status r)))
    (is (= {:present 4 :bypassed 0 :refused 0} (:counts r)))
    (is (every? #(= :present (:status %)) (vals (:by-node r))))))

(deftest live-selected-shape-continues-with-four-absences
  ;; Verbatim selected target/policy from f9abc990's debugger stop.
  (let [decision {:status :selected
                  :action {:kind :cascade-candidate :target "M-custom-harness"
                           :id "M-custom-harness/policy-e22941ff9f6a8f30c06a"}}
        measured (exercise/require-complete! decision)
        r (get-in measured [:selection-certificate :loop-node-exercise])]
    (is (= (:action decision) (:action measured)))
    (is (= :selected (:status measured)))
    (is (= {:present 0 :bypassed 0 :refused 4} (:counts r)))
    (is (= {:R6 :candidate-space-not-retained
            :R13 :policy-rollout-evidence-not-retained
            :R14 :selection-posterior-not-retained
            :CTAU-CLASS :class-preference-evidence-not-retained}
           (update-vals (:by-node r) :reason)))
    (is (= :incomplete (:status r)))))

(deftest complete-exercise-crosses-the-launch-gate
  (let [measured (exercise/require-complete! exercised)]
    (is (= (exercise/receipt exercised)
           (get-in measured [:selection-certificate :loop-node-exercise])))
    (is (= (:selection-law exercised) (:selection-law measured)))))

(deftest meta-repair-selection-records-ordinary-node-absence
  (let [decision {:status :selected :action {:target "A-self-heal"}
                  :selection-law {:applied :meta-algorithm-selection
                                  :source :outer-task-selection
                                  :algorithm-id "A-self-heal"}}
        r (get-in (exercise/require-complete! decision)
                  [:selection-certificate :loop-node-exercise])]
    (is (= :incomplete (:status r)))
    (is (= {:present 0 :bypassed 0 :refused 4} (:counts r)))
    (is (every? #(= :meta-algorithm-wrong-route (:reason %)) (vals (:by-node r))))))

(deftest abstention-rationale-is-measured-not-invented
  (doseq [refusals [[{:kind :no-problems :target "E-x"}] []]]
    (let [decision {:status :abstained :refusals refusals}
          r (get-in (exercise/require-complete! decision)
                    [:selection-certificate :loop-node-exercise])]
      (is (= (if (seq refusals) :complete :incomplete) (:status r)))
      (is (= 4 (get-in r [:counts (if (seq refusals) :bypassed :refused)]))))))
