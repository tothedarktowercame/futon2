(ns fixtures.absent-is-loud.positive
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

(defn loud-read
  "Read one EDN file and throw if it is absent."
  [path]
  (if (.exists (io/file path))
    (edn/read-string (slurp path))
    (throw (ex-info "missing fixture file" {:path path}))))

(defn silent-read
  [path]
  (try
    (when (.exists (io/file path))
      (edn/read-string (slurp path)))
    (catch Exception _ nil)))

(defn optional-read
  "Optional fixture input: returns nil when absent."
  [path]
  (when (.exists (io/file path))
    (edn/read-string (slurp path))))

(def positive-loud
  (loud-read "/home/joe/code/futon2/checks/fixtures/absent_is_loud/present.edn"))

(def positive-silent
  (when-let [v (silent-read "/home/joe/code/futon2/checks/fixtures/absent_is_loud/missing.edn")]
    v))

(def positive-optional
  (optional-read "/home/joe/code/futon2/checks/fixtures/absent_is_loud/optional.edn"))

;; name-only claim of safety, no docstring: NOT a declaration (owner amendment at the gate)
(defn safe-read
  [path]
  (try
    (when (.exists (io/file path))
      (edn/read-string (slurp path)))
    (catch Exception _ nil)))

(def positive-safe-named
  (safe-read "/home/joe/code/futon2/checks/fixtures/absent_is_loud/missing-too.edn"))

(defn marker-input? [x]
  (and (map? x) (contains? x :missing)))

(defn loud-marker-read [path]
  (if (.exists (io/file path))
    (edn/read-string (slurp path))
    {:missing path}))

(defn swallowing-caller [path]
  (let [result (loud-marker-read path)]
    (when-not (marker-input? result)
      result)))

(def positive-marker-swallowed
  (swallowing-caller
   "/home/joe/code/futon2/checks/fixtures/absent_is_loud/missing-marker.edn"))

(defn another-loud-marker-read [path]
  (if (.exists (io/file path))
    (edn/read-string (slurp path))
    {:unreadable path}))

(defn another-swallowing-caller [path]
  (let [result (another-loud-marker-read path)]
    (if (marker-input? result)
      nil
      result)))

(def positive-another-marker-swallowed
  (another-swallowing-caller
   "/home/joe/code/futon2/checks/fixtures/absent_is_loud/another-missing-marker.edn"))
