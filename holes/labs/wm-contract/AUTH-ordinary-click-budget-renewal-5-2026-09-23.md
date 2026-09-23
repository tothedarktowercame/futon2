# Renewal: ordinary WM clicks, sixth budget of five — Joe, 2026-09-23

Recorded by claude-5 on exhaustion of the fifth budget authorized at
`AUTH-ordinary-click-budget-renewal-4-2026-09-21.md` @ futon2 `00e7f9d6`.

## Provenance

Heard from Joe directly, in claude-5's operator buffer (emacs-repl), in answer
to a report that the ⟨1⟩5 re-fire was refused with
`ordinary-click-budget-exhausted` (5 of 5 consumed):

> I award five more clicks, please keep following the plan step by step with
> dispatches to zai.

No further conditions were stated.

## What this budget is for

Executing `PROOF-wm-works-2026-09-22.md`, which Joe signed. Steps ⟨1⟩1–⟨1⟩4 are
proved; ⟨1⟩5 — one live click on the reference target, decided by G — is the
step these clicks are for, and ⟨1⟩6–⟨1⟩8 judge that same click's close, its
measurement and the B update.

The previous budget's last click (`wm-click-41d88e5d`, 2026-09-22) ran end to
end and closed `:grounded-change`, and its certificate records Joe's class
preference {focused 11/20, related 7/20, unrelated 1/20, stop-the-line 1/20} as
what the live decision consumed. It failed ⟨1⟩5's check on two counts, both
since fixed, reviewed and reloaded: the reference ticket was withheld before
selection by the repair-supply gate (futon2 `d6565a01` narrows it to generated
and sourceless repair targets), and the accepted-increment predicate threw on
the recorded token-row shape (futon2 `74dc5de1`, with verbatim fixtures).

Seats: `wm-author` (author) and `wm-reviewer` (reviewer). `wm-repair-reviewer`
is off the roster, so `codex-13` stands in as repair reviewer until that seat
is restored.

## Operationally

- **Budget: 5 ordinary clicks**, allocated 2026-09-23 on this record.
- **Renewal remains Joe's** on exhaustion.
- **Record-keeping contingency carries over**: a durable run record, and
  bug/repair records, for every budgeted click.
- **Stop discipline carries over**: fire only on a clean, un-bypassed
  preflight; a new obstruction is diagnosed and retried within budget; the
  same obstruction twice means stop and diagnose.
- Consumption is counted by `futon3c.wm.ordinary-click-budget/consume!`
  against this document's identity.
- Still unfixed from the previous renewals: ledger entries record
  `caller-unknown` instead of the calling seat.
