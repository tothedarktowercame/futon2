# The ruling surface: certification, not judgment-from-tables

Joe, voice, 2026-06-12 16:18 (closing the loop opened by the golden
walk and the metric stall):

Flight mode kept returning "big tables of numbers that are very
difficult to rule on." The rulable artifact is the SELECTED CASCADE in
context, and the full chain at that:

    problem (circumstance)
    -> selected patterns (the cascade)
    -> recognized hole ("a hole that was a recognized hole")
    -> solution applied

"I would just be certifying that the system is working the way it's
supposed to be working."

Design consequences:
1. **Render the chain, not the scores.** The ruling surface presents
   problem -> cascade -> hole -> solution at concept level (the
   paper-anatomy/golden-graph idiom). Numbers stay behind it.
2. **Certification is cheap; judgment is dear.** When the system
   surfaces its own reasoning chain, the operator's act collapses to
   certify / flag — the consent-gate + pudding-certificate idiom.
   Operator attention becomes a scarce resource spent on FLAGGED
   chains, DP/random-access style, not on every row.
3. **One ruling feeds three lanes**: flight anatomy (judgment+ground
   organ on a real flight), the substrate metric (patterns ruled
   right-for-problem must be near it — golden triplets for free), and
   the training lane (reward signal).
4. **Hole vocabulary is shared**: "recognized hole" = hungry scope
   (futon6 golden-walk satiety); solution = feeding/discharge. One
   satiety algebra across papers, missions, flights.

Status: design note; the build is the cascade pretty-print ruling
surface (fable-1's flight-pretty-print proposal, grown to the full
chain). Pairs: claude-3 (metric), claude-1 (contest consumer).

## Advance typing (Joe, voice, 16:40) — the automation of "talking it through"

"If we really wanted to automate this, I wouldn't have to be talking
it through. We would say: what kind of mission advance are we aiming
for? Quick wins closable immediately — a documentation-only fix? Or
something that's one Codex fix away, because that's documented in the
mission?"

This is the satiety algebra applied to missions. A mission's open hole
has a TYPE that determines what feeds it — and the type is mechanically
detectable from the mission file + substrate-2:

- {:hungry-for :checkpoint-only} — substantively done, needs the
  closing checkpoint (the bundle-closure case already delegated to the
  WM by standing ruling).
- {:hungry-for :ratified-car} — a next car is ALREADY DOCUMENTED and
  ratified in the mission text (flight-pretty-print was exactly this:
  Checkpoint 21 said "next car on this mission"). Detectable by
  pattern; dispatchable to Codex without conversation.
- {:hungry-for :design} — the hole needs shape before anyone can build.
  Routes to a Claude pairing, not the Codex pool.
- {:hungry-for :operator-ruling} — blocked on a judgment only Joe can
  make (mission-close, consent gates, golden rulings).

The WM advance loop then runs without narration: rank missions by
(hunger-type x cost x value); :ratified-car items auto-assemble their
cascade (the chain is pre-written in the mission file — harvest it,
R12-style, don't infer it); :checkpoint-only items bundle-close;
:design and :operator-ruling items QUEUE for their respective humans/
Claudes with the chain rendered for certification. Today's manual hops
(tile -> mission-mode -> checkpoint 21 -> bell codex-3) were a hand
exec of exactly this loop, one item, type :ratified-car.
