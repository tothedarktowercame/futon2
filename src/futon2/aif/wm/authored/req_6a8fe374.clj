(ns futon2.aif.wm.authored.req-6a8fe374
  "Authored operational clauses for held executable cascade
  5a0fa2adeb101b6bc644bf26511b22f5b88463ac1ef0490e941fd24b22de6995
  (phase :compile, requisition
  6a8fe374958818d6246ee46acc6dbdb851c7cdf66930626395572519879d195c).

  Resolutions for the twelve :operational-clause-owed holes named by the
  requisition.  Each decision is an operational clause attributed to the
  pattern occurrence it resolves: the operation names what the clause
  enacts, completion-check names the observable condition, and
  :consumes/:produces describe the transition.  Nothing here claims an
  application-order edge or a square-derived completion."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["problems/theory-general-enough-to-drive-the-stack" 0]
   {:operation :check-theory-against-stack-behaviour
    :capability :wm/pattern-apply
    :consumes :candidate-theory
    :produces :stack-grounded-theory-check
    :completion-check
    [:theory-predicts-stack-observable
     "the theory is exercised against a real stack behaviour it must predict, not illustrated on its own terms"]}
   ["agent/reduction-to-kernel" 1]
   {:operation :reduce-problem-to-its-kernel
    :capability :wm/pattern-apply
    :consumes :described-problem
    :produces :kernel-statement
    :completion-check
    [:kernel-stated-shorn-of-context
     "the problem is restated as the smallest kernel that still fails, with incident detail stripped away"]}
   ["agent/scope-before-action" 2]
   {:operation :fix-scope-before-acting
    :capability :wm/pattern-apply
    :consumes :task-request
    :produces :scoped-task-record
    :completion-check
    [:scope-recorded-before-any-edit
     "what is in and out of scope is written down and pinned before the first change is made"]}
   ["aif/declare-the-conditioning" 3]
   {:operation :declare-the-conditioning
    :capability :wm/pattern-apply
    :consumes :model-output-or-judgment
    :produces :declared-conditioning-record
    :completion-check
    [:conditioning-named-with-output
     "the conditions that shaped the judgment — inputs, prompts, priors — are declared alongside it, not implied"]}
   ["aif/no-self-certification" 4]
   {:operation :route-certification-outside-the-author
    :capability :wm/pattern-apply
    :consumes :authored-artifact
    :produces :externally-certified-artifact
    :completion-check
    [:certifier-distinct-from-author
     "the artifact's acceptance is witnessed by a party other than its author, with the reviewer named"]}
   ["agent/eat-your-own-tail" 5]
   {:operation :apply-the-mechanism-to-itself
    :capability :wm/pattern-apply
    :consumes :mechanism-under-construction
    :produces :self-application-witness
    :completion-check
    [:mechanism-used-on-its-own-output
     "the mechanism processes one of its own products as an input, and the run is retained"]}
   ["agent/handoff-preserves-context" 6]
   {:operation :carry-context-through-handoff
    :capability :wm/pattern-apply
    :consumes :sending-agent-state
    :produces :receiving-agent-context
    :completion-check
    [:receiver-restates-task-without-re-asking
     "the receiving agent can restate the task and its constraints from the handoff alone, without a clarifying round-trip"]}
   ["agent/eat-your-own-tail" 7]
   {:operation :apply-the-mechanism-to-itself
    :capability :wm/pattern-apply
    :consumes :mechanism-under-construction
    :produces :self-application-witness
    :completion-check
    [:mechanism-used-on-its-own-output
     "the mechanism processes one of its own products as an input, and the run is retained"]}
   ["agent/coordination-has-cost" 8]
   {:operation :account-coordination-cost-in-the-decision
    :capability :wm/pattern-apply
    :consumes :proposed-dispatch
    :produces :costed-dispatch-decision
    :completion-check
    [:cost-of-dispatch-stated-before-sending
     "the bell/whistle decision records what the coordination costs and what it buys, before dispatch"]}
   ["agency/delivery-receipt" 11]
   {:operation :issue-a-delivery-receipt
    :capability :wm/pattern-apply
    :consumes :completed-dispatch
    :produces :receipt-record
    :completion-check
    [:receipt-names-what-was-delivered-and-where
     "each delivery carries a receipt naming the artifact, the commit or evidence id, and the receiving channel"]}
   ["agent/student-dispatch" 12]
   {:operation :dispatch-student-with-bounded-task
    :capability :wm/pattern-apply
    :consumes :teachable-subtask
    :produces :student-dispatch-record
    :completion-check
    [:student-task-bounded-and-checkable
     "the student receives a bounded task with an explicit completion check the dispatching agent can verify"]}
   ["agent/eat-your-own-tail" 13]
   {:operation :apply-the-mechanism-to-itself
    :capability :wm/pattern-apply
    :consumes :mechanism-under-construction
    :produces :self-application-witness
    :completion-check
    [:mechanism-used-on-its-own-output
     "the mechanism processes one of its own products as an input, and the run is retained"]}})

(defn decisions
  "The hole resolutions for this requisition, bound to the cascade digest
  they repair.  Returns the decision map keyed by hole id when the context
  names this cascade, else a typed mismatch (never a silent wrong answer)."
  [{:keys [cascade-digest phase] :as _ctx}]
  (if (and (= cascade-digest
              "5a0fa2adeb101b6bc644bf26511b22f5b88463ac1ef0490e941fd24b22de6995")
           (= :compile phase))
    clauses
    {:status :error
     :kind :patch-cascade-context-mismatch
     :cascade-digest cascade-digest
     :phase phase}))
