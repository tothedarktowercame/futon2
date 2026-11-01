# L5-22 — Re-derive stored mission classifications when the parser changes (claude-2 → codex-14, 2026-11-01)

Basis: `reconciliation/L5-22-D-registry-misses-open-missions-codex14-2026-11-01.md`. Lean: no change.
futon2 `main`; worktree `~/worktrees/futon2/codex14-l5-22`, branch `codex14/l5-22`; never check out a
branch in `/home/joe/code/futon2`. Mode: work. Two behaviours, two commits, one bell.

## The defect

`refresh-mission-substrate!` (`mission_substrate_ingest.clj` / `mission_registry.clj`) compares the
file sha and reports `{:scanned 342 :unchanged 342}`; it never re-derives stored props when the
**parser** changed. Commit `a1419060a` (2026-10-01) stopped treating Status prose ("awaiting Joe") as
an operator gate, but 15 missions ingested before it still carry `:mission/operator-gated? true` with
their Status line stored as the gate line, so they are not live and never reach the field. Q1 fails
on exactly these 15 plus one pathless entity (`6fb9177c-…`, `M-ukrns-wp`, from the retired hinge-log
bridge: `{:scope/role "mission" :mission/relation "relates-to"}` and nothing else).

## Behaviour 1 — classification provenance and re-derivation

Each mission record stores the digest of the classification rule set that derived it
(`:mission/classifier-sha256`, from the parser namespace's source bytes or an explicit version
constant; pick the one that changes when the rule changes and say which). The refresh re-derives
props when the file sha **or** the classifier digest differs from the stored one, and reports
`:reclassified n` beside `:unchanged`. Run it once (serving JVM, reload from main first); the 15
become live; record the before/after live counts and the 15 ids in the bell.

## Behaviour 2 — a pathless mission entity is not a selectable mission

`live-mission?` requires a source path and sha; an entity with neither is typed
`:mission/unselectable-reason :pathless-foreign-entity` and listed under `:registry/non-open` on
the record with that reason, never in the open roster. Do not delete the entity (the hinge-log bridge
is retired; its residue is a finding for the substrate owner, cite the entity id).

## Tests

1. A record whose stored classifier digest differs is re-derived on refresh although its file sha is
   unchanged; one whose both match is `:unchanged`.
2. Live pin: `M-descriptive-essay-of-the-stack` (stale gate line = its Status line) → live after
   re-derivation; a mission with a real `Gate:` line stays gated.
3. `M-ukrns-wp`'s shape → not live, typed reason on `:registry/non-open`.
4. Q1 on the next click: census and enumeration agree, or the remaining difference is listed by id.

Gates: clj-kondo, check-parens, `clojure -X:test :nses` for touched namespaces. Commit; bell claude-2
with the shas, receipt, the live counts before/after, and the refresh report line.
