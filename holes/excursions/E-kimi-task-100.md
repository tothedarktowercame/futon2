# E-kimi-task-100 — HG2-Ic-D: how the real evaluator reaches the field's ready entries (discovery, no edits)

**Requisition:** completed — 2026-09-27T03:37:10Z, job invoke-1790479875977-25274-62066a07, state done

Clocked in by claude-8 for kimi-1 on 2026-09-27 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# HG2-Ic-D: how the real evaluator reaches the field's `:ready` entries (⟨1⟩3 of PROOF-2a-PLAN; discovery before the caller-side wiring packet)

HG2-Ic-D (claude-8 → kimi-1), discovery under the coding-handoff protocol: you find out and propose, claude-8 reads; bell claude-8 back with the report below. NO commit, no edit to any tracked file, no test run beyond reading; `git status` in futon2 unchanged when you finish (codex-3's ITEM3-CONSUME-I lane holds war_machine.clj, flight.clj, temporal_input.clj, token_belief_predecessor.clj dirty — leave them). Your HG2-Ia (futon2 7f9fb3619) records the finding this packet closes the design for: the field's declared sources carry `:construction-parameters {:budget :move-cost}` (flight_driver.clj:190) but no `:construction`, so a live `:ready` entry records `{:absent :no-evaluator-supplied}`; your HG2-Ib (5217cb619) then keeps such a target at its E mass. The direction (Joe 9df277b13) wants ΔG_t for the ready targets from the SAME G selection uses — one authority — so the question is the route, not a new evaluator.

Read, quoting lines:
1. `flight_runner.clj:245-255` (`target-view` attaches `:construction {:construct ic/construct :budget … :move-cost … :evaluate-g wm/constructed-candidate-g}` to the click's view) and `war_machine.clj`'s `constructed-candidate-g` (its arity `[problem candidate]`, what it needs from PROBLEM — beta, schedules, locators, the assembled cascade-problem — and what it returns: `{:value :universe}` after the WIRE-24-A2 change?). Say exactly which keys of the assembled problem it reads (grep its body; the R5 `cascade-lane` call at :5985ff) and which of those the field does not have.
2. `target_field.clj` `assess` (:252-318 at HEAD before your commit; re-read at HEAD): the field's `sources` come from `cascade-sources/load-declared` via `flight_driver`/`outer_loop`; what does the field hold for a `:ready` target that `cascade-problems/assemble-one` would need (facts = the universe reading, wants, interpretations with receipts, locators?) and what is missing (beta from the accumulation? the preference schedule? measured-A labels?).
3. The two candidate routes, with their cost in lines and their authority consequences: (A) the caller (flight_driver / outer_loop, where `target-field` is called) supplies `:construction` with an `:evaluate-g` closure that assembles the base problem per target through `cascade-problems/assemble-one` (or the same path `target-view` uses) and calls `constructed-candidate-g` — the field stays as HG2-Ia left it; (B) the field itself calls the assembly for `:ready` targets and scores with the injected 2-arity evaluator. Say which keeps one authority for G and which keeps the field pure (no store access), and which you recommend and why. If a third route is better (for instance: the outer loop's plan mode already assembles the chosen target's problem — could it assemble every `:ready` target's and hand the ΔG back to the field?), name it.
4. What a live run would show today: how many `:ready` targets exist at HEAD (the direction said 0 on 09-25; count from the newest field record under `data/wm-runs/` or `runs/`, read-only, or say there is none to read), so the packet's test can be honest about the population.

Report (bell claude-8): the evaluator's needs vs the field's holdings as a table; the recommended route with files, functions and ~lines (under 250 for the implementation packet, or split); whether it needs a ruling from Joe (I expect none: the authority is 9df277b13 and the one-G rule); anything not yours, unfixed. Say what you read and what you did not.
