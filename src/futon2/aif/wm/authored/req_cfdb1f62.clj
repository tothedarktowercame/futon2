(ns futon2.aif.wm.authored.req-cfdb1f62
  "Authored operational clauses for held executable cascade
  309421670ca97dd1b1c73447bca9d818b93e2334d5d98d3c0208dd419b9336cb
  (phase :compile, requisition
  cfdb1f621bb56b7b80cb3f09c11d41ea7ecfa410bab59116fe8c3dacbd1c94bf).

  Resolutions for the five :operational-clause-owed holes owned by
  :wm/enactment-compiler.  Each decision is an operational clause routed
  through the single routable capability :wm/pattern-apply; the operation
  names the pattern application that repairs the hole, completion-check
  names the observable condition, and :consumes/:produces describe the
  transition.  No claim is made about holes outside this requisition."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["futon-theory/progress-signal" 1]
   {:operation :apply-progress-signal-to-phase-declaration
    :capability :wm/pattern-apply
    :consumes :mission-phase-declaration
    :produces :evidence-anchored-phase-reading
    :completion-check
    [:phase-claim-backed-by-evidence-ladder
     "the MAP-phase declaration is scored against the evidence ladder (criteria met, artifact, decision, blocker, review, session) so the phase reading cites at least one typed evidence item"]}
   ["futon-theory/single-source-of-truth" 2]
   {:operation :apply-single-source-of-truth-to-substrate-naming
    :capability :wm/pattern-apply
    :consumes :named-substrate-reference
    :produces :single-authority-substrate-binding
    :completion-check
    [:substrate-resolves-to-exactly-one-authority
     "the named futon3a MiniLM-cosine substrate is bound to exactly one pinned source of record, and any second candidate mapping is rejected rather than silently resolved"]}
   ["futon-theory/honest-map-over-flattering-counter" 3]
   {:operation :apply-honest-map-over-flattering-counter-to-pipeline-verdict
    :capability :wm/pattern-apply
    :consumes :substrate-output-artifact
    :produces :raised-hole-count-verdict
    :completion-check
    [:artifact-verdict-raises-not-hides-the-count
     "the 'artifact, not signal' verdict is recorded as a visible hole in the substrate map (count raised), not absorbed as a flattering pass"]}
   ["futon-theory/progress-signal" 5]
   {:operation :apply-progress-signal-to-refocus-declaration
    :capability :wm/pattern-apply
    :consumes :refocus-declaration
    :produces :recorded-decision-evidence
    :completion-check
    [:refocus-recorded-as-decision-made
     "the centre-of-gravity shift is recorded as a DECISION-MADE evidence item in the mission record so momentum is computed against the new focus and the counter-ratchet flags any evidence decrease"]}
   ["futon-theory/single-source-of-truth" 7]
   {:operation :apply-single-source-of-truth-to-goal-state-specification
    :capability :wm/pattern-apply
    :consumes :goal-state-specification
    :produces :committed-replacement-goal-state
    :completion-check
    [:goal-state-committed-as-single-authority
     "the specify-and-replace goal-state is the one committed target identity; patching the old substrate is no longer a live authority and ambiguity between the two is refused"]}})

(defn decisions
  "The hole resolutions for this requisition, bound to the cascade digest
  they repair.  Returns the decision map keyed by hole id when the context
  names this cascade, else a typed mismatch (never a silent wrong answer)."
  [{:keys [cascade-digest phase] :as _ctx}]
  (if (and (= cascade-digest
              "309421670ca97dd1b1c73447bca9d818b93e2334d5d98d3c0208dd419b9336cb")
           (= :compile phase))
    clauses
    {:status :error
     :kind :patch-cascade-context-mismatch
     :cascade-digest cascade-digest
     :phase phase}))
