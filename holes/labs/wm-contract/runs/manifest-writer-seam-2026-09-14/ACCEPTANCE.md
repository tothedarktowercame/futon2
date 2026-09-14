# Independent review acceptance — evidence-manifest writer seam

Reviewer: claude-15, 2026-09-14. Scope: codex-24 commits 3b462553
(writer + controls), 35394131 (receipts). Verdict: ACCEPTED.

Checked:

- File scope: full_loop_cohort.clj + cohort tests + receipts;
  full_loop_runner.clj, close_retention.clj, evidence_manifest.clj
  untouched.
- All four anchors in the diff, all refusals inside event-record and
  therefore before write-new!: (1) :evidence-manifest only alongside
  :retention-inputs (:manifest-without-retention otherwise); (2)
  validate-manifest without source rereads, ordered agreement
  verified against the COMPLETED block (writer-stamped instants), and
  the new per-entry :admitted-at <= :recorded-at check refusing
  :evidence-admitted-after-close — the ordering duty the mechanism
  acceptance assigned to the writer is discharged; (3) durable
  payload carries :close-evidence-manifest and :close-retention with
  both raw markers dropped; (4) additive paths intact (retention-only
  and legacy cells unchanged; misplaced-marker guard extended to the
  manifest key on non-close checkpoints).
- Controls: agreeing happy path with durable readback equality and
  block-ids = manifest-ids; four refusal cases each asserting no
  event file (manifest-without-retention, reordered agreement,
  far-future admission, tampered manifest sha); misplaced manifest on
  :selection; all prior retention controls still green.
- Receipts: cohort kondo baseline-delta 0/0, test kondo 0/0,
  full-driver parens, fresh-JVM 21 tests / 96 assertions exit 0 at
  3b462553. Clean first pass — no failed attempts to retain.

The retrospective-evidence chain on the writer side is now complete:
a close can durably prove which literal bytes were admitted, that
their digests were recomputed at admission, and that every admission
preceded the close instant. Remaining: the runner slice that builds
the real manifest from this attempt's durably written sibling
records.
