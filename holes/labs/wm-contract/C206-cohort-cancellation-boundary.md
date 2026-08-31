# C206 — cohort 46 cancellation boundary

Date: 2026-08-31

## Finding

A cancelled operator attempt is **accepted, closed, and counted** by the current
cohort machinery. It is neither rejected nor excluded. `:cancelled` is in the
runtime `outcome-kinds` (`src/futon2/aif/full_loop_cohort.clj:25-33`), the runner
closes a cancelled Agency job with that outcome
(`src/futon2/aif/full_loop_runner.clj:735-750`), and the cohort ledger counts
attempt directories rather than filtering by the preregistered
`:counts-as-attempt` vector (`src/futon2/aif/full_loop_cohort.clj:354-373`).
The cancellation control exercises that path
(`test/futon2/aif/full_loop_runner_test.clj:1799-1815`).

Cohort 46 did **not** preregister `:cancelled`; its closed-outcome list is the ten
values at `holes/labs/M-aif-full-loop-46/cohort.edn:12-24`. Consequently a
cancelled attempt consumes an attempt ordinal and appears in the ledger, but it
cannot honestly be pooled under cohort 46's original outcome taxonomy. Its
analysis begins a new semantic stratum. Retrospectively adding the outcome to
the preregistration would erase the boundary preregistration exists to expose.

The formal `Cohort` added by C179 states the right carrier shape: semantic epoch
and `preregisteredOutcomes` are explicit, with `OutcomeClass` an epoch-specific
type parameter (`mathlib4:DarkTower/WarMachine/Holes.lean:111-119`). It does not
currently validate the runtime cohort file or split runtime ledgers. Thus it
specifies the boundary but does not absorb this attempt automatically.

## Operator consequence

Joe must know before the run: cancelling the Agency job is operationally safe
and recorded, but the resulting attempt starts a new analytic stratum and must
not be pooled with cohort 46's preregistered outcome classes. This is an
operator-visible consequence, not a request to amend cohort 46 and not a new
pending design decision.

## Other post-preregistration vocabulary

The history from cohort 46's preregistration (`eeb5e67`) to the current tree has
one addition to `full_loop_cohort.clj`'s closed-outcome vocabulary:
`:cancelled` at `9b0dac1`. The 2026-08-20 change `4fea3b3` typed an ineligible
trigger as `:incomplete` but deliberately did not expand the preregistered
trigger or outcome set.

The other typed values added today do not enter cohort 46's preregistered
taxonomy:

- `:avoidance-unknown` is a rendered diagnostic
  (`scripts/futon2/report/war_machine.clj:4185`; control at
  `test/futon2/report/war_machine_test.clj:21`).
- `:legacy-era` and `:malformed` classify prediction/precision provenance
  (`src/futon2/aif/precision.clj:55-68`).
- `:predates-field` and `:malformed` classify trace-schema reads
  (`src/futon2/aif/trace.clj:388-402`).
- `:channel-gap-exclusion` is a belief-update diagnostic
  (`scripts/futon2/report/war_machine.clj:4058`; control at
  `test/futon2/report/war_machine_test.clj:61`).

None is read by `full_loop_cohort.clj`, appears in cohort 46's
`:counts-as-attempt`, `:excluded`, `:checkpoint-order`, or `:allowed-triggers`,
or changes its semantic-epoch taxonomy. The sweep therefore found one affected
vocabulary addition, not five.

## Verification

- `git diff eeb5e67..HEAD -- src/futon2/aif/full_loop_cohort.clj` — only
  `:cancelled` expands `outcome-kinds`; the other change types an existing
  refusal.
- `clojure -X:test :vars '[futon2.aif.full-loop-runner-test/cancelled-author-closes-distinctly-from-build-failure]'`
  exercises the accepted-and-closed cancellation path.
- No preregistration file was amended.
