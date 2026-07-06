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
