# Ruled R-cascade drawing

Generated deterministically by `bb futon7/scripts/r-cascade-draw.bb`. Input pins: ruled EDN `f3b3111473fa1295615e8b4e576a598e4f636d7c2562a9141bb64f920c70f39d`; control stages `9d87c52e1e87f17fd3283a8beb2e73e8ae37852bc78c4ebcaa5f641af20b8f47`.

- Units: **19**.
- Token-carried support edges: **34**; transitive reduction: **26**.
- Longest-path layers: **6**; units alone in a layer: layer 0 `click-input`, layer 5 `R16`.
- Common-origin meets: **104**; incomparable pairs without a meet: **4** (`R16/R4`, `R17/R4`, `R4/R6-select`, `R4/SCAN`).
- Fine extensions respecting the five-stage column order: **0**, as recorded in `REPORT-r-cascade-2026-10-05.md`; stages are only colours in the stage rendering.

## Edges removed by transitive reduction

- `R1` → `R3` (`mu`)
- `R2` → `R17` (`o`)
- `R2` → `R3` (`o`)
- `R2` → `R3a` (`o`)
- `R2` → `R4` (`interp`)
- `R2` → `R8` (`o`)
- `R6-candidates` → `R6-select` (`pi`)
- `click-input` → `R4` (`A, B, T`)
