# War Machine outer/full-loop cohort — preregistration

This is a fresh evidence epoch. The historical WM achievement ledger remains
historical evidence, but none of its ticks, artifact enactments, or mirrored
outcomes belongs to this cohort.

The unknown contents of the forty attempts are not preregistered. The protocol
and stopping rule are: after an explicit activation marker is written, take the
first forty distinct natural wall-clock opportunities in order. An opportunity
counts even if the machine abstains, finds no action, has no suitable agent,
hits a guardrail, fails to dispatch, fails to build, or makes no grounded
change. Manual tests, replays, pre-activation events, and duplicate scheduler
delivery do not count. There are no replacement attempts.

This makes agent availability and machine state measured parts of the process,
not inclusion criteria. It also avoids forcing mission diversity: repeated
selection of one mission would be a result about the controller, not a reason
to alter the cohort.

## Flight dossier

Each attempt is an append-only sequence of typed checkpoints:

1. `:time-step` — opportunity identity, trigger, machine state, agent roster,
   code/configuration state, and a semantic-epoch identifier.
2. `:selection` — selected mission/action, ranked alternatives, and the
   machine's recorded reasons. Abstention is recorded here.
3. `:construction` — cascade, open sorries, wiring diagram, deposit and
   patterns. This resembles a Mission document but does not assert that the
   Mission lifecycle was followed.
4. `:dispatch` — chosen agent, availability, prompt/deposit reference and job
   identity, or the reason no dispatch occurred.
5. `:build` — returned artifacts, generated code, commits, validations,
   patterns actually used, and any in-line improvements.
6. `:adjudication` — independently derived before/after witnesses,
   build-match, and substrate dial result.
7. `:closed` — outcome, duration and resource use.

Every payload is either a term with a ground or a typed sorry. Missing
information is therefore data. An artifact-only construction may be logged,
but it cannot be classified as grounded success. `:grounded-change` requires a
resolved independent before/after witness and a moved dial.

The machine-readable commitment is [cohort.edn](cohort.edn). Activation is a
separate append-only operation so the preregistration can be reviewed before
the clock starts. The writer refuses activation if the protocol is malformed,
refuses a 41st attempt, refuses duplicate opportunity IDs, and refuses closure
until every required checkpoint is present (as a term or a typed sorry).

## Evaluation

The denominator is all forty opportunities. Primary results are dispatch,
completion, grounded-change, and grounded-no-change rates. Secondary reporting
covers selection concentration, stated reasons, calibration, availability,
time/resource use, patterns, in-line improvements, and the code-version
trajectory. Failures and typed sorries are reported directly rather than
filtered out.

This cohort is observational: it evaluates the machine operating in whatever
state it reaches at each time-step. Normal development may continue during
collection: the cohort describes forty consecutive opportunities of an
evolving machine, not forty replicates of a frozen binary. Each attempt must
record the loaded Git identity, dirty status, resolved mode flags,
configuration digest, and semantic epoch. A semantic epoch increments whenever
ranking, policy support, dispatch, build, adjudication, or grounding semantics
change. Results are reported in chronological order and stratified by epoch;
an upgrade never permits discarding or replacing an attempt. Deliberate
experimental interventions, alternative selectors, or baselines still belong
in separately preregistered cohorts.
