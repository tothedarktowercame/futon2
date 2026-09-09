# Learning: theory, PLOP history and runtime — 2026-09-09

Finding: the previous question to Joe conflated parameter learning, structural
learning and learned cascade proposals. Withdraw the recommendation to adopt
"offline-only learning" as a blanket RUN4 disposition. Joe has not ruled it.
The registry's :learning entry describes R17 structure learning, not every
adaptive process in the system.

## What the cited AIF formulation requires

Primary source: Da Costa et al., *Active inference on discrete state-spaces:
a synthesis*, [arXiv v2, sections 8–9 and Appendix A.1](https://arxiv.org/html/2001.07203v2).
Section 8 derives parameter learning from variational free energy, with
Dirichlet counts updated by observation/state-belief outer products. Equation
21 explicitly permits an end-of-trial update and carries the posterior into
the next trial's prior. Appendix A.1 gives corresponding transition/initial-state
updates. Section 9.1 describes BMR using existing evidence, including trial-end
reduction. Thus these learning processes need not occur within every action
step. The relevant distinction is between scheduled learning whose result is
consumed later and an unused offline artifact.

Interpretation for our system: a fixed-parameter action/perception model can be
a scoped AIF model, but it does not demonstrate the parameter-learning process
we also claim. Likewise a tested BMR function does not demonstrate adaptive
structure feeding future actions. We need an explicit model/update/consumer
contract rather than a blanket AIF-compliant label or an assumed per-tick rule.

## What PLOP says, and what the artifacts support

The paper driver p4ng/plop-2026.tex includes sec-overview and sec-catalog;
sec-overview conditionally includes sec-overview-plop. Read the actual learning
passages at p4ng/sec-catalog.tex:340-353, not just the four-choice registry.

| Mechanism | Existing artifact and actual computation | Limit relevant to RUN4 |
|---|---|---|
| Parameter learning of the generative model | The declared AIF update uses observations and inferred states. futon2/src/futon2/aif/a4a.clj:85-113 instead recounts capability×mission edges/discharges from a fixed 0.1 prior | That different carrier is not the tick model's observation/state Dirichlet update; a witnessed absence of the import does not implement it |
| Structural BMR | futon2/src/futon2/aif/r17_offline.clj:64-101 records frozen input, every proposed merge, delta-F threshold, resulting structure and replay input | It returns a usable envelope, but a later production consumer must actually adopt the result; current src/scripts census finds no external reference to r17-offline |
| GFlowNet/slush | futon2/holes/labs/slush-demo/DECISION-2026-07-13.md retains GFN as diversity supplier and the external-adjudication apparatus; its superior-success claim was retired after the 40-flight comparison | Do not discard the apparatus, or reinstate a better-chooser claim. futon2/src/futon2/aif/a4a_substrate.clj:164-170 explicitly reads deposited candidates without live training or sampling |
| Outcome-to-proposal feedback | futon2/holes/labs/slush-demo/SPEC-full-loop-gfn.md:16-54 proposes adjudication → reliability/reward update → retraining → later proposals, including new-pattern admission | This is the intended closed cycle; the current PLOP catalogue itself says that closing the witnessed-outcome-to-later-selection loop remains a proposal |
| Cascade relation credit | futon3/checks/learn_edge_weights.clj:35-55 separates edge weights from proposals and credits recorded attachments | It explicitly lacks per-attachment G improvement and does not feed the stores into cascade construction. Attachment frequency is not evidence of usefulness |
| Habit | futon2/scripts/futon2/report/war_machine.clj:6609-6620 carries scheduler prior separately and folds strategic selections through strategic-habit/carry | Frequency of selection is not outcome learning. The strategic fixture still stands; forward accumulation alone is not promotion |
| Outcome-based selection gain | futon2/scripts/futon2/report/war_machine.clj:6091-6107 folds prior trace realized-outcome through selection-gain/fold-realized-outcome | No sample means no update; this requires an admissible paired outcome and is not structure learning |

### A concrete PLOP/source disagreement

p4ng/sec-overview-plop.tex:65 and sec-catalog.tex:340 claim an inter-tick BMR
pass on 26 observations fed Tick B. The later audit
futon2/holes/labs/wm-contract/VERIFY-r-nodes.edn:1805 records that the cited
Campaign S IDs were found only in prose, not retained records. Its R17 audit
at :354-364 separately reports no production r17-offline caller and a different
accumulation carrier. Today's read-only src/scripts search still finds only the
namespace itself for r17-offline; the pure A4a reduction has its own internal
calls, which must not be mistaken for a live tick consumer.

This does not prove that the historical event never occurred. It means that
retaining its paper claim is not a substitute for reproducing an evidence chain
now. The existing PLOP text needs an evidence reconciliation ticket before its
conference claims are finalized; no paper text was changed in this investigation.

## A concrete learning contract to design before Joe rules

Proposed minimal demonstration, not yet commissioned implementation:

1. Name the learned object. For generative learning, identify the precise
   observation/outcome and latent-state domains, the prior, and how current
   observations and state beliefs supply sufficient statistics. Do not import
   capability×mission counts under another name.
2. At a declared step/trial boundary, produce a versioned posterior from
   admissible new evidence, with event identity and exactly-once treatment.
   No evidence gives a named hold. Preserve explicit zeros and domain refusals.
3. Persist that posterior and prove by a subsequent execution that the next
   prediction/scoring step reads it. Record old/new model hashes, counts and
   predictions. A posterior change need not change the winning action, but
   model consumption must be observed. Include a disconnected-consumer control.
4. Separately, if cascade-use learning is in RUN4 scope, attribute independently
   judged outcomes to actually executed members, update the named reliability
   or proposal model, and have the next proposal stage consume that version.
   Preserve authored graph authority: learned edge proposals are not authored
   edges. Distinguish attachment counts, selection counts and success evidence.
5. BMR may operate at that boundary or on a declared slower schedule, testing
   reduced priors against accumulated evidence and admitting a changed model
   through its owner. A principled no-change is a result; it must not become a
   reason to invent an improvement. GFN retraining remains a separate proposal
   mechanism with the July diversity-only standing and its controls retained.

The implementation decision therefore needs to specify both the learning
object and its feedback boundary. "Offline" alone neither establishes a
compliant learning loop nor rules one out. A per-step scheduler frequency
update alone cannot discharge the generative-learning or cascade-quality claim.
No approval is requested from Joe until these distinct scopes are understood.

## Executed checks and limits

Fresh separate-process tests: r17-offline, bmr and a4a, **26 tests / 92 assertions,
0 failures/errors**. This verifies pure reduction and replay behavior, not live
wiring. Source census and paper/audit reads are the evidence for the wiring and
claim discrepancies; no live run, no state writes, no new GFN training, and no
code or registry changes were made. The historical cascade_learn implementation
was not located by filename searches across the surveyed Futon repos; its name
in the July specification is not treated as a verified current component.

Online primary-source retrieval: PMC returned a browser challenge and UCL PDF
fetch timed out; the arXiv HTML supplied the inspected learning equations.
Local first searches included a nonexistent Campaign glob; subsequent scoped
src/scripts and named audit reads supply the findings reported here.
