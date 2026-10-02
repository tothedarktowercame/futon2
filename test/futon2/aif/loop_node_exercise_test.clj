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
    (is (= {:present 4 :refused 0} (:counts r)))
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
               (set (map :node (:refused (ex-data e))))))))))

(deftest complete-exercise-crosses-the-launch-gate
  (is (identical? exercised (exercise/require-complete! exercised))))
