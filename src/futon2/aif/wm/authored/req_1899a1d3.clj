(ns futon2.aif.wm.authored.req-1899a1d3
  "Authored operational clauses for held executable cascade
  f326030db7c14e30990395487626787653773fc86bcec82ca7b4177f2713643b
  (phase :compile, requisition
  1899a1d30627e5d0ccb27d0fd31795f5e034ed87f181553ce7714d6cf5b6ab7d).

  Resolutions for the twelve :operational-clause-owed holes of the
  M-recommendation-bindings reading (policy-7e8a0806fea29fa47914,
  analysis digest 54558739922fa7db94f035fc7573d430033fbee7e87acce089da6b5f4ed38441).
  Each decision is an operational clause attributed to the reading's
  occurrence squares: the operation names what the occurrence enacts in
  the mission text (each span is a line of the mission header at HEAD,
  /home/joe/code/futon5a/holes/missions/M-recommendation-bindings.md),
  the completion-check names the observable condition in that span, and
  :consumes/:produces describe the transition.  Nothing here claims a
  square-derived completion and no application-order edge is authored."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["contracts/resolve-pointers-at-head" 0]
   {:operation :resolve-next-move-bindings-at-head
    :capability :wm/pattern-apply
    :consumes :mission-title-goal-span
    :produces :head-resolved-operational-surface-references
    :completion-check
    [:title-demands-operational-landing
     "mission title records 'make the War Machine's next-move land in operational surfaces', so every binding target must name a committed, inspectable surface"]}
   ["futon-theory/mission-lifecycle" 1]
   {:operation :record-explicit-derive-state
    :capability :wm/pattern-apply
    :consumes :mission-status-span
    :produces :recorded-derive-state
    :completion-check
    [:derive-state-dated
     "mission header records Status: OPEN — DERIVE (2026-04-26) as an explicit, dated lifecycle state"]}
   ["agent/handoff-preserves-context" 2]
   {:operation :record-owner-handoff-provenance
    :capability :wm/pattern-apply
    :consumes :mission-owner-span
    :produces :recorded-handoff-provenance
    :completion-check
    [:owner-names-handoff-source
     "owner line names 'Joe + agent (handoff from War Bulletin 8 conversation)' so the session context's origin survives the handoff"]}
   ["agent/evidence-over-assertion" 3]
   {:operation :cite-upstream-grounding-work
    :capability :wm/pattern-apply
    :consumes :war-bulletin-cross-reference-span
    :produces :anchored-upstream-reference
    :completion-check
    [:upstream-work-path-named
     "cross-ref cites futon3/holes/war-bulletin-8.md as the upstream work this mission grounds"]}
   ["agent/evidence-over-assertion" 4]
   {:operation :cite-instantiated-principles
    :capability :wm/pattern-apply
    :consumes :war-room-cross-reference-span
    :produces :anchored-principle-references
    :completion-check
    [:wr-sections-named
     "cross-ref names futon3/holes/war-room.md §WR-9 and §WR-10 as the principles this mission instantiates"]}
   ["contracts/resolve-pointers-at-head" 5]
   {:operation :extend-cached-prior-with-binds-to
    :capability :wm/pattern-apply
    :consumes :the-stack-reference-span
    :produces :binds-to-schema-extension-declared
    :completion-check
    [:binds-to-extension-named
     "cross-ref names futon5a/holes/stories/THE-STACK.aif.edn as the cached structural prior that needs a :binds-to extension at HEAD"]}
   ["agent/evidence-over-assertion" 6]
   {:operation :cite-invariant-family-inventory
    :capability :wm/pattern-apply
    :consumes :leaf-invariants-cross-reference-span
    :produces :anchored-family-inventory-reference
    :completion-check
    [:inventory-cited-with-content
     "cross-ref cites futon5a/holes/stories/leaf-invariants.aif.edn (216 lines — the AIF+ encoding of the 9 operational + 10 candidate invariant families)"]}
   ["contracts/resolve-pointers-at-head" 7]
   {:operation :bind-candidate-queue-at-head
    :capability :wm/pattern-apply
    :consumes :structural-law-inventory-reference-span
    :produces :head-committed-queue-binding
    :completion-check
    [:queue-source-of-truth-named
     "cross-ref names futon3c/docs/structural-law-inventory.sexp (798 lines) as the candidate-queue source-of-truth; registry shape"]}
   ["agent/evidence-over-assertion" 8]
   {:operation :cite-violation-emitting-runtime
    :capability :wm/pattern-apply
    :consumes :invariant-runtime-reference-span
    :produces :anchored-runtime-source-reference
    :completion-check
    [:runtime-files-named
     "cross-ref names futon3c/src/futon3c/aif/invariant.clj and futon3c/src/futon3c/logic/invariant_runner.clj as the runtime that emits violations"]}
   ["measurement/warrant-travels-with-the-number" 10]
   {:operation :warrant-proof-debt-counters
    :capability :wm/pattern-apply
    :consumes :boundary-counter-reference-span
    :produces :warranted-prototype-count
    :completion-check
    [:counter-rule-and-inputs-named
     "cross-ref names futon3/scripts/devmap_readiness.py and futon3/boundary.edn as the active per-prototype proof-debt counter, so each missing_evidence number carries its rule and inputs"]}
   ["measurement/warrant-travels-with-the-number" 11]
   {:operation :cite-posterior-construction-precedent
    :capability :wm/pattern-apply
    :consumes :aif-join-reference-span
    :produces :recorded-posterior-construction-rule
    :completion-check
    [:precedent-formula-recorded
     "cross-ref records futon0/web/war-machine/src/war_machine/client/aif_join.cljs as the precedent: cached prior + live evidence = empirical posterior"]}
   ["agent/evidence-over-assertion" 12]
   {:operation :declare-downstream-observation-target
    :capability :wm/pattern-apply
    :consumes :portfolio-inference-cross-reference-span
    :produces :recorded-observation-dependency
    :completion-check
    [:downstream-mission-named
     "cross-ref names futon3c/holes/missions/M-portfolio-inference.md as the next move whose target this mission unblocks meaningful observation of"]}})

(defn decisions
  "The hole resolutions for this requisition, bound to the cascade digest
  they repair.  Returns the decision map keyed by hole id when the context
  names this cascade, else a typed mismatch (never a silent wrong answer)."
  [{:keys [cascade-digest phase] :as _ctx}]
  (if (and (= cascade-digest
              "f326030db7c14e30990395487626787653773fc86bcec82ca7b4177f2713643b")
           (= :compile phase))
    clauses
    {:status :error
     :kind :patch-cascade-context-mismatch
     :cascade-digest cascade-digest
     :phase phase}))
