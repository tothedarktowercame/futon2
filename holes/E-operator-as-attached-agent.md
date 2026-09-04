# E-operator-as-attached-agent — the operator's turns in the harness's own vocabulary

Date: 2026-09-04
Parent: `M-zaif-harness-v1` (the harness edition; its phase→node table and
anti-glibness rule are inherited here, not restated).
Owner: claude-1. Driver: Joe.
Status: DRAFT — minted from operator dictation 2026-09-04 morning; rows not
yet on a board (Joe's stet mints them).
Cross-refs: `M-formal-war-machine.md` (the C1–C5 conservation chain — the
*other* face of operator closure); `labs/wm-contract/NOTE-aif-spec-gap.md` §6
(spec = level-indexed invariance; J-rows as un-closed-spec detectors);
`labs/wm-contract/DESIGN-tensions-as-patterns.md`; tension ledger T3, T5.

## The reframe (Joe, dictated 2026-09-04, verbatim)

> "One way to go about this would be to notice that from the point of view of
> the evidence landscape, I look like just another attached agent. So maybe we
> could use the Zaif harness mission at least as a way to bootstrap this, and
> we could think about what role my turns are taking in the stack in light of
> that mission. So maybe rather than making it an entire mission, we could
> make it an excursion from the Zaif harness. Which is a mission."

Context it answers: the 2026-09-03 late-evening threads (NOTE-aif-spec-gap)
plus the 2026-09-04 exchange on whether the ruling cadence of 2026-09-03
could get a Lean specification after the fact. The proposal on the table was
a new mission; Joe's reframe replaces it with something cheaper and more
falsifiable: the harness mission already carries a lifecycle→R-node mapping
with a per-node test discipline for *agents*, and the evidence landscape
already records the operator the same way it records any agent. So instead
of authoring a fresh description of the operator role, run the operator seat
through the mapping that already exists and see where it lands and where it
doesn't.

## Why this is an excursion and not a mission

Two faces of "closure over the operator" are already written down elsewhere:

1. **The conservation face** — M-formal-war-machine's C1–C5 chain (every
   operator turn stored, processed, decorated, cascaded, used in selection).
   The operator as a source population whose turns must not leak.
2. **The closure face** — NOTE-aif-spec-gap §6: the operator as the node
   that closes underdetermined specification; the J-row `:bar` condition (a)
   as the machine's formal report that it has hit the written tower's edge;
   the twelve 2026-09-03 rulings as twelve closure operations.

What is NOT written anywhere is the bridge: whether those two faces are
*node-classifiable* in the harness's own table. That is a bounded question
with a checkable answer over a pinned window — excursion-shaped, not
mission-shaped.

## The question, precisely

M-zaif-harness-v1's phase→node table already classifies one operator turn
kind: HEAD dictation = R2, "operator voice as highest-precision typed
observations — the declared-marks channel, at the point of authorship."

The excursion asks: **does every operator turn kind in a real working window
land on a node the same way — and which kinds land on no node at all?**

Candidate classifications to test against the record (hypotheses, not
findings):

- **Dictated anchors / punch-ins** → R2 (already in the table).
- **Rulings on J-rows** ("stay off for now", "your recommendation", the
  three-rung ladder) → these look like *preference declarations*, i.e. the
  C-vector / R20 territory the spec-gap note already flags ("R20 actually
  needs to be put in terms of some kind of formal specification"). Whether
  the table has a slot for them is exactly the question.
- **Acceptance gates** (ratifying a recommendation, accepting a certificate)
  → R9-shaped (independent witness before action) but issued from *above*
  the written tower, not beside it.
- **Redirections mid-flight** (this excursion's own minting turn) → R4/R5
  material? Or unclassifiable?
- **Refusals of permanence** (RUN4: "it's not permanent") → the operator's
  own analog of the machine's typed refusal — does it mint the same way?

A turn kind that classifies cleanly strengthens carried tension T3's "formal
claim" pole. A turn kind that does NOT classify is direct evidence for T5
(the AIF-generality gap): a real behaviour of the working system that the
AIF table cannot type. Either outcome is a product.

## Method

1. **Pin the window.** The 2026-09-03 ruling session (the J7–J12 walk, arm
   ruling, P-R16, RUN4) — the richest operator-turn sample we have, already
   fully receipted on the wm-contract board and registry. Denominator from
   the session transcript, numerator from the evidence store — the
   transcript-vs-store comparison piloted (and corrected) in
   M-formal-war-machine's C1 investigation; this doubles as a C1 spot-check
   on a fresh window.
2. **Census every operator turn** in the window: one row per turn — verbatim
   anchor (or store record id), proposed node, and the *receipt* that the
   machine consumed it at that node (registry flip sha, worklist row,
   ledger event). Anti-glibness inherited: a census row without a consuming
   receipt is decoration and does not ship.
3. **Publish the honest gap list**: turn kinds with no node, stated as
   typed absences (which node family is missing, or whether the miss is
   above the table entirely). Gap entries append evidence events to T5 in
   the tension ledger (standing authorization conditions apply: additive,
   attributed, validator green in the same commit).
4. **Optional formal acceptance** (only if the census warrants it): the
   closure-loop conformance certificate discussed 2026-09-04 — transcribe
   the pinned board/registry/ledger into Lean tables and prove the
   discipline invariants by `decide`, the U49 recipe applied to the operator
   loop. This is priced as a follow-on row, not a precondition; the census
   is the excursion's core deliverable.

## What this excursion does NOT do

- No automation of rulings — the census may show which turn kinds are
  machine-draftable (2026-09-03 data: two of twelve ruling contents were
  machine-drafted and accepted; three were preference/purpose calls no
  lower level could make), but acting on that is future work, J-gated.
- No new mission, no registry flips, no changes to M-zaif-harness-v1's
  table — if the census wants a table change, that is a finding reported to
  the parent mission, ruled there.
- No retro-census of older sessions; one window.

## Feeds

Into: T3 (a new cashing test — the mapping's reach extended or bounded at
the operator seat); T5 (gap evidence either way); M-formal-war-machine C1
(fresh-window spot-check); Part 3 of Futon-2026 (the operator-role
clarification, now with a census instead of an assertion); the PLoP
revision (if "operator as attached agent" recurs resolved the same way, the
birth rule in DESIGN-tensions-as-patterns applies).
