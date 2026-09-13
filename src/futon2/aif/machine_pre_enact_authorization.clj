(ns futon2.aif.machine-pre-enact-authorization
  "Pure E3 verifier for an exact pending occurrence/construction. It emits an
   isolated mechanism result only; no action is authorized or enacted here."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.r9-checker :as r9])
  (:import (java.io PushbackReader StringReader)
           (java.nio ByteBuffer)
           (java.nio.charset CodingErrorAction StandardCharsets)
           (java.nio.file Files LinkOption Path)
           (java.security MessageDigest)
           (java.time Instant)))

(def schema-version :wm/r9-pre-enact-authorization-v1)
(def ^:private schemas
  {:pending :wm/e3-pending-construction-v1
   :verdict :wm/e3-independence-verdict-v1
   :review :wm/e3-independent-review-v1})

(defn- refuse! [kind message data]
  (throw (ex-info message (assoc data :refusal kind))))

(defn- digest [bytes]
  (apply str (map #(format "%02x" (bit-and 0xff %))
                  (.digest (doto (MessageDigest/getInstance "SHA-256")
                             (.update bytes))))))

(defn- strict-form! [bytes path]
  (try
    (let [decoder (doto (.newDecoder StandardCharsets/UTF_8)
                    (.onMalformedInput CodingErrorAction/REPORT)
                    (.onUnmappableCharacter CodingErrorAction/REPORT))
          rdr (PushbackReader. (StringReader. (str (.decode decoder (ByteBuffer/wrap bytes)))))
          eof (Object.) form (edn/read {:eof eof} rdr) tail (edn/read {:eof eof} rdr)]
      (when (identical? eof form) (refuse! :e3/evidence-empty "Empty evidence" {:path (str path)}))
      (when-not (identical? eof tail) (refuse! :e3/evidence-trailing-form "Trailing EDN" {:path (str path)}))
      form)
    (catch clojure.lang.ExceptionInfo e (throw e))
    (catch Throwable e (refuse! :e3/evidence-malformed "Malformed UTF-8 EDN"
                                {:path (str path) :cause (.getMessage e)}))))

(defn- resolve-one! [root label {:keys [relative-path sha256]}]
  (when-not (and (string? sha256) (re-matches #"[0-9a-f]{64}" sha256))
    (refuse! :e3/pin-missing "Evidence pin missing" {:label label}))
  (let [base (.normalize (.toAbsolutePath (.toPath (io/file root))))
        rel (Path/of (str relative-path) (make-array String 0))
        path (.normalize (.toAbsolutePath (.resolve base rel)))]
    (when (or (.isAbsolute rel) (not (.startsWith path base)))
      (refuse! :e3/path-escape "Evidence path escapes root" {:label label}))
    (when-not (Files/isRegularFile path (into-array LinkOption [LinkOption/NOFOLLOW_LINKS]))
      (refuse! :e3/evidence-unreadable "Evidence is not a regular file" {:label label}))
    (let [bytes (Files/readAllBytes path) actual (digest bytes) record (strict-form! bytes path)]
      (when-not (= sha256 actual) (refuse! :e3/pin-mismatch "Evidence bytes changed" {:label label}))
      (when-not (= (schemas label) (:schema/version record))
        (refuse! :e3/schema-mismatch "Evidence schema mismatch" {:label label}))
      {:label label :sha256 actual :record record})))

(defn- nonblank? [x] (and (string? x) (not (str/blank? x))))
(defn- sha-text [x] (digest (.getBytes (pr-str x) StandardCharsets/UTF_8)))
(defn- valid-pin? [pin]
  (and (= #{:label :sha256} (set (keys pin)))
       (keyword? (:label pin))
       (string? (:sha256 pin))
       (re-matches #"[0-9a-f]{64}" (:sha256 pin))))
(defn- valid-subject? [subject]
  (and (= #{:candidate/occurrence-id :action :construction :field-pins
            :producer/id :claim/id :artifact/ref :trace/id}
          (set (keys subject)))
       (every? nonblank? ((juxt :candidate/occurrence-id :producer/id :claim/id
                                :artifact/ref :trace/id) subject))
       (map? (:action subject)) (seq (:action subject))
       (map? (:construction subject)) (seq (:construction subject))
       (vector? (:field-pins subject)) (seq (:field-pins subject))
       (every? valid-pin? (:field-pins subject))
       (= (count (:field-pins subject)) (count (distinct (map :label (:field-pins subject)))))))
(defn- before? [a b]
  (try (.isBefore (Instant/parse a) (Instant/parse b)) (catch Throwable _ false)))

(defn verify-pre-enact
  "Resolve independent pinned pending/verdict/review records and verify that an
   independent R9 verdict names the exact not-yet-enacted subject."
  [{:keys [mode evidence-root evidence]}]
  (when-not (contains? #{:isolated-test :production} mode)
    (refuse! :e3/mode-unknown "Unknown mode" {:mode mode}))
  (when (= :production mode)
    (refuse! :e3/production-authority-unavailable
             "Serving R9 retention/genesis and pending-event authority are not installed" {}))
  (when-not (= #{:pending :verdict :review} (set (keys evidence)))
    (refuse! :e3/evidence-set-incomplete "Pending, verdict and review records are required" {}))
  (let [resolved (mapv #(resolve-one! evidence-root % (evidence %)) [:pending :verdict :review])
        records (into {} (map (juxt :label :record) resolved))
        pending (:pending records) verdict (:verdict records) review (:review records)
        subject (:subject pending)
        ids [:model/id :model/revision :run/id :cohort/id :tick/index :event/id]
        exact-subject-keys #{:candidate/occurrence-id :action :construction :field-pins
                            :producer/id :claim/id :artifact/ref :trace/id}]
    (when-not (and (= :isolated-test (:scope pending))
                   (every? nonblank? ((juxt :model/id :model/revision :run/id
                                           :cohort/id :event/id) pending))
                   (nat-int? (:tick/index pending))
                   (= exact-subject-keys (set (keys subject)))
                   (valid-subject? subject))
      (refuse! :e3/pending-context-invalid "Pending context/subject is incomplete" {}))
    (when-not (= :pending-pre-enact (:phase pending))
      (refuse! :e3/not-pending-pre-enact "Context is post-event or retroactive" {:phase (:phase pending)}))
    (doseq [[label record] [[:verdict verdict] [:review review]]]
      (when-not (= :isolated-test (:scope record))
        (refuse! :e3/scope-laundering "All evidence must retain isolated scope" {:label label}))
      (when-not (= (select-keys pending ids) (select-keys record ids))
        (refuse! :e3/context-mismatch "Evidence belongs to another event context" {:label label}))
      (when-not (= subject (:subject record))
        (refuse! :e3/subject-mismatch "Evidence belongs to another construction" {:label label})))
    (when-not (map? (:r9/input review))
      (refuse! :e3/canonical-r9-evidence-missing "Canonical R9 input is absent" {}))
    (let [expected-r9-subject {:boundary :e3/pre-enact
                               :artifact-ref (:artifact/ref subject)
                               :digest (sha-text subject)}
          r9-input (:r9/input review)]
      (when-not (= expected-r9-subject (:subject r9-input))
        (refuse! :e3/canonical-r9-subject-mismatch "Canonical R9 input names another subject" {}))
      (when-not (and (= (:producer/id subject) (get-in r9-input [:role-binding :author]))
                     (= (:reviewer/id review) (get-in r9-input [:role-binding :reviewer]))
                     (= (:trace/id subject) (get-in r9-input [:producer-job :trace-id]))
                     (= (:review-trace/id review) (get-in r9-input [:reviewer-job :trace-id])))
        (refuse! :e3/canonical-r9-provenance-mismatch "Canonical R9 roles/traces differ" {}))
      (let [checked (r9/check-independence r9-input)]
        (when-not (= checked (:canonical-admission verdict))
          (refuse! :e3/canonical-r9-admission-mismatch "Verdict is not the canonical checked result" {}))))
    (when (or (not (nonblank? (:producer/id subject)))
              (not (nonblank? (:reviewer/id review))))
      (refuse! :e3/identity-missing "Producer or reviewer identity missing" {}))
    (when (= (:producer/id subject) (:reviewer/id review))
      (refuse! :e3/self-review "Producer and reviewer are the same seat" {}))
    (when-not (and (= (:reviewer/id review) (:reviewer/id verdict))
                   (= (:claim/id subject) (:claim/id review))
                   (= (:artifact/ref subject) (:artifact/ref review))
                   (= (:trace/id subject) (:producer-trace/id review))
                   (= (:review-trace/id verdict) (:review-trace/id review)))
      (refuse! :e3/review-join-mismatch "Review provenance does not join exact subject" {}))
    (when-not (and (nonblank? (:authority/ref review))
                   (before? (:completed-at review) (:authorization-at pending)))
      (refuse! :e3/review-not-pre-enact "Review is missing, unowned, or not before authorization" {}))
    {:schema/version schema-version :scope :isolated-test
     :decision :mechanism-authorized :phase :pre-enact
     :identity (select-keys pending ids) :subject subject
     :verdict :independent :reviewer/id (:reviewer/id review)
     :sources (mapv #(dissoc % :record) resolved)
     :external-dependencies
     {:production-authority :unavailable
      :r6-scoring-and-posterior :required
      :runtime-consumer :before-enact}}))
