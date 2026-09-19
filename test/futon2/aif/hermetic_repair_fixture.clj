(ns futon2.aif.hermetic-repair-fixture
  "Suite fixture that makes production repair/trip stores unreachable."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [is testing]]
            [futon2.aif.repair-obligation :as repair]
            [futon2.aif.tripwire :as tripwire])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def production-repair-root repair/default-root)
(def production-trip-root tripwire/default-trip-root)

(defn- temp-root [prefix]
  (.getPath (.toFile (Files/createTempDirectory
                      prefix (make-array FileAttribute 0)))))

(defn- file-set [root]
  (let [base (io/file root)]
    (if (.exists base)
      (->> (file-seq base)
           (filter #(.isFile %))
           (map #(.toString (.relativize (.toPath base) (.toPath %))))
           set)
      #{})))

(defn with-hermetic-stores
  "Run a test namespace against temporary default stores and prove that the
  production file sets did not change. This catches any path that accidentally
  falls through an unstubbed default-arity repair or trip-report call."
  [run-tests]
  (let [repair-before (file-set production-repair-root)
        trips-before (file-set production-trip-root)
        repair-root (temp-root "wm-repair-suite-")
        trip-root (temp-root "wm-trip-suite-")]
    (with-redefs [repair/default-root repair-root
                  tripwire/default-trip-root trip-root]
      (run-tests))
    (testing "the suite cannot deposit production repair or trip artifacts"
      (is (= repair-before (file-set production-repair-root)))
      (is (= trips-before (file-set production-trip-root))))))

(defn runner-repair-options
  "Explicit temporary store callbacks also protect direct REPL/debug callers,
  where clojure.test namespace fixtures are not installed."
  []
  (let [root (temp-root "wm-runner-repair-")]
    {:repair-root root
     :tripwire/report-root (str (io/file root "trips"))
     :tripwire/repair-record-fn (partial repair/record-system-failure! root)
     :repair-system-record-fn (partial repair/record-system-failure! root)
     :repair-record-fn (partial repair/record-review-failure! root)
     :repair-open-fn (partial repair/open-obligations root)
     :repair-implement-fn (partial repair/record-implementation! root)
     :repair-resolve-fn (partial repair/resolve! root)
     :repair-supersede-fn (partial repair/supersede! root)}))
