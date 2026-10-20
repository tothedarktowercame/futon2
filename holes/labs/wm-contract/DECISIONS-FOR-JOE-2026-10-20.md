# Decisions for Joe — 2026-10-20 (claude-5, written 05:55Z)

Two items. Everything else in the self-heal lane is in motion under the standing
continue (M10b-4 next; see `logic-model/PACKETS-2026-10-19.md`). Each item says what
is asked, where it stands, the options, and one recommendation. Rulings deferred are
fine; nothing below blocks the code packets.

## State at the time of writing

- Lean `SelfHealRoute.lean` is at rev 10 (mathlib4 `darktower` `92c213cdd6`): the
  closing law is met in code (`closingToday_eq_closing`, rev 9) and the re-queue edge is
  met for every outcome that writes a `:retry-inconclusive` observation
  (`nextToday_eq_next_ofObserved`). Two outcomes still await forever: a retry that fails
  with a different injury, and a retry that left no record (`requeue_todayMissesUnobserved`).
  M10b-4 closes the first in `meta_terminal_injury.clj`; the second needs a decision in
  the coordinator, which I will make in lane and record.
- Code: futon2 `6308c946a` (ledger: `:retry-limit`, re-queued `:enacted`, `:abandoned
  :retry-budget-spent`), `f579f4ef8` (attributed observation, `pending-requeue`),
  `aa3ec313b` (coordinator re-queue edge), `493388b7b` (wiring); futon3c `e4f58cfc4`
  (`retry-click-id` by ordinal), `f2e5a9fa6` (retry port), `7efc13bd8` (runner wiring).
  All reviewed, each with a mutation run in a worktree.
- Live ledger `data/wm-meta-injuries/ledger.edn`: one injury open
  (`injury/d755c886a22d6fc8c04db7fada64089e07157f62`), `:attempt-limit 3`,
  `:retry-limit 3`, four abstention observations. No repair has been authored or enacted
  on it; no `transaction.edn` exists.
- Ordinary-click budget (live, `futon3c.wm.ordinary-click-budget/availability`, 05:52Z):
  128 allocated, 128 consumed, 0 available. Clicks have been paused at 0 since 2026-10-19
  while the route was formalised and the code brought to the law.

---

## 1. The first coordination click's budget

**What is asked.** A grant of ordinary clicks so the self-heal route runs live on the open
injury. Under your rule a grant is your words in the buffer; I then raise `allocated` in
`futon3c/src/futon3c/wm/ordinary_click_budget.clj`, commit, and reload.

**What one click does now.** The ordinary click reaches the self-heal route, the
coordinator prepares, authors and enacts a repair for `d755c886…`, and queues a retry
child (`wm-retry-<repair-id>-1`). Since 10-15 every ordinary click has abstained, so the
retry abstains; `integrate` writes the attributed `:retry-inconclusive` observation; the
next ordinary click finds it through `pending-requeue` and re-queues a fresh child
(`wm-requeue-1-1-<repair-id>`), and so on to `:requeues 3`, when the ledger records
`:abandoned :retry-budget-spent` and the coordinator stops with no port call. That path is
proved in Lean and tested in code, and no live record of it exists yet (rev 10 hole 2).

**What is not settled before the clicks run.**
- M10b-4 (in lane, next): if the retry fails with a *different* injury, today the
  coordinator awaits forever on `d755c886…`. Under the abstention regime this is unlikely
  on the first clicks; I will land M10b-4 before spending any grant unless you say
  otherwise.
- Whether the retry child draws on the same ordinary-click ration as the coordination
  click. I have not read that path this session; the counts below assume it does.

**Options.**
- (i) 0: stay paused until M10b-4 and the second-outcome decision land. Costs the record
  witness another day.
- (ii) 2 clicks: one coordination click and one re-queue. Produces the first
  `:retry-requeue-1` receipt, the missing record witness for rev 10, and stops.
- (iii) 8 clicks: the whole path on one injury (enact + retry, three re-queues with their
  children) to `:abandoned :retry-budget-spent`, with one spare for a repair between
  clicks.
- (iv) 10 overnight at the lane's discretion, as on 2026-09-24.

**Recommendation: (iii), after M10b-4 lands.** It is the first time the ledger's
`:retry-limit` and the typed abandonment are exercised live, and the Lean's hole list
cannot shrink on fewer clicks. (ii) if you want to look at the first receipt before the
rest runs.

---

## 2. claude-2's anchoring pass on `SelfHealRoute.lean`

**What is asked.** Whether claude-2 spends revisions on anchoring the Lean's constructors
to the code by function name and keyword rather than by line (the offer in its M6L
bell-back, answered in the M11L packet's last section).

**The offer.** Every constructor gets `(T classify :ordinary-selection-abstention)`-style
anchors in its docstring; success states are anchored to their `{:status ...}` sites, with
the branch condition where one function emits the same status twice; a state with no
producing site is then a visible hole in the encoding, which is the measure you asked
for. `align.py` resolves each pair to today's line and reports drift instead of comparing
hand-written numbers. One state family per revision, `RouteState` first; no constructor,
edge or theorem changes in an anchoring revision; claude-2 writes, I review.

**Why it is on your sheet.** It is work on the Lean that adds no theorem, so it is a
choice of what claude-2's revisions go to while the code packets run. Rev 7 and rev 8
each spent part of a revision correcting line cites that had drifted; rev 10 cites about
forty lines across five files, all of which will drift at the next edit to any of them.

**Options.**
- (i) Go now: `RouteState` first, one family per revision, interleaved with the code
  packets (claude-2 is idle between Lean revisions).
- (ii) Go after the first live run (item 1), so the record witness comes first.
- (iii) Decline; keep line cites and correct them by hand each revision.

**Recommendation: (i).** If you would rather not rule on this, I will treat it as a lane
choice under the standing continue and dispatch (i) after M10b-4.

---

Not on the sheet (technical, proceeding in lane): M10b-4; the coordinator's treatment of
a retry click with no record; whether `retriesSpent` stays a Bool in the model (rev 10
hole 3).
