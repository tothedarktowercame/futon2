# Declared-effect live wiring review — 2026-10-22

Status: implementation contract incomplete; no Trial or deployment. Trial5 remains closed as build-failed. Pure producer job2378 is independent.

## Executed constraint

`close_retention.clj:87` accepts exactly six mint-input keys. The standalone `declared-effect-occurrence-binding-probe.clj` adds only `:declared-effect-sha256` and asserts the actual `:shape-invalid` refusal. Result is retained in the adjacent EDN. The earlier advice to pass a digest into mint-action-occurrence-once! is not supported by the current occurrence schema.

## Required production connections

1. `full_loop_runner.clj:7087`: selection checkpoint currently freezes prediction from the scoring decision. A compiled route needs separate declaration observations and effect prediction, without representing these as scoring inputs.
2. `full_loop_runner.clj:7201` and `close_retention.clj:87`: mint canonical occurrence once. Proposed separate versioned binding receipt immediately afterwards: occurrence identity/digest, selected target, declared-effect record digest, source/base revision pins. Verify against the immutable selection record at dispatch and close. This is a proposal pending contract review, not an implemented binding.
3. `d_predecessor_task_authority.clj:402`: context obtains universe only from token-belief-stage prospective carry. Compiled route lacks this. Effect universe must come from the frozen declaration observation record with explicit source, not a synthesized scoring certificate or belief-carry receipt.
4. `full_loop_runner.clj:7518` and `d_predecessor_task_authority.clj:115`: capture-result snapshots declaration reads. Wire the same exact source pins and verify bytes unchanged before dispatch; changing HEAD later must not move the before observation.
5. `full_loop_runner.clj:7769`: build measurement/comparison and selected-want accounting must consume the frozen effect record and corresponding observation locators. Preserve unknown observations as unknown and actual prediction errors, including zero-predicted/observed true.
6. `full_loop_runner.clj:5917`: terminal measurement and accounting must use identical record/binding. Changing only the build call would leave close reporting inconsistent. Preserve existing legacy vector callers explicitly.

## Review questions

Can a separate versioned binding receipt provide the same occurrence-to-declared-effect invariant while leaving action and scoring certificate identities unchanged? Specify exact verification ownership and required retain/dispatch/close joins. If occurrence itself must embed the digest, define a versioned occurrence migration with explicit legacy handling; never relax exact-map validation ad hoc.

No declaration/N/A exception is permitted. A target's authored wants, locator and horizon remain external to the run. No target declaration is created by this review.
