# C579 — F10 live-run rider, war-machine half

## 1. What ran

The outcome-domain ruling requires at least one new end-to-end run of relevant
subcomponents before treating the seed as validated
(`futon2:holes/labs/wm-contract/aif-equations.edn:212`).  After the zero-POST
preflight passed, `wm_run.sh` made three ticks under one lock.  The committed
projection is `futon2:holes/labs/wm-contract/runs/F10-outcome-domain/03-live-run.edn:1`;
the ignored source trace is covered by `futon2:.gitignore:49` and pinned there by
path, SHA-256 and the three run ids.

The pre-run substrate-2 probe at port 7071 was refused (`curl` exit 7, HTTP
`000`).  This is a recorded run condition.  The run used futon2 git SHA
`3f1ef0d7827bc03bc96e95506d10c543ef48ee3b` and emitted timestamps from
2026-09-07T23:09:01.901362921Z through 2026-09-07T23:12:22.838749091Z
(`03-live-run.edn:1`).

The preflight and lock records are TRANSCRIBED, not gated — they were only in
the dispatch job's transcript and in `/tmp`, both of which age out, so they are
copied here verbatim.  `clojure -M:test holes/labs/wm-contract/r6_zero_post_preflight.clj`
printed:

```
r6-preflight: real diagnostic tick in 9 ms
r6-preflight: POSTs attempted: 0
r6-preflight: writes the tick would make:
r6-preflight: run-lock writes PASSED THROUGH: 0
r6-preflight: run lock after the tick: STILL PRESENT (263 bytes) -- the run will be refused
r6-preflight: paths read: 1; .admintoken reads: 0
r6-preflight: PASS — no POST on the real path, and the admin token was never read
```

The lock the preflight left behind was its own — `{:pid 2661929, :agent "joe",
:run-id "76ea63e5-e7d5-4458-b8fe-76c17706c980", :acquired-at
"2026-09-07T23:07:07.244708125Z"}`, the preflight JVM still alive when it was
read — and it was gone before `wm_run.sh` took its own at 23:08:19.  This is the
stranded-lock shape `r6_zero_post_preflight.clj:29-42` documents, resolved by
waiting rather than by removing another holder's lock.  `/tmp/wm-run-lock-holder.out`
recorded `{:held ".../data/wm-trace/.run-lock", :pid 2664957, :token
"37857be0-bf64-4282-bae7-cdb4bf77b6e9"}`, matching the token in
`03-live-run.edn:1`.

## 2. What the records attest

The declaration supplies the twelve-wide seed and four fold entries at
`futon2:src/futon2/aif/ruled_outcome_c.clj:43-84`.  The machine emits its live
preference stack at `futon2:src/futon2/aif/efe.clj:877`.  In this run the stack
was `:partial` and rank-indexed: each present rank carried its own `:value`
vector.  That differs from the packet's shorthand single top-level `:value`;
the checker handles both shapes and projects the shape actually found
(`03-live-run.edn:1`).  `:partial` is a status, so review added the number
behind it: the split is **146 present / 1 absent of 147 ranks, identically on
all three ticks** (`03-live-run.edn:1`, `:ranks-present`/`:ranks-absent`).  The
partiality is one ranked action that carries no preference stack at all, not a
mostly-unrecorded stack — `:reason :missing-from-some-ranked-actions` at
`futon2:src/futon2/aif/trace.clj:336-344`.  Why that one is missing is not
diagnosed here.

Across all three ticks, `:ruled-outcome-c` and `:c-ser` were absent.  Every
present rank carried the folded `:floor` layer, whose `:basis` names
`src/futon2/aif/preferences.clj` as emitted by the record declaration at
`futon2:src/futon2/aif/efe.clj:121-126`.  The actual `:c-entries` value was
`nil` on every tick—not `[]` as in the earlier run cited by the packet—and is
empty under Clojure's collection predicate (`03-live-run.edn:1`).  The seed
computed from running code had exact mass 1, support equal to the twelve-member
authority, and seven derived zero-mass dispositions (`03-live-run.edn:1`).

## 3. Controls, and the one that was missing

The null control sends the unchanged declaration and same run through the same
checker route, requires acceptance, and compares its artifact byte-for-byte.
Declaration-copy plants independently flip the ruled-folded, c-int-folded, and
seed-validity conjuncts.  A trace-copy plant injects a live ruled layer and
flips only the live-absence conjunct.  The 2026-09-04 trace selects none of
these run ids (`futon2:holes/labs/wm-contract/f10_live_run_controls.sh:11-89`).

**Review finding, repaired here.**  `:c-int-floor-attested` is the only conjunct
that makes a POSITIVE claim about the live run — the other five say something is
absent, undeclared, or matches a pin.  As first written it read
`(every? floor-ok? ticks)` over a floor set the projection filters out of the
trace, so an empty floor set satisfied it.  Measured, not inferred: renaming all
438 occurrences of `:layer/id :floor` in a trace copy left `:floor #{}` in the
artifact and the checker still printed `F10 LIVE RUN PASS` with
`:c-int-floor-attested true`.  The existing `c-int-unfolded` control could not
see this, because it moves the same conjunct by flipping the declaration's
`:folded?` flag and never touches the trace — the gate was passing for a reason
other than the one it names, which is the third slice in a row on this row to
carry that defect (slices 3, 4 and 5).

Repaired at `f10_live_run_check.bb:113-116,122`: a nonempty floor per tick, and
nonempty ticks.  A sixth control plants the absent floor on the real path and
requires exactly `:c-int-floor-attested` to move
(`f10_live_run_controls.sh:57-75`).  The repair also changed what the
prior-trace control proves: an empty tick list now fails
`:c-int-floor-attested` as well as `:run-identity`, and the control asserts both
(`f10_live_run_controls.sh:77-85`).  Artifact sha256 after the repair:
`e76b1e4061481f3b000e9afadaf76f288dfa468506dc564af9fc5c501368c53d`.

**Not gated, stated.**  The `:lock-holder` and `:substrate-7071-probe` maps in
`03-live-run.edn:1` are typed constants in the checker
(`f10_live_run_check.bb:87-93`), not measurements; only
`:lock-absent-after-run?` is computed, and it asks whether a lock file exists at
CHECK time, which is also true of any run that never took one.  Section 1
transcribes the out-of-band records they were copied from.  C579's own file:line
pointers are not covered by `pointer_check` (6 registry files, and this sheet is
not one), so they rest on the review's reading.  The trace this check pins is
gitignored and mutable: another tick on 2026-09-07 would change its sha256 and
turn `:run-identity` red permanently.  The durable evidence of the run is the
three committed `tick-run-record-2026-09-07-*.edn` files.

## 4. Scope limits

This is only the war-machine half of the live-run rider.  The live tick loop
folds preference layers and records them, but it does not produce terminal
flight dispositions.  The newest full-loop cohort attempt remains
`data/wm-full-loop/wm-outer-loop-46-v1/attempt-061`, modified 2026-07-27
(run identity not found in a committed source).  Restarting that cohort is
Joe's call.  Therefore this sheet does not claim the rider is fully discharged.

No Q was built, no scorer or tick was wired, no ruling or registry was written,
and no F6 rung was moved.  The declared fold boundary remains a declaration;
this slice measures only what the new war-machine run emitted.  The lock was
absent after the run, with acquisition and release-observation data recorded in
`03-live-run.edn:1`.
