# The two placed/ready exclusions on click 4 (2026-11-06)

Scope: read-only diagnosis of `T-jvm-provenance-before-multi-jvm` and
`T-outer-belief-separate-machine-failure` in click `wm-click-ca5cf289`.
The retained selection record is
`data/wm-runs/selection-record-2026-11-06-wm-click-ca5cf289-ccf4-43eb-a747-a15a2d7472a8.edn`
(single-line EDN, cited below as `selection-record:1`).

## 1. `T-jvm-provenance-before-multi-jvm`: authority coverage gap

### Where and what was read

Production was in area-focus mode.  `outer-observation` classified every
Giraffe-field target against the pinned task-area corpus and retained focus
manifest (`src/futon2/aif/outer_observation.clj:270-303`).  The click's focus
areas were `#{"Agency" "voxterm" "memory" "Arxana"}`; its corpus SHA was
`98c8d019...` and focus-manifest SHA was `a2af3be4...`
(`selection-record:1`).

`area-focus/classify-target` looks up the target string in the corpus.  A
missing row yields relation `:unknown` and reason `:no-area-row`; otherwise
the row's `:area`/`:also` fields determine `:focus`, `:associated`, or
`:useful-elsewhere` (`src/futon2/aif/area_focus.clj:24-54`).  There is no row
for `T-jvm-provenance-before-multi-jvm` in
`resources/wm/focus/task-areas-v1.json`.  Thus the exact relation input was
not a malformed string: it was **no task-area row**.  `known-term-field` then
excluded the resulting `:unknown`, retaining reason `:relation-unrecognised`
and the authority pins (`src/futon2/aif/outer_select.clj:198-241`).

The ticket's pinned source was
`/home/joe/code/futon3c/holes/tickets/T-jvm-provenance-before-multi-jvm.md`,
SHA `a66ce6d7...`.  Its text plainly concerns serving JVM provenance: the
title states that a JVM cannot say what code it is (line 1), and the proposed
repair is `GET /api/alpha/provenance` on every JVM (lines 51-65).  It also
retains three acceptance bullets at lines 207-213.  None of that text is an
area declaration consumed by this reader; area classification comes only
from the separate pinned corpus.

### Verdict

This is a **true fact about the classification authority**, not a defect in
the lookup: the row is absent and the reader honestly refuses to infer one
from prose.  It is nevertheless a coverage gap, not evidence that the ticket
has no relation to the focus.

Supplying the missing task-area row is ordinary evidence authoring and review,
not necessarily a Joe-only decision.  The author must cite the ticket and
choose a supported area (plausibly `Agency` and/or `WM`) on evidence; the
choice must not be reverse-engineered merely to make Q2 pass.  Joe is needed
only if the intended ownership/area is genuinely ambiguous or changes the
area vocabulary.  No code red test is warranted.  A corpus coverage test could
report open, ready, placed targets lacking rows, but it should not invent their
relations.

## 2. `T-outer-belief-separate-machine-failure`: reader defect

### Where and what was read

Giraffe's retained row is ready, cone 0, unblocks 0, and contains this
completion receipt:

```clojure
{:schema :wm/completion-surface-v2
 :completion-class :completion-definition-not-found
 :checkbox-count 0 :criteria-count 0 :located-criteria-count 0
 :source-pin {:path "/home/joe/code/futon2/holes/tickets/T-outer-belief-separate-machine-failure.md"
              :sha256 "c63283c2..."}}
```

`giraffe-field` treats that exact class as undefined completion
(`src/futon2/aif/outer_observation.clj:52-54`) and, under the pinned exclusion
switch, emits `:completion-definition-not-found` with the receipt and switch
pins (`outer_observation.clj:180-229`).  This happens before relation admission.

The completion producer reads the pinned document bytes, then combines
unchecked boxes, `mission-criteria`, and `mission-c` prose.  It returns
`:completion-definition-not-found` only when all three are empty
(`src/futon2/aif/task_completion_surface.clj:8-37`).

The document is not actually missing completion evidence.  It has:

* heading `## Acceptance evidence` at
  `holes/tickets/T-outer-belief-separate-machine-failure.md:50`;
* three concrete acceptance bullets at lines 52-57: apparatus failure leaves
  target alpha unchanged and appears interoceptively; genuine task outcomes
  still update alpha; and before/after alpha plus dataset digests are retained.

The generic criteria reader accepts headings named `Completion criteria`,
`Acceptance`, `Acceptance criteria`, `Success criteria`, `Exit criteria`, or
`Done when`, but its regex deliberately rejects extra words after
`Acceptance` (`src/futon2/aif/mission_criteria.clj:38-41`).  Therefore
`Acceptance evidence` opens no criteria section and its bullets disappear
(`mission_criteria.clj:100-122`).  The older markdown reader is narrower still:
it recognizes only a `Completion criteria` heading plus numbered list, or an
inline bold completion paragraph (`src/futon2/aif/mission_c.clj:292-357`).

### Verdict and smallest repair packet

This is a **reader defect**.  The document uses a normal, semantically precise
completion heading and supplies checkable evidence bullets, but the aggregate
completion reader reports no definition at all.

Smallest fix: extend `mission-criteria/criteria-heading-re` at
`src/futon2/aif/mission_criteria.clj:38-41` to recognize exactly
`Acceptance evidence` (not arbitrary `Acceptance ...` prose).  The existing
bullet reader then returns the three criteria.  Their absent inline verdicts
correctly yield `:completion-prose-needs-locators`, rather than falsely
claiming completion; that class is not the undefined-completion exclusion.

Red test on the real seam:

1. Give `task-completion-surface/describe` a source-pinned ticket whose bytes
   contain `## Acceptance evidence` followed by these three bullets.
2. Before the fix it returns `:completion-definition-not-found` with zero
   criteria; after the fix it returns `:completion-prose-needs-locators`,
   `:criteria-count 3`, and three unlocated criteria.
3. Feed that retained row through `outer-observation/giraffe-field`; before the
   fix it is excluded as `:completion-definition-not-found`, after it is not
   excluded for that reason.  Preserve a falsifier showing prose such as
   `## Acceptance tests discussed` is not silently accepted.

This needs ordinary reader maintenance, not a Joe decision.  Supplying the
later checkable locators/verdicts is ordinary authoring too; the proposed fix
does not manufacture them.

## 3. Counterfactual `serves-a-way` result

If both targets were honestly admitted, `serves-a-way` would pick
`T-jvm-provenance-before-multi-jvm`.  Both retained Giraffe rows have cone 0
and unblocks 0, so the stable cone/unblocks sorts preserve their input order;
the JVM ticket precedes the outer-belief ticket (retained ranks 233 and 234,
source orders 539 and 543).  Cascade steps are stable and left-to-right
(`src/futon2/aif/outer_score.clj:122-158,171-198`).

An exact prediction digest cannot yet be computed honestly because the JVM
ticket's missing area row is precisely the evidence that determines whether
its relation is `:focus`, `:associated`, or `:useful-elsewhere`.  The model's
landing depends on that relation (`outer_score.clj:263-294`).  I therefore do
not assign it a digest.

Distinctness from `E-queries-and-scopes` is nevertheless determined once the
ticket has *any* recognized relation: the JVM ticket's retained unblocks is 0,
so its landing gain is `:none`; `E-queries-and-scopes` retained landing is
`[:unrelated :one]` and digest `a7d10268...` (`selection-record:1`).  Each
recognized JVM relation therefore produces `focused/related/unrelated` with
gain `none`, not `unrelated/one`, so its outcomes digest cannot equal the
existing class.  The exact second digest must wait for the independently
authored relation row.
