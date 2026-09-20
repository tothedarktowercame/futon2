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
