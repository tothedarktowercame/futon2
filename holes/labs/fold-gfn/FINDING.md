# fold-GFN — first increment finding (claude-4, 2026-07-01)

**Feasibility: YES.** `gflownet` framework (~/code/gflownet, Choices-env + TB + small MLP, CPU) + torch in
`futon6/.venv` → laptop-GFN is buildable, reusing `train_cascade_gfn.py` as the template.

**Reward harness built** (`reward.py`): mission + selected patterns → minimal linear CLean wiring →
0-sorry gate (clean_to_lean) → structural meaningfulness. Runs.

**BUT the naive reward is FLAT — the M1 wall, confirmed for the fold:**
- `0sorry=1.0` for full-cascade, subset, AND scramble — a *linear* method-spine always type-checks, so the
  0-sorry gate does NOT discriminate pattern-selections.
- `struct_sim=0.0` throughout — the 33-d structure embedding is near-orthogonal on mission wirings.
- ⇒ reward ≡ 0.4 for everything. A GFN cannot concentrate on a flat reward (exactly M1's cascade-sampler
  failure).

**The crux = a STEEP reward. Two fixes (next):**
1. **Typed constructor** — build wirings with real typed ports (consumes/produces TYPES that can mismatch),
   so 0-sorry actually FAILS on bad selections → the gate becomes discriminating. (The honest 0-sorry
   steepness M1 wanted only exists if wirings can fail.)
2. **Empirical-match reward** — for the A-small missions, reward = overlap(selected spine, the empirical
   `.clean.edn` `:clean/seq`). Directly steep; supervised by the 10 hand-authored folds.
Recommend (2) first (immediately steep, uses the gold corpus), then (1) for generalization beyond the 10.

**Status:** laptop-GFN reward is the load-bearing piece; naive version flat; steep-reward redesign is the
next build. Reuses Choices-env + clean_to_lean + the A-small corpus. No GPU needed.

## FIX WORKED (2026-07-01) — steep reward → GFN learns + generalizes, laptop-only

`fold_gfn.py` (minimal trajectory-balance GFN, torch/CPU `futon6/.venv`), reward = exp(β·F1(selected
spine, empirical spine)), task = select the wiring spine from a mission's cascade (verified well-posed:
94% of spine-methods ∈ cascade).
- **Reward climbs 0.398 → 0.68** over 800 steps — the GFN concentrates. **The M1 flat-reward wall is
  passable with a steep reward** (the whole point).
- **Leave-one-mission-out greedy spine recovery: mean F1 = 0.69 vs random-subset 0.51** — it generalizes
  a transferable "which cascade patterns become wiring boxes" policy across held-out missions.
- **Laptop-only** (no GPU). First clearly-positive ML result in M-fold-ansatz.

**Honest caveats:** n=10 missions (small); F1 0.69 vs 0.51 is a real-but-modest lift; this is spine-
SELECTION (the load-bearing fold decision — which patterns), not full wiring-structure generation (next).
**But the load-bearing claim holds: a steep reward makes the GFN a working fold-designer, on the laptop.**
This is the direct positive counterpart to the three shallow-embedding negatives — same "signal/reward
steepness is the crux" lesson, now on the *winning* side.
