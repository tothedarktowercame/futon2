# U10 per-R-node coverage

This survey covers every node in the table at
`holes/labs/wm-contract/SPEC-zaif-harness-v1.md:18-30`.  The machine-readable
matrix is `U10-node-coverage.edn`.  Its search trail names the four futon3c
test namespaces, U6, the WM AIF namespaces, and the zaif controller/hydrator.
No test or runtime code was changed.

## What exists

R2 and R16 have generated zaif memory-tool contract pairs
(`../futon3c/test/futon3c/agents/zai_memory_tool_contract_test.clj:103-148`).
R6 and R17 have direct zaif controller tests
(`../futon3c/test/futon3c/agents/zaif_controller_test.clj:7-34,181-227`).
U6 supplies partial mapped coverage for R1, R3-R6, R8, R14, R16, and R17
(`test/futon2/aif/zaif_full_loop_test.clj:291-620`).  That does not make the
two implementations one implementation: the live zaif controller computes
scalar arm values in `../futon3c/src/futon3c/agents/zaif_controller.clj:105-140`,
while WM belief, EFE, posterior, and learning live in `src/futon2/aif/`.
U6 explicitly adapts the former into the latter.

The provenance boundary matters.  U6's real inputs are the recorded operator
turn and arm-history counts.  Its R4 predictions and R8 realised observation
are planted by `test/futon2/aif/zaif_full_loop_test.clj:152-167`.  The negative
control at lines 539-562 produces different R5 and R8 orderings under different
plants, so those orderings belong to the plant, not to recorded zaif behavior.

S7's probe found real WM fields -- 14 R2 channels, 417-entry R1/R3
mu-pre/mu-post maps, R5/R6 decision explanations, and R14 tau -- in three tick
records (`holes/labs/zaif-harness/worklist.edn:51`).  They are evidence, not
unit-test fixtures: S7's extractor remains open, so the matrix does not count
them as tests.

## Gaps

- R1/R3 have no two-turn persistence/directional-update test, and R4 has no
  real zaif observation model.  D8 owns the real task-belief input; S7 owns WM
  fixture extraction.
- R5 has only plant-dependent U6 coverage.  D10 must put the clocked mission
  into real decision evidence before a real mission-conditioned risk test is
  possible.
- R6 has two implementations and the recorded zaif corpus is dominated by
  tie-order choices (83/114); D9 owns the replay after D8.
- R7 has no test or common implementation.  U9 owns the precision table and
  ordering assertion; S7 must locate or record the absent WM field.
- R9 and R13 have no tests.  The nearby API refusal tests do not establish R9
  worker/adjudicator separation, and the declared T=1 has no R13 pin.
- R8 and R14 are partial and plant-derived; R16 covers memory-family tools but
  lacks a real WM enactment; R17 pins the fixed zaif stub but does not connect
  it to WM receipt-fed learning.  S7 owns the missing WM fixtures.

In short, shared machinery exists inside the WM side and U6 deliberately calls
it, but no listed node currently has one production code path serving both the
live zaif and WM uptakes.  The spec table is presently the mapping between the
two implementations.
