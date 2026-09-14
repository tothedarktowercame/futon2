# Independent review acceptance — close-retention operand discovery

Reviewer: claude-15, 2026-09-14. Scope: codex-24 commit cffa43d1
(TN-close-retention-discovery-2026-09-14.md, read-only, single file).
Verdict: ACCEPTED.

Checked against source (not the note's claims):

- File scope: one technote; nothing else in the commit range.
- All six pins recomputed — match.
- close! body read at full_loop_runner.clj:3008-3052: close-state is
  built by rereading @selected-entity-belief with a freshly sampled
  Instant/now; the closed term carries no :closed-at; and the return
  of cohort/close-attempt! — the only value holding the writer-minted
  :recorded-at — is discarded. The three sharpest verdicts
  (state never observed post-action; closed-at minted but not in the
  submitted cell; cutoff never minted) all follow.
- grep confirms selected-entity-belief is written exactly once
  (reset! at :3224, during selection) and only dereferenced at close
  — the stale-by-construction hazard is real, not stylistic.
- measured_a_annotation.clj:103-115 read: exact conditioning key set
  with nonblank :transition/id and :action/id and point
  :post-action-at-close — the no-coercion verdicts (click/attempt/
  sequence/job ids are not occurrence ids) are grounded in the
  validator, not just in taste.
- The seven cutoff candidates each carry a real code point; candidate
  6 (cohort closed-event :recorded-at) is correctly identified as the
  only single-writer instant, and the note correctly refuses to
  collapse evidence-freeze and cutoff semantics silently.

Consequence: the implementation route is now fully constrained. Four
design anchors (occurrence minting point, observation port with typed
absence, declared model identity or typed absence, closed-at/cutoff
from the single writer with a separate earlier state-evidence freeze)
are settleable by the accepted spec + rubric + standing no-coercion
rulings — decided in-lane by the execution lead in the next packet;
no Joe ruling required unless implementation shows an anchor cannot
be honored.
