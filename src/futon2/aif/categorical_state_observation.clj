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
(def evidence-claim-schema :wm/categorical-state-evidence-claim-v1)
(def acceptance-subject-schema :wm/categorical-state-acceptance-subject-v1)

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

(def supported-assertions (set (mapcat identity (vals rubric))))

(defn- refuse! [reason path & [data]]
  (throw (ex-info (str "Categorical state observation refused: " (name reason))
                  (merge {:refusal reason :path path} data))))

(defn- demand! [pred reason path & [data]]
  (when-not pred (refuse! reason path data)))

(defn- nonblank? [x]
  (and (or (string? x) (keyword? x) (symbol? x))
       (not (str/blank? (name x)))))

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

(defn acceptance-subject
  "Complete immutable subject an independent reviewer accepts. Resolved source
  pointers and records are included; changing bytes behind a stable reference,
  context, observer origin, scope, provenance, or limitations changes this value."
  [observation resolved-claims observer observer-source expected]
  [acceptance-subject-schema
   (subject observation)
   resolved-claims
   {:ref (get-in observation [:authority :observer/ref])
    :source observer-source
    :record observer}
   expected])

(defn acceptance-subject-digest [frozen-subject]
  (sha256-bytes (.getBytes (pr-str frozen-subject) StandardCharsets/UTF_8)))

(defn- classify-rubric! [observation assertions]
  (let [declared (get-in observation [:categorical-status :value])
        assertions (set assertions)
        matches (->> rubric
                     (keep (fn [[status required]]
                             (when (every? assertions required) status)))
                     set)]
    (demand! (= rubric-id (get-in observation [:rubric :id]))
             :rubric-version-mismatch [:rubric :id])
    (demand! (seq assertions) :insufficient-categorical-evidence [:evidence])
    (demand! (every? supported-assertions assertions) :unsupported-evidence-assertion
             [:evidence] {:assertions assertions})
    (demand! (seq matches) :insufficient-categorical-evidence [:evidence])
    (demand! (= 1 (count matches)) :ambiguous-categorical-evidence
             [:rubric :assertions] {:matching-statuses matches})
    (demand! (= declared (first matches)) :rubric-state-mismatch
             [:categorical-status :value] {:rubric-status (first matches)})))

(defn- validate-time! [observation expected]
  (let [p (:point observation)
        action-start (instant! (:action/started-at p) [:point :action/started-at])
        action-end (instant! (:action/completed-at p) [:point :action/completed-at])
        cutoff (instant! (:evidence/cutoff-at p) [:point :evidence/cutoff-at])
        disposition-at (instant! (:disposition/recorded-at p) [:point :disposition/recorded-at])
        annotation-at (instant! (:annotation/created-at p) [:point :annotation/created-at])]
    (doseq [k [:action/started-at :action/completed-at :evidence/cutoff-at
               :disposition/recorded-at]]
      (demand! (= (get-in expected [:point k]) (get p k))
               :observation-time-mismatch [:point k]))
    (demand! (not (.isAfter action-start action-end)) :temporal-order-invalid [:point])
    (demand! (not (.isAfter action-end cutoff)) :temporal-order-invalid [:point])
    (demand! (.isBefore cutoff disposition-at) :temporal-outcome-leakage [:point])
    (demand! (not (.isBefore annotation-at cutoff)) :temporal-order-invalid [:point])
    {:annotation-at annotation-at :cutoff cutoff}))

(defn- forbidden-payload? [x]
  (cond
    (map? x) (or (some forbidden-evidence-kinds (keys x))
                 (some forbidden-payload? (vals x)))
    (coll? x) (some forbidden-payload? x)
    :else (forbidden-evidence-kinds x)))

(declare authority-record!)

(defn- validate-evidence! [observation expected resolver io-opts]
  (let [items (get-in observation [:evidence :claims])
        expected-point (:point expected)
        expected-entity (get-in expected [:subject :entity/id])
        cutoff (instant! (:evidence/cutoff-at expected-point) [:expected :point :evidence/cutoff-at])]
    (demand! (and (vector? items) (seq items)) :insufficient-categorical-evidence
             [:evidence :claims])
    (mapv
     (fn [idx item]
       (let [ref (:claim/ref item)
             claim (authority-record! resolver :evidence ref io-opts)
             cp (:point claim)]
         (demand! (= evidence-claim-schema (:schema claim)) :malformed-evidence-claim
                  [:evidence :claims idx])
         (demand! (nonblank? (:claim/id claim)) :malformed-evidence-claim
                  [:evidence :claims idx :claim/id])
         (demand! (= :categorical-status-evidence (:claim/type claim))
                  :forbidden-evidence-source [:evidence :claims idx :claim/type])
         (demand! (= (:authority/scope expected) (:authority/scope claim))
                  :evidence-scope-mismatch [:evidence :claims idx :authority/scope])
         (demand! (= (:authority/provenance expected) (:authority/provenance claim))
                  :evidence-provenance-mismatch
                  [:evidence :claims idx :authority/provenance])
         (demand! (= expected-entity (get-in claim [:subject :entity/id]))
                  :evidence-entity-mismatch [:evidence :claims idx :subject])
         (doseq [k [:run/id :cohort/id :attempt/id :checkpoint/ref]]
           (demand! (= (get expected-point k) (get cp k))
                    :evidence-point-mismatch [:evidence :claims idx :point k]))
         (demand! (not (.isAfter (instant! (:observed-at cp)
                                           [:evidence :claims idx :point :observed-at]) cutoff))
                  :evidence-after-cutoff [:evidence :claims idx :point :observed-at])
         (demand! (not (forbidden-payload? (:payload claim)))
                  :forbidden-evidence-source [:evidence :claims idx :payload])
         (demand! (supported-assertions (:assertion claim))
                  :unsupported-evidence-assertion [:evidence :claims idx :assertion])
         {:ref ref :claim/id (:claim/id claim) :assertion (:assertion claim)
          :observed-at (:observed-at cp) :source (::source (meta claim))
          :claim claim}))
     (range) items)))

(defn- authority-record! [resolver kind ref io-opts]
  (demand! (fn? resolver) :authority-resolver-missing [:authority])
  (let [resolved (resolver kind ref)]
    (demand! (map? resolved) :authority-not-found [:authority kind])
    (with-meta (read-pinned-form! resolved io-opts) {::source resolved})))

(defn validate-observation!
  "Return a qualified envelope or throw ex-info with :refusal.

  `authority` is fixed outside the candidate and supplies :resolver plus
  optional I/O injection for tests. The resolver must independently map
  observer/review references to authorized origins and a pinned review record."
  [observation {:keys [resolver io-opts expected]}]
  (demand! (= schema (:schema observation)) :unsupported-schema [:schema])
  (demand! (not (contains? observation :review)) :candidate-owned-review [:review])
  (demand! (not (contains? (:rubric observation) :assertions))
           :candidate-owned-rubric-assertions [:rubric :assertions])
  (demand! (nonblank? (:observation/id observation)) :missing-identity [:observation/id])
  (demand! (= :post-action-pre-disposition-at-close (get-in observation [:point :state-point]))
           :wrong-conditioning-point [:point :state-point])
  (doseq [k [:run/id :cohort/id :attempt/id :checkpoint/ref]]
    (demand! (nonblank? (get-in observation [:point k])) :missing-identity [:point k]))
  (demand! (nonblank? (get-in observation [:subject :entity/id]))
           :missing-identity [:subject :entity/id])
  (demand! (map? expected) :expected-context-missing [:expected])
  (demand! (#{:test :production} (:authority/scope expected))
           :authority-scope-missing [:expected :authority/scope])
  (demand! (and (nonblank? (get-in expected [:authority/provenance :config/id]))
                (nonblank? (get-in expected [:authority/provenance :revision])))
           :authority-provenance-missing [:expected :authority/provenance])
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
  (let [{:keys [annotation-at cutoff]} (validate-time! observation expected)
        limitations (:limitations observation)
        retrospective? (.isAfter annotation-at cutoff)]
    (demand! (map? limitations) :limitations-missing [:limitations])
    (doseq [k [:retrospective? :missingness :selection :method :rubric]]
      (demand! (contains? limitations k) :limitations-missing [:limitations k]))
    (demand! (= retrospective? (:retrospective? limitations))
             :retrospective-flag-mismatch [:limitations :retrospective?])
    (demand! (= :reviewed-categorical-annotation (:method limitations))
             :limitations-method-mismatch [:limitations :method])
    (demand! (= rubric-id (:rubric limitations))
             :limitations-rubric-mismatch [:limitations :rubric]))
  (let [resolved-claims (validate-evidence! observation expected resolver io-opts)
        assertions (mapv :assertion resolved-claims)
        _ (classify-rubric! observation assertions)
        observer-ref (get-in observation [:authority :observer/ref])
        review-ref (get-in observation [:authority :review/ref])
        observer (authority-record! resolver :observer observer-ref io-opts)
        observer-source (::source (meta observer))]
    (demand! (= authority-schema (:schema observer)) :observer-unauthorized [:authority :observer/ref])
    (demand! (= :categorical-state-observer (:role observer))
             :observer-unauthorized [:authority :observer/ref])
    (demand! (nonblank? (:principal/id observer)) :observer-unauthorized [:authority :observer/ref])
    (demand! (= (:authority/scope expected) (:authority/scope observer))
             :observer-unauthorized [:authority :observer/ref])
    (demand! (= (:authority/provenance expected) (:authority/provenance observer))
             :observer-unauthorized [:authority :observer/ref])
    (let [frozen-subject (acceptance-subject observation resolved-claims
                                             observer observer-source expected)
          digest (acceptance-subject-digest frozen-subject)
          review (authority-record! resolver :review review-ref io-opts)]
      (demand! (= review-schema (:schema review)) :review-unauthorized [:authority :review/ref])
      (demand! (= :categorical-state-reviewer (:role review))
               :review-unauthorized [:authority :review/ref])
      (demand! (nonblank? (:reviewer/id review)) :review-unauthorized [:authority :review/ref])
      (demand! (= (:authority/scope expected) (:authority/scope review))
               :review-unauthorized [:authority :review/ref])
      (demand! (= (:authority/provenance expected) (:authority/provenance review))
               :review-unauthorized [:authority :review/ref])
      (demand! (not= (:principal/id observer) (:reviewer/id review))
               :self-review [:authority :review/ref])
      (demand! (= :accepted (:verdict review)) :categorical-observation-unreviewed
               [:authority :review/ref])
      (demand! (= frozen-subject (:subject review)) :review-subject-mismatch
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
       :subject frozen-subject
       :subject/sha256 digest
       :observer-origin observer-ref
       :review-origin review-ref
       :observer-source observer-source
       :review-source (::source (meta review))
       :resolved-evidence-claims resolved-claims
       :derived-rubric-assertions assertions
       :method :reviewed-categorical-annotation
       :cohort (get-in observation [:point :cohort/id])
       :authority/scope (:authority/scope expected)
       :authority/provenance (:authority/provenance expected)
       :conditioning-point expected
       :limitations (:limitations observation)})))

(defn validate-observations!
  "Validate observations and refuse disagreeing labels at one exact point."
  [observations authority]
  (let [validated (mapv #(validate-observation! % authority) observations)
        groups (group-by #(select-keys (:conditioning-point %)
                                      [:subject :point :authority/scope])
                         validated)]
    (doseq [[point xs] groups]
      (when (< 1 (count (set (map #(get-in % [:observation :categorical-status :value]) xs))))
        (refuse! :categorical-observation-conflict [:observations] {:point point})))
    validated))
