# Excursion: hierarchical preferences, where a higher level sets C (E-hierarchical-preferences)

**Date:** 2026-10-28 · **Status:** HELD. Written down so it is not lost; not part of the R20 step rebuild.
**Authored by:** claude-11, at Joe's request ("write down the hierarchical preferences extension that we talked about").
**Relates:** [[SPEC-r20-step-function-2026-10-28]] (section "Preferences C are learned", paragraph "Later upgrade") · [[E-C-vector-live]] · [[M-G-over-cascades]] · pattern `aif/hierarchical-and-temporal-depth` · problem node `problems/r15-hierarchy-and-timescale`.
**Lean:** `mathlib4/DarkTower/WarMachine/R20Step.lean` (`learnedClassPreference`), `CTauClassPreference.lean` (`terminalPreference`, `classPreference`), `R15StrategicTarget.lean`.
**Keywords (for search):** hierarchical C, context-dependent preferences, empirical prior on C, higher level sets set-points, preference drift toward habit, contextualisation.

---

## Where this came from

On 2026-10-28 Joe asked whether the preferences C (the 55/35/5/5 kit over
ending classes) are fixed or learned. The literature has three answers
(claude-11 turn 214):

1. **Fixed C**, set by the modeller (Friston et al. 2017, process theory).
2. **Learned C**: Dirichlet counts `c`, updated from observed outcomes, C the
   normalised `c` (Smith, Friston & Whyte 2022 §4, "analogous" to Eqs. 34/37;
   written out in Sajid et al. 2021 and Sajid, Tigas et al. 2021 §4.1).
3. **Context-dependent C**: in a hierarchical model the level above supplies a
   lower level's preferences as its predictions, so C changes with context
   without being learned by counting (Pezzulo, Rigoli & Friston 2015).

Joe chose (2) for the rebuild because it can be cited as written and gets the
machine working, and asked that room be left to upgrade later (turn 215).
The spec records (3) as the later upgrade. This note is that upgrade, written
out far enough that someone can pick it up.

## What exists now

- **Learned flat C (R20 slice 1, 1b).** `State` carries counts `c` over the
  four terminal classes; prior `c₀ = κ·(55,35,5,5)/100`; update
  `c ← ω_c·c + η_c·onehot(ending)`. C at a click is `c / Σ c`, passed to
  `learnedClassPreference C horizon tau`. There is one `c` for the whole run.
- **The strategic focus already sets what the classes mean.**
  `R15StrategicTarget.lean` follows Parr, Pezzulo & Friston (2022), p. 204:
  higher levels set the lower level's set-points ("what they have to
  achieve") and leave it free to choose the means; p. 102 says goal selection
  (C) can depend on higher levels. Our reading there: the focus decides which
  class a reached target falls in (focused, related, unrelated). The
  distribution over the classes is still the single kit.

So the higher level today decides *which outcomes are focused*, but not *how
much focused outcomes are preferred over related ones*. The extension is the
second part.

## The extension

The higher level supplies the lower level's C. Concretely, C at a tactical
click is a function of the higher level's current state, not one global
vector:

    C_click = f(higher-level state)

Two forms are worth distinguishing, because the literature supports them to
different degrees:

- **(a) Higher-level outcome as an empirical prior on C.** The higher level's
  predicted outcome is the lower level's C (Pezzulo et al. 2015: "empirical
  priors in generative models correspond to 'drives' and 'goals'"; Parr et al.
  2022 p. 102). Nothing is counted at the lower level; C moves because the
  higher level's beliefs move. This is the version the sources describe.
- **(b) Context-indexed learned counts.** Keep one count vector per
  higher-level context, `c[ctx]`, update only the one for the context the click
  ran in, and set `C = Σ_ctx q(ctx) · normalise(c[ctx])`. This combines (2)
  and (3). We have **not** found a paper that writes this out; it would need a
  source before it goes in the spec.

Pezzulo et al. 2015 is conceptual ("successive hierarchical
contextualisations"); it gives no learning scheme for C. That is why the spec
cites it only for the idea.

## Why it is worth doing

- **Drift toward habit.** With counting alone, the agent comes to prefer the
  outcomes it already gets (claude-11 turn 214; the trade-off in the title of
  Sajid, Tigas et al. 2021). κ and ω_c set how fast. A higher level supplies
  preferences from somewhere other than the agent's own recent outcomes, which
  counting cannot do.
- **Moving set-points.** Pezzulo et al. 2015 (Discussion) argue that
  continually updated empirical priors supply "a 'moving target' for
  behaviour", which is their answer to the dark-room problem. For the WM, a
  mission that changes phase (exploring vs. closing out) plausibly wants a
  different balance between focused and related work.
- **Kit as inherited prior.** The same passage suggests "an initial set of
  priors might correspond to Pavlovian values shaped by evolution, while
  others are acquired by learning". That is a fair description of the 55/35/5/5
  kit plus learned counts, and of where a higher level would sit above them.

## How it maps onto the WM

- The higher level is the **outer loop**: the same loop as the inner one,
  with policies for selecting tasks, chosen from Cascade Live (Joe,
  2026-10-28; `futon3/library/cascades/select-tasks-from-cascade-live.flexiarg`).
  Its state is what that loop believes about the current mission and focus.
- Under (a), the outer loop's predicted outcome for the selected task would
  become the inner loop's C for the clicks working on it.
- This must not reintroduce inner policies before selection: the outer level
  sets C for the task it has already selected; it does not score inner plans
  for unselected tasks.

## Seams already left for it

- `learnedClassPreference` takes C as an argument, so a C computed from the
  higher level can be passed in without changing `classPreference`'s shape.
- The spec keeps the C update as one named function in `Makeup`, so a
  context-indexed rule (b) can replace the flat one.
- Theorems about C are stated for every makeup; a hierarchical version would
  restate `click_c_at` with the higher-level state as an extra input.

## Open before building

1. A source that gives a learning (or inference) rule for a C supplied from
   above. Candidates to read, not yet checked: Friston, Rosch, Parr, Price &
   Bowman (2017), "Deep temporal models and active inference" (Neurosci.
   Biobehav. Rev.), which may link levels through initial states D rather than
   through C; Parr, Pezzulo & Friston (2022) ch. 7 on hierarchical models.
   If the sources link levels only through D, say so and decide whether C
   from above is still what we want.
2. What the higher level's state is, in terms of the outer loop's actual
   beliefs, and whether it exists in `State` yet.
3. Whether (a) or (b), given what (1) turns up.
4. How the existing `click_c_at` and `click_zero_C_is_kit` results carry over
   (they should be the special case of a single context).

Local copies of the papers: `~/notes/papers/` (`pezzulo2015`, `smith2022`,
`sajid`; not committed).
