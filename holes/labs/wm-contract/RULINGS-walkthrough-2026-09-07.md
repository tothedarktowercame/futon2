# Rulings walkthrough — 2026-09-07

Working file for Joe + claude-1 to walk the queued fundamentals decisions one
at a time. Each item: the question, where the full sheet is, and a **Ruling**
line that stays `PENDING` until Joe rules. Rulings themselves are recorded by
Joe (or transcribed at his word) into `aif-equations.edn :choices` /
`control-map-edges.edn :decisions` per the 2026-09-01 rule; this file only
tracks the walkthrough.

Order: F10 D3 first (it blocks D1 and D2), then D1, D2; then the two rulings
that unblock F12 slice 4; then the four secondary F12 choices; then a note on
the F11 question forming.

---

## 1. F10 / D3 — where serendipity value lives

- Sheet: `C538-F10-outcome-domain-decision-sheet.md` §6 D3.
- Question: two records conflict — C537 (serendipity potential is
  preference-side, mass on being-in-interesting-territory) vs
  P-validated-R5 §2a′ (evidence vertex carries no C; a preference on
  "having learned" is greed). Candidate reconciliation: C_ser prefers the
  *occasion* (anomaly-on-the-board, organisations vertex), not the *update*
  (having-learned, evidence vertex).
- Blocks: D1 and D2.
- **Ruling: OPTION 1 (Joe, 2026-09-07)** — adopt the reconciliation: C_ser's
  preferred object is the *occasion* (organisations vertex), not the *update*
  (evidence vertex). §2a′ stands; C537 stands.
- Joe's framing, to carry into D1/D2 and beyond (near-verbatim):
  - Preferences are complex and can be broken down multiple ways across
    multiple layers — cf. the Buddhist four frames of reference, mapped
    across the tetrahedral vertices (nouns, verbs, organisations, evidence):
    "we could have preferences related to all four components and indeed even
    how they orchestrate or work together in workflows."
  - Dark-room problem: doing nothing to avoid surprise fails because it is
    "detrimental to everything the agent realizes it is working on or
    interested in — and it would be a huge surprise to find oneself stuck and
    trapped in a dark room." Serendipity has to be organized in terms of what
    surprisal is *on*.
  - Design patterns each deal with a tension/problem; "finding these
    interesting tensions is where value is born."
  - Eating-own-tail: the tail is the discrepancy between the system's
    behavior and some model of it — "organize and understand that
    discrepancy, not deny it and not avoid it, but use it to fuel the next
    layer of learning." Serendipity systems: see discrepant information, then
    try to make sense of it.
  - Scope of this ruling: "what we're doing now is not solving that once and
    for all, but creating something like a **preference registry** and
    seeding it with some content that would allow us to get moving." New
    preferences can then be formed, registered, and moved around — which is
    meta-preference: which preference do we prefer at a given moment.
  - Meta-preference is "kind of the dual of policy." AlphaZero already dealt
    with this — policy layer vs valuation/reward layer; worth rethinking the
    above in those terms.
- Registry transcription: to be written into `aif-equations.edn :choices`
  together with D1 and D2 once all three are ruled (one publish-window
  write, transcribed at Joe's word).

## 2. F10 / D1 — the outcome domain (which `Obs v` declarations)

- Sheet: C538 §2 (five candidates A–E), §5 recommendation.
- Question: which outcome domain(s) get declared. Recommendation:
  `Obs organisations` := terminal flight dispositions (candidate D, the only
  one with a measured policy-conditional distribution);
  `Obs evidence` := certification/update records valued by EIG, no C.
- **Ruling: PENDING-EVIDENCE (Joe, 2026-09-07)** — no ruling until a few
  examples are computed. The direction is endorsed (the §5 seed plus
  named-empty slots at nouns and verbs), but as the first direction for
  *gathering evidence*, not as policy.
- Joe's framing (near-verbatim):
  - "Anything saying 'oh well, it's not computed yet' — that's not a reason
    to prefer what's computed already. We need to develop some examples and
    be generative in our thinking here."
  - The risk of ruling now: "if we're computing over missions but we leave
    out tickets, excursions — and if we are computing over preferences but
    we leave out the complexity — ultimately we're going to become very
    formulaic, rather than computing something over what's ultimately a
    **graph structure**."
  - Inspiring example: the memory white paper's use of a **graph Laplacian**
    to measure properties of memory retrieval. "It's that kind of
    computational approach — graph Laplacians or other similar things, over
    the complex space of missions, tickets, excursions, problems — that
    would allow this to really have a robust interpretation."
  - "Right now, because of its complexity, I don't have any obvious examples
    that I can rule on."
- Evidence excursion (opened 2026-09-07): compute worked examples before
  D1 returns to Joe —
  1. a small KL[Q(o|π) ‖ C] worked example over the 14 flight dispositions
     using the measured conditionals (claude-1, direct);
  2. assemble the mission/ticket/excursion/problem graph from the futon
     repos and compute Laplacian/spectral properties over it, in the style
     of the memory white paper (dispatched).
- Evidence half 2 LANDED (codex-2, futon2 `a13278a8`, reviewed by claude-1
  2026-09-07): 479 documents (299 missions, 160 excursions, 20 problems,
  **0 tickets** — no `holes/tickets/` or `TICKET-*` artifact exists in any
  of the nine repos, recorded mechanically), 410 hyperedges, 2,078
  incidences, no clique expansion. **The graph has real wiring structure**:
  on the 350-node largest component, normalized λ₂ = 0.0410 vs null
  0.2235 ± 0.0518 (z = −3.53); unnormalized agrees in direction
  (z = −2.88); 200 degree/size-preserving rewirings, seed 54112026. Not the
  one-hyperedge degeneracy of the deployed-memory graph — more
  bottlenecked/modular than its degree sequence predicts. Caveat for any
  preference computation: 112 components, 69 no-reference documents; a
  domain over the whole corpus must state its treatment of the small
  components and isolates. Review: re-ran the full pipeline; spectral.edn
  and receipt.md byte-identical, graph.edn differed in exactly one line
  (the futon2 HEAD pin, which their own commit had advanced) — content
  deterministic; commit touched only runs/D1-evidence/.
- Still owed before D1 returns to Joe: the small worked KL example
  (half 1, claude-1 direct).
- Registry transcription: deferred with the ruling.

## 3. F10 / D2 — family of C's or one C over the tagged sum

- Sheet: C538 §3 (the two disagreeing Lean surfaces), §4 (three arms).
- Question: arm 1 (family, no join) / arm 2 (one C, family = support
  partition; recommended) / arm 3 (one domain, rest typed-absent).
  Also settles which Lean surface the model keeps
  (`Holes.lean:151` per-vertex C vs `:6778` single PreferenceDistribution).
- **Ruling: ARM 2, WITH AN EXTENSION (Joe, 2026-09-07)** — one C over the
  tagged sum, the family as support partition. AND: **lift the
  evidence-vertex exclusion.** The evidence vertex may carry preference
  mass — specifically a preference for *epistemologically valid* evidence.
- Joe's framing (near-verbatim):
  - Origin of the whole futon-2026 War Machine rebuild: "we had gathered
    evidence that the machine wrestled, but we never gathered evidence that
    it was running according to any model that had been validated, and that
    was an oversight. So there's a clear preference there for finding
    evidence that is epistemologically valid."
  - Go proverbs vs AlphaZero: the proverbs are not encoded, AlphaZero
    rediscovers them; "they are not necessarily hard and fast rules, but
    they are backed up by evidence."
  - The risk of keeping the exclusion: "a different version of the dark room
    problem, where we have loads of preferences that are effectively
    internal to the system, and no preference is related to its actual
    performance or our grasp and understanding of that performance."
- Interaction with D3 and §2a′, as claude-1 reads the two rulings together
  (flag if wrong): D3's occasion/update distinction *stands* — C_ser still
  prefers occasions at the organisations vertex, and mass on raw
  "having learned" is still the greed §2a′ warned about. What is amended is
  §2a′'s *blanket* "no C at evidence": the evidence vertex now gets a named
  support region whose preferred object is **epistemic validity** — being in
  the state where the machine's evidence about its own performance is valid
  (model-conformance witnessed, not just activity recorded) — which is a
  preference about the *relation between record and reality*, not a reward
  for information gain. EIG remains the value of learning; the C-region is
  for the validity of what is learned from.
- Lean consequence: this lands cleanly on arm 2's carrier — the single
  kernel at `Holes.lean:6778` carries no evidence-exclusion hypothesis; it
  was the *per-vertex* surface (`:151`, the one arm 2 drops) that excluded
  evidence by hypothesis. So the exclusion-lift removes a tension rather
  than creating one.
- Registry transcription: with D1/D3, one publish-window write. The §2a′
  amendment to P-validated-R5 must be a dated follow-up section in that
  file (never a silent edit), citing this ruling.

## 4. F12 / `:organise-carrier` — which carrier the O-laws are stated of

- Sheet: `C558-F12-decision-sheet.md` §1, six arms, all run.
- Question: organise returns a Cascade but the laws as proved are stated of a
  CascadeDiff value; which carrier (or function-framing) holds O1–O4.
  No arm states all four laws of one carrier without paying; the payments
  differ in kind (extra hypothesis / undeclared Score parameter / unwritable
  law / undetermined third origin).
- Unblocks (with #5): F12 slice 4.
- **Ruling: ARM 6, `:function-at-the-cascadediff-codomain`, WITH AN EXEMPLAR
  PROVISO (Joe, 2026-09-07)** — "given the complexity entailed, we need to
  find an example... let's see if it even works at all in principle, once,
  on something. We'll try to build to this specification and then see how
  it looks after that."
- Reading: the ruling chooses the carrier (all four laws jointly stated and
  discharged at the function-at-CascadeDiff signature), and commissions a
  build-to-spec exemplar before the choice is treated as validated. The
  natural exemplar is exactly what C558 §1 says would change the
  measurement: a real recorded run whose precedence moves and carries a
  score (the producer at futon3:checks/construct_cascade.clj:402 writes []
  literals today), exercised through a conformant implementation at the
  arm-6 signature — O4 non-vacuous on a run rather than only on the C59
  fixture and the ants record. F12 slice 4, once unblocked, is the
  designated place for that build.
- Registry transcription: with #5, as the pair that unblocks F12.

## 5. F12 / `:organise-sorry` — what is done with the sorry at `Holes.lean:861`

- Sheet: C558 §2, six arms, all run. The sorry is inert (zero term
  references across DarkTower).
- Question: discharge-with-any-inhabitant (refuted by the laws) /
  discharge-with-a-conformant-implementation (a proved selection between
  witnesses the record cannot distinguish) / amend-to-existence-statement
  (content already proved) / seal-opaque (evidence lost) / leave-refused
  (census line stays) / amend-the-type (= #4).
- Unblocks (with #4): F12 slice 4.
- **Ruling: DESTINATION AMEND-TO-EXISTENCE AT THE ARM-6 SIGNATURE, STAGED
  (Joe, 2026-09-07)** — Joe accepted claude-1's staged plan: the sorry's
  destination is the arm-6 conformance predicate plus `∃ f, Conformant f`
  (content already proved; preserves the docstring's refusal as a theorem);
  operationally the declaration stays refused until the slice-4 exemplar
  validates arm 6, so the Holes.lean/contract edit happens once, after the
  build-to-spec look.
- Joe's addition (near-verbatim): "when we put in a refusal, what we're
  really doing is alluding to a new design pattern, and we should write
  that down, probably in our problems library, with the typical
  if-however-then format. I can see the logic of that here, although I'm
  not going to spell it out in detail."
- DONE: the pattern is authored —
  `futon3 library/problems/deliberate-refusals-allude-to-unwritten-patterns.flexiarg`
  (futon3 `524fa95`, parse gate 59/0), source-class self-derived, with the
  organise refusal as the witnessed instance and the elaboration marked as
  claude-1's.
- Registry transcription: with #4, as the pair that unblocks F12.

## 6. F12 / `:organise-o3-field` — which field O3 reads over

- Sheet: C558 §3, three arms.
- **Ruling: ARM 3, `:over-the-nodes-organise-did-not-add` (Joe, 2026-09-07)**
  — the no-bootstrap reading, accepted on claude-1's recommendation. At the
  ruled CascadeDiff carrier the set is `selected ∪ admittedBy`, so the
  recorded run stays a witness (edge 18-19 endpoints are admitted) while
  the node-set reading's self-justification (edge 26-25) is refused by
  construction. Consequence: the slice-4 exemplar restates arm 6's O3
  clause in no-bootstrap form (slice-11 predicate, no new mathematics).
- Joe's addition on item 5's meta-pattern: the meta node is still the meta
  level; the ruling itself — the deferral, and how the build is sequenced —
  is a design pattern to write. DONE:
  `futon3 library/problems/contract-edits-wait-for-their-exemplar.flexiarg`
  (futon3 `cdb5e8a`, parse gate 60/0): registry now, exemplar next,
  contract edit once.
- Registry transcription: joins #4 and #5 in the same publish-window write
  (watcher extended before it fired).

## 7. F12 / `:organise-o4-denominator` — which rounds O4's acting order is read over

- Sheet: C558 §4, five arms.
- **Ruling: ARM 5, `:split-acting-order-over-transcript-score-over-primary`
  (Joe, 2026-09-07)** — C541 §4 option (i) as written: score over the
  gate-graded rounds (evidence against a determined oracle label), acting
  order over every transcript round (behavior the cascade does whether or
  not the record grades it).
- Joe's framing (near-verbatim): the escalating scales of things-unchanged
  are "an opportunity to learn something as we go... the system is
  ultimately going to create an escalating chain of warnings and errors or
  other signals — 'nothing has changed over this very long epoch' would be
  the ultimate one — so we better just shut this whole thing down and wait
  for someone to get us unstuck. Arm 5 seems to allow not only a complex
  orchestration across those layers, but also some opportunity for tuning
  and learning at the meta level what works. Arm 5 gets us unstuck now and
  creates the possibility for further development later."
- Consequence carried into slice 4: include the separating construction if
  reachable (transcript acting order unmoved while the score moves), since
  arm 5 is currently observationally equivalent to the transcript arm.
  This ruling does NOT decide item 8 — every non-primary TRUE measured so
  far rests on the plant item 8 is about.
- Registry transcription: joins the publish-window write (script extended
  before the watcher fired).

## 8. F12 / `:organise-o4-after-the-law-encoding`

- Sheet: C558 §5, three arms.
- **Ruling: ARM B, `:encode-a-then-a-member-already-carries`, ADMISSIBLE AS
  MARKED INSTRUMENTATION, WITH THREE RIDERS (Joe, 2026-09-07)** —
  (1) provenance marking is the rule (planted encodings labeled
  constructed-instrumentation, never corpus facts); (2) the owed un-fitting
  guard restatement gets written (scope extended to say when
  fitting-to-a-law is admissible); (3) the real fix is generative rather
  than a table repair.
- Joe's framing (near-verbatim): "the cascades are meant to correspond to
  production rule systems... patterns should have THEN statements
  available, which are authored and which can be adapted into production
  rules through an **attested interpretation**, or, if it's not attested,
  a **documented interpretation**... We're not tuning to a win; we're
  tuning to a law, or a structural requirement of how the system works. At
  the same time, we do need to exercise that and come up with some
  examples." On rider 3 specifically: it "really shouldn't be too hard" to
  chain cascades to two rule-bearing members now — the fresh @why/@how
  edges and the new problems sub-library give material "tuned to the very
  work that occupies us in this particular project."
- Examples excursion (opened, same shape as D1's): construct at least one
  two-rule-bearing cascade from the committed library, each rule's
  interpretation attested or documented; joins the slice-4 exemplar's
  scope or runs beside it.
- Registry transcription: joins the publish-window write (five rulings
  total; watcher re-armed on the post-publish gap after the 14:37 race).

## 9. F12 / `:organise-third-origin` — what fills `admittedBy`

- Sheet: C558 §6, four arms. The only witness built so far is a lookup on
  the recorded input — the field CAN be filled consistently; nothing
  computes it.
- **Ruling: ARM 4, `:support-grain-input`, WITH A DATA-AVAILABILITY RIDER
  (Joe, 2026-09-07)** — the signature gains a `Set P` attribution input
  (determination without over-committing to rule grain; inter-derivable
  both ways, so the upgrade is an indicator instantiation later). The
  attribution input joins the Score parameter in the same staged single
  Holes.lean edit.
- Joe's rider, beyond the structural decision (near-verbatim): the
  one-distinct-rule-id corpus is "just a data availability problem...
  that limitation needs to be addressed in order for any decision here to
  be meaningful. Alongside that structural issue, we need to start to
  populate the rules... my vision for how these cascades would work is
  that they're actually complex... we need to start up a **new mining
  loop** that actually goes and produces some historical cascades that we
  can use as a source of data" — reconstituting the Cascade Live problem
  hierarchies as cascades that solved them, per the mission-cloth
  precedent, upgrading the library-loop/why-how machinery.
- COMMISSIONED: `holes/labs/library-loop/MINING-historical-cascades.md` —
  sources (W1's six mapped solutions first), product shape (futon3:checks
  format, attested/documented rule interpretations, two-plus rule-bearing
  members as the scarce target), discipline (mining not fitting; exemplar
  before loop).
- EXEMPLAR LANDED AND REVIEWED (codex-18, futon2 `e059f0a5`): the dark
  policy-grain compliance solution as a cascade — 3 rule-bearing members
  with attested interpretations (corpus max was 1), all witness pointers
  verified at pins, admitted-set honesty kept. Format HOLDS; two format
  notes (THEN-span convention, admission-evidence standard) and the zai
  loop role spec recorded in the MINING note.
- Registry transcription: joins the publish-window write (six rulings —
  every C558 choice now decided).

## 10. (forming, not yet asked) F11 — the two readings of F4

- Slices 2–3 showed the readings disagree in both directions on the record.
  Not yet a registered choice; the loop is still slicing F11. Listed here so
  the walkthrough ends with a look at where it is by then.
- **Ruling: NOT YET ASKED**
