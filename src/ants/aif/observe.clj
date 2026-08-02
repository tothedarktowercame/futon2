(ns ants.aif.observe
  "Sensing and normalization helpers for the active inference ants."
  (:require [clojure.math :as math]))

(def ^:private default-max-food 5.0)
(def ^:private default-max-pher 5.0)

(def moore-directions
  "Stable nine-cell Moore sensorium: eight compass neighbours plus self."
  [[:nw [-1 -1]] [:n [0 -1]] [:ne [1 -1]]
   [:w [-1 0]]   [:self [0 0]] [:e [1 0]]
   [:sw [-1 1]]  [:s [0 1]]   [:se [1 1]]])

(def directional-sensory-keys
  "Scalar channel keys used by predictive coding and EFE for the 9-cell fields."
  (vec (for [channel ["food" "pher"]
             [direction _] moore-directions]
         (keyword channel (name direction)))))

(defn sensory-value
  "Read a scalar or directional observation channel from an observation map."
  [observation channel]
  (case (namespace channel)
    "food" (get-in observation [:food-field (keyword (name channel))] 0.0)
    "pher" (get-in observation [:pher-field (keyword (name channel))] 0.0)
    (get observation channel 0.0)))

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

(defn- enemy-species
  [world species]
  (let [armies (or (:armies world) [:classic :aif])]
    (first (remove #(= % species) armies))))

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

(defn- directional-fields
  [world [x y]]
  (let [food-max (max-food world)
        pher-max (max-pher world)
        entries (for [[direction [dx dy]] moore-directions
                      :let [candidate [(+ x dx) (+ y dy)]
                            valid? (in-bounds? world candidate)
                            cell (when valid? (cell-at world candidate))]]
                  [direction {:loc (when valid? candidate)
                              :food (normalize (:food cell) food-max)
                              :pher (normalize (:pher cell) pher-max)}])]
    {:food-field (into {} (map (fn [[direction entry]]
                                 [direction (:food entry)])) entries)
     :pher-field (into {} (map (fn [[direction entry]]
                                 [direction (:pher entry)])) entries)
     :sensorium-locs (into {} (map (fn [[direction entry]]
                                     [direction (:loc entry)])) entries)}))

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
  - :food-field     normalized food at NW/N/NE/W/self/E/SW/S/SE
  - :pher-field     normalized pheromone at the same nine cells
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
  - :food-progress  carried food weighted by proximity to home
  "
  [world {:keys [loc species] :as ant}]
  (let [loc (or loc [0 0])
        species (or species :aif)
        cell (cell-at world loc)
        neighbor-cells (neighbor-cells world loc)
        neighbor-foods (map #(double (or (:food %) 0.0)) neighbor-cells)
        neighbor-phers (map #(double (or (:pher %) 0.0)) neighbor-cells)
        {:keys [food-field pher-field sensorium-locs]}
        (directional-fields world loc)
        food-max (max-food world)
        pher-max (max-pher world)
        home (get-in world [:homes species])
        enemy (let [enemy-spec (enemy-species world species)]
                (get-in world [:homes enemy-spec]))
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
     :food-field food-field
     :pher-field pher-field
     :sensorium-locs sensorium-locs
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
     ;; Carried progress is still exposed to the ant's survival risk. The
     ;; forward model preserves this mean across a cache drop and reduces its
     ;; predictive variance once the food is banked in the shared grid.
     :food-progress (clamp01 (* cargo (proximity world loc home)))
     :white? (if white? 1.0 0.0)}))

(defn sense->vector
  "Return the observation map as a consistent vector ordering useful for ML-ish maths."
  [observation]
  (mapv #(sensory-value observation %)
        (concat [:food :pher :food-trace :pher-trace :home-prox :enemy-prox :h :ingest
                 :friendly-home :trail-grad :novelty :dist-home :reserve-home :cargo]
                directional-sensory-keys)))
