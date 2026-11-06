(ns futon2.aif.wm.authored.req-54009013
  "Authored operational clauses for held executable cascade
  d0c68aad93cd1e6efd98f3069f4c8b3dc82f358675f41563c102deadee8ae211
  (phase :compile, requisition
  54009013db8f4f51e9ee903b940aab6878868b56c1fa80156d0956921ab2a84f).

  Resolutions for the seven :operational-clause-owed holes of the
  M-librarian reading (target futon4/holes/missions/M-librarian.md).
  Each decision is an operational clause attributed to the reading's
  occurrence squares over that document: the operation names what the
  occurrence enacts in the mission text, completion-check names the
  observable condition in that text, and :consumes/:produces describe
  the transition.  Nothing here claims a square-derived completion and
  no application-order edge is authored."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["futon-theory/mission-scoping" 0]
   {:operation :bound-mission-object-at-charter
    :capability :wm/pattern-apply
    :consumes :mission-heading-naming-object
    :produces :chartered-mission-with-bounded-object
    :completion-check
    [:heading-names-native-3d-editing-practice
     "mission heading bounds the object as 'native 3D editing as a regular practice (Steam Frame)' (M-librarian line 1)"]}
   ["process-coherence/status-refresh-before-work" 1]
   {:operation :refresh-status-banner-before-work
    :capability :wm/pattern-apply
    :consumes :status-banner-and-body-gates
    :produces :re-derived-current-phase-record
    :completion-check
    [:banner-re-derived-against-vsat-and-hardware-state
     "any later work re-derives the phase by chasing the VSAT delivery gate and the unshipped-hardware condition named in the body, citing what was checked (status banner + body sentences)"]}
   ["futon-theory/mission-dependency" 2]
   {:operation :record-blocks-edge-to-vsat-delivery
    :capability :wm/pattern-apply
    :consumes :prose-gate-in-status-sentence
    :produces :explicit-blocks-relation-in-mission-graph
    :completion-check
    [:mission-doc-cites-specific-blocker
     "M-librarian cites the client-facing VSAT delivery as its specific blocker ('gated behind the client-facing VSAT delivery')"]}
   ["coordination/intent-to-mission-binding" 3]
   {:operation :bind-operator-remark-to-chartered-mission
    :capability :wm/pattern-apply
    :consumes :dated-operator-remark-provenance
    :produces :mission-anchored-charter-record
    :completion-check
    [:charter-records-remark-dates-and-attribution
     "charter line records 'Chartered 2026-08-10 from a 2026-08-09 operator remark during invoice-202506 planning' so later PSR/PUR/PAR anchor to the mission"]}
   ["futon-theory/mission-scoping" 4]
   {:operation :mark-mission-as-optional-bonus-lap
    :capability :wm/pattern-apply
    :consumes :mission-self-priority-characterization
    :produces :explicit-scope-out-relative-to-client-delivery
    :completion-check
    [:self-description-bounds-priority-below-client-delivery
     "mission describes itself as a \"bonus lap\" explicitly gated behind the client-facing VSAT delivery, bounding what it may claim"]}
   ["futon-theory/mission-dependency" 5]
   {:operation :restate-blocks-edge-in-intake-summary
    :capability :wm/pattern-apply
    :consumes :wrapped-prose-gate-restatement
    :produces :graph-consistent-gate-in-summary
    :completion-check
    [:summary-gate-matches-graph-edge
     "the wrapped intake prose restates the same blocker (client-facing VSAT delivery) with no drift from the recorded blocks relation"]}
   ["coordination/capability-gate" 6]
   {:operation :gate-start-on-hardware-availability
    :capability :wm/pattern-apply
    :consumes :external-hardware-availability-condition
    :produces :capability-validated-start-condition
    :completion-check
    [:no-scoping-or-resourcing-before-hardware-ships
     "mission stays unscoped and unresourced while 'the hardware it targets has not shipped'; work starts only once the capability exists"]}})

(defn decisions
  "The hole resolutions for this requisition, bound to the cascade digest
  they repair.  Returns the decision map keyed by hole id when the context
  names this cascade, else a typed mismatch (never a silent wrong answer)."
  [{:keys [cascade-digest phase] :as _ctx}]
  (if (and (= cascade-digest
              "d0c68aad93cd1e6efd98f3069f4c8b3dc82f358675f41563c102deadee8ae211")
           (= :compile phase))
    clauses
    {:status :error
     :kind :patch-cascade-context-mismatch
     :cascade-digest cascade-digest
     :phase phase}))
