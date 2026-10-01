# Cascade specification v2: support, meets, and generative precedence

Date: 2026-10-01. Status: candidate replacement specification for PROOF-2b.

## Historical reading

The three structures below are related but not identical.

1. The 1968 multi-service-centre language was drawn as a **cascade**: larger
   scale patterns above, smaller scale patterns cascading downward. The
   cascade showed how patterns fit together and interact.
2. In *A City Is Not a Tree*, a **semilattice** describes overlapping systems:
   whenever two units overlap, their intersection is itself a recognisable
   unit. A tree instead makes any two units nested or disjoint. Alexander also
   warned that arbitrary overlap is chaos; it must be the right overlap.
3. A **generative sequence** supplies build order. Alexander later wrote that
   the 1977 language did not by itself reliably generate a good design step by
   step; the creative power lies in the order in which steps unfold.

Sources:

- https://christopher-alexander-ces-archive.org/book/a-pattern-language-which-generates-multi-service-centers/
- https://www.patternlanguage.com/archive/cityisnotatree.html
- https://www.patternlanguage.com/sequences/otherinfo/intro.html

Therefore `cascade = semilattice = build order` is not the specification.

## Policy construction

A policy is constructed for a pinned problem from component patterns in a
pinned library snapshot. Construction produces three typed relations.

### 1. Completion/support

`supports(child, parent)` says that applying or maintaining the child helps
complete the larger parent pattern in this problem. A child may support
several parents. This is the early cascade's larger-to-smaller-scale network,
oriented here from contributor to completed whole.

### 2. Overlap/meet

`meet(left, right) = shared` says that two co-requirements overlap in a
recognisable unit already present in the policy. The meet is not merely an
edge label: it names what must satisfy both requirements. An asserted overlap
without a warranted meet is a typed construction defect. Arbitrary overlap is
not credited.

### 3. Generative precedence

`before(a, b)` says that, for this problem, applying `a` before `b` permits the
whole to unfold while preserving earlier results. It is problem-specific and
must not be inferred from pattern names, directory order, support direction,
or graph endpoint sorting.

The relation is acyclic. Incomparable units may be co-applicable only when the
transition model supports that claim; absence of a precedence edge is not an
assumption of simultaneous success.

## Information-theoretic reading

For fixed component units, the generative order constrains the feasible
execution sequences. Under the declared uniform-over-linear-extensions sample
model, ordering ambiguity is `log2(number of linear extensions)`. A warranted
partial order therefore reduces predictive ambiguity relative to the flat
bag. This enters the ordinary ambiguity term of expected free energy. There is
no edge-count reward.

The result is conditional on evidence for the ordering and on the predictive
model representing feasible executions. Adding false edges makes a wrong
model more certain; it is not useful compression.

## Construction receipt

Every constructed policy records:

- problem bytes and digest;
- library snapshot and digest;
- component pattern ids and source digests;
- support edges with evidence;
- overlap pairs, their meet units, and evidence;
- precedence edges with evidence;
- excluded retrieved patterns and typed reasons;
- the construction algorithm/version;
- the predictive schedule model and its ambiguity calculation.

The policy set is the exact image of this construction. A caller-supplied menu
without the receipt is not a policy set.

## Lean acceptance obligations

1. Component occurrences refer to the pinned library.
2. Support permits multiple parents and is not silently identified with
   precedence.
3. Every declared overlap names a meet in the policy and satisfies the meet
   laws under the declared support/order interpretation.
4. Precedence is acyclic and independently evidenced.
5. The execution kernel never turns every missing precedence edge into an
   assumption of simultaneous success.
6. On matched units and a matched schedule model, the exemplar proves
   `extensions(chain) < extensions(partial) < extensions(flat)` and the
   corresponding entropy inequalities.
7. Adding an unwarranted edge fails provenance even when it lowers entropy.
8. The generated runtime sample and Lean term have equal units, three
   relations, extension count, and ambiguity value.

The existing `CascadeSpec` proves useful carrier facts, provenance facts,
acyclicity and structural deduplication, but it currently conflates the policy
structure with `precedes + overlap` and does not carry support or meet as the
separate relations above. Its three real retractions with empty `precedes`
remain evidence inputs, not completed acceptance of this specification.

