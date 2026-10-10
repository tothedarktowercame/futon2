(ns wm-seed-replay-inputs
  "Create the isolated, point-in-time mutable-data input tree for WM replay.

  This script intentionally requires no Futon namespaces: it runs before the
  replay JVM loads data-owning namespaces. The destination must not exist."
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.pprint :as pp]
            [clojure.string :as str])
  (:import [java.time Instant]))

(def input-paths
  ["wm-repair-obligations"
   "wm-pattern-feedback"
   "wm-habit"
   "wm-learning-trials"
   "wm-observation-labels"
   "wm-scoring-cache"
   "wm-rationale"
   "wm-cascade-proposals"
   "wm-ticket-queue"
   "wm-runs"])

(defn file-census [root]
  (let [root (io/file root)]
    {:files (count (filter #(.isFile ^java.io.File %) (file-seq root)))
     :bytes (reduce + 0 (map #(.length ^java.io.File %)
                            (filter #(.isFile ^java.io.File %) (file-seq root))))}))

(defn relocate-root-references!
  "Relocate absolute data-root references retained by repair evidence.  Those
  paths are part of its validation contract, so a byte-for-byte directory copy
  is not a usable isolated snapshot."
  [directory source-root destination-root]
  (reduce
   (fn [n file]
     (if-not (.isFile ^java.io.File file)
       n
       (let [before (slurp file)
             after (str/replace before source-root destination-root)]
         (if (= before after)
           n
           (do (spit file after) (inc n))))))
   0
   (file-seq (io/file directory))))

(defn seed! [source-root destination]
  (let [source (.getCanonicalFile (io/file source-root))
        dest (.getCanonicalFile (io/file destination))]
    (when (.exists dest)
      (throw (ex-info "Replay snapshot destination already exists"
                      {:failure-kind :replay-snapshot-destination-exists
                       :destination (.getPath dest)})))
    (.mkdirs dest)
    (let [entries
          (mapv
           (fn [relative]
             (let [src (io/file source relative)
                   dst (io/file dest relative)]
               (if-not (.exists src)
                 {:path relative :status :absent}
                 (let [source-before (file-census src)
                       result (shell/sh "cp" "-a" "--reflink=auto"
                                        (.getPath src) (.getPath dst))]
                   (when-not (zero? (:exit result))
                     (throw (ex-info "Failed to copy replay input"
                                     {:path relative :result result})))
                   (let [source-after (file-census src)]
                     (when-not (= source-before source-after)
                       (throw (ex-info "Production input changed while snapshotting"
                                       {:failure-kind :replay-snapshot-source-changed
                                        :path relative
                                        :before source-before :after source-after})))
                     (merge {:path relative :status :copied
                             :source source-before}
                            (file-census dst)))))))
           input-paths)
          relocated (relocate-root-references!
                     (io/file dest "wm-repair-obligations")
                     (.getPath source) (.getPath dest))
          manifest {:schema :wm/production-input-snapshot-v1
                    :created-at (str (Instant/now))
                    :source-root (.getPath source)
                    :destination (.getPath dest)
                    :relocated-root-reference-files relocated
                    :entries entries}
          marker (io/file dest ".wm-production-input-snapshot.edn")]
      (spit marker (with-out-str (pp/pprint manifest)))
      manifest)))

(defn -main [& args]
  (try
    (let [[source destination] args]
      (when-not (and source destination)
        (throw (ex-info "Usage: SOURCE-DATA-ROOT DESTINATION" {})))
      (pp/pprint (seed! source destination)))
    (finally
      ;; clojure.java.shell uses agent thread pools, which otherwise keep this
      ;; one-shot pre-JVM snapshot process alive after the copy is complete.
      (shutdown-agents))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
