# C480 — AC4: the fallback selector stops substituting zero for sorry pressure

Worklist row `:AC4` (class I). C130 §3 migration at the site the census records
as `src/futon2/aif/policy.clj:144-145`, tally instance `:policy-sorry-count`.
Decided by Joe's 2026-09-02 ruling (`DECISIONS-PENDING.md`, futon2 `2f34c26`):
typed absence, loud malformed, no fabricated values, and every abstention
persisted as a typed present-only record.

## What the site did before

`default-mode-select` — the I6 compositional-closure fallback, the selector the
war machine runs when `select-action` throws — read its one observation input
through a `0.0` default:

```clojure
sorry-pressure (double (get observation :sorry-count-norm 0.0))
```

and then branched on it. A tick that never measured sorry pressure was selected
as though it had measured **no** pressure: rule 1's test `> 0.3` failed on a
number nobody observed, and `:address-sorry` work was skipped for that reason.

**Measured, both directions, on `(obs/observe {})`** with a `:no-op` and one
`:address-sorry` candidate: the pre-AC4 expression reads `0.0`, fails rule 1,
and falls through to rule 3, choosing `:address-sorry` — a choice made under a
reading that does not exist. The new selector returns
`{:action :abstain :reason :sorry-pressure-unknown}` with a `:unknown` record.

## What it does now

The channel is read through the observation envelope by
`sorry-pressure-record` (`src/futon2/aif/policy.clj:248-286`), using the same
reader the diagnostics use — `free-energy/channel-source-status`
(`src/futon2/aif/free_energy.clj:33-54`), made public for this row so the
selector and the diagnostics cannot drift apart about what counts as observed,
including the legacy plain-map allowance a second implementation would be free
to lose. Three typed records:

- **`:present`** — the channel was observed and carries a finite number.
  Carries `:value`, the double the rules compare to `0.3`. **A measured `0.0`
  lands here**: zero pressure is a reading, not an absence.
- **`:unknown`** — the channel was **not observed**. No `:value` key is
  written, and `:absent` carries the envelope's own `:reason` and `:paths`.
- **`:refused`** — the channel is present but is **not a finite number** (a
  string, a keyword, `true`, a vector, NaN, an infinity). `:offending` names
  the value it was given.

Absence and malformation are separated exactly as they are in
`compute-prediction-error` (AC1) and `infer-mode-record` (AC3): a channel
nobody measured is a gap in the scan; a channel that arrived as a string is a
producer defect and has to stay loud. `nil` is the boundary between them —
`{:sorry-count-norm nil}` is `:unknown`, `{:sorry-count-norm "x"}` is
`:refused`.

When the record is not `:present`, the selector **abstains and returns
control** (`src/futon2/aif/policy.clj:302-404`):

```clojure
{:action :abstain
 :reason :sorry-pressure-unknown | :sorry-pressure-malformed
 :gap-report <the :learn-action-class recommendations, as elsewhere>
 :sorry-pressure <the typed record>
 :source :default-mode}
```

The `:gap-report` is what makes it loud rather than merely silent: the same
capability-gap enumeration `select-action`'s own abstain branch surfaces to the
operator.

## When the pressure is required — the one place this row is narrower than AC3

**Exactly when at least one `:address-sorry` candidate is admitted.** Rules 1
and 3 are the only two that rest on the reading and both need an
`:address-sorry` candidate to fire; with an empty `addr-sorrys` the outcome is
decided by rules 2 and 4 alone and the value is not read at all. Choosing there
is not choosing under a substituted value, so it is not blocked.

With an `:address-sorry` candidate present, the two live rules make **opposite
claims about the same unobserved number** — rule 1 asserts `> 0.3`, rule 3
asserts `<= 0.3` — so neither may be taken, and the selector abstains.

This is narrower than AC3's all-six requirement because the branch structure is
narrower, not because the standard is. AC3's classifier ends in `:else
:multiplied`, so every one of its six features is read on every path; this
selector's pressure is read on two of four paths, and which paths are live is
decided by the candidate set the caller supplies. What neither producer may do
is *assert* a value nobody measured.

The record is attached to every decision, not only the abstentions, with
`:required?` and `:abstained?` added at the decision, so a reader can tell an
inconsequential unknown from the one that stopped a selection.

## Behaviour change, and its measured blast radius

`default-mode-select` has exactly one production caller: the `catch` arm at
`scripts/futon2/report/war_machine.clj:5178-5190`. It runs only when
`policy/select-action` throws. So the abstention is reachable on a tick where
three things hold at once: `select-action` threw, an `:address-sorry` candidate
was admissible, and `:sorry-count-norm` was unsourced.

**Measured on real ticks** — the twenty S1b records
(`holes/labs/wm-contract/runs/2026-09-01-s1b/wm-trace-s1b.edn`):

- `:decision :source` is absent on **20/20**, so `select-action` succeeded on
  every tick and the fallback never ran. On that corpus this row writes no
  `:sorry-pressure` key and no `:default-mode-events`, and is byte-identical.
- `:sorry-count-norm` is nonetheless `:variant :absent` on **20/20** in the
  persisted envelope, with `:reason :source-field-missing`, `:paths [[:graph
  :summary :total-sorrys]]` and `:coerced-to 0.0`. Had the fallback run on any
  of those ticks, the pre-AC4 code would have selected under a fabricated
  `0.0` — on every one of them. This is not a hypothetical channel.
- `:admissible-actions` carries **zero** `:address-sorry` candidates on
  **20/20**. So even had the fallback run, `:required?` would have been false
  and the new selector would have chosen the same action the old one did. The
  conditional requirement is not doing quiet work in its own favour here: on
  this corpus it changes no outcome, only what is reported.

A caveat about replay, worth stating because it looks like a contradiction:
replaying `sorry-pressure-record` against a persisted record's numeric
`:observation` map returns `:present 0.0` on 20/20, because that map is a plain
EDN map whose channel metadata did not survive serialization — it is precisely
the legacy boundary `channel-source-status` supports. The absence lives in the
record's `:observation-envelope`, which is where the 20/20 figure above is
read. The live path holds the metadata-bearing observation and reads `:absent`.

## Self-repair condition

`default-mode-events` (`src/futon2/aif/policy.clj:288-300`) is the present-only
projection: a vector of at most one, empty when the fallback read a real
pressure and empty when the fallback never ran (a `select-action` decision
carries no `:sorry-pressure` key at all). The war machine projects it at
`scripts/futon2/report/war_machine.clj:5191-5197` and puts it on the judgement
at `:5367`; it persists at `src/futon2/aif/trace.clj:555-564`, beside AC1's
`:prediction-triple-events`, AC2's `:belief-aggregation-events` and AC3's
`:strategic-mode-events`.

No key means one of two things here — `select-action` succeeded, or the
fallback ran and read a real pressure — and the decision's own `:source` tells
them apart. Both are different claims from "the selector did not report". AC8's
harvester is what turns these into work items; this row only guarantees they
exist and are typed.

## Gates

- **clj-kondo** on the five changed files (`src/futon2/aif/policy.clj`,
  `src/futon2/aif/free_energy.clj`, `src/futon2/aif/trace.clj`,
  `scripts/futon2/report/war_machine.clj`,
  `test/futon2/aif/policy_test.clj`): **0 errors, 0 warnings**.
- **check-parens** (`futon4/dev/check-parens.el`, `arxana-check-parens-cli`):
  **OK** on all five.
- **Tests**: `clojure -M:test -m cognitect.test-runner -d test/futon2` —
  **998 tests, 6100 assertions, 0 failures, 0 errors** (was 987/5996 at AC3:
  +11 deftests, +104 assertions). `futon2.report.war-machine-test` requires
  `futon2.report.war-machine`, so the caller edit is compiled by the suite.

## Planted cases (`test/futon2/aif/policy_test.clj:344-518`)

**Absent.** `(obs/observe {})` with `:address-sorry` candidates → abstain
`:sorry-pressure-unknown`, asserted *specifically* not to select
`:address-sorry` (the branch the substituted zero used to reach), with no
`:value` key, `:required? true`, `:abstained? true`, and `:absent` carrying the
envelope's own `:source-field-missing` and a non-empty `:paths`. A legacy plain
map with no metadata → the same abstention with `:status-metadata-missing`.

**Malformed.** `"x"`, `:keyword`, `true`, `[]`, NaN, `+Infinity` and
`-Infinity`, each refused with `:offending` naming the channel and the value it
was given, and no `:value` key.

**The separation itself.** `nil` and `"x"` in the same channel produce
different statuses and different reasons.

**The control the row turns on.** A **measured** `0.0` (a scan carrying
`:total-sorrys 0`) selects `:address-sorry` with `:value 0.0` in the record,
while the same channel unsourced does not select at all. Same numeric zero,
different provenance, different behaviour.

**The requirement condition.** With no `:address-sorry` candidate and an
unobserved channel, both the learn branch and the no-op branch still select,
with `:required? false` and `:abstained? false` on a record that still reports
the `:unknown`. And an empty candidate set still abstains as `:no-candidates` —
no candidates is a different fault from no reading, and the two reasons stay
distinct.

**Loudness.** The abstention's `:gap-report` carries the
`:learn-action-class` recommendations, and rule 2 is asserted *not* to be
reached by treating rule 1 as false.

**The projection.** `default-mode-events` returns `[record]` on an abstention,
`[]` on a clean read, `[]` on a `select-action` decision, and `[]` on `nil`.

**Read alone.** `sorry-pressure-record` is exercised directly, without the
selector, for all three statuses.

The five pre-existing `default-mode-select` tests are unchanged and still pass:
none of them presents an `:address-sorry` candidate together with an
unmeasured channel, which is the only combination whose behaviour moved.

## Lint and tally

`checks/absence-coercion-dispositions.edn:47-50` flips `:blocked` →
`:fix-now` with the control (`:summary` `:fix-now` 12 → 13, `:blocked` 4 → 3).
`bb checks/preemptive_absence_coercion_lint.clj`: findings **4 → 3**, this
site's finding gone, the three remaining being AC5–AC7's sites.

p4ng `empirics-futon/defect-repair-tally.edn` row `:policy-sorry-count`
`:open` → `:repaired`. Totals over the fixed 61-instance population: 53
repaired / 7 open / 1 partial → **54 / 6 / 1**.

## Pre-existing red, not mine

`bb p4ng/empirics-futon/pointer_check.bb` reports the same two unresolved
pointers AC1, AC2 and AC3 all recorded: one pointer, quoted twice, in AC7's
worklist row, naming a file under `src/futon2/aif/adapters/`, which the checker
cannot resolve because that directory is not on its root allowlist. 570
pointers in 3 files, 2 unresolved, both the same one.
`negative_controls.sh` fails on that pointer and on nothing this row added.
(Named here without the `file:line` form on purpose: `pointer_check` reads
`worklist.edn` as one of its three registries, so writing the pointer verbatim
in this row's ledger evidence would mint a third copy of the same red.)

## Not done here

- The census `:at` key stays `policy.clj:144-145`. It is the join key the lint
  and the C12 census share; AC1–AC3 set the precedent of naming the new lines
  in the `:control` prose rather than moving the key. The pointer is now three
  code blocks away from the site it names, which is the cost of that precedent
  and is worth raising once for all four rows rather than diverging here.
- `trace-schema-version` is **not** bumped, matching AC1–AC3, which added
  `:prediction-triple-events`, `:belief-aggregation-events` and
  `:strategic-mode-events` without a bump. The version ledger's own rule ("bump
  on any change to the record's key set") arguably owes one bump covering all
  four present-only keys. Flagged for the reviewer rather than settled by a
  lone bump on the fourth row.
- Whether anything may **act** on an abstention is untouched. This selector's
  abstention returns control to its caller, which is the `catch` arm; what the
  war machine does with an abstaining fallback decision is the same thing it
  did with an abstaining `select-action` decision, and the hard-guard question
  (`DECISIONS-PENDING.md` §3) is not reopened here.
- `select-action` itself is not touched. It reads no observation channel; the
  substituted zero this row removes existed only in the fallback.
- The figure is not regenerated (publish-time, TN §9a gate rule).
