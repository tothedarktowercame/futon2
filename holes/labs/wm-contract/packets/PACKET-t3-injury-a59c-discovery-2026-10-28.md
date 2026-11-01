From claude-11. Discovery only: read code, run records, the injury ledger and retained receipts (read-only); no edits, no reloads, no clicks. Bell claude-11 back with a summary + the FULL commit sha (`git rev-parse HEAD`).

## Goal

Repair iteration 2 (codex-11, `/home/joe/notes/wm-next/criteria-completeness/repair-iteration-2/receipt.md`):
dedicated self-heal for open injury `injury/a59c85a6…` (T3, "exit and
stop-line completeness", `futon2/src/futon2/aif/tripwire.clj:95`; tripped at
initialization in historical run `2026-10-27-c9d132d8-a2be-42b3-8369-fd23e9b9924f`)
diagnosed successfully, but its proposed acceptance test passed 5 tests / 33
assertions before any repair, so the machine correctly refused
`:meta-self-heal/acceptance-test-passes-before-repair`. Find out which of two
things is true, with evidence.

## Questions

1. What exactly did T3 trip on in run c9d132d8 (record path, the tripwire's
   inputs and the failing condition, file:line of the check)?
2. Is that condition still producible by today's code? Reproduce it read-only
   by evaluating the T3 check on that run's retained inputs against today's
   functions (a REPL evaluation in a scratch JVM or test, not the serving JVM,
   and not a click). Cite which commit changed the behaviour if it no longer
   trips.
3. The diagnosis's proposed acceptance test (find it from the receipt /
   diagnosis file): does it exercise the T3 condition at all? Construct the
   failing case from the run's inputs and say whether that test would catch
   it.
4. Conclusion, one of:
   - (a) defect still present and the diagnosis's test is not discriminating:
     what a discriminating test is (inputs, expected red);
   - (b) defect already corrected: the commit, and the supported route by
     which the injury ledger accepts "already corrected" with evidence (cite
     the supported entry point in meta_self_heal / medic / acceptance code;
     do not invent one).

Output: `/home/joe/code/futon2/holes/labs/wm-contract/DISCOVERY-t3-injury-a59c-2026-10-28.md`;
commit only that file by explicit path.
