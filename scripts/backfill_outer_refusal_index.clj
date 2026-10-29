(ns backfill-outer-refusal-index
  "One-off tooling-JVM entry point for the terminal refusal index."
  (:require [futon2.aif.outer-refusal-index :as index]))

(defn -main [& [run-record-dir output]]
  (when-not (and run-record-dir output)
    (throw (ex-info "usage: <run-record-dir> <new-output.ednl>"
                    {:kind :outer/refusal-index-arguments})))
  (println (index/backfill! run-record-dir output)))
