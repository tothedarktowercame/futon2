# Packet: retain the policy-precision update in the click record (2026-10-22)

From claude-2. Joe approved (2026-10-22) changing what clicks record so the R20
rows can be filled. codex-4's extraction
(`holes/labs/wm-contract/NOTE-r20-lean-catchup-2026-10-22-answers.md`) found both
recent clicks report the precision row as `:precision-update-not-retained`, carry `{}`.

## Goal (one behaviour)
Every click retains, at `[:selection-certificate :policy-precision-state :update]`,
the fields of Lean `PrecisionFromObservation.TickRecord`
(`mathlib4/DarkTower/WarMachine/PrecisionFromObservation.lean:251`):
per-policy E, G, F, prior, posterior, plus `:beta-prior`, `:beta` and the solve
result. The extractor `apparatus_certificates.clj/precision-row` already reads
`:beta-prior`, `:beta`, `:solve`; extend it to read the per-policy vectors
and emit a real row when all are present.

## Where to look
- `src/futon2/aif/policy.clj` ~592: `beta-state` is assoc'd as
  `:policy-precision-state`; find why `:update` is absent in live clicks
  (`data/wm-runs/tick-run-record-2026-10-21-752b48a7-*.edn`).
- `src/futon2/aif/wm/apparatus_certificates.clj` `precision-row`.
Touch only these precision paths; codex-4 is concurrently adding
`:focus-receipt` to the same selection certificate, so rebase before committing.

## Acceptance
- A decision built through the real policy path carries the update with all
  TickRecord fields, keyed per policy consistently across E/G/F/prior/posterior.
- `precision-row` yields a non-absent row for it; still typed-none when absent.
- Bad case: a record whose posterior is not the softmax at `:beta` of E,G,F
  (Lean `Conforms`) must be reported as non-conforming, not passed.
- If retention is impossible without a model change, stop and report why rather
  than synthesising values.

## Gates
clj-kondo, `futon4/dev/check-parens.el`, relevant futon2 tests. Ground rules in
`futon2/CLAUDE.md`: no serving-JVM load or click in this packet.

Bell claude-2 back with a summary + commit shas.
