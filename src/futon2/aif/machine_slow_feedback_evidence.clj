(ns futon2.aif.machine-slow-feedback-evidence
  "Pure E6b verifier. It replays one pinned temporal feedback update and checks
   independently supplied complete application evidence. It neither persists
   state nor enforces exactly-once storage."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.machine-enactment-correspondence :as e2b]
            [futon2.aif.machine-pre-enact-authorization :as e3]
            [futon2.aif.temporal-hierarchy :as hierarchy])
  (:import [java.io PushbackReader StringReader]
           [java.nio ByteBuffer]
           [java.nio.charset CodingErrorAction StandardCharsets]
           [java.nio.file Files LinkOption Path]
           [java.security MessageDigest]))

(def schema-version :wm/r16-r15-feedback-evidence-v1)
(def source-order [:context :prior-state :e2b-subject :lifecycle-relation :outcome
                   :outcome-review :outcome-review-artifact :next-state
                   :application-ledger :application-universe])
(def ^:private schemas
  {:context :wm/e6b-transition-context-v1
   :prior-state :wm/e6b-prior-slow-state-v1
   :e2b-subject :wm/e6b-e2b-subject-v1
   :lifecycle-relation :wm/e6b-lifecycle-relation-v1
   :outcome :wm/e6b-outcome-authority-v1
   :outcome-review :wm/e6b-outcome-review-v1
   :outcome-review-artifact :wm/e6b-outcome-review-artifact-v1
   :next-state :wm/e6b-next-slow-state-v1
   :application-ledger :wm/e6b-application-ledger-v1
   :application-universe :wm/e6b-application-universe-v1})

(defn- refuse! [kind message data]
  (throw (ex-info message (assoc data :refusal kind))))
(defn- nonblank? [x] (and (string? x) (not (str/blank? x))))
(defn- hex [bytes] (apply str (map #(format "%02x" (bit-and 0xff %)) bytes)))
(defn- digest [bytes]
  (hex (.digest (doto (MessageDigest/getInstance "SHA-256") (.update bytes)))))
(defn- value-digest [x] (digest (.getBytes (pr-str x) StandardCharsets/UTF_8)))

(defn- strict-form! [bytes path]
  (try
    (let [decoder (doto (.newDecoder StandardCharsets/UTF_8)
                    (.onMalformedInput CodingErrorAction/REPORT)
                    (.onUnmappableCharacter CodingErrorAction/REPORT))
          rdr (PushbackReader. (StringReader. (str (.decode decoder (ByteBuffer/wrap bytes)))))
          eof (Object.) form (edn/read {:eof eof} rdr) tail (edn/read {:eof eof} rdr)]
      (when (identical? eof form) (refuse! :e6b/source-empty "Empty source" {:path (str path)}))
      (when-not (identical? eof tail)
        (refuse! :e6b/source-trailing-form "Trailing EDN form" {:path (str path)}))
      form)
    (catch clojure.lang.ExceptionInfo e (throw e))
    (catch Throwable e
      (refuse! :e6b/source-malformed "Malformed strict UTF-8 EDN"
               {:path (str path) :cause (.getName (class e))}))))

(defn- resolve! [root label pin]
  (let [{:keys [relative-path sha256]} pin
        base (.normalize (.toAbsolutePath (.toPath (io/file root))))
        rel (Path/of (str relative-path) (make-array String 0))
        path (.normalize (.toAbsolutePath (.resolve base rel)))]
    (when-not (and (string? sha256) (re-matches #"[0-9a-f]{64}" sha256))
      (refuse! :e6b/source-pin-missing "Source pin missing" {:label label}))
    (when (or (.isAbsolute rel) (not (.startsWith path base)))
      (refuse! :e6b/source-path-escape "Source path escapes configured root" {:label label}))
    (when-not (Files/isRegularFile path (into-array LinkOption [LinkOption/NOFOLLOW_LINKS]))
      (refuse! :e6b/source-unreadable "Source is not a regular file" {:label label}))
    (let [bytes (Files/readAllBytes path) actual (digest bytes)
          record (strict-form! bytes path)]
      (when-not (= sha256 actual)
        (refuse! :e6b/source-pin-mismatch "Source bytes changed" {:label label}))
      (when-not (= (schemas label) (:schema/version record))
        (refuse! :e6b/source-schema-mismatch "Source schema differs" {:label label}))
      {:label label :sha256 actual :record record})))

(defn- finite-positive? [x]
  (and (number? x) (Double/isFinite (double x)) (pos? (double x))))
(defn- instant [x]
  (try (java.time.Instant/parse x) (catch Throwable _ nil)))
(defn- valid-entry? [x]
  (and (= #{:alpha :beta :intrinsic-value :n-emissions :n-followthrough :as-of}
          (set (keys x)))
       (finite-positive? (:alpha x)) (finite-positive? (:beta x))
       (number? (:intrinsic-value x)) (Double/isFinite (double (:intrinsic-value x)))
       (nat-int? (:n-emissions x)) (nat-int? (:n-followthrough x))
       (some? (instant (:as-of x)))))
(defn- identity-of [x]
  (select-keys x [:model/id :model/revision :run/id :tick/index]))
(defn- valid-identity? [x]
  (and (or (keyword? (:model/id x)) (nonblank? (:model/id x)))
       (nonblank? (:model/revision x)) (nonblank? (:run/id x))
       (nat-int? (:tick/index x))))

(defn verify-feedback
  [{:keys [mode evidence-root sources canonical]}]
  (when-not (contains? #{:isolated-test :production} mode)
    (refuse! :e6b/mode-unknown "Unknown verifier mode" {:mode mode}))
  (when (= :production mode)
    (refuse! :e6b/production-authority-unavailable
             "Independent production feedback authority is unavailable" {}))
  (when-not (= (set source-order) (set (keys sources)))
    (refuse! :e6b/source-set-incomplete "Every E6b source is required" {}))
  (let [resolved (mapv #(resolve! evidence-root % (sources %)) source-order)
        records (into {} (map (juxt :label :record) resolved))
        context (:context records) prior (:prior-state records) e2b (:e2b-subject records)
        relation (:lifecycle-relation records)
        outcome (:outcome records) outcome-review (:outcome-review records)
        review-artifact (:outcome-review-artifact records) claimed (:next-state records)
        ledger (:application-ledger records) universe (:application-universe records)
        source-pins (into {} (map (juxt :label :sha256)) resolved)
        source-tick (:tick/index context) destination (:destination/tick-index context)
        cls (:fast/action-class outcome) occurrence (:candidate/occurrence-id context)
        application-id (:application/id context)
        common (identity-of context)
        canonical-e3 (try (e3/verify-pre-enact (:e3 canonical))
                          (catch Throwable x
                            (refuse! :e6b/canonical-e3-unavailable
                                     "Canonical E3 evidence did not resolve" {:cause (.getMessage x)})))
        canonical-e2b (try (e2b/verify-correspondence (:e2b canonical))
                           (catch Throwable x
                             (refuse! :e6b/canonical-e2b-unavailable
                                      "Canonical E2b evidence did not resolve" {:cause (.getMessage x)})))]
    (when-not (and (= :isolated-test (:scope context)) (valid-identity? context)
                   (= (inc source-tick) destination)
                   (every? nonblank? ((juxt :prior-state/revision :next-state/revision
                                            :feedback/event-id :application/id) context))
                   (some? occurrence) (map? (:action context)) (seq (:action context))
                   (keyword? (:fast/action-class context))
                   (instant (:destination/as-of context))
                   (map? (:canonical/e3-context context))
                   (map? (:canonical/e2b-context context))
                   (map? (:canonical/field-subject context))
                   (nonblank? (:outcome-reviewer/id context))
                   (nonblank? (:outcome-observer/id context)))
      (refuse! :e6b/transition-context-invalid "Fixed transition context is incomplete" {}))
    (doseq [[label record] records]
      (when-not (= :isolated-test (:scope record))
        (refuse! :e6b/scope-mismatch "All resolved sources must retain isolated scope"
                 {:label label :scope (:scope record)})))
    (when-not (and (= (select-keys common [:model/id :model/revision :run/id :tick/index])
                      (select-keys (:identity canonical-e3)
                                   [:model/id :model/revision :run/id :tick/index]))
                   (= (select-keys common [:model/id :model/revision :run/id :tick/index])
                      (:identity canonical-e2b)))
      (refuse! :e6b/canonical-context-mismatch
               "Transition identity differs from canonical E3/E2b" {}))
    (when-not (and (= common (identity-of prior))
                   (= (:prior-state/revision context) (:state/revision prior))
                   (map? (:slow/intrinsics prior)) (seq (:slow/intrinsics prior))
                   (every? keyword? (keys (:slow/intrinsics prior)))
                   (every? valid-entry? (vals (:slow/intrinsics prior)))
                   (contains? (:slow/intrinsics prior) cls))
      (refuse! :e6b/prior-state-incomplete-or-stale
               "Prior state must be complete and contain the outcome class" {}))
    (let [e3-context (select-keys (:identity canonical-e3)
                                  [:model/id :model/revision :run/id :cohort/id :tick/index :event/id])
          e2b-context (merge (:identity canonical-e2b)
                             (select-keys canonical-e2b [:cohort/id :event/id]))
          field-subject {:e3/field-pins (get-in canonical-e3 [:subject :field-pins])
                         :e2b/e1-source-pins (get-in canonical-e2b [:subject :e1-source-pins])
                         :e2b/approved-domain (get-in canonical-e2b [:subject :approved-domain])}
          relation-subject {:e3/context e3-context :e2b/context e2b-context
                            :field/subject field-subject
                            :e3/digest (value-digest canonical-e3)
                            :e2b/digest (value-digest canonical-e2b)
                            :candidate/occurrence-id occurrence
                            :action (:action context)
                            :fast/action-class (:fast/action-class context)}]
    (when-not (and (= (:canonical/e3-context context) e3-context)
                   (= (:canonical/e2b-context context) e2b-context)
                   (= (:canonical/field-subject context) field-subject)
                   (= (select-keys common [:model/id :model/revision :run/id :tick/index])
                      (select-keys e3-context [:model/id :model/revision :run/id :tick/index]))
                   (= (select-keys common [:model/id :model/revision :run/id :tick/index])
                      (:identity canonical-e2b))
                   (= common (identity-of e2b))
                   (= occurrence (:candidate/occurrence-id e2b))
                   (= (:action context) (:action e2b))
                   (= (:fast/action-class context) (:fast/action-class e2b))
                   (= (:selected canonical-e2b) (:enacted canonical-e2b))
                   (= {:candidate/id occurrence :action (:action context)} (:enacted canonical-e2b))
                   (= :mechanism-authorized (:decision canonical-e3))
                   (= occurrence (get-in canonical-e3 [:subject :candidate/occurrence-id]))
                   (= (:action context) (get-in canonical-e3 [:subject :action]))
                   (= (value-digest canonical-e2b) (:canonical/e2b-digest e2b))
                   (= (value-digest canonical-e3) (:canonical/e3-digest e2b))
                   (= :e3-authorizes-e2b-enactment (:relation/type relation))
                   (= relation-subject (:subject relation))
                   (nonblank? (:relation/id relation))
                   (some? (instant (:enactment/at relation))))
      (refuse! :e6b/canonical-context-mismatch
               "Canonical E3/E2b context or ordered field subject differs" {})))
    (when-not (and (= common (identity-of outcome))
                   (= occurrence (:candidate/occurrence-id outcome))
                   (= (:action context) (:action outcome))
                   (= (:fast/action-class context) cls)
                   (keyword? cls) (contains? #{:succeeded :failed} (:terminal/status outcome))
                   (true? (:fast/witnessed? outcome))
                   (instance? Boolean (:fast/succeeded? outcome))
                   (= (= :succeeded (:terminal/status outcome)) (:fast/succeeded? outcome))
                   (nonblank? (:outcome/evidence-id outcome))
                   (nonblank? (:outcome/authority-ref outcome))
                   (not= (:outcome/producer-id outcome) (:outcome/reviewer-id outcome))
                   (every? nonblank? ((juxt :outcome/producer-id :outcome/reviewer-id) outcome)))
      (refuse! :e6b/outcome-authority-invalid
               "A boolean terminal independently witnessed outcome is required" {}))
    (let [outcome-subject (select-keys outcome
                                       [:model/id :model/revision :run/id :tick/index
                                        :candidate/occurrence-id :action :fast/action-class
                                        :terminal/status :terminal/at :fast/witnessed? :fast/succeeded?
                                        :outcome/evidence-id :outcome/producer-id])
          prior-times (mapv (comp instant :as-of val) (:slow/intrinsics prior))
          prior-at (last (sort prior-times))
          enactment-at (instant (:enactment/at relation))
          terminal-at (instant (:terminal/at outcome))
          reviewed-at (instant (:reviewed-at outcome-review))
          destination-at (instant (:destination/as-of context))]
      (when-not (and terminal-at reviewed-at destination-at prior-at enactment-at
                     (= prior-at (instant (:prior-state/as-of context)))
                     (.isBefore prior-at enactment-at)
                     (or (= enactment-at terminal-at) (.isBefore enactment-at terminal-at))
                     (or (= terminal-at reviewed-at) (.isBefore terminal-at reviewed-at))
                     (.isBefore reviewed-at destination-at)
                     (= (:outcome-reviewer/id context) (:reviewer/id outcome-review))
                     (= (:outcome-observer/id context) (:observer/id outcome-review))
                     (= {:observer/id (:observer/id outcome-review)
                         :observer/origin (:observer/origin relation)
                         :observer/authority-ref (:observer/authority-ref relation)
                         :outcome/subject outcome-subject}
                        (:observer/subject relation))
                     (keyword? (:observer/origin relation))
                     (nonblank? (:observer/authority-ref relation))
                     (not= (:reviewer/id outcome-review) (:observer/id outcome-review))
                     (= :accepted (:review/outcome outcome-review))
                     (= outcome-subject (:subject outcome-review))
                     (= (:outcome/reviewer-id outcome) (:reviewer/id outcome-review))
                     (= (:outcome/authority-ref outcome) (:review/id outcome-review))
                     (= (:outcome-review-artifact source-pins)
                        (:review/artifact-sha256 outcome-review))
                     (= {:subject outcome-subject :review/id (:review/id outcome-review)
                         :reviewer/id (:reviewer/id outcome-review)
                         :observer/id (:observer/id outcome-review)
                         :reviewed-at (:reviewed-at outcome-review)
                         :executed? true}
                        (select-keys review-artifact
                                     [:subject :review/id :reviewer/id :observer/id
                                      :reviewed-at :executed?])))
        (refuse! :e6b/outcome-review-unresolved
                 "Outcome review does not bind the exact outcome subject" {})))
    (let [production-outcome (select-keys outcome [:fast/action-class :fast/witnessed?
                                                    :fast/succeeded?])
          actual-state (hierarchy/advance-slow-state
                        (select-keys prior [:slow/mode :slow/intrinsics]) production-outcome
                        {:as-of (:destination/as-of context) :run-id (:run/id context)
                         :evidence-ref (:outcome/evidence-id outcome)})
          expected-next {:schema/version :wm/e6b-next-slow-state-v1 :scope :isolated-test
                         :model/id (:model/id context) :model/revision (:model/revision context)
                         :run/id (:run/id context) :tick/index destination
                         :state/revision (:next-state/revision context)
                         :predecessor/revision (:prior-state/revision context)
                         :feedback/event-id (:feedback/event-id context)
                         :state actual-state}
          input-subject {:context (value-digest context) :prior (value-digest prior)
                         :e2b (value-digest e2b) :outcome (value-digest outcome)}
          transition-subject {:model/id (:model/id context) :model/revision (:model/revision context)
                              :run/id (:run/id context) :source/tick-index source-tick
                              :destination/tick-index destination
                              :candidate/occurrence-id occurrence :action (:action context)
                              :fast/action-class cls :prior-state/revision (:prior-state/revision context)
                              :next-state/revision (:next-state/revision context)
                              :feedback/event-id (:feedback/event-id context)}
          expected-entry {:application/id application-id :feedback/event-id (:feedback/event-id context)
                          :prior-state/revision (:prior-state/revision context) :status :committed
                          :input/digests input-subject :output/digest (value-digest expected-next)}
          universe-ids (:complete/application-ids universe)
          entries (:entries ledger)
          ids (mapv :application/id entries)
          matches (filterv #(= application-id (:application/id %)) entries)]
      (when-not (= expected-next claimed)
        (refuse! :e6b/next-state-mismatch "Claimed next state differs from production replay" {}))
      (when (empty? matches)
        (refuse! :e6b/feedback-not-applied "No committed application" {}))
      (when (> (count matches) 1)
        (refuse! :e6b/duplicate-feedback "Duplicate application identity" {}))
      (when (or (seq (filter #(and (not= application-id (:application/id %))
                                   (= (:feedback/event-id context) (:feedback/event-id %))) entries))
                (seq (filter #(and (not= application-id (:application/id %))
                                   (= (:prior-state/revision context) (:prior-state/revision %))) entries)))
        (refuse! :e6b/feedback-conflict "Event or prior revision has another application" {}))
      (when-not (and (= transition-subject (:transition/subject universe))
                     (= (:application-ledger source-pins) (:ledger/sha256 universe))
                     (= ids universe-ids) (= (count ids) (count (distinct ids)))
                     (= :independently-configured-complete (:authority/status universe))
                     (nonblank? (:authority/owner universe))
                     (= (value-digest transition-subject) (:transition/subject-digest universe)))
        (refuse! :e6b/application-universe-incomplete
                 "Independent complete application universe is absent or conflicting" {}))
      (cond
        (not= expected-entry (first matches))
        (refuse! :e6b/feedback-conflict "Committed application pins conflict" {}))
      {:schema/version schema-version :scope :isolated-test
       :identity common :destination/tick-index destination
       :application expected-entry :state actual-state
       :replay/identical? true :storage-enforcement? false
       :production-edge-fired? false
       :sources (mapv #(dissoc % :record) resolved)
       :dependencies {:temporal-hierarchy/sha256
                      "e3e532ae1b0b123730299bd7caa1105b074b7d27912c5508d29c395f21d34eef"
                      :intrinsic-values/sha256
                      "ea07fb662fed93e801e613a102f35f7baa3c3053fd636d478d14e504a1be758b"}})))
