# Task: author SPEC-DERIVED build-match bindings for a done-mission CLean

> **Closed 2026-11-08 (already done).** zai-10 authored the spec-derived autoclock-in bindings in `src/futon2/aif/actuator_a3.clj` (s1, s2, s6 bound; s3-s5 left unbound as abstract; s6 endpoint `ReconstitutableLineage` in `reviewed-endpoint-bindings`) and the grounding note `holes/bindings-autoclock-in-grounding.md`. Evidence: futon2 `9a0a2a2a8`; `actuator_a3.clj:34-43` and `:71-82`; clj-kondo 0/0 and check-parens OK on the current file, 2026-11-08. Checked by f2 in the closure pass (claude-6, FIX/WONTFIX sheet).

**Status:** DONE — closed 2026-11-08, see the closure note.

## Acceptance checklist (2026-09-30)

- [x] `reviewed-box-bindings` contains a spec-derived six-box entry for `futon3c-d/mission/autoclock-in`, with abstract/process outputs explicitly left unbound. (`9a0a2a2a8`)
- [x] `reviewed-endpoint-bindings` contains the terminal `s6` `:discharges` binding for `futon3c-d/mission/autoclock-in`. (`9a0a2a2a8`)
- [x] `holes/bindings-autoclock-in-grounding.md` names every box's `:produces`, chosen substrate type or unbound status, and its spec-derived rationale. (`9a0a2a2a8`)
- [x] The changed Clojure passes `clj-kondo` and `futon4/dev/check-parens.el` without a substrate write or `:7071` restart. (`9a0a2a2a8`; rechecked 2026-11-08)

**Author:** zai-10 (reviewed lane — claude-4 reviews + runs build-match + returns a gap report).
**This round's target:** `autoclock-in` (`futon3c-d/mission/autoclock-in`).

## Goal
Make a *done* mission testable through **build-match** by authoring the per-box
substrate bindings its CLean is missing. Then claude-4 runs build-match to measure the
**drift** between the authored CLean spec and what was actually built.

## READ FIRST
- `futon6/holes/clean/autoclock-in.clean.edn` — the CLean (6 boxes, each with `:produces`).
- `futon2/src/futon2/aif/actuator_a3.clj` — `reviewed-box-bindings` + `reviewed-endpoint-bindings`
  (the **M-learning-loop** entry is the exemplar format).
- `futon2/holes/M-actuator-spec-build-match.md` — how bindings feed build-match.

## THE CRITICAL RULE — spec-derived, NOT reverse-engineered
For each box, derive its binding from the box's `:produces` **meaning + `:text`** (what type this
box *should* produce), cross-referenced with the substrate-2 **schema** (explore via authed
Drawbridge only to learn the right type NAMES — e.g. what `:hx/type` / `:entity/type` values the
clock/lineage work uses). **Do NOT query inhabitation and pick types to make build-match pass** —
that is circular and destroys the whole test. You must be able to justify each binding from the box
meaning *without* knowing whether it's inhabited. The drift is the signal; claude-4 measures it.

## TASK
For each of autoclock-in's 6 boxes decide:
- a substrate binding `{:kind :entity|:hyperedge :type <kw> :endpoint-types [...]?}` — OR
- **unbound** if the `:produces` is an abstract/process type (an invariant, a spoiled-guard) with no
  substrate entity/hyperedge manifestation. Not every box grounds — say so honestly.
The terminal box (`s6`, `:discharges`) → also add a `reviewed-endpoint-bindings` entry.

## DELIVER
1. Edit `actuator_a3.clj`: add `reviewed-box-bindings "futon3c-d/mission/autoclock-in" {...}`
   (+ `reviewed-endpoint-bindings` for the terminal), in the M-learning-loop format. Commit on the
   current actuator branch.
2. `futon2/holes/bindings-autoclock-in-grounding.md` — per box: `:produces`, chosen substrate type
   (or "unbound: abstract"), and WHY, argued from the box meaning.
3. Gates: `clj-kondo` clean, `futon4/dev/check-parens.el` clean, **no `:7071` restart, no substrate
   writes** (schema reads only).

Bell **claude-4** back with the commit sha + the grounding-note path. Do NOT touch build-match/A3
logic — only add the bindings + the note.
