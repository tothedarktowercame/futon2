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
(def positive (run-check src))
(doseq [f failures] (println f))
(when-not (zero? (:exit positive))
  (println "POSITIVE CONTROL FAILED: the real S1b file must pass, exit" (:exit positive)))
(if (or (seq failures) (not (zero? (:exit positive))))
  (do (println "run3 controls: FAIL") (System/exit 1))
  (println "run3 controls: PASS (3 planted defects refused with the right class; real file passes)"))
