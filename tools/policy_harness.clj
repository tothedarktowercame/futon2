(ns tools.policy-harness
  (:require [ants.aif.policy :as policy]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]))

(def scenarios
  [{:name :forage-normal
    :inputs {:mu {:h 0.38 :cargo 0.0}
             :prec {:tau 1.2}
             :observation {:food 0.65
                           :pher 0.20
                           :food-trace 0.50
                           :pher-trace 0.30
                           :home-prox 0.40
                           :enemy-prox 0.35
                           :h 0.38
                           :hunger 0.38
                           :ingest 0.22
                           :friendly-home 0.0
                           :trail-grad 0.15
                           :novelty 0.70
                           :dist-home 0.40
                           :reserve-home 0.55
                           :cargo 0.0
                           :recent-gather 0.20
                           :mode :outbound}
             :config {:actions [:forage :return :pheromone :hold]}}}
   {:name :loaded-homebound
    :inputs {:mu {:h 0.72 :cargo 0.55}
             :prec {:tau 0.95}
             :observation {:food 0.30
                           :pher 0.35
                           :food-trace 0.20
                           :pher-trace 0.25
                           :home-prox 0.25
                           :enemy-prox 0.50
                           :h 0.72
                           :hunger 0.72
                           :ingest 0.08
                           :friendly-home 0.0
                           :trail-grad 0.18
                           :novelty 0.45
                           :dist-home 0.55
                           :reserve-home 0.40
                           :cargo 0.80
                           :recent-gather 0.05
                           :mode :homebound}
             :config {:actions [:return :forage :hold]}}}
   {:name :white-space-scout
    :inputs {:mu {:h 0.48 :cargo 0.05}
             :prec {:tau 1.1}
             :observation {:food 0.02
                           :pher 0.01
                           :food-trace 0.05
                           :pher-trace 0.04
                           :home-prox 0.20
                           :enemy-prox 0.30
                           :h 0.48
                           :hunger 0.48
                           :ingest 0.12
                           :friendly-home 0.0
                           :trail-grad 0.05
                           :novelty 0.90
                           :dist-home 0.60
                           :reserve-home 0.65
                           :cargo 0.05
                           :recent-gather 0.0
                           :mode :outbound}
             :config {:actions [:pheromone :forage :return]}}}
   {:name :deficit-colony
    :inputs {:mu {:h 0.85 :cargo 0.30}
             :prec {:tau 0.8}
             :observation {:food 0.25
                           :pher 0.15
                           :food-trace 0.20
                           :pher-trace 0.18
                           :home-prox 0.35
                           :enemy-prox 0.45
                           :h 0.85
                           :hunger 0.85
                           :ingest 0.05
                           :friendly-home 0.0
                           :trail-grad 0.22
                           :novelty 0.30
                           :dist-home 0.45
                           :reserve-home 0.05
                           :cargo 0.30
                           :recent-gather 0.10
                           :mode :homebound}
             :config {:actions [:return :hold :forage :pheromone]}}}
   {:name :maintain-pheromone
    :inputs {:mu {:h 0.42 :cargo 0.10}
             :prec {:tau 1.4}
             :observation {:food 0.35
                           :pher 0.60
                           :food-trace 0.45
                           :pher-trace 0.55
                           :home-prox 0.70
                           :enemy-prox 0.30
                           :h 0.42
                           :hunger 0.42
                           :ingest 0.18
                           :friendly-home 0.2
                           :trail-grad 0.48
                           :novelty 0.25
                           :dist-home 0.15
                           :reserve-home 0.80
                           :cargo 0.10
                           :recent-gather 0.40
                           :mode :maintain}
             :config {:actions [:pheromone :hold :forage]}}}])

(defn- evaluate [{:keys [inputs] :as scenario}]
  (let [{:keys [mu prec observation config]} inputs]
    (assoc scenario :results (policy/eval-policy mu prec observation config))))

(defn- write-goldens! [scenarios]
  (let [evaluated (map evaluate scenarios)
        target (io/file "test" "resources" "goldens" "policy_harness.edn")]
    (.mkdirs (.getParentFile target))
    (with-open [w (io/writer target)]
      (binding [*print-namespace-maps* false]
        (pprint/pprint evaluated w)))))

(defn -main [& _]
  (write-goldens! scenarios)
  (println "wrote policy harness goldens to test/resources/goldens/policy_harness.edn"))
