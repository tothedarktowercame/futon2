(ns futon2.aif.meta-outcome-dataset
  "Pure empirical META outcome rows from an externally pinned run manifest."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.meta-field-observation :as field])
  (:import [java.time Instant]))

(def schema :wm/meta-outcome-dataset-v1)
(def manifest-schema :wm/run-record-manifest-v1)
(def projection-schema :wm/learning-projection-v1)
(declare read-record-bytes)

(defn record-entry [entry]
  (select-keys entry [:path :sha256]))

(defn manifest-source-pin [entries]
  (let [body {:schema manifest-schema :entries (mapv record-entry entries)}]
    {:path "wm://run-record-manifest-v1" :sha256 (field/digest body)}))

(defn- refusal [reason details]
  {:schema schema :status :refused :reason reason :details details})

(defn manifest
  "Pin an explicitly enumerated sequence of run-record paths."
  ([paths] (manifest paths #(java.nio.file.Files/readAllBytes (.toPath (io/file %)))))
  ([paths read-bytes]
   (let [entries (mapv (fn [path] {:path path :sha256 (field/sha256 (read-bytes path))}) paths)
         body {:schema manifest-schema :entries entries}]
     (assoc body :source-pin (manifest-source-pin entries)))))

(defn- manifest-check [m expected-pin]
  (let [body {:schema (:schema m) :entries (mapv record-entry (:entries m))}
        paths (map :path (:entries m))]
    (cond
      (not= expected-pin (:source-pin m)) :external-manifest-pin-mismatch
      (not= (:sha256 expected-pin) (field/digest body)) :manifest-content-drift
      (not= manifest-schema (:schema m)) :manifest-schema-invalid
      (not= (count paths) (count (set paths))) :manifest-paths-duplicated
      :else nil)))

(defn projection
  "Retain exactly the run-record fields consumed by the outcome dataset and
  its policy-provider adapter. RECORD-PIN is the verified source identity."
  [record record-pin]
  (let [outer (:outer-task-selection record)
        feedback (:cascade-feedback record)]
    (merge {:schema projection-schema :record-pin record-pin}
         (select-keys record [:run/id :click/id :startedAt :traceWritten])
         (when (contains? record :failure)
           {:failure (select-keys (:failure record) [:kind :stage])})
         (when (contains? record :outer-task-selection)
           {:outer-task-selection
            (merge (select-keys outer [:schema :status :reason])
                   {:support (if (vector? (:support outer)) [] (:support outer))
                    :excluded (if (vector? (:excluded outer)) [] (:excluded outer))
                    :draw (if (map? (:draw outer)) {} (:draw outer))
                   :chosen {:id (get-in outer [:chosen :id])}
                    :action (select-keys (:action outer) [:target])}
                   (when-let [policy (:policy outer)]
                     {:policy
                      (merge (select-keys policy [:kind])
                             {:selected-outer-cascade-policy
                              {:cascade-digest
                               (get-in policy [:selected-outer-cascade-policy
                                               :cascade-digest])}})}))})
         (when (contains? record :terminal-receipt)
           {:terminal-receipt (select-keys (:terminal-receipt record)
                                           [:outcome :failure-kind :failure-stage])})
         (when (contains? record :run-output)
           {:run-output (select-keys (:run-output record) [:outcome])})
         (when (contains? record :terminal)
           {:terminal (select-keys (:terminal record) [:outcome])})
         (when (contains? record :registered-run/timing)
           {:registered-run/timing
            (select-keys (:registered-run/timing record) [:status :wall-clock-ms])})
         (when (contains? record :registered-run/model-usage)
           {:registered-run/model-usage
            (let [usage (:registered-run/model-usage record)]
              (assoc (select-keys usage [:status])
                     :jobs (mapv #(select-keys % [:total-tokens :input-tokens :output-tokens])
                                 (:jobs usage))))})
         (when (contains? record :d-task-enactment)
           {:d-task-enactment (select-keys (:d-task-enactment record) [:target])})
         (when (contains? record :cascade-feedback)
           {:cascade-feedback
            (merge (select-keys feedback [:status :artifact :blocker :patterns])
                   (when-let [outcomes (get-in feedback [:mission-state :execution-outcomes])]
                     {:mission-state {:execution-outcomes outcomes}}))})
         (when-let [decision (:decision record)]
           {:decision
            (merge (select-keys decision [:selected-enacted-identity])
                   {:selection-law
                    {:per-policy-argmax
                     {:action {:target (get-in decision [:selection-law :per-policy-argmax
                                                         :action :target])}}}
                    :selection-certificate
                    {:selected-canonical-cascade
                     {:digest (get-in decision [:selection-certificate
                                                :selected-canonical-cascade :digest])}}})}))))

(defn- read-input [read-bytes source]
  (let [record-source (record-entry source)
        projection-source (:projection source)
        chosen (or projection-source record-source)
        {read-status :status bytes :bytes read-reason :reason
         read-details :details} (read-record-bytes read-bytes (:path chosen))
        actual (when (= :readable read-status) (field/sha256 bytes))]
    (cond
      (= :absent read-status)
      {:status :excluded :exclusion (cond-> {:source record-source
                                             :reason (if projection-source
                                                       (keyword (str "projection-" (name read-reason)))
                                                       read-reason)}
                                      read-details (assoc :details read-details))}
      (not= (:sha256 chosen) actual)
      {:status :excluded :exclusion {:source record-source
                                     :reason (if projection-source
                                               :projection-source-drift
                                               :record-source-drift)
                                     :expected (:sha256 chosen) :actual actual}}
      :else
      (let [value (try (edn/read-string (String. ^bytes bytes "UTF-8"))
                       (catch Exception _ ::invalid))]
        (cond
          (= ::invalid value)
          {:status :excluded :exclusion {:source record-source
                                         :reason (if projection-source
                                                   :projection-invalid-edn
                                                   :record-invalid-edn)}}
          (and projection-source
               (or (not= projection-schema (:schema value))
                   (not= record-source (:record-pin value))))
          {:status :excluded :exclusion {:source record-source
                                         :reason (if (not= projection-schema (:schema value))
                                                   :projection-schema-invalid
                                                   :projection-record-pin-mismatch)
                                         :expected record-source
                                         :actual (:record-pin value)}}
          :else
          {:status :admitted
           :record (if projection-source value (projection value record-source))})))))

(defn- target-kind [target]
  (cond
    (str/starts-with? (str target) "M-") :mission
    (str/starts-with? (str target) "E-") :excursion
    (str/starts-with? (str target) "T-") :ticket
    :else nil))

(defn- read-record-bytes
  "Classify a manifest read without making one unavailable record authoritative
  over its verified neighbours.  The source pin remains the original manifest
  claim; this function never derives a replacement pin from changed bytes."
  [read-bytes path]
  (try
    {:status :readable :bytes (read-bytes path)}
    (catch java.nio.file.NoSuchFileException t
      {:status :absent :reason :record-missing
       :details {:error-class (.getName (class t))}})
    (catch java.io.FileNotFoundException t
      {:status :absent :reason :record-missing
       :details {:error-class (.getName (class t))}})
    (catch Throwable t
      {:status :absent :reason :record-unreadable
       :details {:error-class (.getName (class t))}})))

(defn- canonical-outer-receipt? [receipt]
  (and (= :wm/outer-task-selection-v1 (:schema receipt))
       (map? (:policy receipt))
       (vector? (:support receipt))
       (vector? (:excluded receipt))
       (map? (:draw receipt))
       (string? (get-in receipt [:chosen :id]))
       (= (get-in receipt [:chosen :id]) (get-in receipt [:action :target]))))

(defn- selected-target [record]
  (let [receipt (:outer-task-selection record)]
    (if (canonical-outer-receipt? receipt)
      {:id (get-in receipt [:chosen :id]) :source :canonical-outer}
      (when-let [target (get-in record [:decision :selection-law
                                        :per-policy-argmax :action :target])]
        {:id target :source :legacy-inner-fallback}))))

(defn- raw-outcome [record]
  (or (get-in record [:terminal-receipt :outcome])
      (get-in record [:run-output :outcome])
      (get-in record [:terminal :outcome])))

(defn outcome-class [outcome]
  (cond
    (contains? #{:grounded-change :already-satisfied} outcome) :closure
    (= :grounded-progress outcome) :grounded-progress
    (contains? #{:guardrail-refusal :substrate-unavailable :agent-unavailable
                 :dispatch-failed :historical-verification-awaiting-validation
                 :historical-verification-refused} outcome) :typed-blocker
    (contains? #{:abstained :no-selection :build-failed :incomplete :cancelled
                 :grounded-no-change :artifact-only} outcome) :abstention-or-failure
    :else :typed-unknown))

(def ^:private apparatus-failure-kinds
  #{:resume-protocol-violation :phase-failure :dispatch-failed
    :transport-failure :build-failed :cancelled :substrate-unavailable
    :agent-unavailable :occurrence-square-redecision-failed})

(def ^:private apparatus-failure-stages
  #{:cascade-resume :dispatch :transport :build :occurrence-square-redecision})

(def ^:private task-outcome-kinds
  #{:task-failure :typed-blocker :guardrail-refusal :abstained
    :grounded-no-change :artifact-only})

(defn terminal-cause
  "The one terminal-evidence cause boundary used by the outer learner.
  Outcome labels alone never turn incomplete/build failures into task evidence."
  [record]
  (let [outcome (raw-outcome record)
        failure (:failure record)
        kind (or (:kind failure) (get-in record [:terminal-receipt :failure-kind]))
        stage (or (:stage failure) (get-in record [:terminal-receipt :failure-stage]))
        detail (or (:detail failure) (get-in record [:terminal-receipt :failure-detail]))
        base {:schema :wm/terminal-cause-v1 :outcome outcome
              :kind kind :stage stage :detail detail}]
    (cond
      (or (contains? apparatus-failure-kinds kind)
          (contains? apparatus-failure-stages stage))
      (assoc base :class :apparatus-failure :reason :typed-apparatus-failure)

      (= :untyped-failure kind)
      (assoc base :class :unclassified :reason :untyped-terminal-failure)

      (contains? #{:grounded-change :already-satisfied :grounded-progress} outcome)
      (assoc base :class :task-outcome :reason :witnessed-task-outcome)

      (and (contains? task-outcome-kinds kind)
           (or (contains? #{:abstained :grounded-no-change :artifact-only} outcome)
               (contains? #{:task-failure :typed-blocker :guardrail-refusal} kind)))
      (assoc base :class :task-outcome :reason :typed-task-outcome)

      (contains? #{:incomplete :build-failed :cancelled} outcome)
      (assoc base :class :unclassified :reason :terminal-label-not-causal-evidence)

      :else
      (assoc base :class :unclassified :reason :terminal-cause-unclassified))))

(defn- interoceptive-evidence [record source selection cause]
  (when (contains? #{:apparatus-failure :unclassified} (:class cause))
    (let [body {:schema :wm/r20-terminal-evidence-v1
                :run/id (:run/id record) :click/id (:click/id record)
                :selected-target (:id selection)
                :cause (select-keys cause [:class :reason :kind :stage :outcome :detail])
                :record-reference source}]
      (assoc body :digest (field/digest body)))))

(defn- typed-value [value unit source]
  (if (some? value)
    {:status :present :value value :unit unit :source source}
    {:status :absent :reason :not-explicitly-recorded}))

(defn- timing [record]
  (let [t (:registered-run/timing record)]
    (if (and (= :complete (:status t)) (pos-int? (:wall-clock-ms t)))
      (typed-value (:wall-clock-ms t) :milliseconds
                   [:registered-run/timing :wall-clock-ms])
      (typed-value nil nil nil))))

(defn- token-use [record]
  (let [u (:registered-run/model-usage record)
        jobs (:jobs u)]
    (if (and (= :complete (:status u)) (vector? jobs)
             (every? #(and (nat-int? (:total-tokens %))
                           (= (:total-tokens %)
                              (+ (or (:input-tokens %) 0) (or (:output-tokens %) 0)))) jobs))
      (typed-value (reduce + 0 (map :total-tokens jobs)) :tokens
                   [:registered-run/model-usage :jobs :total-tokens])
      (typed-value nil nil nil))))

(defn- quality [record selection]
  {:outer-selection-retained (= :canonical-outer (:source selection))
   :selected-identity-retained (boolean selection)
   :selected-identity-class (or (:source selection) :absent)
   :enacted-identity-retained
   (boolean (or (get-in record [:d-task-enactment :target])
                (get-in record [:decision :selected-enacted-identity])))
   :terminal-output-retained (contains? record :run-output)
   :trace-written (true? (:traceWritten record))})

(defn- selected-want-effects [record]
  (let [accounting (get-in record [:cascade-feedback :mission-state
                                   :execution-outcomes])
        by-class (:by-class accounting)]
    (if (= :verified (:status accounting))
      {:status :present
       :source [:cascade-feedback :mission-state :execution-outcomes]
       :receipt/id (:receipt/id accounting)
       :counts (into (sorted-map)
                     (map (fn [class]
                            [class (count (get by-class class []))]))
                     [:reached :progressed :blocked :untouched])
       :selected-wants (vec (:selected-wants accounting))}
      {:status :absent
       :reason (if (map? accounting)
                 :selected-want-accounting-not-verified
                 :selected-want-accounting-not-recorded)
       :source [:cascade-feedback :mission-state :execution-outcomes]})))

(defn- grounded-artifact [record]
  (let [artifact (get-in record [:cascade-feedback :artifact])]
    (if (and (map? artifact) (string? (:repo artifact))
             (string? (:commit artifact)) (true? (:grounded? artifact)))
      {:status :present
       :source [:cascade-feedback :artifact]
       :repo (:repo artifact) :commit (:commit artifact)}
      {:status :absent
       :reason (if (map? artifact)
                 :grounded-artifact-not-established
                 :artifact-not-recorded)
       :source [:cascade-feedback :artifact]})))

(defn- blocker [record]
  (let [failure (:failure record)
        cascade-blocker (get-in record [:cascade-feedback :blocker])]
    (cond
      (and (map? failure) (some? (:kind failure)))
      {:status :present :kind (:kind failure) :stage (:stage failure)
       :source [:failure]}

      (= :present (:status cascade-blocker))
      {:status :present :kind (:kind cascade-blocker)
       :stage (:stage cascade-blocker)
       :source [:cascade-feedback :blocker]}

      :else
      {:status :absent :reason :no-typed-blocker-recorded})))

(defn- pattern-reinforcement [record]
  (let [feedback (:cascade-feedback record)
        applications (get-in feedback [:patterns :applications])]
    (if (= :verified (:status feedback))
      {:status :present
       :source [:cascade-feedback :patterns]
       :selected (count (get-in feedback [:patterns :selected] []))
       :positive (count (get-in feedback [:patterns :positive-reinforcement] []))
       :no-want-effect (count (filter #(= :no-want-effect (:status %))
                                      applications))}
      {:status :absent :reason :verified-pattern-feedback-not-recorded})))

(defn- collateral-progress [outcome want-effects artifact]
  (let [counts (:counts want-effects)
        no-selected-effect? (and (= :present (:status want-effects))
                                 (zero? (+ (get counts :reached 0)
                                           (get counts :progressed 0))))]
    (if (and (= :grounded-progress outcome)
             no-selected-effect?
             (= :present (:status artifact)))
      {:status :present
       :kind :grounded-collateral-progress
       :source [[:terminal-receipt :outcome]
                [:cascade-feedback :mission-state :execution-outcomes]
                [:cascade-feedback :artifact]]}
      {:status :absent
       :reason (cond
                 (not= :grounded-progress outcome) :terminal-class-not-grounded-progress
                 (not= :present (:status want-effects)) :selected-want-effects-unknown
                 (not no-selected-effect?) :selected-want-effect-recorded
                 :else :grounded-artifact-not-established)})))

(defn produce
  "Read each manifest entry once and partition it into a usable row or typed
  exclusion. Repetition chronology is derived from canonical start instants."
  [{:keys [manifest expected-manifest-pin read-bytes]
    :or {read-bytes #(java.nio.file.Files/readAllBytes (.toPath (io/file %)))}}]
  (if-let [bad (manifest-check manifest expected-manifest-pin)]
    (refusal bad {})
    (loop [entries (:entries manifest) rows [] exclusions [] interoception []]
      (if-let [entry (first entries)]
        (let [source (record-entry entry)
              input (read-input read-bytes entry)]
          (if (= :excluded (:status input))
            (recur (next entries) rows (conj exclusions (:exclusion input)) interoception)
            (let [record (:record input)
                  run-id (:run/id record) click-id (:click/id record)
                      selection (selected-target record)
                      cause (terminal-cause record)
                      intero (interoceptive-evidence record source selection cause)
                      interoception (cond-> interoception intero (conj intero))
                      target (:id selection) kind (target-kind target)
                      started-at (when (string? (:startedAt record))
                                   (try (Instant/parse (:startedAt record))
                                        (catch Throwable _ nil)))]
                  (if-not (and (string? run-id) (string? click-id) kind)
                    (recur (next entries) rows
                           (conj exclusions
                                 {:source source
                                  :reason (cond
                                            (not (string? run-id)) :run-identity-missing
                                            (not (string? click-id)) :click-identity-missing
                                            (nil? target) :outer-task-identity-missing
                                            :else :outer-task-kind-invalid)
                                  :evidence {:run/id run-id :click/id click-id
                                             :selected-target target}})
                           interoception)
                    (if-not started-at
                      (recur (next entries) rows
                             (conj exclusions
                                   {:source source
                                    :reason (if (nil? (:startedAt record))
                                              :start-instant-missing
                                              :start-instant-malformed)
                                    :evidence {:run/id run-id :click/id click-id
                                               :startedAt (:startedAt record)}})
                             interoception)
                      (let [outcome (raw-outcome record)
                            want-effects (selected-want-effects record)
                            artifact (grounded-artifact record)
                            row {:source source :run/id run-id :click/id click-id
                               ;; The parsed record is retained only after its manifest bytes
                               ;; have verified.  Consumers must use this value rather than
                               ;; rereading the path (which could have disappeared or drifted).
                               :verified-record record
                               :task {:id target :kind kind}
                               :started-at {:status :present
                                            :value (.toString started-at)
                                            :unit :utc-instant
                                            :source [:startedAt]}
                               :terminal-outcome {:raw (or outcome {:absent :not-recorded})
                                                  :class (outcome-class outcome)
                                                  :source (cond
                                                            (get-in record [:terminal-receipt :outcome]) [:terminal-receipt :outcome]
                                                            (get-in record [:run-output :outcome]) [:run-output :outcome]
                                                            (get-in record [:terminal :outcome]) [:terminal :outcome]
                                                            :else {:absent :not-recorded})}
                               :terminal-cause cause
                               :elapsed (timing record) :token-use (token-use record)
                               :selected-want-effects want-effects
                               :grounded-artifact artifact
                               :collateral-progress
                               (collateral-progress outcome want-effects artifact)
                               :blocker (blocker record)
                               :pattern-reinforcement (pattern-reinforcement record)
                               :quality (quality record selection)
                               :projection-evidence
                               {:target-source (if (= :canonical-outer (:source selection))
                                                 [:outer-task-selection :chosen :id]
                                                 [:decision :selection-law :per-policy-argmax :action :target])}}]
                        (recur (next entries) (conj rows row) exclusions interoception)))))))
        (let [duplicate-identities (->> rows
                                        (group-by (juxt :run/id :click/id))
                                        (keep (fn [[identity matches]]
                                                (when (> (count matches) 1) identity)))
                                        vec)
              duplicate-starts (->> rows
                                    (group-by #(get-in % [:started-at :value]))
                                    (keep (fn [[instant matches]]
                                            (when (> (count matches) 1) instant)))
                                    vec)
              rows (->> rows
                        (sort-by #(get-in % [:started-at :value]))
                        (reduce (fn [{:keys [rows occurrences]} row]
                                  (let [target (get-in row [:task :id])
                                        prior (get occurrences target 0)]
                                    {:rows (conj rows (assoc row :repetition
                                                            {:prior-occurrences prior
                                                             :position (inc prior)}))
                                     :occurrences (update occurrences target (fnil inc 0))}))
                                {:rows [] :occurrences {}})
                        :rows)
              body {:schema schema :status :produced
                    :manifest-source-pin expected-manifest-pin
                    :rows rows :exclusions exclusions
                    :interoceptive-evidence interoception
                    :counts {:inputs (count (:entries manifest))
                             :usable (count rows) :excluded (count exclusions)
                             :task-kinds (into (sorted-map) (frequencies (map #(get-in % [:task :kind]) rows)))
                             :terminal-classes (into (sorted-map) (frequencies (map #(get-in % [:terminal-outcome :class]) rows)))
                             :availability
                             {:elapsed-ms (count (filter #(= :present (get-in % [:elapsed :status])) rows))
                              :token-use (count (filter #(= :present (get-in % [:token-use :status])) rows))
                              :closure-or-progress (count (filter #(contains? #{:closure :grounded-progress}
                                                                            (get-in % [:terminal-outcome :class])) rows))
                              :repetition (count (filter #(pos? (get-in % [:repetition :prior-occurrences])) rows))}
                             :record-read
                             {:declared (count (:entries manifest))
                              :readable (- (count (:entries manifest))
                                           (count (filter #(contains? #{:record-missing :record-unreadable}
                                                                      (:reason %)) exclusions)))
                              :verified (count rows)
                              :excluded (count exclusions)
                              :missing (count (filter #(= :record-missing (:reason %)) exclusions))
                              :drifted (count (filter #(= :record-source-drift (:reason %)) exclusions))
                              :reasons (into (sorted-map) (frequencies (map :reason exclusions)))}}}]
          (cond
            (seq duplicate-identities)
            (refusal :record-identities-duplicated
                     {:identities duplicate-identities})
            (seq duplicate-starts)
            (refusal :record-start-instants-duplicated
                     {:instants duplicate-starts})
            :else
            (assoc body :source-pin {:path "wm://meta-outcome-dataset-v1"
                                     :sha256 (field/digest body)})))))))

(defn verify
  ([_dataset] (refusal :external-run-record-authority-required {}))
  ([dataset authority]
   (let [expected (produce authority)]
     (if (= expected dataset)
       {:schema schema :status :verified :source-pin (:source-pin dataset)}
       (refusal :dataset-does-not-match-run-record-authority
                {:expected expected :actual dataset})))))
