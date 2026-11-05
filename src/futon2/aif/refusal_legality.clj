(ns futon2.aif.refusal-legality
  "Pure correspondence of `DarkTower/WarMachine/RefusalLegality.lean`.

  `loop-stop-legal?`, `case-checked?`, `click-legal?`, and `disposition`
  mirror the Lean definitions `loopStopLegal`, `caseChecked`, `clickLegal`,
  and `disposition`, respectively. Evidence field names intentionally match
  `LoopStopEvidence` and `ClickEvidence` exactly. This namespace performs no
  I/O and does not infer evidence from a terminal keyword.")

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
