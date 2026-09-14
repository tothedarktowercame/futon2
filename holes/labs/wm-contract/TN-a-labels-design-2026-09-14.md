# a-labels acquisition design — 2026-09-14

Status: **design discovery only**.  This note creates no annotation, authority
binding, observation, label, pair, count, or A-matrix content.  It reads the
accepted rubric and validator plus the accepted close-retention carrier at
futon2 tree `3a6f4f61111cb66adf830a6ddae9c0ddccff366d`.

## Answer in brief

A retrospective annotation is a new independently authored judgment over an
immutable, pre-cutoff evidence bundle.  It does not alter the close's typed
absent state port.  Of the three statuses with grounded candidate shapes,
`:addressed` is the nearest feasible first annotation on a future successful
repair close, but even it needs a pre-cutoff finite-requirement inventory,
literal raw evidence, and an exact-subject independent review artifact.  A
generic grounded-change close is insufficient.  `:refined` needs pinned
before/after entity revisions plus a semantic review; `:strengthened` needs
prior standing plus genuinely new support and a review that expressly leaves
the claim unresolved.  Current sibling records contain pieces of these
arguments, but `:admitted-evidence` is empty and none supplies a complete
rubric judgment.

## 1. Retrospective route on a new close

### Common acquisition sequence

For a future close which follows a successful selection, the runner mints one
action/transition occurrence after discrimination and before construction
(`full_loop_runner.clj:3340-3349`).  The cohort writer supplies the one
`:recorded-at` used as both `:closed-at` and `:evidence-cutoff`, and replaces
`:retention-inputs` with the validated block before writing the close
(`full_loop_cohort.clj:318-355`).  The accepted carrier requires exact action,
run/cohort/attempt identities, distinct action and transition ids, action time,
typed state/model ports, and ordered unique evidence ids
(`SPEC-close-retention-v1.md:6-40`; implementation
`close_retention.clj:50-90,142-178`).

After that close, a retrospective observer may inspect only literal records
whose bytes were frozen no later than the retained cutoff.  The observer emits
one `:wm/measured-a-annotation-v1` with `:provenance :mode :retrospective`, the
same frozen cutoff, and an explicit reason.  Each evidence item embeds literal
bytes and their recomputed SHA-256.  A distinct reviewer then accepts the
validator's fixed-order `acceptance-subject`; annotation creation and review
may be later than the cutoff (`measured_a_annotation.clj:57-78,123-167,
196-220`).  The original close remains unchanged.

The examples below are hypothetical future records shaped like the actual r8
sequence: `001-time-step`, `002-selection`, `003-construction`,
`004-dispatch`, `005-build`, `006-adjudication`, `007-closed`, plus the trace,
grounding witness, and morning-brief item.  The r8 key inventory confirms that
selection retains ranked candidates/reasons; construction retains fold,
mission and correspondence; build retains artifacts/commits/validation; and
adjudication retains before/after/witness/build-match/dial.  That is machinery
shape, not a claim that r8 itself qualifies.

### `:refined` — conditionally feasible, missing semantic before/after proof

Rubric criterion `:wm/observed-status-refined-v1` requires byte-pinned before
and after artifacts for the same entity, ordered revisions, and an independent
review which names the narrowed, clarified or re-anchored dimensions and
distinguishes refinement from editing, support gain, resolution and
contradiction (`RUBRIC-observed-status-v1.md:74-87`).

A future construction record could pin the exact mission/requirement before
the work.  Its build record could name the authored commit and files, and the
adjudication/grounding witness could retain before/after substrate objects and
the independent review job.  A trace can pin the selected occurrence and
state visible to the scorer; the morning brief can describe the delivered
change.  These records are useful only if all their literal bytes are admitted
at close.

They still fall short today: a commit id plus file list is not literal before
and after entity bytes; the grounding witness's generic `before`/`after` and
`dial-moved?` do not identify narrowed dimensions; a normal implementation
review does not necessarily distinguish refinement from the other six rubric
statuses.  The additional pre-cutoff artifact is an exact entity revision
pair plus an exact-subject semantic review recording those dimensions and
ruling out contradiction/resolution.  Without it the retrospective result is
`:evidence-insufficient`, not `:refined`.

### `:strengthened` — conditionally feasible, missing prior-standing chain

Rubric criterion `:wm/observed-status-strengthened-v1` requires prior standing
evidence, a new independently observable support artifact, and a review
explaining how it increases support for the same unresolved entity; every
subject and revision must join before cutoff (`RUBRIC-observed-status-v1.md:
89-103`).

The construction/selection siblings can identify the live target and claimed
obligation.  Raw build receipts or a produced artifact can be the new support,
and adjudication can point to an independent review.  The trace can corroborate
the occurrence; the morning brief is a delivery projection, not itself prior
standing.

They fall short unless a separately pinned prior-standing record predating the
action and a review explicitly saying “support increased but the criterion is
still live” are frozen before cutoff.  Activity, test volume, posterior
movement, and generic passing tests are forbidden by the rubric.  A grounded
change whose witness says the repair is resolved tends toward `:addressed`;
without an express still-live decision it makes strengthened/addressed
ambiguous and must refuse.

### `:addressed` — feasible for the right close, but not from disposition

Rubric criterion `:wm/observed-status-addressed-v1` requires a pre-cutoff
statement of the exact finite requirement, evidence resolving **every** limb,
and independent review recording no unresolved contradiction; mechanical
limbs require raw receipts and semantic limbs the review's exact evidence
(`RUBRIC-observed-status-v1.md:105-118`).

This is the best candidate for annotation #1.  On a future bounded repair, the
selection/construction records can pin the exact obligation and finite
acceptance limbs; dispatch pins the commissioned worker/job; build plus the
referenced artifact can retain code and raw execution receipts; adjudication
and its grounding witness can retain the independent review and exact
before/after resolution; the trace can bind the selected occurrence; the
morning brief can corroborate delivery.  All must exist before the cohort
writer's cutoff and be admitted by exact record id/hash.

Current machinery still lacks an explicit complete-limb inventory joined to
raw evidence and an exact rubric review stating no unresolved contradiction.
`:outcome :grounded-change`, `:grounded? true`, `:resolved? true`, a completion
flag, or a morning-brief achievement cannot replace those requirements.  Thus
the route is feasible on a specially prepared new close, not automatically on
every successful close and not retrospectively on r8 as retained.

## 2. What could be admitted at close

The runner currently supplies `:admitted-evidence []` and typed absent state
and model ports (`full_loop_runner.clj:3029-3049`).  The carrier requires ids
to be ordered, unique strings, and requires an observed state's evidence id to
occur in that vector (`close_retention.clj:162-171`).  A later packet must
decide the admission policy; this note enumerates candidates without choosing
them:

1. the `001-time-step` event id: opportunity, execution authority and code
   state;
2. the `002-selection` event id: exact selected target/action, ranked field,
   selection reasons, belief/trace reference;
3. the minted `:transition/id` and `:action/id`, with the complete occurrence
   bytes (these are conditioning identity, and may also index evidence);
4. the `003-construction` event id: finite mission/obligation, fold output,
   wiring/correspondence and any explicit acceptance-limb inventory;
5. the `004-dispatch` event id and immutable dispatch/commission receipt id;
6. the `005-build` event id plus each immutable authored artifact and raw gate
   receipt id/hash;
7. the `006-adjudication` event id, independent reviewer job/receipt id, and
   grounding-witness id;
8. the redirected/full trace record id and hash for this exact occurrence,
   when successfully written before cutoff;
9. an independently produced categorical-observation id, when it exists
   before close and is eligible for the close's observed state port;
10. the delivery-QA record and morning-brief item, but only if their immutable
    bytes exist before cutoff (the close itself cannot admit evidence created
    afterward);
11. exact before/after entity revision artifacts, prior-standing records, or
    complete-requirement records needed by the chosen rubric criterion.

An “id” here must resolve to one strict single-form/literal-byte record and a
digest.  Merely inserting a string into `:admitted-evidence` proves neither
existence nor pre-cutoff immutability.  The implementation packet therefore
needs an independently configured resolver/manifest, source hashes, and an
admission instant for each candidate.  The close writer should accept the
already resolved ordered ids; it must not search mutable stores while deciding
the label.

This serves two different cases.  For a contemporaneous close-time state, the
independent observation must already exist, have `observed-at < cutoff`, have
`action-at <= state-at <= closed-at`, and its evidence id must be admitted
(`SPEC-close-retention-v1.md:35-40`).  For a retrospective annotation, the
close's state remains absent, but the admitted manifest proves which source
bytes were frozen by the cutoff.

## 3. Seats and authority artifacts

The current draft deliberately leaves all authority values unbound
(`rubric-authority-draft.edn:16-44`).  The validator keeps candidate and
configured authority separate, requires observer/reviewer identities to be
distinct, recomputes origin and authorization byte hashes, and requires an
accepted artifact whose parsed bytes equal the acceptance record without its
artifact wrapper (`measured_a_annotation.clj:170-220`).

The minimal binding is:

1. **Observer-origin record, before observation.**  An authorized campaign
   owner issues a versioned record naming one observer seat, its origin and
   scope, the rubric id/hash, allowed close/source roots, and validity window.
   Literal bytes and SHA-256 populate `authority.observer.origin`.  The seat is
   neither rubric author codex-23 nor campaign reviewer claude-15.  The record
   must not be authored by the observer itself.
2. **Reviewer-authorization record, before acceptance.**  A separate versioned
   record names claude-15 (or another explicitly authorized reviewer), the
   rubric/validator versions, exact production scope and validity window.
   Literal bytes and SHA-256 populate `authority.reviewer.authorization`.
   It grants review authority, not permission to author the label.
3. **Frozen evidence manifest, at close.**  The runner/cohort boundary records
   the selected evidence ids, exact source paths/roots and SHA-256s no later
   than cutoff.  This is analogous to a machinery execution cohort pin: the
   configured authority is external to the candidate and the execution later
   echoes rather than invents it.  The R10 commission precedent similarly
   binds an independently written commission's literal subject/digest before
   dispatch; `futon3c/wm/r10_commission.clj` is SHA-256
   `9a4f1cb30f26996c51af996be905a6ccfd6eec618666b36b07e8ebd9bd336275`,
   and its contract is
   `futon3c/holes/labs/wm-contract/SPEC-r10-click-commission-v1.md`, SHA-256
   `0a89e1869d570cba8f5ba81abc9be61ced2a1a2c7ea577fd79fae0fd0612dc06`.
4. **Annotation, after close for the retrospective route.**  The authorized
   observer receives a criterion-specific evidence view, emits exactly one
   annotation, embeds the frozen literal bytes and hashes, declares
   retrospective provenance, and signs/names its source record.  It cannot
   see or use later evidence.
5. **Exact-subject acceptance, after annotation.**  The reviewer validates
   evidence and conflicts, then writes an acceptance whose `:subject` is
   exactly `measured-a-annotation/acceptance-subject`, whose
   `:subject-sha256` hashes its `pr-str`, and whose artifact literal bytes parse
   to the acceptance core.  That supplies `authority.acceptance`; validation
   is then run with the configured authority, not fields copied from the
   candidate.

No seat is bound by this note.  Joe/operator input is needed once to authorize
the observer origin and reviewer authorization if no already adopted campaign
charter explicitly grants those two roles.  Joe need not choose the status or
write the annotation.  If the existing delegation is judged sufficient, its
literal retained bytes and hash must be named; a conversational inference is
not an authority record.

## 4. Smallest workflow for annotation #1

1. **Admission-manifest packet (implementation author; claude-15 review).**
   Add the strict evidence resolver/manifest and runner input seam.  Commission
   missing, mutable, post-cutoff, duplicate and borrowed-evidence refusals.
   The close still defaults to typed absence, never an empty-is-complete view.
2. **Authority packet (operator/campaign owner, then independent review).**
   Pin the accepted rubric, nominate a non-codex-23 observer, authorize a
   distinct reviewer, and retain both literal authority records.  Joe provides
   the operator commit only if the campaign delegation is not already a
   pin-compatible authority.
3. **One-close preparation packet (execution lead; claude-15 reviews
   readiness).**  Choose one finite requirement suited to `:addressed`; declare
   its limbs and evidence ids before dispatch, configure redirected or approved
   production evidence destinations, and prove every source can be frozen
   before cutoff.  This creates no label.
4. **One authorized real run (execution lead).**  Produce one new close with
   occurrence, model identity, complete admitted evidence and honest outcome.
   A failed/incomplete run is retained and does not receive a convenient label.
5. **Blind retrospective observation (authorized observer).**  Provide the
   frozen criterion evidence without posterior or disposition fields; the
   observer either emits one proposed annotation or a typed
   insufficient/ambiguous/conflicting finding.
6. **Exact-subject review (claude-15).**  Inspect the unredacted frozen bundle,
   check leakage and every rubric limb, write acceptance or rejection, then run
   the accepted validator.  Only a positive validator result becomes
   annotation #1; pairing and A estimation remain later packets.

## 5. Hazards and controls

### Outcome and posterior leakage

The rubric expressly forbids model posterior and target close disposition as
label sources (`RUBRIC-observed-status-v1.md:28-33`).  The observer should
therefore receive a deterministic criterion view that excludes
`:entity-state-at-close`, belief/posterior fields, close `:outcome`, and any
morning-brief prose that merely restates that outcome.  It should expose the
subject identity, occurrence/time/cutoff, predeclared criterion, and admissible
raw evidence.  The full bundle remains available to the reviewer to detect
conflict; blinding is not permission to hide contrary evidence.  Controls must
show that adding a disposition or posterior does not change a label and that
using either as the only support refuses.

Selection bias remains: choosing only apparently successful closes after
seeing their outcomes would bias A.  Close eligibility and observation routing
must be fixed before outcome reveal, and refusals/abstentions retained.

### `:reopened` cannot bootstrap itself

The reopened criterion requires an already independently accepted terminal
annotation plus later renewed-live evidence and an authorized reopening
decision in one revision chain (`RUBRIC-observed-status-v1.md:147-161`).  The
first annotation cannot be `:reopened` unless such a prior accepted annotation
already exists for that entity.  Current count is zero, so reopening is not a
route to annotation #1.

### Retrospective temporal strain

The validator permits `cutoff <= annotation-created-at <= reviewed-at` and
requires every evidence observation time `<= cutoff`
(`SPEC-measured-a-annotation-v1.md:34-38`;
`measured_a_annotation.clj:103-138,216-220`).  Retrospection therefore strains
neither creation nor review time.  The risk is falsely backdating evidence:
filesystem mtime, a later copy, a git commit date, or a morning brief written
after close cannot prove pre-cutoff existence.  Only the close's admitted
byte/digest manifest (or an independently timestamped append-only record
already named by it) can do so.

Also, the new carrier defines cutoff equal to close time, while its observed
state evidence must be strictly earlier (`SPEC-close-retention-v1.md:21-22,
35-39`).  A retrospective annotation can never be written back into that
already closed state port.  It remains a separate annotation-to-close join.

## Source-byte pins

SHA-256 was recomputed over each cited whole file:

| source | SHA-256 |
|---|---|
| `RUBRIC-observed-status-v1.md` | `5999521d6e712db5824412a995287d5f1276dc37d08fe504748879226512e788` |
| `rubric-authority-draft.edn` | `b03d5343c2e2305d3e7ea26f69be815a49bbaf668e9fb4e951aee031e0215e20` |
| `SPEC-close-retention-v1.md` | `c14a8a87aedd2ad999def4971abb7377bb721a8a788b081e33b3064e3fdd5899` |
| `src/futon2/aif/close_retention.clj` | `555ff470065fdba4c92b8869c3911d5b33f14fea91f078baea97c583ec617ead` |
| `src/futon2/aif/full_loop_cohort.clj` | `e68fe87821140a2f08895cb2de83db1a030152a40081fbbd62c1b7582a977c1b` |
| `src/futon2/aif/full_loop_runner.clj` | `652b49e90fd70dd4671691c7dc99b13c82363f89b14ca46646a0f3c9da60dc6b` |
| annotation validator spec | `6815b48da949ee9b3b1aa87b5f4221f5852602910ac241ffcbc39454810522ec` |
| `src/futon2/aif/measured_a_annotation.clj` | `6bbf9dff4b35253031e566f5b166e2e454f8a89c793f8948b2dd75e8f3205a85` |

The last hash was recomputed with the same whole-file SHA-256 method as the
others; the futon3c precedent hashes are stated inline with their paths.
