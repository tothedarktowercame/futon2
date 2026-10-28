From claude-11. Clojure task, one namespace + one test. Bell claude-11 back with a summary + the FULL commit sha (`git rev-parse HEAD`).

## Goal

Give `meta-self-heal-production/recover-unadmitted-transaction!` (your
`4968701cf` + `e8542f339`) a real medic authority, so the 2aef live recovery
can run. codex-11 held the live step at authority (read their note first:
`/home/joe/notes/wm-next/criteria-completeness/2aef-recovery/decision.md`):
`meta_medic_authority.clj` `resolve-context` (`:448`) and `verify` (`:425`)
accept only the historical-misclassification and genuine-repair grammars, and
the recovery tests use a local verifier, so nothing proves the recovery works
with the installed provider.

## Change (in `meta_medic_authority.clj`, following the genuine-repair grammar's shape, `:272-423`)

A third, recovery grammar, with its own schemas (intent, evaluation, review,
authorization, e.g. `:wm/meta-medic-transaction-recovery-intent-v1` …), whose
intent binds exactly what codex-11 listed:

- injury, repair, attempt;
- the current ledger root (entry id / pin of the latest transition for the injury);
- canonical transaction path and SHA-256;
- deferred transaction path and SHA-256;
- the ledger-admitted author receipt pin and patch.

`verify` dispatches on the intent schema as it does now; the existing two
grammars' behaviour must not change. The verifier, called with the recovery's
context (`:operation :current :candidate :evidence`, see
`meta_self_heal_production.clj` recover fn), returns true only if every bound
value equals the live candidate/evidence and the review and authorization
receipts are from trusted identities distinct from the intent's author, the
same rule the genuine-repair grammar uses. Ledger validation calls through the
same verifier must keep working exactly as before (codex-11's point: the
recovery fn calls the verifier twice for two purposes).

Make `recover-unadmitted-transaction!` hand the verifier enough to resolve
the recovery bundle the way `resolve-context` does for dispositions (decide
the smallest change; say what you chose). Do not edit
`meta_repair_coordinator.clj` or `meta_injury_ledger.clj`.

## Tests

Hermetic, using the real provider (`authority-verifier` / `resolve-context`
with a trust fixture as the genuine-repair tests do), not a local lambda:

- a complete recovery bundle for the 2aef-shaped fixture → archive succeeds, ledger bytes unchanged;
- each bound value altered in turn (canonical sha, deferred sha, attempt, author pin) → authority refused, nothing moved;
- reviewer identity = intent author → refused;
- an existing genuine-repair test still passes unchanged (name it).

## Gates

- `clj-kondo --lint`, `futon4/dev/check-parens.el` on changed files.
- The new test namespace, the existing medic-authority tests, and
  `unadmitted-transaction-recovery-test`. Skip long suites; say what you did not run.
- No reloads, clicks, authority installation or live-store edits. Commit only
  your files by explicit path (futon2 is shared).
