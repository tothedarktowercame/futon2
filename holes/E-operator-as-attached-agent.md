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

## Method refinement — two controlled vocabularies (Joe, dictated 2026-09-04, second exchange)

> "All turns... with some suitable caveat, are tagged with patterns, based on
> an embedding. And we also have a free text search. So much as we were doing
> with our library loop, we could go run around looking at those operator
> turns and trying to use the patterns as an initial limited vocabulary...
> One problem with that, though, is that this remains destructured in that
> there is no sense, for example, of clocking in on a mission phase. But we
> could use the mission phase terminology — identify, map, argue, derive,
> instantiate, and so on — as another controlled vocabulary whereby the
> operator does move the agent from section to section within the mission...
> look at the occurrence of that mission vocabulary and look at the patterns
> that are associated with those turns, and that would give a fairly tightly
> scoped sample of turns to look at that could relate to how the operator
> changes the workflow. And we could come back to the other turns that happen
> under other auspices later."

So the census instrument is a **cross of two vocabularies**, neither authored
for the purpose:

1. **Pattern tags** (embedding-assigned): every turn fires a
   `context-retrieval` event into the evidence store (`:coordination` docs,
   body `{"event" "context-retrieval", "query" <turn text>, "results"
   [<ranked patterns>]}`), so the pattern library is already an emergent
   tag vocabulary over operator turns — trusted case-by-case, per Joe's
   caveat ("the assumption that those embeddings might be correct in some
   cases").
2. **Mission-phase words** (operator-issued): IDENTIFY/MAP/DERIVE/ARGUE/
   VERIFY/INSTANTIATE/DOCUMENT — the vocabulary Joe actually uses to move
   an agent between sections. Occurrence of a phase word in an operator
   turn is a candidate *clocking event*, which is exactly the structure the
   raw store lacks ("no sense of clocking in on a mission phase").

Where the two vocabularies agree on a turn — phase word present, and the
retrieval record's patterns accumulate consistently across such turns — the
turn's role is attested twice, independently. Keywords/phrases that
accumulate attached to patterns are the product: "then we'd know what the
operator is actually doing on a turn-by-turn basis."

### Pilot receipts (claude-1, 2026-09-04, run against the live store)

- Instrument: `GET :7073/api/alpha/evidence/text-search` (FTS5 sidecar,
  `author=joe` filter works; per README-fts, check `:ok` and treat 503 as
  retry, never as empty).
- Phase-word counts over Joe-authored docs (limit 500): identify 278,
  derive 233, argue 125, instantiate 147, document 262, survey 123,
  lifecycle 171; map and verify saturate the cap (common in other senses —
  they need co-occurrence filtering, e.g. with "phase" [415] or "mission").
- Usage verified section-moving, not incidental: e-66dc0a39 ("I'd move the
  INSTANTIATE-0 items to INSTANTIATE-1..."), e-88a65c30 ("please do
  instantiate"), e-39cdb738 ("INSTANTIATE now would be good, we're in a
  strong position to run it end-to-end").
- The turn→pattern join demonstrated: Joe turn 2026-05-30 "I still think
  INSTANTIATE can be driven by you end to end..." → retrieval record
  e-e9b2024c (claude-5, turn 20) → patterns ukrns/reader-run-path (rank 1),
  transition/persistence-conditions (rank 2), embeddings via futon3a.
- Aggregation exists server-side: `GET /api/alpha/patterns/activation`
  explodes all context-retrieval records per pattern with the first 240
  chars of each query — filter activations by phase vocabulary and the
  survey table falls out. (Route scans all evidence; price it before
  running wide.)

### Caveats the census must carry (typed, not waved at)

- **Prefix truncation**: the retrieval query is ~the first 100 chars of the
  turn — pattern tags are computed from a prefix, out of context. A tag on
  a long dictated turn may reflect only its opening clause.
- **Coverage denominator**: not every operator turn necessarily has a
  retrieval record (retrieval is per-agent-session machinery, agent-id
  varies). The census must report turns-with-tags / total-turns for the
  window — the C1 denominator discipline applied to the tagging layer
  itself.
- **Staged scope** (Joe's ruling in the dictation): phase-vocabulary turns
  first; "the other turns that happen under other auspices" later.

## Census run 1 — 2026-09-03 ruling window (claude-10, 2026-09-04)

Artifacts: `holes/labs/E-operator-as-attached-agent/` — `CENSUS-2026-09-03.md`
(report), `operator_turn_census.bb` (producer, read-only, artifacts
byte-identical on two consecutive runs), `classification.edn` (the node and
turn-kind judgements, labelled fiat), `00`–`03` EDN artifacts.

Headline numbers, window 2026-09-03T20:50Z–23:30Z: **18 operator turns** (21
store records less 3 session-start events), across the three sessions the seat
was driving at once. **18/18 have a pattern-tag record.** **7 land on a
phase→node table row** (R2 ×4, R4/R5 ×1, R9 ×1, R16 ×1 contested), **11 land on
none**. 13 turns carry a consuming receipt (9 direct, 4 indirect), 28 receipts
in all. Both outcomes the excursion wanted are present: a clean cash and an
honest gap list of nine turn kinds.

Three things run 1 changed about the method as written above.

1. **The prefix-truncation caveat was understated.** Measured, there are two
   query-construction regimes. On the claude REPL surface the query is the
   turn's first 100 characters — but for a short turn most of those characters
   are the *agent's reply*, not the operator's ask (26 of 100 operator
   characters on `the wm-build-loop is quiet`). On the codex REPL surface the
   query begins with a 97-character bell envelope, so **three characters** of
   Joe's words reach the embedder on all four such turns. Not truncation of the
   operator's words; displacement of them. The content join fails on exactly
   those four turns, which is why the producer records its join stage per row.

2. **The coverage-denominator worry does not hold on this window.** Coverage is
   18/18, lag 16 s to 1175 s. The store is a usable denominator here; the
   *receipt* layer is not (see 3).

3. **The two vocabularies do not intersect on the pinned window.** Zero
   mission-phase words — identify, map, derive, argue, verify, instantiate,
   document, survey — occur in any of the 18 turns. Method step 1 pins the
   ruling session and the Method refinement stages phase-vocabulary turns
   first; those select disjoint samples. The phase-vocabulary arm is
   *relocated*, not merely deferred: it needs its own window (the pilot's own
   hits are May–June). Read positively: a ruling session is not a clocking
   session.

And one defect found in passing, reported to whoever owns the J rows rather
than repaired here: all six J-row `:reviewed-at` values disagree with the store
record of the turn that caused the ruling, and five are anti-causal (J7–J10 by
83 minutes, four of them predating the session start; J11 by 100 seconds).
They are hand-typed rather than derived from `:evidence/at`.

Not done in run 1: no T5/T3 ledger append (the §6 gap list is ready for one,
withheld while the excursion is DRAFT and its rows are unminted); no proposed
change to `M-zaif-harness-v1`'s table; no second window.

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
