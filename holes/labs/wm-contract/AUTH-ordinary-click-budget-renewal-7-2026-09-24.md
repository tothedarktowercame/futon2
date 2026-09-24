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
   **Repaired 2026-09-24**, four commits: futon2 `e61a10cb` (ticket-link
   binding), `210dcdb0` (storable grounding values), `d1f67d13` + `a4043fce`
   (a write the store reports as rescued, or reports no stage for, refuses
   before the readback); futon1b `a425d18`, `e6b8990`, `89f65d8` make the
   store's own message and every pre-put reshape readable from outside the
   JVM. All are loaded: futon2 reloaded into the serving futon3c JVM from
   master, futon1b restarted 02:47:39Z.
2. **The stop-line rule, settled by Joe 2026-09-24.** Asked a third time, Joe:
   *"if there is a stop-line, in my vocabulary that means the system should be
   repaired from outside."* It is not per-defect. An open stop-line means the
   line is stopped: no clicking, and the repair is done from outside the
   machine's ordinary running. Clicks resume when the board is clear.

   The board is **35 open `:machine-failure` findings** (128 findings less 59
   resolutions and 17 dismissals, filtered to that class) — not the sixteen
   this file previously recorded. Six are the `:grounded-no-change` artifacts
   of (1), 12 already carry an implementation and 7 a verification, and the
   oldest six date to 2026-07. That every one of these accrued while the
   machine kept clicking is the thing the rule exists to stop; Joe, 2026-09-23:
   *"15 should never accrue."*

(1) is now fixed and (2) is now answered, so neither is what holds the clicks.
The rule in (2) is: the board must be clear. Thirty-five open stop-lines stand,
so the five clicks stay banked until the outside repair has cleared them. When
it has, they are spent under the ordinary rules: issue-time accounting, failed
runs never refund, and consumption counted against this authorization's
identity.

## Joe's further offer, 2026-09-24 — noted, not allocated

After the board work began Joe said: *"I can award up to five more clicks,
although it sounds like you only are going to need one of them."*

**Nothing is allocated on that offer and `allocated` stays 5.** This grant's
five are entirely unspent — the ledger carries zero entries whose
`:authorization` is this file's, against 35 entries under the seven earlier
grants. Minting a renewal-8 on top of an untouched renewal-7 would create a
second budget to reconcile and no additional capacity.

The authority is recorded here so it can be drawn on without another round
trip: if the proof needs more than the five in hand, this line is the operator
consent, and the draw is recorded by raising `allocated` in
`ordinary_click_budget.clj` with a note naming this section — not by a new
document.

## Mechanics

`futon3c/src/futon3c/wm/ordinary_click_budget.clj` carries `authorization` (this
file's path and committed sha) and `allocated` 5. Consumption counts ledger
entries whose `:authorization` equals that map, so the thirty-five clicks spent
under the seven earlier grants are neither double-counted nor erased — each
remains in the ledger citing the authority in force when it was spent.

## Joe's grant to claude-8, 2026-09-24 — ten clicks, allocated

Heard from Joe directly, in claude-8's operator buffer (emacs-repl), after
the PROOF-2 strategy and the first B4 carriers landed and were reloaded:

> I would also like to say that I would award 10 clicks for you to use
> overnight as you see fit.

No further conditions were stated. Per the "further offer" section above,
a draw is recorded by raising `allocated` in `ordinary_click_budget.clj`
with a note naming this section, not by a new document. `allocated` goes
from 5 to **15**: the five of the eighth grant (claude-5's lane, banked,
unspent) plus these ten (claude-8's lane, overnight discretion). Both draw
on this authorization's identity; the ledger's `:caller` says which lane
spent which. Consumption rules are unchanged: issue-time accounting, failed
runs never refund.

How claude-8 intends to spend them, recorded before the first is spent:
clicks are the scarce input to PROOF-2, and R7 says L is preregistered
before the first proof click. So the ten are for (a) at most one or two
tuning clicks outside L to validate the newly reloaded carriers live
(`:candidate-derivations`, `:law-applied`, `:certificate-schema`) — such
clicks are published, are not in L, and cannot be selected into it later
(A19/A21); and (b) the preregistered sequence L-v1 once L-REG and READY
have landed and been reviewed. Whether (a) fires tonight depends on the
board reading at the time, taken from the store, not from this note.
