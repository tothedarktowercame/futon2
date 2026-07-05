# Mistakes & remedies ledger — running, falsifiable

**Purpose (operator-directed, 2026-07-04):** a running list of mistakes
and the remedies put in place, structured so each remedy can be CHECKED
FOR EVIDENCE — for it or against it. Add entries as mistakes occur;
update Evidence as it accumulates. A remedy with no evidence check is
just a hope. Gate for E-live-loop-1 S2+ and all further agent-driven
work: no step may regress a listed remedy.

Format per entry: **Mistake** (what happened, where recorded) /
**Remedy** (what changed, where) / **Evidence check** (what would show
it working — and what would falsify it) / **Status**.

---

## 1. Unbounded retrieval — the wedged search
- **Mistake:** tool-grep walked dotdirs/stores unbounded; an agent hung
  50 min slurping a 279 MB binary (M-custom-harness §14.1).
- **Remedy:** prune list + 2 MB file cap + 30 s budget + explicit
  truncation sentinel (real_backend.clj, committed 9257d9d).
- **Evidence check:** no tool turn wedged >5 min on search; sentinel
  string appears in ledgers when budget hit. Falsified by: any new
  wedge on a search tool.
- **Status:** remedy live; no recurrence since.

## 2. Orphaned process trees on timeout
- **Mistake:** run_shell timeout SIGKILLed only the outer bash; clojure→
  java trees survived invisibly at default heap (§14.2).
- **Remedy:** whole-tree kill, descendants first (9257d9d); futon1b
  :node aliases capped -Xmx1g/-XX:MaxDirectMemorySize=1g.
- **Evidence check:** `pgrep -a java` after any timed-out run shows only
  the serving JVM (zai-9 now runs this check unprompted — that habit IS
  the evidence). Falsified by: any stray java after a timeout.
- **Status:** remedy live + verified (grandchild reap test).

## 3. Pouch RSS runaway — both box deaths
- **Mistake:** warm pouches grew unbounded; kernel OOM killed 12 GB and
  16.4 GB claude processes; serving JVM died in both cascades (syslog
  17:15, 20:27; §14.2 corrected note).
- **Remedy:** per-pouch RSS ceiling (FUTON3C_KANGAROO_MAX_RSS_MB, 4 GB)
  swept before each acquisition; mid-turn never killed (d21c32d).
- **Evidence check:** `[kangaroo] … recycling` lines in dev console;
  syslog shows NO further `Killed process (2.1.201)` entries; healthy
  pouches stay ~300 MB. Falsified by: another kernel OOM of a pouch.
- **Status:** remedy live + verified (real eviction demonstrated).

## 4. "Announced is not sent" — sender side
- **Mistake:** zai-9 twice ended turns with the checkpoint bell/doc
  write announced but unsent (M-futon1b-port E-scoreboard).
- **Remedy:** harness prompt rule ("the bell is part of the checkpoint,
  not an epilogue"), persisted 4627de0; reviewer watches for it.
- **Evidence check:** checkpoint bells arrive AT checkpoints (zai-9's
  P2c bell and zai-10's Leg-3 bell already did). Falsified by: any turn
  whose final text announces an unsent action.
- **Status:** remedy live; 1 post-rule recurrence at round-exhaustion
  (see §12) — watch jointly.

## 5. Reply-vs-dispatch ambiguity — receiver side
- **Mistake:** zai-10 held for a bell that its dispatcher's reply
  already was; root cause was the dispatcher promising "separate bells"
  then delivering inline (transcript analysis §A.4).
- **Remedy:** prompt rule ("replies to your own bell are operative
  dispatches"); dispatcher-side rule (keep delivery-channel promises;
  say "this reply IS the dispatch") — both in the transcript analysis.
- **Evidence check:** zero holds-for-never-coming-bells since.
  Falsified by: any agent parked "holding" on a reply-carried dispatch.
- **Status:** remedy live both sides.

## 6. Tool exception kills the whole turn
- **Mistake:** a nil :path arg NPE'd through resolve-path and destroyed
  a 37-event turn mid-flight (Flight 1, attempt 1).
- **Remedy:** turn-loop catches every tool Throwable → "TOOL ERROR
  (turn continues)" fed back to the model; resolve-path nil guard
  (committed same night).
- **Evidence check:** ledgers show TOOL ERROR strings followed by
  corrected retries; zero jobs in state=failed from tool-arg exceptions.
  Falsified by: any turn death traceable to a tool exception.
- **Status:** remedy live + verified.

## 7. Transport truncation misread as model failure
- **Mistake:** max_tokens=4096 truncated large write_file args →
  parse-arguments silently mapped broken JSON to {} → five identical
  "empty invoke" retries; reviewer misdiagnosed as context degeneration
  (E-zai-agent-upgrades corrected note).
- **Remedy:** corrupted-args explicit error ("do NOT retry identically,
  chunk the write" — claude-18); max_tokens → 8192; chunk-the-write
  instruction in dispatches. LESSON: check transport (finish_reason,
  max_tokens) BEFORE blaming the model.
- **Evidence check:** zero parameter-less retry loops; corrupted-in-
  transit errors rare and followed by chunked writes (zai-10 Leg 3:
  3-pass chunked write succeeded). Falsified by: recurrence of the loop.
- **Status:** remedy live + live-verified (8192 pipe test OK). Residue
  open: finish_reason=length on PLAIN TEXT still unsurfaced.

## 8. Wrong-cause post-mortems
- **Mistake:** the orphan-JVM theory was asserted for the crashes before
  reading syslog; retracted same evening when the kernel OOM record
  named the true victims (§14.2 attribution correction).
- **Remedy:** post-mortem discipline: primary logs (syslog/journal)
  before mechanism-fitting; corrections written into the SAME documents
  that carried the wrong claim.
- **Evidence check:** every crash claim cites its log line. Falsified
  by: a post-mortem naming a cause without primary-source evidence.
- **Status:** discipline adopted; this ledger is part of it.

## 9. Stale status banners
- **Mistake:** M-first-flights' banner gated Phase B on a condition a
  ground-control ruling had revised IN THE SAME DOCUMENT; both revised
  prerequisites were satisfied elsewhere; the WM re-selected the mission
  ~674 ticks without noticing; humans nearly did the same.
- **Remedy:** pattern `process-coherence/status-refresh-before-work`
  (d433bbb): re-derive status from body + dependencies at every mission
  takeup; update the banner citing what was checked.
- **Evidence check:** status lines carry "checked against …" updates at
  takeup; time-from-armed-to-noticed shrinks from weeks to one takeup.
  Falsified by: another mission worked from a stale banner.
- **Status:** pattern in library; NOT yet retrievable by the cascade
  lane (see §11 wiring) — process obligation on drivers meanwhile.

## 10. Energy spent where documents already answer
- **Mistake:** the cascade lane burned 674 ticks of evaluation on ψ
  scraps whose questions the mission docs answered; three independent
  sufficient causes (wrong ψ grain; constructor unarmed; lane never
  reads the constructor) — E-live-loop-1 findings 1, 2, 6.
- **Remedy:** E-live-loop-1's S1 (sorry-grain ψ: CONFIRMED, ablation
  held), S2 design (escrow + replay seam), S3 pending; pattern
  `process-coherence/is-this-even-a-problem` as cascade seed.
- **Evidence check:** S1 table (12/12 rel up; F discriminates). Next:
  a replayed escrow ΔG actually consumed on the live path (S2), and a
  lane-γ design (S3). Falsified by: post-S2 ticks still 0-for-N with
  a fed escrow and live seam.
- **Evidence (2026-07-05, E-live-loop-2 2g, claude-20):** the escrow ΔG
  WAS consumed on the live path — the act-gate in the serving JVM read
  deposit `ft-autoclock-in-001` through the pinned seam: ΔG −4/9,
  `:delta-G/source :fold-escrow`, verdict `:pass` (ΔF +0.813). Trace id
  `2026-07-05T07:56:19.107773396Z` in
  `futon2/data/wm-trace-escrow-witness/wm-trace-2026-07-05.edn`; standing
  gate = `scripts/gate_2g_provenance.clj`; seam now ON by default
  (`close_loop.clj`). Honest scope: the witness was a direct lane-entry
  eval at the deposit grain — SCHEDULED ticks still cannot reach the
  pinned seam (enact.clj's 1-arity caller + banner-grain lane ψ), so
  the falsifier "post-S2 ticks still 0-for-N" stays live until the
  caller wiring + ψ-grain follow-on lands.
- **Status:** RESOLVED -- the falsifier "post-S2 ticks still 0-for-N" is false.
- **Evidence (2026-07-05, E-live-loop-3 L4, zai-2):** the falsifier
  "post-S2 ticks still 0-for-N" is RETIRED. A NATURAL cron-fired tick
  (11:00Z / 12:00 BST, zero human involvement) consumed escrow dG
  through the pinned seam on the scheduled path:
  `:delta-G/source :fold-escrow`, `:gate-delta-G -0.7` for
  M-bayesian-structure-learning, verdict `:pass` (dF +1.463). Trace in
  `data/wm-trace/wm-trace-2026-07-05.edn` (tick stamp
  2026-07-05T11:00:09Z). The first `:fold-escrow` in the daily trace's
  history (1 vs 10 legacy `:fold` entries). Standing gate =
  `scripts/gate_l4_natural_tick.clj`. The full chain: L1 (legacy seam
  cold) -> L2 (sorry-grain psi) -> L3 (scheduled caller injects pinned
  seam with deposit-grain circumstance) -> L4 (operator ruling
  "classical route off" -> escrow fills the dG leg on the cron tick).
  The 2g witness was a direct eval; this is the natural tick. The
  falsifier is false.


## 11. Futile repetition unnoticed — the keystone
- **Mistake:** nothing at any layer notices 0-for-N: the arena (674
  ticks), the model (5 identical retries), the harness (silent), the
  human as only alarm.
- **Remedy:** pattern `process-coherence/stuck-means-signal` (c48891b);
  MECHANICAL: repetition detector in the zai turn loop (this commit) —
  3 identical tool calls → warning injected into the tool result;
  5 → instruction to stop and bell the reviewer. PROPOSED, not built:
  WM per-lane 0-for-N counter → operator bulletin at nag level.
- **Evidence check:** detector lines appear in ledgers when loops start
  and the NEXT event is a changed approach or a reviewer bell — not a
  6th identical call. Falsified by: any ≥5-identical-call run that the
  harness let pass silently.
- **Status:** harness detector LIVE (this commit); WM counter pending.

## 12. Work held in ledgers, not documents
- **Mistake:** zai-10's Leg-2 research and claude-19's S2 design both
  ended turns with results in ledger narration but not in the doc —
  one was nearly lost to a transport defect.
- **Remedy:** dispatch rule: checkpoint EARLY in resume-turns; wind-down
  bells demand "write what you hold, partial-and-honest"; "ledger
  narration is not the record; the doc is."
- **Evidence check:** excursion docs updated in the SAME turn findings
  are made (claude-19's final turn did exactly this). Falsified by: a
  finding recoverable only from a ledger.
- **Status:** rule in active use; candidate for harness prompt if it
  recurs.

## 13. Provenance-stripping projections
- **Mistake:** twice in one day, a projection stripped the field an
  agent needed: proof-paths lacked session-id/kind (agents couldn't
  recognize their own PARs); wm-tick bodies unprojected (EFE terms
  agent-invisible — Flight 1 Finding 1).
- **Remedy:** proof-path bridge now carries :evidence/session-id +
  :discipline-kind (9257d9d). PENDING: the wm-tick emitter upgrade
  (evidence_emit.clj carries cascade verdicts + risk-mode + EFE terms)
  — triply motivated, not yet built.
- **Evidence check:** zai-9 recognized its own PARs (done); after the
  emitter upgrade, an agent tool-query must return EFE terms. Falsified
  by: another "the data exists but no reader can see it" discovery.
- **Status:** half done; emitter upgrade is the open half.

## 14. Generic prices masked armed work -- classical-route-off
- **Mistake:** the dG reconciliation order (rollout -> classical ->
  escrow -> nil) let the 10-entry classical rule table fill the dG leg
  BEFORE the sha-pinned, operator-armed, reviewer-verified escrow
  deposit could fire. The L4 tick exposed this: M-bayesian-structure-
  learning verdicted :pass with classical dG -0.077 while the deposit
  carried dG -0.7 -- a 9x underestimate. The gate was honest (it
  preferred the available source) but the richer construction was
  silently ignored. This was invisible until both sources could fire
  on the same mission (E-live-loop-3 L4, 2026-07-05).
- **Remedy:** operator ruling "turn off classical fold as a route"
  (2026-07-05). The classical fold's dG leg is unplugged from the live
  reconciliation via `*classical-fold-dG?*` (dynamic var, default
  false, REVERTIBLE). The new order is rollout -> pinned escrow ->
  abstain. The classical fold code and tests stay; only the live route
  closes. Close-loop source: `close_loop.clj`.
- **Evidence check:** `:delta-G/source :fold-escrow` on cron ticks for
  deposited missions; NO classical `:fold` sources in new traces for
  missions with matching deposits. Falsified by: a new trace showing
  `:fold` source for a deposited mission whose classical fold resolved
  (meaning the flag regressed or the order reverted).
- **Status:** remedy live (this commit); standing gate = L4 trace check
  (`scripts/gate_l4_natural_tick.clj`).
