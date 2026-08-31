# C225 — cross-repository pin-behind state

Date: 2026-08-31. Owner: `wm-organization`.

## Decision

`q_interface_completeness_check` distinguishes a historical pin whose file has
moved as `PIN_BEHIND`. It remains a failing state. The report names the commit
whose blob matches the pin and every later commit that changes that same path.
It does not use an age window, retry, or inferred landing grace period.

Git can establish that the pinned content existed and that later commits
changed the pinned spine. Git cannot distinguish a two-repository delivery
mid-landing from an abandoned follow-up pin: both have the same repository
state. The report therefore carries
`distinguishable-cause? false` and
`reason :git-proves-content-change-not-landing-intent`. A dispatcher can judge
the named commits quickly, but the checker does not turn that judgement into an
automatic pass.

If the pinned hash is not found in the path's Git history, the separate failing
state is `STALE_UNATTRIBUTED`. Missing inputs remain `UNAVAILABLE`; matching
content is `CURRENT`.

## Falsifier and canonical invocations

```sh
bb checks/q_interface_completeness_check.clj
bb checks/q_interface_completeness_check.clj --negative-control
bb checks/q_interface_completeness_check.clj --negative-pin-behind
```

The pin-behind control selects a genuine earlier `Holes.lean` blob. On the
2026-08-31 run it pinned `871160aaca…`; the checker named the later
spine-changing commit `3aa5a59b0c…`, returned `pass? false`, and accepted the
negative control. Thus a pin left behind by a real spine change remains red.

The live pin had already caught up concurrently when this delivery ran, so the
positive check reported every pin `CURRENT` and exited 0.

The full workspace gate was deliberately not run: C222 was repairing its
scheduling and the dispatch explicitly identified it as contended.
