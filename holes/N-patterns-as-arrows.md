# N-patterns-as-arrows — the categorical substrate for design patterns

> A design pattern is an arrow `W(A) → M(A)`: context-comonad in,
> justification-monad out. The MetaCA tokens are the hom-set of the level
> below, so the pheno→geno→exo→xeno tower is iterated exponentiation, and
> NEXT-STEP is the sign channel by which a pattern stages its own revision.
> One formalism; the 256 rule bytes and the flexiarg corpus are two carriers.

**Provenance:** Joe + claude-6, 2026-07-16, emacs-repl session following the
XTDB#5637 excursion. Continues [[N-strategy-as-computational-object]] (the
Level-1/Level-2 crux) and reads the propagator/exotype results of
`futon5:M-propagators` (branch `M-propagators-2026-07-15`,
`holes/missions/M-propagators.md`) as the worked MetaCA instance.

## 0. The data to fit — three instances of one frame

1. **Flexiarg / WR corpus** (`futon3/library/`, war-room rulings):
   `IF / HOWEVER / THEN / BECAUSE (+ NEXT-STEPS)`, slot fillers in natural
   language, ~1073 patterns.
2. **MetaCA geno rules** (futon5): `IF(left) HOWEVER(right) THEN(next)
   BECAUSE(ego)`, sometimes `NEXT-STEP(pheno)`. Slot fillers are abstract
   tokens that happen to be 8-bit lookup rules — hence computational.
3. **Narrative patterns as tiny War Machines** (Joe, this session): e.g.
   `repl-beats-cli`, where NEXT-STEP is a *preregistered intervention that
   would raise belief in the pattern itself* ("improve REPL background-task
   performance; if good enough, the CLI fallback becomes unnecessary"), and
   once it lands, the next step *changes* — each pattern carries a staged
   queue, a nonstarter.org unit.

The formalism must make (1) and (3) instances of the machinery that already
demonstrably computes in (2), without shoe-horning.

## 1. The base formalism: a pattern is a biKleisli arrow

Fix an ambient category **E** (Set to start; cartesian closed is all we
need). Three parameters:

- **Carrier** `A` — the token alphabet. MetaCA geno level: `A = G`, the 256
  rule bytes. Flexiarg corpus: `A` = the controlled vocabulary (see §5 —
  *not yet finite*, and that is the known gap, not a surprise).
- **Context comonad** `W` on **E** — packages "a token in its context".
  **`W(A) = [S, A]` for a shape MONOID `S`**: the unit of `S` is the ego
  position and its multiplication composes shifts, giving
  `ε(x) = x 1` and `δ(x)(s)(t) = x (s·t)`. The monoid requirement is not
  decoration — it is forced. (*Formalised: `DarkTower/Patterns/DistributiveLaw.lean`,
  where the comonad laws carry `[Monoid S]` as a typeclass; that IS the proof.
  For 1D MetaCA, `S = Multiplicative ℤ`.*)

  **The radius-1 window `A × A × A` is NOT `W`** — it is what a local rule
  *reads* from the context, with `IF = π_left`, `HOWEVER = π_right`,
  `ε = π_ego`. Correction to v0, which set `W(A) = A × A × A` outright: the
  counit laws plus naturality force `δ` to be an associative operation on
  {left, ego, right} with ego as two-sided identity — i.e. a monoid on the
  three positions — and there are **exactly 11** of them. v0 named none, and
  the only shift-like choice (`ℤ/3`) makes the neighbourhood **wrap**: the
  left neighbour's left neighbour becomes the right neighbour, a width-3
  periodic CA rather than a window on a line. Capobianco–Uustalu (cited below)
  already use `A^ℤ` for exactly this reason.

  **The counit is the BECAUSE register**: what extraction returns is the
  self, and the self-state is the justification. (Same role the interface
  object `K` plays in double-pushout rewriting `L ← K → R`: the preserved
  part licenses the rewrite.) The comultiplication `δ : W ⇒ WW` (context of
  contexts) is what cascade wiring quietly uses: to compose local rules you
  need each neighbour's context too — which is precisely why the window
  cannot serve as `W`: from `(a,b,c)` alone you cannot recover `a`'s own
  left neighbour.

  **Open, and it bites the MetaCA specifically.** The engine uses *fixed
  Rule-0 boundaries* (`futon5:propagator_orbit_proof.md`), which are not
  shift-invariant — and shift-invariance is what `δ` needs. So the actual
  MetaCA carrier is neither `A^ℤ` nor `A^(ℤ/n)`. Note the resonance: that same
  boundary is why the orbit proof *rejected* the 0↔1 complement quotient.
  Whether a finite line with fixed boundaries carries a comonad at all is
  unresolved.
- **Effect monad** `M` on **E** — the annotation on output. Minimally the
  writer monad `M(A) = J × A` over a justification monoid `J`; unit = "no
  justification", multiplication = composition of justifications. (**NB for §3**:
  if `Σ` is folded in as `M(A) = (J × Σ) × A`, then **`Σ` must itself be a
  monoid** — writer needs a unit and multiplication in *both* factors. `Σ =
  {bored, interesting}` has no given multiplication, and choosing one is a
  choice, not a discovery. Formalised with `[Monoid K]` throughout.) THEN is
  the `A`-component of the output; BECAUSE-as-produced is the `J`-component.
  (BECAUSE appears twice — as the ego register consumed and the
  justification emitted — and that is right: the rule reads a warrant and
  writes a warrant.)

**Definition (pattern).** A pattern over `(A, W, M)` is an arrow
`f : W(A) → M(A)` in **E** — equivalently an endo-arrow on `A` in the
biKleisli category `BiKl(W, M)`, given a distributive law
`λ : W∘M ⇒ M∘W`.

> **v0 proposed the wrong `λ` and it is now refuted.** v0 said: "collect the
> three justifications and multiply in `J`; canonical when `J` is commutative
> enough". **It is not a distributive law at any `J`.** Collecting violates the
> **counit axiom** on every context with a non-ego point, and *finiteness and
> commutativity do not repair it* — the counterexample is explicit over `ℕ`
> (`collectLambda_counit_counterexample`). The intuition: `ε` extracts the ego,
> so the effect surviving extraction must be the ego's effect alone; a product
> over the whole context is not that.
>
> **The lawful `λ` is `egoLambda`**: transport the whole value context, but emit
> **only the annotation at ego** — `λ(x) = ((x 1).1, fun s => (x s).2)`. All four
> mixed axioms check. *Formalised and built:
> `DarkTower/Patterns/DistributiveLaw.lean`.*
>
> **So neighbour signs cannot ride the writer.** They belong in a separate
> emission/coalgebra layer — which is **§3 of this note**. The formalism refutes
> §1 and the repair was already written in §3; the two just had not been
> introduced. §3's `e : W(A) → Σ` is where the collection that `λ` forbids
> actually lives.

**Composition is the cascade.** `g ∘ f` in `BiKl(W, M)` is defined exactly
when types match; one pattern's THEN feeding another's IF *is* this
composition. Level-1 wiring ([[N-strategy-as-computational-object]]) is the
composition structure; Level-2's "does this THEN genuinely unify with that
IF" is "does this composite exist". A THEN no IF ever consumes — the
mechanically-findable strategic hole — is an arrow with no composable
successor. Justifications accumulate along composites via `J`: the audit
trail of a strategy is the writer output of its cascade.

**The four generalization moves** (what "apply to other tokens and
rule-sets" means, precisely):

| move | mechanism |
|---|---|
| other tokens | change carrier `A`; transport along `g : A → B` by naturality |
| other context shapes | comonad morphism `W' ⇒ W` pulls patterns back (1D nbhd, graph, slot-adjacency of the corpus itself) |
| other effects | monad morphism on `M` (richer `J`, add emission — §4) |
| compose rule-sets | monoids/categories of patterns under biKleisli composition |

Nothing about the 256 tokens is load-bearing anywhere in this table.

## 2. The tower: tokens of level n+1 are the arrows of level n

The reason the MetaCA slot fillers "happen to correspond to 8-bit look-up
rules": they are a hom-set. Let `P = 2` be the pheno alphabet and
`N = W_P(P) ≅ P³` the 8 neighbourhoods. Then

```
G  =  [N, P]  =  hom(W_P(P), P)      |G| = 2⁸ = 256
```

**The geno alphabet is the exponential object of the pheno level.** In a
cartesian closed **E** this is a construction, not a coincidence, and it
iterates — futon5's hierarchy is the tower read off the formalism:

| level | futon5 name | carrier | operator |
|---|---|---|---|
| 0 | pheno | `P` | a rule `f ∈ [N, P]` |
| 1 | geno | `G = [N, P]` | blending / mutation: `W_G(G) → G` |
| 2 | exo | operators on `G` | `switch(sign, σ_A, σ_B)` — propagator compositions |
| 3 | xeno | exotype space | selection/evolution over exotypes |

Each level's *tokens* are the level below's *arrows or operators*, and each
level's dynamics reads *signs* emitted from the bottom (§4).

**Propagators are semantic, by definition.** M-propagators' live hypothesis
(§1.4 there) — σ is "copy the response for neighbourhood `u` into the
response for neighbourhood `σ(u)`, inverted" — becomes the *definition* of
a propagator in this substrate: an endo-operator on `[N, P]` **induced by a
pair of maps** `s : N → N`, `ν : P → P` (here: a permutation of
neighbourhoods and negation), never touching the bit encoding. This is
what makes propagators transportable: the recipe `(s, ν)` is stated at the
(shape, value) level, so it has a meaning over any `[N', P']` — wiring
diagrams, an "ant rule byte" — whereas an operator defined on the encoding
transfers only by accident. **Preregisterable transfer criterion for L5:**
an operator is eligible for transfer iff it factors through `(s, ν)`-form;
the transferred object is the same recipe on the new exponential.

**Why the conjugacy negative had to be true.** L2 found the live twin
`(0 2 4 6)(1 3 5 7)` and dead twin `(0 1 2 3)(4 5 6 7)` exactly conjugate,
so no σ-only property separates them. In this language: properties of σ as
a bare permutation are exactly its conjugation invariants (cycle type), but
σ acts on `[N, P]`, where `N`'s elements have *semantics* through the
evaluation map `ev : [N, P] × N → P` and through which neighbourhoods the
pheno trace actually presents. Conjugating by τ rebases `N`; the dynamics
differ in how τ interacts with `ev` and the trace (τ fixes the homogeneous
neighbourhoods 000/111 and 3-cycles the rest — it scrambles precisely the
part that the coupled system feels). The live/dead distinction is a
property of the *two-level coupled system*, and the formalism says so
before you run anything.

## 3. NEXT-STEP is the sign channel — no shoe-horning required

The apparent mismatch: narratively NEXT-STEP is a preregistered experiment
that updates belief in the pattern; in MetaCA, pheno is one black-or-white
pixel. The resolution is that neither is the output token — **NEXT-STEP is
the channel by which pattern application feeds back into pattern
revision**, and both the pixel and the experiment outcome sit in that
register: an emitted observable, *read as a sign*, that stages what happens
to the pattern next.

**Definition (pattern with agency).** A full flexiarg unit is a triple:

1. base rule `f : W(A) → M(A)` — IF/HOWEVER/THEN/BECAUSE as in §1;
2. emission `e : W(A) → Σ` into a sign alphabet Σ (fold into the writer:
   `M(A) = J × Σ × A`) — the NEXT-STEP register as *observable*;
3. revision policy `ρ : Σ → (Pat → Pat)` where `Pat = hom(W(A), M(A))` —
   each sign stages an operator *on the pattern itself*. The staged
   operators are exactly §2's propagators, one level up.

The unit is then a Mealy-style coalgebra with the pattern in its own state:
`step : (Pat × Q) × W(A) → M(A) × (Pat × Q)`, where `Q` is the staged
queue. *That* is "each design pattern is its own tiny War Machine /
nonstarter unit" — pattern, queue, sign-reader, as one machine.

**The MetaCA instance already runs.** The exotype construction
(M-propagators §2c) is literally `ρ`: `switch(:boredom, σ_A, σ_B)` reads a
per-cell pheno predicate (the pixel-as-sign) and stages which propagator
applies. Baldwin — *bored → mutate, interesting → hold* — is a revision
policy with Σ = {bored, interesting}, and its measured value (mutation rate
falling 0.055 → 0.002 as structure emerges) is what a good NEXT-STEP queue
does: revise hard while the pattern is uninformative, hold while it is
paying. `repl-beats-cli` runs the same machine at narrative speed: the
preregistered improvement is `e`, its outcome is the sign, and on success
`ρ` both *raises the pattern's J-weight* and *advances the queue* (the next
step changes). Active-inference gloss, one line: `e` is the experiment the
pattern runs on the world to reduce uncertainty about itself; Baldwin's
boredom drive is that policy written in elisp in 2014.

**Belief lives in the enrichment.** Take `J` ordered (a quantale of
confidences rather than a free monoid of reasons): homs are then
`J`-weighted, cascade composition combines weights along paths, and `ρ`'s
update on success is an operation on the weight. "Increase our belief in
the design pattern itself" is a monotone map on the enrichment — nothing
more exotic than that is needed to start.

## 4. What the formalism buys (predictions, not vibes)

1. **Decidability has a precise price.** Composition/unification is
   decidable iff the homs are finitely presented — i.e. the controlled
   vocabulary ([[N-strategy-as-computational-object]] §controlled-vocab) is
   not adjacent to this note, it is its presentation. And CT sharpens what
   the vocabulary census must deliver: not synonym clusters alone but a
   finite token set per slot *plus* a unification relation closed under
   composition with identities. That is a checkable spec for the census.
2. **A transfer test you can preregister.** §2's `(s, ν)`-criterion for L5:
   before running ants, exhibit the operator in shape/value form and name
   the ant-domain `N'` and `P'`. (Composes with M-propagators' authority
   gate — this is the *eligibility* gate that precedes it.)
3. **Strategic holes are non-composable arrows** — Level 2's question
   becomes typecheck-shaped once the vocabulary is finite.
4. **The tower is reflexive.** PSR/PUR — records about pattern *use* — are
   level-2 objects (operators on patterns staged by signs from use), so the
   machinery that studies exotypes is the machinery that studies pattern
   governance. One substrate, as hoped.

## 5. Honest gaps

- ~~The distributive law `λ : W∘M ⇒ M∘W` is asserted, not constructed.~~
  **CLOSED 2026-07-16 (codex-6/codex-7, reviewed claude-2) — and it closed as a
  COUNTEREXAMPLE, which §6.1 explicitly invited.** `W = (−)³` was the wrong `W`
  (it is a *window*, not the comonad; `W(A) = [S,A]`, `S` a monoid) and the
  collecting `λ` was the wrong `λ` (refuted; finiteness and commutativity do not
  save it). The lawful `egoLambda` keeps only the ego's annotation, so neighbour
  signs must live in §3's emission layer. `DarkTower/Patterns/DistributiveLaw.lean`,
  `lake build` clean, no `sorry`. **New gap in its place:** does a finite line
  with *fixed boundaries* — the actual MetaCA — carry a comonad at all? See §1.
- For corpus (1) and (3) the carrier is not finite yet — everything in §4.1
  waits on the vocabulary census. The MetaCA instance is the only one where
  the formalism currently *computes* end to end.
- The tower needs cartesian closure; Set is fine, but if the substrate
  moves to typed/weighted settings the exponentials need checking.
- The AIF reading of `e`/`ρ` is a gloss until a revision policy on a
  *narrative* pattern is actually run and measured (the Baldwin rate curve
  is the MetaCA-side evidence; there is no corpus-side twin yet).
- House counterweight (M-propagators §3 applies verbatim): transfer is the
  falsification test, not the victory lap.

## 6. First slices

1. ~~**λ and the axioms**~~ — **DONE 2026-07-16.** It was the counterexample,
   not the equations, and it forced a different `W` as well as a different `λ`.
   See §1 and §5. What remains from this slice: (a) the fixed-boundary comonad
   question; (b) `Σ`-as-monoid is now an explicit hypothesis rather than a
   discovery, and §3 should say which monoid it intends.
2. **The substrate as a protocol** — small Clojure namespace: carrier,
   `extract`/`extend` for `W`, writer ops for `M`, `compose`, and the
   `(s, ν)` propagator constructor. Instantiate twice: MetaCA geno rules
   (must reproduce `:rule-permute` behaviour bit-for-bit — the apparatus in
   M-propagators §2 is the oracle) and the WR-ruling corpus with
   slot-adjacency as `W` (composition = the existing Level-1 cascade).
   Same `compose`, two corpora — the claim made executable.
3. **One narrative unit end-to-end** — write `repl-beats-cli` as the triple
   `(f, e, ρ)` with its real staged queue, and run one revision step when
   the background-task improvement lands. The first pattern to become a
   war machine *formally*.

## Cross-references

- [[N-strategy-as-computational-object]] — the Level-1/Level-2 crux this
  note gives a semantics for; controlled vocabulary as presentation.
- `futon5:M-propagators` (branch `M-propagators-2026-07-15`) — the worked
  instance: propagator family, exotypes/switch, Baldwin rate curve,
  conjugacy negative, transfer gates.
- `futon3a/holes/labs/M-memes-arrows/` — the slot-reading machinery that
  would populate the homs from the NL corpus.
- Capobianco–Uustalu, *A categorical outlook on cellular automata* (CA as
  co-Kleisli arrows of the context comonad) — the base-level prior art;
  the tower and the sign/revision layer are the additions here.

## Disposition

A **note**: it fixes the formalism and names its gaps; it does not scope
the build. Promotion path: slice 1 (λ) into this note; slice 2 as an
`E-pattern-substrate` lab under futon2 or futon5; slice 3 as the first
PSR/PUR-adjacent pilot.
