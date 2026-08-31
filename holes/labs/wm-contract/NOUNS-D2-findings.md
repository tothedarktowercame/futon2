# NOUNS-D2 — record-less R-node census findings

The census is `noun-census.edn`. Its named consumer is NOUNS-D3 ordering; it does not create or imply problem records.

## Automatability score before execution

| criterion | score | reason |
|---|---:|---|
| typed ports | 1 | Inputs are the staged-node registry, catalogue, problem-record filenames, and Clojure source; outputs are one EDN census and this note. |
| acceptance named | 1 | Every record-less staged node must have one catalogue line, an implementation presence/absence value, and a carrier candidate or refusal. |
| executable falsifier | 0 | No pre-existing checker validates the census against all four sources; EDN readability and population arithmetic are executable, semantic correspondence is not. |
| evidence consumed | 1 | The EDN names `:wm-nouns/NOUNS-D3-ordering` as consumer. |
| pinned/loud reads | 1 | The two generated p4ng inputs are pinned to commit `4140cc5`; absent implementations and denominator discrepancies are values. |
| bounded/reversible | 1 | Two new files under `holes/labs/wm-contract/`; no generated file, runner, or external state is changed. |
| decisions covered/refusable | 1 | Catalogue/carrier ambiguity is retained as alternatives; the arity and population contradictions are refused. |

Score: **6/7**. It is safe for unattended read-only census work, but not for automatic semantic acceptance; a future generator/checker should close criterion 3.

## Findings

The requested population is not 13. There are 18 staged nodes. Four staged nodes have records (R2, R8, R9, R16); the fifth record, R19, has no stage entry. Therefore **14 staged nodes are record-less**. The census includes all 14 rather than silently sacrificing one to the commissioned count.

All 14 have representative Clojure implementations. That does not mean the catalogue contract is discharged: the census establishes a locus, not conformance. Ordering reflects the cheapest/highest-consequence records first: boundary and evidence carriers (R20, R10, R12), then carried epistemic state (R1, R3, R7), then prediction/scoring/selection, with offline structure learning last.

The carrier question is also under-typed. The charter says “three carrier kinds” but names four: chain, cascade, stack, and route. The EDN therefore records a best candidate plus alternatives rather than inventing a three-way partition. The clearest cases are R1/R7/R11/R14/R15 as stacks, R6/R13/R17 as cascades, R3/R12 as chains, and R10/R20 as routes; R4 and R5 expose overlaps that need a proper carrier type before a record can settle them.

Finally, `R14→TRACE` terminated at a noun absent from both the stage registry and Figure 4. NOUNS-D4 resolved it as an ACT/assurance `:store` member, not an R-node: the implementation writes trace data, while the catalogue defines no TRACE pattern. TRACE remains separate from the 14 record-less R-node population and Figure 4 still does not draw the store.

Spine holding cost (consumer: `wm-nouns` regression watch): **63 declarations contain 31 holes; 14 are bound to passing witnesses and 17 are unbound** (P-validated-R5 5/12, P-glossary-mathematics 0/7, P-R9 4/7, P-R8 3/3, P-R2 2/2; measured from `holes-contract.json` and `checks/witness-registry.edn`, updated by NOUNS-D3 when `cascadeGrainPi` closed on 2026-08-31).
