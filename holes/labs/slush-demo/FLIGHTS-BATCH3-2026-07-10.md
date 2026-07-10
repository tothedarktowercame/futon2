# FLIGHTS — Batch 3 (six deposits, runner zai-3)

**Arming:** Joe (zai-3 created for the third batch; same scope as batches 1–2:
artifact-only, no substrate-2, no JVM, no production edits, operator forks
escalate). Acceptance preregistered here before execution; adjudicators
mechanical; RUNNER-CONTRACT.md in force (exact gate invocations, decoded-value
psi-sha convention, stop-on-blocker).

**These missions have DISCHARGEABLE wants** — unlike futonzero, slice passes
here can graduate to want-grain adjudications where the mission's own
completion criteria are met. Grain recorded per flight as before.

## B3-F1 — ft-canon-fingerprint-store-002 (schema + jiji idempotence slice)
Build in `futon6/holes/labs/M-canon-fingerprint-store/` (create):
`canon-store-schema.sql` (CanonFingerprint + CanonAggregate per the mission's
resolved decisions: Billey-Tenner shape, frequency-ordered, scope bindings;
evidence fields NOT NULL) + `smoke_canon_store.py` (stdlib sqlite3): create
store from schema, insert ≥5 fixture fingerprints, run the MAP-REDUCE
aggregation TWICE — the jiji vigilance check: second run must produce a
byte-identical aggregate (idempotent re-run).
**Acceptance:** script runs from any cwd, exit 0; re-run aggregate hash
identical (print both); NULL-evidence insert REJECTED (show the error).

## B3-F2 — ft-canon-fingerprint-store-003 (evidence-ledger integrity slice)
Build `ledger_check.py` (same lab dir): validates fingerprint records cite
paper_id + strategy + strategy_anchor (non-blank); run against B3-F1's fixture
store; include 2 inline malformed records that must be REJECTED with named
errors.
**Acceptance:** real fixtures green; both malformed rejected; any cwd; exit 0.

## B3-F3 — ft-autoclock-in-002 (reclock decision function slice)
Build `reclock_decision.py` + `test_reclock_decision.py` (futon3c lab dir
`M-autoclock-in/`, create): the pure decision function (save-event timeline →
promote/hold) implementing N-saves-within-W with dominance + hysteresis;
thresholds explicit, documented UNCALIBRATED v0. Tests must cover the
mission's named calibration risks: burst-thrash (guai overshoot) and
drift-under-gentleness (xun) — one test per risk, plus the no-op trap (firing
without state change = hold).
**Acceptance:** tests green from any cwd (paste summary line); thresholds
documented UNCALIBRATED.

## B3-F4 — ft-autoclock-in-003 (warrant table as data slice)
Extract `warrant-table.edn` (same lab dir): the three landed auto-clock rules
(explicit-token, creation, edit-activity) as inspectable data — per rule:
condition, warrant (WHY), success-witness shape, and the SILENT-FORK field
(what undecidable case this rule can hit and currently swallows — run-13's
finding made data). Source: the mission doc + the landed elisp
(read-only; locate it — STOP if not found, do not reconstruct from memory).
Plus `validate_warrant_table.py`: shape check + 1 malformed inline fixture.
**Acceptance:** table covers all 3 rules with all 4 fields; validator green
from any cwd; check-parens OK line pasted.

## B3-F5 — ft-state-snapshot-witness-002 (family schema slice)
Write `family-schema.edn` (futon3c lab dir `M-state-snapshot-witness/`,
create): the four siblings (inventory, registry, repo-refs, hud-render) ×
four components (container, projection-fn, cadence, emit-fn) — landed sibling
filled from the real landed work (locate it; STOP if not found), the three
Codex siblings declared with their per-container holes explicit (run-14 h1
made data, not guessed). Plus `validate_family_schema.py` with 1 malformed
fixture.
**Acceptance:** 4×4 grid present, landed sibling's entries cite real paths,
unfilled cells are explicit :hole entries (never invented); validator green;
check-parens OK pasted.

## B3-F6 — ft-state-snapshot-witness-003 (replay derivation slice)
Build `replay_state.py` + `test_replay_state.py` (same lab dir): active state
derived from an append-only event log by replay (b6 fact-lifecycle + b5
tri-store facts layer) — given fixture snapshot events, reconstruct "state as
of event N"; duplicates rejected (b4 append-only-audit); state never stored,
always derived.
**Acceptance:** tests green from any cwd incl. a duplicate-rejection test and
an as-of-N reconstruction test (run-14 h4's missing VIEW, minimally realized).

## Execution
Serial on zai-3; claude-2 reviews each against its acceptance block verbatim
(every claimed gate re-run from a different cwd). Fix-don't-re-bell. After all
six: adjudication (want-grain where the mission's own criteria are met),
unsealing, R̂ retrain, scoreboard, AND THE LEAVE-ONE-MISSION-OUT TRANSFER
RECHECK — the number this batch exists to move.
