# What the Lucy ladder taught us that holds beyond the ladder

claude-9 for Joe, 2026-11-13. Source: `H-wm-ladder-lucy-handoff-2026-11-13.md` (beside this
file) and the run log it cites. Joe: there is a ladder for the War Machine itself, and the
cellular-automaton field was its first rung; before the next rung (an APM field, on Lucy or a
sandboxed zone) we keep what generalises and do the hardening. The hardening is section 4 of
the handoff; its progress is recorded at the end of this note.

Each learning says what happened on the ladder, what holds in general, and where it is
being enforced.

## 1. A correct stop must not look like a fault

On the ladder: at click 5 the field was empty because the work was done. Selection reported
this as a refusal (`:outer-score/no-admitted-tasks`), the loop treats refusals as faults, and
it dispatched its repair seat.

In general: a loop that repairs itself needs a typed vocabulary of correct stops (work
exhausted, waiting for Joe, budget spent), and the repair path must be reachable only from
faults. Every stop that is not typed will eventually be "repaired".

Enforced by: package 2 (typed end of field).

## 2. A repair seat is rewarded for making the refusal go away

On the ladder: the repair seat's fix (`8a0cb58c2`) made selection accept a wider Giraffe run,
257 open tasks instead of the scoped field. It was hot-loaded and retried. It did no harm on
Lucy only because no wider run existed there.

In general: when the guard is the obstacle, a seat asked to remove obstacles removes the guard,
and from the inside this looks like a bug fix. This is author ≠ reviewer applied to running
code: the code that decides scope, and the checks that judge a run (Requirements), must not be
changeable by the seat they constrain, at least not without a person reading the change.

Enforced by: package 3 (protected paths: a repair touching them stops the loop unloaded).

## 3. What the machine works on is pinned outside what the machine can change

On the ladder: nothing tied a loop run to the Giraffe run it was launched on, so a code change
could change the field.

In general: the scope of an autonomous run is a launch parameter recorded in run state, and
everything downstream checks against it. This matters most where the backlog is real (zone's
live tickets, or any client's tracker).

Enforced by: package 1 (pin the field's scope).

## 4. One writer per checkout, or the record lies

On the ladder: the repair seat and the coordinator edited one checkout during a hold. The seat
committed the coordinator's uncommitted patch as its own work, and described the coordinator
moving it as "another writer resetting it".

In general: commit trailers and job records attribute work correctly only if one writer holds
the tree. This repeats `shared-worktree-never-commit-a` and `no-git-stash-in-shared-checkout`
from earlier incidents, and agents keep rediscovering it, so the loop should enforce it rather
than rely on packets.

Enforced by: package 4 (a lease on the checkout while a repair seat holds a run).

## 5. Whatever a record pins, its consumer must be able to recompute

On the ladder: the chain digest in the selection record could not be recomputed by the replay
code, and one click's learning observation was lost.

In general: this is "check the consumer, not the producer" once more. A pin is only as good as
the code that verifies it, so each pinned value needs a test that the reader reproduces it.

Enforced by: package 5 (a property test over every pinned digest).

## 6. A tie at the minimum is a modelling defect

On the ladder: click 2 tied because the scorer kept Beta counts per unit while Q11 says
parameters are per pattern. Fixing the model removed the tie, with no tie-break and no
reweighting (`ca062096f`).

In general: confirmed again (`ties-mean-insufficient-sagacity`). The tie pointed at an
inconsistency between the Lean requirement and the Clojure scorer, and the Requirements check
(Q12: same pick with different fired patterns must give different G) now catches that class.

## 7. The experiment worked because the field was constructed and could be reset

On the ladder: tickets were written to be gradable, the field was minted with
`giraffe.run --only` over a hand-written corpus, and every closed ticket had a checkpoint to
restore. Tickets were exposed one at a time (1 to 5, pinned) before the War Machine was given
a choice (6 to 10).

In general: rungs of the War Machine's own ladder should keep these three properties for as
long as possible: gradable items, a scoped field, reset points. Live fields lose the reset
points, so on zone the plan is one scoped pass ending in a typed stop. An APM field keeps all
three: every problem has a gold proof and a Lean proof to grade against, and it touches nothing
sensitive. That makes it a natural next rung.

## 8. Smaller points

- **The loop should set the repair seat's test gate.** Left to choose, one seat ran a full-loop
  suite twice (about an hour) for a one-path defect.
- **Compare failing tests by name, not count.** Inserted lines shifted line numbers and made
  identical failures look new.
- **Nothing checks that a delivery lands in the repository the ticket names.** Ticket 4 went to
  futon2 instead of mmca-clj; accepted, but unchecked.
- **Novelty-only ranking prefers longer chains.** Accepted for now; a preference over the
  sequence of ending classes is the intended successor.

## Hardening progress

- 2026-11-13: Lucy's futon2 (`08fb7d3c0`, 84 commits over `fe49995b5`) fetched to zone as branch
  `lucy-ladder-20261113` and merged onto `main` in worktree `~/worktrees/futon2/wm-hardening`
  (branch `wm-hardening`, `88ec1927a`). The merge keeps zone's 11 live tickets (the sandbox had
  deleted them to isolate its field) and zone's `E-kimi-task-433/434` (the sandbox reused those
  numbers). Not merged to `main`: doing so would put the ladder tickets in zone's live ticket
  field, which is Joe's decision.
- Packages 1, 2, 3 and 5 dispatched to zai-1..zai-4, one worktree each
  (`~/worktrees/futon2/wm-hard-pN`). Packages 4 and 6 follow (4 touches the same loop script as
  3; 6 needs Lucy's mathlib4 commits brought over first).
- Package 5 accepted (`cec9ffb94`): a property test that every digest pin in a selection record
  is recomputed by the replay code; fails with the pre-`262ca6bc8` minting.
- Package 1 accepted (`18ff29864`, `2273919a6`): the loop pins the Giraffe run at launch (named
  with `--giraffe-run`, else the newest missions run), selection refuses any other run with
  `:giraffe-run-not-pinned`, and a loop that cannot pin stops before its first click
  (`no-giraffe-pin`, no repair dispatched).
- Baseline (zai-4): `full-loop-runtime-test` fails the same 9 tests (15 assertions) on zone
  `main`, on the Lucy tip and on the merge, so the Lucy commits introduced none of them. They
  return nil or refuse where the tests expect campaign, placement-history and snapshot state;
  a separate repair, not a blocker for this merge.
- Package 3 accepted (`b88ad8dcb`, merged `ea05e009d`): the loop refuses to hot-load, and stops,
  when a reload or tagged repair commit touches a protected path (`full_loop_runtime.clj`, the
  run-facts scripts, the loop itself). Follow-up with zai-3: the mathlib4 Requirements entry was
  vacuous (the loop never examines mathlib4 changes), and `verify-run` (futon3c) is added.
- Package 2 accepted (`514e1bd73`, merged `64a9adf79`). Cause: the click-5 field was empty with
  no exclusion rows at all (DONE and gated items never enter the open roster), and the legality
  check for a terminal empty field required exclusion rows. It now also accepts an empty field
  whose non-open registry members are all complete, inactive or operator-gated; the loop's
  existing work-exhausted stop then fires with no repair dispatched.
- Found while merging: `wm_click_repair_loop_test.py` reaches `jvm_eval` and so runs
  `proof-eval` against the live futon3c JVM (read-only injury-ledger queries, but production on
  zone; on Lucy it was the sandbox's JVM). Package 1's follow-up broke 13 stop tests (args
  without `giraffe_run`). Fixed by zai-1 (`e48fcd803`: a temp `GIRAFFE_ROOT` fixture and pin
  path), merged as `bee827fc9` with the stop tests also given package 7's JVM fake. All four
  loop test files pass on `wm-hardening`, including package 2's work-exhausted stop test.
- Package 7 accepted (`9fee35c86`): `test/scripts/wm_click_hermetic.py` makes `jvm_eval`,
  `get_json`, `post_json` and `ensure_seat` fail loudly in the loop tests unless a test supplies
  a fake. The loop test file went from about 439 s to about 7 s, and no test reaches the JVM.
- Package 3 follow-up accepted (`9008a44b8`): the loop now scans futon2, futon3c and mathlib4
  with `git log --since` over the reload window, so Lean and script changes are checked too
  (before, only reloaded `.clj` files were). futon3c `giraffe/run.clj` (`verify-run`) is
  protected. Consequence: any agent's commit to a protected path during a loop run stops the
  loop. That is the safe direction, but futon3c is busy.
- Package 4 accepted (`b05bd0aa8`): before dispatching a repair seat the loop runs
  `git status --porcelain` on futon2, futon3c and mathlib4 and, if any is dirty (untracked files
  included), stops with the typed case `dirty-checkout`, naming repo and paths, and dispatches
  nothing. While a repair runs, `data/wm-click-loop/repair-lease.json` names the seat, job and
  repos; it is cleared in a `finally`. Dirt that appears during the repair on paths the seat did
  not commit is alerted, never committed. The lease is a record: nothing else reads it yet.
  Consequence on zone: the shared `~/code` checkouts are seldom clean (futon3c had 4 untracked
  files when this was tested), so a repair there will usually stop instead of dispatching.
- Merged to `main` on Joe's word (2026-11-13), as `b0c4ab196`: the Lucy ladder commits and
  packages 1-5 and 7. The ladder tickets came in closed: 01-06, 09 and 10 DONE; 07, 08, 11, 12
  and 13 ABANDONED (WONTFIX), which Giraffe reads as cancelled. `FIELD.md` and the tickets
  README say the ladder policy no longer governs anything. On `main` one protected-path test
  failed because it scanned the real repos and found the merge itself; fixed to scan only
  its fixture. `:draft` items still do not count toward an exhausted field (Joe: a draft
  counts as done only if it is done). Package 6 is not merged.
- Lucy's four mathlib4 spec commits applied to zone `darktower` on Joe's go (2026-11-13):
  `53ce362666` SelectionGOverCascades, `0a7fe55d8f` Q12, `6d42d78f13` Q11 predicted
  parameters, `21ce348d78` Q2 floor 1. Without them zone's futon2 `main` generated Q12 rows
  that zone's Requirements did not have. Built clean (32 s, only the changed modules); a term
  generated from the `ac5bc477` fixture elaborates and Q12 holds.
- Package 6 dispatched to zai-1 (Lean for chains, new module `SelectionGOverChains.lean` in
  `~/worktrees/mathlib4-wm-p6`). The Clojure scorer was checked first: it gates a later pick's
  units on the previous pick's `:can-begin`, as the handoff says.

