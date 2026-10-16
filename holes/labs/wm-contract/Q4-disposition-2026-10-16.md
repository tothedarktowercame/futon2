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

Superseded by the amendment below.

## Amendment: per-policy horizons under certified normalization

Requested by codex-68 (invoke-1792109255347-14874-109ac40e). futon2 branch
`claude-10-q5-family` from `9cd91ee65`; mathlib4 `darktower` from
`9805823462`.

**Ruling (codex-68).** Each compared policy keeps its own horizon. Mixed
horizons are comparable only because the scorer normalizes every policy's
G terms the same declared way, `:per-step-capacity-and-pattern`:
risk/(H·log outcome-count), ambiguity/H, information gain/pattern count.
G is then a controller objective over per-step rates, not raw cumulative
EFE. Per-policy horizons are not accepted when the normalization is absent,
differs between policies, is unrecognized, or is not bound to the G terms
that were compared.

### Lean (`DarkTower/WarMachine/Requirements.lean`)

- `RunFacts.horizonLength`, `preferenceSteps`, `gradedPreferenceSteps` and
  `preferenceSemantics` are removed. They are replaced by
  `policyPreferences : List PolicyPreference`, one record per compared
  policy: policy id, its own horizon, steps, graded steps, semantics,
  `normalization : GNormalization`, and its evaluated `terms`.
- `GNormalization` has two constructors: `perStepCapacityAndPattern` and
  `rawCumulative` (the unnormalized horizon sum). An absent or unrecognized
  declaration has no constructor, so it is NR at export.
- `Q4` now requires:
  - the run-level terms and counts, as before;
  - every record's policy is compared, every compared policy has a record,
    and there are as many records as compared policies (no omission, extra
    or duplicate);
  - each record is `Valid`: all three terms evaluated, a positive horizon,
    a preference at every step `0..H-1`, and grading at every step under
    progressive semantics;
  - one common normalization across the records, and equal horizons unless
    that law is `perStepCapacityAndPattern`.
- New theorems, all `by decide`:
  - holds: `good_Q4_mixed_horizons` (H = 4 and 2, per-step) and
    `equal_horizons_raw_Q4`;
  - fails: `mixed_horizons_raw_not_Q4`, `mixed_normalization_not_Q4`,
    `omitted_policy_record_not_Q4`, `foreign_policy_record_not_Q4`,
    `duplicate_policy_record_not_Q4`, `policy_term_missing_not_Q4`.

  `good` now compares 2 policies rather than 1911. No other requirement
  reads `comparedPolicies`, and `decide` over 1911 records would not
  finish. The `deterministicTerminal` and `progressiveRowsMissing`
  witnesses are restated per policy.

### Runtime

- **Scorer** (`cascade_observation_scoring`): `g-normalization-laws` names
  the recognized laws with their versions. `normalize-g-terms` computes the
  terms from a declaration and the raw sums; the scorer itself uses it, so
  recomputation is exact. Each scored entry records
  `:normalization-declaration` (law, version, H, outcome count, pattern
  count). `cascade_shape_g/score-arranged` copies the declaration and the
  raw sums into the scored record (`:g-normalization`, `:raw-g-terms`).
- **Receipt** (`reading-policy-preference/certify`): the receipt carries
  `:normalization {:declaration :raw :terms :fit-ambiguity}`. Certify
  refuses a declaration that is unrecognized, whose H, outcome count or
  pattern count are not the record's, or that does not reproduce the
  scorer's terms. A record with no declaration is certified with
  `{:status :absent}`, and the Q4 carrier refuses it.
- **Carrier** (`q4-preference`, schema `:wm/q4-preference-v2`): one entry
  per post-Q6 compared policy: structural key, H, steps (τ−1), graded
  steps, semantics, `{:law :version}`, and term booleans. Run-level term
  conjunction and counts are kept. There is no longer a mixed-horizon
  refusal. A policy's normalization refuses as follows:
  - `:normalization-absent`;
  - `:normalization-unrecognized` (unknown law or version);
  - `:normalization-inputs-not-the-receipt's` (H or outcome count);
  - `:normalization-not-bound-to-g-terms`, when any of these fails:
    - the declared normalization of the raw sums equals the bound terms;
    - risk + (ambiguity + fit ambiguity) − information gain equals the
      compared G within 1e-9;
    - every recorded finite term equals its normalized value.

  Two recognized laws in one comparison are recorded, and Q4 fails on
  them.
- **Exporter**: `policyPreferences` comes from the verified carrier only.
  On the reading route, the four run-wide fields are NR ("per-policy
  horizons: see policyPreferences"); they stay exported for the history
  table and alert text. Other routes export `policyPreferences` as NR,
  because they carry no normalization declaration.
- **Report card**: the Q4 verdict mirrors the amended Lean Q4.
- **Lean term generator** (`wm_check_run_facts`): renders
  `policyPreferences`.

### Evidence

**`wm.q4-preference-route-test`: 4 tests, 90 assertions, 0 failures,** on
the production runner seam (`run-opportunity!`, one selector call per
run). The tests ran with `load-identity/required-sources` pointed at the
worktree, as `generate.clj` does; otherwise the runner refuses on source
drift from the canonical checkout.

- **Positive** (H = 2, 2 policies): one record per compared policy, with
  steps and grading `[0 1]`, progressive, and
  `{:law :per-step-capacity-and-pattern :version 1}`. Every receipt
  carries the scorer's declaration. The exporter's `policyPreferences`
  matches `comparedPolicies`, `horizonLength` is NR, and the report card
  gives `:pass`.
- **Q6:** 2 certified, 1 compared, 1 record; Q4 holds.
- **Mixed horizons** (`p/a` + `p/c`, H = 2 and 3): certified, steps
  `range H` per policy. Q4 holds and the report card gives `:pass`.
  - Both receipts re-declared raw cumulative (consistently): certified,
    Q4 fails, the report card gives `:fail`.
  - One receipt re-declared: Q4 fails.
- **Forgeries,** each with the carrier recomputed:
  - missing or non-finite term: certified, Q4 fails;
  - normalization absent (removed, or the scorer's `{:status :absent}`):
    `:normalization-absent`;
  - unknown law or version: `:normalization-unrecognized`;
  - declared H not the receipt's: `:normalization-inputs-not-the-receipt's`;
  - raw sums edited, normalized terms not those of the compared G, or a
    recorded term not the normalized one: `:normalization-not-bound-to-g-terms`;
  - the previous round's forgeries keep their reasons.
- **Zero term:** a zero information gain, forged consistently (raw sum,
  term, receipt G and certificate row G), still certifies, and Q4 holds.
- **Carrier forgeries** (omitted, extra and foreign policy records):
  the mirrored Q4 fails, and the exporter refuses them with
  `:q4-carrier-not-bound-to-record`.

**Planted controls** (`~/notes/q4n/plants/`), each applied alone, run
against the route test, then reverted:

| plant | failures |
|---|---|
| N1 scored record does not retain the declaration | 41 failures, 5 errors |
| N2 carrier skips the normalization checks | 12 |
| N3 unknown law dispatched as per-step | 0 (the guard in front of the dispatch already rejects it) |
| N3b the recognition guard removed | 2 |
| N4 raw-to-terms binding skipped | 2 |
| N5 Q4 mirror drops the common-law clause | 2 |
| N6 Q4 mirror drops compared-identity equality | 2 |
| N7 report card accepts any law | 1 |
| P2 zero term treated as absent | 2 |

**Lean.** `Requirements.lean` and `RequirementsReader.lean` build with no
errors (`lake build DarkTower.WarMachine.RequirementsReader`). The
fixtures are generated from runner exports by
`test/fixtures/q4-lean/generate.clj` and checked with `lake env lean`:

| fixture | Q4 |
|---|---|
| `Q4Single` | holds |
| `Q4Excluded` | holds |
| `Q4MixedHorizons` | holds |
| `Q4TermMissing` | violated |
| `Q4MixedHorizonsRaw` | violated |
| `Q4MixedNormalization` | violated |
| `Q4NormalizationUnknown` | unverifiable |
| `Q4NormalizationAbsent` | unverifiable |
| `Q4OmittedPolicyRecord` (export edited) | violated |
| `Q4ForeignPolicyRecord` (export edited) | violated |

The existing Q5, Q6 and Q8 fixtures (7 files) elaborate unchanged, with Q4
NR as before. Their NR reason strings still name the retired fields; they
are strings, not fields, and were not regenerated.

**Affected controls,** 62 namespaces, branch against base `9cd91ee65`,
same harness:

| | tests | assertions | failures | errors |
|---|---|---|---|---|
| branch | 807 | 8253 | 355 | 30 |
| base | 807 | 8215 | 355 | 30 |

The failing tests are identical by test name and file. They are the
pre-existing failures, mostly in `full-loop-runner-test`.

**Static gates.**
- clj-kondo: 0 errors. It reports 5 warnings, the same 5 as base.
- check-parens: OK.
- `git diff --check`: clean in both repos.

Not run: full CI, live click, hot load.
