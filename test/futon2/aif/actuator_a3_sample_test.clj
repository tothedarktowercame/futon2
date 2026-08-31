(ns futon2.aif.actuator-a3-sample-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.actuator-a3 :as a3]))

(def expected-sampled-missions
  #{"futon5a-d/mission/learning-loop"
    "futon3c-d/mission/autoclock-in"
    "futon3c-d/mission/state-snapshot-witness"
    "futon3c-d/mission/single-entry-point"})

(deftest sampled-domain-is-explicit
  (testing "the A3 check names its four-member sample rather than implying a census"
    (is (= expected-sampled-missions (set (keys a3/sampled-candidate-cleans))))
    (is (= 4 (count a3/sampled-candidate-cleans)))))

(deftest legacy-complete-sounding-var-is-absent
  (testing "the old name cannot silently regain complete-population semantics"
    (is (nil? (ns-resolve 'futon2.aif.actuator-a3 'reviewed-candidate-cleans)))))
