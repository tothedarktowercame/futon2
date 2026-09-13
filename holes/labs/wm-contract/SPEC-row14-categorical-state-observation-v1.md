# Row 14 categorical-state observation v1

Status: accepted acquisition method refined into an implementation candidate;
not a kernel, close attachment, count, observation of physical ground truth, or
admission.

The conditioning state is the selected entity's adjudicated WM lifecycle status
at `:post-action-pre-disposition-at-close`. Evidence freezes after the action
completes and strictly before the target disposition is adjudicated. Annotation
and review may occur later; retrospective use must be declared in
`:limitations` and may use only sources whose own observation timestamps are at
or before the immutable cutoff. The target disposition, model posterior,
posterior argmax, interest transition category, and interest projected standing
are forbidden label evidence.

The rubric is `:wm/categorical-state-rubric-v1`:

| Status | Required evidence assertion |
| --- | --- |
| `:spawned` | entity newly created |
| `:refined` | framing sharpened or re-anchored |
| `:strengthened` | support gained from evidence |
| `:addressed` | resolved by evidence, not contradicted |
| `:falsified` | contradicted by evidence |
| `:foreclosed` | deliberately closed off |
| `:reopened` | prior terminal standing and an explicit reopening |

Zero matches is insufficient; multiple matches is ambiguous; disagreement
between independently accepted labels at one exact point is conflict. All three
refuse and yield no pair. `state/reopened` is a transition event and its
post-event interest standing is `:live`; neither is silently a WM `:reopened`
label.

Authority is external. The candidate contains opaque observer/review references.
A fixed resolver supplies an authorized observer origin and a pinned review
record. The review names a distinct reviewer and binds the canonical subject
(schema, observation id, entity, full temporal/run identity, state, method,
rubric assertions, evidence pointers, and limitations) both structurally and by
SHA-256. Candidate-owned acceptance is rejected. Every evidence and review
source is strict UTF-8, exactly one EDN form, hashed and parsed from the same
bytes, then checked against mutation during validation.

This rubric operationalizes the existing lifecycle descriptions in
`belief.clj:37-42` and
`futon3/library/structure/interest-event-vocabulary.flexiarg:70-136`; it does
not claim that an annotation reveals a hidden state without error. A future
measurement record must retain rubric, method, cohort, retrospective flag,
missingness and selection limits.
