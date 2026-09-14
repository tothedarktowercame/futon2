# wm-contract board — refreshed every claude-15 tick

Updated: 2026-09-14 ~22:06Z (DAG revision + parallel lanes opened)

## OUTSTANDING blocks — honest count
First completion revision of the DAG snapshot committed (`714822b6`): the
snapshot format had NO "done" state, so nothing could ever turn green.
Now: **3 done** (a-rubric, a-validator, r10-caller — committed acceptances),
**1 in-flight** (a-labels), 52 blocked / 7 unbuilt / 9 built-not-wired /
4 ruled-parked. The a-estimate → q-witness-flip chain Joe named is genuinely
red: a-estimate needs a-labels annotations; q-witness-flip needs q-resolver
(needs policy-plans + model-assembly).

## IN FLIGHT (3 parallel lanes now)
- **cohort-53 attempt-002** (measured, zai-5 author, cells through 004-dispatch)
  — the a-labels critical path. On a close retaining an approved review →
  exercise-5 → annotation #1.
- **categorical-ambiguity** — codex-23, job `invoke-1789423557292-20872-803ce400`
  (running). Pure estimator per LEAD-DECISIONS:196-201. Unlocks risk-ambiguity-witness.
- **existing-record-audit** — codex-21, job `invoke-1789423558330-20873-40385e48`
  (running). Rows 13/15/23 evidence discovery. Unlocks 4 nodes.
- Park `park-a638e62c` covers both packets; fallback wakeup ~25 min.

## DONE (this sitting, newest first)
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
