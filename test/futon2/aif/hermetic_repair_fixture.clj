(ns futon2.aif.hermetic-repair-fixture
  "Suite fixture that makes production repair/trip stores unreachable."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [is testing]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.meta-injury-store :as meta-injury-store]
            [futon2.aif.meta-self-heal-production :as meta-self-heal]
            [futon2.aif.outer-learning-manifest :as outer-learning-manifest]
            [futon2.aif.repair-obligation :as repair]
            [futon2.aif.tripwire :as tripwire])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def production-repair-root repair/default-root)
(def production-trip-root tripwire/default-trip-root)
(def production-phase-log runner/default-phase-log)
(def production-meta-injury-root meta-injury-store/default-root)
(def production-meta-injury-ledger-path meta-self-heal/default-ledger-path)
(def production-meta-injury-transaction-path meta-self-heal/default-transaction-path)
(def production-learning-manifest-path outer-learning-manifest/default-path)
(def production-run-record-dir runner/default-run-record-dir)

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

(defn production-file-set [root]
  (into {} (for [file (file-seq (io/file root)) :when (.isFile file)]
             [(.toString (.relativize (.toPath (io/file root)) (.toPath file)))
              [(.length file) (slurp file)]])))

(defn- run-stream-length
  "The stream only ever grows, so its length catches an appended :started.
  file-set would not: it compares paths, and the stream's path never changes."
  [root]
  (.length (io/file root "run-stream.edn")))

(defn with-hermetic-stores
  "Run a test namespace against temporary default stores and prove that the
  production file sets did not change. This catches any path that accidentally
  falls through an unstubbed default-arity repair or trip-report call."
  [run-tests]
  (let [repair-before (file-set production-repair-root)
        trips-before (file-set production-trip-root)
        phase-bytes-before (.length (io/file production-phase-log))
        injuries-before (production-file-set production-meta-injury-root)
        ledger-before (production-file-set production-meta-injury-ledger-path)
        transaction-before (production-file-set production-meta-injury-transaction-path)
        learning-manifest-before (production-file-set production-learning-manifest-path)
        repair-root (temp-root "wm-repair-suite-")
        run-record-root (temp-root "wm-runner-record-suite-")
        run-stream-before (run-stream-length production-run-record-dir)
        trip-root (temp-root "wm-trip-suite-")
        injury-root (str (io/file repair-root "wm-meta-injuries"))
        ledger-path (str (io/file injury-root "ledger.edn"))
        transaction-path (str (io/file injury-root "transaction.edn"))
        learning-manifest-path (str (io/file repair-root "outer-learning-manifest.edn"))
        phase-log (str (io/file repair-root "wm-full-loop-phases.edn.log"))]
    (with-redefs [repair/default-root repair-root
                  tripwire/default-trip-root trip-root
                  runner/default-run-record-dir run-record-root
                  meta-injury-store/default-root injury-root
                  meta-self-heal/default-ledger-path ledger-path
                  meta-self-heal/default-transaction-path transaction-path
                  outer-learning-manifest/default-path learning-manifest-path
                  runner/default-phase-log phase-log]
      (run-tests))
    (testing "the suite cannot deposit production repair or trip artifacts"
      (is (= repair-before (file-set production-repair-root)))
      (is (= trips-before (file-set production-trip-root)))
      (is (= phase-bytes-before (.length (io/file production-phase-log))))
      (is (= injuries-before (production-file-set production-meta-injury-root))
          "the deposit sink (meta-injury-store/default-root) must stay untouched")
      (is (= ledger-before (production-file-set production-meta-injury-ledger-path))
          "the durable ledger (meta-self-heal/default-ledger-path) must stay
           byte-for-byte untouched: a sealed-ledger append cannot be undone")
      (is (= transaction-before (production-file-set production-meta-injury-transaction-path))
          "the durable terminal transaction file must stay untouched")
      (is (= learning-manifest-before (production-file-set production-learning-manifest-path))
          "the production outer-learning manifest (outer-learning-manifest/default-path)
           must stay byte-for-byte untouched: a test close must publish only into
           the rebound suite manifest or the fixture's explicit temp manifest")
      (is (= run-stream-before (run-stream-length production-run-record-dir))
          "the production run-stream must not grow: run 7712b197 (2026-10-31
           14:49Z, started, never closed) reached it from
           t3-held-resume-stop-line-test because this fixture left the
           run-record root live. A live click running beside the tests also
           appends here, so run these between clicks."))))

(defn runner-repair-options
  "Explicit temporary store callbacks also protect direct REPL/debug callers,
  where clojure.test namespace fixtures are not installed."
  []
  (let [root (temp-root "wm-runner-repair-")]
    {:repair-root root
     :outer-learning-manifest-path (str (io/file root "outer-learning-manifest.edn"))
     :tripwire/report-root (str (io/file root "trips"))
     :tripwire/repair-record-fn (partial repair/record-system-failure! root)
     :repair-system-record-fn (partial repair/record-system-failure! root)
     :repair-record-fn (partial repair/record-review-failure! root)
     :repair-open-fn (partial repair/open-obligations root)
     :repair-implement-fn (partial repair/record-implementation! root)
     :repair-resolve-fn (partial repair/resolve! root)
     :repair-supersede-fn (partial repair/supersede! root)}))
