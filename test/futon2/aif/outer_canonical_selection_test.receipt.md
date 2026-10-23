# O3-2 canonical selection receipt

Worktree: `/home/joe/worktrees/futon2/o3-2-canonical-select`  
Branch: `o3-2-canonical-select`  
Base: `1cadf58bd`.

Negative control: before selection read the bound action's digest rather than
the fixture projection's absent top-level digest, the new selector returned
`:no-scored-canonical-policy`; the b5 assertions for wiring/bindings therefore
failed.  After the correction, it reads `[:action :cascade-digest]`, exactly
the pin O3-1 retains.

Command:

```
clojure -X:test :nses '[futon2.aif.outer-canonical-selection-test futon2.aif.outer-canonical-g-frontier-test]'
```

Result:

```
Ran 7 tests containing 42 assertions.
0 failures, 0 errors.
```

The new controls are `b5-canonical-draw-replaces-the-objective-tie-only-for-choice`,
`ln-e-does-not-change-a-retained-canonical-choice`, and
`refused-frontier-rows-never-enter-the-minimum`.  Kondo on changed Clojure:
0 errors/0 warnings (one pre-existing informational note); check-parens `OK`;
`git diff --check` exit 0.

## Review (claude-7, 2026-10-23)

Review fixes on top of `6361e3d62`:

- `select-live` put the old objective receipt under `[:policy :meta-selection]`
  when the canonical carrier was activated, so the receipt's `:meta-selection`
  (read by `browser-receipt` and the decision manifest) named the objective's
  winner while `:chosen`/`:action` named the canonical one.  It now records the
  canonical selection there; the objective stays under `:policy-objective`.
- The lnE test called `evaluate` twice on identical inputs.  It now builds two
  evidence declarations differing only in lnE, shows the old objective's winner
  flips, and that the canonical choice is unchanged.
- Added `select-live-activates-the-canonical-carrier` (packet item (e)): a
  select-live run with a frozen focus reaches the canonical carrier, verify
  passes, the receipt action is the canonical choice and differs from the
  objective's winner.

Red control: with the `:meta-selection` fix disabled, the select-live test fails
at the `:meta-selection :action` assertion (`T-occ/policy-2` vs `T-occ/policy-1`).

Command:

```
clojure -X:test :nses '[futon2.aif.outer-canonical-selection-test futon2.aif.outer-canonical-g-frontier-test]'
```

Result:

```
Ran 8 tests containing 54 assertions.
0 failures, 0 errors.
```

Pre-existing, not caused by O3-2: the select-live namespaces
(`outer-policy-runtime-selection-test`, `meta-live-outer-assessed-selector-test`,
`outer-policy-field-boundary-test`, `meta-live-outer-selector-test`) fail 49
assertions identically at `1cadf58bd` and at this branch.  Most refuse with
`:selection-as-of-required` before any selection, because their inputs predate
`aaa85f9b4` (2026-10-22), which made `:selection-as-of` required on the ordinary
path.
