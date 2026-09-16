# Revision 3 K1/K2/K3 predecessor correction

Coordinator notice `invoke-1789574781644-21523-62169405` arrived after implementation
6c417fc8 and its test addendum b7dde58a. This correction implements the appended
revision-3 contract before independent acceptance; no contract conflict was found.

## Exclusion requires positive producer evidence

The exact not-reached-construction marker is checked against all seven checkpoint
envelopes using the existing checkpoint order, schema version and cell validation.
Same cohort/attempt/ordinal, sequence, checkpoint type, directory identity and
monotone times are required. The construction and subsequent checkpoints must be
coherent producer not-reached sorries with the close outcome; a judgment, carrier,
cascade or later performed checkpoint contradicts exclusion. Close success/artifact
claims and conflicting selected-target evidence refuse.

Selection must be explicit: a coherent selected action or a no-selection/not-reached
selection sorry. Missing fields alone do not establish non-construction. A valid
no-selection chain needs no target; its construction exclusion applies regardless
of target, without claiming a different target was selected.

Any present retention block must validate and agree with checkpoint/action identity
and time. Any present manifest must validate, agree with retention IDs, bind all
six pre-close checkpoint files, match every supplied source digest, and precede
close. A corrupt present binding is never ignored. Absence of a manifest is allowed
for the positively evidenced historical marker form explicitly authorized in K2;
it is not a waiver for actual construction history.

Excluded attempts retain seven source paths/digests, identity and reason, both in
successful predecessor provenance and in a subsequent actual-predecessor refusal.
K1 stays intact: a later proven non-construction close does not repair or reset an
older unsupported actual construction. No roots are narrowed and no epoch or
migration is invented. Fully deleted closed history remains undetectable.

## Executed controls

Predecessor namespace: 13 tests / 150 assertions pass. Selected-target marker chains
with/without manifest and explicit no-selection chains can be excluded, with or
without an older valid predecessor. Missing construction, contradictory carrier or
cascade, bad identity/type, malformed/unknown marker, corrupted manifest or source,
and performed later checkpoint/close claims refuse. Earlier no-older damaged-history
controls remain. An unsupported older construction still refuses after a later
no-selection marker, retaining exclusion provenance.

The new controls reject the pre-ruling 6c417fc8 source in a separate temporary test
JVM (expected exit 1). Caller mission/ticket controls pass, 2 tests / 16 assertions.
No reason to rerun the earlier full slow caller matrix for this marker-only branch;
the changed path is exercised directly by the predecessor suite.

The actual WM08 classification revision inspected is fb849b22. The hermetic
k23-classification script passes two real runner cases: truncated history gives
interpretation/invalid-receipt and environmental-hold; an injected NullPointerException
at construction gives untyped-failure and machine-failure. Both call only the fixture
interpreter, never the legacy constructor or build worker. This checks routing, not
permission to discharge a history obligation. Source/classifier outside the allowed
boundary is untouched. Kondo and parens pass; exact commands and raw output retained.

No independent implementation acceptance, live use, operational recovery, census,
DAG change, serving action or successor is claimed. Prior receipts remain unchanged.
