# Investigate: are cascade overlap modes precision on shared factors?

**Status:** OPEN — INVESTIGATION (discovery only; no code change until the
findings are reviewed with Joe)
Parent: M-aif-a-matrix-faithfulness; excursion of futon3c M-repl-circus (Joe, 2026-11-11)

## The question

While annotating the conference entry "The Semilattice-Rollout Execution
Witness" for the plop2026 docbook (`p4ng/plop-2026-code-annotations.json`,
follow-up of 2026-10-19), claude-4 proposed an active-inference reading of
how pattern occurrences combine in a cascade. Joe (2026-10-19) likes the
three-part reading but doubts the code matches it; this ticket is to find out.

The proposed reading treats each overlap between two occurrences as a claim
about the factorisation of the generative model:

1. **Full (recombinant) blend** (a houseboat): the two occurrences share one
   hidden-state factor, mapped with high precision into all outcome
   modalities of both.
2. **Selective blend** (a floating chicken coop): they share the factor, with
   precision on only some modalities; selective projection is
   modality-specific likelihood precision.
3. **Containment, no blend** (an alcove in a living room): no shared factor;
   a hierarchical relation in which the outer occurrence's state supplies an
   empirical prior for the inner one.
4. A **shared-name candidate** is a low-precision hypothesis that a factor is
   shared, for structure learning (Bayesian model reduction, R17) to confirm
   or reject.

## What the code has (at `759c75dd5ce0`)

`src/futon2/aif/wm/executable_cascade.clj` `classify-edges`: an `:overlap`
edge is a `:gluing` only with a witness of kind `:isomorphism` or
`:consistency` (`witness-conforming?`), else a `:shared-name-candidate`;
reading-order relations are descriptive; `cascade_enactment_compiler.clj`
lists `:scope-nesting` and `:gluing` among descriptive evidence kinds.
The occurrence squares are in `three_halves_square.clj`.

## To find out

1. What `:isomorphism` and `:consistency` witnesses actually assert, who
   produces them, and how often each occurs in recorded cascades. Do they
   correspond to full and selective blends, or to something else?
2. Whether any recorded overlap is containment, and how it is represented
   (`:scope-nesting`? reading order?).
3. Whether the squares (`three_halves_square.clj`) carry anything that
   could serve as a per-modality precision, or as the shared-factor identity.
4. Whether the 3/2-category treatment (joint coverage, gluing, coherence;
   see the M-象-2000 checkpoint of 2026-10-11) states the interchange law,
   and whether its failure separates containment from blending.
5. Which parts of the reading are contradicted by the code, and which are
   simply absent.

## Deliverable

A short note in `holes/labs/` answering 1-5 with file:line evidence and
counts from recorded cascades, ending with: matches / partly matches / does
not match, and what a faithful representation would need. No code change.

## After the findings (Joe, 2026-11-11)

"it could be a good addition to the zone.hyperreal.enterprises Caddy-served
blog once we have the analysis back (particularly if we agree it is working
well, but a negative finding could also be interesting)". Joe also links it
to "the GCC for patterns" he is discussing with claude-cs: "the example of a
"floating chicken coop" is a good way to understand how patterns compose,
even though there is no such "floating chicken coop" in our pattern library".

Joe (2026-11-11, a further example): "imagine someone in a submarine saying
"I really feel cooped up in here". This is an interesting example b/c we
don't need to go via "floating prison" or "floating chicken coop", even
though we *might* want to go via "houseboat"." And on order of work: "the
best way to make sense of #7 would be via some live examples whose
representational quality we trust, not via historical examples" — so #7
waits on the Circus invariants (M-repl-circus, "Two invariants").

## Provenance

claude-4, 2026-10-19, at Joe's request ("investigate further via a ticket").

## Completion criteria

- The investigation note exists — check: a note under futon2 holes/labs/ (ticket's stated deliverable) answering questions 1-5 (what :isomorphism/:consistency witnesses assert and their counts; whether containment is represented; whether the squares carry per-modality precision; the interchange law; contradicted vs absent). **Not started.**
- The note ends with the verdict and the faithful-representation sketch — check: the note's closing lines give matches / partly matches / does not match and what a faithful representation would need. **Not started.**
- Findings reviewed with Joe before any code change — check: a review note or Joe ruling recorded against the note (the ticket's own constraint: no code change until findings are reviewed). **Not started.**
