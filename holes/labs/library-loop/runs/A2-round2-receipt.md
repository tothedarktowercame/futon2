# A2 library annotation round 2 — zaif refusal frontier

Date: 2026-09-06. Futon3 input and output: `3318838` (no supported edge was
found, so no futon3 file or commit was made). Scope was entries 9–16 of the
zaif-cascade refusal frontier. Pattern bodies were not changed.

## Results

### `cascades/declared-skeleton` — no committed source

The pattern states that an undeclared cascade cannot be measured, reviewed,
or diffed (`library/cascades/declared-skeleton.flexiarg:12-35`). Rejected
`problems/most-patterns-have-no-globally-recognised-rationale`: that node says
patterns lack recognised rationales and hence graph certificates
(`library/problems/most-patterns-have-no-globally-recognised-rationale.flexiarg:10-24`),
not that stable inter-pattern relations are undeclared. The PA11z discovery
also found no committed problem source for this pattern. The existing negative
annotation remains.

### `control/effort-estimation` — no committed source

The pattern addresses estimates which omit either token cost, human time, or
uncertainty (`library/control/effort-estimation.flexiarg:6-18`). Rejected
`problems/r11-hierarchical-shared-budget`: R11 concerns arbitration of a
shared consumable budget and refusal of over-subscription
(`library/problems/r11-hierarchical-shared-budget.flexiarg:11-19`), not
estimating implementation and review effort. No annotation was added.

### `equity/confident-adaptation-as-published` — no committed source

The problem stated by the pattern is that trainers cannot adapt confidently
without a shared fixed/flexible expectation scaffold
(`library/equity/confident-adaptation-as-published.flexiarg:27-42`). No
committed `library/problems` node states that problem. Rejected
`problems/transferable-open-research-practice`: its source and remit are the
OR3 case-study section (`library/problems/transferable-open-research-practice.flexiarg:10-21`),
not this equity/NPT programme problem. No annotation was added.

### `equity/confident-adaptation-merged` — no committed source

This variant states the same missing-scaffold problem, deepened as an
informational conversion and governance problem
(`library/equity/confident-adaptation-merged.flexiarg:14-29`). No committed
problem node states it. The OR3 section node was rejected for the same scope
mismatch as above. No annotation was added.

### `equity/contribution-pathways` — no committed source

The pattern states that engagement remains recipient-shaped and becoming is
capped when sustained contribution creates no recognised governance standing
(`library/equity/contribution-pathways.flexiarg:10-24`). No committed problem
node states that contribution/voice problem. Rejected
`problems/process-conduct-is-unassured`: it concerns assurance of delegated
work's commissioning, return, closure, and surfacing
(`library/problems/process-conduct-is-unassured.flexiarg:10-18`), not
participant governance. No annotation was added.

### `equity/contribution-pathways-merged` — no committed source

The merged variant states that programme delivery becomes goodwill-dependent
and extractive without contribution pathways and governance voice
(`library/equity/contribution-pathways-merged.flexiarg:14-29`). No committed
problem node states that problem. The process-conduct node was rejected for
the same mismatch above. No annotation was added.

### `equity/protected-interaction-as-published` — no committed source

The pattern states that the topic breadth exceeds what one trainer can cover,
creating breadth/depth tension and hidden coordination overload
(`library/equity/protected-interaction-as-published.flexiarg:27-46`). No
committed problem node states the trainer-coverage problem. Rejected
`problems/r11-hierarchical-shared-budget`: finite resource arbitration is not
the declared-delivery-profile and network-coverage problem. No annotation was
added.

### `gauntlet/modeline-persists-across-worlds` — no committed source

The pattern states that peripheral hops replace the agent's world and lose
dynamic AIF state unless a persistent status surface carries it
(`library/gauntlet/modeline-persists-across-worlds.flexiarg:9-25`). Rejected
`problems/r1-belief-state`: R1 requires a hidden-state belief distribution
carried across ticks (`library/problems/r1-belief-state.flexiarg:11-19`), not
a HUD/modeline carrying confidence, mana, pattern, and temperature across
peripherals. Rejected `problems/process-conduct-is-unassured`: typed handoff
assurance does not state the cross-peripheral UI-state problem. No annotation
was added.

## Complete searches

The following committed-corpus search was run once per regular expression;
each invocation completed without output and was **not truncated**:

```sh
for re in \
  'undeclared cascade|declared (edge|relation|structure)|stable relation|definite graph' \
  'effort estimation|token consumption|human time|person.hours' \
  'confident adaptation|expectation scaffold|fixed.*flexible|communal specification' \
  'contribution pathways|recipients without voice|governance standing|co-producer' \
  'protected interaction|breadth.*trainer|delivery profile|impostor' \
  'modeline persists|persistent modeline|peripheral hop|world changes|state resets'
do
  git -C /home/joe/code/futon3 grep -I -i -l -E "$re" HEAD -- \
    'library/problems/*.flexiarg'
done
```

The wider hole-tree candidate search used:

```sh
rg -n -i 'declared skeleton|undeclared cascade|stable relation|effort estimation|token consumption|human time|confident adaptation|expectation scaffold|fixed.*flexible|scaffold|contribution|voice|governance|protected interaction|breadth|modeline|peripheral|continuity|world' \
  /home/joe/code/futon2/holes --glob '*.md' --glob '*.flexiarg'
```

That broad output was truncated by the tool and therefore was used only to
locate candidates, never to support an absence claim. Candidate problem nodes
were then read directly at the pointers above. The six complete, untruncated
`git grep -l` enumerations are the basis for the library-problem absence
claims.

## Gate measurement

Before: `21/53 refused`, independently reproduced after round 1 at futon3
`3318838`.

After: regenerated `L1-census-graph.edn` from the same futon3 tree, extracted
the 53 consulted IDs from `checks/zaif-cascade.edn` with the token and
unique-keyword algorithm in `l17_advisory_report.py`, and ran:

```sh
bb runs/l13_graph_gate.clj L1-census-graph.edn zaif-patterns.edn down-problems+wr
```

Result: `gate: 21/53 refused`. Delta: **21 -> 21 (0)**. The refusal count did
not rise. This unchanged result is the expected control for a round in which
the evidence rule permits no new edge.

## Validation

No futon3 files changed, so there was no new annotation syntax to parse and no
futon3 commit to create. Existing unrelated futon3 modifications and untracked
math patterns, and unrelated futon2 run/simulation files, were left untouched.
