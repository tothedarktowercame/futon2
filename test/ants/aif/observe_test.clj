(ns ants.aif.observe-test
  (:require [clojure.test :refer [deftest is testing]]
            [ants.aif.observe :as observe]))

(def base-world
  {:grid {:size [5 5]
          :max-food 5.0
          :max-pher 4.0
          :cells {[2 2] {:food 5.0 :pher 2.0}
                  [1 2] {:food 3.0 :pher 1.0}
                  [3 2] {:food 1.0 :pher 0.0}
                  [2 1] {:food 0.0 :pher 0.5}
                  [2 3] {:food 4.0 :pher 3.5}}
          :max-dist (Math/sqrt 32)}
   :homes {:aif [4 4]
           :classic [0 0]}})

(deftest clamp01-bounds
  (is (= 0.0 (observe/clamp01 -1)))
  (is (= 0.25 (observe/clamp01 0.25)))
  (is (= 1.0 (observe/clamp01 3))))

(deftest normalize-with-bounds
  (is (= 0.0 (observe/normalize nil 5.0)))
  (is (= 0.0 (observe/normalize 2 2 2)))
  (is (= 0.5 (observe/normalize 2.5 0 5))))

(deftest g-observe-collects-fields
  (let [ant {:species :aif :loc [2 2] :mu {:h 0.8} :cargo 0.25 :ingest 0.4}
        obs (observe/g-observe base-world ant)]
    (testing "local values normalize"
      (is (== 1.0 (:food obs)))
      (is (= 0.5 (:pher obs))))
    (testing "neighbour mean handling"
      (is (< 0.0 (:food-trace obs) (:food obs)))
      (is (< 0.0 (:pher-trace obs) 1.0)))
    (testing "home proximity computed"
      (is (> (:home-prox obs) 0.0))
      (is (<= (:enemy-prox obs) 0.5)))
    (testing "hunger pulled from ant state"
      (is (= 0.8 (:h obs)))
      (is (= 0.8 (:hunger obs))))
    (testing "ingest carries recent feed proxy"
      (is (= 0.4 (:ingest obs))))
    (testing "friendly home indicator"
      (is (zero? (:friendly-home obs))))
    (testing "trail gradient and novelty"
      (is (<= 0.0 (:trail-grad obs) 1.0))
      (is (= 1.0 (:novelty obs))))
    (testing "distance and reserves"
      (is (> (:dist-home obs) 0.0))
      (is (zero? (:reserve-home obs))))
    (testing "cargo normalized"
      (is (= 0.25 (:cargo obs))))))

(deftest sense-vector-ordering
  (let [obs {:food 0.1 :pher 0.2 :food-trace 0.3 :pher-trace 0.4
             :home-prox 0.5 :enemy-prox 0.6 :h 0.7 :ingest 0.15
             :friendly-home 1.0 :trail-grad 0.25 :novelty 0.8
             :dist-home 0.4 :reserve-home 0.3 :cargo 0.8}]
    (is (= [0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.15 1.0 0.25 0.8 0.4 0.3 0.8]
           (observe/sense->vector obs)))))
