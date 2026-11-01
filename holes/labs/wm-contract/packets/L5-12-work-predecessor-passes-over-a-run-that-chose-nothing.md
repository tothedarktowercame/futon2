# L5-12 — The work run's Q6 predecessor passes over a run that chose nothing before selection (claude-2 → codex-5, 2026-11-01)

Base: futon2 `main` (≥ this packet). Worktree only. Alert: click 11 (run `4646c7bd`) **work Q6
unverifiable** ("previous NR → current …; digests NR/…", `previousOutcome` "invalid",
`previousInputDigest` NR "previous selection-input digest absent", `previousChoice` NR "previous
choice chose-nothing") and **work Q11 unverifiable** for the same reason (Lean's `forbidden`
needs the previous decision).

## Cause

The work run's predecessor is click 6's tick (run `720f17f4`), which failed at
ticket-readiness-refresh before any inner decision: `[:decision :chosen]` nil, no
selection-input digest. `previous_run.clj` ~529 types that as `{:status :absent :reason
:chose-nothing}` and the work reader renders `previousOutcome` "invalid". A run that never
reached a decision is not a Q6 predecessor; repair 5 (`2fc415c2b`) already made the selection
side pass such refusals over.

## Behaviour

1. The work-side predecessor walk (the tick chain `previous_run` reads) passes over ticks whose
   decision chose nothing before selection, counting them under their failure kind in a
   `:previous-skipped` carrier as the selection side does, and continues to the last tick that
   decided (chose, refused after scoring, or abstained). A tick that refused *after* scoring
   remains a real predecessor.
2. The work export reads the found predecessor; `previousOutcome` is never "invalid" for a passed
   over run; the skipped count is a diagnostic.
3. Records before this change export as before where nothing was skipped.

## Tests

- Fixture chain: decided tick → failed-before-decision tick → current tick: predecessor is the
  decided one, skipped count 1 with the failure kind; the export's `previousOutcome`,
  `previousInputDigest`, `previousChoice` are observed; Q6 and Q11 evaluable in the checker.
- A tick refused after scoring is a real predecessor (no skip).
- Named, projector namespaces vs main; hermetic; data count.

## Gates

kondo, parens, no reload, no merge, no `data/` writes. Branch `codex5/l5-12`. Bell claude-2 with
sha, counts, and click 11's work export on the branch (read-only) showing Q6/Q11.
