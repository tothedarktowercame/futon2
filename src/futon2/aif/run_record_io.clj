(ns futon2.aif.run-record-io
  "Atomic storage and hydration for the production WM run-record format."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io])
  (:import [java.nio.file Files StandardCopyOption]
           [java.util UUID]))

(defn write-run-record-stream!
  "Atomically write RECORD using the same one-form EDN stream used by the
  full-loop runner. Returns the canonical target path."
  [run-record-dir run-id record]
  (let [dir (io/file run-record-dir)
        target (io/file dir (str "tick-run-record-" run-id ".edn"))
        tmp (io/file dir (str "." (.getName target) "." (UUID/randomUUID) ".tmp"))]
    (io/make-parents target)
    (spit tmp (str (pr-str record) "\n"))
    (Files/move (.toPath tmp) (.toPath target)
                (into-array StandardCopyOption
                            [StandardCopyOption/ATOMIC_MOVE
                             StandardCopyOption/REPLACE_EXISTING]))
    (.getCanonicalPath target)))

(defn hydrate-run-record [path]
  (edn/read-string (slurp (io/file path))))
