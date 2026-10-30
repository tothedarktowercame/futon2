(ns futon2.aif.wm.authored.req-ea0692dd
  "Authored operational clauses for held executable cascade
  dd69db0e977f8ac60e74923fb5da066b6503817b5054df608acab8a92489fefe
  (phase :compile, requisition ea0692dd730e6fb618f4f1502d7559f35f959c9cd0107c24d2a6f041c8f94cbc).

  Resolutions for the eleven :operational-clause-owed holes of the
  M-peradam-grounding reading (analysis digest
  5910a9d721354274cd1be052baa692a6cd7d20acacc836f99d0fcbdd3a6031b7).
  Each decision is an operational clause attributed to the reading's
  occurrence squares: the operation names what the occurrence enacts in the
  mission text, completion-check names the observable condition in that
  text, and :consumes/:produces describe the transition.  Nothing here
  claims a square-derived completion and no application-order edge is
  authored."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["contracts/holder-states-the-claim" 0]
   {:operation :state-mission-holder-in-header
    :capability :wm/pattern-apply
    :consumes :mission-identity
    :produces :mission-holder-record
    :completion-check
    [:mission-id-names-holder
     "mission header names M-peradam-grounding as the holder its claims resolve to"]}
   ["test-registry/judge-adequacy" 2]
   {:operation :record-open-criteria-with-named-gap
    :capability :wm/pattern-apply
    :consumes :cert-half-self-test-result
    :produces :open-criteria-record
    :completion-check
    [:criteria-open-until-live-discharge
     "mission status records criteria 1-3 OPEN because no live discharge has flowed through the cert-half and the ledger is empty"]}
   ["agent/evidence-over-assertion" 3]
   {:operation :wire-certified-discharge-into-value-channel
    :capability :wm/pattern-apply
    :consumes :certified-discharge-object
    :produces :evidence-anchored-value-signal
    :completion-check
    [:value-channel-carries-certified-discharge
     "mission imperative wires the peradam — the certified discharge — into the learning loop's value channel"]}
   ["contracts/holder-states-the-claim" 7]
   {:operation :record-phase-and-drafter-provenance
    :capability :wm/pattern-apply
    :consumes :phase-provenance-fragment
    :produces :phase-holder-record
    :completion-check
    [:phase-and-drafter-named
     "mission records Phase: IDENTIFY drafted by Fable on 2026-06-10 so the stage holder is stated, not implied"]}
   ["mission-coherence/logic-model-before-code" 8]
   {:operation :fix-dispatch-plan-before-build
    :capability :wm/pattern-apply
    :consumes :planning-session-output
    :produces :fixed-dispatch-plan
    :completion-check
    [:pools-assigned-before-code
     "mission dispatch plan assigns PM handoff to the Opus pool and build to the codex pool before implementation"]}
   ["contracts/holder-states-the-claim" 9]
   {:operation :name-principal-as-claim-holder
    :capability :wm/pattern-apply
    :consumes :principal-line
    :produces :principal-holder-record
    :completion-check
    [:principal-stated-explicitly
     "mission states Principal: Joe, the holder of the mission's decision rights"]}
   ["agent/evidence-over-assertion" 10]
   {:operation :anchor-prover-role-to-repo-and-corpus
    :capability :wm/pattern-apply
    :consumes :repo-role-claim
    :produces :anchored-repo-map
    :completion-check
    [:prover-anchored-to-futon7-corpus
     "mission anchors the prover-machine role to futon7 and names peradams-corpus.edn"]}
   ["agent/state-is-hypothesis" 12]
   {:operation :hold-prior-as-revisable-hypothesis
    :capability :wm/pattern-apply
    :consumes :futon6-prior
    :produces :calibratable-prior-record
    :completion-check
    [:prior-trains-on-discharge-value
     "mission names futon6 as the prior the discharge value eventually trains and calibrates, not a fixed truth"]}
   ["forward-model/seal-the-graduation-criteria" 13]
   {:operation :bind-to-sealed-recognition-criterion
    :capability :wm/pattern-apply
    :consumes :sibling-mission-criterion
    :produces :sealed-graduation-bar
    :completion-check
    [:criterion-plus-three-witnesses-fixed
     "mission binds graduation to M-pudding-peradams' fixed recognition criterion and three-witness certificate, held outside this mission's editing reach"]}
   ["contracts/holder-states-the-claim" 16]
   {:operation :resolve-policy-obligations-to-named-holder
    :capability :wm/pattern-apply
    :consumes :policy-cross-reference
    :produces :policy-holder-record
    :completion-check
    [:must-b-resolved-to-wm-policies-doc
     "mission resolves rollout, promote! and MUST-B obligations to futon2/holes/M-wm-policies.md as the statement holder"]}
   ["agent/evidence-over-assertion" 17]
   {:operation :reject-self-graded-reward-as-value-source
    :capability :wm/pattern-apply
    :consumes :reward-source-warning
    :produces :goodhart-guard-record
    :completion-check
    [:success-world-attested-not-self-graded
     "mission warns reward-is-self-graded is Goodhart's door and requires success to be something the world says, not the prior about itself"]}})

(defn decisions
  "The hole resolutions for this requisition, bound to the cascade digest
  they repair.  Returns the decision map keyed by hole id when the context
  names this cascade, else a typed mismatch (never a silent wrong answer)."
  [{:keys [cascade-digest phase] :as _ctx}]
  (if (and (= cascade-digest
              "dd69db0e977f8ac60e74923fb5da066b6503817b5054df608acab8a92489fefe")
           (= :compile phase))
    clauses
    {:status :error
     :kind :patch-cascade-context-mismatch
     :cascade-digest cascade-digest
     :phase phase}))
