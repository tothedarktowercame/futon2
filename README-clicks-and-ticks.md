# Clicks And Ticks

*Futon2 is the home of the strategic AIF apparatus that consumes the
futon stack's temporal axes. This README frames the two clock sources
the apparatus listens to, why they're different in kind, and how they
compose into a single perceived-time substrate. Sibling document to
`~/code/futon3c/README-bells-and-whistles.md` (which frames futon3c's
agent-coordination surfaces in the same shape: same engine, different
contracts per surface).*

## What clicks and ticks are

| Clock | Source | Substrate | Cadence | What fires it |
|---|---|---|---|---|
| `tick` | wall-clock | cron / systemd-timer / Drawbridge | Configurable; default hourly | Calendar time passing — fires whether or not anything has happened in the stack |
| `click` | Evidence Landscape entries | authoritative Futon1b invoke-job ledger + sibling typed-event streams | Event-driven; varies | Operator-engagement events or an explicitly requested/continuous full-loop opportunity |

Ticks measure *calendar* time. Clicks measure *engagement* time. Both
are valid temporal substrates; neither alone is sufficient.

Pure wall-clock sampling dilutes signal — a tick fired while the
operator is asleep, with no observation changes since the previous
tick, contributes a zero-error row to R7's rolling-variance and
artificially deflates precision toward floor. Pure event-driven
sampling under-samples stable periods — if nothing happens for 12
hours, R7 has no way to distinguish "channel is stable" from "we
haven't checked." The two failure modes are dual; neither clock alone
escapes both.

## Two primary subclasses of click

A click measures *one unit of agent activity*. The activity is
usually-but-not-necessarily interactive. Two primary subclasses cover
how clicks arise in practice:

| Subclass | What it represents | Typical evidence sources | Agent-in-the-loop? |
|---|---|---|---|
| **`:turn` — interactive coding-agent turn** | One turn of an interactive coding-agent session (REPL, CLI, IDE). The current Joe + claude-4 emacs-repl exchange IS an instance — each Joe-prompt / claude-response pair is one turn. | Agency `:duree-click-invoke` ledger; futon3c forum posts; Drawbridge nREPL evals | Yes (human-in-the-loop per turn) |
| **autonomous click** | One step taken by an agent inhabiting a peripheral (per `~/code/futon3/docs/guides/README-peripherals.md`). The agent receives typed inputs from its capability envelope, decides, produces a step; no human-in-the-loop per cycle. | Peripheral runners (`fuclaude-peripheral.ts`, `fucodex-peripheral.ts`, `futon3.drawbridge.claude`); pattern-space-explorer ants; eventual M-INC `state/*` events | No (agent acts autonomously within the peripheral's capability envelope) |

`:turn` is a *subclass* of click, not a replacement for it. The
existing `:duree-click-*` taxonomy below tags clicks by their
evidence source (where the WM reads the click from); the `:turn`
vs autonomous framing tags what *kind* of agent activity produced the
click. Both axes are useful: evidence-source for substrate routing,
agent-activity-kind for operator comprehension.

### Interesting case: the WM itself as an inhabitable peripheral

Today the WM is an *apparatus* that runs scheduled (cron / Drawbridge
/ operator-manual) and DOES something with belief + EFE. The
peripheral contract (typed inputs, constrained tool envelope,
normalised outputs per `README-peripherals.md`) makes a richer
reframing available: **the WM could become a peripheral that an agent
inhabits.** An agent stepping into the WM-peripheral would receive
its current `:mu-pre`, `:observation`, `:anticipation`, and
`:ranked-actions`; choose an action (or abstain) under the WM's
EFE-scoring constraint; emit a `:decision` and updated `:mu-post`.

Each such step would be an autonomous click in this taxonomy. The
operator-visible distinction from today's WM is that the *judgement*
moves from inside the WM's clojure to inside an agent's reasoning,
while the substrate (trace records, belief domain, action types)
stays the same. The WM's R6 softmax + abstain becomes an
*envelope-constraint* the inhabiting agent honours rather than
something the WM's code computes directly.

This is forward-looking framing, not current implementation; the WM
today runs as-apparatus. But the click taxonomy is forward-compatible:
an inhabited-WM step would tag as `:duree-click-wm-peripheral-step`
(or similar) and route into the same R7 perceived-time substrate the
WM consumes.

## The two-clock convolution as perceived-time substrate

The honest move (per Joe 2026-05-18, two prior arts in this codebase
already implement the pattern) is to model perceived-time as the
**convolution of the two clocks**, not as two parallel state machines:

```
perceived-time-delta(from, to) =
  min(wall-clock-delta(from, to) / wall-window-seconds,
      click-count-delta(from, to) / click-window-count)
```

Whichever bound hits first ends the window; the score within the
window weights position-against-the-combined-window against the
strength of what's detected. This is the affect-transitions
shape ported up from
`~/code/futon1/apps/open-world-ingest/src/open_world_ingest/affect_transitions.clj`
(`lookahead-minutes ∧ lookahead-utterances`; link-strength score =
`0.6 × intent-confidence + 0.4 × proximity`).

Bergsonian *durée* operationalised: what matters is what happened
*within* the perceived window, not what happened in absolute time.
Pure cron sampling assumes time-as-uniform-substrate (a clock
ticking) — but the futon stack's value-bearing events are not
uniformly distributed in calendar time, so the precision signal that
falls out of pure-cron is misaligned with what an operator-coupled
agent should care about.

## How clicks and ticks compose with R7 (adaptive precision)

The futon WM's R7 implementation (`futon2/src/futon2/aif/precision.clj`,
v0.13) computes per-channel precision as `variance-component +
need-component`, where the variance-component is rolling-variance
over a bounded per-channel error history.

Under v0.13 the rolling window counts raw calls. Under the v0.14+ plan
in `futon2/docs/futon-aif-completeness.md` §R7 "Forward design", the
window will count *perceived-time-deltas*: older entries decay by
their perceived-time distance, not by their raw count. A
wall-clock-fired call that follows nothing counts less than a
click-fired call that follows real engagement.

Concretely:

```
For each trace record carrying :trigger and :perceived-time-delta:
  windowed-entry = {
    :error          <prediction error this record>
    :weight         (1 - perceived-time-distance-to-now), floored at 0.1
  }

rolling-variance = weighted-variance(window-entries)
precision        = max(precision-floor,
                       min(precision-cap,
                           1 / max(rolling-variance, min-variance) + need-component))
```

The `:trigger` field on each trace record names whether the call was
`:wallclock-cron` (a tick), `:duree-click-<kind>` (a click — kind tags
the source: `:repl`, `:mission-edit`, `:forum-post`, `:m-inc-state-*`,
`:operator-manual`), or `:operator-manual` (ad-hoc).

## Sources of clicks

These are the typed-event surfaces in the futon stack that can fire
clicks; they exist independently of the WM and are already
Evidence-Landscape-typed:

| Source | Substrate | Click kind | Status |
|---|---|---|---|
| Agency invoke-job ledger | Agency routing with Futon1b as authoritative substrate | `:duree-click-invoke` | live |
| Forum PSR/PUR/PAR posts (futon3c) | `futon3c/forum/*` | `:duree-click-forum-psr` etc. | partial; futon3c being ported |
| Mission-doc edits | `~/code/futon{0,7,...}/holes/missions/*.md` | `:duree-click-mission-edit` | file-watch detectable |
| M-INC `state/*` events | M-INC step (b) — not yet landed | `:duree-click-m-inc-state-*` | HEAD-as-escrow until step (b) commits |
| Operator on-demand full loop | `clojure -M:wm-full-loop once` | `:duree-click-on-demand` | live |
| Continuous full loop | `clojure -M:wm-full-loop continuous` | `:duree-click-continuous` | live |

The WM reads from these as-needed; it does not own any of them. Each
source has its own retention discipline and its own typing convention.

## Sources of ticks

| Source | Substrate | Cadence | Status |
|---|---|---|---|
| cron entry | `crontab -u joe` | Configurable | no authoritative full-loop cron installed; optional because the same runner supports durée mode |
| systemd user timer | `~/.config/systemd/user/wm-scheduled.timer` | OnCalendar configurable | optional substitute for cron |
| Drawbridge-scheduled invocation | futon3c nREPL | configurable | available if futon3c is up |

The authoritative entrypoint is `clojure -M:wm-full-loop`. `once` enacts one
on-demand durée click; `continuous` runs sequential durée opportunities (with
optional `--count` and `--interval-seconds`); `tick` enacts one wall-clock
opportunity. `clojure -M:wm-scheduled` is the same real full loop with `tick`
preselected. The former judgement/fold-only script is retained under the
explicitly non-actuating alias `:wm-judgement-only`; it is not a scheduler
entrypoint.

Each opportunity uses the same state machine: observe and update belief,
select exactly one policy, construct against that selected policy, dispatch a
configured author, require a verifiable substantive commit, dispatch a distinct
configured reviewer, ground the approved implementation and discharge in
Futon1b, queue Morning Brief QA, and close every cohort checkpoint. Defaults are
`zai-5` as author and `codex-7` as reviewer; `--author` and `--reviewer` or the
corresponding `FUTON_WM_*_AGENT` environment variables replace them.
The laptop authority defaults to Futon1b on `127.0.0.1:7073`;
`FUTON_SUBSTRATE_URL` (or `FUTON1B_URL`) overrides it on other hosts. Every
opportunity proves the semantic entity route before dispatching either agent.

Morning Brief reviews are entered with `clojure -M:wm-full-loop qa ...`. On the
next judgement, applicable QA events pass through the same declared A-matrix
belief update as other typed events. Reviews outside the current belief domain
are held and remain unconsumed rather than being silently dropped.

## The trace IS the state store

A discipline that emerged from the v0.12 R7 wiring and was reinforced
during 2026-05-19 bell-coordination with the VSATARCS side: **no
separate cross-call state file lives anywhere else; the trace is the
canonical store for all state that needs to persist between calls.**

Concretely:

| State carried across calls | Lives in trace as |
|---|---|
| Per-channel precision (R7) | `:precision-state` field on each trace record |
| Per-entity belief (R1) | `:mu-pre` + `:mu-post` fields on each trace record |
| Prediction errors per channel (R3a) | `:prediction-errors` field on each trace record |
| R3 micro-step convergence | `:micro-step-trace` field on each trace record |
| Anticipation snapshot (read-only) | `:anticipation` field on each trace record |
| Decision + ranked actions | `:decision` + `:ranked-actions` fields |
| Morning Brief QA fold | `:morning-brief-events`, held events, and consumed event ids |

On the VSATARCS side, the same discipline holds: the VSATARCS trace
stores `:wm-trace-anchor` entries identifying the last WM-trace record
already processed; `follow-wm` reads its own trace to discover where
to resume, no separate state file needed.

**Why this matters:** schema evolution becomes the only state-evolution
surface. Adding new derived state means adding a new field to
`trace-record` (per the `futon2.aif.trace/trace-record` shape — see
its docstring for the v0.16 schema). The trace file is the
*reconstructibility-from-disk* substrate that a cold-start agent can
read to recover full apparatus state. Per the
`feedback_reload_safety.md` memory entry, this is the discipline that
makes the WM safe to reload.

**Bilateral symmetry:** both AIF subsystems (WM-side and VSATARCS-side)
implement the same discipline. When the bridge compares trace records
across sides, all state-of-interest is in the trace — neither side
hides cross-call state in a sidecar file. This makes bilateral drift
signals interpretable: drift between sides reflects what their
respective traces show, not what either side privately knows.

## Implementation: the Arxana Clock + outstanding issues (E-arxana-clock)

The clicks/ticks framing is realised as the **Arxana Clock** — one read-only
surface over the system's time-drivers — in
`futon3c/holes/excursions/E-arxana-clock.md`. As of 2026-06-26:

- **Aggregator** `futon3c/scripts/arxana_clock.bb` — **scan-primary** (not a
  registry you can skip): scans ALL cron locations + user/system systemd timers
  + the JVM thread table, **reconciles** periodic threads against the
  `futon3c.cyder` registry, flags the residue as **⚠ UNACCOUNTED**, and prints a
  **coverage manifest** (every source scanned). Writes an EDN snapshot.
- **View** `futon4/dev/arxana-vsatarcs-clock.el` — the regular-Emacs Arxana
  surface (`M-x arxana-clock-browse`), sibling to the Arxana Ledger.
- **Turn-trigger** `futon3c/src/futon3c/clock/turn_trigger.clj` — the every-≈N-
  clicks driver (the `:turn` subclass above), polling the invoke-jobs ledger;
  its first rider keeps the serving-JVM C-vector (belly) fresh.

### WM Evidence Landscape emission

`scripts/wm_scheduled_run.clj` can also publish one compact `war-machine`
evidence entry per completed tick. It is disabled by default; enable it with:

```bash
FUTON2_WM_EMIT_EVIDENCE=1 FUTON3C_EVIDENCE_BASE=http://localhost:7070 \
  clojure -M scripts/wm_scheduled_run.clj
```

The POST is best-effort with a short timeout. The private EDN trace remains the
source of truth and is written regardless of Evidence Landscape availability.

**Outstanding issues (named here, deferred — tracked in E-arxana-clock):**

- **Completeness is bounded, not absolute.** The thread scan is ground-truth-
  complete for threads that *exist*, but cannot prove no hidden
  `(future (loop …))` will be *created* later; coverage is auditable (the
  manifest), not provable. UNACCOUNTED residue attribution is heuristic
  (token-overlap) — e.g. `FileSystemWatchService ×22` is flagged though it may
  belong to the multi-watcher.
- **cyder cadence/next-fire is mostly `—`.** Only the turn-trigger declares a
  cadence; the other in-JVM processes (watchers, schedulers) carry no cadence/
  next-fire metadata yet.
- **No stale-driver alarm.** A driver whose next-fire has passed without firing
  (the substrate-2 5-week-freeze failure mode) is not yet flagged — now
  tractable since the scan yields real next-fire times to compare against.
- **The turn-trigger is not auto-started on JVM boot** (operator action, like
  the cron install); and the snapshot EDN is a generated artifact (re-run the
  aggregator; don't trust a stale copy).

## What this README is NOT

- **Not a futon3c bells-and-whistles mirror.** Bells / whistles /
  whistle-streams are agent-coordination *transports* (request /
  response / progress). Clicks and ticks are temporal *substrates*
  (event-driven / calendar-driven sampling). Same depth of framing,
  different role in the stack.
- **Not the implementation.** The implementation lives at
  `futon2/src/futon2/aif/precision.clj` (today, v0.13 — raw-count window)
  and will be extended for perceived-time per the design above. The
  v0.14 anticipation-driven time-conditioning landing this roadmap
  delivery uses the SAME two-clock framing for forward-axis pressure
  (anticipated event deadlines act as time-anchored ticks).
- **Not a substitute for the M-INC event vocabulary.** When M-INC step
  (b) lands, M-INC `state/*` events become a first-class click source.
  Until then, the WM consumes operator-facing surfaces (invoke ledger,
  manual runs, mission-doc file mtimes) as click proxies.

## See also

- `~/code/futon2/docs/futon-aif-completeness.md` §R7 "Forward design:
  dual-clock perceived-time R7" — the design's home in the AIF contract.
- `~/code/futon2/docs/ants-aif-audit.md` — cyberants reference
  implementation audit; the ants' `:recent` window + `dhdt` derivative
  are the within-WM-scope analogues.
- `~/code/futon1/apps/open-world-ingest/src/open_world_ingest/affect_transitions.clj` —
  the original affect-transitions implementation with the literal
  convolution shape.
- `~/code/futon5a/README-anticipation.md` — forward-axis anticipation
  substrate; complements clicks-and-ticks (backward axis) with
  events-in-horizon (forward axis).
- `~/.claude/projects/-home-joe-code/memory/feedback_perceived_time_dual_clock.md` —
  the methodology memo captured 2026-05-18.
- `~/code/futon3c/README-bells-and-whistles.md` — sibling README at
  comparable scope on the agent-coordination side.
- `~/code/futon3c/holes/excursions/E-arxana-clock.md` — the Arxana Clock
  (the implementation of this framing) + its outstanding issues.
