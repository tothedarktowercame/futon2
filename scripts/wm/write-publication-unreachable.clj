#!/usr/bin/env bb
;; H-PUBLISH-A2: write the 68 :wm/publication-unreachable-v1 markers for the
;; resolutions H-PUBLISH-D (5cbf0031) found unpublishable on
;; tick-run-record-2026-09-24-1790225596. Ruling (Joe, 2026-09-24): the
;; marker, not dismissal.
;;
;; One-shot. Refuses: any id whose class is not in this table (the writer
;; itself refuses any id with no resolutions/ record), any existing marker
;; (write-new! semantics). Writes only under
;; data/wm-repair-obligations/publication-unreachable/ — the data/ write
;; authorization of the H-PUBLISH-A2 packet covers this path only.
;;
;; Run: bb scripts/wm/write-publication-unreachable.clj
;;   or: clojure -M scripts/wm/write-publication-unreachable.clj

(require '[clojure.edn :as edn]
         '[futon2.aif.repair-obligation :as repair])

(def root "data/wm-repair-obligations")

(def table
  "The 68 ids and their H-PUBLISH-D classes. Read from the packet's table;
  every entry's resolutions/ record must exist or the write refuses."
  (edn/read-string (slurp "scripts/wm/publication-unreachable-table.edn")))

(def now (str (java.time.Instant/now)))

(doseq [[id class] (sort table)]
  (repair/write-publication-unreachable!
   root id
   {:schema :wm/publication-unreachable-v1
    :repair/id id
    :class class
    :reason ":resolution-context-unavailable"
    :ground "H-PUBLISH-D 5cbf0031"
    :ruled-by "joe"
    :ruled-at "2026-09-24"
    :written-by "kimi-2"
    :written-at now}))

(println (count table) "markers written")
