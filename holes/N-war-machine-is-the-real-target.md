# N-war-machine-is-the-real-target — why the vocabulary thread must not be dropped

**Provenance:** Joe, 2026-07-16: "I don't want to give up on the controlled
vocabulary idea, because the one place we use design pattern cascades pretty
regularly is in the War Machine. In principle we could apply propagators to
those. But I think we should try it with the tokamak for a bit first."

Written **before** the tokamak run reports, deliberately — so that whatever it
says cannot retro-fit the reasoning. If the tokamak fails spectacularly, this
note should still stand; if it succeeds, this note should still say the tokamak
was never the point.

## The tokamak is a testbed, not a target — and the difference is load-bearing

The tokamak has properties that make it a *good first try* and a *bad place to
stop*:

| | tokamak | War Machine |
|---|---|---|
| carrier | `g : BINS → ACTS`, both typed | flexiarg cascades, prose slots |
| N | 5 rule-count bins | mission/pattern structure |
| P | 5 propagators | ? — the open question |
| objective | **broken** (§4b: argmax on a band; undecidable) | grounded (the stack's own next move) |
| cost of being wrong | a CA field dies | a real decision gets made |
| runs in | seconds | the actual working day |

The tokamak types **because someone already did the vocabulary work** — `phi`
bins the field and `actions` names five sigmas. That is a controlled vocabulary;
it just happens to have been written as a Clojure map instead of a flexiarg. So
the tokamak does not show that we can skip the vocabulary. **It shows what having
one buys**, on a domain where it was cheap.

The War Machine is where cascades are actually used, and its vocabulary does not
exist. That is the whole problem and the tokamak does not touch it.

## Why the vocabulary thread is the one that generalises

Today's sequence, honestly:

1. **flexiarg slots** — refuted. A pun on the number 8; the index had no
   structure for an order to be *of* (`E-one-propagator-one-pattern.clj`).
2. **ant actions** — index genuinely semantic, but every prescription was
   CONDITIONAL, so the carrier collapsed to `ρ` (`ants-as-sexps.clj`).
3. **tokamak policy** — types cleanly, because `g : BINS → ACTS` absorbs the
   condition into its *domain*.

The lesson is not "climb until it types". It is **the policy reading**: a
conditional prescription is a function whose domain is the condition. That
reading is domain-independent, and it is the thing to carry to the War Machine —
not the tokamak's bins, which are about rule counts and mean nothing there.

So the question the War Machine inherits is not "what is its tokamak?" but:
**what is the WM's N, and what is its P?** Candidates, none checked:

- **N** = the WM's own regimes. Something already bins its state — the
  `*agents*` pane, the mission clock, the gate pipeline's G5→G0. If any of those
  is a `phi`, it is N and nobody has said so.
- **P** = the WM's arms. What does a WM cascade's THEN actually *select*? If the
  answer is "prose", there is no P and the ants' failure repeats exactly. If the
  answer is a finite set of moves (admit / defer / gate / escalate), that IS a P
  and it is already typed.

**WR-4 is the evidence that a P might exist.** Its `:gate` takes a pattern and
yields a pattern, and its NEXT-STEP is a two-valued sign
(`some-surface` / `none` → `admit` / `defer`). That is one arm of a WM action
vocabulary, already written, already binary. If there are four more, the WM has
its ACTS.

## The trap I would fall into, named in advance

The tokamak will produce numbers in a few minutes. The temptation will be to
read them as evidence about *the approach* rather than about *the tokamak*. They
are not. §4b already established that the tokamak's objective is broken, so:

- **If the cascade "wins"** — it won at a game whose scoring is known-invalid.
  That is not evidence the pattern-cascade-as-controller idea is right.
- **If the cascade "loses"** — it lost at the same invalid game. That is not
  evidence it is wrong either.

What the run CAN tell us is behavioural and narrow, and worth having:

1. **Do the two UNEVIDENCED boxes ever fire?** `tkc-dead` and `tkc-collapsing`
   are authored guesses, because §4b measured those bins "came back EMPTY". If
   they never fire, the cascade's self-report is confirmed by its first run and
   two of five patterns are decoration.
2. **Is tkc-mid's NEXT-STEP load-bearing?** The box claims "without it this box
   is Figure 8 and the field dies". `cascade` vs `cascade-no-NEXT` on identical
   seeds is a direct test of a pattern's own claim about itself. **This is the
   only preregistered falsifiable claim in the cascade**, and it is a claim a
   pattern makes about its own sign channel.
3. **Does the cascade reach `:rich`?** `:rich` is FREE under the shape map — the
   regime no rewrite can write. Whether the controller ever gets there is a fact
   about the controller.

None of those needs an objective. All three are the kind of thing the War Machine
would want to ask of its own cascades.

## What to carry across, and what to leave

**Carry:** the policy reading (domain = condition); the ν fixed-point test
(cheap, prunes before building); the discipline of marking `:UNEVIDENCED` boxes
so a plausible BECAUSE cannot hide an authored guess; TK-3 (same seeds).

**Leave:** the bins (rule counts mean nothing to the WM); the arms (sigmas are
MetaCA-specific); and above all the objective-shaped hole — the WM *has* a
grounded objective where the tokamak does not, and that asymmetry is the WM's
advantage, not a detail.

## Disposition

A **note**, written pre-result. The claim: the tokamak is a testbed for the
policy reading, and the vocabulary work is the thing that transfers. The next
question is not about the tokamak at all — it is *what are the War Machine's N
and P*, and WR-4's two-valued gate is the first evidence that a P exists.
