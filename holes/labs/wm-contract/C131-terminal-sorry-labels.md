# C131 — terminal `sorry` labels

Date: 2026-08-31

No declaration meaning changed.  The six remaining Lean `sorry` terms now name
their terminal category at the declaration site.

## Deliberate implementation refusals

- `C`: no record selects a vertex-local preference implementation; C122's
  census proves the global declaration is free but does not license a value.
- `find`: the recorded F1–F4 instances do not choose a canonical implementation.
- `organise`: the recorded O1–O4 instance does not choose a canonical
  implementation.

Each reads `DELIBERATE IMPLEMENTATION REFUSAL · contract kind HOLE
intentionally`.

## Permanent external attestations

- `preferenceStackLiveRecorded` names
  `checks/preference_stack_binding_check.clj` and `PreferenceStackWitness.edn`.
- `wmRunsOnce` names `checks/wm_runs_once_witness.clj` and the recorded
  `TickRunRecord`.
- `wmRunConformsToWiring` names `checks/wm_route_conformance.clj`, the recorded
  run, and the live Figure 4 edge layers.

Each reads `PERMANENT EXTERNAL ATTESTATION · Lean cannot prove an event ·
evidence is the executable witness · contract kind HOLE intentionally`.
C114's declined alternative is recorded beside each: narrowing would prove a
pinned instance, not the existing world-level claim.
