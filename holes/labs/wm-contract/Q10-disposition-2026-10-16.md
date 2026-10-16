# Q10 disposition: same-pattern arrangement controls (2026-10-16)

Requested by codex-68 (invoke-1792116451048-15014-ccc25699). Branch: futon2
`claude-10-q5-family` from `378fb3668`; mathlib4 `9e79af1bee`, unchanged.

## Diagnosis

`Requirements.lean` (`Q10`, RunFacts fields at lines 210-213):

- `differentArrangementPairs`: compared cascade pairs with the same pattern
  set and different arrangements;
- `arrangementPairsDistinguishedByG`: those pairs represented as distinct
  policies, each with its own numeric G;
- `Q10 := 0 < differentArrangementPairs ∧ distinguished = differentArrangementPairs`.

Lean reads two counts and no identities, so the identity and completeness
work has to be done by the carrier that produces them.

Before this change: the exporter emitted both fields as not recomputable
on every route ("census absent"); the report card and the Lean term
generator already computed Q10 from the two numbers; no runtime code
recorded pairs.

**The real population.** On the reading-family route a policy is a
structurally keyed arrangement (`[:wm/reading-policy-v1 target id {:nodes
[[occurrence pattern] …] :edges [[from to kind] …] :order […]}]`), formed
for the selected task only (Q2). A probe of the production click
(`~/notes/q10/probe.clj`, log beside it) found controls that production
forms by itself, not ones a test seeds:

| reading | constructed | same-pattern pair | compared |
|---|---|---|---|
| p/a p/b | precedes(a→b) [reading-alternatives], overlap(a,b) [retraction] | yes | both (run 1); one (run 2, Q6) |
| p/a p/b p/c | two arrangements of a, b, c | yes | both (run 1); one (run 2) |
| p/a p/c | {a,c} [reading]; {a,b,c} [retraction through the Steiner point p/b] | no | both |

In each control the two members carry different numeric G (p/a p/b:
2.157 for precedes and 1.237 for overlap).

**No theory/runtime conflict.** The theory's population (compared pairs)
exists on the runtime route and can be enumerated from retained identities,
so I implemented it instead of stopping. Readings I settled myself:

1. *Pattern set* is the set of pattern ids in the key's nodes; multiplicity
   belongs to the arrangement.
2. *Arrangement* is the key's nodes, edges and order. When each pattern
   occurs once in a policy, occurrences are renamed to their patterns
   before comparison, so `[p/a 0]→[p/b 1]` and `p/a→p/b` are the same
   arrangement. Otherwise the census compares occurrences and records
   `:compared-by :occurrence` on the pair. Order is included because it is
   part of the key and determines the co-apply units the scorer reads.
3. *Own numeric G* means exactly one certificate candidate row bound to
   the policy's key, finite, and equal to the G the family summary reports
   for that policy id. The census records whether the two values differ
   (`:g-differs`) but does not require it, because the Lean docstring asks
   for "distinct policies, each with its own numeric G". The field name
   `…DistinguishedByG` could be read as requiring the values to differ. If
   Theory means that, the change is one clause.
4. Only compared pairs count, as the Lean says. A constructed pair with a
   member that was not compared is enumerated and refused with that
   member's typed reason; it is never dropped silently.

## Runtime (futon2)

`src/futon2/aif/q10_arrangement_census.clj`, sealed by the runner at `:q10`
after `:q2` and `:q9`:

- **Pins.** The population is the verified Q2 carrier's entry for the
  chosen task. The census pins Q2's `reading-id`, `graph-id`, `library-id`,
  `kernel-id` and the Q2 carrier's sha256. A Q2 carrier that does not
  verify (stale reading, graph, slice or kernel; omitted or invented
  policy; hidden failure) makes Q10 not recomputable with Q2's reason.
- **Stages.** From the decision's `:policy-stages` and Q6 carrier,
  through `q8-frontier/cascade`, the reader Q8 uses. When the record's Q8
  carrier is certified, its stages must be identical. A bound,
  typed-not-recomputable Q8 carrier (run 1: no prior frontier) does not
  block the census. An absent, tampered or unbound Q8 carrier does.
- **Members.** Each constructed key gets one of these statuses:
  - `:compared` (in the selection-law posterior);
  - `:requirement-excluded` (Q6, with the carrier's reason);
  - `:not-scored` (with the scorer's recorded `:family-failures` entry);
  - `:not-keyed` (Q2's `:policy-arrangement-not-keyable`).

  Anything else is `:hidden-refusal` and makes the census not
  recomputable.
- **Population checks** (each not recomputable, typed): constructed
  duplicated; compared duplicated; keyed constructed ≠ Q2 policies;
  compared ≠ the stages' posterior; compared ⊄ constructed (foreign);
  compared ≠ scored − Q6-excluded; selected not compared.
- **Pairs.** Every unordered pair of keyed members over the same pattern
  set is enumerated:
  - `:refused :arrangement-equal`;
  - `:refused :member-not-compared` (with the members' statuses and
    reasons);
  - `:control`, `:distinguished true|false` (with the G refusal when
    false).

  `:counts` gives the controls and the distinguished controls;
  `:selected-in-control` records whether the selected policy belongs to a
  control.
- **Abstention, refusal, repeat-refusal.** These decisions record no
  `:policy-stages`, so the census is not recomputable
  (`:policy-stages-absent` or Q2's reason), not "0 pairs".
- `verify-record` recomputes the census from the record without its
  `:q10`. The exporter (`wm_run_facts/q10-facts`) reads the two counts only
  from a verified census on the reading route; any other carrier, or any
  other route, exports not recomputable with its reason. The report card
  and the Lean generator needed no change.

## Evidence

**`wm.q10-arrangement-census-route-test`: 8 tests, 129 assertions, 0
failures,** driven by production clicks (`run-opportunity!`, select-live,
the real reading-family decision), the Q8 route test's seam.

- **Arrangement reading:** occurrence renaming gives equal arrangements; a
  different edge kind gives different ones; a repeated pattern is
  compared by occurrence.
- **First click (p/a p/b):** census verified; population = Q2's two
  policies, both `:compared`; pins = Q2's entry and sha256; one control
  `{p/a, p/b}` (precedes vs overlap), each member's G its own candidate
  row, values differ, selected policy in the control. Export `1, 1`; Lean
  term rendered; report card passes. Q2 and Q9 still verify.
- **Repeat click:** Q6 excluded run 1's refused choice; the census types
  it `:requirement-excluded` with `:previous-choice-refused-on-same-inputs`,
  refuses the pair as `:member-not-compared`, and the certified Q8
  carrier's stages are identical. Export `0, 0`: Q10 fails.
- **p/a p/c:** the retraction adds the Steiner point p/b, so the pattern
  sets differ: no pairs, `0, 0`, fails.
- **Typed refusals, counted:** a member with a recorded scoring failure
  is `:not-scored` and its pair refused (`0, 0`); a compared member without
  a numeric G, or whose G disagrees with its family summary, leaves the
  control undistinguished (`1, 0`, fails).
- **Planted controls on the record, census resealed by a consistent
  forger, each not recomputable with the reason checked and the exporter
  NR:**
  - omission from the stages: `:constructed-population-not-q2's`;
  - duplication: `:constructed-policy-duplicated`;
  - a foreign compared identity: `:foreign-compared-policy`;
  - a hidden refusal (a member dropped from scored and posterior with no
    failure): `:hidden-refusal`;
  - compared outside the stages: `:compared-not-scored-minus-q6-excluded`;
  - Q2 census omitting a policy: `:construction-policy-omitted`;
  - stale reading pin: `:construction-reading-not-the-field's`;
  - stale graph pin: `:construction-graph-not-the-field's`;
  - Q2 carrier edited: `:q2-carrier-tampered`;
  - Q2 carrier resealed: `:q2-carrier-not-bound-to-record`;
  - Q8 stages changed and resealed: `:q8-carrier-not-bound-to-record`;
  - Q8 carrier absent: `:q8-carrier-absent`;
  - Q6 carrier edited: `:q6-carrier-absent-or-tampered`;
  - selected policy not compared: `:selected-policy-not-compared`;
  - another route: `:not-the-reading-family-route`.
- **Carrier forgeries:**
  - absent, tampered or wrong schema: absent or tampered;
  - counts, a dropped pair or pins resealed, or another run's census:
    `:q10-carrier-not-bound-to-record`.
- **Abstention (stale reading):** not recomputable.

**Planted bugs** (`~/notes/q10/plants/plant.py`, logs beside it), each
caught by the route test:

| plant | failures + errors |
|---|---|
| P1 runner does not attach `:q10` | 45 |
| P2 arrangement ignores edge kind | 18 |
| P3 omission from Q2's population unchecked | 2 |
| P4 duplication unchecked | 4 |
| P5 foreign compared identity unchecked | 2 |
| P6 hidden refusal typed as not-scored | 4 |
| P7 Q8 binding ignored | 8 |
| P8 G not checked against the family summary | 2 |
| P9 verify accepts an unbound census | 8 |
| P10 exporter skips verification | 21 |
| P11 refused pairs counted as controls | 2 |
| P12 Q2 carrier not verified | 10 |
| P13 Q6-excluded member counted as compared | 6 |
| P14 selected-not-compared unchecked | 4 |

**Lean.** `test/fixtures/q10-lean/generate.clj` exports through the real
runner; each fixture checked with `lake env lean` (mathlib4 `9e79af1bee`):

| fixture | Q10 | others |
|---|---|---|
| `Q10First` | holds | Q2, Q6, Q8 unverifiable (no prior frontier); Q7, Q9 violated |
| `Q10Repeat` | violated | Q2, Q4, Q5, Q6, Q8 hold; Q7, Q9 violated |
| `Q10DifferentSets` | violated | |
| `Q10WithoutOwnG` | violated (1 pair, 0 distinguished) | |
| `Q10Missing`, `Q10Tampered`, `Q10HiddenRefusal` | unverifiable | |

Regenerating gives byte-identical files except `Q10Repeat`, whose Q6
`previousInputDigest`/`currentInputDigest` change from run to run (they
stay equal to each other, so no verdict changes). That comes from the
existing Q6 input digest, not from Q10.

**Focused branch vs base** (`~/notes/q10/focused.txt`: the Q9 list plus
the Q10 route test; base `378fb3668` in `~/worktrees/futon2/q10-base`):
- branch: 157 tests, 1294 passes, 0 failures, 0 errors;
- base: 149 tests, 1165 passes, 0 failures, 0 errors.

The Q10 test was rerun alone after the last lint edits: 8 tests, 129
assertions, 0 failures.

**Static.**
- clj-kondo, changed source and test: no new warnings (`full_loop_runner`
  keeps its one warning from base). The generator script has the two
  unresolved-namespace warnings that the in-ns script pattern always
  gives (the Q9 generator has more).
- check-parens: OK on the five changed files; it catches a planted
  unmatched bracket.
- `git diff --check`: clean.

Not run: full CI, the full affected-namespace sweep, a live click, a hot
load. The committed Q2, Q8 and Q9 Lean fixtures predate the census and
still show Q10 as not recomputable; I did not regenerate them.

## Open

- **Q10 and Q8 do not hold on the same click in this fixture.** Q8 needs
  a prior frontier (run 2). On run 2, Q6 removes the refused run-1 choice,
  which was half of the only control, so Q10 fails there. That is the
  definitions agreeing, not a defect, but the bounded representative run
  will show Q10 true only on a click whose compared set still holds both
  members of a same-pattern pair.
- **Production controls depend on the Steiner question from Q2.** A
  control appears when the retraction over a reading adds no pattern the
  reading did not name. If retraction were restricted to the slice (one
  answer to the Q2 Steiner question), controls would become more common.
- **"Distinguished by G"** is read as "own numeric G" (the docstring);
  requiring different values is a one-clause change if Theory wants it.
- **Abstention, refusal and repeat-refusal** are not recomputable, not
  "0 pairs", because those decisions keep no policy stages.
