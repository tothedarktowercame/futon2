# Discovery: executable historical-misclassification steps (S4b)

Date: 2026-10-29. Canonical code plus the retained authority records named
below; discovery only.

## 1. Retire as a correction

`verify-historical` accepts only the historical intent/evaluation/review/
authorization schemas (`src/futon2/aif/meta_medic_authority.clj:15-18,
174-189`). The evaluation must have schema `:wm/meta-medic-evaluation-v1`, the
intent's exact digest and evaluator identity, and `:findings` exactly equal to
the table selected by the intent's replay kind (`:237-251`). The two accepted
tables are literal code data (`:39-55`):

- producer-qualified replay: classification `:producer-qualified-already-satisfied`,
  empty history `:no-injury`, attributable retry `:retry-inconclusive`, while
  unsupported refusal and DONE-at-base remain `:injury`, and removal of either
  guard or qualifier is `:rejected`;
- successful-close replay: classification `:successful-task-outcome`, empty
  history `:no-injury`, attributable retry `:discharged`, while unsupported
  refusal, DONE-at-base, image removal and failure-present are `:injury`.

These are not merely “already-satisfied or successful-close findings”: every
control must be present and exactly valued. The a9 receipt's actual shape is:

> `:findings {:classification :producer-qualified-already-satisfied :empty-history :no-injury :attributable-retry :retry-inconclusive :unsupported-refusal :injury :done-at-base :injury :guard-removal :rejected :qualifier-removal :rejected}`

(`/home/joe/notes/wm-next/offline-repair/authority/a9-evaluation/evaluation-receipt.edn:23-30`).
The 9c62/entry-45 shape substitutes the successful-close seven findings
(`/home/joe/notes/wm-next/offline-repair/authority/45-evaluation/evaluation-receipt.edn:27-34`).

A machine step can compute the positive replay rows: both evaluators call the
pure `meta-terminal-injury/classify` and `integrate` functions, whose current
classification and ingestion rules are at
`src/futon2/aif/meta_terminal_injury.clj:157-219,250-300`. But the historical
grammar does not derive the complete findings map from one reproduction result;
the receipts also cite separately run or previously reviewed negative controls
(a9 receipt `:31-42`; entry 45 receipt `:35-53`). Producing a qualifying
evaluation therefore needs a controlled evaluator that runs and pins all rows.
It does not intrinsically require a human: trust authenticates a seat/session
and exact pin (`meta_medic_authority.clj:163-172,265-271`). Today, however,
codex-12 signed both evaluation receipts (a9 `:1-5`; entry 45 `:1-5`), codex-11
signed the independent qualification reviews (a9 qualification `:1-12`; entry
45 qualification `:15-25,74-79`), and codex-11 is also the authorizer in both
authorization receipts (a9 authorization line 1; entry 45 authorization
`:28-30`). Historical verification requires evaluator and reviewer seats to
differ (`meta_medic_authority.clj:197-198`), but does not require authorizer to
be a third seat.

## 2. What was reproduced

**a9b900a4.** The retained complete run record
`tick-run-record-2026-10-24-d0608eec-...edn` was read, pinned, and passed to
`terminal/classify`; its classification was then integrated into an empty
ledger and a synthetic enacted-retry ledger
(`/home/joe/notes/wm-next/offline-repair/authority/a9-evaluation/evaluate.clj:
6-32,34-77`). The output records:

> `:classification {:classification :non-injury, :non-injury-reason :producer-qualified-already-satisfied, :failure-kind :already-satisfied, :failure-stage :author-wait}`

(`/home/joe/notes/wm-next/offline-repair/authority/a9-evaluation/replay-output.edn:1`).
The command and output pin are sealed in the evaluation receipt (`:18-22`).

**9c622038.** Entry 45 similarly read the retained complete
`tick-run-record-2026-10-26-8be8ff03-...edn`, called `classify`, and integrated
the result into empty and synthetic enacted histories
(`/home/joe/notes/wm-next/offline-repair/authority/45-evaluation/evaluate.clj:
6-36,38-83`). Its output records:

> `:classification {:classification :non-injury, :non-injury-reason :successful-task-outcome, :failure-kind :none, :failure-stage :unknown-stage}`

(`/home/joe/notes/wm-next/offline-repair/authority/45-evaluation/replay-output.edn:1`),
with command/output pin at evaluation receipt `:21-26`.

Thus neither replay drove the original trip/click through the production runner;
both reclassified a retained terminal run record through pure classifier and
integrator code, with synthetic histories for controls. They were hand-authored
offline scripts. The production helper `meta-injury-store/ingest-record-file!`
can pin, parse, classify and transact a record (`src/futon2/aif/meta_injury_store.clj:
205-222`), but it acts on the current ledger and is not a replay-for-one-injury
phase. Searches for a function replaying a retained trip or click by injury ID
in `meta_self_heal_production.clj`, `meta_repair_coordinator.clj`,
`meta_terminal_injury.clj`, and `meta_injury_store.clj` found none.

## 3. Was the producer located before “not a defect”?

Yes in both historical episodes, but not as a retained locate receipt. The
recorded three-step cascade omits a locate step that actually happened.

For a9, patch `36afa9142` is titled “Classify qualified already-satisfied closes
as non-injuries” and changes `meta_terminal_injury.clj`; the evaluation intent
retains it as `:patch-revision` and separately names the later classifier
revision (`a9-evaluation/intent.edn:18-20`). The replay receipt explicitly says
the retained record classifies under that later source as producer-qualified
non-injury and retains the patch as provenance (`a9-evaluation/receipt.md:7-11`).
So the classifier producer was located and repaired before the disposition
replay decided that this retained occurrence was not an injury.

For 9c62, patch `aa8bf5ed5` changes the same classifier to conform the close
outcome image; the intent binds that patch, classifier revision, and successful
close replay (`45-evaluation/intent.edn:18-20`). The review says it
“Independently reproduced successful-close non-injury classification” and
checked the repair revision's ancestry (`45-evaluation/coordinator-review.md:
7-9`). Again, locate preceded the decision.

The runtime mirror exposes the mismatch: the historical cascade lists only
reproduce, decide, retire (`src/futon2/aif/repair_cascade_g.clj:45-49`), while
the decision guard requires both `:producer-located` and `:reproduced`
(`:51-63`). Therefore `admitted-steps` admits only reproduce and retire, as the
existing test records; the historical evidence supports inserting locate, not
weakening the guard or claiming the decisions were made without localization.

## 4. Smallest executable phase sequence

The smallest faithful coordinator sequence is five effect phases; it cannot be
implemented only by renaming today's author/review/enact/retry ports:

1. **`:locate` port** — reads the pinned occurrence/source and names the
   classifier producer/revision. Seal a normal coordinator port receipt for
   phase `:locate`. No existing production function returns this evidence;
   a locate receipt payload grammar is needed.
2. **`:reproduce` port** — calls `meta-terminal-injury/classify` and `integrate`
   on the retained record plus controlled histories. Seal phase `:reproduce`
   with pins to record, classifier revision and replay output. The functions
   exist (`meta_terminal_injury.clj:157-219,250-300`), but no production port or
   receipt payload grammar binds this replay to the chosen injury.
3. **`:evaluate` (decide) port** — runs the replay-specific positive and
   negative controls and seals the existing `:wm/meta-medic-evaluation-v1`
   receipt. `meta-medic-authority/verify` can verify an assembled bundle
   (`meta_medic_authority.clj:533-555`), but no existing function constructs
   the evaluation or its exact findings; production/construction grammar is
   needed, not a new medic verification grammar.
4. **`:qualification-review` port** — a different seat reruns/reviews the
   evaluation and seals the existing
   `:wm/meta-medic-qualification-review-v1` receipt with verdict
   `:historical-misclassification-qualified` (`meta_medic_authority.clj:
   252-257`). No production constructor exists; only verification does.
5. **`:retire-correction` port** — obtains the existing scoped authorization,
   assembles the final operation, resolves its verifier with
   `meta-medic-authority/resolve-context` (`:557-597`), then calls
   `meta-injury-store/transact-medic-disposition!`, which locks, verifies,
   appends, revalidates and atomically writes (`meta_injury_store.clj:159-203`).
   Seal the coordinator phase receipt around that result. Existing intent,
   authorization and medic-operation grammars are sufficient; the missing part
   is a coordinator payload grammar binding these pins/result to the selected
   cascade step.

I searched the canonical four production/store files above and the retained a9
and entry-45 evaluation/adoption records. I did not inspect tests beyond the
S4 discovery's cited admitted-count result, other injuries, or noncanonical
worktrees.
