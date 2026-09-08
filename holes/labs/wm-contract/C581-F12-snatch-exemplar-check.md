# C581 — F12 slice 9 part 2: snatch exemplar check

## Scope and pins

The checker validates the committed snatch exemplar without editing its Lean
source, and records a passing verdict at
`futon2:holes/labs/wm-contract/runs/F12-organise/24-snatch-exemplar.edn:186`.
The derivation is pinned to futon3 commit
`cdb5e8a56fd907beb6a99f8b88af9de50ff93126`, with actual and expected values
equal at
`futon2:holes/labs/wm-contract/runs/F12-organise/24-snatch-exemplar.edn:44-45`.
The last commit touching `DarkTower/WarMachine/Holes.lean` is pinned to
`61c4825dc3e373fd1b761b800814bf85f5770b88`, again with actual and expected
values equal at
`futon2:holes/labs/wm-contract/runs/F12-organise/24-snatch-exemplar.edn:46-47`.

## Derived row and transcription

The checker loads `find-organise`, calls `read-repository`,
`cascade-diff-table`, `organise-laws`, `organise-reproduces-record?`, and
`fast-forward` in a futon3 subprocess; the source locations used are recorded
at `futon2:holes/labs/wm-contract/runs/F12-organise/24-snatch-exemplar.edn:111-114`.
The header supplies exactly 24 indices, is a bijection onto `0..23`, and names
exactly the derived repository patterns, as measured at
`futon2:holes/labs/wm-contract/runs/F12-organise/24-snatch-exemplar.edn:48-53`.

Added, admitted, and repository-pattern values match through that index map at
`futon2:holes/labs/wm-contract/runs/F12-organise/24-snatch-exemplar.edn:118-123`,
and selected at
`futon2:holes/labs/wm-contract/runs/F12-organise/24-snatch-exemplar.edn:130`.
Both acting-order lists match their derived row values at
`futon2:holes/labs/wm-contract/runs/F12-organise/24-snatch-exemplar.edn:116-117`,
and both precedence lists and the scores `3` and `-5` at
`futon2:holes/labs/wm-contract/runs/F12-organise/24-snatch-exemplar.edn:124-129`.
All 26 authored `standsOn` pairs agree between the repository derivation and
the Lean literal at
`futon2:holes/labs/wm-contract/runs/F12-organise/24-snatch-exemplar.edn:131-185`.
The aggregate transcription result is true at
`futon2:holes/labs/wm-contract/runs/F12-organise/24-snatch-exemplar.edn:1`.

## Per-declaration pointer binding

A file-wide set of fixture spans cannot detect citations exchanged between two
docstrings. The checker therefore binds every fixture span to the declaration
whose docstring contains it; actual and fixture-derived expected maps agree at
`futon2:holes/labs/wm-contract/runs/F12-organise/24-snatch-exemplar.edn:89-110`.
That binding would have caught part 1's defect: six value docstrings cited the
wrong fixture spans, because the expected owners for added, acting, score, and
precedence are explicit at
`futon2:holes/labs/wm-contract/runs/F12-organise/24-snatch-exemplar.edn:100-109`.
The citation-exchange plant preserves the citation multiset, lands, and still
turns the verdict false at
`futon2:holes/labs/wm-contract/runs/F12-organise/24-snatch-exemplar.edn:77-80`.

## Three routes to the surviving edge

The subprocess calls `find-organise`'s own `fast-forward` over the full node
set. Its ten mapped edges are recorded at
`futon2:holes/labs/wm-contract/runs/F12-organise/24-snatch-exemplar.edn:13-23`,
and they equal both the row's computed edges and the ten-entry duplicate-free
Lean list at
`futon2:holes/labs/wm-contract/runs/F12-organise/24-snatch-exemplar.edn:12-41`.

The same function over `nodes \ added-by-organise` independently yields only
`[0 2]` at
`futon2:holes/labs/wm-contract/runs/F12-organise/24-snatch-exemplar.edn:40`.
Filtering the Lean ten through `snatchInAdded` also yields only `[0 2]` at
`futon2:holes/labs/wm-contract/runs/F12-organise/24-snatch-exemplar.edn:24`.
That predicate is READ from the Lean rather than re-spelled here
(`futon2:holes/labs/wm-contract/f12_snatch_exemplar_check.bb:86-90`), and its
four indices are required to equal `snatchAdded`, at
`futon2:holes/labs/wm-contract/runs/F12-organise/24-snatch-exemplar.edn:25-26`.
The distinction is not cosmetic: an earlier build of this checker carried the
four indices as a literal, and dropping `22` from `snatchInAdded`
(`mathlib4:DarkTower/WarMachine/F12SnatchExemplar.lean:233`) left it passing,
so the declaration the third route decides through was unguarded. The derived
no-bootstrap set, the Lean filter, and the literal expected singleton agree at
`futon2:holes/labs/wm-contract/runs/F12-organise/24-snatch-exemplar.edn:42`.
Thus the earlier headline “nine of ten refused” is mechanically witnessed by
a ten-edge full set and a one-edge surviving set, rather than left as prose
(`futon2:holes/labs/wm-contract/runs/F12-organise/24-snatch-exemplar.edn:13-42`).

## Laws, carrier shape, and forbidden forms

All four derived organise-law predicates are true and the row reproduces its
record at
`futon2:holes/labs/wm-contract/runs/F12-organise/24-snatch-exemplar.edn:54-55`.
The conformance proof uses exactly `o1`, `o2`, `o3`, `o4`, `oattr`, `oauth`,
and `osel`, adds no `structure Conformant`, reuses both ruled sans structures,
and finds both definitions in `F12RuledCarrier.lean`, as measured at
`futon2:holes/labs/wm-contract/runs/F12-organise/24-snatch-exemplar.edn:3-10`.
Counts of `sorry`, line-leading `axiom`, and `native_decide` are all zero at
`futon2:holes/labs/wm-contract/runs/F12-organise/24-snatch-exemplar.edn:43`.

## Mutation controls

| Plant | Landed | Verdict after | Evidence |
|---|---:|---:|---|
| Drop selected acting member in fixture | true | false | `24-snatch-exemplar.edn:57-60` |
| Flatten after score in fixture | true | false | `24-snatch-exemplar.edn:61-64` |
| Flatten after precedence in fixture | true | false | `24-snatch-exemplar.edn:65-68` |
| Drop recorded added member in fixture | true | false | `24-snatch-exemplar.edn:69-72` |
| Remove protect→institutions before fast-forward | true | false | `24-snatch-exemplar.edn:73-76` |
| Exchange declaration citations, preserving multiset | true | false | `24-snatch-exemplar.edn:77-80` |
| Shift one declaration span | true | false | `24-snatch-exemplar.edn:81-84` |
| Append a theorem containing `sorry` | true | false | `24-snatch-exemplar.edn:85-88` |

All eight controls landed and all eight mutated verdicts are false at
`futon2:holes/labs/wm-contract/runs/F12-organise/24-snatch-exemplar.edn:56-88`;
the first five perturb the fixture or derived relation and re-run the derivation
path rather than editing a verdict field (`futon2:holes/labs/wm-contract/f12_snatch_exemplar_check.bb:162-189`).

Four further controls were run against the checker during review, each restored
afterwards, and are recorded in the worklist row's `:progress` rather than in
the artifact, because they mutate the Lean source the checker reads: dropping
`22` from `snatchInAdded` (`mathlib4:DarkTower/WarMachine/F12SnatchExemplar.lean:233`),
dropping `(18, 22)` from `snatchOrganisedEdgeList`
(`mathlib4:DarkTower/WarMachine/F12SnatchExemplar.lean:226-228`), repointing the
authored pair `18 → 22` to `18 → 23` in `snatchRepo`
(`mathlib4:DarkTower/WarMachine/F12SnatchExemplar.lean:54-56`), and exchanging
header indices `7` and `11`
(`mathlib4:DarkTower/WarMachine/F12SnatchExemplar.lean:9-11`). All four fail the
checker. One control that must NOT fail — appending an unrelated arithmetic
theorem — still passes, so the checker discriminates rather than refusing any
edit at all.

## Open reading, not a ruling

Part 1 leaves one reading for Joe: instantiating this snatch row at the ruled
signature cannot simultaneously reproduce the run's ten organised edges and
satisfy ruled o3. The measured full-node fast-forward has ten edges at
`futon2:holes/labs/wm-contract/runs/F12-organise/24-snatch-exemplar.edn:13-23`,
while the ruled no-bootstrap fast-forward over the selection alone has only
`[0 2]` at
`futon2:holes/labs/wm-contract/runs/F12-organise/24-snatch-exemplar.edn:40-42`.
The run fast-forwards over the up-closure; the ruled clause fast-forwards over
the selection after subtracting organised additions
(`mathlib4:DarkTower/WarMachine/F12SnatchExemplar.lean:100-114`). Which reading
to register or rule is Joe's decision. This checker records the divergence and
stops; it makes no ruling (`futon2:holes/labs/wm-contract/runs/F12-organise/24-snatch-exemplar.edn:186`).
