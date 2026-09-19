# Receipt: dismiss two hermetic fixture-pollution findings

Date: 2026-09-19 UTC

Each call used:

- authority: `Joe -> claude-12 repair-queue ownership 2026-09-19; CLASSIFY @ 3db4e8e3 rows 1-2 (authorized transition, not a pretended repair)`
- reason: `:fixture-pollution-subject-not-production`
- cause fix: `futon2 6cdb308a (hermetic fixtures), warrant test-registry-15940d9e…`
- actor: `codex-24`

The reviewed verb at `02387be6d05b594009fb4b5c06b7071a718a4362`
derived all proof from the immutable finding payloads. Both calls returned
`:dismissed-fixture-pollution`; there were no refusals.

## Queue counts

Using the runner-eligible `open-obligations` predicate (`:open`, excluding
`:environmental-hold`):

- Before: **9**
- After: **7**

## Dismissal records

- `data/wm-repair-obligations/dismissals/repair-ea1-a7a5fc7c81ad45251922d33718170eba32a100cda770df0af9bcd759e28df913--attempt-001-artifact-binding-mismatch.edn`
  — SHA-256 `4504fdad345b9b1f1fbd7d30c50c19e55de0ef2a65507d72f087c9cfaacdcd28`
  — retained resolution `/repo` → `/tmp/debug-standing-readback13347244878016165142`
- `data/wm-repair-obligations/dismissals/repair-ea1-b28b40fe3c109454107faa2309cfcf0e51abf79e39c436fda76a2d7d665f1913--attempt-001-artifact-binding-mismatch.edn`
  — SHA-256 `5fec9dc7ed8c12c42376dd764d95d9d7d1174f14f41327714c0c097d8140f985`
  — retained resolution `/repo` → `/tmp/debug-baseline6082102988716713796`

No other finding was dismissed and no click occurred.

## Subject rebind without rerun

Called `futon3c.test-registry.validation/bind-subject!` with actor
`codex-24`; no test command was executed:

```edn
{:entry/type :subject-binding
 :entry/id "65757de9-50c3-4c88-af91-92065f7ab3a3"
 :subject-id "repair-store/dismiss-unexecuted"
 :warrant-id "test-registry-81279a57e10a87a3351c115a860f2e78f714a4091d3215ee4b86a9fb5c9a6acf"
 :actor "codex-24"
 :at "2026-09-19T18:53:34.110617294Z"}
```
