# C162 — pre-merge timing

Date: 2026-08-31. Method: `/usr/bin/time` around complete Make targets from a
fresh process, followed immediately by a warm repeat. Kernel caches were not
dropped. Full stdout and timing files were retained in `/tmp/c162-*` during the
measurement session.

| Target | Cold wall seconds | Warm wall seconds | Outcome |
|---|---:|---:|---|
| `make ci` | 105.65 | 101.61 | exit 0 / exit 0 |
| `make workspace-gate` | 32.91 | 33.53 | exit 0 / exit 0 |
| `make pre-merge` | 118.08 | 125.35 | exit 0 / exit 2 |

The red warm pre-merge completed CI (1,045 tests / 6,250 assertions) and then
reported concurrent strict-contract drift. It is included as observed wall
time, not misreported as a pass. A prior warm attempt stopped during CI after
89.62 seconds when the preemptive-repair gate observed another concurrent
artefact boundary failure.

One streaming-timestamp profiling run measured CI at 106.22 seconds. Its main
consumers were `wm-operational-certificate-test` 28.17 s,
`r8-f-contract-test` 25.92 s, `futon2.aif.full-loop-runner-test` 12.64 s, and
`r2-channel-contract-test` 12.40 s. Together they account for about 74% of the
run. A profiled workspace gate took 29.72 seconds; the
`c116-pre-boundary-stored-f` semantic control alone took 17.95 seconds (60%).

Decision: approximately two minutes is fast enough for the named pre-merge
review, so there is no tier split. Removing checks now would trade a measured
bounded cost for an unspecified schedule. If operator evidence later shows
skips, optimize the five measured dominators first. Work moved to nightly in
future must have a named command and cadence; nightly is a schedule, not an
exemption.
