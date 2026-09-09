# FoldC successor integration: two axes — 2026-09-09

Claude's review of 02b317f5 / 78e1482a / b76719cd23 is accepted as the
integration direction: preserve the old preference-fold mathematics and check
the scalar contribution on a separately typed axis. This refines the era
comparison, rather than translating the illustrative values 3/8/7 into KL.
The successor certificate already exists at futon2 6356087c and mathlib4
2af52ef97e; its independent review is pending. This note does not implement the
integration or make the old red gate green.

## Consumer census

Read-only rg census of futon2 src/checks/test, futon3/checks,
mathlib4/DarkTower/WarMachine and p4ng/empirics-futon found:

- futon2/src/futon2/aif/ruled_outcome_c.clj:54 defines fold-declaration.
- futon2/checks/fold_c_witness.clj:42-69 is the runtime consumer: it filters
  folded/in-ruled-sum rows into one set, then compares that with Lean and fixture.
- futon2/test/futon2/aif/ruled_outcome_c_test.clj:33-46 expects the four IDs,
  their provenance, c-int outside the ruled sum and ruled-outcome-c folded.
  A typed-axis addition need not erase any of these facts.
- futon2/checks/wm_workspace_gate.clj:309 invokes the positive wrapper;
  lines 547-549 invoke the two negative modes. Preserve these entrypoints.
- futon2/checks/witness-registry.edn:231 and
  futon2/checks/witness-fragments/FoldC.edn:2 route through that wrapper and
  retain the old positive receipt. Ledger recording belongs to claude-1.
- mathlib4/DarkTower/WarMachine/FoldCWitness.lean:14-17 declares the empty
  preference-layer list and proves its identity. No other use of that named
  declaration appeared in the surveyed Lean tree.

This is a census of the named directories and symbols, not a whole-workspace
proof of absence. The production scorer does not consume fold-declaration to
calculate KL; its calculation remains in efe.clj and disposition_risk.clj.

## Proposed integration contract

1. Retain the four declaration IDs and their provenance. Add an explicit
   scalar-risk classification for the actually folded ruled-outcome-c record;
   do not toggle folded? false to hide it from the check. Classification of an
   unimplemented contribution must remain unimplemented/undetermined, not an
   invented active preference layer.
2. Classify every active in-scope record before comparing. Unknown or ambiguous
   active classifications refuse. Compare the preference-layer axis with the
   old empty Lean/fixture set, and the scalar-risk axis with the named successor
   witness and runtime certificate. Neither axis may be silently discarded.
3. The scalar axis must validate source pins and rerun the finite binding and
   scorer checks. A matching name alone is not sufficient. Re-emitting a fresh
   certificate is not the same as validating the previously reviewed one.
4. Keep the old positive receipt's historical claim. Update its misleading
   current-runtime prose only through source-aware re-attestation if touched.
   Keep the two old mutation controls; add independent wrong-axis, omitted-risk,
   stale-certificate and wrong-scorer controls. Separate baseline failure from
   escaped mutation in diagnostics without casually changing the workspace
   runner's established exit contract.
5. Regenerate the successor certificate when its pinned runtime declaration
   changes; preserve deterministic two-run comparison and review provenance.
   No source or ledger mutation is performed by this proposal.

This integration is the next implementation step after successor review. Then
resume the parked find marker and contract/accounting/probe/prereg/audit refresh
chain, and run the full p4ng build as part of acceptance, as Claude requested.
The e-pre-go-live experiment brief f9433625 remains the separate commissioned
organise task; it has not yet run.
