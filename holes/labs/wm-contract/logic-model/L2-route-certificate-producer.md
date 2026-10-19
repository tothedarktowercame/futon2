# L2 — run-time certificate producer for the self-heal route (proposal)

claude-2, 2026-10-19. Proposal only; nothing here is built. Sources read at futon2
`e4e53617a`, futon3c `fd12530c8`, mathlib4 `darktower` `01df7da6d9`.

## The three legs

- **AIF module:** none found. No `DarkTower/AIF/*` or `DarkTower/WarMachine/*` module
  models injury, repair or route arbitration, and `SelfHealRoute.lean` imports only
  `Mathlib.Data.List.Basic`. `lean.md` says L1 "has the first two" legs; the AIF leg is
  absent. L2 follows the certificate discipline of `DarkTower/AIF/Certificates.lean`, which does
  not state the route. claude-5 to name the module, or record a typed none.
- **Table:** L1, `DarkTower.WarMachine.SelfHealRoute.transitions` and `conforms`.
- **Producer:** this note.

## Where it runs

Every click closes through futon3c `runner_service.clj:779` (`result-summary` calls
`persist-click-run-binding!`, `:573`). The binding is written atomically at `:665`, and
a report-card hook is launched after it without being awaited (`:780-782`). L2b adds the
route check to that hook. `wm_click.sh` is not the right site: it exits at `:379-382`
when no run record exists, which is every refused click, and clicks from the HTTP API
never run it.

## What it reads

1. The binding `data/wm-click-run-bindings/click-run-binding-<click-id>.edn`, in
   particular `:runtime-disposition` (F7, `runner_service.clj:605`).
2. When `:run-record` is present, that record's `:meta-self-heal/readiness`
   (`full_loop_runner.clj:1320`).

## Projection to an L1 path

The producer turns these fields into a `List RouteState`. Any step the record does not
establish is a typed gap and is not filled with a guessed state. Click 3's binding
(`wm-click-2b1d568c`, `:absent-terms [:acceptance-run-port :diagnosis-evidence
:acceptance-test]`) establishes a `route!` refusal: `:diagnosis-evidence` is produced only
by `route!` (`meta_self_heal_production.clj:924`). It does not establish diagnosis or
admission. Its verdict is therefore `conforms-suffix` over `[readinessReady,
routeRefused, terminalRecord]`, with the prefix marked `:not-recorded`. It is not
`conforms`.

## What it writes

The producer writes `click-run-binding-<click-id>.edn.route.edn` beside the binding:
the path, the gap list, the verdict (`:conforms` / `:violates` with the first missing
edge / `:not-checked` with a reason), and the mathlib head and `SelfHealRoute.lean` blob
sha. It does not write into `<run-record>.requirements.edn`. `wm_run_alert.py:92-114`
rewrites that file as a whole, and a refused click has no record to put a sidecar beside.

## Lean as the authority

Recommendation: evaluate the Lean module, as `wm_run_alert.py` already does for Q1–Q10.
Generate `example : conforms [<path>] = true := by decide`, plus the negation, and run
`lake env lean`. The verdict is the kernel's decision on the actual table, and there is
no second copy that can drift. The alternative is a Clojure transcription pinned to the Lean
file's sha. It needs an agreement test over all
mentioned-state pairs, and splits authority between runs of that test. One thing
not to copy from `wm_run_alert.py:105`: there, a Q missing from Lean's output defaults to
`"holds"`. L2 must report `:not-checked` when Lean returns no verdict.

## Bad cases to pin

- Click 2 (`terminal-publication-…08771834…`, `click2RevertedPath`) must produce
  `:violates` with the edge `readinessReady → ordinarySelection`.
- Click 3, above, must produce `conforms-suffix` with the prefix not recorded, not
  `:conforms`.
- A truncated path `[activeInjury, diagnosisPrepared]` currently `conforms`
  (`SelfHealRoute.lean:105-108` checks adjacent pairs only). The producer must also
  require that a path starts at `activeInjury`/`noActiveInjury` and ends at
  `terminalRecord`.

## Size

- **L2a** (futon2): one script, `scripts/wm_route_check.py`, with its test. Binding (and
  optional record) in, sidecar out.
- **L2b** (futon3c): one call in the `result-summary` hook. It is not awaited, and a
  failure is logged as `:not-checked`.
- Prerequisite: the L1 table defects in the L1 reading (claude-2 bell, 2026-10-19).
  Click 4 (`wm-click-fca1c921`) shows that runtime order puts `admissionRefused` before
  a readiness/route refusal, and that edge is not in the table.

## What it does not claim

It does not certify that diagnosis, admission or enactment succeeded. It does not certify
any state the binding or record does not carry. It does not certify the attributable
retry (`full_loop_runtime.clj:492-499`), which reaches ordinary work under an active
injury and is not in L1.
