# Standing runner contract (fold/flight lanes, Slice 2-live)

Give this file to every new runner IN THE FIRST BELL, before any assignment.
It exists because mid-batch corrections cost a round-trip each (zai-2 learned
the gate rule at flight 5 of 6; the operator rightly asked why it wasn't in
the handoff). Versioned here; the TN records when it changes.

## Gates — the exact commands, no variations

A gate claim without the gate's own output is a false claim even when the
artifact happens to be fine. RUN the gate ON THE ARTIFACT, PASTE the verbatim
output. "The script exited 0" is not the artifact passing its gate.

1. Paren/EDN balance (any .edn you write or edit):
   `emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval "(arxana-check-parens-cli)" -- <file>`
   A pass PRINTS the word `OK`. Paste that line. Silence or anything else =
   FAIL: fix the file, re-run, only then bell back. (Two artifacts in batch 2
   were one brace short behind claimed passes — one claim was a non-run of the
   checker. The reviewer re-runs every gate; discrepancies are recorded.)
2. Tests: run the suite named in your assignment FROM A DIFFERENT cwd than
   where you wrote it, paste the summary line (`N tests, M assertions,
   0 failures, 0 errors`). Everything executable uses FILE-RELATIVE paths.
3. Self-report failures: a first-run FAIL that you fix and re-run is a normal,
   creditable event — say so in the bell-back (batch-2 F6 precedent). A
   claimed pass that the reviewer's re-run falsifies is the discipline
   failure.

## Blinding

Never open anything matching `*.SEALED.json` under
`futon2/holes/labs/slush-demo/findings/proposals/`. You must not learn which
proposer authored a cascade. This holds even if a file you legitimately read
mentions proposals; the seal is the boundary.

## Scope

Artifact-only: no meme.db writes, no substrate-2 writes, no JVM interaction;
new files in mission lab dirs only (an assignment may name specific existing
lab files as editable). Operator-owned forks escalate to the operator via your
reviewer; warrants cover which/how within the envelope, never whether-outward.

## Honesty conventions (minted by your predecessors — keep them)

- STOP-on-blocker: missing data source / impossible criterion → bell back with
  what you searched; never synthesize, never pad.
- Satiety: `:partial` = a needed part of the pattern is unavailable or does
  not transpose; `:full` + policy-hole = the excluded part is
  inapplicable-by-judgment (excluding it is correct application).
- Honest overlap: two patterns grounding the same contribution are recorded as
  a policy-hole, never presented as independent contributions.
- Policy-holes are first-class; single-pattern cascades fold at their true
  size (holes carry the story); large cascades earn their wiring (seq = real
  order dependency, copar = co-active lenses).
- Psi is authored FRESH from the mission doc per deposit (resolver-blind,
  first-contact discipline), `:psi-sha256` = real sha256 of the psi string
  **as the EDN reader decodes it** (escapes like `\n` resolved to real
  characters — hash the value, not the source text; convention settled at
  Fold Run 10, where the runner was right and the reviewer's checker wasn't).
  Compute the sha FROM the canonical EDN-decoded value (e.g. Clojure
  `edn/read-string` then hash), never from a source-side reconstruction —
  Unicode choices (smart vs ASCII quotes, dash forms) diverge silently
  (Run 23). `:pins [:psi-sha256]` only for blinded folds.
- Bell-backs end with one PUR-style prediction-error line: what surprised you.
  These have been the highest-value lines of every shift.
- Psi staleness guard: the psi is authored from the doc's CURRENT state, not
  IDENTIFY alone (IDENTIFY is the mission's FIRST phase — an attested want
  goes stale as later phases land). If your assignment flags doc-vs-code
  divergence, the landed work is HAVE and the psi covers what REMAINS.
