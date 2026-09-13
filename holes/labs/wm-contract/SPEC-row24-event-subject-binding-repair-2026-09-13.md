# Row 24 event-subject binding repair

Status: structural rejection refinement only; no successful certificate,
runtime change, byte authentication, or admission.

The commissioned counterexample was valid: the first event-binding predicate
allowed an ordinary `.match` whose selected and enacted subjects had the same
semantic action but different occurrence identifiers. Its earlier theorem
only rejected disagreement between an actual selected subject and its expected
selected subject. It did not compare selection with enaction.

The repaired ordinary-match branch now requires exact occurrence-id equality
in addition to action equality. Selection and enaction remain separately
pinned event records, so their source paths and byte pins may differ while both
refer to the same occurrence. This preserves duplicate semantic-action
occurrences: equal action strings alone do not establish correspondence.

The divergence branch now requires one exact authority subject across all
three layers. `EventBindingEvidence` supplies the complete selected/enacted
event pair. `RunBindingEvidence` must carry the corresponding projected
occurrence/action pair, fixed run, class, grounds, evidence source, authority
identity, and authority pin. `FullScopeEvidence.divergenceAuthorityPin` must be
that same pin. The class must already occur in the externally fixed allowed
class universe. The underlying `FullQualifyingRun` still conservatively
requires different semantic action strings for a divergence; this repair does
not widen that policy.

New theorems reject same-action selected/enacted occurrence mismatch and an
unshared divergence authority pin. All prior implication and rejection
theorems remain available through the imported layers, including exact sole
legacy-scalar retirement.

F11 still owes independent byte authentication, construction of the immutable
universes, and exact record-family and connection run joins. Supplied strings
and hashes are compared structurally here and do not become trusted merely by
inhabiting these structures.

The single targeted compilation at mathlib4 commit
`4cbdfe0d0d0e7a6464399df88de639bf7b4d5ca3` exited zero and emitted the owned
olean. Eight axiom reports contain only `propext`, `Classical.choice`, and
`Quot.sound`; no `sorryAx`. Two unused-variable warnings are retained. There
were no failed proof attempts.
