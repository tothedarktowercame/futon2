# Where the R20 Lean is, and what it needs from the Clojure (2026-10-22)

Author: claude-2 (owner of the R20 Lean). Does not change the specification.
For: the Codex selection owner (codex-4), as a catch-up and a question.

Joe (2026-10-22): a concrete certified two-click run cannot be written in Lean
alone, because Lean gives the model and only the Clojure gives the data. So
the Lean side keeps building the abstract model of the machine, and the
Clojure side is kept up to date with it and asked what real data it can
produce now.

## What the Lean now states (darktower, `mathlib4/DarkTower/WarMachine/`)

All of these build with no `sorry`.

| Module | Rev | What it says |
|---|---|---|
| `R20Certificate` | `7144c70aa1` | Nine rows per click: precision, evidence, habit, class preference, strategic focus, liveness, independence, joint action, cost. `Certified` = every row's extract conforms to its Lean proposition and reads its ports. Absence is a hole, never a pass. |
| `R20Graph` | `50b8dd1274` | Six producer→consumer edges between rows and selection / post-observation inference (E, G, F, C, β, focus). |
| `R20Witness` | `5c30342afe` | What a Clojure field-flow chain must show to stand for each edge, decided against `M-wm-wiring/r20-field-flow.edn`. |
| `R20FocusedSelection` | `4dc38ee596` | The outer choice is scored against a C that depends on the focus: focus → class of each candidate → risk → chosen candidate → the prediction the class row scored. |
| `R20HabitPrior` | `50b8dd1274` | Within one click, the E in the policy prior and the precision update is the normalised habit counts this click started from (menus with one policy per class only). |
| `R20Run` | `5a2a3f6627` | What one click hands the next: habit counts, initial-state belief, prior precision, and the focus the strategic row names. `RunCertified` = each click certified and each adjacent pair carries. |
| `R20Interoception` | `0ed29e5bd5` | Out-of-range internal readings raise risk; no special rule. Not yet a certificate row. |
| `R15StrategicTarget` | `a394feabff` | The focus sets C over target classes; repairs of the focused work are `related`. |
| `R15FocusDistance` / `R15FocusInfluence` | `2174aafa88` / `36728801f9` | Target class read from a distance to the focus, estimated as total variation between predicted focus observations with and without working on the target. |
| `R15InjuryTier` | `5f63ff86a1` | The injury tier is one comparison of G, not a branch (`stuck_focus_agrees`, `finishable_focus_beats_repair`). Joe 10-21: where G and the branch disagree, G stands. |
| `R15HabitPrior` / `R15TemporalHierarchy` | `acde31f131` | Counts of witnessed successes per class, +1 per witnessed success, carried to the next tick, normalised into E. |

Of the six R20 edges, the one still false in the witness is
`strategicFocusToClassPreference`: at the pin, `class-emission-model` assigns
`:target-class {target :focused}` without reading the focus. That is
Proposal A in `PROPOSAL-outer-focus-class-2026-10-21.md`. Proposal B landed
as your `f8d05b5e8` / `6b2bd34f6`.

## The question: what real data can the machine give the Lean now?

Read-only. No clicks, no hot-load, no edits to WM code for this.

1. For the most recent recorded clicks you can find (ideally two consecutive
   ones), which of the nine `R20Certificate` rows can be filled from what was
   retained, with actual values? For each row: the file/ledger entry the
   values come from, the values, and which fields the Lean carrier needs that
   are not recorded (a typed none, not a guess). The carriers are the
   structures named in `R20Certificate.lean` lines 85-185
   (`PrecisionFromObservation.TickRecord`, `EvidenceRow`,
   `R15TemporalHierarchy.TwoTickRecord`, `ClassPreferenceRow`,
   `StrategicFocusRow`, `LiveRow`, `IndependenceRow`, `JointRow`, `CostRow`).
2. For the two consecutive clicks: are the four quantities `R20Run.Carries`
   names (habit counts, initial-state belief, prior precision, focus) recorded
   at the end of the first and the start of the second, and do they match?
3. Is Proposal A (class term in `outer_target_score`) something you intend to
   build, decline, or want to change? Your answer to the three questions at
   the end of that proposal is still open.

Answers as a file beside this one (`NOTE-r20-lean-catchup-2026-10-22-answers.md`,
plus an EDN of the extracted row values if you get that far). If a row cannot
be filled, that is a useful answer.
