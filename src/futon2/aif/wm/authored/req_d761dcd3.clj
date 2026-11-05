(ns futon2.aif.wm.authored.req-d761dcd3
  "Authored operational clauses for held executable cascade
  4da079e3f28460856f7f137980f8894a52b2bafc68e11b3220eca44baaf05a7b
  (phase :compile, requisition
  d761dcd30db28dfb86f692bad96192e11ca609898c88e31349d931c6b10d7857).

  Resolutions for the ten :operational-clause-owed holes of the
  M-usage-hacking reading (analysis digest
  00346c5c72f33786ca1eac0245cec90c0f53d0c413e60b9b73a92f11baa0407b).
  Each decision is an operational clause attributed to the reading's
  occurrence squares over futon0/holes/missions/M-usage-hacking.md:
  the operation names what the occurrence enacts in the mission text,
  completion-check names the observable condition in that text, and
  :consumes/:produces describe the transition.  Nothing here claims a
  square-derived completion and no application-order edge is authored."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["task-selection/spend-what-the-seats-have" 0]
   {:operation :spend-seat-quotas-deliberately
    :capability :wm/pattern-apply
    :consumes :provider-quota-observations
    :produces :evidence-backed-allocation-strategy
    :completion-check
    [:spending-derived-from-evidence
     "mission replaces rules of thumb with quota consumption joined to workload and outcome evidence (M-usage-hacking §1)"]}
   ["task-selection/the-date-decides" 1]
   {:operation :pin-proposal-date-and-capture-window
    :capability :wm/pattern-apply
    :consumes :mission-date-line
    :produces :dated-observational-window-record
    :completion-check
    [:zai-window-replayable-from-durable-observations
     "mission date 2026-07-14 with the Zai 13–18 July 2026 window replayable from durable observations (§3, §8)"]}
   ["task-selection/find-out-before-building" 2]
   {:operation :complete-identify-before-building
    :capability :wm/pattern-apply
    :consumes :provider-accuracy-audit
    :produces :identified-reset-semantics-record
    :completion-check
    [:identify-exit-criteria-met
     "mission stays OPEN — IDENTIFY until provider accuracy, reset semantics and one full reset window per provider are documented (§7, §8)"]}
   ["task-selection/the-operator-asked" 3]
   {:operation :hold-operator-authorization-gate
    :capability :wm/pattern-apply
    :consumes :operator-ownership-line
    :produces :authorization-gate-record
    :completion-check
    [:joe-authorizes-actuation-transition
     "Joe explicitly authorises any transition from shadow recommendations to automatic adaptive scheduling (§8)"]}
   ["mission-coherence/logic-model-before-code" 4]
   {:operation :verify-invariants-by-replay-before-actuation
    :capability :wm/pattern-apply
    :consumes :policy-invariants-and-historical-trace
    :produces :deterministic-policy-replay-oracle
    :completion-check
    [:zai-example-replayed-with-explainable-decisions
     "deterministic replay reproduces the Zai 13–18 July window with explainable decisions from the fixed 50% policy and at least one adaptive candidate (§8)"]}
   ["task-selection/what-others-wait-on-first" 5]
   {:operation :order-schema-and-adapters-before-derivations
    :capability :wm/pattern-apply
    :consumes :cross-referenced-predecessor-artifacts
    :produces :shared-observation-schema-first-ordering
    :completion-check
    [:schema-precedes-burndown-and-replay
     "MAP's provider-neutral observation schema precedes DERIVE burn rates and forecasts, since burndown, attribution and replay all build on it (§7)"]}
   ["war-machine/state-capture" 6]
   {:operation :tie-usage-work-to-external-obligations
    :capability :wm/pattern-apply
    :consumes :provider-usage-rendering-tool
    :produces :outcome-joined-consumption-record
    :completion-check
    [:consumption-joined-to-useful-outcomes
     "usage work answers which useful outcomes consumption produced; raw token consumption alone is out of scope (§2 q4, §4.2)"]}
   ["war-machine/operational-not-decorative" 7]
   {:operation :make-burndown-operational-not-decorative
    :capability :wm/pattern-apply
    :consumes :stack-hud-display-surface
    :produces :evidence-deriving-fail-closed-display
    :completion-check
    [:burndown-renders-history-with-provenance-and-fails-closed
     "all-agent burndown renders recorded history and reset boundaries with provenance and forecast error visible, and missing evidence fails closed (§5, §6 invariants 1 and 6, §8)"]}
   ["budgeted-action-selection/mana-gated-work" 8]
   {:operation :gate-unattended-dispatch-on-forecast-affordance
    :capability :wm/pattern-apply
    :consumes :conservative-zai-quota-gate
    :produces :reserve-protective-gated-candidate-policy
    :completion-check
    [:gate-honors-every-window-and-foreground-reserve
     "candidate policy permits unattended work only when forecast unused quota at reset exceeds a configured reserve, respecting every active window including the 04:15 short-reset case (§3.2, §3.4, §6 invariant 3)"]}
   ["task-selection/write-it-down-instead" 9]
   {:operation :record-outcomes-as-durable-joined-ledger
    :capability :wm/pattern-apply
    :consumes :formal-progress-outcome-history
    :produces :append-only-ledger-with-outcome-links
    :completion-check
    [:ledger-links-consumption-to-outcome-evidence
     "versioned append-only observation schema with durable links to outcome evidence so selection chooses from recorded history (§4.1, §5)"]}})

(defn decisions
  "The hole resolutions for this requisition, bound to the cascade digest
  they repair.  Returns the decision map keyed by hole id when the context
  names this cascade, else a typed mismatch (never a silent wrong answer)."
  [{:keys [cascade-digest phase] :as _ctx}]
  (if (and (= cascade-digest
              "4da079e3f28460856f7f137980f8894a52b2bafc68e11b3220eca44baaf05a7b")
           (= :compile phase))
    clauses
    {:status :error
     :kind :patch-cascade-context-mismatch
     :cascade-digest cascade-digest
     :phase phase}))
