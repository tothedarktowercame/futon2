# wm-contract board — refreshed every claude-15 tick

Updated: 2026-09-14 ~23:15Z (packet 1 accepted + fixed; packet 2 with codex-24)

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
- **ruling packet 2: successor discharge + readback** — codex-24, job
  `invoke-1789426402247-20904-237755dc` (running), park `park-a119b57f`.
  Existing resolution writer + explicit T/R/S relation + five ruled negative
  controls. Packet 3 (cohort-54 T/R/S/O prereg) is mine, right after.

## DONE (this sitting, newest first)
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
