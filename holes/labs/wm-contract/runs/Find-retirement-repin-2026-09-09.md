# Find retirement and re-pin — 2026-09-09

Marker mathlib4 **bf79f988b3**; emitted contract **8b38ceec46**.
The marker claims existence of a conformant implementation at the named
three-pattern instantiation (ab8e4b8762), with Item-20 statements 93ade5be78.
Opaque find and its laws are unchanged; no runtime correspondence is claimed.

The marker and contract require separate commits because the emitted contract
records the source commit. Both are the same source re-attestation slice, not
a relaxation of a population re-pin obligation.

## Regeneration and exact delta

Contract source SHA: bf79f988b3131701ff9ed257d371cc45dd5eea5b.
Contract SHA256: 333b31739f984d2a291da1ce0551ae219fcc1aa5fb4af4e5a414910b8f69bde9.
The audit's generator-owned :as-of remains **2026-09-08**, inherited from
accounting; this report's run date is September 9. Do not read that inherited
date as today's observation time or hand-edit it to appear fresh.

- Contract emission: two runs byte-identical.
- Variable situation accounting: two runs byte-identical; --check and
  --negative-untyped pass.
- Readiness axiom probe: two runs byte-identical; build :ok, zero local sorry,
  zero of six theorem axiom lists nonempty.
- Prereg transcription: two runs byte-identical. Only 00-source.edn and
  04-controls.edn change, each at the Holes byte hash. No assertion changed.
- Closability audit EDN/Markdown: two runs byte-identical. Find's
  pre-run-closable/not-ready row disappears from the OPEN audit. Total open
  rows 11→10; declaration open rows 10→9; pre-run-closable 4→3; run-gated stays
  7 (six declarations and one glossary-side row). Remaining typings hold;
  pointers shift to current source locations. wmRunConformsToWiring and
  enactedEqualsSelectedWhenRankOneGated remain run-gated.
- Readiness EDN/README snapshots re-emitted twice and byte-compared.

127 positive/guarded WarMachine modules build green, **8623 jobs**. The two
raw intentionally failing FoldC negative modules are not positive build targets;
their mutation-caused rejection was checked by the accepted two-axis wrapper,
not counted as successful raw elaboration. This marker edit does not change
their mathematical dependency declarations.

Fresh readiness output:

```
RUN4 certificates        GREEN
RUN4 definitions-intact  GREEN
RUN4 wiring-pin          GREEN
RUN4 run-pins            GREEN
RUN4 regenerates         GREEN
RUN4 lean-probe          GREEN
RUN4 hole-open           GREEN
RUN4 closability-audit   GREEN
RUN4 invalidators        GREEN
VERDICT: READY
```

## Publish gate: BLOCKED, not accepted

`bash build-p4ng.sh futon-2026` was actually run and exited **1**. Its first
blocking assertion is p4ng/empirics-futon/negative_controls.sh:80:

```
expected: ^Total & 10 & 61 & 53 & 0 &
observed: Total & 9 & 62 & 53 & 0 &
negative_controls: FAIL -- the declaration rung bands moved
```

The gate's own retained comment pins the September 7 F10 bands. Find's current
closure changes the generated bands; a reviewed count re-pin is needed. The
assertion was NOT weakened or edited. Full publish acceptance is therefore
outstanding even though the RUN4 meter reports READY. Later publish gates were
not reached; no claim is made that this is the only remaining publish blocker.
The generator output also reports stale Lean/Q-interface view pins; those are
reported warnings, not diagnosed here as the terminating assertion.

Build log: /tmp/find-retirement-p4ng-build.log. Positive Lean build log:
/tmp/find-retirement-final-build.log. The build regenerated p4ng views (some
were already dirty before invocation) and futon2 workflow-report.edn; those
are not included in this scoped artifact commit or reverted over other work.
No ledger, registry, frontier or run data edits. No live run.

One diagnostic git diff was issued from p4ng with a futon2 path and failed
outside-repository; the correctly located rerun supplied the audit delta above.
