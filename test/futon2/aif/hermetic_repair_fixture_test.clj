(ns futon2.aif.hermetic-repair-fixture-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.declaration-reads-test :as declaration-reads]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.hermetic-repair-fixture :as fixture]
            [futon2.aif.meta-injury-store :as meta-injury-store]
            [futon2.aif.previous-run :as previous-run]))

(use-fixtures :once fixture/with-hermetic-stores)

(deftest phase-log-default-is-hermetic-under-the-repair-fixture
  (let [before (.length (io/file fixture/production-phase-log))
        opts (runner/config {})]
    (runner/emit-phase! opts {:opportunity-id "test/f28-fixture"} {:phase :fixture :transition :start})
    (is (.exists (io/file (:phase-log opts))))
    (is (= before (.length (io/file fixture/production-phase-log)))
        "the default phase emitter must not append the production phase log")))

(deftest declaration-run-deposits-only-under-the-hermetic-injury-root
  (let [before (fixture/production-file-set fixture/production-meta-injury-root)
        _ (declaration-reads/record-run (fn [] nil))]
    (is (seq (filter #(.isFile %) (file-seq (io/file meta-injury-store/default-root)))))
    (is (= before (fixture/production-file-set fixture/production-meta-injury-root)))))

(deftest run-stream-start-appends-only-under-the-hermetic-run-record-root
  ;; Row TEST-WRITES-CANONICAL-RUN-STREAM (2026-10-31): run 7712b197
  ;; (started 14:49Z, never closed) reached the canonical run-stream from
  ;; t3-held-resume-stop-line-test because with-hermetic-stores left
  ;; runner/default-run-record-dir live. Read from what the receiver reads:
  ;; the stream file in the CURRENT (rebound) run-record root must carry the
  ;; started event, and the production root must be unchanged.
  (let [production-before (fixture/production-file-set fixture/production-run-record-dir)
        stream (io/file runner/default-run-record-dir "run-stream.edn")]
    (previous-run/start! runner/default-run-record-dir
                         "test-fixture-run-record-root" "2026-10-31T00:00:00Z")
    (is (.isFile stream) "the started event landed in the HERMETIC root")
    (is (.contains (slurp stream) "\"test-fixture-run-record-root\""))
    (is (= production-before
           (fixture/production-file-set fixture/production-run-record-dir))
        "the canonical run-stream must not gain the fixture's :started event")))
