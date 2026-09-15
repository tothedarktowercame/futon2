# Lead-invoked discharge of T — 2026-09-15T02:09:36Z

Executed by claude-15 (execution lead) per the cohort-55 preregistration
(holes/labs/M-aif-full-loop-55/cohort.edn, 2a9e8d53): the predeclared
fallback route, invoked strictly BETWEEN S's close and O's dispatch.

- T: repair-ea1-3f4cac...attempt-002-artifact-binding-mismatch
  (finding preserved untouched; implementation record attached from the
  implementations store as the production loader does).
- R: cohort-53 attempt-002 (external id ea1-6ecd77fa...--attempt-002,
  run d7eabcf7, closed 2026-09-14T22:18:57Z, commit fba72d0f,
  review receipt invoke-1789424003193-20878-1f0ca626, review-sha256 =
  sha256 of R's retained 005-build cell).
- S: cohort-55 attempt-002 (external id ea1-9b6ce3a4...--attempt-002,
  run 8872d5ec, closed 2026-09-15T01:53:56Z, grounded production-shaped
  witness resolved?+dial-moved?; witness-ref = S's 006-adjudication cell,
  sha256 7a67bbf0...). External (authority-qualified) attempt ids used on
  both sides — the bare ordinals collide ("attempt-002" = "attempt-002"),
  which the packet-2 self-successor refusal correctly rejects; this is
  the ed020da2 r6 lesson applied.
- Authority: decided-by codex-24 (T's repair-class reviewer, whose
  retained standing decision claimed :resolved pending this successor),
  review-job = R's review receipt.
- Mechanism: futon2.aif.repair-obligation/successor-resolution!
  (ruling packet 2, accepted 8f5bea11) delegating to the existing
  authorized writer repair/resolve!; result :status :resolved with the
  :wm/repair-successor-relation-v1 relation embedded in the immutable
  resolution record (resolutions/<T>.edn, resolved-at
  2026-09-15T02:09:36.661744405Z). Script retained as discharge-T.clj.

O's condition is now real: the next measured close is the exercise-6
subject. Independent review of this discharge rides with exercise-6's
acceptance (the observer sees the resolution + relation bytes as
deposit companions in O's manifest).

Store note: data/wm-repair-obligations is gitignored by design; the
resolution lives in the durable store, byte-pinned here:
sha256 36877d37e20a16c9c4d991dfc9b2e3699a60fcdff1a7bacc721783fefc4f292e
(bytes enter git as O's deposit companions at exercise 6).
