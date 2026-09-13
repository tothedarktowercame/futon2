# E6 contract review — 20644

Reviewed 42f2a9bd against actual source. Eight table pins (seven unique files) match current bytes, retained in lead-pins.json. Documentation only, no tests or runtime operations performed.

Adopt E6a/E6b as separate bounded isolated verifier contracts. Lead updated stale E5 prerequisite to accepted7ec1e99a and clarified comparison-arm identity versus actual event enactment. No unchanged selection counts as behavioral influence. Independent canonical R6 score/posterior correspondence remains required; joining asserted scores cannot replace it.

E6b must validate boolean success and complete prior state before calling advance-slow-state: current function defaults missing success to failure and absent class state to fresh-entry. Its exactly-once result can only be conditional on independently complete ledger evidence, not enforce live storage. Pin transitive intrinsic update dependencies. Expected transition/context, ledger completeness and outcome authority must be outside candidate self-claims.

Next: codex22 pure E6a, codex23 pure E6b, separate owned files and isolated sources; independently review lead specification edits in those packets. No runtime consumption, persistence integration, actual action or certificate authorized.
