# Historical successor/store review — 2026-09-11

Reviewed 37c31a30/e119acf9/78d905a9/4d9d73ba. Real terminal reader and
historical transition tests passed 10/37 before local changes. The adapter
uses the existing durable evidence bundle and accepts only its successful,
safe grounded-change classification; unknown/missing evidence refuses.

Local corrections: store independently requires validation-attempt differ from
verification-attempt as well as failed-attempt; copied verification bytes must
still match the candidate digest at the second read; stored execution ID is
shape-validated; verification-evidence directory receives the same canonical
no-symlink guard as the admission directory. The real-reader test supplies a
capability with the same attempt at the store boundary and asserts refusal.
A capture-port race control rejects copy drift. Directory controls cover both
internal directories. Tests: state 2/19, terminal/successor 10/38, repair 9/36;
lint 0/0, parens OK, diff checks clean.

Integration requirement: define the identity grain explicitly. The bundle's
:attempt-id is the controller attempt; :terminal-projection/:attempt/id is the
execution attempt. Do not compare a local execution ordinal with a controller
ID and call their unequal strings proof of distinct executions. The runner
must derive and persist a typed/bound verification execution identity from
its actual context, and the successor reader must compare like identities.
A same-execution/different-controller-label control must refuse. If controller
identity is the chosen grain, declare it and retain the durable execution join.

Now connect side-effect-free candidate selection, distinct historical action
execution, typed outcome/checkpoints and repair transition to run-opportunity!.
Keep ordinary author/grounding paths unchanged. No invented code commit or
U88-success verdict for verification. Require actual isolated full lifecycle
before live activation. Failed historical attempts/evidence remain unchanged.
