(ns futon2.aif.machine-slow-feedback-retrospective-projection
  "Pure extraction of a retrospective evidence draft from a pinned complete
   capture. Completeness remains a typed unavailable authority."
  (:require [clojure.edn :as edn]
            [futon2.aif.machine-slow-feedback-capture :as capture]
            [futon2.aif.machine-slow-feedback-provenance :as provenance])
  (:import (java.nio.charset StandardCharsets)
           (java.security MessageDigest)
           (java.util Base64)))

(def ^:private hex64 #"[0-9a-f]{64}")
(defn- refuse! [kind data]
  (throw (ex-info (name kind) (assoc data :refusal kind))))
(defn- sha256 [^bytes bs]
  (apply str (map #(format "%02x" (bit-and 255 %))
                  (.digest (doto (MessageDigest/getInstance "SHA-256") (.update bs))))))
(defn- decode [descriptor]
  (let [bs (.decode (Base64/getDecoder) ^String (:bytes/base64 descriptor))]
    (edn/read-string (String. bs StandardCharsets/UTF_8))))
(defn- encoded [kind value]
  (let [bs (.getBytes (pr-str value) StandardCharsets/UTF_8)]
    (array-map :kind kind :encoding :utf8-pr-str
               :bytes/base64 (.encodeToString (Base64/getEncoder) bs)
               :sha256 (sha256 bs))))
(defn- ledger-row [application]
  (let [keys* #{:application/id :feedback/event-id :prior-state/revision
                :status :input/digests :output/digest}
        row (select-keys application keys*)]
    (when-not (and (= keys* (set (keys row))) (= :committed (:status row))
                   (= #{:context :prior :e2b :outcome} (set (keys (:input/digests row))))
                   (every? #(and (string? %) (re-matches hex64 %))
                           (conj (vec (vals (:input/digests row))) (:output/digest row))))
      (refuse! :e6b-retrospective/ledger-row-invalid {}))
    (array-map :application/id (:application/id row)
               :feedback/event-id (:feedback/event-id row)
               :prior-state/revision (:prior-state/revision row)
               :status :committed
               :input/digests (array-map :context (get-in row [:input/digests :context])
                                         :prior (get-in row [:input/digests :prior])
                                         :e2b (get-in row [:input/digests :e2b])
                                         :outcome (get-in row [:input/digests :outcome]))
               :output/digest (:output/digest row))))

(defn project
  "Revalidate CAPTURE-PIN and extract TARGET-APPLICATION-ID. The result is an
   immutable structural draft and explicitly refuses completeness authority."
  [{:keys [capture-pin target-application-id] :as input}]
  (when-not (= #{:capture-pin :target-application-id} (set (keys input)))
    (refuse! :e6b-retrospective/input-schema-invalid {}))
  (when-not (and (string? target-application-id) (seq target-application-id))
    (refuse! :e6b-retrospective/target-invalid {}))
  (let [validated (capture/readback capture-pin)
        c (:record validated)
        tx-by-digest (into {} (map (juxt :digest decode) (:transaction-objects c)))
        provenance-by-digest (into {} (map (juxt :digest identity) (:provenance-objects c)))
        index (:application-universe c)
        matches (filterv #(= target-application-id (:application/id %)) index)]
    (when (empty? index) (refuse! :e6b-retrospective/no-applications-at-genesis {}))
    (when-not (= 1 (count matches))
      (refuse! (if (empty? matches)
                 :e6b-retrospective/target-not-found
                 :e6b-retrospective/target-duplicate)
               {:target-application-id target-application-id}))
    (let [resolved
          (mapv
           (fn [entry]
             (let [tx (tx-by-digest (:transaction-sha256 entry))
                   pd (:provenance-sha256 entry) p-desc (provenance-by-digest pd)]
               (when-not (and tx p-desc (= pd (:provenance-sha256 tx)))
                 (refuse! :e6b-retrospective/index-resolution-incomplete {:entry entry}))
               (let [p (provenance/readback {:bytes/base64 (:bytes/base64 p-desc)
                                             :expected-sha256 pd})
                     row (ledger-row (:application tx))]
                 (when-not (= (select-keys entry
                                           [:application/id :feedback/event-id
                                            :prior-state/revision])
                              (select-keys row [:application/id :feedback/event-id
                                                :prior-state/revision]))
                   (refuse! :e6b-retrospective/index-transaction-disagreement {:entry entry}))
                 {:entry entry :transaction tx :transaction/descriptor
                  (first (filter #(= (:transaction-sha256 entry) (:digest %))
                                 (:transaction-objects c)))
                  :provenance p :provenance/descriptor p-desc :ledger-row row})))
           index)
          target (first (filter #(= target-application-id
                                    (get-in % [:entry :application/id])) resolved))
          ledger (mapv :ledger-row resolved)
          p-record (get-in target [:provenance :record])
          proposal (:proposal-evidence p-record)
          next-record (:next proposal)
          next-encoded (encoded :derived-complete-next-record next-record)
          ledger-encoded (encoded :derived-six-field-application-ledger ledger)]
      (when-not (= (:sha256 next-encoded) (get-in target [:ledger-row :output/digest]))
        (refuse! :e6b-retrospective/next-output-digest-disagreement {}))
      (array-map
       :schema :wm/e6b-retrospective-projection-v1
       :scope :isolated-test
       :status :structural-projection-only
       :authority/status :none
       :capture (array-map :raw-sha256 (:sha256 validated)
                           :store/id (:store/id c) :generation (:generation c)
                           :head-digest (:head-digest c)
                           :chain-digests (:chain-digests c))
       :target (array-map :application/id target-application-id
                          :index-entry (:entry target)
                          :transition/subject (get-in target [:transaction :application
                                                             :transition/subject])
                          :transaction/descriptor (:transaction/descriptor target)
                          :provenance/descriptor (:provenance/descriptor target))
       :application-ledger ledger-encoded
       :application-ledger/records ledger
       :next-state next-encoded
       :next-state/record next-record
       :original-sources (:original-sources p-record)
       :canonical-closure (:canonical-closure p-record)
       :carrier-projection (:carrier-projection p-record)
       :digest-roles (array-map
                      :capture/raw (:sha256 validated)
                      :ledger/derived (:sha256 ledger-encoded)
                      :next-record/derived (:sha256 next-encoded)
                      :prior-carrier (get-in p-record [:carrier-projection :prior :sha256])
                      :next-carrier (get-in p-record [:carrier-projection :next :sha256]))
       :completeness (array-map :status :refused
                                :refusal :e6b-retrospective/completeness-authority-unavailable)
       :restart-authorized? false))))
