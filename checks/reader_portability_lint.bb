#!/usr/bin/env bb
(ns checks.reader-portability-lint
  (:require [babashka.fs :as fs]
            [clojure.string :as str]))

(def default-roots ["scripts" "checks"])
(def source-read-annotation
  #"reader-portability:\s*allow-source-read\s+reason=\S+")
(def bare-file-read
  #"(?s)(?<![A-Za-z0-9_./-])read-string\s*\(\s*slurp\b")

(defn source-files [roots]
  (->> roots
       (mapcat #(fs/glob % "**{.clj,.bb}"))
       (remove #(str/includes? (str %) "/fixtures/reader_portability/"))
       (sort-by str)
       vec))

(defn line-number [text offset]
  (inc (count (re-seq #"\n" (subs text 0 offset)))))

(defn annotated-source-read? [text offset]
  (let [before (subs text 0 offset)
        nearby (->> (str/split-lines before) (take-last 3) (str/join "\n"))]
    (boolean (re-find source-read-annotation nearby))))

(defn findings-in [path]
  (let [text (slurp (str path))
        matcher (re-matcher bare-file-read text)]
    (loop [out []]
      (if (.find matcher)
        (let [offset (.start matcher)]
          (recur (cond-> out
                   (not (annotated-source-read? text offset))
                   (conj {:path (str path)
                          :line (line-number text offset)
                          :finding :source-reader-at-persisted-boundary
                          :required 'clojure.edn/read-string}))))
        out))))

(defn scan [paths]
  (vec (mapcat findings-in paths)))

(defn -main [& args]
  (let [mode (first args)
        bare-fixture "checks/fixtures/reader_portability/bare_persisted_read.clj"
        source-fixture "checks/fixtures/reader_portability/exempt_source_read.clj"]
    (case mode
      "--control-bare"
      (let [findings (scan [bare-fixture])]
        (if (= 1 (count findings))
          (do (println "reader-portability-lint: control PASS (bare persisted read rejected) exit-convention=0-pass/1-findings/2-control-slipped")
              0)
          (do (println "reader-portability-lint: control FAIL (bare persisted read slipped)" (pr-str findings))
              2)))

      "--control-exempt-source"
      (let [findings (scan [source-fixture])]
        (if (empty? findings)
          (do (println "reader-portability-lint: control PASS (reasoned source read exempted) exit-convention=0-pass/1-findings/2-control-slipped")
              0)
          (do (println "reader-portability-lint: control FAIL (source exemption rejected)" (pr-str findings))
              2)))

      (let [paths (source-files default-roots)
            findings (scan paths)]
        (println (pr-str {:files-scanned (count paths)
                          :findings findings
                          :count (count findings)}))
        (println (str "reader-portability-lint: "
                      (if (empty? findings) "PASS" "FAIL")
                      " findings=" (count findings)
                      " exit-convention=0-pass/1-findings/2-control-slipped"))
        (if (empty? findings) 0 1)))))

(System/exit (apply -main *command-line-args*))
