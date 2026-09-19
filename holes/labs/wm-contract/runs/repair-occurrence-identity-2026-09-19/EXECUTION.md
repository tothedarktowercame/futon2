# Repair occurrence identity — execution receipt

Date: 2026-09-19 UTC

Implementation commits:

- `0c9499f4` — occurrence identity, atomic finding reuse, observation records,
  and containment propagation
- `15ec4d6b` — create the canonical observation parent and compare retry ids
- `ab70f0cc`, `84c4c126` — correct the controls' file filtering and eager T8
  fixture realization
- `59867021`, `988ccb6f` — registry scope and explicit aggregate namespace

## Contract implemented

New runner-originated findings carry `:repair/occurrence` with a stable id
derived from `[authority-qualified origin, durable event/job id, typed failure
kind]`.  Display attempt ordinals are not the identity.  The finding filename
is derived from the occurrence id and remains protected by the existing
publication lock plus `CREATE_NEW`; exact replay reuses the immutable finding,
while different semantic bytes under the same occurrence refuse
`:repair-finding-conflict`.

Each observation is separately appended under `occurrence-evidence/` and
references the one finding.  Close containment propagates an existing
occurrence.  Initialization derives its occurrence from run authority plus a
retained trip/job/event identity and derives its display attempt label from
that occurrence instead of minting a random UUID.  Existing finding files are
untouched.  Tripwire witness identity itself is intentionally unchanged.

## Static gates

```text
clj-kondo --lint src/futon2/aif/repair_obligation.clj src/futon2/aif/full_loop_runner.clj test/futon2/aif/repair_obligation_test.clj test/futon2/aif/full_loop_runner_test.clj test/futon2/aif/repair_occurrence_identity_test.clj
```

PASS: 0 errors, 0 warnings; one informational pre-existing `str` finding.

```text
emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval '(arxana-check-parens-cli)' -- --no-defaults src/futon2/aif/repair_obligation.clj src/futon2/aif/full_loop_runner.clj test/futon2/aif/repair_obligation_test.clj test/futon2/aif/full_loop_runner_test.clj test/futon2/aif/repair_occurrence_identity_test.clj
```

PASS: `OK`. `git diff --check` also passed.

## Honest registration trail

The first submitted command used unsupported `-X:test :nses` syntax. The
registry refused it as `:explicit-namespace-required` before executing tests;
it minted no evidence id. An explicit aggregate namespace was then committed.

Three executed attempts are retained:

1. `test-registry-4d57920b28c271496177677e3e0ac0a91827715d83c665e606c796a349ccb876`
   — 5 tests, 11 assertions, 2 failures, 3 errors. This exposed a missing
   canonical parent for occurrence-evidence locking and a timestamp-vs-id test
   assertion.
2. `test-registry-dba255a467afe37b7432b3e324bdf8f77c49173987a3b547774eed774f714bd7`
   — 5 tests, 16 assertions, 3 failures. Two controls counted the publication
   lock as a finding; the T8 fixture used a lazy publication sequence.
3. `test-registry-3f4cf19e72daf1a85d48946ebec8f15ed7cd14861c6d5e627975ee2e308c8e32`
   — 5 tests, 16 assertions, 1 failure. Direct readback proved T8 worked; the
   remaining fixture still read before realizing its lazy writes.

The corrected registration passed and bound the subject:

```text
evidence-id test-registry-97ab21445fa4626c876c9b550ab614d945d851f2bf12dcbd1c476fdded8e4a53
warrant? true
results {:assertions 16, :duration-ms 4559, :errors 0, :exit 0, :failures 0, :tests 5}
bound repair-store/occurrence-identity -> test-registry-97ab21445fa4626c876c9b550ab614d945d851f2bf12dcbd1c476fdded8e4a53
```

Passing artifacts:

- `9a73c2e3-aad6-4e48-a93b-b4c8cdef6bbc.closure.edn` — SHA-256 `159f14540f96d834a23e04c0b3d036247764e56b0691a5efe74b848fcaddcc6e`
- `9a73c2e3-aad6-4e48-a93b-b4c8cdef6bbc.log` — SHA-256 `0f5e780dd173f6143c3cf32fecec6506e565fcc9752201d824ff3c887ed6e8ae`

No repair-store mutation outside temporary fixtures, finding disposition,
machine click, or serving-JVM contact occurred.
