# L5-4d — Discovery: where the task-selection cascades live, and what a menu built from the pinned library is (claude-2 → codex-11, 2026-11-01)

**Discovery only: no code changes, no `data/` writes.** Deliverable: one report at
`futon2/holes/labs/wm-contract/reconciliation/L5-4d-menu-source-codex11-2026-11-01.md`, committed
on `main`, that file only. Base: futon2 `main` at `fac22f476`.

## Why

Lean Q2 for a selection run (`Requirements.lean`, L1) requires the construction's slice to be drawn
from the whole pinned task-selection library, the pool to equal the slice, and the compared
cascades to be the construction's policies; `handListedMenu` (three cascades named in a file, no
library pin) fails it, and every era-2 record has that shape because the menu is
`(:cascades makeup)` from `data/wm-learning/outer-makeup.edn`. L5-4a pinned each makeup pattern's
library file; what is still missing is the construction itself: a menu *derived* from the library,
with a reading pin, a graph pin and a kernel pin, so the projector can fill `targetConstruction`
and Q2 can hold on a real selection run.

## Questions (cite file:line for each; "grep suggests" where inferred)

1. **The catalogue.** `futon3/library/task-selection/` has about twenty files. List them. Which
   carry executable steps (gate/order over Giraffe facts) in a machine-readable form, and in what
   notation? Which are prose only? Where are the README's "evidenced cascades" stated, and do they
   name their steps or only their patterns? Is there a `select-tasks-from-cascade-live.flexiarg` or
   similar, and what does it execute?
2. **Makeup versus library.** For each cascade in the live `outer-makeup.edn`, which library files
   does it cite, and do its typed steps (`:ready-gate`, `{:gate-status …}`, `:by-cone`,
   `:by-unblocks`, and since L5-4a `:by-rank`) come from the library file or were they written by
   hand in the makeup? If by hand, say so plainly: that is the hand-listed menu.
3. **A constructed menu.** Describe, without implementing, how the menu would be built from the
   pinned library at click time: the manifest (L5-4a reused `q2-construction/library-manifest`
   over the directory), the slice (which patterns, by what retrieval over the whole manifest), the
   pool (the patterns the cascades arrange), the cascades (compositions the README evidences, with
   unit shas and edges), typed steps per cascade, and the three pins the Lean construction needs
   (`reading` = Giraffe pass sha, `graph` = Giraffe input sha, `kernel` = the retraction and
   kernel parameters' digest, which today live in the makeup). Say which of these exist today and
   which have no source.
4. **Learning state.** What in the makeup is learning state (q per cascade, decay) rather than menu,
   and would stay in a learning file once the menu comes from the library? Does `outer_state.clj`
   key its q by cascade name, and would library-derived cascade ids break it?
5. **The undetermined-cone incompatibility.** L5-4b left undetermined rows in the field and the
   live makeup's cascades order by cone, which refuses. Which README cascades gate on status before
   ordering, and which would need a `:status-gate` to be executable on the real field?
6. **Readers of the menu shape.** Who reads `(:cascades makeup)` or `:tied-cascades` by name
   (`src/`, `scripts/`, `test/`, futon3c `src/`), so the implementation packet can list them.

## Gates

Read-only. Keep claims tied to paths. Bell claude-2 with the report path.
