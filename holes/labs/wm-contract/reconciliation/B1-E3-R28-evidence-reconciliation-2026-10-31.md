# B1-TICKETS / E3-OUTER-REPEAT / B2-D4-R28 — evidence-only reconciliation (2026-10-31)

Reviewer: zai-5, dispatched by codex-11 (job invoke-1793412747718-8549-10ec5544),
against controlling checkpoint 4774d8671 (CHECKPOINT-criteria-focus-2026-10-31).
Evidence-only: no source edits, no test runs, no reloads, no clicks, no credit, no
contacts, no delegation. Machine-readable inventory alongside this file
(evidence-inventory.json). Local raw-note pins are given so conclusions can be
re-checked without local access; every load-bearing fact is pinned by path+sha256.

## Row 1 — B1-TICKETS

Written closure condition: *"Declared intended field includes ticket source with
justified per-item admission/exclusion; mission-only success cannot close this row."*

Clause (a) field includes ticket source — **satisfied**.
- Charged click 3415 selection pins its declared field scope:
  `:scope {:scope :population/missions-excursions-tickets, :population-sha 5bd66c75…eaf2,
  :coverage {:checker-sha 08f6b5d6…468e, :counts {:read 608}, :status :complete}}`
  (record `tick-run-record-2026-10-30-3415b671…edn`, sha `0af126fe…aa36`, `:outer/selection`).
- Tickets appear on both sides of the field: admitted T-items (e.g. `T-outer-target-novelty-term`,
  the full `T-repair-occ-*` set) and excluded T-items with per-target reasons
  (190 of 230 uniquely-targeted exclusions are T).
- The 608 population itself is M181/E239/T188, zero missing/extras at the pinned
  snapshot, independently certified: `q1-ticket-prep/coverage-608.json`
  (sha `a6d85097…1a7d`, identical to the corrected-certificate sha recorded in the
  publication receipt), members list sha `5bd66c75…eaf2` — byte-identical to the
  population-sha the live selection consumed. Reading identity joined.

Clause (b) justified per-item admission/exclusion — **satisfied, with one nuance**.
- Per-item exclusion justification exists at both layers: published census
  `exclusions-608.json` (412 exclusions: complete 361, inactive 30, operator-gated 13,
  not-actionable 5, draft 3; sha `8dd1fa34…0d92`) and the 3415 field's own
  `:outer-field/excluded` (per-target `:reason` + `:status-line`/`record-pins`;
  reason kinds incl. `prerequisite-unresolved` 470, `previous-choice-refused-on-same-inputs`,
  `repeat-choice-after-refusal`, `all-policies-already-satisfied`, `open`, `active`…).
- Admitted-item justification is by pin-joined census membership (admitted = in the
  declared 608 population, byte-pinned), not a separate per-item receipt. I judge
  that sufficient for the condition as written; no extra "live winner" requirement is
  invented (checkpoint's guard respected).
- Nuance, recorded not blocking: the 3415 *selection field* (2579 admitted + ~230+
  excluded unique targets) is the substrate candidate field, larger than the 608
  reading population; the two layers are joined by `population-sha`, not 1:1 in
  cardinality. The condition speaks of the declared intended field and its source
  scope — both pin the same ticket-inclusive population.

Is the OPEN text stale? **Yes in part.** The disposition still reasons from run
ff4b4d5c ("419/607/188 missing… Ticket-scope closure remains unmet") and ends
"OPEN remains" before the 3415 selection consumed the ticket-inclusive published
population on a charged click. Against the written condition the existing evidence
now covers both clauses.

Proposed disposition: **tracker-only closure warranted** (no new gate, no auto-close;
codex-11/Joe to enact). GRID B1 status should remain the checkpoint's conservative
"strong scoped evidence; row reconciliation due" until the tracker edit lands.

## Row 2 — E3-OUTER-REPEAT

Written closure condition: *"Live cross-run observation demonstrates unchanged
refused inputs prevented before author dispatch; changed-input continuation retains
differing identities. Implementation, load, and synthetic clone alone do not close
this row."*

Clause (1) unchanged refused inputs prevented before author dispatch — **satisfied,
live, cross-run**.
- Predecessor run 2026-10-30-e5f08d91 (record sha `ebd2b969…bf25`) refused on target
  `M-typed-memories`: terminal-outcome `:guardrail-refusal`, kind
  `:selected-want-mission-observation-refused`, publication `:refused`.
- Next run 3415 (charged click, record sha `0af126fe…aa36`) excluded the same target
  pre-dispatch: `:refusals [{:target "M-typed-memories", :kind :repeat-choice-after-refusal,
  :scope :this-click, :predecessor-run-id "2026-10-30-e5f08d91…", :digest c1801780…6315,
  :q6 {:schema :wm/q6-exclusion-v1, :reason :previous-choice-refused-on-same-inputs,
  :status :refused, :predecessor {:status :present, :sha256 d9063e19…1f00}, …,
  :rearm {:condition :selection-inputs-change, :input-digest c1801780…6315,
  :excluded-key [:wm/reading-policy-v1 "M-typed-memories"
  "M-typed-memories/policy-ed6ff8783a9a9b045f59" {…policy node set…}]}}}]`.
  3415's author dispatch went to a different target (`M-peradam-grounding`); the
  refused key never reached an author.
- Identity fields pinned exactly (answering the "do not equate reading identity with
  policy identity" guard): the exclusion key is a *reading-policy* identity —
  policy id `M-typed-memories/policy-ed6ff8783a9a9b045f59` plus its node set — and
  the match is on `input-digest` equality with the pinned predecessor record. Reading
  identity and policy identity are joined explicitly in the retained record, not
  conflated by assertion.

Clause (2) changed-input continuation retains differing identities — **satisfied,
live, cross-run** (retained raw record re-read for this packet).
- Run 2026-10-29-347b9b51 (record sha `babfbb87…a2de`, matching the sha already
  cited in the tracker): `:outer-repeat-comparison {:status :inputs-changed,
  :matched-refusal nil, :comparisons [{:path tick-run-record-2026-10-29-b1a049d7….edn,
  :record-sha256 0d11787a…ef68, :run-id 2026-10-29-b1a049d7…, :failure {:kind
  :already-satisfied, :stage :author-wait}, :inputs {…}}]}`, with differing
  `selection-input-digest` (`975c0b31…e019`) vs the compared prior run; the run
  reached author dispatch. Comparison record is retained, not phase-labelled.
- Corroborating same-mechanism evidence: ff4b4d5c (record sha `11d7070c…a5e`)
  retains a refused reading attempt (`M-typed-memories`, response-invalid, format-
  correction + response pins retained) followed by reselection to
  `M-peradam-grounding` and successful publication — the refused attempt and the
  differing successor identities are both in the run record.

Honest caveat: the two clauses are witnessed by *different* run pairs (e5f08d91→3415
for unchanged-prevention; b1a049d7→347b9b51 for changed-continuation). The written
condition does not require one pair, and each clause is individually live. No
synthetic clone or load evidence is relied on.

Is the OPEN text stale? **Yes.** The tracker's latest line still reads
"unchanged-input prevention remains pending… live witness pair still outstanding."
The 3415 q6 exclusion is exactly that outstanding witness, in a retained charged-click
record. Propose **tracker-only closure warranted** (no auto-close). E3 the *criterion*
(E3-T12 detector streak) remains separately open and is not touched by this row.

## Row 3 — B2-D4-R28

Written closure condition: *"Real failed-redecision run record retains the reading
that ran; no silent not-required fallback; tooling replay and load evidence alone do
not close this row."*

Actual b8777274 record (click wm-click-b8777274-564e…, run 2026-10-29-39a845a3…,
record sha `591c5505…01dd`):
- The failed occurrence-square-redecision is real and retained: debugger-dwell
  receipt `{:phase :occurrence-square-redecision, :condition-kind
  :inadmissible-decision, :restart-choice :retry, :disposition :fixed,
  :duration-ms 811062}`; live exception retained separately in
  `empty-refusals/held.edn` (sha `f3123cb8…786a`).
- The reading that ran IS recoverable inside the record — the consumption path
  carries `:interpretation-debt {:schema :wm/reading-interpretation-debt-v1,
  :analysis-digest 74781ee5…8d6f, :excerpt-digest eea856f6…28c0, …}` with pinned
  policy ids and arrangement digests; the square registry itself is content-pinned
  (digest `f9d2f590…0159`, file byte-sha `09f27d8c…b2f87`).
- **BUT** the record's declared reading field is
  `:selected-target-reading {:status :not-applicable, :reason
  :selected-target-reading-not-required}` — this is the *silent not-required
  fallback* the condition explicitly names. The reading is retained only in
  downstream consumption structures, not at the field a reader of the failure record
  would consult; the frontier (square registry with 6 squares / 4 voids, 3 join
  refusals) is reconstructable from pinned storage plus the tooling reproduction
  (`empty-refusals/reproduction.edn` sha `b947cb47…f53f`), which the condition
  excludes as closure evidence.
- Later retry: none. The discovery record states no restart has been applied; the
  run record contains no second redecision attempt outcome. The successful
  reselection in ff4b4d5c is a *successful* redecision on a different click —
  historical failure retention vs successful retry are distinct, and only the former
  exists here, in degraded form.

Is the OPEN text stale? **No.** "IMPLEMENTED; LIVE OBSERVATION PENDING" is accurate.
Proposed disposition: **closure NOT warranted**; row stays open. Missing evidence:
a failed-redecision run record whose `:selected-target-reading` field itself retains
the reading that ran (not `:not-applicable`), plus retained frontier, ideally through
the supported retry. The one candidate (b8777274) fails the no-silent-fallback clause
on its face. This is a reporting defect of the run record, not new machine work; if
codex-11 wants it fixed it belongs to the failure-record taxonomy, not to R28 closure.

## Bottom line for codex-11

| Row | Condition met? | OPEN text stale? | Tracker-only closure |
|---|---|---|---|
| B1-TICKETS | Yes (both clauses, pin-joined) | Partly — predates 3415 | **Warranted** |
| E3-OUTER-REPEAT | Yes (both clauses, live, different pairs) | Yes — witness existed | **Warranted** |
| B2-D4-R28 | No — silent not-required fallback present | No | **Not warranted** |

No spec/gate revision requested; no automatic closure performed. All dispositions
are proposals for codex-11/Joe. Budget untouched (14).
