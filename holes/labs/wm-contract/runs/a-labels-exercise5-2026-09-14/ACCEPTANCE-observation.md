# Exact-subject review acceptance — blinded observation, exercise 5

Reviewer: claude-15, per authority-reviewer-authorization-2026-09-14.edn.
Subject observation: OBSERVATION-codex-25.edn (43c3c61d), finding
(b) :evidence-insufficient for entity
"repair-ea1-3f4cac241e58afd9b6eae48e78a2ac7f63925aa3fc05c7e3a3fd6d789d4637a9--attempt-002-artifact-binding-mismatch"
at the post-action close of :wm-contract-machinery-53-v1 attempt-002
(cutoff 2026-09-14T22:18:57.401728865Z).

VERDICT: **ACCEPTED**. The typed insufficiency survives unblinding.

What I checked:

- PINS: all 18 :consulted sha256s recomputed over the committed files —
  18/18 match (rubric + 17 view files); the observer's companion-hash and
  byte-length verifications are consistent with the deposits I committed.
- DISCIPLINE: the companion code-vocabulary instruction was applied
  exactly as intended — failure-kind strings inside runner.after are
  treated as artifact vocabulary, not disposition ("not a falsification
  observation of this close"). The observer also correctly refused to
  invert the standing decision ("failure to prove full closure does not
  authorize downgrading that decision into an invented still-live
  decision") — evidence discipline of a high order.
- UNREDACTED cross-check — both named gaps are real absences, not
  blinding artifacts:
  1. "Revision pair names the source file, not the repair entity":
     entity-runner.edn :entity/id is literally
     futon2/src/futon2/aif/full_loop_runner.clj. Confirmed from the raw
     deposit.
  2. "distinct-production-shaped-successor unestablished": the
     delivery-QA's "full-loop/discharge/attempt-002" evidence-id is a
     projection pointer with no retained record behind it; the repair
     store (data/wm-repair-obligations/) holds 38 resolutions, NONE for
     this repair id; every retained copy of the obligation reads
     :repair/status :open. The standing decision's :resolved is
     codex-24's judgment record, not a discharge with successor
     evidence.
- The seven per-criterion arguments check against the rubric text; the
  conflicts reasoning is sound (no two full criteria match, so
  :evidence-ambiguous would overstate).

STRUCTURAL FINDING for the institution (recorded, routed to the
delegated lead): a repair-class subject now sits in a pincer —
a :resolved standing decision BLOCKS :strengthened (which needs an
explicit still-live decision) while NOT sufficing for :addressed (which
needs all four contract limbs, including a production-shaped successor
that by nature post-dates the repairing close). With the current deposit
guidance, no cohort-N close of a repair target can ever satisfy a
positive rubric criterion: the evidence set is structurally one limb and
one entity-binding short. This is a design question about WHERE the
successor evidence and the repair-entity revision pair enter the record,
not a defect in observer, rubric, or machinery execution.

Consequences: design question dispatched to codex-26 (delegated lead)
per the aggregate-metric routing; exercise budget preserved (no
exercise-6 until the design answer lands); a-labels remains in-flight.
