# N-tokamak-patterns — deriving a pattern class from a parked experiment

**Provenance:** Joe, 2026-07-16: "I'm wondering if we could make our lives a bit
easier by looking for a new class 'tokamak design patterns' and then applying
propagators *to those*." Derived, not grepped — no such class exists.

**Verdict up front: the first half is right and better than you argued for; the
second half is the same wall one level up, and I think it's the third time today
we've climbed it.**

## Where they come from

`futon5:M-propagators` §4b parked the tokamak ("the Tokamak has done enough")
and, in parking it, wrote down what it had learned about *itself*:

> **Methodological findings worth more than the numbers** (all self-inflicted,
> all logged so they are not repeated)

That heading is a pattern class that hasn't been lifted. Three findings there,
plus two in the parking rationale. Below, derived in the argument core.

## The derived patterns

### TK-1 · Return contaminates credit

```
IF        you score a window's action by the return realised to end-of-run
HOWEVER   later windows run DIFFERENT actions, so the return is not this
          action's consequence
THEN      score the ACTION, not the situation it left behind
BECAUSE   G_w estimates "how good was this SITUATION", never "how good was this
          ACTION" — rotate+1 scored .226 rather than ~0 because rotate+2 carried
          the rest of the run. The trap was BAILED OUT by its successors, and at
          n=1 the estimate never corrected.
```

### TK-2 · Random exploration is unbiased over actions, not over states

```
IF        you explore by choosing actions at random to get an unbiased estimate
HOWEVER   the states that reveal the phenomenon are reachable only by a policy
          that COMMITS, and a random policy never commits
THEN      seed exploration with hold-k trajectories, not with uniform choice
BECAUSE   the :dead and :collapsing bins came back EMPTY. Collapse is only
          reachable by HOLDING a propagator. So the very states that expose the
          trap were off-distribution for the policy meant to discover them —
          V's spread collapsed to .017 against r's .13–.26, the advantage term
          went inert, A ≈ r, and the arm degenerated into fixed rotate+2,
          pixel-identical to it.
```

### TK-3 · Score all arms on the same seeds

```
IF        you compare two arms by their mean scores
HOWEVER   the arms ran on different seeds
THEN      score every arm on the SAME seeds before comparing
BECAUSE   run 2 reported memory beating greedy; it was memory's LATE seeds
          against greedy's EARLY ones, and the real result was the opposite.
```

### TK-4 · argmax is the wrong shape for a band

```
IF        the property you want lies BETWEEN two extremes
HOWEVER   your controller maximises a proxy for it
THEN      do not use argmax; a band is not a maximum
BECAUSE   EoC is a band between order and chaos. Even a VALID proxy is Goodhart
          bait under argmax — the optimiser walks to an edge the band excludes.
```

### TK-5 · The mission's own bank, ignored by its owner

```
IF        a mission has already BANKED that an instrument cannot answer a
          question
HOWEVER   the instrument is the only one you have
THEN      stop optimising it; the bank is a result, not an obstacle
BECAUSE   §2 banked genotype transport as "registered-null separation, not an
          EoC instrument ... can reject impostors but cannot certify
          edge-of-chaos". FOUR RUNS optimised it anyway. That is the mission's
          own bank being ignored by its own owner.
```

## The half that is RIGHT, and it is the half that matters

**Their BECAUSE register is well-formed, and that is exactly what the ants get
wrong.** claude-2 found — and I confirmed 5/5 from source — that every ant
pattern's BECAUSE points at Futon5 or at graph walkers, never at the ant. Their
ego is the exporter's, so the patterns have no address in the ant's world.

Read TK-1..TK-5's BECAUSE slots: *"G_w estimates how good was this situation"*,
*"the :dead bins came back empty"*, *"it was memory's late seeds against
greedy's early ones"*, *"the mission's own bank ignored by its own owner"*. Every
one is about **the experimenter's own machinery**. The ego IS the experimenter.

If BECAUSE is π_ego — the counit, the self-state — then these patterns have a
self where the ants have a customer. That is not a small difference: it is the
difference between a pattern that can be evaluated in its own world and one that
cannot. So as a *class*, Joe's instinct is right, and right for a sharper reason
than "we know this domain well".

**And there is a genuine find in here that nobody logged.** §4b's third
established point reads:

> **The trap is only a trap if held.** rotate+1 (Figure 8) has the *highest*
> single-window transport in the set (.319/.273) and decays to exactly 0 when
> held. ... Its "myopia" is adaptive: re-probing every window means it never
> holds anything, so it surfs the transient and leaves.

**That is `settles_if_nu_has_fixed_point`, discovered empirically, months before
the Lean.** Holding → decays to 0 → settles → death. Not holding → survives.
oxf-claude-2's theorem says a carrier with a fixed point settles and settling is
death; the tokamak *measured* that and wrote it down as a controller tip. The
theory and the experiment found the same thing from opposite ends and nobody put
them in the same room. That alone justifies lifting the class.

## The half that is WRONG, and I think it is a pattern in us

"Applying propagators to those" needs a typed `P` and a fixed-point-free
`ν : P → P`. Look at what TK-1..TK-5 actually prescribe: *"score the action, not
the situation"*, *"seed with hold-k"*, *"use the same seeds"*, *"don't argmax"*.
These are **methodological constraints**. There is no finite index set they range
over and no value set for ν to move. It is `ν`-undefined-on-English again —
verbatim the wall the ant prose hit, one storey up.

**And that is the third time today.** Slots → actions → propagators. Each climb
looked like it found a typed carrier and each one turned out to be `ρ`:

- the 8 flexiarg slots: refuted as a pun on the number 8 (the index had no
  structure for an order to be *of*);
- the 4 ant actions: index genuinely semantic, but every prescription turned out
  CONDITIONAL, so `(condition → (action → dir))` = ρ;
- now the tokamak: better ego, but the prescriptions are prose again.

Going up a level is *always available* and always feels like progress, because
the new level's objects look typed until you read their verbs. The tower is
infinite. If the rule is "when the carrier resists, climb", we will climb
forever and always be one level from a result. **I would rather name this as our
failure mode than take the fourth step.**

There is a steelman I want to state fairly, because it is not nothing: the
tokamak's *actions* ARE propagators — it chooses among them — so `P` = the arm
set is finite and typed, and a propagator acting on a tokamak policy would be the
theory applied to its own controller. That is real. But it is a claim about the
tokamak's **policy**, not about TK-1..TK-5, which are patterns about how to
*build* such a policy. The class Joe is deriving and the carrier that would host a
propagator are two different objects, and merging them is the move that would let
us believe we had one when we had the other.

## What I would do instead

**Lift the class for the ego, not for the propagator.** TK-1..TK-5 are worth
having as flexiargs because they are the corpus's only patterns whose BECAUSE is
self-directed — they are the control group for claude-2's finding. If the ants'
patterns have no address because their ego is the exporter's, then a class that
demonstrably HAS an address is what makes that claim measurable rather than
rhetorical. That is a real use and it needs no propagator.

**Do not apply propagators to them yet.** The blocking question is unchanged and
is one level down: the ants need `:drop` — a vocabulary that can express an
action the four verbs cannot — and no amount of climbing supplies it.

## Honest note

TK-1..TK-5 are derived from §4b's prose by me, today, and are not attested. §4b
also says the tokamak was "parked deliberately, not abandoned — with the reason
stated so it is not re-opened by accident". Lifting its lessons is harvesting,
which §4b invites ("worth more than the numbers"). Running propagators on them
would be re-opening, which it does not.
