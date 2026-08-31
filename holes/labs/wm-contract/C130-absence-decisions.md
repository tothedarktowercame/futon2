# C130 — remaining absence decisions

Date: 2026-08-31. Scope: the eight blocked C12 rows after C127. No option is
implemented here. C98 remains the governing semantic rule: absence contributes
no evidence, scores retain support, and unlike supports are not silently
compared.

## Commissioner decision — representation, not ranking

### 1. Numeric observation vector (`observation.clj:152`)

**Question.** Should the public vector boundary be **A**, tagged readings, or
**B**, an explicitly named legacy numeric projection plus a mandatory envelope
for new consumers? **Today:** `sense->vector` turns missing channels into
`0.0`. **Effects:** A breaks numeric-vector callers but makes absence
unrepresentable as zero; B preserves old callers and makes blindness explicit,
without changing ranking until callers migrate. **Reading:** choose **B** as
the compatibility boundary, deprecate it, and require the already-persisted
envelope for new consumers. This is wm-evidence's recommendation, not a ruling.

## Joe decisions — ranking, belief, or action authority

### 2. Prediction triple (`free_energy.clj:98-100`)

**Question.** When observation, predicted mean, or variance is missing, should
the producer **A** omit that channel with a typed reason, or **B** refuse the
whole prediction update? **Today:** each missing member becomes `0.0` and feeds
precision and belief update. **Effects:** A changes belief from the observed
subset and may change later ranking; B holds/refuses the tick and therefore
also changes later ranking. Malformed model output fails under either option.
**Reading:** **A** for genuinely absent observations, **B** for missing model
parameters; retain the resulting support in the update record.

### 3. Strategic mode (`free_energy.clj:138-143`)

**Question.** With any required feature absent, should mode inference **A**
emit reason-bearing `:unknown`, or **B** infer from a specified partial/prior
rule? **Today:** six absences become zeros and can fabricate `:dark`, suppress
`:depositing`, or help satisfy `:hermit`. **Effects:** A removes mode-conditioned
selection for incomplete ticks; B can select while blind and makes the chosen
fallback operational. **Reading:** **A**; any prior/stale/partial inference
should be a separately specified policy. This is safety-relevant.

### 4. Missing sorry pressure (`policy.clj:144-145`)

**Question.** Should the fallback selector **A** abstain/return control with an
unknown-input reason, or **B** continue through branches that do not inspect
sorry pressure? **Today:** absence becomes low pressure (`0.0`) and can select
learn, address-sorry, or no-op. **Effects:** A may stall or invoke upstream
failure routing; B may act while blind to a declared priority signal. **Reading:**
**A**, returning to a named upstream selector rather than manufacturing a
no-op. This directly changes action selection and is safety-relevant.

### 5. Rollout step producer (`rollout.clj:129`)

**Question.** Must every proposed move be **A**, a validated `:scored` or
reason-bearing `:unscored` variant, or **B**, remain a partial map with numeric
fallbacks? **Today:** missing `:score` becomes `0.0` while priors and costs are
formed. **Effects:** A changes which moves can enter ranking and makes the next
decision explicit; B preserves rankings that may depend on fabricated zero.
**Reading:** **A**. This is not merely record shape because validation changes
the ranking population.

### 6. Unscored rollout move (`rollout.clj:158`)

**Question.** Once `:unscored` is explicit, should rollout **A** exclude that
move and continue, or **B** refuse the rollout? **Today:** missing delta, then
missing absolute score, becomes zero cost. **Effects:** A changes the candidate
set and can select another move; B suppresses the whole rollout. **Reading:**
**B** until a producer states why exclusion preserves completeness. This is
safety-relevant wherever rollout authorises action.

### 7. Fulab temperature (`adapters/fulab.clj:81`)

**Question.** Without prediction error, should fulab **A** compute temperature
from uncertainty alone, or **B** refuse to sample? **Today:** missing error is
`0.0`, indistinguishable from a perfect prediction. **Effects:** A changes
temperature and candidate probabilities under partial evidence; B prevents a
ranked/sample decision. **Reading:** **B**; uncertainty-only temperature would
be a new model, not a missing-data repair.

### 8. Belief aggregation (`belief.clj:1040-1052`)

**Question.** Should aggregation **A** omit honestly absent channels, retain
support, and reject malformed entries, or **B** refuse any incomplete
collection? **Today:** missing weighted error and precision become zeros, so
malformed, absent, and measured-zero records aggregate alike. **Effects:** A
changes belief from the valid subset and may change later ranking; B holds the
belief/tick and may also change later ranking. **Reading:** **A**, exactly C98's
no-evidence semantics, with loud refusal for malformed records; do not salvage
malformed collections.

## Decision ownership and current gate

I agree with the proposed ownership boundary: item 1 is a commissioner-level
API/diagnostic-shape choice; items 2–8 alter ranked populations, future belief,
sampling, or safety authority and therefore belong to Joe. The recommendations
above are readings intended to be overridden explicitly.

No behaviour changed. Canonical absence lint remains **8**, and the disposition
ledger still covers all 18 C12 rows as **9 fix-now · 1 exempt-with-reason · 8
blocked**. Current full-suite evidence remains futon2 **1,038 tests / 6,212
assertions** and futon3 **248 / 1,518**, both green at C127.
