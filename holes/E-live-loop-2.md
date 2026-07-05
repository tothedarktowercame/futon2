# E-live-loop-2 — the steppable continuation (microsteps + gates)

**Status: OPEN (2026-07-05 morning, operator-directed). Driver:
claude-19 (continuing from E-live-loop-1). Reviewer: claude-16.
Operator gate: Joe — steps marked ⚠ARMED run only on his word.**

## Why steppable (the operator's design, 2026-07-05 07:35)

Rather than running in 3 big steps, run in MICROSTEPS: each has an
action, a machine-checkable GATE, and recorded evidence. A later step
may only be (re)run when ALL its prerequisite gates are green. Gates
are re-runnable at any time — they double as standing evidence checks
(mistakes-ledger discipline): a gate that was green and goes red is a
regression alarm, not an embarrassment.

**Apparatus:** step state lives in `e-live-loop-2-steps.edn` (same dir);
the stepper is `futon2/scripts/live_loop_step.py`:

    python3 futon2/scripts/live_loop_step.py status          # scoreboard
    python3 futon2/scripts/live_loop_step.py gate <id>       # run one gate
    python3 futon2/scripts/live_loop_step.py gates           # run all defined gates
    python3 futon2/scripts/live_loop_step.py runnable <id>   # may I perform this step's ACTION?

The stepper never performs actions (agents do, under the usual gates);
it enforces ordering, runs gate commands, and records pass/fail history
with timestamps. A step whose gate command is still `:manual` shows as
undefined (red) — honest by default.

## Microsteps

Prerequisite (imported from E-live-loop-1, already green):
- **S1** — sorry-grain measurement delivered + reviewed + ablation held.
  Gate: S1 table exists with ablation addendum.

### 2-series — the constructive path (decomposed S2)

- **2a — seed embeddings.** Regenerate the MiniLM pattern-embeddings
  artifact so the three process-coherence patterns are present.
  Gate: all three pattern ids appear in
  `futon3a/resources/notions/minilm_pattern_embeddings.json`.
- **2b — phylogeny registration.** Register the seeds in
  `futon6/data/pattern-phylogeny-edges.json` (the eligibility gate at
  `cascade_construct.py:188-197` makes unregistered patterns
  display-only). Gate: all three ids in the phylogeny's patterns set.
- **2c — seed prior treatment.** Implement the chosen mechanism (prior
  floor / anchor slot per the S2 design recommendation) so seeds are not
  Occam-fined at the 2.398-nat unseen default. Gate: probe run shows a
  seed pattern retrievable with per-seed complexity below 2.0 nats
  (driver attaches the probe as the gate command).
- **2d — escrow record shape.** Land the `ft-*.edn` fold-turn record
  contract (authored tier of the sortie-11 triple schema) with
  reject-loudly loading. Gate: a deliberately malformed record is
  REJECTED loudly by the loader test.
- **2e — replay seam, built dark.** The `close_loop.clj` seam that
  consults escrowed fold-turns (exact prompt-sha match, stored ΔG
  re-asserted on load), dark behind a flag, default off. Gate: seam
  unit test replays a MOCK fold-turn and re-asserts ΔG; live path
  behavior unchanged with flag off (K2 assertions both sides).
- **2f ⚠ARMED — one real deposit.** One LLM-fold turn by an agent, on
  one S1 cascade (candidates: state-snapshot-witness/:s4 or
  autoclock-in), recorded to `futon6/data/fold-turns/` with Joe's
  verbatim arming word as a field. Gate: deposit loads cleanly;
  sha-match + ΔG re-assertion pass.
- **2g ⚠ARMED — live replay.** Flag on; the act-gate consumes the
  escrowed ΔG on the live path. Gate: a trace record shows an act-gate
  verdict whose ΔG leg came from the escrow (provenance field), and
  the mistakes-ledger §10 evidence slot gets its entry.

### 3-series — futility feedback (decomposed S3; needs ALL 2-series green)

- **3a — futility observable.** The per-lane 0-for-N counter emitted
  where agents and the operator can read it (ties to the wm-tick
  emitter upgrade, ledger §13). Gate: counter queryable via an agent
  tool; shows the true historical count.
- **3b — γ update rule.** The precision-update rule specified and
  SIMULATED against the 674-tick census (no live wiring). Gate: the
  simulation note shows γ trajectory under the historical record and
  under a synthetic paying-lane record — they must diverge sensibly.
- **3c — operator bulletin.** Lane-futility alerts wired to the
  operator-lane at nag level past threshold (stuck-means-signal at the
  WM grain, ledger §11's open half). Gate: a synthetic 0-for-N burst
  produces a bulletin entry in dry-run.

## Rules

1. Actions only when `runnable` says so (all prerequisite gates green).
2. Gates re-run freely; a green→red transition is a regression alarm —
   ledger entry before proceeding.
3. ⚠ARMED steps additionally need Joe's word, per-step, recorded in the
   step history.
4. Every action under the usual code gates (clj-kondo, check-parens,
   K2 assertions) and the mistakes-ledger gate from E-live-loop-1.
