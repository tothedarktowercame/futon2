(ns ants.aif.forward
  "Pure forward kernel for the ant forager (R4).

   `ant-kernel` is the SINGLE-ANT local transition: position/move, food-gather,
   deposit, local pheromone-drop, ingest/cargo/recent-gather update — under a
   chosen macro-action (:forage/:return/:hold/:pheromone).  It is a PURE
   function: no atoms, no logging, no global world mutation.  Any randomness is
   routed through an injectable `:rand-fn` (defaults to `clojure.core/rand-nth`,
   preserving bit-identical behaviour with the historical `war.clj` live path).

   The kernel operates on a **local-view** — a read-only snapshot of the cells
   the ant can see and affect (its own cell + neighbours + home).  It returns a
   next-ant-state plus a set of **effects** (food-deltas, pher-deltas,
   score/reserve deltas) that the live caller applies to the world.

   `forward-predict` calls the SAME kernel and returns a distribution (mean +
   per-channel variance) suitable for EFE scoring in later slices.

   Faithfulness tag: FEP-derived (R4 — one pure forward kernel shared by
   live-step and prediction)."
  (:require [ants.aif.observe :as observe]))

;; --------------------------------------------------------------------------- ;;
;; Local-view: a read-only snapshot of what the ant can see/affect.
;; --------------------------------------------------------------------------- ;;

(defn local-view
  "Build a local-view snapshot from the world for a single ant.

   Contains exactly the information the kernel needs:
   :size       grid [w h]
   :cells      map loc->{:food :pher :ant :home}  (full grid; the kernel only
               reads cells by loc — the snapshot is read-only)
   :ants       the full ants map (id → ant) — needed for occupant species
               lookup during move (ally swap logic)
   :home       the ant's home loc
   :max-food   grid max-food bound
   :max-pher   grid max-pher bound"
  [world ant]
  (let [species (:species ant)
        home    (get-in world [:homes species])
        size    (get-in world [:grid :size])
        max-food (double (or (get-in world [:grid :max-food]) 5.0))
        max-pher (double (or (get-in world [:grid :max-pher]) 3.0))
        cells   (get-in world [:grid :cells])
        ants    (:ants world)]
    {:size     size
     :cells    cells
     :ants     ants
     :home     home
     :max-food max-food
     :max-pher max-pher}))

;; --------------------------------------------------------------------------- ;;
;; Pure helpers ported from war.clj — operating on local-view, not world.
;; No duplication: these are THE implementations; war.clj calls ant-kernel.
;; --------------------------------------------------------------------------- ;;

(defn- clamp-loc
  [[w h] [x y]]
  [(-> x (max 0) (min (dec w)))
   (-> y (max 0) (min (dec h)))])

(defn- neighbor-locs
  [[[w h] [x y]]]
  (for [dx [-1 0 1]
        dy [-1 0 1]
        :when (not (and (zero? dx) (zero? dy)))
        :let [nx (+ x dx)
              ny (+ y dy)]
        :when (and (<= 0 nx) (< nx w) (<= 0 ny) (< ny h))]
    [nx ny]))

(defn- cell-at
  [view loc]
  (get-in view [:cells loc] {:food 0.0 :pher 0.0 :ant nil :home nil}))

(defn- empty-neighbours
  [view loc]
  (->> (neighbor-locs [(:size view) loc])
       (filter #(nil? (:ant (cell-at view %))))))

(defn- step-toward
  [loc target]
  (let [[x y] loc
        [tx ty] target
        step (fn [a b] (cond (< a b) (inc a)
                             (> a b) (dec a)
                             :else a))]
    [(step x tx) (step y ty)]))

(defn- dist2
  [[x y] [hx hy]]
  (let [dx (- hx x)
        dy (- hy y)]
    (+ (* dx dx) (* dy dy))))

(defn- random-empty-neighbour
  [view loc rand-fn]
  (when-let [empties (seq (empty-neighbours view loc))]
    (rand-fn (vec empties))))

(defn- richest-neighbour
  "Pure version of war.clj's richest-neighbour.
   Additional args:
     visit-counts  the ant's visit-counts map (for least-visited fallback)
     rand-fn       injected RNG for random-empty-neighbour"
  ([view loc visit-counts rand-fn]
   (richest-neighbour view loc nil visit-counts rand-fn))
  ([view loc species visit-counts rand-fn]
   (richest-neighbour view loc species visit-counts rand-fn nil))
  ([view loc species visit-counts rand-fn {:keys [min-food] :or {min-food 0.1}}]
   (let [size (:size view)
         neighbours (neighbor-locs [size loc])
         home (when species (:home view))
         filtered (if (and home (seq neighbours))
                    (remove #(= % home) neighbours)
                    neighbours)
         candidates (seq filtered)
         best (when candidates
                (apply max-key #(:food (cell-at view %) 0.0) candidates))
         best-food (when best (double (or (:food (cell-at view best)) 0.0)))]
     (cond
       (and best (>= best-food min-food)) best
       (seq candidates)
       (let [visits visit-counts
             current-pher (double (or (:pher (cell-at view loc)) 0.0))
             pher-best (apply max-key #(:pher (cell-at view %) 0.0) candidates)
             pher-val (double (or (:pher (cell-at view pher-best)) 0.0))
             empties (vec (empty-neighbours view loc))
             least-visited (when (and visits (seq empties))
                             (apply min-key #(get visits % 0) empties))
             empties-pher (when (seq empties)
                            (apply max-key #(:pher (cell-at view %) 0.0) empties))]
         (or (when (> pher-val (+ current-pher 0.05)) pher-best)
             (when empties-pher
               (let [pher-empty (double (or (:pher (cell-at view empties-pher)) 0.0))]
                 (when (> pher-empty (+ current-pher 0.02)) empties-pher)))
             least-visited
             empties-pher
             best
             loc))
       :else (or (random-empty-neighbour view loc rand-fn)
                 best
                 loc)))))

(defn- attempt-move
  "Pure move on local-view.  Returns move-result with occupant info.
   Does NOT mutate view — the caller applies occupant changes as effects."
  [view ant target]
  (let [loc (:loc ant)
        size (:size view)
        target (clamp-loc size target)
        occupant-id (:ant (cell-at view target))]
    (cond
      (= target loc)
      {:loc loc :target target :moved? false :blocked? false}

      (nil? occupant-id)
      {:loc target :target target :moved? true :blocked? false
       :vacate loc :occupy target}

      (= occupant-id (:id ant))
      {:loc loc :target target :moved? false :blocked? false}

      :else
      (let [occupant (get-in view [:ants occupant-id])]
        (if (= (:species occupant) (:species ant))
          {:loc target :target target :moved? true :blocked? false :swapped? true
           :vacate loc :occupy target :swap-to loc :swap-id occupant-id}
          {:loc loc :target target :moved? false :blocked? true})))))

(defn- random-wander
  "Pure random wander on local-view.  rand-fn is injected."
  ([view ant rand-fn]
   (random-wander view ant {} rand-fn))
  ([view ant {:keys [prefer-home] :or {prefer-home false}} rand-fn]
   (let [loc (:loc ant)
         size (:size view)
         neighbours (neighbor-locs [size loc])
         empties (vec (empty-neighbours view loc))
         visits (:visit-counts ant)
         least-visited (when (and (seq empties) visits)
                         (let [min-count (apply min (map #(get visits % 0) empties))]
                           (some #(when (= (get visits % 0) min-count) %) empties)))
         target (cond
                  least-visited least-visited
                  (seq empties) (rand-fn empties)
                  prefer-home (:home view)
                  (seq neighbours) (rand-fn (vec neighbours))
                  :else loc)]
     (if target
       (-> (attempt-move view ant target)
           (assoc :wander? true))
       (let [streak (int (get ant :white-streak 0))
             target (if (>= streak 3)
                      (let [[x y] loc
                            dirs [[2 0] [-2 0] [0 2] [0 -2]]
                            candidates (->> dirs
                                            (map (fn [[dx dy]] [(+ x dx) (+ y dy)]))
                                            (map #(clamp-loc size %))
                                            (remove #(= % loc)))]
                        (when (seq candidates)
                          (nth candidates (mod streak (count candidates)) loc)))
                      nil)
             dest (or target loc)]
         (-> (attempt-move view ant dest)
             (assoc :wander? true)))))))

(defn- move-ant
  "Pure move on local-view with wander fallback.  rand-fn injected."
  ([view ant target rand-fn]
   (move-ant view ant target {} rand-fn))
  ([view ant target opts rand-fn]
   (let [{:keys [wander? wander-when-still? prefer-home?]
          :or {wander? true
               wander-when-still? false
               prefer-home? false}} opts
         attempt (attempt-move view ant target)]
     (cond
       (and wander? (:blocked? attempt))
       (or (random-wander view ant {:prefer-home prefer-home?} rand-fn)
           attempt)

       (and wander-when-still? (not (:moved? attempt)))
       (or (random-wander view ant {:prefer-home prefer-home?} rand-fn)
           attempt)

       :else
       attempt))))

(defn- home-directed-move
  "Pure home-directed move on local-view.  rand-fn injected."
  [view ant home rand-fn]
  (when home
    (let [loc (:loc ant)
          size (:size view)
          base (step-toward loc home)
          neighbours (neighbor-locs [size loc])
          candidates (->> (cons base neighbours)
                          (remove nil?)
                          (distinct)
                          (sort-by #(dist2 % home)))]
      (loop [[dest & more] candidates
             ant ant]
        (when dest
          (let [res (move-ant view ant dest {:wander? false
                                              :wander-when-still? false
                                              :prefer-home? true}
                              rand-fn)]
            (if (:moved? res)
              res
              (recur more ant))))))))

;; --------------------------------------------------------------------------- ;;
;; Pure gather / deposit / pheromone / ingest (operate on local-view)
;; --------------------------------------------------------------------------- ;;

(defn- gather-food
  "Pure food gather.  Returns updated ant + food-delta for the current cell."
  [view ant]
  (let [loc (:loc ant)
        species (:species ant)
        home (:home view)
        home-cell? (and home (= loc home))
        cell (cell-at view loc)
        friendly-home? (and home-cell?
                            (= (:home cell) species))
        capacity (- 1.0 (:cargo ant))
        available (if friendly-home?
                    0.0
                    (:food cell 0.0))
        min-take 0.15
        proposed (min capacity (min available 0.7))
        take (if (>= proposed min-take) proposed 0.0)
        ant (if (pos? take)
              (-> ant
                  (update :cargo + take)
                  (assoc :white-streak 0))
              (-> ant
                  (update :white-streak (fnil inc 0))))]
    {:ant ant
     :gather take
     :food-delta (when (pos? take) {loc (- take)})}))

(defn- deposit-food
  "Pure food deposit at home.  Returns updated ant + score/reserve deltas."
  [view ant]
  (let [home (:home view)
        loc (:loc ant)
        dist-home (if home (dist2 loc home) Double/POSITIVE_INFINITY)
        ;; Snap to home if within range (matches war.clj deposit-food)
        loc (if (and home (> dist-home 0) (<= dist-home 4)) home loc)
        at-home? (= loc home)
        cargo (:cargo ant)]
    (if (and at-home? (pos? cargo))
      {:ant (assoc ant :loc loc :cargo 0.0)
       :deposit cargo
       :score-delta cargo
       :reserve-delta cargo}
      {:ant (assoc ant :loc loc)
       :deposit 0.0})))

(defn- pheromone-drops
  "Compute pheromone deltas for a pheromone action.  Returns {loc amount}."
  [ant amount target]
  (let [loc (:loc ant)
        deposit (double (max 0.0 amount))
        unit (if (pos? deposit) (/ deposit 3.0) 0.0)
        ahead (when (and target loc)
                (let [[x y] loc
                      [tx ty] target
                      dx (- tx x)
                      dy (- ty y)]
                  [(+ tx dx) (+ ty dy)]))]
    (cond-> {}
      (pos? unit) (assoc loc unit)
      (and target (pos? unit)) (assoc target (* 2.0 unit))
      (and ahead (pos? unit)) (assoc ahead unit))))

(defn- adjust-ingest
  [ant {:keys [add decay] :or {add 0.0 decay 0.55}}]
  (let [prior (double (or (:ingest ant) 0.0))
        decay (double (observe/clamp01 decay))
        add (double (max 0.0 add))
        next (+ (* decay prior) add)
        ingest (observe/clamp01 next)]
    (assoc ant :ingest ingest)))

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

(defn- predict-hunger
  "Simplified local hunger model for the prediction path (generative belief).

   h' = clamp01(h + burn + load_pressure*cargo - feed*food - rest*home_prox)

   This mirrors affect/tick-hunger's structure but uses only locally-observable
   quantities (no colony/queen state). The divergence between this belief-model
   and the live step's colony-coupled adjust-hunger is the R4-legitimate
   prediction error that F measures."
  [ant view]
  (let [h (double (or (:h ant) 0.5))
        cargo (double (or (:cargo ant) 0.0))
        loc (:loc ant)
        cell (cell-at view loc)
        food (double (or (:food cell) 0.0))
        home (:home view)
        [w h-size] (:size view)
        max-dist (let [w-d (max 1 (dec (or w 1)))
                       h-d (max 1 (dec (or h-size 1)))]
                   (Math/sqrt (+ (* w-d w-d) (* h-d h-d))))
        home-prox (if home
                    (let [[x y] loc
                          [hx hy] home
                          dist (Math/sqrt (+ (* (- hx x) (- hx x))
                                             (* (- hy y) (- hy y))))]
                      (observe/clamp01 (- 1.0 (/ dist (max 1e-9 max-dist)))))
                    0.0)
        burn 0.02
        feed 0.05
        rest 0.03
        load-pressure 0.25
        delta (+ burn (* load-pressure cargo) (- (* feed food)) (- (* rest home-prox)))
        h' (observe/clamp01 (+ h delta))]
    (assoc ant :h h')))

(defn- finalize-ant
  "Apply post-kernel transforms: visit-count increment + hunger belief model.
   Used by forward-predict (the prediction path); the live step applies these
   via war.clj's track-visit + adjust-hunger."
  [ant view]
  (-> ant
      (update-in [:visit-counts (:loc ant)] (fnil inc 0))
      (predict-hunger view)))

;; --------------------------------------------------------------------------- ;;
;; ant-kernel: the pure single-ant transition.
;; --------------------------------------------------------------------------- ;;

(defn ant-kernel
  "Pure single-ant local transition under a macro-action.

   Args:
     view     local-view (from `local-view`)
     ant      the ant state map
     action   one of :forage :return :hold :pheromone :dead
     opts     {:rand-fn     injected RNG (defaults to clojure.core/rand-nth)
               :mu-goal     goal loc for :pheromone (defaults to view :home)}

   Returns:
     {:ant        next-ant-state
      :effects    {:food-deltas  {loc delta}   (negative = consumed)
                   :pher-deltas  {loc delta}   (positive = dropped)
                   :score-delta  double         (deposit at home)
                   :reserve-delta double        (reserve increase)
                   :moved?       bool
                   :wander?      bool
                   :gather       double
                   :deposit      double
                   :ingest       double
                   :target       loc
                   :dead?        bool}}

   Pure: no atoms, no logging, no world mutation.  Deterministic given the same
   view + ant + action + rand-fn."
  ([view ant action]
   (ant-kernel view ant action {}))
  ([view ant action {:keys [rand-fn mu-goal] :or {rand-fn rand-nth} :as _opts}]
   (let [visit-counts (:visit-counts ant)]
     (case action
       :dead
       {:ant ant
        :effects {:moved? false :wander? false :gather 0.0 :deposit 0.0
                  :ingest (:ingest ant 0.0) :dead? true :target (:loc ant)}}

       :forage
       (let [best (richest-neighbour view (:loc ant) (:species ant)
                                     visit-counts rand-fn)
             move-res (move-ant view ant best {:wander-when-still? true} rand-fn)
             ant-moved (assoc ant :loc (:loc move-res))
             gather-res (gather-food view ant-moved)
             ant-gathered (:ant gather-res)
             g (double (:gather gather-res 0.0))
             ant-ingested (adjust-ingest ant-gathered {:add g :decay 0.6})
             ant-final (blend-recent-gather ant-ingested (min 1.0 g))]
         {:ant ant-final
          :effects {:moved? (:moved? move-res false)
                    :wander? (:wander? move-res false)
                    :gather g
                    :deposit 0.0
                    :ingest (:ingest ant-final 0.0)
                    :dead? false
                    :target (:target move-res (:loc ant-final))
                    :food-deltas (:food-delta gather-res)
                    :occupant (when (:moved? move-res)
                                (select-keys move-res [:vacate :occupy :swap-to :swap-id]))}})

       :return
       (let [home (:home view)
             primary (home-directed-move view ant home rand-fn)
             target (step-toward (:loc ant) home)
             direct (when-not (:moved? primary)
                      (move-ant view ant target
                                {:wander-when-still? true :prefer-home? true}
                                rand-fn))
             wander (when (and (not (:moved? direct)) (not (:moved? primary)))
                      (random-wander view ant {:prefer-home true} rand-fn))
             move-result (or (when (:moved? primary) primary)
                             (when (:moved? direct) direct)
                             (when (:moved? wander) wander)
                             (or wander direct primary)
                             {:loc (:loc ant) :target (:loc ant) :moved? false :wander? false})
             ant-moved (assoc ant :loc (:loc move-result))
             dep-res (deposit-food view ant-moved)
             ant-deposited (:ant dep-res)
             d (double (:deposit dep-res 0.0))
             ant-ingested (adjust-ingest ant-deposited {:add 0.0 :decay 0.55})
             ant-final (decay-recent-gather ant-ingested)]
         {:ant ant-final
          :effects {:moved? (:moved? move-result false)
                    :wander? (:wander? move-result false)
                    :gather 0.0
                    :deposit d
                    :ingest (:ingest ant-final 0.0)
                    :dead? false
                    :target (:target move-result (:loc ant-final))
                    :score-delta (:score-delta dep-res 0.0)
                    :reserve-delta (:reserve-delta dep-res 0.0)
                    :occupant (when (:moved? move-result)
                                (select-keys move-result [:vacate :occupy :swap-to :swap-id]))}})

       :hold
       (let [home (:home view)
             at-home? (= (:loc ant) home)
             move-res (if (not at-home?)
                        (move-ant view ant (step-toward (:loc ant) home) {} rand-fn)
                        (move-ant view ant (:loc ant)
                                  {:wander? true :wander-when-still? true} rand-fn))
             ant-moved (assoc ant :loc (:loc move-res))
             ant-ingested (adjust-ingest ant-moved {:add 0.0 :decay 0.5})
             ant-final (decay-recent-gather ant-ingested)]
         {:ant ant-final
          :effects {:moved? (:moved? move-res false)
                    :wander? (:wander? move-res false)
                    :gather 0.0
                    :deposit 0.0
                    :ingest (:ingest ant-final 0.0)
                    :dead? false
                    :target (:target move-res (:loc ant-final))
                    :occupant (when (:moved? move-res)
                                (select-keys move-res [:vacate :occupy :swap-to :swap-id]))}})

       :pheromone
       (let [home (:home view)
             goal (or mu-goal home)
             target (step-toward (:loc ant) goal)
             cargo (double (or (:cargo ant) 0.0))
             base-dep (+ 0.25 (* 0.85 cargo))
             pher-deltas (pheromone-drops ant base-dep target)
             move-res (move-ant view ant target {} rand-fn)
             ant-moved (assoc ant :loc (:loc move-res))
             ant-ingested (adjust-ingest ant-moved {:add 0.0 :decay 0.45})
             ant-final (decay-recent-gather ant-ingested)]
         {:ant ant-final
          :effects {:moved? (:moved? move-res false)
                    :wander? (:wander? move-res false)
                    :gather 0.0
                    :deposit 0.0
                    :ingest (:ingest ant-final 0.0)
                    :dead? false
                    :target (:target move-res (:loc ant-final))
                    :pher-deltas pher-deltas
                    :occupant (when (:moved? move-res)
                                (select-keys move-res [:vacate :occupy :swap-to :swap-id]))}})

       ;; default: hold
       (let [home (:home view)
             at-home? (= (:loc ant) home)
             move-res (if (not at-home?)
                        (move-ant view ant (step-toward (:loc ant) home) {} rand-fn)
                        (move-ant view ant (:loc ant)
                                  {:wander? true :wander-when-still? true} rand-fn))
             ant-moved (assoc ant :loc (:loc move-res))
             ant-ingested (adjust-ingest ant-moved {:add 0.0 :decay 0.5})
             ant-final (decay-recent-gather ant-ingested)]
         {:ant ant-final
          :effects {:moved? (:moved? move-res false)
                    :wander? (:wander? move-res false)
                    :gather 0.0
                    :deposit 0.0
                    :ingest (:ingest ant-final 0.0)
                    :dead? false
                    :target (:target move-res (:loc ant-final))
                    :occupant (when (:moved? move-res)
                                (select-keys move-res [:vacate :occupy :swap-to :swap-id]))}})))))

;; --------------------------------------------------------------------------- ;;
;; forward-predict: call the kernel + add principled noise → distribution.
;; --------------------------------------------------------------------------- ;;

(defn- seeded-rand-fn
  "Create a deterministic rand-fn from a seed. Returns a function that picks
   a random element from a collection (same interface as rand-nth)."
  [seed]
  (let [rng (java.util.Random. (long seed))]
    (fn [coll]
      (let [v (vec coll)
            n (count v)]
        (if (pos? n)
          (v (.nextInt rng n))
          nil)))))

(def ^:private default-predict-noise
  "Per-channel noise stdev for forward-predict v1."
  {:food 0.05 :pher 0.05 :cargo 0.03 :ingest 0.05})

(defn forward-predict
  "Predict the next ant-state distribution by calling ant-kernel + adding
   principled per-channel Gaussian noise.

   Returns:
     {:mean     next-ant-state (deterministic kernel output)
      :variance {channel sigma2}  (per-channel prediction variance)}

   The mean equals the deterministic kernel next-state exactly (acceptance 3).
   The variance is a simple per-channel Gaussian noise model — adequate for v1."
  ([view ant action]
   (forward-predict view ant action {}))
  ([view ant action {:keys [seed noise] :or {seed 42} :as opts}]
   (let [rand-fn (if seed (seeded-rand-fn seed) rand-nth)
         kernel-opts (-> opts
                         (assoc :rand-fn rand-fn)
                         (select-keys [:rand-fn :mu-goal]))
         result (ant-kernel view ant action kernel-opts)
         next-ant (finalize-ant (:ant result) view)
         noise-map (or noise default-predict-noise)
         variance (into {}
                        (for [[ch sd] noise-map]
                          [ch (* (double sd) (double sd))]))]
     {:mean next-ant
      :variance variance
      :effects (:effects result)})))
