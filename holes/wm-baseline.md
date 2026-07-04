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
- **`:goal-outcome-mode :kl`** — LIVE since 2026-07-04 (D-1e flip, operator
  decision, M-aif-faithfulness §2.1; claude-4). `:becomes` entries score by the
  exact Bernoulli KL (`predictive-goal-outcome-risk-kl`, nats, T 0.1 unfitted —
  the 22b0024 structural-gap evidence); range entries keep the hinge inside the
  KL form. Resolves in both rank lanes via `arena-goal-outcome-mode`
  (`war_machine.clj`); library default in `compute-efe` stays `:hinge`.
  Known scale: ≈ ×9.6 the hinge on the live belly. Escape hatch:
  `FUTON_WM_GOAL_OUTCOME_MODE=hinge`. Mode stamped per ranked action as
  `:goal-outcome-mode` (trace-whitelisted at birth).
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
reproduces · `:precision-state` / `:policy-precision` (γ) · `:wm-version`
provenance stamp. Enactment flights also log to `p4ng/sequel-notebook.org`.

**`:wm-version` (B-0a) — LIVE since 2026-07-04 (claude-9).** Each scheduled-tick
record answers "which code, which config" with no human correlation step:
`{:git-sha :git-dirty? :risk-mode :ambiguity-mode :goal-outcome-mode
:kl-channel-weights :c-temperature :live-wire? :trace-schema-version}` — the
git identity of the one-shot JVM's checkout (`:git-dirty?` counts tracked
modifications only), the mode flags resolved via the SAME fns the rank lanes
call (`arena-mode-flags` in `war_machine.clj` — never a second env read), the
runner's live-wire switch, and the monotonic record-shape version
(`futon2.aif.trace/trace-schema-version`, now 2). Accessor:
`(futon2.aif.trace/wm-version-of record)`. Present-only: records written before
2026-07-04 (and bare `judge` calls) carry no stamp — for those, "which WM" is
still hand-correlation of timestamps against commit times. NOTE the known
attribution caveats inside the stamped era: the `cd0d25d` (:risk-mode) and D-1e
flips landed hours BEFORE B-0a; their boundary is recoverable via commit times +
the per-action `:risk-mode`/`:goal-outcome-mode` keys.

## Achievement readout

B-0c ledger **LANDED** (claude-10, 2026-07-04): `scripts/wm_achievement_ledger.bb`
→ `holes/labs/M-aif-faithfulness/wm-achievement-ledger.{edn,html}` (deterministic,
read-only; re-run at mission close = the before/after exhibit). Per-day AND
per-week census of ticks · decisions/modes · act-gate passes/fails (with missions)
· enactments + realized outcomes (expected vs realized ΔG) · γ samples + value
trajectory · channel-gap trajectory. Cascade placeholders excluded; suspend-gaps
(>2h) listed as machine-suspend, NOT failures.

**First readout (2026-05-18 → 2026-07-03, 41 files):** **682 ticks, all decided**;
**78 act-gates** (36 pass / 35 fail / 7 abstain); **36 enactments**, **35 numeric
realized outcomes** (mean |expected − realized| = 0.15 nats); **γ 1.00 → 1.27 over
34 samples**; modes multiplied 284 · hermit 220 · stop-the-line 152 · stagnant 22 ·
depositing 4; 30 suspend-gaps (not failures). (`:risk-mode`/`:wm-version` not yet
persisted per tick → date+mode segmentation; upgrades when B-0a lands.)

## Where the faithfulness ledger lives

Per-quantity badges: `data/r18-badges.edn`, rendered in
`holes/aif-wiring-explainer.html` (regen via `scripts/r18_badges_to_js.bb`).
Gap-by-node inventory + repair plan: `holes/M-aif-faithfulness.md` §1.1–§2.3.
