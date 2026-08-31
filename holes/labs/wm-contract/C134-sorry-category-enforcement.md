# C134 — enforce terminal `sorry` categories

Date: 2026-08-31

`checks/lean_sorry_category_check.clj` makes C131's source labels bidirectional
and closed.  It parses each declaration with its immediately preceding Lean
docstring and enforces:

- every `:= sorry` has exactly one allowed category;
- deliberate implementation refusals and permanent external attestations must
  have `sorry`;
- witnessed-instance obligations must not have `sorry` (the compatibility
  required by C112's five intentionally proved obligations);
- permanent attestations name at least one existing `checks/*.clj` executable;
- any `… · contract kind HOLE intentionally` category outside the three-name
  vocabulary fails.

The literal request that every category label correspond to `sorry` conflicts
with C112: `WITNESSED-INSTANCE OBLIGATION` deliberately labels proved
definitions.  The enforced compatibility matrix preserves that decision while
still rejecting a permanent-attestation label on a proved declaration—the
reverse-C109 defect named in C134.

Four mutations independently exercise the requested failures: an unlabelled
`sorry`, a double label, a permanent label on proved `machineHasNoC`, and an
attestation naming a missing checker.  The check and all four controls are
explicit workspace-gate commands; the file is also in the gate's closed
inventory.
