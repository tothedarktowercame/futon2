# Packet receipt: occurrence-identity boundary in the reading-family path

**Excursion:** E-implementing-G-over-cascades (Packet A preparation)
**Branch:** `zai-7/e-goc-occurrence-identity` (base 315872e0b)
**Agent:** zai-7 (dispatched by codex-68, requisition invoke-1792195076110-15941)
**Date:** 2026-10-17
**Status:** COMPLETE — occurrence identity repaired on the reading-family
construction/scoring path; production-shaped fixture supplied; inventory below.

## 1. What was repaired

All changes are in `src/futon2/aif/cascade_shape_g.clj` (the shared
reading-family scorer) and one new test namespace:

1. **`structural-identity` node sequence is now occurrence-grain.**
   Before: `(mapv #(or (:pattern %) (:id %) %) (:nodes c))` — pattern ids.
   Two policies citing the same patterns in the same order at different
   fragments deduplicated into ONE policy (`target-policy-family/deduplicate`
   and `materialize-policies`), silently dropping a variant and sharing one
   policy-id. After: `node-occurrence-id` ([pattern fragment-index]). Planted
   control: two readings with identical pattern order but shifted fragments
   collapse under the old pattern-grain view and stay distinct now.

2. **`fit-evidence` is per occurrence.** Before: one fit node per distinct
   pattern id; a citation the reading never made at its own fragment borrowed
   another fragment's accepted evidence (pattern-grain substitution). After:
   occurrence-bearing nodes read only the refs at their own fragment;
   `:occurrence-id` travels on each fit node; `:interpretation-owed` names
   occurrences (patterns for legacy bare-pattern fixtures). Planted control:
   a p/dup occurrence at an unread fragment is `:not-read` per-occurrence
   while the pattern-grain join calls it `:accepted`.

3. **`occurrence-arrangement` preserves 象 provenance per occurrence.**
   Each arranged node now carries `:source-sha256` (the cited pattern bytes'
   sha from the ref), `:fragment-span` {:start :end}, `:sentence-id`,
   `:pattern-rationale`, and a typed unread four-map square
   `:blend-square {:status :unread :reason :four-maps-not-authored}`.
   Typed absences ({:status :absent :reason ...}) when the source node cannot
   supply a field; nothing fabricated. This is what Packet A's canonical
   record needs for occurrence binding; zai-6's validator consumes it later.

Already occurrence-grain and verified unchanged (no repair needed):
`prefix-admission/reading-policy-key` (nodes are `[occurrence-id pattern]`),
`arranged->candidate` units/descent/patterns map, the scorer's done/overlap
tokens, family-selection stage keys.

## 2. Production-shaped fixture

`test/futon2/aif/wm/reading_occurrence_identity_test.clj`: one 象 analysis
with p/dup cited at fragments 0, 1, 2 (three distinct occurrences) and
p/other sharing fragment 1 with the second p/dup citation (an overlap /
gluing candidate). Descriptive reading order arrives as role-qualified edges
(:context-enables / :precedes) from fragment order and stays distinct from
application order (which nothing on this path infers). The fixture drives:
`analysis->cascades` -> `arranged->candidate` -> `reading-policy-key` ->
`score-policy` -> `family-selection/select-over-families` (the one-selector
boundary). 6 tests, 40 assertions, all passing, including negative controls.

## 3. Test summaries (focused runs, futon2 `clojure -M:test`)

- `futon2.aif.wm.reading-occurrence-identity-test` — 6 tests / 40 assertions, 0 failures (NEW)
- `futon2.aif.cascade-shape-g-test` — 13 tests / 49 assertions, 0 failures
  (includes the ^:slow recorded-artifact test: 69 reported / 27 distinct
  policies unchanged, so no live artifact dedup shifted)
- `futon2.aif.target-policy-family-test` + `futon2.aif.analysis-cascade-test` — 17 tests / 5741 assertions, 0 failures
- `futon2.aif.wm.family-selection-test` — 11 tests / 66 assertions, 0 failures
- `futon2.aif.wm.reading-policy-habit-test` — 15 tests / 99 assertions, 0 failures
- `futon2.aif.wm.reading-family-decision-test` — 10 tests / 76 assertions, 0 failures
- `futon2.aif.policy-prefix-admission-test` — 9 tests / 29 assertions, 0 failures
- `futon2.aif.wm.target-policy-field-decision-test` (+ policy-prefix-f) — 0 failures
- clj-kondo clean (0 errors/0 warnings, fresh cache) on both changed files;
  `futon4/dev/check-parens.el` clean.

## 4. Inventory: remaining pattern-grain / flat-vector assumptions

| Site | Grain | Packet B/C must change? |
|---|---|---|
| `src/futon2/aif/analysis_cascade.clj:108` — raw arrangement `:precedence (mapv :pattern nodes)` | pattern vector | Packet B: re-key by occurrence id or drop (shape-g recomputes occurrence-grain precedence; today the raw vector is informational only) |
| `src/futon2/aif/wm/cascade_decision.clj:749-755` — `admit-cascade-problem` square lookup `[id ...]` over `precedence` (pattern ids); `:blend-squares id` | pattern id | Packet B: re-key by occurrence or refuse candidates with duplicate pattern ids (two occurrences would substitute one square) |
| `src/futon2/aif/wm/cascade_decision.clj:843-849` — controlled three-halves join by `pattern-id` | pattern id | Packet B: same |
| `src/futon2/aif/wm/cascade_decision.clj:887-931` — `pattern-sequence` flat vector digests (`:scoring-order`, `:enactment-order`) | pattern vector | Packet C: the flat-vector control path; must bind to the canonical cascade identity instead |
| `src/futon2/aif/interpretation_construction.clj` (whole manifest path: interpretations, precedence, need-edges by pattern id) | pattern grain | By design for interpreted cascades; Packet B keeps it, but blend-square joins must not mix it with occurrence-keyed reading data |
| `src/futon2/aif/wm/cascade_decision.clj:223`, `:655-765` — `pattern-maps` precedence, `(get interpretations %)` | pattern id | Packet B (manifest path, unchanged semantics if no duplicate patterns per candidate) |
| `learning-trial-ledger/pattern-theta` (pattern-grain habit) | pattern grain | INTENTIONAL: habit is per pattern. `arranged->candidate` already binds per-pattern theta to occurrence-keyed units. No change. |
| `full_loop_runner.clj:6388` — `:reading-policy-execution-semantics-owed` stop keyed on flat `:precedence :co-apply` map | flat vector | Packet C: replaced by the executable-cascade frontier (zai-6's canonical record) |
| futon3c `holes/labs/M-象-2000/data/occurrences/*.edn` — squares keyed by pattern + turn | pattern+turn | Read-only input; Packet A joins per-occurrence via fragment/span provenance, not by pattern id |
| `storage/wm-target-readings` 象 artifacts (pattern_refs per fragment) | per-fragment refs | Already occurrence-capable; producers supply source_sha256 + span per ref (verified in fixture) |

## 5. Blockers / handoffs

- None on this packet. The `:blend-square {:status :unread}` typed field is a
  placeholder seam: when 象 four-map squares join this path (Packet A), the
  unread marker must be replaced by the occurrence-resolved square or a typed
  void/unread distinction — never defaulted.
- zai-6 owns `:wm/executable-cascade-v1` and the canonical validator; this
  packet did not define them. NOTE for codex-68: an UNTRACKED draft
  `src/futon2/aif/wm/executable_cascade.clj` was already present in the
  shared worktree before this dispatch; zai-7 extended it earlier in the same
  turn (before the requisition arrived) and then left it untracked and
  uncommitted — reconcile with zai-6 before adopting or deleting it. The
  ~744 characters elided by a read-truncation in the original draft's
  validator section mean zai-7's copy is a reconstruction, not a byte-exact
  restore of what was found.
- Policy-ids minted by `target-policy-family/stable-id` change content for
  fragment-variant policies (structural-identity input changed). No stored
  artifact on this branch depends on the old ids; downstream (habit store
  keys) are derived per-run.

## 6. Integrator review correction

Independent review found that the first packet made the node sequence
occurrence-grain but left `structural-identity`'s edge endpoints pattern-grain.
Two policies with the same repeated occurrences and a different occurrence as
the source of `p/dup -> p/other` could therefore still collapse.  The
integrator now computes structural identity from `occurrence-arrangement`, so
both nodes and edge endpoints use occurrence ids.  A planted pattern-grain
identity is caught by
`structural-identity-keeps-occurrence-grain-edge-endpoints`.

The review also changed the unread square seam to preserve an authored
occurrence square when supplied, and made fit evidence recognize an
occurrence by its recorded fragment rather than by assuming occurrence ids are
vectors.  The affected focused gate after these corrections is 21 tests / 92
assertions, zero failures and errors; clj-kondo reports zero errors/warnings and
`git diff --check` is clean.
