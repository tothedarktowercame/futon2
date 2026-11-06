(ns futon2.aif.wm.authored.req-c0a7396b
  "Authored operational clauses for held executable cascade
  defd441ec9b42332441c0afa09608dac505e0c94a646bfa0f572730d1e000f36
  (phase :compile, requisition
  c0a7396bc63b4fb1c71b492d090453d889649ecf95120b746b97f005eb59baef).

  Resolutions for the eight :operational-clause-owed holes of the
  M-webarxana reading (target futon4/holes/missions/M-webarxana.md,
  opening header).  Each decision is an operational clause attributed
  to the reading's occurrence squares over that header: the operation
  names what the occurrence enacts in the mission text,
  completion-check names the observable condition in that text, and
  :consumes/:produces describe the transition.  Nothing here claims a
  square-derived completion and no application-order edge is authored."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["gauntlet/world-is-hypergraph" 0]
   {:operation :frame-mission-as-shared-hypergraph-surface
    :capability :wm/pattern-apply
    :consumes :mission-title-declaring-hypergraph-scope
    :produces :declared-collaborative-surface-over-existing-futon-hypergraph
    :completion-check
    [:mission-title-names-collaborative-hypergraph-surface
     "mission header line 1 names the target 'Collaborative Hypergraph Surface' over the existing futon hypergraph, framing all later header fragments (futon4/holes/missions/M-webarxana.md, fragment 0-55)"]}
   ["storage/canonical-interface" 1]
   {:operation :anchor-mission-instantiation-date-in-record
    :capability :wm/pattern-apply
    :consumes :mission-opening-header-without-instantiation-anchor
    :produces :dated-readable-mission-contract-surface
    :completion-check
    [:header-records-instantiation-date
     "mission header records '**Date:** 2026-04-12' so mission state is recoverable from the document itself without scraping internals (futon4/holes/missions/M-webarxana.md, fragment 57-77)"]}
   ["inbox-zero/gate-fails-loudly" 2]
   {:operation :declare-status-with-enumerated-unmet-criteria
    :capability :wm/pattern-apply
    :consumes :mission-instantiated-with-testing-pass-done
    :produces :open-status-paired-with-acceptance-checklist-of-pending-items
    :completion-check
    [:open-status-line-sits-above-checklist-naming-unmet-items
     "status line 'OPEN — OPEN — INSTANTIATE (2026-04-12). Interactive testing pass done.' sits directly above an acceptance checklist (2026-09-30) with four unchecked items (deployment, per-pin hop depth, link-annotation persistence, compressed-diagram through-edge) (futon4/holes/missions/M-webarxana.md, fragment 78-160)"]}
   ["gauntlet/world-is-hypergraph" 3]
   {:operation :pin-prototype-capability-baseline-on-existing-substrate
    :capability :wm/pattern-apply
    :consumes :v1-prototype-feature-set-delivered
    :produces :declared-workbench-model-baseline-of-implemented-capabilities
    :completion-check
    [:header-enumerates-delivered-prototype-capabilities
     "header lists 'Multi-focus canvas, d3-force layout, diagrams, link annotations' matching the checked first acceptance item (18-test Playwright suite implemented) (futon4/holes/missions/M-webarxana.md, fragment 161-225)"]}
   ["inbox-zero/gate-fails-loudly" 4]
   {:operation :enumerate-residual-queue-next-to-status
    :capability :wm/pattern-apply
    :consumes :core-validation-complete-with-remaining-work
    :produces :pending-list-checkable-against-completion-criteria
    :completion-check
    [:pending-line-names-residual-work-under-open-status
     "header line 'Pending: deployment, some polish items.' names the residual queue directly under the OPEN status, checkable against completion criteria 3, 4 and 7 (futon4/holes/missions/M-webarxana.md, fragment 226-265)"]}
   ["storage/graph-memory-contract" 5]
   {:operation :assert-substrate-operational-before-surface-work
    :capability :wm/pattern-apply
    :consumes :candidate-blockers-futon1a-arxana-browser-prototype
    :produces :declared-blocker-free-state-grounded-in-operational-substrate
    :completion-check
    [:blocked-by-clause-names-operational-dependencies
     "header declares '**Blocked by:** None (futon1a operational, Arxana Browser operational,' grounding the no-blocker claim in substrate status (futon4/holes/missions/M-webarxana.md, fragment 266-336)"]}
   ["storage/canonical-interface" 6]
   {:operation :close-dependency-via-committed-artifact-and-documented-interfaces
    :capability :wm/pattern-apply
    :consumes :blocker-clause-naming-artifact-dependency
    :produces :dependency-closure-through-committed-prototype-and-http-api
    :completion-check
    [:prototype-commit-recorded-in-source-material
     "header closes the blocker clause with 'WebArxana v1 prototype committed)' and Source material names futon4/dev/web/webarxana/ (commit 4e5831f, 2026-04-12) and futon1a/src/futon1a/api/routes.clj as the interface surfaces (futon4/holes/missions/M-webarxana.md, fragment 337-370)"]}
   ["storage/graph-memory-contract" 7]
   {:operation :assign-ownership-with-store-side-contract-repair
    :capability :wm/pattern-apply
    :consumes :two-repo-ownership-split-surface-versus-store
    :produces :futon4-ownership-building-within-futon1a-type-system
    :completion-check
    [:owner-line-assigns-futon4-with-futon1a-dependency
     "header assigns '**Owner:** futon4 (WebArxana), with dependencies on futon1a (data store)' and Owner-and-dependencies section directs fixing futon1a props persistence rather than a separate metadata store (futon4/holes/missions/M-webarxana.md, fragment 371-443)"]}})

(defn decisions
  "The hole resolutions for this requisition, bound to the cascade digest
  they repair.  Returns the decision map keyed by hole id when the context
  names this cascade, else a typed mismatch (never a silent wrong answer)."
  [{:keys [cascade-digest phase] :as _ctx}]
  (if (and (= cascade-digest
              "defd441ec9b42332441c0afa09608dac505e0c94a646bfa0f572730d1e000f36")
           (= :compile phase))
    clauses
    {:status :error
     :kind :patch-cascade-context-mismatch
     :cascade-digest cascade-digest
     :phase phase}))
