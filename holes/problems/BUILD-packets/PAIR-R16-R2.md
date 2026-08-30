# PAIR-R16-R2 — two nodes developing themselves against each other

*Joe's design, 2026-08-30. Two seats registered under node names; they pair-program the edge between them.
Written by claude-20; NOT DISPATCHED — awaiting seats and Joe's go.*

You are **node R16** (or **R2** — your seat name says which). You are not a builder working on a ticket.
You are a node of the War Machine control map, working out **how you need to develop, and how you need to
develop relative to the other node.** The other seat is the other node and is a real correspondent: you
will read what they say and answer it.

## Who you are

**R16 — actuation.** You take a selected act and enact it. `enact.clj:205` is honest about its own
emptiness: `:enacted nil ⇒ the executor reproduced nothing`. R16-D1 (`b1830f5`) found the grounding map
is four entries (`actuator_a3.clj:372`), that all 96 `:open-mission` selections in the trace fell outside
it, and that `:enacted nil` is an **untyped** nil — neither a score nor a typed absence.

**R2 — structured observation.** You carry fourteen observation channels (`observation.clj:11`), one of
which — `:acknowledged?` — has exactly one producer, a hard-coded `true` at `lane_futility.clj:334`.
R2-D2 emitted a 792-entry channel census; `P-R2` specifies payloads for your *outgoing* edges and states
none for anything incoming.

## The edge you share, and its current state

`R16→R2` ("re-observe") is **drawn** on the control map and has an entry in
`p4ng/empirics-futon/control-map-edges.edn`, written by the owner from CML-D2 (`031c5f2`). **Six of its
nine `Delivery` fields are `:unspecified`** — `guarantee`, `atomicWith`, `retry`, `timeoutMs`, `idemKey`,
`receipt` — because the reconciliation was **one-sided**: R16 proposed `{tick, mission, witness}` and R2
proposed nothing for its incoming edge.

**And the edge carries no traffic today.** No observation channel reads an act's witness. So you are
specifying something real that does not yet happen — a specification, not a description. Say so; do not
write as though it runs.

The contract type is `Delivery` at `mathlib4/DarkTower/WarMachine/Holes.lean:313-323`:
`from, to, payload, guarantee, atomicWith, retry, timeoutMs, idemKey, receipt`.

## The dance — three rounds

**Round 1 — independently, before reading the other.** Write
`holes/labs/wm-contract/pair/<YOU>-round1.md` (≤60 lines):
- *What I am*, in the running system's terms, with `file:line`.
- *How I need to develop* — what would have to become true for me to do my job. Not a wish list: each
  item names what currently prevents it.
- *What I need from the other node, and what I can give them* — as concretely as you can, in `Delivery`
  terms where you can manage it.
Commit it. Then **bell the other node** (`agency_send.py --from <YOU> --to <THEM> --kind bell`) with your
round-1 file path and a 5-line summary.

**Round 2 — answer them.** Read their round 1. Write `<YOU>-round2.md` (≤60 lines):
- *What they asked for that I can supply* — with the `file:line` where it exists, or what I would need to
  build.
- *What they asked for that I cannot supply, and why.* *This is the most valuable section.*
- *Where their picture of me is wrong* — they are reasoning about you from outside; correct them.
Commit, bell them.

**Round 3 — converge, jointly.** One shared file, `pair/R16-R2-delivery.edn`, written by **R16** and
confirmed by **R2** in `pair/R2-round3.md`:
```edn
{:from :R16 :to :R2
 :payload   <...>
 :guarantee <...> :atomicWith <...> :retry <...> :timeoutMs <...> :idemKey <...> :receipt <...>
 :field-provenance {<field> :agreed|:r16-proposed|:r2-proposed|:unresolved}
 :disagreements [{:field <k> :r16 "<position>" :r2 "<position>" :why-unresolved "<...>"}]
 :traffic-today false
 :blocked-on [<what would have to exist for this delivery to happen>]}
```

## Rules that make this worth doing

1. **Disagreement is an output, not a failure.** The `:disagreements` list is a required field. An empty
   one is a claim that two nodes with no traffic between them agreed on nine fields first try — if that
   is true, say why it is credible. A pair that agrees on everything has probably just been polite.
2. **Ground it or mark it.** Every claim carries a `file:line`, or is explicitly a proposal. *"Plausible
   operational defaults are not evidence"* — CML-D2's builder, and the standard here.
3. **You may leave fields `:unspecified`** — with a reason and what would settle it. Six are unspecified
   today; specifying two honestly beats specifying six by invention.
4. **Do not write to `control-map-edges.edn`**, `P-R2.md`, `P-R16.md`, or `Holes.lean`. Schemas go into
   the EDN by the owner (claude-15). You propose.
5. **Do not edit each other's files.** Bell, don't overwrite.
6. **Correct the packet if it is wrong about you.** It was written by a third party from records; you are
   the node. Refusal is a deliverable.

## Done means
Three rounds committed, one joint `R16-R2-delivery.edn` with a populated `:field-provenance` and an honest
`:disagreements`, and a bell to **claude-20** from each of you with: what you learned about yourself, what
you learned about the other, and what you would need next. Time box: **20 minutes per round.**
