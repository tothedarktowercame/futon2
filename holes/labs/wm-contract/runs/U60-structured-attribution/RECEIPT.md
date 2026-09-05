# U60 -- structured run attribution for tensions

Row `:U60` (worklist.edn), epic `EPIC-run-era.md`, C511 section 4. The row's
boundary: **capture only.** Whether a structural attribution should MEAN
something different from a prose one is the pending (A)/(B)/(C) ruling, and
nothing here answers it.

## What was wired

**MINT.** `futon2.aif.task-belief-ladder/refusal-tension`
(`src/futon2/aif/task_belief_ladder.clj:393`) takes `:records`
(`:423`) and lands it at `:tension/provenance :records` (`:433`) --
the field U39's mint payload has always written
(`u39_selection_retrospective.bb:483-487`) and no committed tension carried.

**IT IS OMITTED WHEN EMPTY, AND THAT IS NOT TIDINESS.** `append-tension!` is
`:already-present` only for a payload matching the committed one exactly
(`u41_tension_ledger.bb:178-181`). An unconditional `:records []` would turn
`u52_mint_refusals.bb`'s documented replay of the two U52 tensions -- committed
2026-09-04, before the key existed -- into the append-only ledger's
identity-conflict refusal (`:183-185`). Pinned both ways at
`test/futon2/aif/task_belief_ladder_test.clj:172-194` and by control 8y.

**THE OMISSION IS DECLARED PER FIELD, NOT DECIDED IN THE PRODUCER.**
`u52_ladder.clj:56-68` marks `:s5` and `:re5` `:minted-before-the-run-key`;
`mint-payload:390-408` reads that flag, so a NEW field carries the key without
an edit. A DEFECT WAS FOUND BY READING THE FIRST PAYLOADS RATHER THAN THE CODE:
`field-of` rebuilt the field map from three keys and dropped the declaration, so
the first run gave `:records` to all three fields including the two already
committed. Fixed by merging onto the declaration (`:107-118`), and the
transcript that would have shipped the wrong ledger is what caught it.

**READ.** `u41_tension_ledger.bb`: `structured-run-keys:494-505` (strings only
-- a keyword in there is a defect to be seen, not stringified into a match),
`attribute-tension:507-529`, `run-attribution:531-547`, folded into
`run-provenance-scan:549-581`.

**STRUCTURED IS PREFERRED AND IS NOT BACKED UP BY PROSE.** A tension carrying
`:records` has said which runs it is about; its pointers are then context, and a
run id appearing in one of them is not a second, weaker vote. Control 8w plants
exactly that case -- a tension declaring run A whose pointer names run B -- and
requires B to come back unattributed.

## What did NOT change, and how that is shown

`:verdict-deposited` (`u41_tension_ledger.bb:597-601`) reads the same two
substring conditions it read before. **Control 8t re-implements the pre-U60
condition and compares it to the shipped receipt over 8 runs on two ledgers**
(`p4ng/empirics-futon/negative_controls.sh:767-852`): 0 disagreements on each.
The planted ledger carries a structural attribution and the curated one does
not (`structural-seen 1` / `0`), so the comparison is not made blind to the new
path, and the verdict folds are pinned (`{:green 4, :typed-absence 4}` planted,
`{:green 2, :typed-absence 6}` curated) so an all-`:red` ledger cannot pass 8t
vacuously.

**`:run-attribution` is carried only when some tension names the run**
(`:578-581`), the placement :U58 named. Control 8u
(`negative_controls.sh:854-878`) replays the receipt for the one deposited run
whose receipt still reproduces, `2026-09-04-010-accepted`: no attribution key,
byte-identical. The same conditionality governs
`:why-the-ledger-cannot-be-run-scoped` (`:613-638`, verbatim while no tension
carries the key) and `attribution-note` (`:640-663`, appended to the row's
notes, absent when there is nothing to name).

## Shown, on a stepped run

`wm_step.sh init/step/accept` from `/tmp/wm-step-u60`, run lock held across the
tick and released, trace/rationale/receipt redirected to the sandbox, nothing
under `data/` written. Step `001-u60-mint`, tick exit 0, run-id
`3416e82b-771d-454d-8d4e-ae3d279cd23c`, normalized sha
`4ae9feec6daf8aae5c2a373582159e5dd8a7ac5f90b9474144afe2e73d360c45`, accepted as
`runs/2026-09-05-u60`. `r6_zero_post_preflight.clj` PASS before the first step
(0 POSTs, 0 `.admintoken` reads, 1663 paths read).

The ladder replayed over that run's trace through its own producer, not a copy:
`U52_EXTRA_FIELD` (`u52_ladder.clj:70-79`) adds one field beside the pinned two,
into `runs/2026-09-05-u60/ladder/`. Its mint payload carries
`:records ["3416e82b-771d-454d-8d4e-ae3d279cd23c"]`; the two pinned payloads
carry none.

`mint.txt`: minted through `append-tension!` -- `:wm-ladder/u60-3416e82b-zero-support`
appended, ledger validates at 8 tensions.

`dry-run-planted.txt`: `--deposit 2026-09-05-u60 --dry-run` against that ledger.
`:fold {:prose-scan 7, :structural 1}`; the new tension `:attribution :structural`
`:declares` and `:matched` the run id; **the two U52 tensions marked
`:attribution :prose-scan`**, which is the row's clause.

`dry-run-prose-fallback-re5.txt`: the fallback FIRING, not merely marked --
`--deposit 2026-09-04-re5 --dry-run` against the CURATED ledger attributes
`:wm-ladder/re5-8ae111bc-zero-support` by `:prose-scan`, matching both the run
directory name and the tick uuid inside a pointer string. Control 8v pins it.

## THE MINT WENT TO A COPY, NOT THE CURATED LEDGER -- two independent reasons

`FUTON_TENSION_LEDGER` (`u41_tension_ledger.bb:48-59`) redirects u41's reads and
the append; `--deposit` refuses under it with exit 3 (`:689-700`) because a
run-era row is a claim about the curated ledger, while `--dry-run` is allowed,
bannered (`:701-703, :722-723`) and writes nothing. Control 8x. The planted
ledger is committed here as `tension-ledger-planted.edn`; the curated one is
untouched (`git diff --quiet`, checked in 8x and after the mint).

**(1) THE LADDER'S OWN CONTROL IS RED ON TODAY'S CORPUS, and a mint would put a
false count in a curated ledger.** `u52_ladder.clj`'s
`:positive/refusal-is-typed-and-grounded` reports `mistyped 53` on every field
including the two pinned ones: `refusal-record`
(`src/futon2/aif/task_belief_ladder.clj:308-326`) grew a SECOND reason,
`:no-open-holes`, in commit `24cea67e` -- AFTER the U52 mint (`7157af97`) -- and
`refusal-payload:371-391` still labels the whole partition with the single
`:task-belief/refusal :task-belief/zero-support-construction-exhausted` and
counts all of it. The u60 tension therefore says 98 candidates were refused for
zero-support when 53 were refused for having no open holes. Not repaired here:
that is U52's mint semantics, and this row's instruction is to do what its
acceptance says and nothing more.

**(2) A DEPOSITED RECEIPT IS ALREADY DIVERGENT, AND ONE MORE TENSION WOULD MAKE
IT THREE.** This check folds the WHOLE ledger into every receipt
(`:live-derivation`, `u41_tension_ledger.bb:602-618`), so any tension added to
the ledger rewrites every run's receipt. It has already happened once, and the
verdict FLIPPED: `tensions-cashed-2026-09-01-s5.edn` and
`tensions-cashed-2026-09-04-re5.edn` were deposited at `:tensions 5` with
`:verdict-deposited :typed-absence`; the U52 mint on 2026-09-04 put their tick
uuids into the ledger's prose, and a replay today produces `:green` and
`:tensions 7`. Measured, not inferred: control 8t's `verdict-fold` for the
curated ledger is `{:green 2, :typed-absence 6}` and the two greens are those
runs. `2026-09-04-010-accepted` (deposited after the mint, at 7 tensions) still
replays byte-identically, which is what 8u guards. **THIS IS PRE-EXISTING AND
NOT CAUSED BY :U60** -- the same measurement on the pre-U60 code returns the
same two greens -- but it is the strongest argument in the tree for the
(A)/(B)/(C) ruling, so it is recorded rather than worked around.

## Also found, not repaired

`u52_ladder.clj`'s report no longer reproduces on today's corpus: `case-history`
reads the live `data/wm-trace`, which has grown, so the rung-3 partitions moved
81 -> 99 (`:s5`) and 80 -> 98 (`:re5`) and four controls are red across the two
pinned fields. The extra field changes none of that -- the pinned arms fail
identically with and without it, checked against a two-field baseline run. The
committed `runs/U52-ladder/` artifacts are NOT regenerated, and
`u52_mint_refusals.bb` reading them still replays `:already-present`.

## Not done, stated

`wm_step.sh deposit` NOT run: no run-era ledger row landed for
`2026-09-05-u60`, and the battery's first-pass receipts are committed
undeposited (`runs/2026-09-05-u60/battery.log`). `gen_aif_dag.bb` not run and
nothing regenerated into a publish (TN 9a). No `aif-equations.edn :choices` and
no `control-map-edges.edn :decisions` entry -- this row records no ruling. The
three deposited runs' rows untouched. The pre-existing untracked
`holes/labs/zaif-harness/runs/build-loop.{lock,log}` left alone.

## Gates

clj-kondo 0 errors / 0 warnings on each of `task_belief_ladder.clj`,
`task_belief_ladder_test.clj`, `u41_tension_ledger.bb`, `u52_ladder.clj`,
`u52_mint_refusals.bb`, `u56_tension_scan.bb`, `u56_fold_consequences.bb`,
linted separately (the cross-file duplicate-require artefact :U57 and :U59
recorded); `futon4/dev/check-parens.sh` OK on all seven; `bash -n` on
`negative_controls.sh`; `negative_controls.sh` PASS 49 negative / 32 positive
(46/28 before); `pointer_check.bb` 1364 pointers / 0 unresolved (1357 before);
`futon2.aif.task-belief-ladder-test` 13 tests / 55 assertions, 0 failures 0
errors; `run_era_ledger.bb --check` green after the accept; `worklist_check.bb`
exit 0.

`u56_pointer_check` 0 unresolved on the receipt (12), `u52_ladder.clj` (3),
`u52_mint_refusals.bb` (0), `u56_tension_scan.bb` (2) and
`u56_fold_consequences.bb` (3). POINTER DRIFT REPAIRED, since the scan moved
down `u41_tension_ledger.bb`: `u56_tension_scan.bb:14-18` cited `:467-484` and
`:500-505`, `u56_fold_consequences.bb:47-48` cited `:476-479`; both re-resolved
against the post-edit file. ONE UNRESOLVED IS PRE-EXISTING AND NOT REPAIRED:
`u41_tension_ledger.bb` carries a bare `README.md` pointer (lines 265-288) inside the
`u39-mint-payload` docstring, which the checker resolves against the wrong root
-- identical on HEAD before this row, and it is U39's transcribed text.
