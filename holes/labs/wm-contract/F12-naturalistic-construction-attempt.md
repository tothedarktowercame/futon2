# F12 — fresh library construction does not yet exercise O4

This attempts the next naturalistic-exemplar slice after the Snatch warm-up,
using the repaired whole-library constructor identified in F12's progress-2b.
Joe's commissioned sequence is at
`holes/labs/wm-contract/RULINGS-walkthrough-2026-09-08.md:66-83`.

Run-record id **F12-32-naturalistic-construction** pins futon3 HEAD and the
constructor's input digest. The existing constructor gate passes, including its
five exercised negative controls, at
`holes/labs/wm-contract/runs/F12-organise/32-naturalistic-construction.edn:1-95`.
The fresh cascades have 20 nodes/3 edges and 41 nodes/14 edges; the receipt
contains their actual selected, admitted, added, node and edge sets, and their
construction records. All three executable O1–O3 predicates hold on both rows
(same run-record id). No claim of Lean conformance is made by this attempt.

The O4 implication returns true on both rows, but its antecedent is false and
four required observation fields are absent: acting order before/after and
score before/after. The measured refusal is at
`holes/labs/wm-contract/runs/F12-organise/32-naturalistic-construction.edn:154-162`
and
`holes/labs/wm-contract/runs/F12-organise/32-naturalistic-construction.edn:403-411`.
This is caused by the producer, which writes both precedence vectors as empty
and says nothing is played at `futon3:checks/construct_cascade.clj:402-421`.
Its report deliberately omits O4 from the checked laws and records the reason
at `futon3:checks/construct_cascade.clj:732-735`.

The repaired constructor therefore supplies a real graph, but not the
commissioned execution witness. Counting its implication as exercised, filling
the missing scores with zeros, or treating admission order as played order
would not supply that evidence. The scope is this constructor's two cascades;
this is not an assertion that no other naturalistic example can exist.

To remove this blocker, extend the naturalistic example with an execution
adapter for authored rules among the constructed members. Record an actual
precedence change, the resulting acting orders and scores with their sources,
and check the existing O4 predicate at `futon3:checks/find_organise.clj:541-549`.
The test must reject a changed precedence with both acting order and score
held fixed, and reject missing observations as an unexercised witness. Then
transcribe the actual record through the ruled Lean signature. The staged
Holes amendment remains contingent on that evidence; this attempt supplies no
new ruling and does not release it.

Reproduce from `/home/joe/code/futon3`:

```sh
bb -cp checks ../futon2/holes/labs/wm-contract/f12_naturalistic_attempt.clj
```

The command prints the receipt and exits **2** for an unexercised exemplar;
constructor failures throw before receipt emission. Two runs returned 2 and
produced byte-identical receipts. clj-kondo: 0 errors/0 warnings; check-parens:
OK. This is an offline construction using existing functions, with no WM run,
live JVM loading, registry edit, Lean edit, or publication regeneration.
