(ns futon2.aif.wm.authored.req-5706e20b
  "Authored operational clauses for held executable cascade
  4bf6ef8c4352bd6c819aa907669ed8ce198a32707b36bbacf82d854435670418
  (phase :compile, requisition
  5706e20b37267cefdb0fba16051556f58fc0d64cfb82df1a45b59d27f558c3b6).

  Resolutions for the nine :operational-clause-owed holes owned by
  :wm/enactment-compiler in this requisition.  Each decision is an
  operational clause attributed to the occurrence squares of the
  T-car3-queue-routing-design-note reading (analysis digest
  0d16711818ec3ec9439eab904bd7370b62573453a0add698ee6cd4c5fdb519ec),
  whose spans index the opening of
  futon3c/holes/tickets/T-car3-queue-routing-design-note.md (title,
  dated status paragraph, and the model-only ratification block): the
  operation names what the occurrence enacts in that text,
  completion-check names the observable condition there, and
  :consumes/:produces describe the transition.  No claim is made about
  holes outside this requisition and no application-order edge is
  authored."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["test-registry/bind-the-subject" 0]
   {:operation :scope-phase-one-closure-to-fresh-witness
    :capability :wm/pattern-apply
    :consumes :phase-one-heading
    :produces :phase-scoped-closure-condition
    :completion-check
    [:close-condition-names-phase-two-witness
     "heading scopes 'Phase 1 design note' (chars 25-44) and the paragraph closes only 'when Phase 2 surface adapters confirm external surfaces or accept the fallback formally' (chars 1251-1345)"]}
   ["features/operator-turns-enter-the-observation-vector" 1]
   {:operation :capture-turns-with-stable-identity
    :capability :wm/pattern-apply
    :consumes :accept-site-ingress
    :produces :typed-turn-observation
    :completion-check
    [:all-in-repo-accept-sites-supply-identity
     "status line rules 'capture gap at futon3c accept sites CLOSED (all in-repo accept sites now supply stable :from/:surface/:msg-id)' (chars 46-193)"]}
   ["invariant-coherence/reachable-from-boot" 2]
   {:operation :persist-route-and-seq-markers
    :capability :wm/pattern-apply
    :consumes :turn-queue-ordering-state
    :produces :durable-route-seq-marker-state
    :completion-check
    [:markers-exist-at-named-lines
     "'Persisted route + seq markers exist (turn_queue.clj:213,241,523)' (chars 194-259) grounds ordering in durable state"]}
   ["test-registry/bind-the-subject" 3]
   {:operation :audit-every-accept-site
    :capability :wm/pattern-apply
    :consumes :accept-site-enumeration-question
    :produces :exhaustive-site-audit-record
    :completion-check
    [:enumerated-list-confirms-from-and-surface
     "audit of every turn-queue/accept-async! / accept-operator-block! site in futon3c/transport/http.clj concludes 'each supplies `:from` and `:surface`' (chars 260-528)"]}
   ["test-registry/bind-the-subject" 4]
   {:operation :migrate-generated-msg-id-to-job-id
    :capability :wm/pattern-apply
    :consumes :generated-msg-id-fallback-sites
    :produces :stable-msg-id-accept-sites
    :completion-check
    [:every-fallback-site-passes-job-id
     "'every site that previously fell back to a generated msg-id now passes `:msg-id (str job-id)`' (chars 529-621)"]}
   ["test-registry/bind-the-subject" 5]
   {:operation :pin-retry-dedupe-to-named-test
    :capability :wm/pattern-apply
    :consumes :retry-safety-claim
    :produces :test-pinned-retry-dedupe-warrant
    :completion-check
    [:named-test-holds-dedupe
     "'Retry safety is pinned by `accept-sites-must-supply-stable-msg-id-for-retry-dedupe` in `test/futon3c/agency/turn_queue_v2_test.clj`' (chars 739-870)"]}
   ["eight-gates/roll-back-hold" 7]
   {:operation :hold-fallback-pending-phase-two
    :capability :wm/pattern-apply
    :consumes :unknown-normalized-external-ingress
    :produces :interim-fallback-contract-held-open
    :completion-check
    [:closure-held-until-adapters-or-formal-acceptance
     "'Close when Phase 2 surface adapters confirm external surfaces or accept the fallback formally.' (chars 1251-1345) keeps the fallback working without adopting it as final"]}
   ["eight-gates/roll-back-hold" 8]
   {:operation :submit-design-for-ratification-only
    :capability :wm/pattern-apply
    :consumes :model-authored-design-proposal
    :produces :ratification-pending-design-note
    :completion-check
    [:no-implementation-included-this-phase
     "status block reads 'Status: model-only proposal for ratification. No implementation is included in this phase.' (chars 1426-1517)"]}
   ["eight-gates/roll-back-hold" 9]
   {:operation :bound-exclusion-to-this-phase
    :capability :wm/pattern-apply
    :consumes :implementation-exclusion-statement
    :produces :phase-bounded-deferral
    :completion-check
    [:exclusion-ends-at-phase-boundary
     "closing fragment 'this phase' (chars 1506-1516) confines the excluded implementation to phase 1 only"]}})

(defn decisions
  "The hole resolutions for this requisition, bound to the cascade digest
  they repair.  Returns the decision map keyed by hole id when the context
  names this cascade, else a typed mismatch (never a silent wrong answer)."
  [{:keys [cascade-digest phase] :as _ctx}]
  (if (and (= cascade-digest
              "4bf6ef8c4352bd6c819aa907669ed8ce198a32707b36bbacf82d854435670418")
           (= :compile phase))
    clauses
    {:status :error
     :mismatch {:cascade-digest cascade-digest
                :phase phase
                :expected-cascade-digest "4bf6ef8c4352bd6c819aa907669ed8ce198a32707b36bbacf82d854435670418"
                :expected-phase :compile}}))
