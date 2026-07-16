# M-patchboard-viz — see the airship before deciding it can't fly

**Status:** SPEC, 2026-07-16. Owner: claude-2 (review + architecture). Build: belled to codex-1.
**Motive (Joe, 2026-07-16):** *"Currently we have been flying blind with regard to this
crucial middle layer (i.e. the entire wiring) and deeming the results poor. Well, we don't
know. It would be like saying 'this airship will never take off' — but never actually
seeing the airship."* And: *"The key thing I think is understanding just what terminals are
being set up. The ants, ultimately, are just going to be a patch-board like a synth. The
previous run is probably just some crunchy chord and not a composition."*

**Why now:** `F-what-the-propagator-actually-does.md` (futon5) gives us, for the first
time, a model of the middle layer that is *readable off a diagram*. The viz renders that
model. Without it the patch board would just be 14 knobs.

---

## 0. The model the middle pane renders

The propagator is a **constraint system**: the operator `bit[f(k)] := ¬bit[k]` asserts
`g[f(k)] = ¬g[k]`. Solving it (not simulating it) gives three parts:

| part | definition | meaning | Figure 8 |
|---|---|---|---|
| **FREE** | jacks never written (not in image of `f`) | pinned by blending/boundary — **the scaffold** | `{7}` |
| **CHAIN** | satisfiable constraints | determined by propagation from FREE — **the structure** | `6←5←4←3←2←1` |
| **UNSAT** | `f(k)=k` ⇒ `g[k]=¬g[k]` | can never settle — **the motion** | `{0}` |

`futon5/scripts/propagator_constraint_model.py` derives Figure 8's `{42,170}` from this
analytically, no simulation.

**The one fact the UI exists to make unmissable:**

> **A permutation is onto, so it has NO free jacks — all 40,320 of them. No scaffold, ever.**
> The ant port used permutations. Identity is 8/8 UNSAT = pure noise, which is exactly why
> it was the worst arm (yield 8.6). See `F-propagator-on-c-vector-NEGATIVE.md`.

So the board **MUST** allow **non-injective** patches. A UI that only offers permutations
reproduces the bug that made the ants fail. This is requirement #1, not a nicety.

## 1. The terminals — what is actually on each board

### MetaCA board: 8 jacks

Legacy `truth-table-3` order (`futon5/vendor/metaca/256ca-2014-12-29-BUGGY.el:97`):

| jack | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 |
|---|---|---|---|---|---|---|---|---|
| condition | `000` | `001` | `010` | `100` | `011` | `101` | `110` | `111` |

Jack *k* holds the rule's **response bit** to neighbourhood *k*. The byte is both the data
and the rule (it is applied to its own bit-planes — see F-doc §2).

### Ant board: 14 jacks

The 14-channel observation ABI (`futon2/src/ants/aif/policy.clj:102`). Jack *k* holds the
**preference** (mean/sd) for observation *k*:

| jack | channel | `:outbound` | `:homebound` |
|---:|---|---|---|
| 0 | `:food` | 0.55 | **nil** |
| 1 | `:pher` | 0.35 | 0.30 |
| 2 | `:food-trace` | **nil** | **nil** |
| 3 | `:pher-trace` | **nil** | **nil** |
| 4 | `:home-prox` | 0.20 | 0.70 |
| 5 | `:enemy-prox` | 0.10 | 0.10 |
| 6 | `:h` | 0.40 | 0.40 |
| 7 | `:ingest` | 0.60 | 0.65 |
| 8 | `:friendly-home` | **nil** | **nil** |
| 9 | `:trail-grad` | 0.30 | 0.25 |
| 10 | `:novelty` | **nil** | **nil** |
| 11 | `:dist-home` | 0.50 | 0.15 |
| 12 | `:reserve-home` | 0.60 | 0.65 |
| 13 | `:cargo` | 0.40 | 0.10 |

`nil` ⇒ KL=0 (C-mean = predicted-mean), i.e. **dead jacks** — the ant has no preference
there. Four are permanently dead (`:food-trace`, `:pher-trace`, `:friendly-home`,
`:novelty`); `:food` is dead only when homebound. Render dead jacks distinctly: they are
*inert*, which is NOT the same as FREE (free jacks carry a pinned value that propagates).

## 2. SUPERSEDED — Joe's design-pattern reframe (2026-07-16, later)

**The section below is kept for the record but its premise is wrong.** Joe:

> each hanzi character in the MetaCA represents a **design pattern**. This design pattern
> says: **IF (left) HOWEVER (right) THEN (next) BECAUSE (ego)**. ... The terminals you
> talked about are actually more like **tokens**. So, IF `:carrying` for example. So, 14 vs
> 8 is kind of irrelevant. The hanzi/exotypes/iiching symbols are **operators** that could
> locally alter any design pattern's behaviour. E.g. instead of IF `:carrying` — rather
> IF `:carrying + :hungry`.

This is already in the code, verbatim — `futon5/src/futon5/mmca/exotype.clj:1-13`:

> *Exotypes: kernel-rewrite operators derived from sigils. An exotype is 36 bits, not 8:
> LEFT (8): IF / preconditions; EGO (8): BECAUSE / rationale (the sigil); RIGHT (8):
> HOWEVER / risks; NEXT (8): THEN / outcomes; PHENOTYPE-FAMILY (4).*

And `futon5/README-sigils.md`: a sigil is an 8-bit exotype plus a **xenotype-32** — one
8-bit word per IF/HOWEVER/THEN/BECAUSE clause, each tagged with its nearest I-Ching anchor.
`scripts/head_exotype_probe.py` already computes this for missions.

**So a CA rule entry IS a design pattern**: given `(left, ego, right) → next`, read it as
IF left HOWEVER right THEN next BECAUSE ego. A rule byte is **8 design patterns** — a small
pattern language. And the propagator, `bit[f(k)] := ¬bit[k]`, is a **contrast operator over
their THENs**: *whatever you conclude in situation k, conclude the opposite in situation
f(k)*.

### What this does to the constraint model — it survives and gains a meaning

| constraint part | pattern-language reading |
|---|---|
| **FREE** | the **givens** — patterns whose THEN nothing derives |
| **CHAIN** | the **derivation** — THENs fixed by contrast with others |
| **UNSAT** | the **HOWEVER** — irreducible tension, `g[k]=¬g[k]`, can never settle |

Figure 8 = **one given, a six-step chain, one irreducible tension.** The unsatisfiable core
is not a defect of the operator; it is the *force* that makes a pattern alive rather than a
rule.

### What dissolves, and what survives

- **DISSOLVED: 8 vs 14.** Joe is right. The pattern FORM is fixed at four slots; the
  terminals are **tokens** filling them. Counting jacks was the wrong level.
- **SURVIVES, and sharpens: the KIND mismatch.** The C-vector is a set of *preferences*
  (observation → desirability). It has **no IF/THEN** — it is not a design pattern at all.
  So the ant port ran a *pattern-rewrite operator on a non-pattern*. That is a deeper
  statement of what `F-propagator-on-c-vector-NEGATIVE.md` measured ("a rule byte is not a
  goal"), and it says where to look instead: **the ant's design patterns are in its POLICY
  (situation → action), not in C.**

### v0 scope change (belled to codex-1, job invoke-1784212645729-648-87dd0e76)

**BUILD** the MetaCA board (8 bit-plane jacks) + FREE/CHAIN/UNSAT readout + genotype/
phenotype pane. **DROP** the 14-jack ant board — leave a seam. Expressing the ant as a
design-pattern cascade is real design work, not yet done; shipping a C board would ship the
mis-mapping.

### ANSWERED — the ant's design patterns exist, are wired to G, and HAVE NEVER FIRED

Joe: *"If it's AIF, it should have G-over-policies — the policies (at least in the war
machine version) ARE design patterns."* Correct, and they are already implemented.

**Five design patterns** (`src/ants/aif/pattern_sense.clj:15`):
`:cyber/baseline`, `:cyber/hunger-coupling`, `:cyber/cargo-return`,
`:cyber/pheromone-tuner`, `:cyber/white-space`.

Each has IF/HOWEVER/THEN/BECAUSE shape in `pattern_efe.clj`. E.g. `cargo-return-risk`:

```clojure
(and (= action :return)      ; IF
     (< cargo 0.15)          ; HOWEVER
     (not= mode :homebound)) ; -> risk penalty  ; THEN
;; BECAUSE: "cargo-return-discipline" -- the pattern id IS the rationale
```

They are **wired into the live EFE** — `policy.clj:611` calls
`(pattern-efe/pattern-efe pattern-id action observation {:efe efe})`.

**THE CHAIN, AND WHERE IT BREAKS:**

```
core.clj:137     pattern-feats (when (get-in ant [:cyber-pattern :id]) ...)
pattern_sense:109  pattern-id (get-in ant [:cyber-pattern :id])
policy.clj:609   pattern-id (get-in observation [:pattern :pattern/active])
policy.clj:610   pattern (when pattern-id ...)        <- nil => pattern-efe NEVER RUNS
```

Everything hangs on `ant → :cyber-pattern → :id`. **The only writers of that key in the
entire tree are `src/ants/cyber.clj:210` and `:242`** — the deprecated cyberants. Nothing
in `src/ants/aif/`, nothing in `xeno_loop.clj`, nothing in `ant_authority_gate.clj` ever
sets it. (`pattern_sense.clj:125` only bumps `:ticks-active`.)

> **Every xeno-loop and authority-gate run to date was on ants with all five design
> patterns switched OFF.** The policy space was four macro actions
> (`[:hold :forage :return :pheromone]`, `policy.clj:12`) and nothing else. Not a bad
> patch — an instrument with the patterns unplugged.

**The irony, and a correction to `README-xeno-loop.md` §0.** That section says *"If you find
yourself wiring something into `:cyber-pattern`, stop."* But it also says, accurately,
*"only `:id` and `:ticks-active` are consumed anywhere."* Both are true, and together they
mean: **`:cyber-pattern :id` is the ignition switch for the design patterns.** The §0 guard
is right about `:config` (genuinely inert — `:policy-priors`, `:pattern-sense`,
`:adapt-config` are stored and never read) and wrong about `:id` (live and load-bearing).
The guard rail written to prevent the cyberants error is currently also preventing the
patterns from ever running.

**The gap vs the War Machine.** The ant computes G over **4 macro actions**, with patterns
as a *penalty term* on those actions (`policy.clj:611` adds `pattern-efe` to the
controller score). The WM computes G over **policies that ARE patterns**. So in the ant, the
pattern layer is a *modulation*, not the policy space. Matching the WM means making **G
range over the patterns themselves**.

### The port, restated

1. **Turn the patterns on** — set `:cyber-pattern :id` on modern AIF ants. Needs a §0
   amendment first (`:id` live, `:config` inert), or the guard will read as a violation.
2. **Make G range over patterns**, not over 4 actions with a pattern penalty.
3. **Reify the patterns as DATA.** They are currently hard-coded `cond`/`case` in
   `pattern_efe.clj`. An exotype cannot rewrite Clojure. To do Joe's
   `IF :carrying` → `IF :carrying + :hungry`, a pattern must be data:
   `{:pattern/id … :IF [[:action := :return]] :HOWEVER [[:cargo :< 0.15]] :THEN [:risk 0.3] :BECAUSE "…"}`
4. **Only then** does an exotype have something to act on.

Steps 1–3 are prerequisites. Until they land there is no ant board, and no propagator
target.

---

## 2b. (SUPERSEDED) THE DESIGN PROBLEM — flagged, needs Joe

**The two boards are not the same kind of object, and they are not the same size.**

| | MetaCA jack | ant jack |
|---|---|---|
| holds | a **response** to a condition | a **desirability** of an observation |
| the object is | a **policy** entry (condition → action) | a **preference** entry |
| count | 8 | 14 |
| type | binary | continuous (mean/sd) |

So **one wiring cannot literally drive both panes.** This is the same mismatch
`F-propagator-on-c-vector-NEGATIVE.md` found by measurement ("a rule byte is not a goal"),
now visible in the terminal list.

And the conjugacy theorem says transferring σ *literally* is meaningless anyway: σ's
abstract structure carries no outcome information; the registration against semantics
carries all of it.

**What CAN be shared is the constraint-graph SHAPE** — how many FREE, how long the CHAIN,
how big the UNSAT core. That is substrate-independent; σ is not.

**Resolution for v0 (recommended, revisit after Joe has played):** two boards, one shape.
The middle pane holds *both* patch boards. Patching either updates its own pane. A **shape
readout** (FREE / CHAIN / UNSAT counts) is shown for both, so Joe can dial the ant board to
*the same shape* as a MetaCA wiring he likes and see what the ants do. The claim being
tested is "same shape → same character of behaviour", which is exactly the transfer
hypothesis, made visible instead of assumed.

## 3. The three panes

```
┌─────────────────┬───────────────────────────┬─────────────────┐
│  ANTS           │  PATCH BOARDS (control)   │  METACA         │
│  live colony    │  ant: 14 jacks            │  genotype rows  │
│  yield readout  │  metaca: 8 jacks          │  + phenotype    │
│                 │  FREE/CHAIN/UNSAT readout │  rule histogram │
└─────────────────┴───────────────────────────┴─────────────────┘
```

- **Left — ants.** Existing sim (`src/ants/aif/`, **not** `cyber.clj` — see
  `README-xeno-loop.md` §0). Show the colony, and per-ant C as a small bar strip so the
  scaffold is visible *in the agent*.
- **Middle — the patch boards.** Click jack *k*, then jack *j*, to lay a cable `k → j`
  meaning `j := ¬k`. Self-loop `k → k` allowed (that is UNSAT/motion). **Multiple cables
  into one jack allowed** (that is the bug's doubled bit 0). **Jacks with no incoming cable
  are FREE** — colour them as the scaffold. Live readout: `FREE {…} CHAIN {…} UNSAT {…}`
  and, for MetaCA, the derived attractor set (e.g. `{42,170}`) computed by the constraint
  solver, not by simulation.
- **Right — MetaCA.** Genotype spacetime (sigil colours) + phenotype. **NB the genotype
  layer is autonomous** — it never reads the phenotype (F-doc §1), so the genotype pane is
  the real object and the phenotype is a rendering. Render both; label which is which.

## 4. Hard requirements (each is a scar)

1. **Non-injective patches MUST be expressible.** Permutations have no scaffold; the bug is
   not a permutation. If the UI cannot express `k ↦ max(k-1,0)`, it cannot express Figure 8.
2. **The propagator fires DURING the run, every tick, against live state.** Not applied-then-
   frozen. `README-xeno-loop.md` §2 — the category error that produced "drift beat evolved".
3. **No hidden config.** Every knob that changes dynamics is on screen. (The Copycat port's
   `run()` silently hardcoded a temperature formula that suppressed the one interesting
   answer — `futon2/vendor/copycat/README.md`. Same class of trap.)
4. **σ over the union with per-mode partial application**, not the intersection —
   `README-xeno-loop.md` §2. The intersection deletes `:food`.
5. **This is an instrument, not a gate.** It produces *insight*, not a verdict. Do not add a
   score that ranks wirings; the paper's own thesis is that the eye beats the instruments
   here and that this is not fixable. The eye is the point.

## 5. Acceptance

- Joe can patch a wiring and watch both panes change **live**, with no restart.
- Setting the ant board to identity shows **14/14 UNSAT** and visibly destroys foraging —
  the sham, made visible.
- Setting the MetaCA board to `k ↦ max(k-1,0)` shows **FREE {7}, UNSAT {0}**, derived
  attractor `{42,170}`, and reproduces the Figure-8 picture in the right pane.
- The shape readout is correct against `futon5/scripts/propagator_constraint_model.py`.

## 6. Out of scope for v0

Sweeps, fitness, selection, evolution. This is the airship's hangar, not its flight test.
