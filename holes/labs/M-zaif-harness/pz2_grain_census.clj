#!/usr/bin/env bb
;; PZ2 — ledger grain census (M-zaif-harness). Question: does γ(cascade)
;; have enough events per cell on the real record, or does v0 start
;; coarser? Event source = the PZ1 correction corpus (final truth for the
;; labeled 120; the 153-hit scan scaled by measured precision for the
;; full window). Cell attribution = mission tokens (M-/E-/C-) present in
;; the turn context — the same signal the autoclock uses.
(require '[clojure.edn :as edn] '[clojure.string :as str])

(def sheet (:items (edn/read-string (slurp "pz1-labeling-sheet.edn"))))
(def truth (:rows (edn/read-string (slurp "pz1-final-truth.edn"))))
(def scan (edn/read-string (slurp "pz1-sample-scan.edn")))

(def ctx-of (into {} (map (fn [i] [(:id i) (:context i)])) sheet))

(defn missions [text]
  (distinct (map first (re-seq #"\b([MEC]-[a-z0-9][a-z0-9-]{2,})\b" (str text)))))

;; --- labeled trues: events per mission cell ---
(def true-rows (filter :true? truth))
(def cell-counts
  (frequencies (mapcat (fn [r] (or (seq (missions (ctx-of (:id r)))) ["<no-mission-token>"]))
                       true-rows)))
(println "== labeled true corrections (n=" (count true-rows) ") per mission cell ==")
(doseq [[m n] (sort-by (comp - val) cell-counts)] (println (format "  %3d  %s" n m)))
(let [attributed (remove #(= "<no-mission-token>" (key %)) cell-counts)]
  (println (format "cells: %d | cells with >=3 events: %d | >=5: %d | unattributed rows: %d"
                   (count attributed)
                   (count (filter #(>= (val %) 3) attributed))
                   (count (filter #(>= (val %) 5) attributed))
                   (get cell-counts "<no-mission-token>" 0))))

;; --- full-window projection: 153 hits x .417 precision, per mission ---
(def hits (:hits scan))
(println "\n== full-window projection (153 lexicon hits x .417) ==")
(if (seq hits)
  (let [hc (frequencies (mapcat (fn [h] (or (seq (missions (or (:snippet h) "")))
                                            ["<no-mission-token>"])) hits))
        proj (->> hc (map (fn [[m n]] [m (* n 0.417)])) (sort-by (comp - second)))]
    (doseq [[m n] (take 12 proj)] (println (format "  %5.1f  %s" (double n) m)))
    (println (format "cells with projected >=2 true events: %d of %d"
                     (count (filter #(>= (second %) 2.0) proj)) (count proj))))
  (println "  (scan file has no :hits key — keys:" (keys scan) ")"))
