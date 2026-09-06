# A1 library annotation round 1 — zaif refusal frontier

Date: 2026-09-06. Futon3 input: `50bd5309`; annotation commits:
`bc9f777` and `3318838`. Scope was the first eight entries in
`PA11z-exemplar/zaif-frontier-remeasure.txt`. Pattern bodies were not changed.

## Results

### `agent/handoff-preserves-context` — edge committed (`bc9f777`)

Edge: `@why problems/process-conduct-is-unassured`.

Problem statement, verbatim:

> “Handoff machinery exists un-assured: bells/parks/packets are conventions
> in CLAUDE.md prose, not typed objects any checker refuses on.”

Source: `holes/problems/P-assured-process.md:41-43`. The required typed
handoff and checked-return form is stated at `:55-65`.

### `agent/trail-enables-return` — no committed source

Rejected `problems/process-conduct-is-unassured`: it states that the handoff
lifecycle lacks typed assurance, but not this pattern's problem—that without a
queryable trail an agent cannot distinguish rejected paths, retrace decisions,
or recover without repetition. The pre-existing negative annotation remains.

### `ai4ci/ai-core-function` — no committed source

Rejected `problems/operator-turns-become-inference-observations`: it concerns
operator turns entering an inference vector, not the absence of systematic,
reviewable links between mathematical exposition's informal cues and proof
steps. No AI4CI problem node exists in the committed `library/problems`
corpus. No annotation was changed.

### `ai4ci/funding-justification` — no committed source

No committed problem node states that a pilot budget appears speculative or
infrastructure-heavy when its purchased activities are not itemised. Generic
fundamentals, scope, and evidence-ledger nodes do not state that funding
problem. No annotation was added.

### `ai4ci/pilot-feasibility` — no committed source

No committed problem node states that an AI/explanation pilot will exceed a
six-month schedule without explicit scope fences. Generic scope and blocker
patterns are mechanisms, not problem-stating nodes for this claim. No
annotation was added.

### `aif/belief-aware-risk-term` — edge committed (`3318838`)

Edge: `@why problems/r5-expected-free-energy-core`.

Problem statement, verbatim:

> “The operational criterion requires at least risk and ambiguity, **‘both
> ... computed against the predictive forward model from R4’** ... The
> registry makes those meanings precise: risk is `D_KL[Q(o|pi) || C]` and
> ambiguity is `E_{Q(s|pi)}[H(P(o|s))]`.”

Source: `library/problems/r5-expected-free-energy-core.flexiarg:18-19`
(ultimately `PROBLEMS-r4-r5-r6-pilot.md`, R5). The pattern supplies a
belief-aware Gaussian risk mechanism for that stated predictive-risk problem.

### `aif/status-gated-belief-update` — edge committed (`3318838`)

Edge: `@why problems/r3-belief-update`.

Problem statement, verbatim:

> “R3 solves how observations change belief without either overwriting it or
> letting it become immovable ... The registry writes the reduction as `mu <-
> mu + alpha Pi eps`, importing prior belief, precision, error, and step size.”

Source: `library/problems/r3-belief-update.flexiarg:18-19` (ultimately
`PROBLEMS-r1-r3-r3a-batch3.md`, R3). Status gating prevents this
observation-driven update from being applied to an instance for which no
observation exists.

### `ants/pheromone-trail-tuner` — no committed source

No committed problem node states that uncoupled pheromone laying produces
noise rather than useful communication. Rejected the R7 evidence-precision
node: evidence-channel trust in the WM is not the ant's hunger/novelty/reserve
coupling. Rejected the general navigation-tension node: it does not state a
pheromone communication problem. No annotation was added.

## Gate measurement

Before: `24/53 refused`, from the committed PA11z remeasurement at futon3
`50bd5309`:

```text
gate: 24/53 refused
```

After: regenerated `L1-census-graph.edn` from an isolated worktree at futon3
`3318838`, extracted the 53 consulted IDs from `checks/zaif-cascade.edn` with
the same token and unique-keyword algorithm as `l17_advisory_report.py`, then
ran:

```sh
bb runs/l13_graph_gate.clj L1-census-graph.edn zaif-patterns.edn down-problems+wr
```

Result:

```text
gate: 21/53 refused
```

Delta: **24 -> 21 (-3)**. The three newly reachable patterns are exactly the
three annotated above. All three edge targets occur as nodes and all three
edges are `:resolved true` in the regenerated graph.

## Validation

`l2_parse_gate.py agent /home/joe/code/futon3/library` reported 17 files and
0 failures. `l2_parse_gate.py aif /home/joe/code/futon3/library` reported 33
files and 0 failures. `git diff --check` passed before both commits. Existing
unrelated futon3 changes (`resources/sigils/patterns-index.tsv` and two
untracked math patterns) and unrelated futon2 run/simulation files were left
untouched.
