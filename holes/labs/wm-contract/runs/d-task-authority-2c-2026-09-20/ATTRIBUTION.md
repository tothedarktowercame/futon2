# D 2c full runner attribution

Exact baseline: aae84fc302207cd79cdf0adf9353706beb2b516a.
Both comparable runs execute 180 tests / 966 assertions.

| Attribution | Failures | Errors |
| --- | ---: | ---: |
| Reproduced on committed baseline | 43 | 1 |
| Concurrent production-store write observed by draft fixture | 1 | 0 |
| Draft-introduced | 0 | 0 |

The comparable baseline exits 1 with 43 failures and 1 error. The draft exits 1
with 44 failures and 1 error. This is NOT a claim that all 44 failures reproduce
on baseline. The extra draft failure has a separate, corroborated provenance.

attribution-comparison.json retains each failure/error header, assertion source
location, expected expression, actual diagnostic, and log line. It matches all
43 baseline failed assertions and the same historical EDN reader error. Only
compiler-generated gensym suffixes in expected expressions are normalized.
Raw diagnostic differences remain: generated repair IDs, object identities,
timestamps, temporary paths, and draft dispatch metadata. Nonempty dispatches
remain nonempty in both arms; the historical reader error is the same unknown
#object tag. No test was skipped or assertion weakened.

The single unmatched assertion is hermetic_repair_fixture.clj:40. Its before/
after file sets differ by exactly trip-63155249-d070-4d14-89fd-5f05279022b8.edn.
The retained trip records T8/C1 at 20:52:02Z. Matching production phase events
bind its opportunity e487c964-b81d-4abd-886d-fa135bc90ace to machinery-65
attempt-002. That opportunity is absent from the draft test log. The phase
sequence continues after the test run and ends at 20:54:05Z. This is concurrent
production activity observed by the fixture, not a draft-created test artifact.
See concurrent-production-trip.edn, concurrent-production-phases.ednlog, and
concurrent-production-evidence.json (source paths and hashes).

The first full baseline invocation omitted :test dependencies; the construction
emitter could not load babashka.fs. Its 43 failures / 2 errors are retained as
baseline-full-runner-incomplete-classpath.* and are NOT the comparable baseline.
The corrected invocation includes :test and preserves the loaded-source byte
equality guard against the exact baseline checkout. No shared JVM was loaded.
The full namespace exercises construction tests, but no construction-fold
implementation or legacy fixture was modified. Codex-8 was notified of the
production T8/C1 evidence.

Under Claude-12's zero-draft-introduced-failure ruling, this clears the runner
regression gate; it does not make the runner namespace green. Legacy recovery/
historical fixture migration remains a separate packet. D's focused precommit
runs pass 15 tests / 131 assertions, with clean lint and parentheses checks.
The landing retains C3/C4 revision-pair affirmation only; other readers remain
explicit unavailable measurements. Conditioning remains :not-wired.
