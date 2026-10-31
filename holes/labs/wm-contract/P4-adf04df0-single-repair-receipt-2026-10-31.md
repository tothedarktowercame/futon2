# P4 receipt — one bounded release for injury/adf04df0 — REFUSED at diagnosis derivation (honest held state)

Executor: zai-4, dispatch codex-11 (8619), 2026-10-31. PLAN-criteria-completion P4.
Outcome: **one parent click issued and charged (243/229/14 → 243/230/13); the
dedicated meta-self-heal route typed-refused before any author dispatch; the
repair attempt stays unspent. No retry issued. No attempt 2, no second parent,
no child.** Stopped per dispatch rule (precharge/refusal → exact cause, no retry).

## Pre-issuance conditions (all cleared before launch)

1. **Test-side-effect reconciliation (Kimi 8579/8590).** The broad-suite event
   printed `fold-turn QUARANTINED .../futon6/data/fold-turns/ft-war-machine-pilot-001.edn`.
   Filesystem proof (read-only, no restoration needed):
   `ft-war-machine-pilot-001.edn` still at its original path, **15584 bytes,
   mtime 2026-07-13 18:07:21 UTC**, sha256
   `f6a6dffac6d75d28199698cafd05b95e69a3fb20c6858dfc8affda6e2472c815` —
   predates job 8579 (2026-10-31) by months. `data/quarantine/` and
   `data/fold-turns-quarantine/` untouched since 2026-06-14 / 2026-07-13.
   The QUARANTINED verdict is an in-memory classification printed to *err*
   (prompt-reconstruction gap in the Jul 13 deposit), not a file mutation —
   consistent with 象-2's supplemental job 8590 findings.
   **Incident wording correction (codex-11 8624):** the 8590 main report's
   "No broad suite" claim is FALSE — job 8579 event39 did accidentally run the
   broad suite; the source no-write proof (`fold_escrow.clj` QUARANTINED arm
   prints to stderr + in-memory map only) resolves THIS quarantine concern,
   and is not a claim that arbitrary broad-suite effects are impossible.
   Kimi's report doc must carry that correction; it is not an execution
   blocker. Result record:
   `GET /api/alpha/invoke/jobs/invoke-1793414500779-8590-a96d8e4d`.
2. **Fresh supported readiness (serving JVM, standing verifier — not a proxy).**
   Ledger `:loaded`, 130 entries, 0 defects; effective active injuries exactly
   `injury/adf04df0f31ab8727ac57d9c7f2496d8fa28598c`; `spent-author-attempts = []`;
   policy `{:status :allowed :cap 1 :attempts-spent []}`; live serving caps
   author 1 / retry-child 1; running? false; budget 243/229/14. Real route
   composition witnessed on the serving JVM: `configure-self-heal-ports`
   → concrete ports, `self-heal-serving-readiness {:status :ready :defects []}`,
   all ten adapter port fns present (agency-invoke, enact, retry, diagnosis,
   acceptance, acceptance-review, acceptance-run, retry-click-id,
   integration-equivalent, retry-click-status), real cast wm-author /
   wm-reviewer / wm-repair-reviewer with fresh session identities and
   target-worktree-root `/home/joe/worktrees/futon2/self-heal`; canonical base
   727db74ab. No dummy seals or test redefs.
3. **Canonical state.** futon2 main HEAD `727db74ab`, tracked-clean (only
   untracked evidence/ and holes/labs additions); no src changes since cap
   merge 5197d9f6c (diff empty); four protected hashes pinned before launch
   (see protected-before.txt): ledger `ca73914f…1a44`, consumption
   `3574077d…ce8b3a`, outer-state `cfeaa0e6…365`, outer-makeup
   `f3125835…e3a`. Heap ~9.3 GB of 251 GB host; serving runner code current.

## Execution

Launcher: `WM_CLICK_POST_TIMEOUT_S=300 scripts/wm_click.sh --run --issuing-caller
codex-11 --ask-seats zai-1,zai-2,zai-3`, detached, no outer kill (log:
`run001/adf04df0-single-repair/p4/launcher.log`). Click
`wm-click-0930ccf1-264e-4ff1-bc5d-8f5deb2e4d9b`, started 03:08:45Z, charged
one parent credit (consumption.jsonl +1, caller codex-11, new sha
`c7b9d4f3a683fee50908279eecdab7db22ebd16dd7b9ed25decf35ef9fd883ed`).

**Terminal refusal, exact cause chain:**
- runtime diagnosis observation: **`:cause-scope/predicate-not-result-derived`**
  — the live meta-diagnosis producer refuses to derive a diagnosis from the
  injury's cause record (adf04df0's author-delivery mislabel carries no
  result-derived cause-scope predicate). Full abbreviated request retained in
  the click result.
- acceptance observation: `:unavailable :meta-self-heal/acceptance-diagnosis-unavailable`
  (diagnosis refused ⇒ no acceptance can be proposed).
- dedicated route: `runtime-disposition {:status :refused :reason
  :meta-self-heal/production-config-required :absent-terms [:diagnosis-evidence
  :acceptance-test]}` — the work order cannot be prepared without a diagnosis
  evidence pin and acceptance test, and none can be manufactured.
- `ordinary-selection-called? false`; no run record; binding absent; the
  launcher's own ownership alert (`expected run=2026-10-31-b8eb1b6a… actual
  run=None`) is the refused-launch shape, not a corruption.

**After state (verified live):** ledger `:loaded` 130 entries sha `ca73914f…1a44`
**unchanged**; effective injury set exactly adf04df0; `spent []`; policy allowed
cap 1; outer-state/outer-makeup hashes unchanged. No author contact, no review,
no enact, no retry child — the cap and the whole repair pipeline were never
reached. Budget 243/230/**13**.

## Frozen criterion observations (repair task ≠ original task)

- **A1:** one charge for one admitted parent (14→13), refusal explicitly
  recorded on the click result; no refused-launch double charge. Evidence:
  consumption.jsonl tail + click result.
- **A2:** serving envelope witnessed pre-launch (identities, ports, heap, idle).
- **A3:** authority-aware effective set exactly adf04df0; per-run disposition:
  the pre-existing cause-scope limitation (below) is the blocker, surfaced.
- **C1:** no execution identity beyond click id (no run record — refusal
  preceded allocation); first divergence = diagnosis derivation.
- **C2:** durable baseline pinned before dispatch (protected-before.txt);
  exactly-once external contact NOT claimed (no contact occurred at all).
- **C3/C4:** not reached — no author artifact, no review (honest: not "no
  evidence", but "no dispatch").
- **D3:** ledger and click result agree on refusal; no history rewrite.
- **D4:** parent/child accounting: 1 parent charged, 0 children, 0 attempts
  spent; budget 243/230/13.
- **E1:** click ~seconds to typed terminal refusal; safe refusal is not useful
  completion — P4 remains open, honestly held.
- **E4:** budgets/ledger/jobs conserved (ledger + state hashes unchanged);
  no corrective intervention was performed inside the run.

## Natural P3 witness (not manufactured)

The refusal is itself the naturally-occurring work-order option projection
witness: production prepare projected absent terms `[:diagnosis-evidence
:acceptance-test]` from real runtime diagnosis refusal — the option/identity
plumbing is live and honest (refuses rather than improvising inputs).

## Blocker for any future P4 re-release (needs new dispatch, not this packet)

`cause-scope/predicate-not-result-derived`: the injury's cause record lacks a
result-derived cause-scope predicate, so the runtime diagnosis producer cannot
legitimately derive a diagnosis. Fixing that is a source change to the
diagnosis/cause-scope contract (owner: zai-4 implementation, zai-5 review per
standing rule). No such change was made here.
