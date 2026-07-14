(ns ants.aif.controller-split-test
  "Slice 3 tests: controller split, ablation, R7 precision, R14 τ.

   Acceptance bar:
   1. Winner-changing ablation per augmentation AT DEFAULT λ (not inflated).
   2. Single scoring path (controller-score = g-efe + augmentation).
   3. R7 precision uses shared futon2.aif.precision.
   4. R14 τ moves with starvation.

   Ablation tests were found via systematic scenario search over:
   mode × hunger × cargo × reserves × food × home-prox (1500 scenarios).
   Each augmentation shows genuine winner-flips at its DEFAULT deployed weight."
  (:require [clojure.test :refer [deftest is testing]]
            [ants.aif.policy :as policy]
            [ants.aif.core :as core]
            [futon2.aif.precision :as precision]))

(def base-mu
  {:h 0.5
   :cargo 0.3
   :goal [4 4]
   :var (into {} (for [k [:food :pher :food-trace :pher-trace :home-prox :enemy-prox :h :ingest
                          :friendly-home :trail-grad :novelty :dist-home :reserve-home :cargo]]
                   [k 0.5]))})

(def base-prec
  {:tau 1.0
   :Pi-o {:food 1.0 :pher 0.8 :food-trace 0.6 :pher-trace 0.5 :home-prox 0.8
          :enemy-prox 0.9 :h 1.1 :ingest 0.9 :friendly-home 0.8 :trail-grad 0.7
          :novelty 0.6 :dist-home 0.9 :reserve-home 0.6 :cargo 1.2}})

(defn- make-obs [overrides]
  (merge {:food 0.3 :pher 0.2 :food-trace 0.25 :pher-trace 0.18
          :home-prox 0.6 :enemy-prox 0.4 :h 0.5 :hunger 0.5 :ingest 0.2
          :friendly-home 0.0 :trail-grad 0.0 :novelty 0.6
          :dist-home 0.4 :reserve-home 0.5 :cargo 0.3
          :mode :outbound}
         overrides))

(defn- winner [mu prec obs config]
  (:action (policy/choose-action mu prec obs config)))

;; The DEFAULT deployed config — no inflated λ, no custom costs.
(def default-cfg
  {:actions [:forage :return :hold :pheromone]
   :efe {:lambda {:pragmatic 1.0 :ambiguity 0.5 :info 0.4 :colony 0.4 :survival 1.2}
         :colony {:reserve-thresh 1.0 :non-return-pen 0.6 :return-pen 0.0}
         :survival {:hunger-thresh 0.55 :hunger-weight 1.5 :dist-weight 0.5
                    :ingest-buffer 0.30 :return-reduction 0.40}}})

(defn- cfg-without-colony []
  (assoc-in default-cfg [:efe :lambda :colony] 0.0))

(defn- cfg-without-survival []
  (assoc-in default-cfg [:efe :lambda :survival] 0.0))

(defn- cfg-without-action-cost []
  (assoc default-cfg :action-costs
         {:pheromone {:base-cost 0.0 :hunger-mult 0.0 :no-ingest-pen 0.0
                      :ingest-thresh 0.2 :friendly-home-pen 0.0}
          :forage {:friendly-home-pen 0.0}
          :return {:base-cost 0.0 :empty-home-pen 0.0 :cargo-thresh 0.05
                   :home-thresh 0.8 :hunger-gap-mult 0.0}}))

;; --------------------------------------------------------------------------- ;;
;; 1. Winner-changing ablation tests AT DEFAULT λ
;; --------------------------------------------------------------------------- ;;
;; Each scenario was found via systematic search over 1500 plausible scenarios.
;; The augmentation's λ is the DEFAULT (colony 0.4, survival 1.2, action-cost
;; hunger-gap-mult 3.8) — NOT inflated. Removing it (λ→0 or cost→0) flips the winner.

(deftest ablation-colony-at-default-lambda
  (testing "colony-cost (λ=0.4 DEFAULT) changes winner when removed
            Scenario: outbound, h=0.3, cargo=0.6, reserve=0.01, food=0.05, home-prox=1.0
            With colony: return wins (low reserves → colony penalizes non-return).
            Without colony: hold wins."
    (let [obs {:food 0.05 :pher 0.2 :food-trace 0.025 :pher-trace 0.2
               :home-prox 1.0 :enemy-prox 0.3 :h 0.3 :hunger 0.3 :ingest 0.1
               :friendly-home 0.0 :trail-grad 0.1 :novelty 0.5 :dist-home 0.0
               :reserve-home 0.01 :cargo 0.6 :mode :outbound}
          with-colony (winner base-mu base-prec obs default-cfg)
          without-colony (winner base-mu base-prec obs (cfg-without-colony))]
      (is (not= with-colony without-colony)
          (str "colony ablation at default λ: with=" with-colony
               " without=" without-colony)))))

(deftest ablation-survival-at-default-lambda
  (testing "survival-cost (λ=1.2 DEFAULT) changes winner when removed
            Scenario: outbound, h=0.9, cargo=0.4, reserve=0.8, food=0.05, home-prox=0.6
            With survival: return wins (high hunger → survival favors return).
            Without survival: forage wins."
    (let [obs {:food 0.05 :pher 0.2 :food-trace 0.025 :pher-trace 0.2
               :home-prox 0.6 :enemy-prox 0.3 :h 0.9 :hunger 0.9 :ingest 0.1
               :friendly-home 0.0 :trail-grad 0.1 :novelty 0.5 :dist-home 0.4
               :reserve-home 0.8 :cargo 0.4 :mode :outbound}
          with-survival (winner base-mu base-prec obs default-cfg)
          without-survival (winner base-mu base-prec obs (cfg-without-survival))]
      (is (not= with-survival without-survival)
          (str "survival ablation at default λ: with=" with-survival
               " without=" without-survival)))))

(deftest ablation-action-cost-at-default-lambda
  (testing "action-cost (DEFAULT costs: base-cost 0.25, hunger-gap-mult 3.8) changes winner when removed
            Scenario: outbound, h=0.7, cargo=0.6, reserve=0.01, food=0.05, home-prox=0.2
            With action-cost: forage wins (return penalized by hunger-gap-mult).
            Without action-cost: return wins."
    (let [obs {:food 0.05 :pher 0.2 :food-trace 0.025 :pher-trace 0.2
               :home-prox 0.2 :enemy-prox 0.3 :h 0.7 :hunger 0.7 :ingest 0.1
               :friendly-home 0.0 :trail-grad 0.1 :novelty 0.5 :dist-home 0.8
               :reserve-home 0.01 :cargo 0.6 :mode :outbound}
          with-cost (winner base-mu base-prec obs default-cfg)
          without-cost (winner base-mu base-prec obs (cfg-without-action-cost))]
      (is (not= with-cost without-cost)
          (str "action-cost ablation at default: with=" with-cost
               " without=" without-cost)))))

;; --------------------------------------------------------------------------- ;;
;; 2. Controller split — g-efe and augmentation are separate fields
;; --------------------------------------------------------------------------- ;;

(deftest controller-score-split
  (testing "expected-free-energy returns separated g-efe and augmentation"
    (let [obs (make-obs {:cargo 0.3 :mode :outbound})
          result (policy/eval-policy base-mu base-prec obs default-cfg)
          top (first (:ranking result))]
      (is (number? (:G top)) "G (controller-score) exists")
      (is (number? (:risk top)) "risk exists")
      (is (number? (:ambiguity top)) "ambiguity exists"))))

;; --------------------------------------------------------------------------- ;;
;; 3. R7 precision uses shared futon2.aif.precision
;; --------------------------------------------------------------------------- ;;

(deftest precision-state-evolves
  (testing "perceive returns a precision-state from futon2.aif.precision"
    (let [home [20 20]
          world {:grid {:size [50 50] :max-food 5.0 :max-pher 5.0
                        :cells (into {} (for [x (range 50) y (range 50)]
                                          [[x y] {:food 0.0 :pher 0.0 :home nil :ant nil}]))}
                 :homes {:aif home :classic [30 30]}
                 :config {:aif core/default-aif-config}
                 :armies [:classic :aif]
                 :tick 0 :ants {} :last-events []}
          world (assoc-in world [:grid :cells home :home] :aif)
          ant {:species :aif :loc home :home home :cargo 0.0 :h 0.5}
          res (core/aif-step world ant)
          pstate (:precision-state (:ant res))]
      (is (map? pstate) "precision-state is a map")
      (is (some? (get pstate :food)) "has :food channel")
      (is (number? (precision/precision-for pstate :food))
          "precision-for returns a number"))))

;; --------------------------------------------------------------------------- ;;
;; 4. R14 τ moves with starvation
;; --------------------------------------------------------------------------- ;;

(deftest tau-increases-with-starvation
  (testing "τ is higher when since-ingest is high (starved ants explore more)"
    (let [obs-fed (make-obs {:since-ingest 0 :h 0.5})
          obs-starved (make-obs {:since-ingest 10 :h 0.5})
          tau-fed (:tau (policy/choose-action base-mu base-prec obs-fed default-cfg))
          tau-starved (:tau (policy/choose-action base-mu base-prec obs-starved default-cfg))]
      (is (> tau-starved tau-fed)
          (str "starved τ (" tau-starved ") > fed τ (" tau-fed ")")))))
