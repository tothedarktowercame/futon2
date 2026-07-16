# N-strategy-as-computational-object

> The pattern template is a typed frame, so a strategy cascade can be read
> *formally* off the patterns alone. What turns that formal description into a
> *computational* object is reading **inside** the slots — following how terms
> like "inhabit" or "reward" flow through the frame. The first is done; the
> second is the frontier. This note names the distinction, because it is the
> crux of everything the stack has been doing.

**Provenance:** Joe, 2026-07-14, during the "Fabled Sessions" strategy-cascade
discussion (see [`../../futon3/holes/war-bulletin-12.md`](../../futon3/holes/war-bulletin-12.md)
§strategy-#4 and its §9 companion in `M-self-documenting-stack`). Triggered by
the observation that the War Room's WR-1…WR-22 decisions are already typed
argument-objects, so the strategy layer that has run since mid-February can be
lifted into the same cascade grammar as the forward `backlog-cascade` and the
live `pipeline-pattern-cascade`.

## Level 0 — the template is a typed frame

The stack's design-pattern format (`*.flexiarg`, `futon3/library/`) has named
slots. Its argument core is:

```
! conclusion
  + context
  + IF        — the goal / precondition the pattern serves
  + HOWEVER   — the obstacle / default-instinct it corrects
  + THEN      — the move it prescribes
  + BECAUSE   — the justification (+ nested evidence)
  + NEXT-STEPS
```

A War Room ruling (`WR-n: IF / HOWEVER / THEN / BECAUSE / Evidence`) is a
**flexiarg instance restricted to the argument core.** Across the library the
core slots are the dominant vocabulary (`because:` 162, `when:` 141, `if:` 133,
`then:` 130, `however:` 128). So patterns, WR-rulings, and mission-scoped
arguments are all instances of *one* typed frame.

## Level 1 — reading patterns formally (DONE): a cascade from the patterns alone

Because the frame is typed, a pattern-instance is a **box** and the slots give
the **wires** for free: one instance's `THEN` / `NEXT-STEPS` is a candidate
`IF` / `context` for another. Build the graph over the WR-ruling corpus and you
get a **bona fide strategy cascade** — 22 typed rulings, wired by slot-adjacency,
with ψ read from the `conclusion`/`IF` slots and its drift visible across eras
(Feb *prove-the-stack* → Apr **WR-4 inhabit-before-building** → Jun **B11
identity-governance** → Jul **B12 reward-is-the-binding-constraint**). This is
*structural* formality: the graph is real, machine-readable, and not a prose
gloss. It is the same construction as [[N-backlog-as-cascade]] /
[[HOWTO-backlog-cascade]], applied to the retrospective corpus instead of the
prospective one.

**This much we can do today.** The three cascades — retrospective (bulletins),
live-actual (`pipeline-pattern-cascade-live.html`), forward
(`backlog-cascade-merged-v0`) — compose on the one canonical mission-node
identity B11 established (171 nodes, 0 conflicts).

## Level 2 — reading *inside* the slots (THE FRONTIER): strategy becomes computational

Level 1 treats each slot as an opaque string. The slot *fillers* are still
natural language: `THEN: "inhabit the surfaces that exist rather than building
new ones"`. The graph knows the ruling exists and roughly what it points at; it
does **not** know that the **term "inhabit"** asserted in WR-4's `THEN` is the
same term that constitutes an agent's world in the `umwelt-not-architecture`
pattern (whose own `BECAUSE` argues for *inhabitants*, not *RPC clients*), nor
that it re-enters as a precondition three bulletins later.

To make the strategy a computational object you have to **read inside the
slots** — lift the terms to first-class and track their **flow**:

- a term is *introduced* in some slot (reward enters at B11's ARGUE);
- it *transforms* across the cascade (reward: candidate signal → measured
  honest-negative in futon3a → **the binding constraint** at B12);
- it *discharges or fails to discharge* a later obligation (does WR-4's `THEN`
  "inhabit" actually satisfy the `IF` of the missions that cite it?).

When you can follow a term through the frame, the cascade stops being a
**description of** a strategy and becomes a **strategy you can compute over**:
you can ask whether a ruling's `THEN` genuinely unifies with a downstream `IF`,
whether "reward" keeps one referent or silently forks, where a term dead-ends
(a `THEN` no `IF` ever consumes = a strategic hole, mechanically found). ψ stops
being a label and becomes a *value the cascade carries and updates.*

## Why this is the crux

The proof cascade and the strategy cascade are **the same kind of object**; the
only difference is which corpus the slot-reader runs on. Everything the stack
builds — patterns, missions, proofs, WR-rulings — is typed argument-frames. If
reading-inside-the-slots works, it works uniformly: the machine that formalizes
a proof obligation is the machine that formalizes a strategic commitment. That
is why this is not one more feature but the spine.

## What already exists for Level 2 — and the honest gap

Level 2 is not un-imagined; its machinery has been **built and exercised on the
mission/proof corpus** (`futon3a/holes/labs/M-memes-arrows/`):

- **`fold_engine.clj`** — honest NL→rule extraction: turning prose slot-fillers
  into typed rules. This *is* reading inside slots. (See [[M-fold-ansatz]],
  `E-fold-engine`.)
- **`obligation_accuracy.py`** — obligation-unification, tiered + directional:
  does a discharge-term (a `THEN`) unify with an obligation (an `IF`)? This *is*
  term-flow across the frame.
- **`wiring_corpus.py` + `reward_v0/v1/v2.py`** — the "wiring corpus as
  compositional memory" (p4ng's *fourth memory*): the corpus of how
  discharge-terms wire to obligation-terms, used to learn a reward over them.

**The honest gap** (house discipline — name it, don't bury it): this machinery
has been run on the **fold-turn / proof corpus**, never on the **strategy /
WR-ruling corpus**. And its measured transfer is *bounded*, not free — reward_v2
came in **below null** on the mission corpus, and p4ng records a one-label-wide
transfer shortfall. So "point the slot-reader at the bulletins" is a genuine
research step with its own falsifiable risk, not a switch-flip. What is new here
is the *claim that the corpora are the same type* — that the strategy layer is
eligible for exactly this treatment.

## Controlled vocabulary — the generative face of Level 2

*(Joe, 2026-07-14, once the pattern corpus was indexed into futon1b.)*

Level 2 as stated above is *analytic*: observe how a term flows. Its
**generative** twin is the payoff: **flow the prose patterns toward a controlled
vocabulary — still written in English** — so each pattern is at once a
natural-language object (readable, evocative) and a formal object (its slot
terms drawn from a finite, canonical set). The formality lives not in escaping
to notation but in **controlled diction**: the pattern stays English; the
machine sees canonical terms. "Still English" is the whole trick — and a
controlled vocabulary is exactly the finite term-set over which slot-flow (does
this `THEN` unify a downstream `IF`) becomes *decidable*. Controlled vocabulary
is therefore not adjacent to Level 2; it is the mechanism that makes it
tractable.

**The text index is the missing middle layer.** Without it you cannot see the
vocabulary, so convergence is guesswork. With slots indexed as columns/edges
(now in futon1b as `code/v05/pattern-slot` + `pattern/<slot>` props), the layer
runs both directions:
- **induce (upward):** census the terms actually used per slot across the
  ~1073-pattern corpus → the controlled vocabulary is *induced from the corpus,
  not imposed on it*;
- **flow (downward):** rewrite each slot toward its canonical term
  (inhabit/dwell/reside → one; reward/reinforce → one), keeping the sentence
  natural.

Measure → converge → re-measure — a loop that was impossible before the index.

**futon5's under-powered precedent (iching/iiching).** Those patterns were the
exemplars because they already lived at the *formal* pole — their slots use a
genuinely controlled diction (`:mix-mode`, `:bit-order`, `:word-variation`,
`:tier`, `:program-kind`: finite, machine-runnable). But they are MMCA *program*
specs, not readable design patterns; and the prose patterns (umwelt, the
gauntlet set) sit at the *NL* pole with free diction. Nothing connected the two
poles — no shared space in which to ask "how far is this prose pattern from the
controlled ideal, and what is the path?" So futon5 could point at iching and say
*"like that, but for everything,"* and stop. The corpus-wide text index is the
connective tissue: prose and controlled-vocab patterns finally occupy one
queryable space, turning "flow this toward that" from a gesture into a
measurable operation.

**First empirical step:** a per-slot term census (especially `THEN`, the
prescriptive move) → the raw material from which the controlled vocabulary is
induced, and the synonymy clusters that convergence would collapse.

## Worked micro-example (two term-flows to try first)

1. **"inhabit"** — introduced WR-4 (`THEN`, 2026-04-10, B6); grounded by the
   `umwelt-not-architecture` pattern's `BECAUSE`; test whether the missions that
   cite WR-4 have `IF`-slots it actually unifies with, or whether "inhabit"
   dead-ends (a strategic hole).
2. **"reward"** — introduced B11 (ARGUE); transformed to *honest-negative*
   (futon3a reward_v2 < null); crowned *the binding constraint* at B12 (from the
   40-flight assessment). A term whose referent is stable across three eras but
   whose *value* inverts — the exact thing Level 1 cannot see and Level 2 can.

## Cross-references

- Forward face: [[N-backlog-as-cascade]], [[N-multi-cascade-strategy]],
  [[HOWTO-backlog-cascade]]; artifact
  `futon7/holes/M-futon-forward-model.backlog-cascade-merged-v0-brief.html`.
- Live-actual face: `futon3c/holes/excursions/pipeline-pattern-cascade-live.html`
  (the durable agent→mission clock + forward-model ROI layer; B11's namesake).
- Retrospective face: `futon3/holes/war-room.md` (WR-1…WR-22) +
  `war-bulletin-{1..12}.md`.
- Slot-reading machinery: `futon3a/holes/labs/M-memes-arrows/{fold_engine.clj,
  obligation_accuracy.py, wiring_corpus.py, reward_v2.py}`; [[M-fold-ansatz]],
  [[M-marks-to-labels]]; p4ng `main-2026.tex` (fourth memory, discharge-trained
  proposal).
- Self-documenting home: `futon7/holes/M-self-documenting-stack.md` §9 (WM-owned
  bulletins — the retrospective face this note formalizes).

## Disposition

A **note**, not a mission. It names the methodological crux and the two levels;
it does not scope the Level-2 build. Concrete first slice, when promoted: lift
the WR-ruling corpus into the flexiarg-typed cascade (Level 1, cheap — the
rulings are already instances), then run `obligation_accuracy` / `fold_engine`
over that corpus (Level 2, real research) starting with the two term-flows
above. If it holds, the strategy layer is the first non-proof corpus to become
a computational object.
