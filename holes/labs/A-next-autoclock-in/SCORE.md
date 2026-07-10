# A-next blinded study — SCORE (autoclock-in, 2026-07-01)

**Question:** how much of the mission's *interface* (the empirical sorry) does the **cascade alone**
determine? Blinded agent reconstructed `…-CASCADE.edn` from only the 10 cascade patterns; scored here
against the sealed `…-EMPIRICAL.edn` (recovered from doc + code + git + XTDB).

## Facet-by-facet

| facet | empirical (ground truth) | blinded (from cascade) | recovery |
|---|---|---|---|
| **codomain / want-signature** | `→ DurableLineage` (durable · **witnessed** · **single-active** · **reconstitutable-after-teardown**) | `→ DurableClockInRecord` (durable · witnessed · per-id · timeline-linked) | **~60% — SHAPE yes, READ-side no.** Got durable+witnessed+per-id; **missed reconstitution** (the mission's actual point) and **single-active** |
| **:have endpoints** | agent nodes · **canonical `<repo>-d/mission/<id>` nodes** · `clock/clocked-on` · `clock_store` ns · repl surface · `held/on-mission` | durable store · per-id store · prior-manual-clock (to repair) · cycle-boundary · schedule | **~50%.** Recovered durable-store + per-id-store + prior-manual-state; **missed the canonical mission nodes**, held/on-mission; endpoints left `:in-map :unconfirmed` (blinding) |
| **:want endpoint** | the `reconstitute` output (survives teardown) | a durable per-id `:clock-in` record | **~40%.** Got the record; **missed the reconstitution/read codomain** |
| **typed-holes** | order-independent-retract (Gu) · canonical-identity · repl-sync | repair-of-drift ✓ · shape-not-instance ~ · trigger-schedule ~ (+5 over-generated) | **SHAPE ~70%, CONTENT ~15%.** `hexagram-18-gu` correctly PREDICTED a repair-hole exists — but the *specific* repair (the order-independent-retract bug) is a **code-level fact absent from the cascade** |

## Headline finding (the real result)

**The cascade determines the interface's TYPE/SHAPE, but under-determines its grounded ENDPOINTS and the
CONTENT of its holes.** Concretely:
- **Recovered from the cascade (~strong):** the *kind* of thing being built — a durable, witnessed,
  per-id record with a **repair** step and a schedule/trigger. Remarkably, `hexagram-18-gu` in the
  cascade correctly signalled *"there is a repair here"* without any code access.
- **NOT recovered (~weak):** (a) the **reconstitution** codomain and **single-active** invariant (the
  read-side / the mission's point); (b) the **canonical mission-node** endpoints (a substrate-2 fact);
  (c) the **specific content** of the holes (the order-independent-retract bug — a code fact).

## What this means for the ansatz (probe (a))

- **cascade→sorry is partially embeddable**: it recovers the interface *type* well, so an embedding
  step *can* propose the sorry's shape. But the **endpoint-grounding** (which real substrate-2 nodes)
  and **hole-content** need the *circumstance* (code/substrate), not the cascade — so the fold genuinely
  needs `(cascade, circumstance)`, exactly as the interface signature says.
- This is a **clean, honest result either way**: it tells us *where* embedding helps (shape) and where
  it can't (grounding) — which sharpens B2 into "does embedding recover the grounded endpoints, given
  the cascade *and* the substrate-2 neighbourhood?" rather than cascade-alone.
- n=1; the post-hoc method scales (every done mission = a free (empirical-sorry, cascade) pair), so this
  becomes a real recovery-rate curve over the corpus.
