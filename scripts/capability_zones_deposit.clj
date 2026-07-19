(ns capability-zones-deposit
  "Deposit the accepted pca3-v1 harvest records into the live posterior store.

  DRY-RUN BY DEFAULT: validates every record, checks store health, and checks
  that no prior capability-zone deposit exists. Writes ONLY with an explicit
  `--execute` argument — the arming of which is Joe-gated (walk verdict,
  M-capability-zones.md). Aborts rather than double-deposits: replayed
  rehydration would survive duplicates, but the store record should not carry
  them."
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [futon2.aif.intrinsic-values :as iv]
            [futon2.aif.substrate :as substrate]))

(def artifact-path "data/capability_zones/harvest-2026-07-19-3d.edn")
(def run-id-prefix "capability-zone-pca3-v1:")

(defn- store-records []
  (iv/fetch-records {:limit 2000}))

(defn- validate [records]
  (let [problems
        (keep-indexed
         (fn [i r]
           (let [missing (remove #(contains? r %)
                                 [:class :as-of :alpha-post :beta-post
                                  :alpha-pre :beta-pre :outer-loop-run-id])]
             (when (seq missing)
               {:index i :class (:class r) :missing (vec missing)})))
         records)]
    (vec problems)))

(defn -main [& args]
  (let [execute? (some #{"--execute"} args)
        artifact (edn/read-string (slurp artifact-path))
        records (sort-by :as-of (:records artifact))
        problems (validate records)
        existing (store-records)
        prior-deposit (filter #(str/starts-with? (str (:outer-loop-run-id % ""))
                                                 run-id-prefix)
                              existing)]
    (println "artifact records:" (count records)
             "| classes:" (count (distinct (map :class records))))
    (println "record-shape problems:" (count problems))
    (when (seq problems) (println (take 5 problems)))
    (println "store baseline (wm-hyperparameter-update):" (count existing))
    (println "prior capability-zone deposits found:" (count prior-deposit))
    (cond
      (seq problems)
      (do (println "ABORT: records fail shape validation.") (System/exit 1))

      (seq prior-deposit)
      (do (println "ABORT: capability-zone records already present — no double deposit.")
          (System/exit 1))

      (not execute?)
      (println "DRY-RUN complete: ready to deposit" (count records)
               "records to" (substrate/configured-url)
               "— rerun with --execute (Joe-gated) to write.")

      :else
      (do (println "EXECUTING deposit of" (count records) "records…")
          (doseq [r records] (iv/persist-record! r))
          (let [after (store-records)]
            (println "store count after:" (count after))
            (println "rehydrating in-process atom from store…")
            (iv/rehydrate-from-store!)
            (println "done."))))))

(apply -main *command-line-args*)
