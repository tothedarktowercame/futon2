(ns futon2.aif.machine-slow-feedback-provenance
  "Pure structural constructor for an immutable E6b provenance envelope.

   All inputs remain unauthenticated structural evidence. This namespace does
   not execute a transition, own HEAD, publish storage, or confer authority."
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [futon2.aif.machine-slow-state-carrier :as carrier])
  (:import (java.io PushbackReader StringReader)
           (java.nio ByteBuffer)
           (java.nio.charset CodingErrorAction StandardCharsets)
           (java.security MessageDigest)
           (java.util Base64)))

(def closure-roles
  [:e3/pending :e3/verdict :e3/review :r9/input
   :config/e1 :config/e2b :config/e3 :config/canonical
   :e1/ranked-support :e1/field-membership :e1/costs :e1/utilities :e1/budgets
   :e2b/context :e2b/selection :e2b/enactment])
(def output-roles [:e3 :e2b])
(def ^:private e3-source-order [:pending :verdict :review])
(def ^:private e2b-witness-order [:context :selection :enactment])
(def ^:private e1-source-order
  [:ranked-support :field-membership :costs :utilities :budgets])
(def ^:private hex64 #"[0-9a-f]{64}")
(defn- refuse! [kind data] (throw (ex-info (name kind) (assoc data :refusal kind))))
(defn- nonblank? [x] (and (string? x) (not (str/blank? x))))
(defn- sha256 [^bytes bs]
  (apply str (map #(format "%02x" (bit-and 255 %))
                  (.digest (doto (MessageDigest/getInstance "SHA-256") (.update bs))))))
(defn- form-bytes [x] (.getBytes (pr-str x) StandardCharsets/UTF_8))
(defn- value-digest [x] (sha256 (form-bytes x)))
(defn- strict-edn [^bytes bs]
  (try
    (let [decoder (doto (.newDecoder StandardCharsets/UTF_8)
                    (.onMalformedInput CodingErrorAction/REPORT)
                    (.onUnmappableCharacter CodingErrorAction/REPORT))
          rdr (PushbackReader. (StringReader. (str (.decode decoder (ByteBuffer/wrap bs)))))
          eof (Object.) x (edn/read {:eof eof} rdr) tail (edn/read {:eof eof} rdr)]
      (when (or (identical? eof x) (not (identical? eof tail)))
        (refuse! :e6b-provenance/invalid-edn-cardinality {}))
      x)
    (catch clojure.lang.ExceptionInfo e (throw e))
    (catch Throwable e
      (throw (ex-info "invalid strict UTF-8 EDN" {:refusal :e6b-provenance/invalid-edn} e)))))
(defn- exact-keys! [label expected x]
  (when-not (and (map? x) (= expected (set (keys x))))
    (refuse! :e6b-provenance/schema-invalid {:label label})))
(defn- descriptor! [role x]
  (exact-keys! role #{:bytes/base64 :source-sha256 :value-sha256} x)
  (let [bs (try (.decode (Base64/getDecoder) ^String (:bytes/base64 x))
                (catch Throwable e
                  (throw (ex-info "invalid base64" {:refusal :e6b-provenance/bytes-invalid
                                                     :role role} e))))
        record (strict-edn bs)]
    (when-not (and (re-matches hex64 (:source-sha256 x ""))
                   (re-matches hex64 (:value-sha256 x ""))
                   (= (:source-sha256 x) (sha256 bs))
                   (= (:value-sha256 x) (value-digest record)))
      (refuse! :e6b-provenance/digest-mismatch {:role role}))
    {:bytes/base64 (:bytes/base64 x) :source-sha256 (:source-sha256 x)
     :value-sha256 (:value-sha256 x) :record record}))
(defn- pins [resolved roles]
  (into {} (map (fn [role] [role (:source-sha256 (resolved role))]) roles)))
(defn- pin-map [config]
  (into {} (map (fn [[role pin]] [role (:sha256 pin)]) (:sources config))))
(defn- witness-pin-map [config]
  (into {} (map (fn [[role pin]] [role (:sha256 pin)]) (:witnesses config))))
(defn- ordered-pins! [boundary expected-order xs]
  (when-not (and (vector? xs)
                 (= expected-order (mapv :label xs))
                 (= (count xs) (count (distinct (map :label xs))))
                 (every? #(and (= #{:label :sha256} (set (keys %)))
                               (keyword? (:label %))
                               (string? (:sha256 %))
                               (re-matches hex64 (:sha256 %))) xs))
    (refuse! :e6b-provenance/source-manifest-invalid {:boundary boundary}))
  (mapv :sha256 xs))
(defn- configured-pins [config-map order]
  (mapv #(get-in config-map [% :sha256]) order))
(defn- e3-identity [x]
  (select-keys x [:model/id :model/revision :run/id :tick/index :cohort/id :event/id]))
(defn- canonical-pending-subject [pending]
  (select-keys pending [:model/id :model/revision :run/id :cohort/id :tick/index
                        :event/id :phase :authorization-at :subject]))

(defn- validate-closure! [closure outputs proposal]
  (when-not (= (set closure-roles) (set (keys closure)))
    (refuse! :e6b-provenance/closure-incomplete {}))
  (when-not (= (set output-roles) (set (keys outputs)))
    (refuse! :e6b-provenance/output-closure-incomplete {}))
  (let [c (into {} (map (fn [role] [role (descriptor! role (closure role))]) closure-roles))
        o (into {} (map (fn [role] [role (descriptor! role (outputs role))]) output-roles))
        e1 (get-in c [:config/e1 :record]) e2b-cfg (get-in c [:config/e2b :record])
        e3-cfg (get-in c [:config/e3 :record]) canonical (get-in c [:config/canonical :record])
        pending (get-in c [:e3/pending :record])
        review (get-in c [:e3/review :record]) verdict (get-in c [:e3/verdict :record])
        r9-input (get-in c [:r9/input :record]) admission (:canonical-admission verdict)
        e3-output (get-in o [:e3 :record]) e2b-output (get-in o [:e2b :record])
        e2b-context (get-in c [:e2b/context :record])
        e2b-selection (get-in c [:e2b/selection :record])
        e2b-enactment (get-in c [:e2b/enactment :record])
        proposal-common {:model/id (get-in proposal [:identity :model/id])
                         :model/revision (get-in proposal [:identity :model/revision])
                         :run/id (get-in proposal [:identity :run/id])
                         :tick/index (get-in proposal [:identity :source/tick-index])}
        transition-subject (:transition/subject proposal)
        e1-pins (pins c [:e1/ranked-support :e1/field-membership :e1/costs
                         :e1/utilities :e1/budgets])
        e2b-pins (pins c [:e2b/context :e2b/selection :e2b/enactment])
        e3-pins (pins c [:e3/pending :e3/verdict :e3/review])]
    (when-not
     (and (= canonical {:e3 e3-cfg :e2b e2b-cfg})
          (= r9-input (:r9/input review))
          (= e1 (:e2a-resolver e3-cfg) (:e2a-resolver e2b-cfg))
          (= e1-pins
             {:e1/ranked-support (get (pin-map e1) :ranked-support)
              :e1/field-membership (get (pin-map e1) :field-membership)
              :e1/costs (get (pin-map e1) :costs)
              :e1/utilities (get (pin-map e1) :utilities)
              :e1/budgets (get (pin-map e1) :budgets)})
          (= e2b-pins
             {:e2b/context (get (witness-pin-map e2b-cfg) :context)
              :e2b/selection (get (witness-pin-map e2b-cfg) :selection)
              :e2b/enactment (get (witness-pin-map e2b-cfg) :enactment)})
          (= e3-pins
             {:e3/pending (get-in e3-cfg [:evidence :pending :sha256])
              :e3/verdict (get-in e3-cfg [:evidence :verdict :sha256])
              :e3/review (get-in e3-cfg [:evidence :review :sha256])})
          ;; Canonical outputs must name the exact resolved source bytes.  A
          ;; coherently re-pinned config is not allowed to borrow stale output.
          (= (ordered-pins! :e3/output-sources e3-source-order (:sources e3-output))
             (mapv #(:source-sha256 (c (keyword "e3" (name %)))) e3-source-order))
          (= (ordered-pins! :e2b/output-e1-sources e1-source-order
                            (get-in e2b-output [:sources :e2a :e1-verification :sources]))
             (configured-pins (:sources e1) e1-source-order))
          (= (ordered-pins! :e2b/output-witnesses e2b-witness-order
                            (get-in e2b-output [:sources :witnesses]))
             (configured-pins (:witnesses e2b-cfg) e2b-witness-order))
          (= (:subject e2b-output)
             (:subject e2b-context) (:subject e2b-selection) (:subject e2b-enactment))
          (= (:event/id e2b-output)
             (:event/id e2b-context) (:event/id e2b-selection) (:event/id e2b-enactment))
          (= (:identity e2b-output) (:binding e2b-selection) (:binding e2b-enactment))
          (= (:selected e2b-output)
             {:candidate/id (:selected/occurrence-id e2b-selection)
              :action (:selected/action e2b-selection)}
             {:candidate/id (:selected/occurrence-id e2b-enactment)
              :action (:selected/action e2b-enactment)})
          (= (:enacted e2b-output)
             {:candidate/id (:enacted/occurrence-id e2b-enactment)
              :action (:enacted/action e2b-enactment)})
          (= (ordered-pins! :e2b/subject-e1-pins e1-source-order
                            (get-in e2b-output [:subject :e1-source-pins]))
             (configured-pins (:sources e1) e1-source-order))
          (= (e3-identity pending) (e3-identity verdict) (e3-identity review)
             (e3-identity (:identity e3-output)))
          (= (:subject pending) (:subject verdict) (:subject review) (:subject e3-output))
          (= (ordered-pins! :e3/pending-field-pins e1-source-order
                            (:field-pins (:subject pending)))
             (configured-pins (:sources e1) e1-source-order))
          (= (get-in proposal [:canonical/digests :e3]) (value-digest e3-output))
          (= (get-in proposal [:canonical/digests :e2b]) (value-digest e2b-output))
          (= :mechanism-authorized (:decision e3-output))
          (= :exact-occurrence-and-action (:correspondence e2b-output))
          (= proposal-common
             (select-keys (:identity e3-output) [:model/id :model/revision :run/id :tick/index])
             (:identity e2b-output))
          (= (:candidate/occurrence-id transition-subject)
             (get-in e3-output [:subject :candidate/occurrence-id])
             (get-in e2b-output [:selected :candidate/id])
             (get-in e2b-output [:enacted :candidate/id]))
          (= (:action transition-subject)
             (get-in e3-output [:subject :action])
             (get-in e2b-output [:selected :action])
             (get-in e2b-output [:enacted :action]))
          (= :admitted (:decision admission))
          (= {:boundary :e3/pre-enact
              :artifact-ref (:artifact/ref (:subject pending))
              :digest (value-digest (canonical-pending-subject pending))}
             (:subject r9-input))
          (= (value-digest (canonical-pending-subject pending)) (:subject-digest admission))
          (= (:artifact/ref (:subject pending)) (:artifact/ref review))
          (= (:claim/id (:subject pending)) (:claim/id review))
          (= (:trace/id (:subject pending)) (:producer-trace/id review))
          (= (:artifact/ref (:subject pending)) (get-in r9-input [:producer-job :artifact-ref]))
          (= (:checker-source-sha256 r9-input) (:checker-source-sha256 admission))
          (= (:bootstrap-anchor r9-input) (:bootstrap-anchor admission))
          (= (get-in r9-input [:role-binding :author]) (get-in admission [:roles :author]))
          (= (get-in r9-input [:role-binding :reviewer]) (get-in admission [:roles :reviewer]))
          (= (select-keys (:producer-job r9-input) [:job-id :trace-id]) (:producer admission))
          (= (select-keys (:reviewer-job r9-input) [:job-id :trace-id]) (:reviewer admission))
          (= (:admission-at r9-input) (:at admission)))
      (refuse! :e6b-provenance/closure-join-mismatch {}))
    {:inputs c :outputs o}))

(defn- expected-head! [head projection]
  (exact-keys! :expected-head
               #{:store/id :generation :transaction-sha256 :state/revision :state-sha256} head)
  (when-not (and (nonblank? (:store/id head)) (nat-int? (:generation head))
                 (re-matches hex64 (:transaction-sha256 head ""))
                 (= (:state/revision head) (get-in projection [:prior :carrier :state/revision]))
                 (= (:state-sha256 head) (get-in projection [:prior :sha256])))
    (refuse! :e6b-provenance/expected-head-mismatch {}))
  head)

(defn construct
  "Construct and validate a serialized structural provenance envelope."
  [{:keys [proposal-evidence original-sources canonical-closure canonical-outputs
           carrier-projection expected-head] :as input}]
  (exact-keys! :input
               #{:proposal-evidence :original-sources :canonical-closure
                 :canonical-outputs :carrier-projection :expected-head} input)
  (let [projection (carrier/project-transition
                    {:proposal-evidence proposal-evidence :original-sources original-sources})]
    (when-not (= projection carrier-projection)
      (refuse! :e6b-provenance/carrier-projection-mismatch {}))
    (let [closure (validate-closure! canonical-closure canonical-outputs proposal-evidence)
          head (expected-head! expected-head projection)
          retrospective-input {:context (get-in proposal-evidence [:input/digests :context])
                               :prior (get-in proposal-evidence [:input/digests :prior-state])
                               :e2b (get-in proposal-evidence [:input/digests :e2b-subject])
                               :outcome (get-in proposal-evidence [:input/digests :outcome])}
          record {:schema :wm/e6b-transition-provenance-v1 :scope :isolated-test
                  :status :structurally-validated :authority/status :none
                  :proposal-evidence proposal-evidence
                  :original-sources (:original-sources projection)
                  :canonical-closure closure :carrier-projection projection
                  :expected-head head
                  :retrospective-application-view
                  {:application/id (:application/id proposal-evidence)
                   :feedback/event-id (:feedback/event-id proposal-evidence)
                   :prior-state/revision (get-in proposal-evidence [:prior :state/revision])
                   :status :committed :input/digests retrospective-input
                   :output/digest (get-in projection [:digest-roles :complete-next-record])}
                  :committed-at (:committed-at proposal-evidence)}
          bs (form-bytes record) back (strict-edn bs)]
      (when-not (= record back) (refuse! :e6b-provenance/roundtrip-mismatch {}))
      {:schema :wm/e6b-serialized-provenance-v1 :scope :isolated-test
       :status :structural-artifact :authority/status :none
       :record record :text (String. bs StandardCharsets/UTF_8)
       :bytes/base64 (.encodeToString (Base64/getEncoder) bs)
       :sha256 (sha256 bs)})))
