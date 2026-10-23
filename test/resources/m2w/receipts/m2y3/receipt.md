# M2y-3 — pattern-domain frozen prediction

Branch: `m2y3-pattern-prediction`. Worktree: `/home/joe/worktrees/futon2/m2y3`.
Test HEAD/base: `d7f5be46d962fb61f0c56c57b81e188c30c778b7`, with the source bytes
pinned below. Own JVM, OpenJDK 21.0.12.1 (Ubuntu), cwd the worktree.
No click, reload, live-store write, measurement or accounting change.

## Implementation decisions

All three selected-outer seal producers attach the versioned input before
computing the carrier pin. Verification recomputes it from the selected entry;
decision projection transfers it into the certificate. Old sealed carriers
remain verifiable, but projection reports typed absence rather than synthesising
an unsealed prediction input. The precision-family freeze path is unchanged
when no pattern input key is supplied.

The model digest uses recursively sorted maps AND sets (canonical EDN), including
q0, horizon, want, transition kernel and the entire observation model. Invalid
source inputs yield typed absence; a tampered input cannot reuse its digest.

The pattern prediction's `:action` retains kind/id/target/precedence only and
does not copy mission wants, declared-wants, or mission observation locators.
This is required by the packet's “no :hole/ token anywhere in the result” case;
the selected decision's action is not modified. Native tokens are not wrapped.

Landing-target clarification: the canonical checkout is on `main`, and no
`master` branch exists. Land on the existing `main`, not a new parallel branch.

## Live fixture provenance

Source `/home/joe/notes/M2v/decision-3-0.edn` SHA256:
`539a79578d704f7ed8a5a273a8cf15febd77af1512295b4fef38ea0b7ddfaeee`.
Projection `test/resources/m2w/pattern-prediction-90ca.edn` SHA256:
`894ef46746d877abbcc7f3156f494c5006e30e0c9e58274ca362ff3857872d0f`.

Read with clojure.edn via bb. From
`[:decision :selection-certificate :selected-outer-cascade-policy :selected-entry]`,
select `:horizon-steps :observation-model :transition-kernel :policy-id
:cascade-digest :target-id`; retain `:prediction`'s `:initial-belief` and
`:action`'s `:kind :id :target :precedence`. All selected values are verbatim;
no synthetic model input or rate. The projection is not an objective-verifier
replay: its test exercises the real seal producer and real rollout, while the
existing selected-outer namespace exercises full seal verification/projection.

## Gates and exact commands

`clojure -X:test :nses '[futon2.aif.m2y3-pattern-prediction-test futon2.aif.token-outcome-test futon2.aif.token-outcome-coapply-test futon2.aif.token-outcome-pair-test futon2.aif.outer-policy-token-stage-test futon2.aif.selected-outer-cascade-policy-test]'`

`tests.log`: exit 0; 37 tests, 464 assertions, 0 failures/errors. Includes M2y-1
(`outer-policy-token-stage-test`) and the extra selected-outer seal tests.

Mutation: replace `(if pattern? token [target token])` with `[target token]`.
`clojure -X:test :nses '[futon2.aif.m2y3-pattern-prediction-test]'`
`bad-case.log`: exit 1; 2 tests, 18 assertions, 2 failures, 0 errors. Both failures
are in `retained-90ca-freezes-native-pattern-wants`: exact native-token equality
and three-element identity. Restored the original expression, same command:
`restored.log`: exit 0; 2 tests, 18 assertions, 0 failures/errors.

`clj-kondo --lint src/futon2/aif/token_outcome.clj src/futon2/aif/selected_outer_cascade_policy.clj test/futon2/aif/m2y3_pattern_prediction_test.clj`
`kondo.log`: 0 errors, 0 warnings.

`emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval '(arxana-check-parens-cli)' src/futon2/aif/token_outcome.clj src/futon2/aif/selected_outer_cascade_policy.clj test/futon2/aif/m2y3_pattern_prediction_test.clj`
`parens.log`: OK. `git diff --check`: exit 0.

Validated source SHA256s (same bytes before mutation and after restoration):

- token_outcome.clj: `c314ca122405762537a67bac0b9dec2565040e338bade9975e26dfb6d109744a`
- selected_outer_cascade_policy.clj: `4ec00d339d4f67c0586c403df068e5c6c1b2d6d2937c6586e665b1134e88f6a7`
- m2y3_pattern_prediction_test.clj: `b5a20374d98bb225f48c7c3690421d1fce376fe7e07a3481686348296edfb541`

Controls additionally cover absent carrier, absent source inputs, double-qualified
want rejection, changed horizon/kernel identities, and tampering with a hashed
carrier. This receipt establishes prediction only, not measurement or falsifier
acceptance.
