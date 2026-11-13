(ns futon2.aif.wm.authored.req-fc2b99ce
  "Authored operational clauses for held executable cascade
  5943ab7e2797ff87a572ec8de98408581be6142d28ad51b7de8cf23328eeb48e
  (phase :compile, requisition
  fc2b99ce64b22ae7109ac20a02fdf071d2d0df28923ca40d2e2ac98c874f2163).

  Resolutions for the nine :operational-clause-owed holes of the
  T-ladder-05-settling-confinement reading (analysis digest
  ba327e4d3903b6f025a5d7e647792472e36d7505319bbbb4afb7c462bd878dda).
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
     "the 'Acceptance criteria' header is taken plainly as the plan the item is judged against, its exhaustive scope and refuted-claim-allowed limit included, with no amendment authored against it"]}
   ["pattern-mining/probe-the-claimed-property-not-the-acceptance-proxy" 1]
   {:operation :enumerate-every-ring-deterministically-per-n
    :capability :wm/pattern-apply
    :consumes :the-tns-settling-and-confinement-claims-as-checkable-questions
    :produces :a-per-n-record-over-the-full-ring-set-not-a-sample
    :completion-check
    [:exhaustive-probe-not-a-proxy
     "a deterministic test enumerates every starting ring for each n in the stated range and records outcomes per n, so the gate certifies the claimed property itself rather than a sampled or narrowed stand-in"]}
   ["math-informal/failure-mode-characterization" 2]
   {:operation :record-the-four-settling-statistics-per-n
    :capability :wm/pattern-apply
    :consumes :the-per-n-enumeration-results
    :produces :maximum-transient-nonsettling-count-worst-start-and-damage-depth
    :completion-check
    [:all-four-regime-quantities-recorded
     "each n's record carries the maximum transient, the number of rings that never reach a fixed point, a worst-case starting ring, and the maximum damage depth, characterising the failure regime rather than a bare pass/fail"]}
   ["math-informal/construct-an-explicit-witness" 3]
   {:operation :record-each-claim-verdict-with-its-smallest-counterexample
    :capability :wm/pattern-apply
    :consumes :the-two-tn-claims-and-the-enumeration-record
    :produces :upheld-or-refuted-per-claim-with-a-minimal-witness-when-refuted
    :completion-check
    [:refutation-carries-an-explicit-minimal-witness
     "each of the TN's two claims is recorded as upheld or refuted, and a refuted verdict names the smallest counterexample starting ring, never a bare failure flag"]}
   ["operator/accept-the-plan-as-stated" 4]
   {:operation :accept-the-no-weakening-constraints-as-stated
    :capability :wm/pattern-apply
    :consumes :the-announced-scope-of-n-ring-set-and-settles-definition
    :produces :criteria-held-unamended-with-refuted-allowed-as-a-result
    :completion-check
    [:no-narrowing-excluding-or-redefining-authored
     "the range of n is not narrowed, no rings are excluded, and 'settles' is not redefined to obtain a pass; a refuted claim is accepted as the result, so no weakening amendment is authored"]}
   ["operator/mark-the-standing-of-the-claim" 5]
   {:operation :declare-and-check-the-expected-claim-standing
    :capability :wm/pattern-apply
    :consumes :the-declared-expected-outcome-for-both-claims
    :produces :verified-standing-refuted-settling-upheld-confinement
    :completion-check
    [:expected-standing-tested-against-the-record
     "the stated expectation (closed / settling claim refuted, confinement claim upheld) is treated as a reading to check: the record must confirm exactly that standing, and any difference is reported"]}
   ["agent/evidence-over-assertion" 6]
   {:operation :anchor-expected-values-to-the-cited-independent-enumeration
    :capability :wm/pattern-apply
    :consumes :the-claude-4-reference-enumeration-values
    :produces :cross-checkable-transient-and-worst-start-expectations
    :completion-check
    [:numbers-anchored-to-the-reference-not-self-consistency
     "the expected values (transient floor(n/2)-1, first exceeding 6 at n = 16, 9 at n = 20, worst start a single 1) are checked against the cited independent enumeration, not merely the test's own internal consistency"]}
   ["math-informal/exploit-symmetry" 7]
   {:operation :identify-the-uniform-rings-as-the-nonsettling-orbit
    :capability :wm/pattern-apply
    :consumes :the-per-n-enumeration-of-nonsettling-rings
    :produces :the-two-uniform-rings-named-as-the-complete-nonsettling-set
    :completion-check
    [:nonsettling-set-counted-by-symmetry-not-elementwise
     "the record shows exactly two rings per n, the uniform ones, never settle, so the nonsettling set is characterised by its symmetry class rather than an unstructured list"]}
   ["math-informal/check-the-extreme-cases" 8]
   {:operation :pin-damage-depth-across-the-full-n-range
    :capability :wm/pattern-apply
    :consumes :the-damage-depth-measurements-per-n
    :produces :depth-1-confirmed-at-every-n-from-4-to-20
    :completion-check
    [:bound-inclusive-of-both-range-endpoints
     "damage depth is recorded as 1 for every n = 4..20, both endpoints included, so the confinement bound is not established from comfortable mid-range points alone"]}})

(defn decisions
  "The hole resolutions for this requisition, bound to the cascade digest
  they repair.  Returns the decision map keyed by hole id when the context
  names this cascade, else a typed mismatch (never a silent wrong answer)."
  [{:keys [cascade-digest phase] :as _ctx}]
  (if (and (= cascade-digest
              "5943ab7e2797ff87a572ec8de98408581be6142d28ad51b7de8cf23328eeb48e")
           (= :compile phase))
    clauses
    {:status :error
     :kind :patch-cascade-context-mismatch
     :cascade-digest cascade-digest
     :phase phase}))
