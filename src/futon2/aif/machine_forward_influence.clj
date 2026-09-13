(ns futon2.aif.machine-forward-influence
  "Pure bounded E6a comparison. It replays two E5 arms under one fixed
   context, but refuses before E2/E3/E2b unless canonical R6 score/posterior
   correspondence exists. No asserted score record can satisfy that absence."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.machine-slow-prior-evidence :as e5])
  (:import (java.io PushbackReader StringReader)
           (java.nio ByteBuffer)
           (java.nio.charset CodingErrorAction StandardCharsets)
           (java.nio.file Files LinkOption Path)
           (java.security MessageDigest)))

(def schema-version :wm/r15-r6-r16-forward-influence-v1)
(def ^:private schemas
  {:fixed-context :wm/e6a-fixed-comparison-context-v1
   :r6-scoring-a :wm/e6a-r6-correspondence-status-v1
   :r6-scoring-b :wm/e6a-r6-correspondence-status-v1})

(defn- refuse! [kind message data]
  (throw (ex-info message (assoc data :refusal kind))))
(defn- digest [bytes]
  (apply str (map #(format "%02x" (bit-and 0xff %))
                  (.digest (doto (MessageDigest/getInstance "SHA-256") (.update bytes))))))
(defn- strict-form! [bytes path]
  (try
    (let [decoder (doto (.newDecoder StandardCharsets/UTF_8)
                    (.onMalformedInput CodingErrorAction/REPORT)
                    (.onUnmappableCharacter CodingErrorAction/REPORT))
          rdr (PushbackReader. (StringReader. (str (.decode decoder (ByteBuffer/wrap bytes)))))
          eof (Object.) form (edn/read {:eof eof} rdr) tail (edn/read {:eof eof} rdr)]
      (when (identical? eof form) (refuse! :e6a/source-empty "Empty source" {:path (str path)}))
      (when-not (identical? eof tail) (refuse! :e6a/source-trailing-form "Trailing EDN" {:path (str path)}))
      form)
    (catch clojure.lang.ExceptionInfo e (throw e))
    (catch Throwable e (refuse! :e6a/source-malformed "Malformed UTF-8 EDN"
                                {:path (str path) :cause (.getMessage e)}))))
(defn- resolve! [root label pin]
  (let [expected (:sha256 pin)]
    (when-not (and (string? expected) (re-matches #"[0-9a-f]{64}" expected))
      (refuse! :e6a/source-pin-missing "Missing source pin" {:label label}))
    (let [base (.normalize (.toAbsolutePath (.toPath (io/file root))))
          rel (Path/of (str (:relative-path pin)) (make-array String 0))
          path (.normalize (.toAbsolutePath (.resolve base rel)))]
      (when (or (.isAbsolute rel) (not (.startsWith path base)))
        (refuse! :e6a/source-path-escape "Source escapes root" {:label label}))
      (when-not (Files/isRegularFile path (into-array LinkOption [LinkOption/NOFOLLOW_LINKS]))
        (refuse! :e6a/source-unreadable "Source is not a regular file" {:label label}))
      (let [bytes (Files/readAllBytes path) actual (digest bytes) record (strict-form! bytes path)]
        (when-not (= expected actual) (refuse! :e6a/source-pin-mismatch "Source bytes changed" {:label label}))
        (when-not (= (schemas label) (:schema/version record))
          (refuse! :e6a/source-schema-mismatch "Source schema differs" {:label label}))
        {:label label :sha256 actual :record record}))))

(defn verify-forward-influence
  [{:keys [mode authority-root sources e5-resolvers]}]
  (when-not (contains? #{:isolated-test :production} mode)
    (refuse! :e6a/mode-unknown "Unknown mode" {:mode mode}))
  (when (= :production mode)
    (refuse! :e6a/production-authority-unavailable "Production E6a sources are absent" {}))
  (when-not (= (set (keys schemas)) (set (keys sources)))
    (refuse! :e6a/source-set-incomplete "Fixed context and both R6 status records are required" {}))
  (when-not (= #{:a :b} (set (keys e5-resolvers)))
    (refuse! :e6a/e5-resolvers-incomplete "Both independently configured E5 resolvers are required" {}))
  (let [resolved (mapv #(resolve! authority-root % (sources %))
                       [:fixed-context :r6-scoring-a :r6-scoring-b])
        records (into {} (map (juxt :label :record) resolved))
        context (:fixed-context records)
        arm-a (e5/verify-shaping (:a e5-resolvers))
        arm-b (e5/verify-shaping (:b e5-resolvers))
        expected-identity (:identity context)
        expected-domain (:unshaped context)]
    (when-not (and (= :isolated-test (:scope context))
                   (= expected-identity (:identity arm-a) (:identity arm-b))
                   (= expected-domain (:unshaped arm-a) (:unshaped arm-b))
                   (= (:depth arm-a) (:depth arm-b) (:depth context))
                   (= (:arm/id-a context) :a) (= (:arm/id-b context) :b)
                   (not= (:slow/mode arm-a) (:slow/mode arm-b))
                   (= [(:slow/mode arm-a) (:slow/mode arm-b)] (:slow/modes context)))
      (refuse! :e6a/fixed-context-mismatch "Arms differ outside independently fixed slow state" {}))
    (if (= (:shaped arm-a) (:shaped arm-b))
      {:schema/version schema-version :scope :isolated-test
       :qualification :no-behavioral-influence
       :reason :shaped-tables-identical
       :arms [{:arm/id :a :slow/mode (:slow/mode arm-a)}
              {:arm/id :b :slow/mode (:slow/mode arm-b)}]
       :note "No-change cannot qualify causal influence."}
      (do
        (doseq [label [:r6-scoring-a :r6-scoring-b]
                :let [record (label records) arm (if (= label :r6-scoring-a) arm-a arm-b)]]
          (when-not (and (= :isolated-test (:scope record))
                         (= expected-identity (:identity record))
                         (= (:shaped arm) (:shaped-input record))
                         (= :unavailable (:correspondence/status record))
                         (= :canonical-r6-score-and-posterior-proof-absent (:reason record)))
            (refuse! :e6a/r6-status-subject-mismatch "R6 absence record is stale or asserted" {:label label})))
        (refuse! :e6a/r6-scoring-correspondence-unavailable
                 "No canonical complete R6 scoring/posterior correspondence exists"
                 {:changed? true
                  :first-changed-occurrence
                  (some (fn [[a b]] (when (not= a b) (:candidate/occurrence-id a)))
                        (map vector (:shaped arm-a) (:shaped arm-b)))
                  :downstream {:e2a :not-reached :e3 :not-reached :e2b :not-reached}
                  :sources (mapv #(dissoc % :record) resolved)})))))
