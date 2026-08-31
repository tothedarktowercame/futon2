# C259 — workspace-gate receipt reuse

Date: 2026-08-31

## Result

`run-readiness` no longer executes `checks/wm_workspace_gate.clj`. It consumes
the latest terminal bounded-runner receipt for that exact command, run from the
Futon2 root, only when all of the following are true:

- inner and outer exits are zero, the gate verdict is `pass`, and the wrapper's
  independent `:resource-status` is `clean`;
- the wrapper receipt still identifies the current clean Futon2 content; and
- the gate's own provenance identifies the exact current clean content of all
  four repositories it checked: Futon2, Mathlib4, p4ng, and Futon3.

The gate now emits that four-repository provenance as JSON in its durable
output. Content identity, not wall-clock age, is the acceptance rule: an older
receipt over unchanged content remains evidence, while a newer receipt over a
different basis is refused. Missing legacy provenance is also refused. No
fallback rerun occurs inside readiness.

This preserves the readiness contract while removing the duplicate roughly
1 GB chain-rehearsal/gate JVM from the quiet-window sequence.

## Falsifier

Canonical invocation:

```sh
make run-readiness-workspace-receipt-control
```

The control constructs an exact four-repository basis and accepts it, then
changes only p4ng's tree identity. The changed basis must be rejected with
`:reason workspace-basis-differs` and `:repository p4ng`. Convention:
`0=control passed`, `1=control failed`, `2=mutation slipped`.

Live readiness was also run without executing a gate. It refused the available
legacy gate receipt as `receipt-missing-workspace-basis`; the current Futon2 and
p4ng trees were dirty as well. This is the intended fail-closed result, not an
instruction to rerun automatically.

## Verification

- `make run-readiness-workspace-receipt-control` — exit 0; exact basis accepted,
  changed p4ng basis rejected.
- `python3 -m py_compile scripts/run_readiness.py` — exit 0.
- `python3 scripts/run_readiness.py` — exit 1 (`NOT-READY`); workspace receipt
  refused as lacking the new provenance; no workspace gate launched.

Per C259, no workspace gate or full suite was run in this focused pass.

## Finding

The bounded wrapper's Futon2 identity alone is insufficient. The workspace
gate makes claims over four repositories, so safe reuse requires the gate's own
four-repository basis. Reusing only the wrapper basis would certify Mathlib4,
p4ng, and Futon3 changes the receipt never covered.
