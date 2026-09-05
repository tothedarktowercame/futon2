# N — Process-trap recording conventions (WM ∘ APM)

Joint note. Drafted by claude-7 (APM), reviewed by claude-1 (WM), on Joe's
instruction of 2026-09-05 to align two campaigns that fell into the same
process traps independently.

Status: **binding on new records in both campaigns.** Existing records are not
retroactively invalid; they are reclassified under §3 when next touched.

---

## 0. Governing rule

> **A rung is only real where something refuses to advance without it.**

Everything below is subordinate to that sentence. It is Joe's fundamentals-gate
principle generalised, and it is the reason this note is a schema rather than
an essay.

The evidence for the rule is this note's own subject matter. APM has a
2,281-line Lean specification of its machine — `apm-lean/DarkTower/
APMCycleMachine.lean` — that is precise, theorem-backed, and was *not enough*.
Its acceptance checker runs only on the frame-close path, so 17 of 40 frames in
`jit-all-open-v2` were ever checked and none since 2026-09-02. Drift
accumulated in exactly the region where nothing refused to advance. A prose
conventions note with no checker will do worse than a Lean spec with a
jurisdictional gap, and the Lean spec already failed.

Corollary, binding: **a rung with no refusing checker is listed as
`aspirational` in the campaign's rung table.** Visible, not fake. §5.

---

## 1. Trap catalogue

Each trap is stated as a mechanism, then its receipts. Instances are cited so
the note's authority is its receipts, not its prose. `[APM]` instances were
verified by claude-7 against source; `[WM]` instances are claude-1's, spot-checked
by claude-7 where noted.

### T1 — Fake done via binary closure

*Mechanism.* A {0,1} status closes over a denominator that does not mean what
the accounting takes it to mean. The arithmetic is then honest and the total is
still false.

- `[WM]` Box-2 coverage reported "Free energies & scores: 0 gaps" while seven
  core EFE nouns had no constructor. `PredictiveOutcomeKernel` — the *type* of
  `Q(o|pi)` — is closed-with-witness while the F0 census finds `Q(o|pi)`
  uninhabited. Declarations written at the wrong quantifier close instantly.
  → `holes/labs/wm-contract/TRACE-box2-scores-accounting.md:33`
  (verified by claude-7, 2026-09-05)
- `[APM]` The open-problem selection rule is literally
  `:open :positive-status-sorry-count` — a problem is open iff its Lean file
  has sorries. Sorry-count *is* the completeness accounting.
- `[APM]` `b00J02` (frame f60) and `b01A02` (frame f61): zero sorries, treated
  as work product. Both discharge counting steps with `native_decide` — one and
  five `._native.native_decide.` axioms respectively. Sorry-free and not
  proofs. Found by running `#print axioms` by hand, not by the machine.
  → apm-lean `e3c24e76`
- `[APM]` `b96J03` is recorded `:closed` in the queue while master carries
  three sorries. Frame lifecycle status and proof state are independent, so
  "closed" never meant "proved".
- `[APM]` The queue's `:completed` list mixes `:closed` and `:partial` frames.
  "19 completed" is not 19 solved.

### T2 — Spec/plan drift by displacement, with no decision

*Mechanism.* An operative artifact is replaced by another without any actor
deciding to replace it. Archaeology finds momentum, seat deaths, and parked
gates — no decision.

- `[WM]` The commissioned spine-first dependency-ordered build (2026-08-30) was
  displaced by an automatable closure ledger. No actor-authored decision
  performed the replacement.
  → `holes/problems/DRIFT-2026-09-05-account.md:125`
- `[APM]` The Lean spec defines the failure vocabulary —
  `PhaseFailureClass {transport, evidence}` ×
  `PhaseFailureDisposition {delayedRetry, apparatusRepair}`, with
  `evidence → delayedRetry` proven invalid. The implementation instead grew a
  `park` sink across **thirteen** apparatus fault classes. No decision replaced
  the specified vocabulary.
- `[APM]` claude-7 then added a *fourth* vocabulary — `fault_taxonomy.clj` with
  `:integrity`/`:frame` — without reading the spec that already had two.
  Displacement committed by a well-intentioned repair, by the agent later
  writing this note.

**T2a — jurisdictional gap (the mechanism under T2).** Drift goes where the
checker is not. APM's checker had jurisdiction over success and none over
failure; failure handling is precisely where the hotfixes accumulated. When
diagnosing drift, first ask *which region no instrument covers* — that is
where to look, before asking who decided what.

### T3 — Built-but-not-wired invisibility

*Mechanism.* Machinery is inhabited but unconsumed. Binary accounting cannot
represent it, so it reads as present.

- `[WM]` Four confirmed instances, incl. the R14 dial and fixture-fed EIG.
  → `holes/labs/wm-contract/EPIC-run-era.md`
- `[APM]` `campaign_trace/emit!` and the Lean trace checker: the designated
  acceptance authority, wired only to the success path.
- `[APM]` `frame_park_decisions/-main`: its only caller was the campaign
  babysitter. When that mutation path was correctly removed from the watcher
  (futon3c `aa139dce`), the reconciliation became unwired. Caught only by
  cross-reading two packets that were each individually correct.
- `[APM]` `process_watchdog/default-notify!` shells out to `notify-send`,
  **which is not installed on the host**. A dead escalation path of unknown age.
- `[APM]` `apm-axiom-audit.service` runs an axiom sweep *beside* the machine and
  was never a criterion *inside* it. That is what permitted T1's b00J02.

### T4 — The spec-silence pair

Two opposite errors, both reading a specification's silence as content. Named
as a pair because catching one disposes people toward the other.

**T4a — drift into silence.** Unspecified behaviour accretes where the spec
says nothing (see T2).

**T4b — inference from absence.** Behaviour is *deleted*, or a prohibition
asserted, on the grounds that the spec does not mention it.

- `[APM]` claude-7 asserted "park is reachable from exactly one place" from a
  `grep`, and commissioned deleting twelve code paths. codex-9 refused: the two
  park theorems (`projection_repair_before_bound_cannot_park`,
  `exhausted_projection_repair_parks_for_claude_and_continues_queue`) are local
  to `projectionRepairExhaustionSuccessor` and prove nothing about other paths.
  A grep shows what a spec *contains*, never what it *forbids*.

**Binding rule.** *An absence claim carries its search, or it is a T4b
violation.* The recording form already exists on the WM side and is adopted
verbatim: U54's census records `cwd: NOT FOUND` with the exact searches
enumerated; `DRIFT-2026-09-05-account.md` marks links **not established** with
the search trail. "Not established" is a *finding*; "absent" without a search
is an inference.

### T5 — Reflexivity failure in a gate

*Mechanism.* An instrument enforcing property P does not itself satisfy P.

- `[APM]` The two theorems establishing the `native_decide` rejection gate were
  themselves proved `by native_decide` — the gate's correctness rested on the
  axiom it exists to forbid. Now `by decide`, depending on no axioms at all.
  → apm-lean `3f071199`
- `[WM]` The F6 readiness registry must itself be rung-bearing: its validator
  refuses advancement without evidence, and the validator's own tests are the
  P-on-itself check. (Open; contributed as a live instance to fix.)

**Binding rule.** A gate enforcing P must satisfy P, or record an exemption
with author, date and rationale.

### T6 — Recorded but not surfaced

*Mechanism.* A decision is recorded honestly, in a field designed for it, and
reaches no one. This is drift with an audit trail.

- `[APM]` Fourteen parked frames in `jit-all-open-v2` carried complete,
  well-formed decision records — and every one had `bell-required=false`. Joe
  learned that frames were being parked at all a week later, from claude-7,
  not from the machine. Nine were flagged `:decision/apparatus-repair-required?
  true` with a written `:decision/recommended-route`; none were executed.
- `[WM]` The "REPORTED, NOT REPAIRED" evidence-string culture records honestly
  and surfaces only through claude-1's narration.

### T7 — Detection wired to a single catastrophic response

*Mechanism.* Excellent detection, one response, and the response is maximal.

- `[APM]` All four campaign stops carrying a structured cause were
  `:integrity`; none were `:substrate`. `:regulator-failed` was hardcoded as
  integrity, so *any* tick returning `:ok false` durably disabled the campaign.
  Each individual detection was correct. The wiring was the defect, and it is
  why ~84 sessions of agents each fixed a real bug and none fixed the machine.

### T8 — Watcher that observes and defers

*Mechanism.* A watcher detects correctly, defers once, and then narrates.

- `[APM]` The campaign babysitter logged
  `REGULATOR NOT RUNNING WITH ACTIVE FRAME [f85]: deferring notification to the
  frame watchdog` every 20 seconds for **fourteen hours** — roughly 2,500 times
  — having belled once. Its cooldown would have permitted 42 further bells. A
  watcher that cannot escalate is worse than none, because it looks like
  supervision.

---

## 2. Rung grammar

A lane-independent core, plus campaign-specific evidence kinds. Rungs are
ordered and **none is skippable**. A rung is claimed only with its evidence
kind; fixtures are refused by negative control at `constructed`.

### 2.1 Core (lane-independent)

```
named → transcribed → witnessed → constructed → RECORDED → wired → validated → run-correlated
```

`RECORDED` is new in this note and sits *between* `constructed` and `wired`.
It is not a synonym for `wired`. It asserts that the accounting knows the
artifact exists.

**Why it is its own rung — receipts on both sides:**

- `[APM]` Four proofs — `a99J05`, `a99J06`, `aunk04`, `b90A03` — were complete,
  elaborating, kernel-clean and statement-faithful, and sat **unrecorded for a
  week** while master still showed 2, 1, 3 and 1 sorry. Their frames had parked
  on Guide schema faults. The mathematics was finished; the record never moved.
  → apm-lean `624e81d7`
- `[WM]` U60's attribution tension was minted into a committed *copy* while the
  curated ledger stayed ignorant. Artifact finished, accounting blind.

The gap between *done* and *the books know it is done* is where both campaigns
lost work, and no existing rung named it.

### 2.2 WM evidence kinds

```
named → type-transcribed → formula-transcribed → witnessed → constructed
      → RECORDED → wired → validated → run-correlated
```

### 2.3 APM evidence kinds (proofs)

```
stated → typechecked → sorry-free → kernel-clean → statement-faithful
       → RECORDED → consumed → prereg-matched
```

- **`kernel-clean`** — `#print axioms` on the target declaration is a **subset**
  of `{propext, Classical.choice, Quot.sound}`.

  *Subset, never equality.* Both APM gates originally compared for equality,
  which rejects proofs that are **strictly stronger** than the standard: one
  depending on `[propext, Quot.sound]` fails, and a fully constructive proof
  fails hardest, because Lean prints `does not depend on any axioms` with no
  list at all and the parser returned nil. Fixed in apm-lean `3f071199` /
  futon3c `d52f2047`. WM's axiom probes grep the same string and would have hit
  the same wall on partial lists.

  Related: an inconclusive probe is **not** clean. A parse yielding nothing now
  returns `:apm-proof-standard-observation-missing`.

- **`statement-faithful`** — delivered signatures compared against the
  commissioned statement. A prover can satisfy a *weaker* statement under the
  right identifier and clear every other rung. APM verified this by hand for
  the four recovered proofs; nothing in the machine checks it. WM's analogue:
  a worklist row can satisfy a weaker statement under the right id and clear
  review, checked by hand today. **The convention demands it mechanically:
  delivered-vs-commissioned signature comparison.** Currently `aspirational` in
  both campaigns.

---

## 3. Ancestry and surfacing fields (F5)

Every operative plan artifact pins:

| field | meaning |
|---|---|
| `ancestor/commissioning` | the commissioning artifact this descends from |
| `clauses/inherited` | clauses carried forward |
| `clauses/dropped` | dropped clauses, each with author, date, rationale |
| `deps/derived` | derived dependency list |
| `notify/required?` | whether this record must be surfaced |
| `notify/discharged-at` | the surfacing event id — **not** a timestamp alone |

A checker refuses a plan with no ancestor, or with a consumer ordered before
its producer.

**Recording is not surfacing.** Per T6, a record with `notify/required? true`
is **not filed** until `notify/discharged-at` names a real surfacing event.

- WM discharge channel: the morning bulletin (B1, the producer half of
  morning-brief). A not-done decision or dropped clause is not filed until it
  appears in a bulletin whose item id becomes its `notify/discharged-at`. B1
  exists precisely so that surfacing needs no bell storm.
- APM discharge channel: currently the campaign babysitter's escalation path
  (agent bell on bounded backoff, widening to operator speech). `aspirational`
  until park decisions actually set `bell-required` true.

**`:decision/not-done` is adopted across both campaigns**, exported from APM's
park decision records verbatim. It states what was *not* done and not
fabricated. The model text, from frame f54:

> "The frame is not voided or forgotten. It is disposed `:partial` with the
> valid Student receipt and exact `[:guide-candidates-invalid]` residual
> preserved. No Guide verdict or memory review was invented."

Note that this excellent field failed completely in practice — fourteen times —
because it was never surfaced. The field is necessary and not sufficient; §3's
`notify/*` pair is what makes it load-bearing.

---

## 4. Drift archaeology form

For post-incident accounts of T2/T4:

1. **Passive voice forbidden.** Every link names actor, commit, date.
2. **"Not established"** is the required phrasing where no actor is found —
   accompanied by the search trail that failed to find one.
3. **Absence claims carry their search** (T4b). Enumerate the exact searches.
4. **Name the uncovered region first** (T2a): which region had no instrument?
   That is where drift will be, and it is a faster question than "who decided".

---

## 5. Checker ownership

A rung is only real where something refuses to advance without it (§0). Each
campaign names its checkers and **what each refuses**. A rung with no refusing
checker is marked `aspirational`.

### WM

| checker | refuses |
|---|---|
| `worklist_check.bb` | board rows failing structural validity |
| F6 registry validator *(to be built)* | rung advancement without the rung's evidence kind |
| `run_era_ledger.bb` | deposits failing ledger form |
| `negative_controls.sh` | generator/checker integrity failures shown by planted defects |
| fixture-refusal at `constructed` | **aspirational** — lands with the F6 validator |

### APM

| checker | refuses |
|---|---|
| `DarkTower/APMCampaignTraceChecker.lean` | operational traces not refining the Lean-owned phase order |
| `bank_audit/proof-standard-for-source!` | closes whose axioms fall outside the standard; inconclusive probes |
| `scripts/apm_invariant_checker.bb` | reports JVM count, worktree ceiling, disk headroom, churn |
| statement-faithfulness | **aspirational** — no checker |
| park-decision surfacing | **aspirational** — no checker |

APM's trace checker carries a known jurisdictional gap (T2a): it runs only at
frame close. Extending it to failure outcomes is specified but not built; until
then, every failure-path rung in §2.3 is effectively `aspirational` regardless
of what this table says. Recorded here rather than quietly assumed.

---

## 6. What this note does not do

It does not make either campaign's records correct. It makes their *shape*
comparable and their gaps *visible*. Per §0, every row above marked
`aspirational` is a place where this note is currently prose — and prose is
what both campaigns already had.

---

*Drafted by claude-7, 2026-09-05, under the coding-handoff protocol
(`CLAUDE.md` §Coding-handoff): author ≠ reviewer. claude-1 reviews.*
