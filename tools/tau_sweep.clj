(ns tools.tau-sweep
  "Batch sweeps for the AIF tau coupling parameters."
  (:require [ants.war :as war]
            [clojure.string :as str]))

(def ^:private parameter-grid
  "Baseline parameter grid explored on each sweep iteration."
  (for [tau-reserve  [0.4 0.6 0.8]
        tau-survival [0.4 0.6 0.8]
        pressure     [1.0 1.4]]
    {:tau-reserve tau-reserve
     :tau-survival tau-survival
     :pressure pressure}))

(def ^:private default-ticks 800)

(defn- quiet-simulate
  [config]
  (with-redefs [war/log-alice (fn [_] nil)
                war/termination-message (fn [& _] nil)]
    (war/simulate config {:hud? false})))

(defn- collect-metrics
  [world]
  (let [scores (:scores world)
        ants   (vals (:ants world))
        taus   (keep #(get-in % [:prec :tau]) ants)
        stats  (get-in world [:stats :aif] {})
        actions-total (double (or (:actions-total stats) 0))
        pher-count (double (or (:pheromone-count stats) 0))
        pher-rate (if (pos? actions-total) (/ pher-count actions-total) 0.0)
        pher-trail-samples (double (or (:pheromone-trail-samples stats) 0))
        pher-trail-sum (double (or (:pheromone-trail-sum stats) 0.0))
        pher-trail-avg (if (pos? pher-trail-samples)
                         (/ pher-trail-sum pher-trail-samples)
                         0.0)]
    {:aif-score     (double (:aif scores))
     :classic-score (double (:classic scores))
     :gap           (- (double (:aif scores)) (double (:classic scores)))
     :reserve       (double (get-in world [:colonies :aif :reserves] 0.0))
     :tau-avg       (if (seq taus) (/ (reduce + taus) (double (count taus))) 0.0)
     :tau-min       (if (seq taus) (apply min taus) 0.0)
     :tau-max       (if (seq taus) (apply max taus) 0.0)
     :termination   (or (get-in world [:termination :reason]) :ticks)
     :actions-total actions-total
     :pheromone-count pher-count
     :pheromone-rate pher-rate
     :pheromone-trail-avg pher-trail-avg}))

(defn- next-suggestion
  "Heuristic suggestion for the next probe around PARAMS based on METRICS."
  [{:keys [tau-reserve tau-survival pressure]} {:keys [gap reserve]}]
  (cond
    (> gap 15.0)
    {:tau-reserve (* 0.85 tau-reserve)
     :tau-survival (* 0.85 tau-survival)
     :pressure (+ pressure 0.2)}

    (< gap -5.0)
    {:tau-reserve (* 1.15 tau-reserve)
     :tau-survival (* 1.15 tau-survival)
     :pressure (max 0.8 (- pressure 0.2))}

    (< reserve 20.0)
    {:tau-reserve (* 1.1 tau-reserve)
     :tau-survival tau-survival
     :pressure (max 0.8 (- pressure 0.2))}

    :else
    {:tau-reserve tau-reserve
     :tau-survival tau-survival
     :pressure pressure}))

(defn- run-configuration
  [ticks params]
  (let [config {:ticks ticks
                :aif {:precision {:tau-reserve-gain (:tau-reserve params)
                                   :tau-survival-gain (:tau-survival params)}
                      :efe {:survival {:pressure-norm (:pressure params)}}}}
        world   (quiet-simulate config)
        metrics (collect-metrics world)]
    (merge params
           metrics
           {:suggestion (next-suggestion params metrics)})))

(defn- format-result
  [{:keys [tau-reserve tau-survival pressure
           aif-score classic-score gap reserve
           tau-avg tau-min tau-max termination suggestion
           actions-total pheromone-count pheromone-rate pheromone-trail-avg]
    :as _result}
   iteration]
  (format "iter=%02d params=%s | AIF %.2f vs Classic %.2f (Δ=%.2f) | reserve=%.2f | tau_avg=%.3f range=[%.3f %.3f] | pher_rate=%.3f trail=%.3f (cnt=%d/%d) | term=%s | next=%s"
          iteration
          (pr-str {:tau-reserve tau-reserve
                   :tau-survival tau-survival
                   :pressure pressure})
          aif-score
          classic-score
          gap
          reserve
          tau-avg
          tau-min
          tau-max
          pheromone-rate
          pheromone-trail-avg
          (long pheromone-count)
          (long actions-total)
          termination
          (pr-str suggestion)))

(defn- merge-summary
  [acc {:keys [gap reserve tau-avg tau-min tau-max termination suggestion
               pheromone-rate pheromone-trail-avg pheromone-count actions-total]
        :as result}]
  (let [k (select-keys result [:tau-reserve :tau-survival :pressure])
        current (get acc k {:runs 0
                             :gap-sum 0.0
                             :reserve-sum 0.0
                             :tau-avg-sum 0.0
                             :tau-min ##Inf
                             :tau-max ##-Inf
                             :terminations []
                             :pher-rate-sum 0.0
                             :pher-trail-sum 0.0
                             :pher-count-sum 0.0
                             :actions-sum 0.0})]
    (assoc acc k {:runs        (inc (:runs current))
                  :gap-sum     (+ (:gap-sum current) gap)
                  :reserve-sum (+ (:reserve-sum current) reserve)
                  :tau-avg-sum (+ (:tau-avg-sum current) tau-avg)
                  :tau-min     (min (:tau-min current) tau-min)
                  :tau-max     (max (:tau-max current) tau-max)
                  :terminations (conj (:terminations current) termination)
                  :pher-rate-sum (+ (:pher-rate-sum current) pheromone-rate)
                  :pher-trail-sum (+ (:pher-trail-sum current) pheromone-trail-avg)
                  :pher-count-sum (+ (:pher-count-sum current) pheromone-count)
                  :actions-sum (+ (:actions-sum current) actions-total)
                  :suggestion  suggestion})))

(defn- summarise
  [aggregate-map]
(doseq [[params {:keys [runs gap-sum reserve-sum tau-avg-sum tau-min tau-max terminations suggestion
                         pher-rate-sum pher-trail-sum pher-count-sum actions-sum]}]
          (sort-by (juxt :tau-reserve :tau-survival :pressure) aggregate-map)]
    (let [gap-avg (/ gap-sum (double runs))
          reserve-avg (/ reserve-sum (double runs))
          tau-avg (/ tau-avg-sum (double runs))
          pher-rate-avg (/ pher-rate-sum (double runs))
          pher-trail-avg (/ pher-trail-sum (double runs))
          pher-count-avg (if (pos? runs)
                           (/ pher-count-sum (double runs))
                           0.0)
          actions-avg (if (pos? runs)
                        (/ actions-sum (double runs))
                        0.0)
          term-summary (->> terminations
                            frequencies
                            (map (fn [[term cnt]] (format "%s×%d" (name term) cnt)))
                            (str/join ", "))]
      (println (format "SUMMARY params=%s | runs=%d | Δ_avg=%.2f | reserve_avg=%.2f | tau_avg=%.3f range=[%.3f %.3f] | pher_rate_avg=%.3f trail_avg=%.3f (cnt=%.1f/%.1f) | terms=%s | next=%s"
                       (pr-str params)
                       runs
                       gap-avg
                       reserve-avg
                       tau-avg
                       tau-min
                       tau-max
                       pher-rate-avg
                       pher-trail-avg
                       pher-count-avg
                       actions-avg
                       (if (seq term-summary) term-summary "-")
                       (pr-str suggestion))))))

(defn- parse-int
  [s]
  (try (Integer/parseInt s)
       (catch Exception _ nil)))

(defn run-sweeps
  "Run ITER sweep iterations over PARAMETER-GRID, returning aggregated results."
  ([iterations]
   (run-sweeps iterations parameter-grid))
  ([iterations grid]
   (let [iterations (max 1 (int iterations))
         acc (atom {})]
     (dotimes [idx iterations]
       (let [iteration (inc idx)]
         (println (format "== Sweep %d/%d ==" iteration iterations))
         (doseq [params grid]
           (let [result (run-configuration default-ticks params)]
             (swap! acc merge-summary result)
             (println (format-result result iteration))))))
     (println "== Summary ==")
     (summarise @acc))))

(defn -main
  "CLI entry point. Optional first arg sets the number of sweep iterations (default 10)."
  [& args]
  (let [iterations (or (some-> args first parse-int) 10)]
    (run-sweeps iterations)))

(comment
  ;; Example REPL invocation
  (run-sweeps 2))
