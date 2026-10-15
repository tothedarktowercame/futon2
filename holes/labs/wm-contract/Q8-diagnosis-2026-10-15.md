# Q8 diagnosis: the frontier certificate on the production route (2026-10-15)

Requested by codex-68 (invoke-1792104867638-14785-c1a1f3dd). Branch
`claude-10-q5-family` at `dada0a66d`. **Stopped at two theory conflicts.**
No code changed. The packet asks to stop and report a precise conflict
rather than invent semantics, and each conflict below needs a ruling before a
runtime carrier has a definite shape.

## What Q8 requires

`Requirements.Q8` (with `IncrementalTaskFrontier`) has two parts.

**Outer task frontier.**
- `frontierRefresh.Valid`: an exact incremental partition of the prior
  frontier into retained, invalidated and removed entries. Retained entries
  are literally equal and unaffected by the delta. Invalidated entries and
  added tasks are recomputed.
- `currentOpenTasks = openTasks`, which is all open missions, excursions and
  tickets.
- `frontierSelection.Valid current openTasks`: the selected entry is in the
  frontier, the frontier covers exactly `openTasks`, and the selected entry is
  `keyLE` (`comparisonKey : Nat`, ties to the lower task id) every frontier
  entry.

**Selected cascade** (local to the selected task):
- `availableAdmissible = constructed = admitted = scored = posterior`;
- if more than one cascade is admissible, more than one is in the posterior.

## Production route as it runs

- **Outer selection.** `meta-live-outer-selector/select-live` →
  `meta-pipeline-selector/select`. The authority
  `resources/wm/outer-learning-authority.edn` has `:mode :live`, so the
  effective selection is the `:complete-union` pairwise comparison with
  `missing-channel-cost 1.0`. The winner is the unique candidate with no
  pairwise loss. No unique minimum is a typed refusal.
- **Inner selection.** `war_machine` sets `cascade-targets` to
  `[selected-task-id]` only. The reading-family field therefore holds one
  target, so the one G comparison is target-local, which matches Q8's
  cascade part.
- **Real record** `tick-run-record-2026-10-15-b8f72b63…`:
  - 698 open tasks (165 missions, 443 excursions, 90 tickets) = 104 in
    support + 594 excluded.
  - Exclusion reasons: 510 `:pipeline/not-on-current-map`; 67
    repair-finding; 15 ownership; 2 standing or source.
  - All 104 support tasks were compared: 5356 = C(104,2) pairs, all
    `:complete-union` at missing-channel-cost 1.0.
  - Selected `M-futonzero-mvp`, with `:minimum-pairwise-task-state-G`.

## Current Q8 evidence is not from the production route

- **The only producer is not on the production route.** The one producer of
  `:wm/q8-frontier-certificate-v1` is `q8-frontier-certificate/project`. Its
  only caller is `integrated-controlled-sut/run-controlled!`, whose namespace
  says it "is not referenced by the live JVM". The full-loop runner writes no
  `[:certificates :q8]`, `[:controlled-sut :certificates :q8]` or
  terminal-receipt Q8. Every production record therefore exports Q8 as NR;
  the real record above has no `:certificates`.
- **The exporter would accept any well-shaped map.** `wm-run-facts`
  `q8-frontier-facts` checks keys only. It checks no seal, and nothing ties
  the cascade sets to the record's own decision (family, admission, scoring
  or posterior). A hand-built map passes.
- **The helper uses the old identity.** The controlled helper builds policy
  identity as `[target id (mapv :id precedence)]`, the precedence-vector
  identity. That shape cannot represent the structural reading-policy key
  (see Q6).
- **There is no prior frontier.** Production recomputes the whole outer
  field every tick and persists no frontier. Nothing exists to diff a
  refresh against, and no prior frontier is a selector input.

## Conflict 1: Q6 exclusion against the cascade correspondence

On a run where Q6 triggers, the previous run's refused policy P1 is
constructed, admitted (Prefix-F, precision and preference certification) and
scored. It is then removed from the menu before the one selector call
(`038a94c0a`, reviewed in `dada0a66d`). With policies P1 and P2:

    constructed = admitted = scored = {P1, P2},  posterior = {P2}

That is exactly `twoAdmissibleOnePosterior` in `IncrementalTaskFrontier.lean`,
proved invalid there (`two_admissible_one_posterior_invalid`,
`scored_posterior_mismatch_invalid`). `q6-repeat-after-refusal-test` reaches
this state on the production runner: menu 2, removed 1, the excluded key
absent from the certificate's candidates.

None of the obvious ways out holds:

- **Move the exclusion before scoring or admission.** That breaks
  `constructed = admitted` or `admitted = scored` instead. It also breaks Q5:
  `precision-config` builds the reading-precision identity over every ranked
  policy (`:policies`). A run with P1 removed has a different identity from
  the run before, so the precision carry refuses
  `:reading-precision-identity-changed` and selection needs an explicit beta
  declaration under ruling (b). This was read from the code, not tested.
- **Keep P1 in the posterior and select among the rest.** That is selecting
  a runner-up after the selection law, which the Q6 packet forbids.
- **Leave P1 out of constructed and scored.** That misreports what ran.

On Q6-triggered runs Q8 therefore cannot hold without a change to the
requirement. Rulings that would resolve it:

- **(1a)** Amend `TargetCascadeCertificate`: add `requirementExcluded :
  Finset Id`. Then `available = constructed = admitted = scored`,
  `posterior = scored \ requirementExcluded`, and each excluded id must be
  certified by a Q6 exclusion carrier, with multiplicity required over the
  remainder. This needs a mathlib4 change.
- **(1b)** Q6 takes precedence: Q8 is reported violated on Q6-triggered runs.

## Conflict 2: the frontier covers all open tasks, but production ranks only the eligible

`SelectionCertificate.Valid` needs the frontier to be exactly `openTasks`
(698 above), with the selected entry key-minimal over all of it. Production
gives the 594 excluded tasks no comparison key: eligibility (pipeline map,
repair, ownership, standing, feasibility) is a filter applied before ranking.
A frontier entry for each of them needs a key.

- **Proposed encoding.** Production behaviour is exactly lexicographic:
  ineligible after eligible, then the complete-union cost. That gives every
  open task a key, and the selected task is unchanged. I have not adopted it,
  because it states a ranking law for tasks that production never ranks; it
  needs a ruling.
  - **(2a)** Accept the lexicographic key, named in the entry's
    `rankingLaw`.
  - **(2b)** Amend Q8 so the selection ranges over the eligible frontier,
    with eligibility certified per task. This needs a mathlib4 change.
- **The cost part needs no ruling.** Under `:complete-union` the pairwise
  comparison is an entry-local scalar order. For candidates a and b over the
  channel union Ca ∪ Cb, the difference `g(a) − g(b)` equals
  `Σ_{c∈Ca}(a_c − m) − Σ_{c∈Cb}(b_c − m)`, because the channels each lacks
  contribute m to both sides. So `key(a) = Σ_{c∈Ca}(a_c − m)` orders exactly
  as the pairwise winner relation, and it does not depend on which other
  tasks are present.
  - The Lean `comparisonKey` is a `Nat` (`%d` in `wm_check_run_facts`).
    Doubles map to it monotonically through their IEEE sortable bit pattern.
  - Production sums each pair's channel set in hash-set order. The producer
    must therefore check the key order against the recorded `:pairwise`
    winners and refuse typed on any disagreement, not assume it.
- **Ties.** Production refuses when there is no unique minimum, while Lean
  breaks ties by task id. This is consistent on every run that selects.

## Refresh and the learning-claim standard

`RefreshCertificate.Valid` can be met by a full recompute. The prior frontier
is persisted, and the next run reads it from the run stream. An entry
counts as retained when its dependency observations are unchanged and it
recomputes to the identical entry. The producer refuses typed when an entry's
value changed with no changed dependency, which means a dependency was left
out.

The prior frontier is not a selector input in production, so the chain
observation → durable write → next-run lookup → changed selector input →
changed decision does **not** apply to Q8 as built. The refresh certificate
would audit exactness against the prior run; it would not be learning.
Whether Q8 needs actual incremental reuse (the selector consuming retained
entries) is a third ruling. Population-wide inputs make every key that uses
them depend on one shared observation, so any change to one invalidates all
of those entries. These inputs are the outer-learning belief (rebuilt from
the run-record manifest every run), the preference costs and the pipeline
snapshot.

## Not done

No producer, exporter change, test or Lean fixture. Each would encode a
choice from conflicts 1–2. No full CI, no live click, no hot load.
