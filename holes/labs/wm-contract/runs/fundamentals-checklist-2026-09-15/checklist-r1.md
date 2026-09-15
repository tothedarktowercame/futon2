# War Machine: fundamentals-first completion checklist

**Revision 1 — 2026-09-15. Prepared by codex-28 at Joe's request. NOT YET RATIFIED.**

## Verdict

**The specified War Machine is not complete. No qualifying all-node run has been established.** There are implemented components, mathematical definitions and proofs, reviewed reference computations, and grounded machinery attempts. These are real accomplishments at their stated scopes. They do not establish that the connected machine implements the specified active-inference system.

This checklist states the acceptance obligations and preserves the evidence limits. It is a consolidated review document in the existing wm-contract directory, not a new execution service or an alternative specification. Until ratification is complete, it must not be advertised as the definitive agreed account. See the adjacent ratification record for responses on the exact document hash.

## 1. What determines completion

Authority is Joe's specification and explicit corrections. Agent-authored scoping notes, classifications, parked work and paper edits cannot weaken it.

1. **Real cascades, grounded in use.** [M-G-over-cascades](../../M-G-over-cascades.md), HEAD, records Joe's June 23 instruction: concretely construct cascades from real patterns, their links and mission usage, and evaluate composition through grounded discharge rather than a demo-tuned metric. [June 12 Sortie 12](/home/joe/code/futon3c/holes/flights/F-wm-piloted-2026-06-12.md:355) records the earlier cascade-grain requirement.
2. **The paper's operational claims define the completion frame.** [September 12 rulings, Item 3](RULINGS-walkthrough-2026-09-12.md:141) requires the complete PLoP operational-claim inventory, not just its margin notes.
3. **Two final artifacts.** Item 4 requires a timestamped run doing real work and Lean-computed validation of that run's runtime certificate, including AIF and wiring correspondence.
4. **All nodes first.** Item 5, [line 222](RULINGS-walkthrough-2026-09-12.md:222), requires every node defined, validated and working, provably. Earlier runs are machinery tests. Machine Q must be built, not removed from scope. No whole-node exclusion follows from an agent's deadline or reporting choice.
5. **No workaround.** Missing model inputs, independent evidence or consumers remain missing. Invented observations, silently substituted policies, self-certified outcomes and renamed engineering scores cannot close a row.
6. **Broader purpose remains visible.** The four Joe-endorsed [apex requirements](../../DESIGN-REQUIREMENTS-apex-2026-09-09.md) are warranted records, one queryable self-account, feedback reaching participants, and accountable choice including goal formation. None is discharged merely by naming a preference weight.

The older [FUNDAMENTALS.edn](FUNDAMENTALS.edn) tests a narrower question: whether constructors are absent on BOTH the Lean and runtime sides. Its six-item census excludes some wiring and interpretation gaps. **That criterion is not the completion criterion here.** A theorem with no production consumer remains incomplete under Item 5.

### Evidence needed to check off a requirement

For each applicable row retain: (a) its specification and semantic assumptions; (b) a definition/law at that meaning, with actual proof dependencies; (c) the matching implementation and independent correspondence checks, including rejecting controls; (d) its actual production caller and effective configuration; (e) joined runtime evidence that the required producer, consumer and downstream effect occurred. Pure interface definitions, recorded-table proofs and operational mechanisms have different proof obligations; none may borrow another's evidence.

In the tables, **PARTIAL** means a named smaller accomplishment exists and completion remains open. **OPEN** means the required completion evidence is absent in this audit. **UNRESOLVED** means source disagreement or incomplete investigation must be settled. None of these means DONE. Historical evidence is explicitly dated. This audit reviewed sources and existing receipts; it did not run a new WM attempt, load code or rerun the Lean/Clojure suites.

## 2. Fundamental constructions and their use

Each unchecked box is required completion work. Source keys appear in section 6.

| ID | Required behavior | Accomplished, at the stated scope | What has not been accomplished / acceptance evidence still needed |
|---|---|---|---|
| F01 | Sound, shared probability and model carriers | mathlib4 `480a666ad2` adds duplicate-free support, zero mass off support and nonnegative PrecisionMap; author reports committed rejection controls and successful build. Independent acceptance receipt not yet supplied. | [ ] **PARTIAL:** bind dependent proofs and admitted witnesses to the repaired source; verify repair acceptance and avoid inheriting old admissions by filename. Record State/Action/Outcome, model revision and numeric semantics consistently. [K,L] |
| F02 | Current belief becomes the model's state distribution | Rows 7 and 13 record categorical belief readers, explicit float admission and carried updates. | [ ] **PARTIAL:** prove the actual post-update entity/context distribution reaches the production cascade predictor; no fabricated joint state or uniform fallback. [W rows 7,13; T] |
| F03 | Action-conditioned B matches the machine's declared dynamics | Row 8 source module, proofs and reference witness exist for declared priors. | [ ] **PARTIAL:** full live action/pattern interpretation coverage, same B at prediction and belief-update consumers, complete policy-plan bindings, measured-vs-declared provenance. The two-action reference does not cover the production domain. [W8,W14,D,S] |
| F04 | Observation A has the required observed authority and compatible domain | Annotation rubric/validator and observation exercises exist; the categorical ambiguity implementation refuses unlicensed A. | [ ] **OPEN:** accepted independent state/outcome pairs, coverage and sparse-data follow-on collection, measured A, admitted model assembly. A belief argmax is not an observed label; one accepted annotation is not a complete model. [W14,D] |
| F05 | Q(s|pi) and Q(o|pi) are constructed from those inputs over the full policy | Rows 6/9 supply multi-step modules, generic laws and independent reference calculations, with float and placeholder-A caveats. | [ ] **PARTIAL:** real cascade plans and measured A/B/context feed the actual scorer; every horizon step and model identity retained. Standalone constructor acceptance is not live integration. [W6–9,W14,T] |
| F06 | C is the specified preference distribution on Q's outcome domain | Ruled terminal `machineC`, twelve tagged masses, seven zeros, normalized seed and supplied-kernel risk exist. Channel preferences demonstrably affect controller scores. | [ ] **PARTIAL:** compatible state-dependent disposition bridge and actual scorer use with matched Q/C and pins. Codex-27 reports subsequent scope settlement to terminal machineC; exact owner authority is requested. Arbitrary intermediate C_tau construction is not silently imposed by this audit. Preserve zero-preference refusal and exclusion of preferences over evidence content. No replacement preference numbers are implied. [T,C,S] |
| F07 | Parameter prior/posterior and information gain have machine meaning | Row 11 finite registered-hypothesis module and Bayes/reference checks exist. | [ ] **PARTIAL:** actual model inputs and observations, refresh of registrations after A/model changes, actual epistemic consumer and evidence. Continuous Dirichlet claims cannot be inferred from a finite-hypothesis witness. [W11,W14,L] |
| F08 | `find` admits applicable real patterns with source-bound interpretation receipts | Opaque interface, F1–F4 laws and recorded-table certificates exist. Runtime `find_receipt` (`e8760f25`) and explicit receipt-mode construction (`24dc6068`, owner fixes `706ca3e8`/`983d4c49`) now exist; codex-27 reports independent acceptance. | [ ] **PARTIAL:** serving activation and an ordinary receipt-mode run with real source-bound interpretations and nonvacuous F2/F4 evidence. Production empty F4 designation is explicitly vacuous. Source-level integration is creditable; it is not recorded successful use. [T,L,S] |
| F09 | `organise` constructs the ruled attributed cascade change | Four-input F12RuledCarrier and Snatch formal exemplar exist. Runtime `bd803bc0`, admissions repair `24dc6068`, and receipt-mode call sites now exist; codex-27 reports independent acceptance. | [ ] **PARTIAL:** serving activation, ordinary constructed family and click receipt with preserved authored relations, nonvacuous no-bootstrap/precedence/acting evidence and exact downstream policy identity. Legacy `organise` still has a sorry; its replacement must be explicitly connected. [T,L] |
| F10 | G evaluates whole interpreted cascades using the AIF law | CascadeEFE and CascadeEFEPolicies define finite guarded transitions, horizon predictions, risk+conditional ambiguity, equivalent information-gain expression, and composition-sensitive controls. Old arbitrary Lean score is replaced by a canonical alias. | [ ] **OPEN:** runtime implementation of this definition, independent numeric correspondence, complete real candidate family, compatible C/model, and a selection that actually consumes these scores. Action/channel Gaussian controller scores do not discharge this. [T,S,L] |
| F11 | Candidate prior/posterior and choice preserve full policy identity | Mathematical admissible priors and some production-pinned policy/posterior witnesses exist. | [ ] **PARTIAL:** full cascade+precedence identity, declared prior/choice rule, applicable per-policy F and complete detail coverage, no mission-only identity collapse, and independent selected-to-enacted correspondence. Prior-only mathematics is not the observed-data posterior. [S,T,W16,W23] |
| F12 | Horizon, interaction, hierarchy and interpretation mean what the specification says | Shared-horizon mathematics and scoped composition/independence examples exist. | [ ] **OPEN:** legitimate current anticipation, effective horizon >=2 and FPI consumer evidence; fair common observation schedule; order/overlap and nested interpretation correspondence. No policy-length penalty from raw sums over different horizons. Declared extrapolations and constructed arithmetic are not established world dynamics. [S,H,T] |

## 3. Every node and every connection

This roster includes R1–R17, R19–R20, mediator R3a, TRACE and the catalogue's R17 variants. The current catalogue/control map has no R18 entry; omission is recorded, not a hidden completion award. R19 is retained even though it is absent from the generated control-stage list.

| Node | Current bounded accomplishment | Remaining completion obligation |
|---|---|---|
| R1 belief | Categorical state/carry and pinned correspondence records | [ ] **PARTIAL:** current real belief used by the same admitted model and subsequent selection. F02/F05. |
| R2 observation | Structured observations and retained update envelopes | [ ] **PARTIAL:** independent observed authority, currency, complete observation-to-update/learn joins. F04. |
| R3 update | Filter/update implementation and some pinned calculations | [ ] **PARTIAL:** real event/A/B/model-bound update witness and downstream effect; recorded initialization is not a measured update. |
| R3a prediction error | Projection source and measured consumer evidence | [ ] **PARTIAL:** current binding and source-to-consumer edges in the qualifying record; preserve hosting distinctions. |
| R4 forward model | Channel predictor and separate machine predictor modules | [ ] **PARTIAL:** F03–F05 on the actual cascade path, with shared dynamics. |
| R5 expected free energy | Canonical Lean cascade definition; standalone risk/categorical ambiguity arithmetic | [ ] **OPEN:** F06/F10 connected and witnessed in production. |
| R6 action/policy space | Retrieval, action ranking, posterior components; September 15 controller/ticket source changes | [ ] **PARTIAL:** F08–F11, complete candidate authority, actual choice and enactment. Ticket source handoff explicitly did not claim deployment. |
| R7 channel precision | Running mechanism and pinned equation witness | [ ] **PARTIAL:** current input/update/consumer chain and relevant gain applicability; do not confuse channel precision with policy precision. |
| R8 present fit / policy F | Equation components and F_pi witness machinery | [ ] **PARTIAL:** applicable live policy F/detail coverage and posterior use. Exact retired legacy scalar is not a whole-node exemption. |
| R9 independent certification | Refusing join checker, real-pair partial joins, typed anchor refusal | [ ] **OPEN:** authenticated genesis, retained exact review commissions, independent producer/reviewer binding and production consumer. Parking deployment does not satisfy this node. |
| R10 entrypoint | On-demand gated entrypoint; later commissioned dispatch evidence reported | [ ] **PARTIAL:** exact current R10 dispatch and bounded R2/predecessor joins and required lifecycle evidence. A functioning click is not full R10 assurance. No cron restoration is required by Item 2 amended. |
| R11 hierarchical budget | Standalone arbitration and some integration work; census admits absences | [ ] **OPEN:** actual ranked-field shared-budget restriction and consumer, independent lifecycle evidence. All seven required lifecycle cells remain obligations; reporting them absent does not complete R11. |
| R12 two-layer calibration | Catalogue describes internal and external calibration separately | [ ] **UNRESOLVED:** gather exact current producer/consumer and independently grounded external calibration evidence. Do not silently omit this node because the recent DAG focuses elsewhere. |
| R13 depth | Definition/reference witness and depth branch source | [ ] **OPEN:** legitimate effective depth >=2, actual branch computations and same-run continuations. A requested configuration is insufficient. |
| R14 commitment | Temperature/precision components and pinned witnesses | [ ] **PARTIAL:** applicable posterior/gain law, actual selection influence and R20 composition. Variational-mode insensitivity cannot be counted as successful modulation. |
| R15 hierarchy/timescale | Slow-state implementation and machinery feedback examples reported | [ ] **PARTIAL:** slow-state prior and selection influence, independent outcome-to-next-state feedback, full lifecycle acquisition; a phase change is not the entire hierarchical model. |
| R16 actuation, including realised cascade witness | Grounded machinery attempts, author/reviewer/close records and construction validation | [ ] **PARTIAL:** exact independently witnessed execution of the selected cascade, retained guarded interpretation, re-observed outcome and continuation. Comparing the selected input to itself at construction does not prove enactment. |
| R17 structure learning | Accumulation recurrence, anti-recount and model-reduction components | [ ] **PARTIAL:** actual observation-to-count-to-model-change-to-next-decision chain over real capability substrate; nonvacuous BMR evidence. |
| R17', R17'', R17''', R16→R6 | Exploration/generator/genesis/proposal mechanisms described in catalogue | [ ] **UNRESOLVED:** separately establish each real producer, independent admission, learned change and actual later consumer; generation alone is not validated structure learning. |
| R19 preferences | Terminal seed and channel C as F06 | [ ] **PARTIAL:** F06's compatible predictive bridge plus declared authorship, composition and revision provenance. |
| R20 self-monitoring | Live trips (including loaded-code drift), refusing checks and bounded Agency reliability receipts | [ ] **PARTIAL:** genuine-trip snapshot/history, applicable trip→R14 composition, discharge restoration and actual decision effect; catalogue-wide coverage not implied by narrow receipts. |
| TRACE and connections | Durable checkpoints, terminal records, source pins and some joined receipts | [ ] **PARTIAL:** complete current node/edge census, semantic identity and actual mandatory-edge firing in the same run. Include R6→R13→R14→R16→R2; reconcile conditional and retired edges and drawn/source differences. |

Newer September 15 source corrections: find `e8760f25`; organise `bd803bc0`; receipt construction `24dc6068` plus `706ca3e8`/`983d4c49`; preregistered pre/end measurement `cae5f6d9` (author reports 207 tests/1,228 assertions, independent acceptance not yet supplied). These supersede the older four-link trace at source level. No qualifying click evidence was supplied.

Node evidence: W rows 12–25, D's named prerequisites, T's current four-link call-site trace, L's proof audit, H's current configuration discovery, catalogue and generated control-stage roster. These references establish bounded facts and known gaps; the unresolved rows explicitly need more evidence before anyone signs completeness.

## 4. Whole-loop and paper obligations

| ID | Checklist item | State and required evidence |
|---|---|---|
| E01 | [ ] Machine-level validity, not just equation arithmetic | **OPEN:** show model inputs and policy semantics address the intended real work; bind implementation to those definitions. Finite fixtures and recorded tables alone cannot establish this. |
| E02 | [ ] Producer/consumer retention and independent acquisition | **PARTIAL:** checkpoints and emitters exist. Complete immutable census universes, evidence acquisition, freshness, exact commission preimages and independent semantic ownership remain required. |
| E03 | [ ] Complete rejecting certificate pipeline | **PARTIAL:** typed attestation/generator machinery exists. Complete F11 acquisition/adapter, source binding, semantic predicates, rejecting controls and end-to-end tamper tests. A complete record of failures is useful but is not positive validation. |
| E04 | [ ] Accepted F11/F2 reconciliation | **OPEN:** exact author→independent review→gates→grounded discharge evidence for row 27; do not inherit a repair packet's completion prose. |
| E05 | [ ] Qualifying timestamped run + Lean validation | **OPEN:** every required node and connection positively validated at the specified meaning, actual real work, retained inputs/outputs and independently computed certificate. Machinery r8 and annotation A8 cannot replace this. |
| E06 | [ ] All operational paper claims reconciled | **OPEN:** the 150-row completion inventory, including seven historically refuted claims, all unresolved/missing claims, current additions and deletions, matched to actual scoped evidence. Rewrite to facts; do not lower the build requirement by rewriting the promise away. |
| E07 | [ ] Cross-paper promises and historical evidence | **PARTIAL:** companion referent ruling exists. Pin actual delivered formal/experimental sections; locate or explicitly dispose of missing historical artifacts, correct stale proof counts and unsupported validation wording. |
| E08 | [ ] Wider purpose, including humans and goal formation | **OPEN:** evidence for all four apex requirements, including participant feedback and accountable formation of goals. The choice of an already supplied task is insufficient for goal formation. |
| E09 | [ ] Staged capability-zone/native-currency scope reconciled | **UNRESOLVED:** retain historical S1.5 acceptance, staged paper status and remaining load/demand/currency claims separately. No automatic rebuild, promotion or exclusion by presentation. |
| E10 | [ ] Closure, publication and agreement | **OPEN:** all required owners ratify this exact account; paper/board status points to the agreed revision. Joe retains acceptance of the qualifying fact. |

### Substitutions that do not discharge the specification

- An abstract Lean model in place of the machine's model and runtime correspondence.
- A six-constructor census in place of all defined, validated, working nodes.
- A controller's action score in place of G over interpreted cascades.
- Embedding retrieval or an unreceipted semilattice in place of find/organise.
- A truthful false certificate in place of a qualifying run.
- A machinery repair close or eligible observation annotation in place of all-node completion.
- Source implementation, a default flag, or an environment value in place of a production consumer and recorded use.
- A packet's independent review in place of ratification of this checklist.

## 5. Reconcile the previous lists without dropping obligations

**Published pages fetched on September 15:** the fetched plop HTML has **11** literal OUTSTANDING markers. Joe reported 13; the earlier DAG has 13 paper-note rows, two of which are HONEST ABSENCE/STAGED. They are not interchangeable counts or a closed specification. The pinned fetched bytes are in the audit evidence folder. The companion's board says READY and explicitly warns that admission is not run evidence; it mixes multiple populations and historical source states.

The 11 current PLoP occurrences, in document order, map as follows:

| Occurrence | Subject | This checklist |
|---|---|---|
| 1 | Whole-loop warrant, rows 24–28 | E02–E05 |
| 2 | Trip-to-commitment link | R20/R14 |
| 3 | Qualifying run | E05 |
| 4 | R9 anchor; surrounding R5/R10 caveats | R9, F04/F10, R10 |
| 5 | Effective horizon and detail configuration | F12, R8/R13 |
| 6 | Review-commission retention | R9, E02 |
| 7 | Cascade G, F13 | F08–F12 |
| 8 | Measured A and machine-Q activation | F03–F05, F10 |
| 9 | Applicable commitment composer | R14/R20 |
| 10 | Actual depth branch | R13 |
| 11 | Trip-to-commitment link repeated | R20/R14 |

**WORK-REMAINING 1–39:** 1–5 → E02/E03/TRACE; 6–11 → F01–F07; 12–16 → R1/R3/R6/R8/R17 and E01; 17–20 → R9–R12/R15/R20; 21–23 → TRACE/connections/R16/F11; 24–28 → E02–E05; 29–32 → E06/E07; 33 → E03/E05 convergence; 34 → F06/E09 outcome scope; 35 → F09; 36–38 → E02/E06/E10 dependency tickets/blocked view/census; 39 → F06/F12 C_tau. A closed bookkeeping row gives credit only for that bookkeeping.

**Outstanding DAG:** the adjacent coverage appendix preserves every node and its reported status, mapped to this checklist. Its status is a dated input, not today's completion verdict. In particular, `ruled-parked` R9 is still an open completion obligation. Proposed alternative scope branches are not adopted here.

**150 paper claims:** retain the entire source inventory as the detailed E06 checklist, including every P001–P150 ID and original evidence limits. Its original 14 validated / 48 exists-unvalidated / 21 absent-or-lost / 7 refuted / 60 unclassified tally is a historical snapshot, not a fresh September 15 validation count. No marginal-note search can replace that inventory. Current claims introduced since the snapshot must be reconciled during review.

## 6. Evidence index and audit limits

- **W:** [WORK-REMAINING](WORK-REMAINING.md). Mixed dates and bounded completion annotations; not accepted uncritically as current truth.
- **D:** [outstanding DAG](runs/outstanding-dag-2026-09-14/dag.json), including its source pins and acknowledged scope conflicts.
- **T:** [four-link source trace](TN-fundamentals-four-link-trace-2026-09-15.md), codex-27. Distinguishes source calls from serving code and dated clicks.
- **L:** [Lean audit](TN-lean-audit-2026-09-15.md), codex-26. Executed axiom checks at its snapshot, not a whole-tree current guarantee.
- **K:** [kernel repair discovery](TN-probability-kernel-repair-2026-09-15.md), plus current `mathlib4/DarkTower/WarMachine/Holes.lean`. Discovery's 'not implemented' status is historical; current source contains the repair.
- **S:** [cascade semantics](SPEC-cascade-policy-semantics-2026-09-15.md). Separates book authority, Joe's rulings, agent interpretations and extrapolations.
- **C:** [paper C contract](/home/joe/code/p4ng/sec-c-vector.tex).
- **H:** [configuration/depth discovery](TN-paper13-paper07-closure-path-2026-09-15.md).
- [Full paper claim inventory](COMPLETION-LIST-plop2026-2026-09-12.md).
- [Ticket source-only handoff](runs/ticket-selection-2026-09-15/IMPLEMENTATION.md).
- [Current PLoP page](https://zone.hyperreal.enterprises/wip/plop-2026.html) and [companion](https://zone.hyperreal.enterprises/wip/futon-2026.html); fetched snapshots are retained alongside the manifest.

Audit evidence, input hashes, dated page snapshots, coverage appendix and Agency response records live in `runs/fundamentals-checklist-2026-09-15/`. The hash ratified by agents is the checklist byte hash, recorded outside this file. Corrections require a new revision/hash and renewed assent to the changed content. Agent silence, queue acceptance, quota errors and previous packet approvals are not assent. Agent agreement cannot override Joe's specification.
