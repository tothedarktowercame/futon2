;; gate_2g_provenance.clj — E-live-loop-2 gate 2g (standing evidence check).
;; PASS iff (a) the seam is ON by default (*escrow-replay?* true from source —
;; the persistent 2g fix); (b) the real escrow loads clean (pins re-assert);
;; (c) a trace record in the escrow-witness trace dir carries an
;; :act-gate-verdicts entry whose ΔG leg came from the PINNED escrow:
;; :coverage-score-source :fold-escrow, :coverage-score-delta equal (1e-9) to the named deposit's
;; stored :eval :coverage-score-delta, verdict :pass. The witness dir is separate from the
;; live daily trace ON PURPOSE: the WM reads γ-state back from its own trace;
;; witness records must never sit in that read-back path.
;; Run: cd /home/joe/code/futon2 && clojure -M scripts/gate_2g_provenance.clj
(ns gate-2g-provenance
  (:require [clojure.java.io :as io]
            [futon2.aif.close-loop :as cl]
            [futon2.aif.fold-escrow :as esc]
            [futon2.aif.trace :as trace]))

(def witness-dir "/home/joe/code/futon2/data/wm-trace-escrow-witness")

(defn- witness-records []
  (for [f (.listFiles (io/file witness-dir))
        :let [nm (.getName f)
              [_ date] (re-matches #"wm-trace-(\d{4}-\d{2}-\d{2})\.edn" nm)]
        :when date
        r (trace/read-trace :dir witness-dir :date-str date)]
    r))

(defn -main []
  (when-not (true? cl/*escrow-replay?*)
    (println "GATE 2g FAIL — *escrow-replay?* is not ON by default (the persistent fix regressed)")
    (System/exit 1))
  (let [{:keys [deposits rejected]} (esc/load-deposits)]
    (when (or (seq rejected) (empty? deposits))
      (println "GATE 2g FAIL — escrow not clean:" (count deposits) "deposit(s)," (mapv :reason rejected))
      (System/exit 1))
    (let [stored (into {} (map (juxt :fold-turn/id #(get-in % [:eval :coverage-score-delta]))) deposits)
          hits (for [r (witness-records)
                     v (:act-gate-verdicts r)
                     :when (and (= :fold-escrow (:coverage-score-source v))
                                (= :pass (:verdict v))
                                (number? (:coverage-score-delta v))
                                (when-let [dg (stored (:fold-turn/id v))]
                                  (< (Math/abs (- (double (:coverage-score-delta v)) (double dg))) 1e-9)))]
                 (assoc v :trace/timestamp (:timestamp r)))]
      (if (seq hits)
        (do (doseq [h hits]
              (println (format "  %s @ %s: dG %s from :fold-escrow, verdict :pass"
                               (:fold-turn/id h) (:trace/timestamp h) (:coverage-score-delta h))))
            (println "GATE 2g PASS —" (count hits)
                     "trace verdict(s) with escrow provenance matching stored deposit dG")
            (System/exit 0))
        (do (println "GATE 2g FAIL — no trace verdict with :coverage-score-source :fold-escrow matching a stored deposit dG in" witness-dir)
            (System/exit 1))))))

(-main)
