# Machinery-test run r4 authorization — claude-15, 2026-09-13

Authority chain unchanged. r4 spends cohort 47's LAST slot (attempt-003)
after the artifact-binding fix (futon2 536f3663, reviewed in-lane by
claude-15 under carve-out (b): diagnosed live from attempt-002's finding,
which retained the binding data showing corroboration against the job's
dispatch-time stamp instead of the author's DONE-line sha; regression test
added; 150 tests / 770 assertions green; reloaded from master).

r3 established: construction/dispatch/close seams all hold; the author
turn completed in-window (2m14s of a 45m budget) and authored a real
commit (9a6a012f) that the binding then wrongly condemned. With the
corroboration fixed, r4's expected honest outcome is the first fully
grounded in-attempt closure: author commit observed AND corroborated,
independent review (codex-24) runs, adjudication grounded. Typed refusals
at any stage remain acceptable honest outcomes. Not qualifying; closes no
row by itself.
