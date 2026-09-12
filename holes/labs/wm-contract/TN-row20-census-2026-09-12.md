# TN row 20 — census scope for R11 and R15

Date: 2026-09-12. Scope: discovery only. No production, census, registry, or
generated publication file is changed here. All line references below were
re-read at futon2 HEAD while preparing this note.

## 1. What the census is today

There are two evidence axes, and conflating them makes row 20 look easier than
it is.

The formal readiness ladder is assembled by
`p4ng/empirics-futon/gen_rnode_dossiers.py`. It derives whether a node has an
equation or is declared plumbing and refuses the ambiguous case
(`gen_rnode_dossiers.py:375-382`). Equation-bearing nodes have the full formal
ladder (subject to a FUNDAMENTALS cap); plumbing terminates formally at
`named` (`:619-625`). That is how R14 and R17 obtain their current
`formula-transcribed` rows: the generated table records R14 with one equation
and R17 with two (`p4ng/sec-rnode-dossiers-generated.tex:78,85`). This formal
rung is not process evidence.

The process-evidence census is the editable facts document
`holes/labs/wm-contract/ALIGN-rnode-process-census.md`. Its schema is seven
ordered lifecycle cells — commissioned, dispatched, parked, returned, checked,
recorded, surfaced (`ALIGN-rnode-process-census.md:201-210`). Its vocabulary is
defined at `:8-12`: `exists` means node-linked running code refuses advancement
or records that transition; `named-only` means conduct is named without such a
check or record; `absent` means the recorded search found no node-linked
implementation. Generic Agency conduct is deliberately not credited
(`:214-218`).

The consumer parses exactly that matrix (`p4ng/empirics-futon/gen_rnode_dossiers.py:249-275`).
For a node with no row it renders `.......` and no evidence pointer (`:504-511`).
For plumbing, merely reaching the terminal formal rung `named` is insufficient:
all seven lifecycle values must be exactly `exists` before the node is at its
obligated state (`:477-486`). The published legend says `.......` means outside
census scope, not seven absences (`:633-640`). Thus inclusion grants a typed,
pointer-bearing statement for every lifecycle cell and makes the node eligible
for the obligated-state calculation. Absence from scope costs both: no cell is
claimed, and a plumbing node cannot be counted complete even when its formal
ladder has correctly reached `named`.

The current matrix contains R9, R10, R12, R20, TRACE and R16 only
(`ALIGN-rnode-process-census.md:203-210`). A useful correction to the task's
example is that R14 and R17 do **not** currently have lifecycle rows. R17 was
explicitly omitted from the original requested census
(`ALIGN-rnode-process-census.md:16-23`); R14 is a loop node never added. Their
formal evidence exists on the other axis, while both show `.......` in the
generated table. R11 and R15 have the same outside-scope display
(`p4ng/sec-rnode-dossiers-generated.tex:79-80`).

## 2. R11 — Hierarchical shared budget

### Declared claim

R11 is declared plumbing, not equation-bearing: the registry's plumbing vector
contains R11 and R15 at `holes/labs/wm-contract/aif-equations.edn:520-521`.
The operational contract says that if multiple AIF agents act on shared state,
a coordination layer must make their actions compose coherently, but calls the
current single-observer WM scope N/A until another writer shares its substrate
(`docs/futon-aif-completeness.md:280-284`). The catalogue's narrower apparatus
claim requires locally factored proposals to be arbitrated against one shared
budget at every hierarchy level (`p4ng/sec-catalog.tex:243`). These scopes are
not silently equivalent.

### Machinery and evidence present

The narrower finite arbiter is real source machinery:

- `src/futon2/aif/hierarchical_budget.clj:157-212` selects the exact feasible
  portfolio, records every node's usage/budget, and asserts the all-ancestor
  charging invariant.
- `src/futon2/aif/hierarchical_budget_adapter.clj:66-107` adapts separately
  ranked fields and emits a self-contained replay receipt.
- `src/futon2/aif/policy.clj:23-31` exposes an explicit boundary.

This does not make it live. The retained V7 census
`runs/V7-R11-node-sim/00-r11.edn` (SHA-256
`b80a827f5260fb5bce9cfe34ba9192ca3a4d3690d005194cf0d3de5501323042`)
searched 631 code files, found zero production callers, and found no R11 trace
route or derived arbiter output; its negative controls plant and detect a
production caller, trace witness, equation, campaign artifact, and stale
citation. `VERIFY-r-nodes.edn:1841-1858` records the reviewed conclusion and
receipt. The catalogue narrates Campaign S, but the V7 search found the supplied
identifiers only in prose, not a structured run record. It therefore cannot be
promoted into lifecycle evidence.

### Verdict

**(a) A working-evidence row is constructible now.** Its honest strip is
`-------`: every cell is `absent`, with a node-specific basis saying the
implemented arbiter/adapter/policy boundary has no production caller and no
retained node-linked run record. Pins: the V7 receipt above;
`hierarchical_budget.clj:157-212`;
`hierarchical_budget_adapter.clj:66-107`; `policy.clj:23-31`; and the R11 V7
review account at `VERIFY-r-nodes.edn:1841-1858`.

Verdict (b) does not apply. Draft basis if someone proposed it, to make the
refusal explicit: “R11's *formal* ladder is complete at `named` because it is
declared plumbing and has no equation; its *obligated* ladder is not complete,
because zero of seven node-linked lifecycle transitions is evidenced.” Verdict
(c) is unnecessary for census inclusion. Later runtime credit would require an
integration-owned production call plus retained node-linked judgement/trace and
the appropriate lifecycle boundary records; row 20 should not build that.

## 3. R15 — Hierarchy and timescale

### Declared claim

R15 is also declared plumbing at `aif-equations.edn:520-521`. The operational
claim is a two-or-more-level generative model in which an upper-level state
parameterises a lower-level prior and belief propagates across levels
(`docs/futon-aif-completeness.md:434-439`). The catalogue states a narrower
explicit coupling: a strategic target conditions a fast loop, independently
witnessed fast outcomes update the next slow state, and temporal discount is
not reused as commitment temperature (`p4ng/sec-catalog.tex:245`). It expressly
limits Campaign S to coupling, not a general nested generative model.

### Machinery and evidence present

`src/futon2/aif/temporal_hierarchy.clj` implements the narrower two-timescale
mechanism. Its module contract distinguishes hierarchy from rollout depth and
states its two-level limitation (`:1-43`); `apply-slow-prior` changes the fast
loop's prior/cost (`:111-163`); `hierarchical-rollout` applies that shaping
before rollout (`:165-180`); and `advance-slow-state` refuses unwitnessed fast
outcomes before producing the Beta update and next slow mode (`:190-237`).

Again, this is not live-path evidence. The retained V7 census
`runs/V7-R15-node-sim/00-r15.edn` (SHA-256
`508054e635f153e8f6f892e0de1a94d18234d668d7f34166810cf87f3a17dc17`)
found references confined to the namespace, its test, and the V7 harness; zero
production callers; zero `:slow/mode`, `:slow/intrinsics`, or
`:slow/previous-mode` values in 889 retained trace records; and zero R15 route
hops. Its nine negative controls include planted slow-state trace and caller
evidence. The reviewed account is at `VERIFY-r-nodes.edn:1771-1839`. The
catalogue's Campaign S prose is preserved in the receipt, but no structured
Campaign S record was located, so it cannot license an `exists` cell.

### Verdict

**(a) A working-evidence row is constructible now.** Its honest strip is also
`-------`: all seven cells `absent`, based on the retained caller/corpus census,
not inferred from the lack of a matrix row. Pins: the V7 receipt above;
`temporal_hierarchy.clj:1-43,111-180,190-237`; and
`VERIFY-r-nodes.edn:1771-1839`.

Verdict (b) does not apply. Draft basis for refusing a false completion:
“R15's formal plumbing ladder terminates at `named`; its obligated lifecycle
ladder is incomplete at zero of seven `exists` cells, and the built standalone
two-timescale namespace has no production caller or retained slow-state trace.”
Verdict (c) is unnecessary for the census row. To earn later positive cells,
an integration owner must connect the slow/fast mechanism, retain its slow
state and R15 route identity, and commission the relevant lifecycle boundaries;
the broader nested-belief construction remains separate build work under the
R15 implementation obligation, not a census edit.

## 4. Smallest row-20 packet split

1. **R11 census row.** Add one R11 matrix row with seven `absent` cells and a
   node-specific evidence paragraph citing the pinned V7 receipt and current
   arbiter/adapter/policy boundary. Run the dossier consumer's matrix/schema
   controls and confirm the rendered strip changes from `.......` to `-------`
   while `at-obligated-state` remains false. No production or registry edit.
2. **R15 census row.** Add one R15 matrix row with seven `absent` cells and a
   node-specific evidence paragraph citing the pinned V7 receipt and current
   temporal-hierarchy source. Run the same controls and confirm only its scope
   marker changes; no live or general nested-model claim is introduced.

These packets are independent facts rows. A later positive-cell packet must be
triggered by new node-linked runtime/lifecycle evidence and may update only the
cells that evidence actually serves.
