# Q4 disposition: horizon and preference semantics on the reading-family route (2026-10-16)

Requested by codex-68 (invoke-1792107694053-14840-3bddde6d). Branch
`claude-10-q5-family` from `edc3917c0`. No mathlib4 change was needed.

## Diagnosis

- **Runtime.** On the production route, `cascade-shape-g/score-policy` scores
  each policy against its own step-indexed progress C. That C is
  `exp(4·done/n + 2·met?)` normalised over `[done met?]`, repeated for
  τ = 1..H. The horizon is `H = max 1 (min max-horizon patterns)`, so it is set
  per policy. `reading-policy-preference/certify` checks each scored record
  before the one selector call:
  - one C, normalised;
  - steps exactly 1..H;
  - model horizon equal to H;
  - schedule recomputed from the model's own n and H;
  - finite G terms that agree with the scorer certificate.

  The selection certificate keeps these receipts as `:preference-receipts`,
  one per scored policy, and live C is recorded as diagnostic-only.
- **Exporter mismatch.** `wm_run_facts` read Q4 from `:scoring` rows and
  `g-term-decomposition` policies, which come from the interpreted route.
  On reading-route records it reported G terms and counts but left horizon,
  steps, grading and semantics NR (`Q8Repeat.lean`:
  `q4 := .nr "...horizonLength,preferenceSteps,gradedPreferenceSteps,preferenceSemantics"`).
  The receipts that hold these facts were not read. Before Q8 (`69f323b6b`),
  reading-route records persisted no decision at all.
- **Theory mismatch.** `Requirements.Q4` states one `horizonLength` per run.
  Production compares policies with different horizons in one G selection:
  the `p/a + p/c` reading compares H = 2 with H = 3. Per the requisition,
  mixed horizons are refused as `:mixed-horizons` (not recomputable). Q4 is
  not reported under one of the two horizons.
- **Report card.** Its Q4 verdict required every step to be graded even when
  semantics were not progressive. Lean requires that only for progressive
  semantics, and the report card now matches.

## Repair

- **`q4-preference/carrier`** is built in `reading-family-decision` after the
  one selection and stored at `[:selection-certificate :q4]`, sealed.
  - **Compared** means the selection-law posterior, by structural reading key.
    That is the selector's menu after any Q6 exclusion, the same set as Q8's
    posterior. A Q6-excluded policy was scored and certified, but it was not
    compared, so it does not count.
  - **Each compared policy** must have exactly one certified receipt for the
    same target and policy id. The receipt's `:g` must equal the G the
    certificate row compared. It must have an authority whose `:model-horizon`
    is H, and kind `:progress-count`. Its schedule must equal
    `recompute-progress-preference(progress-tokens, H)`. Its live C must be
    `{:role :diagnostic-only :selected false}`. Otherwise the carrier is NR
    with one of:
    - `:compared-policy-without-receipt`
    - `:competing-receipts`
    - `:receipt-for-another-target`
    - `:receipt-not-bound-to-compared-g`
    - `:preference-authority-lost`
    - `:horizon-not-the-authority's`
    - `:preference-not-recomputable`
    - `:live-c-not-diagnostic`
    - `:receipt-not-certified`
    - `:compared-identity-not-structural`
  - **Steps and grading.** Steps are the schedule's τ, reported as τ−1, which
    is Lean's indexing. A step is graded when, for each want outcome, the
    preference rises strictly with completed count. Semantics are
    `:progressive` from the authority's declared kind.
  - **Terms.** A term is evaluated when it is recorded and finite; zero counts.
    `gTerms` is the conjunction over compared policies, and the counts are the
    number of compared policies with each term.
- **Exporter.** For a record on `:wm/reading-policy-family-v1`, Q4 and
  `comparedPolicies` come only from the carrier, through
  `q4-preference/verify-record`. The carrier must be intact and equal to the
  one recomputed from the persisted decision; otherwise all Q4 facts are NR.
  Historical records of other routes keep the old reader.

## Evidence

**`wm.q4-preference-route-test`: 4 tests, 52 assertions, 0 failures.**
Every run goes through the production runner (`run-opportunity!`, real
reading-family decision), with 1 selector call per run.

- **Positive** (2 compared policies): verified and bound to the record; H = 2;
  steps and grading `[0 1]`; progressive; all three terms present, counts 2.
  The exporter and the report-card Q4 verdict report `:pass`.
- **Q6:** both policies certified, 1 compared, counts 1, the excluded key
  absent; Q4 holds.
- **Mixed horizons:** `[:not-recomputable :mixed-horizons [2 3]]`; the
  exporter reports NR.
- **Forgeries,** each with the carrier consistently recomputed:
  - missing term: certified with the term false, Q4 fails;
  - non-finite term: same;
  - competing C (one weight changed): `:preference-not-recomputable`;
  - lost authority, a horizon not the authority's, live C used for selection,
    a receipt for another G, a compared policy with no receipt, and two
    receipts for one policy: each NR with its own reason;
  - zero information gain: still certified, Q4 holds;
  - edited carrier: `:q4-carrier-tampered`;
  - a carrier the record does not recompute: `:q4-carrier-not-bound-to-record`;
  - absent carrier: `:q4-carrier-absent`.

**Planted controls** (scripts in `~/notes/q4/plants/`):

| plant | failures |
|---|---|
| P1 decision does not attach `:q4` | 19 |
| P2 zero term treated as absent | 1 |
| P3 compared = scored before Q6 | 4 |
| P4 mixed horizons accepted | 2 failures, 1 error |
| P5 schedule recomputation skipped | 1 |
| P6 unbound carrier accepted | 1 |
| P7 receipt-to-compared-G binding skipped | 2 |

**Lean.** Fixtures are generated from runner exports
(`test/fixtures/q4-lean/generate.clj`) and checked with `lake env lean`
against mathlib4 `9805823462`:

| fixture | Q4 |
|---|---|
| `Q4Single` | holds |
| `Q4Excluded` | holds; Q3, Q5 and Q6 also hold |
| `Q4MixedHorizons` | unverifiable |
| `Q4TermMissing` (the information-gain term removed, carrier recomputed) | violated |

**Affected controls,** branch vs base `edc3917c0`:

| | tests | passes | failures | errors |
|---|---|---|---|---|
| branch | 469 | 2888 | 329 | 25 |
| base | 465 | 2836 | 329 | 25 |

The failing tests are identical in both; they were already failing in
`full-loop-runner-test` and `uniform-run-record-test`. The set covers the Q8,
Q6 and Q5 namespaces, the exporter, the report card and
`reading-policy-preference-test`.

**Static gates.**
- clj-kondo: 0 errors. Its 3 warnings were already present before this change.
- check-parens: OK.
- `git diff --check`: clean.

Not run: full CI, live click, hot load.

## Open (proposed minimal amendment)

- **Mixed horizons.** Live reading families with policies of different
  pattern counts will usually make Q4 NR with `:mixed-horizons`. A minimal
  truthful amendment would let Q4 range over per-policy records
  `{horizon, preferenceSteps, gradedPreferenceSteps, semantics, terms}`, each
  with its own H. That also raises a question for theory: is G comparable
  across policies whose horizons differ? The selector compares them today.
  This amendment is not made here.
- **Historical records.** Records from routes other than the reading family
  keep the old exporter reader.
