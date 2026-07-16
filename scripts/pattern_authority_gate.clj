(ns pattern-authority-gate
  "Pattern actuator authority gate for the modern AIF ant.

  This is invoked by scripts/ant_authority_gate.clj in `pattern` mode.  It
  activates exactly one design pattern on every ant, exposes lambda.pattern,
  and compares every arm to `off` on paired, geometry-screened boards.

  Controls run first.  Any non-exact sham or lambda-zero comparison aborts
  before a real-pattern cell is evaluated."
  (:require [ants.aif.experiment :as experiment]
            [ants.war :as war]
            [clojure.string :as str]))

(def species :aif)
(def food :patchy)
(def size [30 30])
(def nearest-max 8.0)
(def default-lambdas [0.1 0.5 1.0])

(def sham-pattern :cyber/baseline)
(def real-patterns
  [:cyber/cargo-return
   :cyber/white-space
   :cyber/hunger-coupling
   :cyber/pheromone-tuner])
(def all-patterns (into [sham-pattern] real-patterns))

(defn board-nearest-food
  "Distance from the AIF home to the closest food cell.  This is pure board
  geometry: it runs no ant and observes no arm outcome."
  [seed ticks]
  (let [world (experiment/make-seeded-world
               species food seed (+ 10000 seed) size ticks)
        [hx hy] (get-in world [:homes species])
        distances (keep (fn [[[x y] cell]]
                          (when (> (double (or (:food cell) 0.0)) 0.05)
                            (Math/sqrt
                             (double (+ (* (- x hx) (- x hx))
                                        (* (- y hy) (- y hy)))))))
                        (get-in world [:grid :cells]))]
    (if (seq distances) (apply min distances) 999.0)))

(defn screen-seeds
  "Take `n` candidate seeds whose board geometry has reachable food."
  [candidates n ticks]
  (->> candidates
       (map (fn [seed] [seed (board-nearest-food seed ticks)]))
       (filter (fn [[_ distance]] (<= distance nearest-max)))
       (take n)
       vec))

(defn activate-pattern
  "Set both live switches for one run.  `pattern-id` nil is the off arm.
  Every initial ant receives the same singular pattern identity."
  [world pattern-id lambda-pattern]
  (-> world
      (assoc-in [:config :aif :efe :lambda :pattern]
                (double lambda-pattern))
      (update :ants
              (fn [ants]
                (into (empty ants)
                      (map (fn [[id ant]]
                             [id (if pattern-id
                                   (assoc ant :cyber-pattern
                                          {:id pattern-id :ticks-active 0})
                                   (dissoc ant :cyber-pattern))]))
                      ants)))))

(defn- thrash-transition?
  [before after]
  (and (contains? #{:forage :return} before)
       (contains? #{:forage :return} after)
       (not= before after)))

(defn- accumulate-events
  [{:keys [last-actions] :as metrics} events]
  (reduce (fn [m {:keys [id action]}]
            (let [prior (get last-actions id)]
              (-> m
                  (update-in [:actions action] (fnil inc 0))
                  (assoc-in [:last-actions id] action)
                  (cond-> (thrash-transition? prior action)
                    (update :thrash inc)))))
          (or metrics {:actions {} :last-actions {} :thrash 0})
          events))

(defn run-cell
  "Run one pattern/lambda cell on one paired seed.  Besides yield, retain
  action counts and forage/return alternation as behavior diagnostics."
  [pattern-id lambda-pattern seed ticks]
  (loop [world (-> (experiment/make-seeded-world
                    species food seed (+ 10000 seed) size ticks)
                   (activate-pattern pattern-id lambda-pattern))
         tick 0
         metrics {:actions {} :last-actions {} :thrash 0 :trace []}]
    (if (>= tick ticks)
      {:seed seed
       :yield (double (get-in world [:scores species] 0.0))
       :starved (let [dead (count (filter #(and (= species (:species %))
                                                  (= :starvation (:cause %)))
                                            (:graveyard world)))]
                  (/ dead 3.0))
       :actions (:actions metrics)
       :thrash (:thrash metrics)
       ;; Full events make the controls byte-auditable (including G/P).  The
       ;; behavior trace excludes controller telemetry, so a changed score
       ;; that leaves the ant's action and state path alone is not called a
       ;; behavioral move.
       :trace (:trace metrics)
       :behavior-trace
       (mapv #(select-keys % [:id :action :mode :cargo :ingest :h :loc :target
                              :moved :wander :gather :deposit :dead :white?])
             (:trace metrics))}
      (let [next-world (war/step world)]
        (recur next-world
               (inc tick)
               (-> (accumulate-events metrics (:last-events next-world))
                   (update :trace into (:last-events next-world))))))))

(defn run-arm
  [pattern-id lambda-pattern seeds ticks]
  (mapv #(run-cell pattern-id lambda-pattern % ticks) seeds))

(defn mean
  [xs]
  (if (seq xs) (/ (reduce + xs) (double (count xs))) 0.0))

(defn- choose
  [n k]
  (reduce * 1.0
          (map (fn [i] (/ (double (- n i)) (inc i))) (range k))))

(defn sign-test-p
  "Exact two-sided binomial sign-test. Ties are excluded by the caller."
  [n majority-count]
  (if (zero? n)
    1.0
    (min 1.0
         (* 2.0
            (reduce + (map #(* (choose n %)
                               (Math/pow 0.5 n))
                           (range majority-count (inc n))))))))

(defn summarize
  "Paired yield summary for an arm against off.  A live actuator differs in
  either direction at p<.05; authority is not merit."
  [arm lambda-pattern results off-results]
  (let [yields (mapv :yield results)
        off-yields (mapv :yield off-results)
        deltas (mapv - yields off-yields)
        nonzero (filterv #(not (zero? %)) deltas)
        wins (count (filter pos? nonzero))
        losses (count (filter neg? nonzero))
        informative (count nonzero)
        p (sign-test-p informative (max wins losses))
        behavior-seeds (count (filter false?
                                      (map (fn [[result off]]
                                             (= (:behavior-trace result)
                                                (:behavior-trace off)))
                                           (map vector results off-results))))
        action-keys (sort (into #{} (mapcat (comp keys :actions))
                                (concat results off-results)))
        behavior (into (sorted-map)
                       (for [action action-keys]
                         [action
                          (mean (map (fn [[result off]]
                                       (- (get-in result [:actions action] 0)
                                          (get-in off [:actions action] 0)))
                                     (map vector results off-results)))]))
        thrash-delta (mean (map (fn [[result off]]
                                  (- (:thrash result) (:thrash off)))
                                (map vector results off-results)))]
    {:arm arm
     :lambda (double lambda-pattern)
     :mean-yield (mean yields)
     :delta-yield (mean deltas)
     :wins wins
     :losses losses
     :informative informative
     :ties (- (count deltas) informative)
     :sign-p p
     :live-actuator? (< p 0.05)
     :behavior-seeds behavior-seeds
     :per-seed-delta deltas
     :behavior-delta (assoc behavior :forage-return-thrash thrash-delta)}))

(defn exact-results?
  "Observable run results, including every action event, are byte-equal.  The
  internal pattern tick counter intentionally differs and is not an outcome."
  [left right]
  (= left right))

(defn- print-row
  [{:keys [arm lambda delta-yield wins informative sign-p ties live-actuator?]}]
  (println (format "  %-18s %6.1f %+10.3f %3d/%-3d %9.5f %5d  %s"
                   arm lambda delta-yield wins informative sign-p ties
                   (if live-actuator? "DIFFERS — LIVE" "no detected difference"))))

(defn- parse-lambdas
  [raw]
  (if (str/blank? raw)
    default-lambdas
    (let [values (mapv #(Double/parseDouble %)
                       (str/split raw #","))]
      (when (or (empty? values) (some #(<= % 0.0) values))
        (throw (ex-info "lambda sweep must contain positive values"
                        {:type :pattern-gate/bad-lambdas :raw raw})))
      values)))

(defn -main
  [& args]
  (let [n (Integer/parseInt (or (first args) "20"))
        ticks (Integer/parseInt (or (second args) "300"))
        lambdas (parse-lambdas (nth args 2 nil))
        screened (screen-seeds (range 1 10000) n ticks)
        _ (when (< (count screened) n)
            (throw (ex-info "not enough geometry-screened boards"
                            {:type :pattern-gate/insufficient-boards
                             :wanted n :found (count screened)})))
        seeds (mapv first screened)
        _ (println "=== PATTERN ACTUATOR AUTHORITY GATE ===")
        _ (println "  PREREGISTERED: live actuator iff paired sign test differs from off at p<.05")
        _ (println (format "  %d held-out paired seeds x %d ticks; patchy %dx%d; nearest food <= %.1f"
                           n ticks (first size) (second size) nearest-max))
        _ (println "  held-out seeds (screened on geometry only, never selected against):" seeds)
        _ (println "  lambda.pattern sweep:" lambdas)
        off-results (run-arm nil 0.0 seeds ticks)
        ;; The controls are deliberately completed before any real lambda>0 arm.
        sham-controls (into {}
                            (for [lambda lambdas]
                              [lambda (run-arm sham-pattern lambda seeds ticks)]))
        zero-controls (into {}
                            (for [pattern all-patterns]
                              [pattern (run-arm pattern 0.0 seeds ticks)]))
        sham-ok? (every? #(exact-results? off-results (get sham-controls %)) lambdas)
        zero-ok? (every? #(exact-results? off-results (get zero-controls %)) all-patterns)]
    (println)
    (println "  CONTROL AUDIT")
    (doseq [lambda lambdas]
      (println (format "    sham lambda=%-4.1f exact per seed: %s"
                       lambda (if (exact-results? off-results
                                                  (get sham-controls lambda))
                                "YES" "NO"))))
    (println "    lambda=0 exact per seed for all five pattern ids:"
             (if zero-ok? "YES" "NO"))
    (when-not (and sham-ok? zero-ok?)
      (throw (ex-info "pattern authority instrument failed its exact controls; real arms not run"
                      {:type :pattern-gate/broken-instrument
                       :sham-exact? sham-ok?
                       :zero-lambda-exact? zero-ok?})))
    (let [sham-summaries (mapv (fn [lambda]
                                 (summarize "sham" lambda
                                            (get sham-controls lambda)
                                            off-results))
                               lambdas)
          real-results (into {}
                             (for [pattern real-patterns
                                   lambda lambdas]
                               [[pattern lambda]
                                (run-arm pattern lambda seeds ticks)]))
          real-summaries (mapv (fn [pattern lambda]
                                 (summarize (name pattern) lambda
                                            (get real-results [pattern lambda])
                                            off-results))
                               (mapcat #(repeat (count lambdas) %) real-patterns)
                               (cycle lambdas))
          summaries (into sham-summaries real-summaries)
          movers (filter :live-actuator? real-summaries)
          result {:gate :pattern-authority
                  :preregistered-verdict {:test :paired-sign
                                          :alpha 0.05
                                          :claim :differs-from-off}
                  :screen {:kind :geometry
                           :nearest-food-max nearest-max
                           :boards screened
                           :held-out? true}
                  :seeds seeds
                  :ticks ticks
                  :lambdas lambdas
                  :off-yields (mapv :yield off-results)
                  :controls {:sham-exact-per-seed? sham-ok?
                             :zero-lambda-exact-per-seed? zero-ok?}
                  :cells summaries
                  :live-actuators (mapv (juxt :arm :lambda) movers)
                  :authority-pass? (boolean (seq movers))}]
      (println)
      (println (format "  %-18s %6s %10s %7s %9s %5s  %s"
                       "arm" "lambda" "delta" "wins/n" "sign-p" "ties" "verdict"))
      (doseq [row summaries] (print-row row))
      (println)
      (println "  BEHAVIOR AUDIT (all cells with any changed action trace):")
      (let [behavior-cells (filter #(pos? (:behavior-seeds %)) real-summaries)]
        (if (seq behavior-cells)
          (doseq [{:keys [arm lambda behavior-seeds behavior-delta]} behavior-cells]
            (println (format "    %-18s lambda %.1f traces %d/%d; mean action deltas %s"
                             arm lambda behavior-seeds n
                             (pr-str behavior-delta))))
          (println "    none — every real-pattern action trace equals off")))
      (println)
      (println (if (seq movers)
                 "  AUTHORITY PASS — at least one design pattern is a live actuator. Authority is not merit."
                 "  AUTHORITY FAIL — no design pattern differs from off. Stop the pattern line here."))
      (spit "/tmp/pattern-authority-gate.edn" (pr-str result))
      (println "  wrote /tmp/pattern-authority-gate.edn")
      result)))
