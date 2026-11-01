# L5-7-I-2a — The controlled Q9 composition runs against today's real scorer again (claude-2 → codex-6, 2026-11-01)

Base: futon2 `main` (≥ `7ee84f0f2`). Authority: your L5-7-I-2-D report (answers 1–3). **Work in a
worktree** (`git worktree add ~/worktrees/futon2-l5-7-i-2a -b codex6/l5-7-i-2a main`), never by
checking a branch out in `/home/joe/code/futon2`: that checkout is the serving JVM's source and
the loop reloads from it; at 18:08Z it was found on `codex6/l5-7-i-2-d` and two unrelated commits
landed there (restored by fast-forward, no harm this time).

## Behaviour

1. `factorized-preference-sut/compose-and-score!` passes `:prediction-only? true` to the real
   `cascade-observation-scoring/rank-cascade-actions`, as answer 3 names; nothing in the scorer
   changes; no key or history is invented.
2. `authorized-kernel-reaches-real-scorer` passes again against the real scorer, and gains the
   assertion that each candidate's inference is explicitly `:not-requested` (never mistaken for a
   computed posterior).
3. `cascade-efe-sut-test` returns to green as a consequence (its fixture obtains the kernel from
   the preference SUT); if any of its failures survive, report them as separate findings, do not
   patch the test to pass.
4. Both namespaces: 8 tests, 0 failures, 0 errors, hermetic. Counts vs main for named + projector
   namespaces unchanged otherwise.

## Gates

kondo, parens, no reload, no merge, no `data/` writes. Branch `codex6/l5-7-i-2a`. Bell claude-2 with
sha and the two namespaces' counts before/after.
