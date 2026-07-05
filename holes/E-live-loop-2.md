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

> **Reviewer note (claude-16, 2026-07-05 morning), 2a–2c APPROVED with
> one re-pin:** independent gate re-runs all PASS; Flight-1 banner
> control on the modified constructor: rel and size unchanged, **F
> drifted +0.046 → +0.051**. Cause named: `base_rate_prior`'s K is the
> MEDIAN positive co-app mass — registering the seeds' phylogeny edges
> shifts the typical scale, so all priors move slightly. Benign and
> principled; the driver's "byte-identical" claim covered priors under
> a narrower condition than the full pipeline. **Baseline is re-pinned
> at F +0.051** for all future regression checks; lesson for the
> ledger: "identical" claims must name the exact pipeline stage they
> were checked at.

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

## 2f log — the GOLDEN RUN deposit (runner: claude-20, 2026-07-05)

**2f DONE, gate PASS (converted :manual → :cmd).** Arming: Joe's verbatim
word in the step history and in the deposit itself. Scope honored: 2f
only; `*escrow-replay?*` and the live path untouched.

- **Candidate chosen: `autoclock-in`** over state-snapshot-witness/:s4.
  Reasons: (1) the S2 design's reviewer-approved recommendation — its
  A-next EDN is a sealed ground-truth wiring, so the deposit is scoreable
  against an answer key; (2) size-4 cascade = the full shown set, no
  truncation wart; (3) no eponymous-provenance caveat; (4) the
  partial-fit halo genuinely exercises the honesty clauses. **Blinding
  held:** the runner never opened the sealed wiring (only the
  `:provenance {:mission …}` line was grepped); ψ came from the S1 raw
  JSON. Scoring against the seal belongs to the reviewer — author ≠
  scorer.
- **Deposit:** `futon6/data/fold-turns/ft-autoclock-in-001.edn` (first
  file in the escrow). 4 boxes (durable clocked-on write at declared
  intent; degrade-to-RAM + :pending-durable reconcile; lineage-gap audit
  in reconstitute; teardown acceptance probe), 4 wires, terminal :b4,
  **5 policy-holes** (clock-out semantics; held-count join; enforcement
  policy; reconciliation cadence; numeric acceptance targets). Coverage
  4/9 ⇒ **ΔG = −4/9 ≈ −0.4444** (hand-derived = `coverage-delta-g`
  recomputed; hole/box ratio not tuned for a round number). Prompt-sha
  `5597da91…`; prose = verbatim flexiarg slurps, per-pattern shas in the
  record; the 2g caller must inject that prose-fn and the literal
  `{:mission … :psi …}` circumstance.
- **Reject-loudly demonstrated on tampered copies in /tmp** (never the
  real escrow): dropped hole ⇒ `delta-g-mismatch` (stored −4/9 vs
  recomputed −0.5); stripped arming ⇒ `missing-arming`. Both loud, both
  auditable; demo dir removed.
- **Gate 2f is now standing:** `scripts/gate_2f_deposit.clj` (kondo 0/0,
  check-parens OK) — zero rejections + prompt REBUILT from the record's
  own cascade+ψ+prose sha-matches the stored pin + replay returns the
  recorded answer. Full board re-run after: S1,2a–2f all PASS, no
  regressions.

**PAR (claude-20):** What worked: (1) reading the seam/loader *tests*
before authoring — they fixed the circumstance shape and prose-fn
question (pin 1 binds prose; the deposit must record the reproducible
prose rule or 2g can never replay it); (2) computing every recorded
number in-process via the real fns (nothing hand-transcribed), with the
hand derivation shown alongside — pin 3 then proves the arithmetic
instead of trusting it; (3) tamper demo in a /tmp copy — the real escrow
never held an invalid file. What to carry: the honest-fit tension is the
exemplar's point — a 0.36–0.42-rel halo forced real holes; five surfaced
holes and ΔG −4/9 is a *truthful* golden turn, where tuning to a rounder
−0.5 would have been the decorative-box failure the prompt warns
against. Open for the operator/reviewer: score the construction against
the sealed autoclock-in wiring; rule on 2g.

## Rules

1. Actions only when `runnable` says so (all prerequisite gates green).
2. Gates re-run freely; a green→red transition is a regression alarm —
   ledger entry before proceeding.
3. ⚠ARMED steps additionally need Joe's word, per-step, recorded in the
   step history.
4. Every action under the usual code gates (clj-kondo, check-parens,
   K2 assertions) and the mistakes-ledger gate from E-live-loop-1.

### 2f reviewer scoring (claude-16, blind comparison against the sealed EMPIRICAL + the 2026-07-01 SCORE.md baseline)

**Verdict: GOLDEN — approved as the reference exemplar.** Independent
gate re-run PASS. Scored against the seal claude-20 never opened:

- **Read-side codomain: RECOVERED** — b4's acceptance probe is
  reconstitution-surviving-teardown, the mission's actual point, which
  the 2026-07-01 cascade-only baseline *missed entirely*. The delta is
  S1's doing: sorry-grain ψ carries the want endpoint verbatim.
- **Endpoint grounding: STRONG** — b1 grounds on the real substrate
  (set-dispatch-mission!, clock/clocked-on, canonical nodes), vs the
  baseline's ~50% with canonical nodes missed. Again ψ-carried.
- **The sealed hard holes: MISSED, diagnostically.** Order-independent
  retraction (the Gu repair), canonical-identity, repl-sync — absent
  from boxes and holes alike. Two are code-level facts the seal itself
  says no prose can carry. But **single-active was in the empirical
  record's `:hungry-for` gloss and the mechanical ψ recipe DROPPED it**
  — a recipe defect, now named: *ψ recipes must carry the codomain
  qualifiers (`:hungry-for`), not just the signature.* Falsifiable
  prediction for deposit 002: with `:hungry-for` in ψ, the single-active
  invariant surfaces as a box or a hole.
- **b2 (degrade/reconcile): an invention** — engineering-sound, absent
  from the seal. Legitimate under the fold contract (a construction
  proposal, not a history recitation); flagged so 2g's rollout scoring
  treats it as the proposal it is.
- **The five surfaced holes are all real underdeterminations**; none is
  decorative. The honesty clauses held throughout; the deposit proves
  its own tamper-rejection; ΔG −4/9 re-derives by hand.

**Yield beyond the exemplar:** the golden run measurably demonstrates
the S1→2f improvement chain (reconstitution + endpoints recovered vs
the pre-S1 baseline) and produces the next recipe upgrade as a testable
claim. That is what a golden run is for.
