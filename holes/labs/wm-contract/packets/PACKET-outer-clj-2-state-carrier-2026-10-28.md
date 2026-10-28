From claude-11. Clojure task, one new namespace + its test + one data file. Bell claude-11 back with a summary + the FULL commit sha (`git rev-parse HEAD`).

## Goal

Clojure packet 2 of the outer loop: the carrier for the outer level's learned
state between clicks, aligned with Lean `R20Step.State` (fields `c`,
`stateCounts`, `gainCounts`) and `R20Step.init` / `Makeup` (the kits).
Design: `/home/joe/code/futon2/holes/labs/wm-contract/NOTE-outer-G-computation-2026-10-28.md`;
reuse map: `DISCOVERY-outer-loop-clojure-2026-10-28.md` §4–5 (no learned C or
outer counts survive; `outer-learning-manifest` is a publication ledger, not
state, and must not be mutated). No runner wiring, no reloads, no clicks.

## Files

- `data/wm-learning/outer-makeup.edn` — the starter kits and rates, each
  value with a one-line comment saying where it comes from:
  `:c-kit` (55/35/5/5 over focused/related/unrelated/stop-the-line, 0 for
  not-yet-evaluated; Joe's starter kit), `:kappa`, `:state-kit` (per status
  over doable/blocked; ready leaning doable, not-ready leaning blocked,
  undetermined even — say these are starter values to be learned),
  `:gain-kit` (favouring more items made ready), the rates
  `:eta-c :omega-c :eta-state :omega-state :eta-gain :omega-gain`, and
  `:default-reach` (task reach is absent in the runtime; a stated limit).
- `src/futon2/aif/outer_state.clj`, namespace `futon2.aif.outer-state`:
  - `load-makeup` (validates positivity and 0 < ω ≤ 1 as the Lean `Makeup`
    fields require; refuses with a typed reason otherwise);
  - `init-state` (Lean `init`: c = κ·kit, counts = kits);
  - `read-state` (the current state from
    `data/wm-learning/outer-state.edn`, or `init-state` when absent, marked
    `:source :init`);
  - `write-state!` (atomic: temp file + rename in the same directory; the
    written map pins the makeup file's sha256 and a click count);
  - `read-state` refuses a state whose makeup pin differs from the current
    makeup's sha (typed `:makeup-changed`), rather than mixing kits.

## Tests (`test/futon2/aif/outer_state_test.clj`, temp dirs only)

- init from kits equals the Lean `init` shape (c = κ·kit);
- write then read round-trips;
- a changed makeup is refused on read;
- an invalid makeup (ω = 0, negative kit) is refused on load;
- a failed write leaves the previous state file intact.

## Gates

- `clj-kondo --lint` and `futon4/dev/check-parens.el` on the new files.
- `clojure -M:test -n futon2.aif.outer-state-test` only.
- Commit only the three new files by explicit path (futon2 is shared; never
  `git commit -a`). Do not create `outer-state.edn` itself.
