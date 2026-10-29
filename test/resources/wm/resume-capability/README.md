# Frozen resume capability witness

Run: 2026-10-29-43ec20a7-c3ba-4726-a43c-a827b2b3e66d.
Canonical run record SHA256: 7f117d5b702d1fef59ee5e32992a639ac8655d8c0d3ea2ce72969df3e43d5d26.

- checkpoint.edn: EDN round-trip of data/wm-on-prod-history/checkpoints/<run>/attempt-002.edn.
- decision.edn: :decision projection of that run record. The runner witness restores its action from the retained checkpoint, not the pre-redecision per-policy-argmax action (those have different digests).
- author-source.clj: exact fdba6f19db389db788e8569bc7c037481c099687 source, SHA256 c7339cca4c123fc7fe3994541db206917e668b774b20d38f48d9cd522ef11498. Tests require the same authored namespace already committed in src.

The negative arm uses these clauses unchanged. The positive arm changes only each clause's :capability to :wm/pattern-apply. It is an explicitly labelled constructive modification, not a claim that the author supplied it. Both arms use the real compiler/resume and runner, with hermetic stores, frozen selection, and no external contacts. The construction arm stops intentionally at construction before dispatch.
