(ns futon2.aif.machine-slow-feedback-retrospective-projection-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [futon2.aif.machine-slow-feedback-capture :as capture]
            [futon2.aif.machine-slow-feedback-capture-test :as capture-test]
            [futon2.aif.machine-slow-feedback-retrospective-projection :as projection]
            [futon2.aif.machine-slow-feedback-store-v2 :as store]
            [futon2.aif.machine-slow-feedback-store-v2-test :as store-test])
  (:import (java.nio.charset StandardCharsets)
           (java.util Base64)))

(defn- refusal [f]
  (try (f) nil (catch clojure.lang.ExceptionInfo e (:refusal (ex-data e)))))
(defn- fixture [committed?]
  (let [[_ s provenance] (#'store-test/setup)]
    (when committed? (store/commit! s (#'store-test/pin provenance)))
    (let [artifact (capture/construct (store/capture s))
          target (get-in (capture/readback {:bytes/base64 (:bytes/base64 artifact)
                                            :expected-sha256 (:sha256 artifact)})
                         [:record :application-universe 0 :application/id])]
      [s artifact target])))
(defn- pin [artifact]
  {:bytes/base64 (:bytes/base64 artifact) :expected-sha256 (:sha256 artifact)})
(defn- decode-descriptor [descriptor]
  (let [bs (.decode (Base64/getDecoder) ^String (:bytes/base64 descriptor))]
    {:bytes bs :record (edn/read-string (String. bs StandardCharsets/UTF_8))
     :sha256 (#'projection/sha256 bs)}))

(deftest deterministic-complete-projection-retains-distinct-evidence
  (let [[s artifact target] (fixture true)
        input {:capture-pin (pin artifact) :target-application-id target}
        a (projection/project input) b (projection/project input)]
    (is (= a b))
    (is (= :structural-projection-only (:status a)))
    (is (= :none (:authority/status a)))
    (is (= {:status :refused
            :refusal :e6b-retrospective/completeness-authority-unavailable}
           (:completeness a)))
    (is (= 1 (count (:application-ledger/records a))))
    (is (= #{:application/id :feedback/event-id :prior-state/revision
             :status :input/digests :output/digest}
           (set (keys (first (:application-ledger/records a))))))
    (is (= #{:context :prior :e2b :outcome}
           (set (keys (get-in a [:application-ledger/records 0 :input/digests])))))
    (let [{:keys [record sha256]} (decode-descriptor (:application-ledger/source a))]
      (is (= {:schema/version :wm/e6b-application-ledger-v1
              :scope :isolated-test
              :entries (:application-ledger/records a)} record))
      (is (= sha256 (get-in a [:digest-roles :ledger-source/raw])))
      (is (not= sha256 (get-in a [:digest-roles :ledger/derived]))))
    (is (= {:schema :wm/e6b-completeness-subject-draft-v1
            :authority/status :none
            :capture/raw-sha256 (get-in a [:digest-roles :capture/raw])
            :ledger-source/raw-sha256 (get-in a [:digest-roles :ledger-source/raw])
            :target/transition-subject (get-in a [:target :transition/subject])}
           (:completeness-subject/draft a)))
    (is (= 7 (count (:original-sources a))))
    (is (= #{:inputs :outputs} (set (keys (:canonical-closure a)))))
    (is (not= (get-in a [:digest-roles :next-record/derived])
              (get-in a [:digest-roles :next-carrier])))
    (is (false? (:restart-authorized? a)))
    (store/release! s)))

(deftest pure-ledger-source-encoding-preserves-two-row-order
  (let [[s artifact target] (fixture true)
        projected (projection/project {:capture-pin (pin artifact)
                                       :target-application-id target})
        first-row (first (:application-ledger/records projected))
        second-row (assoc first-row :application/id "synthetic-second"
                          :feedback/event-id "synthetic-event"
                          :prior-state/revision "synthetic-prior")
        input [first-row second-row]
        a (projection/encode-ledger-source input)
        b (projection/encode-ledger-source (vec input))
        decoded (decode-descriptor (:descriptor a))]
    (is (= a b))
    (is (= input (get-in decoded [:record :entries])))
    (is (= (:sha256 (:descriptor a)) (:sha256 decoded)))
    (let [changed (assoc-in a [:record :entries 0 :application/id] "caller-change")]
      (is (not= changed a))
      (is (= input (get-in (decode-descriptor (:descriptor a)) [:record :entries]))))
    (store/release! s)))

(deftest target-and-genesis-refusals
  (let [[s artifact _] (fixture true)]
    (is (= :e6b-retrospective/target-not-found
           (refusal #(projection/project {:capture-pin (pin artifact)
                                          :target-application-id "missing"}))))
    (is (= :e6b-retrospective/input-schema-invalid
           (refusal #(projection/project {:capture-pin (pin artifact)
                                          :target-application-id "missing"
                                          :authority :candidate}))))
    (store/release! s))
  (let [[s artifact _] (fixture false)]
    (is (= :e6b-retrospective/no-applications-at-genesis
           (refusal #(projection/project {:capture-pin (pin artifact)
                                          :target-application-id "anything"}))))
    (store/release! s)))

(deftest capture-pin-and-structural-mutations-refuse-before-projection
  (let [[s artifact target] (fixture true)
        input {:capture-pin (pin artifact) :target-application-id target}
        capture-record (#'capture-test/record artifact)
        malformed (assoc-in capture-record
                            [:application-universe 0 :prior-state/revision] "borrowed")
        malformed-pin (#'capture-test/artifact-from-record malformed)]
    (is (= :e6b-capture/readback-pin-mismatch
           (refusal #(projection/project
                      (assoc-in input [:capture-pin :expected-sha256]
                                (apply str (repeat 64 "0")))))))
    (is (= :e6b-capture/head-join-invalid
           (refusal #(projection/project {:capture-pin malformed-pin
                                          :target-application-id target}))))
    ;; Projection consumes immutable capture strings; repeated extraction does
    ;; not modify the caller's exact input artifact.
    (let [before artifact _ (projection/project input)]
      (is (= before artifact)))
    (store/release! s)))
