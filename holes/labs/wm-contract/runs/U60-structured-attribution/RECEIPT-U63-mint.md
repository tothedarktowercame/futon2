# U63 -- the curated mint, and the receipt drift it causes

Row `:U63` (worklist.edn), epic `EPIC-run-era.md`. Two things: repair the mint
payload U52 got wrong, then complete U60's mint into
`holes/labs/wm-contract/tension-ledger.edn` for real. `RECEIPT.md` beside this
file is U60's, whose mint went to a copy for the two reasons this row removes.

## 1. The payload asserted one reason over a two-reason partition

`refusal-payload` (`src/futon2/aif/task_belief_ladder.clj:393-438`) wrote
`:task-belief/refusal :task-belief/zero-support-construction-exhausted` and one
`:refused-count` over the whole rung-3 partition. `classify`'s hole rule
(`:262-300`, added in `24cea67e`, AFTER U52's mint `7157af97`) had already made
a second reason reachable: a candidate with `:open-hole-count 0` is refused
`:no-open-holes` whatever its case history says (`refusal-record:335-338`). On
the 2026-09-05 u60 field 53 of the 98 refusals carry that reason, so the tension
minted from this payload would have said **98 zero-support refusals over a
partition of 45 and 53**.

What the record carries now: `:task-belief/refusal` is the vector of reasons the
RECORDS carry, `:refused-by-reason` one count each, `:refused-count` the total
they sum to, and `:refused-sample` a sample per reason -- one drawn from the
union cannot be read against either count. `refusal-reasons`
(`:311-321`) is the declaration; `refusals-by-reason` (`:323-328`) counts what
is there, so an undeclared third reason would appear as its own count rather
than vanish into one of these two.

Measured on the three fields (`runs/2026-09-05-u60/ladder/05-controls.edn`):

| field | rung-3 | `:no-open-holes` | zero-support |
|---|---|---|---|
| s5   | 99 | 53 | 46 |
| re5  | 98 | 53 | 45 |
| u60  | 98 | 53 | 45 |

`u52_ladder.clj`'s `:positive/refusal-is-typed-and-grounded` (`:311-356`) passes
on all three, `mistyped 0` and `ungrounded 0`. It checks four things it did not
check before: every reason is one of the two DECLARED; the basis grounds THAT
reason (a `not found` string for zero support; the `:no-open-holes` rule plus
the preserved `:refusal/overridden-task-belief` for the hole rule, which is what
distinguishes it from a candidate the ladder never reached); and the payload the
mint would carry has the records' counts, under the records' reasons, summing to
the records' total. Two controls stay red on all fields for the reason U60
recorded and this row does not touch: the live `data/wm-trace` corpus has grown
since 2026-09-04, so `a-recorded-chosen-key-reaches-rung-1` (s5) and
`case-history-wins` (re5, u60) no longer reproduce.

## 2. The mint landed in the curated ledger

`mint-curated.txt`: `u52_mint_refusals.bb --refusals
runs/2026-09-05-u60/ladder/04-refusals.edn --field u60 --append`, with
`FUTON_TENSION_LEDGER` UNSET, through `append-tension!`
(`u41_tension_ledger.bb:161-203`) -- the ledger's sole write API.
`:wm-ladder/u60-3416e82b-zero-support` appended carrying
`:tension/provenance {:records ["3416e82b-771d-454d-8d4e-ae3d279cd23c"]}`, the
tick of the stepped run `runs/2026-09-05-u60`. Ledger validates at 8 tensions /
16 events, 0 defects, all 12 u41 controls pass, and
`runs/U41-tension-ledger/` is refreshed to match.

IDEMPOTENT BOTH WAYS, shown rather than asserted: re-running the u60 append
returns `already-present`, and the COMMITTED two-field artifact
(`runs/U52-ladder/04-refusals.edn`, not regenerated -- its payloads no longer
reproduce on today's corpus) still replays `already-present` for both U52
tensions. The payload change would have broken that had those two been
regenerated, which is why they were not.

U60's outstanding acceptance clause is discharged on the curated artifact: run
`2026-09-05-u60`'s `:tensions-cashed` scan now attributes
`:wm-ladder/u60-3416e82b-zero-support` `:attribution :structural`,
`:declares` and `:matched` the tick id, ledger fold
`{:prose-scan 7, :structural 1}`.

## 3. THE RECEIPT DRIFT, MEASURED AND NOT REPAIRED

`:live-derivation` folds the WHOLE ledger into EVERY run's receipt
(`u41_tension_ledger.bb:602-613`), so a tension added anywhere rewrites every
deposited run's replay. `receipt-drift.txt` is the measurement,
BEFORE and AFTER, both produced by `u63_receipt_drift.bb` -- a read-only replay
of u41's own receipt builder, re-derivable by pointing `FUTON_TENSION_LEDGER` at
`git show <sha>:holes/labs/wm-contract/tension-ledger.edn`:

| deposited run | replays byte-identically BEFORE the mint | AFTER | verdict deposited | verdict on replay |
|---|---|---|---|---|
| `2026-09-01-s5`           | no  | no | `:typed-absence` | `:green` |
| `2026-09-04-re5`          | no  | no | `:typed-absence` | `:green` |
| `2026-09-04-010-accepted` | YES | no | `:typed-absence` | `:typed-absence` |

**`2026-09-04-010-accepted` is the one this row moved.** It was the last
deposited receipt that still reproduced; it now differs in exactly three fields,
all of them whole-ledger folds and none of them about this run:
`:live-derivation` (`:tensions` 7 -> 8, `:events` 15 -> 16, `:status-fold`
`{:carried 7}` -> `{:carried 8}`, one more `:statuses` entry),
`:run-provenance` (`:tension-provenance-shapes` gains `[:pointers :records
:when :who]`; `:events-by-date` gains `{"2026-09-05" 1}`) and
`:why-the-ledger-cannot-be-run-scoped` (the sentence's carrier count moves).
Its OWN scan of itself does not move -- `:run-id-appears-in-ledger?` false,
`:tick-ids-appearing` `[]`, no `:run-attribution` key -- and its verdict does
not move. Control 8u (`p4ng/empirics-futon/negative_controls.sh:869-921`) is
now that measurement: it pinned byte-identity under U60 and pins the shape of
the drift here.

`2026-09-01-s5` and `2026-09-04-re5` were ALREADY divergent before this row, and
by more: both were deposited at 5 tensions with `:verdict-deposited
:typed-absence`, and both have replayed `:green` since the 2026-09-04 U52 mint
put their tick uuids into the ledger's prose. This row deepens that divergence
(5 -> 8 tensions rather than 5 -> 7) without moving either verdict.

**NOTHING IS REPAIRED.** No deposited receipt file is rewritten, no run-era row
is touched, no `--deposit` was run. The three rows keep the verdicts they were
deposited with; `run_era_ledger.bb --check` is green over 24 rows / 0 defects.
That is the receipt-only reading `:AD1` proposes to record as a machine-adopted
default -- **and `:AD1` is still `:open`**: this row reports the divergence
because its acceptance requires it to, and rules on nothing.

## Not done, stated

No `aif-equations.edn :choices` and no `control-map-edges.edn :decisions` entry:
this row records no ruling. `gen_aif_dag.bb` NOT run, nothing regenerated into a
publish (TN 9a). No tick, no run lock, no substrate call, no network: the ladder
replay is read-only over `data/wm-trace` and the committed run store. Nothing
written under `data/`. `runs/U52-ladder/` NOT regenerated. The two other red
u52_ladder controls (section 1) are U60's finding and are left where they are.
The pre-existing untracked `holes/labs/zaif-harness/runs/build-loop.{lock,log}`
were left alone.
