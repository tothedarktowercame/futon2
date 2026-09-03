# The runtime-validation catalog

**Row:** `:U36`. **Date:** 2026-09-03. **Machine-readable artifact:**
`RUNTIME-VALIDATION-CATALOG.edn`. **Validator:**
`holes/labs/wm-contract/runtime_validation_check.bb`. **Raw run log:**
`runs/U36-runtime-validation/test-run-2026-09-03.txt`.

Joe, 2026-09-03: *"start to get a list of the runtime validations that we can
produce with evidence... on the global run, but also on the per-node behavior,
because that is what was really missing last time."* Per-node is the organizing
axis, per Joe. This document is the narrative; the EDN is the data, and every
count below is computed from it by the validator rather than typed here.

```
COUNTS: 84 rows (59 per-node, 25 global-run) | nodes 19/19 | status exists=69 exists-but-stale=4 red=3 named-gap=8 | flips=6 | test namespaces 47/50 green | pointers=175
```

## 1. What was measured, and how

102 test namespaces were invoked one JVM each on 2026-09-03 — 78 under
`futon2/test/futon2/aif`, 20 at the `futon2/test` root, 4 in futon3c
(`clojure -X:test :nses '[<ns>]'` and `clojure -M:test -n <ns>`; futon2 HEAD
`42a957c`, futon3c HEAD `17fde898`). They ran 1209 tests and 10605 assertions.
99 namespaces came back green and **3 came back red**. The full log is the
`runs/U36-runtime-validation/` file above; the 50 namespaces a catalog row
actually cites are carried in the EDN's `:test-runs`, each with its exit code.

The statuses in this catalog were therefore *measured*, not asserted. The
validator enforces the consequence: a row that claims `:evidence-kind
:test-green` must name a namespace present in `:test-runs`, and a namespace
that exited non-zero cannot be typed as coverage — it must be typed `:red`.

**Pointer convention.** Every pointer is repo-relative from `/home/joe/code`
with an optional `:LINE` or `:A-B`, and `runtime_validation_check.bb` resolves
all 169 of them by direct path join. That is deliberately *not*
`pointer_check.bb`'s convention, which matches a bare filename against a
hand-maintained allowlist of directories. This catalog cites five repositories
and two test trees; under the allowlist form, a valid pointer into any
directory nobody had cited before would have reported "file not found", which
is the defect that checker's own header records six times (C472, C473, AC7,
U11, U13, U23).

## 2. Per-node: what validates each R node live

All 19 nodes of the drawn map (`p4ng/empirics-futon/control-stages.edn`) carry
at least one row. The node binding of a WM-side row is recorded, not assumed:
`:self-declared` means the test's own namespace docstring names the node
(R1, R4, R5, R6, R7, R9, R12, R14, R15, R16, R20, TRACE do), `:by-source-namespace`
means the equations registry's `:code` field names the namespace the test
covers, `:by-registry-row` means a row of `aif-equations.edn` or
`checks/witness-registry.edn`, `:by-run-record` a recorded tick, and
`:by-matrix` the zaif-side U10 matrix.

The three kinds of per-node evidence that exist today:

- **Unit suites.** The largest are R6 (`policy-test`, 54/296), R1/R3
  (`belief-test`, 78/2030) and R2's live channel contract
  (`r2-channel-contract-test`, which reads the trace corpus rather than a
  fixture). The thinnest is R9 — 7 tests carrying 10 assertions, for the node
  whose whole content is that a claim is checked by someone other than its
  author.
- **Recorded per-node fixtures.** 40 files over run-id × node from U12's
  falsifier, 30 present and 9 *typed absences*. The absences are evidence:
  `801976e7-R16.edn` says `:no-enactment-yet` rather than being empty.
- **Replays over recorded history.** U4's ambiguity sweep is the template —
  882 archived ticks re-scored with the term dropped, five controls run before
  any measurement was read, byte-identical on two runs.

Where a node's zaif half is absent the row is `:named-gap` with the row that
owns it: R1/R3/R4 real-input tests (U11, D8, S7) and the R13 horizon pin (U13).

## 3. Global run: what validates a whole tick or a whole run

`wmRunsOnce` and `wmRunConformsToWiring` are *permanent external attestations*
in Lean — the annotation says plainly that Lean cannot prove an event, and
names the executable witness instead. Both witnesses were run for this catalog
and **both pass** when handed a record: `wm_runs_once_witness.clj` reports
`selectorSeam=stub:first-ranked-authorized-mission preferenceLayers=5`, and
`wm_route_conformance.clj` on record `0a18c4f7` reports `hops=9 conformant=9
unmapped=0 | original-fired=3/21 measured-fired=6/8`.

Above the tick, RUN3 checks a whole *run* against the drawn topology, selecting
that run's records by `:run/id` equality and classifying a traversed retired
edge by grounds; its last recorded verdict is `:conformant` over
`2026-09-01-s5`. Around both sit the standing gates — 34 publish negative
controls, the registry pointer check, the ledger check, the 43-witness Lean
family, and U35's fresh type-check verdict (68/68 modules, exit 0).

**Determinism has a component-grain answer and no run-grain answer.** The
posterior replays at delta zero from the record itself, with a negative control
that breaks the replay; U4's sweep is byte-identical across runs; R17's offline
reduction replays a recorded input to equality. None of these re-runs a tick.
A whole-tick replay is minted here as a named gap rather than left as the
ambient assumption that one exists.

**One named gap closed since first publication (2026-09-03, U37).**
`:g/enumeration-completeness` asked whether the selector enumerates everything
there is to work on. It now does have an answer for missions: an independent
filesystem scan finds 133 available and the three 2026-09-02 records enumerate
the same 133, membership diff empty both ways, every one of the other 2,076
files under `*/holes/missions` carrying a typed exclusion reason
(`runs/U37-enumeration-completeness/`). Its refusal side is the second new row,
`:g/enumeration-completeness-controls`. The same replay reports what missions
being complete does not cover: excursions (157) and tickets (33) have no
proposer at all, so 190 items sit outside the candidate pool — carried as
`:kind-not-enumerated`, which is a typed absence rather than a gap in testing.

## 4. The flip feed for U32

Six J-gated flips are enumerated with the line that reads the flag, the flags
each depends on, and the nodes the flipped path exercises — derived from the
flag's own read site and the code it turns on, not from intuition:

| flip | read at | exercises |
|---|---|---|
| `FUTON_WM_MISSION_C=1` | `war_machine.clj:85-99` | R2, R4, R5 |
| `FUTON_WM_SELECTION_LAW=full-score-posterior` | `war_machine.clj:207-219` | R5, R6, R8, R14, R16 |
| `FUTON_WM_FPI_POSTERIOR=1` | `war_machine.clj:135-153` | R4, R6, R8 |
| `FUTON_WM_TAU_MODE=variational-beta-gamma` | `war_machine.clj:740-761` | R6, R8, R14 |
| the guide-gate boolean flip | `U14d-consumer-census.md:23-31` | R9 |
| the zaif U14e-flip | `U14-tickle-migration-proposal-v1.md:1-20` | R9 |

Each row's `:gates-flip` names the flips it is a gate line for, and the
validator refuses a `:gates-flip` entry that is not a key of `:flips`, so U32's
checker can consume the product without re-deriving either side. **This row
performs no flip and rules on nothing.**

Read across the two: the selection-law flip touches five nodes, and one of them
— R16 — currently has an *open* obligation about it. `enactedActionEqualsSelected`
is refuted by record (50 comparable records, 50 differ, 0 agree), and its
successor `enactedEqualsSelectedWhenRankOneGated` is an open hole whose
falsifier is a run in which the rank-1 selection passes its gate and a
different action is enacted. The flip changes what "selected" means, so that
hole is the R16 gate line it has to answer.

## 5. Findings

Eight, all code-backed with pointers, none of them a ruling. Rulings go only to
`aif-equations.edn :choices` or `control-map-edges.edn :decisions`, and only by
Joe.

1. **No run can be certified today.** `checks/wm_operational_certificate.clj:11`
   pins `control-map-edges.edn` at `64485bb0…`; the live file is `7fecd071…`.
   The SVG pin still matches, so only the edge-data half moved — which is what
   this campaign's D-item edits to the edge registry did. `:topology-pin-valid?`
   is false, so every certification verdict is `:fail`, including the two test
   cases whose expected verdict is `:incomplete`. 5 of 35 assertions red.
   Re-pinning is a decision about which edge-registry revision the certificate
   authority is; it belongs with the D-item review, not with a catalog row.
2. **Two route denominators under one word.** The receipt's `:route-verdict`
   (`run_tick_once.clj:145-176`) counts hops against the drawn layer alone; the
   conformance check (`wm_route_conformance.clj:26-52`) counts against drawn ∪
   measured. Both write `:conformant` and `:unmapped`. On record `0a18c4f7` the
   receipt says 3 conformant / 6 unmapped and the check says 9 / 0. The Lean
   hole's falsifier is the check's definition, so a reader taking the receipt's
   own verdict as the conformance answer reads a 3-of-9 run where the
   obligation says 9 of 9.
3. **The `wmRunsOnce` witness does not see today.** Its default record
   selection matches `tick-run-record-YYYY-MM-DD.edn`, so the three RUN11-era
   records whose filenames carry a run id are invisible and the bare invocation
   certifies the 2026-08-31 record. It does not fail — it answers from older
   evidence, which is the harder failure to notice.
4. **The absence-coercion gate is red and guarding nothing.**
   `preemptive-repair-lint-test` asserts 7 blocked findings and gets 0: the
   dispositions file now holds 17 rows, none `:blocked` (16 `:fix-now`, 1
   `:exempt-with-reason`). The debt was worked down and the pin was not moved
   with it.
5. **The build-level repair gate is red on one untracked file.**
   `C484-refusal-harvester.md:23` cites `refusal-sweep-state.edn`, which is
   present on disk and untracked in git, so the artefact lint reads it as a
   citation to an untracked path. One `git add`, owned by C484.
6. **One test file is invisible to the runner.**
   `fold_realized_zero_coverage_test.clj` has no `deftest` form — it is a script
   with its own PASS/FAIL printing — so `clojure -X:test` reports "Ran 0 tests"
   and exit 0 for it. Loaded directly it runs and passes. One instance in 20
   root namespaces, and nothing checks for the class.
7. **Nine `:code` ranges in the equations registry land on unrelated code**,
   and `pointer_check.bb` passes every one of them, because it verifies that a
   range sits inside a file and not that the target says what the row claims.
   The registry's own F row records this happening once — it is not once. All
   six `war_machine.clj` pointers now land in the mark2 batch-pipeline scanning
   region of a 6302-line file: the AIF tick code moved and the ranges did not
   follow. Three more are in `src`: `:depth` points at a docstring about
   `:unscored` moves when the horizon default lives at `rollout.clj:474-479`,
   and `:temperature` and `:action` both point at `gap-report` when
   `effective-temperature` is at `policy.clj:77` and `select-action` at
   `policy.clj:672`. The other `src` pointers checked do land on the function
   their row names. Not repaired here: the registry is signed, and correcting
   it is a registry edit under the §9a supersession discipline — a row of its
   own. What would close the class is modest: let a `:code` field carry the
   defining form's *name* beside the range, and let the check assert that the
   name appears within it.
8. **U10's narrative is behind its own matrix.** `U10-node-coverage.md`'s Gaps
   section still says R7 has no test and R8/R14 are partial and plant-derived,
   while `U10-node-coverage.edn` now types R7, R8 and R14 as `:covered` with
   recorded record-ids. The later U19z/U21z rows updated the matrix and not the
   prose; the md's two addenda do not cover it. The same staleness sits on R9,
   which U10 records as missing and U14 packet (a) landed the same evening.
   U10's own *line numbers* into the futon3c test files have drifted too — its
   R5 citations at 93 and 121 now land on R8 material — which is the same class
   as finding 7 one directory over.

## 6. What a reviewer should check

- Re-run `bb runtime_validation_check.bb` — it resolves all 169 pointers,
  closes the vocabulary, and refuses if the COUNTS line above drifts from the
  data.
- Re-run `bash runtime_validation_controls.sh`, which is the reason to believe
  the line above means anything: five planted defects that must be refused *by
  reason*, and two positive controls so that a checker which refused everything
  could not pass as one that refuses the defect.
- Spot-check the three red rows by re-running their namespaces; the failure
  text quoted in each `:note` should reproduce verbatim.
- Re-run the two Lean witnesses with an explicit record argument and confirm
  the pass lines quoted in §3.
- Judge the honesty of the `:provenance` field on the per-node rows: `:planted`
  and `:mixed` rows are coverage of a mechanism, not of recorded behaviour, and
  the catalog should not be read as claiming otherwise.
