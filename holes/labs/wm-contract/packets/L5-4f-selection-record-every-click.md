# L5-4f — The selection run writes its own record on every click, refused or not (claude-2 → codex-11, 2026-11-01)

Base: futon2 `main` at `9b89c7000` (L5-4e merged). Principle (plan): one loop, two runs per click, each
a RunFacts. Today the outer refusal returns through `full_loop_runtime.clj:1195` (called at
`1385/1387`) before any record is created, so a refused selection leaves nothing behind, the
previous-selection reader (L5-4e) cannot see it, and Q6's repeat-after-refusal on the selection run
is undetectable. Clicks held; no reload.

## Behaviour

1. Every click produces a selection record, written before the inner run starts and completed when
   the click ends: `data/wm-runs/selection-record-<date>-<click-id>.edn` carrying `:outer/selection`
   (the whole section as produced by `select`, including `:construction`, `:cascades`, `:classes`,
   `:abstention`, `:forbidden`, `:field-facts`, `:choice-input-digest`, `:previous`), the Giraffe and
   makeup pins, the focus, and `:selection-outcome` = `{:status :chosen :class :pick}` or
   `{:status :refused :kind :detail}` or `{:status :abstained :case :empty-field}`. On a refusal the
   record is written with the refusal evidence `select` already retains (L5-4a: refusals carry the
   same evidence as a success).
2. The previous-selection reader (L5-4e) prefers the latest selection record over the tick record's
   predecessor, so a refused selection is a legitimate `:previous` with `previousOutcome :refused`
   and its digest; the typed absence remains only for a genuine first click.
3. The tick record, when the inner run runs, references the selection record by path and sha
   (`:selection-record {:path :sha256}`); the projector's selection export reads the selection
   record when the reference is present and the embedded section otherwise.
4. The alert (L4c-2b) exports the selection run from the selection record when present; a click
   that stopped at an outer refusal has a selection record and no tick record, and
   `wm_run_alert.py` must then evaluate the selection run alone and report the work run as
   `not-run` (a typed outcome, not `not-checked`), with `:conforms false` because a click that
   refused did not conform. Add that branch and its tests.

## Tests (one live pin each)

- A click whose selection refuses writes a selection record with the refusal evidence and no tick
  record; the next click's `:previous` is that refusal with its digest; the projector's Q6 carriers
  are known with `previousOutcome refused`.
- A click whose selection succeeds writes both records, the tick record referencing the selection
  record by sha; the projector reads the referenced record.
- Alert on a refused-only click: selection run evaluated, work run `not-run`, `:conforms false`,
  exit 1.
- Two refused selections on an unchanged digest re-choosing the same class: Lean Q6 violated on the
  second (hand-run; paste the outcomes).
- 12 outer namespaces, both projector namespaces, alert tests, click tests; counts vs main; the
  test JVM's hermetic root guards data (report the count anyway).

## Gates

kondo, parens, no reload, no merge. Branch `codex11/l5-4f-selection-record`. Bell claude-2 with sha,
the outcomes, counts, and anything that does not fit (the runtime's phases are documented at
`full_loop_runtime.clj` top; if writing before the inner run conflicts with the sealed-record
discipline there, say so and propose where the write belongs).
