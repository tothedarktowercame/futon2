# A-next gold corpus — the canonical pin (10 `(cascade, sorry, wiring)` triples)

**Purpose.** The ground-truth corpus for **M-fold-ansatz phase-2** (`cascade → SORRY`) and the fair
target for **E-fold-embed-pipeline**. Each triple is a real mission's:
- **cascade** — its `applied` patterns (from the cascade-real graph);
- **sorry** — its typed interface (`:want-signature` + `:endpoints` over real substrate-2/code entities
  + `:typed-holes`), recovered **post-hoc from doc + code + git + XTDB** (Joe's A-next method);
- **wiring** — its CLean/DarkTower construction (`futon6/holes/clean/<m>.clean.edn`, 0-sorry-gated).

**Acceptance:** each sorry is recovered *independently* of its wiring, so `terminals = endpoints` is a
real cross-check. Two triples (#9 state-snapshot-witness, #8 single-entry-point) the independent recovery
**disagreed** with the wiring's optimistic discharge — that disagreement is signal, not noise.

**Honesty:** holes are graded by what actually discharged them — `:discharged-by <sha>` where built,
`:open`/`:unverified`/`:research` where not. Several missions are IDENTIFY/RESEARCH-stage; the corpus
reflects this rather than fabricating closure. This is *better* training/eval signal than uniform "closed".

## The 10 (canonical numbering)

| # | mission | canonical id | maturity | discharge summary | eps · holes |
|---|---|---|---|---|---|
| 1 | autoclock-in | futon3c-d/…/autoclock-in | CLOSED | discharged (durable lineage; the sealed blinded-study reference) | 7 · 3 |
| 2 | invariant-queue-unstuck | futon3c-d/…/invariant-queue-unstuck | CLOSED | discharged (4 meta-invariants: boundary/ratchet/canary/taps) | 7 · 4 |
| 3 | a-sorry-enterprise | futon5a-d/…/a-sorry-enterprise | IDENTIFY | **mostly open** — AIF loop specified; proximity scorer + closure posterior unbuilt | 7 · 4 |
| 4 | agency-rebuild | futon3-d/…/agency-rebuild | CLOSED (superseded) | discharged (A0–A5 green; 5 shas) | 8 · 5 |
| 5 | f6-ingest | futon3-d/…/f6-ingest | archived | discharged vs a **real run manifest** (ner 0.9958, scope 0.3769); 2 holes partial (moist-run) | 6 · 5 |
| 6 | pattern-ingest | futon3-d/…/pattern-ingest | IDENTIFY | 1/4 partial (1600 pattern-slot edges live); 3 open (pattern-origin/related-to = 0) | 6 · 4 |
| 7 | patterns-done-right | futon0-d/…/patterns-done-right | part-open | L1/L2 shipped; L3 research (category in a `(comment)`); L4 open (receipt code absent) | 7 · 4 |
| 8 | single-entry-point | futon3c-d/…/single-entry-point | resolved-A | discharged on live box (fork → in-JVM); Path-B orphan-teardown open | 9 · 3 |
| 9 | state-snapshot-witness | futon3c-d/…/state-snapshot-witness | built (linear) | discharged except tracer track-4-2 closure (`:unverified` — wiring over-claimed) | 7 · 4 |
| 10 | stepper-calibration | futon3c-d/…/stepper-calibration | in-progress | Class A (P1/P7) discharged RED; Class B spec-only; Class C open (needs superpod) | 6 · 3 |

Files: `futon2/holes/labs/A-next-<mission>/<mission>-sorry-EMPIRICAL.edn` · wirings:
`futon6/holes/clean/<mission>.clean.edn`.

## Provenance of the recovery (auditable)
- #1 sealed by claude-4 (2026-07-01, the template). #2 by the Claude owner (2026-07-01, pilot).
- #3–#10: 8 parallel recovery agents (2026-07-02), each independently reading doc+code+git+XTDB; every
  cited sha + live-XTDB count + filesystem claim spot-audited by the owner (author ≠ reviewer). All 10
  parse as EDN (`bb`).

## Supersedes
The earlier `futon6/data/fold-embed/pairs.jsonl` bundle (110 pairs) targeted a **coarse used-var proxy**
with **empty cascades** (a mission-id key mismatch) — it recovered the wrong object. This corpus is the
correct ground truth. See the E-fold-embed-pipeline STATUS UPDATE (2026-07-02).
