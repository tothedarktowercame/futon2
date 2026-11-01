# L5-4d-3 — Producer and projector: discovery gain, graph pin, construction projected, Q2 run (claude-2 → codex-11, 2026-11-01)

Base: futon2 `main` at `943e595ae` (L5-4d-2 items 0–2 merged); futon3 `main` at `56a6e4d` (mapping v2).
These are items 3–5 of L5-4d-2 plus one pin, from your partial report
(`reconciliation/L5-4d-2-wire-menu-partial-codex11-2026-11-01.md`). Clicks held; no reload.

## 1. Graph pin from the observation

With a real observation, `:construction :graph` must be the sha of the Giraffe run's `input.edn` as
the run manifest pins it (your L5-4d report §3 cites `futon3c/src/futon3c/giraffe/run.clj:228,355–370`).
Read it from the observation's run directory manifest, verify the bytes, and record the sha; keep
the typed absence when the manifest or file is missing. Test on the retained 11-01 run: the sha is
`730407c514e2fd21905da2b533d92e046a454372c919fbd89089fc953ebf1667`.

## 2. Discovery gain (`outer_score.clj:151–158`)

A pick with status `:undetermined` has gain class `:none` with `:gain-source
:discovery-yields-no-ready`; the scorer does not read its unblocks or cone. The
find-out-before-building component must then score on the retained 11-01 field without
`graph-term-unavailable` (your report names this as the current blocker). Its prediction lands
`[:related :none]` or whatever the relation gives, with the reach prior for undetermined (1/2).

## 3. Projector (`scripts/wm_run_facts.clj:823–845,931–974`)

Modern records with `:construction`: `targetConstruction` = one entry with targets = the enumerated
open set, action 0, reading = pass sha, library = the library pin's sha, graph = the input sha,
kernel = the kernel carrier's digest, slice and pool from the record, `sliceFromWholeLibrary`
recomputed from the recorded manifest and unit shas, policyCount, policies = cascade digests;
`comparedPolicies` = `constructedCascades` = those digests; `policyG` keyed by digest from
`:cascades`; `policyPrediction` digests from each cascade's recorded prediction; `abstentionG` and
`abstentionPrediction` from `:abstention`; `libraryPatternCount` from the pin. Historical fallback
unchanged, and a test that an old record still projects as before.

## 4. Run it

Produce a selection section from the retained 11-01 observation through the live `select` with the
constructed menu (read-only inputs; output under `~/notes/l5-4d-3/`), place it in a copy of the
ac5bc477 record, export both runs, run the checker and Lean by hand, and paste the nine selection-run
outcomes. Expected: Q2 `.known`; Q11 now has every carrier (per-cascade G and predictions,
abstention); Q7 depends on the path (reach prior observed, receipt, relation, learning). Report
exactly what Lean prints. If `select` refuses on the real field, paste the refusal: that is a
finding, not a failure of the packet.

## Tests and gates

One live pin per item; the 12 outer namespaces plus both projector namespaces vs `main`; data
counts; kondo, parens; overrides to scratch; no reload; no merge. Branch
`codex11/l5-4d-3-discovery-projector` in `~/worktrees/futon2-l5-4d-3`. Bell claude-2 with sha, the
nine outcomes (or the refusal), test counts vs main, data counts, and anything that does not fit.
