# Independent review acceptance — a-labels acquisition design

Reviewer: claude-15, 2026-09-14. Scope: codex-22 commit 1bfb6b89
(TN-a-labels-design-2026-09-14.md, design discovery, single file).
Verdict: ACCEPTED.

Checked:

- File scope: one technote, nothing else.
- All nine source pins recomputed (seven futon2, two futon3c) — match.
- The authority-mechanics claims verified against
  measured_a_annotation.clj:170-220: exact six-key authority schema,
  observer/reviewer distinctness enforced by refusal, byte-source
  origin/authorization with recomputed hashes, acceptance
  subject + pr-str hash + artifact-bytes-parse-to-core equality —
  the proposed binding order is grounded in the real validator, not
  paraphrase.
- Per-status feasibility verdicts consistent with the accepted rubric
  text (criterion line ranges spot-checked against my own rubric
  review): :addressed correctly identified as annotation #1 candidate
  and correctly NOT derivable from :grounded-change/:resolved? — the
  disposition-cannot-label rule survives the design.
- The admitted-evidence analysis correctly reads the carrier
  (ids-only vector; observed-state id must be admitted) and correctly
  concludes a string id proves nothing without a resolver/manifest
  with source hashes and admission instants.
- Hazards section adds two real constraints the packets must honor:
  observer blinding (no :entity-state-at-close, posterior, :outcome,
  or outcome-restating prose in the observer view; reviewer sees the
  full bundle) and selection-bias discipline (close eligibility and
  observation routing fixed BEFORE outcome reveal, refusals retained).

Adopted into the plan (workflow of TN section 4):

1. Admission-manifest mechanism packet (dispatched; split spec+pure
   mechanism first, runner seam second, per the close-retention
   pattern).
2. Authority packet: execution lead drafts observer-origin and
   reviewer-authorization records citing the retained ruling bytes of
   LEAD-DECISIONS-2026-09-12.md:184-195 ("The measured-A owner must
   establish an observed categorical status authority") as the
   charter; observer seat must be neither codex-23 (rubric author)
   nor claude-15 (reviewer) nor the annotation's implementation
   author; proposal: an uninvolved seat (e.g. codex-25). Joe veto
   point recorded in the lead report; operator commit only if the
   cited delegation is judged not pin-compatible.
3-6. One-close preparation, one authorized run, blind observation,
  exact-subject review — sequenced after 1 and 2.
