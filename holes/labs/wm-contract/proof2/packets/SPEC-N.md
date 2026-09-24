# SPEC-N — consumed numbers and the real-valued model

2026-09-24. Discovery/specification only; no clause standing or independent
review claimed. Serves strategy row 1 and W0–W6. No runtime/Lean code, record,
configuration, or JVM changed; no click or Lean build run.

## 1. Revision and evidence boundary

Code citations below use these inspected revisions:

- **R** = futon2 `93531d415a115c7b426f37e06d7d5542ecb5dae4`;
  unprefixed `src/`, `scripts/`, `holes/` paths are relative to R.
- **L** = mathlib4 `77fdbda5b5629b3c8f6c7f9bbb027da0436ba1b3`;
  `WM/` means `DarkTower/WarMachine/`, `ML/` means `Mathlib/` at L.
- The governing specifications are PROOF-2 STRATEGY §4/§5 row 1, THEOREM
  W0–W6 including the F sentence, Correction and Review amendments, ASSUME
  A2/A7–A19/A21, and `proof2/packets/CERT-S.md` including its Correction.
  Their paths are under `holes/labs/wm-contract/`. Proposed changes below
  are amendments, not readings that silently change those drafts.

Read-only record inspection used:

| Record under `data/wm-runs/tick-run-record-<id>.edn` | Raw SHA-256 |
|---|---|
| `2026-09-23-1790131591` | `7314951f0ad6d339d561a9f7ec4f5dc14042873e4602873dddb590701ba61f25` |
| `2026-09-23-1790199409` | `7b3c56df1633bbdf1a0e32527f5778bbe45f2f883b6bb439647c693b813bd957` |

In both, `[:decision :selection-certificate :certificate-schema]` and
`[:decision :selection-certificate :model-inputs]` are absent. They are
pre-schema diagnostic evidence, not positive PROOF-2 clicks. Throughout,
`SC` abbreviates `[:decision :selection-certificate]`; `<id>` is CERT-S's
small candidate id, joined with the payload hash, never a positional substitute.

**Finding:** preserve exact equalities for the rational algebra, and replace
machine-to-transcendental equality by a kernel-checked refinement relation.
A finite double denotes an exact dyadic rational; that does not make it the
exact rational estimator or exact real logarithm intended by a model. Some
special cases *are* exact (`log 1 = 0`, a singleton posterior = 1); the rule is
not “doubles can never equal model values.” Each claimed equality needs proof.

The records exhibit both sides. In 1790199409:

- `SC :candidates 0 :id :precedence 0 :theta` is `3/4`, and precedence 1
  is `1/4`; `SC :candidates 1 :id :precedence 3 :theta` is `1/16`.
  These are exact rational inputs, each with `:theta-source :recorded-trials`
  at the same pattern map.
- `SC :candidates 0 :g` is `0.7324151971422708`, and candidate 1's `:g`
  is `1.9138870492238644`. Both candidates have `:f nil` and
  `:f-status :not-supplied` at those candidate paths.
- `[:decision :selection-law :posterior <payload-with-id-C1>]` is the
  double `0x1.87c9e98b73e5fp-1`; the C2 entry is
  `0x1.e0d859d23068bp-3`. Exact binary decoding gives their sum as
  **`1 + 7/36028797018963968`**, not 1. This arithmetic observation was
  checked read-only; it is not a Lean witness or a claim of causal error.

The older record has `SC :candidates 0 :habit = 1.0`, `:g =
0.5978370007556204`, and absent F as above; its singleton
`[:decision :selection-law :posterior]` has value 1.0. It cannot establish
multi-candidate numerical stability. Neither record supplies the proposed
numeric certificates or admits the missing F inputs.

## 2. Inventory of numerical obligations

**Rational-possible** means finite arithmetic over recorded exact rational
inputs, including division by a proved nonzero rational. It does not authorize
replacing a consumed rounded result by a later rational recomputation.
**Transcendental** means the general definition uses log/exp; symbolic
cancellation may reduce a particular instance to rational arithmetic.
**Discrete** equalities (ids, hashes, counts, Options, sets and enums) need exact
checking but no floating tolerance.

| W | Every numeric equality/comparison in scope | Class and witness rule | Definition / runtime evidence at the pinned revisions |
|---|---|---|---|
| W0 | `2 ≤ length`; theta bounds; normalized slots' theta equality; each emitted transition entry equals `patternKernel`/`cascadeKernel`; unequal rows for an effect-distinct pair | Count discrete; theta/rows rational-possible. Check each finite entry exactly; a rounded difference is not semantic non-equivalence. `interpret`/`firstEnabled` equalities themselves are discrete. | L `WM/CascadeTransition.lean:22`, `:42`, `:76`, `:102`, `:149`; R `src/futon2/aif/cascade_model_manifest.clj:293` rejects non-rational theta at `:303`. Paths: `SC :candidate-derivations <id>` emitted rows/normalized slots, plus candidate precedence theta. |
| W1 | Rate = estimator(counts, smoothing); consumed A entry = `tokenLikelihood`; nonnegative entries and sums = 1; predicted outcome = `predictedOutcome` | Rational-possible for a declared rational estimator and rational B/prior; finite products and sums. Estimator identity alone does not prove rationality: a quantile, fitted sigmoid or irrational smoothing parameter needs its own refinement. | L `WM/TokenObservation.lean:31`, `:39`, `:84`, `:146`; `WM/PolicyRollout.lean:114`. R `src/futon2/aif/cascade_model_manifest.clj:191`/`:209` and `:217`. Paths: `SC :model-inputs <id> :A`, decomposition `:terms :A`, predicted-outcome rows. |
| W2 | Predicted state, observation probability, `exactUpdate = some q`; pointwise quotient; sum q = 1; q differs from observed-facts point mass | Rational-possible, with positive evidence proved before division; none/zero-support branch exact. Nondegeneracy is an exact finite inequality, not “difference exceeds a display tolerance.” | L `WM/ExactBeliefTrajectory.lean:47`, `:51`, `:56`, `:76`; R `src/futon2/aif/exact_belief_core.clj:5`, `:8`, `:12`, `:29` admits only integer/ratio probabilities. Paths: `SC :model-inputs <id> :D` (`:sPrev`, `:o`, `:A`, `:B`, `:q`), token-belief input/stage joins. |
| W3 | Each F = VFE; exact-update F = −log evidence; prefix total = sum of step Fs; posterior = selectionPosterior; full-law vs F-ablation selected-action inequality | F/log and general posterior transcendental. Prefix indexing/summation structure is exact, but its summands are generally not rational. Two action-stability proofs plus distinct action ids are required for the intervention. | L `WM/PolicyVariationalFreeEnergy.lean:43`, `:296`; `WM/ExactBeliefTrajectory.lean:99`; `WM/PolicySelection.lean:24`, `:29`. R `src/futon2/aif/cascade_free_energy.clj:90`–`:103` accumulates rational evidence then casts before `Math/log`; `src/futon2/aif/cascade_selection.clj:109`–`:118` scores and exponentiates doubles. Paths: `SC :model-inputs <id> :F`, its `:f-prefix`, `SC :candidates <i> :f`, `:policies <i> :f`, decomposition `:terms :F`, `[:decision :selection-law]`. |
| W4 | Cascade kernel → predicted state → predicted outcome → exact-update posterior → next stored/consumed belief; equality with `exactBeliefAt`/`tokenBeliefAt` | Rational-possible on the same exact-rational A/B/beliefs; identity and temporal joins remain discrete. If an approximate belief is actually stored, an exact rational *shadow* trajectory does not establish this equality. | L `WM/PolicyRollout.lean:38`, `:114`; `WM/ExactBeliefTrajectory.lean:177`, `:230`. Paths: `SC :Q-link`, its prior/posterior hashes, and the joined `:model-inputs <id> :D` in both clicks. |
| W5 | Concentration array = `accumulate`; each cell = prior + Σ outcome×belief; theta = normalized concentration; resulting B row = kernel; duplicate contributes zero; same latest version; old/new-B Bayes actions differ | Concentrations, normalization and kernels rational-possible; versions/dedup discrete; downstream G/posterior/action comparison transcendental in general. Positive rational priors and nonnegative rational trials preserve positive concentrations. | L `WM/DirichletLearning.lean:19`, `:50`, `:60`, `:82`; `WM/CascadeTransition.lean:42`. R `src/futon2/aif/learning_trial_ledger.clj:273`–`:286` returns `(/ (+ successes 1/2) (+ n 1))`. Paths: close `[:b-update]`, `SC :model-inputs <id> :B`, candidate theta, `SC :B-read :predecessor-chain`. |
| W6 | All inherited equalities; E and beta/gamma; C authority → utility → normalized C; risk/ambiguity/horizon G; final posterior and action marginal | E/count normalization and gamma=1/beta rational-possible. Utility is rational-possible for rational scales; general exponential C, log C, KL risk, entropy, G and softmax require refinement. Summing *ideal* posterior masses is real arithmetic; summing *decoded recorded* masses is rational arithmetic, but the two are not automatically equal. | L `WM/TokenPreference.lean:48`, `:52`, `:56`, `:76`; `WM/PolicyHorizon.lean:33`, `:50`, `:61`; `WM/PolicyPrecision.lean:25`; `WM/ActionMarginal.lean:21`, `:26`. R `src/futon2/aif/cascade_model_manifest.clj:514`, `:529`, `:544`, `:651`, `:710`; `src/futon2/aif/cascade_selection.clj:47`, `:109`, `:147`. Paths: `SC :scoring <i> :c`, candidate `:habit`, model inputs/decomposition, selection-law `:beta`, `:gamma`, `:posterior`, `:action-marginal`. |

No log or exp is needed for the finite Bayes quotient in W2. Conversely,
recording C's input weights as ratios does not make its exponential normalizer
rational. A model with likelihood tempering also needs the actual versioned
formula: the current sparse scorer documents A^zeta normalization at
R `src/futon2/aif/cascade_model_manifest.clj:1024`; a nontrivial real power
cannot inherit the untempered rational classification without a reduction proof.

For W5, the two-outcome/singleton-state construction yields exact posterior
concentrations `(s+1/2, n−s+1/2)` and theta `(s+1/2)/(n+1)`. That is the
arithmetic specialization to prove. It does **not** prove that a whole-attempt
measurement is a per-pattern transition trial or that a singleton state is the
categorical token belief. A13's semantic/identity obligations remain separate.

## 3. Exact carrier and consumption rule

Use CERT-S §1/§3's integers/ratios and `#wm/double "<hex>"` without changing
any existing candidate joins or top-level paths. For rationals, require signed
integer numerator, positive denominator, reduction to coprime form, and no
bounded-integer overflow in the extractor. `0` has denominator 1. Cast into
Lean ℝ only after proving the rational calculation; casts preserve +, × and
division under the proved denominator condition.

For a finite binary64 value with sign b, exponent field E and fraction M:

```
normal:    decode = (−1)^b (2^52 + M) 2^(E−1023−52),  1 ≤ E ≤ 2046
subnormal: decode = (−1)^b M 2^(−1074),               E = 0
```

These are exact rationals. Equivalently parse the canonical hexadecimal
significand into integer N with k fractional hex digits and exponent e:
`decode = sign × N × 2^(e−4k)`. NUM-R/L must verify binary64 representability,
normal/subnormal ranges and canonical grammar, not accept arbitrary hex real
literals. Preserve the sign of zero in the raw identity even though both zeros
map to rational zero. Refuse NaN and infinities in the finite decoder. A
mathematical infinite-risk branch is a separately typed value with a support
proof, not an IEEE overflow reinterpreted as EReal.top.

Current precedent for bit-preserving identity is R
`src/futon2/aif/action_identity.clj:26`–`:47`: finite doubles use
`Double/toHexString`, and fixed printer settings are at `:14`. Its tagged-tree
hash at `:62` is **not** CERT-S's proposed tagged-EDN hash codec; NUM-R must
implement the specified codec, not assume existing digests interchangeable.
A decimal display is not by itself proof of lost bits: a round-tripping
parser can preserve a double. But treating the displayed decimal as an exact
*decimal rational* changes its mathematical value. Hex removes that ambiguity;
old records need an explicit verified decoding rule and remain pre-schema.

The exact obligation for a rational-computable term is:

```
recorded output's decoded rational = rationalFormula(recorded exact inputs)
```

This is a checked equality, not a definition of the left side. E.g. a recorded
binary approximation to 1/3 must fail equality to 1/3; NUM-R must not
“rationalize” it into 1/3. Decoding all three entries of a purported uniform
three-way binary vector does not make their sum equal 1. Do not silently
renormalize it, clamp it, or substitute a recomputed exact vector. Where the
runtime really consumes integers/ratios, as the current exact conditioning core
requires (R `src/futon2/aif/exact_belief_core.clj:5`–`:10`), direct equality is
available. If a rational-capable computation actually used floating arithmetic
and equality fails, either the producer must later consume exact values or the
reviewed theorem must explicitly extend refinement to that term. This packet
does not grant an exact shadow permission under A16.

## 4. Numeric refinement: a theorem about the value, not a claimed tolerance

For each finite transcendental value, let d be the exact rational decoding of
the recorded consumed double, X the intended real expression on the recorded
model inputs, and l,u,e rational witnesses. Require a Lean proof of:

```
l ≤ X ∧ X ≤ u
0 ≤ e ∧ d−e ≤ l ∧ u ≤ d+e
```

This implies `|X − d| ≤ e`; proving only `l ≤ d ≤ u` proves nothing about X.
Each e must also satisfy a **preregistered**, versioned per-term error budget.
Otherwise a huge interval would make numerical correspondence vacuous. No
universal `1e-12` is licensed here. Input rounding, log/exp evaluation, arithmetic
and accumulation errors must fit the total budget, not just the last library
call. Exact normalization of the ideal distribution and a proved bound on the
recorded normalization residual are different claims.

An offline extractor may compute candidate rational endpoints/proof terms from
immutable inputs; Lean checks them. This is mathematical checking, not filling
a missing empirical field after selection. A2/A16 still require the consumed
double, all model inputs, identity and consumption joins to have been retained.
For a bound on one concrete result we need not axiomatize the JVM Math library:
prove the real-expression interval and its distance to the *recorded result*
directly. This establishes finite-instance refinement, not global correctness
or correctly-rounded behavior of `Math/log`/`Math/exp` for all inputs.

### 4.1 Existing exact analytic dependencies

These are source-verified at L, not guessed lemma names or newly compiled proofs:

| Available declaration | Source and role |
|---|---|
| `Real.exp_bound` | `ML/Analysis/Complex/Exponential.lean:513`: for `|x| ≤ 1`, `0<n`, `|exp x − Σ[m<n] x^m/m!| ≤ |x|^n × ((n+1)/(n!×n))`. Rational x makes polynomial and remainder rational. |
| `Real.exp_bound'` | Same file `:519`: one-sided nonnegative-x upper bound; useful but the two-sided lemma above is sufficient. |
| `Real.exp_add`, `Real.exp_nat_mul`, `Real.exp_neg` | Same file `:207`, `:229`, `:236`: range reduction/reconstruction. For rational x choose natural k with `|x/2^k|≤1`, bound exp there, then square k times. Propagate positive endpoint bounds exactly. |
| `Real.exp_pos`, `Real.exp_lt_exp`, `Real.exp_le_exp` | Same file `:280`, `:309`, `:313`: positivity and monotone transport of score/input intervals. |
| `Real.log_le_iff_le_exp`, `Real.le_log_iff_exp_le` | `ML/Analysis/SpecialFunctions/Log/Basic.lean:160`, `:164`: for x>0, prove log x ≤ u by x ≤ exp u, and l ≤ log x by exp l ≤ x. |
| `Real.log_lt_iff_lt_exp`, `Real.lt_log_iff_exp_lt` | Same file `:162`, `:166`: strict versions, including the concrete falsifier below. |
| `Real.exp_log`, `Real.log_exp`, `Real.log_le_log_iff` | Same file `:58`, `:74`, `:146`: cancellations and propagation for positive log arguments. |
| `le_div_iff₀`, `div_le_iff₀`, `div_le_div₀` | `ML/Algebra/Order/GroupWithZero/Unbundled/Basic.lean:1123`, `:1127`, `:1274`: positive-denominator quotient inequalities. Finite sums/products and rational side conditions can then be proved algebraically. |
| `PolicySelection.selectionPosterior_finite`, `selectionPosterior_all_finite` | `WM/PolicySelection.lean:63`, `:82`: reductions to normalized exponential weights, subject to their hypotheses. |
| `ActionMarginal.actionMarginal`, `IsBayesAction` | `WM/ActionMarginal.lean:21`, `:26`: finite grouped sums and maximum predicate; not an existing numerical-error theorem. |

To bound log of rational x>0, choose rational l,u; certify an *upper* exp(l)
bound ≤x and a *lower* exp(u) bound ≥x. The two iff lemmas give the log
interval. This avoids inventing a log-series theorem. For an input interval
0<xL≤x≤xU, use the corresponding endpoints and positivity. If the interval
straddles zero, the logarithm is not certified; refine the input or report a
held numerical obligation. Do not add an epsilon floor.

The generic range-reduction wrapper, its rational certificate checker and the
lift from these endpoints to the model expressions remain **owed**, even though
the analytic leaves already exist. The specific proposed modules
`WM/Proof2/NumberEncoding.lean`, `LogRefinement.lean`,
`SelectionRefinement.lean` do not exist at L (no `WM/Proof2/` directory).
This is not a claim that mathlib lacks generic analysis or order lemmas.

### 4.2 F, C and G composition

The finite VFE formula is a support sum of q times log q minus log(lik×prior),
not a rational total merely because q is rational (L
`WM/PolicyVariationalFreeEnergy.lean:43`). First prove its exact support branch.
When q is the exact update, use `exactUpdate_minimises_vfe` with all of its
normalization/positivity hypotheses (L `WM/ExactBeliefTrajectory.lean:99`–`:105`)
to reduce the real expression to −log Z. Otherwise certify the finite support
sum directly; a claimed posterior may not borrow the equality case.

For an admitted prefix, bind each step's A/B/prior/o/q and certify each Fi;
then add endpoint bounds for **every** admitted step. A recorded floating total
must be checked separately: decoding the total is not necessarily the exact
sum of decoded per-step doubles. Its accumulation discrepancy can be evaluated
as rational arithmetic and included in the budget. SPEC-F must settle the
prefix definition; using only the final step does not solve this numeric issue.

For C, preserve the full outcome space and ruled zeros. Bound exp utilities,
the full normalizer and each division, or prove the exact closed-form reduction
before bounding it (L `WM/TokenPreference.lean:52`–`:57`). For G, bound each
finite KL and ambiguity sum and its horizon total (L
`WM/PolicyHorizon.lean:33`–`:63`). Exact support decides zero terms and infinite
risk *before* evaluating logs. No tolerance may turn a declared zero into a
positive preference. Current sparse/factorized optimizations are not licensed
merely by close numerical tests: the runtime's own description notes ~1e-13
agreement and preconditions at R `src/futon2/aif/cascade_model_manifest.clj:1014`;
the concrete bound must refer to the same semantic G expression and inputs.

### 4.3 Softmax and action stability

For finite policy i define `zi = log Ei − Fi − gamma Gi`. Ei>0 and beta>0
are proof obligations. Bound zi by [li,ui] after composing the F/G/E/precision
bounds. Do not use an ideal gamma reciprocal while ignoring a consumed rounded
gamma, or assume floating `G/beta` equals rounded `gamma*G`; the current scorer
uses division at R `src/futon2/aif/cascade_selection.clj:113`.

Obtain rational positive bounds `ai ≤ exp zi ≤ bi`. With `A=Σ ai>0`, `B=Σ bi`,

```
ai/B ≤ idealPosterior_i ≤ bi/A.
```

Sharper denominator-dependent bounds may be added later. Check each recorded
posterior double against the ideal interval and its budget; derive the ideal
sum=1 from the normalization definition. A scalar residual test on the floating
vector is not a proof of equality to softmax. Mathematical infinite-G policies
get exact zero only after the appropriate support proof
(L `WM/PolicySelection.lean:34`); hardware overflow is not that proof.

For an action a, aggregate **all** its policies. Since the softmax denominator
is common and positive, define unnormalized action bounds
`La=Σ[i:action(i)=a] ai`, `Ua=Σ[i:action(i)=a] bi`. A sufficient stability test is:

```
for every b != recordedAction:  U_b < L_recordedAction.
```

This proves a unique maximizer of ideal action mass. Alternatively sum posterior
intervals. Prove the recorded action and actual projection/tie-rule identity
join to this maximizer. A strict policy-score gap alone suffices only when the
policy→action map is injective (compare L
`WM/ActionMarginal.lean:62`, `bayesAction_of_injective`); many policies can own
one action. The runtime sums posterior doubles starting at 0.0 and compares
those sums at R `src/futon2/aif/cascade_selection.clj:147`–`:161`: if checking
that arithmetic from component masses, account for sum rounding as well.
Do not equate a decoded recorded action mass with the unrounded rational sum
without checking it.

Overlapping action intervals leave uniqueness **unproved**. Refine bounds or
retain `:action-stability-unresolved`; this is a proof status, not an instruction
to halt ordinary operation. An exactly proved real tie can use the declared
name order, but overlapping intervals do not establish a tie. `IsBayesAction`
permits ties; the runtime deterministic tie rule needs an additional finite
order proposition. Prove stability independently in both W3 intervention arms
and both W5 old/new-B arms, then prove the two certified action ids differ.
Only the named term/model changes; all other inputs obey A17's byte joins.
No search beyond preregistered L to obtain a larger gap (R7).

## 5. Certificate amendment and bounded successors

Keep CERT-S's paths and id+payload joins. Proposed schema revision (not a v1
reinterpretation): add at `SC :numeric-refinement` a versioned collection of
entries keyed by **full value key path**, each also carrying candidate id and
payload hash where applicable. Entries bind:

- source value/content hash and consumed-at identity from the existing term;
- encoding kind, original hex or integer/ratio, and verified exact decoding;
- semantic expression id/version and hashes of every leaf input;
- exact-rational equality **or** finite rational interval, error bound and
  preregistered budget/version; mathematical top is a separate support case;
- generated Lean declaration/module identity, proof result and axiom audit.

For example an F entry points to `SC :model-inputs :C1 :F`, and separately joins
`SC :candidates <C1-position> :f`, `SC :policies <C1-position> :f` and its
F decomposition occurrence. Selection entries point to
`[:decision :selection-law :posterior <candidate-payload>]` and action-marginal
values, retaining the actual historical map-key shape until a versioned
producer changes it. Do not create a second, positional id universe.

**Required correction to CERT-S §1:** “exact-rational prefix-sum dual-carrier”
cannot mean the mathematical F total. Use machine-double total + symbolic
prefix expression/inputs + rational bounds. A sum of decoded step doubles is
an optional rational arithmetic diagnostic, not the exact VFE. Likewise an
exact-decoded double is a rational *value*, but only a successful equality
check makes it a rational-model witness. These are value-form/refinement-rule
changes, so CERT-S §5 requires a new certificate version, proposed
`:wm/proof2-certificate-v2`; do not edit historical v1/pre-schema records.
Offline proof artifacts may bind the immutable record by hash; they may not
backfill missing empirical/consumption inputs.

Bounded successor statements, all owed rather than silently assumed:

1. **NUM-R/L:** total finite binary64/ratio decoder with canonicality,
   signed-zero identity and nonfinite refusal; prove value preservation and
   exact cast/arithmetic transport. Swapping numerator/denominator must fail.
2. **NUM-T-D/L:** a rational exp-bound certificate for |x|≤1 using
   `Real.exp_bound`; then positive range-reduction/repeated-squaring transport;
   then the log-interval wrapper via the two named iff lemmas. These are
   separate bounded lemmas if one packet is insufficient.
3. **Composition:** sound interval +/−/×/positive-division and finite-sum
   rules, support handling, prefix-F aggregation, and C/G expression wrappers.
   Specify each semantic expression, not only a generic epsilon checker.
4. **NUM-S-L:** normalized exponential quotient bounds, grouped-action bounds,
   strict-gap→unique `IsBayesAction`, and exact-tie/name-order handling. Add
   concrete consumed-result error checking; none of the existing selection
   equalities establishes those interval hypotheses.
5. **Extraction:** bind each proof leaf/result to CERT-S hashes and consumption
   loci. A syntactically correct proof about a different decoded input fails P.

## 6. What the symbolic-log precedent establishes

L `WM/MachinePolicyFreeEnergyWitness.lean:6`–`:8` explicitly separates symbolic
logs from floating deltas. `positiveVarianceContribution` (`:18`) leaves
`Real.log (2 * Real.pi * (1/4))` symbolic; `machineTwoChannelTotal` (`:55`)
proves the exact two-channel symbolic expression; temperature fixtures (`:64`,
`:69`) prove rational algebra exactly. Its model is the Gaussian residual/
variance expression in `WM/MachinePolicyFreeEnergy.lean:45`–`:63`, not the
categorical VFE of `WM/PolicyVariationalFreeEnergy.lean:43`.

R `holes/labs/wm-contract/f8_policy_free_energy_readback.clj:29`–`:42` defines
1e-12 and evaluates a transcribed symbolic expression using `Math/log` and
`Math/PI`; `:90` announces floating deltas and `:112` checks the tolerance.
That is a useful separation of obligations and a regression comparison. It
provides no Lean bound on the JVM log/pi evaluations, no proof that a recorded
double equals that real expression, no categorical observed-prefix F witness,
and no stability theorem for an action near a tie. Similar names do not make
these two F definitions interchangeable. This packet did not execute that
readback (its purpose here is source precedent, not a positive record).

## 7. Concrete falsifiers, unblocked work, and proposed theorem amendments

**Exact strategy-row falsifier: “A rounded log presented as exact ℝ equality.”**
Use decoded double `d = #wm/double "0x1.62e42fefa39efp-1"`, i.e.
`6243314768165359/9007199254740992`, and claim `(d:ℝ)=Real.log 2`.
Take exact rationals
`l=69314718055994530/10^17`, `u=69314718055994532/10^17`.
Then d<l. Applying `Real.exp_bound` with n=24 at l and u reduces
`exp l < 2 < exp u` to rational inequalities:

```
P24(l) + R24(l) < 2 < P24(u) − R24(u)
P24(x) = Σ[m<24] x^m/m!
R24(x) = |x|^24 × 25/(24!×24).
```

Those rational inequalities and d<l were checked read-only using exact fraction
arithmetic during this discovery. The named strict log/exp iff lemmas give
`d < l < log 2 < u`, refuting the equality. This is a concrete proof design,
**not an already compiled Lean negative witness**; NUM-T-L must compile it and
reviewers independently fix X before positive evaluation (A18). An emitter's
“within 1e-12” annotation cannot make the equality true.

**Additional required falsifier: a near-tie winner flips inside certified error.**
Two policies map to distinct actions; equal positive habits, gamma=1, G=0.
Recorded scores are `za=−1+3/4096`, `zb=−1+1/4096`, so the recorded winner is a.
Suppose each score has certified error bound 1/2048. The permitted exact scores
`za*=−1+1/4096`, `zb*=−1+3/4096` reverse the winner by strict monotonicity of exp.
All numbers defining the scores/errors are dyadic and can be checked exactly;
F is positive (`−z`) on both possibilities. A checker accepting a unique action
merely because the point estimate wins, or calling the overlapping intervals a
tie and applying name order, must fail. This falsifies the *inference of action
stability*, even when the individual numerical error bounds are valid.

**Unblocks:** NUM-R, NUM-L, NUM-T-D, bounded analytic leaves for NUM-T-L,
NUM-S-L, and amended honest W statements/CERT-S carriers. It does not discharge
W0–W6, invent measured A, supply missing F prefixes, or authorize a proof click.

**Missing definitions — proposed amendments, not operator questions:**

1. Define `DecodeExact` and `RefinesWithin` as above and preregister per-term
   error budgets plus a separate action-stability requirement. Replace each
   generally false machine-double=real-expression assertion in W3/W6 (and the
   W5 selection comparison) by the proved refinement; retain literal equality
   for W0/W1/W2/W4/W5 rational subterms. Exact model identities remain exact.
2. Define the full admitted-prefix F sum and its per-step posterior inputs
   jointly with SPEC-F; amend CERT-S's purported rational mathematical F total.
   F=0 ablation must be named as an intervention retaining G, not “habit-alone”
   or a fabricated historical F. The A12/A17 discrepancy still needs SPEC-F's
   reviewed intervention rule; numeric bounds cannot resolve it.
3. Define strict action stability, exact-tie handling, numerical unresolved
   standing, and mathematical-top versus overflow. Mathematical failure or
   insufficient precision stays an open CHECK under R8, never a proof by
   emitter tolerance or a runtime veto introduced by this packet.
4. State explicitly that decoded floating normalization residuals are bounded
   separately from exact normalization of the ideal carrier. Missing consumed
   inputs or failed exact rational equalities may not be repaired by offline
   reconstruction or approximate equality without another reviewed amendment.
