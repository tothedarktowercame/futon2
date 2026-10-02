(ns futon2.aif.loop-node-exercise-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.wm.loop-node-exercise :as exercise]))

(def exercised
  {:abstention {:status :not-abstained}
   :selection-law {:policy-comparison {:status :compared}}
   :selection-certificate
   {:candidates [{:policy {:target "M-x" :precedence [:p1 :p2]
                           :enacted-steps {:p1 :p1 :p2 :p2}}
                  :computed-f {:model {:kind :class-emission :horizon 4}}}
                 {:policy {:target "M-y" :precedence [:p3 :p4]
                           :enacted-steps {:p3 :p3 :p4 :p4}}
                  :computed-f {:model {:kind :class-emission :horizon 4}}}]
    :policies [{:id :C1} {:id :C2}]
    :policy-precision-state {:temperature 0.75
                             :probabilities {:C1 0.6 :C2 0.4}}}})

(deftest distinguishes-exercised-nodes-from-declared-machinery
  (let [r (exercise/receipt exercised)]
    (is (= :complete (:status r)))
    (is (= {:present 4 :bypassed 0 :refused 0} (:counts r)))
    (is (every? #(= :present (:status %)) (vals (:by-node r))))))

(deftest singleton-click-does-not-claim-temperature-or-temporal-depth
  (let [decision {:abstention {:status :not-abstained}
                  :selection-certificate
                  {:candidates [{:policy {:target "M-x" :precedence [:p]}
                                 :computed-f {:model {:kind :class-emission :horizon 4}}}]
                   :policies [{:id :C1}]}}
        r (exercise/receipt decision)]
    (is (= :refused (get-in r [:by-node :R6 :status])))
    (is (= :refused (get-in r [:by-node :CTAU-CLASS :status])))
    (is (= :refused (get-in r [:by-node :R13 :status])))
    (is (= :refused (get-in r [:by-node :R14 :status])))
    (try
      (exercise/require-complete! decision)
      (is false "singleton selection must stop before dispatch")
      (catch clojure.lang.ExceptionInfo e
        (is (= :required-loop-node-unexercised
               (:failure-kind (ex-data e))))
        (is (= #{:R6 :R13 :R14 :CTAU-CLASS}
               (set (map :node (:refused (ex-data e))))))
        (is (= 1 (get-in (ex-data e) [:decision-context :candidate-count])))
        (is (= "M-x"
               (get-in (ex-data e)
                       [:decision-context :candidates 0 :policy :target])))))))

(deftest complete-exercise-crosses-the-launch-gate
  (is (identical? exercised (exercise/require-complete! exercised))))

(deftest debugger-context-retains-abstention-rationale
  (let [decision {:status :abstained
                  :refusals [{:kind :no-problems :target "E-x"}]}]
    (is (identical? decision (exercise/require-complete! decision)))
    (let [r (exercise/receipt decision)]
      (is (= :complete (:status r)))
      (is (= 4 (get-in r [:counts :bypassed])))
      (is (every? #(= :typed-selection-refusal (:because %))
                  (vals (:by-node r)))))))

(deftest rationale-free-abstention-still-stops
  (try
    (exercise/require-complete! {:status :abstained :refusals []})
    (is false "an unexplained abstention must stop")
    (catch clojure.lang.ExceptionInfo e
      (is (= :required-loop-node-unexercised
             (:failure-kind (ex-data e)))))))
