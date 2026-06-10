# FutonZero — AlphaZero ideas over the futon stack

*How the War Machine learned to select over policies, not just rank next-steps.*
Authored 2026-06-09 (claude-1, coordinating claude-3 + claude-4 + the codex pool).
Companion to `M-wm-policies` (the build) and `futon-aif-completeness.md` (the R1–R12 contract).

## 1. The one idea

AlphaZero plays a game by combining a **policy prior** (a network that proposes promising
moves) with a **search** (MCTS that rolls move-sequences out and backs up a value), trained by
**self-play reward**. FutonZero plays the *development* game the same way: the "board" is the
futon stack's own structure, the "moves" are patterns/arrows, a "policy" is a *cascade* (a
pattern-language assembled for a circumstance), and the reward is a **peradam** (a certified unit
of earned capability). The War Machine is the player.

**Honest caveat on the lineage (Fable's review, 2026-06-09).** AlphaZero's *defining* feature is not
prior+search+value — that's classical game-tree search with a learned evaluation (Stockfish-with-a-prior).
What makes it AlphaZero is the **closed self-play loop**: an adversary that *is itself*, generating an
automatic curriculum, with an *incorruptible* terminal reward. **FutonZero v1 does not have that yet** —
the value (`C`) is handcrafted, the prior isn't trained from outcomes, and R2 is deferred. So v1 is
structurally a **single-agent MDP**, closer to **AlphaTensor** (which framed algorithm-discovery as a
single-player "TensorGame") than to AlphaZero. Earning the name needs two things v2 supplies: **R2**
(search-result trains the prior = the self-play loop), and an **adversary** — and the natural adversary is
the **Pudding Prover**: the anti-laundering verifier that tries to *refute* a peradam claim. That makes the
loop genuinely two-player and imports the curriculum dynamics. Until then, read the table below as the
*architecture*, not the *closed loop*.

**…or maybe self-play is the wrong frame entirely (Joe, 2026-06-09 — measured negative on the realized-`G(π)`
floor 2026-06-10, revisit under the grounded yardstick; see the falsifiable-test result below).** The peradam may be
*exogenous* (like AlphaZero's Lee Sedol match — the proof, not the trainer), and the endogenous engine may
not be a formal adversary at all but **argument-across-possible-worlds**: competing *pattern-theoretic
buildouts* of the same circumstance, the more-*whole* one winning. This would **out-bootstrap** AlphaZero —
they dropped the *game history* (no human games), we drop the *game board* (no perfect simulator): the
cascade's `C`-score *evaluates* a possible world directly, so **the patterns (good-enough invariants)
replace the simulated playout**. And the machinery exists: `E-possible-world-regulator` is the referee, the
multi-agent dialectic (gradient *vs* rollout; disagreement-as-signal) is the argument, the judge-panel
patterns are the tournament. Generative dialectic (the curriculum) + adversarial refutation (the
Pudding-Prover peradam, the incorruptible win) + the exogenous peradam-anchor (against Goodhart drift) may
be the real shape. **Falsifiable test:** does arguing across buildouts beat the single best buildout? If not,
it's ceremony. This is plausibly the conceptual core that makes FutonZero *Futon*-Zero rather than a port.

> **Measured (M-arguing-worlds v0, 2026-06-10 — codex-1 build, claude-1 PM-review, `2eca617`):** on the
> realized-`G(π)` floor, **no** — on a genuinely-diverse generator (4 lenses, pattern-overlap 0.11, moves
> disagree) the dialectic does *not* beat the single best buildout (`:single-best-holds`). So **on this
> floor it is ceremony** — Joe's own argument-across-worlds hypothesis falsified and recorded honestly, a
> clean campaign negative (guarded against both failure modes: yardstick is realized-`G(π)` not `C`, and a
> non-diverse generator returns `:monotone-generator` before judging — not a circular grade, not a
> diversity artifact). **Caveat:** the yardstick is the realized-`G(π)` floor, *not* the grounded peradam;
> the peradam-yardstick seam is `:escrowed` ([[M-peradam-grounding]]) and the verdict is revisit-able under
> it. **Interlock:** M-pattern-posteriors v0 (sparse near-null) lands the *same* conclusion from the other
> lane — both point at the grounded peradam as the unlock.

The pivot that made this possible: **Expected Free Energy is a property of *policies*, so `G(π)`
is a path integral, not a scalar field.** A "next step" is the degenerate length-1 policy. Ranking
single actions over a static field is the special case; the general object is the *geodesic* — the
least-EFE *path* through the terrain toward a goal-anchor.

## 2. The mapping (AlphaZero → FutonZero)

| AlphaZero | FutonZero | Built by |
|---|---|---|
| The board (a position) | substrate-2 state — the unified scope/mission/capability/pattern hypergraph (Futon City) | claude-3 (materialize+link) |
| The local value of a square | the metric `g(s)` = Salingaros `C = T·(10−H)` (epistemic) + graph-ascent (pragmatic) | claude-3 + claude-1 |
| A move | a pattern / a `(have, want)` arrow (claude-4's meme-arrow store) | claude-4 |
| The policy head `P(s,a)` | the **gradient prior** — `grad(loss)(A)` ranks candidate moves (the `jax_refine` port over the substrate metric) | claude-3 |
| MCTS search | the **rollout** — `futon2.aif.rollout`, a discounted path-integral `G(π)=Σ γ^t g(s_t)` over move-sequences, PUCT-weighted by the prior | claude-4 |
| The backed-up value | `G(π)` (lower = better) | claude-4 |
| Self-play reward → trains the policy net | **the peradam** → realized `G(π)` per move trains the gradient prior (R2 return channel) | (v2) |
| A whole game / opening book | a **cascade** = a pattern-semilattice assembled on-the-fly for a circumstance `|ψ⟩` — *the scored ARGUE* | claude-4 (construction) |
| "Goal" (win) | a **capability goal-anchor** in the star-map; reachable *summits* vs unreachable *islands* | claude-1 + claude-3 |

Two ideas are genuinely futonic, not just borrowed:
- **A cascade is an on-the-fly Pattern Language** (Christopher Alexander's original sense). It *is*
  the ARGUE phase of a mission made first-class: "what patterns make the case for *this* design?"
  Patterns overlap, so a policy is a **semilattice**, not a sequence ("A City is Not a Tree").
- **Wholeness is the value, and it's monotone.** Coherent reinforcement doesn't self-limit; size is
  bounded *externally* by coverage-saturation (the argument is expressed) + a parsimony budget — not
  by an internal complexity-aversion. "Reward wholeness, never pointwise-greedy" is the lesson, and
  it recurs at every scale (the same degeneracy that made the Emacs cursor rank #1 reappeared at the
  policy-scoring level, with the same fix).

## 3. The numbers (the board FutonZero plays on)

- **~278 mission documents** (`M-*.md` across futon0–7); **~199 active missions** in substrate-2,
  **230 mission+capability nodes** at the v1 reachability grain.
- **1,073 embedded patterns** (the MiniLM index the cascade constructor ranks over; **996
  `.flexiarg` patterns** in the library); **2,538 pattern co-application edges** (the phylogeny =
  the prior over which patterns combine).
- **5,517 scopes** in substrate-2 (**194 scope-trees** materialized in D1) — the fine-grained board
  that scope-grain v2 brings online for the rollout.

## 4. Where it stands (honest v1)

- **Live now:** the single-step EFE fixed (the cursor demoted #1→#3, on-ascent work #1/#2), and the
  **visible cascade-policy lane** — every WM snapshot auto-carries, for its top missions, the
  budget-6 cascade-policy (e.g. advancing `M-capability-star-map` → a coherent 6-pattern ARGUE,
  wholeness `C`=9.88; the silly cursor's argument is *thin*, `C`=1.145 — a second signal that agrees
  with the ranking).
- **Built + verified, scope-grain-gated:** the rollout search (witness passes — a 2-step policy
  `G=−1.0` beats greedy `G=−0.2` where step-1 unlocks step-2) and the real gradient prior (anchors
  behave: summits ≫ islands). In v1 the rollout's reachable set is 3 summits, so its multi-step
  value-add waits on scope-grain v2; the cascade lane is v1's visible non-degenerate-policy path.
- **v2 (in flight, claude-3 + claude-4):** scope-grain (richens the reachable graph), the R2 return
  channel (the learnable prior — closing prior→search→train), and the pilot *consuming* the cascade
  (acting on the policy, not just displaying it).

So FutonZero v1 is a real, honest first instantiation: the player sees the board, proposes
move-policies, scores them by wholeness, and surfaces the cascade — with the deeper search and the
self-improving loop staged behind a clearly-labeled edge.

## 5. What v1 is NOT — open tensions + a kill criterion (Fable's review, incorporated)

A typed sorry beats a smooth claim. Honest edges, in load-bearing order:

- **Lineage (see §1 caveat):** single-agent MDP, AlphaTensor-shaped — *not yet* AlphaZero. The adversary
  (Pudding Prover as refuter) + R2 (the self-play loop) are what would earn the name.
- **The reward is self-graded — Goodhart's door.** Wholeness `C` is computed by the system over its *own*
  pattern assembly: a cascade can score coherent *without doing work* (assemble six historically-co-applied
  patterns → a respectable `C` whether or not the ARGUE bears on the circumstance). **So `C` is the *value
  heuristic*, not the reward.** The reward is the **peradam**, and "certified by whom?" is answered
  *externally* by the **Pudding Prover's 3-witness certificate** (labor + arrow + fruit). **Laundering
  `C`-scores into reward without that check is precisely the failure the anti-laundering invariant forbids**
  — the same de-laundering discipline applied to `certify-peradam` (this session). The peradam's
  certification *is* a typed proof-state; `C` only ranks candidates *for* it.
- **Semilattice vs the linear rollout.** A cascade is a *semilattice* (patterns overlap — "A City is Not a
  Tree"), but `G(π) = Σ γ^t g(s_t)` is a path integral over a *sequence*; MCTS rolls out linear orders. So
  either the rollout **linearizes** the cascade at search time (quietly betraying the partial-order claim
  where it matters most), or it needs a procedure that natively samples partial orders — **GFlowNets** are
  the better-fitting machinery (sample compositional objects *proportional to reward* — nearly a definition
  of "assemble a pattern-language scored by wholeness"). In v1 the cascade lane (the semilattice) and the
  rollout (the linear path) are **separate** machineries; reconciling them is open.
- **The parsimony budget is a hyperparameter, not a principle.** Since `C` is monotone and bounded only
  externally, **the budget *is* the regularizer** — budget-6 sets the character of *every* cascade. Why 6?
  An *empirical* read of the marginal-coverage knee (~5–8 strong centres before the tail goes marginal), not
  a law — **labeled here as a tunable constant.** And, per this doc's own scale-invariant-degeneracy lesson,
  *exactly* the kind of unexamined constant where the next degeneracy could hide.
- **The witness is constructed — the real test is empirical, and here is the kill criterion.** The
  2-step-unlocks case passes, but the open question is whether mission terrain has unlocking structure
  *densely enough* for multi-step search to pay rent. If most of the ~199 missions are reachable greedily,
  the rollout is elegant machinery for a flat board. **Falsifiable prediction:** *after scope-grain v2,
  ≥ ~15% of top-ranked policies should be non-greedy (a multi-step `G(π)` strictly beating its greedy
  prefix). If not, the rollout is over-engineering — kill it and let the cascade lane carry the value alone.*
  (15% is a first-cut threshold; refine against v2 data. The point is that v1 currently lacks a kill
  criterion — this is it.)

## 6. The pitchable artifact (Fable's strategic note)

The most *legible* result is **not** the search — it's the **cascade-as-scored-ARGUE**: *"given this
circumstance, here is the pattern-language that makes the case, with a coherence score."* That reads to
people who will never care about EFE geodesics — including the **UKRN / consulting audience** the stack is
converting before August. The thin-argument signal (cursor `C`=1.145 vs on-ascent `C`≈9.9) is the
communicable hook: **the system can tell you when its own case is weak.** That is *assurance-machinery*
language, and it is the bridge from FutonZero back to the commercial spine (`M-futon-forward-model`).
