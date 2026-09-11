# Independent control-fix review and next executable slice

Reviewed futon2 3dcbb4d7/68e7706a and futon3c b075c323/5cf767de.
Accepted within their implemented boundaries.

The retained c6a463b6 marker reproduction now returns :incomplete with
"port failure", one attempt and one closed attempt. The catch compares object
identity with an internal completion token, emitted after transition validation;
an arbitrary exception data boolean cannot authorize admission. This is an
in-process control-flow guard, not isolation against privileged Clojure code.

Independent trusted entry: 7 tests / 45 assertions, zero failures/errors.
Independent C-fold config: 3 / 16, zero failures/errors.
Independent deployment materializer: 5 / 12, zero failures/errors.
Frozen seeded-c.edn SHA256 remains
1459952e2d3e4534760ba8512482fc3cb3aa4020d2c3464b2056efaff6112e1f.
Historical control outcomes are excluded from the probabilistic disposition
carrier; this does not assert a learned preference over historical admissions.
The materializer accepts historical authority only from server dependencies.

## Next implementation, already authorized

run4_terminal_projection/projection currently returns nil when neither
selection nor construction contains :run4/task-pin. This is correct for the
existing enacted-task schema. Historical admission instead retains
:run4/requested-pin, marked authenticated-not-enacted, and actual selected
repair action. Preserve this distinction in a versioned historical projection.

Reuse the existing durable admission/click/binding/run-record joins. Bind the
requested pin digest, actual action, execution identity, canonical verification
artifact and admission, cohort identity/checkpoints/close, and source digests.
The reader must recapture and validate these artifacts itself. Publish before
binding reference, and refuse missing, corrupt or cross-identity evidence.
A historical outcome establishes awaiting-validation only, never repair
resolution, grounded-change, or success of a requested U88 action.

Carry that typed result into recording, visibility and a separate controller
transition without weakening existing task-result validators. A distinct
production successor remains mandatory; continuation needs its own eligible
frozen pin and ordinary gates. No automatic replay of consumed admission.

The positive gate is actual materialized configuration through async runner,
activated disposable cohort, real historical store, close/run record, projection,
strict reader, recording and controller/visibility. Worker/review ports may use
explicit fixtures; durable admission and outcome readers may not be stubbed.
Include source drift, foreign click/attempt, missing close/admission, duplicate
step and zero-capacity existing inspection controls.

No live capacity, attempts, services, credentials or historical evidence changed.

Independent full runner: 130 tests / 617 assertions, zero failures/errors.
