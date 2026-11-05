(ns futon2.aif.wm.authored.req-87836ca5
  "Authored operational clauses for held executable cascade
  8d1d2f84448aa6b21e4b53979ee2a281e25712e144c7d05cebdab85d11bceecf
  (phase :compile, requisition 87836ca588f71bd1480b1eabb0b91036f5a687f90f47d945bc46fe0ff6314715).

  Resolutions for the thirteen :operational-clause-owed holes of the
  M-patterns-done-right opening reading (analysis digest
  99a718a7718ddeee30d7c54264b80e19110cb4c6cdca98e2eee4b748c062ce59).
  Each decision is an operational clause attributed to the reading's
  occurrence squares: the operation names what the occurrence enacts in
  the mission text, completion-check names the observable condition in
  that text, and :consumes/:produces describe the transition.  Nothing
  here claims a square-derived completion and no application-order edge
  is authored."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["futon-theory/mission-lifecycle" 0]
   {:operation :record-mission-open-status
    :capability :wm/pattern-apply
    :consumes :mission-status-line
    :produces :open-status-record
    :completion-check
    [:status-line-reads-open
     "mission status line reads 'OPEN — OPEN' for M-patterns-done-right, drafted 2026-05-04, sibling to M-the-futon-stack"]}

   ["futon-theory/mission-scoping" 1]
   {:operation :bind-completion-to-level-evidence
    :capability :wm/pattern-apply
    :consumes :target-completion-line
    :produces :evidence-paced-completion-record
    :completion-check
    [:completion-paced-by-four-levels
     "mission's Target completion reads 'open-ended — paced by the four levels' evidence, not a calendar'"]}

   ["invariant-coherence/single-locus" 2]
   {:operation :record-single-coordination-home
    :capability :wm/pattern-apply
    :consumes :home-repo-annotation
    :produces :mission-home-locus-record
    :completion-check
    [:futon0-named-cross-repo-home
     "mission names futon0 as 'the cross-repo coordination home per single-locus/mission-home' on the Home-repo line"]}

   ["futon-theory/mission-dependency" 3]
   {:operation :index-cross-reference-edges
    :capability :wm/pattern-apply
    :consumes :cross-reference-list
    :produces :mission-dependency-edge-index
    :completion-check
    [:cross-reference-header-present
     "mission carries a '**Cross-references:**' header enumerating its missions, excursions, and files"]}

   ["futon-theory/single-source-of-truth" 5]
   {:operation :ground-mission-on-canonical-parser-invariant
    :capability :wm/pattern-apply
    :consumes :cct-1-thread-reference
    :produces :canonical-parser-grounding-record
    :completion-check
    [:cct-1-named-as-ground
     "mission cites CCT-1 'Canonical parser per data-object class' as 'the canonical-parser invariant that grounds the whole mission'"]}

   ["futon-theory/mission-dependency" 6]
   {:operation :record-inherited-calibration-artifacts
    :capability :wm/pattern-apply
    :consumes :retrieval-calibration-reference
    :produces :inherited-artifact-edge
    :completion-check
    [:p1-survey-rulebook-inherited
     "mission records that M-pattern-retrieval-calibration ships parser P-1, the clause-vocabulary survey, and the reshape rulebook 'that this mission inherits'"]}

   ["pattern-discipline/pattern-to-code-receipts" 7]
   {:operation :bind-level-4-to-demonstrable-value
    :capability :wm/pattern-apply
    :consumes :application-diagnostic-reference
    :produces :level-4-operational-binding
    :completion-check
    [:level-4-named-operational-form
     "mission states 'This mission's Level-4 (code receipts) is the operational form of that commitment' for the (T, ∇, Δ) demonstrable-value commitment"]}

   ["pattern-discipline/peripheral-as-sokoban" 8]
   {:operation :reframe-excursion-as-level-1-floor
    :capability :wm/pattern-apply
    :consumes :pattern-peripheral-excursion-reference
    :produces :level-1-sokoban-floor-record
    :completion-check
    [:sokoban-floor-named
     "mission reframes E-pattern-peripheral 'as the Level-1 Sokoban floor'"]}

   ["futon-theory/single-source-of-truth" 10]
   {:operation :record-reshape-rulebook-authority
    :capability :wm/pattern-apply
    :consumes :reshape-rulebook-reference
    :produces :verb-taxonomy-authority-record
    :completion-check
    [:closed-verb-taxonomy-named
     "mission names E-clause-vocabulary-reshape.sexp as 'the rulebook with verb taxonomy (replace / substructure / metadata / facet / reject) and the conclusion-aliases set'"]}

   ["peripherals/pattern-becomes-peripheral" 11]
   {:operation :grow-peripheral-from-single-bound-action
    :capability :wm/pattern-apply
    :consumes :pattern-author-mockup-reference
    :produces :level-1-peripheral-growth-spec
    :completion-check
    [:submit-draft-single-bound-action
     "mission records pattern_author.clj as the working Level-1 mockup where 'submit-draft! is the single bound action' and specifies what it should grow into"]}

   ["peripherals/constrained-execution-envelope" 12]
   {:operation :feed-level-2-from-author-prep-algorithm
    :capability :wm/pattern-apply
    :consumes :author-flexiarg-algorithm-reference
    :produces :level-2-algorithm-guidance-binding
    :completion-check
    [:agent-prep-algorithm-named
     "mission names author-flexiarg-core-and-wire-discoverability.md as 'the agent-prep algorithm for Level-2 (algorithm-guided authoring)'"]}

   ["pattern-discipline/patterns-as-categorical-objects" 13]
   {:operation :declare-level-3-categorical-substrate
    :capability :wm/pattern-apply
    :consumes :futon5-dsl-reference
    :produces :level-3-functorial-substrate-record
    :completion-check
    [:functorial-admission-named
     "mission names the futon5 ct/dsl.clj design-pattern-category DSL 'Substrate for Level-3 (functorial admission)'"]}

   ["pattern-discipline/patterns-as-categorical-objects" 14]
   {:operation :run-pattern-to-wiring-engine
    :capability :wm/pattern-apply
    :consumes :pattern-to-wiring-script-reference
    :produces :level-3-wiring-output-record
    :completion-check
    [:generator-named-engine
     "mission names futon5/scripts/pattern_to_wiring.clj, output in resources/xenotype-wirings/, 'Engine for Level-3.'"]}})

(defn decisions
  "The hole resolutions for this requisition, bound to the cascade digest
  they repair.  Returns the decision map keyed by hole id when the context
  names this cascade, else a typed mismatch (never a silent wrong answer)."
  [{:keys [cascade-digest phase] :as _ctx}]
  (if (and (= cascade-digest
              "8d1d2f84448aa6b21e4b53979ee2a281e25712e144c7d05cebdab85d11bceecf")
           (= :compile phase))
    clauses
    {:status :error
     :kind :patch-cascade-context-mismatch
     :cascade-digest cascade-digest
     :phase phase}))
