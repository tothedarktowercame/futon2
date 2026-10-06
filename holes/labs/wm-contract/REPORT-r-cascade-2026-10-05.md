# R-node cascade discovery — 2026-10-05

Reproduce in a fresh process:

```sh
cd /home/joe/code/futon7
bb scripts/r-cascade.bb
```

## Inputs

| input | SHA-256 |
|---|---|
| `/home/joe/code/p4ng/empirics-futon/aif-lean-dag.edn` | `65c292cdc5f8b8a7c9645033c28b7959d43cf0bd8d16701df3a8e81c6c72c7dd` |
| `/home/joe/code/p4ng/empirics-futon/control-stages.edn` | `9d87c52e1e87f17fd3283a8beb2e73e8ae37852bc78c4ebcaa5f641af20b8f47` |
| `/home/joe/code/p4ng/sec-catalog.tex` | `00a5151dc05b83d93ea4ec2bbf5f23cd0f68bb77a88583378c4983be5eed3016` |

The input has 17 drawn equation nodes and 43 registry-versus-Lean edges.

## SCCs before the cut

- `R1`, `R16`, `R17`, `R2`, `R3`, `R3a`, `R4`, `R5`, `R6`, `R7`, `R8`

## Applied cut

| edge | terms | class | reason |
|---|---|---|---|
| `R16` → `R1` | `:u` | `:imported` | `:cross-tick-state` |
| `R16` → `R2` | `:u`, `:world` | `:term-present-not-imported` | `:cross-tick-state` |
| `R3` → `R1` | `:s-next` | `:imported` | `:cross-tick-state` |
| `R4` → `R1` | `:A`, `:B` | `:absent` | `:learned-parameter` |
| `R4` → `R2` | `:A`, `:B` | `:absent` | `:learned-parameter` |
| `R4` → `R3` | `:A`, `:B` | `:absent` | `:learned-parameter` |
| `R4` → `R3a` | `:A`, `:B` | `:absent` | `:learned-parameter` |
| `R6` → `R4` | `:pi`, `:r` | `:imported` | `:split-unit` |
| `R8` → `R6` | `:F-pi` | `:imported` | `:split-unit` |
| `R7` → `R3` | `:Pi` | `:absent` | `:fit-feedback` |
| `R8` → `R3` | `:F-pi` | `:absent` | `:fit-feedback` |

The two `:split-unit` edges are removed only for the unsplit refusal pass and are restored with routed endpoints in the explicit split pass. No proposed edge was rejected.

## Unsplit refusal and explicit split

After the cut, the unsplit graph still has these cycles and therefore refuses as a cascade: `R4`, `R7`; `R17`, `R6`.

- `R6` → `R6-candidates` + `R6-select`; `R6-candidates` carries `:interp`, `:pi`, `:r`; `R6-select` carries `:E`, `:F-pi`, `:G`, `:Q-pi`, `:tau`. Added internal edge `R6-candidates` → `R6-select` carrying `:pi`.
- `R4` → `R4-model` + `R4-rollout`; `R4-model` carries `:A`, `:B`; `R4-rollout` carries `:mu`, `:interp`, `:T`, `:pi`, `:r`, `:rates`, `:Q-o-pi`. Added internal edge `R4-model` → `R4-rollout` carrying `:A`, `:B`.

The split graph is acyclic. CascadeOrder meets below use paths `meet → left/right`; ConstructionReceipt meet witnesses use paths `left/right → meet`. The receipt therefore records these results under `:cascade-order-meets` and leaves its Lean-shaped `:meets` empty rather than forging reversed paths.

## Split-DAG descent edges

| edge | token set | class | carried in Lean |
|---|---|---|---|
| `CTAU-TOKEN` → `R5` | `:C-tau` | `:absent` | false |
| `R1` → `R17` | `:mu` | `:term-present-not-imported` | true |
| `R1` → `R3` | `:mu` | `:term-present-not-imported` | true |
| `R1` → `R3a` | `:mu` | `:imported` | true |
| `R1` → `R4-rollout` | `:mu` | `:imported` | true |
| `R13` → `CTAU-CLASS` | `:T` | `:consumer-unplaced` | true |
| `R13` → `CTAU-TOKEN` | `:T` | `:imported` | true |
| `R13` → `R4-rollout` | `:T` | `:imported` | true |
| `R14` → `R6-select` | `:tau` | `:imported` | true |
| `R17` → `R6-select` | `:E` | `:imported` | true |
| `R17` → `SCAN` | `:a-conc` | `:imported` | true |
| `R19` → `R5` | `:C`, `:U-t` | `:term-present-not-imported` | true |
| `R2` → `R1` | `:o` | `:imported` | true |
| `R2` → `R17` | `:o` | `:term-present-not-imported` | true |
| `R2` → `R3` | `:o` | `:imported` | true |
| `R2` → `R3a` | `:o` | `:imported` | true |
| `R2` → `R4-rollout` | `:interp` | `:imported` | true |
| `R2` → `R6-candidates` | `:interp` | `:imported` | true |
| `R2` → `R7` | `:o`, `:ref-label` | `:absent` | false |
| `R2` → `R8` | `:o` | `:imported` | true |
| `R2` → `SCAN` | `:o` | `:absent` | false |
| `R3a` → `R3` | `:eps` | `:imported` | true |
| `R3a` → `R7` | `:eps` | `:absent` | false |
| `R4-rollout` → `R5` | `:A`, `:Q-o-pi` | `:absent` | false |
| `R4-model` → `R7` | `:A` | `:term-present-not-imported` | true |
| `R4-rollout` → `R8` | `:A` | `:absent` | false |
| `R5` → `R6-select` | `:G` | `:imported` | true |
| `R6-select` → `R16` | `:Q-pi`, `:pi` | `:imported` | true |
| `R6-candidates` → `R17` | `:pi` | `:imported` | true |
| `R6-candidates` → `R4-rollout` | `:pi`, `:r` | `:imported` | true |
| `R6-candidates` → `R8` | `:pi` | `:imported` | true |
| `R7` → `R17` | `:wc` | `:term-present-not-imported` | true |
| `R7` → `R4-rollout` | `:rates` | `:imported` | true |
| `R8` → `R6-select` | `:F-pi` | `:imported` | true |
| `R6-candidates` → `R6-select` | `:pi` | `:imported` | true |
| `R4-model` → `R4-rollout` | `:A`, `:B` | `:absent` | false |

## Meets (CascadeOrder.IsMeet)

| left | right | meet |
|---|---|---|
| `CTAU-CLASS` | `CTAU-TOKEN` | `R13` |
| `CTAU-CLASS` | `R16` | `R13` |
| `CTAU-CLASS` | `R4-rollout` | `R13` |
| `CTAU-CLASS` | `R5` | `R13` |
| `CTAU-CLASS` | `R6-select` | `R13` |
| `CTAU-CLASS` | `R8` | `R13` |
| `CTAU-TOKEN` | `R4-rollout` | `R13` |
| `CTAU-TOKEN` | `R8` | `R13` |
| `R1` | `R6-candidates` | `R2` |
| `R16` | `R3` | `R3a` |
| `R16` | `SCAN` | `R17` |
| `R17` | `R3` | `R3a` |
| `R3` | `R4-rollout` | `R3a` |
| `R3` | `R5` | `R3a` |
| `R3` | `R6-candidates` | `R2` |
| `R3` | `R6-select` | `R3a` |
| `R3` | `R7` | `R3a` |
| `R3` | `R8` | `R3a` |
| `R3` | `SCAN` | `R3a` |
| `R3a` | `R6-candidates` | `R2` |
| `R5` | `R8` | `R4-rollout` |
| `R6-candidates` | `R7` | `R2` |
| `R6-select` | `SCAN` | `R17` |

Incomparable pairs with no common lower bound: (`CTAU-CLASS`, `R1`), (`CTAU-CLASS`, `R14`), (`CTAU-CLASS`, `R17`), (`CTAU-CLASS`, `R19`), (`CTAU-CLASS`, `R2`), (`CTAU-CLASS`, `R3`), (`CTAU-CLASS`, `R3a`), (`CTAU-CLASS`, `R4-model`), (`CTAU-CLASS`, `R6-candidates`), (`CTAU-CLASS`, `R7`), (`CTAU-CLASS`, `SCAN`), (`CTAU-TOKEN`, `R1`), (`CTAU-TOKEN`, `R14`), (`CTAU-TOKEN`, `R17`), (`CTAU-TOKEN`, `R19`), (`CTAU-TOKEN`, `R2`), (`CTAU-TOKEN`, `R3`), (`CTAU-TOKEN`, `R3a`), (`CTAU-TOKEN`, `R4-model`), (`CTAU-TOKEN`, `R6-candidates`), (`CTAU-TOKEN`, `R7`), (`CTAU-TOKEN`, `SCAN`), (`R1`, `R13`), (`R1`, `R14`), (`R1`, `R19`), (`R1`, `R4-model`), (`R13`, `R14`), (`R13`, `R17`), (`R13`, `R19`), (`R13`, `R2`), (`R13`, `R3`), (`R13`, `R3a`), (`R13`, `R4-model`), (`R13`, `R6-candidates`), (`R13`, `R7`), (`R13`, `SCAN`), (`R14`, `R17`), (`R14`, `R19`), (`R14`, `R2`), (`R14`, `R3`), (`R14`, `R3a`), (`R14`, `R4-model`), (`R14`, `R4-rollout`), (`R14`, `R5`), (`R14`, `R6-candidates`), (`R14`, `R7`), (`R14`, `R8`), (`R14`, `SCAN`), (`R17`, `R19`), (`R19`, `R2`), (`R19`, `R3`), (`R19`, `R3a`), (`R19`, `R4-model`), (`R19`, `R4-rollout`), (`R19`, `R6-candidates`), (`R19`, `R7`), (`R19`, `R8`), (`R19`, `SCAN`), (`R2`, `R4-model`), (`R3`, `R4-model`), (`R3a`, `R4-model`), (`R4-model`, `R6-candidates`).

## Extensions and boundaries

- Linear extensions: **951616092**; log2 = **29.825804427302**.
- Fine extensions respecting the five-stage PERCEIVE → BELIEVE → EVALUATE → SELECT → ACT column order: **0**.
- Input descent edges opposing that coarse column order: `R13` → `CTAU-CLASS`, `R13` → `CTAU-TOKEN`, `R13` → `R4-rollout`, `R4-model` → `R7`, `R4-rollout` → `R8`, `R6-candidates` → `R4-rollout`, `R6-candidates` → `R8`.
- Sources: `R13`, `R14`, `R19`, `R2`, `R4-model`.
- Sinks: `CTAU-CLASS`, `R16`, `R3`, `SCAN`.

## Absent-class edges

- `CTAU-TOKEN` → `R5`: `:C-tau`; `:token-carried-in-lean false`.
- `R2` → `R7`: `:o`, `:ref-label`; `:token-carried-in-lean false`.
- `R2` → `SCAN`: `:o`; `:token-carried-in-lean false`.
- `R3a` → `R7`: `:eps`; `:token-carried-in-lean false`.
- `R4-rollout` → `R5`: `:A`, `:Q-o-pi`; `:token-carried-in-lean false`.
- `R4-rollout` → `R8`: `:A`; `:token-carried-in-lean false`.
- `R4-model` → `R4-rollout`: `:A`, `:B`; `:token-carried-in-lean false`.

In the receipt semantics these edges contribute no produced or needed token IDs; consequently their support witnesses have empty `:tokens` and Lean `edgeValid` would reject them.


## Hasse description

```clojure
(sources R13 R14 R19 R2 R4-model) (CTAU-TOKEN -(C-tau)-> R5) (R1 -(mu)-> R3a) (R13 -(T)-> CTAU-CLASS) (R13 -(T)-> CTAU-TOKEN) (R13 -(T)-> R4-rollout) (R14 -(tau)-> R6-select) (R17 -(E)-> R6-select) (R17 -(a-conc)-> SCAN) (R19 -(C,U-t)-> R5) (R2 -(o)-> R1) (R2 -(interp)-> R6-candidates) (R3a -(eps)-> R3) (R3a -(eps)-> R7) (R4-rollout -(A,Q-o-pi)-> R5) (R4-model -(A)-> R7) (R4-rollout -(A)-> R8) (R5 -(G)-> R6-select) (R6-select -(Q-pi,pi)-> R16) (R6-candidates -(pi)-> R17) (R6-candidates -(pi,r)-> R4-rollout) (R7 -(wc)-> R17) (R7 -(rates)-> R4-rollout) (R8 -(F-pi)-> R6-select) (sinks CTAU-CLASS R16 R3 SCAN)
```

This S-expression states only the split DAG's directed token-carrying edges, sources, and sinks; it does not claim that the input graph itself is a one-click cascade.


## Ruled-defaults pass (R1–R4, 2026-10-06)

This third pass keeps the earlier unsplit refusal and two-unit split pass above on record. It applies the ruled defaults as data: the same 11-edge R1 cut; only R6 split into `R6-candidates` and `R6-select`; R4 retained as the rollout unit; and one `click-input` root. The former R4-model → R7 edge is re-routed as `click-input` → R7 carrying `A`; the former model → rollout edge becomes `click-input` → R4 carrying `A, B`.

The root's produced set, derived from the explicit synthetic input edges, is `:A`, `:B`, `:C`, `:T`, `:U-t`, `:o`, `:tau`; its needs are empty. R13, R14, R19, and R2 remain units and receive `T`, `tau`, `C/U-t`, and `o` respectively from the root. They are retained as pass-through/source-processing boxes because each still produces terms downstream; no former source was folded away.

The ruled graph has 19 units, 40 edges (34 token-carried in Lean, 6 `:absent` excluded), and 129196596 linear extensions. Its receipt has 104 common-origin `MeetWitness` values for 108 incomparable pairs. Pairs without a meet: [["R16" "R4"] ["R17" "R4"] ["R4" "R6-select"] ["R4" "SCAN"]]. Paths run from each meet to each operand, matching mathlib4 `7f497d3bd1`.

`click-input → R4` carries `A, B, T`: although the ruling introduces that edge for `A/B`, `ConstructionReceipt.edgeValid` requires the complete unit-level `produces ∩ needs`, and R4 also needs `T` through R13. The first generated proof run with only `A/B` failed `support.all edgeValid`; the exact-intersection edge is the data used below.

The generated ruled EDN SHA-256 is `f3b3111473fa1295615e8b4e576a598e4f636d7c2562a9141bb64f920c70f39d`; the generated Lean header carries the same pin. The deliberate bad receipt adds excluded edge `CTAU-TOKEN → R5` with `∅` tokens to both support and precedence.

### Lean build

```text
Build completed successfully (585 jobs).
```
