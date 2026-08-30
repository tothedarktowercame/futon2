# facts-R6 — pointers and verbatim snippets for the R6 worksheet

**Collected:** 2026-08-30. **Scope:** facts only (file:line + quotes + commands). No classification, no interpretation. Companion to `R8-glossary-formalisation.md` (its §0, §1, §2, §4 columns are what these facts feed).

Repo roots: `p4ng` = `/home/joe/code/p4ng`, `futon2` = `/home/joe/code/futon2`, `futon3c` = `/home/joe/code/futon3c`, `mathlib4` = `/home/joe/code/mathlib4`.

---

## 1. PAPER

### 1a. `p4ng/sec-catalog.tex`

`grep -n 'R6' sec-catalog.tex` → lines 45, 62, 112, 129, 237, 344, 351.

**:62–66 (the R-number drift note):**
> At R6 the contract means softmax selection with abstain, where the catalogue means the candidate action space; at R12 it means an outer loop inferring the inner loop's hyperparameters, where the catalogue means two layers of calibration; and at R9 it means named validation properties, where the catalogue means no self-certification.

**:45 (same note, earlier branch):** "older \emph{completeness contract}, in which R6, R9, and R12 had narrower or different meanings."

**:112 (figure table row):** `& R6 --- Candidate Pattern Action Space & Constructs a bounded, reason-bearing choice set. \\`

**:129:** `& R16 $\rightarrow$ R6 --- Discharge-Trained Cascade Proposal & Learns diverse proposals from realised outcomes. \\`

**:237 (the R6 pattern paragraph, full):**
> \paragraph{Candidate Pattern Action Space (R6).} \textbf{If} you want pattern choice to be legible and comparable, and to avoid collapse into one habitual pattern. \textbf{However} if the action space is any pattern at any time, evaluation becomes unstructured and selection is dominated by recency. \textbf{Then} define the action set as a \emph{constructed} candidate set of pattern IDs produced by retrieval plus gating rules, of bounded size, with explicit reasons for inclusion and exclusion. \textbf{Because} constraining the action space makes downstream scoring meaningful rather than performative.

**:344 (R17''' HOWEVER clause, mentions R6):**
> \textbf{However} R6 can retrieve only the patterns already written down, R13 can compose only the moves already in the vocabulary, embedding nearness proposes similarity rather than a new action, and structure learning can reduce or compare the structures it has but cannot author a missing one.

**:351 (Discharge-Trained Cascade Proposal, closing R16 → R6) — opening and footnote:**
> \textbf{If} you want the candidate cascades the agent proposes (R6) to improve with experience --- to reach for the compositions that have actually gone well, not a retrieval ranking frozen at authoring time. \textbf{However} embedding-similarity retrieval is a \emph{prior}, not a value: it ranks patterns that \emph{look} relevant to the problem text, and on the historical corpus the patterns a successful mission actually used sit hundreds of places down that cosine ranking --- similarity commits to nothing, so a proposal built from it alone cannot learn. \textbf{Then} train the proposal distribution from \emph{realised outcome} … a generative flow network over the cascade-construction semilattice, which samples the \emph{diversity} of alive compositions …

Footnote (same line): "in a preregistered head-to-head over forty externally adjudicated builds, the trained proposer did \emph{not} beat the retrieval-prior incumbent on success rate, and a mission-conditioned variant made transfer to unseen missions worse rather than better. What it did win on decisively was \emph{diversity} of proposals. The disposition we adopted is therefore the honest one: keep it as a diversity supplier, not as a better chooser".

### 1b. `futon2/docs/futon-aif-completeness.md` `### R6` (lines 147–173)

**:147–151:**
> ### R6 — Softmax action selection with abstain
> `P(a) ∝ exp(−G(a) / τ)`, sampled or argmax. Abstain semantics: when the predictive distribution is too uncertain to discriminate among candidates, the agent declines to act.
> **Operational check.** Find the action-selection function. Verify softmax with temperature τ; verify an abstain branch exists and fires under high uncertainty.

**:153:** "**Satisfied as of v0.5 (2026-05-17).** Three new namespaces ship:"

**:155:** "`futon2.aif.action-proposer` — `ActionProposer` protocol, `bootstrap-proposer` (always-available default), `compose-proposers`. The bootstrap proposer surfaces `:no-op` plus one `:learn-action-class` action for every action-type whose `forward-model/can-propose?` returns false. This is the **"need to learn" / actionable self-model** move (per Joe 2026-05-17)"

**:157:** "`futon2.aif.policy/select-action` — top-level R6 deliverable. Takes a `rank-actions` output, applies softmax over G-totals with adaptive τ (`τ = max(spread/k, τ-min)`), returns either the chosen action (`{:action :rank :G-total :tau :softmax-weights}`) or an abstain branch (`{:action :abstain :reason :gap-report :ranked-actions}`)."

**:160:** "**Abstain semantics**: fires when (a) no candidates, or (b) `:no-op` is present and the best action's G-total is not at least `abstain-epsilon` below `:no-op`'s. The abstain branch carries a **gap-report** enumerating the `:learn-action-class` actions the proposer surfaced"

**:165:** "`can-propose?` defmulti dispatches per action type; default false; `:no-op` and `:learn-action-class` arms return true. Other action types default to false until a substrate adapter ships and overrides the multimethod."

**:167:** "The bootstrap proposer sets `:intrinsic-value 0.1` on `:learn-action-class` actions; this is the bias that makes `:learn-action-class` outrank `:no-op` when no other action ranks higher."

**:169:** tests "cover: … select-action chosen branch, all four abstain trigger conditions, gap-report enumeration, ε tunability."

**:171 (v0.5 snapshot):** "every concrete action type (`:address-sorry`, `:open-mission`, `:fire-pattern`) had `can-propose?` returning false — no substrate adapter shipped in v0.5. … The WM correctly surfaced three `:learn-action-class` recommendations (one per gated action class)."

**:173 (v0.6 snapshot):** "The WM's candidate set is now `{:no-op×1, :learn-action-class×2 (:fire-pattern + :open-mission), :address-sorry×1 (target :sorry/wm-aif-substrate-addressability)}`. The top-3 ranked under current parameter tuning are the two `:learn-action-class` actions (G≈0.0925) followed by `:no-op` (G≈0.1925); `:address-sorry` ranks 4th at G≈0.2045 … **This is a real tuning observation** — under v0.6 parameters the WM prefers expanding capability over addressing the one sorry it can address."

### 1c. `p4ng/empirics-futon/promotion-tests.edn` R6 entry (lines 148–176)

```
{:node "R6" :wr "WR-19"
 :statement
 "given a demonstrated option that is not being taken up, the select stage
  GENERATES a candidate that would move it -- a different buyer, a different
  obligation, a different unit of acceptance -- rather than re-ranking the
  menu it already has."
 :null-control
 "the same input through the ranker alone. If the top of the re-ranked menu
  and the generated candidate are the same item, nothing was generated."
 :external-fixture
 {:case :galois-inc
  :phase "SELECT"
  :dated "DARPA I2O funded $18M over 4.5 years and Galois delivered; the
          transition buyer did not adopt. GAO 2018 on the same buyers."
  :must-emit
  "the candidate the menu did not contain. …"
  :why-external
  "This is the ring's own claim tested where it can fail. Our menu is one we
   wrote; a menu somebody else was stuck inside is the harder case."}
 :registers-when {:built "a generator exists beside the ranker"
                  :ran   "it is run against the fixture"
                  :live  "the null control fails"}}
```

---

## 2. GLOSSARY — `p4ng/sec-glossary.tex` (81 lines)

`grep -n -i 'Pattern language\|Control states\|slush\|Embedding space\|Softmax and controller\|candidate\|proposer\|action space' sec-glossary.tex` → lines 21, 31, 33, 35, 46, 48, 50, 52, 60, 62, 70, 76.

**:33 Pattern language / cascade (full):**
> \paragraph{Pattern language / cascade.} A design pattern is a named response to a recurring problem, written so other people can recognize and reuse it. This paper makes patterns operational: the same pattern text can guide a human reader and serve as an action candidate or warrant for an agent \cite{…}. In this paper, \emph{cascade} is the operational name for a pattern language in use: a staged composition of patterns fitted to the selected mission. It says which warrants are being combined, and in what operational order, before any work is enacted. The harness is not merely choosing an isolated next command; it is proposing a small pattern-language construction that can be priced, checked, folded, and only then enacted.\footnote{… \srcref{futon2/scripts/futon2/report/cascade_lane.clj} builds the cascade-policy: \texttt{cascade-policy-for} constructs it, and \texttt{cascade-lane} records \texttt{:shown [pattern-ids...]}, \texttt{:cascade-score}, and \texttt{:policy-rollout-score}.}

**:35 Control states $U$ and the policy vocabulary (full):**
> \paragraph{Control states $U$ and the policy vocabulary.} In a discrete active-inference model, $U$ is the set of actions, also called \emph{control states}; a policy $\pi$ is an allowable sequence drawn from those controls, and the $E$ vector below places a prior over the already-allowable policies. This paper works one level higher: a design pattern is treated as an elementary or temporally extended \emph{control schema}, while a cascade is the policy composed from those schemas. The correspondence is functional rather than literal --- AIF does not know about design-pattern prose --- but it locates the important difference between two kinds of novelty. Cascade construction searches new combinations inside a fixed vocabulary $U$; pattern authoring proposes $U' = U \cup \{u^*\}$ and must also propose how the new control changes expected states, $B' = B \cup \{B^{u^*}\}$, before a forward model can score policies containing it. The induced horizon-$H$ policy space therefore grows, $\Pi_H(U) \subset \Pi_H(U')$. Saying that a new pattern enters at a ``uniform prior'' is an engineering shorthand: canonical $E$ has one entry per complete allowable policy, whereas the implementation's pattern-level proposal/reliability prior is a separate quantity that may eventually induce priors over cascades containing the pattern. R17$^{\prime\prime\prime}$ names this policy-vocabulary expansion and its residual explicitly \cite{…}.

**:62 GFlowNet ``slush'' (full):**
> \paragraph{GFlowNet ``slush''.} A GFlowNet is a sampler that learns to generate many high-reward objects rather than only the single best one. In the paper, the slush proposes diverse pattern compositions --- cascades --- for the selected mission, sampling a cascade $S$ for mission $m$ with probability $P(S \mid m) \propto \exp(\beta\,\hat R(S\mid m))$, where $\hat R$ is the learned reward and the inverse temperature $\beta$ is kept deliberately low so the distribution stays broad. Diversity is the point: a greedy (high-$\beta$) proposer yields a monoculture, and the loop learns fastest from varied proposals. … In offline tests the slush beats a greedy baseline on diversity and coverage, while its corpus-global information term does not steer above uniform. … The loop therefore uses the slush as a low-temperature diversity supplier rather than treating text coverage as reward.\footnote{… the producer-side live-loop lookup is \texttt{slush-candidates} in \srcref{futon2/src/futon2/aif/a4a_substrate.clj}.}

**:52 Embedding space (full):**
> \paragraph{Embedding space.} An embedding turns text into a point in a large numerical space. Nearby points often have related meanings. The paper uses this as a generator of hypotheses: nearby patterns may be candidates for composition or comparison, but nearness alone is not proof.\footnote{The constellation data used by A4a comes from \srcref{futon3c/holes/excursions/pipeline-semilattice-clusters.edn}.}

**:31 Softmax and controller calibration (full; the abstain word does not occur in this entry):**
> \paragraph{Softmax and controller calibration.} Softmax turns scores into a probability distribution: $p_i = e^{-G_i/\tau}\big/\sum_j e^{-G_j/\tau}$\eqanchor{softmax}. Because lower scores are better, softmax is applied to the negated scores, so low-cost actions become high-probability picks; the temperature $\tau$ sets how decisively --- low $\tau$ sharpens toward the best option, high $\tau$ flattens and explores. A worked example: $G=[1,2,4]$ at $\tau=1$ gives $p\approx[70\%,\,26\%,\,4\%]$, and halving $\tau$ to $0.5$ pushes the best option toward $88\%$. The effective temperature is set through the outcome-feedback selection gain $g$, as $\tau_{\mathrm{eff}}=1/g$; score-spread normalisation is disabled. This is an engineering calibration control, not an inferred variational policy precision. At the selection seam the learned habit prior enters unscaled by temperature --- the choice is over $-G/\tau_{\mathrm{eff}} + \ln E(\pi)$ --- and each decision records, in its decision explanation, the per-term contributions and which of the two terms governed it.\footnote{Action ranking and calibration are discussed in R14; the scheduled loop records the selected action, score breakdown, and decision explanation in the trace.}

`grep -n -i 'abstain' sec-glossary.tex` → not found (no glossary entry uses the word).

**Other entries mentioning candidate / proposer / action space:**

- **:21 Expected free energy $G$:** "The implementation persists this two-term core apart from its controller augmentation --- posterior spread, urgency, intrinsic credit, **feasibility masks**, and coverage bonuses are reported beside it, never hidden inside it. … The paper uses two grains: $G(a)$ scores candidate tasks, while $S(\pi)$ scores short tactical cascades of patterns for the chosen task".
- **:42–46 Habit prior $\widehat E$** (entry header at :42, formula): `\widehat E(a_i) = \frac{n_{k(a_i)}+\alpha}{\sum_{j\in\Pi_{\mathrm{feasible}}}(n_{k(a_j)}+\alpha)}, \alpha=1` — ":46: This is a real learned frequency prior, and live traces show it can govern a choice even when $G$ prefers another candidate. The residual is grain, not algebra: its categories are mission/action-class identities rather than complete pattern cascades". Footnote: "\srcref{futon2/src/futon2/aif/policy.clj} implements $\ln\widehat E-G/\tau$".
- **:50 Aliveness:** "the discharge-trained proposer credits the realised $L$ of a \emph{built} cascade back to the patterns it used".
- **:70 Substrate and Drawbridge** footnote: "guarded star/candidate writes are in \srcref{futon2/src/futon2/aif/a4a_substrate.clj}".
- **:76 Strategic mission selection:** "A principled strategic layer instead distinguishes (i) reason-bearing policy support, including hard executability and operator gates; (ii) predicted mission outcomes and their uncertainty …; (iii) context-conditioned proposal potentials; and (iv) the separate frequency habit $E_S(\pi_S)$. … an outer cascade of control patterns then acts directly as an ordered retrieval and candidate-construction policy."
- **:35** is the only entry with the phrase "policy space"; the literal phrase "action space" does not occur in the glossary (`grep -n -i 'action space' sec-glossary.tex` → not found).

---

## 3. EXCURSION — `futon3c/holes/excursions/E-R6-red-ring-fill.md` (249 lines)

**Premise as opened (:3–5):**
> **Opened:** 2026-08-26 · claude-13, from Joe: *"What I fear for R6 is another example of the same pattern: a selection over a small pre-ordained whitelist, or a 'wired' component that just no-ops, etc."*

**:7–8:** "**The fear is correct, and it is not one mechanism but four. The ring's own note describes none of them accurately.**"

**:12–15:** "**Verdict so far: R6 is red for none of the reasons recorded.** The tension proposer is built and live-installed; the ring says it is unbuilt. What is actually wrong is that the stratum **cannot account for what it generated or failed to generate**."

**Status table (:17–24), verbatim:**

| | state |
|---|---|
| the ring's stated reason | **FALSIFIED** — `aif2/tension.clj`, live at `war_machine.clj:4378` since 2026-06-01 |
| requirements | **STATED** — A (attestation), B (as-of), C (WR-20 registry), below |
| module property | **NAMED** — `surveyedSpace`, family 9 of the rosetta; `CandidateSpace.lean` unwritten |
| slice 1 — artifact provenance | **OPEN**, and blocking the module |
| slice 2 — full-loop proposer set | **OPEN**, cheap |
| WR-20 has no ring | **OPEN — Joe's call** |

**Nouns used** (as they appear): candidate space; proposer / tension-proposer / bootstrap / pattern-enumerator / mission-enumerator / sorry-enumerator / portfolio proposer; proposer set; action-class inventory (`action-types`); curvature signal / curvature artifact (`M-substrate-metric.R2-curvature-full.json`, `top_propose_candidates`); attestation; as-of / staleness; registry (WR-20); `surveyedSpace`; family 2 / family 3 / family 5 / family 9; `typedAbsence`, `selfContainedRecord` (GainChain.lean); `:learn-action-class`; "no addressable entities".

**The live composition (:41–43):**
> The live composition is five proposers, not one:
>     bootstrap · pattern-enumerator · mission-enumerator · sorry-enumerator · tension

**The four mechanisms (headings :55, :71, :90, :97):**
1. ":55 ### 1. The generative proposer generates from a four-entry frozen file" — ":57–60 `tension/read-curvature-signal` reads `futon3c/holes/missions/M-substrate-metric.R2-curvature-full.json`. That file **exists** — and is dated **2026-06-03**, ~12 weeks stale. Its `top_propose_candidates` key is a list of **four**."
2. ":71 ### 2. The action-class inventory is a set literal, against a ruling" — ":73 `forward_model.clj:25-32` — `action-types` is a hardcoded 14-element set" — ":76–78 **WR-20 (2026-05-31, Joe)** rules exactly against this: *"Move action-class support **and the proposer set** from code into extensible registries"*" — ":82–83 **`WR-20` appears zero times in `wr-overlay.edn`** — a ruling with no ring, while R6 carries only WR-19."
3. ":90 ### 3. A live component that no-ops on absent input" — ":92 `war_machine.clj:4367` — *"Fail-safe — absent/malformed ⇒ `[]` ⇒ tension-proposer silent ⇒ WM unchanged."*"
4. ":97 ### 4. A component dark by flag" — ":99–101 `portfolio_action_proposer.clj` — `:close-mission`, `:survey-mission`, `:apply-cascade` — `*portfolio-proposer-active?*` `false`, bound `true` only in `test/futon2/portfolio_dry_run.clj:36`. Never run live."

**Internal chain / edges drawn (:122–135):**
> Nothing distinguishes **"the space is genuinely empty"** from **"the generator did not run."**
> - tension silent — no tension, or no signal?
> - portfolio dark — no `:close-mission` candidates, or the proposer is off?
> - artifact frozen — these are the high-curvature nodes, or this is a June file?
> - set literal — these are the action kinds, or nobody added the fifteenth?
> Four indistinguishabilities, one property. And it is **family 2 at the SELECT stratum** — the same requirement as `typedAbsence` in `GainChain.lean` (out-of-domain ≠ no-data) and the same as R5's *an absence is reported with the discipline of a poor score*. One family, three columns.

**Requirement A (:137–165):**
> **An empty contribution is a record, not a missing row.** For every *registered* proposer, the composed space carries whether it ran, on what input, and what it produced — including `0`.
> **The implementation locus is exact.** `action-proposer/compose-proposers` is three lines: `(mapcat propose) → distinct → vec`. The protocol declares `proposer-id` *"for tracing / logging"* at `action_proposer.clj:31` and the composer **never calls it**. Two of five proposers self-stamp provenance into their own candidates — `pattern_registry.clj:360` `:proposer-id :pattern-enumerator`, `aif2/tension.clj:125` `:provenance {:proposer-id …}` — and bootstrap, sorry-enumerator and portfolio do not.
> **❌ The naive fix: make every proposer stamp its id.** It recreates the defect exactly. **A proposer that emits nothing stamps nothing** …
> **✅ The requirement-satisfying fix:** the *composer* writes one attestation per **registered** proposer, whether or not it emitted — `{:proposer-id … :ran? … :input-ref … :emitted n}`. The record is keyed by the registry, not by the output, so absence has somewhere to live.
> **Acceptance.** On a tick where nothing is addressable, the trace names five proposers with `:emitted 0` and a reason each. The 2026-07-15 archive is the counter-case: 24 attempts, `"no addressable entities"` seven times each, and no record of which proposers ran — so the operator learned it at attempt 24 rather than attempt 2.

**Requirement B (:167–184):** "**A live decision must not depend on evidence whose age is unstated.** Family 3 in `GainChain.lean` is `selfContainedRecord` … The dual: **a live read must carry its as-of, and its consumer must declare a staleness bound.** … ✅ The signal carries `:as-of`; `read-curvature-signal` returns `fresh(sig) | stale(sig, age) | absent` — three values, not `[]`".

**Requirement C (:186–201):** "**A stratum's inventory is data, and extension does not edit code.** This is not a new requirement — it is **WR-20 (2026-05-31)**, unimplemented at two of three strata. … **Acceptance.** A new action class or proposer is added with no edit to `forward_model.clj` and no edit to `war_machine.clj:4370`."

**The module property / CandidateSpace.lean / surveyedSpace / family 9 (:203–217):**
> In the shape module 1 established, and family 9 of the rosetta (`M-formal-war-machine` §2.1b), whose Lean cell currently reads *"unstated — the R6 module's slot"*:
> > **`surveyedSpace`** — the ordering step consumes only a space in which every registered contributor is accounted for, each contribution carrying its input's as-of.
> `DarkTower/WarMachine/CandidateSpace.lean` at the light standard, with refusal theorems named after the dated incidents — **2026-06-03** (a signal read as current three months on) and **2026-07-15** (24 attempts, no attestation) — and a positive witness. Not before slice 1: the artifact's provenance may change what "contributor" means.

**:221–224:** "Every naive fix above **adds an entry** … Every requirement-satisfying fix **removes the need for entries**, by making the registry the thing that is recorded rather than the output. That is `E-R8`'s test, and R6 fails it in four places at once."

**:228–230 What this makes R6:** "Not *"the proposer is unbuilt"*. R6 is **a stratum that can generate but cannot account for what it generated or failed to** — and the ring's recorded reason was the part nobody had checked, as with R8 and R14."

**Slices (:232–239):**
1. "**Provenance of the curvature artifact** — what writes it, when did it last run, is it meant to be live? *(discovery, one packet)*"
2. "**The full-loop runner's proposer composition** — same five, or fewer?"
3. "**WR-20's ring** — it governs R6's substance and has no node. … *Joe's call.*"
4. "Only then: what "generate from tension" would mean with a live signal."

**Tickets / rulings cited:** WR-19 (via `wr-overlay.edn`), WR-20 (`futon3/library/war-room/wr-20-action-class-inventory-becomes-data.flexiarg`), M-aif2 slice-1 (2026-06-01 live install), `E-R14-red-ring-fill.md`, `E-R8-red-ring-fill.md` (slice 4, slice 5), `p4ng/empirics-futon/NOTE-select-is-map-plus-derive.md` ("why R6 is MAP, and why the node holds two functions"), `M-formal-war-machine.md` §2.1b.

**07-15 archive claim (:103–113):**
> All 24 attempts recorded *"no addressable entities for X in current substrate"* across seven classes and selected `:learn-action-class` 22 times. That string is generated by `action_proposer/gap-actions`, so **the bootstrap proposer was the only one emitting**.
> **Not established:** whether that run composed the same five proposers. The composition read above is in `war_machine.clj`'s judge path; the 07-15 run went through the full-loop runner.

---

## 4. LEAN — `mathlib4/DarkTower/WarMachine/`

`ls -la /home/joe/code/mathlib4/DarkTower/WarMachine/` → `CascadeOrder.lean`, `CommitmentTemperature.lean`, `ContractEmitter.lean`, `CoverageReport.lean`, `GainChain.lean`, `PolicyGrade.lean` (six files).

`ls /home/joe/code/mathlib4/DarkTower/WarMachine/CandidateSpace.lean` → **not found** ("No such file or directory").

`grep -n -i 'surveyedSpace\|family 9\|candidate\|\bR6\b' /home/joe/code/mathlib4/DarkTower/WarMachine/*.lean`:

- `ContractEmitter.lean:195`: `"author ≠ reviewer holds in the WM today; E-R6 addresses candidate-space membership, which is family 9",`
- `ContractEmitter.lean:198`: `reservedJson' 9 "candidate-space-membership" .designedUnbuilt`
- `ContractEmitter.lean:199`: `"specified in E-R6-red-ring-fill; no Lean module yet"]`
- `GainChain.lean:129`: `-- translated from APMCycleMachine.lean:234 validStudentTerminalCandidate` (unrelated use of the word)
- `CommitmentTemperature.lean:109`: `(fun best candidate => if score best < score candidate then candidate else best)` (argmax helper; unrelated use of the word)

`surveyedSpace` → **not found** in any `.lean` file. `family 9` (as a string) → not found except via `reservedJson' 9`.

**ContractEmitter.lean:190–199 (`reservedJson`, full):**
```lean
def reservedJson : Json :=
  Json.arr #[
    reservedJson' 3 "self-contained-record" .prosOnly
      "the SCALE-MATCH PIN is enforced by care rather than by code (§2.1, family 3)",
    reservedJson' 6 "separated-powers" .alreadyHolds
      "author ≠ reviewer holds in the WM today; E-R6 addresses candidate-space membership, which is family 9",
    reservedJson' 7 "pinned-exit" .alreadyHolds
      "the act-gate holds today",
    reservedJson' 9 "candidate-space-membership" .designedUnbuilt
      "specified in E-R6-red-ring-fill; no Lean module yet"]
```
`.designedUnbuilt` serialises as `"designed-unbuilt"` (`:180`).

---

## 5. CODE — `futon2/src/futon2/aif/` (+ `aif2/`, `scripts/futon2/report/war_machine.clj`)

### 5a. `action_proposer.clj` (70 lines)

Defn list (`grep -n '^(def'`): `:27 (defprotocol ActionProposer` (methods `:28 propose`, `:31 proposer-id`), `:34 (defn- gap-actions`, `:48 (def bootstrap-proposer`, `:63 (defn compose-proposers`.

**ns docstring :18–23:** "Contract: this namespace doesn't move R-criteria on its own; it's compositional infrastructure for R6 (action selection). As of the R12 narrow-take-up landing, `:learn-action-class` recommendations carry an `:intrinsic-value` sourced from `futon2.aif.intrinsic-values/credit-for` (atom-backed posterior) rather than a static 0.1"

**:27–32 protocol:**
```clojure
(defprotocol ActionProposer
  (propose [_ state]
    "Return a seq of candidate action maps the proposer can offer for
     this state. May be empty if the proposer has nothing to offer.")
  (proposer-id [_]
    "Stable keyword identifier for tracing / logging."))
```

**:34–46 gap-actions (full):**
```clojure
(defn- gap-actions
  "For each action-type that is NOT proposable in the current state
   (excluding :no-op and :learn-action-class themselves), emit a
   :learn-action-class action carrying the rationale."
  [state]
  (let [base-types (disj fm/action-types :no-op :learn-action-class)
        gaps (remove #(fm/can-propose? state %) base-types)]
    (for [target-class gaps]
      {:type :learn-action-class
       :target-class target-class
       :intrinsic-value (iv/credit-for target-class)
       :rationale (str "no addressable entities for " target-class
                       " in current substrate")})))
```

**:57–61 bootstrap-proposer body:** `(propose [_ state] (concat [{:type :no-op}] (gap-actions state)))` `(proposer-id [_] :bootstrap)`.

**:63–70 compose-proposers (full):**
```clojure
(defn compose-proposers
  "Concatenate proposals from multiple proposers in order; de-duplicate
   by action-map equality. Returns a vector."
  [proposers state]
  (->> proposers
       (mapcat #(propose % state))
       distinct
       vec))
```

**Is `proposer-id` declared and never called?** `grep -rn 'proposer-id' src scripts test` (in `futon2`):

- Declaration: `src/futon2/aif/action_proposer.clj:31`.
- Implementations (`(proposer-id [_] …)`): `action_proposer.clj:61` `:bootstrap`; `pattern_registry.clj:379` `:pattern-enumerator`; `mission_registry.clj:349` `:mission-enumerator`; `sorry_registry.clj:126` `:sorry-enumerator`; `portfolio_action_proposer.clj:90` `:portfolio-action`; `aif2/tension.clj:158` `(:id entry)`.
- **Calls of the protocol fn in `src`/`scripts`: none.** Every non-test hit outside the implementations is a *map key* named `:proposer-id` stamped on candidates: `pattern_registry.clj:281,304,309,360` (`:proposer-id :pattern-enumerator`), `aif2/tension.clj:125` (`:provenance {:proposer-id (:id entry) …}`), `full_loop_runner.clj:1233` (`select-keys action [:type :target :mission-path :target-class :proposer-id …]`).
- Calls in `test` only: `action_proposer_test.clj:87`, `sorry_registry_test.clj:70`, `mission_registry_test.clj:191`, `portfolio_dry_run.clj:56`, `aif2/tension_test.clj:124`.
- `compose-proposers` (`:63–70`) does not reference `proposer-id`.

### 5b. `policy.clj` (`select-action`, abstain, gap-report)

Defn list: `:21 adaptive-temperature`, `:35 effective-temperature`, `:71 softmax-weights`, `:95 find-no-op`, `:100 gap-report`, `:109 default-mode-select`, `:168 candidate-explanation`, `:182 decision-explanation`, `:215 ranking-entry`, `:223 strategic-recommendation`, `:289 select-action`.

**ns docstring :4–19:** "`select-action` is the top-level R6 deliverable: take a ranked-action list (from `efe/rank-actions`), apply softmax over controller-scores with adaptive temperature τ, and return either the chosen action or an abstain branch with a structured gap-report. Contract: contributes to R6 (softmax action selection with abstain) per `futon2/docs/futon-aif-completeness.md`. … Theory: AIF softmax selection — `P(a) ∝ exp(−G(a) / τ)`. Adaptive τ scales with EFE spread: tight spreads → high τ → diffuse selection → abstain trips. Abstain semantics: when the best action's controller-score is not meaningfully below `:no-op`'s controller-score, the WM declines to act and surfaces a gap-report enumerating the `:learn-action-class` recommendations the bootstrap proposer detected."

**:100–107 gap-report (full):**
```clojure
(defn- gap-report
  "Enumerate the :learn-action-class recommendations in a ranked-action
   list (their action maps), in order of appearance. Used by abstain to
   surface capability gaps to the operator."
  [ranked]
  (->> ranked
       (filter #(= :learn-action-class (-> % :action :type)))
       (mapv :action)))
```

**:289–340 select-action docstring (extract):**
> Abstain branch:
>   {:action :abstain
>    :reason :no-action-beats-no-op | :no-candidates
>    :gap-report <vec of :learn-action-class action maps, possibly empty>
>    :ranked-actions <input ranked-actions, for trace>}
> Abstain fires when:
> - ranked-actions is empty, OR
> - :no-op is present AND (no-op.controller-score − best.controller-score) < ε
>   (i.e. the best action isn't meaningfully better than doing nothing).

Also `:294–295`: ":abstain-epsilon — minimum (no-op.controller-score − best.controller-score) required to NOT abstain. Default 0.01." and `:305–311`: ":selection-boundary — :actuation (default, historical semantics) or :strategic-recommendation. The latter emits a live **non-abstaining** controller-head recommendation, treats the current scheduler-grain habit as an inspectable counterfactual, and leaves enactment to downstream act gates."

**Abstain sites in code:** `:350–354` (`(empty? ranked-actions)` → `:reason :no-candidates`), `:379–385` (no-priors path: `abstain? (and no-op-entry (< (- no-op-g best-g) abstain-epsilon))` → `:reason :no-action-beats-no-op`), `:408–414` (priors path, same test against `chosen-g`). `:372–373`: when `(= :strategic-recommendation selection-boundary)` the code returns `(strategic-recommendation …)` before any abstain check.

**:223–228 strategic-recommendation docstring:** "Select the controller head at the strategic mission grain while retaining the scheduler-grain habit calculation as an inspectable counterfactual. This is a live selection, not an operator-approval request. It deliberately does not authorize enactment: downstream act gates own that decision." Body `:236–238`: `controller-entries (filterv #(not= :no-op (get-in % [:action :type])) ranked-actions)`, `chosen (or (first controller-entries) (first ranked-actions))`; output includes `:no-op-comparison {:controller-score no-op-g :controller-margin (when no-op-g (- no-op-g chosen-g)) :blocks-recommendation? false}` (`:277–280`).

**The "four abstain trigger conditions" as tested** — `test/futon2/aif/policy_test.clj:177–221`:
1. `:180 select-action-empty-input-abstains-test` — "empty ranked input → abstain with :no-candidates reason"
2. `:186 select-action-abstains-when-no-action-beats-no-op-test` — `[[:no-op 0.5] [:address-sorry 0.499]]` → `:no-action-beats-no-op`
3. `:194 select-action-abstain-no-op-only-test` — ":no-op alone in the candidates → abstain"
4. `:200 select-action-abstain-gap-report-test` — two `:learn-action-class` entries at 0.501/0.502 vs no-op 0.5 → abstain with `(= 2 (count (:gap-report out)))`
Plus `:217 select-action-no-no-op-returns-best-test` ("if :no-op isn't in candidates, best is always returned (no abstain)") and `:225 select-action-epsilon-tunable-test`.

**Temperature (`:21–70`):** `adaptive-temperature`: `(max tau-min (/ spread k))`, defaults `tau-min 0.01 k 5.0`. `effective-temperature` docstring `:36–37`: "TWO layers, separated per the R6 faithfulness audit (M-aif-faithfulness §2.2 B-2d)"; `:tau-mode :spread (DEFAULT) τ_eff = τ_spread / g`; `:selection-gain-only τ_eff = 1 / g`; "`Both modes are engineering calibration policies and are reported as such.`"

### 5c. Who builds the candidate set

`ls src/futon2/aif/ | grep -i 'candidate\|constellation\|feasib\|support'` → **not found** (no `candidate_space` or `constellation` namespace). `find src scripts -iname '*candidate*' -o -iname '*constellation*'` → no output.

**Only call site of `compose-proposers`:** `scripts/futon2/report/war_machine.clj:4370` (`grep -rn 'compose-proposers' src scripts`; the other hit, `aif2/tension.clj:145`, is a docstring).

**`war_machine.clj:4362–4379` (the live proposer vector):**
```clojure
        wm-state {:observation observation :belief wm-belief :sorrys wm-sorrys
                  :missions wm-missions
                  :patterns wm-patterns
                  :anticipation anticipation-snapshot
                  ;; M-aif2 slice-1 live install (consent-gated, Joe 2026-06-01):
                  ;; inject the delivered E1 curvature signal. Fail-safe —
                  ;; absent/malformed ⇒ [] ⇒ tension-proposer silent ⇒ WM unchanged.
                  :curvature-signal (tension/read-curvature-signal)}
        wm-candidates (ap/compose-proposers
                       [ap/bootstrap-proposer
                        pattern-registry/pattern-enumerator-proposer
                        mission-registry/mission-enumerator-proposer
                        sorry-registry/sorry-enumerator-proposer
                        ;; M-aif2 slice-1: credited + admissibility-gated
                        ;; tension-proposer — emits existing S2 classes via κ at
                        ;; high-curvature actionable substrate-2 nodes (E1 consume).
                        (tension/tension-proposer)]
                       wm-state)
```
`portfolio-action-proposer` is not in this vector.

**Pipeline after composition (`war_machine.clj`):** `:4391–4397` `wm-enriched-candidates (->> wm-candidates enrich-candidates-with-structural-pressure (#(enrich-candidates-with-mission-value % recent-trace-records)) interest-net/enrich-candidates)` → `:4439` `wm-ranked-domain-base (efe/rank-actions wm-state wm-enriched-candidates wm-efe-opts)` → `:4440` `wm-policy-exclusions (-> wm-ranked-domain-base meta :policy-support/excluded)` → `:4441–4448` habit-prior attach → `:4449` `wm-ranked (->> wm-ranked-domain apply-anamnesis-tiebreak …)` → `:4470–4485`:
```clojure
        ;; v0.13 R6 enhancement: pre-filter by can-execute? admissibility
        ;; (composes with can-propose? at proposer-side); then run
        ;; deliberative select-action with default-mode-select as a
        ;; try/catch fallback for I6 compositional closure.
        wm-admissible (filterv #(fm/can-execute? wm-state (:action %)) wm-ranked)
        controller-decision
        (try (policy/select-action
              wm-admissible
              {:selection-gain selection-gain-value
               :selection-boundary :strategic-recommendation
               …
               :temperature-opts {:tau-mode (arena-tau-mode)}})
             (catch Exception _
               (policy/default-mode-select wm-state wm-admissible)))
```
`git log -S':selection-boundary :strategic-recommendation' -- scripts/futon2/report/war_machine.clj` (read-only) → `191e168 2026-07-23 separate strategic recommendation from actuation`.

Trace keys written (`:4696–4701`): `:ranked-actions wm-ranked+cascades` (cascade actions appended to the ranked vector, `wm-ranked+cascades (vec (map-indexed … (into (vec wm-ranked) cascade-actions)))`), `:admissible-actions wm-admissible` with comment "The decision is drawn from this executable support, not the served ranked/advisory display. Full-loop discrimination must inspect the same Π_feasible domain.", `:policy-support-exclusions (vec wm-policy-exclusions)`, `:decision wm-decision`.

**Full-loop runner:** `full_loop_runner.clj:2156–2158` `selection-judge (or (:judge-fn opts) (fn [days] (wm/generate-war-machine days {…})))` — it calls the same `war_machine` judge; `grep -n -i 'proposer\|compose-proposers\|bootstrap' src/futon2/aif/full_loop_runner.clj` → only `:905–922` (the `:learn-action-class` capability-contract text: `:required-components [:addressable-substrate-enumerator :action-proposer-registration :instance-executability-check :production-actuation-path]`, acceptance `{:check :proposer-support :claim "can-propose? is true only when real addressable targets exist"}` …) and `:1233` (`select-keys … :proposer-id`). `:872–880 repair-entry`: `{:action {:type :repair-machine-failure …} :controller-score ##-Inf :G-efe ##-Inf :selection-source :stop-the-line}`.

**What makes a candidate feasible:**

- Proposer-side gate — `forward_model.clj:227–242 can-propose?`: "Per-action-type capability check: can this action class be addressed against the current substrate? Default: false (the WM has no proposer for it)." Arms: `:default false`, `:no-op`/`:learn-action-class`/`:pursue`/`:decompose` true; `sorry_registry.clj:58 :address-sorry (boolean (seq (:sorrys state)))`; `mission_registry.clj:306 :open-mission` and `:314 :advance-mission` `(boolean (seq (:missions state)))`; `pattern_registry.clj:272 :fire-pattern (boolean (some addressable-pattern? (:patterns state)))`. No arms for `:close :close-mission :close-hole :survey :survey-mission :apply-cascade` (→ default false → `gap-actions` emits `:learn-action-class` for each).
- `forward_model.clj:25–32 action-types`: `#{:no-op :address-sorry :open-mission :advance-mission :close :close-mission :close-hole :survey :survey-mission :apply-cascade :fire-pattern :learn-action-class :pursue :decompose}` (14 entries); docstring "Extending the set means adding both a `predict-effects` multimethod arm (below) and a `can-propose?` arm if the action requires substrate addressability."
- Feasibility mask (Π_feasible) — `efe.clj:155–164` docstring: "the applicability penalty (1000·[not-applicable], and the off-map penalty) is a DOMAIN RESTRICTION — canonical seat Π_feasible, a mask on the policy domain, not a value judgment … In `:policy-support` mode, `rank-actions` excludes infeasible policies before scoring". `efe.clj:233–252 policy-support-verdict`: applies only when `graph-feasibility-mode = policy-support-mode`, graph non-nil, and `(:type action) = :open-mission`; verdicts `{:feasible? true :reason :not-graph-gated}`, `{:feasible? false :reason :mission-absent-from-capability-graph}`, `{:feasible? false :reason :required-capabilities-unsatisfied :required (vec (:scope mission))}`, `{:feasible? true :reason :graph-applicable}`. `efe.clj:254–265 partition-policy-support`: "Return {:included actions :excluded [{:action :reason ...}]}. This is the Π_feasible boundary; excluded policies never receive a controller score." `efe.clj:808–825 rank-actions` returns `(with-meta ranked {:policy-support/excluded (vec excluded)})`.
- Execution-side gate — `forward_model.clj:252–268 can-execute?`: "Per-action-instance admissibility check. Default `true` — if a proposer surfaced the action, it's assumed executable unless an action-type-specific override says otherwise." `:address-sorry` executable only if target is in `(:sorrys state)`; `mission_registry.clj:310,318` `:open-mission`/`:advance-mission` → `live-mission-target?`; `pattern_registry.clj:277` `:fire-pattern` → `addressable-pattern?`.
- Tension proposer gate — `aif2/tension.clj:95–103 admissible?`: `(and (not= :pruned (:status entry)) (:operator-ratified? consent))`; `:89–93 default-consent {:operator-ratified? true}`; `:198–209 read-curvature-signal`: "Returns [] on ANY error/absence" reading `:top_propose_candidates` from `:182 "/home/joe/code/futon3c/holes/missions/M-substrate-metric.R2-curvature-full.json"`. `ls -la --time-style=long-iso` of that file → `9877 2026-06-03 20:28`; `python3 -c "…len(d['top_propose_candidates'])"` → `4`; no date/as-of key in the JSON (top-level keys: `bridge_candidate_count, component_count, curvature_distribution, curvature_edges_sampled, edge_cap, edge_count, fetched_by_type, largest_component, limit_per_type, node_count, report, timing, top_negative_edges, top_propose_candidates`).
- Portfolio proposer — `portfolio_action_proposer.clj:6–7`: "DARK by default (*portfolio-proposer-active?* false). When off, propose returns []"; `:83 (if-not *portfolio-proposer-active?* [] …)`; `grep -rn 'portfolio-proposer-active' src scripts test` → bound `true` only at `test/futon2/portfolio_dry_run.clj:36`.

**Other proposers' emitted shapes:** `mission_registry.clj:335–349` emits `{:type :advance-mission :target (:id m) :weight 1.0 :mission-path … :open-hole-count … :rationale (str "mission substrate: " …)}` — no `:proposer-id` key. `sorry_registry.clj:118–126` emits `{:type :address-sorry :target (:id s) :weight 1.0 :intrinsic-value … :rationale (str "open sorry: " …)}` — no `:proposer-id` key. `pattern_registry.clj:355–378` emits `{:type :fire-pattern … :proposer-id :pattern-enumerator :retrieval-score … :retrieval-rationale …}`. `aif2/tension.clj:113–130 route-candidate` emits `{:type (kappa node-type) :target … :weight … :intrinsic-value (iv/credit-for (:id entry)) :rationale … :provenance {:proposer-id (:id entry) :min-kappa … :strain-edge … :resolvedness … :inclusion-reason "curvature-strain ∧ unresolved ∧ actionable"}}`.

**`trace.clj`:** `:168–169` schema note "10 — records the live/legacy disposition of predictability, homeostatic, and graph-feasibility controls per ranked action, and adds the top-level :policy-support-exclusions audit trail (2026-07-13)". `:262–264` the trace record builder writes `:ranked-actions (mapv strip-ranked-action …)`, `:policy-support-exclusions (vec …)`, `:decision (strip-decision …)`. `grep -n 'admissible-actions' src/futon2/aif/trace.clj` → no output (the key written at `war_machine.clj:4699` is not in the trace builder).

---

## 6. BADGES

### 6a. `futon2/data/r18-badges.edn`

`grep -n -i 'R6\|effective-temperature-softmax\|gap-exploration-bonus'` → 29, 51, 53, 146, 255.

- `:29` `"R6" "Policy selection with abstention"` (requirement-name table).
- `:53` `:SEL {:requirement "R6" :quantities [:effective-temperature-softmax]}`.
- `:49–52` `:GCTRL {:requirement "R5" :quantities [:controller-score :predictability-bonus :homeostatic-pressure :structural-pressure :graph-control-score :gap-exploration-bonus :model-uncertainty-bonus]}` (so `:gap-exploration-bonus` sits under R5's controller node, not R6).
- There is no `:R6` key in the file (`grep -n ':R6' data/r18-badges.edn` → not found).

**`:146–155 :effective-temperature-softmax` (full):**
```clojure
  :effective-temperature-softmax
  {:badge :engineering-control,
   :claims "spread-normalised controller softmax",
   :cite "Da Costa et al. 2020, P(π) = σ(−γ·G(π))",
   :code-ref "futon2/src/futon2/aif/policy.clj:35",
   :computes
   "LIVE softmax(−G/τ_eff), τ_eff = 1/g, where g is the explicitly engineering outcome-feedback selection gain; FUTON_WM_TAU_MODE=spread restores historical τ_spread/g",
   :repair
   "complete for removal of the spread-normalisation residual; g remains an engineering control rather than inferred variational policy precision",
   :repair-built
   "B-2d :selection-gain-only FLIPPED LIVE by Joe 2026-07-13. Existing shadow established zero winner or abstain changes by construction and an entropy-only commitment effect. FUTON_WM_TAU_MODE=spread is the provenance-stamped rollback hatch; reviewer-side badge/disposition audit remains separate."},
```

**`:255–266 :gap-exploration-bonus` (full):**
```clojure
  :gap-exploration-bonus
  {:badge :engineering-control,
   :claims "caller-supplied mission-gap exploration bonus",
   :cite "engineering controller augmentation; explicitly not EIG",
   :code-ref "futon2/src/futon2/aif/efe.clj:176",
   :computes
   "6.0 × caller-supplied gap-score lookup; no expected-information computation",
   :repair
   "complete: renamed and kept outside G-efe; a future EIG term must compute expected uncertainty reduction",
   :note
   "M-evaluate-policies 2026-07-03: now PERSISTED (2d6533e whitelist; was stripped — part of the hidden 8.3%-flip residual, driven to 0.000e0 live). Disposition: real expected-uncertainty-reduction or fold into pragmatic; 6.0-weight saturation flagged (E-possible-world-regulator).",
   :repair-built
   "B3 shares the real policy-conditioned EIG kernel in epistemic_value.clj. UNWIRED: a gap label can index where to inspect, but it cannot enter EIG until each policy predicts observations and their posterior updates. The lookup remains an honestly named engineering control."}
```

### 6b. `p4ng/empirics-futon/wr-overlay.edn:40`

```
{:node "R6"  :wr "WR-19" :holds false :note "tension must GENERATE, not only rank -- the candidate space is ranked, not proposed; tension-proposer unbuilt"}
```

### 6c. `futon2/holes/aif-r1-r16-pattern-map.md:32`

Table header (`:24`): `| R | requirement | pattern (`library/aif/…`) | status | the honest reality |`

```
| **R6** | Softmax selection with abstain | `candidate-pattern-action-space` | ✓ | Real. |
```
(the only R6 row; `grep -n 'R6'` returns line 32 only.)

---

## 7. CORPUS — `futon2/data/wm-trace/wm-trace-2026-*.edn` (52 files, 791 records)

**Command** (reader loop over all top-level forms; `clojure.edn/read` on a `PushbackReader` with `:eof` sentinel and `{:default (fn [t v] v)}`):

```bash
cat > /tmp/scan-r6.clj <<'EOF'
(require '[clojure.edn :as edn] '[clojure.java.io :as io])
(defn read-forms [f]
  (with-open [r (java.io.PushbackReader. (io/reader f))]
    (loop [acc []]
      (let [x (edn/read {:eof ::eof :default (fn [t v] v)} r)]
        (if (= x ::eof) acc (recur (conj acc x)))))))
(defn median [xs] (let [s (vec (sort xs)) n (count s)] (when (pos? n) (nth s (quot n 2)))))
(def files (->> (file-seq (io/file "/home/joe/code/futon2/data/wm-trace"))
                (filter #(re-matches #"wm-trace-2026-.*\.edn" (.getName %)))
                (sort-by #(.getName %))))
(def all (atom []))
(println "file | records | ranked-actions count min/med/max | abstain | chosen | no-decision | has-exclusions")
(doseq [f files]
  (let [forms (try (read-forms f) (catch Exception e (println "ERR" (.getName f) (.getMessage e)) []))
        recs (filter map? forms)
        ra (map #(count (:ranked-actions %)) recs)
        dec (map :decision recs)
        abst (count (filter #(= :abstain (:action %)) dec))
        chosen (count (filter #(and (map? %) (map? (:action %))) dec))
        nodec (count (filter nil? dec))
        excl (count (filter #(contains? % :policy-support-exclusions) recs))]
    (swap! all into recs)
    (println (.getName f) "|" (count recs) "|" (when (seq ra) (str (apply min ra) "/" (median ra) "/" (apply max ra))) "|" abst "|" chosen "|" nodec "|" excl)))
(let [recs @all
      ra (map #(count (:ranked-actions %)) recs)
      decs (map :decision recs)]
  (println "\nTOTAL records:" (count recs))
  (println "ranked-actions count overall min/med/max:" (apply min ra) (median ra) (apply max ra))
  (println "decision :action = :abstain:" (count (filter #(= :abstain (:action %)) decs)))
  (println "decision chosen (map action):" (count (filter #(map? (:action %)) decs)))
  (println "decision nil:" (count (filter nil? decs)))
  (println "abstain :reason freq:" (frequencies (map :reason (filter #(= :abstain (:action %)) decs))))
  (println "chosen decision :action :type freq:" (frequencies (map #(get-in % [:action :type]) (filter #(map? (:action %)) decs))))
  (println "ranked-actions :action :type freq (all entries):" (frequencies (mapcat #(map (fn [e] (get-in e [:action :type])) (:ranked-actions %)) recs)))
  (println "records with :policy-support-exclusions key:" (count (filter #(contains? % :policy-support-exclusions) recs)))
  (println "  non-empty exclusions:" (count (filter #(seq (:policy-support-exclusions %)) recs)))
  (println "  exclusion :reason freq:" (frequencies (map :reason (mapcat :policy-support-exclusions recs))))
  (println "  sample exclusion:" (first (mapcat :policy-support-exclusions recs)))
  (println "records with :admissible-actions:" (count (filter #(contains? % :admissible-actions) recs)))
  (println "records with :gap-report in decision:" (count (filter #(contains? (:decision %) :gap-report) recs)))
  (println "records whose ranked-actions contain :learn-action-class:" (count (filter (fn [r] (some #(= :learn-action-class (get-in % [:action :type])) (:ranked-actions r))) recs)))
  (println "any key containing 'proposer' or 'attest' at top level:" (distinct (filter #(re-find #"(?i)propos|attest" (name %)) (mapcat keys recs))))
  (println "any ranked-action entry :action with :proposer-id:" (count (filter #(get-in % [:action :proposer-id]) (mapcat :ranked-actions recs))))
  (println "any ranked-action entry :action with :provenance:" (count (filter #(get-in % [:action :provenance]) (mapcat :ranked-actions recs)))))
EOF
bb /tmp/scan-r6.clj
```

**Output — per file** (`file | records | ranked-actions min/med/max | abstain | chosen | no-decision | has-exclusions`):

```
wm-trace-2026-05-18.edn | 7 | 4/5/15 | 0 | 7 | 0 | 0
wm-trace-2026-05-19.edn | 9 | 15/15/15 | 0 | 9 | 0 | 0
wm-trace-2026-05-21.edn | 13 | 153/154/154 | 0 | 13 | 0 | 0
wm-trace-2026-05-22.edn | 24 | 154/154/155 | 0 | 24 | 0 | 0
wm-trace-2026-05-23.edn | 24 | 155/157/160 | 0 | 24 | 0 | 0
wm-trace-2026-05-24.edn | 24 | 160/161/161 | 0 | 24 | 0 | 0
wm-trace-2026-05-25.edn | 21 | 159/161/218 | 0 | 21 | 0 | 0
wm-trace-2026-05-26.edn | 17 | 211/216/218 | 0 | 17 | 0 | 0
wm-trace-2026-05-27.edn | 9 | 211/215/215 | 0 | 9 | 0 | 0
wm-trace-2026-05-30.edn | 2 | 182/211/211 | 0 | 2 | 0 | 0
wm-trace-2026-05-31.edn | 19 | 181/183/184 | 0 | 19 | 0 | 0
wm-trace-2026-06-01.edn | 12 | 182/184/194 | 0 | 12 | 0 | 0
wm-trace-2026-06-02.edn | 14 | 194/196/196 | 0 | 14 | 0 | 0
wm-trace-2026-06-03.edn | 24 | 194/196/198 | 0 | 24 | 0 | 0
wm-trace-2026-06-04.edn | 24 | 196/198/199 | 0 | 24 | 0 | 0
wm-trace-2026-06-05.edn | 15 | 197/199/199 | 0 | 15 | 0 | 0
wm-trace-2026-06-06.edn | 24 | 82/197/199 | 0 | 24 | 0 | 0
wm-trace-2026-06-07.edn | 23 | 82/82/85 | 0 | 23 | 0 | 0
wm-trace-2026-06-08.edn | 24 | 85/85/88 | 0 | 24 | 0 | 0
wm-trace-2026-06-09.edn | 23 | 86/88/92 | 0 | 23 | 0 | 0
wm-trace-2026-06-10.edn | 22 | 90/95/207 | 0 | 22 | 0 | 0
wm-trace-2026-06-12.edn | 7 | 121/121/121 | 0 | 7 | 0 | 0
wm-trace-2026-06-13.edn | 24 | 121/122/124 | 0 | 24 | 0 | 0
wm-trace-2026-06-14.edn | 20 | 123/123/134 | 0 | 20 | 0 | 0
wm-trace-2026-06-15.edn | 8 | 115/115/134 | 0 | 8 | 0 | 0
wm-trace-2026-06-16.edn | 14 | 106/115/116 | 0 | 14 | 0 | 0
wm-trace-2026-06-17.edn | 15 | 106/106/108 | 0 | 15 | 0 | 0
wm-trace-2026-06-18.edn | 12 | 106/106/106 | 0 | 12 | 0 | 0
wm-trace-2026-06-21.edn | 3 | 106/106/106 | 0 | 3 | 0 | 0
wm-trace-2026-06-22.edn | 17 | 106/108/108 | 0 | 17 | 0 | 0
wm-trace-2026-06-23.edn | 13 | 107/109/109 | 0 | 13 | 0 | 0
wm-trace-2026-06-24.edn | 10 | 107/109/109 | 0 | 10 | 0 | 0
wm-trace-2026-06-25.edn | 15 | 108/110/112 | 0 | 15 | 0 | 0
wm-trace-2026-06-26.edn | 12 | 110/110/112 | 0 | 12 | 0 | 0
wm-trace-2026-06-27.edn | 11 | 110/110/112 | 0 | 11 | 0 | 0
wm-trace-2026-06-28.edn | 14 | 110/110/110 | 0 | 14 | 0 | 0
wm-trace-2026-06-29.edn | 18 | 108/110/110 | 0 | 18 | 0 | 0
wm-trace-2026-06-30.edn | 18 | 108/110/110 | 0 | 18 | 0 | 0
wm-trace-2026-07-01.edn | 23 | 110/110/112 | 0 | 23 | 0 | 0
wm-trace-2026-07-02.edn | 34 | 108/110/112 | 0 | 34 | 0 | 0
wm-trace-2026-07-03.edn | 20 | 108/110/110 | 0 | 20 | 0 | 0
wm-trace-2026-07-04.edn | 38 | 110/110/146 | 0 | 38 | 0 | 0
wm-trace-2026-07-05.edn | 18 | 101/145/145 | 0 | 18 | 0 | 0
wm-trace-2026-07-06.edn | 21 | 106/107/109 | 0 | 21 | 0 | 0
wm-trace-2026-07-09.edn | 1 | 107/107/107 | 0 | 1 | 0 | 0
wm-trace-2026-07-14.edn | 14 | 113/113/113 | 0 | 14 | 0 | 14
wm-trace-2026-07-15.edn | 6 | 113/113/115 | 0 | 6 | 0 | 6
wm-trace-2026-07-16.edn | 6 | 116/116/116 | 0 | 6 | 0 | 6
wm-trace-2026-07-17.edn | 1 | 114/114/114 | 0 | 1 | 0 | 1
wm-trace-2026-07-18.edn | 1 | 114/114/114 | 0 | 1 | 0 | 1
wm-trace-2026-07-19.edn | 1 | 116/116/116 | 0 | 1 | 0 | 1
wm-trace-2026-07-21.edn | 2 | 119/122/122 | 0 | 2 | 0 | 2
```

**Output — totals:**

```
TOTAL records: 791
ranked-actions count overall min/med/max: 4 112 218
decision :action = :abstain: 0
decision chosen (map action): 791
decision nil: 0
abstain :reason freq: {}
chosen decision :action :type freq: {:learn-action-class 146, :address-sorry 459, :open-mission 96, :advance-mission 90}
ranked-actions :action :type freq (all entries): {:learn-action-class 1052, :no-op 791, :address-sorry 3375, :open-mission 53869, :fire-pattern 948, :advance-mission 43352, :apply-cascade 450}
records with :policy-support-exclusions key: 31
  non-empty exclusions: 31
  exclusion :reason freq: {:mission-absent-from-capability-graph 124}
  sample exclusion: {:reason :mission-absent-from-capability-graph, :action {:type :open-mission, :target futon4-d/mission/essays-diachronic-model, :weight 0.5000000000000002, :intrinsic-value 0.5, :rationale tension at mission futon4-d/mission/essays-diachronic-model: min-incident-κ -0.5556, resolvedness 0.10 — strain bridge [...], :provenance {:proposer-id :s1/tension, :min-kappa -0.5555555555555558, :strain-edge [...], :resolvedness 0.1, :inclusion-reason curvature-strain ∧ unresolved ∧ actionable}, :structural-pressure-per-action 0.0}}
records with :admissible-actions: 0
records with :gap-report in decision: 0
records whose ranked-actions contain :learn-action-class: 604
any key containing 'proposer' or 'attest' at top level: ()
any ranked-action entry :action with :proposer-id: 33
any ranked-action entry :action with :provenance: 1327
```

**Decision shape (last record, 07-21):** `decision keys: (:habit-prior-applied? :tau-spread :rank :controller-score :decision-explanation :action :tau :selection-gain)`; `:reason nil`; no `:selection-boundary` key. Top-level keys of that record: `(:anticipation :decision :free-energy :habit-prior-state :micro-step-trace :mode :morning-brief-consumed-event-ids :morning-brief-events :morning-brief-held-events :mu-post :mu-pre :observation :policy-support-exclusions :precision-state :prediction-errors :ranked-actions :selection-gain :timestamp :variational-free-energy :wm-version)`.

**Which proposers ran — per-file proposer-id stamps found inside ranked-actions** (second script `/tmp/scan-r6b.clj`, same reader, counting `(or (get-in e [:action :proposer-id]) (get-in e [:action :provenance :proposer-id]))`):

- `:s1/tension` (via `:provenance`) present 06-01 through 07-09 — e.g. 06-05: 60, 06-06: 86, 07-02: 68, 07-04: 76, 07-09: 1. (06-01..06-04 carry it too: a 06-01 check found 4 ranked entries with `:action :provenance {:proposer-id :s1/tension …}`, `:type :open-mission`.)
- `:pattern-enumerator` (via `:proposer-id`) present 07-15: 6, 07-16: 18, 07-19: 3, 07-21: 6.
- No stamp for `:bootstrap`, `:mission-enumerator`, `:sorry-enumerator`, or `:portfolio-action` anywhere in the corpus.

**Raw greps:**
- `grep -c 'proposer' wm-trace-2026-*.edn | grep -v ':0$'` → nonzero for 41 files from `06-01` (1) to `07-21` (2); zero for all May files.
- `grep -c 'attestation' wm-trace-2026-*.edn | grep -v ':0$'` → **no output** (zero in every file).
- `grep -c ':abstain' wm-trace-2026-*.edn | grep -v ':0$'` → `07-02: 4`, `07-05: 13`, `07-06: 18`. Context (`grep -o '.\{0,40\}:abstain.\{0,40\}' wm-trace-2026-07-05.edn | sort | uniq -c`): `"M-canon-fingerprint-store", :verdict :abstain-missing-leg, :delta-F -0.454, :delta-G` (×4) and `… :verdict :abstain-missing-leg, :delta-F -0.598 …` (×9); the third script confirms the only top-level key containing `:abstain` is `:act-gate-verdicts`, not `:decision`.

### 7b. The 07-15 wm-full-loop archive

Directory: `/home/joe/code/futon2/data/wm-full-loop/archives/stop-line-2026-07-15/wm-outer-loop-40-v1/` — `ls | grep -c attempt` → **24** attempts.

`grep -rho 'no addressable entities' wm-outer-loop-40-v1 | wc -l` → **205** occurrences (`002-selection.edn` files: 9 per attempt for 22 attempts, 7 for attempt-001; attempt-023 has 0).

`grep -rho 'no addressable entities for :[a-z-]*' wm-outer-loop-40-v1 | sort | uniq -c`:
```
     23 no addressable entities for :apply-cascade
     23 no addressable entities for :close
     23 no addressable entities for :close-hole
     23 no addressable entities for :close-mission
     67 no addressable entities for :fire-pattern
     23 no addressable entities for :survey
     23 no addressable entities for :survey-mission
```
(seven action classes.)

Selected action types (`grep -A1 ':selected-action' */002-selection.edn | grep -o ':type :[a-z-]*' | sort | uniq -c`): `22 :type :learn-action-class`, `1 :type :address-sorry` (attempt-001). Each selection file carries 12 `:rank` entries in `:ranked-candidates`; attempt-023's payload is `{:sorry {:outcome :agent-unavailable, :kind :not-reached-selection}}` (recorded-at `2026-07-15T18:00:07Z`).

`grep -rl 'proposer' .` in the archive → only `derived-projections/failed-wm-traces.edn` (no `002-selection.edn` names a proposer).

Across the whole `data/wm-full-loop/` tree (all cohorts 40–46, incl. archives): `grep -rho 'no addressable entities' data/wm-full-loop/ | wc -l` → 2375; classes `:apply-cascade 336, :close 335, :close-hole 335, :close-mission 335, :fire-pattern 293, :survey 335, :survey-mission 406`.

---

## 8. MISSION — `futon2/holes/missions/M-formal-war-machine.md`

`grep -n -i '\bR6\b\|family 9\|surveyedSpace\|weights versus membership\|proposer\|attestation\|Tier 1 #1'` → 43, 199, 201, 204, 373, 457, 720, 726, 985, 1666–1668, 1911, 1922, 1929, 1941.

**:199 (rosetta row, family 9):**
```
| **9** | **the ordering step consumes a surveyed space** | **⑪**° | *unstated — the R6 module's slot* | — | `full_loop_runner.clj:872` `repair-entry` (works, `##-Inf`); `portfolio_action_proposer.clj` (dark) | **R6** |
```
**:201 (family 10 row):** `| **10** | **the cascade is a well-formed order** | **⑳**, gated at ㉒ | `acyclicDescent`, `hasMeets` — *dispatched 2026-08-27* | `CascadeOrder.lean` | … | R6/R11 |`

**:203–205:** "**Family 10 is new, 2026-08-27.** No existing family covers whether the cascade the loop constructs is well formed: family 9 is about the *space consumed* at ⑪, this is about the *structure produced* at ⑳."

**:372–376:**
> Both new families are in **SELECT**, and both are where 2026-08-26's work landed: family 8 is `I(τ ; action) = 0` on the enacting path, family 9 is the difference between changing a ranking's *weights* and changing its *membership*. So the rosetta grows in two directions — a column (implementation) and rows (WM-native requirements APM cannot state).

**:455–458:** "of five, the ring's recorded reason was wrong or unverified** — R8's "no producer" (there was one, armed 07-08), R14's "not yet located in the code" (`policy.clj:35`), R6's "tension-proposer unbuilt" (`aif2/tension.clj`, live since 06-01), …"

**:720 (module table):** `| **R6** | `E-R6-red-ring-fill` | — | — | reserved, family 6 |`

**:724–728:** "**1 · Families 8 and 9 exist only in prose.** §2.1c names them — family 8 is `I(τ ; action) = 0`, family 9 is weights-versus-membership — and neither appears in `GainChain.lean`, in `ContractEmitter.lean`, or in `CommitmentTemperature.lean` itself, which never mentions a family number. The emitter reserves 3, 6 and 7 and" (note: `ContractEmitter.lean:198` now reserves 9 — see §4 above).

**:985–986:** "3. **R6 (family 9, weights versus membership)** and **R2** — designed in their excursions, unbuilt. Building them *after* the standard is what tests whether the standard was worth writing."

**:1664–1669:** "The four-element whitelist has been frozen since **2026-06-03** — three months as a defect — but it was recorded on **2026-08-27** (`E-R6`, `E-R8` slice 5), so its latency is one day. Family 9 in the rosetta is `*unstated — the R6 module's slot*`, and it is currently our **only** recorded-and-unstated incident."

**:1911 heading:** `### 3.2 Tier 1 — attestation and typed absence *(each stated as a contract clause first)*`

**:1920–1922 (Tier 1 #1 row):**
```
| 1 | the **composer** writes one attestation per *registered* proposer — ran?, input-ref, emitted-n — whether or not it emitted | "the space is empty" vs "the generator did not run" | `action_proposer/compose-proposers`; `proposer-id` is declared at `:31` and never called |
```
(rows 2–4 at `:1923–1925`: `read-curvature-signal` returns `fresh | stale(age) | absent` (`aif2/tension.clj:198`); grounded producer returns `:domain-mismatch`; "stop reporting τ as governing".)

**:1927–1931:** "**Why #1 first.** The 2026-07-15 archive shows 24 attempts, each recording *"no addressable entities"* for seven action classes, with no record of which proposers ran. With attestation the operator sees it at **attempt 2** instead of attempt 24. It is additive, it is cheap, and it is the instrument every later repair is judged with."

**:1939–1942:** "`aif-r1-r16-pattern-map.md` asks *is it built and load-bearing?*; `wr-overlay.edn` asks *does the discipline hold?* They use the same R-numbers and disagree on R6 and R8."

**:43:** lists `action_proposer.clj` among the source files in scope.

---

## Not-found register (commands run)

- `CandidateSpace.lean`: `ls /home/joe/code/mathlib4/DarkTower/WarMachine/CandidateSpace.lean` → No such file.
- `surveyedSpace` in Lean: `grep -n -i 'surveyedSpace' /home/joe/code/mathlib4/DarkTower/WarMachine/*.lean` → no hits.
- `candidate_space` / `constellation` namespace: `ls src/futon2/aif/ | grep -i 'candidate\|constellation'` and `find src scripts -iname '*candidate*' -o -iname '*constellation*'` → none.
- `:R6` key in `r18-badges.edn`: `grep -n ':R6' data/r18-badges.edn` → none.
- `abstain` in the glossary: `grep -n -i 'abstain' p4ng/sec-glossary.tex` → none.
- `attestation` in wm-trace: `grep -c 'attestation' wm-trace-2026-*.edn` → 0 in every file.
- `:admissible-actions` in trace records: 0 of 791 records; `grep -n 'admissible-actions' src/futon2/aif/trace.clj` → none.
- `:decision :action :abstain` in wm-trace: 0 of 791 records.
- Proposer-id stamps for `:bootstrap`, `:mission-enumerator`, `:sorry-enumerator`, `:portfolio-action` in wm-trace: none.
- `proposer` in any 07-15 archive `002-selection.edn`: none (`grep -rl 'proposer'` hits only `derived-projections/failed-wm-traces.edn`).
