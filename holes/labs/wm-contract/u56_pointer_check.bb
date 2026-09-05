#!/usr/bin/env bb
;; U56 -- the per-artifact pointer check for C511-repair-or-elaborate.md.
;;
;;   bb u56_pointer_check.bb
;;
;; Every file:line or file:A-B pointer in C511 must resolve: the file exists,
;; A <= B, and both are within the file's line count. Exit 1 on any failure.
;;
;; TWO RESOLUTION CONVENTIONS, and the second one is here for a reason. A
;; pointer is resolved FIRST as a path relative to ~/code (the convention
;; runtime_validation_check.bb:16-27 adopted, and the reason it gives -- a
;; hand-maintained allowlist of directories matched against a BARE FILENAME
;; reports "file not found" for every directory nobody has cited before, and
;; pointer_check.bb's own header records six occasions of exactly that), and
;; only then against a small bare-filename allowlist for the sources this
;; artifact cites by short name. C511 cites files three levels inside
;; runs/<run-id>/, which the bare-filename form cannot reach.
(require '[clojure.java.io :as io] '[clojure.string :as str])

(def home (System/getProperty "user.home"))
(def code (str home "/code/"))
(def futon2 (str code "futon2/"))
(def bare-roots
  [(str futon2 "src/futon2/aif/") (str futon2 "scripts/futon2/report/")
   (str futon2 "scripts/futon2/") (str futon2 "scripts/") (str futon2 "src/futon2/")
   (str futon2 "checks/") (str futon2 "holes/labs/wm-contract/")
   (str code "mathlib4/DarkTower/WarMachine/") (str code "p4ng/empirics-futon/")])

(def artifact (str futon2 "holes/labs/wm-contract/C511-repair-or-elaborate.md"))
(def ptr-re #"([A-Za-z0-9_][A-Za-z0-9_./-]*\.(?:clj|bb|sh|edn|md|json|lean)):(\d+)(?:-(\d+))?")

(defn resolve-file [f]
  (let [as-repo-rel (io/file futon2 f)
        as-code-rel (io/file code f)]
    (cond
      (.isFile as-repo-rel) as-repo-rel
      (.isFile as-code-rel) as-code-rel
      :else (first (for [r bare-roots :let [x (io/file r f)] :when (.isFile x)] x)))))

(defn line-count [^java.io.File f]
  (with-open [r (io/reader f)] (count (line-seq r))))

(let [text (slurp artifact)
      ptrs (vec (distinct (map first (re-seq ptr-re text))))
      bad (vec (for [p ptrs
                     :let [[_ f a b] (re-matches #"(.+):(\d+)(?:-(\d+))?" p)
                           a (Long/parseLong a)
                           b (some-> b Long/parseLong)
                           file (resolve-file f)]
                     :let [problem (cond (nil? file) "file not found"
                                         (and b (> a b)) "inverted range"
                                         (< a 1) "line 0"
                                         (> (or b a) (line-count file))
                                         (format "past EOF (file has %d lines)" (line-count file)))]
                     :when problem]
                 [p problem]))]
  (println (format "u56_pointer_check: %d distinct file:line pointers in C511, %d unresolved"
                   (count ptrs) (count bad)))
  (doseq [[p why] bad] (println "  UNRESOLVED" p "--" why))
  (System/exit (if (seq bad) 1 0)))
