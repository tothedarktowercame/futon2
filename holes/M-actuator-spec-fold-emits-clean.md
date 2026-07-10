# Spec: extend the fold to emit the CLean wiring (embedded `:clean` + loader gate)

**Owner/reviewer:** claude-4 (Codex builds, claude-4 reviews). **Idea (Joe):** the
CLean wiring is the *curriculum* the build must match; making it well-formed is a
**deposit gate** — a fold can't deposit a blueprint whose CANALIZE isn't valid.

**PROVEN:** the M-learning-loop CLean (`futon6/holes/clean/M-learning-loop.clean.edn`,
hand-authored by claude-4 from `ft-learning-loop-010`) passes `clean_argcheck` (all 8
gates). That is the reference/fixture. The `:clean` is **embedded in the deposit**, not
a separate artefact (Joe's call).

## Build

### 1. Loader gate — `futon2/src/futon2/aif/fold_escrow.clj`
When loading a deposit:
- **`:clean` present** → validate it with `clean_argcheck`'s exact logic (8 gates: parse,
  boxes, copar, wires, **G5 ports type-check**, holes, **G7 acyclic**, shape). On any gate
  failure → REJECT like the other pins, with the gate reason in `:rejected`.
- **`:clean` absent** → do NOT hard-reject (the existing 17 deposits have no `:clean` —
  grandfather them); tag `:clean-missing`. Add a `strict-clean?` option (default **false**)
  that rejects on absent, to flip on once deposits are backfilled.
- **REUSE `clean_argcheck`, do NOT reimplement the 8 gates in futon2** (divergence risk).
  Mechanism your choice: shell out (`bb futon6/scripts/clean_argcheck.bb <temp .clean.edn
  written from the :clean map>`, parse PASS/FAIL), or require its `check-clean` fn. Same
  semantics as the bb script is the requirement.

### 2. Fold recipe — `futon3c/scripts/author_deposit_for.sh` (and note the overnight twin)
Add a STEP requiring the fold agent to emit the `:clean` block (the CANALIZE of the wiring):
- box-type every box: `:id :method :text :produces` (+ `:consumes` for non-sources)
- `:clean/wires` `{:from :to :carries}` where `:carries` = the `:from` box's `:produces`
  AND is in the `:to` box's `:consumes` (G5)
- typed holes on boxes: `:hole {:kind :discharge <∈ #{:sorryProof :queryAnswer
  :ungroundedBinder}> :satiety <∈ #{:parse :payoff :canon :bundling :role}> :wanted "…"}`
- `:clean/seq` = box `:method`s in listed order (G3); `:clean/copar`; `:clean/shape`
  `{:macro :holes-at :discharges-at :note}`
- **the fold is NOT done until the `:clean` passes `clean_argcheck`** (the fold self-checks
  its own CANALIZE before depositing).
- Exemplars to READ: `futon6/holes/clean/autoclock-in.clean.edn` (golden) and
  `M-learning-loop.clean.edn`.

## Acceptance (tests)

- Deposit whose `:clean` = the M-learning-loop CLean → loads, `:clean` validated PASS.
- Deposit with a malformed `:clean` (e.g. a wire `:carries` the `:to` box doesn't
  `:consume` → G5, or a cycle → G7) → REJECTED with the gate reason.
- Deposit with NO `:clean` → loads, tagged `:clean-missing` (grandfathered); with
  `strict-clean? true` → rejected.
- The existing 17 deposits still load (no `:clean` → grandfathered).

## Gates
clj-kondo clean; `futon4/dev/check-parens.el` clean; tests green. Reuse `clean_argcheck`
(no gate reimplementation). Do NOT restart :7071. Bell **claude-4** back with commit SHAs.
