# wm-contract board — refreshed every claude-15 tick

Updated: 2026-09-14 ~21:55Z (after Joe's visibility request)

## IN FLIGHT
- **close-containment packet** — codex-24, job `invoke-1789421829469-20865-d22954a6` (running),
  park `park-e5c5c784`, deadline ≈22:20Z. Goal: a close-time refusal writes a typed 007
  instead of orphaning the attempt (the cohort-53 attempt-001 crash).
- Loop fallback wakeup armed for 22:03Z; the park bellback is the primary wake.

## DONE (this sitting, newest first)
- Role change (Joe): claude-15 leaves the loop cast; worker seats only. Memory recorded.
- Pair-companion admission ACCEPTED: codex-24 `2ed49ee5` + receipt `714d9dd8`, acceptance `286d5967`.
- Stale-claim freshness fix (in-lane, from the loop reviewer's finding): `94ce60fe`.
- Diagnosis: cohort-53 attempt-001 close crashed on companion admission; attempt orphaned (no 007).
- Cohort 53 minted/activated/rebound (`202e447a` / futon3c `cd88864f`); cohort 52 exhausted 2/2.
- Exercise-4 blinded observation (codex-25) accepted `:evidence-insufficient` (`c9e033a9`).

## NEXT (in order)
1. Review containment packet on bellback; acceptance record.
2. My independent review of zai-5's `386cfe33` (seat registration; loop-rejected, sits on main).
3. Reload serving JVM from master (reload, not restart — allowed).
4. Fire cohort-53 attempt-002 measured click WITH worker-only casting (author zai-5,
   reviewer codex-22, repair-reviewer codex-24) + dated casting-amendment note.
5. Exercise-5 subject (pre-declared): first measured close whose build cell retains an approved review.

## BLOCKED ON JOE
- Nothing.

## POLL HANDLES
- Job: `GET localhost:7070/api/alpha/invoke/jobs/<job-id>` · Parks: `GET localhost:7070/api/alpha/parked`
- Attempt cells: `futon2/data/wm-full-loop-machinery-53/wm-contract-machinery-53-v1/attempt-001/`
- Machine ledger: `runs/outstanding-dag-2026-09-14/STATUS.edn` (15 entries)
