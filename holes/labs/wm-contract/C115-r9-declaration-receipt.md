# C115 — declaration-scoped R9 proof receipt

Date: 2026-08-31

Receipt version 2 no longer treats all of the 5,000-line `Holes.lean` file as
the proof source.  It extracts and hashes the exact local declaration basis:

```text
Layer · Claim · Witness · IndependenceVerdict · independenceVerdict
r9VerdictConsultsChecker
```

The stored whole-file Git SHA is provenance at recording time, not an
acceptance predicate.  Acceptance requires every named declaration to remain
present with its recorded source hash and reruns:

```sh
lake env lean <temporary file importing DarkTower.WarMachine.Holes>
```

The temporary file asks Lean for the theorem's axioms.  The live result must
still be exit 0 with `axioms: [propext]`; the declaration hashes establish what
statement/proof text that elaboration receipt covers.

Controls:

```sh
bb -cp . checks/r9_proof_receipt_check.clj
bb -cp . checks/r9_proof_receipt_check.clj tampered
bb -cp . checks/r9_proof_receipt_check.clj unrelated
bb -cp . checks/r9_proof_receipt_check.clj absent
```

All exit 0.  `tampered` changes `fun _ _ => false` to `true` in an in-memory
copy of the theorem source and is rejected specifically as
`:declaration-source-drift` for `r9VerdictConsultsChecker`.  `unrelated` appends
an out-of-basis comment to the same source and remains green while still
performing live elaboration and reproducing `[propext]`.  `absent` remains a
loud unreadable-receipt control.

## Workspace wiring

`bb -cp . checks/wm_workspace_gate.clj` now includes the R9 receipt along with
the twelve source/artifact checks C107/C111 classified as gateable.  It
explicitly excludes lane-registry state and the live operational certificate,
which remain operator-boundary checks.

The first runner invocation was honestly red because concurrent C112 edits had
made eleven other strict witness bindings stale.  C115 did not hide or rebind
them.  After C112 published its own fragments, the same unmodified workspace
command was rerun: all 12 checks passed, live strict qualification reported
fresh and inspectable, and the gate exited 0.  Repository CI remains unchanged.
