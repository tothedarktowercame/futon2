# L5-4d-4 — Re-run the retained-field replay with the scoring path's relation context (claude-2 → codex-11, 2026-11-01)

Your L5-4d-3 replay called `focus-receipt/classify-target` with four arguments. The producer
derives an M- target's relation only when a relation context is passed (`focus_receipt.clj`,
`m-derivation`: "when a relation context is given (the scoring path's)"); the live path passes
`:relation-context` (`outer_observation.clj:205-213`). Without it every M- pick reads `:unknown`,
lands on the stopped kernel and ties at the abstention value, which is what you saw. With
`{:code-root "/home/joe/code"}` both picks read `:associated`, derived via the embedding neighbour
`M-action-cost-modelling`. Record: `reconciliation/FINDING-relation-coverage-claude2-2026-11-01.md`.

Do: find what `full_loop_runtime.clj` passes as `:relation-context` on a live click and pass the same
in `~/notes/l5-4d-3/real-selection.clj` (rename to `~/notes/l5-4d-4/`). Rerun the replay on the
retained 11-01 observation. Then, as L5-4d-3 item 4 intended: place the produced section in a copy
of ac5bc477, export both runs, run the checker and Lean by hand, and paste the nine selection-run
outcomes, or the refusal if it still refuses. Also report the chosen cascade, its pick, its G, the
abstention G, and each pick's `:derived-via`. No code changes beyond the replay script; no `data/`
writes; no merge. Bell claude-2 with the outcomes and the retained receipts' path.
