# C-R17-BMR-D — the consumer of the carried concentrations

Date: 2026-09-26. Author: codex-1. Discovery only; claude-8 reviews.

Read pins:

- futon2 `9de9f023cebab7236777ffe2a2ed27c430093ce6`.
- mathlib4 `4d565382b2934d6b96f5cfe6a0a5ad9ee668fb9c`.
- futon3c `dc784b631e6f372d5f595035d2a02c00a0163479`.

Paths below are relative to futon2 unless prefixed with another repo.
Local literature was read, not inferred from the registry's reference prose:
`holes/labs/wm-contract/refs/friston2018bmr.txt:351-428` (section 5 and Table 1).
Its SHA256 is `a261a875e07cdded45e810e8da2cc7838e7dabcff606830dcd7eb1c6d261161f`;
the PDF's is `4d55b4997dfcc28a03ac823f052d956a5f1e68eef6668b873ac3aeb8c888069f`.
`refs/README.md:3-8,16` explains that these retrieved files are gitignored and
records the PDF checksum/source. No flight, click, shared-JVM load, live-data
write, code change, registry change, or map change was made.

## 1. What a reduction means, and what has not been declared

**What a4a actually does.** `src/futon2/aif/a4a.clj:132-165` builds named
capability rows, each a vector over the sorted mission-id outcomes, using a
uniform 0.1 prior (`:11-13`). `reduce-concepts` enumerates every unordered pair
of capability rows (`:211-230`). For rows x and y, each of length m:

- Full prior a is the length-2m vector filled with the namespace's `prior`
  constant; full posterior A is `concat x y` (`score-pair`, `:178-183`).
- Reduced prior a' is `concat p p`, where `p_j = (x_j + y_j)/2`
  (`pair-reduced-prior`, `:173-176`). It is a repeated shared average of the
  POSTERIOR rows, not their sum and not a componentwise decrease of a.
- The BMR kernel returns A' = A + a' - a and the log-beta evidence difference;
  acceptance is `delta-F <= acceptance-threshold`, with threshold -3.0
  (`bmr.clj:108-138`, constant at `:4-8`).
- Accepted pairs are unioned, with deterministic representative ids
  (`a4a.clj:192-200,231-246`). A resulting concept row is
  `0.1 + sum_member(row_member_j - 0.1)`, not A' from the pairwise score
  (`class-row`, `:202-209`, installed at `:248-257`). Pair scoring and the
  subsequent aggregation are distinct operations.

`r17_offline.clj:50-62,75-96` canonicalizes either a corpus or a concentration
map, calls that reducer, and records proposals and resulting structure.
Its receipt calls the proposal `:merge-capability-concepts` (`:39-48`). It
performs no substrate writes (`:1-6`). This proposal family belongs to the
capability/mission model, not the Channel/Status model.

**What Lean permits.** `mathlib4/DarkTower/WarMachine/Holes.lean:7276-7277`
defines `DirichletConcentrations` as a nonempty list of strictly positive
reals. `DirichletConcentrationsWitness.lean:7-10` supplies only the example
[2,1]; it declares no machine carrier or merge family. `Holes.lean:7308-7310`
computes A' componentwise with zip. `ModelReduction.lean:21-54` requires all
four vectors positive and of equal length and requires A' = A + a' - a.
**There is no condition a' <= a**, no channel/status names, and no proposal
search in those declarations. The caller supplies a' and proves the resulting
A' positive. `BayesianModelReductionWitness.lean:13-25` exemplifies count
preservation ([10,4], [1,1/100], [1,1] gives [10,301/100]); it is not a general
restriction to decreasing priors.

**No machine proposal family found.** The registry's `:model-reduction`
(`holes/labs/wm-contract/aif-equations.edn:481-498`) supplies the formula,
imports concentrations, and binds the threshold; it does not name channel or
status merges. C453 (`C453-citation-verification.md:30,53-56`) corrects the
required inputs and sign/threshold attribution, not the proposal family.
Friston section 5 (`refs/friston2018bmr.txt:373-385`) illustrates pruning a
cause/outcome mapping by changing a prior coordinate; Table 1 (`:403-428`)
supplies the distribution-specific formula. Neither chooses Futon's proposal
family. The separate offline parent declaration explicitly declares no model
content (`r17-parent-model.edn:25-29`).

Two natural possibilities, neither adopted here:

1. **Channel merge:** identify two observation rows. If enacted, this means
   fewer distinguishable observation channels and requires a rule for the
   merged observation and likelihood. It changes the named carrier currently
   fixed by `observation.clj:18-32` and `Holes.lean:1430-1440`.
2. **Status merge:** identify two status columns. If enacted, this means a
   coarser entity-status alphabet, requiring corresponding belief, likelihood,
   transition and event interpretation changes. The current alphabet is
   `belief.clj:37-42` / `MachineBeliefState.lean:16-21`.

Neither can be passed to the existing BMR composite by simply deleting a row
or column: that violates sameLength. Scoring needs an explicitly declared
same-carrier prior transformation representing the proposed constraint;
actual carrier contraction belongs to a later model revision. Copying a4a's
averaging construction would choose that transformation without authority.
Likewise, the paper's zero-prior pruning example is outside the positive
Lean/runtime domain; silently inserting epsilon would not implement it.

## 2. Adapter, coordinate order, and the three concentration inputs

`machine_accumulation.clj:18-21,33-39` stores named nested maps and explicit
support vectors. A proposed reversible adapter would flatten by named lookup,
not map iteration, into 98 coordinates indexed by `7*i+j`, with channel outermost:

```
channels = [:loop-health :support-coverage :attack-coverage :mission-health
            :stack-pct :consulting-pct :portfolio-pct :mathematics-pct
            :active-repo-ratio :sorry-count-norm :coupling-density
            :ticks-firing-ratio :depositing-signal :annotation-health]
statuses = [:spawned :refined :strengthened :addressed
            :falsified :foreclosed :reopened]
```

Channel order is the Clojure vector `observation.clj:18-32`, matching Lean's
`Holes.Channel.all` (`Holes.lean:1437-1440`) under the named keyword/constructor
translation. Status membership is Clojure's UNORDERED `belief/status-set`
(`belief.clj:37-42`); the order above is Lean's `Status.all`
(`MachineBeliefState.lean:20-21`), not sorted keywords. The current accumulator
initializes supports in sorted-key order (`war_machine.clj:1547-1548`), so its
stored vector order differs from both declaration orders. This is not a reason
to reorder values positionally: lookup each [channel status] by name and record
the adapter's ordered coordinate list. The inverse rebuilds those same names.

Proposed adapter scope: two pure conversions plus shared carrier validation;
require exactly those 14 row keys and seven keys in EACH row, unique support
entries, all 98 finite positive cells, and matching named coordinates for a,
A, a' and A'. Return typed `:carrier-mismatch` for missing/extra/duplicate
coordinates or a wrong vector length. Never zip before validation.
`ModelReduction.lean:30-35,58-71` explicitly supplies the no-truncation
obligation and its bad case. Runtime `bmr.clj:57-82,120-131` already refuses
unequal lengths and nonpositive/nonfinite inputs/results, but its exceptions
have diagnostic fields rather than the proposed `:carrier-mismatch` kind.
The adapter must translate failures into the consumer's receipt, not let them
abort selection. The runtime converts inputs to doubles (`bmr.clj:63`), so
numerical comparison tests need a stated tolerance; key/order round trips are
exact.

The inputs have different authorities:

- **a:** the declared initialization prior, 1.0 on each coordinate, from
  `machine-accumulation-config.edn:1-6`, model revision `wm-status-v1`, entity
  `arxana/stack/futon-v1/leaf/2/2`. The single-entity binding is explicit in
  `CONTRACT-machine-model-v1.md:18-29`. This is the origin prior for the
  accumulated population, not automatically the preceding tick's posterior.
- **A:** the current successful accumulation outcome's
  `[:state :concentrations]` (`war_machine.clj:1558-1563,1576-1584`); after
  persistence it is the corresponding trace's
  `[:accumulation-state :concentrations]` (`trace.clj:590-594`).
- **a':** an explicit proposal's same-carrier reduced prior. No declaration
  choosing it for the machine was found in the sources in section 1.

**The receipt alone cannot supply a and A.** It contains state digest and
identities, not concentrations (`war_machine.clj:1579-1584`). Kernel `step`
drops `:initialization` from its returned state (`machine_accumulation.clj:33-39`);
the adapter returns initialization separately only on the cold-start tick
(`war_machine.clj:1563`). Thus later state records do not independently retain
the origin prior. Reading today's config and assuming it is the historical
prior is unsafe: the lineage work must retain or resolve a named immutable
initialization authority. BMR input should explicitly cite that authority,
entity/revision, state digest, and proposal authority. Missing provenance is a
typed absence, not a new prior or a search for a convenient older state.

**A statistical decision remains beyond serialization.** One call of
`bmr/log-multivariate-beta` on the flattened vector uses one normalization over
all 98 cells (`bmr.clj:84-90`). A conditional model with an independent
Dirichlet over channels for each status would instead use seven length-14
vectors and a sum of log-normalizers; a status-given-channel model would have
14 length-7 vectors. These generally give different evidence. The matrix
recurrence does not choose a factorization (`machine_accumulation.clj:23-39`),
and the flat Lean composite does not choose one either. The requested flat
adapter is small, but calling it an observation-likelihood BMR would require
an explicit statistical declaration. Do not hide that choice in flattening.

## 3. Consumer site, persistence and enactment boundaries

Source census: `rg 'r17|reduce-concepts|bayesian-model-reduction|:accumulation-state'
over `src/` and `scripts/` finds the offline run at `r17_offline.clj:78`, the
pair scorer at `a4a.clj:183`, and another reducer entry through
`concept->stddev` (`a4a.clj:265-274`); none accepts the trace-carried matrix.
`war_machine.clj:1540` reads that matrix only as the next accumulation prior;
`:7652` and `trace.clj:590-592` forward/persist it. No a4a/BMR call occurs in
`war_machine.clj` or `full_loop_runner.clj` at the read pins.
The positive requirement is `MachineDirichletAccumulation.lean:43-47` and
`Holes.lean:8106-8132`: realised outcomes, a nonempty contributing history,
and consumerInput = updated. A pure calculation over a seed alone does not
satisfy that run requirement.

**Proposed placement, not an implementation:** after
`war_machine.clj:7387-7392` produces the accumulation outcome, a pure consumer
can take that SAME outcome plus the declared origin prior and proposal set.
Do not reread "latest trace" to obtain the current A: the current trace is not
written until `:7740-7754`, and another route could intervene. At replay time,
read the specific trace named by tick id and verify its state against the
receipt's SHA256 (UTF-8 `pr-str` of the whole state, no newline;
`:1565-1584`). Hash verification and origin-prior lineage are separate needs.

Attach a record-only result after selection beside the accumulation receipt
(the existing attachment is `with-accumulation-receipt`, `:1590-1595`, applied
at `:7718-7719`), for example:

```
[:decision :model-reduction]
{:status :computed
 :state-sha256 ... :tick-id ... :entity ... :model/revision ...
 :prior-authority ... :proposal-authority ... :coordinates [...]
 :proposals [{:merge ... :delta-f ... :accepted? ...}]}
```

If accumulation is absent, provenance cannot be resolved, or the proposal set
is undeclared, record `{:status :absent :reason ...}` and the relevant upstream
receipt; do not compute from an older state, invent proposals, initialize, or
gate the run. The exact proposal key is contingent on section 1's declaration;
`:merge` does not itself decide which family to use. Map runtime `:delta-F` and
`:accept?` explicitly if the receipt uses `:delta-f` / `:accepted?`.

Persistence is not automatic on every route:

- Trace preserves the complete decision (`trace.clj:137-159,464`), so the new
  subtree survives there; the accumulation matrix remains separately stored
  (`:588-594`). Scheduled route writes that judgement at
  `scripts/wm_scheduled_run.clj:150-154`.
- Flight's record selects decision keys (`full_loop_runner.clj:731-733`);
  `:model-reduction` would need adding. Currently it retains `:accumulation`
  but no BMR result. Flight forwards neither the trace gate nor accumulation
  configuration (`:4837-4841`), so the present accumulation outcome is typed
  `:accumulation-not-configured` (`war_machine.clj:1569-1571`).
- The one-shot receipt also selects only `:accumulation`
  (`scripts/futon2/run_tick_once.clj:232`); retaining BMR there is another
  explicit key addition, not a second computation.

At futon3c's pin, a case-insensitive search for `r17`, `accumul`, or `reduction`
over ALL 4,784 lines of `holes/labs/M-wm-wiring/wm-flight-wiring.edn` returns no
matches. Its box collection starts at `:35`. There is no existing R17 box to
claim. A later map pass needs explicit producer/consumer sites: accumulation
and reduction boxes as needed, and wires accumulation -> reduction -> record.
The proposed score is never an instruction to mutate the model. The registry's
actual action decision is `:action`, R16, importing Q-pi and pi
(`aif-equations.edn:434-445`), with candidate space `:policy-set`, R6
(`:325-335`). No separate equation row authorizing a machine channel/status
schema migration was found. Such structural application needs its own declared
proposal/application decision; a BMR threshold alone cannot supply it.

## 4. Order of work and the seed correction

**Recommend BMR-I after LINEAGE-I and FLIGHT-I, and only after the proposal
family and statistical factorization are declared: close each hole separately
and record absence rather than add an undeclared consumer.** No flight is
needed to build or test those packets; this is an order of implementation,
not a request to run production to discover the design.

The premise that a seed offers "one tick of data" is false for the retained
seed. I read ALL EDN forms in `data/wm-trace/wm-trace-2026-09-12.edn`, not just
the first form. At source line 4 (zero-based record index 3), the state-bearing
record has:

```
:timestamp "2026-09-12T17:28:09.498448087Z"
:run/id absent
:accumulation-update-input absent
:accumulation-state :last-tick "2026-09-12T17:28:09.498448087Z"
:accumulation-state :initialization {:authority :declared :prior 1.0}
:accumulation-initialization
  {:authority :declared :prior 1.0 :model/revision "wm-status-v1"}
98 concentration cells; distinct values #{1.0}
```

Thus A = a for this declared seed. For every admissible a', A' = a', and the
four log-beta terms cancel to Delta-F = 0 (the equation is
`bmr.clj:121-138` / `Holes.lean:7291-7295`). The -3 rule accepts none. This is
zero-data evidence, not one observed update. It cannot witness the nonempty
and positive-contribution conditions in `Holes.lean:8121-8132`.
A later hypothetical single-tick fixture would be useful but must be named
hypothetical, not relabelled as that live seed.

Smallest later implementation scope after the declarations: a pure named
matrix/vector adapter and proposal scorer, then a separate record-only judge
attachment plus persistence keys. Tests should pin the 98-coordinate round
trip with distinct cell values, reordered map insertion, missing/extra cells,
wrong vector lengths, nonpositive inputs/results, A' - a' = A - a, identity
proposal Delta-F = 0, the zero-data seed result, and a real initialized/stepped
kernel outcome reaching the scorer unchanged. Persistence tests must reread
the receipt and state digest from disk; an absent accumulation or undeclared
proposal must leave selection pr-str-identical. A run-bearing claim waits for
LINEAGE-I's declared restart and FLIGHT-I's forwarding; it is not inferred from
those arithmetic tests.

## Findings outside this packet, left unchanged

- Origin initialization is not retained in every accumulated state; later
  BMR must not silently replace it with the current config (section 2).
- The machine proposal family and joint-versus-conditional factorization are
  undeclared in the inspected authorities (sections 1-2).
- `a4a/score-pair` uses the namespace's 0.1 prior even when the supplied
  concentration map carries a different `:prior` (`a4a.clj:167-183`), and
  pairwise a' is posterior-derived. Passing the machine's 1.0-prior matrix
  through this function would therefore not be an adapter.
- `concept->stddev` is an additional entry into a4a reduction
  (`a4a.clj:265-274`), not a consumer of accumulated Channel/Status parameters.
- The registry's model-reduction code pointer still names a4a lines 126-159;
  actual pair scoring/reduction is now 173-257. The accumulation row's prose
  still describes the old scheduled-id throw (`aif-equations.edn:478`), while
  `wm_scheduled_run.clj:108,135` now mints and forwards run-id. No registry edit.
- The Lean comparison module's header says no public a4a function accepts a
  prior state plus observations (`MachineDirichletAccumulation.lean:22-28`);
  current `a4a.clj:82-87,122-130` has state-taking update/observed-posterior APIs.
  They remain a different capability/mission model; the stale statement is
  not repaired here.
