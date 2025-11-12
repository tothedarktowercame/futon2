(ns ants.aif.observe
  "Sensing and normalization helpers for the active inference ants."
  (:require [clojure.math :as math]))

(def ^:private default-max-food 5.0)
(def ^:private default-max-pher 5.0)

(defn clamp01
  "Clamp a scalar to the closed interval [0,1]."
  [x]
  (-> x (max 0.0) (min 1.0)))

(defn normalize
  "Normalize a scalar given inclusive bounds. Degenerate ranges collapse to 0."
  ([value max-val]
   (normalize value 0.0 max-val))
  ([value min-val max-val]
   (cond
     (nil? value) 0.0
     (<= max-val min-val) 0.0
     :else
     (-> (/ (- (double value) min-val)
            (double (- max-val min-val)))
         clamp01))))

(defn invert
  "Return 1-x within [0,1]."
  [x]
  (- 1.0 (clamp01 x)))

(defn- grid-size [world]
  (or (get-in world [:grid :size]) [1 1]))

(defn- max-food [world]
  (double (or (get-in world [:grid :max-food]) default-max-food)))

(defn- max-pher [world]
  (double (or (get-in world [:grid :max-pher]) default-max-pher)))

(defn- grid-max-dist [world]
  (or (get-in world [:grid :max-dist])
      (let [[w h] (grid-size world)
            w' (max 1 (dec w))
            h' (max 1 (dec h))]
        (math/sqrt (+ (* w' w') (* h' h'))))))

(defn- cell-at [world loc]
  (get-in world [:grid :cells loc]
          {:food 0.0 :pher 0.0 :home nil :ant nil}))

(defn- neighbor-offsets []
  (for [dx [-1 0 1]
        dy [-1 0 1]
        :when (not (and (zero? dx) (zero? dy)))]
    [dx dy]))

(defn- in-bounds?
  [world [x y]]
  (let [[w h] (grid-size world)]
    (and (<= 0 x) (< x w)
         (<= 0 y) (< y h))))

(defn- neighbor-cells
  [world [x y]]
  (->> (neighbor-offsets)
       (map (fn [[dx dy]] [(+ x dx) (+ y dy)]))
       (filter (partial in-bounds? world))
       (map (partial cell-at world))))

(defn- mean
  [values]
  (if (seq values)
    (/ (reduce + values) (double (count values)))
    0.0))

(defn- proximity
  "Proximity is 1 when collocated, 0 at max grid distance."
  [world loc target]
  (if (and target (in-bounds? world target))
    (let [dist (let [[x y] loc
                     [tx ty] target
                     dx (- tx x)
                     dy (- ty y)]
                 (math/sqrt (+ (* dx dx) (* dy dy))))
          max-dist (max 1e-9 (grid-max-dist world))]
      (-> (/ dist max-dist)
          clamp01
          invert))
    0.0))

(defn g-observe
  "Gather normalized sensory evidence for an ant.

  Returns a map with normalized keys:
  - :food           local food density
  - :pher           local pheromone strength
  - :food-trace     neighbour food mean
  - :pher-trace     neighbour pher mean
  - :home-prox      closeness to friendly home
  - :enemy-prox     closeness to opposing home
  - :h              agent's felt hunger (derived from latent state)
  - :hunger         alias for :h for preference-based policies
  - :ingest         recent ingest rate proxy (decays between events)
  - :friendly-home  1 when the ant stands on its own hive cell
  - :trail-grad     pheromone gradient magnitude relative to neighbours
  - :novelty        inverse visit frequency (1/(1+visits))
  - :dist-home      normalized distance from home
  - :reserve-home   normalized colony reserves
  "
  [world {:keys [loc species] :as ant}]
  (let [loc (or loc [0 0])
        species (or species :aif)
        cell (cell-at world loc)
        neighbor-cells (neighbor-cells world loc)
        neighbor-foods (map #(double (or (:food %) 0.0)) neighbor-cells)
        neighbor-phers (map #(double (or (:pher %) 0.0)) neighbor-cells)
        food-max (max-food world)
        pher-max (max-pher world)
        home (get-in world [:homes species])
        enemy (get-in world [:homes (if (= species :aif) :classic :aif)])
        hunger (or (get-in ant [:mu :h]) (get ant :h) 0.5)
        cargo (double (or (:cargo ant) 0.0))
        ingest (double (or (:ingest ant) 0.0))
        friendly-home (if (and home (= loc home)
                               (= (:home cell) species))
                        1.0
                        0.0)
        pher-self (normalize (:pher cell) pher-max)
        neighbor-pher-norms (map #(normalize (:pher %) pher-max) neighbor-cells)
        trail-grad (if (seq neighbor-pher-norms)
                     (clamp01 (- (apply max neighbor-pher-norms) pher-self))
                     0.0)
        visits (double (or (get-in ant [:visit-counts loc]) 0.0))
        novelty (clamp01 (/ 1.0 (+ 1.0 visits)))
        dist-home (if home
                    (let [[x y] loc
                          [hx hy] home
                          dx (- hx x)
                          dy (- hy y)
                          dist (Math/sqrt (+ (* dx dx) (* dy dy)))
                          max-dist (max 1e-9 (grid-max-dist world))]
                      (clamp01 (/ dist max-dist)))
                    1.0)
        reserves (double (or (get-in world [:colonies species :reserves]) 0.0))
        max-reserve (double (or (get-in world [:config :hunger :queen :initial]) 5.0))
        reserve-home (clamp01 (/ reserves (max max-reserve 1e-6)))
        recent-gather (clamp01 (double (or (:recent-gather ant) 0.0)))
        epsilon-food 0.05
        epsilon-pher 0.10
        epsilon-trace 0.10
        white? (and (< (double (or (:food cell) 0.0)) (* epsilon-food food-max))
                    (< (double (or (:pher cell) 0.0)) (* epsilon-pher pher-max))
                    (< (mean neighbor-foods) (* epsilon-trace food-max)))]

    {:food       (normalize (:food cell) food-max)
     :pher       (normalize (:pher cell) pher-max)
     :food-trace (normalize (mean neighbor-foods) food-max)
     :pher-trace (normalize (mean neighbor-phers) pher-max)
     :home-prox  (proximity world loc home)
     :enemy-prox (proximity world loc enemy)
     :h          (clamp01 hunger)
     :hunger     (clamp01 hunger)
     :ingest     (clamp01 ingest)
     :friendly-home friendly-home
     :trail-grad trail-grad
     :novelty    novelty
     :dist-home  dist-home
     :reserve-home reserve-home
     :recent-gather recent-gather
     :cargo      (clamp01 cargo)
     :white? (if white? 1.0 0.0)}))

(defn sense->vector
  "Return the observation map as a consistent vector ordering useful for ML-ish maths."
  [observation]
  (mapv observation
        [:food :pher :food-trace :pher-trace :home-prox :enemy-prox :h :ingest
         :friendly-home :trail-grad :novelty :dist-home :reserve-home :cargo]))
