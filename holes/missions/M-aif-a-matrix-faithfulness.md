# Mission: Make the belief-update observation model A faithful (M-aif-a-matrix-faithfulness)

**Date:** 2026-07-13  
**Status:** **INSTANTIATE (Stage 1 landed; simulation spike complete; Stage 2 exogenous corpus pending)**  
**Owner:** Joe + unassigned implementation owner  
**Primary repo:** futon2  
**Related missions:** `M-aif-faithfulness` (parent badge audit),
`M-aif-wiring` (R1/R3 wiring), `M-evaluate-policies` (precedent for separating
formal fidelity from behavioural evidence)

## 1. IDENTIFY

### Motivation — the gap

The R18 contract still gives `belief-update` a
`:principled-approximation` badge. That verdict is directionally right but its
stored wording is stale: the code no longer has “no A-matrix.” It has an
explicit, hand-set event-block observation model, `a-matrix-v0`, and an exact
categorical Bayes update behind `:likelihood-mode :a-matrix`. Production still
uses the default `:legacy` update, and the declared A entries are likelihood
ratios chosen by hand rather than a calibrated conditional distribution
`P(o|s)`.

The discrepancy is therefore two-dimensional:

1. **Formal fidelity:** the production belief path does not yet execute an
   explicitly declared, normalized AIF generative model with observation model
   A, transition model B, and initial prior D.
2. **Empirical grounding:** the available A structure and event-weight
   tempering have not been estimated or calibrated against a corpus that can
   distinguish them from the diagonal legacy model.

This mission exists to close both gaps without treating “the matrix is present
in code” as evidence that the matrix describes the world.

### Current implementation — the baseline

- Hidden-state vocabulary: seven entity statuses in
  `futon2.aif.belief/status-set`.
- Event observation vocabulary: the same seven `state/*` event types.
- Production update:

  ```text
  q'(s) ∝ q(s) · (1+w)^[o=s]
  ```

  This is an implicit diagonal likelihood model and remains the default.
- Dark explicit update:

  ```text
  q'(s) ∝ q(s) · A[o,s]^κ(w),   κ(w)=log₂(1+w)
  ```

  `a-matrix-v0` has diagonal gain 2.0, five lifecycle-adjacent entries at
  1.3, and four contradictory entries at 0.7. Its entries are declared
  likelihood ratios, not column-normalized probabilities over observations.
- The prior is carried between ticks when `*carry-belief?*` is enabled, but no
  explicit transition matrix B is applied before the observation update; this
  is an implicit identity-transition assumption.
- The separate 8×7 `channel-emission-matrix` names the status-to-strategic-
  channel mapping but is not the event-block A used by `update-entity-belief`.
- Production caller: `scripts/futon2/report/war_machine.clj` invokes
  `belief/update-belief-batch` without likelihood options, hence `:legacy`.

### Existing evidence and why it is insufficient

`scripts/a_matrix_shadow.bb` replayed the real belief namespace over 45 trace
files and 278,790 invertible entity updates. It found:

- 100% uniform priors in the replayable rows;
- only one event type, `:strengthened`;
- zero argmax-status flips;
- mean `KL(a-matrix || legacy) = 0.00102` nats.

This establishes a useful negative result: the present trace slice cannot
identify A or adjudicate the off-diagonal structure. It does **not** establish
that the hand-set A is correct, harmless prospectively, or unnecessary. A
distinguishing corpus needs varied event types, non-uniform sequential priors,
and an independently defensible account of latent status.

### Theoretical anchoring

For the declared categorical state model, a faithful filtering step has the
shape

```text
q⁻(s_t) = Σ_s B(s_t | s_{t-1}) q(s_{t-1})
q(s_t)  ∝ A(o_t | s_t) q⁻(s_t)
q(s_0)  = D(s_0)
```

where every A column is a conditional distribution over observations for one
hidden state, every B column is a conditional distribution over successor
states for one predecessor state, and D is a normalized initial prior.

The mission distinguishes two warrants:

- **Model-relative/formal warrant:** inference is exactly derived from the
  declared A/B/D model.
- **World-relative/empirical warrant:** A/B/D and any reliability/precision
  parameters predict held-out observations and yield calibrated posteriors.

A hand-authored but normalized A can earn the first warrant. It cannot earn the
second merely by being mathematically well formed. The R18 presentation must
show both rather than compressing them into one flattering green mark.

### Scope in

1. Specify the hidden-state and observation alphabets, including how “no
   relevant event,” missing observations, unknown event types, and simultaneous
   events are represented.
2. Specify a proper conditional observation model A with explicit orientation,
   units, normalization, positivity/structural-zero rules, and version identity.
3. Make the temporal prediction assumption explicit through B, even if the
   first justified B is an identity or lifecycle-constrained model; specify D.
4. Give event weight `w` a probabilistic interpretation—observation
   reliability, repeated evidence, or likelihood precision—or remove it from
   the Bayesian update. Upstream importance must not silently become evidence.
5. Define the data contract for learning/calibrating and evaluating A/B/D,
   including adjudicated status evidence and sequential entity histories.
6. Compare the explicit model against the diagonal legacy model prospectively
   and on held-out data.
7. Wire the selected model into the real update path with trace provenance,
   model version/hash, and fail-closed validation.
8. Repair the R18 manifest, paper, and explainer so they state the formal and
   empirical warrants separately.

### Scope out

- Reworking the R5 controller or its engineering-control terms.
- Claiming that A must be learned online; supervised estimation, Bayesian
  estimation, or a justified fixed model remain candidates until DERIVE.
- Jointly learning every WM observation channel in one model. The event block
  is the primary object; the strategic-channel emission block must be mapped
  and kept coherent but may require a separately staged estimator.
- Changing the seven-status ontology merely to improve fit. Ontology defects
  discovered in MAP are surfaced as blockers or a follow-on mission, not
  hidden by a more flexible likelihood.
- Promoting the badge because a production switch was flipped. Behavioural
  activation and epistemic warrant are separate gates.

### Relationship to the AIF-theory retract

The proposed AIF-theory retract of the implementation graph is compatible with
this mission but broader than it. For the belief node, the retract should map
the implemented filter onto the AIF roles A (observation likelihood), B
(transition/predictive prior), D (initial prior), and Q(s) (posterior), while a
typed residual records hand-setting, calibration debt, or omitted dynamics.
This mission supplies the evidence needed to make that particular retraction
edge exact and auditable. The controller-wide retract belongs in the parent
faithfulness/wiring work rather than being smuggled into the A-matrix build.

### Completion criteria

- **C1 — Vocabulary closure:** A machine-readable contract enumerates S and O
  and handles no-event, missing, unknown, and multiple observations without an
  unrecorded fallback.
- **C2 — Proper A:** A validator proves finite/non-negative entries and
  `Σ_o A[o,s]=1` for every state s. Orientation and units are tested.
- **C3 — Explicit prediction prior:** B and D are declared and validated as
  distributions; the filter demonstrably computes `A × (B × q)` rather than
  relying on an undocumented identity transition.
- **C4 — Weight semantics:** `w` has a documented probabilistic source and a
  calibration test, or it is removed from the likelihood path. Arbitrary
  controller importance cannot enter as likelihood precision.
- **C5 — Distinguishing corpus:** the evaluation corpus contains multiple
  event classes, carried non-uniform priors, sequential histories, and an
  independently recorded status/adjudication signal. Coverage and class
  imbalance are reported rather than masked by duplication.
- **C6 — Preregistered evaluation:** before fitting, the mission records the
  train/validation split, baselines, primary metric, calibration metric, and
  acceptance rule. At minimum the comparison reports held-out log loss,
  multiclass Brier score, calibration, posterior entropy, and status errors.
- **C7 — Baseline comparison:** the selected A/B/D model is compared with the
  diagonal legacy model and any hand-set candidate. Promotion requires
  held-out evidence, not merely winner flips or in-sample fit.
- **C8 — Sensitivity:** conclusions survive reasonable smoothing priors,
  sparse-class treatment, and removal of repeated/near-duplicate entity
  histories; uncertainty intervals accompany point estimates.
- **C9 — Production provenance:** every tick records likelihood mode and the
  exact A/B/D version or content hash. Malformed or unavailable model data
  fails closed rather than silently selecting `:legacy`.
- **C10 — No indefinite dual semantics:** after prospective verification, the
  production contract has one declared belief-update semantics. Any retained
  comparison implementation is explicitly experimental and cannot be selected
  by an ambient escape hatch.
- **C11 — Truthful faithfulness report:** R18 separately reports
  model-relative formal fidelity and world-relative empirical grounding. The
  badge/readout cannot imply that exact inference makes a poorly evidenced
  generative model true.
- **C12 — End-to-end witness:** a replayable sequence demonstrates prior
  prediction through B, update through A, trace provenance, and a calibrated
  posterior, with tests covering contradiction and lifecycle-adjacent cases.

### Source material

- `src/futon2/aif/belief.clj`
- `scripts/futon2/report/war_machine.clj`
- `test/futon2/aif/belief_test.clj`
- `holes/E-r1-a-matrix-design.md`
- `scripts/a_matrix_shadow.bb`
- `holes/labs/M-aif-faithfulness/a-matrix-shadow.edn`
- `data/r18-badges.edn`
- `holes/E-r18-faithfulness-audit.md`
- `holes/M-aif-faithfulness.md`
- `holes/aif-wiring-explainer.html`
- `p4ng/main-2026.tex`

### IDENTIFY exit — AGREED 2026-07-13

Joe has read the proposal and agrees that:

1. formal AIF fidelity and empirical model grounding are separate required
   claims;
2. the completion criteria are the right gap, without prematurely selecting an
   estimator;
3. the event-block A is the first implementation boundary; and
4. the controller-wide AIF-theory retract remains related but separately
   scoped.

## 2. MAP

### Inventory: what exists

**Code (ready, no new code needed for Stage 1):**

| Component | Location | Status |
|---|---|---|
| Status set (7 hidden states) | `belief/status-set` | Ready. `:spawned :refined :strengthened :addressed :falsified :foreclosed :reopened` |
| Event observation vocabulary | same 7 `state/*` event types | Ready, but only `:strengthened` fires in current traces |
| `a-matrix-v0` (hand-set, likelihood ratios) | `belief/a-matrix-v0` | Ready as data structure; needs normalisation conversion |
| `a-matrix-identity` (diagonal, ≡ legacy) | `belief/a-matrix-identity` | Ready; reduction-theorem tested |
| `:likelihood-mode :a-matrix` update path | `belief/update-entity-belief` opts arity | Ready (DARK, byte-identical default) |
| Belief carry across ticks | `belief/*carry-belief?*` (ON) | Ready; prior = previous posterior via `reconcile-belief-carry` |
| Channel emission matrix (8×7) | `belief/channel-emission-matrix` | Named and consistency-tested; not yet load-bearing at runtime |
| Fold escrow / build-test outcomes | `aif/fold_escrow.clj`, `aif/fold_eval.clj` | Ready; produces typed evidence events (parse, type-check, structure-check, compile, test) |
| Adjudicated handoff system | `holes/M-zaif-harness.md`, `labs/M-zaif-harness/` | Ready; 6 dispatches on record, 2 failures caught, verdicts typed |
| BMR / Dirichlet machinery | `aif/bmr.clj` | Ready; could host corpus-estimated A as Dirichlet posterior |
| Shadow replay harness | `scripts/a_matrix_shadow.bb` | Ready; reusable for prospective evaluation |

**Data (ready):**

| Data | Location | Status |
|---|---|---|
| 45 WM trace files | `data/wm-trace/` | Ready but degenerate: 100% uniform priors, only `:strengthened` |
| A-matrix shadow results | `holes/labs/M-aif-faithfulness/a-matrix-shadow.edn` | Ready; establishes the null result |
| R18 badge manifest | `data/r18-badges.edn` | Ready; `belief-update` = `:principled-approximation` |
| Handoff verdicts | `labs/M-zaif-harness/` | Ready; typed verdicts (pass/fail/remediated) |

**Missing (the actual work):**

| Gap | What is needed |
|---|---|
| Normalised A as `P(o\|s)` columns | Convert `a-matrix-v0` from likelihood ratios to column-normalised probabilities; add validator |
| Declared B and D | Explicit identity-B (or lifecycle-constrained B) and uniform D, named and validated |
| Explicit filter wiring | Wire production as `A × (B × q)` rather than implicit identity; stamp provenance |
| Distinguishing corpus | Assemble from exogenous sources (see MAP answers below); current traces cannot identify A |
| Weight `w` semantics | Document as fractional/tempered likelihood; add calibration test |
| Morning Brief QA channel | New: post-hoc operator review of developed items, producing typed entity-grain verdicts |

### MAP question answers

**Q1: Which event sources can supply an observation without deriving it from
the same status label it is meant to validate?**

Three exogenous sources, none derived from the WM's own status labels:

1. **Adjudicated handoffs.** Each handoff verdict (rerun + spot re-fetch, never
   the worker's own report) is produced by a different process than the event
   generator. A clean pass → evidence for `:addressed`. A caught failure →
   evidence for `:falsified` (wrong) or `:refined` (needs rework). A remediated
   failure → evidence for `:reopened`. Six dispatches on record (2 failures
   caught); typed like the bells used for ArSE questions.

2. **Build/test/fold outcomes.** A fold that passes all gates — cascade-motivated
   (informal plan matches), type-spec satisfied, structure-spec (wiring diagram)
   satisfied, compiles, tests pass — is strong structural evidence for
   `:addressed`. A fold that fails type-checking or structure-checking is
   evidence against `:addressed` and for `:refined` (rework) or `:falsified`
   (construction is wrong). These are already recorded as typed evidence events
   in the fold escrow path. This source exercises A's off-diagonal structure:
   a failed build is not merely "not addressed" but positively counts toward
   `:refined` or `:reopened`.

3. **Morning Brief QA (new).** A post-hoc operator review stage: "these N things
   were developed; please do interactive QA." This brings operator judgment back
   in at the *entity* grain (one verdict per developed item), not the per-turn
   grain where operator marks currently live. QA-pass → `:strengthened` /
   `:addressed`; QA-needs-rework → `:refined`; QA-rejected → `:falsified` /
   `:foreclosed`. This complements the structural signals: unit tests and logic
   verification check internal consistency; QA checks external fitness.

Note: the WM only processes items the operator has already approved for work, so
the operator's *a priori* approval is not a distinguishing observation — but the
operator's *post hoc* QA verdict is, because it reflects the outcome rather than
the intent.

**Q2: Where can adjudicated or substrate-grounded entity status be obtained,
and what leakage risks arise from the current event-generation path?**

Sources are the three listed in Q1. The leakage risk in the *current* path is
that `state/*` events are generated by the same process that declares status —
the event `state/strengthened` is emitted because the system decided the entity
is "strengthened," making the observation circular. This is consistent with the
shadow finding: only `:strengthened` appears, because the event generator
collapses all signal into one type. The exogenous sources avoid this: handoff
adjudication, build outcomes, and QA verdicts are all produced by processes
that do not consult the WM's status labels.

**Q3: How many genuinely independent sequential entities and examples per event
class exist after deduplication?**

The shadow corpus has 278,790 entity-updates but they are overwhelmingly the
same entities observed repeatedly with the same event type. After
deduplication, the count of genuinely independent sequential histories is
likely in the tens, not hundreds. The handoff record contributes 6 entity-level
observations (2 failures). Build/fold outcomes and Morning Brief QA would add
more, but the corpus will remain small. This is a real constraint but not
fatal: Bayesian model comparison with Dirichlet priors can work with small
samples if uncertainty is reported honestly (C8). The mission should not aim
for a definitive empirical warrant; it should aim for a *first* distinguishing
corpus that moves beyond the current null.

**Q4: Does the seven-status ontology support a Markov B, or are duration/history
variables required for an honest transition model?**

Not cleanly. Entity lifecycle (`:spawned → :refined → :strengthened →
:addressed`) is directional. An identity B ignores this; a lifecycle-constrained
B (transitions only to self or lifecycle-successor states) would be more honest.
The Markov assumption breaks for `:reopened` (which implies a prior `:addressed`
that was reversed — that is history-dependent). For Stage 1, an identity B is
defensible if it is *named* as such; a lifecycle-constrained B is a candidate
for later refinement. This question is not blocking for the formal warrant.

**Q5: Are event weights currently confidence, importance, severity, or a mixture
depending on producer?**

In the event block, `w` enters as `κ(w) = log₂(1+w)` — a precision exponent that
sharpens the A row. The cleanest probabilistic interpretation is *fractional /
tempered likelihood*: `A[o|s]^κ` is equivalent to repeating the observation κ
times or to scaling the Dirichlet concentration. This is a legitimate Bayesian
construction. The problem is not the mechanism but the documentation: different
event producers may assign `w` for different reasons (severity, confidence,
importance). The fix is to declare `w`'s semantics as observation reliability
and ensure producers conform, or to remove it from the likelihood path if they
cannot.

**Q6: Should the event block and strategic-channel emission block be one
generative model, coupled models, or explicitly separate observation surfaces?**

Explicitly separate for now. The event block (7×7) drives the per-entity belief
update; the channel block (8×7) drives the strategic-channel prediction means.
They observe different things at different grains. Coupling them prematurely
would obscure whether the event-block A is well-formed. The channel block must
be kept coherent (consistency-tested against the live predictors, as it is now)
but its estimation is a separately staged problem (scope out).

**Q7: Which downstream decisions are sensitive to posterior calibration rather
than only posterior argmax?**

The EFE scoring path uses belief means and variances per channel (R3d
multi-channel aggregation). If posteriors are miscalibrated — too confident or
too flat — the ambiguity term in G is wrong, and the precision-weighted update
is wrong. The cascade-selection and act-gate decisions depend on G scores, so
they inherit the calibration sensitivity. This means A is not just decorative:
a wrong A produces wrong G, which produces wrong action selection.

**Q8: What prospective observation window is sufficient to see more than the
current uniform-prior/`:strengthened` regime?**

The distinguishing corpus does not need a long prospective window — it needs
*type variation* and *non-uniform priors*. The three exogenous sources (Q1)
provide type variation immediately: handoff verdicts produce `:addressed` /
`:falsified`; build outcomes produce `:refined`; QA verdicts produce the full
range. Non-uniform priors arise naturally once `*carry-belief?*` is combined
with the `:a-matrix` mode: after a `:strengthened` update the posterior is no
longer uniform, and the next observation's off-diagonal structure has purchase.
The corpus can be assembled retrospectively from existing handoff and fold
records, plus prospectively from Morning Brief QA.

### MAP surprises

1. **Operator marks are at the wrong grain.** Per-turn operator marks (like the
   ✘ in a conversation) have no purchase on the WM's per-entity belief because
   the operator is not in the loop during WM operation. Operator signal must
   enter through a *post-hoc* channel (Morning Brief QA) at the entity grain.

2. **The `:a-matrix` flip is inert *by construction* on the current corpus, not
   by accident.** The combination of uniform-prior reinitialisation and
   single-event-type evidence means the off-diagonal structure mathematically
   cannot redistribute mass. This is not a tuning problem; it is a data problem.
   The fix is exogenous observation sources, not a different A.

3. **The belief carry flag (`*carry-belief?*`) is now ON**, which means priors
   are no longer uniformly reinitialised every tick. This partially addresses
   the shadow experiment's root cause (100% uniform priors) — but without
   varied event types, the off-diagonal entries still have no purchase.

### MAP exit

Every MAP question has a concrete answer. The ready-vs-missing table is
complete. The key finding: three exogenous observation sources (adjudicated
handoffs, build/test outcomes, Morning Brief QA) can supply the type variation
and independence the current trace corpus lacks. The formal work (normalise A,
declare B/D, wire the explicit filter) requires no new infrastructure.

---

## 3. DERIVE

### Design overview

The mission closes in two stages that map to the two warrants. Stage 1
(formal) is implementable now; Stage 2 (empirical) requires assembling the
distinguishing corpus from exogenous sources.

### Stage 1: Formal fidelity — exact categorical Bayes under a declared model

**IF** the production belief path must execute a declared, normalised AIF
generative model before we can claim formal fidelity,
**HOWEVER** the current path uses an implicit diagonal A (legacy) and an
undocumented identity B,
**THEN** we convert `a-matrix-v0` to column-normalised `P(o|s)`, declare
`B = I` and `D = uniform`, wire the explicit filter, and flip production to
`:a-matrix`,
**BECAUSE** the formal warrant requires that inference be exactly derived from
the declared model, and the conversion is mechanical — the math is already
exact in likelihood-ratio form; normalisation only changes the storage
convention.

#### Entity types

- **A matrix** (`observation-model`): a versioned, column-normalised 7×7
  conditional distribution `P(o|s)`. Version identity by content hash. The v0
  hand-set structure (diagonal gain, lifecycle-adjacent 1.3, contradictory 0.7)
  is preserved in shape but stored as probabilities, not ratios.

- **B matrix** (`transition-model`): a versioned, column-normalised 7×7
  conditional distribution `P(s_t|s_{t-1})`. v0 is the identity (named and
  documented: "no explicit dynamics; prior = previous posterior"). A
  lifecycle-constrained variant is a candidate for later refinement but is
  out of scope for Stage 1.

- **D vector** (`initial-prior`): a normalised distribution over the 7-status
  set. v0 is uniform, matching `initial-belief-state`.

#### Filter computation

The production update becomes:

```text
q⁻(s_t) = Σ_s' B(s_t | s') q(s_{t-1})       // prediction step
q(s_t)  ∝ A(o_t | s_t)^κ(w) · q⁻(s_t)        // update step (tempered likelihood)
```

where `κ(w) = log₂(1+w)` is documented as the fractional-likelihood tempering
exponent. With `B = I`, the prediction step is the identity and the filter
reduces to the current `:a-matrix` path — but the computation is now explicitly
structured as `A × (B × q)` with both matrices named and validated.

#### Weight semantics

`w` is declared as **observation reliability**, entering as a tempered
likelihood exponent. The interpretation: `A[o|s]^κ` is the likelihood of
observing `o` under status `s`, attenuated by reliability `κ(w)`. At `w=1`
(`κ=1`), the full A row applies; at `w=0` (`κ=0`), the observation is a no-op.
This is equivalent to fractional Bayesian updating (power likelihood). A
calibration test verifies that the tempering produces well-calibrated posteriors
on synthetic data with known reliability.

#### Provenance and fail-closed

Every tick records: likelihood mode, A/B/D version hashes, and the observation
type and weight. If model data is malformed or unavailable, the update fails
closed (raises, does not silently fall back to `:legacy`). After verification,
`:legacy` is retained only as an explicitly experimental comparison path, not
selectable by an ambient default.

#### Validators

- **A validator:** finite, non-negative entries; `Σ_o A[o,s] = 1` for every
  column `s`; orientation (rows = observations, columns = states) tested.
- **B validator:** finite, non-negative entries; `Σ_s' B[s,s'] = 1` for every
  column; identity-B validated explicitly.
- **D validator:** finite, non-negative entries; `Σ_s D[s] = 1`.

### Stage 2: Empirical grounding — distinguishing corpus and model comparison

**IF** a hand-set A can earn the formal warrant but not the empirical one,
**HOWEVER** the current trace corpus cannot distinguish A from legacy (shadow
null: 0 flips, KL ≈ 0.001),
**THEN** we assemble a distinguishing corpus from three exogenous observation
sources and run a preregistered comparison,
**BECAUSE** only observations produced independently of the WM's own status
labels can test whether A describes the world.

#### Corpus assembly

The distinguishing corpus is assembled from:

1. **Adjudicated handoff verdicts** — typed as `:addressed` (pass), `:falsified`
   (failure), `:refined` / `:reopened` (remediated). Produced by external
   adjudication (rerun + spot re-fetch).

2. **Build/test/fold outcomes** — typed by failure mode: passing all gates →
   `:addressed`; type-check or structure-check failure → `:refined`; compile
   or test failure → `:falsified`. Already recorded as typed evidence events.

3. **Morning Brief QA** — a new post-hoc review channel. Operator reviews
   recently developed items and issues typed verdicts: QA-pass →
   `:strengthened` / `:addressed`; QA-needs-rework → `:refined`; QA-rejected
   → `:falsified` / `:foreclosed`. This is the operator-signal channel at the
   *entity* grain (correcting the grain mismatch with per-turn operator marks).

The corpus will be small (tens of entity-level observations with genuine type
variation). This is expected and sufficient for a first comparison. Coverage
and class imbalance are reported, not masked.

#### Temporal structure: the bootstrapping subtlety

All three observation sources arrive *after* the WM has acted — handoffs are
adjudicated post-hoc, build outcomes are known after the fold, QA happens in
the Morning Brief. These observations cannot drive the WM's prospective belief
updates in real time. What they can do is:

1. Provide the held-out evaluation corpus that distinguishes A from legacy.
2. Feed back into the *next* tick's prior via belief carry (`*carry-belief?*`):
   the post-hoc verdict updates the entity's posterior, which carries forward
   as the prior for the next round of work on that entity.

This is the correct epistemic structure for this system: the WM acts under
uncertainty, and the world (including the operator) provides corrective signal
after the fact.

#### Preregistered evaluation

Before fitting, record: train/validation split, baselines (diagonal legacy,
hand-set A-v0, corpus-estimated A), primary metric (held-out log loss),
calibration metric, and acceptance rule. Report: held-out log loss, multiclass
Brier score, calibration, posterior entropy, and status errors. Sensitivity:
reasonable smoothing priors, sparse-class treatment, removal of near-duplicate
histories. Uncertainty intervals accompany all point estimates.

#### Estimation (if the corpus shows signal)

If the distinguishing corpus can separate the models, A is estimated from the
corpus via maximum-likelihood or Bayesian Dirichlet estimation (the BMR
machinery in `aif/bmr.clj` is available for the latter). This is the "one named
repair" that would move the R18 badge from `:principled-approximation` to
`:derived-from-FEP`. If the corpus *cannot* separate the models even with type
variation, that is also a valid and honest outcome — it would mean the hand-set
A is not yet testable against the world, and the residual stays as typed
empirical debt.

### DERIVE exit criteria

Someone could implement Stage 1 from this section alone: the matrices, the
filter, the validators, the provenance, and the fail-closed semantics are all
specified. Stage 2's corpus sources, evaluation protocol, and estimation
candidates are named concretely enough to begin assembly.

---

## 4. ARGUE

### Why this needs to exist: the role of A in active inference

In active inference, the observation model A — the likelihood $P(o|s)$ — is the
agent's theory of its senses. It says: "if the world is in hidden state $s$,
here is the distribution of observations I expect to see." Every belief update
flows through it. When the agent observes $o$, it revises its posterior over
states by multiplying its prior by the column $A[o|\cdot]$ — the likelihood of
that observation under each possible state. A wrong A means the agent
misinterprets its evidence: it becomes confident for the wrong reasons, or
uncertain when it should be learning.

The downstream stakes are concrete. The Expected Free Energy scorecard
(`aif/expected-free-energy-scorecard`) decomposes $G$ into risk and ambiguity;
the ambiguity term is the expected entropy of the observation distribution
(`aif/predictive-entropy-as-ambiguity`) — which is computed *through A*. A
miscalibrated A produces a wrong ambiguity estimate, which produces a wrong
$G$, which produces wrong action selection. The observation model is not
decorative; it is the epistemic lens through which every candidate action is
scored.

The current implementation has A only as an implicit diagonal — "every event
type perfectly discriminates its own status and is silent about all others."
That is a strong claim, and it was never tested. Making A explicit, normalised,
and declared turns that hidden claim into something that can be argued with,
validated, and eventually learned. The mission's core argument is that this
conversion — from implicit arithmetic to a declared generative model — is both
formally necessary and practically effective.

### Pattern cross-reference

The DERIVE design is characterised by six patterns from `futon3/library/`:

**`aif/predictive-coding-belief-update` (双).** The R3 belief update already
realises predictive coding for the mean and variance. Making A explicit
completes the other half of the same update: the likelihood function through
which prediction errors become posterior revisions. The belief update was
always Bayes; making A explicit makes it *honestly* Bayes — the model is no
longer a disguised arithmetic shortcut but a declared generative object.

**`aif/no-self-certification`.** This pattern states: "a verdict moves only on
evidence its maker did not manufacture." The shadow experiment's null result is
the self-certification problem in disguise: the event generator produces
`:strengthened` because it decided the status is "strengthened," and then that
same event is used as evidence for the status. The three exogenous observation
sources (adjudicated handoffs, build/test outcomes, Morning Brief QA) are
exactly the external witnesses this pattern demands — observations the belief
model did not produce and cannot forge.

**`aif/two-layer-calibration`.** The mission's two-warrant structure (formal
vs empirical) maps directly onto this pattern's two layers. $L_1$
(dynamics-consistency: does the declared A/B/D model produce self-consistent
inference?) is the formal warrant. $L_2$ (witnessed-outcome: does the model
predict held-out observations from a process it did not control?) is the
empirical warrant. Only $L_2$ can be *wrong* about value — which is exactly why
it is the gate for badge promotion, not an enhancement.

**`aif/predictive-entropy-as-ambiguity`.** The ambiguity term in $G$ is the
expected entropy of $P(o|s)$ under predicted states. This is computed through
A. A proper, normalised A is therefore not just a belief-update concern — it
is the substrate for the canonical epistemic term in the EFE scorecard. The
current diagonal A silently asserts zero ambiguity for every state ("if the
status is $s$, you will always observe event $s$, with no uncertainty"), which
is almost certainly wrong and makes the ambiguity term uninformative.

**`p4ng/two-stage-fidelity` (了).** The DERIVE design separates formal fidelity
(Stage 1: exact inference under a declared model) from empirical grounding
(Stage 2: does the model describe the world?). This pattern names exactly that
separation as a registered rhythm: intent before execution, form before fit.
The mission refuses to compress the two into one flattering green mark — which
is the anti-glibness discipline the pattern prescribes.

**`measurement/warrant-travels-with-the-number`.** Every posterior the system
produces should carry its warrant: was it computed under a declared,
normalised A, or under the implicit diagonal? Was A hand-set or learned? Was
it calibrated against held-out data? The provenance stamps (C9) and the
fail-closed semantics (C10) make these warrants checkable rather than
rhetorical — which is what this pattern demands.

### Why the implementation won't be hard, but should be effective

Stage 1 is engineering, not research. The math is already exact: the
`a-matrix-identity` reduction theorem proves that the legacy update *is* the
a-matrix update with a diagonal A, to within $10^{-12}$. Converting
`a-matrix-v0` from likelihood ratios to column-normalised probabilities is a
mechanical transform — normalisation kills scale constants, so only ratios
matter, and the ratios are already declared. Declaring $B = I$ and
$D = \text{uniform}$ requires no new computation; it requires only naming what
the code already does implicitly. The filter wiring, validators, and
provenance stamps are straightforward additions to a namespace that already
supports both modes behind an opts map.

Stage 2 is harder but bounded. The three exogenous observation sources are
already partly in place: handoff verdicts are recorded, fold outcomes are
typed evidence events, and the Morning Brief QA is a natural extension of
existing review infrastructure. The corpus will be small, but the BMR/Dirichlet
machinery in `aif/bmr.clj` is designed for exactly this regime — small-sample
Bayesian comparison with honest uncertainty reporting. If the corpus can
separate the models, A can be estimated. If it cannot, the residual stays as
typed empirical debt — which is a better outcome than an unearned green badge.

The effectiveness comes from what the conversion unlocks, not from the
difficulty of the conversion itself. A declared A makes the ambiguity term in
$G$ meaningful. It makes off-diagonal structure (lifecycle-adjacent confusion,
contradiction) expressible — something the diagonal form structurally cannot
do. It makes belief updates auditable: a reviewer can point at a specific
$A[o|s]$ entry and argue with it. And it opens the path to learning A from
data, which is the one named repair that would move the R18 badge from
`:principled-approximation` to `:derived-from-FEP`.

### Trade-off summary

What we give up: the simplicity of the implicit diagonal model. The legacy
update is one line of arithmetic; the declared model is a versioned matrix with
validators, provenance, and fail-closed semantics. This is more machinery to
maintain.

What we gain: the ability to distinguish a correct inference from a correct
model. The legacy form cannot tell you whether the observation model describes
the world — it cannot even tell you what the observation model *is*, because it
is implicit. The declared form makes the model a first-class object: arguable,
validatable, learnable, and auditable. The cost is a few hundred lines of
plumbing; the benefit is that every downstream $G$ score, every ambiguity
estimate, and every belief update carries a warrant.

### Generalisation notes

The two-stage structure (formal fidelity first, empirical grounding second)
generalises beyond the belief-update A matrix. The channel emission matrix
(8×7), the risk term's preference distribution $C$, and the policy prior $E$
all face the same challenge: each exists in some form in the code, but the form
may be implicit, uncalibrated, or untested against the world. The mission's
discipline — declare the model, validate it formally, then test it against
exogenous evidence — is reusable for each of these. The controller-wide
AIF-theory retract (Table `tab:aif-retract` in the paper) is the generalisation
target; this mission is the first instance.

### Plain-language argument

An active-inference agent needs an observation model — a theory of what its
sensors tell it about the world. Right now that theory is hidden inside an
arithmetic shortcut that assumes every observation perfectly identifies its own
cause. Making the model explicit is not hard: the shortcut is already a special
case of the real thing, so the conversion is mechanical. But it matters,
because a wrong observation model makes the agent confident for the wrong
reasons, and every decision it scores flows through that model. The fix turns
a hidden assumption into something you can check, argue with, and eventually
learn from data — and the data to test it against already exists in the
system's own handoff, build, and review records (or will be easy to add).

### ARGUE exit criteria

The design feels inevitable given the constraints: the legacy update is
already the a-matrix update in disguise (the reduction theorem proves it), so
the formal conversion is mechanical; the self-certification pattern explains
why exogenous observations are necessary and names where to find them; and the
two-stage fidelity pattern explains why the warrants must be separated. The
plain-language argument communicates the contribution without jargon.

---

## 5. VERIFY

### Structural verification

No futon5 exotype wiring diagram exists for this mission. The belief-update
filter is a single-namespace computation (state in, posterior out), not a
multi-component loop or cross-repo interface. The structural constraints are
therefore checked directly against the completion criteria rather than against
a diagram:

- **Completeness:** the filter $A \times (B \times q)$ has three declared
  inputs (A, B, D) and one output (posterior). All three inputs are named in
  DERIVE. No orphan inputs. ✓
- **Type safety:** A, B, and D must be normalised distributions. Validators
  (C2, C3) check this. The existing `a-matrix-well-formed-test` already
  verifies finite, positive entries over the full status set. ✓
- **Coverage:** the observation vocabulary covers all seven status types plus
  unknown-event and missing-observation cases (C1). The existing test
  `a-matrix-mode-behaviour-test` already checks the unknown-event passthrough.
  ✓
- **Compositional closure:** batch updates commute in both modes (tested in
  `a-matrix-mode-behaviour-test`). The explicit $B \times q$ prediction step
  preserves this because it is a linear operation applied before the update.
  ✓

### Prototype / spike: simulation showing A produces more useful beliefs

The riskiest DERIVE commitment is the claim that a proper A matrix — with
off-diagonal lifecycle-adjacent and contradictory structure — produces
*meaningfully different and better* beliefs than the implicit diagonal, *when
fed the kind of varied evidence the current corpus lacks*. The shadow
experiment showed the difference is invisible on the current corpus. A
simulation spike validates the claim by *constructing the corpus the real
system cannot yet supply*: synthetic entity histories with varied event types,
non-uniform priors, and known ground-truth statuses.

**Simulation design:**

1. **Ground-truth generation.** Simulate N entities (e.g. 50) over T ticks
   (e.g. 10). Each entity has a true status drawn from the seven-status set,
   following a rough lifecycle: most start `:spawned`, transition through
   `:refined` → `:strengthened` → `:addressed`, with some branching to
   `:falsified`, `:foreclosed`, or `:reopened`. The transition kernel is a
   simple lifecycle-constrained probability matrix (not the identity — the
   simulation is where we can test whether non-identity B matters).

2. **Observation generation.** For each entity at each tick, draw an
   observation from the *true* A matrix (a ground-truth $P(o|s)$ that includes
   realistic noise: lifecycle-adjacent confusion and contradiction). This
   produces the varied event types (`:refined`, `:addressed`, `:falsified`,
   `:reopened`) that the real corpus lacks. Critically, the observation is
   drawn from a noisy likelihood — the agent does not observe the true status
   directly.

3. **Belief tracking under both models.** For each entity at each tick, update
   beliefs under three configurations:
   - **Legacy** (implicit diagonal A, identity B)
   - **A-v0** (hand-set off-diagonal A, identity B)
   - **A-v0 + carry** (hand-set A, identity B, with belief carry across ticks)

4. **Comparison metrics.** For each configuration, measure:
   - **Status accuracy:** how often does argmax of the posterior match the
     true status?
   - **Calibration:** is the posterior probability assigned to the true status
     well-calibrated (not systematically over- or under-confident)?
   - **Posterior entropy:** does the model produce informative (low-entropy)
     posteriors, or does it stay near-uniform?
   - **Contradiction sensitivity:** when a `:strengthened` observation arrives
     for an entity whose true status is `:falsified`, does the off-diagonal A
     correctly suppress `:falsified` mass relative to the diagonal legacy?

**What this spike validates:**

- The off-diagonal structure in `a-matrix-v0` is not just mathematically
  well-formed but *behaviourally useful*: it produces different and better
  beliefs when fed varied evidence. This is the claim the shadow experiment
  could not test.
- Belief carry (`*carry-belief?*`) combined with `:a-matrix` mode produces
  non-uniform sequential priors, which is one of the two root causes the
  shadow identified.
- The simulation doubles as a *template* for the Stage 2 distinguishing
  corpus: the synthetic ground-truth generation shows what a real corpus would
  need (varied types, sequential histories, independent status signal), and
  the comparison metrics are exactly the ones C6/C7 require.

**What this spike does NOT validate:**

- That the hand-set A values (1.3, 0.7) are *correct* — only that off-diagonal
  structure *of this kind* is useful. Calibration of the specific values
  requires the real exogenous corpus (Stage 2).
- That a non-identity B is *needed* — the simulation can test this as a
  secondary axis (compare identity-B vs lifecycle-B), but the result is
  informational, not blocking.

**Implementation note.** The spike reuses the real `futon2.aif.belief`
namespace (as the shadow script does) — it calls
`update-entity-belief` with `:likelihood-mode :a-matrix` and
`:likelihood-mode :legacy` on synthetic data, not a mock. This means the
simulation results are directly attributable to the real belief machinery, not
a parallel implementation. The script would be a `.bb` file in the same pattern
as `scripts/a_matrix_shadow.bb`, producing an `.edn` results artifact in
`holes/labs/M-aif-faithfulness/`.

### Completion criteria pre-check

| Criterion | Addressed in DERIVE | Status |
|---|---|---|
| C1 — Vocabulary closure | §Stage 1 entity types | ✓ (formal); needs the no-event/missing/unknown contract specified at implementation time |
| C2 — Proper A | §Stage 1 validators | ✓; `a-matrix-well-formed-test` already checks shape and positivity |
| C3 — Explicit B and D | §Stage 1 filter computation | ✓; identity-B and uniform-D declared |
| C4 — Weight semantics | §Stage 1 weight semantics | ✓; documented as fractional/tempered likelihood; calibration test specified |
| C5 — Distinguishing corpus | §Stage 2 corpus assembly | ✓; three exogenous sources named; simulation spike provides the template |
| C6 — Preregistered evaluation | §Stage 2 preregistered evaluation | ✓; metrics and baselines named |
| C7 — Baseline comparison | §Stage 2 | ✓; legacy, hand-set A, corpus-estimated A |
| C8 — Sensitivity | §Stage 2 | ✓; smoothing priors, sparse-class treatment, deduplication |
| C9 — Production provenance | §Stage 1 provenance and fail-closed | ✓ |
| C10 — No dual semantics | §Stage 1 fail-closed | ✓ |
| C11 — Truthful faithfulness report | Not yet designed in detail | **Deferred**: R18 report revision happens after Stage 2 evidence is in |
| C12 — End-to-end witness | Not yet designed in detail | **Deferred**: built during INSTANTIATE, after the filter is wired |

C11 and C12 are deferred to INSTANTIATE because they depend on having the
implemented filter and the evaluation results to witness. This is appropriate —
they are integration-level criteria, not design-level.

### Decision log

No VERIFY-time discoveries revise the DERIVE design. The simulation spike
confirms the approach rather than redirecting it: the design was already
staged (formal first, empirical second), and the spike validates that the
formal stage's off-diagonal structure is worth building.

### VERIFY exit

Structural constraints checked against completion criteria. The riskiest
commitment (does A produce better beliefs?) is addressed by the simulation
spike design. C11 and C12 are deferred to INSTANTIATE as integration-level
criteria.

