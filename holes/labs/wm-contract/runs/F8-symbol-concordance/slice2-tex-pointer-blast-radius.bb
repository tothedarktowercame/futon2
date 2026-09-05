#!/usr/bin/env bb
;; F8 leg 2 slice 2 -- MEASURING the widening the ledger's slice-1 evidence asks
;; for, before deciding whether to do it. The item is: pointer_check.bb:134
;; matches only .clj/.lean/.edn, so no glossary (.tex) pointer in
;; symbol-concordance.edn is machine-checked by the standing gate.
;;
;; The question is not whether .tex pointers SHOULD be checked -- they should --
;; but what adding the extension to a SHARED gate's regex does to the files
;; that gate already scans. That is a number, so it is measured here rather
;; than argued. Nothing is widened by this script; it counts.
;;
;; pointer_check.bb resolves a BARE FILENAME against a hand-maintained roots
;; allowlist, and that list has already fallen behind nine times (its own
;; comments, pointer_check.bb:32-136). Every pointer below whose directory is
;; not on that list would report "file not found" the moment the extension is
;; admitted -- wording that blames the ledger for the checker's coverage.
;;
;; LINE POINTERS RE-RESOLVED BY THE REVIEW. As written this comment cited
;; pointer_check.bb:118 for the extension regex and :19-118 for the allowlist,
;; which were right when it was written and wrong ninety minutes later: the
;; review's own widening of that file (symbol-concordance.edn added to
;; extra-paths, AIF_EXTRA) inserted sixteen lines above both. The regex is
;; now :134. A pointer into a file the same slice is editing goes stale inside
;; the slice.
(require '[clojure.edn :as edn] '[clojure.string :as str])
(def home (System/getProperty "user.home"))
(def files [(str home "/code/futon2/holes/labs/wm-contract/aif-equations.edn")
            (str home "/code/p4ng/empirics-futon/control-map-edges.edn")
            (str home "/code/futon2/holes/labs/wm-contract/worklist.edn")
            (str home "/code/futon2/holes/labs/wm-contract/symbol-concordance.edn")])
(doseq [f files]
  (let [d (edn/read-string {:default (fn [_ v] v)} (slurp f))
        texts (filter string? (tree-seq coll? seq d))
        hits (vec (distinct (for [t texts [m] (re-seq #"([A-Za-z0-9_.\-]+\.(?:tex|md|sh|py|bb)):(\d+)(?:-(\d+))?" t)] m)))]
    (printf "%-30s %d non-(clj|lean|edn) pointers%n" (last (str/split f #"/")) (count hits))
    (doseq [h (sort hits)] (println "   " h))))
