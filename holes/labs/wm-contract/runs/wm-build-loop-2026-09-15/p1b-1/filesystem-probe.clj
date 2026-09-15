(import '(java.nio.file Files StandardOpenOption StandardCopyOption)
        '(java.nio.channels FileChannel))
(let [d (Files/createTempDirectory "wm-store-probe-" (make-array java.nio.file.attribute.FileAttribute 0))
      a (.resolve d "a.edn") b (.resolve d "b.edn")]
  (try
    (Files/write a (.getBytes "{}\n" "UTF-8")
                 (into-array java.nio.file.OpenOption [StandardOpenOption/CREATE_NEW StandardOpenOption/WRITE]))
    (with-open [f (FileChannel/open a (into-array java.nio.file.OpenOption [StandardOpenOption/WRITE]))]
      (.force f true))
    (Files/move a b (into-array java.nio.file.CopyOption [StandardCopyOption/ATOMIC_MOVE]))
    (with-open [f (FileChannel/open d (into-array java.nio.file.OpenOption [StandardOpenOption/READ]))]
      (.force f true))
    ;; Real replacement of an existing destination, as required for HEAD.
    (Files/write a (.getBytes "{:replacement true}\n" "UTF-8")
                 (into-array java.nio.file.OpenOption [StandardOpenOption/CREATE_NEW StandardOpenOption/WRITE]))
    (with-open [f (FileChannel/open a (into-array java.nio.file.OpenOption [StandardOpenOption/WRITE]))]
      (.force f true))
    (Files/move a b (into-array java.nio.file.CopyOption [StandardCopyOption/ATOMIC_MOVE]))
    (with-open [f (FileChannel/open d (into-array java.nio.file.OpenOption [StandardOpenOption/READ]))]
      (.force f true))
    (assert (= "{:replacement true}\n" (slurp (.toFile b))))
    (println {:jdk (System/getProperty "java.version")
              :provider (str (.provider (.getFileSystem d)))
              :file-store (str (Files/getFileStore d))
              :file-force :ok :atomic-move :ok :atomic-replacement :ok :directory-force :ok})
    (finally (Files/deleteIfExists a) (Files/deleteIfExists b) (Files/delete d))))
