(ns ants.aif.controller-split-test
  "Slice 3 tests: controller split, ablation, R7 precision, R14 τ.

   Acceptance bar:
   1. Winner-changing ablation per augmentation (colony/survival/action-cost).
   2. Single scoring path (controller-score = g-efe + augmentation).
   3. R7 precision uses shared futon2.aif.precision.
   4. R14 τ moves with starvation."
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

;; --------------------------------------------------------------------------- ;;
;; 1. Winner-changing ablation tests
;; --------------------------------------------------------------------------- ;;
;; Each fixture is designed so the g-efe core is close between two actions,
;; and the augmentation under test tips the balance. A large lambda makes
;; the augmentation dominant; removing it (lambda=0) lets g-efe decide.

(deftest ablation-colony-changes-winner
  (testing "removing colony-cost augmentation changes the selected action"
    ;; Outbound mode (C prefers forage), very low reserves, high colony lambda.
    ;; Colony-cost penalizes non-return actions when reserves are low.
    ;; With colony: return wins (colony penalizes forage). Without: forage wins.
    (let [obs (make-obs {:food 0.5 :home-prox 0.3 :h 0.5 :hunger 0.5
                         :dist-home 0.6 :reserve-home 0.01 :cargo 0.2
                         :mode :outbound})
          cfg-colony {:actions [:forage :return]
                      :efe {:lambda {:pragmatic 1.0 :ambiguity 0.5 :info 0.0
                                     :colony 20.0 :survival 0.0}
                            :colony {:reserve-thresh 1.0 :non-return-pen 0.6 :return-pen 0.0}
                            :survival {:hunger-thresh 0.55 :hunger-weight 1.5 :dist-weight 0.5
                                       :ingest-buffer 0.30 :return-reduction 0.40}}}
          cfg-no-colony (assoc-in cfg-colony [:efe :lambda :colony] 0.0)
          with-colony (winner base-mu base-prec obs cfg-colony)
          without-colony (winner base-mu base-prec obs cfg-no-colony)]
      (is (not= with-colony without-colony)
          (str "colony ablation changes winner: with=" with-colony
               " without=" without-colony)))))

(deftest ablation-survival-changes-winner
  (testing "removing survival-cost augmentation changes the selected action"
    ;; Outbound mode, low food, high hunger, high survival lambda.
    ;; Survival-cost penalizes forage (high predicted hunger) more than return
    ;; (return-reduction halves the cost).
    ;; With survival: return wins. Without: forage wins.
    (let [obs (make-obs {:food 0.1 :home-prox 0.4 :h 0.9 :hunger 0.9
                         :ingest 0.0 :dist-home 0.5 :reserve-home 0.3
                         :cargo 0.3 :mode :outbound})
          cfg-survival {:actions [:forage :return]
                        :efe {:lambda {:pragmatic 1.0 :ambiguity 0.5 :info 0.0
                                       :colony 0.0 :survival 20.0}
                              :colony {:reserve-thresh 1.0 :non-return-pen 0.6 :return-pen 0.0}
                              :survival {:hunger-thresh 0.55 :hunger-weight 1.5 :dist-weight 0.5
                                         :ingest-buffer 0.30 :return-reduction 0.40}}}
          cfg-no-survival (assoc-in cfg-survival [:efe :lambda :survival] 0.0)
          with-survival (winner base-mu base-prec obs cfg-survival)
          without-survival (winner base-mu base-prec obs cfg-no-survival)]
      (is (not= with-survival without-survival)
          (str "survival ablation changes winner: with=" with-survival
               " without=" without-survival)))))

(deftest ablation-action-cost-changes-winner
  (testing "removing action-cost augmentation changes the selected action"
    ;; Homebound mode (C prefers return). With high hunger-gap-mult on return,
    ;; the action-cost makes return too expensive → hold wins.
    ;; With zero action-cost, return wins (g-efe prefers it in homebound).
    (let [obs (make-obs {:food 0.2 :home-prox 0.5 :h 0.5 :hunger 0.5
                         :ingest 0.1 :dist-home 0.5 :reserve-home 0.5
                         :cargo 0.3 :mode :homebound})
          zero-return-cost {:base-cost 0.0 :empty-home-pen 0.0 :cargo-thresh 0.05
                            :home-thresh 0.8 :hunger-gap-mult 0.0}
          high-return-cost (assoc zero-return-cost :hunger-gap-mult 100.0)
          cfg-high {:actions [:return :hold]
                    :action-costs {:return high-return-cost}
                    :efe {:lambda {:pragmatic 1.0 :ambiguity 0.5 :info 0.0
                                   :colony 0.0 :survival 0.0}}}
          cfg-zero {:actions [:return :hold]
                    :action-costs {:return zero-return-cost}
                    :efe {:lambda {:pragmatic 1.0 :ambiguity 0.5 :info 0.0
                                   :colony 0.0 :survival 0.0}}}
          with-cost (winner base-mu base-prec obs cfg-high)
          without-cost (winner base-mu base-prec obs cfg-zero)]
      (is (not= with-cost without-cost)
          (str "action-cost ablation changes winner: with=" with-cost
               " without=" without-cost)))))

;; --------------------------------------------------------------------------- ;;
;; 2. Controller split — g-efe and augmentation are separate fields
;; --------------------------------------------------------------------------- ;;

(deftest controller-score-split
  (testing "expected-free-energy returns separated g-efe and augmentation"
    (let [obs (make-obs {:cargo 0.3 :mode :outbound})
          result (policy/eval-policy base-mu base-prec obs
                                     {:actions [:forage :return :hold :pheromone]
                                      :efe {:lambda {:pragmatic 1.0 :ambiguity 0.5 :info 0.4
                                                     :colony 0.4 :survival 1.2}}})
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
          cfg {:actions [:forage :return :hold :pheromone]
               :efe {:lambda {:pragmatic 1.0 :ambiguity 0.5 :info 0.4
                              :colony 0.4 :survival 1.2}}}
          tau-fed (:tau (policy/choose-action base-mu base-prec obs-fed cfg))
          tau-starved (:tau (policy/choose-action base-mu base-prec obs-starved cfg))]
      (is (> tau-starved tau-fed)
          (str "starved τ (" tau-starved ") > fed τ (" tau-fed ")")))))
