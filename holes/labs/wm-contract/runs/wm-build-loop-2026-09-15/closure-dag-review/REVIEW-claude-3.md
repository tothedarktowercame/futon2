# Closure DAG review: claude-3

This is a read-only review requested by codex-28 (`invoke-1789514060101-21287-f42b90ab`). Delivery order: this goes to codex-28 by bell after the WM-01-float-carrier-1 result.

Reviewed files, in `p4ng/wm-walkthroughs/build-loop/closure/`:

| File | sha256 |
|---|---|
| `CLOSURE-DAG.md` | `795b660c…` |
| `closure-dag.json` | `f4470036…` |
| `closure-dag.edn` | `789d027f…` |
| `LOOP-CONTRACT.md` | `bf6b63df…` |
| `validation.json` | `4211e47f…` |

## Verdict: CORRECTIONS

The graph is structurally sound. Seven problems need fixing before it drives dispatch:
- the meaning of an edge is lost when the graph is projected into either existing loop;
- several closure nodes need evidence that is produced by a node gated behind them;
- none of the operator-authority or production-operation gates the checklist names is a node;
- five delivery edges serialize work that doesn't need to wait;
- a few edges the cascades imply are missing;
- two pairs of R nodes are mutually dependent but drawn as an ordering;
- the existing topology loop violates the loop contract in five places (section 7).

## 1. Structural checks: all pass (I re-derived each myself)

- `checklist-sha256` `feaae4b6…` equals the current `CHECKLIST-fundamentals.md`.
- All 60 cascade `cascade-sha256` pins equal the current files.
- The graph is acyclic by my own depth-first search over `depends-on`.
- The Markdown predecessor table equals the JSON edge for edge.
- These agree with `validation.json`.

## 2. Edge meaning: this is where false circular gates arise

`edge-meaning` says an edge is a deliverable required before final acceptance, and that implementation may start before the dependency's checkbox closes. That is the right meaning, but **neither existing loop can represent it**:
- `wm-build-loop.sh` and `build_step.bb` treat a row as runnable only when every `:depends-on` is `:done` (`loopable?`).
- `topology-build-loop.sh` selects `next-open` from its worklist projection and dispatches only runnable rows.

Projected unchanged, every closure-to-closure edge becomes a dispatch block. The graph has no cycles, but these cases deadlock anyway:
- **WM-11 ↔ R16.** WM-11's own clause requires "independent selected-to-enacted correspondence". That is R16's work ("exact independently witnessed execution of the selected cascade"), and R16 depends on the WM-11 closure.
- **WM-03 ↔ R3.** WM-03 requires "same B at prediction and belief-update consumers". The belief-update witness is R3's content, and R3 depends on the WM-03 closure.
- **WM-02 ↔ R3.** WM-02 requires the "actual post-update" distribution. The real update is R3's, and R3 depends on the WM-02 closure.

**Correction:**
- Give `closure-dag.json` two edge kinds:
  - `dispatch-requires`: delivery and evidence nodes only, used by the projection to gate dispatch;
  - `accept-requires`: closure ordering, which never gates dispatch.
- Add shared evidence nodes that both sides of each pair require:
  - `EV-enactment`, requiring WM-09-delivery and WM-11-delivery, before both WM-11 and R16;
  - `EV-update`, requiring WM-02-delivery, WM-03-delivery and the WM-04 machinery (see section 4), before WM-02, WM-03 and R3.

## 3. Missing real-evidence and authority nodes

Several closure clauses need production operations or operator rulings. The loop contract keeps these outside the loop ("existing production-operation gates remain separate"), but none of them is a node. So the supervisor cannot report BLOCKED with the cause named, and it will keep dispatching packets that cannot finish.

Add non-dispatchable nodes, each with an owner:

| Node | Feeds | Source in the checklist |
|---|---|---|
| `OPS-serving-activation` | WM-02, WM-05, WM-08, WM-09, WM-10, WM-11 closures | WM-08 "serving activation"; WM-09 "serving activation"; WM-02 "production cascade predictor" |
| `OPS-ordinary-run/click` | WM-09 ("click receipt"), R16, E05 | |
| `AUTH-observation-commission`, then `WM-04-evidence` | WM-04, R2, R17 | WM-04 "independently authorized, blinded observation"; exercise 7 found 0/7 and codex-22's census 0/86. This is the largest real-evidence prerequisite in the graph, and today it is hidden inside WM-04-delivery's acceptance text |
| `AUTH-F4-scope` | WM-08 | "qualifying F4 scope … UNRESOLVED" |
| `AUTH-horizon-semantics` | WM-12 | "pending operator semantics cannot silently default"; the Alexander formulation is unsettled |
| `AUTH-mission-preferences` | WM-13 | "Mission layer remains undeclared" |
| `AUTH-C-bridge` | WM-06 | "state-to-disposition bridge remains subject to the model/C authority" |

## 4. Delivery edges that serialize work unnecessarily

- **WM-01-delivery, the prerequisite of eight deliveries.** Its acceptance includes "proof bindings", which is the whole rebinding campaign (binding-1 was one of many). Split it:
  - `WM-01-carrier-contract` (numeric-1 `ac821857`+`7f546b63`, support-1 `9aad9adf`, float-carrier-1 in flight) as the downstream prerequisite;
  - `WM-01-bindings`, which feeds only the WM-01 closure.
- **WM-05-delivery ← WM-04-delivery.**
  - WM-04-delivery's acceptance is observation acquisition: "independent eligible observations, exact review, coverage".
  - WM-05's cascade says "WM-02/03/04/06 are dependency interfaces in this WM-05 design".
  - Correction: split `WM-04-machinery` (admission, assembly and typed refusal) from `WM-04-evidence`. WM-05-delivery requires WM-04-machinery; the measured-A authority moves to the WM-05 closure (`accept-requires` WM-04-evidence).
- **WM-07-delivery ← WM-04-delivery.** Same correction: require WM-04-machinery, and move the evidence to the WM-07 closure.
- **WM-05-delivery ← WM-09-delivery and WM-12-delivery.**
  - WM-05's cascade names neither.
  - WM-12-delivery waits on operator semantics (`AUTH-horizon-semantics`).
  - Correction: WM-05-delivery requires the existing policy-plan interface and a declared horizon with a typed refusal. Move "real cascade plans … every horizon step" to the WM-05 closure (`accept-requires` WM-09-delivery and WM-12-delivery).
- **WM-06-delivery ← WM-13-delivery.**
  - WM-06's clause: "The full C-family obligation is separately retained below". The owner bell limits the packet to terminal `machineC`.
  - WM-06's cascade: "Output a member request and explicit unresolved dependencies on WM-13".
  - WM-13 waits on mission authority.
  - Correction: drop the edge. Keep the WM-13 member request as an explicit residual on the WM-06 closure.
- **WM-08-delivery ← WM-01-delivery.** WM-08's cascade names WM-03, WM-09 and WM-14, not WM-01. find/interpretation receipts do not consume the probability carriers. Correction: drop the edge.

## 5. Closure edges: missing or too strong

- **WM-03 closure: add WM-02-delivery.** WM-03's cascade: "Belief-update binding: prior state, same admitted B…"; "WM-02 state/belief … remain dependencies". The belief-update consumer belongs to WM-02. Or route it through `EV-update`.
- **WM-02 closure: drop WM-11-delivery.** WM-02's cascade: "uses policy identity at WM-02's interface without taking over WM-11's posterior work". "Subsequent selection" belongs to R1.
- **WM-04 closure: drop WM-05-delivery and WM-07-delivery.** WM-04's clause requires accepted labels, measured A and admitted assembly, not consumption. The consumer joins belong to R2 and R17.
- **WM-01 closure: include WM-13-delivery, or say why not.** C families are distribution carriers too (FoldC layers). WM-06 is included, WM-13 isn't.
- **Relations the R/E clauses themselves name but the graph lacks.** Confirm each as an edge, or record it as a non-dependency:
  - R10 ← R2: "bounded R2/predecessor joins";
  - R16 ← R2 (via `WM-04-evidence`): "re-observed outcome";
  - R17 ← R2, R6: "observation-to-count-to-model-change-to-next-decision";
  - R15 ← R2: "independent outcome-to-next-state feedback";
  - E08 ← WM-13, R19: goal formation and preferences.

  Every R/E cascade also links WM-01; that is boilerplate, not a dependency.

## 6. Mutual relations drawn as orderings

- **R14 ↔ R20.** R14 requires "R20 composition". R20 requires "applicable trip→R14 composition". The graph has R20 ← R14, so R14's acceptance needs evidence from its successor. Add `EV-trip-commitment-composition`, required by both.
- **R2 ↔ R3.** R2 requires "complete observation-to-update/learn joins", and R3 needs R2's observations. The graph has neither edge. Use `EV-update`, or a join evidence node required by both.

## 7. Runtime integration against the existing loops

**`wm-build-loop.sh` and `build_step.bb`: unsuitable, as `LOOP-CONTRACT.md` already says.**
- It runs anonymous `codex exec` / `claude -p` / zai-1 rather than named Agency seats.
- It has no persisted job ids.
- It notifies claude-1.
- Its publish step runs `build-p4ng.sh` and broad `git add`.
- It uses the historical `worklist.edn`, with F-first priorities.

**`topology-build-loop.sh` (apm-lean): the right base.** It already provides:
- synchronous `/invoke/jobs` polling;
- `runs/inflight.edn` with reclaim on restart (`resume_inflight`);
- one dispatch at a time;
- review before the next row;
- nonce-bound receipts;
- `TOPOLOGY_LEDGER`/`TOPOLOGY_RUNS` overrides, which the comments require for a second loop;
- `--dry-run`, `--replay` and `--once`.

It violates the loop contract in five places:

| Location | Current behaviour | Contract |
|---|---|---|
| `notice_target` | falls back to the first registered codex seat when the owner isn't registered | notify codex-28 only; fail visibly |
| `resolve_seat` | replaces an unavailable seat with any registered codex seat, or registers a fresh one via `/agents/auto`. This could put a codex seat in the reviewer role, or draw Joe's reserved seats: the defaults are `PROBE_SEAT`/`STRATEGY_SEAT` codex-12, owner codex-18 and reviewer codex-20, and codex-12 and codex-18 are among the seats Joe reserved | authors from codex-2/3/4, reviewer claude-3 |
| `validate` / `repository_safe` | hard-wires the topology `worklist_check.bb` and the apm-lean `.lake/packages` authority; `ROOT` is apm-lean, and transitions are committed there | a WM validator and the WM ledger's repository |
| terminal branch | `DONE` whenever the projection has no open or unreviewed rows; `PAUSED` only for `:needs-owner`/`:blocked` | `DONE` only when every closure node is accepted, otherwise `BLOCKED` with named causes |
| probe/strategy phases | exist | WM has none; codex-28 elects |

## 8. Smallest compatible adaptation

**Attended continuation needs no code.** The bellback path already runs this way: codex-28 elects a packet, I dispatch it to codex-2/3/4, review it and bell back. Five packets closed today without Joe saying "continue". The only change is that codex-28 elects the next packet from the corrected graph, projecting ready packets over `dispatch-requires` only.

The path has one real weakness. A park's resume is dropped if no REPL buffer is polling, although the job id survives.

**If an unattended supervisor is wanted,** run a separate instance of `topology-build-loop.sh` with its own `TOPOLOGY_LEDGER`/`TOPOLOGY_RUNS`. It needs five small switches, all off by default so the topology campaign is unchanged:
1. `STRICT_SEATS=1`: `resolve_seat` dies unless the configured seat is registered and on the allow-list (authors codex-2/3/4, reviewer claude-3). No substitution and no `/agents/auto`.
2. `STRICT_NOTIFY=1`: no fallback target. A failed bell to codex-28 is logged, and the loop exits non-zero.
3. `LEDGER_CHECK` and `REPO_ROOT` overrides: a WM projection validator in place of `worklist_check.bb`, skip the apm-lean package-authority check, and commit transitions to the WM ledger's repository.
4. `CLOSURE_DAG=<path>`: `DONE` only when every closure item in `closure-dag.json` is accepted; otherwise `BLOCKED`, listing the blocked nodes with their AUTH/OPS/evidence preconditions.
5. Refuse `:phase probe` and `:phase strategy` rows.

Before going live:
- run `--dry-run`;
- run `--replay` over the recorded WM-01 chain (acceptance-1 → numeric-1 → support-1 → binding-1 receipts);
- move from manual dispatch to the supervisor in a single step, so the two never dispatch at the same time.

## Limits

- Read-only. I didn't start either loop or change any code.
- I checked edges against the cascades' own cross-reference sentences and the checklist clauses. I didn't re-read every R/E cascade in full, so the relations listed at the end of section 5 are candidates to confirm, not rulings.
