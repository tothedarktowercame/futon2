# E-cascade-tokamak-run1 — the pattern cascade ran the tokamak

**Date:** 2026-07-16. Joe: *"I think it would be great to try it! Even if it
fails spectacularly it would be interesting."* It did not fail spectacularly. It
failed **narrowly and informatively**, in the one place it had staked a claim.

**Apparatus:** `futon5-tok` (worktree of `origin/M-propagators-2026-07-15`),
`scripts/cascade_run.clj`. 12 seeds, 6 windows × 20 generations, width 60.
Every controller saw the **same seeds** (TK-3). No transport, no objective —
`cascade-tokamak-v0` carries none on purpose.

## What each controller did

| controller | bins visited | arms fired | final regime | died |
|---|---|---|---|---|
| **cascade** | `:lean 4, :mid 24, :rich 44` | `:rotate+1 17, :sigma-5127 44, :three+five 11` | `:mid 9, :rich 3` | **0/12** |
| **cascade-no-NEXT** | `:collapsing 1, :lean 7, :mid 25, :rich 39` | `:rotate+1 25, :rotate+2 1, :sigma-5127 39, :three+five 7` | `:collapsing 1, :lean 3, :mid 5, :rich 3` | **1/12** |
| fixed:rotate+1 | `:dead 42, :collapsing 4, :lean 7, :mid 7, :rich 12` | `:rotate+1 72` | `:dead 12` | **12/12** |
| fixed:rotate+2 | `:lean 1, :mid 13, :rich 58` | `:rotate+2 72` | `:lean 3, :mid 3, :rich 6` | 0/12 |
| fixed:identity | `:rich 72` | `:identity 72` | `:rich 12` | 0/12 |

## 1. §4b's central claim, reproduced

**`fixed:rotate+1` kills the field 12/12**, visiting `:dead` in 42 of its 72
windows. §4b: *"rotate+1 has the highest single-window transport in the set and
decays to exactly 0 when held ... the trap is only a trap if held."* Held, it
dies every time. tkc-mid's BECAUSE is vindicated by a run that did not exist when
it was written.

## 2. tkc-mid's NEXT-STEP claim is OVERSTATED — and that is the finding

The box says, in its own words:

> **THE NEXT-STEP IS THE WHOLE PATTERN — without it this box is Figure 8 and the
> field dies.**

Measured, identical seeds: **cascade 0/12 died; cascade-no-NEXT 1/12.**

The sign channel *did* change behaviour — `:rotate+1` fired 25× without it vs 17×
with it, exactly as designed. But removing it did **not** make the box Figure 8.
One seed in twelve reached `:collapsing`; none reached `:dead`. **The claim is
directionally right and rhetorically too strong**, and the cascade's own first run
says so.

This is §4b's verdict on itself, verbatim — *"Switching beats the best fixed
propagator — weakly. ... Suggestive. **Not banked.**"* The tokamak said that about
its greedy arm; the cascade now says it about its own sign channel. **A pattern
made a falsifiable claim about itself and the claim came back weaker than
written.** That is the most useful thing here, and it is only visible because the
claim was written down sharply enough to be wrong.

**What the run does NOT license:** "the NEXT-STEP is useless". 0/12 vs 1/12 with
n=12 is one seed. It licenses "the claim as stated is not supported at this n" —
nothing about the effect's true size. Fixing that needs more seeds, and the
honest move is to say so rather than to round 1/12 either way.

## 3. The UNEVIDENCED boxes never fired — self-report CONFIRMED

`cascade`: `:dead` **0**, `:collapsing` **0**. The two boxes marked
`:evidence :NONE` never ran. cascade-tokamak-v0 predicted exactly this, from §4b's
"those bins came back EMPTY", and its first run confirms it. **Two of five
patterns are decoration.**

## 4. …and TK-2 explains why, live

`fixed:rotate+1` visits `:dead` **42 times**. So the bins are perfectly
reachable — *the cascade just never goes there*. TK-2: *"the very states that
reveal the trap are off-distribution for the policy meant to discover them."*

**The cascade avoids death, so it can never learn what to do in it.** Its two
authored boxes are unreachable *by the policy that contains them* — a controller
cannot gather evidence for the branches it is good at avoiding. The single
exception is the tell: the only time an unevidenced box fired (`:collapsing`, once)
was in **cascade-no-NEXT** — the variant with its sign channel removed. Breaking
the pattern is what reached the state the pattern had no evidence for.

§4b's fix — *"seed exploration with hold-k trajectories"* — is exactly what would
populate those two boxes. The run reproduces the problem that fix was written for.

## 5. fixed:identity never leaves `:rich` (72/72)

Consistent with §1.3 (*"identity is a random walk, no attractor"*) and with
tkc-dead's BECAUSE. It never collapses — and never goes anywhere. A control that
cannot die and cannot act. Worth remembering when reading `:rich`'s FREE status:
the regime no rewrite can write is also the one identity never leaves.

## Honest limits

- **n = 12 seeds.** Every comparison here is small. 0/12 vs 1/12 is one seed.
- **No objective, so no ranking.** The table describes behaviour. "cascade died
  0/12" is not "cascade won" — dying less is not a goal the cascade was given, and
  `fixed:identity` also dies 0/12 while doing nothing at all. That row exists to
  make the point: not-dying is free.
- **My errors on the way, logged:** (1) v1 launched with Bash
  `run_in_background`, which futon3c/CLAUDE.md forbids for warm-pouch agents —
  it died and produced 0 bytes; (2) I claimed dropping `transport-of` would make
  it "~100× faster" — measured, it was **25%** (33s → 24.5s per rollout), because
  I attributed the total to a component I never isolated. That is **TK-1**
  ("`G_w` estimates how good was this SITUATION, never how good was this ACTION")
  committed in a script whose header cites TK-1; (3) I hand-wrote the wiring
  diagram instead of copying `tokamak_advantage.clj:80` — "Unknown component";
  (4) I rendered to `.png` when `write-image!`'s own docstring says PPM only.

## Disposition

The cascade ran the tokamak. The interesting result is not that it survived —
`fixed:identity` also survives, by doing nothing. It is that **a pattern's
preregistered claim about its own sign channel was measured and came back
weaker than stated**, and that **the cascade's two unevidenced boxes were
confirmed unreachable by the very policy that contains them**.

Next, if pursued: hold-k seeding to reach `:dead`/`:collapsing` and give those two
boxes evidence — or delete them and admit the cascade has three patterns.
