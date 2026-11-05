(ns futon2.aif.serving-envelope-test
  "A2-ENVELOPE-RECEIPT-ABSENT. The fixture is built from the retained
  record of run 0be4fc7d (tick-run-record-2026-10-31-0be4fc7d-…edn):
  participants wm-author / wm-reviewer / wm-repair-reviewer with issuing
  caller claude-1, started 2026-10-31T20:17:26Z. The receiver-side test
  reads the envelope back from the hydrated tick record, the path zai-6's
  reconciliation matrix reads (:serving-envelope of
  data/wm-runs/tick-run-record-<run-id>.edn)."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.run-record-io :as run-record-io]
            [futon2.aif.serving-envelope :as sut])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

;; --- fixture from run 0be4fc7d -------------------------------------------

(def fixture-seats
  "The run-0be4fc7d casting as the Agency roster would report it at start."
  {:wm-author {:status "idle" :invoke-ready? true}
   :wm-reviewer {:status "idle" :invoke-ready? true}
   :wm-repair-reviewer {:status "restored" :invoke-ready? true}
   :claude-1 {:status "invoking" :invoke-ready? true}})

(def fixture-jobs
  [{:job-id "j1" :agent-id "claude-1" :state "running"}
   {:job-id "j2" :agent-id "wm-author" :state "queued"}
   {:job-id "j3" :agent-id "wm-reviewer" :state "delivered"}])

(def fixture-run-id "2026-10-31-0be4fc7d-70db-4e96-8beb-ea24a1867896")
(def fixture-started-at "2026-10-31T20:17:26.481664936Z")

(defn- stub-observe-opts []
  {:gc-fn (fn [] nil)
   :roster-fn (fn [_agency-base] fixture-seats)
   :invoke-jobs-fn (fn [_agency-base] {:ok true :count 3 :jobs fixture-jobs})})

(def fixture-config
  {:agency-base "http://127.0.0.1:7070"
   :substrate-url "http://127.0.0.1:7073"
   :author "wm-author"
   :reviewer "wm-reviewer"
   :repair-reviewer "wm-repair-reviewer"
   :agent-budget-ms 3600000
   :trigger :duree-click-on-demand
   :cohort? true
   :semantic-epoch 7
   :revision-rounds 1
   ;; not an admission opt; must not leak into the receipt
   :run-record-dir "/should-not-appear"})

;; --- capture ---------------------------------------------------------------

(deftest capture-retains-heap-queue-seats-and-admission-opts
  (let [envelope (sut/capture (stub-observe-opts) fixture-config)]
    (is (= :wm/serving-envelope-v1 (:schema envelope)))
    (is (= :run-start-before-author-dispatch (:capture-point envelope)))
    (is (string? (:captured-at envelope)))
    (testing "heap after GC, as wm_click.sh preflight reports it"
      (is (= :measured (get-in envelope [:heap :status])))
      (is (integer? (get-in envelope [:heap :heap-used-mib])))
      (is (pos? (get-in envelope [:heap :heap-max-mib]))))
    (testing "agency queue snapshot at start"
      (is (= {:status :measured
              :source :agency-invoke-jobs-endpoint
              :total 3
              :by-state {"delivered" 1 "queued" 1 "running" 1}}
             (get-in envelope [:agency :queue]))))
    (testing "agency seat state at start"
      (is (= :measured (get-in envelope [:agency :seats :status])))
      (is (= 4 (get-in envelope [:agency :seats :seat-count])))
      (is (= {:status "restored" :invoke-ready? true}
             (get-in envelope [:agency :seats :seats "wm-repair-reviewer"]))))
    (testing "the actual admission opts, and only those"
      (is (= :measured (get-in envelope [:admission-opts :status])))
      (is (= "wm-author" (get-in envelope [:admission-opts :opts :author])))
      (is (= :duree-click-on-demand
             (get-in envelope [:admission-opts :opts :trigger])))
      (is (not (contains? (get-in envelope [:admission-opts :opts])
                          :run-record-dir))))
    (testing "unobservable components declared, not asserted"
      (is (= sut/baseline-unknown-components
             (:unknown-components envelope))))))

(deftest capture-lists-failed-observations-as-unknown
  (let [envelope (sut/capture
                  (assoc (stub-observe-opts)
                         :invoke-jobs-fn (fn [_] (throw (ex-info "agency down" {})))
                         :roster-fn (fn [_] (throw (ex-info "agency down" {}))))
                  fixture-config)]
    (is (= :unknown (get-in envelope [:agency :queue :status])))
    (is (= :unknown (get-in envelope [:agency :seats :status])))
    (is (some #{:agency-queue} (:unknown-components envelope)))
    (is (some #{:agency-seats} (:unknown-components envelope)))
    ;; a failed observation must not refuse the run's own instrumentation
    (is (= :measured (get-in envelope [:heap :status])))
    (is (= :measured (get-in envelope [:admission-opts :status])))))

;; RED-SUITES-3-K (claude-6, 2026-11-05): capture receives the runner's whole
;; option map, whose :queue-fn is the Morning Brief queuer. The envelope must
;; never call it, and must not report its return value as a measured queue.
(deftest capture-does-not-call-the-morning-brief-queuer
  (let [brief-calls (atom [])
        envelope (sut/capture
                  (-> (stub-observe-opts)
                      (dissoc :invoke-jobs-fn)
                      (assoc :queue-fn (fn [item] (swap! brief-calls conj item) [])
                             :invoke-jobs-fn (fn [_] (throw (ex-info "agency down" {})))))
                  fixture-config)]
    (is (empty? @brief-calls))
    (is (= :unknown (get-in envelope [:agency :queue :status])))))

;; --- receiver-side: read the envelope back from the tick record -------------

(defn- temp-root []
  (.getCanonicalPath
   (.toFile (Files/createTempDirectory "serving-envelope-"
                                       (make-array FileAttribute 0)))))

(def ^:private minimal-result
  {:outcome :offline-no-selection
   :trace-path "fixture-trace"
   :backtrace {:decision {:selection-law {:posterior {:a 1}}
                          :c {:status :derived}}}})

(defn- persist-opts [root extra]
  (merge {:scan-render-fn (fn [& _] nil)
          :nano-time-fn (constantly 10)
          :run-timing/start-nanos 10
          :run-record-dir (str (io/file root "runs"))
          :observation-labels-path (str (io/file root "labels.edn"))}
         extra))

(deftest persisted-record-carries-envelope-at-receiver
  (let [root (temp-root)
        envelope (sut/capture (stub-observe-opts) fixture-config)
        persisted (runner/persist-run-record!
                   (persist-opts root {:serving-envelope envelope})
                   fixture-run-id fixture-started-at minimal-result)
        hydrated (run-record-io/hydrate-run-record (:run-record persisted))]
    (is (= :present (:run-record-status persisted)))
    (testing "the receiver reads :serving-envelope from the tick record"
      (is (= :wm/serving-envelope-v1
             (get-in hydrated [:serving-envelope :schema])))
      (is (= :run-start-before-author-dispatch
             (get-in hydrated [:serving-envelope :capture-point])))
      (is (= {"delivered" 1 "queued" 1 "running" 1}
             (get-in hydrated [:serving-envelope :agency :queue :by-state])))
      (is (= 4 (get-in hydrated [:serving-envelope :agency :seats :seat-count])))
      (is (integer? (get-in hydrated [:serving-envelope :heap :heap-used-mib])))
      (is (= "wm-author"
             (get-in hydrated [:serving-envelope :admission-opts :opts :author])))
      (is (= sut/baseline-unknown-components
             (get-in hydrated [:serving-envelope :unknown-components]))))))

(deftest record-without-capture-retains-an-explicit-absence
  (let [root (temp-root)
        persisted (runner/persist-run-record!
                   (persist-opts root {})
                   fixture-run-id fixture-started-at minimal-result)
        hydrated (run-record-io/hydrate-run-record (:run-record persisted))]
    (is (= sut/absent-receipt (:serving-envelope hydrated)))))
