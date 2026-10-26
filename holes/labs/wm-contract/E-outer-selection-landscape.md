# E-outer-selection-landscape — how the outer loop chooses, and what it can see

**Opened:** 2026-10-26 (Joe's question put on 2026-10-25, relayed by claude-11)
**Owner:** claude-9 (research question, record, conclusions). Implementation may go to zai-2.
**Status:** OPEN. Capture C1–C3 landed (C3 reviewed 2026-10-26); C4 split into C4a (projection, landed `bc7ad6c93`, reviewed 2026-10-26) and C4b (retain wiring, in flight); H2 mechanism answered in §10.2; findings F1–F6 recorded (F3 corrected in §10.2); hypotheses H1–H3 open, H4 answered (no, by hand-read; §5); §10 answers Joe's 2026-10-26 questions.
**Related:** `futon3c/holes/missions/M-giraffe.md` (`357edb090`, one work item as data, with dependencies);
`STOCKTAKE-wm-2026-10-25.md`; `PROPOSAL-outer-focus-class-2026-10-21.md`;
`mathlib4/DarkTower/WarMachine/Proof2/OuterSelectionCanonicalG.md` (O2/O3 notes);
`GOverCascades.lean:67` (`cascadeGrainG`).

## 1. The question

Joe (2026-10-25, via claude-11): how does the War Machine's outer loop actually
choose what to work on, and does it know enough about the state of the mission
landscape to choose well? Some missions are good in principle but not right now,
because what they depend on does not exist yet. The outer loop's G was meant to
be based on Cascade Live for that reason. Joe has not seen how the canonical G
works in practice. The next run should capture the outer loop's cascade and a
trace of its process, so that it can be studied.

Two sub-questions, kept apart:

- **Q1 (mechanism).** What is the sequence of stages between "every M/E/T/A
  document on disk" and "one target", and what does each stage drop?
- **Q2 (knowledge).** At the stage where G scores, what does G see about a
  mission, and what about the landscape (dependencies, readiness, what has
  been built) is invisible to it?

## 2. Source read for this record

The latest click's retained receipt, read directly with a one-form EDN parse
(not through meta-latest):

    futon2/data/wm-full-loop-machinery-150/wm-contract-machinery-150-v1/attempt-002/002-selection.edn
    key [:payload :judgment :outer-task-selection]   schema :wm/outer-task-selection-v1
    selection-as-of 2026-10-24T20:40:24Z   receipt size 15.7 MB (the key alone)

Code read: `outer_canonical_selection.clj` (whole), `outer_canonical_g.clj`
(whole), `cascade_coapply_g.clj:47-97` (`score-one`), `family_selection.clj:
380-436` (`sealed-pre-choice-menu`), `meta_live_outer_selector.clj:670-900`
(`select-live`), `target_reading_registry.clj:150-200`,
`target_reading_preparation.clj`, `full_loop_runtime.clj:476-495`,
`futon3c/src/futon3c/wm/meta_latest.clj:168-180`.

## 3. Q1: the funnel, as the 2026-10-24 receipt records it

Every number below is a count over the receipt. Stage names are the code's own.

| Stage | Where | In | Out | Dropped, by typed reason |
|---|---|---|---|---|
| S0 registry field | `select-live` tasks | 786 | 786 | |
| S1 standing + map + ownership | `select-live` 690-760 | 786 | 109 | `:pipeline/not-on-current-map` 526; `:repair-finding/root-missing` 102; `:repair-finding/attached-to-root` 25; `:repair-finding/root-not-current` 13; `:ownership/ambiguous-active-owner` 6; `:ownership/actively-held` 3; `:mission-standing-conflict` 1; `:task-document-source-unavailable` 1 |
| S2 target-policy field (readings → families) | `full-eligible-target-field` | 109 | 7 computed | `:no-current-target-reading` 101; `:occurrence-squares-not-authored` 1 |
| S3 policies per family | same | 7 families | 26 policies | (6 families × 4, 1 family × 2) |
| S4 canonical binding → sealed menu | `sealed-pre-choice-menu` + `bind-ranked` | 26 | 4 entries | `:occurrence-identity-absent` 9 refusals (three further policies each of M-stack-geometry, M-war-machine-wiring, M-recommendation-bindings); 10 policies of M-formal-war-machine, M-digital-nomad-patterns, M-pattern-posteriors unbound, their menu failure rows `:target-class-unknown` 10, `:seed-not-in-graph` 2, `:isolated-seed` 2; 3 policies of M-self-improvement-loop neither bound nor refused, with no per-policy record (menu failure rows carry a target id only; that target's rows are `:frontier-too-wide-for-exact-enumeration` 3, `:seed-not-in-graph` 2); the remaining failure rows belong to bound targets and did not block the menu: `:void-pattern-citation` 8, `:isolated-seed` 4 |
| S5 canonical G frontier | `outer-canonical-g/produce` | 4 | 4 scored | refused 0; horizon 10 = min(limit 10, max unit count 27) |
| S6 selection | `outer-canonical-selection/evaluate` | 4 | 1 | min G; two equal minima (M-war-machine-wiring, M-recommendation-bindings, both G = −3.9560226735640867); uniform draw u = 0.2253 → M-war-machine-wiring |

Correction (2026-10-26 00:15Z, from the C1a review): the S4 row first named
`:embedding-node-not-retained` 10. That keyword occurs in the receipt 88 times,
all under the sealed menu's `:target-relations` as a relation reason, never as
a failure or refusal kind. The counts above are the menu's `:refusals` and
`:failures` kinds, which is what the C1a trace reads. S4's arithmetic does not
close from the receipt alone: 26 in, 4 bound, 9 refused, 10 unbound by target,
and 3 policies with no record at all. The trace lists those 3 under
`:policies-without-binding-record`.

Corrections to what was passed to me as established:

- "Of 109 eligible items, G scored 4; why the other 105 got no score is not
  confirmed." Confirmed now, and it is not one cause: 101 had never been read
  (no target reading at the current excerpt digest; `found-digests []` for every
  one, so not stale but never read), 1 had no authored occurrence squares, 3
  were read and had policies but every policy lost its embedding node at
  binding (S4).
- "The full cascade per candidate is dropped; only the digest survives." Not so
  for the receipt. All four scored cascades are retained whole, with structure
  and the four-map squares per node, at
  `[:outer-policy-recomputation :q4 <i> :canonical-cascade]` (45–135 KB each).
  The digest match was checked for all four. What is true is that
  `data/wm-executable-cascades/` (gitignored, 27 files) is a separate store; it
  does hold all four of these digests today, including `c601dc56…`.
- The sealed menu is also retained whole, at
  `[:policy :selected-outer-cascade-policy :sealed-menu]` (4 entries, 9
  refusals, 142 failures). The top-level `[:policy :sealed-menu]` is only a
  pointer to it (`:status :carried-on-carrier`).

So the receipt already holds everything a stage trace needs. What is missing is
the trace itself (one place that says S0→S6 with counts and reasons) and the
per-candidate G terms (S5 keeps status, ids, digest, G, rank only; `score-one`
had computed risk, ambiguity, and novelty per step and per candidate under
`:certificate`, and `outer_canonical_g.clj:88-97` drops them).

## 4. Q2: what G sees, and what the machine has read

**F1. G sees a cascade, not a mission.** The score is `cascadeGrainG`
(`scoreCascadeCoN`): risk plus ambiguity of the cascade's predicted
observations over H steps under the class-emission model, minus the expected
parameter novelty of the patterns it co-applies. The mission enters only through
the reading that produced the cascade (S2) and the class model (S4). Nothing in
S5 or S6 reads the mission document, its wants, its status, or any other
mission.

**F2. No readiness or dependency term exists.** A blocked item is removed at
S1 by ownership or by a typed repair-finding reason, or never reaches S2.
"Good, but not now" has no representation anywhere in S0–S6. This is the
M-giraffe point, and this record is one of its sources.

**F3. The machine has read 8 of its 109 eligible items.** A target reading is
an analysis of the mission's excerpt, stored under
`/home/joe/code/storage/wm-target-readings/targets/<target-sha>/<excerpt-sha>.edn`.
There are 9 reading records on disk, all written since 2026-10-01. The runner's
pre-choice preparation (`full_loop_runtime.clj:486-491`) reads at most 3 new
targets per click, in field order, under a 30-minute deadline. So the field G
can score grows by at most three missions per click, and which three is
decided by document order, not by anything G or the operator chose.

**F4. The choice set is therefore "the missions already read".** On
2026-10-24 that was 7 read, 4 bindable. A selection over 4 items drawn from
109 by reading order is not yet a selection over the landscape.

**F5. Two different cascades scored exactly the same G** (27 units vs 12 units,
equal to 16 digits). With the terms dropped this cannot be explained from the
receipt. See H1.

**F6. The old objective is still computed beside the canonical one**
(`:policy-objective`, `gamma·G − lnE`); it does not choose. O3-2 made canonical
selection active with no objective fallback (`select-live` 865-875).

## 5. Hypotheses, to be answered by the next run's capture

- **H1.** The equal G in F5 arises because, under the class-kernel C with a
  uniform-over-equal class, risk and ambiguity at a shared horizon depend on the
  class model and H and not on the cascade's units, so distinct cascades of
  equal class collapse to one score, and novelty alone separates them. If so,
  G is insensitive to most of what a cascade contains. Test: C2 terms for the
  two tied rows; compare risk, ambiguity, novelty term by term.
- **H2.** Reading order (F3) is the real selector of the landscape. Test: list
  the next 3 needs the preparation plan would take at HEAD
  (`target-reading-preparation/plan`) and check whether they are the first three
  unread in registry order.
- **H3.** The embedding-node loss at S4 (10 policies, 3 missions) is a store
  staleness, not a property of the missions. Test: `focus_receipt.clj:252,389`
  reason production; check whether the embedding store holds those nodes now.
- **H4.** A dependency term would have changed the 2026-10-24 choice. Test
  after C1–C3: of the 4 scored, which have unbuilt dependencies named in their
  mission text? (Hand-read; feeds M-giraffe's field list.)
  **Answered, probably not (claude-11, hand-read, 2026-10-26; table in
  `futon3c/holes/missions/M-giraffe.md`, commit `29e72624a`).** The tied pair
  (M-war-machine-wiring, M-recommendation-bindings) each name one unmet need
  that can be checked now and one that cannot (a ruling on WM-I4; a dangling
  mission name), so a count of unmet needs leaves the tie where G left it.
  What the read adds to the field: needs come in five kinds (an artifact or
  field exists; another item's status; a ruling; a dangling name; an
  operator practice) and only the first two are machine-checkable, so the
  rest need a typed cannot-determine reason; the edge must say which side
  owns the need, since only M-stack-geometry separates its own Missing
  column from its dependencies in its text; and "enables" edges are needed
  beside "needs" (M-recommendation-bindings names M-portfolio-inference as
  something it enables). Limit: for E-live-means-live only the Status line
  was read, not whether the coverage gap persists. Closed here; the field
  design continues in M-giraffe.

## 6. Capture to land before the next run (C1–C4)

Joe's invariant: the machine stays runnable, and the capture must not change
what is selected. The design principle that guarantees the second: **every
capture is either a pure projection of data the receipt already holds, or an
extra key on a row that `evaluate` never reads.** `evaluate`
(`outer_canonical_selection.clj:11-50`) reads `:status`, `:G`, `:policy-id`,
`:cascade-digest`, `:action-pin` and the menu pin; nothing else.

- **C1 `:selection-trace`** (one new namespace, `futon2.aif.outer-selection-trace`,
  plus one retain-time call). A pure function receipt → trace: the S0–S6 table
  of §3 as data, each stage with `:in`, `:out`, `:dropped` as a map reason →
  count, and for S4 and S6 the ids involved. Being a projection, it can be run
  today over attempt-002 and must reproduce §3 exactly; that is its test pin
  (live-pin rule: values verbatim from the retained receipt). The runner retains
  it at retain time (`full_loop_runner` `retain-outer-task-selection`), after
  the selection is final. Placement (decided at the C1a review, 2026-10-26):
  beside the receipt, not inside it, at `[<arm> :outer-selection-trace]` on the
  selection cell, where `<arm>` is `:judgment` or `:sorry` as for the receipt.
  Reason: the receipt's schema `:wm/outer-task-selection-v1` is checked by
  `selection_world.clj` and `construction_receipt_lean_adapter.clj`; the trace
  has its own schema `:wm/outer-selection-trace-v1`, and keeping it beside the
  receipt leaves every receipt consumer reading exactly what it read before.
- **C2 per-row G terms.** In `outer_canonical_g.clj` results, keep on each
  `:scored` row a `:terms` key: `(get-in r [:certificate :g-terms])` and
  `(get-in r [:certificate :parameter-novelty])` with the per-step `:tau`,
  `:g`, `:expected-information-gain`. Refused rows unchanged. Size: a few KB per
  row. Correction (2026-10-26, packet premise check): the per-step `:tau`, `:g`,
  `:expected-information-gain` live at `[:certificate :steps]`
  (`cascade_coapply_g.clj:82-85`), not under `:parameter-novelty`, whose own
  `:steps` carry `:tau :before :expected-novelty`. The packet keeps `:steps`
  projected to the three keys and `:parameter-novelty` without its `:steps`.
  `evaluate` returns the whole selected row under `:selected`, so that map will
  carry `:terms` too; the selection (status, action, policy-id, draw) does not change. The frontier pin is a digest of the body and is minted per run, so
  adding the key changes no cross-run comparison; a test pins that
  `evaluate` over a frontier with and without `:terms` returns the same
  selection for the same draw.
- **C3 cascade locator.** On each frontier row, `:cascade-at
  [:outer-policy-recomputation :q4 <i>]` when the q4 carrier holds a cascade
  with the same digest, else `{:absent :cascade-not-retained}`. This is a
  reader-side addition in `meta-latest/g-frontier`, not a producer change,
  since the producer already retains the cascade.
  Correction (2026-10-26, packet premise check before dispatch): the reader
  is `futon3c.wm.meta-latest/g-frontier` (`futon3c/src/futon3c/wm/meta_latest.clj:168`),
  not a futon2 namespace; the cascade sits on the q4 rows at
  `[:outer-policy-recomputation :q4 i :canonical-cascade]`, whose `:digest`
  equals the row's `:cascade-digest` (checked on attempt-002: 4 rows, 4
  scored entries, every digest matched). The locator is the index path into
  the same receipt map.
- **C4 reading-preparation outcome.** Added 2026-10-26 from §10.2. Not "retain
  the `prepare!` result": on every reading-era click `prepare!` was never
  called (mechanism in §10.2), so retaining its result would retain nothing
  again. Instead a typed outcome on every click. C4a: a pure projection
  `futon2.aif.reading-preparation-outcome/outcome` of (judgement, the
  `:record` that `selected-target-reading-step` returned) with schema
  `:wm/reading-preparation-outcome-v1` and `:status` one of `:not-run`
  (reason `:no-selected-need` or `:reading-fn-absent`), `:ran` (the
  `prepare!` reason, preparation status, attempted/published/pending
  targets), `:selected-target-reading` (the per-target request), or
  `:unrecognised`; always with `:chosen` and `:policy {:status :reason}`
  copied from the receipt. Live pin on attempt-002: `:not-run`,
  `:no-selected-need`, chosen `M-war-machine-wiring`. Landed as futon2
  `bc7ad6c93` (zai-2), reviewed below. C4b, after C4a's
  review: a 4-arity `retain-outer-task-selection` taking the record, call
  site `full_loop_runner.clj:7766` passing `selected-target-reading-record`,
  the outcome retained at `[<arm> :reading-preparation]` beside the trace.
  `evaluate` reads neither key. Landed as futon2 `a625a08a8` (zai-2),
  reviewed in §9. Status: C1–C4 all landed (C1 `f12700102`+`5637a787d`,
  `fa18ace07`; C2 `725a7a70f`; C3 futon3c `970690d5e`; C4 `bc7ad6c93`,
  `a625a08a8`). The capture of §6 is complete; nothing in it is loaded into
  a running JVM except `meta-latest` (C3), so a click on the futon2 side
  needs a reload from master first.

Not in scope before the run: any dependency or readiness term (that is H4 and
M-giraffe), any change to reading order or the batch limit of 3, any change to
the tie law.

## 7. Runnability

24 selector tests were reported failing at HEAD before any of this
(`meta-live-outer-assessed-selector-test` 23, `…-ordinary-certificate-test` 1;
zai-1's finding). Rerun at HEAD on 2026-10-26 as bounded test
`bounded-1792972922560-outer-selector-tests-head` (claude-9): 6 tests, 72
assertions, 24 failures, 0 errors. All 23 failures in the first namespace are
one test, `assessed-objective-is-activated-only-through-its-verified-carrier`
(`meta_live_outer_assessed_selector_test.clj:67` onward), which expects
`[:policy :status] :selected` and a `:partial` policy objective and gets
`:refused` with no objective: it pins the O3-1 assessed-basis activation, and
O3-2 removed the objective fallback (`select-live` 865-875, "canonical refusal
is terminal"). The 1 failure in the second namespace,
`frozen-trial-five-replay-and-the-exact-seal-gap` (`:104`), carries its own
comment "frozen capture is not pin-recomputable".

**Decision:** neither bears on runnability. Both pin behaviour the live route no
longer has. Disposition is a follow-up, not a pre-run block: retire the
assessed-activation test or rewrite it to pin the O3-2 refusal, and refreeze or
drop the trial-five capture. Decision rule used: a failing test bears on
runnability only if it pins the ordinary selected route (`select-live` with a
sealed menu and a canonical selection).

## 8. Coordination

- codex-11's fix 1 (`368079c98`, `full_loop_runner.clj` + a test) and zai-1's
  fix 3a (`03fdb29ee`, `meta_live_outer_selector.clj` + its test) have both
  landed on futon2 master. C1's retain-time call touches `full_loop_runner.clj`
  after fix 1; packets rebase on HEAD and commit explicit paths only.
- C1–C3 are three packets, one file/behaviour each, in order C1, C2, C3, each
  reviewed before the next is dispatched.

## 10. Additions of 2026-10-26 (Joe, via claude-11): Lean at selection, receipts history, precedence

Joe treats three findings as stop-the-line, each on its own
(`futon3c/holes/missions/M-giraffe.md`, `720ee5ae0`): unreachable eligible
missions (F3/F4), unselectable tickets, and G without a dependency input (F2).
Three questions were put to this record. Answers from this turn's reads; every
count is from the files named.

### 10.1 Which Lean statement should rule out "eligible but never read"

**The statement exists, and the runtime hands it nothing to check.**

- `IncrementalTaskFrontier.lean:126` `SelectionCertificate.Valid frontier openTasks`
  requires `taskSet frontier = openTasks` and the selected entry to be a strict
  key-minimum against every other frontier member. `Requirements.lean:362`
  instantiates it with `openTasks = r.frontierCensus.eligible`, and
  `Requirements.lean:274-276` require one outer action per eligible task. Under
  these, a frontier of 4 rows over 109 eligible is invalid. `EligibilityCensus.Valid`
  (`IncrementalTaskFrontier.lean:184`) separately requires every open task to be
  eligible or excluded with a typed reason (reason `0` refused).
- What the runtime binds to that frontier: the receipt's `:q8-frontier`
  (`q8_frontier_certificate.clj`, `q8_frontier.clj:143` `outer-frontier`), whose
  ranking law is `:meta-pipeline/complete-union-channel-sum-v1`, the pairwise
  ranking over all eligible tasks of the selector that O3-2 replaced.
- Since the canonical carrier activates, `meta_live_outer_selector.clj:1045-1052`
  writes `:q8-frontier` as `{:status :certified :carrier (:recomputation
  activated-carrier)}`: no schema, no census, no entries, no selected.
  `q8-frontier/certificate` over that stub, run in the live JVM over the
  attempt-002 receipt this turn, returns `:status :not-recomputable`, reasons
  `[:outer-frontier-absent-or-tampered]`. The attempt-002 checkpoints and
  `retained/` hold no Q8 certificate at all.
- `Proof2/OuterSelectionCanonicalG.lean` states the per-menu law only (minimum
  G, equal minima in menu order, the tie draw, assessed pairs). Nothing in it or
  in `GOverCascades.lean` relates the menu to the eligible census.

So the 101 are, at the Lean layer, neither ranked nor excluded with a reason:
exactly the silence `EligibilityCensus.Valid` is written to refuse, and it is not
reached. The canonical G frontier is bound to no Lean frontier statement.

**What would catch it.** Bind the canonical frontier to Q8 instead of the stub:
an `outer-frontier` whose entries are the S5 rows and whose census lists the S2
`:no-current-target-reading` 101, the S2 `:occurrence-squares-not-authored` 1,
and the S4 refusals and unbound targets as typed exclusions. Then
`Requirements.lean:362` is decidable on this run: it passes only if every one of
the 101 is carried as an exclusion with its reason, and Joe's rule (a field most
of whose eligible items can never reach G is the whitelist in another form) is
then a judgement over a recorded reason count rather than over an absence. A
stricter statement, if Joe wants Lean itself to refuse: an exclusion reason that
is a reading-availability reason, not a task-standing reason, is not admissible
at the frontier census. That statement does not exist today; the census admits
any non-zero reason id.

### 10.2 Selection receipts across runs

Source: every `002-selection.edn` under `futon2/data` (293 files, 3.4 GB; index
at `~/notes/wm-next/selection-receipts-index.txt`), projected by
`~/notes/wm-next/scripts-sel-history.clj` into
`~/notes/wm-next/selection-receipts-history.tsv` (one row per receipt: arm,
support and excluded counts, read ids with their positions in support order,
unread count, scored ids, policy status, chosen id and its position). Two files
were not parsed, machinery-107 attempt-001 (2.4 GB) and machinery-108
attempt-001 (164 MB).

Three eras of selection receipt:

| Era | Receipts | Span | Selector | Scored | Read set |
|---|---|---|---|---|---|
| wm-full-loop / machinery 47-66 | 86 + 14 | 2026-07-14 to 09-20 | controller over 4 actions (`:ranked-candidates`, `:selected-mission ":C1"`) | no target field | none |
| machinery 100-138 (`:meta-pipeline`) | 41 | 2026-10-03 to 10-19 | pairwise `:complete-union` over all eligible (109 tasks, 5886 pairs on m130) | all eligible | none required |
| machinery 142-150 (`:readings`) | 15 | 2026-10-19 to 10-24 | readings gate S2, then canonical G from m149 | 4 (m149-150 only) | 6-7, constant |

Over the 15 reading-era receipts:

- distinct targets ever read: 7 (M-digital-nomad-patterns, M-formal-war-machine,
  M-pattern-posteriors, M-recommendation-bindings, M-self-improvement-loop,
  M-stack-geometry, M-war-machine-wiring);
- distinct ever scored by canonical G: 4 (the sealed-menu entries of §3);
- distinct ever chosen: 7, equal to the read set;
- read count per receipt: 6, 7, 7, 7, 6, 6, 7, 7, 7, 7, 7, 7, 7, 7, 7. It never grew.
- positions of the read set in support order (m150): 6, 12, 54, 58, 70, 88, 101.

Over the 41 meta-pipeline receipts: 34 distinct chosen, including 4 repair
tickets (m101-102, support 35) and `A-self-heal` (m137-138); chosen positions
spread over 6 to 104 of the field.

**Correction to F3.** The read set is not the first few in field order. It is
the set of targets the meta-pipeline selector chose on 2026-10-16 to 10-18:
the 9 reading records on disk (`storage/wm-target-readings/targets/`) were
written 2026-10-16 03:58 to 10-18 22:38, each the selected-target reading of
that era's chosen target (m117-002 M-vsatarcs-writer, m124-002 M-stack-geometry,
m125-001 M-pattern-posteriors, m125-002 M-custom-harness, m127-001
M-recommendation-bindings, m130-001 M-formal-war-machine, m130-002
M-digital-nomad-patterns, m131-001 M-self-improvement-loop, then
M-war-machine-wiring). No reading has been written since 10-18 22:38, across
15 reading-era clicks. The 3-per-click preparation
(`full_loop_runtime.clj:486-491`, `reading-preparation/prepare!`, limit
`(min 3 (max 1 (count needs)))`, 30-minute deadline) has added no target in
this era, and its outcome is retained in no checkpoint of attempt-002 (001-007,
`retained/`, `evidence/` checked). Whether it ran and refused, or did not run,
the receipt cannot say. Measured, then: the field G can score is frozen at what
the previous selector happened to choose in its last three days, and the 101
have been unreachable on every click since the readings gate went in.
Two readings on disk (M-vsatarcs-writer, M-custom-harness) are not in the
read set; whether they left the eligible 109 at S1 or went stale was not checked.

**H2 is answered for the history and reopened for the mechanism.** Reading
order did not select the landscape; nothing did, because nothing read. The next
run's capture needs one more item (C4): retain the `prepare!` result (its
`:reason`, one of `:batch-complete`, `:batch-limit`, `:deadline`, or a refusal,
and the needs it took) beside the selection trace.

**Mechanism (read 2026-10-26, before the C4 packet).** The receipt cannot
say because nothing ran, and nothing ran by construction:

- `full_loop_runner.clj` `selected-target-reading-step` calls the reading
  function only when `selected-target-reading/selected-need` is non-nil, that
  is when the chosen target (or the sole family) has a
  `:no-current-target-reading` / `:stale-target-reading` failure. Otherwise it
  returns `:record nil`, and the runner's `cond->` at `:7756` retains a
  `:selected-target-reading-request` only for a non-nil record. So "not
  needed" and "never retained" are the same absence on the checkpoint.
- Inside that function (`full_loop_runtime.clj:476-492`) the 3-per-click
  `prepare!` batch over the pre-choice field runs only when
  `prechoice-field` is non-nil, which needs `[:policy :reason]` in
  `#{:incomplete-policy-menu-coverage :no-canonically-bound-policy}` and no
  chosen target. Otherwise the function prepares the chosen target's own
  reading and nothing else.
- Over the 18 reading-era receipts (machinery-142..150, both attempts),
  `[:policy :reason]` is nil on the 15 selected and
  `:learning-authority-unavailable` on the one refused (m142-002); none
  carries either reading-record schema. The chosen target always comes from
  the read set, so it never has a reading need, so the function is never
  called, so `prepare!` never runs. The 101 unread targets can only get a
  reading on a click where the outer selection refuses for menu coverage,
  and with 7 readable targets it never does.

So H2's mechanism: the readings gate (S2) and the reading preparation are
mutually exclusive by the gating condition. Preparation is the path for an
empty menu; the menu is never empty. Whether that is the intended design or
an accident of the 10-19 readings-gate change is a question for Joe (the
answer decides whether C4 is followed by a producer change). C4 as now
specified (§6) retains a typed `:not-run :no-selected-need` on every such
click, so the next receipt says this itself.

### 10.3 Precedence: built from what each step needs, or from document order

**From needs.** `construction.clj:125` `containment-order` derives the descent
edge `[A B]` exactly when A produces a token that B's guard needs; this is the
mechanism of `cascade-construction/order-by-what-each-step-needs` ("take
precedence from those dependencies"). Document order enters only as the
candidate's `:precedence` list, which is checked against the derived descent;
edges the list reverses are recorded as `:precedence-violations`, not refused.

**What canonical G reads.** `cascade_coapply_g.clj:31-45` (`candidate!`)
accepts only `{:co-apply {:units :descent :patterns}}`, refuses a cyclic
descent, and reads the descent in the novelty term
(`expected-novelty` over `model/enabled-frontier units descent patterns`) and,
through the co-application kernel, in risk and ambiguity. The list-kernel
fallback that would score a violating `:precedence` (`efe.clj:1006`
`order-use`) is not on the canonical path.

**Answer to "can G score two cascades equally when one has its steps in an
impossible order".** Not in the canonical G: it never sees an order other than
the one the steps' needs dictate, and a cyclic one is refused. What it cannot
see is a need satisfied by nothing inside the cascade: a step whose guard needs
a token no unit produces and q0 does not hold. That is F2 and M-giraffe's third
finding, restated at the cascade grain: the dependency G lacks is on things
outside the cascade, not on the order within it. The H1 tie (27 vs 12 units,
equal G to 16 digits) is therefore not an order blindness; C2's retained terms
will say whether it is the class-model collapse H1 proposes. Constructing the
case was not cheap: the scoring inputs (observation model, pattern counts,
state) are not in the retained receipt in the form `compare-at-horizon` takes.
With C2 landed the cheap version is: compare the per-step novelty of the two
tied rows, since novelty is the one term that reads the descent.

### 10.4 Not duplicated

codex-11's migration of the 61 rotted close/brief fixtures
(`~/notes/wm-next/close-test-triage/report.md`) is dispatched by claude-11 and
touches tests only; nothing in C1-C3 or C4 overlaps it.

## 9. Log

- 2026-10-26 00:04Z claude-9: record opened; §3–§5 from the attempt-002
  receipt and the code listed in §2. Test rerun launched. C1 packet next.
- 2026-10-26 00:06Z claude-9: selector tests rerun at HEAD, §7 decision recorded.
  C1a dispatched to zai-2: job `invoke-1792973141191-3983-7b83c696`, park
  `park-57f2ee01-3c56-4372-a837-16df58912ef7` (deadline 60 min). C1b (retain-time
  wiring), C2 and C3 wait on its review.
- 2026-10-26 00:15Z claude-9: C1a reviewed and accepted. zai-2's commit
  `f12700102` (5 files, reader only; selector, runner and canonical-G files
  untouched). Checked: (1) `trace` is a pure function of the receipt map, no
  IO, total over absent paths (typed absences); (2) the fixture reproduces
  byte-for-byte from `extract.clj` over the attempt-002 receipt
  (sha256 of receipt `d02726f2…`, of fixture `09a51e10…`); (3) the pin matches
  §3 at every stage, with the S4 correction above. Bad case constructed:
  renaming the refusal kind `:occurrence-identity-absent` in the fixture makes
  the one test fail with the renamed kind visible in the diff. Review fix,
  carve-out (b), `5637a787d`: S4 now lists the 3 M-self-improvement-loop
  policies with no binding record; test, clj-kondo and check-parens green.
  Two differences from the packet's expectations, both the file's: the S4
  reason names (above) and the horizon, which the receipt keeps at
  `[:policy :canonical-g-frontier :horizon-choice :horizon]` while the
  receipt-level `[:horizon-choice :horizon]` is nil. S5 keeps no `:refused`
  rows; the trace records `{:absent :refused-rows-not-retained}`. C1b next.
- 2026-10-26 00:24Z claude-9: C1b reviewed and accepted. zai-2's commit `fa18ace07`
  (runner `retain-outer-task-selection` + the require; new ns
  `outer-selection-trace-retain-test`). Checked: (1) receipt stored unchanged at
  `[arm :outer-task-selection]`, trace beside it at `[arm :outer-selection-trace]`,
  arm chosen as before; (2) the runner diff is those two hunks only, no selector
  file touched; (3) the test pins the selected arm (identical receipt, trace equal
  to `trace`, existing key kept, no sorry arm), the sorry arm, and the absent
  receipt with typed absences at every stage. First run of a new test, so rerun
  once: 1 test, 39 assertions, 0 failures. Bad cases run here: `(retain … true nil)`
  (the live call site at runner `:7766` passes `(:outer-task-selection judgement)`,
  which can be nil) does not throw and gives a trace of typed absences; the
  `:status :absent` receipt on the sorry arm likewise. Observation, not changed:
  the trace for a nil receipt and for an `:absent` receipt are the same map; the
  trace carries no receipt-status field, so the receipt beside it is what says
  which. Discovery item read: the only closed key-set checks are
  `receipt_construction.clj:213-224` (record envelope and the construction sorry
  payload) and neither reaches the selection arm; `full_loop_cohort.clj:82` is
  open-world (missing keys only). No consumer refuses the new key. C2 packet
  premises checked against `cascade_coapply_g.clj:73-94` and `evaluate`; §6
  corrected (per-step terms at `[:certificate :steps]`). C2 dispatched to zai-2:
  job `invoke-1792974125398-4005-03399257`, park
  `park-6421b795-af87-4356-b60b-b1b123ba5823` (deadline 60 min), packet
  `~/notes/wm-next/packets/c2-outer-canonical-g-terms.md`. C3 waits on its review.
- 2026-10-26 claude-9: §10 added on claude-11's bell (Joe's three questions).
  Lean: the frontier statement exists (`Requirements.lean:362` via
  `IncrementalTaskFrontier.SelectionCertificate.Valid`) and is handed a stub;
  `q8-frontier/certificate` over attempt-002 returns `:not-recomputable`
  `[:outer-frontier-absent-or-tampered]` (live JVM). History: 293 receipts
  indexed, 58 with `:outer-task-selection` projected to
  `~/notes/wm-next/selection-receipts-history.tsv`; reading era 15 receipts,
  read set 7 and never growing, scored 4, chosen 7 = read set; no reading
  written since 2026-10-18 22:38. F3 corrected (read set = previous selector's
  choices, not field order). Precedence: needs-derived (`containment-order`);
  canonical G sees no other order. C4 (retain `prepare!` result) added to the
  capture list.
- 2026-10-26 claude-9: C2 reviewed and accepted (futon2 `725a7a70f`, zai-2).
  Record check: one commit, two files, shas match. Adequacy: the test pins
  the three parts per scored row, step count = horizon, exact key sets, the
  stack-geometry row verbatim (`:G` -2.004267726446009, `:total` 5N), sum
  of step `:g` = `:G`, refused rows without `:terms`, and four-draw
  invariance of `evaluate` with and without `:terms`. Gap found and fixed in
  review: the typed-absence path had no test; added
  `nil-certificate-parts-are-typed-absences` (5 tests, 131 assertions,
  green). No warrant registered for either run; dirty tree, not a block.
  C3 dispatched to zai-2 (`invoke-1792975064828-4026-26cfc9b7`, park
  `park-37949f42-f55f-473d-a525-dd900355184f`), packet at
  `~/notes/wm-next/C3/C3-cascade-locator-packet.md`; §6 C3 corrected.
- 2026-10-26 00:45Z claude-9: C3 reviewed and accepted (futon3c `970690d5e`,
  zai-2, two files). Record check: shas match, gates reported (kondo 0/0,
  parens OK, 6 tests / 38 assertions green). Adequacy: the four
  `:cascade-at` index paths are pinned on a fixture whose q4 order, digests,
  ranks and horizon I re-read from the attempt-002 receipt this turn (G
  values rounded to 3 dp and policy-ids synthetic in the fixture; digests,
  targets, ranks, horizon verbatim); round-trip `get-in` asserted; the three
  bad cases (orphan digest, no `:outer-policy-recomputation`, one-character
  digest change) each produce the typed absence; pre-change summary equal
  after `dissoc :cascade-at`. No warrant registered; dirty tree, not a
  block. `futon3c.wm.meta-latest` reloaded from master in the :6768 JVM
  (`cascade-at` bound; sample frontier gives `[:outer-policy-recomputation
  :q4 0]`). Then the C4 premise read above (§10.2 mechanism): C4 respecified
  in §6 and split. C4a dispatched to zai-2: job
  `invoke-1792975449233-4029-e42fd9ca`, park
  `park-1411a0c6-9f39-42dd-b259-2b93938531dd` (deadline 45 min, read back
  at 44.6), packet `~/notes/wm-next/C4/C4a-reading-preparation-outcome-packet.md`.
  Packet note: the sent text asks the agent to check whether the live
  `:policy` map holds `:reason`; it does, with value nil (checked after
  send), so the case-1 pin is `:policy {:status :selected :reason nil}`.
- 2026-10-26 00:48Z claude-9 (entry first written as 00:55Z, ahead of the clock; the commit is 00:48:37Z): C4a reviewed and accepted (futon2 `bc7ad6c93`,
  zai-2, two new files, +188). Record check: shas match, gates reported
  (kondo 0/0, parens OK, 1 test / 34 assertions green). Adequacy: case 1 is
  pinned as a whole map with the live `:policy {:status :selected :reason
  nil}` (the receipt carries `:reason` with value nil; the packet's own
  pin said so after the correction); case 2 is non-vacuous (the test runs
  `selected-need` and asserts the need before the outcome); case 3 asserts
  the exact key set, so `:records`/`:frontier` cannot leak; both bad cases
  are constructed and caught (`:records` nil gives `:ran` with `[]`;
  `:published true` on the request schema stays case 4); `(outcome nil
  nil)` is pinned. No warrant registered; dirty tree, not a block. Packet
  premise correction, found by zai-2 and confirmed in
  `selected_target_reading.clj:223-250` this turn: `selected-need` reads
  families from `[:decision :selection-certificate :target-policy-field
  :families]` and source rows from `[:cascade-problems :target-sources]`;
  the packet's `[:outer-task-selection :policy-field]` is `prechoice-field`'s
  input (line 209), a different function. With the packet's shape case 2
  would have been vacuous; the test builds the real paths. Runner
  observation for C4b: a non-nil reading record is already retained at
  `[:judgment :selected-target-reading-request]` (`full_loop_runner.clj:7756`),
  whole, `:frontier` and `:records` included; a nil record is retained
  nowhere, and `retained-selected-target-reading` (line ~1155) then reads
  it as `{:status :not-applicable :reason :selected-target-reading-not-required}`,
  the same answer whether the reading fn was absent or no need existed.
  C4b adds the typed key beside that reader and leaves it unchanged
  (exempted, not changed). C4b dispatched to zai-2: job
  `invoke-1792975687531-4030-0c0f0b1e`, park
  `park-634eabc8-07e9-44f6-b2c6-b1c829f42383` (deadline 45 min, read back
  at 45.0), packet `~/notes/wm-next/C4/C4b-reading-preparation-retain-packet.md`.
- 2026-10-26 00:50Z claude-9: C4b reviewed and accepted (futon2 `a625a08a8`,
  zai-2; runner +24/−8 in three hunks, new ns
  `reading-preparation-retain-test`). Record check: shas match, the diff is
  the require, the arity split and the call site only; the
  `:selected-target-reading-request` clause and the other `cond->` clauses
  are untouched; the 3-arity body is the C1b body verbatim. Gates reported:
  kondo on the runner 0 errors / 4 warnings before and after (the same
  four), test file 0/0, parens OK, 3 tests / 92 assertions green including
  C1b's retain test. First run of a new test, so rerun once here: 1 test,
  19 assertions, 0 failures. Adequacy: the selected arm pins the outcome as a
  whole map (`:not-run`, `:no-selected-need`) and the non-vacuity check runs
  `selected-need` on the judgement and asserts nil before the outcome is
  used; the sorry arm carries all three keys with `:judgment` untouched; the
  3-arity leaves no `:reading-preparation` on either arm; a hand-built
  `:wm/target-reading-preparation-v1` record with `:records` and `:frontier`
  is retained as `:ran` with neither key, which is the size bound C4 is for;
  the nil-outcome bad case does not throw. One thing the test does not pin:
  that the call site passes the fourth argument. That is read from the diff
  (hunk 3) and smoke-covered by `full-loop-runner-1-4-wiring-test` (4 tests
  / 19 assertions, reported green); a live record with the key waits on the
  next click. Note on the 4-arity: its parameter is named
  `reading-preparation-outcome`, the same as the namespace alias; the body
  uses only the local, and the alias resolves as a qualified symbol
  elsewhere, so nothing is shadowed in effect. No warrant registered; dirty
  tree, not a block. **C1–C4 complete.** Next: the capability narrative for
  Joe and the click budget grant.

- 2026-10-26T00:53Z (claude-9) Serving reload for the capture. Drift probe
  (`~/notes/b4-reload/drift.clj`, read-only) reported 109 registered
  namespaces, two differing from disk: `futon2.aif.full-loop-runner`
  (captured 2026-10-25T23:55Z, pre-C1b/C4b) and
  `futon2.aif.outer-canonical-g` (captured 2026-10-23, pre-C2). No other
  job was running in the JVM. Both were reloaded from master with
  `require :reload`; the re-probe reports 110 registered and no drift, the
  extra registration being `futon2.aif.reading-preparation-outcome` (C4a),
  pulled in by the runner. Outputs retained at
  `~/notes/wm-next/outer-selection/drift-2026-10-26{,-after-reload}.edn`.
  The serving runner and canonical-G producer are now the C1–C4 code; the
  next click needs only the budget grant and the Lean prediction.

- 2026-10-26T03:00Z (claude-9) Q8a dispatched: the §10.1 fix, approved by
  Joe via claude-11. Packet
  `~/notes/wm-next/outer-selection/Q8a-canonical-frontier-packet.md`, zai-2,
  job `invoke-1792983612964-4051-84eb31d0` (running at 03:00:13Z), park
  `park-50fab59e-8c5f-4469-b6b4-6a0b1e20117d` (deadline read back at 44.7
  min). One behaviour: on the canonical-G route the receipt's `:q8-frontier`
  becomes a sealed `:wm/q8-outer-frontier-v1` carrier computed from the
  receipt alone (new `q8-frontier/canonical-outer-frontier`), entries the S5
  scored rows one per task with `comparison-key` over `{:G g}`, census the
  S1 population and its `:ineligible-reason` exclusions, and a new
  `:unranked` list carrying the S2/S4/S5 drops with their typed reasons. The
  design choice, stated to claude-11: S1 reasons are task-standing and shrink
  `eligible`; reading-availability and binding reasons do not, so on
  attempt-002 the carrier is a `:decided-failure` with
  `:census-verdict :frontier-not-eligible-census` (4 ranked, 105 unranked by
  reason) beside the existing `:tied-minimum` verdict, and an eligible task
  with neither an entry nor a typed reason is refused
  `:eligible-task-unaccounted`. `certificate` passes `:census-verdict`
  through when the outer carrier has one; old records recompute unchanged.
  Baselines taken this turn in a fresh JVM: the retained attempt-002 run
  record (`data/wm-runs/tick-run-record-2026-10-24-25e24e8b…edn`) stores
  `:q8 {:status :not-recomputable :reasons [:outer-frontier-absent-or-tampered]}`,
  `verify-record` and a fresh `certificate` agree; its Q6 predecessor is
  `:present` with no `:frontier`, and 0 of the 102 closed events in
  `data/wm-runs/run-stream.edn` carry a `:q8-frontier`, so the first run
  after Q8a will still report `:no-prior-frontier` and the second will have
  a prior. Two further gaps found for the "recomputable" bar, each its own
  packet after Q8a: the canonical-route decision has no
  `[:selection-certificate :policy-stages]` or `:q6`, so `cascade` returns
  `:policy-stages-absent` (Q8b: the selected task's stages from the carrier's
  `:recomputation :q4` and the menu); and `:q2-outer-actions` is the same
  stub (Q2 on this route, `Requirements.lean:274-276`, one action per
  eligible task).
- 2026-10-26 claude-9 (03:08Z): Joe, on the §10.2 reading-era finding: the
  machine is to do useful operator-aligned work; there is no point running it
  while it is known defective or injured, or while it cannot admit and make
  sense of the body of historical work slated but not done (here the 101
  eligible tasks never read). Course set: no clicks; assemble an
  observational defect list, build the correct Lean model and the Clojure
  repairs, then run. The §10.2 mechanism question ("intended design or
  accident of the 10-19 packets") went to codex-11, who ran machinery-142..150:
  packet `~/notes/wm-next/outer-selection/Q-codex-11-reading-era.md`
  (observation, mechanism check, intent, its own defect lines with evidence,
  the two off-set readings), job `invoke-1792984118310-4057-f05f52d8`, park
  `park-f51ce8ea-0595-4f84-b43f-a3494d37c20d`. Context given to it: the gate
  was built 10-19 by codex-1 (`69307114c`, `fc7265a81`) under ruling
  `3bc41ccc1`, whose body says "selection produces readings on demand; a gate
  is not finished without its producer", and the live path never runs the
  producer. The defect list starts at
  `~/notes/wm-next/outer-selection/DEFECTS-reading-era.md` on its reply.
- 2026-10-26 claude-9 (03:24Z): codex-11 replied (job
  `invoke-1792984118310-4057-f05f52d8`). Its four code citations and the
  m150-002 receipt claims verified by reading: `full_loop_runner.clj:2385`
  gates on `selected-need`; `selected_target_reading.clj:201` admits
  pre-choice preparation only for the two menu-coverage reasons;
  `full_loop_runtime.clj:485` runs the batch only on that branch, comment at
  492 "builds the first menu"; ruling `3bc41ccc1` item 4 says selection
  produces readings on demand. codex-11 finds no warrant for empty-menu-only
  preparation: an incomplete composition, not a policy. m150-002 parsed:
  109 support, 677 excluded; failures 101 `:no-current-target-reading`,
  1 `:occurrence-squares-not-authored` (`M-vsatarcs-writer`, in support),
  and 8 read families of which only `M-pattern-posteriors` has no failure;
  the chosen `M-war-machine-wiring` carries `:isolated-seed`.
  `M-custom-harness` is excluded at S1 for ambiguous owner (codex-6, codex-8).
  m142-002 `[:payload :sorry :outer-task-selection :policy]` is
  `:refused :learning-authority-unavailable`, a reason outside the
  preparation trigger set. The observational defect list is started:
  `~/notes/wm-next/outer-selection/DEFECTS-reading-era.md`, 11 defect lines
  (R1 producer never runs vs the ruling; R2 preparation absence untyped;
  R3 Q8 stub, in flight on zai-2; R4 policy stages absent; R5 Q2 stub;
  R6 learning-authority refusal; R7 prediction input at admission; R8
  measurement-domain mismatch; R9 map-as-callable resume; R10 stale serving
  composition, kept as a check; R11 parent/child requirements gap) and two
  unclassified observations. Each line names the Lean clause that should
  catch it and the repair. No clicks until the list is closed, the Lean
  clauses exist, and the repairs land.
- 2026-10-26 claude-9 (03:36Z): Q8a reviewed and accepted. zai-2 landed
  `f764d36a4` (job `invoke-1792983612964-4051-84eb31d0`): on the canonical-G
  route the receipt's `:q8-frontier` is now a sealed
  `:wm/q8-outer-frontier-v1` carrier (`q8_frontier.clj`
  `canonical-outer-frontier`); its census is the S1 open population with
  typed exclusions, its entries the S5 scored rows one per task, and every
  eligible task without an entry is listed `:unranked` with stage and typed
  reason, else the carrier refuses `:eligible-task-unaccounted`. Record
  check: diff matches the report. Test adequacy: the stub bad case, the
  unaccounted-task refusal (two constructions), the non-vacuous census pass,
  the seal, and the old-route certificate are each pinned; the m150-002
  fixture is a projection that re-extracts byte-identical from the retained
  receipt (checked). One run of the new namespace: 7 tests, 44 assertions,
  green. On m150-002 the verdict is `:decided-failure`, census
  `:frontier-not-eligible-census` 4 ranked / 105 unranked, selection
  `:tied-minimum`. zai-2 reports that `outer_canonical_selection_test` and
  `q8-frontier-route-test` are nondeterministic at HEAD before its change
  (line 101 digest equality fails 3/4 isolated runs); recorded as U3 on the
  defect list, needs its own discovery packet. Defect list R3 moves to
  REPAIRED-FORWARD. Q8b (policy stages, R4) and Q2 (R5) remain open.
- 2026-10-26 claude-9 (04:02Z): claude-5's Lean pass reviewed and accepted.
  Five modules on mathlib4 darktower, one commit each: SelectionPreparation
  `8683505654` (R1, R2), IncrementalTaskFrontier additions `bf7fa3f6a4`
  (R3, R12, R4, U1 conjecture), SeamRefusal `feb1c7cf35` (R6, R9),
  AdmissionInputs `1c1045c81d` (R7, R8), ClickRecordBinding `bed6efedba`
  (R10, R11). Record check: each lake warrant's code-file sha256 equals the
  committed file's, exit 0, no errors, no sorries. Witness mode holds on
  every clause: the observed receipt is a control that provably fails a
  forward predicate; nothing cites Clojure as the definition. The m150
  encoding was checked against `attempt-002-projection.edn` by EDN read:
  109 support ids with the eight read tasks at sorted positions 31, 40, 65,
  71, 73, 79, 93, 98; 101 unmet families; ranked menu failure kinds
  {seed-not-in-graph, isolated-seed, void-pattern-citation,
  frontier-too-wide-for-exact-enumeration, occurrence-identity-absent};
  `:target-class-unknown` on the three S4-unranked only (4 + 4 + 2 rows).
  Verdicts, shas and terms are on each line of
  `~/notes/wm-next/outer-selection/DEFECTS-reading-era.md`. Two changes to
  the list from the review: R5 is not a defect (claude-5 stopped it: the
  clause contradicted the ruled Q2, `Requirements.lean:257-266`, codex-68
  option (c); the residual question, whether the task-level outer action
  relates to the retained menu's action pin, goes on Joe's next sheet); R2
  was already repaired forward by C4a `bc7ad6c93` and C4b `a625a08a8`, which
  the list had as "written, not dispatched". One amendment asked of
  claude-5: R1's deferral conjunct counts only attempted rows against the
  budget, so a batch with a refusal in it could never be valid; a refusal
  spends a slot (`prepare!` today stops the whole batch at the first
  refusal, which the clause rightly refuses). Dispatched 04:02Z, each
  parked: claude-5 the R1 amendment and the R13 clause
  (`invoke-1792987323665-4086-b92c5664`); codex-9 the R12 Clojure repair,
  headline `:reason` as the set difference against the ranked kinds,
  `:unexplained` when empty, `:ranked-kinds` on the census verdict, fixture
  re-pinned (`invoke-1792987328646-4087-b09b22b7`); codex-10 the R6
  pass-through, `provide` carrying the belief's `:reason` and `:details`
  with `:belief-status`, no default reason for an absent one
  (`invoke-1792987332764-4088-2648f36a`). Packets under
  `~/notes/wm-next/outer-selection/packets/`. R13 and R1 Clojure repairs
  follow their clauses; R14 waits on its semantic choice. zai-3's U3 report
  still running. Times in two earlier entries of today (stamped 04:00Z and
  04:10Z) were written ahead of the clock and are corrected in place to
  their commit times.
- 2026-10-26 claude-9 (03:37Z; first stamped 04:00Z, ahead of the clock; commit time is authoritative): defect list closed for the Lean pass
  (`~/notes/wm-next/outer-selection/DEFECTS-reading-era.md`). R6: the cause
  was retained all along in m142-002's learning belief details
  (`ClassCastException`, "Default comparator requires nil, Number, or
  Comparable: #{}", loading `resources/wm/outer-learning-authority.edn`);
  `outer_policy_provider_runtime.clj:73` rebuilds the refusal without them
  and collapses every non-produced belief reason to
  `:learning-authority-unavailable`; `load-shadow` at HEAD is `:produced`.
  R9 repaired by `ae9e71584`. R8: only the Lean admission bridge
  (`mathlib4 44466e785`) landed; no runtime change since the m150 clicks.
  U1 classified: reading graph failures are family-level annotations that
  `family-coverage-kind` ignores; ranked targets carry the same kinds. That
  yields R12: Q8a's unranked `:reason` (first sorted menu-failure kind)
  names `:seed-not-in-graph` / `:isolated-seed` for two S4-unranked targets
  while ranked targets carry the same kinds; the discriminating kind is
  `:target-class-unknown` (all three S4-unranked targets, none ranked). The
  menu's `:failures` concatenates reading annotations, scoring failures and
  binding refusals with no origin tag (`family_selection.clj:411-429`).
  Repair is a small follow-on to Q8a. claude-11's overview field (789 items,
  10-25 18:21Z) reconciles with m150-002 (786) on every shared item.
- 2026-10-26 claude-9 (03:41Z; first stamped 04:10Z, ahead of the clock): claude-11's reconciliation returned. The
  one count they could not match, `:ownership/actively-held` 2 against 3,
  resolves by an EDN read of `attempt-002-projection.edn`: M-G-wm-wiring,
  E-implementing-G-over-cascades, M-diagramprover (their regex missed the
  third). The ticket only in their field,
  `T-repair-occ-c8da1b18…`, is absent from m150-002 under any reason. U3
  discovery is running twice: codex-14 for claude-11 (Joe-approved,
  `invoke-1792985530725-4069-bffd0c6c`, report to
  `~/notes/wm-next/selection-test-flake/report.md`) and zai-3 for claude-9
  (`invoke-1792985982530-4076-b7757030`, dispatched seven minutes later,
  before the first was known); both read-only, both left to finish.
- 2026-10-26 claude-9 (03:44Z): Q8a loaded into the serving JVM. Reloaded
  `futon2.aif.q8-frontier` and `futon2.aif.meta-live-outer-selector` from
  the master checkout (03:43Z); `drift.clj` then reports no drift over 110
  registered namespaces. Ran `canonical-outer-frontier` and `certificate`
  in that JVM over the retained `attempt-002/002-selection.edn` itself,
  not the projection: open 786, eligible 109, excluded 677, 4 entries
  (recommendation-bindings, self-improvement-loop, stack-geometry,
  war-machine-wiring), 105 unranked, selected M-war-machine-wiring,
  carrier `:decided-failure`, selection `:tied-minimum`, census
  `:frontier-not-eligible-census` with the same by-reason counts the
  fixture gives, carrier intact; certificate `:not-recomputable` with
  reasons `[:no-prior-frontier :policy-stages-absent]`, census verdict
  carried through, intact. The fixture and the receipt agree. The park
  wake for the Q8a job arrived at 03:42Z, after the inline review above.
- 2026-10-26 claude-9 (03:47Z): claude-11's second reconciliation pass
  found M-diagramprover under `:ownership/ambiguous-active-owner` in the
  10-25 18:21Z preview and under `:ownership/actively-held` in m150-002,
  which leaves the ambiguous count one short on their side. EDN read of
  both receipts settles it: the ambiguous item that m150-002 has and the
  preview does not is `M-futonzero-prelim-practice`. In the preview it is
  in `support` (eligible), not excluded under any reason. Its mission file
  (`futon0/holes/missions/`, last commit 1d85d02 on 2026-09-30) did not
  change between the two receipts (m150-002 recorded 2026-10-24T20:51Z),
  so the reading moved with live ownership state, not with the document.
  Both ownership lines are now exact: actively-held 3 vs 2
  (M-diagramprover), ambiguous 6 vs 6 (M-futonzero-prelim-practice out,
  M-diagramprover in).
- 2026-10-26 claude-9 (04:05Z): zai-3's U3 report returned
  (`~/notes/wm-next/outer-selection/U3-nondeterminism.md`, runs at futon2
  `d7850885d`) and was reviewed against codex-14's. Record check: the
  note exists at the stated path, the twelve logs and the exit-code
  summary under `/tmp/u3-logs/` match its counts (canon 4 red of 6, q8 6
  of 6), the futon2 tracked tree is clean, and every cited line reads as
  described at the current HEAD (`rand` at `outer_canonical_selection.clj:39`,
  the drawless `evaluate` call at `meta_live_outer_selector.clj:849`, the
  config gate at 828-830 and the refusal at 903-904, the q8 fixture's
  `outer-receipt` at `q8_frontier_route_test.clj:78-83`, the failing
  assertions at test lines 101 and 154). No warrant: nothing was changed.
  Judgment: the two seats, each unaware of the other's result, name the
  same two causes, the unseeded tie draw and the config-less Q8 fixture.
  zai-3 adds the pin-the-draw evidence (the explicit-draw deftests were
  green 6/6 while only the drawless one flipped) and the isolated refusal
  output for the Q8 fixture; codex-14 adds the fresh-process order runs
  that zai-3 skipped for budget. U3 is settled and stays split into R13
  and R14. zai-3's fix alternative, deriving the draw from the frontier
  pin, is recorded under R13 and not taken. One packet slip of mine: the
  dispatch gave the q8 test path without its `wm/` directory.
- 2026-10-26 claude-9 (04:09Z): codex-9's R12 repair returned (futon2
  `5c4e8b2df`, "Make outer frontier reasons discriminating") and was
  reviewed. Record check: the diff touches only `q8_frontier.clj` and
  `wm/q8_canonical_frontier_test.clj`; trailers name codex-9 under my
  dispatch. Adequacy: the carrier now computes `ranked-kinds` from the
  menu failure and refusal rows of targets with menu entries, and an
  unranked task's headline is the first sorted kind outside that set, or
  `:unexplained`; the census verdict carries `:ranked-kinds`. On the
  fixture the set is the five kinds the Lean clause took as its
  `rankedKinds` parameter, so the Clojure and the Lean agree on the input
  as well as the output. The re-pins match `m150_repaired_discriminating`
  (both headlines `:target-class-unknown`, histogram 101 / 1 / 3). The
  test derives the ranked kinds from the fixture's rows rather than typing
  them, and the bad case is constructed: an unranked task carrying only a
  shared kind headlines `:unexplained`, and gains its own kind as headline
  when one is added. Two observations, neither needing a change: the
  test's refusal-row target reads `:target-id` before the policy-id
  prefix while the source reads the prefix only (equal on the fixture,
  asserted); and the S2 and S5 branches take the same headline rule,
  which the clause's wording covers. No warrant; spot-run at 04:07Z, 8
  tests, 49 assertions, green. R12 is REPAIRED-FORWARD in the defect
  list. Packet slip of mine, the second tonight of the same kind: the
  packet named `futon2.aif.q8-frontier-test`, which does not exist; the
  q8 tests live under `wm/`.
- 2026-10-26 claude-9 (04:14Z): codex-10's R6 repair returned (futon2
  `e74d11ebf`, "outer policy: carry learning belief refusal") and was
  reviewed. Record check: two files, trailers name codex-10 under my
  dispatch, sha matches the job's artifact. Adequacy: the test constructs
  the m142-002 belief verbatim and pins `provide` carrying reason and
  details, the schema-invalid reason not renamed, and a reasonless belief
  typed `:learning-belief-not-produced`. That pins the seam the packet
  named, but not the Lean's consumer: `SeamRefusal.lean` wrapper 3 is the
  `:kind` the selector adds, so `m142_repaired_carried` is about the
  receipt's `[:policy]` entry, two seams further on. codex-10 flagged the
  first of them (`resolve-provider` rebuilds the refusal from the reason);
  the second is the selector building `[:policy]` from `:status` and
  `:reason` alone. The reason now carried end to end; the cause stopped at
  `provide`. Fixed directly, not re-belled (carve-out b): both seams carry
  `:details` when the defect is the producer's own refusal, and a test in
  `meta_live_outer_selector_test` runs the m142 shape through
  `select-live` and reads `[:policy :details]`; it fails with the seams
  unfixed (one failure at the details assertion) and passes fixed. futon2
  `6dc72816b`. No warrant from codex-10; its count (5 tests, 29
  assertions) matched my run of the runtime namespace. Packet slip of
  mine: the packet scoped the repair to `provide` while the Lean target
  was the receipt; the first reader check should have gone one seam
  further. Also found: `outer_policy_field_boundary_test` has been red
  since `aaa85f9b4` (`:selection-as-of-required`), unrelated to tonight.
- 2026-10-26 claude-9 (04:20Z): claude-5's clause job returned (mathlib4
  darktower `88f6cb59f4` SelectionPreparation amendment, `26beba373a`
  SelectionDraw). Record check: each sha touches one module; the commits
  carry `Agent-Session` matching the job's session but no `Agent-Id` or
  `Agency-Job` trailer (mathlib4 may lack the hook; noted, not acted on).
  R1 amendment accepted: the budget conjuncts count refusals as spent
  slots, the one existing theorem that becomes false under the amendment
  gained the hypothesis that makes it true again, and the three controls
  bracket the budget and refuse today's stop-at-first-refusal shape by
  totality. R13 accepted: validity is `expectedOutput inputs = some
  output`, so a tie with no sealed draw has no valid output and the only
  conforming behaviour is a typed refusal; the controls are the
  attempt-002 rows with the receipt's `:u` sealed as input, selecting what
  the click selected. I checked the entry order against attempt-001's
  frontier (same four rows, same tie) and the floor arithmetic against
  `evaluate`'s `(int (* u (count tied)))`. Warrants are claude-5's
  record (lake exit 0, 2946 jobs); I did not find a registry lookup
  route for the ids and did not rebuild. R13 Clojure packet dispatched to
  codex-14 with one finding of my own first: the click script already
  seals a per-run seed (`outer-task-seed`, war_machine.clj:6648) and
  passes it to the selector, so the entropy input exists in the record
  and is dropped at `selector` (line 1122) and ignored by `select-live`.
  The repair derives the draw from that seed; the runner needs no change.
  An addendum bell carries the wrapper fix, since the packet's checkpoint
  would otherwise stop the job on it. Parks `park-aa3f4f39` and
  `park-807ebff3`, 45 min each.
- 2026-10-26 claude-9 (04:33Z): codex-9's R1 part (a) returned (futon2
  `ff789d5e7`, job `invoke-1792988536265-4122-7a2d54a5`). Record check:
  five files, the four named plus the p37 runtime test whose pins moved
  with the batch semantics; trailers codex-9 / Dispatched-By claude-9.
  Accepted: `prepare!` no longer stops at the first refusal; a refused need
  leaves `:pending`, every attempt spends a slot, `:reason` is only
  batch-complete / batch-limit / deadline, status is pending while needs
  remain, else refused when any row refused, else prepared; the outcome
  projection gains `:refused`. The tests build the amended clause's
  controls (one throw among publishes at limit 3: three rows, 768 pending,
  the `m150TwoAttemptedOneRefused` shape; all-refusing at limit 3: three
  refused rows; a completed batch with one refusal: refused /
  batch-complete). No warrant; I ran the three namespaces once as the
  first run of new tests: the two preparation namespaces green, p37 red
  with five assertions whose cause is `:selection-as-of-required` from the
  selector. That red is at HEAD since `aaa85f9b4` (10-22), after p37's
  last edit before tonight; codex-9 laid it at codex-14's dirty selector
  files, which the reason string rules out. codex-14's R13 addendum
  stopped on the same red in `outer_policy_runtime_selection_test` (15
  failures in a clean worktree at `ff789d5e7` without its change) and
  asked for a fourth-file licence or a waiver: waived as pre-existing,
  commit the three files, bell `invoke-1792989109914-4132-7eb140f6`, park
  `park-d7ce48de`. One stale-helper defect behind both reds (`run-live`
  and the p37 fixture omit `:selection-as-of`); it gets its own small
  packet after R1b, not a line on the defect list. R1 part (b) dispatched
  to codex-9 from a read of the three gates (runner 2385 and 7295, runtime
  485 via `prechoice-field`, all on `selected-need`, which is nil under a
  readable winner): a `preparation-needs` / `preparation-needed?` pair in
  `selected_target_reading.clj`, the two runner gates swapped to it, the
  runtime branch running the batch over the receipt's `:policy-field` with
  the chosen target's need first, a checkpoint if the retained-selection
  path needs more than the predicate swap, and a new namespace under
  `test/futon2/aif/wm/` that pins the bad case (readable winner, four
  unmet families: the step at HEAD never calls the fn). Premises checked
  before dispatch: `:policy-field` is on every ordinary receipt
  (`meta_live_outer_selector.clj:1039`); `retained-outer-selector`
  (`full_loop_runtime.clj:378`) keeps the winner across the redecide;
  `repair!` is `repair-need!` on the selected need. Job
  `invoke-1792989152008-4137-2285ef7d`, park `park-c503c4a7`, deadline
  05:17Z. Packets under `~/notes/wm-next/outer-selection/packets/`
  (`R1b-batch-fires-with-a-winner-packet.md`, `R13-gate-waiver-bell.md`).
- 2026-10-26 claude-9 (04:36Z): codex-14's R13 repair returned (futon2
  `726d38432`, waiver job `invoke-1792989109914-4132-7eb140f6`; the
  first job's wake arrived after the commit had landed, so the review ran
  on this wake). Record check: the three named files, trailers codex-14 /
  Dispatched-By claude-9, no warrant, kondo clean. Adequacy: `evaluate`
  has no `rand`; nil draw on a tie refuses `{:absent :no-sealed-draw}`,
  1.0 refuses `{:invalid 1.0}`; `select-live` seals `:seed` as
  `[:policy :selection-draw]` via `java.util.Random` and `selector`
  forwards it. The tests are the Lean controls on the m150 menu
  (`m150_repaired_valid`, `m150Other`, `m150UnitDraw`,
  `m150_untied_needs_no_draw`) plus a seeded `run-live` whose chosen
  action matches the carrier's entry and an unseeded one that refuses.
  Production path read: `war_machine.clj:1551` already passes `:seed`,
  and the runtime wrappers pass the input through, so the wrapper line
  was the whole gap. Carrier replay unchanged (recorded `:u` at
  `selected_outer_cascade_policy.clj:190`). First run of new tests: 8
  tests, 40 assertions, green. R13 REPAIRED-FORWARD in DEFECTS. The waived
  red (`run-live` helper omits `:selection-as-of`) is the stale-helper
  packet, queued after R1b.
- 2026-10-26 claude-9 (04:42Z): codex-9's R1 part (b) reviewed on the
  work-order nudge, before its park wake (futon2 `b06aa11d5`, job
  `invoke-1792989152008-4137-2285ef7d`). Record check: the four files the
  packet named (the two runner gates, the runtime branch, the predicate
  pair, and the new namespace under `test/futon2/aif/wm/`), trailers
  codex-9 / Dispatched-By claude-9, no warrant, kondo 0 errors (the four
  runner warnings predate the commit), parens OK. Adequacy: the new tests
  build the bad case the defect names (readable winner, four unmet
  families) and it is caught at the predicate and at the step; the
  unmet-winner order is pinned; the no-field case is pinned empty. The
  runtime branch was not covered by the packet's (iii), so I drove the
  real `:selected-target-reading-fn` from `production-defaults` with
  absent source files and an author port that throws if contacted: an
  unmet winner in a field of five yields rows T-win, T-1, T-2 (all
  `:selected-target-reading-unpinned`, refused at the `.isFile` guard
  before any shell-out), pending T-3, T-4; a readable winner yields T-1,
  T-2, T-3 with T-4 pending; no field falls back to the exact `repair!`
  path; no field and a readable winner yields nil. Ordering: the chosen
  family is moved to the front of `:families` before `prepare!`, which
  plans in family order. Two things noted and left: the chosen target's
  batch row now takes its path from the field's `:target-sources` rather
  than the outer receipt's `:source` (both built from the same task
  declarations, `family_selection.clj:214`), so the exact path's
  `:source-pin-conflict?` guard no longer applies to it; and the
  selection phase no longer evaluates `selection-terminal-condition`
  while preparation is needed, which only matters with the restart
  debugger attached, and nothing attaches it except the operator's
  `runs/one-click-frontier-2026-10-22/attach.clj`. R1 is
  REPAIRED-FORWARD in DEFECTS with both shas. First run of the new
  namespace: 2 tests, 15 assertions, green.
- 2026-10-26 claude-9 (04:44Z): stale-helper packet dispatched to codex-14
  (job `invoke-1792989843531-4152-91760a0d`, park `park-163db998`, 45 min):
  `run-live` (`outer_policy_runtime_selection_test.clj:37`) and p37's
  `zero-assessed-frontier` fixture supply `:selection-as-of` and the
  focus trio the selector has required since `aaa85f9b4`; selector
  untouched, no pin moved, checkpoint on any red with another reason.
  Packet `~/notes/wm-next/outer-selection/packets/stale-helper-selection-as-of-packet.md`;
  the R1b runtime spot-check script is beside it
  (`r1b-runtime-spot-check.clj`). Next after its return: R14's semantic
  choice.
- 2026-10-26 claude-9 (04:56Z): codex-14's stale-helper packet (job
  `invoke-1792989843531-4152-91760a0d`) stopped at its checkpoint as the packet
  asked: the helper keys took the reds from 15 to 9 and 5 to 3, kondo and
  parens clean, nothing committed. The remaining reason, read before deciding:
  `:invalid-canonical-tie-draw` with `:selection-draw {:absent :no-seed}`. I
  drove the test file's own `run-live` (probe
  `~/notes/wm-next/outer-selection/packets/stale-helper-reason-probe.clj`):
  the fixture's two policies tie on canonical G to 16 digits in every case, the
  provider's lnE no longer separates them (O3-2, `218f9f2ba`), and with a seed
  every case selects. So the reds were not the helper but claims that predate
  O3-2 and R13; the namespace was red from `aaa85f9b4` to today and never
  pinned either change. Finished under carve-out (b): futon2 `6bc93912a`, two
  test files, trailers claude-9 with codex-14's helper edits inside it and
  named in the message. Per test: the three that want a selection pass
  `seed` (7) in their own input, since `outer-canonical-selection-test` pins
  that the unseeded base map refuses; `unique-minimum-...` renamed
  `seeded-canonical-choice-...` because the fixture has no unique canonical
  minimum; the receipt-action assertion reads `:id` (the key O3-2 chose and
  its test pins); p37's bounded test seeds its `run-live`. The
  incomplete-authority test claimed a refusal that O3-2 removed by design and
  no ruling covers: re-pinned to the typed record (`:partial`, the dropped
  digest under `:frontier :term-absent`, objective `:partial`), not to "a
  choice follows"; the question is U4 in DEFECTS. No selector edit, no pin
  moved. Run of the three namespaces together: 17 tests, 98 assertions, 0
  failures. Next: R14's packet (its choice was made at 04:03Z; R1, R12 and
  R13 have all landed, which was its sequencing condition).
- 2026-10-26 claude-9 (05:00Z): R14 discovery packet dispatched to codex-10
  (job `invoke-1792990785240-4172-357ac3cd`, park
  `park-88647337-a535-4f35-ae16-9dff12f1aafa`, deadline 05:44Z; the packet's
  header stamp 05:00Z was written a few seconds ahead of the clock, sent
  04:59:45Z). Discovery only, in a worktree on branch `r14-discovery`:
  `outer-receipt` in `test/futon2/aif/wm/q8_frontier_route_test.clj` is given
  the ordinary inputs (as-of, focus trio, seed, a real
  `:policy-field-config` from `reading-route/field` with real task sources),
  the namespace is run once, and the per-test reds and the shapes of
  `:q8-frontier`, `:q8` and the policy status are reported; no assertion
  edit, checkpoint on any edit needed outside the fixture. The re-pin packet
  is written from those shapes after review. Packet
  `~/notes/wm-next/outer-selection/packets/r14-q8-route-discovery-packet.md`.
- 2026-10-26 claude-9 (05:12Z): R14 discovery returned (codex-10, job
  `invoke-1792990785240-4172-357ac3cd`, branch `r14-discovery` `f759c5ce6`).
  Record check: fixture only, as the packet asked (`outer-receipt` given as-of,
  the focus trio, seed 14, real task sources and a `:policy-field-config` from
  `reading-route/field`); no assertion edit; trailers codex-10, dispatched by
  claude-9; kondo 0/0, parens OK. The seat reports one slip, a
  `clojure -M:test -e` that started the broad suite and was killed, results
  unused. Checkpoint fired: `[:policy :status] :refused`,
  `:canonical-binding-refused`. All three families `:computed` (3 reported, 2
  distinct each) and S4 refuses all six policies: each reading-alternatives
  policy `:cascade-validation-error :graph-endpoint-foreign` on its one
  reading-order edge `["p/a" 0] → ["p/b" 1]`; each retraction policy
  `:occurrence-identity-absent` (nodes `"p/a"`, `"p/b"`, no fragment index).
  Q8 frontier absent, Q8 `:not-recomputable :outer-frontier-absent-or-tampered`;
  7 tests, 75 assertions, 47 failures, 1 error, one green
  (`forged-stale-and-omitting-carriers-are-not-accepted`). Cause, read in
  source and confirmed by a probe in the worktree JVM
  (`~/notes/r14-discovery/probe-readings.clj`): `executable_cascade.clj`
  `occurrence-node` makes a node only from an occurrence whose reading carries
  a validated three-halves square; without one the occurrence is an unread
  census entry, not a node. `cascade_decision/occurrence-readings` finds both
  fixture occurrences readable (span, source sha, both pinned digests
  present), but the reading-route fixture publishes no occurrence squares, so
  both are unread, the structural layer has no nodes, and the reading-order
  edge's endpoints are foreign to an empty node set. The clause names the edge;
  the census carries the unread reason. The 2026-10-24 click bound four entries
  because those targets have squares in `storage/wm-occurrence-squares` (8
  records). Two things this settles. (i) The inner route
  (`cascade_decision.clj:1262`) binds only under `:require-occurrence-squares?`,
  which is why `cascade_decision_test` selects on the same fixture: it never
  binds; the outer route binds unconditionally in `sealed-pre-choice-menu`. A
  green inner-route test on this fixture is therefore no evidence that its
  policies are canonically bindable. (ii) Retraction policies are pattern-grain
  by construction (`retraction_cascade/from-retraction` carries no fragment
  index) and collapse at the pre-selector identity check on every bind: the
  fixture's 3 and the 9 of the 2026-10-24 click (S4 row) are one refusal. No
  retraction policy is selectable on the canonical route, though every one is
  generated and scored first. Opened as U5 in DEFECTS. R14 itself needs one
  more fixture input before the re-pin, the artifact the real click had:
  squares for the cited occurrences, built with
  `three-halves-square/build-from-situation-maps` against pattern files that
  carry the four parts (`parts-check` refuses the fixture's one-line
  flexiargs) and published through `occurrence-square-registry/publish!`, with
  `:occurrence-square-root` and `:pattern-library-root` in the config (absent,
  `families-for-field` reads the production store, a hermetic leak). The same
  publisher and validator production reads, not a stub. Step-2 packet
  `~/notes/wm-next/outer-selection/packets/r14-q8-route-squares-packet.md`:
  squares in the fixture, shapes reported, checkpoint before any assertion
  edit; the re-pin follows.
- 2026-10-26 claude-9 (05:32Z): R14 steps 2 and 3 read; step 4 dispatched.
  Step 2 (codex-10, `15642b5c4`): the fixture authors one three-halves
  square per cited occurrence with the production builder and publishes
  through the production registry; every `:graph-endpoint-foreign` refusal
  is gone, the menu seals with only the three U5 retraction refusals, and the
  selector then refuses `:outer-policy-evidence-provider-required`, since the
  test calls `select-live` directly and the port the full-loop runtime
  injects is empty. Step 3 (codex-10, `74efc423a`): the test supplies the
  production provider body over a test-owned learning state and the selector
  reaches `:selected`; but all three cascades tie at G 0.8339233107355899,
  the seeded draw picks M-third, and the runner drops the receipt as
  `:receipt-identity-inconsistent` because the fixture's inner decision is
  for M-current (`full_loop_runner.clj:1098`), so every Q8 reason stayed
  `:outer-frontier-absent-or-tampered`. Four read-only probes in the
  worktree JVM (`~/notes/r14-discovery/probe-step4*.out`) settled the
  re-pin's premises: canonical G is ranked from the cascades, so belief rows
  cannot break the tie but readings can (M-current `["p/a" "p/b"]` against
  three-pattern readings gives G -0.271 vs 0.819, `:certified`, verdict
  `:pass`); the snapshot's one-section `:section-status` left membership
  incomplete and M-off in `:unknown-targets`, outside the canonical census
  (five sections `:ok` restore the exclusion); a declared-priority change
  is not a canonical observation, a republished reading invalidates exactly
  its task, and a fresh field per click changes every cascade digest;
  `canonical-outer-frontier` is a pure projection of the receipt and
  refuses the forged selection, absent frontier, missing and foreign rows
  as predicted. One new defect from the bad cases, R15: a non-numeric G
  throws inside `canonical-outer-frontier` before its own exactness
  refusal can fire (`q8_frontier.clj:131`). Step-4 packet
  `~/notes/wm-next/outer-selection/packets/r14-q8-route-repin-packet.md`
  (codex-10 `invoke-1792992691426-4196-1e137762`): readings per target,
  five section statuses, `reading-route/republish!`, claims per deftest
  from the probed shapes, checkpoint after the fixture change. Two
  observations not opened as defects: the shorter cascade wins at the field
  horizon in every shape tried; the canonical census has no place for a
  task whose pipeline membership is unknown.
- 2026-10-26 claude-9: H4 closed on claude-11's bell. The hand-read of the
  four scored missions is in `futon3c/holes/missions/M-giraffe.md`
  (`29e72624a`): a dependency count leaves the 2026-10-24 tie in place.
  §5 H4 and the Status line amended; nothing in §6 changes, since the
  dependency term was already out of scope for the next run.
- 2026-10-26 claude-9: R14 step 4 reviewed and merged to main (`84c3521cb`;
  five test-only commits by codex-10, `0ab48d1bf` the re-pin). Record
  check: two test files, no src, trailers in order; the fixture checkpoint
  was red only at the two obsolete complete-union claims in the positive
  deftest, the four adversarial deftests green; final run 7 tests / 87
  assertions green; kondo and parens rerun here since the step-4 gate
  outputs were not written to files. Test adequacy: every packet claim is
  in the diff and none is weakened; the dropped complete-union bad cases
  (winner flipped, pair missing, non-scalar law) have canonical
  counterparts (forged selection, absent frontier, no selection,
  unaccounted and foreign rows), and the non-numeric-G case is held for
  R15. The Q8 route pin now certifies the canonical route as production,
  with the complete-union arm as the injury arm; it also pins that a
  declared-priority change is not a canonical observation and that a
  republished reading invalidates exactly its task. R15 dispatched to
  codex-10 (`invoke-1792993191457-4198-ed425ddc`, packet
  `~/notes/wm-next/outer-selection/packets/r15-exact-key-guard-packet.md`):
  bad case first (string or nil G throws inside `canonical-outer-frontier`),
  then the one conjunct that `outer-frontier` already has. Lean:
  `IncrementalTaskFrontier.lean:33` makes the key a total Nat, so the typed
  refusal is the model's boundary and no clause is added.
- 2026-10-26 claude-9: R15 reviewed and merged to main (`9f2a7c844`; codex-10
  `9b71271a0` bad case, `70f4158b5` guard). Record check: trailers in order,
  src diff one conjunct at `q8_frontier.clj:306`, the twin of
  `outer-frontier` `:173`; kondo 0/0, parens OK. Test adequacy: the
  checkpoint log shows string and nil G erroring with `NullPointerException`
  from `Numbers.lt` inside `strict-selection-correspondence`, the exact-G
  control green; after the guard 9/53 and the route regression 7/87 green.
  The expected refusal is the cond's existing `:channel-value-not-exact`
  branch naming the offending task, so the pin is that the guard lets
  evaluation reach the typed refusal. The packet's report-only probe found
  R16: two scored rows of one task with G of mixed types throw
  `ClassCastException` in `row-choice` (`:248`), one seam before the R15
  guard; recorded OPEN in `DEFECTS-reading-era.md`, no Lean clause for the
  same reason as R15. Open for packets: R1 part (b), R4, R8, R11, R16.
- 2026-10-26 claude-9: R16 reviewed and merged to main (`f5a77e21d`; codex-10
  `5229fb224` bad case, `b697fd8d9` repair). Record check: trailers in
  order, src diff is `row-choice` only (`q8_frontier.clj:248-252`): a row
  whose G has no `comparison-key` is chosen ahead of the ranked rows,
  digest-sorted among such rows, so the cond's existing
  `:channel-value-not-exact` branch refuses the task; all-exact rows keep
  the least-G order. Test adequacy: the checkpoint log shows the mixed pair
  erroring with `ClassCastException` in both orders and the nil control
  green; after the repair 10/56 and the route 7/87 green, kondo 0/0,
  parens OK. The existing two-rows test is untouched. Open for packets:
  R4 (Q8b), R8, R11.
- 2026-10-26 claude-9: R4/Q8b dispatched to codex-10
  (`invoke-1792994044346-4200-1c69bd9d`, packet
  `~/notes/wm-next/outer-selection/packets/r4-q8b-stages-packet.md`). Read
  before writing it: the canonical-route certificate carries `:q4` rows, the
  frontier pin and the menu pin but no `:policy-stages`; menu entries carry
  reading-policy keys at `[:action :policy-key]`, so the stages use the
  family route's Id type; `:selection-law` has no `:posterior` on this
  route, so `cascade` also needs a posterior fallback. Found while reading:
  the canonical route never computes a Q6 exclusion (`previous-run/exclusion`
  is only called at `cascade_decision.clj:1248`), so `[:selection-certificate
  :q6]` is nil on attempt-002 and `cascade` refuses
  `:q6-carrier-absent-or-tampered` next; recorded as R17 in
  `DEFECTS-reading-era.md`, discovery packet after Q8b. Open: R4 (in
  flight), R17, R8, R11.
- 2026-10-26 claude-9: R4/Q8b reviewed and merged to main (`a64e1d950`;
  codex-10 `7941e4732` tests and the retained attempt-002 carrier
  projection, `21ce39958` repair). The canonical route now records the
  selected task's stages in `[:selection-certificate :policy-stages]`
  (constructed = admitted = the task's menu keys, scored = keys with a
  scored frontier row, posterior = keys with a `:q4` row) with
  `:stage-provenance` of three pins, and `cascade` takes the stages'
  posterior when `:selection-law` has none. The stage Ids are the menu's
  reading-policy keys, the type Q6's `:excluded-keys` use. Tests: the
  retained carrier projects one key in every stage; a second entry with a
  refused frontier row gives constructed 2, scored 1, Valid mirror false;
  with stages the cascade's refusal moves from `:policy-stages-absent` to
  `:q6-carrier-absent-or-tampered`, which is R17. The integration pin is
  all the target's menu keys (its fixture has two scored policies), not the
  singleton the packet asked for. Six namespaces 37/242 green, spot rerun
  19/146 green. Open for packets: R17 (discovery), R8, R11.
- 2026-10-26 claude-9: R17 discovery dispatched to codex-10
  (`invoke-1792994741258-4201-3b1018e6`, packet
  `~/notes/wm-next/outer-selection/packets/r17-q6-discovery-packet.md`,
  report-only). Five questions: which of the family inputs-digest fields
  (`previous_run.clj:168-187`) have a canonical counterpart and what should
  rearm a refusal on this route; where before `evaluate` the exclusion sits
  so `seal-canonical` and `verify`'s replay see the same population given
  the menu-pin check (`outer_canonical_selection.clj:18`); what
  `decide-exclusion` answers on the attempt-002 predecessor today (a probe;
  expected `:not-triggered`, since its outcome is `:already-satisfied` and
  its `:input-digest` is nil, the same defect one run earlier); where the
  terminal takes the digest from; and every reader of
  `[:selection-certificate :q6]`. Read for the packet: the predecessor var
  is bound around the whole run (`full_loop_runner.clj:8991-8995`) and the
  selector reads it at `meta_live_outer_selector.clj:773`, so the input is
  there; the route just never computes the exclusion. Open: R17 (discovery
  in flight), R8, R11.
- 2026-10-26 claude-9 (06:23Z): R17 discovery returned (codex-10,
  `~/notes/r17-q6-discovery/findings.md`) and checked against the source.
  Its Q3 probe said the attempt-002 predecessor was tampered; it was not.
  `value-digest` hashed `pr-str` under the ambient print vars, so a
  carrier sealed inside the futon3c server (root `*print-namespace-maps*`
  false) read as tampered in a `clojure -M` process (clojure.main binds
  it true) and the reverse: on the run stream the 10-22..24 terminals
  verify only with it false, the 10-25 ones only with it true. The
  runner's predecessor read has the same hidden input, so a click bound
  differently from its predecessor got a Q6 that never fires. Recorded as
  R18 and repaired in-lane: `value-digest` binds the printer
  (`981479974`, merged `0cf37bdfb`), bad case plus two verbatim stream
  pins in `wm/value_digest_print_binding_test`; nine consumer namespaces
  55/393 green. The family Q6 test is red at HEAD on its own (U6, nine
  failures, fixture grew; not chased). R17 implementation dispatched to
  codex-10 (`invoke-1792995696336-4202-724cb1a9`, packet
  `packets/r17-q6-implementation-packet.md`): one behaviour, exclusion
  computed between the frontier and `evaluate` from a canonical digest
  over menu pin, frontier pin and sorted policy keys (not the draw),
  carried through `seal-canonical`, `verify` and `decision`. Open after
  it: R8, R11.
- 2026-10-26 claude-9 (06:52Z): R17 implementation returned (codex-10,
  `77ed4e902` tests, `f513c8298` fix) and reviewed: the canonical route
  now computes a Q6 exclusion from a digest over menu pin, frontier pin
  and policy keys, filters `evaluate` by key with the pin check on the
  unfiltered menu, and carries the sealed carrier through `seal-canonical`,
  `verify`, `decision` and the abstain, so `q8/cascade` answers `:present`
  with the excluded set and the terminal records a refusal. The six bad
  cases each reach the production reader they claim. One review finding
  fixed in-lane (`9fb2e226c`): both input digests sorted entries by
  `pr-str` under the ambient printer before hashing, one seam before the
  R18 binding; `stable-order` sorts by value digest, pinned with a reversed
  menu under `*print-length* 1`. Merged `5f6c5f12c`; family Q6 keeps the
  U6 nine; route green. DEFECTS: R17 REPAIRED-FORWARD, R18 sibling noted.
  The first repaired canonical click seeds the digest; the second can
  exclude. Open: R8 (runtime measures mission holes against pattern-done
  predictions), R11 (parent verdict unbound from child completion).

- 2026-10-26 claude-9 (07:04Z): R8 discovery returned (codex-10,
  `~/notes/r8-measurement-discovery/findings.md`) and read against
  `a3acbbff2`, every cited line checked. The measured tokens are the
  dispatch declarations' locators read as C3/C4; the predicted tokens are
  the scorer's pattern-done wants; the two paths never meet. Attempt-002
  holds no per-occurrence boolean at the after revision: 27 fold
  conditions, all `:unchecked`, each an authored prose completion check
  that nothing evaluates (compiler retains, plan validator shapes,
  falsifier's seven checks omit it). A probe of the 27 against the mission
  file at the before and after revisions: 13 without a quotable phrase,
  13 present at both revisions, one absent at both, so a substring reading
  would not measure the attempt. Decision: the R8 repair is a producer of
  SuppliedObservation-shaped rows with typed absence
  (`packets/r8-pattern-observation-producer-packet.md`, codex-10), and
  R8a records that the boolean branch needs an executable completion-check
  form and its evaluator, which do not exist. DEFECTS: R8 discovery read,
  R8a added. Open: R8 (producer in flight), R8a, R11.
- 2026-10-26 claude-9 (07:08Z): R8 producer packet dispatched to codex-10
  (`packets/r8-pattern-observation-producer-packet.md`). R11 discovery
  dispatched to codex-12
  (`packets/r11-parent-child-verdict-discovery-packet.md`) after reading
  the mechanism: the click script mints the run id, the self-heal parent
  closes `:awaiting-retry` with no run record and no pointer to its child,
  the child is launched only after the parent's running flag drops and
  inherits the script's run id, and no requirements verdict is ever
  computed on the child's record. The parent record is the Lean
  `⟨none, notChecked⟩` by construction. Open: R8 (producer in flight),
  R8a, R11 (discovery in flight).
- 2026-10-26 claude-9 (07:12Z): R8 producer returned (codex-10, `6579b7574`
  tests, `26abdbef9` source) and reviewed: `pattern-observation/supply`
  emits one SuppliedObservation-shaped row per wanted pattern-done token,
  bound to occurrence, effect and after revision, `:observed` a typed
  absence naming why no boolean exists; a mission-domain prediction is
  `:not-applicable`; no boolean is ever read from a fold row. The relabelling
  cases (fold status promoted, boolean field in the fold row, match by
  pattern name) each reach the production function and fail it. One review
  finding fixed in-lane (`6fefe431b`): the test pinned a record sha that
  matched no file. Merged `51478f4ed`; warrant re-registered at the fix.
  Open: R8 wiring (nothing calls `supply` yet), R8a, R11 (discovery in
  flight, codex-12).
- 2026-10-26 claude-9 (07:15Z): R8 wiring packet dispatched to codex-10
  (`packets/r8-pattern-observation-wiring-packet.md`): the retained
  token-outcome receipt gets the supplied pattern rows and a pattern-domain
  comparison beside the mission one, so attempt-002's shape becomes two
  pattern-done predictions against two pattern-domain rows, each typed
  missing. Learning stays on the D-task schema its Joe-signed contract
  names; a pattern-row consumer there is a contract v3 question (R8b).
  Open: R8 (wiring in flight), R8a, R8b, R11 (discovery in flight).
- 2026-10-26 claude-9 (07:18Z): R11 discovery returned (codex-12,
  `~/notes/r11-parent-child-discovery/findings.md`) and read against the
  runner service and the coordinator. The parent's four durable artifacts
  carry no requirements verdict and none names the child; the child id is
  known at queue time only in memory and in the repair ledger's enacted
  transition, and attempt 1 has two such transitions, so even the join by
  injury, repair and attempt does not pick a parent's own child. Neither
  child has a requirements file. Packet A dispatched to codex-12
  (`packets/r11-parent-child-binding-packet.md`): the parent's click
  state names the child before launch and receives its completion on
  close, with `parent-verdict` as the Lean function; packet B (the script
  waits for the child and runs the requirements check) follows review.
  Open: R8 (wiring in flight), R8a, R8b, R11 (A in flight).
- 2026-10-26 claude-9 (07:23Z): R8 wiring returned (codex-10, `da52894d8`
  tests, `8df24a22a` source) and reviewed: the retained token-outcome
  receipt now carries `:pattern-observations` (the supplied rows from the
  construction fold) and `:pattern-comparison` (a pattern-domain reader
  that matches rows by effect and revision and shares its verdict table
  with the mission reader) beside the mission comparison. Learning, pairs
  and surprises read only the mission keys, pinned by a retention whose
  learning receipt equals one without the new keys. Warrant checked at the
  source sha, one spot run from the worktree. Merged `8f0191a91`. R8 is
  repaired forward at the retention seam; attempt-002's two pattern-done
  predictions now meet two pattern-domain rows, each typed missing.
  Open: R8a (an evaluator for authored completion checks), R8b (learning
  contract v3, decision sheet), R11 (A in flight).
- 2026-10-26 claude-9 (07:29Z, first written as 07:55Z ahead of the clock): R11 binding returned (codex-12, `2de8eace4`
  tests, `0c5e083c3` source) and reviewed: a self-heal parent's durable
  click state now names the retry child it launches (`:child`, completion
  `:pending`) before the launch, the child's close writes its completion
  back, and `parent-verdict` reads the Lean function (`pending` is
  `:not-checked`, `complete v` is `v`, no child is `:not-applicable`).
  The packet's child-close test stubbed `close-click!`; the review added a
  real stub-runner child click so the carry of `:parent-click-id` through
  the configured options is pinned, and typed two nil fallthroughs in the
  reader. Merged fast-forward `910ce4e7e`, reloaded, recent statuses
  unchanged. R11 is repaired forward at the binding seam; every child's
  verdict is still `:not-checked :requirements-not-run` until the click
  script waits for the child and runs the requirements check (packet B).
  Open: R8a, R8b (decision sheet), R11 B.
- 2026-10-26 claude-9 (07:41Z): R11 child wait returned (codex-12, `390353523`
  tests, `91c3cfcbe` source) and reviewed: `wm_click.sh` now calls
  `wm_click_wait_child.py` after its tracking loop and before the run
  record check; the waiter reads only `GET /api/alpha/wm/click?click-id=`,
  returns at once when no child is bound, polls a bound child to
  `complete`, and refuses on the cap or two consecutive route failures,
  so the requirements check reads the child's record instead of finding
  it absent. The stub route's shape was read against the producer
  (cheshire keyword names) and a live GET. Warrant checked at the source
  sha, one spot run from the worktree. Merged fast-forward `91c3cfcbe`
  (script only, nothing reloaded). Packet C dispatched to codex-12
  (`packets/r11-child-verdict-packet.md`, job
  `invoke-1793000391515-4210-fdcb976a`): the service reads the child's
  `.requirements.edn`, checks it names the child's run record, and writes
  `:passed`/`:failed` into the parent's `:child :completion :verdict`
  behind a POST route; C2 (the script's call) follows review. Open: R8a,
  R8b (decision sheet), R11 C.
- 2026-10-26 claude-9 (07:50Z): R11 child verdict returned (codex-12,
  `ca4b9f19a` tests, `5ff819fc0` source) and reviewed:
  `record-child-verdict!` carries a `.requirements.edn` into the parent's
  `:child :completion :verdict` only when the file names the child's run
  record and the child is complete with no recorded verdict; every other
  case is a typed refusal that leaves the state as it was;
  `POST /api/alpha/wm/click/child-verdict` is the route. The fixture's
  keyword-keyed file did not match the producer: `wm_run_alert.py` quotes
  keywords as strings, so every real file would have been refused; review
  commit `973f86e46` reads the producer's form, pinned in the on-disk
  shape (U8). Merged fast-forward, both namespaces reloaded, three live
  probes answered. C2 done directly (futon2 `8a81958ca`): `wm_click.sh`
  records the child's verdict after the requirements check and exits 1
  when it cannot. Open: R8a, R8b (decision sheet); R11 unexercised by a
  click.
