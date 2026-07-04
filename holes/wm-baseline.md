# State of the War Machine — operational baseline (B-0b)

**What this is:** the short answer to "what is the WM *right now*" — what runs on
what clock, what a tick does, what's recorded, which mode flags are live. This is
M-aif-faithfulness deliverable B-0b (§2.0). **Maintenance contract (§2.4 gate):
every parcel that changes what a tick does MUST update this doc in the same
parcel.** A repair that changes behaviour without updating this doc fails review.

**Last verified:** 2026-07-04 (claude-12) — against crontab, syslog, logs, trace
files, and code, not recited from other docs.

## Clocks

| When | What | Log |
|---|---|---|
| Hourly, `0 * * * *` | scheduled judgement tick — `clojure -M:wm-scheduled` (`scripts/wm_scheduled_run.clj`), one-shot JVM, never the serving JVM | `logs/wm-scheduled.log` |
| Daily 04:00 UTC | outer loop `-M:wm-outer-loop` — R12 hyperparameter take-up | `logs/wm-outer-loop.log` |
| Daily 04:30 UTC | shared-corpus embedding refresh (futon3a `index_patterns.sh`) | `logs/shared-corpus-refresh.log` |

**Caveat — the WM sleeps when Dionysus sleeps.** Cron doesn't fire while the
machine is suspended; e.g. no ticks 2026-07-03 18:04 → 2026-07-04 08:00. A gap in
the trace is not a fault; distinguish suspend-gaps (syslog CRON lines stop
entirely) from tick failures (CRON fires, no trace/log line).

## What one tick does

Observe the 13–14 channel schema → belief update → K=3 rollout over candidate
policies → EFE ranking → softmax/abstain selection (γ-modulated) → act-gate over
the cascade lane (rollout → classical fold → escrowed LLM fold) → on first
`:pass`, artifact-only enactment via the deterministic executor
(`futon2.aif.enact`) → trace append. Cascade placeholder rows are appended AFTER
`wm-decision` (Car-3 seam) and never enter the selection pool — downstream stats
must exclude them (E6/census convention; `scripts/wm_ihtb2_check.clj` is the
tripwire).

## Live mode flags (as of 2026-07-04, post `cd0d25d`)

- **`:risk-mode :kl`** — LIVE since 2026-07-04 (flip `cd0d25d`, operator
  decision, M-evaluate-policies §15). Uniform channel weights (canonical joint-KL
  config), `default-c-temperature` 0.1. Escape hatch: `FUTON_WM_RISK_MODE=hinge`.
- **`:ambiguity-mode :gaussian-entropy`** — LIVE (D5c flip `8ae1090`, operator
  decision). Both flags resolve in both rank lanes via `arena-risk-mode` /
  `arena-ambiguity-mode` in `scripts/futon2/report/war_machine.clj`.
- **W1 `G-goal-outcome` KL** — flip IN FLIGHT (D-1e verdict, Joe 2026-07-04;
  claude-4 executing). Until that parcel lands, the non-KL predictive form is
  live; `predictive-goal-outcome-risk-kl` (`c_vector.clj`) is the dark twin.
- **Badges:** 7 `:principled-approximation` / 9 `:analogical` / 0
  `:derived-from-FEP` (`data/r18-badges.edn`). G-risk + G-ambiguity raise is
  PENDING live-tick evidence + C9 burn-in.

## What is recorded

Per-tick EDN appended to `data/wm-trace/wm-trace-YYYY-MM-DD.edn` (continuous
since 2026-05-18, modulo suspend-gaps). A record carries: `:tick` + `:timestamp`
· full observation vector (value/preferred/gap/in-range? per channel) · belief
before/after · `:free-energy` decomposition · `:ranked-actions` with ALL EFE
summands per candidate (post-`2d6533e` whitelist; `:risk-mode` and
`:score-provenance` stamped per action) · `:decision` + `:mode` ·
`:act-gate-verdicts` · `:enactment` audit · `:realized-outcome` when the executor
reproduces · `:precision-state` / `:policy-precision` (γ). Enactment flights also
log to `p4ng/sequel-notebook.org`.

**Not yet recorded:** `:wm-version` provenance stamp (git sha + dirty flag +
resolved flag set + trace schema version) — B-0a, IN FLIGHT (codex-1). Until it
lands, "which WM produced this tick" = correlate tick timestamps against futon2
commit times by hand.

## Achievement readout

B-0c ledger IN FLIGHT (claude-10): per-day/per-week census of ticks · decisions
vs. abstains · act-gate passes · enactments + realized outcomes · γ samples ·
channel-gap trajectories. Until it lands, the raw trace plus
`scripts/wm_trace_census.bb` is the only readout.

## Where the faithfulness ledger lives

Per-quantity badges: `data/r18-badges.edn`, rendered in
`holes/aif-wiring-explainer.html` (regen via `scripts/r18_badges_to_js.bb`).
Gap-by-node inventory + repair plan: `holes/M-aif-faithfulness.md` §1.1–§2.3.
