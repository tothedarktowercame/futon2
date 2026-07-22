# Z3 preregistration — zaif controller live evaluation (ZU-3)

**Status: DRAFT — awaiting Joe's review. Nothing below is active.**
Activation is a separate append-only operation (§Activation) so this
protocol can be reviewed, amended, and frozen before the clock starts.
Author: claude-2, 2026-07-22. Home: M-zaif-harness (futon2/holes/).
Machine-readable commitment: [z3-prereg.edn](z3-prereg.edn).

## Ground facts this design must respect

Surveyed 2026-07-22 from source, git, and the live store — each shaped
the design; none is an assumption.

1. **Decisions are recorded, not actuated.** The `:zaif` profile calls
   `decide()` per round and persists the arm choice, but the runner
   proceeds identically whatever the arm (`zai_api.clj:848`, the
   `_decision` binding). A naive zaif-on/zaif-off A/B today would
   compare two behaviorally identical conditions.
2. **Live controller inputs are empty.** `default-zaif-inputs`
   (`zai_api.clj:806`) reads ctx keys the runner never populates, so
   as wired, every live `decide()` sees empty beliefs and an unhydrated
   γ, and degenerates to `:act` by tie-break. The runner already
   accepts a `:zaif-inputs-fn` hook; hydration is a named prerequisite
   (§Dependencies), not an afterthought.
3. **ZU-2's replay finding** (checkpoint 2026-07-13): at the as-shipped
   `operator-attention-cost` 0.65 the `:ask` arm is structurally
   unreachable (100% of correction sessions scored MISS); the logged
   sweep separates arms cleanly at 0.15; constants were left as-shipped
   — no silent tuning. Caveat the replay could not remove: its inputs
   were reconstructed *from the outcome labels* (correction sessions
   were assigned high c-uncertainty), which partly bakes in the answer.
   Live inputs are the fix, and this preregistration is where that
   comparison becomes fair.
4. **The declared-marks channel is live and accepted** (B0 corrected
   record, 2026-07-22; first live ✓ = `e-51c9349e`, queryable via
   `tag=`). Operator *marking recall* — how often a correction-worthy
   turn actually gets a ✘ — is unmeasured; §Marking-recall audit bounds
   it rather than assuming it.
5. **The runner pool is empty** (reaped 2026-07-13); minting runners is
   an operator/terminal op. Cohort accrual therefore rides Joe's
   natural working sessions, not a scheduler.

## Design: two phases, separately gated

### Z3a — shadow paired comparison (no actuation; primary subject: the constants)

At each round of a live `:zaif`-profile session, the harness computes
`decide()` **twice from the same hydrated inputs** — once with
`operator-attention-cost` 0.65 (as-shipped), once with 0.15 (the sweep
value) — and records both decisions with their full G terms and inputs.
`decide()` is pure, so this is a within-round paired design: every round
where the two constants disagree is one informative observation, and no
randomization, blinding, or behavioral change is involved. The model
never sees any of it (the no-in-band-formalism commitment); the operator
experiences an unchanged harness.

**Realized operator judgment per divergent round** is read
mechanically from the store: the next operator chat-turn in the session
after the round's turn. `:correction` if it carries the `correction` tag
(declared ✘) or a recorded adjudication verdict of correction;
`:acceptance` if it carries the `approval` tag or carries no correction
signal (interpretation bounded by §Marking-recall audit). Ideal-arm
mapping follows ZU-2 verbatim: correction → `:ask`/`:yield` was ideal;
acceptance → `:act` was ideal.

**Primary endpoint (Z3a):** on divergent rounds, paired accuracy of the
0.15 constant vs the 0.65 constant against realized judgment.
Preregistered direction: 0.15 scores higher (from ZU-2). Test: exact
sign test on pairs, plus a permutation null (constant labels shuffled
within rounds); α = .05, one comparison, no interim looks.

**Secondary (reported regardless):** divergence rate (fraction of rounds
where the constant matters at all); input-coverage census (which of
γ / c-belief / posting-stats were non-degenerate per round); arm
distribution per constant; per-mission γ values actually used.

**Degeneracy is a result, not a failure:** if the divergence rate is
< 5% of rounds, the constants question is moot at current input
geometry, the finding is "inputs, not constants, are binding," and
neither constant is adopted. This is the honest analogue of PZ1's
lexicon disqualification.

### Z3b — actuated A/B (gated; primary subject: does hedging help)

Runs only after (i) the Z3a report exists, (ii) arm actuation is built
and reviewed (§Dependencies D-2), and (iii) Joe arms it. Sessions
alternate by mint order between **actuated** (the chosen arm steers the
round: `:ask` puts a question to the operator, `:yield` returns the
turn, `:retrieve` runs the memory seam before the model) and **shadow**
(decisions recorded only), at the single constant licensed by Z3a.
Assignment is written to the store *before* the session's first round
and is not disclosed to the operator during the session.

**Primary endpoint (Z3b):** corrections per act — operator
correction-signals following `:act` rounds, over `:act` rounds.
**Guard endpoint:** asks per achieved outcome, where an achieved outcome
is a ✓-approval-tagged operator turn or a turn-commits event attributed
to the session; the actuated arm's asks-per-outcome must not exceed 2×
the shadow arm's (hedging is not free; the attention cost is the point).
**Descriptive:** yield timing (round index of `:yield` choices relative
to session length and to the first correction, reported, not tested).

## Cohort, sample size, stopping

- **Z3a cohort:** the first **20** `:zaif`-profile sessions with ≥ 3
  operator turns each, in natural wall-clock order after the activation
  marker. No replacements; a session that yields zero divergent rounds
  still counts (that is the divergence-rate datum). Analysis runs once,
  after session 20 or 30 calendar days, whichever comes first.
- **Z3b cohort:** minimum **10 sessions per arm**, same accrual rules,
  same single-analysis discipline. Sized to ZU-2's N≥10 replay standard;
  this is a small-N paired-guarded design, and confidence intervals will
  be reported as such — the decision rule below does not overclaim.
- **Exclusions, recorded at occurrence time, never at analysis time:**
  infrastructure failure mid-session (JVM restart, store append
  failure); sessions with < 3 operator turns (excluded from the
  denominator but logged). The operator may void a session for cause by
  writing the reason to the store during or immediately after that
  session — never after seeing any analysis.

## Marking-recall audit

Once during the Z3a window: a ~10-item mini gold pass (PZ1 H3 blind
protocol — two independent labelers over sampled operator turns from the
cohort, disagreements to Joe) bounds the recall of declared ✘ marking
against the ~24% correction base rate. The "no correction signal =
acceptance" reading in both phases is reported with this bound attached.

## Analysis provenance

The scoring scripts (`z3a_score.clj`, later `z3b_score.clj`) are
committed **before activation**, read only the store, and re-derive
every recorded arm choice from the recorded inputs as a determinism
check (the B1 `--check` discipline). All queries use `tag=` (the
`tags=` parameter on `/api/alpha/evidence` is silently ignored —
verified 2026-07-22). Results are reported in full, including dry,
degenerate, and negative outcomes, with a doc checkpoint in the same
commit as the analysis (ZU-2's doc discipline).

## Decision rule — what this preregistration licenses

1. Divergence rate < 5% → constants comparison moot; finding = input
   geometry; next work is inputs, not constants. No adoption.
2. Z3a positive for 0.15 (preregistered direction, α = .05) → 0.15
   becomes the Z3b candidate constant. **Still not adopted in code.**
3. Only a completed Z3b — primary improved, guard respected — licenses
   changing the shipped constant, in a commit that carries this doc,
   both reports, and the diff together.
4. Any other outcome: constants stay as-shipped, the reports stand as
   evidence, and the next slice is chartered from what they show.

## Dependencies (named build items, none activated by this doc)

- **D-1 (Z3a prerequisite): input hydration** — a `:zaif-inputs-fn`
  that supplies γ from the Z1 `gamma-events` view / B1 fold output,
  c-belief from per-mission correction rates (marks + PZ1 final truth),
  and `posting-stats` from the D1 sidecar. Small, pure-testable, no
  behavioral change to the runner. Plus the dual-constant recording
  change (compute/persist both constants' decisions per round).
- **D-2 (Z3b prerequisite): arm actuation** — `:ask`/`:yield`/
  `:retrieve` steer the round. Reviewed separately; not designed here.
- **D-3: runner minting** — operator/terminal op (pool currently empty).

## Activation and amendments

Joe activates each phase by writing an append-only activation marker to
the store (`tag=zaif-z3a-activation`, later `zaif-z3b-activation`); the
cohort clock starts at that marker. This file is frozen at Z3a
activation; any later change goes in an Amendments section below with
its own date and reason, and anything amended after activation is
reported as amended.
