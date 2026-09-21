# Renewal: ordinary WM clicks, fourth budget of five — Joe, 2026-09-21

Recorded by claude-4 on exhaustion of the third budget authorized at
`AUTH-ordinary-click-budget-renewal-2-2026-09-20.md` @ futon2 `fa49ed93`.

## Provenance of this grant — read this before citing it

**I did not hear this grant from Joe directly.** It reached me as a bell from
claude-12 relaying Joe's words. That is a weaker record than the three grants
before it, each of which claude-4 took from Joe in the operator buffer, and the
difference is recorded here rather than smoothed away.

Joe's words, as claude-12 quoted them verbatim:

> we can have a block of 5 and 2 can be used now.

and, in a second bell shortly after:

> [the remaining 3] are for agents to coordinate and can be used as preferred

claude-12 attached a reading to the first: spend 2 now, hold 3 in reserve
pending a further word. The second bell is that further word — it releases the
reserve to fleet discretion rather than reserving it to a further operator ask.

If Joe reads this and the relay garbled his intent, the correction is his to
make and this document is the thing to correct.

## What the third five bought

All five were spent. Three of them bought defect diagnoses rather than
certified numbers, which is what the record shows and what the paper must say:

| click | outcome |
|---|---|
| `wm-click-456d1ab8` | run record `2026-09-20-...`; opened a `:C1` `:fold-output-invalid` finding |
| `wm-click-67d011a3` | T8 repair deferral — no cascade selection; the deferral was legitimate |
| `wm-click-3a65f8db` | run `2026-09-20-1789940260`, `:incommensurable-family`. Fixed by futon2 `a38becc9` |
| `wm-click-7fc2cc7f` | run `2026-09-20-1789948650`, same refusal — the serving JVM had never loaded `a38becc9` |
| `wm-click-4890d773` | run `2026-09-21-1789948972`, `:initialization-failed`, `Invalid token: :hole/2f9b03b16170`. Fixed by futon2 `29fb2a83` |

Three distinct defects, each found only by spending a click, each fixed and
regression-pinned. The C certification those five were allocated for
(in-domain 3 → 96, corpus weight 0.4729% → 4.1652%) **remains uncertified**.
The counted figure now stands at 113 in-domain of 468 entries, measured through
the production assembly in the serving JVM; a tick either promotes that number
or corrects it.

## What this fourth budget is for

The demonstration the machine has never produced: **one loop closed end to
end.** Every stage restates validly in isolation; no single tick has carried C
through selection, dispatch, verified completion, carry admission and
conditioning.

- **Tick N** — the zero-rate C certify run. Read it for C's certified number,
  the selection, the dispatch, and verified completion. A box is **not**
  expected at N: the realizer witnesses only artifacts that exist at fold time.
- **Tick N+1** — carry admission of the enacted predecessor, conditioning, and
  the box if the authored artifact matches the declared outputs.
- **The remaining three** are at fleet discretion per Joe's second word, to be
  coordinated between claude-4 and claude-12 — a retry under the stop
  discipline, or a rated-run attempt should the `:measured` provenance question
  be ruled on.

None of this needs the three rated-run prerequisites (`76a84bf0`).

## Operationally

- **Budget: 5 ordinary clicks**, allocated 2026-09-21 on this record. This
  document is the authorization each of those clicks cites.
- **Renewal remains Joe's** on exhaustion.
- **The record-keeping contingency carries over**: run records, and bug/repair
  records, for every budgeted click. A click leaving no durable run record
  consumed the budget without honoring the contingency, and that is to be said
  plainly rather than smoothed.
- **Fleet stop discipline**, adopted by claude-4 and claude-12 as our own rule
  and not imposed by Joe: fire only on a clean un-bypassed preflight; a NEW
  obstruction is diagnosed and retried within budget; the SAME obstruction
  twice means stop and diagnose rather than spend.
- Consumption is counted by `futon3c.wm.ordinary-click-budget/consume!` against
  this document's identity, so the first three grants' fifteen clicks are
  neither double-counted nor erased.

## A defect this renewal exposed, unfixed

The three most recent entries in `data/wm-ordinary-clicks/consumption.jsonl`
record `caller-unknown` rather than a cast seat, and `wm_click.sh` preflight
reports the same. The ledger's attribution is degrading while its count stays
exact. Not fixed tonight; recorded so it is not discovered twice.
