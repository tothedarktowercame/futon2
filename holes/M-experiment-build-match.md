# M-experiment-build-match — register experiments the way we already register folds

**Opened:** 2026-08-01. **Owner:** Claude (claude-4). **Operator:** Joe.
**Status:** SPEC. Deliberately named after `M-actuator-spec-build-match.md`,
which is the same idea one domain over.

## The observation

Joe, 2026-08-01: *"rather than hotfixing, we would register the design up front
(along with any changes) … an even more ideal circumstance would be if we had
the Lean system and the Clojure implementation correlated more strongly — the
existing CLean formalism we created along with malli and core.logic might give
us some of the bridge."*

**The bridge is not hypothetical. It is built, it is live, and today's
experiments used none of it.**

## What already exists

`futon2/src/futon2/aif/operational_witness.clj` states the architecture in its
own docstring:

```
interface ✓  clean_argcheck   (the spec is a well-formed typed composition)
structure ✓  build-match      (the build inhabits the composition)
BEHAVIOUR    a logic relation that holds against the live TRANSITION
```

| layer | mechanism | where |
|---|---|---|
| 1 interface | `clean_to_lean.py` renders `*.clean.edn` → Lean; **the render is the gate** — it builds 0-sorry iff the CLean is well-formed | `futon6/scripts/clean_to_lean.py`, `DarkTower/CLeanProofs.lean` |
| 2 structure | `build-match` — every box's `:produces` inhabited in the substrate, from **reviewed** bindings, never builder-chosen | `code_build_match.clj`, `actuator_a3.clj`, `full_loop_runner.clj`, `fold_realized.clj` |
| 3 behaviour | core.logic relations over `(before, event, after)`, run FORWARD against the live transition — *"ungameable in A3's sense: a goal either succeeds against reality or it does not"* | `operational_witness.clj` (5 relations) |

All three are working. All three were built for **folds and proofs**. Nobody
generalised them to **experiments**, so an experiment is currently the least
verified artifact the stack produces.

## Today's three failures, each mapped to the layer that would have caught it

**1. The registration did not match the running experiment** (codex-8's finding
(d)): six arms running, three registered, `no-info-gain` absent from the Lean,
scenarios and seeds absent entirely.

→ **Layer 2, build-match.** The harness config must *inhabit* the registered
structure: every registered arm bound to a running arm, every running arm
registered. This fails at dispatch, not at review. Note the existing design's
discipline transfers directly — the bindings are *reviewed, never
builder-chosen*, which is exactly the property a self-registering harness would
otherwise lose.

**2. The original Slice 5 ablated a term that cancels**, so its contrast could
not move in any environment. Three runs chased it, adding a harsher environment
and then a scarcity condition.

→ **Layer 1, the render gate.** A registration that declares an axis, where the
render requires navigability, does not build. `Slice5Preregistration.lean` now
proves this *after the fact* as `original_obligation_undischargeable`. Under
layer 1 it would have been a build failure in July.

**3. The reviewer's own re-run comparison keyed on `:food-seed` alone**, which
appears once per arm, so it compared arbitrary arms and could not have detected
a mismatch. It reported a confident figure twice.

→ **Layer 3, operational witness.** A checker expressed as a core.logic relation
over `(registered-design, run-artifact, verdict)` names its tuple. A relation
keyed on `(scenario, arm, seed)` cannot silently collapse arms, because the
collapse would have to be written down. The current arrangement — an ad-hoc
script, unregistered, named after the thing it was supposed to do — is precisely
what layer 3 exists to replace.

## The architectural inversion that matters

Today the Lean was **hand-written from prose**, and the Clojure was written
separately. They diverged, and the divergence was found by a reviewer reading
both. That is the failure mode the whole stack is designed to remove.

The fix is direction, not effort:

```
                 experiment.clean.edn          <- the single source
                   /                \
        clean_to_lean.py          malli schema
             |                          |
     generated Lean                harness config
     (render = gate)            (validation = gate)
                   \                /
                 core.logic relation over the run
                       (forward = gate)
```

**If both the Lean and the harness derive from one EDN registration, divergence
is impossible by construction rather than detectable by review.** That is what
"correlated more strongly" means concretely, and it is the same move
`clean_to_lean.py` already makes for proofs.

## What is missing, precisely

1. **An experiment-shaped CLean schema.** `*.clean.edn` currently describes proof
   spines (boxes, `:produces`, method tags). An experiment needs arms, axes,
   scenarios, seed generators, stop rules, decision rule. This is new vocabulary
   in an existing format, not a new format.
2. **The render's navigability gate.** `clean_to_lean.py` must emit the
   `Axis`/`Registration` values such that a constant-score axis fails to build.
   The Lean side already has the theorem; the generator does not yet call it.
3. **A malli schema for the harness config**, derived from the same EDN, so the
   runner refuses to start on an unregistered arm. malli is already in the stack
   (`futon3c/.../shapes.clj`, `mission_shapes.clj`, `war_machine.clj`).
4. **Register the checker.** The verification comparison becomes a relation in
   `operational_witness.clj` rather than a script in `/tmp`.
5. **Amendments are registrations too.** Joe's *"along with any changes"* is the
   load-bearing clause. Slice 5 was amended three times in July and
   re-specified once today; none of those amendments is a registered artifact.

## The cheapest first slice

**S1: register Slice 5's confirmation run** — the one codex-8 says can still be
honestly preregistered with disjoint fresh seeds — as `experiment.clean.edn`,
render it, and make the harness validate against it. One experiment, all three
layers, end to end. If it works the vocabulary is proven; if it does not, the
gap is specific rather than architectural.

**Deliberately not first:** retrofitting the runs already completed. Those are
pilots and should stay labelled as such.

## Why this matters beyond tidiness

`M-aif-stack.md` argues that AIF implementations accrete inert machinery —
quantities computed, canonically named, and annihilated downstream — and that
this is invisible to static audit. Today produced a fourth instance, and it was
*ours*: a verification instrument that computed a number, was named after the
thing it was meant to check, and did not check it.

If the pattern is real, then reviewers and their tools are subject to it too, and
no amount of care fixes that. What fixes it is **making the check derive from
the same source as the thing checked**, so that a checker which stops checking
stops building.

---

## The structural finding (claude-7, 2026-08-01)

Six corrections to the CLean experiment vocabulary landed in one afternoon, all
from an agent **drafting against the format** rather than reading it. The last
one is the one that generalises.

I wrote, in one message, that a disposition block "recording the mechanism while
missing the hazard it creates" would be the failure class we had spent the
afternoon naming — and in the same message built exactly that, in mirror. My
`:original-observation` field guards the control arm against inflation and is
silent about the treatment arm, which the identical mechanism deflates. A
disposition that recorded the adaptivity, retained the original, and still
pooled the rescue units **would have passed my own block.**

claude-7's reading, which I think is right and is stronger than "we were
careless":

> A guard authored in the same message that named the class it then instantiated
> is evidence that this failure mode is **structural**: one party drafting rules
> cannot see the mirror image of the hazard they just guarded.

Their supporting instances, from a different repository:

- V2 §7.4 — neither reviewer catches what the runner leg catches;
- today's twin false-spec incident on the sorry loop;
- and now this.

### The consequence, which is not "review harder"

General review did not catch the mirror. What caught it was **someone trying to
use the guard for a real design.** claude-7 was drafting a cohort registration
and hit the field that would have biased their treatment arm.

So: **guards are validated by use, not by review.** That is a different remedy
from more careful authorship or a second reader, and it is testable — it
predicts that a formalism reviewed by ten people and used by none will carry
mirror-hazards, and that the first genuine user will find them at a rate that
then drops sharply.

Six corrections from the first user, in one afternoon, is the first data point.

### Relation to the mission's other hypothesis

`M-aif-stack.md` argues that AIF implementations accrete inert machinery
invisible to static audit — quantities computed, canonically named, annihilated
downstream. This is its sibling at the level of *rules rather than code*: a
guard that is authored, correctly named, and blind on one side. Both say that
inspection of an artifact by its author cannot establish that the artifact does
what it is named for; only exercising it against reality can.

That is also, precisely, why the three-layer architecture exists. The layers are
not three reviews. They are three *uses*.

## Both ends of an experiment, typed (claude-7, 2026-08-01)

The parent change here and V3's `ValidatedTrace` refactor turn out to be the
same move in two formalisms, and neither party noticed until the ledgers were
compared:

| | governs | mechanism |
|---|---|---|
| `Discharged`-by-role (this repo) | what the experiment may **claim** | a positive-control arm's axis obliges a proof of `¬ Navigable`; a treatment's obliges `Navigable`. Neither is free. |
| `classify : ValidatedTrace → Outcome` (V3) | what its data may **conclude** | an unvalidated trace cannot typecheck into a verdict |

Both replace a docstring with a type. Both were arrived at independently, on the
same day, in response to the same failure class — *semantics living where
enforcement isn't*.

claude-7's distinction is the useful part: **the renderer's refusal polices the
pipeline; `Discharged` polices the artifact.** A hand-written registration that
never touches the generator hits the same wall, because the obligation is in the
type rather than in the toolchain's good behaviour. That is why the laundering
attack was the right test — *"the wrong proof burden cannot be discharged"* is a
stronger sentence than *"the tool said no."*

**Registration #3 is where they meet in one artifact**: V3's E2 confirmation,
typed at both ends, with the adversarial fixtures from both repositories as its
regression suite. Registration types on the front, trace types on the back, and
the experiment sealed at both.

Worth noting what this predicts. If the class is real, then every experiment
apparatus has exactly two places where prose can substitute for enforcement —
the claim it registers and the conclusion it licenses — and closing one leaves
the other open. We each closed one, separately, without seeing the other half.


## Where the type stops and the renderer starts (2026-08-01, consolidated)

Three bends were closed tonight at the type level, which is the right place.
But three *other* constraints ended up in the renderer, and the pattern is now
clear enough to name rather than fix one at a time.

| constraint | type guarantees | renderer guarantees |
|---|---|---|
| `NamedEndpoint` | the name is non-empty | it resolves to a declared `:role :primary` endpoint |
| `Axis.onViolation` | `Option`, **defaulting to `none`** | required for positive-control axes |
| `:environment`, endpoint value types, `:decision` totality | nothing | **nothing** — read zero times |

The third row is claude-7's audit finding: those slots are *correctly permissive*
— a slot that accepts anything is strictly better than one that accepts only
simulations, and it is why a corpus-and-revision "environment" passes at all. But
permissive is not the same as checked, and **a validated slot and an unvalidated
one are indistinguishable in a rendered registration.**

That is the whole problem in one line. "It rendered" currently means *the fields
the renderer happens to inspect were well-formed*, and nothing in the artifact
says which those were. Our 10x10 grid read as an environment that had been
considered. It had been read zero times.

### Two fixes, and the second is the one that generalises

**(a) Migrate load-bearing burdens into constructors.** `VariationPlan` is the
model: an inductive whose every constructor carries its obligation, with the
docstring stating *"there is no constructor which merely suppresses either
burden."* `onViolation` should live on a positive-control axis constructor that
requires it, not as an `Option` defaulting to `none` — a default is how Slice 5's
disposition silently becomes everyone's.

**(b) The render should emit its own validation coverage.** Which slots it
validated, which it passed through unchecked. Then a green registration carries
an honest account of what green covers, and a reader sees that `:environment` was
*unexamined* rather than *approved*.

(b) is the better investment because it does not try to eliminate the boundary —
it makes it visible. Some of this genuinely cannot be closed: the gate cannot
know that food-seed arithmetic is nonsense for a theorem prover, which is why the
last defence against a fabricated registration tonight was an agent declining to
fabricate. A gate whose scope is invisible reads as total. One that reports its
own scope reads as what it is.

### The count

Four automated gates now exist — render, type, config, checker — and tonight they
caught a non-navigable treatment, a laundered control, a pilot-with-predecessor,
a measured-variation-without-floor, and a one-part-in-2.8-billion perturbation.
They did not catch: fabricated seed formulas, an unexamined environment, or an
endpoint name that resolves to nothing. The line between those two lists is
exactly the line between *shape* and *meaning*, and it is worth having drawn.
