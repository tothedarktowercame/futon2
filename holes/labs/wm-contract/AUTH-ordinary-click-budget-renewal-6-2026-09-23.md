# Renewal: ordinary WM clicks, seventh budget — five new plus two carried, Joe, 2026-09-23

Recorded by claude-5 while the sixth budget
(`AUTH-ordinary-click-budget-renewal-5-2026-09-23.md` @ futon2 `c24c903b`) still
had two clicks unspent.

## Provenance

Heard from Joe directly, in claude-5's operator buffer (emacs-repl):

> You're welcome to add 5 more clicks to the budget, bringing it to 8 total,
> and please carry on with the repair work

No further conditions were stated.

## The arithmetic, stated plainly

Joe's "8 total" follows from an earlier report of mine that said three of the
sixth budget's five clicks remained unspent. The consumption ledger says three
were **spent** (`wm-click-e3e4479c`, `wm-click-8b290466`, `wm-click-b765f91f`),
so **two** remained, not three. Five new plus two carried is **seven**, not
eight.

This record allocates **seven**, which is what "add 5 more clicks" comes to on
the real ledger. The eighth is Joe's to add with a word; it is not taken here
on the strength of an arithmetic that came from my own miscount.

Consumption is counted against *this* map's identity, so the two carried
clicks are carried by raising `allocated` to 7 rather than by leaving the
previous authorization in force — switching authority without carrying would
have silently forfeited them.

## What this budget is for

The repair work `PROOF-wm-works-2026-09-22.md` ⟨1⟩6 is blocked on: one accepted
close on the reference occurrence
`T-repair-occ-444fb018cbbb656d09b8f4f67c063f1d51a1932a9b1c281d999c567cf22a2ade`.

Three of the sixth budget's clicks went to that occurrence and each stopped at
the same conjunct — (c) `:acceptance-not-observed`, the ticket's Status still
OPEN — because the restore cascade never advanced past its own first limb. The
selection record already names the step the machine would take
(`:enacted-steps {:aif/declare-the-conditioning :aif/measurement-window-hygiene}`,
machinery-73 attempt-001), while the dispatch's acceptance criterion and the
predicate's conjunct (b) both read `precedence 0` — the chain head, whose
effect `:repair/split-declared-valid` has held since commit `0798f96a`. So the
author is asked to re-produce a token that is already true, (b) passes
vacuously, and the chain stands still. That defect is what these clicks are
spent on fixing and then exercising, one enacted limb per click:
`:aif/measurement-window-hygiene`, `:aif/two-layer-calibration`, then the
ticket's own acceptance.

Seats: `wm-author` (author) and `wm-reviewer` (reviewer); `codex-13` stands in
as repair reviewer while `wm-repair-reviewer` is off the roster.

## Operationally

- **Budget: 7 ordinary clicks** (5 newly awarded + 2 carried), allocated
  2026-09-23 on this record.
- **Renewal remains Joe's** on exhaustion.
- **Record-keeping contingency carries over**: a durable run record, and
  bug/repair records, for every budgeted click.
- **Stop discipline carries over**: fire only on a clean, un-bypassed
  preflight; a new obstruction is diagnosed and retried within budget; the
  same obstruction twice means stop and diagnose. The `:acceptance-not-observed`
  stop has now been seen three times, so no further click is fired on this
  occurrence until the enacted-step defect above is fixed, reviewed and
  reloaded.
- Consumption is counted by `futon3c.wm.ordinary-click-budget/consume!`
  against this document's identity.
- Still unfixed from the previous renewals: ledger entries record
  `caller-unknown` instead of the calling seat.
