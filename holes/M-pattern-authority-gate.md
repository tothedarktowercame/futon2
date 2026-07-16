# M-pattern-authority-gate — do the ant's design patterns do anything at all?

**Status:** SPEC, 2026-07-16. Owner: claude-2 (review + architecture). Build: belled to codex-2.
**Motive (Joe):** *"we could then compare 'ants with naive stupid policies' with 'ants with
enlightened policies' and see which performs better. Not that I'm biased."*

**This is an AUTHORITY GATE, not the experiment.** It answers one question: **do the five
design patterns move the ant?** Nothing about propagators, exotypes, or selection happens
until this passes. This is the gate `cyber.clj` never had, applied to the pattern layer.

---

## 0. The finding this exists to act on

The ant has **five design patterns** (`src/ants/aif/pattern_sense.clj:15`) —
`:cyber/baseline`, `:cyber/hunger-coupling`, `:cyber/cargo-return`,
`:cyber/pheromone-tuner`, `:cyber/white-space` — each with IF/HOWEVER/THEN/BECAUSE shape in
`pattern_efe.clj`, wired into live EFE at `policy.clj:611`.

**They have never fired.** Two independent off-switches, both off by default:

| # | switch | where | state |
|---|---|---|---|
| 1 | `ant → :cyber-pattern → :id` | `core.clj:137`, `pattern_sense.clj:109` | **only ever written by `cyber.clj:210,242`** — the deprecated cyberants. Nothing in `src/ants/aif/`, `xeno_loop.clj`, or `ant_authority_gate.clj` sets it. |
| 2 | `config → :efe → :lambda → :pattern` | `pattern_efe.clj:178` | **defaults to `0.0`**; when zero, `pattern-efe` returns `{:G 0.0}` before computing anything. |

> **Every xeno-loop and authority-gate run to date ran with all five patterns off.** The
> policy space was four macro actions (`policy.clj:12`) and nothing else.

### §0 amendment owed to `README-xeno-loop.md`

§0 says *"If you find yourself wiring something into `:cyber-pattern`, stop."* It also says,
accurately, *"only `:id` and `:ticks-active` are consumed anywhere."* Both true — and
together they mean **`:cyber-pattern :id` is the ignition switch for the design patterns.**
§0 is right about `:config` (`:policy-priors`, `:pattern-sense`, `:adapt-config` are stored
and never read — genuinely inert) and **wrong about `:id`** (live, load-bearing). Amend it,
or the next reader stops at the switch. **Do the amendment as part of this slice.**

## 1. The arms

One pattern is active at a time (`:pattern/active` is singular — no composition yet).

| arm | `:cyber-pattern :id` | `lambda.pattern` | role |
|---|---|---|---|
| `off` | nil | 0.0 | **baseline** — the ant as every previous run had it |
| `sham` | `:cyber/baseline` | > 0 | **THE LOAD-BEARING ARM.** Machinery fully live; `pattern-action-risk` and `pattern-info-gain` both return `0.0` (`pattern_efe.clj:118,170`), so `G = λ·(0−0) = 0`. **Must tie `off` EXACTLY, per seed.** |
| `cargo-return` | `:cyber/cargo-return` | > 0 | risk **and** info-gain |
| `white-space` | `:cyber/white-space` | > 0 | risk **and** info-gain |
| `hunger-coupling` | `:cyber/hunger-coupling` | > 0 | **risk only** — falls through to `0.0` in `pattern-info-gain` |
| `pheromone-tuner` | `:cyber/pheromone-tuner` | > 0 | **risk only** |

**If `sham` does not tie `off` exactly on every seed, STOP and fix the harness.** A
non-zero difference under a provably-zero contribution means the instrument is broken, and
every other number in the run is uninterpretable. This is precisely what made the C-vector
gate trustworthy (`identity` returned exactly 0.000 across 20 paired seeds).

## 2. `lambda.pattern` is a free parameter — do not hide it

The effect size is proportional to `λ_pattern`. There is no principled value. So:

- **Sweep it**: `λ_pattern ∈ {0.1, 0.5, 1.0}` at minimum, and report all cells.
- **Never report a single λ as "the" result.** If a pattern only helps at one λ, say so.
- `λ_pattern = 0` must reproduce `off` bit-exactly — a second, cheaper sham.

## 3. Protocol — reuse the apparatus, do not reinvent it

`scripts/ant_authority_gate.clj` already solves the hard parts. Reuse:

- **Board screening on GEOMETRY** (`distance(home → nearest food) ≤ 8`), never on outcome —
  screening on baseline yield is biased (`README-xeno-loop.md` §5).
- **Paired seeds**, sign test, held-out boards never selected against.
- Scale: ≥20 paired seeds × 300 ticks, patchy 30×30, matching the C gate.

**Preregister the verdict before running.** A pattern is a **live actuator** iff it differs
from `off` by sign test (p<.05) on held-out boards. Note *differs*, not *beats* — a pattern
that reliably makes foraging **worse** is still a live actuator, and that is a PASS for this
gate. Authority is not merit.

## 4. Guard on the bias (Joe flagged it himself)

Joe expects "enlightened" to win. Three reasons that may not happen, all of which are
findings rather than failures — write them into the report:

1. **The patterns were written for the CYBER ants, not the AIF ants.** They may be
   miscalibrated for a different action-selection path. Their thresholds
   (`cargo < 0.15`, etc.) were tuned against a system that no longer exists.
2. **Two of four are pure penalties.** `hunger-coupling` and `pheromone-tuner` have risk but
   **no** info-gain. A pure penalty can only *restrict* the action set — it cannot discover
   anything. "Enlightened" for those two means "more constrained", which may cost yield.
3. **Yield is not the patterns' objective.** `cargo-return-discipline` penalises *thrashing*.
   An ant that thrashes less may forage less. If yield drops, check whether the pattern did
   what it says on the tin before calling it a failure.

## 5. Acceptance

- `sham` ties `off` **exactly**, every seed, every λ. (If not: STOP.)
- `λ_pattern = 0` reproduces `off` bit-exactly.
- Each of the four real patterns gets a per-λ sign test vs `off` on held-out boards.
- The `README-xeno-loop.md` §0 amendment lands (`:id` live, `:config` inert).
- Report states, per arm: Δ yield, wins/n, sign-p — and, for any pattern that moved the ant,
  **what it actually did to behaviour**, not just the scalar.

## 6. Explicitly OUT of scope

Propagators. Exotypes. Selection. Evolution. Composing patterns. Making G range over
patterns rather than actions. Reifying patterns as data.

Those are the follow-on and they all depend on this gate passing. If the patterns do not
move the ant, there is nothing to propagate and the whole line stops here — which would
itself be worth knowing, cheaply, today.
