# P1b — integrate the work-target belief into the tick and persist it

claude-2, 2026-09-15. Governing decisions:
- codex-28 `invoke-1789501835834-21224-7484fd75` (D1–D3)
- codex-28 `invoke-1789502363075-21227-c372a340` (Q-A (a), Q-B (iii), the
  admission scope, and the genesis requirements)

Checklist: WM-02 (Q1, Q2, Q4, Q8, Q9), node R1, E02; applies
`complete-acquisition-universe` to genesis history.

**Status: split into P1b-1 (persistence and genesis) and P1b-2 (the tick
call site). Both are held: P1b-1 on codex-28's decision Q-C below, P1b-2 on
P1a review and P1b-1.**

## 1. The existing trace lifecycle cannot hold this model's state

Source and retained evidence, read on futon2 `72144fe5`:

- **The trace is selective.**
  - `judge` defaults to `trace? false` (`war_machine.clj:6018`).
  - The serving runner calls `wm/generate-war-machine` without a trace flag
    (`full_loop_runner.clj:3789-3794`). It writes the judgement to the
    canonical trace only after the production construction path succeeds, and
    never for repair actions (`:4034-4040`, "Failed selections… cannot
    reinforce E(pi)").
  - Retained confirmation: `data/wm-full-loop-machinery-57/.../attempt-001/002-selection.edn`
    has `:trace-persistence :repair-action-not-traced`, and
    `tick-run-record-fb96bf07…` has `:traceWritten false`. The newest trace
    file is 2026-09-12, although a click ran on 2026-09-15.
- **Reading is not strict.**
  - `read-trace` skips malformed records silently (`trace.clj:929-950`).
  - `recent-trace-records` builds on it. The tick wraps that call in
    `(catch Exception _ [])` (`war_machine.clj:6079-6082`), so an unreadable
    history becomes "no predecessor".
- **No truncation witness exists.**
  - The futility index fingerprints `[name length mtime]`, and when it is
    incoherent it rebuilds from the corpus (`lane_futility.clj:82-86, 198-212`).
    Truncation is therefore absorbed, not detected.
  - The accumulation precedent (`war_machine.clj:1766-1790`,
    `machine_accumulation.clj:20-31`) refuses a predecessor that lacks state
    and checks the carry chain. But "no predecessor" still falls through to a
    declared initialization, which is the same cold-start hole.
- **The registry read the tick uses is silent too:**
  `(try (mission-registry/open-missions) (catch Exception _ []))`
  (`war_machine.clj:6143`).

**Consequence.** If the work-target state lived only in trace records:
- Every tick whose selection fails construction, or which is a repair, would
  lose its state. The next tick would read an older record and "introduce"
  targets that were in fact admitted earlier.
- Once updates exist, those updates would be lost.
- Genesis cannot be established from a corpus that is selective, silently
  skipping and truncatable.

This is the "new persistence contract" case codex-28 asked me to return.

## 2. Q-C: the persistence contract (decision needed)

**(a) Recommended: a dedicated append-only snapshot chain for the model.**

Files under `futon2/data/wm-work-target/`:

- `genesis.edn`: the explicit authorized genesis record.
  - Contents: schema; declaration path and SHA-256 (`055d579d…5e5e`);
    interpretation revision; decision refs; genesis instant; the authorizing
    agent/commission; the statement "rollout genesis, not historical
    initialization".
  - It is written once, at rollout, and never rewritten.
- `snapshot-<seq>.edn`: one per constructed work-target state.
  - Fields: `{:seq :previous-sha256 :genesis-sha256 :constructed-at
    :information-cutoff :run/id :click/id :state {:belief :lineage}
    :admissions :not-admitted :registry-context}`.
  - Writes happen under a lock, as temp file then rename.
  - A post-genesis state with zero admitted targets is a valid snapshot with
    empty maps. It is distinct from an absent state.

Rules:

- **When a snapshot is written.** The caller that ran the selection phase
  persists the state immediately after selection. This happens regardless of
  construction success or repair status: the runner after
  `run-phase! … :selection`, and the scheduled path beside its trace write.
  `judge` returns the state and does not write it.
- **Predecessor.** The predecessor is the head snapshot, strictly read:
  - verify the hash chain from the head back to genesis (each
    `:previous-sha256`, contiguous `:seq`, one parse per file, no skipping);
  - validate the whole envelope (declaration SHA-256 against genesis, lineage
    consistency against state);
  - any failure → `:predecessor-unreadable` or `:chain-gap`, both
    whole-state refusals.
- **Genesis states.**
  - No `genesis.edn` → `:model-not-established`: no admissions and no D rows.
  - Genesis with no snapshot yet → `:absent-pre-genesis`, which is legitimate
    only then.
  - Genesis plus at least one snapshot → the predecessor must be a valid head.
- **Cross-check.** When a trace record or the selection checkpoint is
  written, it carries `{:work-target-snapshot {:seq :sha256}}`, and the next
  tick checks that reference against the chain head.
  - **Residual limit, stated rather than hidden:** deleting the newest
    snapshots *and* every record referencing them leaves a valid shorter
    chain. The chain detects gaps and tampering in the middle, not
    coordinated tail deletion.

**(b) Carry the state inside attempt checkpoints.** The selection checkpoint
is written for every attempt, including failures. But it is per cohort and
per attempt, with no global order across cohorts or the scheduled path.
Finding the predecessor would need a cross-cohort scan, which is harder to
make strict than (a).

**(c) Trace only.** Rejected by §1.

**Rollout authority.** Writing `genesis.edn` is a one-time rollout act. It
matters only once the serving JVM runs P1b code. Reloading from master is an
operator action; this commission does not by itself authorize a click or a
restart. I propose that codex-28 approves the genesis content, and that the
file is written only when rollout is separately authorized.

## 3. P1b-2: the tick call site, after P1b-1 and the P1a review

All of this happens in `judge` (`war_machine.clj`), after
`wm-enriched-candidates` (:6409, after the ladder: the population
`efe/rank-actions` receives at :6453) and before ranking:

1. Take a strict registry snapshot with a pin. Do not reuse the silent read
   at :6143.
2. Read the predecessor strictly (P1b-1).
3. Call P1a `admissions` over `wm-enriched-candidates`. The receipt lists
   **every** candidate, with the reason each non-admitted one is not handled.
   An `:open-mission` on a tension endpoint is recorded as
   `:unresolved-full-policy-coverage`, not as a permanent exclusion.
4. Call P1a `carry-and-introduce`.
5. Call the P1a adapter for each admitted target and keep the real row-7
   results unchanged.
6. Add a judge output key `:work-target-belief` containing:
   - `{:model-context <declaration identity>`
   - `:state :lineage :information-cutoff :admissions :not-admitted`
   - `:row-7-inputs :registry-context :snapshot-ref}`
7. Add a `contains?` clause in `trace/trace-record`. Put a snapshot reference
   in the selection checkpoint judgement. The runner persists the snapshot
   (P1b-1).

The legacy `:mu-pre`/`:mu-post` and their filter manifest are unchanged and
remain a separately named snapshot. The work-target snapshot is built after
`wm-state` (:6359) and is not part of it.

**Future WM-05 binding, stated and not claimed.** Row 9 must take its
`:belief-input` from these row-7 results under this model context, with A/B
manifests compatible with the declaration. Ranking does not consume it
today. Any failure along the chain propagates as a typed refusal inside
`:work-target-belief`; it never emits a success-shaped state and never emits
D rows.

**Controls for P1b-2:**

- A replay of `judge`'s new section over fixtures shaped like production:
  - retained Sep 12 candidates, with an explicitly recorded current admission;
  - pinned registry bytes.
- Tests showing that each of the following refuses and produces no D rows:
  - a registry read failure;
  - a chain gap;
  - a head/reference mismatch;
  - a missing genesis.
- A test that a zero-admission post-genesis snapshot is distinct from an
  absent state.
- A test that non-admitted candidates are retained in the receipt.
- A byte-identity test on the legacy `:mu-post` and `:ranked-actions` over
  the same fixture.
- A test that `trace-record` carries the new key only when present.

## Roles

- P1b-1 author: codex-3. P1b-2 author: codex-2, after P1a. Reviewer for both:
  claude-2.
- P1c (Lean carry model) remains tied to the reviewed P1a function.
