(ns futon2.aif.wm.authored.req-7720426a
  "Authored operational clauses for held executable cascade
  db0af237b84244d7003cfdaf3f61edb9e93403755c04edf7e7640f4283d807b4
  (phase :compile, requisition
  7720426a7979f6a429a730c028cb9cc0cfc6778c97d871f86fda3b992623f1f4).

  Resolutions for the twenty-one :operational-clause-owed holes owned by
  :wm/enactment-compiler in this requisition.  Each decision is an
  operational clause routed through the single routable capability
  :wm/pattern-apply; the operation names the pattern application that
  repairs the hole, completion-check names the observable condition, and
  :consumes/:produces describe the transition.  No claim is made about
  holes outside this requisition."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["problems/r12-two-layer-calibration" 0]
   {:operation :apply-two-layer-calibration-to-compiled-clause
    :capability :wm/pattern-apply
    :consumes :pattern-level-and-instance-level-evidence
    :produces :two-layer-calibrated-clause
    :completion-check
    [:clause-carries-both-layers
     "the compiled clause records both the pattern-level prior and the instance-level observation it was calibrated against, so neither layer can silently stand in for the other"]}
   ["problems/most-patterns-have-no-globally-recognised-rationale" 1]
   {:operation :apply-rationale-binding-to-compiled-clause
    :capability :wm/pattern-apply
    :consumes :pattern-with-unstated-rationale
    :produces :clause-with-stated-rationale
    :completion-check
    [:rationale-stated-not-assumed
     "the compiled clause states why the pattern is expected to help in this position, rather than citing the pattern's identity as its own rationale"]}
   ["problems/most-patterns-have-no-globally-recognised-rationale" 2]
   {:operation :apply-rationale-binding-to-compiled-clause
    :capability :wm/pattern-apply
    :consumes :pattern-with-unstated-rationale
    :produces :clause-with-stated-rationale
    :completion-check
    [:rationale-stated-not-assumed
     "the compiled clause states why the pattern is expected to help in this position, rather than citing the pattern's identity as its own rationale"]}
   ["cascade-construction/read-what-exists-first" 3]
   {:operation :apply-read-what-exists-first-to-hole-resolution
    :capability :wm/pattern-apply
    :consumes :existing-cascade-artifacts
    :produces :resolution-grounded-in-what-exists
    :completion-check
    [:existing-state-cited-before-new-state
     "the resolution cites the existing cascade artifact it read (by pin or path) before introducing any new state"]}
   ["problems/llm-over-corpus-mining-lessons" 4]
   {:operation :apply-corpus-grounding-to-compiled-clause
    :capability :wm/pattern-apply
    :consumes :model-generated-claim
    :produces :corpus-checked-clause
    :completion-check
    [:claim-traceable-to-corpus
     "every substantive claim in the compiled clause is traceable to a pinned corpus source rather than resting on generation alone"]}
   ["problems/r7-evidence-channel-precision" 5]
   {:operation :apply-evidence-channel-precision-to-compiled-clause
    :capability :wm/pattern-apply
    :consumes :evidence-carrying-clause
    :produces :channel-precise-evidence-clause
    :completion-check
    [:evidence-named-to-its-channel
     "the clause names which channel each piece of evidence arrived on, so a reviewer can check the channel rather than the summary"]}
   ["problems/r7-evidence-channel-precision" 6]
   {:operation :apply-evidence-channel-precision-to-compiled-clause
    :capability :wm/pattern-apply
    :consumes :evidence-carrying-clause
    :produces :channel-precise-evidence-clause
    :completion-check
    [:evidence-named-to-its-channel
     "the clause names which channel each piece of evidence arrived on, so a reviewer can check the channel rather than the summary"]}
   ["pattern-mining/probe-the-claimed-property-not-the-acceptance-proxy" 9]
   {:operation :apply-probe-the-claimed-property-to-completion-check
    :capability :wm/pattern-apply
    :consumes :claimed-pattern-property
    :produces :property-probing-check
    :completion-check
    [:check-probes-the-claimed-property
     "the completion check probes the property actually claimed, not a proxy that could pass while the property fails"]}
   ["cascade-construction/read-what-exists-first" 10]
   {:operation :apply-read-what-exists-first-to-hole-resolution
    :capability :wm/pattern-apply
    :consumes :existing-cascade-artifacts
    :produces :resolution-grounded-in-what-exists
    :completion-check
    [:existing-state-cited-before-new-state
     "the resolution cites the existing cascade artifact it read (by pin or path) before introducing any new state"]}
   ["problems/most-patterns-have-no-globally-recognised-rationale" 11]
   {:operation :apply-rationale-binding-to-compiled-clause
    :capability :wm/pattern-apply
    :consumes :pattern-with-unstated-rationale
    :produces :clause-with-stated-rationale
    :completion-check
    [:rationale-stated-not-assumed
     "the compiled clause states why the pattern is expected to help in this position, rather than citing the pattern's identity as its own rationale"]}
   ["problems/r12-two-layer-calibration" 12]
   {:operation :apply-two-layer-calibration-to-compiled-clause
    :capability :wm/pattern-apply
    :consumes :pattern-level-and-instance-level-evidence
    :produces :two-layer-calibrated-clause
    :completion-check
    [:clause-carries-both-layers
     "the compiled clause records both the pattern-level prior and the instance-level observation it was calibrated against, so neither layer can silently stand in for the other"]}
   ["cascade-construction/read-what-exists-first" 13]
   {:operation :apply-read-what-exists-first-to-hole-resolution
    :capability :wm/pattern-apply
    :consumes :existing-cascade-artifacts
    :produces :resolution-grounded-in-what-exists
    :completion-check
    [:existing-state-cited-before-new-state
     "the resolution cites the existing cascade artifact it read (by pin or path) before introducing any new state"]}
   ["cascade-construction/read-what-exists-first" 14]
   {:operation :apply-read-what-exists-first-to-hole-resolution
    :capability :wm/pattern-apply
    :consumes :existing-cascade-artifacts
    :produces :resolution-grounded-in-what-exists
    :completion-check
    [:existing-state-cited-before-new-state
     "the resolution cites the existing cascade artifact it read (by pin or path) before introducing any new state"]}
   ["problems/llm-over-corpus-mining-lessons" 15]
   {:operation :apply-corpus-grounding-to-compiled-clause
    :capability :wm/pattern-apply
    :consumes :model-generated-claim
    :produces :corpus-checked-clause
    :completion-check
    [:claim-traceable-to-corpus
     "every substantive claim in the compiled clause is traceable to a pinned corpus source rather than resting on generation alone"]}
   ["pattern-mining/probe-the-claimed-property-not-the-acceptance-proxy" 16]
   {:operation :apply-probe-the-claimed-property-to-completion-check
    :capability :wm/pattern-apply
    :consumes :claimed-pattern-property
    :produces :property-probing-check
    :completion-check
    [:check-probes-the-claimed-property
     "the completion check probes the property actually claimed, not a proxy that could pass while the property fails"]}
   ["cascade-construction/read-what-exists-first" 17]
   {:operation :apply-read-what-exists-first-to-hole-resolution
    :capability :wm/pattern-apply
    :consumes :existing-cascade-artifacts
    :produces :resolution-grounded-in-what-exists
    :completion-check
    [:existing-state-cited-before-new-state
     "the resolution cites the existing cascade artifact it read (by pin or path) before introducing any new state"]}
   ["problems/llm-over-corpus-mining-lessons" 18]
   {:operation :apply-corpus-grounding-to-compiled-clause
    :capability :wm/pattern-apply
    :consumes :model-generated-claim
    :produces :corpus-checked-clause
    :completion-check
    [:claim-traceable-to-corpus
     "every substantive claim in the compiled clause is traceable to a pinned corpus source rather than resting on generation alone"]}
   ["problems/llm-over-corpus-mining-lessons" 19]
   {:operation :apply-corpus-grounding-to-compiled-clause
    :capability :wm/pattern-apply
    :consumes :model-generated-claim
    :produces :corpus-checked-clause
    :completion-check
    [:claim-traceable-to-corpus
     "every substantive claim in the compiled clause is traceable to a pinned corpus source rather than resting on generation alone"]}
   ["cascade-construction/read-what-exists-first" 20]
   {:operation :apply-read-what-exists-first-to-hole-resolution
    :capability :wm/pattern-apply
    :consumes :existing-cascade-artifacts
    :produces :resolution-grounded-in-what-exists
    :completion-check
    [:existing-state-cited-before-new-state
     "the resolution cites the existing cascade artifact it read (by pin or path) before introducing any new state"]}
   ["cascade-construction/read-what-exists-first" 21]
   {:operation :apply-read-what-exists-first-to-hole-resolution
    :capability :wm/pattern-apply
    :consumes :existing-cascade-artifacts
    :produces :resolution-grounded-in-what-exists
    :completion-check
    [:existing-state-cited-before-new-state
     "the resolution cites the existing cascade artifact it read (by pin or path) before introducing any new state"]}
   ["pattern-mining/probe-the-claimed-property-not-the-acceptance-proxy" 22]
   {:operation :apply-probe-the-claimed-property-to-completion-check
    :capability :wm/pattern-apply
    :consumes :claimed-pattern-property
    :produces :property-probing-check
    :completion-check
    [:check-probes-the-claimed-property
     "the completion check probes the property actually claimed, not a proxy that could pass while the property fails"]}})

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
