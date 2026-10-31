(ns futon2.aif.wm.authored.req-6a8fe374
  "Authored operational clauses for held executable cascade
  5a0fa2adeb101b6bc644bf26511b22f5b88463ac1ef0490e941fd24b22de6995
  (phase :compile, requisition
  6a8fe374958818d6246ee46acc6dbdb851c7cdf66930626395572519879d195c).

  One operational clause per :operational-clause-owed hole; no
  re-selection, no application-order edge, and no claim beyond the
  compiled decisions themselves.  The trusted loader registers this
  namespace's source identity at load time."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["problems/theory-general-enough-to-drive-the-stack" 0]
   {:operation :enact-stack-driven-theory-check
    :capability :wm/pattern-apply
    :consumes :stack-theory-conditioning
    :produces :theory-stack-drive-observation
    :completion-check
    [:theory-check-ran-in-stack
     "the theory was exercised against the live stack, not described offline"]}

   ["agent/reduction-to-kernel" 1]
   {:operation :reduce-problem-to-kernel
    :capability :wm/pattern-apply
    :consumes :scoped-problem-statement
    :produces :kernel-reduction
    :completion-check
    [:kernel-named
     "the scoped problem is restated as its smallest kernel with the remainder explicitly set aside"]}

   ["agent/scope-before-action" 2]
   {:operation :record-scope-before-acting
    :capability :wm/pattern-apply
    :consumes :task-instructions
    :produces :scope-record
    :completion-check
    [:scope-written-pre-action
     "a scope statement exists in the record before any change was made"]}

   ["aif/declare-the-conditioning" 3]
   {:operation :declare-conditioning-before-use
    :capability :wm/pattern-apply
    :consumes :decision-inputs
    :produces :declared-conditioning
    :completion-check
    [:conditioning-declared
     "the conditioning assumptions behind the decision are written down before the decision is relied on"]}

   ["aif/no-self-certification" 4]
   {:operation :route-certification-to-independent-party
    :capability :wm/pattern-apply
    :consumes :completion-claim
    :produces :independent-check-request
    :completion-check
    [:certifier-differs-from-author
     "the checking party is not the authoring party of the claim"]}

   ["agent/eat-your-own-tail" 5]
   {:operation :apply-mechanism-to-itself
    :capability :wm/pattern-apply
    :consumes :mechanism-definition
    :produces :self-application-run
    :completion-check
    [:self-run-recorded
     "the mechanism was run on its own output and the result retained"]}

   ["agent/handoff-preserves-context" 6]
   {:operation :carry-context-across-handoff
    :capability :wm/pattern-apply
    :consumes :sending-session-state
    :produces :receiving-session-state
    :completion-check
    [:context-present-after-handoff
     "the receiving session can answer a question answerable only from the handed-over context"]}

   ["agent/eat-your-own-tail" 7]
   {:operation :apply-mechanism-to-itself-again
    :capability :wm/pattern-apply
    :consumes :mechanism-output
    :produces :second-self-application-run
    :completion-check
    [:second-self-run-recorded
     "a second, distinct self-application is recorded with its own output"]}

   ["agent/coordination-has-cost" 8]
   {:operation :account-coordination-cost-before-dispatch
    :capability :wm/pattern-apply
    :consumes :dispatch-intent
    :produces :costed-dispatch-decision
    :completion-check
    [:cost-recorded-with-dispatch
     "each coordination dispatch carries its cost alongside the decision it justified"]}

   ["agency/delivery-receipt" 11]
   {:operation :issue-delivery-receipt-on-completion
    :capability :wm/pattern-apply
    :consumes :delivered-artifact
    :produces :delivery-receipt
    :completion-check
    [:receipt-pins-artifact
     "a receipt exists that pins the delivered artifact by full-value digest"]}

   ["agent/student-dispatch" 12]
   {:operation :dispatch-scoped-work-to-student
    :capability :wm/pattern-apply
    :consumes :scoped-task
    :produces :student-dispatch-record
    :completion-check
    [:dispatch-named-scope-and-check
     "the student dispatch states the scope and the completion check the student must satisfy"]}

   ["agent/eat-your-own-tail" 13]
   {:operation :apply-mechanism-to-itself-third
    :capability :wm/pattern-apply
    :consumes :self-application-run
    :produces :third-self-application-run
    :completion-check
    [:third-self-run-recorded
     "a third self-application is recorded and compared against the prior two"]}})

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
