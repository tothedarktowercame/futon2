#!/usr/bin/env bb
;; U56 §4 -- replay u41's run-provenance scan against the CURRENT committed
;; tension ledger, for each deposited run. NO DEPOSIT.
;;
;;   bb u56_tension_scan.bb
;;
;; READ-ONLY. Reads tension-ledger.edn and the run stores' receipt filenames.
;; Writes nothing and appends no ledger row: the point is to show what
;; `u41_tension_ledger.bb --deposit` WOULD find today, against what its
;; already-committed rows (seq 9, 13, 18) found when they were written -- and
;; the run-era ledger is append-only (run_era_ledger.bb:241-243), so those rows
;; cannot be rewritten in any case.
;;
;; The scan logic mirrors u41_tension_ledger.bb:549-581 and its green branch at
;; :597-601: `(or run-id-appears-in-ledger? (seq tick-ids-appearing))`. (:U60
;; moved both down the file and added a per-tension BASIS beside the scan,
;; `run-attribution:531-547`; the green condition itself is unchanged, which is
;; why this mirror still holds.)
(require '[clojure.edn :as edn] '[clojure.java.io :as io] '[clojure.string :as str])

(def lab (str (System/getProperty "user.home") "/code/futon2/holes/labs/wm-contract"))
(def ledger-path (str lab "/tension-ledger.edn"))

(defn run-tick-ids
  "The run's tick ids, off its own store's receipt filenames (u41:456-465)."
  [run-id]
  (let [d (io/file lab "runs" run-id)]
    (when (.isDirectory d)
      (->> (.listFiles d)
           (map #(.getName ^java.io.File %))
           (keep #(second (re-matches #"tick-run-record-\d{4}-\d{2}-\d{2}-(.+)\.edn" %)))
           sort vec))))

(let [text (slurp ledger-path)
      ledger (edn/read-string text)]
  (println (format "tension-ledger.edn: %d tensions, %d events"
                   (count (:tensions ledger)) (count (:events ledger))))
  (println (format "  status fold: %s" (pr-str (into (sorted-map) (frequencies (map :tension/status (:tensions ledger)))))))
  (println (format "  event types: %s" (pr-str (into (sorted-map) (frequencies (map :event/type (:events ledger)))))))
  (println "  tensions born of a run (:born-of :refused-prediction with a run in provenance):")
  (doseq [t (:tensions ledger)
          :when (some #(re-find #"runs/\d{4}-\d{2}-\d{2}" (str %))
                      (get-in t [:tension/provenance :pointers]))]
    (println "   " (pr-str (:tension/id t)) "| status" (:tension/status t)
             "| minted-by" (pr-str (:tension/minted-by t))))
  (println)
  (doseq [run-id ["2026-09-01-s5" "2026-09-04-re5" "2026-09-04-010-accepted"]]
    (let [ids (or (run-tick-ids run-id) [])
          in-ledger? (str/includes? text run-id)
          appearing (vec (filter #(str/includes? text %) ids))]
      (println run-id)
      (println (format "  run-id-appears-in-ledger?  %s" in-ledger?))
      (println (format "  tick ids sought %d, appearing %s" (count ids) (pr-str appearing)))
      (println (format "  => u41's green branch fires: %s"
                       (boolean (or in-ledger? (seq appearing))))))))
