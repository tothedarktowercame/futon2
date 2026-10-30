# External review: WM recovery handoff (REPORT-wm-recovery-external-review.md @ 97ac9e551)

Reviewer: a Claude session outside the serving machine (cloud, session
`01DcURqCM4rXHzJMaHCQ8AhG`), reading only what is pushed: futon2 `main`
`97ac9e551`, branch `repair/ledger-authority-composition` `039342915`,
futon3c `master` `130b272db`, mathlib4 `darktower` `b5c7da28f`. No live
ledger, receipt bytes or notes were available, so every statement about
the 11 bundle entries is about the code path they must pass, not about
their bytes. Written under the 2026-10-18 ground rules as a review, not a
build.

## Answers to the five questions

**1. Is the restored authority the exact prior approved context, or does a
fallback broaden trust?** Nothing pushed restores it yet, and nothing
pushed broadens trust. The partial fix changes one expression in
`resolve-medic-authority` (`full_loop_runtime.clj`): the no-options default
verifier becomes `(with-retained-authorities (fn [_] false))`. That union
(`meta_medic_authority.clj`) is deny-all for external authority plus three
entry-local verifiers, each of which re-derives its own receipt from the
entry's own pins. It admits exactly what the resolved-context branches
already admitted for entry-local grammars, so ordinals 118 and 125 pass
and nothing else changes. The 11 bundle-grammar entries still need
`resolve-context` with a `trust` predicate; the default supplies none, so
they still refuse. That is the correct fail-closed state, and the test's
live witness pins it as "13 → 11, deny path still 16".

**2. Can each historical dependency be revalidated by its original pin
without restoring old source or editing history?** Yes, and the code
already makes this possible. `resolve-bytes` reads artifact bytes from the
supplied `artifacts` map keyed by the pin's `:path` string and "opens no
path implicitly"; the path is an identifier, not a filesystem location.
So a recovery bundle is intent + an in-memory map of path → bytes + three
receipt pins + `trust`, and the bytes can come from Git (`7ca70e10e`,
`ae9e71584`, `86024a594` for the three changed-on-disk files) or any
content-addressed store, with no change to current source. The one real
dependency on the current machine is `:expected-ledger-pin`, which must
equal the ledger root at the time the medic operation was appended; that
is already in each entry's `:medic-operation`, so no re-derivation is
needed. Do not re-pin to current disk bytes: `verify-historical` and
`verify-genuine` compare `:original-record-pin` and `:cause-record-pin`
against `current`, and a changed artifact would fail there, which is the
right outcome for a moved file.

**3. Do the serving default and every loader use one resolver after a cold
start, and do tests exercise arbitration?** Not yet one resolver.
`resolve-medic-authority` composes the verifier once per `run-opportunity!`
and threads it as `:meta-medic-authority-verifier`; `diagnosis-context`,
`load-durable`, `effective-active` and `production-readiness` take that
option. But two consumers can still be called with no verifier and fall
to deny-all: `store/load-durable-ledger` with one argument, and
`meta_self_heal_harness.clj` lines 133, 151, 155, which call the one-arity
form. A deny-all load of a ledger with any medic-repaired entry refuses
`:store/tampered-ledger`, so any diagnostic that reaches those paths
reports a refusal that the serving default would not produce. The new
test `default-resolution-exposes-the-retained-union` exercises
`resolve-medic-authority {}` directly, and the live witness exercises the
four loaders, but neither drives `run-opportunity!` from an empty options
map; the entry-point test the handoff asks for is still missing.

**4. Are refusals preserved as typed refusals, and is credit charged only
after preconditions pass?** Two findings. (a) Pre-charge refusal already
exists for this blocker: `meta-self-heal-preflight!` (futon3c
`runner_service.clj`) calls `production-readiness` before the ration
callback, and `production-readiness` refuses
`:meta-self-heal/ledger-unreadable` when the durable ledger is not
`:loaded`. So a tampered-ledger refusal holds the click without spending
it; the report's "further clicks are held" is the machine's own behaviour,
and the requested "pre-charge-refusal test" should assert exactly this
path from the HTTP entry. (b) The absence mapping is real and should be
fixed before recovery is declared: `carried-bindings-observation`
(`meta_self_heal_production.clj`) returns `{:status :absent :reason
:no-active-injury}` whenever `(:status active)` is not `:computed`, which
is also what a refused ledger load yields. Arbitration itself fails
closed, as the report says, because `production-readiness` runs first;
but any diagnostic that reads the bindings observation alone will call a
refusal an absence. The fix is one branch: when `(:status active)` is
`:refused`, return `{:status :refused :reason (:reason active) :defects
(:defects active)}`.

**5. What minimal future-adoption retention contract prevents
recurrence?** Three rules, all expressible with existing shapes. First,
an adoption that appends a `:medic-repaired` observation under a bundle
grammar must also persist the bundle's artifact bytes content-addressed
(sha256 → bytes) and the three trust tuples `{:kind :identity
:intent-digest :receipt-pin}` it was admitted under, sealed under the
ledger root it was appended to. Second, the serving default must load
that sealed record and compose `resolve-context` over it, inside
`with-retained-authorities`, so there is one resolver. Third, the record
is append-only: a new scope needs a new sealed append with its own
authorization receipt, so restoring the 33 tuples authorizes nothing
further. `resolve-context` already refuses ambiguous overlap and
`verify-*` already refuse hash drift, so no new refusal vocabulary is
needed.

## Assessment of the partial fix (`039342915`)

Correct and safe to merge as partial. It replaces a raw single-grammar
verifier with the union every resolved context already used, grants
nothing to the deny path, and its hermetic ordinal-118 fixture tests
admission under the union, refusal under the raw verifier, and five
fail-closed controls (wrong injury, payload swap, unsupported grammar,
tampered bytes with stale sha, missing file). The live witness is
read-only and hash-guarded. Two things to add before calling the default
"one resolver": the `run-opportunity!` entry-point test from question 3,
and the `carried-bindings-observation` refusal preservation from question
4.

## Bounded recovery recommendation

1. Merge `039342915` (13 → 11).
2. Build the sealed trust record for exactly the 11 bundles: artifact
   bytes by sha (recovering the three changed files from the named Git
   commits), 33 tuples, sealed under the ledger root of each append. Load
   it in the default branch as `resolve-context` bundles with `trust` =
   membership in the sealed set, composed through
   `with-retained-authorities`. This is the report's approved scope; it
   adds no grammar and no new trust.
3. Prove in a fresh JVM that the unchanged 129-entry ledger loads through
   all four consumers and that `effective-active-injuries` is `:computed`,
   then repeat after a restart. Add the negative controls the handoff
   lists; `resolve-context` and `verify-*` already supply the refusal
   reasons to assert.
4. Fix the two gaps above (entry-point test; refusal not absence in
   `carried-bindings-observation`) in the same review, since both are
   small and both are what let the blocker hide.
5. Then one click, inspect, continue, checkpoint after three, as the
   handoff says. The citation gate (`582451e3b`) and the Q6 click-local
   reselection (`f9d28dc19`) are on main, so the click-51 shape repeats;
   the open live witnesses are E3's unchanged-input prevention and the
   dispatch-classification closure.

## Relation to the earlier proposal

`PROPOSAL-no-click-efficiency-levers-2026-10-30.md` lever 1 (no re-pick
after a refusal on unchanged input) is overtaken: `f9d28dc19` reuses the
click-local reselection when Q6 empties the family, and click 51 took
that path to M-peradam-grounding. Lever 2 (executed inspection on claims,
record-only) and lever 3 (self-heal admission preview) stand; lever 3 is
now partly answered by the finding in question 4(a), which shows the
ledger-unreadable case already refuses before the ration. The remaining
preview gap is `route!`'s acceptance-test admission, as the proposal says.
