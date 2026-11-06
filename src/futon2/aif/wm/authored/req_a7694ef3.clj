(ns futon2.aif.wm.authored.req-a7694ef3
  "Authored operational clauses for War Machine cascade a4b0d419
  (compile phase, requisition
  a7694ef388f487d469debb10f7a19b40491388bb4d5cdb63bc8d0f0ae6a4d779).

  Resolutions for the twenty-four :operational-clause-owed holes of the
  M-superpod-mark3 reading.  Each decision is an operational clause
  attributed to the occurrence squares authored over
  futon6/holes/missions/M-superpod-mark3.md: the operation names what the
  occurrence enacts in the mission text, completion-check names the
  observable condition in that text, and :consumes/:produces describe the
  transition.  No application-order edge is authored."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["pattern-discipline/patterns-as-categorical-objects" 0]
   {:operation :name-the-mission-artifact-as-a-pattern-tagged-geometrically-annotated-corpus
    :capability :wm/pattern-apply
    :consumes :mission-heading
    :produces :named-mark3-corpus-deliverable
    :completion-check
    [:heading-names-pattern-tags-and-geometric-annotation
     "the mission heading names what is built: a corpus carrying pattern tags and geometric annotation (M-superpod-mark3.md heading)"]}
   ["mission-coherence/logic-model-before-code" 1]
   {:operation :open-the-mission-at-the-identify-stage
    :capability :wm/pattern-apply
    :consumes :status-banner
    :produces :identify-stage-mission-record
    :completion-check
    [:status-banner-reads-open-identify
     "the status banner declares the mission OPEN at the IDENTIFY stage before any harness runs (M-superpod-mark3.md Status line)"]}
   ["storage/deterministic-ingest-pipeline" 2]
   {:operation :split-ownership-between-superpod-runs-and-pipeline-code
    :capability :wm/pattern-apply
    :consumes :owner-line
    :produces :assigned-pipeline-ownership
    :completion-check
    [:owner-line-names-rob-for-runs-and-joe-for-code-and-evaluation
     "ownership is split between who runs superpod (Rob) and who writes and evaluates pipeline code (Joe) (M-superpod-mark3.md Owner line)"]}
   ["pattern-discipline/patterns-as-categorical-objects" 3]
   {:operation :name-futon3-as-the-pattern-application-diagnostic-stake
    :capability :wm/pattern-apply
    :consumes :repos-list
    :produces :futon3-role-tied-to-parent-theory-mission
    :completion-check
    [:repos-list-grounds-futon3-in-the-parent-theory-mission
     "futon3's stake in the repos list is the pattern application diagnostic, tied to the parent theory mission (M-superpod-mark3.md Repos lines)"]}
   ["cascades/heal-by-repair-cascades" 4]
   {:operation :extend-rather-than-replace-the-mark2-substrate
    :capability :wm/pattern-apply
    :consumes :mark2-substrate-description
    :produces :inherited-10-stage-pipeline-and-eprint-plumbing
    :completion-check
    [:predecessor-paragraph-states-inherits-and-extends
     "the predecessor paragraph pins the inherited substrate as of commit 74cc161 and states mark3 extends rather than replaces it (M-superpod-mark3.md Predecessor lines)"]}
   ["problems/pattern-coding-and-novel-force-mining" 5]
   {:operation :place-mark3-as-substrate-side-delivery-of-prototype-2
    :capability :wm/pattern-apply
    :consumes :parent-theory-link
    :produces :positioned-substrate-delivery
    :completion-check
    [:parent-theory-paragraph-positions-mark3-as-prototype-2-substrate
     "the parent-theory paragraph names mark3 the substrate-side delivery for prototype 2 of the parent mission, with witnesses (M-superpod-mark3.md Parent theory lines)"]}
   ["mission-coherence/logic-model-before-code" 6]
   {:operation :reject-the-narrow-runner-cleanup-reading
    :capability :wm/pattern-apply
    :consumes :why-section-opening
    :produces :scoped-mission-beyond-runner-cleanup
    :completion-check
    [:why-section-opens-with-more-than-a-runner-cleanup
     "the motivation section explicitly refuses reading mark3 as a runner cleanup (M-superpod-mark3.md 'mark3 is more than a runner cleanup')"]}
   ["pattern-discipline/patterns-as-categorical-objects" 7]
   {:operation :require-each-turn-or-paper-to-be-a-typed-well-specified-repeatable-pattern-application
    :capability :wm/pattern-apply
    :consumes :pattern-work-context-paragraph
    :produces :coding-requirement-for-agent-turns-and-papers
    :completion-check
    [:paragraph-states-the-typed-well-specified-repeatable-requirement
     "the text requires each agent turn (or paper) be coded as an application of a typed, well-specified, repeatable pattern of demonstrable value (M-superpod-mark3.md Why section)"]}
   ["pattern-discipline/patterns-as-categorical-objects" 8]
   {:operation :demand-demonstrable-value-for-pattern-applications
    :capability :wm/pattern-apply
    :consumes :parent-theory-mission-frame
    :produces :value-demand-on-corpus-coding
    :completion-check
    [:demonstrable-value-named-in-the-coding-sentence
     "the coding sentence carries demonstrable value as a demanded property of applied patterns, per Joe's parent theory mission (M-superpod-mark3.md Why section)"]}
   ["problems/pattern-coding-and-novel-force-mining" 9]
   {:operation :give-the-context-tension-move-pattern-frame
    :capability :wm/pattern-apply
    :consumes :parent-theory-mission-frame
    :produces :bhk-arrow-pattern-frame-on-record
    :completion-check
    [:frame-stated-as-context-tension-move-bhk-arrow
     "patterns are framed as context to tension to move, a BHK-arrow shape whose witness makes the application checkable (M-superpod-mark3.md Why section)"]}
   ["problems/pattern-coding-and-novel-force-mining" 10]
   {:operation :place-the-math-corpus-as-prototype-2
    :capability :wm/pattern-apply
    :consumes :pattern-frame-statement
    :produces :math-corpus-named-as-prototype-2
    :completion-check
    [:sentence-names-the-math-corpus-prototype-2-of-the-frame
     "the math corpus is explicitly named prototype 2 of the pattern frame (M-superpod-mark3.md 'The math corpus is *prototype 2* of that frame')"]}
   ["storage/deterministic-ingest-pipeline" 11]
   {:operation :report-that-the-substrate-already-produces-the-slot-triple
    :capability :wm/pattern-apply
    :consumes :pilot-excursion-reference
    :produces :existing-capability-finding
    :completion-check
    [:pilot-finding-cited-for-the-slot-triple
     "the hand-tagged 25-paper pilot is cited as showing the substrate already produces the slot triple situation_S, xiang_salience, arrow_constraint (M-superpod-mark3.md Why section)"]}
   ["pattern-discipline/patterns-as-categorical-objects" 12]
   {:operation :name-the-missing-named-pattern-overlay-gap
    :capability :wm/pattern-apply
    :consumes :pilot-finding
    :produces :gap-statement-for-the-named-pattern-overlay
    :completion-check
    [:gap-sentence-names-the-overlay-and-its-deliverers
     "the text states what is missing is the named-pattern overlay that R-1 + R-2 add (M-superpod-mark3.md Why section)"]}
   ["mission-coherence/logic-model-before-code" 13]
   {:operation :tie-each-stack-piece-to-the-parent-ifr-properties
    :capability :wm/pattern-apply
    :consumes :r1-r2-r3-geometry-stack-introduction
    :produces :piecewise-correspondence-claim
    :completion-check
    [:stack-paragraph-refuses-internal-cleanup-reading-and-promises-correspondence
     "the stack paragraph states the pieces are not mark3-internal cleanup and each piece corresponds to one of the parent IFR's five properties (M-superpod-mark3.md Why section)"]}
   ["mission-coherence/logic-model-before-code" 14]
   {:operation :enumerate-the-parent-ifr-five-properties-as-criteria
    :capability :wm/pattern-apply
    :consumes :piecewise-correspondence-claim
    :produces :five-named-criteria
    :completion-check
    [:bulleted-list-names-all-five-properties
     "the bulleted list maps each deliverable to well-specified, coverage, repeatable, demonstrable value, and replicable under automation (M-superpod-mark3.md Why section bullets)"]}
   ["coordination/task-shape-validation" 15]
   {:operation :deliver-well-specifiedness-via-r1-prompt-and-r2-hierarchical-pattern-set
    :capability :wm/pattern-apply
    :consumes :five-property-criteria-list
    :produces :r1-plus-r2-mapped-to-well-specified
    :completion-check
    [:first-bullet-maps-r1-and-r2-to-well-specified
     "the R-1 (arxiv prompt) + R-2 (hierarchical pattern set) bullet carries the well-specified property (M-superpod-mark3.md bullet 1)"]}
   ["storage/deterministic-ingest-pipeline" 16]
   {:operation :enforce-coverage-discipline-with-no-nulls
    :capability :wm/pattern-apply
    :consumes :five-property-criteria-list
    :produces :no-null-coverage-rule-on-record
    :completion-check
    [:second-bullet-maps-coverage-discipline-no-nulls-to-coverage
     "the Coverage discipline (no nulls) bullet carries the coverage property (M-superpod-mark3.md bullet 2)"]}
   ["pattern-discipline/patterns-as-categorical-objects" 17]
   {:operation :enforce-slot-distinctness-for-repeatable-application
    :capability :wm/pattern-apply
    :consumes :five-property-criteria-list
    :produces :slot-distinctness-rule-on-record
    :completion-check
    [:third-bullet-maps-slot-distinctness-to-repeatable
     "the Slot-distinctness bullet carries the repeatable property over the slot triple (M-superpod-mark3.md bullet 3)"]}
   ["storage/deterministic-ingest-pipeline" 18]
   {:operation :measure-value-with-the-geometry-artifact-t-total-f2
    :capability :wm/pattern-apply
    :consumes :five-property-criteria-list
    :produces :geometry-artifact-metric-deliverable
    :completion-check
    [:fourth-bullet-maps-the-geometry-artifact-to-demonstrable-value
     "the Geometry artifact (T_total, F₂) bullet carries the demonstrable value property (M-superpod-mark3.md bullet 4)"]}
   ["coordination/task-shape-validation" 19]
   {:operation :enable-replication-with-hierarchical-taxonomy-and-family-level-fallback
    :capability :wm/pattern-apply
    :consumes :five-property-criteria-list
    :produces :family-level-fallback-scheme-on-record
    :completion-check
    [:fifth-bullet-maps-taxonomy-plus-fallback-to-replicable-under-automation
     "the Hierarchical taxonomy + family-level fallback bullet carries the replicable under automation property (M-superpod-mark3.md bullet 5)"]}
   ["ukrns/reader-as-participant" 20]
   {:operation :decouple-the-deliverables-from-the-parent-mission-framing
    :capability :wm/pattern-apply
    :consumes :closing-paragraph-address-to-rob
    :produces :standalone-deliverables-with-optional-framing
    :completion-check
    [:closing-paragraph-tells-rob-the-deliverables-stand-alone
     "the closing paragraph states Rob does not need to engage the parent mission: the deliverables stand alone while the framing explains why (M-superpod-mark3.md closing paragraph)"]}
   ["mission-coherence/logic-model-before-code" 21]
   {:operation :justify-the-substrate-direction-against-alternative-refactors
    :capability :wm/pattern-apply
    :consumes :closing-paragraph-framing
    :produces :direction-rationale-on-record
    :completion-check
    [:closing-paragraph-names-why-this-direction-not-alternatives
     "the framing explains why the substrate edges in this particular direction rather than toward alternative refactors (M-superpod-mark3.md closing paragraph)"]}
   ["storage/deterministic-ingest-pipeline" 22]
   {:operation :cite-the-empirical-seed-and-metrics-predecessors
    :capability :wm/pattern-apply
    :consumes :direction-rationale
    :produces :cross-reference-list-to-pilot-and-metrics-excursions
    :completion-check
    [:cross-references-name-both-excursions
     "the cross-references cite E-math-prototype-pilot.md (empirical seed) and E-substrate-metrics.md (M-superpod-mark3.md closing lines)"]}
   ["storage/deterministic-ingest-pipeline" 23]
   {:operation :name-f2-and-successor-metrics-as-the-measurement-line
    :capability :wm/pattern-apply
    :consumes :metrics-excursion-reference
    :produces :metric-provenance-statement
    :completion-check
    [:final-line-grounds-metrics-in-the-excursion
     "the metrics excursion is cited as the source of the F₂ score and successor metrics (M-superpod-mark3.md final line)"]}})

(defn decisions
  "The hole resolutions for this requisition, bound to the cascade digest
  they repair.  Returns the decision map keyed by hole id when the context
  names this cascade, else a typed mismatch (never a silent wrong answer)."
  [{:keys [cascade-digest phase] :as _ctx}]
  (if (and (= cascade-digest
              "a4b0d4194be08d977ebc7f22f65781313b4cbcaa555b1c498962fcab4d5095b9")
           (= :compile phase))
    clauses
    {:status :error
     :kind :patch-cascade-context-mismatch
     :cascade-digest cascade-digest
     :phase phase}))
