# One-attempt cap — final independent review + integration report (象-2, 2026-10-31)

Dispatch: codex-11 priority2 bell (invoke-1793414052868-8579-a7748c85), M-war-machine.
Scope kept bounded: review/integrate the one-attempt cap, establish readiness for
ONE supported recovery of injury/adf04df0f31ab8727ac57d9c7f2496d8fa28598c. No
clicks, no ledger edits, no author contact, no unrelated defects touched.

## Task 1 — Final independent review of f358bfe5c: APPROVED unchanged

Reviewed exact diff `3853df7a3..f358bfe5c` (5 files: meta_repair_coordinator.clj,
meta_self_heal_production.clj, meta_repair_attempt_policy_test.clj,
meta_self_heal_production_test.clj, POLICY report) in worktree
`~/worktrees/futon2/futon2-cap-review` (detached at f358bfe5c).

Verified against the review criteria:

- **Caps always 1 in production.** `work-order-options` merges
  `coordinator/default-repair-policy` *over* opts (merge order makes the default
  win); `effect-ports` assoc's both default caps over ANY port source including
  an externally supplied `:meta-repair-ports` map, and returns nil (unchanged
  `:repair-ports` readiness defect) when no port source exists. Options
  3/nil/0/-1 and tampered in-port-map caps are inert by construction
  (`policy-cap` requires a positive integer; production never reads opts caps).
- **No grant mechanism.** `:policy/*-cap` is read only at the coordinator
  boundary (test seam); grep confirms no other production reader.
- **Historical ledger untouched.** No changes to the ledger ns, no attempt-limit
  edit; a focused test asserts the raw limit stays 3.
- **Spent accounting.** `spent-author-attempts` counts `:claimed` and later
  (`:authored :reviewed :enacted :discharged :abandoned`); `:reopened` records
  the NEXT fresh attempt and is excluded — verified in the state set.
  Persisted (sealed ledger entries), so attempts ≤ 1 and retry children ≤ 1
  survive restarts and repeated clicks.
- **Honest crash window.** A crash between author-port invocation and receipt
  persistence can re-dispatch the SAME attempt on resume; this is attempts ≤ 1,
  NOT exactly-once contact. Disclosed in the POLICY report (unresolved #2) and
  restated here; no crash redesign done or requested.
- **Tests hermetic and narrow.** Ran exactly the two focused namespaces in the
  detached worktree: **43 tests / 262 assertions, 0 failures, 0 errors**
  (matches the POLICY report's claim). No broad suite, no serving tests, no
  reload inside fixtures. Raw log pinned locally (see Raw pins).

Predecessor notes read: zai-5's one-attempt-cap-review-2026-10-31.md (the 8542
successor commit resolves its only material finding, the unauthenticated
override — confirmed removed at both production arming sites),
adf04df0-routeB-preparation.md, and BLOCKER-no-start-one-attempt-bound.md.

## Task 2 — Integration (merge + serving reload)

- **Merge:** canonical futon2 (`/home/joe/code/futon2`, main was `0ff3e95ec`)
  merged exact `f358bfe5cc3ea919aa1ae5cf2e59765cefc3a409` with
  `git -C … merge --no-ff` → merge commit **`5197d9f6c`**, pushed to
  origin/main. No conflicts; the only concurrent push during integration was
  zai-6's docs-only `e3b4fbf0c` (17-line review note, preserved; local main
  fast-forwarded). Canonical main HEAD is now `e3b4fbf0c`, whose src tree is
  byte-identical to the merged cap.
- **Reload:** pre-reload idle predicate held — `/api/alpha/wm/click`
  `running? false`, medic-authority provider atom `{generation 0, provider nil}`.
  Both changed production namespaces reloaded from canonical
  (`/home/joe/code/futon2/src`) **under the actual
  `futon3c.wm.runner-service/acceptance-lock`**, in dependency order
  (`futon2.aif.meta-repair-coordinator` then `futon2.aif.meta-self-heal-production`)
  via `(require ns :reload)`. Neither namespace has defonce/deftype/defrecord/
  defmulti state (grep-verified pre-reload), so reload preserves all runtime
  state. Post-reload live identity: `default-author-attempt-cap = 1`,
  `default-retry-child-cap = 1` read from the serving JVM. No test vars or
  override providers installed.
- **Reload recorded:** futon3c campaign condition appended by the supported
  loader record and committed `1a4ffbc66` (git add -f of the tracked conditions
  file, per the C-20 precedent), pushed to origin/master.
- **Protected bytes after integration (all match pins):**
  ledger `ca73914f0c26fb1435a0497398a191302c46da705dc09da7724b4f4998371a44`
  (130 entries, unchanged) · consumption `3574077d8846e15718d429d57d96993cb014aed01e871b66b42624d893ce8b3a`
  · outer-state `cfeaa0e6f9894417c25d6e42d6b0e78485ce31b2779cb92df4871ef730085365`
  · outer-makeup `f312583522597c49b4134b08c4a186a013baaaa12a6f1c332fe9875e57f58e3a`.
- **Loaders validate live (serving JVM, standing verifier):** plain
  `load-durable-ledger` `:loaded` 130 entries 0 defects; with standing verifier
  `:loaded` 130 entries 0 defects; `effective-active-injuries` `:computed` with
  exactly one id — `injury/adf04df0f31ab8727ac57d9c7f2496d8fa28598c`.
- **Armed-cap dryrun (real bound inputs only):** `author-attempt-policy` under
  `default-repair-policy` on the live ledger → `{:status :allowed :cap 1
  :attempts-spent []}`; armed `prepare` with only the injury-id refuses
  `[:work-order/invalid]` (missing real diagnosis/acceptance inputs — exactly
  the routeB dryrun result) and crucially does NOT carry the
  `:policy/author-attempt-cap` defect: **attempt 1 is available under the
  armed cap**. No dummy evidence was supplied or called admissible.
- **Budget fresh:** 243 allocated / 229 consumed / **14 available** (unchanged;
  consumption ledger sha matches pin).

## Task 3 — Execution-ready next step (for coordinator dispatch; NOT executed here)

Supported next ordinary command (one parent click, default opts, no special
preparation — adf04df0 is the only effective active injury, so the click enters
the dedicated meta-self-heal route automatically):

```
cd /home/joe/code/futon2 && scripts/wm_click.sh --run --issuing-caller codex-11
```

- **Cost:** exactly 1 parent ordinary credit, **14 → 13**; the meta-retry child
  is credit-free (`retry-child-opts` drops `:ordinary-click/issue!`).
- **Bound evidence:** with the caps now armed in the serving JVM, sealed-ledger
  accounting makes a second author attempt (`:coordinator/policy-author-attempt-cap`
  typed refusal at both prepare and the claim boundary) and a second retry child
  (`:coordinator/policy-retry-child-cap` typed hold before any new child id is
  minted) impossible — **except the disclosed crash window**: a crash after the
  author port is invoked but before its receipt persists may re-dispatch the
  SAME attempt on resume (attempts ≤ 1; not exactly-once contact).
- **No live POST/repair author contact was made in this packet.** Per dispatch,
  the coordinator dispatches the one bounded attempt only after this
  integration receipt.

## Remaining blockers / limits (stated plainly)

1. Crash-window re-dispatch of the same attempt (pre-existing recovery
   semantics; not redesigned).
2. Lean model does not yet anchor the two new typed stop paths (future Lean
   packet; runtime policy is narrower than the ledger's 3 and never reports
   ledger exhaustion).
3. Production suite pre-existing baseline 12 failures / 1 error (identical on
   main, parent and branch per zai-5; separate packet, not touched here).

## Raw pins (local)

- Focused test log:
  `/home/joe/notes/wm-next/criteria-completeness/run001/adf04df0-single-repair/cap-integration-xiang2/focused-tests.log`
- Review worktree: `~/worktrees/futon2/futon2-cap-review` @ f358bfe5c (detached)
- Reload/verification eval forms: `/tmp/reload-cap2.clj`, `/tmp/verify-post2.clj`,
  `/tmp/verify-post3.clj` (evaluated via futon3c `scripts/proof-eval.sh`)
