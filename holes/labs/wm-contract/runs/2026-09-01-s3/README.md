# Stage S3 — 2026-09-01 (RUN8): live τ = β

**This directory holds a REPLAY, not a 20-tick stage run.** RUN8's acceptance
asks for the wiring, for an unsolved tick to be a datum, and for "rank and argmax
movement against the S1 field" — the movement is measured by replaying the
recorded S1 / S1b / S2 fields under the two temperature laws, which is exact and
reproducible, where a fresh run would only add a third field. `ARMS.txt` is that
measurement; `run8_tau_arms.clj` produces it and its controls reproduce each
record's own selection at τ_live at delta 0.0 before any arm is reported.

The live path was exercised twice by `run8_s3_preflight.clj` — one control tick
on the default mode and one real tick with
`FUTON_WM_TAU_MODE=variational-beta-gamma` — 0 POSTs and 0 `.admintoken` reads
each, run lock taken and released. Under S3 that tick selected at
**τ = β = 1.0364669814843985**, `:tau-source :converged-posterior`, γ = 0.9648160702310347,
continuing the S2 carry. Full account: `../../C469-s3-live-tau.md`.

Reproduce:

    clojure -M:test holes/labs/wm-contract/run8_tau_arms.clj
    clojure -M:test holes/labs/wm-contract/run8_s3_preflight.clj
    FUTON_WM_TRACE_POLICY_DETAILS=1 FUTON_WM_FPI_DARK=1 FUTON_WM_BETA_DARK=1 \
      FUTON_WM_TAU_MODE=variational-beta-gamma \
      clojure -M:test holes/labs/wm-contract/run8_s3_preflight.clj
