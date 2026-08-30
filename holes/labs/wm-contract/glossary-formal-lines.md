# glossary-formal-lines

Quoted S1 requirements: “every one of the 33 entries carries: (1) its Gate-0 class … (2) … a `Formal:` line … (3) its **Markov-category rendering** … (4) the **Lean binding** …” and “an entry whose `Formal:` line is a restatement of its prose (no new symbol, no constraint) is not formal and must be marked so; an entry whose formal line contradicts the code (`P-validated-R5` §2b: three unrelated `G`s) reports the contradiction rather than choosing.” (`futon2/holes/problems/P-glossary-mathematics.md:29-43`)

Observed with: `perl -ne 'while(/\\paragraph\\{([^}]*)\\}/g){...}' p4ng/sec-glossary.tex`; `sed -n` reads of the glossary/problem worksheets; `git -C /home/joe/code/mathlib4 show 6fd8a33f:DarkTower/WarMachine/Holes.lean | nl -ba`.

### Active Inference Framework [class: theory-defined]
Formal: AIF := (P(o,s,\pi), Q(s,\pi\mid o), F, G) with present-fit `F = E_Q[\ln Q - \ln P]` and action scoring separated into later entries; the paper's WM use is an operational harness over that tuple, not a brain claim. (anchor: `p4ng/sec-glossary.tex:5`; `futon2/holes/problems/P-glossary-mathematics.md:29-39`)
Markov: composite of observation, transition, and selection morphisms; at loop scale this is the “composite morphism `State → D(State)` per tick,” still unstated. (anchor: `futon2/holes/problems/P-markov-category-spec.md:36-50`)
Lean: missing: `structure ActiveInferenceModel`.
Notes: observed: the glossary operationalises the theory for WM (`p4ng/sec-glossary.tex:5`); inferred, untested: the exact WM tuple still needs the carrier decisions blocked in `Outcome` and `Policy` (`P-validated-R5.md:217-221`).

### Generative model [class: theory-defined]
Formal: a generative model is a joint law `P(o,s,\pi)=P(o\mid s)P(s\mid \pi)P(\pi)` or, at minimum for WM, the observation kernel `A=P(o\mid s)` plus transition/policy components later read by `G`. (anchor: `p4ng/sec-glossary.tex:7`; `futon2/holes/problems/P-glossary-mathematics.md:31-39`)
Markov: `A : S ⇝ O`; transition `B : S×U ⇝ S`; policy prior `E : 1 ⇝ Π`; the loop uses only parts of this today. (anchor: `futon2/holes/problems/P-markov-category-spec.md:42-50`)
Lean: missing: `def GenerativeModel`.
Notes: observed: the glossary names hidden causes and observations but no existing `Holes.lean` declaration yet binds them (`p4ng/sec-glossary.tex:7`; `mathlib4:6fd8a33f:DarkTower/WarMachine/Holes.lean:21-98`).

### Belief state $\mu$ [class: stack-defined]
Formal: `\mu : Channel → (mean × variance)` with variance updated as an EMA of squared miss plus sensor floor `\sigma^2_sensor`; this is a typed state, not yet a canonical variational density. (anchor: `p4ng/sec-glossary.tex:9`; `futon2/docs/futon-aif-completeness.md:82-99`)
Markov: none: stack vocabulary until `\mu` is connected to a declared state object and Bayesian inversion. (anchor: `futon2/holes/problems/P-markov-category-spec.md:45-46,148-149`)
Lean: missing: `structure BeliefState`.
Notes: observed: the glossary and completeness doc agree that WM stores operational hypotheses with variance, but `Holes.lean` has no `BeliefState` declaration (`p4ng/sec-glossary.tex:9`; `P-validated-R5.md:214-220`).

### Observation vector $o$ [class: stack-defined]
Formal: `o : Channel → [0,1]` over the declared channel list, with well-formedness `∀ c, o(c).isSome ↔ c ∈ declared`; the machine's current declared arity is 14, not 13. (anchor: `p4ng/sec-glossary.tex:12`; `mathlib4:6fd8a33f:DarkTower/WarMachine/Holes.lean:325-343`)
Markov: none: stack vocabulary until a declared observation object `O` and observation kernel `A : S ⇝ O` are fixed. (anchor: `futon2/holes/problems/P-markov-category-spec.md:44-46`)
Lean: binds to `r2WellFormed`, `R2Tick`, `R2TickLit`. (`mathlib4:6fd8a33f:DarkTower/WarMachine/Holes.lean:322-343`)
Notes: observed: the glossary says “typed observations,” and the historical Lean file already fixes the declared-channel discipline (`p4ng/sec-glossary.tex:12`; `mathlib4:6fd8a33f:DarkTower/WarMachine/Holes.lean:325-343`).

### Prediction error $\varepsilon$ [class: theory-defined]
Formal: `\varepsilon_k := o_k - \mu_k`; belief correction is `\mu ← \mu + \alpha \Pi \varepsilon`. (anchor: `p4ng/sec-glossary.tex:15`; `futon2/holes/problems/P-glossary-mathematics.md:33`)
Markov: none yet; if R3 is stated categorically, this is part of Bayesian inversion of the observation kernel rather than a standalone morphism. (anchor: `futon2/holes/problems/P-markov-category-spec.md:45-46,148-149`)
Lean: missing: `def predictionError`.
Notes: observed: the formula is canonical in the glossary, but no `Holes.lean` declaration names it (`p4ng/sec-glossary.tex:15`; `mathlib4:6fd8a33f:DarkTower/WarMachine/Holes.lean:345-358`).

### Precision $\Pi$ [class: theory-defined]
Formal: `\Pi_k := 1 / \max(\operatorname{Var}_k, \varepsilon_0)` as inverse variance weighting on error channels. (anchor: `p4ng/sec-glossary.tex:17`; `futon2/docs/futon-aif-completeness.md:189-199`)
Markov: none directly; in the categorical reading it parameterises kernels or conditionals but is not itself a separate morphism. (anchor: `futon2/holes/problems/P-markov-category-spec.md:39-50`)
Lean: missing: `def PrecisionMap`.
Notes: observed: the glossary and completeness contract both treat precision as the theory-defined weight, while `Holes.lean` only mentions `precisionState` as fixture scaffolding inside `R8Tick` (`p4ng/sec-glossary.tex:17`; `mathlib4:6fd8a33f:DarkTower/WarMachine/Holes.lean:350-357`).

### Variational free energy $F$ [class: theory-defined]
Formal: `F = 1/2 · mean_k (\Pi_k \varepsilon_k^2)`; this is the Laplace/Gaussian present-fit scalar. (anchor: `p4ng/sec-glossary.tex:19`; `futon2/holes/problems/P-glossary-mathematics.md:33-34`)
Markov: none at current WM grain; the categorical bridge would attach `F` to a declared generative model, not just to stored channel errors. (anchor: `futon2/holes/problems/P-markov-category-spec.md:118-122`)
Lean: binds to `R8Tick.storedF`, `r8Disposition`, `r8Census`; missing: `def variationalFreeEnergy` for the formula itself. (`mathlib4:6fd8a33f:DarkTower/WarMachine/Holes.lean:345-360`)
Notes: observed: the historical Lean fixture tracks stored `F` but does not define the equation; the worksheet requirement is therefore real, not duplicated (`R8-glossary-formalisation.md:21-36`; `mathlib4:6fd8a33f:DarkTower/WarMachine/Holes.lean:350-360`).

### Expected free energy $G$ [class: theory-defined]
Formal: `G_efe(a) = D_KL[Q(o\mid a) \Vert C] + E H[P(o\mid s)]`; for policies the canonical target is `G(\pi)`, but WM today also has `Holes.G := risk - eig`, so the formal line must report the grain mismatch rather than collapse them. (anchor: `p4ng/sec-glossary.tex:21`; `mathlib4:6fd8a33f:DarkTower/WarMachine/Holes.lean:68-83`)
Markov: risk and ambiguity are functionals of a kernel `Q(o\mid \pi)`; the composition is declared as the eventual Kleisli composite along the cascade. (anchor: `futon2/holes/problems/P-markov-category-spec.md:46-49,71-85`)
Lean: binds partially to `G` and `nonDegenerate`; missing: `def expectedFreeEnergyKernel`. (`mathlib4:6fd8a33f:DarkTower/WarMachine/Holes.lean:68-98`)
Notes: observed: contradiction reported, not resolved: the glossary gives `risk + ambiguity` over `a`, while `Holes.G` is `risk - eig` over a generic `Policy`; `P-glossary-mathematics` explicitly requires reporting this kind of clash (`P-glossary-mathematics.md:41-43`; `P-validated-R5.md:221-245`).

### Risk [class: theory-defined]
Formal: `risk(\pi) := D_KL[Q(o\mid \pi) \Vert C]`; at current WM channel grain this is a summed KL against per-channel preference objects. (anchor: `p4ng/sec-glossary.tex:23`; `R5-glossary-formalisation.md:21-33`)
Markov: KL of the pushforward of the policy kernel against the declared preference distribution. (anchor: `futon2/holes/problems/P-markov-category-spec.md:46-47`)
Lean: missing: `def risk`.
Notes: observed: the noun is theory-defined in the glossary and worksheet, but the historical Lean file only exposes it as an argument to `G`, not as its own declaration (`p4ng/sec-glossary.tex:23`; `mathlib4:6fd8a33f:DarkTower/WarMachine/Holes.lean:68-70`).

### Ambiguity [class: theory-defined]
Formal: `ambiguity(\pi) := E_{Q(s\mid\pi)}[H(P(o\mid s))]`; at current WM grain the implementation uses Gaussian entropy over predicted observation channels. (anchor: `p4ng/sec-glossary.tex:25`; `R5-glossary-formalisation.md:21-37`)
Markov: entropy of the observation kernel or of the composed channel `Q(o\mid\pi)`. (anchor: `futon2/holes/problems/P-markov-category-spec.md:28-34,114-115`)
Lean: missing: `def ambiguity`.
Notes: observed: the glossary's formula is canonical, while the measured machine found “0% flips / 674 ticks,” so the formal line is nontrivial and the inertness belongs in Notes, not in the definition (`R5-glossary-formalisation.md:25-38`; `P-markov-category-spec.md:79-85`).

### Observation model $A$ [class: stack-defined]
Formal: `A[o\mid s] := P(o\mid s)` with each column normalised; the live WM instance is an explicit `7×7` status likelihood matrix. (anchor: `p4ng/sec-glossary.tex:27`; `futon2/holes/problems/P-glossary-mathematics.md:33,36-38`)
Markov: the observation kernel `A : S ⇝ O`. (anchor: `futon2/holes/problems/P-markov-category-spec.md:44-45`)
Lean: missing: `def observationKernel`.
Notes: observed: this is the clearest Markov-ready noun in the glossary, but the worksheet correctly keeps its content stack-defined because the matrix entries are hand-set (`R5-glossary-formalisation.md:24-37`).

### Model uncertainty and EIG [class: stack-defined]
Formal: canonical EIG is `EIG(\pi) := E_{Q(o\mid\pi)} KL[Q(\theta\mid o,\pi) \Vert Q(\theta\mid\pi)]`; the current WM bonus is only `U_model = Σ_c sd(A_c)`, not EIG. (anchor: `p4ng/sec-glossary.tex:29`; `R5-glossary-formalisation.md:24-37`)
Markov: KL between posterior and prior pushforwards, if the parameter kernel is declared; current posterior-spread bonus has no such kernel. (anchor: `futon2/holes/problems/P-markov-category-spec.md:79-93,112-116`)
Lean: missing: `def expectedInformationGain`; missing: `def modelUncertaintyBonus`.
Notes: observed refusal: I cannot state the live bonus as EIG without the missing `Outcome`/`Q(o\mid\pi)` carrier and parameter kernel (`P-markov-category-spec.md:112-116`; `P-validated-R5.md:217-221`).

### Softmax and controller calibration [class: stack-defined]
Formal: `p_i = e^{-G_i/\tau} / Σ_j e^{-G_j/\tau}` and, in the live controller, `\tau_eff = 1/g`; the first is theory-defined, the second is an engineering calibration seam. (anchor: `p4ng/sec-glossary.tex:31`; `R14-glossary-formalisation.md:21-36`)
Markov: `softmax : Score^n ⇝ Fin n`; the live selector is the deterministic post-composition that can forget this stochastic morphism. (anchor: `futon2/holes/problems/P-markov-category-spec.md:48-49`; `CommitmentTemperature.lean:236-292` via `P-markov-category-spec.md:20-27`)
Lean: missing: `def softmax`; existing external binding in `CommitmentTemperature` for `governs`, not in `Holes.lean`.
Notes: observed: the glossary itself labels `\tau_eff=1/g` as engineering, so the class is stack-defined even though the softmax equation is canonical (`p4ng/sec-glossary.tex:31`; `R14-glossary-formalisation.md:21-36`).

### Pattern language / cascade [class: borrowed name]
Formal: `Cascade := (nodes, addedByOrganise, edges, acyclic, precedence)`; semilattice/fold structure is additional structure, not yet a theory-defined AIF noun. (anchor: `p4ng/sec-glossary.tex:33`; `mathlib4:6fd8a33f:DarkTower/WarMachine/Holes.lean:26-32`)
Markov: none: authority/order object first; only later can a constructed cascade support Kleisli composition of per-pattern kernels. (anchor: `futon2/holes/problems/P-markov-category-spec.md:46-48,116`)
Lean: binds to `Cascade`, `organiseO1..O4`, `fastForward`. (`mathlib4:6fd8a33f:DarkTower/WarMachine/Holes.lean:26-32,160-203`)
Notes: observed: the glossary and worksheets use “cascade” functionally rather than literally for AIF policy vocabulary, so I keep the class borrowed even though the record shape is already closed in Lean (`R6-glossary-formalisation.md:21-38`; `P-validated-R5.md:236-245`).

### Control states $U$ and the policy vocabulary [class: borrowed name]
Formal: canonical control vocabulary is `U`; policy space grows from `Π_H(U)` to `Π_H(U')` when a new control is added. For WM this is only an analogy from patterns to control schemas. (anchor: `p4ng/sec-glossary.tex:35`)
Markov: control/state vocabulary is data for transition kernels, not itself a morphism. (anchor: `futon2/holes/problems/P-markov-category-spec.md:44-48`)
Lean: missing: `def ControlSchema`; related closed shapes are `Pattern` and `Repository`. (`mathlib4:6fd8a33f:DarkTower/WarMachine/Holes.lean:21-37`)
Notes: observed: the glossary says the correspondence is “functional rather than literal,” which is why the class stays borrowed rather than theory-defined (`p4ng/sec-glossary.tex:35`; `R6-glossary-formalisation.md:21-38`).

### Policy prior $E$ and habit [class: stack-defined]
Formal: `Q(\pi) ∝ exp(\ln E(\pi) - G(\pi)/\tau)`; live WM uses a Dirichlet posterior predictive `\widehat E(a_i) = (n_{k(a_i)}+\alpha)/Σ_j(n_{k(a_j)}+\alpha)`. (anchor: `p4ng/sec-glossary.tex:37`; `R14-glossary-formalisation.md:26-36`)
Markov: prior on policies is a morphism `1 ⇝ Π`; the live grain is action-category habit, not canonical policy prior. (anchor: `futon2/holes/problems/P-markov-category-spec.md:72-79`)
Lean: missing: `def HabitPrior`.
Notes: observed: the glossary says the implementation is exact in form but not at policy grain, so the class is stack-defined on grain grounds (`p4ng/sec-glossary.tex:37-46`; `R14-glossary-formalisation.md:26-36`).

### Policy $\pi$ [class: borrowed name]
Formal: canonically `\pi ∈ Π_H(U)`; in the paper's higher-grain reading it is a cascade plus a direction of play over that cascade, not a single scheduler action. (anchor: `p4ng/sec-glossary.tex:48`; `P-validated-R5.md:228-245`)
Markov: potentially an index into kernels or a kernel-valued object once `Q(o\mid\pi)` is declared. (anchor: `futon2/holes/problems/P-markov-category-spec.md:72-85`)
Lean: binds textually to `Policy := InformationState → Action`, but that is not the glossary's cascade grain; missing: `def CascadePolicy`. (`mathlib4:6fd8a33f:DarkTower/WarMachine/Holes.lean:45-53`)
Notes: observed contradiction: the historical Lean `Policy` is a function over information states, while the glossary's `\pi` is a scored cascade; `P-validated-R5` flags this exact grain split (`P-validated-R5.md:216-221,228-245`).

### Aliveness $L = T\cdot H$ [class: borrowed name]
Formal: `L := T · H`. (anchor: `p4ng/sec-glossary.tex:50`)
Markov: none: stack vocabulary; if used in learning it is a realised reward readout, not a kernel law. (anchor: `p4ng/sec-glossary.tex:50`; `futon2/holes/problems/P-markov-category-spec.md:36-40`)
Lean: missing: `def aliveness`.
Notes: observed: the equation is explicit, but its provenance is architectural/aesthetic rather than AIF-canonical, so I classify the noun as borrowed (`p4ng/sec-glossary.tex:50`; `PREREG-war-machine.md:45-67`).

### Embedding space [class: stack-defined]
Formal: `e : Text → ℝ^n` with similarity induced by a metric or inner product on `ℝ^n`; the paper only claims neighbourhood as hypothesis generation. (anchor: `p4ng/sec-glossary.tex:52`)
Markov: none: stack vocabulary. (anchor: `futon2/holes/problems/P-markov-category-spec.md:36-40`)
Lean: missing: `def Embedding`.
Notes: observed: the glossary gives a geometric object with one honest constraint (“nearness alone is not proof”), which is enough to avoid prose-only but not enough to make it theory-defined for WM (`p4ng/sec-glossary.tex:52`).

### Bayesian Model Reduction [class: theory-defined]
Formal: `A' = A + a' - a` with counts re-expressed under the reduced prior. (anchor: `p4ng/sec-glossary.tex:54`)
Markov: none directly; this is a model-evidence calculation, not a morphism law. (anchor: `futon2/holes/problems/P-markov-category-spec.md:79-93`)
Lean: missing: `def bayesianModelReduction`.
Notes: observed: the glossary gives the exact algebra and the code path exists in Clojure, but the historical Lean file has no BMR declaration (`p4ng/sec-glossary.tex:54`; `mathlib4:6fd8a33f:DarkTower/WarMachine/Holes.lean:21-360`).

### Dirichlet concentration parameters [class: theory-defined]
Formal: `a ∈ (ℝ_{>0})^n` as Dirichlet pseudo-counts. (anchor: `p4ng/sec-glossary.tex:56`)
Markov: none directly; parameters of a prior over kernels or count tables, not themselves morphisms. (anchor: `futon2/holes/problems/P-markov-category-spec.md:79-93`)
Lean: missing: `def DirichletConcentration`.
Notes: observed: the glossary gives the correct count-table interpretation and the Clojure BMR code uses it, but no Lean binding exists yet (`p4ng/sec-glossary.tex:56`).

### Log multivariate beta and $\Delta F$ [class: theory-defined]
Formal: `\Delta F = \ln B(A) + \ln B(a') - \ln B(a) - \ln B(A')`. (anchor: `p4ng/sec-glossary.tex:58`; `futon2/holes/problems/P-glossary-mathematics.md:33-34`)
Markov: none: analytic evidence comparison, not a Markov-category law. (anchor: `futon2/holes/problems/P-markov-category-spec.md:79-93`)
Lean: missing: `def logMultivariateBeta`; missing: `def deltaFReduction`.
Notes: observed: the glossary distinguishes this `\Delta F` from variational `F`, so the formal line must keep them separate (`p4ng/sec-glossary.tex:58`).

### Bayes factor threshold [class: theory-defined]
Formal: accept reduction only if `\Delta F ≤ -3`. (anchor: `p4ng/sec-glossary.tex:60`)
Markov: none. (anchor: `futon2/holes/problems/P-markov-category-spec.md:79-93`)
Lean: missing: `def bayesFactorThreshold`.
Notes: observed: this is a clean scalar law with a stated sign convention; no `Holes.lean` item currently binds it (`p4ng/sec-glossary.tex:60`).

### GFlowNet “slush” [class: stack-defined]
Formal: `P(S\mid m) ∝ exp(\beta \hat R(S\mid m))` with deliberately low `\beta`. (anchor: `p4ng/sec-glossary.tex:62`)
Markov: a proposal kernel from missions to cascade samples, if treated categorically. (anchor: `futon2/holes/problems/P-markov-category-spec.md:47-48`)
Lean: missing: `def SlushProposalKernel`.
Notes: observed: the glossary already frames this as an offline sampler with bounded live role, so stack-defined is the honest class (`p4ng/sec-glossary.tex:62`; `R6-glossary-formalisation.md:27-32`).

### Fold [class: stack-defined]
Formal: `Fold := (boxes, wires, terminals, holes)` with typed incidence constraints; a checked construction plan, not just prose sequence. (anchor: `p4ng/sec-glossary.tex:64`)
Markov: none: first an engineering wiring object; only later could it support kernel composition along the wiring. (anchor: `futon2/holes/problems/P-markov-category-spec.md:46-48,138-140`)
Lean: missing: `structure FoldPlan`.
Notes: observed: the glossary gives enough shape to avoid prose-only, but there is no historical `Holes.lean` declaration for folds (`p4ng/sec-glossary.tex:64`; `mathlib4:6fd8a33f:DarkTower/WarMachine/Holes.lean:21-360`).

### Act-gate [class: stack-defined]
Formal: `act ↔ (S_cascade > 0) ∧ (ΔS_coverage < 0)`. (anchor: `p4ng/sec-glossary.tex:66`)
Markov: none: engineering control predicate, not an AIF morphism. (anchor: `p4ng/sec-glossary.tex:66`; `R8-glossary-formalisation.md:31-36`)
Lean: missing: `def actGate`.
Notes: observed: the glossary explicitly says these are engineering quantities, so the class is stack-defined and the formal line should preserve that honesty (`p4ng/sec-glossary.tex:66`).

### EDN [class: stack-defined]
Formal: `x ∈ EDN ::= map | vector | list | keyword | symbol | string | number | bool | nil`; the point here is parseable data rather than prose. (anchor: `p4ng/sec-glossary.tex:68`)
Markov: none: stack vocabulary. (anchor: `futon2/holes/problems/P-markov-category-spec.md:36-40`)
Lean: missing: `inductive EdnValue`.
Notes: observed: this is not AIF theory but it is formal enough as a data grammar, so it is not marked prose-only (`p4ng/sec-glossary.tex:68`).

### Substrate and Drawbridge [class: stack-defined]
Formal: `substrate1 := filesystem artefacts`; `substrate2 := typed graph records over substrate1`; `drawbridge : substrate1/agent → option substrate2 write`. (anchor: `p4ng/sec-glossary.tex:70`)
Markov: none: infrastructure vocabulary; categorical content would enter only after a typed observation or actuation kernel is declared. (anchor: `futon2/holes/problems/P-markov-category-spec.md:36-40`)
Lean: missing: `structure Substrate`; missing: `def Drawbridge`.
Notes: observed refusal: I can state the two-layer shape, but not a stronger law about independent re-observation without the witness/endpoint schema Joe is still developing (`p4ng/sec-glossary.tex:70`; `PREREG-war-machine.md:170-204`).

### No self-certification [class: theory-defined]
Formal: a claim is certified only by an `L2` witness independent of its producing part; `independent claim witness := witness.producer ∉ claim.producingPart`. (anchor: `p4ng/sec-glossary.tex:72`; `mathlib4:6fd8a33f:DarkTower/WarMachine/Holes.lean:217-282`)
Markov: comparison of predicted and witnessed kernels is permitted only when the witness is external to the model-producing part. (anchor: `futon2/holes/problems/P-markov-category-spec.md:48-49`)
Lean: binds to `Receipt.nonSelfCertifying`, `independent`, `independenceVerdict`, `r9CheckerSound`, `valueEvidenceRequiresL2`. (`mathlib4:6fd8a33f:DarkTower/WarMachine/Holes.lean:104-109,217-282`)
Notes: observed: this is already a closed rule family in the historical Lean file, so the glossary entry directly serves existing declarations rather than inventing a new one (`p4ng/sec-glossary.tex:72`; `mathlib4:6fd8a33f:DarkTower/WarMachine/Holes.lean:217-282`).

### Demonstration Foundry and have--want arrows [class: stack-defined]
Formal: a have-want arrow is a typed morphism `have → want`; Foundry searches for composable pairs across repositories. (anchor: `p4ng/sec-glossary.tex:74`)
Markov: none by default; this is a search/composition vocabulary, not a probability kernel unless sampling is introduced. (anchor: `futon2/holes/problems/P-markov-category-spec.md:36-40`)
Lean: missing: `structure HaveWantArrow`.
Notes: observed: the glossary gives a concrete arrow object and a search action over it, but no historical Lean binding exists (`p4ng/sec-glossary.tex:74`).

### Strategic mission selection [class: stack-defined]
Formal: two-grain selector `Q(\pi_S) ∝ E_S(\pi_S) exp[-G_S(\pi_S)/\tau_S]` and `Q(\pi_T\mid\pi_S) ∝ E_T(\pi_T\mid\pi_S) exp[-G_T(\pi_T\mid\pi_S)/\tau_T]`. (anchor: `p4ng/sec-glossary.tex:76`)
Markov: hierarchical selection kernels over strategic and tactical policy spaces, if those spaces are declared. (anchor: `p4ng/sec-glossary.tex:76`; `futon2/holes/problems/P-markov-category-spec.md:72-85`)
Lean: missing: `def StrategicPolicy`; missing: `def TacticalPolicyConditional`.
Notes: observed refusal: the formula is explicit, but the carrier for strategic outcomes/support remains stack work, so I cannot bind it to current `Holes.lean` beyond a proposal (`p4ng/sec-glossary.tex:76`; `PREREG-war-machine.md:61-67`).

### Clicks, attempts, and cohorts (measurement vocabulary) [class: stack-defined]
Formal: `click := one loop trigger`; `attempt := one full forward pass from a click`; `cohort := preregistered finite window over attempts with fixed stop rule and outcome taxonomy`. (anchor: `p4ng/sec-glossary.tex:78`)
Markov: none: measurement vocabulary. (anchor: `futon2/holes/problems/P-markov-category-spec.md:36-40`)
Lean: missing: `structure Attempt`; missing: `structure CohortWindow`; related closed shapes are `Delivery`, `Handoff`, `Workflow`. (`mathlib4:6fd8a33f:DarkTower/WarMachine/Holes.lean:293-320`)
Notes: observed: the glossary gives operational set-membership rules and bounded retries, which are formal enough, but the historical Lean file only has the delivery/handoff side (`p4ng/sec-glossary.tex:78`; `mathlib4:6fd8a33f:DarkTower/WarMachine/Holes.lean:293-320`).

### A shared experimental substrate [class: stack-defined]
Formal: one shared typed store with domain-tagged endpoints and a validation ladder `projection < maths < dark-WM < live-WM`; claims transfer the learning mechanism, not domain truth. (anchor: `p4ng/sec-glossary.tex:80`)
Markov: none directly; this is a cross-domain substrate condition, not a kernel law. (anchor: `futon2/holes/problems/P-markov-category-spec.md:36-40`)
Lean: missing: `structure SharedExperimentalSubstrate`.
Notes: observed refusal: I can state the ladder/order and boundary condition from the glossary, but not a tighter theorem about transfer without Joe's still-open endpoint/witness decisions (`p4ng/sec-glossary.tex:80`; `PREREG-war-machine.md:170-204`).

## tally
- theory-defined: 13
- stack-defined: 16
- borrowed name: 4
- undefined: 0
- prose-only entries: 0

## missing Lean declarations
- `ActiveInferenceModel`
- `GenerativeModel`
- `BeliefState`
- `predictionError`
- `PrecisionMap`
- `variationalFreeEnergy`
- `expectedFreeEnergyKernel`
- `risk`
- `ambiguity`
- `observationKernel`
- `expectedInformationGain`
- `modelUncertaintyBonus`
- `softmax`
- `ControlSchema`
- `HabitPrior`
- `CascadePolicy`
- `aliveness`
- `Embedding`
- `bayesianModelReduction`
- `DirichletConcentration`
- `logMultivariateBeta`
- `deltaFReduction`
- `bayesFactorThreshold`
- `SlushProposalKernel`
- `FoldPlan`
- `actGate`
- `EdnValue`
- `Substrate`
- `Drawbridge`
- `HaveWantArrow`
- `StrategicPolicy`
- `TacticalPolicyConditional`
- `Attempt`
- `CohortWindow`
- `SharedExperimentalSubstrate`

## refusals for Joe
- `Model uncertainty and EIG`: cannot promote `U_model = Σ sd(A_c)` to canonical EIG without declared `Outcome`, `Q(o|π)`, and parameter posterior kernels (`p4ng/sec-glossary.tex:29`; `P-markov-category-spec.md:112-116`; `P-validated-R5.md:217-221`).
- `Policy π`: the glossary's cascade-grain policy and `Holes.Policy := InformationState → Action` are different objects; I report the split and choose neither (`p4ng/sec-glossary.tex:48`; `mathlib4:6fd8a33f:DarkTower/WarMachine/Holes.lean:45-53`; `P-validated-R5.md:228-245`).
- `Substrate and Drawbridge`: the two-layer shape is stateable, but a stronger law about independent re-observation needs the still-open witness/endpoint schema (`p4ng/sec-glossary.tex:70`; `PREREG-war-machine.md:170-204`).
- `Strategic mission selection`: the formula is explicit, but the strategic carrier/support objects remain stack work, so only a signature proposal is honest today (`p4ng/sec-glossary.tex:76`; `PREREG-war-machine.md:61-67`).
- `A shared experimental substrate`: the validation ladder is stateable; a transfer theorem is not, pending the same endpoint/witness decisions (`p4ng/sec-glossary.tex:80`; `PREREG-war-machine.md:170-204`).
