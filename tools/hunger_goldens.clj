(ns tools.hunger-goldens
  (:require [ants.aif.affect :as affect]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]))

(def scenarios
  [{:name :normal
    :inputs {:initial-h 0.42
             :tick {:food 0.55 :home-prox 0.35 :cargo 0.10}
             :update {:ingest 0.32 :deposit 0.05 :risk 0.01}
             :modulation {:food 0.55 :pher 0.25 :food-trace 0.40 :pher-trace 0.20
                          :home-prox 0.35 :enemy-prox 0.25}
             :precision-seed {:Pi-o {} :tau 1.0}}}
   {:name :starvation
    :inputs {:initial-h 0.88
             :tick {:food 0.05 :home-prox 0.05 :cargo 0.0}
             :update {:ingest 0.02 :deposit 0.0 :risk 0.12}
             :modulation {:food 0.05 :pher 0.05 :food-trace 0.10 :pher-trace 0.05
                          :home-prox 0.05 :enemy-prox 0.90}
             :precision-seed {:Pi-o {} :tau 0.8}}}
   {:name :overstimulation
    :inputs {:initial-h 0.50
             :tick {:food 0.90 :home-prox 0.20 :cargo 0.15}
             :update {:ingest 0.40 :deposit 0.10 :risk 0.0}
             :modulation {:food 0.90 :pher 0.85 :food-trace 0.70 :pher-trace 0.80
                          :home-prox 0.20 :enemy-prox 0.40}
             :precision-seed {:Pi-o {:food 1.2 :pher 1.0} :tau 1.2}}}
   {:name :heavy-cargo
    :inputs {:initial-h 0.58
             :tick {:food 0.30 :home-prox 0.30 :cargo 0.95}
             :tick-options {:load-pressure 0.40 :burn 0.025}
             :update {:ingest 0.12 :deposit 0.0 :risk 0.05}
             :modulation {:food 0.30 :pher 0.40 :food-trace 0.45 :pher-trace 0.50
                          :home-prox 0.30 :enemy-prox 0.30}
             :precision-seed {:Pi-o {:food 1.1} :tau 0.9}}}
   {:name :high-risk
    :inputs {:initial-h 0.60
             :tick {:food 0.40 :home-prox 0.15 :cargo 0.30}
             :update {:ingest 0.05 :deposit 0.0 :risk 0.40}
             :modulation {:food 0.40 :pher 0.10 :food-trace 0.25 :pher-trace 0.20
                          :home-prox 0.15 :enemy-prox 0.85}
             :precision-seed {:Pi-o {:pher 0.9 :enemy-prox 1.2} :tau 1.1}}}
   {:name :high-ingest
    :inputs {:initial-h 0.70
             :tick {:food 0.80 :home-prox 0.60 :cargo 0.05}
             :update {:ingest 0.80 :deposit 0.20 :risk 0.0}
             :modulation {:food 0.80 :pher 0.30 :food-trace 0.50 :pher-trace 0.35
                          :home-prox 0.60 :enemy-prox 0.20}
             :precision-seed {:Pi-o {:food 1.0 :home-prox 0.9} :tau 1.3}}}])

(defn- evaluate-scenario [{:keys [inputs] :as scenario}]
  (let [{:keys [initial-h tick tick-options update update-options modulation precision-seed precision-options]} inputs
        seed (merge {:Pi-o {} :tau 1.0} precision-seed)
        h-tick (affect/tick-hunger initial-h tick tick-options)
        update-result (affect/update-hunger h-tick update update-options)
        h-final (:h' update-result)
        tau-direct (affect/hunger->tau h-final)
        precision (affect/modulate-precisions seed h-final modulation precision-options)]
    (assoc scenario :results {:tick-hunger h-tick
                              :update update-result
                              :tau tau-direct
                              :precision precision})))

(defn- write-goldens! [scenarios]
  (let [evaluated (map evaluate-scenario scenarios)
        target (io/file "test" "resources" "goldens" "hunger_tau.edn")]
    (.mkdirs (.getParentFile target))
    (with-open [w (io/writer target)]
      (binding [*print-namespace-maps* false]
        (pprint/pprint evaluated w)))))

(defn -main [& _]
  (write-goldens! scenarios)
  (println "wrote hunger/tau goldens to test/resources/goldens/hunger_tau.edn"))
