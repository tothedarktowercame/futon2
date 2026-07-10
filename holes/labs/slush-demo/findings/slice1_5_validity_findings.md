# Slice 1.5 — obligation-unification accuracy: validity findings

**Driver:** Fable (direct, no bells — Joe 2026-07-10) · **Code:**
`futon3a/holes/labs/M-memes-arrows/obligation_accuracy.py` + `aliveness_v3_gate2.py`
**Question:** the v3 lexical accuracy limb scores known dischargers at median 0
(validity failure, TN-gflownets-fable-review). Can a directional,
unification-tiered accuracy fix validity without collapsing into relevance?

## The measure (locked before Task-B rerun; no further variant iteration at n=9)

accuracy = directional want-atom coverage, produces = salient(THEN + `! conclusion:`)
(= `parse()["action"]`, the pragmatic side alexandrian_aif already uses), matched at:
- **T1 exact** token; **T2 stem** (deterministic light stemmer);
- **T3 per-atom semantic** (MiniLM cosine ≥ τ=0.60, fixed a priori; τ=0.55 leaks —
  a negative gains an atom — so the a-priori choice is also the empirically safe one).
Missing flexiarg ⇒ record is MISSING, never 0 (kit-intake: `dsc/evidence-situated-log`
is referenced by closure-folds but exists nowhere in the library).

## Results on the 10 GROUND records (9 measurable)

| scope | success | v3 lexical (THEN) | locked v1.5 (action, T1+2+3) |
|---|---|---:|---:|
| kit-outbox | T | 0.17 | 0.17 |
| kit-intake | T | MISSING (was 0) | MISSING |
| kit-cadence | T | 0.00 | 0.10 (`matured←maturity` T2) |
| E-mission-head/head-sigil | T | 0.38 | 0.50 (`obligations←commitments` T3) |
| E-mission-head/verify-v0 | T | 0.43 | 0.71 (conclusion content) |
| E-mission-head/seeded-beliefs | T | 0.22 | 0.33 (`reading←read` T2) |
| wm-flight/turn-4 | T | 0.00 | 0.10 (`underlying` in conclusion) |
| aif2/inv-tripwire-mapping | T | 0.00 | 0.00 (see residual R1) |
| hypergraph-operator/argue | F | 0.00 | 0.11 (see residual R2) |
| hypergraph-operator/derive | F | 0.00 | 0.11 |

Anti-`1=1` control: still PASSES (substantive +0.703 > trivial −0.078 > bloated −0.100).

**Verdict: measure improved and locked; validity MOSTLY recovered; two honest
residuals, neither fixable at n=9.**
6/7 measurable positives now score > 0 (v3: 4/7); the fixes are auditable
per-atom (witnesses above); the anti-gaming property held at every step.

## Residuals (carried, not patched)

- **R1 — interface-underspecification (inv-tripwire-mapping).** The pattern
  `sidecar/typed-kolmogorov-arrows`' text ("Store arrows with mode, payload,
  scope, confidence, status") genuinely does not contain the discharge content
  ("map declared invariants to runtime detectors that fire") — the *application*
  supplied it. No interface-text measure can score this without becoming
  relevance. Correct fix is upstream: enrich the pattern's THEN (library
  authoring), not the measure. Class flagged so expanded-corpus evaluation can
  report its frequency.
- **R2 — single-atom topical leakage (±0.1 grain).** The cosine-artifact
  negatives gain one atom from the conclusion (`operator(s)`, plus Lean tactic
  `apply` ↔ English `applies` at T2). At atom grain this is indistinguishable
  from signal; variants tried and rejected with reasons: net-production
  (produces − consumes) deletes true discharge content because flexiarg IFs
  restate goals, and still leaks via morphology; tighter τ already at the safe
  point. Expected to wash out statistically at corpus scale — the acceptance
  test on the expanded ground truth (Task B) is AUC vs shuffle null, not
  per-record sign.

## Diagnosis artifacts

Per-record token-level miss analysis (why v3 scored known dischargers 0): miss
classes M1 missing-flexiarg / M2 morphology / M3 synonymy / M4 interface-size +
underspecification. Reproduce via `aliveness_v3_gate2.py` (witness lines).

## What this changes for the plan

Slice 2 remains gated. The expanded ground truth (Task B) is not merely
procedural: it is the statistical enabler that turns R2 from a tie-breaker
anxiety into an averaged-out nuisance, and the discrimination test (AUC vs
null at n≈60–100) is the reward's real acceptance bar, per the v3 spec.
