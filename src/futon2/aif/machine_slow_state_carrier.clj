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
    (let [subject (:transition/subject proposal-evidence)]
      (when-not (and (= (:model/id prior) (:model/id subject))
                     (= (:model/revision prior) (:model/revision subject))
                     (= (:run/id prior) (:run/id subject))
                     (= (:tick/index prior) (:source/tick-index subject))
                     (= (:tick/index next) (inc (:tick/index prior)))
                     (= (:destination/tick-index subject) (:tick/index next))
                     (= (:state/revision prior) (:prior-state/revision subject)
                        (:predecessor/revision next))
                     (= (:state/revision next) (:next-state/revision subject))
                     (= (:feedback/event-id next) (:feedback/event-id subject)
                        (:feedback/event-id proposal-evidence))
                     (= (select-keys prior [:model/id :model/revision :run/id])
                        (select-keys next [:model/id :model/revision :run/id])))
        (refuse! :e6b-carrier/transition-subject-mismatch {})))
    {:schema :wm/e6b-carrier-projection-v1 :scope :isolated-test
     :status :structurally-projected :authority/status :none
     :proposal-evidence proposal-evidence :original-sources sources
     :prior (encode-carrier (carrier prior prior))
     :next (encode-carrier (carrier next (:state next)))
     :digest-roles {:raw-source (:source/digests proposal-evidence)
                    :record-value (:input/digests proposal-evidence)
                    :complete-next-record (value-digest next)}}))
