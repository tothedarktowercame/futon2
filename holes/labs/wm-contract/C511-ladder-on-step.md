# C511 — first stepped tick with the task-belief ladder enabled

Date: 2026-09-05. Step: `013-013-ladder-on`. Run id:
`863c6ab5-3167-4dfb-b197-8f144b6a83e1`. This is a failed measurement: the
ladder affected scoring, but its required judgement record did not survive the
trace boundary. I did not retry, accept, or deposit the step.

## Flag transport and pin

`wm_step.sh step` starts `clojure -M -m futon2.run-tick-once` with three
assignments in front of it and does not clear the parent environment. Therefore
the invocation

```text
FUTON_WM_TASK_BELIEF_LADDER=1 ./wm_step.sh step /home/joe/code/futon2/data/wm-step/w1 013-ladder-on
```

passes the flag by ordinary subprocess inheritance. The flag is not one of the
world inputs hashed by the stepper, so no pin-drift override was used. Before
the tick, `wm_step_records.bb verify` reported 0 missing, 0 added, and 0 changed
files. `step.edn` records pin generation 2 and `:step/pin-verify :ok`.

## What the tick shows

The ladder definitely ran: all 66 ranked actions carry a
`:task-belief/rung`; the chosen `M-zaif-harness-v1` action carries rung 1,
support 4.0, factor 0.8, and the `:direct-case-history` derivation. The ranked
survivor composition is 6 rung-1, 47 rung-2, and 13 out-of-scope.

However, the persisted trace record has neither `:task-belief-ladder` nor
`:task-belief-refusals`. Those are the present-only fields that
`scripts/futon2/report/war_machine.clj:6706-6711` says constitute the ladder's
own judgement record. Consequently the step cannot support a recorded refusal
count or the requested complete census. The four refusals in the RE4 rationale
are later capability-graph admission refusals, not ladder rung-3 refusals.

This is a trace-boundary failure, not evidence that the ladder did not affect
the choice. The observable post-ladder results are:

- chosen candidate: `[:advance-mission "M-zaif-harness-v1"]`, controller rank 1;
- chosen tie width: 1, band `[1 1]` (`:selection-discrimination :green`);
- field: 66 candidates, 53 distinct scores; widest plateau 6 at ranks 61–66;
- surviving rung composition: 6 rung-1, 47 rung-2, 13 out-of-scope;
- ladder refusal count: not recoverable from the persisted record;
- ladder refusal tensions minted by this tick: 0, landing nowhere. The live
  path only builds refusal records; `task_belief_ladder.clj:352-353` explicitly
  assigns append to the separate U52 producer. No deposit command was run.

Relative to U52's ladder-on replay (38 refused, 3 rung-1, 15 rung-2, widest
remaining tie 3), this newer corpus has many more rung-1/rung-2 survivors and a
wider residual plateau (6), while the actual choice is unique. A refusal-count
comparison is invalid because the live trace omitted the ladder judgement.

## Isolation and gates

The live trace remained SHA-256
`f343432d772986ddc2f7fe38107913afc997201ebae9b6240dff152e235b1120`,
mtime `2026-09-04 07:50:47.992676896 +0000`, size 3000420. The four live
rationale files retained their pre-step mtimes and sizes. The run-era ledger
remained SHA-256
`6ad4172d7fee402833d49ae27de1ace6a39e8738c6736b881f999d70b9d3efe7`,
mtime `2026-09-04 16:41:01.480838648 +0000`, size 38699. The run lock was
absent after the step. The pin remains generation 2 and its accepted-step list
still ends at `010-accepted`; no run-era ledger row was deposited.

`wm_step.sh check` ran RE7 only and wrote its artifacts under the step:
1 decision, `:green`, 9/9 controls passing (4 negative and 5 positive).
Repository gates: `negative_controls.sh` PASS (35 negative, 18 positive),
`pointer_check.bb` 1322 pointers and 0 unresolved, and `worklist_check.bb` 155
items OK with 0 validation failures. The worklist checker also warned about
unrelated untracked U56 work already present in the shared tree. No source or
Clojure file was edited, so clj-kondo and check-parens do not apply.

## Addendum, 2026-09-05: the fix-defect loop closed (claude-1)

The trace-boundary defect above is repaired in futon2 `8e6c364d`:
`trace-record` (src/futon2/aif/trace.clj) builds the persisted record as a
constructive whitelist, and U52 never added the two top-level judgement
fields it defined — per-action rung annotations survived, the refusal
census did not. The fix is a present-only clause on the AC1–AC4 pattern;
flag-off trace bytes are unchanged. Gates: trace-test + run-tick-once-test
57 tests / 180 assertions / 0 failures; clj-kondo 0/0; check-parens 0.

Re-step `014-014-ladder-on-fixed` (reset hash-verified against the pin,
run lock held and released, run-id a199cb49): the persisted trace now
carries the judgement record —

```clojure
:task-belief-ladder {:relation :k-doc-xref
                     :generalization-discount 0.5
                     :history-size 17
                     :census {:out-of-scope 17, 3 80, 2 47, 1 6}
                     :refused 80}
```

with `:task-belief-refusals` beside it. So the census the failed step could
not state is: 80 refused (rung 3), 47 constructively scored (rung 2), 6 by
case history (rung 1), 17 out of scope. RE7 on the step: verdict `:green`,
chosen at rank 1, tie 1, band [1 1], field 66, widest plateau not holding
the choice 6; 9/9 controls pass. The pin remains generation 2 and
unadvanced; nothing deposited; the step ran sandboxed with the env flag,
the default is still OFF.

Known remaining gap, deliberate: the live path builds refusal records but
appends no tension entries (task_belief_ladder.clj:352-353 assigns that to
the U52 producer) — 0 tensions minted by this tick. Wiring live tension
minting is follow-on work and is exactly the :tensions-cashed elaboration
U56 is surveying.
