# DOSSIER — what stands between the War Machine and running in a loop

Owner: claude-7. Started 2026-10-22 at Joe's request: "keep a clear dossier of work to do … so that we don't just end up dealing with a giant pile of discovery-based debt and not getting back to the core issue of either running one click that works end to end, or better still, running the machine in a loop. If we can run the machine in a loop we can, presumably, get it to self-upgrade."

This file is the one list. Each item names the goal it blocks. Work that blocks no goal below is not added here; it goes to *Parked*, with one line saying why.

## Goals, in order

- **G1 — one click that works end to end.** One ordinary Trial click selects with the canonical G (Lean `GOverCascades` / `compareAtHorizonN`), executes, observes, updates the belief, and leaves a record whose every term matches the Lean contract (SPEC-useful-trial-loop-contract-2026-10-22.md, C1–C7).
- **G2 — the machine in a loop.** Two or more consecutive clicks with no hand between them, where click n+1 starts from what click n left (C5 carries) and selects afresh.
- **G3 — self-upgrade.** The loop selects and completes work on its own code, warranted by patterns. Not itemised yet: it is reached through G2, and items appear here once G2 runs.

## Rules for this file

1. A new finding gets a row here **only** if it blocks G1 or G2, with the evidence that it does. Otherwise it goes to *Parked*.
2. Each row has a done-when that can be checked against a record or a test, not a description.
3. G1 rows come before G2 rows. Discovery is a row of its own and ends in a finding, not in code.
4. When a row closes, write the closing commit or record in the same sitting.

## G1 — one click end to end

| id | what | why it blocks G1 | done when | state | evidence |
|---|---|---|---|---|---|
| A1 | Canonical G component: shared H, co-application, frontier novelty, per-candidate refusals | the score G1 selects with | merged, tests pinned against the Lean | **done** | futon2 `ee3cc89bc..305ca82e1`, `766c09d0a`, `5aa61b582` |
| A2 | Pattern-count producer from one θ snapshot | canonical G refuses without counts | merged | **done** | futon2 `31a6cf15f` |
| A3 | Canonical G on the reading-family route, `:g-law :canonical-coapply` (opt-in) | the route the law was first wired on | merged; default unchanged (pinned) | **done** | futon2 `9f24b0e87` |
| A4 | Click pass-through `wm_click.sh --g-law canonical-coapply` | a click must be able to ask for the law | merged; HTTP test | **done** | futon2 `70bfc786f`, futon3c `55ffe726a` |
| O1 | Shadow canonical G on the sealed outer menu (diagnostic only) | first look at canonical G on real targets inside a production click | a click record carries the shadow block beside the outer choice | **code done** (futon2 `3c7dd10ce`, reviewed by claude-7); the shadow has not yet appeared in a record: blocked by M1 | `~/notes/step4A-report.json` |
| O2 | **Lean:** the outer selection is `compareAtHorizonN` over outer cascades; outer evidence enters as pattern θ counts | Joe 2026-10-22: "the canonical G should be used in the outer loop … formally, the same." Production clicks choose in `outer_policy_objective/evaluate`, which has its own G | Lean module + theorem relating today's outer objective to it (where they agree, where not) | open (claude-7) | ruling in memory `outer-loop-as-cascade` |
| O3 | Code: the outer objective selects with canonical G | G1 needs the choosing G to be the canonical one | a click whose outer choice is made by canonical G, record matches the O2 statement | open, after O2 | — |
| C4 | Prefix F reaches the policy posterior (`policy_prefix_evidence.clj` stamps `:not-supplied`) | C4 of the contract; the posterior is σ(ln E − F − γG) | a click record with a computed F per policy; contradicted policy weight 0 | open | SPEC §6 C4 |
| C6 | Class from the focus: target class (so terminal preference) changes with the focus | C6 of the contract | conformance test mirroring `changing_focus_changes_terminal_preference` on the live path | open — **re-check first**: score-policy now passes `:scorer-class` from the target class; SPEC §6 was written at `9b597ce85` | SPEC §6 C6 |
| C1 | Observation depends on the action (conformance test) | C1 | test mirrors `fixture_action_changes_observation` | open | SPEC §6 C1 |
| C3 | Rollout starts at the current belief (conformance test) | C3 | test mirrors `rolloutFromBelief_changes_the_outcome` | open | SPEC §6 C3 |
| M1 | **A pending META injury pre-empts every ordinary click.** Click `wm-click-8539daf9…` (2026-10-22 22:28Z) routed to self-heal of the Trial5 injury `injury/04e14314a0ee…`; zai-2's diagnosis was refused twice by the provider ("1261 Prompt exceeds max length", also after compaction); self-heal then refused `:meta-self-heal/production-config-required` (absent: diagnosis-evidence, acceptance-test). No run record, no trace | while the injury stands, no click reaches ordinary selection, so neither O1's shadow nor T1 can be observed | an ordinary click reaches selection: the injury is healed, or its diagnosis completes and the self-heal route closes it. Per the standing rule, a refusal inside self-heal is a symptom to fix, not a reason to fall back to ordinary selection | open — **next**. First: why the diagnosis prompt exceeds the Zai limit (what it contains, how big) | `~/notes/step4A-report.json`, `~/notes/step4A-click.log` |
| T1 | **The click itself:** predict from the Lean, run one Zai-cast click, compare record to prediction | this is G1 | record + trace + prediction file agree, or the first differing field is a new row here | open, after O3 (a shadow click runs with O1) | — |

## G2 — the machine in a loop

| id | what | why it blocks G2 | done when | state | evidence |
|---|---|---|---|---|---|
| C5 | Carries between clicks: habit counts, d ← d + posterior, β prior, previous focus | click n+1 must start where click n ended | two consecutive click records satisfy the four carry equations | open — the 10-21 records retained **no** carries | SPEC §6 C5 |
| L1 | Learning between clicks: an executed pattern's outcome becomes a ledger trial, so its θ counts (A2) move | without it the loop repeats; novelty never falls | after click n, the chosen patterns' `pattern-theta` trial counts are n+1 in click n+1's count table | open — confirm the ledger write is on the live path | `learning_trial_ledger.clj` |
| U1 | Is a click one epoch or several (reselect after each pattern)? | decides what "a step of the loop" is | a ruling or a Lean statement | open, **not needed for G1** (`loop 1` = one-epoch click) | SPEC §5 U1 |
| U3 | How the next focus is chosen | the loop needs a next focus without a hand | ruling or a sourced specification | open, not needed for G1 | SPEC §5 U3 |
| R1 | Unattended driver: fire the next click when the last one closes, with a budget and a stop | the loop with no hand | N clicks run back to back from one command, within the click budget | open — check what `flight_driver.clj` already does before writing anything | — |

## Parked (found, does not block G1/G2)

- Reload completeness: on 2026-10-22 the step-4A reload left the serving `full-loop-runner` stale (it needs `cascade-plan` reloaded first). claude-7 reloaded both at ~22:40Z. A reload list for a click should name the runner; not a G1 row because a stale runner refuses visibly.
- Registry classes: split the equation registry's two classes into literature equation / literature equation at our binding / model specification / gap fill (Joe's question, 2026-10-22). Count then: 13 rows pinned to a literature equation, 7 theory-classed rows with no equation number.
- Lint warnings for unused section variables in `GOverCascades` and its neighbours (codex-11 build log, 2026-10-22). Cleanup when those modules are next touched.

## Log

- 2026-10-22 — dossier started. A1–A4 merged. O1 dispatched. Finding that production clicks bypass the reading-family route (codex-11, `~/notes/step4-route-probe.edn`) produced O1–O3; Joe ruled the direction the same evening.
- 2026-10-22 ~22:40Z — O1 code merged (`3c7dd10ce`). First shadow click pre-empted by the Trial5 META injury: new row M1, now first on G1's path. Runner reloaded.
