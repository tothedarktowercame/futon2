# C173 — pinned operational certificate in the workspace gate

Date: 2026-08-31.

The workspace gate now runs `wm_operational_certificate.clj` over the committed
C167 v20 fixtures, pinned by content rather than date or “latest” selection:

- `tick-run-record-2026-08-31.edn` SHA-256
  `3d4432d09934517811cda1b1b35d7a5a9c1bbc73f137d76f4aecb30f6ab07875`;
- `C167-v20-certificate-resource.edn` SHA-256
  `9c9e566a9d5460ec0bbadf59c58b10627e6434a52974ffd62480dc554e4cfdea`.

The positive certifies that the machinery works on that known run/resource
pair. The gate control changes `:run/id` in an otherwise parseable run record;
the run content pin must reject it. This is a gate-level falsifier, not merely
an isolated checker test.

The exclusion is narrowed, not erased: `current-live-operational-certificate`
remains manual because it requires a newly operator-triggered run and its
resource receipt. The pinned fixture does not claim to certify “today's” or
the latest run. `lane-registry` remains manual because it measures dispatcher
discipline rather than repository validity. Both reasons are emitted in the
gate summary.
