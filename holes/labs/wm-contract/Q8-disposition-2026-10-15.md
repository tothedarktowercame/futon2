# Q8 disposition: frontier certificates on the production route (2026-10-15)

Implements codex-68's rulings 1a, 2b and 3
(invoke-1792105128245-14791-ceb325e3), which answer the conflicts in
`Q8-diagnosis-2026-10-15.md`. Branch `claude-10-q5-family` from `94017b62d`.
mathlib4 commit `9805823462` on branch `darktower`.

## Lean (mathlib4, `DarkTower/WarMachine`)

- **`IncrementalTaskFrontier`** gains `Exclusion`, `EligibilityCensus` and
  `EligibilityCensus.Valid`. Under `Valid`:
  - excluded task ids have no duplicates;
  - every exclusion has a reason other than 0;
  - eligible and excluded tasks are disjoint;
  - eligible ∪ excluded = the open tasks.

  It also gains `omitted_open_task_invalid`.
- **`TargetCascadeCertificate`** gains `requirementExcluded` (ruling 1a).
  `Valid` is now:
  - available = constructed = admitted = scored;
  - excluded ⊆ scored;
  - posterior = scored \ excluded;
  - multiplicity over the selectable remainder.

  New proofs: `exclusion_outside_scored_invalid`, and witnesses
  `requirementExcludedCascade` (valid) and `foreignExclusionCascade`
  (invalid).
- **`Requirements.Q8`** has two new clauses:
  - `frontierCensus.Valid openTasks`, with the refresh and selection ranging
    over `frontierCensus.eligible` (ruling 2b);
  - `selectedCascade.requirementExcluded ⊆ q6Excludable r`. Here
    `q6Excludable` is `{previousChoice.cascade}` when the previous outcome was
    `refused` on the same input digest, and ∅ otherwise.

  New decided theorems:
  - `excluded_with_reason_Q8`, `q6_excluded_Q8` (true);
  - `omitted_open_task_not_Q8`, `invented_eligible_task_not_Q8`,
    `unreasoned_exclusion_not_Q8`, `exclusion_without_q6_not_Q8`,
    `exclusion_of_another_policy_not_Q8` (false).

  The existing Q8 theorems still hold.
- **Build.** `lake build DarkTower.WarMachine.Requirements
  DarkTower.WarMachine.RequirementsReader DarkTower.WarMachine.OuterTaskObjective`
  succeeds. Its two warnings (an unused variable in
  `historical_all_target_breadth_not_a_premise`, and a simpa lint in
  `OuterTaskObjective`) were both there before this change.

## Runtime (futon2)

- **Outer frontier, ruling 2b.** `meta-live-outer-selector/select-live` calls
  `q8-frontier/outer-frontier` on the exact candidates and effective
  selection it used, and stores the sealed result at `:q8-frontier` on the
  outer receipt.
  - **Census.** All received tasks are open. Eligible means the selector
    compared it (`:automated-feasibility` not `:infeasible`). Every excluded
    task carries the selector's own `:ineligible-reason`, or
    `:automated-feasibility/infeasible`, and has no key.
  - **Key.** `Σ_{c∈Ca}(a_c − m)`, summed exactly with BigDecimal. It is
    encoded as `(k + 2^64)·2^1074`, a fixed monotone injection on dyadic
    rationals. For complete-union comparisons this is order-equivalent to the
    pairwise G (derivation in the diagnosis).
  - **Pairwise check.** Every unordered eligible pair must be recorded exactly
    once and its recorded winner must agree with the keys. The selected task
    must be the strict key minimum.
  - **Not recomputable** (each typed):
    - `:pairwise-key-order-mismatch`;
    - `:pairwise-incomplete`;
    - `:pairwise-duplicated`;
    - `:selected-not-strict-key-minimum`;
    - `:ranking-law-not-scalar` (the `:shared-only` / shadow mode);
    - `:channel-value-not-exact`;
    - `:no-outer-selection`.
- **Policy stages, ruling 1a.** `select-over-families` records, for each
  target, the structural reading-policy keys:
  - **constructed:** every family policy. A policy that fails scoring keeps
    the key of its arranged candidate; if even that cannot be compiled, it is
    named `[:wm/reading-policy-unkeyed target id]`.
  - **scored:** every policy with a computed G.
  - **admitted:** the ranked policies after F attachment and certification.

  The decision stores these as `[:selection-certificate :policy-stages]`.
- **Refresh, ruling 3.** The runner closes each run in `run-stream.edn` with
  its sealed frontier. The next run's Q6 predecessor carries it as
  `:frontier`.
  - **Classification.** Entries are compared by task and by dependency
    digest: one for the task's observation (channels and support), one for the
    ranking law with m.
    - A changed observation means the prior entry is invalidated, and its
      task is recomputed at the new epoch.
    - Otherwise the entry is retained literally, keeping its prior epoch.
    - Tasks that left the frontier are removed; tasks that joined are added.
  - **Not recomputable:**
    - first run: `:no-prior-frontier`;
    - tampered prior: `:prior-frontier-tampered`;
    - an unaffected entry that recomputes differently:
      `:retained-entry-changed-without-dependency-change`.

  This is an audit of a full recomputation. It is not learning, it does not
  feed the selector, and no incremental reuse is claimed.
- **Carrier.** `q8-frontier/certificate` is built in `persist-run-record!` and
  stored as `:q8`, sealed and bound to:
  - the outer frontier's seal;
  - the predecessor's seal;
  - the Q6 carrier's seal;
  - the chosen target.

  The cascade's `requirementExcluded` comes only from the decision's intact Q6
  carrier. That carrier must:
  - be bound to this run's predecessor (else `:q6-carrier-stale`);
  - be justified by it (reason `:previous-choice-refused-on-same-inputs`, a
    refused previous outcome, equal digest, and the excluded key equal to the
    previous choice; else `:q6-exclusion-unjustified`);
  - lie within `scored` (else `:q6-exclusion-outside-scored`);
  - not have removed anything without status `:excluded` (else
    `:q6-removal-without-exclusion`).
- **Retained decision.** `persist-run-record!` now keeps the selected decision
  when a completed selection closes on the `:no-selection` cell. This is every
  reading-route click today, because execution semantics are owed. Previously
  the record's `:decision` was empty for these runs, so nothing could be
  recomputed from it. Genuine abstentions are unchanged.
- **Exporter.** `wm-run-facts/q8-facts` uses `q8-frontier/verify-record`. The
  `:q8` carrier must be intact and equal to the carrier recomputed from the
  record alone (`record-inputs`); anything else is NR. The old carrier paths
  `[:certificates :q8]`, `[:controlled-sut :certificates :q8]` and the
  terminal receipt are no longer read.
- **Lean rendering.** `wm_check_run_facts` renders `frontierCensus`, with an
  unreasoned exclusion rendered as reason 0, and `requirementExcluded`. The Q8
  block carries the Q6 facts when they are known. With no exclusion and Q6
  not known, it states `previousOutcome := .invalid`, which makes
  `q6Excludable` empty.

## Evidence

**`wm.q8-frontier-route-test`: 7 tests, 71 assertions, 0 failures, 0
errors.** Each run goes through `run-opportunity!`, with outer selection by
`select-live` in live mode (complete-union, m = 1.0) and the real
reading-family decision. Selector calls: 1 per run.

- **Positive** (`second-run-certifies-q8-with-the-q6-exclusion`):
  - Run 1 is NR with `:no-prior-frontier`, and persists the epoch-0 frontier.
  - Run 2 is certified and `verify-record` accepts it.
  - Census: eligible `M-current`, `M-other`, `M-third`; excluded `M-off` with
    `:pipeline/not-on-current-map`.
  - The 3 pairwise comparisons all agree with the keys.
  - Everything is retained.
  - Cascade: constructed = admitted = scored (2 policies), requirementExcluded
    = {run 1's key}, posterior = the other policy.
  - The mirrored Q8 holds, and all four exporter fields are known.
- **Refresh** (`refresh-classifies-changed-added-and-removed-tasks`):
  - a changed priority invalidates and recomputes exactly `M-third`;
  - an added `M-new` is the only recomputed entry, and the other three are
    retained;
  - removing `M-third` puts it in `removed`;
  - Q8 holds in each case.
- **Adversarial:**
  - **Census forgeries, consistently resealed:** an omitted open task, an
    invented eligible task, and an unreasoned exclusion each pass the record
    binding, and each is refused by Q8.
  - **Pairwise and key:**
    - a flipped winner gives `:pairwise-key-order-mismatch`;
    - a missing pair gives `:pairwise-incomplete`;
    - shared-only gives `:ranking-law-not-scalar`;
    - a string channel value gives `:channel-value-not-exact`;
    - a non-minimum selection gives `:selected-not-strict-key-minimum`;
    - a stored key edited and resealed is NR (not bound).
  - **Q6:** another policy, an extra exclusion, a stale predecessor binding, a
    tampered carrier and a removal without exclusion are all NR, each with its
    own reason.
  - **Carriers:**
    - an edited carrier is `:q8-carrier-tampered`;
    - a resealed carrier, or the previous run's carrier in this record, is
      `:q8-carrier-not-bound-to-record`;
    - a refresh that omits an entry is NR, and its content also fails Q8;
    - an absent carrier is `:q8-carrier-absent`.
  - **Tampered prior frontier** in the stream: `:prior-frontier-tampered`.

**Planted controls.** Each was applied alone, the namespace run, then
reverted (`~/notes/q8/plants/`):

| plant | failures |
|---|---|
| P1 runner does not attach `:q8` | 28 |
| P2 census omits the selector's exclusions | 6 |
| P3 eligible = all tasks | 6 |
| P4 reason read from `:reason` | 6 |
| P5 pairwise check skipped | 2 |
| P6 Q6 exclusion dropped from the cascade | 6 |
| P7 verify accepts an unbound carrier | 5 |
| P8 refresh does not recompute added tasks | 15 |

**Lean.** The fixtures are generated from runner exports
(`test/fixtures/q8-lean/generate.clj`) and checked with `lake env lean`:

- `Q8First`: Q8 unverifiable;
- `Q8Repeat`: Q8 holds, as do Q5 and Q6;
- `Q8Changed`: Q8 holds;
- with P6 applied (`~/notes/q8/lean/Q8ExclusionDropped.lean`): Q8 violated.

**Affected controls,** branch vs base `94017b62d`:
- branch: 454 tests, 2783 passes, 329 failures, 25 errors;
- base: 448 tests, 2709 passes, 329 failures, 25 errors.

The failing and erroring tests are identical. Almost all are in
`full-loop-runner-test`, with 7 failures in
`uniform-run-record-test/offline-tick-record-preserves-validity-and-missing-prefix`;
all were there before this change. They cover the interpreted route and
fixtures, not this change. The branch adds 6 tests and 74 passes.

Migrated assertions:
- `wm-run-facts-test`: unbound Q8 carriers are now NR. This replaces two
  tests that accepted any well-shaped carrier.
- `integrated-controlled-sut-test`: the controlled projection is no longer
  exported as Q8.

**Static gates.**
- clj-kondo, changed files excluding the in-ns generator: 0 errors; 2
  warnings, both already in base.
- check-parens OK.
- `git diff --check` clean in both repositories.

Not run: full CI, live click, hot load.

## Open

- **Ruling 2b is defined only for live mode.** The scalar key exists only for
  `:complete-union`. With the outer-learning authority in shadow mode
  (`:shared-only` pairwise), Q8 is NR with `:ranking-law-not-scalar`.
- **Run 1 after deployment** has no prior frontier, so it is NR.
- **Runs that never close** leave the next run without a prior frontier, so
  that run is NR as well.
- **Normalised channels.** Priority and freshness are normalised over the
  on-map population. Any change to the population's min or max therefore
  invalidates every entry using that channel. This is correct, but it means
  the "retained" count will often be zero.
- **Key size in Lean.** The keys are ~340-digit naturals, which is fine for
  `decide` at fixture scale. The live frontier (about 104 eligible tasks) has
  not been checked in Lean.
- **The retained-decision change** makes reading-route records carry their
  selection-law and certificate. Other exporter facts for those runs (Q1, Q3)
  now compute instead of being NR.
