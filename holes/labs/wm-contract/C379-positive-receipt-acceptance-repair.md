# C379 — positive receipt acceptance repair

Date: 2026-09-01

The C376 attack was valid: the v1 validator compared recorded and live results but did not require success, and treated the source basis and adapter as optional data. Before repair, 11/30 schema receipts recorded elaboration exit 1 and 12/30 recorded `axioms nil`; reproducible failure could pass.

The validator now requires:

- successful recorded and live elaboration (`exit 0`);
- a present axiom vector restricted to `propext`, `Classical.choice`, and `Quot.sound` (an axiom-free theorem is represented by `[]`, never `nil`);
- at least two nonempty source-basis entries, including the complete named theorem and its semantic dependency declarations;
- a nonempty explicit EDN-to-Lean adapter whose named Lean declaration is pinned and whose `lean-field` strings occur in the retained declaration slices;
- the pre-existing fixture bytes/schema/expected values, toolchain identity, declaration hashes, and live result equality checks.

The eleven failed results were stale/missing imported `.olean` artifacts. Rebuilding the named witness modules restored successful elaboration without weakening any theorem. All 30 `PositiveLeanWitnessReceipt/v1` receipts now validate under the strengthened rules; the separately shaped `modelUncertaintyAndEIG` proof receipt also succeeds with exit 0 and matching source basis. Actual verified positive coverage is therefore **31/31**, split as 16 nontrivial and 15 construction/projection-only.

Controls now reject an empty source basis, removal of the semantic-dependency entry, empty or absent adapter, an unrelated Lean field, recorded exit 1, and `axioms nil`; the honest Softmax receipt still passes.
