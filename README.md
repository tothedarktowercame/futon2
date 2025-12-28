# clj-ants-aif

An experiment in active inference ants battling the classic Rich Hickey ruleset. Two hives spawn on opposite corners of a grid and compete for food while adapting via predictive coding, hunger dynamics, and expected free-energy minimisation.

## Getting Started

Clone the repo and make sure you have [Clojure CLI tools](https://clojure.org/guides/getting_started) installed. Everything else comes from `deps.edn`.

```bash
git clone https://github.com/ants/clj-ants-aif.git
cd clj-ants-aif
```

### Run the war sim

```bash
# default scenario (mirrored hives, ~200 ticks)
clj -M:run

# override total ticks from the CLI (e.g. 50 ticks)
clj -M:run 50
```

Each tick prints a HUD line with the classic vs AIF score, tick counter, an EMA of the AIF expected free energy, and hive reserves (a running food bank for each colony). The tail of the line highlights the most recent action per army plus a termination banner when starvation ends the match. For programmatic control, start a REPL (`clj`) and drive the pure API:

```clojure
(require '[ants.war :as war])

;; customise any `war/default-config` keys
(def world (war/run {:size [32 32]
                     :ants-per-side 10
                     :ticks 150}))

;; or iterate silently without HUD output
(def final (war/simulate {:ticks 300 :ants-per-side 8}
                         {:hud? false}))
```

### Unit tests

```bash
clj -M:test -m cognitect.test-runner
```

Covers observation, predictive coding, policy selection, world bootstrapping, and run-loop sanity checks. The alias launches the Cognitect test runner so you can re-run individual namespaces when iterating.

### Benchmarks

```bash
clj -M:bench
```

Times three preconfigured grids (5×5, 25×25, 100×100) and writes `out/bench.csv` with scores, duration, and Δscore. Edit `ants.bench/scenarios` to add or tweak variants before re-running the alias.

### Visualizer

```bash
clj -M:viz
```

Opens a Swing window that animates the war simulation using the same world state as the CLI loop (AIF ants in red, classic in charcoal, food in orange, pheromones in green, fallen ants marked with an X at their death spot). The `clj -M:viz` alias prints a scoreboard update to STDOUT every few ticks so you can see progress even when the window steals focus. Pass an integer to the alias (e.g. `clj -M:viz 20`) to change the logging cadence. From a REPL you can customise everything:

```clojure
(require '[ants.visual :as viz])

(viz/visualize {:size [40 40]
                :ants-per-side 12
                :ticks 400}
               {:delay-ms 60   ;; frame delay
                :log? true     ;; print scoreboard lines to the REPL
                :log-every 10  ;; log once every 10 ticks
                :blocking? false})
```

The non-blocking form returns handles to the frame and world atom so you can inspect state while the animation runs.

## Architecture Notes

- **AIF Core**: `ants.aif.core/aif-step` orchestrates observe → perceive → policy. It stays pure and returns diagnostics (`G`, `P`, trace data).
- **Observe**: `ants.aif.observe/g-observe` normalises local food, pheromone, and home proximity channels into `[0,1]` evidence vectors.
- **Perceive**: `ants.aif.perceive/perceive` runs capped predictive-coding micro-steps, adjusts hunger, and anneals action temperature via `ants.aif.affect`.
- **Policy**: `ants.aif.policy/choose-action` evaluates abstract actions by one-step expected free energy and samples a softmax using precision-driven `tau`.
- **Scenario**: `ants.war` wires mirrored hives, food gradients, pheromone evaporation, and handles classic vs. AIF action dispatch.
- **UI**: `ants.ui` exposes a terminal-friendly scoreboard and HUD helpers; rendering stays outside the AIF modules for purity.

Refer to `AGENTS.md` for an agent-state contract and helper semantics.

Observation vector ordering (the `sense->vector` ABI):

```clojure
[:food :pher :food-trace :pher-trace :home-prox :enemy-prox :h :ingest
 :friendly-home :trail-grad :novelty :dist-home :reserve-home :cargo]
```

### Cyber-AIF Armies

You can now swap the classic faction for Futon3-derived cyber-ants. The Futon5
`cyber_ants` operator reads pattern descriptions under `futon3/library/ants/`
and emits configs Futon2 applies on a per-ant basis. Configure the matchup via
`war/run` or the CLI:

```clojure
(war/run {:armies [:cyber :aif]
          :cyber {:pattern :cyber/white-space-scout}
          :ants-per-side 6})
```

Cyber ants reuse the AIF brain but carry pattern-specific `:aif-config` data,
so you can flash alternative behaviour (baseline, white-space scout, hunger ↔
precision coupling) without editing the simulation code. The HUD, scoreboard,
and Swing visualizer automatically display whichever species you choose.

## Configuration

`ants.war/default-config` covers grid size, food/pheromone caps, agents per side, EMA smoothing, tick count, and the hunger system. Override keys via the map passed to `war/run` or CLI args (ticks shorthand).

### Hunger & Hive Reserves

Every ant carries a hunger drive `h ∈ [0,1]`. Metabolic burn, carrying load, and distance from home push hunger up; gathering or depositing food, plus resting on the nest, pull it down. Key knobs live under `:hunger`:

| Key                                            | Meaning                                                                                                                                                                                                    |
|------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `:initial`                                     | Baseline hunger for newborn ants (`0.36` by default).                                                                                                                                                      |
| `:burn`, `:feed`, `:rest`, `:load-pressure`    | Coefficients passed to `affect/tick-hunger` controlling metabolism, food relief, nest comfort, and cargo stress.                                                                                           |
| `:gather-feed`, `:deposit-feed`, `:home-bonus` | Post-action offsets applied when an ant gathers, deposits, or rests at home.                                                                                                                               |
| `:death-threshold`                             | Hunger level that triggers starvation death (`0.995`).                                                                                                                                                     |
| `:queen`                                       | Hive-wide fuel store: `:initial` reserve, base `:burn`, per-ant `:per-ant` upkeep, `:starvation-grace` ticks tolerated at zero, and `:starvation-boost` applied to worker hunger once the pantry is empty. |

Reserves appear in the HUD as `Hive C:## A:##`. When harvest stalls, reserves tick down each turn; once a hive runs dry for longer than its grace period the simulation halts with an `END queen-starved(...)` marker. If every worker dies first you will instead see `END all-ants-dead`. Should Alice fall in battle the run stops immediately with `END alice-dead` and the CLI prints `Game Over (Alice Died)`.

## License

Eclipse Public License (EPL) 1.0. See `LICENSE` for details.
