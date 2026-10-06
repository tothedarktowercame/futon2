# PROPOSAL — the outer-loop diamond extended with three reading squares (E-aif-cascade B3)

claude-2, 2026-10-06. A proposal for Joe; nothing here is in the contract.
The edn is `proposals/meta-outer-policy-cascade-extended-proposal.edn`
beside this file, marked `;; +` and `;; ~` against
`futon3/library/meta/meta-outer-policy-cascade.edn`.

## The shape

```
                 ┌→ read-the-item-sheet ──────────┐
                 ├→ read-revealed-attention ──────┤
observe ─────────┼→ read-library-coupling ········┼──→ minimise
                 ├→ fill-meta-policy-slots ───────┤
                 └→ injury-routes-to-self-heal ───┘
```

Seven units, ten edges (the coupling edge dotted: produced and carried on
the receipt, not in `minimise`'s needs until ruled). After `observe` the
frontier is five incomparable readings; `minimise` is where they meet
(receipt direction) and `observe` is their common origin (`CascadeOrder`
direction). Linear extensions: 5! = 120, log₂ ≈ 6.9 bits — the diamond had
2. A recipe would have to order five readings that have no order among
them; this is the point at which, in Joe's words, it is not possible to
conceive of it other than as a cascade.

## The three squares, as blends (CASCADE-SPEC-v3 §2)

| unit | I₁ | I₂ | G | B (token) | emergent in B |
|---|---|---|---|---|---|
| `read-the-item-sheet` (B1d) | the item's whole document and commit history | the field row (id, kind, pin) | the item's identity | `:item-sheet` — scopes, keywords, applied-pattern order, register, section and history trajectories, status, phase | *direction*: abstract spin-off vs return to concrete, which neither HEAD nor the row has |
| `read-revealed-attention` (B1c; B1b) | dispatch lineage, operator turns under a mission, co-work | the field row | the item's identity | `:item-attention` — dispatches, sessions, turns 14 d, co-work degree, recency; `:item-unblocks` — a declared rests-on line when present, else typed absence | an *enabler weight*: where the work is, read off behaviour, which no document states |
| `read-library-coupling` (B1) | Cascade Live's applied-pattern relation | the field row | the item's identity | `:item-coupling` — distinct patterns, missions sharing ≥ 1, incidences | coupling through the library (`@how` generalised) — kept apart from unblocks (`@why` generalised) by Joe's ruling |

Each is a reading of the same field observation, so each pastes onto
`observe`'s B; none depends on another, so they are co-applied. They are
Joe's 2026-09-25 "read" steps made concrete, and the circumstance under
which the note's §2 said a square must be added — a fact absent at HEAD —
is now the ordinary case: the readings produce typed absences where the
record has none (attention 0 for 92 of 107 items is a real zero, the
relation existing; `:item-unblocks` absent for all but the 16 items with
a prose relation header).

## What `minimise` does with them

`:conditioning` in the proposal edn says which token conditions which
predicted outcome, so that the generative model's inputs are declared
rather than found in code:

- `:downstream-unblocking` ← `:item-attention` (enabler weight) and
  `:item-unblocks` (declaration, when present);
- `:operator-demand` (new dispreferred outcome) ← `:item-sheet`: marker
  count, register, trajectories;
- `:grounded-progress` ← `:item-sheet`: register and phase;
- `:repeated-target` ← `:item-attention`: recency.

`:refuse-not-zero` stands. A reading that yields a typed absence makes
that term absent for that candidate, and the comparison refuses for it;
a reading that yields 0 because the relation exists and the item has none
is a value.

## The two choices (Joe)

**(i) Operator load: preference term or support change.** The proposal
takes the *preference-term* route (`:operator-demand` dispreferred). The
support-change route — an arm that excludes operator-bound items
overnight, as `injury` excludes M/E/T — is one more square between
`read-the-item-sheet` and `minimise`, and would be drawn the same way.
Recommendation: preference term, because B2 found attributable history
for 4 of 107 items and excluding on an absence would empty the field; the
sheet's register and markers exist for every in-field item, so the term
is always fillable.

**(ii) Coupling: scored or only recorded.** The proposal records
`:item-coupling` on the receipt and does not put it in `minimise`'s
needs. Recommendation: recorded only. B1c showed attention and coupling
are uncorrelated (ρ −0.21) and the current selector already scores
coupling by accident (ρ 0.90 with its occurrence count); scoring it on
purpose would reproduce the last click's choice.

**(iii) The restated pattern** (`meta/select-the-meta-item`, B5) grows a
HOWEVER: ordinary work against self-repair, *and* documenting against
doing — the mission that applies the most patterns against the mission
where the work is.

## What follows if accepted

- B4: validate the proposal edn under `meta_outer_policy`'s
  `contract-errors` in a fresh process; the `ConstructionReceipt` example
  for the seven-unit cascade (`valid`, with the coupling edge rejected
  while it carries no needed token); the four-number script on the
  extended shape and its bag; then the ranking of the 103 cones with the
  provisional prior and the B1c/B1d tokens as the only
  candidate-conditioned inputs, against the live selector's ranking of
  the same run, with each new signal removable by a flag
  (`E-outer-loop-improvement`'s ablation rule).
- The sheet's two defects (relative-token attribution; appended
  checklists in the section trajectory) fixed before the tokens are
  consumed.

## B5 — the restated pattern (draft flexiarg, for Joe to promote or not)

```
@flexiarg meta/select-the-meta-item
@title Select the META Item as One Blend of the Field
@keywords META, outer loop, cascade, blend, G, attention, register, operator load
@audience War Machine designers, operators
@status [status[draft] "evidence[E-aif-cascade B1–B4, 2026-10-05/06; proposal, not promoted]"]

! conclusion:
  Select the next META item by minimising G over policies filled from one
  observation of the field, read several ways at once — slots, injury,
  the item's sheet, where attention has gone, what it shares with the
  library — and keep the readings on the receipt as the reason.

  + context: The War Machine must choose what to work on next from a
    field of missions, excursions, tickets and algorithms, by itself.

  + IF:
    A source-pinned observation of the field exists and each item can be
    read for its slots, its register and trajectory, and the attention it
    has drawn.

  + HOWEVER:
    Ordinary work competes with self-repair the machine may not be able
    to perform; and the item that documents the most competes with the
    item where the work actually is. A single ranking by one count takes
    the first side of each pair by accident.

  + THEN:
    Read the observation in parallel — fill slots, route injury, read the
    sheet, read attention, read coupling — paste the readings into one
    family of filled policies, predict each policy's outcomes from the
    readings that condition them, and select argmin G; carry every
    reading and the nearest alternative on the receipt; a typed absence
    refuses, a measured zero counts.

  + BECAUSE:
    G is defined over policies with predicted consequences, and the
    consequences of working an item depend on what the item is (its
    register and phase), who has been working near it (attention), and
    what the machine can enact (injury) — none of which one count holds.
    Read together they are a blend; the selection is its emergent
    structure, which is why the step cannot be written as a list.

  + NEXT-STEPS:
    next[Rank one recorded run's field with the readings as the only candidate-conditioned inputs and compare with the live selector's ranking (E-aif-cascade B4 part 2).]
```
