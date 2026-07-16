(ns futon2.aif.tripwire-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.tripwire :as tripwire])
  (:import [java.nio.file Files]
           [java.time Instant]))

(defn- temp-dir []
  (.toFile (Files/createTempDirectory "wm-tripwire-test" (make-array java.nio.file.attribute.FileAttribute 0))))

(defn- write-edn! [root child filename value]
  (let [file (io/file root child filename)]
    (io/make-parents file)
    (spit file (pr-str value))
    file))

(deftest t1-turn-conservation-trips-on-ledger-drift
  (is (= [{:kind :turn-conservation
           :runner/dispatched-turns 2
           :agency/dispatch-count 1}]
         (tripwire/evaluate-wire :T1
                                 {:runner/dispatched-turns 2
                                  :agency/dispatch-count 1}))))

(deftest t2-ledger-closure-trips-on-missing-job-and-commit
  (let [violations
        (tripwire/evaluate-wire
         :T2 {:referenced-job-ids ["author" "reviewer"]
              :agency/job-ids #{"author"}
              :referenced-commits [{:repo "/missing" :sha "bad"}]})]
    (is (= [:missing-agency-jobs :missing-commits]
           (mapv :kind violations)))
    (is (= ["reviewer"] (:job-ids (first violations))))))

(deftest t3-exit-completeness-rereads-durable-findings
  (let [root (temp-dir)
        observation {:phase :opportunity :transition :end
                     :outcome :incomplete :attempt-id "attempt-T3"
                     :repair-root (.getPath root) :tripwire/force? true}]
    (testing "a zero-achievement exit without an on-disk finding trips"
      (is (= [:missing-durable-stop-line]
             (mapv :kind (tripwire/evaluate-wire :T3 observation)))))
    (write-edn! root "findings" "repair-T3.edn"
                {:repair/id "repair-T3" :repair/status :open
                 :attempt-id "attempt-T3"})
    (testing "the same exit clears only after the finding is re-read"
      (is (empty? (tripwire/evaluate-wire :T3 observation))))
    (testing "an outcome outside the cohort alphabet independently trips"
      (is (= :unknown-outcome
             (:kind (first (tripwire/evaluate-wire
                            :T3 (assoc observation :outcome :surprise)))))))))

(deftest t6-append-only-repair-store-trips-on-rewrite
  (let [before {"findings/r.edn"
                {:sha256 "old" :record {:repair/id "r" :repair/status :open}}}
        after {"findings/r.edn"
               {:sha256 "new" :record {:repair/id "r" :repair/status :open}}}]
    (is (= [:repair-record-mutated]
           (mapv :kind (tripwire/evaluate-wire
                        :T6 {:repair-before before :repair-after after}))))))

(deftest t6-status-lattice-trips-on-invalid-edge
  (let [before {"findings/r.edn"
                {:sha256 "same" :record {:repair/id "r" :repair/status :open}}}
        after (assoc before "resolutions/r.edn"
                     {:sha256 "resolution"
                      :record {:repair/id "r" :repair/status :resolved}})]
    (is (= [{:repair/id "r" :from :open :to :resolved}]
           (:edges (first (tripwire/evaluate-wire
                           :T6 {:repair-before before :repair-after after})))))))

(deftest t9-wall-clock-trips-beyond-double-budget
  (is (= [{:kind :phase-budget-exceeded :phase :author-wait
           :duration-ms 2001 :budget-ms 1000 :multiple 2.001}]
         (tripwire/evaluate-wire :T9 {:phase :author-wait :transition :end
                                     :duration-ms 2001 :phase-budget-ms 1000}))))

(deftest t11-job-alphabet-trips-on-unknown-state-and-bad-time
  (let [violations
        (tripwire/evaluate-wire :T11
                                {:job-snapshot {:job-id "j1" :state "mystery"
                                                :created-at "yesterday"}})]
    (is (= [:unknown-job-state :unparseable-job-time]
           (mapv :kind violations)))))

(deftest record-action-is-create-new-and-parseable
  (let [root (temp-dir)
        path (tripwire/write-trip-report!
              (.getPath root)
              {:trip/id "trip-fixed" :trip/wire-id :T1
               :trip/action :record :trip/witness {:drift 1}})]
    (is (= :T1 (:trip/wire-id (edn/read-string (slurp path)))))
    (is (thrown? java.nio.file.FileAlreadyExistsException
                 (tripwire/write-trip-report!
                  (.getPath root) {:trip/id "trip-fixed" :trip/wire-id :T1})))))

(deftest every-wire-is-individually-disableable
  (let [reports (atom [])
        record {:phase :synthetic
                :tripwire/snapshot {:runner/dispatched-turns 2
                                    :agency/dispatch-count 1}}]
    (try
      (tripwire/set-wire-enabled! :T1 false)
      (is (identical? record
                      (tripwire/observe! {:tripwire/report-writer
                                          #(swap! reports conj %)}
                                         record)))
      (is (empty? @reports))
      (finally
        (tripwire/set-wire-enabled! :T1 true)))))

(deftest trip-during-trip-degrades-without-recursion-or-escape
  (let [root (temp-dir)
        writes (atom 0)
        record {:phase :synthetic
                :tripwire/snapshot {:runner/dispatched-turns 2
                                    :agency/dispatch-count 1}}
        opts (atom nil)
        writer (fn [_]
                 (swap! writes inc)
                 ;; This nested violation is evaluated while the outer trip is
                 ;; being handled. It must print a diagnostic, not recurse.
                 (tripwire/observe! @opts record))]
    (reset! opts {:tripwire/report-writer writer
                  :tripwire/report-root (.getPath root)})
    (is (identical? record (tripwire/observe! @opts record)))
    (is (= 1 @writes))
    (is (= 1 (count (filter #(.isFile %)
                            (or (.listFiles root) [])))))))

(deftest valid-job-snapshot-and-boundary-values-do-not-trip
  (let [now (str (Instant/now))]
    (is (empty? (tripwire/evaluate-wire
                 :T11 {:job-snapshot {:job-id "j" :state "done"
                                      :created-at now :events [{:at now}]}})))
    (is (empty? (tripwire/evaluate-wire
                 :T9 {:phase :p :transition :end :duration-ms 2000
                      :phase-budget-ms 1000})))
    (is (empty? (tripwire/evaluate-wire
                 :T1 {:runner/dispatched-turns 2
                      :agency/dispatch-count 2})))))
