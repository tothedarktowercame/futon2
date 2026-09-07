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

## 2. What the records attest

The declaration supplies the twelve-wide seed and four fold entries at
`futon2:src/futon2/aif/ruled_outcome_c.clj:43-84`.  The machine emits its live
preference stack at `futon2:src/futon2/aif/efe.clj:877`.  In this run the stack
was `:partial` and rank-indexed: each present rank carried its own `:value`
vector.  That differs from the packet's shorthand single top-level `:value`;
the checker handles both shapes and projects the shape actually found
(`03-live-run.edn:1`).

Across all three ticks, `:ruled-outcome-c` and `:c-ser` were absent.  Every
present rank carried the folded `:floor` layer, whose `:basis` names
`src/futon2/aif/preferences.clj` as emitted by the record declaration at
`futon2:src/futon2/aif/efe.clj:121-126`.  The actual `:c-entries` value was
`nil` on every tick—not `[]` as in the earlier run cited by the packet—and is
empty under Clojure's collection predicate (`03-live-run.edn:1`).  The seed
computed from running code had exact mass 1, support equal to the twelve-member
authority, and seven derived zero-mass dispositions (`03-live-run.edn:1`).

## 3. Non-vacuous controls

The null control sends the unchanged declaration and same run through the same
checker route, requires acceptance, and compares its artifact byte-for-byte.
Declaration-copy plants independently flip the ruled-folded, c-int-folded, and
seed-validity conjuncts.  A trace-copy plant injects a live ruled layer and
flips only the live-absence conjunct.  Finally the 2026-09-04 trace selects none
of these run ids and flips only run identity.  Each mutation checks that its old
text was present, its replacement landed, and its old text is absent
(`futon2:holes/labs/wm-contract/f10_live_run_controls.sh:11-67`).

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
