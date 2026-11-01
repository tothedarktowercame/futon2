# L5-20-L — Q6 needs the predecessor's decision only when the digests match (claude-2 → codex-5, after GR-2)

Owner of the Lean layer: claude-2. mathlib4 `darktower`; worktree `~/worktrees/mathlib4-l5-20-l`
(NOT under `~/worktrees/mathlib4/`, which is the primary); `.lake` symlinked to the primary builds
over the primary's oleans — say so in the bell, I rebuild the primary after review. futon2 half in
`~/worktrees/futon2/codex5-l5-20-l`.

## The defect

Click 1 of the 21:03Z relaunch: Q6 **and** Q11 unverifiable because `previousChoice` projects as
`.nr "previous selection: :choice-unretained"`: the predecessor (click 16) refused after scoring, so
it retained no choice. Lean Q6 reads `previousDecision` only under the premise `previousOutcome =
.refused ∧ previousInputDigest = currentInputDigest`; the digests differ (`bdca6b2a…` vs `9d187ef3…`),
so the fact is not needed, yet the reader's `assess` makes any `.nr` field unverifiable. Q11 lists
`previousChoice` among its required keys (`scripts/wm_check_run_facts.clj` ~line 183) although Lean's
Q11 reads `forbidden`, not the previous decision.

## Goal

`RunFacts.previousDecision : Option Decision`; `none` is the typed value when the predecessor
refused (it chose nothing). Q6's first conjunct becomes: refused predecessor on the same digest →
`currentDecision` is not itself the same refusal (a `.pick` or abstention satisfies it); when the
predecessor chose, `some d ≠ currentDecision` as now. Reader: `previousChoice` absent-because-refused
→ `none`, never `.nr`. Check script: drop `previousChoice` from Q11's required keys; keep
`previousOutcome`/`previousInputDigest`/`refusalHistory`. Projector: emit a typed
`{"refused": true}` (or the agreed shape) instead of `(nr :choice-unretained)` at
`wm_run_facts.clj` ~line 971, only when the predecessor's outcome is `:refused`.

## Tests

Lean: `#eval` cases — refused predecessor, same digest, current pick → Q6 holds; refused predecessor,
different digest, previousDecision none → holds; chosen predecessor same decision same digest → the
existing behaviour. Force a real rebuild (delete the two oleans first; a sub-second build is a
replay). futon2: export of 0068e006 → Q6 and Q11 no longer `.nr` on `previousChoice`; live-pin test.
Bell claude-2 with both shas, the build time, and the verdict on 0068e006.
