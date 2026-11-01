# L5-7-I-2-D — controlled composition regression and I-2 wiring

Read-only discovery on futon2 `236b33365` (branch `codex6/l5-7-i-2-d`). I read the packet and the L5-7-D serving report, traced the relevant source and history, and ran only hermetic namespace tests. No source, `data/`, or live JVM state was changed.

## Answer 1 — regression boundary and changed interface

The first bad commit is `0be247a913f3c56a5d7ef43b0d2d29800b869042` (`Supply reading-policy class free energy`). Its parent `31634de9b` passes `futon2.aif.factorized-preference-sut-test` (4 tests, 21 assertions, 0 failures/errors); the commit itself produces 2 failures and 1 error in `authorized-kernel-reaches-real-scorer`. This is an exact adjacent-commit check, not an inference from dates. The controlled seam was introduced earlier at `0856fc206` and was green before this change.

`0be247a91` changed the `:condition` operation for `:class-emission` in `observation-model/query`. Previously it returned unchanged posterior belief and neutral/not-supplied F. It now extracts `[:class-free-energy :policy-key]`, `:window`, and `:sources` from the prediction context, refuses with `:class-free-energy-history-not-supplied` unless those are present (or the policy-key carrier is explicitly noncanonical), computes the terminal-class prediction for the candidate target, and calls `reading-policy-class-free-energy/evaluate`. The same commit changed `cascade-observation-scoring/score-candidate` to pass the candidate target into that condition query and changed `cascade-shape-g/score-arranged` to construct the new history context for ordinary sealed reading-policy candidates.

The failure becomes family-wide in `cascade-observation-scoring/rank-cascade-actions`: after scoring all candidates it rejects any inference status other than `:computed` or `:history-unavailable-noncanonical-policy-key`, returning `:observation-family-not-selectable`. Later commit `a8a4caec6d` added typed unavailable-history cases, but did not restore the old interface.

Current verification reproduces the packet exactly: the two namespaces run 8 tests / 44 assertions with 17 failures and 1 error. The cascade SUT's many `:observation-kernel` failures are secondary: its fixture obtains that kernel from a preview call to the already-refused preference SUT, so the preview supplies nil before `cascade-efe-sut/compose!` reaches its intended later checks.

## Answer 2 — which side is stale

The controlled composition is stale relative to the class scorer. `factorized-preference-sut/compose-and-score!` still calls `rank-cascade-actions` with only `{:occurrence-id "controlled-q9" :tau horizon}` in `:prediction-context`. Its candidates are also raw controlled candidates, not candidates sealed by `policy-prefix-admission/seal-reading-policy-key`. It therefore supplies neither a canonical per-policy key nor a declared history window and sources.

The serving scorer deliberately acquired those requirements in `0be247a91`, and the ordinary `cascade-shape-g/score-arranged` route was updated in the same commit to satisfy them using the candidate's sealed key and `reading-policy-class-free-energy/history-context`. Thus this is not evidence that serving asks for data no serving producer can supply. It is evidence that the separate controlled caller was omitted from the interface migration.

There is an important semantic distinction: the controlled Q9 test is testing predictive G and consumed C, not conditioning on an observed terminal class. The scorer already exposes that distinction as `:prediction-only?`; when true, `score-candidate` records inference as `:not-requested` and `rank-cascade-actions` does not reject it. If the controlled composition is intended to test class F too, it must instead carry honest per-candidate sealed policy keys and a declared history context; inventing a common key or empty history merely to pass is not an acceptable repair.

## Answer 3 — smallest real-scorer repair

For the stated `authorized-kernel-reaches-real-scorer` contract, the smallest honest change is in `factorized-preference-sut/compose-and-score!`: add `:prediction-only? true` to the options passed to the existing real `cascade-observation-scoring/rank-cascade-actions`. No scorer stub and no scorer change is needed. `cascade-observation-scoring/score-candidate` already implements this mode, and `rank-cascade-actions` already omits inference failures in it. The real rollout, class-emission risk, G, ranking, and retained `[:certificate :consumed-g :C]` still run; only an unrequested posterior/F condition is skipped.

That targeted repair should be accompanied by a test assertion that inference is explicitly `:not-requested` for this controlled predictive composition, so the absence is not mistaken for a computed posterior. `cascade-efe-sut/compose!` then receives the real scored rows again and its existing prefix-F stage remains the separately declared posterior calculation. If its intended model is instead historical class F, the larger honest repair is: seal each controlled candidate with `policy-prefix-admission/seal-reading-policy-key`, obtain a context through `reading-policy-class-free-energy/history-context` (or an explicit hermetic declared context), and pass the per-policy key/carrier plus window/sources through `factorized-preference-sut/compose-and-score!`; that is not the smallest repair to the named test.

## Answer 4 — I-2 against today's scorer

After `preference-registry/load!` validates the production registry and returns its derived terminal distribution and authorization evidence, the distribution must be threaded through the runtime/cascade-decision/family-selection options to the point where `cascade-shape-g/score-arranged` constructs its class model. `cascade-shape-g/class-emission-model` must accept that terminal distribution as an explicit validated input: use unit mass on `:ending/not-yet-evaluated` for every `tau < horizon`, and use the authorized distribution at `tau = horizon`. It must not consult the registry itself, and it must not fall back to `terminal-class-preference` after the factorized route has been selected. The existing sealed policy key and `:class-free-energy` prediction context remain required independently; Q9's C does not replace class-history F.

Canonical mode must carry the same validated terminal distribution and its receipt into `wm.family-canonical-g/rescore`, which reconstructs every per-policy model at the common field horizon. `cascade-coapply-g/compare-at-horizon` should continue checking that all candidate models share the full schedule and match the scoring horizon, but its current `:require-ruled-class-kernel?` reconstruction of hardcoded 55/35/5/5 must become an equality check against the supplied authorized schedule/kernel. `wm.family-canonical-g/certify-score` must likewise recompute/verify against the retained authorized kernel in the canonical inputs/certificate, rather than call `class-emission-model ::ruled ...` and compare to 55/35/5/5.

The checks to preserve are therefore: (1) every preterminal C row is exactly `{ :ending/not-yet-evaluated 1 }`; (2) every terminal C row is exactly the registry-derived authorized distribution; (3) all compared models share that schedule and the common horizon; (4) each scorer result's retained `[:certificate :consumed-g :C :steps]` equals it; and (5) canonical rescore and later certification retain the same authorization-to-consumed-C join. `cascade-coapply-g`'s reduction label should claim the ruled `compareAtHorizonN` specialization only when the supplied authorized kernel is the theorem's kernel; otherwise it is the general `scoreCascadeCoN` map, not the old hardcoded specialization.

## Evidence summary

- Boundary test at `31634de9b`: 4 tests / 21 assertions / 0 failures / 0 errors.
- Boundary test at `0be247a91`: 4 tests / 21 assertions / 2 failures / 1 error.
- Current two-namespace test at `236b33365`: 8 tests / 44 assertions / 17 failures / 1 error.
- No reload, no `data/` write, and no source edit was performed.
