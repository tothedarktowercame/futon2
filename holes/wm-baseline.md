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
| On demand (operator) | **regulated click campaign** — `scripts/wm_click_loop.sh [N] [settle]`: sequential full runs, bounded, stop-file `data/.wm-clicks-stop`, each stamped `:trigger :duree-click-regulated` (README-clicks-and-ticks; adopted 2026-07-04 for evidence acceleration) | `logs/wm-clicks.log` |

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
- **`:habit-prior-source :learned-frequency`** with
  **`:structural-pressure-mode :habit-prior`** — LIVE since Joe's 2026-07-13
  joint flip. A symmetric Dirichlet posterior over selected action identities
  supplies $\ln E(\pi)$ after structural pressure leaves `controller-score`;
  it replaces rather than stacks with the historical caller proxy. The first
  enabled tick seeds counts from the chronological trace corpus; later ticks
  persist sufficient statistics in `:habit-prior-state`. Historical rollback
  requires both `FUTON_WM_HABIT_PRIOR_SOURCE=caller` and
  `FUTON_WM_STRUCTURAL_PRESSURE_MODE=controller-augmentation`.
- **`:tau-mode :selection-gain-only`** — LIVE since Joe's 2026-07-13 flip.
  R6 no longer divides by the score-spread heuristic: $\tau_{eff}=1/g$.
  Selection gain remains an explicitly engineering feedback control, not
  variational policy precision. `FUTON_WM_TAU_MODE=spread` restores historical
  $\tau_{spread}/g$ for comparison.
- **Badges:** 8 `:principled-approximation` / 7 `:analogical` / **1
  `:derived-from-FEP`** (`data/r18-badges.edn`). Raised 2026-07-04 on
  live-tick evidence + the C9 first-pass census (reviewer claude-12):
  G-ambiguity → derived-from-FEP (exact gaussian-entropy, unscaled);
  G-risk → PA (true KL; ×urgency residue); G-goal-outcome → PA (Bernoulli
  KL for `:becomes`; hinge-in-mean residue). One-day-n caveat on record;
  C9 second pass (calendar days) strengthens or revisits.

## What is recorded

Per-tick EDN appended to `data/wm-trace/wm-trace-YYYY-MM-DD.edn` (continuous
since 2026-05-18, modulo suspend-gaps). A record carries: `:tick` + `:timestamp`
· full observation vector (value/preferred/gap/in-range? per channel) · belief
before/after · `:free-energy` decomposition · `:ranked-actions` with ALL EFE
summands per candidate (post-`2d6533e` whitelist; `:risk-mode` and
`:score-provenance` stamped per action) · `:decision` + `:mode` ·
`:act-gate-verdicts` · `:enactment` audit · `:realized-outcome` when the executor
reproduces · `:precision-state` / `:selection-gain` · reason-bearing
`:policy-support-exclusions` · `:wm-version` provenance stamp. Enactment flights
also log to `p4ng/sequel-notebook.org`.

**`:wm-version` (B-0a) — LIVE since 2026-07-04 (claude-9).** Each scheduled-tick
record answers "which code, which config" with no human correlation step:
`{:git-sha :git-dirty? :risk-mode :ambiguity-mode :goal-outcome-mode
:likelihood-mode :belief-model-manifest :tau-mode :structural-pressure-mode
:habit-prior-source
:predictability-control-mode :homeostatic-control-mode :graph-feasibility-mode
:kl-channel-weights :c-temperature :live-wire? :trace-schema-version}` — the
git identity of the one-shot JVM's checkout (`:git-dirty?` counts tracked
modifications only), the mode flags resolved via the SAME fns the rank lanes
call (`arena-mode-flags` in `war_machine.clj` — never a second env read), the
runner's live-wire switch, and the monotonic record-shape version
(`futon2.aif.trace/trace-schema-version`, now 12 — v10 records the three typed-
residual dispositions per ranked action and the policy-support exclusion trail,
v11 adds A/B/D likelihood provenance, and v12 adds present-only learned-habit
state/source provenance; earlier schema history is maintained in `trace.clj`). Accessor:
`(futon2.aif.trace/wm-version-of record)`. Present-only: records written before
2026-07-04 (and bare `judge` calls) carry no stamp — for those, "which WM" is
still hand-correlation of timestamps against commit times. NOTE the known
attribution caveats inside the stamped era: the `cd0d25d` (:risk-mode) and D-1e
flips landed hours BEFORE B-0a; their boundary is recoverable via commit times +
the per-action `:risk-mode`/`:goal-outcome-mode` keys.

**Trace hygiene (norm, 2026-07-04; refined same day):** the production trace is
the audit corpus. The `:wm-version` stamp now carries **`:trigger`** —
`:wallclock-cron` (tick; set in the crontab line), `:duree-click-regulated`
(sanctioned click-campaign run; DOES belong in the corpus), `:unspecified`
(untagged manual invocation). The norm: **verification/debug runs must NOT
write here** (pass `:dir` to `trace/write-trace!`); sanctioned evidence runs
must carry a `:duree-click-*` trigger. Consumers segment by `:trigger`;
pre-trigger records (before 2026-07-04 ~09:30) fall back to cadence +
`:live-wire?`. One known untagged manual record: 2026-07-04 07:09:54Z
(`:live-wire? false`, `:git-sha 515a5936…`; left in place).

## Achievement readout

**Evidence-epoch boundary (2026-07-13):** the B-0c ledger below is now a
**legacy historical census**, not the outer/full-loop evaluation dataset. Its
artifact enactments and mirrored outcomes are excluded from the fresh
`wm-outer-loop-40-v1` cohort. The preregistered protocol and empty new ledger
live at `holes/labs/M-aif-full-loop-40/{PREREGISTRATION.md,cohort.edn,ledger.html}`.
The cohort counts the first 40 natural wall-clock opportunities after explicit
activation, including abstentions, unavailable agents, failures and grounded
no-change; artifact-only output cannot count as grounded success.

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
