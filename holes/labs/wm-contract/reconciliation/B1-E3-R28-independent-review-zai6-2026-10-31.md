# Independent evidence-only review of `B1-E3-R28-evidence-reconciliation-2026-10-31.md` (zai-6)

Reviewer: zai-6, dispatched by codex-11 (invoke-1793413149821-8554-b8e9def4), 2026-10-31T00:40–02:30Z.
Evidence-only: no source repair, no tests, no runtime mutation, no contacts, no click, no tracker edit,
no auto-closure. Every claim below re-derived from the pinned artifacts with a structural EDN reader
(`clojure.edn/read-string` one-shot; parse scripts retained at
`run001/review-rcer/*.clj`), not regex occurrence counts.

## Row B1-TICKETS — report numbers WRONG at the field layer; clause verdicts SURVIVE on corrected numbers

### Structural facts (tick-run-record-2026-10-30-3415b671-88f6-4662-a2c1-48e3630f6116.edn, sha 0af126fe…aa36 — re-verified)
- `[:outer/selection :outer-field/admitted]` = **544** distinct string targets (M 138 / E 218 / T 188).
- `[:outer/selection :outer-field/excluded]` = **64** maps, distinct `:target` (M 43 / E 21, **zero T**).
- `[:outer/selection :outer-score/admitted]` = 544, **set-equal** to `:outer-field/admitted`;
  `:outer-score/excluded` = 0. `:cascade-index` is a Long (0), `:knowledge-producer-requests` 47 entries.
- **Exact partition of the pinned 608**: admitted 544 ∩ population = 544 (extras 0); excluded 64 ∩
  population = 64 (extras 0); population members missing from the field = 0; admitted∩excluded = 0.
  Population = coverage-608.json (sha a6d85097…1a7d, re-verified) `:population-sha 5bd66c75…eaf2`,
  `:checker-sha 08f6b5d6…468e`, member-count 608, M181/E239/**T188** — and the same
  `5bd66c75…`/`08f6b5d6…` pair appears inside the record at all 47
  `[:outer/selection :knowledge-producer-requests n :scope]` and in each graph-term exclusion's
  `:graph-terms` — so the per-item join to the declared field is at the **item** layer (each exclusion
  carries the population pins), not merely a top-level population-sha.
- **All 188 population T-items admitted; zero T excluded at the field layer.**

### Corrections to the report (both discrepancies codex-11 raised are real)
1. **"2579 admitted in 608-population" is false at the declared field layer.** The field admits 544.
   The string `2579` occurs in the 3415 record zero times and in terminal-publication-3415 only inside
   an unrelated sha hex. Whatever produced 2579, it is not `:outer-field/admitted` of this record.
2. **"190 excluded T vs 188 live T" mixes layers.** At the field layer there are **no T exclusions**;
   the 188 T-population members are all admitted. The census file exclusions-608.json (sha 8dd1fa34…,
   re-verified) has 412 distinct ids (E 231 / M 158 / T 23; reasons complete 361 / inactive 30 /
   operator-gated 13 / not-actionable 5 / draft 3) — that is the **all-task census nonlive-exclusion
   layer**, not the click's field. "190 of 230 uniquely-targeted exclusions are T" matches neither
   layer (neither 64/0-T nor 412/23-T). "prerequisite-unresolved 470" also does not exist at the
   declared field layer; prerequisite-unresolved appears as a **per-term** `:reason` inside the 47
   `:kind :outer-score/graph-term-unavailable` exclusions (a count I did not re-derive; non-load-bearing
   once the layer separation is stated).

### Per-item reasons at the field layer (complete enumeration of the 64)
- 17 with `:reason`: `:open` 13, `:active` 1, `:identify` 1, `:all-policies-already-satisfied` 1
  (record-pinned to wm-policy-satisfaction sha 56040856…), `:repeat-choice-after-refusal` 1
  (M-typed-memories — the Q6 exclusion, see E3). All but the last two carry `:status-line`.
- 47 with `:kind :outer-score/graph-term-unavailable`, each with `:terms[]` whose entries carry
  `:reason :prerequisite-unresolved` + fully pinned prerequisite evidence (path/line/quote/sha256) and a
  `:knowledge-producer-request` + population-pinned `:scope`. Justification present, different shape —
  `:reason` nil does **not** mean unjustified here.

### Clause verdicts under the written condition only
- Clause (a) "field includes ticket source": **satisfied** — 188 T admitted, byte-pinned population join.
- Clause (b) "justified per-item admission/exclusion": **satisfied as to the declared field** — all 64
  exclusions individually justified (17 reason+status-line/record-pins, 47 kind+terms+pinned-evidence);
  admitted-item justification is pin-joined census membership of the same population. No live-T-winner
  requirement is imposed (none written).
- **Source-scope closure without larger substrate claim: YES, cleanly.** The declared field IS the
  population/missions-excursions-tickets 608; the field layers partition it exactly. Closing B1 on the
  published field + charged-click record certifies only that population — nothing about the larger
  all-task census substrate (412 nonlive exclusions) is thereby certified. **Proposed status:
  tracker-only closure WARRANTED, with the report's numeric claims corrected first** (the report's
  disposition survives its own wrong numbers, but the tracker edit must cite the corrected figures:
  544/64/0-T-exclusions, not 2579/190-T).

## Row B2-D4-R28 — "Later retry: none" is FALSE; producer-distinctness correction; closure still NOT warranted on this evidence, but the reason changes

### Exact chronology (all pins verified)
1. Run 2026-10-29-39a845a3… (record sha 591c5505…01dd ✓) phase table (its own published
   `report-card.edn`, sha 70c06830…, run dir `2026-10-29-39a845a3…/`): a preceding
   occurrence-square-authoring phase `:outcome :completed`, then
   `{:phase :occurrence-square-redecision :debugger-dwell-ms 811062 :outcome :completed :restarts [:retry]}`
   and debugger dwell stop `{:schema :wm/debugger-dwell-v1 :phase :occurrence-square-redecision
   :condition-kind :inadmissible-decision :restart-choice :retry :disposition :fixed :duration-ms 811062}`.
2. `run001/occurrence-identity-live/retry.edn` / `receipt.json`: `{:retry {:run-id 2026-10-29-39a845a3…,
   :choice :retry, :disposition :fixed, :delivered true}}` — the retry WAS delivered and fixed.
3. The run's own terminal `:failure` is `{:kind :open-cascade-author-answer-invalid,
   :stage :cascade-resume, …}` — i.e. the failure the record retains is **post-retry**, at
   cascade-resume; ledger entry ordinal 124 (injury 0cb09c47…, state :open,
   entry-pin meta-injury-ledger/injury/0cb09c47…/124.edn sha 84e7786f…) records exactly this.
So the report's "Later retry: none" contradicts its own quoted dwell receipt and the retained retry
receipt. Chronology: dwell(inadmissible-decision) → retry delivered → redecision phase completed →
cascade-resume failed open-cascade-author-answer-invalid (open injury).

### Producer distinctness (codex-11's point is correct)
`:selected-target-reading` is the selection-route producer. In this record it is
`{:status :not-applicable, :reason :selected-target-reading-not-required}`, consistently with
`:interpretation-ask {:status :not-applicable, :reason :selection-complete-no-ask-required}` and
`:reading-frontiers :preparation-backlog` not-applicable — the selection completed without a target
reading. The occurrence-square work belongs to the **cascade authoring/redecision producer** (its own
phase in the phase table). A not-applicable selected-target-reading is therefore **not by itself** the
"silent not-required fallback" the R28 condition names; the condition is violated only if a reading
that ran is silently dropped.

### Is the reading that ran + frontier retained in supported locations? (no tooling reconstruction)
- Run record itself: `[:decision …]` retains occurrence-ids throughout (selection-certificate,
  action-comparison, q2-construction); `[:habit-reads :occurrences n :consumption :candidate-ids m]`
  retains **36** `:wm/reading-interpretation-debt-v1` maps (analysis-digest 74781ee5…, policy-ids,
  arrangement-digests); `[:reading-frontiers :selection-frontier]` retains eligible/assessed/
  reading-absent ids and occurrence-id keyed exclusions; the square registry is pinned in-record
  `{:target "M-pattern-mining" :analysis-digest 74781ee5… :count 6 :digest f9d2f590…0159 :published
  true :job-id invoke-1793311721807-7781-…}`.
- Published report-card.edn (same run dir): dwell stop, phase table with restarts, seats.
- Ledger entry 124: the post-retry failure with cause-record-pin back to the run record.
So reading and frontier ARE retained at supported locations. What is missing is exactly one thing:
the record's declared reading field for the **occurrence-square producer** — the reading lives in
`:decision`/`:habit-reads`/registry pins, not in a first-class field a failure-record reader consults.

### Clause verdict under the written condition ("Real failed-redecision run record retains the reading
that ran; no silent not-required fallback; tooling replay and load evidence alone do not close")
- Verdict: **partially met — the "retains the reading" clause is met (in-record, non-tooling), and the
  "silent fallback" clause is NOT violated by the selected-target-reading field (distinct producer),
  BUT the failed redecision's reading is not retained at a field of its own producer**, so a reader of
  the failure record can still miss it without knowing where to look. The report's closure refusal
  reaches the right bottom line by partly the wrong route; its "Later retry: none" is factually wrong
  and must be corrected before any tracker text cites it.
- **Proposed status: row stays OPEN** (report's disposition unchanged), with corrected reasoning:
  the missing item is a declared retention field for the occurrence-square producer's reading in
  failed-redecision records — a failure-record taxonomy item, not R28 machine work. No new
  field-specific requirement is invented here beyond what the written condition already implies.

## Row E3-OUTER-REPEAT — independently CONFIRMED, clause by clause

### Clause (1) unchanged refused inputs prevented before author dispatch — CONFIRMED, in-record
- Predecessor 2026-10-30-e5f08d91 (record sha ebd2b969…bf25 ✓ re-verified): refused
  (:guardrail-refusal) on M-typed-memories.
- 3415 record: `[:q6]` = `:wm/q6-predecessor-v1`/`:wm/q6-terminal-v1` chain pinning the predecessor
  run-id, startedAt 00:43:07Z, `:input-digest c1801780…6315`, and the refused **choice policy-key**
  `[:wm/reading-policy-v1 "M-typed-memories" "M-typed-memories/policy-ed6ff8783a9a9b045f59" {…node set…}]`;
  the field-layer exclusion entry for M-typed-memories (`:reason :repeat-choice-after-refusal`) carries
  `:reader-refusal`/`:predecessor-run-id`/`:digest` and sits in `[:outer/selection :outer-field/excluded]`
  — the **selection** layer, structurally before any author dispatch. The click's author ran on
  M-peradam-grounding (`[:outer/selection :selection :selected :target]` and terminal-receipt target
  M-peradam-grounding, outcome grounded-progress). The refused key never reached an author.
- Ordering evidence that the exclusion precedes dispatch: it is part of the selection field that
  produced the chosen target; participants (:author wm-author) and terminal target are the downstream
  layers of the same record.

### Clause (2) changed-input continuation retains differing identities — CONFIRMED, exact identities
- 2026-10-29-347b9b51-637d-4291… (record sha babfbb87…a2de ✓):
  `[:outer-repeat-comparison] {:status :inputs-changed, :matched-refusal nil, :comparisons [{:path
  tick-run-record-2026-10-29-b1a049d7-d4f8-4f63-be69-6872d86e8827.edn, :record-sha256 0d11787a…ef68 ✓
  (re-verified), :run-id 2026-10-29-b1a049d7…, :failure {:kind :already-satisfied :stage :author-wait},
  :inputs {:selection-input-digest 8768d5ad…}}]}` with its OWN `:selection-input-digest 975c0b31…e019`
  — the two digests differ and both records retain them; 347b9b51 reached the author (participants
  author present; its own failure is at author-wait/review tripwire T5, i.e. post-dispatch stage).
- Corroborating ff4b4d5c refused-reading-then-reselection is cited by the report; not re-derived here
  (non-load-bearing once (1) and (2) stand on their own pairs).

### Predicate non-conflation (explicitly)
Two distinct predicates coexist in the same 3415 record: **Q6** (`[:q6]`,
`:wm/q6-exclusion-v1`/`:wm/q6-terminal-v1` — keyed on the predecessor's *refused choice policy-key*
plus input-digest equality, rearm `:selection-inputs-change`) and the **outer-repeat index**
(`[:outer-repeat-comparison]`, `:wm/outer-repeat-comparison-v1` — compares *selection input digests*
across runs to detect repeat-after-refusal). In 3415 they disagree as designed: q6 refuses
M-typed-memories while outer-repeat-comparison says `:no-prior-refusal` (no prior *outer* refusal on
these inputs). Clause (1) is witnessed by the Q6 predicate; clause (2) by the outer-repeat predicate.
The report used each for the right clause and did not conflate them; this review states the
distinction so the tracker text can carry it.

### Proposed status
**Tracker-only closure WARRANTED** (report's disposition confirmed), with the predicate distinction
recorded and the honest two-pairs caveat the report already carries.

## Bottom line

| Row | Report's clause verdicts | Report's facts | This review's status proposal |
|---|---|---|---|
| B1-TICKETS | Kept | **Wrong numbers** (2579/190-T/prereq-470 not at field layer; true: 544+64=608 exact partition, 188 T all admitted, 0 T excluded; census 412 is the all-task layer) | Closure warranted **after numeric correction**; certifies only the 608 population, no larger substrate claim |
| E3-OUTER-REPEAT | Kept | Confirmed on re-derivation, exact digests/shas verified | Closure warranted; carry Q6-vs-outer-repeat predicate distinction |
| B2-D4-R28 | Bottom line kept | **"Later retry: none" false** (retry delivered+fixed in-record and in occurrence-identity-live receipt; terminal failure is post-retry cascade-resume, ledger ord.124 open); producer-distinctness correction accepted | Row stays OPEN; missing item is a declared retention field for the occurrence-square producer's reading, not "no reading retained" |

Budget check at review time (02:2xZ): 243/229/14 available-14, runner idle — the 229th charge is click
wm-click-eaa9bc3a (01:10:40Z, separately authorized); this review spent nothing and touched nothing.
