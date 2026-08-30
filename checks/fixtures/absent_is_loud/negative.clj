(ns fixtures.absent-is-loud.negative
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

(defn loud-read
  "Read one EDN file and throw if it is absent."
  [path]
  (if (.exists (io/file path))
    (edn/read-string (slurp path))
    (throw (ex-info "missing fixture file" {:path path}))))

(defn fixed-read
  [path]
  (if (.exists (io/file path))
    (edn/read-string (slurp path))
    (throw (ex-info "missing fixture file" {:path path}))))

(defn optional-read
  "Optional fixture input: returns nil when absent."
  [path]
  (when (.exists (io/file path))
    (edn/read-string (slurp path))))

(def negative-loud
  (loud-read "/home/joe/code/futon2/checks/fixtures/absent_is_loud/present.edn"))

(def negative-fixed
  (fixed-read "/home/joe/code/futon2/checks/fixtures/absent_is_loud/missing.edn"))

(def negative-optional
  (optional-read "/home/joe/code/futon2/checks/fixtures/absent_is_loud/optional.edn"))
