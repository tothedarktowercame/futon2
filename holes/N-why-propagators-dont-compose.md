# N-why-propagators-dont-compose — the rule is a CA, and composition scrambles its topology

**Provenance:** Joe, 2026-07-17: *"propagators don't seem to compose well with each
other. But here we have seen that they can be composed with design patterns...
composing propagators in a naive way just wiped out all of the lovely EoC/chaorder
that the individual propagators tend to create. So, here's a thought: *why* do they
create this lovely structure? Because what they are doing is 'composing' in a sense
with whatever the original genotype register is, through repeated application."*

Joe's intuition is right and it is **literally** true — not by analogy. This note
shows the mechanism from the source, solves the propagator exactly, answers the
composition question, and records the one anomaly that refutes the simple story.

## 1. `rule-permute` IS a propagator. Not like one. Is one.

`futon5/xenotype/generator.clj:732`:

```clojure
(defn- rule-permute [rule-bits sigma]
  (let [k   (ca/rnd-int 8)                    ; a random neighbourhood
        src (nth ca/truth-table-3 k)
        dst (get sigma src src)               ; sigma(k)
        di  (.indexOf (vec ca/truth-table-3) dst)
        v   (if (= \0 (nth rule-bits k)) \1 \0)]   ; NOT rule[k]
    ... writes v at position di))
```

Read off the semantics:

> **`rule[σ(k)] := ¬rule[k]`**, at a uniformly random `k`.

That is exactly the propagator `g[s(k)] := ν(g[k])` from
`DarkTower/Patterns/Propagator.lean`, with

| propagator | here |
|---|---|
| `g` — the colouring | the rule's **8-bit truth table** |
| `N` — the index set | the 8 neighbourhoods |
| `s` — the shape map | **σ**, the arm |
| `ν` — the value map | **`Bool.not`** |

**This is why the Lean cares about `Bool.not`.** It is not a toy example chosen for
convenience; it is the operator MetaCA actually runs. `identity_has_no_fixed` and
`bool_fixed_exists_iff_hasAlternatingColouring` are statements about this code.

### The consequence that explains everything downstream

**The genotype's truth table is itself an 8-cell CA** — cells = neighbourhoods,
topology = σ, update = ¬. **A CA inside the CA.** Joe's "composing with the original
genotype register through repeated application" is precisely this: the rule is the
state, and σ is its neighbourhood relation.

## 2. Why a propagator is different from blending and from local mutation

This is Joe's actual question, and the answer falls straight out:

| operator | axis it acts on | what it does |
|---|---|---|
| local rule evolution | random bits | unstructured noise, no geometry |
| **blend-cell** | **across cells** (horizontal) | a **contraction** — drives to consensus |
| **rule-permute** | **within one rule** (vertical) | a **structured, sign-flipping coupling** along σ's cycles |

**The propagator is the only operator that uses the rule's internal geometry.**
Blending mixes rules between cells; mutation ignores structure entirely. The
propagator couples bit `k` to bit `σ(k)` *inside a single rule*, with a negation. It
acts on an axis the other two do not touch. That is the whole of "why they are
different" — there is no third mystery.

## 3. The propagator alone, solved exactly (no simulation)

σ is a **permutation**, so `FREE = ∅` — `permutation_free_eq_empty`, "disjoint from
the mechanism". There is no scaffold. The constraint is `rule[σ(k)] = ¬rule[k]` for
all k. Around a cycle of length L: `rule[k] = ¬^L rule[k]`, so:

> **Satisfiable iff every cycle of σ has EVEN length.**

which is `bool_fixed_exists_iff_hasAlternatingColouring` — an alternating 2-colouring
of a cycle exists iff the cycle is even. 256 states, uniform draws: solve it exactly.

| arm | cycle lengths | absorbing states | P(absorbed) | alone |
|---|---|---|---|---|
| `rotate+1` | `[8]` | 2 — `10101010`, `01010101` | **1.000** | freezes |
| `rotate+2` | `[4 4]` | 4 — `11001100` & rotations | **1.000** | freezes |
| `three+five` | `[3 5]` | **0** | 0.000 | churns forever |
| `sigma-5127` | `[1 1 2 2 2]` | **0** | 0.000 | churns forever |
| `identity` | `[1×8]` | **0** | 0.000 | churns forever |

**§1.3's prose is now derived, not asserted.** *"Ordinary mutation is σ = identity — a
random walk, NO ATTRACTOR."* identity's cycles are all length 1, so its constraint is
`rule[k] = ¬rule[k]` — unsatisfiable at every k, so every draw flips a bit and it can
never stop. The sentence was a description; it is a theorem.

## 4. WHY THEY DON'T COMPOSE

The generative property lives in σ's **cycle type**. Cycle type is a **conjugacy**
invariant — it is *not* preserved by composition, because composition is not
conjugation. `σ_A ∘ σ_B`'s cycle structure is unrelated to `σ_A`'s and `σ_B`'s.

**Composing two propagators rewires the inner CA's topology.** The structure you liked
was a property *of that topology*. Change it and the structure is gone — not because
composition is "wrong", but because **there is no homomorphism from cycle type to
cycle type**.

The sharpest case, computed:

```
sigma-5127        cycles [1 1 2 2 2]     structured coupling
sigma-5127²       cycles [1 1 1 1 1 1 1 1]  =  IDENTITY  — pure noise, no coupling
```

**Two structured couplings compose to no coupling at all.** The field stays alive
(identity is UNSAT, it churns) but it is *structureless*. **That is exactly Joe's
observation** — naive composition did not kill the field, it *wiped out the chaorder*
and left the random walk behind.

Measured over all 9 compositions of the three live arms: **0 became SAT**. So
composition does **not** destroy life by manufacturing an attractor. It destroys
*structure* by collapsing coupling toward 1-cycles. The failure mode is **noise, not
death** — which is why it was hard to see.

## 5. Why design patterns DO compose with propagators

**Because a cascade SELECTS; it does not COMPOSE.**

`g : BINS → ACTS` applies **one σ per window, whole and intact**. Its cycle type is
never touched. The cascade is a **case analysis** — a disjunction over conditions —
not a product in the group. Time-multiplexing preserves what multiplication destroys.

This is the payoff of *a policy's domain is the condition*. The cascade absorbs the
condition into its index, so the arms are only ever **chosen**, never **multiplied**.
Selection is the composition operation propagators actually admit.

## 6. Part II was a CONFIRMATION of Joe's finding, not a counterexample

`E-cascade-times` / the published Part II built exactly the multiplication Joe asked
for — `cascade*a`, i.e. `a ∘ σ` pointwise — **which is naive composition**. And it
broke the arms in precisely the way Joe had already seen:

- `cascade*sigma-5127`'s `:rich` arm became `σ∘σ = identity` → **vacuous**, pixel-identical
  to `fixed:identity`.
- Every other product's arms are **unnamed σ's with unrelated cycle types** — which is
  why they carry no evidence. Not because nobody measured them, but because the
  measurement doesn't transfer: **cycle type doesn't compose**.

The result labelled "the rewrite strips evidence from all five boxes" has a mechanism
now. The evidence was about a cycle type, and the product has a different one.

## 7. THE ANOMALY — and it is where blending enters

**`rotate+1` and `rotate+2` both freeze with probability 1.000 alone. In the CA,
`rotate+1` dies 12/12 and `rotate+2` is the BEST arm (0/12, 31.0 terminal rules).**

So "the propagator has an attractor" does **not** predict CA death. The simple story
is refuted by its own best case, and the refutation is informative: **the propagator
never runs to absorption**, because blending re-blends the rule every generation. The
wiring is `rule := rule-permute(blend(pred, self, succ))` — **one propagator step per
generation, on a freshly blended rule**. The propagator is a **bias**, not an absorber.

That is why neither ingredient works alone, which is Joe's own observation from both
ends:

- **propagator alone** → freezes (SAT) or pure churn (UNSAT). Neither is EoC.
- **blending alone** → contracts to consensus → monoculture → death. *"only-blending
  doesn't improve it much."*
- **together** → blending knocks the rule off the propagator's target; the propagator
  pulls back. **Never settles, never diffuses.**

> **EoC is not in the propagator. It is in the TENSION between the propagator's
> attractor and blending's consensus.** The propagator supplies a *target*; blending
> supplies *perturbation*.

### The hypothesis this suggests (NOT yet tested)

**A propagator kills iff its target AGREES with blending's target.**

- `rotate+1`'s attractors are `{10101010, 01010101}` — **2 rules, one orbit,
  complements**. Every cell is biased toward the *same* pair. That is a **consensus**
  bias, and blending also wants consensus. **They cooperate → monoculture → death.**
- `rotate+2`'s attractors are **4 rules across 2 independent cycles**, whose phases can
  differ per cell. That target is **not** consensus. **They compete → EoC.**

This predicts the ordering without reference to transport, and it explains the anomaly
that refutes the simpler theory. **It is a hypothesis. It has not been run.**

### The experiment that would test it

Vary **only** the attractor set while holding cycle parity fixed: find σ's with all-even
cycles and *k* independent cycles, k = 1…4 (attractor set size 2^k). Predicted: death
probability falls monotonically in k, with k=1 (`rotate+1`) dying and k=2 (`rotate+2`)
living. That isolates "attractor agrees with consensus" from "attractor exists", which
is the one thing the exact solve cannot separate.

## Disposition

**Solid** (from source + exact computation): rule-permute is the propagator, ν = not;
the rule's truth table is an 8-cell CA with topology σ; SAT iff all cycles even;
cycle type is not preserved by composition; `sigma-5127² = identity`; §1.3's
"no attractor" is a theorem about 1-cycles.

**Refuted** (by rotate+2, the best arm): "attractor exists → the CA dies". Absorption
alone predicts nothing about the CA, because blending prevents absorption.

**Hypothesis, unrun:** a propagator kills iff its target agrees with blending's
consensus.

**For oxf-claude-2:** `bool_fixed_exists_iff_hasAlternatingColouring` is load-bearing
here and is present at `ce6cc0c3c2`. `settles_if_nu_has_fixed_point` is still absent
from the emitted sha and is now load-bearing for a **fourth** result.
