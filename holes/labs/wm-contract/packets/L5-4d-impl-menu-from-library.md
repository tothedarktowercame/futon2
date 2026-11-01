# L5-4d-impl — Producer: the menu is constructed from the pinned library; the makeup keeps learning state only (claude-2 → codex-11, 2026-11-01)

Base: futon2 `main` at `a4a91d6c2`. Authority: Lean Q2 for the selection kind (`Requirements.lean` L1:
slice from the whole pinned library, pool = slice, compared = constructed = the construction's
policies, non-zero reading/library/graph/kernel pins, at least two compared cascades) and the
L5-4d report (`reconciliation/L5-4d-menu-source-codex11-2026-11-01.md`), whose findings this
packet takes as given: the executable steps are hand-written in the makeup, no library file carries
them, retrieval over the manifest does not exist, and the scorer reads unblocks for gain.

## Decisions (claude-2), so nothing is invented silently

1. **Executable mappings live in the library, versioned and pinned.** Add
   `futon3/library/task-selection/executable-steps.edn`: for each README cascade that can be
   executed over Giraffe facts today, its typed steps, the README line it realises, and a
   `:version`. Start with exactly the three cascades the makeup carries plus any README cascade
   whose every step is one of `:ready-gate`, `{:status-gate s}`, `:by-cone`, `:by-unblocks`,
   `:by-rank`. A README cascade needing seats, repair priority or portfolio facts is listed with
   `:status :not-executable :needs [...]` and is not in the menu. This file is a reviewed
   interpretation of Joe's prose: write it, commit it on a futon3 branch, and name in the bell
   every line of the README each mapping rests on; claude-2 reviews the mapping and Joe is told.
   The mapping file's sha is part of the library pin.
2. **Retrieval is declared, not hidden.** The slice is the set of patterns cited by executable
   README cascades, computed from the whole manifest with the query recorded:
   `{:query :readme-evidenced-and-executable :manifest-sha … :mapping-sha … :included [...]
   :excluded [{:pattern :reason}]}`. Every manifest pattern appears once, included or excluded with a
   reason. Pool = the units the constructed cascades arrange; assert pool = slice.
3. **Cascades are constructed, with digests.** Each menu cascade: `{:id <name> :digest sha256(pr-str
   [name readme-row units edges]) :units [{:pattern :sha256 :index}] :edges [[i j :precedes] …]
   :steps <typed>}`, the same digest scheme as the witness emitter, so production ids and the
   witness fixture's ids agree for the same composition. Edges: the README order, `:precedes`
   between consecutive units.
4. **Pins.** `:construction {:reading <pass sha> :graph <Giraffe input.edn sha, from the run's
   manifest> :library {:root :sha256 :count :mapping-sha} :kernel sha256(pr-str of the makeup's
   retraction, transition-kernel, c-kit and gain-kit parameters) :slice … :pool … :cascades …
   :policy-count n}` recorded in `:outer/selection`. If the input sha is not reachable from the
   observation, record `:graph {:status :absent :reason …}` typed, which Lean then refuses on Q2.
5. **Makeup keeps learning state and kernel parameters; it loses `:cascades`.** `outer_state.clj`'s
   pending replay reconstructs the chosen steps from the record's `:cascades` entry (L5-4a) by
   digest, not by makeup index; an old pending entry without a digest replays by its recorded steps
   if present, else refuses typed `:pending-steps-unrecoverable`. Learning counts are status-keyed
   and unaffected (report §Learning).
6. **Gain for an undetermined pick.** A cascade that gates on `:undetermined` picks a task whose cone
   and unblocks are not measured. Its gain class is `:none` by the meaning of discovery (a reading
   makes nothing ready by itself; the information it yields is the novelty term), recorded as
   `:gain-source :discovery-yields-no-ready`, as the hand witness does for cascade 3. Do not read
   unblocks for it, and do not invent a cone. A cascade that orders undetermined rows by cone or
   unblocks stays refused (`graph-term-unavailable`); the mapping must not produce one.
7. **Projector.** `wm_run_facts.clj`'s selection export reads `:construction` into
   `targetConstruction` (targets = the enumerated open set; action 0; pins; slice; pool;
   `sliceFromWholeLibrary` computed from the recorded manifest and unit shas; policyCount; policies =
   the cascade digests), `comparedPolicies` = `constructedCascades` = those digests, `policyG` keyed
   by them, `libraryPatternCount` from the pin. Old records keep the makeup-position fallback.

## Tests (one live pin each)

- The constructed menu on the retained 11-01 observation has at least two cascades, every unit sha
  equals the library file's, pool = slice, every manifest pattern included or excluded with a
  reason.
- Digest equality: the production digest of the giraffe-order cascade equals the witness fixture's
  `c1.cascade` id when the witness's composition is used (state the id).
- The makeup without `:cascades` loads; pending replay by digest works on a fresh record; an old
  pending entry replays by its recorded steps.
- Undetermined pick: gain `:none` with `:gain-source`; a mapping that orders undetermined rows by
  cone is rejected at mapping load.
- Projector: on a fresh selection export Q2 is `.known`; run the checker and Lean by hand and paste
  Q2's outcome whatever it is.
- The 12 outer namespaces, both projector namespaces, compared with `main`; data counts.

## Gates

kondo, parens, overrides to scratch, no reload, no merge. futon2 branch
`codex11/l5-4d-menu-from-library` in `~/worktrees/futon2-l5-4d`; futon3 branch
`codex11/task-selection-executable-steps` for the mapping file. Bell claude-2 with both shas, the
mapping file's contents with README line citations, the retrieval record, Q2's Lean outcome, test
counts vs main, data counts, and anything that does not fit. If the whole is larger than it looks,
stop after decisions 1–4 with everything green and say so.
