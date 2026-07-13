# Simulation findings: A-matrix Stage 2 spike (M-aif-a-matrix-faithfulness)

**Date:** 2026-07-13
**Script:** `scripts/aif/a_matrix_simulation.bb`
**Artifact:** `holes/labs/M-aif-faithfulness/a-matrix-simulation.edn`
**Status:** Complete; findings recorded

## What the simulation does

50 synthetic entities × 10 ticks, with:
- A **lifecycle transition kernel** for ground-truth status (spawned → refined → strengthened → addressed, with branching to falsified/foreclosed/reopened)
- A **noisy observation confusion matrix** that generates realistic observations — usually matching the true status but sometimes lifecycle-adjacent or contradictory
- Belief tracking under three configurations: `:legacy`, `:aif` (observation-model-v1 with off-diagonal structure), and `:aif-identity` (normalised identity A)

Posteriors carry across ticks (simulating `*carry-belief?*`), so priors are non-uniform after the first observation. This addresses both root causes the shadow experiment identified (100% uniform priors, single event type).

## Corpus census

All 7 event types are represented in the 500 observations:

| Event type | Count |
|---|---|
| :strengthened | 100 |
| :falsified | 78 |
| :addressed | 76 |
| :refined | 70 |
| :spawned | 68 |
| :foreclosed | 63 |
| :reopened | 45 |

This is the type variation the real corpus (100% :strengthened) completely lacks.

## Key findings

### 1. The off-diagonal structure is behaviourally active

KL divergence between `:aif` and `:legacy` posteriors: mean 0.050, median 0.048, max 0.117 nats.

Compare with the shadow experiment on real data: mean 0.001 nats. The off-diagonal structure moves posteriors **50× more** on a corpus with type variation. This is the core validation: the off-diagonal A is not inert when fed varied evidence.

### 2. Contradiction sensitivity works

When observing `:strengthened` for an entity whose true status is `:falsified`:

| Mode | Mean P(:falsified) in contradiction ticks |
|---|---|
| :legacy | 0.152 |
| :aif | **0.101** |
| :aif-identity | 0.152 |

The `:aif` model suppresses `:falsified` mass by 33% relative to legacy. This is the capability the diagonal form structurally lacks — it can say "this observation counts *against* that status." The effect size (0.05 absolute probability mass redistribution) is small but consistent and correctly signed.

### 3. Posterior entropy is slightly lower under :aif

| Mode | Mean entropy (nats) |
|---|---|
| :legacy | 1.689 |
| :aif | 1.670 |

Lower entropy = more informative posteriors. The off-diagonal structure concentrates belief slightly more than the diagonal, because it actively redistributes mass (up for compatible statuses, down for contradictory ones).

### 4. Raw argmax accuracy is roughly equal

| Mode | Status accuracy |
|---|---|
| :legacy | 0.410 |
| :aif | 0.386 |
| :aif-identity | 0.410 |

The `:aif` model does not improve raw accuracy over legacy with the current hand-set values (1.3, 0.7). This is expected: the values are reasonable but not calibrated, and with identity B and single-entity tracking, the diagonal model is hard to beat on argmax alone. Accuracy improvement requires either (a) calibrating A against real exogenous data, or (b) a non-identity B that models lifecycle transitions.

### 5. :aif-identity exactly reproduces :legacy

The `:aif-identity` results are byte-identical to `:legacy` on every metric. This confirms that the normalised identity observation model is the faithful generalisation of the legacy update — the formal Stage 1 conversion is exact.

## Interpretation

The simulation validates the DERIVE design's central claim: **off-diagonal structure in A is behaviourally useful, not just mathematically well-formed.** It exercises the two capabilities the diagonal form lacks:

1. **Lifecycle-adjacent confusion**: an observation can be mildly compatible with a neighbouring status
2. **Contradiction**: an observation can be evidence *against* an opposed status

The effect on raw accuracy is neutral with uncalibrated values, but the effect on posterior shape is real and correctly signed. Calibration against exogenous data (Stage 2 proper) should improve accuracy, but the simulation proves the machinery works and the structure bites.

The simulation also serves as a **template for the real distinguishing corpus**: the synthetic generation shows what a real corpus needs (varied types, sequential histories, independent status signal), and the comparison metrics are exactly the ones C6/C7 require.

## What this does NOT show

- That the hand-set values (1.3, 0.7) are *correct* — only that off-diagonal structure of this kind produces measurable behavioural effects
- That a non-identity B would improve accuracy — the simulation uses identity B; testing lifecycle-constrained B is a secondary axis not yet explored
- That the real exogenous corpus (handoffs, fold outcomes, QA) will produce the same pattern — the synthetic confusion matrix is an assumption, not a measurement
