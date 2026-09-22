# Proof ⟨1⟩5 pre-flight — read-only, the serving JVM

zai-1, 2026-09-22. All five checks via `futon3c/scripts/proof-eval.sh`
against the live JVM (nine namespaces reloaded after 37d40709), plus the
Agency roster HTTP API. Nothing was changed.

## 1. The live queue's eligible stratum

The queue declaration (`data/wm-ticket-queue/queue.edn` via
`tq/read-declaration`) holds **exactly ONE entry: the reference ticket
T-repair-occ-444fb018…, front, inserted 2026-09-22T05:13:31.54Z** — the same
state as the freeze. No change. With both candidates admitted (check 2), the
single entry is the eligible stratum.

## 2. Both candidates admitted through the live loaders; locators at HEAD

`cs/load-declared` → `cp/assemble` on the target: **zero refusals**, both
constructed candidates present — :C1
`[:apparatus/done-is-observed-running :apparatus/evidence-to-disposition-once]`,
:C2 `[:aif/declare-the-conditioning :aif/measurement-window-hygiene
:aif/two-layer-calibration :contracts/holder-states-the-claim]`. Facts
observed: task-stated **true**; all five evidence/acceptance tokens **false**.
Every locator resolved at HEAD = 7b98c489 (the ⟨1⟩4-recording commit):
task-stated observed **true** (the ticket title line is present); the five
disposition locators observed **false** with `:file-present false` for the
four evidence files (correct — none authored yet) and `:file-present true`
but decl-absent for `:restoration-accepted` (the ticket's Status line reads
OPEN, not DONE). All exactly the frozen state.

## 3. Classification in the serving JVM

`{:class :focus, :kind :ticket-parent, :focus-status :retained, :focus "WM",
:retained-evidence-as-of "2026-09-22T17:31:44Z"}` — the ticket classifies
:focus through its Parent in the LIVE JVM, and the focus context at the
actual now is **:retained WM** (the last window's validity ended 17:31:44Z;
the established focus carries with its original evidence date). Same as the
⟨1⟩3 check.

## 4. The joint decision, read-only, right now

- G(C2) = **0.5978** (= ln(1/0.55)), G(C1) = **2.9957** (= ln 20).
- Posterior under the full law: {C2 **0.9167**, C1 0.0833}.
- Action marginal: **unique maximum at :aif/declare-the-conditioning (C2's
  first action), mass 0.9167** — no tie under the full law.
- log E − F alone (G suppressed): posterior {C2 **0.5**, C1 0.5} — **still
  an exact tie**, resolved by the name-order tie-break (which happens to
  agree with G here).
- All figures identical to the frozen ⟨1⟩3 check.

## 5. Anything that could refuse the click or force repair-entry

- **Stop-line queue: 10** open non-environmental-hold obligations (13
  environmental holds; 45 open obligations total). Per
  RULING-selection-precedence-2026-09-19 (futon2 8b6827da), the count is
  RECORDED AS EVIDENCE and selection proceeds regardless — it does not
  refuse. BUT: the runner's own stop-line logic takes repair-entry when an
  OPEN non-environmental-hold obligation exists at the front of the queue —
  the reference ticket IS an environmental-hold finding's ticket (repair
  class :environmental-hold, excluded from stop-line pre-emption), so
  ordinary selection of the reference target should hold; the preflight
  cannot fully rule out repair-entry for the OTHER 10 without the live
  runner's own assembly. Flagged honestly.
- **Unknown-class targets: NONE in today's family** — the joint family is
  the reference ticket alone (the queue holds one entry), and it classifies
  :focus. No :class-unknown-no-scalar-g stop applies.
- **Cast seats:** wm-author **idle + invoke-ready**, wm-reviewer **idle +
  invoke-ready**; wm-repair-reviewer **ABSENT from the roster** (also
  codex-24). wm_click.sh's casting check treats absent as not-ready — with
  the new firing policy the script WAITS for the seat rather than refusing
  (up to 30 min, then a terminal account). So the click will wait unless
  the repair-reviewer seat is restored or the cast is overridden
  (--repair-reviewer <a live seat>). FLAGGED for the firing decision.
- No click in flight (`running? false`; the last recorded click is
  wm-click-117f822a, ⟨1⟩1's).

## Differences from the frozen reference field

1-4 are IDENTICAL to the freeze. Two live-state items differ, neither in
the frozen field (which froze inputs, not seats): the 10 obligations in the
stop-line queue (recorded-not-blocking per the ruling, with the repair-entry
caveat above), and the missing wm-repair-reviewer seat. Nothing was
adjusted.
