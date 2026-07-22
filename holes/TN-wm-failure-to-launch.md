# TN-wm-failure-to-launch

**Technical note — findings from the War Machine's first live full-loop click.**
Author: claude-6 (Ground Control). Date: 2026-07-18. Owner mission:
`~/code/futon7/holes/M-war-machine-aif-completion.md`. Intended for handoff to a
fixing agent.

## Why "failure to launch"

The first live click **succeeded mechanically** — it ran the full loop end to end
and grounded a real, reviewed change (`:grounded-change`, attempt-023, ~14.5 min).
But it **failed to launch on its intended payload**: the whole point of the
preceding work was to have the WM select a *mission* under the new three-factor
mission-value model. Instead it selected a `:learn-action-class` capability-gap
repair, and **the mission-value model never drove the pick.** The rocket fired and
reached orbit — just not the orbit we built it for. This note catalogs that gap
and the softer issues the run exposed, so the next agent can make a *subsequent*
click actually exercise the value model.

## What already works (do NOT redo these)

- **Substrate ported to Futon1b.** `futon3c` delta-t reader (`mission_delta_t.clj`,
  commit `c40f60d`) and `futon2` WM reads (`substrate.clj`/`war_machine.clj`,
  commit `b6e7091`) both read live 1b EDN. Verified.
- **Three-factor mission-value wired.** `enrich-candidates-with-mission-value`
  (commit `568c44d`) computes `value = (w_c·central + w_s·strategic + w_d·doable)
  × completion-gate × non-progress-decay`, cascade + weights configurable. Verified
  *in isolation* (`/tmp/wm-policy-proto3.clj`, `/tmp/wm-3f-verify.clj`): distinct
  factors, distinct G, merged↔stars policy swap reorders. See design memory
  `project_wm_mission_value_three_factor`.
- **Runtime**: the loop MUST run in the futon3c **serving JVM** (or a JVM with both
  futon2 + futon3c on the classpath). The bare `clojure -M:wm-full-loop` CLI is a
  futon2-only JVM that cannot resolve delta-t — a rogue entrypoint (retiring it is a
  separate tracked item). The first click ran via `bg.py` with futon3c added:
  `/tmp/wm-first-click.sh`.

## Repro artifacts

- Grounded commit under review: **`fc88293`** ("Make fire-pattern retrieval
  substrate live-addressable"), `pattern_registry.clj` +29/−8 + tests.
- Trace decision: `futon2/data/wm-trace/wm-trace-2026-07-17.edn`
  (`:decision` = `:learn-action-class / :fire-pattern`).
- Morning Brief batch: `first-live-2026-07-18`; render with
  `clojure -M:wm-full-loop feature attempt-023`.
- Reference prototypes: `/tmp/wm-policy-proto3.clj`, `/tmp/wm-3f-verify.clj`.

---

## Findings (priority order)

### F1 — [CORE] The selector chose a capability-gap over every mission; the mission-value model never drove the pick

**Symptom.** attempt-023's decision was
`{:type :learn-action-class, :target-class :fire-pattern,
  :rationale "no addressable entities for :fire-pattern in current substrate",
  :controller-score 32.84, :rank 109, :habit-prior-applied? true}`.
The three-factor mission candidates (e.g. `M-expressions-of-interest`, value 0.786)
were in the ranked set but did not win.

**Nuance (don't over-correct).** This was *partly sensible*: `:fire-pattern` was
genuinely broken (see F3 root cause — an IPv6-loopback bug made all pattern
retrieval return empty), so the WM triaging "repair my broken capability before
using it" is defensible. The problem is we cannot yet tell whether the selector
chose wisely or by accident, and we have **no evidence the mission-value model can
win a selection** when nothing is broken.

**Sub-issue — scoring opacity.** A `:rank 109` action was *selected* (not rank 1).
The interaction of `controller-score`, the **habit prior** (`:habit-prior-applied?
true`), `selection-gain`, and the softmax/τ produced a non-argmin pick. This is the
same habit-prior surface that caused the original flat-G dark-room behavior. It must
be made **legible**: for the chosen action, log/emit why it beat the top
mission-value candidate (the per-term contributions, the habit-prior multiplier,
the τ-spread).

**Where.** `scripts/futon2/report/war_machine.clj` — the controller-score
assembly, `apply-anamnesis-tiebreak`, the habit-prior application, and how
`:learn-action-class` / capability-gap actions are scored relative to
`:advance-mission` (they are scored on a *different* axis — intrinsic-value +
habit prior — that the three-factor mission value does not touch).

**Acceptance.**
1. A written explanation (in-code or a short note) of why rank-109
   `:learn-action-class` beat the top mission — reproducible from the trace.
2. A live click (serving JVM) in which, **with no broken capability**, the selector
   picks a *mission* and the trace shows the three-factor value governing the pick
   (its `:central/:strategic/:doable/:mission-value-factor` on the winning action).
3. Capability-gap actions and mission actions compared on a **commensurable** scale
   (or an explicit, documented policy for when a gap-repair preempts a mission —
   analogous to the cascade's livelihood-preemption rule).

### F2 — The reviewer gate ran read-only (no gates executed)

**Symptom.** claude-8's review record: `executed=false, tool-events=0`, yet
`approved=true`. It read the diff and left a correct, substantive note (see F3) but
there is no evidence it ran clj-kondo / check-parens / the tests.

**Why it matters.** Author≠reviewer separation is only a real gate if the reviewer
*exercises* the change, not just reads it. A read-only approve can pass a change
that fails the very gates the author was required to clear.

**Where.** The reviewer dispatch + contract in
`src/futon2/aif/full_loop_runner.clj` (reviewer prompt) and wherever
`executed?`/`tool-events` are recorded from the reviewer job.

**Acceptance.** The reviewer contract requires running the repo gates (clj-kondo,
check-parens, the relevant tests) and reporting their results; a review with
`executed=false` on a code change is surfaced as a weak/failed gate rather than a
silent approve.

### F3 — The grounded fix (`fc88293`) has a config blast-radius beyond its task

**Symptom.** To fix `:fire-pattern` addressability, `fc88293` correctly switched
retrieval to IPv4 `127.0.0.1` (the http-kit listener is IPv4-bound; `localhost`→`::1`
made the substrate appear empty). But it also **inverted the env precedence**:
`configured-evidence-base` now resolves `FUTON3C_SERVER` > `FUTON3C_PORT` >
`FUTON3C_EVIDENCE_BASE`, whereas the prior order (and the sibling files
`evidence_emit.clj`, `scripts/futon2/report/war_machine.clj`) put
`FUTON3C_EVIDENCE_BASE` first. claude-8 flagged exactly this in review.

**Why it matters.** A deployment that sets `FUTON3C_EVIDENCE_BASE` expecting it to
win now silently routes elsewhere; and the WM just modified its *own* retrieval
path, so a wrong precedence call degrades its own senses. Same fault-class as the
bug it fixed.

**Where.** `src/futon2/aif/pattern_registry.clj` (`configured-evidence-base`),
`evidence_emit.clj`, `scripts/futon2/report/war_machine.clj` (`futon3c-url`).

**Acceptance.** One resolution order for the evidence base, shared/consistent across
all three call sites, with the IPv4-loopback fix preserved. A test pinning the
precedence.

### F4 — The feature-card contract is asked-for but not enforced

**Symptom.** The QA sheet for attempt-023 shows section 7/8 "build-time feature card
pending" — codex-8 did not emit a `FULL_LOOP_FEATURE_CARD` marker, and nothing
rejected the deliverable for its absence. (Phase 2, commit `7f9e53f`, *asks* the
author for the card but treats it as best-effort.)

**Why it matters.** The feature-acceptance QA surface only self-populates if the
builder's card is reliably present; "asked but not enforced" is the same
"announced-is-not-sent" failure the project has hit before. Without it, every
grounded click needs the reviewer/operator to reconstruct the feature story by hand.

**Where.** `src/futon2/aif/full_loop_runner.clj` — the author deliverable
validation (it already hard-requires `FULL_LOOP_AUTHOR: DONE <sha>`; the card should
get parallel treatment for grounded builds).

**Acceptance.** A grounded build with no valid feature card is either rejected back
to the author (retry) or flagged as an incomplete deliverable — not silently
grounded with a "pending" sheet.

### F5 — Selection is slow, and ~30s/click is pure waste

**Symptom.** The `:selection` phase took **262s (~4.4 min)**. Of that, four
delta-t **edge-family** fetches (`related-mission`, `mission-cross-ref`,
`file→mission`, `sorry-doc`) each hit their timeout (~6–8s) on the cold cache —
and their ΔT output is **not consumed** by the WM (the mission-value model uses only
`:mission-T` from the `mission-doc` family).

**Where.** `futon3c/src/futon3c/aif/mission_delta_t.clj` (`fetch-types` /
`fetch-hyperedges-by-endpoint`) and the WM's `compute-delta-t-mission` call path.

**Acceptance.** The WM's tension read fetches only what it consumes (mission phase
from `mission-doc`); the unused edge families are not fetched on the WM hot path
(keep them available for delta-t's own ΔT consumers if any). Selection time drops
accordingly. Bonus: profile the remaining >200s judge cost.

---

## Related / context (not this note's asks, but the fixer should know)

- **Pre-existing test isolation bug.** The full futon2 suite has ~26 failures/13
  errors because an earlier test terminates a shared executor →
  `RejectedExecutionException` in later full-loop/tripwire tests. Required nses pass
  in a fresh JVM. Flagged in two prior reviews; a separate cleanup.
- **Rogue CLI.** `clojure -M:wm-full-loop` spins a futon2-only JVM that cannot reach
  delta-t; it must be retired or made to run in the serving JVM. Tracked separately.
- **Minor display bug.** The feature sheet renders "rank 109 of 10" (rank is across
  all action candidates; "of 10" is the mission count) — inconsistent denominator.

## Suggested order

F1 is the reason for the note (the model must be shown to govern a real pick) and
subsumes the scoring-legibility work. F2/F4 harden the loop's gates. F3 is a small,
bounded correctness fix. F5 is a cheap perf win. F1 is the only one that requires a
fresh live click in the serving JVM to close.
