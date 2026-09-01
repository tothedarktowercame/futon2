# C316 — preflight interval and fence-conditional readiness

Date: 2026-09-01

## Actual claim

`wm_preflight` combines process configuration/defaults with a dynamically read
deposit corpus to answer whether a mission can deliberate and act. That is a
readiness-over-an-interval claim, not merely a statement about file contents at
one instant. A deposit or arm can change while it runs.

The command now prints the observation interval and an explicit event claim.
With no fence it reports:

```clojure
{:claim :event-free
 :writer-fence {:status :absent :reason :not-declared}
 :event-free? :unverified
 :distinguishable-cause? false}
```

A technically ready mission is labelled
`READY-CONTENT-ONLY (event-free unverified)`, never unconditional `READY`.

The operator may declare the already-established quiet-window fence:

```text
clojure -M:wm-preflight --writer-fence FENCE_ID MISSION...
```

The output then names that exact ID and labels readiness
`READY (FENCE-CONDITIONAL FENCE_ID)`. The declaration does not acquire a lock;
its soundness is conditional on the operator actually holding the writer fence.
The condition is visible both in the label and structured observation.

## Five content declarations

The C315 single/pinned checks now emit `:content-current` explicitly:

- ablation exact dyadic;
- cascade diff;
- cleanup correction index;
- obligation-ledger reconciliation;
- R2 pinned snapshot.

Their inputs and verdict behaviour are unchanged. These declarations do not
repair or reinterpret the nine sampled hybrid cases.

## Focused verification

```text
wm-preflight-test: 2 tests, 8 assertions, green
unfenced diagnostic preflight: event-free unverified
declared-fence diagnostic preflight: event-free true, fence id visible
five content checks: positive exits 0 with :content-current visible
clj-kondo over touched scripts/checks/tests: 0 errors, 0 warnings
```

The 75 C315 candidates remain unexamined. The original low-risk triage is void;
this delivery does not restore or reuse that label.
