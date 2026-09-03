# U46 — the pinned `find-snatch` record, transcribed

Producer: `holes/labs/wm-contract/u46_find_transcribe.bb` (run from the futon2
repo root; `bb holes/labs/wm-contract/u46_find_transcribe.bb`). Write-up:
`holes/labs/wm-contract/C500-find-rows-close.md`.

Read-only over futon3. No tick, no run lock, no substrate call, no network,
nothing written under `data/`. Deterministic: no artifact carries a wall-clock
field, and two runs over an unchanged fixture are byte-identical.

## Source

`futon3:checks/find-snatch.edn`, sha256
`839897ef8fe44952403700bd237389449ae4735d3da7df8239b1b94dc7ef4dfa`, `:as-of`
futon3 `2734ac570e` — 18 authored Snatch patterns, 6 scenarios, 34 recorded
rounds, drift mismatch count 0.

## Files

| file | what it holds |
|---|---|
| `00-source.edn` | the fixture's identity and the two grains, with the `find_snatch.clj` line ranges each corresponds to |
| `01-rows.edn` | the 34 round rows and 6 scenario rows as transcribed, in fixture order, still in EDN keywords |
| `02-predicates.edn` | F1–F4 evaluated over those rows by the producer, at the grain the check uses |
| `03-controls.edn` | C1–C6 (see below) |
| `lean-block.lean` | the generated Lean literal block, as inserted at `mathlib4 DarkTower/WarMachine/Holes.lean:284-693` |

## Result

F1 34/34, F2 34/34, F3 34/34 over the round rows; F4 6/6 over the scenario rows.
Each is proved in Lean by `decide`, no `sorry` — `wmFindSnatchF1Containment`,
`wmFindSnatchF2Receipted`, `wmFindSnatchF3NonSelfCertifying`,
`wmFindSnatchF4Falsifiable`.

## Controls

- **C1** the fixture's sha256 equals the value the four `Holes.lean` docstrings
  pin. PASS.
- **C2** every emitted constructor name decodes back to the fixture keyword it
  came from, the 18 names are distinct, and the decoded rows equal the fixture
  rows. PASS — this is what makes the literal a transcription rather than a
  retyping.
- **C3** a fabricated pattern occurs in no row, no repository and nowhere in the
  emitted Lean text. PASS.
- **C4** the four mutations `find_snatch.clj --negative-f1..f4` make are rejected
  by the producer's own F1–F4 evaluators. PASS (all four false). Without this,
  C2's round-trip would be a check that cannot fail.
- **C5** on this record `receipted` and `nonSelfCertifying` are the same set in
  all 34 rows (96 receipts, all `:structured-antecedent` with a warrant file), so
  F3 discriminates nothing F2 does not. Reported, not excused.
- **C6** the pin is a snapshot, not a reproduction: re-running `find_snatch.clj`
  today PASSES and rewrites the fixture to sha256 `08a0c3e7…` — identical
  `:scenarios`, `:laws` and `:drift`, a `:repository` grown 18 → 24. Measured
  once by hand and restored; the producer never invokes the check, because a
  producer that did would move the thing it transcribes. C500 §3.
