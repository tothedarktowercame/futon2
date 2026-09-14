# Independent review acceptance — B-prime standing-decision completion

Reviewer: claude-15, 2026-09-14. Scope: codex-24 commits 26694d1e
(implementation + controls), 35ffea78 (delimiter fix), 322db5c9
(legacy arity restore), ea9ca265 (one-shot completion controls),
5ba3d859 (receipts). Verdict: ACCEPTED.

Checked against the ruling's six constraints:

- (1) :measured-acquisition? is an explicit boolean runner option,
  validated at the boundary (:measured-acquisition-option-invalid on
  non-boolean); flag absent = byte-identical behavior; evidence/
  contents alone never trigger enforcement.
- (2) valid-standing-decision? validates via limb-evidence (so the
  >= 80-char :explanation applies) and binds :decided-by /
  :implementation-author to the ACTUAL reviewer/author seats of the
  dispatch, not string trust; wrong seat = malformed = completion
  path.
- (3+4) exactly ONE completion request, through the standard
  dispatch/poll seams as its own phases
  (:standing-completion-dispatch/-wait), prompt exactly as ruled
  (content is the reviewer's; nothing suggested); reviewer writes the
  file itself — no runner-persistence transport.
- (5) an appearing deposit enters via the ordinary admission path,
  hence pre-cutoff; no backfill surface exists.
- (6) absent/malformed after the one request throws typed
  {:failure-kind :standing-evidence-insufficient :outcome
  :incomplete} — the failure-path close retains everything acquired.
- Controls: one-dispatch-one-wait proof; deposit-appears-during-wait
  success; already-valid short-circuit (no redispatch); insufficiency
  typing; prompt gating on/off. Receipts: honest trail (unmatched
  delimiter, then a private-arity regression, each fixed by follow-up
  commit); final tree gates kondo 0/0, parens OK, fresh-JVM 162 tests
  / 879 assertions exit 0 (README clarifies the swapped receipt
  filenames; tests-after-helper.out is the final run).

Reviewer follow-up (mine): the one-line futon3c click passthrough for
the flag, then cohort 52 and the first fully-armed exercise.
