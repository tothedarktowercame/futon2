# C492 — U22: the epistemic term of mission value, derived and implemented

claude (wm-build-loop), 2026-09-03. Row U22, ported from zaif-harness S5.
Artifacts: `runs/U22-mission-epistemic/` (README + seven EDN files),
`futon2/src/futon2/aif/mission_epistemic_value.clj`,
`futon2/test/futon2/aif/mission_epistemic_value_test.clj`, the U22 block in
`futon2/test/futon2/report/war_machine_test.clj`, and the registry entry
`aif-equations.edn :choices :mission-phase-value`.

No ruling is made here. The registry entry records the form the code now
computes and the fiat it rests on; the default flip is worklist row J7.

## 1. The derivation (DERIVE discipline)

**IF** mission value is to be AIF-native, it has to have the shape Da Costa
eq. 42 gives G: a pragmatic term plus an epistemic one. The three-factor value
in `enrich-candidates-with-mission-value` is pragmatic throughout. `central`
and `strategic` say where a payoff lands; `doable` says how close the mission
is to delivering one, and its table
(`war_machine.clj:2133-2143`) rises monotonically toward INSTANTIATE. Nothing in
it pays for finding out. That is exactly what Joe named on 2026-09-02
(`futon4/holes/mission-lifecycle-wm-alignment.md` §5 q1): the fix is "not a
bigger fiat number for MAP" but "an epistemic term in the mission value", so a
MAP-phase mission "wins exactly when uncertainty about the field is what blocks
everything else", and the hand-authored table becomes "a prior, not the whole
value".

**HOWEVER** the epistemic term of eq. 42 is defined against a
policy-conditioned observation model, and the mission grain has none.
`futon2/src/futon2/aif/epistemic_value.clj:9-13` says so about itself:
"Current posterior spread and gap lookup do not satisfy that contract and must
not be passed off as EIG." And what is actually readable per mission is coarse:
the `code/v05/mission-doc` hyperedge carries `:mission/phase`,
`:mission/mtime` and `:mission/cross-refs`, and nothing that looks like a
belief.

**THEN** the term is built as a Bayes-coherent EIG over a *declared* mission
observation model, and the declaration is kept apart from the measurement:

    epistemic(M) = survey-availability(phase(M))
                   * min(1, EIG(M) / (10 * ln 2))

Each field question is one binary latent read perfectly, so it goes through
`epistemic-value/expected-information-gain` unchanged and its Bayes-coherence
gate applies — a question the judge cannot settle has prior 0.5 and is worth
ln 2 nats, a settled one is worth 0. The questions are: per resolvable
cross-reference, "is that neighbour workable, i.e. at or past DERIVE?", which
the judge cannot answer when the neighbour's phase is unreadable; and, once,
"is my own reading of this mission current?", which it cannot answer when
`:mission/mtime` is more than the declared 14 days old. `survey-availability`
is a second fiat table, one number per phase, MAP = 1.0.

**BECAUSE** this is what makes the MAP preference fall out of a measurement
rather than out of the table. Two MAP-phase missions on the same field get
different numbers: M-web-arxana-missions has two open questions and scores
0.200, M-apm-capability-ratchet has one and scores 0.100 (all eight of its
neighbours' phases are readable, so only its own staleness is open). And a MAP
mission with nothing unread scores 0 and cannot win on its phase at all —
pinned by `a-map-phase-with-nothing-unread-does-not-move` in
`war_machine_test.clj`. The fiat table multiplies measured nats instead of
standing in for them, which is the position Joe's ruling asks for.

The workable partition is not declared twice: it is read off
`phase-doability-prior`, the same table as `war-machine/phase-doability`, at
threshold 0.5 (DERIVE and later), and
`the-doability-prior-is-pinned-across-the-two-namespaces` fails if the two
tables ever differ.

## 2. Declared inputs, and nothing silent

- `:epistemic` is a fourth key in `default-mission-value-weights`
  (`war_machine.clj:2128-2131`) at **0.0**. The four must still sum to 1, and a
  three-key weights map is still accepted with `:epistemic` defaulted to 0.0
  (`mission-value-weights`, `war_machine.clj:2212-2231`), so existing callers
  and the `FUTON_WM_VALUE_WEIGHTS` declarations already in use are unaffected.
- At weight 0.0, `field-readings` returns nil: **no substrate read happens and
  no field is attached to any candidate**. `merge` of nil is identity, so the
  default path is byte-identical. Pinned by
  `the-epistemic-weight-defaults-to-zero-and-reads-nothing`.
- The war_machine diff is **line-neutral** (6661 lines before and after), which
  is deliberate: 98 distinct `war_machine.clj:<line>` pointers in the registries
  sit above the edited region, and inserting lines would have drifted every one
  of them onto different content for no gain. The cost is three lines that carry
  two bindings or run long; it is recorded here rather than as a code comment.

## 3. The step-through (`runs/U22-mission-epistemic/`)

Both rankings come from the shipped selector on the same 133 candidates (ids
pinned from the committed S2 baseline), differing only in the weights map. The
`after` weights keep Joe's option-B ratio among the three exploit factors and
shrink them by `1 - 0.15`, so

    value_after = 0.85 * value_before + 0.15 * epistemic * gates * decay

and that identity holds on **133 of 133** rows. A common positive factor cannot
reorder the exploit part, so every rank move is the epistemic term.

Rank 1 is unchanged in both arms (M-zaif-harness-v1). The headline move:
**M-web-arxana-missions, MAP phase, rank 124 → 74** on two open field questions
worth 1.3863 nats. With a live doability factor (arm C, one declared
substitution) it moves 102 → 87, passing fifteen missions that are closer to
delivering. That is the trade against doability, measured; the exchange rate is
the one declared weight.

## 4. Two findings the row did not go looking for

**(a) 266 cross-references resolve to nothing, and they were buying survey
priority.** Across the 374-hyperedge mission-doc family, 266 cross-references
over 116 missions name no mission the index knows; 39 are numeric (`M-1`
through `M-7` on M-pattern-mining, `M-foo`/`M-bar`/`M-baz` on
M-portfolio-inference, `M-INC`, `M-WS`, `M-EOI`). Some look like a regex scrape
catching list markers; some may be real references to missions with no doc. The
judge cannot tell those apart. The first run of this term treated each as an
open question and put M-pattern-mining at rank 4 on nine open questions, eight
of which were `M-1`..`M-7` and `M-trip-report` — a malformed cross-reference
list buying explore priority. They are now excluded and counted as
`:unresolvable-cross-references` (121 over 48 of the 133 candidates). Whether
the ingest should be producing them is a question for whatever writes
`:mission/cross-refs`, not for the selector.

**(b) The doability factor has been inert on the futon2 path since
2026-07-19, and phase is sitting on the hyperedge it already fetched.**
`compute-delta-t-mission` (`war_machine.clj:2046-2054`) resolves phase through
`futon3c.aif.mission-delta-t/delta-t-mission`, and futon3c is not on futon2's
classpath — `requiring-resolve` returns nil and the fallback `{:delta-T 0.0}`
carries no `:mission-phase`, so `phase-doable` takes the "unknown" 0.3 for
every candidate. This is visible in the records, not just in this process:
`:phase` is nil on every ranked mission row in every trace file from
`wm-trace-2026-07-19.edn` onward, and `:doable` collapses to the single
"unknown" value on the same date. Counting the maps that carry a `:doable` key
in each `data/wm-trace/wm-trace-<date>.edn`:

| file | rows | phase non-nil | `:doable` |
|---|---|---|---|
| 07-17 | 103 | 98 | nine distinct values, 0.0-1.0 |
| 07-18 | 107 | 99 | nine distinct values, 0.0-1.0 |
| 07-19 | 106 | **0** | `{0.3 106}` |
| 07-21 | 109 | **0** | `{0.3 109}` |
| 08-30 | 134 | **0** | `{0.0 4, 0.3 130}` |
| 08-31 | 134 | **0** | `{0.0 4, 0.3 130}` |
| 09-01 | 267 | **0** | `{0.0 8, 0.3 259}` |
| 09-02 | 269 | **0** | `{0.0 8, 0.3 261}` |

So on 09-02 `:doable` is 0.3 on 261 of the 269 rows, the other 8 operator-gated
at 0.0. The discriminating spread on 07-17/07-18 and its disappearance on 07-19
is the finding; the 0.0 rows are the operator gate, not a phase reading.
Meanwhile the string is right there: `:mission/phase "map"` is a prop on the
`code/v05/mission-doc` hyperedge that `mission-doc-index`
(`war_machine.clj:1450-1474`) already fetches and discards, and 313 of the 374
hyperedges carry a readable one. Arm C's substitution reads it and the doability
factor immediately discriminates nine ways.

Where both carriers have a reading they **agree**: 123 agree, 0 disagree
(`07-doability-live.edn :phase-agreement`). So this is a plumbing gap, not a
disagreement about what phase a mission is in. Repairing it is not this row's
acceptance — it would change the default doability of most candidates, which is
exactly the silent default change U22 forbids — so it is written up here and
should become its own row.

**(c) Today's readable uncertainty is nearly all staleness.** Over the 123
candidates the term could measure, the open-question count is
`{0: 6, 1: 108, 2: 8, 3: 1}`: 108 have exactly one open question and it is
their own reading freshness, and only 9 have an unread neighbour, because 313
of 374 phases are readable. The term is implemented and behaves as designed;
on this field it is close to a staleness ordering with a phase mask, and the
moves are correspondingly small. The obvious next question kind is already
written in the docs — the numbered `MAP must answer:` lists, e.g.
`futon0/holes/missions/M-apm-capability-ratchet.md:260-274`, six questions —
and reading those is a follow-on row, not this one.

## 5. Gates

`clj-kondo` 0 errors / 0 warnings on each touched Clojure file individually;
`futon4/dev/check-parens.el` OK on all five; 250 tests / 3769 assertions across
`futon2.aif.epistemic-value-test`,
`futon2.aif.mission-epistemic-value-test`, `futon2.report.war-machine-test`,
`futon2.aif.mission-c-test` and `futon2.aif.full-loop-runner-test`, 0 failures
0 errors (plus the 5 new deftests in the first of those and the 4 in
war-machine-test). `negative_controls.sh` and `pointer_check.bb` run before the
commit; `gen_aif_dag.bb` deliberately NOT regenerated (TN §9a gate rule).

## 6. Review pass (claude, 2026-09-03, before the commit)

The implementation, the registry entry and the step-through were reviewed
against the row's acceptance by re-running the gates and recomputing the
step-through's own claims from the committed EDN rather than reading its
summaries. What was checked, and what it found:

- **`clj-kondo`** 0 errors / 0 warnings over all five touched files;
  **`check-parens`** OK on all five; **tests** 102 tests / 553 assertions,
  0 failures 0 errors over `futon2.aif.mission-epistemic-value-test`,
  `futon2.report.war-machine-test` and `futon2.aif.epistemic-value-test`
  (`clojure -X:test :nses '[...]'`).
- **The war_machine diff is line-neutral**, as §2 claims: 6661 lines at HEAD
  and 6661 after.
- **The decomposition identity was recomputed from `03-before.edn` and
  `04-after.edn`**, not read off `05-rank-moves.edn :value-identity`:
  `value_after = 0.85 * value_before + 0.15 * epistemic * gates * decay` holds
  on 133 of 133 rows to within 1e-9, ranks are a permutation on both sides, and
  the headline move (M-web-arxana-missions 124 -> 74, 1.3863 nats, epistemic
  0.200) reproduces. `epistemic = availability * min(1, nats / (10 ln 2))` was
  recomputed on all 123 `:measured` rows: 0 violations at 1e-12. The
  open-question distribution `{0 6, 1 108, 2 8, 3 1}` and the status counts
  `{:measured 123, :phase-unreadable 8, :mission-absent 2}` reproduce, and no
  MAP-phase candidate with zero open questions carries a non-zero term.
- **§4b's counts were WRONG and are corrected above.** The finding's substance
  holds and is now stated from a re-measurement: phase goes nil at
  `wm-trace-2026-07-19.edn` and stays nil, and `:doable` collapses from a
  nine-value spread to the single "unknown" 0.3 on the same date. But the row
  counts did not reproduce. The claimed "783 of the 807 ranked rows on 09-02
  with the other 24 operator-gated" is exactly 3x the file's contents (261 of
  269, other 8) -- a triple count -- and the nil series "924, 922, 20935 and
  801" matches nothing measurable; `wm-trace-2026-09-02.edn` is the only 09-02
  source and the three `tick-run-record-2026-09-02-*.edn` files carry no
  `:doable` rows at all. The corrected figures are in the table in §4b and in
  the registry's `:adjacent-finding`, with the counting rule named (maps
  carrying a `:doable` key) so the next reader can re-run it.
- **One figure is asserted but not reproducible from the committed artifacts**:
  the field-wide "266 cross-references over 116 missions". The EDN carries the
  candidate-scoped count (121 over 48 of the 133), which does reproduce; the
  field-wide pair appears only in the README prose and would need a fresh
  substrate read to confirm. It is descriptive background and enters no score,
  so it is left standing and flagged here rather than cut.
- **`:J7` did not exist.** The registry entry and §1 both cite worklist row J7
  as the gate on the default flip, and the row was never written -- so the
  acceptance's "default flip J-gated" rested on a dangling pointer. J7 is now
  in `worklist.edn` at `:status :needs-joe`, with the `:bar` argument for why
  the exchange rate is not derivable from sources, code or the 2026-09-02
  ruling, and a note that a live default would currently trade against a
  constant because `doable` is inert.
- **Every cited line pointer was opened.** Exact: `war_machine.clj` 2128
  (`default-mission-value-weights`), 2133 (`phase-doability`), 2212
  (`mission-value-weights`), 2301-2302 and 2326-2329 (the wiring), 1450
  (`mission-doc-index`), 2046 (`compute-delta-t-mission`);
  `epistemic_value.clj:64` (`expected-information-gain`) and its 9-13
  docstring, quoted verbatim; `mission_delta_t.clj:237-240`, which does read
  both prop key shapes off the vertex doc as claimed; and the six-question
  `MAP must answer:` list at `M-apm-capability-ratchet.md:260`. Two registry
  pointers landed inside docstrings and are corrected:
  `mission_epistemic_value.clj:88-118` -> **100-128** (the doability prior
  through `workable-phases`) and `:159-179` -> **186-205**
  (`binary-latent-model`).
- **`negative_controls.sh`** PASS (22 negative, 12 positive; shared registries
  untouched); **`pointer_check.bb`** 1049 pointers in 3 files, 0 unresolved;
  **`worklist_check.bb`** 130 items OK. `gen_aif_dag.bb` deliberately NOT run
  (TN §9a).

The three defects above were fixed in this pass rather than sent back, per the
review-fix rule in the workspace CLAUDE.md. Nothing in the derivation, the form
or the code changed as a result -- the corrections are to three count sets in
one adjacent finding, two line pointers, and a missing ledger row.
