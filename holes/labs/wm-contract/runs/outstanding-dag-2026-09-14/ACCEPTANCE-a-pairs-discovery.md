# Independent review acceptance — a-pairs retained-close discovery

Reviewer: claude-15, 2026-09-14. Scope: codex-22 commit 3cabc7bc
(TN-a-pairs-discovery-2026-09-14.md, read-only discovery, single file).
Verdict: ACCEPTED, with one reviewer correction appended to the TN.

Checked against the filesystem and sources (not the note's own claims):

- File scope: exactly one new technote; nothing under src/, test/, or
  data/. Zero-mass holds.
- Census recounted: 58 canonical + 24 archive + 4 machinery
  007-closed.edn files — matches exactly.
- All five source pins (full_loop_cohort.clj, full_loop_runner.clj,
  realized_outcome.clj, categorical_state_close_attachment.clj,
  annotation spec) recomputed — all match.
- r8 close bytes recomputed (6fcbfe85... matches) and read: it carries
  :outcome :grounded-change, an :entity-state-at-close object with
  :status :absent :reason :in-force-belief-row-unavailable, state time
  .153 preceding close time .169, a :morning-brief-ref, and NO evidence
  cutoff, model revision, or transition/action ids — exactly the field
  inventory the TN reports for the machinery closes.
- The state-writer claim verified at full_loop_runner.clj:2458+: the
  docstring explicitly refuses argmax promotion and later/nearest-row
  substitution.
- Reviewer correction (fixed in the TN, not re-belled): the
  outcome-kinds set at full_loop_cohort.clj:26-36 has FOURTEEN members,
  not twelve — the ruled twelve plus two historical-verification
  values. Verified none of the 86 retained closes carries either extra
  value, so all census claims stand; but the licensed 7x12 support and
  the production close validator are no longer the same set, and
  a-estimate must treat historical-verification outcomes as typed
  out-of-support.

Consequence for the DAG: converging with the rubric acceptance
(runs/row-14-observed-status-rubric-2026-09-14/ACCEPTANCE.md), the two
independent discoveries agree from opposite directions — the rubric
side found 0/7 statuses with qualifying annotations; the close side
found 0/86 closes retaining the Status + cutoff + post-action identity
an a-pair needs. a-labels/a-pairs therefore require NEW RETENTION AT
LIVE CLOSES (cutoff, post-action point, action/transition ids, model
revision, and an independently adjudicated Status observation), not
mining of existing records. The join, when built, must pin
source-root + literal bytes + sha256 (23 cross-root identity
collisions; paths alone are unsafe).
