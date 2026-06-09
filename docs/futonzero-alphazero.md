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
