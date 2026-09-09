# E-C-realization: comparing achievements and intermediate lifecycle criteria

2026-09-09, Joe with codex-12. Extends the
[first local calculation](SESSION-C-local-achievement-2026-09-09.md).
This is a comparative survey with executable probes, not approval of a
numerical preference family or a declaration that four clusters are complete.

## Joe's direction, verbatim

> So why don't we try the same thing for, say, each of the completed mission clusters and see if we can find a family. Of different. Preferences related to possibly different clusters or just kind of see... If. What you've done for that one mission, Can be done again a few more times. And then we can see... Whether there's an overall... Way to think about this in terms of I'll see you next time. See. Here also noting that the analysis on the mission level... Looks at completion criteria for the mission, but it doesn't yet look at the process or C sub tau. Criteria that go into making it so that that particular mission would be completed successfully. So we also need to develop some analyses of that nature. And for that, I would suggest we look closely at the Futon 4 Holds Mission Lifecycle document, because any mission... We'd have a set of Intermediate criteria. That are related to... That life cycle. And that's not to say that this is all the preferences that exist or all the ones that we could find, but it's a fairly well defined set.

“Futon 4 Holds Mission Lifecycle” resolves to `futon4/holes/mission-lifecycle.md`,
the existing lifecycle already referenced in this sitting. No definite decoding
is assigned to “I'll see you next time. See.” The instruction to compare more
examples and their intermediate criteria is clear in the surrounding text.

## 1. Coverage and evidence boundaries

The U88 pin groups 19 members under four problem clusters. Membership is not a
closure certificate. The historical W1 survey explicitly records problem-only
members and partial implementations; it cannot establish that every cluster is
complete. The examples below distinguish a completed mission, an achieved part,
and an unfulfilled claim.

| Cluster orientation | Historical example examined | Status established by its record | Qualitative desideratum recovered |
|---|---|---|---|
| A: records carry warrant | M-first-flights Phase A | Operator-accepted Phase A; later work transferred | A measurement carries grounds sufficient for its consumer to admit or reject it. |
| B: one queryable self-account | M-self-representing-stack, completed through M-three-column-stack; represented in the closed capability-star-map mission | Mission records COMPLETE on 2026-03-04; star map records capability and prerequisites | A reader can follow relationships between aspirations, code and evidence, and find the concrete source of a discrepancy. |
| C: feedback reaches participants | M-aif-head, linked by the pinned leaf-cycle story to understanding feeding back into argument | Mission records COMPLETE on 2026-03-15; story asserts the step is closed | Evidence about what happened can be compared with what was intended and can inform a subsequent judgment. This does not establish that every human/stakeholder receives or learns from it. |
| D: accountable next-action choice | M-aif-policy-conditioned-eig | Arithmetic component implemented; generative contract explicitly open | A choice's claimed information value follows from coherent predictions and updates, rather than from current uncertainty alone. |

A, C and D follow named members in the pin/W1 survey. B is an analyst-selected
capability example related to the pinned queryable-self-account problem, not a
newly authored cluster edge. The star-map mission itself pairs with
M-stack-stereolithography, one of B's source problems. No library relation was
written by this analysis.

## 2. Three further local calculations

[Runner](runs/C-realization-family/local_calculations.clj),
[results](runs/C-realization-family/result.edn),
[source pins](runs/C-realization-family/source-pins.edn).
Run from **futon3c**, in a standalone process:

```sh
clojure -M ../futon2/holes/labs/wm-contract/runs/C-realization-family/local_calculations.clj
```

The runner checks source digests before loading calculation modules and again
before reporting. It calls existing pure functions. It does not start a service,
write to the running system, or replay the historical deployments.

### B — prerequisites for a usable self-account

The historical graph `futon0/holes/missions/M-capability-star-map.graph.edn`
records M-self-representing-stack's scope as `[:agency :evidence-persistence]`.
Both prerequisites have recorded `:satisfied` status. The existing
`efe/mission-applicable?` returns **true**. In a labelled counterfactual changing
only evidence-persistence to `:held`, it returns **false**. The antecedent is
nonempty, so this is not vacuous applicability.

The capability node itself records a six-gate trace from devmap through evidence
to source code. Its completed mission records the functional achievement:
aspirational and actual self-images became navigable relationships rather than
separate documents (`futon4/holes/missions/M-self-representing-stack.md:11-51`).
The closure describes durable three-column ingestion and a browsable
violation→resolution loop (`:1235-1302`). These are historical reports, not fresh
live attestations.

What was computed now is a **process precondition**, not mission completion:
the recorded prerequisites support starting this mission; withdrawing a required
capability removes that support. Applicability does not certify the store is
currently durable or that a live query traverses all columns. The family member
suggested by the record is “an account whose connections remain available and
inspectable”, with persistence as one enabling condition.

### C — a proposed feedback satisfaction reading fails a semantic control

M-aif-head's criterion 2 calls for comparing `:propose` with `:execute`
(`futon2/holes/M-aif-head.md:168-171`). Its completed implementation includes
`mission_shapes/compute-prediction-divergence:270`. MAP had found that a narrative
proposal and a list of actual file paths could not be compared (`:368-377`);
DERIVE introduced structured predicted artifacts and success criteria (`:635-670`).

Using explicit **synthetic semantic probes**, not historical flight records:

| Proposed criterion: “feedback delivered” | Divergence returned |
|---|---:|
| Report exists; validation says “feedback delivered” | 0.0 |
| Same report; validation says “feedback delivered: false” | 0.0 |
| No report or validation evidence | 1.0 |
| No declared prediction or criteria | nil |

The code recognizes criterion text as a substring. Consequently, its zero is
not proof that the criterion holds. The arithmetic for artifact mismatch exists;
the intended semantic satisfaction reading does not follow. This blocks using
this number as evidence that feedback succeeded. No reinterpretation, threshold
or parser repair was introduced to make this example pass.

The pinned story's stronger claim also needs care: its named
`futon2/src/futon2/aif/head.clj` path is absent in the current checkout. Historical
closure prose is not a current consumer trace. This example supplies a concrete
negative finding and process-history evidence; it supplies no positive measurement
of human learning and no new cluster-C channel.

### D — accountable information claims

The implemented `epistemic-value/expected-information-gain` is exercised with
the already-existing reduction examples from `epistemic_value_test.clj`:

| Declared model fixture | Recomputed EIG |
|---|---:|
| Binary observation perfectly identifies a 50/50 hidden state | 0.6931471805599453 nats |
| Observation leaves the prior unchanged | 0.0 nats |
| Predicted posterior mixture does not reconstruct prior | Refused |

The 50/50 prior is part of an existing synthetic reduction fixture, not inferred
from historical outcomes. The useful achievement is that arbitrary posterior
maps cannot manufacture claimed information gain: the calculation requires a
coherent model. The mission itself explicitly limits this to arithmetic and
keeps policy observation models and prospective calibration open
(`M-aif-policy-conditioned-eig.md:14-48, criteria C1–C11`).

This is an **epistemic G calculation**, not a C distribution. The associated
process requirement is that forecasts used to justify action are accountable to
their assumptions and evidence. Putting “maximize EIG” into C would conflate
the two quantities; this analysis makes no such move.

## 3. Lifecycle criteria as intermediate desired states

The lifecycle is a well-defined source of process requirements, as Joe suggests.
[lifecycle-criteria.json](runs/C-realization-family/lifecycle-criteria.json)
contains the **verbatim exit conditions**, line ranges and source digest for the
seven phases plus optional HEAD. Extraction does not certify any mission passed
them. The following table is an analytical reading of those requirements.

| Phase | Intermediate state the lifecycle asks for | Why it matters to eventual satisfaction | Observation / evidence form |
|---|---|---|---|
| HEAD, optional | Operator recognizes the account of the problem and its unresolved tensions | Prevents solving an agent-invented substitute problem | Recorded operator recognition; absence stays unknown |
| IDENTIFY | A human agrees the gap and scope; completion is described testably | Gives later work an agreed object and a way to recognize success | Proposal, criterion declarations, human review |
| MAP | Survey questions answered; existing and missing material distinguished | Prevents designing on imagined infrastructure or duplicating available capability | Inventory with source evidence and ready/missing table |
| DERIVE | Types, relations, rules and data flow specify an implementable design | Makes the route to the result explicit enough to build and challenge | Design with rationale and applicable contracts |
| ARGUE | Reasons and trade-offs explain why the design fits; plain-language account works | Exposes unsupported choices and lets participants assess the direction | Argument tied to constraints, patterns and recorded rulings |
| VERIFY | Constraints and risks checked; needed design revisions recorded | Lets a failed prerequisite change the design before implementation | Structural checks, adversarial cases, prototypes and decisions |
| INSTANTIATE | Each completion criterion demonstrated; result reproducible | Converts promised functionality into witnessed functionality | Executable result, run/round-trip evidence and demonstrations |
| DOCUMENT | Others can discover the capability without knowing the mission existed | Makes a local achievement available to later people and work | Navigable documentation, cross-references and discovery evidence |

These are not a preference for collecting documents or maximizing passed checks.
The desired intermediate properties are agreement, adequate knowledge,
implementability, justified choice, detected defects, witnessed functionality,
and discoverability. The listed artifacts are ways to examine those properties;
their mere existence is insufficient.

## 4. Process histories behind the examples

| Example | Recorded intermediate dependency / correction | What this contributes to a Cτ analysis |
|---|---|---|
| A: first flights | IDENTIFY separates rendering debt, substrate debt and return-channel debt; DERIVE draws requirements from the pilot's actual sequence; VERIFY first uses a narrative/mockup with illustrative values marked; logic checks then challenge the witness; INSTANTIATE includes calibration consumption and Joe's side-by-side review. | Prefer progressing through states where the next claim can be justified. A visually better record was insufficient until a consumer could use the evidence. Sources: mission §§1, 3, 5 and exits 1–6; especially `:392-411`, `:497-533`, `:782-804`, `:896-913`. |
| B: self-account / three columns | An earlier proof of concept was reopened after persistence, coverage and invariant gaps were found. Three-column VERIFY checks round-trip per column, idempotency, and visible violations. INSTANTIATE demonstrates detecting and resolving a particular violation. | “More entities” is inadequate: persistence, relationships and a usable correction loop are successive requirements. Reopening a supposedly completed result can be effective progress. Sources: M-three-column-stack motivation and `:1005-1043`, `:1045-1141`; M-self-representing-stack closure. |
| C: AIF head | MAP identifies incompatible proposal/execution shapes. DERIVE adds comparable fields. VERIFY's C9 pre-check discovers that head coverage was only implicit and calls for a new enumeration function; the implementation table subsequently includes H-9. | A useful process state is one where an omitted obligation is exposed and returned to design, not concealed by an all-green summary. Current substring probe shows that historical implementation completion does not end verification of stronger meanings. Sources: M-aif-head `:368-377`, `:635-670`, `:1623-1646`, §6. |
| D: policy EIG | IDENTIFY distinguishes formal/model-relative from empirical/world-relative warrant. Completion criteria require alphabets, normalized distributions, shared updates, coherent mixtures, policy differences and prospective calibration. Arithmetic is implemented while the larger contract remains open. | Local mathematical success must leave missing empirical warrants visible. A later phase is not justified merely because an earlier numerical component works. Sources: mission §1, two warrants, C1–C11 and §2 missing join. |

The completed missions have different documentation structures and dates. This
comparison maps their recorded work to today's lifecycle retrospectively; it
does not invent historical phase approvals or impose later rules as though they
were present at the time.

## 5. What family is beginning to appear?

Two axes are now useful to distinguish:

1. **What capability/outcome is sought?** Warranted measurement; a navigable
   self-account; feedback that changes a justified judgment; accountable action
   evaluation. These overlap across clusters rather than partitioning them.
2. **What must hold while pursuing it?** Agreed purpose, adequate survey,
   explicit design, justified choices, responsive verification, demonstrated
   behavior and discoverability. The lifecycle supplies recurring intermediate
   requirements across different missions.

For discussion, one could index a preference family by mission, outcome facet
and time, with lifecycle state as context. This is notation to investigate, not
a ruled distribution. A phase is not automatically a time τ: phases can repeat,
VERIFY can return to DERIVE, and an outcome can persist across phases. Nor does
completion of lifecycle stages alone logically guarantee the mission's result.

The next mathematical obligation is to connect **these observed/desired states**
to **policy-conditioned transitions between them**. We have several local
predicates and calculations, not yet that shared predictive model. Missing
measurement is distinct from zero satisfaction; both are distinct from zero
preference mass. No missing carrier in C has been filled by relabelling evidence.

## 6. Validation and scope

The new runner's assertions passed for B's nonvacuous prerequisite contrast,
C's semantic non-discrimination and nil for absent predictions, and D's two numerical
reductions and incoherent-model refusal. The saved EDN was reproduced in a
second standalone process. clj-kondo: 0 errors/0 warnings; check-parens: OK on
the new Clojure/EDN. Lifecycle excerpt contents and SHA were checked against
the source; Markdown whitespace checked. A's prior replay is linked, not
represented as a new run here. No shared JVM changes, deployment, preference
weights, library edges, registry entries or worklist rows.
