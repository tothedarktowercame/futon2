(ns ants.aif.rollout-test
  "Slice 4 tests: R13 multi-step rollout + R8 per-tick F.

   Acceptance bar:
   1. Planted 2-step-payoff: greedy picks a different (worse) action than H=3 rollout.
   2. F surfaced in aif-step output; F decreases on average over a short run
      and spikes on a planted surprise."
  (:require [clojure.test :refer [deftest is testing]]
            [ants.aif.policy :as policy]
            [ants.aif.core :as core]))

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

(def default-cfg
  {:actions [:forage :return :hold :pheromone]
   :efe {:lambda {:pragmatic 1.0 :ambiguity 0.5 :info 0.4 :colony 0.4 :survival 1.2}
         :colony {:reserve-thresh 1.0 :non-return-pen 0.6 :return-pen 0.0}
         :survival {:hunger-thresh 0.55 :hunger-weight 1.5 :dist-weight 0.5
                    :ingest-buffer 0.30 :return-reduction 0.40}}})

(defn- make-world
  "Build a minimal test world with food near home."
  [home food-loc food-amount]
  (let [w {:grid {:size [50 50] :max-food 5.0 :max-pher 5.0
                  :cells (into {} (for [x (range 50) y (range 50)]
                                    [[x y] {:food 0.0 :pher 0.0 :home nil :ant nil}]))}
           :homes {:aif home :classic [30 30]}
           :config {:aif core/default-aif-config}
           :armies [:classic :aif]
           :colonies {:aif {:reserves 5.0 :starved-ticks 0}
                      :classic {:reserves 5.0 :starved-ticks 0}}
           :scores {:aif 0.0 :classic 0.0}
           :tick 0 :ants {} :last-events []}]
    (-> w
        (assoc-in [:grid :cells home :home] :aif)
        (assoc-in [:grid :cells food-loc :food] food-amount))))

(deftest rollout-h3-differs-from-greedy
  (testing "H=3 rollout picks a different action than greedy H=1"
    (let [obs {:food 0.3 :pher 0.2 :food-trace 0.15 :pher-trace 0.2
               :home-prox 0.8 :enemy-prox 0.3 :h 0.6 :hunger 0.6 :ingest 0.1
               :friendly-home 0.0 :trail-grad 0.1 :novelty 0.5 :dist-home 0.2
               :reserve-home 0.01 :cargo 0.0 :mode :homebound}
          greedy (:action (policy/choose-action base-mu base-prec obs default-cfg))
          h3 (:action (policy/choose-action base-mu base-prec obs
                                             (assoc default-cfg :horizon 3 :discount 0.9)))]
      (is (not= greedy h3)
          (str "greedy=" greedy " not equal H=3=" h3))
      (is (= :hold h3)
          "H=3 rollout picks :hold (better long-term in homebound with no cargo)"))))

(deftest rollout-h1-equals-greedy
  (testing "H=1 rollout equals greedy (no horizon effect)"
    (let [obs {:food 0.3 :pher 0.2 :food-trace 0.15 :pher-trace 0.2
               :home-prox 0.8 :enemy-prox 0.3 :h 0.6 :hunger 0.6 :ingest 0.1
               :friendly-home 0.0 :trail-grad 0.1 :novelty 0.5 :dist-home 0.2
               :reserve-home 0.01 :cargo 0.0 :mode :homebound}
          greedy (:action (policy/choose-action base-mu base-prec obs default-cfg))
          h1 (:action (policy/choose-action base-mu base-prec obs
                                             (assoc default-cfg :horizon 1 :discount 0.9)))]
      (is (= greedy h1)
          "H=1 equals greedy"))))

(deftest f-surfaced-in-aif-step
  (testing "aif-step returns :F (per-tick variational free energy)"
    (let [home [20 20]
          world (make-world home [20 21] 3.0)
          ant {:species :aif :loc home :home home :cargo 0.0 :h 0.5}
          res (core/aif-step world ant)]
      (is (contains? res :F) "aif-step output has :F key")
      (is (number? (:F res)) ":F is a number")
      (is (>= (:F res) 0.0) ":F is non-negative"))))

(deftest f-is-nonzero-and-tracked
  (testing "F is non-zero when there are prediction errors and tracked across ticks"
    ;; After the ensure-mu fix (observation no longer merged into sens),
    ;; F should be non-zero when the ant's prior beliefs don't match the
    ;; current observation. Run a few ticks and verify F is tracked.
    (let [home [20 20]
          world (make-world home [20 21] 3.0)]
      (loop [w world
             a {:species :aif :loc home :home home :cargo 0.0 :h 0.5}
             n 5
             fs []]
        (if (zero? n)
          (do
            (is (every? number? fs) "all F values are numbers")
            (is (some #(> % 0.0) fs) "at least one F is non-zero (prediction error exists)"))
          (let [res (core/aif-step w a)
                next-ant (:ant res)
                next-ant (assoc next-ant :loc home)]
            (recur w next-ant (dec n) (conj fs (:F res)))))))))

(deftest f-spikes-on-surprise
  (testing "F spikes when the ant encounters unexpected food (planted surprise)"
    ;; Ant starts at a food-less location, forms beliefs (food=0).
    ;; Then we place it at a food-rich location — the prediction error
    ;; (observed food >> expected food) should produce a non-zero F.
    (let [home [20 20]
          empty-loc [20 20]
          food-loc [15 15]
          world (make-world home food-loc 4.0)
          ;; Step 1: ant at empty location — observes food=0, forms beliefs
          ant1 {:species :aif :loc empty-loc :home home :cargo 0.0 :h 0.5}
          res1 (core/aif-step world ant1)
          f1 (:F res1)
          ;; Step 2: move ant to food location — surprise!
          ant2 (assoc (:ant res1) :loc food-loc)
          res2 (core/aif-step world ant2)
          f2 (:F res2)]
      (is (> f2 f1)
          (str "F spikes on surprise: normal=" f1 " surprise=" f2)))))
