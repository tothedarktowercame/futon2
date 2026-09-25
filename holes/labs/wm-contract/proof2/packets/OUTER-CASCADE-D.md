# OUTER-CASCADE-D — the outer loop as a cascade (read-only discovery)

claude-12, 2026-09-25, on claude-8's requisition (terms recorded futon2
`66120f4c`). Read-only: no join built, no ranking wired, nothing loaded into a
JVM, nothing under `data/`. Readers: claude-10 (E-outer-loop, Clause T), claude-8.

Read at: futon2 `66120f4c` (target field `src/futon2/aif/target_field.clj`
last changed `7bd17dfb`; fixture
`test/fixtures/target-field/target-field@futon2-7bd17dfb.edn`); futon3c
`4bc95005` (`src/futon3c/logic/cascade_real.clj`, `cascade_real_live.clj`,
last changed `d0741b04`); `holes/E-outer-loop.md` at `aedcc6ae`;
operator-turn batches under `/home/joe/code/storage/operator-turns/batches/`
(2,160 published analyses at ~15:25Z). Live reads 2026-09-25 15:16–15:30Z.
D2 (same day, claude-10's first read): step names are the field's `:next-step`
values; `construct` guard aligned with the field; §5 split by construction vs
measured; mission → pattern re-read; falsifiers (d) and (e) run.

**Answers up front.**
1. The outer cascade can be stated with today's objects: problem = the
   target field, actions = (target, step kind), patterns = the five step kinds
   (the field's `:next-step` values) plus `learn` and `defer`, guards on
   facts the field and cascade-real already carry. §1.
2. Of E-outer-loop's six proposers, three are carried (mission, ticket,
   bootstrap as `learn` and `defer`); sorry and tension are not; patterns
   enter as evidence, not as targets. Excursions are a new enumerator with
   no old counterpart. O1–O4 and O7 are addressed by the structure; O5, O6 (in part) and
   O8 are not. §2.
3. The per-(target, step) G needs a step-outcome model that does not exist
   (as H-G-TARGET-PRIOR-D §2 found). With rates at
   `:zero-adjudication-identity` and E uniform, the structure **partitions**
   the 59 missions; the 282 excursions and tickets cannot join cascade-real
   by construction. §5.
4. The turn → mission join is weak: the clock path cannot answer for a past
   time; the best-guess path joins 4 of 40 sampled turns. §4.

## 1. The outer cascade as an interpretation

**Problem.** The feasible set of the target field: lifecycle-shaped missions
(futon4 `holes/mission-lifecycle.md`, Conventions; `target_field.clj:20-24`),
tickets and excursions readable at HEAD. Fixture: 343 feasible (59 missions,
30 tickets, 254 excursions), 158 excluded, all `:not-lifecycle-shaped`. Live
read today: 354 feasible (§3 on why it grew).

**Universe (facts per target).**
- From the field: `:kind`, `:next-step` (331 `:read-criteria`, 12
  `:ask-interpretation` in the fixture), the finding behind it
  (`:criteria-not-stated`, `:no-wants`, `:no-published-interpretation`), and
  for asks the named wants with criterion lines (`target_field.clj:125-131`).
- From cascade-real (`GET /api/alpha/cascade-real/graph`), keyed by the
  mission node slug `<repo>-d/mission/<slug>` = target id without `M-`:
  O1 arrows (`have`/`want`, all `:close-hole`), O4 cluster membership, held
  items (`registry` prose 115, sorrys 9), lineage (agent, session, `at`).
  Record shape for lineage in the outer problem (claude-10; not built):
  `{:agent :target :session :at :dispatched-by <caller-id | :kimi-requisition
  | nil>}`, from the clock edge's witness (§5).
- From the operator turns: per fragment `pattern_refs`; rejections and
  candidates typed as evidence of a searched miss. They join only via §4.

**Patterns (step kinds), with guards and produces.** Names are the field's
`:next-step` values (`target_field.clj:10-17`). Guards are support conditions;
none is a refusal (every target stays in the field; `:25-26`).

| pattern | guard (as the field decides it) | produces |
|---|---|---|
| `read-criteria` | no wants read, or every want already true (`:181-182`, `:193`) | criteria stated for the target |
| `ask-interpretation` | open wants and no published interpretation (`:194-195`), or the constructor reports an unproduced need (`:143-145`) | a published interpretation per named want |
| `construct` | a published interpretation exists (`:194`, then `ic/support` at `:199`) and support is not reached (`:207`) | the constructor's finding; `:observation-required` becomes `observe` (`:141`) |
| `observe` | the constructor's `:observation-required` finding (`:141-143`) | observed token values |
| `ready` | `ic/support` returns `:supported` (`:206`) | a supported family, before any G (the flight's input) |
| `defer` | any target | a typed deferral with its reason, never a zero |
| `learn` | a step kind with no executor available for this target | a target (E-/T-) to enable that step kind |

Construction starts once a published interpretation exists; unobserved
tokens are the constructor's own finding, not a guard before it.

`learn` restates the bootstrap proposer ("enable the class itself",
`aedcc6ae:29-32`) over step kinds; `defer` keeps the old `:no-op` as a record
(proposed every tick, never chosen, `aedcc6ae:122-123`).

**Red tape as a cost term, not a guard**: the rate, per step kind, at which
an agent step following an operator ask went beyond it. Today the notes are
prose in the Kimi seats' reports. The field that would carry it, per case: `{:ask <turn-id+fragment span> :followed-by <job id | commit
sha> :step-kind <kw> :beyond-ask? <bool> :basis <span or diff hunk>}`. Its
red-tape-removal case, from the blocks read so far: codex-2 turns 6→8–10 (a
one-attempt repair bound became a "Guide-authored receipt is required"
boundary), codex-10 turn 427 (a void classification became a full test
run), claude-8 turns 2–3 (a race condition became a mathlib rebuild),
claude-13 turns 463–464 → block 008's 17 inbox-zero spam turns, codex-17
turns 70–98 (gates instead of the mathematics). Not built here.

## 2. E-outer-loop's proposers and defects, mapped

Carried: `mission-enumerator-proposer` and `ticket-enumerator-proposer`
(restored as the field's enumerators, `7a5f6c0b`); `bootstrap-proposer` as
`learn` and `defer` (§1). New, with no old counterpart: the excursion
enumerator (254 of 343).

| old proposer (`5d55e7a0^`) | in the outer cascade | reason |
|---|---|---|
| `pattern-enumerator-proposer` | not carried as targets; patterns enter as universe evidence | a pattern is not an M/E/T object; no definition makes it a target |
| `sorry-enumerator-proposer` | not carried as targets; 9 held `sorrys` items visible as facts | `resources/sorrys.edn` entries are not in the field's enumerators |
| `tension-proposer` | not carried | cascade-real has no tension dimension (O2 = 0, `GET /api/alpha/cascade-real`) |

O1 (nothing proposes) and O2 (proposing step dropped): addressed, each
target's `:next-step` is its proposal and the patterns are the proposing step.
O3 (feasibility as refusal): addressed at `7bd17dfb`. O4: addressed by `learn`.

| defect | status in the outer cascade |
|---|---|
| O5 value signals dead | not addressed: mission value, interest posterior and time pressure have no reader here; E at target grain would come from H-E's fold (one record today) |
| O6 sorrys, patterns, tensions left the view | partly: patterns and held sorrys as evidence; tensions absent |
| O7 no record of loss | addressed if the outer record lists each proposer's status as in this table |
| O8 nothing chooses targets | **not addressed**: the choice needs G per (target, step), whose model is absent (§3, §5) |

## 3. Tokens at HEAD and typed absences

| token | at HEAD | source / what would write it |
|---|---|---|
| target set, kind, `:next-step`, finding | yes | `target-field` (live 7.8 s, §6) |
| named wants + criterion lines (asks) | yes, 12 targets | `target_field.clj:125-131` |
| O1 arrows per mission | yes, 176 arrows, 352 nodes | `/cascade-real/graph` `arrows` |
| O4 cluster membership | yes, 117 | `clusters` |
| held items | yes, 124 | `held` |
| lineage (clocked agents) | yes, 88 | `lineage` (futon1b `clock/clocked-on`) |
| mission → pattern citations | yes by direct store read: 1,173 `mission-scope/pattern` edges, 223 missions, 33 of the field's 59 | futon1b `GET /api/alpha/hyperedges?type=mission-scope/pattern` (2 pages, 55.9 s, 15:26:20Z). Through the endpoint: `{:failed-read :section "mission-scope/pattern"}` twice, 15:16Z ("authoritative substrate read timed out") and 15:25:17Z ("request timed out", `:timeout-ms 5000` per page, `/cascade-real/graph` as-of 1790349917976); the section's 5 s page budget is shorter than the store's page time |
| hole-basins | `{:absent :none-found}` (checked: s4-evidence, holes-found 0) | `cascade/hole-target` edges |
| tickets per target | `{:absent :not-per-target}` | `tickets` is a recency list (40 of 4,702) |
| turn evidence per target | `{:absent :no-mission-join}` | a tested join (§4) |
| step-outcome model Q(o \| step) | `{:absent :no-step-outcome-model}` | H-G-TARGET-PRIOR-D §2; decline records could train one |
| target prior E | uniform, no data | H-E's fold at target grain (1 record, M-futon-seams) |
| owner-stated priority | `{:absent :upvote-placeholder}` | `portfolio/policy.clj:119-124` returns 0.0 |
| red-tape link | `{:absent :not-recorded}` | the field in §1 |

**The field grows with dispatch records (falsifier (d), run).** Live: 354
feasible, 11 more than the fixture (`E-kimi-task-29..38`,
`E-usage-aware-dispatch`; 8 `T-repair-occ-*` left). 38 live targets are
`E-kimi-task-N`, and all 38 carry the header "Clocked in by <caller> for
kimi-N … see scripts/kimi-task.sh" (`:origin {:minted-by
"scripts/kimi-task.sh"}`); prefix and header agree on all 265 live excursions. `E-usage-aware-dispatch` (futon3c `79cc0e58`, "Owner: claude-10.
Driver: Joe.") has no such header and is correctly an owner's excursion.
Excursions skip the lifecycle test (`target_field.clj:23-24`), so the 38
enter as work targets; the header is what would tell them apart.

## 4. The mission join for operator turns

Records carry `agent_id`, `session_id`, `turn_id`, `created_at`; none carries
a mission (0 of 3,286 request records).

**Clock path.** The live clock store is an in-memory atom
(`futon3c/src/futon3c/agency/clock_store.clj:21`). Its durable trace is
futon1b `clock/clocked-on`: 88 edges, id keyed `agent.mission`
(`hx:clock/clocked-on:<agent>.<mission>`), so one latest clock per pair with
`clocked-at-ms`, no end time, spanning 07-12 to 09-25. It cannot say what a
session was clocked on at a past time:
`{:absent :clock-history-not-kept}` for this path. Session-id matches alone:
22 of 2,160 published records; 0 of the 40-record sample.

**Best-guess path.** `futon3c/scripts/backfill_turn_commit_mission_bestguess.py`,
run as a copy with its output redirected to `/tmp` (its `OUT` is under
`futon5a/data/`, 2.0 s): 42,479 user turns, 17,607 commits, 3,470 with a
guessed mission (touched `M-*.md` or file-mention overlap). It keys on Claude
transcript session ids, so the 627 codex-seat records cannot match by
construction. Sample: 40 published records, `random.seed(20260925)`.

Session present in best-guess rows: 22 of 40 (1,279 of 2,160). A commit in
the same session within 90 min after the turn, with a guessed mission:
**4 of 40 (10%)** (144 of 2,160, 6.7%). Session has any guessed mission: 24 of
40 (1,211), but that does not say which mission a turn was about.

**Evidence density.** 1,106 of 6,256 published fragments (17.7%) cite a
library pattern. Fragments with neither a rejection nor `no_surface_cue`:
0 in blocks 001–010 and 012; 1,579 in seven blocks analysed before that rule
was checked (000, 021, 023, 025, 027, 030, 031), by script. Turn dates run 08-22 to
09-21; the field is at 09-25. Recency weighting has nothing newer than four
days to weight; a target with no joined turn is
`{:absent :no-turn-evidence}`, never 0.

## 5. The zero-rate ordering on today's field

Receipt: rates `:zero-adjudication-identity` (`futon2 src/futon2/aif/efe.clj:1341-1348`,
the case where every adjudication rate is zero); E uniform over T_f with no
data (H-G-TARGET-PRIOR-D §1); G per (target, step)
`{:absent :no-step-outcome-model}`. No number here is a rate.

With G absent on every member and E uniform, nothing orders the targets.
The structure gives a partition by which facts a target has.

**Join rule.** A target joins cascade-real when its id without `M-`,
lower-cased, equals the slug of a node matching `-d/mission/`
(`futon3c cascade_real_live.clj:273`, `mission-ep`; O1 `:99`, O3 `:65`, O4
`:118`). Excursions and tickets have no node of that form, so they cannot
match: `{:absent :no-excursion-ticket-nodes}`. A join for E-/T- would need
cascade-real to emit `<repo>-d/excursion/<id>` and `<repo>-d/ticket/<id>`
nodes and edges keyed on them; only `tickets` lists them today, as a
recency list, not as facts.

| part of the field | targets |
|---|---|
| cannot match by construction (excursions, tickets) | 282 (252 + 30) |
| measured (missions) | 59 |

Measured, over five fact kinds (O1 arrows, O4 clusters, held, lineage,
mission → pattern citations from the direct read), both ways of counting
lineage:

| fact kinds present | missions, all lineage | missions, dispatch-caused lineage excluded |
|---|---|---|
| 0 | 17 | 18 |
| 1 | 12 | 13 |
| 2 | 14 | 13 |
| 3 | 14 | 13 |
| 4 | 2 (`M-diagramprover`, `M-the-perfect-crime`) | 2 (same) |

Dispatch-caused lineage, by clock-edge witness: 38 `dispatch-mission-id`, 14
inherited with a `caller-id`, 14 Kimi seats (every Kimi call carries its
caller's requisition, `zai_api.clj` `parse-requisition`); 6 of 88 are own.

**Falsifier (e), run.** `M-the-perfect-crime`'s lineage falls 28 → 2: 11 Kimi
seats I dispatched today (jobs name caller claude-12), 3 Kimi seats whose job
records have aged out, 12 inherited from claude-8 and claude-5. The 2 left are
codex-18's and claude-8's own clock decisions, so it stays in the top tier:
tiers count presence, not size, and D's wording of (e) fails. More facts is
not a preference; it says where a step's outcome could be observed.

## 6. Construction cost (one read, wall time)

`GET /api/alpha/cascade-real` 3.76 s (652 B); `/cascade-real/graph` 5.60 s
(84.6 KB; 12.8 s on the 15:25Z re-read); `clojure -M -m
futon2.aif.target-field` in its own JVM 7.81 s incl. start; futon1b
`clock/clocked-on` 3.5 s first, 0.01 s repeat; `mission-scope/pattern` direct
read 55.9 s; best-guess script copy 2.0 s; all 2,160 turn analyses 0.05 s.
About 20 s end to end without the pattern read, about 75 s with it.

## 7. Falsifiers

- (a) Make one target `:criteria-not-stated`: its pattern must become
  `read-criteria`, and no other fact of it may change.
- (b) A guard that drops a target instead of changing its step breaks `:25-26`.
- (c) Two targets with identical facts and step kind must be tied under
  zero rates. Any order between them reads something not in §3.
- (d) Run (§3): 38 of 38 `E-kimi-task-N` carry the kimi-task.sh header;
  none misclassified. Still to run: delete them in a scratch checkout; the
  feasible count must fall by exactly 38.
- (e) Run (§5): lineage 28 → 2 with dispatch-caused entries excluded; the
  tier is unchanged. Restated: with all lineage excluded, a target whose
  tier falls reads lineage; one whose tier holds does not.
- (f) Once the §1 link exists, the red-tape cost is 0 for a step kind with
  no `:beyond-ask? true` and changes when one case is added.
- (g) Rerun the 4-of-40 join with the same seed and best-guess output; a
  different count means the sample or the output changed.
