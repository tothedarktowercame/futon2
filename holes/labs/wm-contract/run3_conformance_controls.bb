#!/usr/bin/env bb
;; Negative controls for run3_conformance.bb. Each plants ONE defect into a copy
;; of the real S1b route and asserts the check refuses it.
(require '[clojure.edn :as edn] '[clojure.java.io :as io])
(def src "runs/2026-09-01-s1b/wm-trace-s1b.edn")
(defn read-forms [p]
  (with-open [r (java.io.PushbackReader. (io/reader p))]
    (loop [a []] (let [f (edn/read {:eof ::eof} r)] (if (= ::eof f) a (recur (conj a f)))))))
(def rec (first (read-forms src)))
(defn tag [n] {:node n :via "planted" :at "2026-09-01T00:00:00Z"})
(defn write-one! [path route]
  (spit path (str (pr-str (assoc rec :wm/route route)) "\n")))
(def base (:wm/route rec))

;; 1. a code-retired ROUTE-GRAIN edge: R7->R14 (retired :r7-r14-conflates-two-precisions,
;;    :grounds :code, and NOT in :route-measured-drawn, so it is route-grain)
(write-one! "/tmp/c1.edn" (into (vec (take 2 base)) [(tag :R7) (tag :R14)]))
;; 2. an unknown pair: R4->R20 appears nowhere
(write-one! "/tmp/c2.edn" (into (vec (take 2 base)) [(tag :R4) (tag :R20)]))
;; 3. a DRAWN edge traversed in reverse: R7->R3 is drawn, R3->R7 is not
(write-one! "/tmp/c3.edn" (into (vec (take 2 base)) [(tag :R3) (tag :R7)]))
;; Writing a planted defect proves nothing until the check is RUN on it and
;; refuses. Each control asserts the exit code AND the class word, so a check
;; that refused for the wrong reason (an unmapped hop where a refutation was
;; planted) is caught too. A positive control on the real file keeps "refuses
;; everything" from passing as "refuses the defect" (claude-1 review, 2026-09-01).
(require '[clojure.java.shell :as sh] '[clojure.string :as str])
(defn run-check [path]
  (let [{:keys [exit out]} (sh/sh "bb" "run3_conformance.bb" path)]
    {:exit exit :out out}))
(def expectations
  [["/tmp/c1.edn" "refutation" "code-retired route-grain edge R7->R14 must be a REFUTATION"]
   ["/tmp/c2.edn" "unmapped"   "unknown pair R4->R20 must be UNMAPPED"]
   ["/tmp/c3.edn" "unmapped"   "reversed drawn edge R3->R7 must be UNMAPPED (direction is checked)"]])
(def failures
  (remove nil?
    (for [[path word why] expectations]
      (let [{:keys [exit out]} (run-check path)
            refused (= 1 exit)
            named (re-find (re-pattern (str "run3: " word "\\s+[1-9]")) out)]
        (when-not (and refused named)
          (str "CONTROL FAILED: " why " -- exit " exit (when-not named (str ", '" word "' not counted"))))))))
;; 4. RUN11 SELECTION CONTROL. Two runs' records share one per-date trace file;
;;    only the records whose :run/id matches this run's receipts are the run.
;;    Planted rather than observed because every record written before RUN11
;;    carries no :run/id -- without this the by-id branch would never execute.
(def sel-root "/tmp/run3-selection-control")
(def sel-trace (str sel-root "/trace"))
(def sel-run (str sel-root "/runs/planted-run"))
(defn spit-forms! [path forms]
  (io/make-parents path)
  (spit path (str/join "\n" (map pr-str forms))))
(def mine-id "11111111-1111-1111-1111-111111111111")
(def theirs-id "22222222-2222-2222-2222-222222222222")
(defn plant-selection-control! []
  (doseq [f (reverse (file-seq (io/file sel-root)))] (.delete f))
  (io/make-parents (str sel-trace "/x"))
  (io/make-parents (str sel-run "/x"))
  ;; Mine: the real S1b route, which is CONFORMANT.
  ;; Theirs: the same record with a planted unknown pair, which is NOT.
  ;; So selecting the wrong records cannot pass by accident -- a check that
  ;; read the whole file would report the unmapped hop and exit 1.
  (spit-forms! (str sel-trace "/wm-trace-2026-09-01.edn")
               [(assoc rec :run/id mine-id)
                (assoc rec :run/id theirs-id
                       :wm/route (into (vec (take 2 base)) [(tag :R4) (tag :R20)]))
                (assoc rec :run/id mine-id)])
  (spit (str sel-run "/tick-run-record-2026-09-01-" mine-id ".edn")
        (pr-str {:run/id mine-id})))
(plant-selection-control!)
(def selection
  (let [{:keys [exit out]} (sh/sh "bb" "run3_conformance.bb" sel-run
                                  :env (assoc (into {} (System/getenv))
                                              "FUTON_WM_TRACE_DIR" sel-trace))]
    {:exit exit :out out}))
(def selection-failures
  (remove nil?
          [(when-not (re-find #"run3: selection by-run-id" (:out selection))
             "SELECTION CONTROL FAILED: run id present but selection did not use it")
           (when-not (re-find #"run3: 2 records, 2 routes" (:out selection))
             "SELECTION CONTROL FAILED: expected exactly the 2 records of this run")
           (when-not (zero? (:exit selection))
             (str "SELECTION CONTROL FAILED: the other run's unmapped hop leaked in, exit "
                  (:exit selection)))]))

(def positive (run-check src))
(doseq [f (concat failures selection-failures)] (println f))
(when-not (zero? (:exit positive))
  (println "POSITIVE CONTROL FAILED: the real S1b file must pass, exit" (:exit positive)))
(if (or (seq failures) (seq selection-failures) (not (zero? (:exit positive))))
  (do (println "run3 controls: FAIL") (System/exit 1))
  (println (str "run3 controls: PASS (3 planted defects refused with the right class; "
                "by-run-id selection took 2 of 3 records from a shared file and left the "
                "other run's planted defect behind; real file passes)")))
