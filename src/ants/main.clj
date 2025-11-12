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

(defn -main
  "Launch the war simulation. Optional CLI args: [ticks]."
  [& args]
  (let [opts (->> args (filter #(str/starts-with? % "--")) set)
        ticks (some->> args
                       (remove #(str/starts-with? % "--"))
                       first
                       parse-int)
        config (cond-> {}
                  ticks (assoc :ticks ticks)
                  (contains? opts "--no-termination") (assoc :enable-termination? false))
        final-world (war/run config)
        summary (ui/scoreboard final-world)]
    (println "Final:" summary)
    (shutdown-agents)))
