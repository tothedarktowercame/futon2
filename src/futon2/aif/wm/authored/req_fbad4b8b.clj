(ns futon2.aif.wm.authored.req-fbad4b8b
  "Authored operational clauses for held executable cascade
  27ee1f800ba25111680c7c7c93355afc069f89d3a666c8a238a7574477e0b937
  (phase :compile, requisition fbad4b8be6eb583b0ad5be984aec251139b288492d200c6d7a15c9618dd11f3f).

  Resolutions for the nineteen :operational-clause-owed holes of the
  M-stack-geometry reading (policy M-stack-geometry/policy-c7d7518e1e185c2e2318,
  analysis digest e183844e1fd2569431413d615fb7b997a8182cdd0b86f14175ed3b83e21e5e4d).
  Each decision is an operational clause attributed to the reading's
  occurrence squares: the operation names what the occurrence enacts in the
  mission text, completion-check names the observable condition in that
  text, and :consumes/:produces describe the transition.  Nothing here
  claims a square-derived completion and no application-order edge is
  authored."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["futon-theory/mission-lifecycle" 1]
   {:operation :record-open-identify-status
    :capability :wm/pattern-apply
    :consumes :mission-status-line
    :produces :open-identify-status-record
    :completion-check
    [:status-open-identify
     "mission status line reads 'OPEN — IDENTIFY'"]}
   ["agent/evidence-over-assertion" 2]
   {:operation :cite-formal-predecessor-artifacts
    :capability :wm/pattern-apply
    :consumes :predecessor-artifact-reference
    :produces :cited-formal-predecessors
    :completion-check
    [:predecessors-cited-by-name
     "mission names `holistic-argument.sexp` and `holistic-argument-sketch.md` as the artifacts that preceded the living synthesis"]}
   ["futon-theory/mission-lifecycle" 3]
   {:operation :record-predecessor-disposition
    :capability :wm/pattern-apply
    :consumes :disposition-fragment
    :produces :superseded-disposition-record
    :completion-check
    [:argument-marked-superseded
     "mission marks the formal argument 'formal argument, superseded'"]}
   ["futon-theory/mission-lifecycle" 4]
   {:operation :adopt-living-synthesis-document
    :capability :wm/pattern-apply
    :consumes :synthesis-reference
    :produces :current-synthesis-record
    :completion-check
    [:three-pillars-named-living
     "mission names `three-pillars.md` as the 'conceptual reframe, living synthesis'"]}
   ["war-machine/state-capture" 5]
   {:operation :capture-mission-control-dependency
    :capability :wm/pattern-apply
    :consumes :mission-control-reference
    :produces :mission-control-dependency-record
    :completion-check
    [:mission-control-cited-by-path
     "mission cites M-mission-control.md as the observational dependency beneath the geometry"]}
   ["war-machine/state-capture" 6]
   {:operation :record-mission-control-observation-scope
    :capability :wm/pattern-apply
    :consumes :observation-scope-fragment
    :produces :observation-scope-record
    :completion-check
    [:scope-approximately-25-missions
     "mission describes Mission Control as the 'observational layer over ~25 active missions'"]}
   ["agent/evidence-over-assertion" 7]
   {:operation :cite-portfolio-inference-dependency
    :capability :wm/pattern-apply
    :consumes :portfolio-inference-reference
    :produces :cited-portfolio-inference
    :completion-check
    [:portfolio-cited-by-path
     "mission cites M-portfolio-inference.md as the inference and policy mission above Mission Control"]}
   ["agent/evidence-over-assertion" 8]
   {:operation :name-portfolio-policy-layer
    :capability :wm/pattern-apply
    :consumes :policy-layer-fragment
    :produces :policy-layer-record
    :completion-check
    [:efe-layer-named
     "mission names the 'generative-model + EFE policy layer' positioned above Mission Control"]}
   ["features/operator-turns-enter-the-observation-vector" 9]
   {:operation :cite-trip-journal-hinge-log-apparatus
    :capability :wm/pattern-apply
    :consumes :hinge-log-reference
    :produces :cited-hinge-log-apparatus
    :completion-check
    [:trip-journal-cited-with-hinge-log
     "mission cites M-trip-journal.md as the hinge-log apparatus the geometry relies on"]}
   ["hdm/deep-storage-to-active-graph" 10]
   {:operation :bind-shared-futon1a-ingestion-substrate
    :capability :wm/pattern-apply
    :consumes :substrate-fragment
    :produces :shared-ingestion-binding
    :completion-check
    [:same-futon1a-substrate-named
     "mission states Stack Geometry and the trip journal use the 'same futon1a ingestion substrate'"]}
   ["problems/operator-turns-become-inference-observations" 12]
   {:operation :connect-sorry-proximity-to-ba-turns
    :capability :wm/pattern-apply
    :consumes :sorry-proximity-fragment
    :produces :ba-turn-evidence-binding
    :completion-check
    [:proximity-paired-with-ba-turns
     "mission pairs the sorry-proximity model with M-trip-journal B→A turns"]}
   ["problems/operator-turns-become-inference-observations" 13]
   {:operation :assign-sorry-discovery-observation-stream
    :capability :wm/pattern-apply
    :consumes :stream-role-fragment
    :produces :sorry-discovery-stream-record
    :completion-check
    [:ba-turns-candidate-stream
     "mission assigns the B→A turns the role of 'candidate observation stream for sorry discovery'"]}
   ["problems/llm-over-corpus-mining-lessons" 14]
   {:operation :cite-learning-loop-sibling-mission
    :capability :wm/pattern-apply
    :consumes :sibling-reference
    :produces :cited-learning-loop-sibling
    :completion-check
    [:learning-loop-cited-by-path
     "mission cites M-learning-loop.md as the pattern-learning sibling mission"]}
   ["problems/llm-over-corpus-mining-lessons" 15]
   {:operation :bind-shared-learning-substrate
    :capability :wm/pattern-apply
    :consumes :substrate-sharing-fragment
    :produces :shared-learning-substrate-binding
    :completion-check
    [:mining-uses-shared-substrate
     "mission states the sibling shares the embedding space and futon1a rendering and that 'pattern-mining uses this'"]}
   ["gauntlet/world-is-hypergraph" 17]
   {:operation :cite-future-hypergraph-operator
    :capability :wm/pattern-apply
    :consumes :operator-reference
    :produces :cited-operator-layer
    :completion-check
    [:hypergraph-operator-cited-eventual
     "mission cites M-hypergraph-operator.md as the eventual Click/Tick consumer of the geometry"]}
   ["gauntlet/world-is-hypergraph" 18]
   {:operation :position-operator-analytic-layer
    :capability :wm/pattern-apply
    :consumes :position-fragment
    :produces :operator-position-record
    :completion-check
    [:analytic-layer-above-geometry
     "mission positions the operator as the 'analytic layer above the geometry'"]}
   ["hdm/deep-storage-to-active-graph" 19]
   {:operation :cite-webarxana-bridge-precedent
    :capability :wm/pattern-apply
    :consumes :excursion-reference
    :produces :cited-bridge-precedent
    :completion-check
    [:excursion-cited-by-path
     "mission cites E-webarxana-as-hinge-log-backend.md as the hinge-log bridge precedent"]}
   ["hdm/deep-storage-to-active-graph" 20]
   {:operation :record-shape-a-entity-landing-precedent
    :capability :wm/pattern-apply
    :consumes :precedent-fragment
    :produces :entity-landing-precedent
    :completion-check
    [:shape-a-named-precedent
     "mission names the Shape A bridge as the 'precedent for how mission entities land'"]}
   ["hdm/deep-storage-to-active-graph" 21]
   {:operation :bind-landing-and-rendering-path
    :capability :wm/pattern-apply
    :consumes :rendering-fragment
    :produces :entity-landing-rendering-binding
    :completion-check
    [:futon1a-storage-webarxana-rendering
     "mission completes the precedent naming futon1a as where mission entities land and WebArxana as how they are rendered"]}})

(defn decisions
  "The hole resolutions for this requisition, bound to the cascade digest
  they repair.  Returns the decision map keyed by hole id when the context
  names this cascade, else a typed mismatch (never a silent wrong answer)."
  [{:keys [cascade-digest phase] :as _ctx}]
  (if (and (= cascade-digest
              "27ee1f800ba25111680c7c7c93355afc069f89d3a666c8a238a7574477e0b937")
           (= :compile phase))
    clauses
    {:status :error
     :kind :patch-cascade-context-mismatch
     :cascade-digest cascade-digest
     :phase phase}))
