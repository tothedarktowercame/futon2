# First renewal-4 click, stage by stage — run 2026-09-21-1790033693

claude-3, 2026-09-21. Click `wm-click-1541060a…`, cast wm-author / wm-reviewer /
wm-repair-reviewer, issued by claude-3 against renewal-4 (futon2 00e7f9d6).
Generated narrative: `narrative.md` in this directory (run-narrative, no rerun).
Asked for by claude-5: for each of A, B, C, G, E and the close-time update, did
it change the live choice or only write a receipt?

## Outcome in one paragraph

Selection chose M-aif-policy-conditioned-eig / :C1, the target behind last
night's surprise (:hole/h6378c65a4012, "Mint the shared posterior updater").
wm-author delivered commit aeb352f8 (one A4a Dirichlet updater shared by the
hypothetical and observed paths; 19 tests / 69 assertions, now warranted
test-registry-46627e13…). wm-reviewer executed the gates and approved;
grounding recorded the implementation and discharge. The close then refused the
whole attempt, :build-failed / :explanation-invalid, because the reviewer's
standing-decision file had the wrong shape (:decision :approve, :explanation
nested in a map-valued :evidence, its own :entity/id). The reviewer prompt listed
key names only; fixed in 6d45e8b7 with a literal template, pinned by a test that
feeds the filled template to the close validator. The commit is on main; only
its certification was lost.

## What each part did to the choice

| Part | Recorded state in this run | Effect on the live choice |
|---|---|---|
| A (observation model) | g-term verdict :degenerate, :identity-kernel. Perceive stage read 14 channels, 417/417 belief rows changed | None on the cascade choice. A is the identity over the checkbox tokens. |
| B (transitions) | Declared kernel of the verified macro-action. Token-belief carry REFUSED, :carry-domain-changed. No learned B, and no learning-trial state in the certificate | None beyond the declared kernel. Learned B was not consumed, so improve-1 has no evidence yet. |
| C (preferences) | g-term :non-degenerate, but the C audit gives 1.001309 : 1 for each wanted hole. 3 of 467 source tokens reached, 5 outcome tokens projected | Negligible. C moves G by thousandths of a nat. |
| D (initial state) | :degenerate, point mass | None. |
| G (EFE) | 9.704758 vs 9.706023 nats, spread 0.00126. Risk 4.852 per step, ambiguity identically zero | Did not decide. Near-tie, and the near-tie threshold is :undeclared. |
| E (habit) | 0.667 vs 0.333. Policy comparison records **decided-by :habit** | **Decided.** Posterior 0.667 / 0.333 is the habit prior. |
| F | :missing, :no-admitted-policy-prefix | None. |
| Q | :degenerate, :open-loop-no-conditioning | None. |
| Novelty (improve-5a) | :route-unavailable, novelty-consumed-in-G? false | Receipt only, and the receipt is empty. |
| Focus (improve-7a) | Both candidates classed :focus (discovered focus: WM) | Receipt only. |
| Preference audit (improve-2a) | Written (the 1.001309 : 1 above) | Receipt only. |
| Update at close | Close failed before the evidence manifest. The final close has no token-outcome comparison, surprise list, route attestation, kernel example or run-ending receipt. Those files sit in retained/, unadmitted | Nothing learned. See next section. |

So Joe's doubt about the foundations holds on this run. The live choice came from
E (habit, 2:1). G was a 0.0013-nat tie, because C is near-uniform, A and D are
degenerate and ambiguity is zero. B was the declared kernel.

## The receipts did not join (improve-8 baseline check)

claude-5 predicted this check would fail if the receipts weren't joining, and it
failed. The run-ending receipt in retained/ says :unknown, from a projection of
:grounded-change. The final 007-closed.edn says :build-failed / :explanation-invalid
with both grounded flags false. The kernel applied to the final close would say
:known-typed-failure. `verify-close` returns false.

Cause: every record-only receipt (token-outcome, surprises, route attestation,
kernel example, run-ending classification) is written to retained/ before the
close can still fail. The failure-path close is built separately, with no
manifest, and admits none of them. This is true of all of today's receipts, not
only improve-8a. It happened on the first click that exercised them.

Replaying the kernel over all closes now: 125 closes, 122 :unknown, 3
:known-typed-failure (this run is the third). The receipt on disk disagrees with
that count.

## Other things this run showed

- The author's commit changes how the A4a posterior is updated, but it carries no
  `Surprise:` trailer. The author prompt does not mention the rule, so the rule
  in the fix list cannot be met by a machine author as things stand.
- The reviewer found that `clojure -M:test -n <ns>` in futon2 treats `-n` as a
  file name, so it ran `clojure -X:test :nses '[…]'` instead. The feature card
  still quotes the `-M:test -n` form.
- The tick perceived in :stop-the-line mode with 9 open obligations. Selection
  proceeded, per the precedence ruling.

## Next, in order (nothing dispatched yet)

1. The failure-path close must admit or reclassify the receipts already written.
   A small runner change: either classify the final judgment and admit the
   retained receipts on the failure path, or write receipts only after the close
   judgment is final.
2. Put the `Surprise:` trailer rule into the author prompt when the selected
   target carries a recorded surprise.
3. Then Joe/claude-5's question, B or C first. This run supports C first:
   with C at 1.0013 : 1, G cannot separate candidates, so a learned B would not
   change the choice either (improve-1's finding).
