(ns wm-futility
  (:require [clojure.pprint :as pp]
            [futon2.aif.lane-futility :as futility]))

(defn- usage! []
  (println "Usage: clojure -M scripts/wm_futility.clj <summary|gate-3a|gate-3b|gate-3c> [trace-dir]")
  (System/exit 2))

(defn- trace-dir [args]
  (or (second args) futility/default-trace-dir))

(defn- load-summary [dir]
  (let [records (vec (futility/trace-records dir))]
    {:records records
     :summary (futility/futility-summary records)}))

(defn- print-top-rows [rows]
  (doseq [{:keys [lane attempts successes failures]} (take 10 rows)]
    (println (format "  %-58s %4d attempts, %4d success, %4d failure"
                     lane attempts successes failures))))

(defn summary! [dir]
  (let [{:keys [summary]} (load-summary dir)]
    (println "WM lane futility summary")
    (println "  trace records:" (:record-count summary))
    (println "  attempts:" (:attempt-count summary))
    (println "  lanes:" (:lane-count summary))
    (println "  zero-for-N lanes:" (:zero-lane-count summary))
    (println "Top lanes:")
    (print-top-rows (:rows summary))
    (println)
    (pp/pprint summary)))

(defn gate-3a! [dir]
  (let [{:keys [records summary]} (load-summary dir)
        hand (futility/hand-counts records)]
    (cond
      (empty? records)
      (do (println "GATE 3a FAIL - no trace records in" dir)
          (System/exit 1))

      (not= (:record-count summary) (:attempt-count summary))
      (do (println "GATE 3a FAIL - some trace records lacked selected actions")
          (System/exit 1))

      (not (futility/summary-matches-hand-counts? summary hand))
      (do (println "GATE 3a FAIL - summary differs from independent hand-count")
          (System/exit 1))

      (zero? (:zero-lane-count summary))
      (do (println "GATE 3a FAIL - no 0-for-N lanes found")
          (System/exit 1))

      :else
      (do
        (println "GATE 3a PASS - lane futility counter queryable and hand-count matched")
        (println "  records:" (:record-count summary)
                 "lanes:" (:lane-count summary)
                 "zero-for-N lanes:" (:zero-lane-count summary))
        (println "  top lanes:")
        (print-top-rows (:rows summary))
        (System/exit 0)))))

(defn gate-3b! [dir]
  (let [{:keys [records]} (load-summary dir)
        historical (futility/historical-gamma-report records)
        synthetic (futility/synthetic-paying-gamma-report)
        historical-gamma (get-in historical [:final :selection-gain])
        synthetic-gamma (get-in synthetic [:final :selection-gain])]
    (cond
      (empty? records)
      (do (println "GATE 3b FAIL - no trace records in" dir)
          (System/exit 1))

      (not (< historical-gamma 0.75))
      (do (println "GATE 3b FAIL - historical 0-for-N trajectory did not hedge:" historical-gamma)
          (System/exit 1))

      (not (> synthetic-gamma 1.25))
      (do (println "GATE 3b FAIL - synthetic paying trajectory did not commit:" synthetic-gamma)
          (System/exit 1))

      :else
      (do
        (println "GATE 3b PASS - gamma trajectories diverge sensibly")
        (println "  historical samples:" (:samples historical)
                 "gamma:" historical-gamma
                 "mean-perf:" (get-in historical [:final :mean-perf]))
        (println "  synthetic samples:" (:samples synthetic)
                 "gamma:" synthetic-gamma
                 "mean-perf:" (get-in synthetic [:final :mean-perf]))
        (System/exit 0)))))

(defn gate-3c! []
  (let [summary (futility/synthetic-futility-summary)
        bulletins (futility/dry-run-bulletins summary)]
    (if (and (= 1 (count bulletins))
             (= :nag (:lane (first bulletins)))
             (:dry-run? (first bulletins)))
      (do
        (println "GATE 3c PASS - synthetic 0-for-N burst produced dry-run nag bulletin")
        (pp/pprint (first bulletins))
        (System/exit 0))
      (do
        (println "GATE 3c FAIL - expected one dry-run nag bulletin, got:")
        (pp/pprint bulletins)
        (System/exit 1)))))

(defn -main [& args]
  (case (first args)
    "summary" (summary! (trace-dir args))
    "gate-3a" (gate-3a! (trace-dir args))
    "gate-3b" (gate-3b! (trace-dir args))
    "gate-3c" (gate-3c!)
    (usage!)))

(apply -main *command-line-args*)
