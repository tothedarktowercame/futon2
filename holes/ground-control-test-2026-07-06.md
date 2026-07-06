# Ground Control Test 2026-07-06 — fold-turn deposit run log

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
