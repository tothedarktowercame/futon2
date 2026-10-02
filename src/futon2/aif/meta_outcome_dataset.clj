(ns futon2.aif.meta-outcome-dataset
  "Pure empirical META outcome rows from an externally pinned run manifest."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.meta-field-observation :as field]))

(def schema :wm/meta-outcome-dataset-v1)
(def manifest-schema :wm/run-record-manifest-v1)

(defn- refusal [reason details]
  {:schema schema :status :refused :reason reason :details details})

(defn manifest
  "Pin an explicitly enumerated sequence of run-record paths."
  ([paths] (manifest paths #(java.nio.file.Files/readAllBytes (.toPath (io/file %)))))
  ([paths read-bytes]
   (let [entries (mapv (fn [path] {:path path :sha256 (field/sha256 (read-bytes path))}) paths)
         body {:schema manifest-schema :entries entries}]
     (assoc body :source-pin {:path "wm://run-record-manifest-v1"
                              :sha256 (field/digest body)}))))

(defn- manifest-check [m expected-pin]
  (let [body (dissoc m :source-pin)
        paths (map :path (:entries m))]
    (cond
      (not= expected-pin (:source-pin m)) :external-manifest-pin-mismatch
      (not= (:sha256 expected-pin) (field/digest body)) :manifest-content-drift
      (not= manifest-schema (:schema m)) :manifest-schema-invalid
      (not= (count paths) (count (set paths))) :manifest-paths-duplicated
      :else nil)))

(defn- target-kind [target]
  (cond
    (str/starts-with? (str target) "M-") :mission
    (str/starts-with? (str target) "E-") :excursion
    (str/starts-with? (str target) "T-") :ticket
    :else nil))

(defn- selected-target [record]
  (or (get-in record [:outer-task-selection :chosen :id])
      (get-in record [:decision :selection-law :per-policy-argmax :action :target])))

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
                 :historical-verification-refused} outcome) :useful-typed-blocker
    (contains? #{:abstained :no-selection :build-failed :incomplete :cancelled
                 :grounded-no-change :artifact-only} outcome) :abstention-or-failure
    :else :typed-unknown))

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

(defn- quality [record target]
  {:outer-selection-retained (map? (:outer-task-selection record))
   :selected-identity-retained (boolean target)
   :enacted-identity-retained
   (boolean (or (get-in record [:d-task-enactment :target])
                (get-in record [:decision :selected-enacted-identity])))
   :terminal-output-retained (contains? record :run-output)
   :trace-written (true? (:traceWritten record))})

(defn produce
  "Read each manifest entry once and partition it into a usable row or typed
  exclusion. Manifest order is the repetition chronology."
  [{:keys [manifest expected-manifest-pin read-bytes]
    :or {read-bytes #(java.nio.file.Files/readAllBytes (.toPath (io/file %)))}}]
  (if-let [bad (manifest-check manifest expected-manifest-pin)]
    (refusal bad {})
    (loop [entries (:entries manifest) rows [] exclusions [] occurrences {}]
      (if-let [{:keys [path sha256] :as source} (first entries)]
        (let [bytes (try (read-bytes path) (catch Throwable _ nil))
              actual (when bytes (field/sha256 bytes))]
          (cond
            (nil? bytes)
            (recur (next entries) rows
                   (conj exclusions {:source source :reason :record-unreadable}) occurrences)
            (not= sha256 actual)
            (refusal :record-source-drift {:path path :expected sha256 :actual actual})
            :else
            (let [record (try (edn/read-string (String. ^bytes bytes "UTF-8"))
                              (catch Throwable _ ::invalid))]
              (if (= ::invalid record)
                (recur (next entries) rows
                       (conj exclusions {:source source :reason :record-invalid-edn}) occurrences)
                (let [run-id (:run/id record) click-id (:click/id record)
                      target (selected-target record) kind (target-kind target)]
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
                           occurrences)
                    (let [prior (get occurrences target 0)
                          outcome (raw-outcome record)
                          row {:source source :run/id run-id :click/id click-id
                               :task {:id target :kind kind}
                               :terminal-outcome {:raw (or outcome {:absent :not-recorded})
                                                  :class (outcome-class outcome)
                                                  :source (cond
                                                            (get-in record [:terminal-receipt :outcome]) [:terminal-receipt :outcome]
                                                            (get-in record [:run-output :outcome]) [:run-output :outcome]
                                                            (get-in record [:terminal :outcome]) [:terminal :outcome]
                                                            :else {:absent :not-recorded})}
                               :elapsed (timing record) :token-use (token-use record)
                               :quality (quality record target)
                               :repetition {:prior-occurrences prior :position (inc prior)}
                               :projection-evidence
                               {:target-source (if (get-in record [:outer-task-selection :chosen :id])
                                                 [:outer-task-selection :chosen :id]
                                                 [:decision :selection-law :per-policy-argmax :action :target])}}
                          occurrences (update occurrences target (fnil inc 0))]
                      (recur (next entries) (conj rows row) exclusions occurrences))))))))
        (let [duplicate-identities (->> rows
                                        (group-by (juxt :run/id :click/id))
                                        (keep (fn [[identity matches]]
                                                (when (> (count matches) 1) identity)))
                                        vec)
              body {:schema schema :status :produced
                    :manifest-source-pin expected-manifest-pin
                    :rows rows :exclusions exclusions
                    :counts {:inputs (count (:entries manifest))
                             :usable (count rows) :excluded (count exclusions)
                             :task-kinds (into (sorted-map) (frequencies (map #(get-in % [:task :kind]) rows)))
                             :terminal-classes (into (sorted-map) (frequencies (map #(get-in % [:terminal-outcome :class]) rows)))
                             :availability
                             {:elapsed-ms (count (filter #(= :present (get-in % [:elapsed :status])) rows))
                              :token-use (count (filter #(= :present (get-in % [:token-use :status])) rows))
                              :closure-or-progress (count (filter #(contains? #{:closure :grounded-progress}
                                                                            (get-in % [:terminal-outcome :class])) rows))
                              :repetition (count (filter #(pos? (get-in % [:repetition :prior-occurrences])) rows))}}}]
          (if (seq duplicate-identities)
            (refusal :record-identities-duplicated
                     {:identities duplicate-identities})
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
