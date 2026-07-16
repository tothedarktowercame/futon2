(ns ants.aif.pattern-authority-gate-test
  (:require [ants.aif.experiment :as experiment]
            [ants.aif.pattern-efe :as pattern-efe]
            [clojure.test :refer [deftest is testing]]
            [pattern-authority-gate :as gate]))

(deftest activation-sets-only-the-live-pattern-switches
  (let [world (experiment/make-seeded-world :aif :patchy 1 10001 [30 30] 10)
        active (gate/activate-pattern world :cyber/cargo-return 0.5)
        off (gate/activate-pattern active nil 0.0)]
    (is (= 0.5 (get-in active [:config :aif :efe :lambda :pattern])))
    (is (every? #(= {:id :cyber/cargo-return :ticks-active 0}
                    (:cyber-pattern %))
                (vals (:ants active))))
    (is (= 0.0 (get-in off [:config :aif :efe :lambda :pattern])))
    (is (every? #(not (contains? % :cyber-pattern)) (vals (:ants off))))))

(deftest baseline-and-zero-lambda-are-provably-zero-controls
  (doseq [lambda gate/default-lambdas
          action [:forage :return :pheromone :hold]]
    (is (= {:G 0.0 :pattern-risk 0.0 :pattern-info 0.0}
           (pattern-efe/pattern-efe :cyber/baseline action
                                    {:cargo 0.8 :h 0.9 :home-prox 0.1
                                     :white? 1.0 :novelty 1.0}
                                    {:efe {:lambda {:pattern lambda}}}))))
  (doseq [pattern gate/all-patterns
          action [:forage :return :pheromone :hold]]
    (is (= {:G 0.0 :pattern-risk 0.0 :pattern-info 0.0}
           (pattern-efe/pattern-efe pattern action
                                    {:cargo 0.8 :h 0.9 :home-prox 0.1
                                     :white? 1.0 :novelty 1.0}
                                    {:efe {:lambda {:pattern 0.0}}})))))

(deftest every-real-pattern-has-a-nonzero-triggering-contribution
  (testing "risk plus info patterns"
    (is (not (zero? (:G (pattern-efe/pattern-efe
                         :cyber/cargo-return :return
                         {:cargo 0.0 :mode :outbound :home-prox 0.0}
                         {:efe {:lambda {:pattern 1.0}}})))))
    (is (not (zero? (:G (pattern-efe/pattern-efe
                         :cyber/white-space :forage
                         {:food 0.0 :white? 1.0 :novelty 0.8}
                         {:efe {:lambda {:pattern 1.0}}}))))))
  (testing "risk-only patterns"
    (is (not (zero? (:G (pattern-efe/pattern-efe
                         :cyber/hunger-coupling :forage
                         {:h 0.9 :cargo 0.2 :home-prox 0.1}
                         {:efe {:lambda {:pattern 1.0}}})))))
    (is (not (zero? (:G (pattern-efe/pattern-efe
                         :cyber/pheromone-tuner :pheromone
                         {:home-prox 1.0 :novelty 0.0 :reserve-home 1.0}
                         {:efe {:lambda {:pattern 1.0}}})))))))

(deftest paired-summary-is-two-sided-and-tie-aware
  (let [off-base (mapv (fn [seed] {:seed seed :yield 10.0 :actions {} :thrash 0})
                       (range 1 7))
        worse (mapv (fn [seed] {:seed seed :yield 9.0 :actions {} :thrash 0
                                :trace [{:action :return}]})
                    (range 1 7))
        off (mapv #(assoc %
                          :trace [{:action :forage}]
                          :behavior-trace [{:action :forage}])
                  off-base)
        worse (mapv #(assoc % :behavior-trace [{:action :return}]) worse)
        tied (assoc worse 5 {:seed 6 :yield 10.0 :actions {} :thrash 0
                             :trace [{:action :return}]
                             :behavior-trace [{:action :return}]})
        all-worse (gate/summarize "fixture" 1.0 worse off)
        with-tie (gate/summarize "fixture" 1.0 tied off)]
    (is (= 0 (:wins all-worse)))
    (is (= 6 (:losses all-worse)))
    (is (= 0.03125 (:sign-p all-worse)))
    (is (:live-actuator? all-worse) "reliably worse still has authority")
    (is (= 6 (:behavior-seeds all-worse)))
    (is (= 1 (:ties with-tie)))
    (is (= 5 (:informative with-tie)))))
