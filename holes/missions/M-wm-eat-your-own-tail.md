# Mission: WM Eat-Your-Own-Tail — a self-improvement loop that bootstraps itself

**Date:** 2026-10-23
**Status:** OPEN — BOOTSTRAPPING — this mission constitutes a self-improvement loop that bootstraps itself: its own running-stack observations (below) name its first changes, and each change widens what the next run can observe.
**Owner:** zai-2 (author) + zai-6 (reviewer)

## What this is

The full-loop machinery (futon2 `wm-full-loop` cohorts) runs attempts,
adjudicates them, and writes records — but nothing in the machinery
*consumes those records to change the next runs*. This mission closes
that tail-to-mouth joint: the loop eats its own run observations.

Enacts pattern `agent/eat-your-own-tail`, occurrence 0 (cascade digest
d005b2f842e2922e6fc530320e22f32362a2a41cc5fc3165dcd1b316f547ff7a,
request e9cf965864d7cc315fd883f8a4ffe0c53427c431884b0653174981e2b268df24):
consume `:running-stack-observations`, produce
`:machinery-changes-for-next-runs`.

## Bootstrap seed: the first running-stack observation

`scripts/observe_full_loop_runs.py` (committed with this mission) digests
every recorded outer-loop cohort's adjudicated outcomes into
`data/wm-full-loop/observed-outcomes.edn` (a fixed point of the run
records; `--check` proves it). Its first reading:

- 7 cohorts, 62 attempts total.
- **54 of 62 attempts carry no 007-closed record at all** — the loop's
  own adjudication layer never landed for the overwhelming majority of
  recorded attempts (cohorts 40–43 are entirely unclosed).
- Outcomes that did land: 4 `grounded-change`, 2 `incomplete`,
  1 `build-failed`, 1 `substrate-unavailable`.

That is the bootstrap: the loop's first self-observation already names
its first improvement target — the closed-record gap — rather than being
handed one from outside.

## Acceptance checklist

- [x] An observer consumes the running stack's own attempt records and
      emits a deterministic, fixed-point digest
      (`scripts/observe_full_loop_runs.py`; `--check` passes).
- [ ] The digest is read by a machinery component *before* the next run
      (not only by operators), and a change to the loop is justified by
      citing digest numbers.
- [ ] The no-closed-record majority (54/62) is either closed (records
      recovered or re-adjudicated) or typed (a named reason per cohort
      for why no adjudication exists).
- [ ] A subsequent cohort's decisions cite this mission and the digest,
      closing the loop: observation -> change -> observed change.

## Discipline notes

- Workarounds forbidden (workspace AGENTS.md): if an invariant blocks a
  change the loop wants, surface the conflict here; never route around.
- Attribution: futon2's branch is `main`; an auto-sweep can commit dirty
  files unsigned — hold this mission's commits in one signed parcel.
