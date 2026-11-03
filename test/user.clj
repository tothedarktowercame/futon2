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

(defn- old-entry? [^Path path cutoff-ms]
  (< (.toMillis (Files/getLastModifiedTime
                 path (make-array java.nio.file.LinkOption 0)))
     cutoff-ms))

(defn- sweep-old-fixtures! []
  (Files/createDirectories fixtures-root (make-array FileAttribute 0))
  (when-not (= "1" (System/getenv "FUTON2_KEEP_FIXTURES"))
    (let [cutoff-ms (- (System/currentTimeMillis) (* 2 60 60 1000))]
      (with-open [entries (Files/list fixtures-root)]
        (doseq [entry (iterator-seq (.iterator entries))
                :when (old-entry? entry cutoff-ms)]
          (delete-tree! entry))))))

(sweep-old-fixtures!)
