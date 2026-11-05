(ns futon2.aif.wm.authored.req-1b668612
  "Authored operational clauses for held executable cascade
  79645d0a816ca07869d9ce867c9fd5d6789645d253a9f476f043d900b3d1c880
  (phase :compile, requisition
  1b6686125d6cfd2e5fff6a3343f0dc042baa11533c5e2999ef9aa346ee5852c4).

  Resolutions for the twenty :operational-clause-owed holes of the
  M-aif-policy-conditioned-eig reading selected 2026-11-05.  Each decision
  is an operational clause attributed to what the pattern occurrence
  enacts in the mission text (holes/missions/M-aif-policy-conditioned-eig.md)
  and, for the promotion hole, the close-stage finding
  repair-occ-25dc2faf0ef641860133dc7c67b828163c3415d7ff980cd04861f62669920a38
  bound to this mission.  The operation names what the occurrence enacts in
  that text, completion-check names the observable condition in that text,
  and :consumes/:produces describe the transition.  Nothing here claims a
  square-derived completion and no application-order edge is authored."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["cascade-construction/read-what-exists-first" 0]
   {:operation :enumerate-existing-components
    :capability :wm/pattern-apply
    :consumes :mission-map-section
    :produces :existing-components-record
    :completion-check
    [:existing-components-table-present
     "mission MAP section enumerates existing components with locations and status (epistemic_value.clj, bmr.clj, a4a.clj, forward_model.clj, belief.clj, efe.clj) before naming the missing join"]}
   ["agent/state-is-hypothesis" 1]
   {:operation :record-honest-kernel-status
    :capability :wm/pattern-apply
    :consumes :current-status-fragment
    :produces :kernel-status-hypothesis-record
    :completion-check
    [:kernel-exists-model-does-not
     "mission's 'Current honest status' records 'The mathematical kernel exists. The generative experiment model does not.'"]}
   ["agent/commitment-varies-with-confidence" 1]
   {:operation :gate-replacement-on-shadow-evidence
    :capability :wm/pattern-apply
    :consumes :stage-4-shadow-plan
    :produces :conditioned-operator-decision-record
    :completion-check
    [:shadow-before-replacement
     "mission DERIVE stage 4 requires shadow evidence (magnitude, winner/abstain changes, calibration) before Joe decides whether EIG replaces the proxy"]}
   ["agent/evidence-over-assertion" 2]
   {:operation :record-explicit-non-solution
    :capability :wm/pattern-apply
    :consumes :non-solution-fragment
    :produces :posterior-entropy-exclusion-record
    :completion-check
    [:posterior-entropy-not-eig
     "mission's 'Explicit non-solution' states futon3c posterior-entropy is action-posterior entropy and 'must not be ported as the repair for this mission'"]}
   ["agent/eat-your-own-tail" 3]
   {:operation :require-shared-updater-path
    :capability :wm/pattern-apply
    :consumes :c5-shared-updater-criterion
    :produces :shared-updater-obligation
    :completion-check
    [:c5-shared-updater-named
     "mission completion criterion C5 requires simulated and realised posterior updates call the same implementation with only the observation source changed"]}
   ["agent/evidence-over-assertion" 4]
   {:operation :cite-b2-measurement
    :capability :wm/pattern-apply
    :consumes :b2-machine-proposed-items
    :produces :cited-b2-measurement-record
    :completion-check
    [:b2-measurement-cited
     "mission's B2 section cites the 2026-09-05 measurement and the raw output under holes/labs/wm-contract/runs/B2-strawman/ for every surviving item"]}
   ["cascade-construction/run-it-on-a-real-case" 4]
   {:operation :require-deterministic-shadow-artifact
    :capability :wm/pattern-apply
    :consumes :verify-instantiate-plan-step-4
    :produces :real-case-shadow-artifact-obligation
    :completion-check
    [:shadow-artifact-path-named
     "mission VERIFY plan step 4 requires a deterministic shadow artifact under holes/labs/M-aif-policy-conditioned-eig/"]}
   ["cascade-construction/read-what-exists-first" 5]
   {:operation :enumerate-scope-out
    :capability :wm/pattern-apply
    :consumes :scope-out-fragment
    :produces :scope-out-record
    :completion-check
    [:scope-out-enumerated
     "mission 'Scope out' forbids calling current action-posterior entropy EIG and treating posterior spread alone as expected reduction"]}
   ["agent/trail-enables-return" 6]
   {:operation :require-trace-provenance
    :capability :wm/pattern-apply
    :consumes :c8-provenance-criterion
    :produces :trace-provenance-obligation
    :completion-check
    [:c8-provenance-fields-named
     "mission completion criterion C8 requires traces carry model identity, prior entropy, expected posterior entropy, EIG, and any fail-closed reason"]}
   ["agent/scope-before-action" 7]
   {:operation :require-observation-closure
    :capability :wm/pattern-apply
    :consumes :c2-closure-criterion
    :produces :observation-alphabet-obligation
    :completion-check
    [:c2-closure-cases-named
     "mission completion criterion C2 requires every policy's Q(o|π,θ) to cover no-result, failure, timeout, conflicting evidence, and missing evidence"]}
   ["agent/handoff-preserves-context" 8]
   {:operation :require-dark-flag-selection-invariance
    :capability :wm/pattern-apply
    :consumes :stage-3-dark-wiring-plan
    :produces :selection-invariance-record
    :completion-check
    [:dark-flag-byte-identical
     "mission DERIVE stage 3 requires the mode flag to preserve byte-identical selection when off"]}
   ["agent/scope-before-action" 9]
   {:operation :condition-proxy-demotion
    :capability :wm/pattern-apply
    :consumes :scope-in-item-8
    :produces :posterior-spread-demotion-record
    :completion-check
    [:demotion-only-once-eig-live
     "mission scope-in item 8 retires or demotes posterior spread to telemetry only once EIG is live"]}
   ["agent/trail-enables-return" 10]
   {:operation :name-single-commit-reversal
    :capability :wm/pattern-apply
    :consumes :b2-reversal-fragment
    :produces :reversal-trail-record
    :completion-check
    [:reversal-sha-named
     "mission B2 section names a single git revert of the one commit whose sha is recorded in holes/labs/wm-contract/proposals/STRAWMAN-M-aif-policy-conditioned-eig.md as the reversal"]}
   ["agent/trail-enables-return" 11]
   {:operation :record-replacement-discipline
    :capability :wm/pattern-apply
    :consumes :c10-replacement-criterion
    :produces :replacement-discipline-record
    :completion-check
    [:c10-invariants-and-shadow
     "mission completion criterion C10 keeps model-uncertainty-bonus in place until placeholder/cascade selection invariants remain green and a shadow has quantified winner, abstain, and scale effects"]}
   ["agent/eat-your-own-tail" 12]
   {:operation :require-reduction-tests
    :capability :wm/pattern-apply
    :consumes :c7-reduction-criterion
    :produces :reduction-test-obligation
    :completion-check
    [:c7-reduction-anchors-named
     "mission completion criterion C7 requires uninformative policy EIG zero, perfectly discriminating binary experiment EIG ln 2, and relabelling invariance"]}
   ["cascade-construction/read-what-exists-first" 13]
   {:operation :record-related-missions
    :capability :wm/pattern-apply
    :consumes :mission-header
    :produces :related-mission-cross-refs
    :completion-check
    [:related-missions-listed
     "mission header lists related missions M-aif-faithfulness, M-aif-a-matrix-faithfulness, and M-evaluate-policies"]}
   ["agent/trail-enables-return" 14]
   {:operation :require-prospective-calibration
    :capability :wm/pattern-apply
    :consumes :c9-calibration-criterion
    :produces :prospective-calibration-obligation
    :completion-check
    [:c9-held-out-metrics-named
     "mission completion criterion C9 requires held-out log loss/Brier evaluation of Q(o|π) and comparison of predicted with realised entropy reductions"]}
   ["agent/eat-your-own-tail" 15]
   {:operation :record-sole-updater-authority
    :capability :wm/pattern-apply
    :consumes :b2-completed-item-1
    :produces :updater-authority-record
    :completion-check
    [:a4a-update-posterior-sole-authority
     "mission records a4a/update-posterior as the sole Dirichlet transition authority with corpus-realised and hypothetical paths through observed-posterior/hypothetical-posterior"]}
   ["coordination/bind-promotion-to-post-repair-replay" 16]
   {:operation :bind-promotion-to-close-repair-replay
    :capability :wm/pattern-apply
    :consumes :close-stage-failure-finding
    :produces :post-repair-replay-binding
    :completion-check
    [:close-failure-bound-to-repair
     "the held :close-stage 'request timed out' failure of M-aif-policy-conditioned-eig is recorded as finding repair-occ-25dc2faf0ef641860133dc7c67b828163c3415d7ff980cd04861f62669920a38 with ticket OPEN until its scoped work is accepted"]}
   ["agent/state-is-hypothesis" 17]
   {:operation :condition-box1-update
    :capability :wm/pattern-apply
    :consumes :c11-honest-update-criterion
    :produces :box1-update-condition-record
    :completion-check
    [:c11-distinguishes-kernel-from-load-bearing
     "mission completion criterion C11 changes Box 1 only after live provenance and prospective evidence distinguish 'kernel exists' from 'epistemic value is load-bearing'"]}})

(defn decisions
  "The hole resolutions for this requisition, bound to the cascade digest
  they repair.  Returns the decision map keyed by hole id when the context
  names this cascade, else a typed mismatch (never a silent wrong answer)."
  [{:keys [cascade-digest phase] :as _ctx}]
  (if (and (= cascade-digest
              "79645d0a816ca07869d9ce867c9fd5d6789645d253a9f476f043d900b3d1c880")
           (= :compile phase))
    clauses
    {:status :error
     :kind :patch-cascade-context-mismatch
     :cascade-digest cascade-digest
     :phase phase}))
