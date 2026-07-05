# E-live-loop-2 — the steppable continuation (microsteps + gates)

**Status: OPEN (2026-07-05 morning, operator-directed). Driver:
claude-19 (continuing from E-live-loop-1). Reviewer: claude-16.
Operator gate: Joe — steps marked ⚠ARMED run only on his word.**

## Why steppable (the operator's design, 2026-07-05 07:35)

Rather than running in 3 big steps, run in MICROSTEPS: each has an
action, a machine-checkable GATE, and recorded evidence. A later step
may only be (re)run when ALL its prerequisite gates are green. Gates
are re-runnable at any time — they double as standing evidence checks
(mistakes-ledger discipline): a gate that was green and goes red is a
regression alarm, not an embarrassment.

**Apparatus:** step state lives in `e-live-loop-2-steps.edn` (same dir);
the stepper is `futon2/scripts/live_loop_step.py`:

    python3 futon2/scripts/live_loop_step.py status          # scoreboard
    python3 futon2/scripts/live_loop_step.py gate <id>       # run one gate
    python3 futon2/scripts/live_loop_step.py gates           # run all defined gates
    python3 futon2/scripts/live_loop_step.py runnable <id>   # may I perform this step's ACTION?

The stepper never performs actions (agents do, under the usual gates);
it enforces ordering, runs gate commands, and records pass/fail history
with timestamps. A step whose gate command is still `:manual` shows as
undefined (red) — honest by default.

## Microsteps

Prerequisite (imported from E-live-loop-1, already green):
- **S1** — sorry-grain measurement delivered + reviewed + ablation held.
  Gate: S1 table exists with ablation addendum.

### 2-series — the constructive path (decomposed S2)

> **Reviewer note (claude-16, 2026-07-05 morning), 2a–2c APPROVED with
> one re-pin:** independent gate re-runs all PASS; Flight-1 banner
> control on the modified constructor: rel and size unchanged, **F
> drifted +0.046 → +0.051**. Cause named: `base_rate_prior`'s K is the
> MEDIAN positive co-app mass — registering the seeds' phylogeny edges
> shifts the typical scale, so all priors move slightly. Benign and
> principled; the driver's "byte-identical" claim covered priors under
> a narrower condition than the full pipeline. **Baseline is re-pinned
> at F +0.051** for all future regression checks; lesson for the
> ledger: "identical" claims must name the exact pipeline stage they
> were checked at.

- **2a — seed embeddings.** Regenerate the MiniLM pattern-embeddings
  artifact so the three process-coherence patterns are present.
  Gate: all three pattern ids appear in
  `futon3a/resources/notions/minilm_pattern_embeddings.json`.
- **2b — phylogeny registration.** Register the seeds in
  `futon6/data/pattern-phylogeny-edges.json` (the eligibility gate at
  `cascade_construct.py:188-197` makes unregistered patterns
  display-only). Gate: all three ids in the phylogeny's patterns set.
- **2c — seed prior treatment.** Implement the chosen mechanism (prior
  floor / anchor slot per the S2 design recommendation) so seeds are not
  Occam-fined at the 2.398-nat unseen default. Gate: probe run shows a
  seed pattern retrievable with per-seed complexity below 2.0 nats
  (driver attaches the probe as the gate command).
- **2d — escrow record shape.** Land the `ft-*.edn` fold-turn record
  contract (authored tier of the sortie-11 triple schema) with
  reject-loudly loading. Gate: a deliberately malformed record is
  REJECTED loudly by the loader test.
- **2e — replay seam, built dark.** The `close_loop.clj` seam that
  consults escrowed fold-turns (exact prompt-sha match, stored ΔG
  re-asserted on load), dark behind a flag, default off. Gate: seam
  unit test replays a MOCK fold-turn and re-asserts ΔG; live path
  behavior unchanged with flag off (K2 assertions both sides).
- **2f ⚠ARMED — one real deposit.** One LLM-fold turn by an agent, on
  one S1 cascade (candidates: state-snapshot-witness/:s4 or
  autoclock-in), recorded to `futon6/data/fold-turns/` with Joe's
  verbatim arming word as a field. Gate: deposit loads cleanly;
  sha-match + ΔG re-assertion pass.
- **2g ⚠ARMED — live replay.** Flag on; the act-gate consumes the
  escrowed ΔG on the live path. Gate: a trace record shows an act-gate
  verdict whose ΔG leg came from the escrow (provenance field), and
  the mistakes-ledger §10 evidence slot gets its entry.

### 3-series — futility feedback (decomposed S3; needs ALL 2-series green)

- **3a — futility observable.** The per-lane 0-for-N counter emitted
  where agents and the operator can read it (ties to the wm-tick
  emitter upgrade, ledger §13). Gate: counter queryable via an agent
  tool; shows the true historical count.
- **3b — γ update rule.** The precision-update rule specified and
  SIMULATED against the 674-tick census (no live wiring). Gate: the
  simulation note shows γ trajectory under the historical record and
  under a synthetic paying-lane record — they must diverge sensibly.
- **3c — operator bulletin.** Lane-futility alerts wired to the
  operator-lane at nag level past threshold (stuck-means-signal at the
  WM grain, ledger §11's open half). Gate: a synthetic 0-for-N burst
  produces a bulletin entry in dry-run.

## 3-series log — futility feedback dry-run (runner: codex-1, 2026-07-05)

**3a DONE, gate PASS (converted :manual -> :cmd).** Agent-queryable
tool: `clojure -M scripts/wm_futility.clj summary` (or the gate
commands below) over `data/wm-trace/*.edn`; no new server process.
Lane key = selected action class plus selected target. A lane is only
credited when the selected target itself receives a `:pass` act-gate
verdict; unrelated passes in the same tick do not hide futility.

Historical hand-count at run time: 43 daily trace files, 733 selected
ticks, 11 lanes, all 11 at 0-for-N under strict target credit. Top rows:
`address-sorry/:sorry/pudding-g1-arrow-witness-binding` 0-for-306;
`learn-action-class` 0-for-141;
`address-sorry/:sorry/wm-aif-substrate-addressability` 0-for-108;
`advance-mission/M-first-flights` 0-for-51;
`open-mission/M-canon-fingerprint-store` 0-for-44.

**3b DONE, gate PASS (converted :manual -> :cmd).** The offline rule
uses R14's existing `policy-precision/fold-realized-outcome`: expected
and realized are both coverage-dG legs, so the signed performance ratio
is unchanged. For this simulation only, a selected lane with no
target-matched pass realizes coverage-dG `0.0` against the act-gate
threshold expectation `-0.25`; a paying synthetic lane realizes
`-0.5` against `-0.25`. No live gamma wiring was added.

Observed trajectories diverged as intended: historical strict 0-for-N
record, 733 samples -> gamma `0.5000000013862944`, mean-perf
`-0.9999999959999999`; synthetic paying lane, 12 samples -> gamma
`1.2599210495067352`, mean-perf `0.33333333288888894`.

**3c DONE, gate PASS (converted :manual -> :cmd).** Dry-run projection
emits operator-lane-shaped items with `:lane :nag`, `:level :nag`,
`:dry-run? true`, and the descriptive booleans needed by
E-wm-operator-lane's classifier. The gate uses a synthetic
`advance-mission/M-synthetic-stuck` 0-for-7 burst and confirms one nag
bulletin item. This is dry-run only; no live bulletin route or process
was wired.

## 2f log — the GOLDEN RUN deposit (runner: claude-20, 2026-07-05)

**2f DONE, gate PASS (converted :manual → :cmd).** Arming: Joe's verbatim
word in the step history and in the deposit itself. Scope honored: 2f
only; `*escrow-replay?*` and the live path untouched.

- **Candidate chosen: `autoclock-in`** over state-snapshot-witness/:s4.
  Reasons: (1) the S2 design's reviewer-approved recommendation — its
  A-next EDN is a sealed ground-truth wiring, so the deposit is scoreable
  against an answer key; (2) size-4 cascade = the full shown set, no
  truncation wart; (3) no eponymous-provenance caveat; (4) the
  partial-fit halo genuinely exercises the honesty clauses. **Blinding
  held:** the runner never opened the sealed wiring (only the
  `:provenance {:mission …}` line was grepped); ψ came from the S1 raw
  JSON. Scoring against the seal belongs to the reviewer — author ≠
  scorer.
- **Deposit:** `futon6/data/fold-turns/ft-autoclock-in-001.edn` (first
  file in the escrow). 4 boxes (durable clocked-on write at declared
  intent; degrade-to-RAM + :pending-durable reconcile; lineage-gap audit
  in reconstitute; teardown acceptance probe), 4 wires, terminal :b4,
  **5 policy-holes** (clock-out semantics; held-count join; enforcement
  policy; reconciliation cadence; numeric acceptance targets). Coverage
  4/9 ⇒ **ΔG = −4/9 ≈ −0.4444** (hand-derived = `coverage-delta-g`
  recomputed; hole/box ratio not tuned for a round number). Prompt-sha
  `5597da91…`; prose = verbatim flexiarg slurps, per-pattern shas in the
  record; the 2g caller must inject that prose-fn and the literal
  `{:mission … :psi …}` circumstance.
- **Reject-loudly demonstrated on tampered copies in /tmp** (never the
  real escrow): dropped hole ⇒ `delta-g-mismatch` (stored −4/9 vs
  recomputed −0.5); stripped arming ⇒ `missing-arming`. Both loud, both
  auditable; demo dir removed.
- **Gate 2f is now standing:** `scripts/gate_2f_deposit.clj` (kondo 0/0,
  check-parens OK) — zero rejections + prompt REBUILT from the record's
  own cascade+ψ+prose sha-matches the stored pin + replay returns the
  recorded answer. Full board re-run after: S1,2a–2f all PASS, no
  regressions.

**PAR (claude-20):** What worked: (1) reading the seam/loader *tests*
before authoring — they fixed the circumstance shape and prose-fn
question (pin 1 binds prose; the deposit must record the reproducible
prose rule or 2g can never replay it); (2) computing every recorded
number in-process via the real fns (nothing hand-transcribed), with the
hand derivation shown alongside — pin 3 then proves the arithmetic
instead of trusting it; (3) tamper demo in a /tmp copy — the real escrow
never held an invalid file. What to carry: the honest-fit tension is the
exemplar's point — a 0.36–0.42-rel halo forced real holes; five surfaced
holes and ΔG −4/9 is a *truthful* golden turn, where tuning to a rounder
−0.5 would have been the decorative-box failure the prompt warns
against. Open for the operator/reviewer: score the construction against
the sealed autoclock-in wiring; rule on 2g.

## 2g log — the seam powered, the first live read (runner: claude-20, 2026-07-05)

**2g DONE, gate PASS (converted :manual → :cmd). Full board green: the
2-series is COMPLETE; 3a/3b unblocked.** Arming: operator's word 07:49
("Yes both are fine, let's proceed with 002 & 2g"). After 675 rounds, a
plan counted.

- **The flag, made persistent by one mechanism:** `*escrow-replay?*` is
  a dynamic var (`close_loop.clj:32`); its source default is now TRUE
  (the operator's ruling made code). The serving JVM had never loaded
  the ns, so requiring it live via proof-eval loaded the edited source —
  live state = boot state, no alter-var-root divergence, nothing
  env-dependent at next boot. Safety of the default flip is *asserted*,
  not assumed: `(and *escrow-replay?* escrow-turn-fn …)` short-circuits
  on the nil turn-fn, so uninjected callers (today's scheduled runner)
  are byte-identical — the 2e seam test gained a flag-on-uninjected leg
  proving exactly that (7 tests / 30 assertions green; kondo 0/0;
  check-parens OK).
- **Witness chosen: direct lane-entry eval IN the serving JVM** (real
  loader + pins over the real escrow, real flexiarg prose, root flag —
  no `binding`), at the deposit grain (S1 ΔF +0.813, the deposit's
  4-pattern shown set). Why honest: a scheduled tick *cannot* reach the
  pinned seam this turn — `enact.clj:91` calls the 1-arity (never
  injects `:escrow-turn-fn`), and the live lane's ψ is banner-grain, so
  pin 1 would rightly abstain against any prompt the lane can currently
  build. That is the sha-binding working, not a failure. A
  `request-tick!` would have witnessed an abstention.
- **The read:** ΔG **−4/9** consumed with `:delta-G/source :fold-escrow`,
  verdict **:pass** (ΔF 0.813 > 0 ∧ ΔG < 0), all 5 policy-holes intact
  through replay; in-JVM K2 asserts (provenance + replayed=stored ΔG)
  held. Trace id `2026-07-05T07:56:19.107773396Z`, persisted via the
  real `write-trace!` into `data/wm-trace-escrow-witness/` — a dedicated
  dir ON PURPOSE: the WM reads γ-state back from its own daily trace,
  and a witness record with default `:policy-precision` in that file
  could reset learned γ at the next tick. WM-I4 held: read, not act —
  the verdict is a preview; nothing enacted.
- **Finding — there are TWO escrow seams.** `enact.clj` (scheduled path)
  carries an older `:llm-escrow` branch reading bare un-pinned wiring
  EDN from `futon2/data/fold-escrow/<mission>.edn` — no arming, no sha,
  no ΔG re-assert. It predates the 2d contract. Recommend deprecating it
  in favor of the pinned seam when the caller wiring lands; until then
  it is a consent-bypass-shaped hole (currently empty, but reachable).
- **Named follow-on (not this turn's scope):** wire the scheduled
  caller (`enact.clj` → 3-arity with `:escrow-turn-fn`/`:prose-fn` +
  deposit-grain circumstance) and/or lift the lane ψ to sorry grain —
  then ledger §10's falsifier ("post-S2 ticks still 0-for-N") gets its
  real test. Gate 2g stands meanwhile: flag-on-by-default + escrow
  clean + witness trace provenance, re-runnable forever.

**PAR (claude-20, 2g):** What worked: (1) auditing the *call path*
before touching anything (E-live-loop-1's own lesson) — it surfaced
both the uninjected 1-arity caller and the second, un-pinned escrow
seam; without that read the "obvious" move (flip flag, request tick)
would have witnessed either an abstention or the wrong seam. (2)
Choosing the witness by what could be defended as honest rather than
what looked most live — and saying plainly what the witness does NOT
show. (3) Checking the trace read-back path before writing to it — the
γ-reset hazard was invisible until `trace-record`'s `:policy-precision`
default met the read-back contract. What to carry: when a flag flip is
"the fix, not a demo", the fix is source + running-process + tests
*together*, and the never-loaded-ns trick (edit source, then first
require) is the cleanest way to keep them one mechanism.

## deposit-002 log — the enriched-ψ experiment (runner: claude-20, 2026-07-05)

**002 DEPOSITED and gate-verified (2f gate PASS over both deposits;
arming = the 07:49 blanket grant, cited verbatim in the record).**
Candidate: `live-geometric-stack` (F>0 regime, S1 cross-check row;
blinding held — only recipe fields regex-extracted, sealed wiring
unopened). Recipe v2 = S1 recipe + the `:hungry-for` value and its
contiguous comment gloss, recorded in the deposit's `:psi-recipe` field.
Artifacts: `p4ng/flights/e-live-loop-2-002-runner.py` + `-results.json`
(v1 and v2 run in the same process, same constructor call as S1).

**Result 1 — the recipe delta changed the cascade materially:**

| | v1 (S1 recipe) | v2 (+ hungry-for gloss) |
|---|---|---|
| bytes | 685 | 1150 |
| size | 9 | 16 |
| F | +1.623 | **+2.027** |
| acc / cx | 3.10 / 5.90 | 5.52 / 13.97 |

v1 reproduced S1's row (size 9, rel-max 0.481; F +1.461→+1.623 drift =
the known re-pinned-prior shift from seed registration). v2 kept 6 of
v1's 9 patterns, dropped 3 (incl. both `aif/*` observation patterns),
added 10 — and F ROSE despite 2.4× the complexity: the gloss buys
coverage that pays its Occam cost. Length confound noted (+465 bytes),
but S1 already showed byte count alone does not drive size (typed-bells
1444B → size 1).

**Result 2 — invariant-class content surfaced, at two grains with a
split verdict:** (a) *retrieval*: v2 pulled invariant-class patterns v1
never saw — `exotic/live-sync-source-truth`, `gauntlet/world-is-hypergraph`,
`invariant-coherence/shape-first-identify`, `stack-blocker-detection`
(failure-signatures-surface = the self-zapping sub-property) — BUT the
four most invariant-flavored arrivals sit at greedy ranks 10–16,
**outside the budget-6 fold window**. (b) *fold*: the gloss rides ψ into
the prompt regardless, and the authored turn boxes the sub-properties
directly (b1 self-zapping, b2 derived-not-stored, b3 debounce-liveness
audit, b4 cross-codebase adapters) with 2 of 6 holes naming ungrounded
invariant specifics (debounce window; geometric derivation contract).
So: the ψ-grain fix works; the budget-6 window is the next clip point.
Whether these match the SEALED holes is the reviewer's blind scoring,
as before. Deposit: `futon6/data/fold-turns/ft-live-geometric-stack-002.edn`
— 6 boxes, 5 wires, terminals [:b2 :b6], 6 holes, ΔG −0.5 (coverage
6/12; round by coincidence, recomputed by the loader), prompt-sha
`e1db629b…`. Seam and live path untouched, per scope.

**PAR (claude-20, 002):** What worked: running BOTH recipes in one
process made the delta attributable (and reproduced the S1 row as a
free cross-check); recording the recipe as a versioned field in the
deposit means the corpus now carries its own methodology history. What
surprised: the enrichment's strongest effect was at *retrieval* rank
order — the invariant patterns arrived but below the display budget;
prediction-testing at the fold grain would have silently failed without
noticing the window clip. Carry: when a recipe upgrade is tested,
check every downstream truncation point before scoring the prediction.

## Rules

1. Actions only when `runnable` says so (all prerequisite gates green).
2. Gates re-run freely; a green→red transition is a regression alarm —
   ledger entry before proceeding.
3. ⚠ARMED steps additionally need Joe's word, per-step, recorded in the
   step history.
4. Every action under the usual code gates (clj-kondo, check-parens,
   K2 assertions) and the mistakes-ledger gate from E-live-loop-1.

### 2f reviewer scoring (claude-16, blind comparison against the sealed EMPIRICAL + the 2026-07-01 SCORE.md baseline)

**Verdict: GOLDEN — approved as the reference exemplar.** Independent
gate re-run PASS. Scored against the seal claude-20 never opened:

- **Read-side codomain: RECOVERED** — b4's acceptance probe is
  reconstitution-surviving-teardown, the mission's actual point, which
  the 2026-07-01 cascade-only baseline *missed entirely*. The delta is
  S1's doing: sorry-grain ψ carries the want endpoint verbatim.
- **Endpoint grounding: STRONG** — b1 grounds on the real substrate
  (set-dispatch-mission!, clock/clocked-on, canonical nodes), vs the
  baseline's ~50% with canonical nodes missed. Again ψ-carried.
- **The sealed hard holes: MISSED, diagnostically.** Order-independent
  retraction (the Gu repair), canonical-identity, repl-sync — absent
  from boxes and holes alike. Two are code-level facts the seal itself
  says no prose can carry. But **single-active was in the empirical
  record's `:hungry-for` gloss and the mechanical ψ recipe DROPPED it**
  — a recipe defect, now named: *ψ recipes must carry the codomain
  qualifiers (`:hungry-for`), not just the signature.* Falsifiable
  prediction for deposit 002: with `:hungry-for` in ψ, the single-active
  invariant surfaces as a box or a hole.
- **b2 (degrade/reconcile): an invention** — engineering-sound, absent
  from the seal. Legitimate under the fold contract (a construction
  proposal, not a history recitation); flagged so 2g's rollout scoring
  treats it as the proposal it is.
- **The five surfaced holes are all real underdeterminations**; none is
  decorative. The honesty clauses held throughout; the deposit proves
  its own tamper-rejection; ΔG −4/9 re-derives by hand.

**Yield beyond the exemplar:** the golden run measurably demonstrates
the S1→2f improvement chain (reconstitution + endpoints recovered vs
the pre-S1 baseline) and produces the next recipe upgrade as a testable
claim. That is what a golden run is for.

## Operator ruling on consent economics (2026-07-05, verbatim in substance)

Both 2g and deposit 002 approved in one word. Standing policy for this
interactive-testing phase: **the planning gate has Joe's blanket
consent** — per-fold arming ceremony is retired for fold-turn AUTHORING
while testing continues interactively. Joe's design sketch for the
durable mechanism: a **mana economy** — the operator awards N mana to a
gate, pre-approving N runs (e.g. "5 mana = approve 5 fold-turns"),
consumed per use, topped up at his discretion. This connects to the
existing mana machinery (mana-snapshot service; mana = positive
aliveness in the aliveness synthesis) and is the designed successor to
one-word-per-act arming. Design note belongs to the consent-gate
lineage (WM-I4); build when the interactive phase ends. ENACTMENT of
plans (as opposed to authoring/reading them) remains held as before.

On plans going unread: "that's an obvious omission that needs to be
fixed" — 2g's flag-on is therefore the FIXED STATE going forward, not a
demo toggle.

## Finding-3 deposits — feeding the deposit economy (runner: zai-10, 2026-07-05)

**Assignment (operator-directed via claude-16): with classical fold off
as a dG route (operator ruling 2026-07-05), escrow deposits ARE the dG
supply. Author new fold-turn deposits under the blanket interactive
grant.**

**Escrow state after this run: four deposits, all loader-accepted, zero
rejections.**

| deposit | mission | boxes | holes | dG | prompt-sha | runner |
|---|---|---|---|---|---|---|
| ft-autoclock-in-001 | futon3c-d/mission/autoclock-in | 4 | 5 | -4/9 | `5597da91...` | claude-20 |
| ft-live-geometric-stack-002 | futon2-d/mission/live-geometric-stack | 6 | 6 | -0.5 | `e1db629b...` | claude-20 |
| ft-bayesian-structure-learning-003 | (bsl mission) | 15 | -- | -- | -- | claude-20 |
| **ft-aif-head-004** | **futon2-d/mission/aif-head** | **6** | **6** | **-0.5** | **`4fc9ceac...`** | **zai-10** |

### ft-aif-head-004 -- the aif-head deposit

**Candidate chosen: `aif-head`** (Mission Peripheral AIF head; Phase 1
COMPLETE, Phase 2 HELD). psi built via the L2 recipe (WANT = the
completion-criteria signature, HUNGRY-FOR = structure-learning, HAVE =
the actual AIF engine + cycle machine + structural law inventory +
evidence landscape). Cascade: size 21 (truncated at 20), F = +8.722,
acc 14.793, cx 24.284, H-coherence 0.93. Strong cascade -- the AIF/
agent/structural-law vocabulary is densely represented in the pattern
library.

**Fold:** folded the 6 highest-rel patterns from the budget-20 cascade
(`ants/baseline-cyber-ant` 0.679, `futon-theory/structural-tension-as-
observation` 0.613, `aif/candidate-pattern-action-space` 0.526, `aif/
structured-observation-vector` 0.502, `aif/belief-state-operational-
hypotheses` 0.501, `gauntlet/aif-as-environment-not-instruction`
0.565). The remaining 14 patterns are recorded in the cascade for a
future wider fold. 6 boxes, 6 wires, terminal :b6. **6 policy-holes:**
structure-learning mechanism; evidence landscape query interface;
refusal threshold calibration; default-mode trigger cadence;
compositional closure proof method (I6); Phase 2 peripheral adaptation
specifics. Coverage 6/12 => **dG = -0.5** (hand-derived = loader-
recomputed). Prompt-sha `4fc9ceac...`; per-pattern prose shas in the
record; prose-source = verbatim flexiarg slurps.

**Loader acceptance: PASS** -- all 4 deposits load, zero rejections.
**Tampered-copy rejection: PASS** -- dG changed from -0.5 to -0.9 in a
/tmp copy => REJECTED loudly with `:delta-g-mismatch` ("pin 3: stored
-0.9 vs recomputed -0.5"). Demo dir removed; real escrow untouched.

**Scoping note:** the fold window (6 patterns) is a deliberate scoping
choice, NOT a budget constraint -- the cascade budget has been 20 since
the operator's morning ruling. Folding fewer patterns against a rich
cascade keeps the construction honest (each box resolves a real HOWEVER
with concrete mission grounding) rather than thinning to cover the full
halo.

**No seal exists for aif-head -> no blind scoring**, per the 003
precedent. The construction is an honest proposal, not a scored answer.
