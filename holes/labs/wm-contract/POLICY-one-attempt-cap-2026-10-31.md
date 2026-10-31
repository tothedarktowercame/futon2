# One-author-attempt / one-retry-child operating caps — review packet

Branch `policy/one-author-attempt-cap` (futon2), prepared by zai-4 on dispatch
codex-11 (8538), after blocker receipt
`run001/adf04df0-single-repair/BLOCKER-no-start-one-attempt-bound.md`.
**Isolated packet: no merge, no serving reload, no click, no ledger mutation,
no author contact.**

Update (2026-10-31, codex-11 dispatch 8542): zai-5's independent review
(one-attempt-cap-review-2026-10-31.md) approved `1bd7428d2` except the
unauthorized production override. Successor commit (this commit, atop
`1bd7428d2`) removes the opts override at BOTH production arming sites,
unconditional default caps of 1, and adds tampering/coordinator-seam tests.
See "Enforcement wiring", "Tests" and unresolved 4–5 below for the corrected
truth: **no production override or grant is supported.**

## Policy authority

Joe's binding rule: **one self-heal attempt per injury** (not the raw ledger's
3). Explicit coordinator ruling (codex-11 8538): implement bounded enforcement;
do not edit historical ledger limits; no 3-attempt execution. The caps:

- **one author/review/enact attempt per injury** — `author-attempt-policy`,
  enforced at `prepare` (fresh and continued preparation, i.e. the parent
  author-failed continuation loop and all later clicks) and at `coordinate`'s
  claim boundary (a resumed transaction continues its own claimed attempt; no
  NEW attempt may be claimed once the cap is spent).
- **at most one retry child per enacted attempt** — `retry-child-policy`,
  enforced in `coordinate-queued-retry` before any new child id is minted or
  dispatched.

Both decisions are computed from **sealed ledger transitions only**
(`spent-author-attempts` counts `:claimed` and later states;
`recorded-retry-child-clicks` counts distinct `:retry-click`s at the current
attempt), so the bound survives restarts and repeated clicks; a per-click
counter would not. A `:reopened` entry records the NEXT (fresh) attempt and is
deliberately not counted as spent.

## Truthful stop, not ledger exhaustion

The stops carry their own typed reasons — `:coordinator/policy-author-attempt-cap`
(preparation defect `:policy/author-attempt-cap`; the production result retains
the counted evidence under `[:prepare :policy-attempt]`) and
`:coordinator/policy-retry-child-cap` (a `:held` result with
`:retry-child-policy`). Neither reports `:attempt-limit-exhausted` or
`:retry-budget-spent`, and neither writes an abandonment entry: the ledger
keeps its own state (`[:reopened 2 :author-failed]` in the author-failure
witness). No manual discharge; no ordinary fallback behind an active injury
(the dedicated route still consumes the opportunity); child-retry arbitration
skip (`(not retry)` gating) unchanged.

## Lean relation (checked, not imported)

`mathlib4 DarkTower.WarMachine/SelfHealRoute.lean`:
- `AbandonReason.attemptLimitExhausted` (L:1835): "`:attempt-limit-exhausted`:
  the next authoring would exceed `:attempt-limit` … `validate-ledger`'s
  abandonment check accepts it only at `attempt = limit`." The Lean model
  anchors the LEDGER's own exhaustion semantics; it does not fix any limit
  value as mandatory, and the attempt-limit is per-injury ledger data.
- `AbandonReason.retryBudgetSpent` (L:1843): the requeue-budget arm
  `(L append-requeue :retry-budget-spent)` when the requeue count reaches the
  ledger's retry limit; "a re-queue authors nothing, so it does not count
  against `:attempt-limit`."

Conclusion: no conflict — the model describes exhaustion behavior, not a
mandatory three-attempt allowance, so a **narrower authorized operating cap is
acceptable** and the anchored behaviors are untouched (our stops never produce
those constructors). The new `:held`/`:refused` policy paths are additions the
Lean model does not yet describe (unresolved item below).

## Enforcement wiring (one shared decision)

`meta_repair_coordinator.clj` defines the caps, the evidence readers, and the
two policy fns; boundaries read them from their opts/ports:
- `prepare` adds defect `:policy/author-attempt-cap` when the policy refuses.
- `coordinate` checks the policy exactly when a NEW `:claimed` entry would be
  created.
- `coordinate-queued-retry` checks the child policy in the pending-requeue
  branch before minting `new-id`.
- `meta_self_heal_production.clj` arms both caps by default and ONLY the
  default — **no production override exists** (review fix 2026-10-31,
  codex-11 dispatch 8542, after zai-5's independent review flagged the prior
  opts override as an unauthorized grant channel): `work-order-options` uses
  `coordinator/default-repair-policy` unconditionally and ignores any
  `:policy/*-cap` in opts; `effect-ports` assoc's the default caps over ANY
  port source — including an externally supplied `:meta-repair-ports` map
  (a caller cannot leave the caps unarmed or tamper them by supplying ports;
  when no port source exists the result stays nil so the readiness
  `:repair-ports` defect is unchanged). Raised caps remain reachable only at
  the coordinator explicit-opts test seam (behavioral negative controls);
  no grant mechanism or grant receipt exists in this packet. Nothing silently
  resets an allowance: once an attempt is spent it stays spent.

## Tests (`futon2.aif.meta-repair-attempt-policy-test`, 8 tests / 58 asserts)

- `first-unspent-injury-is-allowed-and-discharges-normally` — success path
  unchanged (1 author / 1 review / 1 enact, discharged).
- `author-failure-spends-exactly-one-author-contact-despite-raw-limit-three`
  — raw limit stays 3 (asserted); the click's continuation stops at the typed
  policy refusal with exactly **1** author contact; ledger keeps
  `[:reopened 2 :author-failed]`.
- `repeated-clicks-and-restarts-cannot-invoke-a-second-author` — a second
  full run (everything reloaded from disk) refuses the policy with **zero**
  author contacts.
- `negative-control-raised-cap-exposes-the-excess-author-contacts` — replaced
  (8542) by the two tests below.
- `tampered-cap-opts-cannot-disable-production-guard` — passing raised (3),
  nil, and nonpositive (0/-1) caps — and tampered caps INSIDE a supplied
  `:meta-repair-ports` map — through the real production preparation paths
  (`work-order-options` and `effect-ports`) always yields the effective
  default cap of **1**.
- `production-path-with-tampered-cap-three-still-spends-exactly-one-attempt`
  — cap 3 passed through a full production click is inert: exactly **1**
  author contact, typed policy refusal.
- `negative-control-coordinator-seam-raised-cap-exposes-the-excess` — at the
  COORDINATOR explicit-opts test seam (not a grant; production never reads
  `:policy/*-cap`), cap 3 with the SAME spent evidence allows a new claim:
  the cap value, not a status keyword, is the guard.
- `inconclusive-retry-child-gets-no-second-child-across-resume` — armed
  coordinator holds `:coordinator/policy-retry-child-cap`, no port call,
  nothing appended; resume on the same state keeps holding; the unarmed
  negative control DOES enqueue `click-retry-requeue-1` (exposed excess).
- `spent-attempt-counting-follows-sealed-transition-evidence` — `:open` =
  unspent; claimed/enacted attempt 1 = spent `[1]`, refusal never named as
  ledger exhaustion; an unarmed boundary stays legacy-unlimited (this is why
  the legacy coordinator unit suite is unchanged).

Existing suites: `meta-repair-coordinator-test` 35/204 green (unchanged; the
policy + coordinator suites together run 43 tests / 262 assertions, 0
failures);
`meta-self-heal-production-test` on the branch fails exactly its pre-existing
baseline set (12 failures / 1 error, byte-identical list on main); five
click-7 continuation tests asserting the superseded 3-attempt behavior were
re-anchored to the policy stop (renamed
`click-7-author-failure-stops-at-the-policy-attempt-cap`). Ledger-level
requeue suites (`f43`, `f46`) green.

## Historical ledger validation (read-only, branch JVM)

Live ledger `data/wm-meta-injuries-live-2026-10-22/ledger.edn`:
`:loaded`, **130 entries**, sha256 `ca73914f0c26fb1435a0497398a191302c46da705dc09da7724b4f4998371a44`
(unchanged); ledger validation runs inside the loader and inside `prepare`.
**Pin of remaining allowance, not consumed:** for
`injury/adf04df0f31ab8727ac57d9c7f2496d8fa28598c`,
`spent-author-attempts = []`, default policy `{:status :allowed :cap 1
:attempts-spent []}`, and an armed `prepare` dryrun reaches **`:prepared`**
(attempt 1 of raw limit 3, unspent, available under the cap).

## Unresolved / flagged for review

1. The Lean model does not yet describe the two new typed stop paths; a future
   Lean packet should anchor them (and the policy caps) alongside
   `AbandonReason`.
2. Crash-recovery re-dispatch within the SAME attempt: **author attempts ≤ 1
   across all persisted history; this is not exactly-once external contact.**
   A crash between author-port invocation and receipt persistence may
   re-dispatch the same attempt on resume (pre-existing recovery semantics).
   The cap counts attempts, not dispatch retries of one attempt, so this
   limitation does not violate the attempt-number bound; no crash-recovery
   redesign (receipt-first/append-before-invoke) is requested here.
3. Serving arming: futon3c passes no policy keys; futon2's production defaults
   arm the caps, so the serving JVM enforces them only after this branch is
   merged and the namespaces reloaded (separate dispatch; nothing done here).
4. Grants: **no production override is supported** (8542 fix). `:policy/*-cap`
   opts are read only at the coordinator boundary as a test seam; production
   never supplies or honors them, tampered values are inert, and no grant
   mechanism or grant receipt exists in this packet. Any future grant must
   carry explicit operator authority in its own receipt — designed then, not
   now. No new ledger exhaustion states, no history edits, no future policy
   grants.
5. One production-test assertion was re-anchored (8542):
   `effect-ports-carry-the-adapter-click-minting-function` now asserts the
   caps are armed over an explicit port map ("used as given" no longer holds
   for `:policy/*-cap`). Production suite stays at its pre-existing 12/1
   baseline (identical failure-name distribution, verified on this branch).
