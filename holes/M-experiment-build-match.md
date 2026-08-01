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
