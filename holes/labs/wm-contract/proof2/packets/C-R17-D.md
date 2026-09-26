# C-R17-D — the existing recurrence, its inputs, and the missing consumer

2026-09-26. Discovery only; codex-1 for claude-8 review. No runtime invocation,
code/registry/map change, or new ruling.

Read pins: futon2 `35ba9b31388d4b136adb9f6786a00216b071d9b6`, mathlib4
`4d565382b2934d6b96f5cfe6a0a5ad9ee668fb9c`, futon3c
`3170bcff8744c1d9c7afdf5dd5b77d130607af56`. Futon2 differs from the packet's
`d3a05476f` only in PROOF-2a-PLAN.md at this read. Below, WM means
`scripts/futon2/report/war_machine.clj`; other Clojure paths are relative to
`src/futon2/aif/`, and Lean paths to `mathlib4/DarkTower/WarMachine/`.
Line references describe these pins, not the older registry line numbers.

**Principal finding:** do not commission a second accumulator on the premise
that none exists. `machine_accumulation.clj:7–38` and WM:1528–1559 already
implement an opt-in, trace-carried outer-product recurrence. Its last source
commit is `01a5e8a4a`. It is not the complete positive R17 witness: it loses
observation-absence distinctions, admits a zero prior, derives its carriers
from input keys, and has no BMR consumer. Likewise the September 5 public-arity
probe is historical: `a4a.clj:82–130` now exports state-taking posterior
updaters (source commit `aeb352f87`), although `corpus->concentration` still
recounts. These findings predate this packet; nothing is fixed here.

## 1. The tick's o

The matching carrier is **the local `observation` bound to `(obs/observe
scan-data)` at WM:7093**, not located-token checks. `observation.clj:11–32`
declares, in this order:

```
:loop-health :support-coverage :attack-coverage :mission-health
:stack-pct :consulting-pct :portfolio-pct :mathematics-pct
:active-repo-ratio :sorry-count-norm :coupling-density :ticks-firing-ratio
:depositing-signal :annotation-health
```

This is precisely `Holes.lean:1430–1440`'s fourteen constructors/list, with
camelCase translated to Clojure keywords. The values are numeric channel
levels, intended in [0,1], not booleans and **not a categorical distribution
summing to one across channels**. Read the construction at
`observation.clj:116–146`: some entries are copied directly from scans; only
some computed ratios are capped. The docstring's [0,1] is not a complete
finite/range validator. Do not infer nonnegativity from the name or normalize
these channels silently to make the total-mass theorem apply.

WM:7218–7223 feeds this same map to `fe/channel-prediction-error` for
`belief/channels-with-likelihood`. That set is only **eight** channels
(`belief.clj:919–934`), not a new fourteen-element observation carrier.
`free_energy.clj:280–295` reads the channel's source status before calling
`compute-prediction-error`; absent observations omit, malformed/missing model
members refuse (`:215–278`). Thus the packet's WM:6110 pointer is stale: that
region is now constructor scoring.

In contrast, `observation_checks.clj:551–574` runs C3/C4/C5/C6/C8 over located
**tokens**, returns `{:observed #{...} :results {token r} :refused {token r}}`,
and leaves refused tokens out of results. `flight_runner.clj:77–86` translates
those refusals to `:unknown`, never false. There is no Channel coordinate
mapping for these arbitrary tokens. They must not be substituted for o.

Absence is material: `observation.clj:43–76,103–114` gives missing sources a
legacy numeric 0.0 but attaches `:variant :absent`; `:84–101` provides the
EDN-safe envelope. The **existing accumulation** passes the numeric map
straight through (WM:7351–7357,1539–1555); its step only checks finite
nonnegative numbers (`machine_accumulation.clj:25–28`), so it cannot distinguish
an unobserved channel from measured zero. This is a discovered defect, not an
approved R17 absence policy.

Standing principle: `holes/labs/wm-contract/C130-absence-decisions.md:3–6`
states absence contributes no evidence and support is retained. Its §2
(:23–32) recommends omitting absent observations and refusing missing model
parameters; :94–98 explicitly calls these recommendations, not rulings.
The implemented prediction-error docstring attributes that split to Joe's
2026-09-02 decision (`free_energy.clj:215–229`). **No R17-specific decision
between partial-coordinate accumulation and refusing an incomplete tick was
found.** Options: (a) an explicitly masked update, unchanged prior cells for
absent channels plus retained support/reasons, requiring a partial-observation
contract; or (b) refuse this tick for accumulation, leaving the prior intact.
The current Lean `Trial` is a total real vector, so silently encoding unknown
as zero is not evidence for either option. A pure total-vector kernel should
refuse an incomplete adapter output pending that decision.

## 2. The tick's s / μ and the entity decision already present

`belief.clj:37–49` defines the seven status keywords and a uniform per-entity
posterior. They match `MachineBeliefState.lean:16–25`'s seven statuses exactly.
The textual order matches `Status.all`; **Clojure uses a set**, so there is no
promised iteration order. Use keyed maps or an explicitly named order when
serializing vectors. WM:7156–7161 reconciles fresh entity priors with the
previous trace's `:mu-post` via `belief.clj:510–522`; WM:7350 binds the terminal
posterior after the belief microsteps. `trace.clj:444–445` records both full
entity maps as `:mu-pre` and `:mu-post`.

The code does not leave all three choices equally undecided. The existing
recurrence takes **one configured entity's posterior**: WM:1530,1534–1538
selects `pre` and `post` by `entity-id`; :1554–1555 passes `post` as `:belief`.
This occurs before cascade selection (WM:7351 versus :7463); it is **not** the
chosen mission/target entity. The named configuration
`holes/labs/wm-contract/machine-accumulation-config.edn:1–6` selects
`"arxana/stack/futon-v1/leaf/2/2"`. Its referenced contract,
`CONTRACT-machine-model-v1.md:18–29`, adopts explicit single-entity context
and rejects summing/averaging entity rows as a fallback. This is the available
binding authority; it does not establish that a selected cascade target is
that entity. The same contract's R2 (:31–63) uses a tagged outcome vocabulary,
so it must not be cited as authority equating that vocabulary with Channels.

Index consequences, without choosing a new machine model:

- One named entity context: `Channel × Status`, with entity/model identity
  attached to the population; this matches the supplied machine Lean axes.
- One independent matrix per entity: `Entity × Channel × Status`, requiring
  a family of Lean instances and rules for new/disappearing entities.
- A pooled status vector: still `Channel × Status`, but requires an explicit
  pooling rule and a new/changed contract; current R1 forbids fallback pooling.
- Target-entity posterior: `Channel × Status` within each named target context;
  requires a target-to-entity binding and carrier continuity, neither supplied
  by selecting a mission keyword alone.

There are aggregates, but not a ready-made pooled `Status → real` R17 input:
`belief.clj:642–666` averages entity expected-health scalars and entropies;
`:705–736` computes open/healthy/nondormant-mass predictions. These are
channel predictions, not a persisted aggregate categorical μ. Existing
accumulation state itself omits entity identity (`machine_accumulation.clj:30`),
although the trace's update-input carries it (WM:1549); changing entity context
must not silently continue one matrix merely because its status keys match.

## 3. Consumers and the concentration census

The registry's model-reduction site is offline, as its own row says
(`aif-equations.edn:480–497`). Read implementation:
`r17_offline.clj:50–62` accepts an explicit concentration object or recounts
its corpus; `:75–78` calls `a4a/reduce-concepts`. The corpus adapter
`a4a_substrate.clj:45–74` reads substrate capability entities, mission documents,
capability hyperedges and discharges. **I did not invoke that adapter.**

`a4a.clj:132–165` starts afresh with prior 0.1, sorts capability/mission IDs,
and folds corpus events through `observed-posterior`. Its shape is
`{:capabilities [id ...] :outcomes [mission ...]
  :concentrations {capability [alpha-by-outcome-position ...]} :prior 0.1}`.
`reduce-concepts` (:211–257) scores pairs of capability rows;
`score-pair` (:178–183) concatenates their posteriors, builds an independent
uniform 0.1 full prior, and pools rows for the reduced prior. BMR proper
(`bmr.clj:108–134`) takes three positive finite equal-length vectors, computes
`A' = A + a' - a`, and the log-beta difference/−3 threshold. Its validators
are at :53–81. It cannot consume a nested Channel/Status map as-is.

A **structural** adapter can declare channels as row IDs, statuses as a fixed
column vector, and map every keyed cell into that order. But feeding that
object into a4a would then merge *channels as capabilities* and still use its
hard-coded 0.1 prior, not the recorded a_prior. That is a model change, not an
innocent rename. A direct BMR adapter must instead declare the intended model
reduction hypothesis, the exact common coordinate basis for all three arrays,
and carry the actual prior, posterior and reduced prior. Flattening all 98
cells versus separate outcome vectors per status also needs a declared
Dirichlet factorization; neither may be inferred from vector length. No
semantically justified Channel/Status-to-capability/mission mapping was found.

Census: searched `concentration`, `:a-conc`, `a4a/`, then the discovered aliases
and recurrence names in WM, full_loop_runner, and all `src/futon2/aif/`.
Actual readers and routes (not merely every comment match):

| Site | What is read / reachability |
| --- | --- |
| `machine_accumulation.clj:20–36,37–38`; WM:1539–1555 | Prior keyed matrix read by `step`; `recurrence-valid?` compares another step. Opt-in judge path; no BMR handoff. |
| `a4a.clj:82–120,122–130,165` | Shared update reads one prior row and adds a positive weight to one cell; hypothetical/observed wrappers; corpus recount invokes observed wrapper. This supersedes the September 5 assertion that no public updater takes a state. |
| `a4a.clj:167–183,202–209,211–257` | Concentration input, pairwise BMR and merged concept rows; offline model reduction. |
| `a4a.clj:265–274,291–301` | Concept concentrations → Dirichlet stddevs → uncertainty for produced capabilities. |
| `bmr.clj:84–106,108–134` | Log-beta, moments, model reduction over ordered positive vectors. |
| `r17_offline.clj:21–36,50–62,75–96,98–101` | Parent/result structures, input, reduction and replay carry concentration objects. No WM/full-runner call found. |
| `eig_shadow.clj:66–75,79–98` | Reads both updater results, asserts equal matrices and hashes them; record-only shadow, not tick scoring. No WM/full-runner call found. |
| `learning_trial_ledger.clj:294–376,393–435,438–476,487–559,579–625` | Separate B-learning carrier: family × achieved/not × singleton-state, Jeffreys counts, validation, close snapshot, update and theta. `pattern-theta` reads ledger counts to compute theta, not the Channel/Status matrix. |
| `full_loop_runner.clj:4412–4426,4438,4558–4605`; WM:6696–6712,6843–6854 | Close retains B-learning concentration snapshot; subsequent scoring reads per-pattern theta. Distinct from R17 A-learning. |
| `cascade_prior.clj:129–162`; `habit_prior.clj:100–119` | Separate policy-habit concentrations `alpha + counts` normalized into log priors; not a Channel/Status array. |
| `trace.clj:588–592`; WM:7617–7619 | Persists/returns accumulation state and provenance, not a reduction consumer. |

Excluded lookalikes: `a4a_substrate.clj:280–298` computes pattern-grain
uncertainty from local constellation/scope edge counts; its caller
`actuator_a6.clj:127` does not consume accumulated a. Other `a4a/` matches in
that adapter (:112,138,182–186,210,256–273) are document/ID transformations,
not concentration readers. `belief.clj:718` says “concentration” in an English
description of categorical healthy mass, not Dirichlet parameters.

## 4. Carry and first prior: precedent is already implemented

The best-matching precedent is **the existing trace recurrence**, not a new
second owner. WM:7143–7147 reads the last of twelve recent records;
`trace.clj:739–754` returns chronological records from newest daily files.
WM:1540 reads `[:accumulation-state]`, passes its concentrations onward, and
refuses a previous record lacking it with `:accumulation-migration-required`.
Trace writing is conditional on `trace?` (WM:7704–7728);
`trace.clj:588–592` preserves `:accumulation-state`, `:accumulation-update-input`
and initialization. Paths are `data/wm-trace/wm-trace-YYYY-MM-DD.edn`
(`trace.clj:63–87`), appended through `append-indexed-trace!` (:646–674).
The quoted :421–424 is **write-side μ field mapping**, not the prior-read rule.

First prior is not entirely unnamed: the checked-in live config (:3–4) declares
`:authority :declared :prior 1.0 :model/revision "wm-status-v1"`. WM:1543–1548
requires declared initialization on cold start. `machine_accumulation/initialize`
uses it explicitly, although wrongly admits zero for the positive-Dirichlet
contract. This is a named configured prior, not a numerical prior fixed by
Eq. 21; the 0.1 in a4a is for a different population. An unconfigured API
should report `{:absent :no-prior-concentration}`, not reuse either number
silently. Existing code uses `:accumulation-initialization-required` instead.

Route distinction: `run_tick_once.clj:272` and `scripts/wm_scheduled_run.clj:133`
actually load `wm/accumulation-config`; the latter writes its trace at :148.
**The current full-loop/flight selection route does not:**
`full_loop_runner.clj:4833–4841` forwards neither accumulation option nor
`:trace?`; WM:7351 only accumulates when trace or entity is supplied. The tick
record assembly (:674–741) is not a reader/owner for a and does not retain it.
Do not equate “there is code” with “the flight ran it.” If the flight cannot
use the same explicitly scoped durable trace chain, a dedicated owner patterned
on `observation_label_store.clj:56–105,107–148` is a possible **separate packet**
(locked, atomic, explicit initialization); it must not create two authorities.
Also address existing history-read error swallowing at WM:7145–7146 before
claiming a failed read cannot become a cold start.

“Trial” has no automatic flight/click identification. Lean
`DirichletLearning.lean:33–38,60–65` defines a list of tick pairs; **T is its
length**, not cascade anticipation depth or the belief microstep count.
Existing `step` contributes once per opt-in judge invocation, after microsteps
(WM:7350–7357), with run/scan identity and previous trace identity. A flight
contains successive clicks on one target (`flight.clj:1–14,167–176`;
`flight_runner.clj:64–75` runs one opportunity per click). Grouping those clicks
as one trial is a possible explicit batching rule, not a ruling found here.
The current trace predecessor is global to the trace directory, not flight-
or entity-scoped. A flight trial and a trace trial must not be conflated.

## 5. C-R17-I signature, tests, and available live evidence

Propose the pure arithmetic API, **as a repair/consolidation of the existing
kernel rather than a parallel authority**:

```clojure
(dirichlet-accumulation/accumulate a-prior ticks) ; => a-post or typed refusal
;; a-prior, a-post: {channel {status positive-finite-number}}
;; ticks: [[{channel nonnegative-finite-number}
;;          {status nonnegative-finite-number}] ...]
```

Require exactly the fourteen Channel keys and seven Status keys, full rows,
and immutable declared population/entity/model context at the caller. Return
`{:status :missing :kind :trial-not-nonnegative :tick i :path ...}` for a
negative entry (separately identify nonnumeric/nonfinite input),
`:prior-not-positive` for zero/negative prior cells, and `:carrier-mismatch`
for missing/extra/mismatched keys. Missing prior is typed, no initialization
inside accumulate; no clamp, no renormalization. Validate the whole input
before returning an update. Entity/provenance/chain checks belong in the
existing tick adapter and durable owner. The Clojure kernel can use exact
integers/ratios; live doubles require explicit numeric-tolerance reporting,
not claims of exact real equality from rounded results.

The supplied Lean basis is `DirichletLearning.lean:16–21,34–65,76–84,98–109,
117–132,145–164,181–185`. `MachineDirichletAccumulation.lean`'s full header
(:5–48) was read; its requirement is pointwise agreement on the machine's
Channel/Status carriers (:87–91,189–210), plus provenance and consumption
(:225–240), not just proving a helper adds numbers.

Acceptance tests, pure arithmetic with real numeric inputs and complete 14×7
carriers, then separate production-adapter tests:

1. Positive nonuniform prior; one-hot o and s add exactly 1 to one named cell,
   every other cell unchanged (`accumulate_onehot`). Empty ticks return that
   identical prior (`accumulate_nil`).
2. Two different soft ticks: batch equals first-then-second, using the actual
   returned posterior as the second prior (`accumulate_append`).
3. Exact normalized rational o and s for T ticks increase the sum by exactly T.
   Also test nonnormalized input's total increase as Σ(sum o × sum s), not T.
   The live channel map is not normalized, so the conditional theorem does
   not license an unconditional production +T claim.
4. o = 1/2 on two channels, zero elsewhere; s one-hot: two cells each rise
   1/2. Contrast **actual** `a4a/corpus->concentration` with one corpus record:
   it changes only one cell (even the newer updater's weighted record changes
   one cell). This pins the machine counterexample without mocking the recount.
5. Same corpus/tick-derived data recounted twice yields the same object;
   two distinct explicit priors accumulated with that same tick yield distinct
   posteriors retaining their prior difference. State that recount has no
   a-prior argument; do not invent a production function that discards one.
6. Negative tick, zero prior, extra/missing carrier keys, and a tagged absent
   observation must hit their real refusals; no partial store publication.
   Reuse the existing trace chain checks, and add entity-continuity and
   prior-read-failure controls before any claim of production correspondence.

**No suitable live spike pair exists at the pinned futon3c revision.** Census:
58 files, 27 EDN files under `holes/labs/M-wm-wiring/spike/`; none contains
`:mu-pre`, `:mu-post`, `:observation-envelope`, or `:accumulation-state`.
Read example `tick-run-record-2026-09-26-flight-7f89646a-click-1.edn`, SHA256
`a8e04fb97e58808e8fabdb4ab771f3c414b4181ef82dac336729dd472a18d816`:
its top-level map has `:declaration-reads`, `:decision`, `:scan-report`, etc.,
but neither `:observation` nor `:mu-post`. Its located-check evidence does not
supply the missing Channel/Status pair. The schema that *could* supply it is
a full trace: `[:observation-envelope :channels]`, `[:mu-post entity-id]`,
and `[:accumulation-update-input]` if enabled (`trace.clj:444–447,588–592`).
No flight/click was run to fabricate such a witness. The arithmetic tests can
be hermetic; none of the requested spike files can currently pin them to a
live tick's complete o and contextual μ.

## Review boundary / unowned findings

Before C-R17-I claims end-to-end realization, settle missing-coordinate handling,
entity/population continuity, and the reduction hypothesis/adapter. Reuse or
replace the existing kernel deliberately; do not add an unnoticed second one.
Unfixed: stale registry/Lean prose and old arity probe; existing zero-prior and
absence-provenance gaps; input-derived supports; trace read failures swallowed;
flight route not forwarding accumulation configuration; no BMR consumption of
the trace-carried matrix. The separate B-learning, habit, and EIG concentration
readers above are not evidence that the R17 A-learning handoff is closed.
