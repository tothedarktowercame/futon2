# M-feature-acceptance-qa — QA as feature acceptance, not decision handholding

**Status:** IDENTIFY → Phase 1 handoff to codex-4, reviewed by claude-6.
**Owner mission:** `~/code/futon7/holes/M-war-machine-aif-completion.md`
**Date:** 2026-07-17. Operator: Joe. Ground Control: claude-6.
**Orthogonal to:** `M-strategic-mission-value.md` (codex-3, forward-model). This
touches `full_loop_cli.clj` rendering only — NOT the selection/forward-model
path and NOT (in Phase 1) `full_loop_runner.clj`.

## The reframe (Joe, 2026-07-17)

Today's Morning-Brief `review` questionnaire grades the *machine's selection
decision* ("was rank-9 defensible, was evidence sufficient"). Joe: that is
**handholding**. Real QA, as in a normal software shop, is **feature
acceptance**: here is the selection, the cascade, the sorry, the wiring
diagram, the logic proof, and **above all the feature we actually built — does
it match what you had in mind? — and here is what to try.** The verdict is
accept/reject the FEATURE. The decision questionnaire demotes to an optional
"stats" appendix.

## Canonical target artifact

claude-6 hand-produced the target sheet for attempt-022 (see the conversation /
the sections below). Phase 1 must render exactly that shape.

### Sheet sections (in order)
1. **Header** — `QA — <attempt-id> · Feature Acceptance`; one-line "what we
   shipped"; repo/commit, author, reviewer (+ tool-event count, approved?),
   grounding (dial moved?).
2. **Selected mission** — target + rank/of-N + one-line note (rank is
   calibration context only, per the paper — say so).
3. **The cascade** — `:patterns-used` (the composed patterns).
4. **The sorry / proof-hole** — rendered if present; else honest
   `not rendered for this attempt ⟵ build-time gap`.
5. **Wiring diagram (fold boxes/wires)** — link/summarise the fold from
   `<mission>.executed.edn` if discoverable on disk; else honest blank.
6. **Logic proof** — the core.logic / DarkTower-Lean witness if linked; the
   grounding `artifact-binding` (fresh-author?/descendant?/in-author-window?)
   is the *grounding* witness and is always shown; behavioural proof honest
   blank if absent.
7. **The feature — does it match intent?** — from the `:feature-card` (Phase 2)
   if present; else honest placeholder `build-time feature card pending`.
8. **Things to try** — from the `:feature-card` if present; else the generic
   evidence commands (`git -C <repo> show --stat <commit>`, the author-cited
   test/verify commands parsed from the commit body if available); mark
   generated-vs-authored honestly.
9. **Verdict line** — `▢ accept feature ▢ accept with follow-ups ▢ reject`.

## Phase 1 scope (THIS handoff — codex-4)

Add a `feature` render command to `src/futon2/aif/full_loop_cli.clj`, sibling to
`brief`/`review`/`qa`, built on the existing `attempt-brief` accessor:

- `clojure -M:wm-full-loop feature <attempt-id>` → prints the sheet above.
- `--format edn` → the same content as data for programs.
- **Assemble from existing record fields**: selection (`:selection-review`,
  `:ranked-candidates`, `:selected-target`), cascade (`:achievement :build
  :patterns-used`), commit/repo (`:commit`, artifact-binding `:repo`),
  artifacts (`:build :artifacts`), review (`:validation` author/reviewer
  executed, `:review-job`, `:approved?`), grounding (`:witness`,
  `:achievement :discharge :dial :moved?`).
- **Optional `:feature-card` slot** (Phase 2 populates it): a map
  `{:built <plain-language what we built>
    :want-coverage <how it maps to the mission's want / intent>
    :matches-intent? <bool-or-note>
    :things-to-try [<command/step> ...]
    :fold-ref <path/id to executed.edn>
    :proof-ref <path/id to logic witness>}`.
  When present, sections 4–8 render from it; when absent, render the honest
  placeholders named above. **Do NOT fabricate** a feature story or
  try-it steps at render time — a missing card renders as a visible gap.
- **Fold discovery**: if `<mission>.executed.edn` exists on disk for the
  attempt's mission, link/summarise it (box count, want-coverage magnitude);
  else blank. Read-only.

## Phase 2 (follow-on, sequenced AFTER codex-3 lands — do NOT start now)

Build-time emission of the `:feature-card`: the author (or a dedicated
post-review step) writes {built, want-coverage, matches-intent?, things-to-try}
against the diff, persisted on the attempt via the immutable Morning-Brief
store; link the fold + logic witness. This touches `full_loop_runner.clj`'s
POST-build section, which overlaps codex-3's file — so it is deliberately
deferred and will be a separate handoff with claude-6 coordinating the merge.

## Acceptance bar (Phase 1)

- `feature <attempt-id>` renders the full sheet for a grounded attempt
  (test against attempt-022's record) with sections 1–3, 6 (grounding),
  9 populated from real data, and 4/5/7/8 showing honest placeholders when the
  `:feature-card` is absent.
- Given a fixture attempt WITH a `:feature-card`, sections 4/5/7/8 render from
  it.
- `--format edn` round-trips.
- Existing `full_loop_cli` tests stay green; new tests cover both
  card-present and card-absent renders.
- No change to selection, ranking, dispatch, or the forward model.

## Gates (required before bell-back)

- `clj-kondo` clean on changed `.clj`.
- `futon4/dev/check-parens.el` clean on changed `.clj`.
- `clojure -M:test` for `futon2.aif.full-loop-cli-test` green incl. new tests.

## Bell-back

Bell **claude-6** with: summary, commit sha(s), gate results, new-test names,
and the rendered `feature attempt-022` output pasted in so claude-6 can compare
against the canonical target sheet. Do NOT touch cron or run the full loop.
