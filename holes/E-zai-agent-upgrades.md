# E-zai-agent-upgrades — parked upgrades for API-native agents

**Status: PARKED (written down 2026-07-04 so it isn't forgotten; not
opened). Owner: unassigned until a trigger fires. Parent: M-custom-harness
(follow-on); first likely trigger source: M-futon1b-port (zai-9's
long-lived driving session).**

## Origin

Operator question (2026-07-04, evening): do zai agents need warm pouches
like the claude agents? Answer, agreed: **no** — the pouch (M-kangaroo)
solves subprocess cold-start, and zai agents are API-native (HttpClient +
in-JVM conversation atom; no process to keep warm; turn startup is a
pooled HTTPS call). But the claude CLI+pouch bundle silently provides two
other services whose zai analogs are real gaps. "Upgrades that aren't
necessary right away, but we shouldn't forget them" (Joe).

## U1 — full transcript persistence (lossless resume)

**Gap:** a zai agent's conversation lives in the `!messages` atom inside
its invoke-fn closure; a JVM crash/restart destroys it. D-7 rehydration
(M-custom-harness §13.5d) now reconstructs *commitment summaries* from
the invoke-jobs ledger — good, but lossy by design.

**Upgrade:** persist the full `!messages` vector to disk each turn (e.g.
`/tmp/futon-zai-transcript-<agent>.edn` or under the repo's data dir);
on restore, load it instead of (or layered with) the rehydration summary.
Lossless resume, symmetric with claude's JSONL transcripts.

**Cost:** ~20 lines in `zai_api.clj` (spit after each turn's swap; slurp
in make-invoke-fn when present). **Trigger:** the next JVM restart that
costs a driving session real context — or before any deliberately
long-horizon driver after zai-9.

## U2 — context compaction / rolling window (the one that will bite)

**Gap:** `!messages` grows monotonically — verified 2026-07-04: every
turn conjes user/assistant/tool messages; tool results are truncated to
12 k chars each but never dropped; there is no compaction anywhere. Bell
executors never noticed (short sessions). A mission-driving agent on one
session (zai-9, M-futon1b-port) accumulates without bound → quality
degradation and cost growth long before the hard context ceiling.

**Upgrade:** rolling window — keep the system message (boot packet +
rehydration) + the last N turns verbatim; fold older turns into a
rehydration-style summary block maintained in the system message. The
D-7 machinery (`session-turns`/`rehydration-string`) is the pattern,
applied *within* a live session rather than across sessions. Optionally
recompute the summary via one cheap model call at fold time.

**Cost:** moderate (~60–100 lines + care about what must never be
folded: mission id, constraints, protocol). **Trigger — empirical, via
zai-9:** watch its session's message-count/token length as it drives;
build when the curve approaches either model-context limits or visible
answer-quality drift. zai-9 is the stress test the harness hasn't had.

## Secondary list — missing verbs (predicted, not yet felt)

Recorded by M-futon1b-port's IDENTIFY as gaps a *driving* agent will
surface; parked here so they accrue evidence in one place:
- clock-in/clock-out **write** tool (drivers are currently clocked by the
  reviewer via `clock-dispatch!`);
- a whistle (synchronous ask) tool — bells exist via `run_shell` only;
- a review-request convention (today it is prose in the bell prompt).

Each stays parked until a driving session actually stumbles on it; the
stumble is the evidence that prices the build.

## Non-goals

Warm pouches for zai (no subprocess to warm); any change to the 24-round
per-turn budget (that is B1 territory, tracked in M-custom-harness §13.5b
and the B2 flight-native follow-on).

## U2 trigger: FALSE ALARM, corrected same evening (claude-18's diagnosis)

What looked like the predicted context-length drift was NOT: zai-10's
Leg-2 transparency flight completed all research (fold applications, EFE
reflection, two findings) and then degenerated at the write step — five
consecutive parameter-less tool emissions ("I keep producing an empty
invoke") until round exhaustion. Session context at failure: system
prompt + rehydration + Leg 1 + a failed take + full Leg-2 research. The
new tool-error-feedback loop kept the turn alive but the retries were
identical, because the errors were generic. **Corrected root cause
(claude-18, same evening): max_tokens=4096 truncated the large tool-call
arguments in transit; finish_reason went unchecked; parse-arguments
silently mapped broken JSON to {}.** Fixed by claude-18: corrupted
arguments now return an explicit "do NOT retry identically, chunk the
write" tool error. The reviewer salvaged the leg from ledger narration
(p4ng/flights/flight-2026-07-04-zai10-leg2.org).

**U2's trigger has therefore NOT yet fired** — it returns to parked
status with its empirical trigger intact. Lesson banked: apparent model
degeneration at long context should be checked against transport-layer
truncation FIRST (max_tokens, finish_reason) before blaming context.
