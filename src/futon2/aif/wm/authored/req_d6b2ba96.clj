(ns futon2.aif.wm.authored.req-d6b2ba96
  "Authored operational clauses for held executable cascade
  addb2e2b3b70a8e18802909a5cc40ee7223ab20cf61b0db9607237019b5a2815
  (phase :compile, requisition
  d6b2ba962779a80e5b68b0f6fa4febdb59574e51af9426c6c21c0584235c6d7f).

  Resolutions for the six :operational-clause-owed holes of the
  M-f11-find-production-successor reading (analysis digest
  10a830e12f972b0d87f1afe9071878566f5e584799403d02eb0613c0a2967cca).
  Each decision is an operational clause attributed to the reading's
  occurrence squares: the operation names what the occurrence enacts in the
  mission text, completion-check names the observable condition in that
  text, and :consumes/:produces describe the transition. Nothing here
  claims a square-derived completion and no application-order edge is
  authored."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["coordination/bind-promotion-to-post-repair-replay" 0]
   {:operation :bind-successor-track-to-repair-024
    :capability :wm/pattern-apply
    :consumes :mission-title-successor-naming
    :produces :successor-track-binding
    :completion-check
    [:successor-named-in-title
     "mission title names this mission as the 'repair-024 successor' for repair-attempt-024-feature-card-missing-or-invalid"]}
   ["problems/r10-scheduled-entrypoint" 1]
   {:operation :record-backlog-position
    :capability :wm/pattern-apply
    :consumes :mission-status-line
    :produces :backlog-position-record
    :completion-check
    [:first-continuous-backlog-item
     "mission status line places this mission as the 'first continuous-backlog item' of the commissioned WM backlog"]}
   ["agent/commitment-varies-with-confidence" 2]
   {:operation :cite-continuous-operation-authority
    :capability :wm/pattern-apply
    :consumes :operator-direction-fragment
    :produces :cited-operator-authority
    :completion-check
    [:joe-direction-2026-09-12-named
     "mission cites Joe's 2026-09-12 direction that the commissioned WM backlog run continuously"]}
   ["agent/coordination-has-cost" 3]
   {:operation :record-in-lane-disposition-consequence
    :capability :wm/pattern-apply
    :consumes :lane-disposition-fragment
    :produces :successor-vehicle-record
    :completion-check
    [:in-lane-disposition-makes-f11-item-vehicle
     "mission states Claude-15's in-lane disposition makes this ordinary F11 work item the successor vehicle"]}
   ["coordination/bind-promotion-to-post-repair-replay" 4]
   {:operation :bind-promotion-to-independent-replay-receipt
    :capability :wm/pattern-apply
    :consumes :successor-shape-fragment
    :produces :pending-promotion-certificate
    :completion-check
    [:distinct-production-shaped-successor-named
     "mission names this F11 work item the distinct production-shaped successor for repair attempt 024, promotion staying pending on an independent replay receipt"]}
   ["snatch/re-enter-after-observed-repair" 5]
   {:operation :re-enter-through-successor-work
    :capability :wm/pattern-apply
    :consumes :repair-attempt-naming
    :produces :bounded-re-entry-probe
    :completion-check
    [:repair-attempt-024-named-without-relaxing-f11
     "mission names `repair-attempt-024-feature-card-missing-or-invalid` as the target and states it does not relax F11"]}})

(defn decisions
  "The hole resolutions for this requisition, bound to the cascade digest
  they repair.  Returns the decision map keyed by hole id when the context
  names this cascade, else a typed mismatch (never a silent wrong answer)."
  [{:keys [cascade-digest phase] :as _ctx}]
  (if (and (= cascade-digest
              "addb2e2b3b70a8e18802909a5cc40ee7223ab20cf61b0db9607237019b5a2815")
           (= :compile phase))
    clauses
    {:status :error
     :kind :patch-cascade-context-mismatch
     :cascade-digest cascade-digest
     :phase phase}))
