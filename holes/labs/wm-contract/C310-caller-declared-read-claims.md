# C310 — caller-declared mutable-read claims

Date: 2026-09-01

## Neutral observation

`checks.mutable-read-set/observe-files` now reports only observations:

- exact captured bytes and digests;
- `:endpoint-equal?` after comparison;
- an interval with `:started-at` and `:finished-at`;
- typed per-path comparison results.

It does not infer what equality proves.

## Declared claims

Callers use `assess-claim` or `require-claim!` with one of two claims:

- `:content-current`: equal endpoint bytes satisfy the claim. ABA is not a
  content defect because the bytes used by the verdict remain the current bytes.
- `:event-free`: equal bytes alone yield `:event-free? :unverified` and
  `:distinguishable-cause? false`. The claim can be satisfied only with an
  equal before/after monotonic witness or a declared held fence. Movement never
  satisfies it.

Thus `:event-free?` cannot become true merely because two file digests match.

## Consumer declarations

The seven existing consumers explicitly declare `:content-current`: R17,
route conformance, R8 pinned snapshot, R9 proof receipt, holder, control
organization, and figure agreement. The five C304 consumers are all genuinely
content-shaped; none was reclassified as event-shaped. Behaviour and ordinary
verdicts are unchanged.

The C304 eight and C293 90-candidate tail are not rescued by this vocabulary.
Dynamic repository populations, live Agency state, and mixed Git/live claims
still need fences, revision tokens, or basis sandwiches. Calling their endpoint
equality `:content-current` would answer a weaker question than their checks ask.

## ABA control and focused results

The control writes A, captures it, writes B, restores A, and compares:

```text
endpoint-equal?                     true
content-current verdict             :satisfied
event-free verdict                  :unverified
event-free?                         :unverified
distinguishable-cause?              false
```

Focused verification:

```text
mutable-read-set-test: 5 tests, 20 assertions, green
all seven converted positive consumers: green
clj-kondo over substrate, tests, and consumers: 0 errors, 0 warnings
```
