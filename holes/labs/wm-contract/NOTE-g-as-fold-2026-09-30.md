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
