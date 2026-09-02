# C477 — AC1: the prediction triple stops substituting zeros

Ledger row `:AC1` (`worklist.edn`). Decided by Joe's 2026-09-02 ruling
(`holes/problems/DECISIONS-PENDING.md`, futon2 `2f34c26`), which settled all
seven C130 migrations at once on the self-repair condition. This record is the
AC1 half: `free_energy.clj:98-100` in the C12 census, tally instance
`:prediction-triple`.

## What the site did before

`compute-prediction-error` took three inputs and defaulted each of them to a
number nobody measured:

```clojure
(let [pm (double (:mean prediction 0.0))
      pv (double (:variance prediction 0.0))
      o (double (or observed 0.0))
      ...
```

and the war-machine inner loop fed it a fourth substitution of the same kind,
`(get observation ch 0.0)`. Because `observation/observe` already writes 0.0
into every channel it could not source (`src/futon2/aif/observation.clj:121-146`,
recording the substitution only in `::channel-statuses` metadata), that
`(get observation ch 0.0)` never fired its own default — the zero arrived
pre-substituted and typed as absent in metadata the caller did not read. So the
belief update scored an unmeasured channel as an observation of zero, and the
resulting `:prediction-error/v1` record proved its own output shape, not the
presence of its inputs (this is the bound C180 stated).

## What it does now

Three typed records, and which one is emitted is the decision C130 §2 asked for
(`holes/labs/wm-contract/C130-absence-decisions.md:23-32`):

| verdict | when | what the caller does |
|---|---|---|
| `:present` | observation and both likelihood members are finite numbers | scores exactly as before |
| `:absent` | the OBSERVATION was not taken | omits the channel, carrying `:reason` and `:paths` from the envelope |
| `:refused` | a MODEL parameter is missing, or any member is present but not finite | refuses the whole update; nothing scores |

`:absent` records carry no `:observed`, no `:error`, and no `:weighted-error` —
the point is that there is no number to carry. `:refused` records name every
offending member under `:offending`.

**Absent is not malformed, and that is the whole split.** An observation nobody
took omits its channel; a likelihood that produced no mean is a producer defect
and stays loud. C130 §2's reading was "A for genuinely absent observations, B
for missing model parameters", so a partial or non-finite `{:mean :variance}`
map refuses rather than omits.

## Pointers

- Producer: `src/futon2/aif/free_energy.clj:197-272`.
- Envelope-reading form the loop calls: `src/futon2/aif/free_energy.clj:274-289`.
  It reuses the observation-status logic that `channel-reading` already had,
  now factored out as `channel-source-status`
  (`src/futon2/aif/free_energy.clj:33-57`) and returning the RAW value, because
  a caller that has to tell a non-numeric value from an absent one cannot be
  handed a coerced double.
- Caller, omit and refuse: `scripts/futon2/report/war_machine.clj:4839-4860`.
  A refused channel empties the errors map, which passes precision state
  through unchanged (`src/futon2/aif/precision.clj:190-193`) and drives no
  belief event — that is what refusing the update means at this seam, and the
  loop then terminates rather than iterating on nothing
  (`scripts/futon2/report/war_machine.clj:4928-4938`).
- Persistence (the self-repair condition): `src/futon2/aif/trace.clj:530-538`
  writes `:prediction-triple-events` present-only, and the per-step counts land
  present-only in the micro-step trace
  (`scripts/futon2/report/war_machine.clj:4913-4926`). A tick with a complete
  triple on every channel writes the same bytes it wrote before AC1; the key is
  absent, which is a different claim from "the producer did not report".
  AC8's harvester is what turns those records into proposed work items; this row
  only guarantees they exist and are typed.

## Behaviour change, stated rather than buried

This is not a no-op on live ticks. Every channel of `channels-with-likelihood`
whose source field is missing from the scan data used to enter the belief update
as an observation of 0.0 and now does not enter it at all. Which channels those
are depends on the tick's scan data; the omission records are what say so per
tick, and they are the first per-tick evidence of it (C180 called this
"partly measurable now; one new WM trace field for full coverage" — this is that
field).

## Gates

- `clj-kondo` on the four changed Clojure files: 0 errors, 0 warnings.
- `futon4/dev/check-parens.el` on the same four: OK.
- `clojure -M:test -m cognitect.test-runner -d test/futon2`: **971 tests, 5854
  assertions, 0 failures, 0 errors** (was 965 at C473 — seven new deftests, one
  removed: `compute-prediction-error-defaults-test` asserted the coercion this
  row removes, so it could not survive the migration and is replaced by the
  absent/malformed pair).
- Planted cases, `test/futon2/aif/free_energy_test.clj:207-280`: absent
  observation (bare `nil` and through a real empty-scan envelope), missing
  `:mean`, missing `:variance`, empty map, `nil` prediction, non-numeric
  `:mean`, `NaN` `:variance`, infinite observation, and a direct test that the
  absent and malformed verdicts differ on the same channel.
- `bb checks/preemptive_absence_coercion_lint.clj`: findings 7 → **6**; the
  `free_energy.clj:98-100` finding is gone and the six remaining are AC2-AC7's
  sites. Negative control still rejects its mutation.
- `bb p4ng/empirics-futon/pointer_check.bb`: 2 unresolved, **both pre-existing
  and neither mine** — AC7's `fulab.clj:81`, which the checker cannot resolve
  because `src/futon2/aif/adapters/` is not on its root allowlist. Same defect
  class the checker's own comments record twice (C472, C473); left for AC7
  rather than fixed here.

## What this row did NOT do

- `belief.clj:1040-1052` (`r3d-aggregate-driver`) is AC2's site and is
  untouched. It still reads `(:weighted-error err-map 0.0)`; it is now only ever
  handed `:present` records, but its own coercion is AC2's to remove.
- No ruling is recorded here. The decision is Joe's, already in
  `DECISIONS-PENDING.md`; this record is its implementation.
