# Dispatch 2 — retained-record consistency check

Frozen before arithmetic. This is a prerequisite check, not the requested A/B scoring model.

Source: NOTE-test-registry.md, especially “registry entry” and “What the reviewer does instead of rerunning”. A verified complete record retains the exact code, tests, environment and actual results. It is not merely a passing hash comparison.

Construct a ten-position artifact: clean = ten zeros; dense bug = ten ones; sparse bug = a one only at index 9. Test i checks artifact[i] == 0. Thus every executed test catches the dense bug, only test 9 catches the sparse bug, and the full suite decides ground truth for this constructed domain. The author really executes all ten tests; the verifier reads that actual result vector. Hashes bind supplied bytes but are not, in isolation, a proof that execution happened.

Before observing results, each strength scenario has prior P(clean)=P(buggy)=1/2. A verdict qualifies only if its Bayesian confidence is at least 0.95 AND it agrees with the known constructed truth. Uniform spot-check index, all ten draws enumerated. Record mismatch refuses. No numerical costs or G preference distribution are invented for this check.

Question: can a wrong spot-check index conceal the sparse bug when VERIFY has already read the exact complete result vector? If not, dispatch 2's requested exposure requires a different starting record or evidence model. Dense-bug superiority of RERUN is likewise not assumed as a conclusion.

Adequacy error 0.05 or 0.20 cannot erase observed, decisive test results. Their effect on fallback costs requires a specified fallback rule. Missing items for the full comparison: whether the starting record is partial; what adequacy means under that record; rejection/fallback behavior; observation alphabet and C for G; fuel exchange rate. No G table is claimed until those inputs are explicit. No publication.
