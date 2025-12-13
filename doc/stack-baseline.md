# Futon2 Stack Baseline

This note captures the active inference loop for Futon2 plus the reproducible single-tick trace requested for Prototype 0. It lives alongside the generated telemetry in `doc/trace/` so future iterations can diff behaviour.

## Observe → Perceive → Affect → Policy

1. **Observe** (`ants.aif.observe/g-observe`)
   - Reads the local grid cell, neighbour statistics, resource reserves, and ant memory.
   - Normalises every scalar to `[0,1]`, including derived channels such as `:trail-grad`, `:novelty`, `:dist-home`, and reserve pressure. Observation vectors are exported through `sense->vector`, locking the ABI ordering the README mentions.
2. **Perceive** (`ants.aif.perceive/perceive`)
   - Seeds or updates the latent belief `mu` with position, goal, hunger and sensory predictions.
   - Runs predictive-coding micro-steps (defaults: 4 iterations, `alpha=0.45`, `beta=0.28`) that
     * anneal precisions (`affect/modulate-precisions` + `affect/anneal-tau`)
     * compute weighted errors for each sensory key
     * apply hunger drift via `affect/tick-hunger`
     * append trace rows `{:tau … :h … :error …}` for every micro-step.
   - Returns `{:mu … :prec … :trace …}`, so callers can snapshot beliefs without extra APIs.
3. **Affect** (inside `ants.aif.core/aif-step`)
   - Tracks hunger trend windows (`core/dhdt`) and need error.
   - Calls `affect/update-tau` to incorporate hunger velocity and reserve pressure into the precision structure.
4. **Policy** (`ants.aif.policy/choose-action`)
   - Predicts outcomes for each macro action, scores them via expected free energy plus hand-tuned biases, and softmaxes the logits with the hunger-modulated `tau`.
   - Returns `{:action … :policies {action {:G … :p …}} :tau …}`. This is the contract enforced by the new harness goldens.

The Agent contract from `AGENTS.md` still holds: ant state is the grid location stored inside an `agent`, and the cell map carries mutable attributes. The architecture here merely sketches how the pure AIF stack reasons before mutating the world inside a `dosync` transaction.

## Trace & Snapshot Artifacts

Run `clj -M -m tools.trace-tick` to capture a deterministic single-tick update. The script writes:

- `doc/trace/single_tick.edn` – inputs and outputs from a baseline tick. Top-level keys:
  - `:world` → trimmed world (grid definition + home locations)
  - `:observation` → full evidence vector from `g-observe`
  - `:perception.trace` → vector of `{:tau :h :error}` snapshots for each predictive micro-step
  - `:perception.mu` → latent beliefs after the final step (`:pos`, `:goal`, `:h`, `:sens`)
  - `:perception.prec` → annealed precisions including the hunger-driven `:tau`
  - `:policy` → chosen action, coupled `tau`, and ranked action goldens `[{ :action … :p … :G …}]`
  - `:diagnostics` + `:ant` → hunger trend, colony penalties, and the ant’s stored `:last-trace` for downstream tooling
- `doc/trace/single_tick.svg` – a lightweight SVG plot of the predictive-cycle trace showing how `tau`, `h`, and the accumulated error evolve across the four micro-steps. It gives the “single-tick plot” asked for in the readiness scan.

Because the EDN stores raw observables, engineers can diff it after tuning hunger knobs, and the SVG shows whether annealing, tau coupling, or hunger drift regress. No renderer plumbing was touched: the artifacts sit entirely in `doc/` and are generated via the pure `aif-step` API.

## Using the Snapshot API

`ants.aif.core/aif-step` already exposes everything needed for mu/precision snapshots:

```clojure
(let [{:keys [observation perception policy]} (core/aif-step world ant)]
  {:inputs observation
   :beliefs (:mu perception)
   :precisions (:prec perception)
   :trace (:trace perception)
   :policy-summary policy})
```

The new documentation plus `doc/trace/single_tick.edn` codify the shape of these maps so downstream consumers (plotters, notebooks, ML probes) can rely on it without spelunking through namespaces.
