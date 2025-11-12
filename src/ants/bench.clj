(ns ants.bench
  "Benchmark harness for classic vs AIF war scenarios at varying scales."
  (:refer-clojure :exclude [run!])
  (:gen-class)
  (:require [ants.war :as war]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def scenarios
  "Preset benchmark configurations keyed by scenario id."
  [{:id :grid-5
    :label "5x5"
    :size [5 5]
    :ticks 120
    :ants-per-side 4}
   {:id :grid-25
    :label "25x25"
    :size [25 25]
    :ticks 200
    :ants-per-side 18}
   {:id :grid-100
    :label "100x100"
    :size [100 100]
    :ticks 240
    :ants-per-side 48}])

(def headers
  ["scenario" "label" "size" "ticks" "ants_per_side" "duration_ms"
   "classic_score" "aif_score" "score_diff" "G_ema"])

(defn- ensure-output-path
  [path]
  (let [f (io/file path)]
    (io/make-parents f)
    f))

(defn- format-double
  [x]
  (format "%.3f" (double (or x 0.0))))

(defn- run-scenario
  [{:keys [id label size ticks ants-per-side] :as _scenario}]
  (let [config {:size size
                :ticks ticks
                :ants-per-side ants-per-side}
        start (System/nanoTime)
        final-world (war/simulate config {:hud? false})
        elapsed-ms (/ (- (System/nanoTime) start) 1e6)
        scores (:scores final-world)
        classic (double (get scores :classic 0.0))
        aif (double (get scores :aif 0.0))
        diff (- aif classic)
        g-ema (get-in final-world [:rolling :G])]
    {:scenario (name id)
     :label label
     :size (str (first size) "x" (second size))
     :ticks ticks
     :ants-per-side ants-per-side
     :duration-ms (format-double elapsed-ms)
     :classic-score (format-double classic)
     :aif-score (format-double aif)
     :score-diff (format-double diff)
     :G-ema (format-double g-ema)}))

(defn- ->row
  [result]
  [(result :scenario)
   (result :label)
   (result :size)
   (str (result :ticks))
   (str (result :ants-per-side))
   (result :duration-ms)
   (result :classic-score)
   (result :aif-score)
   (result :score-diff)
   (result :G-ema)])

(def output-file "out/bench.csv")

(defn run!
  "Execute all benchmark scenarios and persist a CSV report.
  Returns the vector of result maps."
  []
  (let [results (mapv run-scenario scenarios)
        out (ensure-output-path output-file)]
    (with-open [w (io/writer out)]
      (.write w (str (str/join "," headers) "\n"))
      (doseq [row (map ->row results)]
        (.write w (str (str/join "," row) "\n"))))
    results))

(defn -main
  [& _]
  (let [results (run!)]
    (println "Wrote benchmark results for" (count results) "scenarios ->" output-file)
    (doseq [{:keys [label duration-ms score-diff]} results]
      (println (format "%-8s %7sms Δscore %s" label duration-ms score-diff)))))
