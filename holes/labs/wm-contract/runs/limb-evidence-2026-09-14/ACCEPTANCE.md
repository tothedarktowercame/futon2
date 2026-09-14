# Independent review acceptance — limb-evidence v1 contract

Reviewer: claude-15, 2026-09-14. Scope: codex-24 commits 9e99d901
(spec + mechanism + controls), 05c45463 (receipts), plus reviewer fix
81c10574. Verdict: ACCEPTED.

Checked:

- File scope: spec + one new namespace + one test namespace +
  receipts; no existing namespace touched.
- All three record shapes per the anchors: limb receipt demands raw
  byte digests for stdout/stderr and an integer exit (the
  prose-receipt control shows exactly the gap the blinded observation
  named refusing as :shape-invalid); the standing decision refuses
  :decided-by = :implementation-author BY NAME
  (:standing-decision-not-independent) with no defaults for either
  seat; the revision pair refuses an unchanged byte pair and requires
  nonempty keyword dimensions.
- validate-limb-bundle: contract-order coverage report, absence typed
  (:incomplete with the absent vector) rather than erroneous, and
  only VALIDATED limb receipts provide coverage — decision records
  and revision pairs cannot silently stand in for a mechanical limb.
  The control exercises the real four-limb discharge contract and
  shows :grounded-repair and :distinct-production-shaped-successor
  absent — the honest current state.
- Receipts: kondo 0/0, full-driver parens, fresh-JVM 4 tests / 16
  assertions exit 0 at the pinned tree; clean first pass.

Review fix applied by reviewer (81c10574, not re-belled): the
revision pair validated capture instants individually but never their
ORDER — an inverted or equal pair (a stale capture posing as a
revision) passed. Now refuses :revision-order-invalid; two controls
added (inverted and equal); gates re-run, 4 tests / 18 assertions.

Next (wiring packet): runner-side admission of attempt-scoped
evidence records into the close manifest, stop-the-line on an invalid
record, so a future close can carry these artifacts pre-cutoff.
