# J7 revisit packet — for Joe. Nothing here is ruled.

**J7 (2026-09-03):** *"j7 stay off (but come back to this later)."* The ground
you accepted for the stay was sequencing: a non-zero `:epistemic` default would
have traded explore against a **constant**, because the doability factor read
the same 0.3 for every candidate. That constant is what U44 removed —
`:live-doability?` is implemented, default off, and the flag-off ranking is
pinned identical to the pre-repair one (`05-default-pin.edn`, 133 of 133 rows).

So the question comes back with the ground gone. It comes back as **two**
questions, and the second one was not on the table when you ruled.

## Question 1 (the original J7): does `:epistemic` get a non-zero default?

The four arms, same 133 candidates, same producer, `07-j7-arms.edn`:

| | doability inert | doability live |
|---|---|---|
| `:epistemic` 0.0 | the shipped default | 129 of 133 ranks move |
| `:epistemic` 0.15 | 115 of 133 ranks move | 62 of 133 ranks move |

**The number that speaks to the ruling: the epistemic term at 0.15 moves 115
ranks against an inert doability and 62 against a live one.** Roughly half of
what the term appeared to buy was the doability factor not being there. The
headline mission says the same thing at row grain: M-web-arxana-missions (MAP,
two open questions, 1.3863 nats) is lifted **124 → 74, fifty ranks**, when it
trades against the constant, and **102 → 87, fifteen ranks**, when it trades
against real doability. Its own `:doable` is 0.3 in both arms — the fiat table
prices `map` and `unknown` identically — so the whole difference is what the
other 132 candidates are now worth.

Rank 1 is M-zaif-harness-v1 in all four arms.

**What is still not decidable from sources, code or a prior ruling**, and this
is unchanged from J7's `:bar`: the exchange rate. 0.15 was declared by U22's
step-through and is labelled as such. No retrieved source states one at mission
grain, because the grain is not one the theory writes. A further run cannot
supply it — any weight a run picks is the same fiat, one layer down. What U44
changes is not the availability of the rate but the honesty of the trade: the
term now competes with something.

## Question 2 (new, and it is not the same question): does `:live-doability?` get flipped on?

This is a separate default and I am not folding it into question 1, because
turning it on changes the ranking **by itself**, at `:epistemic` 0.0:

- **129 of 133 ranks move** at the declared weights, 128 of 133 at the shipped
  default weights (C7, so it is not a weights artefact).
- `:doable` goes from two distinct values to nine.
- The missions that gain most are the ones the field marks `instantiate` and
  `verify`: M-wm-aif-policy-grain-compliance 125 → 36, M-sci-reproduction
  116 → 34, M-omni-wm-runner 110 → 40, M-kangaroo 101 → 33.
- **And eight missions leave the ranking**, at value 0.0: the completion gate
  is `(= "complete" phase)` and a nil phase never matched it, so
  M-case-studies, M-essay-corpus-substrate, M-essays-edit-cycle,
  M-futonzero-grounding, M-memory-retrieval, M-run-produces-its-own-brief,
  M-shared-memory-control-build-test and M-typed-holes-lean-handoffs have been
  ranked as ordinary live candidates since 2026-07-19.

**Two arguments for flipping it, stated fairly:**

1. It is a repair, not a preference. The fiat table `phase-doability`
   (`war_machine.clj:2341-2351`) is unchanged and was never in dispute; the
   selector has simply not been reading the input the table takes. Every number
   the live arm produces is that table applied to a string the substrate already
   carries, and where a second carrier can see the same string the two agree on
   123 of 123 with 0 disagreements (`06-phase-agreement.edn`).
2. The completion gate is not a preference either. Ranking a mission the field
   marks complete is a defect on any weighting.

**Three arguments against, stated as fairly:**

1. 129 of 133 ranks is not a repair anyone can eyeball. Whatever the machine
   selects on the first live tick after the flip, the reason will be a phase
   string written by an ingest nobody has audited for this purpose.
2. Five of the 133 have no readable phase and five carry the literal string
   `"unknown"`; all ten keep the 0.3 default. So the flip does not remove the
   constant, it shrinks it from 133 candidates to 10 — and those 10 now sit
   among discriminated neighbours instead of a flat field, which is a different
   position on the ranking than the one they occupied.
3. The recorded corpus was produced under the inert factor. Every trace file
   from `wm-trace-2026-07-19.edn` onward carries `:doable 0.3`, so any
   before/after against live history compares to a field that no longer exists
   — the same objection J8's stay rested on.

**A sequencing option, not a recommendation:** the two defaults can be flipped
in either order or together, and the arms above give the four corners. If they
go together the epistemic term's contribution is the 62-move column and not the
115-move one, which is the number to hold it to.

## What is NOT in this packet

No ruling, no default change, no registry edit. `:choices :mission-phase-value`
is untouched — it is signed at `df5f7bf` and TN §9a says a signed entry gets a
superseding row rather than an amendment, which is a decision for whoever
records the ruling, not for this row. The `:adjacent-finding` on that entry says
the plumbing gap is "NOT REPAIRED HERE ... it wants its own row"; this is that
row, and the update to the finding lives in
`C492-mission-epistemic-value.md` section 4b and in `C498-doability-liveness.md`.
