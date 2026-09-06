# A4 problem-authoring batch 1 receipt

Date: 2026-09-06. Futon3 input: `50f210c`; output: `ba0c8f6`.
This batch mined committed history for the four named missing problems,
created one problem node per exact historical statement, and grounded each
source pattern on that node. No pattern body was changed.

## Results

### `cascades/declared-skeleton` — node committed (`f6ca479`)

Node: `problems/undeclared-cascades-cannot-be-audited`.
Edge: `@why problems/undeclared-cascades-cannot-be-audited`.

Verbatim committed statement:

> “Declaration is what makes an edge witnessable, versionable, and refusable — an undeclared cascade cannot be measured, reviewed, or diffed. The store's fragmentation (nine patterns, zero shared structure) was only VISIBLE because attachments were declared; structure that exists nowhere but in an agent's momentary judgment teaches the store nothing.”

Source: `futon3/library/cascades/declared-skeleton.flexiarg:36-39`, present
since its originating commit `2a49684`. The node quotes that statement and
claims no mechanism at problem grain.

### `control/effort-estimation` — node committed (`1d7d8e5`)

Node: `problems/ai-assisted-effort-needs-two-resources`.
Edge: `@why problems/ai-assisted-effort-needs-two-resources`.

Verbatim committed statement:

> “Tracking only time misses token costs; tracking only tokens misses human bottlenecks; neither captures uncertainty”

Source: `futon3/library/control/effort-estimation.flexiarg:16-17`, present
since its originating commit `84c4045`. The pre-existing pattern lacked the
current required `@how`; the same annotation-only commit added an own-mechanism
`@how` sourced to its conclusion and THEN (`:8-20`).

### `agent/trail-enables-return` — node committed (`6d0efe3`)

Node: `problems/unqueryable-trails-block-recovery`.
Edge: `@why problems/unqueryable-trails-block-recovery`.

Verbatim committed statement:

> “Without a trail, the agent cannot distinguish "unexplored" from "explored and rejected," leading to repeated mistakes or inability to recover.”

Source: `futon3/library/agent/trail-enables-return.flexiarg:18`, present
since its originating commit `771c578`. The node claims no mechanism at
problem grain.

### `ants/pheromone-trail-tuner` — node committed (`b4b5ac5`)

Node: `problems/uncoupled-pheromone-becomes-noise`.
Edge: `@why problems/uncoupled-pheromone-becomes-noise`.

Verbatim committed statement:

> “Without adaptive scaling, ants either flood the board with trails (entropy) or fail to leave breadcrumbs when scouts discover food deserts.”

Source: `futon3/library/ants/pheromone-trail-tuner.flexiarg:38-39`, present
since its originating commit `521967f`. The pre-existing pattern lacked the
current required `@how`; the same annotation-only commit added an own-mechanism
`@how` sourced to its conclusion and THEN (`:28-46`).

## Mining trail

The first pass enumerated candidate files in all commissioned repositories
with one `git grep -l` per expression and repository:

```sh
for re in \
  'undeclared cascade|undeclared.*(edge|relation|structure)|cannot be (measured|reviewed|diffed)|momentary judgment|definite graph' \
  'token (cost|consumption)|human time|person.hours|effort estimate|tracking only (time|tokens)' \
  'queryable trail|retrace|rejected paths|recover without repetition|repeat.*mistake|trail.*return' \
  'uncoupled pheromone|pheromone.*noise|noise.*pheromone|pheromone.*communicat|laying.*pheromone'
do
  for repo in /home/joe/code/futon2 /home/joe/code/p4ng \
              /home/joe/code/futon3 /home/joe/code/futon4
  do
    git -C "$repo" grep -I -i -l -E "$re" HEAD -- \
      'holes/**' 'docs/**' 'sec-*.tex' 'library/**'
  done
done
```

All sixteen enumerations completed and were **not truncated**. Candidate
files were then searched with line output in bounded, explicit path lists.
The exact source statements above were verified in current files and in their
originating commits with:

```sh
git -C /home/joe/code/futon3 show 2a49684:library/cascades/declared-skeleton.flexiarg
git -C /home/joe/code/futon3 show 84c4045:library/control/effort-estimation.flexiarg
git -C /home/joe/code/futon3 show 771c578:library/agent/trail-enables-return.flexiarg
git -C /home/joe/code/futon3 show 521967f:library/ants/pheromone-trail-tuner.flexiarg
git -C /home/joe/code/futon3 blame -L 36,40 -- library/cascades/declared-skeleton.flexiarg
git -C /home/joe/code/futon3 blame -L 16,17 -- library/control/effort-estimation.flexiarg
git -C /home/joe/code/futon3 blame -L 18,18 -- library/agent/trail-enables-return.flexiarg
git -C /home/joe/code/futon3 blame -L 38,39 -- library/ants/pheromone-trail-tuner.flexiarg
```

Those commands completed without truncation. In each case the pattern's
committed `HOWEVER` or `BECAUSE` already stated the problem exactly; the new
node promotes that historical statement into the graph's problem namespace
rather than reconstructing it from the mechanism.

## Gate measurement

Before: `gate: 20/53 refused` at futon3 `50f210c`.

After: regenerated `L1-census-graph.edn` after the four content commits at
futon3 `b4b5ac5` (the later `f165a82` changes citations only), extracted the
53 consulted IDs from `checks/zaif-cascade.edn` using the token and
unique-keyword algorithm in `l17_advisory_report.py`, and ran:

```sh
bb runs/l13_graph_gate.clj L1-census-graph.edn zaif-patterns.edn down-problems+wr
```

Result: `gate: 16/53 refused`. Delta: **20 -> 16 (-4)**. Each of the four new
`@why` edges is `:resolved true` in the regenerated graph.

Futon3 `f165a82` and `ba0c8f6` are follow-up citation-only commits correcting
and tightening current-file line numbers after the annotation insertions
shifted them; they change neither node statements nor graph edges.

## Validation

The L2 parse gate reported 0 failures for the four new problem nodes and for
each of the four touched patterns when run in an isolated path-scoped library
tree. `git diff --check` passed before the commits. Existing unrelated futon3
changes (`resources/sigils/patterns-index.tsv` and two untracked math patterns)
and unrelated futon2 run/simulation files were left untouched.
