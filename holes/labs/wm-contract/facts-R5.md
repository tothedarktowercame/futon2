# facts-R5 — pointers and verbatim snippets for the R5 worksheet

**Date:** 2026-08-30. **Scope:** War Machine node R5 ("Expected Free Energy
Inside an Auditable Controller"; red ring WR-25 = the coverage report / what the
criterion set does NOT cover). **Facts only** — no interpretation, no
classification. Each item carries a `file:line` pointer and a short verbatim
quote. Where something does not exist the command used is given.

All paths are absolute under `/home/joe/code/`.

---

## 1. PAPER

### 1a. `p4ng/sec-catalog.tex`

`grep -n 'R5' p4ng/sec-catalog.tex` → three hits: lines 111, 235, 408.

**Line 111** (catalog table row):
> `Evaluation, selection, and planning & R5 --- Expected Free Energy Inside an Auditable Controller & Separates the canonical score from engineering controls. \\`

**Line 235** (the R5 pattern paragraph, `\label{pat:r5}`), verbatim:
> **If** you want selection to balance canonical outcome risk and ambiguity with operational constraints. **However** calling every useful controller term ``EFE'' makes tuning indistinguishable from storytelling. **Then** compute and persist the canonical core separately --- *risk*, how far the outcomes an action predicts sit from the outcomes the system prefers, plus *ambiguity*, how uninformative those outcomes are expected to be --- and admit engineering terms only as a named augmentation, reported *beside* that core and never folded into it. **Because** the decomposition makes clear whether a recommendation moved because of active-inference quantities or because of an engineering control. *Invariant to preserve:* the two-term core is persisted apart from everything wrapped around it, so any reader can ask which one moved the pick.

**Line 408** — a LaTeX comment (line starts with `%`), fragment:
> `At R5 the implemented Expected Free Energy ($\mathrm{risk}+\mathrm{ambiguity}$) is drawn distinctly from the multi-objective controller that wraps it.`

The word "coverage" does not appear in the R5 paragraph at line 235. The
paragraph does not mention criterion sets, non-coverage, or WR-25.

### 1b. `futon2/docs/futon-aif-completeness.md` — `### R5` at lines 125–146

**Line 125:** `### R5 — EFE with at least two principled terms`

**Line 127:**
> Expected Free Energy decomposes into at minimum: (R5a) pragmatic / risk and (R5b) epistemic / ambiguity. Both are computed against the predictive forward model from R4.

**Line 129:**
> **Operational check.** Find the EFE computation. Verify both pragmatic and epistemic terms are present and computed against the predictive forward model.

**Line 131:**
> **This implementation.** **Satisfied for the R5 minimum as of v0.4 (2026-05-17); disaggregated for nats-EFE on 2026-07-08.** `futon2.aif.efe/compute-efe` scores a `(state, action)` pair by composing R4's forward model with the R3c free-energy decomposition.

**Lines 132–134:**
> - **R5a — G-risk**: pragmatic / risk term. Uses `compute-free-energy`'s :G-pragmatic on the *predicted* next-observation mean, not the current observation. Captures "how far the predicted state is from preferences."
> - **R5b — G-ambiguity**: epistemic / ambiguity term. Sum of per-channel predicted variances from `forward-model/predict.next-observation.variance`. Captures "how uncertain the predicted outcome is" (high-variance actions are higher EFE).
> - **G-total** = G-risk + G-ambiguity.

**Line 136:**
> `futon2.aif.efe/rank-actions` scores a sequence of candidate actions and orders them by G-total ascending (lowest EFE first), with `:rank` annotations.

**Line 140:**
> **Nats-EFE status (audit 2026-07-08).** Canonical scoring target here is `G = risk + ambiguity - EIG`, all in nats. That is **2/3 built and ready in futon2.aif**, with the remaining **1/3 (EIG feed/caller injection) landing on the separate A4a/A1 strand**:

**Line 141** (risk): `:risk-mode :kl` scores `Σ_ch KL(Q~_ch || C_ch)` … "Library default remains `:hinge` for byte-identity; the WM arena resolves `:kl` unless the operator uses `FUTON_WM_RISK_MODE=hinge`."

**Line 142** (ambiguity): `:ambiguity-mode :gaussian-entropy` scores `Σ_ch 0.5*ln(2*pi*e*sigma2_ch)` … "Library default remains `:variance-sum`; the WM arena resolves `:gaussian-entropy` unless the operator uses `FUTON_WM_AMBIGUITY_MODE=variance-sum`."

**Line 143:**
> - **EIG — separate/open for this R5 accounting.** The BMR/A4a read-point and `:G-eig-bmr` bridge are distinct from ambiguity and are intentionally not counted as a completed default R5 core leg here. The A1 caller injects the EIG lookup/closure; absent injection contributes 0.0.

**Line 145:**
> So the honest status is: **historical blend done; nats risk done and flipped in the arena; nats ambiguity dark/library-default-off but ready and arena-resolved; EIG is the remaining third and is owned by the separate 3a/A4a-A1 path.**

Other R5 mentions in the same doc:
- **Line 324** (stack-scope table): `| R5 (principled EFE terms) | F4 + F5 | EFE on predicted state quantifies both bounded balance (R5a pragmatic) and response uncertainty (R5b epistemic). |`
- **Line 344** (status table): `| R5 — Principled EFE terms | **✓ minimum; nats-EFE 2/3 ready as of 2026-07-08** | Historical blend done. Nats risk `:kl` built and arena-flipped; nats ambiguity `:gaussian-entropy` built/validated/provenance-ready with library default still dark; EIG is the separate A4a/A1 leg. |`
- **Lines 373–377:** `**v0.23 R5 disaggregation (2026-07-08):** the old "R5 half-done" shorthand is retired. Minimum R5 remains satisfied, but canonical nats-EFE is now accounted as `risk + ambiguity - EIG`: 2/3 built/ready in futon2.aif (risk KL + Gaussian-entropy ambiguity) and 1/3 separated to the A4a/A1 EIG feed.`
- **Line 392:** `| **Prior preferences over outcomes (the C matrix)** | R5a *uses* it; mislabeled as hyperparams in `aif.preferences`; no first-class criterion | **absent as criterion → R19** |`
- **Lines 394–395:** `| EFE pragmatic (risk) | R5a | present |` / `| EFE epistemic — ambiguity | R5b | present |`
- **Line 482** (R19): "C is not a first-class criterion: it sits implicit inside R5a's risk ("distance from what you prefer") …"

The word "coverage" in this doc's R5 section: not present (`sed -n '125,146p' … | grep -c coverage` → 0). The completeness doc's R5 is about the two-term EFE, not about a coverage report.

### 1c. `p4ng/empirics-futon/promotion-tests.edn` — `:node "R5"` at lines 124–146

Verbatim:
```
{:node "R5" :wr "WR-25"
 :statement
 "given a criterion set that an operator has weighted, the evaluate stage
  reports what the criterion set does NOT cover, and does so with the same
  discipline it applies to a poor score -- not only how the candidates rank
  under the weights it was handed."
 :null-control
 "the same run with the coverage report suppressed. If the operator reaches
  the same decision either way, the report is decoration."
 :external-fixture
 {:case :langchain-langsmith
  :phase "EVALUATE"
  :dated "Klarna scaled on conversation volume, deflection rate, resolution
          time and FTE-equivalents, then reversed"
  :must-emit
  "that the criterion set is incomplete on the dimension the reversal turned
   on. The CEO's own account is that cost was over-weighted and quality was
   what suffered: the criterion the firm judged the assistant by did not
   carry the outcome that turned out to matter. An apparatus that optimises
   the handed criterion faster makes this worse, not better."}
 :registers-when {:built "the coverage report is emitted"
                  :ran   "it is run against the fixture"
                  :live  "the null control fails"}}
```

---

## 2. GLOSSARY — `p4ng/sec-glossary.tex` (33 `\paragraph` entries, lines 5–80)

Entry list (`grep -n -o '\\paragraph{[^}]*}'`): 5 Active Inference Framework · 7 Generative model · 9 Belief state μ · 12 Observation vector o · 15 Prediction error ε · 17 Precision Π · 19 Variational free energy F · **21 Expected free energy G** · **23 Risk** · **25 Ambiguity** · **27 Observation model A** · **29 Model uncertainty and EIG** · 31 Softmax and controller calibration · 33 Pattern language / cascade · 35 Control states U and the policy vocabulary · 37 Policy prior E and habit · 48 Policy π · 50 Aliveness · 52 Embedding space · 54 Bayesian Model Reduction · 56 Dirichlet concentration parameters · 58 Log multivariate beta and ΔF · 60 Bayes factor threshold · 62 GFlowNet "slush" · 64 Fold · 66 Act-gate · 68 EDN · 70 Substrate and Drawbridge · 72 No self-certification · 74 Demonstration Foundry · 76 Strategic mission selection · 78 Clicks, attempts, and cohorts · 80 A shared experimental substrate.

### Line 21 — Expected free energy $G$ (full text)
> Expected free energy is a future-tense action score: if the system takes this action, or follows this policy, how much risk, ambiguity, and useful learning should it expect?  Written out, $G_{\mathrm{efe}}(a) = D_{KL}[Q(o\mid a)\Vert C] + \mathbb{E}\,H[P(o\mid s)]$\eqanchor{Ga}, in nats: the first term is risk, the divergence of predicted outcomes from preferences $C$; the second is ambiguity, the expected observation entropy (both are glossed separately below).  The implementation persists this two-term core apart from its controller augmentation --- posterior spread, urgency, intrinsic credit, feasibility masks, and coverage bonuses are reported beside it, never hidden inside it.  So risk $8$ and ambiguity $6$ give $G_{\mathrm{efe}}=14$, and a controller total of $11$ means the explicitly reported bonuses contributed $-3$.  The paper uses two grains: $G(a)$ scores candidate tasks, while $S(\pi)$ scores short tactical cascades of patterns for the chosen task \cite{FRISTON2016862,neacsu2024structure}.\footnote{Task-level scoring is in \srcref{futon2/src/futon2/aif/efe.clj}; tactical construction scoring uses \srcref{futon2/src/futon2/aif/fold_eval.clj} and the fold escrow path.}

### Line 23 — Risk (full text)
> Risk is the expected cost of ending up in an unwanted state.  The implementation uses the canonical active-inference form: a KL divergence between predicted outcomes and preferred outcomes.\footnote{\srcref{futon2/src/futon2/aif/efe.clj} supports \texttt{:risk-mode :kl}; \srcref{futon2/test/futon2/aif/efe_risk_mode_test.clj} pins the default and the escape hatch.}

### Line 25 — Ambiguity (full text)
> Ambiguity is uncertainty in the mapping from state to outcome.  The notation $P(o\mid s)$\eqanchor{ambiguity} is read ``the probability of observing $o$ if the world is in state $s$.''  Its entropy is high when that state could produce many different observations, and low when the observation is predictable.  Expected entropy means averaging that uncertainty over the states the system may encounter.  Reducing ambiguity means choosing actions that make future evidence more informative \cite{FRISTON2016862}.  For a binary outcome, $P(o\mid s)=[0.5,0.5]$ gives entropy $1$ bit --- maximum ambiguity --- while $[0.9,0.1]$ gives about $0.47$ bit.\footnote{\srcref{futon2/src/futon2/aif/efe.clj} supports \texttt{:ambiguity-mode :gaussian-entropy}; tests are in \srcref{futon2/test/futon2/aif/efe_test.clj}.}

### Line 27 — Observation model $A$ (full text)
> The observation model $A$ is the agent's theory of its sensors: for each hidden state $s$, it specifies the probability distribution over observations $P(o \mid s)$. In the production War Machine belief path, every event update flows through this declared model --- when the agent observes $o$, it revises its posterior by multiplying the predicted prior by the likelihood column $A[o \mid \cdot]$. The implementation declares $A$ as an explicit, column-normalised $7 \times 7$ matrix over the entity-status vocabulary, with three entry classes: diagonal entries (an observation is the strongest evidence for its own status), lifecycle-adjacent entries (an observation is mildly compatible with a neighbouring status), and contradictory entries (an observation is evidence \emph{against} an opposed status). The contradictory class lets the model say ``this observation counts against that status,'' not merely ``for this one.'' The transition model $B$ is the identity, so the prediction step preserves the prior posterior, and the initial prior $D$ is uniform. The filter computes the prediction--update cycle $q^- = Bq$, $q \propto A(o \mid \cdot)^{\kappa(w)} q^-$, where $\kappa(w) = \log_2(1+w)$ is a tempered-likelihood exponent controlled by observation weight. Each trace stamps the selected likelihood mode and content hashes for A, B, and D; a provenance-visible diagonal comparison mode supports controlled ablation. A simulation over 50 synthetic entities with varied event types confirms that the off-diagonal structure is behaviourally active: it redistributes posterior mass at $50{\times}$ the rate of the real trace corpus (KL mean 0.050 vs 0.001 nats), and correctly suppresses contradictory statuses (observing \texttt{strengthened} reduces \texttt{falsified} mass by 33\% relative to the diagonal comparison). This warrants R3's green, model-relative status: the filter is exactly derived from its declared A/B/D model. It does not establish that those hand-set entries describe the world; calibration against exogenous evidence from adjudicated handoffs, build outcomes, and operator review remains open.\footnote{The declared observation model, transition model, validators, categorical filter, model manifest, and production seam are in \srcref{futon2/src/futon2/aif/belief.clj} and \srcref{futon2/scripts/futon2/report/war_machine.clj}; tests are in \srcref{futon2/test/futon2/aif/belief_test.clj} and \srcref{futon2/test/futon2/aif/a_matrix_live_wiring_test.clj}; the simulation is \srcref{futon2/scripts/aif/a_matrix_simulation.bb}; design and findings are in \srcref{futon2/holes/missions/M-aif-a-matrix-faithfulness.md} and \srcref{futon2/holes/labs/M-aif-faithfulness/a-matrix-simulation-findings.md}.}

### Line 29 — Model uncertainty and EIG (full text)
> Expected information gain is an expectation of posterior uncertainty reduction. The structure learner does not compute that expectation: it exposes aggregate posterior spread, $U_{\mathrm{model}} = \sum_c \mathrm{sd}(A_c)$\eqanchor{eig-stddev} over the concepts' Dirichlet posteriors, as a \emph{model-uncertainty bonus}. The bonus may steer exploration inside the engineering controller, but it is deliberately excluded from \texttt{G-efe}: it does not predict an observation, nor the posterior-entropy reduction that observation would cause. A policy-conditioned EIG term would require predicted prior and posterior entropies under each policy.  The boundary is worth stating twice\eqanchor{efe-offline}: online $G_{\mathrm{efe}}$ ranks predicted outcomes, offline Bayesian Model Reduction (below) compares model evidence over stored observations, and posterior spread may suggest which reduction to test --- but none of that turns either quantity into EIG.\footnote{\texttt{:model-uncertainty-bonus} is composed in \srcref{futon2/src/futon2/aif/efe.clj}; the posterior-spread helpers are in \srcref{futon2/src/futon2/aif/a4a.clj} and \srcref{futon2/src/futon2/aif/a4a_substrate.clj}.}

### Entries mentioning "coverage" (`grep -n -i coverage sec-glossary.tex` → lines 21, 48, 62, 66)
- **Line 21** (G): "…feasibility masks, and coverage bonuses are reported beside it, never hidden inside it."
- **Line 48** (Policy π): "a coverage-selected cascade carries a handful of sequential edges"; "$S_{\mathrm{cascade}}=\mathrm{coverage\ reward}-\lambda\cdot\mathrm{prior\ cost}$\eqanchor{cascade-F}"; footnote: "turns a cascade-lane entry into an act-gate with \texttt{:cascade-score} and \texttt{:coverage-score-delta}"; "emits an engineering cascade score (coverage reward minus prior cost)".
- **Line 62** (GFlowNet): "the slush beats a greedy baseline on diversity and coverage"; "pattern-text coverage cannot distinguish a well-written plan from one that works"; "rather than treating text coverage as reward".
- **Line 66** (Act-gate): "act if and only if $S_{\mathrm{cascade}} > 0 \,\wedge\, \Delta S_{\mathrm{coverage}} < 0$ … the fold improves the lower-is-better coverage score. These are engineering control quantities, not $F$ or $G$".

### Entries mentioning "criterion" (`grep -n -i criterion sec-glossary.tex` → line 72 only)
- **Line 72** (No self-certification), in the footnote's numbering caveat only: "that file tests the \srcref{futon2} criterion, not this pattern." No glossary entry defines a criterion set, coverage report, non-coverage, or "uncovered".

---

## 3. EXCURSION — `futon3c/holes/excursions/E-R5-red-ring-fill.md` (370 lines)

### Premise as opened (lines 1–11)
- **Line 1:** `# E-R5-red-ring-fill — the singularity that arrived with no dimension to receive it`
- **Line 3:** `**Opened:** 2026-08-27 · claude-13 at Joe's direction. The fifth and last of the red-ring excursions; the four others opened 2026-08-26/27.`
- **Line 6:** `**R5 is the inverse of R14, and together they exhaust how discrimination fails.**`
- **Lines 8–11:**
  > **R14** — a dimension with **no singularity** on it: τ is computed, reported, and no value of it changes the selected action.
  > **R5** — a singularity with **no dimension** to receive it: `:warm-customer-pays` became satisfied and the machine had no channel on which to register it.

### Status table (lines 15–20)
| | state |
|---|---|
| the ring's claim | **CONFIRMED at source** — and it is the best-evidenced of the five |
| bearer | **external** — *"the one red ring of the five whose cost is borne outside the stack"* |
| theoretical core | the Deleuze/singularity material, which belongs here rather than in R14 |
| relation to module 1 | `foldCompliant` in `GainChain.lean` is R5's property in embryo |

### The claim, checked (lines 24–41)
- Line 24–26: "R5's pattern says `:warm-customer-pays` — *"the strongest signal the outside world can send this stack — someone paid"* — arrived **satisfied, uncounted and unsurfaced**. Verified 2026-08-27:"
- Line 28: "`grep -c warm-customer-pays futon2/src/futon2/aif/observation.clj` → **0**. It is not one of the fourteen channels the machine steers by."
- Line 30: "`grep -c … futon2/src/futon2/aif/efe.clj` → **0**. It reaches no score."
- Line 31–33: "In the evidence store (`storage/futon1a/.../evidence.edn`) it occurs **once**, and that occurrence is prose: *"`warm-customer-pays` → (sales outcome, not an academic demo — **skip** for the fair)"*."
- Line 39–41: "**This is the first of the five rings whose recorded reason survived checking without correction.**"

### Nouns used (as named in the file)
singularity / dimension (l6–11, 52–61); criterion set (l63–68, 77–80); boundary (l65); typed report / typed absence (l77, 88–89); declared domain (l67, 253–254); coverage / "own coverage" (l87–89); rung / satisfied rung (l24, 88, 93); channel (l11, 82, 95); projection (l87, 248); evaluate stage (l63, 149); event (l66, 72); edge (l110–132, 160–171); support / mass / kernel / readouts / mirror (l136–142); `discriminates?` and `entropy` (l141, 150–153); flight / close / `007-closed.edn` (l203–212); coverage statement (l205–208, 231); `:uncovered` (l208); `G(π)` and S-G1..S-G4 (l272–320); wiring (l286–302); policy (l276–284, 312–320).

### The requirement, in the family vocabulary (lines 75–96)
- Line 77: `**Family 2 (typed absence) and family 5 (declared domain), at the criterion set.**`
- Lines 79–80: `> A criterion set states what it does not cover. An event outside it is recorded as *outside*, with the same discipline as a poor score — never as nothing.`
- Lines 82–85: `**❌ The naive fix: add a `:warm-customer-pays` channel.** It recreates the defect one rung along. … a list is the paradigm dimension with no singularity on it.`
- Lines 87–91: `**✅ The requirement-satisfying fix:** the projection reports its **own coverage** — which rungs it counts, and that there exist satisfied rungs it does not — so an uncovered satisfaction is a *typed* report rather than a silence. That is `foldCompliant` one level up: the step must leave a record even when it cannot produce a value.`
- Lines 93–96: `**Acceptance.** A satisfied rung outside the counted set produces a record naming it as uncovered. No rung is silently absent. And, per the singularity test: adding the fifteenth channel must be a consequence of the coverage statement, not an edit to it.`
- Lines 63–68: `**And R5's node statement is the general form.** … **the criterion set must declare its own boundary**, so that an event outside it registers as outside rather than as absent. Family 5's `declaredDomain` and family 2's `typedAbsence`, applied to the criteria themselves rather than to a producer.`
- Lines 70–73: `**WR-25's framing is a special case.** *Good news gets the same evidence discipline as bad* is one asymmetry of a polarity-neutral condition.`

### What makes this ring different (lines 100–108)
- Line 100–102: "Its cost is **borne outside the stack**. … R5's is *"the operator's, and through him the paying party's."*"
- Line 104–108: "R5's acceptance needs an event from outside, and those arrive on the world's schedule. **The instrument must therefore be in place before the next one arrives** — which is WR-27's *born instrumented* applied to the one channel we do not control."

### The build: edge-graph kernel (lines 110–183)
- Line 112–114 (Joe): "aim for a build general enough to work in Snatch, the mathematics patterns, and general-purpose WM behaviour; Snatch is the easy test domain."
- Corpus measurement table (l118–121): `futon3/library` (1227 flexiargs): `@why` 60, `@how` **1 file**, `@see-also` 38; five maths namespaces (~96): 14 / **0** / 21.
- Line 128–129: "**Design consequence, and it is the main one: the build must not depend on `@how`.**"
- Five steps table (l136–142): 1·support (core.logic relation), 2·mass (Beta posterior per edge), 3·kernel (`node → D(node)`; cascade = Kleisli composition), 4·readouts (`discriminates?` and `entropy`), 5·mirror (zero-mass case).
- Line 149–153: "R5's requirement is that the evaluate stage **report what its criterion set does not cover**. Step 4 is that requirement made computable: a channel's entropy *is* the statement of how much the criterion fails to determine, and `discriminates?` is the statement that a dimension carries no singularity at all. **R5 is the ring where a spread becomes reportable rather than merely present.**"

### Dispatch slices (lines 155–183) — "drafted, not sent … **Send to codex-22.**"
| slice | what | acceptance (verbatim fragment) | status in file |
|---|---|---|---|
| S1 · edge-graph reader | typed edge set `{:from :to :kind :attested?}` | "reports **60** `@why` files, **38** `@see-also`, **1** `@how`" | drafted |
| S2 · kernel constructor | `node → D(node)` with `entropy` and `discriminates?` | "same four bindings `checks/how_witness_snatch.clj` returns, unattested edges weight at Beta(1,1), and the entropy of a uniform four-point channel is `log 4`" | drafted |
| S3 · snatch adapter and mirror | facts from the game's design diagrams; mirror = G1-vs-G3 | "witness has positive mass and positive entropy; **mirror has zero mass.**" | drafted; needs S1+S2 |
| S4 · mathematics adapter | facts from the proof apparatus | — | **Held** "until the maths apparatus repair lands" |

### The per-flight guarantee (lines 185–233)
- Line 187–188 (Joe): "the 82 flights are of indicative importance only — what we really need are new guarantees around new flights."
- Line 194–195: "**A frequency computed over closed flights is not falsifiable; a guarantee on the next flight is.**"
- Line 197–200: the 82 flights keep "**fixture design**" role: "the fourteen terminal dispositions, the (π, o) pairing, that 21% of flights lose their π".
- Lines 205–208 (the guarantee):
  > **Every flight's close carries a coverage statement**: the criterion set declared for that flight, and whether the terminal outcome fell inside it. An outcome outside the declared coverage is recorded as `:uncovered` — never omitted.
- **Chain, in Tier-0 shape (lines 214–221):**
  | link | what |
  |---|---|
  | Lean | `CoverageReport.lean` states it (dispatched 2026-08-27) |
  | emitter | the clause joins the emitted contract |
  | Clojure | validates a **new** flight's `007-closed.edn` |
  | mutation test | proves a close that omits the coverage statement is **rejected** |
- Lines 223–229 (what it would have said about 2026-07-15): "Those 22 attempts each recorded *"no addressable entities"* per action class — a form of non-coverage reporting at **action** grain — and then closed with `:outcome :no-selection` and no coverage statement at **flight** grain."
- Line 231–233: "**Acceptance:** the first flight after the clause lands carries a coverage statement, and a hand-mutated close that drops it is refused by the check. No claim is made about any flight before it."

### Honest bounds (lines 237–244)
- "Beta(1,1) is a **prior**, i.e. a stipulation — `S-G3` requires it declared in the artefact, not absorbed into a number."
- "Whether `@holds-at` nodes are the right outcome space is **unresolved**"
- "the first real graphs will be `@why`/`@see-also` shaped: authority and peerage rather than method decomposition. **Those are not the same relation**"

### Slices section (lines 246–257)
1. "**What the projection actually counts** — enumerate the rungs in the counted evidence projection, and whether the projection states its own coverage. *(discovery, cheap)*"
2. "**The coverage statement** — what it would mean for the evaluate stage to report non-coverage with the discipline of a poor score. Design, not code."
3. "**The formalisation** — likely no new vocabulary: `typedAbsence` and `declaredDomain` at criterion grain. Confirm before writing a module; the modular-order note predicts R5 is *"largely instantiation"* of module 1."
4. "**Do not build a `:warm-customer-pays` counter.** Recorded as a slice so the temptation is on the record as refused."

No per-slice status column exists for these four; statuses appear only in the Lean table at l332–335 (below).

### Tickets cited
`grep -n -E 'ticket|T-[a-z]|tickets/' E-R5-red-ring-fill.md` → **no matches** (exit 1). Files cited under "Related" (l259–265): `futon3/library/problems/satisfied-rungs-are-counted-and-surfaced.flexiarg` ("the ring"), `p4ng/empirics-futon/NOTE-singularity-and-discrimination.md`, `p4ng/empirics-futon/NOTE-patterns-as-problems.md`, `E-R14-red-ring-fill.md`, `mathlib4/DarkTower/WarMachine/GainChain.lean` ("`foldCompliant`, R5's property in embryo"). Also cited in body: `NOTE-a-standard-for-G.md` (l272), `TN-APM-cascades-exist-unused.md` (l124), `checks/how_witness_snatch.clj` (l170), `futon2.aif.cascade_prior` (l288).

### Snatch section / G(π) and S-G clauses (lines 267–370, "Added 2026-08-27")
- Lines 274–276 (quoting `NOTE-a-standard-for-G.md`): "**`G` is earned at action grain and not at policy grain.** The only multi-step prediction repeats a single action, so "policy" collapses to "this action, sustained" — and a sustained action is not a π."
- Lines 278–284: "**S-G2 is now satisfiable by a real artefact.** The pattern-driven policy over *Snatch or Share* produces, under G4 against a snatcher, the action sequence `offer → denounce → offer → denounce → offer`, scoring `+3` where grim trigger scores `−1`."
- Lines 288–291: "`futon2.aif.cascade_prior` already states that two cascades with the same patterns wired differently are different policies. … promoting one pattern in the consultation order, changing no pattern text and no membership, moves the G4 score from `+3` to `−5`."
- **S-G4 (lines 296–298):** `> **S-G4** · a quantity may be emitted as `G(π)` only if it is **sensitive to the policy's wiring** — there exists an alternative ordering of the same components under which it differs. A score no re-wiring can move is not scoring the policy.`
- Lines 306–310: grim trigger G1 vs snatcher `offer → abstain → abstain → abstain → abstain` "**passes S-G2** … **S-G4 refuses it and S-G2 does not**".
- **The definition (lines 314–320):**
  > **G(π)** is the value a policy earns by being run: accumulated at action grain over one realised trajectory, and attributed at policy grain to the pattern cascade that produced the actions. A quantity earns the name only if the trajectory is not a single action sustained (S-G2) and the value moves when the cascade is re-wired (S-G4) — and only if it is computed from a predicted outcome distribution and declared preferences in the first place (S-G1), with every stipulated component declared (S-G3).

### What it says the two Lean modules cover / do not (lines 328–370)
- Line 330: "Two different R5 modules, and it is worth not conflating them:"
- Table (l332–335):
  | module | states | status |
  |---|---|---|
  | `DarkTower/WarMachine/CoverageReport.lean` | R5's **coverage** clause — declare the criterion boundary, type the outside | **built and reviewed** |
  | `DarkTower/WarMachine/PolicyGrade.lean` | S-G2 and S-G4 as checkable predicates over a finished run, with the three Snatch witnesses | **built and reviewed 2026-08-27** (codex-22 `3677281f8b`, review fix `0de75bc6e6`) |
- Lines 337–339: "`CoverageReport` was already in place; what was missing was any statement of the naming discipline, which is what makes `G(π)` refusable rather than merely defined."
- Lines 341–347: "`PolicyGrade` proves five things, no `sorryAx`, no Mathlib. Two are general and depend on no axioms at all: **S-G4 forces a policy space with more than one point** … **A singleton-indexed score family therefore fails S-G4**, whatever it scores."
- Lines 349–352: "The first draft had the refusal resting on the choice of `Unit` as the wiring type, which is true but puts the argument in the type rather than in a proof."
- Lines 356–360 (bound 1): "**The wiring type does not witness that its values re-order the same components.** `PatternWiring` has two constructors and nothing forces them to be two orderings of one collection."
- Lines 362–370 (bound 2): "**A wiring-insensitive score may belong to a robust policy rather than a degenerate one.** … **S-G4 is a condition on the measurement, not a verdict on the policy.**"

---

## 4. LEAN — `mathlib4/DarkTower/WarMachine/`

Directory contents: `CascadeOrder.lean CommitmentTemperature.lean ContractEmitter.lean CoverageReport.lean GainChain.lean PolicyGrade.lean`.

Git history of the two files (`git log --format='%h %ad %s' --date=short -- CoverageReport.lean PolicyGrade.lean`, read-only):
```
b78ebc428b 2026-08-27 WarMachine: name the fixture polarities in each module
0de75bc6e6 2026-08-27 PolicyGrade: make the S-G4 refusal a theorem, not a type choice
3677281f8b 2026-08-27 feat(war-machine): formalize policy-grade naming
7b5e7e4769 2026-08-27 Review: declaresCoverage was vacuous; make it state the boundary
dca0c8a52f 2026-08-27 Model typed criterion coverage reports
```

### 4a. `CoverageReport.lean` (239 lines; `import DarkTower.WarMachine.GainChain`, l5; namespace `DarkTower.WarMachine.CoverageReport`, l38)

**Module docstring (l7–36), verbatim excerpts:**
- l8–12: `# R5 coverage reports` / "R5 requires an evaluator to declare the boundary of its criterion set and to emit a typed `uncovered` report for an outcome outside that boundary.  Silence is not a poor score and is not a coverage report."
- l14–17: "The model is parametric in the outcome space.  Its intended carriers include the seven Snatch flowchart leaves, the fourteen War Machine flight dispositions, and the mathematics outcomes `closed`, `tier-a`, `tier-b`, `defective`, plus series-level `void`.  None is built into this module."
- l19–23: "This is the family-2/family-5 vocabulary from `GainChain` applied at criterion grain.  `reportOccurrence` and `criterionSelection` adapt a criterion report to the existing `inhabitedHandle`, `typedAbsence`, and `declaredDomain` predicates; they do not introduce a parallel notion of presence or typed absence."
- l25–27: "The model rules out silent non-coverage and the naive repair that merely adds one criterion.  It does not model probability, entropy, kernels, the quality of a score, or whether a declared criterion is the right one."
- **Fixture polarities (l29–35):**
  > * accepting — `coverage_reported_nonvacuous`
  > * refusing-broken — `warm_customer_pays_uncovered_and_unrecorded_is_refused`
  > * refusing-plausible-fix — `adding_a_channel_does_not_satisfy_coverage` (adding the one known missing outcome is the tempting repair and leaves the next one silent)

**Every declaration (kind · name · line):**
| kind | name | line | note |
|---|---|---|---|
| structure | `CriterionSet (Outcome : Type)` | 44 | field `covers : Outcome → Bool`; docstring: "A criterion set declares its boundary as data.  A Boolean function is total: every outcome receives an explicit covered/not-covered answer." |
| inductive | `Report` | 49 | `scored (value : Int) \| uncovered \| absent`; docstring: "`uncovered` is typed evidence; `absent` is the silence that R5 refuses." |
| def | `declaresCoverage` | 67 | `∃ outcome, criteria.covers outcome = false`; docstring l55–66: "**The set names at least one outcome it does not cover.** An earlier draft stated totality … provable of every criterion set whatsoever. … A set claiming total coverage is the failure mode, not the satisfied case — Klarna measured four real metrics at scale and none of them reported what they did not cover." |
| def | `criterionSelection` | 72 | → `ProducerSelection` with `tick := ⟨0⟩`, `mission := ⟨"criterion-outcome"⟩`, `producer := .groundedDial`, `inDomain := fun _ => criteria.covers outcome`, `preconditionDischarged := true`; docstring: "Adapt criterion membership to family 5's declared-domain predicate." |
| def | `reportOccurrence : Report → FoldOccurrence` | 82 | `.scored v` → `realizedLeg := .measured value`; `.uncovered` → `realizedLeg := .domainMismatch`; `.absent` → `consumed := none`; all `gainMoved := false`; docstring: "Adapt the report to family 2's handle and family 5's typed absence." |
| def | `outsideIsTyped` | 108 | `∀ outcome, criteria.covers outcome = false → evaluate outcome = .uncovered` |
| def | `coverageReported` | 116 | `declaresCoverage criteria ∧ outsideIsTyped criteria evaluate ∧ ∀ outcome, inhabitedHandle (reportOccurrence (evaluate outcome)) ∧ typedAbsence (reportOccurrence (evaluate outcome)) (criterionSelection criteria outcome) ∧ (criteria.covers outcome = true → declaredDomain (criterionSelection criteria outcome))`; docstring l113–115: "R5's chain property." |
| structure | `Verdict (ProblemOutcome InstrumentFinding : Type)` | 130 | fields `problemOutcome`, `instrumentFinding`; "keeps the problem result separate from what the instrument learned while producing it." |
| def | `markWithoutForce` | 137 | `(mark before).problemOutcome ≠ before.problemOutcome ∧ (mark before).instrumentFinding = before.instrumentFinding` |
| inductive | `CustomerOutcome` | 147 | `routineSale \| warmCustomerPays \| nextUnanticipatedSignal` |
| def | `customerCriteria` | 153 | covers routineSale only |
| def | `silentCustomerEvaluation` | 159 | warmCustomerPays ↦ `.absent`, nextUnanticipatedSignal ↦ `.absent` |
| theorem | `warm_customer_pays_uncovered_and_unrecorded_is_refused` | 166 | `¬ coverageReported customerCriteria silentCustomerEvaluation` |
| def | `oneChannelLarger` | 175 | covers routineSale and warmCustomerPays; docstring l172–174: "The naive repair adds the known missing channel, but the next outcome outside the enlarged set is silent again.  Klarna is the external analogue" |
| def | `oneChannelLargerEvaluation` | 181 | nextUnanticipatedSignal ↦ `.absent` |
| theorem | `adding_a_channel_does_not_satisfy_coverage` | 186 | `¬ outsideIsTyped oneChannelLarger oneChannelLargerEvaluation` |
| inductive | `ProblemOutcome` | 192 | `solved \| void` |
| inductive | `InstrumentFinding` | 197 | `clean \| defectiveProblem` |
| def | `defectiveVerdict` | 202 | `⟨.solved, .defectiveProblem⟩` |
| def | `voidProblem` | 205 | sets `problemOutcome := .void` |
| theorem | `void_retains_the_instrument_finding` | 211 | `markWithoutForce voidProblem defectiveVerdict`; docstring: "APM's defective-problem discipline: problem outcome VOID, instrument findings RETAINED." |
| def | `reportingEvaluation` | 215 | warmCustomerPays ↦ `.uncovered`, nextUnanticipatedSignal ↦ `.uncovered` |
| theorem | `coverage_reported_nonvacuous` | 222 | `coverageReported customerCriteria reportingEvaluation` |
| `#print axioms` | ×4 | 234–237 | for the four theorems |

No `abbrev` in the file (`grep -c abbrev` → 0).

### 4b. `PolicyGrade.lean` (175 lines; no imports; namespace `DarkTower.WarMachine.PolicyGrade`, l41)

**Module docstring (l6–39), verbatim excerpts:**
- l7: `# When a run earns the name policy grade`
- l9–11: "This module formalises two naming conditions for `G(π)`.  S-G2 refuses an action sequence that only repeats one action.  S-G4 requires the score to change under some alternative wiring of the same policy components."
- l13–16: "The model contains no probability, no preferences `C`, and no expected free energy.  S-G1 is also out of scope: it concerns the provenance of a predicted distribution, whereas this module concerns naming discipline over a finished run."
- l18–25: "Two limitations, recorded rather than repaired.  `Wiring` does not itself witness that its values are re-orderings of the *same* components; … And a score that no re-wiring moves may belong to a robust policy rather than a degenerate one.  S-G4 refuses the *name* in both cases, because the measurement supplies no evidence that the wiring produced the number — a condition on the measurement, not a verdict on the policy."
- **Fixture polarities (l27–32):**
  > * accepting — `pattern_driven_g4_snatcher_earns_policy_grade`
  > * refusing-broken — `grim_trigger_sharer_refused_by_sg2`
  > * refusing-plausible-fix — `grim_trigger_snatcher_passes_sg2_fails_sg4` (making the actions differ is the obvious way to satisfy S-G2 and is not enough)
- l34–38 (Vocabulary): "This module defines its own vocabulary deliberately: it is naming discipline over a finished run, not a requirement about a fold occurrence, so it adapts to no `GainChain` family and imports nothing."

**S-G statements in PolicyGrade:** S-G2 (l9–10, l49 "S-G2's refused case: every action observed in the run is identical."), S-G4 (l10–11, l54 "S-G4: at least one change of wiring changes the resulting score.", l59–60 "**S-G4 forces a policy space with more than one point.**"), S-G1 (l14 "out of scope"). **S-G3: not mentioned in PolicyGrade.lean** (`grep -c 'S-G3' PolicyGrade.lean` → 0). S-G1 and S-G3 are stated only in the excursion (l314–320, l237).

**Every declaration:**
| kind | name | line | statement |
|---|---|---|---|
| structure | `Run (Action Score : Type)` | 44 | `actions : List Action`, `score : Score` |
| def | `sustainedSingleAction` | 50 | `∀ a ∈ run.actions, ∀ b ∈ run.actions, a = b` |
| def | `wiringSensitive` | 55 | `∃ wiring₁ wiring₂, scoreUnder wiring₁ ≠ scoreUnder wiring₂` |
| theorem | `wiringSensitive_needs_two_wirings` | 61 | `wiringSensitive scoreUnder → ∃ wiring₁ wiring₂ : Wiring, wiring₁ ≠ wiring₂` |
| theorem | `singleton_wiring_fails_sg4` | 71 | `(scoreUnder : Unit → Score) : ¬ wiringSensitive scoreUnder` |
| def | `earnsPolicyGrade` | 78 | `scoreUnder observedWiring = run.score ∧ ¬ sustainedSingleAction run ∧ wiringSensitive scoreUnder` |
| inductive | `Action` | 84 | `offer \| abstain \| denounce \| stop` |
| def | `grimTriggerSharer : Run Action Int` | 92 | `⟨[.offer, .offer, .offer, .offer, .offer], 5⟩` |
| def | `hardcodedSharerScore : Unit → Int` | 96 | `fun _ => 5` |
| theorem | `grim_trigger_sharer_refused_by_sg2` | 99 | score = 5 ∧ sustainedSingleAction ∧ ¬ earnsPolicyGrade |
| def | `grimTriggerSnatcher` | 110 | `⟨[.offer, .abstain, .abstain, .abstain, .abstain], -1⟩` |
| def | `hardcodedSnatcherScore : Unit → Int` | 114 | `fun _ => -1` |
| theorem | `grim_trigger_snatcher_passes_sg2_fails_sg4` | 118 | score = −1 ∧ ¬ sustainedSingleAction ∧ ¬ wiringSensitive ∧ ¬ earnsPolicyGrade |
| inductive | `PatternWiring` | 136 | `observed \| onePromoted`; "The two measured precedence orders of the same twelve patterns." |
| def | `patternDrivenSnatcher` | 142 | `⟨[.offer, .denounce, .offer, .denounce, .offer], 3⟩` |
| def | `patternScoreUnder : PatternWiring → Int` | 146 | `.observed => 3`, `.onePromoted => -5` |
| theorem | `pattern_driven_g4_snatcher_earns_policy_grade` | 152 | score = 3 ∧ patternScoreUnder .onePromoted = −5 ∧ earnsPolicyGrade … |
| `#print axioms` | ×5 | 169–173 | |

No `abbrev`.

### 4c. Related: `ContractEmitter.lean` and `GainChain.lean`
- `ContractEmitter.lean:6` `import DarkTower.WarMachine.CoverageReport`.
- `ContractEmitter.lean:22–26`: "**The `reserved` list is this document declaring its own boundary.**  R5 asks a criterion set to name at least one outcome it does not cover; four families are named as outside, each with a typed reason.  A single top-level `warMachineCompliant` conjunction could not do this — a conjunction has nowhere to put a boundary".
- `ContractEmitter.lean:123–136` `def coverageClauseJson` — `("id", "R5")`, `("name", "coverage-reported")`, `("lean-predicate", CoverageReport.coverageReported)`, conjuncts `[CoverageReport.declaresCoverage, CoverageReport.outsideIsTyped, GainChain.inhabitedHandle, GainChain.typedAbsence, GainChain.declaredDomain]`, `("clojure-locus", "futon2/src/futon2/aif/coverage_check.clj")`.
- `ContractEmitter.lean:160`: the PolicyGrade clause's `clojure-locus` is `("absent", "no-clojure-mirror-yet")`.
- `ContractEmitter.lean:207`: `("coverage-clause", coverageClauseJson)`.
- `GainChain.lean:185` `def foldCompliant (occurrence : FoldOccurrence) (selection : ProducerSelection) : Prop := threadedIdentity … ∧ inhabitedHandle … ∧ durableBeforeFold … ∧ typedAbsence … ∧ dischargedPrecondition …`; docstring l183–184: "Reporting an absence is compliant; skipping the step is not."; l46–47: "`foldCompliant` is separated from `gainChainSound` for the same reason: an out-of-domain mission cannot move the gain, and must still leave a record."

---

## 5. CODE — `futon2/src/futon2/aif/`

### 5a. `efe.clj` (~845 lines)

**ns docstring (l1–30), excerpts:**
- l4–8: "`compute-efe` scores a single `(state, action)` pair by composing R4 (predictive forward model) with the R3c free-energy decomposition. `rank-actions` scores a candidate-action sequence and orders it by the multi-objective controller score (lower = more preferred). The result also exposes `:G-efe` as the canonical risk-plus-ambiguity boundary."
- l10–15: "Contract: contributes to R5 (EFE with at least two principled terms) per `futon2/docs/futon-aif-completeness.md`. The two principled terms required by R5: R5a — pragmatic / risk = G-risk / R5b — epistemic / ambiguity = G-ambiguity"
- l28–30: "Theory: AIF Expected Free Energy decomposition. In the production default, ambiguity is Gaussian observation entropy and risk is outcome divergence from C. The separately named controller controls are not EFE terms."

**defn / def lines (`grep -n -E '^\(defn|^\(def '`):**
- l37 `(defn- ambiguity` — "R5b epistemic term over per-channel predicted variances." Modes `:variance-sum` (arity-1 default) and `:gaussian-entropy` ("Σ_ch ½·ln(2πe·σ²) … Variance is floored at 1e-9"). Body l55–61.
- l69–77 `default-info-weight 0.4`, `default-survival-weight 1.2`, `default-structural-pressure-weight 0.35`, `default-graph-applicability-penalty 1000.0`, `default-graph-body-weight 3.0`, `default-graph-ascent-weight 20.0`, `default-graph-off-map-penalty 0.0`, `default-gap-weight 6.0`, `default-model-uncertainty-weight 1.0`
- l79–82 `legacy-control-mode :controller-augmentation`, `retired-control-mode :telemetry-only`, `legacy-graph-feasibility-mode :score-penalty`, `policy-support-mode :policy-support`
- l90 `default-kl-channel-weights {}`; l97 `default-time-pressure-scale 1.0`
- l99 `capability-satisfied?`, l104 `mission-node`, l107 `mission-applicable?`, l114 `mission-single-cycle-leaf?`, l120 `goal-depths` (private), l134 `mission-ascent-progress`, l151 `graph-control-terms` (computes `:model-uncertainty-bonus` at l185–186: `(* (double model-uncertainty-weight) (double (model-uncertainty-fn mission-id mission)))`), l231 `policy-support-verdict`, l254 `partition-policy-support`, l267 `star-map-contribution?`, l272 `action-target-id`, l282 `gap-control-terms`, l300 `gap-contribution?`, l304 `pre-registered-capability?`, l309 `safe-action?`, l331 `selection-trace-step`, l362 `predictability-bonus` (private), l381 `homeostatic-pressure` (private), **l412 `compute-efe`**, **l808 `rank-actions`**, l828 `rank-star-map-actions`, l837 `select-star-map-action`.

**No defn named `expected-free-energy` or `G` in efe.clj** (`grep -n 'defn.*expected-free-energy\|defn G ' efe.clj` → none). The canonical two-term kernel lives in `core_efe.clj:94 (defn g-efe` (see 5e).

**`compute-efe` docstring (l413–503), key lines:**
- l429–432: `:controller-score <G-risk + G-ambiguity − info-weight×predictability-bonus + survival-weight×homeostatic-pressure − structural-pressure-weight×structural-pressure − gap-exploration-bonus + G-goal-outcome>`
- l435: "Lower :controller-score = more preferred action."
- l495–501: "Result shape (B-2a honest labelling, M-aif-faithfulness §2.2): the output is a MULTI-OBJECTIVE ACTION SCORE WITH AN EFE CORE. `:G-core` (= risk + ambiguity, invariant I3) is the canonical G; `:augmentation-terms` names the eight non-core contributions exactly as they enter `:controller-score` (signed, weighted), and `:controller-augmentation` is their sum. `:controller-score` — the ranking key — is the historical blend, byte-identical across this relabelling; do not read it as canonical EFE (R18/D8)."

**Mode defaults (`:or` map, l537–545):** `ambiguity-mode :gaussian-entropy`, `risk-mode :kl`, `goal-outcome-mode :hinge`, `structural-pressure-mode :controller-augmentation`, `predictability-control-mode legacy-control-mode`, `homeostatic-control-mode legacy-control-mode`, `graph-feasibility-mode legacy-graph-feasibility-mode`, `move-class-intensity-mode :off`. Comment l533–536: "2026-07-08 (Joe-directed): code default aligned to the live-arena canonical modes — a WINNER-CHANGING faithfulness upgrade (real KL / Gaussian-entropy replace the analogical hinge / variance-sum)."

**Risk computation (l565–600):** comment l566–567 ":risk-mode :kl scores risk as Σ_ch w_ch · KL(N(μ_ch,σ²_ch) ‖ C_ch) in nats over the preference densities (pref/c-distribution)." `channel-risk` (l~585) `case risk-mode :kl (reduce + … (pref/kl {:kind :gaussian :mu mu :sigma2 s2} (pref/c-distribution spec :temperature (ch-temp ch))))` else `(:preference-gap-score fe-on-predicted)`; `g-risk (+ channel-risk zone-risk)`; `g-ambig (if learn-action? (:predictive-variance zone-evidence) (ambiguity next-var ambiguity-mode))`.

**Assembly of the total (l~665–735):** `effective-risk (* (- g-risk intrinsic) urgency)`, `risk-control (- effective-risk g-risk)`; `augmentation-terms {:risk-control … :info … :survival … :structural-pressure … :graph-control … :model-uncertainty-bonus (- (:model-uncertainty-bonus graph-terms)) :gap … :goal-outcome g-goal-outcome}`; `g-total-base (+ effective-risk g-ambig info-contribution survival-contribution (if habit-prior? 0.0 sp-contribution) (:graph-control-score graph-terms) (- (:model-uncertainty-bonus graph-terms 0.0)) (- (:gap-exploration-bonus gap-terms)) g-goal-outcome)`; `g-total (if move-class-contribution (+ g-total-base move-class-contribution) g-total-base)`.

**Return map keys (l739–785):** `:G-risk g-risk` (l741), `:G-ambiguity g-ambig` (l742), `:G-goal-outcome g-goal-outcome` (l746), `:G-core (+ g-risk g-ambig)` (l750; comment "the canonical EFE CORE … invariant I3"), `:G-efe (+ g-risk g-ambig)` (l755; comment l751–754: "Honest canonical boundary: risk + ambiguity is the implemented EFE decomposition. The BMR posterior-spread signal is not an expectation of posterior information gain, so it remains quarantined in the controller augmentation and MUST NOT be presented as an EFE leg."), `:controller-augmentation` (l761), `:augmentation-terms` (l762), `:risk-mode risk-mode` (l765), `:ambiguity-mode ambiguity-mode` (l769), `:goal-outcome-mode` (l772), **`:controller-score g-total` (l778)**.

**`rank-actions` (l808–826):** "Score a sequence of candidate actions and order them by controller-score ascending. Returns a vec of `compute-efe` outputs each carrying `:rank` (1 = most preferred)." Body: `(partition-policy-support …)` → `(map #(compute-efe state % opts))` → `(sort-by :controller-score)` → `map-indexed :rank`; metadata `{:policy-support/excluded (vec excluded)}`.

**`:G-total` / `:G-pragmatic` / `:G-epistemic` in current source:** `grep -rn -E ':G-total|:G-pragmatic|:G-epistemic' src/futon2/` → **not found**. The keys now carrying those slots are in `free_energy.clj:35 compute-controller-diagnostics` (returns `{:controller-score g-total :preference-gap-score g-pragmatic :coverage-uncertainty-pressure g-epistemic :per-channel … :avoided-active …}`, l72–75; `g-total (+ (* 0.65 g-pragmatic) (* 0.35 g-epistemic))` l63; `g-epistemic (+ (* 0.4 (- 1.0 (:loop-health obs 0.0))) (* 0.3 (- 1.0 (:attack-coverage obs 0.0))) (* 0.3 (- 1.0 (:support-coverage obs 0.0))))` l59–61). `trace.clj:28` schema: `:free-energy {:preference-gap-score :coverage-uncertainty-pressure :controller-score :per-channel :avoided-active}`; `trace.clj:31`: `:ranked-actions [{:action :G-risk :G-ambiguity :controller-score :rank}]`. Per-action persisted whitelist `trace.clj:110–123` (`select-keys r [:action :G-risk :G-ambiguity :predictability-bonus :homeostatic-pressure :structural-pressure :G-goal-outcome :gap-exploration-bonus :graph-control-score :G-core :G-efe :score-provenance :risk-mode :ambiguity-mode … :controller-score :rank :time-pressure :horizon-steps])`. The `:free-energy` record slot is the current-observation diagnostic, not a per-action EFE.

### 5b. `coverage_check.clj` (~107 lines)

**ns docstring (l1–6):**
> "Pure validation of the R5 coverage statement carried by a flight close. A criterion statement maps named outcomes to booleans.  At least one false entry declares the criterion set's boundary.  Missing data is reported as :unwitnessable; data that is present but contradicts the close is :failed."

**defs / defns:** l9 `clause-id :r5/coverage-reported`; l11 `result-kinds #{:passed :failed :unwitnessable}`; l12 `statement-path [:payload :coverage-statement]`; l13 `terminal-outcome-path [:payload :judgment :outcome]`; l14 `statement-fields #{:criteria :outcome :inside? :report}`; l16 `result` (private); l23 `unwitnessable` (private); l27 `failed` (private); **l31 `check-coverage`**; **l95 `summarize`**.

**`check-coverage` docstring (l32–43):** "Check R5's coverage clause for one close. The coverage statement has this shape: `{:criteria {outcome true, known-outside-outcome false} :outcome outcome :inside? true :report :scored}` An outside outcome instead carries `:inside? false` and `:report :uncovered`.  The terminal outcome remains at `[:payload :judgment :outcome]`."

**Failure/unwitnessable reasons in `cond` (l45–92):** `:unwitnessable` with `:missing-field` `:coverage-statement` / any of the four fields / `:judgment/outcome`; `:failed` with `:why` ∈ `:coverage-statement-not-a-map`, `:criteria-not-a-map`, `:criteria-values-not-boolean`, `:criterion-set-declares-no-boundary`, `:outcome-omitted-from-criterion-set`, `:coverage-outcome-does-not-match-terminal`, `:inside-not-boolean`, `:inside-contradicts-criterion-set`, `:outside-outcome-not-recorded-as-uncovered`, `:covered-outcome-not-recorded-as-scored`; else `:passed`.

**`summarize` docstring (l96–99):** "Return per-result counts and the individual clause results. The summary intentionally has no aggregate success boolean: callers must retain the difference between failed and unwitnessable clauses."

**Callers / tests / emitters:** `grep -rn -E 'coverage-check|check-coverage|coverage-statement|r5/coverage-reported' src test scripts` (excluding the ns itself) → **no hits in `src/`, `test/`, or `scripts/`**. `ls test/futon2/aif/ | grep -i coverage` → nothing. Only textual references: `holes/labs/wm-contract/claude-13-repl-turns-2026-08-27/turn-2026-08-27T1538Z.txt:1,16` (review turn: "REAL existing close -> :unwitnessable :missing-field :coverage-statement"). `grep -rl 'coverage-statement' data/wm-full-loop` → 0 files (of 9 entries). Nothing in `futon3c/src` or `futon3c/test` references it (`grep -rn 'coverage-statement|r5/coverage-reported|check-coverage'` → only an unrelated `check-coverage-ratchet` in `futon3c/src/futon3c/logic/ratchet.clj:31`).

### 5c. `c_vector.clj` — what C is

**ns docstring (l1–34), excerpts:**
- l2: "E-C-vector-live: the War Machine's preference component **C**, kept LIVE."
- l4–8: "The static channel-range C (`futon2.aif.preferences/preferences`) is the *floor*; this ns adds the **goal-outcome** half of C — Friston's preferences over goal-satisfaction outcomes — DERIVED from the live goal/hole corpus in substrate-2 (:7071) rather than hand-set, kept fresh by a corpus-signature freshness guard, and contributed to EFE's risk as an additive term."
- l24–28: "Risk is **static** (distance of the CURRENT corpus from C). The **predictive** risk (a policy π's predicted outcomes vs C — the canonical KL term) is the W1-gated follow-on: it needs the forward model to predict goal-progress under π, i.e. the goals↔methods PROOF join (M-populate-substrate-2 D4). The seam is `goal-outcome-risk` — see its :TODO. Do not remove it (Joe, 2026-06-26)."
- l30–32: "read-only against :7071 (zero writes); degrades to [] (⇒ the static floor) on an unreachable store"

**defn list:** l40 `boilerplate-if` (const); l46 `c-entry`; l60 `frac`⁻; l70 `divergence`⁻; l82 `current-outcome`⁻; l93 `risk-of`; l105 `fetch-entities`⁻; l117 `cap-meta?`; l120 `entries-from-corpus`; l150 `corpus-signature-of`⁻; l160 `derive-stated`; l179 `current-c-vector`; l196 `overlay-dir`; l200 `overlay-files`; l203 `read-overlay-channels`; l214 `refresh!`; l242 `live-signature`; l248 `stale?`; l258 `freshness-check`; l274 `maybe-refresh!`; l282 `default-debounce-ms`; l288 `ensure-belly-fresh!`; l314 `default-goal-outcome-weight` (1.0); **l320 `goal-outcome-risk`** ("STATIC risk … action-independent: a constant offset across policies"); l344 `norm-id`⁻; l353 `id-tokens`⁻; l370 `ref-tokens`⁻; l386 `fetch-durable-join*`⁻; l442 `build-durable-adv`; l466 `durable-join-stats`; l477 `advanced-outcome-ids`; l506 `ref-advanced?`; l512 `credit-satisfy-prob`; **l527 `predictive-goal-outcome-risk`** ("The CANONICAL EFE goal-outcome risk: weight·MEAN divergence of the PREDICTED outcomes under the policy from C … Action-dependent ⇒ it re-ranks policies."); l575 `kl-risk-of`; **l595 `predictive-goal-outcome-risk-kl`**; l633 `merge-entries`. (⁻ = `defn-`.)

### 5d. `epistemic_value.clj` (~100 lines)

**ns docstring (l1–13), verbatim:**
> "Pure policy-conditioned expected information gain. For a policy π with predicted observations Q(o|π), prior Q(s|π), and the posterior that would follow each possible observation Q(s|o,π): EIG(π) = Σ_o Q(o|π) KL[Q(s|o,π) || Q(s|π)]. HONESTY: this kernel is the canonical information-theoretic calculation, but it is not wired into the controller. Wiring requires a defensible policy-conditioned observation distribution and simulated posterior update for each observation. Current posterior spread and gap lookup do not satisfy that contract and must not be passed off as EIG."

**defns:** l17 `probability-distribution!`⁻; l32 `kl-divergence` ("KL[posterior || prior] in nats. Fails closed when posterior mass lies outside prior support."); l49 `posterior-mixture`⁻; **l64 `expected-information-gain`**; l92 `policy-information-gains`.

**`expected-information-gain` docstring (l65–73):** "Compute policy-conditioned EIG in nats. Input: `{:prior Q(s|π) :predicted-observations Q(o|π) :posteriors {o Q(s|o,π)}}` The posterior mixture must reconstruct the prior. This Bayes-coherence gate prevents arbitrary posterior maps from manufacturing apparent information." Throws when `mismatch ≥ tolerance` (1.0e-9).

**Callers:** `grep -rln 'epistemic-value' futon2/src futon2/test` → `futon2/src/ants/aif/policy.clj`, `futon2/src/futon2/aif/epistemic_value.clj`, `futon2/test/futon2/aif/epistemic_value_test.clj`. No file under `futon2/src/futon2/aif/` other than the ns itself requires it (r18-badges `:model-uncertainty-bonus :repair-built`: "UNWIRED").

### 5e. `core_efe.clj` — the domain-agnostic kernel
- ns docstring l2–19: "Shared domain-agnostic AIF core: unit-pure Expected Free Energy (R5). The canonical 2-term EFE core: G_efe = risk + ambiguity where: risk (mode :kl) = Σ_ch w_ch · KL(N(μ_ch, σ²_ch) ‖ N(C_μ_ch, σ²_C_ch)) / ambiguity (mode :gaussian-entropy) = Σ_ch ½·ln(2πe·σ²_ch) … This is the SINGLE implementation both ports consume. It was lifted verbatim from ants.aif.efe … Faithfulness tag: FEP-derived (R5 — unit-pure G_efe)."
- defns: l31 `gaussian-entropy`, l40 `gaussian-kl`, l56 `ambiguity`, l75 `risk`, **l94 `g-efe`** ("Unit-pure Expected Free Energy: risk + ambiguity (the 2-term canonical core)." args: parallel seqs `means variances c-means c-variances` + opts `{:weights :risk-mode :kl :ambiguity-mode :gaussian-entropy}`).
- Callers: `grep -rln 'core-efe' src` → `src/ants/aif/efe.clj` and `core_efe.clj` itself only. `futon2.aif.efe` does **not** require `core-efe` (its `:require` at l31–35 lists `forward-model`, `free-energy`, `preferences`, `c-vector`, `move-class-intensity`).

---

## 6. BADGES

### 6a. `futon2/data/r18-badges.edn` (schema v2, `:date "2026-07-03"`, l17–24)

Requirement map (l48–52):
```
:G {:requirement "R5" :quantities [:G-risk :G-ambiguity]}
:GCTRL {:requirement "R5" :quantities [:controller-score :predictability-bonus :homeostatic-pressure
                                       :structural-pressure
                                       :graph-control-score :gap-exploration-bonus
                                       :model-uncertainty-bonus]}
```
(l69: `:WANT {:requirement "R19" :quantities [:G-goal-outcome]}`)

**`:G-risk` (l97–110):**
- `:badge :derived-from-FEP`
- `:claims "EFE risk = D_KL[Q(o|π) ‖ C]"`
- `:code-ref "futon2/src/futon2/aif/free_energy.clj:44"`
- `:computes "truncated and renormalised per-channel Gaussian KL vs C in nats; urgency and intrinsic credit are separate controller augmentation terms"`
- `:repair "predict outcome distribution Q(o|π) and KL against a normalised preference C"`
- `:repair-built ":risk-mode :kl FLIPPED LIVE 2026-07-03 (operator decision; …)"`

**`:G-ambiguity` (l133–144):**
- `:badge :derived-from-FEP`
- `:claims "EFE ambiguity  E_Q(s|π)[H[P(o|s)]]"`
- `:code-ref "futon2/src/futon2/aif/efe.clj:38"`
- `:computes "LIVE :gaussian-entropy (8ae1090): Σ_ch ½·ln(2πe·σ²) under Q(s|π) — exact under the declared Gaussian channel model, unscaled; FUTON_WM_AMBIGUITY_MODE=variance-sum hatch → historical variance-sum proxy"`
- `:repair "use ½ln(2πe σ²) under Q(s|π) → :derived-from-FEP"`
- `:note` fragment: "influence MEASURED 0% flips/674 ticks (within-tick sd 0.0039 — argmin hears σ not μ, E4)"

**`:controller-score` (l178–188):**
- `:badge :engineering-control`
- `:claims "multi-objective controller score with an EFE core"`
- `:cite "engineering controller objective; G-efe separately reports risk + ambiguity"`
- `:code-ref "futon2/src/futon2/aif/efe.clj:449"`
- `:computes "linear sum of 8 terms in incommensurate units at hand-set weights; risk+ambiguity core diluted"`
- `:repair "complete: G-efe exposes risk + ambiguity and controller-score explicitly names the noncanonical blend"`
- `:note` fragment: ":G-core = risk + ambiguity now emitted + persisted per candidate (D2, I3 checked live: 0 violations). Honest label: MULTI-OBJECTIVE SCORE WITH AN EFE CORE"

**`:G-goal-outcome` (l231–243):**
- `:badge :derived-from-FEP`
- `:claims "predictive goal-satisfaction risk KL[Q(o|π) || C]"`
- `:code-ref "futon2/src/futon2/aif/c_vector.clj:406"`
- `:computes "all open categorical and range goals are scored in a common Bernoulli satisfied/unsatisfied outcome space using exact KL in nats"`
- `:repair "complete for the declared binary goal-satisfaction model; calibrate temperature from outcome frequencies as a separate statistical refinement"`
- `:note` opens: "SHARPEST relabel — a non-KL explicitly labelled canonical/KL; ns docstring concedes it is deferred · M-evaluate-policies 2026-07-03: 0% flips so far (young, 4% of corpus)."

**`:model-uncertainty-bonus` (l84–96):**
- `:badge :engineering-control`
- `:claims "controller model-uncertainty bonus"`
- `:cite "engineering exploration control; not canonical EFE"`
- `:code-ref "futon2/src/futon2/aif/efe.clj:180"`
- `:computes "model-uncertainty-weight × posterior-spread(mission); the default provider is zero and the optional pattern-grain provider aggregates posterior standard deviation"`
- `:repair "complete: renamed and kept outside G-efe; a future EIG term must compute expected posterior entropy reduction"`
- `:repair-built "B3 PURE KERNEL 2026-07-13: epistemic-value/expected-information-gain computes policy-conditioned Σ_o Q(o|π) KL[Q(s|o,π)||Q(s|π)] and fails closed unless the predicted posterior mixture reconstructs the prior. UNWIRED: the controller still lacks a defensible per-policy observation distribution and simulated posterior updates; no badge change claimed."`

**`:G-total`:** `grep -n ':G-total' r18-badges.edn` → **not found**.

No badge in `r18-badges.edn` mentions coverage report, criterion set, or "uncovered" (`grep -n -i -E 'criterion|uncovered|coverage-report' r18-badges.edn` → the only "coverage" entries are `:coverage-score-delta` (l169, "negative coverage fraction, used as an engineering improvement score") and the R16 `:quantities [:coverage-score-delta :cascade-score]` at l61).

### 6b. `p4ng/empirics-futon/wr-overlay.edn`
- l13: `{:as-of "2026-08-22"`
- l39: `{:node "R5"  :wr "WR-25" :holds false :note "good news gets the same discipline as bad -- :warm-customer-pays satisfied, uncounted, unsurfaced"}`
- header l11: ";; :holds is a claim about the running stack on :as-of, not an intention."

### 6c. `futon2/holes/aif-r1-r16-pattern-map.md` — R5 row (l31)
> | **R5** | EFE with ≥2 principled terms | `expected-free-energy-scorecard`, `belief-aware-risk-term`, `predictive-entropy-as-ambiguity` | ◐→ **EIG ARMED (latent until R10)** | Disaggregated (`b28ebf3`): NOT ◐-in-the-middle. **nats-risk** (`:kl`, the hard part, arena-flipped 2026-07-04) ✓ · **nats-ambiguity** (`:gaussian-entropy`, the one derived-from-FEP quantity; now the CODE default too, `90dedca`) ✓ · **EIG** = the open third — now **BUILT (dark, `25b1b8f`, codex-2/claude-5)**: `{constellation→stddev} → mission-EIG → −λ·EIG` at pattern grain (194 missions resolve to non-zero EIG). `G = risk + ambiguity − EIG` has all three legs; the hard part of R5 is done. `:pattern-grain-eig?` is now **default ON** (`a299543`, Joe-authorized) — a Tier-1 (deliberation-side) arm; **latent until R10** runs. Caveat: the EIG is the coarse ~1/√evidence signal (not fine-grained). |

Also l121: `| **R5** | `R5-decomposition` | **0** | `EIG` — **ARMED** (`a299543`, default-on): pattern-grain `{constellation→stddev} → mission-EIG → −λ·EIG`; Tier-1, latent until R10 |`

The pattern map's R5 row does not mention coverage, criterion set, or WR-25 (`grep -n -i 'coverage\|criterion\|WR-25' aif-r1-r16-pattern-map.md` restricted to l31/l121 → none).

---

## 7. CORPUS — `futon2/data/wm-trace/`

**Command** (reader loop over all top-level forms; `clojure.edn/read` on a `PushbackReader` with `:eof` sentinel and `:default (fn [t v] v)`):

```bash
cd /home/joe/code/futon2/data/wm-trace && cat > /tmp/r5-corpus.bb <<'EOF'
(require '[clojure.edn :as edn] '[clojure.java.io :as io])
(defn read-all [f]
  (with-open [r (java.io.PushbackReader. (io/reader f))]
    (loop [acc []]
      (let [x (edn/read {:eof ::eof :default (fn [t v] v)} r)]
        (if (= x ::eof) acc (recur (conj acc x)))))))
(def files (sort (filter #(re-matches #"wm-trace-2026-.*\.edn" (.getName %)) (file-seq (io/file ".")))))
(def recs (mapcat (fn [f] (map #(assoc % ::file (.getName f)) (read-all f))) files))
(println "files" (count files) "records" (count recs))
(doseq [[ks n] (sort-by (comp - val) (frequencies (map #(some-> (:free-energy %) keys sort vec) recs)))]
  (println "  " n ks))
(println (frequencies (mapcat (fn [r] (filter #(re-find #"coverage|criterion|not-covered" (name %)) (keys r))) recs)))
(println (some :coverage-report recs) (some :not-covered recs) (some :criterion recs))
(println (frequencies (filter #(re-find #"coverage|criterion|not-covered" (name %)) (filter keyword? (tree-seq coll? seq (vec (map #(dissoc % ::file) recs)))))))
(let [s (sort (map #(count (:ranked-actions %)) recs))] (println "min" (first s) "median" (nth s (quot (count s) 2)) "max" (last s)))
(def both (filter #(and (contains? (:free-energy %) :G-total) (contains? (:free-energy %) :controller-score)) recs))
(println "both keys:" (count both) "equal:" (count (filter #(= (get-in % [:free-energy :G-total]) (get-in % [:free-energy :controller-score])) both)))
(def ra-both (for [r recs a (:ranked-actions r) :when (and (contains? a :G-total) (contains? a :controller-score))] a))
(println "ranked-action entries with both:" (count ra-both) "equal:" (count (filter #(= (:G-total %) (:controller-score %)) ra-both)))
EOF
bb /tmp/r5-corpus.bb
grep -c -E 'coverage-report|:not-covered|:criterion|coverage-statement' wm-trace-2026-*.edn | grep -v ':0$'
```

**Results (run 2026-08-30):**
- Files: **52** `wm-trace-2026-*.edn` (2026-05-18 … 2026-07-21; plus `wm-shadow-step.json`, not read). Records: **791**.
- **`:free-energy` key sets** (2 distinct):
  - **760** records: `[:G-epistemic :G-pragmatic :G-total :avoided-active :per-channel]` — files `wm-trace-2026-05-18.edn` → `wm-trace-2026-07-09.edn`
  - **31** records: `[:avoided-active :controller-score :coverage-uncertainty-pressure :per-channel :preference-gap-score]` — files `wm-trace-2026-07-14.edn` → `wm-trace-2026-07-21.edn`
  - Every record has a `:free-energy` map (no `nil` group).
- **Coverage report / criterion keys:** top-level keys matching `coverage|criterion|not-covered` → `{}` (none). `:coverage-report` → nil; `:not-covered` → nil; `:criterion` → nil. Nested keyword occurrences matching the regex (whole-tree walk): `{:support-coverage 2902, :attack-coverage 2902, :sorry/r3a-likelihood-attack-coverage 381, :sorry/r3a-likelihood-support-coverage 381, :coverage-uncertainty-pressure 31, :coverage-score-delta 4}` — these are observation channels / sorry tags / the renamed `:G-epistemic` / the R16 act-gate delta, not a coverage report. Raw grep for `coverage-report|:not-covered|:criterion|coverage-statement` across all files: **0 files** (grep exit 1).
- **`:ranked-actions` per record:** min **4**, median **112**, max **218** (n = 791; 0 records with `:ranked-actions` nil).
- **`:G-total` vs `:controller-score`:** records whose `:free-energy` carries **both** keys: **0**; ranked-action entries carrying both: **0**. So they never co-occur and equality cannot be tested on the same record; the two names occupy the same slot on either side of the 07-09 → 07-14 boundary. Ranked-action key sets (103,837 entries total): `:G-total` appears in 100,292 entries across 12 key-set shapes (05-18..07-09); `:controller-score` appears in 3,545 entries across 3 shapes (07-14..07-21), e.g. 3535 × `[:G-ambiguity :G-core :G-efe :G-goal-outcome :G-risk :action :ambiguity-mode :augmentation-terms :controller-augmentation :controller-score :gap-exploration-bonus :goal-outcome-mode :graph-control-score :graph-control-score-proxy :graph-feasibility-mode :graph-feasibility-penalty :habit-prior-bias :habit-prior-source :homeostatic-control-mode :homeostatic-pressure :horizon-steps :predictability-bonus :predictability-control-mode :rank :risk-mode :structural-pressure :structural-pressure-mode :time-pressure]`.
- Supplementary (second script `/tmp/r5-corpus2.bb`, same reader): records where `:free-energy :G-total` = rank-1 action's `:G-total`: **0**; where `:free-energy :controller-score` = rank-1 action's `:controller-score`: **0**. Records with `:variational-free-energy`: **31**. Records with `:wm-version`: **109**. Ranked-action `:risk-mode` values: `{nil 90749, :hinge 324, :kl 12764}`; `:ambiguity-mode`: `{nil 100191, :gaussian-entropy 3646}`. Distinct top-level key sets: 12.

---

## 8. MISSION — `futon2/holes/missions/M-formal-war-machine.md`

`grep -n -E 'R5|coverage-clause|CoverageReport|PolicyGrade|S-G1|family 5|family 9|WR-25'` → lines 204, 282, 373, 391, 413, 718, 719, 726, 732, 736, 737, 748, 749, 760, 761, 799, 840, 965, 985, 1199, 1200, 1218, 1677, 1681, 1683, 1808, 1837, 1847, 1849, 1854, 1858, 1946. `WR-25`: not found in this file. `S-G1`: appears only inside "S-G1…S-G4" at l282.

- **l282:** `| **naming discipline** — when a number may be called `G(π)` | declared-domain clauses S-G1…S-G4 | `PolicyGrade.lean`, cross-cutting |`
- **l391–392:** "Several bear directly on rows the map marks dark or open — `epistemic_value` on R5's "open third", `habit_prior` on the one branch where τ can move an argmax."
- **l413:** "This is what makes family 5 a repair rather than a formality."
- **l718:** `| **R5** | `E-R5-red-ring-fill` | `CoverageReport.lean` | **adapts** to families 2 and 5 | yes — `coverage-clause` |`
- **l719:** `| **R5** (G) | same | `PolicyGrade.lean` | **its own** (`Run`, `wiringSensitive`) | **no** |`
- **l725–727:** "**1 · Families 8 and 9 exist only in prose.** §2.1c names them — family 8 is `I(τ ; action) = 0`, family 9 is weights-versus-membership — and neither appears in `GainChain.lean`, in `ContractEmitter.lean`, or in `CommitmentTemperature.lean`"
- **l732–738:** "**2 · Two of four modules are islands.** `CoverageReport` does the alignment properly: `criterionSelection` and `reportOccurrence` adapt a criterion report to the *existing* `inhabitedHandle`, `typedAbsence` and `declaredDomain`, so it adds no parallel notion of presence or absence. `CommitmentTemperature` and `PolicyGrade` each define their own vocabulary and import nothing. For `PolicyGrade` that was deliberate and stated in its packet"
- **l748–749** (polarity table): `| `CoverageReport` | `coverage_reported_nonvacuous` | `warm_customer_pays_…_is_refused` | `adding_a_channel_does_not_satisfy_coverage` |` / `| `PolicyGrade` | `pattern_driven_g4_snatcher_earns_policy_grade` | `grim_trigger_sharer_refused_by_sg2` | `grim_trigger_snatcher_passes_sg2_fails_sg4` |`
- **l758–762:** "The modules do not share a carrier: `CoverageReport` is parametric in an outcome type, `PolicyGrade` quantifies over finished runs, `CommitmentTemperature` over selectors, `GainChain` over fold occurrences."
- **l798–801:** "**A conjunction has nowhere to put a boundary.** … R5 asks a criterion set to name at least one outcome it does not cover; the contract's `reserved` list does exactly that, and as of `06327f99b2` each entry carries a **typed**"
- **l838–842:** "criterion must report is **where the population falls**, and each stage must either pass an element on or record a *typed* reason for dropping it. That is family 5's typed absence lifted from record grain to population grain"
- **l965–967:** "6. **A Clojure mirror, or a typed hole.** `CoverageReport` has `futon2/src/futon2/aif/coverage_check.clj`. Where no mirror exists the contract entry records the absence rather than omitting the clause."
- **l985:** "3. **R6 (family 9, weights versus membership)** and **R2** — designed in their excursions, unbuilt."
- **l1199–1200** (incident-naming table): `| `CoverageReport` | no — though `warm_customer_pays` is drawn from R5's finding, the module does not say so |` / `| `PolicyGrade` | no — its witnesses are chosen runs of an external game |`
- **l1216–1220:** "Nothing above rules out a contract every one of whose clauses refuses a real incident and which still misses the failure that matters next — which is R5's coverage requirement turned on the contract itself. The `reserved` list with its typed `outside-reason` entries is the beginning of that and not the end of it"
- **l1677–1683:** "This is not new vocabulary — `E-R5-red-ring-fill` defines both failure modes of discrimination: > **R14** — a dimension with **no singularity** on it. > **R5** — a **singularity with no dimension to receive it**. **A failure patched locally is R5 exactly.**"
- **l1806–1810:** "Checked across all five WarMachine modules: **every property is about a finished object** — a run (`earnsPolicyGrade`), a selector (`governs`), a report (`coverageReported`), a relation (`acyclicDescent`), a fold occurrence (`gainChainSound`). Not one is about a **sequence** of them."
- **l1835–1838** (Joe, quoted): "…the old known-failing version continues to have the old behaviour, and the new one supersedes it … the R5 changes have broader implications that go beyond one localized change."
- **l1847–1858:** "#### The R5 claim is not rhetorical — checked / `CoverageReport.lean` imports `GainChain` and adapts to three of its predicates: inhabitedHandle × 3    typedAbsence × 3    declaredDomain × 3 / Those are **families 2 and 5**, and both are conjuncts of `gainChainSound` and `foldCompliant`. So a change to coverage semantics is not a local change to R5; it moves predicates that the chain property and the compliance property both depend on. … This is also, note, the *good* case: `CoverageReport` adapts rather than inventing its own vocabulary (standard clause 1)"
- **l1944–1947:** "3. **Refresh the 07-13 map** — it is stale by twelve `aif/*.clj` namespaces, several bearing on rows it marks dark or open (`epistemic_value` on R5's "open third", `habit_prior` on the one branch where τ moves an argmax)."
- **l204** (family 10 note, mentions family 9 but not R5): "family 9 is about the *space consumed* at ⑪, this is about the *structure produced* at ⑳."
- **l373** (mentions family 9, not R5): "family 8 is `I(τ ; action) = 0` on the enacting path, family 9 is the difference"

---

## Not-found summary (commands)

| item | command | result |
|---|---|---|
| "coverage" in the paper's R5 paragraph | `sed -n 235p p4ng/sec-catalog.tex \| grep -c coverage` | 0 |
| "coverage" in completeness `### R5` | `sed -n '125,146p' futon2/docs/futon-aif-completeness.md \| grep -c coverage` | 0 |
| glossary entry on criterion set / coverage report / uncovered | `grep -n -i 'criterion' p4ng/sec-glossary.tex` | only l72 footnote ("the futon2 criterion") |
| tickets cited in excursion | `grep -n -E 'ticket\|T-[a-z]\|tickets/' E-R5-red-ring-fill.md` | none |
| S-G3 in PolicyGrade.lean | `grep -c 'S-G3' PolicyGrade.lean` | 0 |
| `abbrev` in either Lean file | `grep -c abbrev CoverageReport.lean PolicyGrade.lean` | 0, 0 |
| `expected-free-energy` defn in efe.clj | `grep -n 'defn.*expected-free-energy' efe.clj` | none (kernel is `core_efe.clj:94 g-efe`) |
| `:G-total` / `:G-pragmatic` / `:G-epistemic` in current src | `grep -rn -E ':G-total\|:G-pragmatic\|:G-epistemic' src/futon2/` | none |
| callers/tests of `coverage_check.clj` | `grep -rn -E 'coverage-check\|check-coverage\|coverage-statement\|r5/coverage-reported' src test scripts` | none outside the ns |
| `coverage-statement` in any flight close | `grep -rl coverage-statement data/wm-full-loop` | 0 files |
| `:G-total` badge | `grep -n ':G-total' data/r18-badges.edn` | none |
| coverage/criterion keys in wm-trace | script above + raw grep | none (only `:support-coverage`, `:attack-coverage`, `:coverage-uncertainty-pressure`, `:coverage-score-delta`, sorry tags) |
| `WR-25` in mission file | `grep -c WR-25 M-formal-war-machine.md` | 0 |
