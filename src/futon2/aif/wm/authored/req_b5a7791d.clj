(ns futon2.aif.wm.authored.req-b5a7791d
  "Authored operational clauses for held executable cascade
  eaf91c0bd620a1880a4be16c0b04fc93177a4b2833dcfdafd0c5616b9c76bb5f
  (phase :compile, requisition
  b5a7791d973d228c6864fe15d831be1c1a1a7f2eac23ce56c17207901f1af9b2).

  Resolutions for the five :operational-clause-owed holes of the
  M-formal-patterns reading.  Each decision is an operational clause
  attributed to the occurrence squares authored over
  futon5/holes/M-formal-patterns.md: the operation names what the
  occurrence enacts in the mission text, completion-check names the
  observable condition in that text (the 2026-09-30 acceptance
  checklist), and :consumes/:produces describe the transition.
  Nothing here claims a square-derived completion and no
  application-order edge is authored."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["pattern-coherence/scope-boundaries" 0]
   {:operation :state-inclusion-and-exclusion-boundaries-for-the-mission
    :capability :wm/pattern-apply
    :consumes :mission-title-and-one-line-summary
    :produces :explicit-mission-scope-with-named-assumptions
    :completion-check
    [:mission-scope-bounded-by-one-line-and-checklist
     "the mission's applicability is decidable from its own header: the One-line names what is in (signed-graph coherence via the balance theorem at two scales) and the acceptance checklist names what is out (sorry/axiom/decide substitution, non-Peeragogy languages) (M-formal-patterns.md One-line, checklist items 3-5)"]}
   ["ways-of-working/intents-are-tracked" 1]
   {:operation :track-open-mission-intents-as-checkable-obligations
    :capability :wm/pattern-apply
    :consumes :mission-status-banner
    :produces :dated-acceptance-checklist-on-record
    :completion-check
    [:open-status-backed-by-tracked-checklist-items
     "the OPEN status banner is backed by the 2026-09-30 acceptance checklist whose six items are individually checkable by someone other than the chartering agents (M-formal-patterns.md Acceptance checklist)"]}
   ["ways-of-working/records-carry-warrant" 2]
   {:operation :warrant-the-chartering-provenance-claim
    :capability :wm/pattern-apply
    :consumes :charter-line-with-agents-and-date
    :produces :checkable-charter-provenance-record
    :completion-check
    [:charter-derivation-checkable-by-third-parties
     "the 2026-07-17 chartering claim (oxf-claude-2 + Joe) and the logic-model session are checkable from durable records rather than trusted from the banner (M-formal-patterns.md status line; core-insight section dated 2026-07-17)"]}
   ["mission-coherence/logic-model-before-code" 3]
   {:operation :verify-the-logic-model-before-the-lean-harness
    :capability :wm/pattern-apply
    :consumes :signed-graph-invariants-and-balance-theorem-statement
    :produces :satisfiable-and-enforceable-invariant-model
    :completion-check
    [:core-insight-precedes-and-constrains-the-checklist
     "the two-scale structural identity (pattern slots = MetaCA reads, one balance theorem at pattern and language scale) is established in the core-insight section before the implementation slices, and the checklist enforces it (theorem builds without sorry; S0 records a canonical signed graph; M-formal-patterns.md checklist items 2-3)"]}
   ["ways-of-working/records-carry-warrant" 4]
   {:operation :carry-per-slice-warrants-for-the-handoff-promise
    :capability :wm/pattern-apply
    :consumes :handoff-slices-promise-line
    :produces :per-slice-checkable-completion-records
    :completion-check
    [:each-slice-completion-evidenced-not-asserted
     "every promised slice closes with a matching record — TN-baldwin-reboot.md §10 recording N1-N4/H1-H4 discharged, the Lean build, the two coherence reports, the Peeragogy translation — not a pasted claim (M-formal-patterns.md checklist items 1-6, all currently unchecked)"]}})

(defn decisions
  "The hole resolutions for this requisition, bound to the cascade digest
  they repair.  Returns the decision map keyed by hole id when the context
  names this cascade, else a typed mismatch (never a silent wrong answer)."
  [{:keys [cascade-digest phase] :as _ctx}]
  (if (and (= cascade-digest
              "eaf91c0bd620a1880a4be16c0b04fc93177a4b2833dcfdafd0c5616b9c76bb5f")
           (= :compile phase))
    clauses
    {:status :error
     :kind :patch-cascade-context-mismatch
     :cascade-digest cascade-digest
     :phase phase}))
