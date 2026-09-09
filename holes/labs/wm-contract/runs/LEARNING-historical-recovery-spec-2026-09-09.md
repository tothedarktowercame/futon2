# Recovering the PLOP learning specification — 2026-09-09

Status: historical reconstruction and validation tickets; not a learning-arm
ruling, implementation completion, or new claim of a live run. This is the
learning part of the paper's recovery specification, not a completed census of
all War Machine claims. Companion: LEARNING-theory-history-runtime-2026-09-09.md.

## Authority and purpose

Joe, emacs-repl, 2026-09-09 (verbatim):

> So there were earlier versions of the plot paper that were more detailed. And some of that material is recorded in the Science 2026 paper. So I think the first order of business is to figure out what the claims were at that time and what evidence there was that they actually worked. And try to get ourselves back to that, so that whether it worked in the past or not, we have those claims working now, and we can always elaborate more later. But the main purpose of this... Futon 2026 work is to build a validated version of the system that we claimed was working in the PLOP 2026 paper. And it's worth looking around in the Git histories and any other resources looking at the code. To see what the claims were and how they were substantiated. And that's the specification that I think we should build towards here. Roughly. Except that we need to get it validated and see that it's working.

Interpretation: recover the claimed behavior and its original evidence before
reducing scope to whatever the current registry happens to describe. Historical
claims provide requirements to validate now; they are not proof. Preserve later
explicit withdrawals and Joe's rulings, including GFN diversity-only and the
RUN4-scoped hierarchy disposition. Do not reinstate failed performance claims.

## Historical sources actually opened

Historical references below use repository, commit, file and line. Resolve with
`git -C /home/joe/code/REPO show COMMIT:FILE`; lines refer to those bytes.

| Source | What it contributes |
|---|---|
| p4ng 0059763, main-2026.tex:398 | July 10 detailed memory claim: channel precision, policy precision, entity beliefs, tactical structure belief; explicitly demo-provenance rather than live production counts for the latter |
| p4ng 0059763, main-2026.tex:549-559 | Preregistered full-loop specification: adjudicated fold events, reliability update, reward retraining, sampler retraining, next proposals, and policy-hole minting; S1–S4 plus stopping rules |
| p4ng e6c4dfb, main-2026.tex:426-432 | July 13 memory and exercised-loop narrative, with information-term and transfer limits; compare with the contemporaneous standing decision, rather than reading every present-tense sentence as a receipt |
| p4ng 9b9c5bd / 4b2a042 | July 31 source split into fragments and PLOP/science drivers: the science companion retains the formal apparatus, so searching only plop-2026.tex misses it |
| p4ng 36d1562, sec-discussion-patterns.tex and empirics.tex | August 21 Campaign S narrative introduced with two-tick, 26-observation BMR and slow-state feedback claims |
| p4ng/science-2026.tex:1-25 | Current driver includes sec-glossary, sec-system, limitations and prereg appendix |
| p4ng/sec-glossary.tex:33-66 | Current distinctions: posterior spread versus EIG; scheduler habit versus cascade prior; BMR/count model; learned-reward slush and conditional partition function |
| p4ng/CHANGELOG-plop-2026.md, Discharge-Trained Cascade Proposal | Explicit later withdrawal of the live-policy claim: reading a trained artifact is not live training/sampling or a closed outcome-to-selection loop |

## Recovery tickets: behavior, evidence, present acceptance

These are proposed documentation/implementation tickets, not edits to the live
worklist. Each must retain its historical citation and a fresh validation record.

| ID | Recoverable claim and original evidence | Acceptance for the current implementation |
|---|---|---|
| LR1 | Cross-episode entity beliefs and error precision persist (July memory paragraph). Current code has belief/reconcile-belief-carry and precision/update-precision-state; war_machine.clj:6088-6260 carries calibration and performs updates | Two successive executions with stable entity/channel identity: record prior, evidence, posterior, persisted state and the next read. Test new/resolved entities, absent evidence and repeated evidence. Label state inference separately from learning model parameters; do not revive inferred-gamma claims using selection gain |
| LR2 | GFN learns the declared conditional distribution over compositions. Retained trainer and exact-enumeration results at futon2 add83658; findings/slice0_trainer_results.json | Reproduce conditional-logZ recovery and shared-logZ rejecting control against the exact target, with frozen configuration. This establishes trainer correctness on the finite synthetic task, not useful mission outcomes |
| LR3 | Slush supplies diverse candidates for a mission. July decision fddc9327 retains 8-mode S4 and withdraws better-success S2 | A pinned current mission/library field, reproducible candidate generation, support/validity checks, diversity measurement against incumbent. No superior-success claim or automatic reopening of the dormant A/B experiment |
| LR4 | Externally adjudicated executions improve reliability/reward and thereby later proposals (July full-loop paragraph and SPEC-full-loop-gfn.md:16-59) | One complete event → versioned reliability/reward → trained proposer → subsequent consumed proposal chain, including failed executions, actual used-pattern attribution, data splits, shuffle and anti-gaming controls. Separate a demonstrated update from demonstrated predictive improvement. Test stale/disconnected consumption explicitly |
| LR5 | Structure reduction evaluates simpler models and feeds a later field (Science glossary plus August Campaign S) | Preserve parent, counts, every proposal, delta-F/threshold, accepted or principled-no-change result; replay exactly and show the next candidate constructor using the resulting model. Aggressive 7→1 merging is an historical observation to investigate, not a success target to force |
| LR6 | New patterns can enter the policy vocabulary and earn status through later outcomes (July mint lane; current Science control-vocabulary paragraph) | Record the policy hole, reviewed pattern admission, initial prior and transition/scoring interpretation, then a witnessed use. This remains a separate extension ticket; no automatic authored-edge or canonical-control creation from learned proposals |
| LR7 | Paper's empirical claims have retained, independently inspectable records | Recover Campaign S's original receipts or explicitly replace its evidential role with a newly frozen two-episode demonstration. Keep the historical integrity question separate from whether the new system works |

LR1–LR5 define distinct tests of the learning behavior the papers describe.
LR2 is independently executable now. LR4 and LR5 require actual later consumers;
passing their producer's tests alone cannot close them. LR6's admission semantics
need their existing owner conventions. This note does not decide which tickets
become RUN4 gates; it makes that decision concrete without quietly dropping them.

## Evidence retained, and limits of reconstruction

The July artifacts are substantial, not just prose. Read this turn:

- futon2/holes/labs/slush-demo/README-reproduce.md: names repositories, environment,
  exact gates, and external data deliberately excluded from Git. This is why a
  repository-only absence must not be promoted to universal historical absence.
- findings/slice0_trainer_results.json: conditional max TV 0.0125095739924579,
  logZ error 0.028944223477111564; shared max TV 0.986546983130949.
- findings/slice2_v2_recovery_findings.md: mission-conditioned recovery 3.9%,
  popularity 8.9%, random 2.6%; explicit HONEST NULL, not evidence of better recall.
- DECISION-2026-07-13.md (introduced fddc9327): 40-flight success comparison
  failed, apparatus retained, diversity supplier adopted, A/B dormant pending labels.
- Sealed proposal JSON and eight flight-batch reports remain present. Their
  existence is established; full re-adjudication of those flights was not done here.

Git history ties the trainer to add83658 and the pure R17 envelope to 8fe540b9
(2026-08-20). Reading the latter's introduction shows it deliberately performs
no substrate writes and returns a structure a later tick SHOULD consume. That
producer contract is not itself evidence of the later tick.

All-ref Git pickaxe searches for the Campaign S run ID in futon2 found only
later audit commits a15c9f82, bb8bcfc0 and 88a3cddd. The campaign receipt UUID
in p4ng history first appears in 36d1562's narrative. An all-ref path history
search for **/cascade_learn* in futon2 returned no commits. These are scoped
text/path-history searches, not a search of every external store, deleted
unreachable Git object, remote host or historical database. The prior audit's
repository-scoped absence therefore remains unresolved, not strengthened into
an assertion that the run never happened. Search external archives next only
through their documented read interfaces; do not replay an old live actuator.

## Verification in this investigation

The default python3 initially failed because numpy is absent. The reproduction
README names futon3a/.venv; using its interpreter, test_gfn_core.py passed all
9 tests, including conditional convergence and the shared-logZ fault. No code
change or environment workaround was needed. The full 700-iteration gate also completed successfully. Existing historical findings were
not overwritten (no --write-findings). No paper, registry or live state changed.


Fresh full-gate command (separate process, from slice2):

```text
/home/joe/code/futon3a/.venv/bin/python3 gfn_core.py --iters 700
exit 0
logz_spread_nats: 5.638033105726288
conditional max_tv: 0.012509573992457787
conditional max_logz_err: 0.028944223477111564
shared max_tv: 0.986546983130949
G0a_pass: true
G0b_pass: true
```

The new conditional TV differs from the stored value by approximately 1.1e-16;
this is numerical reproduction, not byte-identity. Both result booleans were
inspected explicitly: the CLI returns zero even without asserting those gates.
This fresh run validates the retained synthetic trainer claim; it neither
recreates the forty live flights nor closes the outcome-to-proposal loop.
