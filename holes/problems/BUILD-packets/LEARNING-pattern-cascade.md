# Learning build: reviewed pattern and cascade revision between runs

2026-09-11. Development direction selected by Joe in the current session.
Parent: `holes/labs/wm-contract/runs/LEARNING-design-proposed-2026-09-09.md` §5.
Design basis: futon3c `3f4edccf`, APM V4 `WM-ALIGNMENT-2026-09-11.md`.

Joe's instruction, verbatim excerpt:

> It's more like the next generation of the run will learn from the previous generation.

## Intended behavior

A completed run leaves both its task evidence and a reviewable diagnosis of
what should change in the patterns or cascade it used. A reviewed revision is
published against an exact parent version. A later run consumes that revision
and supplies evidence of correct use, refusal, failure or unknown outcome.
Generation means a subsequent execution with pinned inputs, not a new agent
species or an assumption that all runs improve.

The Futon2026 backlog can supply eligible development tasks. Preserve each
task's original problem, acceptance criteria and stop-line precedence. Completing
a convenient proxy or making a pattern easier to retrieve does not establish
that the original problem was solved. Revision proposals are ordinary work;
they do not create a second scheduler or bypass mission/admission rules.

## Build sequence

1. **Episode-to-proposal adapter.** Use existing pattern-registry receipts,
   retrieval evidence and actual runner artifact/review records. Bind one
   diagnosed gap to the original task, pattern occurrence, source revision and
   intended behavioral change. Start with one existing pattern's applicability
   contrast; absence of enough evidence yields an explicit incomplete diagnosis.
2. **Review and publication.** Bind independent executed review to candidate
   bytes. Publish through the configured canonical library authority with stale
   parent refusal and immutable history. Pin source, examples, index and policy
   separately. Assign worker, reviewer and publisher responsibilities through
   ordinary roles; do not introduce a permanent TA. New pattern admission or
   changes to authored relations require their own explicit review.
3. **Next-run consumption.** Make source/index/policy revision part of the
   consumer receipt and cache identity. Reuse existing organisation semantics;
   retain selected, authored-closure and policy-admitted node origins. Keep task
   decomposition separate from authored relations. Record what reached the
   worker, what was read, which conditions were checked and what was executed.
4. **Composed development episode.** Freeze a positive task and an inapplicable
   contrast before revision, then test old/new versions using separate worker
   executions and independent result review. Exercise real publication/readback
   and consumer boundaries in disposable storage before live installation.
   Preserve failed and unknown uses. Feed supported diagnoses into another
   proposal rather than crediting every pattern for whole-run success.
5. **Empirics output.** Emit one row per original task/revision/use: prediction,
   source and consumer hashes, evidence, contrast result, cost, task result and
   remaining uncertainty. Report latency and repeated serialization/retrieval
   cost as efficiency measurements, separately from correctness and usefulness.

## Acceptance and scope

The first implementation is complete when a real retained episode produces a
reviewed revision and a distinct episode demonstrably consumes its exact bytes,
with independently checked use or refusal. This establishes the revision cycle;
improved performance requires the comparison evidence and is allowed to fail.

Controls must reject stale-base publication, wrong-review hashes, old cached
revisions, citation-only execution, unsupported authored edges, collapsed repeated
pattern occurrences, and missing observations mislabeled false or successful.
Runtime invariants and any corresponding Lean statements must identify their
actual receipt/consumer correspondence; a theorem about a cascade alone cannot
certify that the running process supplied it.

This work occupies the existing learning component. It does not replace q/A/E
parameter-learning contracts or settle their outstanding modeling choices.
GFlowNet proposal sampling is optional future machinery, not a surrogate for
the revision/consumption cycle. Current RUN4 admission and repair evidence keep
their existing meanings; this packet adds no retrospective launch condition.

Status: selected for development; adapter, publication integration and composed
experiment not yet implemented. No live run or learning improvement is claimed.
