(ns ants.aif.policy-test
  (:require [clojure.test :refer [deftest is testing]]
            [ants.aif.policy :as policy]))

(def mu
  {:h 0.4
   :cargo 0.3
   :goal [4 4]})

(def prec
  {:tau 1.2
   :Pi-o {:food 1.1
          :pher 0.9
          :home-prox 0.8
          :enemy-prox 0.7
          :h 1.5}})

(def observation
  {:food 0.3
   :pher 0.2
   :food-trace 0.25
   :pher-trace 0.18
   :home-prox 0.6
   :enemy-prox 0.4
   :h 0.5
   :hunger 0.5
   :ingest 0.2
   :friendly-home 0.0
   :trail-grad 0.0
   :novelty 0.6
   :dist-home 0.4
   :reserve-home 0.5
   :cargo 0.3})

(deftest expected-free-energy-shapes
  (let [{:keys [action policies tau]} (policy/choose-action mu prec observation)]
    (testing "returns default action from domain"
      (is (keyword? action)))
    (testing "policies include G and probability"
      (doseq [[_ {:keys [G p]}] policies]
        (is (number? G))
        (is (<= 0.0 p 1.0))))
    (testing "policies expose diagnostics"
      (doseq [[_ {:keys [risk ambiguity action-cost]}] policies]
        (is (number? risk))
        (is (number? ambiguity))
        (is (number? action-cost))))
    (testing "probabilities roughly sum to 1"
      (is (<= 0.99 (reduce + (map :p (vals policies))) 1.01)))
    (testing "surface tau"
      (is (number? tau))
      (is (> tau 0.0)))
    (let [home-obs (assoc observation
                           :home-prox 1.0
                           :friendly-home 1.0
                           :ingest 0.1
                           :food 0.8
                           :dist-home 0.0
                           :trail-grad 0.3
                           :reserve-home 0.4)
          home-policy (policy/choose-action mu prec home-obs)
          home-policies (:policies home-policy)]
      (testing "forage blocked when perched on friendly home"
        (is (not (contains? home-policies :forage))))
      (testing "pheromone blocked when perched on friendly home"
        (is (not (contains? home-policies :pheromone))))
      (testing "return option remains available on nest"
        (is (contains? home-policies :return))
        (is (= :return (:action home-policy)))))))

(deftest custom-actions-softmax
  (let [actions [:hold :return]
        {:keys [action policies]} (policy/choose-action mu prec observation {:actions actions})]
    (testing "respects provided action set"
      (is (contains? (set actions) action)))
    (testing "policy keys match"
      (is (= (set actions) (set (keys policies)))))))

(deftest preference-risk-favors-return-when-hungry
  (let [hungry-mu (assoc mu :h 0.9)
        hungry-obs (assoc observation :h 0.9 :hunger 0.9 :ingest 0.0)
        {:keys [policies]} (policy/choose-action hungry-mu prec hungry-obs)]
    (testing "return beats pheromone under high hunger"
      (is (< (:G (policies :return))
             (:G (policies :pheromone)))))))

(deftest pheromone-penalized-when-ingest-absent
  (let [hungry-mu (assoc mu :h 0.85)
        hungry-obs (assoc observation :h 0.85 :hunger 0.85 :ingest 0.05)
        {:keys [policies]} (policy/choose-action hungry-mu prec hungry-obs)]
    (testing "pheromone G is worse than forage and return"
      (is (> (:G (policies :pheromone))
             (:G (policies :forage))))
      (is (> (:G (policies :pheromone))
             (:G (policies :return)))))))

(deftest forage-on-home-penalized
  (let [home-obs (assoc observation
                         :home-prox 1.0
                         :friendly-home 1.0
                         :ingest 0.05
                         :food 0.9
                         :dist-home 0.0
                         :trail-grad 0.25)
        {:keys [action policies]} (policy/choose-action mu prec home-obs)]
    (testing "forage action suppressed on nest tiles"
      (is (not (contains? policies :forage))))
    (testing "pheromone suppressed on nest tiles"
      (is (not (contains? policies :pheromone))))
    (testing "return is chosen when guarding the nest"
      (is (= :return action))
      (is (contains? policies :return)))))

(deftest info-gain-prefers-exploration
  (let [obs (assoc observation :novelty 0.8 :trail-grad 0.1)
        mu' {:h 0.4}
        prec' {:tau 1.0}
        efe {:lambda {:pragmatic 0.0 :ambiguity 0.0 :info 2.0 :colony 0.0 :survival 0.0}}]
    (with-redefs [policy/predict-outcome (fn [_ o action]
                                          (case action
                                            :explore (assoc o :novelty 0.2 :trail-grad 0.3)
                                            :wait o
                                            o))]
      (let [{:keys [action]} (policy/choose-action mu' prec' obs {:actions [:explore :wait]
                                                                  :efe efe})]
        (is (= :explore action))))))

(deftest low-reserves-favour-return
  (let [obs (assoc observation :reserve-home 0.1)
        mu' {:h 0.4}
        prec' {:tau 1.0}
        efe {:lambda {:pragmatic 0.0 :ambiguity 0.0 :info 0.0 :colony 2.0 :survival 0.0}}
        choose #(policy/choose-action mu' prec' obs %)]
    (with-redefs [policy/predict-outcome (fn [_ o _] o)]
      (let [{:keys [action]} (choose {:actions [:return :scout]
                                      :efe efe})]
        (is (= :return action))))))

(deftest survival-cost-pushes-return
  (let [obs (assoc observation :hunger 0.85 :dist-home 0.6 :ingest 0.05)
        mu' {:h 0.85}
        prec' {:tau 1.0}
        efe {:lambda {:pragmatic 0.0 :ambiguity 0.0 :info 0.0 :colony 0.0 :survival 2.0}}
        outcome-return (assoc obs :dist-home 0.1 :ingest 0.4)
        outcome-wander obs]
    (with-redefs [policy/predict-outcome (fn [_ _ action]
                                          (case action
                                            :return outcome-return
                                            :wander outcome-wander
                                            outcome-wander))]
      (let [{:keys [action]} (policy/choose-action mu' prec' obs {:actions [:return :wander]
                                                                  :efe efe})]
        (is (= :return action))))))

(deftest tau-couples-to-colony-and-survival
  (let [mu0 (assoc mu :h 0.5)
        base-prec {:tau 1.0}
        opts {:actions [:forage :return]}
        base-obs (-> observation
                     (assoc :reserve-home 0.35
                            :h 0.5
                            :hunger 0.5
                            :dist-home 0.3
                            :ingest 0.3))
        surplus-obs (assoc base-obs
                            :reserve-home 0.85
                            :h 0.25
                            :hunger 0.25
                            :dist-home 0.1
                            :ingest 0.7)
        deficit-obs (assoc base-obs :reserve-home 0.05)
        risky-obs (assoc deficit-obs
                          :h 0.9
                          :hunger 0.9
                          :dist-home 0.85
                          :ingest 0.0)
        base-tau (:tau (policy/choose-action mu0 base-prec base-obs opts))
        surplus-tau (:tau (policy/choose-action mu0 base-prec surplus-obs opts))
        deficit-tau (:tau (policy/choose-action mu0 base-prec deficit-obs opts))
        risky-tau (:tau (policy/choose-action mu0 base-prec risky-obs opts))]
    (is (> surplus-tau base-tau))
    (is (< deficit-tau base-tau))
    (is (<= risky-tau deficit-tau))
    (is (< risky-tau surplus-tau))
    (is (> surplus-tau 0.9))
    (is (<= risky-tau 0.8))
    (is (pos? base-tau))))
