# FLIGHTS — Batch 2 (six deposits, successor runner zai-2)

**Arming:** Joe (this session: batch of six approved; zai-2 created for the
series). Scope: artifact-only, no substrate-2 writes, no JVM, no production
edits; operator-owned forks escalate. Acceptance preregistered HERE before
execution; adjudicators mechanical. Failed flights are first-class. After all
six: adjudications recorded, seals opened for the batch, R̂ retrained, S2's
first row, batch-3 proposals generated under the retrained reward.

Grain discipline (batch-1 precedent): slice pass/fail per acceptance below;
want-grain labels recorded only where the want itself is adjudicable — the
futonzero pair is expected to yield want-grain negatives (gates measured
uncleared); slice-passes do NOT enter R̂ as want-grain positives.

## B2-F1 — ft-chipwitz-corps-013 (task-as-arrow / reverse-morphogenesis slice)
Build `determination_test.py` (futon3c/holes/labs/M-chipwitz-corps/): given a
choice point + candidate warrant, classify DETERMINED / CONSISTENT-NOT-DETERMINED
/ UNDETERMINED, with b3's three failure modes as named categories; heuristic
documented UNCALIBRATED. Run on Flight 3's pxr-log forks.
**Acceptance:** runs from any cwd; correctly separates the known cases —
fork C → DETERMINED, fork A → CONSISTENT-NOT-DETERMINED, fork B → UNDETERMINED;
classifications appended to pxr-log as typed records.

## B2-F2 — ft-chipwitz-corps-014 (task-shape-validation slice)
Build `choice_point_validator.py` (same lab): G5-style shape check for
choice-point records (required fields, option structure, context refs).
**Acceptance:** validates the real pxr-log entries green; rejects 2 inline
malformed variants; runs from any cwd.

## B2-F3 — ft-learning-loop-013 (evidence-channel invariants slice)
Extend Flight 1's contract artifacts (futon5a/holes/labs/M-learning-loop/):
add an evidence-channel invariants section (b1's A0–A5 mapped to the mining
PXR stream) and a source-noise-filter spec from b5's anti-patterns
(performatively-filled slots; empty-slot semantics). Extend the bb test with
2 new malformed variants exercising the new sections.
**Acceptance:** bb test green (old + new assertions) from any cwd; both new
sections present with per-invariant prose.

## B2-F4 — ft-learning-loop-014 (inhabitation gate slice)
Build `inhabitation_gate.py` (same lab): classify hinge-log entries
inhabited-voice vs consultant-voice per b2, heuristic documented UNCALIBRATED;
emit b3's typed events for the classified sample. Source: locate real
hinge-log entries (M-trip-journal apparatus / futon5a). **If no real hinge-log
source is found, STOP and report the blocker — do not synthesize entries.**
**Acceptance:** runs on ≥10 real entries; typed event log emitted
(check-parens); heuristic + threshold documented UNCALIBRATED.

## B2-F5 — ft-futonzero-generative-013 (advanceability triage slice)
Write `futonzero-advanceability-triage.edn`
(futon6/holes/labs/slush-demo-flights/): every open hole of the mission
classified per b5 — advanceable / 0-cycle-leaf blocked (G-SIM, G-REWARD) /
safe-product — with evidence refs incl. Flight 2's gap report.
**Acceptance:** all holes named in the mission doc + fold h1–h5 appear,
each classified with an evidence ref; check-parens.
**Want-grain:** expected `:discharged? false` (governance manages the blocked
state; it does not clear gates).

## B2-F6 — ft-futonzero-generative-014 (shi coordination slice)
Write `futonzero-sequencing-plan.edn` (same dir): the disciplined-army plan —
gate-work order, named owners (substrate-2/full-C → G-SIM;
M-pudding-peradams → G-REWARD), sequential restraint (what does NOT start
before what), kill criteria carried from the preregistration.
**Acceptance:** owners + order + restraints + kill criteria all present;
check-parens. **Want-grain:** expected `:discharged? false` (same gates).

## Execution
Serial on zai-2; claude-2 reviews each against its acceptance block verbatim
(re-run every claimed PASS, from a different cwd where executable);
fix-don't-re-bell. zai-2 inherits the full folding/flight contract:
resolver-blind (no *.SEALED.json), honest satiety + overlap conventions,
file-relative paths, artifact-only.
