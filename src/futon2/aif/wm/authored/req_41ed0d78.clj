(ns futon2.aif.wm.authored.req-41ed0d78
  "Authored operational clauses for held executable cascade
  53e277f5a62e41b1a3970985c130c07b0e3c6a446f65d19321134f470e9d6dfd
  (phase :compile, requisition
  41ed0d78db05a4f3e2155157430ff96734d355829a3a68252ddc527d9f227e48).

  Resolutions for the five :operational-clause-owed holes of the
  T-outer-belief-separate-machine-failure reading
  (futon2/holes/tickets/T-outer-belief-separate-machine-failure.md).
  Each decision is an operational clause attributed to the reading's
  occurrence squares over that ticket's opening: the operation names what
  the occurrence enacts in the ticket text, completion-check names the
  observable condition in that text, and :consumes/:produces describe the
  transition.  Nothing here claims a square-derived completion and no
  application-order edge is authored."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["apparatus/new-failure-class-is-a-design-defect" 0]
   {:operation :open-design-question-before-absorbing-apparatus-failure
    :capability :wm/pattern-apply
    :consumes :apparatus-failure-arriving-at-outer-belief
    :produces :apparatus-failure-kept-out-of-target-record
    :completion-check
    [:apparatus-failures-not-folded-into-outer-belief
     "ticket imperative holds: 'Do not fold apparatus failures into the outer target belief as task outcomes' — no apparatus failure class is absorbed without a recorded design question one layer down"]}
   ["apparatus/success-must-not-resemble-failure" 1]
   {:operation :write-apparatus-failures-in-distinct-category
    :capability :wm/pattern-apply
    :consumes :task-outcome-category-reused-for-apparatus-failure
    :produces :distinct-apparatus-failure-category-in-ledger-vocabulary
    :completion-check
    [:no-apparatus-failure-recorded-as-task-outcome
     "apparatus failures carry their own vocabulary instead of the 'task outcomes' label, so no downstream reader must reconstruct the attribution"]}
   ["apparatus/open-conditions-live-in-a-ledger" 2]
   {:operation :carry-open-condition-in-ledger-not-projection
    :capability :wm/pattern-apply
    :consumes :open-status-marker-in-ticket-header
    :produces :ledger-entry-with-explicit-transitions
    :completion-check
    [:open-condition-survives-unrelated-runs-until-named-repair
     "the OPEN — FIX NOW condition leaves open only through a transition naming the retry and the repair, never by an unrelated run ending differently"]}
   ["task-selection/the-operator-asked" 3]
   {:operation :quote-operator-ask-as-charter
    :capability :wm/pattern-apply
    :consumes :dated-operator-attribution-in-status-line
    :produces :chartered-ask-in-selectable-pool
    :completion-check
    [:charter-preserves-joes-quoted-ask-and-date
     "the record keeps the '(Joe, 2026-10-19)' attribution verbatim so the delivered fix can be checked against the wanted fix"]}
   ["mission-coherence/logic-model-before-code" 4]
   {:operation :verify-belief-invariants-over-abstract-trace-first
    :capability :wm/pattern-apply
    :consumes :parent-mission-invariant-expressible-over-trace
    :produces :goal-query-model-with-adversarial-trace-coverage
    :completion-check
    [:adversarial-trace-apparatus-failure-as-task-outcome-caught
     "the logic model over the abstract trace catches the adversarial case this ticket exists for — an apparatus failure written as a task outcome — before any live harness change"]}})

(defn decisions
  "The hole resolutions for this requisition, bound to the cascade digest
  they repair.  Returns the decision map keyed by hole id when the context
  names this cascade, else a typed mismatch (never a silent wrong answer)."
  [{:keys [cascade-digest phase] :as _ctx}]
  (if (and (= cascade-digest
              "53e277f5a62e41b1a3970985c130c07b0e3c6a446f65d19321134f470e9d6dfd")
           (= :compile phase))
    clauses
    {:status :error
     :kind :patch-cascade-context-mismatch
     :cascade-digest cascade-digest
     :phase phase}))
