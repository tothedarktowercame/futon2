# MMCA candidates for von Neumann's five state classes

Deliverable of T-ladder-03-mmca-von-neumann-mirror. One row per class:
the MMCA candidate, the locator that supports it, and whether that support
is a computed result (someone ran the enumeration/measurement and the
number is on the page) or a reading (an interpretation of code or prose,
no computation backs it directly). Locators are given relative to the
sandbox root `/home/joe/wm-sandbox/code`, where both cited trees live.
Von Neumann class sizes follow the ticket (quiescent 1, ordinary
transmission 8, special transmission 8, confluent 4, sensitised 8).

| von Neumann class | MMCA candidate | Locator | Computed result or reading |
|---|---|---|---|
| Quiescent (1 state: the unexcited ground) | Rule byte **178** — a blank medium: fixed under all-even operators, entropy-0 quiescent language ("only uniform-ish words"), the material laid where nothing should happen | `futon5/holes/tech-notes/TN-fable-construction-reframe.md` §3, lines 150–151 ("178, 177, 154, 166, 114, 102… — entropy 0, only uniform-ish words: blank media") | computed result (TN's exact enumeration of the fixed set and per-medium entropies, sanity-checked against `grid.clj`'s documented immune-byte counts) |
| Ordinary transmission (8 states: passive signal carriers) | Rule byte **90** — the additive XOR rule, "fixed physics, perpetually active states, perturbations propagate at speed 1 in both directions and superpose linearly": a wire, not a memory | `futon5/holes/tech-notes/TN-fable-construction-reframe.md` §3, lines 146–149 ("90 — … Not a memory. **A wire.**") | computed result (TN's quiescent-language computation; entropy 0 is measured, not asserted) |
| Special transmission (8 states: the actively destructive channel) | The **inverted propagator write**: `propagate-at` with `invert?` — the rule-layer write channel whose default arm flips the written bit, the disruptive counterpart to ordinary state-layer dynamics; `genotype-step` applies it once per cell per step | `mmca-clj/src/mmca/core.clj` lines 92–108 (`propagate-at`, incl. the `invert?` default `true` on line 95) and lines 110–130 (`genotype-step`) | reading (structural reading of the dynamics' source; the destructiveness is an interpretation of `bit-xor value 1`, no computation of a destructiveness property was run) |
| Confluent (4 states: crossing + storage junction, delay and combine signals) | Rule byte **77** — the storage medium: quiescent language with entropy ≈ 0.694 bits/cell, self-settling in ≤ 6 steps, perturbation-confining to depth ≤ 1; 77-regions freeze content words at junctions between 90-wires | `futon5/holes/tech-notes/TN-fable-construction-reframe.md` §3, lines 138–144 ("77 — … Self-settling, positive-capacity, perturbation-confining. The medium.") | computed result (TN's enumeration, 2000/2000 settling runs and 500-trial perturbation measurements) |
| Sensitised (8 states: intermediates through which a quiescent cell becomes any other type) | **none** | — | — |

## "none" rows as open questions

1. **Sensitised — none.** The MMCA rule layer has no intermediate state
   through which a quiescent (blank-medium) cell turns into any other
   type. Type change on the rule layer happens by *operator
   substitution* (a different writing applied by the propagator), which
   is stochastic rejection-sampling from outside the cell, not a
   transient state the cell passes through (TN §2, "Write-by-operator-
   substitution exists but is stochastic"); and the state layer under a
   frozen medium has no excitability ladder either — byte 77's response
   to a single flip stays within ring-distance 1. An open question: is
   there any MMCA reading (e.g. under exotype churn) in which a cell
   carries an identifiable *intermediate* role between blank and wired?
