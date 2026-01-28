(ns ants.tournament
  "Round-robin tournament runner for cyberant configs."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [ants.war :as war]))

(defn- usage []
  (str/join
   "\n"
   ["Round-robin tournament for hex vs sigil cyberants."
    ""
    "Usage:"
    "  clj -M -m ants.tournament --hex PATH --sigil PATH [options]"
    ""
    "Options:"
    "  --runs N         Repeats per matchup (default 20)."
    "  --ticks N        Ticks per run (default 200)."
    "  --hex PATH       EDN with :cyberants for hexagram configs."
    "  --sigil PATH     EDN with :cyberants for sigil configs."
    "  --out PATH       Write EDN summary to file."
    "  --include-aif    Include AIF as third population."
    "  --stacked        Stack all configs per side into one composite (compiled merge)."
    "  --stacked-ct     Stack at module level before compile (policy/precision/sense/adapt)."
    "  --no-termination Disable early termination."
    "  --help           Show this message."]))

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

          (= "--stacked" flag)
          (recur more (assoc opts :stacked true))

          (= "--stacked-ct" flag)
          (recur more (assoc opts :stacked-ct true))

          (= "--no-termination" flag)
          (recur more (assoc opts :no-termination true))

          :else
          (recur more (assoc opts :unknown flag))))
      opts)))

(defn- load-cyberants
  [path]
  (when (and path (.exists (io/file path)))
    (let [data (edn/read-string (slurp path))]
      (vec (:cyberants data)))))

(defn- label-for
  [cfg fallback]
  (or (:pattern-id cfg)
      (:pattern-title cfg)
      (:sigil cfg)
      fallback))

(defn- mean
  [xs]
  (if (seq xs)
    (/ (reduce + xs) (count xs))
    0.0))

(defn- merge-precision
  [configs]
  (let [pis (map #(get-in % [:precision :Pi-o]) configs)
        taus (keep #(get-in % [:precision :tau]) configs)
        keys (->> pis (filter map?) (mapcat keys) set)
        avg-pi (into {}
                     (map (fn [k]
                            [k (mean (keep #(get % k) pis))])
                          keys))]
    (cond-> {:Pi-o avg-pi}
      (seq taus) (assoc :tau (mean taus)))))

(defn- stack-configs
  [configs label]
  {:pattern-id (str "stack/" label)
   :pattern-title (str "Stacked " label)
   :precision (merge-precision configs)
   :pattern-program {:stacked true
                     :members (mapv (fn [cfg]
                                      (select-keys cfg [:pattern-id :pattern-title :sigil]))
                                    configs)}})

(defn- avg-map
  [maps]
  (let [maps (filter map? maps)
        keys (->> maps (mapcat keys) set)]
    (into {}
          (map (fn [k]
                 (let [vals (keep #(get % k) maps)
                       nums (filter number? vals)]
                   [k (if (seq nums)
                        (mean nums)
                        (first vals))]))
               keys))))

(defn- merge-adapt
  [configs]
  (let [adapts (keep :adapt-config configs)
        numeric-keys (->> adapts (mapcat keys) set)
        merged (into {}
                     (map (fn [k]
                            [k (let [vals (keep #(get % k) adapts)
                                     nums (filter number? vals)]
                                 (if (seq nums) (mean nums) (first vals)))])
                          numeric-keys))]
    merged))

(defn- stack-configs-ct
  "Stack at the module level (policy/precision/sense/adapt)."
  [configs label]
  {:pattern-id (str "stack-ct/" label)
   :pattern-title (str "Stacked-CT " label)
   :policy-priors (avg-map (keep :policy-priors configs))
   :precision (merge-precision configs)
   :pattern-sense (avg-map (keep :pattern-sense configs))
   :adapt-config (merge-adapt configs)
   :pattern-program {:stacked true
                     :stacked-ct true
                     :members (mapv (fn [cfg]
                                      (select-keys cfg [:pattern-id :pattern-title :sigil]))
                                    configs)}})

(defn- run-once
  [run-id {:keys [hex-path sigil-path hex-index sigil-index ticks include-aif? no-termination?
                  hex-config sigil-config]}]
  (let [armies (vec (concat [:cyber :cyber-sigil] (when include-aif? [:aif])))
        config (cond-> {:ticks (or ticks 200)
                        :armies armies
                        :cyber (or (and hex-config {:config hex-config})
                                   {:config-path hex-path :index hex-index})
                        :cyber-sigil (or (and sigil-config {:config sigil-config})
                                         {:config-path sigil-path :index sigil-index})}
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

(defn- accumulate
  [acc idx score win?]
  (-> acc
      (update-in [idx :scores] (fnil conj []) (double score))
      (update-in [idx :wins] (fnil conj []) (boolean win?))))

(defn- summarize-side
  [entries labels]
  (into {}
        (map (fn [[idx {:keys [scores wins]}]]
               (let [scores (vec (or scores []))
                     wins (vec (or wins []))
                     mean (mean scores)
                     win-rate (if (seq wins)
                                (/ (count (filter true? wins)) (count wins))
                                0.0)]
                 [idx {:label (get labels idx (str idx))
                       :mean-score mean
                       :win-rate win-rate
                       :runs (count scores)}]))
             entries)))

(defn- summarize
  [rows]
  (let [score (fn [species]
                (map #(double (get-in % [:scores species] 0.0)) rows))]
    {:mean-score {:cyber (mean (score :cyber))
                  :cyber-sigil (mean (score :cyber-sigil))
                  :aif (mean (score :aif))}
     :runs (count rows)}))

(defn -main
  [& args]
  (let [{:keys [help unknown runs ticks hex-path sigil-path out include-aif no-termination stacked stacked-ct]
         :or {runs 20 ticks 200}} (parse-args args)]
    (cond
      help (println (usage))
      unknown (do (println (str "Unknown flag: " unknown))
                  (println (usage)))
      (or (nil? hex-path) (nil? sigil-path))
      (do (println "Missing --hex or --sigil.")
          (println (usage)))
      :else
      (let [hex-cfgs (load-cyberants hex-path)
            sigil-cfgs (load-cyberants sigil-path)
            hex-count (count hex-cfgs)
            sigil-count (count sigil-cfgs)
            hex-labels (into {} (map-indexed (fn [idx cfg]
                                               [idx (label-for cfg (str "hex-" idx))])
                                             hex-cfgs))
            sigil-labels (into {} (map-indexed (fn [idx cfg]
                                                 [idx (label-for cfg (str "sigil-" idx))])
                                               sigil-cfgs))
            stacked? (boolean stacked)
            stacked-ct? (boolean stacked-ct)
            rows (if stacked?
                   (let [hex-stack (stack-configs hex-cfgs "hex")
                         sigil-stack (stack-configs sigil-cfgs "sigil")]
                     (mapv (fn [idx]
                             (run-once (inc idx)
                                       {:hex-path hex-path
                                        :sigil-path sigil-path
                                        :hex-index 0
                                        :sigil-index 0
                                        :hex-config hex-stack
                                        :sigil-config sigil-stack
                                        :ticks ticks
                                        :include-aif? include-aif
                                        :no-termination? no-termination}))
                           (range runs)))
                   (if stacked-ct?
                     (let [hex-stack (stack-configs-ct hex-cfgs "hex")
                           sigil-stack (stack-configs-ct sigil-cfgs "sigil")]
                       (mapv (fn [idx]
                               (run-once (inc idx)
                                         {:hex-path hex-path
                                          :sigil-path sigil-path
                                          :hex-index 0
                                          :sigil-index 0
                                          :hex-config hex-stack
                                          :sigil-config sigil-stack
                                          :ticks ticks
                                          :include-aif? include-aif
                                          :no-termination? no-termination}))
                             (range runs)))
                   (let [pairings (for [h (range hex-count)
                                        s (range sigil-count)
                                        r (range runs)]
                                    {:hex-index h
                                     :sigil-index s
                                     :repeat r})]
                     (mapv (fn [idx {:keys [hex-index sigil-index]}]
                             (run-once (inc idx)
                                       {:hex-path hex-path
                                        :sigil-path sigil-path
                                        :hex-index hex-index
                                        :sigil-index sigil-index
                                        :ticks ticks
                                        :include-aif? include-aif
                                        :no-termination? no-termination}))
                           (range (count pairings))
                           pairings))))
            hex-acc (reduce (fn [acc {:keys [hex-index scores]}]
                              (accumulate acc hex-index
                                          (get scores :cyber 0.0)
                                          (>= (get scores :cyber 0.0)
                                              (get scores :cyber-sigil 0.0))))
                            {} rows)
            sigil-acc (reduce (fn [acc {:keys [sigil-index scores]}]
                                (accumulate acc sigil-index
                                            (get scores :cyber-sigil 0.0)
                                            (> (get scores :cyber-sigil 0.0)
                                               (get scores :cyber 0.0))))
                              {} rows)
            output {:generated-at (.toString (java.time.Instant/now))
                    :hex-path hex-path
                    :sigil-path sigil-path
                    :runs runs
                    :ticks ticks
                    :hex-count hex-count
                    :sigil-count sigil-count
                    :stacked stacked?
                    :stacked-ct stacked-ct?
                    :summary {:hex (summarize-side hex-acc hex-labels)
                              :sigil (summarize-side sigil-acc sigil-labels)
                              :overall (summarize rows)}
                    :rows rows}
            rendered (pr-str output)]
        (if out
          (do
            (io/make-parents out)
            (spit out rendered))
          (println rendered))))))
