# P27 — R14 selection-gain learning and consumption reading

**Packet:** P27 / A22 / R14
**Disposition:** documentation-only correction proposed; no implementation conclusion beyond the
static source reading below.  Source pins in this note are to futon2
`a3603cb4804d82eb0dee792689505df9f7897995`, unless another repository and
commit are named.

## Finding

The R14 selection-gain update has one canonical numeric input: the selected
policy's signed realised-versus-expected performance.  It is not updated by a
regression, belief uncertainty, contradiction rate, or time pressure merely
because such a diagnostic changes.  The implementation's exact calculation is

```
perf = (expected-score - realized-score) /
       (abs(expected-score) + abs(realized-score) + 1e-9)
```

so, under the stated lower-score-is-better convention, a realised score below
the expected score produces positive performance; the reverse produces
negative performance.  This is `selection-gain/policy-performance`
(`src/futon2/aif/selection_gain.clj:85-99`).  `observe-outcome` is the
pair-to-performance adapter and calls `update-selection-gain`
(`selection_gain.clj:164-173`).  The latter appends that scalar, holds gain
at exactly 1.0 until five samples, then maps the bounded-window mean to
`clamp(2^(gain * mean-perf), floor, cap)`; default window/min-history/gain/
floor/cap are 20/5/1.0/0.5/2.0
(`selection_gain.clj:73-81,116-162`).  Thus a diagnostic-only movement with
the same expected and realised legs supplies no changed `perf` to this law
and must not be described as changing selection gain without a new consumer.

The static contract for a foldable observation is
`{:policy ... :expected-score ... :realized-score ... :tick ...}`
(`selection_gain.clj:175-208`).  A well-formed, non-duplicate outcome calls
`observe-outcome`; absent, malformed, or already-seen values retain the
state (`selection_gain.clj:234-246`).  That establishes the *update law*,
not an observation that such outcomes are flowing through a current run.

## Consumers and reachability

Relevant callers/readers found in this reading:

| Component | Relationship | Evidence and limit |
|---|---|---|
| `selection-gain/observe-outcome` | sole local pair adapter into `update-selection-gain` | `selection_gain.clj:164-173`; static |
| `selection-gain/fold-realized-outcome` | validates/deduplicates the R16 record, then calls `observe-outcome` | `selection_gain.clj:175-246`; static |
| `lane-futility/fold-gamma`, `gamma-trajectory` | offline reductions over supplied outcomes | `src/futon2/aif/lane_futility.clj:274-283`; not evidence of a running selector |
| `fold-realized/with-realized-outcome` | prospective producer of the R16 record; dynamic flags select whether/how it is attached | `src/futon2/aif/fold_realized.clj:188-207`; static only |
| trace record handling | carries a present `:realized-outcome` field, but comments alone do not establish a retained current record | `src/futon2/aif/trace.clj:576-584`; static only |
| flat selection path | the report runner says the former flat ranking and its selection-gain carry are deleted | `scripts/futon2/report/war_machine.clj:6593-6606`; static reachability evidence against treating it as a current consumer |

The producer module contains mutually historical staging text at its namespace
header and a current dynamic default of `*live-wire?* true`
(`fold_realized.clj:18-23,32-51`).  That tension is not resolved here by
asserting live operation.  Separately, the selection-gain reader itself says
the field is absent from records written by the tick and names its producer as
outside the `run-tick-once` require closure
(`selection_gain.clj:190-208`).  The frontier ruling therefore permits only
the static result: no retained live observation was read for this packet, and
neither absence nor comments are a pass/fail claim.

The documented selection wiring is `tau_eff =
adaptive-temperature(G-spread) / selection-gain`
(`selection_gain.clj:13-15,265-270`).  It is an engineering commitment
control, explicitly not Friston variational policy precision
(`selection_gain.clj:4-11`).  The existing annotation additionally says
that the campaign outer selector was deterministic and the inner reading
policy used operator-declared beta 1; it is unreviewed historical annotation,
not evidence upgraded by this reading
(`p4ng/plop-2026-code-annotations.json` at p4ng
`2e67e34d7bbc237518b7387ebf8b2c10fd293399:460-486`).

## Diagnostics and outstanding tripwire

The source signature of `policy-performance` accepts only two numbers, and
the update signature accepts only state/performance/options
(`selection_gain.clj:85-99,126-145`).  The local caller enumeration above
contains no regression, uncertainty, contradiction, or time-pressure
argument into either function.  Therefore the bad case is precise: if a
diagnostic changes while the selected policy's expected and realised scores
are unchanged, this update law has the same input and gives no basis for
claiming a gain change.

The paper annotation for the four-diagnostic sentence is `REFUTED`, and
says a factor-one-half tripwire coupling is ruled but **OUTSTANDING**, not
wired into a gain mode (`p4ng/plop-2026-code-annotations.json` at p4ng
`2e67e34d7bbc237518b7387ebf8b2c10fd293399:489-504`).  The paper's own
worknote likewise says its composer is not wired into an applicable gain mode
and that a variational mode ignores task gain
(`p4ng/sec-catalog.tex:375-382`, same p4ng commit).  This packet neither
implements nor validates that deferred coupling.

## Evidence classification

* **Static source reasoning:** the formula, burn-in, record vocabulary, and
  cited caller relationships above.
* **Historical annotation:** campaign-route/beta statements in the unreviewed
  JSON annotation; preserved as such.
* **Live evidence:** none inspected or produced.  No runtime, serving,
  trace/ledger, test, warrant, or probe action was performed.

## Exact proposed paper replacement (not applied)

Replace the R14 **Then** sentence and implementation-status sentence in
`p4ng/sec-catalog.tex:375` with:

> **Then** maintain an engineering selection-gain dial for an applicable
> stochastic selection law.  After burn-in, learn it only from the selected
> policy's signed realised-versus-expected performance:
> `(expected-score - realized-score) / (|expected-score| +
> |realized-score| + epsilon)`.  With lower score better, positive performance
> sharpens commitment and negative performance hedges it; the bounded gain
> divides the selection temperature.  Recent regressions, belief uncertainty,
> contradiction rate, and time pressure are diagnostics, not gain inputs unless
> an explicit consumer is added.  **Because** this keeps outcome learning
> distinct from diagnostic reporting and from variational policy precision.
> *Status in this implementation:* the historical flat selection-gain carry is
> deleted from the current cascade-only runner; no live consumption is claimed
> here.  The tripwire factor-one-half proposal remains ruled but unwired, and
> the documented deterministic outer minimum does not demonstrate gain
> consumption.

This wording deliberately does not call a diagnostic movement a gain update,
and does not turn the pending tripwire into a claim of implementation.

## Proposed correlation-table update (not applied)

Replace the R14 row in `holes/labs/wm-contract/logic-model/correlation.md:296`
with:

```
| R14 | EXISTS-UNVALIDATED (pure bounded outcome-performance update exists; no live consumption established) + REFUTED (the four diagnostics do not feed it; tripwire coupling is OUTSTANDING) | selection_gain.clj::policy-performance/update-selection-gain/fold-realized-outcome; fold_realized.clj::with-realized-outcome; war_machine.clj current cascade-only removal | required input is a new well-formed expected-score/realized-score outcome after burn-in; historical flat carry is deleted and no retained live consumer was inspected |
```

This is a proposed correlation correction only.  It changes neither the paper,
the shared table, nor implementation.
