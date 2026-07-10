# E-mine-mission-transitions — give the agent a memory of previous actions (close R16's rollout path)

**Date:** 2026-06-25
**Status:** **DERIVE — witness PASSED + structure-embedding BUILT.** A mined move flips an abstaining mission's ΔG nil → real; `mission_structure_embed.py` gives same-class provenance (0.79 vs 0.38 text). Readiness map: `mission-mining-readiness.html`.
**Owner:** Joe + claude-1
**Relation:** the home for **criterion (2)** of [[M-aif-wiring]] R16 ("close the loop") — the rollout-path
gap that makes the act-gate abstain. Parent: [[M-wm-policies]] (Track 2, the rollout engine + arrow contract).
Cross-ref: [[E-vwm]] (the 0.5 stall threshold), [[E-llm-fold]] (an LLM reads a cascade → construction),
`futon6/scripts/clean_structure_embed.py` (the structure-embedding precedent: verified N=7 — same-family NN-match 0.86 structure vs 0.14 text; NN-sim 0.95 vs 0.25).

---

## The gap (Joe, 2026-06-25)

R16's loop is wired to the consent gate, but the act-gate **abstains** (`:abstain-missing-leg`) on every
served mission because ΔG (rollout `G(π)`) is `nil` — the mission has **no rollout path**. WM-I4 (operator
arming) is *exogenous/meta*, not an AIF criterion; the real AIF-internal gap is this missing path.

Joe's framing: *"the agent literally can't imagine acting on these missions."* The fix is to **give it a
memory of previous actions** and help it relate the current possible actions to previous ones — mine the
historical missions via the mission↔pattern↔cascade co-embedding.

## MAP — why the path is missing (grounded 2026-06-25)

The move-set (`futon6/data/diffsub-moves.edn`, `diffsub_emit.py`) is **not** truncated-by-cap (the first
hypothesis); the real cause is narrower and structural:

- `diffsub_emit.py` mints moves **only from detached scopes** (open substrate-2 holes; `det_scopes`, line 390),
  in exactly **three action-classes**: `:close-hole`, `:advance-capability`, `:centre-mess`.
- The abstaining targets `M-canon-fingerprint-store` / `M-bayesian-structure-learning` have **0 detached
  scopes** (all `loose-section`/`eightfold-phase`/`map-item`, attached) → the generator emits **zero moves**
  for them → no path → ΔG nil → abstain. (Whole corpus: only ~15 phase-chain moves across 6 missions.)
- So the abstain is *honest* (R6 declining to commit with no evaluable policy), and the deficiency is the
  **action vocabulary**: there is no move-class "apply a cascade that worked on a structurally-similar
  mission." That is exactly the class mining supplies.

## DERIVE — the mining chain (Joe), and where it breaks

1. mission↔pattern co-embedding — **LIVE**: `futon3a/resources/notions/minilm_{pattern,mission}_embeddings.json`
   (490 patterns + 800+ missions, 384-d, L2-normed) + `futon6/data/mission-pattern-scopes.edn`
   (`:applied` + `:try-candidates` per mission).
2. cascade → "missions where its patterns were used" — **free** (invert `:applied`).
3. cascade → set of possible **mission-actions** — **BREAKS HERE**: the co-embedding gives *which missions*
   (prose neighborhood), not *which action within the mission*. That needs mission **structure** (phase +
   scope state), which is computed (`futon6/data/mission-scope-trees/*.json`) but **not embedded**.

**The missing rung = a structure-aware mission embedding.** Precedent exists: `clean_structure_embed.py`
embeds *proof* structure (method-bag/macro/satiety/comb-scalars/bigrams, ~33-d) and beats text decisively
(**verified N=7: same-family NN-match 0.86 vs 0.14; NN-sim 0.95 vs 0.25**). Port it from proofs to missions
(phase sequence, scope-count distribution, applied-pattern graph,
hole-states). DarkTower type discipline: mined transitions = level-1 (iching/changes); precision over which
to trust = level-2 (iiching/curvature) = **R14 γ**. So criterion (2) is strictly upstream of R14:
**coverage → verdict history → policy precision.** Execution of a chosen move is already solved by an LLM
reading the cascade (E-llm-fold); JAX/tensor-CT (futon5) is the *fast* re-impl, deferred ("work at all" first).

## VERIFY — first witness PASSED (2026-06-25)

A single **mined** move (provenance: nearest co-embedded mission's phase move, prior **borrowed not
fabricated**) gives an abstaining mission a real ΔG. Reproduce:

```
cd /home/joe/code/futon2 && clojure -M /tmp/mine-witness.clj     # (scratch script; canon-only vs +mined EDN)
```
Result:
```
canonical diffsub-moves.edn:  0 moves match "canon-fingerprint-store"  →  ΔG = nil      → :abstain-missing-leg
+ 1 mined phase-advance move: 1 move  matches                          →  ΔG = -7.41e-4 → gate computes a verdict
```
ΔG < 0 ⇒ G-leg favorable; with a real ΔF the gate now yields `:pass`/`:fail`, not abstain. Sim-only, zero
`:7071` writes, no arming. **The held R16 arc was held by "no terrain," and a mined move builds terrain.**

**Key finding that set the next build:** the nearest text-co-embedded analog was only **cos 0.31** — below
E-vwm's 0.5 stall threshold — so a text-only mined prior is weak. This was the empirical case for the
structure embedding (verified N=7 on proofs: same-family NN-match 0.86 vs 0.14).

## VERIFY — structure embedding BUILT + better provenance (2026-06-25)

`futon6/scripts/mission_structure_embed.py` — the port of `clean_structure_embed.py` from proofs to
missions. 198 missions → a deterministic **161-d** structural vector (phase presence/detached · class
one-hot · binder histogram · shape scalars · phase-transition bigrams · applied-pattern presence), z-normed
across the corpus + L2 per row. Outputs `futon6/data/mission-structure-embed/{structure-embeddings.npy,
mission-embed.json}`.

**Discrimination (the same-class NN-match, mirroring the proof side's same-macro metric): STRUCTURE 0.79 vs
TEXT 0.38** across 198 missions — structural retrieval finds a same-class mission twice as often as text.

**The provenance payoff, end-to-end:** for the abstaining `M-canon-fingerprint-store` (class `pipeline`),
restricted to missions that already have rollout moves —
- **structural** neighbour = `M-structure-seed-promotion` (sim 0.60, **same class**, has moves);
- **text** neighbour = `M-symbol-grounding` (sim 0.31, **wrong class** `alive`).

Borrowing the *structural* neighbour's move-prior and re-running the witness flips ΔG nil → **−7.71e-4**
(`/tmp/mine-witness-struct.clj`). So the full chain holds: **structure-embed → same-class provenance →
mined move → real ΔG**, with a far better-justified prior than the text neighbour. Still sim-only, zero
`:7071` writes.

## VERIFY — generator across the whole abstaining set (2026-06-25)

`futon6/scripts/mission_mine_moves.py` generalizes the witness: for every move-less mission, mint one
excursion-shaped phase-advance move ("fill the next hole"), prior borrowed from its nearest structural
neighbour-with-moves → `futon6/data/diffsub-moves-mined.edn` (55 canonical + **177** mined).

Rollout flip metric (`/tmp/flip-metric.clj`): **177 / 177** move-less missions now get a non-nil ΔG
(median −7.7e-4, range [−2.3e-3, −6.5e-4]); **0 remain nil.** The whole abstaining set now has a rollout
path — the held R16 arc has terrain everywhere.

**Honest caveat — coverage ≠ quality (the real frontier):** same-class provenance is **63%** (111/177),
sim median 0.42; the 37% with weak / cross-class priors are *guesses*. The path exists everywhere;
sharpening those priors is the job of the turns layer (`MOVE-RECOGNIZER` over turn prose + `TURN-SYNTH`
reading the PSR/PUR where the pattern fired). **n=1 → n=177, diagnosable by provenance quality** — the
metric shape the work is held to.

## VERIFY — MEME-MINE v0 (small sample, 2026-06-25)

`futon6/scripts/mission_mine_memes.py` (`--sample` / `--emit`) + `futon6/data/meme-mine/`. Sampled 12 real
human→agent asks (of **1449** qualifying in the corpus), extracted `(have, want, op)` memes
LLM-in-the-loop (claude-1; the same step a served model runs on a Linode GPU at scale). 11 memes (1 dropped:
auto-bellback noise → sampler must exclude auto/system callers).

**The result that matters:** an EMPIRICAL operation-class vocabulary emerged —
`dispatch · relate · find · build · deploy · assign · preregister · reconstruct · reuse · investigate`
(~10 classes from 11 asks) against the **3** hand-coded move-classes (`close-hole`/`advance-capability`/
`centre-mess`). The move-corpus at its TRUE operational grain; answers the `MOVE-VOCAB` question empirically
(from the turns, not by fiat) — and shows why the phase-advance moves were impoverished.

**Next sub-tasks:** (1) **endpoint-identity** — resolve a meme's free-text have/want to scope/mission/cap
ids and unify onto the arrow store, so a mined meme *grounds a specific mission's weak prior* (the frontier
payoff); (2) **scale** the extraction on a Linode GPU; (3) normalize the op-classes into a canonical set.

## Design note — Excursions ≈ Flights ≈ the move grain (Joe, 2026-06-25)

An Excursion's semantics differ from a Mission's: it is spawned *mid-mission* — "there's a technical /
research hole to fill before the mission can complete." So an excursion is a **reduced mission** that uses
part of the derivation structure to fill ONE hole — structurally the same shape as a Car-3 **Flight** (a
scoped run exercising one criterion; cf. [[E-car3-first-flight]]).

Consequence for the mining: the WM's atomic move IS `:close-hole`, so an **excursion is the natural unit of
a move / cascade** — "to fill hole H in mission M, do excursion E" is a transition with a worked record and
(via its turns / PSR-PUR) an outcome. Missions are *aggregates* of excursion-shaped moves; **excursions may
be a purer instance of the core WM discipline than missions.** Therefore: (a) the phase-advance moves this
pipeline mints are already excursion-shaped ("fill the next hole"); (b) excursions/flights should eventually
be first-class move-records in the corpus (today the scope corpus is mission-centric — `E-*.md` not
necessarily ingested); (c) `M-autoclock-in` should record the mission→excursion spawn so the
turn↔(mission|excursion) link carries the grain. *Not a course change — a grain-tag to fold in.*

## Next (make-it-work-at-all → metric)

1. ~~**Port `clean_structure_embed.py` → missions**~~ — **DONE** (`mission_structure_embed.py`; 0.79 vs 0.38
   same-class NN; structural provenance verified end-to-end).
2. ~~**A mined-move generator for the abstaining set**~~ — **DONE** (`mission_mine_moves.py`; 177/177 flip
   abstain→ΔG; provenance 63% same-class). Remaining on this rung: the live wiring (emit the mined overlay
   into the served judgement so the LIVE gate stops abstaining) + `:advances-cap` by declaration where a
   move closes a capability-scope (the M-wm-policies seam), never inferred.
3. **Metric (progress, not throughput):** at n=1, one served mission gets a non-nil ΔG sourced from ≥1
   precedent (DONE — this witness). Rising n=1→10 as structural neighbors are mined; **diagnosable** — a
   mission that still abstains has no structural precedent yet (a genuine island → "needs a foothold",
   surfaced honestly, per the three-kinds-of-off-map split in [[M-wm-policies]] §4).
