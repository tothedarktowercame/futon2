# Mission: M-arguing-worlds

*Run the falsifiable test on the conceptual core of FutonZero: does **arguing across
competing possible-world buildouts** beat simply taking the single best buildout?
If yes, the endogenous engine of the learning loop is dialectic, not self-play.
If no, record it and stand the frame down — "it's ceremony."*

**Date:** 2026-06-10 — **Phase: HEAD ✓ → IDENTIFY** (drafted by Fable from the
2026-06-10 planning session; PM handoff → Opus pool, build → codex pool).
**Principal:** Joe.
**Repos:** futon2 (rollout, EFE machinery, this file), futon3a (cascade constructor,
notions index), futon3c (E-possible-world-regulator), futon7 (grounded yardstick via
M-peradam-grounding).
**Cross-ref:** `futon2/docs/futonzero-alphazero.md` §1 (the operator note this HEAD
preserves) + §5 (semilattice-vs-rollout, kill-criterion discipline),
`futon3c/holes/E-possible-world-regulator.md` (the referee shape),
`futon2/holes/M-wm-policies.md` §"The policy framework" (cascade construction,
real-`C` findings), [[M-peradam-grounding]] (the yardstick), judge-panel patterns
(`futon3/library/`).

---

## HEAD

### Operator-voice anchor

Preserved verbatim from `futonzero-alphazero.md` §1 (Joe, 2026-06-09, flagged there
as "open, early"):

> **…or maybe self-play is the wrong frame entirely.** The peradam may be
> *exogenous* (like AlphaZero's Lee Sedol match — the proof, not the trainer), and
> the endogenous engine may not be a formal adversary at all but
> **argument-across-possible-worlds**: competing *pattern-theoretic buildouts* of
> the same circumstance, the more-*whole* one winning. This would **out-bootstrap**
> AlphaZero — they dropped the *game history* (no human games), we drop the *game
> board* (no perfect simulator): the cascade's `C`-score *evaluates* a possible
> world directly, so **the patterns (good-enough invariants) replace the simulated
> playout**. […] **Falsifiable test:** does arguing across buildouts beat the
> single best buildout? If not, it's ceremony. This is plausibly the conceptual
> core that makes FutonZero *Futon*-Zero rather than a port.

### What's already felt to be true

The machinery exists piecewise and was not built for this purpose — which is the
encouraging part. The constructor can produce a buildout per `|ψ⟩`; the multi-agent
dialectic (gradient *vs* rollout, disagreement-as-signal) has already paid rent
twice this week (R1 re-flatten catch; the false-knee redirect); the regulator
exists as a referee shape; judge-panel patterns exist as the tournament shape.
The dialectic *feels* like how the stack already works on its best days — this
mission asks whether that feeling survives measurement.

### Anti-glibness discipline

Two ways this goes superficial, both named now:

1. **The tournament grades itself.** If the winner is picked by `C` and the
   comparison is scored by `C`, the test is circular by construction — the
   laundering trap one level up. The yardstick must be *outside* the
   wholeness-scoring loop (grounded outcomes; see IDENTIFY).
2. **Fake diversity.** N reruns of one constructor with jittered seeds are not
   competing possible worlds. If the buildouts don't *genuinely disagree* (different
   pattern sets, different implied moves), the argument step has nothing to argue
   about, and a null result means "the generator was monotone," not "dialectic is
   ceremony." Diversity is a precondition to *check*, not assume.

### Working-economy position

This mission underwrites the *name*: whether FutonZero is a port of AlphaZero or a
genuinely different bootstrap. It is underwritten by M-peradam-grounding (yardstick)
and the cascade lane (generator). It deliberately does NOT block the v2 build
(R2/CH1, scope-grain) — those proceed on the self-play-shaped track while this
tests the alternative frame; the loser of the two frames informs, not vetoes, the
winner.

### Clarity-gap / carried-forward tensions

- What *is* a possible world operationally — a cascade alone, a cascade + its
  implied move-set, or a full buildout with predicted state? (IDENTIFY narrows;
  DERIVE decides.)
- What is the argument step — judge panel, adversarial refutation between buildouts,
  or the gradient-vs-rollout disagreement generalised?
- How many worlds, and where does generation diversity come from (lens variation,
  ψ-decomposition, agent diversity)?
- Relation to the *other* v2 adversary candidate (Pudding Prover refuting peradam
  claims): sibling lane, possibly the same machine seen from the refutation side.
  Noted, not built here.

### Provenance

Operator note in `futonzero-alphazero.md` (2026-06-09), lifted into mission form in
the 2026-06-10 Fable planning session; the falsifiable-test sentence is Joe's own.

---

## 1. IDENTIFY

### Motivation

`futonzero-alphazero.md` §1 stakes a claim ("plausibly the conceptual core") and
supplies its own kill criterion, but no apparatus runs the test. Meanwhile v1's two
machineries for assembling multi-step value — the linear rollout and the
semilattice cascade — remain unreconciled (§5), and the dialectic frame, if it
survives the test, is the natural reconciler: worlds are evaluated whole (the
semilattice's native grain), and argument replaces playout (the rollout's job).
If it fails the test, the GFlowNet/linearization question goes back to the rollout
lane on its own merits. Either result clarifies the architecture; only running the
test gets the result.

### Plain-language gap statement

When the system faces a situation, it currently builds one best argument for what
to do. The open question: is it measurably better to build several genuinely
different arguments and make them fight? Nobody knows; the test is cheap relative
to the architectural weight riding on the answer.

### Theoretical anchoring

- **Wholeness evaluates worlds directly** (Alexander/Salingaros via the cascade
  lane): the claim under test, not an assumption of the test.
- **Disagreement-as-signal** (combining-methods-as-diagnostic, already validated
  live): the dialectic is this discipline promoted from diagnostic to engine.
- **Regulator lesson** (E-possible-world-regulator): no fabricated dynamics — a
  "world" may only contain terrain that exists or is typed `:conjectural`.
- **Kill-criterion discipline** (`futonzero-alphazero.md` §5): the mission ships
  with its own falsifier; a recorded negative is a successful completion.

### Scope in

- **Generation:** N genuinely-diverse buildouts over the same real `|ψ⟩`
  (lens-varied construction at minimum; document the diversity check).
- **The argument step:** a referee protocol producing a winner *and a legible
  argument-record* — the dialectic must be inspectable, not an opaque max().
- **The comparison:** argument-winner vs top-1-`C` buildout, both *executed or
  evaluated against a yardstick outside the wholeness loop* — realized outcomes
  (peradam-attributed discharges from M-peradam-grounding; realized `G(π)` on
  executed moves as the floor), over enough trials to read.
- **The decision:** adopt the dialectic as the endogenous engine, or stand it
  down — recorded either way (a prover-observed choice).

### Scope out

- Replacing the cascade lane or the rollout (whatever the result, both v1 lanes
  stand until their own gates say otherwise — `:O-rollout-kill-criterion`,
  `:O-semilattice-rollout-reconciliation`).
- Building the Pudding-Prover-as-refuter loop (sibling lane, M-peradam-grounding's
  scope-out note).
- Closing R2/CH1 work — proceeds in parallel on its own track.
- Any live writes during tournament evaluation (MUST-B holds: worlds are evaluated
  on copies; only a consented winner's first move ever lands).

### Completion criteria

1. ≥3 buildouts over one real `|ψ⟩` that pass an explicit diversity check
   (pattern-set overlap below a stated bound; disagreeing implied moves) — or a
   recorded finding that the generator cannot yet produce diversity (a legitimate
   blocking discovery that routes back to generation, not to ceremony).
2. A referee protocol with a legible argument-record (who beat whom on what
   grounds), runnable by an agent other than the one that generated the worlds
   (author ≠ judge).
3. The comparison run on a grounded yardstick, N≥ a pre-registered trial count,
   result recorded **whichever way it goes** — including the honest middle
   ("dialectic helps on broad `|ψ⟩`, not on focused ones" is an admissible,
   useful answer).
4. The frame decision recorded with rationale; `futonzero-alphazero.md` §1 updated
   from "open, early" to the measured status.

### Relationship to other missions

| Mission | Relationship |
|---|---|
| M-peradam-grounding | Upstream: supplies the non-circular yardstick; without it, only the realized-`G(π)` floor is available and the result carries a self-grading caveat |
| M-pattern-posteriors | Sibling: posterior-informed vs relevance-only construction is a natural source of genuine buildout diversity |
| M-wm-policies | Host: the cascade lane and rollout this mission compares against and may reconcile |
| M-differentiable-substrate | Parallel track: R2 self-play-shaped learning proceeds regardless; the surviving frame informs v3 |

### Source material

| Source | Role |
|---|---|
| futon3a/holes/labs/M-memes-arrows/cascade_construct.py | buildout generator |
| futon3c/holes/E-possible-world-regulator.md | referee shape + no-fabricated-dynamics rule |
| futon3/library/ judge-panel + mission-coherence patterns | tournament/argument shapes |
| futon2.aif.rollout + e-rollout witnesses | the linear comparator + executed-`G(π)` floor |
| futon2/docs/futonzero-alphazero.md §1, §5 | the claim, the caveats, the kill discipline |

---

## v0 result — built + reviewed PASS (codex-1 build, claude-1 PM-review, 2026-06-10)

**Worktree:** `/home/joe/code/futon2-arguing-worlds`, branch `codex/arguing-worlds-v0` @ `2eca617`
(worktree-isolation — no main disruption). Artifacts: `src/futon2/aif/arguing_worlds.clj`
(lens-varied buildout generator over copied rollout states; Jaccard+disagreement diversity precheck;
realized-`G(π)` referee; `:escrowed` peradam-yardstick seam) + `test/…/arguing_worlds_test.clj`.

**Review (claude-1, real gate — re-verified in the worktree):** both anti-glibness gates HOLD —
(1) **yardstick outside C**: the referee scores by realized rollout `G(π)` (`rollout/score-policies` →
`:realized-G`, sorted on it); `C` is *only* the single-buildout baseline (docstring + code confirm
`score-buildout-c` is "Internal wholeness proxy … Not the referee yardstick"). No circular grade.
(2) **diversity-precheck-first**: `jaccard` pattern-overlap + implied-move disagreement → `:monotone-generator`
*before* judging. Build verifies: arguing-worlds-test **5 tests / 17 assertions, 0 failures** (re-ran);
clj-kondo 0/0; check-parens OK. (Full-suite's lone red is the pre-existing `sorry-registry`
`:wm-aif-substrate-addressability` miss — unrelated, diagnosed earlier this session.)

**The honest v0 finding — the campaign's first clean falsifiable negative (Completion #1–#3, on the
realized-`G(π)` floor):** on a **genuinely-diverse** generator (4 lenses, max-overlap **0.11**, disagree=true)
the dialectic does **not** beat the single best buildout → `:single-best-holds` (winner `:fast-terminal`).
The non-diverse frontier (`:root-seeded-focused`, max-overlap 1.0) correctly returns `:monotone-generator`,
winner nil. So **on the realized-`G(π)` floor, arguing-across-worlds is ceremony** — recorded honestly, and
it is *not* an artifact: not a circular `C`-grade (yardstick is `G(π)`), not a monotone-generator effect
(the diverse case was diverse). Completion #4 (`futonzero-alphazero.md` §1 status update) is the open
follow-on.

**Caveat (verdict not permanent):** the negative is on the realized-`G(π)` *floor*; the **grounded peradam
yardstick** (escrowed, [[M-peradam-grounding]]) could flip it — codex-1 correctly escrowed that seam. v0
negative; revisit under the grounded yardstick. **Interlock:** with [[M-pattern-posteriors]] (sparse-data
near-null, needs grounded evidence to move), **both cascade-lane v0 results converge on M-peradam-grounding
as the unlock** — the campaign spine, confirmed independently from two lanes. Mergeable on operator/coordinator call.
