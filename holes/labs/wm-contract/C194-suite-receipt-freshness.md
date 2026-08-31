# C194 — suite freshness is an evidence blocker

Date: 2026-08-31.

`run-readiness` distinguishes `unavailable` from `unverified`. Reviewer absence
and exhausted admission are unavailable runtime resources. A stale suite
receipt is unverified: the machine may execute, but there is no passing result
for the code it would execute. Both refuse the run without claiming the same
cause.

Current policy is conservative. A bounded suite receipt is fresh only if it is
green, finishes after the repository's current commit time, and the tracked
tree is clean. Therefore the final code-affecting action before Joe's run is a
bounded futon2 suite run. The ordinary durable job
`bg-1788212323446-5` runs `make ci` but cannot refresh readiness because it
produces no bounded outer/resource receipt.

Content identity would be more precise than time: a docs-only commit need not
invalidate executable tests, while a dirty executable change must. But the
current receipt does not record the tested repository tree or tracked diff, so
there is no honest content comparison to make retroactively. The future shape
is `HEAD tree SHA + tracked-diff fingerprint` captured at test start and end;
readiness can then compare that basis to the runnable workspace. Until that
schema exists, the roughly two-minute rerun is cheaper and safer than weakening
the proxy.
