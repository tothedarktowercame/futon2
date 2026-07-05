;; gate_l4_natural_tick.clj -- E-live-loop-3 gate L4 (the exit: ledger section 10 real test).
;; PASS iff the REAL daily trace (data/wm-trace/, NOT the 2g witness dir) contains
;; an :act-gate-verdicts entry with :delta-G-source :fold-escrow on a SCHEDULED tick.
;; This is the proof that a natural scheduled tick consumed escrowed delta-G through
;; the pinned seam -- the falsifier "post-S2 ticks still 0-for-N" gets its real test.
;;
;; The gate reads ALL daily trace records, checks each :act-gate-verdicts entry.
;; It distinguishes the daily trace from the 2g witness dir by checking ONLY
;; data/wm-trace/ (the production path). The witness dir is deliberately separate
;; (2g quarantined it because trace read-back feeds gamma).
;;
;; Run: cd /home/joe/code/futon2 && clojure -M scripts/gate_l4_natural_tick.clj
(ns gate-l4-natural-tick
  {:clj-kondo/config {:linters {:unused-namespace {:level :off}}}}
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

(def ^:private daily-trace-dir
  (str (System/getProperty "user.home") "/code/futon2/data/wm-trace"))

(defn- trace-files []
  (->> (.listFiles (io/file daily-trace-dir))
       (filter #(re-matches #"wm-trace-\d{4}-\d{2}-\d{2}\.edn" (.getName ^java.io.File %)))
       (sort-by #(.getName ^java.io.File %))))

(defn- escrow-verdicts-in [file]
  (try
    (let [records (edn/read-string {:readers {}} (slurp file))
          records (if (map? records) [records] records)]
      (for [r records
            v (:act-gate-verdicts r)
            :when (= :fold-escrow (:delta-G-source v))]
        {:timestamp (:timestamp r)
         :mission (:mission v)
         :verdict (:verdict v)
         :delta-G (:delta-G v)
         :delta-G-source (:delta-G-source v)}))
    (catch Throwable _ [])))

(defn -main []
  (let [hits (mapcat escrow-verdicts-in (trace-files))]
    (if (seq hits)
      (do (doseq [h hits]
            (println (format "  %s: %s dG=%s source=%s verdict=%s"
                             (:timestamp h) (:mission h)
                             (:delta-G h) (:delta-G-source h) (:verdict h))))
          (println "GATE L4 PASS --" (count hits)
                   "scheduled-tick verdict(s) with :delta-G-source :fold-escrow in the daily trace")
          (System/exit 0))
      (do (println "GATE L4 FAIL -- no :delta-G-source :fold-escrow in any daily trace record")
          (println "  (checked" (count (trace-files)) "trace files in" daily-trace-dir ")")
          (System/exit 1)))))

(-main)
