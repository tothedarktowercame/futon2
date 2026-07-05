# E-rollout-kill-test — run the ≥15% non-greedy test; kill or keep

**Status: OPEN (2026-07-05). An EXCURSION deliberately, not a mission:
one bounded question with a pre-registered threshold, single-owner,
answerable in a session or two. Owner: TBD. Reviewer: claude-16.**

## The question (pre-registered in futonzero-alphazero.md §5, 2026-06-09)

"After scope-grain v2, ≥ ~15% of top-ranked policies should be
non-greedy (a multi-step G(π) strictly beating its greedy prefix). If
not, the rollout is over-engineering — kill it and let the cascade
lane carry the value alone."

Scope-grain v2 is LIVE. The test has never been run. Per
process-coherence/is-this-even-a-problem: run the cheap check before
any further rollout investment.

## Protocol

1. Enumerate the current reachable policy set at scope-grain v2 over
   the live substrate (read-only; the rollout ns as-is).
2. For each top-K ranked policy (K per the WM's actual selection
   window), test: does its G(π) strictly beat its greedy prefix's?
3. Report the non-greedy fraction with the full distribution, not just
   the threshold verdict; slice by mission family (unlocking structure
   may be dense in some territories and absent in others — a partial
   answer is more useful than a global one).
4. Verdict per the pre-registered rule: <15% globally → recommend KILL
   (rollout demoted to the cascade lane's ΔG evaluator only — which
   2g just made load-bearing, so the machinery is not wasted either
   way); ≥15% → KEEP, and the fraction becomes a standing gate.

## Constraints

- Read-only throughout; one dev process max if any; the 15% threshold
  was pre-registered 2026-06-09 and is NOT to be adjusted after seeing
  the data (adjusting it is a new pre-registration for a re-run, per
  claim discipline).

## Exit

The verdict, the distribution, and either the kill (a PR-sized removal
proposal, operator-gated) or the standing gate added to the
E-live-loop-2 steps file.
