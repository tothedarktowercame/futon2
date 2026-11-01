(ns futon2.aif.wm.authored.req-b435c487
  "Authored operational clauses for held executable cascade
  ac0b46e62bf32ad9f90ad83c52f2c05a1b35b0e853d7f4c881dbeb9da87b65ff
  (phase :compile, requisition b435c487d4f2dbe8209c5a5dd8aa22c09efd3e0ca30a5667589cebf334f59358).

  Resolutions for the two :operational-clause-owed holes of the
  M-apm-capability-ratchet reading (analysis digest
  c2b409ed22520ca7d365af5d8341e767854b9bb16dd0e48c7571cafe116e58cd).
  Each decision is an operational clause attributed to the reading's
  occurrence squares: the operation names what the occurrence enacts in the
  mission text, completion-check names the observable condition in that
  text, and :consumes/:produces describe the transition.  Nothing here
  claims a square-derived completion and no application-order edge is
  authored."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["agency/self-attribution" 2]
   {:operation :carry-authored-opening-date-in-charter
    :capability :wm/pattern-apply
    :consumes :mission-opening-header-fragment
    :produces :self-dated-charter-record
    :completion-check
    [:date-line-in-mission-body
     "mission opening carries its own '**Date:** 2026-07-22' line rather than leaving the opening date to external metadata"]}
   ["coordination/capability-gate" 3]
   {:operation :hold-mission-open-until-capability-demonstrated
    :capability :wm/pattern-apply
    :consumes :mission-status-line
    :produces :open-capability-gate-record
    :completion-check
    [:status-open-with-map-addendum
     "mission status line reads 'OPEN — MAP' with the 2026-07-22 capability-topology map and evidence-observation substrate survey addendum named"]}})

(defn decisions
  "The hole resolutions for this requisition, bound to the cascade digest
  they repair.  Returns the decision map keyed by hole id when the context
  names this cascade, else a typed mismatch (never a silent wrong answer)."
  [{:keys [cascade-digest phase] :as _ctx}]
  (if (and (= cascade-digest
              "ac0b46e62bf32ad9f90ad83c52f2c05a1b35b0e853d7f4c881dbeb9da87b65ff")
           (= :compile phase))
    clauses
    {:status :error
     :kind :patch-cascade-context-mismatch
     :cascade-digest cascade-digest
     :phase phase}))
