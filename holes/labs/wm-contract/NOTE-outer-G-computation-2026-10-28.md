# How the task-selection G is computed (2026-10-28)

claude-11, after Joe's turns of 2026-10-28 (~15:40–16:10Z). Decisions recorded
here are Joe's unless marked otherwise.

## Rule

The outer loop is the inner loop with different policies, so it uses the same
G: `CascadeEFE.stepG q a c = risk (push q a) c + ambiguity q a`
(`mathlib4 DarkTower/WarMachine/CascadeEFE.lean`). Its model is the outer
level's own, built only from facts about tasks as wholes. No inner (tactical)
policy is built or read to score a task (Ryle's regress; Joe). Q4's "G is not
risk-only" applies to task selection in this sense only.

## The outer model

- **Policy**: a task-selection cascade (`R20Step.SelectionCascade`); applied
  to the observed field it picks one task k, or none.
- **Hidden state of k**: `doable | blocked`. Giraffe's "ready" says every
  recorded need is met; the record can be wrong or incomplete (external,
  undetermined, dangling, ruling needs), so readiness is evidence about the
  state, not the state.
- **Prior over the state, q**: from Dirichlet counts per Giraffe readiness
  status (`ready | notReady | undetermined`) over `doable | blocked`. The
  starting counts are a `Makeup` field (a starter kit, like C's 55/35/5/5);
  they are learned forward from this machine's own clicks. No retrospective
  reconstruction from old records (Joe: build the system now; adapt old data
  later if needed).
- **Observation**: the pair (ending class, items made ready). Items made
  ready comes from Giraffe's intervention: "k lands" sets k's needs true and
  the pass recomputes readiness (`futon3c/src/futon3c/giraffe/pass.clj:42`);
  the count of newly ready items is k's one-step `unblocks`.
- **Observation kernel, a**: if `doable`, (class from k's relation to the
  focus, unblocks k) with probability reach r, else (stopTheLine, 0); if
  `blocked`, (stopTheLine, 0). r is the candidate's `reach` (supplied from
  outside for now).
- **Preferences, C**: over the observation pair. Joe: C includes items made
  ready. The ending-class part is the learned C of slice 1b; the
  items-made-ready part is a second learned count vector with its own starter
  kit, favouring more. How the two combine (product or a joint table) is
  settled in Lean against Q9's ruling that a flat table is not a factorized
  preference.

## What each term then means

- **Risk** favours tasks likely to end in a preferred class and to make other
  work ready.
- **Ambiguity** is Σ_s q(s) H(a(·|s)): a doable task with r = 1/2 costs
  log 2; r = 1 costs nothing. Unreliable tasks pay for it even when ready.
- **Information gain** (in the equivalent pragmatic − information form,
  `CascadeEFE.step_decomposition`) is high where Giraffe cannot say whether a
  task is doable and attempting it would show which: `find-out-before-building`
  falls out of the formula.

## Learning

After the click, the observed pair updates: the ending-class counts (as now),
the items-made-ready counts, and the readiness-status → state counts by the
posterior over the hidden state given the observation (Dirichlet learning with
η, ω from `Makeup`; no hard-coded rates).

## Order of build

1. Lean: the outer model and score as `stepG` (slice 3d).
2. Lean: the three learned updates inside `step` (slice 3e).
3. Clojure follows (with the click route already taking a supplied task).
