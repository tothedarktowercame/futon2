#!/usr/bin/env bb
;; The extractor is the namespace futon2.wm.extract-outcomes
;; (scripts/futon2/wm/extract_outcomes.clj) since M-wm-wiring row 2(b)
;; (claude-10, 2026-09-25). This shim keeps the script entry point and the
;; load-file callers (the proof2 packets' labs, the extractor test's
;; whole-output identity check) working unchanged: it loads the namespace
;; from beside this file, refers its public vars into the loading namespace,
;; and runs -main when invoked as a script.
;;
;; Run: clojure -M scripts/wm/extract-outcomes.clj <mission.md> [--cascades DIR] [--reference C.edn]
;;  or: bb scripts/wm/extract-outcomes.clj <mission.md> [--cascades DIR] [--reference C.edn]

(let [here (.getParentFile (.getAbsoluteFile (java.io.File. ^String *file*)))]
  (when-not (find-ns 'futon2.wm.extract-outcomes)
    (load-file (str (java.io.File. (.getParentFile here) "futon2/wm/extract_outcomes.clj")))))

(refer 'futon2.wm.extract-outcomes)

;; Run as a script only; a test that load-files this file (into its own ns)
;; gets the fns, not a System/exit. clojure -M <this-file> and bb both run in
;; the user ns; load-file from a test does not.
(when (and (= 'user (ns-name *ns*))
           (or *command-line-args*
               (and *file* (= *file* (System/getProperty "babashka.file")))))
  (apply (resolve (quote futon2.wm.extract-outcomes/-main)) *command-line-args*))
