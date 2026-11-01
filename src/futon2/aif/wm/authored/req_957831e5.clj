(ns futon2.aif.wm.authored.req-957831e5
  "Authored operational clauses for held executable cascade
  5dd6db3d0ebc4b79eb3609a15d395a71064025cba4d3d35d093a06f1e0f546b2
  (phase :compile, requisition
  957831e56c499ccd95e27f85227edf10af35f50f28437ce1ddf841a78caa4f1e).

  Resolutions for the eight :operational-clause-owed holes of the
  M-interim-director-proxy-metric-inventory reading.  Each decision is an
  operational clause attributed to that reading's occurrence squares: the
  operation names what the occurrence enacts in the mission header text,
  completion-check names the observable condition in that text, and
  :consumes/:produces describe the transition.  Nothing here claims a
  square-derived completion and no application-order edge is authored."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["musn/declare-scope" 0]
   {:operation :declare-inventory-scope-in-title
    :capability :wm/pattern-apply
    :consumes :mission-title-fragment
    :produces :declared-scope-title
    :completion-check
    [:title-names-inventory-and-scaffold
     "mission title names the artefact as a proxy-metric inventory and value-generating arms scaffold under M-interim-director"]}
   ["futon-theory/mission-lifecycle" 1]
   {:operation :record-close-out-staging-revision
    :capability :wm/pattern-apply
    :consumes :dated-revision-line
    :produces :close-out-staging-state-record
    :completion-check
    [:close-out-staging-dated
     "mission date line records 'last updated 2026-05-19 — close-out staging'"]}
   ["agent/commitment-varies-with-confidence" 2]
   {:operation :hold-scaffold-as-head-escrow
    :capability :wm/pattern-apply
    :consumes :status-line-fragment
    :produces :head-escrow-scaffold-record
    :completion-check
    [:status-open-scaffold-escrow
     "mission status line reads 'OPEN — SCAFFOLD (pre-step-b HEAD-as-escrow artefact)'"]}
   ["futon-theory/mission-dependency" 3]
   {:operation :cite-parent-mission-document
    :capability :wm/pattern-apply
    :consumes :parent-mission-pointer
    :produces :cited-parent-mission
    :completion-check
    [:parent-cited-by-path
     "mission names /home/joe/code/futon7/holes/M-interim-director.md as its parent mission"]}
   ["futon-theory/coordination-protocol" 4]
   {:operation :link-companion-war-bulletin
    :capability :wm/pattern-apply
    :consumes :companion-pointer
    :produces :cited-companion-bulletin
    :completion-check
    [:companion-cited-by-path
     "mission names /home/joe/code/futon3/holes/war-bulletin-9.md as its companion artefact"]}
   ["agency/self-attribution" 5]
   {:operation :attribute-authorship-per-identity
    :capability :wm/pattern-apply
    :consumes :owner-line-fragment
    :produces :attribution-record
    :completion-check
    [:owner-and-author-named-separately
     "mission owner line attributes Joe as operator and claude-13 as author of the initial inventory, dated 2026-05-17"]}
   ["agency/single-routing-authority" 7]
   {:operation :designate-single-ingest-host-surface
    :capability :wm/pattern-apply
    :consumes :cross-ref-ingest-host-fragment
    :produces :ingest-host-binding
    :completion-check
    [:scan-surfaces-named-host
     "mission states the war-machine synthesis peripheral's scan-* surfaces will host the new ingest functions"]}
   ["futon-theory/mission-dependency" 8]
   {:operation :separate-substrate-and-mission-side-wiring
    :capability :wm/pattern-apply
    :consumes :wiring-contrast-fragment
    :produces :wiring-layer-separation-record
    :completion-check
    [:substrate-vs-mission-side-separated
     "mission contrasts M-war-machine-wiring (futon5a) as substrate-side wiring with this artefact as the mission-side wiring (proxy-metrics)"]}})

(defn decisions
  "The hole resolutions for this requisition, bound to the cascade digest
  they repair.  Returns the decision map keyed by hole id when the context
  names this cascade, else a typed mismatch (never a silent wrong answer)."
  [{:keys [cascade-digest phase] :as _ctx}]
  (if (and (= cascade-digest
              "5dd6db3d0ebc4b79eb3609a15d395a71064025cba4d3d35d093a06f1e0f546b2")
           (= :compile phase))
    clauses
    {:status :error
     :kind :patch-cascade-context-mismatch
     :cascade-digest cascade-digest
     :phase phase}))
