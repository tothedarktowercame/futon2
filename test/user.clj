(ns user
  (:import [java.nio.file Files Path]
           [java.nio.file.attribute FileAttribute]
           [java.util Comparator]))

(def fixtures-root
  (Path/of "/home/joe/code/storage/test-fixtures" (make-array String 0)))

(defn- delete-tree! [^Path root]
  (when (Files/exists root (make-array java.nio.file.LinkOption 0))
    (with-open [paths (Files/walk root (make-array java.nio.file.FileVisitOption 0))]
      (doseq [path (iterator-seq
                    (.iterator (.sorted paths (Comparator/reverseOrder))))]
        (Files/deleteIfExists ^Path path)))))

(defn- old-entry?
  "False for an entry that vanished between listing and stat: concurrent test
  JVMs (the warrant runner uses -j 3) create and remove entries under the same
  root, and a sweep must never fail the JVM that runs it."
  [^Path path cutoff-ms]
  (try
    (< (.toMillis (Files/getLastModifiedTime
                   path (make-array java.nio.file.LinkOption 0)))
       cutoff-ms)
    (catch java.nio.file.NoSuchFileException _ false)))

(defn- sweep-old-fixtures! []
  (Files/createDirectories fixtures-root (make-array FileAttribute 0))
  (when-not (= "1" (System/getenv "FUTON2_KEEP_FIXTURES"))
    (let [cutoff-ms (- (System/currentTimeMillis) (* 2 60 60 1000))]
      (with-open [entries (Files/list fixtures-root)]
        (doseq [entry (iterator-seq (.iterator entries))
                :when (old-entry? entry cutoff-ms)]
          (try (delete-tree! entry)
               (catch java.nio.file.NoSuchFileException _ nil)))))))

(sweep-old-fixtures!)
