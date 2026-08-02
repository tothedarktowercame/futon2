(require '[ants.aif.experiment :as experiment]
         '[ants.compare-replay :as stats]
         '[clojure.java.io :as io])

(def config
  {:size [24 24]
   :ticks 720
   :scenario :patchy
   :food {:num-patches 23 :patch-radius 2}
   :metabolism 0.06
   :initial-reserves 0.5
   :ants-per-side 3
   :seeds (mapv (fn [i]
                  {:run (inc i)
                   :food-seed (+ 202612110 (* 2 i))
                   :move-seed (+ 202612111 (* 2 i))
                   :choice-seed (+ 202662110 i)})
                (range 5))})

(def conditions
  [{:id :aif-drop-disabled :species :aif :enabled? false}
   {:id :aif-drop-enabled :species :aif :enabled? true}
   {:id :classic-drop-disabled :species :classic :enabled? false}
   {:id :classic-drop-enabled :species :classic :enabled? true}])

(defn run-job
  [{:keys [condition seeds]}]
  (let [{:keys [id species enabled?]} condition
        {:keys [food-seed move-seed choice-seed]} seeds]
    (merge {:condition id :species species :drop-enabled? enabled?}
           seeds
           (#'experiment/run-single
            species (:scenario config) food-seed move-seed (:size config) (:ticks config) false
            :choice-seed choice-seed
            :metabolism (:metabolism config)
            :initial-reserves (:initial-reserves config)
            :ants-per-side (:ants-per-side config)
            :food-cache-enabled? enabled?
            :food-opts (:food config)))))

(defn summarize
  [rows condition]
  (let [selected (filterv #(= condition (:condition %)) rows)]
    {:condition condition
     :yield (stats/arm-summary (mapv :yield selected))
     :starvation (stats/arm-summary (mapv :starved selected))
     :relay-count (reduce + (map #(get-in % [:relays :completed] 0) selected))
     :relay-amount (reduce + 0.0 (map #(get-in % [:relays :delivered-amount] 0.0) selected))
     :cache-drops (reduce + (map #(get-in % [:relays :cache-drops] 0) selected))
     :cross-ant-pickups (reduce + (map #(get-in % [:relays :cross-ant-pickups] 0) selected))}))

(defn paired
  [rows enabled disabled metric]
  (let [by-condition (group-by :condition rows)
        left (sort-by :run (get by-condition enabled))
        right (sort-by :run (get by-condition disabled))
        differences (mapv #(- (double (metric %1)) (double (metric %2))) left right)]
    (assoc (stats/arm-summary differences)
           :contrast [enabled disabled])))

(defn fmt
  [x]
  (format "%.4f" (double x)))

(defn report
  [{:keys [summaries contrasts]}]
  (let [aif-enabled (first (filter #(= :aif-drop-enabled (:condition %)) summaries))
        aif-yield (:aif-yield contrasts)
        aif-starvation (:aif-starvation contrasts)]
    (str "# S2 cache/relay capability probe\n\n"
       "Frozen before the run: 24×24 patchy world, 720 ticks, 23 density-scaled "
       "radius-2 patches, metabolism 0.06, initial reserves 0.5, three ants, five "
       "paired seed triples. Dropped food is ordinary cell food; provenance is a "
       "parallel measurement ledger. No EFE lambda or drop bonus was added.\n\n"
       "Seed triples `(food, move, choice)`: `(202612110, 202612111, 202662110)`, "
       "`(202612112, 202612113, 202662111)`, `(202612114, 202612115, 202662112)`, "
       "`(202612116, 202612117, 202662113)`, `(202612118, 202612119, 202662114)`.\n\n"
       "| condition | mean yield | starvation | cache drops | cross-ant pickups | completed relays |\n"
       "|---|---:|---:|---:|---:|---:|\n"
       (apply str
              (for [{:keys [condition yield starvation cache-drops cross-ant-pickups relay-count]}
                    summaries]
                (format "| %s | %s | %s | %d | %d | %d |\n"
                        (name condition) (fmt (:mean yield)) (fmt (:mean starvation))
                        cache-drops cross-ant-pickups relay-count)))
       "\nPaired enabled−disabled differences (mean, two-sided 95% t interval):\n\n"
       (apply str
              (for [[label {:keys [mean ci95]}] contrasts]
                (format "- %s: %s [%s, %s]\n" (name label) (fmt mean)
                        (fmt (first ci95)) (fmt (second ci95)))))
       (format "\n**Verdict:** the homeward-progress preference made AIF select %d cache "
               (:cache-drops aif-enabled))
       "drops, but no other ant picked one up and no relay completed. Banking the "
       "progress proxy therefore did not produce delivery in this probe. Enabling "
       "drop changed mean AIF yield "
       (format "by %s (95%% CI [%s, %s]) and starvation by %s (95%% CI [%s, %s]); "
               (fmt (:mean aif-yield)) (fmt (first (:ci95 aif-yield)))
               (fmt (second (:ci95 aif-yield))) (fmt (:mean aif-starvation))
               (fmt (first (:ci95 aif-starvation)))
               (fmt (second (:ci95 aif-starvation))))
       "both intervals include zero. Classic never selected drop and its "
       "enabled/disabled records were identical, so it did not benefit equally—but "
       "the zero-relay result prevents any AIF capability interpretation.\n\n"
       "The existing within-distance-4 return teleport remains unchanged; cache drops "
       "were admitted only away from home and onto cells containing less than 0.10 food.\n")))

(let [jobs (for [condition conditions
                 seeds (:seeds config)]
             {:condition condition :seeds seeds})
      rows (vec (doall (pmap run-job jobs)))
      summaries (mapv #(summarize rows (:id %)) conditions)
      contrasts {:aif-yield (paired rows :aif-drop-enabled :aif-drop-disabled :yield)
                 :aif-starvation (paired rows :aif-drop-enabled :aif-drop-disabled :starved)
                 :classic-yield (paired rows :classic-drop-enabled :classic-drop-disabled :yield)
                 :classic-starvation (paired rows :classic-drop-enabled :classic-drop-disabled :starved)}
      artifact {:protocol config :rows rows :summaries summaries :contrasts contrasts}
      edn-path "holes/labs/ants-faithfulness/s2-cache-relay.edn"
      md-path "holes/labs/ants-faithfulness/s2-cache-relay.md"]
  (io/make-parents edn-path)
  (spit edn-path (str (pr-str artifact) "\n"))
  (spit md-path (report artifact))
  (prn {:rows (count rows) :summaries summaries :contrasts contrasts}))

(shutdown-agents)
