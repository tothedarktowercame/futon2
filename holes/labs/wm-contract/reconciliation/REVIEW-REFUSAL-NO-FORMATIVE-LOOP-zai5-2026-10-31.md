# REVIEW-REFUSAL-NO-FORMATIVE-LOOP — zai-5 discovery (2026-10-31)

Discovery only; no source edits, no click, no canonical write. Case: run
`2026-10-31-adc46631-2156-4c84-850f-5abaa3aeb5fd`, reviewer job
`invoke-1793473761172-8882-652e3bf0`. Read against futon2 origin/main `39e652efb`
(includes `75b074801`, the typed-close fix; and the held-resume detail row's
retained-keys extension, which `75b074801` also touches).

## 0. The case, pinned

The reviewer's actual reply (Agency job ledger, `result-summary`/`result`, job
`invoke-1793473761172-8882-652e3bf0`, state done, 9 tool events): it verified the
committed source (`futon2.aif.wm.authored.req-b9e453fd` at commit `5327b48f…`,
source-digest `7f241c36…cdfb38c` matching, patch var a function, holes 1–17
matching verbatim) and refused on exactly one discrepancy:

> Author answer hole 0 (`cascade-construction/read-what-exists-first`, 0):
> consumes `unrecorded-cascade-repair-transition` / produces
> `auditable-mission-state-transition`; committed source: consumes
> `bare-mission-identifier` / produces `read-anchored-target-identity`.
> … "Resend with hole 0 matching the committed source (or amend the source) and
> I will accept."

The run record retains **zero bytes** of this: structural scan finds no
`"accepted"`, no `accepted-by`, no reason text — only the model-usage row names
the job id (record offset ~96036) and the close says
`{:kind :answer-reviewer-mismatch … :cause {:kind :answer-reviewer-mismatch}}`
(pre-75b074801 mistype; the refusal itself lived only in the job ledger).
After `75b074801` the handler mints
`{:status :error :kind :open-cascade-review-refused :reviewer … :reviewer-job …
:reason (:reason reviewed) :requisition-id … :cascade-digest … :reviewed-reply …}`
(full_loop_runtime.clj:554–570), which flows through the held-resume close into
the record's `:detail` and the repair finding's `:failure-data`. **The only
consumer of `:open-cascade-review-refused` anywhere in src is
meta_terminal_injury's non-injury list** (`explicit-non-injury-kinds`,
meta_terminal_injury.clj:15). Nothing reads `:reason`, `:reviewed-reply`, or
`:author-response-defects`. Joe's gap, confirmed.

## 1. What the ordinary author path does with request-changes — and what is reusable

The ordinary path has a **complete revise-and-resubmit round** already:

- `run-revision-round` (full_loop_runner.clj:4690+, "Run at most one
  revise-and-resubmit round"): when the reviewer's typed negative verdict
  carries nonblank findings (`review-findings-text`, :4539 — "a bare negative
  marker does not authorize another turn") and the revision budget is positive,
  it re-dispatches the author, then re-reviews the amendment.
- `revision-author-prompt` (:4555): "FULL-LOOP REVISION ROUND 2 … REVIEWER
  VERDICT AND FINDINGS (VERBATIM):" + the same acceptance-criterion block as
  the first dispatch (⟨1⟩6) + prior commit shas + "Address the findings without
  widening scope."
- `revision-reviewer-prompt` (:4596): "You are the same independent reviewer …
  Review only that amendment and whether it resolves your **original findings
  without regression**" + ORIGINAL REVIEW (VERBATIM) + the author's pattern
  application claim.
- Budgets: `default-revision-rounds 1` (:172), `default-author-response-corrections 1`
  (:170) for the separate format-level correction loop
  (`author-response-correction-loop`, :4237+ is its refusal site).

The open-cascade handler already uses the **correction shape** for format
failures: `handoff/contact-with-format-correction`
(full_loop_runtime.clj:530→handoff_contract.clj:138, `:format-correction-limit 1`)
and the author prompt has a dedicated slot — "FORMAT CORRECTION (one round
only): the previous response did not parse: …" (runtime:378–382).

**Verdict on reuse:** the handler cannot call `run-revision-round` itself (it
operates on runner-phase state — construction, repo, files, artifact binding,
stop-lines — none of which the held-resume handler holds), but the *shape* it
needs is `contact-with-format-correction`'s: one re-contact of the same author
with the same requisition, carrying the previous failure verbatim, then one
re-contact of the same reviewer with the first review verbatim —
`revision-reviewer-prompt`'s wording transplanted onto
`open-cascade-review-prompt`. Both prompts are handler-local
(`open-cascade-author-prompt`/`-review-prompt`, runtime:353/:390), so the
addition is handler-local too.

## 2. Credit and attempt caps

- **Clicks:** the held-resume retry runs "WITHOUT a new click, WITHOUT
  reselection, under the SAME cascade digest"; the resume protocol *asserts*
  click-budget and selection-counter are unchanged (`open_cascade_resume.clj`
  `resume!` :460–500, `:clicks-spent 0`, `:click-count-unchanged?`). So a
  second author+reviewer round inside the handler costs **zero click credit**
  — the adc46631 click already paid for both agent turns inside one click
  (model-usage rows: author 660 605 tokens, reviewer 122 975).
- **Rounds:** existing policy is uniformly **one**: `default-revision-rounds 1`,
  `:format-correction-limit 1`, `default-author-response-corrections 1`. No
  hard attempt cap exists on held retries beyond the protocol checks, so the
  cap for the new round should be the same policy constant shape: one
  objection round per held resume, then the typed close that exists today.

## 3. How the second review checks the objection was answered, not a fresh opinion

Reuse `revision-reviewer-prompt`'s contract verbatim: the second reviewer prompt
must contain (a) the FIRST review verbatim (for adc46631: the refusal JSON with
the reason, including "Resend with hole 0 matching the committed source … and I
will accept"), (b) the revised author answer, and (c) the instruction to review
**only whether each original finding is resolved without regression**, stating
per-finding resolution. The mechanical pre-check the machine can add for this
objection class costs nothing new: `validate-product`
(open_cascade_resume.clj:295–345) **already computes the claim-vs-source
mismatch** — `claim-matches?` / `:author-response/decisions-mismatch` — and
today records it as a non-gating disclosure (the MISSION-TEXT row's C1 finding).
The reviewer's hole-0 objection is exactly that computed defect. So the loop can
even refuse to spend a reviewer contact until the author's answer matches the
committed source, and the second review's job collapses to confirming the fix
against the stated objection.

## 4. Where the objection, revision and verdict should be retained — and who reads what today

| store | written today | read today |
|---|---|---|
| run record `:failure :detail` | yes, post-75b074801 (`:reason`, `:reviewed-reply`, `:reviewer-job` via `held-refusal-retained-keys`) | diagnosis projections only; **nothing reads `:reason`** |
| repair stop-line finding `:failure-data` | yes (`record-held-resume-stop-line!`, runner:6367, whole refusal map) | repair lifecycle reads findings; findings seed `T-repair-*` targets for selection — but **nothing extracts `:reason` when a repair target is worked** |
| cascade-feedback events (`data/wm-pattern-feedback/events.edn`) | per-run pattern application receipts (`cascade-feedback/record!`, runner:7237+) | **read twice**: `policy.clj:377 attach-pattern-evidence-menu` (selection scoring) and `cascade_revision`'s `pattern-evidence-prior` (revision scoring). Review refusals are never recorded as events. |
| outer-learning manifest | per-run (`outer-learning-manifest/record!`, runner:9920–9929) | only `ticket_readiness.clj:377 read-manifest` (ticket assessment input). Review reasons never reach it. |
| author prompt retrieved context | the open-cascade author prompt embeds the requisition + a format-correction slot | no prior-review slot exists; the fresh requisition's only memory is `:resume-incomplete` missing-fields |

**Written and never read:** the reviewer `:reason` and `:reviewed-reply` (record
+ finding), `:author-response-defects`/`:claim-matches?` in `:answer-delivery`,
and — pre-75b074801, still true of the retained adc46631 record — the refusal
text itself, which exists only in the Agency job ledger.

## 5. Does any such learning reach selection or a later author today?

**No.** The one proven learning channel into selection is pattern-feedback
priors (`pattern-evidence-prior`: successful/supporting/incomplete application
counts → likelihood-ratio factor on the selection menu), and a review-refused
held resume never writes an event — it has no selected-action receipt and no
applications. The next click's author receives a fresh requisition with no
memory of the objection. So the adc46631 objection is typed, retained twice, and
read by no one — Joe's "once-and-dump", precisely.

## Proposed smallest loop (reusing what exists)

In `open-cascade-resume-handler`, when `review-refusal` has a nonblank
`:reason`, run **one objection round** in `contact-with-format-correction`'s
shape: re-contact the same author with the same requisition, prompt embedding
the refusal verbatim + "amend the answer or the source so the objection is
resolved; same patch, same digest"; then re-contact the same reviewer with a
prompt transplanted from `revision-reviewer-prompt` (first review verbatim,
revised answer, per-finding resolution required). Guard the round on the
already-computed `:author-response-defects` (don't spend a reviewer turn on an
answer that still mismatches its source). Second acceptance → continue into
`handle-error!` as today; second refusal → the existing typed
`:open-cascade-review-refused` close, now carrying all three legs
(`:objection`, `:revision`, `:verdict`) in the retained detail. Optionally
publish the round's outcome as one cascade-feedback event — both readers
already exist — so the pattern's `:incomplete-applications` count grows and
selection's empirical prior learns from the refusal.

**Effect on selection:** none required for the loop itself (same click, no
reselection); the optional feedback event is the only selection touch, through
the existing prior.

## The test, built from the real adc46631 reply

Fixture the reviewer contact to return the actual reply (JSON above: accepted
false, the hole-0 reason, "…and I will accept"), the author second-round
contact to return the corrected answer (hole 0 matching the committed source:
consumes `bare-mission-identifier` / produces `read-anchored-target-identity`),
stubs for the first-round contacts; then assert:

1. a second author contact happened, its prompt containing the reason
   **verbatim** (substring of the real reply);
2. the second reviewer prompt contains the first review verbatim and names the
   per-finding resolution requirement;
3. the final result is the accepted path (`:resumed`/`handle-error!` reached),
   and click-budget/selection-counter are unchanged (the protocol's own check
   shape);
4. the retained detail carries `{:objection … :revision … :verdict …}`;
5. negative leg: a second refusal produces the typed
   `:open-cascade-review-refused` close with both rounds retained and **no
   third contact** (round cap 1).

## Method note

Reads only: the adc46631 run record (structural scans quoted), the reviewer job
via the local evidence API (`GET /api/alpha/invoke/jobs/invoke-1793473761172-8882-652e3bf0`),
`75b074801`'s diff, and origin/main source. No click, no store or ledger write;
branch holds only this report.
