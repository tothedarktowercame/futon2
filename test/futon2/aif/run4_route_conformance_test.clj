(ns futon2.aif.run4-route-conformance-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.run4-route-conformance :as sut]))

(def control {:edges [{:from :R20 :to :R12}]
              :route-measured-drawn [] :decisions {}})
(def record {:route [{:fromNode "R20" :toNode "R12" :via "scan" :at_ "now"}]})

(deftest exact-u49-classes-and-carrier-controls
  (is (true? (:conforms? (sut/verdict control record))))
  (is (= :drawn (get-in (sut/verdict control record) [:hops 0 :class])))
  (is (= :unmapped (get-in (sut/verdict control (assoc record :route
                                                        [{:fromNode "R1" :toNode "R99"
                                                          :via "x" :at_ "now"}]))
                         [:hops 0 :class])))
  (is (= :empty-route (:reason (try (sut/verdict control {:route []}) nil
                                    (catch clojure.lang.ExceptionInfo e (ex-data e))))))
  (is (= :discontinuous-route
         (:reason (try (sut/verdict control
                                   {:route [{:fromNode "R20" :toNode "R12" :via "a"}
                                            {:fromNode "R7" :toNode "R8" :via "b"}]}) nil
                       (catch clojure.lang.ExceptionInfo e (ex-data e)))))))
