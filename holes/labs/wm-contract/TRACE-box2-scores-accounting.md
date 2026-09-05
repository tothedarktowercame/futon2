# TRACE: how "Free energies & scores" shows 0 holes over a greenfield

**Trigger (Joe, 2026-09-05, verbatim):** "a version of Box 2, that
actually shows, not bucketed, the real holes. And the fake accounting
whereby 'Free energies & scores' has zero holes must be traced...
that bucket is essentially greenfield when it comes to specification
compliance. Either that or the specification has been written down way
too vaguely."

## The 15 declarations the bucket counts (variable-situation-accounting.edn, :area scores)

| declaration | closure status |
|---|---|
| PredictiveOutcomeKernel | closed-by-record-with-witness |
| ParameterPriorKernel | closed-by-record-with-witness |
| ParameterPosteriorKernel | closed-by-record-with-witness |
| observationEntropy | closed-by-record-negative-space |
| G_eq_expectedFreeEnergy | closed-by-record-negative-space |
| ExpectedInformationGainValue | closed-by-record-negative-space |
| parameterInformationGain | closed-by-record-negative-space |
| modelUncertaintyBonus | closed-by-record-negative-space |
| expectedFreeEnergy | closed-by-record-with-witness |
| ambiguity | closed-by-record-with-witness |
| variationalFreeEnergy | closed-by-record-with-witness |
| softmax | closed-by-record-with-witness |
| predictiveOutcomeRisk | closed-by-record-with-witness |
| expectedInformationGain | closed-by-record-with-witness |
| modelUncertaintyAndEIG | proven-against-pinned-source |

## The verdict on the fork: BOTH, and precisely located

**The specification is written at the wrong quantifier.** Row one is
the exhibit: `PredictiveOutcomeKernel` — the TYPE of Q(o|pi) — is
closed-with-witness, while the F0 census finds Q(o|pi) uninhabited.
Both are true, because the declaration asserts "this type exists and
is faithfully transcribed against the glossary," never "a constructor
from the machine's model, belief, and policy inhabits it and is
consumed on the tick path." Same for ParameterPriorKernel /
ParameterPosteriorKernel (census: theta kernels uninhabited). The
defining equation `G_eq_expectedFreeEnergy` closes by NEGATIVE SPACE —
no runtime witness at all. Every scores declaration is a carrier or a
formula; not one is a constructor obligation. "There exists a type Q"
was declared; "there exists a map into Q, and R4 consumes it" — the
commissioned alignment clause — was never declared, so it could never
show as a hole.

**The accounting then counts the weak declarations faithfully.** The
bucket's 0 is arithmetic over what the transcription era chose to
declare (drift link D5d-era; see DRIFT-2026-09-05-account.md §3-4).
Not fake numbers — a fake DENOMINATOR.

## What the unbucketed Box 2 must show (binds F4)

One row per REAL hole, no aggregation: the 10 declared contract holes
(each with its falsifier) PLUS the 7 census fundamentals (each with
its uninhabited signature, both sides) — 17 rows; and each closed
scores declaration relabelled by what its closure actually asserts
(type-transcribed / formula-transcribed / runtime-witnessed), so a
closed type row can never again be read as an inhabited constructor.
Negative control: un-declaring one fundamental must drop exactly its
row.
