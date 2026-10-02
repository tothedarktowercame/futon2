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
  (let [r (exercise/receipt
           {:abstention {:status :not-abstained}
            :selection-certificate
            {:candidates [{:policy {:target "M-x" :precedence [:p]}
                           :computed-f {:model {:kind :class-emission :horizon 4}}}]
             :policies [{:id :C1}]}})]
    (is (= :refused (get-in r [:by-node :R6 :status])))
    (is (= :refused (get-in r [:by-node :CTAU-CLASS :status])))
    (is (= :refused (get-in r [:by-node :R13 :status])))
    (is (= :refused (get-in r [:by-node :R14 :status])))))
