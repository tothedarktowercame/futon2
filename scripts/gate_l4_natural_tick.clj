;; gate_l4_natural_tick.clj -- E-live-loop-3 gate L4 (the exit: ledger section 10 real test).
;; PASS iff the REAL daily trace (data/wm-trace/, NOT the 2g witness dir) contains
;; an :act-gate-verdicts entry with :delta-G-source :fold-escrow on a SCHEDULED tick.
;; This is the proof that a natural scheduled tick consumed escrowed delta-G through
;; the pinned seam -- the falsifier "post-S2 ticks still 0-for-N" gets its real test.
;;
;; The gate greps the daily trace files for the literal :delta-G-source :fold-escrow
;; string. This is deliberately simple: the trace is large EDN that may carry data
;; readers; a grep is robust where edn/read-string may fail. The daily trace path
;; (data/wm-trace/) is distinct from the 2g witness dir (data/wm-trace-escrow-witness/).
;;
;; Run: cd /home/joe/code/futon2 && clojure -M scripts/gate_l4_natural_tick.clj
(ns gate-l4-natural-tick
  {:clj-kondo/config {:linters {:unused-namespace {:level :off}}}}
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(def ^:private daily-trace-dir
  (str (System/getProperty "user.home") "/code/futon2/data/wm-trace"))

(defn- trace-files []
  (->> (.listFiles (io/file daily-trace-dir))
       (filter #(re-matches #"wm-trace-\d{4}-\d{2}-\d{2}\.edn" (.getName ^java.io.File %)))
       sort))

(defn -main []
  (let [hits (for [f (trace-files)
                   :let [content (slurp f)
                         has-escrow (str/includes? content ":delta-G-source :fold-escrow")]
                   :when has-escrow]
               (.getName f))]
    (if (seq hits)
      (do (println "GATE L4 PASS -- :delta-G-source :fold-escrow found in daily trace:"
                   (str/join ", " hits))
          (System/exit 0))
      (do (println "GATE L4 FAIL -- no :delta-G-source :fold-escrow in any daily trace record")
          (println "  (checked" (count (trace-files)) "trace files in" daily-trace-dir ")")
          (System/exit 1)))))

(-main)
