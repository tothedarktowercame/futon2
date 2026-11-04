(ns futon2.aif.wm.authored.req-27201b12
  "Authored operational clauses for held executable cascade
  bed58baddf2e6d2096e3f9d93248572d44299aacc07b722fa52f115f299322e7
  (phase :compile, requisition
  27201b126abf6bc3f15ec6fda2b9ed2b14f9c5f8ae24d97877c7c08bc452f3e1).

  Resolutions for the eight :operational-clause-owed holes owned by
  :wm/enactment-compiler.  Each decision is an operational clause routed
  through the single routable capability :wm/pattern-apply; the operation
  names the pattern application that repairs the hole, completion-check
  names the observable condition, and :consumes/:produces describe the
  transition.  No claim is made about holes outside this requisition."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["musn/fulab-report-block" 0]
   {:operation :apply-fulab-report-block-to-enactment-completion-record
    :capability :wm/pattern-apply
    :consumes :compile-phase-completion-without-report-block
    :produces :fulab-report-block-anchoring-applied-patterns
    :completion-check
    [:response-ends-with-populated-fulab-report-block
     "the compile-phase response carrying these clauses ends with a [FULAB-REPORT] block whose :applied, :action and :notes fields list the patterns actually applied, so MUSN can record pattern-use evidence anchored in the log"]}
   ["agency/identifier-separation" 1]
   {:operation :apply-identifier-separation-to-cascade-thread-addressing
    :capability :wm/pattern-apply
    :consumes :overloaded-thread-id-in-cascade-bookkeeping
    :produces :typed-identifier-vocabulary-in-cascade-state
    :completion-check
    [:cascade-state-uses-layer-typed-identifier-keys
     "cascade compile-phase state stores LLM continuity identifiers only under continuity keys and transport identifiers (forum thread, MUSN session, WS connection) only inside their transport-specific payloads, with no top-level overloaded thread-id; a grep over the touched state shape shows each identifier key names exactly one layer"]}
   ["agency/identifier-separation" 2]
   {:operation :apply-identifier-separation-to-resume-id-binding
    :capability :wm/pattern-apply
    :consumes :resume-id-clobbered-by-transport-id
    :produces :resume-id-bound-to-llm-continuity-only
    :completion-check
    [:resume-uses-llm-continuity-id-never-transport-id
     "resuming the cascade's compile phase binds the LLM resume id from the continuity store only; no forum-thread, MUSN-session or WS-connection id reaches the resume call, verified by a check that fails when a transport id is substituted"]}
   ["problems/r4-forward-model" 3]
   {:operation :apply-r4-forward-model-to-pure-dynamics-kernel
    :capability :wm/pattern-apply
    :consumes :two-divergent-implementations-of-same-dynamics
    :produces :one-pure-deterministic-dynamics-kernel-shared
    :completion-check
    [:live-and-predicted-dynamics-call-one-kernel
     "the live enactment path and the predictor path both call a single pure deterministic (state, action) -> next-state kernel; the predictor adds uncertainty on top rather than reimplementing dynamics, verified by both call sites resolving to the same var"]}
   ["problems/r4-forward-model" 4]
   {:operation :apply-r4-forward-model-to-policy-conditioned-outcome-distribution
    :capability :wm/pattern-apply
    :consumes :channel-delta-forecast-used-as-outcome-distribution
    :produces :q-o-pi-constructed-from-belief-and-declared-models
    :completion-check
    [:outcome-distribution-is-policy-conditioned
     "candidate-action scoring uses Q(o|pi) = sum_s A(o|s) Q(s|pi) with state rolled forward from mu through B over depth T per the equation registry, so the evaluated object is a policy-conditioned outcome distribution from declared models, not a bare channel delta"]}
   ["problems/r4-forward-model" 5]
   {:operation :apply-r4-forward-model-to-pre-enactment-action-scoring
    :capability :wm/pattern-apply
    :consumes :candidate-actions-enacted-before-evaluation
    :produces :candidates-scored-before-enactment
    :completion-check
    [:scoring-precedes-enactment-in-control-order
     "every enacted candidate action in the compile-phase control flow passes through forward-model scoring first; an enactment reached without a recorded score fails the check"]}
   ["problems/r4-forward-model" 6]
   {:operation :apply-r4-forward-model-to-fidelity-maintenance
    :capability :wm/pattern-apply
    :consumes :untracked-divergence-risk-between-model-and-world
    :produces :declared-model-fidelity-residual-problem-records
    :completion-check
    [:residual-problems-of-forward-model-fidelity-recorded
     "the maintenance and fidelity residuals of the shared-kernel forward model (where predicted and live dynamics can drift, and what recalibration owes) are recorded on the R4 problem node rather than left implicit, so later work measures against the stated residuals"]}
   ["musn/fulab-report-block" 7]
   {:operation :apply-fulab-report-block-to-pattern-use-evidence
    :capability :wm/pattern-apply
    :consumes :pattern-use-not-anchored-in-log
    :produces :musn-parseable-pattern-use-record
    :completion-check
    [:report-block-parseable-by-musn-records-applied-patterns
     "the terminal report block of the compile phase is well-formed for MUSN parsing (:applied names the applied patterns, :action the repair, :notes qualifications), and its evidence-shape is {:applied :string, :action :string, :notes :string}"]}})

(defn decisions
  "The hole resolutions for this requisition, bound to the cascade digest
  they repair.  Returns the decision map keyed by hole id when the context
  names this cascade, else a typed mismatch (never a silent wrong answer)."
  [{:keys [cascade-digest phase] :as _ctx}]
  (if (and (= cascade-digest
              "bed58baddf2e6d2096e3f9d93248572d44299aacc07b722fa52f115f299322e7")
           (= :compile phase))
    clauses
    {:status :error
     :kind :patch-cascade-context-mismatch
     :cascade-digest cascade-digest
     :phase phase}))
