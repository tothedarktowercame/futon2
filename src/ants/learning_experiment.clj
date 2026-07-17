(ns ants.learning-experiment
  "Deterministic river POC for the enactment-and-review ant brain."
  (:require [ants.war :as war]))

(def poc-config
  {:size [20 12]
   :food-max 5.0
   :food-distribution :snowdrift
   :water {:river {:axis :vertical :start 8 :width 2}
           :food 8.0}
   :ants-per-side 1
   :ticks 160
   :enable-termination? false
   :hunger {:initial 0.2
            :metabolic-rate 0.0
            :death-threshold 2.0
            :queen {:initial 100.0
                    :burn 0.0
                    :per-ant 0.0
                    :starvation-grace 1000
                    :starvation-boost 0.0}}})

(defn- seeded-rand-nth
  [seed]
  (let [rng (java.util.Random. (long seed))]
    (fn [coll]
      (let [items (vec coll)]
        (when (seq items)
          (nth items (.nextInt rng (count items))))))))

(defn- add-river-corridor
  "Give both brains the same sparse trail that actually reaches the hazard.

  Classic ants otherwise harvest the dense default snowdrift around home and
  never encounter a river.  The corridor is test-fixture world structure, not
  a learning hint: every dry breadcrumb has the same small food amount and the
  water remains the locally richest cell."
  [world]
  (update-in world [:grid :cells]
             (fn [cells]
               (into {}
                     (map (fn [[[x y :as loc] cell]]
                            [loc (assoc cell :food
                                        (cond
                                          (= :water (:terrain cell)) 8.0
                                          ;; Below the kernel's 0.15 gather
                                          ;; threshold, so breadcrumbs guide
                                          ;; movement without trapping ants in
                                          ;; a locally replenished food cell.
                                          (and (<= 3 x 7) (<= 1 y 5))
                                          (+ 0.07 (* 0.01 x))
                                          :else 0.0))])
                          cells)))))

(defn- empty-window
  [start]
  {:start-tick start
   :end-tick start
   :actions 0
   :water-edges 0
   :water-deaths 0})

(defn- add-events
  [window events tick]
  (let [events (vec events)]
    (-> window
        (assoc :end-tick tick)
        (update :actions + (count events))
        (update :water-edges +
                (count (filter #(= :at-water-edge (:policy-bin %)) events)))
        (update :water-deaths +
                (count (filter :water-death events))))))

(defn- finish-window
  [{:keys [actions water-edges water-deaths] :as window}]
  (assoc window
         :water-death-rate (if (pos? actions)
                             (/ (double water-deaths) actions)
                             0.0)
         :edge-death-rate (if (pos? water-edges)
                            (/ (double water-deaths) water-edges)
                            0.0)))

(defn run-colony
  "Run one brain on the common river map and return windowed drowning rates."
  ([brain]
   (run-colony brain {}))
  ([brain {:keys [ticks window-size seed]
           :or {ticks 160 window-size 20 seed 4242}}]
   (let [config (assoc poc-config :armies [brain])
         initial (-> (war/new-world config)
                     add-river-corridor
                     (assoc :rand-fn (seeded-rand-nth seed)))]
     (loop [world initial
            remaining ticks
            current (empty-window 1)
            windows []]
       (if (zero? remaining)
         (let [windows (cond-> windows
                         (pos? (:actions current)) (conj (finish-window current)))
               ants (->> (:ants world)
                         vals
                         (filter #(= brain (:species %)))
                         (sort-by :id))]
           {:brain brain
            :world world
            :rates windows
            :totals (finish-window
                     (reduce (fn [acc window]
                               (-> acc
                                   (update :actions + (:actions window))
                                   (update :water-edges + (:water-edges window))
                                   (update :water-deaths + (:water-deaths window))))
                             (empty-window 1)
                             windows))
            :learned-cascades
            (mapv (fn [ant]
                    {:id (:id ant)
                     :at-water-edge (get-in ant [:learning-policy :at-water-edge])
                     :reviews (:learning-reviews ant)
                     :revisions (->> (:learning-revisions ant)
                                     (filter #(= :at-water-edge (:bin %)))
                                     (take 3)
                                     vec)})
                  (take 3 ants))})
         (let [world' (war/step world)
               tick (:tick world')
               events (filter #(= brain (:species %)) (:last-events world'))
               current' (add-events current events tick)]
           (if (zero? (mod tick window-size))
             (recur world'
                    (dec remaining)
                    (empty-window (inc tick))
                    (conj windows (finish-window current')))
             (recur world' (dec remaining) current' windows))))))))

(defn run-poc
  "Run fixed and learning colonies on identical maps and random seeds."
  ([]
   (run-poc {}))
  ([opts]
   (let [classic (run-colony :classic opts)
         learning (run-colony :learning opts)
         seed (long (or (:seed opts) 4242))
         cascade-samples (->> [learning
                               (run-colony :learning (assoc opts :seed (inc seed)))
                               (run-colony :learning (assoc opts :seed (+ seed 2)))]
                              (map-indexed
                               (fn [replicate run]
                                 (assoc (first (:learned-cascades run))
                                        :replicate replicate)))
                              vec)
         classic-rate (get-in classic [:totals :water-death-rate])
         learning-rate (get-in learning [:totals :water-death-rate])]
     {:classic classic
      :learning learning
      :learned-cascade-samples cascade-samples
      :learning-beat-fixed? (< learning-rate classic-rate)})))

(defn- printable-rates
  [run]
  (mapv #(select-keys % [:start-tick :end-tick :water-deaths
                         :water-death-rate :edge-death-rate])
        (:rates run)))

(defn -main
  [& _]
  (let [{:keys [classic learning learned-cascade-samples
                learning-beat-fixed?]} (run-poc)]
    (println "classic water-death-rate-over-time"
             (pr-str (printable-rates classic)))
    (println "learning water-death-rate-over-time"
             (pr-str (printable-rates learning)))
    (println "learning beat fixed" learning-beat-fixed?)
    (println "learned cascades" (pr-str learned-cascade-samples))))
