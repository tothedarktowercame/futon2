# Audit: Lean bindings of the AIF equation registry — 2026-09-16

Auditor: codex-10, independent and read-only (no edits, commits, dispatches, Lean builds or production probes). Commissioned by claude-4 for Joe, who checked each batch's quotations and counterexamples as it came in. Scope: all 18 rows of `aif-equations.edn`, each judged against the equation the row cites, never against production behaviour. Counterexamples were derived from the definitions and have not been compiled as Lean fixtures.

Pins: mathlib4 `darktower` at `f40c936a64227ba81592a71f3d937e6fbdbf0e4c`; registry at futon2 `6802fa87e777` (A1) and `ab0b56f223a8` (A2–A4); the registry file itself is unchanged between them. Rows without a `:lean-at` pin were compared against mathlib4 `d22ddd13857c` (the registry's 2026-09-01 as-of date).

Verdict vocabulary: MATCHES / MATCHES-WITH-UNDECLARED-REDUCTION / DIVERGES / CANNOT-TELL.

Related, different scope: `TN-lean-audit-2026-09-15.md` (census of WarMachine declarations and paper citations; not per-equation fidelity).

## Summary

| Row | Registry status (before) | Verdict | Finding | Section |
|---|---|---|---|---|
| :observe | closed | CANNOT-TELL | No world/action relation; constructor packages supplied values only | A2 §1 |
| :prediction-error | closed | DIVERGES | Refuses on missing variance; admits negative variance | A2 §2 |
| :precision | closed | DIVERGES | Regularised windowed estimate + floor/cap ≠ 1/max(Var, ε₀) | A2 §3 |
| :free-energy | closed | MATCHES | Exact declared reduction of Buckley eq. 45 | A1 §1 |
| :policy-free-energy | closed | DIVERGES | Zero-variance tolerance branch scores an impossible observation 0 | A3 §1 |
| :belief-update | closed | DIVERGES | Tempered categorical reweighting, not μ + αΠε; admits negative masses | A2 §4 |
| :belief-state | closed | DIVERGES | Posterior type unconstrained; reconciliation drops carried beliefs | A2 §5 |
| :forward-model | carrier-only | CANNOT-TELL | Carrier accurate; rollout from μ, A, B, T unstated | A3 §2 |
| :risk | closed | MATCHES-WITH-UNDECLARED-REDUCTION | Exact finite KL; requires positive preference on all listed outcomes | A3 §3 |
| :ambiguity | closed | MATCHES | Exact Da Costa eqs. 45–48; imports should be Q(s\|π), not Q(o\|π) | A3 §4 |
| :expected-free-energy | closed | DIVERGES | Ambiguity supplied as an arbitrary function | A3 §5 |
| :policy-set | closed | MATCHES | Limited stack-defined claim: set of supplied candidates | A4 §1 |
| :depth | closed | DIVERGES | Independent per-term depths instead of one horizon | A4 §2 |
| :temperature | closed | DIVERGES | Three laws, only the β branch matches; admits zero/negative τ | A4 §3 |
| :policy-posterior | closed | DIVERGES | `exp`/`log` are arbitrary function arguments | A1 §2 |
| :action | closed | DIVERGES | Head selection; no policy-to-action mass aggregation | A4 §4 |
| :dirichlet-accumulation | carrier-only | CANNOT-TELL | Carrier accurate; no accumulation law or common dimension | A4 §5 |
| :model-reduction | closed | DIVERGES | `List.zip` silently truncates unequal-length vectors | A1 §3 |

Totals: 3 MATCHES, 1 MATCHES-WITH-UNDECLARED-REDUCTION, 11 DIVERGES, 3 CANNOT-TELL. No divergence is post-pin drift; the one repair since 2026-09-01 (`480a666ad2`, ProbabilityKernel) moved definitions towards the equations.

## Citation errors found

- :prediction-error — error terms are defined in Buckley eqs. 46–47, not 45.
- :belief-update — one fixed step of Buckley eq. 59 does not give μ + αΠε (generalised-coordinate flow and dynamical-prior terms must also be removed).
- :policy-posterior — the habit term is Friston 2016 eq. 7, not eq. 6.
- :expected-free-energy — eq. 42 uses state risk; outcome risk also needs Da Costa eq. 44.
- :depth — Da Costa eq. 42 has no sum over τ (repeated in the MachineDepth.lean header).
- :temperature — γ = 1/β is stated after Friston 2017 eq. 2.7, not in eq. 2.1.

---

# A1 — free-energy, policy-posterior, model-reduction

Agency job `invoke-1789575388787-21546-420ad337`. Report text verbatim.

Read-only audit complete. **The changes move toward the cited equations, but the posterior and BMR declarations still admit inputs for which they do not state those equations.** I found no evidence in the inspected change messages or adjacent comments that these changes replaced AIF mathematics with production behaviour.

Audit pins:

- Lean: `mathlib4` branch `darktower`, `f40c936a64227ba81592a71f3d937e6fbdbf0e4c`.
- Registry: `futon2` `6802fa87e777ac61ff45f40f967603de530d7e9d`. Its HEAD advanced during inspection; the registry and source index remained unchanged.
- Baseline: `d22ddd13857c40d88b6ce81fe21915c22ea20ffe`.

Below, `registry` means `futon2/holes/labs/wm-contract/aif-equations.edn`; Lean filenames are under `mathlib4/DarkTower/WarMachine/`. Current quotations use the pins above unless another commit is specified. Retrieved PDFs/text are **uncommitted**, so they have no source commit SHA. Their PDF checksums match the committed index, `refs/README.md:12–18`. I inspected the local sources, including PDF images where extraction lost notation.

## 1. `:free-energy` → `variationalFreeEnergy`

**a. Registry and cited equation**

At `registry:92–94`:

```clojure
:ref :buckley2017
:formal "F = 1/2 * mean_k (Pi_k eps_k^2)"
:eq "buckley2017 eq. 45 (reduction: ½ ln σ terms dropped; mean over channels instead of sum)"
```

The reference entry additionally declares:

> “single channel per error, sample variance with a floor, mean over channels, one fixed-step update.”

— `registry:29`, futon2 `6802fa87e777`.

Buckley equation (45) contains two sums over components and dynamical orders, with terms
\[
\frac{\epsilon_z^2}{2\sigma_z}+\frac12\ln\sigma_z,
\qquad
\frac{\epsilon_w^2}{2\sigma_w}+\frac12\ln\sigma_w.
\]
Source: `refs/buckley2017.txt:1114–1137`, PDF p. 28, checksum prefix `44da91a454474f8a`. It is Laplace-encoded energy approximating variational free energy. Removing the logarithmic terms and taking the declared channel mean gives the registry formula.

**b. Current and baseline declarations**

Current, [`Holes.lean:7094–7097`](/home/joe/code/mathlib4/DarkTower/WarMachine/Holes.lean:7094), mathlib4 `f40c936a6422`:

```lean
def variationalFreeEnergy (precision : PrecisionMap) (error : Channel → ℝ) : VariationalFreeEnergyValue :=
  ⟨(1 / 2 : ℝ) *
    ((Channel.all.map fun k => (precision k).value * (error k) ^ 2).foldl (· + ·) 0 /
      Channel.all.length)⟩
```

Baseline, `Holes.lean:6273–6276`, mathlib4 `d22ddd1385`, retrieved with `git show`:

```lean
def variationalFreeEnergy (precision error : Channel → ℝ) : VariationalFreeEnergyValue :=
  ⟨(1 / 2 : ℝ) *
    ((Channel.all.map fun k => precision k * (error k) ^ 2).foldl (· + ·) 0 /
      Channel.all.length)⟩
```

**c. Commits changing the declaration**

Exactly **one** of the 25 file-touching commits changes this declaration. I checked declaration text against each commit’s first parent, as well as `git log -L`.

- `480a666ad27c18477b4b9df86e11862be3930d50`: changes precision from arbitrary real weights to `PrecisionMap`, reading each weight through `.value`. The arithmetic is unchanged. `PrecisionMap` is `Channel → NonnegativeReal` (`Holes.lean:7086` at this commit).

The added nonnegativity theorem is separate from the declaration.

**d. Verdict: MATCHES**

The declaration computes exactly the registry’s explicitly reduced formula over the fourteen channels enumerated in `Holes.lean:1369–1379`.

This verdict concerns that reduction, not equality with unreduced Buckley (45). The function does not establish that supplied precision values are inverse Gaussian variances or that supplied errors arise from a generative model. Those are input meanings, not computations performed here. Zero precision is admitted; a proper finite-variance Gaussian has strictly positive precision.

**e. Direction and motivation**

**Toward the cited equation:** negative precision was previously admitted; the change excludes it without changing results on nonnegative inputs.

The adjacent comment says:

> “Precision is a nonnegative channel-indexed multiplicative weight.”

— `Holes.lean:7085`, mathlib4 `480a666ad2`.

The commit subject is “Enforce finite-support probability kernels and nonnegative VFE precision” (`480a666ad2`, commit metadata). Neither states a production-agreement motivation. The probability-kernel changes in that commit do not enter this formula.

## 2. `:policy-posterior` → `softmaxWithFPi`

**a. Registry and cited equation**

At `registry:177–179`:

```clojure
:ref :parr2022
:also-refs [:dacosta2020 :friston2017 :friston2016]
:formal "Q(pi) := softmax(ln E(pi) - G(pi)/tau - F_pi(pi))"
:eq "parr2022 eq. B.9 (π = σ(ln E − F − G), p. 247) with γ = 1/τ from friston2017 eq. 2.1 / dacosta2020 A.2. E's meaning: friston2016 eq. 6 (habit as policy concentration parameters)"
```

Parr B.9 reads:

\[
\nabla_\pi F=0\iff \pi=\sigma(\ln E-F-G).
\]

Source: `refs/parr2022.txt:12659–12664`, printed p. 247, PDF checksum prefix `f1ddb2efaf4f86f9`. The preceding text identifies the vector entries as policy-conditioned \(F_\pi\). Da Costa A.2 explicitly introduces \(\sigma(-\gamma G)\): `refs/dacosta2020.txt:1262–1271`. Friston (2017) equation (2.1) likewise contains this policy prior: `refs/friston2017.txt:329–339`.

**Citation discrepancy:** the retrieved Friston (2016) PDF’s equation **(6)** concerns expected free energy and likelihood parameters. Its equation **(7)** contains the policy/habit updates. I checked PDF p. 7, printed p. 868. The habit-concentration discussion is at `refs/friston2016.raw.txt:1076–1087`. Thus the registry’s specific equation-(6) attribution is inaccurate; Parr B.9 independently supplies the posterior formula.

**b. Current declaration**

[`PolicyPosterior.lean:8–15`](/home/joe/code/mathlib4/DarkTower/WarMachine/PolicyPosterior.lean:8), mathlib4 `f40c936a6422`:

```lean
def softmaxWithFPi {PolicyIndex : Type*} (exp log : ℝ → ℝ)
    (habit : PolicyIndex → ℝ)
    (grade : PolicyIndex → DarkTower.WarMachine.Holes.ExpectedFreeEnergyValue)
    (fPi : PolicyIndex → ℝ) (tau : ℝ) (policies : List PolicyIndex) : List ℝ :=
  let weights := policies.map fun π =>
    exp (log (habit π) - (grade π).value / tau - fPi π)
  let total := weights.foldl (· + ·) 0
  weights.map fun weight => weight / total
```

**c. History**

The declaration first appears in `Holes.lean:7232–7238` at `bcdd2ee406a5031c6d2be4760bcaab57a35d7366`.

- `ea5bb437e8cd6c917c94b519bff683377fbb33a8`: relocates it to `PolicyPosterior.lean`, qualifying imported names.
- `b3f9b56ef29d0de784782c86dc89f046f0e699c9`: adds `noncomputable section`.
- `780de57fc3023cacc2b487d5b34948820d466b04`: closes that section.

The latter three changes do not change the formula. The current registry **explicitly records** the rebinding and these origins at `registry:181`; the old top-level date should not be read as evidence that the rebinding was unrecorded.

**d. Verdict: DIVERGES**

**The score expression matches; the declaration as typed does not require a softmax.**

`exp` and `log` are unconstrained function arguments, not `Real.exp` and `Real.log`. For example, a caller may supply `exp := fun _ => 0`. For any nonempty policy list, every resulting weight is zero, including after division by the zero total under Lean’s real arithmetic. That is not a categorical posterior.

The declaration also admits nonpositive habit values, zero/negative temperature, and empty or duplicate policy lists. No hypotheses establish the intended policy support or probability semantics.

With actual exponential/logarithm, positive habit and temperature, and a nonempty enumeration of distinct policies, it computes the registry equation exactly. Those conditions are absent from the binding. This is broader than an undeclared mathematical reduction: permitted inputs can produce something other than the claimed posterior.

**e. Direction and motivation**

- Adding the unscaled `−F_pi` term: **toward** Parr B.9, correcting the earlier binding’s omission.
- Relocation and section fixes: **neither**; semantically unchanged.
- The unconstrained function arguments were already present in the old `softmax` (`Holes.lean:7240–7245` at the audit pin); the new declaration inherits that defect.

The adjacent comment includes:

> “evidence: Row 16 R6 production-trace witness”

and:

> “falsifier: the declared `F_π` term is omitted”

— `PolicyPosterior.lean:7`, mathlib4 `f40c936a6422`; originally `Holes.lean:7231`, `bcdd2ee406`.

This names production evidence but does **not** say the mathematics was changed to accommodate runtime behaviour. The changed term is demanded by the cited equation.

## 3. `:model-reduction` → `modelReductionDecision`

**a. Registry and cited equation**

At `registry:201–205`:

```clojure
:ref :friston2018bmr
:formal "Delta F := ln P(y|full) - ln P(y|reduced) = ln B(A) + ln B(a') - ln B(a) - ln B(A') over Dirichlet concentrations a (prior), A (posterior), a' (reduced prior); a reduction is accepted when Delta F <= -3"
:eq "friston2018bmr eq. 9 and Table 1 (Dirichlet), sign reversed: see :references :friston2018bmr :sign-convention"
:threshold-ref :kass1995
```

The reduction note says:

> “Hence 'accept when ΔF_WM ≤ −3' means the reduced model wins by ≥ 3 nats.”

— `registry:51`, futon2 `6802fa87e777`.

Equation (9) derives reduced-model evidence using the posterior expectation of the reduced/full prior ratio. Table 1 gives, in the registry’s notation,
\[
A'=A+a'-a,\qquad
\Delta F_{\rm paper}=\ln B(a)-\ln B(a')+\ln B(A')-\ln B(A).
\]

Sources: `refs/friston2018bmr.txt:270–280,425–428`; I checked the notation visually in PDF Table 1, p. 10, checksum prefix `4d55b4997dfcc28a`. Negating this expression gives the registry’s sign exactly.

Kass–Raftery’s scale puts \(2\ln BF\) between 6 and 10 in the strong-evidence category. I checked scanned PDF p. 5, printed p. 777, checksum prefix `08ac5292c9595325`. This supports the declared three-nat threshold, not a universal BMR acceptance rule.

**b. Current declaration**

[`ModelReduction.lean:34–44`](/home/joe/code/mathlib4/DarkTower/WarMachine/ModelReduction.lean:34), mathlib4 `f40c936a6422`:

```lean
def modelReductionDecision
    (a A aPrime APrime : DirichletConcentrations)
    (hAPrime : APrime.val = bayesianModelReduction A.val aPrime.val a.val) :
    ModelReductionDecision where
  fullPrior := a
  fullPosterior := A
  reducedPrior := aPrime
  reducedPosterior := APrime
  evidenceChange := modelReductionFreeEnergyChange A aPrime a APrime
  accepted := bayesFactorThreshold (modelReductionFreeEnergyChange A aPrime a APrime)
  reducedEquation := hAPrime
```

Its relevant referenced definitions are:

```lean
def bayesianModelReduction (A aPrime a : List ℝ) : List ℝ :=
  (A.zip (aPrime.zip a)).map fun x => x.1 + x.2.1 - x.2.2
```

— `Holes.lean:7248–7249`, mathlib4 `f40c936a6422`.

`modelReductionFreeEnergyChange` implements the four signed log-beta terms exactly (`Holes.lean:7230–7234`); `logMultivariateBeta` uses the log-gamma normalizer (`:7219–7222`); acceptance is `change.value ≤ -3` (`:7237`).

**c. History**

`9fd88669f5cc5ef57eb422de930cf5276f19294c` introduces the module and composite declaration. No subsequent commit changes this file before the audit pin. The rebinding from the threshold-only declaration is explicitly recorded at `registry:206`.

**d. Verdict: DIVERGES**

**The signs, normalizer and threshold match. The componentwise domain does not.**

`DirichletConcentrations` requires only a nonempty list of positive reals (`Holes.lean:7216`). Nothing requires the four vectors to share a dimension. `List.zip` silently truncates to the shortest input; the supplied equation proof does not prevent this.

A concrete source-level counterexample is:

- `a = [1,1]`
- `A = [4,4]`
- `aPrime = [1]`
- `APrime = [4]`

All four concentration types are satisfied, and the zipped equation holds. The declaration then computes
\[
\Delta F=\ln B(4,4)=-\ln 140<-3,
\]
so acceptance holds. But it compares a two-coordinate Dirichlet with a one-coordinate Dirichlet, not the common-coordinate prior replacement in the cited derivation.

This counterexample is an algebraic deduction from the source, **not a newly compiled Lean fixture**. With equal-dimensional, aligned inputs and positive reduced posterior, the declaration matches the registry formula. Its current type does not enforce that domain.

**e. Direction and motivation**

The composite binding moves **toward** the cited equation: it adds the posterior relation and evidence calculation to the previous threshold-only binding. It inherits the truncating helper; it does not introduce or repair that defect.

The module says:

> “The registry's model-reduction sentence has three parts.”

— `ModelReduction.lean:6`, mathlib4 `9fd88669`.

The nearby comment explicitly requires positivity of the componentwise posterior (`:31–33`). Neither the commit message nor these comments states that the equation was changed to agree with production. The later identity fixture does mention a recorded fixture (`:74–75`), but that does not explain away the missing common-dimension condition.

No production-conformance proofs or other registry rows were audited. No files were changed, no commits made, and no dispatches sent.

| Row | Verdict | Reason |
|---|---|---|
| `:free-energy` | **MATCHES** | Exact declared reduced formula; change strengthens precision’s domain. |
| `:policy-posterior` | **DIVERGES** | Correct score, but arbitrary `exp`/`log` permit non-posteriors. |
| `:model-reduction` | **DIVERGES** | Correct evidence algebra, but unequal dimensions silently truncate. |

---

# A2 — observe, prediction-error, precision, belief-update, belief-state

Agency job `invoke-1789576636449-21580-76bf8318`. Report text verbatim.

**Batch 1 complete.** Four bindings diverge from the registry’s written quantity or its declared domain. The observation binding establishes a representation, but not the world/action relationship needed to assess its boundary claim.

No edits, commits, dispatches, production probes, or Lean builds were performed.

**Audit references.** Registry quotations below are from `futon2` commit `ab0b56f223a83083ec9b3c43a720a90112dd0d42`, file `holes/labs/wm-contract/aif-equations.edn`, abbreviated **R**. Lean quotations are from `mathlib4` commit `f40c936a64227ba81592a71f3d937e6fbdbf0e4c`, under `DarkTower/WarMachine/`, abbreviated **M**.

All five complete module files—not merely their named declarations—are byte-identical to their respective registry pins. Relevant imported `Channel`, `Channel.all`, `ObservationVector`, and `predictionError` declarations also match the observation/prediction-error pins. Mathlib sources and the Lean toolchain have not changed since the earliest pin, `e2e8ee9649`.

Retrieved sources are uncommitted and therefore have no commit SHA. Their PDF hashes match `refs/README.md:12,15` at R: Buckley `44da91a454474f8a…`; Friston 2017 `c0f3c5f090f21e1b…`. Source citations below identify those files separately.

## 1. `:observe` → `machineObservation`

**a. Registry and source**

R, `aif-equations.edn:74–76`:

```clojure
:class :stack-defined :ref :friston2017
:imports [:world]
:formal "o_t <- structured observation of the world after action u_{t-1}"
```

There is **no `:eq` field**. The note explicitly says:

> “THIS ROW IS :class :stack-defined AND ITS :formal LINE IS A BOUNDARY, NOT A FORMULA”

— R, `aif-equations.edn:78`.

There is consequently no uniquely cited equation to quote. For context only, the retrieved Friston source represents observations through
\[
P(o_t\mid s_t)=\operatorname{Cat}(A)
\]
and action-dependent transitions through
\[
P(s_{t+1}\mid s_t,\pi)=\operatorname{Cat}(B(u=\pi(t))).
\]
These occur in equation (2.1), `refs/friston2017.txt:323–334`. They do not specify this stack’s fourteen-channel construction.

**b. Declaration, dependencies, history**

M, [`MachineObservation.lean:58–61`](/home/joe/code/mathlib4/DarkTower/WarMachine/MachineObservation.lean:58):

```lean
def machineObservation (values : Channel → ℝ)
    (variants : Channel → MeasurementVariant) :
    ObservationVector × ObservationEnvelope :=
  (⟨values⟩, envelopeOf values variants)
```

Relevant dependencies, M, `MachineObservation.lean:42–52`:

```lean
inductive MeasurementVariant | observed | absent deriving DecidableEq, Repr

structure ObservationEnvelope where
  variant : Channel → MeasurementVariant
  value : Channel → ℝ

def envelopeOf (values : Channel → ℝ) (variants : Channel → MeasurementVariant) :
    ObservationEnvelope := ⟨variants, values⟩
```

M, `Holes.lean:7077–7078`:

```lean
structure ObservationVector where
  value : Channel → ℝ
```

`Channel` has fourteen constructors (`Holes.lean:1369–1379`). Thus the value function is total over a fixed, nonempty coordinate set.

The module and these relevant dependencies are unchanged from `a4c2276d515730a32db27f97f1fd955a9bae1433`.

**c. Domain assessment**

The constructor guarantees that the vector and envelope contain identical numeric coordinates. It accepts arbitrary real values and independently supplied absence tags.

For example, constant value `5` with every variant `.absent` is admitted. But that is **not by itself a counterexample to the cited AIF observation boundary**: neither the formal line nor the cited source imposes the stack’s purported `[0,1]` range or zero-on-absence convention.

What is missing is a world state, observation operation, action/time index, or predicate relating the supplied values to the world after the preceding action. The definition cannot distinguish a measurement from fabricated coordinates.

**d. Verdict: CANNOT-TELL**

The representation is established; the boundary claim is not. No specific source equation or Lean relation supplies the missing world/action semantics. Calling the constructor a correct observation process would exceed what it states.

**e. Citation and imports**

The reference is broad rather than demonstrably wrong. There is no equation number to verify.

`:imports [:world]` does **not** match the actual inputs: already-computed channel values and measurement variants. No `world` input or mapping from it is represented. The action/time relationship in `:formal` is also absent.

## 2. `:prediction-error` → `machineChannelPredictionError`

**a. Registry and source**

R, `aif-equations.edn:80–82`:

```clojure
:ref :buckley2017
:imports [:o :mu]
:formal "eps_k := o_k - mu_k"
:eq "buckley2017 eq. 45 error terms (reduction: no generalised coordinates)"
```

The note acknowledges:

> “THE SUBTRACTED TERM IS NOT mu. It is :predicted-mean”

— R, `aif-equations.edn:83`.

It also declares the present/absent/refused result and says equality with the formal expression requires the predicted mean to equal the belief mean.

Buckley (45) contains squared sensory and dynamical errors. Their definitions immediately follow:
\[
\epsilon^\alpha_{z[n]}=\phi^\alpha_{[n]}-g^\alpha_{[n]},\qquad
\epsilon^\alpha_{w[n]}=\mu^\alpha_{[n+1]}-f^\alpha_{[n]}.
\]
These are **equations (46) and (47)**, `refs/buckley2017.txt:1128–1134`, PDF p. 28.

**b. Declaration, dependencies, history**

M, [`MachinePredictionError.lean:160–165`](/home/joe/code/mathlib4/DarkTower/WarMachine/MachinePredictionError.lean:160):

```lean
noncomputable def machineChannelPredictionError (minVariance : ℝ)
    (o : Field) (p : Prediction) : Outcome :=
  match p.mean, p.variance, o with
  | .value m, .value v, .value ob => .present (presentRecord minVariance ob m v)
  | .value _, .value _, .missing => .absent Member.observed
  | _, _, _ => .refused (offences o p)
```

Input types, M, `MachinePredictionError.lean:81–90`:

```lean
inductive Field where
  | missing
  | notFinite
  | value (x : ℝ)

structure Prediction where
  mean : Field
  variance : Field
```

Output types, M, `MachinePredictionError.lean:129–142`:

```lean
structure PresentRecord where
  observed : ℝ
  predictedMean : ℝ
  predictedVariance : ℝ
  error : ℝ
  perCallPrecision : ℝ
  weightedError : ℝ

inductive Outcome where
  | present (r : PresentRecord)
  | absent (m : Member)
  | refused (offending : List Offence)
```

Arithmetic, M, `MachinePredictionError.lean:146–153`:

```lean
noncomputable def presentRecord (minVariance observed predictedMean predictedVariance : ℝ) :
    PresentRecord :=
  { observed := observed
    predictedMean := predictedMean
    predictedVariance := predictedVariance
    error := observed - predictedMean
    perCallPrecision := 1 / max predictedVariance minVariance
    weightedError := (observed - predictedMean) * (1 / max predictedVariance minVariance) }
```

Refusal construction concatenates offences from the mean, variance and observation (`:106–124`); a missing variance contributes `Offence.missing Member.variance`.

All these definitions are unchanged from `1282b75e3223d3f94d536ebabbb5b4125989722d`.

**c. Domain assessment and counterexamples**

The all-value branch **always subtracts correctly**. There is no sign or subtraction error.

However, consider:

```text
minVariance = 1/100
o = Field.value 3
p.mean = Field.value 1
p.variance = Field.missing
```

The result is `refused [missing variance]`, although the registry’s scalar expression has all its operands and equals `3 − 1 = 2`. The extra variance requirement changes the domain of the written error calculation.

The richer result also lacks basic variance/precision constraints. With `minVariance = -1`, `observed = 3`, `predictedMean = 1`, and `predictedVariance = -2`, it returns a present record with error `2`, precision `-1`, and weighted error `-2`. No proper Gaussian variance interpretation supports that record.

Finally, `p.mean` is unconstrained by a belief or prediction map. I could not establish its identification with the registry’s `mu`.

**d. Verdict: DIVERGES**

The full declaration is a typed producer with extra gating and extra quantities, not the scalar function declared by the row. Its present error component matches conditionally. The registry note documents this discrepancy; documentation does not make the two functions equal.

**e. Citation and imports**

Equation (45) contains the errors but does not define them; (46)/(47) are the precise references. Dropping generalised coordinates alone also does not turn a general observation map \(g(\mu)\) into \(\mu\): identity observation mapping, or redefining `mu` as predicted observation, is additionally needed.

`:imports [:o :mu]` is incomplete for this binding. It takes observation, predicted mean, predicted variance and a minimum variance. The relationship from stored `mu` to predicted mean is not part of the declaration.

## 3. `:precision` → `machinePrecision`

**a. Registry and source**

R, `aif-equations.edn:86–88`:

```clojure
:ref :buckley2017
:imports [:eps :eps0]
:formal "Pi_k := 1 / max(Var(eps_k), eps0)"
:eq nil
```

The row’s note explicitly distinguishes its estimate from the source:

> “In buckley2017 eq. 45 the variances σ are model parameters, not sample statistics”

— R, `aif-equations.edn:91`.

Its Lean note records the additional floor/cap clamp and regularized error variance (`:89`).

The source’s explicit precision definition is equation (84):
\[
\Lambda_{z[n]}^{\alpha(i)}=1/\sigma_{z[n]}^{\alpha(i)},\qquad
\Lambda_{w[n]}^{\alpha(i)}=1/\sigma_{w[n]}^{\alpha(i)}.
\]
Source: `refs/buckley2017.txt:2096–2101`. The reference entry points to (84), R `aif-equations.edn:28`. Neither a rolling estimator nor these clamps follows from that definition.

**b. Declaration, dependencies, history**

M, [`MachinePrecision.lean:101–102`](/home/joe/code/mathlib4/DarkTower/WarMachine/MachinePrecision.lean:101):

```lean
noncomputable def machinePrecision (p : PrecisionParameters) (history : List ℝ) : ℝ :=
  min (max (varianceComponent p history) p.floor) p.cap
```

M, `MachinePrecision.lean:63–75`:

```lean
structure PrecisionParameters where
  windowSize : Nat
  minVariance : ℝ
  priorVariance : ℝ
  priorStrength : ℝ
  floor : ℝ
  cap : ℝ
  windowPositive : 0 < windowSize
  minVariancePositive : 0 < minVariance
  priorVariancePositive : 0 < priorVariance
  priorStrengthPositive : 0 < priorStrength
  floorPositive : 0 < floor
  floorLeCap : floor ≤ cap
```

M, `MachinePrecision.lean:82–96`, three declarations:

```lean
def windowOf (n : Nat) (history : List ℝ) : List ℝ :=
  history.drop (history.length - n)

noncomputable def regularizedErrorVariance (p : PrecisionParameters) (window : List ℝ) : ℝ :=
  (p.priorStrength * p.priorVariance + (window.map (fun e => e ^ 2)).sum)
    / (p.priorStrength + window.length)

noncomputable def varianceComponent (p : PrecisionParameters) (history : List ℝ) : ℝ :=
  1 / max (regularizedErrorVariance p (windowOf p.windowSize history)) p.minVariance
```

All are unchanged from `e2e8ee9649908a5564da6237b7d8def80a4bbaf5`.

**c. Domain assessment and counterexamples**

Positivity is well constrained. An empty history is admitted but does not divide by zero: its variance estimate is `priorVariance`. There is no zip truncation or arbitrary replacement for a fixed analytic function.

The discrepancy is mathematical:

- Use `windowSize=20`, `minVariance=1/100`, `priorVariance=priorStrength=1`, `floor=1/10`, `cap=200`—the declared `defaults` at `MachinePrecision.lean:202–214`.
- For twenty errors equal to `10`, the regularized estimate is `667/7`; its reciprocal is `7/667`.
- `machinePrecision` instead returns `1/10`.

This remains a counterexample even if the registry’s `Var` is generously interpreted as the Lean regularized estimate. Existing source theorems state these exact values and inequality: `MachinePrecisionWitness.lean:72–90`, M. I inspected their statements rather than rerunning their proofs.

There is a second distinction: the estimate uses uncentred squared errors plus a prior, not conventional centred variance. Twenty zero errors give Lean precision `21`, whereas the written formula with centred variance zero and `eps0=1/100` gives `100`.

**d. Verdict: DIVERGES**

The binding computes a different estimator followed by an additional clamp. These differences are already disclosed, but are not equality under the written formal expression.

**e. Citation and imports**

`:eq nil` accurately avoids attributing the full stack formula to a numbered source equation. Buckley supports inverse model variance; it does not establish this estimator. The row itself acknowledges that its content is stack-defined despite its theory-defined classification.

`:imports [:eps :eps0]` captures error history and minimum variance only partially. The result additionally depends on window size, variance prior, prior strength, floor and cap.

## 4. `:belief-update` → `machineBeliefUpdate`

**a. Registry and source**

R, `aif-equations.edn:113–115`:

```clojure
:ref :buckley2017
:imports [:mu :Pi :eps :alpha]
:formal "mu <- mu + alpha Pi eps"
:eq "buckley2017 eq. 59 (reduction: one gradient step with fixed step size alpha)"
```

The note acknowledges:

> “THE :formal LINE ABOVE IS NOT THE MAP THE MACHINE RUNS”

— R, `aif-equations.edn:116`.

It describes instead a categorical Bayesian filter tempered by \(\kappa(w)=\log_2(1+w)\), with identity transition.

Buckley (59), visually checked in PDF p. 34, states:
\[
\begin{aligned}
\dot\mu&=\mu'-\kappa_a\left[-\epsilon_{z[0]}/\sigma_{z[0]}+\epsilon_{w[0]}/\sigma_{w[0]}\right],\\
\dot\mu'&=\mu''-\kappa_a\left[-\epsilon_{z[1]}/\sigma_{z[1]}+\epsilon_{w[0]}/\sigma_{w[0]}+\epsilon_{w[1]}/\sigma_{w[1]}\right],\\
\dot\mu''&=-\kappa_a\,\epsilon_{w[1]}/\sigma_{w[1]}.
\end{aligned}
\]
Text location: `refs/buckley2017.txt:1387–1401`.

**b. Declaration, dependencies, history**

M, [`MachineBeliefUpdate.lean:181–186`](/home/joe/code/mathlib4/DarkTower/WarMachine/MachineBeliefUpdate.lean:181):

```lean
noncomputable def machineBeliefUpdate {n : Nat} [NeZero n]
    (eventWeight entityCount totalInconsistency health : ℝ)
    (kind : BeliefEventType) (likelihood prior : Fin n → ℝ) : Fin n → ℝ :=
  categoricalUpdate
    (kappa (attributedWeight eventWeight entityCount totalInconsistency health kind))
    likelihood prior
```

The relevant helper chain is, M, `MachineBeliefUpdate.lean:51–68,73–81`:

```lean
def inconsistency (kind : BeliefEventType) (health : ℝ) : ℝ :=
  match kind with
  | .strengthened => 1 - health
  | .foreclosed => health

noncomputable def attributionNorm (eventWeight entityCount totalInconsistency : ℝ) : ℝ :=
  if 0 < totalInconsistency then
    eventWeight * entityCount / totalInconsistency
  else 0

noncomputable def attributedWeight (eventWeight entityCount totalInconsistency health : ℝ)
    (kind : BeliefEventType) : ℝ :=
  inconsistency kind health *
    attributionNorm eventWeight entityCount totalInconsistency

noncomputable def normalise {n : Nat} [NeZero n] (q : Fin n → ℝ) : Fin n → ℝ :=
  let total := ∑ i, q i
  if total = 0 then fun _ => 1 / n else fun i => q i / total

noncomputable def temperedLikelihood (kappa likelihood : ℝ) : ℝ :=
  if kappa = 0 then 1 else likelihood ^ kappa

noncomputable def categoricalUpdate {n : Nat} [NeZero n]
    (kappa : ℝ) (likelihood prior : Fin n → ℝ) : Fin n → ℝ :=
  normalise fun i => temperedLikelihood kappa (likelihood i) * prior i

noncomputable def kappa (weight : ℝ) : ℝ :=
  Real.log (1 + weight) / Real.log 2
```

All are unchanged from `0e89cc1cb5622b252b43ced54dbfa65959043a5b`.

**c. Domain assessment and counterexamples**

This is multiplicative likelihood reweighting followed by normalization, not additive gradient descent.

For `n=2`, zero event weight, unit entity count and total inconsistency, zero health, `.strengthened`, likelihood `[1,1]`, and prior `[1/2,1/2]`, the output remains `[1/2,1/2]`. An additive coordinate update with `alpha=1/10`, `Pi=2`, `eps=1` gives `7/10`. These latter operands are not arguments of the binding, and no relation forces its event parameters to encode them. The source also states this contrast in `registryAdditiveFormIsNotGeneral`, `MachineBeliefUpdate.lean:158–165`.

There is an independent categorical-domain defect. Use the same zero-weight parameters, likelihood `[1,1]`, and prior `[-1,2]`. Its sum is one, so the result is exactly `[-1,2]`: a negative posterior mass. All inputs are admitted.

`[NeZero n]` correctly excludes an empty state space, and shared `Fin n` prevents unequal lengths. However, neither prior nor likelihood has positivity constraints. Event parameters also have unrestricted real domains, with no guarantee that `1 + attributedWeight` is positive. `Real.log` and real power are fixed functions here, not arbitrary function arguments.

**d. Verdict: DIVERGES**

The binding differs from the specified additive update even before its missing probability constraints are considered. The registry’s declared categorical interpretation also fails on admitted inputs.

**e. Citation and imports**

Equation (59) is a worked-model system with generalised-coordinate flow and dynamical-prior errors. Taking one fixed-size step does **not alone** yield `mu + alpha Pi eps`: additional removal or restriction of those terms is necessary. Equations (48)–(50) provide the more general gradient-descent context (`refs/buckley2017.txt:1173–1210` and following).

`:imports [:mu :Pi :eps :alpha]` does not describe this function’s arguments. `prior` can represent a categorical belief, but there are no direct precision, error or step-size inputs. Instead it takes event/attribution parameters, event kind and likelihood. The upstream driver helpers elsewhere in the module are not called by `machineBeliefUpdate`.

## 5. `:belief-state` → `machineBeliefState`

**a. Registry and source**

R, `aif-equations.edn:119–124`:

```clojure
:class :stack-defined :ref :buckley2017
:imports [:mu-next]
:formal "mu_t := the stored belief after the update at t-1"
:lean "machineBeliefState"
```

There is **no `:eq` field**. The note identifies the intended representation as:

> “a map entity-id -> normalised categorical over a SEVEN-member status set”

and explicitly says:

> “THE :formal LINE IS FALSE AS WRITTEN”

— R, `aif-equations.edn:125`.

It describes reconciliation of the preceding posterior with a fresh entity domain. No specific Buckley equation is cited for this stack storage operation; I cannot attribute one to it.

**b. Declaration, dependencies, history**

M, [`MachineBeliefState.lean:16–32`](/home/joe/code/mathlib4/DarkTower/WarMachine/MachineBeliefState.lean:16):

```lean
inductive Status
  | spawned | refined | strengthened | addressed | falsified | foreclosed | reopened
  deriving DecidableEq, Repr

def Status.all : List Status :=
  [.spawned, .refined, .strengthened, .addressed, .falsified, .foreclosed, .reopened]

abbrev Entity := Nat
abbrev Posterior := Status → ℝ
abbrev machineBeliefState := Entity → Option Posterior

noncomputable def uniformPrior : Posterior := fun _ => 1 / 7

def reconcileBeliefCarry (fresh carried : machineBeliefState) : machineBeliefState :=
  fun entity => match fresh entity with
    | none => none
    | some prior => some ((carried entity).getD prior)
```

The separately defined constraint is only, M, `MachineBeliefState.lean:61–62`:

```lean
def Normalised (posterior : Posterior) : Prop :=
  (Status.all.map posterior).sum = 1
```

It is not part of `Posterior` or `machineBeliefState`. The preservation theorem assumes normalized inputs (`:71–74`).

All are unchanged from `3a8e26f61e2ef3f4e96c92c96d39743442133db9`.

**c. Domain assessment and counterexamples**

The named binding is a **type abbreviation**, not a storage or recurrence equation. It contains no update input or time relationship.

It also admits non-beliefs. For example, entity `0` can carry a function assigning `-1` to `.spawned`, `2` to `.refined`, and zero elsewhere. This even satisfies the module’s `Normalised` predicate, while violating categorical nonnegativity. An all-zero posterior is likewise admitted by the type.

For the temporal claim, use valid distributions:

```text
carried 0 = some uniformPrior
fresh e = none for every entity e
```

Then `reconcileBeliefCarry fresh carried 0 = none`, whereas the preceding stored posterior at entity `0` was present. Thus the associated reconciliation operation is not unconditional carry of the prior update.

These are separate findings: invalid probability values are admitted by the binding itself; changed membership is demonstrated by the associated helper named in the registry note.

**d. Verdict: DIVERGES**

The type does not enforce the registry’s declared normalized categorical representation, and the associated operation does not satisfy its written carry equation. Both defects exist at the registry pin; they are not later drift.

**e. Citation and imports**

Buckley is a general reference, not a numbered citation for this entity-domain reconciliation. No source equation establishing that operation was identifiable.

`:imports [:mu-next]` has no counterpart in the type abbreviation. The reconciliation helper takes **both** fresh and carried states; only the carried argument plausibly corresponds to the preceding `mu-next`. The fresh-domain input is omitted from the imports.

The numerical counterexamples above were derived from the inspected definitions; the precision arithmetic was additionally checked with exact rational arithmetic. They were not added or compiled as new Lean fixtures. Production agreement was not used as evidence of equation fidelity.

| Registry row | Verdict | Decisive finding |
|---|---|---|
| `:observe` | **CANNOT-TELL** | Constructor supplies no world/action observation relationship. |
| `:prediction-error` | **DIVERGES** | Extra variance gating can refuse a defined scalar subtraction. |
| `:precision` | **DIVERGES** | Regularized squared-error estimate and clamp change the written formula. |
| `:belief-update` | **DIVERGES** | Categorical reweighting replaces the additive step; negative masses are admitted. |
| `:belief-state` | **DIVERGES** | Unconstrained posterior type and domain reconciliation do not establish the stated belief carry. |

---

# A3 — policy-free-energy, forward-model, risk, ambiguity, expected-free-energy

Agency job `invoke-1789576973471-21589-29024e45`. Report text verbatim.

**Batch 2 complete.** The kernel repair strengthens the mathematics. Current ambiguity matches its cited equation; risk matches on a restricted finite-KL domain. Policy free energy and expected free energy admit concrete counterexamples. The forward-model’s `:carrier-only` status is accurate, but its roll-forward equation remains unstated.

No edits, commits, dispatches, production probes or Lean builds were performed.

**Citation convention:** **R** = futon2 `ab0b56f223a83083ec9b3c43a720a90112dd0d42`, `holes/labs/wm-contract/aif-equations.edn`. **M** = mathlib4 `f40c936a64227ba81592a71f3d937e6fbdbf0e4c`, with Lean paths relative to `DarkTower/WarMachine/`. Every quotation below uses these commits unless otherwise specified.

Retrieved PDFs/text are uncommitted, so have no commit SHA. Their PDF hashes match `refs/README.md:13,18` at R: Da Costa `66bfbf026448835f…`, Parr `f1ddb2efaf4f86f9…`. I read the extracted equations and visually checked Da Costa PDF p. 27.

## 1. `:policy-free-energy` → `machinePolicyFreeEnergy`

**a. Registry and source**

R:102–105:

```clojure
:ref :parr2022
:imports [:Q-o-pi :o]
:formal "F_pi := sum_k 1/2 ( ln(2 pi v_k(pi)) + (o_k - mu_k(pi))^2 / v_k(pi) )"
:eq "parr2022 eq. B.9, p. 247 -- the F term of pi = sigma(ln E - F - G). B.9 names the term and does not fix the likelihood; the Gaussian per-channel form is this stack's reduction, so like :precision this row is theory-defined in ROLE and stack-defined in CONTENT."
```

Additional declared reductions include:

> “THE :formal LINE ABOVE IS THE POSITIVE-VARIANCE BRANCH ONLY”

— R:107.

> “HORIZON ONE AND RETROSPECTIVE”

— R:112.

The notes describe variance flooring, deterministic-zero handling and complete-or-off coverage. These are admissions about the binding, not evidence that it equals the formal line.

Parr B.9 states:
\[
\nabla_\pi F=0\iff\pi=\sigma(\ln E-F-G).
\]
Source: `refs/parr2022.txt:12659–12664`, printed p. 247.

The same passage identifies its \(F_\pi\) with B.4. The more general definition is B.2:
\[
F(\pi)=E_{Q(\tilde s\mid\pi)}
[\ln Q(\tilde s\mid\pi)-\ln P(\tilde o,\tilde s\mid\pi)]
\ge-\ln P(\tilde o\mid\pi).
\]
Source: `refs/parr2022.txt:12509–12514`. Thus the registry explicitly selects a Gaussian predictive negative-log-likelihood score rather than transcribing the general functional.

**b. Declaration, dependencies and history**

M, [`MachinePolicyFreeEnergy.lean:59–63`](/home/joe/code/mathlib4/DarkTower/WarMachine/MachinePolicyFreeEnergy.lean:59):

```lean
noncomputable def machinePolicyFreeEnergy
    (channels : List ChannelDatum) (tolerance floor : ℝ)
    (mode : AbsentVarianceMode) : Except PolicyFreeEnergyError ℝ :=
  channels.foldlM (fun total datum =>
    return total + (← channelPolicyFreeEnergy datum tolerance floor mode)) 0
```

Relevant dependencies, M, `MachinePolicyFreeEnergy.lean:35–56`:

```lean
inductive VarianceStatus | present | absent deriving DecidableEq, Repr
inductive AbsentVarianceMode | reject | floor deriving DecidableEq, Repr
inductive PolicyFreeEnergyError | invalidVariance | deterministicMismatch
  deriving DecidableEq, Repr

structure ChannelDatum where
  residual : ℝ
  variance : ℝ
  varianceStatus : VarianceStatus := .present

noncomputable def channelPolicyFreeEnergy
    (datum : ChannelDatum) (tolerance floor : ℝ)
    (mode : AbsentVarianceMode) : Except PolicyFreeEnergyError ℝ :=
  let effectiveVariance :=
    if datum.variance = 0 ∧ datum.varianceStatus = .absent ∧ mode = .floor
    then floor else datum.variance
  if effectiveVariance < 0 then .error .invalidVariance
  else if effectiveVariance = 0 then
    if |datum.residual| ≤ tolerance then .ok 0
    else .error .deterministicMismatch
  else .ok ((Real.log (2 * Real.pi * effectiveVariance) +
    datum.residual ^ 2 / effectiveVariance) / 2)
```

The **entire module** is unchanged from registry pin `d7a45a358acbc7680b44267032607216bf2a4b12`. No relevant arithmetic-library or toolchain change was found.

**c. Domain and counterexample**

On positive supplied variances, this computes the declared Gaussian sum exactly, using fixed `Real.log` and `Real.pi`.

But this admitted input returns `.ok 0`:

```text
channels = [{ residual = 1, variance = 0, varianceStatus = present }]
tolerance = 1
floor = 1/100
mode = reject
```

The observation differs from the deterministic prediction. A zero-variance point prediction assigns it zero probability; its negative log probability is not zero. Nor is the positive-variance Gaussian formula defined at this variance. The tolerance branch therefore assigns a finite score outside either interpretation.

Other domain observations:

- Negative effective variances are rejected, appropriately.
- `floor` and `tolerance` are unrestricted reals; a zero floor can enter the same deterministic branch.
- Residuals are supplied directly, without a relation to `o − mu`.
- The list does not establish channel identity or complete coverage.
- An empty list returns zero. That is a valid empty sum, not independently a defect; it does not establish complete observation coverage.

**d. Verdict: DIVERGES**

The positive-variance component matches the declared reduction. The complete binding admits successful scores that are neither that Gaussian quantity nor exact deterministic surprisal. The registry documents the additional branches but does not turn them into the cited equation.

**e. Citation and imports**

The B.9 citation is accurate **as a use-site**, and the registry correctly says it does not derive the Gaussian formula. B.2/B.4 are needed to discuss what \(F_\pi\) itself means. A Gaussian likelihood alone does not eliminate the variational posterior/complexity term; that requires an additional identification or approximation.

`:imports [:Q-o-pi :o]` does not match the concrete inputs: residual/variance records, status tags, tolerance, floor and mode. No predictive distribution or observation enters directly, and no bridge establishes their relationship to those records.

## 2. `:forward-model` → `PredictiveOutcomeKernel`

**a. Registry and source**

R:126–130:

```clojure
:ref :dacosta2020
:imports [:mu :A :B :pi :T]
:formal "Q(o|pi) := sum_s A(o|s) Q(s|pi), Q(s|pi) rolled forward by B from mu over depth T"
:eq "dacosta2020 eq. 42-44 (the predictive terms A s_π)"
:lean-status :carrier-only
:note "Q-interface-completeness.edn records 2 missing wires for this row"
```

Da Costa (44) contains:
\[
(A s^\pi_\tau)\cdot\bigl(\log(A s^\pi_\tau)-\log C\bigr).
\]
Source: `refs/dacosta2020.txt:1418–1423`, PDF p. 27. Its \(A s^\pi_\tau\) is the predicted outcome distribution used by the registry. This passage does not itself specify the complete depth-\(T\) rollout algorithm.

**b. Declaration, dependencies and history**

M, [`Holes.lean:7024–7025`](/home/joe/code/mathlib4/DarkTower/WarMachine/Holes.lean:7024):

```lean
abbrev PredictiveOutcomeKernel (PolicyIndex : Type*) (Obs : Vertex → Type*) :=
  ProbabilityKernel PolicyIndex (Outcome Obs)
```

M, `Holes.lean:7015–7021`:

```lean
structure ProbabilityKernel (S O : Type*) where
  support : S → List O
  mass : S → O → ℝ
  nonnegative : ∀ s o, 0 ≤ mass s o
  normalised : ∀ s, ((support s).map (mass s)).sum = 1
  support_nodup : ∀ s, (support s).Nodup
  mass_eq_zero_of_not_mem : ∀ s o, o ∉ support s → mass s o = 0
```

M, `Holes.lean:146–154`:

```lean
inductive Vertex where
  | nouns
  | verbs
  | organization
  | evidence
  deriving DecidableEq, Repr

abbrev Outcome (Obs : Vertex → Type*) := Sigma Obs
```

Declaration-by-declaration comparison across all 25 `Holes.lean` commits since `d22ddd1385` found:

- `PredictiveOutcomeKernel` and `Outcome`: unchanged.
- `fcd1261c303c2beca08a6812eba4a7ce2e83d722`: renames `Vertex` constructors from people/money/organisations to nouns/verbs/organization. **Neither toward nor away** from the probability equation: the four-way tagging structure remains.
- `480a666ad27c18477b4b9df86e11862be3930d50`: adds `support_nodup` and `mass_eq_zero_of_not_mem`. **Toward** the equation’s probability semantics.

At baseline, `ProbabilityKernel` had only support, mass, nonnegativity and list-sum normalization (`Holes.lean:6196–6200`, `d22ddd1385`). It admitted duplicate-counted mass and arbitrary positive mass outside the listed support.

**c. Domain assessment**

The current type supplies a genuine finite probability distribution for every supplied policy:

- nonnegative mass;
- total mass one;
- no duplicate-counting;
- zero mass outside the finite support.

For an actual policy, empty support is impossible because its sum would be zero.

It supplies **none** of the relationships to `mu`, `A`, `B` or `T`. For example, a deterministic model predicting outcome `a` and a separately supplied kernel concentrated at distinct outcome `b` are individually well formed; nothing in this carrier relates them.

That example illustrates a missing equation, not a false theorem: the carrier does not assert rollout correctness.

**d. Verdict: CANNOT-TELL**

The registry’s **`:carrier-only` status is accurate**. The carrier now correctly represents finite policy-conditioned outcome distributions, but does not state their construction by marginalization and rollout. Equation fidelity cannot be established from this binding.

**e. Citation and imports**

Equations (42)–(44) support the use of predictive distributions; the citation is imprecise for the full temporal rollout.

The abbreviation takes type parameters only. `pi` becomes the kernel’s row index; `mu`, `A`, `B` and `T` are absent. The imports describe the intended producer, not this carrier.

## 3. `:risk` → `predictiveOutcomeRisk`

**a. Registry and source**

R:131–133:

```clojure
:ref :dacosta2020
:imports [:Q-o-pi :C]
:formal "risk(pi) := D_KL[Q(o|pi) || C]"
:eq "dacosta2020 eq. 44 (risk over outcomes)"
```

There is no row-level reduction note.

Da Costa (44), quoted above, is exactly outcome-space KL with \(Q(o\mid\pi)=A s^\pi_\tau\): `refs/dacosta2020.txt:1418–1423`.

**b. Declaration, dependencies and history**

M, [`Holes.lean:7147–7151`](/home/joe/code/mathlib4/DarkTower/WarMachine/Holes.lean:7147):

```lean
def predictiveOutcomeRisk {PolicyIndex : Type*} {Obs : Vertex → Type*}
    (Q : PredictiveOutcomeKernel PolicyIndex Obs) (Cdist : PreferenceDistribution Obs)
    (_positivePreference : ∀ π o, o ∈ Q.support π → 0 < Cdist.mass () o)
    (π : PolicyIndex) : ℝ :=
  (Q.support π).map (fun o => Q.mass π o * Real.log (Q.mass π o / Cdist.mass () o)) |>.sum
```

M, `Holes.lean:7045–7046`:

```lean
abbrev PreferenceDistribution (Obs : Vertex → Type*) :=
  ProbabilityKernel Unit (Outcome Obs)
```

The relevant kernel and outcome definitions are quoted in row 2.

The risk declaration, preference alias and outcome alias are unchanged from baseline (`predictiveOutcomeRisk` at `Holes.lean:6312–6316`, `d22ddd1385`). Their relevant dependency changes are exactly the Vertex rename and kernel repair described above: respectively **neither**, and **toward** the cited equation.

**c. Domain assessment**

For admitted inputs, this is exact finite-distribution KL:

- normalization and support completeness are enforced;
- `Real.log` is fixed;
- zero predictive masses contribute zero;
- positive preference mass prevents division by zero where terms are evaluated;
- differing support-list lengths do not cause truncation: there is no zip.

The restriction is stronger than the registry states. Cases with positive predictive mass and zero preference mass have infinite KL and are excluded rather than represented.

There is also a representational restriction: positivity is required on every **listed** predictive outcome, even if its predictive mass is zero. For example, `Q=C=δ_a` has KL zero, but listing an additional zero-mass outcome `b` in `Q.support` prevents construction of the positivity argument when `C(b)=0`. Removing that padding admits the same distribution.

**d. Verdict: MATCHES-WITH-UNDECLARED-REDUCTION**

It matches KL on its admitted domain. The registry does not declare restriction to finite-valued KL with positive preferences across the entire listed predictive support. This restriction is explained in the Lean comment (`Holes.lean:7144–7146`, M), but not in the registry row.

No admitted-input numerical counterexample was found.

**e. Citation and imports**

The equation citation is correct. The mathematical inputs agree with `[:Q-o-pi :C]`, plus a policy evaluation argument and the additional positivity proof. Construction of `Q` from a model remains the separate forward-model obligation.

## 4. `:ambiguity` → `ambiguity`

**a. Registry and source**

R:134–136:

```clojure
:ref :dacosta2020
:imports [:Q-o-pi :A]
:formal "ambiguity(pi) := E_{Q(s|pi)}[H(P(o|s))]"
:eq "dacosta2020 eq. 45-48"
```

No mathematical reduction is declared. The subsequent measurement notes do not change this equation.

The cited source states:
\[
H[P(o_\tau\mid s_\tau)]
=-\sum_{o_\tau}P(o_\tau\mid s_\tau)\log P(o_\tau\mid s_\tau)
\tag{45}
\]
and
\[
E_{Q(s_\tau\mid\pi)}[H[P(o_\tau\mid s_\tau)]]
=H\cdot s^\pi_\tau.
\tag{48}
\]
Sources: `refs/dacosta2020.txt:1428–1431,1465–1470`. Equations (46)–(47) derive the likelihood/matrix representation.

**b. Declaration, dependencies and history**

M, [`Holes.lean:7160–7164`](/home/joe/code/mathlib4/DarkTower/WarMachine/Holes.lean:7160):

```lean
def ambiguity {PolicyIndex State Observation : Type*}
    (predictedState : ProbabilityKernel PolicyIndex State)
    (A : observationKernel State Observation) (π : PolicyIndex) : ℝ :=
  (predictedState.support π).map
    (fun s => predictedState.mass π s * observationEntropy A s) |>.sum
```

M, `Holes.lean:7069,7155–7157`:

```lean
abbrev observationKernel (State Observation : Type*) := ProbabilityKernel State Observation

def observationEntropy {State Observation : Type*}
    (A : observationKernel State Observation) (s : State) : ℝ :=
  -((A.support s).map (fun o => A.mass s o * Real.log (A.mass s o))).sum
```

All three declarations are unchanged from baseline (`Holes.lean:6320–6329` for the two functions). The relevant changed dependency is `ProbabilityKernel`, repaired by `480a666ad2`: **toward** the equation. `Vertex` is not a dependency of this generic declaration.

**c. Domain assessment**

Both state weights and observation rows are normalized finite distributions. Support duplication and hidden outside-support mass are excluded. Zero masses obey the usual \(0\log0=0\) convention. No arbitrary entropy function, zipped lists or unconstrained probability weights enter.

The function computes the cited conditional-entropy expectation for every admitted input. The caller supplies the predicted-state distribution; deriving it from a rollout is a separate obligation.

**d. Verdict: MATCHES**

The arithmetic and admitted probability domain match the cited discrete equation. No additional Gaussian or differential-entropy approximation occurs here.

**e. Citation and imports**

The citation is correct, but **the imports are wrong**: the function and formal expression need `Q(s|pi)`, whereas `:Q-o-pi` denotes `Q(o|pi)`.

These are not interchangeable. Even with fixed `A`, equal outcome marginals can have different expected conditional entropy: one predicted state may have a fair-coin observation row, while an equal mixture of two deterministic states produces the same fair-coin outcome marginal with zero ambiguity. Thus outcome prediction alone does not supply the missing state distribution.

## 5. `:expected-free-energy` → `expectedFreeEnergy`

**a. Registry and source**

R:153–157:

```clojure
:ref :dacosta2020
:imports [:risk :ambiguity]
:formal "G(pi) := risk(pi) + ambiguity(pi)"
:eq "dacosta2020 eq. 42 WITHOUT the novelty term -E[D_KL[Q(A|o,s)||Q(A)]]"
:omits "novelty (parameter-exploration) term of eq. 42; the WM has no Dirichlet Q(A) live, so the term is identically absent rather than approximated"
:conflict "Holes.G := risk - eig over a generic Policy; G_eq_expectedFreeEnergy bridges the two under stated assumptions (glossary-formal-lines.md, G entry)"
```

Da Costa (42) states:
\[
G(\pi)=
E_{Q(s_\tau\mid\pi)}[H(P(o_\tau\mid s_\tau))]
+D_{\rm KL}[Q(s_\tau\mid\pi)\Vert P(s_\tau)]
-E_{P(o_\tau\mid s_\tau)Q(s_\tau\mid\pi)}
[D_{\rm KL}[Q(A\mid o_\tau,s_\tau)\Vert Q(A)]].
\]
Source: `refs/dacosta2020.txt:1403–1405`.

The registry explicitly omits novelty and uses the outcome-risk substitution from (44) through its risk row.

**b. Declaration, dependencies and history**

M, [`Holes.lean:7167–7171`](/home/joe/code/mathlib4/DarkTower/WarMachine/Holes.lean:7167):

```lean
def expectedFreeEnergy {PolicyIndex : Type*} {Obs : Vertex → Type*}
    (Q : PredictiveOutcomeKernel PolicyIndex Obs) (Cdist : PreferenceDistribution Obs)
    (positivePreference : ∀ π o, o ∈ Q.support π → 0 < Cdist.mass () o)
    (ambiguity : PolicyIndex → ℝ) : PolicyIndex → ExpectedFreeEnergyValue :=
  fun π => ⟨predictiveOutcomeRisk Q Cdist positivePreference π + ambiguity π⟩
```

The result wrapper imposes no constraint, M, `Holes.lean:162–163`:

```lean
structure ExpectedFreeEnergyValue where
  value : ℝ
```

The relevant kernel/risk dependencies are quoted above. The declaration and wrapper are unchanged from baseline (`expectedFreeEnergy` at `Holes.lean:6332–6336`, `d22ddd1385`). The kernel repair moves its risk component **toward** the source; the Vertex rename is mathematically neutral.

The bridge remains unchanged too. Its key hypotheses are:

```lean
(risk_eq : ∀ π, risk π = predictiveOutcomeRisk Q Cdist positivePreference π)
(ambiguity_eq : ∀ π, ambiguity π = -eig π)
```

— M, `Holes.lean:7180–7181`.

Those assumptions do not identify the supplied ambiguity with the kernel-derived entropy expectation.

**c. Domain and counterexample**

Take one policy, a deterministic one-state/one-outcome model, and `Q=C=δ_o`. Both true risk and true ambiguity are zero.

Supply:

```text
ambiguity := fun _ => -1
```

All kernel and positivity requirements hold, but `expectedFreeEnergy` returns `-1` instead of the cited reduced value `0`.

This is not merely an absent nonnegativity proof: the function argument is unrelated to the canonical `ambiguity` declaration. Supplying `+1` would also produce the wrong value for this deterministic model.

There is also no requirement that outcome prediction and supplied ambiguity arise from the same predicted-state distribution and observation kernel.

**d. Verdict: DIVERGES**

The function correctly adds a computed KL to an arbitrary real-valued function. Its domain does not ensure that the latter is the equation’s ambiguity term. The declared omission of novelty does not authorize this substitution.

**e. Citation and imports**

Equation (42) uses **state** risk; obtaining this row’s outcome-risk version also needs (44). That choice is recoverable from the preceding risk row, but citing (42) alone is imprecise.

`:imports [:risk :ambiguity]` describes the intended decomposition. The Lean implementation instead takes `Q`, `C`, a positivity proof and an unconstrained ambiguity function. It recomputes risk and does not enforce the declared ambiguity dependency.

The bridge to `G` proves equality under supplied algebraic assumptions; it does not establish the standard AIF relationship between information gain and conditional entropy.

All counterexamples above are deductions from the inspected definitions, not newly compiled fixtures. I did not evaluate production conformance.

| Registry row | Verdict | Finding |
|---|---|---|
| `:policy-free-energy` | **DIVERGES** | Zero-variance tolerance branch can assign zero cost to an impossible deterministic observation. |
| `:forward-model` | **CANNOT-TELL** | Carrier-only status is accurate; marginalization and rollout are unstated. |
| `:risk` | **MATCHES-WITH-UNDECLARED-REDUCTION** | Exact finite KL, restricted by preference positivity across listed predictive support. |
| `:ambiguity` | **MATCHES** | Exact finite conditional-entropy expectation; registry imports name the wrong predictive distribution. |
| `:expected-free-energy` | **DIVERGES** | Arbitrary ambiguity input need not be the cited entropy expectation. |

---

# A4 — policy-set, depth, temperature, action, dirichlet-accumulation

Agency job `invoke-1789577344338-21591-b7144194`. Report text verbatim.

**Batch 3 complete.** The candidate-set constructor matches its limited stack-defined claim. Depth, temperature and action diverge. The Dirichlet binding correctly remains `:carrier-only`: it constrains individual concentration vectors but states no accumulation law.

No edits, commits, dispatches, production probes or Lean builds were performed.

**Citation convention:** **R** = futon2 `ab0b56f223a83083ec9b3c43a720a90112dd0d42`, `holes/labs/wm-contract/aif-equations.edn`. **M** = mathlib4 `f40c936a64227ba81592a71f3d937e6fbdbf0e4c`, with Lean paths relative to `DarkTower/WarMachine/`. Quotations use these commits unless another is specified.

Retrieved source files are uncommitted and have no commit SHA. The PDFs match the indexed hashes: Da Costa `66bfbf026448835f…`; Friston 2017 `c0f3c5f090f21e1b…` (`refs/README.md:13,15`, R). I read the cited passages and checked the PDF images where typography matters.

## 1. `:policy-set` → `machinePolicySet`

**a. Registry and source**

R:158–163:

```clojure
:class :stack-defined :ref :friston2017
:imports []
:formal "pi ranges over the candidate action space"
:lean "machinePolicySet"
```

There is no `:eq` field. The note describes the selected representation:

> “machinePolicySet maps the ranked List Candidate consumed by select-action to that carrier”

— R:164.

There is therefore no specific cited equation to quote. The retrieved Friston source describes a policy as a **sequence of actions**, sampled from a Gibbs distribution with inverse temperature \(\gamma\): `refs/friston2017.txt:359–364`, printed p. 8. Its equation (2.1) contains \(P(\pi)=\sigma(-\gamma G(\pi))\), `:329–339`. Neither specifies this stack’s candidate-set construction.

**b. Declaration, dependencies and history**

M, [`MachinePolicySet.lean:21–22`](/home/joe/code/mathlib4/DarkTower/WarMachine/MachinePolicySet.lean:21):

```lean
def machinePolicySet (ranked : List Candidate) : CandidateActionSpace Candidate :=
  {candidate | candidate ∈ ranked}
```

M, `F12RuledCarrier.lean:18`:

```lean
abbrev CandidateActionSpace (P : Type*) := Set P
```

M, `MachineAction.lean:15–19`:

```lean
structure Candidate where
  id : Nat
  score : ℤ
  noOp : Bool
  deriving DecidableEq, Repr
```

The complete `MachinePolicySet`, `F12RuledCarrier` and `MachineAction` files are unchanged from pin `738cae3a540ac03a4d77e1ebe399c7c6dccd8cb6`. There is no subsequent relevant change to classify.

**c. Domain assessment**

For every input list, the result contains exactly its candidate records. Duplicate records collapse under set membership; ordering is discarded. An empty list yields the empty set. The formal line requires neither nonemptiness nor a probability distribution.

The set is over **whole records**, not `Candidate.id`: equal IDs with different scores remain distinct members. No invariant identifies an ID with a unique action, and no time-indexed action sequence is represented. These limit what can be inferred about AIF policies, but do not falsify the row’s stated candidate-space construction.

**d. Verdict: MATCHES**

This matches the deliberately limited, stack-defined extensional candidate-set claim. It does not establish correspondence between those candidates and Friston’s temporal policies, nor adequacy of the generated candidate space.

**e. Citation and imports**

The reference is general rather than a precise derivation. There is no wrong equation number to identify.

`:imports []` omits the supplied ranked candidate collection. It can describe a separately declared domain, but not the dependency of this particular constructor.

## 2. `:depth` → `machineDepth`

**a. Registry and source**

R:165–169:

```clojure
:ref :dacosta2020
:imports []
:formal "T := temporal policy depth (the range of the sums in Q(o|pi) and G)"
:eq "dacosta2020 eq. 42 (sum over τ)"
```

The note admits:

> “machineDepth is Option Nat -> EfeDepths, not a number”

and:

> “THE :formal LINE IS FALSE AS WRITTEN”

— R:170.

The cited Da Costa equation (42) is:
\[
G(\pi)=
E_{Q(s_\tau\mid\pi)}[H(P(o_\tau\mid s_\tau))]
+D_{\mathrm{KL}}[Q(s_\tau\mid\pi)\Vert P(s_\tau)]
-E_{P(o_\tau\mid s_\tau)Q(s_\tau\mid\pi)}
[D_{\mathrm{KL}}[Q(A\mid o_\tau,s_\tau)\Vert Q(A)]].
\]

Source: `refs/dacosta2020.txt:1403–1405`, PDF p. 27. **It has a fixed future \(\tau\), not a sum over \(\tau\).** The surrounding discussion identifies this future time as usually the policy horizon \(T\), `:1304–1309`.

**b. Declaration, dependencies and history**

M, [`MachineDepth.lean:133`](/home/joe/code/mathlib4/DarkTower/WarMachine/MachineDepth.lean:133):

```lean
abbrev machineDepth := Option Nat → EfeDepths
```

M, `MachineDepth.lean:108–126`:

```lean
def effectiveDepth : Option Nat → Nat
  | some k => if 2 ≤ k then k else 1
  | none => 1

structure EfeDepths where
  risk : Nat
  homeostatic : Nat
  ambiguity : Nat
  information : Nat
  deriving DecidableEq, Repr

def efeDepths (requested : Option Nat) : EfeDepths :=
  let k := effectiveDepth requested
  ⟨k, k, 1, 1⟩
```

The module is unchanged from `9eef38b6a1117924fcf6476671ec21c3d4dd8a6b`. No relevant arithmetic-library or toolchain change was found.

**c. Domain and counterexample**

`machineDepth` is a type of functions returning four independent natural numbers. It neither denotes a single horizon nor constrains its inhabitants to coherent horizons.

The designated inhabitant provides a direct counterexample:

```text
efeDepths (some 3) = ⟨3, 3, 1, 1⟩
```

Risk and ambiguity thus receive different depth indices. They cannot both equal the single temporal index used in the cited decomposition.

An arbitrary inhabitant such as `fun _ => ⟨0,2,5,99⟩` is also admitted, demonstrating that even the designated dispatch rule is not enforced by the named type.

The registry notes that state-independent variances can make differently indexed values coincide. That does not establish common indexing, but it **does prevent inferring a numerical scoring difference merely from this example**.

**d. Verdict: DIVERGES**

The binding is a type for independently indexed quantities, not the scalar temporal horizon described by the row. Its designated inhabitant explicitly disagrees across terms.

**e. Citation and imports**

The attribution “eq. 42 (sum over τ)” is wrong: equation (42) contains no temporal sum. The Lean header and registry note repeat this citation error. This does not rescue the binding: the source still evaluates the displayed decomposition at one common future \(\tau\).

`:imports []` does not describe `efeDepths`, which takes a requested horizon. The named abbreviation itself is a type, not a quantity-producing function.

## 3. `:temperature` → `machineTemperature`

**a. Registry and source**

R:171–175:

```clojure
:class :stack-defined :ref :friston2017
:imports []
:formal "tau := commitment temperature (inverse precision gamma of policy selection)"
:eq "friston2017 eq. 2.1 (γ = 1/β); dacosta2020 A.2"
```

The note admits:

> “THE :formal LINE NAMES ONE QUANTITY AND THE MACHINE HAS THREE LAWS.”

and:

> “THE :eq LINE IS TRUE OF ONE ARM ONLY.”

— R:176.

Friston equation (2.1) contains
\[
P(\pi)=\sigma(-\gamma G(\pi)).
\]
Source: `refs/friston2017.txt:329–339`.

The identity \(\gamma=1/\beta\) appears in the explanation following **equation (2.7)**, `:683–684`. Da Costa A.2 introduces \(\sigma(-\gamma G(\pi))\) and identifies \(\gamma\) as inverse temperature, `refs/dacosta2020.txt:1262–1271`.

**b. Declaration, dependencies and history**

M, [`MachineTemperature.lean:42–50`](/home/joe/code/mathlib4/DarkTower/WarMachine/MachineTemperature.lean:42):

```lean
noncomputable def machineTemperature (opts : TemperatureOpts) : Except TemperatureError ℝ :=
  let g := max opts.tauMin opts.selectionGain
  match opts.mode with
  | .spread => .ok (opts.spreadTemperature / g)
  | .selectionGainOnly => .ok (1 / g)
  | .variationalBetaGamma =>
      match opts.variationalBeta with
      | some (.finite beta) => if 0 < beta then .ok beta else .error .invalidVariationalBeta
      | _ => .error .invalidVariationalBeta
```

Relevant types, M, `MachineTemperature.lean:18–37`:

```lean
inductive TauMode | spread | selectionGainOnly | variationalBetaGamma
  deriving DecidableEq, Repr

inductive MachineNumber
  | finite (value : ℝ)
  | nonfinite

structure TemperatureOpts where
  mode : TauMode
  tauMin : ℝ
  spreadTemperature : ℝ
  selectionGain : ℝ
  variationalBeta : Option MachineNumber := none

inductive TemperatureError | invalidVariationalBeta deriving DecidableEq, Repr
```

The separate spread helper is, M, `MachineTemperature.lean:142–144`:

```lean
noncomputable def adaptiveTemperature (tauMin k : ℝ) : List ℝ → ℝ
  | [] => tauMin
  | gs => max tauMin (gRange gs / k)
```

But `machineTemperature` takes `spreadTemperature` directly; no condition ties that field to this helper.

The complete module is unchanged from `b31e5db2e494a0f11c4c35077c25d7399b1d19c9`.

**c. Domain and counterexamples**

A positive-input discrepancy already exists:

```text
tauMin = 1/100
spreadTemperature = 3/5
selectionGain = 2
variationalBeta = some (finite (1/4))
```

The three modes return respectively `3/10`, `1/2`, and `1/4`. Only the last satisfies `tau = beta`, hence the cited `gamma = 1/beta`. These values are stated in `threeLawsDisagree`, M, `MachineTemperature.lean:180–186`.

Independently, the engineering modes admit invalid inverse-temperature values:

```text
mode = selectionGainOnly
tauMin = -2
selectionGain = -1
spreadTemperature = 1
variationalBeta = none
```

This returns `.ok (-1)`. Setting both gain inputs to zero returns `.ok 0` under Lean’s real division. Neither supplies the positive temperature associated with the cited positive precision model.

Even with positive `tauMin` and gain, `.spread` accepts a negative supplied `spreadTemperature`.

**d. Verdict: DIVERGES**

Only the validated variational branch implements the cited beta relation. Other branches implement different laws, and their admitted domain includes zero and negative temperatures. Merely defining `gamma := 1/tau` afterward would not establish the cited probabilistic precision semantics.

**e. Citation and imports**

Equation (2.1) introduces precision in policy selection; the explicit reciprocal-beta identity is located after (2.7). The registry combines the two under an imprecise equation reference.

`:imports []` omits mode, minimum temperature, spread temperature, selection gain and optional beta. The binding does not infer beta or relate it to a precision posterior.

## 4. `:action` → `machineAction`

**a. Registry and source**

R:184–190:

```clojure
:ref :friston2017
:imports [:Q-pi]
:formal "u_t := argmax_u sum_pi delta(u, pi_t) Q(pi)"
:eq "dacosta2020 eq. 11 (Bayesian-model-average argmax); friston2017 eq. 2.3 selects the action minimising expected outcome prediction error -- two deterministic rules in the literature, neither is sampling (C453)"
```

The note admits:

> “THE CARRIER IS NOT A FUNCTION OF A POSTERIOR”

— R:191.

Da Costa (11) states precisely:
\[
u_t=\arg\max_{u\in U}\sum_{\pi\in\Pi}\delta_{u,\pi_t}Q(\pi).
\]
Source: `refs/dacosta2020.txt:685–697`.

Friston (2.3) instead selects the action minimizing
\[
E_Q[D(P(o_{t+1}\mid s_{t+1})\Vert R(o_{t+1}\mid s_t,u))].
\]
Source: `refs/friston2017.txt:385–407`, printed/PDF p. 8, visually checked. Neither source says that an arbitrary deterministic selector suffices.

**b. Declaration, dependencies and history**

M, [`MachineAction.lean:49–56`](/home/joe/code/mathlib4/DarkTower/WarMachine/MachineAction.lean:49):

```lean
def machineAction (boundary : SelectionBoundary) (law : StrategicLaw)
    (fPiEntered anyHabitPrior : Bool) (ranked scored : List Candidate) : Option Candidate :=
  match boundary with
  | .strategicRecommendation =>
      let candidates := strategicCandidates ranked
      if law = .fullScorePosterior ∧ fPiEntered then firstArgmax (strategicCandidates scored)
      else candidates.head?
  | .actuation => if anyHabitPrior then lastArgmax scored else ranked.head?
```

`Candidate` is quoted in row 1. The dispatch vocabularies and helpers are, M, `MachineAction.lean:22–41`:

```lean
inductive StrategicLaw | controllerHead | fullScorePosterior deriving DecidableEq, Repr
inductive SelectionBoundary | strategicRecommendation | actuation deriving DecidableEq, Repr

def firstArgmax : List Candidate → Option Candidate
  | [] => none
  | x :: xs => some (xs.foldl (fun best c => if best.score < c.score then c else best) x)

def lastArgmax : List Candidate → Option Candidate
  | [] => none
  | x :: xs => some (xs.foldl (fun best c => if best.score ≤ c.score then c else best) x)

def strategicCandidates (xs : List Candidate) : List Candidate := xs.filter (!·.noOp)
```

The module is unchanged from `461720489008ecadc5eef01f4cc1c0c323e7b86f`.

**c. Domain and counterexamples**

A direct counterexample uses one candidate per distinct action:

```text
boundary = strategicRecommendation
law = controllerHead
fPiEntered = true
anyHabitPrior = false
ranked = scored = [⟨0,0,false⟩, ⟨1,1,false⟩]
```

The function selects candidate `0`. Under the normalized posterior proportional to `[exp 0, exp 1]`, equation (11) selects action `1`. There is no tie.

Further constraints are absent:

- `ranked` and `scored` need not contain the same candidates.
- Integer scores have no enforced relationship to `Q(pi)`.
- Empty input is admitted and returns `none`.
- Strategic filtering can remove the highest-posterior action.
- No policy-to-current-action projection or posterior-mass aggregation exists.

**The non-default argmax branch is not generally equation (11), either.** For four policies with scores `[0,0,0,1]`, suppose the first three select action `a` and the fourth selects `b`. Individual-policy argmax selects `b`, but equation (11) selects `a`, because its unnormalized mass is `3 > exp 1`.

That example concerns the missing policy/action correspondence: the Lean carrier does not even represent the required projection. Equality needs an additional restriction such as one policy per current action, or prior aggregation to action-level scores.

The theorem `posteriorOrderIsScoreOrder` proves only pairwise score/posterior ordering (`MachineAction.lean:105–107`, M). It does not prove preservation of argmax after summing policies by action.

Opposite tie-breaking conventions alone are **not** a violation: both tied maximizers satisfy an unspecified argmax rule.

**d. Verdict: DIVERGES**

Several branches ignore posterior scores. Even the score-maximizing branch lacks the policy-to-action aggregation required by the cited equation.

**e. Citation and imports**

The source equation numbers are correct. The registry’s assertion that deterministic selection conforms to equation (11) is too broad. Likewise, its note identifying the non-default policy-score argmax with equation (11) needs the extra one-policy-per-action or action-aggregation assumption.

`:imports [:Q-pi]` does not match the declaration: no posterior is supplied. Inputs are dispatch controls and two unconstrained candidate lists.

## 5. `:dirichlet-accumulation` → `DirichletConcentrations`

**a. Registry and source**

R:192–196:

```clojure
:ref :dacosta2020
:imports [:o :mu]
:formal "a = a + sum_tau o_tau (x) s_tau  (Dirichlet concentrations accumulated from observed outcome/state pairs)"
:eq "dacosta2020 eq. 21 (A-learning; B/D learning A.1 eqs. 27-33 per C453)"
:lean "DirichletConcentrations"
:lean-status :carrier-only
```

The note says:

> “THE CARRIER DELIBERATELY DID NOT MOVE”

and explains that `machineDirichletAccumulation` describes a different operation, so it was not substituted as this row’s binding — R:197.

Da Costa (21) is, distinguishing posterior from prior typography:
\[
a_{\mathrm{post}}=a_{\mathrm{prior}}
+\sum_{\tau=1}^{T}o_\tau\otimes s_\tau.
\]
Source: `refs/dacosta2020.txt:943–955`, PDF p. 18, visually checked. The source then updates the prior to that posterior for the next trial.

The secondary citation is also correctly located: A.1 introduces the extended model and derives B/D updates, ending with equations (32)–(33), `refs/dacosta2020.txt:1201–1260`.

**b. Declaration, dependencies and history**

M, [`Holes.lean:7216`](/home/joe/code/mathlib4/DarkTower/WarMachine/Holes.lean:7216):

```lean
def DirichletConcentrations := {xs : List ℝ // xs ≠ [] ∧ ∀ x ∈ xs, 0 < x}
```

It is identical to `Holes.lean:6936` at pin `4d89779d08ba625b8a9e1bc0a52cb42f19c567dc`. Declaration history shows no subsequent change.

The related BMR helper is also unchanged from that pin:

```lean
def bayesianModelReduction (A aPrime a : List ℝ) : List ℝ :=
  (A.zip (aPrime.zip a)).map fun x => x.1 + x.2.1 - x.2.2
```

— M, `Holes.lean:7248–7249`; pin `4d89779d08`, `Holes.lean:6968–6969`.

A separate, unbound algebraic update exists:

```lean
noncomputable def declaredAccumulation (a : Channel → Status → ℝ)
    (ticks : List ((Channel → ℝ) × (Status → ℝ))) : Channel → Status → ℝ :=
  fun channel status => a channel status +
    (ticks.map fun tick => tick.1 channel * tick.2 status).sum
```

— M, `MachineDirichletAccumulation.lean:86–89`, unchanged from `4d89779d08`.

This does not return `DirichletConcentrations` or establish a bridge to that carrier.

**c. Domain assessment**

The named carrier guarantees a finite, nonempty vector of strictly positive concentrations. Empty, zero and negative entries are excluded.

It correctly does **not** require entries to sum to one: Dirichlet concentrations are parameters, not categorical probabilities.

It provides no:

- outcome/state matrix shape or coordinate identity;
- common dimension across different vectors;
- preceding concentration, observations or state-belief inputs;
- outer-product sum or recurrence;
- relationship between previous and subsequent values.

For example, `[1]` and `[1,1]` are independently valid inhabitants. That is not inherently wrong for a general concentration carrier. It becomes a defect when a consumer treats them as same-dimensional vectors without proving that condition.

The A1 BMR issue therefore remains: `List.zip` truncates unequal lengths, and this carrier adds no cross-vector dimension constraint. Nothing has changed since the pin to repair it.

**d. Verdict: CANNOT-TELL**

The **`:carrier-only` status is accurate**. The type supplies the scalar domain for an individual Dirichlet parameter vector, not evidence that equation (21) is implemented or satisfied.

The separate raw outer-product expression shows that the algebra is written elsewhere; it does not close this binding’s accumulation claim.

**e. Citation and imports**

Equation (21) and the A.1 references are correctly identified. The registry’s plain-text `a = a + …` loses the source’s prior/posterior typographic distinction; it should be read as an update, not a self-equality.

`:imports [:o :mu]` describes two inputs to the intended update, but the carrier takes neither. The equation also depends on a prior concentration and a time-indexed sequence/horizon, which those imports do not explicitly represent.

Counterexamples in this report are deductions from the inspected definitions, not newly compiled Lean fixtures. No post-pin change was found that explains or repairs these findings.

| Registry row | Verdict | Finding |
|---|---|---|
| `:policy-set` | **MATCHES** | Exact extensional set of supplied candidate records; no temporal-policy correspondence established. |
| `:depth` | **DIVERGES** | Independent per-term depths replace a common horizon; cited equation (42) has no temporal sum. |
| `:temperature` | **DIVERGES** | Only one branch implements the beta relation; others admit zero or negative temperatures. |
| `:action` | **DIVERGES** | Head selection and individual-candidate argmax do not implement general action-marginal argmax. |
| `:dirichlet-accumulation` | **CANNOT-TELL** | Positive-vector carrier is valid, but accumulation and common dimensions are unstated. |

---

