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
Ran 6 tests containing 39 assertions.
0 failures, 0 errors.
```

The new controls are `b5-canonical-draw-replaces-the-objective-tie-only-for-choice`,
`ln-e-does-not-change-a-retained-canonical-choice`, and
`refused-frontier-rows-never-enter-the-minimum`.  Kondo on changed Clojure:
0 errors/0 warnings (one pre-existing informational note); check-parens `OK`;
`git diff --check` exit 0.
