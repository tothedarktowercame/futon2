# L5-5 — the two paths that bypass outer selection, as selections (claude-3, 2026-11-01)

Discovery only. Base: futon2 `f53be9709`. No code, serving or `data/` change. No click.
Scripts: `~/notes/l5-5/` (`census*.clj`, `probe.clj`). The probe wrote two scratch decision
records under `/tmp` and deleted them.

## Recommendations

**Direct task entry: no third `RunKind`, and no "must win".** A declared pick becomes an
observation with a declared source. The selection run happens over the same open field. The
asked task is reachable through one library cascade, and it wins only by G. If it loses, the
record shows why. Nothing depends on the bypass today: 0 of 290 click acceptances carried a
task, and no script sends one.

**Self-heal: keep the route for now, fix its record, then bring repair into selection in three
steps.** `repair-goes-to-the-front` can be made executable from facts the repo already holds.
`end-in-something-shown` can be made executable for repair tickets only. An injury cannot
reach the field today: none of the 9 pinned injuries joins to a finding. The route has to stay
reachable for the case where selection itself is what is injured.

**Do first, either way:** defects D1–D3 below. Any `:not-exercised` record is currently
projected as `abstain-empty-field`, and the click after it cannot reach Q6 conformance.

## 1. Direct task entry

### Where it arrives

| Entry | Site | Reaches `run-ordinary-opportunity-impl!` |
|---|---|---|
| HTTP | `futon3c/src/futon3c/transport/http.clj:10152` turns a JSON `task` string into `{:task {:id …}}`; `:10220` keeps it in `:acceptance-payload` | yes, branch at `full_loop_runtime.clj:1324` |
| In-process | any caller of `run-opportunity!` with `:task` | same branch |
| CLI | `full_loop_cli.clj:37-50` `runner-opts` has no task flag | no |
| Click scripts | `wm_click.sh`, `wm_click_loop.sh`, `wm_click_prepare.py`, `wm_click_repair_loop.py` send no task | no |

The HTTP clause came from `86bde441a` (2026-10-28), the day `a04b157d2` deleted the outer
selector and required a caller-supplied task. `768c27e94` wired R20 selection back the same
day. The direct branch stayed. Use since then: `grep` over all 290 files in
`futon3c/data/wm-click-acceptances/` has one hit, and that hit is test source quoted inside a
prompt, not a `task` field. So 0 of 290. Test namespaces do use it in process (at least 8 under
`test/`, for example `full_loop_runtime_test`, `outer_selection_wiring_test`).

The ordinary path also sets `:task` after choosing (`full_loop_runtime.clj:1406-1408`) and calls
the same `run-ordinary-opportunity-with-task!`. Removing the branch at `:1324` does not touch
that call.

### What is recorded about the choice today

`full_loop_runtime.clj:1327-1328` writes section `{:status :not-exercised :reason
:direct-task-entry}` and outcome `{:status :not-exercised :path :direct-task :reason
:task-supplied}`. The decision record does not name the task. The tick has the work decision's
target. `:issuing-caller` and the task id stay in the acceptance file. Nothing retains the
operator's words, and `http.clj` has no field for them.

### What a selection record would carry

- **Field:** the same open set, built by the same observation. No restriction to the asked task.
- **Declared source:** a new carrier, `{:who :at :words :target :acceptance-pin}`. The pattern's
  guard is exact: "the operator's words, dated, with where they were said"
  (`task-selection/the-operator-asked.flexiarg:9`).
- **Menu:** the ordinary menu plus one cascade. The packet's single-cascade menu fails Q2 at
  `Requirements.lean:372` (`comparisonFloor` = 2 for `.selection`, `:293`).
- **G:** `outer_score.clj:148-171` computes it from status, relation, gain and reach, as for any
  pick. An unknown relation lands on the stopped outcome, so the pick predicts stopping and
  fails `predictsOtherThanStopping` in Q11.
- **Reach:** from `:field-facts`, so the target must be admitted. On the newest sectioned tick
  (`ac5bc477`, 2026-11-01) 545 were admitted and 81 excluded. An excluded target has no fact, so
  the asked task cannot be scored. It is refused with its exclusion reason, which matches the
  pattern: "If it cannot be done, the inventory of why becomes the result."
- **Abstention floor:** `abstention-score` (`outer_score.clj:241`) is unchanged. Q11 requires the
  chosen G strictly below it.

### The two readings

**A third `RunKind` is the wrong reading.** A kind with floor 1 makes lawful the one-cascade menu
that `Requirements.lean:833` keeps as a decided negative witness and the plan lists as a
tripwire. The projector already guards this: "A record choosing its own label could dodge the
selection floor of two" (`wm_run_facts.clj:657-658`). It also breaks the September ruling's
uniformity: "every run carries the same certificate burden — possible only when every run
performs selection" (`RULING-selection-precedence-2026-09-19.md:14-20`).

**"Must win" is also wrong.** If the cascade must win, its G or its menu position is being set
to produce the result, and the comparison decides nothing.

**The honest reading: it competes, and the instruction enters as an observation.** The library
already says so: the pattern's `@effect` is "adds the want to the field as a candidate … it is
then gated and ordered like any other candidate."

### Theory: an observation with a declared source, not a prior over policies

The instruction is evidence about a hidden state, namely what the operator currently wants. In
the kernel that state is the focus, and it reaches G through
`relation(focus, target)` → ending (`outer_score.clj:107`). So the channel is the relation.

- **Not E.** Under ruling 1 the posterior is a record and E does not choose, so an E bump would
  have no effect. Where it did have one, it would raise a policy without any claim about what
  would happen, which is what the ruling removed.
- **Not C.** C is learned (`wm-preferences-c-are-learned`). Rewriting it per ask would change what
  the machine prefers, not what it believes.
- **Yes, the relation.** `relation-source-precedence` (`focus_receipt.clj:105`) has no operator
  source. A new source kind, with the retained words as its receipt, would give the asked target
  a known relation. `focus-receipt` is "record-only" and "never supplies scoring C" (`:2`), so
  this does not cross into C.

### A blocker in the class rule

The relation channel alone is not enough. A class is keyed by prediction digest and G units, and
`outer_select.clj:172` sets `:representative-rule :first-in-menu-order-within-one-prediction-class`.
Suppose the asked task and another task land on the same `[ending gain]` with the same reach. They
are then one class, and menu order chooses the pick. The charter would have no effect, or else
someone places the operator cascade first, which is the rigged result in a hidden slot.

The outcome space has to tell them apart. `the-operator-asked` has a prediction of its own:
"what is delivered is recognised by the operator as what was asked". Adding that as an outcome
factor is a kernel version change (`outer_score.clj:7-10`). It does not change
`Requirements.lean`. I have not designed it. It is the next discovery if this route is taken.

### Q2 and Q11 consequences

- Q1 is unchanged. An ask for something not open must first be published as a ticket, or
  `target ∈ enumeratedTasks` fails. That is the pattern's "put it in the selectable pool".
- Q2 is met by the ordinary menu, with no change.
- Q11 is unchanged. An asked task that loses is an ordinary selection of something else.
- Q7 gains one path entry if the charter is a carrier: a pick claiming a charter with no receipt
  counts as an absence.
- Tripwires to add: a charter with empty words; a menu whose order depends on the charter.
- The executable row is `:operator-portfolio`, `:not-executable`, needs `[:operator-charter
  :capacity :cost-risk :resolvedness :portfolio-composition]`
  (`futon3/library/task-selection/executable-steps.edn`). A single-pattern component cascade, as
  `:only-what-can-start-now` is to row 1, needs only the charter.

If Joe wants a task run regardless of G, that is an override, and it should stay what it is: the
selection run still happens, the enacted task differs from the minimum, and Q11 fails on the
record. I would not build that unless asked. No one has used the entry since it was added.

## 2. Self-heal

### Trigger and what it knows

`run-opportunity-with-authority!` (`full_loop_runtime.clj:1557`) reads the injury ledger before
any field is built: "An active injury consumes the opportunity without fallback." `route!`
(`meta_self_heal_production.clj:2072`) returns nil with no active injury, and a map otherwise.

An injury entry holds `:injury-id`, `:cause-id`, `:failure-kind`, `:failure-stage`,
`:detected-click`, `:cause-record-pin`, `:state`, `:attempt`. It has **no task target**. Observed
in `data/wm-meta-injuries/ledger.edn`: 76 entries, 9 injuries with a cause pin.

There are two repair stores, and only one reaches the field:

| | Meta injuries | Repair obligations |
|---|---|---|
| Store | `data/wm-meta-injuries/ledger.edn` | `data/wm-repair-obligations/findings/` |
| Task identity | none | ticket `T-<repair-id>` |
| On the field | no | yes: 162 of 545 admitted on `ac5bc477` |
| Open | not counted (see limits) | 299 findings: 171 machine-failure, 100 environmental-hold, 17 review-failure, 9 incomplete-recoverable, 2 actuation |
| Repaired target | via `:cause-record-pin` to the tick | `:target`: 149 of 299 have one (59 `M-`, 12 `T-`, 2 `E-`, 76 other); 150 have none |

The route's own acts are diagnosis, author, review, enact, acceptance and retry. They are not a
cascade over a task. A self-heal click has no work run. `:meta-repair-retry` then re-enters
`run-ordinary-opportunity!` (`full_loop_runtime.clj:1640`), so the **retry child is already an
ordinary selection** with its own record. It does not pin the repaired target unless the parent
carried `:task` (`runner_service.clj:1468-1481`).

Volume: 91 of 744 click bindings took `:dedicated-meta-self-heal`, of which 37 refused
`production-config-required` and 34 reached `:awaiting-retry`. A further 7 took
`:open-out-of-band-medic`.

### Could row 3 run over the field?

| Step | Executable now? | What it needs |
|---|---|---|
| `only-what-can-start-now` | **Yes.** `:ready-gate`, `outer_menu.clj:25` | nothing |
| `repair-goes-to-the-front` | **No, but the facts exist.** | (a) a new step: `outer_menu.clj:25-27` and `outer_state.clj:193` admit only `:ready-gate`, `:by-cone`, `:by-unblocks`, `:by-rank`, `:status-gate`. (b) a per-task obligation fact in `:field-facts`. `classify-target` already computes it at `focus_receipt.clj:429-452` (`active-repair?`, `{:kind :open-repair-obligation …}`), but only as a relation source, and it requires a string `:machine-repo`, which 243 of 299 open findings have. |
| `end-in-something-shown` | **Repair tickets only.** | `:needs [:verifiable-capability]`. 283 of 299 open findings carry a `:discharge-contract` (`:distinct-repair-commit`, `:independent-review`, `:distinct-production-shaped-successor` on the one I read). After the repair gate, the field is repair tickets, so this covers row 3. It does not generalise to row 6. |

`repair-goes-to-the-front`'s effect is conditional: "if a repair obligation is open **on the path
of the next candidate**". On `ac5bc477`, 42 open findings have a `:target` that is an admitted
task, and all 162 admitted repair tickets are their own candidates. A plain "keep repair tickets"
gate would fire on every run, because the field always holds repair tickets. Then the row reduces
to a fixed preference, and G only compares it with the other cascades. The path test needs a
definition: I recommend "the ticket's own finding has `:target` equal to the gate's next
candidate", computed after the readiness gate.

### The injury has no way onto the field

Q1 requires `enumeratedTasks = openTasks`, so an injury cannot enter as its own id. It has to
become an open ticket. `medic_ticket_closure.clj:52-80` defines the join, by the cause byte pin or
by the finding's `:origin-run/id` equal to the cause record's own run id. Censused over the live
stores, it is empty: 0 of 299 findings carry `:cause-record-pin`, 1 of 299 carries
`:origin-run/id`, and none of the 9 pinned injuries matches a finding. So no injury has a ticket
on the field today, and the first step below is to publish one for each injury at the moment it
opens.

### What cannot be moved

Selection may be the thing that is injured. The route must stay reachable when selection cannot
run, or the machine cannot repair the code that picks its repairs (`exhausted-frame-never-blocks-queue`).
That case stays a non-selection, stays typed, and alerts.

The 2026-09-19 ruling also says an ordinary click "always runs cascade selection" and open
obligations "no longer pre-empt". The later META arbitration pre-empts. Under row 3, if G picks
something else while an injury is open, the injury waits. That disagrees with "active injury
consumes the opportunity" and with `repair-before-the-next-click`. Bringing self-heal into
selection means choosing between those. My recommendation: an injury whose failure stage lies on
the next candidate's path is on the path by definition, so the gate fires and the arbitration
becomes the gate's consequence, not a separate rule. "Stage lies on the path" is not yet defined,
and that is why this comes last.

### Order

1. Publish a ticket when an injury opens, with the join key on the finding.
2. Fix the record (D1–D4). The route stays.
3. Add the obligation fact and the step; put row 3 on the menu as a compared cascade. Self-heal
   arbitration is unchanged, so this adds no behavioural risk beyond selection.
4. Then decide arbitration by the path definition.

## 3. Records

Today, `:not-exercised` carries only the fields quoted in section 1, plus the run identity and
`:chain-index` added by `write-decision!`. Self-heal differs in `:path :meta-self-heal` and
`:reason`, taken from the result. A completion receipt follows on both paths.

| Field | Selection record | Direct today | Self-heal today | Direct needs | Self-heal needs |
|---|---|---|---|---|---|
| Field | `:field-facts` | absent | absent | same open set | same open set |
| Giraffe, makeup pins | `:pins` | empty | empty | yes | yes |
| Construction | `:construction` | absent | absent | library slice + charter cascade | library slice + row 3 |
| Compared | `:cascades`, `:classes` | absent | absent | yes | yes |
| Abstention | `:abstention` | absent | absent | yes | yes |
| Choice | `:selection`, `:chosen-class` | absent | absent | by G | by G |
| Digest | `:choice-input-digest` | absent | absent | yes | yes |
| Previous | `:previous` | absent | absent | yes | yes |
| Learning | `:outer/learning` | absent | absent | yes | yes |
| Declared source | none | absent | n/a | new charter carrier | injury / obligation receipt |
| Target | `:selection :selected` | **not named** | **none** | asked task, if it wins | repair ticket |

## 4. Readers

| Reader | What it does with these records |
|---|---|
| `wm_run_facts.clj:1115-1195` | `section?` is true for the two-key map, so it takes the populated branch. See D1. `:workRunStatus` covers `:refused` and `:abstained` only (`:1185-1187`). See D2. |
| `wm_run_alert.py` | Evaluates both runs. Fails closed: rc=1 on the scratch record. |
| `previous_run.clj:221`, `:260-277` | Returns `{:status :not-exercised}`, and this becomes the next selection's `:previous`. See D3. |
| `wm_click.sh:697-699` | Falls to the standalone record when no tick exists. |
| `wm_click.sh:724-730`, `:748-758`, `:784-790` | Self-heal goes to the child's record. A medic-only discharge exits 0 with "requirements not applicable", so that click gets no Lean verdict. |
| `selection_record.clj` `latest` | Indexes `:not-exercised` like any decision, and refuses to skip back past it. |
| `futon3c` | No reader of `:selection-record`, `:selection-outcome` or `:outer/selection`. `runner_service.clj:1045`, `:1376` read `:runtime-disposition` from the result, including `:ordinary-selection-called?`. |
| `scripts/wm_route_check.py:197` | Reads `:ordinary-selection-called?` from the binding. |
| Report card: `scripts/wm_report_card.clj`, `controlled_report_card.clj` | Grep finds no read of the selection record or the section. Not read in full. |

Live: **no `selection-record-*` file exists in `data/wm-runs/`**, so neither path has run since
L5-4f. D1–D3 have not yet happened in production. They will on the first such click.

## Defects found

**D1. `:not-exercised` projects as a claimed abstention on an empty field.** Shown by putting
scratch records through the real `selection-facts-for-record`: `currentChoice
"abstain-empty-field"`, `enumeratedTasks []`, on both paths. `selection-decision`
(`wm_run_facts.clj:1007`) reads a nil target as that case. The alert fails closed (Q1 violated,
"0 enumerated / 645 open tasks", Q6 line "current abstain-empty-field"), but its text states
something that did not happen. No typed `Decision` exists for "selection did not run".

**D2. A self-heal record's alert reports a work run that did not exist.** `:workRunStatus` is
nil for `:not-exercised`, so the alert printed `work run:` with nine Qs from the decision record.

**D3. The click after any bypass cannot conform on Q6.** Read, not run: the next selection's
`:previous` is `{:status :not-exercised}`, so `selection-history-facts` (`wm_run_facts.clj:917`)
returns not-recomputable for all three previous-run facts. A retry child's predecessor is its
parent's bypass, so every self-heal retry inherits this. The fix must keep the rule that the
reader does not skip back silently: for example, `:not-exercised` could carry the last real
decision's reference, with a typed gap.

**D4. `:path :meta-self-heal` also labels failures that are not self-heal.**
`full_loop_runtime.clj:1664-1667` writes it for anything with no ref: authority refusal (`:1658`)
and an exception in arbitration (`:opportunity-exception`, `:1661`).

**D5. The direct decision record does not name the task** (section 1).

**D6. A third entry, outside this packet.** A flight carries a hand-placed target
(`flight_driver.clj:87`, `{:absent :hand-placed}` at `:201`). Over HTTP it reaches
`run-opportunity!` with `:flight` and no `:task` (`http.clj:10168`), so outer selection runs.
`full_loop_runtime.clj` passes `:flight` through only as an option key at `:1074`. So the outer
pick and the flight's target may differ. Read only; I did not trace the assembly.
`flight_runner.clj:76` defaults to `runner/run-opportunity!`, which skips the runtime and writes
no selection record. `flight_driver.clj:305` uses the HTTP click function by default.

## Limits

- D1, D2: the probe ran on two scratch records. The `probe.clj` projection used an empty snapshot;
  the alert run used the real census.
- D3 and all statements about the click scripts are from reading code.
- Tick census: 256 tick records; 28 of the newest 80 have a section, dated 2026-10-29 to 11-01.
- The open-injury count is omitted: 48 of 76 ledger entries carry no `:state`, and
  `effective-active` needs the medic verifier, which I did not run. Folding the last `:state` by
  hand would give a wrong number.
- `:target` kinds in section 2 are by id prefix.
- "42 open findings" and "162 admitted repair tickets" are on one tick. No trend is measured.
