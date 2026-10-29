(ns futon2.aif.reading-policy-class-history-index
  "Compact, coverage-checked class history for reading policies."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.outer-refusal-index :as durable])
  (:import [java.nio.file Files StandardCopyOption]
           [java.math BigInteger]
           [java.security DigestInputStream MessageDigest]
           [java.util UUID]))

(def index-name "reading-policy-class-history-index.ednl")
(def terminal-name #"^tick-run-record-.+\.edn$")
(def terminal-classes #{:focused :related :unrelated :stop-the-line})

(defn- q6-terminal [record]
  (some #(when (and (map? %) (= :wm/q6-terminal-v1 (:schema %))) %)
        (tree-seq coll? seq record)))

(defn index-entry [record record-path record-sha256]
  (let [terminal (q6-terminal record)
        key (get-in terminal [:choice :policy-key])
        class (get-in record [:run-ending-classification :class])]
    (cond-> {:schema :wm/reading-policy-class-history-index-entry-v1
             :status (if (and (= :present (get-in terminal [:choice :status]))
                              (vector? key) (terminal-classes class))
                       :indexed :not-applicable)
             :run-id (or (:run/id record) (:run-id record))
             :record-path (.getCanonicalPath (io/file record-path))
             :record-sha256 record-sha256}
      key (assoc :policy-key-sha256 (identity/digest key))
      class (assoc :class class))))

(defn append-run-record! [dir record-path record]
  (durable/append-entry! (io/file dir index-name)
                         (index-entry record record-path
                                      (durable/sha256-file record-path))))

(defn terminal-files [dir]
  (let [root (io/file dir)]
    (if (.isDirectory root)
      (->> (.listFiles root) (filter #(.isFile ^java.io.File %))
           (filter #(re-matches terminal-name (.getName ^java.io.File %)))
           (sort-by #(.getName ^java.io.File %)) vec)
      [])))

(defn read-index [dir]
  (let [file (io/file dir index-name)]
    (if-not (.isFile file)
      {:status :history-index-absent :index-path (.getCanonicalPath file)}
      (let [rows (with-open [reader (io/reader file)]
                   (mapv edn/read-string (line-seq reader)))
            records (mapv #(.getCanonicalPath ^java.io.File %) (terminal-files dir))
            indexed-paths (mapv :record-path rows)
            covered (set indexed-paths)
            missing (vec (remove covered records))
            unexpected (vec (remove (set records) indexed-paths))
            ordered? (= records indexed-paths)
            unreadable (filterv #(= :unreadable (:status %)) rows)]
        (if (or (seq missing) (seq unexpected) (not ordered?) (seq unreadable))
          {:status :history-index-incomplete :index-path (.getCanonicalPath file)
           :missing-records missing :unexpected-records unexpected
           :ordered-projection? ordered? :unreadable unreadable
           :covered-count (count covered) :record-count (count records)}
          {:status :complete :index-path (.getCanonicalPath file)
           :rows rows :covered-count (count covered) :record-count (count records)})))))

(defn history [dir policy-key window]
  (let [index (read-index dir)]
    (if-not (= :complete (:status index))
      (assoc index :value nil :history nil :window window)
      (let [digest (identity/digest policy-key)
            rows (->> (:rows index)
                      (filter #(and (= :indexed (:status %))
                                    (= digest (:policy-key-sha256 %))))
                      (take-last window) vec)]
        {:status :complete :window window :index-path (:index-path index)
         :coverage (select-keys index [:covered-count :record-count])
         :sources (mapv (fn [row]
                          {:path (:record-path row)
                           :record {:run/id (:run-id row)
                                    :terminal {:schema :wm/q6-terminal-v1
                                               :choice {:status :present
                                                        :policy-key policy-key}}
                                    :run-ending-classification {:class (:class row)}}})
                        rows)}))))

(defn backfill! [dir output]
  (let [target (.getCanonicalFile (io/file output))
        temporary (io/file (.getParentFile target)
                           (str "." (.getName target) "." (UUID/randomUUID) ".tmp"))]
    (when (.exists target)
      (throw (ex-info "Class history index target exists"
                      {:kind :history-index-exists :path (.getPath target)})))
    (io/make-parents target)
    (try
      (let [files (terminal-files dir)
            bytes-read (volatile! 0)]
      (with-open [writer (io/writer temporary)]
        (doseq [file files]
          (let [path (.getCanonicalPath file)
                digest (MessageDigest/getInstance "SHA-256")
                parsed (try
                         (with-open [stream (DigestInputStream. (io/input-stream file) digest)
                                     reader (java.io.PushbackReader. (io/reader stream))]
                           (let [record (edn/read reader)]
                             (loop [] (when-not (= -1 (.read reader)) (recur)))
                             record))
                         (catch Exception e e))
                _ (vswap! bytes-read + (.length file))
                pin (format "%064x" (BigInteger. 1 (.digest digest)))
                row (try
                      (if (instance? Exception parsed)
                        (throw parsed)
                        (index-entry parsed path pin))
                      (catch Exception e
                        {:schema :wm/reading-policy-class-history-index-entry-v1
                         :status :unreadable :record-path path :record-sha256 pin
                         :error-class (.getName (class e)) :error (.getMessage e)}))]
            (.write writer (str (pr-str row) "\n")))))
      (Files/move (.toPath temporary) (.toPath target)
                  (into-array StandardCopyOption [StandardCopyOption/ATOMIC_MOVE]))
      {:path (.getCanonicalPath target) :records (count files) :bytes-read @bytes-read})
      (finally (Files/deleteIfExists (.toPath temporary))))))
