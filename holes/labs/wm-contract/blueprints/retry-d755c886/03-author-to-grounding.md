# Blueprint 3: from construction to grounding (phases 16-18, 22-30)

Part of [00-TARGET](00-TARGET.md). Status: **no run on this route has got
this far**, and I have not read these phases against a reading-policy
construction. The `8dd7c2cff` test stops at `:author-dispatch` with stubbed
ports, which shows construction hands over and nothing about what follows.

Child 8 will be the first run to reach these phases on this route. Without
checks here, the pattern of blueprint 0 repeats: one click per stage.

## Leaves (each to be read, one discovery packet per leaf)

### 3.1 Reviewer wake and the discrimination guard (phases 16-17)

Claim: the reviewer seat is cast and is not the author, and
`(:passes? discrimination)` holds for the selected entry
(`full_loop_runner.clj:7150-7157`, throws `:policy-nondiscrimination`).
To read: how `discrimination` is computed for a sealed outer-policy
selection, and whether a menu where one policy leads by a wide margin passes.

### 3.2 The action occurrence is minted (phase 18)

Claim: `mint-action-occurrence-once!` accepts a reading-policy action
(kind, id, co-apply precedence, `:cascade-digest`). To read: what it requires
of the action.

### 3.3 The author is asked for something it can commit (phases 22-24)

This is the question I would read first. The author prompt
(`author-prompt`, `full_loop_runner.clj:3444`) asks for a complete parcel
committed in the target repository, and phase 24 fails `:build-failed` when
there is "no verifiable commit". The construction from blueprint 2 is one box
with a `:deduction` warrant ("selected cascade has a certified executable
refinement") and, for child 7's policy, no policy holes.

To read: what the prompt says when the wiring has no holes, and what a
reading-policy click is expected to produce in a repository. If the answer is
"nothing", phase 24 will stop the click, and that is a question about the
route's design rather than a defect to fix stage by stage.

### 3.4 Review, approval and grounding (phases 25-30)

Claim: the reviewer gets a reviewable diff, approves it with the execution
evidence the gate wants (`:review-execution-evidence-missing`,
`:reviewer-falsifier-failed`, `full_loop_runner.clj:7574-7641` in the phase
table), and `ground-commit!` produces a witness. To read once 3.3 is answered:
these phases depend on what the author commits.

## How these checks should run

Phases 22-30 call seats, so they cannot be replayed without spending a seat
job. The checks here are readings of the code against the construction
blueprint 2 produces, written down before the click in the way Lean rev 25
predicted click 11 (`logic-model/lean.md`). Each reading names the line that
would stop the click, or says that none does.
