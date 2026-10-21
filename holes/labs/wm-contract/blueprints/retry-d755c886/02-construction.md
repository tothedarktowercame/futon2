# Blueprint 2: construction (phases 15, 19, 20)

Part of [00-TARGET](00-TARGET.md). Status: fixed in futon2 `8dd7c2cff`
(codex-1, branch off main `eb6ebc04a`), reviewed, **not merged or loaded**.
Child 7 stopped here (`9c998f2e`, `:fold-output-invalid`).

This is the blueprint for the validation in that commit, step by step.

## What happened in child 7

`construction-wiring-result` chose a fold route by looking only at the
construction. A `:selected-cascade` construction with non-empty
`:interpretation-receipts` went to `fold-cascade/realize`; anything else went
to the classical fold. A reading policy has no receipts (blueprint 1, leaf
1.2), so the classical fold ran. It read the action's co-apply map as one
unfolded pattern, emitted a policy hole with only `:unfolded-pattern`, and
shape validation refused it for missing `:free`, `:why` and `:obligation/id`.

The `:cascade-ready` result from phase 14 was never passed to construction.

## How construction works after `8dd7c2cff`

### Step 1: the runner passes the execution result in (`full_loop_runner.clj:7275-7278`)

If phase 14 produced a cascade execution, the construction gets
`:cascade-execution` (the persisted execution record) and `:shown` is set to
`[<the selected action's id>]`.

### Step 2: the route is chosen (`construction-wiring-result`, `:2915-2922`)

In order:

1. construction has `:cascade-execution` → `ready-cascade-fold` (new);
2. `:selected-cascade` with non-empty receipts → `fold-cascade/realize` (unchanged);
3. otherwise → classical fold (unchanged).

### Step 3: `ready-cascade-fold` checks the execution against the selection (`:2835-2896`)

It refuses with `:cascade-not-ready` unless the status is `:cascade-ready`.
Otherwise it loads two records from disk: the selected cascade (`:saved`) and
the compiled one (`<compiled-digest>.edn` beside it). It builds only if all of
these hold:

- the action's `:cascade-digest` = the execution's = the saved record's digest;
- the execution's `:compiled-digest` = the compiled record's digest;
- the two records have the same `:structural-digest`;
- `validate-cascade` certifies both records;
- if the compiled cascade has a frontier: routing is `:routed`, for this
  compiled digest, over the same occurrence ids, every step with a route;
- if it has no frontier: nothing unread in the census and no `:open-hole`
  node.

If any of these fails it refuses with
`:cascade-execution-not-certified-for-selection`. If a record cannot be read
it refuses with `cascade-execution-evidence-unreadable: …`. A refusal is
`{:fold/refused true :refusal/class :cascade-execution …}`.

When it builds, it emits one box for the selected action, with warrant kind
`:deduction` ("compilation is a deduction, not an observed effect"). Each
`:open-hole` node becomes a policy hole that carries `:free`, `:why` and
`:obligation/id`, which are the three fields child 7 was missing.

### Step 4: the two validators run as before

- shape: `fold/validate-fold-output-v1` on the emitted wiring;
- correspondence: `fold/validate-fold-correspondence`, the boxes against
  `:shown`. Step 1 set `:shown` to the one action id, so it matches the one
  box.

## Which test checks which step

From `test/futon2/aif/cascade_ready_construction_test.clj` (warrant
`test-registry-b721de67…`, 2 tests, 21 assertions, 0 failures, at `8dd7c2cff`):

| step | checked by |
|---|---|
| 1 | the positive test goes through the runner with `:construction-wiring-fn nil`, so the production code passes the execution in |
| 2 | a counter on `classical/classical-fold` stays at 0; `:fold/route :dedicated-cascade` |
| 3, refusals | a bare `{:status :cascade-ready}` with no saved records; `:held`; `:refused`; real records with a different `:cascade-digest`: each refused |
| 3, frontier branch | not checked by me; I did not confirm a case with a non-empty frontier |
| 4 | shape and correspondence both `:ok`; the run stops at `:author-dispatch`, not `:construction` |
| revert | with the change reverted, the test fails as child 7 did: one classical fold call, stop at `:construction`, `:fold-output-invalid` |

The second test (an uncompiled cascade) is held at phase 14 and never reaches
construction, so it does not test this commit.

## Open before merge

`:shown` is also what the trace records as the patterns of the run
(`:7339` `:patterns`, `:7878` `:patterns-selected`). After this change a
reading-policy run records the cascade id there instead of its member
patterns. No test pins either. The smaller change would leave `:shown` alone
and give only the correspondence check the one-id list on this route.

## Check before the click

After merge and reload: run child 7's recorded decision through the serving
JVM's `construct-for-decision` and `construction-wiring-result`, as in
DISCOVERY-9c998f2e.md section A, with its recorded cascade execution
attached. Expected: `:fold/route :dedicated-cascade`, both validations `:ok`.
This needs the records at the execution's `:saved` path to still exist.
