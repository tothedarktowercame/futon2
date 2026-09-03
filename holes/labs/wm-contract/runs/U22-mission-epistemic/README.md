# U22 step-through — the epistemic term of mission value, before and after

claude (wm-build-loop), 2026-09-03. Row U22 (ported from zaif-harness S5).
Every number below comes from `war-machine/enrich-candidates-with-mission-value`
— the function the live judge calls — run twice on the same candidate set,
differing only in the declared weights map. Nothing is recomputed by hand;
the script sorts what the selector returned.

Producer: `holes/labs/wm-contract/u22_mission_epistemic.clj`, run from the
futon2 root as `clojure -M holes/labs/wm-contract/u22_mission_epistemic.clj`.
Read-only: no tick, no trace append, no run lock, no substrate write. The only
substrate traffic is the read of the `code/v05/mission-doc` family the judge
already performs.

## What the term is

    epistemic(M) = survey-availability(phase(M))
                   * min(1, EIG(M) / (10 * ln 2))

`EIG(M)` is the sum over M's field questions of the policy-conditioned expected
information gain of resolving that question, computed by
`futon2.aif.epistemic-value/expected-information-gain` — the canonical kernel
with its Bayes-coherence gate, not a spread or a gap lookup (that namespace's
docstring refuses those). Implementation:
`futon2/src/futon2/aif/mission_epistemic_value.clj`.

MEASURED: for each id in M's `:mission/cross-refs` **that names a mission the
index knows**, whether that neighbour's `:mission/phase` is readable — an
unreadable phase leaves the latent "is this neighbour workable (at or past
DERIVE)?" at 0.5 and worth ln 2 nats, a readable one settles it and is worth 0
— plus whether M's own `:mission/mtime` is inside the declared 14-day window.

DECLARED: the workable latent, the perfect-observation model,
`phase-survey-availability` (one fiat number per phase, MAP = 1.0), the 14-day
window, and the reference of 10 open questions. The availability table is
hand-authored exactly like `phase-doability` — under Joe's 2026-09-02 ruling
that is what it is allowed to be, because it multiplies measured nats instead
of standing in for them.

## The two weight declarations

| | central | strategic | doable | epistemic |
|---|---|---|---|---|
| before | 0.20 | 0.50 | 0.30 | — (defaults to 0.0) |
| after | 0.17 | 0.425 | 0.255 | 0.15 |

`before` is Joe's option-B declaration of 2026-09-02
(`zaif-harness runs/S3-declared-weights.edn`), the weights the 09-02 live trace
records carry. `after` is declared BY THIS STEP-THROUGH and by nobody else: the
three exploit weights keep option-B's ratio exactly and are shrunk by
`1 - 0.15`. That construction is what makes this a clean before/after —

    value_after = 0.85 * value_before
                  + 0.15 * epistemic * completion-gate * operator-gate * decay

holds on **133 of 133** rows (`05-rank-moves.edn :value-identity`, 4 of them
trivially because the gates zero them), and a common positive factor cannot
reorder the exploit part. So every rank move below is the epistemic term and
not a re-weighting of my choosing.

## Arm A/B — the selector as this process finds it

133 candidates, ids pinned from the committed S2 baseline
(`zaif-harness runs/S2-step-through-1/02-weights.edn`, sha256 in
`01-candidates.edn`). Rank 1 does not change: **M-zaif-harness-v1**, 0.5900 →
0.5015, so the term does not disturb the selection Joe's option-B weights
produced. 115 of 133 rows move.

The furthest movers, and every one of them is a survey-phase mission:

| mission | phase | rank before | rank after | open / questions |
|---|---|---|---|---|
| M-web-arxana-missions | map | 124 | **74** | 2 / 12 |
| M-populate-substrate-2 | map | 113 | 82 | 1 / 8 |
| M-pheromone-field | map | 112 | 81 | 1 / 3 |
| M-zai-learning-loop | head | 129 | 98 | 1 / 4 |
| M-metric-harness | map | 108 | 80 | 1 / 1 |
| M-usage-hacking | identify | 122 | 94 | 1 / 1 |

**The stated epistemic reason for the headline move.** M-web-arxana-missions is
in MAP and declares twelve field questions; the judge cannot read a phase for
two of its neighbours and its own doc reading is stale, so two questions are
open and resolving them is worth 1.3863 nats — 0.200 of the declared
reference, times MAP's availability 1.0. That, at weight 0.15, is 0.030, and
it is the whole of its rise from rank 124 to rank 74 in a band where the
exploit values are nearly tied. Its centrality (0.0) and strategic role (0.0)
did not change.

The other direction is the same mechanism, and it is the property Joe's ruling
asked for. M-apm-capability-ratchet is also in MAP, but the judge can read all
eight of its neighbours' phases, so its only open question is its own
staleness: 0.100, and it moves 79 → 75, four places rather than fifty. A MAP
phase buys nothing here on its own.

M-dionysus-winddown does not move at all: it carries an operator gate, the
gate product is 0.0, and the epistemic term is multiplied by it like every
other factor. Measured 0.100 and enacted 0.0, recorded separately.

## Arm C — the same two arms with a live doability factor

ONE DECLARED SUBSTITUTION, and it is why this arm exists.
`compute-delta-t-mission` resolves phase through
`futon3c.aif.mission-delta-t`, which is not on futon2's classpath, so in arms
A/B the doability factor gets `:phase nil` → 0.3 for every candidate. That is
not an artefact of this script: the live records show the same thing
(`:doable 0.3` on 261 of the 269 ranked mission rows in
`data/wm-trace/wm-trace-2026-09-02.edn`, the other 24 operator-gated at 0.0;
`:phase` is nil on every ranked row in every trace file since 2026-07-19). With
doability inert, "the epistemic term beats doability" would be untested. So
arm C redefines `compute-delta-t-mission` to return the `:mission/phase`
already on the mission-doc hyperedge — the same carrier the epistemic term
reads, and the same string futon3c reads off the endpoint vertex
(`mission_delta_t.clj:237-240`). Nothing else is redefined.

Doability then discriminates: `{1.0 25, 0.8 7, 0.6 2, 0.5 14, 0.4 5, 0.3 25,
0.2 35, 0.1 8, 0.0 12}` over the 133 rows. MAP is 0.3, INSTANTIATE is 1.0, so
this arm is the epistemic term pushing against a factor that prices exactly the
opposite thing.

It still moves the survey missions, less far — 62 of 133 rows move:

| mission | rank before | rank after | doable | open / questions |
|---|---|---|---|---|
| M-web-arxana-missions | 102 | **87** | 0.30 | 2 / 12 |
| M-differentiable-code | 50 | **40** | 0.30 | 2 / 8 |
| M-eoi-outbox-management | 38 | **28** | 0.30 | 1 / 3 |
| M-paper-reverse-morphogenesis | 37 | 30 | 0.50 | 2 / 5 |
| M-tpg-coupling-evolution | 51 | 47 | 0.30 | 1 / 2 |

Rank 1 is again unchanged (M-zaif-harness-v1, 0.8000 → 0.6800). **This is the
trade against doability, measured:** at epistemic weight 0.15 a MAP-phase
mission with two open field questions passes 15 competitors that are closer to
delivering, and does not reach the top. Whether that is the right exchange rate
is a number for Joe, not for this row; what the row establishes is that the
exchange rate exists and is set by one declared weight.

## What the term declines to score, and why the moves are small

Two measurements limit how much this term can currently discriminate, and both
are field facts rather than choices in the code.

**A cross-reference that resolves to nothing is not a question.** 121 of the
candidates' cross-references, over 48 of the 133, name no mission the index
knows: `M-1` through `M-7` on M-pattern-mining, `M-foo`/`M-bar`/`M-baz` on
M-portfolio-inference, `M-INC`, `M-WS`, `M-EOI` (266 over 116 missions across
the whole 374-hyperedge family, 39 of them numeric). Some are a regex scrape
catching list markers; some may be real references to missions with no doc. The
judge cannot tell those apart, so they are excluded and counted as
`:unresolvable-cross-references` rather than paid ln 2 nats each. **This was
found by the term producing a wrong answer first:** before the exclusion,
M-pattern-mining ranked 4th on nine open questions, eight of which were
`M-1`..`M-7` and `M-trip-report`. A malformed cross-reference list was buying
survey priority.

**Today's readable uncertainty is almost all staleness, not an unread
neighbourhood.** Over the 123 candidates the term could measure, the
open-question count is `{0: 6, 1: 108, 2: 8, 3: 1}` — 108 of them have exactly
one open question, and it is their own reading freshness. Only 9 have an
unread neighbour at all, because 313 of the field's 374 mission docs carry a
readable phase. So the term is implemented and behaves as designed, and on this
field it is close to a staleness ordering with a phase mask. Getting more out
of it needs more question kinds, and the mission docs already carry the obvious
one: the numbered `MAP must answer:` lists (e.g.
`futon0/holes/missions/M-apm-capability-ratchet.md:260-274`, six questions).
Reading those is a follow-on row, not this one.

## What the term does not use, recorded anyway

`06-field-census.edn`. Of the 374 `code/v05/mission-doc` hyperedges, all 374
carry a mission id and 313 carry a readable phase. That census is the same for
every candidate on a tick, so it cannot move a rank; it is the denominator the
per-mission numbers sit in, and it is recorded rather than folded into a score.

Per-candidate coverage: 123 of 133 `:measured`, 8 `:phase-unreadable` (the
mission-doc phase is absent or the literal string "unknown"), 2
`:mission-absent-from-mission-doc-index`. Each is a typed status carrying 0.0,
not a flat 0.0 that would read as "nothing to learn here". The clamp fired on
none of the 123 (max 3 open questions, 2.0794 nats against a reference of
6.9315), so on this field the factor is exactly the mission's EIG rescaled by a
constant.

`:s2-drift`: 1 of 133 rows differs from the S2 baseline's own central/strategic
numbers — M-zaif-harness-v1's strategic 0.0 → 1.0, which is S3 act 3 (the
spine box added in futon7 c5b7288). Expected, and it is why `before`'s rank 1
is M-zaif-harness-v1 here where S2's was M-expressions-of-interest.

## Replay

From the futon2 root, with the substrate up on :7073:

    clojure -M holes/labs/wm-contract/u22_mission_epistemic.clj /tmp/u22-replay

`:epistemic-as-of` is pinned to 2026-09-03 in the script, so the freshness
questions do not drift with the clock, and the candidate id set is read from
the committed S2 file with its sha256 recorded in `01-candidates.edn`. The
mission-doc phases and cross-refs are read live, so a replay after the field
changes will differ — by design, and `06-field-census.edn` is where that shows.
