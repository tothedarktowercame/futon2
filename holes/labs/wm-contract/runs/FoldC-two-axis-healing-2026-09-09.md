# FoldC two-axis healing — 2026-09-09

The runtime declaration keeps `:ruled-outcome-c` folded and names its
`:composition-axis :risk-contribution`. Active in-scope rows with missing or
unknown axes refuse. No contribution is removed to make the old comparison
pass. The preference-layer axis remains empty; the risk axis is compared across
runtime, PreferenceRiskBoundary.runtimeRiskContributionIds (mathlib4 ef8d39379c),
and generated fold-c-axes.edn.

The wrapper also elaborates the risk-boundary module and reruns the finite
execution certificate in a temporary directory, comparing both output files
byte-for-byte with the committed certificate. Stale reviewed bytes cannot be
accepted merely because regeneration succeeds. This adds finite mass/scorer
correspondence to the declaration comparison; it does not claim a live run or
complete source dependency closure.

## Reproduction

From futon2, derive the second-axis fixture:

```sh
bb -cp .:src -e '(require (quote [checks.fold-c-axes :as a]) (quote [clojure.pprint :as pp])) (spit a/fixture-path (with-out-str (pp/pprint (a/derived-fixture))))'
```

The old fixture gains only an EDN comment pointing at the era report. Its receipt
is re-attested through `checks.positive-proof-receipt/basis-record`, retaining
all old claims/history. Its EDN comment also points at the era report. The
original Lean algebra and its fixture values 3/8/7 are unchanged. Re-attestation
updates the fixture byte hash; pretty printing accounts for the larger textual
diff, not expanded mathematical claims.

The successor is regenerated using:

```sh
bb -cp .:src -m checks.preference-risk-receipt holes/labs/wm-contract/runs/separated-risk-certificate
```

It incorporates the new boundary source pin and changed runtime declaration and
generator hashes; all masses and measured risk values remain unchanged.

## Gates

The positive mode and --negative-order, --negative-folded, and
--negative-risk-missing each exit 0. In the old two modes, the planted Lean
statement itself fails with its expected diagnostic needles while the baseline
passes. The new mode removes the Lean risk ID set *at the comparator input*,
leaving runtime and fixture unchanged; that mismatch is rejected. It is a
comparator mutation control, not a mutation of the Lean source file.

Baseline failure now says `baseline FAIL` and exits 1, even in a negative mode;
only a mutation escaping an otherwise healthy baseline exits 2. This makes the
printed 0-pass/1-fail/2-mutation-slipped convention truthful.

Focused suites (axes, ruled-outcome-c, existing certificate): 7 tests / 30
assertions / 0 failures or errors. Tests cover unknown/missing active axes,
retention of a genuine preference-layer declaration, omitted runtime/Lean/fixture
agreement, and the existing runtime-score/bridge controls. Clojure and EDN lint
0/0; check-parens and diff checks pass. No registry/worklist/frontier changes.

The axis fixture's second derivation is byte-identical; repeated wrapper runs
also regenerate both successor artifacts and require exact byte identity.
The explicit null control checks equality of two complete baseline fact maps.
The old receipt's second basis-record derivation is separately compared before
commit. Independent review precedes the parked find-retirement/re-pin chain.
