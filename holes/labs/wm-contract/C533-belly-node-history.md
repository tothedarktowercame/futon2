# C533 — belly-node history: what C actually recorded

Historical reference only, commissioned by Joe on 2026-09-06. This note does
not recommend reusing this C definition and does not decide F10.

## 1. What it was

`c-vector` called the pre-existing channel ranges the static **floor**, then
added a goal-outcome half derived from the live goal/hole corpus at `:7071`
([`src/futon2/aif/c_vector.clj:1-8`](../../../src/futon2/aif/c_vector.clj)).
The namespace explicitly assigns the model—not merely the code—to
M-goals-and-holes, including “the C-entry shape, the 5 channel flavours, [and]
the A/B/C/D/E faithfulness diff” (`c_vector.clj:10-20`).

The record produced by `c-entry` was:

```clojure
{:flavour flavour :outcome-ref outcome-ref :preferred preferred
 :weight weight :status status :provenance provenance}
```

It refused construction without flavour, outcome reference, preference, and
provenance; status defaulted to `:open`, and an unoriented weight to 0.3 with
an explicit basis (`c_vector.clj:45-56`). The five implemented flavours were
`:stated`, `:mess`, `:incompleteness`, and the 應-voice split `:reach` and
`:correction`: the mission defines the latter split at
[`holes/M-goals-and-holes.md:104-110`](../../M-goals-and-holes.md), while the
namespace identifies stated plus the three overlay producers at
`c_vector.clj:14-20,213-246`. `:latent` belonged to the wider conceptual hole
family but had no feed (`M-goals-and-holes.md:29-35,68-85`), so it was not one
of the five materialised channel flavours.

The two layers were genuinely different objects. The floor was thirteen
declared healthy ranges such as `:loop-health [0.8 1.0]`,
`:mission-health [0.5 1.0]`, and `:sorry-count-norm [0.0 0.3]`
([`src/futon2/aif/preferences.clj:9-24`](../../../src/futon2/aif/preferences.clj));
`current-C` still describes those as the behavior-preserving static floor
(`preferences.clj:60-70`). The live layer derived cap and sorry outcomes,
folded the three overlay files, and retained last-good state on failed reads
(`c_vector.clj:248-274`).

The A/B/C/D/E comparison was the reason for the node: A is likelihood, B is
transition, C is prior preferences, D is the initial-state prior, and E the
policy prior (`M-goals-and-holes.md:23-27`). That audit found C only implicit
inside R5 risk and mislabeled as static hyperparameters; R1–R18 had no
first-class preference criterion (`M-goals-and-holes.md:23`). The excursion
records the implementation decisions as live derivation, cache/floor
degradation, and mission ownership
([`holes/E-C-vector-live.md:60-63`](../../E-C-vector-live.md),
`E-C-vector-live.md:85-103`).

## 2. What it recorded

I searched the canonical and archived stores with:

```sh
find data holes -type f -name '*wm-trace*.edn'
rg -l ':G-goal-outcome|:goal-outcome-replay-inputs|:belly' data holes
```

Across the canonical `data/wm-trace/wm-trace-*.edn`, goal-outcome fields occur
on 2026-07-02 through 07-06, 07-09, 07-14 through 07-19, 07-21, 08-30,
08-31, 09-01, 09-02, and 09-04. The first day has 34 tick records, 15 with a
goal-outcome value; from 2026-07-03 onward every canonical tick on the listed
days carries it. No canonical file after 2026-09-04 existed at census time.
No canonical tick has a top-level `:belly` key; that absence was checked with
`rg -n ':belly' data/wm-trace`.

One concrete steering record is
`data/wm-trace/wm-trace-2026-07-04.edn`, timestamp
`2026-07-04T07:03:48.095349930Z`: its first-ranked action is
`{:type :advance-mission, :target "M-first-flights"}`, with
`:G-risk 115.51069060941217`, `:G-goal-outcome 2.814240556270732`, and
`:goal-outcome-mode :kl`. The trace source explains the historical boundary:
the term had been computed but stripped until it was whitelisted on
2026-07-02 (`src/futon2/aif/trace.clj:93-105`); mode entered schema v1 on
07-04, replay inputs v15, and the preference stack v16
(`trace.clj:184-227`).

The three belly-bearing namespaces record different things:

* `trace.clj` records each ranked action's score, goal-outcome term, scoring
  mode, and—since v15—the exact C-entry/projection inputs needed for replay
  (`trace.clj:84-110,223-227`).
* `evidence_emit.clj` copies the winner's goal-outcome number into
  `:G-breakdown :goal-outcome`, and will copy a caller-supplied tick
  `:belly` count into the compact evidence record
  (`src/futon2/aif/evidence_emit.clj:73-81,127-154`).
* `fold_eval.clj` records no belly value. Its namespace prose merely lists
  `predictive-goal-outcome-risk` as a later alternative to the implemented
  coverage score (`src/futon2/aif/fold_eval.clj:1-17,20-34`).

## 3. What the paper showed

The PLoP-era prose said the cycle would “refresh the belly of ~455
preferences (R19) that candidates score against”
([`p4ng/main-2026.tex.prebak:376`](../../../../p4ng/main-2026.tex.prebak)).
Its figure caption described preferred-state backflow as “the belly (R19,
venter / 丹田)” and contrasted it with mind and hand (`main-2026.tex.prebak:489`).
The catalog caption was more qualified: R19 was the preference organ “built
and steering through a provisional in-memory join while its durable proof
join remains open” ([`p4ng/sec-catalog.tex:406`](../../../../p4ng/sec-catalog.tex)).

The generated [`p4ng/aif-wiring-brain.svg`](../../../../p4ng/aif-wiring-brain.svg)
places R19 outside/below the brain and draws its preference feed toward R5;
the SVG is minified onto line 2, whose embedded labels and edges are the
machine-readable pointer. The surrounding paper caption supplies the intended
brain/body reading (`main-2026.tex.prebak:489`).

## 4. What still runs today

There are two paths, and they do not behave alike. The older scheduled wrapper
calls `cv/maybe-refresh!` immediately before generating/scoring, then emits
the resulting entry count as `:belly`
([`scripts/wm_scheduled_run.clj:91-116`](../../../scripts/wm_scheduled_run.clj)).
By contrast, repository-wide search
`rg -n 'ensure-belly-fresh!|maybe-refresh!|current-c-vector' src scripts`
finds no caller of `ensure-belly-fresh!`; only its definition exists. EFE
defaults directly to `cv/current-c-vector` (`src/futon2/aif/efe.clj:725-749`).
The measured current CLI closure runs one JVM per tick, initializes the
`defonce` atom afresh, and has no refresh caller, leaving `:entries []` and
goal-outcome risk zero
([`holes/labs/wm-contract/C509-inter-tick-state-boundary.md:93-116`](C509-inter-tick-state-boundary.md)).
Review re-run (2026-09-06): the same search also shows `cv/maybe-refresh!`
called from `scripts/promote_c_entries.bb:94` and (as a `:refresh-fn` default)
`src/futon2/aif/full_loop_runner.clj:2459`; both belong to the older
in-process/ops paths, not the one-shot CLI closure, so the degradation
finding is unchanged.

The newest canonical evidence, `data/wm-trace/wm-trace-2026-09-04.edn` at
`2026-09-04T07:46:52.742121237Z`, names both `:live-goal-outcomes` and
`:c-vector-overlays` in its preference-stack provenance, yet the winning
action has `:G-goal-outcome 0.0` and its replay input contains
`:c-entries []`. Thus the declarations still travel in trace provenance, but
the present one-shot scoring path measured here had degraded to the static
channel floor. This is a historical/current-state observation, not an F10
design judgment.

## 5. The deferred half

The preserved seam says:

> “The predictive risk (a policy π's predicted outcomes vs C — the canonical
> KL term) is the W1-gated follow-on: it needs the forward model to predict
> goal-progress under π, i.e. the goals↔methods PROOF join ... The seam is
> `goal-outcome-risk` ... Do not remove it (Joe, 2026-06-26).”

That text remains at `src/futon2/aif/c_vector.clj:23-27`; the excursion records
the same decision as a “named, un-removable seam”
(`holes/E-C-vector-live.md:85-89`). What it awaited was a policy-conditioned
forward prediction of goal progress plus the durable goals-to-methods proof
join, rather than distance from the current corpus alone.

As a later fact, a typed runtime `Q(o|pi)` now exists:
`predictive-outcome-distribution` composes belief, controlled transition, and
observation kernels and validates the returned row
([`src/futon2/aif/machine_q.clj:1-24`](../../../src/futon2/aif/machine_q.clj),
`machine_q.clj:364-380`). The F1 receipt calls this the machine-grain
`Q(o|pi)` instance (`holes/labs/wm-contract/runs/F1-machine-q/03-machine-grain-q.edn:186`).
That fact does not establish that the old belly definition should consume it.

## Scope limits

This census searched the canonical trace directory and filenames containing
`wm-trace` below `data/` and `holes/`; it did not inspect untracked external
stores, journal history, or substrate-2's live database. Dates describe files
present on 2026-09-06, not a proof that no tick ran elsewhere. The paper
survey was limited to `sec-catalog.tex`, `main-2026.tex.prebak`, and
`aif-wiring-brain.svg`. No live JVM was reloaded or queried.
