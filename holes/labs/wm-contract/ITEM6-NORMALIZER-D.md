prototyping our way forward, not closure of the proof

# ITEM6-NORMALIZER-D — one matrix, three declared readings

Read-only discovery, codex-2 for claude-8, 2026-09-27. Source revision:
**futon2 `44495df3b8ebd532700976af814a82ad7bafce5e`**. The inspected source files
still matched this revision before writing the report. No flight, source,
registry, test, declaration, adapter or scorer change is part of this packet.

The accumulator does **not** choose among the three readings. On the same
non-uniform fixture and the same positive soft-prior proposal, the existing
BMR function gives **0.071195702167**, **0.016404153337**, and
**0.007604599385 nats**, respectively. These are different models of the same
weighted counts, not three equivalent normalizations. None meets the existing
`delta-F <= -3` threshold; that comparison does not authorize adoption.

## 1. Source trace and shapes

All paths and lines below are relative to futon2 at the revision above.

| Hop | Source | Shape and operation |
|---|---|---|
| Raw scan to judge | `scripts/futon2/report/war_machine.clj:8064`, `:8087` | `generate-war-machine` assembles `scan-data` (graph, loop-health, support/attack, mission-triage, frames, annotation graph, etc.) and calls `judge`. |
| Observation | `scripts/futon2/report/war_machine.clj:7156`; `src/futon2/aif/observation.clj:103` | `observe` returns `{channel -> numeric value}` for the 14 channels declared at `observation.clj:11`. Each coordinate has its own projection; there is **no division by the sum across channels**. Ratios such as active/total repositories and firing/total ticks are local channel normalizations (`:129–141`), not a categorical distribution across channels. |
| Observation provenance | `observation.clj:41`, `:84` | Metadata distinguishes missing source from observed zero; `observation-envelope` makes it EDN-safe as `{:channels {channel {:variant :observed :value v} ...}}`. Missing-source numeric zeros are not admitted as observed accumulation input. |
| Entity posterior | `src/futon2/aif/belief.clj:37`, `:370`; `war_machine.clj:7219`, `:7423` | Belief is `{entity-id -> {status -> mass}}`; the seven statuses are addressed/falsified/foreclosed/refined/reopened/spawned/strengthened. The updater documents a normalized single-entity posterior. `wm-belief-pre` is the pre-loop belief; `wm-belief` is the final loop belief. This normalizes **belief**, not the concentration matrix. |
| Accumulation boundary | `war_machine.clj:7424`, `:1532` | Passes the observation, its envelope, pre/post entity beliefs and declared initialization. `accumulation-step-for-tick` requires the observation coordinates to have matching `:observed` receipts (`:1538`), picks one configured entity (`:1544`), and preserves lineage. |
| Input receipt | `war_machine.clj:1574–1580` | `:update-input` records observation, both beliefs, ordered supports, tick/predecessor and model revision. The actual recurrence receives **post** belief (`:belief post`), not pre-belief. Supports are sorted keyword vectors. |
| Initialization | `src/futon2/aif/machine_accumulation.clj:7–21`; `war_machine.clj:1557` | A strictly positive scalar prior fills every `{channel {status concentration}}` cell. Live config `holes/labs/wm-contract/machine-accumulation-config.edn:4` currently declares `1.0`. |
| Update | `machine_accumulation.clj:23–42` | For every row channel c and column status s: `A'[c,s] = A[c,s] + observation[c] * belief[s]`. No axis sum, probability normalization, likelihood or factorization appears. Support, finite nonnegative inputs and predecessor identity are checked. |
| Persistence | `war_machine.clj:1586–1611`, `:7691`; `src/futon2/aif/trace.clj:588–594`, `:664–682` | Records accumulation state, exact update inputs, initialization and receipt. The publication finalizer may replace a stale accumulation with typed absence. Persistence does not flatten, transpose or normalize the matrix. |

With normalized μ, one tick adds total mass `Σ_c o_c`, column s receives
`μ_s Σ_c o_c`, and row c receives `o_c`. A raw scan is a vector of simultaneous
signals, not one sampled channel. `machine_accumulation/step` itself checks
nonnegativity, not `Σ μ = 1`; that belief contract is upstream.

## 2. Fixture and proposal

I inspected the default trace store for a persisted accumulator. The found
state is the fourth top-level record in
`data/wm-trace/wm-trace-2026-09-12.edn`, last tick
`2026-09-12T17:28:09.498448087Z`; every cell is `1.0`. File SHA256:
`3b25d2d43e3ebbcf19fc6f82e103ae49907b81d0cf009fa8d7884e74ef53e175`.
It cannot distinguish the readings and lacks a retained update-input in that
record. I do not treat this historical state as evidence of today's admission
or lineage contract. Instead the following **synthetic accumulated fixture**
is fully specified and calculated using the current pure accumulator.

Use the alphabetical channel order printed below, indices c=1..14, and
alphabetical status order s=1..7. Initialize every cell to exact integer 1;
perform one tick with `o_c = c/14` and `μ_s = s/28`. Thus `Σ μ=1`,
`Σ o=15/2`, and **A[c,s] = 1 + c*s/392**. This is an input-level exact-rational
fixture, not a claim that a live scan or live double-valued belief produced
these exact ratios. The accumulator uses generic Clojure `+` and `*`, so it
preserves the fixture's ratios; production inputs and the current configured
prior often use doubles.

The **same proposal** for all readings changes only the prior at
`[:active-repo-ratio :addressed]` from 1 to 2: `a′ = a + e_(1,1)`.
All prior and posterior coordinates stay positive; dimensions are unchanged.
It is a weak prior preference for that cell, chosen independently of the data
for illustration, not an endorsed machine preference. The reduced posterior
is `A′ = A + a′ - a = A + e_(1,1)`; `A[1,1]=393/392` becomes `785/392`.
“Reduction” here is the existing API's name for a soft-prior constraint, not
literal category merging or a reduction in parameter count.

| Channel (row) | addressed | falsified | foreclosed | refined | reopened | spawned | strengthened |
|---|---:|---:|---:|---:|---:|---:|---:|
| active-repo-ratio | 393/392 | 197/196 | 395/392 | 99/98 | 397/392 | 199/196 | 57/56 |
| annotation-health | 197/196 | 99/98 | 199/196 | 50/49 | 201/196 | 101/98 | 29/28 |
| attack-coverage | 395/392 | 199/196 | 401/392 | 101/98 | 407/392 | 205/196 | 59/56 |
| consulting-pct | 99/98 | 50/49 | 101/98 | 51/49 | 103/98 | 52/49 | 15/14 |
| coupling-density | 397/392 | 201/196 | 407/392 | 103/98 | 417/392 | 211/196 | 61/56 |
| depositing-signal | 199/196 | 101/98 | 205/196 | 52/49 | 211/196 | 107/98 | 31/28 |
| loop-health | 57/56 | 29/28 | 59/56 | 15/14 | 61/56 | 31/28 | 9/8 |
| mathematics-pct | 50/49 | 51/49 | 52/49 | 53/49 | 54/49 | 55/49 | 8/7 |
| mission-health | 401/392 | 205/196 | 419/392 | 107/98 | 437/392 | 223/196 | 65/56 |
| portfolio-pct | 201/196 | 103/98 | 211/196 | 54/49 | 221/196 | 113/98 | 33/28 |
| sorry-count-norm | 403/392 | 207/196 | 425/392 | 109/98 | 447/392 | 229/196 | 67/56 |
| stack-pct | 101/98 | 52/49 | 107/98 | 55/49 | 113/98 | 58/49 | 17/14 |
| support-coverage | 405/392 | 209/196 | 431/392 | 111/98 | 457/392 | 235/196 | 69/56 |
| ticks-firing-ratio | 29/28 | 15/14 | 31/28 | 8/7 | 33/28 | 17/14 | 5/4 |

Factor totals, in the displayed channel/status order:

- `joint`: `{:a [98], :A [211/2], :reduced-a [99], :reduced-A [213/2]}`
- `channel-given-status`: `{:a [14 14 14 14 14 14 14], :A [799/56 407/28 829/56 211/14 859/56 437/28 127/8], :reduced-a [15 14 14 14 14 14 14], :reduced-A [855/56 407/28 829/56 211/14 859/56 437/28 127/8]}`
- `status-given-channel`: `{:a [7 7 7 7 7 7 7 7 7 7 7 7 7 7], :A [99/14 50/7 101/14 51/7 103/14 52/7 15/2 53/7 107/14 54/7 109/14 55/7 111/14 8N], :reduced-a [8 7 7 7 7 7 7 7 7 7 7 7 7 7], :reduced-A [113/14 50/7 101/14 51/7 103/14 52/7 15/2 53/7 107/14 54/7 109/14 55/7 111/14 8N]}`

| Reading | ln B(a) | ln B(A) | ln B(a′) | ln B(A′) | ΔF |
|---|---:|---:|---:|---:|---:|
| joint | -349.954118040770 | -388.204438882833 | -354.539085519441 | -392.860602063670 | 0.071195702167 |
| channel-given-status | -157.865146971864 | -181.365944995952 | -160.504204301479 | -184.021406478904 | 0.016404153337 |
| status-given-channel | -92.109516968142 | -110.158199890384 | -94.055427117197 | -112.111714638825 | 0.007604599385 |

## 3. What the normalizers and scores mean

For a factor with concentration vector x, the mean denominator is `Σ_i x_i`.
The Dirichlet density is `Π θ_i^(x_i-1) / B(x)`, where
`B(x)=Π Γ(x_i)/Γ(Σ x_i)`. The table gives **ln B**, not ln(1/B).
For independent factors these log normalizers add. The `a`, `A`, `reduced-a`,
`reduced-A` totals above give every factor's denominator, in support order.
In particular, under the three readings the full-posterior denominators are:

1. **Joint channel×status:** one total `211/2` for 98 categories.
2. **Channel given status:** seven totals `14 + 15*s/56`, one per column.
3. **Status given channel:** fourteen totals `7 + c/14`, one per row.

`bmr.clj:84–90` implements ln B. At `:108–138` it computes
`ΔF = ln B(A) + ln B(a′) - ln B(a) - ln B(A′)` and accepts at `ΔF <= -3`.
For readings 2 and 3 I call that existing function separately per factor and
**sum ΔF**, not the per-factor accept booleans. All unchanged factors cancel.
This is reduced-versus-full negative-log-evidence difference, under each
stipulated weighted Dirichlet model; a positive score favors the full model.

For the one-cell +1 proposal, the gamma recurrence simplifies each affected
factor's score to `ln(A_total*a_11 / (a_total*A_11))`, independently checking
the implementation's floating-point gamma calculation:

| Reading | Exact log-ratio expression | ΔF, nats |
|---|---|---:|
| One joint distribution | `ln(422/393)` | 0.071195702167 |
| Seven channel-given-status factors | `ln(799/786)` | 0.016404153337 |
| Fourteen status-given-channel factors | `ln(132/131)` | 0.007604599385 |

The same cell has different posterior-to-prior mean ratios because it competes
against 97 cells, 13 channels, or 6 statuses. The numbers illustrate sensitivity
to the model claim; they do not select the claim on Joe's behalf.

## 4. Existing consumers and proposed declaration sentence

- **`machine_accumulation.clj:7–42`:** axis-neutral recurrence with named
  observation rows and state columns. It does not commit to any of the three
  Dirichlet factorizations.
- **`bmr.clj:57–90`, `:92–105`, `:108–138`:** one ordered vector is one
  Dirichlet factor; `dirichlet-moments` sums that vector to `alpha0`. The caller
  chooses whether to flatten or split a matrix. A nested map is not its input.
- **`r17_offline.clj:49–60`, `:74–96`:** delegates to A4a's capability/mission
  corpus adapter and concept reduction; it does not read machine accumulation
  trace fields. Its expected structure has `:capabilities`, `:outcomes` and
  rows of vector concentrations, not the machine's `:support` and nested maps.
- **`a4a.clj:132–164`:** builds capability rows × mission-outcome columns with
  prior `0.1` and discrete observed counts. **`:173–183` concatenates TWO rows
  for a single BMR call** and builds its proposed prior from the observed row
  averages. This is a pair-specific joint vector, not any one of our three
  whole-matrix readings. **`:264–274` computes uncertainty per row** using
  `dirichlet-moments`. Those two uses cannot be cited as a coherent existing
  seven-column factorization of the machine accumulator.
- **`trace.clj:588–594`, `:650–682`:** preserves or removes accumulation data
  with its receipt; it makes no probability/model claim. Inspected production
  references do not connect these fields to `r17-offline` or `bmr`.

Suggested sentence for Joe to accept or amend (a proposal only):

> For this prototype, each status s indexes an independent Dirichlet parameter
> θ_(·|s) over the fourteen channel labels, updated with weighted exponents
> o_c μ_s and scored by summing the seven factors' log-normalizer differences;
> this is a declared weighted parameter model, not a generative likelihood of
> raw scans, and it does not close the BMR model claim in PROOF-2a.

Equivalently the update factor is proportional to
`Π_s Π_c θ_(c|s)^(o_c μ_s)`, with `Σ_c θ_(c|s)=1` per s. The word “weighted”
matters: observed channel amplitudes need not sum to one. It preserves the
observation-given-state orientation without claiming that this channel label
is a sampled categorical observation. Its suitability for the machine remains
an empirical modelling question; a better model can be compared later under
Joe's provisional ruling. The first downstream result should be a replayable
score receipt, with adoption still deferred and positive soft priors retained.

## 5. Unfixed findings and limits

The old A4a adapter cannot simply be pointed at this matrix: it has different
axes, input schema, scalar prior, proposal construction and factorization.
Flattening the 98 values into `bmr` silently chooses reading 1; calling it per
channel row silently chooses reading 3. The recommended reading needs an
explicit column adapter, ordered supports, actual parent prior and an aggregate
score receipt. No such adapter is written here, and no old consumer is changed.

The state records a scalar prior and supports, not a factorization declaration.
Its old seed alone is not a sufficient verified trajectory for restart. The
synthetic recurrence check is evidence about arithmetic only, not observation
provenance, operational continuity, measured A, or proof closure.

## 6. Reproduction and validation

Executed the following read-only script with `clojure -M` at the pinned source.
It invokes the existing accumulator and BMR, verifies the recurrence and
normalization of the supplied μ, and checks each score against its exact
log-ratio within `1e-10`. No test namespace, scorer or repo source was added.
The script's output supplies the full matrix and all tables above.

```clojure
(require '[futon2.aif.machine-accumulation :as acc] '[futon2.aif.bmr :as bmr]
         '[futon2.aif.observation :as obs] '[futon2.aif.belief :as belief] '[clojure.pprint :as pp])
(let [cs (vec (sort obs/observation-channels)) ss (vec (sort belief/status-set))
      o (zipmap cs (map #(/ % 14) (range 1 15)))
      mu (zipmap ss (map #(/ % 28) (range 1 8)))
      init (acc/initialize cs ss 1)
      state (acc/step init {:id "synthetic-1" :previous-id nil :observation o :belief mu})
      A (mapv (fn [c] (mapv #(get-in state [:concentrations c %]) ss)) cs)
      prior (vec (repeat 14 (vec (repeat 7 1))))
      reduced (assoc-in prior [0 0] 2)
      groupings {:joint #(vector (vec (mapcat identity %)))
                 :channel-given-status #(apply mapv vector %)
                 :status-given-channel identity}
      scores (into {} (for [[reading group] groupings
                    :let [a (group prior) big (group A) small (group reduced)
                          results (mapv bmr/bayesian-model-reduction a big small)
                          post (mapv (fn [x y z] (mapv + x (mapv - y z))) big small a)]]
                [reading {:totals {:a (mapv #(reduce + %) a) :A (mapv #(reduce + %) big)
                                   :reduced-a (mapv #(reduce + %) small) :reduced-A (mapv #(reduce + %) post)}
                          :log-B {:a (reduce + (map bmr/log-multivariate-beta a))
                                  :A (reduce + (map bmr/log-multivariate-beta big))
                                  :reduced-a (reduce + (map bmr/log-multivariate-beta small))
                                  :reduced-A (reduce + (map bmr/log-multivariate-beta post))}
                          :delta-F (reduce + (map :delta-F results))}]))]
 (assert (:ok state))
 (assert (acc/recurrence-valid? init {:id "synthetic-1" :previous-id nil :observation o :belief mu} state))
 (assert (= 1 (reduce + (vals mu))))
 (doseq [[reading ratio] {:joint 422/393 :channel-given-status 799/786 :status-given-channel 132/131}]
  (assert (< (Math/abs (- (get-in scores [reading :delta-F]) (Math/log (double ratio)))) 1e-10)))
 (pp/pprint {:channels cs :statuses ss :o o :mu mu :matrix A :scores scores}))
```
