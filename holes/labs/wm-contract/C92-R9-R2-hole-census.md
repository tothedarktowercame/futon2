# C92 — R9 and R2 hole census

Date: 2026-08-31

Contract authority: `86186c37444ac9f1b9d54818b092bbbc586854f4`. No declaration is discharged in this pass.

## Three-way split

### Dischargeable (3)

1. **`r9VerdictConsultsChecker`** — For any witness known to be inside the declared producing part, there exists an unsound checker whose result is `independent`; this demonstrates that `independenceVerdict` actually consults its checker argument. Empty `Part` is vacuous because the membership premise cannot be supplied, not a counterexample. Evidence exists in `R9-D2-report.lean`: the constructive checker is `fun _ _ => false`. C87's proof receipt independently elaborates that exact theorem and reports no `sorryAx`. It needs the existing proof moved into `Holes.lean`, followed by contract regeneration/rebinding.

2. **`r9WmVerdictsSound`** — The pinned declared-part table contains thirteen `self` rows, and membership is computed from each producer and declared part. `R9-D2-report.lean` proves soundness, but currently over its generated sibling `wmVerdictsDeclaredFixture`; the source constant now has the same rows. It needs a direct `simp` proof over `wmVerdictsDeclared` in `Holes.lean`, so the proof is about the contract's object rather than a parallel fixture.

3. **`wmTraceR2`** — This is a data-carrier hole, not a proposition vulnerable to a degenerate quantifier. `R2-D2-report.lean` currently emits a content-pinned 801-row sibling constant (`54` files, SHA-256 `b2c3aeb4…`) with two explicitly ill-formed rows. It needs the generator to emit or install that literal as the source constant itself, with the content pin retained and absence rows explicit. Merely proving facts about `R2GeneratedFixture.wmTraceR2` does not define `Holes.wmTraceR2`.

### Blocked (1)

4. **`r2ContractCensusWmTrace`** — The claim is fixture-indexed and therefore not false by arbitrary-list instantiation: the census of the named `wmTraceR2` is exactly two. The generated sibling already passes `native_decide`. Discharge is blocked on `wmTraceR2` becoming the same source object; after that, the existing `native_decide` proof is directly reusable. The current note saying 792 entries is stale relative to the pinned 801-row fixture and must be updated during discharge.

### False (0)

None of the four has the degenerate universal defect. The only universal declaration, `r9VerdictConsultsChecker`, has a premise that makes empty carriers vacuous and has a constructive proof for every inhabited case satisfying that premise.
