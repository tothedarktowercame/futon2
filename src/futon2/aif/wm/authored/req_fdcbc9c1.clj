(ns futon2.aif.wm.authored.req-fdcbc9c1
  "Authored operational clauses for held executable cascade
  8f38950b10b5f88b6755ca4163115efb76cb6739bafba234a3d92c8b3e212ca3
  (phase :compile, requisition
  fdcbc9c163c79868a96f8c70d584a08d9cb2aa14fc75bd1e5a4566b07e11044f).

  Resolutions for the three :operational-clause-owed holes of the
  M-run-produces-its-own-brief reading (analysis digest
  cf3708b3cfdd7d6032a0f98ee661bb31c794b0cb166920e22e07d8177ce5d755).
  Each decision is an operational clause attributed to the reading's
  occurrence squares over
  futon2/holes/missions/M-run-produces-its-own-brief.md: the operation
  names what the occurrence enacts in the mission text, completion-check
  names the observable condition in that text, and :consumes/:produces
  describe the transition.  Nothing here claims a square-derived
  completion and no application-order edge is authored."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["meta/designing-thoughts-not-text" 0]
   {:operation :emit-brief-structure-during-the-run
    :capability :wm/pattern-apply
    :consumes :run-event-and-decision-stream
    :produces :structure-first-run-brief
    :completion-check
    [:brief-structured-by-the-run-itself
     "the mission file's brief is produced by the run as structure (what happened, what was decided, what remains), so the wording is a rendering of the run rather than post-hoc prose (heading 'a run produces its own brief')"]}
   ["measurement/warrant-travels-with-the-number" 1]
   {:operation :attach-warrant-to-the-status-line
    :capability :wm/pattern-apply
    :consumes :mission-status-marker-and-its-basis
    :produces :warranted-open-status-record
    :completion-check
    [:status-open-checkable-from-the-record
     "the '**Status:** OPEN' line carries its basis (rule or checkpoint id, pointers to evidence) so a skeptical reader can verify why the mission is open from the record alone, instead of archaeology over history"]}
   ["measurement/warrant-travels-with-the-number" 2]
   {:operation :warrant-the-recorded-absence-of-prior-status
    :capability :wm/pattern-apply
    :consumes :prior-status-slot-examination
    :produces :falsifiable-absence-record
    :completion-check
    [:absence-verifiable-not-asserted
     "'No prior status was recorded.' is recorded with its check (file, span, digest) so the emptiness of the prior-status slot is falsifiable evidence of the mission's starting point, not an assertion"]}})

(defn decisions
  "The hole resolutions for this requisition, bound to the cascade digest
  they repair.  Returns the decision map keyed by hole id when the context
  names this cascade, else a typed mismatch (never a silent wrong answer)."
  [{:keys [cascade-digest phase] :as _ctx}]
  (if (and (= cascade-digest
              "8f38950b10b5f88b6755ca4163115efb76cb6739bafba234a3d92c8b3e212ca3")
           (= :compile phase))
    clauses
    {:status :error
     :kind :patch-cascade-context-mismatch
     :cascade-digest cascade-digest
     :phase phase}))
