# wm-contract board — refreshed every claude-15 tick

Updated: 2026-09-15 ~05:05Z (A5 preflight accepted; cohort 57 EXECUTING with required preflight step)

## OUTSTANDING blocks — honest count
First completion revision of the DAG snapshot committed (`714822b6`): the
snapshot format had NO "done" state, so nothing could ever turn green.
Now: **5 done** (a-rubric, a-validator, r10-caller, categorical-ambiguity,
existing-record-audit),
**1 in-flight** (a-labels), 52 blocked / 7 unbuilt / 9 built-not-wired /
4 ruled-parked. The a-estimate → q-witness-flip chain Joe named is genuinely
red: a-estimate needs a-labels annotations; q-witness-flip needs q-resolver
(needs policy-plans + model-assembly).

## IN FLIGHT
- **Logic-model lane (claude-5, 2026-10-20 04:54Z)**: M10a landed (futon2 `83f72ba1e`
  source by codex-3, `11fe9094a` tests by claude-5): `integrate` discharges only on a
  successful retry; an abstained retry is `:retry-inconclusive` and stays enacted. M11L
  (claude-2, job `invoke-1792472009621-17876-e5a72b15`, park `park-97ce4eb3`, deadline
  05:43Z): SelfHealRoute rev 9, `closingToday = closing` and the re-queue edge with a
  retry budget, Lean first; M10b (code) after it. Ledger:
  `logic-model/PACKETS-2026-10-19.md`. Clicks paused, budget 0.
- **Cohort-57 attempt-001 = O-57** (click wm-click-47248e0c, 03:59Z; author
  codex-23, reviewer codex-22). The prereg (1ee3e269) makes the deposit
  preflight a REQUIRED author step and hands the author the accepted worked
  template. A5 preflight ACCEPTED (855953a9/acfc78ca — lead re-ran it:
  template green, five failure shapes refuse with production keywords).
  Exercise 8 rides on this close regardless of outcome; a clean one is the
  first qualifying-annotation candidate (A8).

## DONE (this sitting, newest first)
- **Packet 3 complete**: cohort-54 prereg `94b4272a` (T/R/S/O declared pre-outcome:
  T = the artifact-binding repair; R = cohort-53 att-002; S = first grounded close
  here; O = first measured close after the resolution exists; S-as-O declared
  impossible — deposits precede grounding). Activated; futon3c rebound `28f97372`;
  serving JVM reloaded (cohort echo 54, packet-2 seam + readback live). Discovery:
  the runner ALREADY auto-resolves implemented obligations at the next grounded
  close (stop-line-validation), and subject-pair companions carry arbitrary bytes,
  so no new admission code was needed.
- Ruling packet 2 ACCEPTED (`8f5bea11`): successor discharge through the existing
  writer with explicit T/R/S relation record; five ruled negative controls + the
  projection-pointer refusal; pure cutoff-aware derived-state readback. Carry-over
  completed in-lane (`eec07f38`): :standing-readback now pinned END-TO-END into the
  written 007 (a second dropped-thread in close!'s judgment assembly found+fixed).
  Fixture insight recorded: the retention-test family closes :build-failed silently
  since 48dfa037; tests pass on retention-only assertions — cleanup queued.
- Ruling packet 1 ACCEPTED (`81ce8543`, acceptance `735c377e`) with one reviewer fix
  in-lane (`d1ff5aab`): the pending-successor store annotation was computed then
  DISCARDED — now ensure-standing-decision! returns the readback and the grounded
  close persists :standing-readback. Subject pairs = T's obligation bytes
  (subject-*.edn, exact id enforced); source pairs demoted to supporting-*.edn;
  ambiguous names refuse.
- **Route A RULED and ACCEPTED** (codex-26 `6bfe8750`, acceptance `ff323720`, 4/4 pins):
  the :addressed subject for repair entity T is a later predeclared observation
  boundary O — the repairing close never labels its own future; S may serve as O
  only if everything (successor evidence, actual resolution, after-state, standing
  review) beats O's single cutoff; pending S at the boundary = typed insufficiency.
  Three bounded packets authorized.
- Exercise-5 observation ACCEPTED :evidence-insufficient (`1744f829`): 18/18 pins,
  both gaps verified real (revision pair names the artifact, not the repair entity;
  no successor-limb evidence — the "discharge" id is a projection pointer, repair
  store still :open). STRUCTURAL PINCER identified: :resolved blocks :strengthened,
  missing successor blocks :addressed — repair-class subjects cannot currently reach
  any positive status. Design routed to codex-26, NOT another blind exercise.
- **cohort-53 attempt-002 CLOSED :grounded-change** (zai-5 author, codex-24 approved,
  single round, commit `fba72d0f`: non-commit artifact claims classify as
  :artifact-ref-malformed — composes with the freshness fix, my stale-claim test
  survived adapted). Close carries retention + 15-entry manifest + limb receipts +
  revision pair + the FIRST enforced standing decision (:resolved, codex-24).
  The artifact-binding repair target that opened 2026-09-13 is now claimed resolved.
- Exercise-5 declaration + blinded view committed `cecd6f7d` (17 files; companion
  code-vocabulary nuance documented).
- existing-record-audit ACCEPTED (TN `069b0c30`, codex-21; acceptance committed; pins
  re-verified, absence claim reproduced). Row 23: 12 usable byte-pinned joins. Rows
  13/15: typed insufficiency, named classes — downstream needs NEW production capture.
  effective-config sharpened: missing class :effective-horizon-detail-fpi-configuration-record.
- categorical-ambiguity: ALREADY DONE this morning (codex-22 `602ff836`, my acceptance
  pre-compaction); codex-23 correctly detected the duplicate dispatch, zero edits;
  node flipped done (`354eba57`). risk-ambiguity-witness now waits only on q-risk inputs.
- DAG completion revision `714822b6` (verify :status :pass, mirror equality held).
- Close containment accepted `0198384c`; pair-companion admission accepted `286d5967`;
  stale-claim freshness fix `94ce60fe`; zai-5 386cfe33 reviewed `04267707`.
- Serving JVM reloaded (all repairs live); casting amendment (claude-15 out of cast).
- Exercise-4 accepted :evidence-insufficient; cohorts 50-52 exhausted; cohort 53 armed.

## NEXT
1. Bellbacks: review both packets; flip their DAG nodes on acceptance.
2. Attempt-002 close: typed-or-grounded 007; B-prime check; exercise-5 if eligible.
3. Then: effective-config readback packet (third no-prereq node); dedupe/injectable-
   registration cleanup packet.

## BLOCKED ON JOE
- Nothing now. (Frontier item 8 — slow-to-policy semantic law + precision-law
  adoption — is a ruling-shaped item that will need a decision sheet eventually;
  not urgent, will prepare one when its lane opens.)

## POLL HANDLES
- Jobs: `GET localhost:7070/api/alpha/invoke/jobs/<job-id>` · Parks: `GET localhost:7070/api/alpha/parked`
- Attempt cells: `futon2/data/wm-full-loop-machinery-53/wm-contract-machinery-53-v1/attempt-002/`
- DAG: `runs/outstanding-dag-2026-09-14/dag.edn` (+ verify_dag.bb) · Ledger: STATUS.edn

## RESOLVED (2026-09-20 late): cohort serialization boundary — fixed at the writer (10d3dc90, receipt c7c0545f; reviewed claude-12)

The runner persists dispatch responses with pr-str and reads them back
with clojure.edn/read-string; a payload carrying functions/atoms
writes fine and fails later, elsewhere, as an opaque reader error
(#object). Trigger removed by the fixture migration (20e15631 —
observers now return valid responses or throw); the WEAKNESS itself is
explicitly unresolved (codex-5's finding, baseline artifact retained
at sha 850cb973... in the migration receipt). Fix shape when taken:
the writer refuses or tags non-round-trippable payloads at WRITE time
— failures surface where they are caused. RESOLUTION: the writer
round-trips its emitted string through edn/read-string; success writes
the ORIGINAL string byte-identically, unreadable values become
{:payload/status :non-edn-payload :reason :not-round-trippable
:summary <sanitized 256-char prefix>}. Both-sides tested incl. the
retained baseline payload and checkpoint/close/ledger continuity;
cohort 28/153 + runner 180/998 green, warrants registered. No reader
changes; existing corrupt historical files stay as-is (record, not
rewrite).

## OPEN ITEM (2026-09-20, from cascade-realizer gate): fold-realized-test pre-existing errors

fold-realized-test errors 3x with "actuator-a3: rejected deposits in
corpus load" (:prompt-not-reconstructable) on CLEAN baseline 1ed5006e
— byte-identical logs with and without the realizer draft (attribution
in cascade-realizer-2026-09-20/STATUS.md). Pre-existing suite debt,
not introduced; needs its own classification (why are a3's deposits
unreconstructable — data rot, schema drift, or a real actuator
defect?). Owner: unassigned.

## REOPENED (2026-09-21): pr-str/read-string boundary — second costume

The cohort-writer fix (10d3dc90) closed the dispatch-response
instance; click 5 died on the same boundary at record MINTING:
want-token produced :hole/2f9b03b16170 (digit-leading keyword name —
pr-str writes it, the reader rejects it), fixed at 29fb2a83
(injective h-prefix, both-sides tests). General item: round-trip
guards belong at every record write/mint boundary; two demonstrated
costumes now exist to test against. Also for preflight (from the
five-click post-mortem): (a) pre-click checks must ROUND-TRIP a
decision record through the reader, not merely compute one; (b) a
loaded-namespaces-vs-disk probe for tick-critical namespaces (two
clicks died on committed-but-unloaded code). Owner: unassigned.

## RULING NEEDED (morning, from item-5 discovery): who admits a machine-proposed interpretation?

Supply path (b) is designed: worklist ticket -> :assures
{pattern, signature} -> pinned apparatus pattern for evidenced ticket
links; pinned-tension retrieval for other wants; retrieval similarity
and prose signatures never count as applicability (codex-8 discovery,
invoke-1789960853067). The open design decision: generated proposals
need INTERPRETATION RECEIPTS (a reading of the pattern against the
target — the field the three hand-admitted cascades carry with
author/date/pin), and every existing receipt was a deliberate agent
act. Options: (i) proposals enter :proposed and an agent lane reviews
and admits the reading (interpretation admission becomes reviewed
work inside the Act cycle); (ii) a typed hole-kind applicability
mapping is added to the 14 apparatus patterns (0/14 today) so
admission is mechanical where the mapping is exact; (iii) both — (ii)
where typed, (i) elsewhere. Also in scope: mission_hole_wants
assigning ONE generic pattern to every mission is the "generic
closure pattern" the sketch bans; it should become a decline or a
real per-mission proposal. Packet (a) (no empty candidates, uniform
admission, recorded declines) is dispatched separately and does not
wait on this.

## MORNING LIST addition (2026-09-21): U80 / U83 (from codex-1's decline ledger)
The only two non-completed rows in the evidenced worklist join are
ruling-shaped: U80 requires resolving contradictory requirements;
U83 requires restating or retiring a task whose queue no longer
exists. Both declined for admission tonight (correctly — not
current unmet objectives an agent can read a pattern against);
both need Joe's word, not build work.

## OPEN ITEM (2026-09-21 ~05:40): futon3c ast-helper burst leak

The 17:01Z 2026-09-20 executor-wedge event fan-out spawned 168
python_ast_helper.py children of the futon3c JVM in one two-second
burst; none were reaped and they idled 12.5h until killed 05:40Z
(JVM verified healthy after; 3 live helpers remain, normal). Fix
shape when taken: the helper invocation path needs reap-on-error /
pool bounds so a burst failure cannot orphan its workers. Owner:
unassigned; futon3c lane.

## SelfHealRoute rev 9 accepted; M10b split, M10b-1 running (claude-5, 2026-10-20 05:08Z)

The retry-closing law is met in code (`closingToday_eq_closing`, futon2 `11fe9094a`,
mathlib4 `darktower` `7b2bbf9b88` + `2cda33008c`). The re-queue edge is now the law in
Lean and not in code: an inconclusive retry today ends `:awaiting-retry` with nothing
queued, forever (`requeue_todayAwaitsForever`). Realising it touches four sites, so
M10b is three packets: M10b-1 ledger (codex-1, `invoke-1792472845052-17877-ee13627a`),
then M10b-2 adapter id, then M10b-3 coordinator + runner. Open for Joe: the first
coordination click's budget (click budget 0 by design); claude-2's anchoring pass.

## M10b-1 accepted; live ledger migrated; M10b-2 running (claude-5, 2026-10-20 05:21Z)

The ledger can now record the re-queue edge (futon2 `6308c946a` codex-1, review tests
`ca3893720`). Two review findings fixed: the validator's re-queue rules were unpinned by
forged entries, and the live production ledger lacked the new `:retry-limit` key, so it
loaded as tampered; migrated with a backup beside it. M10b-2 (adapter retry id with the
re-queue ordinal) is on codex-7, job `invoke-1792473638818-17878-c815b880`, park
`park-a0c76dec-eb2d-49e5-ac93-3ceac529dcc0`. M10b-3 (coordinator + runner) next. Still
open for Joe: the first coordination click's budget; claude-2's anchoring pass.

## M10b-2 accepted; M10b-3 split in three, M10b-3a running (claude-5, 2026-10-20 05:28Z)

The adapter's retry click id carries a re-queue ordinal (futon3c `e4f58cfc4` codex-7,
review test `603170b12`: the shipped adversarial pair did not collide under the naive
suffix form it named; the pair that does is now pinned). Reading the premises for
M10b-3 moved the re-queue site: the inconclusive observation (T:222-225) names no
retry click, so the coordinator cannot attribute it. M10b-3a (observation attribution
plus a `pending-requeue` reader, futon2 L + T) is on codex-1, job
`invoke-1792474061581-17879-9eca5030`, park `park-35596544-79b8-40b8-b354-20892b1a792c`.
M10b-3b (coordinator `:queued` branch) and M10b-3c (adapter retry port) follow. Still
open for Joe: the first coordination click's budget; claude-2's anchoring pass.

## M10b-3a accepted; M10b-3b (coordinator) and M10b-3c (adapter port) running (claude-5, 2026-10-20 05:36Z)

futon2 `f579f4ef8`: the `:retry-inconclusive` observation carries `:injury-id` and
`:retry-click`, validated for that kind only (the live ledger's four bare abstentions
still load), and `pending-requeue` reads the latest enacted entry's inconclusive
observation. Two mutations in a worktree each turned their test red; no findings.
M10b-3b (codex-1, `invoke-1792474542158-17880-9ecbd9a9`) re-queues from the coordinator's
`:queued` branch through a `:retry-click-id` port; M10b-3c (codex-7,
`invoke-1792474553368-17881-ea94ff3f`) makes the adapter's retry port queue a
coordinator-supplied click and expose the minting function. Production wiring and Lean
rev 10 follow. Open for Joe: the first coordination click's budget, claude-2's anchoring
pass.

## M10b-3 landed: inconclusive retries re-queue end to end (claude-5, 2026-10-20 05:44Z)

futon3c `f2e5a9fa6` (codex-7) makes the adapter's retry port queue a coordinator-supplied
click and exposes `retry-click-id` as `:retry-click-id-fn`; futon2 `aa3ec313b` (codex-1)
makes the coordinator's `:queued` branch re-queue while `pending-requeue` names an
inconclusive observation, ledger first and port second, under `:retry-requeue-<n>`
receipts, abandoning on the spent budget; futon3c `7efc13bd8` and futon2 `493388b7b`
(mine) wire the minting function through the runner and `effect-ports` and read the
click result from the latest retry receipt. Both reviews ran a mutation in a worktree;
one test gap (the typed refusal with no minting port) was filled in `493388b7b`. A stale
runner-service assertion left by F15 is repaired in `7efc13bd8`. Next: Lean rev 10
(claude-2). Open for Joe: the first coordination click's budget, claude-2's anchoring pass.

## SelfHealRoute rev 10 accepted; decision sheet for Joe (claude-5, 2026-10-20 05:53Z)

mathlib4 `darktower` `92c213cdd6` (claude-2): the re-queue edge is met in code for every
outcome that writes a `:retry-inconclusive` observation (`nextToday_eq_next_ofObserved`),
and the two it misses are proved exactly (`nextToday_disagreement`: a retry that fails
with a different injury, and a retry that left no record; witness
`requeue_todayMissesUnobserved`). Read against futon2 `493388b7b` and futon3c
`7efc13bd8`; no findings. Next in lane: M10b-4 closes the observation gap in T. Open for
Joe, now as one sheet, `DECISIONS-FOR-JOE-2026-10-20.md`: the first coordination click's
budget (live: 128 allocated, 128 consumed, 0 available); claude-2's anchoring pass.
