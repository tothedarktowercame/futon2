# facts-R14 — pointers and verbatim snippets for the R14 worksheet

**Date collected:** 2026-08-30. **Scope:** facts only; no classification, no
interpretation. Every item carries a file:line pointer; nulls carry the command
that produced them. Corpus counts were produced by a babashka reader loop over
every top-level EDN form (script text reproduced in §7).

---

## 1. PAPER

### 1a. `p4ng/sec-catalog.tex`

`sec-catalog.tex:115` (index table row):

> `& R14 --- Selection Gain as Commitment Temperature & Makes exploration versus commitment an explicit dial. \\`

`sec-catalog.tex:239` (the R14 paragraph, in full):

> `\hypertarget{pat-r14}{}\paragraph{selection gain as Commitment Temperature (R14).}\label{pat:r14} \textbf{If} you want exploration versus exploitation to depend on operational pressure (deadlines, repeated failures, instability), not on randomness alone. \textbf{However} without an explicit dial the agent either thrashes or tunnels, and there is nothing to turn. \textbf{Then} maintain a \emph{commitment temperature} governing how decisively the best-scoring candidate wins: turned down, the system commits to its front-runner; turned up, it keeps the field open --- and couple that dial to diagnostic signals (recent regressions, uncertainty in the belief state, contradiction rate, time pressure). \textbf{Because} a single commitment dial allows systematic control of stochasticity without changing the scoring model at all.  \emph{Invariant to preserve:} exploration is a dial, not a random number generator buried in the selector.`

`sec-catalog.tex:370` (inside the R20 operational-witness paragraph, last sentence):

> `The link from a trip to the commitment dial of R14 --- a wire firing should make the system less sure of itself --- remains chartered but not yet implemented.`

`sec-catalog.tex:376` (LaTeX comment, figure TODO):

> `% internal channels); dashed chartered edge R20 -> R14 (interoceptive gamma).`

### 1b. `futon2/docs/futon-aif-completeness.md`

Line 3 (preamble): "**R1–R13 are ratified criteria; R14–R19 are first-class tracked-open gaps** — R14–R18 surfaced by the 2026-06-24 triangulation against the canonical FEP/AIF decomposition".

Line 353 (status table row):

> `| R14 — Precision over policies (γ) | **✓ as of v0.22 (2026-07-02)** — machinery + LIVE feed + burn-in crossed; γ=1.0 EARNED (8 realized samples, mean-perf 0.0); **variance-evidence tracked open** (§R14 — taken up inside the mining run) | `futon2.aif.policy-precision`: τ_eff = τ_spread/γ; folds R16's `:realized-outcome` with tick-dedup; two integrity bugs review-caught + fixed same day (retired-schema 0.5-pin; rollout-scale leg pinned to coverage-ΔG at caller AND contract). |`

Line 360 (v0.21 honest claim, excerpt): "R14 (γ over policies, absent)".

Lines 362–370 (v0.22 update, excerpt): "**R16 and R14 flip to ✓.** … γ crossed its burn-in the same day (1.0 EARNED, 8 samples, mean-perf 0.0) after two review-caught integrity fixes. … Tracked open: R14 **variance evidence** (γ driven off 1.0 by diverse outcomes — a named workstream inside the mining run)".

Lines 407–429 (`### R14 — Precision over policies (the γ term)`, in full):

> Canonical AIF carries precision (confidence) over the **policy prior** — the γ term that scales how sharply the agent commits to the best policy and is itself inferred from outcomes — not only over observation channels (which is R7).
>
> **Status: SATISFIED as of v0.22 (2026-07-02) — with one tracked-open follow-on.** The checkable form is met: `futon2.aif.policy-precision` scales selection sharpness (`τ_eff = τ_spread / γ`, bounded [0.5, 2.0], 5-sample burn-in) and IS updated from realized policy-outcome history — R16's live `:realized-outcome` trace contract, folded with tick-dedup. As of 2026-07-02: **γ = 1.0 EARNED** (8 realized samples, mean-perf 0.0 — a posterior, not the held prior). Integrity hardening the same day: the retired v0 `:error-history` schema had pinned γ at 0.5 on 8 degenerate samples for 5 days (fixed: `coerce-state`); the expected leg is pinned to coverage-ΔG at both the enact caller and the `realized-outcome-of` contract so no rollout-vs-coverage scale mismatch can recur (claude-10 review must-fix).
> **Variance evidence — CLOSED (v0.23, 2026-07-03T00:18Z).** The closure record exists: **γ = 1.01576** in a live trace record (19 samples, mean-perf 0.0226), traceable to the first non-zero perf sample **+0.4286** — produced when the executor's honest reach extension (fold_engine +6 rules, NL→rule from the live-lane patterns' own THEN clauses, futon3a `ce2b153`; the sweep's 72 rule-candidates were REVIEWED AND REJECTED for this purpose — E-aif-post-mission-mining) made the enacted construction over-deliver against futon2's independent predictor: realized −0.5 vs expected −0.2 on M-bayesian-structure-learning. The WM observed a plan beat its prediction and COMMITTED HARDER (τ_eff sharpened). Confidence earned from outcomes, in production, end to end — the R14 story complete. (Both directions: the hedge side arrives naturally with the first under-delivering pass; the machinery is symmetric and now demonstrably live.)
> **Checkable form (original, for the record).** A γ scaling policy-selection sharpness in `futon2.aif.rollout/select-policy`, updated from realized policy-outcome history (the R13 return channel is the natural feed). ✓

Line 484 (excerpt): "it should also re-check **D** (≈R1, partial) and **E** (≈R12/R14, partial), the other prior matrices the depth-diff under-weighted."

Note: the namespace `futon2.aif.policy-precision` named at line 353 and line 409 does not exist on disk — `ls /home/joe/code/futon2/src/futon2/aif/policy_precision.clj` → `No such file or directory`. The file present is `selection_gain.clj` (see §5); the corpus key `:policy-precision` is present in 216 records (see §7).

---

## 2. GLOSSARY — `p4ng/sec-glossary.tex`

Entry titles and line numbers (`grep -n -o '\\paragraph{[^}]*}'`): 33 entries; those relevant here are at lines 17 (Precision Π), 31 (Softmax and controller calibration), 35 (Control states U and the policy vocabulary), 37 (Policy prior E and habit), 48 (Policy π), 72 (No self-certification), 76 (Strategic mission selection).

### 2a. `sec-glossary.tex:31` — *Softmax and controller calibration* (in full)

> `\paragraph{Softmax and controller calibration.} Softmax turns scores into a probability distribution: $p_i = e^{-G_i/\tau}\big/\sum_j e^{-G_j/\tau}$\eqanchor{softmax}.  Because lower scores are better, softmax is applied to the negated scores, so low-cost actions become high-probability picks; the temperature $\tau$ sets how decisively --- low $\tau$ sharpens toward the best option, high $\tau$ flattens and explores.  A worked example: $G=[1,2,4]$ at $\tau=1$ gives $p\approx[70\%,\,26\%,\,4\%]$, and halving $\tau$ to $0.5$ pushes the best option toward $88\%$. The effective temperature is set through the outcome-feedback selection gain $g$, as $\tau_{\mathrm{eff}}=1/g$; score-spread normalisation is disabled. This is an engineering calibration control, not an inferred variational policy precision.  At the selection seam the learned habit prior enters unscaled by temperature --- the choice is over $-G/\tau_{\mathrm{eff}} + \ln E(\pi)$ --- and each decision records, in its decision explanation, the per-term contributions and which of the two terms governed it.\footnote{Action ranking and calibration are discussed in R14; the scheduled loop records the selected action, score breakdown, and decision explanation in the trace.}`

### 2b. `sec-glossary.tex:37–46` — *Policy prior $E$ and habit* (in full)

> `\paragraph{Policy prior $E$ and habit.} Canonical $E(\pi)$ is the prior probability of an already-allowable policy before the current observations are used to judge it.  It is commonly read as habit: repeated selection can increase a policy's prior probability without establishing that the policy is good.  At the selection seam this prior has the characteristic unscaled seat`
> `\[ Q(\pi) \;\propto\; \exp\!\left(\ln E(\pi)-G(\pi)/\tau\right), \]`
> `so temperature or policy precision modulates the expected-free-energy term, not the habit prior.  The War Machine implements this form exactly but not yet at the canonical policy grain.  It folds selected scheduler actions into stable categories $k(a)=(\text{type},\text{target or target-class})$ and uses a symmetric Dirichlet posterior predictive over the currently feasible menu,`
> `\[ \widehat E(a_i) = \frac{n_{k(a_i)}+\alpha}{\sum_{j\in\Pi_{\mathrm{feasible}}}(n_{k(a_j)}+\alpha)}, \qquad \alpha=1, \]`
> `with no recency decay; duplicate identities divide their category mass.  The resulting $\ln\widehat E$ enters selection unscaled and is persisted with its sufficient statistics and decision explanation.  This is a real learned frequency prior, and live traces show it can govern a choice even when $G$ prefers another candidate.  The residual is grain, not algebra: its categories are mission/action-class identities rather than complete pattern cascades, and the tactical cascade lane does not yet consume it.  Consequently \emph{pattern enters at the uniform prior}, \emph{cascade prior}, and this scheduler-level $\widehat E$ must not be treated as synonyms.\footnote{The pure fold and posterior predictive are in \srcref{futon2/src/futon2/aif/habit_prior.clj}; \srcref{futon2/src/futon2/aif/policy.clj} implements $\ln\widehat E-G/\tau$; live wiring and persistence are in \srcref{futon2/scripts/futon2/report/war_machine.clj}.  The measured case in which the learned prior overruled the $G$ ranking is reconstructed in \srcref{futon2/holes/TN-wm-rank109-explained.md}.}`

### 2c. `sec-glossary.tex:48` — *Policy $\pi$* (in full)

> `\paragraph{Policy $\pi$.} In this paper, a policy $\pi$ is the active-inference name for a pattern language/cascade when that composition is being scored.  Its units are design patterns and their foldable warrants, not generic commands.  It is tempting to read the composition as purely temporal --- do this pattern, then this one, then this one --- but that is the impoverished, tree-shaped reading.  After Alexander's \emph{a city is not a tree}, and its stack-scale analogue \emph{a proof is not a tree}, the cascade is a \emph{semilattice} of patterns: they compose both by sequential dependency (\texttt{BV.seq}, the ``then'') and by cross-cutting co-application (\texttt{BV.copar}, the overlapping meets where a single pattern belongs to several sub-constructions at once).  Empirically the overlap axis dominates --- a coverage-selected cascade carries a handful of sequential edges against many more co-application meets, and some carry no ``then'' at all.  In $S(\pi)=\sum_t \rho^t s(s_t)$\eqanchor{Gpi}, the summation sign means ``add this over the cascade steps.''  The small $s(s_t)$ is the cost of the state reached at step $t$; the large $S(\pi)$ is the total engineering rollout score of the cascade-policy.  The factor $\rho^t$ discounts later steps, so nearer consequences count more --- the same discounted-return idea as in reinforcement learning, letting a distant payoff matter without swamping the immediate situation.  (For $s=[3,2,4]$ and $\rho=0.9$: $3 + 0.9\cdot2 + 0.81\cdot4 = 8.04$.)  This temporal sum scores only the sequential axis; the co-application structure is what the \emph{fold} turns into a wiring diagram, and what the cascade's \emph{wholeness} (coherence $\times$ intensity, after Alexander) measures.  The distinction earns its keep twice over: a pattern that looks poor as an isolated first move may be good as the opening of a longer construction, and a cascade flattened to a linear list folds to an \emph{empty} wiring where the same cascade folded \emph{as a semilattice} yields a full construction (one box per pattern, its meets as \texttt{copar}).  Cascade size is then not a fixed budget but a model choice, $\arg\max_\pi S_{\mathrm{cascade}}(\pi)$ with $S_{\mathrm{cascade}}=\mathrm{coverage\ reward}-\lambda\cdot\mathrm{prior\ cost}$\eqanchor{cascade-F} (coverage reward $5$ against prior cost $2$ at $\lambda=1$ scores $3$): an everything-cascade scores negative --- in information-theoretic terms, selecting nothing accomplishes nothing.\footnote{R13 addresses this one-step-vs-policy-horizon issue: the gap is between scoring only the next action and scoring the pattern-language cascade/policy it opens. \srcref{futon2/src/futon2/aif/close_loop.clj} turns a cascade-lane entry into an act-gate with \texttt{:cascade-score} and \texttt{:coverage-score-delta}; \srcref{futon2/src/futon2/aif/enact.clj} carries \texttt{:shown} into enactment. Fold/cascade checking uses \srcref{futon2/src/futon2/aif/fold_llm.clj}, \srcref{futon2/src/futon2/aif/fold_escrow.clj}, and \srcref{futon2/scripts/fold_author.clj}.  The cascade's semilattice (descent $=$ \texttt{BV.seq}, co-application $=$ \texttt{BV.copar}) is constructed in \srcref{futon3a/holes/labs/M-memes-arrows/cascade_construct.py} (\texttt{construct\_cascade}, \texttt{chosen\_semi\_lattice}) over the pattern phylogeny \srcref{futon6/data/pattern-phylogeny-edges.json}, which emits an engineering cascade score (coverage reward minus prior cost) and Alexander wholeness at each cascade size.}`

### 2d. Other entries mentioning γ / τ / selection gain / precision over policies

`sec-glossary.tex:17` — *Precision Π*, footnote:

> `\footnote{Per-channel evidence precision is the R7 pattern; selection gain $g$ is the R14 pattern and is logged through the War Machine trace path.}`

`sec-glossary.tex:35` — *Control states U and the policy vocabulary* (excerpt): "a policy $\pi$ is an allowable sequence drawn from those controls, and the $E$ vector below places a prior over the already-allowable policies. This paper works one level higher: a design pattern is treated as an elementary or temporally extended \emph{control schema}, while a cascade is the policy composed from those schemas."

`sec-glossary.tex:72` — *No self-certification* (excerpt): "Calibration inherits the same rule in two layers: $L_1$ compares the model's predicted $G$ against its own realised $G$ --- cheap, run every cycle, but self-referential, so it may never be reported as value evidence --- while $L_2$ compares predictions against outcomes the model did \emph{not} produce, and only $L_2$ certifies that the model's value is right."

`sec-glossary.tex:76` — *Strategic mission selection* (excerpt, the τ_S/τ_T sentence): "The resulting R15 hierarchy selects a strategic mission-policy by $Q(\pi_S)\propto E_S(\pi_S)\exp[-G_S(\pi_S)/\tau_S]$, then selects a tactical pattern cascade conditionally by $Q(\pi_T\mid\pi_S)\propto E_T(\pi_T\mid\pi_S)\exp[-G_T(\pi_T\mid\pi_S)/\tau_T]$, with separate identities, evidence, and learned state at the two grains."

No glossary entry uses the word "gamma"/"γ" as a symbol for policy precision: `grep -n -iE 'gamma' sec-glossary.tex` matches only line 33 (`\cite{...gamma1994a...}`). The phrase "precision over policies" does not occur in `sec-glossary.tex` (`grep -n -i 'precision over polic'` → no match); "policy precision" occurs at lines 31 and 41 ("not an inferred variational policy precision"; "temperature or policy precision modulates the expected-free-energy term").

---

## 3. EXCURSION — `futon3c/holes/excursions/E-R14-red-ring-fill.md` (905 lines)

**Header (lines 1–7):** "Opened: 2026-08-26 · claude-13 at Joe's direction, as the successor to `E-R8-red-ring-fill`. Excursion from `futon2/holes/missions/M-formal-war-machine.md`. **Read this first: R14 is not red for R8's reason, and this excursion must not open as a repair.**"

### 3a. The premise as opened (lines 31–60, `## The premise, stated before any work`)

> R8 is red on a **demonstrated defect**. Its problem pattern carries `+ salience: 香, by duration` with dates, an archive path, and a named bearer …
>
> R14 is red on an **unfilled salience hole**, and its own pattern says so:
>
> > `?salience(required)`: No source row supplies a 香 instance for commitment temperature specifically. … **No dated observation records commitment temperature being wrong, costly, or noticed.** Recorded as an unfilled hole rather than argued from the mechanism: hallucinating content into the interval produces false salience.
> >
> > `+ whose problem:` **unknown, and that is the finding.** Of the five rings this is the one for which no party has yet been shown to bear a cost.
>
> — `futon3/library/problems/commitment-temperature-is-instrumented-as-gain.flexiarg`
>
> It also **refuses in advance the obvious shortcut**: the nearest salience instance is WR-27's *"three uninstrumented loops found"*, which the pattern says is generic to the ruling and *"is carried by R8's dead outer loop rather than by this node."* …
>
> **Consequence.** Slice 1 is not a repair and not a formalisation. It is the salience question. A legitimate outcome is *"no bearer found"*, which would make R14 red on an evidence hole rather than a defect

The flexiarg it quotes exists: `/home/joe/code/futon3/library/problems/commitment-temperature-is-instrumented-as-gain.flexiarg` (2021 bytes, dated Aug 25). Its IF/HOWEVER/THEN/BECAUSE verbatim:

> `+ IF: An actuator loop uses commitment temperature to govern a live label-supplied flip.`
> `+ HOWEVER: Commitment temperature is explicitly a gain, but without birth-time instrumentation it can only be diagnosed retroactively.`
> `+ THEN: Record commitment temperature at the flip, link it to the resulting actuator transition, and measure how it changes the next selected action.`
> `+ BECAUSE: A declared gain is operational only when the loop records enough before-and-after evidence to shape it rather than reconstruct it later.`
> `?evidence(required): No source row demonstrates birth-time commitment-temperature instrumentation across an actuator transition.`

Header fields of the flexiarg: `@why war-room/wr-27-a-loop-is-born-instrumented-for-its-gain`, `@holds-at R14`; context: "Red ring at R14, ruling WR-27, established 2026-08-22 in p4ng/empirics-futon/wr-overlay.edn."

### 3b. Status table at 2026-08-26 end of day (lines 9–29)

> **Verdict so far: R14 is red on a *disconnected dial*** — a third kind, distinct from R8's demonstrated defect and from this excursion's opening guess of an evidence hole. On the enacting path no value of τ can change the selected action; that is verified at source, proved in Lean, and bounded information-theoretically.

| | state |
|---|---|
| **slice 2** — where τ_eff is applied | **DONE.** Located by Fable, verified at source by claude-13. `policy.clj:35/223/238/377`, `war_machine.clj:4476/4527` |
| **slice 1** — the bearer | **OPEN.** Four candidates on the table, none filed. `?salience(required)` stays empty pending Joe |
| **slice 3** — the pairing record | **BLOCKED by design.** Do not instrument a cut wire; see repair menu |
| **formalisation** | **BUILT** — `DarkTower/WarMachine/CommitmentTemperature.lean`, 9 theorems, `lake env lean` exit 0, zero `sorry` |
| **company level** | **DONE** — `futon0/analysis/business-models/NOTE-select-column-and-R14.md` |
| **operator level** | **OPEN**, deliberately thin; one uncorroborated self-report |

> **The structural finding, in one line:** the War Machine's working feedback edge changes the candidate *set* (`repair-entry`, `##-Inf`), which argmax respects; R14's dial changes *weights*, which argmax annihilates.

### 3c. The two faces of R14 (lines 62–80, `## The correction this forces to module 1`)

> `NOTE-modular-formalisation-order.md` and `E-R8-red-ring-fill` both state **module 1 = R8 + R14, one module**, on three signs: WR-27 carries `@holds-open R8 R14`; commit `b624242` armed both, one flag named *"R14 live-wire migration"*; and one measurement (`:selection-gain` 1.0 in all 65 occurrences) exposed both.

| face | what it is | status |
|---|---|---|
| **gain-in** | `:selection-gain`, the terminal consumer of R8's realised-outcome fold. `selection_gain.clj` — a scalar, clamped to [0.5, 2.0], burn-in at exactly 1.0 until `min-history` samples accrue | **modelled**, as `gainAdvances` in `DarkTower/WarMachine/GainChain.lean` |
| **temperature-out** | τ_eff = adaptive-temperature(G-spread) / selection-gain, and its effect on the **next selected action** — the pattern's actual demand | **located and modelled, 2026-08-26** — `policy.clj:35`, and `CommitmentTemperature.lean`. *This cell read "not yet located in the code" when the excursion opened; that was wrong, from a truncated grep* |

> the cut does not fall between R8 and R14 — **it falls inside R14**, between the gain it consumes and the temperature it emits. `gainAdvances` is the seam: module 1 ends where it asserts the gain moved, and says nothing about what the movement did.

### 3d. Slice texts (lines 82–130)

- **Slice 1 — the bearer** (line 87, "*opened as blocking; superseded — run slice 2 first*"). Question: "Does any dated observation record commitment temperature being wrong, costly, or noticed?" Constraints: "May not cite R8's dead outer loop"; "May not argue from the mechanism"; "The corpus must be named as a path (`T-wm-wrong-corpus-26082026`)."
- **Slice 2 — where τ_eff is actually applied** (line 102). "`selection_gain.clj:14` states `τ_eff = adaptive-temperature(G-spread) / selection gain`. A first grep of `futon2/src/futon2/aif/` finds temperatures in a softmax at only two sites, and **neither is obviously this one**: `cascade_prior.clj:173` (a shadow-cascade `:tau`) and `preferences.clj:128` (`default-c-temperature` …)". Question: "Does the R14 temperature reach policy selection at all, and at which step?"
- **Slice 3 — the pairing record** (line 117). Quotes R14's THEN; "That is **family 2 (inhabited handle) applied to the temperature→action edge**: a step counts only if it left something durable, and here the durable thing is a *pair* — (τ at the flip, the action subsequently selected)." "**The naive fix that would recreate a known defect.** Log τ per tick."

### 3e. The internal chain / edges (verbatim)

Line 190–193 table (strategy note 1):

| step | where | what it does |
|---|---|---|
| τ computed | `futon2/src/futon2/aif/policy.clj:35` `effective-temperature` | `:spread` → τ_spread / g; `:selection-gain-only` → 1 / g |
| g fed in | `futon2/scripts/futon2/report/war_machine.clj:4248–4269` | reads `:selection-gain` from the previous trace record, folds `:realized-outcome`, passes `selection-gain-value` |
| τ used | `war_machine.clj:4476` → `policy/select-action` with `:selection-boundary :strategic-recommendation` | `strategic-recommendation` (`policy.clj:223`) computes τ, then sets `chosen` to `(first controller-entries)` — the rank-1 action, **independent of τ**. τ reaches only the reported `:controller-ranking` scores, the habit *counterfactual*, and `:softmax-weights` |
| action enacted | `war_machine.clj:4526` `wm-decision` | `assoc`s `:action (:action strategic-action)` — the output of `invoke-strategic-selection`, the reason-bearing selector — **over** whatever the controller chose |

Line 195: "So on the enacting path the temperature→action edge is cut twice: the selector takes the rank-1 action regardless of τ, and the reason-bearing selector then replaces that action anyway. The default `:actuation` branch is the same shape (`best = (first ranked-actions)`, `policy.clj:377`; τ shapes only the `:softmax-weights` it reports). The one branch where τ can change the argmax is the habit-prior branch — `scores = −G/τ + ln E` — which runs only under `:structural-pressure-mode :habit-prior`."

Line 300 (strategy note 4, the arrow chain):

```
    g moved  ⟹  τ_eff moved  ⟹  the selected action is a function of τ_eff
```

> `gainAdvances` (`GainChain.lean:156`) asserts the first arrow's premise and nothing after it. The `CommitmentTemperature.lean` chain property is the second and third arrows together; per the light standard, never one predicate per arrow, because `effective-temperature` and `strategic-recommendation` each typecheck alone and the defect is in their composition.

Lines 383–397 (information-theoretic reading):

```
    I(τ ; selected action) = 0 bits
```
> **This is a bound, not a code observation.** g reaches `:action` only through τ: `chosen` (`policy.clj:238`) is a function of `ranked-actions` alone, and the only other readers of `:selection-gain` are an audit field written *into* the record (`enact.clj:226`) and reporting (`lane_futility`) — neither gates an action. With `g → τ → action` a Markov chain, the data processing inequality gives
```
    I(realized outcomes ; selected action) ≤ I(g ; action) ≤ I(τ ; action) = 0
```

Line 434–441 (the general statement):
> τ carries information only when it trades off two terms with different τ-scaling. A single-term argmax annihilates it. Precision is meaningful only relative to something else.

Lines 738–747 (`repair-entry`, the working membership edge):
```
    (defn- repair-entry [obligation]
      {:action {:type :repair-machine-failure
                :rationale (str "stop-the-line: " …)}
       :controller-score ##-Inf
       :G-efe ##-Inf
       :selection-source :stop-the-line})
```
> `efe/rank-actions` sorts ascending on `:controller-score` (`efe.clj:823`) and `strategic-recommendation` takes `(first controller-entries)` (`policy.clj:238`), so the repair entry **preempts the entire ranking**. … `argmax` is invariant to how you *scale* a ranking and perfectly sensitive to *what is in* it.

### 3f. Review table — claude-13 verification at source (lines 343–352)

| claim | verified at | verdict |
|---|---|---|
| τ computed in `effective-temperature` | `policy.clj:35` | ✅ and the two modes are exactly as stated: `:spread` ⇒ τ_spread/g, `:selection-gain-only` ⇒ 1/g |
| the live selector ignores τ | `policy.clj:223` `strategic-recommendation`, `chosen` at **:238** = `(or (first controller-entries) (first ranked-actions))` | ✅ τ feeds only `scores` → `habit-order` → `counterfactual-idx`; it never touches `chosen` |
| default branch likewise | `policy.clj:377` `best (first ranked-actions)` when `priors?` is false | ✅ |
| habit-prior is the one branch where τ can move the argmax | `policy.clj:~400–410`: `scores = −g/τ + lp`, then `chosen-idx (apply max-key scores …)` | ✅ — and the reason is exact: `lp` is *not* scaled by τ |
| the enacting path takes the τ-free boundary | `war_machine.clj:4476` passes `:selection-boundary :strategic-recommendation` | ✅ |
| and then overwrites the action | `war_machine.clj:4527` `wm-decision (assoc controller-decision :action (:action strategic-action) …)` | ✅ the edge is cut twice, as stated |
| the trace archive | 52 files in `futon2/data/wm-trace/` | ✅ `:tau-mode` occurrences `:spread` 76 / `:selection-gain-only` 35; **every file from 07-15 through 07-21 carries τ = 1.0 and nothing else**; pre-flip values 0.80012, 1.60024, 0.1056, 15.0504 all present; **exactly 3** `:governed-by :habit-prior` records |

Line 354: "**Not confirmed, and worth one line of work:** whether `invoke-strategic-selection` (`war_machine.clj:4097`) itself consumes τ."

### 3g. Salience candidates (lines 254–262 table; lines 874–882)

| candidate | date | what it is |
|---|---|---|
| the τ-mode flip | 2026-07-13 (Joe; `9d8f2de`, first trace 07-14) | `arena-tau-mode` → `:selection-gain-only`, so τ_eff = 1/g; g pinned ⇒ τ_eff ≡ 1.0 in all 31 records since. Before the flip (07-04..07-09, `:spread`) τ varied: 0.80, 1.60, 0.106, 15.05. |
| ants dead τ | 2026-08-01 | `PREDICTION-outcome.md` — a preregistered prediction falsified; codex-8/codex-9's work |
| operator ⑯ | 2026-08-26 | thirtyfour-steps: *"temperature demonstrably varies — after two wrong assumptions the search got more conservative"* |
| TryHarder 0/22 | 2026-07-15 | `futon2/data/wm-full-loop/archives/stop-line-2026-07-15/` — "attempt-002–022 and 024 were no-selection" |

Line 882: "The last is strongest and is evidence about the **missing edge**, not the dial."

### 3h. Repair menu (lines 444–450 and 765–770)

| option | route | precedent |
|---|---|---|
| (a) sample `P(π)` | make weights behavioural | canonical AIF; none in-stack |
| (b) second non-τ-scaled term | habit prior | 3 records, live-vs-shadow unestablished |
| (c) accept it is decorative | — | — |
| **(d) route the gain through the candidate set** | membership, not weights | **`repair-entry`, working since ≈2026-07-25** |

### 3i. Tickets, rulings, commits cited (`grep -o -E 'T-[A-Za-z0-9-]+|WR-[0-9]+|WB-[0-9]+|`[0-9a-f]{7,10}`'`)

- Ticket: `T-wm-wrong-corpus-26082026` (lines 96, 235, 324, 362).
- Rulings: WR-27 (lines 52, 65, 165, 249, 424, 670, 862, 885), WR-26 (549, 608, 670, 791, 792), WR-25 (672), WR-0 (670, 885). WB-15 (249: "The War Bulletins (WB-15 mints WR-27)").
- Commits: `b624242` (66, armed both flags), `9d8f2de` (258, τ-mode flip), `661010e6f8` and `42e85d7010` (135, the Lean module and its review).

### 3j. What the excursion says the Lean module does and does not cover

Lines 132–160:
> **`mathlib4/DarkTower/WarMachine/CommitmentTemperature.lean`** — codex-18 `661010e6f8`, reviewed and strengthened at `42e85d7010`. Standalone (no Mathlib), integer arithmetic throughout, `lake env lean` exit 0, zero `sorry`, no `sorryAx`. Nine theorems:

| theorem | what it records |
|---|---|
| `live_selector_does_not_govern` | the headline: `¬ governs modeOnly` |
| `single_term_argmax_annihilates_temperature` | `¬ governs argmaxScore`, and *algebraically* — τ·0 = 0 via `commitmentScore_zero_prior`, not from an unused argument |
| `repairing_r8_changes_no_action` | deterministic data processing at the R8/R14 seam |
| `live_gain_repair_changes_no_action`, `default_branch_gain_repair_changes_no_action` | the same, discharged for the actual selectors |
| `record_sensitivity_is_not_governance` | the live system satisfies the *refused* weakening and fails the requirement |
| `scores_move_action_does_not` | the finding on one fixed ranking: reported scores differ, action identical |
| `habit_prior_governs` | non-vacuity — a real two-entry witness whose winner flips |
| `mode_only_ignores_temperature` | `policy.clj:238` as a lemma |

> **No concessions, per Joe (2026-08-26):** `governs` is the unweakened existential — two temperatures, different actions — and `governsTheRecord` exists only to be refused. `G` is an opaque ordered score, so every result holds for **any** `G`: settling `G(π)` is not a prerequisite for this repair.

> The refusal theorems are therefore named after *disconnection* incidents rather than cost incidents, which is honest and leaves `?salience(required)` untouched.

(The excursion's table lists 9 theorem names; the file on disk has 13 `theorem` declarations — see §4. The four extra are the Markov-category section added after: `factorsThroughDiscard_iff_temperatureInvariant`, `not_governs_iff_factorsThroughDiscard`, `discard_absorbs_upstream`, `live_selector_factors_through_discard`.)

### 3k. Nouns the excursion uses (list, from the text)

commitment temperature; τ / τ_eff / τ_spread; selection gain g / `:selection-gain`; `:tau-mode` (`:spread`, `:selection-gain-only`); argmax / mode; softmax / `P(π) ∝ exp(−G/τ)`; entropy of the policy distribution; habit prior / `ln E` / `:governed-by :habit-prior`; `:controller-ranking`, `:softmax-weights`, `:habit-adjusted-ranking`, `:counterfactual`; `:selection-boundary :strategic-recommendation`; `:structural-pressure-mode :habit-prior`; `invoke-strategic-selection` / reason-bearing selector; `wm-decision`; realized outcome / `gainAdvances`; bearer / salience hole 香 / `?salience(required)`; disconnected dial; family 2 (inhabited handle), family 8; I(τ;action); data processing inequality; Markov chain `g → τ → action`; candidate set / membership vs weights; `repair-entry` / `##-Inf` / `:selection-source :stop-the-line`; repair obligation; stop-the-line / andon cord; TryHarder; policy-support mismatch; `:no-selection`; module 1 / the seam; gain-in / temperature-out faces; pairing record; live vs shadow.

---

## 4. LEAN — `mathlib4/DarkTower/WarMachine/CommitmentTemperature.lean` (307 lines)

Declarations (`grep -n -E '^(structure|inductive|def|abbrev|theorem|lemma|namespace|end) '`):

```
49:namespace DarkTower.WarMachine.CommitmentTemperature
51:structure Action where                       -- id : String
55:structure Entry where                        -- action : Action; g : Int; l : Int
62:abbrev Temperature := Nat
64:abbrev Selector := Temperature → List Entry → Option Action
75:def governs (s : Selector) : Prop :=          -- ∃ entries τ₁ τ₂, s τ₁ entries ≠ s τ₂ entries
83:structure SelectionRecord where              -- temperature, reportedScores : List Int, action : Option Action
89:abbrev RecordEmitter := Temperature → List Entry → SelectionRecord
93:def governsTheRecord (r : RecordEmitter) : Prop :=
97:def temperatureValue (τ : Temperature) : Int := Int.ofNat (τ + 1)
101:def commitmentScore (τ : Temperature) (entry : Entry) : Int :=   -- -entry.g + temperatureValue τ * entry.l
105:def argmaxBy (score : Entry → Int) : List Entry → Option Entry
113:def modeOnly : Selector :=                    -- fun _ entries => entries.head?.map Entry.action
119:def argmaxScore : Selector := fun τ entries =>
124:def habitPrior : Selector := fun τ entries =>
127:def cautiousAction : Action := ⟨"cautious"⟩
128:def habitualAction : Action := ⟨"habitual"⟩
133:def switchingEntries : List Entry :=
139:def liveRecord : RecordEmitter := fun τ entries =>
145:def temperatureInvariant (s : Selector) : Prop :=
148:theorem mode_only_ignores_temperature :
154:theorem live_selector_does_not_govern : ¬ governs modeOnly := by
158:theorem commitmentScore_zero_prior (τ : Temperature) (entry : Entry) :
164:theorem argmax_score_temperature_invariant : temperatureInvariant argmaxScore := by
168:theorem single_term_argmax_annihilates_temperature : ¬ governs argmaxScore := by
175:theorem repairing_r8_changes_no_action
186:theorem live_gain_repair_changes_no_action
196:theorem default_branch_gain_repair_changes_no_action
207:theorem record_sensitivity_is_not_governance :
217:theorem scores_move_action_does_not :
226:theorem habit_prior_governs : governs habitPrior := by
251:def factorsThroughDiscard (s : Selector) : Prop :=
256:theorem factorsThroughDiscard_iff_temperatureInvariant (s : Selector) :
266:theorem not_governs_iff_factorsThroughDiscard (s : Selector) :
280:theorem discard_absorbs_upstream
289:theorem live_selector_factors_through_discard : factorsThroughDiscard modeOnly :=
307:end DarkTower.WarMachine.CommitmentTemperature
```

No `inductive` declarations. 13 `theorem`s, 14 `def`s, 3 `structure`s, 4 `abbrev`s. Lines 293–305 are `#print axioms` for all 13 theorems.

Module docstring (lines 6–47), verbatim excerpts:

> This standalone outline states the temperature-out face of R14.  The required channel is behavioural: for some fixed ranking, changing commitment temperature changes the selected action.  Merely copying the temperature into a record is explicitly weaker and is refused below.
>
> The enacting path modelled here is the live `:selection-boundary :strategic-recommendation` path.  Its selected action is the first controller entry (`policy.clj:238`) and is later installed from `strategic-action` (`war_machine.clj:4476,4527`); temperature changes reported scores but not that action.  The default argmax branch has the same invariance. The habit-prior branch has nonzero capacity because its score is `-G + τL`; three records under that mode exist in `futon2/data/wm-trace/`, but whether they are live decisions or shadow calculations is not established.
>
> `G` is an opaque ordered controller score.  Nothing here defines or validates `G(π)`.  The selector is deterministic; sampling from `P(π)`, one proposed repair, is not modelled.  This file states no emitter, Clojure contract, mutation test, or running-system repair.
>
> The `Temperature` value `t : Nat` encodes the positive integer temperature `t + 1`.  Thus all score comparisons use integer arithmetic and never divide. This continues `GainChain.gainAdvances`: repairing the incoming gain cannot alter an action while the final temperature-to-action edge remains constant.
>
> ## Fixture polarities
>
> * accepting — `habit_prior_governs`
> * refusing-broken — `live_selector_does_not_govern`
> * refusing-plausible-fix — `record_sensitivity_is_not_governance` (making the emitted record move with τ looks like restoring governance and does not)
>
> ## Vocabulary
>
> This module defines its own vocabulary (`Selector`, `governs`, `factorsThroughDiscard`) rather than adapting to a `GainChain` family, because its subject is a selector's dependence on a parameter rather than the presence, domain, or durability of a fold occurrence.  Its contract entry is family 8, which has no APM source.

`governs` docstring (lines 67–74): "R14's uncompromised requirement.  For a deterministic selector this is the finite behavioural form of positive information from temperature to the selected action: the channel is not constant. … non-constancy is equivalent to the *existence* of a temperature distribution under which the mutual information is positive, not to its being positive under every input law."

`switchingEntries` (line 133): `[ { action := cautiousAction, g := 0, l := 0 }, { action := habitualAction, g := 2, l := 1 } ]` — docstring: "At encoded temperature `0` (actual temperature 1), cautious scores `0` and habitual scores `-1`; at encoded temperature `2` (actual temperature 3), habitual scores `1` and wins."

Markov section header (lines 233–249): "In a Markov category every object carries a commutative comonoid — copy `Δ` and discard `ε` — and a morphism is independent of an input exactly when it **factors through that input's discard**. … `Mathlib`'s `Probability/Kernel/Category/Stoch.lean` carries the measure-theoretic version … it is not imported here because this file is deliberately standalone. The trigger for reaching for it is repair option (a), sampling `P(π)`, which is a genuine stochastic morphism."

---

## 5. CODE — `futon2/src/futon2/aif/`

### 5a. `policy.clj` (defn lines: `grep -n -E '^\(defn'`)

```
21:(defn adaptive-temperature
35:(defn effective-temperature
71:(defn softmax-weights
95:(defn- find-no-op
100:(defn- gap-report
109:(defn default-mode-select
160:(defn- numeric-range [xs]
168:(defn- candidate-explanation [entry idx tau ln-e]
182:(defn- decision-explanation
215:(defn- ranking-entry
223:(defn- strategic-recommendation
289:(defn select-action
```

`policy.clj:1–19` ns docstring (excerpt): "`select-action` is the top-level R6 deliverable: take a ranked-action list (from `efe/rank-actions`), apply softmax over controller-scores with adaptive temperature τ, and return either the chosen action or an abstain branch with a structured gap-report. … Theory: AIF softmax selection — `P(a) ∝ exp(−G(a) / τ)`. Adaptive τ scales with EFE spread: tight spreads → high τ → diffuse selection → abstain trips."

`policy.clj:21–33` `adaptive-temperature` docstring: "Compute τ from the EFE spread of a candidate set. High spread → low τ (sharp pick); tight spread → high τ (diffuse / abstain-leaning). Floored at `tau-min` so degenerate (identical) EFE inputs don't divide by zero downstream. Defaults: tau-min 0.01, k 5.0." Body: `(max tau-min (/ spread k))` where `spread = (- (apply max g-totals) (apply min g-totals))`.

`policy.clj:35–69` `effective-temperature`, docstring in full:

> The selection temperature actually used. TWO layers, separated per the R6 faithfulness audit (M-aif-faithfulness §2.2 B-2d):
>
>   g        — R14 engineering outcome-feedback selection gain. High g ⇒ lower τ_eff ⇒ sharper commitment; low g ⇒ flatter. It is not variational policy precision.
>   τ_spread — `adaptive-temperature` = range(G)/k: an adaptive-calibration heuristic (tight spread → diffuse selection → abstain trips) historically STACKED on g. Not part of the canonical form; now a separately-justified layer that can be switched off.
>
> `:tau-mode` in the opt-map selects the layering:
>
>   :spread (DEFAULT)   τ_eff = τ_spread / g — byte-identical to the historical stacked behaviour.
>   :selection-gain-only τ_eff = 1 / g — fixed-baseline controller mode; the spread calibration layer is OFF.
>
> g is floored at `tau-min` defensively in both modes so a degenerate g can never divide τ to zero or flip its sign. g = 1.0 (the default and burn-in prior) reduces :spread EXACTLY to the spread-only temperature, and :selection-gain-only to τ_eff = 1.
>
> Both modes are engineering calibration policies and are reported as such.

Body (`policy.clj:61–69`):
```clojure
  ([g-totals selection-gain {:keys [tau-min tau-mode]
                    :or {tau-min 0.01 tau-mode :spread}
                    :as temperature-opts}]
   (let [g (max (double tau-min) (double selection-gain))]
     (case tau-mode
       :spread (/ (adaptive-temperature g-totals temperature-opts) g)
       :selection-gain-only (/ 1.0 g)
       (throw (ex-info "unknown :tau-mode (expected :spread or :selection-gain-only)"
                       {:tau-mode tau-mode}))))))
```
**τ_eff = 1/g is computed at `policy.clj:67`.** The `:tau-mode` values accepted by the code are exactly `:spread` and `:selection-gain-only` (line 68 throws otherwise). A third name, `:gamma-only`, appears only in `futon2/scripts/dark_mode_shadow.bb:9,149` (`;;   :tau-mode :gamma-only (B-2d, policy/effective-temperature)`) — `grep -n 'gamma-only' policy.clj` → no match.

`policy.clj:71–82` `softmax-weights` docstring: "P(a) ∝ exp(ln E(a) − G(a) / τ), normalised to sum to 1.0. … The 2-arity form is the historical σ(−G/τ) — equivalently ln E ≡ 0 … The 3-arity form is the R12 HABIT-PRIOR SEAM (M-aif-faithfulness D-1d): `log-priors` aligns with `g-totals` and enters the score UNSCALED by τ. The semantic point is that controller temperature modulates G, never the habit prior. This seam is THE place a future real ln E(π) (R12 per-action-class posteriors) enters — do not add a second prior site." Body: `scores (mapv (fn [g lp] (+ (/ (- (double g)) (double tau)) (double lp))) g-totals lps)`.

`policy.clj:182–213` `decision-explanation`: returns `{:winner :top-G :top-mission-value-factor :tau-mode :tau-effective :selection-gain :habit-prior-stats :span-diagnostics :governed-by}`; line 211–213:
```clojure
     ;; A prior that changes the argmin-G winner governed the actual choice;
     ;; an aligned prior may have a wide span without deciding the winner.
     :governed-by (if (= winner top-g) :G :habit-prior)}))
```

`policy.clj:223–238` `strategic-recommendation`: docstring "Select the controller head at the strategic mission grain while retaining the scheduler-grain habit calculation as an inspectable counterfactual. This is a live selection, not an operator-approval request. It deliberately does not authorize enactment: downstream act gates own that decision." Body lines 231–238:
```clojure
  (let [tau-spread (adaptive-temperature g-totals temperature-opts)
        tau (effective-temperature g-totals selection-gain temperature-opts)
        scores (mapv (fn [g lp] (+ (/ (- (double g)) (double tau))
                                    (double lp)))
                     g-totals log-priors)
        controller-entries (filterv #(not= :no-op (get-in % [:action :type]))
                                    ranked-actions)
        chosen (or (first controller-entries) (first ranked-actions))
```

`policy.clj:289–310` `select-action` docstring (kwargs excerpt): "`:temperature-opts` — passed to `adaptive-temperature` AND `effective-temperature`; may carry `:tau-mode` (:spread default | :selection-gain-only …). `:selection-gain` — g, the R14 learned inverse-temperature (`futon2.aif.selection-gain`). τ_eff = τ_spread / g. Default 1.0 ⇒ behaviour identical to the spread-only path. `:habit-prior-stats` — optional sufficient-statistic summary for the decision explanation; it never changes selection. `:selection-boundary` — :actuation (default, historical semantics) or :strategic-recommendation."

`policy.clj:372–377`: `(if (= :strategic-recommendation selection-boundary) (strategic-recommendation …) (if-not priors? (let [best (first ranked-actions) …`. `policy.clj:401–406` (habit-prior branch): `scores (mapv (fn [g lp] (+ (/ (- (double g)) (double tau)) (double lp))) g-totals log-priors)` then `chosen-idx (apply max-key scores (range (count scores)))`.

### 5b. `selection_gain.clj` (defn/def lines)

```
72:(def default-window-size 20)
73:(def default-min-history 5)         ; burn-in: selection gain ≡ 1.0 until this many samples
76:(def default-gain 1.0)
77:(def default-selection-gain-floor 0.5)
78:(def default-selection-gain-cap 2.0)
79:(def default-initial-selection-gain 1.0)
81:(def ^:private perf-epsilon 1.0e-9)
83:(defn policy-performance
99:(defn initial-selection-gain-state
109:(defn- mean
114:(defn- selection-gain-from-mean-perf
124:(defn update-selection-gain
162:(defn observe-outcome
173:(defn fold-realized-outcome
208:(defn coerce-state
225:(defn selection-gain-for
```

ns docstring (lines 1–70), excerpts: "Adaptive outcome-feedback selection gain (R14 controller calibration). … this learns a single bounded scalar selection gain — an engineering commitment control — from the realized-vs-expected outcomes of the policies it chose. selection gain ≈ 1/τ, the inverse selection temperature … It is not Friston's variational policy precision: its learning signal is realised controller performance, not expected free energy under the policy posterior." Line 14: `τ_eff = adaptive-temperature(G-spread) / selection gain`. Line 22: `perf = (expected-score − realized-score) / (|expected-score| + |realized-score| + ε) ∈ [−1,1]`. Line 42: `selection gain = clamp( 2^(gain · perf̄), floor, cap )      ; gain 1.0, floor 0.5, cap 2.0`. Lines 48–51: "BURN-IN (the R19-KL pattern): selection gain stays EXACTLY 1.0 until ≥ `min-history` realized samples accrue". Lines 61–65 state shape: `{:selection-gain :perf-history :mean-perf :samples}`. Line 67: "Contract: R14 (precision over policies) per `M-aif-wiring`."

`selection-gain-for` (line 225–230): "This is the value selection divides τ by." `coerce-state` (208–223): "The R14 v0 feed (live for one day, 2026-06-27, retired for the signed-perf redesign) persisted a symmetric-|error| schema (`:error-history`/`:mean-error`) whose 8 samples were DEGENERATE … pinning selection gain to the floor 0.5 … (found 2026-07-02: the live WM hedging 2× on junk)."

### 5c. `habit_prior.clj` — exists (defn/def lines)

```
24:(def default-alpha 1.0)
25:(def state-version 1)
27:(defn policy-key
39:(defn initial-state
51:(defn coerce-state
70:(defn observe-action
81:(defn fold-record
86:(defn fold-records
91:(defn state-stats
100:(defn log-priors
121:(defn attach-log-priors
```
ns docstring (lines 1–22, excerpt): "A learned categorical habit prior E(π) from observed policy frequencies. … `E(π=i | Π_feasible) = (n_i + α) / Σ_{j∈Π_feasible}(n_j + α)` … HONESTY: this is a learned habit/frequency prior, not evidence that a policy is good. v1 uses α=1 and no recency decay. … Selection gain g and the prior's alpha/decay/span set the numerical prior-vs-G balance".

### 5d. Live wiring — `futon2/scripts/futon2/report/war_machine.clj`

`war_machine.clj:235–245` `arena-tau-mode`:
> "B-2d R6 τ-layer separation, FLIPPED LIVE by Joe 2026-07-13. `:selection-gain-only` removes the score-spread calibration heuristic so the explicitly engineering selection gain alone controls commitment: τ_eff = 1/g. `FUTON_WM_TAU_MODE=spread` is the provenance-stamped rollback hatch to historical τ_spread/g behaviour. This does not relabel g as variational policy precision."
```clojure
  (if (= "spread" (System/getenv "FUTON_WM_TAU_MODE")) :spread :selection-gain-only))
```
`war_machine.clj:355`: `:tau-mode (arena-tau-mode)` (written into `:wm-version`). `war_machine.clj:4476–4483`: `(policy/select-action wm-admissible {:selection-gain selection-gain-value :selection-boundary :strategic-recommendation :habit-prior-stats … :temperature-opts {:tau-mode (arena-tau-mode)}})`. `war_machine.clj:4526–4530`: `wm-decision (assoc controller-decision :action (:action strategic-action) :reason :reviewed-live-reason-bearing-policy :selection-boundary :reason-bearing-strategic-policy`.

Other readers of `:selection-gain` (`grep -rn 'selection-gain' enact.clj lane_futility.clj`): `enact.clj:10` (docstring), `enact.clj:175` `*selection-gain-escrow-feed?*` ("γ-FEED REWIRE (operator-armed 2026-07-05 …)"), `enact.clj:224–229` writes `:selection-gain-expected-score` / `:selection-gain-source` into the enactment record; `lane_futility.clj:274–313` folds outcomes into `:first-gamma`/`:last-gamma` for reporting.

---

## 6. BADGES / OVERLAY / PATTERN MAP

### 6a. `futon2/data/r18-badges.edn`

Line 53: `:SEL {:requirement "R6" :quantities [:effective-temperature-softmax]}`
Lines 59–60: `:R14 {:requirement "R14" :quantities [:outcome-feedback-selection-gain :policy-performance-ratio]}`

Lines 146–156 `:effective-temperature-softmax`:
> `:badge :engineering-control`
> `:claims "spread-normalised controller softmax"`
> `:cite "Da Costa et al. 2020, P(π) = σ(−γ·G(π))"`
> `:code-ref "futon2/src/futon2/aif/policy.clj:35"`
> `:computes "LIVE softmax(−G/τ_eff), τ_eff = 1/g, where g is the explicitly engineering outcome-feedback selection gain; FUTON_WM_TAU_MODE=spread restores historical τ_spread/g"`
> `:repair "complete for removal of the spread-normalisation residual; g remains an engineering control rather than inferred variational policy precision"`
> `:repair-built "B-2d :selection-gain-only FLIPPED LIVE by Joe 2026-07-13. Existing shadow established zero winner or abstain changes by construction and an entropy-only commitment effect. FUTON_WM_TAU_MODE=spread is the provenance-stamped rollback hatch; reviewer-side badge/disposition audit remains separate."`

Lines 157–167 `:policy-performance-ratio`:
> `:badge :engineering-control`
> `:claims "(γ's learning signal — not itself labelled an AIF quantity)"`
> `:cite "engineering realised-versus-expected controller feedback"`
> `:code-ref "futon2/src/futon2/aif/selection_gain.clj:82"`
> `:computes "(expected−realized)/(|expected|+|realized|+ε) ∈ [−1,1]; signed, scale-free control-loop feedback"`
> `:repair "replace/augment with the E[G]-over-policies signal if it is to feed γ canonically"`
> `:note "honestly named (no AIF label) ⇒ no R18 relabeling fault; :analogical only as a variational quantity"`

Lines 219–230 `:outcome-feedback-selection-gain`:
> `:badge :engineering-control`
> `:claims "outcome-feedback selection gain"`
> `:cite "Da Costa et al. 2020 (β update from E[G] over policies); Friston et al. 2017"`
> `:code-ref "futon2/src/futon2/aif/selection_gain.clj:123"`
> `:computes "γ = clamp(2^(gain·mean-perf),0.5,2.0) from realized-vs-expected outcomes, not E[G]-over-policies"`
> `:repair "complete: renamed namespace, state, trace, selection option, and realised-outcome fields so no variational-γ claim remains"`
> `:note "borderline — the estimator is a substitution, not an approximation of the β-update"`

### 6b. `p4ng/empirics-futon/wr-overlay.edn:45`

> `{:node "R14" :wr "WR-27" :holds false :note "commitment temperature is explicitly a gain; instrumented at birth or diagnosed retroactively"}`

(Adjacent, line 42, for reference: `{:node "R8" :wr "WR-27" :holds false :note "a loop is born instrumented for its gain -- the per-tick mismatch IS the gain reading, and the daily cadence that would read it stopped 2026-07-14 (last outer-loop run 2026-07-27); the 2026-08-20 instrumented campaigns are a separate mechanism"}`)

### 6c. `futon2/holes/aif-r1-r16-pattern-map.md:40`

> `| **R14** | Precision over policies (γ) | `policy-precision-commitment-temperature` | ◐→ **ARMED (latent until R10 + data)** | The grounding half is done and the live-wire migration coded (`d36086f`): `*gamma-grounded-feed?*` routes γ's realized feed to the A5 **substrate dial**. **Both arms now FLIPPED** (`*live-wire?*` + `*gamma-grounded-feed?*` default-on, `b624242`, Joe-directed). What remains is **not code, not arms** — real fold-variance data + an R10 run: γ stays starved until a run produces grounded samples (**inert-until-data**), so armed ≠ moving. |`

Line 124: `| **R14** | `R14-decomposition` | **0** | `live-wire` **ARMED** (`b624242`, default-on); only `accumulate` (a data dependency) remains — no code, no arms |`. Line 140: "real fold-variance *data* to move (R14 inert-until-data, not a code/arm gate)".

---

## 7. CORPUS — `futon2/data/wm-trace/wm-trace-2026-*.edn`

**Files:** 52 (`ls wm-trace-2026-*.edn | wc -l` → 52; also present: `wm-shadow-step.json`, not read). **Records:** 791 top-level forms, all maps, 0 non-map forms. Date range of `:timestamp`: `2026-05-18T19:42:49Z` .. `2026-07-21T10:05:12Z`.

**Reader-loop command** (`bb /tmp/r14-corpus.clj`, run from `/home/joe/code/futon2/data/wm-trace`; the reader core, used identically in all four passes):
```clojure
(require '[clojure.edn :as edn] '[clojure.java.io :as io])
(defn read-all [f]
  (with-open [r (java.io.PushbackReader. (io/reader f))]
    (loop [acc []]
      (let [x (edn/read {:eof ::eof :default (fn [t v] v)} r)]
        (if (= x ::eof) acc (recur (conj acc x)))))))
(def files (sort-by #(.getName %) (filter #(re-matches #"wm-trace-2026-.*\.edn" (.getName %)) (file-seq (io/file ".")))))
(def recs (vec (mapcat (fn [f] (map #(assoc % ::file (.getName f)) (read-all f))) files)))
;; counts: (count (filter #(contains? % k) recs)) for top-level; a recursive find-key for "anywhere";
;; values: recursive collect-vals of key k; paths: recursive key-path walk.
```
(Full scripts at `/tmp/r14-corpus.clj`, `/tmp/r14-corpus2.clj`, `/tmp/r14-corpus3.clj`, `/tmp/r14-corpus4.clj`.)

**Key counts (records carrying the key):**

| key | top-level | anywhere in record | path(s) |
|---|---|---|---|
| `:tau` | 0 | **791** (every record) | `[:decision :tau]` 791 |
| `:tau-mode` | 0 | **107** | `[:wm-version :tau-mode]` 107; `[:decision :decision-explanation :tau-mode]` 4 |
| `:selection-gain` | **31** | 31 | top-level |
| `:habit-prior-state` | **31** | 31 | top-level |
| `:policy-precision` | **216** | 216 | top-level |

**Distinct `:tau-mode` values (all occurrences):** `{:spread 76, :selection-gain-only 35}`. By file: `:spread` in 07-04 (36), 07-05 (18), 07-06 (21), 07-09 (1); `:selection-gain-only` in 07-14 (14), 07-15 (6), 07-16 (6), 07-17 (1), 07-18 (2), 07-19 (2), 07-21 (4). Files 05-18..07-03 carry no `:tau-mode`.

**Distinct `[:selection-gain :selection-gain]`:** `{1.0 31}`. **Distinct `[:selection-gain :samples]`:** `{0 31}`. Full `:selection-gain` map distinct: `{{:selection-gain 1.0, :perf-history [], :mean-perf nil, :samples 0} 31}`. All 31 are in files 07-14..07-21.

**`:policy-precision`** (the predecessor key, 216 records, files 06-27..07-09): keys `{:policy-precision 216, :samples 216, :error-history 99, :mean-error 99, :perf-history 117, :mean-perf 117, :last-outcome-tick 113}`. `:samples` values range 0..76 (`{8 93, 76 34, 0 4, 26 3, …}`). `:policy-precision` scalar values observed include `1.0`, `0.5000000001423888` (06-27..07-02, the retired `:error-history` schema, 8 samples), `1.0157577633069552` (19 samples), `1.0612130232433021` (22), `1.287244150067791` (35), `1.3459001920611886` (39..73), `1.3375092522591208` (76). `:policy-precision :gamma` → not found.

**Distinct `[:decision :tau]` per era (from the per-file table):** 05-18..06-04: 0.0204, 0.0272, 0.0518, 0.1, 0.1056, 0.1066, 0.164, 0.181, 0.33x; 06-05..06-07: 0.19x, 0.26x, 5.597; 06-08..06-09: 5.597, 14.60, 14.88, 15.0504; 06-09..06-10: 1.0796, 0.7949; 06-12..06-27: 0.80012; 06-27..07-02: 1.60024; 07-02..07-06: 0.80005, 0.7461, 0.7504, 0.7472, 0.65–0.79, 3.27, 3.22; 07-09: 1.01287; **07-14..07-21: 1.0 only (31 records)**.

**`:samples` anywhere** (mixes `:selection-gain`, `:policy-precision`, and `:habit-prior-state` `:samples`): 0 (35), 8 (93), 76 (34), 761..791 (habit-prior counts, 1–2 each), plus 1..75 from `:policy-precision`.

**`:habit-prior-state`** sample (07-14): `{:version 1, :alpha 1.0, :counts {[:advance-mission [:target "M-first-flights"]] 69, [:learn-action-class [:target-class :fire-pattern]] 50, [:address-sorry [:target :sorry/pudding-g1-arrow-witness-binding]] 307, …}, :samples 761, :recency-decay :none}`.

**`[:wm-version :structural-pressure-mode]`:** `{:g-summand 76, :habit-prior 31}`. **`[:decision :habit-prior-applied?]`:** present in 31 records, all `true`. **`[:decision :selection-boundary]`:** not found in any record. **`[:decision :counterfactual]`, `[:decision :habit-adjusted-ranking]`, `[:decision :softmax-weights]`, `[:decision :controller-ranking]`:** 0 records each. **`:realized-outcome`:** 88 records.

**"govern"/"verdict" search.** Records whose strings/keywords match `(?i)govern|verdict` anywhere: 723 — but the matches are dominated by `:act-gate-verdicts` (112 records), `:verdict` (211) and the literal string `arxana/stack/futon-v1/globe/6-governance-interface` (1446 occurrences). The only key stating which term governed is **`[:decision :decision-explanation :governed-by]`**, present in **4 records**:

| file | timestamp | `:governed-by` | enacted `:action` (type/target) | `:top-G` action | `:winner :rank` | enacted = winner? | enacted = top-G? |
|---|---|---|---|---|---|---|---|
| wm-trace-2026-07-18.edn | 2026-07-18T14:33:21Z | `:habit-prior` | `:advance-mission "M-learning-loop"` | `:advance-mission "M-expressions-of-interest"` | 3 | yes | no |
| wm-trace-2026-07-19.edn | 2026-07-19T00:05:32Z | `:G` | `:advance-mission "M-expressions-of-interest"` | same | 1 | yes | yes |
| wm-trace-2026-07-21.edn | 2026-07-21T05:53:19Z | `:habit-prior` | `:advance-mission "M-learning-loop"` | `:advance-mission "M-distributed-frontiermath"` | 10 | yes | no |
| wm-trace-2026-07-21.edn | 2026-07-21T10:05:12Z | `:habit-prior` | `:advance-mission "M-learning-loop"` | `:address-sorry :sorry/pattern-typed-theme-liberates-the-solo` | 72 | yes | no |

**Tally: habit/prior governed = 3; G governed = 1.** All four carry `:tau 1.0`, `:tau-effective 1.0`, `:tau-mode :selection-gain-only`, `:selection-gain 1.0`, `:habit-prior-applied? true`, and `[:wm-version :structural-pressure-mode] :habit-prior`. In each, the record's `:decision :action` matches the explanation's `:winner :action` on `(:type, :target)`. Decision keys on these records: `(:action :controller-score :decision-explanation :habit-prior-applied? :rank :selection-gain :tau :tau-spread)`. No `:counterfactual`, `:habit-adjusted-ranking` or `:selection-boundary` key is present on them. No other record carries a `:decision-explanation`.

Distinct strings containing "govern" (8): the five `…/6-governance-interface…` path strings, the keyword name `governed-by` (4), and two `Assumptions Commons — Governance of the Shared Evidence File …` context-retrieval strings. No free-text string states which term governed.

---

## 8. MISSION — `futon2/holes/missions/M-formal-war-machine.md`

(`grep -n -E 'R14|family 8|I\(τ;action\)|CommitmentTemperature|governs'`)

Line 38: "R7 (evidence channel) supplying R3, R8 and R14; R9 (no self-certification) constraining R16; R20 and R12 both feeding R7."

Line 191 (families table): `| 1 | identity threading | ⑯→㉒→㉖→⑨°, persisted ㉞ | `threadedIdentity` | `GainChain.lean` | γ dedups on `:tick` (`fold_realized.clj`) vs `(System/currentTimeMillis)` at `scripts/wm_scheduled_run.clj:108` — **two clocks** | R8/R14 |`

Line 198: `| **8** | **temperature governs the action** | **⑯** | `governs`, `temperatureInvariant`, `gainAdvances` | `CommitmentTemperature.lean` | `policy.clj:35` (τ computed), `:238` and `:377` (argmax, τ-free), `war_machine.clj:4476,4527` | **R14** |`

Line 281: `| **uncertainty** — which action, given a distribution | **Markov categories** | family 8 (`factorsThroughDiscard` is already a Markov-shaped statement), ⑯ |`

Lines 372–374: "Both new families are in **SELECT**, and both are where 2026-08-26's work landed: family 8 is `I(τ ; action) = 0` on the enacting path, family 9 is the difference between changing a ranking's *weights* and changing its *membership*."

Lines 441–442: "The modular plan (`p4ng/empirics-futon/NOTE-modular-formalisation-order.md`) makes module 1 R8 + R14 with a three-clause property. Those three clauses are families 4, 5 and 1 of the table above."

Lines 454–457: "**In five cases out of five, the ring's recorded reason was wrong or unverified** — R8's "no producer" (there was one, armed 07-08), R14's "not yet located in the code" (`policy.clj:35`), R6's "tension-proposer unbuilt" …"

Lines 471–474: "`CommitmentTemperature.lean`'s `live_gain_repair_changes_no_action` proves that R8's repairs cannot move a selected action while R14's edge is cut — so a Tier-3 repair landed today could not be *evaluated*, because nothing would distinguish "it worked" from "it never ran"."

Lines 491–494: "The Lean half is started: `DarkTower/WarMachine/GainChain.lean` (families 1, 2, 4, 5 + the chain property) and `CommitmentTemperature.lean` (family 8). Absent: the emitter, the Clojure validator, the mutation suite, the qualification record — four of APM's five links."

Lines 566–568: "**Sensitivity-shaped families do not.** Family 8 (`governs`) is a claim about counterfactual τ — *would a different temperature have chosen differently* — and no record can witness it. Those stay Lean theorems about the code, not contract"

Line 591: `| `:tau`, `:decision` | **52** | — (family 8, the one that *cannot* be trace-validated) |`; line 592: `| `:tau-mode` | 11 | — |`; line 593: `| `:selection-gain`, `:author`, `:reviewer` | 7 | 6 (separated powers) |`

Line 717: `| **R14** | `E-R14-red-ring-fill` | `CommitmentTemperature.lean` | **its own** (`governs`, `Selector`, `factorsThroughDiscard`) | **no** |`

Lines 725–729: "**1 · Families 8 and 9 exist only in prose.** §2.1c names them — family 8 is `I(τ ; action) = 0`, family 9 is weights-versus-membership — and neither appears in `GainChain.lean`, in `ContractEmitter.lean`, or in `CommitmentTemperature.lean` itself, which never mentions a family number." (Note: the on-disk Lean docstring line 46 does say "Its contract entry is family 8"; the mission text predates or disagrees with that line — recorded, not resolved.)

Lines 735–738: "`CommitmentTemperature` and `PolicyGrade` each define their own vocabulary and import nothing. For `PolicyGrade` that was deliberate and stated in its packet; for `CommitmentTemperature` it was not decided, it just happened."

Line 747 (polarity table): `| `CommitmentTemperature` | `habit_prior_governs` | `live_selector_does_not_govern` | `record_sensitivity_is_not_governance` |`

Lines 761–762: "`CommitmentTemperature` over selectors, `GainChain` over fold occurrences."

Lines 789–790: "That is R14's defect one level up — a quantity computed, reported, and unable to change any decision — and the whole discrimination programme says the information is in what fails."

Line 981: "1. **Complete the contract over what exists** — emit family 8 and the policy-grade clause, and reconcile the family numbering between §2.1c and the emitter."

Line 1198: `| `CommitmentTemperature` | no |` (table of modules whose incident is named in the module).

Line 1680: "> **R14** — a dimension with **no singularity** on it."

Lines 1730–1731: "`factorsThroughDiscard_iff_temperatureInvariant` and `not_governs_iff_factorsThroughDiscard` — over *every* selector"

Line 1808: "**every property is about a finished object** — a run (`earnsPolicyGrade`), a selector (`governs`), a report (`coverageReported`) …"

Lines 1947–1949: "`habit_prior` on the one branch where τ moves an argmax). 4. **The non-vacuity column** — per R-node, which run exercised this mechanism and how do we know it executed. Eighteen rows; R8 and R14 are effectively done."

Lines 1958–1960: "- **R14's edge** — (a) sample `P(π)`, (b) a second non-τ-scaled term, (d) route the gain through the candidate set as `repair-entry` already does. Each changes the *type* of the signal differently; (d) converts a graded quantity into a"

---

## Nulls (with the commands)

- `futon2/src/futon2/aif/policy_precision.clj` — not found (`ls policy_precision.clj` → No such file or directory). The completeness doc's `futon2.aif.policy-precision` (lines 353, 409) names it.
- `:tau-mode :gamma-only` in `policy.clj` — not found (`grep -n 'gamma-only' policy.clj`, exit 1); present only in `scripts/dark_mode_shadow.bb:9,149`.
- `:policy-precision :gamma` in the corpus — not found (collect-vals over all records → `{}`).
- `[:decision :selection-boundary]`, `[:decision :counterfactual]`, `[:decision :habit-adjusted-ranking]`, `[:decision :softmax-weights]`, `[:decision :controller-ranking]` in the corpus — 0 records each (bb pass 3).
- `:governing-term` key in the corpus — not found.
- `wm-version :schema-version` — not found in any record.
- "precision over polic" in `sec-glossary.tex` — no match (`grep -n -i 'precision over polic'`).
- An `inductive` declaration in `CommitmentTemperature.lean` — none.
