# C500 — the four `find` rows close under the J9 criterion

Date: 2026-09-03 · worklist row `:U46` · executes the J9 ruling
(`RUNBOOK.md`, "What ends a `closed-by-record` evidence obligation"), which
dispositions `findF1Containment`, `findF2Receipted`,
`findF3NonSelfCertifying` and `findF4Falsifiable` by name.

## 1. What moved

Contract holes 14 → 10, closed 110 → 114, declaration population unchanged at
124: four declarations moved between the columns, none was added or removed.
Accounting `:open-hole` 15 → 11 and `:proven-against-pinned-source` 11 → 15;
declaration fence `:pre-run-closable` 8 → 4. Every generated number came from a
regeneration.

One declaration per commit, as the row asked:

| declaration | mathlib4 commit | theorem |
|---|---|---|
| `findF1Containment` | `65ec7e4c89` | `wmFindSnatchF1Containment` (`Holes.lean:776`) |
| `findF2Receipted` | `357b8d0a08` | `wmFindSnatchF2Receipted` (`:788`) |
| `findF3NonSelfCertifying` | `ddef5448ab` | `wmFindSnatchF3NonSelfCertifying` (`:803`) |
| `findF4Falsifiable` | `0bab8f813f` | `wmFindSnatchF4Falsifiable` (`:817`) |

with `057e5eccbd` the re-emission. No transcription refused to discharge, so
there is no typed finding of the kind the row reserved for that case; §3 and §4
are the two things the discharges do *not* establish, recorded rather than left
for a reader.

## 2. The transcription

`futon3:checks/find-snatch.edn`, sha256
`839897ef8fe44952403700bd237389449ae4735d3da7df8239b1b94dc7ef4dfa` — the value
all four docstrings pin — becomes Lean literals at `Holes.lean:284-693`:
`SnatchPattern` (the 18 authored ids as constructors), `snatchRepository`,
`FindSnatchScenario` (the 6 recorded scenarios), `findSnatchZeroMass` (each
scenario's declared zero-mass pattern), `FindSnatchRowLit` and its `toRow`, and
two tables.

Two tables rather than one, because `find_snatch.clj` checks the four laws at
two grains and one table would have had to fudge one of them:

- `findSnatchRounds` — 34 rows, one per recorded round, the grain
  `find_snatch.clj:157-171` iterates. F1, F2 and F3 are proved here.
- `findSnatchScenarios` — 6 rows, each carrying the recorded `:selected-union`,
  the grain `find_snatch.clj:172-177` iterates. F4 is proved here. This is the
  stronger statement: F4 per recorded round follows from it.

The block is GENERATED from the fixture by
`holes/labs/wm-contract/u46_find_transcribe.bb`, not hand-typed, and the
generator is deterministic (two runs over an unchanged fixture write
byte-identical artifacts; there is no wall-clock field).

Each proof is `decide` over the table, no `sorry`, no `native_decide` — the
`lean_state_probe` still reports **0 axioms**, so nothing here rests on
`Lean.ofReduceBool`. Each has a Boolean form on the literal (`f1Ok`…`f4Ok`) and
a soundness lemma (`findF1Containment_toRow`…) carrying it to the set-level
declaration, so what is decided is the recorded rows and not merely some lists:
`toRow` reads each list as the set of its members, and
`listNil_of_setOf_mem_eq_empty` is the bridge for F1's empty-selection branch.
`f4Ok` discharges the existential and the `repository.Nonempty` conjunct from the
same recorded member — which is the degeneracy `C70` found and the 2026-08-31
amendment (`C85`) removed.

## 3. The pin names a snapshot the check no longer reproduces

Running `futon3:checks/find_snatch.clj` today PASSES — and **overwrites**
`checks/find-snatch.edn`, because the positive path recomputes the report from
the current `library/snatch` and spits it to that path. What it writes is not
what is pinned:

| | pinned | re-run 2026-09-03 |
|---|---|---|
| sha256 | `839897ef…` | `08a0c3e7…` |
| `:as-of` | `2734ac570e` | `10203307a1` |
| `:repository` | 18 patterns | 24 patterns |
| `:scenarios`, `:laws`, `:drift` | — | identical |

The six added ids are `grim-cuts-the-cascade-and-never-widens-it`,
`have-a-temperament`, `lead-with-the-exchange-rule`,
`play-the-authored-order-first`, `promote-the-remedy-before-the-exit` and
`widen-the-cascade-only-on-evidence`; none was removed.

What follows, and what does not. The recorded rows are stable — every selection,
receipt and absence is byte-identical across the growth — so the transcription is
of content that reproduces. The pinned 18-member repository is also the
*stronger* F1 statement (containment in a smaller set), so transcribing the pin
rather than the re-run understates nothing. But a `fixture-sha256` in a docstring
is supposed to name a file you can recompute, and this one does not: the check
that verifies the fixture is the same process that replaces it. That is a defect
in the pinning discipline, not in the four proofs, and it is why the producer
deliberately does not invoke `find_snatch.clj` — a producer that did would move
the thing it transcribes. The pinned file was restored after each measurement
here (`git checkout -- checks/find-snatch.edn`), and futon3's working tree is
left as it was found.

Repairing it wants its own row: either freeze the fixture (a `--check` mode that
compares instead of writing) or re-pin the docstrings to a regenerated record and
re-transcribe. Both are choices about the fixture's contract, not about F1–F4.

## 4. What F3's proof does not show

On this record the recorded non-self-certifying set **equals** the recorded
receipted set in all 34 rows: every recorded receipt is `:structured-antecedent`
carrying a warrant file, and none is `:score-alone` (96 receipts, one route).
So `wmFindSnatchF3NonSelfCertifying` discriminates nothing that
`wmFindSnatchF2Receipted` does not, and the difference between F2 and F3 is
carried by the rejecting control `--negative-f3` — which rewrites a receipt to
score-alone and is rejected — rather than by the evidence. Said in the theorem's
docstring, the declaration's docstring and the audit verdict, because a reader
counting four discharged obligations would otherwise read four independent facts
where the record supplies three.

## 5. The legs, checked for this row rather than inherited

- **(3) Lean transcription — MET**, §2.
- **(2) Rejecting witness — MET, at the current contract sha.**
  `find_snatch.clj` exits 0 with drift mismatches 0, and all five controls are
  rejected: `--negative-f1` (a selection member outside the repository),
  `--negative-f2` (the first selected pattern's receipt dropped),
  `--negative-f3` (a receipt rewritten to score-alone), `--negative-f4` (the
  recorded omitted member struck from the repository), `--negative` (antecedent
  drift). All re-run here. `checks/witness-fragments/findF1Containment.edn` is
  re-stamped to contract-sha `0bab8f813f`, and **two stale pins in it were
  corrected from the re-run rather than carried**: `:run-sha` was `594127059d`,
  and `:live-invariant :check-sha` was `ab2b290d`, which predates futon3
  `4e1c410446` (2026-09-02) moving the check onto the generic find/organise
  path. The U27 audit had flagged this binding as one pin behind; it was two.
- **(1) Persisted record — INAPPLICABLE, said so rather than counted as met.**
  The declared observation is a recorded find-receipt table over authored library
  text, not a run observation. What backs the fixture was checked rather than
  assumed: the drift check reports 0 mismatches against the authored antecedents,
  and the recorded rows reproduce (§3).

## 6. Controls

`u46_find_transcribe.bb` writes `runs/U46-find-rows/03-controls.edn`:

| control | result |
|---|---|
| C1 the fixture's sha256 is the value the four docstrings pin | PASS |
| C2 every emitted constructor name decodes back to the fixture keyword, and the decoded rows equal the fixture rows | PASS (18/18 names, 40/40 rows) |
| C3 a fabricated pattern occurs in no row, no repository, no emitted Lean text | PASS |
| C4 the four mutations `--negative-f1..f4` make are REJECTED by the producer's own evaluators | PASS (all four false) |
| C5 F3/F2 independence over this record | reported, §4 — 34/34 rows equal |
| C6 the pin is a snapshot, not a reproduction | reported, §3 |

C4 matters because it is what makes C2's round-trip mean something: a
transcription that could not fail is not evidence that this one passed.

## 7. Gates

| gate | result |
|---|---|
| `lake build DarkTower.WarMachine.Holes` | 2704 jobs, completed successfully, 0 errors, at each of the four commits |
| `lean_state_probe.bb` | 68/68 modules exit 0, 0 error diagnostics, 10 `sorry` (unchanged), **0 axiom**; source registers 10 holes to the contract's 10 |
| `find_snatch.clj` + 5 controls | positive exit 0, all five negatives rejected |
| `generate_variable_situation_accounting.bb` | WROTE; `--negative-empty`, `--negative-untyped`, `--negative-drift` all PASS |
| `gen_model_coverage.py` | 133 variables → 114 closed, 10 open, 0 unclassified |
| `negative_controls.sh` | PASS (33 negative, 16 positive) |
| `pointer_check.bb` | 1197 pointers in 3 files, 0 unresolved |
| `merge_witnesses.bb --check` | round-trips (43 entries) |
| `worklist_check.bb` | re-run after the ledger commit |

`negative_controls.sh` control 1c pins the two declaration columns the paper
prints; it moves 110/14 → 114/10 with the reason recorded beside U45's previous
re-pin, as that control requires. The pair still totals 124.

## 8. Not done, stated

- **No ruling was written.** Nothing was added to `aif-equations.edn :choices`
  or `control-map-edges.edn :decisions`. The J9 criterion and its disposition of
  these four declarations by name are Joe's, already recorded in `RUNBOOK.md`;
  this row executes them.
- `gen_aif_dag.bb` not run into a publish (TN §9a).
- The fixture-pin defect of §3 is **reported, not repaired** — repairing it means
  changing what the fixture's contract is, which is not this row's acceptance.
- The `find` and `organise` declarations are untouched: both are standing
  implementation refusals, closable only by an owner ruling, and they are why the
  demonstration-machinery row still shows 2 open holes rather than 0.
- The 18 drifted witness receipts (U29's debt, C499 §"the pre-existing red") are
  untouched; strict `contract_lint` and `wm_workspace_gate` remain red for that
  reason and the others C499 attributed one by one. `wm-status-receipt.json` was
  not regenerated here: `p4ng/wm-status.pdf` was already dirty in the shared
  checkout when this row started, and re-running a whole-suite status snapshot is
  not this row's acceptance. The figures that ARE derived from the contract were
  regenerated: `sec-lean-holes-generated.tex`, `sec-model-coverage-generated.tex`,
  `sec-variable-situation-generated.tex`, `sec-lean-state-generated.tex` and
  `war-room-tetrahedron.svg`.
