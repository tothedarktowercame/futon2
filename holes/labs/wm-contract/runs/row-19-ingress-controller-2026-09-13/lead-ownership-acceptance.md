# Independent store ownership review and lead touch-up

Reviewed changes c5dd3103 through be83071d. Two original ownership pins match historical d2e362fb bytes; subsequent release/retry commits and retained final 5fbc62cb raw gates establish 10 tests/36 assertions, clean kondo and explicit parens. Same-JVM and separate-process exclusion, explicit release/refusal, no-overwrite initialization and non-EDN refusal now cover the prior lead counterexamples at the controller entrypoint.

One small source gap remained: projection validation on recovery/persistence did not enforce the map/requested-job-id relation enforced by new intake. Lead source commit 232df039 applies it to the common projection validator. A regression proves mismatched payload identity refuses before publication and the previously empty store remains readable. Changed-source gates passed 11 tests/38 assertions, zero failures/errors, kondo zero warnings/errors, explicit parens OK. Full argv, exit codes, source hashes and raw stdout/stderr are retained in lead-projection-gates.json and associated files.

Accepted as isolated controller machinery, subject to independent review of lead touch-up. The file adapter is a trusted implementation detail; controller lifetime ownership is mandatory. No serving lifecycle reconciliation, HTTP wiring, loopback control listener, external first-installation fence, restart readiness, deployment or admission follows.

Next bounded packet independently reviews 232df039, then implements offline lifecycle reconciliation against independently resolved hot-ledger, durable queue and delivery snapshots. Missing evidence must not be interpreted as drained. No live HTTP wiring until reconciliation is reviewed; preserve all accepted work and stable resume identities.
