# Historical-cascade mining exemplar receipt

## Choice and bounded claim

I chose the policy-grain compliance solution (`M-wm-aif-policy-grain-compliance`)
because it has the richest committed trail among W1's six mapped solutions:
the mission states two exact grain mismatches (`holes/missions/M-wm-aif-policy-grain-compliance.md:12-26`),
declares the hierarchy and staged construction (`:34-46`, `:78-149`), names
the production functions and reduction tests (`:80-85`, `:119-149`), and the
F7 artifact records a real two-cascade dark menu and its ranking
(`holes/labs/wm-contract/runs/F7-cascade-policy/f7-cascade-policy-decision.edn:145-201`).
The mined solved behaviour is deliberately narrower than the mission: complete
same-mission cascade candidates can be constructed and ranked in dark mode.
The run explicitly says it does not select, enact, or write machine state
(`f7-cascade-policy-decision.edn:154-185`), so this record claims none of those.

Source pins read for this record:

- futon2: `5620ecd3bc33f4bf622f439e8c741708a28f4c59`
- futon3: `cdb5e8a56fd907beb6a99f8b88af9de50ff93126`

## Nodes and origin classification

All three nodes are `selected`, not `admitted`. The mission's initial plan
names the two-level hierarchy, construction of a candidate menu, and ranking
by separately named cascade quantities (`M-wm-aif-policy-grain-compliance.md:34-46`).
Its implemented slices retain the same three operations (`:91-149`). I found
no record that a library node was drawn into this work during execution, so an
`admitted` classification would be invented.

1. `aif/hierarchical-and-temporal-depth` — the mission requires an explicit
   strategic/tactical hierarchy and gives each level separate quantities
   (`M-wm-aif-policy-grain-compliance.md:34-59`). The pattern states precisely
   that coupling (`futon3/library/aif/hierarchical-and-temporal-depth.flexiarg:15-21,32-43`).
2. `aif/candidate-pattern-action-space` — Slice 1 specifies a concrete bounded
   menu with pattern IDs, semilattice wiring, source, and inclusion/exclusion
   reasons, then records the built `cascade-policy-menu-for` frontier
   (`M-wm-aif-policy-grain-compliance.md:91-125`). The pattern's mechanism is a
   bounded candidate set with explicit reasons
   (`futon3/library/aif/candidate-pattern-action-space.flexiarg:16-30`).
3. `aif/expected-free-energy-scorecard` — Slice 2 ranks each menu with separately
   named `ln E_cascade` and cascade score and records every term, support,
   identity, and counterfactual winner
   (`M-wm-aif-policy-grain-compliance.md:127-149`). The pattern requires named
   terms and persisted breakdowns
   (`futon3/library/aif/expected-free-energy-scorecard.flexiarg:17-31`).

`cascades/declared-skeleton` was not included even though W1 associated it with
the policy-grain problem. Its THEN block requires authoring stable upstream
relations in pattern files (`futon3/library/cascades/declared-skeleton.flexiarg:30-39`);
the mission records semilattice wiring as policy identity, but does not say
that this upstream-authoring mechanism participated. Association is not an
involvement witness.

## Authored dependencies

- `hierarchical-and-temporal-depth -> candidate-pattern-action-space`: the
  mission's hierarchy constructs an admissible `Pi_cascade(a)` before choosing
  a tactical policy (`M-wm-aif-policy-grain-compliance.md:34-44`).
- `candidate-pattern-action-space -> expected-free-energy-scorecard`: Slice 1a
  constructs complete distinct policies (`:119-125`); Slice 2 then attaches and
  records the ranking quantities (`:127-132`). This is document order and an
  explicit input dependency, not an inferred library edge.

No direct `@why`/`@how` edge joins these three exact IDs. The record therefore
cites the mission spans which author the dependencies and does not relabel
`@see-also` as a dependency.

## Rule-bearing members

1. `construct-a-bounded-explicit-candidate-menu` — THEN span
   `futon3/library/aif/candidate-pattern-action-space.flexiarg:26-27`.
   Interpretation class: `attested`; Slice 1 specifies the same constructed
   candidate fields, bounded support, and inclusion/exclusion reasons
   (`M-wm-aif-policy-grain-compliance.md:91-125`).
2. `keep-hierarchical-quantities-distinct` — THEN span
   `futon3/library/aif/hierarchical-and-temporal-depth.flexiarg:32-38`.
   Interpretation class: `attested`; the mission explicitly separates the
   strategic action from tactical cascade construction and distinguishes
   `E_scheduler`, `E_cascade`, and `S_cascade`
   (`M-wm-aif-policy-grain-compliance.md:34-59`).
3. `record-the-ranking-terms` — THEN span
   `futon3/library/aif/expected-free-energy-scorecard.flexiarg:27-28`.
   Interpretation class: `attested`; Slice 2 says to record every term,
   normalization support, identity, and counterfactual winner (`:127-132`).

## What could not be established

- No library pattern ID appears verbatim in the mission. Command (complete,
  untruncated output: no matches, exit 1):

  ```sh
  rg -n 'candidate-pattern-action-space|hierarchical-and-temporal-depth|expected-free-energy-scorecard|cascades/declared-skeleton' holes/missions/M-wm-aif-policy-grain-compliance.md
  ```

  Node involvement is therefore attested by exact mechanism descriptions and
  implemented slice witnesses, not by ID mention.
- No node-level admission event could be recovered. Command (complete,
  untruncated output: one generic occurrence, “admitted shadow case,” at line
  108; it does not name a pattern or describe drawing one into the work):

  ```sh
  rg -n 'admitted|admission|drawn in|selected patterns|patterns admitted' holes/missions/M-wm-aif-policy-grain-compliance.md
  ```

- The F7 artifact is a later execution witness for the implemented dark path,
  not evidence that the mission reached live compliance. Its producer contract
  says `:selects-or-enacts? false` and `:writes-machine-state? false`
  (`f7-cascade-policy-decision.edn:154-160`).

## Reproduce and verify

From `/home/joe/code/futon2`:

```sh
bb -e '(require (quote clojure.edn)) (let [x (clojure.edn/read-string (slurp "holes/labs/library-loop/runs/mining-exemplar/cascade.edn"))] (assert (= 3 (count (:nodes x)))) (assert (= 3 (count (:rules x)))) (assert (= (set (:selected x)) (set (map :pattern (:nodes x))))) (assert (empty? (:admitted x))) (println :ok))'
git diff --check -- holes/labs/library-loop/runs/mining-exemplar
```

Resolve all cited library files at the pinned futon3 revision:

```sh
git -C /home/joe/code/futon3 cat-file -e cdb5e8a56fd907beb6a99f8b88af9de50ff93126^{commit}
for f in candidate-pattern-action-space hierarchical-and-temporal-depth expected-free-energy-scorecard; do test -f "/home/joe/code/futon3/library/aif/$f.flexiarg"; done
```
