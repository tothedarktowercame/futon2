# AGENTS

This document is the contract for agent behavior in **clj-ants-aif**. It lets you (and Codex) work on agent brains without touching rendering or world plumbing.

## TL;DR
- Agents are **ants** whose *state is the grid location* stored in an **agent** (Clojure `agent`).
- The ant's *attributes* (dir, species, brain, latent AIF state) live in the **cell map** at that location: `@(place loc) => {:ant {...} :food ... :pher ... :home ...}`.
- A brain is a function that receives `loc` and returns the *next* `loc`, while mutating the world inside a `dosync` transaction using helpers (`move`, `turn`, `take-food`, `drop-food`).

## Species & Brains

- **Classic (black)**: rule-based `behave-classic` (Rich Hickey’s original).
- **AIF (red)**: `behave-aif` → calls `(aif-step world loc ant)` which performs: observe → perceive (micro-steps) → evaluate actions (expected free energy) → act.

Ant map keys (always present):
```clojure
{:dir int               ; 0..7
 :species :classic|:aif
 :brain   :classic|:aif

 ;; AIF-only (created lazily by ensure-aif-state)
 :mu   {:pos [x y] :goal [gx gy] :h double}   ; latent beliefs
 :prec {:Pi-o {:food double :pher double :h double}
        :tau double}}                         ; action temperature
