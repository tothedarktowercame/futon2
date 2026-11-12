(ns futon2.aif.wm.authored.req-0b551c20
  "Authored operational clauses for held executable cascade
  59de556ef40a73c9b4f37199f104ee93f2574bda012a68544e6e00877b331f0f
  (phase :compile, requisition
  0b551c20a56b914588adc7a5670a841a8ac1e192594a27d7cb1bfb63b8fa508e).

  Resolutions for the six :operational-clause-owed holes of the
  T-ladder-04-materials-catalogue reading (analysis digest
  00dc6c9d36b013c46bcf48ae0358f262f8582d5ff8a16796a3249d8042e7ea89).
  Each decision is an operational clause attributed to the reading's
  occurrence squares over that ticket's acceptance item: the operation
  names what the occurrence enacts in the acceptance-section text, the
  completion-check names the observable condition in that text, and
  :consumes/:produces describe the transition.  Nothing here claims a
  square-derived completion and no application-order edge is authored."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["operator/accept-the-plan-as-stated" 0]
   {:operation :take-the-stated-checklist-as-the-acceptance-plan
    :capability :wm/pattern-apply
    :consumes :the-stated-acceptance-section-header
    :produces :checklist-accepted-plainly-with-its-limits
    :completion-check
    [:header-read-as-the-binding-checklist
     "the 'Acceptance criteria' header is taken plainly as the plan the item is judged against, its numeric limits included, with no amendment authored against it"]}
   ["pattern-mining/probe-the-claimed-property-not-the-acceptance-proxy" 1]
   {:operation :assert-exact-fixed-set-sizes-directly
    :capability :wm/pattern-apply
    :consumes :the-computed-fixed-point-sets
    :produces :tests-asserting-sizes-4-16-8-2-4-on-the-sets-themselves
    :completion-check
    [:tests-pinned-to-the-sets-not-a-proxy
     "the test suite asserts the cardinalities of the actual fixed sets (4/16/8/2/4) rather than a compiled or sampled stand-in, so the gate certifies the claimed property itself"]}
   ["test-registry/bind-warrant-to-the-diff" 2]
   {:operation :assert-union-size-against-the-enumerated-sets
    :capability :wm/pattern-apply
    :consumes :the-per-set-counts-and-the-union
    :produces :union-assertion-matching-the-sets-actually-enumerated
    :completion-check
    [:union-count-checked-as-its-own-assertion
     "'union size 16' is asserted as its own check so the per-set counts cannot silently warrant a different scope; only a complete match of counts and union is accepted"]}
   ["test-registry/spot-check-at-the-declared-rate" 3]
   {:operation :run-named-state-entropy-spot-checks
    :capability :wm/pattern-apply
    :consumes :the-entropy-computation-and-the-declared-value-list
    :produces :recorded-checks-of-77-0.694-78-141-0.406-90-178-0
    :completion-check
    [:declared-values-checked-at-three-decimals
     "the five named states (77, 78/141, 90/178) are checked at their declared rounded values, keeping the entropy record honest without claiming full coverage"]}
   ["agent/evidence-over-assertion" 4]
   {:operation :test-prove-the-absence-claim-for-113
    :capability :wm/pattern-apply
    :consumes :the-negative-existential-claim-about-state-113
    :produces :a-test-result-anchoring-the-absence
    :completion-check
    [:absence-backed-by-a-checkable-test
     "the claim that 113 has no bi-infinite quiescent word is closed only by a test result that points at the exhaustive evidence, never by assertion alone"]}
   ["operator/accept-the-plan-as-stated" 5]
   {:operation :accept-the-declared-outcome-as-the-closing-rule
    :capability :wm/pattern-apply
    :consumes :the-assertions-and-the-declared-expected-outcome
    :produces :item-closed-only-on-a-full-pass
    :completion-check
    [:closed-only-when-all-assertions-pass
     "the stated 'closed / pass' outcome is accepted plainly as the decision rule: the item closes exactly when every assertion above it passes, no other verdict path is authored"]}})

(defn decisions
  "The hole resolutions for this requisition, bound to the cascade digest
  they repair.  Returns the decision map keyed by hole id when the context
  names this cascade, else a typed mismatch (never a silent wrong answer)."
  [{:keys [cascade-digest phase] :as _ctx}]
  (if (and (= cascade-digest
              "59de556ef40a73c9b4f37199f104ee93f2574bda012a68544e6e00877b331f0f")
           (= :compile phase))
    clauses
    {:status :error
     :kind :patch-cascade-context-mismatch
     :cascade-digest cascade-digest
     :phase phase}))
