From claude-11. Clojure task, one new namespace + its test. Bell claude-11 back with a summary + the FULL commit sha (`git rev-parse HEAD`).

## Goal

Clojure packet 3 of the outer loop: build the outer observation for one
click, aligned with Lean `R20Step.Observation` (`field`, `facts`, `focus`) and
the inputs `futon2.aif.outer-score/outer-score` already takes
(`:focus :relation :facts :field`, see `src/futon2/aif/outer_score.clj` at
`e6c39ef7e`). Reuse map:
`/home/joe/code/futon2/holes/labs/wm-contract/DISCOVERY-outer-loop-clojure-2026-10-28.md`
§2–3. No runner wiring, no reloads, no clicks, no writes outside temp dirs in
tests.

## New namespace `futon2.aif.outer-observation`

- `giraffe-field` — from a verified Giraffe run directory (missions source;
  first real run `~/code/storage/giraffe/20261028T154151.285Z-b916cd4e`):
  verify the run with `futon3c.giraffe.run`'s own `verify-run` (resolved
  dynamically, as the discovery note §2 says is possible, or by re-implementing
  nothing: if it cannot be resolved, refuse with a typed reason), read
  `pass.edn`, and return the open items as candidates `{:target id :reach r}`
  with facts `{id {:status s :unblocks n :cone n}}`, where `s` is the pass's
  `:ready` / `:not-ready` / `:undetermined` and n are the counts of the pass's
  `:unblocks` / `:cones` sets. Closed items are not in the field. `r` is the
  makeup's `:default-reach` (`data/wm-learning/outer-makeup.edn`; task reach is
  absent in the runtime, a stated limit).
- `outer-observation` — combines the field with the current focus
  (`futon2.aif.focus-receipt/decision-focus`) and a relation function built on
  `focus-receipt/classify-target` mapping to `:focus` / `:associated` /
  `:useful-elsewhere`; `:unknown` stays unknown (the score then treats the
  pick as no class, i.e. stop-the-line landing, as the Lean `outcomeClass =
  none` case does). Returns the map `outer-score` takes, plus provenance:
  the Giraffe run id and its manifest digest, and the focus source.

## Tests (`test/futon2/aif/outer_observation_test.clj`)

- On a small fixture run directory written in a temp dir in the Giraffe run
  format (or by calling the real Giraffe pass on a tiny items/edges input):
  closed items excluded; statuses, unblocks and cones carried exactly;
  `:undetermined` (a cycle) kept distinct from `:not-ready`.
- A run whose manifest does not verify is refused.
- The relation function returns `:unknown` for an unresolvable target rather
  than a guess, and the field built from the real run
  `20261028T154151.285Z-b916cd4e` has 147 open items and 140 ready (read-only;
  skip with a stated reason if the directory is absent).

## Gates

- `clj-kondo --lint` and `futon4/dev/check-parens.el` on the new files.
- `clojure -M:test -n futon2.aif.outer-observation-test` only.
- Commit only the two new files by explicit path (futon2 is shared; never
  `git commit -a`).
