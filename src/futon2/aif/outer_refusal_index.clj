(ns futon2.aif.outer-refusal-index
  "Durable compact index publication for terminal War Machine records.

  The serving click appends one forced EDN line after its terminal record is
  published. Legacy backfill is an explicit tooling-JVM operation."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.outer-refusal-history :as history]
            [futon2.aif.ticket-publication-io :as publication])
  (:import [java.math BigInteger]
           [java.nio.channels FileChannel]
           [java.nio.file Files LinkOption OpenOption StandardCopyOption StandardOpenOption]
           [java.security MessageDigest]
           [java.util UUID]))

(def ^:private terminal-record-name
  #"^tick-run-record-\d{4}-\d{2}-\d{2}-.+\.edn$")

(defn sha256-file
  "Return the SHA-256 hex digest of FILE without retaining its bytes."
  [file]
  (let [digest (MessageDigest/getInstance "SHA-256")
        buffer (byte-array 65536)]
    (with-open [in (io/input-stream file)]
      (loop []
        (let [n (.read in buffer)]
          (when (pos? n)
            (.update digest buffer 0 n)
            (recur)))))
    (format "%064x" (BigInteger. 1 (.digest digest)))))

(defn append-entry!
  "Atomically append ENTRY as one UTF-8 line to INDEX-FILE.

  A separate lock serializes readers of the old index with publication. The
  repository's publication helper forces a complete replacement before its
  atomic rename, so a crash exposes either the old index or the old index plus
  the complete new line, never a partial line."
  [index-file entry]
  (let [file (.getCanonicalFile (io/file index-file))
        lock-file (io/file (.getParentFile file) (str (.getName file) ".lock"))
        line (str (pr-str entry) "\n")]
    (io/make-parents file)
    (when (or (Files/isSymbolicLink (.toPath file))
              (Files/isSymbolicLink (.toPath lock-file)))
      (throw (ex-info "Refusal index path is a symbolic link"
                      {:kind :outer/refusal-index-path-refused
                       :path (.getPath file)})))
    (with-open [channel (FileChannel/open
                         (.toPath lock-file)
                         (into-array OpenOption
                                     [StandardOpenOption/CREATE
                                      StandardOpenOption/WRITE
                                      LinkOption/NOFOLLOW_LINKS]))
                _lock (.lock channel)]
      (publication/publish-text!
       file
       (str (if (.isFile file) (slurp file) "") line)
       true))
    entry))

(defn append-run-record!
  "Append the compact projection of an already-published terminal RECORD."
  [dir record-path record]
  (let [path (.getCanonicalPath (io/file record-path))]
    (append-entry! (io/file dir history/index-name)
                   (history/index-entry record path (sha256-file path)))))

(defn- terminal-files [dir]
  (let [root (io/file dir)]
    (if (.isDirectory root)
      (->> (.listFiles root)
           (filter #(.isFile ^java.io.File %))
           (filter #(re-matches terminal-record-name (.getName ^java.io.File %)))
           (sort-by #(.getName ^java.io.File %)))
      [])))

(defn backfill!
  "Read legacy terminal records once and atomically publish a compact index.

  An unreadable legacy record produces a typed :unreadable row carrying its
  path, digest and parse error; it does not make every future click parse or
  reject that large historical file. OUTPUT must not already exist."
  [dir output]
  (let [target (.getCanonicalFile (io/file output))
        parent (.getParentFile target)
        temporary (io/file parent (str "." (.getName target) "."
                                            (UUID/randomUUID) ".tmp"))]
    (when (.exists target)
      (throw (ex-info "Refusal index backfill target exists"
                      {:kind :outer/refusal-index-exists
                       :path (.getPath target)})))
    (io/make-parents target)
    (try
      (with-open [writer (io/writer temporary)]
        (doseq [file (terminal-files dir)]
          (let [path (.getCanonicalPath file)
                pin (sha256-file file)
                row (try
                      (history/index-entry
                       (with-open [reader (java.io.PushbackReader. (io/reader file))]
                         (edn/read reader))
                       path pin)
                      (catch Exception e
                        {:schema :wm/outer-refusal-index-entry-v1
                         :status :unreadable
                         :record-path path
                         :record-sha256 pin
                         :error-class (.getName (class e))
                         :error (.getMessage e)}))]
            (.write writer (str (pr-str row) "\n")))))
      (Files/move (.toPath temporary) (.toPath target)
                  (into-array StandardCopyOption [StandardCopyOption/ATOMIC_MOVE]))
      (.getCanonicalPath target)
      (finally
        (Files/deleteIfExists (.toPath temporary))))))
