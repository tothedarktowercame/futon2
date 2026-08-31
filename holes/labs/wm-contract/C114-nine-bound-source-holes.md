# C114 — nine bound declarations with Lean `sorry`

Date: 2026-08-31

The nine split as **3 provable from pinned source objects / 3 blocked / 3
external attestations**.  All nine have executable evidence; that does not by
itself turn an external observation into a Lean theorem or prove a universal
law about an opaque implementation.

## Provable from pinned source objects — 3

- `r9TwoRunCensus`: `wmVerdictsLedgerAlone` and `wmVerdictsDeclared` are already
  source constants.  `simp` or `native_decide` can prove both lengths and the
  thirteen `unknown` / thirteen `self` verdicts.  Cost is small: two generated
  thirteen-row tables, already compactly defined in `Holes.lean`.
- `r9WmPerRowDeclarations`: the same source `wmVerdictsDeclared` plus
  `declaredVerdictRow` determines exactly O7/O14/O15 as row-text declarations.
  A direct `simp` proof should be comparable to `r9WmVerdictsSound`; no fixture
  installation or generated-object equality is needed.
- `r8EraBoundary`: all 792 exact rows are now the source `wmTraceR8`.  The three
  field/date equivalences are decidable over that object, so `native_decide`
  can prove the claim without a sibling.  The containing module currently
  builds in roughly 5–13 seconds with the full literal; this is the only
  materially larger proof of the three.

## Blocked on a named implementation/scope decision — 3

- `findF1Containment`
- `findF2Receipted`
- `findF3NonSelfCertifying`

All three universally quantify over the opaque, deliberately refused `find`.
The serialized `FindReceiptTable` demonstrates the properties for recorded
Snatch scenarios, not for every `State`, `P`, tension, and repository.  A Lean
proof therefore requires either (a) defining canonical `find` and proving the
laws, overturning the standing “implementation, not a law” refusal, or (b)
dated scope amendments to fixture-indexed `FindReceiptRow` propositions, as C85
did for F4.  An adapter from the fixture to the current universals would have to
assume the missing correspondence and is not a proof.

## External attestations, not theorems in their current form — 3

- `preferenceStackLiveRecorded`: the executable witness says a recorded live
  instance's C was backed by its stack.  The naked `Prop` claims this about
  running instances, which Lean cannot observe.  Its `sorry` is permanent while
  that remains the claim; the executable live invariant is the right evidence.
- `wmRunsOnce`: occurrence of a completed tick is a fact about the world.  Lean
  cannot derive that an invocation happened.  `TickRunWitness` is the right and
  only evidence for the current claim, so the `sorry` is permanent.
- `wmRunConformsToWiring`: the checker compares an observed tick route with the
  live original/measured Figure 4 layers.  Lean cannot observe either execution
  or repository configuration.  The executable conformance witness is the
  correct evidence; the current naked `Prop` has a permanent `sorry`.

Each external claim could instead be narrowed to a theorem over explicitly
pinned Lean values (a `PreferenceStackWitness`, `TickRunRecord`, and wiring
snapshot).  That would prove a recorded instance, not the present world-level
claim, and should be recorded as a scope amendment and witnessed-instance
obligation rather than described as discharging the same proposition.

## Consequence for contract reporting

Of the 14 genuine source holes, five are standing refusals, three are directly
provable finite-source obligations, three are blocked universal `find` laws,
and three are external attestations whose executable bindings are their final
evidence unless their scope changes.  Thus a raw Lean-hole count is not a
backlog count.
