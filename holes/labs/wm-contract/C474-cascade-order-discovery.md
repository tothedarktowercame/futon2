# C474 — I4 slice (a): what constructs the cascade, what reads R5 before R6,
# and what inverting the order would cost

**Row:** worklist.edn `:I4` (`:class :I`, owner claude-20). **Slice (a) is
DISCOVERY ONLY** — the acceptance says so in its own words ("DISCOVERY FIRST, no
implementation in the same slice"), which is why `:loop-mode
:one-slice-per-invocation` was added to the row: a description of the acceptance,
not a scheduling preference. Same handling as I5. No code changed, no ruling
written, no registry touched.

**The ruling under test.** `p4ng/empirics-futon/control-map-edges.edn:258-263`,
`:r6-r14-order`, `:grounds :ruling`, Joe 2026-08-30: *"select the target first,
then construct the cascade to match"*, `:derived-control-path [:R6 :R5 :R14 :R13
:R16]`, retiring `[:R5 :R6] [:R6 :R13] [:R11 :R16]` and adding `[:R11 :R14]
[:R14 :R13] [:R13 :R16]`. RUN3 classified the S1b route's `R5->R6` hop
**ruling-unrealised**: the machine still runs R5 before R6 and no code was
changed to match.

---

## 1. What constructs the cascade

`futon2.report.cascade-lane/cascade-lane`
(`scripts/futon2/report/cascade_lane.clj:404-454`), which shells out to the
Python constructor `cascade_serve.py` (`cascade_lane.clj:20-22`, launched at
`:41-59`, 30 s hard timeout at `:32-35`, memoized on `(psi, budget, epsilon)` at
`:63,:72`).

**Its one in-tick call site is `scripts/futon2/report/war_machine.clj:5047-5051`,
and it stands BEFORE selection**, not after: `cascade-policies` is bound at
`:5047`, `policy/select-action` is not called until `:5101-5110`. Its argument is
`wm-ranked` — the EFE-ordered list out of R5 — so the cascade is constructed from
the ranking.

Which entries it builds for (`cascade_lane.clj:438-451`):

- the top `n` (=3, `war_machine.clj:5049`) entries whose action type is
  `:open-mission`, and
- **entry #1 = `decision-entry`** (`cascade_lane.clj:391-402`) when
  `*gate-decision-target?*` is true — the default since an operator ruling of
  2026-07-06 quoted in the var's docstring at `cascade_lane.clj:381-389`: *"Yes,
  we should accept the decision so that the machine can act on its decision."*

That 2026-07-06 ruling has the same content as the 2026-08-30 one. It was
implemented, and **it was implemented against the wrong step**: `decision-entry`
returns `(first ranked-actions)` (`cascade_lane.clj:397`), the rank-1 of R5's
ordering. Section 4 measures how far that is from the target the machine records.

The lane is built a **second** time on the actuation path:
`enact/act-gates-with-shown` (`src/futon2/aif/enact.clj:162-171`) re-runs
`cascade-lane` over `(:ranked-actions judgement)` and `close-loop!` enacts the
**first `:pass`** gate (`enact.clj:307`), not the gate for the decided
mission. Reached only from `scripts/wm_scheduled_run.clj:108` under
`FUTON_WM_LIVE_WIRE`.

**No recorded run built a cascade at all.** `run_tick_once.clj:211` and
`full_loop_runner.clj:2501` both pass `:include-advisory-lanes? false`, so
`cascade-policies` is `[]` (`war_machine.clj:5047-5051`) on every tick in
`runs/2026-09-01*`. The `R5->R6` hop RUN3 classified is the ranking→selection
hop; the cascade is not on it. (Corroborated independently by C465's actuator
table, whose pointer `war_machine.clj:4820-4824` for this site has since drifted
to `:5047-5051` — reported, not repaired.)

Also worth stating because it reads like target-first and is not: the judge
docstring at `war_machine.clj:4666-4670` already says `:include-advisory-lanes?
false` is *"for real actuation, where the selected target is constructed once in
the subsequent construction phase"*. The construction phase it names is
`enact/enact!` → `engine-wiring` (`enact.clj:243-285`), and `enact!` receives
whichever gate passed first, so "the selected target" is not what it constructs
either.

## 2. What reads R5 before R6

R5 is `efe/rank-actions` (`war_machine.clj:5002`, route-tagged `:R5` at `:5003`);
R6 is `policy/select-action` (`:5101-5110`, route-tagged `:R6` at `:5113`). The
tick takes `:selection-boundary :strategic-recommendation` (`:5104`), so R6's
body is `policy/strategic-recommendation` (`src/futon2/aif/policy.clj:340-420`).
It reads R5's output in two distinct ways, and they are not the same kind of
dependency:

1. **The ordering, positionally.** `chosen (or (first controller-entries) (first
   ranked-actions))` where `controller-entries` is the input with `:no-op`
   dropped (`policy.clj:363-366`). R6's choice on this path *is* "the first entry
   of R5's sort that is not a no-op". No argmax, no sampling.
2. **The scores.** `g-totals (mapv :controller-score ranked-actions)`
   (`policy.clj:525`) → `adaptive-temperature` and `effective-temperature`
   (`policy.clj:358-359`, defined at `:32` and `:76`) → τ, the softmax weights,
   `:controller-ranking`, `:habit-adjusted-ranking` and the counterfactual
   (`policy.clj:389-401`).

The input is `wm-admissible` = `(filterv can-execute? wm-ranked)`
(`war_machine.clj:5091`), so every value R6 consumes is R5's.

**Consequence for the ruling, stated plainly: R6 cannot be moved before R5 by
reordering.** `select-action` is a function of the EFE scores and their order;
with R5 removed from in front of it there is no input. Putting R6 first requires
a *different selection law*, which is a J-class question, not a build.

There is a reading under which R6-before-R5 is already true, and it is worth
separating rather than letting it blur the above. `control-stages.edn:15` labels
R6 **"Candidate action space"**, not "selection". The candidate space is built at
`war_machine.clj:4933-4942` (`ap/compose-proposers`) and enriched at `:4954-4960`,
both before `rank-actions` at `:5002`, and it is `rank-actions`' second argument.
So **R6-as-candidate-space → R5 is a code fact today and carries no route tag**;
`R5->R6` is tagged because the tag names `select-action`. This is the same
observation already recorded in `control-map-edges.edn :derived-undrawn` as
`{:from :R6 :to :R5 … "reverses the drawn R5→R6 for the catalogue R6"}`. One box,
two roles, one tag — and which role the ruling retires decides whether I4 is a
no-op or a new selection law.

## 3. The three targets in one tick

The tick settles a target three times, and the last one is neither of the first
two:

| # | Step | Where | What it picks |
|---|---|---|---|
| 1 | R5 rank-1 | `war_machine.clj:5002-5013` | argmin G over the enriched candidates — **what `cascade-lane` builds for** (`cascade_lane.clj:397`) |
| 2 | R6 controller decision | `policy.clj:365` via `war_machine.clj:5101-5110` | first non-`:no-op` of `wm-admissible` |
| 3 | R14 strategic selection | `war_machine.clj:5126-5138` | first of `selected-mission-ids` found in `wm-admissible` |

and `wm-decision` then **overwrites the R6 action with the R14 one**:
`(assoc controller-decision :action (:action strategic-action) …)`
(`war_machine.clj:5155-5157`). That overwritten decision is what
`:ranked-actions`/`:decision` persist (`war_machine.clj:5277`) and what the trace
records.

The R14 candidate pool is a hard-coded three-element set
(`war_machine.clj:5114-5117`) intersected with `wm-admissible` in EFE order
(`:5118-5124`), handed to `strategic-selection-fn`.

## 4. Measured, on the records

Read with `clojure.edn` over `runs/2026-09-01-s1b/wm-trace-s1b.edn` (20 records)
and `runs/2026-09-01-s5/wm-trace-s5.edn` (4 records); `:ranked-actions` is
`wm-ranked+cascades` (`war_machine.clj:5277`) and
`[:decision :controller-ranking]` is R6's view of `wm-admissible`
(`policy.clj:389-393`).

- **R5 rank-1 = R6's input rank-1 in 20/20 S1b records** — 145 ranked actions,
  145 in `:controller-ranking`, so nothing was filtered by `can-execute?` and R6
  chose R5's rank-1.
- **R6's choice ≠ the recorded decision in 20/20 S1b and 4/4 S5 records.** R5/R6
  rank-1 was `M-expressions-of-interest` on all 24; the recorded decision target
  was `M-aif-policy-conditioned-eig` (10) or `M-wm-aif-policy-grain-compliance`
  (10) on S1b and the same two alternating on S5.
- So the entry `cascade-lane` would have built for — had the lane been on — is
  the one target the machine did **not** commit to, on 24 of 24 records.
- **Caveat, because it bounds every number above.** The recorded ticks ran the
  *stub* selector: `:selectorSeam "stub:first-ranked-authorized-mission"` in all
  four S5 receipts; the stub returns `(first scheduler-habit-ranking)`
  (`run_tick_once.clj:72-73`) because `futon3c.peripheral.live-wm-selection/validated-selection`
  (`run_tick_once.clj:18-19`) did not resolve. What the live selector picks is
  **not measured here**. The gap between step 2 and step 3 is measured; its
  *size* under the live selector is not.

## 5. What inverting the order would cost

Two different asks live inside one ruling, with very different prices.

**(i) Build the cascade for the committed target instead of R5's rank-1 — small,
and the couplings are enumerable.** Move the `cascade-policies`/`cascade-actions`
bindings (`war_machine.clj:5047-5065`) below `wm-decision` (`:5155`) and feed
them the decided target. What that touches:

- `wm-ranked+cascades` (`:5067-5068`) is built by appending `cascade-actions` and
  renumbering `:rank` over the concatenation. Appending later leaves the order
  and therefore the numbering unchanged; the persisted `:ranked-actions`
  (`:5277`) changes only in *which* cascade entries it carries.
- **The one real coupling:** `f-pi-dark-readback` (`:5079-5082`) and
  `beta-dark-carry` (`:5083-5086`) run over `wm-ranked+cascades`, and under
  `FUTON_WM_TAU_MODE=variational-beta-gamma` `beta-dark-fields` feeds
  `variational-temperature-opts` into `select-action` (`:5109-5110`). Under that
  mode the cascade actions are inside the field β is solved on, so making the
  cascade depend on the decision makes β depend on the cascade *and* the decision
  depend on β — a cycle where today there is none. The comment at `:5069-5078`
  says these were deliberately placed before selection for exactly this reason.
  The mode is off by default (`war_machine.clj:643-664`); RUN8/S3's β series were
  measured over `wm-ranked+cascades`, so any move changes what the S2/S3 arms are
  comparable to.
- `decision-entry` and `*gate-decision-target?*` (`cascade_lane.clj:381-402`)
  become either redundant or the seam where the decided target is injected.
- `enact/act-gates-with-shown` (`enact.clj:162-171`) rebuilds the lane from the
  served `:ranked-actions` and `close-loop!` enacts the **first passing** gate
  (`enact.clj:307`). Constructing for the decided target upstream does not
  make the decided target the one enacted; that is a second, separate change on
  the R16 side.
- No recorded run exercises any of it (§1), so a run that shows the change would
  first have to turn `:include-advisory-lanes?` on — which turns on the Python
  subprocess, up to 4 builds per tick at a 30 s ceiling each
  (`cascade_lane.clj:32-35`) against a 14.22 s mean tick span (I5's measurement
  over S1b). Memoization is on `(psi, budget, epsilon)`, so target-first changes
  the cache keys, not the number of builds.

**(ii) Make R6 run before R5 — not a reordering at all.** §2: `select-action`
consumes R5's scores and R5's order and nothing else. The price of literal
inversion is a selection rule that does not read G, which is a new law and Joe's
to rule on, not this row's to build. Under the catalogue reading of R6, the
inversion is already true in code and needs no build at all — only a decision
about which relation `[:R5 :R6]` draws.

## 6. What slice (b) owes

1. Say which of the two readings of R6 the ruling retires — §2 shows the answer
   decides whether I4 is a code change or a registry correction. This is a
   question for Joe if it cannot be settled from `:r6-r14-order`'s own text.
2. If (i): decide the β/F_pi ordering under `variational-beta-gamma` before
   moving anything, since that is where the move creates a cycle.
3. Reconcile the 2026-07-06 operator ruling (`cascade_lane.clj:381-389`) with the
   2026-08-30 one: they say the same thing, the earlier one is implemented, and
   it is implemented against rank-1. Whether that counts as "the ruling is
   realised at the wrong step" or "there are two rulings" is a ledger question,
   not a code one.
4. The other two pairs `:r6-r14-order` retires — `[:R6 :R13]` and `[:R11 :R16]` —
   are untouched by this discovery. `[:R6 :R13]` is independently code-retired by
   `:r6-r13-depth-precedes-selection` (`control-map-edges.edn:235-239`), so only
   `[:R11 :R16]` has no second ground.
5. Nothing here is a ruling. `:decisions` and `:choices` stay as they are.
