# Make Q2, Q4 and Q8 decidable on every run, including failed ones

**Status:** OPEN — FIX NOW (Joe, 2026-10-18)
Parent: M-wm-aif-policy-grain-compliance

## Observed failure

On the 10-18 readiness clicks the Lean requirement verdicts that carry the
AIF content were never evaluated. Click 5
(`data/wm-runs/2026-10-18-f016ed51-62de-426e-88c2-768fa18b00dc/report-card.*`):
Q1, Q3, Q5, Q6 pass; Q7, Q9, Q10 fail; **Q2, Q4, Q8 not recomputable**.
Clicks 3 and 4 are similar (3/2/5 and 4/3/3 pass/fail/not-recomputable).

The carriers were present; their verification refused (from
`report-card.edn`, read by `scripts/wm_run_facts.clj` at `73495c50d02f`):

- Q4 facts (`gTerms`, `comparedPolicies`, `policyPreferences`, ...):
  `"Q4 carrier normalization-not-bound-to-g-terms"`;
- Q8 facts (`frontierCensus`, `frontierSelection`, `selectedCascade`), and
  through them Q2: `"Q8 carrier selected-not-strict-key-minimum"`.

Two consequences:

1. Whether a run's G and selection comply with AIF is unknown for every
   recent run, because the runs failed on tooling first.
2. A tie at selection (click 5 was `:chosen-by-tiebreak`) surfaces as
   "not recomputable" instead of as a failed requirement. The futon2 Q8
   verifier demands a strict minimum, while Lean's
   `IncrementalTaskFrontier.SelectionCertificate.Valid` uses `keyLE`
   (a tied minimum is valid) and `OuterTaskObjective.selected_is_minimum`
   names stable task identity as the tie law. Two copies of one rule disagree.

## Scoped task

1. Q4: bind the declared G normalization to the G terms in the selected
   decision's Q4 carrier, so `q4/verify-record` verifies and Q4 is decided
   (pass or fail) on every run that reaches selection.
2. Q8: make a tied selection a decided outcome, not a missing fact. Joe's
   ruling of 2026-10-18 is that a tie is a failure (a symptom of incomplete
   ingest), so Q8 should evaluate to **fail** on a tie, carrying the tied
   members and the absent terms. The Lean side currently accepts a tie
   (`keyLE`, identity tie law); changing it is a specification change and
   needs Joe's explicit confirmation in the ticket before the Lean file is
   edited. Until then, keep the futon2 verifier and Lean in agreement by
   reporting the tie explicitly rather than as not-recomputable.
3. Record the frontier census, selection, compared policies and G terms as
   soon as they exist in the run, so a later failure (reading, resume,
   build) does not remove them from the record.

Out of scope: changing how G is computed (see
`T-outer-target-novelty-term.md`, `T-outer-target-g-policy-prior.md`).

## Operator confirmation (Joe, 2026-10-18)

"Tied minimum fails" is already in the Clojure verifier; it should go into
Lean instead. So item 2 is confirmed as a specification change: Lean's Q8
(and the outer selection law it relies on, `keyLE` in
`IncrementalTaskFrontier.SelectionCertificate.Valid` and the identity tie
law in `OuterTaskObjective.selected_is_minimum`) should make a tied minimum
fail, and the Clojure verifier should then follow the Lean rule rather than
hold its own copy.

## Acceptance evidence

- Re-run `wm_report_card.clj` over the existing click-5 record (no new click):
  Q4 is pass or fail, not not-recomputable; Q8 is fail with reason tie, and
  Q2 is decided.
- A planted record with a strict minimum: Q8 passes.
- A planted record whose run fails after selection (e.g. at resume): Q2, Q4
  and Q8 are still decided.
- The report card's own Clojure predicates (`scripts/wm_report_card.clj`
  reimplements Q1-Q7 and Q10) agree with the Lean checker on all three
  planted records.

## Provenance

Found by claude-4 while checking which patterns have runtime certificates
(plop2026 docbook pass, `p4ng/plop-2026-code-annotations.json`
`certificate_coverage`), 2026-10-18. Joe asked for it as a fix-now ticket.
