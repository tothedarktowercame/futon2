# D 2c: execution evidence producer discovery

Discovery only, at `db756f2eee45692b070c7b684db8b6edf551b5a0`. No producer,
actuation, reload, new authority or positive admission fixture was added.
Source hashes and checked anchors are in `source-evidence.json`.

## Finding

The execution path offers an existing occurrence identity and corroborated
Git/review records. It does not yet bind those records to the admitted token
observation domain or establish an executed cascade transition. A producer
alone cannot close EV-enactment: its independent verifier and next-tick reader
join must be built together. E2b's production refusal remains correct.

## Actual path and available evidence

All line references below are in `src/futon2/aif/` unless qualified otherwise.

| Site | Evidence and limitation |
| --- | --- |
| `full_loop_runner.clj:4004`, `close_retention.clj:70` | Before construction, mint once: run/cohort/attempt, action ID, transition ID, selected action value and its digest. Reuse this identity; do not mint a parallel enactment identity. D currently uses `wm-live-selection-<time>`, so the record must explicitly bind these distinct identifiers by the same retained decision. |
| `full_loop_runner.clj:1447` | Cascade construction retains selected action, precedence and interpretation/construction receipts. This is a construction contract, not execution of those patterns. |
| `full_loop_runner.clj:4079–4121` | Canonical judgment trace is written before author dispatch. Construction checkpoint subsequently writes selected=selected. Neither is a post-execution witness. |
| `full_loop_runner.clj:4286–4309` | Ordinary action dispatch is an Agency author job, with target and prompt. Recovery reuses an older author job. A done job proves neither every planned pattern ran nor each predicted token was produced. |
| `full_loop_runner.clj:680–782`, `4366–4411` | Fresh-artifact binding corroborates a claimed commit against repository observation, ancestry and the author time window. Retain exact repository, pre-dispatch revision, returned commit, job and binding; HEAD movement alone is insufficient. |
| `full_loop_runner.clj:4470–4510` | Final revision/review state yields final commit/files and independent review execution gate. Build checkpoint records these, including rejected reviews; it is too early to label every record approved. |
| `full_loop_runner.clj:4598–4688`, `2650` | After approval, grounding writes and reads back implementation/discharge entities. Adjudication retains the witness. This establishes substrate publication, not the selected token-set transition. |
| `full_loop_runner.clj:3618–3653` | Close already gathers checkpoint evidence under the action occurrence. Its retention inputs explicitly say independent observation unavailable and declared model identity unthreaded. Those are existing typed holes to fill, not override. |
| `full_loop_runner.clj:499–573` | Terminal run-record is atomically persisted, but its projection currently omits an enactment witness. Adding a checkpoint alone will not make it available here. |
| `scripts/futon2/report/war_machine.clj:6392,6671` | Next tick reads the latest judgment trace and passes it to D admission. It does not resolve terminal evidence by the prior action occurrence. A late producer needs an explicit exact-identity lookup, not a nearest-time lookup or mutation of the earlier trace. |

## Existing measurements are useful, but not interchangeable

`fact_measurement.clj:178,200` already writes `fact-pre.edn`, end measurements,
source snapshots and `fact-end.edn`, with a pinned reader plan. It is activated
only when interpretation receipts exist (`full_loop_runner.clj:4122`). Its
fact IDs and literal readers are not established as the target-qualified token
universe consumed by D. Pre facts come from the interpretation receipt;
post measurements preserve unknown/unplanned results. Do not relabel these
as contract-admitted C3/C4/C5 observations without proving the mapping.

`observe-end!` (`full_loop_runner.clj:3382`) runs once, including failure
paths, and uses explicit reader receipts rather than ordinary review prose.
Its first normal call is after the build checkpoint and before grounding
(:4510); calling it again after grounding will not obtain a new observation.
A post-grounding token observation must therefore be a separately defined
measurement time, not a claim that these existing end measurements ran later.

The declared-token source (`cascade_sources.clj:103–112,149`) uses mechanical
checks and distinguishes unknown from false. C3/C4 resolve the declared
revision; C5 can resolve additional repository loci (`observation_checks.clj:
41–107`). Pin every resolved revision in a new before/after record. Do not
silently replace a locator's declared SHA with an authored commit, or infer
negative tokens from absent measurements. No J rates or new observation
kinds are proposed.

## Proposed structural join, subject to review

1. At selection/construction, retain the existing action occurrence, D carry
   occurrence, selected-action digest, declared token universe/model identity,
   observation-contract declaration digests and resolved pre-observation inputs.
2. For the ordinary fresh-author route, assemble a claim after final approval
   and adjudication from final artifact binding, author/review jobs and pinned
   before/after token evidence. Keep `:independent-check-required`. Also retain
   typed incomplete/rejected claims on failure, without successful admission.
   Do not use the earlier trace or selected=selected as execution evidence.
3. Persist the claim under the occurrence in immutable attempt evidence; retain
   its path and digest in close evidence and the terminal run-record. Extend
   the actual production verifier to independently resolve these artifacts.
   Merely removing E2b's production refusal is not a solution.
4. At next tick, resolve evidence for precisely the prior carry/selected action
   occurrence. Verify occurrence/action/domain/revisions and observation
   authority before selecting B. Missing, conflicting or domain-changing
   evidence keeps the existing typed refusal/fresh initialization.

## Decisions needed before implementation

**Executed action granularity:** the runner dispatches one author task for a
selected cascade, not a logged per-pattern executor. Is independently verified
completion of that whole action the authorized macro-action whose declared
cascade kernel supplies B, or does admission require actual per-pattern
execution evidence? Either requires an explicit correspondence rule. Artifact
existence and review approval alone cannot decide that rule.

**Observation revision contract:** define how the existing declared locator
source admits the before/after revision pair, and how interpretation fact IDs
(if reused) correspond to D's qualified tokens. Re-reading the same pinned
revision does not measure the authored change. Replacing it silently changes
the observation contract.

**Recovery and historical routes:** recovery (`:author-dispatch` branch) reuses
an older artifact; historical verification (`:4135–4188`) can close with no
new author/build and a separate verification artifact. They need explicit
occurrence/revision rules. Until those exist, these routes remain refused;
do not label an old commit as a newly executed transition.

## Acceptance proposed for the producer/verifier pair

A positive case must use the real producer and independent artifact resolver,
with a revision-bound, checkable token change and exact occurrence join.
Controls should construct: selected=selected without execution; approved
commit with no admitted token evidence; wrong occurrence/action; concurrent
unrelated HEAD movement; changed model/domain; malformed or missing evidence;
recovery claiming fresh execution; and an earlier pre-dispatch trace that
cannot see the terminal witness. Persist/read-back must preserve refusals and
must not transform model-misfit or unknown evidence into successful execution.

Discovery stops here for the owner's review. No production verdict movement
is claimed, and no test suite was rerun for this documentation-only deliverable.
