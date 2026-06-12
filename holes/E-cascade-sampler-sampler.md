# E-cascade-sampler-sampler — a contest over cascade-sampling methods

Date: 2026-06-12
Parent: `M-wm-policies` (the build); resolves-candidate for the semilattice-vs-
linear-rollout tension in `futon2/docs/futonzero-alphazero.md` §5.
Owner: fable-2 (assigned 2026-06-12 — Joe: "let's focus on this"; harness
spec owner-side, contestant code bellable per the coding-handoff protocol).
Status: IN FLIGHT — harness recon begun 2026-06-12.

The doubled name is the design (Joe, 2026-06-12, "stet"): this is not "build
the GFlowNet sampler" — it is a **sampler over samplers**. Several methods
generate budget-6 cascades for the same circumstance set; one yardstick judges
them; the excursion's product is the comparison, and "the incumbent holds" is
a fully valid outcome (the M-arguing-worlds clean-negative discipline).

## Why now

- `futonzero-alphazero.md` §5 names the open tension: a cascade is a
  semilattice ("A City is Not a Tree"), but the rollout's
  `G(π) = Σ γ^t g(s_t)` is a path integral over a sequence — either the
  search linearizes the cascade (betraying the partial-order claim where it
  matters most) or it needs machinery that samples partial orders natively.
  The doc itself names GFlowNets as the better-fitting machinery.
- The candidate library exists and is healthy:
  `github.com/alexhernandezgarcia/gflownet` — Apache-2.0, Mila lineage,
  pushed 2026-06-11, CPU-only install supported, custom environments are the
  designed extension point. Our problem is small by its standards
  (1,073-pattern action space, budget-6 objects, MLP policy). Friction: pins
  Python 3.10 (dedicated venv).
- M-first-flights (2026-06-12 whistle salvo) just gave flight records an
  R11 training-example projection with derived-never-authored validity
  masks — which is within shouting distance of trajectory-balance training
  input. A trainable sampler and the R2 return channel are two ends of one
  pipe; this excursion stands up the sampler end.

## The contestants (minimum field)

1. **Incumbent**: the deterministic budget-6 cascade constructor (the live
   cascade lane). The baseline every challenger must beat — on the
   yardstick, not on elegance.
2. **GFlowNet** (trajectory-balance loss): `CascadeEnv` (state = pattern-set
   under budget + EOS; the library's Tetris/Scrabble shape) + `C`-proxy.
3. **Linear rollout** (existing `futon2.aif.rollout`, PUCT over the
   gradient prior) — the §5 rival, entered honestly with its linearization.
4. **Cheap stochastic baselines**: greedy-with-ε noise and
   random-under-budget. The null arm. If greedy-ε matches the GFlowNet, the
   heavy machinery is over-engineering — kill it (the doc's own
   kill-criterion discipline, applied to samplers).

The field is open: other methods (GA over pattern-sets, simulated annealing,
judge-panel assembly) may enter if cheap. Every entrant runs on the SAME
circumstance set and is judged by the SAME yardstick.

## The yardstick discipline (the Goodhart guard)

- **Generation may use `C`** (wholeness) as its proxy, with eyes open.
- **Judgment never uses `C`.** The contest is scored on the grounded
  yardstick: the realized-`G(π)` floor today; the peradam when
  M-peradam-grounding unescrows. Laundering `C`-scores into the verdict is
  the anti-laundering failure the alphazero doc forbids — a GFlowNet trained
  on raw `C` and judged on `C` would *institutionalize* Goodhart.
- **The falsification stands**: M-arguing-worlds showed
  diversity-of-candidates does NOT beat single-best on the realized-`G(π)`
  floor (`:single-best-holds`, 2eca617). This excursion does not re-run that
  hypothesis with fancier machinery. The questions are different:
  (a) does native semilattice sampling find cascades the linearizing methods
  structurally cannot? (b) is any sampler *trainable* from flight records
  (the R2 hook)? Diversity is measured and reported, never credited as value.

## Exit conditions

1. A circumstance set (≥ 10 circumstances drawn from live WM snapshots) and
   a frozen scoring harness: every entrant emits cascades; realized-`G`
   judged identically. The `C`-proxy for the GFlowNet is verified against
   the JVM scorer on N samples before any training run (mechanical check).
2. All four minimum contestants produce scored entries on the full
   circumstance set. Partial fields are reported as partial.
3. A verdict table: per-method realized-`G` (median + spread), wall-clock,
   and a structural column — how often the winner's cascade is NOT reachable
   by the linearizing methods (the §5 question made measurable).
4. The trainability probe: one trajectory-balance training run consuming the
   R11 projection of the existing flight corpus (backfilled + witness
   flights), reporting whether the trained policy's samples improve on its
   untrained samples. This exercises the M-first-flights exit-5 lane from
   the consumer side; a null result is a result.
5. A recommendation back to the parent: which machinery (if any) replaces or
   joins the cascade lane, with the kill-criterion verdicts recorded —
   written into `futonzero-alphazero.md` §5 as the tension's resolution or
   its honest survival.

## Scope out

- Training the production prior (FutonZero v2 / R2 owns that; we feed it
  evidence about WHICH sampler is trainable).
- The ground metric (M-substrate-metric); the contest runs on today's `C`
  and realized-`G` exactly as they stand.
- Any consent-gate or autonomy change; sampled cascades are CANDIDATES for
  the existing gated lanes, never actions.
- GPU/superpod runs. v0 is CPU-sized by design; scale-up is a follow-on
  decision taken on evidence.

## Checkpoint 0 — 2026-06-12
Chartered from the morning's review chain: satisficing review of
M-first-flights against futonzero-alphazero.md → three whistle amendments
(return-channel debt, R11, loop-closing exit-5) → Joe pointed at the
gflownet library → fit assessed (semilattice-native + trainable-prior;
diversity pitch explicitly NOT the warrant) → Joe: "this is exactly an
excursion of that shape," named with the deliberate doubling.
Next: owner assignment (Joe's call), then the harness spec (owner-side)
before any contestant code (bellable).
