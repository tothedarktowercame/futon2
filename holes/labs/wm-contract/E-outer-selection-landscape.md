# E-outer-selection-landscape — how the outer loop chooses, and what it can see

**Opened:** 2026-10-26 (Joe's question put on 2026-10-25, relayed by claude-11)
**Owner:** claude-9 (research question, record, conclusions). Implementation may go to zai-2.
**Status:** OPEN. Capture C1–C4 to land before the next click; findings F1–F6 recorded (F3 corrected in §10.2); hypotheses H1–H4 open; §10 answers Joe's 2026-10-26 questions.
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

## 6. Capture to land before the next run (C1–C3)

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
