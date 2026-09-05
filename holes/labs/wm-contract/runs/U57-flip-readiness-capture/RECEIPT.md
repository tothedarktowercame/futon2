# U57 — capture flip-readiness per step

Worklist row `:U57`, `EPIC-run-era.md`. The elaborate leg U56 typed for
`:flip-readiness` (`C511-repair-or-elaborate.md:132-159`): the check's six
sources are read at the moment of asking, so a run that did not pin them cannot
have its verdict re-derived, and the deposit can only record a typed absence.
The stepper now pins them and the check can read the pin back.

## What was added

**The capture** — `wm_step_records.bb:233-256` lists the six files
`flip_readiness_check.bb:178-183` reads, each with the environment variable that
overrides it. `:258-265` lists the two repos the `:per-node-tests` line reads
live git in (`repo-dir`, `flip_readiness_check.bb:344`). `source-identity`
(`:280-309`) records, per source, the sha256 of the bytes NOW plus the git
identity that fetches them back — last commit touching the path, that commit's
blob sha1, the worktree's blob sha1, and `:worktree-matches-commit?`.
`flip-readiness-capture` (`:311-330`) adds the two repo heads with their
porcelain lists and the one `git log` the `:contract-pin` line runs
(`flip_readiness_check.bb:240-241`), which is a source and is not a file. It is
written into the step's world record at `wm_step_records.bb:367`, taken before
the tick at `wm_step.sh:246`, and already copied into a run store at
`wm_step.sh:511`.

`world-files` (`wm_step_records.bb:150-172`) is a different set — C509 item R7's
tick inputs — and exactly one of the six is on it (`:holes-contract`, `:167`).
It is left alone.

**The residual C511 named** — `:step/futon2-tree-dirty?` is a boolean
(`wm_step.sh:123`), so it cannot exclude a dirty-then-reverted-without-a-commit
path. `tree_dirty_files` (`wm_step.sh:129-134`) records the porcelain LIST
beside it, written at `wm_step.sh:303`. It emits the list through `prn` of a
vector rather than a hand-built literal: a path holding a quote would otherwise
produce a step record that cannot be read back.

**The as-of mode** — `bb flip_readiness_check.bb --as-of <world-record.edn>`
(`flip_readiness_check.bb:99-190`). Resolution is hash-verified with two routes
and a refusal:

1. `git show <last-commit>:<rel>` — used if its sha256 is the recorded one.
2. otherwise the live file, if ITS sha256 is the recorded one. This is the only
   route that recovers bytes that were never committed, which is the case the
   capture marks `:worktree-matches-commit? false`.
3. otherwise the source is unresolvable and the mode REFUSES before deriving
   anything (`:170-185`).

The refusal is placed before the derivation on purpose. Carrying an unresolvable
source as a problem would let the six lines fall through to their live defaults
and print a verdict block under an `AS-OF` header — a derivation labelled with a
run and read from today's tree, which is the mislabelling the deposit comment at
`flip_readiness_check.bb:590-603` declines to make in the other direction.
`moved-since-head` (`:351-380`) refuses the same way when the capture holds no
git state for a repo the catalog's `:heads` names, rather than falling back to
`HEAD`.

A repo sha alone would not do this. C511 section 1 records the wrong-commit
extraction it caught — `69721b12`, the contract AUTHORITY, against the
`4bbc7111` that re-emitted the JSON — and only the content hash rejected it.
This step's capture reproduces both: `:contract` is pinned at `4bbc7111c4b6`
with sha256 `4e1feed965e9…`, and `:holes-lean-last-commit` is `69721b12…`.

`--as-of` composes with neither `--deposit` nor `--emit` (`:743-761`). What a
deposit made from a re-derivation asserts about an already-deposited run is the
open question `C511-repair-or-elaborate.md:499-521` records, and it is not
settled here; and `FLIP-READINESS.md` and its sidecar record the tree, not a run.

## The demonstration

One step, `wm_step.sh step /tmp/wm-step-u57 u57-capture`, run-id
`15eca31e-751d-4ecd-a259-68a5dd7a0408`, tick exit 0, normalized sha
`9c3699b04e5e983ea9d347cd42f57c4d75443f3e91028411b92b193e81841300`. The run lock
was held across the tick and released (`step.edn` `:step/run-lock`), the trace
and receipt were redirected to the sandbox, and nothing under `data/` was
written. `world-before.edn` and `step.edn` here are that step's, verbatim.

The capture carries all six sources, all six resolvable from a commit:

| source | sha256 | pinned at | in a commit |
|---|---|---|---|
| `:catalog` | `244becbeb00c…` | futon2 `7157af97267e` | yes |
| `:accounting` | `3632fce88ca9…` | futon2 `82a18cfa0cc7` | yes |
| `:hole-audit` | `9ec3f82c9974…` | futon2 `f67a23fc6b02` | yes |
| `:tally` | `608752834e4a…` | p4ng `e508eceb1751` | yes |
| `:receipt` | `afe7b70d4e45…` | p4ng `90d58c02264a` | yes |
| `:contract` | `4e1feed965e9…` | mathlib4 `4bbc7111c4b6` | yes |

plus `futon2@9d53814107ee` with 5 porcelain entries, `futon3c@673e08539250`
with 0, and Holes.lean last changed at `69721b12…`.

`as-of-summary.txt` is `--as-of world-before.edn --summary`; `live-summary.txt`
is `--summary` at the same tree state. They are byte-identical, both exit 0:

```
GATE: 7 flips | 0 READY | 7 BLOCKED | lines per flip: contract-pin box2-holes figure5-partials mission-gauges per-node-tests flag-chain
```

which is also the derivation U56 reproduced by hand for
`2026-09-04-010-accepted` (C511 section 1), and the one the seq-21 deposit
recorded live. That is the expected-value control the row named.

## Controls

`negative_controls.sh:530-579`, four negative and two positive, all over a world
record taken into `$T` — no run lock, no step, no shared registry touched.

* **8g** (negative) a captured source whose recorded sha256 no longer matches
  either its commit or the live file makes `--as-of` refuse; the output names
  the source and carries no `GATE:` line at all. Without the last assertion the
  control would pass over the earlier draft, which printed a full verdict block
  assembled from live sources under the `AS-OF` header and exited 1.
* **8h** (positive) the same capture unplanted derives, and derives byte-for-byte
  what the live mode derives at the same tree state. Without it, 8g's refusal
  would be indistinguishable from an as-of mode that refuses everything.
* **8i** (negative, twice) `--as-of --deposit` and `--as-of --emit` are both
  refused, each for its own stated reason.
* The world census asserting `flip-readiness sources 6/6 hashed` is the second
  positive: the capture is checked to be complete, not assumed.

## Not done, stated

No `src/` changed, so no behaviour of the machine moved. `--emit` was NOT run and
`FLIP-READINESS.md` and `runs/U32-flip-readiness/flip-readiness.edn` are
untouched. No ledger deposit and nothing written under `runs/RE3-check-deposits/`.
The step was NOT accepted — `wm_step.sh accept` creates a run store and runs the
deposit battery, and this row asks for neither. `gen_aif_dag.bb` was not run and
nothing was regenerated into a publish (TN §9a). No `aif-equations.edn :choices`
and no `control-map-edges.edn :decisions` entry. The pre-existing untracked
`holes/labs/zaif-harness/runs/build-loop.{lock,log}` were left alone.

## What this does NOT reach: the three runs already deposited

`--as-of` is refused, with the reason printed, on a world record that predates
the capture:

```
$ bb flip_readiness_check.bb --as-of runs/2026-09-04-010-accepted/world-before.edn
flip_readiness_check --as-of: runs/2026-09-04-010-accepted/world-before.edn carries
no :world/flip-readiness -- it predates U57's capture, so this run's sources were
never pinned and no as-of derivation is possible          (exit 2)
```

That is the point U56 made about `:contract-pin` and it holds here unchanged
(`C511-repair-or-elaborate.md:22-46`): this is an ELABORATE, so it makes the NEXT
run derivable and does nothing for `2026-09-01-s5`, `2026-09-04-re5` or
`2026-09-04-010-accepted`. U56's re-derivation for the accepted run remains a
hand-assembled env block, and s5 is refuted outright — four of the five futon2
sources first exist 37-43 hours after it ran. Where a demonstrated repair for the
three deposited runs may land is `C511-repair-or-elaborate.md:499-521`'s open
question and is untouched here.

## Gates

`r6_zero_post_preflight.clj` PASS (0 POSTs, 0 `.admintoken` reads, 1663 paths
read) before the first step. `wm_step.sh determinism` IDENTICAL — two further
steps from the same pin agree on every decision field, `0ccadb636dc5` both,
0 of 17 hashed inputs moved. `futon2.aif.task-belief-ladder-test` 12 tests /
51 assertions / 0 failures / 0 errors. clj-kondo 0 errors 0 warnings on
`flip_readiness_check.bb` and `wm_step_records.bb` (linted separately: linting
both in one invocation reports the shared top-level `require` as a duplicate,
which is an artefact of neither script carrying an `ns` form).
`futon4/dev/check-parens.sh` OK on both. `bash -n` on `wm_step.sh` and
`negative_controls.sh`. `negative_controls.sh` PASS (38 negative, 20 positive).
`pointer_check.bb` 1342 pointers / 0 unresolved. `worklist_check.bb` exit 0.
