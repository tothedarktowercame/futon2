# C296 — executable format-proof recognition

Date: 2026-09-01. Owner: `wm-evidence`.

## Finding and correction

The live-artifact format-boundary lint had acquired the mirror of its original
defect. C284 correctly stopped accepting `FORMAT-PROOF` comments, but the
war-room rule then accepted only the literal token
`repair-status-population-valid`. The current war-room generator performs the
real check as a `doseq` membership validation immediately before aggregation,
so the lint reported a defect where executable proof existed.

The lint now recognises the bounded structural claim actually needed:

1. traverse the exact collection later aggregated;
2. validate the exact aggregated key against a literal finite set;
3. reject loudly in that same form; and
4. perform the validation before `frequencies`.

It does not treat comments, an equivalent check over another collection, a
generic validation helper, or unrelated nearby conditionals as proof.

The first rerun exposed a second instance of the same false-positive shape.
Commit p4ng `635b6a2` validates the live topology's aliased `counts` map by
presence, integer type, and closed vocabulary before formatting. The previous
regex looked only for one spelling containing `classification-counts` on the
same line. A second bounded recognizer now requires all three validations and
four loud exits before the exact formatter; it does not accept the associated
comment as evidence.

## Controls and result

```sh
python3 checks/live_artifact_format_boundary_lint.py --negative-control
python3 checks/live_artifact_format_boundary_lint.py
```

The control runner exited 0 and established:

- raw and marker-only `%d` inputs remain findings;
- explicit same-scope presence proof remains accepted;
- a marker-only categorical population remains unproved;
- exact collection/key membership plus loud rejection is accepted;
- the same proof over the wrong collection is rejected;
- the committed war-room generator is accepted;
- the committed live-topology generator is accepted;
- a topology proof marker without executable checks is rejected.

The live lint now reports **0 findings across 8 generators**, exit 0. This is
tested extinction after owner repairs, not suppression: both unsafe controls
still reject, unreadable inputs remain unavailable, and the recognizers state
their bounded false-negative limit. Arbitrary helper/interprocedural proof is
still reported as unverified because the lint cannot establish its relation to
the rendered carrier.

No generator was edited.
