(ns futon2.aif.ticket-publication-io
  "Durable file publication for tickets and their queue declaration."
  (:require [clojure.java.io :as io]
            [futon2.aif.load-identity :as identity])
  (:import [java.nio ByteBuffer]
           [java.nio.channels FileChannel]
           [java.nio.file Files LinkOption OpenOption StandardOpenOption StandardCopyOption]))

(identity/register! *ns* *file*)

(defn checked-file! [path]
  (let [file (.getAbsoluteFile (io/file path))]
    (when (or (Files/isSymbolicLink (.toPath file))
              (not= (.normalize (.toPath file)) (.toPath (.getCanonicalFile file))))
      (throw (ex-info "Publication path traverses a link" {:kind :publication-path-refused :path (str file)})))
    (when (and (.exists file) (not (.isFile file)))
      (throw (ex-info "Publication destination is not a file" {:kind :publication-path-refused :path (str file)})))
    file))

(defn force-directory! [file]
  (with-open [channel (FileChannel/open (.toPath (io/file file)) (make-array OpenOption 0))]
    (.force channel true)))

(defn publish-text!
  "Force a complete temporary file before publishing. With replace? false,
   hard-link creation is atomic and cannot overwrite an existing ticket.
   A process crash can leave an unreferenced temporary file, never half a ticket."
  [path text replace?]
  (let [file (checked-file! path)]
    (io/make-parents file)
    (checked-file! file)
    (let [parent (.getParentFile file)
          temporary (Files/createTempFile (.toPath parent) ".ticket-publication-" ".tmp"
                                          (make-array java.nio.file.attribute.FileAttribute 0))]
      (try
        (with-open [channel (FileChannel/open temporary (into-array OpenOption [StandardOpenOption/WRITE]))]
          (let [buffer (ByteBuffer/wrap (.getBytes ^String text "UTF-8"))]
            (while (.hasRemaining buffer) (.write channel buffer)))
          (.force channel true))
        (if replace?
          (Files/move temporary (.toPath file)
                      (into-array StandardCopyOption [StandardCopyOption/ATOMIC_MOVE StandardCopyOption/REPLACE_EXISTING]))
          (try
            (Files/createLink (.toPath file) temporary)
            (catch java.nio.file.FileAlreadyExistsException _
              (checked-file! file)
              (when-not (Files/isRegularFile (.toPath file) (into-array LinkOption [LinkOption/NOFOLLOW_LINKS]))
                (throw (ex-info "Publication destination is not regular" {:kind :publication-path-refused}))))))
        (force-directory! parent)
        (.getPath file)
        (finally (Files/deleteIfExists temporary))))))
