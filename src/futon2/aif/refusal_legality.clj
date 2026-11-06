(ns futon2.aif.refusal-legality
  "Pure correspondence of `DarkTower/WarMachine/RefusalLegality.lean`.

  `loop-stop-legal?`, `case-checked?`, `click-legal?`, and `disposition`
  mirror the Lean definitions `loopStopLegal`, `caseChecked`, `clickLegal`,
  and `disposition`, respectively. Evidence field names intentionally match
  `LoopStopEvidence` and `ClickEvidence` exactly. C2 rechecks the retained
  source pin against its current bytes and the durable Morning Brief receipt;
  it does not infer evidence from a terminal keyword."
  (:require [clojure.java.io :as io]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.c2-question :as c2-question]
            [futon2.aif.harm-rules :as harm-rules]
            [futon2.aif.morning-brief :as morning-brief]))

(def ^:private harm-rule-ids
  (into #{} (map :id) (:rules (harm-rules/load-rules))))

(defn- retained-harm-match? [{:keys [rule declined-action declined-action-digest]}]
  (and (contains? harm-rule-ids rule)
       (map? declined-action)
       (string? declined-action-digest)
       (= declined-action-digest (identity/digest declined-action))))

(defn loop-stop-legal?
  [{:keys [kind scoredField operatorSignal]}]
  (case kind
    :work-exhausted (empty? scoredField)
    :operator-override (true? operatorSignal)
    false))

(defn case-checked?
  [{:keys [kind satisfactionMeasured ambiguityCited questionInBrief
           harmRuleMatched]}]
  (case kind
    :already-satisfied (true? satisfactionMeasured)
    :legitimate-question (and (true? ambiguityCited) (true? questionInBrief))
    :harm-declined (true? harmRuleMatched)
    false))

(defn click-legal?
  [{:keys [exclusionRecorded] :as evidence}]
  (and (case-checked? evidence) (true? exclusionRecorded)))

(defn disposition
  [{:keys [refusal evidence]}]
  (case refusal
    :stop (if (loop-stop-legal? evidence) :loop-stop :debugger-error)
    :terminal (if (click-legal? evidence) :click-terminal :debugger-error)
    :debugger-error))

(defn retained-scored-field
  "Project the retained outer scored field after this click's reading refusals.
  Mirrors `ReadingRefusalReselection.reducedField`: refused targets are removed
  from the unchanged admitted field. Missing carrier data remains nil."
  [opts]
  (when-let [admitted (get-in opts [:outer/selection :outer-field/admitted])]
    (let [refused (into #{} (keep :target)
                        (get-in opts [:outer/selection :reading-reselection :refusals]))]
      (into [] (remove refused) admitted))))

(def work-outcomes
  "Close outcomes that did work: the `:changed` image of
  `meta-terminal-injury/close-outcome-image`.  Everything else that reaches
  `close!` is validated, including outcomes outside the image: `outcome-from`
  passes any thrown `:outcome` through unchanged, so an open no-work set would
  let a new outcome skip validation as if it were work."
  #{:grounded-change :grounded-progress})

(def no-work-outcomes
  "The no-work part of `meta-terminal-injury/close-outcome-image`, named for
  readers and tests.  Validation does not consult it; see `work-outcomes`."
  #{:already-satisfied :grounded-no-change :artifact-only :abstained
    :no-selection :agent-unavailable :guardrail-refusal :harm-declined :dispatch-failed
    :build-failed :substrate-unavailable :incomplete :cancelled
    :historical-verification-awaiting-validation
    :historical-verification-refused})

(defn- c2-close-evidence [data]
  ;; The runner catches author exceptions and retains their ex-data under
  ;; :error-data before validated-close!; direct callers may supply that map.
  (let [retained (if (map? (:error-data data)) (:error-data data) data)
        checked (:c2/question retained)
        carrier (when (= :carrier (:status checked)) (:carrier checked))
        receipt (:c2/brief-receipt retained)
        source (get-in carrier [:ambiguity :source])
        ambiguity-cited?
        (boolean
         (when (and carrier (string? (:path source)) (string? (:sha256 source)))
           (try
             (c2-question/ambiguity-cited? carrier (slurp (io/file (:path source))))
             (catch Throwable _ false))))
        question-in-brief?
        (boolean
         (and carrier
              (= (:question-digest carrier) (:question-digest receipt))
              (morning-brief/c2-receipt-valid? receipt)))]
    {:refusal :terminal :case :C2
     :evidence {:kind :legitimate-question
                :ambiguityCited ambiguity-cited?
                :questionInBrief question-in-brief?
                :missionSourceShaPresent (string? (:sha256 source))
                :exclusionRecorded false}}))

(defn evidence-for-close
  "Build RefusalLegality evidence solely from retained close carriers.
  Returns nil only for `work-outcomes`. OPERATOR-SIGNAL is an independently retained
  proof, never inferred from a terminal keyword."
  [outcome data scored-field operator-signal]
  (when-not (contains? work-outcomes outcome)
    (cond
      (= :author-question outcome)
      (c2-close-evidence data)

      (contains? #{:already-satisfied :grounded-no-change} outcome)
      {:refusal :terminal :case :C1
       :evidence {:kind :already-satisfied
                  :satisfactionMeasured
                  (= :admitted (get-in data [:mission-observation :status]))
                  :missionSourceShaPresent
                  (string? (:mission-source-sha256 data))
                  :exclusionRecorded false}}

      (= :harm-declined outcome)
      {:refusal :terminal :case :C3
       :evidence {:kind :harm-declined
                  :harmRuleMatched (retained-harm-match? (:harm-match data))
                  :missionSourceShaPresent
                  (string? (:mission-source-sha256 data))
                  :exclusionRecorded false}}

      (and (= :abstained outcome) (= :empty-field (:failure-kind data)))
      (if (nil? scored-field)
        {:refusal :other :case :L1
         :evidence {:reason :scored-field-carrier-absent}}
        {:refusal :stop :case :L1
         :evidence {:kind :work-exhausted :scoredField scored-field
                    :operatorSignal false}})

      (and (= :debugger/aborted (:failure-kind data))
           (= :abort (:debugger/restart data))
           (true? operator-signal))
      {:refusal :stop :case :L2
       :evidence {:kind :operator-override :scoredField []
                  :operatorSignal true}}

      (and (= :operator-cancelled (:failure-kind data))
           (true? operator-signal))
      {:refusal :stop :case :L2
       :evidence {:kind :operator-override :scoredField []
                  :operatorSignal true}}

      :else {:refusal :other})))

(defn- retained-discharged-abort?
  "True when the close data carries the debugger's retained proof that an
  operator already chose abort for this failure at a phase stop. The
  :debugger/discharged? mark is minted only by the debugger's discharge path
  (the breakpoint cancellation already retains it); it is a retained proof,
  never inferred from a terminal keyword. Held run 2026-11-06-73839343
  (repair wm-click-loop-repair-b310a7df): a phase-level abort of an ordinary
  typed failure rethrew the failure without the mark, the close could not
  see the decided abort, and raised a second operator decision for the same
  discharged condition — the bounce that stoppable-failure?'s discharge
  contract ('must close the run, not bounce back into a second stop')
  forbids."
  [data]
  (or (true? (:debugger/discharged? data))
      (true? (get-in data [:error-data :debugger/discharged?]))))

(defn resolve-close-refusal
  "Validate a close before persistence. AWAIT-FN receives the illegal-refusal
  condition data and returns a debugger restart map. READ-SCORED-FIELD is a
  0-arity fn, called on every pass, so `:retry` after a live repair sees the
  repaired carrier rather than the one read before the hold. The returned
  outcome/data pair is ready for close persistence."
  [attached? await-fn outcome data read-scored-field operator-signal]
  (loop [outcome outcome, data data, prior nil]
    (if-let [refusal (evidence-for-close outcome data (read-scored-field) operator-signal)]
      (let [;; :pending-exclusion is a runner intermediate, not a fifth Lean
            ;; disposition. It means caseChecked holds while
            ;; exclusionRecorded is unknown until the finished record's
            ;; compact index line is published.
            d (if (and (contains? #{:C1 :C3} (:case refusal))
                       (case-checked? (:evidence refusal))
                       (true? (get-in refusal [:evidence :missionSourceShaPresent])))
                :pending-exclusion
                (disposition refusal))
            retained {:disposition d :case (:case refusal)
                      :evidence (:evidence refusal)}]
        (if (and attached? (= :debugger-error d)
                 (not (retained-discharged-abort? data)))
          (let [{:keys [action value]} (await-fn
                                        {:kind :illegal-refusal
                                         :original-kind (or (:failure-kind data) outcome)
                                         ;; Repair 1b5608db (held run 6d57248a):
                                         ;; a reading-refusal close held with
                                         ;; :evidence nil and no target or
                                         ;; error-data, so the operator page
                                         ;; could not say what refused.
                                         ;; D4-READING-REFUSAL-FINDING-CONTEXT
                                         ;; applies here too: the refusal's
                                         ;; typed context rides the condition.
                                         :original-failure
                                         (select-keys data
                                                      [:failure-stage :target
                                                       :error-data])
                                         :disposition retained
                                         :evidence (:evidence refusal)})]
            (case action
              :retry (recur outcome data prior)
              ;; A debugger-supplied value must not close looking like an
              ;; ordinary outcome: retain what it replaced.
              :use-value (recur (:outcome value)
                                (assoc (:data value)
                                       :refusal/restart :use-value
                                       :refusal/replaced
                                       (or prior {:outcome outcome
                                                  :disposition retained}))
                                (or prior {:outcome outcome
                                           :disposition retained}))
              :continue {:outcome outcome
                         :data (assoc data :refusal/disposition retained
                                           :refusal/restart :continue)}
              :abort {:outcome :cancelled
                      :data (assoc data :failure-kind :debugger/aborted
                                        :debugger/restart :abort
                                        :refusal/disposition retained
                                        :refusal/restart :abort)}
              (throw (ex-info "Unknown close-refusal restart" {:action action}))))
          ;; A discharged abort persists as the :continue restart would: the
          ;; operator decision was already made and retained at the phase
          ;; stop, so the close proceeds and records what it retained.
          (if (retained-discharged-abort? data)
            {:outcome outcome
             :data (assoc data :refusal/disposition retained
                               :refusal/restart :continue)}
            {:outcome outcome :data (assoc data :refusal/disposition retained)})))
      {:outcome outcome :data data})))
