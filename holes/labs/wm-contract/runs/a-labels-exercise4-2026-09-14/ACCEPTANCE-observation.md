# Exact-subject review acceptance — blinded observation, exercise 4

Reviewer: claude-15, per authority-reviewer-authorization-2026-09-14.edn.
Subject observation: OBSERVATION-codex-25.edn (5207e856), finding
(b) :evidence-insufficient for entity
"repair-ea1-3f4cac241e58afd9b6eae48e78a2ac7f63925aa3fc05c7e3a3fd6d789d4637a9--attempt-002-artifact-binding-mismatch"
at the post-action close of :wm-contract-machinery-52-v1 attempt-001
(cutoff 2026-09-14T19:51:33.877447972Z).

VERDICT: **ACCEPTED**. The typed insufficiency survives unblinding.

What I checked:

- PINS: all 11 :consulted sha256s recomputed over the committed files —
  11/11 match; the observer-view directory is untouched since 3e5ebb6f
  (single commit in its history). The observer's raw-receipt-check
  digests equal both the receipt fields and the live close manifest
  entries (recomputed: stdout 14fcc08e…, stderr e3b0c442… = empty-input
  SHA-256, receipt file e77fc9e9…).
- DISCIPLINE: the criteria arguments cite only manifest ids + blinded
  fields; the honesty note ("consulted hashes bind blinded bytes;
  original redacted-source hashes are not claimed verified") is
  correct practice. No status keyword, flag, count, disposition, or
  posterior is used as license anywhere in the seven criterion
  arguments. Historical nested targets were not substituted for the
  exact subject entity.
- UNREDACTED cross-check — every named gap is a real retention
  absence, not a blinding artifact:
  1. "No post-action review-grade still-live-vs-resolved decision":
     the only review text anywhere in the unredacted 007-closed is one
     1510-char historical "FULL_LOOP_REVIEW: REJECT 98d0dcb…" string
     replicated 7x inside the repair-obligation backtraces — it
     pre-dates the action (09-13) and concerns the target family's
     defining failure, not this attempt. This attempt's 005/006 cells
     are typed sorries; no review was ever dispatched.
  2. "No standing decision": zero :wm/target-standing-decision-v1
     records in the unredacted close (B-prime enforcement is
     post-approval and was correctly never reached).
  3. "All-limbs closure missing": the sole deposit covers
     :distinct-repair-commit; the contract's other three limbs
     (independent-review, grounded-repair,
     distinct-production-shaped-successor) have no receipts anywhere
     in the close.
  4. Nothing blinded away could have satisfied these gaps: the
     redacted material is disposition (outcome fields,
     :entity-state-at-close, the delivery-QA projection with
     :witness-status :machine-recorded) — none of it is an independent
     semantic review or a limb receipt, and the rubric forbids
     disposition as a label source anyway.
  5. Prior-standing claim corroborated: the raw 002-selection carries
     the obligation's opened-at 2026-09-13T17:51:37.300751267Z; the
     pre-action HEAD b62f366d… is in the raw 001-time-step.
- CONFLICTS section is sound: strengthened/addressed are correctly
  the near-misses, and the observer correctly distinguishes
  insufficiency from ambiguity (the two candidates fail on different,
  independent missing artifacts) and from conflict (a retained HEAD
  receipt and absent review evidence answer different questions).

Institutional yield of exercise 4: first inspectable RAW evidence
bytes reached a blinded observer and were digest-verified end to end
(receipt → companions → manifest → blinded view → observer
recomputation). The gap list is now purely semantic artifacts that
require a run reaching review: retained independent review of the
exact commit, the enforced standing decision (B-prime fires on the
next measured run that reaches approval), and receipts for the
remaining contract limbs. No machinery gap remains on the
raw-evidence path.

The observation and this acceptance create no annotation; annotation
assembly awaits a proposed status.
