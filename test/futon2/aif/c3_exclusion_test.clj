(ns futon2.aif.c3-exclusion-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.c3-exclusion :as sut]
            [futon2.aif.outer-refusal-history :as history]
            [futon2.aif.outer-refusal-index :as index]))

(def action {:operation :git-push :repo "/repo" :remote "origin"
             :ref "refs/heads/main" :force? true})

(defn temp-dir []
  (.getPath (.toFile (java.nio.file.Files/createTempDirectory
                      "c3-exclusion-test"
                      (make-array java.nio.file.attribute.FileAttribute 0)))))

(defn record [target]
  {:run/id "run-c3"
   :outcome :harm-declined
   :target target
   :failure {:kind :harm-declined :detail {:target target}}
   :effective-run-identity {:target target :mission-source-sha256 "mission-sha"}
   :harm-match {:rule :H2 :declined-action action
                :declined-action-digest (identity/digest action)}})

(deftest harm-declined-row-is-published-on-the-single-index-line
  (let [dir (temp-dir)
        source (io/file dir "tick-run-record-run.edn")
        pending (assoc (record "M-target") :refusal/disposition
                       {:disposition :pending-exclusion :case :C3})]
    (spit source (pr-str pending))
    (index/append-run-record! dir source pending)
    (let [line (first (history/read-index dir))]
      (is (= :click-terminal (get-in line [:refusal/disposition :disposition])))
      (is (= :C3 (get-in line [:refusal/disposition :case])))
      (is (= :row (get-in line [:c3-exclusion :status])))
      (is (= :H2 (get-in line [:c3-exclusion :row :rule])))
      (is (= (identity/digest action)
             (get-in line [:c3-exclusion :row :declined-action-digest]))))))

(deftest non-mission-c3-cannot-record-an-exclusion
  (let [dir (temp-dir)
        source (io/file dir "tick-run-record-run.edn")
        pending (assoc (record "T-target") :refusal/disposition
                       {:disposition :pending-exclusion :case :C3})]
    (spit source (pr-str pending))
    (index/append-run-record! dir source pending)
    (let [line (first (history/read-index dir))]
      (is (= :debugger-error (get-in line [:refusal/disposition :disposition])))
      (is (= :exclusion-not-recorded (get-in line [:refusal/disposition :reason])))
      (is (nil? (:c3-exclusion line))))))

(deftest c3-row-refuses-missing-carriers
  (is (= :not-c3 (:reason (sut/exclusion-row (assoc (record "M-x") :outcome :other)))))
  (is (= :harm-match-absent (:reason (sut/exclusion-row (dissoc (record "M-x") :harm-match)))))
  (is (= :digest-carrier-absent (:reason (sut/exclusion-row (record "T-x"))))))

(deftest row-reads-the-match-where-a-real-record-retains-it
  ;; A run record keeps thrown ex-data under [:failure :detail] (via
  ;; failure-detail); the fixture above puts :harm-match at top level.
  (let [{:keys [harm-match] :as top} (record "M-x")
        real (-> top
                 (dissoc :harm-match :outcome)
                 (assoc-in [:failure :detail :harm-match] harm-match)
                 (assoc-in [:failure :detail :outcome] :harm-declined))
        result (sut/exclusion-row real)]
    (is (= :row (:status result)))
    (is (= (identity/digest action)
           (get-in result [:row :declined-action-digest])))))
