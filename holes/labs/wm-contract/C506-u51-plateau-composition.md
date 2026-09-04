# C506 — U51: what the 55-way plateau is made of

Row: `worklist.edn :U51` (class D, discovery; epic `EPIC-run-era.md`, the ruling
of 2026-09-04 "yes to fixing the ladder to remove the plateau… A 55-way tie
should be seen as an obvious defect"). Split from U52 by handoff discipline:
this row measures, U52 implements.

Producer: `u51_plateau_composition.bb` (read-only; no tick, no run lock, no
substrate call, no network, nothing under `data/` written). Artifact:
`runs/U51-plateau-composition/plateau-composition.edn`, 12 controls, all pass.
Two consecutive runs are byte-identical.

**No scoring or selection code changed.** No `:choices` and no `:decisions`
entry was written; the kin question below is left open on purpose.

## The rungs, as the ruling states them

`aif-equations.edn :choices :task-belief-actand-source`, ruled by Joe
2026-09-03: rung 1 direct case history; rung 2 constructive generalization from
kin, marked `:constructed` with its derivation; rung 3 a typed refusal that
mints a tension record.

Operationalised here at mission grain:

- **rung 1** — the candidate's own action key `[type target]` is the chosen
  action of at least one persisted decision in the recorded corpus.
- **rung 2** — no case history of its own, but a kin candidate has some.
- **rung 3** — neither.

## The corpus, and the one thing it does not contain

57 files, 889 records under `data/wm-trace` (untracked; the corpus leg
reproduces only on a machine holding it, exactly as U39 and U40 read it). Over
those 889 decisions the machine has ever chosen **17 distinct action keys**, and
**0** of them carry an outcome — no record reaches any of the three places
`trace-outcome` looks (`war_machine.clj:2391-2394`). That re-measures U39's
era-disjointness from the candidate side: rung 1 here means *the machine chose
this before*, never *and it worked*, because nothing in the corpus says whether
anything worked.

(Drift found in passing, recorded not repaired: U39 and U40 both cite
`trace-outcome` as `war_machine.clj:2328-2332`; that range now falls inside
`compute-delta-t-mission`'s body. The function is at `:2391-2394`.)

## The s5 plateau: 55 candidates, 2 with case history

Subject: the committed run-store trace `runs/2026-09-01-s5/wm-trace-s5.edn`,
tick `4e35e740`, field of 145. Widest plateau **55 wide at ranks 73–127**, all
`:advance-mission`, sharing controller-score 5.055525524889362; the chosen
action sits inside it at rank 123.

| rung | count | who |
|---|---|---|
| 1 — direct case history | **2** | `M-wm-aif-policy-grain-compliance` (44 decisions), `M-shared-memory-control-build-test` (4) |
| 2 — kin with case history (union of all four measured relations) | **53** | see the relation table |
| 3 — nothing | **0** under the union; **39** under the only relation that discriminates |

Both rung-1 members already had their history *before* this tick, and both keep
it with the tick's own day excluded.

**The mission with the most case history in the plateau is scored identically to
54 missions with none.** `M-wm-aif-policy-grain-compliance` has been chosen 44
times and sits at the same number as missions the machine has never chosen.

## Why they tie — an exact predicate, not a description

Control `positive-3`: the plateau is *exactly* the extension of a predicate over
recorded fields —

> an `:advance-mission` candidate with centrality 0.0, cascade role 0.0, an
> unresolvable phase, no operator gate, no completion gate and no non-progress
> decay

— 55 predicted, 55 actual, sets equal. Their shared `:mission-value-factor` 0.09
recomputes as 0.30 × 0.3: the `:doable` weight
(`war_machine.clj:2358-2361`) times the `"unknown"` phase doability
(`war_machine.clj:2363-2373`, applied at `:2507-2509`), with the other three
terms of the blend contributing zero (`war_machine.clj:2569-2571`) and decay 1.0
(`war_machine.clj:2630`). The `:epistemic` weight is 0.0 by default, so the
fourth term is off.

So the plateau is not a coincidence of arithmetic. It is the class of missions
about which every scored channel is silent, receiving the default for the one
channel that has a default. Four candidates satisfy the first three clauses and
are *not* in the plateau, which is the predicate earning its keep: the previous
tick's selection (decay 0.5, factor 0.045, rank 128) and three operator-gated
missions (factor 0.0, ranks 129/131/132).

Inside the plateau, exactly three recorded things vary: the identity fields
(`:target`, `:mission-path`, `:rationale`), `:rank` — which is the tiebreak
order, not a measurement — and **`:open-hole-count`, which takes 12 distinct
values from 0 to 24 and enters no score**. Every scored term is constant.

## How much each rung can actually drain

The ruling names kin as "kin actand-classes in the same table, shared patterns,
cascade-catalog playout". At `:arm-session` grain the v1 relation is written
down (same arm + same correction-label, zaif `:U11g`). **Nothing equivalent is
declared at mission grain**, so this row measures four candidate relations
computable from recorded fields alone and rules on none. Each row's rung-2
assignment names the relations that grounded it.

| relation | rung-2 members | distinct kin signatures over the 55 | largest group sharing one | members with no kin history |
|---|---|---|---|---|
| `:k-type` same action type | 53 | 3 | 53 | 0 |
| `:k-repo` same home repository | 24 | 10 | 18 | 30 |
| `:k-produces` shared capability-graph `:produces` | 0 | 1 | 55 | 55 |
| `:k-doc-xref` documents name each other or a mission in common | 14 | 54 | 2 | 39 |

**This table is the answer to "how much of the 55 can rung 2 drain", and the
two numbers that matter are in different columns.** `:k-type` reaches 53 of 55
and refines them into 3 signatures with 53 sharing one: every member would be
pooled with the same two case-history missions and handed the same constructed
value — the plateau again, one derivation deeper. `:k-doc-xref` refines almost
completely (54 signatures, largest group 2) but reaches only 14, leaving 39 at
rung 3. `:k-produces` is empty because the capability-graph input recorded on
every one of the 55 carries zero `:produces` entries — arm C's material is not
there.

A rung-2 rule drains the plateau only if its kin relation partitions the plateau
more finely than the score does. Of the four measured, one reaches nearly
everything and separates nothing; one separates nearly everything and reaches a
quarter.

The 14 `:k-doc-xref` members and the mission each is kin to are listed per row in
the artifact — e.g. `M-aif-a-matrix-faithfulness` → `M-aif-policy-conditioned-eig`,
`M-wm-aif-policy-grain-compliance`; `M-typed-memories` →
`M-shared-memory-control-build-test`, `M-wm-aif-policy-grain-compliance`.

## The re5 field: the plateau did not go away, it grew by one

`runs/2026-09-04-re5/wm-trace-re5.edn`, first tick `8ae111bc`, field of 146:
widest plateau **56 wide at ranks 75–130** at score 3.5175439026866684, all
`:advance-mission`. Rungs 3 / 53 / 0, relation counts 53 / 24 / 0 / 15 —
the same shape as s5.

RE5's rationale record reports `:controller-score-tie {:count 1 :rank-band [1 1]}`
and RE7 expects a `:green` verdict for this run. Both are about the **choice**:
the chosen action sits at rank 1, outside the plateau. **The field still carries
a 56-way tie.** A check that verdicts only on the chosen candidate's tie width
will read this run as clean, which is why RE7's row already asks for the
field-plateau width census beside the verdict; this measurement says the census
is not decoration.

## Is the plateau a property of one tick or of the run?

Per-tick, from the four s5 tick ids named by the committed RE4 rationale store,
and from the four committed re5 traces:

- s5 widths `[55 55 55 55]`, membership union 56 / intersection 54. The two
  members that move are `M-wm-aif-policy-grain-compliance` and
  `M-aif-policy-conditioned-eig` — the alternating pair, each leaving the
  plateau on the tick after it is chosen (non-progress decay) and returning on
  the next.
- re5 widths `[56 56 56 56]`, union 56 / intersection 56 — identical membership
  on every tick.
- Every s5 tick chose a candidate inside its own plateau; no re5 tick did.
  Every tick of both runs chose a candidate that has case history.

## Scope, stated

- Kin is searched among the tick's **own** recorded candidates, because those
  are the entries whose kin-bearing fields the trace record carries. 11 keys
  with case history are absent from the s5 field (10 from re5's), listed in the
  artifact; extending the kin search to them can only move candidates from rung
  3 to rung 2, never the other way.
- The union row is an **upper bound**, not a recommendation. Choosing the kin
  relation is U52's design question or a `:choices` entry, and this row does not
  take it.
- Whole-field rung counts are reported beside the plateau ones (s5: 11 / 127 /
  7; re5: 12 / 127 / 7). The 7 at rung 3 in each field are the `:address-sorry`,
  `:no-op` and `:fire-pattern` candidates — non-mission action types with no
  case history and no kin under any measured relation.

## Controls

12, all pass, 6 per field.

- `negative-1` — a planted candidate of a type no chosen key in the field
  carries, in a repository no candidate lives in, with no document and no
  `:produces`, classifies rung 3.
- `positive-4` — the *same* planted candidate retyped `:advance-mission`
  classifies rung 2 under `:k-type` alone. The pair is what makes the rung-3
  answers mean something: the classifier is not answering "nothing" because the
  candidate is unknown to it. (Written after the first version of `negative-1`
  passed for the wrong reason — the planted row fell out of a lookup table
  rather than being classified; kin tokens are now computed for any candidate by
  the same code path.)
- `negative-2` — no candidate anywhere in either field carries outcome-bearing
  case history (0, as U39 predicts).
- `positive-1` — a candidate the corpus records as chosen classifies rung 1,
  citing its own decision count.
- `positive-2` — the plateau's controller-score is single-valued, so the
  partition cannot be an artefact of score arithmetic.
- `positive-3` — the predicate/plateau set equality and the 0.30 × 0.3
  recomputation above.

## What U52's acceptance can be measured against

- s5: 55 plateau members, 2 at rung 1, and a rung-2 reach between 14 and 53
  depending on the kin relation declared — with 39 at rung 3 under the only
  measured relation that separates plateau members from each other.
- re5: 56 plateau members, 3 at rung 1, rung-2 reach between 15 and 53.
- A rung-2 rule that leaves the plateau one value wide has not drained it; the
  refinement column, not the reach column, is where that shows.
- `:open-hole-count` is recorded on every plateau member, varies 0–24, and no
  score reads it. Whether it belongs in a rung-2 derivation is a design question
  this row does not answer.
