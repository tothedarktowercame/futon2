(ns futon2.aif.machine-slow-prior-evidence
  "Pure E5 evidence verifier. Resolves a complete unshaped occurrence domain,
   slow-state/weight authority, claimed shaped output, and independent depth;
   recomputes the production slow-prior function without selecting or scoring."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.temporal-hierarchy :as hierarchy])
  (:import (java.io PushbackReader StringReader)
           (java.nio ByteBuffer)
           (java.nio.charset CodingErrorAction StandardCharsets)
           (java.nio.file Files LinkOption Path)
           (java.security MessageDigest)))

(def schema-version :wm/r15-r6-slow-prior-evidence-v1)
(def ^:private schemas
  {:unshaped :wm/e5-unshaped-candidates-v1
   :slow-state :wm/e5-slow-state-authority-v1
   :shaped :wm/e5-shaped-candidates-v1
   :depth :wm/e5-independent-depth-authority-v1})

(defn- refuse! [kind message data]
  (throw (ex-info message (assoc data :refusal kind))))
(defn- hex [bytes] (apply str (map #(format "%02x" (bit-and 0xff %)) bytes)))
(defn- sha256 [bytes]
  (hex (.digest (doto (MessageDigest/getInstance "SHA-256") (.update bytes)))))
(defn- strict-form! [bytes path]
  (try
    (let [decoder (doto (.newDecoder StandardCharsets/UTF_8)
                    (.onMalformedInput CodingErrorAction/REPORT)
                    (.onUnmappableCharacter CodingErrorAction/REPORT))
          rdr (PushbackReader. (StringReader. (str (.decode decoder (ByteBuffer/wrap bytes)))))
          eof (Object.) form (edn/read {:eof eof} rdr) tail (edn/read {:eof eof} rdr)]
      (when (identical? eof form) (refuse! :e5/source-empty "Empty source" {:path (str path)}))
      (when-not (identical? eof tail) (refuse! :e5/source-trailing-form "Trailing EDN" {:path (str path)}))
      form)
    (catch clojure.lang.ExceptionInfo e (throw e))
    (catch Throwable e (refuse! :e5/source-malformed "Malformed UTF-8 EDN"
                                {:path (str path) :cause (.getMessage e)}))))
(defn- resolve! [root label pin]
  (let [relative-path (:relative-path pin) expected-sha (:sha256 pin)]
  (when-not (and (string? expected-sha) (re-matches #"[0-9a-f]{64}" expected-sha))
    (refuse! :e5/source-pin-missing "Source SHA-256 is missing" {:label label}))
  (let [base (.normalize (.toAbsolutePath (.toPath (io/file root))))
        rel (Path/of (str relative-path) (make-array String 0))
        path (.normalize (.toAbsolutePath (.resolve base rel)))]
    (when (or (.isAbsolute rel) (not (.startsWith path base)))
      (refuse! :e5/source-path-escape "Source escapes root" {:label label}))
    (when-not (Files/isRegularFile path (into-array LinkOption [LinkOption/NOFOLLOW_LINKS]))
      (refuse! :e5/source-unreadable "Source is not a regular file" {:label label}))
    (let [bytes (Files/readAllBytes path) actual (sha256 bytes) record (strict-form! bytes path)]
      (when-not (= expected-sha actual) (refuse! :e5/source-pin-mismatch "Source bytes changed" {:label label}))
      (when-not (= (schemas label) (:schema/version record))
        (refuse! :e5/source-schema-mismatch "Source schema differs" {:label label}))
      {:label label :sha256 actual :record record}))))

(defn- finite-number? [x]
  (and (number? x) (Double/isFinite (double x))))
(defn- identity-of [record]
  (select-keys record [:model/id :model/revision :run/id :tick/index]))
(defn- valid-candidate? [candidate]
  (and (= #{:candidate/occurrence-id :action :move/class :prior :step-score-delta}
          (set (keys candidate)))
       (some? (:candidate/occurrence-id candidate))
       (map? (:action candidate)) (seq (:action candidate))
       (keyword? (:move/class candidate))
       (finite-number? (:prior candidate)) (pos? (double (:prior candidate)))
       (finite-number? (:step-score-delta candidate))))
(defn- as-production-move [candidate]
  (-> candidate
      (assoc :move/id (:candidate/occurrence-id candidate))
      (dissoc :candidate/occurrence-id)))
(defn- as-evidence-candidate [move]
  (-> move
      (assoc :candidate/occurrence-id (:move/id move))
      (dissoc :move/id)))

(defn verify-shaping
  [{:keys [mode root sources]}]
  (when-not (contains? #{:isolated-test :production} mode)
    (refuse! :e5/mode-unknown "Unknown verifier mode" {:mode mode}))
  (when (= :production mode)
    (refuse! :e5/production-authority-unavailable "Independent production sources are absent" {}))
  (when-not (= (set (keys schemas)) (set (keys sources)))
    (refuse! :e5/source-set-incomplete "All four sources are required" {}))
  (let [resolved (mapv #(resolve! root % (sources %)) [:unshaped :slow-state :shaped :depth])
        records (into {} (map (juxt :label :record) resolved))
        input (:unshaped records) slow (:slow-state records)
        claimed (:shaped records) depth (:depth records)
        identities (mapv identity-of (vals records))
        candidates (:candidates input)
        ids (mapv :candidate/occurrence-id candidates)
        mode-value (:slow/mode slow)
        weights (:weight-table slow)]
    (when-not (and (apply = identities) (= :isolated-test (:scope input)
                   (:scope slow) (:scope claimed) (:scope depth)))
      (refuse! :e5/cross-run-or-scope "Sources disagree on identity or scope" {}))
    (when-not (and (vector? candidates) (seq candidates) (every? valid-candidate? candidates)
                   (= (count ids) (count (distinct ids))))
      (refuse! :e5/unshaped-domain-invalid "Unshaped occurrence domain is incomplete" {}))
    (when-not (and (keyword? mode-value) (map? (:slow/intrinsics slow))
                   (not (str/blank? (str (:weight-table/revision slow))))
                   (= weights (hierarchy/mode-prior-weights mode-value)))
      (refuse! :e5/slow-authority-invalid "Slow mode/weight authority is absent or stale" {}))
    (when-not (every? #(and (finite-number? %) (pos? (double %))) (vals weights))
      (refuse! :e5/invalid-weight "Weights must be finite and positive" {}))
    (when-not (and (pos-int? (:horizon/requested depth))
                   (pos-int? (:horizon/effective depth)))
      (refuse! :e5/depth-authority-invalid "Independent depth authority is invalid" {}))
    (when-not (= {:requested (:horizon/requested depth) :effective (:horizon/effective depth)}
                 (:depth/unchanged claimed))
      (refuse! :e5/horizon-rewrite "Shaping must not rewrite independent depth" {}))
    (let [actual (mapv as-evidence-candidate
                       (hierarchy/apply-slow-prior (mapv as-production-move candidates) mode-value))]
      (when-not (= ids (mapv :candidate/occurrence-id (:candidates claimed)))
        (refuse! :e5/domain-mismatch "Shaped output dropped, reordered, or replaced occurrences" {}))
      (when-not (= actual (:candidates claimed))
        (refuse! :e5/shaping-mismatch "Claimed shaped table differs from production function" {}))
      {:schema/version schema-version :scope :isolated-test
       :identity (first identities) :slow/mode mode-value
       :slow/intrinsics (:slow/intrinsics slow)
       :weight-table {:revision (:weight-table/revision slow) :weights weights}
       :depth {:authority :independent :requested (:horizon/requested depth)
               :effective (:horizon/effective depth) :unchanged true}
       :unshaped candidates :shaped actual
       :law {:prior :base-times-weight
             :step-score-delta :base-plus-negative-log-weight
             :base-cost-retained true}
       :sources (mapv #(dissoc % :record) resolved)
       :external-dependencies {:r13-depth :independently-required
                               :r6-scoring-and-selection :not-performed
                               :e6-forward-and-feedback :open}})))
