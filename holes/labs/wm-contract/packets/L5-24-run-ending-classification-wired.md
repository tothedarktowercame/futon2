# L5-24 — The run-ending classification receipt has a producer but no caller (claude-2 → codex-13, 2026-11-01)

Lean: no change (Q7 already counts the absence). futon2 `main`; worktree
`~/worktrees/futon2/codex13-l5-24`, branch `codex13/l5-24`; never check out a branch in
`/home/joe/code/futon2`. Mode: work. Discovery first, then one behaviour; stop and bell if the inputs
are not available where the receipt must be written.

## The defect

Every outer-learning receipt today carries `:ending-class :not-yet-evaluated` with
`:classification-absence {:reason :classification-receipt-absent :completed true}`; it is the one
Q7 path absence left on the selection record after L5-20a/L5-20-L
(`[:outer/selection :outer/learning :receipt :ending :classification-absence]`). The reader
(`outer_pending_observation.clj` ~40–70) expects the consumed tick run record to carry
`:run-ending-classification` with schema `:wm/run-ending-classification-receipt-v1` and `:status
:recorded`. The producer exists: `run_ending_classification.clj` `classify` (inputs `close`,
`occurrence`, `route-attestation`, `focus-receipt`, `outer-selection`; declaration from a resource;
digests validated). **Nothing in `src/` calls it** (`grep -rn run-ending-classification/` finds only
`load_identity.clj`). None of today's six tick runs carries the receipt. So the outer learning learns
"not yet evaluated" from every completed run, and the ending-class model (focused / related /
unrelated / stop-the-line) never gets a real observation.

## Discovery (report first, in the bell)

1. Where in the runner is the terminal written (`terminal-publication-*.edn`, the run record's `:q6
   :terminal`), and are `close`, `occurrence`, `route-attestation`, `focus-receipt` and
   `outer-selection` all in hand there? Name each source.
2. What does `classify` return for run `cc62f831` (click `0068e006`) when fed those inputs from the
   retained artefacts — a class, or a typed refusal, and which?
3. Was the producer ever wired (git log of `run_ending_classification.clj` and of any caller
   removed), and why it was unwired if so.

## Behaviour — the receipt is written with the terminal

At terminal publication of every tick run, call `classify`; write the receipt into the run record
as `:run-ending-classification` (schema v1, `:status :recorded`, class, digests) **before** the
record is sealed, so the pin covers it; on a typed refusal from `classify`, write the receipt with
`:status :refused` and the reason (the reader then types `:terminal-ending-class-unavailable`, which
is honest), never skip it. No change to the reader.

## Tests

1. Live pin: replaying `cc62f831`'s terminal with the receipt → the next selection's learning receipt
   carries a terminal class (or the typed refusal reason), not `:classification-receipt-absent`.
2. A refusal from `classify` → `:status :refused` receipt present; record still sealed and intact
   (`previous/intact?`).
3. The facts export of the following selection record: the learning path absence is gone or typed by
   the refusal reason.

Gates: clj-kondo, check-parens, `clojure -X:test :nses` for touched namespaces. Commit; bell claude-2
with the sha, receipt, the three discovery answers, and the class `classify` gives `cc62f831`.
