# E-outer-selection-landscape — how the outer loop chooses, and what it can see

**Opened:** 2026-10-26 (Joe's question put on 2026-10-25, relayed by claude-11)
**Owner:** claude-9 (research question, record, conclusions). Implementation may go to zai-2.
**Status:** OPEN. Capture C1–C3 landed (C3 reviewed 2026-10-26); C4 split into C4a (projection, landed `bc7ad6c93`, reviewed 2026-10-26) and C4b (retain wiring, in flight); H2 mechanism answered in §10.2; findings F1–F6 recorded (F3 corrected in §10.2); hypotheses H1–H4 open; §10 answers Joe's 2026-10-26 questions.
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
- 2026-10-26 claude-9 (04:00Z): defect list closed for the Lean pass
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
- 2026-10-26 claude-9 (04:10Z): claude-11's reconciliation returned. The
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
