(ns ants.aif.hunger-regression-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [ants.aif.affect :as affect]))

(def hunger-goldens
  (-> (io/file "test" "resources" "goldens" "hunger_tau.edn")
      slurp
      edn/read-string))

(defn- evaluate [{:keys [inputs]}]
  (let [{:keys [initial-h tick tick-options update update-options modulation precision-seed precision-options]} inputs
        seed (merge {:Pi-o {} :tau 1.0} precision-seed)
        h1 (affect/tick-hunger initial-h tick tick-options)
        updated (affect/update-hunger h1 update update-options)
        h-final (:h' updated)
        tau (affect/hunger->tau h-final)
        precision (affect/modulate-precisions seed h-final modulation precision-options)]
    {:tick-hunger h1
     :update updated
     :tau tau
     :precision precision}))

(deftest hunger-tau-trajectories-stable
  (doseq [{:keys [results] :as scenario} hunger-goldens
          :let [scenario-name (:name scenario)]]
    (testing (name scenario-name)
      (is (= results (evaluate scenario))))))
