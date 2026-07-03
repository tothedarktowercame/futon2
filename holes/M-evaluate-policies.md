# Mission: Evaluate policies honestly — make R5's score what it says it is

**Date:** 2026-07-03
**Status:** HEAD + IDENTIFY complete 2026-07-03 (operator gates passed in-session);
**MAP COMPLETE 2026-07-03 (§7.11) — all questions Q1–Q8 answered**; headline facts:
epistemic pole behaviourally absent (flip v2: survival 47.9 / structural-pressure 44.5 /
risk 35.9 / hidden 8.3 / info 0.0 artifact-corrected / ambiguity 0.0); core-only agreement
35.3% with 98.4% type-switches → learn-action-class (**overlay = prosthetic C**); F7 = 293
cascade rows, all wholeness-bearing, 0 ever ΔG-scored (`(or dG 0.0)` at
war_machine.clj:3797); dispositions §7.6. DERIVE §8 (D1–D6
flag-gated, IHTB-1..3, GF, I1–I5, PSR-1..3). **ARGUE §9 = the exhibit
(`holes/labs/M-evaluate-policies/exhibit/argue-exhibit.pdf`, 6pp, 3 real cascades ×
every valuation; 3 candidate meta-patterns §9.2) — pending Joe's read.** VERIFY next;
C10 confirmation + D5 dark-vs-default = Joe's
(chartered from the R18 faithfulness audit — Joe: "the items from R5 should
probably be put into the IDENTIFY stage of a new mission")
**Owner:** unassigned (framed by Joe + claude-11)
**Sources:** `holes/E-r18-faithfulness-audit.md` + `data/r18-badges.edn` (claude-2, 2026-07-03,
futon2 `dcbe021`) · `src/futon2/aif/efe.clj` (the code under discussion) ·
`docs/futon-aif-completeness.md` §R5/§R18 (v0.23) · ledger context
`holes/E-aif-post-mission-mining.md` #6.6 (this mission absorbs it).

## 0. HEAD — What do we talk about when we talk about EFE?

*(Added same-day as pre-IDENTIFY intake, per `futon4/holes/mission-lifecycle.md`. IDENTIFY
below was authored first; this section preserves the operator-shape that motivates it.)*

**Operator-voice anchor.** Joe, 2026-07-03, verbatim: *"What do we talk about when we talk
about EFE?"* — the Carver question, asked of a formalism instead of love, and with the same
force: the word is in constant confident use by parties who may not mean the same thing by it.
When `efe.clj` says `G-total`, the wiring diagrams say "Expected Free Energy," and the paper
says "EFE-scored action selection," at least three referents are in play: (i) the **canonical
quantity** from the active-inference literature (risk + ambiguity under a generative model, in
nats); (ii) the **design tradition** — the pragmatic/epistemic decomposition as a shape that
invites new terms; (iii) the **working blend** that actually ranks the WM's candidate actions
every hour. The mission exists because the one word is doing the work of all three, and only
(i) has a mathematical warrant to the name.

**The policy turn (operator emphasis, second intake pass, same day).** For a while the WM
computed an argmax over possible *next actions*, not policies — the mistake ratified as **R13
(policy adequacy)** in `docs/futon-aif-completeness.md` §R13: the degenerate single-step
selector passed R1–R12 while ranking a silly mission #1 (cursor-#1), and in operation, in
Joe's words, "the War Machine would get stuck and wouldn't be able to do anything except ask
me for help." Since then the workup has been to treat **policies as pattern cascades** in the
Alexander/Moran sense (`futon5a/essays/moran-1971-agent-cascades/reading-moran-1971.md`),
most recently via C-cascade-real — so we *should* now be able to create real pattern cascades
pretty much on demand. An exemplar of the desired look exists in a parallel project
(`~/julia-chat/relationship-analysis/pattern-cascade.dot` — 19 patterns, weighted cascade
edges, three colour-strata), and relative to the WM we have an embedding of patterns and
missions and have produced cascades in practice. So the mission's title question is asked *of
cascades*: what is the EFE **of a policy-as-cascade**, not of a next action — R13's lesson
carried into the evaluation layer rather than stopping at the selection mechanism.

**The diagrams keep their seats — but each must show its papers.** "EFE" also labels
meaningful, valued diagrams — e.g. `futon6/data/mission-efe-field-embed.html`. Operator
constraint, verbatim: "I don't want to lose the diagrams, but I do want to double check what
EFE *this* one is." Checked this session: the generator
(`futon6/scripts/mission_efe_field.py`) is internally honest — its header reads "g(s) =
per-step cost / local metric (epistemic pole). **NOT the EFE.** EFE = G(π) = the geodesic
over this; drawn later as policy streamlines." So the field diagram renders a **fourth
referent**: (iv) the per-step cost field g(s), the *integrand* whose path-quantity is sense
(i). Only the filename carries the analogical label — the same slippage §2 catalogues,
already caught by the artifact's own comments. The constraint going forward: keep every
diagram, and have every EFE-labelled surface declare which sense it shows.

**The wanted exhibit (tooling, possibly IDENTIFY-stage).** A PDF that exhibits some real
cascades (pictures like the `.dot` exemplar), shows their EFE scores (via whatever
computation), and shows what it means for one of them to be *selected*. Tuning and
understanding that relative to various definitions of EFE is in some sense the whole mission
— but it needs tooling, and that tooling may belong as early as IDENTIFY, so that the *gaps
between different definitions of EFE can be visually inspected* while the characterisation
data (§3.1) accumulates.

**The differentiable substrate (operator implementation intuition, third intake pass, same
day).** Joe, frankly-as-implementation-detail: THIS is where JAX could be useful. We have the
embedding of patterns and missions — and could add turns→patterns too, or whatever else we
wanted to chuck in the pot — giving a nice **differentiable field that can simultaneously be
read as a graph** (pattern→pattern and mission→pattern reference; optionally turn→pattern
tagging). So a diagram like `pattern-cascade.dot` should be adjustable to circumstances in a
very nice differentiable way: if we wanted "affirm" to be part of a big connected component,
look for more links; if weave→repair needed a further breakdown, refine that edge — all fast,
over the existing pattern/mission carpet. Two honest notes travel with this. (a) *HEAD
discipline:* this is an implementation intuition carried forward to DERIVE, not a commitment
— the field/graph dual read is the same terrain/path structure as g(s)/G(π), which is
encouraging, but "differentiable" is a property the current score does not yet deserve. (b)
*The tie to the mission's core:* gradient-adjusting cascades against the score means the
units problem stops being cosmetic and becomes a **conditioning problem** — incommensurate
hand-set weights distort the gradient field, and the 1000·[not-applicable] penalty is a
cliff, not a slope. The honesty work (§3.3, §3.5) is a *precondition* for the JAX move, not a
rival to it. Also note the selection-mechanism shift smuggled inside: canonical AIF softmaxes
over *enumerated* policies; differentiable adjustment is policy *optimization* — a different
(interesting) mechanism that would itself need the "which EFE?" question answered first.

**What's already felt to be true.** The loop works: it closed live 2026-07-02 and γ is
calibrating — whatever G-total is, selecting on it produces defensible behaviour, so this is
an honesty problem, not a wreckage problem. The EFE *shape* (sense ii) has been genuinely
generative: the eight terms exist because the decomposition invited them, and that fertility
is real even where the mathematics is analogical. And G-ambiguity already earned
`principled-approximation` — proof the canonical standard is reachable from this codebase, not
utopian.

**Anti-glibness discipline.** Two symmetric failure modes. (a) *Glib retention:* keep calling
everything EFE because Friston-compliance reads well — a straight "we do" violation. (b) *Glib
renunciation:* relabel the blend "multi-objective score," ship the rename, and skip the real
work of discovering what the blend optimizes — the euphemism-shaped exit. The discipline
against both is empirical: the flight log carries per-term, per-candidate, per-tick values, so
every claim about what the score *is* must be checked against what selection *does*. No term
keeps or loses the EFE label by argument alone.

**Working-economy position.** Downstream: the WM's hourly action choice ranks by this blend,
so its honesty bounds the honesty of everything the WM does; the paper's central sentence
("EFE-scored action selection") depends on it; and every new summand (G-goal-outcome arrived
2026-07-02) inherits the units problem at birth. Upstream: it is underwritten by the R18
audit (the eight R5 verdicts in §2 are the evidence base) and by the burn-in flight corpus
accumulating hourly.

**Clarity-gap / carried-forward tensions** (named, not settled — §3 picks them up):
1. *Referent:* which of the three senses is the label "EFE" allowed to claim — and does the
   apparatus earn sense (i) back, or does the text move to sense (iii)'s honest name?
2. *Blend semantics:* what G-total actually optimizes is written down nowhere; at current
   weights the effective selection may be 2–3-term.
3. *Units:* incommensurability is currently absorbed invisibly by hand-set weights.
4. *Mask-vs-value:* feasibility (the 1000·[not-applicable]) is smuggled into a value term.
5. *Core-vs-overlay:* whether selection on canonical G = risk + ambiguity alone is
   behaviourally acceptable.
6. *Unit-of-evaluation:* R13's lesson at the evaluation layer — the score should be of
   policies-as-cascades (semilattices), not next actions. What is G over a cascade, and does
   each of the eight terms even lift from action-grain to cascade-grain?
7. *Diagram referents + exhibit tooling:* every EFE-labelled diagram declares its sense (the
   futon6 field-embed = g(s), the integrand — verified 2026-07-03); the cascade-exhibit PDF
   (§3.6) is the instrument that would make the definitional gaps visible.
8. *Differentiable adjustment (JAX):* cascades tuned to circumstances by gradient over the
   pattern/mission embedding field — carried to DERIVE as candidate implementation, gated on
   the units/mask repairs (a score with hand-set incommensurate weights and 1000·cliffs is
   not yet a gradient-worthy objective), and naming a selection-mechanism question of its own
   (enumerate-and-softmax vs optimize).

**Provenance.** Live operator prompt, Joe → Fable (claude-code session), 2026-07-03: "As a
simple HEAD — What do we talk about when we talk about EFE?" — issued against a mission file
already chartered at IDENTIFY (Joe + claude-11, same day, from the R18 audit). Second intake
pass same session: operator emphasis on the R13 argmax-over-actions history, policies as
Alexander/Moran pattern cascades (C-cascade-real), the cascade-exhibit want, and the
futon6-diagram referent check (references verified against `docs/futon-aif-completeness.md`
§R13, the Moran reading, the `.dot` exemplar, and the field script's header). Third intake
pass same session: the differentiable-substrate / JAX implementation intuition (tension 8).
HEAD exit (operator recognises this as faithful) is Joe's call.

## 1. The tension (IDENTIFY)

The War Machine selects its actions hourly by softmaxing `G-total`, which the code and the
diagrams call **Expected Free Energy**. The R18 audit examined all eight terms of that sum
against the canonical literature and found: **one principled approximation, seven analogicals,
and a total that is not EFE** — it is a multi-objective score with an EFE-shaped core, in
incommensurate units, at hand-set weights. The loop *works* (it closed live 2026-07-02; γ is
calibrating), but the evaluation layer's self-description is the least honest part of a system
whose other layers just earned their labels.

This matters now for three live reasons:
- **Selection ranks by the blend every tick.** Whatever the blend actually optimizes is what the
  WM actually pursues. Nobody has written down what that IS, in one place, in honest units.
- **The paper** (`p4ng/sequel-notebook.org`) says "EFE-scored action selection." After the audit,
  the honest sentence is "multi-objective score with an EFE core" — unless this mission earns the
  original sentence back.
- **The units problem compounds.** Every new term (G-goal-outcome arrived 2026-07-02) is added
  linearly into a sum whose terms already can't be compared; hand-set weights absorb the damage
  invisibly.

## 2. The evidence — the eight R5 verdicts (verbatim from the audit)

| term | badge | claims to be | actually computes | repair |
|---|---|---|---|---|
| G-total | analogical | Expected Free Energy | linear sum of 8 terms in incommensurate units at hand-set weights; risk+ambiguity core diluted | expose canonical G = risk+ambiguity separately; label the blend a multi-objective score, not EFE |
| G-risk | analogical | EFE risk = D_KL[Q(o‖π) ‖ C] | weighted L1 hinge-distance of predicted point-estimate from preference ranges; no distribution/log/KL | predict outcome distribution Q(o‖π) and KL against a normalised preference C |
| G-ambiguity | principled-approximation | EFE ambiguity E_Q(s‖π)[H[P(o‖s)]] | sum of per-channel predicted variance (monotone Gaussian-entropy proxy) | use ½ln(2πe σ²) under Q(s‖π) → derived-from-FEP |
| G-info | analogical | epistemic info-gain (EIG) | Σ max(0,1−variance) = complement of ambiguity (same next-var); no belief-entropy reduction | compute real EIG over beliefs (as the futon3c portfolio sibling does) |
| G-survival | analogical | survival-pressure EFE term | unweighted Σ hinge-gap over 4 critical channels — a second G-risk on a subset | fold into risk, or rename 'homeostatic pressure' and drop the EFE framing |
| G-structural-pressure | analogical | weighted EFE term | pass-through of a caller-injected scalar × 0.35; not a computed quantity | derive from the generative model, or rename as an exogenous weight |
| G-graph-pragmatic | analogical | pragmatic value | 1000·[not-applicable] + 3·holes − 20·ascent-progress (graph heuristic counts/gates) | split: applicability = feasibility mask; ascent = pragmatic proxy toward C |
| G-gap | analogical | epistemic 'room to grow' | 6.0 × caller-supplied gap-score lookup; no expected-information computation | compute expected uncertainty-reduction from filling the gap |

(`G-goal-outcome` is homed on R19/the belly, not here — its repair is the named W1, tracked with
the PROOF join. It is however the ninth summand of the same blend, so §3's structural questions
include it.)

## 3. What the mission is FOR (the questions, not yet the answers)

Per the method, IDENTIFY names the territory without committing to a construction:

1. **What does the blend actually optimize?** Characterise the current G-total empirically (the
   flight log + traces give per-term values per candidate per tick — real data, accumulating
   hourly) before changing anything. Which terms ever flip a decision? At current weights, is the
   selection effectively 2-term? 3-term? (If six terms never matter, the honest repair may be
   deletion, not derivation.)
2. **Core vs overlay.** The audit's minimal repair: expose canonical `G = risk + ambiguity`
   separately from the heuristic overlay, so "EFE" names only the part that is one. Does the WM
   behave acceptably selecting on the core + a feasibility mask alone?
3. **Commensurability.** If heuristic terms stay, what puts them in common units — normalisation
   per term? everything as (approximate) bits/nats? weights learned rather than hand-set (R12's
   Beta-credit machinery is adjacent)?
4. **Term-by-term dispositions.** Each row's repair is a candidate; some repairs are one-liners
   (G-ambiguity's entropy formula), some are merges (G-survival into risk), some are renames
   (G-structural-pressure as an exogenous weight), some are real builds (G-risk's distributional
   KL — which shares machinery with the belly's W1).
5. **The mask question.** G-graph-pragmatic's 1000·[not-applicable] is a feasibility HARD
   constraint smuggled into a value term. Should infeasibility be a mask before scoring, not a
   penalty inside it?
6. **The exhibit question (unit-of-evaluation + tooling; HEAD tensions 6–7).** The R13 lesson
   says the thing being scored is a *policy-as-cascade*, not a next action — so the
   characterisation in §3.1 should be inspectable at cascade grain. Candidate tooling, possibly
   built during IDENTIFY: render real cascades (dot-style, like
   `~/julia-chat/relationship-analysis/pattern-cascade.dot`), score each under multiple EFE
   definitions side by side (current 8-term blend; canonical core risk+ambiguity; per-term
   contributions), and exhibit what selection of one of them means — a PDF/HTML where the *gap
   between definitions is visible per cascade*. Sub-question: do the eight terms even lift from
   action-grain to cascade-grain, or only some? (The futon6 field-embed shows the answer's
   shape: g(s) is per-node terrain, G(π) is the path-quantity — the exhibit is where the two
   stop being conflated visually.) The differentiable-substrate intuition (HEAD tension 8)
   names the candidate engine for the exhibit's "adjust cascade to circumstances" dimension:
   gradient over the pattern/mission embedding field (JAX) — but as a DERIVE-stage choice,
   gated on §3.3/§3.5 making the score gradient-worthy.

## 4. Boundaries

- **γ is out of scope and already safe**: its calibration pair is coverage-ΔG vs coverage-ΔG
  (claude-10 review fix, 2026-07-02) — the blend never enters γ. Nothing here touches R14.
- **The belly's W1** (real predictive-KL for G-goal-outcome) stays with R19/E-C-vector-live §11.
- **Reduction safety is a hard constraint**: the WM runs hourly in production; every change lands
  behind the established discipline (behaviour-identical defaults, sim-first, flight-log
  before/after comparison — the same shape as R14's burn-in evidence).

## 5. Exit criteria for IDENTIFY → MAP

*(Re-homed 2026-07-03, operator-approved per §6.9: items 1–3 below are now MAP survey
questions Q1–Q3 (§7.1); item 5's exhibit build is Q7; item 4 remains the mission-level
decision C6. IDENTIFY exited on the lifecycle's own criterion — operator agreed gap real,
scope right. Original list kept for the record:)*

1. The empirical characterisation (§3.1) exists: per-term decision-influence measured over ≥1 week
   of flights (the burn-in corpus is accumulating now).
2. The core/overlay split (§3.2) is ARGUE-able with data: what selection-on-core-only would have
   chosen differently, tick by tick.
3. Each of the 8 terms has a proposed disposition (repair / merge / rename / delete) with its cost.
4. The paper's sentence is decided: either the apparatus earns "EFE-scored" or the text says
   "multi-objective score with an EFE core" (we-do discipline — the label follows the code, not
   the aspiration).
5. The exhibit question (§3.6) is resolved: either a first cascade exhibit exists (a few real
   cascades rendered with per-definition EFE scores and the selection consequence shown), or
   its build is explicitly scheduled into MAP with §3.1's characterisation not blocked on it.
   Either way, the EFE-labelled diagrams in the mission's orbit each have a declared sense
   (the futon6 field-embed's is settled: g(s), per its generator header).

## 6. IDENTIFY — lifecycle-checklist completion (2026-07-03, Joe + Fable session)

§§1–5 above were the first IDENTIFY pass (Joe + claude-11). This section completes the
`futon4/holes/mission-lifecycle.md` IDENTIFY checklist, accreting rather than rewriting.

### 6.1 Checklist coverage map

| checklist item | satisfied where |
|---|---|
| Motivation | §1 (the tension) + §0 HEAD (three intake passes) |
| Theoretical anchoring | §6.2 (new) |
| Scope in/out | §4 + §6.4 (consolidated, updated for HEAD passes 2–3) |
| Completion criteria | §6.5 (new — mission-level; §5 is the phase-exit list, see §6.9) |
| Relationship to other missions | §6.6 (new) |
| Source material | header Sources + §6.7 (consolidated) |
| Owner and dependencies | §6.8 (new; owner assignment stays with Joe) |
| Shape-first (optional) | §6.3 (taken — 5 sibling instances found) |

### 6.2 Theoretical anchoring

- **Canonical AIF/EFE** (Friston / Parr / Da Costa / Pezzulo), as mediated by
  `docs/futon-aif-completeness.md`: R18 (faithfulness) is the parent criterion, R5 the locus;
  R13 (policy adequacy) fixes the unit of evaluation at policy grain.
- **Faithfulness-badge epistemology** from the R18 audit (`principled-approximation` /
  `analogical` / etc.): a label is earned by what the code computes, not by what it gestures
  at — the "we do" discipline applied to mathematical vocabulary.
- **Alexander/Moran pattern cascades**
  (`futon5a/essays/moran-1971-agent-cascades/reading-moran-1971.md`; C-cascade-real): policies
  are cascades/semilattices over the pattern-and-mission graph, producible on demand.
- **Combining-methods-as-diagnostic** (`futon3/library/`): scoring the same cascade under
  multiple EFE definitions and *reading the disagreement* is that pattern instantiated at the
  evaluation layer — the exhibit (§3.6) is its instrument.
- **Projections-of-a-latent-metric** (M-efe-bge-followon-actions, futon6): carpet springs, BGE
  embedding, and EFE terrain are competing projections of a mission-metric we don't yet know
  how to define; the referent question (§0, senses i–iv) is the same frame applied to the
  word "EFE" itself.

### 6.3 Shape-first reading (optional IDENTIFY move — taken)

The gap is one instance of a shape: **an EFE-labelled surface must declare and earn its
sense** (i canonical quantity / ii design tradition / iii working blend / iv per-step field).
Sibling instances under `efe-referent-declaration/`:

| instance | surface | sense today |
|---|---|---|
| `wm-blend` | `efe.clj` G-total (this mission's core object) | (iii), aspiring to expose an (i)-core |
| `rollout` | `futon2.aif.rollout` G(π) path accumulator (R13) | (i)-shaped at policy grain, over (iii)'s g |
| `portfolio-surface` | `futon3c.portfolio.policy/effect` (real EIG) | the honest sibling — repair exemplar for G-info |
| `field-diagrams` | futon6 `mission-efe-field*` | (iv), declared in generator header; filename lags |
| `paper-sentence` | `p4ng/sequel-notebook.org` "EFE-scored action selection" | currently unearned — §5.4 decides |

Five instances; not `:special-case`. The invariant candidate is checkable: grep EFE-labelled
surfaces, ask each for its declared sense, badge the declaration against the computation.

### 6.4 Scope (consolidated; extends §4)

**In:** the 8 (+ G-goal-outcome as 9th structural) summands; empirical characterisation
(§3.1); core/overlay split (§3.2); commensurability (§3.3); per-term dispositions (§3.4);
feasibility mask (§3.5); cascade-grain lift of the terms + exhibit tooling (§3.6); referent
declarations across the five §6.3 instances; the paper's sentence.

**Out / deferred:** γ / R14 (§4 — already safe); the belly's W1 (§4 — R19, coordinate on
shared KL machinery, don't duplicate); **JAX differentiable adjustment** (HEAD tension 8 —
DERIVE-stage candidate, gated on §3.3/§3.5); **turns→patterns embedding enlargement** (HEAD
pass 3 "chuck in the pot" — optional, not required by any completion criterion); futon6
layout-family questions (owned by M-efe-bge-followon-actions); acting/arming (WM-I4 consent
gate untouched).

### 6.5 Mission-level completion criteria (testable)

- **C1 — Decision-influence report:** per-term flip-rate/influence table over ≥1 week of
  flights; script committed and rerunnable.
- **C2 — Core exposed:** code + trace emit canonical `G-core = risk + ambiguity` separately
  from the overlay; visible in the flight log.
- **C3 — Dispositions executed:** each of the 9 summands has a landed repair / merge / rename
  / delete (behaviour-identical defaults, sim-first, before/after flight comparison) or an
  explicit deferral ticket.
- **C4 — Commensurability decided:** a mechanism (normalisation / nats / learned weights) is
  implemented, or declined with an IF/HOWEVER/THEN/BECAUSE record in DERIVE.
- **C5 — Feasibility is a mask:** infeasibility applied before scoring, not as 1000·penalty
  (or recorded contrary decision).
- **C6 — Paper sentence matches apparatus** (either direction — §5.4).
- **C7 — Cascade exhibit shipped:** ≥3 real cascades rendered, each scored under ≥2 EFE
  definitions (blend + canonical core at minimum), with the selection consequence shown.
- **C8 — Referent declarations:** all five §6.3 instances carry a declared, badge-checked
  sense; the futon6 filename/label reconciled or annotated.
- **C9 — Reduction safety evidenced:** every landed change has its before/after flight-log
  comparison archived (no un-flagged behaviour regression).
- **C10 — (candidate, Joe 2026-07-03) M-G-over-cascades closed:** that mission defined the
  cascade/G-definition territory this one evaluates against; Joe flagged its closure as a
  probable exit criterion here ("we may want to close that mission as an exit criterion for
  this one too"). Mission-close remains Joe's call; recorded as candidate pending his
  confirmation at DERIVE.

### 6.6 Relationship to other missions

- **Chartered by:** `E-r18-faithfulness-audit` (its R5 verdicts are §2 verbatim).
- **Absorbs:** `E-aif-post-mission-mining` #6.6.
- **Bounded by:** R14 γ calibration (running, out of scope); R19/W1 in `E-C-vector-live` §11
  — the G-risk distributional-KL repair shares machinery with W1; coordinate, don't duplicate.
- **Fed by:** C-cascade-real (cascades on demand — the exhibit's subjects); M-wm-policies +
  R13 (the policy-grain lesson); M-efe-bge-followon-actions (BGE co-embedding, 1302
  mission+pattern vectors, `futon3a/resources/notions/bge_*_embeddings.json`; the
  projection-family frame).
- **Feeds:** the p4ng paper sentence; any future differentiable-policy DERIVE (HEAD tension
  8); the unit discipline every future summand inherits at birth.
- **Sibling apparatus:** futon3c portfolio EFE surface — the standing exemplar that honest
  terms (real EIG) are buildable in this stack.
- **Adjacent-in-flight (found at MAP-open, F3):** `M-G-over-cascades` (exploratory DERIVE)
  owns the *definition* of cascades and of G over them; this mission owns term-honesty and
  the multi-definition exhibit. Coordinate at the boundary; neither duplicates the other.

### 6.7 Source material (consolidated)

Header Sources, plus (added by HEAD passes 2–3 and this section): `futon2.aif.rollout`; the
flight-log/trace corpus (accumulating hourly); `docs/futon-aif-completeness.md`
§R13/§R18/§R19; `futon5a/essays/moran-1971-agent-cascades/reading-moran-1971.md`;
`~/julia-chat/relationship-analysis/pattern-cascade.dot` (exemplar);
`futon6/scripts/mission_efe_field.py` + `futon6/data/mission-efe-field-embed.html`;
`futon3a/resources/notions/bge_{mission,pattern}_embeddings.json`;
`futon3c.portfolio.policy`/`effect`.

### 6.8 Owner and dependencies

Repos: **futon2** (primary — `efe.clj`, rollout, traces), **futon6** (exhibit + diagrams),
**futon3a** (embeddings), **futon3c** (portfolio sibling, flight infrastructure), **futon5a**
(theory essay), **p4ng** (paper). Owner: still unassigned — assignment is Joe's call at the
IDENTIFY gate; the natural handoff point is MAP, whose first work item (§3.1
characterisation) is self-contained given flight-log access.

### 6.9 Note on §5 (phase-exit vs phase-shape)

§5's items 1–2 (empirical characterisation, core-vs-overlay data) are *research products* —
under the lifecycle they are MAP work (MAP "produces facts, not decisions"). Proposal, for
decision at the gate: re-home §5.1–5.3 as MAP survey questions Q1–Q3, and let IDENTIFY exit
on the lifecycle's own criterion — a human agrees the gap is real and the scope is right.
This keeps the gate crossable now instead of blocking it on a week of corpus.

**Gate record (2026-07-03):** Joe approved the re-homing and the phase transition in-session
("Yes we can rehome the misplaced items into MAP and move into that phase next"). IDENTIFY
exited; MAP opened (§7).

## 7. MAP (opened 2026-07-03)

MAP is research: facts, not decisions. Survey questions below; findings accrete as they
close. First reconnaissance done in the opening session (Joe + Fable).

### 7.1 Survey questions

- **Q1** *(was §5.1)*: Per-term decision-influence over the trace corpus — which terms ever
  flip a decision at current weights? Is effective selection 2-term? 3-term? Prerequisite
  discovered by F2: first establish which terms are even **live in production**.
- **Q2** *(was §5.2)*: Core-only counterfactual — what would selection on
  `G-core = risk + ambiguity` (+ feasibility mask) have chosen differently, tick by tick?
- **Q3** *(was §5.3)*: Per-term disposition inventory — for each of the 9 summands, the
  candidate repair/merge/rename/delete from §2, with cost estimate.
- **Q4**: What exactly does the trace corpus carry — schema, per-candidate coverage,
  term-composition over time? (The blend changed mid-corpus: G-goal-outcome from 2026-07-02.)
- **Q5**: Cascade supply for the exhibit — what do C-cascade-real + the BGE co-embedding
  (1302 mission+pattern vectors) provide today; what renders a `.dot` from a real cascade?
- **Q6**: What does the honest sibling (futon3c portfolio real-EIG) actually compute, in
  enough detail to serve as the template for the G-info repair?
- **Q7** *(was §5.5's build)*: What tooling exists toward the exhibit
  (`mission_efe_field.py`, dot pipelines, PDF assembly) — and what is the minimal exhibit:
  ≥3 real cascades × ≥2 EFE definitions × selection consequence?
- **Q8**: Where do the weights live at runtime, and what is the sim-first / reduction-safety
  harness for changing anything in the blend?

### 7.2 Findings (F-numbered; opening reconnaissance 2026-07-03)

- **F1 — The corpus is already sufficient for Q1/Q2.** `futon2/data/wm-trace/` holds **42
  daily trace files (2026-05-18 → 2026-07-03)**, ~34 ticks/day, ~108 candidates/tick, with
  **per-candidate per-term G values persisted** (3,690 per-term occurrences per day-file
  checked). §3.1's "≥1 week of flights" is already satisfied — no need to wait for burn-in.
  Caveat: blend composition changed over the window (G-goal-outcome appears only from
  2026-07-02; 1,620 vs 3,690 occurrences in its first day-file).
- **F2 — SURPRISE: two audited terms are absent from the persisted traces.** `:G-gap` and
  `:G-graph-pragmatic` occur **zero** times in both the earliest (2026-05-18) and latest
  (2026-07-02) day-files. Either they don't run in the production lane or they don't
  persist. The R5 table audits the *code*; the trace audits the *deployment*; they differ.
  Q1 must open with a term-liveness census. (If they are simply dead in production, the
  "honest repair may be deletion" hypothesis from §3.1 gains two immediate candidates.)
- **F3 — SURPRISE (scope): `M-G-over-cascades` already owns HEAD tension 6's territory.**
  `futon2/holes/M-G-over-cascades.md` (2026-06-22; HEAD+IDENTIFY+MAP done, exploratory
  DERIVE; Joe + claude-2, claude-1 on theory; continuation of M-wm-policies) is defining
  cascades and G over them — "G over cascades needs cascades to be *defined*." Boundary
  proposal: **that mission defines the policy object and its G; this mission makes the
  terms honest and builds the multi-definition exhibit.** Cross-feed both ways: its probes
  want honest terms; our exhibit wants its cascade definition. (§6.6 updated.)
- **F4 — Weights confirmed as hand-set defaults**, `efe.clj:50–57`: info 0.4, survival 1.2,
  structural-pressure 0.35, graph-body 3.0, graph-ascent 20.0, gap 6.0; G-total assembly
  documented at `efe.clj:323–325`.
- **F5 — Two distinct artifact families, don't conflate:** the per-tick corpus for Q1/Q2 is
  `futon2/data/wm-trace/` (daily EDN); the richer *flight-as-derivation* records
  (`futon3c.aif.flight-record`, spec v0.4, 5 artifacts from the first-flights era) are a
  different lane — and notably that spec already types the grain question
  (`:g-grain :one-step-action` as an honest ghost), i.e. grain-honesty is first-class there.

### 7.3 Ready vs missing (updates as questions close)

| ready (no new code) | missing (the actual work) |
|---|---|
| 674-tick per-candidate per-term trace corpus | ~~term-liveness census + flip-rate script~~ **done — §7.4** |
| `wm_trace_census.bb` + `q1-census.edn` (Q1 artifacts) | core-only counterfactual detail (Q2 — agreement number exists, tick-by-tick diff missing) |
| `efe.clj` term functions + weight defaults | whitelist fix for `:G-gap`/`:G-graph-pragmatic` persistence (observability repair) |
| rollout G(π) apparatus (R13) | disposition cost table (Q3) |
| BGE co-embedding, 1302 vectors | exhibit assembler: cascade → multi-definition scores → PDF (Q7) |
| `.dot` exemplar + graphviz; `mission_efe_field.py` | coordination note with M-G-over-cascades (F3/C10) |
| R5 verdicts (`r18-badges.edn`); portfolio EIG sibling code | Q4 schema/composition audit; Q8 harness inventory |

### 7.4 Q1 ANSWERED (2026-07-03) — term-liveness census + flip-rates

**Apparatus:** `scripts/wm_trace_census.bb` (committed, rerunnable); report artifact
`holes/labs/M-evaluate-policies/q1-census.edn`. Reads every EDN form per day-file (one form
per tick), reconstructs G-total per candidate from the persisted terms at the default
weights (no runtime overrides found in `wm_scheduled_run.clj`/`wm_outer_loop.clj`), and asks
per term: does removing its weighted contribution change the argmin winner?

**Corpus (corrects F1's arithmetic):** 41 day-files (05-18 → 07-03), **674 ticks**, mean
133.6 candidates/tick. Winners: address-sorry 439 · open-mission 193 · learn-action-class 42.

**F2 RESOLVED — stripped, not dead.** `trace.clj strip-ranked-action` (line 71) whitelists
persisted terms; `:G-gap` and `:G-graph-pragmatic` are computed in production but dropped at
persist time (the in-code comment records G-goal-outcome having had exactly this defect
until R19). Their combined contribution is recoverable as the reconstruction residual:
mean |residual| 0.32, **max 72.0** — and removing it flips **8.3%** of decisions. So the
blend is partly steered by terms no flight can show. Repair candidate: one-line whitelist
addition (the R19 precedent), before any semantic change.

**Flip-rates (percentage of 674 ticks where removing the term changes the winner):**

| term | persisted | flip-rate |
|---|---|---|
| G-survival | 98.1% | **47.9%** |
| G-structural-pressure | 78.0% | **44.5%** |
| G-risk | 100% | **35.9%** |
| G-info | 98.1% | **21.8%** |
| hidden (graph-pragmatic − gap) | 0% | **8.3%** |
| G-ambiguity | 100% | **0.0%** |
| G-goal-outcome | 4.0% (arrived 07-02) | 0.0% |

**Readings:**
1. **Selection is genuinely multi-term** — four terms flip ≥20% of ticks. The "effectively
   2-term?" hypothesis from §3.1 is refuted; the honest-repair-by-deletion route does not
   get cheap wins here (except possibly goal-outcome, too young to judge).
2. **G-ambiguity never flips a decision (0.0% over 674 ticks).** One of the only two
   canonical-core terms is decision-irrelevant at current magnitudes (~0.015 vs G-info
   ~14) — the units/commensurability problem (§3.3) is now *measured*, not asserted: the
   only term with a `principled-approximation` badge is numerically inaudible.
3. **Core-only agreement = 35.3%.** Selecting on canonical G = risk + ambiguity alone would
   have chosen differently in ~2/3 of ticks — §3.2's core/overlay question now has its
   number, and "expose the core separately" is a *reporting* repair, not a behaviour-
   preserving swap.
4. The two dominant flip-terms (survival 47.9%, structural-pressure 44.5%) are both
   `analogical`-badged — the blend's behaviour is dominated by its least-earned terms.

**Caveats:** flip analysis is at remove-whole-term grain (not marginal-weight
perturbation); residual conflates graph-pragmatic with gap (only the whitelist fix
separates them); blend composition changed mid-corpus (structural-pressure absent in early
files, goal-outcome only from 07-02); default weights assumed (verified for the two
runners, not for every historical invocation).

**CORRECTION (same day, census v2 — `q1-census-v2.edn`).** The Q2 probe (§7.5) exposed a
lane artifact in the first run: `:apply-cascade` candidates are persisted as
`{:G-total 0.0, :rank, :action}` — **no term decomposition at all** (placeholder rows from
the cascade lane, present in 147/674 ticks = exactly 21.8%). Removing G-info's large
uniform bonus (−0.4 × ~14) lifted every ordinary candidate above 0.0, "flipping" the win
to a scoreless placeholder — so **G-info's 21.8% flip-rate was 100% artifact**. Lane-aware
re-run (cascade rows censused separately, excluded from flips):

| term | flip-rate v2 |
|---|---|
| G-survival | **47.9%** |
| G-structural-pressure | **44.5%** |
| G-risk | **35.9%** |
| hidden (graph-pragmatic − gap) | **8.3%** |
| G-info | **0.0%** (was 21.8%, artifact) |
| G-ambiguity | **0.0%** |
| G-goal-outcome | 0.0% (young) |

Reading 2 sharpens into the headline MAP fact: **both epistemic-pole terms have zero
decision influence over 674 ticks.** The WM's live selection is entirely
pragmatic/homeostatic (risk + survival + structural-pressure + the hidden graph/gap
residual). The blend's "epistemic value" is, behaviourally, decoration at current
magnitudes — the EFE claim's epistemic half is absent in deployment. (Within-tick spreads
confirm why: G-ambiguity values span ~0.03 at weight 1; G-info spans ~0.03 at weight 0.4,
affinely coupled to ambiguity as ≈ N − Σvariance over the same variance map.)

### 7.5 Q2 ANSWERED (2026-07-03) — tick-by-tick core-only counterfactual

**Apparatus:** `scripts/wm_core_counterfactual.bb` (committed);
`holes/labs/M-evaluate-policies/q2-core-counterfactual.edn` (summary + all 674 tick
records). Core = `G-risk + G-ambiguity`, no feasibility mask — the mask half of §3.2 is
not computable from traces until the whitelist repair lands (graph-pragmatic is stripped).

**Numbers:** agreement 35.3% (436/674 disagree). Of the disagreements, **98.4% switch
action TYPE**, and the flow is one-way traffic:

| blend winner → core winner | ticks |
|---|---|
| address-sorry → learn-action-class | 257 |
| open-mission → learn-action-class | 114 |
| address-sorry → open-mission | 58 |
| open-mission → open-mission (target switch) | 7 |

Under the live blend the WM picked learn-action-class 42/674 times (6%); under core-only
it would pick it **413/674 (61%)**. Rank geometry: the blend's winner always sits within
the core's top 6 (median 4) of ~133 candidates; the core's winner is usually the blend's
#2 (median; max 123). So core and blend agree about the *neighbourhood* and disagree about
the *summit*: the core surface is nearly flat at the top (spreads ~0.03), and the overlay
terms break the near-ties.

**Reading (the mission's deepest MAP fact so far):** the overlay is what makes the WM do
world-work. Strip it, and selection drifts to introspective learn-action-class actions —
exactly the "WM starved of input sources" attractor M-war-machine-first-outing found.
In canonical EFE the pull toward world-work is supposed to come from *risk under strong
preferences C*; here it is carried almost entirely by the analogical overlay
(survival + structural-pressure + hidden graph/gap). I.e. **the overlay is a prosthetic
C** — which both explains why the seven analogical terms exist and names the principled
repair route: strengthen preferences/risk (the belly's W1 direction, R19) until the core
can carry the pragmatic load the overlay currently smuggles.

**F7 — the cascade lane is in the arena but unscored.** 147/674 ticks include
`:apply-cascade` candidates; every one is a placeholder (`:G-total 0.0`, no
decomposition); **none has ever won**. For a mission whose HEAD asks the question *of
cascades*, this is the sharpest gap found in MAP: G over cascades is literally not
computed in the production arena — the policy-grain candidates sit in the ranking with a
constant. Direct input to M-G-over-cascades (C10) and HEAD tension 6.

### 7.6 Q3 ANSWERED (2026-07-03) — per-term disposition inventory (proposals for DERIVE)

MAP produces facts; these dispositions are *candidates informed by Q1-v2/Q2 data*,
ratified (or revised) in DERIVE. Costs: S = small/mechanical, M = bounded build,
L = real build.

| term | badge | flip v2 | proposed disposition | cost |
|---|---|---|---|---|
| G-total | analogical | — | split: expose `G-core = risk + ambiguity` alongside; label the blend a multi-objective score unless/until earned (C2/C6) | S (reporting) |
| G-risk | analogical | 35.9% | repair: predict outcome distribution Q(o‖π), KL against normalised C — **coordinate with belly W1** (shared machinery; don't duplicate) | L |
| G-ambiguity | principled-approx | 0.0% | keep; formula upgrade ½ln(2πe σ²) is a one-liner — but *audibility* requires the §3.3 units decision, else it stays decoration | S formula / M audibility |
| G-info | analogical | 0.0% (artifact-corrected) | replace with real EIG over beliefs (futon3c portfolio sibling = template), or delete pending that build — current term is affinely redundant with ambiguity | M |
| G-survival | analogical | **47.9%** | merge into risk as named homeostatic pressure; drop EFE framing. Dominant flip term ⇒ any change is sim-gated (C9) | M |
| G-structural-pressure | analogical | **44.5%** | rename: exogenous weight, not a computed EFE term (caller-injected scalar × 0.35); deriving it from the generative model is a separate, optional L | S rename |
| G-graph-pragmatic | analogical | in hidden 8.3% | **persist first** (whitelist, R19 precedent); then split feasibility mask (pre-scoring) from ascent-as-pragmatic-proxy (§3.5) | S persist / M split |
| G-gap | analogical | in hidden 8.3% | persist first; then real expected-uncertainty-reduction or fold into pragmatic; gap-weight 6.0 saturation already flagged by E-possible-world-regulator | S persist / M repair |
| G-goal-outcome | (R19/belly) | 0.0% (young) | out of scope here (W1); keep persisted; re-census once it has corpus | n/a |
| *(cascade rows)* | — | never win | either score cascades for real (that IS M-G-over-cascades / C10) or remove placeholder rows from the argmin arena; the current 0.0 constant is neither | S exclude / C10 score |

**Order-of-operations proposal for DERIVE:** (1) observability repairs — whitelist
persist for gap/graph-pragmatic + decide cascade-row handling (all S, behaviour-safe);
(2) reporting split G-core vs overlay (C2); (3) the commensurability decision (§3.3/C4) —
prerequisite for ambiguity/info audibility *and* for HEAD tension 8's gradient ambitions;
(4) semantic repairs in influence order: survival merge, risk KL (with W1),
structural-pressure rename; (5) cascade scoring via M-G-over-cascades (C10).

**MAP state after Q1–Q3:** Q1 ✓ (v2), Q2 ✓, Q3 ✓ (proposals). Q4 largely subsumed (schema
+ per-tick form structure + lane split established en route; composition drift documented).
Open: Q5 (cascade supply for the exhibit), Q6 (portfolio EIG template detail), Q7 (exhibit
tooling), Q8 (sim-first harness inventory).

### 7.7 Q5 ANSWERED (2026-07-03) — cascade supply for the exhibit

**The supply already exists, inside the trace corpus.** The production path is
`futon2.report.cascade-lane` (live in the WM since 2026-06-09; sim-only, read-only,
budget-truncated at 6, 30s hard timeout) shelling to
`futon3a/holes/labs/M-memes-arrows/cascade_serve.py` → `cascade_construct.py`
(phylogeny-greedy ordering + coverage-saturation stop, grounded in the descent/co-app
phylogeny since 2026-06-10; MiniLM relevance — note: *not* the BGE co-embedding, which is
a separate artifact). Each constructed cascade returns the ordered pattern list, Alexander
wholeness (T·H), and `F-free-energy = accuracy − λ·complexity`.

**Corpus census: 293 `:apply-cascade` rows** across the 147 ticks, **every one carrying
`:cascade {:shown [pattern-ids] :wholeness}`** — real cascades over real missions, already
persisted. And **0/293 have a non-zero `:G-total`**: `war_machine.clj:3797` sets
`(or dG 0.0)` where dG = rollout ΔG — designed to carry both act-gate legs (ΔF cascade-side,
ΔG rollout-side), but dG has been nil in every one of 293 instances. **F7 sharpened: the
cascade lane has its own value functional (wholeness/F) which never reaches the arena, and
the arena leg (ΔG) never fires — two currencies, glued by a constant.** The ΔG-wiring
failure is a concrete defect report for M-G-over-cascades (C10).

**Missing for the exhibit:** only a cascade→dot emitter (the julia-chat exemplar's look;
graphviz precedents exist: `futon6/scripts/render-golden-graph.bb`,
`sprint-review-wiring-to-graphviz.py`). Cost S.

### 7.8 Q6 ANSWERED (2026-07-03) — the portfolio real-EIG template

`futon3c.portfolio.policy`: G = −(pragmatic + λ_epis·epistemic + upvote) + effort, with
λ_epis = 0.4. The *heuristic* `epistemic-value` is per-action-type — but the **real EIG**
enters as `:epistemic-override`: `posterior-entropy` (normalized Shannon entropy of the
action-posterior P(a) ∝ exp(−G/τ)) is passed for `:acquire-patterns` (M-wm-policies
Track 3). **Template for the G-info repair:** the epistemic term = measured entropy of a
real posterior, not a variance complement; the override mechanism shows how to introduce
it per-action-class without rewriting the whole scorer. Transplant shape for futon2: an
entropy over `futon2.aif.belief` posteriors + expected reduction per action; cost M
(confirms §7.6's estimate). Caveat: action-posterior entropy is itself a proxy for
belief-state EIG — honest, but worth its own badge when transplanted.

### 7.9 Q7 ANSWERED (2026-07-03) — exhibit tooling inventory

**Ready:** 293 persisted cascades with pattern lists + wholeness (§7.7); per-tick blend
G-totals and core G for every ordinary candidate (censused by the two Q-scripts);
graphviz + two dot-emitting precedents; PDF assembly precedents
(`futon6/scripts/render_anatomy_pdf.py` — use the TeXLive 2025 engine, verify the .pdf
mtime not the "OK"; pandoc→xelatex route via the Arxana essays exporter). The dual
`g(s)`-vs-`G(π)` framing for the exhibit's prose is already drawn in
`mission_efe_field.py`'s header.

**Missing (= the Q7 build list):** (a) cascade→dot emitter from a trace row's
`:shown` list (S); (b) multi-definition score panel per cascade — wholeness/F (native,
present), rollout ΔG (currently nil — render honestly as "unwired", it IS the finding),
blend G-total and core G of the tick's competing ordinary winner for context (S, data
already extracted); (c) assembler to PDF, one page per cascade + a selection-consequence
caption ("this cascade sat in the arena at 0.0 and lost to X by margin m") (M).
Minimal exhibit = 3 cascades × the 3-panel score + dot rendering. Total cost M.

### 7.10 Q8 ANSWERED (2026-07-03) — weights + sim-first / reduction-safety harness

**Weights:** defaults in `efe.clj:50–57` only; no overrides in either production runner
(`wm_scheduled_run.clj`, `wm_outer_loop.clj`) — confirmed. Changing a weight = changing
the deployed objective directly; there is no config layer in between.

**Harness inventory (what "sim-first" concretely has):** (i) the cascade lane itself is
the exemplar — sim-only/read-only over a state copy; (ii) shadow lane:
`wm_shadow_extract.bb` → `data/wm-trace/wm-shadow-step.json` + `wm_shadow_pilot.py` (JAX
differentiable belief step, superpod-gated — the concrete prior art for HEAD tension 8);
(iii) R14's burn-in comparison shape (`futon3c.aif.calibration`); (iv) the two Q-scripts
from this MAP are now the before/after comparator for any blend change — rerun census +
counterfactual on pre/post traces and diff flip-rates/winners (this is C9's mechanism,
already built). **Gap:** `scripts/policy_harness.clj` is an empty stub (one comment line)
— the *named* policy harness does not exist; C9's replay-style "what would the last N
ticks have chosen under the candidate blend" script is a small build (S–M) on top of the
census reader.

### 7.11 MAP EXIT — all survey questions answered

Q1 ✓ (v2, artifact-corrected) · Q2 ✓ · Q3 ✓ (dispositions for DERIVE) · Q4 ✓ (subsumed:
schema, lane split, composition drift) · Q5 ✓ (293 cascades in-corpus; dot emitter is the
only gap) · Q6 ✓ (posterior-entropy override template) · Q7 ✓ (build list, cost M) ·
Q8 ✓ (harness inventory; policy_harness.clj is a stub; census scripts = C9 comparator).
Ready-vs-missing (§7.3) complete as updated by §§7.4–7.10. Surprises documented: the
lane artifact (§7.4-v2), the prosthetic-C reading (§7.5), F7 + the ΔG-nil defect (§7.7).
**Lifecycle exit criterion met; DERIVE opens next** — §7.6's order-of-operations is its
input, C10's confirmation and the §6.9-style gate remain Joe's.

## 8. DERIVE (opened 2026-07-03, Joe + Fable session)

Design for the §7.6 order-of-operations. Everything lands behind **behaviour-identical
defaults** (§4 reduction safety); each stage is independently shippable and independently
revertible. File targets are exact; a builder should need nothing beyond this section.

### 8.1 Entity/relation/data-flow deltas

**No new entity types.** The mission adds *fields* to existing artifacts and one new
artifact class:

- **Trace ranked-entry** (persisted by `trace.clj strip-ranked-action`, line 71): gains
  `:G-gap`, `:G-graph-pragmatic`, `:G-core` (whitelist additions), and for cascade rows
  `:score-provenance :placeholder` (see D1b). Consumers (census scripts, flight-record,
  VSATARCS readers) are select-keys/get-based — extra keys are backward-compatible.
- **Defect report** (new artifact, one file):
  `holes/labs/M-evaluate-policies/defect-dG-nil-for-cascades.md` → handed to
  M-G-over-cascades: 293/293 `:apply-cascade` rows at `(or dG 0.0)`
  (`war_machine.clj:3797`); rollout leg never fires; include one reproducing tick id.
- **Exhibit artifacts** (Q7 build, INSTANTIATE): `holes/labs/M-evaluate-policies/exhibit/`
  — per-cascade `.dot`/`.svg` + `exhibit.pdf`.
- **Data flow unchanged**: `compute-efe` → `score-actions` (sort at `efe.clj:493`) →
  judge → `trace-record` (`trace.clj:125`) → `data/wm-trace/`. All D-stages sit inside
  this existing pipe.

### 8.2 D1 — Observability (S, strictly additive)

- **D1a whitelist:** add `:G-gap :G-graph-pragmatic` to the `select-keys` vector at
  `trace.clj:71-73`. Precedent: the R19 fix for `:G-goal-outcome` (comment above the fn).
- **D1b placeholder honesty:** in `war_machine.clj` cascade-action construction
  (~line 3790-3799), add `:score-provenance (if dG :rollout-dG :placeholder)` next to
  `:G-total (or dG 0.0)`. Do **not** change the 0.0 fallback (it is load-bearing — see
  IHTB-2). The marker makes the constant self-describing; census v3 keys its lane guard
  on it instead of on nil-`:G-risk`.

### 8.3 D2 — Core/overlay split (S, strictly additive)

In `compute-efe` (efe.clj, the `merge` at ~458-473): emit
`:G-core (+ g-risk g-ambig)` alongside `:G-total`. Add `:G-core` to the D1a whitelist.
The decision map (judge) copies the winner's `:G-core`. Docs sentence (completeness doc
§R5 + the paper): "selection ranks by a multi-objective score with an EFE core
(`:G-core = risk + ambiguity`, reported per candidate)". This *is* C2, and C6's honest
branch until/unless later stages earn the stronger sentence.

### 8.4 D3 — Commensurability (decision C4)

**Decided: document-and-stage, not live renormalisation.**

- Add a units row per term to the §R5 table: risk/survival = preference-gap units ×
  urgency; ambiguity/info = summed variance units (complementary pair); structural
  pressure = exogenous scalar; graph = heuristic counts w/ 1000-mask; gap = weighted
  lookup; goal-outcome = weighted probability mass. (Facts, from the audit + code.)
- **IHTB-1**: IF terms are incommensurate and the weights absorb it invisibly, HOWEVER
  live renormalisation (z-norm over a trailing window) changes the deployed objective in
  a data-dependent way that defeats before/after attribution and can oscillate, THEN we
  document units now, expose `:G-core` (D2), and run renormalisation only as a **replay
  experiment** (D6 harness) whose output is a report, not a live change, BECAUSE the
  mission's bar is honesty-then-repair under reduction safety, and Q1 shows the practical
  bite (ambiguity inaudibility) is not fixable by scaling alone while C is weak (§7.5).
- Nats become available to the core pair only after D5a (real KL) — noted as the
  trajectory, not claimed.

### 8.5 D4 — Relabels without rewires (S)

- **Survival:** keep the computation and the persisted key; the *decomposition report*
  (docs + completeness §R5) presents `G-risk + 1.2·G-survival` jointly as **pragmatic /
  homeostatic pressure**, dropping the claim that survival is a distinct canonical EFE
  term. **IHTB-3**: IF the honest fix is merge-or-rename, HOWEVER renaming persisted keys
  breaks every trace consumer (census, flight-record, VSATARCS) for zero behavioural
  gain, THEN badges/docs carry the rename and keys stay, BECAUSE the key name is API and
  the badge is the honesty surface (same call as R18's badge system itself).
- **Structural-pressure:** same move — documented as *exogenous weight* (caller-injected
  scalar × 0.35), badge stays `analogical` with the sharper gloss `exogenous`.

### 8.6 D5 — Semantic repairs (flag-gated; default = current behaviour)

- **D5a risk-KL (L, shared with belly W1):** new fn in `futon2.aif.preferences`:
  `c-distribution` — per channel, a normalised preference density over [0,1] (uniform on
  [lo,hi] with exponential tails, temperature parameter); new
  `kl-risk` in efe.clj: Q(o|π) = per-channel Gaussian (mean/variance already produced by
  `fm/predict`), risk = Σ_ch Π_ch · KL(Q_ch ‖ C_ch). Behind opt-in `:risk-mode :kl`
  (default `:hinge`). **Module boundary with W1**: `c-distribution` lives in
  preferences and is the shared artifact; the belly imports it, neither reimplements.
- **D5b info→EIG (M):** port the portfolio pattern: `posterior-entropy` over the
  candidate set's softmax P(a) ∝ exp(−G/τ) (τ from policy-precision); expose as
  `:epistemic-mode :posterior-entropy` (default `:variance-complement`). Per §7.8's
  caveat, the transplant is badged `principled-approximation (action-posterior proxy)`,
  not `derived-from-FEP`.
- **IHTB-2 (ordering constraint, found in MAP):** IF G-info is behaviourally inert
  within-lane (flip 0.0%) and affinely redundant, HOWEVER *deleting* it shifts every
  ordinary G-total up by ≈ +5.6 (−0.4×~14), pushing them above the cascade placeholders'
  0.0 so unscored cascades would start WINNING the arena, THEN G-info stays numerically
  in place until either the placeholder rows are excluded from the argmin or D5b
  replaces it at matched scale, BECAUSE the placeholder constant is load-bearing in the
  current arena and any term deletion must be sequenced after cascade-row handling.
- **D5c ambiguity formula:** `0.5·ln(2πe·σ²)` summed per channel behind
  `:ambiguity-mode :gaussian-entropy` (default `:variance-sum`). One-liner; audibility
  still governed by D3's decision.

### 8.7 D6 — Replay harness (S–M; fills the Q8 gap, implements C9)

`scripts/wm_replay_blend.bb`: read persisted ticks (census reader), recompute each
candidate's total under a **candidate formula spec** (EDN map: weights, included terms,
modes), output winner-diff + flip-rate table vs actual — the before/after comparator for
every D-stage and the vehicle for D3's renormalisation experiment. Pure trace-side; no
JVM, no live state. (Limitation, stated: replay can vary weights/terms over *persisted*
values; it cannot re-run forward-model modes (D5a/D5c need sim-first spikes in VERIFY.)

### 8.8 Views / UI

No new interactive surfaces. Outputs are artifacts: census/replay reports (EDN + printed
tables), the exhibit PDF (Q7 list), updated §R5 table in the completeness doc. The
operator-facing change is textual: bulletin/docs say "multi-objective score with an EFE
core" wherever they said "EFE" of the blend.

### 8.9 Wiring diagram

Skipped, with reason: all changes are internal to the existing score→persist pipe of one
component (no new ports, loops, or cross-repo interfaces); the only cross-boundary
artifact is the D5a `c-distribution` module, whose boundary is stated in prose. If DERIVE
later grows a cross-lane redesign (cascade scoring), that belongs to M-G-over-cascades'
diagram, not here.

### 8.10 GF fidelity contract

| capability | class | tripwire |
|---|---|---|
| Hourly selection behaviour under default flags | **preserve** | I1 replay-equality (below); census flip-rates unchanged pre/post D1/D2 deploy |
| Trace schema consumability (existing readers) | **adapt** (additive keys only) | census v2 + flight-record run unmodified against new traces |
| Cascade rows present in arena | **preserve** (until C10) | count of `:apply-cascade` rows per tick unchanged |
| "EFE" label on the blend in docs/paper | **drop** (replaced by earned language) | C6 check: grep for the old sentence fails |

### 8.11 Invariant rules (checkable; VERIFY's logic-model targets)

- **I1 replay-equality:** under default flags, recomputed totals from persisted terms +
  D-stage code = persisted `:G-total` for every entry of the last N ticks (tolerance:
  |Δ| ≤ 1e-9 + the pre-existing hidden-term residual, which D1a drives to ~0 for new
  ticks).
- **I2 no bare constants:** every persisted ranked entry has a full decomposition OR
  `:score-provenance :placeholder`. (Census v3 asserts.)
- **I3 core additivity:** `:G-core = :G-risk + :G-ambiguity` exactly, every entry.
- **I4 no silent steering:** whitelist ⊇ terms entering `:G-total`; post-D1 residual
  mean-abs ≈ 0 on new ticks (census `:residual`).
- **I5 label-follows-code:** the five §6.3 surfaces each carry a declared sense; blend
  labelled multi-objective-with-core until a stronger claim is earned (C6/C8).

### 8.12 PSR — Pattern Selection Records

#### PSR-1: flag-gated prototyping-forward repairs

- Pattern chosen: prototyping-forward (futon2 sorrys discipline) + reduction-safety
- Candidates: big-bang semantic rewrite; long-lived feature branch
- Rationale: WM runs hourly in production; every semantic change (D5) ships dark behind
  a mode flag with replay/sim evidence before flipping; observability (D1/D2) ships
  first precisely because it is additive.
- Confidence: high.

#### PSR-2: combining-methods-as-diagnostic for the exhibit

- Pattern chosen: combining-methods-as-diagnostic (futon3/library)
- Candidates: single-score exhibit; prose-only comparison
- Rationale: the exhibit's three panels (wholeness/F, ΔG-unwired, blend-vs-core) put
  disagreeing valuations of the SAME cascade side by side; the disagreement is the
  diagnostic, per the pattern's validated live use.
- Confidence: high.

#### PSR-3: logic-model-before-code for I1–I5

- Pattern chosen: mission-coherence/logic-model-before-code
- Candidates: assert-in-tests-only
- Rationale: I1–I5 are exactly the shape that pattern serves — invariants checkable over
  an abstract trace before code hardens; VERIFY runs the model with a conforming witness
  + per-invariant adversarial case.
- Confidence: medium-high (pattern young but validated on outing_invariants).

### 8.13 DERIVE second pass (2026-07-03): readiness verdict + D7/D8

**Operator question:** ready to DERIVE a solution now, or more experiments first ("in
which case part of the DERIVE is the experimental series")? **Verdict: ready — with the
experimental series inside DERIVE**, exactly the parenthetical. Three experiments have
already run and each *changed the design* (census → IHTB-2's ordering constraint;
counterfactual → the prosthetic-C repair route; spectral → menu-prior-not-selector), so
experiments are design instruments here, not preliminaries. Per-stage readiness:
D1/D2/D6/D8 ready now (evidence sufficient, behaviour-safe); D3/D4 decidable as D7
evidence lands; D5 ships dark regardless (flips gated on D7 + W1 + Joe).

**D7 — the experimental series (first-class DERIVE component):**
- **E1** term-liveness census — ✓ ran (§7.4, v2; `wm_trace_census.bb`)
- **E2** core-only counterfactual — ✓ ran (§7.5; `wm_core_counterfactual.bb`)
- **E3** spectral potentials vs constructor functionals — ✓ ran
  (`E-evaluate-policies-spikes.md` spike 2)
- **E4** replay renormalisation sweep — pending D6 (`wm_replay_blend.bb`); decides §3.3
  empirically (does any commensuration change winners for the better?)
- **E5** conditioned-cascade probe — pending D1: re-score cascades with the live
  observation vector as circumstance (per §9.7's hand-off) and measure the value shift
  vs the ψ-scrap baseline
- **E6** KL-vs-hinge shadow comparison — pending D5a dark: run `:risk-mode :kl` in
  shadow over N ticks, diff winners vs `:hinge` (the D5-flip evidence)

**D8 — formal reconciliation (SHIPPED, exhibit pp. 8–9):** the standard formulations in
LaTeX (VFE; EFE risk+ambiguity and its epistemic/pragmatic dual; Gaussian ambiguity;
P(π) = σ(ln E − γG) over a finite menu), per the R18 audit's reference frame (futon2
`dcbe021`), then a 13-row term-by-term table (canonical object | computed object |
relation) and the one-paragraph technical statement. Its organizing result — the
sharpest form of §2's "ambiguity about what we are even computing": **the blend is a
flattened generative model**. Terms that canonically live in C (survival, goal-outcome),
in the habit prior E(π) (structural-pressure), and in the feasible menu Π (the
1000-mask) were projected into one sum beside the two real G-summands and two epistemic
proxies. The repair path in canonical terms: risk-as-KL absorbs survival+goal-outcome
into C; real EIG replaces info/gap; ambiguity in nats; mask → Π_feasible; pressure →
ln E(π); selection semantics + conditioning declared. Endpoint: G in nats = risk +
ambiguity (− EIG), everything else living where the theory puts it — the state that
would earn back "EFE-scored". (Embedding the same section into
`aif-wiring-explainer.html` is a DOCUMENT-phase item — the PDF is the canonical carrier
for now.)

### 8.14 DERIVE exit check (see §9 for ARGUE)

Implementable-from-this-section: D1 (2 file edits, exact lines), D2 (1 merge key + 1
whitelist entry), D3 (docs table + declared decision), D4 (docs/badges), D5 (2 new fns +
3 mode flags with defaults named), D6 (1 script, spec'd I/O), exhibit (Q7 list). Open to
ARGUE: whether D5a/D5b flip from dark-to-default inside this mission or ship dark and
wait for W1 (current stance: ship dark; flipping is a Joe-gated act with replay+sim
evidence). C10 confirmation still pending with Joe.

## 9. ARGUE (2026-07-03) — the exhibit IS the argument

**Operator decision (Joe, 2026-07-03, verbatim intent):** ARGUE is "best developed in the
form of the PDF with real cascades, real computations… this is the place where the
complexity of what's going on under the bonnet needs to see some light of day."
Accordingly the ARGUE body is the artifact:

> **`holes/labs/M-evaluate-policies/exhibit/argue-exhibit.pdf`** (6 pp; generator
> `scripts/exhibit_cascade_argue.py`, rerunnable; per-cascade `.dot/.png/.pdf` alongside)

Three real cascades — two production subjects (M-bayesian-structure-learning,
M-canon-fingerprint-store; the only two missions the lane has ever cascaded in the arena)
plus one **fresh** cascade constructed from this mission's own ψ — each carrying *every
valuation the stack can attach*: T, H, wholeness T·H, accuracy, complexity,
F = accuracy − 0.25·complexity (all recomputed live through the production constructor and
production ψ recipe), then the arena rows: blend G "not computed — placeholder 0.0",
rollout ΔG "abstained — outside the v2 move-set", canonical core "not defined for
cascades". The empty cells are typeset as first-class results. Page 1 carries the
plain-language argument (lifecycle item, discharged *inside* the artifact).

### 9.1 What the real numbers added beyond MAP

- **ψ reads the paperwork, and the paperwork moves.** Recomputed wholeness for
  M-bayesian-structure-learning is 0.792 vs 4.141 persisted — because its Status line now
  says SUPERSEDED, and the production ψ recipe (want = title, have = status) *feeds the
  status back into valuation*. The system's cascade values respond to mission lifecycle
  state through prose. Real, and double-edged: honest sensitivity vs valuation drift with
  no changelog.
- **The freshest, richest cascade of the three is this mission's own** (saturated size 14,
  truncated to budget 6; wholeness 4.33; F 4.861 — highest on both currencies). The
  method eating its own tail produced the strongest structure in the sample.
- **Wholeness and F agree on ordering (3 > 1 > 2) in this sample** — so the live
  combining-methods disagreement is not *between* the native currencies but between both
  of them and the arena's constant. (n=3: a sample fact, not a theorem — the exhibit
  prints both orderings so future runs can catch a divergence.)
- **M-canon-fingerprint-store cascades to size 1 with F < 0** — a single-pattern
  "cascade" the act-gate would reject on the F leg if the F leg were consulted at the
  arena; today it sits at 0.0 like everything else. Sharpest single illustration of the
  exchange-rate gap.

### 9.2 Pattern cross-reference (lifecycle item)

- `combining-methods-as-diagnostic` — instantiated by the exhibit's 3-currency panel
  (PSR-2); validated in the strong form: the diagnostic disagreement surfaced at the
  lane/arena boundary rather than where expected (between wholeness and F).
- `prototyping-forward` + reduction-safety — the D-stage design (PSR-1).
- `mission-coherence/logic-model-before-code` — I1–I5 as VERIFY's model (PSR-3).
- `prove-the-artifact-not-the-wiring` — the exhibit proves the diagnostic *surface* on
  real cases (non-seeded subjects; discriminates — separates the three cascades and
  exposes the arena constant; consumed — it is the ARGUE body).
- **Candidate NEW meta-patterns** (Joe: "we *might* find some meta-level design
  patterns"), named for library write-up in DOCUMENT:
  1. **`currency-before-merge`** — never sum value-signals from different lanes without
     an explicit exchange rate; a constant standing in for a missing exchange rate (the
     cascade 0.0, the 1000·mask) *is* the smell. Generalises §3.3 beyond this blend.
  2. **`placeholder-is-load-bearing`** — glue constants introduced as inert become
     behavioural dependencies (IHTB-2); sibling of the designed false-floor in G-ties.
     Before deleting an "inert" term, ask: what does the constant beat?
  3. **`valuation-reads-the-paperwork`** — when ψ/spec inputs are living documents,
     valuations drift with prose edits; date-stamp or content-hash the ψ at scoring time.

### 9.3 Theoretical coherence + trade-offs (lifecycle items)

The IDENTIFY anchors held: the R18 referent split (senses i–iv) survived contact with
production data and gained a measured shape (epistemic pole inert; overlay = prosthetic
C). Deepest alignment: canonical AIF locates pragmatic pull in preferences C — MAP/ARGUE
show the analogical terms are *compensating for weak C*, so the repair route (strengthen
C via W1; expose the core; commensurate before merging) is the theory-conformant one, not
a cosmetic relabel. Trade-offs accepted (each an IHTB in §8): persisted keys stay (API >
nominal honesty; badges carry the rename); live renormalisation declined (attribution >
elegance); D5 ships dark (production safety > completion optics); cascade scoring
deferred to M-G-over-cascades (ownership > speed).

### 9.4 Generalization + exit

The exhibit method (one artifact × every valuation × empty-cells-as-results) transfers to
any multi-lane scorer in the stack — the futon3c portfolio surface and any future
G-over-cascades design can be audited with the same panel; the census/replay pair
transfers to any argmin-over-persisted-scores selector. **Exit check:** the design reads
as forced by the evidence (§7 numbers → §8 order-of-operations → §9 exhibit), and the
plain-language page stands alone. ARGUE complete pending Joe's read of the PDF; VERIFY
next (logic-model I1–I5 + D5a/c sim spikes).

### 9.5 The fold test (Joe's ask, second ARGUE pass): the cascade CONSTRUCTS — ΔF ∧ ΔG both real

**Operator prompt:** "run the LLM fold *over this cascade*… does the LLM fold lead to a
complementary sorry + DarkTower wiring diagram?" Answer: **yes, both** — exhibit pp. 6–7.

- **Recorded LLM-turn fold** (impl #2 of `futon2.aif.fold`, turn-fn = this session,
  recorded per the incident-safe design): `exhibit/fold-turn.edn`. Six boxes, each a
  D-stage fitted to this circumstance with its pattern's HOWEVER resolved; spine
  s1 typed-score-vector → s2 legible-arena → s3 admission-gate → s4 bounded-experiments
  → s5 durable-evidence → s6 PAR-close.
- **Shared evaluation:** coverage 6/8 = 0.75 ⇒ **ΔG = −0.750** (fold-eval
  coverage→rollout, γ⁰·(−coverage)). With cascade-3's **F = 4.861** this is the first
  cascade in the mission's orbit with **both act-gate legs real: ΔF > 0 ∧ ΔG < 0** — the
  conjunction the production arena has never once evaluated (every arena ΔG abstained).
- **DarkTower wiring:** `exhibit/m-evaluate-policies.clean.edn` →
  `clean_to_lean.py --mode standalone` → `darktower-check.lean` → **core-lean compile
  PASS, 0 errors** (structural soundness: boxes compose, spine is a valid `BV.seq`,
  discharge polarities valid). Structure certified, not semantics — the floor that rules
  out hallucinated wiring.
- **The complementary sorries** (typed, per the hole grammar): in-box —
  s3 `sorry/queryAnswer/parse` (I2 enforced in the judge path, not census-only) and
  s6 `sorry/sorryProof/payoff` (meta-patterns written to futon3/library); cascade-level
  policy-holes — **value-currency commensuration** and **probabilistic term
  constructions** (D5a/D5b).
- **The finding that makes this ARGUE-grade:** the fold **independently re-derives the
  mission's own gap list**. Its boxes are the DERIVE stages; its policy-holes are D3's
  commensuration question and D5's probabilistic builds — convergence between the
  pattern-library route and the audit-data route to the same two "real work" sites. And
  policy-hole 1 is *itself* evidence for `currency-before-merge` as a genuine library
  gap: the fold reached for such a pattern and found nothing to fold.

### 9.6 Non-totality of EFE-over-cascades (operator observation, third ARGUE pass)

**Joe, 2026-07-03, on cascade-3 being "never in the arena":** "maybe *in some sense* 'EFE
over cascades' isn't total — here's a cascade, we were able to evaluate it, but we can't
really look at maxima or minima across *all* cascades… we can't even really approximate.
Like, consider PageRank style methods."

Sharpened into three distinct partialities:
1. **Domain:** arena ΔG is defined only inside the v2 move-set (the abstention of §7.7) —
   a coverage gap, fixable.
2. **Cost:** pointwise G-over-cascades is total only *relative to an oracle* — each
   evaluation is a constructor run (F) or an LLM fold-turn (ΔG). Economic totality, not
   mathematical.
3. **Extremization:** min/max over cascade space is not effectively computable — finite-
   but-astronomical per snapshot, open-ended over time (extensible patterns, endless
   link-types per M-G-over-cascades' HEAD). Enumeration, not countability, is the barrier.

**The precision that saves the label:** canonical AIF never extremizes the policy space
either — it softmaxes a *given finite menu*; the menu-generator is the E-matrix (habit
prior), already flagged partial in the completeness doc. `construct_cascade` IS the
E-matrix at cascade grain: an amortized one-sample proposal seeded by ψ. Consequence for
C6-grade language: the arena's selection is **"argmin over a generated menu"**; selection
quality is bounded by proposal quality; claiming optimality over the space is the
dishonesty, not failing to compute it. **R13 recurs one level up: policy adequacy →
proposal adequacy.**

**PageRank reading:** stationary/spectral potentials over the pattern graph (2,721 co-app
+ 561 descent edges — computable today) = global fixed point at node grain, lifted
additively to cascades. Exactly the futon6 g(s)/G(π) split. Caveats: additive lifts miss
the functional's interaction terms (H-coherence product, submodular saturation, complexity
penalty) ⇒ **priors and bounds, not extrema**. One real guarantee: coverage alone is
monotone submodular, so the constructor's greedy is within (1−1/e) of optimal *coverage*
under budget; the guarantee dies for F and T·H.

**Where it lands:** the sampler contest is E-cascade-sampler-sampler's charter — GFlowNet
(sample ∝ exp(−G/τ), the theory-conformant move) already an entrant; PageRank/spectral
potentials proposed as a second, cheap entrant; candidate bounded experiment =
potential-sum ranking vs constructor F/wholeness on real cascades, measuring the
non-additive residual (combining-methods again). Oracle-call budgets are mandatory
(budget-bounds-exploration, reflexively). Hand-off note owed to that excursion +
M-G-over-cascades (C10): *selection semantics must be declared — extremum (intractable)
vs amortized sample (GFN) vs field-lift bound (spectral).*

### 9.7 Value is conditional on the situation (operator observation, fourth ARGUE pass)

**Joe, 2026-07-03:** "it seems *somewhat* strange to evaluate 'policies' in absence of
'the situation'… even in [the one-off 'exactly the right pattern' case] it is contextual —
the classic Context / Problem / Solution form for patterns gets 'pushed' into the real
world which must *supply* the matching context."

**Canon agrees exactly:** EFE is G(π; Q(s), C) — expectation under the *current* belief
state and preferences; a policy has no value in vacuo. Scoring cascades without the
situation is not EFE even in shape.

**Situatedness audit of our valuations (the asymmetry):** arena G-total for atomic
actions conditions on the live observation vector (properly situated); rel/T/accuracy
condition on ψ — a 160-char title+status scrap; H-coherence conditions on nothing
(intrinsic); complexity on corpus base-rates (unsituated); fold-ΔG on the circumstance
*only through the oracle* (the LLM turn resolves HOWEVERs against it; the coverage
formula never sees it). So the two lanes consume **different slices of the world** — a
second, independent reason (beside §9.5's currencies) the arena comparison was never
apples-to-apples.

**Re-reading of §9.1's ψ-coupling finding:** the 4.141→0.792 collapse on a status-line
change is not drift — it is *correct conditionality on a thin proxy*. The defect is the
starved conditioning channel (paperwork fragment instead of belief state), not the
sensitivity. `valuation-reads-the-paperwork` is what fit-scoring looks like when the
context input is prose.

**Typed continuation of §9.6:** the C/P/S pattern form is a conditional
(Context → Problem → Solution); application is modus ponens with the world supplying the
antecedent (Alexander's "fit": form has no goodness apart from context). So
G-over-cascades is not merely partial — it is **curried**: `G(cascade)` is a lambda;
`G(cascade | situation)` is the value. The fold *contract* already types this honestly
(`fold : (cascade, circumstance) → …`); the implementations degrade circumstance to a
string, and arena `:apply-cascade` rows are never re-conditioned on the tick's
observation.

**Consequences:** (a) the §6.3 referent-declaration shape gains a third axis — every
EFE-labelled surface declares **sense (i–iv) × selection-semantics (§9.6) ×
conditioning (what slice of the situation it consumed)**; (b) fourth candidate
meta-pattern: **`declare-the-conditioning`** ("score the fit, not the form") — joins the
§9.2 three for DOCUMENT; (c) repair direction for the cascade lane (hand-off to
M-G-over-cascades / C10): condition cascade valuation on the same
structured-observation-vector the atomic lane uses — our own fold's s1 box, applied
reflexively — so one situation feeds all valuations; (d) cheap exhibit improvement
(offered, not built): a "conditions on" column in the valuation tables.

**Both offers EXECUTED same-day → `holes/E-evaluate-policies-spikes.md`** (E-prefix
excursion, Fable-owned): spike 1 = the conditions-on column (exhibit regenerated, 8 pp);
spike 2 = spectral potentials vs constructor functionals over a 13-mission sample —
ρ(Σπ, F) = 0.885 but heavily size-mediated (size confound ~0.85–0.88); size-controlled
mean keeps ρ 0.808 vs F, drops to 0.566 vs wholeness (the H-product is where additivity
fails — §9.6's caveat measured); top-of-ranking diverges (field-lift picks M-aif2, F
picks this mission) ⇒ spectral = viable menu prior, NOT a selector; and the production
lane has been cascading the two *weakest* ψs in the sample while the strongest never
entered the arena — menu quality bounds selection quality, illustrated.

### PUR-1: combining-methods-as-diagnostic (exhibit application)

- Pattern: combining-methods-as-diagnostic
- Actions taken: built a 3-currency valuation panel over 3 real cascades (2 production, 1
  fresh); recomputed all native-currency values through the production constructor + ψ
  recipe; typeset non-computed valuations as first-class cells.
- Outcome: success
- Prediction error: medium — expected wholeness-vs-F disagreement; the actual diagnostic
  signal appeared at the lane/arena boundary (both native currencies vs the 0.0
  constant), plus an unexpected diachronic signal (ψ status-coupling).
- Notes: three candidate meta-patterns named (§9.2) for DOCUMENT-phase library write-up.

### PUR-2: llm-fold (fold contract impl #2) applied to cascade-3

- Pattern: fold : (cascade, circumstance) → {:wiring :delta-g :policy-holes} — LLM-turn
  build axis (E-llm-fold), shared coverage→rollout evaluation, DarkTower CLean render
  as structural gate
- Actions taken: recorded fold turn over the 6 shown patterns (fold-turn.edn); CLean
  authored (m-evaluate-policies.clean.edn); clean_to_lean standalone render compiled
  with core lean (0 errors); coverage/ΔG computed per fold-eval; wiring diagram +
  fold section added to the ARGUE exhibit (pp. 6–7); generator extended
  (make_fold_dot + fold section, rerunnable from fold-summary.json).
- Outcome: success
- Prediction error: low-medium — expected the fold to work on hygiene patterns; did
  not predict the *convergence* (fold's policy-holes = DERIVE's two "real work" sites)
  or that the fold would independently evidence the currency-before-merge library gap.
- Notes: first cascade in this mission's orbit with both act-gate legs real
  (ΔF = 4.861 > 0 ∧ ΔG = −0.75 < 0); the arena has never evaluated that conjunction —
  strengthens C10 (score cascades for real via M-G-over-cascades).

## 11. VERIFY (opened incrementally 2026-07-03; **ARGUE gate PASSED same day** — Joe:
"Broadly we are OK to proceed into VERIFY." The exhibit PDF stands as the ARGUE record;
"broadly" noted — any specific ARGUE objections Joe raises later are folded here as
verification-time discoveries, per the lifecycle's decision-log slot)

- **Structural verification:** wiring diagram deliberately absent (§8.9 records why);
  the DarkTower CLean render (§9.5, core-lean PASS) stands in as the structural check
  for the fold's construction.
- **Logic model I1–I5 (PSR-3) — BUILT + GREEN:**
  `test/futon2/aif/invariant_model_test.clj` — plain checkable predicates over an
  abstract trace (no core.logic in deps; pattern essence preserved: conforming witness
  ⇒ 0 violations across all five invariants; one adversarial trace per invariant,
  each caught by exactly the intended check — including replays of the two real
  historical defects: I1's tampered-total = the 0.296 hidden residual, I4's
  dropped-key = F2's whitelist strip). 6 tests / 6 assertions / 0 failures;
  clj-kondo 0/0.
- **Deploy finding (parcel A):** the hourly WM tick is a **fresh cron process**
  (`0 * * * * … clojure -M:wm-scheduled`) — no long-lived JVM holds this code, so
  `2d6533e` self-deploys at the next on-the-hour tick. No reload, no serving-JVM
  hazard. Live-tick checks (I4 residual→0, provenance markers present) run once
  post-deploy ticks exist.
- **Completion-criteria pre-check:** C1 ✓ (census) · C2 landed (2d6533e), live-verify
  pending next tick · C3 in progress (§7.6 + D-stages) · C4 decided-documented (D3),
  E4 evidence inbound (claude-8) · C5 design decided (mask → Π_feasible), build pending
  · C6 language drafted (D2/D8), paper edit pending · C7 ✓ exhibit (10 pp) · C8 declared
  senses ✓ (§6.3 + D8), badge re-check at DOCUMENT · C9 mechanism built (census/replay
  comparators), archive discipline live · C10 candidate, Joe's call.
- **E4 REVIEWED + C4/D3 EMPIRICALLY CLOSED (2026-07-03):** the sweep (ledger row B)
  shows every commensuration config just moves winners toward `learn-action-class` —
  the same starved-input attractor as core-only — at rates from 11.4% (equal-unit) to
  93.8% (naive 1/σ ambiguity). No config has a warrant for "better"; renormalisation
  alone is a *different arbitrary weighting*, not a repair. **§8.4's D3 decision
  (document units, decline live renorm) is vindicated by its own experiment**: the
  honest units fix remains the canonical-slot restoration (D8) — strengthen C, put
  E/Π terms back in their slots — not σ-scaling. Decision-log entry per the lifecycle:
  C4 = decided + now evidence-backed. One design note carried forward: argmin hears
  *within-tick* dispersion; corpus-σ is the wrong normalisation unit if anyone revisits.
- **Remaining VERIFY items:** D5a sim spike / E6 shadow comparison (fully unblocked —
  boundary agreed, contract ratified); live-tick I-checks (watcher armed); the exhibit
  could gain E4/E5 panels at DOCUMENT.
- **E7 (new D7 entry, 2026-07-03):** post-merge re-census of G-goal-outcome's decision
  influence. claude-4's `2315d49` (branch `wm-policies-proof-strategy-checkpoint`;
  seam-checked: `c_vector.clj` only, `predictive-goal-outcome-risk` unchanged, no
  contract impact) makes the belly's advancement join far livelier (190/455 entries
  reachable; re-ranks vs no-op). Our "goal-outcome flips 0% (young)" is dated the
  moment that merges — rerun `wm_trace_census.bb` after merge + a few days' corpus and
  update §7.4's table. Cheap: the apparatus already exists.
- **E6 hypothesis sharpened (analytic, from the E4 review):** the D5c
  `:gaussian-entropy` mode changes ambiguity's *dispersion*, not just its magnitude —
  entropy differences are ½·ln(σ²ᵢ/σ²ⱼ), so within-tick variance ratios of ~6× become
  ~0.9-unit spreads (vs ~0.03 raw). Unlike E4's external 1/σ scaling, the canonical
  formula may make ambiguity audible *intrinsically*. Untestable from traces
  (per-channel σ² not persisted; only the sum is) ⇒ this is exactly what the E6
  shadow run must measure before any flip — and a caution that D5c is NOT
  behaviourally-neutral-if-flipped, despite being a "one-liner".
- **W1 boundary (D5a): AGREED 2026-07-03** — claude-4 (new W1 driver), durably at
  `E-C-vector-live.md:230`. Ownership: **`c-distribution` lives in
  `futon2.aif.preferences`, owned by this mission's D5a; W1 consumes** — neither builds
  a private C. Ratified consumer requirements, adopted into the D5a spec as its
  interface contract:
  1. **Bernoulli/point-mass form** for binary `:becomes` goal-outcome entries (Q = the
     policy's predicted satisfaction prob via `credit-satisfy-prob`); continuous
     density only for range channels. Expose `(log-preference dist x)` + `(kl q dist)`
     for both Gaussian and Bernoulli Q.
  2. **KL in nats, densities normalised over [0,1], stated in one place** — so D5a's
     Σ Π_ch·KL and W1's goal-outcome term add honestly into G-total. Temperature:
     higher = softer; temp→0 = hard hinge/point-mass; default documented.
  3. **Degrade-safe:** flag off / density absent ⇒ exactly current hinge behaviour.
  4. **bb-loadable** (pure Clojure or dynamic-resolve; `c_vector.clj` keeps loading
     under babashka).
  Faithfulness via the dcbe021 badge layer + the D8 formulas (exhibit pp. 8–9).
  **D5a is now fully specified and unblocked** — build can be dispatched or done
  owner-side once VERIFY's live checks close.
- **Live I-check baseline (13:52):** last tick on disk = 12:04Z (pre-deploy, correct):
  new keys absent, provenance nil, residual mean 7.4e-2 / max 4.0 on that tick — the
  "before" snapshot the first post-deploy tick will be diffed against. Watcher re-armed
  (3rd arm; the prior two died with session cycles — known bg-shell lifecycle, not a
  defect).

### PUR-3: logic-model-before-code (I1–I5 model)

- Pattern: mission-coherence/logic-model-before-code
- Actions taken: modelled all five §8.11 invariants as violation-collecting predicates
  over abstract ticks/surfaces; conforming witness + 5 adversarial traces (two replaying
  real historical defects); wired into the standard test suite so the model runs on
  every `clojure -X:test`.
- Outcome: success
- Prediction error: low — the pattern fit exactly; one deviation: no core.logic/pldb in
  futon2 deps, so plain predicates instead (recorded; the checkable-proposition essence
  is intact).
- Notes: I4's adversarial case needed care to isolate from I1 (a dropped key that also
  falsifies the total fires both — the model made that coupling visible, which is
  itself a small design insight: observability defects masquerade as arithmetic ones).

## 10. Coordination ledger (2026-07-03 — session on Agency as claude-5)

Operator brought the session onto the Agency mesh; handoffs cut per the coding-handoff
protocol (author ≠ reviewer; review stays with claude-5). Roster at dispatch: codex-1
idle (only Codex), claude-1/2/8/10/11 idle.

| parcel | to | job-id (suffix) | contents | gates asked |
|---|---|---|---|---|
| A (code) | ~~codex-1~~ → **claude-11** | ~~…c612645b~~ …19f4c9e6 | D1a whitelist + D1b `:score-provenance` + D2 `:G-core` + D5c dark `:ambiguity-mode`. *(codex-1 failed: OpenAI usage limit until 2026-07-18; re-routed.)* **✓ REVIEWED — PASS. Commit `2d6533e`.** Diff read in full: all four edits per spec — whitelist + I4 comment (R19 precedent cited); `:G-core (+ g-risk g-ambig)` ⇒ I3 holds by construction (same vars as the emitted `:G-risk`/`:G-ambiguity`); two-arity `ambiguity` with byte-identical `:variance-sum` default and 1e-9-floored `:gaussian-entropy`; `:score-provenance` beside the untouched load-bearing 0.0. **Reviewer re-ran the gates independently:** `clojure -X:test` → 404 tests / 1297 assertions / 0 failures (matches claim); clj-kondo 0 errors 0 warnings on the 3 source files; paren-balance read OK; no conflict markers; tree clean. Note: live persistence of the new keys begins only when the serving JVM next picks up futon2 code — deploy/reload is an operator-visible step, not part of this parcel. **D1+D2+D5c LANDED ⇒ E5's successor probes and I4's residual→0 check unblock on the next fresh ticks** | ✓ all gates re-run |
| B (apparatus+experiment) | **claude-8** | …74576ec9 | D6 `wm_replay_blend.bb` (+ sweep driver `wm_e4_renorm.bb`) + E4 → `e4-renorm-sweep.edn`. **✓ REVIEWED — PASS. Commit `be716e6`.** E4 headline: identity spec flips 0.0% (harness-level I1 + third independent formula validation); **z-norm flips 34.4%** (hidden residual argmin-inert under it); **1/σ on ambiguity alone OVERSHOOTS to 93.8%** — "audible" at 1/σ means *dominant*; equal-unit mildest at 11.4%; core-only replay 64.8% = exact complement of census 35.3% (Q2 cross-validated by an independent implementation). Best finding: **magnitude vs variation** — argmin hears σ, not μ (G-info's μ=13.97 is a uniform bonus; decisive terms are high-σ survival/risk) — refines §7.4's reading 2. Review checks: artifact + findings read; **reviewer re-ran identity spec → 0.0% flips, winners bit-identical**; kondo 0/0 per-file (the 2 warnings in a joint lint = ns-less-script artifact, dissolved); determinism re-verified. Reviewer note for D3: their σ is corpus-wide; selection is *within-tick* — any future normalisation should use within-tick dispersion | ✓ all gates re-run |
| C (experiment) | **claude-10** | …2471e292 | E5 conditioned-cascade probe → `e5-conditioned-probe.json` | **✓ REVIEWED — PASS.** Commit 57ae720. **E5 answer: prose-grain conditioning nearly rebuilds cascades** — mean Jaccard(base, cond) 0.128, 11/13 top-pattern flips, mean \|ΔF\| 1.95, F-ordering ρ 0.566. §9.7's sensitivity claim empirically confirmed. Author added a third caveat unprompted (homogenization: inter-mission Jaccard 0.006→0.091 — part of the shift is convergence on the shared situation). Review checks: commit+artifact read; dilution flag verified false; **signed-ΔF bias check run by reviewer** — F rises only 6/13, gains concentrated in AIF-register missions (+5.2..+5.7) ⇒ uniform length-inflation ruled out, *topical-affinity confound* named instead. Interpretation carries claude-2's slice-2a qualifier: conditioning is necessary-not-sufficient. Full rerun not performed (determinism rests on recorded tick + pure pipeline — stated, not hidden). Bonus: this mission's conditioned cascade pulled `expected-free-energy-scorecard` / `policy-precision-commitment-temperature` / `belief-state-operational-hypotheses` — the situated constructor reached for the evaluation-layer patterns |
| D (coordination) | **claude-2** | …281ab605 → …c1ee82b4 | defect report + §9.6/9.7 requirements + C10 FYI. **✓ COMPLETE** — durable ack at `M-G-over-cascades.md:833`: (a) mechanism ACCEPTED, folded as first-class coverage gap (ties to their T1 typed-link direction); (b) boundary MATCHES — their slice-2a robust negative *qualifies* our conditioning axis: **richer conditioning is likely necessary-not-sufficient** (carry into E5's interpretation at review); (c) all three DERIVE declarations ACCEPTED as requirements. Return-offer: reuse the badged faithfulness layer (cascade-F + coverage→ΔG both `:principled-approximation`, `dcbe021`) rather than re-deriving — adopted for D5/D7 | ✓ |

**Held with claude-5 (not dispatched, with reasons):** D5a risk-KL (L build; needs W1
module-boundary coordination + ARGUE gate first); I2 gate in the production judge path
(serving-JVM-adjacent; VERIFY first); VERIFY logic-model I1–I5 (PSR-3 — owner-side);
meta-pattern flexiargs (DOCUMENT phase); all reviews of bells-back (each reviewed
against its gates before anything is accepted — diff read, verify step rerun, checks
stated). Phase note: parcels A–C are D7 experimental apparatus + behaviour-identical
observability, i.e. DERIVE-internal per §8.13; nothing semantic flips anywhere.
