# TN-War-Machine-Restart — operating the armed full loop

Audience: any agent (or operator) restarting the War Machine full loop after
2026-07-16, with no session context assumed. Canonical companions:
`holes/missions/M-wm-tripwires.md` (the interoception contract),
`p4ng/main-2026.tex` §Catalog R20 (the pattern + operational witness),
`futon2/src/futon2/aif/full_loop_runner.clj` (the machine),
`futon2/src/futon2/aif/tripwire.clj` (the nerves).

## 1. How to restart

**Preflight (2 minutes, do all of it):**
1. Pick an author and an ordinary-work reviewer from the live roster; confirm
   the standing repair reviewer (`codex-1`, Ground Control, unless explicitly
   overridden) is connected too:
   `GET http://localhost:7070/api/alpha/agents` — all required agents must be
   `idle` with `invoke-ready? true`. Author ≠ effective reviewer, always. Both
   species work in both roles (proven: claude-7 author / codex-6 reviewer
   grounded `bf14dca`).
2. **Never bell an agent and then immediately dispatch it work.** A busy
   agent at dispatch time yields a typed `:agent-unavailable` run (cheap, but
   wasted). If you send context/guidance by bell first, poll the roster until
   the recipient is idle before launching (the launcher-chain pattern below).
3. Check the board: no unexpected open stop-lines —
   ```
   cd /home/joe/code/futon2 && clojure -M -e "
   (require '[futon2.aif.repair-obligation :as repair])
   (clojure.pprint/pprint
     (mapv (juxt :repair/id :repair/class :repair/status)
           (repair/open-obligations)))"
   ```
   Open non-environmental lines PREEMPT ordinary selection (oldest first).
   `:environmental-hold` lines ride and auto-resolve on the next grounded
   change. `:awaiting-validation` lines ride and validate ONE per grounded
   run (deliberate `take 1` — do not "fix" this).

**Launch (one-shot opportunity, armed):**
```bash
systemd-run --user --unit wm-run-$(date +%s) --working-directory=/home/joe/code/futon2 \
  --collect bash -c 'clojure -M:wm-full-loop once \
    --author <author-agent> --reviewer <reviewer-agent> \
    --repair-reviewer codex-1 \
    --tripwire-action park-and-summon > /tmp/wm-run.log 2>&1'
```
- `--tripwire-action` ∈ `record` (shadow) | `stop-line` | `park-and-summon`
  (production default choice as of 2026-07-16; escalation is an explicit
  operator decision, compiled default is `record`).
- `--reviewer` reviews ordinary mission work. `--repair-reviewer` is the
  standing Ground Control review lane for every selected stop-line repair;
  its visible compiled default is `codex-1`. The machine fails closed before
  author dispatch if the effective reviewer is unavailable or equals the
  author.
- systemd-run, not a background shell: runs survive the launching session.
  `tick` instead of `once` is the cron-shaped trigger; no cron is currently
  installed (deliberate — see the paper's R10 gate discussion).
- Watch by polling `systemctl --user is-active <unit>`; durable evidence does
  not depend on the watcher.

## 2. What to expect

**Phase sequence** (all phases logged to
`data/wm-full-loop-phases.edn.log`, one EDN per line):
`stop-line-memory → selection → construction → author-dispatch → author-wait
→ build-resolution → reviewer-dispatch → reviewer-wait → grounding →
stop-line-resolution → opportunity end`.

**Durations observed:** selection 1–7 min (G-efe over the mission corpus —
the Agency looks idle during this; that is normal, dispatch is "just slow");
author turn 1–9 min; review 2–3 min; whole opportunity 4–15 min.

**Outcomes** (`:outcome` on the opportunity-end phase record):
- `:grounded-change` — author committed, reviewer approved
  (`FULL_LOOP_REVIEW:` first line-anchored marker wins), Futon1b grounded,
  one awaiting-validation line also production-validates.
- `:build-failed` — reviewer rejected; a typed `:independent-review-failure`
  finding now owns the line. Expect this often; it is the system working.
  Rejection texts have been consistently high-quality — read them.
- `:agent-unavailable`, `:dispatch-failed` — environmental holds; typed,
  non-preempting, self-resolving. Usually mean you raced an agent's mailbox.
- `:incomplete` with `:artifact-binding-mismatch` — the author narrated a sha
  it did not create in this window. The runner binds review to the
  REPO-OBSERVED HEAD across the author window (T13); authors must land a
  fresh commit and narrate that fresh sha. Recovery paths (re-presenting
  banked artifacts) are exempt.

**Trips** (the interoception): reports land append-only under
`data/wm-tripwires/trips/`; in `park-and-summon` a trip also opens a typed
`:invariant-tripped` finding, parks an investigation join, and bells the
summon recipient (`tripwire/summon-recipient`, currently `claude-6` — change
it if you are the operator's reviewer of record). Failures degrade DOWN the
ladder (summon→stop-line→record), never block the run. Expect silence: 8
runs produced exactly one trip (T7, a true reading under wrong precision,
since refined). If a trip fires: read the report, decide repair-the-machine
vs revise-the-wire (both precedented — see §4 of M-wm-tripwires), and
discharge via `record-implementation!` with independent-review evidence.

**Ledger discipline you must keep:**
- Out-of-band reviews are INVISIBLE to runner-constructed prompts. If you
  review something outside the loop, either ledger it on the line it
  concerns or relay it to the author by bell before the next run.
- Findings are append-only; discharge through the lattice
  (open→awaiting-validation→resolved | open→superseded), never delete.
- Deferred smalls get selected eventually: in a system that chooses its own
  work, every open finding is a future run. Keep the board clean.

## 3. Current state (as of 2026-07-16 evening)

Zero open preempting stop-lines. ~10 lines `:awaiting-validation` (the
attempt-006→016 lineage + trip + canaries), draining one per grounded run.
Wires T1–T11, T13 enabled; T12 chartered stub (zero-grounding target wedge —
enable after implementing its predicate). Calibration `clojure -M -m
futon2.aif.tripwire-calibration` must stay 5/5. Tests are hermetic by
construction (`697bb45`); if a test ever writes to
`data/wm-repair-obligations/`, that is a regression.

## 4. The interoceptive-γ link (S4) — chartered, not built

**The idea.** The tripwires give the machine interoception: observations of
its own trajectories. S4 closes the loop from noticing to *feeling*: a trip
should modulate the system's confidence in its own machinery. In AIF terms,
γ (the precision on policy selection — implemented as the selection gain
`g`, R14) is currently learned only from task-level evidence; S4 makes the
trip stream a second evidence source, about the machine rather than the
world.

**Concretely (design sketch):**
- Maintain a machine-confidence factor `γ_m ∈ (0,1]`, default 1. An
  UNDISCHARGED trip finding lowers it (e.g. `γ_m = base^k` for k open trip
  findings, base ≈ 0.5); discharge restores it. Wire-level learned precision
  (the demotion ladder, already half-built) handles chronic low-value wires;
  γ_m is the acute, global response.
- Consume it where confidence already acts: multiply into the selection gain
  before the softmax (lower γ_m → flatter policy distribution → more
  conservative, more evidence-hungry selection), and gate grounding on
  operator confirmation while `γ_m < threshold`.
- **The observable prediction (paper-grade):** post-trip behavior is
  measurably more deliberate until discharge — plot γ_m (or realized
  selection entropy) against time-since-last-undischarged-trip from the
  phase ledger. The Closing-the-Loop sequel wants exactly this figure: a
  precision parameter learned from the system's own interoceptive error
  signal, with visible behavioral consequence.
- Guards: the wire budget (M-wm-tripwires practical contract §4) caps
  self-monitoring cost; γ_m must not respond to `:record`-mode or
  test-root trips; and a trip during lowered-γ operation must not compound
  into paralysis (floor γ_m, or count only distinct wire-ids).

**Why it matters:** it is the last unbuilt edge in the organism metaphor the
paper now states — the hand acts (R16), the belly prefers (R19), the nerves
notice (R20); S4 is the noticing changing how boldly the hand moves. Build
it only after a few more armed runs accumulate real trip statistics: the
γ response should be calibrated against observed trip base rates, not
guessed.

## 5. Key commits (futon5a lineage + futon2 machinery)

| What | Commit |
|---|---|
| Generation-flip architecture (A2) | futon5a `099906e` |
| Reader-retention identity rule | futon5a `2b912f0` |
| Publisher mutex (grounded terminus) | futon5a `bf14dca` |
| Full-loop hardening (13→0 findings) | futon2 `cf7e538..be7364a` |
| Tripwires S1/S2/S3b | futon2 `3e9b601`, `41ff7b0`, `6d1a4f3` |
| T13 + repo-observed binding | futon2 `48dfa03` |
| T7 convergence exemption + roster fix | futon2 `859df12` |
| Test hermeticity | futon2 `697bb45` |
