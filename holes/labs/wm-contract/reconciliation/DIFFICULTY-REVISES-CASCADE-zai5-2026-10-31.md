# DIFFICULTY-REVISES-CASCADE — discovery report (zai-5, 2026-10-31)

Row: DIFFICULTY-REVISES-CASCADE (tracker edit pending; packet from claude-1
treated as the row). Discovery only; no source edits, no click, no canonical
write. Base: origin/main `3b8440263`.

Joe's bar (verbatim, 2026-10-31): "the formative learning was supposed to be:
if we need to overcome a difficulty we should see if we can find a way out,
because that updates the thinking/planning from the cascade!!!" So a difficulty
overcome must leave a **revised or extended cascade, or a new or revised
pattern**, that changes later thinking/planning — not only a corrected answer.

## (1) How many times has revise-from-blocker run for real?

Counted over `data/wm-pattern-feedback/events.edn` (20 events, 2026-10-01 →
2026-10-31) and all 822 records in `data/wm-runs/`.

- The events file carries 7 `:wm/provisional-cascade-revision-v1` schema hits:
  2 with `:status :revised` and 1 with `:status :refused,
  :kind :whole-mission-source-unavailable`; the remaining 4 are the same
  structures embedded in later events' `:latest` projection (copies, not new
  attempts).
- **Exactly one real revision ever succeeded**: run `2026-10-02-1790901017`
  (target `M-autonomous-pattern-lifecycle`, blocker
  `:review-request-changes`). Its `:history` records ordinal 0 identity
  `6cf2090f…` (head scope) → ordinal 1 identity `3f8bfa71…` (whole-mission
  scope). The two `:revised` events are both this run (same revision recorded
  on two turns of the continued attempt `1790902540`).
- **The one revision was a no-op on content**: `:delta {:added [], :removed [],
  :retained [all four patterns]}` — the whole-mission skim re-produced the
  same pattern set with different receipts, so the "way out" changed nothing
  the cascade thinks with. Identity changed only because receipts are part of
  `candidate-identity` (`cascade_revision.clj:15-19`).
- **Never applied, never consumed.** The revised identity `3f8bfa71…` appears
  in exactly two run records (`tick-run-record-2026-10-02-1790902540…` 20×,
  `tick-run-record-2026-10-04-d197fe96…` 6×) and in **every** occurrence it is
  inside a `:history [...]` field of a retained revision receipt — never as a
  `:selected-action`, `action-sha256`, or enacted-action digest. No later run's
  selection, construction, or enactment ever referenced it.
- The 8 `:whole-mission-context-required` refusals seen in run records of
  2026-10-09/10 are also copies: they sit inside feedback-metadata `:latest`
  quoting the original 2026-10-04-d197fe96 event. Not new attempts.
- So: **run for real twice (one revised, one refused), zero applications,
  zero later consumptions, and the single success revised nothing.**

## (2) Which difficulties reach it today?

Only one door: `run-revision-round` (`full_loop_runner.clj:4658`) fires
`cascade-revision/blocker` + `revise-from-blocker` (:4672–4677) **only** when
the full-loop review job finishes `done` carrying REQUEST_CHANGES findings AND
`(:revision-rounds opts)` is positive. Everything else does not reach it:

| Difficulty | Reaches revise-from-blocker? | What happens instead |
|---|---|---|
| Full-loop review REQUEST_CHANGES, `revision-rounds > 0` | **Yes (the only door)** | whole-mission skim → revised candidate → `apply-to-construction` |
| Reviewer refusal on the **open-cascade path** | No | objection round (a5b9bbd91) revises the **answer** only; second refusal closes typed `:open-cascade-review-refused` |
| Delivery refusal (`patch-delivery/check-delivery`) | No | typed error before hot-load; nothing revised |
| Claim mismatch (`:claim-matches? false`, `:author-response-defects`) | No | computed and appended (`open_cascade_resume.clj` validate-product), read by nobody — disclosure-correct, consequence-free |
| Guard refusal (`:selected-action-not-provisional-cascade`, `:typed-blocker-required`, …) | No | typed refusal retained in the receipt; no reader |
| Stop line | No | stop-lines are kept in force and only re-reviewed; never trigger a cascade change |
| Mid-run blockers recorded in feedback events (e.g. `:target-has-no-mechanical-acceptance-declaration`, event 1) | No | recorded in the event, but nothing at a later run's start feeds a past blocker into construction |

Note also `revision-rounds` defaults to 0 (`run-revision-round`'s
`(pos? (long (or (:revision-rounds opts) 0)))`), so even the one door is
closed unless explicitly opened — consistent with 15/20 events recording
`:cascade-revision {:status :absent, :reason :no-mid-run-cascade-revision}`.

## (3) Can a held open cascade be revised at all?

**No, not through this machinery.** `revise-from-blocker`
(`cascade_revision.clj:132`) first checks
`(not= :cascade-candidate (:kind original))` where `original` is
`(:selected-action construction)`. On the resume path there is no
construction and no selected action at all: `open-cascade-resume-handler`
(`full_loop_runtime.clj:538`) recognizes the held error and builds an
**authoring requisition** (`wm/open_cascade_resume.clj:158-190`,
schema `:wm/open-cascade-authoring-requisition-v1`) carrying `:cascade-digest`,
`:phase`, `:holes`, author/reviewer. It has no `:kind` and is not a
`:cascade-candidate`; handing it to `revise-from-blocker` would refuse with
`:selected-action-not-provisional-cascade` (or earlier, since there is no
`construction` map in the resume opts at all). The cascade on the resume path
is identified only by digest — it is not a revisable object there.

If it were made revisable, the natural granularity the existing machinery
supports is **clauses in the existing one**: `revise`'s contract is
pattern-set deltas (`:delta {:added :removed :retained}`) over the same
target, with append-only `:history`. A *new* cascade would just be another
candidate with the same target; an *added hole* is not expressible in the
revision schema at all (holes live in the requisition, not the candidate).

## (4) How does a new or revised pattern get written and reviewed today?

- **Write**: there is no run-driven writer. `futon3/library/*.flexiarg` files
  change by human/auto-sweep commits in the futon3 repo (recent log:
  `2dfd512 futon3: add 1 file (auto-sweep)`). Nothing in futon2 writes a
  pattern file.
- **Review**: only whatever the human commit process provides. The
  LEARNING-pattern-cascade build packet (`holes/problems/BUILD-packets/
  LEARNING-pattern-cascade.md`) step 2 — independent review bound to candidate
  bytes, publication against an exact parent version, stale-parent refusal —
  is explicitly **"not yet implemented"** (status line: "adapter, publication
  integration and composed experiment not yet implemented").
- **Read by a run? Yes, this half is built.** Runs read the library and pin
  it: `enact.clj:90-105` slurps the flexiarg prose (`prose-fn`);
  interpretation receipts pin `futon3/library/musn/*.flexiarg` sha256 (visible
  in the events file); `pattern_registry.clj` reads the library root. And
  selection consumes cross-run experience: every close appends a
  `:wm/cascade-execution-feedback-v1` event (`cascade-feedback/record!`,
  wired at `full_loop_runner.clj:7222-7245`), and the next run's selection
  multiplies habit mass by the pattern evidence prior
  (`policy.clj:377 attach-pattern-evidence-menu`, metadata loaded at
  `full_loop_runtime.clj:1230 load-construction-metadata`).
- **Built vs designed**: built = reading/pinning the library + empirical
  per-pattern evidence prior (counts of successful/supporting/incomplete
  applications). Designed only = episode-to-proposal adapter, reviewed
  publication of a revised pattern, and the consumption receipt of a revision
  (LEARNING packet steps 1–3).
- **Written and never read (existing gap)**: the `:cascade-revision` map and
  everything else in construction-metadata's `:latest` projection
  (`cascade_feedback.clj`, `:latest (select-keys (last events) [:receipt/id
  :run/id :mission-state :blocker :cascade-revision :artifact])`) — no code
  reads `:latest` (grep across src finds no consumer). The revision history,
  the blocker kinds, and the refusal kinds inside events are equally unread:
  `counts` reads only `:patterns :selected`/`:applications`.

## (5) Smallest path that closes the loop

The loop's two ends already exist and are already wired: **writes** at every
close (`cascade-feedback/record!`, now also on objection rounds, a5b9bbd91)
and **reads** at next selection (`attach-pattern-evidence-menu`). The missing
inch is that what is written about a *difficulty and its way out* — the
revision delta — is not one of the things the read side counts. So the
smallest closing change reuses both ends:

1. **Write side (extend the existing event, no new store):** when a
   difficulty is overcome via an objection round or `revise-from-blocker`,
   the feedback event already emitted records the way out as pattern
   applications with a typed outcome — e.g. `{:pattern <id> :status
   :successful :evidence {:difficulty {:kind :review-request-changes}
   :way-out :objection-round-resolved}}`. A pattern ADDED by a revision that
   then carried a resolved round counts as a `:resolved-after-difficulty`
   application; a pattern that was present and got the same blocker twice
   counts `:incomplete` (already true today on second refusal).
2. **Read side (one clause in `pattern-evidence-prior`, `cascade_feedback.clj:258`):**
   count `:resolved-after-difficulty` like (or slightly above) `successful`,
   so a pattern that demonstrably got a run out of a blocker earns prior mass.
   Nothing else changes: same metadata schema, same menu attach, same E term.
3. **The test (this is the acceptance, not the deposit):** after a real
   difficulty is overcome, a LATER run's ranked-action must carry
   `:habit-provenance :pattern-feedback` rows naming the way-out pattern with
   a nonzero count and a shifted `:factor` — machine-checkable in the later
   run's retained selection record against the earlier event. Deposit without
   that consumption line is failure, per the row's own bar.

What this proposal deliberately does **not** build, because it would be
written and never read: a standalone revision registry file; a new
`:latest`-style projection field; publishing revised flexiarg bytes from a
run (LEARNING step 2 — right to leave designed until a reader exists); and a
new "cascade-difficulty" event type alongside the existing one.

**Worked case — adc46631 hole-0.** The reviewer refused on consumes/produces
divergence (job `invoke-1793473761172-8882-652e3bf0`). Under the proposal the
way out would have been recorded as: blocker kind
`:open-cascade-review-refused`, difficulty `hole-0 consumes/produces
divergence`, way out `:objection-round-resolved` (if the author's amendment
had resolved it) attached to the patterns of the hole's cascade digest; and
the *cascade-level* lesson — this cascade's operational clause schema lets an
author declare `:produces` that the source never produces — would surface as
one `:incomplete` application for the cascade's patterns when the second
refusal closed, which the next selection for that target already reads. As it
actually happened (second refusal / typed close), only the penalty leg fired;
the revision leg had nothing to attach to, which is exactly the gap this row
names: the answer got a second chance, the cascade never did.

## Pins

- `cascade_revision.clj` (revise :76, revise-from-blocker :132,
  apply-to-construction :160) at 3b8440263.
- `full_loop_runner.clj` :4672-4677 (sole call site), :7222-7245 (event write).
- `full_loop_runtime.clj` :538 (open-cascade-resume-handler), :725-770
  (objection-round feedback event), :1230 (metadata load).
- `wm/open_cascade_resume.clj` :158-190 (authoring-requisition — no :kind).
- `policy.clj` :377; `cascade_feedback.clj` :258 (prior), :232-240 (:latest,
  unread).
- Data: `data/wm-pattern-feedback/events.edn` lines 4-6; run records
  `2026-10-02-1790901017`, `2026-10-02-1790902540`, `2026-10-04-d197fe96`.
- Docs: `holes/problems/BUILD-packets/LEARNING-pattern-cascade.md`,
  `holes/labs/wm-contract/runs/LEARNING-design-proposed-2026-09-09.md`.
