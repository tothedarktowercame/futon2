(ns futon2.aif.wm.authored.req-f0ba279a
  "Authored operational clauses for held executable cascade
  af6294ee687ca69fac2cb0ddd7e75c92cccf38e716a5082f3a3333d41e218f91
  (phase :compile, requisition f0ba279aab94dd4656a995a4d854484b93bc8ac4e5ebf2b6b4566b2752b5ed87).

  Resolutions for the nine :operational-clause-owed holes of the
  M-futonzero-prelim-practice reading (analysis digest
  dcaa2395d93d270bd2c2e7c61c178d1ed43bc41b938013641dfd3485d9d57ead).
  Each decision is an operational clause attributed to the reading's
  occurrence squares: the operation names what the occurrence enacts in the
  mission text, completion-check names the observable condition in that
  text, and :consumes/:produces describe the transition.  Nothing here
  claims a square-derived completion and no application-order edge is
  authored."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["cascade-construction/read-what-exists-first" 0]
   {:operation :record-mission-identity-read
    :capability :wm/pattern-apply
    :consumes :mission-header-fragment
    :produces :mission-identity-record
    :completion-check
    [:mission-title-names-file
     "mission header names M-futonzero-prelim-practice as the file being read"]}
   ["apparatus/the-system-stops-on-schedule" 1]
   {:operation :pin-open-head-lifecycle-stage
    :capability :wm/pattern-apply
    :consumes :mission-status-banner
    :produces :open-head-stage-record
    :completion-check
    [:status-reads-open-head
     "mission status banner reads 'OPEN — HEAD'"]}
   ["agent/evidence-over-assertion" 2]
   {:operation :date-operator-session-anchors
    :capability :wm/pattern-apply
    :consumes :operator-session-fragments
    :produces :dated-operator-session-record
    :completion-check
    [:sessions-cited-by-date
     "mission cites the operator sessions by explicit dates, 'operator shape captured 2026-08-15' and 'second operator session 2026-09-28'"]}
   ["problems/process-conduct-is-unassured" 4]
   {:operation :record-design-not-build-boundary
    :capability :wm/pattern-apply
    :consumes :scope-constraint-fragment
    :produces :design-not-build-boundary-record
    :completion-check
    [:sketch-bounded-as-design-not-build
     "mission bounds the design sketch 'here *as design*, not build'"]}
   ["apparatus/the-system-stops-on-schedule" 5]
   {:operation :withhold-identify-phase-advance
    :capability :wm/pattern-apply
    :consumes :lifecycle-phase-fragment
    :produces :phase-not-reached-record
    :completion-check
    [:identify-explicitly-not-reached
     "mission states 'Not yet IDENTIFY'"]}
   ["problems/r10-scheduled-entrypoint" 6]
   {:operation :delegate-phase-discipline-to-lifecycle-doc
    :capability :wm/pattern-apply
    :consumes :lifecycle-authority-reference
    :produces :lifecycle-authority-binding
    :completion-check
    [:discipline-cited-by-path
     "mission cites `futon4/holes/mission-lifecycle.md` as the authority for phase discipline"]}
   ["problems/process-conduct-is-unassured" 8]
   {:operation :record-head-is-not-design-rule
    :capability :wm/pattern-apply
    :consumes :head-conduct-fragment
    :produces :head-not-design-rule-record
    :completion-check
    [:head-explicitly-not-design
     "mission states of HEAD 'It is not design'"]}
   ["inbox-zero/promote-at-turn-end" 9]
   {:operation :scope-constraint-over-appended-content
    :capability :wm/pattern-apply
    :consumes :forward-reference-fragment
    :produces :banner-scoped-content-binding
    :completion-check
    [:constraint-scoped-to-everything-below
     "mission scopes the banner's constraint with 'Nothing below'"]}
   ["apparatus/the-system-stops-on-schedule" 10]
   {:operation :refuse-curriculum-scope-advance
    :capability :wm/pattern-apply
    :consumes :scope-refusal-fragment
    :produces :no-curriculum-scope-record
    :completion-check
    [:curriculum-explicitly-unspecified
     "mission closes the banner with 'Nothing below specifies a curriculum'"]}})

(defn decisions
  "The hole resolutions for this requisition, bound to the cascade digest
  they repair.  Returns the decision map keyed by hole id when the context
  names this cascade, else a typed mismatch (never a silent wrong answer)."
  [{:keys [cascade-digest phase] :as _ctx}]
  (if (and (= cascade-digest
              "af6294ee687ca69fac2cb0ddd7e75c92cccf38e716a5082f3a3333d41e218f91")
           (= :compile phase))
    clauses
    {:status :error
     :kind :patch-cascade-context-mismatch
     :cascade-digest cascade-digest
     :phase phase}))
