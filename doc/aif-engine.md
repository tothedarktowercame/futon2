# AIF Engine Contract (Futon2)

This note establishes Futon2 as the shared AIF engine. Domain adapters (ants,
Fulab, Futon5) map their observation/action spaces onto the common engine.

## Core responsibilities

- Maintain belief state and update loop (domain-agnostic).
- Select actions/patterns based on adapter-provided candidates.
- Return summaries that downstream systems can attach to PSR/PUR.

## API surface (library call)

```clojure
(require '[futon2.aif.engine :as engine]
         '[futon2.aif.adapters.fulab :as fulab])

(def eng (engine/new-engine (fulab/new-adapter) {:beliefs {}}))

(engine/select-pattern eng {:decision/id "decision-1"
                            :candidates ["fulab/blast-radius" "fulab/tradeoff-record"]})

(engine/update-beliefs eng {:decision/id "decision-1"
                            :outcome {:status :success}})
```

## Result shapes

Selection result (PSR-like):
```edn
{:decision/id "decision-1"
 :candidates [...]
 :chosen "..."
 :aif {:G-chosen 0.42
       :G-rejected {...}
       :tau 0.65
       :belief-id "..."}}
```

Outcome update (PUR-like):
```edn
{:aif/state {...}
 :aif {:prediction-error 0.12
       :tau-updated 0.71
       :belief-delta {...}}}
```

## Adapter boundaries

- `futon2.aif.adapters.ants` connects the engine to ant world state
  (food/pheromone/grid observations).
- `futon2.aif.adapters.fulab` connects to Fulab agents
  (code/docs/pattern/tool observations).
- `futon2.aif.adapters.futon5-mca` connects to Futon5 search
  (cellular automata state, fitness signals).

Adapters should be thin and map domain observations into the shared engine
belief/observation vocabulary.
