# adf04df0 delivery-failure evidence — recovery and prevention report

Branch `retention/delivery-failure-evidence` (isolated worktree, base
origin/main `b2dd77218`). No click, no ledger write, no reload, no merge,
no diagnosis contact. Canonical inputs read-only.

## 0. Efficiency steering (2026-10-31, Joe via codex-11): how this packet meets it

This completes an existing obligation under an efficiency constraint —
durable replay-input retention as a cheap debugging witness, not a platform.

- **Reuse, not a new platform.** The retention module (239 lines) follows
  the existing futon2 per-seam evidence pattern (`task_execution_evidence`,
  `interpretation_evidence`, `repair_discharge_evidence`, …) and reuses
  `load-identity/sha256` for content addressing; no new storage service,
  no generalized replay engine. The seam itself is registered in the ONE
  existing unified handoff index and audited by the ONE existing audit.
  Nothing of a run is reconstructed: the retained record is exactly the
  receiver's check input (the author answer's requisition/cascade/patch
  fields), the typed result, the linkage pins (job id, tick-record
  sha-pinned source record) — no reply replay, no click simulation.
- **One documented command** (isolated process, no sessions, no notes, no
  live effects, read-only on every input; only writes are two NEW
  content-addressed files + this report in a worktree-local dir,
  overridable via `ADF04DF0_EVIDENCE_ROOT` / `FUTON3C_INVOKE_JOURNAL`):

      ./scripts/recover-adf04df0-evidence.sh

  It verifies every pin (tick-record sha, blob digest at the resolved
  commit, ancestry of canonical HEAD, journal linkage) and reproduces the
  delivery-check result on the exact retained input.
- **Historical vs fixed classifier, explicitly separated** in the retained
  record's `:typed-result :replay` map: `:historical-classifier` is the
  pre-`c96bd9871` mislabel (`:commit-not-on-canonical-head` under
  `:author-delivery-wrong-repository`), recorded from the repair finding,
  NOT re-executed; `:fixed-classifier-now` is the current
  `check-delivery` actually re-run in the fresh process (typed
  `:abbreviated-commit-descriptor`, no git access — the descriptor
  refusal precedes the location scan).
- **Measured from actual execution** (fresh JVM, 2026-10-31): wall clock
  ~61 s (dominated by JVM + deps startup, not the recovery logic);
  max RSS ~430 MB (JVM baseline); artifacts 1,553 B (retained record) +
  1,045 B (proposal) ≈ **2.6 KB total**. Setup dependencies: futon2
  checkout with Clojure CLI/deps, read access to the Agency invoke-jobs
  journal and the canonical futon2 git repo (read-only `rev-parse`,
  `merge-base --is-ancestor`, `show`). No network, no Agency, no ledger.
- **Regression for the future** (not a static row): the seam test drives
  the real handler's delivery-error branch and fails if a future delivery
  failure again refuses without retaining a usable input.

## 1. Recovery (fresh process, verified)

`scripts/recover-adf04df0-evidence.sh` → `scripts/recover_adf04df0_evidence.clj`
reads the Agency invoke-jobs journal (under /tmp by default, so the command is not repeatable after a reboot; the record committed beside this report is), verifies every linkage step
typed, and retains the exact failing check input content-addressed:

- **Retained pin** `6601c31e45e9d29e747ea645ed9383f7c2ad0db6b074044837e91b3c73faa739`
  in `data/wm-delivery-failure-evidence-recovery/` (worktree-local; canonical
  data tree untouched; root overridable via `ADF04DF0_EVIDENCE_ROOT`).
- **Readiness**: source record `:present` at pinned sha `ba359b5e…`;
  retained input `:usable`; limits `:readiness-is-not-diagnosis`,
  `:carrier-admission-requires-review`.
- **Supplemental carrier proposal** written alongside; admission is
  review-required and does NOT bypass the cause-entry contract
  (`:cause-scope/input-pin-not-source-record` named explicitly).

### Linkage chain (all asserted in the script)

injury ledger ordinal 130 (`injury/adf04df0…`) → cause-record-pin = tick
record (sha `ba359b5e…`, asserted byte-identical) → tick record contains
job `invoke-1793409156227-8523-78848068` and the selected canonical cascade
`fcbc11a5…` → journal job 8523 (wm-author, caller wm-full-loop) final
`:result-text` → fenced answer JSON `{requisition 6a8fe374…, cascade-digest
5a0fa2ade…, patch.commit "8561ecdc7"}` → `rev-parse` resolves the
abbreviation to `8561ecdc7666…` (ancestor of canonical HEAD, blob digest
matches `c049abef…` at `src/futon2/aif/wm/authored/req_6a8fe374.clj`) →
repair finding `repair-ea1-19929445…--attempt-002` independently pins the
job, `req-6a8fe374`, cascade `5a0fa2ade…`, and the abbreviated + full commit.

### Correction to the dispatch's stated linkage

The dispatch (job 8669) named job 8523 — **correct**. Mid-session I
suspected a mismatch because the tick record's cascade-digest is `fcbc11a5…`
not the answer's `5a0fa2ade…`. That is not a mismatch: the two digests are
different levels. `fcbc11a5…` is the **selected canonical cascade**
(selection-certificate level, in the tick record); `5a0fa2ade…` is the
**requisition-level** cascade in the author answer, pinned by the repair
finding. The script asserts both links and asserts the answer's digest is
NOT the tick record's selected cascade, so the two levels cannot be
conflated silently.

## 2. Prevention (reuse of the unified handoff checker — no parallel infra)

- `resources/wm/handoff-sites.edn`: one new row
  `:execution/open-cascade-delivery-check` (producer :open-cascade-author-answer,
  consumer :patch-delivery-check) with `:decoder
  :cascade-patch-delivery/check-delivery` **and** `:retention
  :wm/delivery-failure-evidence-v1`. Response-format validation (decoder) and
  durable replay-input sufficiency (retention) are documented in-row as
  distinct obligations at one seam.
- `handoff_site_index.clj` audit: new typed problem
  `:retention-schema-invalid` (declared retention must be a schema keyword,
  same shape rule as `:contract`).
- Stale rows repaired so the ONE audit is green again (it was red at base):
  `:execution/open-cascade-author` anchor → `(handoff/contact-with-format-correction`
  and `:execution/open-cascade-reviewer` anchor → `reviewed (when reviewer-response`
  (runtime was refactored in a8c12af10 without updating the index — exactly
  the anchor-drift class Joe flagged). `ordinary_author_refusal.clj` excluded
  from the response-map caller check with an in-test justification (it
  re-validates a retained job with the shared reader as a predicate; it
  consumes no live handoff).
- Regression that makes the omission visible dynamically
  (`test/futon2/aif/wm/delivery_seam_retention_test.clj`): (a) the index
  declaration must match `dfe/schema-version` and the audit must verify;
  (b) the REAL `open-cascade-resume-handler` delivery-error branch, driven
  with an adf04df0-shaped answer, must yield the typed refusal AND a
  retained, content-addressed record whose exact abbreviated descriptor and
  cascade survive, usable via `evidence-readiness`. Before the retention
  wiring, (b) fails on `:delivery-evidence` missing — the silent loss the
  injury recorded, now a red test rather than a ledger entry.

### Explicitly unsupported capability (documented, not worked around)

The meta-self-heal diagnosis **producer** invoke lives in futon3c
(`futon3c/wm/meta-self-heal-adapters` `producer-invoke!`), while
`handoff-site-index` audits `:source` anchors relative to the futon2
checkout root; a cross-repo row would always audit `:source-missing`. Until
the index grows a cross-repo convention, that seam is covered by the wrapper
fidelity fix + test in `meta_self_heal_production` (`:kind`/`:payload`
reason propagation), not by an index row.

## 3. Tests

- `futon2.aif.wm.delivery-failure-evidence-test` — 6 tests green.
- `futon2.aif.wm.delivery-seam-retention-test` — 2 tests green (new).
- `futon2.aif.handoff-site-index-test` — green (was failing at base; count
  updated 19 → 20).
- `futon2.aif.meta-self-heal-production-test` — wrapper fidelity test added;
  pre-existing baseline failures unchanged.
- Recovery script: fresh JVM, no session or notes dependency (it does read the /tmp journal), idempotent
  (re-run re-retains at the same content address).

## 4. Open decisions for review

1. Supplemental-carrier contract change (cause-entry requires
   `input.record-pin == source-record-pin`) is proposed, NOT applied —
   admission review-required.
2. The tick/answer two-digest-level reading above wants reviewer
   confirmation before any diagnosis retries on it.
3. Whether the worktree-local recovery store should later be admitted into
   the canonical `data/wm-delivery-failure-evidence/` root is a review
   decision; this branch does not write canonical data.
