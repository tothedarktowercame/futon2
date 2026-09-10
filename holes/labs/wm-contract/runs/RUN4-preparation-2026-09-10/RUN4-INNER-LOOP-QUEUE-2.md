# RUN4 inner-loop queue — concrete preparation, round 2 (2026-09-10)

Zai-2, follow-up to `RUN4-INNER-LOOP-QUEUE.md` (bf054892) after Codex-17's
review. Note + drafts + one parser-validation test only. No worklist,
registry, frontier or data writes; no live activation; no worker execution
outside WM; no Codex-16 dispatch; no Claude.

## 1. U88 — draft mission + frozen fixture delivered

- Draft mission: `draft-missions/M-u88-contextual-preferences.md` (DRAFT,
  outside discovery roots). Frozen fixture + rubric inside it, extracted from
  `SESSION-IAD-feedback-interpretation-2026-09-10.md`: the two-recipient
  caption-review episode (A author + O owner, revision 2, coverage 2,
  receipt standard `:authorized-inbox`) with the eleven-step event/state
  table.
- Every explicit prerequisite is represented as **data with a typed
  unknown/refusal arm** — membership roster (establisher + per-recipient
  reason), affected-consumer list, authority (fixture-supplied adoption
  warrant), payload view + digest, receipt standard (transport-acceptance is
  named strictly weaker; missing inbox adapter = adapter gap, never a
  relabeling), deadline (required iff timeliness claimed), applicability
  (:established/:not-applicable/:unknown, unknown blocks; no manufactured
  team for solo tasks). No policy is picked where the note leaves a binding
  open.
- Testable rejecting cases per the review: expected inbox receipt vs
  transport acceptance (steps 2–3); one recipient cannot discharge another
  (step 7 vs 3); revision mismatch (step 6) and digest mismatch (step 5);
  duplicate-id idempotence (step 4); missing applicability/authority block
  activation; frozen denominator on disconnect; deadline ⇒ overdue never
  satisfied; unauthorized amendment refused. Per review, the rubric does
  **not** require proving absence of every future rule-revision path.
- Scope exclusions restated in the mission: no selector, masses, endpoints,
  adoption, or live anything; development demonstrates only the proposed
  interpreter with supplied fixture authority.

## 2. Learning candidate — corrected synopsis

Round 1 wrongly conflated the R-A..R-D record vocabulary (from the
memory-transfer technote) with the existing learning design. The actual
design is `runs/LEARNING-design-proposed-2026-09-09.md` (commissioned by
Item 3 of `runs/Item25-model-choice-chain-2026-09-09/README.md`), whose own
variables are:

1. `q_t(s)` state belief per admitted event — gaps: the current filter's
   evidence-dependent exponent kappa(w) must be either recorded as the
   declared powered-likelihood model or replaced by an explicitly versioned
   untempered mode (explicit choice for review); zero-normalizer must become
   a typed model/evidence contradiction (tested, not assumed).
2. `a[o,s]` Dirichlet observation likelihood — gaps: initial concentrations
   declaration (proposal: strength one per column), structural-zero support
   representation (no flooring), posterior-predictive-vs-digamma consumer
   separation, correction/supersession recomputation, successor-A activation
   failure refusing the claimed learning run.
3. `n_E(k)` habit counts — gap: strategic counts collected only; final
   strategic selector keeps fixture E_S until separately authorized.
4. `M` structural revision via BMR — gaps: review-boundary event count with
   run budget; validation of the proposal/hold path without claiming an
   accepted change; no import of capability×mission A4a counts as
   observation/status counts.

Draft task (candidate 2 revised): resolve/document the concrete evidence /
update / schedule / persisted-state / next-consumer gaps **for these
variables**, keeping each unresolved choice explicit for Joe; no adoption, no
implementation ruling. Also corrected: the offline-only question was
**WITHHELD, not ruled** — Joe said he is "not prepared yet to say one way or
another" (`SESSION-model-choices-2026-09-09.md:57-63`); the design must carry
that withholding, not treat it as settled either way.

## 3. Reconciliation candidate — draft mission delivered

`draft-missions/M-f10-u83-blockage-reconciliation.md`: proposals only; frozen
row excerpts to be digest-pinned at worker start (worker verifies digests
before writing; a mutated row stops the task); cited evidence pre-pinned
(C-WIRING-REVIEW for F10's refuted wording plus its two carried-forward real
obligations — observation-model gap, live-run rider; MORNING-BRIEF-reduction
:73 for U83's nonexistent queue).

## 4. F11 exclusion — corrected

Round 1 excluded F11 citing the issue-board design's vocabulary-conflict
finding. That finding is **superseded by current evidence**: the live RUN4
readiness meter (`runs/F2-run4-readiness/READINESS.edn`, emitted by
`run4_readiness.bb`, last commit dc455666 2026-09-09) reads nine GREEN lines
and **VERDICT READY** with `:blocked-on []`; the meter's own
reported-not-gated note covers the hole readiness values. F11 stays out of
this queue only because the board carries no current next-action or
eligibility record for it — not because of the old conflict.

## 5. Parser validation (run, green)

`test/futon2/aif/u88_draft_mission_test.clj`, in the
`futon2.aif.run4-draft-mission-test` convention: both drafts are absent from
production `open-missions`; installed at a disposable canonical-shaped path
(`<temp>/futon2/holes/missions/M-*.md`) each parses with `:status-class
:draft` and is excluded from open work; after the simulated marker flip
(DRAFT → OPEN) only inside the disposable root, each is ordinary open work.
Production is never activated. Results: 2 tests / 13 assertions / 0
failures; the existing run4 draft test still passes (1/11/0); clj-kondo 0/0;
check-parens OK. (Runner form: `clojure -M:test -m cognitect.test-runner -n
<ns>` — this repo's :test alias has no -M main-opts, so `-n` must go to the
runner main.)

## Ownership note

Pending human ownership applies only to **closure/adoption claims** (U88 row
acceptance, adoption of any draft mission, application of proposed row
corrections) — not to this preparation, which Joe's 2026-09-10 direction
already authorizes.
