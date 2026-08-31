#!/usr/bin/env bb
(ns r2-channel-contract-test
  (:require [babashka.fs :as fs]
            [checks.r2-channel-contract :as lint]
            [clojure.edn :as edn]
            [clojure.test :refer [deftest is run-tests testing]]))

(def snapshot-path "test/fixtures/r2-channel-contract/snapshot.edn")

(defn- snapshot [] (edn/read-string (slurp snapshot-path)))

(defn- observation [extra-or-missing]
  (into {}
        (map (fn [channel] [channel 0.5]))
        extra-or-missing))

(defn- with-corpus [records f]
  (let [dir (fs/create-temp-dir {:prefix "r2-channel-contract-"})]
    (try
      (spit (str (fs/path dir "wm-trace-fixture.edn"))
            (apply str (map #(str (pr-str %) "\n") records)))
      (f (str dir))
      (finally (fs/delete-tree dir)))))

(deftest committed-r2-snapshot-owns-exact-counts-and-pin
  (let [{:keys [records expected recorded-at]} (snapshot)]
    (is (= "2026-08-31" recorded-at))
    (with-corpus
      records
      (fn [trace-dir]
        (let [report (lint/lint-paths {:trace-dir trace-dir})]
          (is (= (:files expected) (get-in report [:summary :files])))
          (is (= (:forms expected) (get-in report [:summary :forms])))
          (is (= (:conformant-records expected)
                 (get-in report [:summary :conformant-records])))
          (is (= (:key-set-mismatches expected)
                 (get-in report [:summary :key-set-mismatches])))
          (is (= (:failure-classes expected)
                 (get-in report [:summary :failure-classes])))
          (is (= (:content-pin expected) (get-in report [:content-pin :sha256]))))))))

(deftest live-r2-gate-emits-moving-counts-and-enforces-channel-invariants
  (let [report (lint/lint-paths {})
        summary (:summary report)]
    ;; Counts are evidence, not fixed expectations. The two known malformed
    ;; observations keep this live gate honestly red until their source is fixed.
    (is (every? #(contains? summary %)
                [:files :forms :conformant-records :conformance-ratio]))
    (is (<= 0.0 (:conformance-ratio summary) 1.0))
    (is (zero? (:undeclared-key-count summary)))
    (is (= [:observation-keys] (:failure-classes summary)))
    (is (false? (:pass? summary)))
    (is (every? #(seq (:missing %)) (:checks report)))
    (is (= [:loop-health :support-coverage :attack-coverage :mission-health
            :stack-pct :consulting-pct :portfolio-pct :mathematics-pct
            :active-repo-ratio :sorry-count-norm :coupling-density
            :ticks-firing-ratio :depositing-signal :annotation-health]
           (get-in report [:channel :values])))
    (is (= 11 (get-in report [:channel :source :line])))
    (is (true? (get-in report [:likelihood :partition-valid?])))))

(deftest corpus-fixtures-discriminate-source-keyed-contract
  (let [declared (:channels (lint/read-declarations {}))
        complete (observation declared)]
    (testing "every record with a fifteenth undeclared key fires"
      (with-corpus
        (repeat 5 {:observation (assoc complete :undeclared-fifteenth 0.5)})
        (fn [trace-dir]
          (let [report (lint/lint-paths {:trace-dir trace-dir})]
            (is (= 5 (get-in report [:summary :forms])))
            (is (= 5 (get-in report [:summary :key-set-mismatches])))
            (is (= 0 (get-in report [:summary :conformant-records])))
            (is (every? #(= [:undeclared-fifteenth] (:undeclared %))
                        (:checks report)))))))
    (testing "a record missing a declared channel fires"
      (with-corpus
        [{:observation (dissoc complete :annotation-health)}]
        (fn [trace-dir]
          (let [report (lint/lint-paths {:trace-dir trace-dir})]
            (is (= 1 (get-in report [:summary :key-set-mismatches])))
            (is (= [:annotation-health]
                   (get-in report [:checks 0 :missing])))))))))

(deftest non-map-observation-is-not-an-empty-observation
  (with-corpus
    [{:observation nil}]
    (fn [trace-dir]
      (let [report (lint/lint-paths {:trace-dir trace-dir})]
        (is (= 1 (get-in report [:summary :malformed-observations])))
        (is (= :not-a-map (get-in report [:checks 0 :reason])))))))

(deftest generated-lean-is-directly-substitutable-for-the-interface-hole
  (let [{:keys [records]} (snapshot)]
    (with-corpus
      records
      (fn [trace-dir]
        (let [{normalized :records} (lint/read-trace-corpus trace-dir)
              report (lint/lint-paths {:trace-dir trace-dir})
              generated (lint/lean-fixture-text report normalized)]
          (is (re-find #"def wmTraceR2 : List R2TickLit" generated))
          (is (re-find #"r2ContractCensus wmTraceR2" generated))
          (is (re-find #"= 1 := by" generated))
          (is (re-find #"(?m)^  native_decide$" generated))
          (is (not (re-find #"wmTraceR2Generated" generated))))))))

(when (= *file* (System/getProperty "babashka.file"))
  (let [{:keys [fail error]} (run-tests)]
    (System/exit (if (zero? (+ fail error)) 0 1))))
