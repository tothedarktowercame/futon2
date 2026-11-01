# L5-7-D — Discovery: how the Q9 factorized-preference certificate reaches the serving click (claude-2 → codex-11, 2026-11-01)

Base: futon2 `main` (≥ `a2a3123a2`). Read-only: no source change, no `data/` write, no reload.
Context: the hold is lifted and the machine runs continuously (plan, status 17:2xZ). Each
click's two-run Lean verdict is the criterion. Both runs fail Q9 today, by design:
`scripts/wm_run_facts.clj` (line ~795) exports `factorizedPreference` as a verified negative
because its sole producer, `futon2.aif.factorized-preference-sut`, "is deliberately absent
from the serving composition", and the selection export copies the work run's Q9 facts
(line ~1164). Lean Q9 (`Requirements.lean` 477) needs a `Certificate` valid against two
registries (`FactorizedPreferenceSemantics.lean` 84–140): a relevance calibration
(focused/related/unrelated costs) authorized by a receipt whose `authorizedBy` is the
calibration's authority, and a factorization authorization (identity, version, authority
≠ the selector's authority, law `completionThenConditionalRelevance`).

## Questions (answer each with file:line evidence, or "not found")

1. **Attachment point.** `factorized-preference-sut/compose-and-score!` composes "at the real
   class scorer". Which serving function is that scorer today (`cascade_selection`,
   `decision_gate`, `policy`, …), on which call does the inner selection score classes during
   a click, and what inputs would `compose-and-score!` need there that the serving path has
   or lacks?
2. **Authorizations that exist.** `resources/wm/standing-authority/policy.edn` carries
   `:authorized-by`. Is there any calibration authorization (costs + receipt + authority) and
   any factorization authorization (identity `:futon2/completion-first-softmax-v1`, version 1,
   authority, receipt) anywhere in `resources/`, `data/`, or the registries the exporter reads
   (`factorized-preference-record` → `[:registries :calibration]`, `[:registries :factorization]`)?
   If none: name exactly what must be authorized and by whom (the authority must differ from
   the selector's authority) so Joe can authorize it in one decision.
3. **Carrier shape on the records.** What does the exporter's `factorized-preference-record`
   reader expect on the run record (keys, schema `:wm/factorized-preference-v1`), and would the
   same carrier need to be on the selection record for the selection run, or is the copy from
   the work run (line ~1164) the intended route?
4. **Existing pins.** Which tests exercise `factorized-preference-sut` and `cascade_efe_sut`
   today, and do they run the real class scorer or a stub?
5. **Smallest wiring.** Propose, in prose with the functions named, the smallest change that
   puts a valid carrier on the tick record of a real click, and what it would refuse (typed)
   when an authorization is missing. Do not implement.

## Report

`reconciliation/L5-7-D-q9-serving-codex11-2026-11-01.md`, committed on a branch
`codex11/l5-7-d` (report only). Bell claude-2 with the sha and the five answers in brief.
