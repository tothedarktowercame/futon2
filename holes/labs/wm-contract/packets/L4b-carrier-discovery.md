# L4b — Discovery: where the record keeps each Lean carrier (claude-2 → claude-3, 2026-11-01)

Plan slice L4, second part. **Discovery only: no code changes.** The deliverable is one
report, `futon2/holes/labs/wm-contract/reconciliation/L4b-carrier-map-claude3-2026-11-01.md`,
that the L4c implementation packet will be written from. Bell claude-2 with its path.

State: mathlib4 darktower `55878e243a` (L1–L3b: `RunKind`, `Decision`, `SelectionPath`,
`policyG`, `abstentionG`, `policyPrediction`, `abstentionPrediction`); futon2 main
`641719d4c` (L4a + Decision rendering). The checker reads these export keys and today the
projector emits typed `.nr` for: `targetConstruction`, `constructorPatternCount`
(reason "selection certificate target construction absent"), `policyG`, `abstentionG`,
`policyPrediction`, `abstentionPrediction`. The selection run is not projected at all.

## Questions, one table row each

For every carrier below, in the 11-01 record copy at
`~/notes/l4a/claude2/tick-run-record-2026-11-01-ac5bc477-9e34-4249-a692-f13a9f22a324.edn`
(and then across all 28 era-2 records in `futon2/data/wm-runs/tick-run-record-2026-10-29*`
through `…-11-01*`, read-only), report: the EDN path; the value's shape; whether the value is
exact (integer, ratio, sha) or a double; whether it is present in all 28; and which producer
namespace writes it (`grep -rn` in `src/futon2/aif`, cite file:line).

**Work run (inner), `RunFacts` fields:**
1. `targetConstruction` — start at `[:decision :selection-certificate :q2-construction]`
   (`:schema :wm/q2-construction-census-v1`, `:status :recorded`, `:library` pinned, count
   1527). Map each `TargetConstruction` field: `targets`, `action`, `reading`, `library`,
   `graph`, `kernel`, `slice`, `pool`, `policyCount`, `policies`. Say which have no source.
   `sliceFromWholeLibrary` is to be *computed* by the projector (every slice pattern is in
   the pinned manifest with the cited bytes): say what inputs exist for that computation.
2. `constructorPatternCount` — from the same census, or absent.
3. `policyG` — candidates: `[:decision :selection-certificate :scoring]` (indexed, `:g-terms`
   with `:combination :risk-plus-ambiguity-minus-information-gain`),
   `[... :target-policy-families * :policies * :g-terms]`, `[... :preference-receipts * :g-terms]`,
   `[... :parameter-novelty * :terms :serving-G]`, `[... :candidates * :class-emission :risk]`.
   Which one is the G the selection law minimised (`:law-applied {:law :sigma-log-E-minus-gamma-G …}`)?
   Are they doubles? Do the policy ids there match `comparedPolicies`' ids
   (`[... :q4 :compared]`)? One id namespace is required (L2 review).
4. `abstentionG` — `[:decision :abstention]` reads `{:status :not-abstained}`. Is a
   stopped-kernel G computed anywhere on the inner path? If not, say so plainly: that
   becomes an L5 requirement and Q11 stays unverifiable on work runs until then.
5. `policyPrediction` — per policy, the predicted outcome distribution. Start at
   `[... :candidates * :certificate :node-evaluations * :states * :kernel]`. What is the
   smallest stable thing to digest per policy, and is it the same object the law scored?
6. `abstentionPrediction` — the stopped distribution. Present? (Likely not: see 4.)
7. `currentChoice`/`previousChoice` already project; confirm the `.pick` ids are the same
   namespace as `comparedPolicies` (`[:decision :chosen :id]` is
   `"E-comprehension-foundation/policy-78fb…"`; what are the compared ids?).

**Selection run (outer), from `[:outer/selection]`:**
8. The field: `:outer-field/admitted`, `:outer-field/excluded` → `enumeratedTasks` and
   the 17 typed exclusions; counts across the 28.
9. Policies: `:tied-cascades` (names), `:cascade-id`, `:cascade-index`, `:cascade-picks`,
   `:cascade-scores` (doubles; `[3.2121… ×3]` here) → `comparedPolicies`, `policyG`.
   Is there a per-cascade prediction, or only the one `:prediction {:outcomes {…}}`? If one,
   whose is it?
10. Abstention: `:no-pick` appears in `:cascade-picks` and `:cascade-pick-relations`. Is
    its score in `:cascade-scores`, or separate? Which value is the abstention G?
11. Construction: is there anything in the record pinning the task-selection library, the
    Giraffe pass (`:giraffe/run-id`, `:giraffe/manifest-sha256`), the makeup
    (`:makeup/sha256`) and the cascades' pattern ids, enough to fill a `TargetConstruction`
    for the selection kind? List the gaps against the hand witness
    `futon2/holes/labs/wm-contract/witnesses/outer-selection-conformant-2026-11-01.edn`.
12. `SelectionPath`: `receipt` (digest of `:outer/selection` itself), `pickRelation`
    (`:selected-relation`, `:cascade-pick-relations`), `pickReach` (`:selection :selected :reach`,
    from `:default-reach`), `pendingRun`/`consumedRuns` (`:outer/learning`, the
    `:classification-absence` entries), `learningApplied` (`:outer/learning :status`).
13. `previousOutcome`/`previousInputDigest`/`currentInputDigest` for the selection run: is
    there a Q6-shaped carrier on the outer path at all?

## Exactness: propose, do not decide

Every G in the record appears to be a double. Q11 compares exact rationals and the checker
refuses floats. Propose the projection rule for L4c: a declared denominator (I expect
10^6), the float recorded alongside the rational for audit, and the consequence stated:
two scores within 10^-6 become an exact tie and Q11 refuses the run. That is the intended
direction (arithmetic noise must not decide a selection). Give the smallest observed gap
between distinct scores across the 28 records so Joe can judge the scale.

## Gates

Read-only on `data/`. No commits except the report. Keep every claim tied to a path you
read; where you infer a producer from a grep, say "grep suggests" rather than asserting.
Bell claude-2 with the report path.
