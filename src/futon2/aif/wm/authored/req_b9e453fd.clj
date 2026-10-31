(ns futon2.aif.wm.authored.req-b9e453fd
  "Authored operational clauses for held executable cascade
  f2b33ac45c1c87b240070acf4c6a41e2cc5683a75a1e1522be8b276e7ea49c8c
  (phase :compile, requisition
  b9e453fdf6d6253fcefa8c686e4ee5387c1e949f3a4c8d8fc42f4bbd1d57d3ad).

  Resolutions for the eighteen :operational-clause-owed holes owned by
  :wm/enactment-compiler.  Each decision is an operational clause routed
  through the single routable capability :wm/pattern-apply; the operation
  names the pattern application that repairs the hole, completion-check
  names the observable condition, and :consumes/:produces describe the
  transition.  No claim is made about holes outside this requisition."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["cascade-construction/read-what-exists-first" 0]
   {:operation :apply-read-what-exists-first-to-target-identity
    :capability :wm/pattern-apply
    :consumes :bare-mission-identifier
    :produces :read-anchored-target-identity
    :completion-check
    [:target-identity-read-from-existing-doc
     "the cascade's target identity is read from the existing mission document and cross-references rather than restated from the mission name alone; the reading cites the actual sources and their disagreements"]}
   ["agent/state-is-hypothesis" 1]
   {:operation :apply-state-is-hypothesis-to-open-criteria
    :capability :wm/pattern-apply
    :consumes :cert-half-cleanliness-treated-as-live-proof
    :produces :open-criteria-held-as-revisable-hypotheses
    :completion-check
    [:criteria-one-two-stay-open-until-live-discharge
     "criteria 1-2 remain open, held as revisable hypotheses with the empty ledger as the falsifying observation; they update only when a live discharge flows through the cert-half, never on self-test success alone"]}
   ["agent/evidence-over-assertion" 2]
   {:operation :apply-evidence-over-assertion-to-criterion-closure
    :capability :wm/pattern-apply
    :consumes :unanchored-criterion-closure-claim
    :produces :probe-anchored-closure-record
    :completion-check
    [:closure-cites-executed-guard-probe
     "criterion 3's closure record cites the executed guard probe with date and the 3/3 rejection tally, so the closure is checkable rather than an assertion; any future closure follows the same anchor rule"]}
   ["agent/eat-your-own-tail" 3]
   {:operation :apply-eat-your-own-tail-to-value-channel-wiring
    :capability :wm/pattern-apply
    :consumes :discharge-value-left-outside-the-loop
    :produces :certified-discharge-wired-into-value-channel
    :completion-check
    [:live-loop-consumes-certified-discharge
     "the certified discharge is attached to the learning loop's live value channel so run outcomes steer the next run, while outcome judgment stays independent of the loop's own signals"]}
   ["agent/evidence-over-assertion" 4]
   {:operation :apply-evidence-over-assertion-to-success-attestation
    :capability :wm/pattern-apply
    :consumes :self-graded-it-worked-claim
    :produces :externally-attested-success-signal
    :completion-check
    [:success-signal-originates-outside-the-prior
     "the 'it worked' signal entering the value channel originates from the world outside the prior (external attestation through the discharge), and a prior-issued self-grade is rejected as a success source"]}
   ["cascade-construction/read-what-exists-first" 5]
   {:operation :apply-read-what-exists-first-to-portfolio-placement
    :capability :wm/pattern-apply
    :consumes :assumed-portfolio-placement
    :produces :read-portfolio-placement-with-consumer
    :completion-check
    [:placement-read-from-existing-portfolio-docs
     "the mission's constituency in C-pudding-prover and its first-machine-consumer relation to M-pudding-peradams are read from the existing portfolio documents and cited, not assumed from the mission statement"]}
   ["agent/trail-enables-return" 6]
   {:operation :apply-trail-enables-return-to-seam-pointer
    :capability :wm/pattern-apply
    :consumes :unnamed-seam-and-consumer
    :produces :queryable-seam-pointer-record
    :completion-check
    [:seam-pointer-names-consumer-and-definition
     "the machine-consumer seam is recorded as a queryable pointer naming both the consuming mission (M-pudding-peradams) and the seam's definition site (M-differentiable-substrate section 8.7), so later work returns without re-derivation"]}
   ["agent/scope-before-action" 7]
   {:operation :apply-scope-before-action-to-phase-declaration
    :capability :wm/pattern-apply
    :consumes :undeclared-mission-phase
    :produces :declared-phase-with-exit-condition
    :completion-check
    [:phase-declared-before-substantive-action
     "the mission's phase (IDENTIFY at drafting) is declared with its exit condition before substantive build action proceeds, so scope violations require explicit expansion rather than silent drift"]}
   ["agent/handoff-preserves-context" 8]
   {:operation :apply-handoff-preserves-context-to-pool-assignment
    :capability :wm/pattern-apply
    :consumes :cold-start-pool-transfer
    :produces :recorded-handoff-with-planning-anchor
    :completion-check
    [:handoff-names-pools-and-planning-source
     "the pool assignment records the drafting pool (Opus via PM handoff), the build pool (codex), and the upstream planning record (the futonzero-alphazero.md planning session), so receiving agents do not cold-start"]}
   ["agent/scope-before-action" 9]
   {:operation :apply-scope-before-action-to-principal-declaration
    :capability :wm/pattern-apply
    :consumes :direction-and-acceptance-without-authority
    :produces :named-principal-in-mission-scope
    :completion-check
    [:principal-named-at-mission-scope
     "the mission's principal (Joe) is named at scope so direction changes and acceptance are auditable to a single authority"]}
   ["agent/trail-enables-return" 10]
   {:operation :apply-trail-enables-return-to-prover-locator
    :capability :wm/pattern-apply
    :consumes :unlocatable-prover-machine
    :produces :repository-and-file-locator-record
    :completion-check
    [:prover-locator-names-repo-and-corpus-file
     "the prover machine's locator names the repository (futon7) and the corpus file (peradams-corpus.edn), so the machine is returnable without re-searching the tree"]}
   ["agent/trail-enables-return" 11]
   {:operation :apply-trail-enables-return-to-return-channel-locator
    :capability :wm/pattern-apply
    :consumes :untraceable-value-return-path
    :produces :named-return-channel-record
    :completion-check
    [:return-channel-names-repo-and-role
     "the R2 return channel is recorded with its repository (futon2) and role among the seam repositories, so the value's return path is traceable when the loop is wired"]}
   ["agent/eat-your-own-tail" 12]
   {:operation :apply-eat-your-own-tail-to-prior-training-target
    :capability :wm/pattern-apply
    :consumes :cold-prior-outside-the-loop
    :produces :prior-named-as-live-training-destination
    :completion-check
    [:certified-value-reaches-the-futon6-prior
     "the certified value's destination is recorded as the futon6 prior it eventually trains/calibrates, so run outcomes are routed back into the mechanism rather than left as post-hoc summaries"]}
   ["cascade-construction/read-what-exists-first" 13]
   {:operation :apply-read-what-exists-first-to-recognition-criterion
    :capability :wm/pattern-apply
    :consumes :restated-recognition-criterion
    :produces :cited-recognition-criterion-and-certificate
    :completion-check
    [:criterion-cited-from-source-mission
     "the recognition criterion and three-witness certificate are cited from M-pudding-peradams rather than paraphrased, so later work measures against the recorded definition"]}
   ["agent/trail-enables-return" 14]
   {:operation :apply-trail-enables-return-to-substrate-citation
    :capability :wm/pattern-apply
    :consumes :vague-substrate-cross-reference
    :produces :file-and-section-pinned-citation
    :completion-check
    [:citation-pins-file-and-section
     "the substrate seam citation pins both the file path (futon6/holes/missions/M-differentiable-substrate.md) and the section (8.7), making the seam's definition directly returnable"]}
   ["agent/eat-your-own-tail" 15]
   {:operation :apply-eat-your-own-tail-to-two-sided-seam-naming
    :capability :wm/pattern-apply
    :consumes :one-directional-seam-reference
    :produces :two-sided-seam-binding
    :completion-check
    [:seam-named-from-both-sides
     "the seam is named from both sides (R2-CH2 and M-pudding-peradams), binding the return channel and the mission so the observation path stays live for subsequent runs on each side"]}
   ["coordination/bind-promotion-to-post-repair-replay" 16]
   {:operation :apply-bind-promotion-to-post-repair-replay-to-rollout-gate
    :capability :wm/pattern-apply
    :consumes :local-wiring-success-treated-as-promotion-discharge
    :produces :promotion-gated-on-independent-replay-receipt
    :completion-check
    [:promotion-requires-matching-independent-replay-receipt
     "any promotion of the wired loop under the MUST-B obligation stays pending until a successful independent replay receipt matches the repaired revision and the rejected controls; local test passes and receipts for other revisions do not discharge it"]}
   ["agent/state-is-hypothesis" 17]
   {:operation :apply-state-is-hypothesis-to-self-graded-reward
    :capability :wm/pattern-apply
    :consumes :self-graded-reward-treated-as-true-success
    :produces :falsifiable-reward-hypothesis-with-external-falsifier
    :completion-check
    [:reward-held-as-hypothesis-with-world-falsifier
     "the self-graded reward channel is held as a falsifiable hypothesis with external attestation as the falsifier, so contradiction by the world's verdict triggers update rather than defense of the self-grade"]}})

(defn decisions
  "The hole resolutions for this requisition, bound to the cascade digest
  they repair.  Returns the decision map keyed by hole id when the context
  names this cascade, else a typed mismatch (never a silent wrong answer)."
  [{:keys [cascade-digest phase] :as _ctx}]
  (if (and (= cascade-digest
              "f2b33ac45c1c87b240070acf4c6a41e2cc5683a75a1e1522be8b276e7ea49c8c")
           (= :compile phase))
    clauses
    {:status :error
     :kind :patch-cascade-context-mismatch
     :cascade-digest cascade-digest
     :phase phase}))
