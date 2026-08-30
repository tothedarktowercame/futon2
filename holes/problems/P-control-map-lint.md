# P-control-map-lint — The wiring as a checked contract

Problem record (delivery-lifecycle v2). Not a node: the instrument that moderates the overlap points.
Opened 2026-08-30 by claude-15 on Joe's go. Owner: claude-15. First packet the tech lead dispatches.

## S1

**problem.** The R-node build fans out to several builders at once (`BUILD-tech-lead-charter.md`). The
overlap points are the Figure-4 edges, lifted to data in `p4ng/empirics-futon/control-map-edges.edn`
(22 `:edges` — 21 `:drawn`, 1 `:unresolved` `R5→R5` — and 26 `:derived-undrawn` all carrying `:by`, one of which is the `:chartered` R20→R14; 48 entries in all — corrected 2026-08-30 after claude-20's pre-dispatch parse: this record first double-counted the chartered edge). Nothing checks that an edge
has a payload schema, a fixture, and two endpoint records that agree on it. Without that, two builders can
each satisfy their own record and disagree at the edge — which is how R8's five individually-defensible
deliveries composed into silence.

**now.** `checks/library_graph_lint.clj` (futon3) is the precedent: baseline, attestations, fails closed,
report EDN, fixture tests. The `Delivery` type is in `futon4/holes/delivery-lifecycle.md` §0.6.

**solved.** A bb linter `futon2/checks/control_map_lint.clj` over the EDN such that: every `:drawn` edge
carries `:schema` (a map or the typed absence `:unspecified`), `:fixture` (one example payload, or
`:unspecified`), and `:endpoints-agree?` computed from the two `P-R<n>.md` records referencing the edge — **as a typed value, not a boolean**: `:no-endpoint-record` when neither endpoint has a record, `:one-endpoint-record` when one does, `:schema-unspecified` when both records exist but the edge has no `:schema` to compare (builder's fifth value, ratified by claude-15 2026-08-30 at the owner gate), `true`/`false` only when both exist and a schema does (today: 0 drawn edges with both, 6 with one, 15 with none — claude-20, 2026-08-30; a boolean `false` there would assert disagreement where there is nothing to compare, F1's empty-vs-didn't-run);
every `:derived-undrawn` edge carries `:by`; per node, in/out counts and how many edges are fully specified;
the report fails closed on a drawn edge with no entry at all. **Baseline, honestly:** the first run reports
21 drawn edges with `:schema :unspecified` — that number is the build's starting organised-fraction for the
wiring, and it is reported in two lines (specified vs unspecified), never as a percentage of "done".
**Falsifier:** a fixture test with a drawn edge missing from the EDN must fail; a fixture with two endpoint records and a `:schema` that one of them does not mention must report `:endpoints-agree? false` — exercised on fixtures only, since no live edge has two records yet.

**facades:** a schema copied from a code signature and presented as agreed by both endpoints; a fixture that
is the only payload the edge has ever carried (a fixture is an example, and the record must say so);
"specified" counted from the presence of a key rather than a non-`:unspecified` value.

**status.** open.
**holder.** claude-20 (tech lead) → codex-1 (CML-D1)  
**parent.** BUILD  *(fifth precept, §0.10 — added 2026-08-30)*

## deliveries
- **CML-D1 — build (one file, one fixture test).** As above. Gates: kondo, check-parens, `bb -cp . test/…`
  green, run twice deterministic, `git diff --check`. Commit explicit paths in futon2. No push.
- **CML-D2 — schema packets, one edge each**, dispatched by the tech lead only after both endpoint records
  exist; the schema is written into the EDN by the *owner* (claude-15) after the two builders' proposals are
  compared — never by a builder.

## log
- 2026-08-30 record written (claude-15).
- 2026-08-30 CML-D1 owner gate PASSED (claude-15): `7272099` + `dfe8c80`; tests 4/20/0 re-run; live lint 15 `:no-endpoint-record` / 6 `:one-endpoint-record`; claude-20's whole-token fix is right (substring matching would have agreed on 'the **id**entity'); `:schema-unspecified` ratified.
- 2026-08-30 amended (claude-15) on claude-20's pre-dispatch findings: entry count 48 not 49; `:endpoints-agree?` typed absence. Both commissioner-side; caught by the tech lead's row-11 run before the builder saw them.

**Two lines, 16:48Z (claude-20's cadence report, reproduced from the linter):** drawn 21 — **specified 0 /
unspecified 21**; endpoints: `:no-endpoint-record` 15, `:one-endpoint-record` 6, both 0. The wiring's organised
fraction is where it was this morning, and that is the honest number after a day of nodes-before-edges.
**Caveat measured:** each of the six one-endpoint edges (`R2→R3`, `R16→R2`, `R7→R8`, `R8→R5`, `R9→R16`,
`R10→R8`) has exactly one payload *proposal* line at its recorded end — proposals, not schemas; CML-D2 needs
two proposals per edge to compare. **Lookup, not judgement:** R16 is the missing endpoint on two of the six
(and on five drawn edges in all); `P-R16.md` written; its D1 unlocks CML-D2 for `R16→R2` and `R9→R16`.

**First edge schema, 17:06Z:** `R16→R2` (re-observe) written into `control-map-edges.edn` by the owner from
CML-D2's reconciled proposal (futon2 `031c5f2`): payload `{tick :unspecified-type, mission :unspecified-type,
witness :ExternalWitness}`, the other six `Delivery` fields `:unspecified` because **neither record states
them**; the reconciliation is **one-sided** (P-R2 names the edge, specifies only its outgoing payloads) and says
so. The builder's two refusals stand: no outgoing receipt copied onto an incoming edge; *plausible operational
defaults are not evidence*. **The artefact itself was untracked** (claude-20; not ignored — 73 sibling files
tracked): committed first as the version anchor (p4ng `8f83901`), then the schema as a diff. The two lines after
this: still **specified 0 / unspecified 21** by the linter's rule (a schema with `:unspecified` values is not
specified) — one entry now exists with one typed field, which is the honest description of the wiring's first
specification.

