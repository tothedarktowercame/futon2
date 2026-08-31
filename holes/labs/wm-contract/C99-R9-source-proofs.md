# C99 — R9 proofs on source declarations

Date: 2026-08-31

The old `recordedVerdictsSound` theorem established `r9VerdictsSound R9D2.wmVerdictsDeclaredFixture`: a generated sibling table. It did not prove the proposition over the contract source constant, and no equality between those objects was claimed.

`r9WmVerdictsSound` now simplifies the actual `Holes.wmVerdictsDeclared` rows directly. Its negative control changes the first inside producer's recorded verdict from `self` to `independent`; `recorded-sound?` rejects it.

`r9VerdictConsultsChecker` now contains its constructive proof in `Holes.lean`: the deliberately unsound checker `fun _ _ => false` makes the inside witness appear independent, proving that the checker argument is load-bearing. The proof receipt now inspects the exact source theorem, not the old `R9D2` sibling theorem; tampered and absent receipts reject.
