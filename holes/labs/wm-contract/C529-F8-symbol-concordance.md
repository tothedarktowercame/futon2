# C529 — F8 symbol concordance census

Date: 2026-09-05

This slice adds `symbol-concordance.edn`: 25 rows for the distinct
`:defines`/`:exogenous` symbols in `aif-equations.edn`, plus five additional
pointer-backed readings of those symbols.  It is a census, not a notation
ruling, and it adds no checker.

## Reproduced premises

The committed independent probe at
`runs/F8-symbol-concordance/independent-probe.bb` reports 18 defined symbols,
7 exogenous symbols, 25 in their union, 22 imports, no unresolved import, and
the sole registry case-fold collision `Pi`/`pi`.  The source rows are
`aif-equations.edn:67-217`.

The glossary uses capital `Pi` at `p4ng/sec-glossary.tex:15`, `:17`, `:19`,
and `:39`: evidence precision is explicit at `:17`, while the policy-space
reading `Pi_H(U)` is at `:39`.  `Pi_feasible` is at `:47`, inside a display
whose dollar delimiters are on other lines, so the probe's same-line math-span
scanner does not report it.  The glossary has no `gamma`; policy precision
`gamma = 1/beta` is instead a cross-source precision reading documented at
`C461-beta-gamma-discovery.md:14-45`.  It is not a third capital-Pi use in the
glossary.

The other supplied readings also reproduce: observation-model `A` at
`p4ng/sec-glossary.tex:31` versus Dirichlet posterior `A` at `:58`; registry
depth `T` at `aif-equations.edn:161-165` versus aliveness intensity at
`p4ng/sec-glossary.tex:54`; entropy operator, policy horizon and harmony uses
of `H` at `:21`, `:39`, and `:54`; and GFlowNet inverse temperature `beta` at
`:66`.  The last two are not concordance rows because `H` and `beta` are not
members of the registry's 25-symbol bound.

The runtime collision also reproduces.  Prediction error emits its
likelihood-derived value under `:precision` in
`src/futon2/aif/free_energy.clj:252-278`; `weighted-error` then preserves that
as `:per-call-precision` and overwrites `:precision` with R7's history-derived
value at `src/futon2/aif/precision.clj:212-233`.

## Relationship to the prior notation census

`glossary-assurance-summary.edn:9-10` closes the earlier census with seven
entries.  Its individual bases are: preference `C`,
`C252-preference-distribution-binding.md:5-8`; predictive `Q(o|pi)`,
`C257-predictive-outcome-kernel-binding.md:5-8`; transition `B`,
`C261-transition-kernel-binding.md:5-8`; parameter-prior `Q(theta|pi)`,
`C265-parameter-prior-kernel-binding.md:5-9`; parameter-posterior
`Q(theta|o,pi)`, `C270-parameter-posterior-kernel-binding.md:5-10`; reduction
`DeltaF`, `C277-model-reduction-free-energy-change-binding.md:3-8`; and
observation `o`, `C282-observation-vector-binding.md:3-8`.

This registry carries `C`, `Q(o|pi)`, `B`, `DeltaF`, and `o`.  It does not
carry the two parameter-kernel Q forms because they are not registry
`:defines` or `:exogenous` symbols.  The new Pi/pi, A, T, and runtime-precision
collisions were absent from the prior seven because that census bound the Lean
glossary carrier bindings, while this slice compares the AIF equation registry,
paper notation, and runtime keys.  The `F`/`Delta F` row restates the prior
NOUNS-D3 family rather than claiming a newly discovered collision.

## Explicit absences

Rows use reasoned absence values rather than guessed identifiers.  There is no
registry Lean identifier for policy set `pi`, observation model `A`, transition
model `B`, preference `C`, or habit prior `E`; no separate Lean carrier for
stack-defined `alpha` or `eps0`; and no War Machine identifier for exogenous
`world`.  The registry gives no code site for `pi`; `alpha` is a local annealing
expression rather than a var; and `world` is an exogenous boundary rather than
a single var.  Glossary math-span searches found no `F-pi`, `eps0`, or world
symbol.  The alternate policy-space Pi and aliveness T readings have no
corresponding machine identifier or single runtime var found by the bounded
search.  The policy-free-energy row has no glossary symbol, and the registry's
variational-F note names no single runtime producer.

## Out of bound, proposed for slice 2

The packet's `H` and GFlowNet `beta` examples are real but contradict its
literal row bound: neither bare symbol occurs among the 25 registry symbols.
The same is true of policy-precision `gamma` and the two parameter-kernel Q
forms.  A checker should decide explicitly whether its refusal domain is
case-fold collisions only, runtime-key reuse, or broader reading collisions;
mixing those keys without declaring the comparison relation would make a
`gamma`/`Pi` finding look like a spelling collision when it is a conceptual
precision collision.

## For slice 2

Check first that every `:collisions/:members` id resolves to exactly one symbol
row, that case-fold groups are recomputed rather than trusted, and that
runtime-key collisions use the runtime key rather than the rows' display
symbols.  The checker must not turn this census into a ruling about which
reading wins.
