# A3 library annotation round 3 — zaif refusal frontier remainder

Date: 2026-09-06. Futon3 input: `3318838`; annotation commit: `50f210c`.
Scope was the final eight entries of the 24-pattern zaif-cascade refusal
frontier. Pattern bodies were not changed.

## Results

### `budgeted-action-selection/mana-gated-work` — edge committed (`50f210c`)

Edge: `@why problems/r11-hierarchical-shared-budget`.

Problem statement, verbatim:

> “R11 solves how locally factored proposals share a finite resource without
> silently oversubscribing it ... local agents propose inside sub-budgets and
> a coordinator arbitrates the shared consumable budget at every level.”

Source: `library/problems/r11-hierarchical-shared-budget.flexiarg:11-19`.
Mana-gated work implements that problem's refusing boundary: a candidate runs
only if the shared session balance affords its expected cost
(`library/budgeted-action-selection/mana-gated-work.flexiarg:12-23,70-84`).
The file lacked the current required `@how`; the same annotation-only commit
added an own-mechanism `@how` sourced to those lines. No pattern body changed.

### `gauntlet/teaching-inversion` — no committed source

The pattern's problem is that agents discover material the human has not seen
but the communication channel returns results rather than teachable insight
(`library/gauntlet/teaching-inversion.flexiarg:9-25`). Rejected
`problems/process-conduct-is-unassured`: checked returns and surfaced process
failures (`library/problems/process-conduct-is-unassured.flexiarg:10-18`) do
not state an agent-to-human learning problem. No annotation was added.

### `hdm/non-capture` — no committed source

The pattern states that proprietary enclosure, paywalls, patent thickets, and
funding dependencies can capture an open mathematical commons
(`library/hdm/non-capture.flexiarg:5-24`). No committed problem node states
that commons-enclosure problem. Generic process assurance and transferable
open-research-practice nodes were rejected: neither names ownership,
licensing, enclosure, or dependency capture. No annotation was added.

### `math-formalization-CA/measure-integration-api` — no committed source

The pattern addresses hidden library generality and totalized integrals whose
missing premises yield junk values rather than failures
(`library/math-formalization-CA/measure-integration-api.flexiarg:12-61`). Its
existing `@why math-informal/reduce-to-known-result` is a mechanism-pattern
edge, not a problem-node edge. Rejected
`problems/spec-and-implementation-must-converge-to-runtime-certificates`: that
node concerns agreement between WM implementation and Lean specification
(`library/problems/spec-and-implementation-must-converge-to-runtime-certificates.flexiarg:10-18`),
not measure-theory API discovery or integrability premises. No annotation was
changed.

### `math-formalization-CV/frontier-bound-from-arc-hypotheses` — no committed source

The pattern addresses a hard global frontier theorem entangled with routine
arc hypotheses (`library/math-formalization-CV/frontier-bound-from-arc-hypotheses.flexiarg:11-55`).
No committed problem node states that complex-variable formalization problem.
The convergence node was rejected for the same spec/runtime mismatch above.
No annotation was added.

### `math-formalization-CV/holomorphic-disk-api` — no committed source

The pattern states that classical disk theorems are packaged by interfaces,
while required hypothesis shape, radius, and branch information are implicit
in textbook statements (`library/math-formalization-CV/holomorphic-disk-api.flexiarg:11-59`).
No committed problem node states that problem. The convergence node was again
rejected as a different problem. No annotation was added.

### `math-strategy/missing-dependency-protocol` — no committed source

The pattern states that a purported missing theorem may be a malformed search,
and that a real gap must be localized with its scoped search trail rather than
worked around or spread over several `sorry`s
(`library/math-strategy/missing-dependency-protocol.flexiarg:11-57`). Its
existing `@why math-informal/reduce-to-known-result` does not land on a problem
node. No committed problem node states this dependency-localization problem;
the general convergence node was rejected. No annotation was changed.

### `ukrns/publication-cadence` — no committed source

The pattern states the citation/currency conflict between a changing living
document and fixed snapshots (`library/ukrns/publication-cadence.flexiarg:13-30`).
Rejected `problems/transferable-open-research-practice`: that section-level
OR3 node concerns transferable practices distilled from case studies
(`library/problems/transferable-open-research-practice.flexiarg:10-21`), not
publication version identity. The existing negative annotation remains.

## Complete searches

This search was run once per expression over every committed problem node.
It completed and was **not truncated**. Only the budget expression returned
files: `fundamentals-drowned-by-backlog.flexiarg` and
`r11-hierarchical-shared-budget.flexiarg`; all other expressions returned no
files.

```sh
for re in \
  'budget|oversubscri|mana|afford|priority' \
  'teaching inversion|teach.*human|human.*learn|result.only|insight.*result' \
  'non.capture|commons|enclosure|paywall|proprietary|gated service' \
  'measure.theoretic|integrability|junk value|Haar|Bochner' \
  'frontier bound|arc hypoth|holomorphic disk|disk theorem|branch cut|Rouch' \
  'missing dependency|not in Mathlib|absence claim|one sorry|terrain gap' \
  'publication cadence|citation ambiguity|living document|frozen snapshot|which version'
do
  git -C /home/joe/code/futon3 grep -I -i -l -E "$re" HEAD -- \
    'library/problems/*.flexiarg'
done
```

Candidate files were read in full around their problem statements and the
specific rejected candidates are named above. Absence conclusions rely on
the complete filename enumeration, not a head-limited listing.

## Gate measurement

Before: `21/53 refused` at futon3 `3318838`.

After: regenerated `L1-census-graph.edn` at `50f210c`, extracted the same 53
consulted IDs from `checks/zaif-cascade.edn` using the token and unique-keyword
algorithm in `l17_advisory_report.py`, then ran:

```sh
bb runs/l13_graph_gate.clj L1-census-graph.edn zaif-patterns.edn down-problems+wr
```

Result: `gate: 20/53 refused`. Delta: **21 -> 20 (-1)**. The new edge occurs
in the regenerated graph as `:resolved true`; the seven unsupported patterns
remain in its refused list.

## Validation

`l2_parse_gate.py budgeted-action-selection /home/joe/code/futon3/library`
reported 1 file and 0 failures. `git diff --check` passed before the futon3
commit. Existing unrelated futon3 changes and untracked math patterns, and
unrelated futon2 run/simulation files, were left untouched.
