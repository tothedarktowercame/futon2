# C570 — F12 mining exemplar evidence

## 1. What this record exercises

The record contains the direct chain hierarchy → candidate menu → scorecard at
`futon2:holes/labs/library-loop/runs/mining-exemplar/cascade.edn:33-43`.  The
checker re-derives that chain as `[[0 1] [1 2]]` and matches both ordered pairs
and directions to `miningRepo` at
`futon2:holes/labs/wm-contract/runs/F12-organise/19-mining-exemplar.edn:135`.
Thus the two direct-edge theorems give O2 and O3 true antecedents at
`mathlib4/DarkTower/WarMachine/F12MiningExemplar.lean:87-99`, unlike the
edge-empty ants fixture recorded at
`mathlib4/DarkTower/WarMachine/F12AntsExemplar.lean:73`.

The same chain also distinguishes ruled fast-forward from unrestricted
reachability.  Ruled fast-forward refuses `0 → 2` because the intermediate
member `1` is not outside the member set at
`mathlib4/DarkTower/WarMachine/F12MiningExemplar.lean:101-115`; the full-reach
negative control admits that path and is refuted specifically through O3 at
`mathlib4/DarkTower/WarMachine/F12MiningExemplar.lean:123-153`.

## 2. What this record cannot stress

The record has one precedence map, not a before/after pair, at
`futon2:holes/labs/library-loop/runs/mining-exemplar/cascade.edn:66-69`, and one
acting order at
`futon2:holes/labs/library-loop/runs/mining-exemplar/cascade.edn:70-76`.
Accordingly the transcription uses each value on both sides, making O4's
antecedent false by construction at
`mathlib4/DarkTower/WarMachine/F12MiningExemplar.lean:55-65`; this is not a
measured precedence or acting-order change.

No unnamespaced score field occurs in the record, as measured by
`:transcription :score-keys-found` at
`futon2:holes/labs/wm-contract/runs/F12-organise/19-mining-exemplar.edn:138`.
The Lean carrier therefore instantiates `Score := Unit` at
`mathlib4/DarkTower/WarMachine/F12MiningExemplar.lean:59`, so this run cannot
stress O4's score-change disjunct.  The admission set is empty at
`futon2:holes/labs/library-loop/runs/mining-exemplar/cascade.edn:30-32` and is
independently matched by `:transcription :admitted` at
`futon2:holes/labs/wm-contract/runs/F12-organise/19-mining-exemplar.edn:134`;
arm four's attribution input is consequently instantiated empty here.

## 3. Record and pointer measurements

The checker derives the header index map, selected and admitted sets, both edge
directions, ranked precedence, acting order, and score-key absence from the EDN
before comparing them with the Lean text; the resulting paired values are at
`futon2:holes/labs/wm-contract/runs/F12-organise/19-mining-exemplar.edn:128-139`.
It binds record citations to their individual owning declarations rather than
comparing a file-wide set; actual and derived expected bindings agree at
`futon2:holes/labs/wm-contract/runs/F12-organise/19-mining-exemplar.edn:75-90`.

The three rule sources are read from the record's futon3 pin recorded at
`futon2:holes/labs/library-loop/runs/mining-exemplar/cascade.edn:10-12`.
All three files exist, each path matches the rule's `:member`, and each span is
non-empty and exactly equals its marker-inclusive THEN block, as measured at
`futon2:holes/labs/wm-contract/runs/F12-organise/19-mining-exemplar.edn:94-127`.
All three spans include the `+ THEN:` marker and therefore fail zaif's strict
`from > then-line` convention while passing the marker-inclusive convention;
both predicates remain visible at
`futon2:holes/labs/wm-contract/runs/F12-organise/19-mining-exemplar.edn:94-127`.

At that same pin, the parsed `@why`/`@how` targets contain none of the three
member IDs at
`futon2:holes/labs/wm-contract/runs/F12-organise/19-mining-exemplar.edn:42-43`,
while the separately parsed `@see-also` targets are recorded per pattern under
`:library-edge-absence :patterns` at
`futon2:holes/labs/wm-contract/runs/F12-organise/19-mining-exemplar.edn:13-41`.
This supports only the asserted absence of a library edge among these three
patterns; the two record edges remain mission-attested relations at
`futon2:holes/labs/library-loop/runs/mining-exemplar/cascade.edn:33-43`.

## 4. Structural and mutation controls

The measured conformance block uses all seven ruled clauses, introduces no new
`Conformant*` structure, and reuses both `ConformantOrganiseRuledSansO3` and
`ConformantOrganiseRuledSansO4` from the ruled carrier, at
`futon2:holes/labs/wm-contract/runs/F12-organise/19-mining-exemplar.edn:2-9`.
The forbidden-token counts are all zero and the `Holes.lean` last-touch pin is
unchanged at
`futon2:holes/labs/wm-contract/runs/F12-organise/19-mining-exemplar.edn:10-11`.

Six controls reverse an edge, permute precedence, exchange two declaration
citations, shift a citation, append `sorry`, and move a THEN span outside its
block.  Every plant landed and every mutated verdict is false at
`futon2:holes/labs/wm-contract/runs/F12-organise/19-mining-exemplar.edn:44-74`.
The citation-exchange control additionally records that its citation multiset is
unchanged at
`futon2:holes/labs/wm-contract/runs/F12-organise/19-mining-exemplar.edn:55-59`,
so it directly tests the permutation-blind failure mode rather than changing
the set of citations.

These are measurements only.  The checker verdict is recorded at
`futon2:holes/labs/wm-contract/runs/F12-organise/19-mining-exemplar.edn:140`; no
ruling or worklist transition is made here.
