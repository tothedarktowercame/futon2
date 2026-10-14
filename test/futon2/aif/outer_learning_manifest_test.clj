(ns futon2.aif.outer-learning-manifest-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.outer-learning-manifest :as manifest]))

(deftest publication-is-atomic-idempotent-and-drift-refusing
  (let [dir (.toFile (java.nio.file.Files/createTempDirectory
                      "outer-learning-manifest"
                      (make-array java.nio.file.attribute.FileAttribute 0)))
        record (io/file dir "tick-run-record-r1.edn")
        manifest-path (.getPath (io/file dir "manifest.edn"))]
    (spit record (pr-str {:run/id "r1" :click/id "c1"}))
    (is (= :recorded (:status (manifest/record! manifest-path (.getPath record)))))
    (is (= :already-recorded
           (:status (manifest/record! manifest-path (.getPath record)))))
    (is (= 1 (count (:entries (edn/read-string (slurp manifest-path))))))
    (spit record (pr-str {:run/id "r1" :click/id "c1" :mutated true}))
    (is (= :outer-learning-record-drift
           (try (manifest/record! manifest-path (.getPath record))
                nil
                (catch clojure.lang.ExceptionInfo e (:kind (ex-data e))))))))
