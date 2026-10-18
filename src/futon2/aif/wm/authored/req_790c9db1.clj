(ns futon2.aif.wm.authored.req-790c9db1
  "Authored operational clauses for held executable cascade
  c935271947ceb7cb220bd7d132a89b938df8e98bb1b31ece1a5743d870ea6a9b
  (phase :compile, requisition 790c9db1a5ccda5cfe9f0b9834ae4ee3cc37fefd908b553634096dd361965d1e).

  Resolutions for the eight :operational-clause-owed holes of the
  M-self-improvement-loop reading.  Each decision is an operational clause
  attributed to the reading's occurrence squares (analysis digest
  27dc8cf394d1c04e3bb6d4a5f5430a40b7e8344744b08b117f7606bccd269112):
  the operation names what the occurrence enacts in the mission text,
  completion-check names the observable condition in that text, and
  :consumes/:produces describe the transition.  Nothing here claims a
  square-derived completion and no application-order edge is authored."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["agent/eat-your-own-tail" 0]
   {:operation :constitute-self-improvement-loop
    :capability :wm/pattern-apply
    :consumes :running-stack-observations
    :produces :machinery-changes-for-next-runs
    :completion-check
    [:loop-bootstraps-itself
     "mission title and status name a self-improvement loop that bootstraps itself"]}
   ["agent/trail-enables-return" 1]
   {:operation :record-charter-date
    :capability :wm/pattern-apply
    :consumes :chartering-context
    :produces :dated-mission-record
    :completion-check
    [:date-field-present
     "mission opening block carries Date: 2026-02-25"]}
   ["agent/sense-deliberate-act" 2]
   {:operation :declare-phases-with-bootstrap
    :capability :wm/pattern-apply
    :consumes :mission-state
    :produces :explicit-phase-record
    :completion-check
    [:phases-named-together
     "mission records OPEN — IDENTIFY + DERIVE (bootstraps itself)"]}
   ["agency/self-attribution" 3]
   {:operation :record-undelegatable-ownership
    :capability :wm/pattern-apply
    :consumes :ownership-decision
    :produces :principal-attribution-record
    :completion-check
    [:owner-and-condition-named
     "mission names Joe as owner and states this is the one mission that can't be delegated"]}
   ["agent/scope-before-action" 4]
   {:operation :declare-entry-condition
    :capability :wm/pattern-apply
    :consumes :blocker-survey
    :produces :entry-scope-declaration
    :completion-check
    [:blockers-and-start-named
     "mission records Blocked by: Nothing — starts this Sunday"]}
   ["agent/handoff-preserves-context" 5]
   {:operation :define-cross-ref-handoff
    :capability :wm/pattern-apply
    :consumes :stack-component-context
    :produces :cross-reference-record
    :completion-check
    [:cross-ref-header-carries-components
     "mission cross-ref list names futon3b, futon2, futon5a/P0-P2, futon4, futon3c and futon6"]}
   ["agent/state-is-hypothesis" 6]
   {:operation :treat-stack-model-as-revisable
    :capability :wm/pattern-apply
    :consumes :stack-self-representation
    :produces :revisable-stack-model
    :completion-check
    [:self-representing-touchpoint-cited
     "mission cites M-self-representing-stack (futon4) as a dependency of the loop"]}
   ["agent/budget-bounds-exploration" 7]
   {:operation :bound-exploration-against-last-mile
    :capability :wm/pattern-apply
    :consumes :exploration-surface
    :produces :budgeted-commitment-mix
    :completion-check
    [:last-mile-touchpoint-cited
     "mission cites M-futon3-last-mile (futon3c) as a bounded commitment alongside the exploration surface"]}})

(defn decisions
  "The hole resolutions for this requisition, bound to the cascade digest
  they repair.  Returns the decision map keyed by hole id when the context
  names this cascade, else a typed mismatch (never a silent wrong answer)."
  [{:keys [cascade-digest phase] :as _ctx}]
  (if (and (= cascade-digest
              "c935271947ceb7cb220bd7d132a89b938df8e98bb1b31ece1a5743d870ea6a9b")
           (= :compile phase))
    clauses
    {:status :error
     :kind :patch-cascade-context-mismatch
     :cascade-digest cascade-digest
     :phase phase}))
