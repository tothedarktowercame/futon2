(ns futon2.aif.wm.authored.req-1a602e71
  "Authored operational clauses for held executable cascade
  5b72e89c0d2a698261b923b138e39a70789edc44165b3fe715514bd4c20f7870
  (phase :compile, requisition 1a602e718ff6387db16a53a6675b9a571346898a71f4ec1eda718034f4a07002).

  Resolution for the single :operational-clause-owed hole of the
  M-pattern-posteriors reading (policy-2f5c7f6d3345d326aae3, analysis
  digest c77069919496de91822b137c0d2f5d70bbb08d008ddc0b0255c8bd57f813296b).
  The decision is an operational clause attributed to the reading's
  occurrence square: the operation names what the occurrence enacts in the
  mission text (span 45-60, 'OPEN — IDENTIFY'), the completion-check names
  the observable condition in that text, and :consumes/:produces describe
  the transition.  Nothing here claims a square-derived completion and no
  application-order edge is authored."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["futon-theory/mission-lifecycle" 0]
   {:operation :declare-mission-lifecycle-state
    :capability :wm/pattern-apply
    :consumes :mission-status-span
    :produces :recorded-identify-phase
    :completion-check
    [:status-open-identify-recorded
     "mission opening block records Status: OPEN — IDENTIFY"]}})

(defn decisions
  "The hole resolutions for this requisition, bound to the cascade digest
  they repair.  Returns the decision map keyed by hole id when the context
  names this cascade, else a typed mismatch (never a silent wrong answer)."
  [{:keys [cascade-digest phase] :as _ctx}]
  (if (and (= cascade-digest
              "5b72e89c0d2a698261b923b138e39a70789edc44165b3fe715514bd4c20f7870")
           (= :compile phase))
    clauses
    {:status :error
     :kind :patch-cascade-context-mismatch
     :cascade-digest cascade-digest
     :phase phase}))
