# Testimony study — planned vs built vs specified

Opened 2026-09-18 by claude-12, under Joe's design the same day: in
parallel with each census dispatch D1–D7 (zai testifies to what IS BUILT),
claude-4 testifies to what it PLANS TO BUILD for the same term cluster,
and both testimonies are compared against the Lean specification.

## The three witnesses

| witness | testifies to | artifact |
|---|---|---|
| zai (per dispatch) | what is built | `D<N>-<slug>.edn` (PLAN.md schema) |
| claude-4 (per dispatch) | what it plans to build / claims built | `T<N>-claude4.edn` (schema below) |
| Lean | the specification | census modules + carriers (frozen core) |

## Testimony schema (claude-4, per cluster)

```edn
{:testimony :T1
 :terms
 [{:term :G
   :planned  [{:var "intended name" :wm-item "..." :status :planned | :in-progress
               :inputs ["..."] :depends-on ["..."]}]
   :claims-built [{:var "..." :wm-item "..." :note "..."}]
   :no-plans? false}]
 :grounding ["DAG/commission artifacts read, by path"]}
```

## Comparison, per term (filled by claude-12 after both land)

- **P↔S**: does the plan's shape match the Lean signature? (a plan that
  cannot fill the spec's inputs is divergent before it is built)
- **B↔S**: does the built producer match the Lean signature? (from the
  census ledger's :match)
- **P↔B**: does the plan acknowledge what is already built? (duplicate
  risk: planning what exists; facade risk: claiming built what is not)

Discrepancy classes: :plan-diverges-from-spec, :built-diverges-from-spec,
:plan-ignores-built, :claims-built-not-found, :consistent.
Results table appended below as each pair (Dn, Tn) completes.

## Results

### G cluster — D1 (2c4e022a + review fix f3d0417b) × T1 (2afbc6ed), compared 2026-09-18

| term | P↔S | B↔S | P↔B |
|---|---|---|---|
| :G | no new plans — testimony is claims-built only (the DAG records obligations, not build plans). Pre-registered finding stands: `LF-expected-free-energy` is *accepted* while its own acceptance text leaves the ambiguity argument unbound → **:plan-diverges-from-spec** (at the obligation-record level) | **:matches on the declared reduction domain** (horizon↔T, precedence-fn↔π, :spec↔constant C, pointwise risk with ⊤-iff-zero-C; refuses outside the domain rather than mis-scoring) | all 6 claims-built confirmed by the census, incl. the private `outcome-risk-pointwise` both witnesses flagged independently → **:consistent** |
| :risk | :no-plans (informative) | :not-checked — dedicated OutcomeRiskKL pass queued | :consistent |
| :ambiguity | :no-plans | :matches (within the identity-A known-failing context) | **discrepancy found and resolved**: census said `step-ambiguity` :live; testimony said built-but-not-reached; rerun showed the sole call site is inside `horizon-g` (enumerating reference) → testimony right, census corrected in place (f3d0417b). Class: **:census-role-error** (new). Note: the census hedged in prose while stretching the role field — prose hedges don't survive joins |

**Cross-cutting findings (D1):** `active-horizon-g` is Lean-aligned and
DEAD (test-only island; nothing from the tick reaches it) → new class
**:aligned-but-dead**, the facade-adjacent case: elaboration without
reachability. `efe/compute-efe` dead on the tick (verified: zero call
forms outside its file). Three Clojure kernels of one Lean `outcomeRisk`;
three-plus G computations across grains. The DAG's `sparse-g` field has
file:line rot (497/536 vs actual 505/555) — independent evidence for
name-based addressing.
