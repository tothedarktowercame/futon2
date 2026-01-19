(ns ants.main
  "Entry point for the classic vs AIF war scenario."
  (:gen-class)
  (:require [ants.ui :as ui]
            [ants.war :as war]
            [clojure.string :as str]))

(defn- parse-int
  [s]
  (try
    (Integer/parseInt s)
    (catch Exception _ nil)))

(defn- parse-args
  [args]
  (loop [args args
         opts {}]
    (if (seq args)
      (let [[flag & more] args]
        (cond
          (= "--config" flag)
          (recur (rest more) (assoc opts :config-path (first more)))

          (= "--sigil-config" flag)
          (recur (rest more) (assoc opts :sigil-config-path (first more)))

          (= "--cyber-index" flag)
          (recur (rest more) (assoc opts :cyber-index (parse-int (first more))))

          (= "--sigil-index" flag)
          (recur (rest more) (assoc opts :sigil-index (parse-int (first more))))

          (= "--include-aif" flag)
          (recur more (assoc opts :include-aif true))

          :else
          (recur more (update opts :args (fnil conj []) flag))))
      opts)))

(defn -main
  "Launch the war simulation. Optional CLI args: [ticks]."
  [& args]
  (let [opts-set (->> args (filter #(str/starts-with? % "--")) set)
        positional (remove #(str/starts-with? % "--") args)
        ticks (some->> positional first parse-int)
        {:keys [config-path sigil-config-path cyber-index sigil-index include-aif]} (parse-args args)
        cyber-config (when config-path
                       {:config-path config-path
                        :index cyber-index})
        sigil-config (when sigil-config-path
                       {:config-path sigil-config-path
                        :index sigil-index})
        armies (cond
                 (and cyber-config sigil-config)
                 (vec (concat [:cyber :cyber-sigil] (when include-aif [:aif])))

                 cyber-config
                 [:cyber :aif]

                 :else nil)
        config (cond-> {}
                  ticks (assoc :ticks ticks)
                  (contains? opts-set "--no-termination") (assoc :enable-termination? false)
                  (seq armies) (assoc :armies armies)
                  cyber-config (assoc :cyber (assoc (get war/default-config :cyber {})
                                                    :config-path config-path
                                                    :index cyber-index))
                  sigil-config (assoc :cyber-sigil (assoc (get war/default-config :cyber-sigil {})
                                                          :config-path sigil-config-path
                                                          :index sigil-index)))
        final-world (war/run config)
        summary (ui/scoreboard final-world)]
    (println "Final:" summary)
    (shutdown-agents)))
