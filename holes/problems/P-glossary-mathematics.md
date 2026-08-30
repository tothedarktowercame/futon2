# P-glossary-mathematics — Every glossary entry carries the mathematics it stands on

Problem record (delivery-lifecycle v2). Opened 2026-08-30 by claude-15 on Joe's direction: *"ask the Codex
agent to go through the entire glossary again and see if we can add more detail on the actual mathematics
underpinning the active inference framework — most likely a large amount routed through the Markov-category
infrastructure — otherwise I worry we'll be building something abstract, an implementation not grounded in
the actual content of the theory."* Owner: claude-15. **holder.** claude-15 → codex-5 (G-D1). **parent.**
P-validated-R5 (Gate 0) and P-markov-category-spec.

## S1

**problem.** The paper's glossary (`p4ng/sec-glossary.tex`, 33 `\paragraph` entries) is the document the
prereg's Gate-0 table cites as the definition of each R-node's nouns, and the R8/R2 worksheets' §6 already
asked for a `Formal:` line per entry. Today's Lean holes (`Holes.lean`, 23 at `6fd8a33f`) state *laws* over
types whose mathematical content is carried by prose: `G = risk − EIG` names a subtraction, not the
expected-free-energy decomposition; `C` is "a preference distribution"; `Policy : InformationState → Action`
is a function type with no kernel; precision Π, the observation model A, F as ½·mean(Π ε²), BMR's ΔF and the
Bayes factor are formulae in the glossary that nothing in Lean or the records restates. The risk Joe names
is the July risk from the other side: an implementation that is faithful to an *abstraction* of the theory
rather than to the theory.

**now.** What exists to route through: `Mathlib.Probability.Kernel.Category.Stoch` elaborates (Markov
categories with copy Δ, discard ε; determinism = commutes with copy; conditionals); `CommitmentTemperature.lean:236–292`
holds the finite `factorsThroughDiscard` theorems; `P-markov-category-spec.md` §0–§3 fixes which node the
Markov layer spans and the order of work (one edge first); `futon2/docs/futon-aif-completeness.md` is last
year's requirements checklist (three different "G(π)"s); the Snatch microcosm has a concrete kernel
(`how_kernel_snatch.clj`) and a derived Q(o∣π).

**solved.** A worksheet in which **every one of the 33 entries** carries: (1) its Gate-0 class
(theory-defined / stack-defined / borrowed name / undefined); (2) for the theory-defined ones, a `Formal:`
line — the mathematical statement on AIF's own terms (generative model P(o, s, π); variational density Q;
F = E_Q[ln Q − ln P]; G(π) = E_Q[ln Q(s∣π) − ln P(o, s∣π)] with its risk/ambiguity and EIG/pragmatic
decompositions written out; Π as inverse variance of the likelihood; A as P(o∣s); softmax over −G/τ; BMR's
ΔF via the log multivariate beta; the Bayes-factor threshold) **with a literature anchor** (Friston et al.,
Parr–Pezzulo–Friston 2022, Da Costa et al. 2020; Fritz 2020 and Smithe for the categorical reading); (3) its
**Markov-category rendering** where one exists — which object, which kernel, which composition (A as a
kernel S ⇝ O; the generative model as a composite; Bayesian inversion as the conditional; policies as
kernels or as indexed families of them; ambiguity as the entropy of a kernel's output; EIG as a KL between
pushforwards) — or "none: this entry is stack vocabulary"; (4) the **Lean binding**: which declaration in
`Holes.lean` this formal line would give a body or a law to, or `missing` with the declaration it would
need. **Falsifier:** an entry whose `Formal:` line is a restatement of its prose (no new symbol, no
constraint) is not formal and must be marked so; an entry whose formal line contradicts the code
(`P-validated-R5` §2b: three unrelated `G`s) reports the contradiction rather than choosing.

**facades:** formula-shaped prose; a citation to a whole book; the Markov-category rendering as a
vocabulary swap ("kernel" for "function") with no composition law stated; "grounded" claimed for an entry
whose formal line no code or Lean item binds to; deriving the glossary from the code (the July direction —
the code is the thing being judged).

**status.** open.

## deliveries
- **G-D1 — discovery/draft, no edits to the paper** (codex-5). The worksheet above, all 33 entries, in
  `futon2/holes/labs/wm-contract/glossary-formal-lines.md` (≤ 450 lines), every claim about the stack with
  file:line, every formula with its anchor. Refusal permitted per entry ("I cannot state this on the theory's
  terms without a decision from Joe: …").
- **G-D2 — the `Formal:` lines into `sec-glossary.tex`** — Joe's paper, so dispatched only on Joe's word
  after G-D1's review, one section at a time.
- **G-D3 — Holes.lean bindings** — the owner ratifies each `missing` declaration (charter 6), after AD-D2.

## log
- 2026-08-30 record written (claude-15); G-D1 dispatched (status line below).
- 2026-08-30 16:19Z **G-D1 reviewed by claude-15 — passes** (futon2 `c847bca`, 254 lines; codex-5's bell said
  "staged" — it is committed). Checked: 33 `###` entries, 33 `Formal:` lines; class tally **13 theory-defined /
  16 stack-defined / 4 borrowed / 0 undefined**; three formal lines against the glossary's own mathematics
  — F = ½·mean_k(Π_k ε_k²) (`sec-glossary.tex:19`, exact), the G decomposition with the *grain mismatch
  reported not collapsed* (glossary: risk + ambiguity over an action a; `Holes.G := risk − eig` over a
  generic Policy; the three-G contradiction of §2b is in the Notes, unresolved as required), ΔF ≤ −3
  (`:60`, exact); the `CommitmentTemperature.lean:236–292` pointer opened (the finite Markov section);
  **prose-only falsifier applied by me** to the stack entries (EDN as a data grammar; click/attempt/cohort
  as definitions; Fold as a typed incidence structure) — each adds a symbol or a constraint, so "0 prose-only"
  stands, with the honest gloss that stack entries are formal as *data shapes*, not as AIF mathematics.
  **Missing-declaration list:** 35 names, none already in `Holes.lean` under another name (checked); the
  AIF core among them — `GenerativeModel`, `observationKernel` (A), `BeliefState` (μ), `PrecisionMap` (Π),
  `variationalFreeEnergy`, the G decomposition (`risk`, `ambiguity` as functionals of Q(o∣π)),
  `expectedInformationGain`, `softmax`, `bayesianModelReduction` / `logMultivariateBeta` /
  `deltaFReduction` / `bayesFactorThreshold` — is G-D3's first batch, and **several are bodies, not holes**:
  where the glossary gives the formula, Lean gets the definition.
  **Refusals (for Joe):** (1) *Model uncertainty and EIG* — `U_model = Σ sd(A_c)` cannot be promoted to
  canonical EIG without a declared Outcome, Q(o∣π) and parameter-posterior kernels (the spine); (2) **the two
  π's** — the glossary's π is a *scored cascade*; `Holes.Policy := InformationState → Action` is a function
  (P-validated-R5 §3, Joe's "policy is an operation on cascades" vs the decided function type): reported, not
  collapsed — **this one is Joe's to settle**, since both are his; (3) Substrate/Drawbridge — two-layer shape
  stateable, the independent re-observation law waits on the witness schema; (4) Strategic mission
  selection — formula explicit, carrier unresolved; (5) Shared experimental substrate — validation ladder
  stateable, transfer theorem not.
  **G-D2 (the `Formal:` lines into `sec-glossary.tex`) is HELD for Joe.** G-D3 packet written and sent to
  claude-13 for its read.

**STATUS 2026-08-30 16:07Z:** G-D1 dispatched to codex-5 — job `invoke-1788106044356-4363-64f6725a`, park `park-97f15f37-e884-4b05-845a-0931a2123614` (deadline +55 min). G-D2 (paper edits) on Joe's word only.

**STATUS 16:27Z:** G-D3 dispatched to codex-22 (job `invoke-1788107219429-4392-e72310bf`, park `park-6d39a05e-d4db-401d-a9e7-1c5fbdeeab32`) after two refusals by claude-13 (rev 1: my :39 transcription dropped ln E(π); rev 2: the Policy gate was unsatisfiable — selector on a field equal on all 48) and a PASS on rev 3 (md5 `b86dac7f`). codex-22 holds `Holes.lean` for the duration. G-D2 (paper) held for Joe.

**G-D3 PASSED the owner gate 16:34Z** (codex-22, mathlib4 `be322f91` + `c09316cc` + `1b09974a` + `66317c71`).
Recomputed here: **24 + 8 = 32 = 32** (pinned T0; JSON holes; sorry count by direct elaboration); zero errors;
closed 24 → 31; re-emit byte-identical; `source.git-sha` = the module's last commit (`1b09974a`); two-number
Policy gate by name vs JSON@`53c5e466`: N_selected 15 = 7 bodies + 8 holes, `Policy` 0 among them; exactly two
refusals. **Every new body diffed against its glossary line at source:** `predictionError` ε_k := o_k − μ_k
(:15); `PrecisionMap` Channel → ℝ≥0 (:17); `variationalFreeEnergy` = ½ · Σ_k Π_k ε_k² / |Channel.all| — the
mean, the ½, ε² (:19); `deltaFReduction` = ln B(A) + ln B(a′) − ln B(a) − ln B(A′), primes as written (:58);
`bayesFactorThreshold` ΔF ≤ −3 (:60); `softmax` weights exp(log(habit π) − grade π / τ) — **both terms** (:39);
`bayesianModelReduction` A′ = A + a′ − a componentwise (:54). Holes: `GenerativeModel`, `observationKernel`,
`BeliefState`, `expectedFreeEnergy` (doc tag carries the grain-mismatch sentence verbatim; `Holes.G` untouched),
`expectedInformationGain`, `logMultivariateBeta`. Refused: `modelUncertaintyAndEIG` (needs Outcome/Q — the
spine), `cascadeGrainPi` (the glossary's scored-cascade π, named without redefining `Policy` — **Joe's decision**).
**Two honest notes:** (1) `logMultivariateBeta` is a hole (the glossary gives its role, not its analytic form;
a larger Mathlib import was forbidden), so `deltaFReduction` is a *body-over-a-hole* — counted as **6 full bodies
+ 1 body-over-a-hole**, not 7 (claude-13's watch item); (2) `softmax` takes `exp log : ℝ → ℝ` as parameters
rather than importing `Real.exp` — the body states the formula's shape; instantiate with the analysis import when
that is acceptable. 15 declarations from 13 entries: BMR split from its Beta dependency; canonical EIG split
from the refused live identification — reported, not fitted.
**Class of each of the 13:** body — prediction error, precision, F, ΔF (over the Beta hole), Bayes-factor
threshold, softmax, BMR; hole — active-inference model/generative model, observation model A, belief state μ,
expected free energy G, EIG; refused — model uncertainty & EIG, policy π (cascade grain).
**Lean two lines now: 31 bodies / 32 holes.** The contract lint sees the new contract: 5 bindings `:stale`
(ablation, F1–F4 — bound against `32b92969`), R9's four `:witnessed`; re-binding is a re-run, not an edit (AD-D5).
G-D2 (paper) still Joe's.

