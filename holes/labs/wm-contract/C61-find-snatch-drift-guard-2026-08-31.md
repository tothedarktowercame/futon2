# C61 — `find-snatch` drift guard

**Pins retained:** contract authority `26a66d88ca2ad67e779406be9bb0faddb283837f`;
C56 R2 corpus `b2c3aeb408cc4de59947ad93f9c1ea17b735fc0da26e188ada7c24609bffbca1`.

futon3 commit `0cf5524` makes nonzero antecedent drift a rejecting acceptance.
It does not change the authored clauses, runner predicates/text, or any binding.

Canonical invocations from `/home/joe/code/futon3`:

```sh
clojure -Sdeps '{:paths ["checks"]}' -M -m find-snatch
clojure -Sdeps '{:paths ["checks"]}' -M -m find-snatch --negative
```

The positive run emits `checks/find-snatch.edn`, reports 21 mismatches, prints
`finding=antecedent-drift`, and exits **1**. The negative control starts from a
synthetic clean drift result, injects one mismatch, observes the same rejection,
and exits **0**; a slipped mutation would exit 2.

This red is not a regression. It is the first honest result from an acceptance
that had reported the same 21 mismatches since `f1998a5` without acting on them.
Refreshing the four `findF*` pins past this red would preserve the false closure.

The F2 consequence is recorded beside the original law in
`holes/problems/P-validated-R5.md`: presence of a receipt cannot decide whether it
receipts the antecedent that actually fired when the runner and authored clause are
different representations.
