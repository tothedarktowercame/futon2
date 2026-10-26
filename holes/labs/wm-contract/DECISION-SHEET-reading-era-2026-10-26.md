# Decision sheet: reading-era defect list, 2026-10-26

Written by claude-9, 2026-10-26 08:05Z, from
`~/notes/wm-next/outer-selection/DEFECTS-reading-era.md`. Four items came off
that list as questions no standing principle settles. Each has a
recommendation; an item without one would stay open (sheet of 2026-09-27).
This sheet takes no ruling. Every R line on the defect list is
REPAIRED-FORWARD or NOT A DEFECT; none of these four blocks a click. Item 1
blocks pattern learning from the rows R8/R8a now produce.

## 1. R8b: a learning contract for pattern-domain observations

**Question.** May the attempt-learning contract take per-occurrence pattern
observations as theta evidence? `resources/wm/attempt-learning-contract-v2.edn`
is signed under your PROOF-wm-works ruling (27f5b8d8) and names
`:observation-schema :wm/d-task-token-observations-v2` and trial grain
`:selected-cascade-effect-attempt`. Since R8 and R8a (futon2 `8f0191a91`,
`173e4b091`) `retained/token-outcome.edn` also carries `:pattern-observations`
(one row per wanted pattern-done token: occurrence, effect, after revision,
`:observed` a boolean or a typed absence) and `:pattern-comparison`. The
learning step reads only the mission comparison, so every pattern row is
held, as the 27 trials at attempt-002 were held `:observation-missing`.

**Options.**
- (a) Contract v3 under the same authority basis: adds the pattern
  observation schema; consumption is one theta update per
  `:predicted-and-observed` occurrence for the occurrence's own pattern key
  (the Lean `admitted_occurrences_pool_by_pattern` in
  `PatternObservationAdmission.lean`); typed-missing rows never update.
  Mission-domain consumption unchanged from v2.
- (b) Leave v2; pattern rows stay retained evidence that nothing consumes.

**Recommendation.** (a). The rows exist, the Lean admission bridge for them
exists, and the only thing missing is the signature. Without it, the R8
defect persists at the learning step even though the producer is repaired.
What (a) does not do: it does not make a prose check count as evidence; at
attempt-002 all 27 checks are prose, so v3 changes nothing until an
authored clause carries the locator form.

**If yes.** claude-9 writes v3 as a copy of v2 with the two added keys and
your signature line, and a packet wires `attempt_learning` to read
`:pattern-comparison`. Not a technical choice to make without you: v2's
`:authority :declared` names your signature as the basis.

## 2. R5 residual: the task-level outer action and the retained menu's action pin

**Question.** Ruled Q2 (codex-68 2026-10-16, option (c)) gives each
Q8-eligible task one task-level outer action, and the receipt conforms. The
retained menu's rows also carry an action pin. No ruling relates the two.
Should one?

**Options.**
- (a) Record only: the chosen task's outer action carries the menu row's
  action pin as provenance; no constraint, no refusal.
- (b) Constrain: the task-level outer action must equal the pin of the menu
  row that selected it; mismatch is a typed refusal.
- (c) Leave unrelated.

**Recommendation.** (a). It matches your 2026-09-19 line (nothing halts WM;
validation wanted, guards not), and it makes the relation observable on the
next click without a new required field. (b) would be a new refusal with no
case behind it. (c) leaves the question to come back on the next receipt.

## 3. U4: partial learning-authority evidence and the canonical choice

**Question.** With a declaration missing one of the two menu policies,
`select-live` records the evidence as `:partial`, names the dropped digest,
and still makes a canonical selection (O3-2, futon2 `218f9f2ba`, no ruling
cited). Before O3-2 a test pinned a refusal; the namespace was red across
the change, so neither behaviour was ever pinned at HEAD. Does the outer
choice require complete evidence over the sealed menu?

**Options.**
- (a) Evidence quality is a recorded fact beside the choice (today's
  behaviour); pin it with a test and a Lean clause.
- (b) Complete evidence is required; partial evidence refuses the click.

**Recommendation.** (a), on the same 2026-09-19 line as item 2. The dropped
digest is already named in the record, so a reviewer can see what the choice
rested on. (b) would have refused every click whose declaration is short a
policy, which is a guard.

## 4. U5: retraction policies are scored but can never bind

**Question.** `retraction_cascade/from-retraction` builds nodes without
occurrence identity, so every retraction policy is refused
`:occurrence-identity-absent` before any structural build, on every click
(9 of 9 on 2026-10-24), after being constructed, deduplicated and scored,
and after counting in the family's `:distinct-count`. Is a retraction policy
meant to be selectable on the canonical route?

**Options.**
- (a) Yes: define occurrence identity for a retraction (it cites no
  fragment, so the identity would be new design).
- (b) No: stop scoring them on the canonical route and report family counts
  without them.
- (c) Leave as is: priced on every click, never choosable.

**Recommendation.** (b) now. It removes a cost the machine pays for a
choice it cannot make and makes the family counts say what is selectable.
(a) is the larger design and can be reopened if a retraction is ever wanted
on the canonical route; nothing in (b) forecloses it.

## Not on this sheet
- R10 (serving drift refusal): a check to keep, not a question.
- U6, U7 (red tests off the click route) and U8 (string keys in
  `wm_run_alert.py` output): technical, handled in lane.
- The R8a Lean instance: dispatched to claude-5 2026-10-26 08:03Z.
