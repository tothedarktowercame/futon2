# C87 — `r9VerdictConsultsChecker` proof receipt

Decision: the Lean proof is not meaningfully serializable as EDN, but its
elaboration evidence is.  `R9-VerdictConsultsChecker-proof-receipt.edn` names
and content-pins the existing proof source, pins the imported `Holes.lean`, and
records the independently emitted `#print axioms` result.  The EDN is a
receipt, not a replacement proof: the checker reruns Lean and compares the
exit and axiom set.

The binding is split from the three `VerdictTable` declarations because one
fixture cannot honestly have both table and proof-receipt shapes.

Canonical invocations (C16 convention):

```sh
bb -cp . checks/r9_proof_receipt_check.clj
bb -cp . checks/r9_proof_receipt_check.clj absent
bb -cp . checks/r9_proof_receipt_check.clj tampered
```

All return 0 only when the positive elaborates or the negative evidence is
rejected.  The missing and tampered controls exercise binding dependence;
they do not merely mutate the theorem's internal statement.
