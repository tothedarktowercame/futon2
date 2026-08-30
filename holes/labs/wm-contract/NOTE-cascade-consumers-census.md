# NOTE — who reads and writes Cascade? (census, 2026-08-31, claude-15; commissioned by Joe's I_evidence_consumed walkthrough)

## The PLoP loop's report-up slot, located
`p4ng/sec-overview-plop.tex:11-21`: six stages; **Learn** is the report-up: "uses the independently witnessed
outcomes to recalibrate later choices and, between runs, to revise the structures from which future candidates are
proposed." The helix Joe described IS Learn's between-runs half — the writeback to the pattern landscape.
What actually happened to it (all previously-gated findings, assembled here):
- Learn's INPUT (independently witnessed outcomes) stopped arriving 2026-07-06 13:04Z — no record carries
  `:enactment` after that (H1 correction, M-formal-war-machine §2).
- The nightly observer (R10) last ran 07-05; entrypoint no longer in the repo.
- The writeback instruments went dormant the same week: slush deposits 07-09/07-11; fold-gfn outputs 07-02/07-11.
So the loop's final handoff had no consumer AND no producer from early July: the helix flattened, then stopped.
The PLoP paper documents the cycle; nothing in it names WHO consumes Learn's output — I_evidence_consumed's
falsifier, at the paper's own top level.

## The cascade census — three notions, near-disjoint consumers
| cascade notion | written by | read by | outside its module? |
|---|---|---|---|
| **Lean/theory Cascade** (P-validated-R5 §3e; CascadeOrder.lean; `snatch-cascade.edn`) | the R5 lane; snatch experiments | `find_snatch.clj`, `playout_snatch.clj` only | **NO — unconsumed outside the microcosm** (Joe's suspicion confirmed) |
| **cascade-lane entries** (fold world) | `cascade_lane.clj` (from held-work ledger, diffsub-moves 06-09-stale, index) | `close_loop.clj` (PURE given an entry; act-gate `{:cascade-score :coverage-score-delta}`), `enact.clj` (apply-cascade! over `:shown`), fold_escrow/semilattice | yes — a real lifecycle, but its inputs include AUD-D1 class-(iv) stale artefacts |
| **strategy cascade** (`backlog-cascade-merged-v0.edn`, 07-13) | forward-model merge (07-13, dormant) | `war_machine.clj` cascade-role-map (display/policy lane) | yes, one reader, stale input |
| **cascades/ patterns** (5 flexiargs, 08-17) | authored 08-17 | minilm index (5/5 present → retrieval); spider wave 2 scope | yes — the landscape carries them; attestation pending |
| `cascade_prior.clj` | itself | **nobody** (one test provenance string) | **NO** |

## The point, in the precept's terms
The new Lean `Cascade` currently has the same defect the old loop died of: defined, unconsumed. The old system's
end-of-loop handoff (Learn → revise the pattern landscape) is not superseded yet — it is VACANT. What exists now
that did not in July: the landscape is live-indexed daily, the spider attests @why/@how with evidence rules and a
reflection-excluded corpus, and `I_evidence_consumed` names the requirement. The natural wiring (NOT commissioned;
Joe's call): R16's typed act-witness (already the top repair) feeds Learn; Learn's writeback = attested edits to
the pattern landscape (the spider verifying them); the Lean Cascade becomes the carrier close_loop/enact and the
WM's cascade-policies all read, replacing the three private notions. Then "who reads a cascade?" has one answer
per edge of the wiring diagram, and the loop's output has a consumer: the landscape, and the next run.
