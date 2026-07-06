# Ground Control Test 2026-07-06 — fold-turn deposit run log

## Ground-control review (codex-1)

Pre-registered question: can independently commissioned `zai` agents produce valid fold-turn deposits from clear instructions alone?

Strict metric: `N/5` first deposit submissions pass PIN-1B plus 2f with zero ground-control content repairs.

Result: **3/5** commissioned agents produced mechanically valid deposits. The two failures (`zai-4`, `zai-11`) exhausted the one-continuation allowance on HTTP 429 service overloads and produced no deposit to evaluate. Among agents that reached a deposit submission, **3/3** passed PIN-1B and 2f without ground-control content repairs.

Interpretation against the pre-registration: strict `3/5` sits in the "one revision cycle" band, but the named failure modes are not deposit-contract misunderstandings. The supported issues are:
- transport/service instability under parallel commissioning (`HTTP 429`, repeated);
- shared-log ambiguity: the prompt said "append your run log", but `zai-5` overwrote the ground-control scaffold before later agents appended;
- tracking ambiguity for ignored fold-turn files: `zai-6` left `ft-aif-faithfulness-001.edn` as ignored runtime state even though its deposit passed mechanically; ground control force-added it after review so the artifact is not lost.

Doc-debug repairs made before dispatch in `/home/joe/code/futon3c/holes/GROUND-CONTROL.md`:
- §1 now says to spawn and immediately dispatch each fresh agent before spawning the next, because repeated `/agents/auto` calls without an intervening invoke reclaimed the same idle, session-less `zai-4`.
- §3.5 now instructs authors to compute stored `:eval :delta-g` via `futon2.aif.fold-eval/coverage-delta-g` over `fold-llm/construction->wiring`, retaining hand coverage only as supporting evidence.

Independent review evidence:

```text
$ cd /home/joe/code/futon2 && clojure -M scripts/gate_2f_deposit.clj
  ft-aif-faithfulness-001: sha-match + replay + dG re-assert OK (dG -0.5555555555555556; ...)
  ft-evaluate-policies-009: sha-match + replay + dG re-assert OK (dG -0.6153846153846154; ...)
  ft-legacy-sorry-cleanup-001: sha-match + replay + dG re-assert OK (dG -0.5714285714285714; ...)
GATE 2f PASS — 11 deposit(s) load cleanly, sha-matched, replayable
```

Dispatch/review table:

| Agent | Mission | Rounds | Result | Deposit | Commit(s) | Ground-control finding |
|---|---|---:|---|---|---|---|
| `zai-4` | `futon2-d/mission/operational-vocabulary` | 2 | failed | none | none | first turn reached pin setup after mana spend, then HTTP 429; continuation failed immediately with HTTP 429 |
| `zai-5` | `futon2-d/mission/evaluate-policies` | 1 | pass | `ft-evaluate-policies-009` | futon6 `e33b9e3`, futon2 `255090e` | valid; overwrote shared log scaffold instead of appending |
| `zai-6` | `futon2-d/mission/aif-faithfulness` | 2 | pass | `ft-aif-faithfulness-001` | futon2 `20bc0ac`; futon6 committed by ground control after review | valid; first turn HTTP 429 after mana spend; deposit file was ignored/untracked until ground-control packaging |
| `zai-8` | `futon2-d/mission/legacy-sorry-cleanup` | 2 | pass | `ft-legacy-sorry-cleanup-001` | futon6 `e72a0f2`, futon2 `ac10bf6` | valid; first turn failed before work with HTTP 429; agent fixed its own prompt-sha/trailing-newline mismatch before final submission |
| `zai-11` | `futon2-d/mission/fold-ansatz` | 2 | failed | none | none | first turn HTTP 429 mid-flight; continuation failed immediately with HTTP 429 |

---

## Run: ft-evaluate-policies-009 (zai-5)

**Mission:** futon2-d/mission/evaluate-policies
**Primary doc:** /home/joe/code/futon2/holes/M-evaluate-policies.md
**Agent:** zai-5
**Edge:** invoke-1783327295882-577-07917bbb

### §0 Mana spend (FIRST act)

- Gate: `fold-authoring`
- Consume call: `(mg/consume! "fold-authoring" "ground-control-test-2026-07-06: ft-evaluate-policies-009 deposit")`
- Result: `{:ok true, :balance 4}`
- Spend timestamp: `2026-07-06T08:44:09.327175204Z`
- Award cited: `{:n 5, :operator "Joe 2026-07-06 ~09:45: 5-agent commissioning test — 'we could test just that part of the loop on, say, 5 or so independently commissioned Zai agents. It shouldn't hurt to seed in a variety of well formed plans.'", :at "2026-07-06T08:37:50.536122772Z"}`
- Balance after spend: 4

### §1 ψ (sorry-grain)

Authored from M-evaluate-policies.md §1 (the tension) + §0 HEAD (EFE honesty, the Carver question).

- ψ-sha256: `345bb0824b8e04b6288077b2146ff0fce24d9badf91bfdfb18c32a775eb33165`
- ψ-length: 1903 chars
- No sealed corpus → no-blind-scoring stated honestly in `:psi-recipe`

### §2 Cascade

- Constructor: `cd futon3a && .venv/bin/python holes/labs/M-memes-arrows/cascade_serve.py`
- Constructor commit: `fe8aceab21c48783d0721c4ccb19e3806bb12f45`
- Size: 12 (budget 20, untruncated)
- F-free-energy: 3.999
- Shown pattern-ids (with rel):
  1. aif/expected-free-energy-scorecard (rel 0.521)
  2. aif/term-to-channel-traceability (rel 0.472)
  3. aif/candidate-pattern-action-space (rel 0.389)
  4. aif/experimental-comparison-of-EFE-variants (rel 0.505)
  5. aif/predictive-entropy-as-ambiguity (rel 0.444)
  6. aif/belief-aware-risk-term (rel 0.351)
  7. structure/interest-event-vocabulary (rel 0.343)
  8. system-coherence/facet-before-aggregating (rel 0.332)
  9. aif/no-self-certification (rel 0.36)
  10. aif/declare-the-conditioning (rel 0.467)
  11. aif/valuation-reads-the-paperwork (rel 0.343)
  12. aif/placeholder-is-load-bearing (rel 0.407)

### §3 Fold (LLM turn — zai-5 judgment)

8 boxes folded from 8 of the 12 shown patterns (4 scoped out: candidate-pattern-action-space, interest-event-vocabulary, facet-before-aggregating, valuation-reads-the-paperwork — retrieved patterns are invitations, not obligations; the charter's scope-out beats the cascade's suggestion).

Each box has `:fits-pattern` AND `:addresses-however` engaging the actual circumstance.

### §4 Wiring (v2)

- Connectives: `{:seq 7, :tensor 2}`
- Finding: mostly :seq (the cascade has a strong linear dependency chain from the core-overlay split through the census to the exhibit/sentence). The two :tensor edges are the parallel branches from b1 (the scorecard feeds both the mask split b3 and the ambiguity repair b5 in parallel, non-signalling).
- Terminals: `[{:id :b4 :discharges :want-signature} {:id :b7 :discharges :want-signature}]` — two terminals for the two WANT clauses (the exhibit that makes the gap visible, and the paper's sentence decided by code).

### §5 ΔG (real fold-eval path)

```
(futon2.aif.fold-eval/coverage-delta-g
  (futon2.aif.fold-llm/construction->wiring answer))
```

- Boxes: 8
- Holes: 5
- Coverage: 8/13 = 0.6153846153846154
- **ΔG: -0.6153846153846154**

### §6 PINS (computed via REAL functions)

- `:prompt :sha256` = `2942248e10ac177b6451895af8d9b403953597dcaa373095c32baaa778502ef5`
  - Computed via `futon2.aif.fold-llm/fold-prompt` with cascade pattern-ids, circumstance `{:mission "futon2-d/mission/evaluate-policies" :psi <exact ψ>}`, prose-fn = verbatim flexiarg slurp
  - Reconstructed by loader: MATCH (pin-1b verified)
- `:prompt :prose-sha256` = per-pattern sha256 of verbatim flexiarg files (12 entries)
- `:prompt :prose-source` = `"verbatim (slurp \"/home/joe/code/futon3/library/<pattern-id>.flexiarg\")"`
- `:arming` = mana gate spend (gate fold-authoring, timestamp 2026-07-06T08:44:09.327175204Z, balance 4)

### §7 Deposit

- Path: `/home/joe/code/futon6/data/fold-turns/ft-evaluate-policies-009.edn`
- Contract: v2

### §8 Proofs

**load-deposits (acceptance):**
- 9 deposits loaded, 0 rejected
- ft-evaluate-policies-009 found with delta-g -0.6153846153846154, prompt-sha 2942248e..., arming operator joe

**tamper-copy (rejection):**
- Tampered copy in /tmp with ΔG changed to -0.999999
- REJECTED: `:delta-g-mismatch` — pin 3: stored -0.999999 vs recomputed -0.6153846153846154

**Gate 2f:**
- `[2f] gate PASS` — 9 deposit(s) load cleanly, sha-matched, replayable

---

## zai-6 run log — ft-aif-faithfulness-001

Agent: zai-6. Mission: `futon2-d/mission/aif-faithfulness`. Continuation of `invoke-1783327310371-578-9eba1d34` (first invoke failed mid-flight HTTP 429; one continuation `invoke-1783327471398-585-d254b101` sent).

### §0 Mana

- Pre-existing spend detected (consumed before the crash, not duplicated):
  `{:type :spend, :gate-id "fold-authoring", :purpose "ground-control-test-2026-07-06 ft-aif-faithfulness-001 authoring", :at "2026-07-06T08:43:08.623680383Z"}`
- Balance after spend: 6 (was 7 before this spend).

### §1 ψ

- ψ-sha256: `31520d1d84e200afe529e761c9b082b794e1e1b2db06367b36da5439e0a0cf2b`
- ψ-recipe: no sealed corpus; empirical sorry-grain from M-aif-faithfulness §0 + §1.1 + §2.1 verdict ledger. no-blind-scoring stated honestly.

### §2 Cascade

- Constructor commit: `fe8aceab21c48783d0721c4ccb19e3806bb12f45` (futon3a HEAD)
- Size: 5, truncated: false, F-free-energy: 0.296, budget: 20, lambda: 0.25
- Shown pattern-ids + rel:
  - `sidecar/append-only-semantic-audit` (rel 0.478, mc 0.144)
  - `sidecar/explicit-promotion-to-facts` (rel 0.434, mc 0.304)
  - `sidecar/fact-lifecycle-event-types` (rel 0.455, mc 0.319)
  - `pattern-discipline/pattern-to-code-receipts` (rel 0.416, mc 0.291)
  - `pattern-discipline/conclusion-required-others-recommended` (rel 0.4, mc 0.2)
- Note: cascade is sidecar/pattern-discipline flavored, NOT AIF-engine flavored — this is an honest library-gap signal (the dark-build-flip pattern the mission names was not surfaced). Recorded as policy-holes, not enriched.

### §3-4 Fold + Wiring (v2)

- 5 boxes, 4 policy-holes, 1 terminal (:b4 discharges :want-signature)
- Wiring: b1→b2 :seq, b1→b3 :seq, b2→b4 :seq, b3→b4 :seq, b3→b5 :copar
- Finding: 4 of 5 edges are :seq (the badge-audit → promotion → receipt chain is genuinely sequential); one :copar edge (b3→b5, the D8 disposition is coupled with the lifecycle event — only-valid-together). Not faked variety.
- Policy-holes surface: B-0a tick-provenance mechanism, the dark-build flip pattern itself, per-quantity D8 disposition types, receipt witness shape per badge transition.

### §5 ΔG

- Computed via real path: `(futon2.aif.fold-eval/coverage-delta-g (futon2.aif.fold-llm/construction->wiring answer))`
- Coverage: 5/(5+4) = 0.5556; ΔG = -0.5555555555555556
- Hand coverage derivation matches: 5 boxes, 4 holes, 9 total → 5/9 = 0.5556.

### §6 PINS (computed via REAL functions)

- `:prompt :sha256` = `4cea144955e9d8d56ee3f64513f44b3cbe0e17fda91d05a753a69189cbd279ba`
  - Computed via `futon2.aif.fold-llm/fold-prompt` with cascade pattern-ids, circumstance `{:mission "futon2-d/mission/aif-faithfulness" :psi <exact ψ>}`, prose-fn = verbatim flexiarg slurp.
  - Reconstructed by loader: MATCH (pin-1b verified).
- `:prompt :prose-sha256`:
  - `sidecar/append-only-semantic-audit` → `20b4c6ea725c9636f768742d372d0862eb3a872bb21b42ba14724c96be892f4e`
  - `sidecar/explicit-promotion-to-facts` → `64a898137fb7cfdd19f39e6072c00bace8c2aedc6e745962688d95b3daf2c468`
  - `sidecar/fact-lifecycle-event-types` → `83232f9ef4c5c45f73feec70a5d7d7cbf2006b0375271ee01cbe8ad4f8a2a5e3`
  - `pattern-discipline/pattern-to-code-receipts` → `abc5e83ed95104547bdfccff0b774a3744f9206fb1436fcfb29ab679f7b25cc3`
  - `pattern-discipline/conclusion-required-others-recommended` → `778bc06285d440a76a02f9a15b6b2c11d66a5434d16f7961a8ec6ba43d85ac18`
- `:prompt :prose-source` = `"verbatim futon3/library/<id>.flexiarg"`
- `:arming` = mana gate spend (gate fold-authoring, spend at 2026-07-06T08:43:08.623680383Z, balance-after 6; operator word from Joe 2026-07-06 ~09:45 award).

### §7 Deposit

- Path: `/home/joe/code/futon6/data/fold-turns/ft-aif-faithfulness-001.edn`
- Contract: v2

### §8 Proofs

**load-deposits (acceptance):**
- 10 deposits loaded, 0 rejected (8 original + ft-evaluate-policies-009 + ft-aif-faithfulness-001 + ft-legacy-sorry-cleanup-001 = 11 files, 10 valid — legacy-sorry-cleanup-001 failed pin-1b prompt reconstruction, NOT mine)
- ft-aif-faithfulness-001 found with delta-g -0.5555555555555556, prompt-sha 4cea1449..., arming operator joe

**tamper-copy (rejection):**
- Tampered copy in /tmp/tamper-test with ΔG changed to -0.123456789
- REJECTED: `:delta-g-mismatch` — pin 3: stored -0.123456789 vs recomputed -0.5555555555555556

**Gate 2f (my deposit):**
- `[2f] gate FAIL` at the DIR level — but the failure is on `ft-legacy-sorry-cleanup-001.edn` (zai-8's deposit), NOT mine. My deposit passes all 2f checks individually: (a) load OK, (b) prompt-sha match (stored 4cea... = rebuilt 4cea...), (c) replay match (recorded answer = turn-fn output), (d) delta-g -0.5556.
- The dir-level gate cannot PASS while legacy-sorry-cleanup-001 is broken; that is zai-8's repair, not mine.

---

## zai-8 run log — ft-legacy-sorry-cleanup-001

Agent: zai-8. Mission: `futon2-d/mission/legacy-sorry-cleanup`. Continuation of `invoke-1783327385437-583-18a2dfc7` (first invoke `invoke-1783327321228-579-470b6606` failed before work with HTTP 429; one continuation sent).

### §0 Mana

- Consumed FIRST per §0: `(mg/consume! "fold-authoring" "ground-control-test-2026-07-06: ft-legacy-sorry-cleanup deposit authoring")` → `{:ok true, :balance 3}`
- Spend entry: `{:type :spend, :gate-id "fold-authoring", :purpose "ground-control-test-2026-07-06: ft-legacy-sorry-cleanup deposit authoring", :at "2026-07-06T08:45:48.724953313Z"}`
- Balance after spend: 3.

### §1 ψ

- ψ-sha256: `b7b62cc38b77361ff3210e37213287a5c93d80971f9469eb4e9de89f600eeddb`
- ψ-recipe: no-blind-scoring — no sealed corpus exists for this mission's sorries; the ψ is distilled from the mission doc's actual tension (§1 IDENTIFY the gap, §3 review protocol) and the snapshot's enumerated kind/status distribution. Stated honestly.

### §2 Cascade

- Constructor commit: `fe8acea` (futon3a HEAD)
- Size: 11, truncated: false, F-free-energy: 1.113, budget: 20, lambda: 0.25
- Shown pattern-ids + rel:
  - `contributing/stack-scan-logging-protocol` (rel 0.543, mc 0.163)
  - `futon-theory/mission-interface-signature` (rel 0.526, mc 0.158)
  - `futon-theory/local-gain-persistence` (rel 0.485, mc 0.339)
  - `stack-coherence/commit-intent-alignment` (rel 0.478, mc 0.335)
  - `devmap-coherence/ifr-f1-dhammavicaya` (rel 0.46, mc 0.414)
  - `storage/durability-throughput-gate` (rel 0.437, mc 0.306)
  - `coordination/session-durability-check` (rel 0.437, mc 0.218)
  - `invariant-coherence/shape-first-identify` (rel 0.437, mc 0.218)
  - `futon-theory/derive-exits-on-a-minted-sorry` (rel 0.47, mc 0.235)
  - `sidecar/append-only-semantic-audit` (rel 0.458, mc 0.229)
  - `exotic/tri-store-lineage` (rel 0.445, mc 0.311)

### §3-4 Fold + Wiring (v2)

- 8 boxes (folded from 8 of 11 patterns; 3 scoped out as policy-holes — storage/durability-throughput-gate, coordination/session-durability-check, exotic/tri-store-lineage — all honest scope-outs per the charter).
- 6 policy-holes total (3 scope-outs with `:free nil` + 3 genuine ungroundable policy questions).
- Terminals: `[{:id :b4 :discharges :want-signature} {:id :b7 :discharges :want-signature}]` — two terminals for the scan-log output and the disposition-alignment output.
- Wiring connectives: b1->b2 :seq, b2->b5 :seq, b2->b6 :copar, b1->b3 :tensor, b3->b4 :seq, b5->b8 :seq, b6->b7 :seq, b8->b7 :seq.
- Finding: mostly :seq (the shape-first -> derive -> interface-recovery chain is genuinely sequential). One :copar (b2->b6: the typed-exit and local-gain-persistence dispositions are coupled — only-valid-together). One :tensor (b1->b3: the shape-first enumeration and the append-only audit trail are parallel, non-signalling).

### §5 ΔG (real fold-eval path)

- Boxes: 8, Holes: 6, Coverage: 8/14 = 0.5714285714285714
- **ΔG: -0.5714285714285714** (via `futon2.aif.fold-eval/coverage-delta-g` over `fold-llm/construction->wiring`)
- Hand coverage derivation matches: 8/14 = 0.5714.

### §6 PINS (computed via REAL functions)

- `:prompt :sha256` = `0b03a758bbb977dccc9077aa7b35092d901422cc03239db2e2c95f6849e209a9`
  - Computed via `futon2.aif.fold-llm/fold-prompt` with cascade pattern-ids, circumstance `{:mission "futon2-d/mission/legacy-sorry-cleanup" :psi <exact ψ>}`, prose-fn = verbatim flexiarg slurp.
  - Reconstructed by loader: MATCH (pin-1b verified).
  - **Correction during authoring**: initial prompt-sha `d49e4b37...` was computed from ψ WITHOUT trailing newline (the raw `/tmp/psi.txt` file). The EDN stores the ψ WITH trailing `\n` (the final newline before the closing quote in the EDN literal). The loader reads the EDN-ψ (with newline) and reconstructs a DIFFERENT prompt, causing pin-1b mismatch. Fixed by recomputing the sha from the exact EDN-ψ as the loader reads it. The ψ text is canonical; the trailing newline is part of it.
- `:prompt :prose-sha256` = 11 per-pattern sha256 values (see deposit file for all)
- `:prompt :prose-source` = `"verbatim futon3/library/<pattern-id>.flexiarg"`
- `:arming` = mana gate spend (gate fold-authoring, spend at 2026-07-06T08:45:48.724953313Z, balance-after 3; operator word from claude-16 award).

### §7 Deposit

- Path: `/home/joe/code/futon6/data/fold-turns/ft-legacy-sorry-cleanup-001.edn`
- Contract: v2

### §8 Proofs

**load-deposits (acceptance):**
- 11 deposits loaded, 0 rejected
- ft-legacy-sorry-cleanup-001 found with delta-g -0.5714285714285714, prompt-sha 0b03a758...

**tamper-copy (rejection):**
- Tampered copy in /tmp/tamper-test with ΔG changed to -0.1234567890123456
- REJECTED: `:delta-g-mismatch` — pin 3: stored -0.1234567890123456 vs recomputed -0.5714285714285714

**Gate 2f:**
- `[2f] gate PASS`
