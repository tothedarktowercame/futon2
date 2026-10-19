# Ruling: the frontier is measured, not pass/fail (2026-10-19)

Joe, dictated at ~07:55Z on 2026-10-19, after a day of clicks ending at
selection boundaries (`outer-policy-evidence-provider-required`, then
`incomplete-policy-menu-coverage` / `no-current-target-reading`), each
boundary a gate with no producer behind it:

> "The running it offline can be hazardous because the only machine that
> is properly configured to run on production is one that's actually been
> running on production the whole time, so just running in test JVMs that
> don't have all the data loaded won't work. [...] Trying to get it to run
> as is and use that as a measurement is sensible: if it says 'I've only
> assessed 5 things' we should write that down. That's one of the things
> that the Lean certification for runtime behavior is supposed to provide,
> but if it's erroring out and not actually giving a run, there is no such
> thing as runtime behavior; all the runtime behavior is a fail. We should
> look at the frontier as a measurable, qualitative and quantitative
> feature, not as a binary pass-fail. That's the problem: Codex has been
> treating this as if it was a pass-fail binary test rather than an actual
> research project."

This restates, in terms the code can read, the 2026-09-19 guards ruling
(nothing halts WM runs during tuning; evidence yes, vetoes no) that the
selection boundaries built since have not honoured.

## Operative consequences

1. **A boundary that finds an input absent records the absence and
   proceeds with what exists.** The typed absence goes into the run
   certificate (what was required, what was present, what was assessed).
   It does not end the run. "Absence must not read as a value" still
   holds: the absence is recorded as an absence, never defaulted to a
   number; but it is recorded on a run that continues, not on a run that
   stops.
2. **Every run reports its frontier as a measurement.** Counts and names:
   targets with a current reading over eligible targets, policies scored
   over policies in the menu, authority terms declared over terms the
   objective uses, and so on. A run that assessed five targets says five.
   The Lean runtime certificates certify a run; a run that errors before
   selection has no runtime behaviour to certify, so an erroring run is a
   measurement of zero, not a certificate of anything.
3. **"Offline" means the serving JVM.** A replay or harness proves a
   route only in the process that has the production data loaded, with
   the debugger attached and no click spent. A fresh test JVM without the
   data is not a proof of the route; its green result does not authorise
   a live click. (This corrects step 3 of
   futon3/library/apparatus/recorded-failures-before-live-runs.)
4. **Immediate application.** Selection produces target readings on
   demand for the targets it is weighing and records how many of the
   eligible targets it assessed, rather than requiring a stored reading
   for every eligible target before choosing any. The strict outer
   objective reports its verdict and its missing terms as evidence on the
   run; it does not veto the click.
5. **Gates are paired with producers.** A new boundary that can refuse
   for a missing input is not finished until the step that produces that
   input in production exists or the absence is recorded-and-continued
   under (1). A refusal test with a fixture that passes is not that step.

Context: holes/labs/wm-contract/outer-policy-production-route-3e2d-2026-10-19.md
(the fail-closed provider boundary); /home/joe/notes/codex-1-abstention-review/
one-click-2026-10-19-result.md (the reading-coverage boundary);
RULING-selection-precedence-2026-09-19.md (the guards ruling it restates);
futon3/library/apparatus/{special-flow-is-a-path-not-a-trail-of-exceptions,
open-conditions-live-in-a-ledger}.flexiarg.
