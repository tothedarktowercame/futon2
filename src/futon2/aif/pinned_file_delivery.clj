(ns futon2.aif.pinned-file-delivery
  "Shared exact-byte delivery for producer-authored files. No publication."
  (:require [clojure.java.io :as io]
            [futon2.aif.load-identity :as identity]))

(defn contained-file [root path]
  (when (and (string? root) (string? path) (seq path))
    (let [root (.getCanonicalFile (io/file root))
          candidate (io/file path)
          file (.getCanonicalFile (if (.isAbsolute candidate) candidate (io/file root path)))]
      (when (and (not= root file) (.startsWith (.toPath file) (.toPath root))) file))))

(defn read-file
  "Read FILE once and verify the SHA of those exact bytes before decoding."
  [file expected]
  (if-not (and file (.isFile ^java.io.File file))
    {:status :refused :reason :file-absent}
    (try
      (let [bytes (java.nio.file.Files/readAllBytes (.toPath ^java.io.File file))
            actual (identity/sha256 bytes)]
        (if (= expected actual)
          {:status :file :text (String. bytes "UTF-8")
           :pin {:path (.getCanonicalPath ^java.io.File file) :sha256 actual}}
          {:status :refused :reason :sha-mismatch :expected expected :actual actual}))
      (catch java.io.IOException e
        {:status :refused :reason :file-unreadable :message (ex-message e)}))))
