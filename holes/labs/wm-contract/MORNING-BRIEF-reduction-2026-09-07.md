# Morning-brief decision queue, reduced to four rulings (2026-09-07)

Joe, 2026-09-07: "I do not have time or interest to judge 73 attempts …
we need to reduce all of that down to something that I can actually wrap
my head around." This sheet is that reduction. The 73 pending items in
`data/wm-morning-brief/items/` cluster into four classes by their own
recorded fields (outcome, `:resolved?`/`:dial-moved?` witness state, and
whether the item is a real attempt or a synthetic canary/initialization
probe). **Four class-level rulings below replace ~300 per-attempt
judgment cells.** Once ruled, the steward applies each ruling per-attempt
as `reviews/mbqa-*` records citing this sheet and your imprimatur — the
precedent is attempt-035's existing review ("Recorded by claude-1 with
Joe's imprimatur").

The items are July-era (queued from 2026-07-14) wm-full-loop
implementation attempts — the same corpus the D1 KL worked example
walked. Judging class A also enriches the F10 outcome-domain evidence.

## Class A — 21 witness-bearing successes (14 :grounded-change, 7 :ok,
all with :resolved? true and :dial-moved? true)

These carry an implementation commit, a named reviewer, and a discharge
record. Two are canaries (76c0d5df, 79086952) and one (attempt-035) you
already reviewed in July, leaving **18 real, unjudged**. This class is
the 15 belief-learning-blocked items' home: their belief events cannot
land until `substantive-achievement` is judged.

**Proposed ruling A:** accept the class wherever the steward's audit
confirms (i) the commit exists in the named repository, (ii) the recorded
review job completed, and (iii) the target was not superseded since July.
Attempts failing any check go on an exception list for you — the steward
presents exceptions, never the whole class. Belief events land only for
audited passes.

## Class B — 28 :ok without witness fields

Outcome :ok but no :resolved?/:dial-moved? record — older-schema ticks
with nothing for a belief event to bind to.

**Proposed ruling B:** steward audits each for a recoverable witness; any
attempt with none is closed as `:audited-no-witness`, judged
`substantive-achievement: none` (no belief event), recorded and counted —
not silently dropped. Recoverable ones migrate to class A treatment.

## Class C — 15 :build-failed (7 real, 8 canary)

Honest negatives. They already serve as evidence (they are the
`:build-failed` mass in the D1 Q(o|π) table).

**Proposed ruling C:** close as recorded honest negatives; no belief
event; no per-attempt judgment. Canaries additionally marked as probes.

## Class D — 9 infrastructure/misc (:agent-unavailable, :agent-job-stalled,
:agent-job-timeout, :substrate-unavailable, :incomplete, 1 bulletin)

Mostly canary/initialization records; none carries judgeable work.

**Proposed ruling D:** close as environmental, no judgment content;
the bulletin record routed to its own channel if it is not an attempt.

## What you actually decide

Four accept/amend calls on the rulings above (plus, later, the short
exception list ruling A generates). Everything else is steward work:
audit, apply, record, and keep the queue at zero as new items arrive.
The steward never presents you a raw queue again — a growing backlog in
this register becomes an exception sheet, bounded by construction.

Full per-attempt class lists: derivable from `items/` by the recorded
fields above; the steward's first receipt will pin them attempt by
attempt with commit shas, so the sheet stays small here.

## RULED AND APPLIED (same day)

Joe, in session: "I agree with all of your suggestions. So basically we
can deal with all 73 of those items in this turn because I accept your
proposed strategy for processing them."

**One correction found during application, stated before the numbers.**
This sheet's class B ("28 :ok without witness") was a misparse: my
first-pass regex matched `:outcome :ok` inside nested witness blobs, not
the items' own top-level outcome. Read through the reader's actual keys,
no item has outcome `:ok`. True populations: **A = 21** (all
grounded-change, `:witness :resolved? true`), **C = 29** build-failed,
**D = 23** infra/canary/bulletin, **B = 0**. Every item still falls under
an accepted ruling (the phantom-B items are real C or D members), so no
new ruling was needed; recorded here so the sheet does not overclaim.

Application (all through `futon2.aif.morning-brief/review!` — validated
answers, deterministic review-ids, immutable CREATE_NEW files; nothing
hand-written):

- **271 review records** written to `data/wm-morning-brief/reviews/`,
  reviewer `joe`, each note carrying the class rationale + "Recorded by
  claude-1 with Joe's imprimatur", answers per class:
  A: feature-verdict `:accept-feature` (commit audited present in its
  repo — all 21 found, zero exceptions), substantive-achievement `:yes`,
  evidence-sufficiency `:sufficient`, selection-quality `:uncertain`
  (honest: no July calibration basis).
  C: feature-verdict `:reject`, substantive-achievement `:no`,
  machine-response `:correct` (failure honestly recorded),
  evidence-sufficiency `:sufficient` (the failure is fully evidenced).
  D: feature-verdict `:reject` (nothing completed to accept),
  substantive-achievement `:uncertain`, evidence-sufficiency
  `:insufficient`, machine-response `:correct`.
- **15 belief events minted, all `:strengthened`, all class A** — exactly
  the belief-blocked set; C and D carry no entity targets so mint none,
  as ruled. Events land at the war machine's next tick via
  `unseen-belief-events`.
- **Pending after: 0.** The DECISION-DUE component clears at the next
  status run. The review files are machine-state (`data/` untracked,
  append-only); this section is the committed record.
