(ns futon2.aif.outer-learning-manifest
  "Atomic append-only authority over run records admitted to outer learning."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.meta-field-observation :as field])
  (:import [java.nio.file Files StandardCopyOption]
           [java.util UUID]))

(def schema :wm/outer-learning-run-manifest-v1)
(def default-path
  (str (System/getProperty "user.home")
       "/code/futon2/data/wm-learning/run-record-manifest.edn"))

(defn read-manifest
  ([] (read-manifest default-path))
  ([path]
   (let [file (io/file path)]
     (if (.isFile file)
       (edn/read-string (slurp file))
       {:schema schema :entries []}))))

(defn- valid? [manifest]
  (and (= schema (:schema manifest))
       (vector? (:entries manifest))
       (every? #(and (string? (:path %))
                     (boolean (re-matches #"[0-9a-f]{64}" (:sha256 %))))
               (:entries manifest))))

(defn- atomic-write! [path value]
  (let [target (io/file path)
        tmp (io/file (.getParentFile target)
                     (str "." (.getName target) "." (UUID/randomUUID) ".tmp"))]
    (io/make-parents target)
    (spit tmp (str (pr-str value) "\n"))
    (Files/move (.toPath tmp) (.toPath target)
                (into-array StandardCopyOption
                            [StandardCopyOption/ATOMIC_MOVE
                             StandardCopyOption/REPLACE_EXISTING]))
    (.getCanonicalPath target)))

(defn record!
  "Pin RUN-RECORD-PATH. Repeating the identical entry is idempotent; changing
  bytes behind an existing path refuses rather than rewriting history."
  ([run-record-path] (record! default-path run-record-path))
  ([manifest-path run-record-path]
   (let [record-file (io/file run-record-path)]
     (when-not (.isFile record-file)
       (throw (ex-info "Outer-learning run record is unavailable"
                       {:kind :outer-learning-record-unavailable
                        :path run-record-path})))
     (let [canonical (.getCanonicalPath record-file)
           sha (field/sha256 (Files/readAllBytes (.toPath record-file)))
           entry {:path canonical :sha256 sha}
           current (read-manifest manifest-path)]
       (when-not (valid? current)
         (throw (ex-info "Outer-learning manifest is invalid"
                         {:kind :outer-learning-manifest-invalid
                          :path manifest-path})))
       (if-let [existing (some #(when (= canonical (:path %)) %) (:entries current))]
         (if (= entry existing)
           {:schema schema :status :already-recorded :entry entry
            :manifest-path manifest-path}
           (throw (ex-info "Outer-learning run record drifted after publication"
                           {:kind :outer-learning-record-drift
                            :existing existing :observed entry})))
         (let [next (update current :entries conj entry)]
           (atomic-write! manifest-path next)
           {:schema schema :status :recorded :entry entry
            :manifest-path manifest-path
            :manifest-sha256 (field/digest next)}))))))
