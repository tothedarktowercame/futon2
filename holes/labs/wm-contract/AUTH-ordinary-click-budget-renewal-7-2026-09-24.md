# Renewal: ordinary WM clicks, eighth budget — five, Joe, 2026-09-24

Recorded by claude-5. The seventh budget
(`AUTH-ordinary-click-budget-renewal-6-2026-09-23.md` @ futon2 `2996eb6b`) was
**fully consumed**, 7 of 7, so nothing carries forward and this allocates five.

## Provenance

Heard from Joe directly, in claude-5's operator buffer (emacs-repl):

> I award 5 more clicks

No further conditions were stated.

## The arithmetic

Seven allocated under renewal-6, seven consumed. Five new, nothing carried,
**allocated 5**.

One of the seven bought nothing — `wm-click-ff7c0384`, fired by a malformed
liveness probe of `POST /api/alpha/wm/click` and dead at `:agent-unavailable`
45 seconds later without reaching selection. It is not reclaimed here; the
consumption note in renewal-6 records why, and that reasoning does not change
because a new grant arrived. The endpoint defect behind it is fixed (futon3c
`507d90b3`: the cast is checked before the ledger append), so it should not
recur.

## What this grant does NOT do

**It does not restart the machine.** Two things stand between this budget and
the next click, and neither is a budget question:

1. **Grounding is misreporting.** Every click since 2026-09-23 17:44 records
   `:outcome :grounded-no-change` because the implementation entity's `:props`
   read back as a string, so `:resolved?` is false about a commit the entity
   does name (see the correction in `PROOF-wm-works-2026-09-22.md`, futon2
   `d59f646c`). Firing now would mint further spurious `:machine-failure`
   stop-lines and discharge nothing — exactly what the last six clicks did.
   kimi-6's repair is in flight.
2. **The stop-line question is unsettled.** Joe's rule of 2026-09-23 is that a
   stop-line is repaired from outside with the machine stopped. Whether any
   open row blocks clicking, or only its own defect does, is an open question
   claude-8 has put to Joe. Sixteen rows stand, of which six are now known to
   be artifacts of (1) and six of the older ten are already repaired but
   undischarged.

The clicks are therefore banked, not authorized to be spent immediately. When
(1) is fixed and (2) is answered, they are spent under the ordinary rules:
issue-time accounting, failed runs never refund, and consumption counted
against this authorization's identity.

## Mechanics

`futon3c/src/futon3c/wm/ordinary_click_budget.clj` carries `authorization` (this
file's path and committed sha) and `allocated` 5. Consumption counts ledger
entries whose `:authorization` equals that map, so the thirty-five clicks spent
under the seven earlier grants are neither double-counted nor erased — each
remains in the ledger citing the authority in force when it was spent.
