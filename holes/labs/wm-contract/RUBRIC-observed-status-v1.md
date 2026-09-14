# Observed WM status rubric v1

Status: author draft for independent review. This document defines evidence
criteria; it creates no annotation, observation, pair, count, probability mass,
or A-matrix cell. The future observer and this rubric's reviewer are distinct
seats.

## Authority and common law

The domain is exactly `#{:spawned :refined :strengthened :addressed
:falsified :foreclosed :reopened}` (`src/futon2/aif/belief.clj:37-42`, whole-file
SHA-256 `39c1dabaf7be878b638ee1f14f22e4d54f2b5b9ed1a2cc09bdd7b63d1b27a595`).
The semantic grounding is the retained event vocabulary
(`/home/joe/code/futon3/library/structure/interest-event-vocabulary.flexiarg:70-84`,
whole-file SHA-256
`b51296613be9c61bddf9b58d082eaaecbad91ef139373f73438762b2e0943405`).

Every qualifying observation uses the accepted annotation schema and an
ordered, nonempty evidence vector. Each evidence item retains a literal source
record as `:bytes`, its recomputed `:sha256`, `:observed-at`, a single
`:supports` value, and empty `:conflicts`. The evidence source must be about the
exact entity/run/cohort/attempt/checkpoint and must exist no later than the
immutable post-action, pre-disposition cutoff. A distinct authorized reviewer
accepts the complete subject after annotation creation. Retrospective
annotation may use only bytes frozen by that cutoff and must say it is
retrospective.

The following never qualifies, alone or wrapped in an operator-sounding field:
a chosen status keyword, interest `:event/type`, interest `:posterior-state`, a
reused interest event, renamed posterior argmax, model posterior, target close
disposition, mere first appearance, elapsed time, or absence of contrary
evidence. No zero cell is smoothed and no missing observation receives invented
mass.

Common refusals:

- zero rubric matches is `:evidence-insufficient`;
- more than one matching status is `:evidence-ambiguous`;
- any source or accepted annotator disagreement is `:evidence-conflicting`;
- source/subject/time/hash mismatch refuses before label acceptance;
- evidence about different lifecycle questions cannot be voted, averaged, or
  resolved by precedence.

The closest real retained source is the checkpoint map in
`/home/joe/code/futon7/holes/M-interim-director.md:259-315` (whole-file SHA-256
`92b2d5972ef184c860dd35cacf9a08b89f4f8e102d56a55b550b1fe30ba39bda`).
It contains actual event maps with `:event/id`, `:event/type`,
`:checkpoint/ref`, `:target/entity-id`, `:prior-state`, `:posterior-state`,
`:evidence/refs`, and `:operator/rationale`. The source itself warns at lines
257-258 that rationales paraphrase Joe and require adjustment before formal
close. It lacks WM run/cohort/attempt identity, immutable evidence cutoff,
literal referenced evidence bytes, independent observer authority, and exact
review acceptance. Its rows are therefore examples of retained *candidate
source shape*, never existing measured-A labels.

## Per-status criteria

### `:spawned` — `:wm/observed-status-spawned-v1`

Qualifying evidence must retain an entity-creation record with exact entity id,
creator/source identity, creation time after the action and before cutoff, plus
a pinned pre-action domain inventory proving that exact entity id was absent.
The creation and inventory records must share the declared entity namespace and
revision. Both are evidence payloads of the annotation, not an inferred
`state/spawned` keyword.

Refuse first-seen-only records, link creation, copied/renamed entities, an event
whose `:prior-state` already exists, or an inventory that is incomplete or from
another namespace/revision. Creation and refinement both matching is ambiguous;
an earlier occurrence conflicts. **Retained exemplar: typed absent.** No
retained record inspected carries the required creation plus complete prior
inventory at a WM close.

### `:refined` — `:wm/observed-status-refined-v1`

Qualifying evidence must retain byte-pinned before and after artifacts for the
same entity and an independent review record naming the narrowed, clarified, or
re-anchored dimensions. The after revision must follow the before revision,
both must precede cutoff, and the review must distinguish refinement from mere
editing, support gain, resolution, or contradiction.

Refuse a changed hash without a semantic before/after finding, formatting-only
changes, self-description as refined, or a remaining contradiction that makes
`:falsified` equally supported. The real `evt-close-2` at
`M-interim-director.md:278-286` has the vocabulary fields and an explanatory
rationale, but not literal before/after bytes or independent exact-subject
acceptance; it is a grounded candidate shape, not a qualifying observation.

### `:strengthened` — `:wm/observed-status-strengthened-v1`

Qualifying evidence must retain the prior standing evidence, a new independently
observable support artifact, and a review that identifies how the new artifact
increases support for the same entity without claiming resolution. All three
subjects and revisions must join and precede cutoff.

Refuse repeated old evidence, activity/volume alone, operator intent, a generic
passing test unrelated to the entity claim, posterior movement, or an already
resolved item. If evidence also resolves the criterion, `:strengthened` versus
`:addressed` is ambiguous unless the review explicitly decides the still-live
boundary. Real `evt-close-1` and `evt-close-3` at
`M-interim-director.md:266-276,288-301` retain new-support rationales and refs,
but the refs are not retained literal bytes and acceptance is not independent;
they are grounded candidate shapes only.

### `:addressed` — `:wm/observed-status-addressed-v1`

Qualifying evidence must retain a pre-cutoff statement of the exact finite
requirement/claim, evidence resolving every stated acceptance limb, and an
independent review recording no unresolved contradiction. Mechanical limbs use
raw execution receipts; semantic limbs retain the review's exact evidence.

Refuse a close disposition, completion/status flag, partial gate pass, work
performed without requirement closure, or silence about outstanding limbs. A
contradiction or missing limb conflicts; partial progress matching
`:strengthened` makes the label ambiguous. Real `evt-close-5` at
`M-interim-director.md:307-315` carries a claim-specific rationale and evidence
ref, but no complete requirement inventory, literal evidence bytes, or
independent acceptance; it is a grounded candidate shape only.

### `:falsified` — `:wm/observed-status-falsified-v1`

Qualifying evidence must retain the exact pre-existing falsifiable proposition,
the decisive test/observation protocol fixed before its result, raw result
bytes, and independent review explaining the contradiction. Proposition,
protocol, result, entity, and time must join exactly and precede cutoff.

Refuse ordinary implementation/build failure, unmet work, reviewer rejection,
absence of evidence, or a test designed after seeing the result. If the result
shows only that an implementation needs revision, `:refined` remains possible
and the label is ambiguous. **Retained exemplar: typed absent.** No inspected
retained record supplies the complete proposition/protocol/result/review chain
at a WM close.

### `:foreclosed` — `:wm/observed-status-foreclosed-v1`

Qualifying evidence must retain an authorized, deliberate no-go/closure decision
for the exact entity, its stated alternatives and rationale, effective time,
and evidence that the decision was in force at cutoff. Closure authority must
be independent of the annotation candidate.

Refuse inactivity, resource shortage alone, build/test failure, rejection
without durable closure, `:falsified`, or the target close disposition. A still
active alternative or later pre-cutoff reopening conflicts. **Retained
exemplar: typed absent.** No inspected record is an independently authorized
foreclosure joined to a WM close.

### `:reopened` — `:wm/observed-status-reopened-v1`

Qualifying evidence requires both (1) a prior independently accepted terminal
annotation for the same entity with status `:addressed`, `:falsified`, or
`:foreclosed`, and (2) later, pre-cutoff renewed-live evidence plus an explicit
authorized reopening decision. The terminal annotation, renewed evidence, and
decision must form one ordered revision chain.

Refuse `:state/reopened` alone, `:posterior-state :live`, strong current
standing without prior closure, prior closure without renewed evidence, or two
different entities/revision chains. The vocabulary explicitly says reopening
resets standing to `:live` (`interest-event-vocabulary.flexiarg:82-84`); neither
the event nor resulting standing is silently the WM `:reopened` observation.
**Retained exemplar: typed absent.** No inspected record has both independently
accepted prior terminal state and renewed-live evidence at the exact WM point.

## Grounding result and next boundary

All seven operational criteria are grounded in the retained semantic authority.
Three statuses (`:refined`, `:strengthened`, `:addressed`) have real candidate
record shapes; four (`:spawned`, `:falsified`, `:foreclosed`, `:reopened`) have
typed-absent exemplars. Zero of seven has a currently qualifying measured-A
annotation. The later a-labels packet must acquire observations under this
rubric with separately retained observer origin and exact-subject reviewer
acceptance; this document cannot occupy either seat.
