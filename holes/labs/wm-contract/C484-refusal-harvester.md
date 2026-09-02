# C484 — the tail-eater v0: harvesting typed refusals into proposals and draft patterns

**Row:** worklist `:AC8`. **Date:** 2026-09-02. **Author:** claude (build loop, any-lane).
**Decides nothing.** Everything this apparatus writes is a proposal or a draft.

## What the row asked for

Joe's 2026-09-02 C130 ruling decided AC1–AC7 on one condition
(`holes/problems/DECISIONS-PENDING.md:343-364`): refusals and `:unscored`
records must be "fed back in as issues needing to be addressed, so the machine
can become self repairing". AC1–AC7 made the records exist and be typed. AC8 is
the mechanism that turns them into work, plus the ruling extension of the same
day: a *recurring* refusal class is additionally minted as a **draft design
pattern** in `library/problems/` — the tension recorded before its resolution
exists.

## What was built

| artefact | what it is |
|---|---|
| `holes/labs/wm-contract/harvest_refusals.bb` | the harvester |
| `holes/labs/wm-contract/PROPOSED-ROWS.md` | generated; one proposal per (site, reason) class |
| `holes/labs/wm-contract/refusal-sweep-state.edn` | generated; watermarks and cumulative per-class counts |
| `futon3/checks/library_graph_lint.clj` | the `@draft` quarantine, four laws |
| `futon3/flexiarg-directives.edn` | `:draft` registered `:standard` |
| `futon3/README-flexiarg.md` §5a | the quarantine table, written where `@why-posthoc`'s is |
| `futon3/test/library_graph_lint_test.clj` | `a-minted-draft-is-counted-apart-and-claims-no-authority` |
| `p4ng/build-p4ng.sh` | runs the harvester at publish, between `gen_aif_dag.bb` and the tetrahedron |

## The sweep

**Sources.** Two, and the report names both whether or not they yield anything.

1. The WM trace, `data/wm-trace/wm-trace-*.edn`, one EDN map per line.
2. The C130 absence-coercion lint,
   `checks/preemptive_absence_coercion_lint.clj`, whose first stdout line is a
   machine-readable map.

**Collection is by record SHAPE, not by trace key**, and that correction is the
main thing this row learned. Every record AC1–AC7 emits stamps itself with a
`:producer-contract` and a `:status`, so the sweep walks the whole trace record
and collects every map carrying both, at any depth. The first version walked a
fixed list of seven top-level trace keys, and it **missed AC5 and AC6
completely** while reporting a confident zero: their records do not travel under
`:move-score-events` in the trace at all. The cascade lane folds them into
`:policy-rollout-events` on an act-gate entry
(`scripts/futon2/report/cascade_lane.clj:384-386,399-409`), which reaches the
record through `:act-gate-verdicts` and `:enactment`
(`src/futon2/aif/close_loop.clj:131-132`,
`src/futon2/aif/enact.clj:216-217,317-318`, persisted at
`src/futon2/aif/trace.clj:588-594`). A shape walk follows the record wherever a
caller puts it; a key walk follows it only where somebody remembered to look.
The declared-producer table in the report is now a *coverage* report over the
eight contracts, not the collector's scope.

A member verdict inside `:offending` (`{:member :mean :status :missing}`)
carries a `:status` but no `:producer-contract`, and is correctly not collected:
it is part of a record, not one.

**Which records are work.** The OK set is closed and small —
`#{:present :scored :contributing}` — and *everything else* becomes a proposal.
That direction matters: a `:status` this script has never seen is flagged
`UNCLASSIFIED STATUS` in the proposal rather than dropped, because silently
dropping an unrecognised absence is the same defect class AC1–AC7 just removed.
The seven statuses currently emitted at the sites
(`free_energy.clj`, `belief.clj`, `policy.clj`, `rollout.clj`,
`adapters/fulab.clj`) are `:absent :refused :unknown :unscored :omitted
:rejected :uncosted`.

**Aggregation key.** `(:producer-contract record, :reason record)` — the
producer's own published identity, so the class name is not a string this
script invented. The eight contracts in the tree are `:prediction-error/v1`,
`:r3d-aggregate-driver/v1`, `:strategic-mode/v1`, `:default-mode-pressure/v1`,
`:rollout-move-score/v1`, `:rollout-move-cost/v1`, `:rollout-refusal/v1`,
`:fulab-temperature/v1`.

**"Since the last sweep".** Trace files are append-only per date, so the
watermark is a line count per file — exact, and cheap. A file that got *shorter*
was not written by an append: it is re-read from line 0 and the report says so
in bold. The lint has no clock, so its watermark is the previous sweep's
finding-key set.

**An unreadable line is a finding, not a skip.** A trace line that mentions a
declared event key and then will not parse is filed under
`:trace-reader/v1 / :unreadable-trace-line`. Dropping it would be the silent
absence again, one layer up.

## What it does NOT do

It does not write `worklist.edn`. One row, one writer: a proposal becomes a
ledger row when a person reads it and files it. It records no ruling. The draft
patterns it mints claim no authority and carry no `@why`.

## The `@draft` quarantine (four laws, all in `library_graph_lint.clj`)

| law | a draft |
|---|---|
| `:fraction-organised` | in **neither** numerator nor denominator. `:patterns-draft` and `:patterns-authored` are reported beside it |
| acyclicity of `@why` | edges out of a draft are not admitted to the graph |
| an authored `@why` **on** a draft | is a **failure** (`:draft-claims-authority`) — not a silent exclusion |
| `:argument-bodies-unchanged` | exempt (the minter rewrites the body each sweep); digests reported as `:draft-body-digests` |

Dangling targets, new-edge-attested and refused-edge-removed apply unchanged,
exactly as they do to `@why-posthoc`. Promotion is the editorial act that ends
the exemption: delete `@draft`, write the `@why`, and the file falls under the
body-digest law while the new edge needs its attestation — so nothing a script
wrote reaches the authored graph without a reader. That is "earns standing
through attestation", carried by the machinery that already exists rather than a
second one.

The third law is why the first two are rules and not merely permissions. Without
it, "a draft claims no authority" would be enforced by *exclusion* — a draft
could carry a `@why` and the lint would quietly not look at it.

## The two controls, run on EVERY invocation

`--no-controls` skips them; nothing in the tree passes it. Both run in a temp
directory before the real sweep touches anything, and a failure exits 2 and
writes nothing.

1. **planted-refusal-reaches-proposals.** A planted
   `:prediction-error/v1 / :malformed-prediction-triple` refusal record must
   appear in `PROPOSED-ROWS.md` as class
   `prediction-error-v1--malformed-prediction-triple`. This is the acceptance
   test AC8 names.
2. **planted-refusal-nested-two-levels-reaches-proposals.** An AC6-shaped
   `:rollout-move-cost/v1 / :cost-not-supplied` record planted inside
   `:act-gate-verdicts → :policy-rollout-events` must also reach the proposals.
   This is the regression guard for the key-walk mistake above. It is not
   vacuous: mutating the collector to stop recursing (deleting the
   `(run! walk (vals node))` line) makes this control fail and the script exit
   2, while control 1 still passes.
3. **clean-corpus-says-empty-loudly.** A corpus of `:present` records only must
   produce the `EMPTY SWEEP` section, not an absent one.

The last control is the one worth stating plainly: **a harvester that writes
nothing when it finds nothing cannot be told apart from one that did not run**,
and an unrunnable harvester is the standing red the ruling is against. So an
empty sweep rewrites the file in full with the sources it read, the line counts,
and the declared-key table, and prints a banner on stderr.

## The first real sweep, and what its zero means

Sweep 1 over 55 trace files (882 lines) and the C130 lint (0 findings) found
**zero** records. That zero is honest but it is not yet informative, for two
reasons the report states itself:

- The AC1–AC7 producers landed *today*
  (`afe5c49 … 497dca7`), after the last WM run wrote
  `data/wm-trace/wm-trace-2026-09-01.edn` at 22:54 on 2026-09-01. No tick has
  run since the typed records existed.
- **One of the eight declared producers has no path to the trace at all.**
  `:fulab-temperature/v1` (AC7) is emitted at
  `src/futon2/aif/adapters/fulab.clj:190,311,343,366` and a grep of `src/` and
  `scripts/` for `fulab/select-pattern` or the `adapters.fulab` namespace
  outside the file itself returns nothing — the adapter has no caller, which
  AC7's own row records as C226. Its refusals are invisible to this sweep **by
  construction, not by absence**, and the coverage table says so on every run
  rather than leaving a reader to infer it from a zero.

A census of the whole existing corpus confirms the zero rather than assuming it:
walking every `producer-contract`-bearing line of all 55 trace files yields
**0** maps carrying both a `:producer-contract` and a `:status`. The only
`:producer-contract` in the corpus is the top-level `:r8/stored-f-controller-v1`
stamp, which carries no `:status` and is correctly not a record.

## Gates

- `clj-kondo`: 0 errors, 0 warnings on `harvest_refusals.bb`,
  `futon3/checks/library_graph_lint.clj`, `futon3/test/library_graph_lint_test.clj`.
- `check-parens` (`futon4/dev/check-parens.el`): OK on all four changed
  Lisp/EDN files.
- `bb --classpath .:test test/library_graph_lint_test.clj` in futon3: 14 tests,
  167 assertions, 0 failures, 0 errors. The PRE-CHANGE test file (`git show HEAD:test/…`)
  run against the NEW lint is 13 tests / 151 assertions, 0 failures — so no
  existing assertion moved; the delta is one new `deftest` with six
  `testing` blocks, plus one assertion added to the directive-ontology test.
- `bash p4ng/empirics-futon/negative_controls.sh`: PASS (16 negative, 10
  positive).
- `bb p4ng/empirics-futon/pointer_check.bb`: 619 pointers, 0 unresolved.
- Live library lint unchanged on `problems`: 1254 files, `:pass? true`,
  `:fraction-organised 1.0`, `:patterns-draft 0`.
- Harvester run three times against a planted corpus: watermark advanced
  (3 new → 1 new → 1 new), cumulative count accrued (3 → 4), and the draft file
  count stayed at 1 per class across all three — a re-sweep rewrites the draft's
  evidence list and mints no sibling.

## What a reviewer should check

1. Read `harvest_refusals.bb` and confirm it writes no ledger and no ruling.
2. Run it with `WM_HARVEST_*` pointed at a temp directory and a planted refusal
   record; confirm the class reaches `PROPOSED-ROWS.md` and, past
   `WM_HARVEST_THRESHOLD`, a draft appears in the drafts directory.
3. Sweep twice and confirm exactly one draft file per class.
4. Confirm the four quarantine laws in `library_graph_lint.clj` and that
   `a-minted-draft-is-counted-apart-and-claims-no-authority` fails if any is
   removed.
5. Confirm the AC7 "no path to the trace" claim by grepping `src/` and
   `scripts/` for `fulab/select-pattern` and the `adapters.fulab` namespace, and
   confirm the AC5/AC6 path by reading
   `scripts/futon2/report/cascade_lane.clj:384-386` through
   `src/futon2/aif/enact.clj:216-217`.
6. Delete the `(run! walk (vals node))` line in `typed-records` and confirm the
   nested control fails — the shape walk is what the AC5/AC6 path depends on.
