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
- **Ruling: PENDING**

## 3. F10 / D2 — family of C's or one C over the tagged sum

- Sheet: C538 §3 (the two disagreeing Lean surfaces), §4 (three arms).
- Question: arm 1 (family, no join) / arm 2 (one C, family = support
  partition; recommended) / arm 3 (one domain, rest typed-absent).
  Also settles which Lean surface the model keeps
  (`Holes.lean:151` per-vertex C vs `:6778` single PreferenceDistribution).
- **Ruling: PENDING**

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
