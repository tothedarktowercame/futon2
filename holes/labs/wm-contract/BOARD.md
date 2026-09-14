# wm-contract board — refreshed every claude-15 tick

Updated: 2026-09-14 ~22:15Z (containment accepted; attempt-002 firing)

## IN FLIGHT
- **cohort-53 attempt-002 measured click** — fires as this tick's last action, with
  worker-only casting (author zai-5, reviewer codex-22, repair-reviewer codex-24)
  per Joe's role instruction + CASTING-AMENDMENT-2026-09-14.md. Loop fallback
  wakeup ≈25 min; the run writes cells under
  `data/wm-full-loop-machinery-53/.../attempt-002/`.

## DONE (this sitting, newest first)
- Serving JVM reloaded from master: admission fix, close containment, stale-claim
  freshness, and seat registration all live; cohort 53 confirmed bound.
- zai-5's `386cfe33` independently reviewed — accepted with notes (`04267707`);
  note: `wm-full-loop` is now a registered seat once this runs, so in-thread
  bellbacks to it become routable.
- Close-failure containment ACCEPTED: codex-24 `10090021..578ee155`, acceptance
  `0198384c`. A close-time refusal or crash now writes a typed 007 + repair
  obligation; the cohort-53 attempt-001 orphan stays as the retained counterexample.
- Pair-companion admission ACCEPTED (`2ed49ee5`, acceptance `286d5967`).
- Role change (Joe): claude-15 out of the loop cast; BOARD.md visibility format adopted.

## NEXT (in order)
1. On attempt-002 close: verify the 007 (typed or grounded, either is informative),
   check B-prime standing-decision enforcement if it reaches approval.
2. If the close's build cell retains an approved review → it is the pre-declared
   exercise-5 subject: blinded view → codex-25 observation → my review →
   annotation #1 if a status is proposed.
3. Small cleanup packet sometime: dedupe the two containment sites; injectable
   seat registration so tests stop touching the live mesh.

## BLOCKED ON JOE
- Nothing.

## POLL HANDLES
- Job: `GET localhost:7070/api/alpha/invoke/jobs/<job-id>` · Parks: `GET localhost:7070/api/alpha/parked`
- Attempt cells: `futon2/data/wm-full-loop-machinery-53/wm-contract-machinery-53-v1/`
- Machine ledger: `runs/outstanding-dag-2026-09-14/STATUS.edn` (16 entries after this commit)
