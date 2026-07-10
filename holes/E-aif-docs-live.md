# Excursion: one live data model behind paper + tracker + explainer (E-aif-docs-live)

**Date:** 2026-07-01 · **Status:** CHARTERED — parked; **resume at a pause point** (work in
progress now: claude-5's DISSOLUTION (A) body-reconstruction + the `cascade-real/graph` endpoint are
being actively built; do NOT refactor the endpoint/explainer under them). **Owner:** claude-4.
**Relates to:** C-cascade-real §7 DISSOLUTION (the follow-on) · [[project_sequel_paper_closing_the_loop]]
(the paper this serves) · `futon2/holes/aif-wiring-explainer.html` (the explainer to unify).

## The ask (Joe)
Make the paper (the literate notebook) **correlate in real time** with
`aif-wiring-explainer.html` via **Nelson-style transclusions**, so that the **literate notebook +
the build-tracker + the explainer all flow from ONE underlying data model**. Edit a state once → all
three views move. Kill drift.

## Why (the disease)
The same "tracked items" (R-rungs, O-dimensions, paper-claims, feature-rows) are **duplicated and
already drifting** across: the explainer's hand-authored `NODES`/`IFACES`/`EDGES`; its spawned sibling
`aif-cascade-loop-live.html`; `pipeline-pattern-cascade-live.html`; the notebook's Appendix A
claims-ledger; and the DISSOLUTION checklists. Transclusion from one source is the cure — and it is the
**C-cascade-real thesis ("one cascade, not seven pictures") applied to the docs themselves.**

## What already exists to reuse (mostly unification, not new machinery)
- **`/api/alpha/cascade-real` + `/api/alpha/cascade-real/graph`** — already serve the O-dimension model
  live (`{lineage, clusters, holes, arrows, held, patterns, counts}`), zero hand rows, CORS-enabled for
  `file://` fetch. Server: `futon3c/src/futon3c/transport/http.clj` `handle-cascade-real[-graph]` (~5451).
- **`pipeline-pattern-cascade-live.html`** — already transcludes those (fetch → render*). The PoC.
- **`docs/wiring-claims.edn` + `wiring-evidence.edn`** — the futon "claim → commit-scoped evidence"
  convention IS Nelson provenance in EDN form; extend it, don't reinvent.
- **The explainer's embedded `NODES`** — the one piece still hand-authored; the seed for the registry.

## Architecture
**One registry of `tracked-item`s**, each `{:id :title :state :evidence :owner :backlinks}`, two state kinds:
- **derived** (live-computable): cascade dimensions, census counts, verify-live composition, gate-green
  — already in `cascade-real/graph`.
- **declared** (authored): R-rung wiring states, holes, promises, ownership — an EDN registry seeded by
  lifting the explainer's `NODES` out of the HTML.

**One endpoint** merges derived + declared and serves items **with provenance** (canonical id +
evidence commit/endpoint/number + backlinks). **Each view transcludes by id, links back to evidence:**
- explainer → `fetch()` its nodes instead of embedding (states derive live);
- notebook Appendix A → a fetch-and-render `#+begin_src` cell;
- tracker (DISSOLUTION checklist) → regenerated from the same source.

## The one open design choice (Joe's domain — decide at resume)
**(a) unify** R-rungs + O-dimensions + paper-claims into one id-namespace, vs **(b) federate** them as
distinct kinds under a common `tracked-item` schema, cross-linked Nelson-style (an item transcludes
another). **Recommendation: (b) federated** — mirrors the C-cascade-real lesson (each *kind* keeps its
own canonical id-scheme; compose on **links**, not by collapsing namespaces). **EDN-first** (matching
`docs/*-claims.edn`), path to substrate-2 later.

## Slice 1 (MVP — proves it end-to-end, build first at resume)
1. Lift the explainer's `NODES`/`IFACES` → **`futon2/holes/aif-rungs.edn`** (single authored source).
2. Serve **`/api/alpha/aif-rungs`** (declared states + R↔O cross-refs derived from `cascade-real`).
3. Retrofit `aif-wiring-explainer.html` to `fetch()` its nodes (delete the embedded copy).
4. Notebook Appendix A → a live fetch-and-render cell against the same endpoint.
→ Editing one state in `aif-rungs.edn` moves the explainer **and** the paper. Transclusion proven.

## Boundaries / sequencing
- **Do not start until claude-5's DISSOLUTION (A) endpoint work is at a pause point** — coordinate; the
  `cascade-real/graph` endpoint is the shared surface and must not be refactored under active work.
- Excursion scope = the transclusion/unification layer ONLY. It consumes `cascade-real/graph`; it does
  not rebuild the cascade body (that's DISSOLUTION (A)) or the AIF loop (that's the R16/R14 work).
- Gates at resume: clj-kondo on any Clojure; the endpoint reload-safe (extra-routes, per prior C-cascade
  work); one edit → two views propagate, demonstrated.
