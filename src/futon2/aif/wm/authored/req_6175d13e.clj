(ns futon2.aif.wm.authored.req-6175d13e
  "Repair patch for requisition 6175d13e383362996169138b188d2db40d70708022ecff7c61b034f8a9d9d406,
   cascade 01a5ee5e36a6eb89bdc3afd891188a58ed87c6cfa442a35e43b1d6a5c92e98c7 phase :compile.
   Supplies the operational clause owed for each of the six held
   :operational-clause-owed holes (owner :wm/enactment-compiler)."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["operator/define-the-progress-metric-in-the-briefing" 0]
   {:operation :state-acceptance-metric-in-the-briefing-itself
    :capability :wm/pattern-apply
    :consumes [:ticket-briefing-without-stated-metric]
    :produces [:briefing-carrying-its-own-progress-metric]
    :completion-check
    [:briefing-names-progress-metric-alongside-the-directive
     "the compiled entry states what progress IS in the same breath as the work directive, so downstream work optimizes the metric, not legible proxies"]}
   ["operator/delegate-with-the-check-attached" 1]
   {:operation :attach-acceptance-check-to-each-sub-ticket-delegation
    :capability :wm/pattern-apply
    :consumes [:sub-ticket-proposal-without-check]
    :produces [:sub-ticket-carrying-its-acceptance-check-and-dependencies]
    :completion-check
    [:every-sub-ticket-travels-with-its-check
     "each proposed sub-ticket is emitted together with its explicit acceptance check and named dependencies, so the receiver builds to the test rather than the description"]}
   ["pattern-mining/probe-the-claimed-property-not-the-acceptance-proxy" 1]
   {:operation :probe-independent-gradability-itself-not-a-proxy
    :capability :wm/pattern-apply
    :consumes [:sub-ticket-acceptance-check]
    :produces [:check-that-grades-the-claimed-property-directly]
    :completion-check
    [:check-tests-gradability-not-its-representation
     "the acceptance check on each sub-ticket probes the claimed property (independent gradability) and states its coverage; a proxy pass does not admit the sub-ticket"]}
   ["operator/defer-with-a-named-trigger" 2]
   {:operation :stop-only-with-a-concrete-named-blocker
    :capability :wm/pattern-apply
    :consumes [:decomposition-attempt]
    :produces [:stop-with-concrete-explanation-or-decomposition]
    :completion-check
    [:stop-names-what-blocks-decomposition
     "a stop decision records the concrete explanation of why responsible decomposition is not yet possible; a silent or vague stop is refused"]}
   ["pattern-mining/probe-the-claimed-property-not-the-acceptance-proxy" 3]
   {:operation :score-partial-unchecked-work-as-failure
    :capability :wm/pattern-apply
    :consumes [:partial-implementation-without-checks]
    :produces [:visible-non-success-state]
    :completion-check
    [:unchecked-partial-work-is-a-visible-non-success
     "an implementation whose checks were not run is scored as failure and stays a visible non-success state, never counted as progress"]}
   ["operator/name-the-done-signal-act" 4]
   {:operation :pre-name-the-done-signal-act-in-the-outcome
    :capability :wm/pattern-apply
    :consumes [:ticket-without-named-done-act]
    :produces [:expected-outcome-naming-the-completion-act]
    :completion-check
    [:done-is-recognizable-on-sight
     "the compiled entry pre-names the act whose performance constitutes done (stop with decomposition or reason), so the loop can end cleanly at that signal"]}})

(defn decisions
  "Function of the resume context {:cascade-digest :phase}; returns the
  operational clause map keyed by hole id, bound to the cascade this patch
  repairs.  A foreign context is a typed mismatch, never a silent answer."
  [{:keys [cascade-digest phase] :as _ctx}]
  (if (and (= cascade-digest
              "01a5ee5e36a6eb89bdc3afd891188a58ed87c6cfa442a35e43b1d6a5c92e98c7")
           (= :compile phase))
    clauses
    {:status :error
     :kind :patch-cascade-context-mismatch
     :cascade-digest cascade-digest
     :phase phase}))
