# E-kimi-task-95 — C14-P8-AS-R3-REFERENCE-D: does the class observable have an A-S R3 reference (discovery)

**Requisition:** in-progress — dispatched 2026-09-27T02:32:41Z to kimi-6 as invoke-1790476361891-25245-56ce0e92

Clocked in by claude-8 for kimi-6 on 2026-09-27 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# C14-P8-AS-R3-REFERENCE-D: does the class family's observable have a reference under A-S Revision 3? (⟨1⟩4 of PROOF-2a-PLAN, compatibility check 5; discovery only)

C14-P8-AS-R3-REFERENCE-D (claude-8 → kimi-6), discovery under the coding-handoff protocol: you find out, claude-8 reads; bell claude-8 back with the report below. NO commit, no edit to any tracked file, no run of any test, no flight, no click, no load into :6768. Work read-only from `/home/joe/code/futon2` at HEAD; `git status` must be as you found it when you finish (the registry is dirty from another lane: do not touch it).

The question, from codex-2's continuation (`holes/labs/wm-contract/PROOF-2a-CODEX-CONTINUATION-2026-09-26.md` :72–:95, check 5): "Per-class reference mechanism: must be checked against A-S Revision 3 and its implementation. This continuation has not independently accepted those packets or asserted that a reference exists for a new observable." kimi-2's C14-D (futon2 ad561637f, §check (5)) found no later commit closes it. You settle what the check means and what its answer is at HEAD.

Read, in this order, and quote the lines you rely on:
1. A-S Revision 3: `holes/labs/wm-contract/proof2/packets/A-S.md` from `## Revision 3` (line 182 on): what a REFERENCE is for an observable (the token labels: a checker mechanism whose identity is part of the label key, recorded vs admitted, per-cell minimum 5, a changed mechanism is a new population), and what §2's "reader's rules" require before a rate for a class can be consumed.
2. The implementation of that reference for tokens: `src/futon2/aif/observation_labels.clj`, `observation_label_store.clj`, `observation_label_reader.clj` (Revision 3's reader), and the consumers at `scripts/futon2/report/war_machine.clj:5905–5935` (the lane's sourced rates) — say what "reference" concretely is there: which mechanism string, which key, which counts.
3. The class family's observable: `src/futon2/aif/observation_model.clj:195–232` (the class emission and the `:class-unknown-no-scalar-g` refusal), `scripts/futon2/report/war_machine.clj:6202–6230` (class-observation-model, the 55/35/5/5 literal, `:status :synthetic :calibrated false`) and `:6632` (scorer-class), `src/futon2/aif/focus_receipt.clj:244` (classify-target), `src/futon2/aif/cascade_observation_scoring.clj:144–160`. The observable here is a target's CLASS (focused / related / unrelated / stop-the-line) as emitted from a corpus relation row. Under Revision 3's definition: does that observable have a reference — a mechanism that labels the class independently of the model, with recorded-vs-admitted labels and a population — or is its A the model's own emission with no measurement behind it? If the latter, name exactly what is missing (mechanism identity, label store, reader) and what already exists that could serve (the relation rows? the focus receipt?).
4. Whether any test or record at HEAD asserts a per-class rate or label for the class observable (grep `test/` and `data/wm-runs/` for `class-preference`, `target-class`, `:class-unknown`, `scorer-class`; count only, no run).

Report (bell claude-8): Revision 3's definition of a reference in one paragraph with line numbers; the token reference as implemented (mechanism, key, counts) in one paragraph; the answer for the class observable: REFERENCE EXISTS / REFERENCE ABSENT / PARTIAL, with what exists and what is missing, each with a path:line; what a closing packet would build (files, ~lines) and whether it needs a ruling from Joe (for instance: is the class label a thing the machine may measure, or a stipulation that needs no reference?); anything you found that is not yours, unfixed. Say what you read and what you did not.
