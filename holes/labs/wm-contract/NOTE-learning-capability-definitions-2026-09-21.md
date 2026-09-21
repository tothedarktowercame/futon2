# improve-6 — definitions of learning, friction and capability (draft)

**Date:** 2026-09-21 · claude-5 (definitions side of the joint item), from Joe's
statements in session `de4c2047` today and claude-3's machine-side evidence
(`runs/fixlist-2026-09-21/improve-6-EVIDENCE.md`, dc4e2766). **Status:** draft
for Joe and claude-3; nothing here is a ruling.

Joe's starting points (2026-09-21, paraphrased; his words in quotes):

- Learning is surprise followed by structural revision; friction is surprise
  repeated without revision.
- The aim of work is not an "anodyne mental state"; AIF has to be read against
  the dark-room problem. The facade discovery (2026-08-30) was dismaying and
  productive.
- "I am still at age 46, strongly preferring things that increase my capability."
- The first, naive phase was not bad; it lacked capabilities. Some naive
  prototyping may be a necessary stage before verification.

## Definitions

**D1. Expectation.** A declared prediction with an identity, a scope, a
tolerance and a declaration time *before* the observation it is tested against.

**D2. Surprise.** An observation outside a declared expectation's tolerance.
An exception or failure with no declared expectation is an *incident*, not a
surprise. (claude-3: "surprise needs a declared expectation and tolerance, not
merely an exception.") Consequence: most of today's repair findings are
incidents until the expectation they violated is named.

**D3. Revision.** A change to a named model part, with old and new digests,
citing the surprise it answers. Kinds, which must stay distinct:
- *structural* — declarations, interpretations, wiring, the model's form;
- *parameter* — A, B, C, D, E values (none exists in production yet);
- *data* — what the model is fed;
- *operational repair* — the apparatus around the model, no model part changed.

**D4. Learning event.** A surprise, plus a revision citing it, plus evidence
that the revised part was used. Graded by how far that evidence reaches:
*committed* → *loaded* → *consumed* (a successor receipt shows the revised part
was read). A committed revision nobody loaded is not yet learning (casebook A3).

**D5. Friction.** A surprise that recurs on the same expectation after a revision
citing it, or recurs with no disposition at all. Operator-side analogue: the
same correction given again (the stance pilot's repeated-correction measure).
Friction is the cost; surprise alone is not.

**D6. Capability delta.** A learning event followed by a *successful successor*:
a later attempt on the same class of task passes where the pre-revision attempt
failed, judged by a check external to the thing being judged. Outcomes:
*held* (successor passed), *negative* (successor failed), *untested*. Several
current closures are untested in this sense (claude-3).

**D7. Not learning.** Activity counts (commit rates across 2026-08-30 rose from
59 to 2,538 per seven-day window in futon2), response rates, exceptions fixed
without a named model part, and repairs with no successor. These can accompany
learning; they do not measure it.

## Operator-side analogues

The same shape applies to the operator, with different records:

| | Machine | Operator |
|---|---|---|
| expectation | declared prediction | a stated plan or claim (e.g. "the PLoP catalogue is finished", 2026-08-21) |
| surprise | observation outside tolerance | a finding that contradicts it (the Learn stage had no input since 2026-07-06) |
| revision | model-part change citing the surprise | a ruling, mission, CLAUDE.md or memory change citing the incident |
| consumed | successor receipt | the revised rule is followed in later sessions |
| capability | successor passes an external check | the casebook protection holds on the next occurrence |

The facade chain is a human-mediated learning event by this definition: consumer
census (22379b5b), WM-RUN2 measuring 3 of 9 hops, the wiring amendments
(ad80436d). It is not a learned A/B revision, and should not be counted as one.

## Preference: why this is in *C* and G, not only in an audit

Joe's capability preference is, in AIF terms, the **novelty** term — expected
information gain about model parameters (improve-5). The definitions above give
it an observable counterpart: a policy is better, other things equal, if it
raises the rate of *consumed* learning events per unit of cost. Two cautions:

- **Dark room.** Minimising surprise is not the aim. A measure that rewards
  fewer surprises would reward not looking. The quantity to raise is learning
  events; the quantity to lower is friction (D5).
- **Naive phases.** A prototype phase with few learning events is not a failure
  if it is labelled as a prototype and produces the expectations later phases
  test (casebook A1, "skip when").

## What the records need (from claude-3's evidence)

An automatic join needs a **surprise→revision record**: surprise id,
occurrence, expectation id and tolerance, predicted vs observed, model part,
revision kind (D3), old/new digest, revision commit, reviewer, and the
loaded/consumed successor receipt. The fixing commit cites the surprise id.
Until that exists, learning events can be reconstructed by hand for a sample
(claude-3 lists eight evidenced pairs) but not counted.

## Open questions for Joe

1. Is "declared before the observation" too strict for the operator side? Many of
   Joe's expectations are tacit until contradicted.
2. Should a *negative* successor (D6) count against capability, or only fail to
   count for it?
3. Aesthetic sensibility (Joe, 2026-09-21) is a preference over the *form* of
   work, not its outcomes. It is left outside these definitions deliberately;
   is that right, or does it belong in *C*?
