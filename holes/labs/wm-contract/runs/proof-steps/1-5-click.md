# ⟨1⟩5 — the live click, fired and reported from the records

zai-1, 2026-09-23. Pre-flight clean in the serving JVM, then the click fired
from futon2 (`bash scripts/wm_click.sh --run --author wm-author --reviewer
wm-reviewer --repair-reviewer codex-13`), logged to
`/tmp/claude-5/click-1-5c.log`.

## Pre-flight (all five clean)

1. **The ticket SURVIVES record-supply** in the live tick's assembly path
   (load-declared + assemble + the REAL supply scan + record-supply):
   `:survives-record-supply true`, the old
   `:repair-closure-observation-unavailable` decline does NOT fire, and the
   distinction record shows it supplied-by-declaration.
2. **Queue**: `{:status :front-stratum :eligible-targets ["T-repair-occ-444fb018…"]}`
   — the ticket IS the admitted front entry; the eligible stratum is
   non-empty.
3. **Candidates/class**: both candidates constructed; classification
   `{:class :focus :kind :ticket-parent :focus-status :retained :focus "WM"}`.
4. **The decision standing**: G(C2)=0.5978, G(C1)=2.9957 (the same values as
   every prior check).
5. **Blockers**: no :unknown targets in the family; wm-author, wm-reviewer
   and codex-13 all idle + invoke-ready; no click in flight.

## The click, from the records

- **Click id** `wm-click-e3e4479c-e873-4dae-8292-44de95c317e6`; **run id**
  `2026-09-23-1790131591`; **attempt dir**
  `data/wm-full-loop-machinery-72/wm-contract-machinery-72-v1/attempt-001/`;
  **outcome** `:grounded-change`; **binding** verified/confirmed per the
  script's terminal report (run-binding published at stage close).
- **SELECTED: the reference ticket, candidate :C2** — the run record's
  `:ticket-queue {:eligible-targets ["T-repair-occ-444fb018…"]}`, decision
  `:decided-by :ticket-queue`, `:choice` = `:aif/declare-the-conditioning`
  producing `[T … :repair/split-declared-valid]`, and the `:stratum-posterior`
  keys are the reference ticket's C2 — the machine selected through the
  ticket-queue stratum, by the class-scored G, exactly as the proof's
  theorem states.
- **What was delivered**: commit `ee22106c` "Declare prospective EIG
  held-out split" — `resources/wm/eig/held-out-split.edn` (23 lines),
  `src/futon2/aif/held_out_split.clj` + its test, load-identity wiring, and
  13 lines on the ticket file. C2's declared first action, executed and
  reviewed, closed `:grounded-change`.
- **`:accepted-increment` on the close**:
  `{:accepted? :no-acceptance-declared
    :reason :target-has-no-mechanical-acceptance-declaration …}` — the
  predicate ran, recorded its typed result, and (as designed) refused
  nothing: the close proceeded. The reason is honest: the runner's wiring
  reads the acceptance declaration from
  `[:controller-decision :action :accepted-increment :acceptance]`, a slot
  the class-scoring decision does not populate — the predicate found no
  acceptance map and correctly said so rather than guessing. (The earlier
  adapter fix 74dc5de1 addressed a different crash; this is the
  input-plug still not carrying the declared acceptance.)
- **B update**: NOT written — correctly. The guard requires
  `{:accepted? true}`; the recorded result was `:no-acceptance-declared`, so
  `b-update` was never called. No spurious update.
- **Certificate**: `:preference-audit` `{:status :recorded
  :consumed-preference-kind :class-emission :class-preference {focused
  11/20, related 7/20, unrelated 1/20, stop-the-line 1/20}
  :class-preference-provenance {… "PROOF-wm-works 1.3; Joe 2026-09-22
  ruling"…}}` — the class certificate says what was actually consumed.
  `:precision-family` carries `:model-id "0f06c582…"` and the model's `:q0`
  is the point mass at `[T :admission/task-stated]` with `:horizon 4` —
  the precision chain carried (state :held on model change with
  reinitialization recorded, per its own law).
- **Horizon**: T = 4, the source-declared value, riding on the model
  (`:horizon 4` in the precision-model) — the declarations list itself is
  not echoed on this record, only the effective T.

## Honest summary against the step's check

The click CHOSE as the proof requires — one command, the reference target,
the ticket-queue stratum, the class-scored G picking C2, work delivered and
closed `:grounded-change`. Two ⟨1⟩6/⟨1⟩7 items remain open and are recorded
as such: the accepted-increment predicate's acceptance INPUT is not yet
wired from the decision to the close (so it correctly reports
:no-acceptance-declared rather than true), and consequently no B update was
written — both are downstream of the same missing plug, not new defects.
