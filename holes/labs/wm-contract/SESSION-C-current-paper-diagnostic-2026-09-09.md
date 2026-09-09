# Current-paper preference diagnostic — 2026-09-09

## Joe's direction, verbatim

> Okay, well for a pilot I think this is excellent because it does show that we're able to recover clearly identified preferences by looking at the historical material. And the question then becomes just how do we operationalize that into a computation? And maybe to make this a little bit more complete and... Something that we can define recursively, We should look at the preferences that are expressed in the PLOP 2026 paper and the Futon 2026 paper. Because, then we can see whether any of the historical preferences that we've described actually help. Define work that we could do. On those papers or on those trajectories that are outlined in those papers. Because that's the current work we're concerned with. So, basically, I'd like to construct a... Family of preference distributions which allow us to... Use C in a diagnostic way that's helpful. With the current workflow. So that we can rather quickly demonstrate that we can take these qualitative analyses and turn them into the kind of computational analysis that we need in order to Run the system effectively. And if we can do that initially by getting it to build the system effectively, then we can include mining preferences as part of the run. And get going rather quickly, rather than needing to do a complete survey of all historical and all future possible preferences. We just need to make a registry of them. That we can use to move forward. So I would say we're quite close now towards getting... A minimal working version. That we can demonstrate is effective. In the specific sphere of activity that we're concerned with now. But in order to do that, we need to make clear in the way we just did with those pilots. What our current preferences look like. Through those artifacts that I mentioned.

## Bounded implementation

The local registry and executable diagnostic are in
`runs/C-current-paper-diagnostic/`. This is a session registry, not an edit to
WM's equations, choices, decisions, or worklist. Entries are source-grounded
candidates for Joe's review. No preference mass or production setting is ruled
by this artifact.

Read the paper drivers and their relevant included sections. The registry pins
verbatim passages from PLoP's `sec-what-this-is.tex` and `sec-conclusion.tex`,
and Futon's `sec-transfer.tex` and `sec-evaluation-outline.tex`. Each entry
names its criterion, historical connection, measurement obligation, lifecycle
phase, and proposed current work. The five entries are:

| Preference | Historical connection | Current diagnostic / work |
|---|---|---|
| Claims supported at the scope asserted | A: flight admissibility with identical numerical error | Reconcile the recap's measurement claim with Part III's outline status. |
| Reasons a person can inspect and dispute | B: persistence prerequisite for self-account | Have an independent reader reconstruct a diagnostic decision from its record. |
| Witnesses distinguish satisfaction from denial | C: feedback substring probe | Design the missing semantic carrier against the existing negative example. |
| Instrument helps solve a real problem | D: informative prediction arithmetic alone does not show useful action | Design a bounded current-work trial with an independent outcome review. Payment remains a separate coordinate. |
| Patterns recognisable beyond one implementation | Historical missions supply enactments | Examine a particular independent enactment; do not infer a promotion census. |

These connections are analyst interpretations, not newly discovered causal
proofs. In particular, the external-usefulness entry is not reduced to a count
of tests or internally conforming artifacts. The registry's binary satisfaction
schema is a candidate local representation, not a claim that all preferences
are binary. Its broader entries still need measurement design; the current
executable binary comparison uses only the narrow claim-scope condition and
prior feedback probe.

## The distribution calculation

For each narrow binary criterion, propose the family

    C_i(satisfied) = p_i
    C_i(unsatisfied) = 1 - p_i
    1/2 < p_i < 1

The interval encodes only the proposed qualitative ordering. It supplies no
selected mass, no strength of preference, no weight between criteria and no
joint distribution. It is a candidate design for Joe, not a derived numerical
estimate. Existing explicit zeros remain untouched. The endpoint p=1 would
mean a hard prohibition with infinite KL for failure; it is outside this
pilot's soft family, not smoothed or implicitly chosen.

For a *witnessed or stipulated* binary state, Q is a point mass. Then

    KL(delta_unsatisfied || C_i) - KL(delta_satisfied || C_i)
      = log(p_i / (1 - p_i)) > 0.

Thus a comparison can be decisive without selecting p_i. The program compares
known coordinates, reports opposing changes as an unranked trade-off, and
blocks comparisons when an unknown coordinate changes. Unchanged unknowns are
unscored; no claim about their future constancy follows. This is coordinatewise
comparison, not a calculated joint or weighted G. The formula is algebraic;
the program executes the corresponding Boolean partial-order comparison.

## Actual current-work pilot

The generated Futon recap says both "Part III states the test rather than
passing it" and "inside the boundary Part III measured". The latter claim's
producer is `p4ng/empirics-futon/argument_outline.py`; the generated file warns
against editing it by hand. The interpretation of that claim as
`measurement-completed`, against `outline-only` support, is an explicit analyst
annotation awaiting independent review. A string check merely binds that
annotation to the pinned source; it does not discover semantics.

The current diagnostic records claim-warrant=false under that annotation. The
counterfactual changes only the annotated claim scope to outline-only and
returns claim-warrant=true. This improves the known coordinate throughout the
family above. It is a proposed producer correction, not an executed paper edit,
not an empirically predicted successful action, and not demonstrated reader or
commercial benefit.

The pinned previous C probe contributes the second false coordinate: delivered
and explicitly denied feedback both had divergence 0. The Python runner reads
the prior result, not a fresh execution of the underlying Clojure function.
The other three preference readings remain unknown. The separate acceptance
grid is parsed as EDN: 160 unique case/phase cells, all 160 solved readings and
all 160 paid readings unknown. That establishes the recorded grid's state;
it does not establish that no evidence exists anywhere else.

## Process and recursion

The task proposals attach to the previously pinned mission-lifecycle exits:
ARGUE for intelligible reasons, VERIFY for semantic discrimination,
INSTANTIATE for a reproducible usefulness trial, DOCUMENT for accurate and
findable reports. These phase labels locate proposed obligations; they do not
certify that any lifecycle exit has been met or define a time-indexed C_tau.

Each proposed task can be expanded using the same record schema: source,
criterion, observation, review, and next task. For example, the recap correction
can acquire a producer change, regenerated views, and an independent review of
scope consistency. That is the bounded recursive construction proposed here.
The registry does not automatically infer parent satisfaction from child
completion. How/why contributions are not yet sufficient production rules or
probabilities of execution.

Mining during a run can append candidate records with provenance to this local
schema and then obtain review. Automatic preference adoption, lifecycle/time
mapping, numerical mass selection, observation-model marginalization, and live
policy integration still require design. This pilot establishes a working
retrospective diagnostic, not an enabled production preference model.

## Reproduction and validation

From any directory:

    python3 /home/joe/code/futon2/holes/labs/wm-contract/runs/C-current-paper-diagnostic/diagnostic.py

Requires Python 3 and `bb`; the latter parses the existing EDN in a standalone
process. No live endpoints, source loading into shared JVMs, or writes occur.
Paper sources are read with `git show` from the exact revision in the registry.
Concurrent working-tree changes to the transfer and evaluation sections were
detected during validation; they are not incorporated into this snapshot.
The runner reports working-tree differences for quoted sources on stderr.
Source SHA and exact quote checks reject changes to the pinned inputs. Compare stdout with the saved
`result.json`. Controls check unknown evidence, unresolved comparisons,
opposing improvements, reversal, and unchanged states. A changed source pin
must fail rather than silently adopt changed evidence.

No paper, production scorer, global registry, worklist, or ruling is changed.
