(ns futon2.aif.wm.authored.req-da880259
  "Authored operational clauses for held executable cascade
  47d66794861fe039b5a7d62184284c56b12e19fec3df43c3df03c1365a127dde
  (phase :compile, requisition
  da880259e12c491017fabe72df71233547a86d676d19a2134a4946e684088962).

  Resolutions for the two :operational-clause-owed holes of the
  T-ladder-06-layout-premise reading (both occurrences of
  pattern-mining/probe-the-claimed-property-not-the-acceptance-proxy).
  Each decision is an operational clause attributed to the occurrence
  squares: the operation names the direct property probe the occurrence
  enacts, completion-check names the observable evidence condition in the
  ticket text, and :consumes/:produces describe the transition.  No engine
  choice is authored: the ladder 7-8 engine decision stays with Joe."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["pattern-mining/probe-the-claimed-property-not-the-acceptance-proxy" 0]
   {:operation :probe-genotype-step-invariance-directly
    :capability :wm/pattern-apply
    :consumes :claimed-invariance-premise
    :produces :interface-blend-evidence
    :completion-check
    [:noninvariance-evidence-with-named-blends
     "evidence records that genotype-step is not invariant on the 77|90|178 rule field under even8, producing interface blends 95 and 250"]}
   ["pattern-mining/probe-the-claimed-property-not-the-acceptance-proxy" 1]
   {:operation :probe-propagator-preservation-directly
    :capability :wm/pattern-apply
    :consumes :claimed-preservation-property
    :produces :exact-preservation-record
    :completion-check
    [:propagator-only-field-preserved-exactly
     "evidence records that propagator-only steps preserve the 77|90|178 field exactly, isolating genotype-step as the blend source before stopping for Joe's ruling on ladders 7-8"]}})

(defn decisions
  "The hole resolutions for this requisition, bound to the cascade digest
  they repair.  Returns the decision map keyed by hole id when the context
  names this cascade, else a typed mismatch (never a silent wrong answer)."
  [{:keys [cascade-digest phase] :as _ctx}]
  (if (and (= cascade-digest
              "47d66794861fe039b5a7d62184284c56b12e19fec3df43c3df03c1365a127dde")
           (= :compile phase))
    clauses
    {:status :error
     :kind :patch-cascade-context-mismatch
     :cascade-digest cascade-digest
     :phase phase}))
