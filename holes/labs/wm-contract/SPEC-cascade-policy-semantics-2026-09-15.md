# Cascade-policy semantics — packet 1a, 2026-09-15

**Status: proposed statements for claude-20’s review and Joe’s concept decisions; no Lean implementation authorized by this note.** Purpose: define AIF over pattern cascades, not retrofit the existing controller scores. Outcome space and preferences remain parameters. Packet 1b waits for the decisions below and the shared ProbabilityKernel repair by codex-27; no parallel probability type is proposed.

## 1. What the sources require, and what they do not yet define

Joe’s operator statement requires concretely constructed cascades over real patterns, their links and mission usage, and a G grounded in how those compositions are used—not a hand-picked graph or demo-tuned metric (`futon2/holes/M-G-over-cascades.md:19–49`). A single pattern is the degenerate case; independent per-pattern scores discard interactions (`:115–142`). The later design calls G “expected tension-discharge across scales” and distinguishes a tension field from discharge along a path (`:602–632`). This is a substantive intention, not permission to rename an arbitrary scalar G.

A pattern has a guard `fires : State → Prop` and a partial consequent `then : State → Option Action` (`mathlib4/DarkTower/WarMachine/Holes.lean:24–27`). This is a production-rule interface, not yet stochastic transition semantics. The cascade record has nodes, authored additions, acyclic edges and precedence (`:30–36`). The glossary requires both sequential descent and cross-cutting co-application, with shared patterns belonging to overlapping sub-constructions (`p4ng/sec-glossary.tex:52`). Its current two-grain G(a)/engineering-S(π) wording (`:21,52`) does not settle Joe’s requested cascade-G meaning; this note does not edit the paper.

Organise constrains provenance and structure: O1 is the three-origin node union; O2 requires organised edges to have authored reachability; O3 is the ruled no-bootstrap fast-forward relation (`F12RuledCarrier.lean:25–36`; recorded predecessors `Holes.lean:1032–1051`). These are **not** stochastic causality or scheduling laws. In particular the Snatch exemplar has selected nodes `{0,2,10}`, added nodes `{7,11,18,22}`, precedence `[5,2,0,3,6,10,20]`, and acting order `[10,2,0]`, changing to `[20]` (`F12SnatchExemplar.lean:33–88`). Precedence therefore cannot silently become “execute every node once in this order.” The recorded changed integer score is not a derived EFE value.

**Proposed interpretation boundary:** retain pattern identities, sequential dependencies, co-application membership/shared occurrences and precedence as distinct data. An admissible interpretation supplies finite-step control bundles, with a joint meaning for co-applied rules and an explicit rule for guard failure, conflicts, shared effects and termination. Sequential dependencies constrain eligibility; co-application is interpreted jointly, not arbitrarily serialized. Its mapping from the existing cascade/organise result must be specified and checked, not assumed from field names. The current bare `Cascade` does not supply all this data.

**Multiple orderings:** until a rule is fixed, an underspecified cascade has no unique numeric G. Possible semantics are a declared schedule, a declared distribution over legal schedules, or order-invariance proved for the relevant observable process. Do not choose the first ordering, minimize over orderings, or invent uniform weights. If schedule uncertainty is marginalized, its joint distribution must determine Q before scoring; generally G of that mixture is not the average of schedule-specific G because KL is nonlinear.

## 2. Proposed finite generative signature and quantities

For finite nonempty latent state S and observation O, finite control domain U, horizon T, and a finite nonempty duplicate-free family Π of admissible **pattern cascades**, use the repaired existing `ProbabilityKernel`. Require:

- initial belief `q₀ : ProbabilityKernel Unit S`;
- observation kernel `A : ProbabilityKernel S O`;
- controlled transition `B : ProbabilityKernel (S × U) S`;
- reviewed cascade interpretation `K(π,τ) : ProbabilityKernel S U`, including any explicit scheduler state in S; K must come from the agreed production-rule/bundle semantics, not arbitrary score functions;
- preferences `Cτ : ProbabilityKernel Unit O`, policy prior `E : ProbabilityKernel Unit Π`, and precision γ > 0, with E(π) > 0 on the admitted policy family.

Whether controls may depend on hidden state, observed history or a fixed plan is a question below; the general K signature does not claim the live policy has hidden-state access. The notation τ=1,…,T means transition then observation; that convention too needs acceptance. No tension alphabet, curvature estimator or discharge preference is hardcoded.

Define by finite marginalization:

`qτ(s′|π) = Σs,u qτ−1(s|π) K(π,τ)(u|s) B(s′|s,u)`;
`Jτ(s,o|π) = qτ(s|π) A(o|s)`; `Qτ(o|π) = Σs Jτ(s,o|π)`.

For each τ use **exactly** `PreferenceRiskSeparation.riskAdmissible Qτ Cτ` (`PreferenceRiskSeparation.lean:45–49`): positive predictive mass at zero preferred mass makes the finite score inadmissible; zero predictive mass contributes zero. Do not smooth away zeros. Proposed canonical quantity:

`Gτ(π) = KL(Qτ(·|π) || Cτ) + Σs qτ(s|π) H(A(·|s))`; `G(π) = Στ=1…T Gτ(π)`.

Define information gain from the same joint as mutual information `Iτ = Σs,o Jτ log[Jτ/(qτ Qτ)]`, using only positive-joint terms, and preference cost `Lτ = −Σo Qτ log Cτ(o)`, omitting zero-Q terms. The proposed decomposition is **Gτ = Lτ − Iτ**, not “KL risk minus information gain,” and not an assumption that information gain is negative ambiguity. Parameter-learning information gain is a different quantity and is not silently included.

Proposed posterior: `Qpolicy(π) = exp(log E(π) − γ G(π))/Σρ∈Π exp(log E(ρ) − γ G(ρ))`, using Real.exp/log. This is the packet’s future-score posterior; its relationship to the existing observed-policy Fπ term remains explicit question 7 below.

## 3. Acceptance theorem statements (no proofs in this packet)

1. **Well-formed predictions:** normalized nonnegative q₀, K, B and A yield normalized nonnegative qτ, Jτ and Qτ at every τ≤T, with duplicate-free support and zero off-support mass preserved.
2. **Risk domain:** a positive Qτ mass at preferred zero contradicts the existing riskAdmissible predicate; zero-Q terms contribute zero. Under admission all terms in the displayed finite G are defined without substituted preferences.
3. **Derived decomposition:** under those domain conditions, for each admitted π,τ, `Gτ = Lτ − Iτ`, hence `G = Στ Lτ − Στ Iτ`. Derive from J and its marginals; do not take the equality or estimator agreement as a hypothesis.
4. **Information and score bounds:** Iτ≥0; with discrete finite A, ambiguity≥0 and KL≥0, hence Gτ≥0. These are categorical results, not differential-entropy claims.
5. **Posterior:** finite nonempty Π, E positive on Π and γ>0 imply a positive partition function and normalized policy posterior. Equal priors and strictly smaller G imply strictly larger posterior mass.
6. **Composition control:** construct a finite model and two admissible cascades containing the same patterns whose agreed composition semantics yield different Qτ and different summed G. Neither a precedence-indicator score nor a pasted G value qualifies. This is a mathematical example, not live evidence.
7. **No-interaction control:** construct a model under explicit independence/invariance conditions giving equal **per-step observable distributions and ambiguity contributions**, hence equal summed G, for the compared legal compositions. Commuting transitions alone imply neither this premise nor equal cumulative G; terminal equality alone is insufficient.
8. **Singleton:** a one-pattern, one-step interpretation yields its controlled one-step q₁/Q₁ and G₁, and the cascade sum equals that one-step score. Do not erase residual effects by claiming this for arbitrary T.

Packet 1b: no sorry, native_decide or new axioms; print every theorem’s actual axiom dependencies. After review, replace the old arbitrary-risk/eig cascadeGrainG and update its docstring/owed seam, leaving one canonical cascade-G definition. F13 runtime correspondence remains separate, not satisfied by these mathematical controls.

## 4. Questions for Joe — unresolved, not implementation defaults

1. **Meaning/sign of G:** is expected cross-scale tension-discharge represented through outcomes/preferences in the canonical G above, or is a distinct path functional intended? How does beneficial discharge relate to minimizing G? What bridge is required? Keeping O/C open preserves this question, not its answer.
2. **Composition carrier:** what exactly is a co-application—simultaneous rule application, shared construction constraints, or another semantics? What are occurrence identity, shared-effect counting and conflict rules? Which organised edges express temporal causality rather than authored justification?
3. **Precedence/order:** is precedence a preference over applicable patterns, a hard constraint, or a scheduler? Which of fixed schedule, declared stochastic schedule, or proved invariance handles multiple legal orders? Are repeated applications allowed?
4. **Production rules:** what happens when fires is false, then is none, multiple rules compete, or a bundle cannot compose? Which supplied transition represents a successful rule and which represents failure? No no-op/refusal branch is selected here.
5. **Time and information:** what is a step (pattern, joint bundle, completed fold, or observation boundary)? Fixed horizon or completion with an explicit terminal convention? Is feedback allowed; which information can determine controls? Is undiscounted Στ the intended objective, or is a justified scale/time weighting required?
6. **Outcomes/preferences:** what observations and state summarize the whole affected cascade/mission/stack, including displacement of tension across regions/scales? Who supplies C and how does the declared discharge interpretation determine it? No alphabet or empirical parameter is chosen here.
7. **Observed-policy evidence:** should the proposed prior E already incorporate past evidence, or must the posterior separately include the existing Fπ term? The requested `log E − γG` formula must not silently retire `−Fπ` or count the same evidence twice.

These questions determine the model. Claude-20 reviews and routes them to Joe before packet 1b; no paper edits, machine mapping, model-parameter acquisition or registry changes are part of 1a.
