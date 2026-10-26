(ns futon2.aif.outer-learning-manifest-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.meta-field-observation :as field]
            [futon2.aif.outer-learning-manifest :as manifest]))

(deftest publication-is-atomic-idempotent-and-drift-refusing
  (let [dir (.toFile (java.nio.file.Files/createTempDirectory
                      "outer-learning-manifest"
                      (make-array java.nio.file.attribute.FileAttribute 0)))
        record (io/file dir "tick-run-record-r1.edn")
        manifest-path (.getPath (io/file dir "manifest.edn"))]
    (spit record (pr-str {:run/id "r1" :click/id "c1"}))
    (is (= :recorded (:status (manifest/record! manifest-path (.getPath record)))))
    (let [entry (first (:entries (edn/read-string (slurp manifest-path))))]
      (is (.isFile (io/file (get-in entry [:projection :path]))))
      (is (= (get-in entry [:projection :sha256])
             (field/sha256
              (java.nio.file.Files/readAllBytes
               (.toPath (io/file (get-in entry [:projection :path]))))))))
    (is (= :already-recorded
           (:status (manifest/record! manifest-path (.getPath record)))))
    (is (= 1 (count (:entries (edn/read-string (slurp manifest-path))))))
    (let [projection-path (get-in (edn/read-string (slurp manifest-path))
                                  [:entries 0 :projection :path])
          original (slurp projection-path)]
      (spit projection-path "{:drift true}\n")
      (is (= :outer-learning-projection-drift
             (try (manifest/record! manifest-path (.getPath record))
                  nil
                  (catch clojure.lang.ExceptionInfo e (:kind (ex-data e))))))
      (spit projection-path original))
    (spit record (pr-str {:run/id "r1" :click/id "c1" :mutated true}))
    (is (= :outer-learning-record-drift
           (try (manifest/record! manifest-path (.getPath record))
                nil
                (catch clojure.lang.ExceptionInfo e (:kind (ex-data e))))))))
