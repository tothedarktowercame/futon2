(ns futon2.aif.wm-seed-replay-inputs-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [wm-seed-replay-inputs :as seed])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(deftest snapshot-is-created-before-the-destination-can-be-used
  (let [base (.toFile (Files/createTempDirectory
                       "wm-seed-test-" (make-array FileAttribute 0)))
        source (io/file base "source")
        destination (io/file base "destination")]
    (doseq [relative seed/input-paths]
      (.mkdirs (io/file source relative)))
    (spit (io/file source "wm-repair-obligations" "finding.edn")
          (pr-str {:path (.getPath (io/file source "wm-repair-obligations" "e.edn"))}))
    (let [manifest (seed/seed! source destination)
          marker (edn/read-string
                  (slurp (io/file destination
                                  ".wm-production-input-snapshot.edn")))]
      (is (= :wm/production-input-snapshot-v1 (:schema manifest)))
      (is (= manifest marker))
      (is (= 1 (:relocated-root-reference-files manifest)))
      (is (= {:path (.getPath (io/file destination "wm-repair-obligations" "e.edn"))}
             (edn/read-string (slurp (io/file destination
                                              "wm-repair-obligations" "finding.edn")))))
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"already exists"
                            (seed/seed! source destination))))))
