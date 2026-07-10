# Supervised Flight — 2026-07-06 (first supervised run of the ready-state pipeline)

Pilot: zai-13 (fresh lane). Ground control: claude-16. Operator: live follow-mode.
Cards page: `futon2/holes/flight-pipeline-cards-iv.html` (ready-state ledger).
Manual: `futon3c/holes/GROUND-CONTROL.md`.
Consent budget: 2 mana on fold-authoring (consume 1 FIRST if/when a card-5 fold is needed).

---

## attempt 2 (zai-13, fresh lane)

### LEG 1 — T-0 PREFLIGHT + CARDS 1-2

Timestamp: 2026-07-06, evidence gathered this session.

#### (i) Instrument check — df -h + write/remove probes

All three paths on the same partition:

| Path | Filesystem | Size | Used | Avail | Use% |
|---|---|---|---|---|---|
| `futon2/data/wm-trace/` | `/dev/nvme0n1p2` | 938G | 654G | 236G | 74% |
| `futon6/data/` | `/dev/nvme0n1p2` | 938G | 654G | 236G | 74% |
| `storage/futon1a/default/doc-store/` | `/dev/nvme0n1p2` | 938G | 654G | 236G | 74% |

Write/remove probes: all three WRITE-OK + REMOVE-OK.

#### (ii) Gamma sample tail — `test/fold_realized_zero_coverage_test.clj`

```
ALL TESTS PASS
```

Key evidence: realized-G is 0.0 for the zero-coverage executor (legal). Three deposits sampled with expected + realized both recorded:
- `ft-aif-faithfulness-001`: expectedG=-0.556 realizedG=0.0 holes=5 signed-error=0.556
- `ft-evaluate-policies-009`: expectedG=-0.615 realizedG=0.0 holes=12 signed-error=0.615
- `ft-legacy-sorry-cleanup-001`: expectedG=-0.571 realizedG=0.0 holes=10 signed-error=0.571

Anti-fake-calibration guard passes (realized-G 0.0 ≠ expected-G -1.0).

#### (iii) Escrow standing

- `load-deposits` over `futon6/data/fold-turns/`: **13 accepted, 0 rejected** (from regression check 3a).
- Gate 2f: **PASS** (`bb scripts/live_loop_step.bb gate 2f` → `[2f] gate PASS`).

#### (iv) Baseline bundle — reference regression + commit SHAs

`clojure -M scripts/reference_regression.clj` — 8 checks, **7 pass, 1 DRIFT**:

| Check | Result | Detail |
|---|---|---|
| 1a: constructor peradam cascade | PASS | size=11 F=2.227 top=aif/no-self-certification rel=0.521 |
| 1b: constructor aif-head cascade | PASS | size=21 F=8.862 top=ants/baseline-cyber-ant rel=0.679 trunc=true |
| 2: fold-pin prompt-sha (every deposit) | PASS | 13/13 deposits reconstruct byte-exact |
| 3a: escrow load-deposits all accepted | PASS | 13 deposits, 0 rejected |
| 3b: escrow synthetic tamper rejected | PASS | delta-g-mismatch |
| 4: kill-test p3_retry_enriched verdict | PASS | OVERALL=fail A(R1)=fail A(R2)=fail B(R1)=pass B(R2)=pass |
| 5: mana award/consume/refuse round-trip | PASS | award->3, consume->2/1/0, refuse@0 |
| 6: peradam refusal census | **DRIFT** | 13 deposits (ref frozen at 8); causes: 001/002 unstructured-witnesses, 003-008 missing-seal |

Check 8 DRIFT is the **KNOWN deposit-count-sensitive** peradam refusal-census check (manual §2: "the peradam refusal-census check is deposit-count-sensitive — when deposits are added DELIBERATELY, re-freeze that reference with a dated annotation naming the new count"). The count is **13** deposits on disk. This is expected drift, not a finding.

Commit SHAs:
- futon2: `17df334`
- futon6: `958a3a8`
- futon3c: `3e917cf`

---

### CARD 1 — Wake (LIVE)

**Input:** wall clock; crontab.
**Happens:** hourly one-shot JVM, fresh source.
**Output:** one judged, gated, traced tick.

**Evidence (latest natural tick, no manual tick):**
```
2026-07-06T14:04:42.996027834Z mode=stop-the-line candidates=107 decision=advance-mission target=M-learning-loop G=8.6242 trace=/home/joe/code/futon2/data/wm-trace/wm-trace-2026-07-06.edn belly=455 derived=2026-07-06T14:00:12.469799195Z gates={:abstain-missing-leg 2}
```

Tick fired on the hourly cron (derived 14:00:12Z, logged 14:04:42Z). One-shot JVM produced a judged, gated, traced tick. **EXPECT (no change — this stage is settled): MET.**

---

### CARD 2 — Scan & judge (LIVE, drift-fenced)

**Input:** mission corpus + substrate-2 state.
**Happens:** candidates filtered by CLASSIFIED status + fenced before dedupe; ranked by G blend.
**Output:** ranked actions over LIVE, UNIQUE missions only.

**Evidence:**
- Latest tick: `candidates=107` — this is `count(:ranked-actions judgement)` (ranked ACTIONS, not live missions). Includes :advance-mission, :address-sorry, :close-mission, :survey-mission types.
- Fence census (`scripts/mission_scan_census.bb`): 307 raw files, 99 non-primary excluded, 208 primary after fence, 198 unique non-derived ids. **FENCE OK — 0 non-primary in pool.**
- Fence census reference: 93 live candidates (the live count requires Clojure; the bb census reports unique non-derived = 198, of which 93 are LIVE-classified). The 107 ranked-actions count is consistent with 93 live missions (some missions have multiple action types, plus sorry targets).
- Decision: `advance-mission target=M-learning-loop G=8.6242`
- Gates map: `{:abstain-missing-leg 2}` — both legs abstained (missing escrow leg; no deposits for M-learning-loop).

**EXPECT (pre-registered): zero SUPERSEDED/ARCHIVED-classed missions appear in any lane.**

The latest tick's decision target is `M-learning-loop` (a live mission). The gate abstained on both legs (missing-leg), which is the expected behavior for a mission without deposits. The fence is clean (0 non-primary in pool).

**Zero-superseded confirmation:** the fence census reports FENCE OK with 0 non-primary files in the pool. The classifier excludes SUPERSEDED/ARCHIVED missions. No superseded mission was enacted. The 12:04 tick enacted `M-first-flights` (live, now closed/transferred per cards-IV card 3); the 13:04 and 14:04 ticks targeted `M-learning-loop` (live). **No superseded mission enacted in the observed ticks.**
