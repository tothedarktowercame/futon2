(ns futon2.aif.wm.authored.req-7720426a
  "Authored operational clauses for held executable cascade
  db0af237b84244d7003cfdaf3f61edb9e93403755c04edf7e7640f4283d807b4
  (phase :compile, requisition 7720426a7979f6a429a730c028cb9cc0cfc6778c97d871f86fda3b992623f1f4).

  Resolutions for the twenty-one :operational-clause-owed holes of the
  M-pattern-retrieval-calibration reading (policy
  M-pattern-retrieval-calibration/policy-bd8f7522f4d4f0b4296f, analysis
  digest 1454ee082c62a38019403be2d61e57749e54c9eecab17ed3557f8b0241220e2b).
  Each decision is an operational clause attributed to the reading's
  occurrence squares, whose spans index the canonical mission text at
  futon3/holes/missions/M-pattern-retrieval-calibration.md (header and
  cross-refs section): the operation names what the occurrence enacts in
  that text, completion-check names the observable condition there, and
  :consumes/:produces describe the transition.  Nothing here claims a
  square-derived completion and no application-order edge is authored."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["problems/r12-two-layer-calibration" 0]
   {:operation :bind-goal-to-calibration-title
    :capability :wm/pattern-apply
    :consumes :mission-title-line
    :produces :calibration-goal-record
    :completion-check
    [:title-names-pattern-retrieval-calibration
     "mission title line reads '# Mission: Pattern Retrieval Calibration'"]}
   ["problems/most-patterns-have-no-globally-recognised-rationale" 1]
   {:operation :record-derive-phase-status
    :capability :wm/pattern-apply
    :consumes :mission-status-line
    :produces :derive-status-record
    :completion-check
    [:status-open-derive
     "mission status line reads 'OPEN — DERIVE'"]}
   ["problems/most-patterns-have-no-globally-recognised-rationale" 2]
   {:operation :record-operator-ownership
    :capability :wm/pattern-apply
    :consumes :owner-line
    :produces :owner-record
    :completion-check
    [:owner-named-joe
     "mission names Joe as owner ('**Owner:** Joe')"]}
   ["cascade-construction/read-what-exists-first" 3]
   {:operation :enumerate-cross-references
    :capability :wm/pattern-apply
    :consumes :mission-header
    :produces :cross-refs-listing
    :completion-check
    [:cross-refs-section-present
     "mission opens a '**Cross-refs:**' listing before further claims"]}
   ["problems/llm-over-corpus-mining-lessons" 4]
   {:operation :cite-pattern-mining-finding
    :capability :wm/pattern-apply
    :consumes :cross-ref-entry
    :produces :cited-artifact-proneness
    :completion-check
    [:mining-mission-establishes-artifact-proneness
     "mission cites M-pattern-mining.md as having established the current substrate artifact-prone"]}
   ["problems/r7-evidence-channel-precision" 5]
   {:operation :name-minilm-cosine-substrate-defect
    :capability :wm/pattern-apply
    :consumes :substrate-description
    :produces :artifact-proneness-record
    :completion-check
    [:minilm-cosine-named-artifact-prone
     "mission states the futon3a MiniLM-cosine substrate is artifact-prone and unsafe to treat as a clean sensor"]}
   ["problems/r7-evidence-channel-precision" 6]
   {:operation :record-clean-sensor-unavailability
    :capability :wm/pattern-apply
    :consumes :sensor-status-fragment
    :produces :uncalibrated-sensor-record
    :completion-check
    [:clean-sensor-withdrawn
     "mission concludes it is 'unsafe to treat as a clean sensor'"]}
   ["pattern-mining/probe-the-claimed-property-not-the-acceptance-proxy" 9]
   {:operation :state-typed-application-requirement
    :capability :wm/pattern-apply
    :consumes :diagnostic-mission-reference
    :produces :application-requirement-record
    :completion-check
    [:application-well-specified-repeatable-useful
     "mission requires pattern application be well-specified, repeatable, and demonstrably useful"]}
   ["cascade-construction/read-what-exists-first" 10]
   {:operation :cite-pattern-peripheral-excursion
    :capability :wm/pattern-apply
    :consumes :excursion-reference
    :produces :cited-creation-side-gate
    :completion-check
    [:excursion-cited-by-path
     "mission cites E-pattern-peripheral.md as the companion Codex-handoff excursion"]}
   ["problems/most-patterns-have-no-globally-recognised-rationale" 11]
   {:operation :record-creation-retrieval-symmetry
    :capability :wm/pattern-apply
    :consumes :symmetry-fragment
    :produces :symmetric-gate-record
    :completion-check
    [:creation-side-gate-named-symmetric
     "mission names the excursion the symmetric creation-side gate to this mission's retrieval-side calibration"]}
   ["problems/r12-two-layer-calibration" 12]
   {:operation :bind-retrieval-side-calibration-role
    :capability :wm/pattern-apply
    :consumes :calibration-role-fragment
    :produces :retrieval-side-calibration-binding
    :completion-check
    [:mission-is-retrieval-side
     "mission marks itself the retrieval-side calibration counterpart"]}
   ["cascade-construction/read-what-exists-first" 13]
   {:operation :record-p1-shipping-block
    :capability :wm/pattern-apply
    :consumes :dependency-status-fragment
    :produces :p1-block-record
    :completion-check
    [:blocks-on-p1-named
     "mission states it 'Blocks on P-1' (the canonical flexiarg parser)"]}
   ["cascade-construction/read-what-exists-first" 14]
   {:operation :locate-canonical-parser-path
    :capability :wm/pattern-apply
    :consumes :parser-path-fragment
    :produces :canonical-parser-path-record
    :completion-check
    [:projection-clj-path-named
     "mission names futon3a/src/futon/flexiarg/projection.clj as the P-1 parser that must ship"]}
   ["problems/llm-over-corpus-mining-lessons" 15]
   {:operation :record-advisory-first-mode
    :capability :wm/pattern-apply
    :consumes :landing-mode-fragment
    :produces :advisory-mode-record
    :completion-check
    [:advisory-mode-first
     "mission says the excursion lands in advisory mode first"]}
   ["pattern-mining/probe-the-claimed-property-not-the-acceptance-proxy" 16]
   {:operation :record-enforcement-precondition
    :capability :wm/pattern-apply
    :consumes :enforcement-condition-fragment
    :produces :violation-corpus-precondition
    :completion-check
    [:enforced-after-violation-corpus
     "mission defers enforced mode until a violation corpus exists"]}
   ["cascade-construction/read-what-exists-first" 17]
   {:operation :cite-trip-journal-btoa-complement
    :capability :wm/pattern-apply
    :consumes :complement-reference
    :produces :cited-btoa-complement
    :completion-check
    [:trip-journal-cited-as-complement
     "mission cites M-trip-journal.md as the B→A complement to this mission"]}
   ["problems/llm-over-corpus-mining-lessons" 18]
   {:operation :bind-ab-improvement-requirement
    :capability :wm/pattern-apply
    :consumes :learning-compatibility-fragment
    :produces :btoa-learnable-form-binding
    :completion-check
    [:improve-ab-in-btoa-learnable-form
     "mission states it should improve the A→B side in a form the B→A side can learn from"]}
   ["problems/llm-over-corpus-mining-lessons" 19]
   {:operation :record-learnability-criterion
    :capability :wm/pattern-apply
    :consumes :learnability-fragment
    :produces :learnability-criterion-record
    :completion-check
    [:btoa-can-actually-learn
     "mission's criterion is that the B→A side can 'actually learn from' the result"]}
   ["cascade-construction/read-what-exists-first" 20]
   {:operation :cite-candidate-queue-reweighting-excursion
    :capability :wm/pattern-apply
    :consumes :excursion-reference
    :produces :cited-phase1-weighting
    :completion-check
    [:reweighting-excursion-cited-by-path
     "mission cites E-btoa-candidate-queue-reweighting-v0.md as phase-1 B→A/SORRY weighting"]}
   ["cascade-construction/read-what-exists-first" 21]
   {:operation :cite-evidence-sample-mapping-excursion
    :capability :wm/pattern-apply
    :consumes :excursion-reference
    :produces :cited-first-hand-labeled-sample
    :completion-check
    [:mapping-excursion-cited-with-sample
     "mission cites E-btoa-evidence-sample-mapping-v0.md together with the first hand-labeled sample"]}
   ["pattern-mining/probe-the-claimed-property-not-the-acceptance-proxy" 22]
   {:operation :record-hand-labeled-seed-sample
    :capability :wm/pattern-apply
    :consumes :sample-fragment
    :produces :hand-labeled-seed-record
    :completion-check
    [:first-hand-labeled-sample-named
     "mission names the phase-1 weighting and the first hand-labeled sample as the seed evidence"]}})

(defn decisions
  "The hole resolutions for this requisition, bound to the cascade digest
  they repair.  Returns the decision map keyed by hole id when the context
  names this cascade, else a typed mismatch (never a silent wrong answer)."
  [{:keys [cascade-digest phase] :as _ctx}]
  (if (and (= cascade-digest
              "db0af237b84244d7003cfdaf3f61edb9e93403755c04edf7e7640f4283d807b4")
           (= :compile phase))
    clauses
    {:status :error
     :kind :patch-cascade-context-mismatch
     :cascade-digest cascade-digest
     :phase phase}))
