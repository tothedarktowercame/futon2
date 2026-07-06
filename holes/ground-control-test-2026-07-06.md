# Ground Control Test 2026-07-06 — Flight Log

## zai-4 deposit: ft-operational-vocabulary-001

**Mission:** futon2-d/mission/operational-vocabulary
**Agent:** zai-4
**Edge:** invoke-1783327264520-576-73738e2e

### Mana spend
- Gate: fold-authoring
- Spend at: 2026-07-06T08:44:00.433298832Z
- Purpose: ground-control test 2026-07-06: fold-deposit for M-operational-vocabulary
- Balance after: 4 (consumed from 7; concurrent spends by other test agents reduced it further)

### ψ
- sha256: `6d5b9eaa87238385bb8542ee1107138150b0c02dfb142d898e353576a7599614`
- Source: hand-authored sorry-grain from M-operational-vocabulary mission doc tension (§1 abstain-because-nil-G, §3a forward/backward duality, §4 CPU-validated HAVE, §5 pending box run)
- ψ-recipe: no sealed corpus; no blind scoring — honest statement

### Cascade (cascade_serve.py, futon3a commit fe8acea)
- Size: 4 (untruncated, budget 20)
- F-free-energy: -0.863
- Shown patterns (with rel/mc):
  0. data-mining/constrain-extraction-to-the-downstream-vocabulary (rel 0.534, mc 0.16)
  1. structure/interest-event-vocabulary (rel 0.534, mc 0.16)
  2. iching/hexagram-39-jian (rel 0.506, mc 0.152)
  3. iching/hexagram-40-jie (rel 0.45, mc 0.585)

### Fold (LLM judgment)
- 4 boxes, each with :fits-pattern and :addresses-however engaging the actual circumstance
- 4 honest policy-holes (move-class allowlist membership, granularity enum for meme/sorry entities, boilerplate trust-filter threshold, promotion-path sequencing)
- Coverage: 0.5 (4 boxes / 8 total)
- ΔG: -0.5 (via futon2.aif.fold-eval/coverage-delta-g)

### Wiring (v2)
- b1->b2 :seq (constrained vocabulary feeds event-typing)
- b2->b3 :tensor (obstruction audit runs alongside, non-signalling — sorry half held out)
- b3->b4 :seq (obstruction resolution gates the release)
- b1->b4 :tensor (allowlist discipline applies to box-run output independently)
- Terminal: b4 (discharges :want-signature)

### Pins (computed via real functions, /tmp/pin.clj)
- prompt sha256: `417331cdd971a24469b301bb522c271c49622147b0c74366b39d9f624f91fbd8`
- reconstructed prompt sha256: `417331cdd971a24469b301bb522c271c49622147b0c74366b39d9f624f91fbd8` (MATCH)
- prose-sha256:
  - data-mining/constrain-extraction-to-the-downstream-vocabulary => 776e9fff9a2328b6ce8017d15226f385267f3fd7f002fd3d697e0b1ccd1a1d52
  - structure/interest-event-vocabulary => 35ae9a5e02976f23e7629997c3d564df5d9e6b2eeacfe1e17c1d23679633b20e
  - iching/hexagram-39-jian => ea434ed455af40dd5106457e72248b1e596322cd0e0119bf4e0be0156c78fffd
  - iching/hexagram-40-jie => cecdaeb0ae4b16b6bec2d57da439c023da66f35902b083f7a9b64bfca96f4bbd
- prose-source: verbatim futon3/library/<id>.flexiarg

### Proofs
1. **load-deposits** over /home/joe/code/futon6/data/fold-turns/: 13 deposits loaded, 0 rejected. Mine accepted (delta-g -0.5, prompt-sha 417331...).
2. **Tamper rejection**: copy in /tmp with delta-g changed from -0.5 to -0.999 → REJECTED with `[delta-g-mismatch]` pin 3: stored -0.999 vs recomputed -0.5.
3. **Gate 2f**: PASS

### Deposit path
/home/joe/code/futon6/data/fold-turns/ft-operational-vocabulary-001.edn

### Notes
- First flight attempt 429'd mid-flight after pin computation; this is the one allowed continuation, citing the existing mana spend at 08:44:00Z.
- The two iChing patterns (39-jian obstruction, 40-jie release) form a natural pair with the forward/backward duality: the sorry half is the obstruction the forward mine cannot force through; the box run is the release.
- Honest 4-box/4-hole split on a 4-pattern cascade — zero holes would be overconfidence.
