(ns futon2.aif.wm.authored.req-f7374bcce
  "Authored operational clauses for held executable cascade
  8d37d377a381625761650d22c4f3a83f7f27cf9320a0b4cb43937147b9f7d907
  (phase :compile, requisition f7374bcceac03b0bde21d32fa2c10df8edeffb0e8482acf6bfcf9d11e0a50414).

  Resolutions for the twenty-seven :operational-clause-owed holes of the
  M-war-machine-wiring reading (policy-2629917c87a61978be8f, analysis
  digest c8402106cd699388b85a9387c851b2a45fd28e0cdf6445524f21fc236efa72a7).
  Each decision is an operational clause attributed to the reading's
  occurrence squares: the operation names what the occurrence enacts in the
  mission text, completion-check names the observable condition in that
  text, and :consumes/:produces describe the transition.  Nothing here
  claims a square-derived completion and no application-order edge is
  authored."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["gauntlet/world-is-hypergraph" 0]
   {:operation :state-mission-identity-as-bridge
    :capability :wm/pattern-apply
    :consumes :mission-title-line
    :produces :mission-identity-vertex
    ;; Executable form (R8b, decision sheet 2026-10-26 item 1 (a)). The prose
    ;; this replaces read "mission title names 'War Machine Wiring' as the
    ;; last-mile bridge to M-live-geometric-stack"; that condition is the
    ;; mission's title line, read as a C4 declaration head at the after
    ;; revision by futon2.aif.completion-check/evaluate. The other
    ;; twenty-six clauses stay prose and stay typed missing.
    :completion-check
    [:artifact-locator
     {:class :C4 :repo "futon5a"
      :path "holes/missions/M-war-machine-wiring.md"
      :decl "# Mission: War Machine Wiring (last-mile bridge to M-live-geometric-stack)"}]}
   ["futon-theory/mission-lifecycle" 1]
   {:operation :record-open-verify-status-with-checkpoints
    :capability :wm/pattern-apply
    :consumes :mission-status-block
    :produces :dated-verify-status-record
    :completion-check
    [:status-open-verify-dated
     "mission status block reads 'OPEN — VERIFY (2026-05-01)' and names Checkpoint 1, the V-1 substrate-2 coverage pre-condition, and the D-7.5 vocabulary refinements"]}
   ["agent/handoff-preserves-context" 2]
   {:operation :record-joint-ownership
    :capability :wm/pattern-apply
    :consumes :mission-ownership-line
    :produces :joint-owner-record
    :completion-check
    [:owner-field-present
     "mission carries 'Owner: Joe + Claude'"]}
   ["gauntlet/world-is-hypergraph" 3]
   {:operation :map-repositories-into-world-vertices
    :capability :wm/pattern-apply
    :consumes :repository-listing-line
    :produces :repository-role-map
    :completion-check
    [:repos-line-with-roles
     "mission 'Repos:' line assigns each of futon5a, futon3c, futon3, futon2, futon5 a parenthesized role"]}
   ["agent/evidence-over-assertion" 4]
   {:operation :cite-head-mission-by-path
    :capability :wm/pattern-apply
    :consumes :head-mission-reference
    :produces :cited-head-mission
    :completion-check
    [:head-cited-by-path
     "mission cites futon3c/holes/missions/M-war-machine.md as 'the head: move-selection'"]}
   ["war-machine/state-capture" 5]
   {:operation :capture-head-dispatch-responsibilities
    :capability :wm/pattern-apply
    :consumes :head-responsibility-lines
    :produces :head-state-record
    :completion-check
    [:head-responsibilities-named
     "mission names the head's policy, bidding, Tick/Click dispatch and recurring AIF tick"]}
   ["agent/evidence-over-assertion" 6]
   {:operation :cite-tuning-mission-by-path
    :capability :wm/pattern-apply
    :consumes :tuning-mission-reference
    :produces :cited-tuning-mission
    :completion-check
    [:tuning-cited-by-path
     "mission cites futon3c/holes/missions/M-war-machine-tuning.md as the head's policy"]}
   ["war-machine/state-capture" 7]
   {:operation :record-tuning-surface-role
    :capability :wm/pattern-apply
    :consumes :tuning-surface-fragment
    :produces :tuning-surface-state
    :completion-check
    [:tuning-surface-named
     "mission completes the tuning citation with 'tuning surface'"]}
   ["gauntlet/world-is-hypergraph" 8]
   {:operation :cite-manifold-mission-as-vertex
    :capability :wm/pattern-apply
    :consumes :manifold-mission-reference
    :produces :cited-manifold-vertex
    :completion-check
    [:manifold-cited-by-path
     "mission cites futon3/holes/missions/M-live-geometric-stack.md as 'the manifold'"]}
   ["measurement/warrant-travels-with-the-number" 9]
   {:operation :bind-manifold-observables-to-citation
    :capability :wm/pattern-apply
    :consumes :manifold-observable-listing
    :produces :warranted-observable-set
    :completion-check
    [:observables-travel-with-manifold
     "mission binds the live observables (T, ∇, Δ, drift) to the futon-stack substrate in the manifold citation"]}
   ["agent/evidence-over-assertion" 10]
   {:operation :cite-pair-mission-by-path
    :capability :wm/pattern-apply
    :consumes :pair-mission-reference
    :produces :cited-pair-mission
    :completion-check
    [:diagnostic-cited-by-path
     "mission cites futon3/holes/missions/M-pattern-application-diagnostic.md"]}
   ["hdm/deep-storage-to-active-graph" 11]
   {:operation :surface-pair-mission-boundary-interface
    :capability :wm/pattern-apply
    :consumes :pair-mission-fragment
    :produces :boundary-growth-interface
    :completion-check
    [:interface-named
     "mission names substrate-2's pair-mission and its boundary-growth interface"]}
   ["agent/evidence-over-assertion" 12]
   {:operation :cite-aif-engine-doc-by-path
    :capability :wm/pattern-apply
    :consumes :aif-mechanics-reference
    :produces :cited-aif-engine-doc
    :completion-check
    [:doc-cited-by-path
     "mission cites futon2/doc/aif-engine.md for AIF mechanics"]}
   ["agent/evidence-over-assertion" 13]
   {:operation :cite-aif-ants-source-with-tests
    :capability :wm/pattern-apply
    :consumes :ants-source-reference
    :produces :cited-ants-implementation
    :completion-check
    [:ants-cited-with-tests
     "mission cites futon2/src/ants/clj_ants_aif.clj and its tests under futon2/test/ants/"]}
   ["war-machine/state-capture" 14]
   {:operation :record-ants-reference-role
    :capability :wm/pattern-apply
    :consumes :ants-fragment
    :produces :foraging-reference-state
    :completion-check
    [:ants-role-named
     "mission names the AIF ants implementation 'the autonomous-foraging reference'"]}
   ["agent/evidence-over-assertion" 15]
   {:operation :cite-head-specific-aif-framings
    :capability :wm/pattern-apply
    :consumes :head-aif-references
    :produces :cited-head-framings
    :completion-check
    [:framings-cited-by-path
     "mission cites futon2/holes/M-aif-head.md and M-aif4iad.md as head-specific AIF framings"]}
   ["agent/evidence-over-assertion" 16]
   {:operation :complete-framing-citation
    :capability :wm/pattern-apply
    :consumes :framing-fragment
    :produces :completed-framing-citation
    :completion-check
    [:framings-word-present
     "mission completes the framing citation with the word 'framings'"]}
   ["agent/evidence-over-assertion" 17]
   {:operation :cite-canonical-wiring-diagram-exposition
    :capability :wm/pattern-apply
    :consumes :exposition-reference
    :produces :cited-canonical-exposition
    :completion-check
    [:chapter0-cited-as-canonical
     "mission cites futon5/docs/chapter0-aif-as-wiring-diagram.md as 'the canonical' exposition"]}
   ["gauntlet/world-is-hypergraph" 18]
   {:operation :record-visual-idiom-edge
    :capability :wm/pattern-apply
    :consumes :exposition-fragment
    :produces :visual-idiom-edge
    :completion-check
    [:idiom-described
     "mission describes the 'AIF as wiring diagram' exposition as the visual idiom an autonomous WM should emit"]}
   ["gauntlet/world-is-hypergraph" 19]
   {:operation :bind-idiom-to-mission-emission
    :capability :wm/pattern-apply
    :consumes :idiom-fragment
    :produces :per-mission-emission-binding
    :completion-check
    [:emission-per-mission
     "mission closes the idiom line with 'WM should emit per mission'"]}
   ["hdm/deep-storage-to-active-graph" 20]
   {:operation :surface-existing-ants-capture
    :capability :wm/pattern-apply
    :consumes :ants-data-reference
    :produces :captured-ants-data
    :completion-check
    [:capture-cited-by-path
     "mission cites futon5/data/missions/futon2-aif-ants.edn as 'existing capture of' AIF-ants mission data"]}
   ["hdm/deep-storage-to-active-graph" 21]
   {:operation :bind-capture-to-its-data
    :capability :wm/pattern-apply
    :consumes :capture-fragment
    :produces :bound-ants-capture
    :completion-check
    [:capture-subject-named
     "mission completes the capture citation with 'AIF-ants mission data'"]}
   ["agent/evidence-over-assertion" 22]
   {:operation :cite-stereolithography-checkpoint
    :capability :wm/pattern-apply
    :consumes :checkpoint-reference
    :produces :cited-checkpoint
    :completion-check
    [:checkpoint-cited-by-section
     "mission cites futon5a/holes/missions/M-stack-stereolithography.md § Checkpoint 6"]}
   ["war-machine/state-capture" 23]
   {:operation :record-piano-roll-trace-role
    :capability :wm/pattern-apply
    :consumes :piano-roll-fragment
    :produces :piano-roll-trace-state
    :completion-check
    [:piano-roll-described
     "mission describes the piano roll as the head's post-hoc trace projected onto the substrate"]}
   ["measurement/warrant-travels-with-the-number" 24]
   {:operation :bind-trace-projection-coordinates
    :capability :wm/pattern-apply
    :consumes :projection-fragment
    :produces :warranted-projection
    :completion-check
    [:projection-coordinates-named
     "mission names the projection coordinates as (substrate × time)"]}
   ["agent/evidence-over-assertion" 25]
   {:operation :cite-prototype-renderer-with-artifacts
    :capability :wm/pattern-apply
    :consumes :renderer-reference
    :produces :cited-prototype-renderer
    :completion-check
    [:renderer-and-artifacts-cited
     "mission cites futon5a/scripts/piano_roll.py and /tmp/piano-roll-v0.{pdf,png} as the prototype trace renderer"]}
   ["war-machine/state-capture" 26]
   {:operation :record-prototype-renderer-state
    :capability :wm/pattern-apply
    :consumes :renderer-fragment
    :produces :prototype-renderer-state
    :completion-check
    [:prototype-named
     "mission completes the renderer citation with 'prototype trace renderer'"]}})

(defn decisions
  "The hole resolutions for this requisition, bound to the cascade digest
  they repair.  Returns the decision map keyed by hole id when the context
  names this cascade, else a typed mismatch (never a silent wrong answer)."
  [{:keys [cascade-digest phase] :as _ctx}]
  (if (and (= cascade-digest
              "8d37d377a381625761650d22c4f3a83f7f27cf9320a0b4cb43937147b9f7d907")
           (= :compile phase))
    clauses
    {:status :error
     :kind :patch-cascade-context-mismatch
     :cascade-digest cascade-digest
     :phase phase}))
