# Operator View — WM UI build spec (handoff: claude-1 → Codex, 2026-06-05)

**Owner/reviewer:** claude-1 (author of the forward-model + this spec; will review the diff).
**Data backbone (separate, claude-7):** the Morning Bulletin endpoint — already-divided, do NOT build it here.
**Goal (Joe, 2026-06-05):** a professional-grade **Operator view** inside the War Machine UI — a dashboard featuring
the **Morning Bulletin** + a **forward-model "what to act on"** strip + a **Futon Runner easter-egg**. All three read
**real projections**.

This is **additive** — a 6th view mode. Do not disturb the existing 5 (`:stack :self-watch :aif-stack :missions
:patterns`). Build defensively: the data endpoints may not be live yet → **graceful empty-states**, never errors.

## Scope — files (`:in` read-only context, `:out` edit)

`:in` (read for patterns; do not change behaviour):
- `futon2/web/war-machine/src/war_machine/client/core.cljs` — the **dashboard-card pattern** to copy is
  `inhabitation-log-card` (~L1068) and `vsatarcs-status-card` (~L1172): `[:section.dashboard-card …]` with
  `dashboard-card-header`, `dashboard-empty`, `dashboard-item`, severity classes. The **view-toggle** is in
  `toolbar` (L34–50, the `case vmode` cycle). `open-target-in-emacs!` (used in `next-move` tile ~L655) is the
  file-open mechanism for the easter-egg. The **root render** (compose toolbar + center + side panels) is later in
  this file — find it and branch on `:operator`.
- `futon2/web/war-machine/src/war_machine/client/api.cljs` — the fetch pattern (`api/load!`, base `http://localhost:7070`).
- `futon2/web/war-machine/src/war_machine/client/graph.cljs` — the `case vmode` render dispatch (~L159).
- `futon2/web/war-machine/src/war_machine/client/state.cljs` — `s/view-mode` atom + add an `s/operator-bulletin` atom.

`:out` (edit):
- `core.cljs`: (1) add `:operator` to the toolbar toggle cycle (`:patterns :operator`, `:operator :stack`);
  (2) add a `legend` case for `:operator`; (3) add the `operator-dashboard` component; (4) branch the **root render**
  so that when `vmode = :operator` the center area renders `operator-dashboard` **instead of** the hex graph (the
  side legend/stat panels may stay or be suppressed — your call for a clean dashboard).
- `api.cljs`: add `load-operator-bulletin!` → `GET http://localhost:7070/api/alpha/war-machine/operator-bulletin`,
  store in `s/operator-bulletin`; call it from `api/load!`. On non-200 / fetch error, store `{:unavailable true}`
  (NOT an exception) so the dashboard shows an empty-state.
- `state.cljs`: add `(defonce operator-bulletin (r/atom nil))`.

## The dashboard layout (`operator-dashboard` component)

Three stacked sections, each a `dashboard-card`:

### 1. Morning Bulletin  (data: `s/operator-bulletin` ← claude-7's endpoint)
Frozen contract (build against this exact shape; endpoint may be absent → empty-state):
```
{:date "YYYY-MM-DD" :generated-at "<iso>" :nag [item…] :brief [item…] :silent-count N :total M}
item = {:id :title :why :lane("nag"|"brief") :source("mission"|"business-sorry") :target :salience(num|null)}
```
- Header: `Morning Bulletin · <date>` + a "live"/"unavailable" badge.
- **NAG** rows (render first, red accent `#dc2626`): `:title` bold + `:why` dim + a `:source` chip. Sort by
  `:salience` desc (null sinks last). If empty: "Nothing needs you right now."
- **BRIEF** rows (yellow accent `#eab308`): same shape, sorted by `:salience` desc.
- **SILENT**: a single dim line "`:silent-count` discharged overnight" (count only — silent items are never listed).
- Each row clickable → `open-target-in-emacs!` with the `:target` (mission id → its doc; sorry id → leave a console
  log for now). Reuse the existing open mechanism.
- Empty-state (endpoint absent / `:unavailable`): "Morning bulletin not wired yet (awaiting operator-lane endpoint)."

### 2. Forward model — what to act on  (data: `GET /api/alpha/forward-model`, may 404 → empty-state)
Expected JSON (the `mint.edn` summary; endpoint produced separately by claude-1):
```
{:cash-now N :next-action ":S-…" :priced-sorries [{:sorry :metric :expected-lift :blocked? :discharge}…]
 :unpriced-sorries [{:sorry :discharge}…] :traction-metric {:gradient […]} :projected-trajectory […]}
```
- **NEXT ACTION** big: the `:discharge` of the `:next-action` sorry (e.g. "issue Invoice #4, send, Eric pays").
- Actionable-now sorries (unblocked) ranked by `:expected-lift`; gated ones dimmed with "⛔ blocked".
- The unpriced crux (cold-conversion) as one line: "n:0→1 — `:discharge`".
- Empty-state if 404: "Forward model endpoint not wired yet."

### 3. Easter egg
A discreet footer link/button **"▶ 蒲团 Futon Runner"** → open
`futon7/holes/M-futon-forward-model.runner.html` via `open-target-in-emacs!` (`{:kind :workspace-file :path …}`),
the same mechanism the next-move tile uses. (Later it will read the real projections; v1 just launches it.)

## Acceptance bar
- A 6th toggle position `View: operator` appears and cycles correctly (…→ patterns → operator → stack).
- The operator view renders the three sections; with **no** endpoints live it shows three clean empty-states (no
  console errors, no thrown exceptions).
- With a **mock** bulletin in `s/operator-bulletin` (you may hardcode one in a comment/test), nag/brief render
  sorted by salience, silent shows count-only.
- Existing 5 views unchanged (regression: toggle through all 6, each still renders).

## Gates (must pass — see AGENTS.md)
- `clj-kondo` clean on the changed `.cljs`.
- `futon4/dev/check-parens.el` (or the repo's paren checker) clean on the changed files.
- **shadow-cljs compiles** (`npx shadow-cljs compile app` from `futon2/web/war-machine`, or the repo's build) with
  no warnings introduced.
- Playwright smoke (if a harness is handy, e.g. the existing `wm-*-verify.mjs`): load the WM UI, toggle to operator,
  screenshot, assert no console errors. If no harness, describe manual verification in your report.

## Descriptive guard (load-bearing)
The bulletin's signals are **descriptive, not predictive** (c-joint = structural centrality; days-since = git fact;
p = operator prior). The view must **present** them, never relabel them as predictions. No scores/gamification leak
from Futon Runner into this surface (see M-futon-forward-model §12).

## Report back
**Bell `claude-1` back** (`futon3c/scripts/agency_send.py --to claude-1 --kind bell`) with: a short summary, the
commit SHA(s), the gate results (clj-kondo / check-parens / shadow-cljs / smoke), and anything you had to decide
(e.g. where you branched the root render). claude-1 will review the diff against the WM UI conventions.
