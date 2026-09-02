# C479 — AC3: strategic-mode inference stops substituting zeros

Worklist row `:AC3` (class I). C130 §3 migration at the site the census records
as `src/futon2/aif/free_energy.clj:138-143`, tally instance `:infer-mode`.
Decided by Joe's 2026-09-02 ruling (`DECISIONS-PENDING.md`, futon2 `2f34c26`),
whose reading for this site is **option A**: reason-bearing `:unknown` when a
required feature is absent, and no partial or prior rule.

## What the site did before

`infer-mode` read all six of its features through a `0.0` default:

```clojure
(let [stack (get obs :stack-pct 0.0)
      consulting (get obs :consulting-pct 0.0)
      loop-h (get obs :loop-health 0.0)
      active (get obs :active-repo-ratio 0.0)
      ticks (get obs :ticks-firing-ratio 0.0)
      depositing (get obs :depositing-signal 0.0)]
  (cond ...))
```

and then branched on them. Six absences arrived as six zeros, so the first
branch — `(and (< active 0.2) (< loop-h 0.3))` — fired and the function
returned `:dark`. **Measured, both directions, on `(obs/observe {})`:** the new
producer returns `{:status :unknown :mode :unknown :reason
:required-feature-absent}`; the old six-default branch structure on the same
observation returns `:dark`. That is the defect stated plainly: an empty scan
was classified as a *system in the dark*, which is a claim about the system,
not a report that nothing was observed. Absences could also suppress
`:depositing` (consulting and depositing both read as 0.0) or help satisfy
`:hermit` (`consulting < 0.05` and `depositing < 0.05` are satisfied by
absence).

## What it does now

Each of the six features is read through the observation envelope via the
factored `channel-source-status` (`src/futon2/aif/free_energy.clj:33-57`), the
same reader AC1's `channel-prediction-error` uses. The producer
`infer-mode-record` (`src/futon2/aif/free_energy.clj:349-423`) emits one of
three typed records:

- **`:present`** — all six features are finite numbers. Carries `:mode` (one of
  the seven mode keywords) and `:features`, the six doubles the classification
  rests on, so a reader can re-derive the verdict from the record alone.
- **`:unknown`** — at least one required feature was **not observed**. `:mode`
  is `:unknown`, `:absent` names *every* missing feature with the envelope's own
  `:reason` and `:paths`, and **no `:features` key is written**: no
  classification is offered, so none is claimed.
- **`:refused`** — a feature is present but **not a finite number** (a string, a
  keyword, NaN, an infinity). `:mode` is `:unknown` and `:offending` names every
  such feature with the value it was given. A refusal still carries `:absent`
  when both faults occur, so neither is lost.

Absence and malformation are separated here exactly as they are in
`compute-prediction-error`: a feature nobody measured is a gap in the scan; a
feature that arrived as a string is a producer defect and has to stay loud.

**Why all six are required** (`strategic-mode-features`,
`src/futon2/aif/free_energy.clj:300-311`): the classifier's last branch is
`:else :multiplied`, so returning `:multiplied` asserts that none of the six
other conditions held — a claim about all six features at once. There is no
subset on which a partial classification is sound. No prior/stale/partial rule
is specified here; C130 §3 says any such rule should be separately specified,
and it has not been.

The branch structure itself is unchanged, factored out as
`classify-strategic-mode` (`src/futon2/aif/free_energy.clj:313-347`) over a map
of six features the caller has already established are finite. What changed is
that it is no longer reachable with a substituted zero.

`infer-mode` survives as `(:mode (infer-mode-record obs))`
(`src/futon2/aif/free_energy.clj:425-436`), so callers that only render or key
off the mode keep the shape they had. It now returns `:unknown` where it used
to return a fabricated classification.

Plain explicit maps remain a supported legacy boundary, because
`channel-source-status` supports them: a caller handing over `{:stack-pct 0.2
…}` without observation metadata gets its six features read, and the keys it
left out are absent rather than zero.

## What is NOT decided here

Whether anything may **act** on `:unknown` is the separate hard-guard decision
(`DECISIONS-PENDING.md` §3, `C113-avoidance-unknown-safety-design.md`) and is
untouched. This producer's contract is only that it never invents a mode.

## Behaviour change, not a no-op — and its measured blast radius

Within `judge` the local `mode` binding reaches exactly two places
(`scripts/futon2/report/war_machine.clj`): `avoidance-losses` at `:5308`, and
the `:mode` / `:mode-prior` keys of the judgement at `:5310-5311`. No policy,
selector or actuator reads it. So on an unclassifiable tick:

- `:mode` is `:unknown` and `:mode-prior` is `0.0` — `pref/mode-prior` has no
  `:unknown` entry and the call site already defaults to `0.0`.
- `avoidance-losses` emits no `:avoided-mode` loss, because `:unknown` is not
  `:hermit`. It does not emit a positive claim either.
- The `:stop-the-line` metabolic override is upstream of this and unchanged: a
  tripped tripwire still sets `:stop-the-line` regardless of `base-mode`.

**Measured on real ticks:** across the twenty S1b records
(`holes/labs/wm-contract/runs/2026-09-01-s1b/wm-trace-s1b.edn`), all six mode
features are `:variant :observed` on 20/20 —
`records-with-an-absent-feature 0`. So on that corpus this row is
byte-identical: the record classifies, no `:strategic-mode-events` key is
written. The change bites on an incomplete scan, which is what it is for.

## Self-repair condition

A tick that could not classify carries the record itself out as
`:strategic-mode-events` (`scripts/futon2/report/war_machine.clj:4694-4701`), a
vector of at most one, and it persists **present-only** at
`src/futon2/aif/trace.clj:547-554`, beside AC1's `:prediction-triple-events`
and AC2's `:belief-aggregation-events`. No key means the tick's `:mode` was
inferred from six observed features — a different claim from "the classifier
did not report". AC8's harvester is what turns these into work items; this row
only guarantees they exist and are typed.

## Gates

- **clj-kondo** on the four changed files
  (`src/futon2/aif/free_energy.clj`, `src/futon2/aif/trace.clj`,
  `scripts/futon2/report/war_machine.clj`,
  `test/futon2/aif/free_energy_test.clj`): 0 errors, 0 warnings.
- **check-parens** (`futon4/dev/check-parens.el`): OK on all four files.
- **Tests**: `clojure -M:test -m cognitect.test-runner -d test/futon2` —
  **987 tests, 5996 assertions, 0 failures, 0 errors** (was 979/5929 at AC2:
  +8 deftests). `futon2.report.war-machine-test` requires
  `futon2.report.war-machine`, so the caller edit is compiled by the suite.

## Planted cases (`test/futon2/aif/free_energy_test.clj:303-426`)

**Absent.** An empty scan through `obs/observe` → `:unknown`, asserted
*specifically* not to be `:dark`, with all six features named and each carrying
`:source-field-missing` and a non-empty `:paths`. Each of the six stripped one
at a time from an otherwise complete scan, asserting that exactly that one
feature is reported — one missing feature is enough, there is no partial rule.
A legacy plain map missing two features, carrying `:status-metadata-missing`.

**Malformed.** A string, a keyword, NaN, `+Infinity` and `-Infinity` in
`:stack-pct`, each refused with `:offending` naming the feature and the value it
was given. Two malformed features at once, asserting both are named rather than
only the first.

**Both at once.** Malformed `:stack-pct` with `:depositing-signal` absent:
refuses, and still reports the absent feature.

**The separation itself.** `nil` and `"x"` in the same feature produce different
statuses and different reasons. And the control that gives the row its point: a
**measured** `:depositing-signal 0.0` classifies with `0.0` in `:features`,
while the same channel unsourced does not classify at all.

The pre-existing branch tests were rewritten against a helper supplying all six
features (`with-mode-features`), since a two-key map no longer classifies; a
`:stagnant` case was added, which the old set did not cover.

## Lint and tally

`checks/absence-coercion-dispositions.edn:34-37` flips `:blocked` → `:fix-now`
with the control. `bb checks/preemptive_absence_coercion_lint.clj`: findings
**5 → 4**, this site's finding gone, the four remaining being AC4–AC7's sites.

p4ng `empirics-futon/defect-repair-tally.edn` row `:infer-mode` `:open` →
`:repaired`. Totals over the fixed 61-instance population: 52 repaired / 8 open
/ 1 partial → **53 / 7 / 1**.

## Pre-existing red, not mine

`bb p4ng/empirics-futon/pointer_check.bb` reports the same two unresolved
pointers AC1 and AC2 both recorded: one pointer, quoted twice, in AC7's
worklist row, naming a file under `src/futon2/aif/adapters/`, which the checker
cannot resolve because that directory is not on its root allowlist.
`negative_controls.sh` fails on that one pointer and on nothing this row added:
558 pointers in 3 files, 2 unresolved, both the same one. (Named here without
the `file:line` form on purpose. `pointer_check` reads three registries —
`aif-equations.edn`, p4ng `control-map-edges.edn`, and `worklist.edn` — so
writing the pointer verbatim in this row's ledger evidence would mint a third
copy of the same red; keeping the same wording here keeps the two texts
matched.)

## Not done here

- The census `:at` key stays `free_energy.clj:138-143`. It is the join key the
  lint and the C12 census share; AC1 and AC2 set the precedent of naming the
  new lines in the `:control` prose rather than moving the key.
- `avoidance-losses` gains **no** `:strategic-mode-unknown` entry. It already
  emits an `:avoidance-unknown` loss per unknown *channel*, so the same
  treatment for an unknown *mode* is available and arguably owed — but the
  acceptance for this row is the typed record, and adding a rendered loss would
  change `:loss-count` on unclassifiable ticks. Recorded as an option, not
  taken.
- `:mode-prior` is `0.0` for `:unknown` because the call site defaults to
  `0.0`. That is the treatment `:scanning` already gets — `pref/mode-prior` has
  no `:scanning` entry either, and `infer-mode` has been able to return
  `:scanning` throughout. A prior of zero is a claim ("this mode is
  impossible") rather than an absence, so the pair is worth a row; it is a
  different site from this one and is not touched here.
- The figure is not regenerated (publish-time, TN §9a gate rule).
