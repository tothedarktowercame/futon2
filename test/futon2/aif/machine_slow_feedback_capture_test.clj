(ns futon2.aif.machine-slow-feedback-capture-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.machine-slow-feedback-capture :as codec]
            [futon2.aif.machine-slow-feedback-provenance :as provenance]
            [futon2.aif.machine-slow-feedback-store-v2 :as store]
            [futon2.aif.machine-slow-feedback-store-v2-test :as store-test])
  (:import (java.nio.charset StandardCharsets)
           (java.util Base64)))

(defn- refusal [f]
  (try (f) nil (catch clojure.lang.ExceptionInfo e (:refusal (ex-data e)))))
(defn- setup-capture [committed?]
  (let [[_ s artifact] (#'store-test/setup)]
    (when committed? (store/commit! s (#'store-test/pin artifact)))
    [s (store/capture s)]))
(defn- artifact-pin [artifact]
  {:bytes/base64 (:bytes/base64 artifact) :expected-sha256 (:sha256 artifact)})
(defn- record [artifact]
  (edn/read-string (String. (.decode (Base64/getDecoder) ^String (:bytes/base64 artifact))
                            StandardCharsets/UTF_8)))
(defn- artifact-from-record [r]
  (let [bs (.getBytes (pr-str r) StandardCharsets/UTF_8)]
    {:bytes/base64 (.encodeToString (Base64/getEncoder) bs)
     :expected-sha256 (#'codec/sha256 bs)}))
(defn- decode-bytes [^bytes bs] (edn/read-string (String. bs StandardCharsets/UTF_8)))
(defn- encode-record [r] (.getBytes (pr-str r) StandardCharsets/UTF_8))
(defn- replace-last-tx [capture tx]
  (let [old (last (:chain-digests capture)) bs (encode-record tx) digest (#'codec/sha256 bs)
        index (assoc-in (:application-universe capture) [(dec (count (:application-universe capture)))
                                                         :transaction-sha256] digest)
        head (decode-bytes (.decode (Base64/getDecoder)
                                    ^String (get-in capture [:head-object :bytes/base64])))
        head' (assoc head :transaction-sha256 digest :application-index index
                     :state/revision (get-in tx [:next :revision])
                     :state-sha256 (get-in tx [:next :state-sha256]))
        hb (encode-record head') hd (#'codec/sha256 hb)]
    (-> capture
        (assoc :head-digest hd
               :head-object {:bytes/base64 (.encodeToString (Base64/getEncoder) hb)
                             :source-sha256 hd}
               :application-universe index
               :chain-digests (assoc (:chain-digests capture)
                                     (dec (count (:chain-digests capture))) digest))
        (update :transaction-objects #(assoc (dissoc % old) digest bs)))))
(defn- rebuild-provenance [record]
  (provenance/construct
   {:proposal-evidence (:proposal-evidence record)
    :original-sources (update-vals (:original-sources record) #(dissoc % :record))
    :canonical-closure (update-vals (get-in record [:canonical-closure :inputs]) #(dissoc % :record))
    :canonical-outputs (update-vals (get-in record [:canonical-closure :outputs]) #(dissoc % :record))
    :carrier-projection (:carrier-projection record)
    :expected-head (:expected-head record)}))

(deftest deterministic-genesis-and-non-genesis-roundtrip
  (doseq [committed? [false true]]
    (let [[s capture] (setup-capture committed?)
          a (codec/construct capture)
          reordered (assoc capture
                           :transaction-objects (into (sorted-map) (:transaction-objects capture))
                           :provenance-objects (into (sorted-map) (:provenance-objects capture)))
          b (codec/construct reordered)
          rb (codec/readback (artifact-pin a))]
      (is (= (:sha256 a) (:sha256 b) (:sha256 rb)))
      (is (= (if committed? 1 0) (get-in rb [:record :generation])))
      (is (= :none (:authority/status rb)))
      (is (= :absent (get-in rb [:record :completeness-authority])))
      (is (false? (get-in rb [:record :restart-authorized?])))
      (store/release! s))))

(deftest construct-clones-caller-buffers
  (let [[s capture] (setup-capture true)
        a (codec/construct capture)
        first-buffer (val (first (:transaction-objects capture)))]
    (aset-byte ^bytes first-buffer 0 (byte 0))
    (is (= (:sha256 a) (:sha256 (codec/readback (artifact-pin a)))))
    (store/release! s)))

(deftest missing-extra-and-mutated-objects-refuse
  (let [[s capture] (setup-capture true)
        tx-key (first (keys (:transaction-objects capture)))
        p-key (first (keys (:provenance-objects capture)))]
    (is (some? (refusal #(codec/construct
                          (update capture :transaction-objects dissoc tx-key)))))
    (is (some? (refusal #(codec/construct
                          (assoc-in capture [:transaction-objects (apply str (repeat 64 "f"))]
                                    (byte-array [1]))))))
    (let [changed (aclone ^bytes (get (:provenance-objects capture) p-key))]
      (aset-byte changed 0 (byte 0))
      (is (= :e6b-capture/object-digest-mismatch
             (refusal #(codec/construct (assoc-in capture [:provenance-objects p-key] changed))))))
    (store/release! s)))

(deftest strict-readback-controls
  (let [[s capture] (setup-capture true) a (codec/construct capture)
        valid-bytes (.decode (Base64/getDecoder) ^String (:bytes/base64 a))]
    (doseq [[label bs expected]
            [[:invalid-utf8 (byte-array [(unchecked-byte 0xc3) (byte 0x28)]) :e6b-capture/invalid-edn]
             [:trailing (.getBytes (str (String. valid-bytes StandardCharsets/UTF_8) " nil")
                                   StandardCharsets/UTF_8)
              :e6b-capture/invalid-edn-cardinality]
             [:duplicate-key (.getBytes "{:schema :a :schema :b}" StandardCharsets/UTF_8)
              :e6b-capture/invalid-edn]]]
      (testing (name label)
        (is (= expected
               (refusal #(codec/readback
                          {:bytes/base64 (.encodeToString (Base64/getEncoder) bs)
                           :expected-sha256 (#'codec/sha256 bs)}))))))
    (is (= :e6b-capture/readback-pin-mismatch
           (refusal #(codec/readback (assoc (artifact-pin a) :expected-sha256
                                            (apply str (repeat 64 "0")))))))
    (store/release! s)))

(deftest coherent-record-mutations-still-refuse
  (let [[s capture] (setup-capture true) a (codec/construct capture) r (record a)
        duplicate (update r :transaction-objects conj (first (:transaction-objects r)))
        bad-index (assoc-in r [:application-universe 0 :prior-state/revision] "borrowed")
        bad-head (assoc r :generation 8)
        omitted (dissoc r :provenance-objects)]
    (is (= :e6b-capture/duplicate-object
           (refusal #(codec/readback (artifact-from-record duplicate)))))
    (is (= :e6b-capture/head-join-invalid
           (refusal #(codec/readback (artifact-from-record bad-index)))))
    (is (= :e6b-capture/head-join-invalid
           (refusal #(codec/readback (artifact-from-record bad-head)))))
    (is (= :e6b-capture/schema-invalid
           (refusal #(codec/readback (artifact-from-record omitted)))))
    (store/release! s)))

(deftest transaction-provenance-and-state-joins-refuse-coherent-rehashes
  (let [[s c] (setup-capture true)
        old (last (:chain-digests c)) tx (decode-bytes (get (:transaction-objects c) old))]
    (is (= :e6b-capture/transaction-provenance-disagreement
           (refusal #(codec/construct
                      (replace-last-tx c (assoc-in tx [:application :status] :borrowed))))))
    (let [state (assoc-in (get-in tx [:next :state]) [:slow/intrinsics :alpha :alpha] 99.0)
          changed (assoc tx :next (assoc (:next tx) :state state
                                         :state-sha256 (#'codec/sha256 (encode-record state))))]
      (is (= :e6b-capture/transaction-provenance-disagreement
             (refusal #(codec/construct (replace-last-tx c changed))))))
    ;; Reusing the consumed prior revision as HEAD destination is rejected by
    ;; the linear revision census before provenance could disguise it.
    (let [revision (get-in tx [:prior :revision])
          state (assoc (get-in tx [:next :state]) :state/revision revision)
          changed (assoc tx :next {:revision revision :state state
                                   :state-sha256 (#'codec/sha256 (encode-record state))})]
      (is (= :e6b-capture/state-revision-conflict
             (refusal #(codec/construct (replace-last-tx c changed))))))
    (store/release! s)))

(deftest provenance-expected-parent-must-be-the-actual-parent
  (let [[s c] (setup-capture true)
        old-tx (last (:chain-digests c)) tx (decode-bytes (get (:transaction-objects c) old-tx))
        old-p (:provenance-sha256 tx)
        p-record (decode-bytes (get (:provenance-objects c) old-p))
        forged (rebuild-provenance
                (assoc-in p-record [:expected-head :transaction-sha256]
                          (apply str (repeat 64 "f"))))
        pd (:sha256 forged)
        c' (-> c
               (assoc-in [:provenance-objects pd]
                         (.decode (Base64/getDecoder) ^String (:bytes/base64 forged)))
               (update :provenance-objects dissoc old-p)
               (assoc-in [:application-universe 0 :provenance-sha256] pd)
               (replace-last-tx (assoc tx :provenance-sha256 pd)))]
    (is (= :e6b-capture/provenance-parent-disagreement
           (refusal #(codec/construct c'))))
    (store/release! s)))

(deftest genesis-semantics-are-replayed-not-assumed
  (let [[s c] (setup-capture false)
        old (first (:chain-digests c)) genesis (decode-bytes (get (:transaction-objects c) old))
        forged (assoc genesis :authority nil) bs (encode-record forged) digest (#'codec/sha256 bs)
        head (decode-bytes (.decode (Base64/getDecoder)
                                    ^String (get-in c [:head-object :bytes/base64])))
        head' (assoc head :transaction-sha256 digest) hb (encode-record head') hd (#'codec/sha256 hb)
        c' (-> c
               (assoc :head-digest hd
                      :head-object {:bytes/base64 (.encodeToString (Base64/getEncoder) hb)
                                    :source-sha256 hd}
                      :chain-digests [digest]
                      :transaction-objects {digest bs}))]
    (is (= :e6b-store-v2/genesis-authority-invalid (refusal #(codec/construct c'))))
    (store/release! s)))

(deftest coherent-prior-generation-refuses
  (let [[s c] (setup-capture true)]
  (try
    (let [old (last (:chain-digests c))
          tx (edn/read-string (String. ^bytes (get (:transaction-objects c) old) "UTF-8"))
          bad (assoc-in tx [:prior :generation] 999)
          bs (.getBytes (pr-str bad) "UTF-8") d (#'codec/sha256 bs)
          index (assoc-in (:application-universe c) [0 :transaction-sha256] d)
          head (edn/read-string (String. (.decode (Base64/getDecoder) ^String (get-in c [:head-object :bytes/base64])) "UTF-8"))
          head' (assoc head :transaction-sha256 d :application-index index)
          hb (.getBytes (pr-str head') "UTF-8") hd (#'codec/sha256 hb)
          c' (-> c (assoc :head-digest hd :head-object {:bytes/base64 (.encodeToString (Base64/getEncoder) hb) :source-sha256 hd}
                         :application-universe index :chain-digests (assoc (:chain-digests c) 1 d))
                 (update :transaction-objects #(assoc (dissoc % old) d bs)))
          result (refusal #(codec/construct c'))]
      (is (= :e6b-capture/parent-disagreement result)))
    (finally (store/release! s)))))
