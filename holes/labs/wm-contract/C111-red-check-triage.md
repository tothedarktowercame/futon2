# C111 — disposition of C107's five red checks

Date: 2026-08-31

No red check was wired into `clojure -T:build ci`; its semantics are unchanged.

## 1. Organization pin — fixed, then eligible for workspace gate

The stale SHA referred to the whole
`p4ng/empirics-futon/control-map-edges.edn` input.  C69 added measured-route
layers, changing its content SHA from `827af5d2…` to `64485bb0…`; it did not
change the 22 drawn edges classified by `control-organization.edn`.  The pin was
updated in p4ng commit `efdc08b` with that reason in the read record.  The check
now passes with 22 drawn, 11 classified, and 11 honestly unclassified.

Disposition: **wire into a cross-repository workspace gate**, not repository CI,
because its declared inputs live in p4ng.

## 2. Lane registry — deliberately manual

The check correctly rejected a completed job still recorded as holding.  That
is operator/dispatcher state, not source validity; making compilation depend on
whether a commissioner has closed a live seat would turn useful operational
pressure into build noise.

Disposition: **manual operational boundary check**, required before dispatch
closure and operator ticks, never part of `clojure -T:build ci`.

## 3. Preference binding — invocation fixed, then eligible

The check was sound; C107 invoked JVM production code with Babashka.  Its
canonical positive and negative commands are:

```sh
clojure -M -m checks.preference-stack-binding-check
clojure -M -m checks.preference-stack-binding-check --negative absent
```

Both exit 0; the negative command rejects the absent witness.  The JVM command
also reproduces five layers and a present trace value equal to the computed and
serialized stacks.

Disposition: **wire into the future workspace gate using Clojure**, not BB.

## 4. R9 proof receipt — green now, dependency boundary needs repair

C107 observed source-content, source-git, and import-git drift while concurrent
edits to `Holes.lean` were present.  Once those edits settled, the original
receipt passed again and reproduced `axioms: [propext]`.  The red result was
real at observation time, not a defective predicate or a receipt to refresh
silently.

Nevertheless, version 1 pins the SHA and last-touch commit of the entire
5,000-line `Holes.lean` file twice (as proof source and imported module).  That
is too broad: unrelated declarations invalidate the receipt.  The next receipt
version should pin the exact `r9VerdictConsultsChecker` declaration slice and
retain live Lean elaboration plus its axiom result.  Changes to that declaration
then invalidate the source receipt; changes elsewhere are judged by whether
the theorem still elaborates with the recorded type/axioms rather than by an
unrelated byte change.

Disposition: **fix the receipt dependency boundary, then wire**.  Present green
does not erase that design debt.

## 5. Run-once default — fixed, then eligible

The old default invented today's dated path and failed with a generic slurp
exception when no operator run existed that day.  The default now selects the
lexicographically newest committed `tick-run-record-YYYY-MM-DD.edn`.  If none
exists, it fails loudly and names the expected path pattern.  Explicit paths
remain authoritative.

Positive now selects `tick-run-record-2026-08-30.edn` and exits 0.  Its negative
mutation removes `:traceWritten`, is rejected, and exits 0.

Disposition: **wire the committed-fixture mode into a workspace gate**.  A live
operator certificate must still pass its explicit new run path rather than
silently relying on the newest historical fixture.
