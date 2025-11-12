(ns ants.war
  "Scenario wiring for classic vs AIF ant skirmishes."
  (:require [ants.aif.core :as aif]
            [ants.aif.observe :as observe]
            [ants.aif.affect :as affect]
            [ants.ui :as ui]
            [clojure.string :as str]))

(def ^:private white-thresh 0.05)
(def ^:private ingest-eps 0.05)

(def ^:dynamic *per-ant-log* nil)

(defn- log-ant! [event]
  (when (fn? *per-ant-log*)
    (*per-ant-log* event)))

(defn- on-white?
  [world loc]
  (let [food (double (or (get-in world [:grid :cells loc :food]) 0.0))]
    (<= food white-thresh)))

(def default-config
  {:size [24 24]
   :food-max 5.0
   :pher-max 3.0
   :ticks 200
   :ants-per-side 6
   :ema-alpha 0.1
   :enable-termination? true
   :hunger {:initial 0.38
            :burn 0.028
            :feed 0.052
            :rest 0.028
            :load-pressure 0.28
            :gather-feed 0.11
            :deposit-feed 0.08
            :home-bonus 0.025
            :death-threshold 0.99
            :queen {:initial 1.8
                    :burn 0.18
                    :per-ant 0.085
                    :starvation-grace 6
                    :starvation-boost 0.045}}
   :aif aif/default-aif-config})

(def ^:private default-hunger (:hunger default-config))
(def ^:private default-aif (:aif default-config))

(def alice-id :aif-5)

(defn- team-counts
  [world]
  (let [ants (vals (:ants world))]
    {:classic (count (filter #(= (:species %) :classic) ants))
     :aif     (count (filter #(= (:species %) :aif) ants))}))

(defn termination-message
  [world]
  (when-let [reason (get-in world [:termination :reason])]
    (let [scores (:scores world)
          counts (team-counts world)
          base (case reason
                 :alice-dead "Game Over (Alice Died)"
                 :queen-starved "Game Over (Queen Starved)"
                 :all-ants-dead "Game Over (All Ants Dead)"
                 "Game Over")]
      (format "%s | Final Score Classic %.2f (ants %d) vs AIF %.2f (ants %d)"
              base
              (double (get scores :classic 0.0))
              (int (get counts :classic 0))
              (double (get scores :aif 0.0))
              (int (get counts :aif 0))))))

(defn- merge-deep
  [& ms]
  (letfn [(merge* [a b]
            (merge a
                   (into {}
                         (for [[k v] b]
                           [k (if (and (map? (get a k)) (map? v))
                                (merge* (get a k) v)
                                v)]))))]
    (reduce merge* {} (remove nil? ms))))

(defn- hunger-config
  [world]
  (merge-deep default-hunger (get-in world [:config :hunger])))

(defn- queen-config
  [world]
  (:queen (hunger-config world)))

(defn- grid-max-dist
  [[w h]]
  (Math/sqrt (double (+ (* (max 1 (dec w)) (max 1 (dec w)))
                        (* (max 1 (dec h)) (max 1 (dec h)))))))

(defn- center
  [[w h]]
  [(/ (dec w) 2.0) (/ (dec h) 2.0)])

(defn- distance
  [[x y] [tx ty]]
  (let [dx (- tx x)
        dy (- ty y)]
    (Math/sqrt (double (+ (* dx dx) (* dy dy))))))

(defn- initial-food
  [[w h] [x y] food-max]
  (let [ctr (center [w h])
        dist (distance [x y] ctr)
        max-dist (max 1e-6 (grid-max-dist [w h]))
        falloff (- 1.0 (/ dist max-dist))]
    (-> (* (observe/clamp01 falloff) food-max)
        (+ (* 0.2 (Math/sin (+ (* 0.4 x) (* 0.3 y)))))
        (max 0.0))))

(defn- build-grid
  [{:keys [size food-max pher-max]}]
  (let [[w h] size
        cells (into {}
                    (for [x (range w)
                          y (range h)]
                      (let [loc [x y]]
                        [loc {:food (initial-food size loc food-max)
                              :pher 0.0
                              :home nil
                              :ant nil}])))
        max-dist (grid-max-dist size)]
    {:size size
     :max-food food-max
     :max-pher pher-max
     :max-dist max-dist
     :cells cells}))

(defn- place-home
  [world species loc]
  (-> world
      (assoc-in [:homes species] loc)
      (assoc-in [:grid :cells loc :home] species)))

(defn- clamp-loc
  [[w h] [x y]]
  [(-> x (max 0) (min (dec w)))
   (-> y (max 0) (min (dec h)))])

(defn- id-for
  [species idx]
  (keyword (str (name species) "-" idx)))

(defn- spawn-ant
  [world species idx loc]
  (let [id (id-for species idx)
        hunger-cfg (hunger-config world)
        initial-h (double (or (get-in world [:ants id :h]) (:initial hunger-cfg) 0.35))
        ant {:id id
             :species species
             :brain (if (= species :aif) :aif :classic)
             :loc loc
             :dir 0
             :cargo 0.0
             :h (observe/clamp01 initial-h)
             :mu (:mu (get-in world [:ants id]))
             :prec (:prec (get-in world [:ants id]))}
        world (assoc-in world [:ants id] ant)]
    (-> world
        (assoc-in [:grid :cells loc :ant] id))))

(defn- spawn-army
  [world species home n]
  (reduce (fn [world idx]
            (let [[dx dy] [(mod idx 2) (quot idx 2)]
                  offset (if (= species :aif)
                           [(- dx) (- dy)]
                           [dx dy])
                  loc (clamp-loc (get-in world [:grid :size])
                                 (mapv + home offset))]
              (spawn-ant world species idx loc)))
          world
          (range n)))

(defn new-world
  "Create a fresh war world using config (merged with defaults)."
  ([] (new-world default-config))
  ([config]
   (let [config (-> (merge default-config config)
                    (update :hunger #(merge-deep default-hunger %))
                    (update :aif #(merge-deep default-aif %)))
         grid (build-grid config)
         homes {:classic [2 2]
                :aif (let [[w h] (:size config)]
                       [(max 1 (- w 3)) (max 1 (- h 3))])}
         hunger (:hunger config)
         queen (:queen hunger)
         initial-reserve (double (or (:initial queen) 0.0))
         world {:tick 0
                :config config
                :grid grid
                :homes {}
                :scores {:classic 0.0 :aif 0.0}
                :colonies {:classic {:reserves initial-reserve
                                     :starved-ticks 0}
                           :aif {:reserves initial-reserve
                                 :starved-ticks 0}}
                :rolling {:G nil}
                :stats {:aif {:actions-total 0
                              :pheromone-count 0
                              :pheromone-trail-sum 0.0
                              :pheromone-trail-samples 0}}
                :ants {}
                :hero {:alice-dead? false}
                :graveyard []
                :last-events []}]
     (let [world (reduce (fn [w [species loc]]
                           (place-home w species loc))
                         world
                         homes)
           per-side (:ants-per-side config 6)
           world (spawn-army world :classic (:classic homes) per-side)
           world (spawn-army world :aif (:aif homes) per-side)]
       world))))

(defn- decaying
  [current rate]
  (* current rate))

(defn- evaporate
  [world]
  (let [homes (->> (get world :homes)
                   vals
                   (remove nil?))
        radius 2
        rate-near 0.85
        rate-far 0.97
        rate-for (fn [[x y]]
                   (let [near? (some (fn [[hx hy]]
                                       (and hx hy
                                            (<= (max (Math/abs (double (- hx x)))
                                                     (Math/abs (double (- hy y))))
                                                radius)))
                                     homes)]
                     (if near?
                       rate-near
                       rate-far)))]
    (update-in world [:grid :cells]
               (fn [cells]
                 (reduce-kv (fn [m loc cell]
                              (assoc m loc (update cell :pher
                                                   #(decaying (double (or % 0.0))
                                                              (rate-for loc)))))
                            {}
                            cells)))))

(defn- neighbor-locs
  [[[w h] [x y]]]
  (for [dx [-1 0 1]
        dy [-1 0 1]
        :when (not (and (zero? dx) (zero? dy)))
        :let [nx (+ x dx)
              ny (+ y dy)]
        :when (and (<= 0 nx) (< nx w) (<= 0 ny) (< ny h))]
    [nx ny]))

(defn- empty-neighbours
  [world loc]
  (let [size (get-in world [:grid :size])]
    (->> (neighbor-locs [size loc])
         (filter #(nil? (get-in world [:grid :cells % :ant]))))))

(defn- random-empty-neighbour
  [world loc]
  (when-let [empties (seq (empty-neighbours world loc))]
    (rand-nth (vec empties))))
(defn- track-visit
  [world ant]
  (if (and ant (:id ant) (:loc ant))
    (let [loc (:loc ant)
          visits (update (or (:visit-counts ant) {}) loc (fnil inc 0))
          ant' (assoc ant :visit-counts visits)
          world' (assoc-in world [:ants (:id ant)] ant')]
      {:world world'
       :ant ant'})
    {:world world
     :ant ant}))

(defn- richest-neighbour
  ([world loc]
   (richest-neighbour world loc nil))
  ([world loc species]
   (richest-neighbour world loc species nil))
  ([world loc species {:keys [min-food] :or {min-food 0.1}}]
   (let [size (get-in world [:grid :size])
         neighbours (neighbor-locs [size loc])
         home (when species (get-in world [:homes species]))
         filtered (if (and home (seq neighbours))
                    (remove #(= % home) neighbours)
                    neighbours)
         candidates (seq filtered)
         best (when candidates
                (apply max-key #(get-in world [:grid :cells % :food] 0.0) candidates))
         best-food (when best (double (or (get-in world [:grid :cells best :food]) 0.0)))]
     (cond
       (and best (>= best-food min-food)) best
       (seq candidates)
       (let [ant (some-> world
                         (get-in [:grid :cells loc :ant])
                         (get-in world [:ants]))
             visits (:visit-counts ant)
             current-pher (double (or (get-in world [:grid :cells loc :pher]) 0.0))
             pher-best (apply max-key #(get-in world [:grid :cells % :pher] 0.0) candidates)
             pher-val (double (or (get-in world [:grid :cells pher-best :pher]) 0.0))
             empties (vec (empty-neighbours world loc))
             least-visited (when (and visits (seq empties))
                             (apply min-key #(get visits % 0) empties))
             empties-pher (when (seq empties)
                            (apply max-key #(get-in world [:grid :cells % :pher] 0.0) empties))]
         (or (when (> pher-val (+ current-pher 0.05)) pher-best)
             (when empties-pher
               (let [pher-empty (double (or (get-in world [:grid :cells empties-pher :pher]) 0.0))]
                 (when (> pher-empty (+ current-pher 0.02)) empties-pher)))
             least-visited
             empties-pher
             best
             loc))
       :else (or (random-empty-neighbour world loc)
                 best
                 loc)))))

(defn- attempt-move
  [world ant target]
  (let [loc (:loc ant)
        size (get-in world [:grid :size])
        target (clamp-loc size target)
        occupant-id (get-in world [:grid :cells target :ant])]
    (cond
      (= target loc)
      {:world world :ant ant :target target :moved? false :blocked? false}

      (nil? occupant-id)
      (let [world (assoc-in world [:grid :cells loc :ant] nil)
            ant (assoc ant :loc target)
            world (assoc-in world [:grid :cells target :ant] (:id ant))
            world (assoc-in world [:ants (:id ant)] ant)]
        {:world world
         :ant ant
         :target target
         :moved? true
         :blocked? false})

      (= occupant-id (:id ant))
      {:world world :ant ant :target target :moved? false :blocked? false}

      :else
      (let [occupant (get-in world [:ants occupant-id])]
        (if (= (:species occupant) (:species ant))
          (let [world (assoc-in world [:grid :cells loc :ant] occupant-id)
                world (assoc-in world [:grid :cells target :ant] (:id ant))
                ant (assoc ant :loc target)
                occupant (assoc occupant :loc loc)
                world (assoc-in world [:ants (:id ant)] ant)
                world (assoc-in world [:ants occupant-id] occupant)]
            {:world world
             :ant ant
             :target target
             :moved? true
             :blocked? false
             :swapped? true})
          {:world world :ant ant :target target :moved? false :blocked? true})))))

(defn- random-wander
  ([world ant]
   (random-wander world ant {}))
  ([world ant {:keys [prefer-home] :or {prefer-home false}}]
   (let [loc (:loc ant)
         size (get-in world [:grid :size])
         neighbours (neighbor-locs [size loc])
         empties (vec (empty-neighbours world loc))
         visits (:visit-counts ant)
         least-visited (when (and (seq empties) visits)
                         (let [min-count (apply min (map #(get visits % 0) empties))]
                           (some #(when (= (get visits % 0) min-count) %) empties)))
         target (cond
                  least-visited least-visited
                  (seq empties) (rand-nth empties)
                  prefer-home (get-in world [:homes (:species ant)])
                  (seq neighbours) (rand-nth (vec neighbours))
                  :else loc)]
     (if target
       (-> (attempt-move world ant target)
           (assoc :wander? true))
       (let [streak (int (get ant :white-streak 0))
             target (if (>= streak 3)
                      (let [size (get-in world [:grid :size])
                            [x y] loc
                            dirs [[2 0] [-2 0] [0 2] [0 -2]]
                            candidates (->> dirs
                                            (map (fn [[dx dy]] [(+ x dx) (+ y dy)]))
                                            (map #(clamp-loc size %))
                                            (remove #(= % loc)))]
                        (when (seq candidates)
                          (nth candidates (mod streak (count candidates)) loc)))
                      nil)
             dest (or target loc)]
         (-> (attempt-move world ant dest)
             (assoc :wander? true)))))))

(declare step-toward log-alice)

(defn- move-ant
  ([world ant target]
   (move-ant world ant target {}))
  ([world ant target {:keys [wander? wander-when-still? prefer-home?]
                      :or {wander? true
                           wander-when-still? false
                           prefer-home? false}}]
   (let [attempt (attempt-move world ant target)]
     (cond
       (and wander? (:blocked? attempt))
       (or (random-wander (:world attempt) (:ant attempt) {:prefer-home prefer-home?})
           attempt)

       (and wander-when-still? (not (:moved? attempt)))
       (or (random-wander (:world attempt) (:ant attempt) {:prefer-home prefer-home?})
           attempt)

       :else
       attempt))))

(defn- dist2
  [[x y] [hx hy]]
  (let [dx (- hx x)
        dy (- hy y)]
    (+ (* dx dx) (* dy dy))))

(defn- home-directed-move
  [world ant home]
  (when home
    (let [loc (:loc ant)
          size (get-in world [:grid :size])
          base (step-toward loc home)
          neighbours (neighbor-locs [size loc])
          candidates (->> (cons base neighbours)
                          (remove nil?)
                          (distinct)
                          (sort-by #(dist2 % home)))]
      (loop [[dest & more] candidates
             world world
             ant ant]
        (when dest
          (let [res (move-ant world ant dest {:wander? false
                                              :wander-when-still? false
                                              :prefer-home? true})]
            (if (:moved? res)
              res
              (recur more (:world res) (:ant res)))))))))
(defn- step-toward
  [loc target]
  (let [[x y] loc
        [tx ty] target
        step (fn [a b] (cond (< a b) (inc a)
                             (> a b) (dec a)
                             :else a))]
    [(step x tx) (step y ty)]))

(defn- gather-food
  [world ant]
  (let [loc (:loc ant)
        species (:species ant)
        home (get-in world [:homes species])
        home-cell? (and home (= loc home))
        cell (get-in world [:grid :cells loc])
        friendly-home? (and home-cell?
                            (= (:home cell) species))
        capacity (- 1.0 (:cargo ant))
        available (if friendly-home?
                    0.0
                    (get cell :food 0.0))
        min-take 0.15
        proposed (min capacity (min available 0.7))
        consecutive (int (get-in ant [:white-streak] 0))
        take (if (>= proposed min-take) proposed 0.0)
        world (if (pos? take)
                (update-in world [:grid :cells loc :food] #(max 0.0 (- (double %) take)))
                world)
        ant (if (pos? take)
              (-> ant
                  (update :cargo + take)
                  (assoc :white-streak 0))
              (-> ant
                  (update :white-streak (fnil inc 0))))
        world (if (pos? take)
                (assoc-in world [:ants (:id ant)] ant)
                world)]
    {:world world
     :ant ant
     :gather take}))

(defn- deposit-food
  [world ant]
  (let [species (:species ant)
        home (get-in world [:homes species])
        dist-home (if home (dist2 (:loc ant) home) Double/POSITIVE_INFINITY)
        [world ant] (if (and home (> dist-home 0) (<= dist-home 4))
                      (let [world (assoc-in world [:grid :cells (:loc ant) :ant] nil)
                            ant (assoc ant :loc home)
                            world (assoc-in world [:grid :cells home :ant] (:id ant))
                            world (assoc-in world [:ants (:id ant)] ant)]
                        [world ant])
                      [world ant])
        at-home? (= (:loc ant) home)
        cargo (:cargo ant)]
    (if (and at-home? (pos? cargo))
      (let [world (update-in world [:scores species] + cargo)
            world (update-in world [:colonies species :reserves] (fnil + 0.0) cargo)
            ant (assoc ant :cargo 0.0)
            world (assoc-in world [:ants (:id ant)] ant)]
        {:world world :ant ant :deposit cargo})
      {:world world :ant ant :deposit 0.0})))

(defn- kill-ant
  [world ant {:keys [cause] :as details}]
  (let [id (:id ant)
        loc (:loc ant)
        world (update world :ants dissoc id)
        world (if (= id (get-in world [:grid :cells loc :ant]))
                (assoc-in world [:grid :cells loc :ant] nil)
                world)
        entry (merge {:id id
                      :species (:species ant)
                      :tick (:tick world)
                      :cause cause
                      :loc loc}
                     details)
        hero? (= id alice-id)
        grave {:id id
               :species (:species ant)
               :cause cause
               :tick (:tick world)
               :loc loc}]
    (-> world
        (update :graveyard conj entry)
        (update-in [:grid :cells loc :graves] (fnil conj []) grave)
        (cond-> hero? (assoc-in [:hero :alice-dead?] true)))))

(defn- adjust-hunger
  [world ant observation event]
  (let [cfg (hunger-config world)
        current (double (or (:h ant) (:initial cfg) 0.5))
        ingest (double (max 0.0 (or (:ingest event) 0.0)))
        gather (double (max 0.0 (or (:gather event) 0.0)))
        deposit-raw (double (max 0.0 (or (:deposit event) 0.0)))
        home? (= (:loc ant) (:home event))
        home-bonus (double (max 0.0 (or (:home-bonus cfg) 0.0)))
        deposit (-> deposit-raw
                    (+ (if home? home-bonus 0.0))
                    (min 1.0))
        risk (double (max 0.0 (or (:risk event) 0.0)))
        queen (queen-config world)
        starvation-boost (double (max 0.0 (or (:starvation-boost queen) 0.0)))
        reserves (double (max 0.0 (or (get-in world [:colonies (:species ant) :reserves]) 0.0)))
        starving-ticks (int (or (get-in world [:colonies (:species ant) :starved-ticks]) 0))
        cargo (double (max 0.0 (or (:cargo ant) 0.0)))
        load-pressure (double (max 0.0 (or (:load-pressure cfg) 0.0)))
        metabolic-base (double (max 0.0 (or (:metabolic-rate cfg)
                                            (:burn cfg)
                                            0.015)))
        metabolic-rate (-> metabolic-base
                           (+ (* load-pressure cargo))
                           (+ (if (pos? starving-ticks) starvation-boost 0.0))
                           (+ (if (<= reserves 0.5) (* 0.5 starvation-boost) 0.0))
                           (+ (if (< gather 0.4) 0.04 0.0)))
        hunger-obs {:ingest ingest
                    :deposit deposit
                    :risk risk}
        {:keys [h' dh]} (affect/update-hunger current hunger-obs {:metabolic-rate metabolic-rate})
        _ (affect/warn-if-ingesting-while-hunger-rises!
           hunger-obs
           dh
           (fn [payload]
             (when (= alice-id (:id ant))
               (println "[AIF WARN] ingest high but hunger rising"
                        (pr-str (merge {:tick (:tick world)
                                        :action (:action event)
                                        :loc (:loc event)}
                                       payload))))))
        hunger (observe/clamp01 h')
        ant (-> ant
                (assoc :h hunger
                       :dhdt dh
                       :mu (assoc (or (:mu ant) {}) :h hunger))
                (update :recent
                        (fn [recent]
                          (if (seq recent)
                            (let [idx (dec (count recent))
                                  entry (nth recent idx)
                                  entry (-> entry
                                            (assoc-in [:obs :h] hunger)
                                            (assoc-in [:obs :hunger] hunger))]
                              (assoc recent idx entry))
                            recent))))
        threshold (double (or (:death-threshold cfg) 0.999))]
    (if (>= hunger threshold)
      (let [dead-event (-> event
                           (assoc :action :dead
                                  :cause :starvation
                                  :h hunger
                                  :dhdt dh))]
        (log-alice dead-event)
        {:world (kill-ant world ant {:cause :starvation :h hunger})
         :event dead-event
         :ant nil
         :dead? true})
      {:world (assoc-in world [:ants (:id ant)] ant)
       :event (assoc event :h hunger :dhdt dh)
       :ant ant
       :dead? false})))

(defn- consume-colonies
  [world]
  (let [{:keys [burn per-ant starvation-grace]} (queen-config world)
        burn (double (or burn 0.0))
        per-ant (double (or per-ant 0.0))
        grace (max 0 (int (or starvation-grace 0)))]
    (reduce (fn [w species]
              (let [ants (vals (:ants w))
                    alive (count (filter #(= (:species %) species) ants))
                    upkeep (+ burn (* per-ant alive))
                    reserves (double (or (get-in w [:colonies species :reserves]) 0.0))
                    reserves' (max 0.0 (- reserves upkeep))
                    starving? (<= reserves' 0.0)
                    prior (int (or (get-in w [:colonies species :starved-ticks]) 0))
                    ticks (if starving? (inc prior) 0)
                    w (assoc-in w [:colonies species :reserves] reserves')
                    w (assoc-in w [:colonies species :starved-ticks] ticks)
                    queen-dead? (and (> grace 0) (>= ticks grace))
                    w (assoc-in w [:colonies species :queen-starved?] queen-dead?)]
                w))
            world
            (keys (:colonies world)))))

(defn- check-termination
  [world]
  (if (true? (get-in world [:config :enable-termination?] true))
    (let [alice-dead? (true? (get-in world [:hero :alice-dead?]))
          all-dead? (empty? (:ants world))
          starved (some (fn [[species {:keys [queen-starved?]}]]
                          (when queen-starved?
                            {:species species}))
                        (:colonies world))]
      (cond
        alice-dead?
        (assoc world :terminated? true
               :termination {:reason :alice-dead
                             :tick (:tick world)})

        all-dead?
        (assoc world :terminated? true
               :termination {:reason :all-ants-dead
                             :tick (:tick world)})

        starved
        (assoc world :terminated? true
               :termination {:reason :queen-starved
                             :species (:species starved)
                             :tick (:tick world)})

        :else
        (dissoc world :terminated? :termination)))
    (dissoc world :terminated? :termination)))

(defn- add-pheromone
  [world loc amount]
  (if (and loc (pos? amount))
    (let [max-pher (double (or (get-in world [:grid :max-pher]) 3.0))]
      (update-in world [:grid :cells loc :pher]
                 (fn [pher]
                   (min max-pher (+ (double (or pher 0.0)) amount)))))
    world))

(defn- pheromone-drop
  ([world ant amount]
   (pheromone-drop world ant amount nil))
  ([world ant amount target]
   (let [loc (:loc ant)
         deposit (double (max 0.0 amount))
         unit (if (pos? deposit) (/ deposit 3.0) 0.0)
         ahead (when (and target loc)
                 (let [[x y] loc
                       [tx ty] target
                       dx (- tx x)
                       dy (- ty y)]
                   [(+ tx dx) (+ ty dy)]))]
     (-> world
         (add-pheromone loc unit)
         (add-pheromone target (+ unit unit))
         (add-pheromone ahead unit)))))

(defn- classic-policy
  [world ant]
  (let [loc (:loc ant)
        cargo (:cargo ant)
        food-here (get-in world [:grid :cells loc :food] 0.0)
        neighbour-food (get-in world [:grid :cells (richest-neighbour world loc (:species ant)) :food] 0.0)]
    (cond
      (> cargo 0.2) :return
      (> food-here 0.2) :forage
      (> neighbour-food food-here) :forage
      :else :pheromone)))

(defn- adjust-ingest
  [world ant {:keys [add decay]
              :or {add 0.0 decay 0.55}}]
  (let [prior (double (or (:ingest ant) 0.0))
        decay (double (observe/clamp01 decay))
        add (double (max 0.0 add))
        next (+ (* decay prior) add)
        ingest (observe/clamp01 next)
        ant (assoc ant :ingest ingest)
        world (if-let [id (:id ant)]
                (assoc-in world [:ants id] ant)
                world)]
    {:world world
     :ant ant
     :ingest ingest}))

(defn- blend-recent-gather
  [ant sample]
  (let [prev (double (or (:recent-gather ant) 0.0))
        sample (observe/clamp01 (double (max 0.0 sample)))
        blended (observe/clamp01 (+ (* 0.5 prev) (* 0.5 sample)))]
    (assoc ant :recent-gather blended)))

(defn- decay-recent-gather
  [ant]
  (let [prev (double (or (:recent-gather ant) 0.0))
        decayed (observe/clamp01 (* 0.5 prev))]
    (if (zero? decayed)
      (dissoc ant :recent-gather)
      (assoc ant :recent-gather decayed))))

(defn- attach-policy-diagnostics
  "Decorate action events with policy-derived diagnostics for richer logs."
  [event ant]
  (if (and event (= (:species ant) :aif))
    (if-let [policy (:last-policy ant)]
      (let [policies (:policies policy)
            chosen (when policies (get policies (:action event)))
            chosen-G (:G chosen)
            extract (fn [k]
                      (when-let [info (get policies k)]
                        {:action k
                         :G (:G info)
                         :p (:p info)
                         :hunger (double (or (get-in info [:outcome :hunger])
                                             (get-in info [:outcome :h])
                                             0.0))
                         :ingest (double (or (get-in info [:outcome :ingest]) 0.0))
                         :cargo (double (or (get-in info [:outcome :cargo]) 0.0))}))
            alt-actions (when (seq policies)
                          (->> policies
                               (remove (fn [[k _]] (= k (:action event))))
                               (map (fn [[k info]]
                                      {:action k
                                       :G (:G info)
                                       :p (:p info)
                                       :delta (when (number? chosen-G)
                                                (- (:G info) chosen-G))}))
                               (sort-by :G)
                               (take 2)
                               vec))]
        (cond-> (assoc event :policy-tau (:tau policy))
          chosen-G (assoc :policy-chosen-G (double chosen-G))
          (seq alt-actions) (assoc :policy-alt-actions alt-actions)
          true (assoc :policy-forage (extract :forage)
                      :policy-return (extract :return)
                      :policy-hold (extract :hold)
                      :policy-pheromone (extract :pheromone))))
      event)
    event))

;; --- Pretty helpers (local-only; no external deps) --------------------------
(defn- b01 [b] (if b 1 0))
(defn- fmt2 [x] (format "%.2f" (double (or x 0.0))))
(defn- fmt-vec2 [[x y]] (format "[%d %d]" (long x) (long y)))

;; --- Per-AIF ant debug line (Alice) -----------------------------------------

;; --- Logging config & selector ---------------------------------------------

(defn- same-id?
  "Nil/keyword/string/number tolerant equality for ant IDs."
  [a b]
  (letfn [(norm [x]
            (cond
              (nil? x)             nil
              (number? x)          (long x)
              (keyword? x)         (name x)
              (string? x)          x
              :else                x))]
    (= (norm a) (norm b))))

(defn- should-log-alice?
  "Return true if this event should be printed by log-alice.
   Configurable via world config:
   {:logging {:alice? true :alice-id 5}}  ; or :alice-id :alice or \"alice\"
  "
  [world {:keys [species id name] :as _event}]
  (let [cfg       (get-in world [:config :logging] {})
        enabled?  (get cfg :alice? true)
        alice-id  (get cfg :alice-id 5)]
    (and enabled?
         (= species :aif)
         (or (same-id? id alice-id)
             (and name (same-id? name alice-id))))))

(defn log-alice
  "Pretty-print a single detailed line for Alice (AIF #5 by convention).
   Shows action, movement, cargo/ingest/gather/deposit, policy bits (G/p),
   AND the new telemetry: white? ws si."
  [{:keys [id species action loc target cargo ingest gather deposit moved wander
           h tau risk ambiguity need G p
           white? since-ingest] :as event}]
  ;; keep the same selection heuristic you already use; default to id=5, species :aif
  (when (and (= species :aif)
             (or (= id 5) (= id :alice) (= (:name event) "Alice")))
    (let [ws (or (:white-streak (:ant event)) (:white-streak event) 0) ;; prefer ant's maintained streak if you keep it there
          ;; pull any pre-computed alternatives if your diagnostics attach them
          alts (:alts event)  ;; optional; if absent, we skip the tail
          alts-str (when (seq alts)
                     (str " | alts "
                          (->> alts
                               (take 2)      ;; don’t print the whole book
                               (map (fn [{:keys [act G p]}]
                                      (format "%s G=%s p=%s"
                                              (name act) (fmt2 G) (fmt2 p))))
                               (clojure.string/join "; "))))]
      (println
       (str
        (format "Alice action=%s loc=%s target=%s cargo=%s ingest=%s gather=%s deposit=%s moved=%s wander=%s"
                (name action) (fmt-vec2 loc) (fmt-vec2 target)
                (fmt2 cargo) (fmt2 ingest) (fmt2 gather) (fmt2 deposit)
                (if moved "true" "false") (if wander "true" "false"))
        " | "
        (format "h=%s tau=%s Risk(h,ing)=%s Amb=%s Δh=+%s need=%s G=%s p=%s"
                (fmt2 h) (fmt2 tau) (fmt2 risk) (fmt2 ambiguity)
                (fmt2 (or (:dhdt event) 0.0)) (fmt2 need) (fmt2 G) (fmt2 p))
        ;; NEW: telemetry tail
        " | "
        (format "white=%d ws=%d si=%d"
                (b01 white?) (long ws) (long (or since-ingest 0)))
        (or alts-str ""))))))

;; ===================== AIF PIVOT TRACE (columns = ants) =====================

(def ^:private aif-pivot* (atom {:tick nil :by-id {}}))
(def ^:private pivot-header-printed* (atom false))
(def ^:private emit-tsv?* false)

(defn- short-act [a]
  (case a
    :forage "for"
    :return "ret"
    :hold   "hold"
    :pheromone "phm"
    (name (or a "nil"))))

(defn- fmtd ^String [x]
  (cond
    (nil? x)        "-"
    (boolean? x)    (if x "1" "0")
    (number? x)     (format "%.2f" (double x))
    (keyword? x)    (name x)
    (vector? x)     (let [[x y] x] (format "[%d,%d]" (long x) (long y)))
    :else           (str x)))

(defn- pad [w s] (let [s (str s)] (subs (str s (apply str (repeat (+ 0 (max 0 (- w (count s)))) \space)))
                                        0 (min w (count s)))))

(def ^:private pivot-rows
  ;; [label accessor]
  [[:act (fn [e] (short-act (:action e)))]
   [:mode  (fn [e] (or (:mode e)
                       (get-in e [:observation :mode])
                       (get-in e [:ant :mode])))]  ;; ← add this row
   [:cargo (fn [e] (:cargo e))]
   [:ing   (fn [e] (:ingest e))]
   [:gath  (fn [e] (:gather e))]
   [:dep   (fn [e] (:deposit e))]
   [:h     (fn [e] (:h e))]
   [:tau   (fn [e] (:tau e))]
   [:G     (fn [e] (:G e))]
   [:p     (fn [e] (:p e))]
   [:ws    (fn [e] (or (:white-streak e)
                       (get-in e [:ant :white-streak]) 0))]
   [:si    (fn [e] (or (:since-ingest e) 0))]
   [:white (fn [e] (boolean (:white? e)))]
   ;; uncomment if you also want locations
   ;; [:loc   (fn [e] (:loc e))]
   ;; [:tgt   (fn [e] (:target e))]
   ])

(defn- flush-aif-pivot! []
  (let [{:keys [tick by-id]} @aif-pivot*]
    (when (and tick (seq by-id))
      (let [ids (->> (keys by-id)
                     (sort-by (fn [k]
                                (cond
                                  (number? k) [0 k]
                                  (keyword? k) [1 (name k)]
                                  (string? k) [2 k]
                                  :else [3 (str k)]))))
            rows
            ;; materialize the values you were printing into a map-of-maps:
            ;; {metric -> {id -> value-or-string}}
            (into {}
                  (for [[label f] pivot-rows]
                    [label (into {}
                                 (for [id ids
                                       :let [e (get by-id id)
                                             v (when e (f e))]]
                                   [id v]))]))
            batch {:type  :aif/pivot
                   :tick  tick
                   :ids   ids
                   :rows  rows}]         ;; rows as data, not strings

        ;; 1) live emit for analyzers
        (tap> batch)

        ;; 2) optional: keep your TSV output exactly as before
        (when emit-tsv?*
          (when (compare-and-set! pivot-header-printed* false true)
            (println (clojure.string/join \tab
                                          (concat ["tick" "metric"] (map pr-str ids)))))

          (doseq [[label m] rows]
            (println (clojure.string/join \tab
                                          (into [(str tick) (name label)]
                                                (map (comp fmtd m) ids)))))
          (reset! aif-pivot* {:tick nil :by-id {}}))))))

(defn flush-traces! [] (flush-aif-pivot!))

(defn- collect-aif-pivot! [world event]
  (when (= :aif (:species event))
    (let [t (long (or (:tick world) 0))]         ;; <-- fix tick source
      (swap! aif-pivot*
             (fn [{pt :tick by :by-id :as st}]
               (cond
                 (nil? pt)
                 {:tick t :by-id {(or (:id event) (keyword (or (:name event) "anon"))) event}}

                 (not= pt t)                      ;; new tick → flush then start
                 (do
                   (flush-aif-pivot!)
                   {:tick t :by-id {(or (:id event) (keyword (or (:name event) "anon"))) event}})

                 :else
                 (assoc st :by-id (assoc by (or (:id event) (keyword (or (:name event) "anon"))) event))))))))

;; Call once at sim end to flush the last tick (optional)
(defn flush-traces! []
  (flush-aif-pivot!))

;; --- Base event snapshot (before side-effects) ------------------------------
;; (defn- build-base-event
;;   "Snapshot of the ant/policy before mutating world/ant."
;;   [world ant action]
;;   (let [{:keys [id species loc prec dhdt
;;                 last-risk last-ambiguity last-action-cost
;;                 last-info last-colony last-survival last-G]} ant
;;         {:keys [p]} (get-in ant [:last-policy :policies action] {})
;;         tau (double (or (:tau prec) 0.0))
;;         h   (double (or (get-in ant [:mu :h]) 0.0))
;;         ingest (double (or (:ingest ant) 0.0))
;;         cargo  (double (or (:cargo ant) 0.0))]
;;     {:id id :species species :loc loc :action action
;;      :G (double (or last-G 0.0)) :p (double (or p 1.0))
;;      :tau tau :h h :ingest ingest :cargo cargo
;;      :risk (double (or last-risk 0.0))
;;      :ambiguity (double (or last-ambiguity 0.0))
;;      :action-cost (double (or last-action-cost 0.0))
;;      :info (double (or last-info 0.0))
;;      :colony (double (or last-colony 0.0))
;;      :survival (double (or last-survival 0.0))
;;      :dhdt (double (or dhdt 0.0))}))

;; --- Light telemetry helpers -------------------------------------------------

(def ^:private white-eps 0.05)
(def ^:private ingest-eps 0.01)

(defn- on-white?
  [world loc]
  (let [food (double (or (get-in world [:grid :cells loc :food]) 0.0))]
    (<= food white-eps)))

(defn- step-telemetry
  "Compute lightweight telemetry after the action. We do *not* overwrite the
  main white-streak logic that gather-food already maintains; we only
  maintain :since-ingest and a boolean :white? for reporting."
  [world ant ingest]
  (let [white?   (on-white? world (:loc ant))
        prev-si  (int (or (:since-ingest ant) 0))
        si       (if (> (double ingest) ingest-eps) 0 (inc prev-si))]
    {:white? white? :since-ingest si}))

;; --- Build / finalise event --------------------------------------------------

(defn- build-base-event
  [world ant action {:keys [G P observation]}]
  (let [species (:species ant)
        home    (get-in world [:homes species])]
    {:id (:id ant)
     :species species
     :action action
     :mode (or (:mode ant) (:mode observation))  ;; ← add this
     :G (double (or G 0.0))
     :P (double (or P 0.0))
     :cargo (double (or (:cargo ant) 0.0))
     :ingest (double (or (:ingest ant) 0.0))
     :h (double (or (:h ant) 0.0))
     :tau (double (or (get-in ant [:prec :tau]) 0.0))
     :risk (double (or (:last-risk ant) 0.0))
     :ambiguity (double (or (:last-ambiguity ant) 0.0))
     :action-cost (double (or (:last-action-cost ant) 0.0))
     :info (double (or (:last-info ant) 0.0))
     :colony (double (or (:last-colony ant) 0.0))
     :survival (double (or (:last-survival ant) 0.0))
     :dhdt (double (or (:dhdt ant) 0.0))
     :need (double (or (:need-error ant) 0.0))
     :loc (:loc ant)
     :target (:loc ant)
     :home home
     :moved false
     :wander false}))

(defn- finalise-event
  "Merge base snapshot + effect + telemetry to a loggable event."
  [base {:keys [moved? wander? gather deposit ingest dead? target] :as _effect}
   {:keys [white? since-ingest] :as _telemetry}]
  (-> base
      (cond-> target (assoc :target target))
      (assoc :moved (boolean moved?)
             :wander (boolean wander?)
             :gather (double (or gather 0.0))
             :deposit (double (or deposit 0.0))
             :ingest (double (or ingest 0.0))
             :dead (boolean dead?)
             :white? (boolean white?)
             :since-ingest (int (or since-ingest 0)))))

(defn- update-ant-post
  "Persist minimal telemetry onto the ant for next tick without fighting the
  existing white-streak logic managed by gather/decay."
  [ant {:keys [since-ingest white?]} effect]
  (let [{:keys [dead?]} effect]
    (-> ant
        (assoc :since-ingest (int (or since-ingest 0)))
        (assoc :white? (boolean white?))
        (assoc :dead? (boolean dead?)))))

;; --- Action handlers (pure) --------------------------------------------------

(defn- act-forage
  [world ant]
  (let [best        (richest-neighbour world (:loc ant) (:species ant))
        move-res    (move-ant world ant best {:wander-when-still? true})
        {:keys [world ant moved? wander? target]} move-res
        gather-res  (gather-food world ant)
        world       (:world gather-res)
        ant         (:ant gather-res)
        g           (double (or (:gather gather-res) 0.0))
        ingest-res  (adjust-ingest world ant {:add g :decay 0.6})
        world       (:world ingest-res)
        ant-wi      (:ant ingest-res)
        ant         (blend-recent-gather ant-wi (min 1.0 g))
        world       (assoc-in world [:ants (:id ant)] ant)
        ingest      (:ingest ingest-res)]
    [world ant {:moved? moved? :wander? wander? :gather g :deposit 0.0
                :ingest ingest :dead? false :target target}]))

(defn- act-return
  [world ant]
  (let [home        (get-in world [:homes (:species ant)])
        primary     (home-directed-move world ant home)
        target      (step-toward (:loc ant) home)
        direct      (when-not (:moved? primary)
                      (move-ant (or (:world primary) world)
                                (or (:ant primary) ant)
                                target
                                {:wander-when-still? true
                                 :prefer-home? true}))
        wander      (when (and (not (:moved? direct)) (not (:moved? primary)))
                      (random-wander (or (:world direct) (:world primary) world)
                                     (or (:ant direct) (:ant primary) ant)
                                     {:prefer-home true}))
        move-result (or (when (:moved? primary) primary)
                        (when (:moved? direct)  direct)
                        (when (:moved? wander)  wander)
                        (or wander direct primary)
                        {:world world :ant ant :target (:loc ant) :moved? false :wander? false})
        {:keys [world ant moved? wander? target]} move-result
        dep-res     (deposit-food world ant)
        world       (:world dep-res)
        ant         (:ant dep-res)
        d           (double (or (:deposit dep-res) 0.0))
        ingest-res  (adjust-ingest world ant {:add 0.0 :decay 0.55})
        world       (:world ingest-res)
        ant-wi      (:ant ingest-res)
        ant         (decay-recent-gather ant-wi)
        world       (assoc-in world [:ants (:id ant)] ant)
        ingest      (:ingest ingest-res)]
    [world ant {:moved? moved? :wander? wander? :gather 0.0 :deposit d
                :ingest ingest :dead? false :target target}]))

(defn- act-hold
  [world ant]
  (let [home        (get-in world [:homes (:species ant)])
        at-home?    (= (:loc ant) home)
        move-res    (if (not at-home?)
                      (move-ant world ant (step-toward (:loc ant) home))
                      (move-ant world ant (:loc ant)
                                {:wander? true :wander-when-still? true}))
        {:keys [world ant moved? wander? target]} move-res
        ingest-res  (adjust-ingest world ant {:add 0.0 :decay 0.5})
        world       (:world ingest-res)
        ant-wi      (:ant ingest-res)
        ant         (decay-recent-gather ant-wi)
        world       (assoc-in world [:ants (:id ant)] ant)
        ingest      (:ingest ingest-res)]
    [world ant {:moved? moved? :wander? wander? :gather 0.0 :deposit 0.0
                :ingest ingest :dead? false :target target}]))

(defn- act-pheromone
  [world ant]
  (let [home        (get-in world [:homes (:species ant)])
        goal        (or (get-in ant [:mu :goal]) home)
        target      (step-toward (:loc ant) goal)
        cargo       (double (or (:cargo ant) 0.0))
        base-dep    (+ 0.25 (* 0.85 cargo))
        world       (pheromone-drop world ant base-dep target)
        move-res    (move-ant world ant target)
        {:keys [world ant moved? wander? target]} move-res
        ingest-res  (adjust-ingest world ant {:add 0.0 :decay 0.45})
        world       (:world ingest-res)
        ant-wi      (:ant ingest-res)
        ant         (decay-recent-gather ant-wi)
        world       (assoc-in world [:ants (:id ant)] ant)
        ingest      (:ingest ingest-res)]
    [world ant {:moved? moved? :wander? wander? :gather 0.0 :deposit 0.0
                :ingest ingest :dead? false :target target}]))

(defn- act-dead
  [world ant]
  [world ant {:moved? false :wander? false :gather 0.0 :deposit 0.0
              :ingest 0.0 :dead? true :target (:loc ant)}])

(defn- perform-action
  [world ant action]
  (case action
    :forage    (act-forage world ant)
    :return    (act-return world ant)
    :hold      (act-hold world ant)
    :pheromone (act-pheromone world ant)
    :dead      (act-dead world ant)
    (act-hold world ant)))

;; --- Public: apply-action (refactored) --------------------------------------
;; --- Public: apply-action (now 2-arity & 3-arity) ---------------------------

(defn- apply-action
  "Refactored core.

   Arity 1: (apply-action world ant)
   - Reads the chosen action (& basic diags) off the ant.
   - Falls back to a fresh observation when missing.

   Arity 2: (apply-action world ant {:keys [action G P observation]})
   - Executes the chosen action and returns {:world :ant :event}."
  ;; 2-arity wrapper
  ([world ant]
   (let [action       (or (:last-action ant)
                          (get-in ant [:last-policy :action])
                          :hold)
         ;; keep logs stable even if AIF didn't stash these
         G            (double (or (:last-G ant)
                                  (get-in ant [:last-policy :policies action :G])
                                  0.0))
         P            (double (or (get-in ant [:last-policy :policies action :p]) 1.0))
         observation  (or (:last-observation ant)
                          ;; safe, cheap pre-action observation for classic
                          (observe/g-observe world ant))]
     (apply-action world ant {:action action :G G :P P :observation observation})))

  ;; 3-arity core (your existing body kept intact)
  ([world ant {:keys [action G P observation] :as policy-out}]
   ;; rolling EMA of G (AIF only) + pheromone action stats
   (let [species   (:species ant)
         ema-alpha (get-in world [:config :ema-alpha] 0.1)
         world     (if (= species :aif)
                     (let [world (update-in world [:rolling :G]
                                            (fn [prev]
                                              (let [g (double (or G 0.0))]
                                                (if prev
                                                  (+ (* (- 1 ema-alpha) prev)
                                                     (* ema-alpha g))
                                                  g))))
                           trail (double (or (:trail-grad observation) 0.0))
                           world (update-in world [:stats :aif :actions-total] (fnil inc 0))]
                       (if (= action :pheromone)
                         (-> world
                             (update-in [:stats :aif :pheromone-count] (fnil inc 0))
                             (update-in [:stats :aif :pheromone-trail-sum] (fnil + 0.0) trail)
                             (update-in [:stats :aif :pheromone-trail-samples] (fnil inc 0)))
                         world))
                     world)
         base      (build-base-event world ant action policy-out)
         ;; do the action
         [world1 ant1 effect] (perform-action world ant action)
         ;; light telemetry
         telem   (step-telemetry world1 ant1 (or (:ingest effect) (:ingest ant1) 0.0))
         ant2    (update-ant-post ant1 telem effect)
         event0  (finalise-event base effect telem)
         event   (attach-policy-diagnostics event0 ant2)
         tracked (track-visit world1 ant2)
         world2  (:world tracked)]
   ;; ===== FORCED TRACE 
     (collect-aif-pivot! world2 event)
     {:world world2
      :ant   (:ant tracked)
      :event event})))

(defn- step-ant
  [world ant]
  (if (= (:brain ant) :aif)
    (let [{ant1 :ant :as aif-res}  (aif/aif-step world ant)
          {:keys [world ant event]} (apply-action world ant1)]
      (assoc aif-res :world world :ant ant :event event))

    ;; classic must set :last-action itself
    (let [action                     (classic-policy world ant)
          obs                        (observe/g-observe world ant)
          ant*                       (-> ant
                                         (assoc :last-action action
                                                :last-G 0.0)
                                         (assoc-in [:last-policy :policies action :p] 1.0))
          {:keys [world ant event]}  (apply-action world ant*)]
      {:world       world
       :ant         ant
       :event       event
       :action      action
       :observation obs})))

(defn- inc0
  "Nil-safe increment."
  [n]
  (inc (long (or n 0))))

(defn- add0
  "Nil-safe addition, for update with + val."
  [a b]
  (+ (long (or a 0))
     (long (or b 0))))

(defn step
  "Advance the world by a single tick, returning the updated world."
  [world]
  (let [world (evaporate world)
        ids (sort (keys (:ants world)))]
    (loop [world world
           events []
           [id & more] ids]
      (if-not id
        (-> world
            (update :tick inc0)
            (assoc :last-events events)
            consume-colonies
            check-termination)
        (if-let [ant (get-in world [:ants id])]
          (let [{world' :world
                 ant' :ant
                 event' :event
                 observation' :observation} (step-ant world ant)
                ant-after (or ant' (get-in world' [:ants id]))
                hunger-result (if ant-after
                                (adjust-hunger world' ant-after observation' event')
                                {:world world'
                                 :event event'
                                 :ant nil})
                world-next (:world hunger-result)
                event-next (:event hunger-result)]
            (recur world-next (cond-> events event-next (conj event-next)) more))
          (recur world events more))))))

(defn simulate
  "Run the war scenario for (:ticks config) ticks and return the final world.

  Options map:
  - :hud?    print HUD each tick when true (default true)
  - :on-tick fn invoked with world after each tick advance (overrides hud?)"
  ([config]
   (simulate config {}))
  ([config {:keys [hud? on-tick]
            :or {hud? true}}]
   (let [config (-> (merge default-config config)
                    (update :hunger #(merge-deep default-hunger %))
                    (update :aif #(merge-deep default-aif %)))
         total (:ticks config)
         printer (cond
                   on-tick on-tick
                   hud? (fn [world]
                          (println (ui/scoreboard world)))
                   :else (fn [_] nil))]
     (loop [world (new-world config)
            remaining total]
       (cond
         (:terminated? world)
         (do
           (printer world)
           (when-let [message (termination-message world)]
             (println message))
           (flush-traces!)
           world)

         (zero? remaining)
         (do
           (printer world)
           (flush-traces!)
           world)

         :else
         (do
           (printer world)
           (recur (step world) (dec remaining))))))))

(defn run
  "Run the war scenario for (:ticks config) ticks, printing the HUD each step.
  Returns the final world."
  ([] (run default-config))
  ([config]
   (let [final-world (simulate config {:hud? true
                                       :on-tick (fn [world]
                                                  (println (ui/scoreboard world)))})]
     final-world)))
