# PACKET: R20 valence level as a pure namespace (Hesp et al. 2021 §3.3)

From claude-2. One behaviour: the slow affective level that sets each click's
prior beta, as pure functions. No wiring into any route in this packet.

Authority: Lean `DarkTower/WarMachine/R20Run.lean` in ~/code/mathlib4 at
862666e926, section "The valence level" (lines ~112-160) and the examples
`example_valence_moves`, `zero_charge_keeps_valence`,
`rising_precision_favours_positive`, `run_betaPrior_constant_of_flat`.
Paper: futon2 refs, Hesp 2021 (commit 2570c8cc0), §3.3.2 :260-271, §3.3.3 :283-288.
Mirror the Lean definitions one for one; do not reconstruct from the paper.

## New file: src/futon2/aif/affective_level.clj

A level is `{:rate {:pos r+ :neg r-} :B {[k j] p ...}}`, where B k j is the
probability of k at the next click after j. A valence belief is `{:pos q+ :neg q-}`.

- `descend [level q]` = sum_k rate_k * q_k              (Lean `descend`, the prior beta)
- `charge [beta-prior beta-posterior]` = beta-prior - beta-posterior   (Lean `charge`, AC)
- `affective-evidence [level beta-prior beta-posterior k]`
   = rate_k / (rate_k - AC) * ((beta-prior - AC) / beta-prior)       (Lean `affectiveEvidence`)
- `valence-update [level q beta-prior beta-posterior]`
   = normalize (k -> q_k * evidence_k)                                (Lean `valenceUpdate`)
- `predict [level q]` = k -> sum_j B k j * q_j                         (Lean `predict`)
- `step [level q beta-prior beta-posterior]` -> map with :valence-after-update,
  :valence-next (predict of it), :beta-prior-next (descend of that), :charge.

Typed failures, never a silent fallback (return `{:status :absent :reason ...}`
or throw ex-info with `:error`, pick one and use it throughout):
- beta-prior or beta-posterior not finite or <= 0;
- rate_k - AC <= 0 for some k (the evidence formula is undefined there);
- q not a probability vector (negative entry, or sum not 1 within 1e-9);
- B column not summing to 1 within 1e-9.
In particular, a missing beta-posterior must NOT be read as zero charge.
`step` with an absent posterior returns a typed absence naming
`:beta-posterior-absent`, and does not move the valence.

Provide `hesp-level` (rates 0.5 / 2.0, B the identity) and `flat-level`
(rates 1 / 1, B the identity), as in the Lean examples, with a docstring saying
B is the identity only because Hesp do not print their B.

## Acceptance tests: test/futon2/aif/affective_level_test.clj

1. `example_valence_moves`: hesp-level, q = (1/2,1/2), beta 5/4, beta-bar 1:
   updated valence :pos = 7/11, next beta-prior = 23/22 (within 1e-12).
2. `zero_charge_keeps_valence`: beta-bar = beta leaves q unchanged, for a
   non-even q.
3. `rising_precision_favours_positive`: beta-bar < beta gives evidence(:pos) > evidence(:neg).
4. flat-level: over a 5-step chain with arbitrary positive beta-bar values the
   beta-prior is 1.0 every step.
5. Bad cases, each constructed and shown to fail with its named reason:
   absent beta-posterior; rate_k - AC <= 0 (e.g. hesp-level, beta 5, beta-bar 1);
   q summing to 1.2; a B column summing to 0.9.

## Gates

clj-kondo on the two files, futon4/dev/check-parens.el on both, run the new
test namespace, and scripts/warrant_suite.py. Work in futon2 master directly.
Commit (code + test in one commit).

Bell claude-2 back with a summary + commit shas.
