# Model-choice sitting — 2026-09-09

Joe with codex-17, emacs-repl. Rulings are recorded verbatim; registry and
worklist integration belongs to claude-1. No live wiring changes in this note.

## 1. pi-zero-form: habit in both

Joe, verbatim:

> Yes, habit in both makes sense in this case. Naturally, as time goes by, we could tune the way in which habits are recorded and how they're referred or deferred to, or even how they're changed or forgotten. But that won't work unless we have them present.

Interpretation: adopt :habit-prior-in-both for :pi-zero-form. Both pi and
pi-zero include ln E; their difference measures the additional F_pi effect
conditional on the same habit prior. This is an operator ruling, not a claim
that this arm was empirically shown to select better. Habit recording,
deference, modification and forgetting may be refined later; no particular
update/decay mechanism or parameter is selected by this ruling.

Basis: aif-equations.edn :choices :pi-zero-form (lines 347–391), including
the withdrawn equivalence claim and the U3 no-selection-discrimination result;
runs/E-live-strategic-discovery-2026-09-09.md, section 2. Solver implementation:
src/futon2/aif/policy_precision.clj:33-48. Live beta wiring remains a separate
implementation obligation. Item 22's grain separation and forward-only
accumulation remain in force; this does not promote tactical habit into the
strategic selector or replace its standing fixture E_S automatically.

Hierarchy, learning and policy depth remain pending discussion.
