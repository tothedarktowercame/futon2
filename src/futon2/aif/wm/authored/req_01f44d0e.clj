(ns futon2.aif.wm.authored.req-01f44d0e
  "Authored operational clauses for held executable cascade
  c7df437f1324beb4b66b3ef0d0640c886c7a4fdb9807f20fe6e91879129be29e
  (phase :compile, requisition 01f44d0e561d29ae4d17b580420e74387a3ea3cfdb518ddb91debeb16bf72a90).

  Resolutions for the nine :operational-clause-owed holes owned by
  :wm/enactment-compiler in this requisition.  Each decision is an
  operational clause attributed to the occurrence squares of the
  E-comprehension-foundation reading (analysis digest
  3ba4b0ade3d0f168d230d53de9ade37f17825be1449675a2d11c50ab14110324), whose
  spans index the excursion opening at
  futon6/holes/excursions/E-comprehension-foundation.md (title, verdict,
  byline and framing paragraph): the operation names what the occurrence
  enacts in that text, completion-check names the observable condition
  there, and :consumes/:produces describe the transition.  No claim is
  made about holes outside this requisition and no application-order edge
  is authored."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["agent/scope-before-action" 0]
   {:operation :bind-title-to-topic-declaration
    :capability :wm/pattern-apply
    :consumes :excursion-title-line
    :produces :topic-declaration-record
    :completion-check
    [:title-names-corpus-relative-comprehension
     "excursion title reads '# E-comprehension-foundation — corpus-relative comprehension as the foundation' (excerpt chars 0-78)"]}
   ["stack-coherence/evidence-ledger" 0]
   {:operation :require-evidence-before-foundation-settled
    :capability :wm/pattern-apply
    :consumes :foundational-claim-in-title
    :produces :foundation-claim-pending-evidence-record
    :completion-check
    [:foundation-claim-not-marked-done
     "verdict line classifies the note OPEN with 'no completion marker in head or tail', so the foundational claim is held pending evidence"]}
   ["agent/state-is-hypothesis" 1]
   {:operation :record-open-provisional-status
    :capability :wm/pattern-apply
    :consumes :verdict-line
    :produces :revisable-status-hypothesis
    :completion-check
    [:status-open-with-explicit-pilot-question
     "verdict reads 'OPEN — Foundational design note with an explicitly open pilot question and no completion marker in head or tail' (excerpt chars 119-231)"]}
   ["agent/commitment-varies-with-confidence" 2]
   {:operation :modulate-classification-commitment
    :capability :wm/pattern-apply
    :consumes :classifier-confidence-annotation
    :produces :provisional-low-commitment-classification
    :completion-check
    [:low-confidence-and-author-confirmation-outstanding
     "verdict parenthetical reads 'WM status classification by zai-4, low confidence; not yet confirmed by the author' (excerpt chars 232-319)"]}
   ["agency/self-attribution" 3]
   {:operation :record-signed-byline
    :capability :wm/pattern-apply
    :consumes :note-byline
    :produces :signed-authorship-record
    :completion-check
    [:byline-names-claude-1-joe-and-date
     "byline reads '*claude-1 + Joe, 2026-06-22.' (excerpt chars 321-349)"]}
   ["agent/scope-before-action" 4]
   {:operation :announce-architecture-description-scope
    :capability :wm/pattern-apply
    :consumes :framing-paragraph-opening
    :produces :artifact-description-scope-record
    :completion-check
    [:holistic-architecture-under-readiness-named
     "framing names 'The holistic architecture under the readiness' cards as the artifact to be described (excerpt chars 350-395)"]}
   ["agent/evidence-over-assertion" 5]
   {:operation :ground-cards-in-pipeline-outputs
    :capability :wm/pattern-apply
    :consumes :readiness-cards-claim
    :produces :output-grounded-cards-record
    :completion-check
    [:cards-explain-what-pipeline-produces
     "framing commits the cards to 'what the pipeline produces' rather than aspiration (excerpt chars 396-432)"]}
   ["agent/state-is-hypothesis" 6]
   {:operation :delimit-improvement-claim
    :capability :wm/pattern-apply
    :consumes :improvement-claim-with-domain
    :produces :falsifiable-improvement-hypothesis
    :completion-check
    [:improves-as-we-run-explained-and-delimited
     "framing demands why '\"improves as we run\" is true (and where)' (excerpt chars 433-478), delimiting the claim's domain"]}
   ["agent/handoff-preserves-context" 8]
   {:operation :preserve-foundation-for-phase-3
    :capability :wm/pattern-apply
    :consumes :foundation-maintained-by-discipline
    :produces :phase-3-assumable-foundation
    :completion-check
    [:phase-3-can-assume-foundation
     "closing line reads 'so Phase 3 can assume it' (excerpt chars 530-556), conditioning Phase 3 on the preserved foundation"]}})

(defn decisions
  "The hole resolutions for this requisition, bound to the cascade digest
  they repair.  Returns the decision map keyed by hole id when the context
  names this cascade, else a typed mismatch (never a silent wrong answer)."
  [{:keys [cascade-digest phase] :as _ctx}]
  (if (and (= cascade-digest
              "c7df437f1324beb4b66b3ef0d0640c886c7a4fdb9807f20fe6e91879129be29e")
           (= :compile phase))
    clauses
    {:status :error
     :mismatch {:cascade-digest cascade-digest
                :phase phase
                :expected-cascade-digest "c7df437f1324beb4b66b3ef0d0640c886c7a4fdb9807f20fe6e91879129be29e"
                :expected-phase :compile}}))
