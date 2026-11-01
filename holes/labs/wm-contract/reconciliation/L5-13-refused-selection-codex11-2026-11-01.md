# L5-13 — consistent post-score refusal projection

Base: eb3dc7982 (includes packet 20b979c61 and L5-11 merge).
Branch: codex11/l5-13. Worktree only.

The retained selection-outcome is authoritative. If it says refused, currentChoice
is not-recomputable with the refusal kind even when stale selected-target/cascade
fields survive in the section. Chosen and abstained records retain their prior
projection.

Decision-path absences are **zero** on a refusal: no decision was enacted and
population-wide alternatives are not an execution path. The refusal detail remains
in the immutable record; this does not erase or reinterpret that evidence.
Q5 is not-recomputable with "no interpretation in this run". The checker preserves
that reason and Q11's "selection refused: :pick-not-below-abstention" in its .nr
clauses. Lean Decision and InterpretationOrder were not changed; those cases
remain claude-2's specification work.

The runtime deposits computed learning status into refusal evidence immediately
after resolve-pending-learning returns, before scoring. Later evidence updates merge
rather than replace it. The refusal record therefore retains the same status/reason/
ending/gain/receipt fields as a chosen selection. No learning state or selection
rule was changed. The three learning statuses are regression-tested through the
actual ordinary runtime and immutable refusal writer, with a scoring throw.

## Validation

| Arm (projectors + runtime wiring) | Tests | Assertions | Failures | Errors |
|---|---:|---:|---:|---:|
| Pinned main | 95 | 349 | 5 | 0 |
| Branch | 97 | 372 | 5 | 0 |

All five failures are the identical existing
missing-task-selects-and-enters-existing-path assertions (lines 89–93).
New tests pass: SHA-pinned read-only click-12/13 records, identical decision
signature, reason-preserving checker output, learning projection when present,
and producer retention for none/applied/deferred.
Kondo zero errors/warnings; check-parens.el OK; diff check clean.
Hermetic roots/overrides used. Canonical data counts are 45938→45938 for each
arm; no hermeticity failure. Exit 1 is the existing assertion baseline.
No reload, merge, live click or production data write.

## Both historical exports and Lean

Read-only inputs:
- click 12: selection-record-2026-11-01-wm-click-878186ce-4e68-411b-8d2e-581c559b72cb.edn,
  SHA c92ed1991a46c00af10d4279135888af0703fa542c6570a3614b3db9799535c4.
- click 13: selection-record-2026-11-01-wm-click-28d27460-bc9d-44c1-9555-74b8b6931300.edn,
  SHA 0cca2d2a5a1d32fdd3e6db829586691622c98833721bfb9861cb7754e6cd9d02.

Full exports: /home/joe/notes/l5-13/click12.json and click13.json, using one
shared read-only snapshot. Both now export:
- currentChoice NR "selection refused: :pick-not-below-abstention";
- pathAbsenceCount 0;
- interpretationOrder NR "no interpretation in this run";
- selectionPath NR "no :outer/learning status on the selection".

Historical learning status was not fabricated or backfilled. Sources and other
retained evidence still differ between records; identical means refusal signature
and verdicts, not byte-identical full exports with invented matching provenance.

Checker-generated Lean was executed for each. The nine outcomes are identical:
Q1 violated; Q2 unverifiable; Q3 holds; Q4 holds; Q5 unverifiable;
Q6 unverifiable; Q7 unverifiable; Q9 violated; Q11 unverifiable.
The two Lean output logs compare byte-identical. This is consistent reporting,
not a conformance claim.

## Evidence hashes

Commands, scratch settings, exports and logs: /home/joe/notes/l5-13/.
- baseline.log: SHA256 03633727891705aae2c8e7dcf299759c9de776a52e89ab62ecbdedca33bf12e2
- final.log: SHA256 decfd9a2262fcc58c68fc1c703693c988f694cce3cf460c504c619816e6ac790
- click12.json: SHA256 4e83c62ffd5859ad25d3b00b98c2969268f4df4c0b8e349136c1d9b9f37ec30e
- click13.json: SHA256 3106c52fc086f2360610df1456e032d4c1a7c87a83d0625a4b47abe0c69379eb
- click12-lean.log: SHA256 edc3410f9db5b974d05de5b1d3904d4e4ae07bece20499ecb4dc1e2b0e93c2fb
- click13-lean.log: SHA256 edc3410f9db5b974d05de5b1d3904d4e4ae07bece20499ecb4dc1e2b0e93c2fb
