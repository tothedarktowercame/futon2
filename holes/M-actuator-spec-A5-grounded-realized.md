# Spec: A5 — ground the realized outcome in the substrate (build-match dial → γ)

**Owner/reviewer:** claude-4 (Codex builds, claude-4 reviews). **Extends:**
`futon2/src/futon2/aif/fold_realized.clj`; connects it to
`futon2/src/futon2/aif/actuator_a3.clj` (build-match). **Pays down:** R14 (γ fed a *real*
sample), R16 (the realized leg stops being a mirror).

## The tautology being replaced
`fold_realized/realized-outcome-of` sets (line 81):
```clojure
:realized-G (fe/coverage->delta-g (realized-coverage enacted-wiring))
```
`:realized-G` is coverage over the executor's **reproduction** of its own wiring — high even
when the world did not change. A5 grounds it: the realized outcome is the **actual substrate
dial** (build-match), so γ learns from what changed in the WORLD, not from reproduction fidelity.

## Design — a GROUNDED realized-outcome, apples-to-apples in endpoint-count units

1. **`realized-outcome-grounded [mission-id decision opts]`** (new, in fold_realized; requires
   `actuator-a3`):
   - **`:realized-G`** = `(- bound inhabited)` from `actuator-a3/box-match-snapshot` for the
     mission — the **remaining undischarged want-signature endpoints** (real substrate read;
     lower is better in FE convention; 0 = fully discharged). Grounded, NOT reproduction-coverage.
   - **`:expected-G`** = 0 for a discharge action (its target is *fully discharge*). A richer
     per-endpoint WM prediction is a follow-on — note it, don't build it.
   - **`:realized-source :substrate-dial`** — label it, distinct from the coverage path.
   - **SCALE-MATCH pin (do NOT violate):** both legs in endpoint-count units — never pair a
     substrate-dial realized-G with a coverage-ΔG expected-G (the mismatch that pinned γ before).
2. Thread through **`with-realized-outcome`** — the seam is UNCHANGED, still `*live-wire?*`-gated
   (γ holds at the prior until we flip it, so this does not disturb the current coverage feed).
3. `policy-precision` reads the grounded pair → γ. A partially-discharged mission yields a real,
   variance-≠-0 sample (M-learning-loop: realized-G 1 vs expected 0 ⇒ perf ≈ −1).
4. **Anti-tautology discipline preserved:** keep the "realized ≡ expected exactly ⇒ flag" leg.
   Now realized is grounded, so exact-equality means genuinely fully-discharged, not a mirror.

## Acceptance (tests)
- **M-learning-loop:** `realized-outcome-grounded` → `:realized-G 1` (bound 2, inhabited 1 via
  build-match), `:expected-G 0`, `:realized-source :substrate-dial` — a REAL grounded sample.
- A **fully-discharged** mission (all bound endpoints inhabited) → `:realized-G 0` → perf neutral.
- **The realized-G comes from the substrate dial, verifiably NOT from `realized-coverage`** — a
  test where reproduction-coverage ≠ substrate-dial confirms A5 reports the substrate number.
- FALSIFIER: `:realized-G` derived by re-running the fold / coverage ⇒ fail.

## Gates
clj-kondo clean; `futon4/dev/check-parens.el` clean; tests green. Reuse
`actuator-a3/box-match-snapshot`; stay `*live-wire?*`-gated; do NOT restart :7071. Bell
**claude-4** back with commit sha + the M-learning-loop grounded sample.
