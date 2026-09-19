# Receipt: two authorized production-shaped validation clicks

Date: 2026-09-19 UTC

Authority: `AUTH-ordinary-click-budget-2026-09-19.md`, with the two-click
authorization in item 3 of `RULING-selection-precedence-2026-09-19.md`.
These are grants 2 and 3 of the five-click P-0 allocation. Each grant was
issued before its worker, through the ordinary-click budget machinery. No run
was retried.

The unforced shell preflight initially refused before issuing a grant because
its legacy diagnostic still treated open repair obligations as selection
preemption. The committed ruling says open obligations are evidence rather
than a veto, so the documented `--force` transport was used. Budget issuance
and consumption still occurred normally before each worker; no budget refusal
was bypassed.

## Grant 2 / click 1

- Click: `wm-click-cd6bf111-3508-4775-a351-66fca00dc86c`
- Issued: `2026-09-19T20:02:27.983605002Z`
- Issuing caller: `codex-23`
- Run: `2026-09-19-1789848147`
- Record: `data/wm-runs/tick-run-record-2026-09-19-1789848147.edn`
- Durable binding: `/home/joe/code/futon3c/data/wm-click-run-bindings/click-run-binding-wm-click-cd6bf111-3508-4775-a351-66fca00dc86c.edn`
- Outcome: `agent-unavailable` at `FULL_LOOP_CLOSE`; binding reported durable
  and verified.
- Finding: `data/wm-repair-obligations/findings/repair-ea1-f9a0a2fabd9e1a60116e846608a0e56e0b6f4a8f23cab048bafdde39a10ec0f6--attempt-002-agent-unavailable.edn`
- `scripts/wm_run_validity.bb`: `INVALID (0/5 ok)`; C source, rates
  provenance, posterior, U37, and G terms are missing. The G census says
  `:missing` because `:no-recorded-cascade-selection`.
- `:open-stop-lines`: absent from the run record (not an empty collection).
- Ordinary selection: not demonstrated. The record has no controller decision
  and no `:selection-source`; its only decision datum explicitly says
  `:no-recorded-cascade-selection`. In particular, it also does not contain
  `:selection-source :stop-the-line`.
- Habit provenance and mass vector: absent. No ranked-candidate menu was
  recorded, so there are no per-candidate `:source` or `:count` values and no
  mass vector whose constancy can be assessed. This must not be read as a
  constant zero vector.
- Selected mission: absent; no candidate was selected.
- C source: absent; the record establishes neither `:derived` nor a fallback
  constant.
- G-term decomposition: `{:schema :wm/g-term-decomposition-v1,
  :status :missing, :reason :no-recorded-cascade-selection, :policies []}`.

## Grant 3 / click 2

- Click: `wm-click-f5de94db-c46c-419d-8abf-c1b48082bb24`
- Issued: `2026-09-19T20:03:54.361432984Z`
- Issuing caller: `codex-23`
- Run: `2026-09-19-1789848234`
- Record: `data/wm-runs/tick-run-record-2026-09-19-1789848234.edn`
- Durable binding: `/home/joe/code/futon3c/data/wm-click-run-bindings/click-run-binding-wm-click-f5de94db-c46c-419d-8abf-c1b48082bb24.edn`
- Outcome: `agent-unavailable` at `FULL_LOOP_CLOSE`; binding reported durable
  and verified.
- Finding: `data/wm-repair-obligations/findings/repair-ea1-34838db302be9b75a18540c560b2ecf46e768a8c87bf64afff4e6289b67c5907--attempt-001-agent-unavailable.edn`
- `scripts/wm_run_validity.bb`: `INVALID (0/5 ok)`; C source, rates
  provenance, posterior, U37, and G terms are missing. The G census says
  `:missing` because `:no-recorded-cascade-selection`.
- `:open-stop-lines`: absent from the run record (not an empty collection).
- Ordinary selection: not demonstrated. The record has no controller decision
  and no `:selection-source`; its only decision datum explicitly says
  `:no-recorded-cascade-selection`. In particular, it also does not contain
  `:selection-source :stop-the-line`.
- Habit provenance and mass vector: absent. No ranked-candidate menu was
  recorded, so there are no per-candidate `:source` or `:count` values and no
  mass vector whose constancy can be assessed. This must not be read as a
  constant zero vector.
- Selected mission: absent; no candidate was selected.
- C source: absent; the record establishes neither `:derived` nor a fallback
  constant.
- G-term decomposition: `{:schema :wm/g-term-decomposition-v1,
  :status :missing, :reason :no-recorded-cascade-selection, :policies []}`.

## Boundary

Both authorized clicks were consumed and recorded exactly once. Both failed
before real cascade selection because an author agent was unavailable. The
validity results are evidence, not gates, and are reported without promotion
to successful production validation. No repair was resolved, no source was
changed, and no third click was attempted. Two grants remain in the allocation.
