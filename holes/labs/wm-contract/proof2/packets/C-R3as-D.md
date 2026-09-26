# C-R3as-D — the mean-field state error and the implemented channel error

2026-09-26 — codex-1; review claude-8. Discovery only.

Source pins: futon2 `4e0f5bfd317b19eb86f21138563a56db280a2f2e`;
mathlib4 `4d565382b2934d6b96f5cfe6a0a5ad9ee668fb9c` (contains W5
`2e6638422f`). All Clojure/registry references below are futon2; Lean paths
are under mathlib4 `DarkTower/WarMachine/`. No live load, flight or click.
The numeric probe ran in futon2's own short-lived tooling JVM.

## 1. What prediction error runs

No implementation or consumer of the four-term categorical epsilon_s was
found in `src/futon2/aif/` or `scripts/futon2/report/war_machine.clj`.
Searches included `state-prediction-error`, `statePredictionError`,
`logMessage`, `eps-s`, `compute-prediction-error`, `channel-prediction-error`,
`weighted-error`, and the `Math/log` sites. The logarithms elsewhere implement
entropy, preferences, evidence or the tempered arena update, not the
past/future expected-log transition messages of 4.13. This is a scoped source
finding, not a claim that no external experimental implementation exists.

The implemented error is precisely the legacy scalar channel residual:

- `free_energy.clj:203-278`: observed minus predicted mean (269), reciprocal
  floored variance (270), and their product (278). Missing observations and
  malformed model triples have typed records (250-263).
- `free_energy.clj:280-295`: `channel-prediction-error` obtains a scalar from
  the observation envelope and calls that producer.
- `precision.clj:212-233`: `weighted-error` replaces per-call precision by
  history-tracked precision and multiplies the same residual (224-232).
  It is not a categorical log-message or a derivative with respect to s.
- The live judge calls `belief/predict-observation` at WM 7244-7250,
  `fe/channel-prediction-error` per likelihood channel at 7255-7258,
  filters typed outcomes at 7259-7272, updates precision at 7275-7277,
  and calls `precision/weighted-error` at 7278-7281. The resulting R3d
  aggregate at 7292 drives arena event weights and updates (7317-7335).
  `belief.clj:1199-1228` supplies the moment pairs; 1183-1196 aggregates
  signed weighted residuals (or the single selected channel).

These calls are on the production judge route, including selection inside a
flight: `full_loop_runner.clj:4831-4848` defaults selection-judge to
`wm/generate-war-machine`, and WM 8057 calls `judge`. The one-shot script at
`run_tick_once.clj:291` and scheduled script at `wm_scheduled_run.clj:135`
also call generate-war-machine. This is code reachability, not a new live-run
observation. Flight's separate `conditioning-step` computes evidence and its
negative logarithm (`flight.clj:376-390`), not epsilon_s.

`belief.clj:1126` is the aggregator's explanatory docstring, not the call;
`rollout.clj:150-173` is a move-score classifier's analogy to AC1, not a
prediction-error call at all. The registry correctly labels the residual
row `:legacy-channel-model`, superseded as target by `:state-prediction-error`
(`aif-equations.edn:113-114`). The legacy calculation nevertheless still runs.
The replacement target's whole row is at 499-516; its formal formula at 512
and Lean-at at 515 do not create a Clojure implementation.

## 2. Flight input inventory and the policy-time distinction

The exact flight step is not already evaluating the mean-field error. Its
persisted q is conditioned; W5 evaluates the error at predicted marginals.

| Term | Held / buildable / missing | Evidence |
| --- | --- | --- |
| s_prev | Held as `:s-prev :value`, from this policy's preceding q or the initial target marginal. | `flight.clj:367-368,387`; chain refusal at 332-341. |
| observed o and checked V | Held, including the restriction boundary. | `flight.clj:324-325,383`. |
| A(o\|x) | Buildable for the checked V from held rates; not automatically the full-O A W5 applies. | `flight.clj:369-370,384`; manifest `token-likelihood:191-215`. |
| B into the step | Buildable from persisted precedence and interpretation declarations; the local `pats` and rollout use it already. | `flight.clj:326-327,360-371,385-386`; manifest `cascade-kernel:427-433`. |
| s_tau, predicted | Computed locally as `pushed`, not retained as a named step field. Rebuildable from the same inputs. | `flight.clj:371`; returned fields at 378-390. |
| s_tau+1, predicted | Not computed in this step. Buildable by two-step rollout IF the future policy schedule is declared. | manifest `rollout*:468-477`, `rollout:486-491`. |
| B_next | The kernel is buildable for a declared next precedence/action, but a sequential next-pattern interpretation is NOT the runtime policy rule. | manifest `evaluate-state:412-425`; `rollout:486-489`; flight `constantly pats` at 371. |
| q conditioned on o | Held as `:q`; not W5's s_tau. | `flight.clj:377,388`; exact core `exact_belief_core.clj:29-35`. |
| W5 start belief / plan / horizon | A complete machine-current-belief rollout binding and T>=tau+1 are not established merely by this per-policy flight chain. | Lean `Proof2/StatePredictionErrorAtMachine.lean:77-91,110-121`. |

**Correction to “the next id in precedence”.** A cascade action is the
precedence LIST, not its element at the time index. Each state selects its
first enabled pattern, or identity (`cascade_model_manifest.clj:419-425`).
`rollout` explicitly documents U as precedence lists (486-489), and flight
uses a constant list (371). Even if a persisted list contains several ids,
the second id is not necessarily the next action: a failed stochastic
transition can retry the first; a completed/disabled pattern can be skipped;
different states can select different patterns. The persisted interpretation
map is sufficient to rebuild that cascade kernel, not to infer a new temporal
plan of single patterns.

For a one-pattern list, repeating its cascade is mathematically defined:
retry while enabled, identity after achievement. It is NOT automatically
`outOfHorizon`, and there is no missing second interpretation needed for that
constant-policy extension. What flight does not declare here is a two-step
horizon/plan for an epsilon receipt. If instead an implementation chose a
finite sequential-pattern plan, its one-element horizon would fail W5's
`tau+1 <= T`; such a choice is a different model and cannot be inferred from
list length.

W5 `errorAtStates` at 97-103 uses rollout marginals and B at the time-indexed
policy into/out of tau. Its header 22-32 explicitly says full model O (token
subsets of all V), no C5 restriction. Flight intentionally restricts both
rates and state to checked tokens (369-370). Its record alone does not turn
an unknown unchecked token into a false observation; adopting full-O W5
therefore needs a separately witnessed full observation/model. The row's
formal 512 permits general mean-field marginals; its actual Lean binding
515 chooses predicted marginals, not the conditioned q.

## 3. Real arithmetic and the strict domain

Three qualifications to the request are necessary:

1. Jeffreys rates are strictly between 0 and 1 for each included measured
   cell: `(n+1/2)/(d+1)` (`observation_rates.clj:50-60`), with positive
   counts and prior from `observation_label_reader.clj:7-8,65-74`. Thus the
   finite product over valid measured checked tokens is positive. This
   does not certify A on unmeasured classes/full O. Excluded classes and
   missing observations remain missing; it is too broad to say the A clause
   can never refuse anywhere in production. The probe's requested raw
   rates 1/10 and 1/5 are also positive.
2. B is sparse even at interior theta. `pattern-kernel:306-311` has at most
   two successors and achieved states are absorbing; all other entries are
   zero. Positivity does not fail ONLY at theta endpoints.
3. W5's domain is over ALL x in the fixed carrier, not only current support:
   `StatePredictionError.lean:66-68` requires positive B from each weighted
   predecessor to every x and positive B_next from every x to each weighted
   successor. `StatePredictionErrorAtMachine.lean:102-103` additionally
   requires `forall x, 0 < s_tau(x)`. Dropping zero-current states changes
   the domain and would conceal the endpoint failure.

The following calls were run with `clojure -M /tmp/c-r3as-probe.clj` in
futon2, without loading tests or the runner. These are the substantive calls
(the temporary diagnostic also sums positive-weight log terms and reports
`:log-undefined` before attempting a nonpositive logarithm):

```clojure
(require '[futon2.aif.cascade-model-manifest :as m]
         '[clojure.set :as set])
(def initial {#{:u} 1})
(def rates {:t {:false-neg 1/10 :false-pos 1/5}})
;; For theta = 1/2, then 0:
(def p {:id :p/a :theta theta
        :guard {:status :interpreted
                :clauses [{:present #{} :absent #{}}]}
        :transition {:status :interpreted :produces #{:t}}})
(m/cascade-kernel [p] #{:u})
(m/cascade-kernel [p] #{:u :t})
(m/rollout (constantly [p]) initial 1)
(m/rollout (constantly [p]) initial 2)
(m/token-likelihood rates (set/intersection x #{:t}) #{:t})
```

This explicitly declares a constant-policy two-step extension for the
**diagnostic**, not a new flight rule. Let U=`#{:u}`, T=`#{:u :t}`. These are
the two reachable states. A(U)=1/5, A(T)=9/10. B'=B under that extension.
Exact rational masses were retained; displayed logarithms are doubles
(approximately 15 decimal digits), not exact-real equality claims.

| theta | B(U,U), B(U,T); B(T,U), B(T,T) | s_tau(U,T) | s_tau+1(U,T) |
| --- | --- | --- | --- |
| 1/2 | 1/2, 1/2; 0, 1 | 1/2, 1/2 | 1/4, 3/4 |
| 0 | 1, 0; 0, 1 | 1, 0 | 1, 0 |

At theta=1/2:

- x=U: past=ln(1/2), future=(1/4)ln(1/2)+(3/4)ln(1/2)=ln(1/2),
  minus-current=-ln(1/2). Epsilon(U)=ln(1/5)+ln(1/2)=ln(1/10)
  = **-2.3025850929940455**.
- x=T (IN current support): past=ln(1/2), future contains
  **(1/4)ln B'(U|T)=(1/4)ln 0**, hence no finite epsilon(T).
  A proposed whole-vector receipt must refuse `logUndefined`; it must not
  return the finite U component as though the vector were defined.
- The exact conditioning step is perfectly defined: evidence=11/20,
  q(U)=2/11, q(T)=9/11. It does not need logs of individual B entries.

At theta=0:

- The ONLY current-support state U has past=0, future=0, minus-current=0;
  epsilon(U)=ln(1/5)=**-1.6094379124341003**. There is no support-local
  logarithm failure at U. Claiming one would be false.
- The fixed carrier still contains T. Its past term is **1*ln B(T|U)=ln 0**,
  its future term is **1*ln B'(U|T)=ln 0**, and s_tau(T)=0 violates the
  current-belief positivity clause. Thus the whole-vector W5 refusal is
  again **logUndefined**, even though scanning only rollout support misses it.
- With the rates REQUESTED HERE, evidence=1/5 and q(U)=1. This is NOT the
  contradiction fixture's evidence: that test uses false-positive=0,
  yielding evidence=0 (`flight_conditioning_step_test.clj`,
  `a-contradiction-cannot-become-a-later-prior`). Theta=0 alone is not an
  impossible observation under noisy A.

On the full token powerset carrier there are further unreachable states
(e.g. sets lacking :u); they supply additional zero-current/domain failures.
Already the U,T restriction exhibits the failure, so no such states need
be invented to produce it. A realisation must retain the fixed carrier,
validate the weighted-log and current-positivity clauses, then return a typed
`logUndefined` with offending term/state when they fail. It must neither
clamp nor interpret Lean's `Real.log 0 = 0` as the source's logarithm; the
source has negative infinity, and mixed infinities need not define a real
sum. W5 refuses at 118-120; the row currently omits the current-positivity
clause from its formal domain despite acknowledging it in Lean-at (512,515).

## 4. Recommendation: row documentation only

Recommend **(a)**, C-R3as-I as a registry-only description with `:realised
false`, not an epsilon receipt. No runtime epsilon_s consumer was found;
P12 already computes the exact filtering posterior without this gradient
quantity (`flight.clj:367-390`, shared exact core 29-35). Recording a new
vector which these sparse transitions routinely put outside W5's domain
has no identified reader action, AIF-validity repair, or red-tape-removal
case. Under Joe's stated 2026-09-25 rule, that is insufficient reason to add
a required field/refusal. Naming the absence and the input inventory makes
the registry accurate without changing the machine.

Suggested first paragraph, deliberately narrower than “the WM's belief is
exact”:

> Not realised in code: the flight's token conditioning step implements the
> exact filtering calculation (:state-belief-update, C-R3s-I), while the
> arena retains a tempered status filter. Eq. 4.13 is the retained mean-field
> inference scheme; no consumer of its categorical state error exists on
> the inspected tick/flight paths. Its stationary posterior agrees with the
> exact one-step posterior for a point-mass previous state AND a constant
> future message, under the stated positive-log domain. The existing channel
> residual is a different calculation, not an implementation of this row.

Then include §2's inventory, `:code-at` the read pin and boolean false.
No row edit is made in this D packet.

The extra “constant future message” qualification is required by the actual
Lean theorem: `StatePredictionError.lean:130-138`, especially `hfut` at 136.
A point-mass previous state alone does NOT remove a nonconstant future
message. Further, equality is of the stationary posterior, not equality of
rollout marginals to the posterior nor a claim that epsilon vanishes. At the
stationary posterior epsilon=ln Z (`StatePredictionErrorAtMachine.lean:199-210`).
The row's actual `:eq` at 513 merely names mean-field 4.13, not the stronger
unqualified equality in the request; the equality and reductions are in the
neighbouring exact-update row and underlying theorem.

Option (b) would require an identified consumer, declared full observation,
fixed carrier, temporal plan/horizon, and three predicted marginals, plus
strict log-domain checking. A reader could in principle use it to implement
mean-field optimisation, but that would be a newly chosen inference scheme,
not a diagnostic needed to obtain the existing P12 exact posterior. No such
choice or consumer was found; do not build it speculatively.

## Unowned findings, unchanged

- Precedence order is not a time-indexed sequence of single-pattern actions.
- Sparse/absorbing transitions violate the full log domain at interior theta;
  endpoint-only checks or current-support-only checks would be vacuous here.
- Current-belief positivity is absent from the registry formal domain but
  enforced by W5; point-mass equality also needs a constant future message.
- Positive measured Jeffreys rates do not fill unmeasured/full-O inputs.
- The arena remains tempered, and scalar channel residuals still run despite
  the registry marking their model superseded as the target. This packet
  does not retire them or change their consumers.

Validation: source/Lean inspection and the displayed hermetic arithmetic
probe only. No code, registry, map, runner, live data or futon3c edits.
