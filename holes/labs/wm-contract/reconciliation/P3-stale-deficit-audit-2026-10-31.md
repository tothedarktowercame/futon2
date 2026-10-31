# P3 stale-deficit audit — six rows, evidence-only (2026-10-31)

Reviewer: zai-5, dispatched by codex-11 (job invoke-1793414890003-8600-ffa34c0b),
P3 of PLAN-criteria-completion-2026-10-31. One bounded pass, read-only: no tracker
changes, no code, no broad tests, no live eval/store writes, no source repair, no
author contact, no click, no delegation. Machine-readable inventory alongside
(evidence-inventory-p3.json). Written closure conditions preserved verbatim.
Branch base: origin/main 4afa80e68. Verdicts: CLOSED-WARRANTED / LIVE-WITNESS-OWED /
SOURCE-BLOCKER.

## Milestone 1 — rows 1–3

### Row 1 — C3-AUTHOR-REPOSITORY-HISTORY (C3/B2)

Written closure condition: *"Canonical delivery gate exercised by fdba6f19d in run
43ec20a7."*

**Verdict: CLOSED-WARRANTED.** The exact witness the condition names exists and is
pinned in the raw retained run record:
- Run 2026-10-29-43ec20a7 (`tick-run-record-2026-10-29-43ec20a7-c3ba-4726-a43c-a827b2b3e66d.edn`,
  run-output sha `38c230330dca99aba2850f4ba9bcb50e1e3d5918859cac48e8c3bd5e9e0ca33d`)
  carries `:answer-delivery {:source :reviewed-patch-function, :patch {:namespace
  "futon2.aif.wm.authored.req-aab759a8", :var "decisions", :source-digest
  c7339cca4c123fc7fe3994541db206917e668b774b20d38f48d9cd522ef11498, :commit
  "fdba6f19db389db788e8569bc7c037481c099687"}, :requisition-id aab759a8…47de,
  :answered […]}` — a canonical 40-hex commit delivered through the reviewed-patch
  delivery path, the gate that run e0b9aab6 failed (`:hot-load-failed`, author
  committed to a sibling worktree).
- The delivery was exercised, not just recorded: the patch answered the requisition
  and the run continued past cascade-resume delivery; the run's own terminal failure
  (`:resume-protocol-violation`, held open-cascade) is downstream of and unrelated to
  repository/delivery identity. `run001/author-delivery-live/receipt.json` records the
  same delivery with the identical commit/digest/requisition pins.
- Historical record preserved: e0b9aab6's record is untouched; no retrospective
  ingestion performed or needed (condition asks only for the 43ec20a7 witness).
Corroborating fix `329b280ac` remains in main ancestry (direct fix for the sibling-worktree
failure). Missing evidence: none. Tracker-only closure warranted.

### Row 2 — E4-WORK-ORDER-NIL-RETURN-LIMIT (E4)

Written closure condition: *"A real self-heal attempt uses the corrected option
projection, while absent limits remain absent and explicit nil or negative limits
remain refused."*

**Verdict: LIVE-WITNESS-OWED (integration verified; one receipt-level witness missing).**
- Regression commit 5319b4b47f (2026-10-29 12:47Z, in main) made `work-order-options`
  emit `:acceptance-return-limit nil` when the runtime option was absent, refusing
  every non-runtime caller at the coordinator's natural-number validator — corroborated
  by `immune-loop/old-failures-census.json` (two class-b failures pinned to that commit).
- Corrected projection is in main: `e0cecacca` ("Omit absent acceptance return limit",
  2026-10-29 17:45Z, ancestor of main) and `0a6f09116`; current source at
  `meta_self_heal_production.clj:1766-1767` associates the key only when
  `(contains? opts :meta-acceptance-return-limit)` — absent limits remain absent, and a
  present nil/negative still meets the unchanged natural-number validator (refused).
- Real-attempt clause: a real post-fix self-heal attempt exists — adf04df0's author
  attempt (click eaa9, 2026-10-31, ledger ordinal 130, armed prepare; author job
  reached `cascade_patch_delivery` per descriptor-typing review), which could only
  traverse the corrected projection. **However** neither the eaa9 run record
  (`tick-run-record-2026-10-31-f805ca4b…edn` — zero occurrences of
  `acceptance-return-limit`) nor the ledger entry retains the actual work-order
  options map showing the key absent. Per the "no guessed absence" rule I do not infer
  the omission from traversal alone.
Missing evidence: one retained work-order (or preparation) receipt from a post-fix
real attempt showing `:acceptance-return-limit` absent from the emitted options while
the attempt proceeds. No source blocker; no expanded search performed.

### Row 3 — E4-TERMINAL-INGESTION-IDENTITY (E4/D3)

Written closure condition: *"Missing close classification derives typed Q6 evidence;
otherwise an unclassified terminal failure is scoped to its run and cannot inherit
another injury identity."*

**Verdict: CLOSED-WARRANTED** (integration + subsequent typed identities witnessed;
one sub-clause witnessed only negatively, recorded below).
- Corrected classifier ancestry: merge `fa2fa8fd5` ("Merge reviewed terminal ingestion
  identity and hot-load evidence", 2026-10-29 20:23Z) is an ancestor of main;
  `immune-loop/ingestion-identity/receipt.json` records the serving reload of
  `futon2.aif.meta-terminal-injury`, `wm.open-cascade-resume`, `full-loop-runner`,
  and the historical nil-tuple record (`cause/0304437e…`, run e0b9aab6, injury
  cbd9628e…) preserved under its own pinned classification — injury 9c622038's
  durable history unchanged.
- Subsequent typed terminal identities post-fix, from the live ledger
  (sha `ca73914f0c26fb1435a0497398a191302c46da705dc09da7724b4f4998371a44`, read this
  pass): `f7e97cc665…` opened at ordinal 121 with its own typed cause
  (reviewer-falsifier-failed), and `adf04df0f31…` at ordinal 130 with its own cause-id
  and `:failure-kind :author-delivery-wrong-repository` — both fresh identities, no
  inheritance of a prior injury identity. The historical defect (partly-nil tuple
  `[nil :unknown-stage nil]` hashing into 9c622038) has no post-fix recurrence in the
  retained records examined.
- Honest caveat: the literal first disjunct ("missing close classification derives
  typed Q6 evidence") has no positively-retained post-fix instance in this bounded
  pass — every post-fix terminal examined carried a classification. The scoping
  guarantee is witnessed by fresh typed identities, not by a post-fix unclassified
  terminal being scoped. Recorded, not blocking: the condition's operative protection
  (no foreign identity inheritance) is evidenced.
Missing evidence: none required for closure as written; the caveat above is for the
record.

## Milestone 2 — rows 4–6

### Row 4 — E4-E43-CAUSE-SCOPE (B5/E4 historical mapping)

Written closure condition: *"Real retained cascade compiles cascade-ready through the
repaired producer; preserve self-heal scope failure evidence for later E4 work."*

**Verdict: LIVE-WITNESS-OWED.**
- Producer fix integrated: futon3c `45a6521bf7038df3f2be60920698a9c03116c611`
  ("Supply existing operational clauses to held cascade producer", 2026-10-29 17:55Z)
  is now an ancestor of futon3c master — the checkpoint's "pending review/merge" state
  has advanced.
- Scope-failure evidence preserved: repair-iteration-33 receipt (mechanical discharge
  of e43 at ordinal 114), acceptance 37b6deb4, patch fc9375d466, ledger sha
  `5abf05c6eed46129d8c08cbf19c2f1c01aea8acac5282645eaa520ecab73cc1a` — all retained.
- Cascade-ready witness: **absent**. Post-fix run records examined for the literal
  producer success shape — 3415 (`:status :resumed`/`cascade-ready` 0 occurrences;
  cascade-resume phase completed via a different arm), eaa9, e5f08d91 — none retains
  an `open-cascade-resume` `{:status :resumed :result {:status :cascade-ready …}}`
  through the repaired held-cascade producer. The clause explicitly requires a real
  retained cascade compiling cascade-ready through the repaired producer; tooling or
  load evidence would not close it.
Missing evidence: one retained post-fix cascade-resume success through the repaired
producer showing `:cascade-ready`. No source blocker identified.

### Row 5 — A2-RELATIVE-STORE-ROOTS (A2/E2/E4)

Written closure condition: *"Canonical roots used by reachable writers/readers;
historical split explicitly reconciled or scoped without losing evidence."*

**Verdict: SOURCE-BLOCKER on clause 1; clause 2 satisfied.**
- Clause 2 (split scoped, evidence preserved): `relative-store-paths/inventory.json`
  pins the historical 52-record futon3c split record-by-record (sha256 per file);
  `decision.md` scopes it; reads now use futon2; no records moved. Satisfied.
- Clause 1 (canonical roots for reachable writers): a reachable relative-root fallback
  remains live in current main, with exact first divergence:
  1. `src/futon2/aif/flight_driver.clj:273` constructs `enact-fn` **without**
     `:trace-dir`;
  2. `src/futon2/aif/flight_runner.clj:1074` passes that (nil) `trace-dir` to
     `temporal-update/publish!`;
  3. `src/futon2/aif/temporal_update.clj:181` falls back
     `(or trace-dir lane/default-trace-dir)`;
  4. `src/futon2/aif/lane_futility.clj:15` `(def default-trace-dir "data/wm-trace")` —
     **relative**, resolved against process cwd, so a flight temporal publication on
     the previous-posterior branch takes the trace/index lock under a cwd-dependent
     root instead of the canonical `/home/joe/code/futon2/data/wm-trace`
     (`trace.clj:64`, which the main selection trace correctly uses, and
     `selection_authoring_coupling.clj:15` absolute).
  This is a genuine reachable divergence in code, independent of whether a current
  run001 click traverses flight enactment. Consistent with the checkpoint's DEFERRED
  classification (not a launch gate): recorded here as the concrete first-divergence
  path for a future bounded fix; no fix attempted under this packet.

### Row 6 — B5-INJURIES-HOLD-RUN001 (A3/E4)

Written closure condition: *"No effective injury remains in the medic-corrected
ledger, and the next run001 click starts without an injury stop; retained ledger
projection and click record establish both."*

**Verdict: LIVE-WITNESS-OWED (blocked on P4 recovery, no source blocker).**
- Live ledger read this pass:
  `data/wm-meta-injuries-live-2026-10-22/ledger.edn`, sha256
  `ca73914f0c26fb1435a0497398a191302c46da705dc09da7724b4f4998371a44` (unchanged since
  the descriptor/cap reviews). `injury/adf04df0f31b8727ac57d9c7f2496d8fa28598c`
  at ordinal 130, `:state :open`, `:failure-kind :author-delivery-wrong-repository`,
  detected-click eaa9bc3a. Counting injuries structurally (transition entries with
  `:injury-id`/`:state`), effective injuries = **1** (adf04df0), matching the
  medic-corrected account; the earlier zero-effective disposition (ledger
  `5abf05c6…`, discharges through ordinal 114) is **historical, not current** — the
  checkpoint already says so and the live read confirms it.
- Clause 1 (no effective injury) currently **unmet**; clause 2 (next run001 click
  starts without an injury stop) **unwitnessed** — no post-adf04df0 click exists.
- Path to closure is exactly P4: supported one-attempt recovery (attempt-1 previously
  verified armed/unspent), then one subsequent ordinary start without an injury stop,
  both retained. No new requirement invented.

## Summary table

| Row | Verdict | One-line reason |
|---|---|---|
| C3-AUTHOR-REPOSITORY-HISTORY | CLOSED-WARRANTED | named witness fdba6f19d@43ec20a7 pinned in raw record; historical record preserved |
| E4-WORK-ORDER-NIL-RETURN-LIMIT | LIVE-WITNESS-OWED | fix e0cecacca in main; missing one retained work-order receipt showing absent key on a real attempt |
| E4-TERMINAL-INGESTION-IDENTITY | CLOSED-WARRANTED | fa2fa8fd5 in main + post-fix typed fresh identities (ordinals 121, 130); positive-unclassified sub-clause unwitnessed (recorded caveat) |
| E4-E43-CAUSE-SCOPE | LIVE-WITNESS-OWED | 45a6521bf now in futon3c master; no retained post-fix `:cascade-ready` through repaired producer |
| A2-RELATIVE-STORE-ROOTS | SOURCE-BLOCKER (clause 1) | flight_driver.clj:273 → flight_runner.clj:1074 → temporal_update.clj:181 → lane_futility.clj:15 relative `"data/wm-trace"`; split itself scoped+preserved |
| B5-INJURIES-HOLD-RUN001 | LIVE-WITNESS-OWED | live ledger 130/ca73914f: adf04df0 open, effective=1; historical zero-clearance only; next-click witness pending P4 |

No tracker changes, no closures performed, no code touched, budget 14 untouched.
This audit is independent of P1/P4 recovery as instructed.
