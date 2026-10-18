(ns futon2.aif.wm.authored.req-b0246e5b
  "Authored operational clauses for held executable cascade
  94c73bd78c2087bca94da66414a7eb2492c2ffb8dd18a05bc5ef7da304c71aa7
  (phase :compile, requisition b0246e5b5b418d3e3c7920c201225d9ec56528425e136eb9058fc3bba1ceef0d).

  Resolutions for the nine :operational-clause-owed holes of the
  M-digital-nomad-patterns reading.  Each decision is an operational clause
  attributed to the reading's occurrence squares (analysis digest
  5732b6410709d25a03f8f4a9edafe8fc95ae708d25f0660ecf8f9642c472f216):
  the operation names what the occurrence enacts in the mission text,
  completion-check names the observable condition in that text, and
  :consumes/:produces describe the transition.  Nothing here claims a
  square-derived completion and no application-order edge is authored."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["agent/handoff-preserves-context" 0]
   {:operation :define-head-entry-record
    :capability :wm/pattern-apply
    :consumes :mission-framing-paragraphs
    :produces :head-entry-record
    :completion-check
    [:head-marks-entry
     "mission carries a ## HEAD section fixing the motivating picture, terrain inputs and authored pattern language"]}
   ["agent/state-is-hypothesis" 1]
   {:operation :frame-landscape-position-as-revisable
    :capability :wm/pattern-apply
    :consumes :landscape-position-casting
    :produces :revisable-terrain-model
    :completion-check
    [:role-casting-recorded
     "mission records the landscape position as playing the Futon City role, an analogy open to revision"]}
   ["agent/scope-before-action" 2]
   {:operation :name-terrain-before-question
    :capability :wm/pattern-apply
    :consumes :motivating-picture
    :produces :named-terrain
    :completion-check
    [:landscape-named-before-asking
     "mission names 'a landscape' in the motivating picture before asking what to do"]}
   ["agent/trail-enables-return" 3]
   {:operation :record-dated-terrain-citations
    :capability :wm/pattern-apply
    :consumes :terrain-source-documents
    :produces :dated-trail-anchors
    :completion-check
    [:dated-paths-cited
     "mission cites TN-deep-research-landscape-position-2026-07.md and FINDINGS-2026-07-27.md by full dated path"]}
   ["agent/evidence-over-assertion" 4]
   {:operation :anchor-answer-on-findings
    :capability :wm/pattern-apply
    :consumes :findings-note
    :produces :anchored-terrain-input
    :completion-check
    [:terrain-input-named
     "mission declares the FINDINGS note as the terrain input rather than asserting terrain from memory"]}
   ["agent/sense-deliberate-act" 5]
   {:operation :order-sense-before-answer
    :capability :wm/pattern-apply
    :consumes :terrain-citations-and-question
    :produces :ordered-response-rhythm
    :completion-check
    [:question-follows-sensing
     "mission places the 'what do we do' question after the terrain citations, not before"]}
   ["aif/candidate-pattern-action-space" 6]
   {:operation :construct-bounded-pattern-language
    :capability :wm/pattern-apply
    :consumes :movement-answer-space
    :produces :small-pattern-language
    :completion-check
    [:bounded-language-chosen
     "mission expresses the answer as a small pattern language, bounding the candidate movement set"]}
   ["agency/self-attribution" 7]
   {:operation :record-author-and-date-verbatim
    :capability :wm/pattern-apply
    :consumes :pattern-language-text
    :produces :attributed-pattern-language
    :completion-check
    [:author-dated-verbatim-recorded
     "mission records the pattern language as authored below (Joe, 2026-07-27, verbatim)"]}
   ["agent/reduction-to-kernel" 8]
   {:operation :reject-one-off-plan
    :capability :wm/pattern-apply
    :consumes :response-form-choice
    :produces :reusable-response-kernel
    :completion-check
    [:one-off-plan-rejected
     "mission rejects a one-off plan in favour of the reusable pattern language"]}})

(defn decisions
  "The hole resolutions for this requisition, bound to the cascade digest
  they repair.  Returns the decision map keyed by hole id when the context
  names this cascade, else a typed mismatch (never a silent wrong answer)."
  [{:keys [cascade-digest phase] :as _ctx}]
  (if (and (= cascade-digest
              "94c73bd78c2087bca94da66414a7eb2492c2ffb8dd18a05bc5ef7da304c71aa7")
           (= :compile phase))
    clauses
    {:status :error
     :kind :patch-cascade-context-mismatch
     :cascade-digest cascade-digest
     :phase phase}))
