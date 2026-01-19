(ns ants.compare
  "Batch comparison runner for cyberant configs."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [ants.cyber :as cyber]
            [ants.war :as war]))

(defn- usage []
  (str/join
   "\n"
   ["Compare cyberant configs (hexagram vs sigil)."
    ""
    "Usage:"
    "  clj -M -m ants.compare --hex PATH --sigil PATH [options]"
    ""
    "Options:"
    "  --runs N           Number of runs (default 50)."
    "  --ticks N          Ticks per run (default 200)."
    "  --hex PATH         EDN with :cyberants for hexagram configs."
    "  --sigil PATH       EDN with :cyberants for sigil configs."
    "  --include-aif      Include AIF as third population."
    "  --no-termination   Disable early termination."
    "  --out PATH         Write EDN summary to file."
    "  --help             Show this message."]))

(defn- parse-int
  [s]
  (try
    (Long/parseLong s)
    (catch Exception _ nil)))

(defn- parse-args
  [args]
  (loop [args args
         opts {}]
    (if (seq args)
      (let [[flag & more] args]
        (cond
          (#{"--help" "-h"} flag)
          (recur more (assoc opts :help true))

          (= "--runs" flag)
          (recur (rest more) (assoc opts :runs (parse-int (first more))))

          (= "--ticks" flag)
          (recur (rest more) (assoc opts :ticks (parse-int (first more))))

          (= "--hex" flag)
          (recur (rest more) (assoc opts :hex-path (first more)))

          (= "--sigil" flag)
          (recur (rest more) (assoc opts :sigil-path (first more)))

          (= "--out" flag)
          (recur (rest more) (assoc opts :out (first more)))

          (= "--include-aif" flag)
          (recur more (assoc opts :include-aif true))

          (= "--no-termination" flag)
          (recur more (assoc opts :no-termination true))

          :else
          (recur more (assoc opts :unknown flag))))
      opts)))

(defn- count-cyberants
  [path]
  (if-let [data (and path (.exists (io/file path))
                     (edn/read-string (slurp path)))]
    (count (:cyberants data))
    0))

(defn- run-once
  [run-id {:keys [hex-path sigil-path hex-index sigil-index ticks include-aif? no-termination?]}]
  (let [armies (vec (concat [:cyber :cyber-sigil] (when include-aif? [:aif])))
        config (cond-> {:ticks (or ticks 200)
                        :armies armies
                        :cyber {:config-path hex-path :index hex-index}
                        :cyber-sigil {:config-path sigil-path :index sigil-index}}
                 no-termination? (assoc :enable-termination? false))
        world (war/simulate config {:hud? false})
        scores (:scores world)
        reserves (:colonies world)
        counts (frequencies (map :species (vals (:ants world))))]
    {:run run-id
     :hex-index hex-index
     :sigil-index sigil-index
     :scores scores
     :reserves (into {} (map (fn [[k v]] [k (:reserves v)]) reserves))
     :counts counts
     :terminated? (boolean (:terminated? world))}))

(defn- summarize
  [rows]
  (let [score (fn [species]
                (map #(double (get-in % [:scores species] 0.0)) rows))
        mean (fn [xs]
               (if (seq xs)
                 (/ (reduce + xs) (count xs))
                 0.0))]
    {:mean-score {:cyber (mean (score :cyber))
                  :cyber-sigil (mean (score :cyber-sigil))
                  :aif (mean (score :aif))}
     :runs (count rows)}))

(defn -main
  [& args]
  (let [{:keys [help unknown runs ticks hex-path sigil-path out include-aif no-termination]
         :or {runs 50 ticks 200}} (parse-args args)]
    (cond
      help (println (usage))
      unknown (do (println (str "Unknown flag: " unknown))
                  (println (usage)))
      (or (nil? hex-path) (nil? sigil-path))
      (do (println "Missing --hex or --sigil.")
          (println (usage)))
      :else
      (let [hex-count (count-cyberants hex-path)
            sigil-count (count-cyberants sigil-path)
            rows (mapv (fn [idx]
                         (run-once (inc idx)
                                   {:hex-path hex-path
                                    :sigil-path sigil-path
                                    :hex-index (if (pos? hex-count) (mod idx hex-count) 0)
                                    :sigil-index (if (pos? sigil-count) (mod idx sigil-count) 0)
                                    :ticks ticks
                                    :include-aif? include-aif
                                    :no-termination? no-termination}))
                       (range runs))
            output {:generated-at (.toString (java.time.Instant/now))
                    :hex-path hex-path
                    :sigil-path sigil-path
                    :runs runs
                    :ticks ticks
                    :include-aif include-aif
                    :summary (summarize rows)
                    :rows rows}
            rendered (pr-str output)]
        (if out
          (do
            (io/make-parents out)
            (spit out rendered))
          (println rendered))))))
