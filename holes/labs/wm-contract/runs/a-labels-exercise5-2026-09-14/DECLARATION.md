# Annotation exercise 5 declaration — cohort-53 attempt-002

Declared by claude-15 (execution lead), 2026-09-14, under the same
authorities as exercises 1/2/4 (observer codex-25 per
authority-observer-origin-2026-09-14.edn; reviewer claude-15 per
authority-reviewer-authorization-2026-09-14.edn; charter
LEAD-DECISIONS-2026-09-12.md:184-195).

ELIGIBILITY, fixed before outcome reveal: the cohort-53 preregistration
(holes/labs/M-aif-full-loop-53/cohort.edn, sha256
1c05f6b85c45107173219e17ddde77828977ab1e3ff5079ef793f1cf835d56ea,
committed and activation-pinned before any cohort-53 click) declares the
exercise-5 subject as "the FIRST measured-acquisition close of this cohort
whose build checkpoint retains an approved independent review ...
regardless of that close's outcome." Attempt-001 never reached review
(orphaned close, retained as the containment counterexample). Attempt-002
(click wm-click-d231142d, measured-acquisition true, closed
2026-09-14T22:2xZ) is the first close meeting the condition: its build
cell retains :approved? true with the reviewer's retained review text.
No second-chance selection occurred.

Casting: per CASTING-AMENDMENT-2026-09-14.md (Joe's instruction), worker
seats only — author zai-5, repair-class reviewer codex-24; the click
passed the casting explicitly. claude-15 held no seat in this run.

Deposits in this close (all admitted pre-cutoff, byte-verified): a
:wm/limb-receipt-v1 pair (static checks + test gates, with four verbatim
companion output files), a :wm/entity-revision-pair-v1 (byte-pinned
before/after of the revised runner source, with both revision companions
verbatim), and — for the first time in any exercise — a
:wm/target-standing-decision-v1 with :decision :resolved (decided-by
codex-24, distinct from implementation author zai-5, with a review-grade
:explanation), produced under the B-prime enforcement.

Blinding: same recursive redaction recipe and forbidden-key set as
exercises 2 and 4 (script /tmp/build-observer-view5.clj; committed view
grep-verified). One documented nuance: the six verbatim companion files
are the subject's OWN byte-pinned artifacts — two are snapshots of the
revised source file and two are gate receipts produced before the close
existed. They necessarily contain outcome-vocabulary WORDS (the runner's
own code and test-fixture phase logs mention :grounded-change etc.);
these are code vocabulary inside the subject artifact, not records of
this close's disposition. The close's own disposition fields (outcome,
grounded?, entity-state-at-close, delivery/morning-brief projections,
posterior/controller surfaces) are redacted from every EDN record in the
view, verified by grep over the blinded cells and close-conditioning.
The unredacted bundle goes to the reviewer at acceptance. The observer
applies the rubric as written; nothing here selects a status. A typed
insufficiency remains a valid exercise outcome.
