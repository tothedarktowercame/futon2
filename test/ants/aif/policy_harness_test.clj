(ns ants.aif.policy-harness-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [ants.aif.policy :as policy]))

(def policy-goldens
  (-> (io/file "test" "resources" "goldens" "policy_harness.edn")
      slurp
      edn/read-string))

(defn- run-scenario [{:keys [inputs]}]
  (let [{:keys [mu prec observation config]} inputs]
    (policy/eval-policy mu prec observation config)))

(deftest policy-rankings-locked
  (doseq [{:keys [results] :as scenario} policy-goldens
          :let [scenario-name (:name scenario)]]
    (testing (name scenario-name)
      (is (= results (run-scenario scenario))))))
