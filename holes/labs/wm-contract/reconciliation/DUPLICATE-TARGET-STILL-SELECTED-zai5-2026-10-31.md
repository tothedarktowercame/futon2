# DUPLICATE-TARGET-STILL-SELECTED — zai-5 discovery (2026-10-31)

Discovery only; no source edits. Record examined:
`data/wm-runs/tick-run-record-2026-10-31-0ba3370c-1cc7-4f7d-8537-7edbf6a9ba9c.edn`
(sha256 `6e37349a…badb8`). Prior run:
`…4955b6e6-b818-42ea-b00e-7354a71d88e2.edn` (sha256 `168440e8…d767`,
index row `:record-sha256 "168440e8…"` in `data/wm-runs/outer-refusal-index.ednl`).

## 1. Why the Q6 menu shows 1 when the field holds 544 — level confusion, not a bug in Q6

The two numbers count different things:

- **544/545** is the OUTER field: `[:outer/selection :outer-field/admitted]`
  lists **545 targets** (counted from the record; the row's 544 is off by
  one or excludes the novelty term `T-outer-target-novelty-term`), the
  target-level population produced by `outer-select/select`
  (`outer_select.clj:38-71`, `known-term-field` partition).
- **1** is the INNER menu: the Q6 exclusion carrier's `:menu` is
  `(count ranked)` / `(count (:entries sealed-menu))` — the scored
  **cascade-policy candidates for the already-chosen target**
  (`previous_run.clj:239,272-289`). Outer selection had already fixed
  `M-typed-memories`; only one policy survived admission for it
  (`policy-ed6ff8783a9a9b045f59` — R6 `:singleton-admissible-candidate`,
  R14 `:singleton-policy-posterior` in the loop-node exercise).

So Q6 never sees the 545 targets. It sees one policy AFTER the target is
picked, and it did nothing because `:status :not-triggered,
:reason :previous-outcome-not-refused` — the previous run (4955b6e6)
closed `:already-satisfied`, and `decide-exclusion`
(`previous_run.clj:198-241`) excludes only on a REFUSED previous outcome.
An already-satisfied close is invisible to Q6.

## 2. Where the duplicate guard runs, and the supported pre-selection point

The duplicate guard is `effective-run-history/compare-index` +
`require-novel!` (`effective_run_history.clj:37-52`), invoked at the
`:effective-run-comparison` phase of the runner
(`full_loop_runner.clj:8848-8876`) — i.e. **AFTER outer selection, after
construction, before author**. It fired correctly in 0ba3370c:
`:failure {:kind :outer/duplicate-effective-run :stage
:effective-run-comparison}`, because 4955b6e6's index row is a blocking
row (`:status :indexed`, `:finished-work? true`, comparable identity:
same target, giraffe-manifest `5cf3904e…`, policy-id, mission-source
`fa9566f…`, base-head `d5adbceb0`). But firing there only aborts the run
~40 s of selection too late, and it cannot stop the same target being
offered again next click — `outer-select`'s inputs (focus/relation/facts/
field/state-counts/ending-counts/gain-counts) **do not include the
refusal index or effective-run history at all**; its only exclusion is
term-graph based (`:no-known-graph-members`).

Supported points that could exclude BEFORE selection picks:

- **Outer admission** (`outer-select/select`, `known-term-field`): the
  natural place — a target whose latest index row is a blocking row
  (finished-work? true) with a comparable effective-run identity is not
  admissible to the field until its identity changes.
- **Q6 pre-selection exclusion** (`decide-exclusion`): it already reads
  the predecessor carrier; a blocking finished-work row on unchanged
  inputs is the already-satisfied twin of its existing
  `:previous-choice-refused-on-same-inputs` arm.

Secondary defect found (same run, named, left): the duplicate refusal
closes **invalidly** — `run-output {:outcome :build-failed}` with close
errors `[:unknown-outcome]`, `:invalid-close-outcome` wrapping the
refusal (`record :failure :error "invalid close outcome"`,
`:cause [ex-data {:failure-kind :invalid-close-outcome :outcome
:build-failed :errors [:unknown-outcome]}]`). `require-novel!` throws
`{:outcome :refused …}` but the close path downgraded it to a
`:build-failed` outcome the terminal semantics don't know. So even the
late guard's refusal is recorded mangled.

## 3. What sets :finished-work?, and what an unmeasured-wants already-satisfied close should record

`outer_refusal_history.clj:57`:
`:finished-work? (terminal-injury/finished-work? record)` —
`meta_terminal_injury.clj:118-124`: true when the close-outcome image is
`:changed`, OR `:already-satisfied` AND
`producer-qualified-already-satisfied?` (kinds agree across failure/
detail/run-output and the stop-line producer-qualified check passes —
grounded-commit present, author-wait stage). 4955b6e6 was a
producer-qualified already-satisfied close (existing artifact = mission
text at futon3c `62baed0b`), so `finished-work? true` **even though the
four selected wants were never measured** (mission-observation refused;
D1 not-exercised in the 4955b6e6 matrix).

What it should record: declaration-presence is not finished work. The
row should carry the distinction explicitly — e.g.
`:satisfaction {:evidence :declaration-presence :wants-measured false}`
— and `finished-work?` (which drives `blocking-row?`) should be false,
or a separate `:blocking?` should require measured wants, until a run
actually observes the wants true. An already-satisfied-with-unmeasured-
wants close is evidence to verify, not a terminal blocker; as recorded
it poisons the duplicate guard into blocking a target whose work was
never measured.

## 4. What the next click does

Nothing changes: outer-select scores the same inputs, M-typed-memories
is top of the field and is picked again; ~40 s of selection; Q6
not-triggered (previous outcome not refused); construction; then
`require-novel!` refuses `:outer/duplicate-effective-run` again; the
close mangles it to `:build-failed/:unknown-outcome` again. An infinite
blocked loop — hence OPEN; BLOCKS NEXT CLICK.

## Smallest fix (for a separate packet)

Two independent smallest fixes; either unblocks the loop, both are owed:

1. **Pre-selection exclusion (primary):** in `outer-select` admission
   (or `decide-exclusion`, whichever the owner judges smaller), exclude a
   target whose latest outer-refusal-index row is a blocking row
   (finished-work? true, comparable identity) — typed refusal when the
   field empties, which forces the outer level to the next target.
2. **Honest finished-work (correctness):** `finished-work?` must not be
   true on a declaration-presence already-satisfied close with unmeasured
   wants; record the satisfaction evidence kind on the row (1-row schema
   addition; old rows readable via absence = unmeasured).

Fix 2 alone unblocks this specific loop (the 4955b6e6 row stops
blocking), fix 1 prevents the class. The `:invalid-close-outcome`
mapping of the duplicate refusal is a third, smaller defect worth a row:
`require-novel!`'s `:refused` outcome should survive the close intact.

No source edits made; run-stream, ledger and canonical stores untouched.
