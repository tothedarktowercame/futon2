# U37 — was the selector enumerating everything there was to work on?

**Date:** 2026-09-03 · **Row:** worklist `:U37` · **Records:** the three ticks of
2026-09-02 (`data/wm-trace/wm-trace-2026-09-02.edn`, run ids `0a18c4f7…`,
`4abad68c…`, `801976e7…`).

Joe asked for "validation that it is running over all available missions,
excursions, and tickets". The reason a tick's own record cannot answer that:
a candidate list of 146 looks the same whether the world holds 146 items or
1,619. `holes/NOTE-the-whitelist-provenance.md` records what that looks like
when it goes wrong — a producer whose domain contracted to four missions
inside one working day, in three individually reasonable steps, recorded in no
commit message, docstring or excursion.

So the population is recomputed from the filesystem by code that shares
nothing with the proposers (`src/futon2/aif/enumeration_completeness.clj`:
its own path fences, its own status classifier, its own dedupe), and compared
member by member with what the records enumerated
(`holes/labs/wm-contract/u37_enumeration_replay.clj`).

## The line this narrative is bound to

    COUNTS: records 3; candidates/record 146; mission available 133 enumerated 133 missing 0 phantom 0 complete; excursion available 157 enumerated 0 missing 157 phantom 0 kind-not-enumerated; ticket available 33 enumerated 0 missing 33 phantom 0 kind-not-enumerated

`u37_enumeration_controls.sh` control 7 recomputes that line and requires this
file to carry it, so the prose cannot drift from the population it describes.

## What the three records say

All three: **VERDICT complete**. Identical numbers on all three — the ticks
are 13:46Z, 13:59Z and 14:02Z of the same day and the population did not move
between them.

### missions — 133 available, 133 enumerated, 0 missing, 0 phantom

The two sets agree member for member. Not "the same size": the membership diff
is empty in both directions, so no mission was enumerated that has no doc, and
no mission with a doc went unenumerated.

**2,209** files sit under `*/holes/missions/` across the code root; **1,619**
of them match the contract `M-*.md`; **133** are offered as candidates. The
scan's ledger accounts for the other **2,076** — one entry per file, each with
a path and one reason, and 2,076 + 133 = 2,209 exactly:

| reason | n | what it is |
|---|---|---|
| `:duplicate-id` | 1,207 | the same mission id already taken from a shorter path (the primary checkout); the fourteen `futon3c-*` sibling checkouts carry 97 mission docs each |
| `:non-contract-filename` | 590 | under `holes/missions/` but not `M-*.md` — handoff notes, journals, subdirectory material |
| `:status-terminal` | 104 | leading state token in ARCHIVED / PARKED / SUPERSEDED / ABANDONED / DEFERRED / COMPLETE / CLOSED / DONE / DISCHARGED / ANSWERED / DISSOLVED |
| `:derived-id` | 90 | a dotted id (`M-foo.handoff`) — a document named after a mission, not a mission |
| `:index-check-copy` | 84 | the `futon3c-index-check` directory copy |
| `:status-draft` | 1 | leading DRAFT, or "specified, not yet implemented" |

The `:status-*` reasons are counted over the 238 ids that survive the fences
and the dedupe, the others over files; that is why 104 + 1 is small beside the
rest. `:excluded` in the EDN carries every one of the 2,076 with its path, so
any single file's disposition is answerable without rerunning the scan.

**Where the 133 come from:** futon3c 35, futon6 19, futon4 17, futon0 16,
futon5 13, futon5a 12, futon2 10, futon3 7, futon7 3, futon3a 1. Every one is
a primary checkout. No item survives from a sibling worktree copy
(`futon3c-codex10-f18`, `futon6-old-copy`, …), which is what one would expect
and had not previously been measured: a mission that existed only on a branch
would enter the live candidate pool, and none does today.

**Time of the scan versus time of the records.** The scan runs on 2026-09-03,
the records are from 2026-09-02 13:46–14:02Z. Checked, so the equality is not
an accident of drift: across the ten contributing repos, `git log` since the
first record's start time shows **one** committed change to any `M-*.md`
(futon2 `161ac09`, `M-zaif-harness-v1`, a completion-criteria section), that
change does not touch the Status line, and **no mission doc was added or
deleted** in any repo in the interval.

### excursions — 157 available, 0 enumerated · tickets — 33 available, 0 enumerated

Both are typed `:kind-not-enumerated`, reason `:no-proposer-for-kind`. This is
not the check declining to look: the tick's proposer list is five entries
(`scripts/futon2/report/war_machine.clj:6005-6013`) — bootstrap, patterns,
missions, sorrys, tensions — and none of them walks `holes/excursions/` or
`holes/tickets/`. Confirmed by search: `excursion` appears four times across
`src/` and `scripts/`, all of them reading one specific named file
(`a4a_substrate.clj:15`, `cascade_lane.clj:181`) or using the word in prose;
`ticket` appears zero times.

So **190 items are outside the selector's field of view entirely**, and no
record of a tick says so, because a proposer that does not exist emits no
absence. That is the U37 finding: for missions the enumeration is complete and
now measured; for the other two kinds named in the row, the population is not
narrowed — it is not enumerated at all.

The file patterns for these two kinds (`E-*.md`, `T-*.md`) are
`:contract-source :declared-by-this-check`, not `:code`: no enumerator exists,
so no code defines a contract for them and this check states one. The mission
pattern is `:code` with its pointer (`src/futon2/aif/mission_registry.clj:28-29`).
The distinction is carried in the EDN per kind so a reader is never asked to
take a declared contract for a derived one.

## The regression pin

`bash holes/labs/wm-contract/u37_enumeration_controls.sh` — 5 negative, 2
positive, all planted through the environment so no committed file is mutated:

| control | plant | must be refused with |
|---|---|---|
| 1 | `U37_PLANT_WHITELIST=4` — the incident itself | `MISSING, no typed reason (129)` |
| 2 | `U37_PLANT_WHITELIST=132` — lose exactly one | `MISSING, no typed reason (1)` |
| 3 | `U37_PLANT_PHANTOM=M-this-mission-has-no-doc` | `PHANTOM, enumerated but not on disk (1)` |
| 4 | `U37_PLANT_KIND_ENUMERATOR=excursion` — claim a proposer that does not exist | `MISSING, no typed reason (157)` |
| 5 | `U37_CODE_ROOT=<empty dir>` | `PHANTOM, enumerated but not on disk (133)` |
| 6 | none | the unplanted replay exits 0 with `VERDICT: 3/3 records complete` |
| 7 | none | this file carries the COUNTS line the replay computes now |

Control 2 is there because a check with a tolerance would pass control 1 and
still miss a slow leak. Control 5 is there because a "check" that re-read the
record and compared it to itself would pass controls 1–4: pointing the scan at
an empty code root must turn all 133 recorded candidates into phantoms, and it
does.

## The live path

`FUTON_WM_ENUMERATION_ASSERT=1` makes a tick run the same comparison against
its own candidates and attach the typed record to its decision, where the trace
persists it (`trace-schema-version` 26). Default off, and off is byte-identical:
no scan, no key, and no selection path reads the key when it is present. Cost
of the scan, measured: 127–196 ms for all three kinds
(`scan-cost-2026-09-03.txt`).

## Gates run for this row

`clj-kondo` 0/0 on every changed file; `check-parens` OK; `bash -n` OK;
`u37_enumeration_controls.sh` 5 negative + 2 positive; the 14 test namespaces
that require `futon2.aif.trace` or `futon2.report.war-machine`, plus the new
one, one JVM each — 15 green, 355 tests / 1,868 assertions
(`test-run-2026-09-03.txt`); `runtime_validation_check` PASS (175 pointers, 0
unresolved) and `runtime_validation_controls` PASS after the U36 catalog feed;
`flip_readiness_check` PASS; p4ng `negative_controls` PASS (33 negative, 16
positive) and `pointer_check` 1,147 pointers 0 unresolved; `worklist_check` OK.

One red is left standing and is not this row's: `positive-proof-receipt-test`
fails `:positive-source-drift` because mathlib4 `6dabfb686f` (17:05Z today,
U29's glossary-pointer re-resolution) moved a declaration that
`softmax-positive-receipt.edn` pins by sha256. It fails with the U37 source
changes stashed as well; the reasoning is recorded at the foot of
`test-run-2026-09-03.txt`.

## What this row did not do

No tick was run and no run lock taken — the live assertion is wired and
unit-tested at its call site (`war_machine_test.clj`, flag on and flag off), not
witnessed on a live tick. Nothing was written to `:choices` or `:decisions`; no
proposer was added for excursions or tickets, which is what closing the other
two kinds would mean and is a decision, not a repair.

## Artifacts

- `replay-2026-09-03.edn` — the typed catalogue, one report per record, one
  comparison per kind, with the full `:excluded` ledger (path + reason per file)
- `replay-2026-09-03.txt` — the run log the catalogue was written from
- `scan-cost-2026-09-03.txt` — three consecutive scans in one JVM
