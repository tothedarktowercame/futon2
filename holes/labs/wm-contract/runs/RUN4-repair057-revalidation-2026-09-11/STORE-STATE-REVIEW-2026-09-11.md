# Store state review and directory correction

Reviewed 0f969085/e7b34eaf. The original c326edc7 minimal-admission reproduction
now refuses with Historical admission record corrupt. Real producer/verifier
handoff passes 1/12; side-effect-free candidate, construction, explicit commit
and fresh awaiting-validation read are covered.

Independent directory repro wrote an admission outside the store by replacing
verifications/ with a symlink. Fixed locally: historical findings/verifications
directories must be real canonical children of the store. Parent creation is
fsynced. Reads reject malformed directory entries instead of filtering them
away. The dedicated control checks both reader and writer refusal and leaves
the external directory empty. No existing directory is repaired automatically.

Tests after correction: historical state 2/15, repair obligations 9/36, full
runner 128/606, actual Futon3c producer/verifier/store handoff 1/12; all pass.
Lint 0/0, parens OK, diff clean. Original source pins and live state preserved.

Remaining authority issue for integration: verified-admissions derives the
verification authority from the stored reference's parent directory. A record
cannot authorize its own read root. Use configured server authority, or retain
an immutable validated verification copy under the store's own authority, with
its original provenance. Do not widen containment to whatever path a record
names. Independent review must settle this before live consumption.

The claimed missing successor evidence is already substantially produced:
futon3c.wm.run4-terminal-evidence/read-terminal-evidence-bundle joins durable
admission/click/binding/projection/run record and exposes classification,
identities, captured digests, execution checkpoints, review and grounding.
Implement a read-only historical-successor adapter over that reader. Missing
bundles or unknown classification cannot resolve anything. Bind to the exact
historical verification and its actual execution attempt; refuse same-attempt,
foreign identity, failed review/build, corrupt/different source and ungrounded
results. Store resolution remains a separate immutable transition. Do not use
a supplied production-shaped boolean or task status as substitute evidence.

No live root, record, service, capacity or attempt was changed.
