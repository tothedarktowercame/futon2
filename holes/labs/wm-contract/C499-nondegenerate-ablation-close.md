# C499 — nonDegenerateAblationLaw closes under the J9 criterion

Date: 2026-09-03. Worklist row `:U45`. Author ≠ reviewer: this needs a second read.

## What moved

One contract declaration, `nonDegenerateAblationLaw`, from `hole` to `closed`.
Contract holes 15 → 14, closed 109 → 110; declaration population unchanged at
124, so the pair's total does not move. Accounting `:open-hole` rows 16 → 15,
declaration fence `:pre-run-closable` 9 → 8.

- mathlib4 `4bc55c5968` — `Holes.lean`: the registry row and the docstring.
- mathlib4 `e4a8ac81cb` — `holes-contract.json` re-emitted at that authority.
- futon2 (this commit) — accounting, the U27 audit rows, witness rebind, probe.
- p4ng (this commit) — coverage, the three generated tables/figures, the
  re-pinned coverage control.

## The three legs, named separately

The J9 criterion (`RUNBOOK.md`, futon2 `a2641b3`) has three legs, and "meets the
criterion" is three different claims. Each was checked for this row rather than
inherited:

**(3) Lean transcription — met.** `wmRecordedAblationNonDegenerate`
(`Holes.lean:229`) proves the predicate over the pinned exact-dyadic table, no
`sorry`. Unchanged since `86186c3744`; re-elaborated here (`lake build
DarkTower.WarMachine.Holes`, 2704 jobs, 0 errors; 10 `sorry` warnings, the same
ten as before, none of them this declaration).

**(2) Rejecting witness — met, at the current contract sha.**
`checks/ablation_exact_dyadic_witness.clj` exits 0 over
`ablation-exact-dyadic.edn`, and `--negative` — which replaces every pragmatic
score with its G score so the minimizer sets coincide — is rejected. Both re-run
for this row. The fragment is re-stamped to `4bc55c5968`, so leg (2) holds at the
sha the contract now records, not at an older one.

**(1) Persisted record — inapplicable, and said so.** The RUNBOOK's leg (1) asks
for a persisted run record carrying the declared observation. This declaration's
observation is a recorded *score table*, not a run observation, so the leg does
not apply here. That is written into the docstring and the audit row as
`:inapplicable`, not counted as satisfied: the criterion's own escape clause
("where (3) is inapplicable, (1)+(2) suffice and the declaration says so") is
about which leg is skipped, and a reader is entitled to know which.

What does back the fixture, checked rather than assumed: it is an exact-dyadic
transcription of the `snatcher-dominant`/`g1` case of
`futon3:checks/ablation-snatch.edn`, a persisted record. All ten numbers — five
policies × {G, risk} — were decoded from their `[numerator denominator]` pairs
and compared against that record's doubles. Ten of ten equal. The fixture's
sha256 also still matches the pin in the docstring
(`f315b748420540688ef81086101b5789a4ecb2bd2a84c7a2b491f94fe8c56261`).

## The constructor, which is a deviation from the row's wording

`:U45`'s statement says "move the declaration `mkHole` → `mkClosed`". That names
the kind change, and the kind change is what happened. The literal constructor
was not used, and here is why: `mkClosed` takes only `name` and `owner`, so it
would have nulled `evidence` (`ExactDyadicAblationTable`) and `falsifier`
("recorded G and pragmatic minimizer sets overlap") in the emitted contract, and
stamped `decided "2026-08-30"` — three days before the ruling that closed it. The
falsifier is exactly the proposition the Lean theorem refutes; deleting it at the
moment of closing would remove the content of the closure.

Instead a `mkClosedUnderCriterion` constructor was added beside `mkRefutedByRecord`,
which exists in the same file for the same reason: its comment says `decided` is
"the date the record refuted it — not the batch date `mkWitnessedClosed` carries".
A close with its own date gets its own constructor. Emitted result:
`{kind: closed, decided: "2026-09-03", evidence: "ExactDyadicAblationTable",
falsifier: "recorded G and pragmatic minimizer sets overlap"}`.

## What was NOT done

- **No ruling written.** Nothing was added to `aif-equations.edn :choices` or
  `control-map-edges.edn :decisions`. The ruling is Joe's J9, already recorded;
  this row executes it.
- **The `86186c3744` demotion was not amended.** It stands in history as the
  pre-criterion state, per the row's instruction. The new docstring says so.
- **`gen_aif_dag.bb` was not run into a publish** (TN §9a). The paper was
  stage-built only (`./build-p4ng.sh futon-2026 --stage`), not published.
- **The `:lean-spine` pin in `Q-interface-completeness.edn` was not refreshed.**
  It is pinned at `f812795ca6` and has been `PIN_BEHIND` since U26 and U29 moved
  `Holes.lean` past it; refreshing it now would certify that the Q-facing
  declarations were re-verified across all three commits, and only this one was
  checked. The RUNBOOK's remedy is a per-binding re-verification, not a
  re-stamp, so it stays red and is reported.

## The pre-existing red this row ran into, and did not create

**18 positive-proof receipts are source-drifted, and were before this row.** The
witness rebind (C176 step 3) re-ran all 33 live witness fragments. 15 exit 0 and
were re-stamped; 18 fail, every one of them with
`:failures [:positive-source-drift]` from `checks/positive_proof_receipt.clj`.

This was attributed, not assumed. Each receipt's `:source-basis` pins a sha256
per *declaration text*; those texts were recomputed against both the current
`Holes.lean` and the pre-`:U45` blob (`8323c8ec23`) and compared to the pinned
values. Every stale declaration is stale in *both*: `stale-now` equals
`stale-before-U45` for all 30 receipts, and no receipt gained a stale
declaration. The cause is U29 (`6dabfb686f`), which re-resolved owner pointers
inside docstrings that these receipts had pinned, and did not re-record them.

Consequence for strict lint: it was already `FAIL` before this row (re-run
against the `6dabfb686f` contract: `{:rerun-and-rebind 37}`). It is still `FAIL`,
now at `{:rerun-and-rebind 18}` — the 18 whose checks cannot honestly be
re-stamped because they do not pass. **The 18 were deliberately not re-stamped**:
a `:contract-sha` on a fragment asserts that this witness ran and passed at that
sha, and stamping a failing check would make the registry say something false.

The same drift is what the `futon2` suite's single failure is
(`positive-proof-receipt-test/honest-positive-still-passes` against
`softmax-positive-receipt.edn`), already recorded in `:U44`'s evidence.

**This wants its own row**: re-record the 18 receipts against the current
declaration texts, with each theorem re-elaborated, and re-run the 18 witnesses.
It is U29's debt, it is 18 files, and it is not this row's `:acceptance`.

## Gates

| gate | result |
|---|---|
| `lake build DarkTower.WarMachine.Holes` | 2704 jobs, completed successfully, 0 errors |
| `lean_state_probe.bb` | 68/68 modules exit 0, 0 error diagnostics, 10 `sorry`, 0 axiom; source registers 14 holes to the contract's 14 |
| `contract_authority_current` | PASS-CONTENT-ONLY at `4bc55c5968` |
| `negative_controls.sh` | PASS (33 negative, 16 positive) |
| `pointer_check.bb` | 1196 pointers in 3 files, 0 unresolved |
| `generate_variable_situation_accounting.bb` | WROTE; all three negative modes (`--negative-empty`, `--negative-untyped`, `--negative-drift`) PASS |
| `gen_model_coverage.py` | 133 variables → 110 closed, 14 open, 0 unclassified |
| paper stage-build | 3 pages, 0 `ltx_ERROR` spans, PDF clean, 54 pages |
| `worklist_check.bb` | re-run after the ledger commit |
| strict `contract_lint` | **FAIL**, 37 → 18 stale; pre-existing, see above |
| `wm_workspace_gate` | **FAIL**; pre-existing, see below |

### The workspace gate's failures, attributed

53 failures, all pre-existing:

- 44 are the drifted-receipt class — the 18 witnesses and the negative controls
  that invoke the same checkers (`:c157`, `:c196-*`, `:c208`, `:c212-*`,
  `:c217-*`, `:c232-*`, `:c236-*`, `:c245-*`, `:c257-*`, `:c261-*`, `:c265-*`,
  `:c277-*`, `:c179-*`, `:c168`, `:c172`, `:c332`, `:c440`).
- `:strict-contract` — the same drift, above.
- `:q-interface-completeness` — `:lean-spine` pinned at `f812795ca6`, behind
  since U26; also `:runtime-efe`, `:runtime-selection`, `:topology` pins, none
  touched here.
- `:lean-sorry-categories` — 4 `:attestation-checker-absent` findings, all on
  holes added by H1b/H2/H3/H4 (`:U47`'s subject).
- `:mutable-verdict-claims` — `checks/trace_schema_compatibility.clj` undeclared.
- `:organization` and `:pinned-operational-certificate` — both fail on
  `p4ng/empirics-futon/control-map-edges.edn` pins, untouched here.
- `:p4ng-referent-drift` — 12 findings, all futon2 `src/` files cited in
  `sec-glossary.tex`; no futon2 `src/` file was edited by this row.

Corroboration that this is not new: the `wm-status-receipt.json` snapshot taken
at 16:40 today — before any of this row's commits — already records
`workspace-gate`, `sorry-category`, `referent-drift`, `futon3-suite` and
`mission-criteria-gauges` as red. The receipt was regenerated here; the two
components that moved green → red between the two snapshots are `strict-lint`
(U29's drift, above) and `futon2-suite` (the same drift's test).
