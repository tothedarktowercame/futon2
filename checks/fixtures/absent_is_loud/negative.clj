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

;; same body, but the docstring DECLARES the absence result: conformant
(defn safe-read
  "Optional input: returns nil when the file is absent or unreadable."
  [path]
  (try
    (when (.exists (io/file path))
      (edn/read-string (slurp path)))
    (catch Exception _ nil)))

(def negative-safe-named
  (safe-read "/home/joe/code/futon2/checks/fixtures/absent_is_loud/missing-too.edn"))

(defn marker-input? [x]
  (and (map? x) (contains? x :missing)))

(defn loud-marker-read [path]
  (if (.exists (io/file path))
    (edn/read-string (slurp path))
    {:missing path}))

(defn threading-caller [path]
  (let [result (loud-marker-read path)]
    {:input-status result}))

(def negative-marker-threaded
  (threading-caller
   "/home/joe/code/futon2/checks/fixtures/absent_is_loud/missing-marker.edn"))

(def ^:dynamic *input-status* (atom []))

(defn record-marker! [result]
  (swap! *input-status*
         (fn [issues]
           (cond
             (:missing result) (conj issues {:kind :missing})
             (:unreadable result) (conj issues {:kind :unreadable})
             :else issues))))

(defn recording-marker-read [path]
  (let [result (if (.exists (io/file path))
                 (edn/read-string (slurp path))
                 {:missing path})]
    (record-marker! result)
    result))

(defn recorded-swallowing-caller [path]
  (let [result (recording-marker-read path)]
    (when-not (marker-input? result)
      result)))

(def negative-recorded-then-substituted
  (recorded-swallowing-caller
   "/home/joe/code/futon2/checks/fixtures/absent_is_loud/recorded-missing-marker.edn"))
