# C507 — U52: the three-rung ladder, and what it does to the plateau

Row: `worklist.edn :U52` (class I, implementation; epic `EPIC-run-era.md`).
Implements the ratified ruling `aif-equations.edn :choices
:task-belief-actand-source` (Joe, 2026-09-03, futon2 `efdc401`): case history
wins; where there is none, constructive generalization from kin, marked
`:constructed` with its derivation; only then a typed refusal that mints a
tension record. Split from U51 by handoff discipline: U51 measured the plateau,
this changes it.

**Default off.** `FUTON_WM_TASK_BELIEF_LADDER=1`
(`war_machine.clj:252-283`); with the flag absent the seam returns the
identical candidate vector and attaches no record. The flip is not this row's
to make and the flip-readiness gate says so — see §6.

## 1. What landed

| what | where |
|---|---|
| the ladder | `src/futon2/aif/task_belief_ladder.clj` (388 lines, no I/O but the mission-document read its kin relation declares) |
| the flag | `war_machine.clj:252-283` (`*task-belief-ladder?*`, opt `:task-belief-ladder?`) |
| the case-history index | `war_machine.clj:2667-2713` (`trace-corpus-dates`, `case-history-index`) |
| the seam | `war_machine.clj:2715-2745` (`apply-task-belief-ladder`), called at `war_machine.clj:6273-6274` |
| the record | `war_machine.clj:6706-6711`, present-only |
| tests | `test/futon2/aif/task_belief_ladder_test.clj` (9 deftests, 39 assertions); `test/futon2/report/war_machine_test.clj:2644-2714` (3 deftests) |
| the replay | `u52_ladder.clj`, artifacts `runs/U52-ladder/00-inputs.edn` … `05-controls.edn` |
| the mint | `u52_mint_refusals.bb`, appending through U41's `append-tension!` |

The seam sits between `enrich-candidates-with-mission-value` and
`efe/rank-actions`, because the plateau is a property of the *enriched* field
and because refusing a candidate has to happen before it can be ranked.

## 2. The rungs, operationalised at mission grain

- **rung 1** — the candidate's own action key `[type target]` is the chosen
  action of at least one persisted decision. Support = its own decision count.
- **rung 2** — no direct history, but a kin candidate has some. Support = the
  kin's decision mass. Marked `:task-belief/constructed true` with
  `:task-belief/derivation` naming the relation, the kin keys and their counts.
- **rung 3** — neither. The candidate is **removed from the field** with a typed
  `:refusal/reason :task-belief/zero-support-construction-exhausted`, and the
  partition mints one U41 tension.

Support enters the score through one number: `:mission-value-factor` is
multiplied by `n/(n+1)` at rung 1 and by `0.5 × n/(n+1)` at rung 2. That reaches
the controller score by exactly one path — `forward-model/mission-value-factor`
(`forward_model.clj:111-126`) into `predict-effects :advance-mission`
(`forward_model.clj:152-168`) — which is why §4's count of distinct factors and
count of distinct scores are the same count and not two claims.

**Case history wins is an ordering property of the arithmetic, not an assertion
about it.** A rung-1 candidate with one recorded decision scores 0.5; the
discount holds every constructed value strictly under 0.5. Control
`case-history-wins` checks it on the field and a test checks it at the extremes
(one decision against unbounded kin mass).

**Scope: `:advance-mission` only.** `:no-op`, `:address-sorry` and
`:fire-pattern` pass through with a `:task-belief/rung :out-of-scope` marker and
nothing else changed. Refusing them would remove the machine's fallback moves
for a reason this row has not measured.

## 3. Two declared inputs, neither of them a ruling

The ruling names kin as "kin actand-classes in the same table, shared patterns,
cascade-catalog playout" and **writes no relation down at mission grain** (U51's
finding). This row therefore *declares* one and rules on none:

- `:relation`, default `:k-doc-xref` — the mission documents' own
  cross-references.
- the support map `n/(n+1)` and the `0.5` generalization discount.

Neither is derived from a source. Both are reachable per call, recorded on every
classification, and **not written into `:choices`**; the flip's catalog entry
names their absence as what blocks it. They are the free hand TN-edge-review §1
point 3 sends to Joe.

Why `:k-doc-xref` is the default is a measurement, not a preference — §4.

## 4. The acceptance: the plateau on the recorded fields

Subjects: the committed s5 tick `4e35e740` (field 145, plateau **55 wide** at
ranks 73–127, chosen inside at rank 123) and the re5 tick `8ae111bc` (field 146,
plateau **56 wide** at ranks 75–130). Case-history index: 17 distinct chosen
keys over 889 decisions — the same corpus U51 read, reproduced by the shipped
index.

**s5, under the default relation:** of the 55, **39 are refused**, 2 keep their
own case history and 14 are constructed. The 16 survivors carry **8 distinct
controller scores**, and the widest tie left among them is **3**. The
whole-field widest tie falls from 55 to 6.

**re5:** 38 refused, 3 rung 1, 15 rung 2; 18 survivors over **11** distinct
scores, widest remaining tie **3**.

**The relation is what decides whether the plateau drains, and three of the four
candidates fail:**

| relation (s5) | refused | kept | distinct scores among the kept | widest tie left |
|---|---|---|---|---|
| `:k-type` | 0 | 55 | 3 | **53** |
| `:k-repo` | 29 | 26 | 4 | 18 |
| `:k-produces` | 53 | 2 | 2 | 1 |
| `:k-doc-xref` | 39 | 16 | 8 | **3** |

`:k-type` is the one to look at. It reaches 53 of the 55 — the best *reach* of
the four — refuses nothing, and hands 53 of them the same constructed value: a
53-wide tie where there was a 55-wide one. A ladder defaulting to it would
report a drained plateau and deliver an undrained one, which is exactly the
failure U51's refinement column predicted and the reason the default is the
relation that separates rather than the one that reaches. `:k-produces` is the
opposite failure: the capability-graph input recorded on all 55 carries zero
`:produces` entries, so it constructs for nobody and the ladder degenerates into
a bare refusal — the thing the ruling exists to prevent.

## 5. The replay is the shipped path, and one control is what makes that
checkable

Every rung comes from `war-machine/apply-task-belief-ladder`, the function the
judge calls. Every controller score comes from `efe/compute-efe`, the function
that produced the recorded ones — state rebuilt from the record's own
`:observation` and `:mu-pre`, modes read off the row rather than declared.

Control `off-arm-reproduces-the-record`: with the ladder off, the replay
reproduces **all 145 (and 146) recorded controller scores at maximum absolute
deviation 0.0, 0 rows differing**. That is the pin under everything else — a
difference in the ladder arm is the ladder's and not the replay's.

Control `flag-off-is-the-identity`: the *same vector object* comes back
(`identical?`), refusals empty, no ladder record attached. The default tick
cannot differ from a pre-U52 tick by construction, not by comparison.

**Twelve controls per field, 24 in all, every one passes.** Besides the two
above: `plateau-is-drained`; `case-history-wins`;
`constructed-is-marked-and-derived` (every rung-2 entry marked and carrying a
non-empty provenance); `refusal-is-typed-and-grounded` (count matches the
census, every record carries the one reason and a "not found" basis);
`empty-history-refuses-everything-in-scope` (with the index emptied, every
in-scope candidate reaches rung 3 — so the rung-1 and rung-2 answers read *that
index* and not something correlated with it);
`planted-unknown-candidate-reaches-rung-3` and
`the-same-plant-retyped-reaches-rung-2` (U51's negative-1 / positive-4 pair, the
one that makes a rung-3 answer mean something);
`a-recorded-chosen-key-reaches-rung-1`;
`distinct-factors-give-distinct-scores`; `out-of-scope-candidates-are-untouched`.

Two consecutive runs of the producer are byte-identical (no wall-clock field).

## 6. The flip-readiness gate was consulted, and it says no

`runs/RUNTIME-VALIDATION-CATALOG.edn :flips :task-belief-ladder` added, with
`:exercises [:R4 :R5 :R6]` derived from the one path the factor takes. The gate:

```
FLIP task-belief-ladder BLOCKED-ON [box2-holes figure5-partials flag-chain]
GATE: 7 flips | 0 READY | 7 BLOCKED
```

`flag-chain` is blocking *for this flip specifically*: its declared `:requires`
is a `:choices` ruling on the kin relation and the support map, which is not
machine-derivable and therefore blocks — which is what a flip whose declared
inputs are unruled should read as. `box2-holes` and `figure5-partials` block all
seven and are nobody's flip in particular. `runtime_validation_check.bb` PASS,
181 pointers, 0 unresolved; the catalog narrative's COUNTS line updated to
`flips=7 | pointers=181`, no row status, test run or head moved.

## 7. The tension mint, and where its boundary is

`u52_mint_refusals.bb --append` appends through U41's `append-tension!` — the
ledger's declared sole write API, loaded rather than reimplemented. Two tensions
minted, `:wm-ladder/s5-4e35e740-zero-support` (81 refusals) and
`:wm-ladder/re5-8ae111bc-zero-support` (80), both `:born-of :refused-prediction`
with the typed refusal in-record and the statement its verbatim readback (u41
control 12). The ledger validates after the append: 7 tensions, 15 events, 0
defects. A second `--append` returns `:already-present` and leaves the file
byte-identical.

**Three choices here are stated so they can be refused.**

1. **The live tick does not write the ledger.** `append-tension!` validates,
   appends and atomically replaces a curated file — a file write on the tick's
   critical path. The live ladder therefore builds the refusal records and
   attaches them to the judgement (`:task-belief-refusals`); the mint is a
   producer step over the recorded field. Named, not assumed.
2. **The unit is the partition, not the candidate.** The zaif rung-3 mint
   (`tension-ledger.edn`, `:U28z`) minted one tension for one refused
   (actand, arm) pair; here a rung-3 partition is 81 candidates, and 81
   near-identical tensions would be noise rather than a proto-pattern. The
   members are pointed at (`04-refusals.edn`, one typed record each), not
   inlined, because inlining would put three copies of the list into the ledger.
3. **The id is a keyword, not the refusal vector** as zaif's was, so a replay is
   `:already-present` without a third copy of the refused set.

**A defect in the shared write API, found and repaired.** `append-tension!`
documents that "replaying the identical pair is a no-op", and it was not: it
compared the caller's event against the stored one including `:event/seq`, a
number *the function itself assigns*, so the documented no-op was unreachable
for every caller. U52's mint is the API's second caller and hit it on its first
replay; the first caller appended once and never replayed. The comparison now
ignores `:event/seq` (`u41_tension_ledger.bb:145-172`). u41's own report and
controls re-run green. `u41_tension_ledger.bb` also gained a
`babashka.file` guard so it can be loaded as a library — which is what "sole
write API" requires if a second caller is to use it rather than fork it.

## 8. What is NOT claimed

- **No default flipped, no ruling written.** No `:choices` entry and no
  `:decisions` entry. `aif-equations.edn` is untouched by this row.
- **No live tick was taken.** No run lock held, nothing under `data/` written,
  no substrate write, no network. `gen_aif_dag.bb` not run into a publish
  (TN §9a).
- **Rung 1 means chosen, never chosen-and-it-worked.** U51 measured 0 of the 17
  chosen keys carrying an outcome; nothing in the corpus says whether any
  selection helped. The ladder is built on the only history the corpus holds and
  the index's docstring says so.
- **The refusal is not free.** Under the default relation the ladder refuses 81
  of 132 in-scope s5 candidates — more than the plateau. Whether a machine that
  refuses to rank two thirds of its field is better than one that ranks them all
  identically is not a question this row answers; it is what the flip-readiness
  gate exists to put to Joe.
- **`:open-hole-count` still enters no score.** U51 recorded that it varies 0–24
  over the plateau and is read by nothing. This row does not read it either;
  whether it belongs in a rung-2 derivation remains open.
- **The kin search is within the tick's own field**, as U51's was. Extending it
  to case-history keys absent from the field can only move candidates from
  rung 3 to rung 2.

## 9. Gates

`clj-kondo` 0 errors / 0 warnings on `task_belief_ladder.clj`,
`war_machine.clj`, `war_machine_test.clj`, `task_belief_ladder_test.clj`,
`u52_ladder.clj`, `u52_mint_refusals.bb`, `u41_tension_ledger.bb`;
`futon4/dev/check-parens.sh` OK on all of them. Tests, one namespace per JVM:
`futon2.aif.task-belief-ladder-test` 9/39 green,
`futon2.report.war-machine-test` 97/546 green, and unchanged green on
`futon2.aif.efe-test` (35/137), `futon2.aif.forward-model-test` (24/72),
`futon2.aif.policy-test` (54/296), `futon2.aif.full-loop-runner-test`
(122/576), `futon2.aif.trace-test` (46/145). `runtime_validation_check.bb` PASS;
`flip_readiness_check.bb` PASS after `--emit`; `u41_tension_ledger.bb` 0
defects, 12 controls green. `negative_controls.sh` and `pointer_check.bb` at
commit time — see the ledger row's `:evidence`.
