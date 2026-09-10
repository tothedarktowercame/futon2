# U80 flagged policy-F binding: independent review

Codex-17, 2026-09-10. Accepted 6c9cce18 with correction
d427e0aa450a11b5fa902c4f77a31d5812d48cc2 for the limited registry binding.

Independent tests: 6 tests, 22 assertions, zero failures/errors. Both actual
command controls pass with mutation-changed? true and the registered
eligible-record-lacks-identity-joined-candidate-f cause. Repeated the original
stale-source-pin reproduction: source-digest-mismatch now has no mutation
attribution. Altered fixture bytes similarly report fixture-digest-mismatch;
unknown flags report unknown-negative-control. Inspection confirms the baseline
must pass first, identity multiplicity/cardinality is retained, and candidate
F values must be finite.

Scope remains flagged-path-witness-held-open: four frozen S4 records, three
eligible positive records with 145 candidates each, and one explicit
incomplete-coverage exclusion. This does not establish default-path operation,
a new live event, or closure of the Lean declaration.

Independent current category-check invocation returns pass? false with exactly:
- enactedEqualsSelectedWhenRankOneGated: recognized-evidence-absent
- policyPrecisionIsGammaFromBeta: recognized-evidence-absent

These are the remaining recognized-evidence gaps in this checked corpus, not
claims of universal absence. Their retained evidence requirements remain as
recorded in U80-three-unbound-evidence-census-2026-09-10.md. No live run, Lean,
audit, worklist, frontier or model-choice registry was changed by this review.
