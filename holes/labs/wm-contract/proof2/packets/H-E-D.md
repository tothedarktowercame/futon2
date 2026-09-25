# H-E-D — E, the habit prior, estimated from enactment records

Hole H-E of `PROOF-2a-THEOREM-draft-2026-09-24.md` (Holes table L523;
worked-example finding 7, L478-481). Author: claude-13, 2026-09-25.
Discovery only: no code edits, no writes under `data/`, nothing run through
the machine. One read-only check was run in its own process (§2).

Anchors, all read this session:

| repo | HEAD | file | last commit touching it |
|---|---|---|---|
| futon2 | b457d220 | `src/futon2/aif/cascade_prior.clj` | 3f374efe |
| futon2 | b457d220 | `src/futon2/aif/cascade_habit_store.clj` | 1d20ca88 |
| futon2 | b457d220 | `src/futon2/aif/cascade_habit_reinforcement.clj` | 1d20ca88 |
| futon2 | b457d220 | `src/futon2/aif/policy.clj` | ad039985 |
| futon2 | b457d220 | the PROOF-2a draft | b457d220 |
| futon2 | — | `data/wm-habit/cascade-prior.edn` (gitignored, `.gitignore:67`) | sha256 `6026b590…6a`, mtime 2026-09-23 21:44 |
| futon3c | 7fc18ef0 | `holes/labs/M-futon-seams/exemplar/click-001-enactment.edn` | 1afa87d6 |
| futon3c | 7fc18ef0 | `…/exemplar/click-001.edn` | 6149272b |
| futon3c | 7fc18ef0 | `…/exemplar/construct-replay.edn` | a3f65fcc |
| futon3c | 7fc18ef0 | `…/exemplar/proof2a_check.clj` | cdb2d66f |
| mathlib4 | bd72ddcc | `DarkTower/WarMachine/CascadeEFEPolicies.lean` | 2b22eeef |

## 1. Identity: the policy key for click-001's enacted candidate

`policy-key` (cascade_prior.clj L46-70) needs a map with `:mission`,
`:shown` (a vector of pattern ids, order kept) and `:semilattice` (map or
sequential), and returns `[:pattern-cascade (str mission) shown
(canonical-semilattice semilattice)]`. The live seam builds that map with
`policy-view` (cascade_habit_store.clj L21-31): `:mission` is the
candidate's `:target`, `:shown` is `(mapv :id (:precedence candidate))`,
`:semilattice` defaults to `{}` when the candidate carries none.

The enactment record alone cannot give the key. It names only the click
path and `:candidate :cand/a-registry-first` (enactment L8-9). Its attempts
list patterns in the ENACTED order (mode-gate first, commit 8e5c431e), which
the record itself types as a deviation from the candidate's order
(`:step-order`, L21-22), so reading `:shown` off the attempts would key the
deviation, not the candidate.

Joined to the click by candidate id, three fields are needed and each comes
from a different place:

| key slot | source | value | status |
|---|---|---|---|
| mission | click L119 `[:decision :selection-certificate :candidate-derivations :cand/a-registry-first :target]` | `:inst/i4` → `(str …)` = `":inst/i4"` | present, but see absence A1 |
| shown | not in the click (it carries `:interpretations` as an unordered map, L127-169, and `:containment-order`, L183-191); from construct-replay.edn L17-24, the constructor's precedence | `[choose-the-grain-where-state-lives assignment-binding single-producer count-every-card-back placenta-transfer test-by-reproducing-behaviour mode-gate]` (namespaces as in the file) | needs a third file: absence A2 |
| semilattice | the click carries `:containment-order` (4 edges) and `:co-application-edges` (2 edges, L192-196) under its own names; `policy-view` reads `:semilattice` | `{}` under policy-view's default | absence A3 |

Hand-computed key under the live seam's own rules:

```
[:pattern-cascade ":inst/i4"
 [:cascade-construction/choose-the-grain-where-state-lives
  :coordination/assignment-binding :cycle-machine/single-producer
  :or3/count-every-card-back :gauntlet/placenta-transfer
  :translation/test-by-reproducing-behaviour :realtime/mode-gate]
 {}]
```

Typed absences (recorded, no field proposed):

- **A1 `{:slot :mission :status :ambiguous}`.** The live store's mission
  slots are mission ids (`"M-expressions-of-interest"` etc., §2). At
  click-001 the target is an instance inside a mission (`:inst/i4`; the
  mission is `[:mission :path]` = `holes/missions/M-futon-seams.md`, click
  L22). `":inst/i4"` is click-local naming (click L42-45), so two missions
  that both number instances would share the slot. The missing definition
  is which grain `policy-key`'s first slot names when the target is below
  the mission.
- **A2 `{:slot :shown :status :absent-from-click :found-in
  "construct-replay.edn"}`.** Neither the enactment nor the click carries
  the candidate's precedence.
- **A3 `{:slot :semilattice :status :no-mapping}`.** cascade_prior.clj's
  docstring (L4-7) says two cascades with the same patterns wired
  differently are different policies, but no reader maps the click's
  `:containment-order`/`:co-application-edges` to `:semilattice`. All 7
  live keys carry `{}` (§2).
- **A4 `{:field :record-id :status :absent}`.** The enactment carries no
  id of its own; `:n` is not unique (two attempts carry `:n 1`, L26 and
  L29). The pair `[:click :candidate]` identifies it (P_c: one record
  beside each click).
- **A5 `{:field [:decision :selection-law :candidate] :status :absent}`.**
  P_c joins "to `[:decision :selection-law]` by candidate id" (draft
  L259-262), but click L111-113 carries only `:target`. The preferred
  candidate is at `[:decision :selection-certificate :comparison
  :preferred]` (L318). `check-c` (proof2a_check.clj L180-181) reads
  `(:candidate selection-law cid)`, which defaults to the enactment's own
  id, so its "differs from the selected candidate" test cannot fail on
  this record. That test is vacuous here; for claude-10 (W_c checker owner).

## 2. Corpus: where the 21 live samples came from

The store keeps counts, not per-tick history (store L2-3), but every write
goes through `record-policy!` (store L105-118), which increments by 1
(`observe-policy`, cascade_prior.clj L108-118) and sets the key's basis
under the writer's field: `:selection-bases` for `record-selection!`
(L120-125, basis `:first-ranked-sharing-chosen-action`) and
`:reinforcement-bases` for `record-reinforcement!` (L127-132, basis
`:wm/cascade-habit-observed-want-v1`). Both fields exist since the store's
first commit (4c47b645). Read from the live file:

| count | mission slot | shown | :selection-bases | :reinforcement-bases |
|---|---|---|---|---|
| 9 | M-wm-aif-policy-grain-compliance | 1 pattern | yes | no |
| 5 | M-expressions-of-interest | 3 patterns | yes | no |
| 2 | M-dionysus-winddown | `[]` | yes | no |
| 2 | M-aif-policy-conditioned-eig | one-authority-per-question | yes | yes |
| 1 | M-aif-policy-conditioned-eig | every-entry-has-a-falsifier | no | yes |
| 1 | M-f11-find-production-successor | done-is-observed-running | no | yes |
| 1 | T-repair-occ-444fb018… | 2 patterns | no | yes |

The count-2 key in both fields had at least one write from each writer, so
exactly one of each. Totals: **17** from the legacy selection writer, **4**
from the close rule, **0** from a Clause C enactment. (The Holes owners row,
draft L617, says all 21 were reinforced at selection time; 4 were not.)
Every semilattice slot is `{}`.

What the close rule observes (cascade_habit_reinforcement.clj L57-83):
one increment per runner close (called at full_loop_runner.clj L4396-4399)
for the policy key of the controller's selected action, when the
comparison receipt is well formed and ANY wanted token of the selected
target has `:predicted-and-observed` (L67). It reads target-level wanted
tokens; it does not read which patterns were applied, whether the change
was the selected candidate, or any per-pattern check.

These are two counting rules. Worked on click-001's first attempt: commit
8e5c431e built the provider grain (outcome L5-13) and retired prefix
routing, so `:prefix-routing-retired` would be observed true and the close
rule would increment `:cand/a-registry-first`'s key; W_c fails on that
attempt alone (X_c(a), caught, see below). Clause C makes the enactment
rule the one E can be read from, by definition: E is a prior over policies
(cascade_prior.clj L2; Lean `logits .habit`, CascadeEFEPolicies.lean L71),
a count is evidence that a policy was carried out, and W_c (draft L251-258)
is the only stated condition under which the enacted change is the
candidate whose key receives the count. The close rule attributes an
outcome to the selected key without that condition. The legacy selection
writer counts selections with no outcome at all; its docstring says live
selection must not call it (store L121-122).

Check run (read-only, own process, futon3c 7fc18ef0):
`bb holes/labs/M-futon-seams/exemplar/proof2a_check.clj …/click-001.edn
…/click-001-enactment.edn` → `W_c … PASS`; X_c(first), X_c(check),
X_c(untyped) each `caught`. Not registered: it is a bb script, not a test
namespace.

## 3. The counting rule as data

The unit of E is a complete policy, so an enactment record contributes at
most one increment, to its candidate's key. Per-attempt successes and
failures are trials of the pattern's θ (clause 5, B), not of E.

| input | condition | E increment | carried on the increment receipt |
|---|---|---|---|
| enactment record | W_c holds (every candidate pattern has a successful attempt naming a `:check`; no attempt outside the candidate; each claimed token is produced by its pattern; deviations typed) | +1 to the candidate's key (§1) | record id `[:click :candidate]`, attempt count, deviation kinds |
| enactment record | W_c fails | 0 | the W_c failure strings |
| successful attempt with `:check` | — | 0 by itself; counts only through the record's W_c | — |
| successful attempt without `:check` | makes W_c fail if it is its pattern's only success | 0 | — |
| failed attempt (`:success false`) | — | 0 to E | a B trial for its pattern |
| typed deviation (`:step-order`, `:scope`, …) | W_c admits it | no change to the +1 | its `:kind` |
| untyped deviation | W_c fails | 0 | — |
| attempt at a pattern outside the candidate | W_c fails | refused, 0 | the outside pattern ids |
| record id already counted | — | 0 | `:duplicate-of` the earlier id |

On click-001's record: 8 attempts, 7 successful each with a `:check`, each
claimed token matches the click's `:produces` (checked by hand against click
L128-169), two typed deviations, W_c PASS → **+1**. The first attempt
contributes 0 to E. Over click-001's menu {a, b} with α 1.0 (log-priors
L135-156): counts a 1, b 0; E_a = 2/3 = 0.667, E_b = 1/3 = 0.333;
ln E_a − ln E_b = ln 2 = 0.693.

**Under G_c the count is 0.** X_c(d) (draft L267-268, L281-282) requires
the grain attempt to name G_c's pass as its check. The record's grain
attempt (n 3, L37-40) names an `:artifact` check, and `:grain :checked-by`
is `scripts/grain_check.py` (L17), the post-hoc check G_c replaced. The
checker does not read G_c yet (draft L268), which is why it passes. Under
the amended W_c: counts 0, 0; E = 1/2, 1/2. The record predates G_c, so
this is the record's age, not a defect in it; but the one record in the
corpus is not countable under the clause as now written.

## 4. Falsifiers the implementation's tests must pin

Each is built from the live click-001 record at test time, not from
authored constants.

| id | bad case | must observe |
|---|---|---|
| a | delete attempt n 7 (the only success for `test-by-reproducing-behaviour`) | count for a's key 1 → 0; E_a 0.667 → 0.5 |
| a′ | delete one success for a pattern that has two successes | count unchanged (no such pattern in click-001; construct by duplicating n 4) |
| b | strip `:check` from n 5 | W_c fails; count 0, not 1 |
| c | add an attempt `{:pattern :agency/single-routing-authority …}` (a cand/b pattern) | refused with the outside id; count 0; no increment to b's key either |
| d | fold the same record twice | count 1 |
| d′ | fold the record, then a whitespace-edited copy (different bytes, same `[:click :candidate]`) | count 1; dedupe by record id, not file sha |
| e | the provider-grain first attempt alone (X_c(a)) | count 0, although the close rule on its outcome would give 1 |
| f | add a G_c-pass check to the grain attempt, with the W_c checker reading G_c | count 0 → 1 |

(e) is the test that separates this rule from the close rule; without it
both rules pass (a)-(d) on the full record.

## 5. Effect on selection

Absent: **`{:quantity :G :candidate [:cand/a-registry-first
:cand/b-observe-first] :status :absent}`.** click-001 carries
`:p-all-wants`, `:e-wants` and kernel outputs per candidate (L208-217,
L308-315) and no G; its per-target `:selection-posterior` is
`:missing-definition` (L83-84); click-001-clauses.edn L44 has
`:E {:status :not-measured}`. Stopped here, without inventing G.

Two things hold without G:

- At click-001 itself E cannot change the ranking. Filled forward, the
  enactment post-dates the click; before it, both keys had count 0, so
  E_a = E_b and `lower_G_higher_prior` (Lean L81-87, hypothesis
  `E i = E j`) gives the order G alone gives.
- At a later click offering the same two keys, with the §3 count, the
  `.habit` logits `ln E − G` prefer a over b iff G_a − G_b < ln 2 = 0.693.
  E changes the ranking only if G alone prefers b by less than 0.693 nats.
  Under the G_c reading (count 0) it changes nothing.

## 6. AR-35 (draft L309-314)

The rule in §3 is a different thing from the priming prior. It is an
unconditioned categorical over complete policies; AR-35 is a prior over
lower-level patterns conditioned on higher-level ones. AR-35's falsifier
("remove one priming edge; the recorded prior must change") does not bite
on policy-key E: with `:semilattice {}` (A3) removing an edge changes no
key and no count; with edges in the key it changes the key to a new one at
count 0, which is a change of identity, not of a conditional. One record
(one policy, count 1) estimates no conditional in any case. Whether AR-35's
prior is an E at all cannot be decided from records, however many: the
draft says it needs a Lean definition first (L311-313), and that definition
does not exist.

## Answers in one line each

1. Not from the enactment alone; from enactment + click + construct-replay,
   key above; absences A1-A5 (mission slot ambiguous at instance grain,
   precedence absent from the click, no semilattice mapping, no record id,
   no selection-law candidate id; A5 makes check-c's selected-candidate
   test vacuous).
2. 17 legacy selection writer, 4 close rule, 0 enactment; two rules; Clause
   C's W_c is what makes an enactment count attributable to a policy.
3. One +1 per record iff W_c; attempts are B trials; on click-001 E =
   (0.667, 0.333), or (0.5, 0.5) under G_c.
4. (a)-(d) as asked plus a′, d′, e, f.
5. G absent; no change at click-001 by `lower_G_higher_prior`; ln 2 margin
   at a later click.
