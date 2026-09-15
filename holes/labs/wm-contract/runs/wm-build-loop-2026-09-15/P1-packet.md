# P1 — the belief about the selected work target reaches the predictor

claude-2, 2026-09-15, under Joe's build commission relayed by codex-28
(`invoke-1789501050276-21222-59c12ada`). **Status: proposed; implementation
held on design decisions D1–D3 (codex-28).** Checklist: WM-02, R1; interfaces
to WM-01, WM-03, WM-04, WM-05. Cascade nodes: WM-02 Q1, Q2, Q4, Q5, Q6, Q7,
Q8, Q9; WM-05 C1, C2; WM-04 O1; WM-01 K6.

## 1. The real work situation

- **Occurrence.** Last record of `futon2/data/wm-trace/wm-trace-2026-09-12.edn`
  (SHA-256 `3b25d2d4…e175`, 4 records), timestamp `2026-09-12T17:28:09.498448087Z`.
  This is the latest retained trace; the 2026-09-15 click record
  (`tick-run-record-fb96bf07…`) carries no `:mu-post`.
- **Selected target.** `:advance-mission` on `M-shared-memory-control-build-test`,
  decision fields `:phase "complete"`, `:non-progress-count 4`,
  `:non-progress? true`, `:non-progress-decay 0.2`. Registry today:
  `:status-class :unknown`. The mission file's own status line: "Phases 1–3
  ACCEPTED live; Phases 4–7 ACCEPTED dark/shadow; Phase 8 advice-only ACCEPTED
  replay, enactment rungs operator-gated". WM says complete, the registry says
  unknown, and the mission says enactment is gated. Which state this mission is
  in is exactly the question the target belief should hold.
- **Candidate policy at the prediction boundary.** Today it is only the
  action-grain entry `{:type :advance-mission :target "M-…"}`. No (cascade,
  precedence) candidate exists before selection: `full_loop_runner` constructs
  one cascade *after* the controller selects (TN four-link trace, G3/F3/O3).
  The full candidate is therefore a WM-08/09 dependency, not something P1 can
  supply.
- **Outcome meaning (WM-06).** The ruled terminal disposition of the attempt's
  close, twelve-wide (`ruled_outcome_c.clj`): five seeded positive
  (`:grounded-change` 1/2; `:agent-unavailable`, `:build-failed`,
  `:incomplete`, `:no-selection` 1/8 each) and seven named zeros. The evidence
  vertex is unruled.
- **Information/time boundary.** The strategic belief after carry
  reconciliation and the morning-brief fold, before ranking
  (`war_machine.clj` `wm-belief-pre` → judge → trace `:mu-post`).
- **Model revision.** The belief is produced under the filter manifest
  (`belief.clj`: legacy A `observation-model-v1`, B `transition-model-v1` =
  7×7 identity, D `initial-prior-v1` = uniform). The row-7→8→9 chain uses
  `{:id "wm-status-transition" :revision "row-8-declared-v1"}` with declared
  B `wm-status-action-prior-v1`. These are **different B's** (see D4).

## 2. Existing producers and consumers, and the first demonstrated gap

Producers of the strategic belief:

- `war_machine.clj:6114-6119`: domain = stack-annotation `:sections` ids ∪
  sorry-registry ids. `bootstrap-from-stack-annotations` turns a read failure
  into `[]` without a record (`belief.clj:505-508`). `reconcile-belief-carry`
  gives new entities the uniform fresh prior and **drops** entities absent from
  this tick's bootstrap (`belief.clj:510-522`). This is mirrored in Lean as
  `MachineBeliefState.carriedOnlyEntityIsDropped` (mathlib4 `2b22eeef31`).
- Morning-brief QA events are applied only when their entity is in the domain;
  the rest are held (`war_machine.clj:863-879`). The retained record holds 15,
  all `:strengthened`, all for `full-loop/implementation/<sha>` entities, and
  none for missions or tickets.
- `belief/update-belief` initialises an untracked entity with `uniform-prior`
  before applying the event (`belief.clj:423-`). It is reached by
  `forward_model.clj:361` on the prediction side.

Consumers of the target's belief:

- Row 7 `machine-belief/belief-state-distribution`: **no production caller**
  in futon2 `src`/`scripts` or futon3c `src`.
- `war_machine.clj` `previous-selection-non-progress?` reads
  `[:mu-pre target]` and `[:mu-post target]` for the selected mission. Both
  are nil in the retained record, so `mu-moved?` is false by construction and
  any repeated same-target selection counts as non-progress. This is an
  existing consumer silently reading an absent value as "belief did not move".
  P1 does not change it, but after P1 it would read a real row.

**First demonstrated gap.** The reproducer is `p1-reproducer/reproduce.clj`,
read-only, run in a tooling JVM against futon2 `93837a1e`, with pins in
`p1-reproducer/source-pins.sha256` and output in `p1-reproducer/readback.edn`:

| Probe | Result |
|---|---|
| `:mu-post` domain | 417 entities (414 `arxana/stack/…`, 3 sorry); **0 of 276** registry ids |
| The selected target `M-shared-memory-control-build-test` | `:missing-entity` |
| Live missions `M-G-wm-wiring`, `M-a-sorry-enterprise`, `M-action-cost-modelling` | `:missing-entity` ×3 |
| Live tickets `T-car3-phase2-impl`, `T-car3-queue-routing-design-note`, `T-cx-new-blocks-emacs` | `:missing-entity` ×3 |
| Positive control `arxana/stack/futon-v1/leaf/2/2` | `:ok`, seven-status posterior |
| Negative control `X-claude-2-not-a-registry-id` | `:missing-entity`, **the same refusal as the registry targets** |

The last row is itself a defect. The reader cannot distinguish "outside the
declared domain" from "in the domain with no carried row", and WM-02 Q4
requires those to be different outcomes.

## 3. The proposed build, conditional on D1–D3

Applied patterns: `wm-prediction/meaning-before-carrier` (the per-status work
meaning, D1); `aif/declare-the-conditioning` (the snapshot boundary above);
`wm-prediction/belief-keeps-its-lineage` (introduction, carry, archival);
`wm-prediction/observation-can-disagree` (no registry label as evidence, D3);
`wm-pattern-design/same-policy-through-comparison` and
`wm-prediction/joint-belief-needs-dependence-authority` (single-target scope
only where the candidate declares it); `wm-prediction/probability-laws-with-runtime-meaning`
(unchanged v1.1 admission, no renormalisation);
`wm-prediction/outcome-meaning-across-models` (the same row reaches row 9).

**Inputs:** the registry (`load-missions`, `load-tickets`: id, status-class,
path and the SHA-256 of the bytes read); the previous record's `:mu-post` plus
the new `:belief-lineage`; the declared work-target D (D2).

**Outputs, in the trace record:** work-target rows in `:mu-post`, keyed by
the exact controller `:target` string; `:belief-lineage {id {:introduced-at
:D {:name :revision :authority :declared-prior} :predecessor
:admitted-updates :domain-authority}}`; a typed domain refusal
(`:registry-unreadable`) in place of a silent fallback.

**Invariants:**

1. Belief keys equal the controller's `:target` strings exactly
   (`mission-target-id` normalisation for missions, `T-` ids for tickets).
2. A work target is introduced once, with the named declared D recorded in
   its lineage. It is never re-initialised and never dropped by carry.
3. A target whose lineage records introduction but whose predecessor row is
   missing gets a typed refusal (`:carry-missing`), not D.
4. No update is applied to work-target rows in P1 (D3).
5. `update-belief`'s untracked-entity uniform branch refuses for work-target
   ids and cannot create their rows.
6. Stack-section and sorry-entity behaviour stays byte-identical (regression).
7. The domain declaration lets the reader refuse
   `:entity-outside-declared-domain` separately from `:missing-entity`.

**Rejecting controls:**

- The reproducer's registry probes flip to `:ok`, with lineage showing D at
  introduction.
- The negative control refuses with the distinct domain kind.
- A target leaves the live set (for example, it completes) and its row
  survives the next tick.
- A reopened target carries its retained row.
- A missing predecessor row is refused.
- An unreadable registry is refused.
- An untracked update is refused.
- The stack regression passes.
- One mechanical row-7 → row-8 → row-9 join on a work target, under the
  existing declared-prior model. It is labelled mechanical: its policy is a
  declared action sequence, not a cascade.

**Acceptance evidence:**

- Namespace tests for the listed controls.
- A replay of the pure bootstrap/carry function over the retained previous
  record, with before and after readbacks.
- Gate receipts: clj-kondo, check-parens and the relevant namespace tests.
- If D2 changes the carry rule, a corresponding Lean `MachineBeliefState`
  revision with its axiom printout (WM-01 K6). The existing
  `carriedOnlyEntityIsDropped` would otherwise stop describing the runtime.

No click, reload or serving activation is part of P1.

## 4. Downstream constraints P1 must not foreclose

- **WM-06/13:** the target's state must be one from which A can reach the
  twelve-wide disposition support. P1 fixes no A, and the placeholder
  `declared-outcome-a` (state *i* → outcome *i* mod *n*) must not be read as
  one.
- **WM-08/09/14:** the policy scope that row 7/9 check comes from the
  candidate. Today the candidate is action-grain with one target. When
  interpreted cascades exist, a cascade whose consumes/produces touches more
  entities must get `:joint-construction-required`, which rows 7 and 9 already
  refuse. P1 must not narrow a scope to fit.
- **WM-10/11/12:** every candidate in a comparison reads the same snapshot.
  The lineage must let a later horizon-T prediction name the exact starting
  row.
- **WM-07:** the D of work targets is a state prior, not a parameter
  posterior. `:parameters` hypotheses stay separate.
- **WM-03:** see D4, the same-B requirement.
- **Provenance from the first record:** introduction, D identity and domain
  authority are retained from the first tick that introduces a target. No
  historical rows are backfilled.

## 5. Roles

Author: codex-2. Independent code reviewer: claude-2. Design authority for
D1–D3: codex-28. Scope: the bootstrap/carry/lineage for work targets, the
row-7 domain refusal, and the untracked-update refusal. Result: an existing,
committed, reviewed path by which the actual post-update belief row for the
selected target reaches row 7, with retained lineage.

## Design decisions required

See the bell reply of `invoke-1789501050276-21222-59c12ada`, reproduced in
`LEDGER.md`: D1 state meaning, D2 domain and D, D3 update authority;
recorded for later packets, D4 same B and D5 separate dynamics admission.
