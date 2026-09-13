(ns futon2.aif.machine-slow-state-carrier
  "Pure structural codec for E6b prior/next slow-state carriers.

   This codec checks and preserves an already produced isolated proposal and
   its exact seven source buffers. It does not validate the transition law,
   confer authority, or write storage."
  (:require [clojure.edn :as edn]
            [clojure.string :as str])
  (:import [java.io PushbackReader StringReader]
           [java.nio ByteBuffer]
           [java.nio.charset CodingErrorAction StandardCharsets]
           [java.security MessageDigest]
           [java.time Instant]
           [java.util Base64]))

(def source-roles
  [:context :prior-state :e2b-subject :lifecycle-relation :outcome
   :outcome-review :outcome-review-artifact])
(def ^:private hex64 #"[0-9a-f]{64}")
(defn- refuse! [kind data] (throw (ex-info (name kind) (assoc data :refusal kind))))
(defn- sha256 [^bytes bs]
  (apply str (map #(format "%02x" (bit-and 255 %))
                  (.digest (doto (MessageDigest/getInstance "SHA-256") (.update bs))))))
(defn- value-bytes [x] (.getBytes (pr-str x) StandardCharsets/UTF_8))
(defn- value-digest [x] (sha256 (value-bytes x)))
(defn- nonblank? [x] (and (string? x) (not (str/blank? x))))
(defn- finite? [x] (and (number? x) (Double/isFinite (double x))))
(defn- instant? [x] (try (Instant/parse x) true (catch Throwable _ false)))

(defn- strict-edn [^bytes bs]
  (try
    (let [decoder (doto (.newDecoder StandardCharsets/UTF_8)
                    (.onMalformedInput CodingErrorAction/REPORT)
                    (.onUnmappableCharacter CodingErrorAction/REPORT))
          rdr (PushbackReader. (StringReader. (str (.decode decoder (ByteBuffer/wrap bs)))))
          eof (Object.) x (edn/read {:eof eof} rdr) tail (edn/read {:eof eof} rdr)]
      (when (or (identical? eof x) (not (identical? eof tail)))
        (refuse! :e6b-carrier/invalid-edn-cardinality {}))
      x)
    (catch clojure.lang.ExceptionInfo e (throw e))
    (catch Throwable e
      (throw (ex-info "invalid strict UTF-8 EDN" {:refusal :e6b-carrier/invalid-edn} e)))))

(defn- exact-keys! [label expected x]
  (when-not (and (map? x) (= expected (set (keys x))))
    (refuse! :e6b-carrier/schema-invalid {:label label})))
(defn- valid-entry! [entry]
  (exact-keys! :intrinsic-entry
               #{:alpha :beta :intrinsic-value :n-emissions :n-followthrough :as-of} entry)
  (when-not (and (every? finite? ((juxt :alpha :beta :intrinsic-value) entry))
                 (pos? (double (:alpha entry))) (pos? (double (:beta entry)))
                 (nat-int? (:n-emissions entry)) (nat-int? (:n-followthrough entry))
                 (instant? (:as-of entry)))
    (refuse! :e6b-carrier/coordinate-invalid {})))
(defn- valid-intrinsics! [x]
  (when-not (and (map? x) (seq x) (every? keyword? (keys x)))
    (refuse! :e6b-carrier/coordinate-invalid {}))
  (run! valid-entry! (vals x)))
(defn- valid-identity! [x]
  (when-not (and (or (keyword? (:model/id x)) (nonblank? (:model/id x)))
                 (nonblank? (:model/revision x)) (nonblank? (:run/id x))
                 (nat-int? (:tick/index x)) (nonblank? (:state/revision x)))
    (refuse! :e6b-carrier/identity-invalid {})))
(defn- valid-prior! [x]
  (exact-keys! :prior
               #{:schema/version :scope :model/id :model/revision :run/id :tick/index
                 :state/revision :slow/mode :slow/intrinsics} x)
  (when-not (and (= :wm/e6b-prior-slow-state-v1 (:schema/version x))
                 (= :isolated-test (:scope x)) (keyword? (:slow/mode x)))
    (refuse! :e6b-carrier/prior-invalid {}))
  (valid-identity! x) (valid-intrinsics! (:slow/intrinsics x)))
(defn- valid-update! [x]
  (exact-keys! :update
               #{:class :as-of :outer-loop-run-id :window-days :alpha-pre :beta-pre
                 :alpha-post :beta-post :intrinsic-value-pre :intrinsic-value-post
                 :n-emissions-in-window :n-followthrough-in-window :evidence-refs} x)
  (when-not (and (keyword? (:class x)) (instant? (:as-of x))
                 (nonblank? (:outer-loop-run-id x)) (nat-int? (:window-days x))
                 (every? finite? ((juxt :alpha-pre :beta-pre :alpha-post :beta-post
                                       :intrinsic-value-pre :intrinsic-value-post) x))
                 (nat-int? (:n-emissions-in-window x))
                 (nat-int? (:n-followthrough-in-window x)) (vector? (:evidence-refs x)))
    (refuse! :e6b-carrier/next-state-invalid {})))
(defn- valid-next! [x]
  (exact-keys! :next
               #{:schema/version :scope :model/id :model/revision :run/id :tick/index
                 :state/revision :predecessor/revision :feedback/event-id :state} x)
  (let [state (:state x) feedback (:slow/feedback state)]
    (when-not (and (= :wm/e6b-next-slow-state-v1 (:schema/version x))
                   (= :isolated-test (:scope x))
                   (nonblank? (:predecessor/revision x)) (nonblank? (:feedback/event-id x)))
      (refuse! :e6b-carrier/next-state-invalid {}))
    (valid-identity! x)
    (exact-keys! :next-state #{:slow/mode :slow/intrinsics :slow/previous-mode :slow/feedback} state)
    (when-not (and (keyword? (:slow/mode state)) (keyword? (:slow/previous-mode state)))
      (refuse! :e6b-carrier/next-state-invalid {}))
    (valid-intrinsics! (:slow/intrinsics state))
    (exact-keys! :feedback #{:outcome :update :evidence-ref} feedback)
    (exact-keys! :feedback-outcome
                 #{:fast/action-class :fast/witnessed? :fast/succeeded?} (:outcome feedback))
    (when-not (and (keyword? (get-in feedback [:outcome :fast/action-class]))
                   (true? (get-in feedback [:outcome :fast/witnessed?]))
                   (instance? Boolean (get-in feedback [:outcome :fast/succeeded?])))
      (refuse! :e6b-carrier/next-state-invalid {}))
    (valid-update! (:update feedback))))

(defn- carrier [record slow]
  (array-map :schema :wm/e6b-store-state-carrier-v1
             :model/id (:model/id record) :model/revision (:model/revision record)
             :run/id (:run/id record) :tick/index (:tick/index record)
             :state/revision (:state/revision record)
             :slow/mode (:slow/mode slow) :slow/intrinsics (:slow/intrinsics slow)))
(defn- encode-carrier [x]
  (let [bs (value-bytes x) back (strict-edn bs)]
    (when-not (= x back) (refuse! :e6b-carrier/roundtrip-mismatch {}))
    {:carrier x :text (String. bs StandardCharsets/UTF_8)
     :bytes/base64 (.encodeToString (Base64/getEncoder) bs) :sha256 (sha256 bs)}))
(defn- decode-source! [role descriptor proposal]
  (exact-keys! role #{:bytes/base64 :source-sha256 :value-sha256} descriptor)
  (let [bs (try (.decode (Base64/getDecoder) ^String (:bytes/base64 descriptor))
                (catch Throwable e
                  (throw (ex-info "invalid base64" {:refusal :e6b-carrier/source-invalid
                                                     :role role} e))))
        record (strict-edn bs)]
    (when-not (and (re-matches hex64 (:source-sha256 descriptor ""))
                   (re-matches hex64 (:value-sha256 descriptor ""))
                   (= (:source-sha256 descriptor) (sha256 bs))
                   (= (:value-sha256 descriptor) (value-digest record))
                   (= (:source-sha256 descriptor) (get-in proposal [:source/digests role]))
                   (= (:value-sha256 descriptor) (get-in proposal [:input/digests role])))
      (refuse! :e6b-carrier/source-digest-mismatch {:role role}))
    {:record record :bytes/base64 (:bytes/base64 descriptor)
     :source-sha256 (:source-sha256 descriptor) :value-sha256 (:value-sha256 descriptor)}))

(defn- valid-proposal-shape! [proposal]
  (exact-keys! :identity
               #{:model/id :model/revision :run/id :source/tick-index
                 :destination/tick-index} (:identity proposal))
  (exact-keys! :transition-subject
               #{:model/id :model/revision :run/id :source/tick-index
                 :destination/tick-index :candidate/occurrence-id :action
                 :fast/action-class :prior-state/revision :next-state/revision
                 :feedback/event-id} (:transition/subject proposal))
  (exact-keys! :prior-wrapper #{:state/revision :state :state-sha256} (:prior proposal))
  (exact-keys! :next-wrapper #{:state/revision :state :state-sha256} (:next proposal))
  (exact-keys! :canonical-digests #{:e3 :e2b} (:canonical/digests proposal))
  (exact-keys! :validator #{:source-sha256 :dependency-sha256s} (:validator proposal))
  (exact-keys! :dependencies #{:temporal-hierarchy :intrinsic-values}
               (get-in proposal [:validator :dependency-sha256s]))
  (when-not (and (every? #(re-matches hex64 %)
                         (concat (vals (:input/digests proposal))
                                 (vals (:source/digests proposal))
                                 (vals (:canonical/digests proposal))
                                 [(:source-sha256 (:validator proposal))]
                                 (vals (get-in proposal [:validator :dependency-sha256s]))))
                 (every? nonblank? ((juxt :application/id :feedback/event-id) proposal))
                 (instant? (:committed-at proposal)))
    (refuse! :e6b-carrier/proposal-shape-invalid {})))

(defn- valid-context! [context]
  (exact-keys! :context
               #{:schema/version :scope :model/id :model/revision :run/id :tick/index
                 :destination/tick-index :prior-state/revision :next-state/revision
                 :feedback/event-id :application/id :candidate/occurrence-id :action
                 :fast/action-class :outcome-reviewer/id :outcome-observer/id
                 :prior-state/as-of :destination/as-of :canonical/e3-context
                 :canonical/e2b-context :canonical/field-subject} context)
  (when-not (and (= :wm/e6b-transition-context-v1 (:schema/version context))
                 (= :isolated-test (:scope context))
                 (or (keyword? (:model/id context)) (nonblank? (:model/id context)))
                 (every? nonblank? ((juxt :model/revision :run/id :prior-state/revision
                                         :next-state/revision :feedback/event-id :application/id
                                         :outcome-reviewer/id :outcome-observer/id) context))
                 (nat-int? (:tick/index context))
                 (= (inc (:tick/index context)) (:destination/tick-index context))
                 (some? (:candidate/occurrence-id context))
                 (map? (:action context)) (seq (:action context))
                 (keyword? (:fast/action-class context))
                 (instant? (:prior-state/as-of context))
                 (instant? (:destination/as-of context))
                 (every? map? ((juxt :canonical/e3-context :canonical/e2b-context
                                     :canonical/field-subject) context)))
    (refuse! :e6b-carrier/context-invalid {})))

(defn- outcome-subject [outcome]
  (select-keys outcome
               [:model/id :model/revision :run/id :tick/index
                :candidate/occurrence-id :action :fast/action-class
                :terminal/status :terminal/at :fast/witnessed? :fast/succeeded?
                :outcome/evidence-id :outcome/producer-id]))

(defn- validate-source-subjects! [sources context proposal]
  (let [e2b (get-in sources [:e2b-subject :record])
        outcome (get-in sources [:outcome :record])
        relation (get-in sources [:lifecycle-relation :record])
        review (get-in sources [:outcome-review :record])
        artifact (get-in sources [:outcome-review-artifact :record])
        common (select-keys context [:model/id :model/revision :run/id :tick/index])
        out-subject (outcome-subject outcome)
        relation-subject (:subject relation)]
    (exact-keys! :e2b-subject
                 #{:schema/version :scope :model/id :model/revision :run/id :tick/index
                   :candidate/occurrence-id :action :fast/action-class
                   :canonical/e3-digest :canonical/e2b-digest} e2b)
    (exact-keys! :outcome
                 #{:schema/version :scope :model/id :model/revision :run/id :tick/index
                   :candidate/occurrence-id :action :terminal/status :fast/action-class
                   :terminal/at :fast/witnessed? :fast/succeeded? :outcome/evidence-id
                   :outcome/authority-ref :outcome/producer-id :outcome/reviewer-id} outcome)
    (exact-keys! :lifecycle-relation
                 #{:schema/version :scope :relation/id :relation/type :subject
                   :enactment/at :observer/origin :observer/authority-ref
                   :observer/subject} relation)
    (exact-keys! :lifecycle-subject
                 #{:e3/context :e2b/context :field/subject :e3/time :e3/digest
                   :e2b/digest :candidate/occurrence-id :action :fast/action-class}
                 relation-subject)
    (exact-keys! :observer-subject
                 #{:observer/id :observer/origin :observer/authority-ref :outcome/subject}
                 (:observer/subject relation))
    (exact-keys! :outcome-review
                 #{:schema/version :scope :review/outcome :subject :reviewer/id :review/id
                   :observer/id :reviewed-at :review/artifact-sha256} review)
    (exact-keys! :outcome-review-artifact
                 #{:schema/version :scope :subject :review/id :reviewer/id :observer/id
                   :reviewed-at :executed?} artifact)
    (when-not
     (and (= common (select-keys e2b [:model/id :model/revision :run/id :tick/index]))
          (= common (select-keys outcome [:model/id :model/revision :run/id :tick/index]))
          (= :wm/e6b-e2b-subject-v1 (:schema/version e2b))
          (= :wm/e6b-outcome-authority-v1 (:schema/version outcome))
          (= :wm/e6b-lifecycle-relation-v1 (:schema/version relation))
          (= :wm/e6b-outcome-review-v1 (:schema/version review))
          (= :wm/e6b-outcome-review-artifact-v1 (:schema/version artifact))
          (every? #(= :isolated-test (:scope %)) [e2b outcome relation review artifact])
          (= :e3-authorizes-e2b-enactment (:relation/type relation))
          (nonblank? (:relation/id relation))
          (= (:canonical/e3-context context) (:e3/context relation-subject))
          (= (:canonical/e2b-context context) (:e2b/context relation-subject))
          (= (:canonical/field-subject context) (:field/subject relation-subject))
          (= (:canonical/e3-digest e2b) (:e3/digest relation-subject)
             (get-in proposal [:canonical/digests :e3]))
          (= (:canonical/e2b-digest e2b) (:e2b/digest relation-subject)
             (get-in proposal [:canonical/digests :e2b]))
          (= (:candidate/occurrence-id context) (:candidate/occurrence-id relation-subject))
          (= (:action context) (:action relation-subject))
          (= (:fast/action-class context) (:fast/action-class relation-subject))
          (map? (:e3/time relation-subject))
          (instant? (:enactment/at relation))
          (instant? (:terminal/at outcome))
          (instant? (:reviewed-at review))
          (= out-subject (:subject review) (:subject artifact)
             (get-in relation [:observer/subject :outcome/subject]))
          (= (:review/id review) (:review/id artifact) (:outcome/authority-ref outcome))
          (= (:reviewer/id review) (:reviewer/id artifact) (:outcome/reviewer-id outcome))
          (= (:observer/id review) (:observer/id artifact)
             (get-in relation [:observer/subject :observer/id]))
          (= (:reviewed-at review) (:reviewed-at artifact))
          (= (:observer/origin relation)
             (get-in relation [:observer/subject :observer/origin]))
          (= (:observer/authority-ref relation)
             (get-in relation [:observer/subject :observer/authority-ref]))
          (= (:review/artifact-sha256 review)
             (get-in proposal [:source/digests :outcome-review-artifact]))
          (= :accepted (:review/outcome review)) (true? (:executed? artifact)))
      (refuse! :e6b-carrier/source-subject-mismatch {}))))

(defn- validate-structural-joins! [proposal sources prior next]
  (let [context (get-in sources [:context :record])
        subject (:transition/subject proposal)
        identity (:identity proposal)
        e2b (get-in sources [:e2b-subject :record])
        outcome (get-in sources [:outcome :record])
        feedback (get-in next [:state :slow/feedback])
        update (:update feedback)
        cls (:fast/action-class context)]
    (valid-context! context)
    (validate-source-subjects! sources context proposal)
    (when-not (and
               (= identity {:model/id (:model/id context)
                            :model/revision (:model/revision context)
                            :run/id (:run/id context)
                            :source/tick-index (:tick/index context)
                            :destination/tick-index (:destination/tick-index context)})
               (= subject {:model/id (:model/id context)
                           :model/revision (:model/revision context)
                           :run/id (:run/id context)
                           :source/tick-index (:tick/index context)
                           :destination/tick-index (:destination/tick-index context)
                           :candidate/occurrence-id (:candidate/occurrence-id context)
                           :action (:action context) :fast/action-class cls
                           :prior-state/revision (:prior-state/revision context)
                           :next-state/revision (:next-state/revision context)
                           :feedback/event-id (:feedback/event-id context)})
               (= (select-keys prior [:model/id :model/revision :run/id :tick/index])
                  (select-keys context [:model/id :model/revision :run/id :tick/index]))
               (= (:application/id proposal) (:application/id context))
               (= (:feedback/event-id proposal) (:feedback/event-id context))
               (= (:committed-at proposal) (:destination/as-of context))
               (= (:state/revision (:prior proposal)) (:prior-state/revision context)
                  (:state/revision prior))
               (= (:state/revision (:next proposal)) (:next-state/revision context)
                  (:state/revision next))
               (= (:candidate/occurrence-id context) (:candidate/occurrence-id e2b)
                  (:candidate/occurrence-id outcome))
               (= (:action context) (:action e2b) (:action outcome))
               (= cls (:fast/action-class e2b) (:fast/action-class outcome)
                  (get-in feedback [:outcome :fast/action-class]) (:class update))
               (= (select-keys outcome [:fast/action-class :fast/witnessed? :fast/succeeded?])
                  (:outcome feedback))
               (= (:run/id context) (:outer-loop-run-id update))
               (= (:destination/as-of context) (:as-of update))
               (= (:outcome/evidence-id outcome) (:evidence-ref feedback))
               (= [(:outcome/evidence-id outcome)] (:evidence-refs update))
               (= (:slow/mode prior) (get-in next [:state :slow/previous-mode]))
               (= (select-keys prior [:model/id :model/revision :run/id])
                  (select-keys next [:model/id :model/revision :run/id]))
               (= (:tick/index next) (inc (:tick/index prior)))
               (= (:predecessor/revision next) (:state/revision prior))
               (= (:feedback/event-id next) (:feedback/event-id context))
               (= (:canonical/e3-digest e2b) (get-in proposal [:canonical/digests :e3]))
               (= (:canonical/e2b-digest e2b) (get-in proposal [:canonical/digests :e2b])))
      (refuse! :e6b-carrier/transition-subject-mismatch {}))))

(defn project-transition
  "Structurally project carriers from proposal evidence and exact source bytes."
  [{:keys [proposal-evidence original-sources] :as bundle}]
  (exact-keys! :bundle #{:proposal-evidence :original-sources} bundle)
  (exact-keys! :proposal
               #{:schema :scope :status :identity :transition/subject :application/id
                 :feedback/event-id :prior :next :input/digests :source/digests
                 :canonical/digests :committed-at :validator :authority/status}
               proposal-evidence)
  (when-not (and (= :wm/e6b-transition-proposal-evidence-v1 (:schema proposal-evidence))
                 (= :isolated-test (:scope proposal-evidence))
                 (= :prospectively-validated (:status proposal-evidence))
                 (= :proposal-evidence-only (:authority/status proposal-evidence))
                 (= (set source-roles) (set (keys original-sources)))
                 (= (set source-roles) (set (keys (:input/digests proposal-evidence))))
                 (= (set source-roles) (set (keys (:source/digests proposal-evidence)))))
    (refuse! :e6b-carrier/proposal-shape-invalid {}))
  (valid-proposal-shape! proposal-evidence)
  (let [sources (into {} (map (fn [role]
                                [role (decode-source! role (original-sources role)
                                                      proposal-evidence)]) source-roles))
        prior (get-in proposal-evidence [:prior :state])
        next (get-in proposal-evidence [:next :state])]
    (when-not (and (= prior (get-in sources [:prior-state :record]))
                   (= (:state-sha256 (:prior proposal-evidence)) (value-digest prior))
                   (= (:state-sha256 (:next proposal-evidence)) (value-digest next)))
      (refuse! :e6b-carrier/proposal-record-mismatch {}))
    (valid-prior! prior) (valid-next! next)
    (validate-structural-joins! proposal-evidence sources prior next)
    {:schema :wm/e6b-carrier-projection-v1 :scope :isolated-test
     :status :structurally-projected :authority/status :none
     :proposal-evidence proposal-evidence :original-sources sources
     :prior (encode-carrier (carrier prior prior))
     :next (encode-carrier (carrier next (:state next)))
     :digest-roles {:raw-source (:source/digests proposal-evidence)
                    :record-value (:input/digests proposal-evidence)
                    :complete-next-record (value-digest next)}}))
