# E-outer-selection-landscape — how the outer loop chooses, and what it can see

**Opened:** 2026-10-26 (Joe's question put on 2026-10-25, relayed by claude-11)
**Owner:** claude-9 (research question, record, conclusions). Implementation may go to zai-2.
**Status:** OPEN. Capture C1–C3 to land before the next click; findings F1–F6 recorded; hypotheses H1–H4 open.
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
