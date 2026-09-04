# C508 — RE7: the `:selection-discrimination` check

Row: `worklist.edn :RE7` (class I; epic `EPIC-run-era.md`). Producer:
`re7_selection_discrimination.bb`. Ledger API addition:
`run_era_ledger.bb --catalogue-add`. Certificates:
`mathlib4:DarkTower/WarMachine/Holes.lean:7556,7565,7627,7636`.

## What the check asks, and why 1 is not a threshold anyone chose

A decision whose chosen candidate shares its controller score with other
candidates was not made by the score: the score ranked a plateau and the sort's
tie-break picked the member. So the rule is binary — the chosen candidate's tie
is wider than 1, or it is not. 1 is the width at which a score has a unique
argmin, so "wider than 1" is exactly "the score did not decide it". Control C2
plants a **2**-wide tie and requires it to fire, so the 55 in `2026-09-01-s5` is
a measurement and not the threshold.

Per decision: `:tiebreak` (tie > 1), `:discriminating` (tie = 1), `:absent` (no
tie datum). Per run: `:defect` if any decision is `:tiebreak`; else
`:typed-absence` if any is `:absent` or there are no records; else `:green`.

`:defect` orders before `:typed-absence`, and the other order is arguable: a
tie-break the records DO carry is a fact about the run that a missing datum
elsewhere does not unmake. The absences travel in the row notes rather than
being dropped. Neither run exercises the ordering — all eight records carry tie
data.

## The two runs

| run | decisions | chosen rank | tie width | band | field | verdict |
|---|---|---|---|---|---|---|
| `2026-09-01-s5` | 4 | 123, 115, 123, 115 | **55** | 73–127 | 145 | `:defect` |
| `2026-09-04-re5` | 4 | 1, 1, 1, 1 | 1 | 1–1 | 146 | `:green` |

Ledger seq 15 (`:red`) and seq 16 (`:green`), `run-era-ledger.edn:264-291`.

## The census beside the green, which is the finding

`2026-09-04-re5` is green **and its score field is as flat as s5's**: every one
of its four ticks carries a plateau **56 candidates wide** that the chosen
candidate is not in. The run is green because the R14 selector head sits alone
at rank 1, not because the scoring discriminates.

So the plateau census is emitted beside the verdict and is never a verdict.
Control C8 shows it is not an input: the verdict recomputed with every
`widest-plateau-not-containing-choice` perturbed to 999 is the same verdict, and
so is the verdict computed from the rationale records with no trace read at all.
The Lean `SelectionTie` structure carries `widestPlateauNotChosen` as a field
that **no conjunct of `selectionDiscriminates` reads**, so the same separation
holds on the Lean side by construction.

Without this the green would be read as "the machine now selects by score", and
`02-plateau-census.edn` is what stops that reading.

## Two sources, and the second checks the first

The verdict is read from the run's **rationale records**
(`:rationale/chosen :controller-score-tie`, written at decision time,
`src/futon2/aif/selection_rationale.clj:139-153`). The plateau census is
recomputed from the **committed run-store trace**. Control C1 requires the
recomputation to reproduce every record's tie count, band and chosen rank — all
eight agree. A check that read one number and reported another would pass
without it.

**The trace read is deliberately not the one the records name.** For
`2026-09-04-re5` the records' `:rationale/trace-path` is an absolute path into
the live, untracked `data/wm-trace/wm-trace-2026-09-04.edn` — the file the tick
wrote as it ran, which no reviewer and no other machine can read. The census is
computed from `runs/2026-09-04-re5/wm-trace-re5.edn` instead. Measured, not
assumed: the live file holds exactly four forms, all four carry this run's
`:run/id`s, and they are equal as data to the store's four. (s5's records name
their run-store trace directly, so nothing is substituted there.)
`00-source.edn` records both paths and `:recorded-path-is-the-one-read?`.

## The Lean leg

`SelectionTie` transcribes each decision's tie datum; `selectionDiscriminates`
is the property the check verdicts on. `@[reducible]` because `decide` needs the
`Decidable` instance for that conjunction and instance synthesis does not unfold
an irreducible `def` — the `runConformsToDrawnWiring` precedent.

**The predicate is non-vacuous by construction rather than by a planted
mutation:** `wmS5SelectionDiscrimination` proves its *negation* for s5 and
`wmRe5SelectionDiscrimination` proves it for re5, over the same definition. A
predicate that could not fail could not have both. Two census theorems decide
the numbers (decisions, tie-breaks, widest tie, deepest rank, field size,
widest plateau not holding the choice) so the table above is decided rather than
asserted in prose.

All four `by decide`, no `sorry`, no `native_decide`; `lake build` exit 0 (2704
jobs), module `sorry` count unchanged at 10; `#print axioms` reports *"does not
depend on any axioms"* for all four (and still for U49's and RE5's two).
`holes-contract.json` re-emitted at authority `69721b1268` — 124 declarations
byte-identical, 114 closed / 10 holes, one field moving.

## Minting a check was the one part of the ledger with no API

`--deposit` refuses a check id that is not in `:ledger/check-catalogue`, and the
catalogue was reachable only with an editor — in a file whose header says it is
never hand-edited. `run_era_ledger.bb --catalogue-add` closes that through the
same validate-refuse-atomic-replace path `append-row!` takes.

It also mechanises the catalogue's own rule, which was a sentence until now: *"a
check enters the catalogue when its machinery exists"* is enforced by requiring
`:check/machinery`'s leading pointer to resolve. Enforced **at the add path
only**, not added to `validate`: turning it on for the seven entries already
written would change what `--check` means, and that is not this row's to decide.
(All seven do resolve today, checked.)

Four self-test controls, 20 → 24 (15 negative): C20 appends and shows every row
and `:ledger/head-sha` untouched (the sha chain covers rows only); C21 replays to
`:already-present` byte-identically; C22 refuses a divergent entry for an
existing id; C23 refuses an entry whose machinery does not resolve.

## What is NOT ruled here

**The ledger has no `:defect` verdict and this row does not mint one.** The
declared enum is `#{:green :red :typed-absence}` and `fold-by-run`'s status only
knows those three, so a fourth word would be a change to the status vocabulary —
a preference, and Joe's, exactly as RE6 left the analogous question open. The
CHECK's verdict is `:defect`; the DEPOSITED verdict is `:red`. Both words are in
the artifact, in the row notes and in the catalogue `:check/note`. This is the
one place the row's acceptance text and what was built differ, and it is stated
rather than quietly resolved.

No `:choices` entry and no `:decisions` entry was written.

## One consequence to see before it surprises someone

`2026-09-01-s5` now folds to **`:red`** rather than `:incomplete`.
`fold-by-run`'s `cond` puts `:red-verdict` ahead of `:typed-absences`, so the
six typed absences the run also carries are no longer the printed cause. Nothing
about them changed and RE6's blocker still stands; what changed is that the run
now has a red verdict to report, which is what a first red in this ledger looks
like. `2026-09-04-re5` stays `:incomplete` on four typed absences.

## One imprecision in the two deposited rows, and why it was not repaired

Both rows' notes say the census was "recomputed from the trace they name". For
s5 that is exact. For re5 it is **loose**: the file read is the run store's
committed extraction of the file the records name, not that file. It is loose
rather than false — the four forms are equal as data, measured above — and the
artifact the row points at states the substitution precisely.

It was not repaired because a deposited row's authored fields are final by
design: changing the producer's note text would make `--deposit` for these two
run-ids return `:divergent` instead of `:already-present`, trading a wording
improvement for the replay property the ledger exists to have. The wording is
fixed where it can be fixed — the Lean docstrings and `00-source.edn`.

## Gates, bare exits

`run_era_ledger --check` 0 after each of the three writes (one catalogue add,
two deposits) and on the committed result; `--self-test` 0 (24 controls, 15
negative); `--report` 0. The check itself: 9 controls, 4 negative, all pass on
both runs; two runs over an unchanged tree byte-identical. `lake build` 0;
axiom probe 0. `clj-kondo` 0 errors / 0 warnings on both touched scripts linted
individually; `check-parens` OK on both. `negative_controls.sh` PASS (35
negative, 18 positive); `pointer_check.bb` 1279 pointers / 0 unresolved;
`worklist_check.bb` 0.

## Not done, stated

No `gen_aif_dag.bb` run and nothing regenerated into the publish (TN §9a). No
machine run and no run lock taken — nothing here ticks. No third run: the two
the row names are the only ones in the ledger. No scoring or selection code
changed — this row measures the plateau, it does not remove it (that is U52's
ladder). No `:choices` and no `:decisions` entry.
