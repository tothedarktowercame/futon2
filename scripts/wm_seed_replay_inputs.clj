(ns wm-seed-replay-inputs
  "Create the isolated, point-in-time mutable-data input tree for WM replay.

  This script intentionally requires no Futon namespaces: it runs before the
  replay JVM loads data-owning namespaces. The destination must not exist."
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.edn :as edn]
            [clojure.pprint :as pp]
            [clojure.string :as str])
  (:import [java.time Instant]))

(defn sha256-file [file]
  (let [digest (java.security.MessageDigest/getInstance "SHA-256")]
    (with-open [in (io/input-stream file)]
      (let [buf (byte-array 65536)]
        (loop []
          (let [n (.read in buf)]
            (when (pos? n)
              (.update digest buf 0 n)
              (recur))))))
    (apply str (map #(format "%02x" (bit-and 255 %)) (.digest digest)))))

(defn rebind-repair-evidence! [repair-root]
  (let [evidence-dir (io/file repair-root "verification-evidence")
        admission-dir (io/file repair-root "verifications")]
    (doseq [file (filter #(.isFile ^java.io.File %) (file-seq evidence-dir))]
      (let [record (edn/read-string (slurp file))
            finding (some-> record :finding :path io/file)
            record' (if (and finding (.isFile finding))
                      (assoc-in record [:finding :sha256] (sha256-file finding))
                      record)]
        (spit file (pr-str record'))))
    (doseq [file (filter #(.isFile ^java.io.File %) (file-seq admission-dir))]
      (let [record (edn/read-string (slurp file))
            artifact (some-> record :verification-artifact :path io/file)
            finding (some-> record :finding-artifact :path io/file)
            record' (cond-> record
                      (and artifact (.isFile artifact))
                      (assoc-in [:verification-artifact :sha256]
                                (sha256-file artifact))
                      (and finding (.isFile finding))
                      (assoc-in [:finding-artifact :sha256]
                                (sha256-file finding)))]
        (spit file (pr-str record'))))))

(defn input-paths-from-trace [trace-file]
  (with-open [reader (io/reader trace-file)]
    (->> (line-seq reader)
         (map edn/read-string)
         (keep (fn [{:keys [root path]}]
                 (let [root-path (.toPath (.getCanonicalFile (io/file root)))
                       path-path (.toPath (.getCanonicalFile (io/file path)))]
                   (when (.startsWith path-path root-path)
                     (some-> (.relativize root-path path-path)
                             .iterator iterator-seq first str)))))
         distinct sort vec)))

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

(defn seed! [source-root destination input-paths]
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
          _ (rebind-repair-evidence! (io/file dest "wm-repair-obligations"))
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
    (let [[source destination trace-file] args]
      (when-not (and source destination trace-file)
        (throw (ex-info "Usage: SOURCE-DATA-ROOT DESTINATION DATA-PATH-TRACE" {})))
      (pp/pprint (seed! source destination
                        (input-paths-from-trace trace-file))))
    (finally
      ;; clojure.java.shell uses agent thread pools, which otherwise keep this
      ;; one-shot pre-JVM snapshot process alive after the copy is complete.
      (shutdown-agents))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
