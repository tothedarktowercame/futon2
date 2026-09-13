(ns futon2.aif.categorical-state-observation
  "Pure qualification boundary for independently adjudicated categorical
  observations used by a future measured machine A.

  This module does not attach records to closes, count observations, or estimate
  a kernel.  Observer/reviewer authority comes only from the caller's external
  resolver.  Candidate-owned identity or acceptance fields have no authority."
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [futon2.aif.belief :as belief])
  (:import (java.io PushbackReader StringReader)
           (java.nio ByteBuffer)
           (java.nio.charset CodingErrorAction StandardCharsets)
           (java.nio.file Files Path)
           (java.security MessageDigest)
           (java.time Instant)))

(def schema :wm/categorical-state-observation-v1)
(def rubric-id :wm/categorical-state-rubric-v1)
(def authority-schema :wm/categorical-state-authority-v1)
(def review-schema :wm/categorical-state-review-v1)

(def rubric
  "Evidence assertions required by the existing seven lifecycle semantics.
  Reopened requires both prior terminal standing and an explicit reopening; an
  interest `state/reopened` event or projected `:live` is not either assertion."
  {:spawned #{:entity-newly-created}
   :refined #{:framing-sharpened}
   :strengthened #{:support-gained}
   :addressed #{:resolved-by-evidence}
   :falsified #{:contradicted-by-evidence}
   :foreclosed #{:deliberately-closed-off}
   :reopened #{:previous-terminal-standing :standing-reopened}})

(def forbidden-evidence-kinds
  #{:target-disposition :model-posterior :posterior-argmax
    :interest-event-type :interest-projected-standing})

(defn- refuse! [reason path & [data]]
  (throw (ex-info (str "Categorical state observation refused: " (name reason))
                  (merge {:refusal reason :path path} data))))

(defn- demand! [pred reason path & [data]]
  (when-not pred (refuse! reason path data)))

(defn sha256-bytes [bytes]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256") bytes)]
    (apply str (map #(format "%02x" (bit-and 255 %)) digest))))

(defn- strict-utf8 [bytes path]
  (try
    (let [decoder (doto (.newDecoder StandardCharsets/UTF_8)
                    (.onMalformedInput CodingErrorAction/REPORT)
                    (.onUnmappableCharacter CodingErrorAction/REPORT))]
      (str (.decode decoder (ByteBuffer/wrap bytes))))
    (catch Exception _ (refuse! :invalid-utf8 path))))

(defn- one-form [bytes path]
  (try
    (with-open [reader (PushbackReader. (StringReader. (strict-utf8 bytes path)))]
      (let [form (edn/read {:eof ::eof} reader)]
        (demand! (not= ::eof form) :empty-source path)
        (demand! (= ::eof (edn/read {:eof ::eof} reader)) :multiple-forms path)
        form))
    (catch clojure.lang.ExceptionInfo e (throw e))
    (catch Exception _ (refuse! :malformed-source path))))

(defn read-pinned-form!
  "Read, digest and parse the same first byte buffer, then re-read to detect a
  mutation during validation. `read-bytes` is injectable solely for isolated
  controls; production callers use Files/readAllBytes."
  ([pointer] (read-pinned-form! pointer {}))
  ([{:keys [path sha256]} {:keys [read-bytes]}]
   (demand! (and (string? path) (not (str/blank? path))
                 (string? sha256) (re-matches #"[0-9a-f]{64}" sha256))
            :missing-source-pointer [:source])
   (let [read! (or read-bytes #(Files/readAllBytes (Path/of ^String % (make-array String 0))))
         first-bytes (try (read! path)
                          (catch Exception _ (refuse! :missing-source [:source :path])))
         first-digest (sha256-bytes first-bytes)]
     (demand! (= sha256 first-digest) :source-digest-mismatch [:source :sha256])
     (let [form (one-form first-bytes [:source :path])
           second-bytes (try (read! path)
                             (catch Exception _ (refuse! :source-mutated [:source :path])))
           second-digest (sha256-bytes second-bytes)]
       (demand! (= first-digest second-digest) :source-mutated [:source :path])
       form))))

(defn- instant! [x path]
  (try (Instant/parse x)
       (catch Exception _ (refuse! :invalid-timestamp path))))

(defn subject
  "The exact review subject. Vector field order makes its byte encoding stable."
  [observation]
  [schema
   (:observation/id observation)
   (:subject observation)
   (:point observation)
   (:categorical-status observation)
   (:observation/method observation)
   (:rubric observation)
   (:evidence observation)
   (:limitations observation)])

(defn subject-digest [observation]
  (sha256-bytes (.getBytes (pr-str (subject observation)) StandardCharsets/UTF_8)))

(defn- classify-rubric! [observation]
  (let [declared (get-in observation [:categorical-status :value])
        assertions (set (get-in observation [:rubric :assertions]))
        matches (->> rubric
                     (keep (fn [[status required]]
                             (when (every? assertions required) status)))
                     set)]
    (demand! (= rubric-id (get-in observation [:rubric :id]))
             :rubric-version-mismatch [:rubric :id])
    (demand! (seq assertions) :insufficient-categorical-evidence [:rubric :assertions])
    (demand! (seq matches) :insufficient-categorical-evidence [:rubric :assertions])
    (demand! (= 1 (count matches)) :ambiguous-categorical-evidence
             [:rubric :assertions] {:matching-statuses matches})
    (demand! (= declared (first matches)) :rubric-state-mismatch
             [:categorical-status :value] {:rubric-status (first matches)})))

(defn- validate-time! [observation]
  (let [p (:point observation)
        action-start (instant! (:action/started-at p) [:point :action/started-at])
        action-end (instant! (:action/completed-at p) [:point :action/completed-at])
        cutoff (instant! (:evidence/cutoff-at p) [:point :evidence/cutoff-at])
        disposition-at (instant! (:disposition/recorded-at p) [:point :disposition/recorded-at])
        annotation-at (instant! (:annotation/created-at p) [:point :annotation/created-at])]
    (demand! (not (.isAfter action-start action-end)) :temporal-order-invalid [:point])
    (demand! (not (.isAfter action-end cutoff)) :temporal-order-invalid [:point])
    (demand! (.isBefore cutoff disposition-at) :temporal-outcome-leakage [:point])
    (demand! (not (.isBefore annotation-at cutoff)) :temporal-order-invalid [:point])
    (doseq [[idx evidence] (map-indexed vector (get-in observation [:evidence :items]))]
      (demand! (not (forbidden-evidence-kinds (:kind evidence)))
               :forbidden-evidence-source [:evidence :items idx :kind])
      (demand! (not (.isAfter (instant! (:observed-at evidence)
                                        [:evidence :items idx :observed-at]) cutoff))
               :evidence-after-cutoff [:evidence :items idx :observed-at]))))

(defn- validate-evidence! [observation io-opts]
  (let [items (get-in observation [:evidence :items])]
    (demand! (and (vector? items) (seq items)) :insufficient-categorical-evidence
             [:evidence :items])
    (doseq [[idx item] (map-indexed vector items)]
      (demand! (keyword? (:kind item)) :malformed-evidence [:evidence :items idx])
      (read-pinned-form! (:source item) io-opts))))

(defn- authority-record! [resolver kind ref io-opts]
  (demand! (fn? resolver) :authority-resolver-missing [:authority])
  (let [resolved (resolver kind ref)]
    (demand! (map? resolved) :authority-not-found [:authority kind])
    (read-pinned-form! resolved io-opts)))

(defn validate-observation!
  "Return a qualified envelope or throw ex-info with :refusal.

  `authority` is fixed outside the candidate and supplies :resolver plus
  optional I/O injection for tests. The resolver must independently map
  observer/review references to authorized origins and a pinned review record."
  [observation {:keys [resolver io-opts expected]}]
  (demand! (= schema (:schema observation)) :unsupported-schema [:schema])
  (demand! (not (contains? observation :review)) :candidate-owned-review [:review])
  (demand! (= :post-action-pre-disposition-at-close (get-in observation [:point :state-point]))
           :wrong-conditioning-point [:point :state-point])
  (doseq [k [:run/id :cohort/id :attempt/id :checkpoint/ref]]
    (demand! (some? (get-in observation [:point k])) :missing-identity [:point k]))
  (demand! (some? (get-in observation [:subject :entity/id]))
           :missing-identity [:subject :entity/id])
  (demand! (map? expected) :expected-context-missing [:expected])
  (demand! (= (get-in expected [:subject :entity/id])
              (get-in observation [:subject :entity/id]))
           :observation-entity-mismatch [:subject :entity/id])
  (doseq [k [:run/id :cohort/id :attempt/id :checkpoint/ref]]
    (demand! (= (get-in expected [:point k]) (get-in observation [:point k]))
             :observation-identity-mismatch [:point k]))
  (let [status (get-in observation [:categorical-status :value])]
    (demand! (= :wm/status-v1 (get-in observation [:categorical-status :domain]))
             :state-domain-mismatch [:categorical-status :domain])
    (demand! (belief/status-set status) :state-domain-mismatch
             [:categorical-status :value])
    (demand! (= :reviewed-categorical-annotation (:observation/method observation))
             :derived-state-not-observation [:observation/method]))
  (classify-rubric! observation)
  (validate-time! observation)
  (validate-evidence! observation io-opts)
  (let [observer-ref (get-in observation [:authority :observer/ref])
        review-ref (get-in observation [:authority :review/ref])
        observer (authority-record! resolver :observer observer-ref io-opts)
        review (authority-record! resolver :review review-ref io-opts)
        digest (subject-digest observation)]
    (demand! (= authority-schema (:schema observer)) :observer-unauthorized [:authority :observer/ref])
    (demand! (= :categorical-state-observer (:role observer))
             :observer-unauthorized [:authority :observer/ref])
    (demand! (= review-schema (:schema review)) :review-unauthorized [:authority :review/ref])
    (demand! (= :categorical-state-reviewer (:role review))
             :review-unauthorized [:authority :review/ref])
    (demand! (not= (:principal/id observer) (:reviewer/id review))
             :self-review [:authority :review/ref])
    (demand! (= :accepted (:verdict review)) :categorical-observation-unreviewed
             [:authority :review/ref])
    (demand! (= (subject observation) (:subject review)) :review-subject-mismatch
             [:authority :review/ref])
    (demand! (= digest (:subject/sha256 review)) :review-subject-mismatch
             [:authority :review/ref])
    (demand! (= (:principal/id observer) (:observer/id review))
             :borrowed-review [:authority :review/ref])
    (demand! (= rubric-id (:rubric/id review)) :review-subject-mismatch
             [:authority :review/ref])
    (demand! (not (.isBefore (instant! (:reviewed-at review) [:reviewed-at])
                             (instant! (get-in observation [:point :annotation/created-at])
                                       [:point :annotation/created-at])))
             :temporal-order-invalid [:reviewed-at])
    {:schema :wm/validated-categorical-state-observation-v1
     :status :qualified
     :observation observation
     :subject/sha256 digest
     :observer-origin observer-ref
     :review-origin review-ref
     :method :reviewed-categorical-annotation
     :cohort (get-in observation [:point :cohort/id])
     :limitations (:limitations observation)}))

(defn validate-observations!
  "Validate observations and refuse disagreeing labels at one exact point."
  [observations authority]
  (let [validated (mapv #(validate-observation! % authority) observations)
        groups (group-by #(vector (get-in % [:observation :subject :entity/id])
                                  (select-keys (get-in % [:observation :point])
                                               [:run/id :cohort/id :attempt/id :checkpoint/ref
                                                :evidence/cutoff-at]))
                         validated)]
    (doseq [[point xs] groups]
      (when (< 1 (count (set (map #(get-in % [:observation :categorical-status :value]) xs))))
        (refuse! :categorical-observation-conflict [:observations] {:point point})))
    validated))
