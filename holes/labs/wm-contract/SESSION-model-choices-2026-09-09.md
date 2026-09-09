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

## 2. Hierarchy: single-level for RUN4, with follow-on provisos

Joe, verbatim:

> So, that's fine for run 4, I suppose, to some extent, however. We had previously thought of the outer loop. As having its own AIF, and the inner loop is having its own AIF, and now we have also started to define the ZIF harness so we could get agents working. With an AIF backing as well. However, I don't think any of that's strictly needed for Run 4. As long as we're mainly just getting run 4 to exercise the inner loop, or else if we're conceiving of the outer loop, we're going to use the inner loop. And the inner loop is all part of one AIF system rather than a hierarchical model. So, in any case, we'll get the hierarchical model in due course with the Zaif agents. But... For now. We can do whatever makes sense for Run 4 to get it moving again with these provisos in place. For follow-on actions.

Interpretation: accept the currently single-level prediction model for RUN4's
inner-loop exercise. If outer-loop work is involved in this run, it uses the
inner loop; that does not establish a coupled generative hierarchy. This is an
era/run-scoped disposition of :hierarchy, not a permanent single-level design
or a claim that all of the stack is one AIF system.

Preserve follow-on actions: the outer loop's own AIF, the inner loop's own AIF,
and AIF-backed agents through the Zaif harness must be accounted for in the
future hierarchical model. The ruling does not specify their probabilistic
coupling, timescales or implementation, and does not claim they are already
wired. These follow-ons do not gate RUN4. Preserve both "ZIF" and "Zaif" in
the verbatim text; the existing project reference is P-validated-R5.md:722-727
(M-zaif-harness), rather than creating a new system name from transcription.

Observed implementation basis: aif-equations.edn :choices :hierarchy;
src/futon2/aif/rollout.clj:474-479 explicitly describes flat temporal rollout.
No registry, code, run configuration or live process changes in this record.
Learning and policy depth remain pending discussion; the earlier pending list
above records the state before this ruling.

## 3. Learning: no ruling; theory and implementation investigation requested

Joe, verbatim:

> So, here I think I need to ask some more questions going back to the... Work we did on the PLOP paper because we'd actually thought a lot about learning. We tried something with G-Flow Nets. We had a slush. Presumably, we're going to update the cascades with some evidence based on how they're used. So, I'm not 100% confident with keeping structure learning offline. Even if it's in a later phase of the system. In particular, we're trying to make a... A.I.F. compliant model. And if we're saying, well, for run four, we're just not going to do this essential part of the model. Then I'm not totally comfortable with that, if AIF is assuming that we're doing learning in an online fashion. So... I'm not prepared yet to say one way or another about this until I know more about what AIF itself is asking of us here, firstly, until I think about what we had already done in the PLOP 2026 era, secondly, because I wouldn't want to be doing something worse than that. And I guess whether it's online or offline, I'd like a bit more specificity about what exactly learning is going to be looking like in this implementation. And how that's complying with the AIF model.

No adoption of offline-only learning is inferred. The decision remains open
because Joe requests the theory, PLOP-era evidence and concrete update/consumer
account before deciding. See runs/LEARNING-theory-history-runtime-2026-09-09.md.


## 4. Learning scope corrected: AIF-compatible implementation, history as inspiration

Joe, emacs-repl, 2026-09-09, verbatim:

> So I don't think we need to recover all of the claims from the PLOP paper, especially given that the submitted version didn't go through all of the different rotations and permutations of learning. What I think we need to do is create basically an AIF-compliant claim. Version. And I don't think, again, that AIF is just saying that there's one learning phase that happens. All at the end. I think we can get inspired by the earlier work. And build an implementation that is AIF-compatible and... Go from there.

This corrects the recovery-spec interpretation in b6f4040e: LR1–LR7 are a
source of mechanisms, evidence and possible tests, not a required restoration
checklist or an adopted RUN4 gate set. The implementation target is a stated,
validated AIF-compatible learning model. Historical variants need not all be
implemented; their negative findings remain evidence, not requirements to
reproduce the failed behavior.

The earlier observation that trial-boundary updates are permitted was not a
prescription for one terminal learning phase. It must not be read that way.
Proposed next design separates current-state inference, evidence-driven model
parameter updates, and slower structural/proposal revisions. For each actually
implemented update, name the model variable, admissible evidence, objective or
update equation, timing, persisted state and subsequent consumer. AIF mapping
must be justified for that specific computation; attaching AIF vocabulary to a
reward fit or habit counter is insufficient. The choice of exact update schedule
and which structural/proposal mechanisms to include remains design work.

No offline-only arm, mandatory GFN/BMR combination, or automatic post-RUN4
deferral is inferred. No final :learning arm or compliance result is recorded;
Joe has set the design direction, not supplied all its equations. Registry and
worklist integration belongs to claude-1. No production changes in this note.
