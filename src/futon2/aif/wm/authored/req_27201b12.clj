(ns futon2.aif.wm.authored.req-27201b12
  "Authored operational clauses for held executable cascade
  bed58baddf2e6d2096e3f9d93248572d44299aacc07b722fa52f115f299322e7
  (phase :compile, requisition
  27201b126abf6bc3f15ec6fda2b9ed2b14f9c5f8ae24d97877c7c08bc452f3e1).

  Resolutions for the eight :operational-clause-owed holes, each grounded
  in the named library pattern body (musn/fulab-report-block,
  agency/identifier-separation, problems/r4-forward-model as pinned by
  the requisition's source pins).  The operation names what the pattern's
  THEN clause enacts, the completion-check names the pattern's own
  evidence condition, and :consumes/:produces describe the transition.
  No application-order edge is authored and nothing here claims a
  completion that the pattern text does not state."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["musn/fulab-report-block" 0]
   {:operation :emit-fulab-report-block
    :capability :wm/pattern-apply
    :consumes :turn-applied-patterns
    :produces :pattern-use-evidence-record
    :completion-check
    [:report-block-present-with-fields
     "response contains a [FULAB-REPORT] block with :applied, :action and :notes fields"]}

   ["agency/identifier-separation" 1]
   {:operation :enforce-typed-identifier-vocabulary
    :capability :wm/pattern-apply
    :consumes :identifier-call-sites
    :produces :typed-identifier-vocabulary-record
    :completion-check
    [:zero-ambiguous-keys
     "audit of endpoints shows zero ambiguous keys (no key used with multiple identifier meanings)"]}

   ["agency/identifier-separation" 2]
   {:operation :separate-continuity-from-transport-ids
    :capability :wm/pattern-apply
    :consumes :continuity-state-and-transport-payloads
    :produces :layer-separated-identifier-state
    :completion-check
    [:no-id-swap-or-overwrite
     "a property test generating payloads with forum-thread-id and resume-id asserts they never swap or overwrite one another; only LLM continuity ids live in continuity state, transport ids only inside transport payloads"]}

   ["problems/r4-forward-model" 3]
   {:operation :apply-pure-state-action-transition
    :capability :wm/pattern-apply
    :consumes :candidate-action-and-belief
    :produces :scored-candidate-action
    :completion-check
    [:pure-transition-scores-actions
     "a pure (state, action) -> next-state-distribution is used to score candidate actions before they are taken"]}

   ["problems/r4-forward-model" 4]
   {:operation :bind-live-and-predictor-to-one-dynamics-kernel
    :capability :wm/pattern-apply
    :consumes :live-dynamics-and-predictor
    :produces :shared-deterministic-dynamics-kernel
    :completion-check
    [:single-dynamics-kernel
     "the live dynamics and the predictor both call one pure deterministic dynamics kernel; only the predictor adds uncertainty"]}

   ["problems/r4-forward-model" 5]
   {:operation :construct-policy-conditioned-outcome-distribution
    :capability :wm/pattern-apply
    :consumes :belief-and-declared-models
    :produces :policy-conditioned-outcome-distribution
    :completion-check
    [:q-of-o-pi-computed
     "Q(o|pi) := sum_s A(o|s) Q(s|pi) is produced, with state rolled forward from mu through B over depth T"]}

   ["problems/r4-forward-model" 6]
   {:operation :land-why-edges-on-problem-node
    :capability :wm/pattern-apply
    :consumes :pattern-rationale-edges
    :produces :r4-problem-node-landing
    :completion-check
    [:why-edges-land-on-node
     "patterns whose rationale answers the R4 forward-model problem carry @why edges landing on this problem node"]}

   ["musn/fulab-report-block" 7]
   {:operation :keep-report-block-concise
    :capability :wm/pattern-apply
    :consumes :turn-summary
    :produces :concise-report-block
    :completion-check
    [:concise-annotated-report
     "the response stays concise at the end of the turn while the FULAB-REPORT block anchors the pattern use record in the log"]}})

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
