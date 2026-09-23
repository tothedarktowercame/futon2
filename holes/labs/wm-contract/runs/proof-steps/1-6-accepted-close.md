# ⟨1⟩6 ruling applied: (b) reads the candidate's own produced tokens

zai-1, 2026-09-23. Build commit: see below. Re-evaluated read-only on
machinery-72 attempt-001's real records.

## The build

The runner's predicate call now feeds conjunct (b) the SELECTED candidate's
own declared produced tokens — `(get-in selection-judgment
[:controller-decision :action :precedence 0 :produces])` — measured at the
after-revision through the measurement producer's OWN rows: the d-task
evidence's `:after-token-evidence` (which covers the whole declared
universe, the candidate's tokens included), read through the same
digest-verified source port the comparison used. The horizon's predicted
chain stays in the prediction record; the comparison rows (prediction's
wanted) remain the (c)-facing and surprise-facing record, untouched.
Conjunct (c) unchanged.

## What the producer investigation found (no routing around)

The d-task evidence DOES measure `[T :repair/split-declared-valid]` — the
row exists, bound to after-revision ee22106c, `:file-present true`. The
reason the comparison rows never showed it is that `compare-outcomes`
compares only the PREDICTION's `:wanted` (the terminal chain token), so the
candidate's own token was measured but never compared. The wiring now reads
the producer's full rows — the gap was in the consumer's selection of rows,
fixed at the call site, not inside the predicate.

## The conjunct-by-conjunct verdict on machinery-72 attempt-001

- **(a) HOLDS**: `{:commit ee22106c :pre-dispatch-head c24c903b
  :descendant? true :corroborates? true :claim-in-author-window? true}`.
- **(b) FAILS — but for a NEW, honest reason.** The producer's row for
  `[T :repair/split-declared-valid]` reports `{:observed false :check :C4
  :evidence {:sha ee22106c… :file-present true}}`: the file EXISTS at the
  after-revision, but its disposition is written as an EDN string value —
  `:disposition "HELD-OUT-SPLIT-DECLARED"` on line 2 — not as a
  LINE-INITIAL declaration head, which is what C4's `check-decl-in-file`
  requires (the ticket file's `# Verify…` title and `**Status:** DONE`
  both start lines; this one does not). The measurement producer measured
  correctly and reported false correctly. The verdict:
  `{:accepted? false :failed :b
  :reason :declared-product-not-observed-true}` — the delivered artifact's
  shape does not satisfy its own declared locator.
- **(c) WOULD ALSO FAIL** — same Status-OPEN evidence as before.

## Honest conclusion

The expectation "(a) true, (b) TRUE, (c) false" does NOT hold: (b) is
false because the authored split file carries its disposition in the wrong
shape for the C4 locator the source declared. This is not a wiring defect —
the plug works, the producer measured, the predicate evaluated all three
conjuncts from real inputs. It is an authoring-shape mismatch between the
delivered artifact and the declared locator: either the ticket's later
limbs (or a touch-up on this one) write `HELD-OUT-SPLIT-DECLARED` as a
line-initial head, or the source's locator is re-declared to match the
EDN-string shape (a checker change, NOT done here — C4's line-initial rule
is the workspace's standing declaration discipline). Reported as found; no
B update is warranted; nothing was adjusted to make anything true.
