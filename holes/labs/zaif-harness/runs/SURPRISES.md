# Surprise ledger — what the machine did that we didn't tell it to do

Started 2026-09-02 (Joe: "we should keep track of all the surprises we are
encountering, because I am not sure which of these behaviors are justified").
One entry per surprise: what we expected, what happened, the mechanism as
verified (not as guessed), and a verdict — JUSTIFIED (designed behaviour,
working), DEFECT (fixed or rowed), or DESIGN QUESTION (Joe rules). New
surprises append here; an entry may not be closed without its mechanism read
in code or record.

## The frame for all of them: why this machine is not the APM machine

The APM conductor SUPPLIES the frame and the solver works it — there is no
selection layer to disagree with, so it "does what we tell it" by
construction. The WM's outer loop IS a selection layer: weighing and choosing
is its designed job, and the operator's influence enters through declared
channels (mission docs, gates, the strategy cascade, weights, the clock) —
not through the machine reading intent. What today exposed is that those
channels currently give the operator a VETO (a `**Gate:** operator-…` line
zeroes doability) and a THUMB ON THE SCALE (spine, weights), but no BINDING
"work this now" pin: the clock read-half surfaces the focus and selection
deliberately does not consume it yet (that non-consumption is pinned by
test). So "it hasn't complied twice" decomposes below into one defect and
one working-as-designed — and the missing compliance channel is a build we
can name (see Next, at the end).

## Entries

### 1. Run 1 chose M-wm-aif-policy-grain-compliance while its own ranking said M-zaif-harness-v1 — VERDICT: DEFECT (fixed same day)

Expected: the tick selects its controller head. Observed: chose controller
rank 124. Mechanism, verified in code: the diagnostic stub, NAMED
"first-ranked-authorized-mission", actually took `(first
scheduler-habit-ranking)` — a third ranking channel — and the trace then
stamped `:selection-law {... :moved-from-controller-head? false}` derived
from nothing. Two defects: a mislabeled selector and a false record. Fixed:
`e1f97d6` (stub takes the controller head, is named `stub:controller-head`;
the law is derived from the actual chosen-vs-controller comparison, with
`:consulted-ranking`).

### 2. Run 3 demoted zaif from rank 1 to rank 5 and chose M-expressions-of-interest — VERDICT: JUSTIFIED MECHANISM, with a DESIGN QUESTION for Joe

Expected: zaif stays selected. Observed: fell to 5, the old incumbent chosen
(honestly stamped, this time). Mechanism, VERIFIED in code + record
(war_machine.clj `previous-selection-non-progress?`; run 2's record): run 2
selected zaif, its `:mu-pre` = `:mu-post` (both nil) so belief did not move,
no `:grounded-change` outcome — so the repeat-selection decay fired. That is
the designed explore behaviour: stop re-choosing a mission nobody works.
THE DESIGN QUESTION: the predicate assumes every tick could have produced a
grounded change, but a judge-only tick flies no flights — so under
judge-only ticks, ANY selection decays on the very next tick, and no mission
can stay focused. Candidate fix: decay only when an enactment was attempted
(or when the clock shows the focus held for a full-loop tick). Joe rules
whether that is a change worth a row.

### 3. Run 3's law stamp said the head "moved" when it was the head — VERDICT: DEFECT (fixed)

Mechanism: full action-map equality between the selector's minimal map and
the ranking's enriched map. Same trap as the `rank/N` F_pi join. Fixed same
hour: compare by `policy-key` identity; absent-from-ranking is the explicit
`:not-in-controller-ranking`.

### 4. The machine's own full law ranked zaif FIRST — a surprise in our favour — VERDICT: JUSTIFIED

The hand step-throughs measured only the step-⑬ mission-value blend and
predicted a 0.036 shortfall on centrality; the full controller law (G terms
included) put zaif at rank 1 of 146. The declared inputs (registration,
phase, spine, weights) carried all the way through. Nothing to fix; recorded
because our probe UNDERestimated compliance and that is a surprise too.

### 5. Tick 1 read its own focus as :no-active-clock — NOT a surprise once read

READ precedes the tick's own write, by design; the next tick knew. Recorded
so nobody re-diagnoses it.

### 6. The tail-eater filed its first issue off our punch-in ticks — VERDICT: JUSTIFIED (the C130 design working)

All three ticks emitted the typed refusal `:prediction-error/v1
:source-field-missing` on `:sorry-count-norm` (`:observed` absent);
threshold 3 crossed; sweep 132 proposed a row and minted a quarantined draft
pattern. Minted as wm-contract :I6 (the loop is working it now). Whether the
REFUSAL ITSELF marks a caller defect (the on-demand tick doesn't gather the
sorry census) or a producer defect is exactly I6's question.

### 7. The live selector can never resolve in an on-demand tick — VERDICT: DESIGN GAP (known, unrowed)

futon3c is not on futon2's process classpath, so
`live-wm-selection/validated-selection` always falls to the stub, and the
resolution error is persisted nowhere (codex-20's part-3 finding). Every
on-demand selection to date has been stub-selected. Should be a row when we
next touch the tick machinery.

### 8. zaif's gamma is read on every decision and enters none of them — VERDICT: DEFECT in the caller (unrowed on the zaif board; U6 finding, review-confirmed)

Expected: the burned-in B1 gamma cell (0.7071 for M-futon-forward-model)
shapes act-value. Observed, measured over all 114 recorded sessions and
reproduced byte-identically by the U6 reviewer: the live hydrator sets
:task-belief {} unconditionally (zaif_inputs.clj L190-L194), so act-value's
belief term is 0.0 on every session and gamma multiplies zero. "Recorded but
never consulted" one level down: read into a product with a structurally
absent factor. Fix direction is the hydrator (supply the belief), not the
controller.

### 9. 83 of 114 live zaif decisions were settled by tie order, not by a score — VERDICT: DEFECT-shaped design gap (review-confirmed)

With :act and :yield both structurally 0.0, they tie at the maximum whenever
:retrieve and :ask score <= 0, and choose-arm's case order takes :act. Only
the 31 :retrieve choices were settled by arithmetic. The crew's shipped
controller mostly decides by accident of ordering — same family as ZU-2's
":ask is structurally unreachable", now with the denominator. Feeds U7/U9
and the harness-v1 R7/R4 work.

### Process surprises (the loops, briefly, for completeness)

The three loop stops today were all the pre-flight discipline working:
tracked live-log (fixed: untracked), harvester header churn (fixed: no-op
guard), my own staged receipt (fixed: unstaged, and path-scoped commits
adopted). Plus one commit collision (206bb02 carries two authors' changes;
message says so).

Second collision, 2026-09-02 evening, a NEW mechanism the morning's
path-scoping does not stop: claude-1 ran `git commit --amend` on what it
assumed was its own U14-U17 mint (821980d), but claude-2's lane had landed
the D11 z1_views predicate fix (b8c4463) at HEAD seconds earlier — the
amend REWROTE claude-2's commit as 33f7a46, folding claude-1's U16 reshape
(worklist.edn, 8 lines) into it under claude-2's message. Content intact;
b8c4463 dangling (reflog-reachable); disclosure could not be amended into
33f7a46's message because HEAD moved AGAIN (d0ef0ae) before the fix — the
verify-HEAD-first guard caught that one. Lesson adopted both lanes: in a
shared tree, NEVER --amend; corrections are follow-up commits. Path-scoping
protects what you sweep IN; only never-amend protects what you rewrite.

## Next (the recommendation this ledger feeds)

Lining up the inner loop and trying again is right, in this order: let the
wm loop finish I6 and U6's review; then the one design pass that turns
"do what I told you" into a REAL channel — S4-remainder, where the clocked
mission parameterizes G and an operator-punched clock edge (witness rule
"operator-declared") becomes the binding focus pin the selector honors,
with entry 2's decay question answered in the same design; then re-punch
and fly ONE flight under the clocked mission. Compliance should come from a
declared channel the machine honors, not from us out-weighing its habits.
