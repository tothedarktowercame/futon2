# C-R3s-D — exact filtering, counted A, and the two runtime beliefs

2026-09-26. Discovery only; author codex-1, review claude-8.

Read pins: futon2 `beb4ff05ec368ea3497c38543ca309865ca0a741`;
mathlib4 `4d565382b2934d6b96f5cfe6a0a5ad9ee668fb9c`.
At completion futon2 HEAD is `19579548c90a636bf8c8a10a8cb9b7b153264a74`;
the intervening diff changes only PROOF-2a-PLAN.md. All source and registry
line references below therefore apply to both futon2 pins. Paths beginning
`DarkTower/` are in mathlib4; other paths are in futon2. This note changes
no runtime, registry, or map, and makes no claim of a newly executed flight.

## 1. The flight step implements the exact Bayes calculation

Yes, on valid declared inputs, `flight/conditioning-step` implements the
row's prediction, likelihood, evidence and posterior calculation. It is a
consumer of the already existing exact kernel, not a new update authority.
It does **not** implement all the additional operations of the row's current
Lean binding, `Proof2.BeliefStepAtMachine.machineStep`.

| Row term | Runtime evidence | Agreement and limits |
| --- | --- | --- |
| `s_prev` | `src/futon2/aif/flight.clj:289-296,358-359`: last present step's `:q` for this policy, otherwise target marginal of `[:decision :initial-belief-receipt :value]`; target marginal at 280-287 sums masses of equal projected token sets. | A distribution over token sets, not an entity-to-status map. The chain is policy-keyed. |
| `pred(x) = sum B(x|s0) s_prev(s0)` | `flight.clj:351-362`: interpret the persisted precedence, then `(manifest/rollout (constantly pats) s-prev 1)`. `cascade_model_manifest.clj:437-466` sums transition products; 468-491 iterates it. | Exactly one transition of the interpreted cascade. For a vector of patterns this is first-enabled semantics (412-425), not sequential execution of every pattern in the precedence. |
| stochastic `B` | `cascade_model_manifest.clj:293-311`: `pattern-kernel` validates rational theta in [0,1], places theta on the produced state and 1-theta on the old state (or mass 1 if they coincide); 412-425 uses identity when none is enabled. | Each valid state's outgoing row sums to 1. Invalid theta is a typed refusal. The declared pattern adapter/default provenance is at 282-291. |
| `A(o|x)` | `flight.clj:360-361`: rates restricted to checked V and state intersected with V. `cascade_model_manifest.clj:191-215`: product of token false-negative/false-positive factors. | A categorical law over subsets of checked tokens; unknown/unobserved tokens are marginalised, not treated as false. Rates are validated as exact probabilities (176-187,203-208). |
| `P(o) = sum A(o|x) pred(x)` | `flight.clj:367`: `(reduce + 0 ... (* mass (lik st o)))`. | The same evidence as the exact update, on valid inputs. |
| `s_next = A pred / P(o)` | `flight.clj:368`; `cascade_model_manifest.clj:1226-1255` calls `exact-belief-core/condition-predicted`. `exact_belief_core.clj:29-35` computes weights, their sum and the quotient. | Exact integer/ratio arithmetic for the posterior; distribution and likelihood checks at core 5-10,12-28. |
| typed refusal at zero evidence | `exact_belief_core.clj:32-33`; manifest 1251-1255 returns `:kind :zero-predictive-probability`. `flight.clj:370,379-381` places this inside `:q`, with `:f :contradiction`. | **Outer step remains `:status :present`.** `:contradiction` is the F field, not the outer refusal kind. Missing measured cells have a separate outer `:refused :reason :unmeasured-class` at 340-350. |
| `F_B.2(s_next) = -ln P(o)` | `flight.clj:381` evaluates negative log evidence; `cascade_free_energy.clj:6-22` explicitly explains exact-posterior attainment of this bound, and 90-103 evaluates the corresponding evidence expression. | Yes, this is the attained B.2 value under the exact posterior assumptions. Runtime does not evaluate arbitrary-q B.2 and prove its minimiser. The logarithm converts to double. |

`DarkTower/WarMachine/ExactBeliefTrajectory.lean:47-59` defines the same
prediction/evidence/quotient; 99-110 and 128-140 supply the minimiser iff and
bound. `PolicyRollout.lean:23-33` declares finite S, O, U with B indexed
`u, old-state, new-state`, nonnegative normalised B rows and A distributions;
38-40 fixes the sum's orientation. Sparse Clojure distributions are the
finite-support representation of that calculation.

There is no requirement here that S be the seven statuses.
`Proof2/BeliefStepAtMachine.lean:49-60` is generic in S and O;
its counted specialisation at 215-226 explicitly uses `Finset V` for BOTH
state and observation. Flight's token subsets agree with that carrier grain;
its restriction to checked V is explicit at `flight.clj:321,360-361` and
corresponds to the restricted-token likelihood construction discussed at
`Proof2/KernelAtCounts.lean:238-254`. It is not an arena status update.

The full `machineStep` first obtains the machine's action and world-state
observation law (53-55), rejects an impossible realised-world observation
(56), then calls exactUpdate (58-60). Flight takes policy/precedence from the
persisted enacted entry; it performs no separate world-state observation-law
check. Its caller attaches the result after enactment (`flight.clj:548-560`).
This is evidence for the filtering component, not a complete runtime witness
of `noAction/processImpossible/updateRefused` in that closed loop. Also the
named Lean function contains no precision/zetabar argument or posterior
exponent. Its counted version installs raw `tokenLikelihood` via
`KernelAtCounts.lean:260-266`. The request's phrase about applying zetabar
must not be attributed to this function without a separate site.

`src/futon2/aif/free_energy.clj:1-12` is explicitly the engineering diagnostic
module, with its old variational scalar removed. It is not the B.2 evaluator
for this claim; `cascade_free_energy.clj` is the relevant evidence above.

## 2. Which state the row means, and what the arena actually does

The mathematical row does not force the arena carrier. The registry's `:s`
is an exogenous hidden state (`aif-equations.edn:86`); `:q0` already names the
constructed cascade's token belief (88-90). `:state-belief-update` at 532-538
states exact categorical filtering, not gradient dynamics or per-policy
future/smoothing beliefs. The `:belief-state` formal at 237 specifies one
belief along executed actions and q0 already conditioned on the initial
record (P4: do not reuse o0). Its P12 re-audit at 231-233 expressly names
TokenState/cascadeKernel/tokenLikelihood. W8's counted specialisation is
unambiguously token subsets, as above.

But the same registry's `:belief-state :code` at 235 names the arena's
entity belief **and explicitly acknowledges the separate token belief**.
Thus node R3 and the shared letter s are insufficient grounds to identify
these carriers. The neighbouring `:belief-update` (203-216) is the older
precision-weighted mu update, superseded as WM target; the
`:state-prediction-error` row (499-516) retains the mean-field stationary
message scheme. Neither makes the two runtime beliefs interchangeable.

The arena has `{entity {status probability}}`: seven statuses at
`belief.clj:37-42`, per-entity update at 399-432, event fold at 434-445.
Its A is a hand-set seven-event-by-seven-status table (`a-matrix-v0`,
138-143), materialised at 127-136 and normalised into `observation-model-v1`
at 199-205. This is **not** the fourteen observation channels' token-class
false-negative/false-positive kernel. B is the identity status transition at
214-226. `predict-step` (297-308) is B times q; `update-step` (310-323)
multiplies that prediction by `A(event|status)^kappa`, where
`kappa = log(1+w)/log(2)`, then normalises. `categorical-filter-step`
(325-346) assembles those defaults. At w=1 the exponent is 1; generally
it is a tempered update, not the row's unmodified A update. Moreover
`normalise` at 51-60 returns a uniform prior on zero total, whereas the row
requires typed refusal at zero evidence. Even that special untempered case
does not give the entire refusal contract.

The actual judge uses this variant: `war_machine.clj:822-838` selects arena
likelihood mode (default `:aif`), and 840-846 calls `belief/update-belief-batch`
with only that mode. The driver sign/magnitude at 7292-7294 and annealing at
7301-7304 determine event weights; 7317-7335 constructs and applies those
events. Counted token rates are not supplied as the arena A. The fourteen
channels influence the driver/events; they are not the categorical outcomes
of this seven-event status likelihood.

## 3. The counted rates reach the flight and cascade, not the arena A

The path is already implemented:

1. `observation_label_reader.clj:77-83` reads one store snapshot and calls
   `rates-inputs`; 7-8 declares minimum 5 and the Jeffreys prior; 31-74
   filters mechanisms, counts seen subjects, requires both cells, and emits
   labels/subjects/prior plus exclusions. Current-mechanism matching is at
   10-17. This is measured class evidence, not a status likelihood table.
2. `war_machine.clj:6116-6124` obtains/reuses the view and selects its inputs;
   judge entry at 7121-7122 captures it before assembly/scoring. The public
   decision wrapper at 7026-7027 also captures it for a direct decision call.
   `cascade-lane` at 5947-5949 passes labels, subjects AND prior to
   `observation-rates/sourced-rates`.
3. `measured-a-version` (6361-6430, sourcing at 6399-6402) uses the same
   per-target inputs, qualifies rate keys with target (6410-6413), and
   records rates, measurement, classes and hash (6424-6429). Assembly at
   6911-6916 passes the same view to every target and writes `:measured-a`.
   `observation_rates.clj:50-60` computes the explicit prior's posterior
   mean; `sourced-rates` at 218-260 exposes the token kernel and provenance.
4. `full_loop_runner.clj:731-733` keeps `:measured-a` in the persisted
   decision. `flight.clj:318-324` reads that record, 340-350 rejects checked
   unmeasured classes, 360 restricts rates to checked V, and 361 supplies
   token-likelihood. `flight_conditioning_step_record_test.clj:13-53`
   already exercises a real stored/read record and rates 1/12 with 0/5
   measurement in both cells; 67-73 changes persisted rates and checks that
   the evidence changes. This note read that test; it did not rerun it.

The absence of an arena connection is concrete: its call site passes only
likelihood mode (WM 833-846), and `belief.clj:325-338` selects its declared
A/B defaults. `machine_parameters.clj:34-90` is another finite-kernel
parameter-posterior calculation (weighted likelihood 63-65, posterior
75-83), not an injection into that arena call. Neither the view capture nor
writing measured-A silently replaces the arena kernel.

There is also a numeric Lean/runtime distinction beyond carrier selection.
`Proof2/AdjudicationCounts.lean:120-122` uses n/d, and 63-68 explicitly excludes
priors/posterior means from its claims. W8 calls that machineKernel
(`BeliefStepAtMachine.lean:221-224`). The runtime reader supplies Jeffreys:
with five admitted present and five admitted absent, no errors, each cell is
(0+1/2)/(5+1)=1/12, whereas raw n/d is 0/5=0. Thus W8's counted kernel is not
a proof that these runtime numeric rates are identical. General exactUpdate
still applies to a valid stochastic A supplied with those rates; the
counting-to-kernel bridge needs the declared prior represented explicitly.

## 4. Recommended C-R3s-I scope

Recommend recording the existing flight filtering component truthfully, then
adding only a write-only tick token-posterior receipt once its predecessor,
executed action and new observation are explicitly bound; do not substitute
class rates into the arena's different carrier or change scoring by inference.
This follows one authority per value and the requirement that an absent input
be recorded, not supplied by a guessed mapping or timing convention.

Proposed first sentence of the row's future `:code`:

> Exact categorical prediction/conditioning at the persisted measured token
> rates is implemented on the flight side per enacted policy by
> flight/conditioning-step (flight.clj:298-381), through
> cascade-model-manifest/rollout and exact-update; the tick's arena filter
> uses a different, event-weight-tempered status kernel and does not consume
> those rates.

The row currently has no `:code` or `:realised` field (532-538). If the added
`:realised` is boolean for the full named machineStep/tick obligation, use
false with the above partial realisation stated; a true value cannot mean
both the existing algebra and the unimplemented world-law/closed-loop checks.
The full `machineStep` and the partial filtering calculation must remain
distinguishable in the evidence.

A small implementation packet can reuse the exact kernel; it should not
write another Bayesian filter. `exact_belief_core.clj:12-35` is already the
normalisation/refusal authority, manifest 1226-1255 the token wrapper;
`exact_belief_adapter.clj:8-45` also already provides finite A/B prediction
and conditioning. A new receipt adapter should name: prior token distribution
and predecessor identity; actually enacted action/transition declaration;
new located observation and checked set; snapshot/rates identity; predicted
belief, evidence, posterior or typed refusal. Place it beside the decision
without feeding it back to selection, in the write-only pattern expressly
used at `war_machine.clj:6907-6916` for F1a-2 measured-A.

Two declarations are needed before calling this the tick's filtering update:

- Temporal link: the current selected policy is a future action, not the
  action that produced the already observed input. Bind the previous enacted
  action and a genuinely new observation. Do not recondition q0 on its own
  initial record (registry 237, P4). The flight has this ordering at
  `flight.clj:548-560`; the tick packet needs its own named receipt inputs.
- Carrier: for the W8 row, preserve token subsets. If the requested result is
  instead arena statuses, declare how token truth states/observations and
  class conditional errors induce `A(event|status)`; 5/5 token counts alone
  do not supply that seven-by-seven table. That is a model declaration and
  calibration task, not a one-line A replacement.

Acceptance should pin a persisted counted-rate fixture, the exact predicted
state/evidence/posterior, and zero-evidence refusal through the real shared
kernel; additionally missing predecessor/action/new observation must remain
typed absence. Compare all existing scoring and arena-belief numbers
`pr-str`-identically with the receipt enabled/disabled. No new receipt should
be taken as evidence of the full process-law check until that check is wired
and tested separately. This note does not implement or pre-authorise those
model/time choices.

## Findings outside this packet, left unchanged

- The raw-count Lean versus Jeffreys runtime difference above; minimum and
  active-mechanism admission are additional reader rules, not established
  by merely instantiating W8's raw counter.
- Arena zero-sum normalisation returns uniform (`belief.clj:51-60`), contrary
  to the exact row's zero-evidence refusal; already stated by the registry's
  neighbouring note, not repaired here.
- Flight's zero-evidence step has outer `:status :present` (370) and a refused
  `:q` (368,379). `prior-q` (289-296) selects by outer status and policy only;
  it can therefore select that refusal map as the next prior. This is a
  source-level continuation concern, not a claim of an observed failed flight.
- Flight computes p-o before the exact wrapper validates likelihood results
  (367 before 368). If an invalid persisted rate makes token-likelihood return
  a refusal map, arithmetic can encounter that map first. The normal measured
  source validates rates; malformed-record behaviour needs its own test.
- Generic machineStep selects an action and checks the realised-world law;
  this conditioning component does neither. No zetabar application appears
  in its named Lean definition. These are scope distinctions, not proofs
  that no other machine component implements those operations.

Validation: read source/Lean/tests at the stated pins and checked the
intervening futon2 diff. Markdown-only deliverable; no production/test JVM,
flight, click, live load, registry update or map change was needed.
