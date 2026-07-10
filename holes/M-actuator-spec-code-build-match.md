# Spec: automated code build-match (built-code ⟼ sorry-registry) — the living pipeline dashboard

**Owner/reviewer:** claude-4 (Codex builds, claude-4 reviews). **Idea (Joe):** the CLean
`futon6/holes/clean/aif-grounded-loop.clean.edn` describes the pipeline *itself* (7 boxes =
R-stages). We build-matched it by hand (2/7 ready) but the readiness `:grade` was **curated**.
Automate it — read the **code + `sorry_registry`** per box — so re-running reflects the live
system. This turns the CLean into a re-checkable dashboard.

## Design — the automated readiness probe

For a **code-pipeline** CLean (boxes = code stages, `:produces` = abstract/code types, NOT
substrate entities), readiness is NOT substrate inhabitation. It is:

> **ready? = code-present? AND sorry-clear?**

- **`code-present?`** — the box's namespace(s) load and its key var resolves (a `require` against
  the live code; or query the substrate-2 code-graph `:code/v05/*` if you prefer — your call).
- **`sorry-clear?`** — no OPEN sorry in `futon2.aif.sorry-registry` among the box's gating sorries.

## Build (new ns `futon2.aif.code-build-match`, sibling to the substrate build-match)

1. **`reviewed-code-bindings`** — for `aif-grounded-loop`, map each box →
   `{:code-ns [<ns> …] :code-var <sym>? :gating-sorries [<sorry-id> …]}`. **Authored:** explore
   `futon2.aif.sorry-registry` and find the sorries that gate each stage (e.g. the R3/R4
   `:prototyping-forward` channel-coverage sorries gate b1/b2; the R12 re-ranking gap gates b7).
2. **`code-box-match`** `[clean code-bindings]` — per box → `{:box :code-present? :sorry-clear?
   :open-sorries :ready?}`; dial `{:ready :total}`.
3. Runs against the **live** code + registry, so a later re-run reflects the new state (the point).

## The faithfulness self-check (do NOT skip)

The automation is only as faithful as the sorry-registry's coverage. So: for any box that is
`code-present? AND sorry-clear?` **but which you believe is not truly ready** (e.g. R4 forward
model is shape-only), if there is **no recorded sorry** gating it, report it as
`:faithfulness-gap` — "this reads ready but has no sorry recording its incompleteness." That is an
R18 finding, not a pass. (This is how the dashboard stays honest: a not-ready stage with no sorry
is a missing sorry, surfaced.)

## Acceptance (automated — NO hand-set `:grade` anywhere)

- `aif-grounded-loop` runs: **b3 (R5), b5 (R16, built this session) → ready**; **b2 (R4) → not-ready**
  (code present, a gating sorry open — OR flagged `:faithfulness-gap` if R4 has no sorry);
  **b7 (R12) → not-ready** (no code); b1/b4/b6 → not-ready (gating sorries open). Dial ≈ **2/7**,
  matching the manual result but **derived**.
- Re-running after a stage's sorry is discharged flips that box to ready with no code edit to the checker.

## Gates
clj-kondo clean; `futon4/dev/check-parens.el` clean; tests green. Reuse the CLean-box iteration
pattern. Do NOT restart :7071. Bell **claude-4** back with commit sha + the `aif-grounded-loop`
dial + any `:faithfulness-gap` boxes found.
