#!/usr/bin/env bb
(ns r2-channel-contract-test
  (:require [babashka.fs :as fs]
            [checks.r2-channel-contract :as lint]
            [clojure.test :refer [deftest is run-tests testing]]))

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

(deftest current-r2-channel-contract-baseline
  (let [report (lint/lint-paths {})]
    (is (= 53 (get-in report [:summary :files])))
    (is (= 792 (get-in report [:summary :forms])))
    (is (= 790 (get-in report [:summary :conformant-records])))
    (is (= 2 (get-in report [:summary :key-set-mismatches])))
    (is (= 0 (get-in report [:summary :malformed-observations])))
    (is (= 0 (get-in report [:summary :undeclared-key-count])))
    (is (= :sha256-over-newline-joined-sorted-form-sha256
           (get-in report [:content-pin :algorithm])))
    (is (= "c9add16ac96c973b" (get-in report [:content-pin :prefix])))
    (is (= 2 (get-in report [:r2ContractCensusWmTrace :ill-formed])))
    (is (= [:loop-health :support-coverage :attack-coverage :mission-health
            :stack-pct :consulting-pct :portfolio-pct :mathematics-pct
            :active-repo-ratio :sorry-count-norm :coupling-density
            :ticks-firing-ratio :depositing-signal :annotation-health]
           (get-in report [:channel :values])))
    (is (= 11 (get-in report [:channel :source :line])))
    (is (true? (get-in report [:likelihood :partition-valid?])))
    (is (= #{"2026-05-18T19:42:49.284838608Z"
             "2026-05-18T20:54:12.717822372Z"}
           (set (map (comp str :timestamp) (:checks report)))))))

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
  (let [{:keys [records]} (lint/read-trace-corpus lint/default-trace-dir)
        report (lint/lint-paths {})
        generated (lint/lean-fixture-text report records)]
    (is (re-find #"def wmTraceR2 : List R2TickLit" generated))
    (is (re-find #"r2ContractCensus wmTraceR2" generated))
    (is (re-find #"(?m)^  native_decide$" generated))
    (is (not (re-find #"wmTraceR2Generated" generated)))))

(let [{:keys [fail error]} (run-tests)]
  (System/exit (if (zero? (+ fail error)) 0 1)))
