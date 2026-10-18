(ns futon2.aif.wm.authored.req-5ca29b22
  "Authored operational clauses for held executable cascade
  6d7160e09233aa90b6cca3b1e805ca2adab23e9d39593a20691a83fdcb10ecb7
  (phase :compile, requisition 5ca29b220bcaff1a06cb90e05b59b01cfadc55a08f677876b924741f8eb89eba).

  Resolutions for the four :operational-clause-owed holes of the
  M-formal-war-machine reading.  Each decision is an operational clause
  attributed to the reading's occurrence squares (analysis digest
  5b73f3901a5992827d5de02646d47e70995ba8ec14e3a8de9530e751b068fda3):
  the operation names what the occurrence enacts in the mission text,
  completion-check names the observable condition in that text, and
  :consumes/:produces describe the transition.  Nothing here claims a
  square-derived completion and no application-order edge is authored."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["agent/intent-handshake-is-binding" 0]
   {:operation :record-behavioural-contract
    :capability :wm/pattern-apply
    :consumes :mission-head-reading
    :produces :behavioural-contract-clause
    :completion-check
    [:contract-clause-recorded
     "mission title records 'a behavioural contract for the control loop' as its deliverable"]}
   ["agency/self-attribution" 1]
   {:operation :record-charter-attribution
    :capability :wm/pattern-apply
    :consumes :mission-status-line
    :produces :charter-attribution-record
    :completion-check
    [:charter-names-both-authors
     "status line names CHARTERED 2026-08-25 (Joe + claude-13) explicitly"]}
   ["agent/scope-before-action" 2]
   {:operation :declare-entry-scope
    :capability :wm/pattern-apply
    :consumes :identified-gap
    :produces :entry-scope-declaration
    :completion-check
    [:entry-condition-recorded
     "mission records 'the gap is crisp' as the declared condition for beginning at IDENTIFY"]}
   ["agent/eat-your-own-tail" 3]
   {:operation :apply-lifecycle-rule-to-own-mission
    :capability :wm/pattern-apply
    :consumes :mission-lifecycle-rule
    :produces :documented-lifecycle-deviation
    :completion-check
    [:lifecycle-deviation-cited
     "mission records `HEAD` skipped per futon4/holes/mission-lifecycle.md, citing the rule"]}})

(defn decisions
  "The hole resolutions for this requisition, bound to the cascade digest
  they repair.  Returns the decision map keyed by hole id when the context
  names this cascade, else a typed mismatch (never a silent wrong answer)."
  [{:keys [cascade-digest phase] :as _ctx}]
  (if (and (= cascade-digest
              "6d7160e09233aa90b6cca3b1e805ca2adab23e9d39593a20691a83fdcb10ecb7")
           (= :compile phase))
    clauses
    {:status :error
     :kind :patch-cascade-context-mismatch
     :cascade-digest cascade-digest
     :phase phase}))
