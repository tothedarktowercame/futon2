(ns futon2.aif.repair-discharge
  "Two-phase runner discharge. Implementation A is not its own successor B.
   Store refusals are close results, not exceptions that reopen execution close.
   No dismissal capability is exposed here."
  (:require [clojure.java.io :as io]
            [futon2.aif.repair-discharge-evidence :as evidence]
            [futon2.aif.repair-discharge-receipt :as receipt]
            [futon2.aif.repair-obligation :as repair]
            [futon2.aif.repair-evaluators :as evaluators]))

(def supported-requirements
  #{:distinct-repair-commit :independent-review :grounded-repair
    :distinct-production-shaped-successor})

(defn bind-selected!
  "A prefix is not admission. T candidates must retain the native id, finding
   byte pin, verbatim contract and interpretation admission. Legacy explicit
   repair actions carry the finding itself and use the same authoritative read."
  [root action interpretation]
  (let [legacy? (#{:repair-machine-failure :revalidate-historical-repair} (:type action))
        id (if legacy? (get-in action [:repair-obligation :repair/id]) (:repair/id action))]
    (when (or id (and (string? (:target action)) (.startsWith ^String (:target action) "T-repair-")))
      (evidence/safe-id! id)
      (let [record (repair/discharge-record root "findings" id)
            finding (:value record)
            pin (:finding-source action)]
        (evidence/require! finding :finding-unavailable {:repair/id id})
        (if legacy?
          (evidence/require! (and (= id (:target action))
                                 (= (:discharge-contract finding)
                                    (get-in action [:repair-obligation :discharge-contract])))
                            :finding-binding-mismatch {:repair/id id})
          (evidence/require!
           (and (= (str "T-" id) (:target action))
                (map? interpretation) (seq interpretation)
                (every? #(and (map? %) (seq %)) (vals interpretation))
                (= (:sha256 record) (:sha256 pin))
                (= (.getCanonicalPath (io/file root "findings" (str id ".edn")))
                   (:path pin))
                (= (:discharge-contract finding) (:discharge-contract action)))
           :finding-admission-unestablished {:repair/id id}))
        {:finding finding :finding-record record :target (str "T-" id)}))))

(defn- implementation-close [context]
  (merge (select-keys (:close context) [:attempt/id :run/id :closed-at :grounded?])
         {:repair/id (:repair/id context) :commit (get-in context [:artifact :commit])
          :review-receipt-ids [(get-in context [:review-job :job-id])]
          :review-sha256 (evidence/sha256 (evidence/text-bytes
                                         (evidence/canonical-text (:review-job context))))}))

(defn- successor-close [context]
  (merge (select-keys (:close context) [:attempt/id :run/id :closed-at :grounded?])
         {:repair/id (:repair/id context) :production-shaped? true
          :witness-ref (get-in context [:intent :path])
          :witness-sha256 (get-in context [:intent :sha256])
          :witness (:witness context)}))

(defn- retain-intent! [root context]
  (let [operation (repair/record-discharge-operation! root :intent context)
        snapshot (evidence/snapshot (:path operation))]
    (evidence/require! (= context (get-in snapshot [:value :value])) :intent-readback-mismatch {})
    (assoc context :intent (dissoc snapshot :value))))

(defn finalize!
  "Called only after the execution close was durably written. Evidence/observers
   are supplied by runner-owned ports; user-selected actions do not supply
   callbacks. A crash hook belongs to the integration test boundary, after the
   store accepted resolution and before any publication."
  [{:keys [root repo action interpretation close artifact artifact-binding
           files author reviewer review-job read-job evaluators registry producer
           after-resolution-fn]
    :or {registry {}}}]
  (let [stage (atom :binding) accepted (atom []) id-state (atom nil)]
    (try
      (if-let [{:keys [finding finding-record]} (bind-selected! root action interpretation)]
        (let [id (:repair/id finding)
              _ (reset! id-state id)
              _ (reset! stage :review)
              actual-job (when (:job-id review-job) (read-job (:job-id review-job)))
              gate (evidence/review! files author reviewer actual-job)
              _ (evidence/require! (= (:job-id review-job) (:job-id actual-job)) :review-job-mismatch {})
              _ (reset! stage :eligibility)
              _ (evidence/require! (and (#{:machine-failure :independent-review-failure} (:repair/class finding))
                                        (= :code-commit (get-in finding [:discharge-contract :artifact-shape] :code-commit))
                                        (seq (get-in finding [:discharge-contract :requires]))
                                        (every? supported-requirements (get-in finding [:discharge-contract :requires])))
                                  :unsupported-discharge-contract {:repair/id id})
              _ (evidence/require! (and (true? (:grounded? close)) (:close-snapshot close)
                                        (string? (:attempt/id close)) (string? (:run/id close)))
                                  :durable-close-unavailable {})
              _ (reset! stage :observation)
              evaluator (evidence/admitted-evaluator! (get (or evaluators (evaluators/admissions)) (:failure-kind finding))
                                                     registry finding read-job)
              witness (evidence/observe! evaluator finding
                                         (select-keys finding-record [:path :sha256]) close artifact)
              implementation (repair/discharge-record root "implementations" id)
              resolution (repair/discharge-record root "resolutions" id)
              context {:schema :wm/repair-discharge-context-v1
                       :phase (if implementation :successor-validation :implementation)
                       :repair/id id :close close :artifact artifact :artifact-binding artifact-binding
                       :review-job actual-job :review-evidence gate :witness witness :producer producer}
              evidence {:attempt-id (:attempt/id close) :commit (:commit artifact)
                        :reviewer reviewer :review-job (:job-id actual-job) :review-evidence gate
                        :artifact-binding artifact-binding :witness witness
                        :repair/discharge-context context}]
          (cond
            resolution (receipt/publication-result! root repo id)
            (nil? implementation)
            (do (reset! stage :implementation)
                (repair/record-implementation! root finding
                                               (assoc evidence :repair/discharge-context
                                                      (retain-intent! root context)))
                {:status :awaiting-successor :repair/id id :phase :implementation
                 :repair/discharged? false :implementation-record (str "implementations/" id ".edn")})
            (= (:attempt/id close) (get-in implementation [:value :implementation-attempt]))
            (do
              (evidence/require! (= (select-keys context [:close :artifact :review-job :artifact-binding :repair/id :producer])
                                   (select-keys (get-in implementation [:value :repair/discharge-context])
                                                [:close :artifact :review-job :artifact-binding :repair/id :producer]))
                                :existing-implementation-conflict {:repair/id id})
              {:status :awaiting-successor :repair/id id :phase :implementation :repair/discharged? false})
            :else
            (let [previous (get-in implementation [:value :repair/discharge-context])]
              (reset! stage :successor-validation)
              (evidence/require! previous :implementation-context-unavailable {:repair/id id})
              (let [context (retain-intent! root context)]
               (repair/successor-resolution!
               {:obligation (assoc finding :repair/implementation (:value implementation))
                :repair-close (implementation-close previous)
                :successor-close (successor-close context)
                :authority {:decided-by reviewer :review-job (:job-id actual-job)}
                :resolution-read-fn #(some-> (repair/discharge-record root "resolutions" %) :value)
                :resolve-fn (fn [obligation value]
                              (repair/resolve! root obligation
                                               (assoc value :repair/discharge-context context)))}))
              (swap! accepted conj {:phase :successor-validation :record (str "resolutions/" id ".edn")})
              ;; Tests simulate process death with an Error; normal store
              ;; refusals remain Exceptions and are contained below.
              (when after-resolution-fn (after-resolution-fn))
              (receipt/publication-result! root repo id))))
        {:status :not-applicable :repair/discharged? false})
      (catch Exception e
        (let [reason (or (:repair-discharge/refusal (ex-data e))
                         (:repair-successor/refusal (ex-data e))
                         (:failure-kind (ex-data e)))]
          {:status (cond (= reason :review-not-approved) :review-not-approved
                         (#{:implementation :successor-validation} @stage) :store-refused
                         :else :evidence-unavailable)
           :repair/id @id-state :repair/discharged? false :stage @stage
           :reason (or reason :store-or-evidence-refused) :error (.getMessage e)
           :error-data-edn (pr-str (ex-data e)) :accepted @accepted})))))

(defn finalize-run!
  "Prepare close evidence from the immutable file written by the runner, then
   contain all discharge failures outside the execution close's retry path."
  [{:keys [root close-path closed-event] :as context}]
  (let [result
        (try
          (let [capture (when close-path (evidence/snapshot close-path))]
            (when capture
              (evidence/require! (= closed-event (:value capture)) :closed-event-changed {:path close-path}))
            (finalize! (assoc-in context [:close :close-snapshot] capture)))
          (catch Exception e
            {:status :evidence-unavailable :repair/discharged? false
             :reason (or (:repair-discharge/refusal (ex-data e)) :close-evidence-unavailable)
             :error (.getMessage e) :error-data-edn (pr-str (ex-data e))}))]
    (if (= :not-applicable (:status result)) result
        (try
          (assoc result :event (repair/record-discharge-operation!
                                root :outcome {:close (:close context) :result result}))
          (catch Exception e
            ;; Keep the original refusal and accepted side effects visible.
            ;; The enclosing terminal run record is still the durable result.
            (assoc result :event-persistence-error (.getMessage e)))))))
