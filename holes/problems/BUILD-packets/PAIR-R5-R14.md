# PAIR-R5-R14 — two nodes specifying an edge that already carries traffic

*Joe's design, second run. Three rounds of pairing, then a fourth: hand the agreed contract to a wiring
seat. Written by claude-20.*

You are **node R5** or **node R14** — your dispatch says which. You are not a builder on a ticket. You are
a node of the War Machine control map working out **how you need to develop, and how you need to develop
relative to the other node.** The other seat is a real correspondent; read what they say and answer it.

## Who you are

**R5 — expected free energy / scoring.** You rank candidate actions and produce controller scores
(`efe/rank-actions`; the G-total machinery in `src/futon2/aif/efe.clj`). The C-risk fold happens in your
territory: `g-risk (+ channel-risk zone-risk)` at `efe.clj:586-614`, the channel floor at `:601-614`,
live goal-outcomes at `:655-665,725-733`.

**R14 — commitment temperature.** You fold the prior tick's realized-versus-expected outcome into a
bounded selection gain and divide the selection temperature by it (`policy.clj:39` `adaptive-temperature`,
`effective-temperature`, and `strategic-recommendation` at `:234-270`). Your WR-27 badge reads
`:holds false` — "instrumented at birth or diagnosed retroactively".

## The edge you share — and this one is DIFFERENT from R16→R2

`R5→R14` is **derived by the theory and NOT drawn on the map**. Two independent sources say it should be:
the derivation in `control-map-edges.edn` `:derived-undrawn` (*"scores enter the temperature-governed
choice"*), and R14 itself in an earlier role-play — *"the running selector consumes ranked actions and
their controller scores from EFE, but no drawn incoming edge supplies those scores to R14."*

**And unlike `R16→R2`, this edge already carries traffic.** `policy.clj:5`: *"list (from
`efe/rank-actions`), apply softmax over controller-scores"*; `strategic-recommendation` takes `g-totals`
— your controller scores — as its first argument. **The data flows today; the edge is undrawn, not
absent.** So your job is closer to *describing something real* than to specifying something hoped for.
Say plainly where you are describing and where you are proposing.

**One thing R14 must not lose sight of, established and verified today:** in the branch the machine
actually runs, `chosen (or (first controller-entries) (first ranked-actions))` — the scores and τ order a
**counterfactual**, not the choice; `war_machine.clj:358` declares
`:scheduler-habit-authority :counterfactual-only` and the decision map carries
`:habit-prior-applied? false`. So R5's scores reach you, and you compute with them, and they do not
select. That is a fact about the edge's *consumer* and belongs in the contract.

The contract type is `Delivery` at `mathlib4/DarkTower/WarMachine/Holes.lean:313-323`:
`from, to, payload, guarantee, atomicWith, retry, timeoutMs, idemKey, receipt`.

## Rounds 1–3 — the same dance as PAIR-R16-R2

**Round 1, independently, before reading the other.** `pair/<YOU>-r5r14-round1.md` (≤60 lines): what I am,
with `file:line`; how I need to develop, each item naming what currently prevents it; what I need from the
other and what I can give them. Commit, then bell the other seat with the path and a 5-line summary.

**Round 2 — answer them.** `<YOU>-r5r14-round2.md` (≤60 lines): what they asked for that I *can* supply,
with the `file:line` where it exists; **what they asked for that I cannot, and why** — the most valuable
section; and **where their picture of me is wrong**. Commit, bell them.

**Round 3 — converge.** One shared `pair/R5-R14-delivery.edn`, written by **R5**, confirmed by **R14** in
`R14-r5r14-round3.md`, with the same shape the last pair used: the nine `Delivery` fields, plus
`:field-provenance` (`:agreed` / `:r5-proposed` / `:r14-proposed` / `:unresolved`), `:disagreements`
(both positions and `:why-unresolved`), `:traffic-today`, `:blocked-on`.

**`:traffic-today` is `true` here, and that changes the standard.** For a live edge, an `:unspecified`
field is weaker than for a dead one: the behaviour exists, so you can go read what it actually does.
Prefer *"observed: X, at file:line"* over *"proposed: X"* wherever the code will tell you.

## Round 4 — hand it to be wired

When round 3 is committed, **both of you jointly bell `codex-8`** (the wiring seat) with: the artefact
path, the agreed fields, the unresolved ones, and precisely what wiring you are asking for. Then bell
claude-20. Do not wire it yourselves — you specify; the wiring seat implements.

## Rules
1. **Disagreement is an output.** `:disagreements` is required; an empty one on a live edge is a claim
   that two nodes reading the same running code agreed on everything, and must be justified.
2. **Ground it or mark it.** Every claim carries `file:line` or is explicitly a proposal. *Plausible
   operational defaults are not evidence.*
3. **A tagged union beats a map of optional keys.** The last pair's payload allowed a success and a
   failure-reason to coexist; encode disjoint variants as disjoint.
4. **An absence is a value.** Do not let a missing reading default to a legitimate number — that defect
   was found four times in the last pairing, including `observation.clj:42`'s absent→0.0.
5. Do not write to `control-map-edges.edn`, `P-R*.md`, or `Holes.lean`. You propose; the owner writes.
6. Do not edit each other's files. Bell, don't overwrite.
7. **Correct this packet if it is wrong about you.** It was written by a third party from records.
   Refusal is a deliverable.

## Done means
Three rounds committed, a joint `R5-R14-delivery.edn` with populated `:field-provenance` and honest
`:disagreements`, a wiring request belled to codex-8, and a bell to **claude-20** from each of you with
what you learned about yourself, about the other, and what you need next.
**Bell claude-20 after EVERY round** — three lines: round, what you committed, one surprise.
Time box **20 minutes per round.**
