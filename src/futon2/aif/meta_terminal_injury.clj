(ns futon2.aif.meta-terminal-injury
  "Total terminal-record classification and pure integration into the META
  injury ledger. Injury is the default; only enumerated typed reasons are
  non-injuries. No failure-stage allowlist exists here."
  (:require [clojure.string :as str]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.meta-injury-ledger :as ledger]))

(def classification-schema :wm/meta-terminal-classification-v1)
(def explicit-non-injury-kinds
  {:abstained :ordinary-selection-abstention
   :operator-cancelled :operator-cancelled
   :declared-no-work :declared-no-work
   :diagnostic-only :diagnostic-only})
(def successful-outcomes #{:success :closed :grounded-change :cohort-complete})
(def pin-re #"[0-9a-f]{64}")

(defn- nonblank? [x] (and (string? x) (not (str/blank? x))))
(defn- pin? [x]
  (and (map? x) (nonblank? (:path x))
       (string? (:sha256 x)) (boolean (re-matches pin-re (:sha256 x)))))
(defn- refusal [reason & [details]]
  (merge {:status :refused :reason reason} details))
(defn- failure-kind [record]
  (or (get-in record [:failure :kind])
      (get-in record [:failure :detail :failure-kind])
      (get-in record [:terminal-receipt :failure-kind])))
(defn- failure-stage [record]
  (or (get-in record [:failure :stage])
      (get-in record [:failure :detail :failure-stage])
      :unknown-stage))
(defn- run-id [record] (or (:run/id record) (:run-id record)))
(defn- click-id [record] (or (:click/id record) (:click-id record)))
(defn- outcome [record]
  (or (get-in record [:terminal-receipt :outcome])
      (get-in record [:failure :detail :outcome])
      (:outcome record)))
(defn- target [record]
  (let [candidate (or (get-in record [:failure :target])
                      (get-in record [:failure :detail :target]))]
    (when (or (keyword? candidate) (nonblank? candidate)) candidate)))
(defn- cause-key [record]
  (or (get-in record [:failure :cause :cause-id])
      (get-in record [:failure :detail :cause-id])
      [(failure-kind record) (failure-stage record) (target record)]))
(defn- unconfigured-route? [record]
  (and (= :abstained (failure-kind record))
       (some (fn [node]
               (and (map? node)
                    (some #{:outer-policy-evidence-provider-required}
                          (vals (select-keys node [:reason :kind :failure-kind :absent])))))
             (tree-seq coll? seq
                       [(get-in record [:failure :detail :refusals])
                        (get-in record [:failure :detail :outer-task-selection])
                        (:outer-task-selection record)]))))
(defn- cause-id [record]
  (str "cause/" (identity/digest
                 (cond-> [:meta-terminal-cause (cause-key record)]
                   (unconfigured-route? record)
                   (conj :outer-policy-evidence-provider-required)))))
(defn- injury-id [record]
  (str "injury/" (subs (identity/digest
                        (cond-> [:meta-terminal-injury (cause-key record)]
                          (unconfigured-route? record)
                          (conj :outer-policy-evidence-provider-required))) 0 40)))
(defn- seal-classification [classification]
  (let [digest (identity/digest [:meta-terminal-classification classification])]
    (assoc classification :classification-pin
           {:path (str "meta-injury-classifications/" (:run-id classification) ".edn")
            :sha256 digest})))
(defn verify-classification [classification]
  (try
    (let [pin (:classification-pin classification)
          body (dissoc classification :classification-pin)
          expected (identity/digest [:meta-terminal-classification body])]
      (if (and (= classification-schema (:schema classification))
               (= :classified (:status classification))
               (pin? pin) (= expected (:sha256 pin)))
        {:status :valid}
        (refusal :classification/invalid)))
    (catch Throwable t
      (refusal :classification/malformed {:detail (ex-message t)}))))

(defn classify
  "Classify one terminal record with its immutable source pin. Apparatus,
  algorithm, malformed/unknown-stage, author, review and reload failures all
  take the default injury branch."
  [record source-pin]
  (try
    (let [kind (failure-kind record)
          stage (failure-stage record)
          run (run-id record)
          click (click-id record)
          terminal-outcome (outcome record)
          non-injury-reason (or (when-not (unconfigured-route? record)
                                 (get explicit-non-injury-kinds kind))
                                (when (and (nil? kind)
                                           (contains? successful-outcomes terminal-outcome))
                                  :successful-task-outcome)
                                (when (and (nil? kind) (nil? terminal-outcome))
                                  :absent-terminal-failure))
          defects (cond-> []
                    (not (map? record)) (conj :record/map)
                    (not (pin? source-pin)) (conj :record/source-pin)
                    (not (nonblank? run)) (conj :record/run-id)
                    (not (nonblank? click)) (conj :record/click-id))]
      (if (seq defects)
        (refusal :terminal-record/malformed {:defects defects})
        (seal-classification
         (cond-> {:schema classification-schema
                  :status :classified :classification (if non-injury-reason
                                                        :non-injury :injury)
                  :run-id run :click-id click :record-pin source-pin
                  :failure-kind (or kind :none) :failure-stage stage
                  :trace-written (boolean (:traceWritten record))
                  :run-output-present (boolean (:run-output record))}
           non-injury-reason (assoc :non-injury-reason non-injury-reason)
           (nil? non-injury-reason)
           (assoc :cause-id (cause-id record)
                  :injury-id (injury-id record)
                  :target (or (target record) {:absent :target-not-recorded}))))))
    (catch Throwable t
      (refusal :terminal-record/malformed {:detail (ex-message t)}))))

(defn- ledger-entries [history] (:entries history))
(defn- already-ingested? [history pin]
  (some (fn [entry]
          (or (= pin (:record-pin entry))
              (= pin (:cause-record-pin entry))
              (= pin (:new-cause-pin entry))))
        (ledger-entries history)))
(defn- latest-injury [history injury-id]
  (last (filter #(and (= ledger/transition-schema (:schema %))
                      (= injury-id (:injury-id %)))
                (ledger-entries history))))
(defn- enacted-for-retry [history click-id]
  (last (filter #(and (= ledger/transition-schema (:schema %))
                      (= :enacted (:state %))
                      (= click-id (:retry-click %)))
                (ledger-entries history))))

(defn integrate
  "Pure deterministic ingestion. Non-injuries append inert observations.
  First injury occurrence opens; an enacted repair's attributable retry may
  reopen. Any other active occurrence is retained as an inert repeat audit."
  [history classification]
  (try
    (cond
      (not= :valid (:status (ledger/validate-ledger history)))
      (refusal :ledger/tampered)

      (not= :valid (:status (verify-classification classification)))
      (refusal :classification/invalid)

      (already-ingested? history (:record-pin classification))
      {:status :duplicate :reason :record-already-ingested
       :ledger history :classification classification}

      (= :non-injury (:classification classification))
      (if-let [enacted (enacted-for-retry history (:click-id classification))]
        (let [authored (last (filter #(and (= ledger/transition-schema (:schema %))
                                          (= (:injury-id enacted) (:injury-id %))
                                          (= :authored (:state %))
                                          (= (:attempt enacted) (:attempt %)))
                                    (ledger-entries history)))
              appended
              (ledger/append-transition
               history {:injury-id (:injury-id enacted)
                        :state :discharged
                        :attempt (:attempt enacted)
                        :repair-id (:repair-id enacted)
                        :patch-sha (:patch-sha authored)
                        :retry-click (:click-id classification)
                        :outcome :success
                        :result-record-pin (:record-pin classification)})]
          (assoc appended :classification classification
                 :transition :discharged))
        (let [appended (ledger/append-observation
                        history (:record-pin classification)
                        (:non-injury-reason classification))]
          (assoc appended :classification classification :transition :observation)))

      (= :injury (:classification classification))
      (let [previous (latest-injury history (:injury-id classification))]
        (cond
          (nil? previous)
          (let [appended
                (ledger/append-transition
                 history {:injury-id (:injury-id classification)
                          :state :open :attempt 1
                          :cause-id (:cause-id classification)
                          :cause-record-pin (:record-pin classification)
                          :failure-stage (:failure-stage classification)
                          :failure-kind (:failure-kind classification)
                          :detected-click (:click-id classification)})]
            (assoc appended :classification classification :transition :open))

          (= :enacted (:state previous))
          (if (= (:retry-click previous) (:click-id classification))
            (let [appended
                  (ledger/append-retry-result
                   history {:injury-id (:injury-id classification)
                            :repair-id (:repair-id previous)
                            :patch-sha
                            (:patch-sha
                             (last (filter #(and (= (:injury-id classification)
                                                   (:injury-id %))
                                                (= :authored (:state %)))
                                           (ledger-entries history))))
                            :retry-click (:click-id classification)
                            :new-cause-pin (:record-pin classification)})]
              (assoc appended :classification classification :transition
                     (get-in appended [:entry :state])))
            (refusal :injury/retry-click-mismatch
                     {:expected (:retry-click previous)
                      :actual (:click-id classification)}))

          (contains? ledger/active-states (:state previous))
          (let [appended (ledger/append-observation
                          history (:record-pin classification)
                          :repeated-active-injury)]
            (assoc appended :classification classification
                   :transition :active-injury-retained))

          :else
          (refusal :injury/terminal-history-requires-new-transaction
                   {:state (:state previous)})))

      :else (refusal :classification/unknown))
    (catch Throwable t
      (refusal :injury-integration/malformed {:detail (ex-message t)}))))

(defn replay [history classified-records]
  (reduce (fn [acc classification]
            (if (= :refused (:status acc))
              (reduced acc)
              (integrate (:ledger acc) classification)))
          {:status :seed :ledger history}
          classified-records))
