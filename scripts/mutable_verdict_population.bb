#!/usr/bin/env bb
(ns scripts.mutable-verdict-population
  "Enumerate the mutable-input verification population by a reproducible,
   deliberately broad lexical criterion. This is discovery, not a verdict."
  (:require [babashka.fs :as fs]
            [clojure.string :as str]))

(def roots [(fs/path "checks")])
(def extensions #{"clj" "bb" "py"})
(def boundary-pattern
  #"(?s)(slurp|read_text|read_bytes|readAllBytes|list-dir|listFiles|file-seq|glob|git\b|subprocess|process/shell|ProcessBuilder|HttpClient|urlopen|/api/alpha)")

(defn executable-source? [path]
  (and (fs/regular-file? path)
       (contains? extensions (fs/extension path))
       (not (str/includes? (str path) "/fixtures/"))))

(defn population []
  (->> roots
       (mapcat fs/list-dir)
       (filter executable-source?)
       (filter #(re-find boundary-pattern (slurp (str %))))
       (map str)
       sort vec))

(defn report []
  (let [members (population)]
    {:schema :mutable-verdict-population/v1
     :criterion {:roots (mapv str roots)
                 :top-level-only true
                 :extensions (sort extensions)
                 :fixtures-excluded true
                 :boundary-pattern (str boundary-pattern)}
     :count (count members)
     :members members}))

(when (= *file* (System/getProperty "babashka.file"))
  (prn (report)))
