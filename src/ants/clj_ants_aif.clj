(ns ants.clj-ants-aif
  "Backward-compatible entrypoint that delegates to ants.main."
  (:refer-clojure :exclude [run!])
  (:gen-class)
  (:require [ants.main :as main]))

(defn run!
  "Run the war scenario with an optional tick limit integer."
  ([] (main/-main))
  ([ticks]
   (main/-main (str ticks))))

(defn -main
  [& args]
  (apply main/-main args))
