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

## Verdict (claude-16, 2026-07-05 — the test ran; read both parts)

**Registered test (threshold NOT adjusted): KEEP.** On the mined move-set
(diffsub-moves-mined.edn, 232 moves, scope-grain v2), 20/20 top-ranked
policies (100%, all depth-3) have G strictly below their 1-step prefix's
G, and the global best rollout (−0.00284) strictly beats the global best
1-step (−0.00108). 100% ≥ 15%. Full rows in scripts/kill_test_rollout.clj
output (re-runnable, read-only).

**Supplementary diagnostic (post-hoc, clearly labeled): the pass is
VACUOUS.** The G magnitudes carry the additive signature (3-step ≈
1-step × discount series), and the decisive check confirms it: the best
depth-3 rollout is MOVE-FOR-MOVE IDENTICAL to the iterated-greedy chain
(greedy, apply, re-greedy ×3) — same sequence, same G, search adds
nothing over repeated greedy. Every longer policy "beats its prefix"
because negative costs accumulate, not because step-1 unlocks step-2.
The 2026-06-09 criterion measured accumulation, not unlocking — a
specification error in the original pre-registration, visible only now
that the test ran.

**Recommendation (operator-gated):**
1. RE-REGISTER the discriminating criterion: "best-rollout strictly
   beats the iterated-greedy chain (different sequence or lower G) on
   ≥ X% of roots" — X proposed 10%, fixed before the re-run.
2. Pending that re-run: the multi-step SEARCH (depth>1 expansion) has
   no demonstrated value-add; the doc's own fallback applies ("let the
   cascade lane carry the value"). DEMOTE the search, do not delete:
   `project-policy`/coverage-G evaluation is now LOAD-BEARING in the
   post-classical ΔG reconciliation (rollout → escrow), and the
   depth-1 path is the live one.
3. The kill-test script stands as a re-runnable gate for the re-run.

## Operator resolution (2026-07-05, supersedes the recommendation above)

The vacuous pass is not an encoding accident — it is a LEVEL ERROR
(operator's reading, verbatim in substance): have→want arrows are used
directly as moves, when their designed role is as magnets for cascades.
Arrow-level rollout is ROUTEFINDING over a decomposition, and
decomposition is additive by construction — iterated greedy is provably
optimal, and the search "looks for a decomposition signal that,
mathematically, we can never find." Dependency-edge enrichment (option
1 above) would be better routefinding but still routefinding.

RULING: the arrow-level depth>1 search is RETIRED ON THEORY, not
pending a re-run. project-policy valuation STAYS (load-bearing in the
post-classical ΔG reconciliation). The successor question is a rollout
at the CASCADE level — composing pattern languages (super-additive by
design) to address bigger problems — chartered via E-gflownets-fold
(GFN-for-the-cascade) + the cascade-peripheral idea; registered
criterion in sequel-notebook.org §5.3 when the machinery exists.
