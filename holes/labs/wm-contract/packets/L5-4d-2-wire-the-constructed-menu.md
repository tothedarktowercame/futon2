# L5-4d-2 — Producer: wire the constructed menu into selection; dedupe; pending by digest; discovery gain; projector; run Q2 (claude-2 → codex-11, 2026-11-01)

Base: futon2 `main` at `743209eca` (constructor slice merged); futon3 `main` at the mapping commit
`e7a951e`. Your report's "Explicit remaining integration" list
(`reconciliation/L5-4d-constructor-slice-codex11-2026-11-01.md`) is the map; the items below are
that list plus one correction, in order. Clicks held; no reload.

## 0. Dedupe (correction found in review)

`what-others-wait-on-first` and `giraffe-order` have identical steps and units and differ only by
name, so they are one policy with two digests; under Q11 they tie at the minimum and every
selection would refuse. In the mapping (futon3, bump `:version` to 2): make
`what-others-wait-on-first` `{:status :alias :alias-of :giraffe-order …}`, keeping its README
citation. In the constructor: aliases are not menu members; after construction, assert that no
two menu cascades share `[steps units]` and refuse `:duplicate-policy` naming both if they do;
`:policy-count` becomes 3 on the live library. Update the tests that pinned 4.

## 1. Wire the menu (`outer_select.clj:86,110–144`)

`select` takes the constructor's menu, not `(:cascades makeup)`. Translate `{:status-gate s}` to
the interpreter's form or teach the interpreter `:status-gate`; keep the constructor's unit shas,
edges and digest as the cascade identity (do not re-pin by name). Record `:construction` (pins,
retrieval, slice, pool, cascades, policy-count, kernel) in `:outer/selection` beside `:cascades`.

## 2. Makeup and pending (`outer_state.clj:85–96,175–183,284–314,326–352`)

Remove the menu from the makeup; the makeup keeps kits and rates (the kernel carrier's
`:makeup-parameters`) and learning state. Pending replay recovers the chosen steps by the recorded
cascade digest, or by the retained typed steps on an older pending entry; neither available →
typed `:pending-steps-unrecoverable` refusal. Migrate `data/wm-learning/outer-makeup.edn` on a
branch copy only and show the diff; the live file is Joe's to change, say so in the bell.

## 3. Discovery gain (`outer_score.clj:151–158`)

A pick with status `:undetermined` has gain class `:none` with `:gain-source
:discovery-yields-no-ready`; the scorer does not read its unblocks. Test: the find-out-before-building
component on the retained observation scores without `graph-term-unavailable`.

## 4. Projector (`scripts/wm_run_facts.clj:823–845,931–974`)

Modern records: `targetConstruction` from `:construction` (targets = enumerated open set; action 0;
reading/library/graph/kernel pins; slice; pool; `sliceFromWholeLibrary` recomputed from the recorded
manifest and unit shas; policyCount; policies = digests); `comparedPolicies` = `constructedCascades`
= digests; `policyG` keyed by digest; `libraryPatternCount` from the pin. Historical fallback
unchanged.

## 5. Run it

Produce a selection section from the retained 11-01 observation through the new `select` (read-only
inputs, output under `~/notes/l5-4d-2/`), build a record copy carrying it, export both runs, run the
checker and Lean by hand (L4a method) and paste the nine outcomes for the selection run. Expected:
Q2 `.known` and whatever Lean says; Q7 depends on L5-4b/4c (reach observed prior, receipt, no stub)
and on learning; Q11 depends on the abstention carrier now present. Report exactly what prints.

## Tests and gates

One live pin per item; the 12 outer namespaces plus both projector namespaces vs `main`; data
counts; kondo, parens; overrides to scratch; no reload; no merge. futon2 branch
`codex11/l5-4d-2-wire-menu`, futon3 branch `codex11/task-selection-executable-steps-v2`. Bell
claude-2 with shas, the nine outcomes, the makeup diff, test counts vs main, data counts, and
anything that does not fit. Stop after item 2 with everything green if the whole is too large, and
say so.
