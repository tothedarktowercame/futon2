# Stage S4 — 2026-09-01 (RUN9): F_π live in the policy posterior

Four ticks, one run lock (RUN12), each record carrying its `:run/id` (RUN11).
τ left at the default `:selection-gain-only`, so the only difference from S1 is
F_π: this stage adds one term and nothing else.

    FUTON_WM_TRACE_POLICY_DETAILS=1 FUTON_WM_FPI_DARK=1 FUTON_WM_FPI_POSTERIOR=1 \
      bash holes/labs/wm-contract/wm_run.sh 4 14 claude-20

`wm-trace-s4.edn` holds the four records, selected out of the shared per-date
trace file by `:run/id` (dd00675b, 73282cf8, 200b894f, 5bd37efe).

**F_π entered the live policy posterior on 3 of the 4 ticks.** The second tick
declined — 1 of 145 candidates had no matched previous prediction, and the
coverage rule is complete-or-off — and recorded `:absent :incomplete-coverage`
rather than imputing a value. That is the guard working, on a real run.

Reproduce:

    clojure -M:test holes/labs/wm-contract/run9_s4_arms.clj
    FUTON_WM_TRACE_POLICY_DETAILS=1 FUTON_WM_FPI_DARK=1 \
      clojure -M:test holes/labs/wm-contract/run9_s4_preflight.clj          # control, PASS
    FUTON_WM_TRACE_POLICY_DETAILS=1 FUTON_WM_FPI_DARK=1 FUTON_WM_FPI_POSTERIOR=1 \
      clojure -M:test holes/labs/wm-contract/run9_s4_preflight.clj          # see §3 of C470

The second pre-flight FAILS by design: it suppresses writes, so its "previous
record" is hours old, the candidate set has drifted, and complete-or-off refuses
an isolated tick. The stage run above is what a covered tick looks like.

`run9_s4_arms.clj` reads the CONTROL line before any arm: the replay reproduces
each record's own posterior at Δ 0.0e+00 — including the three that carry F_π,
which is what makes "the recorded S4 posterior is the F_π one" a measurement.

Result, S4 and the S2 counterfactual alike: TV 0.01202, **133 of 145 posterior
ranks move**, max move 130 positions, **argmax unchanged on every tick**.

Full account: `../../C470-s4-live-f-pi.md`.
