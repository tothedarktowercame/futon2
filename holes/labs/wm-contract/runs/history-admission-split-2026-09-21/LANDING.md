# History exclusion and identity admission split

Author: codex-8. Requesting owner: claude-12.
Authorization: invoke-1789962543149-22860-0c2a2ba9.
Repair finding: repair-occ-f9f4e9708737cb15163f92d0d31419aae2097c5095ffdf6b5c4a3dc08d7057ba.
Agency-job independent review is PENDING; no implementation registration,
resolution, or other repair-store closing is performed in this landing.

A malformed historical selection used to throw from attempt-events through
ledger and resolve-lineage!, before run-opportunity!'s catch. Historical scans
now return readable events and typed exclusions naming the attempt, absolute
file path, reason, and string-valued reader error. The next time-step record
retains the exclusions, and resolved-lineage diagnostics are explicitly copied
into the runner's execution-cohort run record.

Time-step identity is different: missing, malformed, or unreadable authoritative
001-time-step.edn refuses admission with :history-identity-unavailable,
:cannot-prove-not-duplicate, and the blocking path. Lineage resolution returns
that diagnostic without advancing the cohort. Execution preflight re-reads the
identity and raises the typed refusal inside the runner's recorded boundary.
start-attempt! repeats the identity check under the cohort lock, before duplicate
checking or creating a directory. No duplicate admission is inferred from a
missing identity.

The binding's exact four-field validator is unchanged. Discovery diagnostics
are separate Clojure map metadata, accessible via lineage-history and explicitly
materialized into persisted run records. They do not authorize admission;
preflight and the locked writer re-read the actual identity. Direct bindings
without metadata receive the same check, and identity errors are additionally
persisted at the run record's top-level :history-admission-refusal.

Checkpoint append/close readers remain strict. receipt-construction's separate
history-read! and previous-construction validation are unchanged: excluding
unknown evidence is not proof of no previous construction. The bad artifact
remains in quarantine, unchanged; tests copy it into scratch roots.

## Acceptance

1. Quarantined poison copied into a scratch cohort: production resolve-lineage!
   and run-opportunity! proceed past initialization and start attempt-002. The
   fixture then intentionally stops at agent-unavailable; no agent dispatch.
2. The readable original time-step is consumed unchanged. Damaged attempts still
   count in the cohort denominator; the test observes two attempts after the
   proceeding run. Existing readable-history and cohort tests also pass.
3. The typed exclusion round-trips via read-edn both in the next time-step and
   the production tick run record. Its reader error is a string, not bad EDN.
4. Unchanged full-loop-runner namespace: results in precommit/postcommit receipts.
5. Before corruption, a repeated scheduler opportunity is refused as duplicate.
   After corrupting its time-step, repeating it is refused with the typed
   identity reason and exact file path. No second attempt is created. The
   production runner returns :incomplete and persists the refusal instead of
   throwing before a run record can be written. Missing and readable-but-invalid
   identities also refuse through preflight without relying on lineage metadata.
6. Non-identity poison is explicitly the proceeding side of the same test pair;
   its exclusion names the selection file, with :history/status :excluded.

The two early history test runs failed because the initial diagnostics were
placed in the four-field binding itself. The binding validator caught that;
its contract was preserved, and the final implementation separates diagnostics
as described above. Intermediate logs (including pre-history-final.log, before the additional
identity tests) are retained, not used as warrants. The final precommit runs
are the three pre-*-gated.log files.

## Validation and deployment

Fresh tooling JVMs only. clj-kondo and check-parens logs accompany this receipt.
Exact file hashes, test counts, roles, implementation commit, postcommit warrant
IDs, and checks are recorded in the JSON/EDN receipts beside this file.
No live tick, shared-JVM load, or actuation was performed.
COMMITTED IS NOT LOADED. The serving JVM needs the authorized canonical reload
before a validation click exercises this repair.
