# U58 — the runtime-validation catalogue's cross-repo identity set, per step

Worklist row `:U58`, `EPIC-run-era.md`. The elaborate leg U56 typed for
`:per-node-runtime-validation` (`C511-repair-or-elaborate.md:204-215`): the
catalogue input reproduces exactly, but its 181 pointers reach four
repositories — futon2 157, futon3c 11, p4ng 8, mathlib4 5 — and futon2's sha
was the only repository identity a step ever recorded. So 24 of the 181
resolved against a tree nobody had written down, and for futon3c the accepted
run's store held no identity at all. A pointer's line range resolving today is
not evidence it resolved at the run.

## What was added

**The capture** — `wm_step_records.bb:333-393`. `runtime-validation-capture`
(`:367-393`) records the catalogue's own identity through the same
`source-identity` U57 added (`:280-309`: sha256 of the bytes now, the last
commit touching the path, that commit's blob sha1, the worktree blob sha1 and
`:worktree-matches-commit?`), and beside it, per repository the catalogue's
pointers name, the head and the porcelain list. It is written into the step's
world record at `wm_step_records.bb:437`, taken before the tick at
`wm_step.sh:248`, and already copied into a run store at `wm_step.sh:511`.

**The repo set is derived, not listed.** `catalog-pointer-repos`
(`wm_step_records.bb:353-365`) re-runs the check's own pointer regex over the
catalogue and keys by the first path segment, which is what a catalogue pointer
is (`runtime_validation_check.bb:19-30`). A catalogue that grows a pointer into
a fifth repository captures that repository without an edit to the stepper. The
regex is duplicated from `runtime_validation_check.bb:71` rather than shared —
the two files belong to different tools — and the duplication is checkable
rather than silent: the basis prints the per-repo counts the check's own
`pointers=181` totals.

**The run-keyed basis** — `runtime_validation_check.bb:206-391`.
`run-keyed-basis` (`:260-328`) reads the run's world record and returns the
identities as an `array-map`, with three statuses: `:captured`,
`:predates-u58-capture`, `:no-world-record`. **Every field comes from the
capture and none from the tree**, so the value is fixed by the run store. That
is deliberate: the deposit receipt embeds it and has to stay byte-stable,
because the ledger requires the receipt committed and unmodified and compares a
replayed deposit field by field (`run_era_ledger.bb:235-245`).

**The deposit cites them** — `deposit-receipt` (`:401-454`) carries
`:run-keyed-basis`, and `basis-note` (`:456-474`) puts one sentence naming the
five identities into the `:row/notes` the ledger row carries.

**Carried only when there is a capture to cite.** A run deposited before this
existed keeps a byte-identical receipt and byte-identical notes; otherwise
replaying its deposit would stop being `:already-present` and become the
append-only ledger's divergence refusal. Verified, not asserted: the receipt
this build produces for `2026-09-04-010-accepted` diffs empty against the
committed `runs/RE6-check-deposits/runtime-validation-2026-09-04-010-accepted.edn`,
and its notes diff empty against that row's `:row/notes` in
`run-era-ledger.edn`. What is missing for those runs is not silent —
`--run-basis 2026-09-04-010-accepted` says `:predates-u58-capture` and exits 2
(`run-basis-precapture.txt`).

**The detector** — `bb runtime_validation_check.bb --run-basis <run-id>`
(`:330-377`) prints the basis and compares it to the tree NOW. Exit 0 every
identity unmoved, 1 one has moved, 2 there is no capture. Exit 1 is a FINDING,
not a tool failure: it says the tree a pointer would resolve against today is
not the tree the run read, and it names how many of the 181 pointers that puts
in doubt. `WORLD_RECORD=<path>` overrides the run store's `world-before.edn`,
which is the seam the controls plant in.

Heads and the catalogue's content hash decide the exit; porcelain LINE COUNTS
are printed and decide nothing. Uncommitted movement inside a repository is
visible as a count that differs and this mode does not rule on it — saying
otherwise would put a claim in the exit code that the capture does not support.

**`--run-basis` does not compose with `--deposit`** (`:384-390`). It exits
before the deposit block, so the two together would have performed the report,
swallowed the deposit and exited 0.

**`--deposit <run-id> --dry-run`** (`:503,509-519`) prints the receipt and the
notes and writes nothing — no receipt file, no ledger row. A run-era row is a
claim about an ACCEPTED run, so this is how a deposit's citation is exhibited
for a step that has not been accepted, without minting a row about a run that
does not exist.

## What this does NOT do

It does not make the check run-scoped. The catalogue carries no run identity —
its rows are keyed by node and axis, its `:test-runs` by namespace and date, and
no tick writes into it (`runtime_validation_check.bb:446-451`) — so the verdict
is still a property of the tree at deposit time and a run with no
runtime-validation artifact in its store still deposits `:typed-absence`. What
changed is that the row now names which tree that was, and what it shares with
the run's. C511's follow-on (ii) — whether a tree artifact should be deposited
per run at all, or folded as a tree-era check — is a design question and is not
answered here.

## The demonstration

One step, `wm_step.sh step /tmp/wm-step-u58 u58-final`, step `002-u58-final`,
run-id `9509555b-b1e2-4825-8dc2-91672f48777b`, tick exit 0, normalized sha
`e33c6375e8aad104d2d81df46b76d8aa4e4aea4bb8f632222467e136ba7aea00`. The run
lock was held across the tick and released (`step.edn` `:step/run-lock`), the
trace, rationale and receipt were redirected to the sandbox, and nothing under
`data/` was written (`:step/live-data-written false`). `world-before.edn` and
`step.edn` here are that step's, verbatim.

The step before it (`001-u58-capture`, run-id
`075df849-1491-4e01-8985-09f066249aa6`) produced the same normalized sha; it was
re-stepped only so the demonstration ran against the final code.

Its capture, all five identities:

| identity | value | pointers |
|---|---|---|
| catalog `RUNTIME-VALIDATION-CATALOG.edn` | sha256 `244becbeb00c…`, committed at futon2 `7157af97267e`, worktree matches | — |
| futon2 | `316091df7b6b…` | 157 |
| futon3c | `d34a1ceb28ae…` | 11 |
| mathlib4 | `4bbc7111c4b6…` | 5 |
| p4ng | `e28ebc9d7b5b…` | 8 |

`run-basis.txt` is `--run-basis` on it: all 5 unmoved, exit 0.
`deposit-dry-run.txt` is the receipt a deposit for that run would write, with
`:run-keyed-basis` carrying the five and the notes naming them, exit 0, nothing
written.

## Controls

`p4ng/empirics-futon/negative_controls.sh:581-652`, planted in COPIES of a
freshly taken world record — read-only, no run lock, no step, nothing under
`data/`.

* **8j** a captured futon3c head replaced by forty zeros must be DETECTED: the
  report names `MOVED futon3c` and says `11 of 181 pointers resolve there`, exit
  non-zero. Without the count the finding would not say how much of the
  catalogue it puts in doubt.
* **8k** a world record with `:world/runtime-validation` removed must exit **2**
  and say `predates-u58-capture`. The exit code is the point: "no identities
  were recorded" and "the identities recorded still match" are different
  findings and only one of them supports a pointer claim.
* **8l** positive — the same capture unplanted reports every identity unmoved,
  and the receipt built from it carries `:run-keyed-basis` and cites all four
  repos and the catalogue blob; `--dry-run` wrote no receipt. Without this, 8j
  would be indistinguishable from a detector that fires always.
* **8m** `--summary` still prints the bare COUNTS line and nothing else — the
  line the narrative quotes and the seq-22 deposit recorded. A capture row may
  not move it.

`negative_controls.sh` 41 negative / 23 positive.

## Pointer drift

Inserting these blocks moved lines in both scripts: the usage header grew by
three, so everything below it in `runtime_validation_check.bb` shifted, and the
capture block shifted `wm_step_records.bb` below `:331`. Every pointer in the
two scripts' own comments and in `u56_pointer_check.bb:11` was re-resolved and
corrected against the post-edit files.

Two live pointers into `wm_step_records.bb` were NOT edited, because they belong
to records written and reviewed earlier and the convention here is that a dated
artifact keeps the pointers it was written with (U57 left `C510-stepper.md`'s
and `C511-repair-or-elaborate.md`'s alone for the same reason): the U57 receipt
`runs/U57-flip-readiness-capture/RECEIPT.md:21` and the U57 row's `:evidence`
both cite `wm_step_records.bb:367` for the world-record write, which is now
`:437`. Named here so the drift is on the record rather than discovered.
