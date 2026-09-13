# HEAD-buffer independent review — isolated acceptance

Reviewed `fbcc9721` / `2aa0b5e3`: all six current and committed pins match. Retained raw focused gates show 11 tests / 54 assertions, clean kondo/parens, and one deliberately failing assertion with exit 1. Passing gates were not rerun. Disclosed command-shape failures are retained as narrative; they are not counted as gate sensitivity evidence.

The descriptor is constructed from `head-r`'s byte buffer, the same read that supplied parsed HEAD and head-digest. Capture returns that immutable Base64/SHA descriptor without reopening HEAD. Recovery completes schema, chain and provenance validation before returning it. The genesis/non-genesis and defensive-view controls support this bounded behavior. The change does not make transaction/provenance arrays immutable, authenticate ownership, or establish complete-capture serialization.

Independent `7301f6d7` acceptance of lead `ba69fe7a` is consumed; both historical pins match. Corrected ledger projection remains required: exact indexed transaction resolution supplies six-field rows; the five-field capture index alone cannot.

Next bounded implementation prerequisite to the full adapter is pure deterministic complete-capture encoding/readback: HEAD descriptor and ordered chain/index, explicit encoding of each transaction/provenance buffer, immutable retained bytes and raw SHA. External completeness must bind that exact representation. This codec must refuse malformed or mismatched sources and label its output structural-only; it must not fabricate external completeness or call the filesystem-based retrospective resolver. Exact adapter joins and independent acceptance remain separate.

Source acceptance is limited to isolated HEAD-buffer retention. Production authority, installed-code identity, completeness/freshness, later-prior acquisition, runtime wiring, first-install fencing, historical commission 20588 and all full-certificate obligations remain open.
