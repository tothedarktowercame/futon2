# C56 — stale-binding rerun stopped on R2 failure

**Authority pin:** `26a66d88ca2ad67e779406be9bb0faddb283837f`

**Result:** stopped without rebinding, as required by the packet. All 16 bindings
remain stale. No registry fragment was changed.

## Executed groups, in order

| Declarations | Check | Exit | Result |
|---|---|---:|---|
| `nonDegenerateAblationLaw` | `cd /home/joe/code/futon3 && clojure -Sdeps '{:paths ["checks"]}' -M -m ablate-g-snatch` | 0 | passed; verdict `nonDegenerate-holds` |
| `findF1Containment`, `findF2Receipted`, `findF3NonSelfCertifying`, `findF4Falsifiable` | `cd /home/joe/code/futon3 && clojure -Sdeps '{:paths ["checks"]}' -M -m find-snatch` | 0 | executable passed; it nevertheless reported 21 drift mismatches, which its current acceptance does not reject |
| `r9VerdictConsultsChecker`, `r9WmVerdictsSound`, `r9TwoRunCensus`, `r9WmPerRowDeclarations` | `bb checks/r9_independence.clj --report holes/labs/wm-contract/R9-D2-report.edn --lean holes/labs/wm-contract/R9-D2-report.lean --p4ng /home/joe/code/p4ng` | 0 | passed; generated artefacts byte-identical |
| `wmTraceR2`, `r2ContractCensusWmTrace` | `bb checks/r2_channel_contract.clj --report holes/labs/wm-contract/R2-D2-report.edn` | 1 | **failed**: 54 files, 801 forms, 799 conforming, 2 key-set mismatches/failures |

The R2 failure is recorded in `R2-D2-report.edn` and its regenerated Lean fixture.
The two historical 2026-05-18 records still lack `:annotation-health`; the newly
expanded corpus did not repair them. The content pin is now
`b2c3aeb408cc4de59947ad93f9c1ea17b735fc0da26e188ada7c24609bffbca1`.

## Not executed after the stop

`wmTraceR8`, `r8CensusWmTrace`, `r8EraBoundary`,
`preferenceStackLiveRecorded`, and `wmRunsOnce` were not run. Their bindings were
not refreshed. This is an explicit skipped state, not a silent absence.

## Qualification consequence

Strict qualification remains red. Rebinding the earlier exit-0 groups alone would
hide the atomic migration's failed stopping point, so none were rebound. The R2
records must be repaired or the contract's acceptance deliberately revised before
C56 resumes. Separately, the find check's 21 tolerated drift mismatches require a
decision about whether its exit-0 acceptance is strong enough for strict renewal.
