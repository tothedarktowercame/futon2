(ns futon2.aif.meta-terminal-injury
  "Total terminal-record classification and pure integration into the META
  injury ledger. Injury is the default; only enumerated typed reasons are
  non-injuries. No failure-stage allowlist exists here."
  (:require [clojure.string :as str]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.meta-injury-ledger :as ledger]
            [futon2.aif.stop-line :as stop-line]))

(def classification-schema :wm/meta-terminal-classification-v1)
(def explicit-non-injury-kinds
  {:author-response-unparseable :author-response-unparseable
   :reviewed-claim-refused :reviewed-claim-refused
   :already-satisfied-wants-unmeasured :already-satisfied-wants-unmeasured
   ;; AlreadySatisfiedReview.lean rev 2: `nonInjury .claimNotOnTarget = true`.
   :claimed-artifact-not-on-selected-target :claimed-artifact-not-on-selected-target
   :abstained :ordinary-selection-abstention
   :operator-cancelled :operator-cancelled
   :declared-no-work :declared-no-work
   :diagnostic-only :diagnostic-only
   ;; Typed guard refusals.  The guards did their job; the attempt closes
   ;; :guardrail-refusal and there is nothing to repair.  Run 0ba3370c
   ;; (injury a67d3e49, ordinal 133) is the retained counterexample: the
   ;; duplicate-effective-run guard refused correctly and the terminal
   ;; classifier still opened an injury because no non-injury kind existed.
   :outer/duplicate-effective-run :duplicate-effective-run
   :outer/unchanged-refused-repeat :unchanged-refused-repeat})
(def successful-outcomes #{:success :closed :grounded-change :cohort-complete})
(def close-outcome-image
  "Transcribes `DarkTower/WarMachine/CloseOutcomeImage.lean` `image`,
  mathlib4 `229c56aabf`."
  {:grounded-change :changed
   :grounded-progress :changed
   :already-satisfied :already-satisfied
   :grounded-no-change :already-satisfied
   :artifact-only :invalid
   :abstained :invalid
   :no-selection :invalid
   :agent-unavailable :invalid
   :guardrail-refusal :refused
   :dispatch-failed :invalid
   :build-failed :invalid
   :substrate-unavailable :invalid
   :incomplete :invalid
   :cancelled :invalid
   :historical-verification-awaiting-validation :invalid
   :historical-verification-refused :invalid})
(def successful-images #{:changed :already-satisfied})
(def pin-re #"[0-9a-f]{64}")

(defn- nonblank? [x] (and (string? x) (not (str/blank? x))))
(defn- pin? [x]
  (and (map? x) (nonblank? (:path x))
       (string? (:sha256 x)) (boolean (re-matches pin-re (:sha256 x)))))
(defn- refusal [reason & [details]]
  (merge {:status :refused :reason reason} details))
(defn- retained-phase [record]
  (or (get-in record [:q6 :terminal :outcome :stage])
      (get-in record [:q6 :terminal :outcome :phase])
      (get-in record [:terminal-receipt :failure-stage])
      (get-in record [:terminal-receipt :phase])
      (get-in record [:run-output :failure-stage])
      (get-in record [:run-output :phase])
      (get-in record [:registered-run/chronology :last-phase])
      (:last-phase record)))

(defn- retained-q6-failure
  "The typed terminal condition retained by Q6 when the close map failed to
  populate :failure.  Q6 is part of the pinned run record, and this projection
  accepts any typed refused/incomplete outcome."
  [record]
  (let [terminal (get-in record [:q6 :terminal])
        observed (:outcome terminal)]
    (when (and (= :wm/q6-terminal-v1 (:schema terminal))
               (= (or (:run/id record) (:run-id record)) (:run/id terminal))
               (= :present (:status observed))
               (contains? #{:refused :incomplete} (:outcome observed))
               (keyword? (:kind observed)))
      {:kind (:kind observed)
       :stage (or (retained-phase record)
                  (when (= :hot-load-failed (:kind observed))
                    :cascade-resume)
                  :unknown-stage)})))

(defn- retained-terminal-outcome [record]
  (or (get-in record [:terminal-receipt :outcome])
      (get-in record [:failure :detail :outcome])
      (:outcome record)
      (get-in record [:q6 :terminal :outcome :outcome])))

(defn- failure-kind [record]
  (or (get-in record [:failure :kind])
      (get-in record [:failure :detail :failure-kind])
      (get-in record [:terminal-receipt :failure-kind])
      (:kind (retained-q6-failure record))
      (when (contains? #{:refused :incomplete}
                       (retained-terminal-outcome record))
        :unclassified-terminal-failure)))
(defn- failure-stage [record]
  (or (get-in record [:failure :stage])
      (get-in record [:failure :detail :failure-stage])
      (:stage (retained-q6-failure record))
      :unknown-stage))
(defn- run-id [record] (or (:run/id record) (:run-id record)))
(defn- click-id [record] (or (:click/id record) (:click-id record)))
(defn- outcome [record]
  (retained-terminal-outcome record))
(defn producer-qualified-already-satisfied? [record]
  ;; PROOF-2b's author-refusal producer establishes commit existence before
  ;; throwing this typed close. Classification trusts only the mutually
  ;; agreeing retained producer fields; it does not infer from author prose or
  ;; arbitrary metadata, and it does not re-run Git as a second authority.
  (let [failure (:failure record)
        detail (:detail failure)
        grounded (:grounded-commit detail)]
    (and (= :already-satisfied (:kind failure))
         (= :author-wait (:stage failure))
         (= :already-satisfied (:failure-kind detail))
         (= :author-wait (:failure-stage detail))
         (= :already-satisfied (:outcome detail))
         (= :already-satisfied (get-in record [:run-output :outcome]))
         (stop-line/producer-qualified-already-satisfied?
          {:outcome (:outcome detail)
           :failure-kind (:failure-kind detail)
           :grounded-commit grounded}))))

(defn finished-work?
  "True only when the existing terminal semantics establish finished work."
  [record]
  (let [image (close-outcome-image (outcome record))]
    (or (= :changed image)
        (and (= :already-satisfied image)
             (producer-qualified-already-satisfied? record)))))
(defn- target [record]
  (let [candidate (or (get-in record [:failure :target])
                      (get-in record [:failure :detail :target]))]
    (when (or (keyword? candidate) (nonblank? candidate)) candidate)))
(defn- cause-key [record]
  (or (get-in record [:failure :cause :cause-id])
      (get-in record [:failure :detail :cause-id])
      (let [kind (failure-kind record)]
        (if (= :unclassified-terminal-failure kind)
          [kind (run-id record)]
          [kind (failure-stage record) (target record)]))))
(def ^:private cause-fields [:reason :kind :failure-kind :absent])
(defn- supported-cause-entries [record]
  ;; P10 retains a bounded cause vector here.  The other entries are the
  ;; terminal refusal positions produced by the runner.  Deliberately do not
  ;; traverse a decision: arbitrary metadata is not a terminal cause.
  (concat (or (get-in record [:failure :causes :causes]) [])
          (or (get-in record [:failure :detail :refusals]) [])
          (keep identity
                [(get-in record [:failure :detail :outer-task-selection :policy])
                 (get-in record [:failure :detail :outer-task-selection :chosen])
                 (get-in record [:outer-task-selection :policy])
                 (get-in record [:outer-task-selection :chosen])])))
(defn- contains-cause? [record cause]
  (some (fn [entry]
          (and (map? entry)
               (some #{cause} (vals (select-keys entry cause-fields)))))
        (supported-cause-entries record)))
(defn- failed-checks [record]
  ;; Retain the falsifier receipt's failed-check identity when the terminal
  ;; record's failure detail carries the receipt. The record itself must name
  ;; the checks; classification never invents them.
  (let [checks (get-in record [:failure :detail :reviewer-falsifier
                               :failed-checks])]
    (when (and (vector? checks) (seq checks) (every? keyword? checks))
      checks)))
(defn- unconfigured-route? [record]
  (and (= :abstained (failure-kind record))
       (contains-cause? record :outer-policy-evidence-provider-required)))
(defn- evaluated-no-action? [record]
  ;; This names the scored terminal result, not an absence found while trying
  ;; to construct it. Do not generalize a missing score into this evidence.
  (and (= :abstained (failure-kind record))
       (contains-cause? record :policy-nondiscrimination)))
(defn- partial-frontier-continuation? [record]
  ;; A selected decision alongside its retained frontier proves that selection
  ;; continued. Counts alone do not establish a continuation.
  (and (= :abstained (failure-kind record))
       (= :selected (get-in record [:decision :status]))
       (map? (get-in record [:decision :selection-certificate :reading-frontier]))))
(defn- abstention-reason [record]
  (cond
    (unconfigured-route? record) nil
    (partial-frontier-continuation? record) :partial-frontier-continuation
    (evaluated-no-action? record) :evaluated-no-action
    :else :ordinary-selection-abstention))
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
          non-injury-reason (if (= :abstained kind)
                              (when-not (unconfigured-route? record)
                                (abstention-reason record))
                              (or (when (producer-qualified-already-satisfied? record)
                                    :producer-qualified-already-satisfied)
                                  (get explicit-non-injury-kinds kind)
                                  (when (and (nil? kind)
                                             (not= :already-satisfied terminal-outcome)
                                             (or (contains? successful-images
                                                            (close-outcome-image
                                                             terminal-outcome))
                                                 (contains? successful-outcomes
                                                            terminal-outcome)))
                                    :successful-task-outcome)
                                  (when (and (nil? kind) (nil? terminal-outcome))
                                    :absent-terminal-failure)))
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
           non-injury-reason (assoc :non-injury-reason non-injury-reason
                                    :observation-class
                                    (case non-injury-reason
                                      :evaluated-no-action :evaluated-no-action
                                      :partial-frontier-continuation :partial-frontier-continuation
                                      :ordinary-selection-abstention :ordinary-abstention
                                      :successful-task-outcome :successful-task-outcome
                                      :producer-qualified-already-satisfied
                                      :producer-qualified-already-satisfied
                                      :operator-cancelled :operator-cancelled
                                      :declared-no-work :declared-no-work
                                      :diagnostic-only :diagnostic-only
                                      :absent-terminal-failure :absent-terminal-failure
                                      :non-injury))
           (nil? non-injury-reason)
           (assoc :cause-id (cause-id record)
                  :injury-id (injury-id record)
                  :observation-class (if (unconfigured-route? record)
                                       :missing-production-machinery
                                       :terminal-failure)
                  :target (or (target record) {:absent :target-not-recorded}))
           (failed-checks record) (assoc :failed-checks (failed-checks record))))))
    (catch Throwable t
      (refusal :terminal-record/malformed {:detail (ex-message t)}))))

(defn- ledger-entries [history] (:entries history))
(defn- already-ingested? [history pin]
  (contains? (ledger/classification-pins history) pin))
(defn- latest-injury [history injury-id]
  (last (filter #(and (= ledger/transition-schema (:schema %))
                      (= injury-id (:injury-id %)))
                (ledger-entries history))))
(defn- enacted-for-retry [history click-id]
  (last (filter #(and (= ledger/transition-schema (:schema %))
                      (= :enacted (:state %))
                      (= click-id (:retry-click %)))
                (ledger-entries history))))

(defn- append-other-injury-retry-observation
  [appended classification enacted transition authority-verifier]
  (let [observed (ledger/append-observation
                  (:ledger appended) (:record-pin classification)
                  :retry-inconclusive
                  {:injury-id (:injury-id enacted)
                   :retry-click (:click-id classification)}
                  authority-verifier)]
    (if (= :appended (:status observed))
      (assoc observed :classification classification :transition transition
             :retry-of (select-keys enacted
                                    [:injury-id :repair-id :retry-click]))
      (assoc observed :classification classification))))

(defn- protocol-violation-family? [kind]
  (and (keyword? kind)
       (str/includes? (name kind) "protocol-violation")))

(defn- append-checked-medic-recurrence
  [history classification coverage authority-verifier signature-verifier]
  (if-not (protocol-violation-family? (:failure-kind classification))
    (ledger/append-medic-recurrence history (:injury-id classification)
                                    (:record-pin classification) authority-verifier)
    (case (try (signature-verifier {:classification classification
                                    :covered-operation (:operation coverage)
                                    :covered-entry (:entry coverage)})
               (catch Throwable _ :missing))
      :match (ledger/append-medic-recurrence
              history (:injury-id classification) (:record-pin classification)
              authority-verifier)
      :mismatch (refusal :medic-recurrence/cause-mismatch)
      (refusal :medic-recurrence/signature-required))))

(defn integrate
  "Pure deterministic ingestion. Non-injuries append inert observations.
  First injury occurrence opens; an enacted repair's attributable retry may
  reopen. Any other active occurrence is retained as an inert repeat audit."
  ([history classification]
   (integrate history classification (constantly false) (constantly :missing)))
  ([history classification authority-verifier]
   (integrate history classification authority-verifier (constantly :missing)))
  ([history classification authority-verifier signature-verifier]
  (try
    (cond
      (not= :valid (:status (ledger/validate-ledger history authority-verifier)))
      (refusal :ledger/tampered)

      (not= :valid (:status (verify-classification classification)))
      (refusal :classification/invalid)

      (already-ingested? history (:record-pin classification))
      {:status :duplicate :reason :record-already-ingested
       :ledger history :classification classification}

      (= :non-injury (:classification classification))
      (if-let [enacted (enacted-for-retry history (:click-id classification))]
        ;; DarkTower.WarMachine.SelfHealRoute.closing_dischargesOnlyOnSuccess:
        ;; these are exactly the two retry outcomes whose closing is discharged.
        (if (contains? #{:successful-task-outcome
                         :producer-qualified-already-satisfied}
                       (:non-injury-reason classification))
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
                        :result-record-pin (:record-pin classification)}
               authority-verifier)]
          (assoc appended :classification classification
                 :transition :discharged))
        (let [appended (ledger/append-observation
                        history (:record-pin classification) :retry-inconclusive
                        {:injury-id (:injury-id enacted)
                         :retry-click (:click-id classification)}
                        authority-verifier)]
          (assoc appended :classification classification :transition :retry-inconclusive
                 :retry-of (select-keys enacted [:injury-id :repair-id :retry-click]))))
        (let [appended (ledger/append-observation
                        history (:record-pin classification)
                        (:non-injury-reason classification) {}
                        authority-verifier)]
          (assoc appended :classification classification :transition :observation)))

      (= :injury (:classification classification))
      (let [previous (latest-injury history (:injury-id classification))
            enacted (enacted-for-retry history (:click-id classification))
            other-injury-retry? (and enacted
                                     (not= (:injury-id enacted)
                                           (:injury-id classification)))
            supported-origin? (#{:open :enacted :reopened} (:state previous))
            coverage (when supported-origin?
                       (ledger/medic-covered-latest
                        history (:injury-id classification) authority-verifier))
            medic-covered? (= :covered (:status coverage))]
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
                          :detected-click (:click-id classification)}
                 authority-verifier)]
            (if other-injury-retry?
              (append-other-injury-retry-observation
               appended classification enacted :open authority-verifier)
              (assoc appended :classification classification :transition :open)))

          (and (= :enacted (:state previous))
               (not other-injury-retry?))
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
                            :new-cause-pin (:record-pin classification)}
                   authority-verifier)]
              (assoc appended :classification classification :transition
                     (get-in appended [:entry :state])))
            (if medic-covered?
              (let [appended (append-checked-medic-recurrence
                              history classification coverage authority-verifier
                              signature-verifier)]
                (assoc appended :classification classification :transition
                       (get-in appended [:entry :state])))
              (refusal :injury/retry-click-mismatch
                       {:expected (:retry-click previous)
                        :actual (:click-id classification)})))

          (and (= :open (:state previous)) medic-covered?)
          (let [appended (append-checked-medic-recurrence
                          history classification coverage authority-verifier
                          signature-verifier)]
            (if other-injury-retry?
              (append-other-injury-retry-observation
               appended classification enacted (get-in appended [:entry :state])
               authority-verifier)
              (assoc appended :classification classification :transition
                     (get-in appended [:entry :state]))))

          (and medic-covered? (= :reopened (:state previous)))
          (let [appended (append-checked-medic-recurrence
                          history classification coverage authority-verifier
                          signature-verifier)]
            (assoc appended :classification classification :transition
                   (get-in appended [:entry :state])))

          (contains? ledger/active-states (:state previous))
          (let [appended (ledger/append-observation
                          history (:record-pin classification)
                          :repeated-active-injury
                          {:injury-id (:injury-id classification)}
                          authority-verifier)]
            (if other-injury-retry?
              (append-other-injury-retry-observation
               appended classification enacted :active-injury-retained
               authority-verifier)
              (assoc appended :classification classification
                     :transition :active-injury-retained)))

          :else
          (refusal :injury/terminal-history-requires-new-transaction
                   {:state (:state previous)})))

      :else (refusal :classification/unknown))
    (catch Throwable t
      (refusal :injury-integration/malformed {:detail (ex-message t)})))))

(defn replay [history classified-records]
  (reduce (fn [acc classification]
            (if (= :refused (:status acc))
              (reduced acc)
              (integrate (:ledger acc) classification)))
          {:status :seed :ledger history}
          classified-records))
