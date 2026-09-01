# C386 — dependency-closure boundary

Date: 2026-09-01

## Finding

Lean can derive the exact transitive **constant-name** closure of an elaborated declaration. The installed `ImportGraph.Imports.RequiredModules` API exposes `Lean.Name.transitivelyUsedConstants`, built from each `ConstantInfo.getUsedConstantsAsSet`. This is a real environment traversal, not source-text inference.

Measured examples show why it cannot honestly be equated with the receipt's curated semantic source list:

- `ChannelWitness.declaredVocabulary`: 23 transitive constants, 17 under `DarkTower.WarMachine`;
- `SoftmaxWitness.referenceWeights`: 16,107 transitive constants, 22 under `DarkTower.WarMachine`.

The closure contains generated/private declarations and the complete imported mathematical implementation. The curated receipt basis instead names the theorem, fixture adapter, and selected semantic declarations a reviewer can interpret. Equality between these populations would be false, while filtering to the local namespace would omit external mathematical semantics.

Constant names alone are also insufficient: changing an omitted dependency's body without changing its name leaves a name closure unchanged. Complete protection would require a schema-v2 machine-derived digest over the elaborated `ConstantInfo` bodies for the full transitive closure (under the pinned toolchain), or hashes of every transitively required compiled module. The former is exact but large (Softmax traverses 16,107 constants); the latter is exact at module grain but recreates noisy whole-file hashing. Neither should be described as the current author's list becoming complete.

## Recorded residual

Every v1 receipt now carries, and the validator requires:

```edn
:dependency-closure
{:mode :author-declared-source-slices
 :machine-complete false
 :reason :lean-transitive-closure-not-content-pinned}
```

Thus v1 verifies every recorded component and successful proof, while explicitly declining the claim that the author-declared slices are the complete semantic dependency closure. The residual is **not theoretically permanent**—Lean supplies the traversal needed for a schema-v2 closure digest—but it is permanent for v1 unless that larger, noisier representation is deliberately adopted and tested.
