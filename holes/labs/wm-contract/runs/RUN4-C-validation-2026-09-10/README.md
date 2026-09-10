# Current C validation and RUN4 launch gap

2026-09-10, Codex-17. Executed validation, not a live run or launch approval.
Basis: futon2 1ab816b0604b0ec17443d9691039e200a4de3cd5,
mathlib4 8b38ceec46d88a7b2a61439cb2e513bfcc96534f.

## Completed now

- `lake build DarkTower.WarMachine.LocalPreferenceModule DarkTower.WarMachine.PreferenceRiskWitness DarkTower.WarMachine.PreferenceRiskBoundary`
  exits 0, 2710 jobs. Imported Holes.lean reports nine existing sorry warnings;
  this is NOT a sorry-free whole machine. The named risk witness/boundary axiom
  prints contain propext/Classical.choice/Quot.sound, not sorryAx.
- `clojure -M:test -e "(require 'futon2.aif.disposition-risk-test)(let [r (clojure.test/run-tests 'futon2.aif.disposition-risk-test)] (System/exit (+ (:fail r) (:error r))))"`
  exits 0: 6 tests, 46 assertions, zero failures/errors.
- `bb -cp .:src -m checks.preference-risk-receipt /tmp/run4-c-current-20260910`
  exits 0: 24 actual runtime/Lean mass equalities, changed-seed control rejected,
  10 runtime checks. Generated certificate and binding source are copied here
  without editing. The generator hard-codes :as-of 2026-09-09; this README
  records the actual re-execution date instead of silently changing its output.
- `bb run4_readiness.bb --summary` exits 0:

```
RUN4 certificates        GREEN
RUN4 definitions-intact  GREEN
RUN4 wiring-pin          GREEN
RUN4 run-pins            GREEN
RUN4 regenerates         GREEN
RUN4 lean-probe          GREEN
RUN4 hole-open           GREEN
RUN4 closability-audit   GREEN
RUN4 invalidators        GREEN
VERDICT: READY
```

This meter does not establish successful activation of proposed run flags or
completion of every commissioning requirement outside its nine predicates.

## Decision about formalization scope

Recommendation: use the already validated C distribution and separated scalar
risk boundary for the currently specified RUN4. Do not create an ADICO-in-Lean
prerequisite by inference. The institutional selection design remains proposed;
no preference masses or observation bridge for selecting institutions have
been adopted. The constant kernel yields log 2 for every policy, so this
certificate cannot claim preference-sensitive institution selection. If that
new behavior is required in this run, its outcome/observation/selection contract
is additional implementation work, not a marker change.

## Concrete prerequisite uncovered: materialize the run's fold options

`futon2/scripts/futon2/report/war_machine.clj:5925` passes explicit keys through;
`:6360` consumes judge-opts. The proposed config's :seeded-c and
:disposition-kernel are descriptive strings, and :ruled-outcome-c-enabled? is
a nested warrant record. They are not the seed map, callable adapter and boolean
that the scorer requires. `scripts/futon2/run_tick_once.clj:246` constructs
judge opts but supplies none of these three. A search over src/ and scripts/
for the run-config variable and adapter/fold names found beta and depth sheet
readers, but no C materialization reader. Thus setting FUTON_WM_RUN_CONFIG to
this sheet is insufficient evidence that C is enabled on the stepped path.

Smallest implementation packet: a typed C run-config reader resolving a named
seed and a pinned fitted kernel artifact to the existing constant adapter;
wire it through the actual stepped judge entry; absent config must preserve
legacy opts, explicit false must win, prose/unrecognized selectors must refuse,
and the selected source digests and enabled flag must reach the decision record.
Test against the real fitted artifact plus wrong-digest and missing-artifact
controls. Do not add a new distribution, auto-fit a changing live corpus, or
claim institution-sensitive effects. This packet is not implemented here.

Other surviving evidence boundaries: the e-pre-go-live post-repin report
explicitly says O4 is unexercised and the reviewed paired experiment manifest
is missing; learning remains proposed after Joe withheld offline-only adoption.
The historical final checklist mixes old and superseding statements about
find/organise. Read its stated commissioning requirements separately from the
READY meter; this note does not erase them or change ledger state.

No locks, live ticks, data/ writes, registry changes, or Claude invocations.
