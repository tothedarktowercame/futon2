# Independent review acceptance — measured-A validator

Reviewer: claude-15. This file makes durable the acceptances previously
recorded only in the operator conversation (gap surfaced by the
outstanding-dag-2026-09-14 discovery, frontier item 1).

## Review 1 (2026-09-14): validator, tests, spec — ACCEPTED

Scope: codex-23 commits 0bac9adf, 55614bc5, 3cea9414, 0fac3d72.
Checked: all four diffs read; receipts validated (not re-run) — 4
tests/17 assertions exit 0 pinned to tree 3cea9414, clj-kondo 0/0,
check-parens OK, two failed attempts retained with stderr matching the
fix commits. Validator verified clause-by-clause against the codex-26
lead disposition's three prerequisites (rubric binding + ambiguity/
conflict/insufficiency refusals + prohibited label sources; full
temporal ordering incl. retrospective provenance; observer!=reviewer
with recomputed byte-hashes and subject-bound acceptance). The strong
rewritten-label control (consistent rewrite still refused on subject
mismatch) verified in the test. Rubric criteria properly OPEN; fixture
scoped :isolated-test; missing authority is a typed absence.
Reviewer fix applied by claude-15: 2fb99821 (drift guard pinning
status-support to futon2.aif.belief/status-set; 5 tests/18 assertions
green).

## Review 2 (2026-09-14): four refusal-branch tests — ACCEPTED

Scope: codex-23 commits 626fd97c, 25710871, fecb12fa.
Checked: diffs touch only the test namespace and additive followup-*
receipts (original receipts intact); followup receipt validated — 5
tests/22 assertions exit 0 at tree 25710871, kondo 0/0 (attempt-1
failure retained), parens OK. The update-acceptance helper correctly
rebuilds artifact bytes+hash so :review-before-annotation and
:acceptance-identity-mismatch fire as the targeted refusals rather
than upstream artifact checks; :retrospective-cutoff-mismatch uses a
well-shaped retrospective block so the mismatch, not a shape refusal,
fires. Every implemented refusal branch now has an assertion naming
its exact reason keyword. No findings.
