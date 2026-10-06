# Consumption-counted progress: four, alcove, and extended arrangements — 2026-10-06

Reproduce in a fresh process from `/home/joe/code/futon2`:

```sh
clojure -M scripts/consumption_four_numbers.clj
```

## Source pins

| input | sha256 |
|---|---|
| `holes/labs/wm-contract/NOTE-outer-cascade-as-pasted-blends-2026-10-05.md` | `b39352d125a41dbb3fc2577e09618e13014e27699592da02b22f3323055cf945` |
| `holes/labs/wm-contract/proposals/meta-outer-policy-cascade-extended-proposal.edn` | `8ac54050a9d1b4469cc1e4676733452fa9283396f95af2d8219970b5c0f1e5ef` |
| `holes/labs/wm-contract/REPORT-diamond-four-numbers-2026-10-05.md` | `be2e5e3265d4bb997ded3c14f65fba86fa33e910971f8f12ee35e34f71e81417` |
| `holes/labs/wm-contract/REPORT-extended-diamond-validation-2026-10-06.md` | `931c3f8deed81539c4f0c07529a6ac4571d374340abff6b2efef79a9acf7f184` |
| `scripts/consumption_four_numbers.clj` | `07afbc19c0431ed74eb4773c645a0214ad509b2a542e8d8dc23bf4f258a269bf` |

## Step 1: can the existing scorer accept consumption progress as data?

No. `score-arranged` has only `[target id cascade]` and `[target id cascade ledger-root]` at `src/futon2/aif/cascade_shape_g.clj:211-217`; it constructs `progress-tokens` from every `:pattern-done` token at lines 250-255. The lower-level `rank-cascade-actions` accepts an `:observation-model` option (`cascade_observation_scoring.clj:220-249`), but `progress-outcome` still computes the count as a static intersection of state with that model's `:progress-tokens` (`observation_model.clj:274-280`). There is no function argument for an externally supplied set-by-τ or count-by-τ series. Consequently rule (ii) below records the exact consumption series and evaluates each observed `[count want-met?]` with the scorer's own private `progress-preference`, but it does **not** report that preference probability as G. Consumption G is typed `:unavailable :scorer-has-no-consumption-observation-input`.

## Measurement rule

The current-rule column is the scorer's exact stochastic rollout: each entry is `τ:[expected fired-pattern count, P(want met)]`. The consumption-rule column is the maximal no-simultaneity typed trace: a producer becomes complete at the first later step when a firing unit consumes one of its products, or at the production step if the want consumes it. An arrangement is refused when its first arrangement frontier contains a unit whose typed needs are not held. R8(a) assigns no separate cost to inert products.

| arrangement | fired-pattern series (i) | G (i) | consumption series (ii) | per-step C probability for (ii) | G (ii) | log2 extensions | G(i)+ordering |
|---|---|---:|---|---|---|---:|---:|
| diamond | `τ1:[0.500000,0.000000] τ2:[1.250000,0.000000] τ3:[1.937500,0.062500] τ4:[2.515625,0.203125]` | 1.279742336087 | τ1:[0,false] τ2:[1,false] τ3:[4,true] τ4:[4,true] | τ1:C[0 false]=0.001389456790 τ2:C[1 false]=0.003776935143 τ3:C[4 true]=0.560546876289 τ4:C[4 true]=0.560546876289 | `:unavailable` | 1.000000000000 | 2.279742336087 |
| chain-1 | `τ1:[0.500000,0.000000] τ2:[1.000000,0.000000] τ3:[1.500000,0.000000] τ4:[2.000000,0.062500]` | 1.516585504479 | τ1:[0,false] τ2:[1,false] τ3:[4,true] τ4:[4,true] | τ1:C[0 false]=0.001389456790 τ2:C[1 false]=0.003776935143 τ3:C[4 true]=0.560546876289 τ4:C[4 true]=0.560546876289 | `:unavailable` | 0.000000000000 | 1.516585504479 |
| chain-2 | `τ1:[0.500000,0.000000] τ2:[1.000000,0.000000] τ3:[1.500000,0.000000] τ4:[2.000000,0.062500]` | 1.516585504479 | τ1:[0,false] τ2:[1,false] τ3:[4,true] τ4:[4,true] | τ1:C[0 false]=0.001389456790 τ2:C[1 false]=0.003776935143 τ3:C[4 true]=0.560546876289 τ4:C[4 true]=0.560546876289 | `:unavailable` | 0.000000000000 | 1.516585504479 |
| bag | `τ1:[2.000000,0.062500] τ2:[3.000000,0.316406] τ3:[3.500000,0.586182] τ4:[3.750000,0.772476]` | 0.392913018968 | `{:status :refused, :kind :typed-needs-unsatisfied-at-first-frontier, :tau 1, :units [{:unit "meta/fill-meta-policy-slots", :needs #{:field-observation}} {:unit "meta/injury-routes-to-self-heal", :needs #{:field-observation :injury-observation}} {:unit "meta/minimise-g-over-filled-meta-policies", :needs #{:filled-candidates :typed-exclusions :rearm-slot :admitted-support}}]}` | — | `:unavailable` | 4.584962500721 | 4.977875519690 |
| house of alcoves | `τ1:[0.500000,0.000000] τ2:[1.500000,0.000000] τ3:[2.437500,0.046875] τ4:[3.203125,0.173828] τ5:[3.785156,0.346924]` | 1.006883494078 | τ1:[0,false] τ2:[1,false] τ3:[4,true] τ4:[4,true] τ5:[4,true] | τ1:C[0 false]=0.001212244242 τ2:C[1 false]=0.002697899176 τ3:C[4 true]=0.219746231441 τ4:C[4 true]=0.219746231441 τ5:C[4 true]=0.219746231441 | `:unavailable` | 3.000000000000 | 4.006883494078 |
| extended shape | `τ1:[0.500000,0.000000] τ2:[2.000000,0.000000] τ3:[3.382813,0.007813] τ4:[4.442139,0.067139] τ5:[5.224663,0.193413] τ6:[5.795177,0.357677] τ7:[6.202315,0.522628]` | 0.732060845075 | τ1:[0,false] τ2:[1,false] τ3:[6,true] τ4:[6,true] τ5:[6,true] τ6:[6,true] τ7:[6,true] | τ1:C[0 false]=0.000960273481 τ2:C[1 false]=0.001700447433 τ3:C[6 true]=0.218772914047 τ4:C[6 true]=0.218772914047 τ5:C[6 true]=0.218772914047 τ6:C[6 true]=0.218772914047 τ7:C[6 true]=0.218772914047 | `:unavailable` | 6.906890595609 | 7.638951440684 |
| extended bag | `τ1:[3.500000,0.007813] τ2:[5.250000,0.133484] τ3:[6.125000,0.392696] τ4:[6.562500,0.636501] τ5:[6.781250,0.800722] τ6:[6.890625,0.895621] τ7:[6.945313,0.946578]` | 0.238107431451 | `{:status :refused, :kind :typed-needs-unsatisfied-at-first-frontier, :tau 1, :units [{:unit "meta/read-the-item-sheet", :needs #{:field-observation}} {:unit "meta/read-revealed-attention", :needs #{:field-observation}} {:unit "meta/read-library-coupling", :needs #{:field-observation}} {:unit "meta/fill-meta-policy-slots", :needs #{:field-observation}} {:unit "meta/injury-routes-to-self-heal", :needs #{:field-observation :injury-observation}} {:unit "meta/minimise-g-over-filled-meta-policies", :needs #{:filled-candidates :typed-exclusions :rearm-slot :admitted-support :item-sheet :item-attention}}]}` | — | `:unavailable` | 12.299208018387 | 12.537315449838 |
| extended chain | `τ1:[0.500000,0.000000] τ2:[1.000000,0.000000] τ3:[1.500000,0.000000] τ4:[2.000000,0.000000] τ5:[2.500000,0.000000] τ6:[3.000000,0.000000] τ7:[3.500000,0.007813]` | 1.310492019077 | τ1:[0,false] τ2:[1,false] τ3:[6,true] τ4:[6,true] τ5:[6,true] τ6:[6,true] τ7:[6,true] | τ1:C[0 false]=0.000960273481 τ2:C[1 false]=0.001700447433 τ3:C[6 true]=0.218772914047 τ4:C[6 true]=0.218772914047 τ5:C[6 true]=0.218772914047 τ6:C[6 true]=0.218772914047 τ7:C[6 true]=0.218772914047 | `:unavailable` | 0.000000000000 | 1.310492019077 |

Counts in the consumption series are completed producers, not raw consumed-token cardinality; this preserves the scorer's outcome shape `[completed-pattern-count want-met?]`. The extended coupling square and the alcove both produce tokens with no consumer, so neither producer completes.

## Control check

The existing scorer was called unchanged for every row. The original four-node controls are diamond 1.2797, bag 0.3929, and both chains 1.5166, reproducing `REPORT-diamond-four-numbers-2026-10-05.md` (1.2797, 0.3929, 1.5166). The values 1.3105 and 0.2381 named in the measurement brief are the **extended** chain and extended bag controls from the 2026-10-06 report; this run gives 1.3105 and 0.2381, respectively. Thus all three specifically requested checkpoints—1.2797 / 1.3105 / 0.2381—are reproduced without conflating the two reports.

## Prediction versus result

| arrangement | prediction made before run | result |
|---|---|---|
| diamond | [0 1 4 4]; G(ii) unavailable | [0 1 4 4]; G(ii) unavailable |
| chain-1 | [0 1 4 4]; G(ii) unavailable | [0 1 4 4]; G(ii) unavailable |
| chain-2 | [0 1 4 4]; G(ii) unavailable | [0 1 4 4]; G(ii) unavailable |
| bag | refused at τ=1 | refused at τ=1 |
| house of alcoves | [0 1 4 4 4]; no gain over diamond | [0 1 4 4 4]; G(ii) unavailable |
| extended shape | [0 1 6 6 6 6 6]; G(ii) unavailable | [0 1 6 6 6 6 6]; G(ii) unavailable |
| extended bag | refused at τ=1 | refused at τ=1 |
| extended chain | [0 1 6 6 6 6 6]; G(ii) unavailable | [0 1 6 6 6 6 6]; G(ii) unavailable |

The bag behavior under R8(a) is **refused at τ=1**: `fill`, `injury`, and `minimise` are arrangement roots but have unmet typed needs. The extended bag is refused for the same reason. The alcove's completed-producer series is the diamond's `[0 1 4 4]` padded to its five-step horizon, so its inert output adds no progress. Both chains equal the diamond on consumption progress because their extra ordering edge carries no needed token; only their separately reported ordering term differs.

## Proposed source change (not applied)

The smallest honest interface is not a precomputed scalar series: it is an outcome function evaluated on every predicted state, because risk requires the full predictive distribution. A proposed change is:

```diff
--- a/src/futon2/aif/observation_model.clj
+++ b/src/futon2/aif/observation_model.clj
@@
-(defn- progress-outcome [{:keys [progress-tokens want]} state]
-  [(count (set/intersection progress-tokens state))
-   (set/subset? want state)])
+(defn- progress-outcome [{:keys [progress-tokens want progress-outcome-fn] :as model} state tau]
+  (if progress-outcome-fn
+    (progress-outcome-fn {:model model :state state :tau tau})
+    [(count (set/intersection progress-tokens state))
+     (set/subset? want state)]))
@@
-(defn- progress-predictive [model belief]
+(defn- progress-predictive [model belief tau]
@@
-            (update out (progress-outcome model state) (fnil + 0) mass))
+            (update out (progress-outcome model state tau) (fnil + 0) mass))
--- a/src/futon2/aif/cascade_shape_g.clj
+++ b/src/futon2/aif/cascade_shape_g.clj
@@
+;; Add an options arity and place its :progress-outcome-fn in model.
```

That callback would also require the rollout state to retain consumed-token provenance; today's state is only a held-token set. The proposal therefore names the missing interface without pretending that a static `:progress-tokens` replacement implements R8(a). No file under `src/` was edited.
