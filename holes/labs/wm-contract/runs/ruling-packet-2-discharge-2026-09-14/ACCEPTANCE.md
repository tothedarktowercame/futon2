# Acceptance — ruling packet 2: successor discharge + readback (8f5bea11, receipt 322a9d0c)

Reviewer: claude-15, per the coding-handoff protocol. Author: codex-24
(job invoke-1789426402247-20904-237755dc).

VERDICT: **ACCEPTED**, with the packet-1 carry-over item completed
in-lane by me (eec07f38).

What I checked (diffs read; receipt validated, not re-run):

- `successor-resolution!` validates the retained R and S closes with
  STRICT key sets (a delivery-projection-pointer evidence set cannot
  even parse as a successor close — the projection refusal is structural,
  and tested), requires grounded? on both, production-shaped? +
  resolved?/dial-moved? witness on S, and an explicit authority
  (decided-by + review-job).
- The T/R/S relation is explicit: :wm/repair-successor-relation-v1 with
  exact attempt/run ids, closed-at instants, R's commit + review receipt
  ids + review sha256, S's witness ref + sha256 — embedded in the
  resolution via the EXISTING writer (injected resolve-fn; the finding
  is never touched). A pre-existing resolution returns a stable
  :already-resolved no-op — never a second write.
- All ruled negative controls present as typed refusals with tests:
  :successor-missing, :successor-lineage-mismatch (borrowed),
  :self-successor (attempt OR run identity), :successor-not-later
  (temporal order), :repair/successor-evidence-invalid (strict shapes,
  covers the projection pointer), :resolution-authority-invalid.
- `repair-derived-state` is pure and cutoff-aware: a resolution at/after
  the cutoff is EXCLUDED, derived-status stays :open, and :derived-from
  names exactly which immutable records ground the state — the ruling's
  finding+resolution relation made explicit.
- Receipt 322a9d0c: single run, kondo 0/0, parens PASS, fresh-JVM 185
  tests / 985 assertions, both namespaces.

Carry-over completed in-lane (recorded in packet-1's acceptance as a
packet-2 check; codex-24 did not add it, so I did): the close-field
threading of :standing-readback is now pinned END-TO-END —
`measured-close-persists-standing-readback` drives a full measured
grounded close and asserts the field in the written 007. Doing so
exposed and fixed a second dropped-thread: the readback reached close!'s
DATA map but the judgment assembly's select-keys dropped it; the 007
payload now carries it (eec07f38). Investigation by-product, recorded
for a future cleanup packet: the retention-fixture family has closed
:build-failed under the T13 repo-consistency check since 48dfa037
(synthetic observer :repo "/repo" vs temp-root builds; ground-fn
:before nil also refuses grounded-change) — its tests pass because they
assert retention properties only, an authored-constants-shaped hazard in
fixture form.

Final gates: 186 tests / 987 assertions across both namespaces, 0/0;
parens OK; kondo 0/0.
