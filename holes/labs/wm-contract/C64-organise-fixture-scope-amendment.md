# C64 — fixture-indexed organise witnesses

Date: 2026-08-31

The original O1–O3 declarations were universal propositions about the polymorphic `organise` implementation. That implementation remains deliberately refused, so the declarations have been narrowed to propositions about the independently derived, pinned `CascadeDiff` fixture. This is an explicit scope weakening: the contract now claims a witnessed instance, not a universal law of `organise`.

The original O4 proposition was false: arbitrary constant `actingOrder` and `score` functions (and an empty policy type) defeat its existential conclusion. O4 now requires an explicit precedence-change hypothesis and proves the corresponding fixture's acting order or score changes. The hypothesis is part of the declaration rather than commentary.

Each binding has a declaration-specific falsifier:

- O1: `--negative-o1` adds an unrecorded node.
- O2: `--negative-o2` adds an unsupported reverse edge.
- O3: `--negative-o3` removes the required fast-forward while retaining authored reachability.
- O4: `--negative-o4` makes the two precedence variants' order and score identical.

The fixture is derived by hand from `P-validated-R5.md`, not emitted by an `organise` or diff implementation. Its source pin and all four mutations are checked by `checks/cascade_diff_witness.clj`.
