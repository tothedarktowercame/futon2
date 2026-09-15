# Cascade-policy semantics — packet 1a, 2026-09-15

**Status: statements with claude-20’s source-based resolutions in §4, subject to Joe’s correction; no Lean implementation authorized by this note.** Purpose: define AIF over pattern cascades, not retrofit the existing controller scores. Outcome space and preferences remain parameters. Packet 1b remains separately reviewed and depends on the shared ProbabilityKernel repair by codex-27; no parallel probability type is proposed.

## 1. Book authority and cascade interpretation

Read directly: Parr, Pezzulo & Friston, *Active Inference* (MIT Press, 2022), `/home/joe/downloads/book_9780262369978.pdf`, SHA-256 `2a98ab22310b168d07b76fd2dfcd625c55720255ad38fef052bc62e2502efb32`. Book page N is physical PDF page N+11. Equations 2.6 (p33/PDF44), 4.7–4.9 (pp72–73/PDF83–84) fix **one G**: ambiguity plus predictive outcome risk, equivalently negative information gain minus expected log preference. The finite future-step sum follows p33 and equation 4.10 (p74/PDF85). Policies are action sequences (p31/PDF42), indexing trajectories (p69/PDF80); A, D and policy-dependent B are defined by 4.5–4.6 (p71/PDF82). Cascade interpretation must supply these transitions, not a replacement scoring function.

Joe’s operator statement requires concretely constructed cascades over real patterns, their links and mission usage, and a G grounded in how those compositions are used—not a hand-picked graph or demo-tuned metric (`futon2/holes/M-G-over-cascades.md:19–49`). A single pattern is the degenerate case; independent per-pattern scores discard interactions (`:115–142`). The later design describes tension-discharge across scales (`:602–632`). Under the present book-based instruction this is potential application semantics for the model parameters, not an alternative definition of G.

A pattern has a guard `fires : State → Prop` and a partial consequent `then : State → Option Action` (`mathlib4/DarkTower/WarMachine/Holes.lean:24–27`). This is a production-rule interface, not yet stochastic transition semantics. The cascade record has nodes, authored additions, acyclic edges and precedence (`:30–36`). The glossary requires both sequential descent and cross-cutting co-application, with shared patterns belonging to overlapping sub-constructions (`p4ng/sec-glossary.tex:52`). Its current two-grain G(a)/engineering-S(π) wording (`:21,52`) does not settle Joe’s requested cascade-G meaning; this note does not edit the paper.

Organise constrains provenance and structure: O1 is the three-origin node union; O2 requires organised edges to have authored reachability; O3 is the ruled no-bootstrap fast-forward relation (`F12RuledCarrier.lean:25–36`; recorded predecessors `Holes.lean:1032–1051`). These are **not** stochastic causality or scheduling laws. In particular the Snatch exemplar has selected nodes `{0,2,10}`, added nodes `{7,11,18,22}`, precedence `[5,2,0,3,6,10,20]`, and acting order `[10,2,0]`, changing to `[20]` (`F12SnatchExemplar.lean:33–88`). Precedence therefore cannot silently become “execute every node once in this order.” The recorded changed integer score is not a derived EFE value.

**Interpretation boundary (resolved in §4):** retain pattern identities, descent, overlap and precedence. One pattern supplies one interpreted controlled operator; an absent interpretation is a typed policy-hole. Co-application means shared-state overlap, not simultaneity. One application is followed by one observation. Precedence orders eligible guarded rules; it is not an instruction to execute every listed node regardless of state.

**Factorization and hierarchy:** chapter 7, pp127,129–130 (PDF138,140–141), figure 7.3, separates hidden-state factors and controlled/uncontrolled transitions; Appendix B, pp244–245 (PDF255–256), permits tensor A. Section 4 resolves scopes through consumes/produces and requires the disjoint-factor commutation result to be proved. Figures 7.12–7.13, pp147–150 (PDF158–161), provide the recursive temporal interpretation; the specific downward D/C mapping is recorded in A5, including its extrapolation label.

**Multiple orderings (A3):** a policy is a (cascade, precedence) pair; distinct legal orders are distinct candidates, without schedule averaging or an arbitrary first-order default. Disjoint-scope swaps are not merged: operator commutation establishes final-state equality, not equality of a score summed after every application, because the intermediate states differ. Two orders are identified only where §3.7's per-step G-invariance is proved. No merge theorem is assumed by this specification.

## 2. Proposed finite generative signature and quantities

For finite nonempty state S and observation O, control domain U, finite horizon T, and a nonempty duplicate-free finite family Π of admissible **(pattern cascade, precedence) pairs**, use the repaired existing `ProbabilityKernel`. Parameters are observation kernel A, initial distribution D, controlled transition kernel B, preferences Cτ, and positive habit weights E on Π. S and O may be product spaces. No outcome alphabet, tension, curvature, or empirical parameter is fixed here.

The central interpretation maps each admitted pair π to `Bπ : ProbabilityKernel S S`: at s choose the first guard-enabled interpreted pattern p*(s) in its precedence and use B^p*(s); if none fires use identity, as A1/A4 specify. An uninterpreted pattern makes the cascade unscoreable. Guard evaluation on hidden state is explicitly EXTRAPOLATION 3, not a claim of runtime correspondence. Candidate pairs are constructed for a selected target. Horizon T is their declared length bound (pattern count or budget); nesting follows A5.

D is the initial-state prior; after observed data, prediction begins with declared current inferred beliefs q₀ (D only at initialization). One application precedes each scored observation: `qτ+1(s′|π) = Σs qτ(s|π) Bπ(s′|s)`, `Jτ(s,o|π) = qτ(s|π) A(o|s)`, `Qτ(o|π) = Σs Jτ(s,o|π)`, and τ=1,…,T. Thus the initial pre-control observation is not counted as a control consequence. Outcomes include target discharge; the concrete alphabet, distributions and inference algorithm remain parameters/separate work.

For each τ use **exactly** `PreferenceRiskSeparation.riskAdmissible Qτ Cτ` (`PreferenceRiskSeparation.lean:45–49`): positive predictive mass at zero preferred mass makes the finite score inadmissible; zero predictive mass contributes zero. Do not smooth away zeros. Proposed canonical quantity:

`Gτ(π) = KL(Qτ(·|π) || Cτ) + Σs qτ(s|π) H(A(·|s))`; `G(π) = Στ=1…T Gτ(π)`.

Define information gain from the same joint as mutual information `Iτ = Σs,o Jτ log[Jτ/(qτ Qτ)]`, using only positive-joint terms, and preference cost `Lτ = −Σo Qτ log Cτ(o)`, omitting zero-Q terms. The proposed decomposition is **Gτ = Lτ − Iτ**, not “KL risk minus information gain,” and not an assumption that information gain is negative ambiguity. Parameter-learning information gain is a different quantity and is not silently included.

**Policy beliefs and F:** use actual `Real.exp` and `Real.log`. Softmax normalizes over Π. The book distinguishes the base prior `σ(−G)` (4.7), habit prior `σ(log E−G)` (B.7, p246/PDF257), and habit posterior `σ(log E−F−G)` (B.9, p247/PDF258; without habit, 4.14, p75/PDF86). Precision prior `σ(−γG)` has γ>0 (B.13, p248/PDF259), where the book explicitly omits E. The combined `σ(log E−γG)` is a stated extension combining those prior factors, reducing to B.7 at γ=1 and B.13 at constant E; it is not labelled the observed-data posterior or a verbatim B.13 formula.

Fπ means the per-policy variational quantity `E_Q[log Q(s̃|π)−log P(õ,s̃|π)]` (B.2, p244/PDF255), with the mean-field specialization in 4.11 (p74/PDF85) and initial-state treatment B.4 (p245/PDF256). Computing/optimizing Fπ is deferred; `Holes.variationalFreeEnergy` is not imported as its substitute. Packet 1b proves future-policy prior normalization/order, not full posterior inference. A posterior formula accepting an arbitrary scalar F would not establish this deferred inference.

## 3. Acceptance theorem statements (no proofs in this packet)

1. **Well-formed predictions:** normalized nonnegative initial beliefs, Bπτ and A yield normalized nonnegative qτ, Jτ and Qτ at every τ≤T, with duplicate-free support and zero off-support mass preserved.
2. **Risk domain:** a positive Qτ mass at preferred zero contradicts the existing riskAdmissible predicate; zero-Q terms contribute zero. Under admission all terms in the displayed finite G are defined without substituted preferences.
3. **Derived decomposition:** under those domain conditions, for each admitted π,τ, `Gτ = Lτ − Iτ`, hence `G = Στ Lτ − Στ Iτ`. Derive from J and its marginals; do not take the equality or estimator agreement as a hypothesis.
4. **Information and score bounds:** Iτ≥0; with discrete finite A, ambiguity≥0 and KL≥0, hence Gτ≥0. These are categorical results, not differential-entropy claims.
5. **Policy prior:** finite nonempty Π, E positive on Π and γ>0 imply a positive partition function and normalized prior for the displayed base, habit and precision forms (and the explicitly combined extension). Equal habit weights and strictly smaller G imply strictly larger prior mass. This does not assert posterior ordering when F differs.
6. **Composition control:** construct a finite model and two admissible cascades containing the same patterns whose agreed composition semantics yield different Qτ and different summed G. Neither a precedence-indicator score nor a pasted G value qualifies. This is a mathematical example, not live evidence.
7. **No-interaction control:** construct a model under explicit independence/invariance conditions giving equal **per-step observable distributions and ambiguity contributions**, hence equal summed G, for the compared legal compositions. Commuting transitions alone imply neither this premise nor equal cumulative G; terminal equality alone is insufficient.
8. **Singleton:** a one-pattern, one-step interpretation yields its controlled one-step q₁/Q₁ and G₁, and the cascade sum equals that one-step score. Do not erase residual effects by claiming this for arbitrary T.

Packet 1b: no sorry, native_decide or new axioms; print every theorem’s actual axiom dependencies. After review, replace the old arbitrary-risk/eig cascadeGrainG and update its docstring/owed seam, leaving one canonical cascade-G definition. F13 runtime correspondence remains separate, not satisfied by these mathematical controls.

## §4. Resolved from Joe's records (claude-20, 2026-09-15); Joe may correct any of them.

### A1. ONE PATTERN = ONE CONTROL (an operator on the state).

Sources: Joe, `M-wm-policies.md:602–603`: patterns/policies are operators on mission/scope state-vectors and a cascade is a product O3 O2 O1 |psi>; `:590–591`: each pattern is itself a policy and policies compose recursively; `:550`: a next-step is a degenerate length-1 policy. `RULINGS-walkthrough-2026-09-07.md:309–316`: THEN statements admit attested, else documented, production-rule interpretations. `p4ng/app-snatch.tex:46–48,53,62`: IF and HOWEVER must both hold, otherwise the pattern has nothing to contribute; a THEN the model cannot carry emits no action.

Answer: each pattern p has guard g_p (IF ∧ HOWEVER) and controlled transition B^p from its attested (else documented) interpretation, recorded with provenance. No interpretation means a policy-hole: every containing cascade is not scoreable (typed missing input), never assigned an invented B. Temporally extended patterns use A5.

EXTRAPOLATION 1: when p's guard does not hold, p contributes the identity transition at that step (it "has nothing to contribute"), not a failure transition.

### A2. CO-APPLICATION = OVERLAP ON SHARED STATE, NOT SIMULTANEITY OR INDEPENDENCE.

Sources: Joe, `M-wm-policies.md:585–586`: policy is a semilattice of overlapping patterns, not a sequence/tree; `:584–585`: pattern_phylogeny is the prior, not the cascade. `p4ng/sec-glossary.tex:52`: one pattern belongs to several sub-constructions. `p4ng/sec-catalog.tex:345`: move interfaces consume If/However tokens and produce Then tokens. Book figure 7.3, pp129–130: factorized hidden states and policy-dependent transitions per factor.

Answer: a pattern's SCOPE is the state factors touched by its consumes/produces. Disjoint scopes act on separate factors; their operators commute and their joint transition is the product of factor transitions, with independence PROVED from disjoint scopes, not assumed. Co-applied patterns share a factor (a meet); their effect is the composed operator O2 O1, generally order-dependent. The co_app phylogeny belongs in habit prior E over cascades, not as an execution edge in B.

EXTRAPOLATION 2: identifying scope with the consumes/produces token sets of the R16 move interface.

### A3. PRECEDENCE IS PART OF THE POLICY; SEVERAL LEGAL ORDERS = SEVERAL POLICIES.

Sources: Joe, `P-validated-R5.md:227–235` (2026-08-30): temperament organizes direction of play, an operation on cascades which operate on other things. `p4ng/app-snatch.tex:113–131`: same patterns, changed precedence, G4 +3 → -5; conflict-resolution order commits the collection. `p4ng/sec-catalog.tex:345`: moves must chain onto the built frontier. Ratified Sortie 8, `F-wm-piloted-2026-06-12.md:275`: a cascade serializes unambiguously as a linear sequence of pattern applications.

Answer: a policy is a (cascade, precedence) pair. Legal precedences respect descent: a pattern acts only after what it stands on is built. Each legal precedence is a distinct policy with its own G; sigma(ln E − gamma G) chooses among them, without averaging or a first-order default. Precedences differing only by disjoint-scope swaps remain distinct policies: commutation (A2) gives equal final states, not equal G summed over every step, because the intermediate states differ. Two precedences are identified only where equal per-step G is proved (§3.7). (Corrected by claude-20 after codex-26's review; an earlier version of this answer claimed commutation sufficed.) Given precedence, serialization is unique (Sortie 8). The choice distribution here retains §2's distinction from an observed-data posterior containing F.

### A4. ONE STEP = ONE PATTERN APPLICATION THEN ONE OBSERVATION; THE POLICY IS A PRODUCTION-RULE SYSTEM, SO IT IS CONTINGENT.

Sources: `p4ng/app-snatch.tex:82–83,135–136`: order patterns with true antecedents by precedence and act on the first whose THEN yields action; one pattern acts per round. Joe, `P-validated-R5.md:340–351`: policy maps information state to action, including repository-wide knowledge and potentially finding the cascade; `:414–415`: select target then construct cascade. `p4ng/sec-catalog.tex:345`: discharge is fraction of target want-signature covered.

Answer: `B_pi(s′|s) = B^{p*(s)}(s′|s)`, where p*(s) is the first pattern in precedence whose guard holds; identity if none fires (the cascade has run out: absorbing). Prediction keeps `q_tau+1(s′|pi) = sum_s q_tau(s|pi) B_pi(s′|s)` and G sums risk plus ambiguity. T is the cascade length bound (pattern count or budget). Pi consists of constructed (cascade, precedence) pairs for the selected target (F7: two or more constructed cascades for one target). Outcomes include target discharge (want-signature coverage); C remains a parameter.

EXTRAPOLATION 3: in prediction the guard is evaluated on the hidden state s; the enacted policy evaluates it on the information state (Joe's type). The two coincide when guards read only observed facts; otherwise this is a stated approximation.

### A5. NESTING = DEEP TEMPORAL MODEL, RECURSIVE, NOT FIXED AT TWO LEVELS.

Sources: Joe, `M-wm-policies.md:590–591`: recursive policy composition; `M-G-over-cascades.md:575–579`: coherence across scales, no hardcoded two levels. `N-strategy-as-computational-object.md:203–209` (Joe verbatim): strategy, policy selection and policy form one coherent cascade. `P-validated-R5.md:414–415`: target first then construction. `p4ng/sec-catalog.tex:323` (R15): strategy fixes tactical target; witnessed outcomes update strategic calibration. Book figures 7.12–7.13, pp147–150: slower levels supply empirical priors, with a higher step spanning a lower sequence.

Answer: one step at a level is one completed episode of the cascade below. A higher level constrains the lower through (i) its state/choice setting lower initial D, the target |psi>, and (ii) its acting pattern's THEN setting lower success preferences C. The lower episode's discharged/not-discharged outcome is the higher observation. The definitions recur at every level. Policies creating policies (Canalize, `M-wm-policies.md:621–623`) are outside packet 1b.

EXTRAPOLATION 4: (ii) follows N-strategy line 237-238 "each level's THEN the level below's IF", which is agent text in that note, not Joe's words.

Parameters remain parameters: A, each B^p from attested interpretations, C, D, E, and state factorization. Their War Machine producers are the separate F13 correspondence work. This section records claude-20's resolutions; it does not establish the theorem targets or authorize Lean implementation by itself.
