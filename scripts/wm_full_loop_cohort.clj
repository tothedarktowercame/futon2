(require '[clojure.pprint :as pp]
         '[futon2.aif.full-loop-cohort :as cohort])

(defn usage []
  (binding [*out* *err*]
    (println "usage:")
    (println "  clojure -M scripts/wm_full_loop_cohort.clj status")
    (println "  clojure -M scripts/wm_full_loop_cohort.clj render")
    (println "  clojure -M scripts/wm_full_loop_cohort.clj activate")
    (println "  clojure -M scripts/wm_full_loop_cohort.clj start CELL.edn")
    (println "  clojure -M scripts/wm_full_loop_cohort.clj checkpoint ATTEMPT TYPE CELL.edn")
    (println "  clojure -M scripts/wm_full_loop_cohort.clj close ATTEMPT CELL.edn")))

(let [[command & args] *command-line-args*]
  (try
    (case command
      "status" (pp/pprint (cohort/ledger))
      "render" (pp/pprint
                 (cohort/write-ledgers!
                  "holes/labs/M-aif-full-loop-40/ledger.edn"
                  "holes/labs/M-aif-full-loop-40/ledger.html"))
      "activate" (println (cohort/activate!))
      "start" (pp/pprint (cohort/start-attempt! (cohort/read-edn (first args))))
      "checkpoint" (let [[attempt type path] args]
                     (pp/pprint
                      (cohort/append-checkpoint! attempt (keyword type)
                                                 (cohort/read-edn path))))
      "close" (let [[attempt path] args]
                (pp/pprint (cohort/close-attempt! attempt (cohort/read-edn path))))
      (do (usage) (System/exit 2)))
    (catch Throwable t
      (binding [*out* *err*]
        (println (.getMessage t))
        (when-let [d (ex-data t)] (pp/pprint d)))
      (System/exit 1))))
