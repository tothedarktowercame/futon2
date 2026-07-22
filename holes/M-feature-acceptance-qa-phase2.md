# M-feature-acceptance-qa — Phase 2: build-time feature-card emission

**Status:** IDENTIFY → handoff, reviewed by claude-6.
**Owner mission:** `~/code/futon7/holes/M-war-machine-aif-completion.md`
**Prereqs (both landed + reviewed PASS):** Phase 1 render (`767e4ec`,
`feature` command reads an optional `:feature-card`) and M-strategic-mission-value
(`8361512`). codex-3's strategic fix did NOT touch `full_loop_runner.clj`, so
Phase 2 has a clean runner to work in — no merge coordination needed.

## Goal

Populate the `:feature-card` the Phase-1 renderer already consumes, so a grounded
attempt's `feature <id>` sheet shows the real feature story + things-to-try +
sorry + wiring + proof instead of the honest `⟵ build-time gap` placeholders.

## Design (settled with Joe)

The **author** (the coding agent that produced the commit) emits the feature
card as part of its build deliverable — it knows what it built and how to
exercise it. `:matches-intent?` is the author's **claim**, NOT a verdict; the
operator's section-9 accept/reject adjudicates it (the R16 shape: builder makes
a replayable claim, external adjudication decides). The reviewer MAY add an
optional `:reviewer-note`.

### Feature-card schema (written onto the attempt item)
```clojure
:feature-card
{:built          <plain-language: what capability/behaviour we shipped>
 :want-coverage  <how it maps to the mission's want / intent>
 :matches-intent? <bool-or-short-note — the AUTHOR's claim>
 :things-to-try  [<concrete command/step> ...]     ; how to exercise it
 :fold-ref       <path/id to the mission's <mission>.executed.edn, if present>
 :proof-ref      <path/id to the core.logic / Lean witness, if present>
 :reviewer-note  <optional, from the reviewer>}
```

## Change points

1. **`src/futon2/aif/full_loop_runner.clj`** — in the author-dispatch / build-
   resolution path, extend the author deliverable to carry the feature card.
   PRIMARY path: require the card in the author's structured return (add it to
   the author prompt/contract). If the existing return contract can't cleanly
   carry it, FALLBACK: a dedicated short post-build prompt to the SAME author
   ("describe the feature you built + how to try it") — either way the card is
   the *builder's* claim. Persist `:feature-card` onto the attempt item **only
   for a successful/grounded build** (never for build-failed / agent-unavailable
   / no-selection).
2. **Artifact linking** (read-only): set `:fold-ref` to the mission's
   `<mission>.executed.edn` when it exists on disk; `:proof-ref` to the
   core.logic / DarkTower-Lean witness when present. Discover/link — do NOT
   generate.
3. **Persistence** via the immutable Morning-Brief store (the same store
   `brief/` writes). ⚠️ Heed the store footgun: an upsert REPLACES — append /
   merge the card onto the existing item, do not clobber the record. Bump the
   Morning-Brief item schema-version if the store validates it.

## Constraints

- Do NOT touch selection, ranking, dispatch policy, the forward model, or the
  `efe`/`war_machine`/`policy`/`proposer` code (codex-3's domain). `full_loop_
  runner.clj` IS in scope.
- **Fail-open:** an attempt with no card (older runs, non-grounded outcomes, or
  an author that returned none) renders the honest Phase-1 gaps — that path must
  stay intact and is regression-tested.
- Do NOT run the live full loop, arm cron, or dispatch a real author during the
  build; use stubbed author returns in tests.

## Acceptance bar

- A runner-level test with a **stubbed author returning a card** persists
  `:feature-card` on a grounded attempt item; `feature <id>` then renders
  sections 4/5/7/8 from the card (not placeholders). Reuse / mirror the
  Phase-1 card-present test's expectations.
- `:fold-ref` / `:proof-ref` are linked when the artifacts exist, blank when not.
- Regression: a grounded attempt whose author returns NO card, and a
  non-grounded attempt, both still render the honest build-time gaps.
- Existing `full_loop_runner` + `full_loop_cli` tests stay green; new tests
  cover card-present emission, card-absent fail-open, and non-grounded skip.

## Gates (required before bell-back)

- `clj-kondo` clean on changed `.clj`.
- `futon4/dev/check-parens.el` clean on changed `.clj`.
- `clojure -X:test` green for `futon2.aif.full-loop-runner-test` and
  `futon2.aif.full-loop-cli-test`, incl. the new tests.

## Bell-back

Bell **claude-6** with: summary, commit sha(s), gate results, new-test names,
and a rendered `feature <attempt-id>` for an attempt that now carries a card
(so claude-6 can confirm sections 4/5/7/8 fill in). Do NOT touch cron or run
the live loop.
