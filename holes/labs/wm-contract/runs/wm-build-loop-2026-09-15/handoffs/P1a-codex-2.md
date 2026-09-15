# Handoff P1a to codex-2: work-target belief module and domain-aware row-7 adapter

From claude-2, the War Machine build lead under Joe's 2026-09-15 commission.
I will review your work independently. **When done, bell claude-2 back with a
summary, the commit sha(s) and the paths to your gate receipts.**

## Goal (one behaviour)

Add a **pure** namespace `futon2.aif.work-target-belief` in futon2. It does
three things:

1. It records which work targets (missions and tickets) a tick's candidate
   population admits.
2. It introduces each newly admitted target once, at a declared initial
   distribution, and carries every previously admitted target forward
   unchanged. Nothing is ever dropped. Every row has a lineage record.
3. It gives the unchanged row-7 reader a domain-aware front door. That front
   door must tell apart an unknown id, a registered id that was never
   admitted, an admitted target whose row is missing, and an admitted
   target that is readable.

This is checklist WM-02 (cascade nodes Q2, Q4, Q6 and Q8) and node R1.
**It is not** an integration into the tick. P1b, a later packet, will do
that. It supplies no observation updates, no B, no A, no prediction and no
serving use.

## Authority: read these first

- The design decision, transcribed as a pinned declaration:
  `futon2/holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/declarations/wm-work-target-interpretation-v1.edn`.
  - Your code must read this file and check its SHA-256. It takes D and the
    interpretation revision from here and must not retype them.
  - A hash mismatch is a typed refusal.
- The packet and the evidence behind it: `.../wm-build-loop-2026-09-15/P1-packet.md`
  and `p1-reproducer/readback.edn`. The reproducer shows that no mission or
  ticket id has a belief row today, and that row 7 gives the same
  `:missing-entity` refusal for a registry target and for an unknown id.
- The existing code:
  - `src/futon2/aif/machine_belief.clj`: the row-7 reader. **Do not modify
    it.** It must keep returning the stored posterior unmodified.
  - `src/futon2/aif/machine_model.clj`: `row-sum-admission`.
  - `src/futon2/aif/mission_registry.clj`: `mission-target-id`,
    `load-missions`, `load-tickets`, `live-ticket?`.
  - `src/futon2/aif/belief.clj`: `status-set`. Note that `reconcile-belief-carry`
    **drops** entities, and that is exactly what this module must not do.

## Inputs and outputs (pure functions: no I/O except reading the declaration)

1. `admissions [candidates registry-snapshot tick-context]`
   - `candidates` are controller candidate entries shaped like
     `{:action {:type .. :target ..} ..}`, or bare action maps. Accept both
     explicitly.
   - A candidate is a work-target admission only if its type is
     `:advance-mission` or `:advance-ticket` **and** its exact `:target`
     string resolves in the registry snapshot:
     - missions through `mission-target-id`;
     - tickets by exact `T-` id.
   - Return:
     - `:admitted`: a map from the exact target string to
       `{:type :registry-id :registry-pin {:path :sha256 :status-class} :admitted-at}`;
     - `:not-admitted`: typed reasons (`:not-a-work-target-type`,
       `:not-registry-eligible`).
   - The registry snapshot is supplied by the caller:
     `{:status :read :entries [..] :read-at ..}` or
     `{:status :unreadable :reason ..}`. If it is unreadable, return a typed
     refusal. Never return an empty admission set.
2. `carry-and-introduce [declaration predecessor admissions tick-context]`
   - `predecessor` is one of these (the caller decides which; P1b wires that):
     - `{:status :present :state <previous work-target state>}`
     - `{:status :absent-pre-genesis}`: no work-target state has ever existed.
     - `{:status :missing-after-genesis}`
     - `{:status :unreadable :reason ..}`
   - The last two are whole-state typed refusals. They must **never** produce
     rows at D.
   - The result state is `{:belief {target posterior} :lineage {target lineage}}`.
   - A target present in the predecessor keeps **its posterior and its
     lineage exactly** (`=`, and keep the ratios as ratios), whether or not it
     is admitted this tick.
   - An admitted target with no lineage in the predecessor is introduced at the
     declaration's `:initial-distribution :masses`. These are exact `1/7`
     ratios; do not use `belief/uniform-prior`, which returns doubles.
   - A target whose lineage exists but whose belief row is absent (or the
     reverse) gets `:carry-missing` for that target. It is never
     re-introduced.
   - Lineage fields:
     - `:introduced-at`
     - `:admission` (the admission record)
     - `:D {:name :revision :authority :declaration-sha256}`
     - `:interpretation-revision`
     - `:decision-ref` (the bell id in the declaration)
     - `:updates :no-admitted-observations`
     - `:information-cutoff`, which is the introduction time and stays
       unchanged across carries, because no update has occurred
   - Include a `:model-context` naming the declaration. The strategic
     `:mu-post` filter manifest is a **different** model; do not merge the two.
3. `target-belief-input [model-context work-target-state registry-ids target]`
   - Return exactly one of:
     - `:entity-outside-registry` (unknown id)
     - `:registered-not-admitted`
     - `:carry-missing`
     - or the unchanged result of
       `machine-belief/belief-state-distribution` called on
       `(:belief work-target-state)`, with the target's lineage attached
       alongside it (not inside `:belief-input`).
   - For an admitted, readable target, the posterior row 7 returns must be `=`
     to the stored row.

## Invariants and rejecting controls (tests required)

Construct fixtures in the shape the production data has: real candidate-entry
shape, and real registry entries from `load-missions` / `load-tickets` (or
pinned copies of them).

Each item below needs a test that would **fail** under the named wrong
implementation.

1. **First introduction.** The masses are exact `1/7` ratios, and
   `row-sum-admission` returns `:exact`.
   - Fails if: doubles or `belief/uniform-prior` are used.
2. **Leaving the candidate set.** An admitted target is absent from the next
   tick's candidates. It survives with an identical posterior, lineage and
   cutoff.
   - Fails if: the carry drops it, as `reconcile-belief-carry` does.
3. **Re-admission.** The target keeps its row and is not re-introduced.
   - Fails if: re-admission resets the target to D.
4. **Unusable predecessor.** A `:missing-after-genesis` or `:unreadable`
   predecessor gives a whole-state refusal and no D rows.
   - Fails if: this is treated as a cold start.
5. **Half-present target.** A target with lineage but no row, or a row but
   no lineage, gets `:carry-missing`.
6. **Four-way adapter.** The adapter keeps the four cases distinct.
   - The unknown id `X-...` is `:entity-outside-registry`.
   - A registered, never-admitted id is `:registered-not-admitted`.
   - Both of these must differ from the `:missing-entity` that plain row 7
     gives.
7. **Readable target.** For an admitted target, the adapter returns row 7's
   `:ok` result and the posterior is `=` to the stored row.
   - Row 7's own tests stay green and row 7 is untouched.
8. **Admission filtering.** A candidate is not admitted if its type is
   something else (for example `:fire-pattern` or `:open-mission` on a
   `futon4-d/mission/...` endpoint), or if its target is not in the
   registry. An unreadable registry refuses.
9. **Declaration pin.** A mismatched declaration hash refuses.
10. **No update path.** There is no function that changes a carried posterior.
    Every lineage says `:no-admitted-observations`.

## Out of scope (do not touch)

- `war_machine.clj`, `trace.clj`, `belief.clj`, `forward_model.clj`,
  `machine_belief.clj`
- Lean
- the non-progress consumer
- channel predictors
- any reload, click, or load into the shared JVM (:6768)

If something in this spec cannot be implemented coherently, **stop and bell
me with the concrete conflict**. Do not weaken a control to make a test pass.

## Gates (futon2 root, a short-lived process of your own)

- `clj-kondo --lint` on the new src and test files.
- `emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval "(arxana-check-parens-cli)"`
  on both files. See that file's header for how to pass the file list.
- `clojure -X:test :nses '[futon2.aif.work-target-belief-test futon2.aif.machine-belief-test]'`
- Follow futon2 `AGENTS.md`: run each gate bare, not piped, and record its
  exit status.
- Write the gate outputs to
  `futon2/holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/p1a/`.

## Commit

The worktree is shared. Stage **explicit paths only**: the new src, the new
test and the receipts. Never use `git commit -a`, and never amend a commit
that is not yours. One scoped commit.

Bell claude-2 back with:
- the summary
- the commit sha
- the receipt paths
- any spec conflict you hit
