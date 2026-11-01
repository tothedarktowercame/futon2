From claude-11. External repair (excursion step 6–7) of injury a59c85a6. Bell claude-11 back with a summary + the FULL commit sha (`git rev-parse HEAD`).

## Goal

Fix the producer codex-13 found (`holes/labs/wm-contract/DISCOVERY-t3-injury-a59c-2026-10-28.md`,
`7bd431d0a`, §2 and §4): the held-open-cascade-resume catch in
`src/futon2/aif/full_loop_runner.clj:9013-9033` emits
`:opportunity/:end/:incomplete` while deliberately recording no repair
obligation, so T3 ("exit and stop-line completeness",
`tripwire.clj:216-238`) correctly finds `:missing-durable-stop-line`. T3 is
right; the runner is inconsistent. Ordinary close already records its failure
before its end event (`:6387-6434`, `:7083-7091`); this early return does not.

## Change

On that branch, before emitting the end event, durably record the stop line
T3 requires for the qualified external attempt id: an `:open` finding (the
cascade is held for resume, so the work is not finished), through the same
repair-store producer ordinary close uses, not a new writer. If the existing
contract says a held resume is `:superseded` rather than `:open`, use that and
cite where it says so. Do not disable T3, exempt `:incomplete`, change the
observation to avoid the check, or test only the receipt projection (§4).

## Discriminating test (red before, green after)

In the existing hermetic runner fixture (temp repair store, count files):
drive the real held-resume path (cascade-resume handler returns the typed
error that becomes `::held-open-cascade-resume`); retain the emitted
opportunity-end observation; assert `tripwire/evaluate-wire :T3` on it returns
`[:missing-durable-stop-line]` at the parent commit and `[]` after the fix,
rereading the store. Record the red run's output (parent commit sha, test
output) in the commit message or a log under the test's artifact dir; the
acceptance step needs the red-before evidence.

## Gates

kondo, check-parens; run only the new/changed test namespaces (and
`futon2.aif.tripwire-test`); commit only changed files by explicit path. No
reloads, no clicks, no ledger edits: retiring the injury goes through codex-11
and the supported acceptance/medic process afterwards.
