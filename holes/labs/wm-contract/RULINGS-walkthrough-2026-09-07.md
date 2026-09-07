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
- **Ruling: PENDING**

## 5. F12 / `:organise-sorry` — what is done with the sorry at `Holes.lean:861`

- Sheet: C558 §2, six arms, all run. The sorry is inert (zero term
  references across DarkTower).
- Question: discharge-with-any-inhabitant (refuted by the laws) /
  discharge-with-a-conformant-implementation (a proved selection between
  witnesses the record cannot distinguish) / amend-to-existence-statement
  (content already proved) / seal-opaque (evidence lost) / leave-refused
  (census line stays) / amend-the-type (= #4).
- Unblocks (with #4): F12 slice 4.
- **Ruling: PENDING**

## 6. F12 / `:organise-o3-field` — which field O3 reads over

- Sheet: C558 §3, three arms.
- **Ruling: PENDING**

## 7. F12 / `:organise-o4-denominator` — which rounds O4's acting order is read over

- Sheet: C558 §4, five arms.
- **Ruling: PENDING**

## 8. F12 / `:organise-o4-after-the-law-encoding`

- Sheet: C558 §5, three arms.
- **Ruling: PENDING**

## 9. F12 / `:organise-third-origin` — what fills `admittedBy`

- Sheet: C558 §6, four arms. The only witness built so far is a lookup on
  the recorded input — the field CAN be filled consistently; nothing
  computes it.
- **Ruling: PENDING**

## 10. (forming, not yet asked) F11 — the two readings of F4

- Slices 2–3 showed the readings disagree in both directions on the record.
  Not yet a registered choice; the loop is still slicing F11. Listed here so
  the walkthrough ends with a look at where it is by then.
- **Ruling: NOT YET ASKED**
