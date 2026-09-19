# LF-temperature route-2 declaration

**Decision requested from Joe:** accept or reject the registry declaration in
§4 as the explicit stack reduction for `:temperature`.  Acceptance is a model
choice, not a finding that the three machine laws match the citation.

## 1. Route 1 is foreclosed

Route 1 (`MATCHES`) is foreclosed.  `MachineTemperature.machineTemperature`
dispatches over three `TauMode` arms
(`mathlib4/DarkTower/WarMachine/MachineTemperature.lean:18,42-50`), and
`MachineTemperature.threeLawsDisagree` proves that one shared, fully defined
input produces three different temperatures, `3/10`, `1/2`, and `1/4`
(`MachineTemperature.lean:172-186`).  More decisively,
`MachineTemperature.engineeringGammaIsNotInverseBeta` proves that the spread
and selection-gain-only arms produce precisions `10/3` and `2` when
`1/beta = 4` (`MachineTemperature.lean:205-216`).  Those two arms therefore do
not satisfy the reciprocal-beta equation.  Re-establishing `MATCHES` for the
three-arm declaration would contradict the declaration being audited.

The registry's current Lean binding is narrower.  `PolicyPrecision.PolicyTemperature`
contains `beta : Real` and the proof field `0 < beta`
(`mathlib4/DarkTower/WarMachine/PolicyPrecision.lean:18-22`), and
`PolicyPrecision.policyPrecision` defines `gamma = 1 / beta`
(`PolicyPrecision.lean:24-25`), with the inverse law proved by
`PolicyPrecision.policyPrecision_mul_beta` (`PolicyPrecision.lean:31-34`).
That binding matches the cited variational quantity; it does not make the two
engineering arms match it.

## 2. The three arms, side by side with the cited equation

The cited theoretical law is: policy selection uses precision `gamma`, with
`gamma = 1/beta` after Friston 2017 equation 2.7; equivalently, for the
temperature used in `-G/tau`, `tau = beta > 0`.  This is the scope stated by
`PolicyPrecision.PolicyTemperature`, `policyPrecision`, and
`precisionWeightedPosterior` (`PolicyPrecision.lean:9-25,40-45`).

| `TauMode` arm | What the Lean machine declaration does | Comparison with `tau = beta > 0`, `gamma = 1/beta` | Domain and consequence |
|---|---|---|---|
| `spread` | Returns `tau = spreadTemperature / max(tauMin, selectionGain)` in `MachineTemperature.machineTemperature` (`MachineTemperature.lean:42-46`).  The spread input can itself be constructed as `max tauMin (gRange G / k)` by `MachineTemperature.adaptiveTemperature` (`MachineTemperature.lean:131-144`). | **Deliberate engineering reduction, not agreement.** It replaces inferred/provided beta with a field-dependent spread and a gain divisor.  What is given up is the reciprocal-beta identity and the interpretation of decisiveness as variational policy precision.  `MachineTemperature.engineeringGammaIsNotInverseBeta` supplies the counterexample (`MachineTemperature.lean:205-216`). | The Lean definition has no positivity hypotheses on `tauMin`, `selectionGain`, `spreadTemperature`, or `k` (`MachineTemperature.TemperatureOpts`, `MachineTemperature.lean:27-34`), so zero or negative tau is representable.  Lean's real division is total: at a zero denominator it returns a real rather than refusing.  No `TemperatureError` branch exists for this arm (`MachineTemperature.machineTemperature`, `MachineTemperature.lean:42-50`). |
| `selectionGainOnly` | Returns `tau = 1 / max(tauMin, selectionGain)` in `MachineTemperature.machineTemperature` (`MachineTemperature.lean:42-47`). | **Deliberate engineering reduction, not agreement.** It discards both beta and the score-field spread, making decisiveness a function only of an engineering gain.  It therefore gives up reciprocal-beta semantics.  `MachineTemperature.engineeringGammaIsNotInverseBeta` proves a shared-input instance with `gamma = 2`, not `4` (`MachineTemperature.lean:205-216`). | The Lean definition again has no positivity hypothesis.  `MachineTemperature.gainFloorPreventsDivisionByZero` proves the intended floor behavior only under the explicit assumption `0 <= tauMin` (`MachineTemperature.lean:220-227`); the carrier itself does not enforce that assumption.  Zero/negative tau is therefore admitted by the declaration, with no `TemperatureError` branch for this arm (`MachineTemperature.lean:42-50`). |
| `variationalBetaGamma` | Returns the supplied finite beta unchanged when `0 < beta`; missing, non-finite, zero, and negative beta return `invalidVariationalBeta` in `MachineTemperature.machineTemperature` (`MachineTemperature.lean:47-50`).  `MachineTemperature.betaIsNotFloored` proves a positive beta is returned even below `tauMin` (`MachineTemperature.lean:59-64`). | **Agrees with the cited equation**, under the explicit assumption `beta > 0`. `MachineTemperature.variationalGammaIsInverseBeta` proves the mapped precision is exactly `1 / beta` (`MachineTemperature.lean:196-203`). | Zero and negative beta are excluded and produce the typed error `invalidVariationalBeta`; they do not fall back to an engineering law (`MachineTemperature.machineTemperature`, `MachineTemperature.lean:35-50`; `MachineTemperature.missingBetaNeverFallsBack`, `MachineTemperature.lean:66-70`). |

### Runtime scope at HEAD

The old three-mode Clojure dispatch named by the registry pointers no longer
exists at HEAD.  The current cascade path calls
`cascade-selection/selection-posterior` with a caller-supplied beta, and
`selection-posterior` refuses a missing, zero, or negative value with the typed
refusal `:invalid-temperature` (`src/futon2/aif/cascade_selection.clj:51-72`).
`policy/select-action-cascades` records the accepted beta as
`{:value beta :status :declared}` (`src/futon2/aif/policy.clj:190-217,219-231`).
Thus “the runtime when handed zero/negative tau” has two time-scoped answers:
the three-arm Lean engineering model returns a real without a refusal in its
two engineering arms, while the current cascade runtime has only the
positive-beta boundary and refuses a non-positive beta before selection.

## 3. Scope and assumptions of the reduction

The reduction applies only to the registry symbol `:tau` at the policy-choice
boundary.  It does not identify selection gain, score spread, likelihood
precision, or preference temperature with beta.  It assumes:

1. the selected policy family is scored by a single positive beta;
2. `tau` denotes that beta, and `gamma` denotes `1 / beta`;
3. the spread and selection-gain-only laws are engineering controls with their
   own provenance, not alternate derivations of the cited variational quantity;
4. any use of an engineering law must be labelled by its mode and must not cite
   Friston's reciprocal-beta identity as its derivation; and
5. the current cascade selector's non-positive-beta refusal is part of the
   admitted domain boundary, not an optional fallback.

This declaration does not approve a beta value or a beta-producing process.

## 4. Registry declaration offered for acceptance

Joe is asked to accept exactly this statement for the `:temperature` row:

> **STACK REDUCTION / CHOICE — `:temperature`:** For the policy-choice
> posterior, the stack chooses the variational domain `beta > 0`, defines
> `tau := beta`, and applies policy precision `gamma := 1 / beta`.  This is the
> quantity cited to Friston 2017 equation 2.1 with the reciprocal identity stated
> after equation 2.7.  The `spread` law
> `tau_spread / max(tau_min, selection_gain)` and the `selectionGainOnly` law
> `1 / max(tau_min, selection_gain)` are deliberately excluded from that
> equation: they are engineering reductions, not derivations of beta, and they
> give up variational reciprocal-beta semantics.  The three-arm Lean carrier
> admits zero/negative results in those engineering arms because it imposes no
> positivity hypotheses and gives them no error branch; the current cascade
> runtime does not expose those arms and refuses missing or non-positive beta as
> `:invalid-temperature`.  Every record must therefore distinguish a declared
> beta from a derived beta and must retain the selection law; no engineering
> temperature may be reported as equation-2.7 precision.

**Consequences if accepted.** The registry must cease presenting the three
arms as one equation.  `R14` inherits the positive-beta variational domain and
the obligation to establish actual commitment modulation from the chosen
beta/gamma law; evidence from spread or selection gain cannot discharge R14.
`WM-11` inherits the same declared beta in the full-policy posterior and Bayes
choice, so its posterior/choice witness is conditional on that declared value
and cannot claim an inferred precision.  The current checklist already asks
R14 for an applicable posterior/gain law and actual selection influence
(`CHECKLIST-fundamentals-2026-09-15.md:73`) and WM-11 for a declared prior/choice
rule, complete policy identity, and independent selected-to-enacted
correspondence (`CHECKLIST-fundamentals-2026-09-15.md:46`); this choice fixes the
temperature semantics those obligations consume but does not discharge either
obligation.

**Consequences if the reduction is wrong.** Every posterior mass produced by
`sigma(ln E - F - G/beta)` has the wrong concentration relative to G; policy
mass and the Bayes action marginal can change, so WM-11 can choose a different
first acting pattern.  R14's claimed commitment modulation then measures an
unapproved scalar rather than variational precision.  Downstream
selected-to-enacted evidence may remain internally consistent while witnessing
the wrong choice distribution, so neither a valid posterior normalization nor
an enactment match repairs the semantic error.

Joe can accept the quoted reduction as written or reject it.  A status edit
without Joe's explicit acceptance does not accept this choice.

## 5. The open beta question

`policy/select-action-cascades` **declares** beta rather than deriving it: its
contract says there is no default because beta “is not approved,” requires the
caller to pass it, and records `:status :declared`
(`src/futon2/aif/policy.clj:219-231`; certificate construction at
`policy.clj:190-217`).  Approving beta would require Joe to choose the authority
for its value and initial condition, its allowed scope/lifetime, and the
evidence showing that this value gives the intended R14 commitment behavior
without violating WM-11's posterior/choice contract.  Deriving beta would be a
different commitment: commission and bind a named update/solve law (for
example, the equation-2.7 fixed point), specify its inputs, convergence and
failure/hold behavior, prove or independently audit the binding, and persist
provenance demonstrating that the beta consumed on a decision is the derived
result rather than a caller assertion.  This document does neither and does not
approve beta; that decision remains Joe's.

## 6. Verification boundary

This is a declaration only.  No `.clj` or `.lean` file was changed.  No warrant
is required because there is nothing executable to warrant, and no test or JVM
run was manufactured for this decision artifact.
