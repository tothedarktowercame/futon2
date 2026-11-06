(ns futon2.aif.wm.authored.req-1c0912b1
  "Authored operational clauses for held executable cascade
  3f11fba926572ddd18b0f5c3b50f2a697f2d4972315bcaffd02ea892654f2734
  (phase :compile, requisition
  1c0912b177caf7954f29605ad671c798b3ccf1c9bf7ffb488937181d24e1fe17).

  Resolutions for the two :operational-clause-owed holes of the
  M-or-training-as-learning-system reading (target
  futon4/holes/missions/M-or-training-as-learning-system.md, opening
  header).  Each decision is an operational clause attributed to the
  reading's occurrence squares over that header: the operation names
  what the occurrence enacts in the mission text, completion-check
  names the observable condition in that text, and :consumes/:produces
  describe the transition.  Nothing here claims a square-derived
  completion and no application-order edge is authored."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["mission-coherence/logic-model-before-code" 0]
   {:operation :pin-mission-in-identify-phase-before-code
    :capability :wm/pattern-apply
    :consumes :header-status-line-with-phase-label
    :produces :declared-pre-implementation-logic-model-stage
    :completion-check
    [:header-declares-open-identify-phase
     "mission header declares the working state as 'OPEN — IDENTIFY' before any harness or committed code, marking the stage where invariants are design-level and verifiable over an abstract trace (chars 12–27 of M-or-training-as-learning-system.md)"]}
   ["meta/exotype-determines-behaviour" 1]
   {:operation :pin-versioned-draft-archive-trail
    :capability :wm/pattern-apply
    :consumes :draft-1-superseded-by-draft-2
    :produces :explicit-exotype-version-boundary-between-drafts
    :completion-check
    [:header-records-versioned-archive-of-prior-draft
     "mission header records 'draft 1 archived as `M-or-training-as-learning-system.v1.md`' so each draft's environmental assumptions are pinned and checkable rather than implicit (chars 50–110 of M-or-training-as-learning-system.md)"]}})

(defn decisions
  "The hole resolutions for this requisition, bound to the cascade digest
  they repair.  Returns the decision map keyed by hole id when the context
  names this cascade, else a typed mismatch (never a silent wrong answer)."
  [{:keys [cascade-digest phase] :as _ctx}]
  (if (and (= cascade-digest
              "3f11fba926572ddd18b0f5c3b50f2a697f2d4972315bcaffd02ea892654f2734")
           (= :compile phase))
    clauses
    {:status :error
     :kind :patch-cascade-context-mismatch
     :cascade-digest cascade-digest
     :phase phase}))
