# OUTER-CASCADE-D — the outer loop as a cascade (read-only discovery)

claude-12, 2026-09-25, on claude-8's requisition (terms by whistle ~15:55Z,
recorded futon2 `66120f4c`). Read-only: no join built, no ranking wired,
nothing loaded into any JVM, nothing written under `data/`. Reader: claude-10
first (owner of `holes/E-outer-loop.md` and Clause T), then claude-8.

Read at: futon2 `66120f4c` (target field `src/futon2/aif/target_field.clj`
last changed `7bd17dfb`; fixture
`test/fixtures/target-field/target-field@futon2-7bd17dfb.edn`); futon3c
`4bc95005` (`src/futon3c/logic/cascade_real.clj`, `cascade_real_live.clj`,
last changed `d0741b04`); `holes/E-outer-loop.md` at `aedcc6ae`;
operator-turn batches under `/home/joe/code/storage/operator-turns/batches/`
(2,160 published analyses at ~15:25Z). Live reads 2026-09-25 15:16–15:30Z.

**Answers up front.**
1. The outer cascade can be stated with today's objects: problem = the
   target field, actions = (target, step kind), patterns = the six step kinds
   plus `learn` and `defer`, guards on facts the field and cascade-real
   already carry. §1.
2. Of E-outer-loop's six proposers, three are carried (mission, ticket,
   bootstrap as `learn` and `defer`); sorry and tension are not; patterns
   enter as evidence, not as targets. Excursions are a new enumerator with
   no old counterpart. O1–O4 and O7 are addressed by the structure; O5, O6 (in part) and
   O8 are not. §2.
3. The per-(target, step) G needs a step-outcome model that does not exist
   (as H-G-TARGET-PRIOR-D §2 found). With rates at
   `:zero-adjudication-identity` and E uniform, the structure **partitions**
   the field; it does not order it. §5.
4. The mission join for operator turns is weak: the clock path cannot answer
   for a past time; the commit best-guess path joins 4 of 40 sampled turns.
   §4.

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
- From the operator turns: per fragment `pattern_refs`, and
  `pattern_rejections` and candidates **typed as evidence of a searched
  miss**, not as citations. They join to targets only through §4.

**Patterns (step kinds), with guards and produces.** Guards are support
conditions on the facts above; none is a refusal (every target stays in the
field; `target_field.clj:25-26`).

| pattern | guard (facts) | produces |
|---|---|---|
| `read-criteria` | finding `:criteria-not-stated` or `:no-wants` | `:criteria-stated` for the target |
| `ask-interpretation` | criteria stated; named wants with no published interpretation | one published interpretation per want |
| `observe` | wants interpreted; tokens unobserved | observed token values |
| `construct` | wants interpreted and observed | a constructed candidate (the inner cascade) |
| `fly` | a candidate with finite ΔG | an enactment record (H-E's fold input) |
| `defer` | any target | a typed deferral with its reason, never a zero |
| `learn` | a step kind with no executor available for this target | a target (E-/T-) to enable that step kind |

`learn` is E-outer-loop's bootstrap proposer ("when no concrete actions exist
for an action class, … enable the class itself", `aedcc6ae:29-32`) restated
over step kinds. `defer` carries the old `:no-op` as a record rather than a
choice (I3: proposed every tick, never chosen, `aedcc6ae:122-123`).

**Red tape as a cost term, not a guard.** The measured quantity would be the
rate at which an agent step that follows an operator ask goes beyond the ask,
per step kind. No record carries that link today: the red-tape notes are
prose in the Kimi seats' reports, not fields. The field that would carry it,
one per case: `{:ask <turn-id+fragment span> :followed-by <job id | commit
sha> :step-kind <kw> :beyond-ask? <bool> :basis <span or diff hunk>}`. Its
red-tape-removal case, from the blocks read so far: codex-2 turns 6→8–10 (a
one-attempt repair bound became a "Guide-authored receipt is required"
boundary), codex-10 turn 427 (a void classification became a full test
run), claude-8 turns 2–3 (a race condition became a mathlib rebuild),
claude-13 turns 463–464 → block 008's 17 inbox-zero spam turns, codex-17
turns 70–98 (gates instead of the mathematics). Not built here.

## 2. E-outer-loop's proposers and defects, mapped

| old proposer (`5d55e7a0^`) | in the outer cascade | reason |
|---|---|---|
| `mission-enumerator-proposer` | carried: the field's missions | restored as the field's enumerator (`7a5f6c0b`) |
| `ticket-enumerator-proposer` | carried: the field's tickets | same |
| (none) | excursion enumerator | added in the field on the same shape; 254 of 343 targets |
| `bootstrap-proposer` | `learn` carried; `:no-op` as `defer` record | §1 |
| `pattern-enumerator-proposer` | not carried as targets; patterns enter as universe evidence | a pattern is not an M/E/T object; no definition makes it a target |
| `sorry-enumerator-proposer` | not carried as targets; 9 held `sorrys` items visible as facts | `resources/sorrys.edn` entries are not in the field's enumerators |
| `tension-proposer` | not carried | cascade-real has no tension dimension (O2 = 0, `GET /api/alpha/cascade-real`) |

| defect | status in the outer cascade |
|---|---|
| O1 nothing proposes | addressed: each target's `:next-step` is its proposal |
| O2 proposing step dropped | addressed: the patterns above are the proposing step |
| O3 feasibility as refusal | addressed already at `7bd17dfb` (every target feasible); guards stay support |
| O4 learn gone | addressed by `learn` |
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
| mission → pattern citations | `{:absent :section-read-failed :section "mission-scope/pattern"}` | the section read timed out on futon1b at 15:16Z (`section-status`); claude-8's count of 73 is `composition.O1xO4` on `/cascade-real`, a different read |
| hole-basins | `{:absent :none-found}` (checked: s4-evidence, holes-found 0) | `cascade/hole-target` edges |
| tickets per target | `{:absent :not-per-target}` | `tickets` is a recency list (40 of 4,702) |
| turn evidence per target | `{:absent :no-mission-join}` | a tested join (§4) |
| step-outcome model Q(o \| step) | `{:absent :no-step-outcome-model}` | H-G-TARGET-PRIOR-D §2; decline records could train one |
| target prior E | uniform, no data | H-E's fold at target grain (1 record, M-futon-seams) |
| owner-stated priority | `{:absent :upvote-placeholder}` | `portfolio/policy.clj:119-124` returns 0.0 |
| red-tape link | `{:absent :not-recorded}` | the field in §1 |

**The field grows with dispatch records.** Live today: 354 feasible, 11 more
than the fixture: `E-kimi-task-29..38` and `E-usage-aware-dispatch`; 8
`T-repair-occ-*` tickets left. 38 of today's feasible targets are
`E-kimi-task-N` excursions, each minted by `futon3c/scripts/kimi-task.sh` as a
one-shot requisition for a Kimi seat. They are lifecycle-free by kind
(excursions are not tested against the form, `target_field.clj:23-24`) and
enter the problem as work targets. Nothing in the field distinguishes a
dispatch record from an owner's excursion.

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

| join | sample 40 | all 2,160 |
|---|---|---|
| session present in best-guess rows | 22 | 1,279 |
| a commit in the same session within 90 min after the turn | — | 230 |
| … with a guessed mission | **4 (10%)** | 144 (6.7%) |
| session has any guessed mission (not per turn) | 24 | 1,211 |

The per-turn rate is 10% on the sample. The session-level rate is higher but
does not say which mission a turn was about.

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
What the structure does give is a partition by which facts a target has
(fixture field × live `/cascade-real/graph`, slug match):

| facts present | targets |
|---|---|
| none | 303 (252 excursions, 30 tickets, 21 missions) |
| one kind | 21 (11 O4 only, 7 O1 only, 3 lineage only) |
| two kinds | 16 |
| three kinds | 3 (all missions) |

40 targets have any cascade-real fact; 35 of them are at `read-criteria`, 5
at `ask-interpretation`. Having more facts is not a preference; it says where
a step's outcome could be observed. Example of why: `M-the-perfect-crime`
has lineage 28, almost all Kimi seats clocked by my own dispatches today. A
lineage count measures dispatch activity, and the outer cascade's own
workers inflate it.

## 6. Construction cost (one read, wall time)

| read | wall |
|---|---|
| `GET /api/alpha/cascade-real` | 3.76 s (652 B) |
| `GET /api/alpha/cascade-real/graph` | 5.60 s (84.6 KB) |
| `clojure -M -m futon2.aif.target-field` in its own JVM (incl. start) | 7.81 s |
| futon1b `clock/clocked-on` (one page, 88 edges) | 3.5 s first, 0.01 s repeat |
| best-guess script copy | 2.0 s |
| all 2,160 turn analyses | 0.05 s |

About 20 s end to end, sequential, dominated by the two cascade-real GETs
and the JVM start.

## 7. Falsifiers

- (a) Remove one target's criteria (make it `:criteria-not-stated`). Its
  pattern must become `read-criteria` and nothing else about its facts may
  change.
- (b) A guard that removes a target from the problem, rather than changing
  its step, contradicts `target_field.clj:25-26` and O3's fix.
- (c) Two targets with identical facts and step kind must be tied under
  zero rates. Any order between them reads something not in §3.
- (d) Delete the `E-kimi-task-*` files from a scratch checkout. The
  feasible count must fall by exactly their number; if the field does not
  change, the count in §3 is wrong.
- (e) Recompute §5 with lineage excluded. `M-the-perfect-crime` must leave
  the three-fact tier; if it does not, lineage is not what put it there.
- (f) The red-tape cost term, once the §1 link exists, must be 0 for a step
  kind with no recorded `:beyond-ask? true`, and must change when one case
  is added. A cost that moves without a new case reads prose, not records.
- (g) The 4-of-40 per-turn join: rerun with the same seed and the same
  best-guess output. Any different count means the sample or the script's
  output changed.
