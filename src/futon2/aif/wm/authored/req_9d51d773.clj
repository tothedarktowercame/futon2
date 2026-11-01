(ns futon2.aif.wm.authored.req-9d51d773
  "Authored operational clauses for held executable cascade
  2410969ef313c6fa239121e7fbfc061a49c902da9119a879faee0d14d1c36e2d
  (phase :compile, requisition
  9d51d77310169338a3da87df39f7815c91cad47e056f9793b10253fbfa843dbf).

  Resolutions for the twenty-three :operational-clause-owed holes of this
  requisition.  Each decision is an operational clause grounded in the
  named library pattern's own committed content: the operation names what
  applying that pattern enacts, completion-check names the observable
  condition in that pattern's body, and :consumes/:produces describe the
  transition.  Nothing here claims an implementation completion and no
  application-order edge is authored."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["problems/r12-two-layer-calibration" 0]
   {:operation :separate-internal-consistency-from-witnessed-value
    :capability :wm/pattern-apply
    :consumes :control-stage-r12-roster-entry
    :produces :two-layer-evidence-separation
    :completion-check
    [:assurance-band-placement-cited
     "the two-layer calibration separates internal consistency from externally witnessed value evidence, as R12's BELIEVE assurance-band roster entry states"]}
   ["problems/most-patterns-have-no-globally-recognised-rationale" 0]
   {:operation :surface-rationale-unreachability
    :capability :wm/pattern-apply
    :consumes :library-traversal-probe-result
    :produces :rationale-reachability-finding
    :completion-check
    [:unreachable-patterns-named
     "the pattern is shown unreachable via @how or @why traversal, so it carries no globally recognised rationale"]}
   ["problems/most-patterns-have-no-globally-recognised-rationale" 1]
   {:operation :bind-problem-node-for-rationale-edges
    :capability :wm/pattern-apply
    :consumes :library-loop-mint-record
    :produces :why-edge-landing-node
    :completion-check
    [:problem-node-serves-why-edges
     "the pattern's rationale annotation gains a @why edge landing on this problem node, per the node's minted purpose"]}
   ["problems/most-patterns-have-no-globally-recognised-rationale" 2]
   {:operation :gate-graph-certificates-on-rationale-reachability
    :capability :wm/pattern-apply
    :consumes :graph-certificate-candidate
    :produces :certificate-participation-check
    :completion-check
    [:unreachable-patterns-excluded-from-certificates
     "patterns without globally recognised rationale do not participate in graph-level certificates"]}
   ["cascade-construction/read-what-exists-first" 3]
   {:operation :read-the-requirement-source
    :capability :wm/pattern-apply
    :consumes :problem-or-requirement-artifact
    :produces :requirement-observation
    :completion-check
    [:requirement-source-read
     "the requirement source has been read as an observation before any pattern is proposed"]}
   ["problems/llm-over-corpus-mining-lessons" 4]
   {:operation :carry-distilled-mining-lessons
    :capability :wm/pattern-apply
    :consumes :data-mining-section-documentation
    :produces :distilled-lesson-record
    :completion-check
    [:lesson-distilled-from-section-doc
     "the lesson carried forward is one the data-mining section's own committed documentation names"]}
   ["problems/r7-evidence-channel-precision" 5]
   {:operation :vary-trust-by-evidence-channel
    :capability :wm/pattern-apply
    :consumes :evidence-channel-observation
    :produces :channel-specific-trust-weighting
    :completion-check
    [:trust-varies-without-discarding-noisy-evidence
     "trust varies by evidence channel without discarding noisy evidence, as R7's roster entry states"]}
   ["pattern-mining/probe-the-claimed-property-not-the-acceptance-proxy" 5]
   {:operation :probe-the-transitive-trust-closure
    :capability :wm/pattern-apply
    :consumes :acceptance-check-definition
    :produces :claimed-property-probe
    :completion-check
    [:probe-targets-claimed-property
     "the probe exercises the claimed property itself, not the acceptance proxy that stands in for it"]}
   ["problems/r7-evidence-channel-precision" 6]
   {:operation :record-channel-precision-placement
    :capability :wm/pattern-apply
    :consumes :control-stage-roster-entry
    :produces :believe-band-placement-record
    :completion-check
    [:roster-entry-cited
     "evidence-channel precision is placed in BELIEVE by the control-stages roster (control-stages.edn:20)"]}
   ["pattern-mining/probe-the-claimed-property-not-the-acceptance-proxy" 9]
   {:operation :detect-vacuous-acceptance
    :capability :wm/pattern-apply
    :consumes :acceptance-run-output
    :produces :vacuity-detection-result
    :completion-check
    [:vacuity-caught-against-real-dependency
     "an acceptance check that passes while the claimed property fails is caught against the real dependency, not a stub"]}
   ["cascade-construction/read-what-exists-first" 10]
   {:operation :read-the-implementation-before-proposing
    :capability :wm/pattern-apply
    :consumes :implementation-source
    :produces :implementation-observation
    :completion-check
    [:implementation-read-as-observation
     "the implementation source has been read and its behaviour recorded as an observation before pattern selection"]}
   ["problems/most-patterns-have-no-globally-recognised-rationale" 11]
   {:operation :run-library-rationale-census
    :capability :wm/pattern-apply
    :consumes :library-pattern-census-input
    :produces :rationale-coverage-census
    :completion-check
    [:census-counts-rationale-reachability
     "the census records which patterns are reachable via @how or @why traversal, per the library-loop lane"]}
   ["problems/r12-two-layer-calibration" 12]
   {:operation :cite-r12-residual-problems-dossier
    :capability :wm/pattern-apply
    :consumes :problem-dossier-fragment
    :produces :r12-scope-record
    :completion-check
    [:dossier-residual-list-named-as-requirement
     "what solving R12 requires is taken from the dossier's residual-problems list, as the node's @how states"]}
   ["cascade-construction/read-what-exists-first" 13]
   {:operation :read-the-live-state
    :capability :wm/pattern-apply
    :consumes :live-system-state-probe
    :produces :live-state-observation
    :completion-check
    [:live-state-read
     "the live state (what the running system actually holds) has been read as an observation before proposing"]}
   ["cascade-construction/read-what-exists-first" 14]
   {:operation :surface-requirement-implementation-disagreements
    :capability :wm/pattern-apply
    :consumes :requirement-and-implementation-observations
    :produces :disagreement-list
    :completion-check
    [:disagreements-listed-before-judgement
     "disagreements between requirement, implementation and live state are listed explicitly before deciding which matter"]}
   ["problems/llm-over-corpus-mining-lessons" 15]
   {:operation :apply-meme-mining-lesson
    :capability :wm/pattern-apply
    :consumes :futon6-mining-derived-lesson
    :produces :applied-lesson-check
    :completion-check
    [:lesson-traced-to-futon6-mining
     "the applied lesson traces to the futon6 meme-mining lessons the section documentation distils"]}
   ["pattern-mining/probe-the-claimed-property-not-the-acceptance-proxy" 16]
   {:operation :audit-dependency-closure-of-trust
    :capability :wm/pattern-apply
    :consumes :trust-or-axiom-closure-claim
    :produces :closure-audit-result
    :completion-check
    [:transitive-closure-probed
     "the transitive axiom/trust closure behind the acceptance check is probed directly, not assumed via the proxy"]}
   ["cascade-construction/read-what-exists-first" 17]
   {:operation :locate-prior-work-before-construction
    :capability :wm/pattern-apply
    :consumes :search-journal-git-locator
    :produces :prior-work-record
    :completion-check
    [:prior-work-located-deterministically
     "prior work is located by the deterministic means (search, journal, git) before any LLM judgement is spent"]}
   ["problems/llm-over-corpus-mining-lessons" 18]
   {:operation :recognise-section-grain-problem-statement
    :capability :wm/pattern-apply
    :consumes :data-mining-section-readme
    :produces :section-grain-problem-record
    :completion-check
    [:problem-stated-in-sections-own-doc
     "the problem is recognised as stated by the section's own committed documentation, not invented here"]}
   ["problems/llm-over-corpus-mining-lessons" 19]
   {:operation :ground-worklist-membership
    :capability :wm/pattern-apply
    :consumes :served-refused-worklist-entry
    :produces :grounded-worklist-membership
    :completion-check
    [:membership-traced-to-readme-opening
     "the worklist membership is grounded in the data-mining README's opening paragraph, per library-loop row L18 v3"]}
   ["cascade-construction/read-what-exists-first" 20]
   {:operation :check-all-four-sources
    :capability :wm/pattern-apply
    :consumes :requirement-implementation-live-state-journal-reads
    :produces :four-source-observation-set
    :completion-check
    [:four-sources-each-read
     "each of the four sources (requirement, implementation, live state, journal) has been read — the pattern's own done condition"]}
   ["cascade-construction/read-what-exists-first" 21]
   {:operation :record-observation-per-source-read
    :capability :wm/pattern-apply
    :consumes :source-read-result
    :produces :check-result-observation
    :completion-check
    [:each-read-enters-as-observation
     "each source read is a check whose result enters as an observation — state information, per the pattern's epistemic-value line"]}
   ["pattern-mining/probe-the-claimed-property-not-the-acceptance-proxy" 22]
   {:operation :report-provenance-inflation-honestly
    :capability :wm/pattern-apply
    :consumes :mining-run-provenance-record
    :produces :provenance-caveat-record
    :completion-check
    [:non-blind-run-counts-flagged
     "counts from non-blind slice runs are flagged as inflated and not what the claim rests on, per the pattern's provenance line"]}})

(defn decisions
  "The hole resolutions for this requisition, bound to the cascade digest
  they repair.  Returns the decision map keyed by hole id when the context
  names this cascade, else a typed mismatch (never a silent wrong answer)."
  [{:keys [cascade-digest phase] :as _ctx}]
  (if (and (= cascade-digest
              "2410969ef313c6fa239121e7fbfc061a49c902da9119a879faee0d14d1c36e2d")
           (= :compile phase))
    clauses
    {:status :error
     :kind :patch-cascade-context-mismatch
     :cascade-digest cascade-digest
     :phase phase}))
