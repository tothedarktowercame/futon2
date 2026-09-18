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

(none yet — D1 in flight, T1 requested 2026-09-18)
