(require '[ants.aif.experiment :as experiment]
         '[ants.war :as war])

;; Diagnostic read of the live selector, not an experimental comparison.
;; Fixed before the run: one patchy 10x10 world, 300 ticks, the first seed from
;; the density-scaling probe's 10x10 cell, and the same frozen environment.
(def config
  {:food-seed 202621110
   :move-seed 202621111
   :choice-seed 202671110
   :size [10 10]
   :ticks 300
   :metabolism 0.06
   :initial-reserves 0.5
   :ants-per-side 3
   :food-opts {:num-patches 4 :patch-radius 2}})

(defn spread
  [xs]
  (if (seq xs)
    (- (apply max xs) (apply min xs))
    0.0))

(defn decision-summary
  [{:keys [winner candidates]}]
  (let [winner-candidate (first (filter #(= winner (:action %)) candidates))
        runners (remove #(= winner (:action %)) candidates)
        runner-up (when (seq runners) (apply min-key :G runners))
        ;; Selection minimizes G. The requested winner-minus-runner value is
        ;; negative; :selection-margin is its positive, ratio-usable inverse.
        signed-margin (if runner-up
                        (- (:G winner-candidate) (:G runner-up))
                        0.0)
        selection-margin (- signed-margin)]
    {:risk (spread (map #(get-in % [:weighted :risk]) candidates))
     :ambiguity (spread (map #(get-in % [:weighted :ambiguity]) candidates))
     :epistemic (spread (map #(get-in % [:weighted :epistemic]) candidates))
     :info (spread (map #(get-in % [:weighted :info]) candidates))
     :signed-margin signed-margin
     :selection-margin selection-margin}))

(defn percentile
  [xs p]
  (let [v (vec (sort xs))
        i (long (Math/floor (* (double p) (dec (count v)))))]
    (nth v i)))

(defn distribution
  [xs]
  (let [xs (vec (map double xs))]
    {:n (count xs)
     :min (apply min xs)
     :p25 (percentile xs 0.25)
     :median (percentile xs 0.50)
     :p75 (percentile xs 0.75)
     :p90 (percentile xs 0.90)
     :p95 (percentile xs 0.95)
     :p99 (percentile xs 0.99)
     :max (apply max xs)
     :mean (/ (reduce + xs) (double (count xs)))}))

(let [{:keys [food-seed move-seed choice-seed size ticks metabolism
              initial-reserves ants-per-side food-opts]} config
      decisions (atom [])
      world (-> (experiment/make-seeded-world
                 :aif :patchy food-seed move-seed size ticks
                 :metabolism metabolism
                 :initial-reserves initial-reserves
                 :ants-per-side ants-per-side
                 :choice-seed choice-seed
                 :food-opts food-opts)
                (assoc :aif-decision-log-fn #(swap! decisions conj %)))]
  (loop [world world
         tick 0]
    (if (< tick ticks)
      (recur (war/step world) (inc tick))
      (let [rows (mapv decision-summary @decisions)
            positive-margin (filterv #(pos? (:selection-margin %)) rows)
            ratios (fn [component]
                     (mapv #(/ (component %) (:selection-margin %)) positive-margin))]
        (prn {:config config
              :decisions (count rows)
              :zero-margin-decisions (count (remove #(pos? (:selection-margin %)) rows))
              :spread-distributions
              (into {}
                    (for [component [:risk :ambiguity :epistemic :info]]
                      [component (distribution (map component rows))]))
              :margin-distribution (distribution (map :selection-margin rows))
              :signed-margin-distribution (distribution (map :signed-margin rows))
              :ratio-distributions
              {:ambiguity-over-margin (distribution (ratios :ambiguity))
               :epistemic-over-margin (distribution (ratios :epistemic))}})))))
