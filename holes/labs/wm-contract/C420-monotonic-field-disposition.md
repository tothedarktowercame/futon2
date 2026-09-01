# C420 — systemd monotonic start is not a freshness predicate

Date: 2026-09-01.

`ExecMainStartTimestampMonotonic` has been removed from the quiet-run
acceptance conditions. The prior check required only positive decimal text;
values `1` and `999999999999999999` were equivalent, so it constrained no
event relationship.

The field is boot-relative. The writer-fence observation records wall-clock
instants but no boot identity or monotonic sample. Consequently the state
machine cannot honestly compare the two, order fence and gate on the monotonic
axis, or detect a restart between them. Retaining a presence check would make a
decorative field look like evidence.

Freshness continues to rest on the predicate that actually fires:
machine-measured ingestion time minus the producer-recorded fence finish must
be between zero and 300 seconds. The attestation must also remain valid through
gate finish. Existing stale-evidence controls exercise that refusal.

A new control removes `ExecMainStartTimestampMonotonic` entirely from all
three producer records and demonstrates that a fresh, otherwise valid tested
phase passes. This proves the removed field is not silently part of the claim;
the separate day-old-fence control still refuses.
