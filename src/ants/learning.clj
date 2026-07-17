(ns ants.learning
  "A small, per-ant policy cascade revised by enactment and review.

  The policy is deliberately local data, not a fitted model.  Each ant starts
  from the classic rule ordering expressed as bins.  Successful entries are
  held; a failed entry alone is revised to a different action.")

(def actions
  "The intentionally small action vocabulary of the learning brain."
  [:forage :return :pheromone :hold :turn-back])

(def default-cascade
  "Classic policy represented as data, plus the water-edge test bin.

  `:at-water-edge` starts with `:forage`: on the POC river map the water cell
  is food-rich, so enacting this inherited foraging response supplies the
  failure that review can fold back into the cascade."
  {:has-cargo :return
   :food-here :forage
   :food-nearby :forage
   :at-water-edge :forage
   :else :pheromone})

(defn ensure-state
  "Attach an independent cascade and review counters when absent."
  [ant]
  (cond-> ant
    (nil? (:learning-policy ant))
    (assoc :learning-policy default-cascade
           :learning-reviews {:success 0 :failure 0}
           :learning-revisions [])))

(defn- neighbour-locs
  [[[w h] [x y]]]
  (for [dx [-1 0 1]
        dy [-1 0 1]
        :when (not (and (zero? dx) (zero? dy)))
        :let [nx (+ x dx)
              ny (+ y dy)]
        :when (and (<= 0 nx) (< nx w) (<= 0 ny) (< ny h))]
    [nx ny]))

(defn policy-bin
  "Derive the first applicable condition bin from the ant and local world.

  Water takes priority because it is an immediate local hazard.  The other
  bins preserve `classic-policy`'s cargo/food/fallback structure."
  [world ant]
  (let [loc (:loc ant)
        size (get-in world [:grid :size])
        neighbours (neighbour-locs [size loc])
        food-here (double (or (get-in world [:grid :cells loc :food]) 0.0))
        food-nearby? (some #(> (double (or (get-in world [:grid :cells % :food]) 0.0))
                               food-here)
                           neighbours)
        water-edge? (some #(= :water (get-in world [:grid :cells % :terrain]))
                          neighbours)]
    (cond
      water-edge? :at-water-edge
      (> (double (or (:cargo ant) 0.0)) 0.2) :has-cargo
      (> food-here 0.2) :food-here
      food-nearby? :food-nearby
      :else :else)))

(defn choose-action
  "Return the action selected by this ant's current cascade."
  [ant bin]
  (get (:learning-policy (ensure-state ant)) bin :hold))

(defn- next-action
  [bin action]
  (if (= bin :at-water-edge)
    :turn-back
    (let [idx (.indexOf ^java.util.List actions action)]
      (nth actions (mod (inc (max -1 idx)) (count actions))))))

(defn review
  "Review one enacted action and return the revised (or held) ant plus signal.

  Progress is deliberately ant-simple: gathering/depositing, lower hunger, or
  a safe move succeeds.  Water, starvation, or no progress fails.  Failure
  changes only the active bin; success preserves it (the Baldwin gate)."
  [ant-before ant-after event]
  (let [ant-after (ensure-state ant-after)
        bin (:policy-bin event)
        action (:action event)
        cargo-before (double (or (:cargo ant-before) 0.0))
        cargo-after (double (or (:cargo ant-after) 0.0))
        hunger-before (double (or (:h ant-before) 0.0))
        hunger-after (double (or (:h ant-after) hunger-before))
        productive? (or (> (double (or (:gather event) 0.0)) 0.0)
                        (> (double (or (:deposit event) 0.0)) 0.0)
                        (> cargo-after cargo-before)
                        (< hunger-after hunger-before))
        safe-move? (and (:moved event) (not (:water-death event)))
        failure? (or (:water-death event)
                     (= :starvation (:cause event))
                     (not (or productive? safe-move?)))
        signal (if failure? :failure :success)
        replacement (when failure? (next-action bin action))
        failure-cause (cond
                        (:water-death event) :water
                        (= :starvation (:cause event)) :starvation
                        :else :zero-progress)
        ant' (-> ant-after
                 (update-in [:learning-reviews signal] (fnil inc 0))
                 (cond-> failure?
                   (assoc-in [:learning-policy bin] replacement)
                   failure?
                   (update :learning-revisions (fnil conj [])
                           {:tick (:tick event)
                           :bin bin
                           :from action
                           :to replacement
                            :cause failure-cause})))]
    {:ant ant'
     :review signal
     :replacement replacement}))
