# RUN4 qualification — stated before execution

Authority: Joe's Item 23 in
[futon2/holes/labs/wm-contract/RULINGS-walkthrough-2026-09-09.md](../../RULINGS-walkthrough-2026-09-09.md),
settling A5 of the final checklist. The qualifying conformance bar is the
preregistered `runConformsToDrawnWiring` meaning:

- The run records at least one route.
- No recorded route is empty.
- No recorded hop is unmapped.
- No recorded hop is a refutation (a code-retired pair traversed at route grain).

These are the assertions transcribed in
[01-assertions.edn](../F2-run4-preregistration/01-assertions.edn).
Qualification does **not** require every drawn edge to fire. The previous
census's one ruling-unrealised hop and 19 unfired edges do not, by themselves,
disqualify a run. Coverage counts must still be reported alongside conformance;
passing conformance is not a claim that every drawn edge was exercised or that
every component works in every setting.

Preregistration control C6 explicitly reserved which run qualifies to Joe:
[04-controls.edn](../F2-run4-preregistration/04-controls.edn), `:C6`, and
[run4_prereg_transcribe.bb](../../run4_prereg_transcribe.bb), lines 68–72.
Item 23 supplies the up-front criterion; it does not fabricate deposits,
prove conformance for a future run, or accept its certificate. The route
transcription and certificate must be produced over the actual run and current
wiring authority. Joe's acceptance remains the act that closes
`wmRunConformsToWiring`, as `:acceptance-closes` in the assertion manifest states.

This statement neither starts a run nor waives the other pre-go-live tasks.
In particular, readiness of the nine-line meter and completion of the separate
implementation checklist are distinct requirements.
