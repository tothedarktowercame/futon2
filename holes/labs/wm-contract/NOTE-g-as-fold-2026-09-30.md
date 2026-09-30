# G as a fold over the cascade's shape; reverse morphogenesis (Joe, 2026-09-30)

Joe: "the concept of a catamorphism is equal to the Visitor pattern which is
the same as 'reduce'. This could bear on why the 'shape' of a cascade
matters. Not only what you do first, but how the cascade may have recursive
structure. Keeping in mind that a pattern cascade is somehow morphogenetic;
indeed, if we looked at existing *completed* missions and tasks we might be
able to do 'reverse morphogenesis' and figure out what cascade would have
given rise to their completion."

Recorded by claude-1 so it is not lost in chat. Not a build plan.

## What the code and the Lean do today (read 2026-09-30)

- A cascade in Lean (`Holes.lean:29-35`) has nodes, edges, an acyclicity
  proof and a precedence list.
- G is computed over `firing pi` (`CascadeEFE.lean:54-56`): the precedence
  list filtered by node membership. The edges are not read by the
  computation. So today G is already a fold, but over a LIST: only "what
  fires first" reaches it. Two cascades with the same firing order and
  different edges have the same G.
- That is the gap behind requirement Q10 (G distinguishes arrangements).

## The idea, stated for this machine

- If a cascade is given as a recursive structure (a pattern together with
  the sub-cascades it stands on, plus overlap), G can be defined by giving
  one rule per way of building a cascade (single pattern; one standing on
  others; two overlapping) and folding. Three consequences:
  1. G is total on cascades by construction, which is Joe's requirement on
     its domain (a structural recursion always returns a value).
  2. Shape enters necessarily: a different structure folds differently.
  3. Adjusting a cascade (adding, removing, re-nesting a pattern) changes G
     through the rule for that one place, which is what minimising G over
     adjustments needs (spec Stage 0).
- What is folded is probably not a bare number: each rule combines how its
  parts change the predicted state, and G is read off the result.
- Growth is the other direction: a cascade unfolds from a seed (a mission's
  HEAD reading). Growing and evaluating together is an unfold followed by a
  fold.
- Open point, not resolved here: a cascade is a semilattice of overlapping
  patterns, so sub-cascades are shared, not a tree. A fold over shared
  structure has to say how a shared part is counted once.

## Reverse morphogenesis

For a completed mission the record exists: the turns in which it was worked
(象 analyses with their pattern references and consecutive-turn order), the
commits, the boxes ticked. From those one can reconstruct the cascade that
was actually followed to completion. Uses:
- it is "completed work" entering the model as data: which cascades,
  of which shape, closed which kinds of task;
- it tests G without running anything: for a completed mission, does
  minimising G over the adjustments of its HEAD cascade come out near the
  cascade that in fact completed it?
A mission of this name already exists for papers
(`futon6/holes/missions/M-paper-reverse-morphogenesis.md`, 2026-04-15:
backward reconstruction of (problem, techniques, patterns, result) tuples as
a corpus for a forward solver). The same move applied to the stack's own
completed missions is what Joe describes. By today's scan roughly 196 of the
337 mission files in the canonical repositories are not open; that count
has not been checked against the closed/complete status rule.

## Interpretation is fit to the circumstance (Joe, 2026-09-30, later)

Joe: "a policy being a structurally distinct cascade makes sense, at least
on a preliminary basis. The 'interpretation' has to be about fitting that
material to the current circumstance. I reckon G would have to do with how
well that interpretation actually fits."

So: a cascade is general material (patterns in an arrangement). Interpreting
it is binding it to the case in hand: for each pattern, what in the current
state meets its IF and HOWEVER, what its THEN amounts to here, what its
BECAUSE would show. A cascade that binds well to the circumstance is a
better policy than one that does not, and selection has to see that.

What exists for measuring fit today:
- the 象 reading: for each pattern it accepts, the fragment of the
  circumstance it fits and a stated reason; for each it rejects, the reason;
  fragments for which no pattern was found (unexplained circumstance);
- the retriever's relevance score for each pattern against the text;
- for a pattern brought in by a graph retraction and not by the reading:
  no evidence yet that it fits this circumstance at all.

Where fit sits in the formalism already implemented: the selection law
recorded in every certificate is "sigma(log E − gamma·G)" with
`:omitted-terms [:F]`, `:reason :f-not-supplied`. F is the term for how
well a policy accounts for what has actually been observed: fit to the
present. G is the expectation over what acting would bring, and within it
ambiguity is higher when the account fits loosely. Every recorded run
omitted F. So the fit Joe describes has a place in the law, and that place
has been empty.

claude-1's first implementation choice for G over the HEAD-derived cascades
(operators read off the arrangement, success rate from the learning ledger)
did not look at fit at all. It is amended: each policy's fit to the
circumstance is computed from the evidence above and enters selection, and a
pattern with no fit evidence (a retraction's connector) is the thing that
is interpreted after selection, the interpretation being the fit.
