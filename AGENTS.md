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
