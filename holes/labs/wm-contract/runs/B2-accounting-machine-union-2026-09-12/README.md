# Packet B2: checked machine-contract accounting union

The accounting producer now invokes packet A's actual
`mathlib4/scripts/emit-machine-contracts.py verify` command before parsing the
manifest-verified bundle. This was chosen over duplicating its checks: the
verifier owns the fourteen-source population, git ownership, exact source and
bundle bytes, Holes component, ten-field declaration shape, and pointer checks.
A verifier refusal occurs before any accounting output is opened.

The verified eight declarations enter the existing row derivation. Their names
come from the verified bundle, their `:named` licences point into that bundle,
and their `:type-transcribed` licences point to the checked definition in each
owning Lean module. None has an entry in `checks/witness-registry.edn`, so all
eight stop at `:type-transcribed`, blocked on `:formula-transcribed`; none gains
readiness. Existing axes and ladder validation are unchanged.

`:as-of` is now derived as the greatest validated ISO date in the declaration
population. The checked union yields `2026-09-12`; the prior Holes-only population
yielded `2026-09-08`.

## Executed controls

All positive outputs were redirected to `/tmp`; the canonical accounting file
was not regenerated or staged.

- Two generator executions produced byte-identical output, SHA-256
  `7217db85e6e3b08a1c89c944fff629b0a153e980edc2095ce08f2ca37ac67ce1`.
- `WM_ACCOUNTING_OUTPUT=... bb scripts/generate_variable_situation_accounting.bb --check`
  passed with 141 rows and 141 resolving pointers.
- `bb scripts/generate_variable_situation_accounting.bb --negative-untyped`
  rejected the planted untyped hole.
- A disposable manifest with one changed source SHA was refused by packet A's
  verifier with exit 1 (`stale source pin`), and no output file was created.
- Against the pre-B2 producer run over the same current witness/worklist inputs,
  all 133 existing row maps are exactly equal, eight rows are added, and no
  existing row changes. Only two authority fields are added; `:as-of` advances
  by derivation.

The first attempted derived-date implementation used numeric `max` on ISO date
strings and exited 1 with `ClassCastException`; it was corrected to validated
lexicographic ordering, which is chronological for `YYYY-MM-DD`. The passing
controls above are from the corrected producer.
