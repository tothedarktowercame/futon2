# C77 — preemptive-repair mechanical lints

Date: 2026-08-31. Holder: `wm-evidence`. Consumer: the War Machine build gate and the owner of each reported locus.

## Contract

The six lints share `checks/preemptive_repair_lint.clj` but have separate canonical entry points and separate injected negative controls. A normal scan exits 0 when it finds no instance and 1 when it finds one or more. A negative-control run exits 0 only when its mutation is rejected and 2 when the mutation slips. The instrument suite gates the latter property; it does not turn known corpus debt green.

| Class | Falsifier implemented | futon2 | futon3 | p4ng | Positive exit | Negative exit |
|---|---|---:|---:|---:|---:|---:|
| acceptance cannot fail | nonzero reported findings paired with exit 0 | 0 | 0 | 0 | 0 | 0 |
| artefact boundary | untracked citation, older derived artefact, or stage branch asserting publication | 0 | 0 | 0 | 0 | 0 |
| stale baseline | exact-count assertion over a named live corpus | 0 | 0 | 0 | 0 | 0 |
| absence coerced | missing input becomes a value and its consumer cannot distinguish it; algebra/configuration identities exempt | **18** | 0 | 0 | **1** | 0 |
| era-blind expectation | exact schema/count assertion over timestamped records without an era term | 0 | 0 | 0 | 0 | 0 |
| record says two things | incompatible ratification/disagreement or duplicate status inside the current-status boundary | 0 | 0 | 0 | 0 | 0 |

The 18 absence findings are grouped sites from the pinned C12 census, not a new recount of source tokens. They remain honest red debt. The other five classes are extinct in the scanned tracked corpus at this pin; their injected controls demonstrate that zero is not caused by an inert scanner. In particular, preserved dated amendment prose is outside the `CURRENT TABLE END` current-status boundary and fixed-size test fixtures are not live-population baselines.

### C79 amendment — detector specimens are not corpus findings

After C77 was committed, four lints found their own `negative-text` specimens. This was not a repository regression: an untracked implementation file was absent from C77's tracked-file corpus, then became visible after commit. The reusable exemption is an explicit `PREEMPTIVE-REPAIR-SPECIMENS-BEGIN` / `PREEMPTIVE-REPAIR-SPECIMENS-END` region. The loader masks only that region while preserving newlines; it does not exempt `checks/` or the containing file. Direct negative-control rows bypass corpus loading and therefore still exercise the specimens. This is class 9's detector/fixture form: one file truthfully contains both the detector and examples of the state it rejects.

### C81 amendment — live absence population dispositioned

`checks/absence-coercion-dispositions.edn` covers all 18 grouped C12 rows: one producer boundary already fixed, one explicit serialization-compatibility exemption, and sixteen named blockers. The canonical absence scan is therefore 16, not 18. Its exact-coverage guard makes an added or silently removed census row an error rather than an implicit exemption.

### C94 amendment — deterministic variance carries provenance

`forward_model.clj` now emits `:variance-status` beside numeric variance, distinguishing a model-supplied value from `{:status :absent, :reason :deterministic-by-action-model}`. The live absence population is **15**: two fixed, one explicit exemption, fifteen blocked.

## Canonical invocations

For each stem below, run `bb -cp . checks/preemptive_<stem>_lint.clj` and append `--negative` for its mutation:

```
acceptance
artefact_boundary
stale_baseline
absence_coercion
era_blind
record_conflict
```

The instrument gate is `bb -cp . checks/preemptive_repair_suite.clj`; its expected result is exit 0 after all six mutations are rejected. The targeted test suite is `bb -cp . test/preemptive_repair_lint_test.clj`.

## Scope and prevention

This is lexical/structural prevention, deliberately not semantic synonym detection or countermodel generation. The scanner narrows assertions to their local expression, distinguishes current status from amendment history, treats stage output as executable shell rather than prose, and retains the C12 algebra/configuration exemptions. Those boundaries prevent the lint from recreating the defect class as false evidence.

Automatability score: **7/7**. Inputs are the tracked files of three named repositories plus the pinned C12 census; acceptance and mutations are executable; findings name locus and consumer; reads fail loudly; the scan is read-only; and ambiguous semantic cases are refused into review rather than guessed.
