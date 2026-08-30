#!/usr/bin/env bb
(ns control-map-lint-test
  (:require [babashka.fs :as fs]
            [checks.control-map-lint :as lint]
            [clojure.test :refer [deftest is run-tests testing]]))

(def edge {:from :R1 :to :R2 :kind :control :status :drawn})

(defn- with-records [texts f]
  (let [dir (fs/create-temp-dir {:prefix "control-map-lint-"})]
    (try
      (doseq [[node text] texts]
        (spit (str (fs/path dir (str "P-" (name node) ".md"))) text))
      (f (str dir))
      (finally (fs/delete-tree dir)))))

(deftest current-control-map-baseline
  (let [report (lint/lint-file {})]
    (is (true? (get-in report [:summary :pass?])))
    (is (= 21 (get-in report [:summary :drawn])))
    (is (= 1 (get-in report [:summary :unresolved])))
    (is (= 26 (get-in report [:summary :derived-undrawn])))
    (is (= 1 (get-in report [:summary :derived-chartered])))
    (is (= 0 (get-in report [:summary :specified])))
    (is (= 21 (get-in report [:summary :unspecified])))
    (is (= {:no-endpoint-record 15 :one-endpoint-record 6}
           (frequencies (map :endpoints-agree? (:edges report)))))))

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

(let [{:keys [fail error]} (run-tests)]
  (System/exit (if (zero? (+ fail error)) 0 1)))
