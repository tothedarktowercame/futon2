# Ruling record: the enacted object is a cascade (claude-2, 2026-11-01, Joe's rulings of 11:48Z)

Joe, 2026-11-01, on the three L5 rulings I recommended: "your recommendations on the rulings
sound good to me, indeed for the first one, it is worth clarifying for the record how 'the
enacted object is a cascade' relates to the notion of a 'generative model' in AIF."

## The three rulings as accepted

1. **Inner selection law.** The chosen cascade is the strict G minimiser over the compared
   cascades (Q11 as stated in `Requirements.lean` at `55878e243a`). The first-action marginal
   in `policy.clj` (posterior summed by first action, then the winning action enacted) is
   retired for the War Machine's inner run, or confined to a setting where the enacted thing
   is a single action. Same-prediction variants are duplicates of one policy, not separate
   policies with separate votes. The record's `:decided-by :G` line must describe what ran.
2. **Repeated identical-input abstention on a task** is a selection-run violation: the work
   run's abstention stays a valid stop; the selection run that picks the same task again on
   unchanged inputs is what fails. Q6 is not extended to abstention; the field is not restricted.
3. **A policy excluded by Q6 before comparison** stays in the comparison, with its G and
   prediction; Q6 forbids choosing it. No typed-exclusion carrier; Q3 and Q11's equalities hold
   because nothing is dropped.

## Why the enacted object is the cascade, in generative-model terms

In active inference the agent carries a generative model: a joint distribution over
observations, hidden states and policies, with the likelihood (A), the transition kernels
indexed by control (B), the preferences over outcomes (C), the priors (D) and the prior over
policies (E). A *policy* is a sequence of control states over a horizon. Expected free energy
G is a functional of a policy: it scores the outcomes the model predicts under that policy
against C, plus the ambiguity and information terms. The posterior over policies is
σ(ln E − γG). That much the War Machine implements, and the record pins it
(`:law :sigma-log-E-minus-gamma-G`, `:gamma`).

The *action marginal* enters at the next step of the textbook discrete-time scheme: the
agent emits one control state per time step, so the probability of emitting control u now is
the posterior mass of every policy whose first control is u. That sum is correct exactly when
the thing emitted is one control state and the agent then re-observes and re-infers before
the next one. The policies are plans; the plan is revised after each step; what is committed
to at any moment is only the first step, and all plans agreeing on that step rightly pool
their support behind it.

In the War Machine the control state is the cascade. A cascade is the arranged structure of
pattern units applied as a whole to the task (a semilattice of pasted pushouts, by Joe's
2026-10-05 rulings); the inner run enacts the chosen cascade to its terminal receipt without
re-selecting between units; the record names one policy id as what ran. The transition
kernels and horizon over which G was computed are the cascade's own, so G(π) is already the
score of committing to the whole arrangement. There is no step inside the click at which the
agent re-observes and chooses a different continuation. The generative model that the
inference is performed on treats a cascade unit as a time step with re-planning after it; the
generative process the runner executes commits to the full cascade. When the model's control
grain and the process's commitment grain disagree, the inference answers a question the
process never asks. The first-action marginal answers "which first unit has the most support
across all plans that might follow it"; the runner then runs one particular plan, which may
not even be the best among those that share that first unit, and in 12 of 13 era-2
multi-policy runs was not the G minimiser at all.

Seen at the grain the process actually uses, every policy is one control state, horizon one.
At horizon one the action marginal *is* the policy posterior: the sum over policies sharing
the control collapses to the single policy. Choosing the maximum of the posterior with a flat
E is choosing the minimum of G. So the ruling does not depart from active inference; it
applies the action-selection step at the grain where the agent commits, which is where the
textbook scheme itself places it.

Two consequences follow from the same consideration.

- **Duplicates.** Policies in a generative model are distinguished by what they predict.
  Two arrangements that induce identical outcome distributions at every step are one policy
  under the model, however many syntactic arrangements realise it; L4b found that the
  same-G variants predict identically in every multi-policy run. Enumerating them separately
  and giving each a share of E multiplies prior mass by representation count, not by
  evidence, and that multiplied mass is what outvoted the lone lower-G policy. Q2 already
  asks for "extensionally distinct valid policies"; the policy space the law ranges over must
  be that quotient.
- **If the grain ever changes.** Should the runner one day apply one unit per click and
  re-select between units, the control state becomes the unit, the policies become sequences
  of units across clicks, and the action marginal becomes the right law again. Q11 would then
  have to name the unit as the enacted object. The ruling follows the commitment grain of the
  runner that exists, and the Lean carries that grain explicitly as the `cascade` in
  `Decision.pick`.

The outer run is the same structure one level up: its control state is a task-selection
cascade, its kernel is the reach model over the field, and the chosen cascade is enacted
whole (one task is selected). Joe's 2026-10-22 ruling that the outer loop uses the canonical G
is the same statement for that level.

Reference for the discrete-time scheme and the action marginal: Parr, Pezzulo and Friston,
*Active Inference* (MIT Press, 2022), the discrete-time generative model and action selection
chapters; Joe's copy is at `~/downloads/book_9780262369978.pdf`. I have not pinned page
numbers here; the record of the law we implement is `policy.clj:609` and the run records.

## Ruling 4 (Joe, 2026-11-01 ~13:30Z): selection is over tasks, not over policies for doing them

Joe: selection "has to be over this field [of] missions ... you can't just select based on the
construction of a cascade, because that would mean you'd never select something you hadn't
selected before and already worked on". The outer selection either does not consider inner
cascades at all or, if it does, gives fair play to tasks for which none has been constructed;
Joe prefers not considering them, "except in the most constrained way to say, well, this thing
is feasible versus that other thing is actually completely infeasible", and never "features of
any previously constructed inner cascade"; those are "facts which apply to the tasks, not to the
policies for doing the tasks".

**Where the principle already holds.** Lean Q2 for a selection run serves the whole enumerated
field; the task-selection cascades order Giraffe facts (status, cone, unblocks, rank); the
grounding's `FieldFact` carries only those facts and a pin; the relation is of the task to the
retained focus. None of these reads an inner policy.

**Where it was being violated or was at risk.**
1. `outer_observation.clj:131` calls `policy-satisfaction/all-satisfied-exclusion`, which drops a
   task from the outer field when every retained inner policy for it matches a "satisfied"
   record (`policy_satisfaction.clj:84-96`; 20 exclusions with `:reason
   :all-policies-already-satisfied` across era-2). That is a policy-derived exclusion of a task.
   The task-level fact is the registry's lifecycle status: a mission whose wants are met is
   closed, and Q1 (enumerated = open) then keeps it out. L5-4b removes this exclusion.
2. Reach. My hand witness took reach for the top ready task from four prior clicks on it. A
   never-attempted task has no such history, so a reach "observed from prior runs" would starve
   the never-selected, which is the degeneracy in a new form. Reach must be a task fact defined
   for every field member (dependency reading completeness, readiness, graph coverage, source
   readability), with click history at most an additional observation and never a precondition.
   L5-4b defines it so; `:default-reach` from a parameter file stays a violation.
3. Feasibility is allowed only as typed task-level facts: source path readable, dependency
   reading status, registry status. Not "a cascade was constructed for it", not "its policies
   were satisfied", not an inner G.

**Lean tripwire (L5-4L-3).** Exclusion reasons on a selection run come from a closed set of
task-level reasons; a policy-derived reason is a violation. The grounding's field facts carry no
inner-policy carrier; the module's docstring states this as the invariant it protects.
