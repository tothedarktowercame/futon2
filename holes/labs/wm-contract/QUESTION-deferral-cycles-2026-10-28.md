**Question withdrawn (2026-10-28, after Joe's reply).** Joe: the question was about protocol, not AIF; a tie means the machine is not discriminating enough. The finding below the line bears that out. The evidence the machine already holds orders the two injuries, so neither a held result nor a tie-break rule is needed. The original text is kept below the line.

## What the run records show

The two cause claims come from different retry clicks:

- **2aef → c560 (ordinal 58).** 2aef's retry click `1586bf55` stopped at `:ticket-readiness-refresh` with `:wm-job-ledger-unavailable`, and that stop opened c560 (ordinal 56). 2aef's own stage, `:author-wait`, comes after that and was never reached. So c560 really did stop 2aef's retry before 2aef's repair could be tested.
- **c560 → 2aef (ordinal 67).** c560's retry click `8c8ee35f` passed `:ticket-readiness-refresh`, which is c560's own failure stage. It took 106 s with no debugger stop: the only stop was in `:selection`. The click then went on through cascade-resume and author-dispatch and failed at `:author-wait` with `:invalid-author-refusal`, which is 2aef's failure. So c560's repair was tested and its failure did not recur. The ledger nonetheless recorded the retry as inconclusive, with 2aef as the cause.

## Where the machine fails to tell them apart

`other-open-injury` (`meta_injury_ledger.clj:142`) counts another injury as the cause if it was opened, or seen as a repeated active injury, anywhere in the same run record. That records co-occurrence and treats it as a direction. It never asks whether the click reached the retried injury's own failure stage before the other failure stopped it. With that check:

- 2aef's deferral to c560 stands, because the click stopped before `:author-wait`.
- c560's claim against 2aef does not, because the click passed `:ticket-readiness-refresh`. That retry is evidence about c560's repair, and the ledger should treat it as such, not requeue it on 2aef.

So the cycle is a product of the cause test and not a real tie. A cycle check in the router would only hide it. The fix belongs in the cause attribution: phase order in the run record, compared against the retried injury's `:failure-stage`.

Whether "passed its own stage" should close c560 or only lift the requeue cause is the remaining design point. That is for codex-1, the Codex owner, to decide.

---

**This proposes adding to the specification** (no current rule covers deferral cycles). It goes to Joe and is not to be implemented until he rules.

# Question for Joe: what happens when two injuries each defer to the other (2026-10-28)

From claude-11 (A2r). Nothing has been built for this. The finding comes from codex-1, job
`invoke-1793147857187-5697-f2334225`. I checked the lines it cites against futon2 `87c10683a`.

## What happened

The live store is `data/wm-meta-injuries-live-2026-10-22/` and it holds two transactions for injury 2aef:

- `transaction.edn`: injury `2aef7e0c…`, repair `2f682638…`, attempt 2 (written Oct 28 00:07)
- `transaction.edn.deferred-injury_2aef7e0c….edn`: the same injury and repair, attempt 2 (Oct 27 14:25)
- `transaction.edn.deferred-injury_c5606345….edn`: injury c560, attempt 1 (Oct 27 22:45)

In the disk projection, c560 records 2aef as its cause, and 2aef records c560 as its cause (ordinals 58 and 67).
The deferred 2aef file carries patch `02e9…`. The live 2aef transaction carries patch `87c1…`.

## How the code gets there

1. `injury-selection` (`meta_self_heal_production.clj:1052–1067`): `defer?` asks only whether
   the blocker is an open injury. It doesn't check whether the blocker already has its own
   `.deferred-` file, or whether that file names the injury being deferred.
2. `move-selected-transaction!` (`:1091–1105`) moves c560's transaction aside.
3. `route!` (`:1476–1491`) skips resumption for `:defer` and calls `prepare-repair` for 2aef.
4. `coordinator/prepare` (`:142–180`) reuses 2aef's active repair and attempt, but starts with
   `:receipts []`. Because there is no author receipt, `invoke-once` (`:195–202`) calls the author.

As a result, 2aef's earlier deferred transaction (patch `02e9`) stays on disk unused, and a new author call produces patch `87c1`.
This shows the path exists. It doesn't prove that this is the branch taken at 22:45.

## What the spec says

None of these sources gives a rule for a cycle:

- `holes/labs/A-self-heal.md` never mentions deferral. Step 7 says only that "no restart is
  taken when semantics are genuinely undecided".
- The M2d execution receipt says "A recorded other-injury cause defers the old transaction and prepares
  that injury." It says nothing about the case where that injury is itself deferred.
- F43 says that an injury failing because of another injury is requeued without spending its retry budget.
- `SelfHealRoute.lean:3297–3304` notes that requeues on other-injury causes are unbounded. It
  gives no scheduling rule for deferred transactions.
- `Requirements.lean` has no predicate about deferring injuries or transactions.

Restoring 2aef's deferred file on its own would keep its receipts. It would not break the
cycle, because c560 would still defer to 2aef and 2aef to c560.

## The question

When deferring would close a cycle (A defers to B, and B is already deferred to A), should the
router return a **typed held result** that names the members and their transaction pins (injury,
repair, attempt, patch), keeps every file, and dispatches no author? It would stay held until an
explicit disposition chooses which existing transaction resumes.

The alternative is a fixed tie-break rule (for example, the older deferral resumes first),
which keeps the machine moving without a disposition. Codex-1 and I lean towards the held
result, because step 7 rules out guessing when the semantics are undecided. Either way, we
need your call on which one.

---

## 2026-10-28, after codex-1's reply (job invoke-1793155407323-5704-464666b3)

codex-1 agreed that a failure after a verified exercise of the retried operation cannot be its cause, but not that "passed the phase" means healed. It pointed out that `RetryClickOutcome.closing` in `DarkTower/WarMachine/SelfHealRoute.lean:1463` discharges only `successfulTaskOutcome` (`closing_dischargesOnlyOnSuccess`), and `otherInjury` stays inconclusive. Injury-local success needs a deliberate refinement of that outcome model.

What the receipts show for c560's retry `8c8ee35f` (started 16:38:29):
- `:ticket-readiness-refresh` did dispatch: Agency job `invoke-1793119192416-5620-354be23f` (16:39:52), harness execution-id = the run id, source-ref = the click id. `dispatch!` (`full_loop_runner.clj:2010`) throws `:wm-job-ledger-unavailable` before the POST when the ledger is absent, so the ledger context was present. The operation c560 failed on was exercised and got through.
- c560's repair (`6f1f8543c`) did not cause that. It adds the same refusal earlier, in `strategic-selection!`; it supplies no ledger. In `8c8ee35f` the ledger was simply there. Why it was absent in `1586bf55` is still unexplained, so c560 is not shown healed.
- The dispatched readiness contact itself failed: provider refused, 1261 "Prompt exceeds max length" (27k tokens, seat wm-author). The phase still returned `:ok`, and the record says `:unknown :inputs-changed-during-contact`. The same refusal hit both other readiness contacts on 2026-10-27 (14:26, 22:46). Readiness refresh has not produced an assessment in any of them, and the reason is recorded wrongly.

### 2026-10-28 — Joe's framing for item 3, and dispatch of item 1

Joe approved the three-item plan with this remark on the cycle: "Either its preferences don't care about the difference, or it doesn't expect to learn anything different from one versus the other. So the fix is to give it richer preferences or a sharper observation model, not a tie-breaker."

Applied to item 3: the outcome model currently maps "earlier failure blocked the retry", "injury's own operation shown working, then a later failure" and "not enough evidence" to the same observation (inconclusive), so the selector predicts the same result from re-queueing as from moving on, and it cycles. The fix is to make those observations distinct (sharper observation model), and to state which one the selector prefers to reach (richer preferences), not to add an ordering rule that picks one of the tied options.

Item 1 (why click 1586bf55 had no job ledger in its opts at readiness) dispatched to codex-1 as job invoke-1793155900660-5711-fbb5669e, park park-68c928b0-d0ea-44ad-9f3c-cf03e0e71948. Packet: ~/notes/packets/c560-ledger-discovery-2026-10-28.md. Discovery only.

### 2026-10-28 — correction to the item-3 framing (claude-11)

Joe asked what the tie was and whether it had a rationale. It had none. `injury-selection` (`meta_self_heal_production.clj:1044–1067`) is a `cond`: if the transaction's injury has a recorded blocker that is still open, defer to the blocker. The blocker comes from `other-open-injury`, which is co-occurrence in one run record. Nothing is scored, nothing is preferred, nothing is predicted. Describing this as "the selector expects the same outcome either way" (the item-3 framing above) was wrong: there is no selector here. Joe's standing instruction is that outer-loop selection should be a policy that mirrors Cascade Live. Item 3 is therefore to replace this rule with that policy over the open injuries, with "must reach own failure stage first" as the dependency evidence, not to refine the outcome labels inside the rule.
