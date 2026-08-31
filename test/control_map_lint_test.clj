#!/usr/bin/env bb
(ns control-map-lint-test
  (:require [babashka.fs :as fs]
            [checks.control-map-lint :as lint]
            [clojure.edn :as edn]
            [clojure.test :refer [deftest is run-tests testing]]))

(def edge {:from :R1 :to :R2 :kind :control :status :drawn})
(def snapshot-path "test/fixtures/control-map/snapshot.edn")

(defn- content-pin [value]
  (let [digest (java.security.MessageDigest/getInstance "SHA-256")]
    (.update digest (.getBytes (pr-str value) "UTF-8"))
    (format "%064x" (java.math.BigInteger. 1 (.digest digest)))))

(defn- with-records [texts f]
  (let [dir (fs/create-temp-dir {:prefix "control-map-lint-"})]
    (try
      (doseq [[node text] texts]
        (spit (str (fs/path dir (str "P-" (name node) ".md"))) text))
      (f (str dir))
      (finally (fs/delete-tree dir)))))

(deftest committed-control-map-snapshot-owns-exact-counts-and-pin
  (let [{:keys [input expected recorded-at]} (edn/read-string (slurp snapshot-path))
        report (lint/lint-data input)]
    (is (= "2026-08-31" recorded-at))
    (is (= (:content-pin expected) (content-pin input)))
    (is (= (:summary expected) (:summary report)))
    (is (true? (get-in report [:summary :pass?])))))

(deftest live-control-map-enforces-independent-baseline-and-emits-moving-counts
  (let [report (lint/lint-file {})]
    (is (true? (get-in report [:summary :pass?])))
    (is (every? #(contains? (:summary report) %)
                [:drawn :unresolved :derived-undrawn :specified :unspecified]))
    (is (= lint/expected-drawn-edges
           (set (map lint/edge-key (:edges report)))))
    (is (apply distinct? (map lint/edge-key (:edges report))))
    (is (every? #(contains? #{:no-endpoint-record :one-endpoint-record
                              :schema-unspecified true false} %)
                (map :endpoints-agree? (:edges report))))
    (is (empty? (:checks report)))))

(deftest drawn-edge-baseline-and-endpoint-agreement-falsifiers
  (testing "a baseline drawn edge missing from the EDN fails closed"
    (let [report (lint/lint-data {:data {:edges [] :derived-undrawn []}
                                  :expected-drawn #{(lint/edge-key edge)}})]
      (is (false? (get-in report [:summary :pass?])))
      (is (= :drawn-edge-missing (get-in report [:checks 0 :reason])))))
  (testing "a derived-undrawn edge without its derivation fails closed"
    (let [report (lint/lint-data
                  {:data {:edges [edge]
                          :derived-undrawn [{:from :R2 :to :R3}]}
                   :expected-drawn #{(lint/edge-key edge)}})]
      (is (false? (get-in report [:summary :pass?])))
      (is (= :missing-by (get-in report [:checks 0 :reason])))))
  (testing "two endpoint records disagree when one omits the declared schema"
    (with-records
      {:R1 "R1→R2 payload carries tick and value."
       :R2 "R1→R2 payload carries tick only."}
      (fn [records-dir]
        (let [schema {:tick :nat :value :number}
              report (lint/lint-data
                      {:data {:edges [(assoc edge :schema schema
                                            :fixture {:tick 1 :value 2})]
                              :derived-undrawn []}
                       :records-dir records-dir
                       :expected-drawn #{(lint/edge-key edge)}})]
          (is (false? (get-in report [:edges 0 :endpoints-agree?])))
          (is (false? (get-in report [:edges 0 :fully-specified?]))))))))

(deftest schema-terms-match-whole-tokens-not-substrings
  ;; Regression (claude-20 review of CML-D1): with substring matching these two
  ;; records reported :endpoints-agree? true and :fully-specified? true, though
  ;; neither mentions the `id` or `val` fields -- "identity" and "evaluation"
  ;; merely contain the letters.
  (with-records
    {:R1 "R1→R2 concerns the identity of the sender and its value proposition."
     :R2 "R1→R2 discusses identity and evaluation, nothing else."}
    (fn [records-dir]
      (let [report (lint/lint-data
                    {:data {:edges [(assoc edge
                                          :schema {:id :string :val :number}
                                          :fixture {:id "a" :val 1})]
                            :derived-undrawn []}
                     :records-dir records-dir
                     :expected-drawn #{(lint/edge-key edge)}})]
        (is (false? (get-in report [:edges 0 :endpoints-agree?])))
        (is (false? (get-in report [:edges 0 :fully-specified?])))))))

(deftest specified-means-values-and-two-record-agreement
  (with-records
    {:R1 "R1->R2 payload carries tick and value."
     :R2 "R1->R2 receipt consumes tick and value."}
    (fn [records-dir]
      (let [report (lint/lint-data
                    {:data {:edges [(assoc edge
                                          :schema {:tick :nat :value :number}
                                          :fixture {:tick 1 :value 2})]
                            :derived-undrawn [{:from :R2 :to :R3 :by [:fixture]}]}
                     :records-dir records-dir
                     :expected-drawn #{(lint/edge-key edge)}})]
        (is (true? (get-in report [:summary :pass?])))
        (is (true? (get-in report [:edges 0 :endpoints-agree?])))
        (is (= 1 (get-in report [:summary :specified])))
        (is (= 0 (get-in report [:summary :unspecified])))))))

(when (= *file* (System/getProperty "babashka.file"))
  (let [{:keys [fail error]} (run-tests)]
    (System/exit (if (zero? (+ fail error)) 0 1))))
