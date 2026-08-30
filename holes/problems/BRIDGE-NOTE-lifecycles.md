# Bridging note — the mission lifecycle and the delivery lifecycle

*Draft by claude-20, 2026-08-30, at Joe's request ("if we're using a different methodology, point to it
in there so that we can make a bridging over from the old methodology document to the new"). Placement is
claude-15's; this is an initial attempt, not a filed record.*

## The link as it actually stands — half-built

    futon4/holes/delivery-lifecycle.md:27   cites the mission's §2.1e formalism table   -> points UP
    M-formal-war-machine.md:4               cites futon4/holes/mission-lifecycle.md
    M-formal-war-machine.md                 cites delivery-lifecycle.md ...................... 0 times
    BUILD-status.md, BUILD-tech-lead-charter.md   cite mission-lifecycle.md ................... 0 times

So the delivery lifecycle already reaches into the mission; nothing reaches back. A reader arriving at the
mission cannot discover the process its DERIVE phase is actually being executed under, and a reader of the
build cannot discover which mission's completion criteria it serves.

## What each governs, stated plainly

- **The mission lifecycle** (`futon4/holes/mission-lifecycle.md`) governs the *mission*: HEAD → IDENTIFY →
  MAP → DERIVE → … `M-formal-war-machine` sits in **§3 DERIVE**.
- **The delivery lifecycle** (`futon4/holes/delivery-lifecycle.md`) governs a *unit of commissioned work*:
  §1 the problem record, §2 the deliveries, the acceptance the commissioner names in advance.
- **They meet at one place**: the mission's §3 DERIVE repair programme is *staffed* as delivery-lifecycle
  problem records. Each `P-R*.md` is a delivery-lifecycle unit; the mission is their parent.

That relation is now recorded in artefacts — `BUILD-status.md:3` names the parent mission,
`M-formal-war-machine §Deliveries` links down — but it is nowhere stated as *method*.

## The part that actually needs bridging: the mission predates the methodology it is now run under

`M-formal-war-machine` was **chartered 2026-08-25**. The precepts today's build ran on were added to the
delivery lifecycle *after* that:

    §0.5  Gate 0 — are the terms defined well enough to proceed?      2026-08-29 (Joe)
    §0.6  Gate 1 — typed wiring diagram, contract on every edge       2026-08-29 (Joe)
    §0.7  The tetrahedron — nouns, transactions, fit, apex            2026-08-30 (Joe)
    §0.8  The big tetrahedron, specified                              2026-08-30 (Joe)
    §0.9  The Sierpiński recursion — which nodes get a tetrahedron    2026-08-30 (Joe)
    §0.10 A fifth precept — workflow state                            2026-08-30 (Joe, PROPOSED)

**So the mission's DERIVE programme was written before the method that is now executing it existed.** That
is the old-to-new gap: not two rival documents, but one mission whose repair programme was planned under
an earlier method and is being delivered under a later one. Today's work is visibly shaped by the later
one — R19 was scoped as a tetrahedron (nouns/verbs/organisation/evidence/mass), the edge lane exists
because of §0.6's contract-on-every-edge, and `:situation-evidence` / `:fit-status` come straight from
§0.7's apex.

## Proposed bridge statement, for whoever files it

> This mission was chartered 2026-08-25 under `mission-lifecycle.md` and sits in §3 DERIVE. Its repair
> programme is **staffed** as delivery-lifecycle units (`delivery-lifecycle.md` §1–§2): one problem record
> per node, deliveries `D1…Dn`, acceptance named in advance. Where the two lifecycles differ, the mission
> lifecycle governs *what the mission is for and when it is done* (§1.6 completion criteria); the delivery
> lifecycle governs *how each unit of work is commissioned, gated and closed*.
>
> The mission predates `delivery-lifecycle.md` §0.5–§0.10 (added 2026-08-29/30). Work executed after those
> sections landed applies them — the tetrahedron shape, Gate 0 and Gate 1, evidence-with-a-falsifier — and
> where that conflicts with a §3 DERIVE item written earlier, **the later precept wins and the DERIVE item
> is amended rather than silently reinterpreted.**

That last clause is the one I would most want reviewed: it is a real policy choice, not a description, and
it should be Joe's or the owner's call rather than mine.

---
**RULED AND PLACED (claude-15, 2026-08-31 01:35Z).** The bridge statement above is filed at the head of
`../missions/M-formal-war-machine.md` §3 DERIVE. The policy clause is ruled IN, with one strengthening: the amended
DERIVE item stays legible (dated amendment beside the original, as with every correction in the BUILD ledger today —
the supersession record, the counterfactual-only amendment, rows 24–28). Rationale: the clause describes what the
day's record already practises; codifying it prevents the silent-reinterpretation failure it names. Joe may overrule;
the ruling is visible in the mission text itself.
