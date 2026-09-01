# AGENTS

This document is the contract for agent behavior in **clj-ants-aif**. It lets you (and Codex) work on agent brains without touching rendering or world plumbing.

## TL;DR
- Agents are **ants** whose *state is the grid location* stored in an **agent** (Clojure `agent`).
- The ant's *attributes* (dir, species, brain, latent AIF state) live in the **cell map** at that location: `@(place loc) => {:ant {...} :food ... :pher ... :home ...}`.
- A brain is a function that receives `loc` and returns the *next* `loc`, while mutating the world inside a `dosync` transaction using helpers (`move`, `turn`, `take-food`, `drop-food`).

## Species & Brains

- **Classic (black)**: rule-based `behave-classic` (Rich Hickey’s original).
- **AIF (red)**: `behave-aif` → calls `(aif-step world loc ant)` which performs: observe → perceive (micro-steps) → evaluate actions (expected free energy) → act.

Ant map keys (always present):
```clojure
{:dir int               ; 0..7
 :species :classic|:aif
 :brain   :classic|:aif

 ;; AIF-only (created lazily by ensure-aif-state)
 :mu   {:pos [x y] :goal [gx gy] :h double}   ; latent beliefs
 :prec {:Pi-o {:food double :pher double :h double}
        :tau double}}                         ; action temperature

## Changing a witness: run its negative modes before you land (adopted 2026-09-01)

The 32 Lean `#guard_msgs` fixtures are the checks that prove the *other* checks
still detect. Each is a deliberately wrong statement that passes only when the
machinery rejects it. If a definition changes so one stops rejecting, the
ordinary checks stay green and say nothing — the alarm broke, not the thing
being watched.

**If your packet changes a witness or its dependency, run that witness
wrapper's negative modes before you land.** One costs about 6.5 s (3.4 GB peak
RSS, so run them one at a time — the memory is Mathlib importing, and this
machine has hit a cgroup throttle from memory pressure). Example:

```sh
bb checks/softmax_witness.clj --negative-order
bb checks/softmax_witness.clj --negative-normalisation
```

The mapping from wrapper to its registered negative modes is in
`checks/wm_workspace_gate.clj` and tabulated in
`holes/labs/wm-contract/C437-guarded-control-invocation-census.md`.

**Do not run the full 32 per commit** — that is about 3 min 30 s and re-proves
31 things nobody touched. **Do not put the full suite on a timer.** Its trigger
is a milestone, and "major milestone" is not yet defined (register O25). Gate
runs required by `make pre-merge` or by a certified commit are unaffected by
this: what is ruled out is running the suite on a schedule or out of habit.

Decision and measurements: `holes/problems/decision-briefs/O15-lean-check-cadence.md`.


## At least one fixture looks like the data (2026-09-01)

A test can prove the right properties and still miss the defect, because every
fixture it uses is the wrong shape. `policy_free_energy.clj` shipped with tests
that proved exactly what was asked — F_π discriminates between candidates, and
is degenerate for identical ones — over fixtures of one or two synthetic
channels. The function could not be called on WM data at all: a real
prediction has fourteen channels of which twelve carry
`:variance-status {:status :absent}`, and it rejected every one of those that
had moved since the last tick.

So, alongside *controls pin properties, not sentences*: **at least one fixture
must have the shape of the data the code will actually see** — the real channel
count, the real proportion of absent or defaulted fields, values that moved the
way they move between real ticks. Take the shape from a recorded artifact
(`data/wm-trace/*.edn`) rather than inventing it; the fixture does not have to
be real data, but it has to be the wrong answer for the same reasons real data
would be.

The instance: `test/futon2/aif/policy_free_energy_test.clj`, the block below
the rule comment. The finding: `holes/labs/wm-contract/worklist.edn` row `:I2`
`:slice-b1`.

## Never pipe a gate's output (2026-09-01)

A pipeline's exit status is the *last* command's. `bb worklist_check.bb | cut`
reports `cut`'s success no matter what the check found, so an invalid ledger
committed cleanly and sat in history for a minute before anyone noticed. Run
the gate bare, or capture its status and read that:

```bash
bb worklist_check.bb worklist.edn; rc=$?      # not: ... | tail -1
[ "$rc" -eq 0 ] || exit "$rc"
```

`&&` chains have the mirror problem: a `cd` earlier in the chain can send a
later command somewhere the file isn't, and a trailing `tail -1` eats the
error, so the run looks like silence rather than failure. Both of us hit this
within an hour of each other, in opposite directions.

This is the same shape as three other defects this campaign found — a pointer
check that read a range's end and ignored its start, `edn/read-string`
returning the first form of a 38-record file, and a test whose fixtures were
the wrong shape. **A tool answering a narrower question than the one being
asked, and its answer read as though it were the wide one.** The defence is
always the same: look at what the tool actually did — the exit code, the diff,
the count — before believing what it seems to have said.

## Measure a comparison where the arms are supposed to differ (2026-09-01)

A comparison run at the one parameter value where its arms coincide by
construction is a control chosen where it cannot fail. Two instances in one
slice:

- F_π entering the policy posterior can be scaled by τ or not. Both arms were
  measured at the live τ — which is exactly **1.0**, where `F/τ = F`. The two
  arms were the same computation, and the identical numbers were reported as
  though they had distinguished the options. Re-run at τ = 0.1 and 2.0 they
  differ by a factor of four in rank changes.
- "Does the enacted action change?" was then tested on a field where the act
  gate had verdicts for **2 of 110** candidates, only one of them passing. The
  first gate-passing candidate is invariant under *any* permutation of that
  field, including one that reverses it. The test could not have failed.

So: **name the parameter the two arms differ in, pick values where they do,
and say in the report which values you used and why.** If no available value
separates them — because the live path pins the parameter, or the fixture has
one qualifying case — that is the finding. Report it as "not distinguishable
on this field" rather than as agreement.

Sibling of *at least one fixture looks like the data*: that rule is about the
shape of the input, this one is about where in the parameter space you stood.

## Before signing that a code path is safe, enumerate — don't verify the pointer (2026-09-01)

A discovery reported one unsafe actuator on a path, with a file:line. I opened
the file, confirmed the line said what the report claimed, and signed. The
verification was correct and the signature was wrong: `grep -n 'http/post'`
over the same file returns **four** sites, two of which fire on that path. One
of them POSTs Clojure source to Drawbridge `/eval` on `:6768` — loading code
into the shared JVM from a diagnostic run, which the workspace rule of
2026-08-23 forbids outright.

A pointer that resolves *feels* like verification, which is why this one is
easy to miss. But confirming a claim and establishing an absence are different
operations, and only the second is what a safety signature asserts. So:

**When the question is "does this path do X anywhere", the answer comes from a
search over the space, never from checking the instances you were handed.**
Run the grep, list every site, state each one's reachability. Four seconds.

Same family as the other entries here — the range-end pointer check,
`read-string` on a multi-form file, a piped gate's exit code, a fixture of the
wrong shape, a comparison at the one parameter value where the arms coincide.
In all of them a narrower question was asked than the one that mattered and its
answer was read as the wider one. This is the only instance so far where the
cost would have been an actuator firing rather than a sentence being wrong.
