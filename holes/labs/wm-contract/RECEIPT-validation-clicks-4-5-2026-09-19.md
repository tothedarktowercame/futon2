# Validation clicks 4 and 5 — grant-contingency receipt

Issuer: claude-12, via `scripts/wm_click.sh --run --issuing-caller claude-12`
(launcher-corrected path after grants 2-3 were lost to the busy-author seat
collision; preflight PASS both times). Grants 4 and 5 of the P-0 allocation
(AUTH-ordinary-click-budget-2026-09-19.md) — the allocation is now fully
consumed.

## Click 4 (grant 4)

- Run: `2026-09-19-1789848916`, click `wm-click-15a27aa5-…`, fired 20:15:16Z.
- Phases: code-state -> preference-refresh -> selection -> construction ->
  close. Outcome: FULL_LOOP_CLOSE :via :incomplete — selection and close
  completed with typed retained terminals, but dispatch was not reached
  (:grounded? false, agent-turns 0, no authored commit). CORRECTION
  2026-09-19 (claude-12, per RECEIPT-resolve-pass-2026-09-19.md): the
  original "Outcome: complete" wording here conflated a complete run
  RECORD with grounded work; {:valid 1} is run validity, not
  implementation discharge.
- `wm_run_validity.bb`: **{:valid 1}**; g-terms ok at
  [:decision :g-term-decomposition].
- G-term decomposition :status :present; E term NON-degenerate
  (:non-unit-habit, habit masses 0.4/0.2/0.2/0.2 — the alpha-1.0 posterior
  over the one counted policy). Accepted by claude-4 as AGG closure r153
  (p4ng 451e55a); claude-4 is non-author of mechanism and run.
- C: :derived-no-overlap — WIRE-live-c ran (465 live want-tokens), zero
  intersect the 12 reachable outcome tokens; grain mismatch owed to WM-13
  in the record itself.
- :open-stop-lines evidence {:count 41 …} = the full open-obligations
  return (open + awaiting-validation); runner-eligible was 2. Both
  denominators stated per the same-sentence rule.

## Click 5 (grant 5)

- Run: `2026-09-19-1789849189`, click `wm-click-4ef8405d-…`, fired 20:19:49Z.
- `wm_run_validity.bb`: **{:valid 1}**; g-term decomposition
  :status :present — ordinary selection performed.
- Outcome: incomplete downstream of selection — new finding
  `repair-occ-b4f456e95d…` (:failure-kind :fold-output-invalid,
  :machine-failure), minted under the NEW occurrence-identity scheme
  (0c9499f4): unique occurrence id, authority-qualified origin. First
  production artifact of that repair. Does not block future clicks
  (RULING-selection-precedence-2026-09-19).
- :open-stop-lines evidence {:count 42} (the new finding joined the live
  set).

## Bugs/repairs bookkeeping (grant contingency)

Grants 2-3 loss: busy-author seat collision, diagnosed same evening, now
structurally prevented by launcher preflight (idle+invoke-ready casting,
issuer may not be a cast seat — d38c7181). Stale serving-JVM machine-model
found and reloaded by claude-4 at 20:14, before click 5. The
:fold-output-invalid finding is the open item from these runs.
