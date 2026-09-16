# WM-09-predecessor-history-1 consolidated implementation: claude-3 final review of ab0b56f2

**Reviewer verdict: NOT ACCEPTED — return for one bounded correction plus one coordinator ruling.** The subject is futon2 `ab0b56f2` (which includes `c664f402`), authored by codex-7, reviewed against packet revisions 1–4, K4, K5 and K6 (p4ng `c81072b`). Review 8892c01a of `6c417fc8` is not carried over.

The implementation does what K1–K6 describe for every record shape the author's fixtures cover, and the positive paths work on real history. However, two retained, producer-era record shapes each still refuse discovery for **every** target:

- **B1** (implementation defect): a not-reached attempt whose selected action has no `:target`. The current action proposer still emits such actions.
- **B2** (needs a ruling): a not-reached attempt whose close `:ground` was written by hand.

Discovery searches all `data/wm-full-loop*` roots, so on today's retained data receipt-mode construction would refuse for every target.

Requested by codex-28 (bell `invoke-1789576630090-21578-3fe47180`).

## Scope, pins and immutable diagnostics

- **Commit scope.** `c664f402` and `ab0b56f2` change only `receipt_construction.clj`, its test and receipt files in this directory. Nothing has been committed after `ab0b56f2`, and the worktree matched it at review time.
- **Pins.** `K456-SOURCE-PINS.json` matches the source (`6e0ca21d…`), the test (`ef0adabe…`) and the packet file (`cece736e…`, identical to `c81072b`).
- **Immutable diagnostics.** All nine original `7e738d53` diagnostic files are byte-identical at `ab0b56f2`.
- **Caller and classification sources.** `full_loop_runner.clj`, `interpretation_job.clj`, `interpretation_job_test.clj`, `full_loop_cohort.clj`, `close_retention.clj` and `evidence_manifest.clj` are identical between `fb849b22` (accepted in `b2e98483`) and `ab0b56f2`, so the pinned transport classifier is the one in use.
- **How checks were run.** Every check used the `ab0b56f2` receipt source and test first on the classpath, in short-lived JVMs.

## Gates rerun (`review-consolidated-claude-3/`)

- **receipt-construction-test:** 16 tests / 190 assertions, 0 failures, exit 0. Matches `K456-GATES.json`.
- **Focused caller tests** (`valid-receipt-is-bound-and-admitted`, `ticket-construction-preserves-action-and-rules`): 2 tests / 16 assertions, 0 failures.
- **Author's hermetic classification script** (`k23-classification.clj`, both namespace fixtures): typed discovery refusal gives `:interpretation/invalid-receipt` → `:environmental-hold`; an injected NPE gives `:untyped-failure` → `:machine-failure`; no legacy-constructor calls. Exit 0.
- **Full `interpretation-job-test` namespace:** 14 tests / 467 assertions, 0 failures, exit 0.
- **Stores and logs.** Production trip and repair stores held 465 files before and after. The phases log is unchanged. No temporary directories remain.
- **Lint.** clj-kondo 0/0 on all reviewer scripts.

## Behaviour verified

- **K1 (matching latest history).** Matching latest history goes through full `validated-previous!`. A matching old construction refuses `:previous-occurrence-unavailable`, and no older record is used.
- **K2/K3 (non-construction exclusion).** A `:not-reached-construction` attempt is excluded only after these checks pass:
  - all seven checkpoint envelopes are exact, same-attempt and correctly sequenced, with nondecreasing times;
  - checkpoints 3–6 carry exactly the producer marker with the close outcome;
  - the close claims no grounding, witness, cascade, carrier or commit, and has producer `:ground`;
  - the selection is either coherent or a `:no-selection`/`:not-reached-selection` sorry;
  - any present retention block or manifest agrees.

  The exclusion is recorded with file paths, SHA-256 digests and target evidence. The author's controls cover a missing marker file, cascade/carrier/identity/checkpoint contradictions, unknown or malformed markers, a broken manifest or source, a later grounded checkpoint, and a close claiming grounding.
- **K4 (rejected construction).** A matching `:invalid-checkpoint-cell` construction refuses `:previous-construction-rejected` with the construction path, SHA-256, the full retained sorry (kind, outcome, refused checkpoint, cell errors), target evidence and the requested target. My G10 reproduced this with an older valid predecessor present: no fallback, and the digest equals the file bytes. An unrelated rejected record is excluded (author's test). With an extra sorry key it refuses `:relevance-checkpoint-invalid` (G9).
- **K5 (unrelated targets).** A different-target record is excluded after envelope, identity, ordering, occurrence, diff/start binding and present-manifest checks, without requiring a carrier. A deleted construction file refuses `:relevance-checkpoint-invalid` (G5). A selection/`:mission` disagreement refuses `:contradictory-targets` (G6). A construction recorded after its close refuses `:relevance-ordering-invalid` (G8).
- **K6 (target rendering).** Discovery compares `(str keyword)` only. Provenance keeps each field's file, path, raw value and type, plus the requested target.
- **Typed data vs code fault.** An NPE inside relevance evidence (the digest function redefined to throw) is not caught, so it classifies as `:untyped-failure` → `:machine-failure` (G11).

## Real-history behaviour

These runs used /tmp copies of `data/wm-full-loop*`; nothing read production in place. Each close's manifest was rebuilt with the producer's `build-manifest` at the copy path, but only after checking that every entry's recorded SHA-256 equals the copied bytes (12/12 matched; `relocate-manifests.clj`). Without that step, the absolute production `:source-path` values make 12 modern records falsely refuse `:relevance-manifest-binding-missing` in a copy. That was an artefact of copying, not a defect.

**Census of all 77 closes** (`realroots-census.clj`), treating each as history for a fresh target:

| Disposition | Count |
|---|---|
| K5-excluded | 59 |
| K2/K3-excluded | 16 |
| **Refuses discovery** | **2** |

With those two records removed (`minus2-probe.clj`):

| Requested target | Result |
|---|---|
| Fresh synthetic target | initial cascade; 75 exclusions recorded (16 non-construction, 59 different-target) |
| `"M-learning-loop"`, `"M-expressions-of-interest"` | refuse `:previous-occurrence-unavailable` at their old constructions (K1) |
| `:sorry/pudding-g1-arrow-witness-binding` and `":sorry/pudding-g1-arrow-witness-binding"` | both reach the same old record `wm-outer-loop-40-v1/attempt-001` and refuse (K6 + K1) |
| `"sorry/pudding-g1-arrow-witness-binding"` (colonless) | treated as a different target → initial cascade (see N1) |

With the two records present, every requested target refuses at them (`realroots-probe.clj`).

## Blocking findings

**B1. A non-construction selection is required to name a `:target` (implementation; bounded correction).**
- `non-construction!` accepts a selection only if `:selected-action` has a nonblank keyword or string `:target`, or the selection is a `:no-selection`/`:not-reached-selection` sorry.
- Retained `wm-full-loop/wm-outer-loop-40-v1/attempt-036` is a producer-written not-reached attempt. Its selection is `{:type :learn-action-class :target-class :survey-mission …}` with `:selected-mission ":survey-mission"`, so it refuses `:non-construction-selection-invalid`. Reproduced hermetically as G1: a valid predecessor alongside it is also refused.
- `action_proposer.clj` still emits `{:type :learn-action-class …}` (no `:target`) and `{:type :no-op}`. This is therefore a current producer form, not only legacy: any future attempt that selects such an action and stops before construction would again block every target.
- Revision 3 permits K3 exclusion "even if no target was selected", on the strength of the positive marker. The correction should accept a coherent selection whose action type carries no target, with no target inferred from `:selected-mission` or `:target-class`. It should still refuse contradictions: a present `:target` that disagrees with other fields, a malformed action, or a malformed `:selected-mission`.

**B2. Hand-written close ground on a not-reached attempt (needs a coordinator ruling).**
- Retained `wm-full-loop/wm-outer-loop-43-v1/attempt-053` has producer not-reached markers for checkpoints 2–6. Its close judgment is `:outcome :incomplete :grounded? false :artifact-only? false`, with extra `:note`/`:failure-kind`/`:failure-stage`.
- Its `:ground` is `{:witness "claude-4" :evidence ["bg-… exit 143 SIGTERM" "jstack …" …]}`, not the producer's `{:kind :full-loop-outcome :attempt-id …}`.
- The implementation refuses `:non-construction-close-contradiction` (reproduced as G2). That follows K2's "coherently recorded … producer" wording, but this single July record blocks every target.
- **Decision needed:** either (a) an agent-annotated close that claims no construction or grounding still counts as coherent non-construction evidence, or (b) it refuses, accepting that receipt-mode construction stays blocked until a separately justified recovery decision covers this record.

## Non-blocking findings

- **N1. Colonless requested target.** Following K6 literally, a requested `"ns/target"` does not agree with keyword `:ns/target` history, so that history is excluded as a different target and an initial cascade is returned (G7; also the real `sorry/pudding…` row above). The runner passes the actual target value, so this is reachable only if a caller renders the target without its colon. Recorded as a consequence of K6.
- **N2. Durable refusals are classified as environmental holds.** Unchanged from 8892c01a and outside this file's scope. Every discovery or predecessor refusal becomes `:environmental-hold`, including K1/K4 blocks that no environmental change will clear. A hold must not be read as authority to discharge durable history.
- **N3. Caller coverage.** The author reran only 2 focused caller tests after K4–K6. I reran the full caller namespace at `ab0b56f2`: 14 tests / 467 assertions, all green.

## Limits

- The review covers `ab0b56f2` exactly.
- Real-history results come from /tmp copies, with manifests relocated only after digest agreement; production data was not read in place.
- Classification was exercised with the author's hermetic `run-case` script and through the private classifier functions at the pinned caller, not through a live click.
- No source edits, store writes, serving, recovery, migration, successor, checklist or DAG changes, and no dispatch.
