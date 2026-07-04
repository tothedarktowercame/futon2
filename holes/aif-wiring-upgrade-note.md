# Upgrade note — deprecating the futon3c portfolio/mission-control AIF subsystem

*Companion to `aif-wiring-explainer.html`. Written 2026-07-04 (claude-10, at Joe's
direction). Status: **PROPOSED** — the deprecation decision is Joe's; the shutoff
steps below are staged and reversible, NOT yet executed.*

## What this is about

The explainer describes the **futon2 War Machine** as the live strategic Active
Inference apparatus — belief · EFE · preferences (the belly, R19) over the real
futon stack. There is a *second, older* AIF subsystem living in **futon3c** as two
coupled peripherals, and it is being **superseded**:

| Deprecated (futon3c peripheral) | What it did | Superseded by |
|---|---|---|
| `portfolio-inference` | A scheduled AIF loop (`scheduler.clj`, `portfolio.core/portfolio-step!`) depositing `:portfolio/observation/belief/policy/step` evidence on a cadence | the **futon2 WM** — the live belief/EFE/preferences loop over the actual stack |
| `mission-control` | Mission-doc **lifecycle** tracking — status classification (in-progress / blocked / stale-in-progress), `:mission/blocked-by` dependency edges, coverage-vs-devmap, snapshot-to-snapshot diff; *feeds* portfolio-inference its review | **C-cascade-real / `pipeline-pattern-cascade-live.html`** — substrate-2-backed per-mission view (cited patterns · holes · held items · clock · cascade). Close to switchover; see the gap below. |
| `issue_holes` (+ `scripts/gh-issue-holes`) | Dormant **one-off library** (last touched **2026-03-08**, never scheduled, no output artifacts) — projects open GitHub issues into the hole/tension vocabulary, anchored to missions/claims | the **unified hole vocabulary** — sorry-registry (in-code proof-holes) + the cascade's per-mission holes. Retire. It was mission-control's *only* external consumer (one `scan-mission-files` call), so retiring it dissolves the last coupling. |

They are **one subsystem**: `mission-control` produces the mission review;
`portfolio-inference` runs `:pi-step` — "live aif-step using mission-control
review". So they retire together and the coupling stays internal to the pair.

### Switchover gap: mission-control → cascade (what's "slightly different")

The cascade view already carries the richer mission **content** (per-mission cited
patterns · holes · held items · clock), off the substrate-2 corpus — that part is a
strict upgrade. What mission-control does that the cascade view does **not yet**
carry is a mission **lifecycle** layer:

| mission-control capability | in cascade-live? | to close the gap |
|---|---|---|
| status classification (in-progress / blocked / **stale-in-progress**) | partial (clock/holes imply activity, no explicit status) | derive status from cascade signals, or drop if the clock+holes suffice |
| `:mission/blocked-by` dependency edges | no | add a blocked-by edge to the cascade, or decide dependencies live elsewhere |
| coverage vs devmap (`:mc-coverage`) | partial (coverage mentioned) | confirm the cascade's coverage == the devmap coverage, or port `:mc-coverage` |
| snapshot-to-snapshot diff (`:mc-diff`, "what changed since last sync") | no (the clock is per-mission, not a cross-sync diff) | the Arxana Clock / a cascade diff covers this? confirm |

**Assessment:** "close to switchover" is accurate. The content substrate is already
better; the remaining work is deciding, per row above, whether each lifecycle signal
is (a) ported onto the cascade, (b) already covered by an adjacent surface (Arxana
Clock, holes), or (c) genuinely superseded and dropped. None is a blocker to
**halting the portfolio flood** now (step 1); they gate the **durable** mission-control
deregister (step 4).

## Why now — the evidence-landscape symptom

This surfaced from a concrete question: *"is the War Machine inspectable / already
in the evidence landscape?"* Querying `GET /api/alpha/evidence` showed:

- **Agent turns ARE in the landscape** (type `coordination`, tags `turn`/`invoke`).
- **The futon2 WM is NOT** — its ticks go to a private local file (`data/wm-trace/*.edn`).
- **`portfolio-inference` dominates it — 305 of the last 500 entries** — with
  `belief`/`policy`/`observation` tags that *look exactly like the WM* but are a
  different AIF loop (`subject {ref/type: portfolio}`).

So the landscape is flooded by the *deprecated* loop while the *live* apparatus is
absent — the precise "data availability + clear interfaces" gap. Retiring the loop
de-noises the bus and removes the decoy.

## Decommission plan (staged, reversible-first)

1. **Reversible halt (immediate, no code change):**
   `(futon3c.portfolio-inference.scheduler/stop!)` in the hub JVM — stops the flood
   now; `start!` restores it. Do this first; it's a clean scheduler halt, not a
   server restart.
2. **Acknowledge the coupled consumer:** `mission-control`'s `portfolio-diff` /
   coverage-diff goes **dormant** (stale, not crashing) once the loop stops
   depositing. Fine if both are being retired together.
3. **`issue_holes` retires too** (Joe, 2026-07-04) — a dormant one-off (last used
   2026-03-08, unscheduled, no artifacts). Nothing to stop; drop
   `scripts/gh-issue-holes` + the ns. Its single `mcb/scan-mission-files` call was
   mission-control's *only* external consumer, so this removes the last coupling and
   mission-control can then deregister cleanly. (Its GitHub-issue-as-hole idea, if
   ever wanted again, re-homes onto the unified sorry/cascade hole vocabulary.)
4. **Durable deprecate (futon3c change):** remove `:portfolio-inference` and
   `:mission-control` from `peripheral/registry.clj` (`all-peripheral-ids` +
   `factories`), mark deprecated in `README-mission-peripheral.md`. **Keep the
   code** as a reference implementation (portfolio-inference's real EIG is cited as
   the positive example in the R18 badge notes for `G-info`).

## The forward move (what makes this stick)

Retiring the decoy is half of it. The other half — from the same investigation —
is making the **live** WM available on the bus with a clear interface:

- **Availability:** `wm-scheduled` POSTs each tick's evidence to
  `/api/alpha/evidence` (mesh-ready via `FUTON3C_EVIDENCE_BASE` — same/other JVM or
  another box stops mattering).
- **Clear interface:** a stable `evidence/subject {ref/type: war-machine, ref/id:
  futon2}` + tags `wm-tick`/`wm-click`/`wm-gate`, so a single query returns *exactly
  the WM* — distinguishable from portfolio-inference and everything else. The
  `wm-gate` edges (`{from, to, verdict}`) make "which way does evidence flow" a query.

Once that lands, inspectability is a property of the bus, not a UI we rebuild per
question — and the explainer can point at a live "here is the WM on the landscape"
query instead of a private trace file.

## TODO before executing

- [x] Successor confirmed (Joe, 2026-07-04): `portfolio-inference` → futon2 WM;
      `mission-control` → **C-cascade-real / `pipeline-pattern-cascade-live.html`**
      (lifecycle-layer gap catalogued above).
- [x] **`portfolio-inference` RETIRED** (Joe's go, 2026-07-04, claude-10):
      (a) LIVE `scheduler/stop!` over Drawbridge — `running? false` (was already
      stopped; on-demand flood had ceased ~14:02Z); (b) NEXT-START deregister from
      futon3c `registry.clj` (`d43323c`, off next JVM boot, reference ns retained);
      (c) wiring-claims `:peripheral/…` + `:daemon/…-scheduler` marked `:deprecated`
      (`a256aca`). Gates: check-parens + clj-kondo clean.
- [ ] Resolve the 4 lifecycle-gap rows (port / route-to-adjacent-surface / drop)
      before the **durable** mission-control deregister.
- [x] `issue_holes` disposition (Joe, 2026-07-04): **RETIRE** — dormant one-off;
      removes mission-control's last external consumer. *(drop not yet executed)*
- [ ] Remaining to execute: drop `issue_holes` + `scripts/gh-issue-holes`; resolve
      mission-control lifecycle gap → deregister mission-control (same pattern as
      portfolio-inference above). Optional: disable the `/api/alpha/portfolio/*`
      HTTP endpoints (on-demand path; requires a caller, so lower urgency).
