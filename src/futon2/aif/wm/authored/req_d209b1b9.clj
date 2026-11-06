(ns futon2.aif.wm.authored.req-d209b1b9
  "Authored operational clauses for held executable cascade
  b250a178a087d9254fcd1673de612613731af6d306edf7b13da3c085ee95171f
  (phase :compile, requisition
  d209b1b9f2cdbe9e54bf7c039dc892f768cc7867c999b8bec077a2e9a1862480).

  Resolutions for the six :operational-clause-owed holes owned by
  :wm/enactment-compiler in this requisition.  Each decision is an
  operational clause attributed to the occurrence squares of the
  M-action-cost-modelling opening reading (analysis digest
  1721c674babf8d6485fad6244cb56db1d30fa05647a847a23ccf4257c6b80a6a),
  whose spans index the mission header at
  futon3c/holes/missions/M-action-cost-modelling.md (title, type,
  status, date, author, replaces and renamed-from lines): the operation
  names what the occurrence enacts in that text, completion-check names
  the observable condition there, and :consumes/:produces describe the
  transition.  No claim is made about holes outside this requisition
  and no application-order edge is authored."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["futon-theory/mission-scoping" 0]
   {:operation :bind-title-to-bounded-mission-object
    :capability :wm/pattern-apply
    :consumes :mission-heading-line
    :produces :bounded-mission-object-record
    :completion-check
    [:title-names-action-cost-benefit-modelling
     "mission title reads '# Mission: Action Cost/Benefit Modelling for the WM' (excerpt chars 0-51), naming a bounded object of work"]}
   ["futon-theory/mission-lifecycle" 1]
   {:operation :record-retitle-lifecycle-transition
    :capability :wm/pattern-apply
    :consumes :excursion-to-mission-retitle-line
    :produces :attributed-lifecycle-transition-record
    :completion-check
    [:retitle-carries-operator-and-date
     "type line records the promotion 'retitled from E- to M- by Joe 2026-05-26 once DERIVE surfaced that this is full-fledged-mission scope' (excerpt chars 150-190 give the attributed retitle)"]}
   ["process-coherence/status-refresh-before-work" 2]
   {:operation :gate-stage-entry-on-rederived-status
    :capability :wm/pattern-apply
    :consumes :status-banner-with-tension-ledger
    :produces :rederived-arming-condition-record
    :completion-check
    [:instantiate-gated-on-joe-ratification
     "status line ends 'Mission ready for INSTANTIATE when Joe ratifies.' (excerpt chars 637-685) after enumerating the 10 carried tensions"]}
   ["futon-theory/mission-lifecycle" 3]
   {:operation :anchor-mission-provenance-date
    :capability :wm/pattern-apply
    :consumes :date-header-field
    :produces :timestamped-provenance-record
    :completion-check
    [:founding-date-recorded-in-header
     "date field reads '**Date:** 2026-05-26' (excerpt chars 696-706), matching the retitle date in the lifecycle trail"]}
   ["coordination/intent-to-mission-binding" 4]
   {:operation :bind-mission-to-named-owner-seat
    :capability :wm/pattern-apply
    :consumes :author-and-seat-byline
    :produces :owner-bound-mission-record
    :completion-check
    [:owner-named-with-seat-and-operator
     "author line reads 'claude-1 (inhabiting `:war-machine-pilot`, emacs-repl with Joe)' (excerpt chars 738-801)"]}
   ["futon-theory/mission-scoping" 6]
   {:operation :record-scope-revision-with-finding
    :capability :wm/pattern-apply
    :consumes :renamed-from-line
    :produces :cited-scope-revision-record
    :completion-check
    [:revision-cites-originating-section
     "renamed-from line justifies the retitle 'after §3.8 surfaced the aliveness-synthesis scope' (excerpt chars 1026-1075)"]}})

(defn decisions
  "The hole resolutions for this requisition, bound to the cascade digest
  they repair.  Returns the decision map keyed by hole id when the context
  names this cascade, else a typed mismatch (never a silent wrong answer)."
  [{:keys [cascade-digest phase] :as _ctx}]
  (if (and (= cascade-digest
              "b250a178a087d9254fcd1673de612613731af6d306edf7b13da3c085ee95171f")
           (= :compile phase))
    clauses
    {:status :error
     :mismatch {:cascade-digest cascade-digest
                :phase phase
                :expected-cascade-digest "b250a178a087d9254fcd1673de612613731af6d306edf7b13da3c085ee95171f"
                :expected-phase :compile}}))
