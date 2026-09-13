# Vocabulary authority and measured-A domain finding

Read-only discovery by codex-26, 2026-09-13. Exact hashes in
vocabulary-authority-pins.json. No replay or measurement run.

Located authority: futon3/library/structure/interest-event-vocabulary.flexiarg.
It explicitly declares itself authoritative and any EDN schema a procedural
adaptation. The missing futon5a/data/interest-event-vocabulary.edn has no tracked
path or path history in the current futon5a checkout (git ls-files and git log
for that exact path returned empty); this does not assert absence in all history
or elsewhere. The existing standing projection is dated 2026-05-29, not a fresh
successful replay.

The vocabulary defines seven state transition EVENT TYPES and link/asserted.
state/reopened resets STANDING to live. Its payload distinguishes prior-state,
posterior-state and event/type. It requires operator-authored checkpoint events,
append-only corrections, evidence references and blocking validation. The source
checkpoint inspected previously explicitly labels rationale as paraphrased; that
is not itself evidence the operator-asserted boundary was independently verified.

WM belief/status-set has seven labels including reopened, excluding live.
Therefore event type and replayed standing are not interchangeable categories.
Simply stripping state/ from the last event, or reading posterior-state as the
WM true-status label, makes an unproved semantic substitution in measured A.

Lead implementation disposition: retain typed absence for measured-A categorical
state until the exact observed variable and source authority are specified and
validated against the seven-state domain. Preserve event type, standing, entity,
checkpoint/time and provenance as separate fields in any future reader. Do not
map live to reopened or invent a nearest category. The next bounded Row14 packet
must reconcile this domain distinction against the ruled measurement contract;
if the contract needs an explicit event-vs-state repair, record that repair
before code emits qualifying counts. This adds no new operator question and
waives no requirement. No measured cells, coverage or row admission established.
