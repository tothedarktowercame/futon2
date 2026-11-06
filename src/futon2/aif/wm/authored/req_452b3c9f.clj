(ns futon2.aif.wm.authored.req-452b3c9f
  "Authored operational clauses for held executable cascade
  af211b165707cdce80bd862b60bd9e6565e9ec2aa61861110e67158fed472dd9
  (phase :compile, requisition
  452b3c9f644031d70ef68e73b6bfdf87f47262171ddb39b993756b71932f0130).

  Resolutions for the eight :operational-clause-owed holes owned by
  :wm/enactment-compiler in this requisition.  Each decision is an
  operational clause attributed to the occurrence squares of the
  M-experiment-build-match reading (analysis digest
  5d2a69040f6a353c38cdaed6441c23c6836cb32c160b5d06457bb535f1e5c62c),
  whose spans index the mission opening at
  futon2/holes/M-experiment-build-match.md (title, dateline/roles,
  status-and-precedent line, cross-domain sentence): the operation names
  what the occurrence enacts in that text, completion-check names the
  observable condition there, and :consumes/:produces describe the
  transition.  No claim is made about holes outside this requisition and
  no application-order edge is authored."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["mission-coherence/logic-model-before-code" 0]
   {:operation :state-registration-parity-goal
    :capability :wm/pattern-apply
    :consumes :mission-title-goal-clause
    :produces :registration-parity-goal-record
    :completion-check
    [:title-names-fold-registration-as-oracle
     "title reads 'register experiments the way we already register folds' (excerpt chars 29-83), naming the existing fold registration as the conforming oracle"]}
   ["agent/scope-before-action" 0]
   {:operation :bind-mission-scope-to-title
    :capability :wm/pattern-apply
    :consumes :mission-title-line
    :produces :declared-mission-scope-record
    :completion-check
    [:single-bounded-aim-declared-before-any-code
     "the H1 (excerpt chars 0-83) fixes the in-bounds territory as experiment registration with the parity clause as exit condition, before any implementation exists (status SPEC)"]}
   ["agent/handoff-preserves-context" 1]
   {:operation :anchor-mission-identity-by-name
    :capability :wm/pattern-apply
    :consumes :mission-heading
    :produces :stable-mission-identifier-record
    :completion-check
    [:title-matches-repo-artifact-stem
     "heading '# M-experiment-build-match' (excerpt chars 0-26) matches the file stem M-experiment-build-match.md, so the anchor resolves without session memory"]}
   ["ways-of-working/operator-turns-logged" 2]
   {:operation :log-mission-opening-date
    :capability :wm/pattern-apply
    :consumes :mission-header-metadata-line
    :produces :dated-origin-record
    :completion-check
    [:opened-date-persisted-in-mission-record
     "header line reads '**Opened:** 2026-08-01.' (excerpt chars 85-108) in the committed mission document"]}
   ["agent/intent-handshake-is-binding" 3]
   {:operation :record-owner-and-operator-roles
    :capability :wm/pattern-apply
    :consumes :role-assignment-line
    :produces :binding-role-handshake-record
    :completion-check
    [:owner-and-operator-named-with-labels
     "line reads '**Owner:** Claude (claude-4). **Operator:** Joe.' (excerpt chars 109-157), pinning accountability in the immutable header"]}
   ["mission-coherence/logic-model-before-code" 4]
   {:operation :hold-mission-at-spec-stage
    :capability :wm/pattern-apply
    :consumes :mission-status-field
    :produces :pre-implementation-stage-marker
    :completion-check
    [:status-open-and-spec-declared
     "status reads '**Status:** OPEN — SPEC.' (excerpt chars 158-182), holding design verification ahead of code"]}
   ["agent/handoff-preserves-context" 5]
   {:operation :record-naming-precedent-decision
    :capability :wm/pattern-apply
    :consumes :naming-explanation-clause
    :produces :precedent-reference-record
    :completion-check
    [:deliberate-naming-names-sibling-document
     "line reads 'Deliberately named after `M-actuator-spec-build-match.md`,' (excerpt chars 183-241), recording the decision, its reason and a resolvable anchor"]}
   ["agent/handoff-preserves-context" 6]
   {:operation :assert-cross-domain-equivalence
    :capability :wm/pattern-apply
    :consumes :sibling-mission-reference
    :produces :scoped-reuse-licence-record
    :completion-check
    [:same-idea-scoped-to-one-domain-over
     "closing sentence reads 'which is the same idea one domain over.' (excerpt chars 242-281), licensing reuse with a named transfer distance"]}})

(defn decisions
  "The hole resolutions for this requisition, bound to the cascade digest
  they repair.  Returns the decision map keyed by hole id when the context
  names this cascade, else a typed mismatch (never a silent wrong answer)."
  [{:keys [cascade-digest phase] :as _ctx}]
  (if (and (= cascade-digest
              "af211b165707cdce80bd862b60bd9e6565e9ec2aa61861110e67158fed472dd9")
           (= :compile phase))
    clauses
    {:status :error
     :mismatch {:cascade-digest cascade-digest
                :phase phase
                :expected-cascade-digest "af211b165707cdce80bd862b60bd9e6565e9ec2aa61861110e67158fed472dd9"
                :expected-phase :compile}}))
