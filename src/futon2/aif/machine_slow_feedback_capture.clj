(ns futon2.aif.machine-slow-feedback-capture
  "Pure, isolated serialization for a complete store-v2 capture.

   This codec establishes byte/digest and structural reachability only.  Its
   output has no authority, completeness, freshness, or restart semantics."
  (:require [clojure.edn :as edn]
            [futon2.aif.machine-slow-feedback-provenance :as provenance])
  (:import (java.io PushbackReader StringReader)
           (java.nio ByteBuffer)
           (java.nio.charset CodingErrorAction StandardCharsets)
           (java.security MessageDigest)
           (java.util Base64)))

(def ^:private hex64 #"[0-9a-f]{64}")
(defn- refuse! [kind data]
  (throw (ex-info (name kind) (assoc data :refusal kind))))
(defn- sha256 [^bytes bs]
  (apply str (map #(format "%02x" (bit-and 255 %))
                  (.digest (doto (MessageDigest/getInstance "SHA-256")
                             (.update bs))))))
(defn- strict-edn [^bytes bs]
  (try
    (let [decoder (doto (.newDecoder StandardCharsets/UTF_8)
                    (.onMalformedInput CodingErrorAction/REPORT)
                    (.onUnmappableCharacter CodingErrorAction/REPORT))
          reader (PushbackReader. (StringReader. (str (.decode decoder (ByteBuffer/wrap bs)))))
          eof (Object.) value (edn/read {:eof eof} reader) tail (edn/read {:eof eof} reader)]
      (when (or (identical? eof value) (not (identical? eof tail)))
        (refuse! :e6b-capture/invalid-edn-cardinality {}))
      value)
    (catch clojure.lang.ExceptionInfo e (throw e))
    (catch Throwable e
      (throw (ex-info "invalid strict UTF-8 EDN" {:refusal :e6b-capture/invalid-edn} e)))))
(defn- exact-keys! [kind expected value]
  (when-not (and (map? value) (= expected (set (keys value))))
    (refuse! kind {:expected expected :actual (when (map? value) (set (keys value)))}))
  value)
(defn- digest! [kind value]
  (when-not (and (string? value) (re-matches hex64 value))
    (refuse! kind {:value value}))
  value)
(defn- descriptor [digest ^bytes source]
  ;; Clone exactly once at the candidate boundary; all parsing, hashing and
  ;; encoding below use this private clone.
  (let [bs (aclone source) actual (sha256 bs)]
    (when-not (= digest actual)
      (refuse! :e6b-capture/object-digest-mismatch {:expected digest :actual actual}))
    {:descriptor (array-map :digest digest
                            :bytes/base64 (.encodeToString (Base64/getEncoder) bs)
                            :source-sha256 actual)
     :record (strict-edn bs)}))
(defn- decode-descriptor [d]
  (exact-keys! :e6b-capture/descriptor-schema-invalid
               #{:digest :bytes/base64 :source-sha256} d)
  (digest! :e6b-capture/descriptor-digest-invalid (:digest d))
  (when-not (= (:digest d) (:source-sha256 d))
    (refuse! :e6b-capture/descriptor-pin-disagreement {}))
  (let [bs (try (.decode (Base64/getDecoder) ^String (:bytes/base64 d))
                (catch Throwable e
                  (throw (ex-info "invalid base64" {:refusal :e6b-capture/invalid-base64} e))))]
    (when-not (= (:digest d) (sha256 bs))
      (refuse! :e6b-capture/object-digest-mismatch {}))
    {:bytes bs :record (strict-edn bs)}))
(defn- index-row [row]
  (exact-keys! :e6b-capture/application-index-invalid
               #{:application/id :feedback/event-id :prior-state/revision
                 :transaction-sha256 :provenance-sha256} row)
  (when-not (every? #(and (string? %) (seq %))
                    ((juxt :application/id :feedback/event-id :prior-state/revision) row))
    (refuse! :e6b-capture/application-index-invalid {}))
  (digest! :e6b-capture/application-index-invalid (:transaction-sha256 row))
  (digest! :e6b-capture/application-index-invalid (:provenance-sha256 row))
  (array-map :application/id (:application/id row)
             :feedback/event-id (:feedback/event-id row)
             :prior-state/revision (:prior-state/revision row)
             :transaction-sha256 (:transaction-sha256 row)
             :provenance-sha256 (:provenance-sha256 row)))
(defn- tx-index-row [digest tx]
  (let [a (:application tx)]
    (index-row {:application/id (:application/id a)
                :feedback/event-id (:feedback/event-id a)
                :prior-state/revision (:prior-state/revision a)
                :transaction-sha256 digest
                :provenance-sha256 (:provenance-sha256 tx)})))
(defn- validate-structure! [record decoded-tx decoded-provenance]
  (let [chain (:chain-digests record) head (:record (decode-descriptor (:head-object record)))
        txs (mapv decoded-tx chain) applications (:application-universe record)]
    (exact-keys! :e6b-capture/head-schema-invalid
                 #{:schema :store/id :generation :state/revision :state-sha256
                   :transaction-sha256 :application-index} head)
    (when-not (and (= :wm/e6b-state-head-v2 (:schema head))
                   (= (:store/id record) (:store/id head))
                   (= (:generation record) (:generation head))
                   (= (:head-digest record) (get-in record [:head-object :source-sha256]))
                   (= (:head-digest record) (get-in record [:head-object :digest]))
                   (= (last chain) (:transaction-sha256 head))
                   (= applications (:application-index head))
                   (= (:generation record) (dec (count chain)))
                   (= {:revision (:state/revision head) :state-sha256 (:state-sha256 head)}
                      (let [current (peek txs)]
                        (if (= :wm/e6b-state-genesis-v2 (:schema current))
                          {:revision (:state/revision current)
                           :state-sha256 (:state-sha256 current)}
                          (select-keys (:next current) [:revision :state-sha256])))))
      (refuse! :e6b-capture/head-join-invalid {}))
    (when-not (and (vector? chain) (seq chain) (= (count chain) (count (distinct chain)))
                   (= (count applications) (dec (count chain)))
                   (every? #(= (count applications) (count (distinct (map % applications))))
                           [:application/id :feedback/event-id :prior-state/revision
                            :transaction-sha256 :provenance-sha256])
                   (= applications (mapv tx-index-row (rest chain) (rest txs))))
      (refuse! :e6b-capture/chain-index-invalid {}))
    (doseq [[generation digest tx] (map vector (range) chain txs)]
      (when-not (and (= (:store/id record) (:store/id tx))
                     (= generation (:generation tx)))
        (refuse! :e6b-capture/transaction-identity-invalid {:digest digest}))
      (if (zero? generation)
        (do
          (exact-keys! :e6b-capture/genesis-invalid
                       #{:schema :store/id :generation :prior :state :state/revision
                         :state-sha256 :application :authority :committed-at} tx)
          (when-not (and (= :wm/e6b-state-genesis-v2 (:schema tx))
                         (= {} (:prior tx)) (nil? (:application tx)))
            (refuse! :e6b-capture/genesis-invalid {})))
        (do
          (exact-keys! :e6b-capture/transaction-schema-invalid
                       #{:schema :store/id :generation :prior :next :application
                         :committed-at :provenance-sha256} tx)
          (when-not (= :wm/e6b-state-transaction-v2 (:schema tx))
            (refuse! :e6b-capture/transaction-schema-invalid {:digest digest}))
          (let [parent (nth txs (dec generation))
                parent-state (if (= :wm/e6b-state-genesis-v2 (:schema parent))
                               {:revision (:state/revision parent)
                                :state-sha256 (:state-sha256 parent)}
                               (select-keys (:next parent) [:revision :state-sha256]))]
            (when-not (and (= (dec generation) (get-in tx [:prior :generation]))
                           (= (nth chain (dec generation))
                              (get-in tx [:prior :transaction-sha256]))
                           (= parent-state (select-keys (:prior tx) [:revision :state-sha256])))
            (refuse! :e6b-capture/parent-disagreement {:digest digest}))
            )
          (let [pd (:provenance-sha256 tx) p (decoded-provenance pd)]
            (when-not p (refuse! :e6b-capture/missing-provenance {:digest pd}))
            ;; Reuse the reviewed pure provenance validation over these exact
            ;; captured bytes; do not trust its cached parsed record.
            (provenance/readback {:bytes/base64 (:bytes/base64 p)
                                  :expected-sha256 pd})))))
    (let [reachable (set (keep :provenance-sha256 (rest txs)))]
      (when-not (= reachable (set (keys decoded-provenance)))
        (refuse! :e6b-capture/provenance-reachability-invalid
                 {:reachable reachable :provided (set (keys decoded-provenance))})))
    record))

(def ^:private capture-keys
  #{:schema :authority/status :scope :store/id :generation :head-digest :head-object
    :chain-digests :application-universe :transaction-objects :provenance-objects
    :completeness-authority :rollback-freshness? :restart-authorized?})

(defn- validate-record! [record]
  (exact-keys! :e6b-capture/schema-invalid capture-keys record)
  (when-not (and (= :wm/e6b-complete-capture-v1 (:schema record))
                 (= :none (:authority/status record)) (= :isolated-test (:scope record))
                 (string? (:store/id record)) (seq (:store/id record))
                 (nat-int? (:generation record)) (= :absent (:completeness-authority record))
                 (= :unproved (:rollback-freshness? record))
                 (false? (:restart-authorized? record))
                 (vector? (:chain-digests record)) (vector? (:application-universe record))
                 (vector? (:transaction-objects record)) (vector? (:provenance-objects record)))
    (refuse! :e6b-capture/schema-invalid {}))
  (digest! :e6b-capture/head-digest-invalid (:head-digest record))
  (let [tx-pairs (mapv (fn [d] [(:digest d) (:record (decode-descriptor d))])
                       (:transaction-objects record))
        p-pairs (mapv (fn [d] [(:digest d) d]) (:provenance-objects record))]
    (when-not (and (= (count tx-pairs) (count (into {} tx-pairs)))
                   (= (count p-pairs) (count (into {} p-pairs))))
      (refuse! :e6b-capture/duplicate-object {}))
    (validate-structure! record (into {} tx-pairs) (into {} p-pairs))))

(defn construct
  "Encode one already recovered isolated store-v2 capture deterministically."
  [capture]
  (exact-keys! :e6b-capture/input-schema-invalid
               #{:schema :scope :store/id :generation :head-digest :head-object
                 :chain-digests :application-universe :transaction-objects
                 :provenance-objects :completeness-authority
                 :rollback-freshness? :restart-authorized?} capture)
  (when-not (= :wm/e6b-store-capture-v2 (:schema capture))
    (refuse! :e6b-capture/input-schema-invalid {}))
  (when-not (and (= :isolated-test (:scope capture))
                 (= :absent (:completeness-authority capture))
                 (= :unproved (:rollback-freshness? capture))
                 (false? (:restart-authorized? capture))
                 (vector? (:chain-digests capture)) (seq (:chain-digests capture))
                 (map? (:transaction-objects capture))
                 (= (set (:chain-digests capture)) (set (keys (:transaction-objects capture))))
                 (map? (:provenance-objects capture)))
    (refuse! :e6b-capture/input-schema-invalid {}))
  (let [head-in (:head-object capture)
        _ (exact-keys! :e6b-capture/head-descriptor-invalid
                       #{:bytes/base64 :source-sha256} head-in)
        _ (when-not (= (:head-digest capture) (:source-sha256 head-in))
            (refuse! :e6b-capture/head-descriptor-invalid {}))
        head-bytes (try (.decode (Base64/getDecoder) ^String (:bytes/base64 head-in))
                        (catch Throwable e
                          (throw (ex-info "invalid HEAD base64"
                                          {:refusal :e6b-capture/invalid-base64} e))))
        head (descriptor (:head-digest capture) head-bytes)
        txs (mapv (fn [digest]
                    (:descriptor (descriptor digest (get (:transaction-objects capture) digest))))
                  (:chain-digests capture))
        pds (sort (keys (:provenance-objects capture)))
        ps (mapv (fn [digest]
                   (:descriptor (descriptor digest (get (:provenance-objects capture) digest)))) pds)
        record (array-map
                :schema :wm/e6b-complete-capture-v1 :authority/status :none
                :scope :isolated-test :store/id (:store/id capture)
                :generation (:generation capture) :head-digest (:head-digest capture)
                :head-object (:descriptor head)
                :chain-digests (vec (:chain-digests capture))
                :application-universe (mapv index-row (:application-universe capture))
                :transaction-objects txs :provenance-objects ps
                :completeness-authority :absent :rollback-freshness? :unproved
                :restart-authorized? false)
        _ (validate-record! record)
        bs (.getBytes (pr-str record) StandardCharsets/UTF_8)]
    {:schema :wm/e6b-complete-capture-artifact-v1
     :authority/status :none
     :bytes/base64 (.encodeToString (Base64/getEncoder) bs)
     :sha256 (sha256 bs)}))

(defn readback
  "Read and fully revalidate a serialized complete capture at an external pin."
  [{:keys [bytes/base64 expected-sha256] :as input}]
  (exact-keys! :e6b-capture/readback-input-invalid #{:bytes/base64 :expected-sha256} input)
  (digest! :e6b-capture/readback-pin-invalid expected-sha256)
  (let [bs (try (.decode (Base64/getDecoder) ^String base64)
                (catch Throwable e
                  (throw (ex-info "invalid base64" {:refusal :e6b-capture/invalid-base64} e))))]
    (when-not (= expected-sha256 (sha256 bs))
      (refuse! :e6b-capture/readback-pin-mismatch {}))
    (let [record (validate-record! (strict-edn bs))
          rebuilt (construct
                   {:schema :wm/e6b-store-capture-v2 :scope :isolated-test
                    :store/id (:store/id record) :generation (:generation record)
                    :head-digest (:head-digest record)
                    :head-object (select-keys (:head-object record)
                                              [:bytes/base64 :source-sha256])
                    :chain-digests (:chain-digests record)
                    :application-universe (:application-universe record)
                    :transaction-objects
                    (into {} (map (fn [d] [(:digest d) (:bytes (decode-descriptor d))])
                                  (:transaction-objects record)))
                    :provenance-objects
                    (into {} (map (fn [d] [(:digest d) (:bytes (decode-descriptor d))])
                                  (:provenance-objects record)))
                    :completeness-authority :absent :rollback-freshness? :unproved
                    :restart-authorized? false})]
      (when-not (= expected-sha256 (:sha256 rebuilt))
        (refuse! :e6b-capture/revalidation-mismatch {}))
      {:schema :wm/e6b-complete-capture-readback-v1
       :authority/status :none :sha256 expected-sha256 :record record})))
