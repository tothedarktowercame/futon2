(ns futon2.aif.wm.authored.req-1ed21d52
  "Authored operational clauses for held executable cascade
  633a3c9f5f6c399fc15d975ba0a848801a60234386ca66f26a493cfa00b18705
  (phase :compile, requisition
  1ed21d52ba744af4e57d7277b991e7c0e500f22e6bd275a3660eb305436c292f).

  Resolutions for the twenty-one :operational-clause-owed holes of the
  M-pattern-retrieval-calibration reading.  Each decision is an operational
  clause attributed to the reading's occurrence squares: the operation names
  what the occurrence enacts in the mission text, completion-check names the
  observable condition in that text, and :consumes/:produces describe the
  transition.  Nothing here claims a square-derived completion and no
  application-order edge is authored."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["problems/most-patterns-have-no-globally-recognised-rationale" 0]
   {:operation :frame-calibration-mission-title
    :capability :wm/pattern-apply
    :consumes :mission-title-fragment
    :produces :calibration-mission-naming
    :completion-check
    [:title-names-retrieval-calibration
     "mission opening names the mission 'Pattern Retrieval Calibration'"]}
   ["problems/most-patterns-have-no-globally-recognised-rationale" 1]
   {:operation :record-open-derive-status
    :capability :wm/pattern-apply
    :consumes :mission-status-line
    :produces :open-derive-status-record
    :completion-check
    [:status-open-derive
     "mission status line reads 'OPEN — DERIVE' with entry, MAP and IDENTIFY dates recorded"]}
   ["problems/most-patterns-have-no-globally-recognised-rationale" 2]
   {:operation :record-mission-owner
    :capability :wm/pattern-apply
    :consumes :owner-line
    :produces :owner-attribution-record
    :completion-check
    [:owner-named-joe
     "mission owner line names Joe"]}
   ["cascade-construction/read-what-exists-first" 3]
   {:operation :open-cross-reference-index
    :capability :wm/pattern-apply
    :consumes :cross-refs-heading
    :produces :cross-reference-list-introduced
    :completion-check
    [:cross-refs-heading-present
     "mission carries a '**Cross-refs:**' heading introducing the linked missions and excursions"]}
   ["problems/llm-over-corpus-mining-lessons" 4]
   {:operation :cite-mining-mission-finding
    :capability :wm/pattern-apply
    :consumes :mining-cross-reference
    :produces :cited-mining-finding
    :completion-check
    [:mining-mission-cited
     "mission cross-references holes/missions/M-pattern-mining.md as having established the current-substrate finding"]}
   ["problems/r7-evidence-channel-precision" 5]
   {:operation :record-substrate-artifact-risk
    :capability :wm/pattern-apply
    :consumes :substrate-finding-fragment
    :produces :channel-precision-demotion-evidence
    :completion-check
    [:substrate-called-artifact-prone
     "mission states the futon3a MiniLM-cosine substrate is 'artifact-prone and unsafe to treat'"]}
   ["problems/r7-evidence-channel-precision" 6]
   {:operation :deny-clean-sensor-claim
    :capability :wm/pattern-apply
    :consumes :sensor-claim-fragment
    :produces :clean-sensor-denial
    :completion-check
    [:clean-sensor-denied
     "mission completes the finding: the substrate is unsafe to treat 'as a clean sensor'"]}
   ["pattern-mining/probe-the-claimed-property-not-the-acceptance-proxy" 9]
   {:operation :state-application-quality-bar
    :capability :wm/pattern-apply
    :consumes :application-requirement-fragment
    :produces :checkable-application-bar
    :completion-check
    [:bar-terms-named
     "mission requires pattern application be 'well-specified, repeatable, and demonstrably useful'"]}
   ["cascade-construction/read-what-exists-first" 10]
   {:operation :cite-companion-excursion
    :capability :wm/pattern-apply
    :consumes :peripheral-excursion-reference
    :produces :cited-companion-excursion
    :completion-check
    [:pattern-peripheral-cited
     "mission cross-references holes/excursions/E-pattern-peripheral.md as the companion Codex-handoff excursion"]}
   ["problems/most-patterns-have-no-globally-recognised-rationale" 11]
   {:operation :record-symmetric-gate-relation
    :capability :wm/pattern-apply
    :consumes :symmetry-fragment
    :produces :creation-retrieval-symmetry-record
    :completion-check
    [:creation-side-gate-named-symmetric
     "mission names the excursion as 'the symmetric *creation*-side gate to this mission's *retrieval*-side calibration'"]}
   ["problems/r12-two-layer-calibration" 12]
   {:operation :scope-retrieval-side-calibration
    :capability :wm/pattern-apply
    :consumes :retrieval-side-fragment
    :produces :retrieval-side-scope-record
    :completion-check
    [:retrieval-side-named
     "mission names this mission as the '*retrieval*-side calibration' half of the gate"]}
   ["cascade-construction/read-what-exists-first" 13]
   {:operation :record-parser-blocker
    :capability :wm/pattern-apply
    :consumes :blocker-fragment
    :produces :p1-dependency-record
    :completion-check
    [:blocks-on-p1
     "mission records 'Blocks on P-1 (canonical parser at futon3a/src/futon/flexiarg/projection.clj)'"]}
   ["cascade-construction/read-what-exists-first" 14]
   {:operation :locate-parser-by-path
    :capability :wm/pattern-apply
    :consumes :parser-path-fragment
    :produces :parser-location-record
    :completion-check
    [:projection-path-cited
     "mission cites futon3a/src/futon/flexiarg/projection.clj and notes the gate lands after it ships"]}
   ["problems/llm-over-corpus-mining-lessons" 15]
   {:operation :record-advisory-first-staging
    :capability :wm/pattern-apply
    :consumes :staging-fragment
    :produces :advisory-mode-record
    :completion-check
    [:advisory-mode-first
     "mission stages the gate to land 'advisory mode first'"]}
   ["pattern-mining/probe-the-claimed-property-not-the-acceptance-proxy" 16]
   {:operation :gate-enforcement-on-violation-corpus
    :capability :wm/pattern-apply
    :consumes :enforcement-fragment
    :produces :violation-corpus-dependency-record
    :completion-check
    [:enforced-after-corpus-exists
     "mission defers enforced mode until 'a violation corpus exists'"]}
   ["cascade-construction/read-what-exists-first" 17]
   {:operation :cite-btoa-complement-mission
    :capability :wm/pattern-apply
    :consumes :trip-journal-reference
    :produces :cited-btoa-complement
    :completion-check
    [:trip-journal-cited
     "mission cross-references futon5a/holes/missions/M-trip-journal.md as 'the B→A complement'"]}
   ["problems/llm-over-corpus-mining-lessons" 18]
   {:operation :constrain-btoa-learnable-form
    :capability :wm/pattern-apply
    :consumes :transfer-fragment
    :produces :learnable-form-constraint
    :completion-check
    [:btoa-consumable-form-required
     "mission requires improving 'the A→B side in a form that the B→A side can actually learn from'"]}
   ["problems/llm-over-corpus-mining-lessons" 19]
   {:operation :state-learnability-bar
    :capability :wm/pattern-apply
    :consumes :learnability-fragment
    :produces :learnability-acceptance-shape
    :completion-check
    [:actually-learn-from-required
     "mission closes the constraint with 'actually learn from.'"]}
   ["cascade-construction/read-what-exists-first" 20]
   {:operation :cite-queue-reweighting-excursion
    :capability :wm/pattern-apply
    :consumes :reweighting-reference
    :produces :cited-queue-reweighting-excursion
    :completion-check
    [:queue-reweighting-cited
     "mission cross-references E-btoa-candidate-queue-reweighting-v0.md"]}
   ["cascade-construction/read-what-exists-first" 21]
   {:operation :cite-evidence-sample-mapping-excursion
    :capability :wm/pattern-apply
    :consumes :sample-mapping-reference
    :produces :cited-sample-mapping-excursion
    :completion-check
    [:sample-mapping-cited-with-phase
     "mission cross-references E-btoa-evidence-sample-mapping-v0.md as 'phase-1 B→A/SORRY' work"]}
   ["pattern-mining/probe-the-claimed-property-not-the-acceptance-proxy" 22]
   {:operation :record-hand-labeled-sample-artifact
    :capability :wm/pattern-apply
    :consumes :sample-fragment
    :produces :hand-labeled-sample-record
    :completion-check
    [:first-hand-labeled-sample-named
     "mission names the phase-1 weighting and 'the first hand-labeled sample'"]}})

(defn decisions
  "The hole resolutions for this requisition, bound to the cascade digest
  they repair.  Returns the decision map keyed by hole id when the context
  names this cascade, else a typed mismatch (never a silent wrong answer)."
  [{:keys [cascade-digest phase] :as _ctx}]
  (if (and (= cascade-digest
              "633a3c9f5f6c399fc15d975ba0a848801a60234386ca66f26a493cfa00b18705")
           (= :compile phase))
    clauses
    {:status :error
     :kind :patch-cascade-context-mismatch
     :cascade-digest cascade-digest
     :phase phase}))
