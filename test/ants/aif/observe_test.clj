(ns ants.aif.observe-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [ants.aif.observe :as observe]))

(defn- grid-coords [w h]
  (vec (for [x (range w)
             y (range h)]
         [x y])))

(defn- edge-coords [w h]
  (->> (grid-coords w h)
       (filter (fn [[x y]]
                 (or (zero? x)
                     (zero? y)
                     (= x (dec w))
                     (= y (dec h)))))
       vec))

(defn- neighbor-coords [[x y]]
  (for [dx [-1 0 1]
        dy [-1 0 1]
        :when (not (and (zero? dx) (zero? dy)))]
    [(+ x dx) (+ y dy)]))

(defn- in-bounds? [[w h] [x y]]
  (and (<= 0 x) (< x w)
       (<= 0 y) (< y h)))

(defn- mean [values]
  (if (seq values)
    (/ (reduce + values) (double (count values)))
    0.0))

(defn- sample-world
  ([] (sample-world {}))
  ([{:keys [edge? degenerate-food? degenerate-pher?]
     :or {edge? false degenerate-food? false degenerate-pher? false}}]
   (gen/let [w (gen/choose 2 6)
             h (gen/choose 2 6)
             queen-initial (gen/double* {:NaN? false :infinite? false :min 0.2 :max 6.0})
             reserves (gen/double* {:NaN? false :infinite? false :min 0.0 :max (max queen-initial 0.2)})
             max-food (if degenerate-food?
                        (gen/return 0.0)
                        (gen/double* {:NaN? false :infinite? false :min 0.1 :max 5.0}))
             max-pher (if degenerate-pher?
                        (gen/return 0.0)
                        (gen/double* {:NaN? false :infinite? false :min 0.1 :max 5.0}))
             foods (gen/vector (gen/double* {:NaN? false :infinite? false :min 0.0 :max (max max-food 0.1)})
                               (* w h))
             phers (gen/vector (gen/double* {:NaN? false :infinite? false :min 0.0 :max (max max-pher 0.1)})
                               (* w h))
             visit-counts (gen/vector (gen/choose 0 20) (* w h))
             loc (if edge?
                   (gen/elements (edge-coords w h))
                   (gen/tuple (gen/choose 0 (dec w)) (gen/choose 0 (dec h))))
             cargo (gen/double* {:NaN? false :infinite? false :min 0.0 :max 3.0})
             ingest (gen/double* {:NaN? false :infinite? false :min 0.0 :max 2.5})
             recent (gen/double* {:NaN? false :infinite? false :min 0.0 :max 2.0})
             mu-h (gen/double* {:NaN? false :infinite? false :min 0.0 :max 1.2})]
     (let [coords (grid-coords w h)
           cell-map (into {}
                          (map-indexed (fn [idx coord]
                                         [coord {:food (nth foods idx)
                                                 :pher (nth phers idx)}])
                                       coords))
           visit-map (into {}
                            (map-indexed (fn [idx coord]
                                           [coord (nth visit-counts idx)])
                                         coords))
           home-aif [0 0]
           home-classic [(dec w) (dec h)]
           cells (-> cell-map
                     (update home-aif (fnil assoc {:food 0.0 :pher 0.0}) :home :aif)
                     (update home-classic (fnil assoc {:food 0.0 :pher 0.0}) :home :classic))
           world {:grid {:size [w h]
                         :max-food max-food
                         :max-pher max-pher
                         :cells cells}
                  :homes {:aif home-aif
                          :classic home-classic}
                  :colonies {:aif {:reserves reserves}}
                  :config {:hunger {:queen {:initial (max queen-initial 1e-6)}}}}
           ant {:species :aif
                :loc loc
                :mu {:h mu-h}
                :cargo cargo
                :ingest ingest
                :recent-gather recent
                :visit-counts visit-map}]
       {:world world
        :ant ant}))))

(def clamped-keys
  [:food :pher :food-trace :pher-trace :home-prox :enemy-prox :h :hunger
   :ingest :friendly-home :trail-grad :novelty :dist-home :reserve-home
   :cargo :recent-gather :white?])

(defn- approx=
  [a b]
  (< (Math/abs (- (double a) (double b))) 1e-9))

(defspec g-observe-values-clamped 200
  (prop/for-all [{:keys [world ant]} (sample-world)]
    (let [obs (observe/g-observe world ant)]
      (every? (fn [k]
                (let [v (double (or (get obs k) 0.0))]
                  (<= 0.0 v 1.0)))
              clamped-keys))))

(defspec g-observe-border-neighbor-means 120
  (prop/for-all [{:keys [world ant]} (sample-world {:edge? true})]
    (let [obs (observe/g-observe world ant)
          loc (:loc ant)
          {:keys [size max-food max-pher cells]} (:grid world)
          foods (->> (neighbor-coords loc)
                     (filter (partial in-bounds? size))
                     (map #(double (get-in cells [% :food] 0.0))))
          phers (->> (neighbor-coords loc)
                     (filter (partial in-bounds? size))
                     (map #(double (get-in cells [% :pher] 0.0))))
          expected-food (observe/normalize (mean foods) max-food)
          expected-pher (observe/normalize (mean phers) max-pher)]
      (and (approx= expected-food (:food-trace obs))
           (approx= expected-pher (:pher-trace obs))))))

(defspec degenerate-field-normalizes-to-zero 120
  (prop/for-all [{:keys [world ant]} (sample-world {:degenerate-food? true
                                                    :degenerate-pher? true})]
    (let [obs (observe/g-observe world ant)]
      (and (zero? (:food obs))
           (zero? (:pher obs))
           (zero? (:food-trace obs))
           (zero? (:pher-trace obs))
           (zero? (:trail-grad obs))))))

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

(deftest dist-home-respects-max-dist
  (let [world (assoc-in base-world [:grid :max-dist] 2.0)
        ant {:species :aif :loc [2 2] :mu {:h 0.5}}
        obs (observe/g-observe world ant)
        [hx hy] (get-in world [:homes :aif])
        [x y] (:loc ant)
        dist (Math/sqrt (+ (Math/pow (- hx x) 2.0)
                           (Math/pow (- hy y) 2.0)))
        expected (observe/clamp01 (/ dist 2.0))]
    (is (approx= expected (:dist-home obs)))))

(deftest sense-vector-ordering
  (let [obs {:food 0.1 :pher 0.2 :food-trace 0.3 :pher-trace 0.4
             :home-prox 0.5 :enemy-prox 0.6 :h 0.7 :ingest 0.15
             :friendly-home 1.0 :trail-grad 0.25 :novelty 0.8
             :dist-home 0.4 :reserve-home 0.3 :cargo 0.8}]
    (is (= [0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.15 1.0 0.25 0.8 0.4 0.3 0.8]
           (observe/sense->vector obs)))))
