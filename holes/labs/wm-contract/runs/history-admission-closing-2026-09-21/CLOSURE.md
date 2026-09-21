# History admission repair: reviewed closure and follow-up

Repair: repair-occ-f9f4e9708737cb15163f92d0d31419aae2097c5095ffdf6b5c4a3dc08d7057ba.
Replacement commit: 707e680f8ef28dbdc40d4f4741a3ec14d75fb9e3.
Follow-up: 7fef1200fed947749fa8169f4316431e719232b6.
Author: codex-8. Requesting owner: claude-12. Reviewer: claude-2.
Review job: invoke-1789963375357-22864-360c27c2, fetched from Agency, done,
APPROVE, independent-review-evidence valid, 10 executed tool events.

The follow-up makes opened-opportunity-ids read only 001-time-step.edn, matching
identity-refusal. Its regression pins a readable time-step-shaped payload in a
non-identity file: it contributes no opportunity identity. The authoritative
file still does. No validator or store gate was weakened.

## Validation

- Kondo and parens: pass, including the executed closing script.
- Cohort pre/post: 30 tests / 170 assertions, zero failures/errors.
- Admission pre/post: 3 / 31, zero failures/errors.
- Runner canonical and pinned: 180 / 998, zero failures/errors.

The canonical runner registration refused :scope-not-committed, naming
scripts/futon2/report/war_machine.clj and src/futon2/aif/cascade_proposals.clj,
which a concurrent lane was editing. Its passing test result and refusal are
retained in registration-runner.edn. A subsequent canonical admission warrant
check refused :environment-mismatch; retained in canonical-checks.json.

Fresh admission and runner registrations therefore use the clean committed
checkout /home/joe/code/futon2-history-closing-tooling at 7fef1200. Both warrants
check valid there; the cohort warrant checks valid on canonical. These scopes
are recorded explicitly in postcommit.json and warrant-checks.json. The pinned
warrants do not attest to later concurrent source changes on canonical.

## Store writes

Fresh tooling JVM, committed API code from the same pinned checkout; default
store root verified as /home/joe/code/futon2/data/wm-repair-obligations.
record-implementation! and resolve! both accepted; no store refusal occurred.
Both records use the repair id above, under implementations/ and resolutions/.
Their actual paths and hashes are in store-records.json; complete API output
and T8 snapshots are in store-closing.log.

Implementation attempt: invoke-1789962543149-22860-0c2a2ba9 (actual author job).
Validation attempt: 8decba32-5014-4e07-9bf2-b7c23ef17c18 (distinct registered
production-path admission run). Replacement commit remains 707e680f.
Review evidence was computed from the fetched job, and artifact binding from
fresh-artifact-binding against the actual author job and retained precommit
receipt. No observer injection or manufactured review verdict was used.

Witness scope is scratch-history production initialization, not a live click.
The original six-case acceptance is supported by the retained run records and
tests. Reviewer probes: six real exclusion round-trips passed; two inputs read
as valid keywords and are explicitly NOT TESTED for exclusion. The reviewer's
133-directory read-only sweep found zero existing identity refusals.

T8 with closed repair ids: no violations before or after this closure. This
finding transitions open -> awaiting-validation -> resolved. Other unrelated
initialization findings remain; no claim that the entire repair store is closed.

## Packet (a) and deployment

All five packet-(a) warrants were freshly re-registered on canonical in
da821ca3. Receipt: ../nonempty-cascades-2026-09-21/LANDING.md and its
canonical-reregistration/receipt.json. That record pins its then-current source;
subsequent source changes must be evaluated against their own warrant scopes.

No live reload or validation click was performed. COMMITTED IS NOT LOADED.
The quarantine artifact is unchanged. This receipt and all generated artifacts
are committed; the final git status is checked separately after the commit.
