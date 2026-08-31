# C55 — contract remainder census

At authority `26a66d88ca2ad67e779406be9bb0faddb283837f`, the contract has
93 declarations: 64 closed and 29 holes.  Normal lint reports 8 unwitnessed,
16 stale, and 5 refused.

## Cost of the strict flip

All 16 stale bindings have named checks and have been inspected; **zero are
stale because nobody looked**.  Fourteen have contract drift only.  Two
(`preferenceStackLiveRecorded`, `wmRunsOnce`) also have fixture drift and need
real reruns rather than an authority-only rebind.  Thus all 16 are assignably
fixable for freshness.

Fourteen stale rows have conformant implemented shape checks.  Two do not:
`r9VerdictConsultsChecker` is a proof-term claim without a separately
inspectable proof artefact, and `preferenceStackLiveRecorded` points at its
executable checker rather than a serialized `PreferenceStackWitness`.
Refreshing those two bindings would satisfy freshness while leaving this
evidential limitation intact.

## Remaining unwitnessed work

- `P-validated-R5`: the four organise laws are blocked together on one
  serialized `CascadeDiff` fixture and an executable checker.  No such
  artefact/check was found.
- `P-R9`: `wmVerdictsLedgerAlone` and `wmVerdictsDeclared` can bind the existing
  R9-D2 table/check now. `valueEvidenceRequiresL2` has a pre-scored 7/7 work
  unit but still needs its WitnessLayerTable check and negative control.
- `organisation`: `wmRunConformsToWiring` has an existing route receipt,
  executable conformance checker, and 7/7 binding work unit; it is ready now.

That is **4 blocked / 4 dischargeable now** among the eight unwitnessed rows.
The exact per-record rows and rerun commands are recorded in
`contract-remainder-census.edn`.

## Refusals

The five refusals are not counted among the eight unwitnessed.  Four remain at
their prior specificity (`C`, `find`, `organise`, `machineHasNoC`).
`modelUncertaintyAndEIG` is sharper: carriers and canonical EIG now exist, and
the missing object is specifically a theorem equating the live spread bonus
with canonical outcome-weighted posterior-to-prior KL.
